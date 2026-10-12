package com.thebackendguy.rentflow.ui.screens.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebackendguy.rentflow.data.TenantBills
import com.thebackendguy.rentflow.data.TenantStore
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.label
import com.thebackendguy.rentflow.data.monthLabel
import com.thebackendguy.rentflow.data.remote.BillDto
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.ArrowLink
import com.thebackendguy.rentflow.ui.components.EmptyState
import com.thebackendguy.rentflow.ui.components.SectionHead
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.screens.landlord.Figure
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import java.time.YearMonth

/** How many bills Home lists under "Recent bills". */
private const val RECENT = 3

/**
 * The tenant's home, from their own bills: this month's bill, what is overdue,
 * any advance held, the totals and the latest bills. Payments are recorded by the
 * landlord, so there is no Pay button; a note says how to pay.
 */
@Composable
fun TenantHomeScreen(firstName: String, shell: Shell, onOpenBill: (String) -> Unit, onOpenBills: () -> Unit) {
    val state = tenantBillsState()
    LaunchedEffect(Unit) { TenantStore.refresh() }
    val room = state.data?.latest?.room?.roomNumber

    AppPage(
        topBar = { shell.TopBar(if (firstName.isEmpty()) "Hello 👋" else "Hello, $firstName 👋", chip = room?.let { "Room $it" }) },
        bottomBar = { shell.BottomNav(0) }
    ) {
        TenantBillsContent(state) { data ->
            if (data.bills.isEmpty()) {
                EmptyState(
                    icon = Lucide.Receipt,
                    title = "No bills yet",
                    message = "When your landlord makes your first bill, it shows up here with what's due."
                )
                HowToPay()
                return@TenantBillsContent
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ThisMonthCard(data, onOpenBill, onOpenBills)
                data.overdue.takeIf { it.isNotEmpty() }?.let { overdue ->
                    val oldest = overdue.minBy { it.monthYear }
                    NoticeRow(
                        Lucide.TriangleAlert, "${inr(overdue.sumOf { it.pendingAmount })} overdue", "Unpaid since ${monthLabel(oldest.monthYear)}",
                        Rf.ErrorContainer.copy(alpha = 0.55f).compositeOver(Rf.Lowest), Rf.ErrorContainer, Rf.Error, link = "View"
                    ) { onOpenBill(oldest.id) }
                }
                if (data.advance > 0) {
                    NoticeRow(
                        Lucide.Wallet, "${inr(data.advance)} advance held", "It comes off your next bills.",
                        Rf.PrimaryFixed, Rf.Lowest, Rf.Primary
                    )
                }
                HowToPay()
            }

            // All-time totals, from the API's summary when it sends one
            val totals = data.totals
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Figure("Billed", inr(totals?.totalDue ?: data.bills.sumOf { it.totalDue }), Rf.OnSurface, Modifier.weight(1f).fillMaxHeight())
                    // Expenses you paid and took off settle bills too, but aren't money received
                    Figure(
                        "Received", inr(totals?.totalPaid ?: data.bills.sumOf { it.paidAmount }), Rf.Secondary, Modifier.weight(1f).fillMaxHeight(),
                        sub = data.adjusted.takeIf { it > 0 }?.let { "+ ${inr(it)} expenses taken off" }, subColor = Rf.Amber
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val due = totals?.totalPending ?: data.bills.sumOf { it.pendingAmount }
                    Figure("Still due", inr(due), if (due > 0) Rf.Error else Rf.OnSurface, Modifier.weight(1f))
                    Figure("Advance held", inr(data.advance), Rf.Primary, Modifier.weight(1f))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHead("Recent bills") { ArrowLink("View all", onOpenBills) }
                data.bills.take(RECENT).forEach { bill -> TenantBillRow(bill) { onOpenBill(bill.id) } }
            }
        }
    }
}

