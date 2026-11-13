package dev.idock.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// ---------------------------------------------------------------------------------------------
// Demo data
// ---------------------------------------------------------------------------------------------

private data class RingtonesTrack(
    val id: Int,
    val title: String,
    val genre: String,
    val durationSec: Int,
)

private val RingtonesTracks = listOf(
    RingtonesTrack(1, "Утренний бит", "Электроника", 72),
    RingtonesTrack(2, "Летний вечер", "Лаунж", 96),
    RingtonesTrack(3, "Неоновый город", "Синтвейв", 84),
    RingtonesTrack(4, "Тихая гавань", "Эмбиент", 108),
    RingtonesTrack(5, "Северный ветер", "Инди", 66),
    RingtonesTrack(6, "Кофе и дождь", "Лоу-фай", 90),
)

private data class RingtonesSaved(
    val id: Long,
    val name: String,
    val durationSec: Int,
    val source: String,
    val fadeIn: Boolean,
    val fadeOut: Boolean,
)

private val RingtonesInitialSaved = listOf(
    RingtonesSaved(1, "Мягкий будильник", 20, "Тихая гавань", fadeIn = true, fadeOut = false),
    RingtonesSaved(2, "Звонок по утрам", 30, "Утренний бит", fadeIn = false, fadeOut = true),
)

private enum class RingtonesSaveState { Idle, Saving, Done }

private const val RingtonesBars = 90
private const val RingtonesMinSec = 5f
private const val RingtonesMaxSec = 30f
private const val RingtonesFadeSec = 3f
private const val RingtonesStaggerMs = 35f
private const val RingtonesEnterMs = 350f
private const val RingtonesStaggerCap = 10

/** Trim selection of one track, kept per track so switching back restores it. */
private class RingtonesSelection(start: Float, end: Float) {
    var start by mutableFloatStateOf(start)
    var end by mutableFloatStateOf(end)
    var playhead by mutableFloatStateOf(start)
}

private fun ringtonesDefaultSelection(track: RingtonesTrack): RingtonesSelection {
    val dur = track.durationSec.toFloat()
    val start = (dur * 0.25f).roundToInt().toFloat()
    val end = min(dur, start + 25f)
    return RingtonesSelection(start, end)
}

/** Deterministic pseudo-random waveform for a track. */
private fun ringtonesWave(seed: Int, count: Int = RingtonesBars): FloatArray {
    val rnd = Random(seed * 7919 + 13)
    val raw = FloatArray(count) { rnd.nextFloat() }
    val cycles = 1.5f + (seed % 3) * 0.75f
    return FloatArray(count) { i ->
        val prev = raw[(i - 1).coerceAtLeast(0)]
        val next = raw[(i + 1).coerceAtMost(count - 1)]
        val smooth = (prev + 2f * raw[i] + next) / 4f
        val t = i / (count - 1f)
        val envelope = 0.5f + 0.5f * abs(sin(t * PI.toFloat() * cycles))
        val intro = min(1f, t * 8f) * min(1f, (1f - t) * 10f)
        (0.14f + 0.86f * smooth * envelope * (0.35f + 0.65f * intro)).coerceIn(0.1f, 1f)
    }
}

