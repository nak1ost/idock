package dev.idock.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CameraFront
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Demo report
// ---------------------------------------------------------------------------------------------

private data class VerifyPart(
    val title: String,
    val icon: ImageVector,
    val factory: String,
    val current: String,
    val replaced: Boolean,
    val note: String,
)

private enum class VerifyPhase { Running, Done }

private const val VerifyScore = 96

private val VerifyParts: List<VerifyPart> = listOf(
    VerifyPart(
        "Серийный номер", Icons.Rounded.Fingerprint, "F4K9 QX7L 2MDN", "F4K9 QX7L 2MDN", false,
        "Серийный номер в системе совпадает с записанным при производстве — корпус и плата из одного комплекта.",
    ),
    VerifyPart(
        "Модель", Icons.Rounded.PhoneIphone, "MDL-3481 · 256 ГБ", "MDL-3481 · 256 ГБ", false,
        "Модель и объём памяти соответствуют заводской конфигурации.",
    ),
    VerifyPart(
        "Материнская плата", Icons.Rounded.DeveloperBoard, "C0X7 2Q91 LKP4", "C0X7 2Q91 LKP4", false,
        "Идентификатор платы совпадает. Плата не заменялась.",
    ),
    VerifyPart(
        "Батарея", Icons.Rounded.BatteryFull, "B3K8 1PZ7 QQ02", "N9L2 7TT4 XA61", true,
        "Серийный номер батареи отличается от заводского — батарею меняли. Это не всегда плохо: " +
            "уточните у продавца, где и когда делали замену, и проверьте ёмкость в разделе «Батарея».",
    ),
    VerifyPart(
        "Задняя камера", Icons.Rounded.PhotoCamera, "RC-55A1 9KD2", "RC-55A1 9KD2", false,
        "Модуль основной камеры оригинальный для этого устройства.",
    ),
    VerifyPart(
        "Фронтальная камера", Icons.Rounded.CameraFront, "FC-20B7 3LX0", "FC-20B7 3LX0", false,
        "Фронтальная камера совпадает с заводской записью.",
    ),
    VerifyPart(
        "Экран", Icons.Rounded.AspectRatio, "DS-7Q2M 81PK", "DS-7Q2M 81PK", false,
        "Дисплей не заменялся: идентификатор панели совпадает.",
    ),
    VerifyPart(
        "Датчик Face ID", Icons.Rounded.Face, "FD-04NX 6T2R", "FD-04NX 6T2R", false,
        "Модуль распознавания лица привязан к этой плате и работает штатно.",
    ),
    VerifyPart(
        "Модуль Wi‑Fi", Icons.Rounded.Wifi, "W1:4C:8E:21", "W1:4C:8E:21", false,
        "Адрес беспроводного модуля совпадает с заводским.",
    ),
    VerifyPart(
        "Bluetooth", Icons.Rounded.Bluetooth, "B1:4C:8E:22", "B1:4C:8E:22", false,
        "Адрес Bluetooth совпадает с заводским.",
    ),
    VerifyPart(
        "Сотовый модуль", Icons.Rounded.SignalCellularAlt, "35 482910 774120 3", "35 482910 774120 3", false,
        "Идентификатор модема совпадает — модуль связи оригинальный.",
    ),
    VerifyPart(
        "Динамик", Icons.Rounded.Speaker, "SP-11D4 0QJ8", "SP-11D4 0QJ8", false,
        "Основной динамик не заменялся.",
    ),
)

private fun verifyStep(progress: Float): String = when {
    progress < 0.38f -> "Чтение серийных номеров…"
    progress < 0.72f -> "Сравнение с заводскими данными…"
    progress < 0.92f -> "Проверка батареи…"
    else -> "Формирование отчёта…"
}

// ---------------------------------------------------------------------------------------------
// Motion helpers
// ---------------------------------------------------------------------------------------------

