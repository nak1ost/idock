package dev.idock.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------------------------------------------------------------------------------------------
// Demo data
// ---------------------------------------------------------------------------------------------

private enum class BackupPhase { Idle, Running, Done }

private data class BackupEntry(
    val id: Long,
    val date: String,
    val sizeLabel: String,
    val ios: String,
    val encrypted: Boolean,
    val note: String,
)

private data class BackupCategory(val id: String, val label: String, val sizeGb: Float)

private val BackupCategories = listOf(
    BackupCategory("photos", "Фото", 11.2f),
    BackupCategory("messages", "Сообщения", 1.8f),
    BackupCategory("contacts", "Контакты", 0.1f),
    BackupCategory("apps", "Приложения", 4.9f),
    BackupCategory("settings", "Настройки", 0.4f),
)

private val BackupInitialEntries = listOf(
    BackupEntry(1, "11 ноября, 21:40", "18,4 ГБ", "iOS 18.1", false, "Фото, сообщения, контакты, приложения и настройки"),
    BackupEntry(2, "28 октября, 09:15", "17,9 ГБ", "iOS 18.0.1", true, "Полная копия, защищена паролем"),
    BackupEntry(3, "2 октября, 23:02", "16,2 ГБ", "iOS 18.0", true, "Полная копия, защищена паролем"),
    BackupEntry(4, "14 сентября, 18:30", "6,3 ГБ", "iOS 17.6", false, "Сообщения, контакты, приложения и настройки"),
)

private val BackupSteps = listOf(
    "Подготовка…",
    "Фото и видео",
    "Приложения и данные",
    "Настройки",
    "Завершение…",
)

private const val BackupDurationMs = 6000f
private const val BackupStaggerMs = 35f
private const val BackupEnterMs = 350f
private const val BackupStaggerCap = 10

private val BackupRu: Locale = Locale.forLanguageTag("ru")

private fun backupStepFor(p: Float): Int = when {
    p < 0.10f -> 0
    p < 0.52f -> 1
    p < 0.80f -> 2
    p < 0.93f -> 3
    else -> 4
}

/** Uneven, believable progress curve for the simulation. */
private fun backupSimCurve(t: Float): Float {
    val smooth = t * t * (3f - 2f * t)
    return (smooth * 0.35f + t * 0.65f).coerceIn(0f, 1f)
}

private fun backupCountLabel(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    val word = when {
        mod10 == 1 && mod100 != 11 -> "копия"
        mod10 in 2..4 && mod100 !in 12..14 -> "копии"
        else -> "копий"
    }
    return "$n $word"
}

private fun backupGb(v: Float): String = "%.1f ГБ".format(BackupRu, v)

