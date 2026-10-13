package com.thebackendguy.rentflow.ui.screens.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebackendguy.rentflow.data.Loadable
import com.thebackendguy.rentflow.data.Photo
import com.thebackendguy.rentflow.data.TenantBills
import com.thebackendguy.rentflow.data.TenantStore
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.isOverdue
import com.thebackendguy.rentflow.data.isPdfLink
import com.thebackendguy.rentflow.data.methodLabel
import com.thebackendguy.rentflow.data.monthLabel
import com.thebackendguy.rentflow.data.parseApiMonth
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.BillDto
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.EmptyState
import com.thebackendguy.rentflow.ui.components.ErrorState
import com.thebackendguy.rentflow.ui.components.LoadingCards
import com.thebackendguy.rentflow.ui.components.PhotoViewer
import com.thebackendguy.rentflow.ui.components.Pill
import com.thebackendguy.rentflow.ui.components.RemoteImage
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.Segmented
import com.thebackendguy.rentflow.ui.components.Spinner
import com.thebackendguy.rentflow.ui.components.TopBarAction
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.components.softShadow
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.screens.landlord.BillEntries
import com.thebackendguy.rentflow.ui.screens.landlord.BillFigures
import com.thebackendguy.rentflow.ui.screens.landlord.BillStatusPills
import com.thebackendguy.rentflow.ui.screens.landlord.Caps
import com.thebackendguy.rentflow.ui.screens.landlord.CoverBar
import com.thebackendguy.rentflow.ui.screens.landlord.CreditNote
import com.thebackendguy.rentflow.ui.screens.landlord.Figure
import com.thebackendguy.rentflow.ui.screens.landlord.InfoBox
import com.thebackendguy.rentflow.ui.screens.landlord.InfoRow
import com.thebackendguy.rentflow.ui.screens.landlord.openAttachment
import com.thebackendguy.rentflow.ui.screens.landlord.plainNumber
import com.thebackendguy.rentflow.ui.screens.landlord.rememberBillDownloader
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * The tenant's own bills (the backend's /tenant/payment): the Payments tab and one
 * bill in full. Bills are recorded by the landlord, so everything here is read-only.
 */

private val LeafMonth = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
private val LeafYear = DateTimeFormatter.ofPattern("yy", Locale.ENGLISH)

/** The tenant's bills; starts loading them if nothing has loaded yet. */
@Composable
internal fun tenantBillsState(): Loadable<TenantBills> {
    val state by TenantStore.state.collectAsState()
    LaunchedEffect(Unit) { if (TenantStore.state.value.data == null) TenantStore.refresh() }
    return state
}

/** [content] once the bills are in; placeholder cards while they load; the reason and a retry if they can't. */
@Composable
internal fun ColumnScope.TenantBillsContent(state: Loadable<TenantBills>, content: @Composable ColumnScope.(TenantBills) -> Unit) {
    val data = state.data
    when {
        data != null -> content(data)
        state.error != null -> ErrorState(state.error, onRetry = TenantStore::refresh)
        else -> LoadingCards(3)
    }
}

/* ------------------------------ Payments tab ------------------------------ */

