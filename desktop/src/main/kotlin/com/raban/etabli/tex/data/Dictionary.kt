// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Desktop twin of android/app/src/main/java/com/raban/etabli/tex/data/Dictionary.kt.
// Same names, validation rules and search semantics, decoded with
// kotlinx.serialization so the code can move unchanged into a shared module.

@Serializable
data class TeXCommand(
    val command: String,
    val name: String,
    val preview: String,
    val category: String,
    @SerialName("package") val pkg: String? = null,
    val usage: String? = null,
)

@Serializable
data class TeXSnippet(
    val title: String,
    val category: String,
    val code: String,
)

@Serializable
data class TeXDictionary(
    val version: Int = 1,
    val categories: List<String>,
    val entries: List<TeXCommand>,
    val snippets: List<TeXSnippet>,
) {
    fun validate() {
        require(version > 0) { "Unsupported dictionary version" }
        require(categories.isNotEmpty() && categories.distinct().size == categories.size) { "Invalid dictionary categories" }
        require(entries.map { it.command }.distinct().size == entries.size) { "Duplicate dictionary command" }
        require(snippets.map { it.title }.distinct().size == snippets.size) { "Duplicate snippet title" }
        require(entries.all { it.command.isNotBlank() && it.name.isNotBlank() && it.category in categories }) { "Incomplete dictionary entry" }
        require(snippets.all { it.title.isNotBlank() && it.category.isNotBlank() && it.code.isNotBlank() }) { "Incomplete snippet" }
    }

    fun search(query: String, category: String? = null): List<TeXCommand> {
        val term = query.trim()
        return entries.filter { entry ->
            (category == null || entry.category == category) &&
                (term.isEmpty() || listOf(entry.command, entry.name, entry.preview, entry.category,
                    entry.pkg.orEmpty(), entry.usage.orEmpty()).any { it.contains(term, ignoreCase = true) })
        }
    }

    fun searchSnippets(query: String): List<TeXSnippet> {
        val term = query.trim()
        return snippets.filter { snippet ->
            term.isEmpty() || listOf(snippet.title, snippet.category, snippet.code)
                .any { it.contains(term, ignoreCase = true) }
        }
    }

    companion object {
        /** Classpath name of the bundled catalogue (the Android asset, packaged by Gradle). */
        const val RESOURCE = "Dictionary.json"

        private val json = Json { ignoreUnknownKeys = true }

        val EMPTY = TeXDictionary(version = 0, categories = emptyList(), entries = emptyList(), snippets = emptyList())

        /** Decodes and validates catalogue JSON; throws IllegalArgumentException on bad input. */
        fun parse(text: String): TeXDictionary {
            val decoded = json.decodeFromString(serializer(), text)
            // Android maps blank optional fields to null; keep the same model.
            return decoded.copy(
                entries = decoded.entries.map { entry ->
                    entry.copy(pkg = entry.pkg?.ifBlank { null }, usage = entry.usage?.ifBlank { null })
                },
            ).also { it.validate() }
        }

        fun loadBundled(loader: ClassLoader = TeXDictionary::class.java.classLoader): TeXDictionary {
            val stream = loader.getResourceAsStream(RESOURCE)
                ?: throw IllegalStateException("$RESOURCE is missing from the application")
            return parse(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        }
    }
}
