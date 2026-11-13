package dev.idock.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import dev.idock.app.data.Prefs
import dev.idock.app.data.ThemeMode
import dev.idock.app.device.UsbDeviceMonitor
import dev.idock.app.ui.onboarding.Onboarding
import dev.idock.app.ui.settings.AuthorCredit
import dev.idock.app.ui.shell.Shell
import dev.idock.app.ui.theme.IDockTheme
import dev.idock.app.ui.theme.LocalThemeSwitcher
import dev.idock.app.ui.theme.ThemeSwitcher
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.hypot
import kotlin.math.max
import kotlinx.coroutines.launch

@Composable
fun AppRoot(prefs: Prefs, monitor: UsbDeviceMonitor) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (prefs.themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val reveal = remember { ThemeRevealState() }
    val scope = rememberCoroutineScope()
    val currentDark by rememberUpdatedState(dark)
    val currentSystemDark by rememberUpdatedState(systemDark)

    val switcher = remember(prefs) {
        ThemeSwitcher { mode, origin ->
            val willBeDark = when (mode) {
                ThemeMode.System -> currentSystemDark
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            if (origin == null || willBeDark == currentDark) {
                prefs.updateThemeMode(mode)
            } else {
                scope.launch { reveal.reveal(origin) { prefs.updateThemeMode(mode) } }
            }
        }
    }

    SystemBarIcons(dark)
    IDockTheme(dark = dark) {
        CompositionLocalProvider(
            LocalThemeSwitcher provides switcher,
            // Text and icons without an explicit colour follow the theme instead of staying black.
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
        ) {
            ThemeRevealBox(state = reveal, ringColor = MaterialTheme.colorScheme.primary) {
                AppContent(prefs, monitor)
            }
        }
    }
}

@Composable
private fun AppContent(prefs: Prefs, monitor: UsbDeviceMonitor) {
    var showOnboarding by rememberSaveable { mutableStateOf(!prefs.onboardingDone) }
    var showCredit by rememberSaveable { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (!showOnboarding) {
            Shell(prefs = prefs, monitor = monitor, onReplayOnboarding = { showOnboarding = true })
        }
        AnimatedVisibility(
            visible = showOnboarding,
            enter = fadeIn(tween(250)),
            exit = slideOutVertically(tween(420)) { -it / 5 } + fadeOut(tween(320)),
        ) {
            Onboarding(
                onFinish = {
                    prefs.completeOnboarding()
                    showOnboarding = false
                    // The author card belongs to the very first launch only.
                    if (!prefs.creditShown) {
                        prefs.markCreditShown()
                        showCredit = true
                    }
                },
            )
        }
        AuthorCredit(visible = showCredit, onDismiss = { showCredit = false })
    }
}

/** Dark icons on the light theme, light icons on the black one, whatever the system theme is. */
@Composable
private fun SystemBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/* ---------- Theme change that spreads out from the point that was tapped ---------- */

@Stable
internal class ThemeRevealState {
    var snapshot by mutableStateOf<ImageBitmap?>(null)
        private set
    var origin by mutableStateOf(Offset.Zero)
        private set
    val progress = Animatable(0f)
    var layer: GraphicsLayer? = null

    /**
     * Takes a picture of the screen as it is, applies [change] underneath it, then opens a growing
     * hole in the picture around [origin] so the new theme shows through.
     */
    suspend fun reveal(origin: Offset, change: () -> Unit) {
        val source = layer
        if (source == null || snapshot != null) {
            change()
            return
        }
        val picture = try {
            source.toImageBitmap()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Throwable) {
            null
        }
        if (picture == null) {
            change()
            return
        }
        this.origin = origin
        progress.snapTo(0f)
        snapshot = picture
        change()
        try {
            progress.animateTo(1f, tween(durationMillis = 640, easing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)))
        } finally {
            snapshot = null
        }
    }
}

@Composable
private fun ThemeRevealBox(state: ThemeRevealState, ringColor: Color, content: @Composable () -> Unit) {
    val layer = rememberGraphicsLayer()
    val hole = remember { Path() }
    DisposableEffect(layer) {
        state.layer = layer
        onDispose { state.layer = null }
    }
    Box(
        Modifier
            .fillMaxSize()
            .drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
                val picture = state.snapshot
                if (picture != null) {
                    val o = state.origin
                    val reach = hypot(max(o.x, size.width - o.x), max(o.y, size.height - o.y))
                    val p = state.progress.value
                    val radius = reach * p
                    hole.rewind()
                    hole.addOval(Rect(center = o, radius = radius))
                    clipPath(hole, clipOp = ClipOp.Difference) { drawImage(picture) }
                    drawCircle(
                        color = ringColor,
                        radius = radius,
                        center = o,
                        alpha = (1f - p).coerceIn(0f, 1f),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            },
    ) {
        content()
    }
}
