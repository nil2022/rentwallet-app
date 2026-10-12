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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebackendguy.rentflow.data.CollectionBills
import com.thebackendguy.rentflow.data.Loadable
import com.thebackendguy.rentflow.data.PaymentStore
import com.thebackendguy.rentflow.data.Photo
import com.thebackendguy.rentflow.data.SettledSplit
import com.thebackendguy.rentflow.data.settledSplit
import com.thebackendguy.rentflow.data.splitSettled
import com.thebackendguy.rentflow.data.apiDate
import com.thebackendguy.rentflow.data.categoryLabel
import com.thebackendguy.rentflow.data.apiMonth
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.isOverdue
import com.thebackendguy.rentflow.data.isPdfLink
import com.thebackendguy.rentflow.data.label
import com.thebackendguy.rentflow.data.methodLabel
import com.thebackendguy.rentflow.data.monthLabel
import com.thebackendguy.rentflow.data.parseApiMonth
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.BillDto
import com.thebackendguy.rentflow.data.remote.ReceiptDto
import com.thebackendguy.rentflow.data.remote.PropertyDto
import com.thebackendguy.rentflow.data.remote.TenantDto
import com.thebackendguy.rentflow.data.shortLabel
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.ErrorState
import com.thebackendguy.rentflow.ui.components.Fab
import com.thebackendguy.rentflow.ui.components.LoadingCards
import com.thebackendguy.rentflow.ui.components.Pill
import com.thebackendguy.rentflow.ui.components.PhotoViewer
import com.thebackendguy.rentflow.ui.components.RemoteImage
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.components.softShadow
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import com.thebackendguy.rentflow.ui.components.photoChooser
import com.thebackendguy.rentflow.ui.components.Toasts
import com.thebackendguy.rentflow.ui.components.Spinner
import com.thebackendguy.rentflow.ui.components.ConfirmSheet
import com.thebackendguy.rentflow.data.round2
import com.thebackendguy.rentflow.data.remote.Uploads
import com.thebackendguy.rentflow.data.remote.UploadFolder
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

/*
 * The Payments tab: the web's phone Rent Collection page (RentCollection.jsx).
 * One month's bills with their totals, the bills still unpaid from earlier months
 * (the overdue view), and the tenants in a room who have no bill for the month.
 */

/** The month, property, status and view last looked at, kept while the app runs so coming back from a form shows them again. */
private object CollectionMemory {
    var month: String? = null
    var propertyId: String? = null
    var filter: Int = 0
    var overdueView: Boolean = false
}

/** Opens Rent Collection on [month] ("YYYY-MM") and [propertyId], as Rent History's months do. */
internal fun showCollectionMonth(month: String, propertyId: String?) {
    CollectionMemory.month = month
    CollectionMemory.propertyId = propertyId
    CollectionMemory.filter = 0
    CollectionMemory.overdueView = false
}

/** Opens Rent Collection on the bills still unpaid from earlier months, across all properties. */
internal fun showOverdueBills() {
    CollectionMemory.propertyId = null
    CollectionMemory.filter = 0
    CollectionMemory.overdueView = true
}

private enum class BillFilter(val key: String, val label: String) {
    All("all", "All"), Pending("pending", "Pending"), Partial("partial", "Partial"), Paid("paid", "Paid")
}