// ---------------------------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current

    var phase by remember { mutableStateOf(BackupPhase.Idle) }
    val ring = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    var sim by remember { mutableFloatStateOf(0f) }

    val backups = remember { mutableStateListOf<BackupEntry>().apply { addAll(BackupInitialEntries) } }
    var nextId by remember { mutableLongStateOf(100L) }
    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }

    var encrypt by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val included = remember {
        mutableStateMapOf<String, Boolean>().apply { BackupCategories.forEach { put(it.id, true) } }
    }
    val totalGb by remember {
        derivedStateOf { BackupCategories.filter { included[it.id] == true }.sumOf { it.sizeGb.toDouble() }.toFloat() }
    }

    var restoreTarget by remember { mutableStateOf<BackupEntry?>(null) }
    var restoringId by remember { mutableStateOf<Long?>(null) }
    var restoredId by remember { mutableStateOf<Long?>(null) }
    val restoreProgress = remember { Animatable(0f) }

    val enterClock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val total = BackupStaggerMs * BackupStaggerCap + BackupEnterMs
        enterClock.animateTo(total, tween(total.toInt(), easing = LinearEasing))
    }

    // Backup simulation state machine.
    LaunchedEffect(phase) {
        when (phase) {
            BackupPhase.Idle -> {
                check.snapTo(0f)
                ring.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            }

            BackupPhase.Running -> {
                sim = 0f
                ring.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
                val start = withFrameNanos { it }
                while (true) {
                    val now = withFrameNanos { it }
                    val t = ((now - start) / 1_000_000f / BackupDurationMs).coerceIn(0f, 1f)
                    val p = backupSimCurve(t)
                    sim = p
                    ring.snapTo(p)
                    if (t >= 1f) break
                }
                phase = BackupPhase.Done
            }

            BackupPhase.Done -> {
                check.snapTo(0f)
                ring.animateTo(1f, tween(200))
                check.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
                delay(1300)
                val parts = BackupCategories.filter { included[it.id] == true }
                val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                backups.add(
                    0,
                    BackupEntry(
                        id = nextId,
                        date = "Сегодня, $time",
                        sizeLabel = backupGb(parts.sumOf { it.sizeGb.toDouble() }.toFloat()),
                        ios = "iOS 18.1",
                        encrypted = encrypt,
                        note = parts.joinToString(", ") { it.label },
                    ),
                )
                nextId += 1
                phase = BackupPhase.Idle
            }
        }
    }

    // Restore simulation.
    LaunchedEffect(restoringId) {
        val id = restoringId ?: return@LaunchedEffect
        restoreProgress.snapTo(0f)
        restoreProgress.animateTo(1f, tween(4200, easing = LinearOutSlowInEasing))
        restoredId = id
        restoringId = null
    }
    LaunchedEffect(restoredId) {
        if (restoredId != null) {
            delay(2400)
            restoredId = null
        }
    }

    val busy = phase != BackupPhase.Idle || restoringId != null
    val passwordOk = !encrypt || password.length >= 4
    val anyIncluded = included.values.any { it }
    val canStart = !busy && passwordOk && anyIncluded

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection) + 20.dp,
            end = contentPadding.calculateEndPadding(layoutDirection) + 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ----- Hero -----
        item(key = "hero") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .backupEnter(enterClock, 0),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val emphasis by animateFloatAsState(
                        targetValue = if (phase == BackupPhase.Idle) 0.38f else 1f,
                        animationSpec = tween(500),
                        label = "ringEmphasis",
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(232.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BackupRing(
                            progress = { ring.value },
                            emphasis = { emphasis },
                            showHead = phase == BackupPhase.Running,
                            modifier = Modifier.fillMaxSize(),
                        )
                        AnimatedContent(
                            targetState = phase,
                            transitionSpec = {
                                (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.9f)) togetherWith
                                    (fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 1.05f))
                            },
                            label = "ringCenter",
                        ) { state ->
                            when (state) {
                                BackupPhase.Idle -> BackupIdleCenter(latest = backups.firstOrNull())
                                BackupPhase.Running -> BackupRunningCenter(progress = { sim })
                                BackupPhase.Done -> BackupDoneCenter(check = check)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    AnimatedContent(
                        targetState = phase,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                        label = "heroAction",
                    ) { state ->
                        when (state) {
                            BackupPhase.Idle -> BackupGradientButton(
                                text = "Создать резервную копию",
                                icon = Icons.Rounded.Backup,
                                enabled = canStart,
                                onClick = {
                                    expandedId = null
                                    phase = BackupPhase.Running
                                },
                            )

                            BackupPhase.Running -> OutlinedButton(
                                onClick = { phase = BackupPhase.Idle },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = CircleShape,
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Отменить", style = MaterialTheme.typography.labelLarge)
                            }

                            BackupPhase.Done -> Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Копия сохранена на этом телефоне",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    val hint = when {
                        phase != BackupPhase.Idle -> null
                        restoringId != null -> "Дождитесь окончания восстановления"
                        !anyIncluded -> "Выберите хотя бы один раздел"
                        !passwordOk -> "Задайте пароль для шифрования"
                        else -> null
                    }
                    AnimatedVisibility(
                        visible = hint != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Text(
                            text = hint ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                        )
                    }
                }
            }
        }

        // ----- Encryption -----
        item(key = "encrypt") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .backupEnter(enterClock, 1),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BackupIconTile(Icons.Rounded.Lock, MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Шифровать копию",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Сохраняет пароли, здоровье и связку ключей",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = encrypt,
                            onCheckedChange = { encrypt = it },
                            enabled = phase == BackupPhase.Idle,
                        )
                    }
                    AnimatedVisibility(
                        visible = encrypt,
                        enter = fadeIn(tween(300)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                        exit = fadeOut(tween(200)) + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                    ) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it.take(64) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                            label = { Text("Пароль копии") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (showPassword) "Скрыть пароль" else "Показать пароль",
                                    )
                                }
                            },
                            supportingText = {
                                Text(
                                    if (password.length >= 4) "Без пароля восстановить копию не получится" else "Минимум 4 символа",
                                )
                            },
                        )
                    }
                }
            }
        }

        // ----- Contents -----
        item(key = "contents") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .backupEnter(enterClock, 2),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Что входит в копию",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        BackupAnimatedSize(totalGb = totalGb)
                    }
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BackupCategories.forEach { cat ->
                            val selected = included[cat.id] == true
                            FilterChip(
                                selected = selected,
                                onClick = { included[cat.id] = !selected },
                                enabled = phase == BackupPhase.Idle,
                                label = { Text(cat.label, style = MaterialTheme.typography.labelLarge) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                                        )
                                    }
                                } else {
                                    null
                                },
                                shape = CircleShape,
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .animateContentSize(spring(stiffness = Spring.StiffnessMedium)),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Копия хранится на этом телефоне и не покидает его",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ----- History -----
        item(key = "history-header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .backupEnter(enterClock, 3)
                    .padding(top = 12.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "Предыдущие копии",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                AnimatedContent(
                    targetState = backups.size,
                    transitionSpec = {
                        val up = targetState > initialState
                        (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith
                            (slideOutVertically { if (up) -it else it } + fadeOut())
                    },
                    label = "historyCount",
                ) { n ->
                    Text(
                        text = if (n == 0) "нет" else backupCountLabel(n),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (backups.isEmpty()) {
            item(key = "history-empty") {
                BackupEmptyState(Modifier.animateItem())
            }
        }

        items(backups, key = { it.id }) { entry ->
            val index = backups.indexOf(entry)
            BackupRow(
                entry = entry,
                isLatest = index == 0,
                expanded = expandedId == entry.id,
                restoring = restoringId == entry.id,
                restored = restoredId == entry.id,
                restoreProgress = { restoreProgress.value },
                actionsEnabled = !busy,
                onToggle = { expandedId = if (expandedId == entry.id) null else entry.id },
                onRestore = { restoreTarget = entry },
                onDelete = {
                    if (expandedId == entry.id) expandedId = null
                    backups.remove(entry)
                },
                modifier = Modifier
                    .animateItem()
                    .backupEnter(enterClock, 4 + index),
            )
        }
    }

    restoreTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { restoreTarget = null },
            icon = { Icon(Icons.Rounded.Restore, contentDescription = null) },
            title = { Text("Восстановить из копии?") },
            text = {
                Text(
                    buildString {
                        append("Данные на iPhone будут заменены содержимым копии от ")
                        append(target.date)
                        append(". Фото, сообщения и настройки, которых нет в копии, будут удалены. ")
                        append("Не отключайте кабель до завершения.")
                        if (target.encrypted) append(" Понадобится пароль копии.")
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        restoreTarget = null
                        restoringId = target.id
                    },
                ) { Text("Восстановить") }
            },
            dismissButton = {
                TextButton(onClick = { restoreTarget = null }) { Text("Отмена") }
            },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Motion helpers
// ---------------------------------------------------------------------------------------------

private fun Modifier.backupEnter(
    clock: Animatable<Float, AnimationVector1D>,
    index: Int,
): Modifier = this.graphicsLayer {
    val p = if (index >= BackupStaggerCap) {
        1f
    } else {
        FastOutSlowInEasing.transform(((clock.value - index * BackupStaggerMs) / BackupEnterMs).coerceIn(0f, 1f))
    }
    alpha = p
    translationY = (1f - p) * 24.dp.toPx()
}

@Composable
private fun backupPressScale(interaction: MutableInteractionSource): () -> Float {
    val pressed by interaction.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "backupPress",
    )
    return { scale.value }
}

// ---------------------------------------------------------------------------------------------
// Ring + centers
// ---------------------------------------------------------------------------------------------

@Composable
private fun BackupRing(
    progress: () -> Float,
    emphasis: () -> Float,
    showHead: Boolean,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val track = cs.surfaceContainerHighest
    val colors = remember(cs.primary, cs.tertiary) { listOf(cs.primary, cs.tertiary) }
    Canvas(modifier) {
        val stroke = 16.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke),
        )
        val p = progress().coerceIn(0f, 1f)
        if (p > 0.002f) {
            drawArc(
                brush = Brush.linearGradient(colors, start = Offset.Zero, end = Offset(size.width, size.height)),
                startAngle = -90f,
                sweepAngle = 360f * p,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                alpha = emphasis(),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (showHead) {
                val angle = (-90f + 360f * p) * (PI.toFloat() / 180f)
                val r = (size.minDimension - stroke) / 2f
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = stroke * 0.22f,
                    center = Offset(center.x + r * cos(angle), center.y + r * sin(angle)),
                )
            }
        }
    }
}

@Composable
private fun BackupIdleCenter(latest: BackupEntry?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (latest != null) "Последняя копия" else "Копий пока нет",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = latest?.sizeLabel ?: "—",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = latest?.date ?: "Создайте первую",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BackupRunningCenter(progress: () -> Float) {
    val step by remember { derivedStateOf { backupStepFor(progress()) } }
    val secondsLeft by remember { derivedStateOf { ceil((1f - progress()) * BackupDurationMs / 1000f).toInt() } }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BackupPercent(progress)
        Spacer(Modifier.height(2.dp))
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInVertically(tween(300)) { it / 2 } + fadeIn(tween(300))) togetherWith
                    (slideOutVertically(tween(250)) { -it / 2 } + fadeOut(tween(200)))
            },
            label = "backupStep",
        ) { s ->
            Text(
                text = BackupSteps[s],
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (secondsLeft > 0) "Осталось около $secondsLeft с" else "Почти готово",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Isolated so that only this text recomposes on every frame of the simulation. */
@Composable
private fun BackupPercent(progress: () -> Float) {
    val value = (progress() * 100f).roundToInt()
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "%",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp),
        )
    }
}

