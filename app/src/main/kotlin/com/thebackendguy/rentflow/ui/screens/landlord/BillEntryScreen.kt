package com.thebackendguy.rentflow.ui.screens.landlord

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.thebackendguy.rentflow.data.DeductionCategories
import com.thebackendguy.rentflow.data.PaymentStore
import com.thebackendguy.rentflow.data.apiDate
import com.thebackendguy.rentflow.data.categoryLabel
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.methodLabel
import com.thebackendguy.rentflow.data.monthLabel
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.BillDto
import com.thebackendguy.rentflow.data.remote.ReceiptDto
import com.thebackendguy.rentflow.data.remote.UploadFolder
import com.thebackendguy.rentflow.data.remote.Uploads
import com.thebackendguy.rentflow.data.round2
import com.thebackendguy.rentflow.data.shortLabel
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.ConfirmSheet
import com.thebackendguy.rentflow.ui.components.DateField
import com.thebackendguy.rentflow.ui.components.ErrorState
import com.thebackendguy.rentflow.ui.components.LoadingCards
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.RfTextField
import com.thebackendguy.rentflow.ui.components.Spinner
import com.thebackendguy.rentflow.ui.components.TextArea
import com.thebackendguy.rentflow.ui.components.Toasts
import com.thebackendguy.rentflow.ui.components.TonalButton
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.screens.tenant.MonthLeaf
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.put
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

/*
 * One entry of a bill, full screen: a payment the tenant made, or an expense the
 * tenant paid for the landlord and took off the rent (a deduction). The web's
 * ReceiptDialog: add one, or change or remove a saved one.
 */

/** What the entry form opens for: a new payment, a new expense deduction, or "entry:<id>" to change one. */
const val ENTRY_PAYMENT = "payment"
const val ENTRY_DEDUCTION = "deduction"
const val EDIT_ENTRY = "entry:"

/** The bill sheet that opened the form, opened again when the user comes back to the list. */
internal object ReopenBill {
    private var id: String? = null

    fun set(billId: String) {
        id = billId
    }

    fun take(): String? = id.also { id = null }
}

/**
 * An entry's number among its kind, oldest = 1 ("Payment 2", "Expense 1"). The API
 * sends a bill's entries newest first.
 */
internal fun entryNumber(bill: BillDto, receipt: ReceiptDto): Int {
    val same = bill.receipts.filter { it.kind == receipt.kind }
    val index = same.indexOf(receipt)
    return if (index < 0) 1 else same.size - index
}

/** "UPI payment", or the expense's own name. */
internal fun entryTitle(bill: BillDto, receipt: ReceiptDto): String =
    if (receipt.isDeduction) receipt.title?.takeIf(String::isNotBlank) ?: "Expense ${entryNumber(bill, receipt)}"
    else "${methodLabel(receipt.method)} payment"

/** What the bill comes to once an entry is added, changed ([receipt] given) or removed ([amount] 0): the web's billAfterReceipt. */
internal data class EntryPreview(val pending: Double, val advance: Double, val status: String)

internal fun billAfterEntry(bill: BillDto, receipt: ReceiptDto?, amount: Double): EntryPreview {
    val before = bill.settledByTenant - (receipt?.amount ?: 0.0)
    val settled = round2(before + amount)
    val need = round2(max(bill.totalDue - settled, 0.0))
    val credit = min(bill.creditApplied, need)
    val pending = round2(need - credit)
    val advance = round2(max(settled - bill.totalDue, 0.0))
    val status = when {
        settled + credit <= 0 -> "pending"
        pending > 0 -> "partial"
        else -> "paid"
    }
    return EntryPreview(pending, advance, status)
}

