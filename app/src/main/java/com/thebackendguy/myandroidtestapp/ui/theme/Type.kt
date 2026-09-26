package com.thebackendguy.myandroidtestapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.thebackendguy.myandroidtestapp.R

// Plus Jakarta Sans variable font; each weight sets the font's wght axis
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
)

private fun style(size: Int, lineHeight: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = tracking.em
)

/** One-off Plus Jakarta Sans style, for the landing page's Tailwind sizes. */
fun jakarta(
    size: Double,
    weight: FontWeight,
    lineHeight: Double? = null,
    tracking: Double = 0.0
) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = if (lineHeight != null) (size * lineHeight).sp else TextUnit.Unspecified,
    letterSpacing = tracking.em
)

/** The web's phone type scale (dashMobileTokens.js `t` and authMobileTokens.js `type`). */
object RfType {
    val Metric = style(32, 38, FontWeight.Bold, -0.025)
    val HeadlineLg = style(28, 36, FontWeight.Bold, -0.02)
    val HeadlineMd = style(22, 28, FontWeight.SemiBold, -0.015)
    val HeadlineSm = style(18, 24, FontWeight.SemiBold, -0.01)
    val BodyLg = style(16, 24, FontWeight.Normal, -0.005)
    val BodyMd = style(14, 20, FontWeight.Normal)
    val BodySm = style(13, 18, FontWeight.Normal)
    val LabelMd = style(13, 16, FontWeight.SemiBold, 0.01)
    val LabelSm = style(11, 14, FontWeight.SemiBold, 0.03)
}

// Material components (dialogs, menus, drawer) pick up the same font
val RfTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = PlusJakartaSans),
        displayMedium = displayMedium.copy(fontFamily = PlusJakartaSans),
        displaySmall = displaySmall.copy(fontFamily = PlusJakartaSans),
        headlineLarge = RfType.HeadlineLg,
        headlineMedium = RfType.HeadlineMd,
        headlineSmall = RfType.HeadlineSm,
        titleLarge = titleLarge.copy(fontFamily = PlusJakartaSans),
        titleMedium = titleMedium.copy(fontFamily = PlusJakartaSans),
        titleSmall = titleSmall.copy(fontFamily = PlusJakartaSans),
        bodyLarge = RfType.BodyLg,
        bodyMedium = RfType.BodyMd,
        bodySmall = RfType.BodySm,
        labelLarge = labelLarge.copy(fontFamily = PlusJakartaSans),
        labelMedium = RfType.LabelMd,
        labelSmall = RfType.LabelSm
    )
}
