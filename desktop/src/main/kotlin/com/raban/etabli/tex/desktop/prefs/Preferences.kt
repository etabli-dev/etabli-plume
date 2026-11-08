// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.desktop.prefs

import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class ThemeMode {
    @SerialName("system") System,
    @SerialName("light") Light,
    @SerialName("dark") Dark,
    ;

    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]
}

/**
 * Everything the desktop app remembers. Favourites are command sources such
 * as `\alpha`, the same keys Android (DataStore) and iOS (UserDefaults) use.
 */
@Serializable
data class Preferences(
    val favourites: Set<String> = emptySet(),
    val alwaysOnTop: Boolean = false,
    val theme: ThemeMode = ThemeMode.System,
)

/** Small JSON file in the per-user configuration directory; written atomically. */
class PreferencesStore(val file: Path) {
    /** Set when an existing file could not be read; the app then starts from defaults. */
    var loadError: String? = null
        private set

    private var unreadableFile = false

    fun load(): Preferences {
        if (!Files.exists(file)) return Preferences()
        return try {
            json.decodeFromString(Preferences.serializer(), Files.readString(file))
        } catch (e: Exception) {
            unreadableFile = true
            loadError = "Could not read saved preferences; starting with defaults."
            Preferences()
        }
    }

    /** Throws IOException when the file cannot be written. */
    fun save(preferences: Preferences) {
        Files.createDirectories(file.parent)
        if (unreadableFile && Files.exists(file)) {
            // Keep the unreadable file for inspection instead of overwriting it.
            Files.move(file, file.resolveSibling("${file.fileName}.corrupt"), StandardCopyOption.REPLACE_EXISTING)
        }
        unreadableFile = false
        val sorted = preferences.copy(favourites = preferences.favourites.toSortedSet())
        val temp = Files.createTempFile(file.parent, "${file.fileName}.", ".tmp")
        try {
            Files.writeString(temp, json.encodeToString(Preferences.serializer(), sorted) + "\n")
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: IOException) {
            Files.deleteIfExists(temp)
            throw e
        }
    }

    companion object {
        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        const val FILE_NAME = "preferences.json"

        /** Environment variable that overrides the directory (tests, portable use). */
        const val DIR_OVERRIDE = "ETABLI_PLUME_CONFIG_DIR"

        fun forCurrentUser(): PreferencesStore = PreferencesStore(
            configDir(System.getenv(), System.getProperty("os.name"), System.getProperty("user.home"))
                .resolve(FILE_NAME),
        )

        fun configDir(env: Map<String, String>, osName: String, home: String): Path {
            env[DIR_OVERRIDE]?.takeIf { it.isNotBlank() }?.let { return Path.of(it) }
            val os = osName.lowercase()
            return when {
                os.startsWith("windows") ->
                    Path.of(env["APPDATA"]?.takeIf { it.isNotBlank() } ?: Path.of(home, "AppData", "Roaming").toString(), "Etabli Plume")
                os.startsWith("mac") ->
                    Path.of(home, "Library", "Application Support", "com.raban.etabli.tex")
                else ->
                    // XDG: a relative XDG_CONFIG_HOME is invalid and must be ignored.
                    Path.of(env["XDG_CONFIG_HOME"]?.takeIf { it.isNotBlank() && Path.of(it).isAbsolute }
                        ?: Path.of(home, ".config").toString(), "etabli-plume")
            }
        }
    }
}
