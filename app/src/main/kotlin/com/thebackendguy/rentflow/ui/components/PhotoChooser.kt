package com.thebackendguy.rentflow.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private val UriListSaver = listSaver<List<Uri>, String>(save = { list -> list.map(Uri::toString) }, restore = { it.map(Uri::parse) })

/**
 * Lets the user add photos from the camera or the gallery, then crop each one.
 * Returns the function that opens the choice; the sheet and the crop screen are
 * shown from here. The gallery may return up to [limit] photos at once, the
 * camera always one. [round] crops in a round 1:1 frame for a profile photo.
 * [onView] and [onRemove] add "View photo" and "Remove photo" to the sheet.
 */
@Composable
fun photoChooser(
    limit: Int = 1,
    round: Boolean = false,
    title: String = "Add a photo",
    onView: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onPicked: (List<Uri>) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var choosing by remember { mutableStateOf(false) }

    // Photos waiting to be cropped, and those already done; kept if Android closes the app meanwhile
    var toCrop by rememberSaveable(stateSaver = UriListSaver) { mutableStateOf(emptyList<Uri>()) }
    var cropped by rememberSaveable(stateSaver = UriListSaver) { mutableStateOf(emptyList<Uri>()) }
    var total by rememberSaveable { mutableIntStateOf(0) }

    fun startCropping(photos: List<Uri>) {
        toCrop = photos
        cropped = emptyList()
        total = photos.size
    }

    fun finishIfDone() {
        if (toCrop.isNotEmpty()) return
        if (cropped.isNotEmpty()) onPicked(cropped)
        cropped = emptyList()
        total = 0
    }

    // Gallery photos can only be read until the app is closed, so each one is copied in first
    fun keep(picked: List<Uri>) {
        scope.launch {
            val copies = picked.mapNotNull { PhotoFiles.copyIn(context, it) }
            if (copies.size < picked.size) Toasts.error("Some photos couldn’t be added. Try picking them again.")
            if (copies.isNotEmpty()) startCropping(copies)
        }
    }

    // The file the camera writes to; kept across the camera app taking over the screen
    var shot by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = shot ?: return@rememberLauncherForActivityResult
        shot = null
        if (saved) startCropping(listOf(uri)) else PhotoFiles.discard(context, uri)
    }

    val single = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) keep(listOf(uri))
    }
    // The multi-photo picker needs a limit above one
    val multiple = if (limit > 1) {
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(limit)) { uris ->
            if (uris.isNotEmpty()) keep(uris)
        }
    } else null

    if (choosing) {
        PhotoSourceSheet(
            title = title,
            hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
            several = limit > 1,
            onView = onView?.let { view -> { choosing = false; view() } },
            onRemove = onRemove?.let { remove -> { choosing = false; remove() } },
            onCamera = {
                choosing = false
                val uri = PhotoFiles.create(context)
                shot = uri
                try {
                    camera.launch(uri)
                } catch (_: ActivityNotFoundException) {
                    shot = null
                    PhotoFiles.discard(context, uri)
                    Toasts.error("No camera app found on this phone.")
                }
            },
            onGallery = {
                choosing = false
                (multiple ?: single).launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onDismiss = { choosing = false }
        )
    }

    val current = toCrop.firstOrNull()
    if (current != null) {
        CropScreen(
            source = current,
            round = round,
            counter = if (total > 1) "Photo ${total - toCrop.size + 1} of $total" else null,
            onUse = { result ->
                PhotoFiles.discard(context, current)
                cropped = cropped + result
                toCrop = toCrop.drop(1)
                finishIfDone()
            },
            onCancel = {
                PhotoFiles.discard(context, current)
                toCrop = toCrop.drop(1)
                finishIfDone()
            },
            onUseAll = if (toCrop.size > 1) ({
                cropped = cropped + toCrop
                toCrop = emptyList()
                finishIfDone()
            }) else null
        )
    }
    return { choosing = true }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSourceSheet(
    title: String,
    hasCamera: Boolean,
    several: Boolean,
    onView: (() -> Unit)?,
    onRemove: (() -> Unit)?,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Rf.Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = RfType.HeadlineSm, color = Rf.OnSurface, modifier = Modifier.padding(bottom = 4.dp))
            if (onView != null) SourceRow(Lucide.Eye, "View photo", "See it full screen", onView)
            if (hasCamera) SourceRow(Lucide.Camera, "Take photo", "Use the camera, then crop", onCamera)
            SourceRow(Lucide.Image, "Choose from gallery", if (several) "Pick photos, then crop each" else "Pick a photo, then crop", onGallery)
            if (onRemove != null) SourceRow(Lucide.Trash2, "Remove photo", "Show the initials instead", onRemove, danger = true)
            TonalButton("Cancel", onClick = onDismiss, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun SourceRow(icon: LucideIcon, title: String, sub: String, onClick: () -> Unit, danger: Boolean = false) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(onClick, pressScale = 0.98f)
            .softShadow(shape)
            .clip(shape)
            .background(Rf.Lowest)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(
            icon, background = if (danger) Rf.ErrorContainer else Rf.PrimaryFixed, tint = if (danger) Rf.Error else Rf.Primary,
            size = 40.dp, radius = 12.dp, iconSize = 20.dp
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = if (danger) Rf.Error else Rf.OnSurface)
            Text(sub, style = RfType.BodySm, color = Rf.OnSurfaceVariant)
        }
        LIcon(Lucide.ChevronRight, size = 18.dp, tint = Rf.Outline)
    }
}

/**
 * New photos, kept in the app's own folder until they are uploaded: camera
 * shots are saved straight into it (through the FileProvider in the manifest)
 * and gallery photos are copied in.
 */
internal object PhotoFiles {
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    private fun newFile(context: Context): File {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        // Photos from earlier days are no longer in any open form
        dir.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - DAY_MS }?.forEach { it.delete() }
        return File(dir, "photo-${UUID.randomUUID()}.jpg")
    }

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** An empty file for the camera to save into. */
    fun create(context: Context): Uri = uriFor(context, newFile(context))

    /** A copy of [source] in the folder, or null if it can't be read. */
    suspend fun copyIn(context: Context, source: Uri): Uri? = withContext(Dispatchers.IO) {
        val file = newFile(context)
        runCatching {
            context.contentResolver.openInputStream(source).use { input ->
                checkNotNull(input) { "Photo can't be opened" }
                file.outputStream().use { input.copyTo(it) }
            }
            uriFor(context, file)
        }.onFailure { file.delete() }.getOrNull()
    }

    /** Saves a cropped photo as a JPEG in the folder, or null if it can't be written. */
    suspend fun save(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        val file = newFile(context)
        runCatching {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            uriFor(context, file)
        }.onFailure { file.delete() }.getOrNull()
    }

    /** Deletes a photo from the folder; photos from anywhere else are left alone. */
    fun discard(context: Context, uri: Uri) {
        if (uri.authority != "${context.packageName}.fileprovider") return
        runCatching { context.contentResolver.delete(uri, null, null) }
    }
}
