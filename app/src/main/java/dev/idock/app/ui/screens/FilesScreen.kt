package dev.idock.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Model
// ---------------------------------------------------------------------------------------------

private enum class FilesKind { Folder, Image, Video, Audio, Document, Archive }

private enum class FilesSort(val label: String) { Name("Имя"), Date("Дата"), Size("Размер") }

private enum class FilesExport { Idle, Running, Done }

private data class FilesNode(
    val id: String,
    val name: String,
    val kind: FilesKind,
    val size: Long,
    val date: LocalDate,
    val children: Int = 0,
)

private class FilesRaw(val name: String, val kind: FilesKind, val sizeKb: Long, val date: LocalDate)

private const val FilesGb = 1024L * 1024L * 1024L
private const val FilesTotalBytes = 128L * FilesGb
private const val FilesUsedBytes = 87L * FilesGb + 430L * 1024L * 1024L

private val FilesMonths = listOf(
    "янв.", "февр.", "мар.", "апр.", "мая", "июн.",
    "июл.", "авг.", "сент.", "окт.", "нояб.", "дек.",
)

private fun filesChildKey(parent: String, name: String): String =
    if (parent.isEmpty()) name else "$parent/$name"

private fun filesKindOf(name: String): FilesKind =
    when (name.substringAfterLast('.', "").lowercase()) {
        "heic", "jpg", "jpeg", "png" -> FilesKind.Image
        "mov", "mp4" -> FilesKind.Video
        "m4a", "mp3", "wav" -> FilesKind.Audio
        "zip", "rar", "7z" -> FilesKind.Archive
        else -> FilesKind.Document
    }

private fun filesDemoTree(): Map<String, List<FilesNode>> {
    fun d(m: Int, day: Int, y: Int = 2026) = LocalDate.of(y, m, day)
    fun dir(name: String, date: LocalDate) = FilesRaw(name, FilesKind.Folder, 0L, date)
    fun file(name: String, kb: Long, date: LocalDate) = FilesRaw(name, filesKindOf(name), kb, date)

    val camera100 = (1..14).map { i ->
        val video = i == 4 || i == 9 || i == 13
        val num = i.toString().padStart(4, '0')
        if (video) {
            file("IMG_$num.MOV", 24_000L + i * 3_100L, d(10, 1 + i * 2))
        } else {
            file("IMG_$num.HEIC", 1_800L + (i * 373L) % 1_500L, d(10, 1 + i * 2))
        }
    }
    val camera101 = (15..20).map { i ->
        val num = i.toString().padStart(4, '0')
        if (i == 18) file("IMG_$num.MOV", 61_400L, d(11, i - 12))
        else file("IMG_$num.HEIC", 2_100L + (i * 211L) % 900L, d(11, i - 12))
    }

    val raw = linkedMapOf(
        "" to listOf(
            dir("DCIM", d(11, 8)),
            dir("Downloads", d(11, 2)),
            dir("Documents", d(10, 27)),
            dir("Books", d(9, 14)),
            dir("Recordings", d(10, 30)),
            file("notes_backup.zip", 4_820L, d(8, 19)),
        ),
        "DCIM" to listOf(
            dir("100APPLE", d(10, 29)),
            dir("101APPLE", d(11, 8)),
            dir("102APPLE", d(11, 10)),
        ),
        "DCIM/100APPLE" to camera100,
        "DCIM/101APPLE" to camera101,
        "DCIM/102APPLE" to emptyList<FilesRaw>(),
        "Downloads" to listOf(
            dir("Shared", d(10, 21)),
            file("report_q3.pdf", 1_240L, d(10, 3)),
            file("invoice_1042.pdf", 312L, d(10, 18)),
            file("wallpapers.zip", 58_300L, d(9, 29)),
            file("podcast_ep12.mp3", 41_700L, d(11, 2)),
            file("manual.pdf", 6_900L, d(7, 11)),
        ),
        "Downloads/Shared" to listOf(
            file("trip_photos.zip", 182_400L, d(10, 21)),
            file("plan.docx", 86L, d(10, 20)),
        ),
        "Documents" to listOf(
            dir("Работа", d(10, 27)),
            dir("Личное", d(9, 2)),
            file("Резюме.pdf", 420L, d(6, 15)),
            file("Смета.xlsx", 96L, d(10, 12)),
            file("Заметки.txt", 4L, d(11, 1)),
        ),
        "Documents/Работа" to listOf(
            dir("Архив", d(1, 9)),
            file("Отчёт.docx", 214L, d(10, 27)),
            file("Презентация.pdf", 8_340L, d(10, 22)),
            file("Таблица.xlsx", 132L, d(10, 15)),
        ),
        "Documents/Работа/Архив" to listOf(
            file("2024.zip", 96_200L, d(1, 9)),
            file("2025.zip", 128_900L, d(1, 9)),
        ),
        "Documents/Личное" to listOf(
            file("Рецепты.pdf", 2_310L, d(9, 2)),
            file("План поездки.docx", 64L, d(8, 24)),
        ),
        "Books" to listOf(
            file("Хроники моря.epub", 3_420L, d(9, 14)),
            file("Основы фотографии.pdf", 24_600L, d(5, 3)),
            file("Сказки.epub", 1_880L, d(12, 20, 2025)),
        ),
        "Recordings" to listOf(
            file("Запись 001.m4a", 3_200L, d(10, 2)),
            file("Запись 002.m4a", 11_900L, d(10, 9)),
            file("Лекция 14 окт.m4a", 52_400L, d(10, 14)),
            file("Идея.m4a", 820L, d(10, 30)),
        ),
    )

    fun folderSize(key: String): Long = raw[key].orEmpty().sumOf {
        if (it.kind == FilesKind.Folder) folderSize(filesChildKey(key, it.name)) else it.sizeKb * 1024L
    }

    return raw.mapValues { (parent, entries) ->
        entries.map { r ->
            val id = filesChildKey(parent, r.name)
            if (r.kind == FilesKind.Folder) {
                FilesNode(id, r.name, r.kind, folderSize(id), r.date, raw[id].orEmpty().size)
            } else {
                FilesNode(id, r.name, r.kind, r.sizeKb * 1024L, r.date)
            }
        }
    }
}