/**
 * [onRecord] starts Record Payment with "Who is it for?"; [onRecordFor] opens the form
 * for a tenant and month ("YYYY-MM"), from Collect or Create bill; [onEditBill] opens it on a saved bill.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RentCollectionScreen(
    shell: Shell,
    onRecord: () -> Unit,
    onRecordFor: (tenantId: String, month: String) -> Unit,
    onBillEntry: (billId: String, mode: String) -> Unit,
    onEditBill: (tenantId: String, billId: String) -> Unit,
    onHistory: () -> Unit
) {
    val portfolioState = portfolioState()
    val now = YearMonth.now()
    var monthText by rememberSaveable { mutableStateOf(CollectionMemory.month ?: now.apiMonth()) }
    var propertyId by rememberSaveable { mutableStateOf(CollectionMemory.propertyId) }
    var filter by rememberSaveable { mutableIntStateOf(CollectionMemory.filter) }
    var search by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var overdueView by rememberSaveable { mutableStateOf(CollectionMemory.overdueView) }
    var notBilledOpen by rememberSaveable { mutableStateOf(false) }
    // Coming back from a bill's entry form opens that bill again
    var openBillId by rememberSaveable { mutableStateOf(ReopenBill.take()) }
    var choosingMonth by remember { mutableStateOf(false) }
    var choosingProperty by remember { mutableStateOf(false) }
    val month = parseApiMonth(monthText) ?: now
    LaunchedEffect(monthText, propertyId, filter, overdueView) {
        CollectionMemory.month = monthText
        CollectionMemory.propertyId = propertyId
        CollectionMemory.filter = filter
        CollectionMemory.overdueView = overdueView
    }
    // A property remembered from another account (or since deleted) no longer applies
    LaunchedEffect(portfolioState.data, propertyId) {
        val portfolio = portfolioState.data ?: return@LaunchedEffect
        if (propertyId != null && portfolio.property(propertyId) == null) propertyId = null
    }

    // The bills for this month and property, loaded again after any bill changes
    val version by PaymentStore.version.collectAsState()
    var retry by remember { mutableIntStateOf(0) }
    var loaded by remember { mutableStateOf(Loadable<CollectionBills>(loading = true)) }
    LaunchedEffect(monthText, propertyId, version, retry) {
        loaded = loaded.copy(loading = true, error = null)
        loaded = when (val result = PaymentStore.collection(month, propertyId)) {
            is ApiResult.Ok -> Loadable(result.value)
            is ApiResult.Fail -> loaded.copy(loading = false, error = result.message)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AppPage(topBar = { shell.TopBar("Payments") }, bottomBar = { shell.BottomNav(4) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Rent Collection", style = RfType.HeadlineMd.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
                        Text(
                            "Bills and payments across your properties.", style = RfType.BodySm, color = Rf.OnSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Row(
                        Modifier.height(36.dp).pressable(onHistory).softShadow(RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp))
                            .background(Rf.Lowest).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        LIcon(Lucide.History, size = 15.dp, tint = Rf.Primary)
                        Text("History", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.Primary)
                    }
                }

                val data = loaded.data
                if (data == null) {
                    if (loaded.error != null) ErrorState(loaded.error.orEmpty(), onRetry = { retry++ }) else LoadingCards(3)
                    return@Column
                }

                val properties = portfolioState.data?.properties.orEmpty()
                val bills = if (overdueView) data.overdue else data.bills
                val totals = data.totals
                // The covered part of the bills, split into money (received plus advance used) and expenses taken off
                val summary = if (!overdueView && totals != null) {
                    Totals(totals.totalDue, splitSettled(totals.totalSettled, totals.totalAdjusted), totals.totalPending, totals.totalAdvanceHeld)
                } else {
                    Totals(
                        billed = bills.sumOf { it.totalDue },
                        money = bills.fold(SettledSplit(0.0, 0.0)) { sum, bill -> sum + bill.settledSplit },
                        due = bills.sumOf { it.pendingAmount },
                        advance = bills.sumOf { it.advanceLeft }
                    )
                }
                val overdueAmount = data.overdue.sumOf { it.pendingAmount }
                val overdueTenants = data.overdue.map { it.tenantId }.distinct().size
                val statusFilter = BillFilter.entries[filter]
                val q = search.trim()
                val rows = bills.filter { bill ->
                    (overdueView || statusFilter == BillFilter.All || bill.paymentStatus == statusFilter.key) &&
                        (q.isEmpty() || "${bill.tenant?.fullName.orEmpty()} ${bill.room?.roomNumber.orEmpty()}".contains(q, ignoreCase = true))
                }
                // Tenants in a room (in the chosen property) with no bill for the month on screen
                val billed = data.bills.map { it.tenantId }.toSet()
                val notBilled = if (overdueView) emptyList() else portfolioState.data?.tenants.orEmpty()
                    .filter { it.roomId != null && (propertyId == null || it.propertyId == propertyId) && it.id !in billed }

                Column(Modifier.alpha(if (loaded.loading) 0.6f else 1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(
                        summary,
                        unpaid = bills.count { it.pendingAmount > 0 },
                        title = if (overdueView) "Collected on these bills" else "Collected in ${month.label()}"
                    )
                    if (!overdueView && overdueTenants > 0) {
                        OverdueLine(overdueAmount, overdueTenants) {
                            filter = BillFilter.All.ordinal
                            overdueView = true
                        }
                    }

                    // Month, property and search; the overdue view swaps the month for its own tag
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when {
                            searching -> {
                                Box(Modifier.weight(1f)) { SearchBox(search, "Search bills") { search = it } }
                                Text(
                                    "Cancel", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.Primary,
                                    modifier = Modifier.pressable({
                                        search = ""
                                        searching = false
                                    }).padding(horizontal = 4.dp)
                                )
                            }
                            overdueView -> {
                                OverdueTag(Modifier.weight(1f))
                                Text(
                                    "Back", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.Primary,
                                    modifier = Modifier.height(40.dp).pressable({ overdueView = false }).softShadow(RoundedCornerShape(11.dp))
                                        .clip(RoundedCornerShape(11.dp)).background(Rf.Lowest).padding(horizontal = 14.dp, vertical = 11.dp)
                                )
                                SquareButton(Lucide.Search, search.isNotEmpty(), "Search") { searching = true }
                            }
                            else -> {
                                MonthStepper(month, Modifier.weight(1f), onMonth = { monthText = it.apiMonth() }, onPick = { choosingMonth = true })
                                SquareButton(Lucide.Building2, propertyId != null, "Property") { choosingProperty = true }
                                SquareButton(Lucide.Search, search.isNotEmpty(), "Search") { searching = true }
                            }
                        }
                    }
                    if (!overdueView) {
                        StatusTabs(filter, counts = BillFilter.entries.map { f -> if (f == BillFilter.All) bills.size else bills.count { it.paymentStatus == f.key } }) {
                            filter = it
                        }
                    }
                    val propertyName = properties.find { it.id == propertyId }?.propertyName
                    if (propertyName != null || (q.isNotEmpty() && !searching) || (!overdueView && month != now)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (propertyName != null) FilterChip(propertyName) { propertyId = null }
                            if (q.isNotEmpty() && !searching) FilterChip("“$q”") { search = "" }
                            if (!overdueView && month != now) FilterChip("Back to this month", plain = true) { monthText = now.apiMonth() }
                        }
                    }

                    if (rows.isEmpty()) {
                        RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(28.dp)) {
                            Text("No bills found", style = RfType.BodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Text(
                                when {
                                    q.isNotEmpty() || statusFilter != BillFilter.All -> "Try a different search or status."
                                    overdueView -> "Nothing is overdue from earlier months."
                                    else -> "Nothing has been billed for ${month.label()} yet."
                                },
                                style = RfType.BodySm, color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        }
                    } else {
                        rows.forEach { bill ->
                            BillCard(bill, showMonth = overdueView, onOpen = { openBillId = bill.id }, onCollect = { onBillEntry(bill.id, ENTRY_PAYMENT) })
                        }
                    }

                    if (notBilled.isNotEmpty()) {
                        NotBilledCard(notBilled, month, notBilledOpen, onToggle = { notBilledOpen = !notBilledOpen }) { tenant ->
                            onRecordFor(tenant.id, month.apiMonth())
                        }
                    }
                    // Room for the floating Record button over the last card
                    Box(Modifier.height(56.dp))
                }

                if (choosingMonth) MonthSheet(month, onPick = { monthText = it.apiMonth() }, onDismiss = { choosingMonth = false })
                if (choosingProperty) {
                    PropertySheet(properties, propertyId, onPick = {
                        propertyId = it
                        choosingProperty = false
                    }, onDismiss = { choosingProperty = false })
                }
                val openBill = (data.bills + data.unpaid).find { it.id == openBillId }
                if (openBill != null) {
                    BillSheet(
                        openBill,
                        onDismiss = { openBillId = null },
                        onEntry = { mode ->
                            ReopenBill.set(openBill.id)
                            openBillId = null
                            onBillEntry(openBill.id, mode)
                        },
                        onEdit = {
                            openBillId = null
                            onEditBill(openBill.tenantId, openBill.id)
                        },
                        onDeleted = { openBillId = null }
                    )
                }
            }
        }
        // Floats above the bottom bar so it stays in reach while the list scrolls
        Fab("Record", Lucide.Plus, onRecord, Modifier.align(Alignment.BottomEnd))
    }
}

/** Billed, the covered part as money and expenses taken off, still due and advance not used yet. */
private data class Totals(val billed: Double, val money: SettledSplit, val due: Double, val advance: Double)

