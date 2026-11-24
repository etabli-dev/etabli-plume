// Copyright 2026 R. Heller
// SPDX-License-Identifier: AGPL-3.0-only

import SwiftUI

struct FavoritesView: View {
    let dictionary: TeXDictionary?
    @Environment(Favorites.self) private var favorites
    @Environment(\.themeAccent) private var accent

    private var favoriteEntries: [TeXCommand] {
        guard let dict = dictionary else { return [] }
        return dict.entries.filter { favorites.favoriteCommands.contains($0.command) }
    }
    private var recentEntries: [TeXCommand] {
        guard let dict = dictionary else { return [] }
        return favorites.recent.compactMap { cmd in
            dict.entries.first(where: { $0.command == cmd })
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Theme.Color.paper.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: Theme.Space.lg) {
                        PromptHeader(["favorites"])
                        Text("Saved commands")
                            .font(Theme.Font.display).foregroundStyle(Theme.Color.ink)
                        Card(title: "favorites", systemImage: "star.fill") {
                            if favoriteEntries.isEmpty {
                                MonoLabel("Tap the star next to a command to save it here.",
                                          color: Theme.Color.faint)
                            } else {
                                list(favoriteEntries)
                            }
                        }
                        Card(title: "recent", systemImage: "clock") {
                            if recentEntries.isEmpty {
                                MonoLabel("Recently-copied commands show up here.",
                                          color: Theme.Color.faint)
                            } else {
                                list(recentEntries)
                            }
                        }
                    }.padding(Theme.Space.lg).readableColumn()
                }
            }
            .navigationBarHidden(true)
        }
    }

    private func list(_ entries: [TeXCommand]) -> some View {
        VStack(spacing: 0) {
            ForEach(entries) { entry in
                HStack(spacing: Theme.Space.md) {
                    Text(entry.preview).font(Theme.Font.title)
                        .foregroundStyle(Theme.Color.ink).frame(width: 44, alignment: .leading)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(entry.command).font(Theme.Font.monoBody).foregroundStyle(Theme.Color.ink)
                        MonoLabel(entry.name, color: Theme.Color.faint)
                    }
                    Spacer()
                    Button {
                        UIPasteboard.general.string = entry.command
                        favorites.recordUse(entry.command)
                    } label: {
                        Image(systemName: "doc.on.doc").foregroundStyle(accent)
                    }.buttonStyle(.plain).accessibilityLabel("Copy \(entry.command)")
                }.padding(.vertical, Theme.Space.sm)
                if entry.id != entries.last?.id {
                    Divider().background(Theme.Color.hairline)
                }
            }
        }
    }
}
