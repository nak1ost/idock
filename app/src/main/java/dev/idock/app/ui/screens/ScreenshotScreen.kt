package dev.idock.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.sin

// ---------------------------------------------------------------------------------------------
// Demo data
// ---------------------------------------------------------------------------------------------

private data class ScreenshotCapture(val id: Long, val variant: Int, val clock: String, val time: String)

private val ScreenshotInitialCaptures = listOf(
    ScreenshotCapture(2, 1, "14:21", "14:21:08"),
    ScreenshotCapture(1, 0, "14:19", "14:19:42"),
)

private class ScreenshotGeometry {
    var root: Rect = Rect.Zero
    var screen: Rect = Rect.Zero
    var strip: Rect = Rect.Zero
}

private data class ScreenshotFlight(val capture: ScreenshotCapture, val from: Rect, val to: Rect)

private const val ScreenshotAspect = 9f / 19.5f
private val ScreenshotThumbWidth = 72.dp
private const val ScreenshotStaggerMs = 35f
private const val ScreenshotEnterMs = 350f
private const val ScreenshotStaggerCap = 10

private val ScreenshotClockFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val ScreenshotTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

private fun screenshotLerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun screenshotCountLabel(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    val word = when {
        mod10 == 1 && mod100 != 11 -> "снимок"
        mod10 in 2..4 && mod100 !in 12..14 -> "снимка"
        else -> "снимков"
    }
    return "$n $word"
}

// ---------------------------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------------------------

