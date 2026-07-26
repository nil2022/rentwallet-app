package com.thebackendguy.myandroidtestapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LightPrimary = Color(0xFF005050)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFF006A6A)
val LightOnPrimaryContainer = Color(0xFF97E7E6)
val LightSecondary = Color(0xFF7D5800)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFDCA6F)
val LightOnSecondaryContainer = Color(0xFF775300)
val LightTertiary = Color(0xFF70371A)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFF8D4E2F)
val LightOnTertiaryContainer = Color(0xFFFFCFBA)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF93000A)
val LightBackground = Color(0xFFFBF9F8)
val LightOnBackground = Color(0xFF1B1C1C)
val LightSurface = Color(0xFFFDFBFF)
val LightOnSurface = Color(0xFF1B1C1C)
val LightSurfaceVariant = Color(0xFFE4E2E2)
val LightOnSurfaceVariant = Color(0xFF3E4948)
val LightOutline = Color(0xFF79747E)
val LightOutlineVariant = Color(0xFFBEC9C8)
val LightInverseSurface = Color(0xFF303030)
val LightInverseOnSurface = Color(0xFFF2F0F0)
val LightInversePrimary = Color(0xFF84D4D3)
val LightSurfaceTint = Color(0xFF006A6A)
val LightSurfaceDim = Color(0xFFDBDAD9)
val LightSurfaceBright = Color(0xFFFEF7FF)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF5F3F3)
val LightSurfaceContainer = Color(0xFFF3EDF7)
val LightSurfaceContainerHigh = Color(0xFFE9E8E7)
val LightSurfaceContainerHighest = Color(0xFFE4E2E2)

val DarkPrimary = Color(0xFF84D4D3)
val DarkOnPrimary = Color(0xFF003D3D)
val DarkPrimaryContainer = Color(0xFF005050)
val DarkOnPrimaryContainer = Color(0xFFA0F0F0)
val DarkSecondary = Color(0xFFF0BF65)
val DarkOnSecondary = Color(0xFF3E2B00)
val DarkSecondaryContainer = Color(0xFF7D5800)
val DarkOnSecondaryContainer = Color(0xFFFFDEA9)
val DarkTertiary = Color(0xFFFFB694)
val DarkOnTertiary = Color(0xFF4A1F07)
val DarkTertiaryContainer = Color(0xFF70371A)
val DarkOnTertiaryContainer = Color(0xFFFFDBCC)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)
val DarkBackground = Color(0xFF1B1C1C)
val DarkOnBackground = Color(0xFFF2F0F0)
val DarkSurface = Color(0xFF1B1C1C)
val DarkOnSurface = Color(0xFFF2F0F0)
val DarkSurfaceVariant = Color(0xFF3E4948)
val DarkOnSurfaceVariant = Color(0xFFBEC9C8)
val DarkOutline = Color(0xFF948F9D)
val DarkOutlineVariant = Color(0xFF3E4948)
val DarkInverseSurface = Color(0xFFF2F0F0)
val DarkInverseOnSurface = Color(0xFF303030)
val DarkInversePrimary = Color(0xFF005050)
val DarkSurfaceTint = Color(0xFF84D4D3)
val DarkSurfaceDim = Color(0xFF121313)
val DarkSurfaceBright = Color(0xFF414242)
val DarkSurfaceContainerLowest = Color(0xFF0E0F0F)
val DarkSurfaceContainerLow = Color(0xFF1B1C1C)
val DarkSurfaceContainer = Color(0xFF222323)
val DarkSurfaceContainerHigh = Color(0xFF2C2D2D)
val DarkSurfaceContainerHighest = Color(0xFF373838)

val TenantPrimary = Color(0xFF006A6A)
val TenantOnPrimary = Color(0xFFFFFFFF)
val TenantContainer = Color(0xFF6FF5F5)

val LandlordPrimary = Color(0xFF7D5800)
val LandlordOnPrimary = Color(0xFFFFFFFF)
val LandlordContainer = Color(0xFFFFDDA1)

val Success = Color(0xFF006D3A)
val OnSuccess = Color(0xFFFFFFFF)
val SuccessContainer = Color(0xFF96F7B4)

val Warning = Color(0xFF8B5000)
val OnWarning = Color(0xFFFFFFFF)
val WarningContainer = Color(0xFFFFDCC0)

val Info = Color(0xFF0061A4)
val OnInfo = Color(0xFFFFFFFF)
val InfoContainer = Color(0xFFD1E4FF)

val PositiveCash = Color(0xFF006D3A)
val NegativeCash = Color(0xFFBA1A1A)

val Chart1 = Color(0xFF006A6A)
val Chart2 = Color(0xFF7D5800)
val Chart3 = Color(0xFF0061A4)
val Chart4 = Color(0xFF6750A4)
val Chart5 = Color(0xFF984061)
val Chart6 = Color(0xFF006D3A)

data class RentWalletColors(
    val tenantPrimary: Color,
    val tenantOnPrimary: Color,
    val tenantContainer: Color,
    val landlordPrimary: Color,
    val landlordOnPrimary: Color,
    val landlordContainer: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val positiveCash: Color,
    val negativeCash: Color,
    val chart1: Color,
    val chart2: Color,
    val chart3: Color,
    val chart4: Color,
    val chart5: Color,
    val chart6: Color,
)

val LightRentWalletColors = RentWalletColors(
    tenantPrimary = TenantPrimary,
    tenantOnPrimary = TenantOnPrimary,
    tenantContainer = TenantContainer,
    landlordPrimary = LandlordPrimary,
    landlordOnPrimary = LandlordOnPrimary,
    landlordContainer = LandlordContainer,
    success = Success,
    onSuccess = OnSuccess,
    successContainer = SuccessContainer,
    warning = Warning,
    onWarning = OnWarning,
    warningContainer = WarningContainer,
    info = Info,
    onInfo = OnInfo,
    infoContainer = InfoContainer,
    positiveCash = PositiveCash,
    negativeCash = NegativeCash,
    chart1 = Chart1,
    chart2 = Chart2,
    chart3 = Chart3,
    chart4 = Chart4,
    chart5 = Chart5,
    chart6 = Chart6,
)

val DarkRentWalletColors = RentWalletColors(
    tenantPrimary = Color(0xFF84D4D3),
    tenantOnPrimary = Color(0xFF003D3D),
    tenantContainer = Color(0xFF005050),
    landlordPrimary = Color(0xFFF0BF65),
    landlordOnPrimary = Color(0xFF3E2B00),
    landlordContainer = Color(0xFF7D5800),
    success = Color(0xFF5EDB83),
    onSuccess = Color(0xFF003919),
    successContainer = Color(0xFF00522A),
    warning = Color(0xFFFFB76B),
    onWarning = Color(0xFF4A2800),
    warningContainer = Color(0xFF6B3C00),
    info = Color(0xFF9ACAFF),
    onInfo = Color(0xFF003450),
    infoContainer = Color(0xFF004A72),
    positiveCash = Color(0xFF5EDB83),
    negativeCash = Color(0xFFFFB4AB),
    chart1 = Color(0xFF84D4D3),
    chart2 = Color(0xFFF0BF65),
    chart3 = Color(0xFF9ACAFF),
    chart4 = Color(0xFFCFBCFF),
    chart5 = Color(0xFFFF9DBF),
    chart6 = Color(0xFF5EDB83),
)

val LocalRentWalletColors = staticCompositionLocalOf {
    LightRentWalletColors
}

val MaterialTheme.rentWalletColors: RentWalletColors
    get() = LocalRentWalletColors.current
