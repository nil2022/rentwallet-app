package com.thebackendguy.rentflow.ui.screens.landlord

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.floorLabel
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.mobileDigits
import com.thebackendguy.rentflow.data.mobileLabel
import com.thebackendguy.rentflow.data.occupancyLabel
import com.thebackendguy.rentflow.data.remote.ImageDto
import com.thebackendguy.rentflow.data.remote.PropertyDto
import com.thebackendguy.rentflow.data.remote.RoomDto
import com.thebackendguy.rentflow.data.remote.TenantDto
import com.thebackendguy.rentflow.data.roomCapacity
import com.thebackendguy.rentflow.data.roomFull
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.EmptyState
import com.thebackendguy.rentflow.ui.components.Fab
import com.thebackendguy.rentflow.ui.components.Pill
import com.thebackendguy.rentflow.ui.components.PrimaryButton
import com.thebackendguy.rentflow.ui.components.RemoteImage
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.SelectField
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.components.softShadow
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType

/*
 * The Rooms tab: the web's phone room list (RoomList.jsx). One property at a
 * time, its summary, then a card per room with who lives there. On a phone every
 * room of the property is in one scroll instead of the web's pages of ten.
 */

/** The property last looked at, kept while the app runs so the tab comes back to it. */
private object RoomsTabMemory {
    var propertyId: String? = null
}

/** The web's status filter: everything, rooms that can take a tenant, or full ones. */
private enum class RoomStatus(val label: String) { All("All"), Space("Has space"), Full("Full") }

/** Natural order, so "9" comes before "10" and "101" before "1A", as on the web. */
private val RoomNumberOrder = compareBy<RoomDto>(
    { it.roomNumber.takeWhile(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE },
    { it.roomNumber }
)

@Composable
fun RoomsScreen(shell: Shell, onOpenRoom: (String) -> Unit, onAssign: (String) -> Unit, onAddRoom: (String?) -> Unit) {
    val state = portfolioState()
    LaunchedEffect(Unit) { LandlordStore.refresh() }
    var chosen by rememberSaveable { mutableStateOf(RoomsTabMemory.propertyId) }
    var query by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableIntStateOf(RoomStatus.All.ordinal) }
    var floor by rememberSaveable { mutableStateOf<Int?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    val portfolio = state.data
    // The chosen property, or the first one until one is chosen
    val property = portfolio?.let { it.property(chosen) ?: it.properties.firstOrNull() }
    val rooms = property?.let { portfolio.roomsOf(it.id) }.orEmpty().sortedWith(RoomNumberOrder)

    fun choose(id: String) {
        chosen = id
        RoomsTabMemory.propertyId = id
        query = ""
        status = RoomStatus.All.ordinal
        floor = null
    }

    Box(Modifier.fillMaxSize()) {
        AppPage(
            topBar = { shell.TopBar("Rooms", chip = property?.let { "${rooms.size} ${if (rooms.size == 1) "room" else "rooms"}" }) },
            bottomBar = { shell.BottomNav(2) }
        ) {
            PortfolioContent(state, loadingImageHeight = 150.dp) { data ->
                if (property == null) {
                    EmptyState(
                        icon = Lucide.DoorOpen,
                        title = "No rooms yet",
                        message = "Add a property first, then its rooms will show up here."
                    )
                    return@PortfolioContent
                }
                val tenantsIn = rooms.associate { it.id to data.occupants(it.id) }
                val full = { room: RoomDto -> roomFull(room.occupancyType, tenantsIn[room.id].orEmpty().size) }

                SelectField(
                    label = "Property",
                    value = property.propertyName,
                    options = data.properties.map { it.propertyName },
                    onSelect = { choose(data.properties[it].id) },
                    icon = Lucide.Building2
                )

                PropertySummary(property, rooms, tenantsIn)

                val statusFilter = RoomStatus.entries[status]
                val q = query.trim()
                val shown = rooms.filter { room ->
                    (floor == null || room.floor == floor) &&
                        when (statusFilter) {
                            RoomStatus.All -> true
                            RoomStatus.Space -> !full(room)
                            RoomStatus.Full -> full(room)
                        } &&
                        (q.isEmpty() || listOfNotNull(room.roomNumber, room.occupancyType, room.roomSize, floorLabel(room.floor))
                            .any { it.contains(q, ignoreCase = true) })
                }
                val filtersOn = (if (statusFilter != RoomStatus.All) 1 else 0) + (if (floor != null) 1 else 0)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { SearchBox(query, "Search room no., type or size") { query = it } }
                        FilterButton(filtersOn) { showFilters = true }
                    }
                    if (filtersOn > 0) AppliedFilters(statusFilter, floor, onClearStatus = { status = RoomStatus.All.ordinal }, onClearFloor = { floor = null })
                    Text(
                        if (shown.size == rooms.size) "Showing all ${rooms.size} ${if (rooms.size == 1) "room" else "rooms"}"
                        else "Showing ${shown.size} of ${rooms.size} rooms",
                        style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
                    )
                }

                if (shown.isEmpty()) {
                    RfCard {
                        Text(
                            if (rooms.isEmpty()) "No rooms added to this property yet. Tap “Add room” to add one." else "No rooms match your filters.",
                            style = RfType.BodyMd, color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        shown.forEach { room ->
                            RoomCard(
                                room, tenantsIn[room.id].orEmpty(), full(room),
                                onOpen = { onOpenRoom(room.id) }, onAssign = { onAssign(room.id) }
                            )
                        }
                    }
                }
                // Room for the floating button over the last card
                Box(Modifier.height(48.dp))

                if (showFilters) {
                    FilterSheet(
                        rooms = rooms,
                        full = full,
                        status = statusFilter,
                        floor = floor,
                        shown = shown.size,
                        onStatus = { status = it.ordinal },
                        onFloor = { floor = it },
                        onDismiss = { showFilters = false }
                    )
                }
            }
        }
        if (property != null) Fab("Add room", Lucide.Plus, { onAddRoom(property.id) }, Modifier.align(Alignment.BottomEnd))
    }
}