@Composable
fun ScreenshotScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val side = Modifier.padding(horizontal = 20.dp)

    val captures = remember { mutableStateListOf<ScreenshotCapture>().apply { addAll(ScreenshotInitialCaptures) } }
    var nextId by remember { mutableLongStateOf(10L) }
    var wallVariant by rememberSaveable { mutableIntStateOf(2) }
    var live by rememberSaveable { mutableStateOf(false) }
    var clock by remember { mutableStateOf(LocalTime.now().format(ScreenshotClockFormat)) }
    var previewId by rememberSaveable { mutableStateOf<Long?>(null) }

    val flash = remember { Animatable(0f) }
    val bounce = remember { Animatable(1f) }
    val flightProgress = remember { Animatable(0f) }
    var flight by remember { mutableStateOf<ScreenshotFlight?>(null) }
    var hiddenId by remember { mutableStateOf<Long?>(null) }
    val geometry = remember { ScreenshotGeometry() }
    val stripState = rememberLazyListState()

    val enterClock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val total = ScreenshotStaggerMs * ScreenshotStaggerCap + ScreenshotEnterMs
        enterClock.animateTo(total, tween(total.toInt(), easing = LinearEasing))
    }

    // The mock clock ticks only while the live stream is on.
    LaunchedEffect(live) {
        while (live) {
            clock = LocalTime.now().format(ScreenshotClockFormat)
            delay(1000)
        }
    }

    BackHandler(enabled = previewId != null) { previewId = null }

    fun shoot() {
        val now = LocalTime.now()
        val capture = ScreenshotCapture(nextId, wallVariant, now.format(ScreenshotClockFormat), now.format(ScreenshotTimeFormat))
        nextId += 1
        captures.add(0, capture)
        val thumbW = with(density) { ScreenshotThumbWidth.toPx() }
        val left = geometry.strip.left + with(density) { 20.dp.toPx() }
        val top = geometry.strip.top
        val to = Rect(left, top, left + thumbW, top + thumbW / ScreenshotAspect)
        hiddenId = capture.id
        flight = ScreenshotFlight(capture, geometry.screen, to)

        scope.launch {
            flash.snapTo(0.85f)
            flash.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        }
        scope.launch {
            bounce.animateTo(0.96f, tween(90))
            bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
        }
        scope.launch { stripState.animateScrollToItem(0) }
        scope.launch {
            delay(260)
            wallVariant += 1
        }
        scope.launch {
            flightProgress.snapTo(0f)
            flightProgress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            if (hiddenId == capture.id) {
                hiddenId = null
                flight = null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { geometry.root = it.boundsInRoot() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 24.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ScreenshotFrame(
                variant = wallVariant,
                clock = clock,
                live = live,
                flash = { flash.value },
                bounce = { bounce.value },
                onScreenPositioned = { geometry.screen = it },
                modifier = Modifier
                    .padding(top = 4.dp)
                    .screenshotEnter(enterClock, 0),
            )

            Spacer(Modifier.height(20.dp))
            ScreenshotShutter(
                onClick = { shoot() },
                modifier = Modifier.screenshotEnter(enterClock, 1),
            )

            Spacer(Modifier.height(20.dp))
            Surface(
                modifier = side
                    .fillMaxWidth()
                    .screenshotEnter(enterClock, 2),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "В реальном времени",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (live) "Экран обновляется непрерывно" else "Показан последний кадр",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = live, onCheckedChange = { live = it })
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = side
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 4.dp, end = 4.dp)
                    .screenshotEnter(enterClock, 3),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "Снимки",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (captures.isEmpty()) "" else screenshotCountLabel(captures.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .screenshotEnter(enterClock, 4),
            ) {
                LazyRow(
                    state = stripState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { geometry.strip = it.boundsInRoot() },
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(captures, key = { it.id }) { capture ->
                        ScreenshotThumb(
                            capture = capture,
                            hidden = { hiddenId == capture.id },
                            onClick = { previewId = capture.id },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    if (captures.isEmpty()) {
                        // Keeps the strip's height stable so the empty card can sit on top of it.
                        item(key = "placeholder") {
                            Spacer(Modifier.size(ScreenshotThumbWidth, ScreenshotThumbWidth / ScreenshotAspect + 22.dp))
                        }
                    }
                }
                dev.idock.app.ui.kit.PlainAnimatedVisibility(
                    visible = captures.isEmpty(),
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(150)),
                    modifier = Modifier.matchParentSize(),
                ) {
                    ScreenshotEmptyStrip(side)
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = side
                    .fillMaxWidth()
                    .screenshotEnter(enterClock, 5),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Для снимка экрана iPhone должен быть разблокирован",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Thumbnail flying from the phone frame into the strip.
        flight?.let { f ->
            val wDp = with(density) { f.from.width.toDp() }
            val hDp = with(density) { f.from.height.toDp() }
            if (f.from.width > 0f) {
                ScreenshotMockScreen(
                    variant = f.capture.variant,
                    clock = f.capture.clock,
                    modifier = Modifier
                        .size(wDp, hDp)
                        .graphicsLayer {
                            val p = flightProgress.value
                            transformOrigin = TransformOrigin(0f, 0f)
                            translationX = screenshotLerp(f.from.left, f.to.left, p) - geometry.root.left
                            translationY = screenshotLerp(f.from.top, f.to.top, p) - geometry.root.top -
                                sin(p * PI.toFloat()) * 48.dp.toPx()
                            val s = screenshotLerp(1f, f.to.width / f.from.width, p)
                            scaleX = s
                            scaleY = s
                            shape = RoundedCornerShape(33.dp)
                            clip = true
                        },
                )
            }
        }

        // Preview overlay.
        val previewCapture = captures.firstOrNull { it.id == previewId }
        AnimatedContent(
            targetState = previewCapture,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
            contentAlignment = Alignment.Center,
            label = "preview",
            modifier = Modifier.fillMaxSize(),
        ) { capture ->
            if (capture != null) {
                ScreenshotPreview(
                    capture = capture,
                    contentPadding = contentPadding,
                    onClose = { previewId = null },
                    onDelete = {
                        previewId = null
                        scope.launch {
                            delay(220)
                            captures.remove(capture)
                        }
                    },
                )
            } else {
                Box(Modifier.size(0.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Motion helpers
// ---------------------------------------------------------------------------------------------

private fun Modifier.screenshotEnter(
    clock: Animatable<Float, AnimationVector1D>,
    index: Int,
): Modifier = this.graphicsLayer {
    val p = if (index >= ScreenshotStaggerCap) {
        1f
    } else {
        FastOutSlowInEasing.transform(((clock.value - index * ScreenshotStaggerMs) / ScreenshotEnterMs).coerceIn(0f, 1f))
    }
    alpha = p
    translationY = (1f - p) * 24.dp.toPx()
}

@Composable
private fun screenshotPressScale(interaction: MutableInteractionSource, pressedScale: Float = 0.97f): () -> Float {
    val pressed by interaction.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "screenshotPress",
    )
    return { scale.value }
}

// ---------------------------------------------------------------------------------------------
// Phone frame and mock screen
// ---------------------------------------------------------------------------------------------

@Composable
private fun ScreenshotFrame(
    variant: Int,
    clock: String,
    live: Boolean,
    flash: () -> Float,
    bounce: () -> Float,
    onScreenPositioned: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val outer = RoundedCornerShape(40.dp)
    Box(
        modifier = modifier
            .width(220.dp)
            .aspectRatio(ScreenshotAspect)
            .graphicsLayer {
                val s = bounce()
                scaleX = s
                scaleY = s
            }
            .clip(outer)
            .background(cs.surfaceContainerHighest)
            .border(1.dp, cs.outlineVariant.copy(alpha = 0.6f), outer)
            .padding(7.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(33.dp))
                .onGloballyPositioned { onScreenPositioned(it.boundsInRoot()) },
        ) {
            Crossfade(targetState = variant, animationSpec = tween(500), label = "wallpaper") { v ->
                ScreenshotMockScreen(variant = v, clock = clock, modifier = Modifier.fillMaxSize())
            }
            if (live) ScreenshotShimmer(Modifier.matchParentSize())
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = flash() }
                    .background(Color.White),
            )
        }
        AnimatedVisibility(
            visible = live,
            enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.7f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.7f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp),
        ) {
            ScreenshotLivePill()
        }
    }
}

@Composable
private fun ScreenshotMockScreen(variant: Int, clock: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val palette = listOf(cs.primary, cs.tertiary, cs.secondary, cs.primaryContainer, cs.tertiaryContainer, cs.secondaryContainer)
    val first = palette[variant % palette.size]
    val second = palette[(variant + 1 + (variant / palette.size) % 5) % palette.size]
    val onWall = cs.onPrimary
    val clockStyle = MaterialTheme.typography.displaySmall
    val measurer = rememberTextMeasurer()

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val (gStart, gEnd) = when (variant % 3) {
            0 -> Offset(0f, 0f) to Offset(w, h)
            1 -> Offset(w, 0f) to Offset(0f, h)
            else -> Offset(w / 2f, 0f) to Offset(w / 2f, h)
        }
        drawRect(Brush.linearGradient(listOf(first, second), start = gStart, end = gEnd))
        drawCircle(
            color = Color.White.copy(alpha = 0.12f),
            radius = w * 0.62f,
            center = Offset(w * (0.15f + 0.18f * (variant % 4)), h * (0.22f + 0.05f * (variant % 3))),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.06f),
            radius = w * 0.5f,
            center = Offset(w * (0.9f - 0.12f * (variant % 3)), h * 0.78f),
        )

        // Status bar dots.
        val pad = w * 0.09f
        repeat(3) { i ->
            drawRoundRect(
                color = Color.White.copy(alpha = 0.75f),
                topLeft = Offset(w - pad - (i + 1) * w * 0.05f, h * 0.025f),
                size = Size(w * 0.035f, h * 0.012f),
                cornerRadius = CornerRadius(h * 0.006f),
            )
        }

        // Clock.
        val layout = measurer.measure(
            text = clock,
            style = clockStyle.copy(color = onWall, fontSize = (w * 0.2f).toSp(), fontWeight = FontWeight.Light),
        )
        drawText(layout, topLeft = Offset((w - layout.size.width) / 2f, h * 0.075f))

        // Icon grid.
        val gap = w * 0.07f
        val icon = (w - pad * 2f - gap * 3f) / 4f
        val gridTop = h * 0.30f
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                val k = r * 4 + c + variant
                if (r == 3 && k % 3 == 0) continue
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.22f + 0.07f * (k % 4)),
                    topLeft = Offset(pad + c * (icon + gap), gridTop + r * (icon + gap * 1.5f)),
                    size = Size(icon, icon),
                    cornerRadius = CornerRadius(icon * 0.27f),
                )
            }
        }

        // Dock.
        val dockH = icon + pad * 0.9f
        val dockTop = h - dockH - pad * 0.9f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.18f),
            topLeft = Offset(pad * 0.55f, dockTop),
            size = Size(w - pad * 1.1f, dockH),
            cornerRadius = CornerRadius(dockH * 0.36f),
        )
        for (c in 0 until 4) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = Offset(pad + c * (icon + gap), dockTop + (dockH - icon) / 2f),
                size = Size(icon, icon),
                cornerRadius = CornerRadius(icon * 0.27f),
            )
        }

        // Home indicator.
        drawRoundRect(
            color = Color.White.copy(alpha = 0.7f),
            topLeft = Offset(w * 0.34f, h - pad * 0.45f),
            size = Size(w * 0.32f, h * 0.007f),
            cornerRadius = CornerRadius(h * 0.0035f),
        )
    }
}