/* ------------------------------ Summary ------------------------------ */

@Composable
private fun SummaryCard(totals: Totals, unpaid: Int, title: String) {
    val (collected, adjusted) = totals.money
    // Settled: collected plus expenses taken off
    val percent = if (totals.billed > 0) ((collected + adjusted) / totals.billed * 100).roundToInt() else 0
    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Caps(title) }
            Text("$percent%", style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.Secondary)
        }
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(inr(collected), style = RfType.HeadlineMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.Secondary)
            Text("of ${inr(totals.billed)} billed", style = RfType.LabelMd, color = Rf.OnSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
        }
        if (adjusted > 0) {
            Text(
                "+ ${inr(adjusted)} expenses taken off", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.Amber,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        PartsBar(listOf(collected to Rf.Secondary, adjusted to Rf.Amber), totals.billed, 7.dp, Modifier.padding(top = 10.dp))
        HorizontalDivider(thickness = 1.dp, color = Rf.Low, modifier = Modifier.padding(top = 12.dp))
        Row(Modifier.padding(top = 10.dp).height(IntrinsicSize.Min)) {
            Column(Modifier.weight(1f)) {
                Caps("Still due")
                Text(inr(totals.due), style = RfType.BodyLg.copy(fontWeight = FontWeight.ExtraBold), color = if (totals.due > 0) Rf.Error else Rf.Secondary)
                Text("$unpaid unpaid ${if (unpaid == 1) "bill" else "bills"}", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(Rf.Low))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Caps("Advance held")
                Text(inr(totals.advance), style = RfType.BodyLg.copy(fontWeight = FontWeight.ExtraBold), color = if (totals.advance > 0) Rf.Primary else Rf.OnSurface)
                Text(
                    if (totals.advance > 0) "Kept for next bills" else "None held",
                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
                )
            }
        }
    }
}

/** The thin red line for unpaid bills from earlier months; tapping it opens the overdue view. */
@Composable
private fun OverdueLine(amount: Double, tenants: Int, onReview: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().pressable(onReview, pressScale = 0.98f).clip(shape)
            .background(Rf.ErrorContainer.copy(alpha = 0.55f)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Rf.ErrorContainer), contentAlignment = Alignment.Center) {
            LIcon(Lucide.TriangleAlert, size = 17.dp, tint = Rf.Error)
        }
        Column(Modifier.weight(1f)) {
            Text("${inr(amount)} overdue", style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.Error)
            Text(
                "$tenants ${if (tenants == 1) "tenant" else "tenants"} unpaid from earlier months",
                style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
            )
        }
        Text("Review", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.Error)
        LIcon(Lucide.ChevronRight, size = 15.dp, tint = Rf.Error)
    }
}

/* ------------------------------ Filters ------------------------------ */

private val ControlShape = RoundedCornerShape(11.dp)

/** ‹ Oct 2026 › — the label opens the month grid; no future months. */
@Composable
private fun MonthStepper(month: YearMonth, modifier: Modifier, onMonth: (YearMonth) -> Unit, onPick: () -> Unit) {
    val atCurrent = month >= YearMonth.now()
    Row(
        modifier.height(40.dp).softShadow(ControlShape).clip(ControlShape).background(Rf.Lowest).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepArrow(Lucide.ArrowLeft, "Previous month", enabled = true) { onMonth(month.minusMonths(1)) }
        Text(
            month.label(), style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).pressable(onPick, pressScale = 0.97f).padding(vertical = 10.dp)
        )
        StepArrow(Lucide.ArrowRight, "Next month", enabled = !atCurrent) { onMonth(month.plusMonths(1)) }
    }
}

@Composable
private fun StepArrow(icon: LucideIcon, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).pressable(if (enabled) onClick else null).clip(RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center
    ) {
        LIcon(icon, size = 16.dp, tint = if (enabled) Rf.Primary else Rf.OutlineVariant, contentDescription = description)
    }
}

/** A 40 dp square control; the dot says its filter is on. */
@Composable
private fun SquareButton(icon: LucideIcon, on: Boolean, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).pressable(onClick).softShadow(ControlShape).clip(ControlShape)
            .background(if (on) Rf.PrimaryFixed else Rf.Lowest),
        contentAlignment = Alignment.Center
    ) {
        LIcon(icon, size = 17.dp, tint = if (on) Rf.Primary else Rf.OnSurfaceVariant, contentDescription = description)
        if (on) Box(Modifier.align(Alignment.TopEnd).padding(7.dp).size(6.dp).clip(CircleShape).background(Rf.Primary))
    }
}

