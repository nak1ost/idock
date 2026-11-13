package dev.idock.app.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.idock.app.data.Prefs
import dev.idock.app.data.ThemeMode
import dev.idock.app.device.UsbDeviceMonitor
import dev.idock.app.ui.dashboard.DashboardScreen
import dev.idock.app.ui.kit.DockSheet
import dev.idock.app.ui.kit.GradientButton
import dev.idock.app.ui.kit.IconTile
import dev.idock.app.ui.kit.Pill
import dev.idock.app.ui.kit.pressScale
import dev.idock.app.ui.screens.FeatureScreen
import dev.idock.app.ui.settings.SettingsScreen
import dev.idock.app.ui.theme.DisplayFamily
import dev.idock.app.ui.theme.LocalDockColors
import dev.idock.app.ui.theme.LocalIsDark
import dev.idock.app.ui.theme.LocalThemeSwitcher
import kotlinx.coroutines.delay

enum class Tab(val label: String, val icon: ImageVector) {
    Device("Устройство", Icons.Rounded.PhoneIphone),
    Photos("Фото", Icons.Rounded.PhotoLibrary),
    Files("Файлы", Icons.Rounded.Folder),
    Apps("Приложения", Icons.Rounded.Apps),
    Tools("Инструменты", Icons.Rounded.Handyman),
}

/** A place in the app: one of the five tabs (depth 0) or a screen opened on top of them. */
private data class Dest(val key: String, val depth: Int)

private const val SETTINGS = "settings"
private val SubScreens = mapOf(
    "backup" to "Резервная копия",
    "ringtones" to "Рингтоны",
    "screenshot" to "Снимок экрана",
    "logs" to "Журнал устройства",
    "verify" to "Проверка устройства",
    SETTINGS to "Настройки",
)

private fun tabKey(tab: Tab) = "tab:${tab.name}"
private fun tabOf(key: String): Tab? = Tab.entries.firstOrNull { tabKey(it) == key }
private fun titleOf(key: String): String = tabOf(key)?.label ?: SubScreens[key].orEmpty()

/** Screens that run on sample data in this version, and say so in the top bar. */
private fun isDemoScreen(key: String) = key != tabKey(Tab.Device) && key != SETTINGS

@Composable
fun Shell(prefs: Prefs, monitor: UsbDeviceMonitor, onReplayOnboarding: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.Device) }
    val stack: SnapshotStateList<String> = rememberSaveable(
        saver = listSaver<SnapshotStateList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() },
        ),
    ) { mutableStateListOf() }
    val holder = rememberSaveableStateHolder()
    val link by monitor.state.collectAsState()

    var toast by remember { mutableStateOf<String?>(null) }
    var powerSheet by rememberSaveable { mutableStateOf(false) }
    var demoSheet by rememberSaveable { mutableStateOf(false) }

    fun pop() {
        if (stack.isNotEmpty()) holder.removeState(stack.removeAt(stack.lastIndex))
    }

    fun open(id: String) {
        when {
            id == "photos" -> {
                stack.clear()
                tab = Tab.Photos
            }
            id == "power" -> powerSheet = true
            id in SubScreens -> if (stack.lastOrNull() != id) stack.add(id)
            else -> toast = "Этот инструмент появится в одной из следующих версий"
        }
    }

    val dest = if (stack.isEmpty()) Dest(tabKey(tab), 0) else Dest(stack[stack.lastIndex], stack.size)

    BackHandler(enabled = stack.isEmpty() && tab != Tab.Device) { tab = Tab.Device }
    BackHandler(enabled = stack.isNotEmpty()) { pop() }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val keyboardOpen = imeBottom > navBottom + 48.dp
    val barVisible = dest.depth == 0 && !keyboardOpen

    val topPadding = statusTop + TopBarHeight + 4.dp
    val tabPadding = PaddingValues(
        top = topPadding,
        bottom = if (keyboardOpen) imeBottom + 12.dp else navBottom + BottomBarHeight + 28.dp,
    )
    val subPadding = PaddingValues(
        top = topPadding,
        bottom = (if (keyboardOpen) imeBottom else navBottom) + 20.dp,
    )

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = dest,
            contentKey = { it.key },
            transitionSpec = { destinationTransition() },
            modifier = Modifier.fillMaxSize(),
            label = "destination",
        ) { d ->
            holder.SaveableStateProvider(d.key) {
                val padding = if (d.depth == 0) tabPadding else subPadding
                when (d.key) {
                    tabKey(Tab.Device) -> DashboardScreen(
                        link = link,
                        demoEnabled = prefs.demoDevice,
                        contentPadding = padding,
                        onRequestPermission = monitor::requestPermission,
                        onDemoChange = prefs::updateDemoDevice,
                        onOpen = ::open,
                        onMessage = { toast = it },
                    )
                    SETTINGS -> SettingsScreen(
                        prefs = prefs,
                        contentPadding = padding,
                        onReplayOnboarding = {
                            stack.clear()
                            onReplayOnboarding()
                        },
                    )
                    else -> FeatureScreen(key = d.key, contentPadding = padding, onOpenTool = ::open)
                }
            }
        }

        TopBar(
            dest = dest,
            statusTop = statusTop,
            onBack = ::pop,
            onDemoInfo = { demoSheet = true },
            onSettings = { open(SETTINGS) },
        )

        BottomScrim(height = navBottom + BottomBarHeight + 44.dp, visible = barVisible)

        AnimatedVisibility(
            visible = barVisible,
            enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = 380f)) { it } + fadeIn(),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBottom + 10.dp),
        ) {
            BottomBar(
                selected = tab,
                onSelect = {
                    stack.clear()
                    tab = it
                },
            )
        }

        Toast(
            message = toast,
            bottom = if (barVisible) navBottom + BottomBarHeight + 26.dp else navBottom + 24.dp,
            onDone = { toast = null },
        )

        DockSheet(visible = demoSheet, onDismiss = { demoSheet = false }) {
            IconTile(Icons.Rounded.Science, accent = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.height(16.dp))
            Text("Здесь пока демо-данные", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Экран показывает, как раздел будет работать. Всё, что на нём есть, — примеры: " +
                    "с вашего iPhone ничего не читается и на него ничего не записывается. " +
                    "Настоящие данные появятся в следующих версиях.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            GradientButton("Понятно", onClick = { demoSheet = false }, modifier = Modifier.fillMaxWidth())
        }

        DockSheet(visible = powerSheet, onDismiss = { powerSheet = false }) {
            Text("Питание iPhone", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "Команды питания работают только с доверенным iPhone. В этой версии они ещё не подключены.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            SheetAction(Icons.Rounded.RestartAlt, "Перезагрузить") {
                powerSheet = false
                toast = "Перезагрузка появится в следующей версии"
            }
            Spacer(Modifier.height(10.dp))
            SheetAction(Icons.Rounded.PowerSettingsNew, "Выключить") {
                powerSheet = false
                toast = "Выключение появится в следующей версии"
            }
        }
    }
}

