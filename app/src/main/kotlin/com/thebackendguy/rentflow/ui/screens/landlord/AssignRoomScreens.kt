package com.thebackendguy.rentflow.ui.screens.landlord

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.Portfolio
import com.thebackendguy.rentflow.data.floorLabel
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.isValidMobile
import com.thebackendguy.rentflow.data.mobileLabel
import com.thebackendguy.rentflow.data.occupancyCount
import com.thebackendguy.rentflow.data.occupancyLabel
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.NewTenantBody
import com.thebackendguy.rentflow.data.remote.RoomDto
import com.thebackendguy.rentflow.data.remote.TenantDto
import com.thebackendguy.rentflow.data.roomCapacity
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.Caption
import com.thebackendguy.rentflow.ui.components.FormBar
import com.thebackendguy.rentflow.ui.components.FormSection
import com.thebackendguy.rentflow.ui.components.InfoCard
import com.thebackendguy.rentflow.ui.components.PersonAvatar
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.RfTextField
import com.thebackendguy.rentflow.ui.components.SectionHead
import com.thebackendguy.rentflow.ui.components.TextLink
import com.thebackendguy.rentflow.ui.components.Toasts
import com.thebackendguy.rentflow.ui.components.compositeOverCard
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.components.softShadow
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.screens.auth.optionalEmailError
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.launch

/* ------------------------------ Add tenant, or give one a room ------------------------------ */

private enum class Step(val label: String) { Tenant("Tenant"), Room("Room") }

/**
 * Adds a tenant in two steps: details, then a room with space (skippable).
 * With [existingTenantId] it only picks a room for that tenant (assign or move);
 * with [presetRoomId] the room is already chosen. Rent and deposit are the room's.
 */
@Composable
fun AddTenantScreen(existingTenantId: String?, presetRoomId: String?, shell: Shell, onBack: () -> Unit, onDone: (String) -> Unit) {
    val state = portfolioState()
    val portfolio = state.data
    if (portfolio == null) {
        AppPage(topBar = { shell.TopBar("Add Tenant", onBack = onBack) }) { PortfolioContent(state) {} }
        return
    }
    val existing = existingTenantId?.let { portfolio.tenant(it) }
    if (existingTenantId != null && existing == null) {
        AppPage(topBar = { shell.TopBar("Assign Room", onBack = onBack) }) { NotFound("Tenant", onBack) }
        return
    }
    AddTenantFlow(portfolio, existing, presetRoomId, shell, onBack, onDone)
}

