package com.thebackendguy.rentflow.ui.screens.landlord

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import com.thebackendguy.rentflow.data.PaymentStore
import com.thebackendguy.rentflow.data.apiDate
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.isOverdue
import com.thebackendguy.rentflow.data.methodLabel
import com.thebackendguy.rentflow.data.monthLabel
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.BillDto
import com.thebackendguy.rentflow.ui.components.CsvShare
import com.thebackendguy.rentflow.ui.components.FadeIn
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.Skeleton
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import java.time.LocalDate

/** How many bills show before "Show more", as on the web. */
private const val PAGE = 6

/**
 * A tenant's bills, newest month first, loaded again whenever a bill changes.
 * Null until they arrive; [Pair.second] is why they couldn't be loaded.
 */
@Composable
internal fun rememberTenantBills(tenantId: String): Pair<List<BillDto>?, String?> {
    val version by PaymentStore.version.collectAsState()
    var bills by remember(tenantId) { mutableStateOf<List<BillDto>?>(null) }
    var error by remember(tenantId) { mutableStateOf<String?>(null) }
    LaunchedEffect(tenantId, version) {
        when (val r = PaymentStore.bills(tenantId = tenantId)) {
            is ApiResult.Ok -> { bills = r.value.sortedByDescending { it.monthYear }; error = null }
            is ApiResult.Fail -> error = r.message
        }
    }
    return bills to error
}

/**
 * The web's Payment History card on a tenant's page: totals, a status filter and
 * every month's bill, newest first. A bill opens its details; Collect and Record
 * Payment open the form ([onRecord] with the bill's month, or null for this month).
 */
@Composable
internal fun PaymentHistoryCard(
    bills: List<BillDto>?,
    error: String?,
    tenantName: String,
    onRecord: (String?) -> Unit,
    onEditBill: (String) -> Unit
) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf("all") }
    var shown by rememberSaveable { mutableIntStateOf(PAGE) }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }

    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(16.dp), spacing = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Rf.PrimaryFixed), contentAlignment = Alignment.Center) {
                LIcon(Lucide.Receipt, size = 18.dp, tint = Rf.Primary)
            }
            Column {
                Text("Payment History", style = RfType.BodyLg.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
                Text("Bills and payments", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
            }
        }

        val list = bills
        // Placeholders pulse until the bills (or an error) arrive, then the card fades in
        FadeIn(list != null || error != null, placeholder = { HistoryPlaceholder() }) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when {
                    list == null && error != null -> Text(error.orEmpty(), style = RfType.BodySm, color = Rf.Error)
                    list == null -> Unit
                    list.isEmpty() -> {
                        Text(
                            "Record this month’s rent and any payment received. Every bill you save shows up here.",
                            style = RfType.BodySm, color = Rf.OnSurfaceVariant
                        )
                        WideAction("Record Payment", Lucide.Plus, Rf.Primary, Rf.OnPrimary) { onRecord(null) }
                    }
                    else -> {
                        // Totals of every bill; advance held only once this tenant has had any
                        val hadAdvance = list.any { it.advanceAmount > 0 || it.creditApplied > 0 }
                        val stats = listOfNotNull(
                            Triple("Billed", list.sumOf { it.totalDue }, Rf.OnSurface),
                            Triple("Received", list.sumOf { it.paidAmount }, Rf.Secondary),
                            list.sumOf { it.pendingAmount }.let { Triple("Still due", it, if (it > 0) Rf.Error else Rf.OnSurface) },
                            if (hadAdvance) Triple("Advance held", list.sumOf { it.advanceLeft }, Rf.Primary) else null
                        )
                        stats.chunked(if (stats.size == 4) 2 else 3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (label, value, color) -> HistoryStat(label, inr(value), color, Modifier.weight(1f)) }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("all" to "All", "pending" to "Pending", "partial" to "Partial", "paid" to "Paid").forEach { (key, label) ->
                                val on = filter == key
                                Text(
                                    label, style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = if (on) Rf.Primary else Rf.OnSurfaceVariant,
                                    modifier = Modifier.pressable({ filter = key; shown = PAGE }, pressScale = 0.96f).clip(CircleShape)
                                        .background(if (on) Rf.PrimaryFixed else Rf.Lowest)
                                        .border(if (on) 1.5.dp else 1.dp, if (on) Rf.Primary else Rf.OutlineVariant, CircleShape)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        val rows = list.filter { filter == "all" || it.paymentStatus == filter }
                        if (rows.isEmpty()) {
                            Text("No $filter bills.", style = RfType.BodySm, color = Rf.OnSurfaceVariant)
                        } else {
                            Column {
                                rows.take(shown).forEachIndexed { index, bill ->
                                    if (index > 0) HorizontalDivider(thickness = 1.dp, color = Rf.Low)
                                    HistoryRow(bill, onOpen = { openId = bill.id }, onCollect = { onRecord(bill.monthYear) })
                                }
                            }
                        }
                        if (rows.size > shown) {
                            WideAction("Show more (${rows.size - shown} more)", Lucide.ChevronDown, Rf.Low, Rf.Primary) { shown += PAGE }
                        }
                        WideAction("Record Payment", Lucide.Plus, Rf.Primary, Rf.OnPrimary) { onRecord(null) }
                        WideAction("Export CSV", Lucide.Download, Rf.Low, Rf.Primary) { shareStatement(context, tenantName, list) }
                    }
                }
            }
        }
    }

    val openBill = bills?.find { it.id == openId }
    if (openBill != null) {
        BillSheet(
            openBill,
            onDismiss = { openId = null },
            onCollect = {
                openId = null
                onRecord(openBill.monthYear)
            },
            onEdit = {
                openId = null
                onEditBill(openBill.id)
            },
            onDeleted = { openId = null }
        )
    }
}

