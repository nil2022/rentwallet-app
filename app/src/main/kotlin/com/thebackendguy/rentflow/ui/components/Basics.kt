package com.thebackendguy.rentflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.theme.PlusJakartaSans
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType

const val BRAND_NAME = "RentFlow"

/** White card with 12dp corners and the soft shadow (the web's Surface). */
@Composable
fun RfCard(
    modifier: Modifier = Modifier,
    color: Color = Rf.Lowest,
    shape: Shape = RoundedCornerShape(12.dp),
    padding: PaddingValues = PaddingValues(16.dp),
    spacing: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressable(onClick, pressScale = 0.985f)
            .softShadow(shape)
            .clip(shape)
            .background(color)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

@Composable
fun Pill(
    text: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    dot: Boolean = false,
    dotColor: Color = content,
    style: TextStyle = RfType.LabelSm,
    horizontal: Dp = 8.dp,
    vertical: Dp = 2.dp,
    trailingIcon: LucideIcon? = null
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = horizontal, vertical = vertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (dot) 6.dp else 4.dp)
    ) {
        if (dot) Dot(color = dotColor)
        Text(text = text, style = style, color = content, maxLines = 1)
        if (trailingIcon != null) LIcon(trailingIcon, size = 16.dp, tint = content)
    }
}

@Composable
fun Dot(color: Color, size: Dp = 6.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

/** Rounded square holding an icon (the web's IconTile). */
@Composable
fun IconTile(
    icon: LucideIcon,
    background: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    radius: Dp = 8.dp,
    iconSize: Dp = 18.dp
) {
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(radius)).background(background),
        contentAlignment = Alignment.Center
    ) {
        LIcon(icon, size = iconSize, tint = tint)
    }
}

@Composable
fun Avatar(
    initials: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    style: TextStyle = RfType.LabelMd.copy(fontWeight = FontWeight.Bold),
    online: Boolean = false
) {
    Box(modifier.size(size)) {
        Box(
            modifier = Modifier.fillMaxSize().clip(CircleShape).background(background),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials, style = style.copy(letterSpacing = 0.sp), color = content, maxLines = 1)
        }
        if (online) {
            val dot = if (size >= 56.dp) 12.dp else 9.dp
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(dot)
                    .clip(CircleShape)
                    .background(Rf.Online)
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}

/** The web Home page logo: #4F46E5 tile, 10/34 corner ratio, white Lucide Home. */
@Composable
fun BrandLogo(size: Dp = 34.dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * (10f / 34f))
    Box(
        modifier = modifier
            .size(size)
            .softShadow(shape, elevation = 6.dp, color = Rf.Logo.copy(alpha = 0.55f))
            .clip(shape)
            .background(Rf.Logo),
        contentAlignment = Alignment.Center
    ) {
        LIcon(Lucide.House, size = size / 2, tint = Color.White)
    }
}

/** Logo tile plus the "RentFlow" wordmark. */
@Composable
fun BrandLockup(
    color: Color,
    modifier: Modifier = Modifier,
    logoSize: Dp = 34.dp,
    fontSize: TextUnit = 17.sp,
    gap: Dp = 10.dp
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap)) {
        BrandLogo(logoSize)
        Text(
            text = BRAND_NAME,
            color = color,
            style = TextStyle(
                fontFamily = PlusJakartaSans,
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.02).em
            )
        )
    }
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier, color: Color = Rf.Outline) {
    Text(text = text, style = RfType.LabelSm, color = color, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Section title with an optional action on the right (the web's SectionHead). */
@Composable
fun SectionHead(
    title: String,
    modifier: Modifier = Modifier,
    alertDot: Boolean = false,
    action: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (alertDot) {
                Box(contentAlignment = Alignment.Center) {
                    Dot(Rf.Error.copy(alpha = 0.18f), size = 16.dp)
                    Dot(Rf.Error, size = 8.dp)
                }
            }
            Text(text = title, style = RfType.HeadlineSm, color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        action?.invoke(this)
    }
}

/** "View all →" style link. */
@Composable
fun ArrowLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.pressable(onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = text, style = RfType.LabelMd, color = Rf.Primary)
        LIcon(Lucide.ArrowRight, size = 16.dp, tint = Rf.Primary)
    }
}

/** Two-column grid; each cell gets a modifier that fills its share of the row. */
@Composable
fun <T> Grid2(
    items: List<T>,
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    cell: @Composable (item: T, modifier: Modifier) -> Unit
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
        items.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                row.forEach { item -> cell(item, Modifier.weight(1f).fillMaxHeight()) }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}
