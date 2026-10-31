// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

import Foundation

// Decoded form of Resources/Dictionary.json. Loaded once at launch.
public struct TeXCommand: Codable, Identifiable, Hashable, Sendable {
    public var id: String { command }   // commands are unique within the JSON
    public let command: String
    public let name: String
    public let preview: String
    public let category: String
    public let `package`: String?
    public let usage: String?
}

public struct TeXSnippet: Codable, Identifiable, Hashable, Sendable {
    public var id: String { title }
    public let title: String
    public let category: String
    public let code: String
}

public struct TeXDictionary: Codable, Sendable {
    public let version: Int
    public let categories: [String]
    public let entries: [TeXCommand]
    public let snippets: [TeXSnippet]

    /// Loaded from the bundled JSON. If decoding fails we return an empty
    /// dictionary and surface the error in the UI — we don't crash.
    public static func loadFromBundle() -> Result<TeXDictionary, Error> {
        guard let url = Bundle.main.url(forResource: "Dictionary", withExtension: "json") else {
            return .failure(LoadError.missing)
        }
        do {
            let data = try Data(contentsOf: url)
            let decoded = try JSONDecoder().decode(TeXDictionary.self, from: data)
            try decoded.validate()
            return .success(decoded)
        } catch {
            return .failure(error)
        }
    }

    public func validate() throws {
        guard version > 0, !categories.isEmpty, Set(categories).count == categories.count,
              Set(entries.map(\.command)).count == entries.count,
              Set(snippets.map(\.title)).count == snippets.count,
              entries.allSatisfy({ !$0.command.isEmpty && !$0.name.isEmpty && categories.contains($0.category) }),
              snippets.allSatisfy({ !$0.title.isEmpty && !$0.category.isEmpty && !$0.code.isEmpty }) else {
            throw LoadError.invalid
        }
    }

    public func search(_ query: String, category: String? = nil) -> [TeXCommand] {
        let term = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return entries.filter { entry in
            (category == nil || entry.category == category) && (term.isEmpty ||
                [entry.command, entry.name, entry.preview, entry.category, entry.package ?? "", entry.usage ?? ""]
                    .contains(where: { $0.lowercased().contains(term) }))
        }
    }

    public enum LoadError: LocalizedError {
        case missing, invalid
        public var errorDescription: String? {
            self == .missing ? "Dictionary.json missing from app bundle." : "Dictionary.json has invalid or duplicate entries."
        }
    }
}
