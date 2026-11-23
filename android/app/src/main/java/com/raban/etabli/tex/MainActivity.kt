// Copyright 2026 R. Heller
// SPDX-License-Identifier: AGPL-3.0-only

package com.raban.etabli.tex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.raban.etabli.tex.ui.RootScreen
import com.raban.etabli.tex.ui.theme.CoderTheme

class MainActivity : ComponentActivity() {
    private val app get() = application as EtabliTeXApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoderTheme { RootScreen(app) }
        }
    }
}
