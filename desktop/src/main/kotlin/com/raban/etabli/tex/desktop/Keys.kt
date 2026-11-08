// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.desktop

import androidx.compose.ui.input.key.Key

val isMacOs: Boolean = System.getProperty("os.name").orEmpty().lowercase().startsWith("mac")

/** Labels used in hints: Cmd/Option on macOS, Ctrl/Alt elsewhere. */
val primaryLabel: String = if (isMacOs) "⌘" else "Ctrl+"
val altLabel: String = if (isMacOs) "⌥" else "Alt+"

enum class KeyAction {
    FocusSearch, ToggleFavourite, ToggleOnTop, PreviousSection, NextSection,
    Escape, Down, Up, PageDown, PageUp, First, Last, Copy, CopyUsage,
}

/**
 * Keyboard map of the window. [primary] is Cmd on macOS and Ctrl elsewhere.
 * List navigation and Enter apply while the search field has focus, which it
 * has on start and after every pointer action.
 */
fun keyAction(key: Key, primary: Boolean, alt: Boolean, shift: Boolean, searchFocused: Boolean): KeyAction? {
    if (primary && !alt) {
        return when (key) {
            Key.F, Key.L -> KeyAction.FocusSearch
            Key.D -> KeyAction.ToggleFavourite
            Key.T -> KeyAction.ToggleOnTop
            Key.DirectionUp -> if (searchFocused) KeyAction.First else null
            Key.DirectionDown -> if (searchFocused) KeyAction.Last else null
            else -> null
        }
    }
    if (alt && !primary) {
        return when (key) {
            Key.DirectionUp -> KeyAction.PreviousSection
            Key.DirectionDown -> KeyAction.NextSection
            else -> null
        }
    }
    if (primary || alt) return null
    if (key == Key.Escape) return KeyAction.Escape
    if (!searchFocused) return null
    return when (key) {
        Key.DirectionDown -> KeyAction.Down
        Key.DirectionUp -> KeyAction.Up
        Key.PageDown -> KeyAction.PageDown
        Key.PageUp -> KeyAction.PageUp
        Key.Enter, Key.NumPadEnter -> if (shift) KeyAction.CopyUsage else KeyAction.Copy
        else -> null
    }
}

/** Applies [action]; returns true when the key event is consumed. */
fun PlumeState.perform(action: KeyAction, focusSearch: () -> Unit): Boolean {
    when (action) {
        KeyAction.FocusSearch -> focusSearch()
        KeyAction.ToggleFavourite -> toggleFavourite()
        KeyAction.ToggleOnTop -> toggleAlwaysOnTop()
        KeyAction.PreviousSection -> moveSection(-1)
        KeyAction.NextSection -> moveSection(1)
        KeyAction.Escape -> {
            clearQuery()
            focusSearch()
        }
        KeyAction.Down -> moveSelection(1)
        KeyAction.Up -> moveSelection(-1)
        KeyAction.PageDown -> moveSelection(PAGE)
        KeyAction.PageUp -> moveSelection(-PAGE)
        KeyAction.First -> selectFirst()
        KeyAction.Last -> selectLast()
        KeyAction.Copy -> copySelected()
        KeyAction.CopyUsage -> copySelectedUsage()
    }
    return true
}

private const val PAGE = 10