/** Two totals and two bills, pulsing, in the shape of the card's content. */
@Composable
private fun HistoryPlaceholder() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(2) {
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Rf.Low).padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Skeleton(Modifier.fillMaxWidth(0.5f).height(9.dp), radius = 5.dp)
                    Skeleton(Modifier.fillMaxWidth(0.7f).height(15.dp))
                }
            }
        }
        Column {
            listOf(0.48f, 0.42f).forEachIndexed { index, width ->
                if (index > 0) HorizontalDivider(thickness = 1.dp, color = Rf.Low)
                Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Skeleton(Modifier.fillMaxWidth(width).height(12.dp))
                        Skeleton(Modifier.fillMaxWidth(width * 0.7f).height(16.dp))
                        Skeleton(Modifier.fillMaxWidth(width + 0.2f).height(10.dp), radius = 5.dp, color = Rf.Low)
                    }
                    if (index == 0) Skeleton(Modifier.size(width = 70.dp, height = 34.dp), radius = 11.dp)
                }
            }
        }
    }
}

@Composable
private fun HistoryStat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Rf.Low).padding(horizontal = 10.dp, vertical = 9.dp)) {
        Caps(label)
        Text(value, style = RfType.BodyMd.copy(fontWeight = FontWeight.ExtraBold), color = color, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}

/** A month's bill: status, total, received and due; Collect while something is due. */
@Composable
private fun HistoryRow(bill: BillDto, onOpen: () -> Unit, onCollect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressable(onOpen, pressScale = 0.98f).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(monthLabel(bill.monthYear), style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
            BillStatusPills(bill)
            Text(inr(bill.totalDue), style = RfType.BodyLg.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
            Text(
                "Received ${inr(bill.paidAmount)}" + if (bill.pendingAmount > 0) " • Due ${inr(bill.pendingAmount)}" else "",
                style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
            )
            CreditNote(bill)
        }
        if (bill.pendingAmount > 0) {
            Text(
                "Collect", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.OnPrimary,
                modifier = Modifier.pressable(onCollect).clip(RoundedCornerShape(11.dp)).background(Rf.Primary).padding(horizontal = 14.dp, vertical = 9.dp)
            )
        } else {
            LIcon(Lucide.ChevronRight, size = 18.dp, tint = Rf.Outline)
        }
    }
}

@Composable
private fun WideAction(label: String, icon: LucideIcon, background: Color, content: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(44.dp).pressable(onClick, pressScale = 0.97f).clip(RoundedCornerShape(12.dp)).background(background),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LIcon(icon, size = 16.dp, tint = content)
        Text(label, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = content)
    }
}

/** The tenant's statement, one row per bill, in the web's columns. */
private fun shareStatement(context: android.content.Context, tenantName: String, bills: List<BillDto>) {
    val header = listOf(
        "Month", "Rent", "Units", "Electricity", "Other charges", "Total due", "Advance used",
        "Received", "Still due", "Extra paid (advance)", "Status", "Method", "Payment date", "Notes"
    )
    val status = mapOf("paid" to "Paid", "partial" to "Partial", "pending" to "Pending")
    val rows = bills.map { b ->
        listOf(
            monthLabel(b.monthYear), b.rent, b.electricity?.unitsConsumed, b.electricity?.amount ?: 0.0,
            b.otherCharges.sumOf { it.amount }, b.totalDue, b.creditApplied, b.paidAmount, b.pendingAmount, b.advanceAmount,
            status[b.paymentStatus].orEmpty() + if (b.isOverdue) " (overdue)" else "",
            if (b.paidAmount > 0) methodLabel(b.paymentMethod) else "",
            apiDate(b.paymentDate)?.toString().orEmpty(),
            b.notes.orEmpty()
        )
    }
    val name = tenantName.trim().replace(Regex("\\s+"), "-").lowercase()
    CsvShare.share(context, "$name-statement-${LocalDate.now()}.csv", header, rows)
}
