package dev.idock.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------------------------
// Model & demo data
// ---------------------------------------------------------------------------------------------

private data class AppsInfo(
    val id: String,
    val name: String,
    val version: String,
    val appBytes: Long,
    val dataBytes: Long,
    val system: Boolean,
    val docs: Int,
) {
    val total: Long get() = appBytes + dataBytes
}

private fun appsMb(mb: Double): Long = (mb * 1_000_000).toLong()

private val AppsDemo: List<AppsInfo> = listOf(
    AppsInfo("notes", "Заметки", "6.4.2", appsMb(38.4), appsMb(112.0), true, 214),
    AppsInfo("weather", "Погода", "3.1.0", appsMb(52.1), appsMb(18.7), true, 6),
    AppsInfo("calc", "Калькулятор", "2.0.4", appsMb(9.8), appsMb(0.4), true, 1),
    AppsInfo("clock", "Часы", "4.2.0", appsMb(14.6), appsMb(2.1), true, 3),
    AppsInfo("camera", "Камера", "8.0.1", appsMb(71.3), appsMb(6.9), true, 4),
    AppsInfo("steps", "Шагомер", "2.8.3", appsMb(64.2), appsMb(41.5), false, 38),
    AppsInfo("reader", "Читалка", "5.12.0", appsMb(118.6), appsMb(864.0), false, 57),
    AppsInfo("recorder", "Диктофон", "1.9.7", appsMb(22.4), appsMb(1_420.0), false, 112),
    AppsInfo("finance", "Финансы", "7.3.1", appsMb(186.0), appsMb(94.3), false, 21),
    AppsInfo("photoed", "Фоторедактор", "4.0.6", appsMb(342.7), appsMb(512.8), false, 76),
    AppsInfo("calm", "Медитация", "3.5.2", appsMb(96.1), appsMb(388.0), false, 44),
    AppsInfo("dict", "Словарь", "11.0.3", appsMb(74.9), appsMb(1_210.0), false, 9),
    AppsInfo("recipes", "Рецепты", "2.2.0", appsMb(48.3), appsMb(73.6), false, 128),
    AppsInfo("sleep", "Трекер сна", "1.4.9", appsMb(57.0), appsMb(22.2), false, 31),
    AppsInfo("planner", "Планер", "6.1.4", appsMb(81.5), appsMb(36.9), false, 402),
    AppsInfo("metro", "Схема метро", "3.9.0", appsMb(133.2), appsMb(48.1), false, 5),
    AppsInfo("chess", "Шахматы", "2.7.5", appsMb(61.8), appsMb(14.3), false, 17),
    AppsInfo("piano", "Музыкальная школа", "1.3.2", appsMb(412.6), appsMb(96.4), false, 22),
)

private val AppsFilters = listOf("Все", "Пользовательские", "Системные")

private fun appsFormatSize(bytes: Long): String {
    val mb = bytes / 1_000_000.0
    if (mb >= 1000) {
        val tenths = Math.round(mb / 100.0)
        return "${tenths / 10},${tenths % 10} ГБ"
    }
    if (mb >= 100) return "${Math.round(mb)} МБ"
    val tenths = Math.round(mb * 10)
    return "${tenths / 10},${tenths % 10} МБ"
}

private fun appsPlural(n: Int, one: String, few: String, many: String): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
}

// ---------------------------------------------------------------------------------------------
// Motion helpers
// ---------------------------------------------------------------------------------------------

