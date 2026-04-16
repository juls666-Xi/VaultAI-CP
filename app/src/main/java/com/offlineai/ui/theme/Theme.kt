package com.offlineai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────────────────────
// Brand Colours
// ──────────────────────────────────────────────────────────────

val PrimaryGreen      = Color(0xFF10A37F) // ChatGPT-inspired teal-green
val PrimaryGreenDark  = Color(0xFF0D8A6B)
val SurfaceDark       = Color(0xFF202123)
val SurfaceMid        = Color(0xFF343541)
val SurfaceLight      = Color(0xFFF7F7F8)
val BubbleUser        = Color(0xFF10A37F)
val BubbleAi          = Color(0xFFFFFFFF)
val BubbleAiDark      = Color(0xFF444654)
val OnPrimaryWhite    = Color(0xFFFFFFFF)

// ──────────────────────────────────────────────────────────────
// Colour Schemes
// ──────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary          = PrimaryGreen,
    onPrimary        = OnPrimaryWhite,
    primaryContainer = PrimaryGreenDark,
    background       = SurfaceDark,
    surface          = SurfaceMid,
    onSurface        = Color(0xFFECECF1),
    onBackground     = Color(0xFFECECF1),
    outline          = Color(0xFF565869),
    surfaceVariant   = Color(0xFF40414F)
)

private val LightColorScheme = lightColorScheme(
    primary          = PrimaryGreen,
    onPrimary        = OnPrimaryWhite,
    primaryContainer = Color(0xFFD1FAE5),
    background       = SurfaceLight,
    surface          = Color(0xFFFFFFFF),
    onSurface        = Color(0xFF1A1A2E),
    onBackground     = Color(0xFF1A1A2E),
    outline          = Color(0xFFD1D5DB),
    surfaceVariant   = Color(0xFFF3F4F6)
)

// ──────────────────────────────────────────────────────────────
// Typography
// ──────────────────────────────────────────────────────────────

val OfflineAiTypography = Typography()   // Uses Material3 defaults (Roboto)

// ──────────────────────────────────────────────────────────────
// Theme Composable
// ──────────────────────────────────────────────────────────────

/**
 * The root Compose theme for the app.
 *
 * @param darkTheme  If true, uses the dark colour scheme.
 *                   Defaults to the system preference, but can be
 *                   overridden from the Settings screen.
 */
@Composable
fun OfflineAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = OfflineAiTypography,
        content     = content
    )
}