private fun filesSorted(items: List<FilesNode>, sort: FilesSort): List<FilesNode> {
    val inner: Comparator<FilesNode> = when (sort) {
        FilesSort.Name -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        FilesSort.Date -> compareByDescending { it.date }
        FilesSort.Size -> compareByDescending { it.size }
    }
    return items.sortedWith(compareBy<FilesNode> { it.kind != FilesKind.Folder }.then(inner))
}

private fun filesOneDecimal(v: Double): String {
    if (v >= 100) return Math.round(v).toString()
    val tenths = Math.round(v * 10)
    return "${tenths / 10},${tenths % 10}"
}

private fun filesFormatSize(bytes: Long): String {
    val kb = 1024.0
    return when {
        bytes < 1024L -> "$bytes Б"
        bytes < 1024L * 1024L -> "${filesOneDecimal(bytes / kb)} КБ"
        bytes < FilesGb -> "${filesOneDecimal(bytes / kb / kb)} МБ"
        else -> "${filesOneDecimal(bytes / kb / kb / kb)} ГБ"
    }
}

private fun filesFormatDate(date: LocalDate): String {
    val base = "${date.dayOfMonth} ${FilesMonths[date.monthValue - 1]}"
    return if (date.year == 2026) base else "$base ${date.year}"
}

private fun filesPlural(n: Int, one: String, few: String, many: String): String {
    val mod100 = n % 100
    val mod10 = n % 10
    val word = when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
    return "$n $word"
}