/* ------------------------------ Property summary ------------------------------ */

/** The property and how its rooms are doing, counted from who actually lives in them. */
@Composable
private fun PropertySummary(property: PropertyDto, rooms: List<RoomDto>, tenantsIn: Map<String, List<TenantDto>>) {
    val floors = rooms.map { it.floor }.distinct().size.takeIf { it > 0 } ?: property.floorCount
    val target = rooms.sumOf { it.rent }
    val full = rooms.count { roomFull(it.occupancyType, tenantsIn[it.id].orEmpty().size) }
    val people = tenantsIn.values.sumOf { it.size }
    val roomsWord = if (rooms.size == 1) "room" else "rooms"
    val levelsWord = if (floors == 1) "level" else "levels"

    RfCard(padding = PaddingValues(0.dp), shape = RoundedCornerShape(16.dp)) {
        RemoteImage(property.images.firstOrNull()?.url, Modifier.fillMaxWidth().height(150.dp), placeholderIcon = Lucide.Building2)
        Column(Modifier.padding(14.dp)) {
            if (property.isActive) {
                Pill("ACTIVE ESTATE", background = Rf.MintSoft, content = Rf.Secondary, style = RfType.LabelSm.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp))
            } else {
                Pill("INACTIVE ESTATE", background = Rf.ErrorContainer, content = Rf.Error, style = RfType.LabelSm.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp))
            }
            Text(
                property.propertyName, style = RfType.HeadlineSm.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                "${listOf(property.address, property.city).filter(String::isNotBlank).joinToString(", ")} • ${rooms.size} $roomsWord across $floors $levelsWord",
                style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
            )
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("Monthly target", inr(target), "Sum of room rents", Rf.Primary, null, Modifier.weight(1f))
                    MiniStat("Total units", "${rooms.size}", "$floors ${if (floors == 1) "floor" else "floors"}", Rf.Primary, null, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat(
                        "Full units", "$full", "$people ${if (people == 1) "tenant" else "tenants"} living here",
                        Rf.Secondary, Rf.Secondary, Modifier.weight(1f)
                    )
                    MiniStat("Units with space", "${rooms.size - full}", "Ready to assign", Rf.Primary, Rf.Primary, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, foot: String, footColor: Color, dot: Color?, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Rf.Low).padding(horizontal = 10.dp, vertical = 9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (dot != null) Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
            Caps(label)
        }
        Text(value, style = RfType.HeadlineSm.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface, maxLines = 1)
        Text(foot, style = RfType.LabelSm, color = footColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The web's small uppercase label. */
@Composable
internal fun Caps(text: String) {
    Text(text.uppercase(), style = RfType.LabelSm.copy(fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp), color = Rf.OnSurfaceVariant)
}

/* ------------------------------ Search and filters ------------------------------ */

@Composable
internal fun FilterButton(on: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box {
        Box(
            Modifier.size(48.dp).pressable(onClick).softShadow(shape).clip(shape).background(Rf.Lowest),
            contentAlignment = Alignment.Center
        ) {
            LIcon(Lucide.SlidersHorizontal, size = 20.dp, tint = Rf.Primary, contentDescription = if (on > 0) "Filters, $on on" else "Filters")
        }
        if (on > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 0.dp).size(18.dp).clip(CircleShape).background(Rf.Primary),
                contentAlignment = Alignment.Center
            ) {
                Text("$on", style = RfType.LabelSm.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold), color = Rf.OnPrimary)
            }
        }
    }
}

