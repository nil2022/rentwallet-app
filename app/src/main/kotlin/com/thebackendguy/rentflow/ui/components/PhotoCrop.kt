package com.thebackendguy.rentflow.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.theme.RentFlowTheme
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Behind photos in the crop and preview screens, which stay dark like any photo editor. */
internal val PhotoBackdrop = Color(0xFF05070D)

/**
 * A full-screen dialog on the dark photo backdrop, in the dark theme, with
 * light status-bar icons.
 */
@Composable
internal fun DarkPhotoDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            if (window != null) {
                window.setDimAmount(0f)
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
        RentFlowTheme(dark = true) {
            Box(Modifier.fillMaxSize().background(PhotoBackdrop)) { content() }
        }
    }
}

/** Frame shapes offered for property and room photos; 0 keeps the photo's own shape. */
private val Ratios = listOf("Original" to 0f, "4:3" to 4f / 3f, "16:9" to 16f / 9f, "1:1" to 1f)

/**
 * Crop one photo. The frame stays put and the photo moves under it: drag to
 * move, pinch to zoom, rotate a quarter turn at a time. [round] crops a
 * profile photo in a round 1:1 frame. [counter] is shown under the title
 * (such as "Photo 2 of 4"); [onUseAll] adds a button that skips cropping.
 */
@Composable
fun CropScreen(
    source: Uri,
    round: Boolean,
    counter: String?,
    onUse: (Uri) -> Unit,
    onCancel: () -> Unit,
    onUseAll: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember(source) { mutableStateOf<Bitmap?>(null) }
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(source) {
        bitmap = loadForCrop(context, source)
        if (bitmap == null) {
            Toasts.error("This photo can’t be opened. Try another one.")
            onCancel()
        }
    }

    var ratioIndex by remember(source) { mutableIntStateOf(0) }
    var rotation by remember(source) { mutableIntStateOf(0) }
    val crop = remember(source) { CropState() }

    DarkPhotoDialog(onDismiss = onCancel) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DarkIconButton(Lucide.X, "Cancel", onClick = onCancel)
                Column(Modifier.weight(1f)) {
                    Text(if (round) "Crop profile photo" else "Crop photo", style = RfType.HeadlineSm, color = Rf.OnSurface)
                    if (counter != null) Text(counter, style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val image = bitmap
                if (image == null) {
                    Spinner(Rf.Primary)
                } else {
                    val ratio = if (round) 1f else Ratios[ratioIndex].second
                    CropStage(image, ratio, rotation, round, crop)
                    Text(
                        "Drag to move · pinch to zoom",
                        style = RfType.LabelSm,
                        color = Rf.OnSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .clip(CircleShape)
                            .background(Rf.Surface.copy(alpha = 0.8f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Rf.Surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!round) {
                        Ratios.forEachIndexed { index, (label, _) ->
                            RatioChip(label, selected = index == ratioIndex, modifier = Modifier.weight(1f)) { ratioIndex = index }
                        }
                    } else {
                        Box(Modifier.weight(1f))
                    }
                    DarkIconButton(Lucide.RotateCw, "Rotate a quarter turn", size = 36) { rotation = (rotation + 90) % 360 }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onUseAll != null) {
                        TonalButton("Use all as they are", onClick = onUseAll, modifier = Modifier.weight(1f), background = Rf.Low)
                    }
                    PrimaryButton(
                        "Use photo",
                        onClick = {
                            val image = bitmap ?: return@PrimaryButton
                            val frame = crop.frame ?: return@PrimaryButton
                            saving = true
                            scope.launch {
                                val out = withContext(Dispatchers.Default) { renderCrop(image, rotation, crop, frame) }
                                val uri = PhotoFiles.save(context, out)
                                saving = false
                                if (uri == null) Toasts.error("The photo couldn’t be saved. Try again.") else onUse(uri)
                            }
                        },
                        loading = saving,
                        enabled = bitmap != null,
                        showArrow = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Where the photo sits under the frame, in pixels of the crop area. */
private class CropState {
    var scale by mutableFloatStateOf(1f)
    var x by mutableFloatStateOf(0f)
    var y by mutableFloatStateOf(0f)
    var frame: Rect? by mutableStateOf(null)
}

@Composable
private fun CropStage(image: Bitmap, ratio: Float, rotation: Int, round: Boolean, crop: CropState) {
    val picture = remember(image) { image.asImageBitmap() }
    var area by remember { mutableStateOf(IntSize.Zero) }
    val turned = rotation % 180 != 0
    // The photo's width and height once rotated
    val iw = (if (turned) image.height else image.width).toFloat()
    val ih = (if (turned) image.width else image.height).toFloat()

    fun minScale(frame: Rect) = max(frame.width / iw, frame.height / ih)
    fun clamp(frame: Rect) {
        crop.scale = crop.scale.coerceIn(minScale(frame), minScale(frame) * 5f)
        crop.x = crop.x.coerceIn(frame.right - iw * crop.scale, frame.left)
        crop.y = crop.y.coerceIn(frame.bottom - ih * crop.scale, frame.top)
    }

    // A new shape, turn or screen size lays the frame out again and fits the photo to it
    LaunchedEffect(image, area, ratio, rotation) {
        if (area == IntSize.Zero) return@LaunchedEffect
        val shape = if (ratio > 0f) ratio else iw / ih
        var fw = area.width * 0.88f
        var fh = fw / shape
        if (fh > area.height * 0.78f) {
            fh = area.height * 0.78f
            fw = fh * shape
        }
        val frame = Rect(Offset((area.width - fw) / 2f, (area.height - fh) / 2f), Size(fw, fh))
        crop.frame = frame
        crop.scale = minScale(frame)
        crop.x = frame.left + (frame.width - iw * crop.scale) / 2f
        crop.y = frame.top + (frame.height - ih * crop.scale) / 2f
    }

    val scrim = PhotoBackdrop.copy(alpha = 0.62f)
    Canvas(
        Modifier
            .fillMaxSize()
            .onSizeChanged { area = it }
            .pointerInput(image, rotation) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val frame = crop.frame ?: return@detectTransformGestures
                    val before = crop.scale
                    crop.scale = (crop.scale * zoom).coerceIn(minScale(frame), minScale(frame) * 5f)
                    val k = crop.scale / before
                    crop.x = centroid.x - (centroid.x - crop.x) * k + pan.x
                    crop.y = centroid.y - (centroid.y - crop.y) * k + pan.y
                    clamp(frame)
                }
            }
    ) {
        val frame = crop.frame ?: return@Canvas
        val s = crop.scale
        withTransform({
            translate(crop.x + iw * s / 2f, crop.y + ih * s / 2f)
            rotate(rotation.toFloat(), pivot = Offset.Zero)
            scale(s, s, pivot = Offset.Zero)
        }) {
            drawImage(
                picture,
                dstOffset = IntOffset(-image.width / 2, -image.height / 2),
                dstSize = IntSize(image.width, image.height),
                filterQuality = FilterQuality.Medium
            )
        }

        val line = 1.5.dp.toPx()
        if (round) {
            val hole = Path().apply { addOval(frame) }
            clipPath(hole, ClipOp.Difference) { drawRect(scrim) }
            drawOval(Color.White.copy(alpha = 0.9f), frame.topLeft, frame.size, style = Stroke(line))
        } else {
            clipRect(frame.left, frame.top, frame.right, frame.bottom, ClipOp.Difference) { drawRect(scrim) }
            drawRect(Color.White.copy(alpha = 0.9f), frame.topLeft, frame.size, style = Stroke(line))
            // Rule of thirds
            val grid = Color.White.copy(alpha = 0.28f)
            for (i in 1..2) {
                val gx = frame.left + frame.width * i / 3f
                val gy = frame.top + frame.height * i / 3f
                drawLine(grid, Offset(gx, frame.top), Offset(gx, frame.bottom), strokeWidth = 1.dp.toPx())
                drawLine(grid, Offset(frame.left, gy), Offset(frame.right, gy), strokeWidth = 1.dp.toPx())
            }
            // Corner marks
            val arm = 18.dp.toPx()
            val thick = 3.dp.toPx()
            val o = thick / 2f
            listOf(
                Triple(frame.topLeft, 1f, 1f),
                Triple(frame.topRight, -1f, 1f),
                Triple(frame.bottomLeft, 1f, -1f),
                Triple(frame.bottomRight, -1f, -1f)
            ).forEach { (corner, dx, dy) ->
                val c = Offset(corner.x - dx * o, corner.y - dy * o)
                drawLine(Color.White, c, Offset(c.x + dx * arm, c.y), strokeWidth = thick)
                drawLine(Color.White, c, Offset(c.x, c.y + dy * arm), strokeWidth = thick)
            }
        }
    }
}

@Composable
private fun RatioChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(34.dp)
            .pressable(onClick, pressScale = 0.95f)
            .clip(CircleShape)
            .background(if (selected) Rf.PrimaryFixed else Rf.Low)
            .then(if (selected) Modifier.border(1.5.dp, Rf.Primary, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = RfType.LabelMd, color = if (selected) Rf.Primary else Rf.OnSurfaceVariant)
    }
}

/** A square icon button on the dark photo screens. */
@Composable
internal fun DarkIconButton(icon: LucideIcon, description: String, size: Int = 36, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size.dp)
            .pressable(onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(Rf.Low),
        contentAlignment = Alignment.Center
    ) {
        LIcon(icon, size = 18.dp, tint = Rf.OnSurface, contentDescription = description)
    }
}

/** The photo at up to 2048 px on its long side, turned upright, or null if it can't be read. */
private suspend fun loadForCrop(context: Context, source: Uri): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, source)) { decoder, info, _ ->
            val long = max(info.size.width, info.size.height)
            if (long > MAX_CROP_SOURCE) {
                val k = MAX_CROP_SOURCE.toFloat() / long
                decoder.setTargetSize((info.size.width * k).roundToInt(), (info.size.height * k).roundToInt())
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }.getOrNull()
}