@Composable
private fun OverdueTag(modifier: Modifier) {
    Row(
        modifier.height(40.dp).clip(ControlShape).background(Rf.ErrorContainer).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LIcon(Lucide.TriangleAlert, size = 16.dp, tint = Rf.Error)
        Text("Earlier months", style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.Error, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** All / Pending / Partial / Paid, each with its count, sharing the width equally. */
@Composable
private fun StatusTabs(selected: Int, counts: List<Int>, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Rf.Low).padding(3.dp)) {
        BillFilter.entries.forEachIndexed { index, f ->
            val on = index == selected
            Column(
                Modifier.weight(1f).pressable(if (on) null else ({ onSelect(index) }), pressScale = 0.96f)
                    .then(if (on) Modifier.softShadow(RoundedCornerShape(10.dp)) else Modifier)
                    .clip(RoundedCornerShape(10.dp)).background(if (on) Rf.Lowest else Color.Transparent)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(f.label, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = if (on) Rf.Primary else Rf.OnSurfaceVariant)
                Text("${counts[index]}", style = RfType.LabelSm.copy(fontSize = 10.sp), color = if (on) Rf.Primary else Rf.OnSurfaceVariant)
            }
        }
    }
}

/** A filter that is on; tapping it takes it off. [plain] is a link-like chip without the cross. */
@Composable
private fun FilterChip(label: String, plain: Boolean = false, onClick: () -> Unit) {
    Pill(
        label,
        background = if (plain) Rf.Lowest else Rf.PrimaryFixed,
        content = Rf.Primary,
        trailingIcon = if (plain) null else Lucide.X,
        style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold),
        horizontal = 10.dp, vertical = 5.dp,
        modifier = Modifier.pressable(onClick).then(if (plain) Modifier.border(1.dp, Rf.OutlineVariant, CircleShape) else Modifier)
    )
}

/* ------------------------------ Bills ------------------------------ */

/** The web's phone bill card: who, status, total, received and due, and how much is covered. */
@Composable
private fun BillCard(bill: BillDto, showMonth: Boolean, onOpen: () -> Unit, onCollect: () -> Unit) {
    val name = bill.tenant?.fullName?.ifBlank { null } ?: "Tenant"
    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(14.dp), spacing = 10.dp, onClick = onOpen) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InitialsCircle(name)
            Column(Modifier.weight(1f)) {
                Text(name, style = RfType.BodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        monthLabel(bill.monthYear).takeIf { showMonth },
                        bill.room?.roomNumber?.let { "Room $it" },
                        bill.property?.propertyName
                    ).joinToString(" • "),
                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Box(Modifier.padding(top = 6.dp)) { BillStatusPills(bill) }
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(inr(bill.totalDue), style = RfType.HeadlineSm.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
                Text(
                    buildAnnotatedString {
                        append("Received ${inr(bill.paidAmount)}")
                        when {
                            bill.pendingAmount > 0 -> {
                                append(" • ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Rf.Error)) { append("Due ${inr(bill.pendingAmount)}") }
                            }
                            bill.advanceAmount > 0 -> append(" • ${inr(bill.advanceAmount)} advance")
                        }
                    },
                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp)
                )
                CoverBar(bill, 5.dp, Modifier.padding(top = 7.dp))
                CreditNote(bill)
            }
            if (bill.pendingAmount > 0) {
                Text(
                    "Collect", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.OnPrimary,
                    modifier = Modifier.pressable(onCollect).clip(RoundedCornerShape(11.dp)).background(Rf.Primary).padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun InitialsCircle(name: String) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(Rf.PrimaryFixed), contentAlignment = Alignment.Center) {
        Text(initials(name), style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.Primary)
    }
}

/** Paid / Partial / Pending, Overdue from an earlier month, and any advance paid on it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BillStatusPills(bill: BillDto) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        val bold = RfType.LabelSm.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        when (bill.paymentStatus) {
            "paid" -> Pill("Paid", Rf.MintSoft, Rf.Secondary, dot = true, style = bold, vertical = 3.dp)
            "partial" -> Pill("Partial", Rf.AmberSoft, Rf.Amber, dot = true, style = bold, vertical = 3.dp)
            else -> Pill("Pending", Rf.Low, Rf.OnSurfaceVariant, style = bold, vertical = 3.dp)
        }
        if (bill.isOverdue) Pill("Overdue", Rf.ErrorContainer, Rf.Error, style = bold, vertical = 3.dp)
        if (bill.advanceAmount > 0) Pill("+${inr(bill.advanceAmount)} advance", Rf.PrimaryFixed, Rf.Primary, style = bold, vertical = 3.dp)
    }
}

/** How much of a bill is covered: advance used (indigo), money received (green), then expenses taken off (amber). */
@Composable
internal fun CoverBar(bill: BillDto, height: Dp, modifier: Modifier = Modifier) {
    val total = max(bill.totalDue, 0.0)
    val credit = min(bill.creditApplied, total)
    val paid = min(bill.paidAmount, max(total - credit, 0.0))
    val adjusted = min(bill.adjustedAmount, max(total - credit - paid, 0.0))
    val rest = max(total - credit - paid - adjusted, 0.0)
    Row(modifier.fillMaxWidth().height(height).clip(CircleShape).background(Rf.Container)) {
        if (total <= 0) return@Row
        if (credit > 0) Box(Modifier.weight(credit.toFloat()).fillMaxHeight().background(Rf.Primary))
        if (paid > 0) Box(Modifier.weight(paid.toFloat()).fillMaxHeight().background(Rf.Secondary))
        if (adjusted > 0) Box(Modifier.weight(adjusted.toFloat()).fillMaxHeight().background(Rf.Amber))
        if (rest > 0) Box(Modifier.weight(rest.toFloat()).fillMaxHeight())
    }
}

/** The web's PartsBar: each part as its share of [whole], side by side from the left. */
@Composable
internal fun PartsBar(parts: List<Pair<Double, Color>>, whole: Double, height: Dp, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(height).clip(CircleShape).background(Rf.Container)) {
        if (whole <= 0) return@Row
        var left = whole
        parts.forEach { (value, color) ->
            val part = min(max(value, 0.0), left)
            if (part > 0) Box(Modifier.weight(part.toFloat()).fillMaxHeight().background(color))
            left -= part
        }
        if (left > 0) Box(Modifier.weight(left.toFloat()).fillMaxHeight())
    }
}

