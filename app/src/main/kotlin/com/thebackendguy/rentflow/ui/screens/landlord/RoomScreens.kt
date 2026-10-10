package com.thebackendguy.rentflow.ui.screens.landlord

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebackendguy.rentflow.data.FloorOptions
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.OccupancyTypes
import com.thebackendguy.rentflow.data.Photo
import com.thebackendguy.rentflow.data.Portfolio
import com.thebackendguy.rentflow.data.floorLabel
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.mobileDigits
import com.thebackendguy.rentflow.data.mobileLabel
import com.thebackendguy.rentflow.data.occupancyLabel
import com.thebackendguy.rentflow.data.plainAmount
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.PropertyDto
import com.thebackendguy.rentflow.data.remote.RoomBody
import com.thebackendguy.rentflow.data.remote.RoomDto
import com.thebackendguy.rentflow.data.remote.TenantDto
import com.thebackendguy.rentflow.data.roomCapacity
import com.thebackendguy.rentflow.data.roomFull
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.ChoiceChips
import com.thebackendguy.rentflow.ui.components.ConfirmSheet
import com.thebackendguy.rentflow.ui.components.FormBar
import com.thebackendguy.rentflow.ui.components.FormSection
import com.thebackendguy.rentflow.ui.components.PhotoGrid
import com.thebackendguy.rentflow.ui.components.PhotoListSaver
import com.thebackendguy.rentflow.ui.components.PhotoViewer
import com.thebackendguy.rentflow.ui.components.RemoteImage
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.RfTextField
import com.thebackendguy.rentflow.ui.components.RowDivider
import com.thebackendguy.rentflow.ui.components.SelectField
import com.thebackendguy.rentflow.ui.components.SwitchRow
import com.thebackendguy.rentflow.ui.components.TextArea
import com.thebackendguy.rentflow.ui.components.Toasts
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.launch

/* ------------------------------ Room details ------------------------------ */

