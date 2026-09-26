package com.thebackendguy.myandroidtestapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebackendguy.myandroidtestapp.data.UserRole
import com.thebackendguy.myandroidtestapp.ui.icons.LIcon
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.icons.LucideIcon
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RfType

/** The curve iOS uses for sheets and segmented controls; the web uses it everywhere. */
val IosEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/** Progress bar that grows in from empty when it first appears (the web's GrowBar). */
@Composable
fun GrowBar(fraction: Float, color: Color, track: Color, height: Dp = 8.dp) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        progress.animateTo(fraction.coerceIn(0f, 1f), tween(durationMillis = 1000, delayMillis = 250, easing = IosEase))
    }
    Box(Modifier.fillMaxWidth().height(height).clip(CircleShape).background(track)) {
        Box(Modifier.fillMaxWidth(progress.value).fillMaxHeight().clip(CircleShape).background(color))
    }
}

/** Pill-shaped segmented filter with a sliding white thumb. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Rf.Low)
            .padding(4.dp)
    ) {
        val segment = maxWidth / options.size
        val offset by animateDpAsState(segment * selected, tween(400, easing = IosEase), label = "segmentThumb")
        Box(
            Modifier
                .offset(x = offset)
                .width(segment)
                .height(28.dp)
                .softShadow(CircleShape)
                .clip(CircleShape)
                .background(Rf.Lowest)
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                val active = index == selected
                val color by animateColorAsState(if (active) Rf.Primary else Rf.OnSurfaceVariant, label = "segmentText")
                Box(
                    Modifier.weight(1f).height(28.dp).pressable({ onSelect(index) }, pressScale = 0.97f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = RfType.LabelMd.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium),
                        color = color,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

enum class QuickTone { Primary, Secondary, Surface, Low }

data class QuickAction(val label: String, val icon: LucideIcon, val tone: QuickTone, val onClick: (() -> Unit)? = null)

/** "Quick actions" row of pills that scrolls sideways past the screen padding. */
@Composable
fun QuickActions(actions: List<QuickAction>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Quick actions", style = RfType.LabelMd, color = Rf.OnSurfaceVariant)
            Caption("Swipe for more")
        }
        Row(
            Modifier
                .bleed(16.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            actions.forEach { action ->
                val (bg, fg, iconTint) = when (action.tone) {
                    QuickTone.Primary -> Triple(Rf.Primary, Color.White, Color.White)
                    QuickTone.Secondary -> Triple(Rf.SecondaryContainer, Rf.OnSecondaryContainer, Rf.OnSecondaryContainer)
                    QuickTone.Surface -> Triple(Rf.Lowest, Rf.OnSurface, Rf.Primary)
                    QuickTone.Low -> Triple(Rf.Low, Rf.OnSurfaceVariant, Rf.Tertiary)
                }
                Row(
                    Modifier
                        .pressable(action.onClick)
                        .softShadow(CircleShape)
                        .clip(CircleShape)
                        .background(bg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LIcon(action.icon, size = 18.dp, tint = iconTint)
                    Text(text = action.label, style = RfType.LabelMd, color = fg, maxLines = 1)
                }
            }
        }
    }
}

/** Landlord / Tenant switch on the login screen (the web's RoleSwitch). */
@Composable
fun RoleSwitch(selected: UserRole, onSelect: (UserRole) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(UserRole.Landlord to Lucide.Building2, UserRole.Tenant to Lucide.House)
    val shape = RoundedCornerShape(12.dp)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .softShadow(shape)
            .clip(shape)
            .background(Rf.Low)
            .padding(4.dp)
    ) {
        val half = maxWidth / 2
        val offset by animateDpAsState(
            if (selected == UserRole.Landlord) 0.dp else half,
            tween(450, easing = IosEase),
            label = "roleThumb"
        )
        Box(
            Modifier
                .offset(x = offset)
                .width(half)
                .height(40.dp)
                .softShadow(RoundedCornerShape(8.dp), elevation = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Rf.Lowest)
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEach { (role, icon) ->
                val active = role == selected
                val color by animateColorAsState(if (active) Rf.Primary else Rf.OnSurfaceVariant, label = "roleText")
                Row(
                    Modifier.weight(1f).height(40.dp).pressable({ onSelect(role) }, pressScale = 1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LIcon(icon, size = 18.dp, tint = color, strokeWidth = if (active) 2.25f else 1.75f)
                    Text(
                        text = role.name,
                        style = RfType.LabelMd.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Medium),
                        color = color
                    )
                }
            }
        }
    }
}

/** Six single-digit boxes over one hidden text field (the web's OtpBoxes). */
@Composable
fun OtpBoxes(code: String, onCodeChange: (String) -> Unit, modifier: Modifier = Modifier, length: Int = 6, error: Boolean = false) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    BasicTextField(
        value = androidx.compose.ui.text.input.TextFieldValue(code, TextRange(code.length)),
        onValueChange = { onCodeChange(it.text.filter(Char::isDigit).take(length)) },
        modifier = modifier.fillMaxWidth(),
        interactionSource = source,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(length) { index ->
                    val digit = code.getOrNull(index)
                    val active = focused && index == code.length.coerceAtMost(length - 1) && code.length < length
                    val shape = RoundedCornerShape(10.dp)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(shape)
                            .background(
                                when {
                                    active -> Rf.Lowest
                                    digit != null -> Rf.PinFilled
                                    else -> Rf.Low
                                }
                            )
                            .border(
                                1.5.dp,
                                when {
                                    error -> Rf.Error
                                    active -> Rf.Primary
                                    else -> Color.Transparent
                                },
                                shape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit?.toString() ?: "•",
                            style = RfType.HeadlineSm.copy(
                                fontSize = 18.sp,
                                fontWeight = if (digit != null) FontWeight.ExtraBold else FontWeight.Normal
                            ),
                            color = if (digit != null) Rf.OnSurface else Rf.Outline
                        )
                    }
                }
            }
        }
    )
}
