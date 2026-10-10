package com.thebackendguy.rentflow.ui.screens.landlord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.PaymentStore
import com.thebackendguy.rentflow.data.Portfolio
import com.thebackendguy.rentflow.data.RentOverview
import com.thebackendguy.rentflow.data.apiMonth
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.label
import com.thebackendguy.rentflow.data.monthLabel
import com.thebackendguy.rentflow.data.propertyTypeLabel
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.BillDto
import com.thebackendguy.rentflow.data.remote.PropertyDto
import com.thebackendguy.rentflow.data.roomFull
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.ArrowLink
import com.thebackendguy.rentflow.ui.components.Avatar
import com.thebackendguy.rentflow.ui.components.Caption
import com.thebackendguy.rentflow.ui.components.HealthTileSpec
import com.thebackendguy.rentflow.ui.components.HealthTiles
import com.thebackendguy.rentflow.ui.components.IconTile
import com.thebackendguy.rentflow.ui.components.QuickAction
import com.thebackendguy.rentflow.ui.components.QuickActions
import com.thebackendguy.rentflow.ui.components.QuickTone
import com.thebackendguy.rentflow.ui.components.RemoteImage
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.SectionHead
import com.thebackendguy.rentflow.ui.components.Segmented
import com.thebackendguy.rentflow.ui.components.Skeleton
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

/** How many properties and rent rows the overview shows, as on the web. */
private const val PROPERTY_PREVIEW = 5
private const val RENT_PREVIEW = 6

/**
 * The landlord's home, the web's phone dashboard (MobileDashboard.jsx): Portfolio
 * Health, Quick actions, Rent this month, Properties and Electricity this month.
 * Every figure comes from the properties, rooms and tenants and from the bills.
 */
@Composable
fun LandlordOverviewScreen(
    firstName: String,
    shell: Shell,
    onRecordPayment: () -> Unit,
    onOpenCollection: () -> Unit,
    onOpenOverdue: () -> Unit,
    onCollect: (tenantId: String, month: String) -> Unit,
    onOpenProperties: () -> Unit,
    onOpenProperty: (String) -> Unit,
    onAddProperty: () -> Unit
) {
    val state = portfolioState()
    val portfolio = state.data
    LaunchedEffect(Unit) { LandlordStore.refresh() }

    // The rent figures, loaded again after any bill changes
    val billsVersion by PaymentStore.version.collectAsState()
    var retry by remember { mutableIntStateOf(0) }
    var overview by remember { mutableStateOf<RentOverview?>(null) }
    var overviewError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(billsVersion, retry) {
        when (val r = PaymentStore.overview()) {
            is ApiResult.Ok -> { overview = r.value; overviewError = null }
            is ApiResult.Fail -> overviewError = r.message
        }
    }

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
            SectionHead("Portfolio Health") { Caption((overview?.month ?: YearMonth.now()).label()) }
            HealthTiles(healthTiles(portfolio, overview, overviewError, onOpenOverdue))
        }

        QuickActions(
            listOf(
                QuickAction("Record Payment", Lucide.Plus, QuickTone.Primary, onRecordPayment),
                QuickAction("Rent Collection", Lucide.ReceiptText, QuickTone.Surface, onOpenCollection),
                QuickAction("Overdue", Lucide.TriangleAlert, QuickTone.Low, onOpenOverdue)
            )
        )

        RentThisMonth(overview, overviewError, onRetry = { retry++ }, onViewAll = onOpenCollection, onCollect = onCollect)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val properties = portfolio?.properties.orEmpty()
            Column {
                SectionHead("Properties") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.pressable(onAddProperty).clip(CircleShape).background(Rf.PrimaryFixed).padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LIcon(Lucide.Plus, size = 16.dp, tint = Rf.Primary)
                            Text("Add", style = RfType.LabelMd, color = Rf.Primary)
                        }
                        Text("View All", style = RfType.LabelMd, color = Rf.Primary, modifier = Modifier.pressable(onOpenProperties))
                    }
                }
                if (portfolio != null) {
                    Caption("${properties.take(PROPERTY_PREVIEW).size} of ${properties.size} Managed", Modifier.padding(horizontal = 2.dp))
                }
            }
            when {
                portfolio == null && state.error == null -> repeat(2) { DashPropertySkeleton() }
                portfolio == null -> Text(state.error.orEmpty(), style = RfType.BodyMd, color = Rf.OnSurfaceVariant)
                properties.isEmpty() -> RfCard {
                    Text("No properties yet. Tap “Add” to get started.", style = RfType.BodyMd, color = Rf.OnSurfaceVariant)
                }
                else -> properties.take(PROPERTY_PREVIEW).forEach { property ->
                    DashPropertyCard(property) { onOpenProperty(property.id) }
                }
            }
        }

        overview?.takeIf { it.readRooms > 0 }?.let { ElectricityCard(it) }
    }
}

