// Copyright 2026 R. Heller
// SPDX-License-Identifier: AGPL-3.0-only

import SwiftUI
import SafariServices

struct SettingsView: View {
    @AppStorage(ThemePreference.userDefaultsKey) private var themeRaw: String = ThemePreference.system.rawValue
    private var theme: ThemePreference { ThemePreference(rawValue: themeRaw) ?? .system }

    @State private var safariURL: WrappedURL?

    var body: some View {
        NavigationStack {
            ZStack {
                Theme.Color.paper.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: Theme.Space.lg) {
                        PromptHeader(["settings"])
                        Text("Settings")
                            .font(Theme.Font.display).foregroundStyle(Theme.Color.ink)
                        themeCard
                        overleafCard
                        aboutCard
                    }.padding(Theme.Space.lg)
                }
            }
            .navigationBarHidden(true)
            .sheet(item: $safariURL) { wrapper in
                SafariView(url: wrapper.url).ignoresSafeArea()
            }
        }
    }

    private var themeCard: some View {
        Card(title: "appearance", systemImage: "paintbrush") {
            Picker("Theme", selection: Binding(
                get: { theme }, set: { themeRaw = $0.rawValue }
            )) {
                ForEach(ThemePreference.allCases) { p in
                    Label(p.label, systemImage: p.systemImage).tag(p)
                }
            }.pickerStyle(.segmented)
        }
    }

    private var overleafCard: some View {
        Card(title: "open in overleaf", systemImage: "arrow.up.forward.app") {
            VStack(alignment: .leading, spacing: Theme.Space.sm) {
                Text("Overleaf doesn't expose a public REST API for editing project content, so EtabliTeX stays offline-first. Tap below to open your Overleaf project list in Safari.")
                    .font(Theme.Font.body).foregroundStyle(Theme.Color.ink)
                PrimaryButton("Open overleaf.com", systemImage: "safari") {
                    safariURL = WrappedURL(url: URL(string: "https://www.overleaf.com/project")!)
                }
                MonoLabel("Git access is available on paid Overleaf plans; iOS doesn't ship a native Git client, so syncing projects on-device is queued for a future release.",
                          color: Theme.Color.faint)
            }
        }
    }

    private var aboutCard: some View {
        Card(title: "about", systemImage: "info.circle") {
            VStack(alignment: .leading, spacing: Theme.Space.sm) {
                Text("EtabliTeX")
                    .font(Theme.Font.headline).foregroundStyle(Theme.Color.ink)
                Text("Offline LaTeX dictionary and interactive cheatsheet. No analytics; no tracking; no third-party SDKs.")
                    .font(Theme.Font.body).foregroundStyle(Theme.Color.faint)
            }
        }
    }
}

private struct WrappedURL: Identifiable {
    let url: URL; var id: String { url.absoluteString }
}
private struct SafariView: UIViewControllerRepresentable {
    let url: URL
    func makeUIViewController(context: Context) -> SFSafariViewController { SFSafariViewController(url: url) }
    func updateUIViewController(_ vc: SFSafariViewController, context: Context) {}
}