private const val MAX_CROP_SOURCE = 2048

/** Draws what is inside the frame at the photo's own resolution (at most 2048 px on the long side). */
private fun renderCrop(image: Bitmap, rotation: Int, crop: CropState, frame: Rect): Bitmap {
    val turned = rotation % 180 != 0
    val iw = (if (turned) image.height else image.width).toFloat()
    val ih = (if (turned) image.width else image.height).toFloat()
    val s = crop.scale
    // Photo pixels per screen pixel, capped so the result stays a sensible size
    val sourceWidth = frame.width / s
    val sourceHeight = frame.height / s
    val fit = min(1f, MAX_CROP_SOURCE / max(sourceWidth, sourceHeight))
    val out = fit / s
    val result = Bitmap.createBitmap(
        max(1, (frame.width * out).roundToInt()),
        max(1, (frame.height * out).roundToInt()),
        Bitmap.Config.ARGB_8888
    )
    android.graphics.Canvas(result).apply {
        drawColor(android.graphics.Color.WHITE)
        scale(out, out)
        translate(-frame.left, -frame.top)
        translate(crop.x + iw * s / 2f, crop.y + ih * s / 2f)
        rotate(rotation.toFloat())
        scale(s, s)
        drawBitmap(image, -image.width / 2f, -image.height / 2f, Paint(Paint.FILTER_BITMAP_FLAG))
    }
    return result
}
