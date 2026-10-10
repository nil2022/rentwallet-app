package com.thebackendguy.rentflow.ui.screens.landlord

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.thebackendguy.rentflow.data.apiDate
import com.thebackendguy.rentflow.data.apiMonth
import com.thebackendguy.rentflow.data.floorLabel
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.label
import com.thebackendguy.rentflow.data.mobileDigits
import com.thebackendguy.rentflow.data.mobileLabel
import com.thebackendguy.rentflow.data.monthLabel
import com.thebackendguy.rentflow.data.occupancyLabel
import com.thebackendguy.rentflow.data.remote.BillDto
import com.thebackendguy.rentflow.data.remote.PropertyDto
import com.thebackendguy.rentflow.data.remote.RoomDto
import com.thebackendguy.rentflow.data.remote.TenantDto
import com.thebackendguy.rentflow.data.roomCapacity
import com.thebackendguy.rentflow.data.shortLabel
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.ConfirmSheet
import com.thebackendguy.rentflow.ui.components.FadeIn
import com.thebackendguy.rentflow.ui.components.PersonAvatar
import com.thebackendguy.rentflow.ui.components.Pill
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.Skeleton
import com.thebackendguy.rentflow.ui.components.Toasts
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
 * The web's phone tenant page (ViewTenant.jsx): who they are with their unit and
 * payment status, the room they live in, roommates, Payment History, contact
 * details and the tenancy actions.
 */

@Composable
fun TenantDetailsScreen(
    tenantId: String,
    shell: Shell,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAssignRoom: () -> Unit,
    onOpenProperty: (String) -> Unit,
    onOpenTenant: (String) -> Unit,
    onRecordPayment: (month: String?) -> Unit,
    onEditBill: (billId: String) -> Unit,
    onDeleted: () -> Unit
) {
    val state = portfolioState()
    val tenant = state.data?.tenant(tenantId)
    val scope = rememberCoroutineScope()
    var confirmRemove by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val (bills, billsError) = rememberTenantBills(tenantId)

    AppPage(topBar = { shell.TopBar(tenant?.fullName ?: "Tenant", onBack = onBack) }, bottomBar = { shell.BottomNav(3) }) {
        PortfolioContent(state) { portfolio ->
            if (tenant == null) {
                NotFound("Tenant", onBack)
                return@PortfolioContent
            }
            val roomId = tenant.roomId
            // Rent and deposit are set on the room
            val room = portfolio.room(roomId)
            val property = portfolio.property(tenant.propertyId ?: room?.propertyId)
            val roomName = tenant.room?.roomNumber ?: room?.roomNumber
            val firstName = tenant.fullName.trim().split(Regex("\\s+")).firstOrNull()?.ifBlank { null } ?: "This tenant"
            val roommates = roomId?.let { portfolio.occupants(it) }.orEmpty().filter { it.id != tenant.id }

            Hero(
                tenant, roomName, property, room,
                bills = bills,
                billsFailed = billsError != null,
                onRecordPayment = {
                    // A bill is made against the tenant's room, so a tenant without one can't be billed
                    if (roomId == null) Toasts.error("Assign $firstName to a room first. Bills are made against the room.")
                    else onRecordPayment(null)
                },
                onEdit = onEdit
            )
            RoomTenancyCard(tenant, room, property, roomName, firstName, roommates.size + 1, onOpenProperty, onAssignRoom)
            if (roomId != null) RoommatesCard(roommates, roomName, firstName, solo = roomCapacity(tenant.room?.occupancyType ?: room?.occupancyType) == 1, onOpenTenant)
            PaymentHistoryCard(bills, billsError, tenant.fullName, onRecord = onRecordPayment, onEditBill = onEditBill)
            ContactCard(tenant, onAddEmail = onEdit)
            TenancyActionsCard(
                inRoom = roomId != null, roomName = roomName, firstName = firstName,
                onChangeRoom = onAssignRoom, onAssign = onAssignRoom,
                onRemove = { confirmRemove = true }, onDelete = { confirmDelete = true }
            )

            if (confirmRemove && roomId != null) {
                val where = roomName?.let { "Room $it" } ?: "their room"
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
                            confirmRemove = false
                        }
                    },
                    onDismiss = { confirmRemove = false }
                )
            }
            if (confirmDelete) {
                ConfirmSheet(
                    icon = Lucide.Trash2,
                    title = "Delete ${tenant.fullName}?",
                    message = "Their account is removed and they can no longer sign in." +
                        (if (roomName != null) " Room $roomName gets their place back." else "") + " This can’t be undone.",
                    confirmLabel = "Delete tenant",
                    busy = busy,
                    onConfirm = {
                        busy = true
                        scope.launch {
                            val done = LandlordStore.deleteTenant(tenant.id).announce("Tenant deleted")
                            busy = false
                            confirmDelete = false
                            if (done) onDeleted()
                        }
                    },
                    onDismiss = { confirmDelete = false }
                )
            }
        }
    }
}

