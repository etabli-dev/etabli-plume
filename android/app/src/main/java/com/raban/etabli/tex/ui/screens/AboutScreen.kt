// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.raban.etabli.tex.EtabliTeXApplication
import com.raban.etabli.tex.ui.theme.*

@Composable
fun AboutScreen(app: EtabliTeXApplication) {
    val t = Coder.tokens
    Column(
        modifier = Modifier.fillMaxSize().background(t.color.paper)
            .verticalScroll(rememberScrollState()).padding(t.space.lg),
        verticalArrangement = Arrangement.spacedBy(t.space.lg),
    ) {
        PromptHeader(listOf("about", "plume"))
        Card(title = "Établi Plume — Android", icon = Icons.Default.Info) {
            Text("a pocket TeX dictionary + cheatsheet.", style = t.font.body)
            Row(horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                MonoLabel("commands"); MonoLabel(app.dictionary.entries.size.toString(),
                                                 color = t.color.faint)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                MonoLabel("snippets"); MonoLabel(app.dictionary.snippets.size.toString(),
                                                 color = t.color.faint)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                MonoLabel("categories"); MonoLabel(app.dictionary.categories.size.toString(),
                                                   color = t.color.faint)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                MonoLabel("version"); MonoLabel("1.0", color = t.color.faint)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                MonoLabel("data"); MonoLabel("on-device · offline", color = t.color.faint)
            }
        }
    }
}
