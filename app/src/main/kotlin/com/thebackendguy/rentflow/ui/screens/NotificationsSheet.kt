package com.thebackendguy.rentflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.thebackendguy.rentflow.data.NotificationFeed
import com.thebackendguy.rentflow.data.NotificationStore
import com.thebackendguy.rentflow.data.UserRole
import com.thebackendguy.rentflow.data.remote.NotificationDto
import com.thebackendguy.rentflow.ui.components.Dot
import com.thebackendguy.rentflow.ui.components.EmptyState
import com.thebackendguy.rentflow.ui.components.ErrorState
import com.thebackendguy.rentflow.ui.components.IconTile
import com.thebackendguy.rentflow.ui.components.Pill
import com.thebackendguy.rentflow.ui.components.Skeleton
import com.thebackendguy.rentflow.ui.components.Spinner
import com.thebackendguy.rentflow.ui.components.TonalButton
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.components.softShadow
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.theme.LocalRfPalette
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The bell's notifications in a bottom sheet. Opening it marks them read on the server. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(role: UserRole, onDismiss: () -> Unit) {
    val feed by NotificationStore.feed.collectAsState()
    LaunchedEffect(Unit) { NotificationStore.open() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Rf.Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Notifications", style = RfType.HeadlineSm, color = Rf.OnSurface)
                if (feed.newCount > 0) {
                    Pill(
                        "${feed.newCount} new",
                        background = Rf.PrimaryFixed,
                        content = Rf.Primary,
                        style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.em),
                        horizontal = 10.dp,
                        vertical = 4.dp
                    )
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(36.dp).pressable(onDismiss).clip(RoundedCornerShape(12.dp)).background(Rf.Low),
                    contentAlignment = Alignment.Center
                ) { LIcon(Lucide.X, size = 18.dp, tint = Rf.OnSurface, contentDescription = "Close") }
            }
            when {
                feed.loading -> LoadingRows()
                feed.error != null -> ErrorState(feed.error!!, onRetry = NotificationStore::open)
                feed.loaded && feed.items.isEmpty() -> EmptyState(
                    icon = Lucide.Bell,
                    title = "You’re all caught up",
                    message = if (role == UserRole.Tenant) "Rent reminders, receipts and messages from your landlord will show here."
                    else "Rent updates, payments and messages from your tenants will show here."
                )
                feed.loaded -> FeedList(feed)
            }
        }
    }
}

@Composable
private fun FeedList(feed: NotificationFeed) {
    val groups = remember(feed.items) { group(feed.items) }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        groups.forEach { (label, items) ->
            item(key = "h-$label") { SectionLabel(label) }
            items(items, key = { it.id }) { NotificationRow(it) }
        }
        item(key = "footer") { Footer(feed) }
    }
}