/** Advance from earlier bills used on this one in indigo, and expenses the tenant took off in amber. */
@Composable
internal fun CreditNote(bill: BillDto) {
    if (bill.creditApplied > 0) {
        Row(Modifier.padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            LIcon(Lucide.Wallet, size = 12.dp, tint = Rf.Primary)
            Text("${inr(bill.creditApplied)} from advance", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Primary)
        }
    }
    if (bill.adjustedAmount > 0) {
        Row(Modifier.padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            LIcon(Lucide.Wrench, size = 12.dp, tint = Rf.Amber)
            Text("${inr(bill.adjustedAmount)} expenses taken off", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Amber)
        }
    }
}

/* ------------------------------ Not billed ------------------------------ */

@Composable
private fun NotBilledCard(tenants: List<TenantDto>, month: YearMonth, open: Boolean, onToggle: () -> Unit, onCreate: (TenantDto) -> Unit) {
    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${tenants.size} ${if (tenants.size == 1) "tenant has" else "tenants have"} no bill for ${month.label()}",
                    style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface
                )
                Text("They live in a room, but nothing has been billed yet.", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
            Text(
                if (open) "Hide" else "Review", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Primary,
                modifier = Modifier.pressable(onToggle).clip(RoundedCornerShape(9.dp)).background(Rf.Low).padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        if (open) {
            tenants.forEach { tenant ->
                HorizontalDivider(thickness = 1.dp, color = Rf.Low, modifier = Modifier.padding(top = 10.dp))
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InitialsCircle(tenant.fullName)
                    Column(Modifier.weight(1f)) {
                        Text(tenant.fullName, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(tenant.room?.roomNumber?.let { "Room $it" }, tenant.property?.propertyName).joinToString(" • "),
                            style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        "Create bill", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.OnPrimary,
                        modifier = Modifier.pressable({ onCreate(tenant) }).clip(RoundedCornerShape(9.dp)).background(Rf.Primary).padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/* ------------------------------ Sheets ------------------------------ */

private val MonthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** The year's months in a grid, with the year to step through; no future months. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthSheet(month: YearMonth, onPick: (YearMonth) -> Unit, onDismiss: () -> Unit) {
    val now = YearMonth.now()
    var year by remember { mutableIntStateOf(month.year) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Rf.Lowest,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Choose month", style = RfType.HeadlineSm.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface, modifier = Modifier.weight(1f))
                StepArrow(Lucide.ArrowLeft, "Previous year", enabled = true) { year-- }
                Text("$year", style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface, modifier = Modifier.padding(horizontal = 8.dp))
                StepArrow(Lucide.ArrowRight, "Next year", enabled = year < now.year) { year++ }
            }
            MonthNames.chunked(4).forEachIndexed { row, names ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    names.forEachIndexed { col, name ->
                        val candidate = YearMonth.of(year, row * 4 + col + 1)
                        val future = candidate > now
                        val on = candidate == month
                        Text(
                            name, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center,
                            color = when {
                                on -> Rf.OnPrimary
                                future -> Rf.OutlineVariant
                                else -> Rf.OnSurface
                            },
                            modifier = Modifier.weight(1f)
                                .pressable(if (future) null else ({
                                    onPick(candidate)
                                    onDismiss()
                                }))
                                .clip(RoundedCornerShape(10.dp)).background(if (on) Rf.Primary else Rf.Low).padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Every property, with "All properties" first; the chosen one is ticked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertySheet(properties: List<PropertyDto>, selected: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Rf.Lowest,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Property", style = RfType.HeadlineSm.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface, modifier = Modifier.padding(bottom = 6.dp))
            PropertyRow("All properties", selected == null) { onPick(null) }
            properties.forEach { property -> PropertyRow(property.propertyName, property.id == selected) { onPick(property.id) } }
        }
    }
}

@Composable
private fun PropertyRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressable(onClick, pressScale = 0.98f).clip(RoundedCornerShape(12.dp))
            .background(if (selected) Rf.PrimaryFixed else Color.Transparent).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LIcon(Lucide.Building2, size = 17.dp, tint = if (selected) Rf.Primary else Rf.OnSurfaceVariant)
        Text(name, style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = if (selected) Rf.Primary else Rf.OnSurface, modifier = Modifier.weight(1f))
        if (selected) LIcon(Lucide.Check, size = 17.dp, tint = Rf.Primary)
    }
}

/* ------------------------------ Bill details ------------------------------ */

/**
 * One bill in full, as the web's bill dialog on a phone: totals, every payment and
 * expense deduction, breakdown, advance and the meter photo, then Collect (or Add
 * payment), Delete, Edit bill and Deduct. [onEntry] opens the entry form with
 * [ENTRY_PAYMENT], [ENTRY_DEDUCTION] or "[EDIT_ENTRY]<id>". The meter photo is saved
 * from here right away; a payment's photo belongs to that payment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BillSheet(bill: BillDto, onDismiss: () -> Unit, onEntry: (String) -> Unit, onEdit: () -> Unit, onDeleted: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewing by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf<String?>(null) } // "photo" or "proof" while one uploads
    var confirmDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val total = bill.totalDue
    val settled = min(bill.settledByTenant + bill.creditApplied, total)
    val percent = if (total > 0) (settled / total * 100).roundToInt() else 100
    val month = parseApiMonth(bill.monthYear)
    val tenantName = bill.tenant?.fullName ?: "this tenant"

    // The tenant's other bills say where this bill's advance went
    var tenantBills by remember { mutableStateOf<List<BillDto>>(emptyList()) }
    LaunchedEffect(bill.id, bill.advanceAmount) {
        if (bill.advanceAmount > 0) (PaymentStore.bills(tenantId = bill.tenantId) as? ApiResult.Ok)?.let { tenantBills = it.value }
    }
    val usedBy = tenantBills.mapNotNull { other ->
        val amount = round2(other.creditSources.filter { it.paymentId == bill.id }.sumOf { it.amount })
        if (amount > 0) other.monthYear to amount else null
    }.sortedBy { it.first }
    val used = round2(max(bill.advanceAmount - bill.advanceLeft, 0.0))
    val usedMonths = usedBy.map { monthLabel(it.first) }.let { if (it.size > 1) "${it.dropLast(1).joinToString(", ")} and ${it.last()}" else it.firstOrNull().orEmpty() }

    // What deleting does to the advance, said in the confirmation
    val deleteNote = when {
        used > 0 -> " Its ${inr(used)} advance is used in ${if (usedMonths.isNotEmpty()) "the $usedMonths bill" else "later bills"}, which will show that much more due" +
            (if (bill.advanceLeft > 0) ", and the ${inr(bill.advanceLeft)} not used yet is removed too." else ".")
        bill.advanceLeft > 0 -> " Its ${inr(bill.advanceLeft)} advance will be removed too."
        bill.creditApplied > 0 -> " The ${inr(bill.creditApplied)} advance it used goes back for the next bill."
        else -> ""
    }

    // Only the meter photo is saved from here: a payment's photo belongs to that payment
    fun saveMeterPhoto(uri: Uri?) {
        if (saving != null) return
        saving = "photo"
        scope.launch {
            val key = if (uri == null) null else when (val r = Uploads.upload(context, uri, UploadFolder.MeterReading)) {
                is ApiResult.Fail -> { Toasts.error(r.message); saving = null; return@launch }
                is ApiResult.Ok -> r.value
            }
            when (val result = PaymentStore.setMeterPhoto(bill.id, key)) {
                is ApiResult.Ok -> Toasts.show(if (uri != null) "Meter photo saved" else "Meter photo removed")
                is ApiResult.Fail -> Toasts.error(result.message)
            }
            saving = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Rf.Lowest,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        // A long bill (advance, two attachments) scrolls; the buttons stay pinned under it, as on the web
        Column {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // The bill's month as a small calendar leaf
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.width(48.dp).softShadow(RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)).background(Rf.Lowest)) {
                        Text(
                            month?.month?.let { MonthNames[it.value - 1].uppercase() } ?: "—",
                            style = RfType.LabelSm.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold), color = Rf.OnPrimary, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().background(Rf.Primary).padding(vertical = 2.dp)
                        )
                        Text(
                            "${month?.year ?: ""}", style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("${monthLabel(bill.monthYear)} bill", style = RfType.BodyLg.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
                        BillStatusPills(bill)
                        Text(
                            listOfNotNull(bill.tenant?.fullName, bill.room?.roomNumber?.let { "Room $it" }, bill.property?.propertyName).joinToString(" • "),
                            style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
                        )
                    }
                    Box(Modifier.size(32.dp).pressable(onDismiss).clip(RoundedCornerShape(10.dp)).background(Rf.Low), contentAlignment = Alignment.Center) {
                        LIcon(Lucide.X, size = 16.dp, tint = Rf.OnSurfaceVariant, contentDescription = "Close")
                    }
                }

                Column {
                    BillFigures(bill)
                    CoverBar(bill, 6.dp, Modifier.padding(top = 10.dp))
                    Row(Modifier.padding(top = 6.dp)) {
                        Text(if (bill.adjustedAmount > 0) "$percent% settled" else "$percent% collected", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, modifier = Modifier.weight(1f))
                        when {
                            bill.advanceAmount > 0 -> Text("${inr(bill.advanceAmount)} extra kept as advance", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Primary)
                            bill.pendingAmount > 0 -> Text("${inr(bill.pendingAmount)} left", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Error)
                            else -> Text("Fully paid", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Secondary)
                        }
                    }
                }

                BillEntries(
                    bill,
                    onOpenProof = { url -> openAttachment(context, url) { viewing = it } },
                    onOpen = { receipt -> receipt.id?.let { onEntry("$EDIT_ENTRY$it") } }
                )

                InfoBox("Breakdown") {
                    InfoRow("Rent", inr(bill.rent), first = true)
                    bill.electricity?.let { meter ->
                        InfoRow(
                            "Electricity", inr(meter.amount),
                            sub = "${plainNumber(meter.previousReading)} → ${plainNumber(meter.currentReading)} kWh • ${plainNumber(meter.unitsConsumed)} units × ${inr(meter.ratePerUnit)}"
                        )
                    }
                    bill.otherCharges.forEach { InfoRow(it.name, inr(it.amount)) }
                    InfoRow("Total due", inr(bill.totalDue), strong = true)
                    if (bill.creditApplied > 0) {
                        val from = bill.creditSources.map { monthLabel(it.monthYear) }.distinct().joinToString(", ")
                        InfoRow(if (from.isNotEmpty()) "Advance used (from $from)" else "Advance used", "− ${inr(bill.creditApplied)}", labelColor = Rf.Primary)
                        InfoRow("To pay", inr(max(bill.totalDue - bill.creditApplied, 0.0)), strong = true)
                    }
                }

                bill.notes?.takeIf(String::isNotBlank)?.let { note ->
                    InfoBox("Note") { InfoRow(note, "", first = true) }
                }

                if (bill.advanceAmount > 0) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Rf.PrimaryFixed).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LIcon(Lucide.Wallet, size = 18.dp, tint = Rf.Primary)
                        Column(Modifier.weight(1f)) {
                            Text("${inr(bill.advanceAmount)} advance", style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.Primary)
                            Text(
                                listOfNotNull(
                                    "Paid beyond this bill.",
                                    usedBy.takeIf { it.isNotEmpty() }?.let { "${inr(it.sumOf { u -> u.second })} is used by the $usedMonths bill${if (it.size > 1) "s" else ""}." },
                                    if (bill.advanceLeft > 0) "${inr(bill.advanceLeft)} is kept for the next bills." else null
                                ).joinToString(" "),
                                style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
                            )
                        }
                    }
                }

                // The meter photo of a bill with a reading; payment photos are on the entries above
                if (bill.electricity != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Caps("Meter photo")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AttachmentSlot(
                                "Meter photo", bill.electricity.meterPhoto, allowPdf = false, busy = saving == "photo", modifier = Modifier.weight(1f),
                                onOpen = { url -> openAttachment(context, url) { viewing = it } }, onPick = { saveMeterPhoto(it) }, onRemove = { saveMeterPhoto(null) }
                            )
                            Box(Modifier.weight(1f))
                        }
                        Text("A new meter photo is saved to this bill right away.", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = Rf.Low)
            Column(
                Modifier.padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Collect what is still due; a fully paid bill can still take an advance
                SheetButton(
                    if (bill.pendingAmount > 0) "Collect ${inr(bill.pendingAmount)}" else "Add payment",
                    Lucide.Plus, Rf.Primary, Rf.OnPrimary, Modifier.fillMaxWidth()
                ) { onEntry(ENTRY_PAYMENT) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetButton("Delete", Lucide.Trash2, Rf.ErrorContainer, Rf.Error, Modifier.weight(1f)) { confirmDelete = true }
                    SheetButton("Edit bill", Lucide.Pencil, Rf.Low, Rf.Primary, Modifier.weight(1f), onEdit)
                    SheetButton("Deduct", Lucide.Wrench, Rf.AmberSoft, Rf.Amber, Modifier.weight(1f)) { onEntry(ENTRY_DEDUCTION) }
                }
            }
        }
    }
    viewing?.let { url -> PhotoViewer(listOf(Photo.Stored("", url)), 0, onClose = { viewing = null }) }
    if (confirmDelete) {
        ConfirmSheet(
            icon = Lucide.Trash2,
            title = "Delete the ${monthLabel(bill.monthYear)} bill?",
            message = "The ${monthLabel(bill.monthYear)} bill of $tenantName is deleted.$deleteNote",
            confirmLabel = "Delete bill",
            busy = deleting,
            onConfirm = {
                deleting = true
                scope.launch {
                    when (val r = PaymentStore.delete(bill.id)) {
                        is ApiResult.Ok -> {
                            Toasts.show("Bill deleted")
                            confirmDelete = false
                            onDeleted()
                        }
                        is ApiResult.Fail -> Toasts.error(r.message)
                    }
                    deleting = false
                }
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

/**
 * Every payment and expense deduction of a bill, newest first, each with its amount,
 * method or category, number or name, date, note and photo, and what they add up to.
 * A row with an id opens it to change or remove; one saved before entries existed can't.
 * Without [onOpen] (the tenant's side, as on the web) the list is read-only and only
 * the photos open; [paidBy] says who paid an expense.
 */
@Composable
internal fun BillEntries(bill: BillDto, onOpenProof: (String) -> Unit, onOpen: ((ReceiptDto) -> Unit)?, paidBy: String = "tenant") {
    val receipts = bill.receipts
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, Rf.Low, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Caps("Payments & deductions")
            if (receipts.isNotEmpty()) {
                Text(
                    "${receipts.size}", style = RfType.LabelSm.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold), color = Rf.OnSurfaceVariant,
                    modifier = Modifier.clip(CircleShape).background(Rf.Low).padding(horizontal = 7.dp, vertical = 1.dp)
                )
            }
        }
        if (receipts.isEmpty()) {
            Text(
                if (bill.creditApplied > 0) "Nothing received in cash." else "Nothing received yet.",
                style = RfType.BodySm, color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
            )
            return@Column
        }
        receipts.forEachIndexed { index, receipt ->
            if (index > 0) HorizontalDivider(thickness = 1.dp, color = Rf.Low)
            EntryRow(bill, receipt, onOpenProof, onOpen, paidBy)
        }
        HorizontalDivider(thickness = 1.dp, color = Rf.Low)
        Row(Modifier.padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Received ${inr(bill.paidAmount)}" + if (bill.adjustedAmount > 0) " • Adjusted ${inr(bill.adjustedAmount)}" else "",
                style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, modifier = Modifier.weight(1f)
            )
            Text(
                "Settled ${inr(min(bill.settledByTenant + bill.creditApplied, bill.totalDue))} of ${inr(bill.totalDue)}",
                style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
            )
        }
        if (onOpen != null && receipts.any { it.id != null }) {
            Text("Tap a row to change or remove it.", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun EntryRow(bill: BillDto, receipt: ReceiptDto, onOpenProof: (String) -> Unit, onOpen: ((ReceiptDto) -> Unit)?, paidBy: String) {
    val deduction = receipt.isDeduction
    val icon = when {
        deduction -> Lucide.Wrench
        receipt.method == "upi" -> Lucide.Smartphone
        receipt.method == "cheque" -> Lucide.Landmark
        else -> Lucide.Banknote
    }
    val date = apiDate(receipt.date)?.shortLabel() ?: "—"
    Row(
        Modifier.fillMaxWidth().pressable(if (receipt.id != null && onOpen != null) ({ onOpen(receipt) }) else null, pressScale = 0.98f).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if (deduction) Rf.AmberSoft else Rf.MintSoft),
            contentAlignment = Alignment.Center
        ) {
            LIcon(icon, size = 16.dp, tint = if (deduction) Rf.Amber else Rf.Secondary)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    (if (deduction) "− " else "") + inr(receipt.amount),
                    style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = if (deduction) Rf.Amber else Rf.OnSurface
                )
                Text(
                    if (deduction) categoryLabel(receipt.category) else methodLabel(receipt.method),
                    style = RfType.LabelSm.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold),
                    color = if (deduction) Rf.Amber else Rf.OnSurfaceVariant,
                    modifier = Modifier.clip(CircleShape).background(if (deduction) Rf.AmberSoft else Rf.Low).padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
            Text(
                if (deduction) "${entryTitle(bill, receipt)} • paid by $paidBy • $date" else "Payment ${entryNumber(bill, receipt)} • $date",
                style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp)
            )
            receipt.notes?.takeIf(String::isNotBlank)?.let {
                Text(it, style = RfType.LabelSm, color = Rf.OnSurface, modifier = Modifier.padding(top = 3.dp))
            }
            // Only matters to whoever could change the other rows
            if (receipt.id == null && onOpen != null) {
                Text("Saved before entries", style = RfType.LabelSm.copy(fontSize = 10.sp), color = Rf.Outline, modifier = Modifier.padding(top = 2.dp))
            }
        }
        receipt.proof?.let { url ->
            Box(
                Modifier.size(40.dp).pressable({ onOpenProof(url) }).clip(RoundedCornerShape(9.dp)).background(Rf.Low),
                contentAlignment = Alignment.Center
            ) {
                if (isPdfLink(url)) LIcon(Lucide.FileText, size = 18.dp, tint = Rf.Error, contentDescription = "Open the PDF")
                else RemoteImage(url, Modifier.fillMaxSize(), placeholderIcon = Lucide.Image, iconSize = 16.dp)
            }
        }
    }
}

/** A PDF opens in the phone's viewer; a photo opens full screen through [onPhoto]. */
internal fun openAttachment(context: android.content.Context, url: String, onPhoto: (String) -> Unit) {
    if (isPdfLink(url)) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } else onPhoto(url)
}

