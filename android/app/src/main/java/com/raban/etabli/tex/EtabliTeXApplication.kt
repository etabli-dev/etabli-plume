// Copyright 2026 R. Heller
// SPDX-License-Identifier: LicenseRef-Proprietary

package com.raban.etabli.tex

import android.app.Application
import com.raban.etabli.tex.data.TeXDictionary
import com.raban.etabli.tex.prefs.Favorites

class EtabliTeXApplication : Application() {
    lateinit var dictionary: TeXDictionary
        private set
    lateinit var favorites: Favorites
        private set
    var loadError: String? = null
        private set

    override fun onCreate() {
        super.onCreate()
        favorites = Favorites(this)
        dictionary = try {
            TeXDictionary.loadFromAssets(this)
        } catch (t: Throwable) {
            loadError = t.message ?: "Failed to load Dictionary.json"
            TeXDictionary(version = 0, categories = emptyList(), entries = emptyList(), snippets = emptyList())
        }
    }
}