/** Periodic light sweep over the live screen — composed only while live mode is on. */
@Composable
private fun ScreenshotShimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1300, delayMillis = 1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "sweep",
    )
    Canvas(modifier) {
        val w = size.width
        val x = -w * 0.8f + sweep * w * 2.4f
        drawRect(
            brush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0f)),
                start = Offset(x, 0f),
                end = Offset(x + w * 0.6f, size.height * 0.4f),
            ),
        )
    }
}

@Composable
private fun ScreenshotLivePill() {
    val cs = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "livePulse",
    )
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(cs.error)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .graphicsLayer { alpha = pulse }
                .clip(CircleShape)
                .background(cs.onError),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "LIVE",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = cs.onError,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Controls
// ---------------------------------------------------------------------------------------------

@Composable
private fun ScreenshotShutter(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val scale = screenshotPressScale(interaction, pressedScale = 0.9f)
    val brush = remember(cs.primary, cs.tertiary) { Brush.linearGradient(listOf(cs.primary, cs.tertiary)) }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .graphicsLayer {
                    val s = scale()
                    scaleX = s
                    scaleY = s
                }
                .border(3.dp, brush, CircleShape)
                .padding(7.dp)
                .clip(CircleShape)
                .background(brush)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(color = cs.onPrimary),
                    role = Role.Button,
                    onClickLabel = "Сделать снимок",
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.PhotoCamera,
                contentDescription = "Сделать снимок",
                tint = cs.onPrimary,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Сделать снимок",
            style = MaterialTheme.typography.labelLarge,
            color = cs.onSurface,
        )
    }
}

