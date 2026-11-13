package dev.idock.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

// ---------------------------------------------------------------------------------------------
// Demo data
// ---------------------------------------------------------------------------------------------

private enum class LogsLevel(val tag: String, val label: String) {
    Error("E", "Ошибки"),
    Warning("W", "Предупреждения"),
    Info("I", "Инфо"),
    Debug("D", "Отладка"),
}

private data class LogsEntry(
    val id: Long,
    val time: String,
    val process: String,
    val pid: Int,
    val level: LogsLevel,
    val message: String,
    val bornAt: Long,
)

private data class LogsTemplate(val process: String, val pid: Int, val level: LogsLevel, val message: String)

private val LogsPool = listOf(
    LogsTemplate("kernel", 0, LogsLevel.Info, "AppleUSBDevice: device configured, speed high"),
    LogsTemplate("kernel", 0, LogsLevel.Debug, "memorystatus: pressure level normal, free pages #"),
    LogsTemplate("kernel", 0, LogsLevel.Warning, "thermal: sensor reading above nominal (# mC)"),
    LogsTemplate("SpringBoard", 64, LogsLevel.Info, "Scene update completed for foreground app"),
    LogsTemplate("SpringBoard", 64, LogsLevel.Debug, "Icon layout cache hit, # items"),
    LogsTemplate("SpringBoard", 64, LogsLevel.Warning, "Snapshot request timed out after # ms"),
    LogsTemplate("backboardd", 58, LogsLevel.Debug, "Display brightness ramp to #%"),
    LogsTemplate("backboardd", 58, LogsLevel.Info, "Touch event stream resumed"),
    LogsTemplate("wifid", 92, LogsLevel.Info, "Network scan finished: # networks found"),
    LogsTemplate("wifid", 92, LogsLevel.Warning, "Link quality degraded, RSSI -# dBm"),
    LogsTemplate("wifid", 92, LogsLevel.Error, "Association attempt failed, retrying in # s"),
    LogsTemplate("locationd", 87, LogsLevel.Debug, "Location request served with accuracy # m"),
    LogsTemplate("locationd", 87, LogsLevel.Info, "Significant location change monitoring enabled"),
    LogsTemplate("bluetoothd", 101, LogsLevel.Info, "Accessory connected, battery #%"),
    LogsTemplate("bluetoothd", 101, LogsLevel.Error, "Pairing record not found for accessory"),
    LogsTemplate("mediaserverd", 77, LogsLevel.Debug, "Audio route changed to speaker"),
    LogsTemplate("mediaserverd", 77, LogsLevel.Warning, "Buffer underrun detected (# frames)"),
    LogsTemplate("powerd", 49, LogsLevel.Info, "Battery level #%, charging via USB"),
    LogsTemplate("powerd", 49, LogsLevel.Debug, "Idle sleep timer reset to # s"),
    LogsTemplate("runningboardd", 70, LogsLevel.Info, "Process assertion acquired for background task"),
    LogsTemplate("runningboardd", 70, LogsLevel.Warning, "Process exceeded CPU budget, throttling for # s"),
    LogsTemplate("nsurlsessiond", 112, LogsLevel.Debug, "Background transfer progress #%"),
    LogsTemplate("nsurlsessiond", 112, LogsLevel.Error, "Request failed: connection reset by peer"),
    LogsTemplate("CommCenter", 83, LogsLevel.Info, "Cellular signal strength # bars"),
    LogsTemplate("lockdownd", 41, LogsLevel.Info, "Pairing session validated for host"),
    LogsTemplate("lockdownd", 41, LogsLevel.Debug, "Service request handled in # ms"),
    LogsTemplate("thermalmonitord", 95, LogsLevel.Warning, "Thermal state changed to fair"),
    LogsTemplate("healthd", 120, LogsLevel.Debug, "Database maintenance pass took # ms"),
)

private const val LogsMaxLines = 500
private const val LogsRateBuckets = 30
private val LogsTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

private fun logsLinesLabel(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    val word = when {
        mod10 == 1 && mod100 != 11 -> "строка"
        mod10 in 2..4 && mod100 !in 12..14 -> "строки"
        else -> "строк"
    }
    return "$n $word"
}

private fun logsMatches(entry: LogsEntry, level: LogsLevel?, query: String): Boolean =
    (level == null || entry.level == level) &&
        (query.isBlank() || entry.message.contains(query, ignoreCase = true) || entry.process.contains(query, ignoreCase = true))

