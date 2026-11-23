// Copyright 2026 R. Heller
// SPDX-License-Identifier: AGPL-3.0-only

package com.raban.etabli.tex.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favStore by preferencesDataStore(name = "tex_favorites")
private val FAV_KEY = stringSetPreferencesKey("commands")

class Favorites(private val context: Context) {
    val flow: Flow<Set<String>> = context.favStore.data.map { it[FAV_KEY] ?: emptySet() }

    suspend fun toggle(command: String) {
        context.favStore.edit { p ->
            val current = p[FAV_KEY] ?: emptySet()
            p[FAV_KEY] = if (command in current) current - command else current + command
        }
    }
}