@Composable
fun TenantBillsScreen(shell: Shell, onOpenBill: (String) -> Unit) {
    val state = tenantBillsState()
    LaunchedEffect(Unit) { TenantStore.refresh() }
    var filter by rememberSaveable { mutableIntStateOf(0) }

    AppPage(topBar = { shell.TopBar("Payments") }, bottomBar = { shell.BottomNav(1) }) {
        Column {
            Text("Your bills", style = RfType.HeadlineMd.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
            Text("Every month your landlord has billed.", style = RfType.BodySm, color = Rf.OnSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        TenantBillsContent(state) { data ->
            if (data.bills.isEmpty()) {
                EmptyState(
                    icon = Lucide.Receipt,
                    title = "No bills yet",
                    message = "When your landlord makes your first bill, it shows up here."
                )
                return@TenantBillsContent
            }
            val bills = data.bills
            val totals = data.totals
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Figure("Billed", inr(totals?.totalDue ?: bills.sumOf { it.totalDue }), Rf.OnSurface, Modifier.weight(1f).fillMaxHeight())
                Figure(
                    "Received", inr(totals?.totalPaid ?: bills.sumOf { it.paidAmount }), Rf.Secondary, Modifier.weight(1f).fillMaxHeight(),
                    sub = data.adjusted.takeIf { it > 0 }?.let { "+ ${inr(it)} expenses" }, subColor = Rf.Amber
                )
                val due = totals?.totalPending ?: bills.sumOf { it.pendingAmount }
                Figure("Still due", inr(due), if (due > 0) Rf.Error else Rf.OnSurface, Modifier.weight(1f).fillMaxHeight())
            }
            val keys = listOf(null, "pending", "partial", "paid")
            val counts = keys.map { key -> bills.count { key == null || it.paymentStatus == key } }
            Segmented(
                options = listOf("All", "Pending", "Partial", "Paid").mapIndexed { i, label -> "$label (${counts[i]})" },
                selected = filter,
                onSelect = { filter = it }
            )
            val shown = bills.filter { keys[filter] == null || it.paymentStatus == keys[filter] }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (shown.isEmpty()) {
                    Text("No bills here.", style = RfType.BodyMd, color = Rf.OnSurfaceVariant, modifier = Modifier.padding(4.dp))
                }
                shown.forEach { bill -> TenantBillRow(bill) { onOpenBill(bill.id) } }
            }
        }
    }
}

/** A month's bill: the month as a calendar leaf, the total, what was received and what is due. */
@Composable
internal fun TenantBillRow(bill: BillDto, onOpen: () -> Unit) {
    val late = bill.isOverdue && bill.pendingAmount > 0
    RfCard(
        shape = RoundedCornerShape(14.dp),
        padding = PaddingValues(12.dp),
        color = if (late) Rf.ErrorContainer.copy(alpha = 0.25f).compositeOver(Rf.Lowest) else Rf.Lowest,
        onClick = onOpen
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MonthLeaf(bill.monthYear, short = true)
            Column(Modifier.weight(1f)) {
                Text(inr(bill.totalDue), style = RfType.BodyLg.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
                Text(
                    "Received ${inr(bill.paidAmount)}" + when {
                        bill.pendingAmount > 0 -> " • Due ${inr(bill.pendingAmount)}"
                        bill.paidAmount > 0 -> " • ${methodLabel(bill.paymentMethod)}"
                        else -> ""
                    },
                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
                )
                CreditNote(bill)
            }
            val bold = RfType.LabelSm.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            when {
                late -> Pill("Overdue", Rf.ErrorContainer, Rf.Error, dot = true, style = bold, vertical = 3.dp)
                bill.paymentStatus == "paid" -> Pill("Paid", Rf.MintSoft, Rf.Secondary, dot = true, style = bold, vertical = 3.dp)
                bill.paymentStatus == "partial" -> Pill("Partial", Rf.AmberSoft, Rf.Amber, dot = true, style = bold, vertical = 3.dp)
                else -> Pill("Pending", Rf.Low, Rf.OnSurfaceVariant, style = bold, vertical = 3.dp)
            }
        }
    }
}

/** The bill's month as a small calendar leaf: "OCT" over "26" (or "2026"). */
@Composable
internal fun MonthLeaf(monthYear: String, short: Boolean) {
    val month = parseApiMonth(monthYear)
    val shape = RoundedCornerShape(10.dp)
    Column(Modifier.width(if (short) 44.dp else 48.dp).clip(shape).background(Rf.Low)) {
        Text(
            month?.format(LeafMonth)?.uppercase() ?: "—",
            style = RfType.LabelSm.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold), color = Rf.OnPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().background(Rf.Primary).padding(vertical = 2.dp)
        )
        Text(
            if (short) month?.format(LeafYear).orEmpty() else "${month?.year ?: ""}",
            style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )
    }
}

/** A colour over a solid one, so a tinted card stays opaque. */
internal fun Color.compositeOver(base: Color): Color = Color(
    red = red * alpha + base.red * (1 - alpha),
    green = green * alpha + base.green * (1 - alpha),
    blue = blue * alpha + base.blue * (1 - alpha)
)

/* ------------------------------ One bill ------------------------------ */

/**
 * One bill in full, as the landlord's bill sheet shows it, without Collect, Edit or
 * Delete: totals, each payment and expense taken off, breakdown, note, advance and the
 * meter photo.
 */
@Composable
fun TenantBillScreen(billId: String, shell: Shell, onBack: () -> Unit) {
    val state = tenantBillsState()
    val listed = state.data?.bills?.find { it.id == billId }
    var fetched by remember(billId) { mutableStateOf<BillDto?>(null) }
    var error by remember(billId) { mutableStateOf<String?>(null) }
    LaunchedEffect(billId) {
        when (val r = TenantStore.bill(billId)) {
            is ApiResult.Ok -> fetched = r.value
            is ApiResult.Fail -> error = r.message
        }
    }
    val bill = fetched ?: listed
    // The tenant's copy shows the property only: the API doesn't send them the landlord's details
    val downloader = rememberBillDownloader()
    val download: (() -> Unit)? = bill?.let { current -> { downloader.download(current, issuedBy = null) } }

    AppPage(
        topBar = {
            shell.TopBar(
                bill?.let { "${monthLabel(it.monthYear)} bill" } ?: "Bill", onBack = onBack,
                action = download?.let { TopBarAction(Lucide.Download, "Download the bill", busy = downloader.busy, onClick = it) }
            )
        }
    ) {
        when {
            bill != null -> BillDetails(bill, state.data?.bills.orEmpty(), downloading = downloader.busy, onDownload = download ?: {})
            error != null -> ErrorState(error.orEmpty(), onRetry = onBack)
            else -> LoadingCards(2)
        }
    }
}