@Composable
private fun Modifier.appsPress(source: InteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "appsPress",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
private fun Modifier.appsEntrance(index: Int, enabled: Boolean): Modifier {
    if (!enabled || index >= 10) return this
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
fun AppsScreen(contentPadding: PaddingValues = PaddingValues(0.dp), modifier: Modifier = Modifier) {
    val apps = remember { AppsDemo.toMutableStateList() }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var sortBySize by rememberSaveable { mutableStateOf(false) }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<AppsInfo?>(null) }
    var showInstall by remember { mutableStateOf(false) }
    var staggerEnabled by remember { mutableStateOf(true) }
    val fabExpanded = remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        delay(800)
        staggerEnabled = false
    }

    val visible by remember {
        derivedStateOf {
            val q = query.trim()
            apps
                .filter { app ->
                    when (filter) {
                        1 -> !app.system
                        2 -> app.system
                        else -> true
                    }
                }
                .filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }
                .let { list ->
                    if (sortBySize) list.sortedByDescending { it.total }
                    else list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                }
        }
    }
    val totalBytes by remember { derivedStateOf { apps.sumOf { it.total } } }
    val systemBytes by remember { derivedStateOf { apps.filter { it.system }.sumOf { it.total } } }

    val fabConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -6f) fabExpanded.value = false
                else if (available.y > 6f) fabExpanded.value = true
                return Offset.Zero
            }
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    val bottomInset = contentPadding.calculateBottomPadding()

    Box(modifier = modifier.fillMaxSize().nestedScroll(fabConnection)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection) + 20.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                end = contentPadding.calculateEndPadding(layoutDirection) + 20.dp,
                bottom = bottomInset + 104.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "summary") {
                AppsSummaryCard(
                    count = apps.size,
                    totalBytes = totalBytes,
                    systemBytes = systemBytes,
                    modifier = Modifier.appsEntrance(0, staggerEnabled),
                )
            }
            item(key = "search") {
                AppsSearchField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .appsEntrance(1, staggerEnabled),
                )
            }
            item(key = "filters") {
                AppsFilterBar(
                    filter = filter,
                    onFilter = { filter = it },
                    sortBySize = sortBySize,
                    onSort = { sortBySize = it },
                    counts = listOf(apps.size, apps.count { !it.system }, apps.count { it.system }),
                    modifier = Modifier.appsEntrance(2, staggerEnabled),
                )
            }
            if (visible.isEmpty()) {
                item(key = "empty") {
                    AppsEmptyState(
                        onReset = {
                            query = ""
                            filter = 0
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            itemsIndexed(visible, key = { _, app -> app.id }) { index, app ->
                AppsRow(
                    app = app,
                    colorIndex = AppsDemo.indexOfFirst { it.id == app.id },
                    expanded = expandedId == app.id,
                    onToggle = { expandedId = if (expandedId == app.id) null else app.id },
                    onDelete = { pendingDelete = app },
                    modifier = Modifier
                        .animateItem()
                        .appsEntrance(index + 3, staggerEnabled),
                )
            }
        }

        ExtendedFloatingActionButton(
            text = { Text("Установить IPA", style = MaterialTheme.typography.labelLarge) },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            onClick = { showInstall = true },
            expanded = fabExpanded.value,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = bottomInset + 16.dp),
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить «${toDelete.name}»?") },
            text = {
                Text(
                    "Приложение и его данные (${appsFormatSize(toDelete.total)}) будут удалены с iPhone. " +
                        "Это действие нельзя отменить.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (expandedId == toDelete.id) expandedId = null
                    apps.removeAll { it.id == toDelete.id }
                    pendingDelete = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Отмена") }
            },
        )
    }

    if (showInstall) {
        AlertDialog(
            onDismissRequest = { showInstall = false },
            icon = { Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Установка IPA") },
            text = {
                Text(
                    "Выберите файл .ipa в памяти телефона — iDock установит его на iPhone. " +
                        "Выбор файла станет доступен, когда устройство будет подключено.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showInstall = false }) { Text("Понятно") }
            },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Header blocks
// ---------------------------------------------------------------------------------------------

@Composable
private fun AppsSummaryCard(count: Int, totalBytes: Long, systemBytes: Long, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val brush = remember(cs.primary, cs.tertiary) { Brush.linearGradient(listOf(cs.primary, cs.tertiary)) }
    val shownCount = remember { Animatable(0f) }
    val shownBytes = remember { Animatable(0f) }
    LaunchedEffect(count) {
        shownCount.animateTo(count.toFloat(), tween(900, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(totalBytes) {
        shownBytes.animateTo(totalBytes.toFloat(), tween(1100, easing = FastOutSlowInEasing))
    }
    val userFraction by animateFloatAsState(
        targetValue = if (totalBytes == 0L) 0f else (totalBytes - systemBytes).toFloat() / totalBytes,
        animationSpec = tween(900, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "userFraction",
    )
    val onAccent = cs.onPrimary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(brush)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Apps, contentDescription = null, tint = onAccent)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Установлено на iPhone",
                style = MaterialTheme.typography.titleMedium,
                color = onAccent.copy(alpha = 0.9f),
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            AppsCountText(value = { shownCount.value }, color = onAccent)
            Spacer(Modifier.width(8.dp))
            Text(
                text = appsPlural(count, "приложение", "приложения", "приложений"),
                style = MaterialTheme.typography.titleMedium,
                color = onAccent.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            AppsBytesText(value = { shownBytes.value }, color = onAccent)
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .drawBehind {
                    drawRect(Color.White.copy(alpha = 0.22f))
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.85f),
                        size = Size(size.width * userFraction, size.height),
                        cornerRadius = CornerRadius(size.height / 2f),
                    )
                },
        )
        Spacer(Modifier.height(10.dp))
        Row {
            AppsLegend("Пользовательские", onAccent, 0.85f)
            Spacer(Modifier.width(16.dp))
            AppsLegend("Системные", onAccent, 0.3f)
        }
    }
}

@Composable
private fun AppsCountText(value: () -> Float, color: Color) {
    Text(
        text = Math.round(value()).toString(),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}

@Composable
private fun AppsBytesText(value: () -> Float, color: Color) {
    Text(
        text = appsFormatSize(value().toLong()),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun AppsLegend(label: String, color: Color, alpha: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = alpha)),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.85f))
    }
}

@Composable
private fun AppsSearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
        cursorBrush = SolidColor(cs.primary),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(CircleShape)
                    .background(cs.surfaceContainer)
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = cs.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text("Поиск приложений", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                    }
                    innerTextField()
                }
                AnimatedVisibility(
                    visible = value.isNotEmpty(),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onValueChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Очистить", tint = cs.onSurfaceVariant)
                    }
                }
            }
        },
    )
}

