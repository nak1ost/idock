package dev.idock.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { System, Light, Dark }

/** Small persistent settings, exposed as Compose state so the UI follows every change at once. */
class Prefs(context: Context) {
    private val store = context.getSharedPreferences("idock", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(
        ThemeMode.entries.getOrElse(store.getInt(KEY_THEME, 0)) { ThemeMode.System },
    )
        private set

    /** The welcome guide has been finished or skipped. */
    var onboardingDone by mutableStateOf(store.getBoolean(KEY_ONBOARDING, false))
        private set

    /** The one-time author card has been shown. It never appears again after that. */
    var creditShown by mutableStateOf(store.getBoolean(KEY_CREDIT, false))
        private set

    /** Show a sample device while no iPhone is connected. */
    var demoDevice by mutableStateOf(store.getBoolean(KEY_DEMO, false))
        private set

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        store.edit().putInt(KEY_THEME, mode.ordinal).apply()
    }

    fun completeOnboarding() {
        onboardingDone = true
        store.edit().putBoolean(KEY_ONBOARDING, true).apply()
    }

    fun markCreditShown() {
        creditShown = true
        store.edit().putBoolean(KEY_CREDIT, true).apply()
    }

    fun updateDemoDevice(enabled: Boolean) {
        demoDevice = enabled
        store.edit().putBoolean(KEY_DEMO, enabled).apply()
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_ONBOARDING = "onboarding_done"
        const val KEY_CREDIT = "author_credit_shown"
        const val KEY_DEMO = "demo_device"
    }
}
