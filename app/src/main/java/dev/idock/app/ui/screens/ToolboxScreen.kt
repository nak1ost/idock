package dev.idock.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.East
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------------------------
// Demo data
// ---------------------------------------------------------------------------------------------

private enum class ToolboxAccent { Primary, Secondary, Tertiary, Error }

private data class ToolboxTool(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: ToolboxAccent,
    val soon: Boolean = false,
)

private data class ToolboxSection(
    val title: String,
    val caption: String,
    val tools: List<ToolboxTool>,
)

private val ToolboxSections = listOf(
    ToolboxSection(
        title = "Основное",
        caption = "4 инструмента",
        tools = listOf(
            ToolboxTool("backup", "Резервная копия", "Копия и восстановление", Icons.Rounded.Backup, ToolboxAccent.Primary),
            ToolboxTool("verify", "Проверка устройства", "Подлинность и состояние", Icons.Rounded.VerifiedUser, ToolboxAccent.Secondary),
            ToolboxTool("power", "Питание", "Перезагрузка и выключение", Icons.Rounded.PowerSettingsNew, ToolboxAccent.Error),
            ToolboxTool("cleanup", "Очистка", "Кэш и временные файлы", Icons.Rounded.DeleteSweep, ToolboxAccent.Tertiary, soon = true),
        ),
    ),
    ToolboxSection(
        title = "Медиа",
        caption = "4 инструмента",
        tools = listOf(
            ToolboxTool("ringtones", "Рингтоны", "Нарезка из любого трека", Icons.Rounded.MusicNote, ToolboxAccent.Tertiary),
            ToolboxTool("screenshot", "Снимок экрана", "Захват и трансляция", Icons.Rounded.Screenshot, ToolboxAccent.Primary),
            ToolboxTool("heic", "Конвертер HEIC → JPG", "Фото в привычном формате", Icons.Rounded.Image, ToolboxAccent.Secondary, soon = true),
            ToolboxTool("audio", "Конвертер аудио", "MP3, AAC, M4R и другие", Icons.Rounded.GraphicEq, ToolboxAccent.Tertiary, soon = true),
        ),
    ),
    ToolboxSection(
        title = "Система",
        caption = "3 инструмента",
        tools = listOf(
            ToolboxTool("logs", "Журнал устройства", "Системные сообщения онлайн", Icons.Rounded.Description, ToolboxAccent.Secondary),
            ToolboxTool("crashes", "Отчёты о сбоях", "Падения приложений", Icons.Rounded.BugReport, ToolboxAccent.Error, soon = true),
            ToolboxTool("icons", "Рабочий стол", "Порядок иконок и папок", Icons.Rounded.GridView, ToolboxAccent.Primary, soon = true),
        ),
    ),
)

private const val ToolboxStaggerMs = 35f
private const val ToolboxEnterMs = 350f
private const val ToolboxStaggerCap = 10

// ---------------------------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------------------------

