// Copyright 2026 R. Heller
// SPDX-License-Identifier: LicenseRef-Proprietary

import SwiftUI

struct CheatsheetView: View {
    let dictionary: TeXDictionary?

    var body: some View {
        NavigationStack {
            ZStack {
                Theme.Color.paper.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: Theme.Space.lg) {
                        PromptHeader(["cheatsheet"])
                        Text("Boilerplate blocks")
                            .font(Theme.Font.display).foregroundStyle(Theme.Color.ink)
                        MonoLabel("tap any block to copy it.", color: Theme.Color.faint)
                        if let dict = dictionary {
                            content(dict)
                        } else {
                            LoadingState("loading…")
                        }
                    }.padding(Theme.Space.lg)
                }
            }
            .navigationBarHidden(true)
        }
    }

    private func content(_ dict: TeXDictionary) -> some View {
        let grouped = Dictionary(grouping: dict.snippets) { $0.category }
        // Preserve the order categories first appear in the JSON.
        var seen: Set<String> = []
        var ordered: [String] = []
        for s in dict.snippets where !seen.contains(s.category) {
            seen.insert(s.category); ordered.append(s.category)
        }
        return VStack(spacing: Theme.Space.lg) {
            ForEach(ordered, id: \.self) { cat in
                Card(title: cat, systemImage: "rectangle.stack") {
                    VStack(spacing: Theme.Space.md) {
                        ForEach(grouped[cat] ?? []) { snippet in
                            SnippetBlock(snippet: snippet)
                        }
                    }
                }
            }
        }
    }
}

private struct SnippetBlock: View {
    let snippet: TeXSnippet
    @State private var copied = false

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Space.xs) {
            HStack {
                Text(snippet.title)
                    .font(Theme.Font.headline).foregroundStyle(Theme.Color.ink)
                Spacer()
                Button {
                    UIPasteboard.general.string = snippet.code
                    withAnimation { copied = true }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                        withAnimation { copied = false }
                    }
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: copied ? "checkmark" : "doc.on.doc")
                        Text(copied ? "copied" : "copy").font(Theme.Font.mono)
                    }
                    .foregroundStyle(copied ? Theme.Color.accent : Theme.Color.faint)
                }.buttonStyle(.plain)
            }
            Text(snippet.code)
                .font(Theme.Font.mono).foregroundStyle(Theme.Color.ink)
                .textSelection(.enabled)
                .padding(Theme.Space.sm)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Theme.Color.paper)
                .overlay(RoundedRectangle(cornerRadius: Theme.Radius.sm)
                    .strokeBorder(Theme.Color.hairline, lineWidth: 1))
        }
    }
}
