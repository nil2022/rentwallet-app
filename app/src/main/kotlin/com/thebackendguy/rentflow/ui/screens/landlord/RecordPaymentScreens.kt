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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebackendguy.rentflow.data.AdvanceBill
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.PaymentStore
import com.thebackendguy.rentflow.data.Photo
import com.thebackendguy.rentflow.data.apiMonth
import com.thebackendguy.rentflow.data.carryAdvance
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.isPdfLink
import com.thebackendguy.rentflow.data.monthLabel
import com.thebackendguy.rentflow.data.round2
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.BillDto
import com.thebackendguy.rentflow.data.remote.UploadFolder
import com.thebackendguy.rentflow.data.remote.Uploads
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.DateField
import com.thebackendguy.rentflow.ui.components.ErrorState
import com.thebackendguy.rentflow.ui.components.FormBar
import com.thebackendguy.rentflow.ui.components.FormSection
import com.thebackendguy.rentflow.ui.components.LoadingCards
import com.thebackendguy.rentflow.ui.components.Pill
import com.thebackendguy.rentflow.ui.components.PhotoViewer
import com.thebackendguy.rentflow.ui.components.RemoteImage
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.RfTextField
import com.thebackendguy.rentflow.ui.components.SelectField
import com.thebackendguy.rentflow.ui.components.SwitchRow
import com.thebackendguy.rentflow.ui.components.TextArea
import com.thebackendguy.rentflow.ui.components.Toasts
import com.thebackendguy.rentflow.ui.components.photoChooser
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.roundToLong

/* ------------------------------ Who is it for? ------------------------------ */

/**
 * The first step of Record Payment from the Payments tab: the tenants who live in a
 * room, each marked with whether this month already has a bill.
 */
@Composable
fun BillTenantPickerScreen(shell: Shell, onBack: () -> Unit, onPick: (String) -> Unit) {
    val state = portfolioState()
    var query by rememberSaveable { mutableStateOf("") }
    var billed by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(Unit) {
        val result = PaymentStore.bills(month = YearMonth.now())
        billed = (result as? ApiResult.Ok)?.value?.map { it.tenantId }?.toSet() ?: emptySet()
    }

    AppPage(topBar = { shell.TopBar("Record Payment", onBack = onBack) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Who is it for?", style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurfaceVariant)
            SearchBox(query, "Search tenants") { query = it }
            PortfolioContent(state) { portfolio ->
                val q = query.trim()
                val tenants = portfolio.tenants
                    .filter { it.roomId != null }
                    .filter { q.isEmpty() || listOfNotNull(it.fullName, it.mobile, it.email, it.room?.roomNumber).any { f -> f.contains(q, ignoreCase = true) } }
                    .sortedBy { it.fullName.lowercase() }
                if (tenants.isEmpty()) {
                    Text(
                        "No tenant in a room matches.", style = RfType.BodyMd, color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                    )
                }
                RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                    tenants.forEachIndexed { index, tenant ->
                        if (index > 0) HorizontalDivider(thickness = 1.dp, color = Rf.Low)
                        Row(
                            Modifier.fillMaxWidth().pressable({ onPick(tenant.id) }, pressScale = 0.98f).padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(Modifier.size(38.dp).clip(CircleShape).background(Rf.PrimaryFixed), contentAlignment = Alignment.Center) {
                                Text(initials(tenant.fullName), style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.Primary)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(tenant.fullName, style = RfType.BodyMd.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    listOfNotNull(tenant.room?.roomNumber?.let { "Room $it" }, tenant.property?.propertyName).joinToString(" • "),
                                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            billed?.let { ids ->
                                val has = tenant.id in ids
                                Pill(
                                    if (has) "Bill exists" else "Not billed",
                                    background = if (has) Rf.MintSoft else Rf.Low, content = if (has) Rf.Secondary else Rf.OnSurfaceVariant,
                                    style = RfType.LabelSm.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), vertical = 3.dp
                                )
                            }
                        }
                    }
                }
                Text(
                    "Only tenants who live in a room are listed. Picking one opens the form for this month.",
                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
                )
            }
        }
    }
}

/* ------------------------------ Record payment ------------------------------ */

/** One extra charge in the form, as typed. */
private data class Charge(val name: String, val amount: String)

private val ChargesSaver = listSaver<List<Charge>, String>(
    save = { list -> list.flatMap { listOf(it.name, it.amount) } },
    restore = { flat -> flat.chunked(2).map { Charge(it[0], it[1]) } }
)

private val QuickCharges = listOf("WiFi", "Gas", "Maintenance", "Water")
private val Methods = listOf(Triple("cash", "Cash", Lucide.Banknote), Triple("upi", "UPI", Lucide.Smartphone), Triple("cheque", "Cheque", Lucide.Landmark))

