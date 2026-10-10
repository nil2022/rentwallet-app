package com.thebackendguy.rentflow.ui.screens.landlord

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.Photo
import com.thebackendguy.rentflow.data.apiDate
import com.thebackendguy.rentflow.data.floorLabel
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.isValidMobile
import com.thebackendguy.rentflow.data.mobileDigits
import com.thebackendguy.rentflow.data.mobileLabel
import com.thebackendguy.rentflow.data.monthLabel
import com.thebackendguy.rentflow.data.occupancyLabel
import com.thebackendguy.rentflow.data.remote.TenantDto
import com.thebackendguy.rentflow.data.shortLabel
import com.thebackendguy.rentflow.ui.components.ActionButton
import com.thebackendguy.rentflow.ui.components.AlertCard
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.Caption
import com.thebackendguy.rentflow.ui.components.ConfirmSheet
import com.thebackendguy.rentflow.ui.components.Detail
import com.thebackendguy.rentflow.ui.components.DetailRows
import com.thebackendguy.rentflow.ui.components.EmptyState
import com.thebackendguy.rentflow.ui.components.Fab
import com.thebackendguy.rentflow.ui.components.FormBar
import com.thebackendguy.rentflow.ui.components.FormSection
import com.thebackendguy.rentflow.ui.components.IconDetail
import com.thebackendguy.rentflow.ui.components.IconRows
import com.thebackendguy.rentflow.ui.components.IconTile
import com.thebackendguy.rentflow.ui.components.PersonAvatar
import com.thebackendguy.rentflow.ui.components.PhotoViewer
import com.thebackendguy.rentflow.ui.components.Pill
import com.thebackendguy.rentflow.ui.components.PrimaryButton
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.RfTextField
import com.thebackendguy.rentflow.ui.components.SectionHead
import com.thebackendguy.rentflow.ui.components.Segmented
import com.thebackendguy.rentflow.ui.components.SwitchRow
import com.thebackendguy.rentflow.ui.components.Toasts
import com.thebackendguy.rentflow.ui.components.photoChooser
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.components.rememberPhotoLoad
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.screens.auth.optionalEmailError
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue

/* ------------------------------ Demo rent status ------------------------------ */

/**
 * This month's rent status. The API has no payments for landlords yet, so it's
 * made up from the tenant's id, the same way every time. Replace with the
 * payments API when it exists.
 */
internal data class DemoRent(val paid: Boolean, val label: String, val lastPayment: String)

private val DayMonth = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)

internal fun demoRent(tenant: TenantDto, rent: Double): DemoRent {
    val today = LocalDate.now()
    val due = today.withDayOfMonth(5)
    val paid = tenant.id.hashCode().absoluteValue % 3 != 0
    val paidOn = due.minusDays((tenant.id.hashCode().absoluteValue % 4 + 1).toLong())
    return DemoRent(
        paid = paid,
        label = if (paid) "Paid ${paidOn.format(DayMonth)}" else "Due ${due.format(DayMonth)}",
        lastPayment = "${inr(rent)} on ${(if (paid) paidOn else paidOn.minusMonths(1)).shortLabel()}"
    )
}

/* ------------------------------ Tenants list ------------------------------ */