@Composable
private fun Modifier.verifyPress(source: InteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "verifyPress",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
private fun Modifier.verifyEntrance(index: Int): Modifier {
    if (index >= 10) return this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 35L)
        progress.animateTo(1f, tween(durationMillis = 350, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 24.dp.toPx()
    }
}

// ---------------------------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------------------------

@Composable
fun VerifyScreen(contentPadding: PaddingValues = PaddingValues(0.dp), modifier: Modifier = Modifier) {
    var runId by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(VerifyPhase.Running) }
    val ring = remember { Animatable(0f) }
    val score = remember { Animatable(0f) }
    var saved by remember { mutableStateOf(false) }
    val expanded = remember { mutableStateListOf<Int>() }

    LaunchedEffect(runId) {
        phase = VerifyPhase.Running
        saved = false
        expanded.clear()
        score.snapTo(0f)
        if (ring.value > 0f) ring.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
        ring.animateTo(1f, tween(2500, easing = FastOutSlowInEasing))
        phase = VerifyPhase.Done
        launch { ring.animateTo(VerifyScore / 100f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)) }
        score.animateTo(VerifyScore.toFloat(), tween(1100, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(saved) {
        if (saved) {
            delay(2600)
            saved = false
        }
    }

    val running = phase == VerifyPhase.Running
    val layoutDirection = LocalLayoutDirection.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection) + 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            end = contentPadding.calculateEndPadding(layoutDirection) + 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "hero") {
            VerifyHero(
                phase = phase,
                ring = { ring.value },
                score = { score.value },
                modifier = Modifier.verifyEntrance(0),
            )
        }
        item(key = "actions") {
            VerifyActions(
                running = running,
                saved = saved,
                onRerun = { runId++ },
                onSave = { saved = true },
                modifier = Modifier.padding(top = 6.dp).verifyEntrance(1),
            )
        }
        item(key = "partsTitle") {
            Text(
                text = "Компоненты",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = 4.dp, top = 14.dp, bottom = 2.dp)
                    .verifyEntrance(2),
            )
        }
        if (running) {
            item(key = "placeholder") {
                VerifyPlaceholders(modifier = Modifier.animateItem())
            }
        } else {
            VerifyParts.forEachIndexed { index, part ->
                item(key = "part:$runId:$index") {
                    VerifyRow(
                        part = part,
                        expanded = index in expanded,
                        onClick = { if (index in expanded) expanded.remove(index) else expanded.add(index) },
                        modifier = Modifier
                            .animateItem()
                            .verifyEntrance(index),
                    )
                }
            }
        }
        item(key = "footnote") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Отчёт носит информационный характер и основан на данных, которые сообщает само устройство.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Hero
// ---------------------------------------------------------------------------------------------