@Composable
private fun AddTenantFlow(
    portfolio: Portfolio,
    existing: TenantDto?,
    presetRoomId: String?,
    shell: Shell,
    onBack: () -> Unit,
    onDone: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val steps = listOfNotNull(Step.Tenant.takeIf { existing == null }, Step.Room.takeIf { presetRoomId == null })
        .ifEmpty { listOf(Step.Room) }
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[stepIndex]
    val lastStep = stepIndex == steps.lastIndex

    var name by rememberSaveable { mutableStateOf("") }
    var mobile by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("Tenant@123") }
    var photo by rememberSaveable { mutableStateOf<Uri?>(null) }
    var roomId by rememberSaveable { mutableStateOf(presetRoomId) }
    var propertyId by rememberSaveable {
        mutableStateOf(portfolio.room(presetRoomId)?.propertyId ?: existing?.propertyId ?: portfolio.properties.firstOrNull { p ->
            portfolio.roomsOf(p.id).any { it.isAvailable }
        }?.id ?: portfolio.properties.firstOrNull()?.id)
    }
    // Set once the account exists, so a retry only assigns the room
    var createdId by rememberSaveable { mutableStateOf(existing?.id) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val room = portfolio.room(roomId)
    val tenantName = existing?.fullName ?: name.trim()
    val moving = existing?.roomId != null

    fun back() {
        submitted = false
        if (stepIndex > 0) stepIndex-- else onBack()
    }
    BackHandler(enabled = stepIndex > 0 && !saving) { back() }

    val nameErr = if (submitted && name.trim().length < 2) "Enter the tenant’s full name" else null
    val mobileErr = if (submitted && !isValidMobile(mobile)) "Enter a valid 10-digit mobile number" else null
    val emailErr = if (submitted) optionalEmailError(email) else null
    val passwordErr = if (submitted && password.length < 6) "Use at least 6 characters" else null

    /** Creates the account if it doesn't exist yet; returns its id. */
    suspend fun ensureTenant(): String? {
        createdId?.let { return it }
        val result = LandlordStore.addTenant(context, NewTenantBody(name.trim(), email.trim().lowercase().ifBlank { null }, mobile, password, null), photo)
        return when (result) {
            is ApiResult.Ok -> result.value.also { createdId = it }
            is ApiResult.Fail -> {
                Toasts.error(result.message)
                null
            }
        }
    }

    fun skipRoom() {
        if (saving) return
        saving = true
        scope.launch {
            val id = ensureTenant()
            if (id != null) {
                LandlordStore.finish()
                Toasts.show("$tenantName added. Assign a room any time.")
                onDone(id)
            }
            saving = false
        }
    }

    /** Creates the tenant if needed, then puts them in the chosen room. */
    fun finish() {
        val target = room
        if (target == null) {
            Toasts.error(if (existing == null) "Choose a room with space, or skip and assign one later." else "Choose a room with space.")
            return
        }
        if (saving) return
        saving = true
        scope.launch {
            val id = ensureTenant()
            if (id != null) {
                when (val assigned = LandlordStore.assignRoom(id, target.id)) {
                    is ApiResult.Ok -> {
                        Toasts.show("$tenantName is now in Room ${target.roomNumber}")
                        onDone(id)
                    }
                    is ApiResult.Fail -> if (existing == null) {
                        // The account exists now, so the lists should show it
                        LandlordStore.finish()
                        Toasts.error("$tenantName was saved, but the room couldn’t be assigned: ${assigned.message}")
                    } else {
                        Toasts.error(assigned.message)
                    }
                }
            }
            saving = false
        }
    }

    fun next() {
        submitted = true
        if (step == Step.Tenant && (name.trim().length < 2 || !isValidMobile(mobile) || optionalEmailError(email) != null || password.length < 6)) return
        if (lastStep) {
            finish()
            return
        }
        submitted = false
        stepIndex++
    }

    val title = when {
        existing == null -> "Add Tenant"
        moving -> "Change Room"
        else -> "Assign Room"
    }
    AppPage(
        topBar = { shell.TopBar(title, onBack = ::back) },
        bottomBar = {
            FormBar(
                primary = when {
                    !lastStep -> "Next: choose room"
                    existing == null -> "Add tenant"
                    moving -> "Move to this room"
                    else -> "Assign room"
                },
                onPrimary = ::next,
                loading = saving && lastStep,
                secondary = if (stepIndex > 0) "Back" else null,
                onSecondary = if (stepIndex > 0) ({ back() }) else null
            )
        }
    ) {
        if (steps.size > 1) StepBar(steps.map { it.label }, stepIndex)

        when (step) {
            Step.Tenant -> {
                if (room != null && presetRoomId != null) RoomSummary(portfolio, room, "Moves into Room ${room.roomNumber}")
                AvatarPicker(name, photo, onPick = { photo = it }, onRemove = { photo = null })
                FormSection(Lucide.UserRound, "Personal details") {
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
                        hint = "Without an email the tenant signs in with their mobile number."
                    )
                    RfTextField(
                        label = "Initial password", value = password, onValueChange = { password = it }, icon = Lucide.Lock,
                        placeholder = "Tenant@123", isPassword = true, error = passwordErr,
                        hint = "Share it with the tenant. They can change it after signing in."
                    )
                }
            }

            Step.Room -> {
                if (existing != null && moving) {
                    val now = listOfNotNull(existing.room?.roomNumber?.let { "Room $it" }, existing.property?.propertyName).joinToString(" • ")
                    InfoCard(Lucide.DoorOpen, "${existing.fullName} is in $now", "Choose the room they move to. Their old room gets the space back.", iconColor = Rf.Primary)
                }
                RoomPicker(
                    portfolio, propertyId, roomId, currentRoomId = existing?.roomId,
                    onProperty = { propertyId = it; roomId = null }, onRoom = { roomId = it }
                )
                if (room != null) {
                    InfoCard(
                        Lucide.IndianRupee, "${inr(room.rent)} a month • ${inr(room.securityDeposit)} deposit",
                        "Rent and deposit are set on the room. Edit the room to change them.", iconColor = Rf.Primary
                    )
                }
                if (existing == null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (saving) Text("Saving…", style = RfType.LabelMd, color = Rf.OnSurfaceVariant)
                        else TextLink("Skip, assign a room later", onClick = ::skipRoom)
                    }
                }
            }
        }
    }
}

