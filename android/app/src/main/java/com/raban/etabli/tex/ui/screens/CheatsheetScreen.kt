// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.raban.etabli.tex.EtabliTeXApplication
import com.raban.etabli.tex.data.TeXSnippet
import com.raban.etabli.tex.ui.theme.*

@Composable
fun CheatsheetScreen(app: EtabliTeXApplication) {
    val t = Coder.tokens
    var category by remember { mutableStateOf<String?>(null) }

    val cats = remember(app.dictionary) { app.dictionary.snippets.map { it.category }.distinct() }
    val filtered = remember(category, app.dictionary) {
        if (category == null) app.dictionary.snippets else app.dictionary.snippets.filter { it.category == category }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(t.color.paper).padding(t.space.lg),
        verticalArrangement = Arrangement.spacedBy(t.space.md),
    ) {
        PromptHeader(listOf("cheatsheet", "${filtered.size}"))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(t.space.sm)) {
            item { Chip(label = "all", selected = category == null) { category = null } }
            items(cats) { c -> Chip(label = c.lowercase(), selected = category == c) {
                category = if (category == c) null else c
            } }
        }
        if (filtered.isEmpty()) {
            EmptyState("No snippets.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(t.space.sm)) {
                items(filtered, key = { it.title }) { s -> SnippetCard(s) }
            }
        }
    }
}

@Composable
private fun SnippetCard(s: TeXSnippet) {
    val t = Coder.tokens
    Card(title = s.title, icon = Icons.Default.Code) {
        MonoLabel(s.category.lowercase(), color = t.color.faint)
        CodeBlock(s.code)
        CopyButton("Copy snippet", s.code)
    }
}
