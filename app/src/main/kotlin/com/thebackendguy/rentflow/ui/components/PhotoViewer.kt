package com.thebackendguy.rentflow.ui.components

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.thebackendguy.rentflow.data.Photo
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.launch

private fun Photo.model(): Any? = when (this) {
    is Photo.Stored -> url
    is Photo.Picked -> uri
}

/**
 * Photos full screen on a dark backdrop, starting at [start]: swipe between
 * them, pinch or double-tap to zoom. With [onChange] it also offers Crop (for
 * photos not uploaded yet), Make cover and Remove, and hands back the new list.
 */
@Composable
fun PhotoViewer(photos: List<Photo>, start: Int, onClose: () -> Unit, onChange: ((List<Photo>) -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(initialPage = start.coerceIn(0, (photos.size - 1).coerceAtLeast(0))) { photos.size }
    var zoomed by remember { mutableStateOf(false) }
    var cropping by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(photos.isEmpty()) { if (photos.isEmpty()) onClose() }
    if (photos.isEmpty()) return
    val page = pager.currentPage.coerceIn(0, photos.size - 1)
    val current = photos[page]

    DarkPhotoDialog(onDismiss = onClose) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DarkIconButton(Lucide.X, "Close", onClick = onClose)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (photos.size > 1) {
                        Text(
                            "${page + 1} / ${photos.size}",
                            style = RfType.LabelMd,
                            color = Rf.OnSurface,
                            modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Box(Modifier.size(36.dp))
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = !zoomed) { index ->
                    ZoomablePhoto(photos[index].model(), active = index == page, onZoomed = { zoomed = it })
                }
                if (onChange != null) {
                    val tag = when {
                        page == 0 -> "Cover photo"
                        current is Photo.Picked -> "New · not saved yet"
                        else -> "Saved photo"
                    }
                    Text(
                        tag, style = RfType.LabelSm, color = Rf.OnSurface,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                            .clip(CircleShape).background(Color.White.copy(alpha = 0.14f)).padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
                if (photos.size > 1) {
                    Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        photos.indices.forEach { i ->
                            Box(
                                Modifier.height(6.dp).width(if (i == page) 16.dp else 6.dp).animateContentSize()
                                    .clip(CircleShape).background(Color.White.copy(alpha = if (i == page) 1f else 0.45f))
                            )
                        }
                    }
                }
            }

            if (onChange != null) {
                Row(
                    Modifier.fillMaxWidth().background(Rf.Surface).navigationBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ViewerAction(Lucide.Crop, "Crop", enabled = current is Photo.Picked, modifier = Modifier.weight(1f)) {
                        cropping = (current as? Photo.Picked)?.uri
                    }
                    ViewerAction(Lucide.Star, "Make cover", enabled = page > 0, modifier = Modifier.weight(1f)) {
                        onChange(listOf(current) + photos.filterIndexed { i, _ -> i != page })
                        scope.launch { pager.scrollToPage(0) }
                    }
                    ViewerAction(Lucide.Trash2, "Remove", danger = true, modifier = Modifier.weight(1f)) {
                        onChange(photos.filterIndexed { i, _ -> i != page })
                    }
                }
            } else {
                Box(Modifier.navigationBarsPadding().height(12.dp))
            }
        }
    }

    // Crop a new photo again; the result takes its place in the list
    val again = cropping
    if (again != null && onChange != null) {
        CropScreen(
            source = again,
            round = false,
            counter = "Crop again",
            onUse = { cropped ->
                onChange(photos.map { if (it is Photo.Picked && it.uri == again) Photo.Picked(cropped) else it })
                PhotoFiles.discard(context, again)
                cropping = null
            },
            onCancel = { cropping = null }
        )
    }
}

/** One photo that zooms with two fingers or a double tap; one finger pans only when zoomed in. */
@Composable
private fun ZoomablePhoto(model: Any?, active: Boolean, onZoomed: (Boolean) -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(active) {
        if (!active) {
            scale = 1f
            offset = Offset.Zero
        }
    }
    LaunchedEffect(scale, active) { if (active) onZoomed(scale > 1f) }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { tap ->
                    if (scale > 1f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = 2.5f
                        offset = (Offset(size.width / 2f, size.height / 2f) - tap) * 1.5f
                    }
                })
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        if (fingers > 1 || scale > 1f) {
                            scale = (scale * event.calculateZoom()).coerceIn(1f, 4f)
                            val maxX = size.width * (scale - 1f) / 2f
                            val maxY = size.height * (scale - 1f) / 2f
                            val moved = offset + event.calculatePan()
                            offset = Offset(moved.x.coerceIn(-maxX, maxX), moved.y.coerceIn(-maxY, maxY))
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
        )
    }
}

@Composable
private fun ViewerAction(icon: LucideIcon, label: String, modifier: Modifier, enabled: Boolean = true, danger: Boolean = false, onClick: () -> Unit) {
    Column(
        modifier
            .alpha(if (enabled) 1f else 0.35f)
            .pressable(if (enabled) onClick else null)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(if (danger) Rf.ErrorContainer else Rf.Low),
            contentAlignment = Alignment.Center
        ) {
            LIcon(icon, size = 18.dp, tint = if (danger) Rf.Error else Rf.Primary)
        }
        Text(label, style = RfType.LabelSm, color = Rf.OnSurface)
    }
}