/* ------------------------------ Hero ------------------------------ */

/** How the second hero tile looks: icon, colours, the figure and the line under it. */
private data class PaymentTile(
    val icon: LucideIcon, val tint: Color, val tile: Color, val value: String, val sub: String,
    val alert: Boolean = false, val loading: Boolean = false
)

/**
 * This month's rent, or what is overdue from earlier months, as the web works it out
 * from the bills: overdue wins over due, due over advance, then paid, then not billed.
 */
@Composable
private fun paymentTile(bills: List<BillDto>?, failed: Boolean): PaymentTile {
    val now = YearMonth.now()
    val month = now.label()
    if (bills == null && failed) return PaymentTile(Lucide.Wallet, Rf.Primary, Rf.PrimaryFixed, "—", "Couldn’t load the bills")
    if (bills == null) return PaymentTile(Lucide.Wallet, Rf.Primary, Rf.PrimaryFixed, "", "", loading = true)
    val current = bills.find { it.monthYear == now.apiMonth() }
    val overdue = bills.filter { it.monthYear < now.apiMonth() && it.pendingAmount > 0 }
    val advance = bills.sumOf { it.advanceLeft }
    val overdueAmount = overdue.sumOf { it.pendingAmount }
    return when {
        overdueAmount > 0 -> PaymentTile(
            Lucide.TriangleAlert, Rf.Error, Rf.ErrorContainer, "${inr(overdueAmount)} overdue",
            "Unpaid since ${monthLabel(overdue.minOf { it.monthYear })}", alert = true
        )
        current != null && current.pendingAmount > 0 -> PaymentTile(
            Lucide.Clock, Rf.Amber, Rf.AmberSoft, "${inr(current.pendingAmount)} due",
            if (current.creditApplied > 0) "$month • ${inr(current.creditApplied)} advance already taken off" else "$month bill"
        )
        advance > 0 -> PaymentTile(
            Lucide.Wallet, Rf.Primary, Rf.PrimaryFixed, "${inr(advance)} advance",
            if (current != null) "Paid up. The next bill uses it first." else "No $month bill yet. It comes off that bill."
        )
        current != null -> PaymentTile(Lucide.CircleCheck, Rf.Secondary, Rf.MintSoft, "Paid", "$month • ${inr(current.paidAmount)} received")
        else -> PaymentTile(Lucide.Wallet, Rf.Primary, Rf.PrimaryFixed, "Not billed yet", "No $month bill")
    }
}

