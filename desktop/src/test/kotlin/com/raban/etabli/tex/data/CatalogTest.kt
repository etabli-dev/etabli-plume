// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogTest {
    private val catalog = Catalog(TeXDictionary.loadBundled())

    @Test fun listsEverySectionWithItsCount() {
        val dictionary = catalog.dictionary
        assertEquals(listOf<Section>(Section.All, Section.Favourites), catalog.sections.take(2))
        assertEquals(Section.Snippets, catalog.sections.last())
        assertEquals(dictionary.categories, catalog.sections.filterIsInstance<Section.Category>().map { it.name })
        assertEquals(133, catalog.count(Section.All, emptySet()))
        assertEquals(0, catalog.count(Section.Favourites, emptySet()))
        assertEquals(10, catalog.count(Section.Snippets, emptySet()))
        assertEquals(38, catalog.count(Section.Category("Greek letters"), emptySet()))
    }

    @Test fun allSectionSearchesCommandsAndSnippetsTogether() {
        val results = catalog.items(Section.All, "bibliography", emptySet())
        assertTrue(results.any { it is CatalogItem.Command })
        assertTrue(results.any { it is CatalogItem.Snippet && it.snippet.title == "BibTeX entry (article)" })
    }

    @Test fun sectionsFilterBeforeTheQuery() {
        val favourites = setOf("\\alpha", "\\beta", "\\sum_{i=1}^{n}")
        assertEquals(
            listOf("\\alpha", "\\beta", "\\sum_{i=1}^{n}"),
            catalog.items(Section.Favourites, "", favourites).map { it.source },
        )
        assertEquals(listOf("\\sum_{i=1}^{n}"), catalog.items(Section.Favourites, "sum", favourites).map { it.source })
        assertTrue(catalog.items(Section.Category("Arrows"), "", emptySet()).all { it.category == "Arrows" })
        assertTrue(catalog.items(Section.Snippets, "", emptySet()).all { it is CatalogItem.Snippet })
        assertEquals(emptyList(), catalog.items(Section.Category("Greek letters"), "sum", emptySet()))
    }

    @Test fun ranksExactAndPrefixMatchesFirst() {
        assertEquals("\\pi", catalog.items(Section.All, "pi", emptySet()).first().source)
        assertEquals("\\in", catalog.items(Section.All, "\\in", emptySet()).first().source)
        assertEquals("\\sum_{i=1}^{n}", catalog.items(Section.All, "sum", emptySet()).first().source)
        val phi = catalog.items(Section.All, "phi", emptySet()).map { it.source }
        assertEquals(listOf("\\phi", "\\Phi"), phi.take(2))
    }

    @Test fun rankingKeepsCatalogueOrderWithinATier() {
        val items = listOf("\\beta", "\\alpha", "\\gamma").map {
            CatalogItem.Command(TeXCommand(it, it.drop(1), "", "Greek"))
        }
        assertEquals(items, Catalog.rank(items, ""))
        assertEquals(items, Catalog.rank(items, "m"))
        assertEquals(listOf("\\alpha", "\\beta", "\\gamma"), Catalog.rank(items, "a").map { it.source })
        assertEquals(listOf("\\alpha", "\\beta", "\\gamma"), Catalog.rank(items, "alpha").map { it.source })
    }

    @Test fun copiesExactSourceIncludingSnippetLineBreaks() {
        val snippet = catalog.items(Section.Snippets, "Equation (numbered)", emptySet()).first()
        assertEquals(catalog.dictionary.snippets.first { it.title == "Equation (numbered)" }.code, snippet.source)
        assertTrue("\n" in snippet.source)
    }
}