private val TopBarHeight = 60.dp
private val BottomBarHeight = 68.dp
private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private fun AnimatedContentTransitionScope<Dest>.destinationTransition(): ContentTransform {
    val from = initialState
    val to = targetState
    return when {
        // Between tabs: a short sideways step in the direction of the tab.
        from.depth == 0 && to.depth == 0 -> {
            val direction = if ((tabOf(to.key)?.ordinal ?: 0) >= (tabOf(from.key)?.ordinal ?: 0)) 1 else -1
            (fadeIn(tween(240, delayMillis = 70)) +
                slideInHorizontally(tween(360, easing = Emphasized)) { direction * it / 12 }) togetherWith
                (fadeOut(tween(110)) +
                    slideOutHorizontally(tween(360, easing = Emphasized)) { -direction * it / 12 })
        }
        // Opening a screen: it comes in from the right while the tab steps back.
        to.depth > from.depth ->
            (fadeIn(tween(260, delayMillis = 60)) +
                slideInHorizontally(tween(420, easing = Emphasized)) { it / 5 }) togetherWith
                (fadeOut(tween(160)) + scaleOut(tween(420, easing = Emphasized), targetScale = 0.94f))
        // Going back: the reverse, with the leaving screen on top.
        else ->
            ((fadeIn(tween(260, delayMillis = 60)) +
                scaleIn(tween(420, easing = Emphasized), initialScale = 0.94f)) togetherWith
                (fadeOut(tween(160)) + slideOutHorizontally(tween(420, easing = Emphasized)) { it / 5 }))
                .apply { targetContentZIndex = -1f }
    }
}