@Composable
private fun BackupDoneCenter(check: Animatable<Float, AnimationVector1D>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BackupCheckMark(progress = { check.value }, modifier = Modifier.size(84.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Готово",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BackupCheckMark(progress: () -> Float, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val path = remember { Path() }
    val segment = remember { Path() }
    val measure = remember { PathMeasure() }
    Canvas(modifier) {
        val p = progress().coerceIn(0f, 1f)
        val pop = FastOutSlowInEasing.transform(min(1f, p * 1.8f))
        drawCircle(
            color = color.copy(alpha = 0.14f),
            radius = size.minDimension / 2f * (0.55f + 0.45f * pop),
        )
        val w = size.width
        val h = size.height
        path.reset()
        path.moveTo(w * 0.30f, h * 0.52f)
        path.lineTo(w * 0.44f, h * 0.66f)
        path.lineTo(w * 0.71f, h * 0.37f)
        measure.setPath(path, false)
        segment.reset()
        measure.getSegment(0f, measure.length * p, segment, true)
        drawPath(
            path = segment,
            color = color,
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Controls
// ---------------------------------------------------------------------------------------------

@Composable
private fun BackupGradientButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val scale = backupPressScale(interaction)
    val brush = remember(cs.primary, cs.tertiary) { Brush.linearGradient(listOf(cs.primary, cs.tertiary)) }
    val contentColor = if (enabled) cs.onPrimary else cs.onSurfaceVariant
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
            .then(
                if (enabled) Modifier.background(brush) else Modifier.background(cs.surfaceContainerHighest),
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = ripple(color = cs.onPrimary),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun BackupAnimatedSize(totalGb: Float) {
    val animated by animateFloatAsState(
        targetValue = totalGb,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "backupSize",
    )
    Text(
        text = "≈ " + backupGb(animated),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun BackupIconTile(icon: ImageVector, accent: Color) {
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

// ---------------------------------------------------------------------------------------------
// History rows
// ---------------------------------------------------------------------------------------------

@Composable
private fun BackupRow(
    entry: BackupEntry,
    isLatest: Boolean,
    expanded: Boolean,
    restoring: Boolean,
    restored: Boolean,
    restoreProgress: () -> Float,
    actionsEnabled: Boolean,
    onToggle: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val scale = backupPressScale(interaction)
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "chevron",
    )

    Surface(
        onClick = onToggle,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            },
        shape = RoundedCornerShape(20.dp),
        color = cs.surfaceContainer,
        interactionSource = interaction,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackupIconTile(
                    icon = Icons.Rounded.History,
                    accent = if (isLatest) cs.primary else cs.secondary,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = entry.date,
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface,
                    )
                    Text(
                        text = "${entry.sizeLabel} · ${entry.ios}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
                if (entry.encrypted) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Зашифрована",
                        tint = cs.tertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Свернуть" else "Подробнее",
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { rotationZ = chevron },
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(250)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(tween(150)) + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)),
            ) {
                Column(Modifier.padding(top = 14.dp)) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    val mode = when {
                        restoring -> 1
                        restored -> 2
                        else -> 0
                    }
                    AnimatedContent(
                        targetState = mode,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                        label = "rowActions",
                    ) { m ->
                        when (m) {
                            1 -> BackupRestoreBar(progress = restoreProgress)
                            2 -> Row(
                                modifier = Modifier.heightIn(min = 48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = cs.primary)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Данные восстановлены",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = cs.primary,
                                )
                            }

                            else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilledTonalButton(
                                    onClick = onRestore,
                                    enabled = actionsEnabled,
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .heightIn(min = 48.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                ) {
                                    Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Восстановить", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = onDelete,
                                    enabled = actionsEnabled,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.error),
                                    border = BorderStroke(1.dp, cs.error.copy(alpha = if (actionsEnabled) 0.45f else 0.15f)),
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Удалить", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupRestoreBar(progress: () -> Float) {
    val cs = MaterialTheme.colorScheme
    val colors = remember(cs.primary, cs.tertiary) { listOf(cs.primary, cs.tertiary) }
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Восстановление…",
                style = MaterialTheme.typography.labelLarge,
                color = cs.onSurface,
                modifier = Modifier.weight(1f),
            )
            BackupRestorePercent(progress)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(cs.surfaceContainerHighest)
                .drawBehind {
                    val p = progress().coerceIn(0f, 1f)
                    if (p > 0f) {
                        drawRoundRect(
                            brush = Brush.linearGradient(colors, start = Offset.Zero, end = Offset(size.width, 0f)),
                            size = Size(size.width * p, size.height),
                            cornerRadius = CornerRadius(size.height / 2f),
                        )
                    }
                },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Не отключайте кабель до завершения",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun BackupRestorePercent(progress: () -> Float) {
    Text(
        text = "${(progress() * 100f).roundToInt()}%",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun BackupEmptyState(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = cs.surfaceContainerLow,
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(cs.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = cs.primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Копий пока нет",
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Создайте первую резервную копию — она появится здесь",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
