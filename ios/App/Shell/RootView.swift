// Copyright 2026 R. Heller
// SPDX-License-Identifier: AGPL-3.0-only

import SwiftUI

struct RootView: View {
    let dictionary: TeXDictionary?
    let loadError: String?

    var body: some View {
        TabView {
            DictionaryView(dictionary: dictionary, loadError: loadError)
                .tabItem { Label("Dictionary", systemImage: "magnifyingglass") }
            CheatsheetView(dictionary: dictionary)
                .tabItem { Label("Cheatsheet", systemImage: "rectangle.stack") }
            FavoritesView(dictionary: dictionary)
                .tabItem { Label("Favorites", systemImage: "star") }
            SettingsView()
                .tabItem { Label("Settings", systemImage: "gear") }
        }
    }
}
