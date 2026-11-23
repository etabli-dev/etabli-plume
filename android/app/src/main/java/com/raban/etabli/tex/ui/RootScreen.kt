// Copyright 2026 R. Heller
// SPDX-License-Identifier: AGPL-3.0-only

package com.raban.etabli.tex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.raban.etabli.tex.EtabliTeXApplication
import com.raban.etabli.tex.ui.screens.AboutScreen
import com.raban.etabli.tex.ui.screens.CheatsheetScreen
import com.raban.etabli.tex.ui.screens.DictionaryScreen
import com.raban.etabli.tex.ui.screens.FavoritesScreen
import com.raban.etabli.tex.ui.theme.Coder

@Composable
fun RootScreen(app: EtabliTeXApplication) {
    val t = Coder.tokens
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = t.color.paper,
        bottomBar = {
            NavigationBar(containerColor = t.color.surface) {
                listOf(
                    Triple("Dictionary", Icons.Default.Book, 0),
                    Triple("Cheatsheet", Icons.Default.Code, 1),
                    Triple("Favorites",  Icons.Default.Star, 2),
                    Triple("About",      Icons.Default.Info, 3),
                ).forEach { (label, icon, idx) ->
                    NavigationBarItem(
                        selected = tab == idx,
                        onClick = { tab = idx },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, style = t.font.mono) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = t.color.accent,
                            selectedTextColor = t.color.accent,
                            indicatorColor = t.color.accentMuted,
                            unselectedIconColor = t.color.faint,
                            unselectedTextColor = t.color.faint,
                        ),
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(t.color.paper)) {
            when (tab) {
                0 -> DictionaryScreen(app)
                1 -> CheatsheetScreen(app)
                2 -> FavoritesScreen(app)
                else -> AboutScreen(app)
            }
        }
    }
}
