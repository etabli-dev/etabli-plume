// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.desktop

import androidx.compose.ui.input.key.Key
import com.raban.etabli.tex.data.CatalogItem
import com.raban.etabli.tex.data.Section
import com.raban.etabli.tex.desktop.prefs.PreferencesStore
import com.raban.etabli.tex.desktop.prefs.ThemeMode
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class PlumeStateTest {
    @get:Rule val tmp = TemporaryFolder()

    private val copied = mutableListOf<String>()

    private fun store() = PreferencesStore(tmp.root.toPath().resolve(PreferencesStore.FILE_NAME))

    private fun state(store: PreferencesStore = store()) = PlumeState.load(store) { copied += it }

    private fun PlumeState.press(key: Key, primary: Boolean = false, alt: Boolean = false, shift: Boolean = false) {
        val action = keyAction(key, primary, alt, shift, searchFocused = true)
        if (action != null) perform(action) {}
    }

    @Test fun typingThenEnterCopiesTheBestMatch() {
        val s = state()
        assertNull(s.loadError)
        s.updateQuery("alpha")
        s.press(Key.Enter)
        assertEquals(listOf("\\alpha"), copied)
        assertEquals("Copied \\alpha", s.status)
    }

    @Test fun arrowKeysMoveTheSelectionWithinBounds() {
        val s = state()
        s.updateQuery("arrow")
        val results = s.results
        assertTrue(results.size > 3)
        s.press(Key.DirectionUp)
        assertEquals(0, s.selectedIndex)
        s.press(Key.DirectionDown)
        s.press(Key.DirectionDown)
        assertEquals(results[2], s.selected)
        s.press(Key.PageDown)
        assertEquals(results.lastIndex.coerceAtMost(12), s.selectedIndex)
        s.press(Key.DirectionDown, primary = true)
        assertEquals(results.lastIndex, s.selectedIndex)
        s.press(Key.DirectionUp, primary = true)
        assertEquals(0, s.selectedIndex)
        s.press(Key.Enter)
        assertEquals(listOf(results[0].source), copied)
    }

    @Test fun newQueryOrSectionStartsAtTheTop() {
        val s = state()
        s.moveSelection(5)
        assertEquals(5, s.selectedIndex)
        s.updateQuery("sum")
        assertEquals(0, s.selectedIndex)
        s.moveSelection(1)
        s.press(Key.DirectionDown, alt = true)
        assertEquals(Section.Favourites, s.section)
        assertEquals(-1, s.selectedIndex)
        s.press(Key.DirectionUp, alt = true)
        s.press(Key.DirectionUp, alt = true)
        assertEquals(Section.Snippets, s.section)
    }

    @Test fun shiftEnterCopiesTheUsageExample() {
        val s = state()
        s.updateQuery("integral")
        assertEquals("\\int_{a}^{b}", s.selected?.source)
        s.press(Key.Enter, shift = true)
        assertEquals(listOf("\\int_{0}^{\\infty} f(x)\\,dx"), copied)
    }

    @Test fun snippetsCopyTheirCompleteSource() {
        val s = state()
        s.showSection(Section.Snippets)
        s.updateQuery("article skeleton")
        s.press(Key.Enter)
        val code = copied.single()
        assertTrue(code.startsWith("\\documentclass") && code.endsWith("\\end{document}"))
        assertFalse(s.toggleFavourite(), "snippets have no favourites")
    }

    @Test fun favouriteShortcutPersistsAcrossRestarts() {
        val s = state()
        s.updateQuery("beta")
        s.press(Key.D, primary = true)
        assertTrue(s.isFavourite(s.selected!!))

        val restarted = state()
        assertEquals(setOf("\\beta"), restarted.preferences.favourites)
        restarted.showSection(Section.Favourites)
        assertEquals(listOf("\\beta"), restarted.results.map { it.source })

        // Removing the last favourite while viewing Favourites empties the list.
        restarted.press(Key.D, primary = true)
        assertEquals(emptyList<CatalogItem>(), restarted.results)
        assertEquals(emptySet(), state().preferences.favourites)
    }

    @Test fun windowSettingsPersist() {
        val s = state()
        s.press(Key.T, primary = true)
        assertTrue(s.preferences.alwaysOnTop)
        s.cycleTheme()
        assertEquals(ThemeMode.Light, s.preferences.theme)
        val restarted = state()
        assertTrue(restarted.preferences.alwaysOnTop)
        assertEquals(ThemeMode.Light, restarted.preferences.theme)
    }

    @Test fun reportsSaveFailuresWithoutChangingState() {
        val blocker = tmp.newFile("not-a-directory")
        val s = state(PreferencesStore(blocker.toPath().resolve(PreferencesStore.FILE_NAME)))
        assertFalse(s.toggleFavourite())
        assertEquals(emptySet(), s.preferences.favourites)
        assertTrue(s.statusIsError)
        assertTrue(Files.isRegularFile(blocker.toPath()))
    }

    @Test fun escapeClearsTheQueryAndListKeysNeedTheSearchField() {
        val s = state()
        s.updateQuery("zeta")
        s.press(Key.Escape)
        assertEquals("", s.query)
        assertNull(keyAction(Key.Enter, primary = false, alt = false, shift = false, searchFocused = false))
        assertNull(keyAction(Key.DirectionDown, primary = false, alt = false, shift = false, searchFocused = false))
        assertEquals(KeyAction.ToggleOnTop, keyAction(Key.T, primary = true, alt = false, shift = false, searchFocused = false))
        assertEquals(KeyAction.FocusSearch, keyAction(Key.F, primary = true, alt = false, shift = false, searchFocused = false))
        assertNull(keyAction(Key.A, primary = true, alt = false, shift = false, searchFocused = true))
    }

    @Test fun clipboardFailureIsReported() {
        val s = PlumeState.load(store()) { throw IllegalStateException("clipboard busy") }
        assertFalse(s.copySelected())
        assertTrue(s.statusIsError)
        assertTrue(s.status!!.contains("clipboard busy"))
    }
}
