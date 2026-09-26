package com.thebackendguy.rentflow.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.decode.DataSource
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType

/* ------------------------------ Loading motion ------------------------------ */

// The curves behind CSS ease-in-out and Tailwind's animate-pulse and animate-ping
private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private val PulseEasing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)
private val PingEasing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

/** Tailwind's animate-pulse: fades to half and back every 2 seconds. */
@Composable
internal fun pulseAlpha(): Float {
    val alpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = 2000
            1f at 0 using PulseEasing
            0.5f at 1000 using PulseEasing
        }),
        label = "pulseAlpha"
    )
    return alpha
}

/** Stitch's sweep over loading placeholders: a wide band, 1.8 s, ease-in-out. */
internal fun Modifier.loadingSweep(): Modifier = composed { sweep(Rf.Sweep, 1800, EaseInOut) }

/** The narrower, slanted band that slides over a photo while it downloads. */
private fun Modifier.photoSheen(): Modifier = composed { sweep(Rf.Sheen, 1400, LinearEasing, band = 0.5f, slant = 0.18f) }

/**
 * Stitch's "Wave Sweep" for a profile photo that is still downloading: a tint
 * that rings outward, a pulsing layer over it, a pulsing camera and the sweep.
 */
@Composable
fun WaveSweep(modifier: Modifier = Modifier, shape: Shape = CircleShape) {
    val motion = rememberInfiniteTransition(label = "waveSweep")
    val ping by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = 1000
            0f at 0 using PingEasing
            1f at 750
        }),
        label = "ping"
    )
    val pulse = pulseAlpha()
    val base = Rf.High
    val tint = Rf.Primary
    BoxWithConstraints(modifier.clip(shape).background(base).loadingSweep(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1f + ping
                    scaleY = 1f + ping
                    alpha = 0.3f * (1f - ping)
                }
                .background(tint.copy(alpha = 0.1f), shape)
        )
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = pulse }.background(base))
        LIcon(
            Lucide.Camera,
            size = maxWidth * 0.42f,
            tint = tint.copy(alpha = 0.7f),
            modifier = Modifier.graphicsLayer { alpha = pulse }
        )
    }
}

/* ------------------------------ Remote photos ------------------------------ */

/** Where one remote photo is: loading, shown or failed. [retry] asks for it again. */
@Stable
class PhotoLoad {
    var state by mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
        internal set
    var attempt by mutableIntStateOf(0)
        private set

    val failed: Boolean get() = state is AsyncImagePainter.State.Error

    fun retry() {
        state = AsyncImagePainter.State.Empty
        attempt++
    }
}

@Composable
fun rememberPhotoLoad(model: Any?): PhotoLoad = remember(model) { PhotoLoad() }

/** 1 once the photo is in, fading in over 0.3 s; photos already in memory show at once. */
@Composable
private fun photoAlpha(load: PhotoLoad): Float {
    val state = load.state
    val instant = state is AsyncImagePainter.State.Success && state.result.dataSource == DataSource.MEMORY_CACHE
    val alpha by animateFloatAsState(
        targetValue = if (state is AsyncImagePainter.State.Success) 1f else 0f,
        animationSpec = if (instant) snap() else tween(300),
        label = "photoFade"
    )
    return alpha
}

/**
 * A photo from a URL or a picked file. While it downloads, the soft indigo
 * placeholder with [placeholderIcon] shows with a light band sliding over it;
 * the photo then fades in. If it can't be fetched, the placeholder says so and
 * offers a retry (only the retry button on small tiles).
 */
@Composable
fun RemoteImage(model: Any?, modifier: Modifier = Modifier, placeholderIcon: LucideIcon = Lucide.Building2, iconSize: Dp = 36.dp) {
    val load = rememberPhotoLoad(model)
    val alpha = photoAlpha(load)
    Box(modifier) {
        if (alpha < 1f) {
            PhotoStandIn(placeholderIcon, iconSize, loading = model != null && !load.failed, failed = load.failed, onRetry = load::retry)
        }
        if (model != null) key(load.attempt) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha },
                onState = { load.state = it }
            )
        }
    }
}