/* ------------------------------ Portfolio health ------------------------------ */

/** The web's four tiles: estates, rooms, overdue and collected. Each pulses until its data is in. */
@Composable
private fun healthTiles(portfolio: Portfolio?, overview: RentOverview?, error: String?, onOpenOverdue: () -> Unit): List<HealthTileSpec> {
    val tiles = mutableListOf<HealthTileSpec>()

    // Estates and their cities
    if (portfolio == null) {
        tiles += HealthTileSpec(Lucide.Building2, "", "", "", Rf.High, Rf.Primary, Rf.Outline, loading = true)
    } else {
        val count = portfolio.properties.size
        val places = portfolio.properties.map { it.city }.filter(String::isNotBlank).distinct().take(3).joinToString(", ")
        tiles += HealthTileSpec(
            Lucide.Building2, "Active", "$count ${if (count == 1) "Estate" else "Estates"}", places.ifEmpty { "No properties yet" },
            Rf.High, Rf.Primary, Rf.Outline
        )
    }

    // Rooms: the rooms the properties say they have (as the web counts them), and how many are full
    if (portfolio == null) {
        tiles += HealthTileSpec(Lucide.DoorOpen, "", "", "", Rf.MintSoft, Rf.Secondary, Rf.Secondary, loading = true)
    } else {
        val declared = portfolio.properties.sumOf { it.roomCount }
        val rooms = portfolio.allRooms
        val full = rooms.count { roomFull(it.occupancyType, portfolio.occupants(it.id).size) }
        val percent = if (rooms.isEmpty()) 0 else Math.round(full * 100f / rooms.size)
        tiles += HealthTileSpec(
            Lucide.DoorOpen, "$percent% full", "$declared ${if (declared == 1) "Room" else "Rooms"}", "${rooms.size - full} with space",
            Rf.MintSoft, Rf.Secondary, Rf.Secondary, subColor = Rf.Secondary
        )
    }

    // Overdue from earlier months; the tile opens those bills
    tiles += when {
        overview != null && overview.overdueTenants > 0 -> HealthTileSpec(
            Lucide.TriangleAlert, "Attention", inr(overview.overdueAmount),
            "${overview.overdueTenants} ${if (overview.overdueTenants == 1) "tenant" else "tenants"} since ${monthLabel(overview.overdueSince.orEmpty())}",
            Rf.ErrorContainer, Rf.Error, Rf.Error,
            background = Rf.ErrorContainer.copy(alpha = 0.4f).compositeOver(Rf.Lowest), titleColor = Rf.OnErrorContainer, subColor = Rf.Error,
            onClick = onOpenOverdue, link = "Review"
        )
        overview != null -> HealthTileSpec(Lucide.TriangleAlert, "All clear", inr(0.0), "Nothing overdue", Rf.High, Rf.Primary, Rf.Outline)
        error != null -> HealthTileSpec(Lucide.TriangleAlert, "", "—", "Couldn’t load the bills", Rf.High, Rf.Primary, Rf.Outline)
        else -> HealthTileSpec(Lucide.TriangleAlert, "", "", "", Rf.High, Rf.Primary, Rf.Outline, loading = true)
    }

    // Collected this month of what was billed
    tiles += when {
        overview != null -> HealthTileSpec(
            Lucide.Wallet, "${overview.percent}%", inr(overview.collected),
            "of ${inr(overview.billed)} billed" + if (overview.advance > 0) " • ${inr(overview.advance)} advance" else "",
            Rf.Container, Rf.Primary, Rf.Primary, background = Rf.High, subColor = Rf.Primary
        )
        error != null -> HealthTileSpec(Lucide.Wallet, "", "—", "Couldn’t load the bills", Rf.Container, Rf.Primary, Rf.Primary, background = Rf.High)
        else -> HealthTileSpec(Lucide.Wallet, "", "", "", Rf.Container, Rf.Primary, Rf.Primary, background = Rf.High, loading = true)
    }
    return tiles
}

