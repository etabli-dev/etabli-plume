// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DictionaryTest {
    private val alpha = TeXCommand("\\alpha", "alpha", "α", "Greek", null, null)
    private val fraction = TeXCommand("\\dfrac", "display fraction", "a/b", "Math", "amsmath", "\\dfrac{a}{b}")
    private val dictionary = TeXDictionary(1, listOf("Greek", "Math"), listOf(alpha, fraction), emptyList())

    @Test fun loadsTheCatalogueTheAndroidAppShips() {
        val bundled = TeXDictionary.loadBundled()
        assertEquals(1, bundled.version)
        assertEquals(123, bundled.entries.size)
        assertEquals(10, bundled.snippets.size)
        assertEquals(10, bundled.categories.size)
        assertEquals("\\alpha", bundled.entries.first { it.name == "alpha" }.command)
        val article = bundled.snippets.first { it.title == "Article skeleton" }.code
        assertTrue(article.startsWith("\\documentclass"))
        assertTrue("\n\\begin{document}\n" in article)
    }

    @Test fun bundledResourceIsByteIdenticalToTheAndroidAsset() {
        val asset = java.io.File(System.getProperty("plume.dictionaryAsset"))
        val resource = TeXDictionary::class.java.classLoader.getResourceAsStream(TeXDictionary.RESOURCE)!!
            .use { it.readBytes() }
        assertTrue(asset.readBytes().contentEquals(resource))
    }

    @Test fun decodesOptionalFieldsLikeAndroid() {
        val parsed = TeXDictionary.parse(
            """
            {"version": 1, "categories": ["Math"],
             "entries": [
               {"command": "\\dfrac", "name": "display fraction", "preview": "a/b", "category": "Math",
                "package": "amsmath", "usage": "\\dfrac{a}{b}", "future": true},
               {"command": "\\cdot", "name": "dot", "preview": "·", "category": "Math", "package": " "}
             ],
             "snippets": []}
            """.trimIndent(),
        )
        assertEquals("amsmath", parsed.entries[0].pkg)
        assertEquals("\\dfrac{a}{b}", parsed.entries[0].usage)
        assertNull(parsed.entries[1].pkg)
        assertNull(parsed.entries[1].usage)
    }

    @Test fun searchesCopyableSourceSymbolsPackagesAndTrimmedNames() {
        assertEquals(listOf(alpha), dictionary.search("  ALPHA \n"))
        assertEquals(listOf(alpha), dictionary.search("α"))
        assertEquals(listOf(fraction), dictionary.search("amsmath"))
        assertEquals(listOf(fraction), dictionary.search("\\dfrac{a}"))
        assertEquals(listOf(alpha), dictionary.search("greek"))
        assertEquals(emptyList(), dictionary.search("alpha", "Math"))
        assertEquals(listOf(fraction), dictionary.search(" ", "Math"))
    }

    @Test fun searchesSnippetTitlesCategoriesAndCode() {
        val bundled = TeXDictionary.loadBundled()
        assertEquals(listOf("Article skeleton"), bundled.searchSnippets("  article SKELETON ").map { it.title })
        assertTrue(bundled.searchSnippets("\\begin{itemize}").any { it.title == "Itemize" })
        assertEquals(bundled.snippets, bundled.searchSnippets(""))
    }

    @Test fun rejectsDuplicateIdsAndUnknownCategoriesBeforeRendering() {
        dictionary.validate()
        assertFailsWith<IllegalArgumentException> { dictionary.copy(entries = listOf(alpha, alpha)).validate() }
        assertFailsWith<IllegalArgumentException> { dictionary.copy(categories = listOf("Greek")).validate() }
        assertFailsWith<IllegalArgumentException> {
            dictionary.copy(snippets = listOf(TeXSnippet("Empty", "Math", ""))).validate()
        }
        assertFailsWith<IllegalArgumentException> { TeXDictionary.parse("""{"categories": [], "entries": [], "snippets": []}""") }
        assertFailsWith<IllegalArgumentException> { TeXDictionary.parse("not json") }
    }
}