@Composable
fun TenantsScreen(shell: Shell, onOpenTenant: (String) -> Unit, onAddTenant: () -> Unit, onAssignRoom: (String) -> Unit) {
    val state = portfolioState()
    LaunchedEffect(Unit) { LandlordStore.refresh() }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val hasTenants = !state.data?.tenants.isNullOrEmpty()

    Box(Modifier.fillMaxSize()) {
        AppPage(
            topBar = { shell.TopBar("Tenants", chip = state.data?.let { "${it.properties.size} properties" }) },
            bottomBar = { shell.BottomNav(3) }
        ) {
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
                SearchBox(query, "Search by name, email or phone") { query = it }

                // Rent alerts stay sample data until the payments API exists
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHead("Tenant alerts") { Caption("2 open") }
                    AlertCard(Lucide.TriangleAlert, "Follow up needed", "Two tenants have upcoming or overdue rent.", Rf.ErrorContainer, Rf.Error)
                    AlertCard(Lucide.Banknote, "Withdrawal processing", "Bank transfer of ₹15,000 is on its way.", Rf.AmberSoft, Rf.Amber)
                }

                // Rent is set on the room
                val rentOf = { t: TenantDto -> portfolio.room(t.roomId)?.rent ?: 0.0 }
                val housed = tenants.filter { it.roomId != null }
                val paid = housed.count { demoRent(it, rentOf(it)).paid }
                val noRoom = tenants.size - housed.size
                Column {
                    SectionHead("All tenants") { Caption("${LocalDate.now().monthLabel().substringBefore(' ')} rent") }
                    Segmented(
                        options = listOf("All (${tenants.size})", "Paid ($paid)", "Due (${housed.size - paid})", "No room ($noRoom)"),
                        selected = filter,
                        onSelect = { filter = it },
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    val q = query.trim()
                    val shown = tenants.filter { t ->
                        val matchesFilter = when (filter) {
                            1 -> t.roomId != null && demoRent(t, rentOf(t)).paid
                            2 -> t.roomId != null && !demoRent(t, rentOf(t)).paid
                            3 -> t.roomId == null
                            else -> true
                        }
                        matchesFilter && (q.isEmpty() || listOfNotNull(t.fullName, t.email, t.mobile, t.property?.propertyName, t.room?.roomNumber)
                            .any { it.contains(q, ignoreCase = true) })
                    }
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (shown.isEmpty()) {
                            Text("No tenants to show here.", style = RfType.BodyMd, color = Rf.OnSurfaceVariant, modifier = Modifier.padding(4.dp))
                        }
                        shown.forEach { tenant ->
                            TenantCard(tenant, rentOf(tenant), onOpen = { onOpenTenant(tenant.id) }, onAssign = { onAssignRoom(tenant.id) })
                        }
                    }
                }
                Box(Modifier.height(48.dp))
            }
        }
        if (hasTenants) Fab("Add tenant", Lucide.UserPlus, onAddTenant, Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun TenantCard(tenant: TenantDto, rentAmount: Double, onOpen: () -> Unit, onAssign: () -> Unit) {
    val phone = mobileLabel(tenant.mobile) ?: tenant.email
    RfCard(padding = PaddingValues(14.dp), spacing = 10.dp, onClick = onOpen) {
        if (tenant.roomId == null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PersonAvatar(tenant.fullName, tenant.profilePic, size = 40.dp, background = Rf.Low, content = Rf.OnSurfaceVariant)
                    Column {
                        Text(tenant.fullName, style = RfType.BodyLg.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Not in a room yet", style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
                    }
                }
                Pill("No room", background = Rf.Low, content = Rf.OnSurfaceVariant)
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Rf.Low).padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LIcon(Lucide.Phone, size = 14.dp, tint = Rf.Primary)
                    Text(phone, style = RfType.LabelSm, color = Rf.Outline)
                }
                Row(Modifier.pressable(onAssign), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Assign room", style = RfType.LabelMd, color = Rf.Primary)
                    LIcon(Lucide.ArrowRight, size = 14.dp, tint = Rf.Primary)
                }
            }
            return@RfCard
        }

        val rent = demoRent(tenant, rentAmount)
        val tint = if (rent.paid) Rf.Secondary else Rf.Error
        val soft = if (rent.paid) Rf.MintSoft else Rf.ErrorContainer
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PersonAvatar(tenant.fullName, tenant.profilePic, size = 40.dp, background = soft, content = tint)
                Column {
                    Text(tenant.fullName, style = RfType.BodyLg.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(tenant.property?.propertyName, tenant.room?.roomNumber?.let { "Room $it" }).joinToString(" • "),
                        style = RfType.LabelSm, color = Rf.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Pill(if (rent.paid) "Paid" else "Due", background = soft, content = tint, dot = true)
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Rf.Low).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Monthly rent", style = RfType.LabelSm, color = Rf.Outline)
                Text(inr(rentAmount), style = RfType.HeadlineSm.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface)
            }
            Text(rent.label, style = RfType.LabelMd, color = tint)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                LIcon(Lucide.Phone, size = 14.dp, tint = Rf.Primary)
                Text(phone, style = RfType.LabelSm, color = Rf.Outline)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("View details", style = RfType.LabelSm, color = Rf.Primary)
                LIcon(Lucide.ChevronRight, size = 14.dp, tint = Rf.Primary)
            }
        }
    }
}

/* ------------------------------ Tenant details ------------------------------ */

