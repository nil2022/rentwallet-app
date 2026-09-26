package com.thebackendguy.rentflow.ui.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A Lucide icon: 24x24 stroked paths. The web varies the stroke width by
 * context (1.75 for field icons, 2.25 for the active tab), so vectors are
 * built per stroke width and cached.
 */
class LucideIcon(private val name: String, private vararg val paths: String) {
    private val cache = HashMap<Float, ImageVector>()

    fun vector(strokeWidth: Float = 2f): ImageVector = cache.getOrPut(strokeWidth) {
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()
    }
}

@Composable
fun LIcon(
    icon: LucideIcon,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = LocalContentColor.current,
    strokeWidth: Float = 2f,
    contentDescription: String? = null
) {
    val vector = remember(icon, strokeWidth) { icon.vector(strokeWidth) }
    Icon(
        imageVector = vector,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint
    )
}
