package com.thebackendguy.myandroidtestapp.ui.screens.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thebackendguy.myandroidtestapp.data.Demo
import com.thebackendguy.myandroidtestapp.data.PayStatus
import com.thebackendguy.myandroidtestapp.data.RentPayment
import com.thebackendguy.myandroidtestapp.data.inr
import com.thebackendguy.myandroidtestapp.ui.components.AlertCard
import com.thebackendguy.myandroidtestapp.ui.components.AppPage
import com.thebackendguy.myandroidtestapp.ui.components.ArrowLink
import com.thebackendguy.myandroidtestapp.ui.components.Caption
import com.thebackendguy.myandroidtestapp.ui.components.DarkCard
import com.thebackendguy.myandroidtestapp.ui.components.DarkCardHeader
import com.thebackendguy.myandroidtestapp.ui.components.DarkMetric
import com.thebackendguy.myandroidtestapp.ui.components.HealthTileSpec
import com.thebackendguy.myandroidtestapp.ui.components.HealthTiles
import com.thebackendguy.myandroidtestapp.ui.components.LedgerCard
import com.thebackendguy.myandroidtestapp.ui.components.LedgerTone
import com.thebackendguy.myandroidtestapp.ui.components.Pill
import com.thebackendguy.myandroidtestapp.ui.components.QuickAction
import com.thebackendguy.myandroidtestapp.ui.components.QuickActions
import com.thebackendguy.myandroidtestapp.ui.components.QuickTone
import com.thebackendguy.myandroidtestapp.ui.components.SectionHead
import com.thebackendguy.myandroidtestapp.ui.components.pressable
import com.thebackendguy.myandroidtestapp.ui.icons.LIcon
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.screens.Shell
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RfType

@Composable
fun TenantHomeScreen(
    payments: List<RentPayment>,
    shell: Shell,
    onPayRent: () -> Unit,
    onOpenReceipt: (Int) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val current = payments.first()
    val paid = current.status == PayStatus.Paid
    val latestReceipt = payments.indexOfFirst { it.status == PayStatus.Paid }

    AppPage(topBar = { shell.TopBar("Hello, Rohan 👋", chip = "Green View") }, bottomBar = { shell.BottomNav(0) }) {
        DarkCard {
            DarkCardHeader("${current.month} rent") {
                if (paid) Pill("Paid", background = Rf.SecondaryContainer.copy(alpha = 0.2f), content = Rf.Mint, dot = true)
                else Pill("Due in 3 days", background = Rf.AmberText.copy(alpha = 0.18f), content = Rf.AmberText, dot = true, dotColor = Rf.AmberDot)
            }
            DarkMetric(
                label = if (paid) "Rent paid" else "Rent due",
                value = inr(current.amount),
                side = if (paid) "Paid ${current.shortDate}" else "Due ${current.shortDate}",
                sideColor = if (paid) Rf.Mint else Rf.AmberText,
                note = "To ${Demo.LANDLORD_NAME} • Credited to landlord wallet"
            )
            OnDarkButton(if (paid) "View receipt" else "Pay rent", onClick = if (paid) ({ onOpenReceipt(0) }) else onPayRent)
        }

        QuickActions(
            listOf(
                QuickAction("Pay rent", Lucide.Wallet, QuickTone.Primary, if (paid) ({ onOpenReceipt(0) }) else onPayRent),
                QuickAction("Receipts", Lucide.ReceiptText, QuickTone.Secondary) { if (latestReceipt >= 0) onOpenReceipt(latestReceipt) },
                QuickAction("Payment history", Lucide.History, QuickTone.Surface, onOpenHistory),
                QuickAction("Lease details", Lucide.FileText, QuickTone.Low, onOpenProfile)
            )
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHead("Your lease") { Caption(Demo.PROPERTY) }
            HealthTiles(
                listOf(
                    HealthTileSpec(Lucide.Landmark, "Held", inr(Demo.DEPOSIT), "Security deposit", Rf.High, Rf.Primary, Rf.Secondary),
                    HealthTileSpec(Lucide.CalendarClock, "Mar 2027", "31 Mar", "Lease ends", Rf.Container, Rf.Primary, Rf.OnSurfaceVariant)
                )
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHead("Alerts") { Caption("2 new") }
            if (paid) {
                AlertCard(Lucide.BadgeCheck, "Payment successful", "${current.month.substringBefore(' ')} rent was credited to ${Demo.LANDLORD_NAME}’s wallet.", Rf.MintSoft, Rf.Secondary)
            } else {
                AlertCard(Lucide.BellRing, "Rent due reminder", "May rent is due on 10 May. Pay before the due date to keep your record clean.", Rf.AmberSoft, Rf.Amber)
            }
            AlertCard(Lucide.BadgeCheck, "Receipt generated", "Your April rent receipt is available in Payment History.", Rf.MintSoft, Rf.Secondary)
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHead("Recent payments") { ArrowLink("View all", onOpenHistory) }
            payments.withIndex().filter { it.value.status == PayStatus.Paid }.take(2).forEach { (index, payment) ->
                LedgerCard(
                    initials = payment.shortMonth,
                    name = payment.month,
                    sub = "Receipt ${payment.transactionId}",
                    amount = inr(payment.amount),
                    status = "Paid",
                    tone = LedgerTone.Settled,
                    meta = "UPI / Bank • Wallet credited",
                    whenText = payment.shortDate,
                    onClick = { onOpenReceipt(index) }
                )
            }
        }
    }
}

/** White call-to-action inside a dark card. */
@Composable
internal fun OnDarkButton(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(onClick, pressScale = 0.98f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.Primary)
        LIcon(Lucide.ArrowRight, size = 18.dp, tint = Rf.Primary)
    }
}