private fun AnnotatedString.Builder.logsAppendHighlighted(text: String, query: String, base: SpanStyle, hit: SpanStyle) {
    if (query.isBlank()) {
        withStyle(base) { append(text) }
        return
    }
    var i = 0
    while (i <= text.length) {
        val idx = text.indexOf(query, startIndex = i, ignoreCase = true)
        if (idx < 0) {
            withStyle(base) { append(text.substring(i)) }
            return
        }
        withStyle(base) { append(text.substring(i, idx)) }
        withStyle(hit) { append(text.substring(idx, idx + query.length)) }
        i = idx + query.length
    }
}

// ---------------------------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------------------------

@Composable
fun LogsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val scope = rememberCoroutineScope()
    val side = Modifier.padding(horizontal = 20.dp)
    val bottomPad = contentPadding.calculateBottomPadding()

    val lines = remember { mutableStateListOf<LogsEntry>() }
    var running by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf<LogsLevel?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var follow by remember { mutableStateOf(true) }
    var unseen by remember { mutableIntStateOf(0) }
    val rate = remember { mutableStateListOf<Int>().apply { repeat(LogsRateBuckets) { add(0) } } }
    val counter = remember { IntArray(1) }
    val nextId = remember { LongArray(1) }
    val listState = rememberLazyListState()

    val visible by remember { derivedStateOf { lines.filter { logsMatches(it, filter, query) } } }
    val counts by remember { derivedStateOf { lines.groupingBy { it.level }.eachCount() } }

    // Line generator.
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val rnd = Random(System.nanoTime())
        while (true) {
            delay(rnd.nextLong(250L, 501L))
            val burst = if (rnd.nextInt(8) == 0) 3 else 1
            repeat(burst) {
                val t = LogsPool[rnd.nextInt(LogsPool.size)]
                val entry = LogsEntry(
                    id = nextId[0]++,
                    time = LocalTime.now().format(LogsTimeFormat),
                    process = t.process,
                    pid = t.pid,
                    level = t.level,
                    message = t.message.replace("#", rnd.nextInt(2, 980).toString()),
                    bornAt = System.currentTimeMillis(),
                )
                lines.add(entry)
                while (lines.size > LogsMaxLines) lines.removeAt(0)
                counter[0]++
                if (!follow && logsMatches(entry, filter, query)) unseen++
            }
        }
    }

    // Lines-per-second buckets for the sparkline.
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        counter[0] = 0
        while (true) {
            delay(1000)
            rate.add(counter[0])
            rate.removeAt(0)
            counter[0] = 0
        }
    }

    // Follow mode: a user drag away from the bottom stops auto-scroll; reaching the bottom resumes it.
    val dragged by listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(listState) {
        snapshotFlow { dragged to listState.canScrollForward }.collect { (isDragged, canForward) ->
            if (isDragged && canForward) follow = false else if (!canForward) follow = true
        }
    }
    LaunchedEffect(follow) {
        if (!follow) return@LaunchedEffect
        unseen = 0
        snapshotFlow { visible.size to visible.lastOrNull()?.id }.collectLatest { (size, _) ->
            if (size > 0) listState.animateScrollToItem(size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection),
                top = contentPadding.calculateTopPadding() + 8.dp,
            ),
    ) {
        // ----- Toolbar -----
        Row(
            modifier = side.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LogsRunButton(running = running, onClick = { running = !running })
            LogsSearchField(query = query, onQueryChange = { query = it.take(40) }, modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    lines.clear()
                    expandedId = null
                    unseen = 0
                    follow = true
                },
                enabled = lines.isNotEmpty(),
            ) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = "Очистить журнал")
            }
        }

        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "all") {
                FilterChip(
                    selected = filter == null,
                    onClick = { filter = null },
                    label = { Text("Все · ${lines.size}", style = MaterialTheme.typography.labelLarge) },
                    shape = CircleShape,
                )
            }
            items(LogsLevel.entries, key = { it.name }) { level ->
                val color = logsLevelColor(level)
                FilterChip(
                    selected = filter == level,
                    onClick = { filter = if (filter == level) null else level },
                    label = { Text("${level.label} · ${counts[level] ?: 0}", style = MaterialTheme.typography.labelLarge) },
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color),
                        )
                    },
                    shape = CircleShape,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        LogsStatusRow(
            running = running,
            total = lines.size,
            rate = rate,
            modifier = side,
        )
        Spacer(Modifier.height(8.dp))

        // ----- Log panel -----
        Box(
            modifier = side
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = 8.dp, bottom = bottomPad + 16.dp),
                ) {
                    items(visible, key = { it.id }) { entry ->
                        LogsLine(
                            entry = entry,
                            query = query,
                            expanded = expandedId == entry.id,
                            onToggle = { expandedId = if (expandedId == entry.id) null else entry.id },
                        )
                    }
                }
            }

            dev.idock.app.ui.kit.PlainAnimatedVisibility(
                visible = visible.isEmpty(),
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(150)),
                modifier = Modifier
                    .matchParentSize()
                    .padding(bottom = bottomPad),
            ) {
                LogsEmptyState(
                    text = when {
                        lines.isEmpty() && !running -> "Нажмите «Старт», чтобы начать чтение журнала"
                        lines.isEmpty() -> "Ожидание сообщений от устройства…"
                        else -> "Нет строк, подходящих под фильтр"
                    },
                )
            }

            dev.idock.app.ui.kit.PlainAnimatedVisibility(
                visible = !follow && visible.isNotEmpty(),
                enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
                exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPad + 16.dp),
            ) {
                LogsJumpPill(
                    unseen = unseen,
                    onClick = {
                        scope.launch {
                            val last = visible.lastIndex
                            if (last >= 0) {
                                if (last - listState.firstVisibleItemIndex > 60) listState.scrollToItem(last - 30)
                                listState.animateScrollToItem(last)
                            }
                            follow = true
                        }
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun logsLevelColor(level: LogsLevel): Color {
    val cs = MaterialTheme.colorScheme
    return when (level) {
        LogsLevel.Error -> cs.error
        LogsLevel.Warning -> cs.tertiary
        LogsLevel.Info -> cs.primary
        LogsLevel.Debug -> cs.onSurfaceVariant
    }
}

@Composable
private fun LogsRunButton(running: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val brush = remember(cs.primary, cs.tertiary) { Brush.linearGradient(listOf(cs.primary, cs.tertiary)) }
    val content = if (running) cs.onSecondaryContainer else cs.onPrimary
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(CircleShape)
            .then(if (running) Modifier.background(cs.secondaryContainer) else Modifier.background(brush))
            .clickable(role = Role.Button, onClick = onClick)
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .padding(start = 14.dp, end = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = running,
            transitionSpec = {
                (scaleIn(tween(220), initialScale = 0.7f) + fadeIn(tween(220))) togetherWith
                    (scaleOut(tween(150), targetScale = 0.7f) + fadeOut(tween(150)))
            },
            label = "runButton",
        ) { isRunning ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isRunning) "Пауза" else "Старт",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = content,
                )
            }
        }
    }
}

