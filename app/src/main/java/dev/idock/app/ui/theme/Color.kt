package dev.idock.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * The palette comes from what the app is about: a cable.
 *   copper  – the conductor            → primary (actions, the cable itself)
 *   patina  – oxidised copper          → secondary ("connected", "matches")
 *   brass   – connector contacts       → tertiary (warnings, the far end of the copper sheen)
 *   ink / paper – a printed box label  → neutrals; the dark theme is true black
 * Every text/background pair used by the app is at least 4.5:1.
 */

internal val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFA44A1C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFBE3D4),
    onPrimaryContainer = Color(0xFF4A1B05),
    inversePrimary = Color(0xFFF2874A),
    secondary = Color(0xFF1F7A66),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5EEE6),
    onSecondaryContainer = Color(0xFF0B3B31),
    tertiary = Color(0xFFA0660A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6E4BF),
    onTertiaryContainer = Color(0xFF3D2800),
    background = Color(0xFFF2F3F1),
    onBackground = Color(0xFF171917),
    surface = Color(0xFFF2F3F1),
    onSurface = Color(0xFF171917),
    surfaceVariant = Color(0xFFE4E6E2),
    onSurfaceVariant = Color(0xFF5A5F58),
    surfaceTint = Color(0xFFA44A1C),
    inverseSurface = Color(0xFF2C2F2C),
    inverseOnSurface = Color(0xFFF0F1EE),
    error = Color(0xFFB3213F),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBDCE1),
    onErrorContainer = Color(0xFF4A0A17),
    outline = Color(0xFF7B7F78),
    outlineVariant = Color(0xFFD5D8D2),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDCDEDA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9FAF8),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFECEEEA),
    surfaceContainerHighest = Color(0xFFE4E6E2),
)

internal val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFF2874A),
    onPrimary = Color(0xFF2A1203),
    primaryContainer = Color(0xFF4A2410),
    onPrimaryContainer = Color(0xFFFFD9C2),
    inversePrimary = Color(0xFFA44A1C),
    secondary = Color(0xFF6FD3B8),
    onSecondary = Color(0xFF00382D),
    secondaryContainer = Color(0xFF123F35),
    onSecondaryContainer = Color(0xFFA9EBD9),
    tertiary = Color(0xFFE9B455),
    onTertiary = Color(0xFF3A2600),
    tertiaryContainer = Color(0xFF4A3608),
    onTertiaryContainer = Color(0xFFFFE2A6),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2EF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF2F2EF),
    surfaceVariant = Color(0xFF272A27),
    onSurfaceVariant = Color(0xFFA6AAA4),
    surfaceTint = Color(0xFFF2874A),
    inverseSurface = Color(0xFFE6E7E3),
    inverseOnSurface = Color(0xFF1B1D1B),
    error = Color(0xFFFF8FA0),
    onError = Color(0xFF4A0A17),
    errorContainer = Color(0xFF5A1322),
    onErrorContainer = Color(0xFFFFD9DE),
    outline = Color(0xFF6B6F69),
    outlineVariant = Color(0xFF30332F),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2A2D2A),
    surfaceDim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0D0E0D),
    surfaceContainer = Color(0xFF151715),
    surfaceContainerHigh = Color(0xFF1F221F),
    surfaceContainerHighest = Color(0xFF2A2D2A),
)

/** Colours outside the Material roles: the printed device label and the floating navigation bar. */
@Immutable
data class DockColors(
    val labelPaper: Color,
    val labelInk: Color,
    val labelInkSoft: Color,
    val barContainer: Color,
)

internal val LightDock = DockColors(
    labelPaper = Color(0xFFFFFFFF),
    labelInk = Color(0xFF171917),
    labelInkSoft = Color(0xFF5A5F58),
    barContainer = Color(0xFFFFFFFF),
)

// The label stays paper-coloured on the black theme, like a real sticker on a dark box.
internal val DarkDock = DockColors(
    labelPaper = Color(0xFFEDEAE2),
    labelInk = Color(0xFF171917),
    labelInkSoft = Color(0xFF55584F),
    barContainer = Color(0xFF1A1C1A),
)

val LocalDockColors = staticCompositionLocalOf { LightDock }
