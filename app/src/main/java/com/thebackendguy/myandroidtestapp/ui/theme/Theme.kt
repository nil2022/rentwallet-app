package com.thebackendguy.myandroidtestapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryVariant,
    onPrimary = Surface,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnSurface,
    secondary = SecondaryVariant,
    onSecondary = Surface,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSurface,
    tertiary = TertiaryVariant,
    onTertiary = Surface,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnSurface,
    background = Background,
    onBackground = OnBackground,
    surface = Background,
    onSurface = OnBackground,
    surfaceVariant = Surface,
    onSurfaceVariant = OnSurface,
    outline = OnSurface,
    inverseOnSurface = Surface,
    inverseSurface = OnBackground,
    error = Error,
    onError = OnError,
    errorContainer = Error,
    onErrorContainer = OnError
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnSurface,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnSurface,
    secondary = Secondary,
    onSecondary = OnSurface,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSurface,
    tertiary = Tertiary,
    onTertiary = OnSurface,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnSurface,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = Surface,
    onSurfaceVariant = OnSurface,
    outline = OnSurface,
    inverseOnSurface = Surface,
    inverseSurface = Background,
    error = Error,
    onError = OnError,
    errorContainer = Error,
    onErrorContainer = OnError
)

@Composable
fun MyAndroidTestAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled by default so custom palette is used
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}