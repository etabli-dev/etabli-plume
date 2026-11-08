// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raban.etabli.tex.desktop.prefs.ThemeMode

// Coder Design System for the desktop app. The numbers below are the values of
// design/coder-design-system.json; DesignTokensTest fails when they drift.

data class SchemeTokens(
    val background: Int,
    val surface: Int,
    val surfaceAlt: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val border: Int,
    val error: Int,
)

object DesignTokens {
    const val ACCENT = 0x28A745
    const val ACCENT_DARK = 0x1E7E34
    const val ACCENT_LIGHT = 0x48C76A

    val light = SchemeTokens(
        background = 0xFFFFFF, surface = 0xF7F8FA, surfaceAlt = 0xEDEFF2,
        textPrimary = 0x1A1C1E, textSecondary = 0x5A5F66, border = 0xD9DCE1, error = 0xD32F2F,
    )
    val dark = SchemeTokens(
        background = 0x121417, surface = 0x1A1D21, surfaceAlt = 0x22262B,
        textPrimary = 0xF2F4F6, textSecondary = 0xA6ACB3, border = 0x33373D, error = 0xEF5350,
    )

    /** typography.scale, in points. */
    val typeScale = mapOf(
        "caption" to 12, "body" to 15, "callout" to 17,
        "title3" to 20, "title2" to 24, "title1" to 30, "largeTitle" to 36,
    )

    /** spacing, in dp. */
    val spacing = mapOf("xs" to 4, "sm" to 8, "md" to 16, "lg" to 24, "xl" to 32, "xxl" to 48)

    /** radius, in dp (pill = 999). */
    val radius = mapOf("sm" to 6, "md" to 10, "lg" to 16, "pill" to 999)
}

private fun rgb(value: Int) = Color(0xFF000000 or value.toLong())

data class PlumePalette(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val text: Color,
    val faint: Color,
    val border: Color,
    val error: Color,
    /** Fills: selection bar, primary buttons. */
    val accent: Color,
    /** Accent used for text and glyphs on the background. */
    val accentText: Color,
    val accentMuted: Color,
)

data class PlumeSpace(
    val xs: Dp = DesignTokens.spacing.getValue("xs").dp,
    val sm: Dp = DesignTokens.spacing.getValue("sm").dp,
    val md: Dp = DesignTokens.spacing.getValue("md").dp,
    val lg: Dp = DesignTokens.spacing.getValue("lg").dp,
    val xl: Dp = DesignTokens.spacing.getValue("xl").dp,
)

data class PlumeRadius(
    val sm: Dp = DesignTokens.radius.getValue("sm").dp,
    val md: Dp = DesignTokens.radius.getValue("md").dp,
    val lg: Dp = DesignTokens.radius.getValue("lg").dp,
)

/** Monospace for commands, labels and numbers; system UI font for prose. */
data class PlumeType(
    val caption: TextStyle,
    val body: TextStyle,
    val mono: TextStyle,
    val monoCallout: TextStyle,
    val title: TextStyle,
    val glyph: TextStyle,
)

data class PlumeTokens(
    val color: PlumePalette,
    val type: PlumeType,
    val dark: Boolean,
    val space: PlumeSpace = PlumeSpace(),
    val radius: PlumeRadius = PlumeRadius(),
)

val LocalPlume = staticCompositionLocalOf<PlumeTokens> { error("PlumeTheme not applied") }

object Plume {
    val tokens: PlumeTokens
        @Composable get() = LocalPlume.current
}

fun palette(dark: Boolean): PlumePalette {
    val scheme = if (dark) DesignTokens.dark else DesignTokens.light
    val accent = rgb(DesignTokens.ACCENT)
    return PlumePalette(
        background = rgb(scheme.background),
        surface = rgb(scheme.surface),
        surfaceAlt = rgb(scheme.surfaceAlt),
        text = rgb(scheme.textPrimary),
        faint = rgb(scheme.textSecondary),
        border = rgb(scheme.border),
        error = rgb(scheme.error),
        accent = accent,
        accentText = if (dark) rgb(DesignTokens.ACCENT_LIGHT) else rgb(DesignTokens.ACCENT_DARK),
        accentMuted = accent.copy(alpha = if (dark) 0.22f else 0.12f),
    )
}

private fun type(p: PlumePalette): PlumeType {
    val scale = DesignTokens.typeScale
    return PlumeType(
        caption = TextStyle(fontFamily = FontFamily.Monospace, fontSize = scale.getValue("caption").sp, color = p.faint),
        body = TextStyle(fontFamily = FontFamily.Default, fontSize = scale.getValue("body").sp, color = p.text),
        mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = scale.getValue("body").sp, color = p.text),
        monoCallout = TextStyle(fontFamily = FontFamily.Monospace, fontSize = scale.getValue("callout").sp, color = p.text),
        title = TextStyle(
            fontFamily = FontFamily.Monospace, fontSize = scale.getValue("title3").sp,
            fontWeight = FontWeight.SemiBold, color = p.text,
        ),
        glyph = TextStyle(fontFamily = FontFamily.Default, fontSize = 56.sp, color = p.text),
    )
}

@Composable
fun PlumeTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val p = palette(dark)
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = Color.White, background = p.background, onBackground = p.text,
            surface = p.surface, onSurface = p.text, outline = p.border, error = p.error,
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = Color.White, background = p.background, onBackground = p.text,
            surface = p.surface, onSurface = p.text, outline = p.border, error = p.error,
        )
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalPlume provides PlumeTokens(color = p, type = type(p), dark = dark), content = content)
    }
}
