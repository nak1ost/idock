package dev.idock.app.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.idock.app.device.BatteryInfo
import dev.idock.app.device.DemoDevice
import dev.idock.app.device.DeviceInfo
import dev.idock.app.device.DeviceMode
import dev.idock.app.device.LinkState
import dev.idock.app.device.StorageInfo
import dev.idock.app.ui.kit.Barcode
import dev.idock.app.ui.kit.CableScene
import dev.idock.app.ui.kit.DockCard
import dev.idock.app.ui.kit.GradientButton
import dev.idock.app.ui.kit.IconTile
import dev.idock.app.ui.kit.Pill
import dev.idock.app.ui.kit.TonalButton
import dev.idock.app.ui.kit.pressScale
import dev.idock.app.ui.theme.LocalDockColors
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private enum class StageStatus { Disconnected, NeedsPermission, Connected, Demo }

@Composable
fun DashboardScreen(
    link: LinkState,
    demoEnabled: Boolean,
    contentPadding: PaddingValues,
    onRequestPermission: () -> Unit,
    onDemoChange: (Boolean) -> Unit,
    onOpen: (String) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val real = (link as? LinkState.Connected)?.info
    val info = real ?: if (demoEnabled && link is LinkState.Disconnected) DemoDevice.info else null
    val status = when {
        real != null -> StageStatus.Connected
        link is LinkState.NeedsPermission -> StageStatus.NeedsPermission
        info != null -> StageStatus.Demo
        else -> StageStatus.Disconnected
    }
    // Keeps the last device on screen while its cards animate away.
    val lastInfo = remember { arrayOfNulls<DeviceInfo>(1) }
    if (info != null) lastInfo[0] = info
    val shown = info ?: lastInfo[0]

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp),
    ) {
        ConnectionStage(
            status = status,
            deviceName = (link as? LinkState.NeedsPermission)?.name ?: info?.title,
            onRequestPermission = onRequestPermission,
            onDemoChange = onDemoChange,
        )

        // The label "prints" out when a device appears.
        AnimatedVisibility(
            visible = info != null,
            enter = expandVertically(tween(700, easing = LinearOutSlowInEasing), expandFrom = Alignment.Top) +
                fadeIn(tween(260)),
            exit = shrinkVertically(tween(320), shrinkTowards = Alignment.Top) + fadeOut(tween(200)),
        ) {
            if (shown != null) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    DeviceLabel(shown, onCopied = { onMessage("Скопировано: $it") })
                    if (shown.mode != DeviceMode.Normal) {
                        Spacer(Modifier.height(16.dp))
                        ModeNotice(shown.mode)
                    }
                    Spacer(Modifier.height(16.dp))
                    Instruments(shown)
                    Spacer(Modifier.height(16.dp))
                    QuickActions(onOpen)
                    if (shown.facts.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Facts(shown.facts)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = status == StageStatus.Disconnected,
            enter = expandVertically(tween(320)) + fadeIn(tween(320)),
            exit = shrinkVertically(tween(260)) + fadeOut(tween(160)),
        ) {
            Column {
                Spacer(Modifier.height(16.dp))
                HowToConnect()
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/* ---------- The cable ---------- */

@Composable
private fun ConnectionStage(
    status: StageStatus,
    deviceName: String?,
    onRequestPermission: () -> Unit,
    onDemoChange: (Boolean) -> Unit,
) {
    val connected = status == StageStatus.Connected || status == StageStatus.Demo
    val plug by animateFloatAsState(
        targetValue = if (connected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 170f),
        label = "plug",
    )
    val ring = remember { Animatable(1f) }
    var wasConnected by remember { mutableStateOf(connected) }
    LaunchedEffect(connected) {
        if (connected && !wasConnected) {
            delay(260)
            ring.snapTo(0f)
            ring.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
        }
        wasConnected = connected
    }
    val scheme = MaterialTheme.colorScheme

    DockCard {
        CableScene(
            plug = { plug },
            live = connected,
            ring = { ring.value },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        )
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = "Этот телефон",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.4f),
            )
            Spacer(Modifier.weight(0.2f))
            Text(
                text = if (connected) deviceName ?: "iPhone" else "iPhone",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.4f),
            )
        }
        Spacer(Modifier.height(18.dp))
        AnimatedContent(
            targetState = status,
            transitionSpec = {
                (fadeIn(tween(260, delayMillis = 90)) + slideInVertically(tween(320)) { it / 6 }) togetherWith
                    fadeOut(tween(120))
            },
            label = "stageStatus",
        ) { current ->
            Column(Modifier.fillMaxWidth()) {
                when (current) {
                    StageStatus.Disconnected -> {
                        Text("Подключите iPhone", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Соедините его с этим телефоном кабелем или переходником OTG. " +
                                "А пока можно посмотреть приложение на примере.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        GradientButton("Показать демо-устройство", onClick = { onDemoChange(true) })
                    }
                    StageStatus.NeedsPermission -> {
                        Text("Найден ${deviceName ?: "iPhone"}", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Разрешите приложению доступ к USB, чтобы прочитать данные устройства.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        GradientButton("Разрешить доступ", onClick = onRequestPermission, icon = Icons.Rounded.Usb)
                    }
                    StageStatus.Connected -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${deviceName ?: "iPhone"} подключён",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Pill("По USB", icon = Icons.Rounded.Usb)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Ниже всё, что устройство сообщает о себе по USB. Приложение только читает эти данные.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    StageStatus.Demo -> {
                        Text("Демо-устройство", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Так выглядит экран с подключённым iPhone. Все данные ниже придуманы.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        TonalButton("Выключить демо", onClick = { onDemoChange(false) })
                    }
                }
            }
        }
    }
}

/* ---------- The device label ---------- */

/** The device described the way the sticker on its box does: name, capacity, barcode, numbers. */
@Composable
fun DeviceLabel(info: DeviceInfo, modifier: Modifier = Modifier, onCopied: (String) -> Unit = {}) {
    val dock = LocalDockColors.current
    val ground = MaterialTheme.colorScheme.background
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(dock.labelPaper)
            .drawBehind {
                // Punched edges, like a label torn off a roll.
                val r = 5.dp.toPx()
                val step = 22.dp.toPx()
                var x = (size.width % step) / 2 + step / 2
                while (x < size.width) {
                    drawCircle(ground, r, Offset(x, 0f))
                    drawCircle(ground, r, Offset(x, size.height))
                    x += step
                }
            }
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = dock.labelInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(info.subtitle, style = MaterialTheme.typography.bodyMedium, color = dock.labelInkSoft)
            }
            if (info.capacity != null) {
                Spacer(Modifier.width(12.dp))
                Text(info.capacity, style = MaterialTheme.typography.headlineSmall, color = dock.labelInk)
            }
        }
        Spacer(Modifier.height(18.dp))
        Barcode(
            seed = info.code,
            color = dock.labelInk,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = info.code,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp, fontFeatureSettings = "tnum"),
                color = dock.labelInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (info.demo) {
                Spacer(Modifier.width(8.dp))
                Pill("Демо", container = dock.labelInk, content = dock.labelPaper)
            }
        }
        Spacer(Modifier.height(12.dp))
        info.rows.forEach { (key, value) ->
            LabelRow(key, value) {
                copyToClipboard(context, key, value)
                onCopied(key)
            }
        }
    }
}

