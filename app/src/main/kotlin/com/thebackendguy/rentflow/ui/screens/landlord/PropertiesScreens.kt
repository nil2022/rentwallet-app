package com.thebackendguy.rentflow.ui.screens.landlord

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.Portfolio
import com.thebackendguy.rentflow.data.floorLabel
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.occupancyLabel
import com.thebackendguy.rentflow.data.propertyTypeLabel
import com.thebackendguy.rentflow.data.remote.PropertyDto
import com.thebackendguy.rentflow.data.remote.RoomDto
import com.thebackendguy.rentflow.ui.components.ActionButton
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.Caption
import com.thebackendguy.rentflow.ui.components.ConfirmSheet
import com.thebackendguy.rentflow.ui.components.EmptyState
import com.thebackendguy.rentflow.ui.components.Fab
import com.thebackendguy.rentflow.ui.components.GrowBar
import com.thebackendguy.rentflow.ui.components.Pill
import com.thebackendguy.rentflow.ui.components.RemoteImage
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.SectionHead
import com.thebackendguy.rentflow.ui.components.TitledCard
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.launch

/* ------------------------------ Properties list ------------------------------ */

@Composable
fun PropertiesScreen(shell: Shell, onOpenProperty: (String) -> Unit, onAddProperty: () -> Unit) {
    val state = portfolioState()
    LaunchedEffect(Unit) { LandlordStore.refresh() }
    var query by rememberSaveable { mutableStateOf("") }
    val hasProperties = !state.data?.properties.isNullOrEmpty()

    Box(Modifier.fillMaxSize()) {
        AppPage(topBar = { shell.TopBar("Properties") }, bottomBar = { shell.BottomNav(1) }) {
            PortfolioContent(state, loadingImageHeight = 104.dp) { portfolio ->
                if (portfolio.properties.isEmpty()) {
                    EmptyState(
                        icon = Lucide.Building2,
                        title = "No properties yet",
                        message = "Add your first property. Then add its rooms and tenants.",
                        actionLabel = "Add property",
                        onAction = onAddProperty
                    )
                } else {
                    val rooms = portfolio.allRooms
                    val let = rooms.count { portfolio.occupants(it.id).isNotEmpty() }
                    CountTiles(
                        listOf(
                            Triple("${rooms.size}", "Rooms", Rf.OnSurface),
                            Triple("$let", "Let", Rf.Secondary),
                            Triple("${rooms.size - let}", "Vacant", Rf.Primary)
                        )
                    )
                    SearchBox(query, "Search by name or city") { query = it }
                    val shown = portfolio.properties.filter {
                        query.isBlank() || listOf(it.propertyName, it.city, it.address, it.state)
                            .any { field -> field.contains(query.trim(), ignoreCase = true) }
                    }
                    Column {
                        SectionHead("Your properties") { Caption("${portfolio.properties.size} total") }
                        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (shown.isEmpty()) {
                                Text("No properties match “$query”.", style = RfType.BodyMd, color = Rf.OnSurfaceVariant, modifier = Modifier.padding(4.dp))
                            }
                            shown.forEach { property -> PropertyCard(property, portfolio) { onOpenProperty(property.id) } }
                        }
                    }
                    // Room for the floating button over the last card
                    Box(Modifier.height(48.dp))
                }
            }
        }
        if (hasProperties) Fab("Add property", Lucide.Plus, onAddProperty, Modifier.align(Alignment.BottomEnd))
    }
}