private fun ringtonesClock(sec: Float): String {
    val s = sec.roundToInt().coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

private fun ringtonesLength(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)

// ---------------------------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------------------------

@Composable
fun RingtonesScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val side = Modifier.padding(horizontal = 20.dp)

    var selectedId by rememberSaveable { mutableIntStateOf(RingtonesTracks.first().id) }
    val track = RingtonesTracks.first { it.id == selectedId }
    val selections = remember { HashMap<Int, RingtonesSelection>() }
    val selection = remember(track.id) { selections.getOrPut(track.id) { ringtonesDefaultSelection(track) } }

    var playing by remember { mutableStateOf(false) }
    var fadeIn by rememberSaveable { mutableStateOf(true) }
    var fadeOut by rememberSaveable { mutableStateOf(true) }
    var name by rememberSaveable(selectedId) { mutableStateOf(track.title) }

    val fadeInAmount by animateFloatAsState(if (fadeIn) 1f else 0f, tween(400), label = "fadeIn")
    val fadeOutAmount by animateFloatAsState(if (fadeOut) 1f else 0f, tween(400), label = "fadeOut")

    var saveState by remember { mutableStateOf(RingtonesSaveState.Idle) }
    val saveProgress = remember { Animatable(0f) }
    val saved = remember { mutableStateListOf<RingtonesSaved>().apply { addAll(RingtonesInitialSaved) } }
    var nextId by remember { mutableLongStateOf(100L) }

    val enterClock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val total = RingtonesStaggerMs * RingtonesStaggerCap + RingtonesEnterMs
        enterClock.animateTo(total, tween(total.toInt(), easing = LinearEasing))
    }

    // Simulated real-time playback, looping inside the selected range.
    LaunchedEffect(playing, selection) {
        if (!playing) return@LaunchedEffect
        if (selection.playhead !in selection.start..selection.end) selection.playhead = selection.start
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = (now - last) / 1_000_000_000f
                last = now
                var ph = selection.playhead + dt
                if (ph >= selection.end || ph < selection.start) ph = selection.start
                selection.playhead = ph
            }
        }
    }

    LaunchedEffect(saveState) {
        when (saveState) {
            RingtonesSaveState.Saving -> {
                saveProgress.snapTo(0f)
                saveProgress.animateTo(1f, tween(2200, easing = FastOutSlowInEasing))
                saved.add(
                    0,
                    RingtonesSaved(
                        id = nextId,
                        name = name.trim(),
                        durationSec = (selection.end - selection.start).roundToInt(),
                        source = track.title,
                        fadeIn = fadeIn,
                        fadeOut = fadeOut,
                    ),
                )
                nextId += 1
                saveState = RingtonesSaveState.Done
            }

            RingtonesSaveState.Done -> {
                delay(1800)
                saveState = RingtonesSaveState.Idle
            }

            RingtonesSaveState.Idle -> Unit
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection),
            end = contentPadding.calculateEndPadding(layoutDirection),
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "pick-header") {
            RingtonesSectionHeader(
                title = "Выберите трек",
                trailing = "${RingtonesTracks.size} демо-трека",
                modifier = side.ringtonesEnter(enterClock, 0),
            )
        }

        item(key = "tracks") {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .ringtonesEnter(enterClock, 1),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(RingtonesTracks, key = { it.id }) { t ->
                    RingtonesTrackCard(
                        track = t,
                        selected = t.id == selectedId,
                        playing = playing && t.id == selectedId,
                        onClick = {
                            if (t.id != selectedId) {
                                playing = false
                                selectedId = t.id
                            }
                        },
                    )
                }
            }
        }

        item(key = "editor") {
            Crossfade(
                targetState = track,
                animationSpec = tween(300),
                label = "editor",
                modifier = side.ringtonesEnter(enterClock, 2),
            ) { t ->
                val sel = remember(t.id) { selections.getOrPut(t.id) { ringtonesDefaultSelection(t) } }
                RingtonesEditorCard(
                    track = t,
                    selection = sel,
                    playing = playing && t.id == selectedId,
                    fadeIn = { fadeInAmount },
                    fadeOut = { fadeOutAmount },
                    onTogglePlay = { playing = !playing },
                )
            }
        }

        item(key = "options") {
            Surface(
                modifier = side
                    .fillMaxWidth()
                    .ringtonesEnter(enterClock, 3),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    RingtonesSwitchRow(
                        title = "Плавное начало",
                        subtitle = "Громкость нарастает за 3 с",
                        checked = fadeIn,
                        onCheckedChange = { fadeIn = it },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    RingtonesSwitchRow(
                        title = "Плавное затухание",
                        subtitle = "Мягкое окончание фрагмента",
                        checked = fadeOut,
                        onCheckedChange = { fadeOut = it },
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(32) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        label = { Text("Название рингтона") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }
        }

        item(key = "save") {
            RingtonesSaveButton(
                state = saveState,
                progress = { saveProgress.value },
                enabled = name.isNotBlank(),
                onClick = { saveState = RingtonesSaveState.Saving },
                modifier = side.ringtonesEnter(enterClock, 4),
            )
        }

        item(key = "mine-header") {
            RingtonesSectionHeader(
                title = "Мои рингтоны",
                trailing = if (saved.isEmpty()) "" else "${saved.size}",
                modifier = side
                    .animateItem()
                    .ringtonesEnter(enterClock, 5)
                    .padding(top = 12.dp),
            )
        }

        if (saved.isEmpty()) {
            item(key = "mine-empty") {
                RingtonesEmptyState(side.animateItem())
            }
        }

        items(saved, key = { it.id }) { item ->
            RingtonesSavedRow(
                item = item,
                onDelete = { saved.remove(item) },
                modifier = side
                    .animateItem()
                    .ringtonesEnter(enterClock, 6 + saved.indexOf(item)),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Motion helpers
// ---------------------------------------------------------------------------------------------

private fun Modifier.ringtonesEnter(
    clock: Animatable<Float, AnimationVector1D>,
    index: Int,
): Modifier = this.graphicsLayer {
    val p = if (index >= RingtonesStaggerCap) {
        1f
    } else {
        FastOutSlowInEasing.transform(((clock.value - index * RingtonesStaggerMs) / RingtonesEnterMs).coerceIn(0f, 1f))
    }
    alpha = p
    translationY = (1f - p) * 24.dp.toPx()
}

@Composable
private fun ringtonesPressScale(interaction: MutableInteractionSource, pressedScale: Float = 0.97f): () -> Float {
    val pressed by interaction.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "ringtonesPress",
    )
    return { scale.value }
}

// ---------------------------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun RingtonesSectionHeader(
    title: String,
    trailing: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = trailing,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RingtonesTrackCard(
    track: RingtonesTrack,
    selected: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val scale = ringtonesPressScale(interaction)
    val container by animateColorAsState(
        targetValue = if (selected) cs.primaryContainer else cs.surfaceContainer,
        animationSpec = tween(300),
        label = "trackContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) cs.onPrimaryContainer else cs.onSurface,
        animationSpec = tween(300),
        label = "trackContent",
    )
    val accent = if (selected) cs.primary else cs.tertiary

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(156.dp)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            },
        shape = RoundedCornerShape(20.dp),
        color = container,
        contentColor = content,
        interactionSource = interaction,
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                if (playing) {
                    RingtonesEqualizer(color = accent, modifier = Modifier.size(22.dp))
                } else {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = accent)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${track.genre} · ${ringtonesLength(track.durationSec)}",
                style = MaterialTheme.typography.bodySmall,
                color = content.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Tiny live equalizer — only composed while playback runs. */
@Composable
private fun RingtonesEqualizer(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "eq")
    val a by transition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "eqA")
    val b by transition.animateFloat(1f, 0.25f, infiniteRepeatable(tween(360), RepeatMode.Reverse), label = "eqB")
    val c by transition.animateFloat(0.45f, 0.9f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "eqC")
    Canvas(modifier) {
        val barW = size.width / 5f
        listOf(a, b, c).forEachIndexed { i, v ->
            val h = size.height * v
            drawRoundRect(
                color = color,
                topLeft = Offset(barW * (i * 2f), size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f),
            )
        }
    }
}

@Composable
private fun RingtonesEditorCard(
    track: RingtonesTrack,
    selection: RingtonesSelection,
    playing: Boolean,
    fadeIn: () -> Float,
    fadeOut: () -> Float,
    onTogglePlay: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = cs.surfaceContainer,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${track.genre} · ${ringtonesLength(track.durationSec)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                    )
                }
                Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = cs.onSurfaceVariant)
            }

            Spacer(Modifier.height(16.dp))

            RingtonesWaveform(
                track = track,
                selection = selection,
                playing = playing,
                fadeIn = fadeIn,
                fadeOut = fadeOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp),
            )

            Spacer(Modifier.height(14.dp))
            RingtonesReadout(selection)
            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                RingtonesPlayButton(playing = playing, onClick = onTogglePlay)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (playing) "Воспроизведение" else "Прослушать фрагмент",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface,
                    )
                    RingtonesPlayTime(selection = selection, playing = playing)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Перетаскивайте ручки или сам фрагмент: от 5 до 30 секунд",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RingtonesWaveform(
    track: RingtonesTrack,
    selection: RingtonesSelection,
    playing: Boolean,
    fadeIn: () -> Float,
    fadeOut: () -> Float,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val wave = remember(track.id) { ringtonesWave(track.id) }
    val dur = track.durationSec.toFloat()
    val gradientColors = remember(cs.primary, cs.tertiary) { listOf(cs.primary, cs.tertiary) }
    val idleBar = cs.outlineVariant
    val selectionTint = cs.primary.copy(alpha = 0.08f)
    val playheadColor = cs.onSurface
    val startKnob = cs.primary
    val endKnob = cs.tertiary
    val gripColor = cs.onPrimary
    val envelopeColor = cs.onSurface.copy(alpha = 0.55f)
    val envelopePath = remember { Path() }

    // 0 — nothing, 1 — start handle, 2 — end handle, 3 — whole range
    var active by remember { mutableIntStateOf(0) }
    val startGrow by animateFloatAsState(if (active == 1) 1f else 0f, spring(stiffness = Spring.StiffnessMedium), label = "startGrow")
    val endGrow by animateFloatAsState(if (active == 2) 1f else 0f, spring(stiffness = Spring.StiffnessMedium), label = "endGrow")
    val rangeGrow by animateFloatAsState(if (active == 3) 1f else 0f, tween(200), label = "rangeGrow")

    Canvas(
        modifier = modifier.pointerInput(selection, dur) {
            var mode = 0
            var anchorStart = 0f
            var anchorEnd = 0f
            var accumulated = 0f
            detectHorizontalDragGestures(
                onDragStart = { pos ->
                    val w = size.width.toFloat()
                    val sx = selection.start / dur * w
                    val ex = selection.end / dur * w
                    val touch = 28.dp.toPx()
                    val ds = abs(pos.x - sx)
                    val de = abs(pos.x - ex)
                    mode = when {
                        pos.x > sx + touch * 0.6f && pos.x < ex - touch * 0.6f -> 3
                        ds <= touch || de <= touch -> when {
                            ds < de -> 1
                            de < ds -> 2
                            pos.x < sx -> 1
                            else -> 2
                        }
                        pos.x > sx && pos.x < ex -> 3
                        else -> 0
                    }
                    anchorStart = selection.start
                    anchorEnd = selection.end
                    accumulated = 0f
                    active = mode
                },
                onDragEnd = {
                    mode = 0
                    active = 0
                },
                onDragCancel = {
                    mode = 0
                    active = 0
                },
                onHorizontalDrag = { change, dragAmount ->
                    if (mode != 0) {
                        change.consume()
                        accumulated += dragAmount / size.width * dur
                        when (mode) {
                            1 -> selection.start = (anchorStart + accumulated).coerceIn(
                                max(0f, anchorEnd - RingtonesMaxSec),
                                anchorEnd - RingtonesMinSec,
                            )

                            2 -> selection.end = (anchorEnd + accumulated).coerceIn(
                                anchorStart + RingtonesMinSec,
                                min(dur, anchorStart + RingtonesMaxSec),
                            )

                            else -> {
                                val shift = accumulated.coerceIn(-anchorStart, dur - anchorEnd)
                                selection.start = anchorStart + shift
                                selection.end = anchorEnd + shift
                            }
                        }
                        if (selection.playhead !in selection.start..selection.end) {
                            selection.playhead = selection.start
                        }
                    }
                },
            )
        },
    ) {
        val n = wave.size
        val w = size.width
        val h = size.height
        val slot = w / n
        val barW = slot * 0.6f
        val midY = h / 2f
        val maxH = h * 0.72f
        val start = selection.start
        val end = selection.end
        val sx = start / dur * w
        val ex = end / dur * w
        val fi = fadeIn()
        val fo = fadeOut()
        val fadeLen = min(RingtonesFadeSec, (end - start) / 3f)
        val ph = selection.playhead

        fun gain(t: Float): Float {
            val rampIn = ((t - start) / fadeLen).coerceIn(0f, 1f)
            val rampOut = ((end - t) / fadeLen).coerceIn(0f, 1f)
            return (1f - fi * (1f - rampIn)) * (1f - fo * (1f - rampOut))
        }

        // Selected range backdrop.
        drawRoundRect(
            color = selectionTint.copy(alpha = selectionTint.alpha * (1f + rangeGrow)),
            topLeft = Offset(sx, 0f),
            size = Size(max(0f, ex - sx), h),
            cornerRadius = CornerRadius(12.dp.toPx()),
        )

        val brush = Brush.linearGradient(gradientColors, start = Offset(sx, 0f), end = Offset(max(sx + 1f, ex), 0f))
        for (i in 0 until n) {
            val t = (i + 0.5f) / n * dur
            val x = (i + 0.5f) * slot
            val inSel = t in start..end
            var amp = wave[i]
            if (inSel) {
                amp *= max(0.12f, gain(t))
                if (playing) {
                    val d = (t - ph) / 1.6f
                    val beat = 0.65f + 0.35f * sin(ph * 12f)
                    amp *= 1f + 0.22f * exp(-d * d) * beat
                }
            }
            val barH = max(barW, min(1f, amp) * maxH)
            val topLeft = Offset(x - barW / 2f, midY - barH / 2f)
            val barSize = Size(barW, barH)
            if (inSel) {
                drawRoundRect(brush = brush, topLeft = topLeft, size = barSize, cornerRadius = CornerRadius(barW / 2f))
            } else {
                drawRoundRect(color = idleBar, topLeft = topLeft, size = barSize, cornerRadius = CornerRadius(barW / 2f))
            }
        }

        // Fade envelope.
        val envAlpha = max(fi, fo)
        if (envAlpha > 0.01f && ex > sx) {
            envelopePath.reset()
            val steps = 48
            for (k in 0..steps) {
                val t = start + (end - start) * k / steps
                val x = t / dur * w
                val y = midY + maxH / 2f - maxH * gain(t)
                if (k == 0) envelopePath.moveTo(x, y) else envelopePath.lineTo(x, y)
            }
            drawPath(
                path = envelopePath,
                color = envelopeColor,
                alpha = envAlpha,
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        // Playhead.
        if (playing || ph > start + 0.05f) {
            val px = (ph / dur * w).coerceIn(sx, ex)
            val a = if (playing) 1f else 0.45f
            drawLine(
                color = playheadColor,
                start = Offset(px, 10.dp.toPx()),
                end = Offset(px, h - 10.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
                alpha = a,
            )
            drawCircle(color = playheadColor, radius = 4.dp.toPx(), center = Offset(px, 10.dp.toPx()), alpha = a)
        }

        // Trim handles.
        fun handle(x: Float, color: Color, grow: Float) {
            drawLine(
                color = color,
                start = Offset(x, 2.dp.toPx()),
                end = Offset(x, h - 2.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
            val kw = (14f + 4f * grow).dp.toPx()
            val kh = (40f + 8f * grow).dp.toPx()
            drawRoundRect(
                color = color,
                topLeft = Offset(x - kw / 2f, midY - kh / 2f),
                size = Size(kw, kh),
                cornerRadius = CornerRadius(kw / 2f),
            )
            val gripH = kh * 0.32f
            drawLine(
                color = gripColor,
                start = Offset(x, midY - gripH / 2f),
                end = Offset(x, midY + gripH / 2f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        handle(sx, startKnob, startGrow)
        handle(ex, endKnob, endGrow)
    }
}

@Composable
private fun RingtonesReadout(selection: RingtonesSelection) {
    val cs = MaterialTheme.colorScheme
    val startSec by remember(selection) { derivedStateOf { selection.start.roundToInt() } }
    val endSec by remember(selection) { derivedStateOf { selection.end.roundToInt() } }
    val length = endSec - startSec
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "${ringtonesClock(startSec.toFloat())} — ${ringtonesClock(endSec.toFloat())}",
            style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "длительность $length с",
            style = MaterialTheme.typography.bodyMedium,
            color = if (length >= RingtonesMaxSec.toInt() || length <= RingtonesMinSec.toInt()) cs.tertiary else cs.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

@Composable
private fun RingtonesPlayTime(selection: RingtonesSelection, playing: Boolean) {
    val sec by remember(selection) { derivedStateOf { selection.playhead.toInt() } }
    Text(
        text = if (playing) "${ringtonesClock(sec.toFloat())} · по кругу" else "Фрагмент воспроизводится по кругу",
        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RingtonesPlayButton(playing: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val scale = ringtonesPressScale(interaction, pressedScale = 0.92f)
    val brush = remember(cs.primary, cs.tertiary) { Brush.linearGradient(listOf(cs.primary, cs.tertiary)) }
    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            }
            .clip(CircleShape)
            .background(brush)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = cs.onPrimary),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = playing,
            transitionSpec = {
                (scaleIn(tween(220), initialScale = 0.6f) + fadeIn(tween(220))) togetherWith
                    (scaleOut(tween(160), targetScale = 0.6f) + fadeOut(tween(160)))
            },
            label = "playIcon",
        ) { isPlaying ->
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                tint = cs.onPrimary,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun RingtonesSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RingtonesSaveButton(
    state: RingtonesSaveState,
    progress: () -> Float,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val scale = ringtonesPressScale(interaction)
    val colors = remember(cs.primary, cs.tertiary) { listOf(cs.primary, cs.tertiary) }
    val gradientAlpha by animateFloatAsState(
        targetValue = when {
            state == RingtonesSaveState.Saving -> 0f
            !enabled -> 0f
            else -> 1f
        },
        animationSpec = tween(300),
        label = "saveGradient",
    )
    val canClick = enabled && state == RingtonesSaveState.Idle

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            }
            .clip(CircleShape)
            .background(cs.surfaceContainerHighest)
            .drawBehind {
                val brush = Brush.linearGradient(colors, start = Offset.Zero, end = Offset(size.width, 0f))
                if (state == RingtonesSaveState.Saving) {
                    val p = progress().coerceIn(0f, 1f)
                    drawRoundRect(
                        brush = brush,
                        size = Size(size.width * p, size.height),
                        cornerRadius = CornerRadius(size.height / 2f),
                        alpha = 0.35f,
                    )
                }
                if (gradientAlpha > 0f) {
                    drawRect(brush = brush, alpha = gradientAlpha)
                }
            }
            .clickable(
                enabled = canClick,
                interactionSource = interaction,
                indication = ripple(color = cs.onPrimary),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.92f)) togetherWith fadeOut(tween(150))
            },
            label = "saveContent",
        ) { s ->
            val onGradient = enabled && s != RingtonesSaveState.Saving
            val tint = if (onGradient) cs.onPrimary else cs.onSurface
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (s) {
                    RingtonesSaveState.Idle -> {
                        Icon(Icons.Rounded.Smartphone, contentDescription = null, tint = if (enabled) tint else cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Сохранить на iPhone",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (enabled) tint else cs.onSurfaceVariant,
                        )
                    }

                    RingtonesSaveState.Saving -> RingtonesSavingLabel(progress)

                    RingtonesSaveState.Done -> {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Готово — рингтон уже на iPhone",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = tint,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RingtonesSavingLabel(progress: () -> Float) {
    Text(
        text = "Сохранение… ${(progress() * 100f).roundToInt()}%",
        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun RingtonesSavedRow(
    item: RingtonesSaved,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val details = buildList {
        add(ringtonesLength(item.durationSec))
        add(item.source)
        if (item.fadeIn && item.fadeOut) add("плавно") else if (item.fadeIn) add("плавное начало") else if (item.fadeOut) add("затухание")
    }.joinToString(" · ")
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cs.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RingtonesIconTile(Icons.Rounded.MusicNote, cs.tertiary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Удалить", tint = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RingtonesIconTile(icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun RingtonesEmptyState(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = cs.surfaceContainerLow,
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RingtonesIconTile(Icons.Rounded.MusicNote, cs.primary)
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Здесь появятся ваши рингтоны",
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Выберите фрагмент трека и сохраните его на iPhone",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
