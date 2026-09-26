package com.thebackendguy.myandroidtestapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val RentFlowColorScheme = lightColorScheme(
    primary = Rf.Primary,
    onPrimary = Rf.OnPrimary,
    primaryContainer = Rf.PrimaryContainer,
    onPrimaryContainer = Rf.OnPrimary,
    inversePrimary = Rf.PrimaryFixed,
    secondary = Rf.Secondary,
    onSecondary = Rf.OnPrimary,
    secondaryContainer = Rf.SecondaryContainer,
    onSecondaryContainer = Rf.OnSecondaryContainer,
    tertiary = Rf.Tertiary,
    onTertiary = Rf.OnPrimary,
    background = Rf.Surface,
    onBackground = Rf.OnSurface,
    surface = Rf.Surface,
    onSurface = Rf.OnSurface,
    surfaceVariant = Rf.Container,
    onSurfaceVariant = Rf.OnSurfaceVariant,
    surfaceTint = Rf.Primary,
    inverseSurface = Rf.InverseSurface,
    inverseOnSurface = Rf.InverseOnSurface,
    error = Rf.Error,
    onError = Rf.OnPrimary,
    errorContainer = Rf.ErrorContainer,
    onErrorContainer = Rf.OnErrorContainer,
    outline = Rf.Outline,
    outlineVariant = Rf.OutlineVariant,
    surfaceContainerLowest = Rf.Lowest,
    surfaceContainerLow = Rf.Low,
    surfaceContainer = Rf.Container,
    surfaceContainerHigh = Rf.High,
    surfaceContainerHighest = Rf.Highest
)

/** RentFlow is light-only, matching the web's light phone design. */
@Composable
fun RentFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RentFlowColorScheme,
        typography = RfTypography,
        content = content
    )
}
