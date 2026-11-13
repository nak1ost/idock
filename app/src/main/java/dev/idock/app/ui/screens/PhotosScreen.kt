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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BurstMode
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Panorama
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Model
// ---------------------------------------------------------------------------------------------

private data class PhotosItem(
    val id: Int,
    val name: String,
    val video: Boolean,
    val duration: String,
    val time: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
)

private data class PhotosGroup(val title: String, val subtitle: String, val items: List<PhotosItem>)

private class PhotosAlbum(
    val title: String,
    val icon: ImageVector,
    val matches: (item: PhotosItem, favorite: Boolean) -> Boolean,
)

private data class PhotosOpen(val item: PhotosItem, val origin: Rect, val groupTitle: String)

private enum class PhotosImport { Idle, Running, Done }

private class PhotosBounds {
    var rect: Rect = Rect.Zero
}

private fun photosMakeItem(id: Int): PhotosItem {
    val video = id % 7 == 3
    val portrait = id % 3 == 1
    val hour = 8 + (id * 5) % 13
    val minute = (id * 17) % 60
    val seconds = 8 + (id * 13) % 95
    return PhotosItem(
        id = id,
        name = "IMG_${4100 + id}.${if (video) "MOV" else "HEIC"}",
        video = video,
        duration = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
        time = "$hour:${minute.toString().padStart(2, '0')}",
        sizeBytes = if (video) (18L + (id * 7L) % 60L) * 1_000_000L else 1_400_000L + (id * 233_000L) % 2_600_000L,
        width = when {
            video -> 1920
            portrait -> 3024
            else -> 4032
        },
        height = when {
            video -> 1080
            portrait -> 4032
            else -> 3024
        },
    )
}

private val PhotosDemoGroups: List<PhotosGroup> = run {
    val specs = listOf(
        Triple("Сегодня", "Пятница, 13 ноября", 7),
        Triple("Вчера", "Четверг, 12 ноября", 11),
        Triple("12 октября", "Понедельник", 9),
        Triple("3 октября", "Суббота", 14),
        Triple("28 сентября", "Понедельник", 7),
    )
    var next = 0
    specs.map { (title, subtitle, count) ->
        PhotosGroup(title, subtitle, List(count) { photosMakeItem(next++) })
    }
}

private val PhotosInitialFavorites: List<Int> =
    PhotosDemoGroups.flatMap { it.items }.filter { it.id % 9 == 2 || it.id == 0 }.map { it.id }

private val PhotosAlbums: List<PhotosAlbum> = listOf(
    PhotosAlbum("Недавние", Icons.Rounded.Schedule) { _, _ -> true },
    PhotosAlbum("Избранное", Icons.Rounded.Favorite) { _, fav -> fav },
    PhotosAlbum("Видео", Icons.Rounded.Videocam) { item, _ -> item.video },
    PhotosAlbum("Снимки экрана", Icons.Rounded.Screenshot) { item, _ -> !item.video && item.id % 5 == 1 },
    PhotosAlbum("Селфи", Icons.Rounded.Face) { item, _ -> !item.video && item.id % 6 == 4 },
    PhotosAlbum("Портреты", Icons.Rounded.Person) { item, _ -> !item.video && item.id % 4 == 2 },
    PhotosAlbum("Панорамы", Icons.Rounded.Panorama) { item, _ -> !item.video && item.id % 11 == 7 },
    PhotosAlbum("Серии", Icons.Rounded.BurstMode) { item, _ -> !item.video && item.id % 13 == 5 },
)

private fun photosFormatSize(bytes: Long): String {
    val mb = bytes / 1_000_000.0
    if (mb >= 1000) {
        val tenths = Math.round(mb / 100.0)
        return "${tenths / 10},${tenths % 10} ГБ"
    }
    if (mb >= 100) return "${Math.round(mb)} МБ"
    val tenths = Math.round(mb * 10)
    return "${tenths / 10},${tenths % 10} МБ"
}

private fun photosLerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

// ---------------------------------------------------------------------------------------------
// Visual helpers
// ---------------------------------------------------------------------------------------------