private fun filesMeta(node: FilesNode): String =
    if (node.kind == FilesKind.Folder) {
        if (node.children == 0) "Пусто · ${filesFormatDate(node.date)}"
        else "${filesPlural(node.children, "объект", "объекта", "объектов")} · ${filesFormatSize(node.size)}"
    } else {
        "${filesFormatSize(node.size)} · ${filesFormatDate(node.date)}"
    }

// ---------------------------------------------------------------------------------------------
// Motion helpers
// ---------------------------------------------------------------------------------------------

@Composable
private fun Modifier.filesPress(source: InteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "filesPress",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
private fun Modifier.filesEntrance(index: Int, enabled: Boolean): Modifier {
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
fun FilesScreen(contentPadding: PaddingValues = PaddingValues(0.dp), modifier: Modifier = Modifier) {
    val tree = remember { filesDemoTree().mapValues { (_, list) -> list.toMutableStateList() } }
    val path = remember { mutableStateListOf<String>() }
    val selected = remember { mutableStateListOf<String>() }
    var sort by rememberSaveable { mutableStateOf(FilesSort.Name) }
    var gridMode by rememberSaveable { mutableStateOf(false) }
    var exportState by remember { mutableStateOf(FilesExport.Idle) }
    val exportProgress = remember { Animatable(0f) }
    var exportedCount by remember { mutableIntStateOf(0) }
    var freedBytes by remember { mutableLongStateOf(0L) }
    var hint by remember { mutableStateOf<String?>(null) }
    var shownHint by remember { mutableStateOf("") }
    var staggerEnabled by remember { mutableStateOf(true) }
    val fabExpanded = remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val selectionMode by remember { derivedStateOf { selected.isNotEmpty() } }

    LaunchedEffect(Unit) {
        delay(800)
        staggerEnabled = false
    }
    LaunchedEffect(hint) {
        val current = hint
        if (current != null) {
            shownHint = current
            delay(2600)
            hint = null
        }
    }

    BackHandler(enabled = path.isNotEmpty() && !selectionMode) {
        path.removeAt(path.lastIndex)
    }
    BackHandler(enabled = selectionMode && exportState == FilesExport.Idle) {
        selected.clear()
    }

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
    val listPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(layoutDirection) + 20.dp,
        top = contentPadding.calculateTopPadding() + 8.dp,
        end = contentPadding.calculateEndPadding(layoutDirection) + 20.dp,
        bottom = bottomInset + if (selectionMode) 180.dp else 104.dp,
    )

    fun openFolder(name: String) {
        selected.clear()
        path.add(name)
    }

    fun jumpTo(depth: Int) {
        selected.clear()
        while (path.size > depth) path.removeAt(path.lastIndex)
    }

    fun toggle(id: String) {
        if (id in selected) selected.remove(id) else selected.add(id)
    }

    fun handleClick(node: FilesNode) {
        if (exportState != FilesExport.Idle) return
        if (selectionMode) {
            toggle(node.id)
        } else if (node.kind == FilesKind.Folder) {
            openFolder(node.name)
        } else {
            hint = "Экспортируйте файл на телефон, чтобы открыть его"
        }
    }

    fun handleLongClick(node: FilesNode) {
        if (exportState == FilesExport.Idle) toggle(node.id)
    }

    Box(modifier = modifier.fillMaxSize().nestedScroll(fabConnection)) {
        AnimatedContent(
            targetState = path.toList(),
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val dir = if (targetState.size >= initialState.size) 1 else -1
                (
                    slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { w -> dir * w / 3 } +
                        fadeIn(tween(240, delayMillis = 60))
                    ) togetherWith (
                    slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { w -> -dir * w / 4 } +
                        fadeOut(tween(180))
                    ) using SizeTransform(clip = false)
            },
            label = "filesDirectory",
        ) { dir ->
            val key = dir.joinToString("/")
            val source = tree[key]
            val items by remember(source, sort) {
                derivedStateOf { filesSorted(source.orEmpty(), sort) }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(if (gridMode) 3 else 1),
                modifier = Modifier.fillMaxSize(),
                contentPadding = listPadding,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "crumbs", span = { GridItemSpan(maxLineSpan) }) {
                    FilesBreadcrumbs(
                        path = dir,
                        onJump = { depth -> if (exportState == FilesExport.Idle) jumpTo(depth) },
                        modifier = Modifier.filesEntrance(0, staggerEnabled),
                    )
                }
                item(key = "storage", span = { GridItemSpan(maxLineSpan) }) {
                    FilesStorageCard(
                        freedBytes = freedBytes,
                        modifier = Modifier.padding(top = 6.dp).filesEntrance(1, staggerEnabled),
                    )
                }
                item(key = "toolbar", span = { GridItemSpan(maxLineSpan) }) {
                    FilesToolbar(
                        sort = sort,
                        onSort = { sort = it },
                        gridMode = gridMode,
                        onToggleGrid = { gridMode = !gridMode },
                        count = items.size,
                        modifier = Modifier.padding(top = 6.dp).filesEntrance(2, staggerEnabled),
                    )
                }
                if (items.isEmpty()) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        FilesEmptyState(modifier = Modifier.animateItem())
                    }
                } else {
                    itemsIndexed(items, key = { _, node -> node.id }) { index, node ->
                        val isSelected = node.id in selected
                        val itemModifier = Modifier
                            .animateItem()
                            .filesEntrance(index + 3, staggerEnabled)
                        if (gridMode) {
                            FilesTile(
                                node = node,
                                selectionMode = selectionMode,
                                selected = isSelected,
                                onClick = { handleClick(node) },
                                onLongClick = { handleLongClick(node) },
                                modifier = itemModifier,
                            )
                        } else {
                            FilesRow(
                                node = node,
                                selectionMode = selectionMode,
                                selected = isSelected,
                                onClick = { handleClick(node) },
                                onLongClick = { handleLongClick(node) },
                                modifier = itemModifier,
                            )
                        }
                    }
                }
            }
        }

        // Upload FAB
        AnimatedVisibility(
            visible = !selectionMode,
            enter = scaleIn(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = scaleOut(tween(200)) + fadeOut(tween(160)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = bottomInset + 16.dp),
        ) {
            ExtendedFloatingActionButton(
                text = { Text("Загрузить на iPhone", style = MaterialTheme.typography.labelLarge) },
                icon = { Icon(Icons.Rounded.Upload, contentDescription = null) },
                onClick = { hint = "Выбор файлов станет доступен после подключения устройства" },
                expanded = fabExpanded.value,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
            )
        }

        // Selection action bar
        AnimatedVisibility(
            visible = selectionMode,
            enter = slideInVertically(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) { it * 2 } +
                fadeIn(tween(200)),
            exit = slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it * 2 } + fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = bottomInset + 12.dp),
        ) {
            val currentKey = path.joinToString("/")
            FilesActionBar(
                count = selected.size,
                exportState = exportState,
                exportedCount = exportedCount,
                progress = { exportProgress.value },
                onClose = { selected.clear() },
                onSelectAll = {
                    val all = tree[currentKey].orEmpty().map { it.id }
                    if (selected.size == all.size) selected.clear()
                    else {
                        selected.clear()
                        selected.addAll(all)
                    }
                },
                onDelete = {
                    val ids = selected.toSet()
                    val list = tree[currentKey]
                    if (list != null) {
                        freedBytes += list.filter { it.id in ids }.sumOf { it.size }
                        list.removeAll { it.id in ids }
                    }
                    selected.clear()
                    hint = "Удалено: ${filesPlural(ids.size, "объект", "объекта", "объектов")}"
                },
                onExport = {
                    scope.launch {
                        exportedCount = selected.size
                        exportState = FilesExport.Running
                        exportProgress.snapTo(0f)
                        exportProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = (1200 + exportedCount * 120).coerceAtMost(2600),
                                easing = FastOutSlowInEasing,
                            ),
                        )
                        exportState = FilesExport.Done
                        delay(1700)
                        selected.clear()
                        delay(300)
                        exportState = FilesExport.Idle
                    }
                },
            )
        }

        // Transient hint
        AnimatedVisibility(
            visible = hint != null && !selectionMode,
            enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) { it } +
                fadeIn(),
            exit = slideOutVertically(tween(220)) { it / 2 } + fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = bottomInset + 88.dp),
        ) {
            FilesHint(text = shownHint)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Header pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun FilesBreadcrumbs(path: List<String>, onJump: (Int) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(path.size) {
        listState.animateScrollToItem(path.size)
    }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(count = path.size + 1, key = { i -> if (i == 0) "root" else "$i:${path[i - 1]}" }) { i ->
            Row(
                modifier = Modifier.animateItem(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (i > 0) {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(18.dp)
                            .graphicsLayer { rotationZ = -90f },
                    )
                }
                FilesCrumb(
                    label = if (i == 0) "Устройство" else path[i - 1],
                    isRoot = i == 0,
                    current = i == path.size,
                    onClick = { onJump(i) },
                )
            }
        }
    }
}

