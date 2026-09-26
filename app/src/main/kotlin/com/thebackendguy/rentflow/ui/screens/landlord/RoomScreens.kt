package com.thebackendguy.rentflow.ui.screens.landlord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thebackendguy.rentflow.data.FloorOptions
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.OccupancyTypes
import com.thebackendguy.rentflow.data.Photo
import com.thebackendguy.rentflow.data.Portfolio
import com.thebackendguy.rentflow.data.apiDate
import com.thebackendguy.rentflow.data.dueDayLabel
import com.thebackendguy.rentflow.data.floorLabel
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.mobileLabel
import com.thebackendguy.rentflow.data.occupancyLabel
import com.thebackendguy.rentflow.data.plainAmount
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.LeaseDto
import com.thebackendguy.rentflow.data.remote.RoomBody
import com.thebackendguy.rentflow.data.remote.RoomDto
import com.thebackendguy.rentflow.data.remote.TenantDto
import com.thebackendguy.rentflow.data.shortLabel
import com.thebackendguy.rentflow.ui.components.ActionButton
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.ChoiceChips
import com.thebackendguy.rentflow.ui.components.ConfirmSheet
import com.thebackendguy.rentflow.ui.components.Detail
import com.thebackendguy.rentflow.ui.components.DetailRows
import com.thebackendguy.rentflow.ui.components.FormBar
import com.thebackendguy.rentflow.ui.components.FormSection
import com.thebackendguy.rentflow.ui.components.IconDetail
import com.thebackendguy.rentflow.ui.components.IconRows
import com.thebackendguy.rentflow.ui.components.IconTile
import com.thebackendguy.rentflow.ui.components.PersonAvatar
import com.thebackendguy.rentflow.ui.components.PhotoGrid
import com.thebackendguy.rentflow.ui.components.Pill
import com.thebackendguy.rentflow.ui.components.PrimaryButton
import com.thebackendguy.rentflow.ui.components.RemoteImage
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.RfTextField
import com.thebackendguy.rentflow.ui.components.RowDivider
import com.thebackendguy.rentflow.ui.components.SectionHead
import com.thebackendguy.rentflow.ui.components.SelectField
import com.thebackendguy.rentflow.ui.components.SwitchRow
import com.thebackendguy.rentflow.ui.components.TextArea
import com.thebackendguy.rentflow.ui.components.TitledCard
import com.thebackendguy.rentflow.ui.components.Toasts
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.launch

/* ------------------------------ Room details ------------------------------ */