@Composable
fun ToolboxScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
    onOpenTool: (String) -> Unit = {},
) {
    val layoutDirection = LocalLayoutDirection.current
    // One clock drives the whole staggered entrance; it runs once and then rests at its end value,
    // so items scrolled out and back in are simply fully visible.
    val enterClock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val total = ToolboxStaggerMs * ToolboxStaggerCap + ToolboxEnterMs
        enterClock.animateTo(total, tween(durationMillis = total.toInt(), easing = LinearEasing))
    }

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
        item(key = "hero") {
            ToolboxHeroCard(
                modifier = Modifier.toolboxEnter(enterClock, 0),
                onClick = { onOpenTool("backup") },
            )
        }

        var order = 1
        ToolboxSections.forEach { section ->
            val headerIndex = order++
            item(key = "header-${section.title}") {
                ToolboxSectionHeader(
                    section = section,
                    modifier = Modifier.toolboxEnter(enterClock, headerIndex),
                )
            }
            section.tools.chunked(2).forEachIndexed { rowIndex, pair ->
                val firstIndex = order
                order += pair.size
                item(key = "row-${section.title}-$rowIndex") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        pair.forEachIndexed { j, tool ->
                            ToolboxTile(
                                tool = tool,
                                onClick = { onOpenTool(tool.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .toolboxEnter(enterClock, firstIndex + j),
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item(key = "footer") {
            Text(
                text = "Инструменты с пометкой «Скоро» появятся в следующих версиях",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 24.dp, end = 24.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Motion helpers
// ---------------------------------------------------------------------------------------------

/** Fade + rise entrance; reads the clock only inside the layer block (no recomposition). */
private fun Modifier.toolboxEnter(
    clock: Animatable<Float, AnimationVector1D>,
    index: Int,
): Modifier = this.graphicsLayer {
    val p = if (index >= ToolboxStaggerCap) {
        1f
    } else {
        val raw = ((clock.value - index * ToolboxStaggerMs) / ToolboxEnterMs).coerceIn(0f, 1f)
        FastOutSlowInEasing.transform(raw)
    }
    alpha = p
    translationY = (1f - p) * 24.dp.toPx()
}

@Composable
private fun toolboxPressScale(interaction: MutableInteractionSource): () -> Float {
    val pressed by interaction.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "toolboxPress",
    )
    return { scale.value }
}

@Composable
private fun toolboxAccentColor(accent: ToolboxAccent): Color {
    val cs = MaterialTheme.colorScheme
    return when (accent) {
        ToolboxAccent.Primary -> cs.primary
        ToolboxAccent.Secondary -> cs.secondary
        ToolboxAccent.Tertiary -> cs.tertiary
        ToolboxAccent.Error -> cs.error
    }
}

// ---------------------------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun ToolboxHeroCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val scale = toolboxPressScale(interaction)
    val gradient = remember(cs.primary, cs.tertiary) {
        Brush.linearGradient(listOf(cs.primary, cs.tertiary))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            }
            .clip(RoundedCornerShape(28.dp))
            .background(gradient)
            .drawBehind {
                // Soft light orbs on the gradient — static, no animation on an idle screen.
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = size.height * 0.85f,
                    center = Offset(size.width * 1.02f, -size.height * 0.12f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.07f),
                    radius = size.height * 0.55f,
                    center = Offset(size.width * 0.82f, size.height * 1.15f),
                )
            }
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = cs.onPrimary),
                onClick = onClick,
            )
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Backup,
                        contentDescription = null,
                        tint = cs.onPrimary,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Рекомендуем",
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onPrimary.copy(alpha = 0.8f),
                    )
                    Text(
                        text = "Резервная копия",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onPrimary,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "Последняя копия — 11 ноября, 21:40",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onPrimary.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "18,4",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onPrimary,
                )
                Text(
                    text = " ГБ",
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .heightIn(min = 40.dp)
                        .clip(CircleShape)
                        .background(cs.onPrimary)
                        .padding(start = 16.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Создать",
                        style = MaterialTheme.typography.labelLarge,
                        color = cs.primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.East,
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "12 480 фото · 86 приложений · не зашифрована",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onPrimary.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun ToolboxSectionHeader(
    section: ToolboxSection,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = section.caption,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ToolboxTile(
    tool: ToolboxTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val accent = toolboxAccentColor(tool.accent)
    val interaction = remember { MutableInteractionSource() }
    val scale = toolboxPressScale(interaction)

    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            val s = scale()
            scaleX = s
            scaleY = s
        },
        shape = RoundedCornerShape(20.dp),
        color = cs.surfaceContainer,
        interactionSource = interaction,
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = 132.dp)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                ToolboxIconTile(
                    icon = tool.icon,
                    accent = accent,
                    muted = tool.soon,
                )
                Spacer(Modifier.weight(1f))
                if (tool.soon) ToolboxSoonPill()
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(14.dp))
            Text(
                text = tool.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (tool.soon) cs.onSurface.copy(alpha = 0.78f) else cs.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = tool.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ToolboxIconTile(
    icon: ImageVector,
    accent: Color,
    muted: Boolean,
) {
    val tint = if (muted) accent.copy(alpha = 0.7f) else accent
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = if (muted) 0.08f else 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ToolboxSoonPill() {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "Скоро",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
