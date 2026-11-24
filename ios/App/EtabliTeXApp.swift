// Copyright 2026 R. Heller
// SPDX-License-Identifier: AGPL-3.0-only

import SwiftUI

@main
struct EtabliTeXApp: App {

    @State private var favorites = Favorites()
    @State private var dictionary: TeXDictionary?
    @State private var loadError: String?

    init() { AppearancePreference.migrateLegacyMode(in: .standard) }

    var body: some Scene {
        WindowGroup {
            RootView(dictionary: dictionary, loadError: loadError)
                .environment(favorites)
                .etabliAppearance()
                .task {
                    switch TeXDictionary.loadFromBundle() {
                    case .success(let dict): dictionary = dict
                    case .failure(let err):  loadError = err.localizedDescription
                    }
                }
        }
    }
}