@Composable
private fun AppsFilterBar(
    filter: Int,
    onFilter: (Int) -> Unit,
    sortBySize: Boolean,
    onSort: (Boolean) -> Unit,
    counts: List<Int>,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppsFilters.forEachIndexed { index, label ->
                AppsChip(label = "$label ${counts[index]}", selected = filter == index, onClick = { onFilter(index) })
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Сортировка",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            AppsChip(label = "Имя", selected = !sortBySize, onClick = { onSort(false) })
            Spacer(Modifier.width(8.dp))
            AppsChip(label = "Размер", selected = sortBySize, onClick = { onSort(true) })
        }
    }
}

@Composable
private fun AppsChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        targetValue = if (selected) cs.secondary.copy(alpha = 0.16f) else cs.surfaceContainer,
        animationSpec = tween(300),
        label = "appsChipBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) cs.secondary else cs.onSurfaceVariant,
        animationSpec = tween(300),
        label = "appsChipFg",
    )
    Row(
        modifier = Modifier
            .appsPress(source)
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .drawBehind { drawRect(bg) }
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(visible = selected) {
            Row {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
        }
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
private fun AppsEmptyState(onReset: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 36.dp, bottom = 24.dp)
            .graphicsLayer {
                alpha = appear.value.coerceIn(0f, 1f)
                val s = 0.9f + 0.1f * appear.value
                scaleX = s
                scaleY = s
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(cs.secondary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = cs.secondary, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Ничего не найдено", style = MaterialTheme.typography.titleLarge, color = cs.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Попробуйте изменить запрос\nили выбрать другой фильтр",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        AppsChip(label = "Сбросить", selected = false, onClick = onReset)
    }
}

// ---------------------------------------------------------------------------------------------
// Row
// ---------------------------------------------------------------------------------------------

@Composable
private fun AppsIcon(name: String, colorIndex: Int) {
    val cs = MaterialTheme.colorScheme
    val pairs = listOf(
        Triple(cs.primary, cs.tertiary, cs.onPrimary),
        Triple(cs.secondary, cs.primary, cs.onSecondary),
        Triple(cs.tertiary, cs.secondary, cs.onTertiary),
        Triple(cs.primaryContainer, cs.tertiaryContainer, cs.onPrimaryContainer),
        Triple(cs.secondaryContainer, cs.primaryContainer, cs.onSecondaryContainer),
        Triple(cs.tertiaryContainer, cs.secondaryContainer, cs.onTertiaryContainer),
    )
    val (from, to, on) = pairs[(colorIndex.coerceAtLeast(0)) % pairs.size]
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(from, to))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = on,
        )
    }
}