@Composable
private fun Hero(
    tenant: TenantDto,
    roomName: String?,
    property: PropertyDto?,
    room: RoomDto?,
    bills: List<BillDto>?,
    billsFailed: Boolean,
    onRecordPayment: () -> Unit,
    onEdit: () -> Unit
) {
    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(16.dp), spacing = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PersonAvatar(tenant.fullName, tenant.profilePic, size = 64.dp, online = tenant.isActive)
            Column(Modifier.weight(1f)) {
                Text(tenant.fullName, style = RfType.HeadlineSm.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActivePill(tenant.isActive)
                    apiDate(tenant.createdAt)?.let {
                        Text("Since ${it.shortLabel()}", style = RfType.LabelSm.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurfaceVariant)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ContactLine(Lucide.Phone, mobileLabel(tenant.mobile) ?: tenant.mobile.orEmpty(), muted = false)
            ContactLine(Lucide.Mail, tenant.email.ifBlank { "No email added" }, muted = tenant.email.isBlank())
        }
        // One tile per row, so the room and the payment status show in full
        if (tenant.roomId != null) {
            UnitTile(
                Lucide.DoorOpen, Rf.Primary, Rf.PrimaryFixed, "Assigned unit", "Room ${roomName.orEmpty()}",
                listOfNotNull(property?.propertyName, (tenant.room?.floor ?: room?.floor)?.let(::floorLabel)).joinToString(" • ")
            )
            val pay = paymentTile(bills, billsFailed)
            UnitTile(pay.icon, pay.tint, pay.tile, "Payment status", pay.value, pay.sub, valueColor = if (pay.alert) Rf.Error else null, loading = pay.loading)
        } else {
            UnitTile(Lucide.DoorOpen, Rf.Primary, Rf.PrimaryFixed, "Assigned unit", "Not assigned", "No room yet", empty = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.weight(1f).height(44.dp).pressable(onRecordPayment, pressScale = 0.97f).clip(RoundedCornerShape(12.dp)).background(Rf.Primary),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LIcon(Lucide.Plus, size = 16.dp, tint = Rf.OnPrimary)
                Text("Record Payment", style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnPrimary)
            }
            Box(
                Modifier.size(44.dp).pressable(onEdit, pressScale = 0.95f).clip(RoundedCornerShape(12.dp)).background(Rf.Low),
                contentAlignment = Alignment.Center
            ) {
                LIcon(Lucide.Pencil, size = 16.dp, tint = Rf.Primary, contentDescription = "Edit details")
            }
        }
    }
}

@Composable
private fun ActivePill(active: Boolean) {
    if (active) Pill("Active", Rf.MintSoft, Rf.Secondary, dot = true, style = RfType.LabelSm.copy(fontWeight = FontWeight.ExtraBold), vertical = 3.dp)
    else Pill("Inactive", Rf.Low, Rf.OnSurfaceVariant, style = RfType.LabelSm.copy(fontWeight = FontWeight.ExtraBold), vertical = 3.dp)
}

@Composable
private fun ContactLine(icon: LucideIcon, text: String, muted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LIcon(icon, size = 14.dp, tint = Rf.OnSurfaceVariant)
        Text(
            text, style = RfType.LabelMd.copy(fontWeight = if (muted) FontWeight.Medium else FontWeight.SemiBold),
            color = if (muted) Rf.OnSurfaceVariant else Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun UnitTile(
    icon: LucideIcon,
    tint: Color,
    tile: Color,
    label: String,
    value: String,
    sub: String,
    empty: Boolean = false,
    valueColor: Color? = null,
    loading: Boolean = false
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).then(if (empty) Modifier.dashedBorder(Rf.OutlineVariant) else Modifier.background(Rf.Low)).padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(tile), contentAlignment = Alignment.Center) {
            LIcon(icon, size = 18.dp, tint = tint)
        }
        Column(Modifier.weight(1f)) {
            Caps(label)
            FadeIn(
                !loading,
                placeholder = {
                    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Skeleton(Modifier.fillMaxWidth(0.58f).height(15.dp))
                        Skeleton(Modifier.fillMaxWidth(0.44f).height(10.dp), radius = 5.dp)
                    }
                }
            ) {
                Column {
                    Text(value, style = RfType.BodyMd.copy(fontWeight = FontWeight.ExtraBold), color = valueColor ?: Rf.OnSurface)
                    if (sub.isNotEmpty()) Text(sub, style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
                }
            }
        }
    }
}

/* ------------------------------ Cards ------------------------------ */

