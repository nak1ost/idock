package dev.idock.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.sp
import dev.idock.app.R

/*
 * One superfamily in two widths (Sofia Sans, SIL Open Font License, full Cyrillic).
 * The condensed cut carries headlines and numbers: Russian words are long, and a narrow face
 * lets them be set large without wrapping, the way they are on a box label. The regular
 * width is for reading.
 */

@OptIn(ExperimentalTextApi::class)
private fun text(weight: Int) = Font(
    resId = R.font.sofia_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

@OptIn(ExperimentalTextApi::class)
private fun condensed(weight: Int) = Font(
    resId = R.font.sofia_sans_condensed,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

// Sofia Sans draws Bulgarian letterforms by default; tagging the text as Russian switches it
// to the forms Russian readers expect (the font's `locl` feature), whatever the system language is.
private val Russian = LocaleList("ru")

val TextFamily = FontFamily(text(400), text(500), text(600), text(700))
val DisplayFamily = FontFamily(condensed(500), condensed(600), condensed(700), condensed(800))

private fun display(size: Int, line: Int, weight: Int = 700) = TextStyle(
    fontFamily = DisplayFamily,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = 0.sp,
    localeList = Russian,
)

private fun body(size: Int, line: Int, weight: Int = 400, tracking: Double = 0.0) = TextStyle(
    fontFamily = TextFamily,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
    localeList = Russian,
)

internal val DockTypography = Typography(
    displayLarge = display(64, 64, 800),
    displayMedium = display(52, 54, 800),
    displaySmall = display(42, 46),
    headlineLarge = display(36, 40),
    headlineMedium = display(31, 35),
    headlineSmall = display(26, 30),
    titleLarge = display(24, 28),
    titleMedium = body(16, 22, 600),
    titleSmall = body(14, 20, 600),
    bodyLarge = body(16, 24),
    bodyMedium = body(14, 20),
    bodySmall = body(12, 17),
    labelLarge = body(14, 20, 600),
    labelMedium = body(12, 16, 600, 0.1),
    labelSmall = body(11, 14, 600, 0.1),
)
