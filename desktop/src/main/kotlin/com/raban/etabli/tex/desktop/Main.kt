// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.raban.etabli.tex.desktop.prefs.PreferencesStore
import com.raban.etabli.tex.desktop.ui.PlumeScreen
import com.raban.etabli.tex.desktop.ui.UiFocus
import com.raban.etabli.tex.desktop.ui.handleWindowKey
import com.raban.etabli.tex.desktop.ui.theme.PlumeTheme
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import org.jetbrains.skia.Image

const val APP_NAME = "Établi Plume"

/** Set by jpackage launchers; absent when started from Gradle. */
val appVersion: String = System.getProperty("jpackage.app-version") ?: "dev"

fun main() = application {
    val state = remember { PlumeState.load(PreferencesStore.forCurrentUser(), ::copyToSystemClipboard) }
    val ui = remember { UiFocus() }
    Window(
        onCloseRequest = ::exitApplication,
        title = APP_NAME,
        icon = remember { appIcon() },
        state = rememberWindowState(width = 1120.dp, height = 720.dp),
        alwaysOnTop = state.preferences.alwaysOnTop,
        onPreviewKeyEvent = { event -> handleWindowKey(event, state, ui) },
    ) {
        LaunchedEffect(Unit) { window.minimumSize = Dimension(760, 480) }
        PlumeTheme(state.preferences.theme) {
            PlumeScreen(state, ui)
        }
    }
}

private fun copyToSystemClipboard(text: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}

private fun appIcon(): Painter? =
    PlumeState::class.java.classLoader.getResourceAsStream("icon-plume.png")?.use { stream ->
        BitmapPainter(Image.makeFromEncoded(stream.readBytes()).toComposeImageBitmap())
    }
