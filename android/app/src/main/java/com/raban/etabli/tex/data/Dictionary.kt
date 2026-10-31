// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class TeXCommand(
    val command: String,
    val name: String,
    val preview: String,
    val category: String,
    val pkg: String?,
    val usage: String?,
)

data class TeXSnippet(
    val title: String,
    val category: String,
    val code: String,
)

data class TeXDictionary(
    val version: Int,
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

    companion object {
        fun loadFromAssets(ctx: Context): TeXDictionary {
            val raw = ctx.assets.open("Dictionary.json")
                .bufferedReader().use { it.readText() }
            val root = JSONObject(raw)
            return TeXDictionary(
                version = root.optInt("version", 1),
                categories = root.getJSONArray("categories").toStringList(),
                entries = root.getJSONArray("entries").mapItems { o ->
                    TeXCommand(
                        command = o.getString("command"),
                        name = o.getString("name"),
                        preview = o.getString("preview"),
                        category = o.getString("category"),
                        pkg = o.optString("package", "").ifBlank { null },
                        usage = o.optString("usage", "").ifBlank { null },
                    )
                },
                snippets = root.getJSONArray("snippets").mapItems { o ->
                    TeXSnippet(
                        title = o.getString("title"),
                        category = o.getString("category"),
                        code = o.getString("code"),
                    )
                },
            ).also { it.validate() }
        }
    }
}

private fun JSONArray.toStringList(): List<String> =
    (0 until length()).map { getString(it) }

private fun <T> JSONArray.mapItems(map: (JSONObject) -> T): List<T> =
    (0 until length()).map { map(getJSONObject(it)) }
