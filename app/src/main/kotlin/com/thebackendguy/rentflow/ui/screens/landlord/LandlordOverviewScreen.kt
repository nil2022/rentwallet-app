package com.thebackendguy.rentflow.ui.screens.landlord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.thebackendguy.rentflow.data.Demo
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.Portfolio
import com.thebackendguy.rentflow.data.apiDate
import com.thebackendguy.rentflow.data.monthLabel
import com.thebackendguy.rentflow.data.PayStatus
import com.thebackendguy.rentflow.data.Tenant
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.ui.components.ActionButton
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.ArrowLink
import com.thebackendguy.rentflow.ui.components.AttentionCard
import com.thebackendguy.rentflow.ui.components.Caption
import com.thebackendguy.rentflow.ui.components.DarkCard
import com.thebackendguy.rentflow.ui.components.DarkCardHeader
import com.thebackendguy.rentflow.ui.components.DarkMetric
import com.thebackendguy.rentflow.ui.components.DarkProgress
import com.thebackendguy.rentflow.ui.components.DarkStat
import com.thebackendguy.rentflow.ui.components.HealthTileSpec
import com.thebackendguy.rentflow.ui.components.HealthTiles
import com.thebackendguy.rentflow.ui.components.IconTile
import com.thebackendguy.rentflow.ui.components.LedgerCard
import com.thebackendguy.rentflow.ui.components.LedgerTone
import com.thebackendguy.rentflow.ui.components.Pill
import com.thebackendguy.rentflow.ui.components.QuickAction
import com.thebackendguy.rentflow.ui.components.QuickActions
import com.thebackendguy.rentflow.ui.components.QuickTone
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.SectionHead
import com.thebackendguy.rentflow.ui.components.Segmented
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import java.time.LocalDate

private const val WITHDRAWAL = 15_000

/**
 * The landlord's home. Portfolio health counts real properties, tenants and
 * rooms; the collections card, needs-attention cards and payment ledger are
 * sample data ([tenants]) until the payments API exists.
 */
@Composable
fun LandlordOverviewScreen(
    firstName: String,
    tenants: List<Tenant>,
    shell: Shell,
    onOpenTenants: () -> Unit,
    onAddProperty: () -> Unit,
    onAddTenant: () -> Unit
) {
    val portfolio = portfolioState().data
    LaunchedEffect(Unit) { LandlordStore.refresh() }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val paid = tenants.filter { it.status == PayStatus.Paid }
    val pending = tenants.filter { it.status != PayStatus.Paid }
    val received = paid.sumOf { it.rent }
    val outstanding = pending.sumOf { it.rent }
    val percent = if (tenants.isEmpty()) 0 else paid.size * 100 / tenants.size

    AppPage(
        topBar = {
            shell.TopBar(
                if (firstName.isEmpty()) "Hello 👋" else "Hello, $firstName 👋",
                chip = portfolio?.properties?.size?.let { "$it ${if (it == 1) "property" else "properties"}" }
            )
        },
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

        // Adding is live; recording payments and reminders wait for the payments API, as on the web
        QuickActions(
            listOf(
                QuickAction("Add property", Lucide.Building2, QuickTone.Primary, onAddProperty),
                QuickAction("Add tenant", Lucide.UserPlus, QuickTone.Secondary, onAddTenant),
                QuickAction("Record payment", Lucide.CirclePlus, QuickTone.Surface) {},
                QuickAction("Send reminders", Lucide.MessageCircle, QuickTone.Low) {}
            )
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHead("Portfolio health") { Caption("From your properties") }
            HealthTiles(portfolioTiles(portfolio))
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
                                ActionButton("Send reminder", Lucide.Send, Rf.Secondary, Rf.OnSecondary, Modifier.weight(1f), doneLabel = "Reminder sent")
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
                    .forEach { (_, tenant) -> TenantLedgerCard(tenant, onOpenTenants) }
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


/** The four portfolio tiles from real data; dashes while it loads. */
@Composable
private fun portfolioTiles(portfolio: Portfolio?): List<HealthTileSpec> {
    if (portfolio == null) {
        return listOf(
            HealthTileSpec(Lucide.Building2, "", "— Properties", "Loading…", Rf.High, Rf.Primary, Rf.Secondary),
            HealthTileSpec(Lucide.Users, "", "— Tenants", "Loading…", Rf.MintSoft, Rf.Secondary, Rf.Secondary),
            HealthTileSpec(Lucide.DoorOpen, "", "— Rooms", "Loading…", Rf.Container, Rf.Primary, Rf.Primary, background = Rf.High),
            HealthTileSpec(Lucide.CalendarClock, "", "— Leases", "Loading…", Rf.Container, Rf.Primary, Rf.OnSurfaceVariant, background = Rf.High)
        )
    }
    val properties = portfolio.properties
    val cities = properties.map { it.city }.filter(String::isNotBlank).distinct()
    val leased = portfolio.tenants.count { it.lease != null }
    val noRoom = portfolio.tenants.size - leased
    val vacant = portfolio.allRooms.count { portfolio.occupants(it.id).isEmpty() }
    // Leases that end within the next two months, soonest first
    val today = LocalDate.now()
    val ending = portfolio.tenants
        .mapNotNull { t -> t.lease?.let { lease -> apiDate(lease.endDate)?.let { end -> Triple(t, lease, end) } } }
        .filter { (_, _, end) -> !end.isBefore(today) && end.isBefore(today.plusMonths(2)) }
        .sortedBy { it.third }
    val next = ending.firstOrNull()

    return listOf(
        HealthTileSpec(
            Lucide.Building2, "Active", "${properties.size} ${if (properties.size == 1) "Property" else "Properties"}",
            when {
                cities.isEmpty() -> "Add your first property"
                cities.size <= 2 -> cities.joinToString(", ")
                else -> "${cities.take(2).joinToString(", ")} +${cities.size - 2}"
            },
            Rf.High, Rf.Primary, Rf.Secondary
        ),
        HealthTileSpec(
            Lucide.Users, if (noRoom > 0) "$noRoom no room" else "All housed", "${portfolio.tenants.size} Tenants",
            "$leased with a lease", Rf.MintSoft, Rf.Secondary, if (noRoom > 0) Rf.OnSurfaceVariant else Rf.Secondary
        ),
        HealthTileSpec(
            Lucide.DoorOpen, "Vacant", "$vacant ${if (vacant == 1) "Room" else "Rooms"}",
            if (vacant == 0) "Every room is let" else "Ready to let", Rf.Container, Rf.Primary, Rf.Primary, background = Rf.High
        ),
        HealthTileSpec(
            Lucide.CalendarClock, next?.third?.monthLabel() ?: "Next 60 days",
            if (ending.isEmpty()) "No leases ending" else "${ending.size} Lease ending",
            next?.second?.let { lease -> listOfNotNull(lease.property?.propertyName, lease.room?.roomNumber?.let { "Room $it" }).joinToString(" • ") }
                ?: "Nothing to renew soon",
            Rf.Container, Rf.Primary, Rf.OnSurfaceVariant, background = Rf.High
        )
    )
}