@Composable
private fun VerifyHero(
    phase: VerifyPhase,
    ring: () -> Float,
    score: () -> Float,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val running = phase == VerifyPhase.Running
    val gradient = remember(cs.primary, cs.tertiary) { listOf(cs.primary, cs.tertiary, cs.primary) }
    val track = cs.surfaceContainerHighest

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cs.surfaceContainer)
            .animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(208.dp), contentAlignment = Alignment.Center) {
            if (running) VerifyPulse(color = cs.primary)
            Canvas(modifier = Modifier.size(184.dp)) {
                val stroke = 14.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                drawArc(
                    color = track,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                val value = ring().coerceIn(0f, 1f)
                if (value > 0.005f) {
                    rotate(-90f) {
                        drawArc(
                            brush = Brush.sweepGradient(gradient, center = center),
                            startAngle = 0f,
                            sweepAngle = 360f * value,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                }
            }
            AnimatedContent(
                targetState = phase,
                transitionSpec = {
                    (fadeIn(tween(300, delayMillis = 100)) + scaleIn(initialScale = 0.8f, animationSpec = tween(400, delayMillis = 100))) togetherWith
                        (fadeOut(tween(160)) + scaleOut(targetScale = 1.1f, animationSpec = tween(160)))
                },
                label = "verifyCenter",
            ) { p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (p == VerifyPhase.Running) {
                        VerifyNumber(value = { ring() * 100f }, suffix = "%", big = false)
                        Text("проверка", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    } else {
                        VerifyNumber(value = score, suffix = "", big = true)
                        Text("из 100", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        AnimatedContent(
            targetState = phase,
            transitionSpec = {
                (fadeIn(tween(300, delayMillis = 120)) + slideInVertically(tween(400, delayMillis = 120)) { it / 3 }) togetherWith
                    fadeOut(tween(150)) using SizeTransform(clip = false)
            },
            label = "verifyBelow",
        ) { p ->
            if (p == VerifyPhase.Running) {
                VerifyStepLabel(progress = ring)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Хорошее состояние",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Почти все компоненты заводские. Заменена батарея.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VerifyStat(Icons.Rounded.CheckCircle, 11, "Совпадает", cs.primary, Modifier.weight(1f))
                        VerifyStat(Icons.Rounded.Warning, 1, "Заменено", cs.error, Modifier.weight(1f))
                        VerifyStat(Icons.Rounded.HelpOutline, 0, "Не определено", cs.secondary, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun VerifyPulse(color: Color) {
    val transition = rememberInfiniteTransition(label = "verifyPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val radius = size.minDimension / 2f * (0.82f + 0.18f * pulse)
                drawCircle(color = color.copy(alpha = 0.16f * (1f - pulse)), radius = radius)
            },
    )
}

@Composable
private fun VerifyNumber(value: () -> Float, suffix: String, big: Boolean) {
    Text(
        text = "${Math.round(value())}$suffix",
        style = if (big) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun VerifyStepLabel(progress: () -> Float) {
    val cs = MaterialTheme.colorScheme
    val step = verifyStep(progress())
    AnimatedContent(
        targetState = step,
        transitionSpec = {
            (fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 2 }) togetherWith
                (fadeOut(tween(150)) + slideOutVertically(tween(200)) { -it / 2 })
        },
        label = "verifyStep",
    ) { label ->
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun VerifyStat(icon: ImageVector, count: Int, label: String, accent: Color, modifier: Modifier = Modifier) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(count) {
        shown.animateTo(count.toFloat(), tween(800, delayMillis = 200, easing = FastOutSlowInEasing))
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        VerifyStatNumber(value = { shown.value }, color = accent)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun VerifyStatNumber(value: () -> Float, color: Color) {
    Text(
        text = Math.round(value()).toString(),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

// ---------------------------------------------------------------------------------------------
// Actions
// ---------------------------------------------------------------------------------------------

@Composable
private fun VerifyActions(
    running: Boolean,
    saved: Boolean,
    onRerun: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val brush = remember(cs.primary, cs.tertiary) { Brush.linearGradient(listOf(cs.primary, cs.tertiary)) }
    val enabledAlpha by animateFloatAsState(if (running) 0.4f else 1f, tween(300), label = "actionsAlpha")
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.graphicsLayer { alpha = enabledAlpha },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VerifyButton(
                text = "Проверить снова",
                icon = Icons.Rounded.Refresh,
                background = Modifier.background(cs.surfaceContainerHigh),
                content = cs.onSurface,
                enabled = !running,
                onClick = onRerun,
                modifier = Modifier.weight(1f),
            )
            VerifyButton(
                text = "Сохранить отчёт",
                icon = Icons.Rounded.Save,
                background = Modifier.background(brush),
                content = cs.onPrimary,
                enabled = !running,
                onClick = onSave,
                modifier = Modifier.weight(1f),
            )
        }
        AnimatedVisibility(
            visible = saved,
            enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically(tween(250)) + fadeOut(tween(200)),
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cs.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = cs.primary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Отчёт сохранён", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                    Text(
                        text = "«Загрузки» · проверка_устройства.pdf",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun VerifyButton(
    text: String,
    icon: ImageVector,
    background: Modifier,
    content: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .verifyPress(source)
            .height(52.dp)
            .clip(CircleShape)
            .then(background)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------
// Component rows
// ---------------------------------------------------------------------------------------------

@Composable
private fun VerifyRow(part: VerifyPart, expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val accent = if (part.replaced) cs.error else cs.primary
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verifyPress(source)
            .clip(RoundedCornerShape(20.dp))
            .background(cs.surfaceContainer)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(part.icon, contentDescription = null, tint = accent)
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = part.title,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                modifier = Modifier.weight(1f),
            )
            VerifyStatusChip(replaced = part.replaced)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VerifyValue("Заводское", part.factory, cs.onSurface, Modifier.weight(1f))
            VerifyValue("Текущее", part.current, if (part.replaced) cs.error else cs.onSurface, Modifier.weight(1f))
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically(tween(220)) + fadeOut(tween(180)),
        ) {
            Text(
                text = part.note,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, start = 2.dp, end = 2.dp),
            )
        }
    }
}

@Composable
private fun VerifyValue(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(cs.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp,
            color = color,
        )
    }
}

@Composable
private fun VerifyStatusChip(replaced: Boolean) {
    val cs = MaterialTheme.colorScheme
    val accent = if (replaced) cs.error else cs.primary
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.12f))
            .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (replaced) Icons.Rounded.Warning else Icons.Rounded.Check,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (replaced) "Заменено" else "Совпадает",
            style = MaterialTheme.typography.labelMedium,
            color = accent,
        )
    }
}

@Composable
private fun VerifyPlaceholders(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "verifyShimmer")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer",
    )
    val base = cs.surfaceContainer
    val highlight = cs.surfaceContainerHighest
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .drawBehind {
                        val x = size.width * shift
                        drawRect(
                            Brush.linearGradient(
                                colors = listOf(base, highlight, base),
                                start = Offset(x - size.width * 0.6f, 0f),
                                end = Offset(x, size.height),
                            ),
                        )
                    },
            )
        }
    }
}
