package com.thebackendguy.myandroidtestapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thebackendguy.myandroidtestapp.ui.theme.Rf

/**
 * The web's Pressable: shrinks a little while pressed instead of a ripple.
 * Does nothing when [onClick] is null, so display-only items stay inert.
 */
fun Modifier.pressable(
    onClick: (() -> Unit)?,
    pressScale: Float = 0.95f,
    enabled: Boolean = true
): Modifier = if (onClick == null) this else composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressScale else 1f,
        animationSpec = tween(150),
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = source,
        indication = null,
        enabled = enabled,
        role = Role.Button,
        onClick = onClick
    )
}

/** The subtle card shadow used across the web's phone screens. */
fun Modifier.softShadow(shape: Shape, elevation: Dp = 1.dp, color: Color = Rf.Shadow): Modifier =
    shadow(elevation = elevation, shape = shape, clip = false, ambientColor = color, spotColor = color)

/** Lets a row scroll edge to edge past its parent's side padding (the web's -mx-4 px-4). */
fun Modifier.bleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra * 2,
            maxWidth = constraints.maxWidth + extra * 2
        )
    )
    layout(placeable.width - extra * 2, placeable.height) {
        placeable.place(-extra, 0)
    }
}