/** Icon tile, title and subtitle that open each card, with an optional [trailing] part. */
@Composable
private fun CardHead(icon: LucideIcon, tint: Color, tile: Color, title: String, sub: String?, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(tile), contentAlignment = Alignment.Center) {
            LIcon(icon, size = 17.dp, tint = tint)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = RfType.BodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
            if (sub != null) Text(sub, style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun RoomTenancyCard(
    tenant: TenantDto,
    room: RoomDto?,
    property: PropertyDto?,
    roomName: String?,
    firstName: String,
    occupants: Int,
    onOpenProperty: (String) -> Unit,
    onAssign: () -> Unit
) {
    val type = tenant.room?.occupancyType ?: room?.occupancyType
    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(16.dp), spacing = 12.dp) {
        CardHead(Lucide.DoorOpen, Rf.Primary, Rf.PrimaryFixed, "Room & Tenancy", "The room this tenant lives in") {
            if (tenant.roomId != null && type != null) {
                val capacity = roomCapacity(type)
                val (text, bg, fg) = when {
                    capacity == null -> Triple("$occupants ${if (occupants == 1) "tenant" else "tenants"} • shared", Rf.MintSoft, Rf.Secondary)
                    occupants >= capacity -> Triple("Full", Rf.ErrorContainer, Rf.Error)
                    else -> Triple("$occupants of $capacity filled", Rf.AmberSoft, Rf.Amber)
                }
                Pill("${occupancyLabel(type)} • $text", bg, fg, style = RfType.LabelSm.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold), vertical = 3.dp)
            }
        }
        if (tenant.roomId == null) {
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(Rf.PrimaryFixed), contentAlignment = Alignment.Center) {
                    LIcon(Lucide.DoorOpen, size = 24.dp, tint = Rf.Primary)
                }
                Text("$firstName isn’t in a room yet", style = RfType.BodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
                Text(
                    "Once you assign a room, its rent, deposit, floor and property show up here.",
                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center
                )
                Row(
                    Modifier.height(40.dp).pressable(onAssign).clip(RoundedCornerShape(11.dp)).background(Rf.Primary).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LIcon(Lucide.Plus, size = 15.dp, tint = Rf.OnPrimary)
                    Text("Assign Room", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.OnPrimary)
                }
            }
            return@RfCard
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Monthly rent", room?.let { inr(it.rent) } ?: "—", "Set on the room", accent = true, Modifier.weight(1f))
            Metric("Security deposit", room?.let { inr(it.securityDeposit) } ?: "—", "Set on the room", accent = false, Modifier.weight(1f))
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Floor", (tenant.room?.floor ?: room?.floor)?.let(::floorLabel) ?: "—", "Room ${roomName.orEmpty()}", accent = false, Modifier.weight(1f))
            Metric("Room size", (tenant.room?.roomSize ?: room?.roomSize)?.ifBlank { null } ?: "—", type?.let(::occupancyLabel).orEmpty(), accent = false, Modifier.weight(1f))
        }
        if (property != null) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Rf.Low, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LIcon(Lucide.Building2, size = 18.dp, tint = Rf.Primary)
                Column(Modifier.weight(1f)) {
                    Text(property.propertyName, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (property.address.isNotBlank()) Text(property.address, style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                LinkText("View property") { onOpenProperty(property.id) }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, sub: String, accent: Boolean, modifier: Modifier) {
    Column(modifier.fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(if (accent) Rf.PrimaryFixed else Rf.Low).padding(10.dp)) {
        Caps(label)
        Text(value, style = RfType.BodyMd.copy(fontWeight = FontWeight.ExtraBold), color = if (accent) Rf.Primary else Rf.OnSurface, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        if (sub.isNotEmpty()) Text(sub, style = RfType.LabelSm.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Row(Modifier.pressable(onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(text, style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Primary)
        LIcon(Lucide.ArrowRight, size = 13.dp, tint = Rf.Primary)
    }
}

@Composable
private fun RoommatesCard(roommates: List<TenantDto>, roomName: String?, firstName: String, solo: Boolean, onView: (String) -> Unit) {
    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(16.dp), spacing = 10.dp) {
        CardHead(Lucide.Users, Rf.Secondary, Rf.MintSoft, "Roommates", "Others sharing Room ${roomName.orEmpty()}") {
            if (roommates.isNotEmpty()) {
                Pill(
                    "${roommates.size} ${if (roommates.size == 1) "roommate" else "roommates"}", Rf.Low, Rf.OnSurfaceVariant,
                    style = RfType.LabelSm.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold), vertical = 3.dp
                )
            }
        }
        if (roommates.isEmpty()) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Rf.Low).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LIcon(Lucide.Info, size = 18.dp, tint = Rf.OnSurfaceVariant)
                Text(
                    if (solo) "This room fits one tenant, so $firstName is the only occupant." else "$firstName is the only one in this room right now.",
                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
                )
            }
        } else {
            roommates.forEachIndexed { index, person ->
                if (index > 0) HorizontalDivider(thickness = 1.dp, color = Rf.Low)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Rf.MintSoft), contentAlignment = Alignment.Center) {
                        Text(initials(person.fullName), style = RfType.LabelSm.copy(fontWeight = FontWeight.ExtraBold), color = Rf.Secondary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(person.fullName, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(mobileLabel(person.mobile) ?: person.mobile.orEmpty(), style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
                    }
                    LinkText("View") { onView(person.id) }
                }
            }
        }
    }
}

@Composable
private fun ContactCard(tenant: TenantDto, onAddEmail: () -> Unit) {
    val context = LocalContext.current
    val digits = mobileDigits(tenant.mobile)
    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(16.dp), spacing = 4.dp) {
        CardHead(Lucide.User, Rf.Secondary, Rf.MintSoft, "Contact & Account", null) { ActivePill(tenant.isActive) }
        InfoLine(Lucide.Phone, "Mobile", mobileLabel(tenant.mobile) ?: tenant.mobile.orEmpty(), first = true) {
            if (digits.isNotEmpty()) {
                MiniButton(Lucide.Phone, "Call", Rf.Primary) { launch(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91$digits"))) }
                MiniButton(Lucide.MessageCircle, "WhatsApp", Color(0xFF16A34A)) {
                    launch(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91$digits")))
                }
            }
        }
        InfoLine(Lucide.Mail, "Email", tenant.email.ifBlank { "Not added" }, muted = tenant.email.isBlank()) {
            if (tenant.email.isNotBlank()) {
                MiniButton(Lucide.Mail, "Send email", Rf.Primary) { launch(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${tenant.email}"))) }
            } else {
                Text("Add", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.Primary, modifier = Modifier.pressable(onAddEmail).padding(6.dp))
            }
        }
        InfoLine(Lucide.LogIn, "Sign-in", if (tenant.email.isNotBlank()) "Email or mobile number" else "Mobile number only")
        InfoLine(Lucide.Clock, "Added on", apiDate(tenant.createdAt)?.shortLabel() ?: "—")
    }
}

private fun launch(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toasts.error("No app on this phone can open that.")
    }
}

@Composable
private fun InfoLine(icon: LucideIcon, label: String, value: String, first: Boolean = false, muted: Boolean = false, actions: @Composable () -> Unit = {}) {
    if (!first) HorizontalDivider(thickness = 1.dp, color = Rf.Low)
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LIcon(icon, size = 16.dp, tint = Rf.Outline)
        Column(Modifier.weight(1f)) {
            Caps(label)
            Text(
                value, style = RfType.LabelMd.copy(fontWeight = if (muted) FontWeight.Medium else FontWeight.Bold),
                color = if (muted) Rf.OnSurfaceVariant else Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { actions() }
    }
}

@Composable
private fun MiniButton(icon: LucideIcon, description: String, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(32.dp).pressable(onClick).clip(RoundedCornerShape(9.dp)).background(Rf.Low), contentAlignment = Alignment.Center) {
        LIcon(icon, size = 15.dp, tint = tint, contentDescription = description)
    }
}

private enum class ActionTone { Indigo, Amber, Red }

@Composable
private fun TenancyActionsCard(
    inRoom: Boolean,
    roomName: String?,
    firstName: String,
    onChangeRoom: () -> Unit,
    onAssign: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit
) {
    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(16.dp), spacing = 10.dp) {
        CardHead(Lucide.ShieldAlert, Rf.Error, Rf.ErrorContainer, "Tenancy Actions", "Room changes and removal")
        if (inRoom) {
            ActionBlock(
                ActionTone.Indigo, "Change room", "Move $firstName to another room in any of your properties.",
                "Move to another room", Lucide.ArrowLeftRight, onChangeRoom
            )
            ActionBlock(
                ActionTone.Amber, "Remove from room", "Frees Room ${roomName.orEmpty()}. $firstName stays in your directory.",
                "Remove from Room ${roomName.orEmpty()}", Lucide.LogOut, onRemove
            )
        } else {
            ActionBlock(
                ActionTone.Indigo, "Assign room", "Pick a property and room for $firstName. Rent comes from the room.",
                "Assign Room", Lucide.Plus, onAssign
            )
        }
        ActionBlock(
            ActionTone.Red, "Delete tenant", "Permanently removes $firstName from your directory. This can’t be undone.",
            "Delete Tenant", Lucide.Trash2, onDelete
        )
    }
}

@Composable
private fun ActionBlock(tone: ActionTone, title: String, text: String, button: String, icon: LucideIcon, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val background = when (tone) {
        ActionTone.Indigo -> Color.Transparent
        ActionTone.Amber -> Rf.AmberSoft
        ActionTone.Red -> Rf.ErrorContainer.copy(alpha = 0.45f)
    }
    Column(
        Modifier.fillMaxWidth().clip(shape).background(background)
            .then(if (tone == ActionTone.Indigo) Modifier.border(1.dp, Rf.Low, shape) else Modifier).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
        Text(text, style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
        val buttonShape = RoundedCornerShape(10.dp)
        val (fill, content) = when (tone) {
            ActionTone.Indigo -> Rf.Lowest to Rf.Primary
            ActionTone.Amber -> Rf.Amber to Color.White
            ActionTone.Red -> Rf.Error to Rf.OnError
        }
        Row(
            Modifier.fillMaxWidth().height(38.dp).pressable(onClick, pressScale = 0.97f).clip(buttonShape).background(fill)
                .then(if (tone == ActionTone.Indigo) Modifier.border(1.dp, Rf.Primary, buttonShape) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LIcon(icon, size = 15.dp, tint = content)
            Text(button, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = content)
        }
    }
}
