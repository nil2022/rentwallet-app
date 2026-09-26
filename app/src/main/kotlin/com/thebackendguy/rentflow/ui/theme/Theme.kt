package com.thebackendguy.rentflow.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit

private fun RfPalette.colorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = Rf.PrimaryContainer,
        onPrimaryContainer = Color.White,
        inversePrimary = primaryFixed,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onPrimary,
        background = surface,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = container,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary,
        inverseSurface = Rf.InverseSurface,
        inverseOnSurface = Rf.InverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        surfaceContainerLowest = lowest,
        surfaceContainerLow = low,
        surfaceContainer = container,
        surfaceContainerHigh = high,
        surfaceContainerHighest = highest
    )
}

private val LightScheme = LightPalette.colorScheme()
private val DarkScheme = DarkPalette.colorScheme()

/** RentFlow's colours, type and Material theme, light or [dark]. */
@Composable
fun RentFlowTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRfPalette provides if (dark) DarkPalette else LightPalette) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = RfTypography,
            content = content
        )
    }
}

/**
 * The light/dark choice from the moon/sun button. Like the web, it follows the
 * phone until the user picks one, and the pick is remembered (also after
 * logging out). On Android 12+ the pick is also passed to the system, so the
 * launch screen opens in it too.
 */
object ThemeMode {
    private const val KEY = "theme_mode"
    private var prefs: SharedPreferences? = null
    private var uiMode: UiModeManager? = null

    /** true for dark, false for light, null to follow the phone. */
    private var picked by mutableStateOf<Boolean?>(null)

    fun init(context: Context) {
        val store = context.getSharedPreferences("rentflow_settings", Context.MODE_PRIVATE)
        prefs = store
        uiMode = context.getSystemService(UiModeManager::class.java)
        picked = when (store.getString(KEY, null)) {
            "dark" -> true
            "light" -> false
            else -> null
        }
        // Brings the system in line with a pick made before it was told about picks
        picked?.let(::tellSystem)
    }

    /** Whether the app is dark right now. */
    @Composable
    fun isDark(): Boolean = picked ?: isSystemInDarkTheme()

    fun set(dark: Boolean) {
        picked = dark
        prefs?.edit { putString(KEY, if (dark) "dark" else "light") }
        tellSystem(dark)
    }

    /** The app's night mode for Android 12+, which draws the launch screen before any of our code runs. */
    private fun tellSystem(dark: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        uiMode?.setApplicationNightMode(if (dark) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO)
    }
}
