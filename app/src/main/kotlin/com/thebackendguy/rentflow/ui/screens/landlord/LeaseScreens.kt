package com.thebackendguy.rentflow.ui.screens.landlord

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
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
import com.thebackendguy.rentflow.data.apiDay
import com.thebackendguy.rentflow.data.floorLabel
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.isValidMobile
import com.thebackendguy.rentflow.data.leaseEnd
import com.thebackendguy.rentflow.data.mobileLabel
import com.thebackendguy.rentflow.data.occupancyLabel
import com.thebackendguy.rentflow.data.ordinal
import com.thebackendguy.rentflow.data.plainAmount
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.LeaseBody
import com.thebackendguy.rentflow.data.remote.NewTenantBody
import com.thebackendguy.rentflow.data.remote.RoomDto
import com.thebackendguy.rentflow.data.remote.TenantDto
import com.thebackendguy.rentflow.data.shortLabel
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.ChoiceChips
import com.thebackendguy.rentflow.ui.components.DateField
import com.thebackendguy.rentflow.ui.components.FormBar
import com.thebackendguy.rentflow.ui.components.FormSection
import com.thebackendguy.rentflow.ui.components.InfoCard
import com.thebackendguy.rentflow.ui.components.PersonAvatar
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.RfTextField
import com.thebackendguy.rentflow.ui.components.SectionHead
import com.thebackendguy.rentflow.ui.components.Caption
import com.thebackendguy.rentflow.ui.components.TextLink
import com.thebackendguy.rentflow.ui.components.compositeOverCard
import com.thebackendguy.rentflow.ui.components.Toasts
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.components.softShadow
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.screens.auth.emailError
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.launch
import java.time.LocalDate

/* ------------------------------ Lease terms ------------------------------ */

/** The lease form's values, with the web's defaults and options. */
@Stable
internal class LeaseTerms(
    moveIn: LocalDate, tenure: Int, dueDay: Int, rent: String, deposit: String, maintenance: String,
    escalation: Int, lockIn: Int, notice: Int, meterStart: String, meterRate: String, roomId: String
) {
    var moveIn by mutableStateOf(moveIn)
    var tenure by mutableIntStateOf(tenure)
    var dueDay by mutableIntStateOf(dueDay)
    var rent by mutableStateOf(rent)
    var deposit by mutableStateOf(deposit)
    var maintenance by mutableStateOf(maintenance)
    var escalation by mutableIntStateOf(escalation)
    var lockIn by mutableIntStateOf(lockIn)
    var notice by mutableIntStateOf(notice)
    var meterStart by mutableStateOf(meterStart)
    var meterRate by mutableStateOf(meterRate)

    /** The room the rent and deposit were last filled in from. */
    var filledFrom by mutableStateOf(roomId)

    /** Rent and deposit come from the room, as on the web: its deposit, or two months' rent. */
    fun fillFrom(room: RoomDto) {
        if (filledFrom == room.id) return
        filledFrom = room.id
        rent = plainAmount(room.rent)
        deposit = plainAmount(if (room.securityDeposit > 0) room.securityDeposit else room.rent * 2)
    }

    val rentError get() = if ((rent.toDoubleOrNull() ?: 0.0) <= 0) "Enter the monthly rent" else null

    fun body(roomId: String, tenantId: String) = LeaseBody(
        roomId = roomId,
        tenantId = tenantId,
        rent = rent.toDoubleOrNull() ?: 0.0,
        securityDeposit = deposit.toDoubleOrNull() ?: 0.0,
        maintenanceCharge = maintenance.toDoubleOrNull() ?: 0.0,
        startDate = moveIn.apiDay(),
        tenureMonths = tenure,
        rentDueDay = dueDay,
        escalationPercent = escalation.toDouble(),
        lockInMonths = lockIn,
        noticePeriodMonths = notice,
        meterStartReading = meterStart.toDoubleOrNull(),
        meterRatePerUnit = meterRate.toDoubleOrNull()
    )

    companion object {
        val Tenures = listOf(6, 11, 12)
        val DueDays = listOf(1, 5, 10)
        val Escalations = listOf(10, 5, 0)
        val LockIns = listOf(3, 6, 0)
        val Notices = listOf(1, 2)

        fun defaults() = LeaseTerms(
            moveIn = LocalDate.now().plusMonths(1).withDayOfMonth(1), tenure = 11, dueDay = 5, rent = "", deposit = "",
            maintenance = "500", escalation = 10, lockIn = 3, notice = 1, meterStart = "", meterRate = "10", roomId = ""
        )

        val Saver = listSaver<LeaseTerms, String>(
            save = {
                listOf(
                    it.moveIn.toString(), "${it.tenure}", "${it.dueDay}", it.rent, it.deposit, it.maintenance,
                    "${it.escalation}", "${it.lockIn}", "${it.notice}", it.meterStart, it.meterRate, it.filledFrom
                )
            },
            restore = {
                LeaseTerms(
                    LocalDate.parse(it[0]), it[1].toInt(), it[2].toInt(), it[3], it[4], it[5],
                    it[6].toInt(), it[7].toInt(), it[8].toInt(), it[9], it[10], it[11]
                )
            }
        )
    }
}

