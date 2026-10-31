// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

import SwiftUI

struct DictionaryView: View {
    let dictionary: TeXDictionary?
    let loadError: String?

    @Environment(Favorites.self) private var favorites
    @State private var query: String = ""
    @State private var selectedCategory: String? = nil

    var body: some View {
        NavigationStack {
            ZStack {
                Theme.Color.paper.ignoresSafeArea()
                content
            }
            .navigationBarHidden(true)
        }
    }

    @ViewBuilder
    private var content: some View {
        if let err = loadError {
            ErrorState(title: "Dictionary unavailable", detail: err)
        } else if let dict = dictionary {
            loaded(dict)
        } else {
            LoadingState("loading dictionary…")
        }
    }

    private func loaded(_ dict: TeXDictionary) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Theme.Space.lg) {
                PromptHeader(["dictionary"])
                Text("TeX commands & symbols")
                    .font(Theme.Font.display).foregroundStyle(Theme.Color.ink)
                searchBar
                categoryFilter(dict)
                let filtered = dict.search(query, category: selectedCategory)
                if filtered.isEmpty {
                    EmptyState(title: "no matches", detail: "try a different name or symbol.",
                               systemImage: "magnifyingglass").frame(height: 240)
                } else {
                    list(filtered)
                }
            }.padding(Theme.Space.lg)
        }
    }

    private var searchBar: some View {
        HStack(spacing: Theme.Space.sm) {
            Image(systemName: "magnifyingglass").foregroundStyle(Theme.Color.faint)
            TextField("filter — search by name, command, or symbol description", text: $query)
                .textFieldStyle(.plain)
                .font(Theme.Font.monoBody).foregroundStyle(Theme.Color.ink)
                .autocorrectionDisabled().textInputAutocapitalization(.never)
            if !query.isEmpty {
                Button { query = "" } label: {
                    Image(systemName: "xmark.circle.fill").foregroundStyle(Theme.Color.faint)
                }.buttonStyle(.plain).accessibilityLabel("Copy \(entry.command)")
            }
        }
        .padding(Theme.Space.sm)
        .background(Theme.Color.surface)
        .overlay(RoundedRectangle(cornerRadius: Theme.Radius.sm)
            .strokeBorder(Theme.Color.hairline, lineWidth: 1))
    }

    private func categoryFilter(_ dict: TeXDictionary) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Theme.Space.sm) {
                chip("all", active: selectedCategory == nil) { selectedCategory = nil }
                ForEach(dict.categories, id: \.self) { cat in
                    chip(cat, active: selectedCategory == cat) { selectedCategory = cat }
                }
            }
        }
    }

    private func chip(_ label: String, active: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(Theme.Font.mono)
                .foregroundStyle(active ? Theme.Color.surface : Theme.Color.ink)
                .padding(.horizontal, Theme.Space.sm)
                .padding(.vertical, Theme.Space.xs)
                .background(active ? Theme.Color.accent : Theme.Color.paper)
                .overlay(RoundedRectangle(cornerRadius: Theme.Radius.sm)
                    .strokeBorder(Theme.Color.hairline, lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.sm))
        }.buttonStyle(.plain)
    }

    private func list(_ entries: [TeXCommand]) -> some View {
        Card(title: "matches  \(entries.count)", systemImage: "function") {
            VStack(spacing: 0) {
                ForEach(entries) { entry in
                    row(entry)
                    if entry.id != entries.last?.id {
                        Divider().background(Theme.Color.hairline)
                    }
                }
            }
        }
    }

    private func row(_ entry: TeXCommand) -> some View {
        HStack(alignment: .top, spacing: Theme.Space.md) {
            Text(entry.preview)
                .font(Theme.Font.title).foregroundStyle(Theme.Color.ink)
                .frame(width: 44, alignment: .leading)
            VStack(alignment: .leading, spacing: Theme.Space.xs) {
                Text(entry.command)
                    .font(Theme.Font.monoBody).foregroundStyle(Theme.Color.ink)
                    .lineLimit(1).truncationMode(.tail)
                HStack(spacing: Theme.Space.sm) {
                    MonoLabel(entry.name, color: Theme.Color.faint)
                    if let pkg = entry.`package` {
                        StatusLabel(pkg, tone: .info)
                    }
                }
                if let usage = entry.usage {
                    Text(usage)
                        .font(Theme.Font.mono)
                        .foregroundStyle(Theme.Color.faint)
                        .lineLimit(1)
                }
            }
            Spacer()
            HStack(spacing: Theme.Space.xs) {
                Button {
                    favorites.toggle(entry.command)
                    favorites.recordUse(entry.command)
                } label: {
                    Image(systemName: favorites.favoriteCommands.contains(entry.command)
                          ? "star.fill" : "star")
                        .foregroundStyle(favorites.favoriteCommands.contains(entry.command)
                                         ? Theme.Color.warn : Theme.Color.faint)
                }.buttonStyle(.plain).accessibilityLabel("Toggle favourite \(entry.command)")
                Button {
                    UIPasteboard.general.string = entry.command
                    favorites.recordUse(entry.command)
                } label: {
                    Image(systemName: "doc.on.doc").foregroundStyle(Theme.Color.accent)
                }.buttonStyle(.plain).accessibilityLabel("Copy \(entry.command)")
            }
        }.padding(.vertical, Theme.Space.sm)
    }
}
