package dev.idock.app.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.idock.app.BuildConfig
import dev.idock.app.data.Prefs
import dev.idock.app.data.ThemeMode
import dev.idock.app.ui.kit.DockCard
import dev.idock.app.ui.kit.IconTile
import dev.idock.app.ui.kit.pressScale
import dev.idock.app.ui.theme.DarkScheme
import dev.idock.app.ui.theme.LightScheme
import dev.idock.app.ui.theme.LocalThemeSwitcher

@Composable
fun SettingsScreen(
    prefs: Prefs,
    contentPadding: PaddingValues,
    onReplayOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DockCard {
            Text("Тема", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(14.dp))
            ThemeModePicker(selected = prefs.themeMode)
        }

        DockCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Rounded.Science, accent = scheme.tertiary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Демо-устройство", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Показывать пример iPhone, пока настоящий не подключён",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = prefs.demoDevice, onCheckedChange = prefs::updateDemoDevice)
            }
        }

        DockCard(onClick = onReplayOnboarding) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Rounded.Replay)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Показать приветствие ещё раз", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Короткий гид по приложению и подключению",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }

        DockCard {
            Text("Что умеет эта версия", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            StatusRow(Icons.Rounded.Check, scheme.secondary, "Находит iPhone, iPad и iPod touch по USB")
            StatusRow(Icons.Rounded.Check, scheme.secondary, "Показывает UDID и режим устройства, копирует их по нажатию")
            StatusRow(Icons.Rounded.Schedule, scheme.tertiary, "Модель, версия iOS, батарея и память — в разработке")
            StatusRow(Icons.Rounded.Schedule, scheme.tertiary, "Файлы, фото, приложения, копии и инструменты пока работают на демо-данных")
            Spacer(Modifier.height(8.dp))
            Text(
                "Прошивки, джейлбрейка и обхода блокировок в iDock нет и не будет.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }

        DockCard {
            Text("О приложении", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("iDock ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Шрифт Sofia Sans — лицензия SIL Open Font License 1.1. " +
                    "Значки Material Icons — лицензия Apache 2.0. " +
                    "iPhone, iPad и iOS — товарные знаки Apple Inc.; приложение с Apple не связано.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusRow(icon: ImageVector, tint: Color, text: String) {
    Row(Modifier.padding(vertical = 6.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Three small previews: follow the system, light, black. Tapping one changes the theme at once,
 * and the new theme spreads out from the preview that was tapped.
 */
@Composable
fun ThemeModePicker(selected: ThemeMode, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ThemeOption(ThemeMode.System, "Как в системе", selected == ThemeMode.System, Modifier.weight(1f))
        ThemeOption(ThemeMode.Light, "Светлая", selected == ThemeMode.Light, Modifier.weight(1f))
        ThemeOption(ThemeMode.Dark, "Чёрная", selected == ThemeMode.Dark, Modifier.weight(1f))
    }
}

@Composable
private fun ThemeOption(mode: ThemeMode, label: String, selected: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val switcher = LocalThemeSwitcher.current
    val source = remember { MutableInteractionSource() }
    var center by remember { mutableStateOf(Offset.Zero) }
    val ring by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.outlineVariant,
        animationSpec = tween(220),
        label = "themeRing",
    )
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .onGloballyPositioned { center = it.boundsInRoot().center }
            .pressScale(source)
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, ring, shape)
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = null,
                role = Role.RadioButton,
                onClick = { switcher.set(mode, center) },
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            when (mode) {
                ThemeMode.Light -> swatch(LightScheme.background, LightScheme.surfaceContainer, LightScheme.primary)
                ThemeMode.Dark -> swatch(DarkScheme.background, DarkScheme.surfaceContainerHigh, DarkScheme.primary)
                ThemeMode.System -> {
                    swatch(LightScheme.background, LightScheme.surfaceContainer, LightScheme.primary)
                    val half = Path().apply {
                        moveTo(size.width, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    clipPath(half) {
                        swatch(DarkScheme.background, DarkScheme.surfaceContainerHigh, DarkScheme.primary)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) scheme.primary else scheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** A tiny screen: ground, a card, an accent line. */
private fun DrawScope.swatch(ground: Color, card: Color, accent: Color) {
    drawRect(ground)
    val inset = 8.dp.toPx()
    drawRoundRect(
        color = card,
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2, size.height - inset * 2),
        cornerRadius = CornerRadius(6.dp.toPx()),
    )
    drawRoundRect(
        color = accent,
        topLeft = Offset(inset * 2, size.height - inset * 2 - 6.dp.toPx()),
        size = Size((size.width - inset * 4) * 0.6f, 6.dp.toPx()),
        cornerRadius = CornerRadius(3.dp.toPx()),
    )
}