private val StatusLabels = mapOf("paid" to "Paid", "partial" to "Partial", "pending" to "Pending")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillEntryScreen(billId: String, mode: String, shell: Shell, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var retry by remember { mutableIntStateOf(0) }
    var bill by remember { mutableStateOf<BillDto?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(billId, retry) {
        loadError = null
        when (val r = PaymentStore.bill(billId)) {
            is ApiResult.Ok -> bill = r.value
            is ApiResult.Fail -> loadError = r.message
        }
    }

    val receiptId = mode.takeIf { it.startsWith(EDIT_ENTRY) }?.removePrefix(EDIT_ENTRY)
    val isEdit = receiptId != null
    val receipt = receiptId?.let { id -> bill?.receipts?.find { it.id == id } }

    var kind by rememberSaveable { mutableStateOf(if (mode == ENTRY_DEDUCTION) ENTRY_DEDUCTION else ENTRY_PAYMENT) }
    var amount by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var method by rememberSaveable { mutableStateOf("upi") }
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("maintenance") }
    var notes by rememberSaveable { mutableStateOf("") }
    var proof by rememberSaveable { mutableStateOf<Uri?>(null) }
    var proofCleared by rememberSaveable { mutableStateOf(false) }
    var filled by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }

    // The form starts from the saved entry, or (a new payment) from what is still due
    LaunchedEffect(bill, receipt) {
        val b = bill ?: return@LaunchedEffect
        if (filled) return@LaunchedEffect
        if (isEdit && receipt == null) return@LaunchedEffect
        filled = true
        if (receipt != null) {
            kind = receipt.kind
            amount = amountText(receipt.amount)
            dateText = (apiDate(receipt.date) ?: LocalDate.now()).toString()
            method = receipt.method ?: "upi"
            title = receipt.title.orEmpty()
            category = receipt.category ?: "other"
            notes = receipt.notes.orEmpty()
        } else if (kind == ENTRY_PAYMENT && b.pendingAmount > 0) {
            amount = amountText(b.pendingAmount)
        }
    }

    val deduction = kind == ENTRY_DEDUCTION
    val current = bill
    val pageTitle = when {
        isEdit -> if (deduction) "Edit expense deduction" else "Edit payment"
        deduction -> "Deduct an expense"
        current != null && current.pendingAmount <= 0 -> "Add an advance"
        else -> "Add payment"
    }

    if (current == null || (isEdit && receipt == null)) {
        AppPage(topBar = { shell.TopBar(pageTitle, onBack = onBack) }) {
            when {
                loadError != null -> ErrorState(loadError.orEmpty(), onRetry = { retry++ })
                current == null -> LoadingCards(2)
                else -> NotFound(if (deduction) "Expense" else "Payment", onBack)
            }
        }
        return
    }

    val value = round2(num(amount))
    val after = billAfterEntry(current, receipt, value)
    val errors = mutableMapOf<String, String>()
    if (value <= 0) errors["amount"] = "Enter an amount more than 0"
    if (LocalDate.parse(dateText).isAfter(LocalDate.now())) errors["date"] = "The date can’t be in the future"
    if (deduction && title.isBlank()) errors["title"] = "Say what the expense was for"
    val shown = if (submitted) errors else emptyMap()

    fun save() {
        submitted = true
        if (errors.isNotEmpty() || saving) return
        saving = true
        scope.launch {
            // A new file is sent as its key; a saved one removed while editing as null
            val key = proof?.let { uri ->
                when (val r = Uploads.upload(context, uri, UploadFolder.PaymentProof)) {
                    is ApiResult.Fail -> { Toasts.error(r.message); saving = false; return@launch }
                    is ApiResult.Ok -> r.value
                }
            }
            val result = if (receipt?.id != null) {
                PaymentStore.updateReceipt(current.id, receipt.id) {
                    put("amount", value)
                    put("date", dateText)
                    put("notes", notes.trim())
                    if (deduction) {
                        put("title", title.trim())
                        put("category", category)
                    } else {
                        put("method", method)
                    }
                    when {
                        key != null -> put("proof", key)
                        proofCleared -> put("proof", JsonNull)
                    }
                }
            } else {
                PaymentStore.addReceipt(current.id) {
                    put("kind", kind)
                    put("amount", value)
                    put("date", dateText)
                    notes.trim().takeIf(String::isNotEmpty)?.let { put("notes", it) }
                    if (deduction) {
                        put("title", title.trim())
                        put("category", category)
                    } else {
                        put("method", method)
                    }
                    key?.let { put("proof", it) }
                }
            }
            when (result) {
                is ApiResult.Ok -> {
                    Toasts.show(result.value.message ?: if (deduction) "Expense deduction saved" else "Payment saved")
                    onDone()
                }
                is ApiResult.Fail -> Toasts.error(result.message)
            }
            saving = false
        }
    }

    AppPage(
        topBar = { shell.TopBar(pageTitle, onBack = onBack) },
        bottomBar = {
            EntryBar(
                saveLabel = when {
                    isEdit -> "Save changes"
                    value > 0 -> "${if (deduction) "Deduct" else "Save"} ${inr(value)}"
                    else -> if (deduction) "Deduct" else "Save"
                },
                saveColor = if (deduction) Rf.Amber else Rf.Primary,
                saving = saving,
                onSave = ::save,
                onCancel = onBack,
                onRemove = if (receipt?.id != null) ({ confirmRemove = true }) else null
            )
        }
    ) {
        // Whose bill it is
        RfCard(shape = RoundedCornerShape(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MonthLeaf(current.monthYear, short = true)
                Column(Modifier.weight(1f)) {
                    Text(
                        listOfNotNull(current.tenant?.fullName, current.room?.roomNumber?.let { "Room $it" }).joinToString(" • "),
                        style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface
                    )
                    Text(
                        if (receipt != null) "${if (deduction) "Expense" else "Payment"} ${entryNumber(current, receipt)} of the ${monthLabel(current.monthYear)} bill"
                        else "Total ${inr(current.totalDue)} • still due ${inr(current.pendingAmount)}",
                        style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
                    )
                }
            }
        }

        // Payment or expense, while adding; a saved entry keeps its kind
        if (!isEdit) {
            KindSwitch(deduction) { toDeduction ->
                if (toDeduction != deduction) {
                    kind = if (toDeduction) ENTRY_DEDUCTION else ENTRY_PAYMENT
                    amount = if (!toDeduction && current.pendingAmount > 0) amountText(current.pendingAmount) else ""
                    submitted = false
                }
            }
        }
        if (deduction) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Rf.AmberSoft).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LIcon(Lucide.Info, size = 16.dp, tint = Rf.Amber, modifier = Modifier.padding(top = 1.dp))
                Text(
                    "Money the tenant spent for you, like a plumber or a repair. It is taken off this bill and is not counted as cash received.",
                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurface
                )
            }
        }

        RfCard(shape = RoundedCornerShape(14.dp), spacing = 14.dp) {
            if (deduction) {
                RfTextField(
                    label = "What was it for?", value = title, onValueChange = { title = it.take(100) }, icon = Lucide.Wrench,
                    placeholder = "e.g. Bathroom tap replaced", capitalization = KeyboardCapitalization.Sentences, error = shown["title"]
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Category", style = RfType.LabelMd, color = Rf.OnSurface)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DeductionCategories.forEach { (key, label) -> CategoryChip(label, category == key) { category = key } }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RfTextField(
                    label = if (deduction) "Amount the tenant paid" else "Amount received",
                    value = amount, onValueChange = { amount = decimalInput(it) }, icon = Lucide.IndianRupee,
                    placeholder = "0", keyboardType = KeyboardType.Decimal, error = shown["amount"]
                )
                if (!isEdit && !deduction && current.pendingAmount > 0 && value != current.pendingAmount) {
                    Text(
                        "Still due ${inr(current.pendingAmount)}", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Primary,
                        modifier = Modifier.pressable({ amount = amountText(current.pendingAmount) }).clip(CircleShape)
                            .background(Rf.PrimaryFixed).padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DateField(if (deduction) "Date of the expense" else "Date received", LocalDate.parse(dateText), { dateText = it.toString() })
                shown["date"]?.let { Text(it, style = RfType.LabelSm, color = Rf.Error) }
            }
            if (!deduction) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("How was it paid?", style = RfType.LabelMd, color = Rf.OnSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Methods.forEach { (key, label, icon) -> MethodButton(label, icon, method == key, Modifier.weight(1f)) { method = key } }
                    }
                }
            }
            FileField(
                label = if (deduction) "Shop bill (optional)" else "Photo (optional)",
                emptyTitle = if (deduction) "Add the shop bill" else "Add a screenshot or PDF",
                emptyHint = "A photo or a PDF. Photos can be cropped.",
                picked = proof,
                savedUrl = if (!proofCleared) receipt?.proof else null,
                allowPdf = true,
                onPick = { proof = it; proofCleared = false },
                onClear = { proof = null; proofCleared = true }
            )
            TextArea(
                "Note (optional)", notes, { notes = it.take(1000) },
                if (deduction) "e.g. Tenant called the plumber, agreed on phone" else "e.g. Rest after salary"
            )
        }

        // What the bill comes to once this is saved
        Text(
            buildPreview(after),
            style = RfType.LabelMd.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurface,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Rf.Low).padding(12.dp)
        )
    }

    if (confirmRemove && receipt?.id != null) {
        val gone = billAfterEntry(current, receipt, 0.0)
        val more = round2(gone.pending - current.pendingAmount)
        val what = if (receipt.isDeduction) "expense “${entryTitle(current, receipt)}”" else "${methodLabel(receipt.method)} payment"
        ConfirmSheet(
            icon = Lucide.Trash2,
            title = if (receipt.isDeduction) "Remove this deduction?" else "Remove this payment?",
            message = "Remove the ${inr(receipt.amount)} $what of ${apiDate(receipt.date)?.shortLabel() ?: "this bill"}?" +
                if (more > 0) " The bill shows ${inr(more)} more due." else " The bill’s advance goes down by as much.",
            confirmLabel = if (receipt.isDeduction) "Remove deduction" else "Remove payment",
            busy = removing,
            onConfirm = {
                removing = true
                scope.launch {
                    when (val r = PaymentStore.deleteReceipt(current.id, receipt.id)) {
                        is ApiResult.Ok -> {
                            Toasts.show(r.value.message ?: if (receipt.isDeduction) "Deduction removed" else "Payment removed")
                            confirmRemove = false
                            onDone()
                        }
                        is ApiResult.Fail -> Toasts.error(r.message)
                    }
                    removing = false
                }
            },
            onDismiss = { confirmRemove = false }
        )
    }
}

