package com.thebackendguy.myandroidtestapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thebackendguy.myandroidtestapp.ui.icons.LIcon
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.icons.LucideIcon
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RfType

/**
 * Full-width indigo button with a trailing arrow (the web's PrimaryButton).
 * While [loading] it shows a spinner and ignores taps.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showArrow: Boolean = true,
    loading: Boolean = false
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f)
            .pressable(onClick, pressScale = 0.98f, enabled = enabled && !loading)
            .softShadow(shape, elevation = 4.dp, color = Rf.Primary.copy(alpha = 0.45f))
            .clip(shape)
            .background(Rf.Primary)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) Spinner(Color.White)
        Text(text = text, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Color.White)
        if (showArrow && !loading) LIcon(Lucide.ArrowRight, size = 18.dp, tint = Color.White)
    }
}

/** Soft button in a tinted fill: tonal indigo, or soft red for log out. */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Rf.Container,
    content: Color = Rf.Primary,
    icon: LucideIcon? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressable(onClick, pressScale = 0.98f)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) LIcon(icon, size = 18.dp, tint = content)
        Text(text = text, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = content)
    }
}

/**
 * Compact action inside a card. With [doneLabel] it switches to a check and
 * the done text after a tap, like the web's ActionButton.
 */
@Composable
fun ActionButton(
    label: String,
    icon: LucideIcon,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    doneLabel: String? = null,
    onClick: (() -> Unit)? = null
) {
    var done by rememberSaveable(label) { mutableStateOf(false) }
    Row(
        modifier = modifier
            .pressable({
                if (doneLabel != null) done = true
                onClick?.invoke()
            }, pressScale = 0.97f)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LIcon(if (done) Lucide.CircleCheck else icon, size = 16.dp, tint = content)
        Text(text = if (done && doneLabel != null) doneLabel else label, style = RfType.LabelMd, color = content, maxLines = 1)
    }
}

@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, bold: Boolean = false) {
    Text(
        text = text,
        style = RfType.LabelMd.copy(fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold),
        color = Rf.Primary,
        modifier = modifier.pressable(onClick, pressScale = 0.97f).padding(vertical = 4.dp)
    )
}
