package com.raban.etabli.tex.data

import org.junit.Assert.*
import org.junit.Test

class DictionaryTest {
    private val alpha = TeXCommand("\\alpha", "alpha", "α", "Greek", null, null)
    private val fraction = TeXCommand("\\dfrac", "display fraction", "a/b", "Math", "amsmath", "\\dfrac{a}{b}")
    private val dictionary = TeXDictionary(1, listOf("Greek", "Math"), listOf(alpha, fraction), emptyList())

    @Test fun searchesCopyableSourceSymbolsPackagesAndTrimmedNames() {
        assertEquals(listOf(alpha), dictionary.search("  ALPHA \n"))
        assertEquals(listOf(alpha), dictionary.search("α"))
        assertEquals(listOf(fraction), dictionary.search("amsmath"))
        assertEquals(listOf(fraction), dictionary.search("\\dfrac{a}"))
        assertEquals(listOf(alpha), dictionary.search("greek"))
        assertEquals(emptyList<TeXCommand>(), dictionary.search("alpha", "Math"))
        assertEquals(listOf(fraction), dictionary.search(" ", "Math"))
    }
    @Test fun rejectsDuplicateIdsAndUnknownCategoriesBeforeRendering() {
        dictionary.validate()
        assertThrows(IllegalArgumentException::class.java) { dictionary.copy(entries = listOf(alpha, alpha)).validate() }
        assertThrows(IllegalArgumentException::class.java) { dictionary.copy(categories = listOf("Greek")).validate() }
        assertThrows(IllegalArgumentException::class.java) {
            dictionary.copy(snippets = listOf(TeXSnippet("Empty", "Math", ""))).validate()
        }
    }
}
