// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.desktop

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.raban.etabli.tex.data.Catalog
import com.raban.etabli.tex.data.CatalogItem
import com.raban.etabli.tex.data.Section
import com.raban.etabli.tex.data.TeXDictionary
import com.raban.etabli.tex.desktop.prefs.Preferences
import com.raban.etabli.tex.desktop.prefs.PreferencesStore

/**
 * UI state and every user action of the desktop window. Kept free of Compose
 * UI and AWT so keyboard behaviour can be unit-tested; [clipboard] performs
 * the actual copy.
 */
class PlumeState(
    val catalog: Catalog,
    private val store: PreferencesStore,
    private val clipboard: (String) -> Unit,
    val loadError: String? = null,
) {
    var preferences by mutableStateOf(store.load())
        private set
    var query by mutableStateOf("")
        private set
    var section by mutableStateOf<Section>(Section.All)
        private set
    private var index by mutableIntStateOf(0)

    /** Last action feedback for the status bar; [statusSerial] changes on every message. */
    var status by mutableStateOf(store.loadError ?: loadError)
        private set
    var statusIsError by mutableStateOf(store.loadError != null || loadError != null)
        private set
    var statusSerial by mutableIntStateOf(0)
        private set

    private val resultsState = derivedStateOf { catalog.items(section, query, preferences.favourites) }
    val results: List<CatalogItem> get() = resultsState.value

    val selectedIndex: Int get() = if (results.isEmpty()) -1 else index.coerceIn(0, results.lastIndex)
    val selected: CatalogItem? get() = results.getOrNull(selectedIndex)

    fun isFavourite(item: CatalogItem): Boolean =
        item is CatalogItem.Command && item.command.command in preferences.favourites

    fun count(section: Section): Int = catalog.count(section, preferences.favourites)

    fun updateQuery(value: String) {
        if (value == query) return
        query = value
        index = 0
    }

    /** Escape: clears the query; returns false when there was nothing to clear. */
    fun clearQuery(): Boolean {
        if (query.isEmpty()) return false
        updateQuery("")
        return true
    }

    fun select(position: Int) {
        if (results.isNotEmpty()) index = position.coerceIn(0, results.lastIndex)
    }

    fun moveSelection(delta: Int) = select(selectedIndex + delta)

    fun selectFirst() = select(0)

    fun selectLast() = select(results.lastIndex)

    fun showSection(target: Section) {
        if (target == section) return
        section = target
        index = 0
    }

    fun moveSection(delta: Int) {
        val sections = catalog.sections
        val current = sections.indexOf(section).coerceAtLeast(0)
        showSection(sections[(current + delta).mod(sections.size)])
    }

    /** Enter: copies the exact source of the selected command or snippet. */
    fun copySelected(): Boolean = selected?.let { copy(it.source, describe(it)) } ?: false

    /** Shift+Enter: copies the usage example of the selected command, when it has one. */
    fun copySelectedUsage(): Boolean {
        val usage = (selected as? CatalogItem.Command)?.command?.usage ?: return false
        return copy(usage, "usage example")
    }

    fun copyPackageLine(item: CatalogItem.Command): Boolean {
        val pkg = item.command.pkg ?: return false
        return copy("\\usepackage{$pkg}", "\\usepackage{$pkg}")
    }

    fun copy(text: String, what: String): Boolean = try {
        clipboard(text)
        report("Copied $what", error = false)
        true
    } catch (e: Exception) {
        report("Could not use the clipboard: ${e.message ?: e.javaClass.simpleName}", error = true)
        false
    }

    /** Ctrl+D: toggles the selected command's favourite; snippets have none. */
    fun toggleFavourite(item: CatalogItem? = selected): Boolean {
        val command = (item as? CatalogItem.Command)?.command?.command ?: return false
        val current = preferences.favourites
        val added = command !in current
        val updated = preferences.copy(favourites = if (added) current + command else current - command)
        if (!persist(updated)) return false
        report(if (added) "Added $command to favourites" else "Removed $command from favourites", error = false)
        return true
    }

    fun toggleAlwaysOnTop(): Boolean {
        val updated = preferences.copy(alwaysOnTop = !preferences.alwaysOnTop)
        if (!persist(updated)) return false
        report(if (updated.alwaysOnTop) "Window stays on top" else "Window no longer stays on top", error = false)
        return true
    }

    fun cycleTheme(): Boolean {
        val updated = preferences.copy(theme = preferences.theme.next())
        if (!persist(updated)) return false
        report("Theme: ${updated.theme.name.lowercase()}", error = false)
        return true
    }

    private fun persist(updated: Preferences): Boolean {
        val keep = selected?.key
        return try {
            store.save(updated)
            preferences = updated
            // Keep the same row selected when the result list changes (e.g. in Favourites).
            val position = results.indexOfFirst { it.key == keep }
            if (position >= 0) index = position
            true
        } catch (e: Exception) {
            report("Could not save preferences to ${store.file}: ${e.message ?: e.javaClass.simpleName}", error = true)
            false
        }
    }

    private fun report(message: String, error: Boolean) {
        status = message
        statusIsError = error
        statusSerial++
    }

    private fun describe(item: CatalogItem): String = when (item) {
        is CatalogItem.Command -> item.command.command
        is CatalogItem.Snippet -> "snippet “${item.snippet.title}”"
    }

    companion object {
        fun load(store: PreferencesStore, clipboard: (String) -> Unit): PlumeState {
            val (dictionary, error) = try {
                TeXDictionary.loadBundled() to null
            } catch (e: Exception) {
                TeXDictionary.EMPTY to "Could not load the command catalogue: ${e.message ?: e.javaClass.simpleName}"
            }
            return PlumeState(Catalog(dictionary), store, clipboard, error)
        }
    }
}