/** Each filter that is on, as a chip; tapping it takes that filter off. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppliedFilters(status: RoomStatus, floor: Int?, onClearStatus: () -> Unit, onClearFloor: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (status != RoomStatus.All) AppliedChip(status.label, onClearStatus)
        if (floor != null) AppliedChip(floorLabel(floor), onClearFloor)
    }
}

@Composable
internal fun AppliedChip(label: String, onRemove: () -> Unit) {
    Pill(
        label, background = Rf.PrimaryFixed, content = Rf.Primary, trailingIcon = Lucide.X,
        style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), horizontal = 10.dp, vertical = 5.dp,
        modifier = Modifier.pressable(onRemove)
    )
}

/** The web's phone filter sheet: Status and Floor, each option with how many rooms it keeps. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    rooms: List<RoomDto>,
    full: (RoomDto) -> Boolean,
    status: RoomStatus,
    floor: Int?,
    shown: Int,
    onStatus: (RoomStatus) -> Unit,
    onFloor: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val counts = mapOf(
        RoomStatus.All to rooms.size,
        RoomStatus.Space to rooms.count { !full(it) },
        RoomStatus.Full to rooms.count(full)
    )
    val floors = rooms.groupingBy { it.floor }.eachCount().toSortedMap()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Rf.Lowest,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filters", style = RfType.HeadlineSm.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(32.dp).pressable(onDismiss).clip(RoundedCornerShape(10.dp)).background(Rf.Low),
                    contentAlignment = Alignment.Center
                ) {
                    LIcon(Lucide.X, size = 16.dp, tint = Rf.OnSurfaceVariant, contentDescription = "Close")
                }
            }
            FilterSection("Status") {
                RoomStatus.entries.forEach { option ->
                    FilterOption(option.label, counts[option], option == status) { onStatus(option) }
                }
            }
            FilterSection("Floor") {
                FilterOption("All floors", null, floor == null) { onFloor(null) }
                floors.forEach { (f, count) -> FilterOption(floorLabel(f), count, floor == f) { onFloor(f) } }
            }
            PrimaryButton("Show $shown ${if (shown == 1) "room" else "rooms"}", onClick = onDismiss, showArrow = false)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(label: String, options: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Caps(label)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { options() }
    }
}

@Composable
private fun FilterOption(label: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .pressable(onClick, pressScale = 0.96f)
            .clip(CircleShape)
            .background(if (selected) Rf.PrimaryFixed else Rf.Lowest)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Rf.Primary else Rf.OutlineVariant, CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = RfType.LabelMd, color = if (selected) Rf.Primary else Rf.OnSurface)
        if (count != null) Text("$count", style = RfType.LabelMd.copy(fontWeight = FontWeight.Medium), color = if (selected) Rf.Primary else Rf.OnSurfaceVariant)
    }
}

/* ------------------------------ Room card ------------------------------ */