@Composable
private fun AppsRow(
    app: AppsInfo,
    colorIndex: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val arrow by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "appsArrow",
    )
    val bg by animateColorAsState(
        targetValue = if (expanded) cs.surfaceContainerHigh else cs.surfaceContainer,
        animationSpec = tween(300),
        label = "appsRowBg",
    )
    var exportPhase by remember { mutableIntStateOf(0) }
    val exportProgress = remember { Animatable(0f) }
    var showDocs by remember { mutableStateOf(false) }

    LaunchedEffect(exportPhase) {
        when (exportPhase) {
            1 -> {
                exportProgress.snapTo(0f)
                exportProgress.animateTo(1f, tween(1800, easing = FastOutSlowInEasing))
                exportPhase = 2
            }
            2 -> {
                delay(2000)
                exportPhase = 0
            }
            else -> Unit
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .appsPress(source)
            .clip(RoundedCornerShape(20.dp))
            .drawBehind { drawRect(bg) }
            .animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onToggle)
                .heightIn(min = 72.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppsIcon(name = app.name, colorIndex = colorIndex)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (app.system) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = "Системное",
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${app.version} · ${appsFormatSize(app.total)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Свернуть" else "Подробнее",
                tint = cs.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { rotationZ = arrow },
            )
        }

        if (expanded) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                AppsSizeBar("Приложение", app.appBytes, app.appBytes.toFloat() / app.total, cs.primary)
                Spacer(Modifier.height(10.dp))
                AppsSizeBar("Документы и данные", app.dataBytes, app.dataBytes.toFloat() / app.total, cs.tertiary)
                Spacer(Modifier.height(14.dp))

                AnimatedContent(
                    targetState = exportPhase,
                    transitionSpec = { fadeIn(tween(250, delayMillis = 60)) togetherWith fadeOut(tween(150)) },
                    label = "appsExport",
                ) { phase ->
                    when (phase) {
                        0 -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppsActionTile(
                                icon = Icons.Rounded.Download,
                                label = "Экспорт IPA",
                                accent = cs.primary,
                                enabled = true,
                                onClick = { exportPhase = 1 },
                                modifier = Modifier.weight(1f),
                            )
                            AppsActionTile(
                                icon = Icons.Rounded.Folder,
                                label = "Документы",
                                accent = cs.secondary,
                                enabled = true,
                                onClick = { showDocs = !showDocs },
                                modifier = Modifier.weight(1f),
                            )
                            AppsActionTile(
                                icon = Icons.Rounded.Delete,
                                label = "Удалить",
                                accent = cs.error,
                                enabled = !app.system,
                                onClick = onDelete,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        1 -> AppsExportProgress(progress = { exportProgress.value })
                        else -> Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(cs.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = cs.primary)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "${app.name}.ipa сохранён в «Загрузки»",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurface,
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showDocs,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    AppsDocsList(app)
                }
                if (app.system) {
                    Text(
                        text = "Системные приложения нельзя удалить",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp, start = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppsSizeBar(label: String, bytes: Long, fraction: Float, accent: Color) {
    val cs = MaterialTheme.colorScheme
    val fill = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        fill.animateTo(fraction.coerceIn(0f, 1f), tween(700, delayMillis = 120, easing = FastOutSlowInEasing))
    }
    val track = cs.surfaceContainerHighest
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(appsFormatSize(bytes), style = MaterialTheme.typography.labelLarge, color = cs.onSurface)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .drawBehind {
                    drawRect(track)
                    drawRoundRect(
                        color = accent,
                        size = Size(size.width * fill.value, size.height),
                        cornerRadius = CornerRadius(size.height / 2f),
                    )
                },
        )
    }
}

@Composable
private fun AppsActionTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .appsPress(source)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f }
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.10f))
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick,
            )
            .heightIn(min = 64.dp)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppsExportProgress(progress: () -> Float) {
    val cs = MaterialTheme.colorScheme
    val brush = remember(cs.primary, cs.tertiary) { Brush.linearGradient(listOf(cs.primary, cs.tertiary)) }
    val track = cs.surfaceContainerHighest
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, cs.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row {
            Text("Экспорт IPA…", style = MaterialTheme.typography.bodyMedium, color = cs.onSurface, modifier = Modifier.weight(1f))
            AppsPercent(progress)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .drawBehind {
                    drawRect(track)
                    drawRoundRect(
                        brush = brush,
                        size = Size(size.width * progress(), size.height),
                        cornerRadius = CornerRadius(size.height / 2f),
                    )
                },
        )
    }
}

@Composable
private fun AppsPercent(progress: () -> Float) {
    Text(
        text = "${(progress() * 100).toInt()}%",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun AppsDocsList(app: AppsInfo) {
    val cs = MaterialTheme.colorScheme
    val entries = listOf(
        "Documents" to "${app.docs} ${appsPlural(app.docs, "файл", "файла", "файлов")}",
        "Library" to appsFormatSize(app.dataBytes * 3 / 10),
        "tmp" to appsFormatSize(app.dataBytes / 20),
    )
    Column(
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cs.surfaceContainerLow)
            .padding(vertical = 6.dp),
    ) {
        entries.forEach { (name, meta) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Folder, contentDescription = null, tint = cs.secondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(name, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface, modifier = Modifier.weight(1f))
                Text(meta, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        }
    }
}
