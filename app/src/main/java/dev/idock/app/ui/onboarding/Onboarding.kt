package dev.idock.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.idock.app.data.ThemeMode
import dev.idock.app.device.DemoDevice
import dev.idock.app.ui.dashboard.DeviceLabel
import dev.idock.app.ui.kit.CableScene
import dev.idock.app.ui.kit.GradientButton
import dev.idock.app.ui.kit.IconTile
import dev.idock.app.ui.settings.ThemeModePicker
import dev.idock.app.ui.theme.LocalIsDark
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

private const val PAGES = 4

/** The welcome guide shown on the first launch (and again on request from the settings). */
@Composable
fun Onboarding(onFinish: () -> Unit) {
    val pager = rememberPagerState(pageCount = { PAGES })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == PAGES - 1

    BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {},
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                AnimatedVisibility(visible = !last, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = "Пропустить",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClick = onFinish)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Pages fade a little as they slide, so the text never overlaps mid-swipe.
                            val distance = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue
                            alpha = 1f - distance.coerceIn(0f, 1f) * 0.6f
                        }
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp),
                ) {
                    val active = pager.settledPage == page
                    when (page) {
                        0 -> PageWelcome(active)
                        1 -> PageConnect(active)
                        2 -> PageStatus()
                        else -> PageTheme()
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PageDots(pager)
                Spacer(Modifier.weight(1f))
                AnimatedContent(
                    targetState = last,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                    label = "next",
                ) { isLast ->
                    if (isLast) {
                        GradientButton(text = "Начать", onClick = onFinish)
                    } else {
                        GradientButton(
                            text = "Далее",
                            icon = Icons.AutoMirrored.Rounded.ArrowForward,
                            onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageDots(pager: PagerState) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(PAGES) { index ->
            val selected = pager.currentPage == index
            val width by animateFloatAsState(
                targetValue = if (selected) 26f else 8f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                label = "dot",
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(width.dp)
                    .clip(CircleShape)
                    .background(if (selected) scheme.primary else scheme.outlineVariant),
            )
        }
    }
}

@Composable
private fun PageText(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
    Spacer(Modifier.height(12.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PageWelcome(active: Boolean) {
    // The cable plugs in once, when the page is first shown.
    val plug = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active && plug.value == 0f) {
            plug.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 120f))
        }
    }
    Spacer(Modifier.height(28.dp))
    CableScene(
        plug = { plug.value },
        live = active,
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
    )
    Spacer(Modifier.height(36.dp))
    PageText(
        title = "iPhone под управлением этого телефона",
        body = "iDock подключается к iPhone по кабелю и показывает, что внутри: сведения об устройстве, " +
            "файлы, фото, приложения и резервные копии. Компьютер не нужен.",
    )
}

@Composable
private fun PageConnect(active: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val reveal by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "steps",
    )
    Spacer(Modifier.height(28.dp))
    Step(1, Icons.Rounded.Cable, scheme.primary, "Соедините телефоны", "Кабелем или переходником OTG", reveal)
    Step(2, Icons.Rounded.LockOpen, scheme.tertiary, "Разблокируйте iPhone", "И нажмите «Доверять», если он спросит", reveal)
    Step(3, Icons.Rounded.Usb, scheme.secondary, "Разрешите доступ к USB", "Запрос появится на этом телефоне", reveal)
    Spacer(Modifier.height(28.dp))
    PageText(
        title = "Три шага до подключения",
        body = "iPhone подключается к этому телефону так же, как к компьютеру. " +
            "Приложение откроется само, когда увидит его.",
    )
}

@Composable
private fun Step(number: Int, icon: ImageVector, accent: Color, title: String, body: String, reveal: Float) {
    // Each step arrives a little after the previous one; they are a real sequence.
    val local = ((reveal * 3f) - (number - 1) * 0.7f).coerceIn(0f, 1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = local
                translationY = (1f - local) * 18.dp.toPx()
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, accent = accent, size = 52.dp)
        Spacer(Modifier.width(16.dp))
        Column {
            Text("$number. $title", style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PageStatus() {
    val scheme = MaterialTheme.colorScheme
    Spacer(Modifier.height(12.dp))
    DeviceLabel(DemoDevice.info)
    Spacer(Modifier.height(24.dp))
    Text("Что работает уже сейчас", style = MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(12.dp))
    StatusLine(Icons.Rounded.Check, scheme.secondary, "Приложение находит iPhone по USB, показывает его UDID и режим.")
    StatusLine(
        Icons.Rounded.Schedule,
        scheme.tertiary,
        "Файлы, фото, приложения и копии пока работают на демо-данных: так видно, как всё будет устроено.",
    )
}

@Composable
private fun StatusLine(icon: ImageVector, tint: Color, text: String) {
    Row(Modifier.padding(vertical = 6.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PageTheme() {
    val dark = LocalIsDark.current
    Spacer(Modifier.height(28.dp))
    // Only the explicit choices are offered here; "as in the system" stays the default until one is tapped.
    ThemeModePicker(selected = if (dark) ThemeMode.Dark else ThemeMode.Light)
    Spacer(Modifier.height(36.dp))
    PageText(
        title = "Светлая или чёрная",
        body = "Выберите тему прямо сейчас. Позже её можно сменить кнопкой с солнцем в верхнем углу или в настройках.",
    )
}