@Composable
private fun FilesCrumb(label: String, isRoot: Boolean, current: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        targetValue = if (current) cs.primary.copy(alpha = 0.14f) else cs.surfaceContainer,
        animationSpec = tween(300),
        label = "crumbBg",
    )
    val fg by animateColorAsState(
        targetValue = if (current) cs.primary else cs.onSurfaceVariant,
        animationSpec = tween(300),
        label = "crumbFg",
    )
    Row(
        modifier = Modifier
            .filesPress(source)
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .drawBehind { drawRect(bg) }
            .clickable(interactionSource = source, indication = LocalIndication.current, enabled = !current, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isRoot) {
            Icon(Icons.Rounded.PhoneIphone, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (current) FontWeight.SemiBold else FontWeight.Medium,
            color = fg,
            maxLines = 1,
        )
    }
}

@Composable
private fun FilesStorageCard(freedBytes: Long, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val used = (FilesUsedBytes - freedBytes).coerceAtLeast(0L)
    val free = FilesTotalBytes - used
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val fill by animateFloatAsState(
        targetValue = if (shown) used.toFloat() / FilesTotalBytes else 0f,
        animationSpec = tween(900, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "storageFill",
    )
    val brush = remember(cs.primary, cs.tertiary) { Brush.linearGradient(listOf(cs.primary, cs.tertiary)) }
    val track = cs.surfaceContainerHighest

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cs.surfaceContainer)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilesIconTile(icon = Icons.Rounded.Storage, accent = cs.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Хранилище iPhone", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                Text(
                    text = "Свободно ${filesFormatSize(free)} из ${filesFormatSize(FilesTotalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
            Text(
                text = filesFormatSize(used),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                modifier = Modifier.animateContentSize(),
            )
        }
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .drawBehind {
                    drawRect(track)
                    drawRoundRect(
                        brush = brush,
                        size = Size(size.width * fill, size.height),
                        cornerRadius = CornerRadius(size.height / 2f),
                    )
                },
        )
    }
}