/** The web's phone room page (RoomDetailView): header, gallery, occupants, details and the property. */
@Composable
fun RoomDetailsScreen(
    roomId: String,
    tab: Int,
    shell: Shell,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenTenant: (String) -> Unit,
    onOpenProperty: (String) -> Unit,
    onAssign: () -> Unit,
    onDeleted: () -> Unit
) {
    val state = portfolioState()
    val room = state.data?.room(roomId)
    var confirmDelete by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<TenantDto?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AppPage(topBar = { shell.TopBar(room?.let { "Room ${it.roomNumber}" } ?: "Room", onBack = onBack) }, bottomBar = { shell.BottomNav(tab) }) {
        PortfolioContent(state, loadingImageHeight = 180.dp) { portfolio ->
            if (room == null) {
                NotFound("Room", onBack)
                return@PortfolioContent
            }
            val property = portfolio.property(room.propertyId)
            val occupants = portfolio.occupants(room.id)
            val let = occupants.isNotEmpty()

            RoomHeaderCard(room, property?.propertyName, occupants.size, onEdit = onEdit, onDelete = { confirmDelete = true })
            RoomGallery(room)
            OccupantsCard(room, occupants, onOpenTenant = onOpenTenant, onRemove = { removing = it }, onAssign = onAssign)
            RoomFactsCard(room)
            if (property != null) PropertyLinkCard(property) { onOpenProperty(property.id) }

            removing?.let { tenant ->
                ConfirmSheet(
                    icon = Lucide.LogOut,
                    title = "Remove ${tenant.fullName}?",
                    message = "Remove ${tenant.fullName} from Room ${room.roomNumber}? The room will have space again. Their account and bills are kept.",
                    confirmLabel = "Remove from room",
                    busy = busy,
                    onConfirm = {
                        busy = true
                        scope.launch {
                            LandlordStore.assignRoom(tenant.id, null).announce("${tenant.fullName} removed from Room ${room.roomNumber}")
                            busy = false
                            removing = null
                        }
                    },
                    onDismiss = { removing = null }
                )
            }

            if (confirmDelete) {
                // The API deletes a room even with tenants in it, leaving them in a room that no
                // longer exists, so the app asks for the room to be emptied first
                ConfirmSheet(
                    icon = Lucide.Trash2,
                    title = "Delete room ${room.roomNumber}?",
                    message = if (let) "This room still has tenants. Remove them from the room first, then delete it."
                    else "The room is removed permanently. This can’t be undone.",
                    confirmLabel = if (let) "OK" else "Delete room",
                    busy = busy,
                    onConfirm = {
                        if (let) {
                            confirmDelete = false
                        } else {
                            busy = true
                            scope.launch {
                                val done = LandlordStore.deleteRoom(room.id).announce("Room deleted")
                                busy = false
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

/** Room number, where it is, how full, what it has, rent and deposit, then Edit and Delete. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoomHeaderCard(room: RoomDto, propertyName: String?, tenants: Int, onEdit: () -> Unit, onDelete: () -> Unit) {
    // The three built-in features, then the room's own comma-separated amenities, as on the web
    val amenities = listOfNotNull(
        "Attached Washroom".takeIf { room.hasAttachedWashroom },
        "Balcony".takeIf { room.hasBalcony },
        "Furnished".takeIf { room.isFurnished }
    ) + room.amenities.orEmpty().split(',', '\n').map(String::trim).filter(String::isNotEmpty)

    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(18.dp), spacing = 14.dp) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Room ${room.roomNumber}",
                    style = RfType.HeadlineLg.copy(fontSize = 26.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold),
                    color = Rf.OnSurface
                )
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LIcon(Lucide.Building2, size = 14.dp, tint = Rf.OnSurfaceVariant)
                    Text(
                        listOfNotNull(propertyName, floorLabel(room.floor), room.roomSize?.takeIf(String::isNotBlank)).joinToString(" • "),
                        style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
                    )
                }
            }
            FillPill(room.occupancyType, tenants)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            HeaderChip(Lucide.Users, "${occupancyLabel(room.occupancyType)} Occupancy", highlight = true)
            amenities.forEach { HeaderChip(Lucide.CircleCheck, it, highlight = false) }
        }
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(14.dp)).background(Rf.Low)
        ) {
            MoneyCell("Monthly Rent", inr(room.rent), "/ mo", Modifier.weight(1f))
            Box(Modifier.width(1.dp).fillMaxHeight().background(Rf.OutlineVariant))
            MoneyCell("Security Deposit", inr(room.securityDeposit), null, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WideButton("Edit Room", Lucide.Pencil, Rf.Primary, Rf.OnPrimary, Modifier.weight(1f), onEdit)
            WideButton("Delete", Lucide.Trash2, Rf.ErrorContainer, Rf.Error, Modifier.weight(1f), onDelete)
        }
    }
}

@Composable
private fun HeaderChip(icon: LucideIcon, label: String, highlight: Boolean) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(if (highlight) Rf.PrimaryFixed else Rf.Low).padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        LIcon(icon, size = 13.dp, tint = if (highlight) Rf.Primary else Rf.Secondary)
        Text(label, style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = if (highlight) Rf.Primary else Rf.OnSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun MoneyCell(label: String, amount: String, unit: String?, modifier: Modifier) {
    Column(modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Caps(label)
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(amount, style = RfType.HeadlineSm.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface, maxLines = 1)
            if (unit != null) Text(unit, style = RfType.LabelSm, color = Rf.OnSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

/** The web's 44 dp page button with an icon. */
@Composable
private fun WideButton(label: String, icon: LucideIcon, background: Color, content: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(44.dp).pressable(onClick, pressScale = 0.97f).clip(RoundedCornerShape(12.dp)).background(background),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LIcon(icon, size = 16.dp, tint = content)
        Text(label, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = content, maxLines = 1)
    }
}

/** Icon tile and bold title that opens each card on the room page, with an optional [trailing] part. */
@Composable
private fun CardTitle(icon: LucideIcon, title: String, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Rf.PrimaryFixed), contentAlignment = Alignment.Center) {
            LIcon(icon, size = 16.dp, tint = Rf.Primary)
        }
        Text(title, style = RfType.BodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** A big photo to swipe, thumbnails under it, and full screen on a tap. */
@Composable
private fun RoomGallery(room: RoomDto) {
    val images = room.images
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { images.size.coerceAtLeast(1) }
    var viewing by remember { mutableStateOf<Int?>(null) }

    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(16.dp), spacing = 12.dp) {
        CardTitle(Lucide.Images, "Room Gallery") {
            if (images.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${images.size} ${if (images.size == 1) "photo" else "photos"}", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
                    if (images.size > 1) {
                        Row(
                            Modifier.pressable({ viewing = pager.currentPage }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LIcon(Lucide.LayoutGrid, size = 14.dp, tint = Rf.Primary)
                            Text("View all", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Primary)
                        }
                    }
                }
            }
        }
        if (images.isEmpty()) {
            Box(Modifier.fillMaxWidth().aspectRatio(3f / 2f).clip(RoundedCornerShape(12.dp))) {
                RemoteImage(null, Modifier.fillMaxSize(), placeholderIcon = Lucide.DoorOpen)
            }
            Text(
                "Use Edit Room to upload room photos.", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium),
                color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(Modifier.fillMaxWidth().aspectRatio(3f / 2f).clip(RoundedCornerShape(12.dp))) {
                HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                    RemoteImage(images[page].url, Modifier.fillMaxSize().pressable({ viewing = page }, pressScale = 1f), placeholderIcon = Lucide.DoorOpen)
                }
                if (images.size > 1) {
                    Text(
                        "${pager.currentPage + 1} / ${images.size}",
                        style = RfType.LabelSm, color = Color.White,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(CircleShape)
                            .background(Rf.InverseSurface.copy(alpha = 0.8f)).padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            if (images.size > 1) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    images.forEachIndexed { index, image ->
                        val active = index == pager.currentPage
                        Box(
                            Modifier
                                .size(width = 58.dp, height = 44.dp)
                                .pressable({ scope.launch { pager.animateScrollToPage(index) } })
                                .alpha(if (active) 1f else 0.6f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(if (active) 2.dp else 0.dp, if (active) Rf.Primary else Color.Transparent, RoundedCornerShape(8.dp))
                        ) {
                            RemoteImage(image.url, Modifier.fillMaxSize(), placeholderIcon = Lucide.DoorOpen, iconSize = 16.dp)
                        }
                    }
                }
            }
        }
    }
    viewing?.let { start ->
        PhotoViewer(images.map { Photo.Stored(it.key, it.url) }, start, onClose = { viewing = null })
    }
}

/** Who lives in the room: a row each with call and Remove, then Assign while there is space. */
@Composable
private fun OccupantsCard(
    room: RoomDto,
    occupants: List<TenantDto>,
    onOpenTenant: (String) -> Unit,
    onRemove: (TenantDto) -> Unit,
    onAssign: () -> Unit
) {
    val capacity = roomCapacity(room.occupancyType)
    val full = roomFull(room.occupancyType, occupants.size)
    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(18.dp), spacing = 12.dp) {
        CardTitle(Lucide.Users, "Occupants") { FillPill(room.occupancyType, occupants.size) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (capacity == null) {
                LIcon(Lucide.Info, size = 14.dp, tint = Rf.OnSurfaceVariant)
                Text("Shared room: no fixed limit", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(capacity) { i ->
                        val on = i < occupants.size
                        Box(
                            Modifier.size(width = 26.dp, height = 8.dp).clip(CircleShape)
                                .background(if (on) Rf.Primary else Rf.Low)
                                .border(1.dp, if (on) Rf.Primary else Rf.OutlineVariant, CircleShape)
                        )
                    }
                }
                Text(
                    "${occupants.size} of $capacity ${if (capacity == 1) "place" else "places"} filled",
                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
                )
            }
        }
        if (occupants.isEmpty()) {
            Text(
                "No one lives in this room yet.\nAssign a tenant to fill it.",
                style = RfType.BodySm, color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().dashedBorder(Rf.OutlineVariant).padding(14.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                occupants.forEach { tenant -> OccupantLine(tenant, onOpen = { onOpenTenant(tenant.id) }, onRemove = { onRemove(tenant) }) }
            }
        }
        if (full) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Rf.Low).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LIcon(Lucide.Info, size = 15.dp, tint = Rf.OnSurfaceVariant)
                Text("Room is full. Remove someone to make space.", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
            }
        } else {
            WideButton("Assign Tenant", Lucide.UserPlus, Rf.Primary, Rf.OnPrimary, Modifier.fillMaxWidth(), onAssign)
        }
    }
}

/** Initials, name and number (tap to open the tenant), a call button and Remove. */
@Composable
private fun OccupantLine(tenant: TenantDto, onOpen: () -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    val digits = mobileDigits(tenant.mobile)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Rf.Low).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            Modifier.weight(1f).pressable(onOpen, pressScale = 0.98f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Rf.MintSoft), contentAlignment = Alignment.Center) {
                Text(initials(tenant.fullName), style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.Secondary)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    tenant.fullName.ifBlank { "Tenant" }, style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold),
                    color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (digits.isNotEmpty()) Text(mobileLabel(tenant.mobile).orEmpty(), style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
            }
        }
        if (digits.isNotEmpty()) {
            Box(
                Modifier.size(32.dp)
                    .pressable({ context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91$digits"))) })
                    .clip(RoundedCornerShape(9.dp)).background(Rf.Lowest),
                contentAlignment = Alignment.Center
            ) {
                LIcon(Lucide.Phone, size = 15.dp, tint = Rf.Primary, contentDescription = "Call ${tenant.fullName}")
            }
        }
        Row(
            Modifier.height(32.dp).pressable(onRemove).clip(RoundedCornerShape(9.dp)).background(Rf.ErrorContainer).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LIcon(Lucide.LogOut, size = 13.dp, tint = Rf.Error)
            Text("Remove", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Error)
        }
    }
}

/** Six facts about the room in two columns, as on the web. */
@Composable
private fun RoomFactsCard(room: RoomDto) {
    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(18.dp), spacing = 12.dp) {
        CardTitle(Lucide.DoorOpen, "Room Details")
        val cells = listOf(
            Fact(Lucide.Layers, "Floor", floorLabel(room.floor)),
            Fact(Lucide.Users, "Occupancy", occupancyLabel(room.occupancyType)),
            Fact(Lucide.Maximize, "Room Size", room.roomSize?.takeIf(String::isNotBlank) ?: "—"),
            Fact(Lucide.Sofa, "Furnishing", if (room.isFurnished) "Furnished" else "Unfurnished"),
            Fact(Lucide.Bath, "Washroom", if (room.hasAttachedWashroom) "Attached" else "Shared", yes = room.hasAttachedWashroom),
            Fact(Lucide.DoorOpen, "Balcony", if (room.hasBalcony) "Yes" else "No", yes = room.hasBalcony)
        )
        cells.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { FactCell(it, Modifier.weight(1f)) }
            }
        }
    }
}