private fun num(text: String?): Double = text?.toDoubleOrNull() ?: 0.0

/** "4000" or "12.5", for a field filled in from a saved amount. */
private fun amountText(value: Double): String = if (value % 1.0 == 0.0) value.roundToLong().toString() else value.toString()

private fun decimalInput(text: String): String = text.filter { it.isDigit() || it == '.' }.take(10)

/**
 * The web's Record Payment form, for the three things a landlord does with a tenant's bill:
 *  - create:  no bill for the month yet. Charges, plus any money already received, are saved together.
 *  - collect: the month has a bill. The money received now is added to what was received before;
 *             on a bill that is already paid, this is "Receive an advance".
 *  - edit:    [editBillId] is given. Its charges and the total received are corrected.
 * Money beyond what a bill needs is advance the tenant's later bills use, and advance
 * left from earlier bills comes off this one. The summary previews what the API works out.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordPaymentScreen(tenantId: String, initialMonth: String?, editBillId: String?, shell: Shell, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val portfolio = portfolioState().data
    val tenant = portfolio?.tenant(tenantId)
    val isEdit = editBillId != null

    // This tenant's bills (to spot an existing month) and the room's bills (the meter belongs
    // to the room, so the last reading can come from a previous tenant's bill)
    var tenantBills by remember { mutableStateOf<List<BillDto>?>(null) }
    var roomBills by remember { mutableStateOf<List<BillDto>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val editBill = tenantBills?.find { it.id == editBillId }
    val roomId = editBill?.roomId ?: tenant?.roomId
    LaunchedEffect(tenantId, roomId, retry) {
        loadError = null
        when (val r = PaymentStore.bills(tenantId = tenantId)) {
            is ApiResult.Fail -> loadError = r.message
            is ApiResult.Ok -> tenantBills = r.value
        }
        if (roomId == null) return@LaunchedEffect
        when (val r = PaymentStore.bills(roomId = roomId)) {
            is ApiResult.Fail -> loadError = r.message
            is ApiResult.Ok -> roomBills = r.value
        }
    }

    // The form, as typed. A null field is untouched: the room's rent, last reading or rate shows.
    var monthText by rememberSaveable { mutableStateOf(initialMonth ?: YearMonth.now().apiMonth()) }
    var rent by rememberSaveable { mutableStateOf<String?>(null) }
    var elecOn by rememberSaveable { mutableStateOf(false) }
    var prev by rememberSaveable { mutableStateOf<String?>(null) }
    var cur by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf<String?>(null) }
    var photo by rememberSaveable { mutableStateOf<Uri?>(null) }
    var photoCleared by rememberSaveable { mutableStateOf(false) }
    var other by rememberSaveable(stateSaver = ChargesSaver) { mutableStateOf(emptyList<Charge>()) }
    var recv by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf("upi") }
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var proof by rememberSaveable { mutableStateOf<Uri?>(null) }
    var notes by rememberSaveable { mutableStateOf<String?>(null) }
    var advanceMode by rememberSaveable { mutableStateOf(false) }
    var filledFromBill by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    fun resetFor(month: String) {
        monthText = month
        rent = null; elecOn = false; prev = null; cur = ""; rate = null; photo = null; photoCleared = false
        other = emptyList(); recv = ""; method = "upi"; dateText = LocalDate.now().toString()
        proof = null; notes = null; advanceMode = false; submitted = false
    }

    // Editing starts from the saved bill, once it has loaded
    LaunchedEffect(editBill) {
        val bill = editBill ?: return@LaunchedEffect
        if (filledFromBill) return@LaunchedEffect
        filledFromBill = true
        monthText = bill.monthYear
        rent = amountText(bill.rent)
        elecOn = bill.electricity != null
        prev = bill.electricity?.let { amountText(it.previousReading) }
        cur = bill.electricity?.let { amountText(it.currentReading) }.orEmpty()
        rate = bill.electricity?.let { amountText(it.ratePerUnit) }
        other = bill.otherCharges.map { Charge(it.name, amountText(it.amount)) }
        notes = bill.notes.orEmpty()
    }

    val title = if (isEdit) "Edit Bill" else "Record Payment"
    val bills = tenantBills
    if (portfolio == null || bills == null || (roomId != null && roomBills == null)) {
        AppPage(topBar = { shell.TopBar(title, onBack = onBack) }) {
            if (loadError != null) ErrorState(loadError.orEmpty(), onRetry = { retry++ }) else LoadingCards(2)
        }
        return
    }
    if ((isEdit && editBill == null) || (tenant == null && editBill == null)) {
        AppPage(topBar = { shell.TopBar(title, onBack = onBack) }) { NotFound(if (isEdit) "Bill" else "Tenant", onBack) }
        return
    }
    if (roomId == null) {
        AppPage(topBar = { shell.TopBar(title, onBack = onBack) }) {
            RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(24.dp)) {
                Text(
                    "A bill is made against the tenant’s room, so ${tenant?.fullName ?: "this tenant"} needs a room before they can be billed.",
                    style = RfType.BodyMd, color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    val room = portfolio.room(roomId)
    val roomRent = room?.rent
    val hasRoommates = portfolio.occupants(roomId).size > 1
    val name = tenant?.fullName ?: editBill?.tenant?.fullName ?: "Tenant"
    val subtitle = listOfNotNull(
        name,
        (room?.roomNumber ?: editBill?.room?.roomNumber)?.let { "Room $it" },
        (portfolio.property(room?.propertyId)?.propertyName ?: editBill?.property?.propertyName)
    ).joinToString(" • ")

    // `existing` is the bill whose payment is being collected; while editing, the charges are the form's own
    val existing = if (isEdit) null else bills.find { it.monthYear == monthText }
    val isPaidUp = existing?.paymentStatus == "paid"
    // Editing changes the charges and note only: payments are entries of the bill now
    val showPayment = !isEdit && (!isPaidUp || advanceMode)
    val last = roomBills.orEmpty().sortedByDescending { it.monthYear }.firstOrNull { it.electricity != null && it.monthYear < monthText }

    val rentValue = rent ?: roomRent?.let(::amountText).orEmpty()
    val prevValue = prev ?: last?.electricity?.currentReading?.let(::amountText).orEmpty()
    val rateValue = rate ?: last?.electricity?.ratePerUnit?.let(::amountText).orEmpty()
    // The bill's note (new bill, edit), or this payment's own note (collect)
    val notesValue = notes.orEmpty()
    val received = num(recv)
    val charges = other.filter { it.name.isNotBlank() || it.amount.isNotEmpty() }

    // Totals as the API will work them out
    val units: Double
    val electricity: Double
    val otherTotal: Double
    val total: Double
    val paidBefore: Double
    if (existing != null) {
        units = existing.electricity?.unitsConsumed ?: 0.0
        electricity = existing.electricity?.amount ?: 0.0
        otherTotal = existing.otherCharges.sumOf { it.amount }
        total = existing.totalDue
        // Money received plus expenses the tenant paid and took off
        paidBefore = existing.settledByTenant
    } else {
        units = if (elecOn && cur.isNotEmpty()) max(round2(num(cur) - num(prevValue)), 0.0) else 0.0
        electricity = if (elecOn && cur.isNotEmpty()) round2(units * num(rateValue)) else 0.0
        otherTotal = charges.sumOf { num(it.amount) }
        total = round2(num(rentValue) + electricity + otherTotal)
        paidBefore = editBill?.settledByTenant ?: 0.0
    }
    // Editing keeps what was paid and taken off; collecting adds to it
    val paidAfter = round2(if (existing != null || isEdit) paidBefore + received else received)

    // Advance, with the backend's rules on this tenant's bills
    val selfId = existing?.id ?: editBill?.id ?: "new"
    val others = bills.filter { it.id != selfId && it.monthYear != monthText }.map { AdvanceBill(it.id, it.monthYear, it.totalDue, it.settledByTenant) }
    fun run(paid: Double) = carryAdvance(others + AdvanceBill(selfId, monthText, total, paid))
    val self = run(0.0).first { it.id == selfId }
    val credit = self.credit
    val payable = round2(total - credit)
    val basePaid = max(payable, paidBefore)
    val added = round2(max(paidAfter - basePaid, 0.0))
    var goesTo = emptyList<Pair<String, Double>>()
    var kept = added
    if (added > 0) {
        val base = run(basePaid)
        val after = run(paidAfter)
        goesTo = after.zip(base).map { (a, b) -> a.monthYear to round2(a.credit - b.credit) }.filter { it.first > monthText && it.second > 0 }
        kept = round2(after.sumOf { it.left } - base.sumOf { it.left })
    }
    val earlierDue = bills.filter { it.monthYear < monthText && it.pendingAmount > 0 }
    val pendingAfter = round2(max(payable - paidAfter, 0.0))
    // What settles the bill: the "Full amount" chip
    val fullAmount = round2(max(payable - paidBefore, 0.0))

    // Mistakes, shown once Save was tapped
    val errors = mutableMapOf<String, String>()
    if (existing == null) {
        if (rentValue.isEmpty()) errors["rent"] = "Enter the rent"
        if (elecOn) {
            if (prevValue.isEmpty()) errors["prev"] = "Enter the previous reading"
            if (cur.isEmpty()) errors["cur"] = "Enter the current reading"
            else if (num(cur) < num(prevValue)) errors["cur"] = "Can’t be less than the previous one"
            if (rateValue.isEmpty()) errors["rate"] = "Enter the rate"
        }
        if (other.any { (it.name.isNotBlank() && it.amount.isEmpty()) || (it.name.isBlank() && it.amount.isNotEmpty()) }) {
            errors["other"] = "Fill in both the name and the amount of each charge"
        }
    }
    // More than the bill needs is fine: the extra is kept as advance
    if (received > 0) {
        if (LocalDate.parse(dateText).isAfter(LocalDate.now())) errors["date"] = "The payment date can’t be in the future"
    } else if (existing != null) {
        errors["recv"] = if (advanceMode) "Enter the advance amount" else "Enter the amount received"
    }
    val shown = if (submitted) errors else emptyMap()

    fun save() {
        submitted = true
        if (errors.isNotEmpty() || saving) return
        saving = true
        scope.launch {
            // A new file is sent as its key; a saved one removed while editing is sent as null
            val meterKey = if (existing == null && elecOn && photo != null) {
                when (val r = Uploads.upload(context, photo!!, UploadFolder.MeterReading)) {
                    is ApiResult.Fail -> { Toasts.error(r.message); saving = false; return@launch }
                    is ApiResult.Ok -> r.value
                }
            } else null
            val proofKey = if (!isEdit && received > 0 && proof != null) {
                when (val r = Uploads.upload(context, proof!!, UploadFolder.PaymentProof)) {
                    is ApiResult.Fail -> { Toasts.error(r.message); saving = false; return@launch }
                    is ApiResult.Ok -> r.value
                }
            } else null

            // The month already has a bill: this money is one more payment of it
            if (existing != null) {
                val result = PaymentStore.addReceipt(existing.id) {
                    put("kind", "payment")
                    put("amount", received)
                    put("method", method)
                    put("date", dateText)
                    proofKey?.let { put("proof", it) }
                    notesValue.trim().takeIf(String::isNotEmpty)?.let { put("notes", it) }
                }
                when (result) {
                    is ApiResult.Ok -> {
                        Toasts.show(result.value.message ?: "Payment added")
                        LandlordStore.refresh()
                        onDone()
                    }
                    is ApiResult.Fail -> Toasts.error(result.message)
                }
                saving = false
                return@launch
            }

            // How and when the money came with a new bill, only when some did
            fun addPayment(builder: JsonObjectBuilder) = with(builder) {
                if (received > 0) {
                    put("paymentMethod", method)
                    put("paymentDate", dateText)
                    proofKey?.let { put("paymentProof", it) }
                }
            }
            val electricityJson: JsonObject? = if (elecOn) buildJsonObject {
                put("previousReading", num(prevValue))
                put("currentReading", num(cur))
                put("ratePerUnit", num(rateValue))
                when {
                    meterKey != null -> put("meterPhoto", meterKey)
                    isEdit && photoCleared -> put("meterPhoto", JsonNull)
                }
            } else null

            val body = when {
                // Replaces the saved charges; a meter photo that isn't sent is kept
                isEdit -> buildJsonObject {
                    put("paymentId", editBill!!.id)
                    put("rent", num(rentValue))
                    put("electricity", electricityJson ?: JsonNull)
                    putJsonArray("otherCharges") { charges.forEach { c -> addJsonObject { put("name", c.name.trim()); put("amount", num(c.amount)) } } }
                    put("notes", notesValue.trim())
                }
                else -> buildJsonObject {
                    put("tenantId", tenantId)
                    put("monthYear", monthText)
                    put("rent", num(rentValue))
                    electricityJson?.let { put("electricity", it) }
                    if (charges.isNotEmpty()) {
                        putJsonArray("otherCharges") { charges.forEach { c -> addJsonObject { put("name", c.name.trim()); put("amount", num(c.amount)) } } }
                    }
                    if (received > 0) {
                        put("paidAmount", received)
                        addPayment(this)
                    }
                    if (notesValue.isNotBlank()) put("notes", notesValue.trim())
                }
            }
            when (val r = PaymentStore.save(if (isEdit) editBill!!.id else null, body)) {
                is ApiResult.Ok -> {
                    Toasts.show(r.value.message ?: if (isEdit) "Bill updated" else "Bill saved")
                    LandlordStore.refresh()
                    onDone()
                }
                is ApiResult.Fail -> Toasts.error(r.message)
            }
            saving = false
        }
    }

    val buttonLabel = when {
        isEdit -> "Save changes"
        existing != null -> if (received > 0) "Record ${inr(received)} ${if (advanceMode) "advance" else "payment"}" else "Enter the amount"
        received > 0 -> "Save bill + ${inr(received)} payment"
        else -> "Save bill"
    }

    // This month and the 11 before it, newest first (plus the form's month if it is older)
    val months = (0 until 12).map { YearMonth.now().minusMonths(it.toLong()).apiMonth() }.let { if (monthText in it) it else listOf(monthText) + it }

    AppPage(
        topBar = { shell.TopBar(title, onBack = onBack) },
        bottomBar = {
            if (showPayment || isEdit) {
                FormBar(
                    primary = buttonLabel, onPrimary = ::save, loading = saving,
                    secondary = if (advanceMode) "Back" else "Cancel",
                    onSecondary = if (advanceMode) ({ advanceMode = false; recv = "" }) else onBack
                )
            } else {
                FormBar(primary = "Close", onPrimary = onBack)
            }
        }
    ) {
        Text(subtitle, style = RfType.BodySm.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurfaceVariant)

        FormSection(Lucide.FileText, "Billing month") {
            SelectField(
                label = "Month",
                value = monthLabel(monthText) + if (bills.any { it.monthYear == monthText } && !isEdit) "  •  bill exists" else "",
                options = months.map { m -> monthLabel(m) + if (bills.any { it.monthYear == m }) "  •  bill exists" else "" },
                onSelect = { resetFor(months[it]) },
                icon = Lucide.Calendar,
                enabled = !isEdit
            )
            when {
                isEdit -> Notice(
                    "Editing the ${monthLabel(monthText)} bill",
                    "The charges and note below replace the ones saved. Payments and expenses taken off are changed one by one from the bill."
                )
                existing != null && isPaidUp -> Notice(
                    "${monthLabel(monthText)} is fully paid",
                    "The bill of ${inr(existing.totalDue)} has been paid in full" + (if (existing.creditApplied > 0) ", ${inr(existing.creditApplied)} of it from advance." else "."),
                    tone = NoticeTone.Ok
                )
                existing != null -> Notice(
                    "A bill for ${monthLabel(monthText)} already exists",
                    "Total ${inr(existing.totalDue)}" + (if (existing.creditApplied > 0) ", ${inr(existing.creditApplied)} covered by advance" else "") +
                        ", received ${inr(existing.paidAmount)}" + (if (existing.adjustedAmount > 0) ", ${inr(existing.adjustedAmount)} expenses taken off" else "") +
                        ", still due ${inr(existing.pendingAmount)}. Enter the amount received now."
                )
                else -> Notice("No bill for ${monthLabel(monthText)} yet", "Fill in the charges below. If money was received, add it too and both are saved together.")
            }
            if (existing == null && !isEdit && credit > 0) {
                Notice(
                    "${inr(credit)} advance will be used",
                    "Paid extra with the ${monthList(self.sources)} bill. It comes off this bill when you save.",
                    tone = NoticeTone.Advance
                )
            }
        }

        // A fully paid bill: money beyond it is recorded as advance
        if (isPaidUp && !advanceMode) {
            RfCard(shape = RoundedCornerShape(16.dp)) {
                Text("Record it as advance. The next bill uses it automatically.", style = RfType.BodySm, color = Rf.OnSurfaceVariant)
                SoftButton("Receive an advance", Lucide.Plus, Modifier.padding(top = 12.dp)) { advanceMode = true }
            }
        }

        if (existing == null) {
            FormSection(Lucide.Zap, "Charges") {
                RfTextField(
                    label = "Rent", value = rentValue, onValueChange = { rent = decimalInput(it) }, icon = Lucide.IndianRupee,
                    placeholder = "0", keyboardType = KeyboardType.Decimal, error = shown["rent"],
                    hint = if (isEdit) null else roomRent?.let { "Filled in from Room ${room?.roomNumber.orEmpty()}’s rent of ${inr(it)}. Change it if this tenant pays a different amount." }
                )
                SwitchRow(Lucide.Zap, "Electricity", elecOn, { elecOn = it }, sub = "The meter belongs to the room, so readings carry on from the last bill")
                if (elecOn) {
                    if (hasRoommates) {
                        Notice(
                            "This room is shared",
                            "The meter is one for the whole room. Add its electricity to one tenant’s bill only, and use Other charges for the others’ share, so it isn’t billed twice.",
                            tone = NoticeTone.Warn
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RfTextField(
                            label = "Previous", value = prevValue, onValueChange = { prev = decimalInput(it) }, icon = Lucide.Gauge,
                            placeholder = "0", keyboardType = KeyboardType.Decimal, error = shown["prev"], modifier = Modifier.weight(1f)
                        )
                        RfTextField(
                            label = "Current", value = cur, onValueChange = { cur = decimalInput(it) }, icon = Lucide.Gauge,
                            placeholder = "0", keyboardType = KeyboardType.Decimal, error = shown["cur"], modifier = Modifier.weight(1f)
                        )
                    }
                    RfTextField(
                        label = "Rate per unit", value = rateValue, onValueChange = { rate = decimalInput(it) }, icon = Lucide.IndianRupee,
                        placeholder = "0", keyboardType = KeyboardType.Decimal, error = shown["rate"],
                        hint = if (isEdit) null else if (last != null) "Previous reading and rate are from the ${monthLabel(last.monthYear)} bill."
                        else "This is the first reading for this room, so enter the meter’s current value as the previous reading."
                    )
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Rf.Low).padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (cur.isEmpty()) "Enter the current reading" else "${amountText(units)} units × ${inr(num(rateValue))}",
                            style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, modifier = Modifier.weight(1f)
                        )
                        Text(if (cur.isEmpty()) "—" else inr(electricity), style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
                    }
                    FileField(
                        label = "Meter photo (optional)",
                        emptyTitle = "Upload a photo of the meter",
                        emptyHint = "Take a clear photo of the reading. You can crop it before saving.",
                        picked = photo,
                        savedUrl = if (isEdit && !photoCleared) editBill?.electricity?.meterPhoto else null,
                        allowPdf = false,
                        onPick = { photo = it; photoCleared = false },
                        onClear = { photo = null; photoCleared = true }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Other charges", style = RfType.LabelMd, color = Rf.OnSurface)
                    other.forEachIndexed { index, row ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RfTextField(
                                label = "", value = row.name, onValueChange = { v -> other = other.mapIndexed { i, c -> if (i == index) c.copy(name = v.take(100)) else c } },
                                icon = Lucide.Receipt, placeholder = "Charge name", modifier = Modifier.weight(1.4f)
                            )
                            RfTextField(
                                label = "", value = row.amount, onValueChange = { v -> other = other.mapIndexed { i, c -> if (i == index) c.copy(amount = decimalInput(v)) else c } },
                                icon = Lucide.IndianRupee, placeholder = "0", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f)
                            )
                            Box(
                                Modifier.size(40.dp).pressable({ other = other.filterIndexed { i, _ -> i != index } }).clip(RoundedCornerShape(10.dp)).background(Rf.ErrorContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                LIcon(Lucide.Trash2, size = 16.dp, tint = Rf.Error, contentDescription = "Remove charge")
                            }
                        }
                    }
                    shown["other"]?.let { Text(it, style = RfType.LabelSm, color = Rf.Error) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        (QuickCharges + "Other").forEach { chargeName ->
                            QuickChip("+ $chargeName") { other = other + Charge(if (chargeName == "Other") "" else chargeName, "") }
                        }
                    }
                }
            }
        }

        // What the existing bill holds
        if (existing != null && !isPaidUp) {
            FormSection(Lucide.FileText, "In this bill") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SummaryLine("Rent", inr(existing.rent))
                    existing.electricity?.let { SummaryLine("Electricity (${amountText(it.unitsConsumed)} units × ${inr(it.ratePerUnit)})", inr(it.amount)) }
                    existing.otherCharges.forEach { SummaryLine(it.name, inr(it.amount)) }
                    if (existing.creditApplied > 0) SummaryLine("Advance used", "− ${inr(existing.creditApplied)}", labelColor = Rf.Primary)
                    SummaryLine("Received so far", inr(existing.paidAmount))
                    if (existing.adjustedAmount > 0) SummaryLine("Expenses taken off", "− ${inr(existing.adjustedAmount)}")
                }
            }
        }

        if (showPayment) {
            FormSection(
                Lucide.Banknote,
                when {
                    isEdit -> "Received"
                    advanceMode -> "Advance received"
                    existing != null -> "Received now"
                    else -> "Payment received (optional)"
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RfTextField(
                        label = if (isEdit) "Total received so far" else if (advanceMode) "Advance amount" else "Amount received",
                        value = recv, onValueChange = { recv = decimalInput(it) }, icon = Lucide.IndianRupee,
                        placeholder = "0", keyboardType = KeyboardType.Decimal, error = shown["recv"]
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (advanceMode && existing != null) {
                            QuickChip("One month’s rent ${inr(existing.rent)}") { recv = amountText(existing.rent) }
                            QuickChip("Clear") { recv = "" }
                        } else {
                            QuickChip("Full amount", selected = fullAmount > 0 && received == fullAmount) { recv = if (fullAmount > 0) amountText(fullAmount) else "" }
                            QuickChip("Half") { recv = if (fullAmount > 0) (fullAmount / 2).roundToLong().toString() else "" }
                            QuickChip("Not received yet") { recv = "" }
                        }
                    }
                }
                if (added > 0) {
                    Notice(
                        if (advanceMode) "${inr(added)} will be kept as advance" else "${inr(added)} more than what’s due",
                        if (goesTo.isNotEmpty()) {
                            "The ${monthList(goesTo.map { it.first })} bill${if (goesTo.size > 1) "s use" else " uses"} ${inr(goesTo.sumOf { it.second })} of it" +
                                (if (kept > 0) ", and ${inr(kept)} waits for the next bill." else ".")
                        } else {
                            (if (advanceMode) "" else "This bill is settled and the extra is kept as advance. ") + "It comes off the next bill that has money due."
                        },
                        tone = NoticeTone.Advance
                    )
                    if (earlierDue.isNotEmpty()) {
                        Notice(
                            "${monthList(earlierDue.map { it.monthYear })} still due",
                            "${inr(earlierDue.sumOf { it.pendingAmount })} is unpaid from earlier months. Advance only goes to later bills, so collect that separately.",
                            tone = NoticeTone.Warn
                        )
                    }
                }
                if (received > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Method", style = RfType.LabelMd, color = Rf.OnSurface)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Methods.forEach { (value, label, icon) ->
                                MethodButton(label, icon, method == value, Modifier.weight(1f)) { method = value }
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DateField("Payment date", LocalDate.parse(dateText), { dateText = it.toString() })
                        shown["date"]?.let { Text(it, style = RfType.LabelSm, color = Rf.Error) }
                    }
                    FileField(
                        label = "Proof (optional)",
                        emptyTitle = "Upload a screenshot or PDF",
                        emptyHint = "A photo of the receipt or a PDF. Photos can be cropped.",
                        picked = proof,
                        savedUrl = null,
                        allowPdf = true,
                        onPick = { proof = it },
                        onClear = { proof = null }
                    )
                }
                TextArea(
                    if (existing != null) "Note on this payment (optional)" else "Notes (optional)",
                    notesValue, { notes = it.take(1000) }, "e.g. Balance to be paid next week"
                )
            }
        }

        // Editing: the bill's note (each payment keeps its own)
        if (isEdit) {
            FormSection(Lucide.FileText, "Note") {
                TextArea("Notes (optional)", notesValue, { notes = it.take(1000) }, "e.g. Electricity from the new meter")
            }
        }

        // Summary
        RfCard(shape = RoundedCornerShape(16.dp), color = Rf.Low) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Caps("Bill summary • ${monthLabel(monthText)}")
                SummaryLine("Rent", inr(existing?.rent ?: num(rentValue)))
                if (if (existing != null) existing.electricity != null else elecOn && cur.isNotEmpty()) SummaryLine("Electricity (${amountText(units)} units)", inr(electricity))
                if (otherTotal > 0) SummaryLine("Other charges", inr(otherTotal))
                SummaryLine("Total due", inr(total), bold = true, top = true)
                if (credit > 0) {
                    SummaryLine("Advance used", "− ${inr(credit)}", labelColor = Rf.Primary)
                    SummaryLine("To pay", inr(payable), bold = true)
                }
                if (existing != null && showPayment) {
                    SummaryLine("Received before", inr(paidBefore))
                    if (received > 0) SummaryLine("Received now", "+ ${inr(received)}", valueColor = Rf.Secondary)
                } else if (isEdit && editBill != null) {
                    SummaryLine("Received", inr(editBill.paidAmount))
                    if (editBill.adjustedAmount > 0) SummaryLine("Expenses taken off", "− ${inr(editBill.adjustedAmount)}")
                } else {
                    SummaryLine("Received", inr(received))
                }
                SummaryLine("Still due", inr(pendingAfter), bold = true, valueColor = if (pendingAfter > 0) Rf.Error else Rf.Secondary)
            }
        }
    }
}

/** "Oct 2026", "Oct 2026 and Nov 2026", "Aug 2026, Sep 2026 and Oct 2026". */
private fun monthList(months: List<String>): String {
    val labels = months.distinct().sorted().map(::monthLabel)
    return if (labels.size > 1) "${labels.dropLast(1).joinToString(", ")} and ${labels.last()}" else labels.firstOrNull().orEmpty()
}

