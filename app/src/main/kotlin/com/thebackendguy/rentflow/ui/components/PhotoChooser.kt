package com.thebackendguy.rentflow.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.io.File

/**
 * Lets the user add photos from the camera or the gallery. Returns the function
 * that opens the choice; the sheet itself is shown from here. The gallery may
 * return up to [limit] photos at once, the camera always one.
 */
@Composable
fun photoChooser(limit: Int = 1, onPicked: (List<Uri>) -> Unit): () -> Unit {
    val context = LocalContext.current
    var choosing by remember { mutableStateOf(false) }

    // The file the camera writes to; kept across the camera app taking over the screen
    var shot by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = shot ?: return@rememberLauncherForActivityResult
        shot = null
        if (saved) onPicked(listOf(uri)) else CameraFiles.discard(context, uri)
    }

    val single = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(listOf(uri))
    }
    // The multi-photo picker needs a limit above one
    val multiple = if (limit > 1) {
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(limit)) { uris ->
            if (uris.isNotEmpty()) onPicked(uris)
        }
    } else null

    if (choosing) {
        PhotoSourceSheet(
            hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
            onCamera = {
                choosing = false
                val uri = CameraFiles.create(context)
                shot = uri
                try {
                    camera.launch(uri)
                } catch (_: ActivityNotFoundException) {
                    shot = null
                    CameraFiles.discard(context, uri)
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
    return { choosing = true }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSourceSheet(hasCamera: Boolean, onCamera: () -> Unit, onGallery: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Rf.Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Add a photo", style = RfType.HeadlineSm, color = Rf.OnSurface, modifier = Modifier.padding(bottom = 4.dp))
            if (hasCamera) SourceRow(Lucide.Camera, "Take photo", "Use the camera", onCamera)
            SourceRow(Lucide.Image, "Choose from gallery", "Pick from the photos on this phone", onGallery)
            TonalButton("Cancel", onClick = onDismiss, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun SourceRow(icon: LucideIcon, title: String, sub: String, onClick: () -> Unit) {
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
        IconTile(icon, background = Rf.PrimaryFixed, tint = Rf.Primary, size = 40.dp, radius = 12.dp, iconSize = 20.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
            Text(sub, style = RfType.BodySm, color = Rf.OnSurfaceVariant)
        }
        LIcon(Lucide.ChevronRight, size = 18.dp, tint = Rf.Outline)
    }
}

/** Files the camera saves into, shared with the camera app through the FileProvider in the manifest. */
private object CameraFiles {
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    fun create(context: Context): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        // Photos from earlier days are no longer in any open form
        dir.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - DAY_MS }?.forEach { it.delete() }
        val file = File(dir, "photo-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun discard(context: Context, uri: Uri) {
        runCatching { context.contentResolver.delete(uri, null, null) }
    }
}
