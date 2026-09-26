package com.thebackendguy.rentflow.ui.screens.landlord

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.thebackendguy.rentflow.ui.components.Pill
import com.thebackendguy.rentflow.ui.components.PrimaryButton
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.RfTextField
import com.thebackendguy.rentflow.ui.components.SectionHead
import com.thebackendguy.rentflow.ui.components.Segmented
import com.thebackendguy.rentflow.ui.components.SwitchRow
import com.thebackendguy.rentflow.ui.components.TitledCard
import com.thebackendguy.rentflow.ui.components.Toasts
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.Shell
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

internal fun demoRent(tenant: TenantDto): DemoRent {
    val lease = tenant.lease
    val today = LocalDate.now()
    val due = today.withDayOfMonth((lease?.rentDueDay ?: 5).coerceIn(1, 28))
    val paid = tenant.id.hashCode().absoluteValue % 3 != 0
    val paidOn = due.minusDays((tenant.id.hashCode().absoluteValue % 4 + 1).toLong())
    return DemoRent(
        paid = paid,
        label = if (paid) "Paid ${paidOn.format(DayMonth)}" else "Due ${due.format(DayMonth)}",
        lastPayment = "${inr(lease?.rent ?: 0.0)} on ${(if (paid) paidOn else paidOn.minusMonths(1)).shortLabel()}"
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
            bottomBar = { shell.BottomNav(2) }
        ) {
            PortfolioContent(state) { portfolio ->
                val tenants = portfolio.tenants
                if (tenants.isEmpty()) {
                    EmptyState(
                        icon = Lucide.Users,
                        title = "No tenants yet",
                        message = "Add a tenant and give them a room. They sign in with the email you add.",
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

                val leased = tenants.filter { it.lease != null }
                val paid = leased.count { demoRent(it).paid }
                val noRoom = tenants.size - leased.size
                Column {
                    SectionHead("All tenants") { Caption("${LocalDate.now().monthLabel().substringBefore(' ')} rent") }
                    Segmented(
                        options = listOf("All (${tenants.size})", "Paid ($paid)", "Due (${leased.size - paid})", "No room ($noRoom)"),
                        selected = filter,
                        onSelect = { filter = it },
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    val q = query.trim()
                    val shown = tenants.filter { t ->
                        val matchesFilter = when (filter) {
                            1 -> t.lease != null && demoRent(t).paid
                            2 -> t.lease != null && !demoRent(t).paid
                            3 -> t.lease == null
                            else -> true
                        }
                        matchesFilter && (q.isEmpty() || listOfNotNull(t.fullName, t.email, t.mobile, t.lease?.property?.propertyName)
                            .any { it.contains(q, ignoreCase = true) })
                    }
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (shown.isEmpty()) {
                            Text("No tenants to show here.", style = RfType.BodyMd, color = Rf.OnSurfaceVariant, modifier = Modifier.padding(4.dp))
                        }
                        shown.forEach { tenant ->
                            TenantCard(tenant, onOpen = { onOpenTenant(tenant.id) }, onAssign = { onAssignRoom(tenant.id) })
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
private fun TenantCard(tenant: TenantDto, onOpen: () -> Unit, onAssign: () -> Unit) {
    val lease = tenant.lease
    val phone = mobileLabel(tenant.mobile) ?: tenant.email
    RfCard(padding = PaddingValues(14.dp), spacing = 10.dp, onClick = onOpen) {
        if (lease == null) {
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

        val rent = demoRent(tenant)
        val tint = if (rent.paid) Rf.Secondary else Rf.Error
        val soft = if (rent.paid) Rf.MintSoft else Rf.ErrorContainer
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PersonAvatar(tenant.fullName, tenant.profilePic, size = 40.dp, background = soft, content = tint)
                Column {
                    Text(tenant.fullName, style = RfType.BodyLg.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(lease.property?.propertyName, lease.room?.roomNumber?.let { "Room $it" }).joinToString(" • "),
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
                Text(inr(lease.rent), style = RfType.HeadlineSm.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface)
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
    onDeleted: () -> Unit
) {
    val state = portfolioState()
    val tenant = state.data?.tenant(tenantId)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmEnd by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    AppPage(topBar = { shell.TopBar(tenant?.fullName ?: "Tenant", onBack = onBack) }, bottomBar = { shell.BottomNav(2) }) {
        PortfolioContent(state) {
            if (tenant == null) {
                NotFound("Tenant", onBack)
                return@PortfolioContent
            }
            val lease = tenant.lease
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
                            IconDetail(Lucide.Mail, "Email", tenant.email)
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

            if (lease != null) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHead("Lease") { Pill("Active", background = Rf.MintSoft, content = Rf.Secondary) }
                    RfCard {
                        DetailRows(
                            listOfNotNull(
                                lease.property?.let { Detail("Property", it.propertyName) },
                                lease.room?.let { Detail("Room", "${it.roomNumber} • ${floorLabel(it.floor)}") }
                            ) + leaseRows(lease)
                        )
                        Row(
                            Modifier.padding(top = 14.dp).pressable({ onOpenRoom(lease.roomId) }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Open room", style = RfType.LabelMd, color = Rf.Primary)
                            LIcon(Lucide.ArrowRight, size = 14.dp, tint = Rf.Primary)
                        }
                    }
                }

                // Sample data until the payments API exists
                val rent = demoRent(tenant)
                TitledCard("Rent status", caption = LocalDate.now().monthLabel()) {
                    IconRows(
                        listOf(
                            IconDetail(Lucide.CircleCheck, "Payment status", if (rent.paid) "Paid" else "Due"),
                            IconDetail(Lucide.History, "Last payment", rent.lastPayment),
                            IconDetail(Lucide.Wallet, "Wallet status", if (rent.paid) "Credited" else "Pending")
                        )
                    )
                }
                ActionButton("Send reminder", Lucide.Send, Rf.Secondary, Color.White, Modifier.fillMaxWidth(), doneLabel = "Reminder sent")
                ActionButton("End lease", Lucide.LogOut, Rf.ErrorContainer, Rf.Error, Modifier.fillMaxWidth()) { confirmEnd = true }
            } else {
                RfCard {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconTile(Lucide.DoorOpen, background = Rf.PrimaryFixed, tint = Rf.Primary, size = 48.dp, radius = 14.dp, iconSize = 24.dp)
                        Text("Not in a room yet", style = RfType.BodyLg.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                        Text("Assign a vacant room to start their lease.", style = RfType.BodySm, color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    PrimaryButton("Assign a room", onClick = onAssignRoom, showArrow = false, modifier = Modifier.padding(top = 16.dp))
                }
            }

            Text(
                "Delete tenant",
                style = RfType.LabelMd, color = Rf.Error, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().pressable({ confirmDelete = true }).padding(vertical = 6.dp)
            )

            if (confirmEnd && lease != null) {
                ConfirmSheet(
                    icon = Lucide.LogOut,
                    title = "End ${tenant.fullName}’s lease?",
                    message = "Room ${lease.room?.roomNumber.orEmpty()} becomes free for another tenant. ${tenant.fullName}’s account and the lease record are kept.",
                    confirmLabel = "End lease",
                    busy = busy,
                    onConfirm = {
                        busy = true
                        scope.launch {
                            LandlordStore.endLease(lease.id).announce("Lease ended")
                            busy = false
                            confirmEnd = false
                        }
                    },
                    onDismiss = { confirmEnd = false }
                )
            }
            if (confirmDelete) {
                ConfirmSheet(
                    icon = Lucide.Trash2,
                    title = "Delete ${tenant.fullName}?",
                    message = if (lease != null) "${tenant.fullName} still has an active lease. End it first, then delete the tenant."
                    else "Their account is removed and they can no longer sign in. This can’t be undone.",
                    confirmLabel = if (lease != null) "OK" else "Delete tenant",
                    busy = busy,
                    onConfirm = {
                        if (lease != null) {
                            confirmDelete = false
                        } else {
                            busy = true
                            scope.launch {
                                val done = LandlordStore.deleteTenant(tenant.id).announce("Tenant deleted")
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
    var active by rememberSaveable { mutableStateOf(tenant.isActive) }
    var picked by rememberSaveable { mutableStateOf<Uri?>(null) }
    var removed by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val nameErr = if (submitted && name.trim().length < 2) "Enter the tenant’s full name" else null
    val mobileErr = if (submitted && !isValidMobile(mobile)) "Enter a valid 10-digit mobile number" else null

    fun save() {
        submitted = true
        if (name.trim().length < 2 || !isValidMobile(mobile)) return
        if (saving) return
        saving = true
        scope.launch {
            val result = LandlordStore.updateTenant(context, tenant.id, name.trim(), mobile, active, picked?.let { Photo.Picked(it) }, removed)
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
                label = "Email address", value = tenant.email, onValueChange = {}, icon = Lucide.Mail, placeholder = "",
                enabled = false, hint = "The email can’t be changed after the tenant is added."
            )
        }
        FormSection(Lucide.ShieldCheck, "Account") {
            SwitchRow(Lucide.UserCheck, "Account active", active, { active = it }, sub = "Inactive tenants can’t sign in")
        }
    }
}

/** Round photo with a camera button; shows [name]'s initials when there's no photo. */
@Composable
internal fun AvatarPicker(name: String, photo: Any?, onPick: (Uri) -> Unit, onRemove: (() -> Unit)?) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) onPick(uri) }
    val open = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    RfCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(68.dp).pressable(open)) {
                when (photo) {
                    null -> PersonAvatar(name.ifBlank { "?" }, null, size = 64.dp)
                    is String -> PersonAvatar(name, photo, size = 64.dp)
                    else -> AsyncImage(
                        model = photo, contentDescription = name, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(CircleShape)
                    )
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).size(26.dp).clip(CircleShape).background(Color.White).padding(2.dp)
                        .clip(CircleShape).background(Rf.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    LIcon(Lucide.Camera, size = 14.dp, tint = Color.White)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Profile photo", style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                Text("Optional. Tap the photo to choose one.", style = RfType.BodySm, color = Rf.OnSurfaceVariant)
                if (photo != null && onRemove != null) {
                    Row(Modifier.pressable(onRemove), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LIcon(Lucide.X, size = 14.dp, tint = Rf.Error)
                        Text("Remove photo", style = RfType.LabelMd, color = Rf.Error)
                    }
                }
            }
        }
    }
}
