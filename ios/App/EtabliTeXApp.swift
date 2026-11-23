// Copyright 2026 R. Heller
// SPDX-License-Identifier: AGPL-3.0-only

import SwiftUI

@main
struct EtabliTeXApp: App {

    @AppStorage(ThemePreference.userDefaultsKey) private var themeRaw: String = ThemePreference.system.rawValue
    @State private var favorites = Favorites()
    @State private var dictionary: TeXDictionary?
    @State private var loadError: String?

    private var theme: ThemePreference { ThemePreference(rawValue: themeRaw) ?? .system }

    var body: some Scene {
        WindowGroup {
            RootView(dictionary: dictionary, loadError: loadError)
                .environment(favorites)
                .preferredColorScheme(theme.colorScheme)
                .tint(Theme.Color.accent)
                .task {
                    switch TeXDictionary.loadFromBundle() {
                    case .success(let dict): dictionary = dict
                    case .failure(let err):  loadError = err.localizedDescription
                    }
                }
        }
    }
}