@Composable
private fun LabelRow(key: String, value: String, onCopy: () -> Unit) {
    val dock = LocalDockColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = "Скопировать", role = Role.Button, onClick = onCopy)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.labelMedium,
            color = dock.labelInkSoft,
            modifier = Modifier.width(116.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
            color = dock.labelInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Rounded.ContentCopy,
            contentDescription = null,
            tint = dock.labelInkSoft,
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

@Composable
private fun ModeNotice(mode: DeviceMode) {
    val scheme = MaterialTheme.colorScheme
    DockCard(color = scheme.tertiaryContainer) {
        Text(
            text = if (mode == DeviceMode.Dfu) "Устройство в режиме DFU" else "Устройство в режиме восстановления",
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onTertiaryContainer,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "В этом режиме iPhone сообщает о себе только по USB. Приложение его не изменяет: " +
                "прошивка и восстановление в iDock не предусмотрены.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onTertiaryContainer,
        )
    }
}

/* ---------- Battery and storage ---------- */

@Composable
private fun Instruments(info: DeviceInfo) {
    val battery = info.battery
    val storage = info.storage
    if (battery == null || storage == null) {
        DockCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Rounded.Lock, accent = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Батарея и память", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Чтобы прочитать их, iPhone должен доверять этому телефону. " +
                            "Такое соединение появится в следующей версии.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        return
    }
    val progress = rememberPlayedOnce(info.code)
    BatteryCard(battery, progress)
    Spacer(Modifier.height(16.dp))
    StorageCard(storage, progress)
}

/** Runs 0→1 the first time a device is shown and stays at 1 afterwards (also when coming back to the tab). */
@Composable
private fun rememberPlayedOnce(key: String): State<Float> {
    var played by rememberSaveable(key) { mutableStateOf(false) }
    val progress = remember(key) { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(key) {
        if (!played) {
            delay(380)
            progress.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
            played = true
        }
    }
    return progress.asState()
}

@Composable
private fun BatteryCard(battery: BatteryInfo, progress: State<Float>) {
    val scheme = MaterialTheme.colorScheme
    DockCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Батарея", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Pill("Заряд ${battery.level}%")
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${(battery.health * progress.value).roundToInt()}",
                style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
            )
            Text(
                text = "%",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 2.dp, bottom = 7.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "ёмкости\nот новой батареи",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 9.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        CellGauge(
            fraction = { battery.health / 100f * progress.value },
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
        )
        Spacer(Modifier.height(14.dp))
        Row {
            Stat("Циклов заряда", battery.cycles.toString(), Modifier.weight(1f))
            Stat("Температура", "${battery.temperature} °C", Modifier.weight(1f))
        }
    }
}

/** A row of cells that light up left to right, like the level meter on a charger. */
@Composable
private fun CellGauge(fraction: () -> Float, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val from = scheme.primary
    val to = scheme.tertiary
    val off = scheme.surfaceContainerHighest
    Canvas(modifier) {
        val count = 28
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (count - 1)) / count
        val lit = fraction() * count
        val radius = CornerRadius(w / 2)
        for (i in 0 until count) {
            val x = i * (w + gap)
            drawRoundRect(off, Offset(x, 0f), Size(w, size.height), radius)
            val amount = (lit - i).coerceIn(0f, 1f)
            if (amount > 0f) {
                drawRoundRect(lerp(from, to, i / (count - 1f)), Offset(x, 0f), Size(w, size.height), radius, alpha = amount)
            }
        }
    }
}