@Composable
fun RoomDetailsScreen(
    roomId: String,
    shell: Shell,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenTenant: (String) -> Unit,
    onAssign: () -> Unit,
    onDeleted: () -> Unit
) {
    val state = portfolioState()
    val room = state.data?.room(roomId)
    var confirmDelete by remember { mutableStateOf(false) }
    var ending by remember { mutableStateOf<TenantDto?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AppPage(topBar = { shell.TopBar(room?.let { "Room ${it.roomNumber}" } ?: "Room", onBack = onBack) }, bottomBar = { shell.BottomNav(1) }) {
        PortfolioContent(state, loadingImageHeight = 120.dp) { portfolio ->
            if (room == null) {
                NotFound("Room", onBack)
                return@PortfolioContent
            }
            val property = portfolio.property(room.propertyId)
            val occupants = portfolio.occupants(room.id)
            val let = occupants.isNotEmpty()

            RfCard(padding = PaddingValues(0.dp)) {
                Box(Modifier.fillMaxWidth().height(120.dp)) {
                    RemoteImage(room.images.firstOrNull()?.url, Modifier.fillMaxSize(), placeholderIcon = Lucide.DoorOpen)
                    Pill(
                        if (let) "Let" else "Vacant", background = Rf.InverseSurface.copy(alpha = 0.8f), content = Rf.InverseOnSurface,
                        dot = true, dotColor = if (let) Rf.Mint else Color(0xFFC3C0FF),
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                    )
                    Column(
                        Modifier.align(Alignment.BottomStart).padding(10.dp).clip(RoundedCornerShape(6.dp))
                            .background(Rf.InverseSurface.copy(alpha = 0.85f)).padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Monthly rent", style = RfType.LabelSm, color = Rf.InverseOnSurface.copy(alpha = 0.8f))
                        Text(inr(room.rent), style = RfType.HeadlineSm.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                }
                Column(Modifier.padding(14.dp)) {
                    Text("Room ${room.roomNumber} • ${floorLabel(room.floor)}", style = RfType.BodyLg.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                    Text(
                        listOfNotNull(property?.propertyName, occupancyLabel(room.occupancyType)).joinToString(" • "),
                        style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
                    )
                }
            }

            if (let) {
                occupants.forEach { tenant ->
                    val lease = tenant.lease ?: return@forEach
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHead("Current lease") { Pill("Active", background = Rf.MintSoft, content = Rf.Secondary) }
                        LeaseCard(tenant, lease) { onOpenTenant(tenant.id) }
                        ActionButton("End lease", Lucide.LogOut, Rf.ErrorContainer, Rf.Error, Modifier.fillMaxWidth()) { ending = tenant }
                    }
                }
                if (room.isAvailable) {
                    ActionButton("Assign another tenant", Lucide.UserPlus, Rf.Container, Rf.Primary, Modifier.fillMaxWidth(), onClick = onAssign)
                }
            } else {
                RfCard {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconTile(Lucide.DoorOpen, background = Rf.PrimaryFixed, tint = Rf.Primary, size = 48.dp, radius = 14.dp, iconSize = 24.dp)
                        Text("This room is vacant", style = RfType.BodyLg.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                        Text(
                            "Assign a tenant to start a lease. The rent and deposit are filled in from this room.",
                            style = RfType.BodySm, color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center
                        )
                    }
                    PrimaryButton("Assign tenant", onClick = onAssign, showArrow = false, modifier = Modifier.padding(top = 16.dp))
                }
            }

            TitledCard("Room details") {
                IconRows(roomFacts(room))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("Edit room", Lucide.Pencil, Rf.Container, Rf.Primary, Modifier.weight(1f), onClick = onEdit)
                ActionButton("Delete", Lucide.Trash2, Rf.ErrorContainer, Rf.Error, Modifier.weight(1f)) { confirmDelete = true }
            }

            ending?.let { tenant ->
                ConfirmSheet(
                    icon = Lucide.LogOut,
                    title = "End ${tenant.fullName}’s lease?",
                    message = "Room ${room.roomNumber} becomes free for another tenant. ${tenant.fullName}’s account and the lease record are kept.",
                    confirmLabel = "End lease",
                    busy = busy,
                    onConfirm = {
                        val leaseId = tenant.lease?.id ?: return@ConfirmSheet
                        busy = true
                        scope.launch {
                            LandlordStore.endLease(leaseId).announce("Lease ended")
                            busy = false
                            ending = null
                        }
                    },
                    onDismiss = { ending = null }
                )
            }

            if (confirmDelete) {
                ConfirmSheet(
                    icon = Lucide.Trash2,
                    title = "Delete room ${room.roomNumber}?",
                    message = if (let) "This room still has a tenant. End the lease first, then delete the room."
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

/** A tenant's lease: who, then the terms. Tapping the tenant opens them. */
@Composable
internal fun LeaseCard(tenant: TenantDto, lease: LeaseDto, onOpenTenant: (() -> Unit)?) {
    RfCard {
        Row(
            Modifier.fillMaxWidth().pressable(onOpenTenant, pressScale = 0.98f).padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PersonAvatar(tenant.fullName, tenant.profilePic, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(tenant.fullName, style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                Text(mobileLabel(tenant.mobile) ?: tenant.email, style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
            }
            if (onOpenTenant != null) LIcon(Lucide.ChevronRight, tint = Rf.Outline)
        }
        HorizontalDivider(thickness = 1.dp, color = Rf.Low, modifier = Modifier.padding(bottom = 12.dp))
        DetailRows(leaseRows(lease))
    }
}

internal fun leaseRows(lease: LeaseDto): List<Detail> {
    val start = apiDate(lease.startDate)?.shortLabel()
    val end = apiDate(lease.endDate)?.shortLabel()
    return listOfNotNull(
        if (start != null && end != null) Detail("Lease", "$start – $end") else null,
        Detail("Monthly rent", inr(lease.rent)),
        Detail("Rent due", dueDayLabel(lease.rentDueDay)),
        Detail("Deposit held", inr(lease.securityDeposit)),
        if (lease.maintenanceCharge > 0) Detail("Maintenance", "${inr(lease.maintenanceCharge)} / month") else null,
        Detail("Lock-in • Notice", "${months(lease.lockInMonths)} • ${months(lease.noticePeriodMonths)}"),
        if (lease.escalationPercent > 0) Detail("Increase on renewal", "${lease.escalationPercent.toInt()}%") else null
    )
}

private fun months(n: Int) = when (n) {
    0 -> "None"
    1 -> "1 month"
    else -> "$n months"
}

private fun roomFacts(room: RoomDto): List<IconDetail> = listOfNotNull(
    IconDetail(Lucide.Users, "Occupancy", occupancyLabel(room.occupancyType)),
    IconDetail(Lucide.IndianRupee, "Security deposit", inr(room.securityDeposit)),
    room.roomSize?.takeIf(String::isNotBlank)?.let { IconDetail(Lucide.Ruler, "Room size", it) },
    IconDetail(
        Lucide.Sofa, "Furnished",
        if (room.isFurnished) room.furnitureDetails?.takeIf(String::isNotBlank) ?: "Yes" else "Not furnished"
    ),
    IconDetail(
        Lucide.Bath, "Washroom • Balcony",
        "${if (room.hasAttachedWashroom) "Attached" else "Shared"} • ${if (room.hasBalcony) "Balcony" else "No balcony"}"
    ),
    room.amenities?.takeIf(String::isNotBlank)?.let { IconDetail(Lucide.Wifi, "Amenities", it) }
)

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
    var photos by remember { mutableStateOf<List<Photo>>(existing?.images.orEmpty().map { Photo.Stored(it.key, it.url) }) }
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