@Composable
internal fun rememberLeaseTerms(): LeaseTerms = rememberSaveable(saver = LeaseTerms.Saver) { LeaseTerms.defaults() }

/** The lease fields. With [compact], the less common terms fold away. */
@Composable
internal fun LeaseTermsForm(terms: LeaseTerms, submitted: Boolean, compact: Boolean = false) {
    var more by rememberSaveable { mutableStateOf(!compact) }
    val end = leaseEnd(terms.moveIn, terms.tenure)

    FormSection(Lucide.CalendarDays, "Tenure") {
        DateField("Move-in date", terms.moveIn, { terms.moveIn = it }, required = true)
        ChoiceChips(
            "Agreement length", LeaseTerms.Tenures.map { "$it months" }, LeaseTerms.Tenures.indexOf(terms.tenure),
            { terms.tenure = LeaseTerms.Tenures[it] }, required = true, hint = "Ends on ${end.shortLabel()}"
        )
        ChoiceChips(
            "Rent due on", LeaseTerms.DueDays.map(::ordinal), LeaseTerms.DueDays.indexOf(terms.dueDay),
            { terms.dueDay = LeaseTerms.DueDays[it] }, required = true, hint = "Of every month"
        )
    }

    FormSection(Lucide.IndianRupee, "Charges") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RfTextField(
                label = "Monthly rent", value = terms.rent, onValueChange = { terms.rent = it.filter(Char::isDigit).take(9) },
                icon = Lucide.IndianRupee, placeholder = "14000", keyboardType = KeyboardType.Number,
                error = if (submitted) terms.rentError else null, modifier = Modifier.weight(1f)
            )
            RfTextField(
                label = "Deposit", value = terms.deposit, onValueChange = { terms.deposit = it.filter(Char::isDigit).take(9) },
                icon = Lucide.IndianRupee, placeholder = "28000", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f)
            )
        }
        RfTextField(
            label = "Maintenance and water", value = terms.maintenance, onValueChange = { terms.maintenance = it.filter(Char::isDigit).take(7) },
            icon = Lucide.IndianRupee, placeholder = "500", keyboardType = KeyboardType.Number, hint = "A fixed amount every month"
        )
    }

    if (!more) {
        Row(
            Modifier.pressable({ more = true }).padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LIcon(Lucide.ChevronDown, size = 16.dp, tint = Rf.Primary)
            Text("More: rent increase, lock-in, notice, sub-meter", style = RfType.LabelMd, color = Rf.Primary)
        }
    }
    AnimatedVisibility(more) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            FormSection(Lucide.FileText, "Terms") {
                ChoiceChips("Rent increase on renewal", LeaseTerms.Escalations.map { if (it == 0) "None" else "$it%" },
                    LeaseTerms.Escalations.indexOf(terms.escalation), { terms.escalation = LeaseTerms.Escalations[it] })
                ChoiceChips("Lock-in", LeaseTerms.LockIns.map { if (it == 0) "None" else "$it months" },
                    LeaseTerms.LockIns.indexOf(terms.lockIn), { terms.lockIn = LeaseTerms.LockIns[it] })
                ChoiceChips("Notice period", LeaseTerms.Notices.map { if (it == 1) "1 month" else "$it months" },
                    LeaseTerms.Notices.indexOf(terms.notice), { terms.notice = LeaseTerms.Notices[it] })
            }
            FormSection(Lucide.Zap, "Electricity sub-meter", caption = "Optional") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RfTextField(
                        label = "Start reading", value = terms.meterStart,
                        onValueChange = { v -> terms.meterStart = v.filter { it.isDigit() || it == '.' }.take(9) },
                        icon = Lucide.Zap, placeholder = "e.g. 1520", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f)
                    )
                    RfTextField(
                        label = "Rate per unit", value = terms.meterRate,
                        onValueChange = { v -> terms.meterRate = v.filter { it.isDigit() || it == '.' }.take(6) },
                        icon = Lucide.IndianRupee, placeholder = "10", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/* ------------------------------ Add tenant (3 steps) ------------------------------ */

private enum class Step(val label: String) { Tenant("Tenant"), Room("Room"), Lease("Lease") }

/**
 * Adds a tenant in three steps: details, a vacant room, the lease terms.
 * With [existingTenantId] it only assigns a room to that tenant; with
 * [presetRoomId] the room is already chosen.
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
    val steps = listOfNotNull(Step.Tenant.takeIf { existing == null }, Step.Room.takeIf { presetRoomId == null }, Step.Lease)
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[stepIndex]

    var name by rememberSaveable { mutableStateOf("") }
    var mobile by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("Tenant@123") }
    var photo by rememberSaveable { mutableStateOf<Uri?>(null) }
    var roomId by rememberSaveable { mutableStateOf(presetRoomId) }
    var propertyId by rememberSaveable {
        mutableStateOf(portfolio.room(presetRoomId)?.propertyId ?: portfolio.properties.firstOrNull { p ->
            portfolio.roomsOf(p.id).any { it.isAvailable }
        }?.id ?: portfolio.properties.firstOrNull()?.id)
    }
    // Set once the account exists, so a retry only assigns the room
    var createdId by rememberSaveable { mutableStateOf(existing?.id) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val terms = rememberLeaseTerms()

    val room = portfolio.room(roomId)
    val tenantName = existing?.fullName ?: name.trim()

    fun back() {
        submitted = false
        if (stepIndex > 0) stepIndex-- else onBack()
    }
    BackHandler(enabled = stepIndex > 0 && !saving) { back() }

    val nameErr = if (submitted && name.trim().length < 2) "Enter the tenant’s full name" else null
    val mobileErr = if (submitted && !isValidMobile(mobile)) "Enter a valid 10-digit mobile number" else null
    val emailErr = if (submitted) emailError(email) else null
    val passwordErr = if (submitted && password.length < 6) "Use at least 6 characters" else null

    /** Creates the account if it doesn't exist yet; returns its id. */
    suspend fun ensureTenant(): String? {
        createdId?.let { return it }
        val result = LandlordStore.addTenant(context, NewTenantBody(name.trim(), email.trim().lowercase(), mobile, password, null), photo)
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

    fun next() {
        submitted = true
        when (step) {
            Step.Tenant -> {
                if (name.trim().length < 2 || !isValidMobile(mobile) || emailError(email) != null || password.length < 6) return
            }
            Step.Room -> {
                if (room == null) {
                    Toasts.error("Choose a vacant room, or skip and assign one later.")
                    return
                }
                terms.fillFrom(room)
            }
            Step.Lease -> {
                val target = room ?: return
                if (terms.rentError != null) return
                if (saving) return
                saving = true
                scope.launch {
                    val id = ensureTenant()
                    if (id != null) {
                        when (val lease = LandlordStore.assignRoom(terms.body(target.id, id))) {
                            is ApiResult.Ok -> {
                                Toasts.show("$tenantName is now in Room ${target.roomNumber}")
                                onDone(id)
                            }
                            is ApiResult.Fail -> Toasts.error(
                                if (existing == null) "$tenantName was saved, but the room couldn’t be assigned: ${lease.message}"
                                else lease.message
                            )
                        }
                    }
                    saving = false
                }
                return
            }
        }
        submitted = false
        if (stepIndex < steps.lastIndex) stepIndex++
    }

    // Arriving on the lease step with the room already chosen fills in its rent
    LaunchedEffect(step, room?.id) { if (step == Step.Lease && room != null) terms.fillFrom(room) }

    val title = if (existing != null) "Assign Room" else "Add Tenant"
    AppPage(
        topBar = { shell.TopBar(title, onBack = ::back) },
        bottomBar = {
            FormBar(
                primary = when (step) {
                    Step.Tenant -> "Next: choose room"
                    Step.Room -> "Next: lease terms"
                    Step.Lease -> if (existing != null) "Assign room" else "Add tenant"
                },
                onPrimary = ::next,
                loading = saving && step == Step.Lease,
                secondary = if (stepIndex > 0) "Back" else null,
                onSecondary = if (stepIndex > 0) ({ back() }) else null
            )
        }
    ) {
        if (steps.size > 1) StepBar(steps.map { it.label }, stepIndex)

        when (step) {
            Step.Tenant -> {
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
                        label = "Email address", value = email, onValueChange = { email = it.trim() }, icon = Lucide.Mail,
                        placeholder = "priya.mehta@example.com", keyboardType = KeyboardType.Email, error = emailErr,
                        hint = "The tenant signs in with this email. It can’t be changed later."
                    )
                    RfTextField(
                        label = "Initial password", value = password, onValueChange = { password = it }, icon = Lucide.Lock,
                        placeholder = "Tenant@123", isPassword = true, error = passwordErr,
                        hint = "Share it with the tenant. They can change it after signing in."
                    )
                }
            }

            Step.Room -> {
                RoomPicker(portfolio, propertyId, roomId, onProperty = { propertyId = it; roomId = null }, onRoom = { roomId = it })
                InfoCard(Lucide.Info, "Rent comes from the room", "You can change the rent and deposit on the next step.", iconColor = Rf.Primary)
                if (existing == null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (saving) Text("Saving…", style = RfType.LabelMd, color = Rf.OnSurfaceVariant)
                        else TextLink("Skip, assign a room later", onClick = ::skipRoom)
                    }
                }
            }

            Step.Lease -> {
                if (room != null) {
                    RfCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Rf.PrimaryFixed), contentAlignment = Alignment.Center) {
                                Text(room.roomNumber, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.Primary, maxLines = 1)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("$tenantName in Room ${room.roomNumber}", style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                                    color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    listOfNotNull(portfolio.property(room.propertyId)?.propertyName, floorLabel(room.floor)).joinToString(" • "),
                                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (presetRoomId == null) TextLink("Change", onClick = ::back)
                        }
                    }
                }
                LeaseTermsForm(terms, submitted)
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

