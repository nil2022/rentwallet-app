package com.thebackendguy.myandroidtestapp.ui.screens.tenant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thebackendguy.myandroidtestapp.data.Demo
import com.thebackendguy.myandroidtestapp.data.PayStatus
import com.thebackendguy.myandroidtestapp.data.RentPayment
import com.thebackendguy.myandroidtestapp.data.inr
import com.thebackendguy.myandroidtestapp.ui.components.AppPage
import com.thebackendguy.myandroidtestapp.ui.components.Caption
import com.thebackendguy.myandroidtestapp.ui.components.DarkCard
import com.thebackendguy.myandroidtestapp.ui.components.DarkCardHeader
import com.thebackendguy.myandroidtestapp.ui.components.DarkMetric
import com.thebackendguy.myandroidtestapp.ui.components.DashedDivider
import com.thebackendguy.myandroidtestapp.ui.components.Detail
import com.thebackendguy.myandroidtestapp.ui.components.DetailRows
import com.thebackendguy.myandroidtestapp.ui.components.DetailStyle
import com.thebackendguy.myandroidtestapp.ui.components.InfoCard
import com.thebackendguy.myandroidtestapp.ui.components.LedgerCard
import com.thebackendguy.myandroidtestapp.ui.components.LedgerTone
import com.thebackendguy.myandroidtestapp.ui.components.Pill
import com.thebackendguy.myandroidtestapp.ui.components.RfCard
import com.thebackendguy.myandroidtestapp.ui.components.SectionHead
import com.thebackendguy.myandroidtestapp.ui.components.Segmented
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.screens.Shell
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RfType

private val FILTERS = listOf(null, PayStatus.Paid, PayStatus.Pending, PayStatus.Failed)

@Composable
fun PaymentHistoryScreen(payments: List<RentPayment>, shell: Shell, onOpenReceipt: (Int) -> Unit, onPay: (Int) -> Unit) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val paid = payments.count { it.status == PayStatus.Paid }
    val pending = payments.count { it.status == PayStatus.Pending }
    val failed = payments.count { it.status == PayStatus.Failed }
    val attention = listOfNotNull(
        if (pending > 0) "$pending rent due" else null,
        if (failed > 0) "$failed failed payment" else null
    )

    AppPage(topBar = { shell.TopBar("Payments") }, bottomBar = { shell.BottomNav(1) }) {
        DarkCard {
            DarkCardHeader("2026 summary") {
                if (failed > 0) Pill("$failed failed", background = Rf.Error.copy(alpha = 0.25f), content = Rf.Rose, dot = true)
            }
            DarkMetric(
                label = "Total paid this year",
                value = inr(payments.filter { it.status == PayStatus.Paid }.sumOf { it.amount }),
                note = if (attention.isEmpty()) "Every payment is up to date." else attention.joinToString(" and ") + " need attention."
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CountTile(paid, "Paid", Rf.Secondary, Modifier.weight(1f))
            CountTile(pending, "Pending", Rf.Primary, Modifier.weight(1f))
            CountTile(failed, "Failed", Rf.Error, Modifier.weight(1f))
        }

        Column {
            SectionHead("All payments") { Caption("Tap for receipt") }
            Segmented(
                options = listOf("All", "Paid", "Pending", "Failed"),
                selected = filter,
                onSelect = { filter = it },
                modifier = Modifier.padding(top = 12.dp)
            )
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val shown = payments.withIndex().filter { FILTERS[filter] == null || it.value.status == FILTERS[filter] }
                if (shown.isEmpty()) {
                    Text("No payments here yet.", style = RfType.BodyMd, color = Rf.OnSurfaceVariant, modifier = Modifier.padding(4.dp))
                }
                shown.forEach { (index, payment) -> PaymentLedgerRow(payment, onOpen = { onOpenReceipt(index) }, onPay = { onPay(index) }) }
            }
        }
    }
}

@Composable
private fun PaymentLedgerRow(payment: RentPayment, onOpen: () -> Unit, onPay: () -> Unit) {
    when (payment.status) {
        PayStatus.Paid -> LedgerCard(
            initials = payment.shortMonth, name = payment.month, sub = payment.transactionId,
            amount = inr(payment.amount), status = "Paid", tone = LedgerTone.Settled,
            meta = "UPI / Bank • Wallet credited", whenText = payment.shortDate, onClick = onOpen
        )
        PayStatus.Pending -> LedgerCard(
            initials = payment.shortMonth, name = payment.month, sub = payment.dateLabel,
            amount = inr(payment.amount), status = "Pending", tone = LedgerTone.Due,
            meta = "Receipt generates after payment", action = "Pay now", onAction = onPay, onClick = onPay
        )
        PayStatus.Failed -> LedgerCard(
            initials = payment.shortMonth, name = payment.month, sub = "Payment failed • ${payment.shortDate}",
            amount = inr(payment.amount), status = "Failed", tone = LedgerTone.Late,
            meta = "No wallet credit was made", action = "Retry", onAction = onPay, onClick = onOpen
        )
    }
}

@Composable
private fun CountTile(count: Int, label: String, color: Color, modifier: Modifier) {
    RfCard(modifier = modifier, padding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), style = RfType.HeadlineSm.copy(fontWeight = FontWeight.Bold), color = color)
            Text(label, style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
        }
    }
}

@Composable
fun ReceiptScreen(payment: RentPayment, shell: Shell, onBack: () -> Unit) {
    val (pillBg, pillFg) = when (payment.status) {
        PayStatus.Paid -> Rf.MintSoft to Rf.Secondary
        PayStatus.Pending -> Rf.High to Rf.Primary
        PayStatus.Failed -> Rf.ErrorContainer to Rf.Error
    }
    val walletLine = when (payment.status) {
        PayStatus.Paid -> "Landlord wallet credited"
        PayStatus.Pending -> "Wallet credit pending"
        PayStatus.Failed -> "No wallet credit"
    }

    AppPage(topBar = { shell.TopBar("Receipt", onBack = onBack) }, bottomBar = { shell.BottomNav(1) }) {
        RfCard {
            Column(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Pill(payment.status.name, background = pillBg, content = pillFg, dot = true)
                Text(inr(payment.amount), style = RfType.Metric, color = Rf.OnSurface)
                Text("${payment.month} • $walletLine", style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
            }
            DashedDivider(Modifier.padding(bottom = 16.dp))
            DetailRows(
                listOf(
                    Detail("Tenant", Demo.TENANT_NAME),
                    Detail("Landlord", Demo.LANDLORD_NAME),
                    Detail("Property", Demo.PROPERTY),
                    Detail("Address", Demo.ADDRESS),
                    Detail("Transaction ID", payment.transactionId, DetailStyle.Mono),
                    Detail("Payment date", payment.dateLabel),
                    Detail("Payment method", payment.method),
                    Detail("Wallet status", if (payment.status == PayStatus.Paid) "Credited" else "Not credited"),
                    Detail("Receipt status", payment.receiptStatus)
                )
            )
        }
        when (payment.status) {
            PayStatus.Paid -> InfoCard(Lucide.ShieldCheck, "Official receipt", "Generated after the landlord wallet was credited.")
            PayStatus.Pending -> InfoCard(Lucide.Info, "Awaiting payment", "The receipt is generated once this rent is paid.", iconColor = Rf.Primary)
            PayStatus.Failed -> InfoCard(Lucide.TriangleAlert, "No receipt", "This payment failed, so no receipt was generated.", iconColor = Rf.Error)
        }
    }
}