@Composable
private fun ScreenshotThumb(
    capture: ScreenshotCapture,
    hidden: () -> Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val scale = screenshotPressScale(interaction, pressedScale = 0.94f)
    Column(
        modifier = modifier.graphicsLayer {
            alpha = if (hidden()) 0f else 1f
            val s = scale()
            scaleX = s
            scaleY = s
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(ScreenshotThumbWidth)
                .aspectRatio(ScreenshotAspect)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, cs.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(),
                    onClickLabel = "Открыть снимок",
                    onClick = onClick,
                ),
        ) {
            ScreenshotMockScreen(variant = capture.variant, clock = capture.clock, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = capture.time,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun ScreenshotEmptyStrip(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        color = cs.surfaceContainerLow,
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cs.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = cs.primary)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Снимков пока нет",
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Нажмите на кнопку затвора — снимок появится здесь",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Preview overlay
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun androidx.compose.animation.AnimatedVisibilityScope.ScreenshotPreview(
    capture: ScreenshotCapture,
    contentPadding: PaddingValues,
    onClose: () -> Unit,
    onDelete: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var feedback by remember(capture.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(1800)
            feedback = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Закрыть",
                onClick = onClose,
            )
            .padding(
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
                start = 20.dp,
                end = 20.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .animateEnterExit(
                    enter = scaleIn(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.85f),
                    exit = scaleOut(tween(200), targetScale = 0.92f),
                ),
            shape = RoundedCornerShape(28.dp),
            color = cs.surfaceContainer,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .aspectRatio(ScreenshotAspect, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(24.dp)),
                ) {
                    ScreenshotMockScreen(variant = capture.variant, clock = capture.clock, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Снимок от ${capture.time}",
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface,
                )
                AnimatedContent(
                    targetState = feedback,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                    label = "feedback",
                ) { message ->
                    Row(
                        modifier = Modifier.heightIn(min = 28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (message != null) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = cs.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(message, style = MaterialTheme.typography.bodyMedium, color = cs.primary)
                        } else {
                            Text("PNG · исходное качество", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ScreenshotAction(Icons.Rounded.Download, "Сохранить\nв галерею", cs.primary) {
                        feedback = "Сохранено в галерею"
                    }
                    ScreenshotAction(Icons.Rounded.Share, "Поделиться", cs.secondary) {
                        feedback = "Снимок готов к отправке"
                    }
                    ScreenshotAction(Icons.Rounded.Delete, "Удалить", cs.error, onClick = onDelete)
                }
            }
        }
    }
}

@Composable
private fun ScreenshotAction(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = screenshotPressScale(interaction, pressedScale = 0.92f)
    Column(
        modifier = Modifier
            .width(96.dp)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            }
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            minLines = 2,
        )
    }
}