@Composable
fun TenantDetailsScreen(
    tenantId: String,
    shell: Shell,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAssignRoom: () -> Unit,
    onOpenRoom: (String) -> Unit,
    onRecordPayment: (month: String?) -> Unit,
    onEditBill: (billId: String) -> Unit,
    onDeleted: () -> Unit
) {
    val state = portfolioState()
    val tenant = state.data?.tenant(tenantId)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmRemove by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    AppPage(topBar = { shell.TopBar(tenant?.fullName ?: "Tenant", onBack = onBack) }, bottomBar = { shell.BottomNav(3) }) {
        PortfolioContent(state) { portfolio ->
            if (tenant == null) {
                NotFound("Tenant", onBack)
                return@PortfolioContent
            }
            val roomId = tenant.roomId
            // Rent and deposit are set on the room
            val room = portfolio.room(roomId)
            val roomName = tenant.room?.roomNumber ?: room?.roomNumber
            val since = apiDate(tenant.createdAt)?.monthLabel()

            RfCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    PersonAvatar(tenant.fullName, tenant.profilePic, size = 56.dp)
                    Column(Modifier.weight(1f)) {
                        Text(tenant.fullName, style = RfType.HeadlineSm, color = Rf.OnSurface)
                        Text(if (since != null) "Tenant since $since" else "Tenant", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
                    }
                    if (tenant.isActive) Pill("Active", background = Rf.MintSoft, content = Rf.Secondary)
                    else Pill("Inactive", background = Rf.Low, content = Rf.OnSurfaceVariant)
                }
                Box(Modifier.padding(top = 14.dp)) {
                    IconRows(
                        listOf(
                            IconDetail(Lucide.Phone, "Mobile", mobileLabel(tenant.mobile) ?: "Not added"),
                            IconDetail(Lucide.Mail, "Email", tenant.email.ifBlank { "Not added" })
                        )
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("Call", Lucide.Phone, Rf.Container, Rf.Primary, Modifier.weight(1f)) {
                    val number = mobileDigits(tenant.mobile)
                    if (number.isNotEmpty()) context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91$number")))
                }
                ActionButton("Edit", Lucide.Pencil, Rf.Container, Rf.Primary, Modifier.weight(1f), onClick = onEdit)
            }

            if (roomId != null) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHead("Room") { Pill("Active", background = Rf.MintSoft, content = Rf.Secondary) }
                    RfCard {
                        DetailRows(
                            listOfNotNull(
                                tenant.property?.let { Detail("Property", it.propertyName) },
                                roomName?.let { Detail("Room", listOfNotNull(it, (tenant.room?.floor ?: room?.floor)?.let(::floorLabel)).joinToString(" • ")) },
                                (tenant.room?.occupancyType ?: room?.occupancyType)?.let { Detail("Occupancy", occupancyLabel(it)) },
                                room?.let { Detail("Monthly rent", inr(it.rent)) },
                                room?.let { Detail("Security deposit", inr(it.securityDeposit)) }
                            )
                        )
                        Row(
                            Modifier.padding(top = 14.dp).pressable({ onOpenRoom(roomId) }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Open room", style = RfType.LabelMd, color = Rf.Primary)
                            LIcon(Lucide.ArrowRight, size = 14.dp, tint = Rf.Primary)
                        }
                    }
                }

                ActionButton("Send reminder", Lucide.Send, Rf.Secondary, Rf.OnSecondary, Modifier.fillMaxWidth(), doneLabel = "Reminder sent")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton("Change room", Lucide.DoorOpen, Rf.Container, Rf.Primary, Modifier.weight(1f), onClick = onAssignRoom)
                    ActionButton("Remove from room", Lucide.LogOut, Rf.ErrorContainer, Rf.Error, Modifier.weight(1f)) { confirmRemove = true }
                }
            } else {
                RfCard {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconTile(Lucide.DoorOpen, background = Rf.PrimaryFixed, tint = Rf.Primary, size = 48.dp, radius = 14.dp, iconSize = 24.dp)
                        Text("Not in a room yet", style = RfType.BodyLg.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                        Text("Give them a room with space. They pay the rent set on it.", style = RfType.BodySm, color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    PrimaryButton("Assign a room", onClick = onAssignRoom, showArrow = false, modifier = Modifier.padding(top = 16.dp))
                }
            }

            // Every month's bill, from the bills API (the web's Payment History card)
            PaymentHistoryCard(tenant.id, tenant.fullName, onRecord = onRecordPayment, onEditBill = onEditBill)

            Text(
                "Delete tenant",
                style = RfType.LabelMd, color = Rf.Error, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().pressable({ confirmDelete = true }).padding(vertical = 6.dp)
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

/* ------------------------------ Edit tenant ------------------------------ */

@Composable
fun TenantFormScreen(tenantId: String, shell: Shell, onBack: () -> Unit, onSaved: () -> Unit) {
    val state = portfolioState()
    val tenant = state.data?.tenant(tenantId)
    if (tenant == null) {
        AppPage(topBar = { shell.TopBar("Edit Tenant", onBack = onBack) }) {
            PortfolioContent(state) { NotFound("Tenant", onBack) }
        }
        return
    }
    EditTenantForm(tenant, shell, onBack, onSaved)
}

@Composable
private fun EditTenantForm(tenant: TenantDto, shell: Shell, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(tenant.fullName) }
    var mobile by rememberSaveable { mutableStateOf(mobileDigits(tenant.mobile)) }
    var email by rememberSaveable { mutableStateOf(tenant.email) }
    var active by rememberSaveable { mutableStateOf(tenant.isActive) }
    var picked by rememberSaveable { mutableStateOf<Uri?>(null) }
    var removed by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val nameErr = if (submitted && name.trim().length < 2) "Enter the tenant’s full name" else null
    val mobileErr = if (submitted && !isValidMobile(mobile)) "Enter a valid 10-digit mobile number" else null
    val emailErr = if (submitted) optionalEmailError(email) else null

    fun save() {
        submitted = true
        if (name.trim().length < 2 || !isValidMobile(mobile) || optionalEmailError(email) != null) return
        if (saving) return
        saving = true
        scope.launch {
            val result = LandlordStore.updateTenant(context, tenant.id, name.trim(), email, mobile, active, picked?.let { Photo.Picked(it) }, removed)
            saving = false
            if (result.announce("Tenant saved")) onSaved()
        }
    }

    AppPage(
        topBar = { shell.TopBar("Edit Tenant", onBack = onBack) },
        bottomBar = { FormBar("Save changes", ::save, loading = saving, secondary = "Cancel", onSecondary = onBack) }
    ) {
        AvatarPicker(
            name = name,
            photo = picked ?: if (removed) null else tenant.profilePic,
            onPick = {
                picked = it
                removed = false
            },
            onRemove = {
                picked = null
                removed = true
            }
        )
        FormSection(Lucide.UserRound, "Details") {
            RfTextField(
                label = "Full name", value = name, onValueChange = { name = it }, icon = Lucide.User,
                placeholder = "e.g. Priya Mehta", capitalization = KeyboardCapitalization.Words, error = nameErr
            )
            RfTextField(
                label = "Mobile number", value = mobile, onValueChange = { mobile = it.filter(Char::isDigit).take(10) },
                icon = Lucide.Phone, placeholder = "98765 43210", prefix = "+91", keyboardType = KeyboardType.Number, error = mobileErr
            )
            RfTextField(
                label = "Email address (optional)", value = email, onValueChange = { email = it.trim() }, icon = Lucide.Mail,
                placeholder = "priya.mehta@example.com", keyboardType = KeyboardType.Email, error = emailErr,
                // As on the web: the API can change a saved email but not remove it
                hint = if (tenant.email.isNotBlank()) "A saved email can be changed but not removed."
                else "Without an email the tenant signs in with their mobile number."
            )
        }
        FormSection(Lucide.ShieldCheck, "Account") {
            SwitchRow(Lucide.UserCheck, "Account active", active, { active = it }, sub = "Inactive tenants can’t sign in")
        }
    }
}

/**
 * Round photo with a camera button; shows [name]'s initials when there's no
 * photo. Tapping it offers to view, take, choose (then crop round) or remove.
 */
@Composable
internal fun AvatarPicker(name: String, photo: Any?, onPick: (Uri) -> Unit, onRemove: (() -> Unit)?) {
    var viewing by remember { mutableStateOf(false) }
    val open = photoChooser(
        round = true,
        title = "Profile photo",
        onView = if (photo != null) ({ viewing = true }) else null,
        onRemove = if (photo != null) onRemove else null
    ) { uris -> onPick(uris.first()) }
    // The saved photo's download, so a failed one can say so next to the text
    val load = rememberPhotoLoad(photo as? String)
    RfCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(68.dp).pressable(open)) {
                when (photo) {
                    null -> PersonAvatar(name.ifBlank { "?" }, null, size = 64.dp)
                    is String -> PersonAvatar(name, photo, size = 64.dp, retryBadge = false, load = load)
                    else -> AsyncImage(
                        model = photo, contentDescription = name, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(CircleShape)
                    )
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).size(26.dp).clip(CircleShape).background(Rf.Lowest).padding(2.dp)
                        .clip(CircleShape).background(Rf.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    LIcon(Lucide.Camera, size = 14.dp, tint = Rf.OnPrimary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Profile photo", style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                Text("Optional. Tap the photo to take or choose one.", style = RfType.BodySm, color = Rf.OnSurfaceVariant)
                if (photo is String && load.failed) {
                    Row(Modifier.pressable(load::retry), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LIcon(Lucide.RefreshCw, size = 14.dp, tint = Rf.Error)
                        Text("Photo didn’t load · Retry", style = RfType.LabelMd, color = Rf.Error)
                    }
                }
            }
        }
    }
    if (viewing && photo != null) {
        val shown = if (photo is Uri) Photo.Picked(photo) else Photo.Stored("", photo.toString())
        PhotoViewer(listOf(shown), 0, onClose = { viewing = false })
    }
}