@Composable
private fun photosPalette(): List<Color> {
    val cs = MaterialTheme.colorScheme
    return remember(cs.primary, cs.secondary, cs.tertiary, cs.primaryContainer, cs.tertiaryContainer) {
        listOf(
            cs.primary,
            cs.tertiary,
            cs.secondary,
            cs.primaryContainer,
            cs.tertiaryContainer,
            cs.secondaryContainer,
            cs.inversePrimary,
        )
    }
}

/** Soft "photo-like" placeholder: an angled two-colour gradient, a light bloom and a gentle bottom shade. */
private fun Modifier.photosGradient(seed: Int, palette: List<Color>): Modifier = this.drawWithCache {
    val n = palette.size
    val first = (seed * 3) % n
    val second = (first + 1 + (seed * 5) % (n - 1)) % n
    val angle = ((seed * 53) % 360) * PI / 180.0
    val half = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val delta = Offset((cos(angle) * half).toFloat(), (sin(angle) * half).toFloat())
    val base = Brush.linearGradient(
        colors = listOf(palette[first], palette[second]),
        start = center - delta,
        end = center + delta,
    )
    val bloomCenter = Offset(
        size.width * (0.2f + ((seed * 37) % 60) / 100f),
        size.height * (0.12f + ((seed * 17) % 40) / 100f),
    )
    val bloom = Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0f)),
        center = bloomCenter,
        radius = size.minDimension * 0.6f,
    )
    val shade = Brush.verticalGradient(
        colors = listOf(Color.Black.copy(alpha = 0f), Color.Black.copy(alpha = 0.20f)),
        startY = size.height * 0.5f,
        endY = size.height,
    )
    onDrawBehind {
        drawRect(base)
        drawRect(bloom)
        drawRect(shade)
    }
}

@Composable
private fun Modifier.photosPress(source: InteractionSource, extraScale: () -> Float = { 1f }): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "photosPress",
    )
    return this.graphicsLayer {
        val s = scale * extraScale()
        scaleX = s
        scaleY = s
    }
}