@Composable
private fun FilesToolbar(
    sort: FilesSort,
    onSort: (FilesSort) -> Unit,
    gridMode: Boolean,
    onToggleGrid: () -> Unit,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilesSort.entries.forEach { option ->
                    FilesSortChip(label = option.label, selected = option == sort, onClick = { onSort(option) })
                }
            }
            IconButton(onClick = onToggleGrid) {
                AnimatedContent(
                    targetState = gridMode,
                    transitionSpec = {
                        (scaleIn(spring(dampingRatio = 0.6f)) + fadeIn()) togetherWith (scaleOut() + fadeOut())
                    },
                    label = "viewToggle",
                ) { grid ->
                    Icon(
                        imageVector = if (grid) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                        contentDescription = if (grid) "Список" else "Сетка",
                        tint = cs.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            text = filesPlural(count, "объект", "объекта", "объектов"),
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
    }
}

@Composable
private fun FilesSortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        targetValue = if (selected) cs.secondary.copy(alpha = 0.16f) else cs.surfaceContainer,
        animationSpec = tween(300),
        label = "sortBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) cs.secondary else cs.onSurfaceVariant,
        animationSpec = tween(300),
        label = "sortFg",
    )
    Row(
        modifier = Modifier
            .filesPress(source)
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

// ---------------------------------------------------------------------------------------------
// Items
// ---------------------------------------------------------------------------------------------

@Composable
private fun FilesIconTile(icon: ImageVector, accent: Color, modifier: Modifier = Modifier, tileSize: Int = 44) {
    Box(
        modifier = modifier
            .size(tileSize.dp)
            .clip(RoundedCornerShape((tileSize * 14 / 44).dp))
            .background(accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size((tileSize * 24 / 44).dp))
    }
}

@Composable
private fun filesAccent(kind: FilesKind): Color {
    val cs = MaterialTheme.colorScheme
    return when (kind) {
        FilesKind.Folder -> cs.primary
        FilesKind.Image -> cs.tertiary
        FilesKind.Video -> cs.secondary
        FilesKind.Audio -> cs.tertiary
        FilesKind.Document -> cs.primary
        FilesKind.Archive -> cs.secondary
    }
}

private fun filesIcon(kind: FilesKind): ImageVector = when (kind) {
    FilesKind.Folder -> Icons.Rounded.Folder
    FilesKind.Image -> Icons.Rounded.Image
    FilesKind.Video -> Icons.Rounded.Movie
    FilesKind.Audio -> Icons.Rounded.MusicNote
    FilesKind.Document -> Icons.Rounded.Description
    FilesKind.Archive -> Icons.Rounded.Archive
}

@Composable
private fun FilesCheck(selected: Boolean, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val fill by animateColorAsState(
        targetValue = if (selected) cs.primary else cs.primary.copy(alpha = 0f),
        animationSpec = tween(220),
        label = "checkFill",
    )
    val border by animateColorAsState(
        targetValue = if (selected) cs.primary else cs.outline,
        animationSpec = tween(220),
        label = "checkBorder",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "checkScale",
    )
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .drawBehind { drawRect(fill) }
            .border(2.dp, border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = cs.onPrimary,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer {
                    scaleX = checkScale
                    scaleY = checkScale
                },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilesRow(
    node: FilesNode,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        targetValue = if (selected) cs.primary.copy(alpha = 0.12f) else cs.surfaceContainer,
        animationSpec = tween(250),
        label = "rowBg",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .filesPress(source)
            .clip(RoundedCornerShape(20.dp))
            .drawBehind { drawRect(bg) }
            .combinedClickable(
                interactionSource = source,
                indication = LocalIndication.current,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .heightIn(min = 68.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = selectionMode,
            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilesCheck(selected = selected)
                Spacer(Modifier.width(12.dp))
            }
        }
        FilesIconTile(icon = filesIcon(node.kind), accent = filesAccent(node.kind))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = node.name,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = filesMeta(node),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (node.kind == FilesKind.Folder) {
            AnimatedVisibility(visible = !selectionMode, enter = fadeIn(), exit = fadeOut()) {
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(22.dp)
                        .graphicsLayer { rotationZ = -90f },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilesTile(
    node: FilesNode,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        targetValue = if (selected) cs.primary.copy(alpha = 0.12f) else cs.surfaceContainer,
        animationSpec = tween(250),
        label = "tileBg",
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .filesPress(source)
            .clip(RoundedCornerShape(20.dp))
            .drawBehind { drawRect(bg) }
            .combinedClickable(
                interactionSource = source,
                indication = LocalIndication.current,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(filesAccent(node.kind).copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            FilesIconTile(icon = filesIcon(node.kind), accent = filesAccent(node.kind), tileSize = 52)
            dev.idock.app.ui.kit.PlainAnimatedVisibility(
                visible = selectionMode,
                enter = scaleIn(spring(dampingRatio = 0.6f)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            ) {
                FilesCheck(selected = selected)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = node.name,
            style = MaterialTheme.typography.labelLarge,
            color = cs.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (node.kind == FilesKind.Folder) filesPlural(node.children, "объект", "объекта", "объектов")
            else filesFormatSize(node.size),
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun FilesEmptyState(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 24.dp)
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
                .background(cs.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = cs.primary, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Папка пуста", style = MaterialTheme.typography.titleLarge, color = cs.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Загрузите сюда файлы с телефона —\nони появятся на iPhone сразу после копирования",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Floating pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun FilesActionBar(
    count: Int,
    exportState: FilesExport,
    exportedCount: Int,
    progress: () -> Float,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val brush = remember(cs.primary, cs.tertiary) { Brush.linearGradient(listOf(cs.primary, cs.tertiary)) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cs.surfaceContainerHigh)
            .border(1.dp, cs.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
            .animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
            .padding(16.dp),
    ) {
        AnimatedContent(
            targetState = exportState,
            transitionSpec = {
                (fadeIn(tween(260, delayMillis = 80)) + scaleIn(initialScale = 0.94f, animationSpec = tween(260, delayMillis = 80))) togetherWith
                    fadeOut(tween(160)) using SizeTransform(clip = false)
            },
            label = "filesBarState",
        ) { state ->
            when (state) {
                FilesExport.Idle -> Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Rounded.Close, contentDescription = "Отменить выбор", tint = cs.onSurfaceVariant)
                        }
                        AnimatedContent(
                            targetState = count,
                            transitionSpec = {
                                val up = targetState > initialState
                                (slideInVertically { h -> if (up) h else -h } + fadeIn()) togetherWith
                                    (slideOutVertically { h -> if (up) -h else h } + fadeOut())
                            },
                            label = "filesCount",
                            modifier = Modifier.weight(1f),
                        ) { n ->
                            Text(
                                text = "Выбрано: $n",
                                style = MaterialTheme.typography.titleMedium,
                                color = cs.onSurface,
                            )
                        }
                        Text(
                            text = "Все",
                            style = MaterialTheme.typography.labelLarge,
                            color = cs.primary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onSelectAll)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilesBarButton(
                            text = "Удалить",
                            icon = Icons.Rounded.Delete,
                            container = Modifier.background(cs.error.copy(alpha = 0.12f)),
                            content = cs.error,
                            onClick = onDelete,
                        )
                        FilesBarButton(
                            text = "Экспорт на телефон",
                            icon = Icons.Rounded.Download,
                            container = Modifier.background(brush),
                            content = cs.onPrimary,
                            onClick = onExport,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                FilesExport.Running -> Column(Modifier.fillMaxWidth().padding(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Экспорт на телефон…",
                            style = MaterialTheme.typography.titleMedium,
                            color = cs.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        FilesProgressCounter(progress = progress, total = exportedCount)
                    }
                    Spacer(Modifier.height(14.dp))
                    val track = cs.surfaceContainerHighest
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
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
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Не отключайте кабель",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }

                FilesExport.Done -> Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val pop = remember { Animatable(0.4f) }
                    LaunchedEffect(Unit) {
                        pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                scaleX = pop.value
                                scaleY = pop.value
                            }
                            .clip(CircleShape)
                            .background(brush),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = cs.onPrimary)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Готово", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                        Text(
                            text = "${filesPlural(exportedCount, "файл", "файла", "файлов")} в «Загрузках» телефона",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilesProgressCounter(progress: () -> Float, total: Int) {
    val done = (progress() * total).toInt().coerceIn(0, total)
    Text(
        text = "$done из $total",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun FilesBarButton(
    text: String,
    icon: ImageVector,
    container: Modifier,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .filesPress(source)
            .height(52.dp)
            .clip(CircleShape)
            .then(container)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FilesHint(text: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(cs.inverseSurface)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = cs.inverseOnSurface, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = cs.inverseOnSurface)
    }
}
