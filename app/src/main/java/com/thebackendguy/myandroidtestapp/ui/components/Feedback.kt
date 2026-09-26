package com.thebackendguy.myandroidtestapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thebackendguy.myandroidtestapp.ui.icons.LIcon
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RfType
import kotlinx.coroutines.delay

/** Round loading spinner for buttons and chips while a request runs. */
@Composable
fun Spinner(color: Color, size: Dp = 18.dp, strokeWidth: Dp = 2.dp) {
    CircularProgressIndicator(Modifier.size(size), color = color, strokeWidth = strokeWidth)
}

/**
 * Error that floats up slowly from the bottom of the screen, stays a few
 * seconds, then slides back down. Tap it to close it sooner.
 */
@Composable
fun FloatingError(message: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val dismiss by rememberUpdatedState(onDismiss)
    // Keeps the text on screen while the note slides away after message turns null
    val last = remember { arrayOf("") }
    if (message != null) last[0] = message

    LaunchedEffect(message) {
        if (message != null) {
            delay(4_500)
            dismiss()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = slideInVertically(tween(650, easing = EaseOutCubic)) { it } + fadeIn(tween(650)),
        exit = slideOutVertically(tween(350, easing = EaseInCubic)) { it } + fadeOut(tween(350))
    ) {
        val shape = RoundedCornerShape(14.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .pressable({ dismiss() }, pressScale = 0.98f)
                .softShadow(shape, elevation = 12.dp, color = Rf.Error.copy(alpha = 0.35f))
                .clip(shape)
                .background(Rf.ErrorContainer)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LIcon(Lucide.TriangleAlert, size = 20.dp, tint = Rf.Error)
            Text(
                text = last[0],
                style = RfType.BodySm.copy(fontWeight = FontWeight.SemiBold),
                color = Rf.OnErrorContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
