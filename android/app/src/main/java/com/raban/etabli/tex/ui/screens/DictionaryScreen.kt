// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raban.etabli.tex.EtabliTeXApplication
import com.raban.etabli.tex.data.TeXCommand
import com.raban.etabli.tex.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DictionaryScreen(app: EtabliTeXApplication) {
    val t = Coder.tokens
    val scope = rememberCoroutineScope()
    val favorites by app.favorites.flow.collectAsState(initial = emptySet())
    var saveError by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }

    val filtered = remember(query, category, app.dictionary) {
        app.dictionary.search(query, category)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(t.color.paper).padding(t.space.lg),
        verticalArrangement = Arrangement.spacedBy(t.space.md),
    ) {
        PromptHeader(listOf("dictionary", "${filtered.size} of ${app.dictionary.entries.size}"))
        TextInput(value = query, placeholder = "search commands…", onChange = { query = it })
        saveError?.let { Text(it, color = t.color.danger) }
        if (app.loadError != null) {
            MonoLabel("⚠ ${app.loadError}", color = t.color.danger)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(t.space.sm)) {
            item {
                Chip(label = "all", selected = category == null) { category = null }
            }
            items(app.dictionary.categories) { cat ->
                Chip(label = cat.lowercase(), selected = category == cat) {
                    category = if (category == cat) null else cat
                }
            }
        }
        if (filtered.isEmpty()) {
            EmptyState("No commands match.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(t.space.sm)) {
                items(filtered, key = { it.command }) { cmd ->
                    CommandRow(
                        cmd = cmd,
                        favorited = cmd.command in favorites,
                        onToggleFav = { scope.launch {
                            try { app.favorites.toggle(cmd.command); saveError = null }
                            catch (e: kotlinx.coroutines.CancellationException) { throw e }
                            catch (_: Exception) { saveError = "Could not save favourites. Free some storage and try again." }
                        } },
                    )
                }
            }
        }
    }
}

@Composable
fun CommandRow(cmd: TeXCommand, favorited: Boolean, onToggleFav: () -> Unit) {
    val t = Coder.tokens
    Card(title = cmd.command, icon = Icons.Default.Book) {
        Text(cmd.name, style = t.font.body)
        if (cmd.preview.isNotBlank()) {
            CodeBlock(cmd.preview)
        }
        cmd.usage?.let { MonoLabel("usage: $it", color = t.color.faint) }
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(t.space.sm),
            modifier = Modifier.fillMaxWidth()) {
            MonoLabel(cmd.category.lowercase(), color = t.color.faint)
            cmd.pkg?.let {
                MonoLabel("· $it", color = t.color.faint)
            }
            Spacer(Modifier.weight(1f))
            CopyButton("Copy command", cmd.command)
            IconButton(onClick = onToggleFav) {
                Icon(
                    imageVector = if (favorited) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (favorited) "Remove ${cmd.command} from favourites" else "Add ${cmd.command} to favourites",
                    tint = if (favorited) t.color.accent else t.color.faint,
                )
            }
        }
    }
}
