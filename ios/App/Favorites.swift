// Copyright 2026 R. Heller
// SPDX-License-Identifier: AGPL-3.0-only

import Foundation
import Observation

// Tiny persistent store for favorites + recently-used commands.
// Backed by UserDefaults — small list, no need for SwiftData here.
@MainActor
@Observable
public final class Favorites {
    public private(set) var favoriteCommands: Set<String> = []
    public private(set) var recent: [String] = []
    private let favKey = "texcompanion.favorites"
    private let recentKey = "texcompanion.recent"
    private let recentMax = 20

    public init() {
        if let arr = UserDefaults.standard.array(forKey: favKey) as? [String] {
            favoriteCommands = Set(arr)
        }
        if let arr = UserDefaults.standard.array(forKey: recentKey) as? [String] {
            recent = arr
        }
    }

    public func toggle(_ command: String) {
        if favoriteCommands.contains(command) {
            favoriteCommands.remove(command)
        } else {
            favoriteCommands.insert(command)
        }
        UserDefaults.standard.set(Array(favoriteCommands), forKey: favKey)
    }

    public func recordUse(_ command: String) {
        recent.removeAll { $0 == command }
        recent.insert(command, at: 0)
        if recent.count > recentMax { recent = Array(recent.prefix(recentMax)) }
        UserDefaults.standard.set(recent, forKey: recentKey)
    }
}
