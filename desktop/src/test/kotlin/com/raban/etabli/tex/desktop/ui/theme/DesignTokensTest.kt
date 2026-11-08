// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.desktop.ui.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The desktop theme must match design/coder-design-system.json exactly. */
class DesignTokensTest {
    private val tokens = Json.parseToJsonElement(File(System.getProperty("plume.designTokens")).readText()).jsonObject

    private fun JsonObject.obj(name: String) = getValue(name).jsonObject
    private fun JsonObject.string(name: String) = getValue(name).jsonPrimitive.content
    private fun hex(value: Int) = "#%06X".format(value)

    @Test fun accentColoursMatch() {
        val color = tokens.obj("color")
        assertEquals(color.obj("accent").string("value"), hex(DesignTokens.ACCENT))
        assertEquals(color.obj("accentDark").string("value"), hex(DesignTokens.ACCENT_DARK))
        assertEquals(color.obj("accentLight").string("value"), hex(DesignTokens.ACCENT_LIGHT))
    }

    @Test fun lightAndDarkSchemesMatch() {
        val color = tokens.obj("color")
        for ((name, scheme) in listOf("light" to DesignTokens.light, "dark" to DesignTokens.dark)) {
            val expected = color.obj(name).mapValues { it.value.jsonPrimitive.content }
            val actual = mapOf(
                "background" to scheme.background, "surface" to scheme.surface, "surfaceAlt" to scheme.surfaceAlt,
                "textPrimary" to scheme.textPrimary, "textSecondary" to scheme.textSecondary,
                "border" to scheme.border, "error" to scheme.error,
            ).mapValues { hex(it.value) }
            assertEquals(expected, actual, name)
        }
    }

    @Test fun spacingRadiusAndTypeScaleMatch() {
        fun ints(obj: JsonObject) = obj.mapValues { it.value.jsonPrimitive.int }
        assertEquals(ints(tokens.obj("spacing")), DesignTokens.spacing)
        assertEquals(ints(tokens.obj("radius")), DesignTokens.radius)
        assertEquals(ints(tokens.obj("typography").obj("scale")), DesignTokens.typeScale)
    }

    @Test fun everyThemeModeIsOffered() {
        val modes = tokens.obj("theming").getValue("modes").toString()
        for (mode in com.raban.etabli.tex.desktop.prefs.ThemeMode.entries) {
            assertEquals(true, "\"${mode.name.lowercase()}\"" in modes, mode.name)
        }
    }
}