/** A property with its cover photo, type, vacancy and how many rooms are let; also on the overview. */
@Composable
internal fun PropertyCard(property: PropertyDto, portfolio: Portfolio, onClick: () -> Unit) {
    val rooms = portfolio.roomsOf(property.id)
    val let = rooms.count { portfolio.occupants(it.id).isNotEmpty() }
    val full = rooms.isNotEmpty() && let == rooms.size
    RfCard(padding = PaddingValues(0.dp), onClick = onClick) {
        Box(Modifier.fillMaxWidth().height(104.dp)) {
            RemoteImage(property.images.firstOrNull()?.url, Modifier.fillMaxSize())
            Pill(
                propertyTypeLabel(property.propertyType), background = Rf.Lowest.copy(alpha = 0.92f), content = Rf.Primary,
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
            )
            Pill(
                text = when {
                    rooms.isEmpty() -> "No rooms yet"
                    full -> "Fully let"
                    else -> "${rooms.size - let} vacant"
                },
                background = Rf.InverseSurface.copy(alpha = 0.8f), content = Rf.InverseOnSurface,
                dot = true, dotColor = if (full) Rf.Mint else Rf.AmberDot,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
            )
        }
        Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(property.propertyName, style = RfType.BodyLg.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LIcon(Lucide.MapPin, size = 12.dp, tint = Rf.OnSurfaceVariant)
                        Text(listOf(property.city, property.state).filter(String::isNotBlank).joinToString(", "),
                            style = RfType.LabelSm, color = Rf.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                LIcon(Lucide.ChevronRight, tint = Rf.Outline)
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$let of ${rooms.size} rooms let", style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
                Text("${property.floorCount} ${if (property.floorCount == 1) "floor" else "floors"}", style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
            }
            GrowBar(if (rooms.isEmpty()) 0f else let / rooms.size.toFloat(), color = if (full) Rf.Secondary else Rf.Primary, track = Rf.Low, height = 6.dp)
        }
    }
}

/* ------------------------------ Property details ------------------------------ */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PropertyDetailsScreen(
    propertyId: String,
    shell: Shell,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddRoom: () -> Unit,
    onOpenRoom: (String) -> Unit,
    onDeleted: () -> Unit
) {
    val state = portfolioState()
    val property = state.data?.property(propertyId)
    var confirmDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AppPage(topBar = { shell.TopBar(property?.propertyName ?: "Property", onBack = onBack) }, bottomBar = { shell.BottomNav(1) }) {
        PortfolioContent(state, loadingImageHeight = 156.dp) { portfolio ->
            if (property == null) {
                NotFound("Property", onBack)
                return@PortfolioContent
            }
            val rooms = portfolio.roomsOf(property.id)

            RfCard(padding = PaddingValues(0.dp)) {
                PhotoPager(property)
                Column(Modifier.padding(14.dp)) {
                    Text(property.propertyName, style = RfType.HeadlineSm, color = Rf.OnSurface)
                    Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LIcon(Lucide.MapPin, size = 12.dp, tint = Rf.OnSurfaceVariant, modifier = Modifier.padding(top = 1.dp))
                        Text(
                            listOf(property.address, property.city, "${property.state} ${property.pincode}".trim())
                                .filter(String::isNotBlank).joinToString(", "),
                            style = RfType.LabelSm, color = Rf.OnSurfaceVariant
                        )
                    }
                    FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FactChip(Lucide.Layers, "${property.floorCount} ${if (property.floorCount == 1) "floor" else "floors"}")
                        FactChip(Lucide.DoorOpen, "${rooms.size} ${if (rooms.size == 1) "room" else "rooms"}")
                        if (property.hasParking) FactChip(Lucide.Car, "Parking")
                        if (property.hasLift) FactChip(Lucide.ArrowUpDown, "Lift")
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("Edit property", Lucide.Pencil, Rf.Container, Rf.Primary, Modifier.weight(1f), onClick = onEdit)
                ActionButton("Delete", Lucide.Trash2, Rf.ErrorContainer, Rf.Error, Modifier.weight(1f)) { confirmDelete = true }
            }

            Column {
                SectionHead("Rooms") {
                    Row(Modifier.pressable(onAddRoom), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LIcon(Lucide.Plus, size = 16.dp, tint = Rf.Primary)
                        Text("Add room", style = RfType.LabelMd, color = Rf.Primary)
                    }
                }
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (rooms.isEmpty()) {
                        RfCard {
                            Text("No rooms yet. Add the rooms you rent out, then assign tenants to them.", style = RfType.BodyMd, color = Rf.OnSurfaceVariant)
                        }
                    }
                    rooms.forEach { room -> RoomRow(room, portfolio.occupants(room.id).map { it.fullName }) { onOpenRoom(room.id) } }
                }
            }

            if (!property.description.isNullOrBlank()) {
                TitledCard("About") {
                    Text(property.description, style = RfType.BodyMd, color = Rf.OnSurfaceVariant)
                }
            }

            if (confirmDelete) {
                val occupied = rooms.any { portfolio.occupants(it.id).isNotEmpty() }
                ConfirmSheet(
                    icon = Lucide.Trash2,
                    title = "Delete ${property.propertyName}?",
                    message = if (occupied) "Some of its rooms still have tenants. End their leases first, then delete the property."
                    else "The property is removed permanently. This can’t be undone.",
                    confirmLabel = if (occupied) "OK" else "Delete property",
                    busy = deleting,
                    onConfirm = {
                        if (occupied) {
                            confirmDelete = false
                        } else {
                            deleting = true
                            scope.launch {
                                val done = LandlordStore.deleteProperty(property.id).announce("Property deleted")
                                deleting = false
                                confirmDelete = false
                                if (done) onDeleted()
                            }
                        }
                    },
                    onDismiss = { confirmDelete = false }
                )
            }
        }
    }
}

/** Swipeable photos with dots and a counter; a placeholder when there are none. */
@Composable
private fun PhotoPager(property: PropertyDto) {
    val images = property.images
    val pager = rememberPagerState { images.size.coerceAtLeast(1) }
    Box(Modifier.fillMaxWidth().height(156.dp)) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            RemoteImage(images.getOrNull(page)?.url, Modifier.fillMaxSize())
        }
        Pill(
            propertyTypeLabel(property.propertyType), background = Rf.Lowest.copy(alpha = 0.92f), content = Rf.Primary,
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
        )
        if (images.size > 1) {
            Text(
                "${pager.currentPage + 1} / ${images.size}",
                style = RfType.LabelSm, color = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(CircleShape)
                    .background(Rf.InverseSurface.copy(alpha = 0.8f)).padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(images.size) { i ->
                    val active = i == pager.currentPage
                    Box(Modifier.size(width = if (active) 16.dp else 6.dp, height = 6.dp).clip(CircleShape)
                        .background(if (active) Color.White else Color.White.copy(alpha = 0.6f)))
                }
            }
        }
    }
}

@Composable
internal fun RoomRow(room: RoomDto, occupants: List<String>, onClick: () -> Unit) {
    val let = occupants.isNotEmpty()
    val bg = if (let) Rf.MintSoft else Rf.PrimaryFixed
    val fg = if (let) Rf.Secondary else Rf.Primary
    RfCard(padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
                Text(room.roomNumber, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = fg, maxLines = 1)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (let) occupants.joinToString(", ") else "Vacant",
                    style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${floorLabel(room.floor)} • ${occupancyLabel(room.occupancyType)} • ${inr(room.rent)}",
                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Pill(if (let) "Let" else "Vacant", background = bg, content = fg)
        }
    }
}