@Composable
private fun LogsSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = cs.onSurface),
        cursorBrush = SolidColor(cs.primary),
        modifier = modifier.height(48.dp),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(cs.surfaceContainerHigh)
                    .padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text("Поиск", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    }
                    inner()
                }
                AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Очистить поиск", modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
    )
}

@Composable
private fun LogsStatusRow(running: Boolean, total: Int, rate: List<Int>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (running) LogsRecordingDot() else Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(cs.onSurfaceVariant.copy(alpha = 0.4f)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (running) "Запись" else "Остановлено",
            style = MaterialTheme.typography.labelMedium,
            color = if (running) cs.error else cs.onSurfaceVariant,
        )
        Text(
            text = "  ·  " + logsLinesLabel(total),
            style = MaterialTheme.typography.labelMedium,
            color = cs.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${rate.lastOrNull() ?: 0} стр/с",
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = cs.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        LogsSparkline(rate = rate, modifier = Modifier.size(width = 64.dp, height = 20.dp))
    }
}

/** Pulses only while recording — composed only in that state. */
@Composable
private fun LogsRecordingDot() {
    val color = MaterialTheme.colorScheme.error
    val transition = rememberInfiniteTransition(label = "rec")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Restart),
        label = "recPulse",
    )
    Canvas(Modifier.size(8.dp)) {
        drawCircle(color = color, radius = size.minDimension / 2f * (1f + pulse * 0.9f), alpha = 0.35f * (1f - pulse))
        drawCircle(color = color, radius = size.minDimension / 2f)
    }
}

