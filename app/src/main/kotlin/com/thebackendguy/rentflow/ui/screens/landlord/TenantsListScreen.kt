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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.PaymentStore
import com.thebackendguy.rentflow.data.RentKind
import com.thebackendguy.rentflow.data.RentStatus
import com.thebackendguy.rentflow.data.apiDate
import com.thebackendguy.rentflow.data.floorLabel
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.label
import com.thebackendguy.rentflow.data.mobileDigits
import com.thebackendguy.rentflow.data.mobileLabel
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.PropertyDto
import com.thebackendguy.rentflow.data.remote.TenantDto
import com.thebackendguy.rentflow.data.shortLabel
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.ConfirmSheet
import com.thebackendguy.rentflow.ui.components.EmptyState
import com.thebackendguy.rentflow.ui.components.FadeIn
import com.thebackendguy.rentflow.ui.components.Fab
import com.thebackendguy.rentflow.ui.components.PersonAvatar
import com.thebackendguy.rentflow.ui.components.Pill
import com.thebackendguy.rentflow.ui.components.PrimaryButton
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.Skeleton
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.launch
import java.time.YearMonth

/*
 * The Tenants tab: the web's phone Tenant Directory (TenantDirectory.jsx). Search, a
 * filter sheet for property and rent status, and a card per tenant with their room,
 * rent and this month's payment status from the bills. Every tenant is in one scroll
 * instead of the web's pages of ten.
 */

/** The web's room-type wording on a tenant card. */
private fun roomTypeLabel(type: String?): String = when (type) {
    "single" -> "Single room"
    "double" -> "Double sharing"
    "triple" -> "Triple sharing"
    "shared" -> "Shared room"
    "other" -> "Other use"
    else -> "Room assigned"
}

/** The web's search: name, mobile, email, or "room 101". */
private fun TenantDto.matches(q: String): Boolean {
    val digits = q.filter(Char::isDigit)
    return listOfNotNull(fullName, email, room?.roomNumber?.let { "room $it" }).any { it.contains(q, ignoreCase = true) } ||
        (digits.length >= 3 && mobileDigits(mobile).contains(digits))
}