@Composable
private fun Modifier.photosEntrance(index: Int, enabled: Boolean): Modifier {
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
fun PhotosScreen(contentPadding: PaddingValues = PaddingValues(0.dp), modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var albumFilter by rememberSaveable { mutableStateOf<Int?>(null) }
    val favorites = remember { mutableStateListOf<Int>().apply { addAll(PhotosInitialFavorites) } }
    val selected = remember { mutableStateListOf<Int>() }
    var importState by remember { mutableStateOf(PhotosImport.Idle) }
    val importProgress = remember { Animatable(0f) }
    var importTotal by remember { mutableIntStateOf(0) }
    var preview by remember { mutableStateOf<PhotosOpen?>(null) }
    val previewProgress = remember { Animatable(0f) }
    var staggerEnabled by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val palette = photosPalette()

    val selectionMode by remember { derivedStateOf { selected.isNotEmpty() } }

    val groups by remember {
        derivedStateOf {
            val album = albumFilter?.let { PhotosAlbums[it] }
            if (album == null) {
                PhotosDemoGroups
            } else {
                PhotosDemoGroups
                    .map { g -> g.copy(items = g.items.filter { album.matches(it, it.id in favorites) }) }
                    .filter { it.items.isNotEmpty() }
            }
        }
    }
    val selectedBytes by remember {
        derivedStateOf {
            val ids = selected.toSet()
            PhotosDemoGroups.sumOf { g -> g.items.filter { it.id in ids }.sumOf { it.sizeBytes } }
        }
    }

    LaunchedEffect(Unit) {
        delay(800)
        staggerEnabled = false
    }

    fun openPreview(item: PhotosItem, origin: Rect, groupTitle: String) {
        preview = PhotosOpen(item, origin, groupTitle)
        scope.launch {
            previewProgress.snapTo(0f)
            previewProgress.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    fun closePreview() {
        scope.launch {
            previewProgress.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
            preview = null
        }
    }

    fun toggle(id: Int) {
        if (id in selected) selected.remove(id) else selected.add(id)
    }

    fun startImport() {
        if (importState != PhotosImport.Idle || selected.isEmpty()) return
        scope.launch {
            importTotal = selected.size
            importState = PhotosImport.Running
            importProgress.snapTo(0f)
            importProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (importTotal * 70).coerceIn(1400, 3600),
                    easing = FastOutSlowInEasing,
                ),
            )
            importState = PhotosImport.Done
            delay(1800)
            selected.clear()
            delay(300)
            importState = PhotosImport.Idle
        }
    }

    BackHandler(enabled = albumFilter != null && !selectionMode && preview == null) {
        albumFilter = null
    }
    BackHandler(enabled = selectionMode && importState == PhotosImport.Idle && preview == null) {
        selected.clear()
    }
    BackHandler(enabled = preview != null) {
        closePreview()
    }

    val layoutDirection = LocalLayoutDirection.current
    val topInset = contentPadding.calculateTopPadding()
    val bottomInset = contentPadding.calculateBottomPadding()
    val gridPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(layoutDirection) + 20.dp,
        top = topInset + 76.dp,
        end = contentPadding.calculateEndPadding(layoutDirection) + 20.dp,
        bottom = bottomInset + if (selectionMode) 180.dp else 24.dp,
    )

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = tab,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (
                    slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { w -> dir * w / 4 } +
                        fadeIn(tween(260, delayMillis = 60))
                    ) togetherWith (
                    slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { w -> -dir * w / 4 } +
                        fadeOut(tween(180))
                    ) using SizeTransform(clip = false)
            },
            label = "photosTab",
        ) { current ->
            if (current == 0) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = gridPadding,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    val filterIndex = albumFilter
                    if (filterIndex != null) {
                        item(key = "filter", span = { GridItemSpan(maxLineSpan) }) {
                            PhotosFilterChip(
                                album = PhotosAlbums[filterIndex],
                                onClear = { albumFilter = null },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    if (groups.isEmpty()) {
                        item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                            PhotosEmptyState(modifier = Modifier.animateItem())
                        }
                    }
                    var running = 0
                    groups.forEachIndexed { groupIndex, group ->
                        val base = running
                        running += group.items.size + 1
                        item(key = "h:${group.title}", span = { GridItemSpan(maxLineSpan) }) {
                            val ids = group.items.map { it.id }
                            val allSelected = selectionMode && ids.all { it in selected }
                            PhotosHeader(
                                group = group,
                                first = groupIndex == 0,
                                selectionMode = selectionMode,
                                allSelected = allSelected,
                                onAction = {
                                    if (importState == PhotosImport.Idle) {
                                        if (allSelected) {
                                            selected.removeAll(ids)
                                        } else {
                                            ids.forEach { if (it !in selected) selected.add(it) }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .animateItem()
                                    .photosEntrance(base, staggerEnabled),
                            )
                        }
                        itemsIndexed(group.items, key = { _, it -> it.id }) { index, item ->
                            PhotosTile(
                                item = item,
                                palette = palette,
                                favorite = item.id in favorites,
                                selectionMode = selectionMode,
                                selected = item.id in selected,
                                onClick = { bounds ->
                                    when {
                                        importState != PhotosImport.Idle -> Unit
                                        selectionMode -> toggle(item.id)
                                        else -> openPreview(item, bounds, group.title)
                                    }
                                },
                                onLongClick = { if (importState == PhotosImport.Idle) toggle(item.id) },
                                modifier = Modifier
                                    .animateItem()
                                    .photosEntrance(base + 1 + index, staggerEnabled),
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = gridPadding,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(PhotosAlbums, key = { _, album -> album.title }) { index, album ->
                        val matching = remember(album) {
                            derivedStateOf {
                                PhotosDemoGroups.flatMap { it.items }.filter { album.matches(it, it.id in favorites) }
                            }
                        }
                        PhotosAlbumCard(
                            album = album,
                            items = matching.value,
                            palette = palette,
                            onClick = {
                                albumFilter = if (index == 0) null else index
                                tab = 0
                            },
                            modifier = Modifier.photosEntrance(index, true),
                        )
                    }
                }
            }
        }

        // Segmented control pinned under the top bar
        PhotosSegmented(
            selected = tab,
            onSelect = { index ->
                if (importState == PhotosImport.Idle && index != tab) {
                    selected.clear()
                    tab = index
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topInset + 8.dp, start = 20.dp, end = 20.dp),
        )

        // Multi-select bar
        AnimatedVisibility(
            visible = selectionMode,
            enter = slideInVertically(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) { it * 2 } +
                fadeIn(tween(200)),
            exit = slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it * 2 } + fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = bottomInset + 12.dp),
        ) {
            PhotosSelectionBar(
                count = selected.size,
                bytes = selectedBytes,
                state = importState,
                total = importTotal,
                progress = { importProgress.value },
                onClose = { selected.clear() },
                onImport = { startImport() },
            )
        }

        // Preview overlay
        val open = preview
        if (open != null) {
            PhotosPreview(
                open = open,
                progress = { previewProgress.value },
                palette = palette,
                favorite = open.item.id in favorites,
                onToggleFavorite = {
                    val id = open.item.id
                    if (id in favorites) favorites.remove(id) else favorites.add(id)
                },
                onClose = { closePreview() },
                topInset = topInset,
                bottomInset = bottomInset,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Segmented control
// ---------------------------------------------------------------------------------------------

@Composable
private fun PhotosSegmented(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val labels = listOf("Все фото", "Альбомы")
    val position by animateFloatAsState(
        targetValue = selected.toFloat(),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "segment",
    )
    val sign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val brush = remember(cs.primary, cs.tertiary) { Brush.linearGradient(listOf(cs.primary, cs.tertiary)) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(cs.surfaceContainerHigh)
            .border(1.dp, cs.outlineVariant.copy(alpha = 0.4f), CircleShape)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .fillMaxHeight()
                .graphicsLayer { translationX = sign * position * size.width }
                .clip(CircleShape)
                .background(brush),
        )
        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { index, label ->
                val color by animateColorAsState(
                    targetValue = if (index == selected) cs.onPrimary else cs.onSurfaceVariant,
                    animationSpec = tween(300),
                    label = "segmentText",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// "All photos" pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun PhotosHeader(
    group: PhotosGroup,
    first: Boolean,
    selectionMode: Boolean,
    allSelected: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val action = when {
        !selectionMode -> "Выбрать"
        allSelected -> "Снять выбор"
        else -> "Выбрать все"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = if (first) 4.dp else 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(group.title, style = MaterialTheme.typography.titleLarge, color = cs.onSurface)
            Text(
                text = "${group.subtitle} · ${group.items.size} фото",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(cs.primary.copy(alpha = 0.10f))
                .clickable(onClick = onAction)
                .animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium))
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            AnimatedContent(
                targetState = action,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "headerAction",
            ) { text ->
                Text(text, style = MaterialTheme.typography.labelLarge, color = cs.primary, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotosTile(
    item: PhotosItem,
    palette: List<Color>,
    favorite: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: (Rect) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val bounds = remember { PhotosBounds() }
    val selectScale by animateFloatAsState(
        targetValue = if (selected) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "tileSelect",
    )
    val tint by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(220),
        label = "tileTint",
    )
    val tintColor = cs.primary
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .onGloballyPositioned { bounds.rect = it.boundsInRoot() }
            .photosPress(source) { selectScale }
            .clip(RoundedCornerShape(12.dp))
            .photosGradient(item.id, palette)
            .drawBehind { drawRect(tintColor.copy(alpha = 0.22f * tint)) }
            .combinedClickable(
                interactionSource = source,
                indication = LocalIndication.current,
                onLongClick = onLongClick,
                onClick = { onClick(bounds.rect) },
            ),
    ) {
        if (item.video) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.34f))
                    .padding(start = 4.dp, end = 7.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Text(item.duration, style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
        }
        AnimatedVisibility(
            visible = favorite,
            enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(7.dp),
        ) {
            Icon(Icons.Rounded.Favorite, contentDescription = "Избранное", tint = Color.White, modifier = Modifier.size(16.dp))
        }
        AnimatedVisibility(
            visible = selectionMode,
            enter = scaleIn(spring(dampingRatio = 0.6f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
        ) {
            PhotosCheck(selected = selected)
        }
    }
}

@Composable
private fun PhotosCheck(selected: Boolean) {
    val cs = MaterialTheme.colorScheme
    val fill by animateColorAsState(
        targetValue = if (selected) cs.primary else Color.Black.copy(alpha = 0.18f),
        animationSpec = tween(200),
        label = "photoCheckFill",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "photoCheckScale",
    )
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .drawBehind { drawRect(fill) }
            .border(2.dp, Color.White, CircleShape),
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

@Composable
private fun PhotosFilterChip(album: PhotosAlbum, onClear: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(cs.secondary.copy(alpha = 0.14f))
                .clickable(onClick = onClear)
                .padding(start = 12.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(album.icon, contentDescription = null, tint = cs.secondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Альбом «${album.title}»", style = MaterialTheme.typography.labelLarge, color = cs.secondary)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.Close, contentDescription = "Сбросить", tint = cs.secondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PhotosEmptyState(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(cs.tertiary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = cs.tertiary, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("В альбоме пока пусто", style = MaterialTheme.typography.titleLarge, color = cs.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Отметьте снимки сердечком в просмотре —\nони появятся здесь",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Selection bar
// ---------------------------------------------------------------------------------------------

@Composable
private fun PhotosSelectionBar(
    count: Int,
    bytes: Long,
    state: PhotosImport,
    total: Int,
    progress: () -> Float,
    onClose: () -> Unit,
    onImport: () -> Unit,
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
            targetState = state,
            transitionSpec = {
                (fadeIn(tween(260, delayMillis = 80)) + scaleIn(initialScale = 0.94f, animationSpec = tween(260, delayMillis = 80))) togetherWith
                    fadeOut(tween(160)) using SizeTransform(clip = false)
            },
            label = "photosBar",
        ) { s ->
            when (s) {
                PhotosImport.Idle -> Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Rounded.Close, contentDescription = "Отменить выбор", tint = cs.onSurfaceVariant)
                        }
                        Column(Modifier.weight(1f)) {
                            AnimatedContent(
                                targetState = count,
                                transitionSpec = {
                                    val up = targetState > initialState
                                    (slideInVertically { h -> if (up) h else -h } + fadeIn()) togetherWith
                                        (slideOutVertically { h -> if (up) -h else h } + fadeOut())
                                },
                                label = "photosCount",
                            ) { n ->
                                Text("Выбрано: $n", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                            }
                            Text(photosFormatSize(bytes), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    PhotosGradientButton(
                        text = "Импортировать на телефон",
                        icon = Icons.Rounded.Download,
                        brush = brush,
                        onClick = onImport,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                PhotosImport.Running -> Column(Modifier.fillMaxWidth().padding(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Импорт на телефон…",
                            style = MaterialTheme.typography.titleMedium,
                            color = cs.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        PhotosCounter(progress = progress, total = total)
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
                        text = "Снимки сохраняются в галерею телефона",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }

                PhotosImport.Done -> Row(
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
                        Text("Импорт завершён", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                        Text(
                            text = "$total фото теперь в галерее телефона",
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
private fun PhotosCounter(progress: () -> Float, total: Int) {
    val done = (progress() * total).toInt().coerceIn(0, total)
    Text(
        text = "$done из $total",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun PhotosGradientButton(
    text: String,
    icon: ImageVector,
    brush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .photosPress(source)
            .height(52.dp)
            .clip(CircleShape)
            .background(brush)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = cs.onPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = cs.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Albums
// ---------------------------------------------------------------------------------------------

@Composable
private fun PhotosAlbumCard(
    album: PhotosAlbum,
    items: List<PhotosItem>,
    palette: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val seeds = remember(items) {
        val ids = items.take(4).map { it.id }
        List(4) { i -> ids.getOrElse(i) { 40 + i * 3 + album.title.length } }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .photosPress(source)
            .clip(RoundedCornerShape(28.dp))
            .background(cs.surfaceContainer)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick)
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp)),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (row in 0 until 2) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        for (col in 0 until 2) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .photosGradient(seeds[row * 2 + col], palette),
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(album.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.titleMedium,
            color = cs.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Text(
            text = "${items.size}",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 4.dp),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Preview overlay
// ---------------------------------------------------------------------------------------------

@Composable
private fun PhotosPreview(
    open: PhotosOpen,
    progress: () -> Float,
    palette: List<Color>,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    topInset: androidx.compose.ui.unit.Dp,
    bottomInset: androidx.compose.ui.unit.Dp,
) {
    val cs = MaterialTheme.colorScheme
    val item = open.item
    val finalBounds: MutableState<Rect?> = remember { mutableStateOf(null) }
    val scrim = cs.surface
    val brush = remember(cs.primary, cs.tertiary) { Brush.linearGradient(listOf(cs.primary, cs.tertiary)) }
    var saveState by remember(item.id) { mutableIntStateOf(0) }
    LaunchedEffect(saveState) {
        if (saveState == 1) {
            delay(900)
            saveState = 2
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(scrim.copy(alpha = 0.97f * progress().coerceIn(0f, 1f))) }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = topInset + 8.dp, bottom = bottomInset + 16.dp),
        ) {
            // Top actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = ((progress() - 0.4f) / 0.6f).coerceIn(0f, 1f) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Закрыть", tint = cs.onSurface)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleFavorite) {
                    AnimatedContent(
                        targetState = favorite,
                        transitionSpec = {
                            (scaleIn(spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)) + fadeIn()) togetherWith
                                (scaleOut(tween(120)) + fadeOut(tween(120)))
                        },
                        label = "favToggle",
                    ) { fav ->
                        Icon(
                            imageVector = if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (fav) "Убрать из избранного" else "В избранное",
                            tint = if (fav) cs.error else cs.onSurface,
                        )
                    }
                }
            }

            // Zooming image
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .aspectRatio(item.width.toFloat() / item.height)
                        .onGloballyPositioned { finalBounds.value = it.boundsInRoot() }
                        .graphicsLayer {
                            val target = finalBounds.value
                            val origin = open.origin
                            if (target == null || target.width <= 0f || origin.width <= 0f) {
                                alpha = 0f
                                return@graphicsLayer
                            }
                            val p = progress()
                            val startScaleX = origin.width / target.width
                            val startScaleY = origin.height / target.height
                            scaleX = photosLerp(startScaleX, 1f, p)
                            scaleY = photosLerp(startScaleY, 1f, p)
                            translationX = photosLerp(origin.center.x - target.center.x, 0f, p)
                            translationY = photosLerp(origin.center.y - target.center.y, 0f, p)
                            val startRadius = 12.dp.toPx() / startScaleX
                            shape = RoundedCornerShape(photosLerp(startRadius, 28.dp.toPx(), p.coerceIn(0f, 1f)))
                            clip = true
                        }
                        .photosGradient(item.id, palette),
                ) {
                    if (item.video) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.30f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Воспроизвести", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }

            // Metadata + actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val t = ((progress() - 0.35f) / 0.65f).coerceIn(0f, 1f)
                        alpha = t
                        translationY = (1f - t) * 24.dp.toPx()
                    }
                    .clip(RoundedCornerShape(28.dp))
                    .background(cs.surfaceContainer)
                    .padding(18.dp),
            ) {
                Text(item.name, style = MaterialTheme.typography.titleLarge, color = cs.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${open.groupTitle}, ${item.time}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PhotosMetaChip(photosFormatSize(item.sizeBytes))
                    PhotosMetaChip("${item.width} × ${item.height}")
                    PhotosMetaChip(if (item.video) "Видео · ${item.duration}" else "HEIC")
                }
                Spacer(Modifier.height(16.dp))
                val source = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .photosPress(source)
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(brush)
                        .clickable(
                            interactionSource = source,
                            indication = LocalIndication.current,
                            enabled = saveState == 0,
                        ) { saveState = 1 },
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = saveState,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically { it / 2 }) togetherWith
                                (fadeOut(tween(150)) + slideOutVertically { -it / 2 })
                        },
                        label = "previewSave",
                    ) { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (s == 2) Icons.Rounded.Check else Icons.Rounded.Download,
                                contentDescription = null,
                                tint = cs.onPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when (s) {
                                    0 -> "Импортировать на телефон"
                                    1 -> "Сохранение…"
                                    else -> "Сохранено в галерею"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = cs.onPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotosMetaChip(text: String) {
    val cs = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = cs.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(cs.surfaceContainerHigh)
            .heightIn(min = 28.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