@Composable
private fun LogsSparkline(rate: List<Int>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val colors = remember(cs.primary, cs.tertiary) { listOf(cs.primary, cs.tertiary) }
    val track = cs.outlineVariant.copy(alpha = 0.5f)
    Canvas(modifier) {
        val n = rate.size
        if (n == 0) return@Canvas
        val max = (rate.maxOrNull() ?: 0).coerceAtLeast(4).toFloat()
        val slot = size.width / n
        val barW = slot * 0.6f
        val brush = Brush.linearGradient(colors, start = Offset.Zero, end = Offset(size.width, 0f))
        for (i in 0 until n) {
            val v = rate[i]
            val h = if (v == 0) barW else (v / max) * size.height
            val topLeft = Offset(i * slot + (slot - barW) / 2f, size.height - h)
            if (v == 0) {
                drawRoundRect(color = track, topLeft = topLeft, size = Size(barW, h), cornerRadius = CornerRadius(barW / 2f))
            } else {
                drawRoundRect(brush = brush, topLeft = topLeft, size = Size(barW, h), cornerRadius = CornerRadius(barW / 2f))
            }
        }
    }
}

@Composable
private fun LogsLine(
    entry: LogsEntry,
    query: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val mono = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp)
    val levelColor = logsLevelColor(entry.level)
    val clipboard = LocalClipboardManager.current

    // Fade in only lines that were just born — scrolling back to old lines does not replay it.
    val appear = remember { Animatable(if (System.currentTimeMillis() - entry.bornAt < 400L) 0f else 1f) }
    LaunchedEffect(Unit) { if (appear.value < 1f) appear.animateTo(1f, tween(300)) }

    val background by animateColorAsState(
        targetValue = if (expanded) cs.surfaceContainerHigh else cs.surfaceContainerHigh.copy(alpha = 0f),
        animationSpec = tween(250),
        label = "lineBg",
    )
    val processColor = cs.onSurface
    val messageColor = cs.onSurface.copy(alpha = 0.86f)
    val hitBackground = cs.primary.copy(alpha = 0.28f)
    val text = remember(entry.id, query, processColor, messageColor, hitBackground) {
        buildAnnotatedString {
            val hit = SpanStyle(background = hitBackground, color = processColor, fontWeight = FontWeight.Bold)
            logsAppendHighlighted(entry.process, query, SpanStyle(color = processColor, fontWeight = FontWeight.SemiBold), hit)
            withStyle(SpanStyle(color = messageColor)) { append(": ") }
            logsAppendHighlighted(entry.message, query, SpanStyle(color = messageColor), hit)
        }
    }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1400)
            copied = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 6.dp.toPx()
            }
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(entry.time.substring(0, 8), style = mono, color = cs.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(levelColor.copy(alpha = 0.14f))
                    .padding(horizontal = 4.dp),
            ) {
                Text(entry.level.tag, style = mono, fontWeight = FontWeight.Bold, color = levelColor)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = mono,
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(tween(200)),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)) + expandVertically(tween(220)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(180)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${entry.time} · ${entry.process}[${entry.pid}] · ${entry.level.label}",
                    style = mono,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        clipboard.setText(
                            AnnotatedString("${entry.time} ${entry.process}[${entry.pid}] <${entry.level.tag}> ${entry.message}"),
                        )
                        copied = true
                    },
                ) {
                    AnimatedContent(
                        targetState = copied,
                        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.6f)) },
                        label = "copyIcon",
                    ) { done ->
                        Icon(
                            imageVector = if (done) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                            contentDescription = if (done) "Скопировано" else "Копировать строку",
                            tint = if (done) cs.primary else cs.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogsJumpPill(unseen: Int, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val brush = remember(cs.primary, cs.tertiary) { Brush.linearGradient(listOf(cs.primary, cs.tertiary)) }
    Row(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(brush)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "К последним ↓",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = cs.onPrimary,
        )
        AnimatedVisibility(visible = unseen > 0) {
            Row {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    AnimatedContent(
                        targetState = unseen,
                        transitionSpec = {
                            (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                        },
                        label = "unseen",
                    ) { n ->
                        Text(
                            text = "+$n",
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = cs.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogsEmptyState(text: String) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(cs.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Description, contentDescription = null, tint = cs.primary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(14.dp))
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "emptyText",
        ) { t ->
            Text(
                text = t,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