/** This month's bill on an indigo card: what is still due (or paid), how much is covered, and View bill. */
@Composable
private fun ThisMonthCard(data: TenantBills, onOpenBill: (String) -> Unit, onOpenBills: () -> Unit) {
    val current: BillDto? = data.current
    val month = YearMonth.now().label()
    val shape = RoundedCornerShape(18.dp)
    val faint = Color(0xFFC7CBFF)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF312E81), Color(0xFF3525CD), Color(0xFF4F46E5))))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$month bill", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = faint, modifier = Modifier.weight(1f))
            when {
                current == null -> HeroPill("Not billed yet", Color.White.copy(alpha = 0.12f), Color.White)
                current.paymentStatus == "paid" -> HeroPill("Paid", Color(0x3334D399), Color(0xFF6EE7B7))
                current.paymentStatus == "partial" -> HeroPill("Partly paid", Color(0x2EFBBF24), Color(0xFFFCD34D))
                else -> HeroPill("Due", Color(0x2EFBBF24), Color(0xFFFCD34D))
            }
        }
        if (current == null) {
            Text("No bill yet", style = RfType.HeadlineMd.copy(fontWeight = FontWeight.ExtraBold), color = Color.White)
            Text("Your landlord hasn’t made this month’s bill yet.", style = RfType.LabelSm, color = faint)
            HeroButton("View all bills", Lucide.History, onOpenBills)
            return@Column
        }
        val paid = current.pendingAmount <= 0
        Column {
            Text(if (paid) "Paid" else "Still due", style = RfType.LabelSm, color = faint)
            Text(
                inr(if (paid) current.totalDue else current.pendingAmount),
                style = RfType.HeadlineMd.copy(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold), color = Color.White
            )
        }
        // How much of the bill is covered: money and advance in green, then expenses taken off in amber
        val total = current.totalDue
        val covered = if (total > 0) ((total - current.pendingAmount) / total).toFloat().coerceIn(0f, 1f) else 1f
        val expenses = if (total > 0) (current.adjustedAmount / total).toFloat().coerceIn(0f, covered) else 0f
        val amber = Color(0xFFFCD34D)
        Row(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f))) {
            if (covered - expenses > 0f) Box(Modifier.weight(covered - expenses).fillMaxHeight().background(Color(0xFF6EE7B7)))
            if (expenses > 0f) Box(Modifier.weight(expenses).fillMaxHeight().background(amber))
            if (covered < 1f) Box(Modifier.weight(1f - covered))
        }
        Row {
            Text(
                buildAnnotatedString {
                    append("Received ${inr(current.paidAmount)}")
                    if (current.adjustedAmount > 0) {
                        append(" • ")
                        withStyle(SpanStyle(color = amber, fontWeight = FontWeight.Bold)) { append("${inr(current.adjustedAmount)} expenses") }
                    }
                    append(" of ${inr(total)}")
                    if (current.creditApplied > 0) append(" • ${inr(current.creditApplied)} advance used")
                },
                style = RfType.LabelSm, color = faint, modifier = Modifier.weight(1f)
            )
            current.room?.roomNumber?.let { Text("Room $it", style = RfType.LabelSm, color = faint) }
        }
        HeroButton("View bill", Lucide.Receipt) { onOpenBill(current.id) }
    }
}

@Composable
private fun HeroPill(text: String, background: Color, content: Color) {
    Row(
        Modifier.clip(CircleShape).background(background).padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(content))
        Text(text, style = RfType.LabelSm.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold), color = content)
    }
}

/** White button on the indigo card. */
@Composable
private fun HeroButton(text: String, icon: LucideIcon, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(42.dp).pressable(onClick, pressScale = 0.98f).clip(RoundedCornerShape(12.dp)).background(Color.White),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LIcon(icon, size = 16.dp, tint = Color(0xFF3525CD))
        Text(text, style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFF3525CD))
    }
}

/** A tinted line with an icon tile, such as what is overdue or the advance held. */
@Composable
private fun NoticeRow(
    icon: LucideIcon,
    title: String,
    sub: String,
    background: Color,
    tile: Color,
    tint: Color,
    link: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().pressable(onClick, pressScale = 0.98f).clip(RoundedCornerShape(14.dp)).background(background)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(tile), contentAlignment = Alignment.Center) {
            LIcon(icon, size = 16.dp, tint = tint)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = tint)
            Text(sub, style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
        }
        if (link != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(link, style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = tint)
                LIcon(Lucide.ChevronRight, size = 14.dp, tint = tint)
            }
        }
    }
}

/** Payments are recorded by the landlord: how the tenant pays. */
@Composable
private fun HowToPay() {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).border(1.5.dp, Rf.OutlineVariant, shape).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LIcon(Lucide.Info, size = 15.dp, tint = Rf.Primary, modifier = Modifier.padding(top = 1.dp))
        Text(
            "Pay your landlord by cash, UPI or cheque. They record it here, and your bill updates.",
            style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant
        )
    }
}