/** The web's RoomCard: the whole card opens the room; a room with space can be filled from here. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoomCard(room: RoomDto, occupants: List<TenantDto>, full: Boolean, onOpen: () -> Unit, onAssign: () -> Unit) {
    val features = listOfNotNull(
        "Attached Washroom".takeIf { room.hasAttachedWashroom },
        "Attached Balcony".takeIf { room.hasBalcony },
        "Furnished".takeIf { room.isFurnished }
    )
    RfCard(padding = PaddingValues(0.dp), shape = RoundedCornerShape(16.dp), onClick = onOpen) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Room ${room.roomNumber}", style = RfType.LabelSm.copy(fontWeight = FontWeight.ExtraBold), color = Rf.Primary, maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Rf.PrimaryFixed).padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    listOfNotNull(floorLabel(room.floor), occupancyLabel(room.occupancyType), room.roomSize?.takeIf(String::isNotBlank)).joinToString(" • "),
                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, modifier = Modifier.weight(1f)
                )
                FillPill(room.occupancyType, occupants.size)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(inr(room.rent), style = RfType.HeadlineMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
                Text("/ month", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            }
            CardPhotos(room.images)

            // Who lives here
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { Caps("Occupants") }
                    val capacity = roomCapacity(room.occupancyType)
                    if (capacity == null) {
                        Text("no fixed limit", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(capacity) { i ->
                                val on = i < occupants.size
                                Box(
                                    Modifier.size(width = 18.dp, height = 6.dp).clip(CircleShape)
                                        .background(if (on) Rf.Primary else Rf.Low)
                                        .border(1.dp, if (on) Rf.Primary else Rf.OutlineVariant, CircleShape)
                                )
                            }
                        }
                    }
                }
                if (occupants.isEmpty()) {
                    Text(
                        "No one lives here yet.", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().dashedBorder(Rf.OutlineVariant).padding(10.dp)
                    )
                } else {
                    occupants.forEach { OccupantRow(it) }
                }
            }

            if (features.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    features.forEach { feature ->
                        Text(
                            feature, style = RfType.LabelSm.copy(fontSize = 10.5.sp), color = Rf.OnSurfaceVariant,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Rf.Low).padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Row {
                Text("Security Deposit", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, modifier = Modifier.weight(1f))
                Text(inr(room.securityDeposit), style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface)
            }
        }
        if (!full) {
            HorizontalDivider(thickness = 1.dp, color = Rf.Low)
            Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(
                    Modifier.fillMaxWidth().height(40.dp).pressable(onAssign).clip(RoundedCornerShape(12.dp)).background(Rf.Primary),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LIcon(Lucide.UserPlus, size = 16.dp, tint = Rf.OnPrimary)
                    Text("Assign Tenant", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.OnPrimary)
                }
            }
        }
    }
}

/** How full the room is, in the web's words and colours. */
@Composable
internal fun FillPill(type: String?, tenants: Int) {
    val capacity = roomCapacity(type)
    val (text, background, content) = when {
        tenants == 0 -> Triple("Empty", Rf.PrimaryFixed, Rf.Primary)
        capacity == null -> Triple("$tenants ${if (tenants == 1) "tenant" else "tenants"} • shared", Rf.MintSoft, Rf.Secondary)
        tenants >= capacity -> Triple("Full", Rf.ErrorContainer, Rf.Error)
        else -> Triple("$tenants of $capacity filled", Rf.AmberSoft, Rf.Amber)
    }
    Pill(text, background = background, content = content, style = RfType.LabelSm.copy(fontWeight = FontWeight.ExtraBold), vertical = 4.dp)
}

/** Initials, name and number; the phone icon calls them. */
@Composable
private fun OccupantRow(tenant: TenantDto) {
    val context = LocalContext.current
    val digits = mobileDigits(tenant.mobile)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Rf.Low).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Rf.MintSoft), contentAlignment = Alignment.Center) {
            Text(initials(tenant.fullName), style = RfType.LabelSm.copy(fontWeight = FontWeight.ExtraBold), color = Rf.Secondary)
        }
        Column(Modifier.weight(1f)) {
            Text(tenant.fullName.ifBlank { "Tenant" }, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (digits.isNotEmpty()) Text(mobileLabel(tenant.mobile).orEmpty(), style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
        }
        if (digits.isNotEmpty()) {
            Box(
                Modifier.size(32.dp).pressable({ context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91$digits"))) }),
                contentAlignment = Alignment.Center
            ) {
                LIcon(Lucide.Phone, size = 16.dp, tint = Rf.Primary, contentDescription = "Call ${tenant.fullName}")
            }
        }
    }
}

internal fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter(String::isNotEmpty).take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

/** Swipeable photos with a counter and dots, or the room placeholder when there are none. */
@Composable
private fun CardPhotos(images: List<ImageDto>) {
    val pager = rememberPagerState { images.size.coerceAtLeast(1) }
    Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp))) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            RemoteImage(images.getOrNull(page)?.url, Modifier.fillMaxSize(), placeholderIcon = Lucide.DoorOpen)
        }
        if (images.size > 1) {
            Text(
                "${pager.currentPage + 1} / ${images.size}",
                style = RfType.LabelSm, color = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(CircleShape)
                    .background(Rf.InverseSurface.copy(alpha = 0.8f)).padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(images.size) { i ->
                    val active = i == pager.currentPage
                    Box(
                        Modifier.size(width = if (active) 14.dp else 6.dp, height = 6.dp).clip(CircleShape)
                            .background(if (active) Color.White else Color.White.copy(alpha = 0.55f))
                    )
                }
            }
        }
    }
}

/** The web's dashed outline for an empty slot. */
internal fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
        cornerRadius = CornerRadius(10.dp.toPx())
    )
}