/** The room a tenant is about to get, with its property and rent. */
@Composable
private fun RoomSummary(portfolio: Portfolio, room: RoomDto, title: String) {
    RfCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Rf.PrimaryFixed), contentAlignment = Alignment.Center) {
                Text(room.roomNumber, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.Primary, maxLines = 1)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(portfolio.property(room.propertyId)?.propertyName, floorLabel(room.floor), "${inr(room.rent)} / month").joinToString(" • "),
                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Progress bars with step names under them. */
@Composable
private fun StepBar(labels: List<String>, current: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.indices.forEach { i ->
                val color by animateColorAsState(if (i <= current) Rf.Primary else Rf.Container, label = "stepBar")
                Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(color))
            }
        }
        Row {
            labels.forEachIndexed { i, label ->
                Text(
                    "${i + 1}. $label",
                    style = RfType.LabelSm,
                    color = if (i <= current) Rf.Primary else Rf.Outline,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Property pills, then that property's rooms with space (not [currentRoomId]), two to a row. */
@Composable
private fun RoomPicker(
    portfolio: Portfolio,
    propertyId: String?,
    roomId: String?,
    currentRoomId: String?,
    onProperty: (String) -> Unit,
    onRoom: (String) -> Unit
) {
    if (portfolio.properties.isEmpty()) {
        RfCard { Text("Add a property and its rooms first, then assign a room here.", style = RfType.BodyMd, color = Rf.OnSurfaceVariant) }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHead("Property") { Caption("${portfolio.properties.size} properties") }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            portfolio.properties.forEach { property ->
                val on = property.id == propertyId
                Row(
                    Modifier
                        .pressable({ onProperty(property.id) })
                        .softShadow(CircleShape)
                        .clip(CircleShape)
                        .background(if (on) Rf.Primary else Rf.Lowest)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LIcon(Lucide.Building2, size = 16.dp, tint = if (on) Rf.OnPrimary else Rf.Primary)
                    Text(property.propertyName, style = RfType.LabelMd, color = if (on) Rf.OnPrimary else Rf.OnSurface, maxLines = 1)
                }
            }
        }
    }
    val rooms = propertyId?.let { portfolio.roomsOf(it) }.orEmpty()
    // The API keeps isAvailable in step with how many live in the room
    val open = rooms.filter { it.isAvailable && it.id != currentRoomId }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHead("Rooms with space") { Caption("${open.size} of ${rooms.size} rooms") }
        if (open.isEmpty()) {
            RfCard {
                Text(
                    if (rooms.isEmpty()) "This property has no rooms yet." else "Every room here is full. Pick another property.",
                    style = RfType.BodyMd, color = Rf.OnSurfaceVariant
                )
            }
        }
        open.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { room ->
                    RoomOption(room, portfolio.occupants(room.id).size, room.id == roomId, Modifier.weight(1f)) { onRoom(room.id) }
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RoomOption(room: RoomDto, occupants: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val ring by animateColorAsState(if (selected) Rf.Primary else Color.Transparent, label = "roomRing")
    Column(
        modifier
            .pressable(onClick, pressScale = 0.97f)
            .softShadow(shape)
            .clip(shape)
            .background(if (selected) Rf.PrimaryFixed.copy(alpha = 0.35f).compositeOverCard() else Rf.Lowest)
            .border(2.dp, ring, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(room.roomNumber, style = RfType.BodyLg.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface, modifier = Modifier.weight(1f))
            LIcon(if (selected) Lucide.CircleCheck else Lucide.Circle, size = 20.dp, tint = if (selected) Rf.Primary else Rf.OutlineVariant)
        }
        Text("${floorLabel(room.floor)} • ${occupancyLabel(room.occupancyType)}", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
        // A room for more than one says how full it is
        if (roomCapacity(room.occupancyType) != 1) {
            Text(occupancyCount(room.occupancyType, occupants), style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
        }
        Text("${inr(room.rent)} / month", style = RfType.LabelMd, color = Rf.Primary)
    }
}

/* ------------------------------ Assign a room with space ------------------------------ */

/** From a room with space: pick a tenant without a room, or add a new one. */
@Composable
fun AssignRoomScreen(roomId: String, shell: Shell, onBack: () -> Unit, onNewTenant: () -> Unit, onDone: () -> Unit) {
    val state = portfolioState()
    val room = state.data?.room(roomId)
    val scope = rememberCoroutineScope()
    var tenantId by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    fun assign() {
        val target = room ?: return
        val tenant = state.data?.tenant(tenantId)
        if (tenant == null) {
            Toasts.error("Choose a tenant, or add a new one.")
            return
        }
        if (saving) return
        saving = true
        scope.launch {
            val done = LandlordStore.assignRoom(tenant.id, target.id).announce("${tenant.fullName} is now in Room ${target.roomNumber}")
            saving = false
            if (done) onDone()
        }
    }

    AppPage(
        topBar = { shell.TopBar(room?.let { "Assign Room ${it.roomNumber}" } ?: "Assign Room", onBack = onBack) },
        bottomBar = if (room != null) ({ FormBar("Assign to Room ${room.roomNumber}", ::assign, loading = saving) }) else null
    ) {
        PortfolioContent(state) { portfolio ->
            if (room == null) {
                NotFound("Room", onBack)
                return@PortfolioContent
            }
            RoomSummary(portfolio, room, "Room ${room.roomNumber} • ${occupancyCount(room.occupancyType, portfolio.occupants(room.id).size)}")
            val waiting = portfolio.tenantsWithoutRoom
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHead("Tenants without a room") { Caption("${waiting.size} found") }
                if (waiting.isEmpty()) {
                    Text("Every tenant already has a room.", style = RfType.BodyMd, color = Rf.OnSurfaceVariant, modifier = Modifier.padding(4.dp))
                }
                waiting.forEach { tenant ->
                    TenantOption(tenant, tenant.id == tenantId) { tenantId = tenant.id }
                }
                RfCard(padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp), onClick = onNewTenant) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Rf.Low), contentAlignment = Alignment.Center) {
                            LIcon(Lucide.UserPlus, size = 18.dp, tint = Rf.Primary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Add a new tenant", style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.Primary)
                            Text("Create their account and give them this room", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
                        }
                        LIcon(Lucide.ChevronRight, tint = Rf.Outline)
                    }
                }
            }
            InfoCard(
                Lucide.IndianRupee, "${inr(room.rent)} a month • ${inr(room.securityDeposit)} deposit",
                "Rent and deposit are set on the room. Edit the room to change them.", iconColor = Rf.Primary
            )
        }
    }
}

@Composable
private fun TenantOption(tenant: TenantDto, selected: Boolean, onClick: () -> Unit) {
    RfCard(
        padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        color = if (selected) Rf.PrimaryFixed.copy(alpha = 0.35f).compositeOverCard() else Rf.Lowest,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PersonAvatar(tenant.fullName, tenant.profilePic, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(tenant.fullName, style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                Text(mobileLabel(tenant.mobile) ?: tenant.email, style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
            }
            LIcon(if (selected) Lucide.CircleCheck else Lucide.Circle, size = 22.dp, tint = if (selected) Rf.Primary else Rf.OutlineVariant)
        }
    }
}
