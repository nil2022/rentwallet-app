package com.thebackendguy.rentflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.theme.LightPalette
import com.thebackendguy.rentflow.ui.theme.LocalRfPalette
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType

/* ------------------------------ Dark summary card ------------------------------ */

/** Soft blue-white used for secondary text on the dark card, at an opacity. */
fun soft(alpha: Float) = Color(0xFFD3E4FE).copy(alpha = alpha)

/** The web's inverse-surface card with two soft glows (YieldCard). */
@Composable
fun DarkCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val glow = Rf.Primary
    val mintGlow = Rf.Secondary
    Column(
        modifier = modifier
            .fillMaxWidth()
            .softShadow(shape, elevation = 4.dp, color = Color(0x33000000))
            .clip(shape)
            .background(Rf.InverseSurface)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width - 48.dp.toPx(), 48.dp.toPx()),
                        radius = 150.dp.toPx()
                    ),
                    radius = 150.dp.toPx(),
                    center = Offset(size.width - 48.dp.toPx(), 48.dp.toPx())
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(mintGlow.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(40.dp.toPx(), size.height - 40.dp.toPx()),
                        radius = 120.dp.toPx()
                    ),
                    radius = 120.dp.toPx(),
                    center = Offset(40.dp.toPx(), size.height - 40.dp.toPx())
                )
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // The card is the same navy in both themes, so what's on it keeps its light-theme colours
        CompositionLocalProvider(LocalContentColor provides Rf.InverseOnSurface, LocalRfPalette provides LightPalette) { content() }
    }
}

/** Uppercase label pill on the left of the dark card, with an optional status pill. */
@Composable
fun DarkCardHeader(label: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label.uppercase(),
            style = RfType.LabelSm.copy(letterSpacing = 0.05.em),
            color = Rf.InverseOnSurface,
            modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).padding(horizontal = 10.dp, vertical = 4.dp)
        )
        trailing?.invoke()
    }
}

/** Label, big number and an optional note to the right and line below. */
@Composable
fun DarkMetric(label: String, value: String, side: String? = null, sideColor: Color = Rf.Mint, note: String? = null) {
    Column(Modifier.padding(top = 4.dp)) {
        Text(text = label, style = RfType.LabelMd, color = soft(0.7f))
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = value, style = RfType.Metric, color = Color.White)
            if (side != null) Text(text = side, style = RfType.LabelMd, color = sideColor, modifier = Modifier.padding(bottom = 6.dp))
        }
        if (note != null) Text(text = note, style = RfType.LabelSm, color = soft(0.7f), modifier = Modifier.padding(top = 6.dp))
    }
}

/* ------------------------------ Info and alert cards ------------------------------ */

/** Tinted note with an icon (the web's InfoCard). */
@Composable
fun InfoCard(
    icon: LucideIcon,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    iconColor: Color = Rf.Secondary,
    background: Color = Rf.Low,
    titleColor: Color = Rf.OnSurface
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier.fillMaxWidth().softShadow(shape).clip(shape).background(background).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LIcon(icon, size = 20.dp, tint = iconColor, strokeWidth = 1.75f, modifier = Modifier.padding(top = 2.dp))
        Column {
            Text(text = title, style = RfType.LabelSm, color = titleColor)
            Text(text = message, style = RfType.BodySm, color = Rf.OnSurfaceVariant)
        }
    }
}

