package com.thebackendguy.rentflow.ui.components

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
import androidx.compose.runtime.collectAsState
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
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Round loading spinner for buttons and chips while a request runs. */
@Composable
fun Spinner(color: Color, size: Dp = 18.dp, strokeWidth: Dp = 2.dp) {
    CircularProgressIndicator(Modifier.size(size), color = color, strokeWidth = strokeWidth)
}

/**
 * A note that floats up slowly from the bottom of the screen, stays a few
 * seconds, then slides back down. Tap it to close it sooner. A new [key]
 * shows it again even when the text is the same.
 */
@Composable
fun FloatingNotice(
    message: String?,
    isError: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    key: Any? = message
) {
    val dismiss by rememberUpdatedState(onDismiss)
    // Keeps the text and colour on screen while the note slides away after message turns null
    val last = remember { Shown() }
    if (message != null) {
        last.text = message
        last.error = isError
    }

    LaunchedEffect(key, message != null) {
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
        val error = last.error
        val shape = RoundedCornerShape(14.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .pressable({ dismiss() }, pressScale = 0.98f)
                .softShadow(shape, elevation = 12.dp, color = if (error) Rf.Error.copy(alpha = 0.35f) else Rf.Shadow)
                .clip(shape)
                .background(if (error) Rf.ErrorContainer else Rf.InverseSurface)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (error) LIcon(Lucide.TriangleAlert, size = 20.dp, tint = Rf.Error)
            else LIcon(Lucide.CircleCheck, size = 20.dp, tint = Rf.Mint)
            Text(
                text = last.text,
                style = RfType.BodySm.copy(fontWeight = FontWeight.SemiBold),
                color = if (error) Rf.OnErrorContainer else Color.White,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private class Shown(var text: String = "", var error: Boolean = true)

/** The red version, for the sign-in screens. */
@Composable
fun FloatingError(message: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    FloatingNotice(message, isError = true, onDismiss = onDismiss, modifier = modifier)
}

/** App-wide short messages, such as "Property saved", shown by [ToastHost]. */
object Toasts {
    data class Toast(val text: String, val isError: Boolean, val id: Long = System.nanoTime())

    private val _current = MutableStateFlow<Toast?>(null)
    val current: StateFlow<Toast?> = _current

    fun show(text: String) {
        _current.value = Toast(text, isError = false)
    }

    fun error(text: String) {
        _current.value = Toast(text, isError = true)
    }

    fun dismiss(id: Long) {
        if (_current.value?.id == id) _current.value = null
    }
}

@Composable
fun ToastHost(modifier: Modifier = Modifier) {
    val toast by Toasts.current.collectAsState()
    FloatingNotice(
        message = toast?.text,
        isError = toast?.isError ?: false,
        onDismiss = { toast?.let { Toasts.dismiss(it.id) } },
        modifier = modifier,
        key = toast?.id
    )
}