@Composable
private fun StorageCard(storage: StorageInfo, progress: State<Float>) {
    val scheme = MaterialTheme.colorScheme
    val colors = listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.outline)
    val free = scheme.surfaceContainerHighest
    DockCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Память", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Pill(
                text = "Свободно ${formatGb(storage.totalGb - storage.usedGb)} ГБ",
                container = scheme.surfaceContainerHighest,
                content = scheme.onSurface,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatGb(storage.usedGb * progress.value),
                style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "ГБ занято\nиз ${formatGb(storage.totalGb)}",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 9.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(22.dp),
        ) {
            val gap = 3.dp.toPx()
            val radius = CornerRadius(7.dp.toPx())
            drawRoundRect(free, size = size, cornerRadius = radius)
            var x = 0f
            storage.parts.forEachIndexed { index, part ->
                val w = size.width * (part.gb / storage.totalGb) * progress.value
                if (w > gap) {
                    drawRoundRect(colors[index % colors.size], Offset(x, 0f), Size(w - gap, size.height), radius)
                }
                x += w
            }
        }
        Spacer(Modifier.height(14.dp))
        storage.parts.chunked(2).forEachIndexed { rowIndex, pair ->
            Row(Modifier.padding(vertical = 4.dp)) {
                pair.forEachIndexed { column, part ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(colors[(rowIndex * 2 + column) % colors.size]),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = part.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${formatGb(part.gb)} ГБ",
                            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"))
    }
}

private fun formatGb(value: Float): String = String.format(Locale.forLanguageTag("ru"), "%.1f", value)

/* ---------- Shortcuts, facts, help ---------- */

@Composable
private fun QuickActions(onOpen: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickTile(Icons.Rounded.PhotoLibrary, "Фото", scheme.primary) { onOpen("photos") }
        QuickTile(Icons.Rounded.Backup, "Копия", scheme.secondary) { onOpen("backup") }
        QuickTile(Icons.Rounded.VerifiedUser, "Проверка", scheme.tertiary) { onOpen("verify") }
        QuickTile(Icons.Rounded.Screenshot, "Экран", scheme.primary) { onOpen("screenshot") }
    }
}

@Composable
private fun RowScope.QuickTile(icon: ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .weight(1f)
            .pressScale(source)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconTile(icon, accent = accent, size = 40.dp)
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun Facts(facts: List<Pair<String, String>>) {
    val scheme = MaterialTheme.colorScheme
    DockCard {
        Text("Сведения", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        facts.forEach { (key, value) ->
            Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text(value, style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"))
            }
        }
    }
}

@Composable
private fun HowToConnect() {
    val scheme = MaterialTheme.colorScheme
    val steps = listOf(
        "Соедините телефоны" to "Кабелем или переходником OTG. iPhone подключается к этому телефону, как к компьютеру.",
        "Разблокируйте iPhone" to "Если он спросит, доверять ли этому устройству, нажмите «Доверять».",
        "Разрешите доступ к USB" to "Этот телефон покажет запрос, как только увидит iPhone.",
    )
    DockCard {
        Text("Как подключить", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        steps.forEachIndexed { index, (title, body) ->
            Row(Modifier.padding(vertical = 9.dp)) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(scheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", style = MaterialTheme.typography.titleSmall, color = scheme.primary)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Не определяется? Проверьте, что кабель передаёт данные, а в настройках этого телефона включён OTG.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}
