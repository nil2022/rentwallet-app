package com.thebackendguy.rentflow.ui.screens.landlord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.Portfolio
import com.thebackendguy.rentflow.data.PayStatus
import com.thebackendguy.rentflow.data.Tenant
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.ArrowLink
import com.thebackendguy.rentflow.ui.components.Caption
import com.thebackendguy.rentflow.ui.components.HealthTileSpec
import com.thebackendguy.rentflow.ui.components.HealthTiles
import com.thebackendguy.rentflow.ui.components.IconTile
import com.thebackendguy.rentflow.ui.components.LedgerCard
import com.thebackendguy.rentflow.ui.components.LedgerTone
import com.thebackendguy.rentflow.ui.components.LoadingCards
import com.thebackendguy.rentflow.ui.components.Pill
import com.thebackendguy.rentflow.ui.components.QuickAction
import com.thebackendguy.rentflow.ui.components.QuickActions
import com.thebackendguy.rentflow.ui.components.QuickTone
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.SectionHead
import com.thebackendguy.rentflow.ui.components.Segmented
import com.thebackendguy.rentflow.ui.components.TonalButton
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType

/** How many properties the overview shows before "View all". */
private const val PROPERTY_PREVIEW = 3

/**
 * The landlord's home, in the order of the web's phone dashboard: portfolio
 * health and the property cards are real; the payment ledger ([tenants]) and
 * the sub-meter card are sample data until those APIs exist.
 */
@Composable
fun LandlordOverviewScreen(
    firstName: String,
    tenants: List<Tenant>,
    shell: Shell,
    onOpenTenants: () -> Unit,
    onOpenProperties: () -> Unit,
    onOpenProperty: (String) -> Unit,
    onAddProperty: () -> Unit,
    onAddTenant: () -> Unit
) {
    val state = portfolioState()
    val portfolio = state.data
    LaunchedEffect(Unit) { LandlordStore.refresh() }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val paid = tenants.filter { it.status == PayStatus.Paid }
    val pending = tenants.filter { it.status != PayStatus.Paid }

    AppPage(
        topBar = {
            shell.TopBar(
                if (firstName.isEmpty()) "Hello 👋" else "Hello, $firstName 👋",
                chip = portfolio?.properties?.size?.let { "$it ${if (it == 1) "property" else "properties"}" }
            )
        },
        bottomBar = { shell.BottomNav(0) }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHead("Portfolio health") { Caption("From your properties") }
            HealthTiles(portfolioTiles(portfolio))
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

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHead("Your properties") {
                if (!portfolio?.properties.isNullOrEmpty()) ArrowLink("View all", onOpenProperties)
            }
            when {
                portfolio == null && state.error == null -> LoadingCards(2, imageHeight = 104.dp)
                portfolio == null -> Text(state.error.orEmpty(), style = RfType.BodyMd, color = Rf.OnSurfaceVariant)
                portfolio.properties.isEmpty() -> RfCard {
                    Text("No properties yet. Add your first one to see it here.", style = RfType.BodyMd, color = Rf.OnSurfaceVariant)
                }
                else -> portfolio.properties.take(PROPERTY_PREVIEW).forEach { property ->
                    PropertyCard(property, portfolio) { onOpenProperty(property.id) }
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

        SubMeterCard()
    }
}

/** The web's sub-meter card, with its sample reading until meter readings have an API. */
@Composable
private fun SubMeterCard() {
    RfCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconTile(Lucide.Gauge, background = Rf.Container, tint = Rf.Primary, iconSize = 20.dp)
                Column(Modifier.weight(1f)) {
                    Text("Sub-meter utilities", style = RfType.HeadlineSm, color = Rf.OnSurface)
                    Text("Grid + solar micro-reading", style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
                }
                Pill("AMR", background = Rf.SecondaryContainer, content = Rf.OnSecondaryContainer, dot = true, dotColor = Rf.Secondary)
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Rf.Low).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total consumed", style = RfType.LabelSm, color = Rf.Outline)
                    Text("2,840 kWh", style = RfType.HeadlineSm.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Billable value", style = RfType.LabelSm, color = Rf.Outline)
                    Text(inr(29_820), style = RfType.HeadlineSm.copy(fontWeight = FontWeight.Bold), color = Rf.Primary)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Billed: 2,160 kWh (76%)", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.Secondary)
                    Text("Uncollected: 680 kWh", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.Tertiary)
                }
                Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Rf.Container)) {
                    Box(Modifier.weight(0.76f).fillMaxHeight().background(Rf.Secondary))
                    Box(Modifier.weight(0.24f).fillMaxHeight().background(Rf.TertiaryContainer))
                }
            }
            TonalButton("Update sub-meter photo readings", onClick = {}, icon = Lucide.Camera)
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


/** The four portfolio tiles from real data; placeholders pulse while it loads. */
@Composable
private fun portfolioTiles(portfolio: Portfolio?): List<HealthTileSpec> {
    if (portfolio == null) {
        return listOf(
            HealthTileSpec(Lucide.Building2, "", "", "", Rf.High, Rf.Primary, Rf.Secondary, loading = true),
            HealthTileSpec(Lucide.Users, "", "", "", Rf.MintSoft, Rf.Secondary, Rf.Secondary, loading = true),
            HealthTileSpec(Lucide.DoorOpen, "", "", "", Rf.Container, Rf.Primary, Rf.Primary, background = Rf.High, loading = true),
            HealthTileSpec(Lucide.Users, "", "", "", Rf.Container, Rf.Primary, Rf.OnSurfaceVariant, background = Rf.High, loading = true)
        )
    }
    val properties = portfolio.properties
    val cities = properties.map { it.city }.filter(String::isNotBlank).distinct()
    val housed = portfolio.tenants.count { it.roomId != null }
    val noRoom = portfolio.tenants.size - housed
    val vacant = portfolio.allRooms.count { portfolio.occupants(it.id).isEmpty() }
    // Rooms that can take another tenant: empty ones, and double/triple/shared rooms not yet full
    val withSpace = portfolio.allRooms.count { it.isAvailable }
    val partlyFilled = portfolio.allRooms.count { it.isAvailable && portfolio.occupants(it.id).isNotEmpty() }

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
            "$housed in a room", Rf.MintSoft, Rf.Secondary, if (noRoom > 0) Rf.OnSurfaceVariant else Rf.Secondary
        ),
        HealthTileSpec(
            Lucide.DoorOpen, "Vacant", "$vacant ${if (vacant == 1) "Room" else "Rooms"}",
            if (vacant == 0) "Every room is let" else "Ready to let", Rf.Container, Rf.Primary, Rf.Primary, background = Rf.High
        ),
        HealthTileSpec(
            Lucide.Users, "Space", "$withSpace With space",
            when {
                withSpace == 0 -> "Every room is full"
                partlyFilled > 0 -> "$partlyFilled partly filled"
                else -> "Rooms that take a tenant"
            },
            Rf.Container, Rf.Primary, Rf.OnSurfaceVariant, background = Rf.High
        )
    )
}
