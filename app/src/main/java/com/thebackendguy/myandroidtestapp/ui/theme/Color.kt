package com.thebackendguy.myandroidtestapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * RentFlow palette, taken 1:1 from the web app's phone design
 * (rent-management-ui: dashMobileTokens.js, authMobileTokens.js, mobileTokens.js).
 * The app is light-only, so these are used directly.
 */
object Rf {
    // Material 3 roles
    val Primary = Color(0xFF3525CD)
    val PrimaryContainer = Color(0xFF4F46E5)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryFixed = Color(0xFFE2DFFF)
    val Secondary = Color(0xFF006C4A)
    val SecondaryContainer = Color(0xFF82F5C1)
    val OnSecondaryContainer = Color(0xFF00714E)
    val Surface = Color(0xFFF8F9FF)
    val Lowest = Color(0xFFFFFFFF)
    val Low = Color(0xFFEFF4FF)
    val Container = Color(0xFFE5EEFF)
    val High = Color(0xFFDCE9FF)
    val Highest = Color(0xFFD3E4FE)
    val OnSurface = Color(0xFF0B1C30)
    val OnSurfaceVariant = Color(0xFF464555)
    val Outline = Color(0xFF777587)
    val OutlineVariant = Color(0xFFC7C4D8)
    val Error = Color(0xFFBA1A1A)
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF93000A)
    val Tertiary = Color(0xFF950029)
    val InverseSurface = Color(0xFF213145)
    val InverseOnSurface = Color(0xFFEAF1FF)
    val Amber = Color(0xFFD97706)

    // Logo tile (web Home page header)
    val Logo = Color(0xFF4F46E5)

    // Tints used on the dark cards and in badges
    val Mint = Color(0xFF85F8C4)
    val MintSoft = Color(0x6682F5C1)      // secondaryContainer at 40%
    val MintWash = Color(0x4082F5C1)      // secondaryContainer at 25%
    val ErrorSoft = Color(0x66FFDAD6)     // errorContainer at 40%
    val ErrorWash = Color(0x33FFDAD6)     // errorContainer at 20%
    val Rose = Color(0xFFFFDADA)
    val AmberText = Color(0xFFFDE68A)
    val AmberDot = Color(0xFFFBBF24)
    val AmberSoft = Color(0xFFFEF3C7)
    val Online = Color(0xFF22C55E)
    val PinFilled = Color(0xFFDFE5FB)
    val PinReady = Color(0xFF0F9D6A)

    // Welcome screen (the web's landing page)
    val Midnight = Color(0xFF0B192C)
    val MidnightVia = Color(0xFF0E213B)
    val Slate50 = Color(0xFFF8FAFC)
    val Slate200 = Color(0xFFE2E8F0)
    val Slate300 = Color(0xFFCBD5E1)
    val Slate400 = Color(0xFF94A3B8)
    val Slate500 = Color(0xFF64748B)
    val Slate600 = Color(0xFF475569)
    val Slate700 = Color(0xFF334155)
    val Slate800 = Color(0xFF1E293B)
    val Slate900 = Color(0xFF0F172A)
    val Emerald400 = Color(0xFF34D399)
    val Emerald500 = Color(0xFF10B981)
    val Indigo400 = Color(0xFF818CF8)

    // Soft shadow the web puts on every card: 0 1px 2px rgba(11,28,48,.06)
    val Shadow = Color(0x290B1C30)
}