/** [yes] null is a plain value; true / false colour it as the web does for Washroom and Balcony. */
private data class Fact(val icon: LucideIcon, val label: String, val value: String, val yes: Boolean? = null)

@Composable
private fun FactCell(fact: Fact, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(Rf.Low).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(Rf.Lowest), contentAlignment = Alignment.Center) {
            LIcon(fact.icon, size = 14.dp, tint = Rf.Primary)
        }
        Column(Modifier.weight(1f)) {
            Caps(fact.label)
            Text(
                fact.value,
                style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold),
                color = when (fact.yes) {
                    true -> Rf.Secondary
                    false -> Rf.OnSurfaceVariant
                    null -> Rf.OnSurface
                },
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** The room's property, with a button to open it. */
@Composable
private fun PropertyLinkCard(property: PropertyDto, onOpen: () -> Unit) {
    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(18.dp), spacing = 12.dp) {
        CardTitle(Lucide.Building2, "Property")
        Column {
            Text(property.propertyName, style = RfType.BodyLg.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface)
            Text(
                listOf(property.address, property.city, property.pincode).filter(String::isNotBlank).joinToString(", "),
                style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, modifier = Modifier.padding(top = 3.dp)
            )
        }
        WideButton("View Property", Lucide.Eye, Rf.Low, Rf.Primary, Modifier.fillMaxWidth(), onOpen)
    }
}