private fun buildPreview(after: EntryPreview): String =
    "After saving: " + (if (after.advance > 0) "nothing due, ${inr(after.advance)} kept as advance for the next bill" else "still due ${inr(after.pending)}") +
        " • status ${StatusLabels[after.status] ?: after.status}"

/** Payment or Expense deduction, as two halves of one switch. */
@Composable
private fun KindSwitch(deduction: Boolean, onSelect: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Rf.Low).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(false to ("Payment" to Lucide.Banknote), true to ("Expense deduction" to Lucide.Wrench)).forEach { (isDeduction, labelIcon) ->
            val on = isDeduction == deduction
            val tint = if (!on) Rf.OnSurfaceVariant else if (isDeduction) Rf.Amber else Rf.Primary
            Row(
                Modifier.weight(1f).height(40.dp).pressable({ onSelect(isDeduction) }, pressScale = 0.97f).clip(RoundedCornerShape(9.dp))
                    .background(if (on) Rf.Lowest else Color.Transparent),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LIcon(labelIcon.second, size = 15.dp, tint = tint)
                Text(labelIcon.first, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = tint)
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.height(36.dp).pressable(onClick, pressScale = 0.96f).clip(shape)
            .background(if (selected) Rf.AmberSoft else Rf.Lowest)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Rf.Amber else Rf.OutlineVariant, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (selected) LIcon(Lucide.Check, size = 14.dp, tint = Rf.Amber)
        Text(label, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = if (selected) Rf.Amber else Rf.OnSurface)
    }
}

/** Remove (when changing a saved entry), Cancel and Save, pinned to the bottom like the app's form bar. */
@Composable
private fun EntryBar(
    saveLabel: String,
    saveColor: Color,
    saving: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onRemove: (() -> Unit)?
) {
    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    Row(
        Modifier
            .zIndex(1f)
            .shadow(12.dp, shape, ambientColor = Rf.Shadow, spotColor = Rf.Shadow)
            .background(Rf.Lowest, shape)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onRemove != null) {
            Box(
                Modifier.size(48.dp).pressable(onRemove).clip(RoundedCornerShape(12.dp)).background(Rf.ErrorContainer),
                contentAlignment = Alignment.Center
            ) {
                LIcon(Lucide.Trash2, size = 18.dp, tint = Rf.Error, contentDescription = "Remove")
            }
        }
        TonalButton("Cancel", onClick = onCancel, modifier = Modifier.width(100.dp))
        Row(
            Modifier.weight(1f).height(48.dp).pressable(onSave, pressScale = 0.98f).clip(RoundedCornerShape(12.dp)).background(saveColor),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (saving) Spinner(Color.White, size = 16.dp)
            Text(if (saving) "Saving…" else saveLabel, style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Color.White)
        }
    }
}