/** White card with a tinted icon tile, a bold title and a message. */
@Composable
fun AlertCard(icon: LucideIcon, title: String, message: String, tileBg: Color, tileFg: Color) {
    RfCard(padding = PaddingValues(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(icon, background = tileBg, tint = tileFg)
            Column {
                Text(text = title, style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                Text(text = message, style = RfType.BodySm, color = Rf.OnSurfaceVariant)
            }
        }
    }
}

/* ------------------------------ Health tiles ------------------------------ */

data class HealthTileSpec(
    val icon: LucideIcon,
    val badge: String,
    val title: String,
    val sub: String,
    val tileBg: Color,
    val iconColor: Color,
    val badgeColor: Color,
    // null for the usual card, title and caption colours
    val background: Color? = null,
    val titleColor: Color? = null,
    val subColor: Color? = null
)

@Composable
fun HealthTile(spec: HealthTileSpec, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(modifier.softShadow(shape).clip(shape).background(spec.background ?: Rf.Lowest).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconTile(spec.icon, background = spec.tileBg, tint = spec.iconColor)
            Text(text = spec.badge, style = RfType.LabelSm, color = spec.badgeColor)
        }
        Text(text = spec.title, style = RfType.HeadlineSm, color = spec.titleColor ?: Rf.OnSurface, modifier = Modifier.padding(top = 12.dp), maxLines = 1)
        Text(
            text = spec.sub, style = RfType.LabelSm, color = spec.subColor ?: Rf.OnSurfaceVariant, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun HealthTiles(tiles: List<HealthTileSpec>) {
    Grid2(tiles) { spec, cellModifier -> HealthTile(spec, cellModifier) }
}

/* ------------------------------ Ledger cards ------------------------------ */

enum class LedgerTone { Settled, Due, Late }

@Composable
fun LedgerCard(
    initials: String,
    name: String,
    sub: String,
    amount: String,
    status: String,
    tone: LedgerTone,
    meta: String,
    modifier: Modifier = Modifier,
    whenText: String? = null,
    metaIcon: LucideIcon = Lucide.BadgeCheck,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val late = tone == LedgerTone.Late
    val due = tone == LedgerTone.Due
    val avatarBg = when (tone) { LedgerTone.Late -> Rf.ErrorContainer; LedgerTone.Due -> Rf.High; LedgerTone.Settled -> Rf.MintSoft }
    val accent = when (tone) { LedgerTone.Late -> Rf.Error; LedgerTone.Due -> Rf.Primary; LedgerTone.Settled -> Rf.Secondary }
    RfCard(
        modifier = modifier,
        color = if (late) Rf.ErrorWash.compositeOverCard() else Rf.Lowest,
        padding = PaddingValues(14.dp),
        spacing = 8.dp,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(initials, background = avatarBg, content = accent)
                Column {
                    Text(text = name, style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = sub, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = RfType.LabelSm.copy(fontWeight = if (late) FontWeight.Medium else FontWeight.SemiBold),
                        color = if (late) Rf.Error else Rf.OnSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = amount, style = RfType.BodyLg.copy(fontWeight = FontWeight.Bold), color = if (late) Rf.Error else Rf.OnSurface)
                Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Dot(accent)
                    Text(text = status, style = RfType.LabelSm, color = accent)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (action != null) {
                Text(text = meta, style = RfType.LabelSm, color = Rf.OnSurfaceVariant, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "$action →", style = RfType.LabelSm, color = Rf.Primary, modifier = Modifier.pressable(onAction))
            } else {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LIcon(metaIcon, size = 14.dp, tint = if (due) Rf.Primary else Rf.Secondary)
                    Text(text = meta, style = RfType.LabelSm, color = Rf.Outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (whenText != null) Text(text = whenText, style = RfType.LabelSm, color = Rf.Outline)
            }
        }
    }
}

/* ------------------------------ Detail lists ------------------------------ */

enum class DetailStyle { Plain, Mono, SuccessPill }

data class Detail(val label: String, val value: String, val style: DetailStyle = DetailStyle.Plain)

/** Label on the left, value on the right, hairlines between rows. */
@Composable
fun DetailRows(rows: List<Detail>) {
    Column {
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(thickness = 1.dp, color = Rf.Low)
            Row(
                Modifier.fillMaxWidth().padding(top = if (index == 0) 0.dp else 10.dp, bottom = if (index == rows.lastIndex) 0.dp else 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = row.label, style = RfType.BodyMd, color = Rf.OnSurfaceVariant, modifier = Modifier.weight(1f))
                when (row.style) {
                    DetailStyle.SuccessPill -> Pill(row.value, background = Rf.MintSoft, content = Rf.Secondary)
                    else -> Text(
                        text = row.value,
                        style = if (row.style == DetailStyle.Mono) RfType.BodySm.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                        else RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                        color = Rf.OnSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

data class IconDetail(val icon: LucideIcon, val label: String, val value: String)

/** Settings-style rows: icon tile, small label, bold value. */
@Composable
fun IconRows(rows: List<IconDetail>) {
    Column {
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(thickness = 1.dp, color = Rf.Low)
            Row(
                Modifier.fillMaxWidth().padding(top = if (index == 0) 0.dp else 10.dp, bottom = if (index == rows.lastIndex) 0.dp else 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(row.icon, background = Rf.Low, tint = Rf.Primary)
                Column(Modifier.weight(1f)) {
                    Text(text = row.label, style = RfType.LabelSm, color = Rf.Outline)
                    Text(text = row.value, style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Section heading with a white card under it. */
@Composable
fun TitledCard(
    title: String,
    caption: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (caption != null) SectionHead(title) { Caption(caption) } else SectionHead(title)
        RfCard(content = content)
    }
}

/** A dashed rule, used where a receipt tears off. */
@Composable
fun DashedDivider(modifier: Modifier = Modifier, color: Color = Rf.Container) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.5.dp)
            .drawBehind {
                val dash = 6.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(color, Offset(x, size.height / 2), Offset((x + dash).coerceAtMost(size.width), size.height / 2), strokeWidth = size.height)
                    x += dash * 2
                }
            }
    )
}

/** Blend a translucent tint over the card colour, so a tinted card stays opaque over its shadow. */
@Composable
@ReadOnlyComposable
fun Color.compositeOverCard(): Color = compositeOver(Rf.Lowest)
