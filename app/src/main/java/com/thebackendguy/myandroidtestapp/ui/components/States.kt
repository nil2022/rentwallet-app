package com.thebackendguy.myandroidtestapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.thebackendguy.myandroidtestapp.ui.icons.LIcon
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.icons.LucideIcon
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RfType

/* ------------------------------ Loading ------------------------------ */

/** Grey block with a moving shine, where content will appear. */
@Composable
fun Skeleton(modifier: Modifier = Modifier, radius: Dp = 8.dp) {
    val shift by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "skeletonShift"
    )
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.linearGradient(
                    colors = listOf(Rf.Low, Color(0xFFF7F9FF), Rf.Low),
                    start = Offset(shift * 600f, 0f),
                    end = Offset(shift * 600f + 600f, 0f)
                )
            )
    )
}

/** Placeholder cards while a list loads for the first time. */
@Composable
fun LoadingCards(count: Int = 3, imageHeight: Dp = 0.dp) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            RfCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                if (imageHeight > 0.dp) Skeleton(Modifier.fillMaxWidth().height(imageHeight), radius = 0.dp)
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Skeleton(Modifier.fillMaxWidth(0.6f).height(16.dp))
                    Skeleton(Modifier.fillMaxWidth(0.4f).height(12.dp))
                    Skeleton(Modifier.fillMaxWidth().height(8.dp))
                }
            }
        }
    }
}

/* ------------------------------ Empty and error ------------------------------ */

@Composable
fun EmptyState(
    icon: LucideIcon,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionIcon: LucideIcon = Lucide.Plus,
    tint: Color = Rf.Primary,
    tile: Color = Rf.PrimaryFixed,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconTile(icon, background = tile, tint = tint, size = 72.dp, radius = 22.dp, iconSize = 36.dp, modifier = Modifier.padding(bottom = 8.dp))
        Text(title, style = RfType.HeadlineMd, color = Rf.OnSurface, textAlign = TextAlign.Center)
        Text(message, style = RfType.BodyMd, color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Row(
                Modifier
                    .padding(top = 16.dp)
                    .pressable(onAction, pressScale = 0.97f)
                    .softShadow(RoundedCornerShape(12.dp), elevation = 4.dp, color = Rf.Primary.copy(alpha = 0.45f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Rf.Primary)
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LIcon(actionIcon, size = 18.dp, tint = Color.White)
                Text(actionLabel, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
        }
    }
}

/** When nothing could load: the reason and a retry button. */
@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        icon = Lucide.WifiOff,
        title = "Couldn’t load this",
        message = message,
        modifier = modifier,
        actionLabel = "Try again",
        actionIcon = Lucide.RefreshCw,
        tint = Rf.Error,
        tile = Rf.ErrorContainer,
        onAction = onRetry
    )
}

/* ------------------------------ Floating button ------------------------------ */

/** "+ Add …" button floating above the bottom tabs. Put it in a Box over the page. */
@Composable
fun Fab(label: String, icon: LucideIcon, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .navigationBarsPadding()
            .padding(end = 16.dp, bottom = 80.dp)
            .pressable(onClick, pressScale = 0.95f)
            .softShadow(shape, elevation = 10.dp, color = Rf.Primary.copy(alpha = 0.5f))
            .clip(shape)
            .background(Rf.Primary)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LIcon(icon, size = 20.dp, tint = Color.White)
        Text(label, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}

/* ------------------------------ Confirm ------------------------------ */

/** Bottom sheet that asks before something is deleted or ended. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmSheet(
    icon: LucideIcon,
    title: String,
    message: String,
    confirmLabel: String,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Rf.Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            IconTile(icon, background = Rf.ErrorContainer, tint = Rf.Error, size = 48.dp, radius = 14.dp, iconSize = 24.dp)
            Text(title, style = RfType.HeadlineSm, color = Rf.OnSurface, modifier = Modifier.padding(top = 14.dp))
            Text(message, style = RfType.BodyMd, color = Rf.OnSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            DangerButton(confirmLabel, busy, onConfirm, Modifier.padding(top = 20.dp))
            TonalButton("Cancel", onClick = { if (!busy) onDismiss() }, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun DangerButton(text: String, busy: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .pressable(onClick, pressScale = 0.98f, enabled = !busy)
            .softShadow(shape, elevation = 4.dp, color = Rf.Error.copy(alpha = 0.4f))
            .clip(shape)
            .background(Rf.Error)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (busy) Spinner(Color.White)
        Text(text, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}

/* ------------------------------ Pictures ------------------------------ */

/**
 * A photo from a URL or a picked file. While it loads, or if there is none,
 * the soft indigo placeholder with [placeholderIcon] shows instead.
 */
@Composable
fun RemoteImage(model: Any?, modifier: Modifier = Modifier, placeholderIcon: LucideIcon = Lucide.Building2, iconSize: Dp = 36.dp) {
    val placeholder = @Composable {
        Box(Modifier.fillMaxSize().background(PhotoPlaceholder), contentAlignment = Alignment.Center) {
            LIcon(placeholderIcon, size = iconSize, tint = Rf.Primary.copy(alpha = 0.5f))
        }
    }
    if (model == null) {
        Box(modifier) { placeholder() }
        return
    }
    SubcomposeAsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = { placeholder() },
        error = { placeholder() }
    )
}

/** Round photo, or initials when there is none. */
@Composable
fun PersonAvatar(
    name: String,
    photoUrl: String?,
    size: Dp,
    background: Color = Rf.PrimaryFixed,
    content: Color = Rf.Primary
) {
    val initials = name.trim().split(Regex("\\s+")).filter(String::isNotEmpty).take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
    if (photoUrl.isNullOrBlank()) {
        Avatar(initials, background = background, content = content, size = size,
            style = if (size >= 56.dp) RfType.HeadlineMd else RfType.LabelMd.copy(fontWeight = FontWeight.Bold))
    } else {
        Box(Modifier.size(size).clip(androidx.compose.foundation.shape.CircleShape)) {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { Avatar(initials, background = background, content = content, size = size) },
                error = { Avatar(initials, background = background, content = content, size = size) }
            )
        }
    }
}