@Composable
private fun SheetButton(label: String, icon: LucideIcon, background: Color, content: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(44.dp).pressable(onClick, pressScale = 0.97f).clip(RoundedCornerShape(12.dp)).background(background),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LIcon(icon, size = 16.dp, tint = content)
        Text(label, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = content)
    }
}

internal fun plainNumber(value: Double): String =
    if (value % 1.0 == 0.0) "%,d".format(value.toLong()) else "%,.2f".format(value)

/** Total due, Received and Still due; Adjusted (expenses the tenant paid and took off) only when there are some, two by two. */
@Composable
internal fun BillFigures(bill: BillDto) {
    val due = if (bill.pendingAmount > 0) Rf.Error else Rf.Secondary
    if (bill.adjustedAmount > 0) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Figure("Total due", inr(bill.totalDue), Rf.OnSurface, Modifier.weight(1f))
                Figure("Received", inr(bill.paidAmount), Rf.Secondary, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Figure("Adjusted", inr(bill.adjustedAmount), Rf.Amber, Modifier.weight(1f))
                Figure("Still due", inr(bill.pendingAmount), due, Modifier.weight(1f))
            }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Figure("Total due", inr(bill.totalDue), Rf.OnSurface, Modifier.weight(1f))
            Figure("Received", inr(bill.paidAmount), Rf.Secondary, Modifier.weight(1f))
            Figure("Still due", inr(bill.pendingAmount), due, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun Figure(label: String, value: String, color: Color, modifier: Modifier, sub: String? = null, subColor: Color = Rf.OnSurfaceVariant) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Rf.Low).padding(10.dp)) {
        Caps(label)
        Text(value, style = RfType.BodyMd.copy(fontWeight = FontWeight.ExtraBold), color = color, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        sub?.let { Text(it, style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = subColor, modifier = Modifier.padding(top = 2.dp)) }
    }
}

