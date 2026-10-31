// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.raban.etabli.tex.EtabliTeXApplication
import com.raban.etabli.tex.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(app: EtabliTeXApplication) {
    val t = Coder.tokens
    val scope = rememberCoroutineScope()
    val favorites by app.favorites.flow.collectAsState(initial = emptySet())
    var saveError by remember { mutableStateOf<String?>(null) }
    val items = remember(favorites, app.dictionary) {
        app.dictionary.entries.filter { it.command in favorites }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(t.color.paper).padding(t.space.lg),
        verticalArrangement = Arrangement.spacedBy(t.space.md),
    ) {
        PromptHeader(listOf("favorites", items.size.toString()))
        saveError?.let { Text(it, color = t.color.danger) }
        if (items.isEmpty()) {
            Card(title = "empty", icon = Icons.Default.Star) {
                MonoLabel("tap the star on any command to save it here.", color = t.color.faint)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(t.space.sm)) {
                items(items, key = { it.command }) { cmd ->
                    CommandRow(cmd, favorited = true, onToggleFav = {
                        scope.launch {
                            try { app.favorites.toggle(cmd.command); saveError = null }
                            catch (e: kotlinx.coroutines.CancellationException) { throw e }
                            catch (_: Exception) { saveError = "Could not save favourites. Free some storage and try again." }
                        }
                    })
                }
            }
        }
    }
}
