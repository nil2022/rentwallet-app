package com.thebackendguy.rentflow.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The colours that change between light and dark. Light is the web's phone
 * design; dark is the web's dark phone palette (rent-management-ui:
 * dashMobileTokens.js, DARK), with the few roles it leaves out filled in to match.
 */
@Immutable
class RfPalette(
    val isDark: Boolean,
    val primary: Color,
    val onPrimary: Color,
    val primaryFixed: Color,
    val primaryFixedDim: Color,
    val onPrimaryFixed: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val surface: Color,
    val lowest: Color,
    val low: Color,
    val container: Color,
    val high: Color,
    val highest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val tertiary: Color,
    val amber: Color,
    val mintSoft: Color,
    val errorWash: Color,
    val amberSoft: Color,
    val sheen: Color,
    val sweep: Color,
    val pinFilled: Color,
    val pinReady: Color,
    val shadow: Color
)

val LightPalette = RfPalette(
    isDark = false,
    primary = Color(0xFF3525CD),
    onPrimary = Color(0xFFFFFFFF),
    primaryFixed = Color(0xFFE2DFFF),
    primaryFixedDim = Color(0xFFC3C0FF),
    onPrimaryFixed = Color(0xFF0F0069),
    secondary = Color(0xFF006C4A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF82F5C1),
    onSecondaryContainer = Color(0xFF00714E),
    surface = Color(0xFFF8F9FF),
    lowest = Color(0xFFFFFFFF),
    low = Color(0xFFEFF4FF),
    container = Color(0xFFE5EEFF),
    high = Color(0xFFDCE9FF),
    highest = Color(0xFFD3E4FE),
    onSurface = Color(0xFF0B1C30),
    onSurfaceVariant = Color(0xFF464555),
    outline = Color(0xFF777587),
    outlineVariant = Color(0xFFC7C4D8),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    tertiary = Color(0xFF950029),
    amber = Color(0xFFD97706),
    mintSoft = Color(0x6682F5C1),      // secondaryContainer at 40%
    errorWash = Color(0x33FFDAD6),     // errorContainer at 20%
    amberSoft = Color(0xFFFEF3C7),
    sheen = Color(0x99FFFFFF),         // the light band over loading photos
    sweep = Color(0xB3FFFFFF),         // Stitch's sweep over loading placeholders
    pinFilled = Color(0xFFDFE5FB),     // a filled box of a sign-in code
    pinReady = Color(0xFF0F9D6A),      // "code complete" on the sign-up check
    shadow = Color(0x290B1C30)         // 0 1px 2px rgba(11,28,48,.06)
)

val DarkPalette = RfPalette(
    isDark = true,
    primary = Color(0xFFAEB0FF),
    onPrimary = Color(0xFF1A0F8F),
    primaryFixed = Color(0xFF2D2A6E),
    primaryFixedDim = Color(0xFF3B3890),
    onPrimaryFixed = Color(0xFFE2DFFF),
    secondary = Color(0xFF68DBA9),
    onSecondary = Color(0xFF00382A),
    secondaryContainer = Color(0xFF0F4D3A),
    onSecondaryContainer = Color(0xFF82F5C1),
    surface = Color(0xFF0C1424),
    lowest = Color(0xFF152036),
    low = Color(0xFF1B2942),
    container = Color(0xFF22334F),
    high = Color(0xFF2A3D5C),
    highest = Color(0xFF34496B),
    onSurface = Color(0xFFE6EEFF),
    onSurfaceVariant = Color(0xFFB4BDD4),
    outline = Color(0xFF8A92AA),
    outlineVariant = Color(0xFF3F4E6B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFFFB3B6),
    amber = Color(0xFFFBBF24),
    mintSoft = Color(0xFF0F4D3A),      // the web uses secondaryContainer for these pills in dark
    errorWash = Color(0x405C1A1A),
    amberSoft = Color(0xFF3D3320),
    sheen = Color(0x1CFFFFFF),         // kept faint so it doesn't glare in the dark
    sweep = Color(0x1FFFFFFF),
    pinFilled = Color(0xFF2A3D5C),
    pinReady = Color(0xFF68DBA9),
    shadow = Color(0x4D000000)         // 0 1px 2px rgba(0,0,0,.3)
)

val LocalRfPalette = staticCompositionLocalOf { LightPalette }

/**
 * RentFlow colours. The Material 3 roles follow the current theme (see
 * [RentFlowTheme]); the brand and dark-card colours are the
 * same in both.
 */
object Rf {
    // Material 3 roles, light or dark
    val Primary: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.primary
    val OnPrimary: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.onPrimary
    val PrimaryFixed: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.primaryFixed
    val PrimaryFixedDim: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.primaryFixedDim
    val OnPrimaryFixed: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.onPrimaryFixed
    val Secondary: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.secondary
    val OnSecondary: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.onSecondary
    val SecondaryContainer: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.secondaryContainer
    val OnSecondaryContainer: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.onSecondaryContainer
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.surface
    val Lowest: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.lowest
    val Low: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.low
    val Container: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.container
    val High: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.high
    val Highest: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.highest
    val OnSurface: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.onSurface
    val OnSurfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.onSurfaceVariant
    val Outline: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.outline
    val OutlineVariant: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.outlineVariant
    val Error: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.error
    val OnError: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.onError
    val ErrorContainer: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.errorContainer
    val OnErrorContainer: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.onErrorContainer
    val Tertiary: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.tertiary
    val Amber: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.amber

    // Soft fills for badges and notes, light or dark
    val MintSoft: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.mintSoft
    val ErrorWash: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.errorWash
    val AmberSoft: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.amberSoft
    /** The band that slides over a photo while it loads. */
    val Sheen: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.sheen
    /** The band that slides over loading placeholders and avatars (Stitch's Wave Sweep). */
    val Sweep: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.sweep
    /** A filled box of a sign-in code, and the "code complete" green. */
    val PinFilled: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.pinFilled
    val PinReady: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.pinReady
    /** Soft shadow the web puts on every card. */
    val Shadow: Color @Composable @ReadOnlyComposable get() = LocalRfPalette.current.shadow

    // Same in both themes: the brand indigo and the dark cards
    val PrimaryContainer = Color(0xFF4F46E5)
    val InverseSurface = Color(0xFF213145)
    val InverseOnSurface = Color(0xFFEAF1FF)

    // Logo tile (web Home page header)
    val Logo = Color(0xFF4F46E5)

    // Tints used on the dark cards and in badges
    val Mint = Color(0xFF85F8C4)
    val Rose = Color(0xFFFFDADA)
    val AmberText = Color(0xFFFDE68A)
    val AmberDot = Color(0xFFFBBF24)
    val Online = Color(0xFF22C55E)
}