/** A colour over a solid one, so a tinted card stays opaque. */
private fun Color.compositeOver(base: Color): Color = Color(
    red = red * alpha + base.red * (1 - alpha),
    green = green * alpha + base.green * (1 - alpha),
    blue = blue * alpha + base.blue * (1 - alpha)
)

/* ------------------------------ Rent this month ------------------------------ */

/** This month's bills and the overdue ones from earlier months, six at a time. */
@Composable
private fun RentThisMonth(
    overview: RentOverview?,
    error: String?,
    onRetry: () -> Unit,
    onViewAll: () -> Unit,
    onCollect: (String, String) -> Unit
) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val all = overview?.let { it.bills + it.overdueBills }.orEmpty()
    val paid = all.count { it.paymentStatus == "paid" }

    Column {
        SectionHead("Rent this month") { ArrowLink("View all", onViewAll) }
        Segmented(
            options = if (overview == null) listOf("All", "Paid", "Unpaid") else listOf("All (${all.size})", "Paid ($paid)", "Unpaid (${all.size - paid})"),
            selected = filter,
            onSelect = { filter = it },
            modifier = Modifier.padding(top = 12.dp)
        )
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                overview == null && error != null -> RfCard {
                    Text(error, style = RfType.BodyMd, color = Rf.OnSurfaceVariant)
                    ArrowLink("Try again", onRetry, Modifier.padding(top = 8.dp))
                }
                overview == null -> repeat(3) { RentRowSkeleton() }
                else -> {
                    val rows = all.filter { filter == 0 || (filter == 1) == (it.paymentStatus == "paid") }.take(RENT_PREVIEW)
                    if (rows.isEmpty()) {
                        RfCard {
                            Text(
                                if (all.isEmpty()) "Nothing has been billed for ${overview.month.label()} yet." else "No bills here.",
                                style = RfType.BodyMd, color = Rf.OnSurfaceVariant
                            )
                        }
                    } else {
                        val month = overview.month.apiMonth()
                        rows.forEach { bill -> RentRow(bill, month) { onCollect(bill.tenantId, bill.monthYear) } }
                    }
                }
            }
        }
    }
}

/** The web's RentRow: a paid bill shows its total, an unpaid one what is due and Collect. */
@Composable
private fun RentRow(bill: BillDto, month: String, onCollect: () -> Unit) {
    val paid = bill.paymentStatus == "paid"
    val late = !paid && bill.monthYear < month
    val name = bill.tenant?.fullName?.takeIf(String::isNotBlank) ?: "Tenant"
    val place = listOfNotNull(bill.room?.roomNumber?.let { "Room $it" }, monthLabel(bill.monthYear).takeIf { late }).joinToString(" • ")
    RfCard(
        shape = RoundedCornerShape(12.dp),
        padding = PaddingValues(14.dp),
        color = if (late) Rf.ErrorContainer.copy(alpha = 0.2f).compositeOver(Rf.Lowest) else Rf.Lowest
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(
                initials(name), background = if (late) Rf.ErrorContainer else Rf.MintSoft, content = if (late) Rf.Error else Rf.Secondary,
                size = 36.dp, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold)
            )
            Column(Modifier.weight(1f)) {
                Text(name, style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (place.isNotEmpty()) {
                    Text(place, style = RfType.LabelSm, color = if (late) Rf.Error else Rf.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    inr(if (paid) bill.totalDue else bill.pendingAmount),
                    style = RfType.BodyLg.copy(fontWeight = FontWeight.Bold), color = if (late) Rf.Error else Rf.OnSurface
                )
                if (paid) {
                    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Rf.Secondary))
                        Text("Paid", style = RfType.LabelSm, color = Rf.Secondary)
                    }
                } else {
                    Text(
                        if (late) "Overdue • Collect" else "Due • Collect", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Primary,
                        modifier = Modifier.padding(top = 2.dp).pressable(onCollect)
                    )
                }
            }
        }
    }
}