@Composable
internal fun InfoBox(title: String, rows: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, Rf.Low, RoundedCornerShape(14.dp))) {
        Box(Modifier.fillMaxWidth().background(Rf.Low).padding(horizontal = 12.dp, vertical = 8.dp)) { Caps(title) }
        rows()
    }
}

@Composable
internal fun InfoRow(
    label: String,
    value: String,
    first: Boolean = false,
    sub: String? = null,
    strong: Boolean = false,
    labelColor: Color? = null,
    valueColor: Color? = null
) {
    if (!first) HorizontalDivider(thickness = 1.dp, color = Rf.Low)
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(
                label, style = RfType.LabelMd.copy(fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium),
                color = labelColor ?: if (strong) Rf.OnSurface else Rf.OnSurfaceVariant
            )
            if (sub != null) Text(sub, style = RfType.LabelSm.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
        }
        Text(
            value, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = valueColor ?: Rf.OnSurface,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f, fill = false)
        )
    }
}

/**
 * A bill's meter photo or payment proof. Empty, it offers to add one; filled, a tap
 * offers to view, change or remove it (a PDF opens in the phone's viewer).
 */
@Composable
private fun AttachmentSlot(
    label: String,
    url: String?,
    allowPdf: Boolean,
    busy: Boolean,
    modifier: Modifier,
    onOpen: (String) -> Unit,
    onPick: (Uri) -> Unit,
    onRemove: () -> Unit,
    readOnly: Boolean = false
) {
    val choose = photoChooser(
        title = label,
        onView = url?.let { { onOpen(it) } },
        onRemove = if (url != null) onRemove else null,
        removeLabel = "Remove ${label.lowercase()}",
        removeHint = "The bill keeps everything else",
        onPdf = if (allowPdf) onPick else null
    ) { uris -> onPick(uris.first()) }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.height(96.dp).pressable(
            when {
                busy -> null
                readOnly -> url?.let { { onOpen(it) } }
                else -> choose
            },
            pressScale = 0.97f
        ).clip(shape)
            .background(Rf.Low).then(if (url == null) Modifier.dashedBorder(Rf.OutlineVariant) else Modifier)
    ) {
        when {
            url == null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                LIcon(if (allowPdf) Lucide.Receipt else Lucide.Camera, size = 22.dp, tint = Rf.Primary)
                Text(
                    if (allowPdf) "Add proof" else "Add meter photo", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold),
                    color = Rf.Primary, modifier = Modifier.padding(top = 4.dp)
                )
            }
            isPdfLink(url) -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                LIcon(Lucide.FileText, size = 26.dp, tint = Rf.Error)
                Text("PDF", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface, modifier = Modifier.padding(top = 4.dp))
            }
            else -> RemoteImage(url, Modifier.fillMaxSize(), placeholderIcon = Lucide.Image, iconSize = 22.dp)
        }
        if (url != null) {
            Text(
                label, style = RfType.LabelSm.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold), color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(6.dp))
                    .background(Rf.InverseSurface.copy(alpha = 0.7f)).padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        if (busy) {
            Box(Modifier.fillMaxSize().background(Rf.InverseSurface.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                Spinner(Color.White)
            }
        }
    }
}

