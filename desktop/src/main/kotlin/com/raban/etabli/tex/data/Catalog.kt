// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.data

// Platform-neutral browsing rules for the desktop app: sections, combined
// command/snippet results and match ranking. No UI or JVM desktop imports.

/** One row of the result list: a command or a snippet. */
sealed interface CatalogItem {
    val key: String
    val title: String
    val category: String

    /** Exact source placed on the clipboard. */
    val source: String

    data class Command(val command: TeXCommand) : CatalogItem {
        override val key get() = "command:${command.command}"
        override val title get() = command.name
        override val category get() = command.category
        override val source get() = command.command
    }

    data class Snippet(val snippet: TeXSnippet) : CatalogItem {
        override val key get() = "snippet:${snippet.title}"
        override val title get() = snippet.title
        override val category get() = snippet.category
        override val source get() = snippet.code
    }
}

/** Entries of the section list. */
sealed interface Section {
    val label: String

    data object All : Section {
        override val label = "All"
    }

    data object Favourites : Section {
        override val label = "Favourites"
    }

    data class Category(val name: String) : Section {
        override val label get() = name
    }

    data object Snippets : Section {
        override val label = "Snippets"
    }
}

class Catalog(val dictionary: TeXDictionary) {
    val sections: List<Section> =
        listOf(Section.All, Section.Favourites) +
            dictionary.categories.map { Section.Category(it) } +
            Section.Snippets

    val size: Int get() = dictionary.entries.size + dictionary.snippets.size

    /**
     * Items of [section] that match [query], best matches first. [favourites]
     * holds command sources, the same keys the Android and iOS apps store.
     */
    fun items(section: Section, query: String, favourites: Set<String>): List<CatalogItem> {
        val commands: List<TeXCommand> = when (section) {
            Section.All -> dictionary.search(query)
            Section.Favourites -> dictionary.search(query).filter { it.command in favourites }
            is Section.Category -> dictionary.search(query, section.name)
            Section.Snippets -> emptyList()
        }
        val snippets: List<TeXSnippet> = when (section) {
            Section.All, Section.Snippets -> dictionary.searchSnippets(query)
            else -> emptyList()
        }
        val items = commands.map { CatalogItem.Command(it) } + snippets.map { CatalogItem.Snippet(it) }
        return rank(items, query)
    }

    fun count(section: Section, favourites: Set<String>): Int = items(section, "", favourites).size

    companion object {
        /**
         * Stable ranking: exact name/source matches first, then prefixes, then
         * the remaining substring matches in catalogue order. A leading
         * backslash in the query is optional.
         */
        fun rank(items: List<CatalogItem>, query: String): List<CatalogItem> {
            val term = query.trim().lowercase().removePrefix("\\")
            if (term.isEmpty()) return items
            return items.sortedBy { item ->
                val title = item.title.lowercase()
                val source = item.source.lowercase().removePrefix("\\")
                when {
                    title == term || source == term -> 0
                    title.startsWith(term) || source.startsWith(term) -> 1
                    else -> 2
                }
            }
        }
    }
}
