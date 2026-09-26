package com.thebackendguy.myandroidtestapp.ui.screens.landlord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thebackendguy.myandroidtestapp.data.Demo
import com.thebackendguy.myandroidtestapp.data.PayStatus
import com.thebackendguy.myandroidtestapp.data.Tenant
import com.thebackendguy.myandroidtestapp.data.inr
import com.thebackendguy.myandroidtestapp.ui.components.ActionButton
import com.thebackendguy.myandroidtestapp.ui.components.AppPage
import com.thebackendguy.myandroidtestapp.ui.components.ArrowLink
import com.thebackendguy.myandroidtestapp.ui.components.AttentionCard
import com.thebackendguy.myandroidtestapp.ui.components.Caption
import com.thebackendguy.myandroidtestapp.ui.components.DarkCard
import com.thebackendguy.myandroidtestapp.ui.components.DarkCardHeader
import com.thebackendguy.myandroidtestapp.ui.components.DarkMetric
import com.thebackendguy.myandroidtestapp.ui.components.DarkProgress
import com.thebackendguy.myandroidtestapp.ui.components.DarkStat
import com.thebackendguy.myandroidtestapp.ui.components.HealthTileSpec
import com.thebackendguy.myandroidtestapp.ui.components.HealthTiles
import com.thebackendguy.myandroidtestapp.ui.components.IconTile
import com.thebackendguy.myandroidtestapp.ui.components.LedgerCard
import com.thebackendguy.myandroidtestapp.ui.components.LedgerTone
import com.thebackendguy.myandroidtestapp.ui.components.Pill
import com.thebackendguy.myandroidtestapp.ui.components.QuickAction
import com.thebackendguy.myandroidtestapp.ui.components.QuickActions
import com.thebackendguy.myandroidtestapp.ui.components.QuickTone
import com.thebackendguy.myandroidtestapp.ui.components.RfCard
import com.thebackendguy.myandroidtestapp.ui.components.SectionHead
import com.thebackendguy.myandroidtestapp.ui.components.Segmented
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.screens.Shell
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RfType

private const val WITHDRAWAL = 15_000