/* ------------------------------ Form parts ------------------------------ */

private enum class NoticeTone { Info, Ok, Warn, Advance }

@Composable
private fun Notice(title: String, body: String, tone: NoticeTone = NoticeTone.Info) {
    val (background, titleColor, icon) = when (tone) {
        NoticeTone.Info -> Triple(Rf.Low, Rf.OnSurface, Lucide.Info)
        NoticeTone.Ok -> Triple(Rf.MintSoft, Rf.Secondary, Lucide.CircleCheck)
        NoticeTone.Warn -> Triple(Rf.AmberSoft, Rf.Amber, Lucide.TriangleAlert)
        NoticeTone.Advance -> Triple(Rf.PrimaryFixed, Rf.Primary, Lucide.Wallet)
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(background).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        LIcon(icon, size = 16.dp, tint = titleColor, modifier = Modifier.padding(top = 1.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = titleColor)
            Text(body, style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium, lineHeight = 16.sp), color = Rf.OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun QuickChip(label: String, selected: Boolean = false, onClick: () -> Unit) {
    Text(
        label, style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Primary,
        modifier = Modifier.pressable(onClick, pressScale = 0.96f).clip(CircleShape)
            .background(if (selected) Rf.PrimaryFixed else Rf.Lowest)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Rf.Primary else Rf.OutlineVariant, CircleShape)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    )
}

@Composable
private fun MethodButton(label: String, icon: LucideIcon, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(11.dp)
    Row(
        modifier.height(42.dp).pressable(onClick, pressScale = 0.97f).clip(shape)
            .background(if (selected) Rf.PrimaryFixed else Rf.Lowest)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Rf.Primary else Rf.OutlineVariant, shape),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LIcon(icon, size = 16.dp, tint = if (selected) Rf.Primary else Rf.OnSurfaceVariant)
        Text(label, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = if (selected) Rf.Primary else Rf.OnSurfaceVariant)
    }
}

@Composable
private fun SoftButton(label: String, icon: LucideIcon, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().height(42.dp).pressable(onClick, pressScale = 0.97f).clip(RoundedCornerShape(12.dp)).background(Rf.Low),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LIcon(icon, size = 15.dp, tint = Rf.Primary)
        Text(label, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.Primary)
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    bold: Boolean = false,
    top: Boolean = false,
    labelColor: Color? = null,
    valueColor: Color? = null
) {
    if (top) HorizontalDivider(thickness = 1.dp, color = Rf.OutlineVariant, modifier = Modifier.padding(vertical = 2.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            label, style = RfType.LabelMd.copy(fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium),
            color = labelColor ?: if (bold) Rf.OnSurface else Rf.OnSurfaceVariant, modifier = Modifier.weight(1f)
        )
        Text(
            value, style = RfType.LabelMd.copy(fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Bold),
            color = valueColor ?: Rf.OnSurface
        )
    }
}

/**
 * A meter photo or payment proof: an empty dashed box to add one, or the file with
 * "Change" and "Remove". [picked] is a new file, [savedUrl] the one saved on the bill.
 */
@Composable
private fun FileField(
    label: String,
    emptyTitle: String,
    emptyHint: String,
    picked: Uri?,
    savedUrl: String?,
    allowPdf: Boolean,
    onPick: (Uri) -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    var viewing by remember { mutableStateOf(false) }
    val isPdf = picked?.let { Uploads.isPdf(context, it) } ?: isPdfLink(savedUrl)
    val has = picked != null || savedUrl != null
    val choose = photoChooser(
        title = label.removeSuffix(" (optional)"),
        onView = if (has && !isPdf) ({ viewing = true }) else null,
        onRemove = if (has) onClear else null,
        removeLabel = "Remove file",
        removeHint = "Save the bill without it",
        onPdf = if (allowPdf) onPick else null
    ) { uris -> onPick(uris.first()) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = RfType.LabelMd, color = Rf.OnSurface)
        if (!has) {
            Row(
                Modifier.fillMaxWidth().pressable(choose, pressScale = 0.98f).dashedBorder(Rf.OutlineVariant).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Rf.PrimaryFixed), contentAlignment = Alignment.Center) {
                    LIcon(if (allowPdf) Lucide.Receipt else Lucide.Camera, size = 17.dp, tint = Rf.Primary)
                }
                Column(Modifier.weight(1f)) {
                    Text(emptyTitle, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface)
                    Text(emptyHint, style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Rf.Low).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(width = 64.dp, height = 52.dp).pressable({
                        if (isPdf) {
                            val target = picked ?: savedUrl?.let(Uri::parse)
                            if (target != null) runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(target, Uploads.PDF).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                            }
                        } else viewing = true
                    }).clip(RoundedCornerShape(8.dp)).background(Rf.Lowest),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPdf) LIcon(Lucide.FileText, size = 24.dp, tint = Rf.Error)
                    else RemoteImage(picked ?: savedUrl, Modifier.fillMaxSize(), placeholderIcon = Lucide.Image, iconSize = 18.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isPdf) "PDF added" else "Photo added", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface
                    )
                    Text(if (picked != null) "Uploads when you save" else "Saved on this bill", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
                }
                Text(
                    "Change", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Primary,
                    modifier = Modifier.pressable(choose).padding(horizontal = 6.dp, vertical = 8.dp)
                )
            }
        }
    }
    if (viewing) {
        val shown = picked?.let { Photo.Picked(it) } ?: savedUrl?.let { Photo.Stored("", it) }
        if (shown != null) PhotoViewer(listOf(shown), 0, onClose = { viewing = false })
    }
}