/* ------------------------------ Add or edit a room ------------------------------ */

/** Add a room to [propertyId], or edit [roomId]. */
@Composable
fun RoomFormScreen(propertyId: String?, roomId: String?, shell: Shell, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val state = portfolioState()
    val portfolio = state.data
    val existing = roomId?.let { portfolio?.room(it) }
    if (portfolio == null || (roomId != null && existing == null)) {
        AppPage(topBar = { shell.TopBar(if (roomId == null) "Add Room" else "Edit Room", onBack = onBack) }) {
            PortfolioContent(state) { NotFound("Room", onBack) }
        }
        return
    }
    RoomForm(portfolio, existing, existing?.propertyId ?: propertyId, shell, onBack, onSaved)
}

@Composable
private fun RoomForm(
    portfolio: Portfolio,
    existing: RoomDto?,
    presetProperty: String?,
    shell: Shell,
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val properties = portfolio.properties

    var property by rememberSaveable { mutableIntStateOf(properties.indexOfFirst { it.id == presetProperty }) }
    var number by rememberSaveable { mutableStateOf(existing?.roomNumber.orEmpty()) }
    var floor by rememberSaveable { mutableIntStateOf(existing?.floor ?: -1) }
    var occupancy by rememberSaveable { mutableIntStateOf(OccupancyTypes.indexOfFirst { it.first == existing?.occupancyType }) }
    var rent by rememberSaveable { mutableStateOf(existing?.let { plainAmount(it.rent) }.orEmpty()) }
    var deposit by rememberSaveable { mutableStateOf(existing?.let { plainAmount(it.securityDeposit) }.orEmpty()) }
    var size by rememberSaveable { mutableStateOf(existing?.roomSize.orEmpty()) }
    var furnished by rememberSaveable { mutableStateOf(existing?.isFurnished ?: false) }
    var washroom by rememberSaveable { mutableStateOf(existing?.hasAttachedWashroom ?: false) }
    var balcony by rememberSaveable { mutableStateOf(existing?.hasBalcony ?: false) }
    var furniture by rememberSaveable { mutableStateOf(existing?.furnitureDetails.orEmpty()) }
    var amenities by rememberSaveable { mutableStateOf(existing?.amenities.orEmpty()) }
    var photos by rememberSaveable(stateSaver = PhotoListSaver) {
        mutableStateOf<List<Photo>>(existing?.images.orEmpty().map { Photo.Stored(it.key, it.url) })
    }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val rentValue = rent.toDoubleOrNull()
    val depositValue = deposit.toDoubleOrNull()
    val propertyErr = if (submitted && property < 0) "Choose a property" else null
    val numberErr = if (submitted && number.isBlank()) "Room number is required" else null
    val floorErr = if (submitted && floor < 0) "Choose a floor" else null
    val occupancyErr = if (submitted && occupancy < 0) "Choose who the room is for" else null
    val rentErr = if (submitted && (rentValue == null || rentValue <= 0)) "Enter the monthly rent" else null
    val depositErr = if (submitted && depositValue == null) "Enter the deposit, or 0" else null

    fun save() {
        submitted = true
        if (property < 0 || number.isBlank() || floor < 0 || occupancy < 0 || rentValue == null || rentValue <= 0 || depositValue == null) {
            Toasts.error("Some details are missing. Check the fields marked in red.")
            return
        }
        if (saving) return
        saving = true
        val body = RoomBody(
            propertyId = properties[property].id, roomNumber = number.trim(), floor = floor,
            occupancyType = OccupancyTypes[occupancy].first, rent = rentValue, securityDeposit = depositValue,
            isFurnished = furnished, furnitureDetails = if (furnished) furniture.trim() else "",
            hasAttachedWashroom = washroom, hasBalcony = balcony, roomSize = size.trim(),
            amenities = amenities.trim(), images = emptyList()
        )
        scope.launch {
            val result = LandlordStore.saveRoom(context, existing?.id, body, photos)
            saving = false
            if (result.announce(if (existing == null) "Room added" else "Room saved")) onSaved((result as ApiResult.Ok).value)
        }
    }

    AppPage(
        topBar = { shell.TopBar(if (existing == null) "Add Room" else "Edit Room", onBack = onBack) },
        bottomBar = { FormBar("Save room", ::save, loading = saving, secondary = "Cancel", onSecondary = onBack) }
    ) {
        FormSection(Lucide.DoorOpen, "Room") {
            SelectField(
                label = "Property", value = properties.getOrNull(property)?.propertyName, options = properties.map { it.propertyName },
                onSelect = { property = it }, icon = Lucide.Building2, required = true,
                // The API keeps a room in its property, as on the web
                enabled = existing == null, hint = if (existing != null) "A room stays in the property it was added to." else null,
                error = propertyErr
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RfTextField(
                    label = "Room number", value = number, onValueChange = { number = it.take(20) }, icon = Lucide.DoorOpen,
                    placeholder = "e.g. 204", capitalization = KeyboardCapitalization.Characters, error = numberErr,
                    modifier = Modifier.weight(1f)
                )
                SelectField(
                    label = "Floor", value = if (floor >= 0) floorLabel(floor) else null, options = FloorOptions.map(::floorLabel),
                    onSelect = { floor = FloorOptions[it] }, icon = Lucide.Layers, required = true, error = floorErr,
                    modifier = Modifier.weight(1f)
                )
            }
            ChoiceChips("Occupancy", OccupancyTypes.map { it.second }, occupancy, { occupancy = it }, required = true, error = occupancyErr)
        }

        FormSection(Lucide.IndianRupee, "Rent") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RfTextField(
                    label = "Monthly rent", value = rent, onValueChange = { rent = it.filter(Char::isDigit).take(9) },
                    icon = Lucide.IndianRupee, placeholder = "14000", keyboardType = KeyboardType.Number, error = rentErr,
                    modifier = Modifier.weight(1f)
                )
                RfTextField(
                    label = "Deposit", value = deposit, onValueChange = { deposit = it.filter(Char::isDigit).take(9) },
                    icon = Lucide.IndianRupee, placeholder = "28000", keyboardType = KeyboardType.Number, error = depositErr,
                    modifier = Modifier.weight(1f)
                )
            }
            RfTextField(
                label = "Room size", value = size, onValueChange = { size = it }, icon = Lucide.Ruler,
                placeholder = "e.g. 150 sq ft", hint = "Optional"
            )
        }

        FormSection(Lucide.Sofa, "Facilities") {
            Column {
                SwitchRow(Lucide.Sofa, "Furnished", furnished, { furnished = it })
                RowDivider()
                SwitchRow(Lucide.Bath, "Attached washroom", washroom, { washroom = it })
                RowDivider()
                SwitchRow(Lucide.Sun, "Balcony", balcony, { balcony = it })
            }
            if (furnished) {
                RfTextField(
                    label = "Furniture details", value = furniture, onValueChange = { furniture = it }, icon = Lucide.Sofa,
                    placeholder = "Bed, wardrobe, study table", capitalization = KeyboardCapitalization.Sentences
                )
            }
            TextArea("Amenities", amenities, { amenities = it }, "Wi-Fi, geyser, power backup")
        }

        FormSection(Lucide.Image, "Photos", caption = "Up to 10") {
            PhotoGrid(photos, onChange = { photos = it })
        }
    }
}