/** Property pills, then that property's rooms with space, two to a row. */
@Composable
private fun RoomPicker(
    portfolio: Portfolio,
    propertyId: String?,
    roomId: String?,
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
    val open = rooms.filter { it.isAvailable }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHead("Vacant rooms") { Caption("${open.size} of ${rooms.size} rooms") }
        if (open.isEmpty()) {
            RfCard {
                Text(
                    if (rooms.isEmpty()) "This property has no rooms yet." else "Every room here is taken. Pick another property.",
                    style = RfType.BodyMd, color = Rf.OnSurfaceVariant
                )
            }
        }
        open.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { room -> RoomOption(room, room.id == roomId, Modifier.weight(1f)) { onRoom(room.id) } }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RoomOption(room: RoomDto, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
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
        Text("${inr(room.rent)} / month", style = RfType.LabelMd, color = Rf.Primary)
    }
}

/* ------------------------------ Assign a vacant room ------------------------------ */

/** From a vacant room: pick a tenant without a room, set the lease terms. */
@Composable
fun AssignRoomScreen(roomId: String, shell: Shell, onBack: () -> Unit, onNewTenant: () -> Unit, onDone: () -> Unit) {
    val state = portfolioState()
    val room = state.data?.room(roomId)
    val scope = rememberCoroutineScope()
    val terms = rememberLeaseTerms()
    var tenantId by rememberSaveable { mutableStateOf<String?>(null) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(room?.id) { if (room != null) terms.fillFrom(room) }

    fun assign() {
        submitted = true
        val target = room ?: return
        val tenant = state.data?.tenant(tenantId)
        if (tenant == null) {
            Toasts.error("Choose a tenant, or add a new one.")
            return
        }
        if (terms.rentError != null || saving) return
        saving = true
        scope.launch {
            val done = LandlordStore.assignRoom(terms.body(target.id, tenant.id)).announce("${tenant.fullName} is now in Room ${target.roomNumber}")
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
            val waiting = portfolio.tenantsWithoutRoom
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHead("Tenants without a room") { Caption("${waiting.size} found") }
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
            LeaseTermsForm(terms, submitted, compact = true)
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
