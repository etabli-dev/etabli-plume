// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.desktop.prefs

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class PreferencesStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun file(): Path = tmp.root.toPath().resolve("nested").resolve(PreferencesStore.FILE_NAME)

    @Test fun startsWithDefaultsWhenNothingWasSaved() {
        val store = PreferencesStore(file())
        assertEquals(Preferences(), store.load())
        assertNull(store.loadError)
    }

    @Test fun favouritesSurviveARestart() {
        val first = PreferencesStore(file())
        first.save(Preferences(favourites = setOf("\\sum_{i=1}^{n}", "\\alpha"), alwaysOnTop = true, theme = ThemeMode.Dark))

        val reopened = PreferencesStore(file()).load()
        assertEquals(setOf("\\alpha", "\\sum_{i=1}^{n}"), reopened.favourites)
        assertTrue(reopened.alwaysOnTop)
        assertEquals(ThemeMode.Dark, reopened.theme)
        // Same keys as the mobile apps: the exact command source, sorted for stable files.
        val text = file().readText()
        assertTrue(text.indexOf("\"\\\\alpha\"") in 0 until text.indexOf("\"\\\\sum_{i=1}^{n}\""), text)
        // The atomic write leaves no temporary files behind.
        assertEquals(listOf(file()), file().parent.listDirectoryEntries())
    }

    @Test fun keepsAnUnreadableFileAsideInsteadOfOverwritingIt() {
        Files.createDirectories(file().parent)
        file().writeText("{ not json")
        val store = PreferencesStore(file())
        assertEquals(Preferences(), store.load())
        assertNotNull(store.loadError)

        store.save(Preferences(favourites = setOf("\\beta")))
        assertEquals("{ not json", file().resolveSibling("${PreferencesStore.FILE_NAME}.corrupt").readText())
        assertEquals(setOf("\\beta"), PreferencesStore(file()).load().favourites)
    }

    @Test fun repeatedRecoveryPreservesEveryUnreadableFile() {
        Files.createDirectories(file().parent)
        for (contents in listOf("first damaged preferences", "second damaged preferences")) {
            file().writeText(contents)
            val store = PreferencesStore(file())
            store.load()
            store.save(Preferences(alwaysOnTop = true))
        }
        val backups = file().parent.listDirectoryEntries("*.corrupt").map { it.readText() }.toSet()
        assertEquals(setOf("first damaged preferences", "second damaged preferences"), backups)
        assertTrue(PreferencesStore(file()).load().alwaysOnTop)
    }

    @Test fun ignoresUnknownFieldsFromNewerVersions() {
        Files.createDirectories(file().parent)
        file().writeText("""{"favourites": ["\\alpha"], "theme": "light", "windowWidth": 900}""")
        val loaded = PreferencesStore(file()).load()
        assertEquals(Preferences(favourites = setOf("\\alpha"), theme = ThemeMode.Light), loaded)
    }

    @Test fun resolvesThePerUserConfigurationDirectory() {
        val home = tmp.root.absolutePath
        val xdg = tmp.newFolder("xdg").absolutePath
        assertEquals(
            Path.of(home, ".config", "etabli-plume"),
            PreferencesStore.configDir(emptyMap(), "Linux", home),
        )
        assertEquals(
            Path.of(xdg, "etabli-plume"),
            PreferencesStore.configDir(mapOf("XDG_CONFIG_HOME" to xdg), "Linux", home),
        )
        assertEquals(
            Path.of(home, ".config", "etabli-plume"),
            PreferencesStore.configDir(mapOf("XDG_CONFIG_HOME" to "relative/dir"), "Linux", home),
        )
        assertEquals(
            Path.of(home, "Library", "Application Support", "com.raban.etabli.tex"),
            PreferencesStore.configDir(emptyMap(), "Mac OS X", home),
        )
        assertEquals(
            Path.of(xdg, "Etabli Plume"),
            PreferencesStore.configDir(mapOf("APPDATA" to xdg), "Windows 11", home),
        )
        assertEquals(
            Path.of(xdg),
            PreferencesStore.configDir(mapOf(PreferencesStore.DIR_OVERRIDE to xdg), "Linux", home),
        )
    }
}