@Composable
private fun ColumnScope.BillDetails(bill: BillDto, allBills: List<BillDto>, downloading: Boolean, onDownload: () -> Unit) {
    val context = LocalContext.current
    var viewing by remember { mutableStateOf<String?>(null) }
    val total = bill.totalDue
    val settled = minOf(bill.settledByTenant + bill.creditApplied, total)
    val percent = if (total > 0) (settled / total * 100).roundToInt() else 100

    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(16.dp), spacing = 12.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MonthLeaf(bill.monthYear, short = false)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    bill.room?.roomNumber?.let { "Room $it" } ?: "${monthLabel(bill.monthYear)} bill",
                    style = RfType.BodyLg.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface
                )
                val place = listOfNotNull(bill.property?.propertyName, bill.property?.city).filter(String::isNotBlank).joinToString(" • ")
                if (place.isNotEmpty()) Text(place, style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
                BillStatusPills(bill)
            }
        }
        BillFigures(bill)
        Column {
            CoverBar(bill, 6.dp)
            Row(Modifier.padding(top = 6.dp)) {
                Text(
                    if (bill.adjustedAmount > 0) "$percent% settled" else "$percent% paid",
                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, modifier = Modifier.weight(1f)
                )
                when {
                    bill.advanceAmount > 0 -> Text("${inr(bill.advanceAmount)} extra kept as advance", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Primary)
                    bill.pendingAmount > 0 -> Text("${inr(bill.pendingAmount)} left to pay", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Error)
                    else -> Text("Fully paid", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Secondary)
                }
            }
        }
    }

    // The bill as an image, saved in Downloads/RentFlow
    Row(
        Modifier.fillMaxWidth().height(44.dp).pressable(if (downloading) null else onDownload, pressScale = 0.98f)
            .softShadow(RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).background(Rf.Lowest),
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (downloading) Spinner(Rf.Primary, size = 16.dp) else LIcon(Lucide.Download, size = 16.dp, tint = Rf.Primary)
        Text(if (downloading) "Making the image…" else "Download bill", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.Primary)
    }

    // Each payment and expense, read-only as on the web; only their photos open
    BillEntries(bill, onOpenProof = { url -> openAttachment(context, url) { viewing = it } }, onOpen = null, paidBy = "you")

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

    // Money paid beyond this bill: which later bills used it, and what is still kept
    if (bill.advanceAmount > 0) {
        val usedBy = allBills.mapNotNull { other ->
            val amount = other.creditSources.filter { it.paymentId == bill.id }.sumOf { it.amount }
            if (amount > 0) monthLabel(other.monthYear) else null
        }
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
                        "You paid this much beyond the bill.",
                        usedBy.takeIf { it.isNotEmpty() }?.let { "Used by the ${it.joinToString(", ")} bill${if (it.size > 1) "s" else ""}." },
                        if (bill.advanceLeft > 0) "${inr(bill.advanceLeft)} comes off your next bills." else null
                    ).joinToString(" "),
                    style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
                )
            }
        }
    }

    // The meter photo the landlord added; payment photos are on the entries above
    bill.electricity?.meterPhoto?.let { meterPhoto ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caps("Attachments")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Attachment("Meter photo", meterPhoto, Modifier.weight(1f)) { openAttachment(context, meterPhoto) { url -> viewing = url } }
                Box(Modifier.weight(1f))
            }
        }
    }
    viewing?.let { url -> PhotoViewer(listOf(Photo.Stored("", url)), 0, onClose = { viewing = null }) }
}

/** A photo or PDF the landlord attached; tapping it opens it. */
@Composable
private fun Attachment(label: String, url: String, modifier: Modifier, onOpen: () -> Unit) {
    Box(modifier.height(96.dp).pressable(onOpen, pressScale = 0.97f).clip(RoundedCornerShape(12.dp)).background(Rf.Low)) {
        if (isPdfLink(url)) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                LIcon(Lucide.FileText, size = 26.dp, tint = Rf.Error)
                Text("PDF", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface, modifier = Modifier.padding(top = 4.dp))
            }
        } else {
            RemoteImage(url, Modifier.fillMaxSize(), placeholderIcon = Lucide.Image, iconSize = 22.dp)
        }
        Text(
            label, style = RfType.LabelSm.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold), color = Color.White,
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(6.dp))
                .background(Rf.InverseSurface.copy(alpha = 0.7f)).padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