@Composable
fun TenantsScreen(
    shell: Shell,
    onOpenTenant: (String) -> Unit,
    onEditTenant: (String) -> Unit,
    onAddTenant: () -> Unit,
    onAssignRoom: (String) -> Unit
) {
    val state = portfolioState()
    LaunchedEffect(Unit) { LandlordStore.refresh() }
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var propertyId by rememberSaveable { mutableStateOf<String?>(null) }
    var statusKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<TenantDto?>(null) }
    var deleting by remember { mutableStateOf<TenantDto?>(null) }
    var busy by remember { mutableStateOf(false) }

    // This month's rent status of every billed tenant, loaded again after any bill changes
    val billsVersion by PaymentStore.version.collectAsState()
    var statuses by remember { mutableStateOf<Map<String, RentStatus>?>(null) }
    var statusesFailed by remember { mutableStateOf(false) }
    LaunchedEffect(billsVersion) {
        when (val r = PaymentStore.rentStatuses()) {
            is ApiResult.Ok -> { statuses = r.value; statusesFailed = false }
            // Without the bills the pills stop pulsing and fall back to Active / Inactive
            is ApiResult.Fail -> statusesFailed = true
        }
    }
    val status = RentKind.entries.find { it.key == statusKey }
    val hasTenants = !state.data?.tenants.isNullOrEmpty()

    Box(Modifier.fillMaxSize()) {
        AppPage(topBar = { shell.TopBar("Tenants") }, bottomBar = { shell.BottomNav(3) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text("Tenant Directory", style = RfType.HeadlineMd.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
                    Text(
                        "Add tenants and see their rooms and rent in one place.", style = RfType.BodySm, color = Rf.OnSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                PortfolioContent(state) { portfolio ->
                    val tenants = portfolio.tenants
                    if (tenants.isEmpty()) {
                        EmptyState(
                            icon = Lucide.Users,
                            title = "No tenants yet",
                            message = "Add a tenant and give them a room. They sign in with their mobile number, or their email if you add one.",
                            actionLabel = "Add tenant",
                            actionIcon = Lucide.UserPlus,
                            onAction = onAddTenant
                        )
                        return@PortfolioContent
                    }
                    // Only tenants in a room are billed; their status shows once the bills load
                    val statusOf = { t: TenantDto ->
                        if (t.roomId == null) null else statuses?.let { it[t.id] ?: RentStatus(RentKind.Unbilled) }
                    }
                    val property = portfolio.property(propertyId)
                    val q = query.trim()
                    val shown = tenants.filter { t ->
                        (property == null || t.propertyId == property.id) &&
                            (status == null || statusOf(t)?.kind == status) &&
                            (q.isEmpty() || t.matches(q))
                    }
                    val filtersOn = (if (property != null) 1 else 0) + (if (status != null) 1 else 0)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { SearchBox(query, "Search name, mobile, email...") { query = it } }
                        FilterButton(filtersOn) { showFilters = true }
                    }
                    if (filtersOn > 0) {
                        AppliedTenantFilters(
                            property = property?.propertyName,
                            status = status?.label,
                            onClearProperty = { propertyId = null },
                            onClearStatus = { statusKey = null }
                        )
                    }

                    if (shown.isEmpty()) {
                        RfCard {
                            Text(
                                if (property != null && status == null && q.isEmpty()) "No tenants are assigned to a room in ${property.propertyName}."
                                else "No tenants match your search or filters.",
                                style = RfType.BodyMd, color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            shown.forEach { tenant ->
                                TenantCard(
                                    tenant = tenant,
                                    rent = portfolio.room(tenant.roomId)?.rent,
                                    status = statusOf(tenant),
                                    statusLoading = tenant.roomId != null && statuses == null && !statusesFailed,
                                    onOpen = { onOpenTenant(tenant.id) },
                                    onEdit = { onEditTenant(tenant.id) },
                                    onDelete = { deleting = tenant },
                                    onAssign = { onAssignRoom(tenant.id) },
                                    onRemove = { removing = tenant }
                                )
                            }
                        }
                        val scopeLabel = property?.let { "in ${it.propertyName}" } ?: "across all properties"
                        Text(
                            if (shown.size == tenants.size) "Showing all ${tenants.size} ${if (tenants.size == 1) "tenant" else "tenants"}\n$scopeLabel"
                            else "Showing ${shown.size} of ${tenants.size} tenants\n$scopeLabel",
                            style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // Room for the floating button over the last card
                    Box(Modifier.height(48.dp))

                    if (showFilters) {
                        TenantFilterSheet(
                            properties = portfolio.properties,
                            tenants = tenants,
                            statusOf = statusOf,
                            propertyId = property?.id,
                            status = status,
                            onApply = { p, s ->
                                propertyId = p
                                statusKey = s?.key
                                showFilters = false
                            },
                            onDismiss = { showFilters = false }
                        )
                    }
                }
            }
        }
        if (hasTenants) Fab("Add tenant", Lucide.UserPlus, onAddTenant, Modifier.align(Alignment.BottomEnd))
    }

    removing?.let { tenant ->
        val where = tenant.room?.roomNumber?.let { "Room $it" } ?: "their room"
        ConfirmSheet(
            icon = Lucide.LogOut,
            title = "Remove ${tenant.fullName} from $where?",
            message = "The room gets their place back. ${tenant.fullName}’s account and bills are kept, and you can give them a room again later.",
            confirmLabel = "Remove from room",
            busy = busy,
            onConfirm = {
                busy = true
                scope.launch {
                    LandlordStore.assignRoom(tenant.id, null).announce("${tenant.fullName} removed from $where")
                    busy = false
                    removing = null
                }
            },
            onDismiss = { removing = null }
        )
    }
    deleting?.let { tenant ->
        ConfirmSheet(
            icon = Lucide.Trash2,
            title = "Delete ${tenant.fullName}?",
            message = "Their account is removed and they can no longer sign in." +
                (tenant.room?.roomNumber?.let { " Room $it gets their place back." } ?: "") + " This can’t be undone.",
            confirmLabel = "Delete tenant",
            busy = busy,
            onConfirm = {
                busy = true
                scope.launch {
                    LandlordStore.deleteTenant(tenant.id).announce("Tenant deleted")
                    busy = false
                    deleting = null
                }
            },
            onDismiss = { deleting = null }
        )
    }
}

/* ------------------------------ Filters ------------------------------ */

/** The property and status that are on, as chips that take one off, then "Clear all". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppliedTenantFilters(property: String?, status: String?, onClearProperty: () -> Unit, onClearStatus: () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (property != null) AppliedChip(property, onClearProperty)
        if (status != null) AppliedChip(status, onClearStatus)
        Text(
            "Clear all", style = RfType.LabelSm.copy(fontWeight = FontWeight.ExtraBold), color = Rf.Error,
            modifier = Modifier.pressable({
                onClearProperty()
                onClearStatus()
            }).padding(horizontal = 4.dp, vertical = 5.dp)
        )
    }
}

/**
 * The web's phone filter sheet: a property and a rent status, applied together.
 * Each choice shows how many tenants it keeps; the status counts follow the property picked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TenantFilterSheet(
    properties: List<PropertyDto>,
    tenants: List<TenantDto>,
    statusOf: (TenantDto) -> RentStatus?,
    propertyId: String?,
    status: RentKind?,
    onApply: (String?, RentKind?) -> Unit,
    onDismiss: () -> Unit
) {
    var draftProperty by remember { mutableStateOf(propertyId) }
    var draftStatus by remember { mutableStateOf(status) }
    val inProperty = tenants.filter { draftProperty == null || it.propertyId == draftProperty }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Rf.Lowest,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filters", style = RfType.HeadlineSm.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface, modifier = Modifier.weight(1f))
                Text(
                    "Reset", style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.Primary,
                    modifier = Modifier.pressable({
                        draftProperty = null
                        draftStatus = null
                    }).padding(4.dp)
                )
            }
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SheetHeading("Property")
                SheetOption("All Properties", tenants.size, draftProperty == null) { draftProperty = null }
                properties.forEach { p ->
                    SheetOption(p.propertyName, tenants.count { it.propertyId == p.id }, draftProperty == p.id) { draftProperty = p.id }
                }
                SheetHeading("Rent status")
                SheetOption("All", inProperty.size, draftStatus == null) { draftStatus = null }
                RentKind.entries.forEach { kind ->
                    SheetOption(kind.label, inProperty.count { statusOf(it)?.kind == kind }, draftStatus == kind) { draftStatus = kind }
                }
            }
            PrimaryButton("Apply filters", onClick = { onApply(draftProperty, draftStatus) }, showArrow = false, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun SheetHeading(text: String) {
    Box(Modifier.padding(top = 10.dp, bottom = 2.dp)) { Caps(text) }
}

/** A radio row, as in the web's sheet, with how many tenants the choice keeps. */
@Composable
private fun SheetOption(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().pressable(onClick, pressScale = 0.98f).clip(shape)
            .background(if (selected) Rf.PrimaryFixed else Color.Transparent)
            .border(1.5.dp, if (selected) Rf.Primary else Rf.OutlineVariant, shape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            Modifier.size(18.dp).clip(CircleShape)
                .background(if (selected) Rf.Primary else Color.Transparent)
                .border(2.dp, if (selected) Rf.Primary else Rf.OutlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
        }
        Text(label, style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface, modifier = Modifier.weight(1f))
        Text("$count", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurfaceVariant)
    }
}

/* ------------------------------ Tenant card ------------------------------ */

/** A status pill's words and colours, as the web's StatusPill and paymentLabel. */
private data class StatusLook(val text: String, val background: Color, val content: Color)

@Composable
private fun statusLook(tenant: TenantDto, status: RentStatus?): StatusLook {
    val advance = status != null && status.advance > 0 && (status.kind == RentKind.Paid || status.kind == RentKind.Unbilled)
    return when {
        status == null -> if (tenant.isActive) StatusLook("Active", Rf.MintSoft, Rf.Secondary) else StatusLook("Inactive", Rf.Low, Rf.OnSurfaceVariant)
        status.kind == RentKind.Overdue -> StatusLook("${inr(status.amount)} overdue", Rf.ErrorContainer, Rf.Error)
        status.kind == RentKind.Due -> StatusLook("${inr(status.amount)} due", Rf.AmberSoft, Rf.Amber)
        advance -> StatusLook(
            "${if (status.kind == RentKind.Paid) "Paid" else "Not billed"} · ${inr(status.advance)} advance", Rf.PrimaryFixed, Rf.Primary
        )
        status.kind == RentKind.Paid -> StatusLook("Paid", Rf.MintSoft, Rf.Secondary)
        else -> StatusLook("Not billed yet", Rf.Low, Rf.OnSurfaceVariant)
    }
}

/** The web's phone TenantCard; tapping the card opens the tenant. */
@Composable
private fun TenantCard(
    tenant: TenantDto,
    rent: Double?,
    status: RentStatus?,
    statusLoading: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAssign: () -> Unit,
    onRemove: () -> Unit
) {
    val overdue = status?.kind == RentKind.Overdue
    val room = tenant.room?.takeIf { tenant.roomId != null }
    val divider = if (overdue) Rf.ErrorContainer else Rf.Low
    RfCard(
        padding = PaddingValues(14.dp),
        spacing = 12.dp,
        shape = RoundedCornerShape(18.dp),
        color = if (overdue) Rf.ErrorContainer.copy(alpha = 0.3f).compositeOn(Rf.Lowest) else Rf.Lowest,
        onClick = onOpen
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            CardAvatar(tenant, inRoom = room != null, overdue = overdue)
            Column(Modifier.weight(1f)) {
                Text(
                    tenant.fullName, style = RfType.BodyLg.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                mobileLabel(tenant.mobile)?.let { ContactLine(Lucide.Phone, it) }
                tenant.email.takeIf(String::isNotBlank)?.let { ContactLine(Lucide.Mail, it) }
            }
            RoomMenu(tenant, inRoom = room != null, onAssign = onAssign, onRemove = onRemove)
        }

        Column {
            Caps("Room / Unit")
            if (room == null) {
                CardTag(Lucide.DoorOpen, "Room Unassigned", Rf.PrimaryFixed, Rf.Primary, Modifier.padding(top = 6.dp))
                Text("No property assigned", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, modifier = Modifier.padding(top = 7.dp))
            } else {
                CardTag(
                    Lucide.Building2, "Room ${room.roomNumber}" + (room.roomSize?.takeIf(String::isNotBlank)?.let { " • $it" } ?: ""),
                    Rf.MintSoft, Rf.Secondary, Modifier.padding(top = 6.dp)
                )
                tenant.property?.let { p ->
                    Text(p.propertyName, style = RfType.BodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface, modifier = Modifier.padding(top = 7.dp))
                }
                val address = listOfNotNull(tenant.property?.address, tenant.property?.city).filter(String::isNotBlank).joinToString(", ")
                Text(
                    listOf(floorLabel(room.floor), address).filter(String::isNotBlank).joinToString(" • "),
                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
                )
            }
        }

        Column {
            Caps("Monthly Rent")
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                if (room != null && rent != null) {
                    Text(inr(rent), style = RfType.HeadlineSm.copy(fontWeight = FontWeight.ExtraBold), color = Rf.Primary)
                    Text("/ mo", style = RfType.LabelSm.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
                } else {
                    Text("—", style = RfType.HeadlineSm.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = divider)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Caps("Payment Status (${YearMonth.now().label()})")
                // The pill pulses until the bills arrive, so it never shows a status that then changes
                FadeIn(!statusLoading, placeholder = { Skeleton(Modifier.size(width = 96.dp, height = 22.dp), radius = 11.dp) }) {
                    val look = statusLook(tenant, status)
                    Pill(
                        look.text, background = look.background, content = look.content, dot = true,
                        style = RfType.LabelSm.copy(fontWeight = FontWeight.ExtraBold), horizontal = 9.dp, vertical = 4.dp
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Caps("Room Type")
                Text(
                    if (room == null) "Not assigned to a room" else roomTypeLabel(room.occupancyType),
                    style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface
                )
                apiDate(tenant.createdAt)?.let {
                    Text("Added ${it.shortLabel()}", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = divider)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            CardAction(Lucide.Eye, "View", Rf.MintSoft, Rf.Secondary, onOpen, Modifier.weight(1f))
            CardAction(Lucide.Pencil, "Edit", Rf.AmberSoft, Rf.Amber, onEdit, Modifier.weight(1f))
            CardAction(Lucide.Trash2, "Delete", Rf.ErrorContainer, Rf.Error, onDelete, Modifier.weight(1f))
        }
    }
}

/** A colour over a solid one, so a tinted card stays opaque. */
private fun Color.compositeOn(base: Color): Color = Color(
    red = red * alpha + base.red * (1 - alpha),
    green = green * alpha + base.green * (1 - alpha),
    blue = blue * alpha + base.blue * (1 - alpha)
)

/** Rounded-square photo with the web's dot: green in a room, amber no room, red overdue. */
@Composable
private fun CardAvatar(tenant: TenantDto, inRoom: Boolean, overdue: Boolean) {
    val (background, content, dot) = when {
        !inRoom -> Triple(Rf.PrimaryFixed, Rf.Primary, Color(0xFFF59E0B))
        overdue -> Triple(Rf.ErrorContainer, Rf.Error, Rf.Error)
        else -> Triple(Rf.MintSoft, Rf.Secondary, Color(0xFF16A34A))
    }
    Box {
        PersonAvatar(
            tenant.fullName, tenant.profilePic, size = 46.dp, background = background, content = content,
            shape = RoundedCornerShape(14.dp)
        )
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 3.dp, y = 3.dp).size(13.dp).clip(CircleShape)
                .background(Rf.Lowest).padding(2.5.dp).clip(CircleShape).background(dot)
        )
    }
}

@Composable
private fun ContactLine(icon: LucideIcon, text: String) {
    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LIcon(icon, size = 12.dp, tint = Rf.OnSurfaceVariant)
        Text(text, style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CardTag(icon: LucideIcon, text: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(8.dp)).background(background).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        LIcon(icon, size = 14.dp, tint = content)
        Text(text, style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = content, maxLines = 1)
    }
}

@Composable
private fun CardAction(icon: LucideIcon, label: String, background: Color, content: Color, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier.height(40.dp).pressable(onClick, pressScale = 0.97f).clip(RoundedCornerShape(12.dp)).background(background),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LIcon(icon, size = 16.dp, tint = content)
        Text(label, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = content)
    }
}

/** ⋮: give the tenant a room, or move them or take them out of theirs; and call them. */
@Composable
private fun RoomMenu(tenant: TenantDto, inRoom: Boolean, onAssign: () -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val digits = mobileDigits(tenant.mobile)
    Box {
        Box(
            Modifier.size(32.dp).pressable({ open = true }).clip(RoundedCornerShape(10.dp)).background(if (open) Rf.Low else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            LIcon(Lucide.EllipsisVertical, size = 18.dp, tint = Rf.OnSurfaceVariant, contentDescription = "More actions for ${tenant.fullName}")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(Rf.Lowest)) {
            if (inRoom) {
                MenuItem(Lucide.ArrowLeftRight, "Move to another room", Rf.OnSurface) { open = false; onAssign() }
                MenuItem(Lucide.LogOut, "Remove from room", Rf.Error) { open = false; onRemove() }
            } else {
                MenuItem(Lucide.DoorOpen, "Assign to a room", Rf.OnSurface) { open = false; onAssign() }
            }
            if (digits.isNotEmpty()) {
                HorizontalDivider(thickness = 1.dp, color = Rf.Low, modifier = Modifier.padding(vertical = 4.dp))
                MenuItem(Lucide.Phone, "Call tenant", Rf.OnSurface) {
                    open = false
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91$digits")))
                }
            }
        }
    }
}

@Composable
private fun MenuItem(icon: LucideIcon, label: String, color: Color, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = color) },
        leadingIcon = { LIcon(icon, size = 16.dp, tint = if (color == Rf.Error) Rf.Error else Rf.OnSurfaceVariant) },
        onClick = onClick
    )
}
