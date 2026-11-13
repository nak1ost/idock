package dev.idock.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import dev.idock.app.data.ThemeMode

private val DockShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Changes the theme. With an [origin] (in root coordinates) the new theme spreads out from that point. */
fun interface ThemeSwitcher {
    fun set(mode: ThemeMode, origin: Offset?)
}

val LocalThemeSwitcher = staticCompositionLocalOf<ThemeSwitcher> { ThemeSwitcher { _, _ -> } }

/** Whether the black theme is the one on screen right now. */
val LocalIsDark = staticCompositionLocalOf { false }

@Composable
fun IDockTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalDockColors provides if (dark) DarkDock else LightDock,
        LocalIsDark provides dark,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = DockTypography,
            shapes = DockShapes,
            content = content,
        )
    }
}
