package dev.idock.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import dev.idock.app.ui.screens.FeatureScreen
import dev.idock.app.ui.theme.IDockTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val SHOTS = "build/shots"

/** Runs the real activity on the JVM: first launch, second launch, tabs, theme change. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-420dpi")
class AppFlowTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    private val prefs
        get() = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("idock", Context.MODE_PRIVATE)

    private fun settle(ms: Long = 1500) = rule.mainClock.advanceTimeBy(ms)

    private fun shot(name: String) {
        settle()
        rule.onRoot().captureRoboImage("$SHOTS/$name.png")
    }

    @Test
    fun firstLaunchShowsGuideThenAuthorOnce() {
        prefs.edit().clear().commit()
        rule.mainClock.autoAdvance = false
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            shot("01_guide_1")
            rule.onNodeWithText("Далее").performClick()
            shot("02_guide_2")
            rule.onNodeWithText("Далее").performClick()
            shot("03_guide_3")
            rule.onNodeWithText("Далее").performClick()
            shot("04_guide_4")
            rule.onNodeWithText("Начать").performClick()
            settle()
            rule.onNodeWithText("nak1ost").assertIsDisplayed()
            shot("05_author")
            rule.onNodeWithText("Продолжить").performClick()
            shot("06_device_empty")
            rule.onNodeWithText("Показать демо-устройство").performClick()
            settle(3000)
            shot("07_device_demo")

            // Second start: no guide, no author card.
            scenario.recreate()
            settle()
            rule.onNodeWithText("nak1ost").assertDoesNotExist()
            rule.onNodeWithText("Пропустить").assertDoesNotExist()
            rule.onNodeWithText("Демо-устройство").assertIsDisplayed()
        }
    }

    @Test
    fun tabsSettingsAndThemeChange() {
        prefs.edit().clear()
            .putBoolean("onboarding_done", true)
            .putBoolean("author_credit_shown", true)
            .putBoolean("demo_device", true)
            .putInt("theme_mode", 1)
            .commit()
        fun tab(name: String) =
            rule.onNode(hasText(name) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        fun idleShot(name: String) {
            rule.waitForIdle()
            rule.onRoot().captureRoboImage("$SHOTS/$name.png")
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.waitForIdle()
            rule.onNodeWithText("nak1ost").assertDoesNotExist()
            listOf("Фото" to "photos", "Файлы" to "files", "Приложения" to "apps", "Инструменты" to "tools", "Устройство" to "device")
                .forEach { (label, file) ->
                    tab(label).performClick()
                    rule.waitForIdle()
                    tab(label).assertIsSelected()
                    idleShot("10_tab_$file")
                }

            // The theme change is caught half-way, while the new theme is still spreading.
            rule.mainClock.autoAdvance = false
            rule.onNodeWithContentDescription("Включить чёрную тему").performClick()
            rule.mainClock.advanceTimeBy(320)
            rule.onRoot().captureRoboImage("$SHOTS/11_theme_reveal.png")
            rule.mainClock.autoAdvance = true
            idleShot("12_device_black")
            rule.onNodeWithContentDescription("Включить светлую тему").assertIsDisplayed()

            rule.onNodeWithContentDescription("Настройки").performClick()
            idleShot("13_settings_black")
            rule.onNodeWithText("Светлая").performClick()
            idleShot("14_settings_light")
            rule.onNodeWithContentDescription("Назад").performClick()
            rule.waitForIdle()
            rule.onNodeWithText("iDock").assertIsDisplayed()
            rule.onNodeWithContentDescription("Включить чёрную тему").assertIsDisplayed()

            // Tools: every tile either opens its screen or says it is not ready yet.
            tab("Инструменты").performClick()
            rule.waitForIdle()
            rule.onNodeWithText("Рингтоны").performClick()
            idleShot("16_ringtones_in_shell")
            rule.onNodeWithContentDescription("Назад").performClick()
            rule.waitForIdle()
            tab("Инструменты").assertIsSelected()
        }
    }
}

/** Renders every feature screen in both themes and lets its animations run for a while. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-420dpi")
class FeatureScreensTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun everyScreenRendersInBothThemes() {
        var screen by mutableStateOf("tab:Photos")
        var dark by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            IDockTheme(dark = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    key(screen) {
                        FeatureScreen(
                            key = screen,
                            contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp),
                            onOpenTool = {},
                        )
                    }
                }
            }
        }
        val screens = listOf(
            "tab:Photos", "tab:Files", "tab:Apps", "tab:Tools",
            "backup", "ringtones", "screenshot", "logs", "verify",
        )
        for (isDark in listOf(false, true)) {
            for (name in screens) {
                rule.runOnUiThread {
                    dark = isDark
                    screen = name
                }
                rule.mainClock.advanceTimeByFrame()
                rule.mainClock.advanceTimeBy(4500)
                val file = name.removePrefix("tab:") + if (isDark) "_black" else "_light"
                rule.onRoot().captureRoboImage("$SHOTS/20_$file.png")
            }
        }
    }
}