@Composable
private fun RentRowSkeleton() {
    RfCard(shape = RoundedCornerShape(12.dp), padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Skeleton(Modifier.size(36.dp), radius = 18.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Skeleton(Modifier.fillMaxWidth(0.6f).height(13.dp))
                Skeleton(Modifier.fillMaxWidth(0.35f).height(10.dp), radius = 5.dp, color = Rf.Low)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Skeleton(Modifier.width(56.dp).height(15.dp))
                Skeleton(Modifier.width(66.dp).height(10.dp), radius = 5.dp, color = Rf.Low)
            }
        }
    }
}

/* ------------------------------ Properties ------------------------------ */

/** The web dashboard's property card: photo with the type and Total Rooms, then name and place. */
@Composable
private fun DashPropertyCard(property: PropertyDto, onOpen: () -> Unit) {
    RfCard(padding = PaddingValues(0.dp), onClick = onOpen) {
        Box(Modifier.fillMaxWidth().height(144.dp)) {
            RemoteImage(property.images.firstOrNull()?.url, Modifier.fillMaxSize(), placeholderIcon = Lucide.Building2)
            Row(
                Modifier.align(Alignment.TopEnd).padding(10.dp).clip(CircleShape).background(Color(0xCC213145))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF85F8C4)))
                Text(propertyTypeLabel(property.propertyType), style = RfType.LabelSm, color = Color(0xFFEAF1FF))
            }
            Column(
                Modifier.align(Alignment.BottomStart).padding(10.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xD9213145))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Total Rooms", style = RfType.LabelSm.copy(lineHeight = 13.sp), color = Color(0xCCD3E4FE))
                Text("${property.roomCount}", style = RfType.HeadlineSm.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
        }
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    property.propertyName.ifBlank { "Untitled property" }, style = RfType.BodyLg.copy(fontWeight = FontWeight.SemiBold),
                    color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${listOf(property.city, property.state).filter(String::isNotBlank).joinToString(", ")} • ${property.roomCount} Units Total",
                    style = RfType.LabelSm, color = Rf.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Rf.Low), contentAlignment = Alignment.Center) {
                LIcon(Lucide.ChevronRight, size = 20.dp, tint = Rf.Primary)
            }
        }
    }
}

@Composable
private fun DashPropertySkeleton() {
    RfCard(padding = PaddingValues(0.dp)) {
        Skeleton(Modifier.fillMaxWidth().height(144.dp), radius = 0.dp)
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Skeleton(Modifier.fillMaxWidth(0.55f).height(15.dp))
                Skeleton(Modifier.fillMaxWidth(0.8f).height(10.dp), radius = 5.dp, color = Rf.Low)
            }
            Skeleton(Modifier.size(36.dp))
        }
    }
}

/* ------------------------------ Electricity ------------------------------ */

/** Units and their amount in this month's bills, once a bill has a meter reading. */
@Composable
private fun ElectricityCard(overview: RentOverview) {
    RfCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconTile(Lucide.Zap, background = Rf.Container, tint = Rf.Primary, iconSize = 20.dp)
                Column(Modifier.weight(1f)) {
                    Text("Electricity this month", style = RfType.HeadlineSm, color = Rf.OnSurface)
                    Text("${overview.readRooms} of ${overview.billedRooms} rooms have a reading", style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Rf.Low).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total consumed", style = RfType.LabelSm, color = Rf.Outline)
                    Text("${unitsLabel(overview.units)} units", style = RfType.HeadlineSm.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Billed as", style = RfType.LabelSm, color = Rf.Outline)
                    Text(inr(overview.electricityAmount), style = RfType.HeadlineSm.copy(fontWeight = FontWeight.Bold), color = Rf.Primary)
                }
            }
        }
    }
}

/** "1,284" or "12.5", with Indian grouping like the web's toLocaleString("en-IN"). */
private fun unitsLabel(units: Double): String {
    val format = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"))
    format.maximumFractionDigits = if (units == Math.floor(units)) 0 else 2
    return format.format(units)
}