@Composable
fun LandlordOverviewScreen(
    tenants: List<Tenant>,
    shell: Shell,
    onOpenTenants: () -> Unit,
    onOpenTenant: (Int) -> Unit
) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val paid = tenants.filter { it.status == PayStatus.Paid }
    val pending = tenants.filter { it.status != PayStatus.Paid }
    val received = paid.sumOf { it.rent }
    val outstanding = pending.sumOf { it.rent }
    val percent = if (tenants.isEmpty()) 0 else paid.size * 100 / tenants.size

    AppPage(
        topBar = { shell.TopBar("Hello, Amit 👋", chip = "${tenants.size} properties") },
        bottomBar = { shell.BottomNav(0) }
    ) {
        DarkCard {
            DarkCardHeader("May 2026 collections") {
                Pill("Wallet synced", background = Rf.SecondaryContainer.copy(alpha = 0.2f), content = Rf.Mint, dot = true)
            }
            DarkMetric(label = "Rent collected this month", value = inr(received), side = "$percent% collected")
            DarkProgress(
                fraction = percent / 100f,
                left = "${paid.size} of ${tenants.size} tenants paid",
                right = "${inr(outstanding)} pending"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkStat(
                    Lucide.TriangleAlert, "${inr(outstanding)} pending", "${pending.size} tenants late",
                    tone = Rf.Rose, toneBg = Rf.Error.copy(alpha = 0.2f), modifier = Modifier.weight(1f), onClick = onOpenTenants
                )
                DarkStat(
                    Lucide.Wallet, "${inr(Demo.WALLET_BALANCE)} in wallet", "Tap to withdraw",
                    tone = Rf.Mint, toneBg = Rf.Mint.copy(alpha = 0.2f), modifier = Modifier.weight(1f), onClick = {}
                )
            }
        }

        // Record, remind, withdraw and add are placeholders until the API exists, as on the web
        QuickActions(
            listOf(
                QuickAction("+ Record payment", Lucide.CirclePlus, QuickTone.Primary) {},
                QuickAction("Send reminders", Lucide.MessageCircle, QuickTone.Secondary) {},
                QuickAction("Withdraw", Lucide.Banknote, QuickTone.Surface) {},
                QuickAction("Add tenant", Lucide.UserPlus, QuickTone.Low) {}
            )
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHead("Portfolio health") { Caption("May cycle") }
            HealthTiles(
                listOf(
                    HealthTileSpec(Lucide.Building2, "Active", "${tenants.size} Properties", "Salt Lake, New Town +3", Rf.High, Rf.Primary, Rf.Secondary),
                    HealthTileSpec(Lucide.Users, "100%", "${tenants.size} Tenants", "Every property let", Rf.MintSoft, Rf.Secondary, Rf.Secondary),
                    HealthTileSpec(
                        Lucide.TriangleAlert, "Attention", "${pending.size} Pending", pending.joinToString(" and ") { it.shortDate }.let { "Due $it" },
                        Rf.ErrorContainer, Rf.Error, Rf.Error,
                        background = Rf.ErrorSoft.compositeOverSurface(), titleColor = Rf.OnErrorContainer, subColor = Rf.OnErrorContainer
                    ),
                    HealthTileSpec(Lucide.CalendarClock, "Sep 2026", "1 Lease ending", "Sunrise Enclave • 30 Sep", Rf.Container, Rf.Primary, Rf.OnSurfaceVariant, background = Rf.High)
                )
            )
        }

        if (pending.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHead("Needs attention", alertDot = true) { ArrowLink("View all", onOpenTenants) }
                pending.forEachIndexed { i, tenant ->
                    val urgent = i == 0
                    AttentionCard(
                        initials = tenant.initials,
                        name = tenant.name,
                        sub = "${tenant.property} • ${tenant.area.substringAfter(", ")}",
                        pill = tenant.dateLabel,
                        pillBg = if (urgent) Rf.ErrorContainer else Rf.Highest,
                        pillFg = if (urgent) Rf.Error else Rf.Primary,
                        avatarBg = if (urgent) Rf.ErrorContainer else Rf.High,
                        avatarFg = if (urgent) Rf.Error else Rf.Primary,
                        amount = inr(tenant.rent),
                        amountColor = if (urgent) Rf.Error else Rf.OnSurface
                    ) {
                        if (urgent) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionButton("Send reminder", Lucide.Send, Rf.Secondary, Color.White, Modifier.weight(1f), doneLabel = "Reminder sent")
                                ActionButton("Log offline", Lucide.ReceiptText, Rf.Container, Rf.OnSurfaceVariant, Modifier.weight(1f), doneLabel = "Logged")
                            }
                        } else {
                            ActionButton("Send 1-tap reminder", Lucide.BellRing, Rf.PrimaryContainer, Color.White, Modifier.fillMaxWidth(), doneLabel = "Reminder sent")
                        }
                    }
                }
            }
        }

        Column {
            SectionHead("Payment ledger") { Caption("Live") }
            Segmented(
                options = listOf("All (${tenants.size})", "Paid (${paid.size})", "Pending (${pending.size})"),
                selected = filter,
                onSelect = { filter = it },
                modifier = Modifier.padding(top = 12.dp)
            )
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                tenants.withIndex()
                    .filter { (_, t) -> filter == 0 || (filter == 1) == (t.status == PayStatus.Paid) }
                    .forEach { (index, tenant) -> TenantLedgerCard(tenant) { onOpenTenant(index) } }
            }
        }

        RfCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconTile(Lucide.Wallet, background = Rf.High, tint = Rf.Primary)
                Column(Modifier.weight(1f)) {
                    Text("Wallet withdrawal", style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                    Text("Bank transfer • ${Demo.BANK_ACCOUNT.removePrefix("HDFC Bank ").let { "HDFC $it" }}", style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
                }
                Pill("Processing", background = Rf.AmberSoft, content = Rf.Amber)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(8.dp)).background(Rf.Low).padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Amount", style = RfType.LabelMd, color = Rf.OnSurfaceVariant)
                Text(inr(-WITHDRAWAL), style = RfType.HeadlineSm.copy(fontWeight = FontWeight.Bold), color = Rf.Error)
            }
        }
    }
}

@Composable
internal fun TenantLedgerCard(tenant: Tenant, onClick: () -> Unit) {
    if (tenant.status == PayStatus.Paid) {
        LedgerCard(
            initials = tenant.initials, name = tenant.name, sub = tenant.property,
            amount = inr(tenant.rent), status = "Paid", tone = LedgerTone.Settled,
            meta = "UPI / Bank • Wallet credited", whenText = tenant.shortDate, onClick = onClick
        )
    } else {
        LedgerCard(
            initials = tenant.initials, name = tenant.name, sub = "${tenant.property} • ${tenant.dateLabel}",
            amount = inr(tenant.rent), status = "Pending", tone = LedgerTone.Late,
            meta = "No reminder sent yet", action = "Remind", onAction = {}, onClick = onClick
        )
    }
}

/** Blend a translucent tint over the page surface. */
private fun Color.compositeOverSurface(): Color = Color(
    red = red * alpha + Rf.Surface.red * (1 - alpha),
    green = green * alpha + Rf.Surface.green * (1 - alpha),
    blue = blue * alpha + Rf.Surface.blue * (1 - alpha)
)