@Composable
private fun PhotoStandIn(icon: LucideIcon, iconSize: Dp, loading: Boolean, failed: Boolean, onRetry: () -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(PhotoPlaceholder)
            .then(if (loading) Modifier.photoSheen() else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val compact = maxWidth < 120.dp || maxHeight < 90.dp
        when {
            !failed -> LIcon(icon, size = min(iconSize, maxWidth * 0.45f), tint = Rf.Primary.copy(alpha = 0.5f))
            compact -> RetryChip(onRetry, withLabel = false)
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LIcon(Lucide.ImageOff, size = 30.dp, tint = Rf.Primary.copy(alpha = 0.6f))
                Text("Photo didn’t load", style = RfType.LabelMd, color = Rf.OnSurface, textAlign = TextAlign.Center)
                RetryChip(onRetry, withLabel = true)
            }
        }
    }
}

@Composable
private fun RetryChip(onRetry: () -> Unit, withLabel: Boolean) {
    Row(
        Modifier
            .pressable(onRetry)
            .clip(CircleShape)
            .background(Rf.Lowest)
            .padding(horizontal = if (withLabel) 10.dp else 6.dp, vertical = if (withLabel) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        LIcon(Lucide.RefreshCw, size = 12.dp, tint = Rf.Primary, strokeWidth = 2.25f, contentDescription = "Retry loading photo")
        if (withLabel) Text("Tap to retry", style = RfType.LabelSm, color = Rf.Primary)
    }
}

/* ------------------------------ Profile photos ------------------------------ */

/**
 * Round profile photo. With no photo it shows the initials. While the photo
 * downloads it shows the Wave Sweep, then fades the photo in; if it can't be
 * fetched it falls back to the initials, with a retry badge when [retryBadge].
 * Pass [load] to follow the download from outside (the photo picker does).
 */
@Composable
fun PersonAvatar(
    name: String,
    photoUrl: String?,
    size: Dp,
    background: Color = Rf.PrimaryFixed,
    content: Color = Rf.Primary,
    online: Boolean = false,
    retryBadge: Boolean = size >= 56.dp,
    load: PhotoLoad = rememberPhotoLoad(photoUrl)
) {
    val initials = name.trim().split(Regex("\\s+")).filter(String::isNotEmpty).take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
    val hasPhoto = !photoUrl.isNullOrBlank()
    val alpha = photoAlpha(load)
    Box(Modifier.size(size)) {
        Box(Modifier.fillMaxSize().clip(CircleShape)) {
            if (alpha < 1f) {
                if (!hasPhoto || load.failed) {
                    Avatar(initials, background = background, content = content, size = size,
                        style = if (size >= 56.dp) RfType.HeadlineMd else RfType.LabelMd.copy(fontWeight = FontWeight.Bold))
                } else {
                    WaveSweep(Modifier.fillMaxSize())
                }
            }
            if (hasPhoto) key(load.attempt) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha },
                    onState = { load.state = it }
                )
            }
        }
        if (retryBadge && load.failed) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp)
                    .size(26.dp)
                    .pressable(load::retry)
                    .clip(CircleShape)
                    .background(Rf.Lowest)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Rf.ErrorContainer),
                contentAlignment = Alignment.Center
            ) {
                LIcon(Lucide.RefreshCw, size = 12.dp, tint = Rf.Error, strokeWidth = 2.25f, contentDescription = "Retry loading photo")
            }
        } else if (online) {
            val dot = if (size >= 56.dp) 12.dp else 9.dp
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(dot)
                    .clip(CircleShape)
                    .background(Rf.Online)
                    .border(2.dp, Rf.Lowest, CircleShape)
            )
        }
    }
}