@Composable
private fun TopBar(
    dest: Dest,
    statusTop: Dp,
    onBack: () -> Unit,
    onDemoInfo: () -> Unit,
    onSettings: () -> Unit,
) {
    val ground = MaterialTheme.colorScheme.background
    Box(
        Modifier
            .fillMaxWidth()
            .height(statusTop + TopBarHeight + 18.dp)
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        0f to ground,
                        0.72f to ground.copy(alpha = 0.94f),
                        1f to ground.copy(alpha = 0f),
                    ),
                )
            },
    ) {
        Row(
            modifier = Modifier
                .padding(top = statusTop)
                .fillMaxWidth()
                .height(TopBarHeight)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = dest.depth > 0,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                Row {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable(onClickLabel = "Назад", role = Role.Button, onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Назад",
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                }
            }
            AnimatedContent(
                targetState = dest.key,
                transitionSpec = {
                    (fadeIn(tween(220, delayMillis = 60)) + slideInVertically(tween(300)) { it / 3 }) togetherWith
                        (fadeOut(tween(100)) + slideOutVertically(tween(200)) { -it / 3 })
                },
                modifier = Modifier.weight(1f),
                label = "title",
            ) { key ->
                if (key == tabKey(Tab.Device)) {
                    Wordmark()
                } else {
                    Text(
                        text = titleOf(key),
                        style = if (tabOf(key) != null) {
                            MaterialTheme.typography.headlineMedium
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (isDemoScreen(dest.key)) {
                Pill(
                    text = "Демо",
                    icon = Icons.Rounded.Science,
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClickLabel = "Что такое демо-данные", onClick = onDemoInfo),
                )
                Spacer(Modifier.width(if (dest.depth == 0) 2.dp else 12.dp))
            }
            if (dest.depth == 0) ThemeToggle()
            if (dest.key == tabKey(Tab.Device)) {
                IconButton(onClick = onSettings) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = "Настройки",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

/** The app's mark: two ports joined by a cable. */
@Composable
private fun Wordmark() {
    val copper = MaterialTheme.colorScheme.primary
    val path = remember { Path() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(26.dp)) {
            val s = size.minDimension
            path.rewind()
            path.moveTo(s * 0.2f, s * 0.3f)
            path.cubicTo(s * 0.2f, s * 0.98f, s * 0.8f, s * 0.98f, s * 0.8f, s * 0.3f)
            drawPath(path, copper, style = Stroke(width = s * 0.12f, cap = StrokeCap.Round))
            drawCircle(copper, radius = s * 0.14f, center = Offset(s * 0.2f, s * 0.24f))
            drawCircle(copper, radius = s * 0.14f, center = Offset(s * 0.8f, s * 0.24f))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "iDock",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Sun / moon button. The new theme spreads out from the button itself. */
@Composable
private fun ThemeToggle() {
    val dark = LocalIsDark.current
    val switcher = LocalThemeSwitcher.current
    var center by remember { mutableStateOf(Offset.Zero) }
    val turn by animateFloatAsState(
        targetValue = if (dark) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 180f),
        label = "themeTurn",
    )
    IconButton(
        onClick = { switcher.set(if (dark) ThemeMode.Light else ThemeMode.Dark, center) },
        modifier = Modifier.onGloballyPositioned { center = it.boundsInRoot().center },
    ) {
        Crossfade(targetState = dark, label = "themeIcon") { isDark ->
            Icon(
                imageVector = if (isDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                contentDescription = if (isDark) "Включить светлую тему" else "Включить чёрную тему",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer { rotationZ = turn },
            )
        }
    }
}

@Composable
private fun BoxScope.BottomScrim(height: Dp, visible: Boolean) {
    val ground = MaterialTheme.colorScheme.background
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(220), label = "scrim")
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(0f to ground.copy(alpha = 0f), 0.55f to ground.copy(alpha = 0.9f), 1f to ground),
                    alpha = alpha,
                )
            },
    )
}

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dock = LocalDockColors.current
    val haptics = LocalHapticFeedback.current
    val tabs = Tab.entries
    val position by animateFloatAsState(
        targetValue = selected.ordinal.toFloat(),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "tabIndicator",
    )
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .height(BottomBarHeight)
            .clip(shape)
            .background(dock.barContainer)
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.7f), shape)
            .drawBehind {
                val cell = size.width / tabs.size
                val pillW = cell - 10.dp.toPx()
                val pillH = size.height - 14.dp.toPx()
                drawRoundRect(
                    color = scheme.primary.copy(alpha = 0.14f),
                    topLeft = Offset(position * cell + (cell - pillW) / 2, (size.height - pillH) / 2),
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                )
            },
    ) {
        tabs.forEach { item ->
            val isSelected = item == selected
            val tint by animateColorAsState(
                targetValue = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                animationSpec = tween(220),
                label = "tabTint",
            )
            val pop = remember { Animatable(1f) }
            LaunchedEffect(isSelected) {
                if (isSelected) {
                    pop.snapTo(0.82f)
                    pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 520f))
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(
                        selected = isSelected,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = {
                            if (!isSelected) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(item)
                            }
                        },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                        },
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = DisplayFamily,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                    color = tint,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, text: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** A short message that slides up above the bottom bar and leaves by itself. */
@Composable
private fun BoxScope.Toast(message: String?, bottom: Dp, onDone: () -> Unit) {
    var last by remember { mutableStateOf("") }
    SideEffect { if (message != null) last = message }
    LaunchedEffect(message) {
        if (message != null) {
            delay(2600)
            onDone()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = 420f)) { it } + fadeIn(),
        exit = slideOutVertically(tween(200)) { it / 2 } + fadeOut(tween(200)),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(start = 24.dp, end = 24.dp, bottom = bottom),
    ) {
        Text(
            text = message ?: last,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}