@Composable
private fun Footer(feed: NotificationFeed) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            feed.hasMore -> {
                Text("Showing ${feed.items.size} of ${feed.total}", style = RfType.BodySm.copy(fontSize = RfType.LabelSm.fontSize), color = Rf.OnSurfaceVariant)
                if (feed.moreError != null) Text(feed.moreError, style = RfType.BodySm, color = Rf.Error, textAlign = TextAlign.Center)
                if (feed.loadingMore) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Rf.Low).padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spinner(Rf.Primary, size = 16.dp)
                        Text("Loading…", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.Primary)
                    }
                } else {
                    TonalButton(
                        if (feed.moreError != null) "Try again" else "Load more",
                        onClick = NotificationStore::loadMore,
                        background = Rf.Low,
                        icon = if (feed.moreError != null) Lucide.RefreshCw else Lucide.ChevronDown
                    )
                }
            }
            // Only worth saying after paging through more than one page
            feed.total > NotificationStore.PAGE_SIZE -> Row(
                Modifier.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LIcon(Lucide.CheckCheck, size = 16.dp, tint = Rf.Secondary)
                Text("That’s all ${feed.total} notifications", style = RfType.LabelMd, color = Rf.OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(Locale.ENGLISH),
        style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
        color = Rf.OnSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

@Composable
private fun NotificationRow(item: NotificationDto) {
    val shape = RoundedCornerShape(16.dp)
    val unread = !item.isRead
    val (icon, tile, tint) = typeStyle(item.type)
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (unread) Modifier else Modifier.softShadow(shape))
            .clip(shape)
            .background(if (unread) Rf.PrimaryFixed.copy(alpha = if (LocalRfPalette.current.isDark) 0.7f else 0.55f) else Rf.Lowest)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(icon, background = tile, tint = tint, size = 40.dp, radius = 12.dp, iconSize = 20.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    item.title,
                    style = RfType.BodyMd.copy(fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold),
                    color = Rf.OnSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(timeLabel(item.createdAt), style = RfType.LabelSm.copy(letterSpacing = 0.em), color = if (unread) Rf.Primary else Rf.OnSurfaceVariant, maxLines = 1)
                if (unread) Dot(Rf.Primary, size = 8.dp)
            }
            Text(item.message, style = RfType.BodySm, color = Rf.OnSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingRows() {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(0.58f, 0.44f, 0.66f, 0.5f, 0.62f, 0.4f).forEach { width ->
            Row(
                Modifier.fillMaxWidth().softShadow(RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(Rf.Lowest)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Skeleton(Modifier.size(40.dp), radius = 12.dp)
                Column(Modifier.weight(1f).padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        Skeleton(Modifier.fillMaxWidth(width).height(14.dp), radius = 7.dp)
                        Spacer(Modifier.weight(1f))
                        Skeleton(Modifier.width(44.dp).height(10.dp), radius = 5.dp, color = Rf.Low)
                    }
                    Skeleton(Modifier.fillMaxWidth(0.92f).height(10.dp), radius = 5.dp, color = Rf.Low)
                    Skeleton(Modifier.fillMaxWidth(0.7f).height(10.dp), radius = 5.dp, color = Rf.Low)
                }
            }
        }
    }
}

/** Icon, tile colour and icon colour for each notification type. */
@Composable
private fun typeStyle(type: String): Triple<LucideIcon, Color, Color> = when (type) {
    "payment_reminder" -> Triple(Lucide.Clock, Rf.AmberSoft, Rf.Amber)
    "payment_confirmation" -> Triple(Lucide.CircleCheck, Rf.MintSoft, Rf.Secondary)
    "property_update" -> Triple(Lucide.House, Rf.PrimaryFixed, Rf.Primary)
    "booking" -> Triple(Lucide.CalendarCheck, Rf.High, Rf.Primary)
    "message" -> Triple(Lucide.MessageSquare, Rf.Container, Rf.Primary)
    else -> Triple(Lucide.Info, Rf.Low, Rf.OnSurfaceVariant)
}

/* ------------------------------ Dates ------------------------------ */

private val Clock12 = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val DayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val DayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

private fun zoned(createdAt: String?): ZonedDateTime? =
    createdAt?.let { runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()) }.getOrNull() }

/** Today, Yesterday and Earlier, newest first as the server sends them. */
private fun group(items: List<NotificationDto>): List<Pair<String, List<NotificationDto>>> {
    val today = LocalDate.now()
    return items.groupBy {
        when (zoned(it.createdAt)?.toLocalDate()) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> "Earlier"
        }
    }.toList()
}

/** "Just now", "12 min ago" and "2 h ago" today; the time yesterday; the date before that. */
private fun timeLabel(createdAt: String?): String {
    val time = zoned(createdAt) ?: return ""
    val now = ZonedDateTime.now()
    val date = time.toLocalDate()
    return when {
        date == now.toLocalDate() -> {
            val minutes = Duration.between(time, now).toMinutes().coerceAtLeast(0)
            when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "$minutes min ago"
                else -> "${minutes / 60} h ago"
            }
        }
        date == now.toLocalDate().minusDays(1) -> time.format(Clock12)
        date.year == now.year -> time.format(DayMonth)
        else -> time.format(DayMonthYear)
    }
}
