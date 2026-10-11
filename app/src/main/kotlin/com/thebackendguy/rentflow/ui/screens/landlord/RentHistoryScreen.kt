package com.thebackendguy.rentflow.ui.screens.landlord

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebackendguy.rentflow.data.PaymentStore
import com.thebackendguy.rentflow.data.SettledSplit
import com.thebackendguy.rentflow.data.apiMonth
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.monthLabel
import com.thebackendguy.rentflow.data.parseApiMonth
import com.thebackendguy.rentflow.data.splitSettled
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.BillSummaryDto
import com.thebackendguy.rentflow.data.remote.MonthTotalsDto
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.CsvShare
import com.thebackendguy.rentflow.ui.components.ErrorState
import com.thebackendguy.rentflow.ui.components.LoadingCards
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.SelectField
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.components.softShadow
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import java.time.Month
import java.time.YearMonth
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * The web's Rent History (RentHistory.jsx): billed, collected and still due for each
 * month of a range, from GET /landlord/payment/summary. A month opens in Rent Collection.
 */

/** The web's ranges: the last 6 or 12 months, or the Indian financial year (April to March). */
private enum class HistoryRange(val key: String) { Six("6"), Twelve("12"), FinancialYear("fy") }

private data class Span(val from: YearMonth, val to: YearMonth, val label: String)

private fun HistoryRange.span(now: YearMonth = YearMonth.now()): Span = when (this) {
    HistoryRange.Six -> Span(now.minusMonths(5), now, "Last 6 months")
    HistoryRange.Twelve -> Span(now.minusMonths(11), now, "Last 12 months")
    HistoryRange.FinancialYear -> {
        val start = if (now.month >= Month.APRIL) now.year else now.year - 1
        Span(YearMonth.of(start, Month.APRIL), YearMonth.of(start + 1, Month.MARCH), "FY $start-${(start + 1) % 100}")
    }
}

/** What share of the bills is settled: money collected plus expenses taken off. */
private fun settledPercent(m: MonthTotalsDto): Int = if (m.totalDue > 0) (m.totalSettled / m.totalDue * 100).roundToInt() else 0

/** The web's moneyOf: the settled part as money collected and expenses taken off. */
private fun moneyOf(m: MonthTotalsDto): SettledSplit = splitSettled(m.totalSettled, m.totalAdjusted)

@Composable
fun RentHistoryScreen(shell: Shell, onBack: () -> Unit, onOpenMonth: (month: String, propertyId: String?) -> Unit) {
    val context = LocalContext.current
    val portfolio = portfolioState().data
    val properties = portfolio?.properties.orEmpty()
    var rangeIndex by rememberSaveable { mutableIntStateOf(HistoryRange.Twelve.ordinal) }
    var propertyId by rememberSaveable { mutableStateOf<String?>(null) }
    val range = HistoryRange.entries[rangeIndex]
    val span = range.span()

    // Bills change on other screens, so ask again whenever one does
    val version by PaymentStore.version.collectAsState()
    var retry by remember { mutableIntStateOf(0) }
    var summary by remember { mutableStateOf<BillSummaryDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(rangeIndex, propertyId, version, retry) {
        loading = true
        error = null
        when (val r = PaymentStore.summary(span.from, span.to, propertyId)) {
            is ApiResult.Ok -> summary = r.value
            is ApiResult.Fail -> error = r.message
        }
        loading = false
    }
    val propertyName = properties.find { it.id == propertyId }?.propertyName

    AppPage(topBar = { shell.TopBar("Rent History", onBack = onBack) }, bottomBar = { shell.BottomNav(4) }) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Rent History", style = RfType.HeadlineMd.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
                    Text("Billed, collected and still due, month by month.", style = RfType.BodySm, color = Rf.OnSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
                val months = summary?.months.orEmpty()
                Row(
                    Modifier.height(36.dp).pressable(if (months.isEmpty()) null else ({
                        shareHistory(context, months, listOfNotNull(propertyName, span.label).joinToString(" "))
                    })).softShadow(RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)).background(Rf.Lowest)
                        .padding(horizontal = 12.dp).alpha(if (months.isEmpty()) 0.5f else 1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    LIcon(Lucide.Download, size = 14.dp, tint = Rf.Primary)
                    Text("CSV", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.Primary)
                }
            }

            // Property and range
            RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(12.dp), spacing = 10.dp) {
                SelectField(
                    label = "Property",
                    value = propertyName ?: "All properties",
                    options = listOf("All properties") + properties.map { it.propertyName },
                    onSelect = { propertyId = if (it == 0) null else properties[it - 1].id },
                    icon = Lucide.Building2
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HistoryRange.entries.forEachIndexed { index, r ->
                        val on = index == rangeIndex
                        Text(
                            r.span().label, style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold),
                            color = if (on) Rf.OnPrimary else Rf.OnSurface, maxLines = 1,
                            modifier = Modifier.pressable({ rangeIndex = index }, pressScale = 0.96f).clip(CircleShape)
                                .background(if (on) Rf.Primary else Rf.Lowest)
                                .border(1.dp, if (on) Rf.Primary else Rf.OutlineVariant, CircleShape)
                                .padding(horizontal = 11.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            val data = summary
            when {
                data == null && error != null -> ErrorState(error.orEmpty(), onRetry = { retry++ })
                data == null -> LoadingCards(2)
                else -> Column(Modifier.alpha(if (loading) 0.6f else 1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Kpis(data)
                    if (data.total.bills == 0) {
                        RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(24.dp)) {
                            Text("No bills in this period", style = RfType.BodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Text(
                                "Bills recorded in Rent Collection or on a tenant’s page show up here month by month.",
                                style = RfType.BodySm, color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        }
                    } else {
                        HistoryChart(data.months) { onOpenMonth(it, propertyId) }
                        MonthCards(data.months) { onOpenMonth(it, propertyId) }
                    }
                }
            }
        }
    }
}

/* ------------------------------ KPIs ------------------------------ */

@Composable
private fun Kpis(data: BillSummaryDto) {
    val now = YearMonth.now().apiMonth()
    val total = data.total
    val overdue = data.months.filter { it.monthYear < now }.sumOf { it.totalPending }
    val billedMonths = data.months.count { it.bills > 0 }
    val percent = settledPercent(total)
    val money = moneyOf(total)
    val colors = chartColors()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Kpi("Billed", inr(total.totalDue), Rf.OnSurface, "$billedMonths ${if (billedMonths == 1) "month" else "months"} with bills", null, Modifier.weight(1f))
            val expenses = money.adjusted > 0
            Kpi(
                "Collected", inr(money.collected), Rf.Secondary,
                if (expenses) "+ ${inr(money.adjusted)} expenses taken off • $percent% settled" else "$percent% of billed",
                { PartsBar(colors.parts(total), total.totalDue, 6.dp, Modifier.padding(top = 8.dp)) }, Modifier.weight(1f),
                subColor = if (expenses) Rf.Amber else null
            )
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Kpi(
                "Still due", inr(total.totalPending), if (total.totalPending > 0) Rf.Error else Rf.OnSurface,
                if (overdue > 0) "${inr(overdue)} overdue from earlier months" else "Nothing overdue", null, Modifier.weight(1f)
            )
            Kpi("Advance held", inr(total.totalAdvanceHeld), if (total.totalAdvanceHeld > 0) Rf.Primary else Rf.OnSurface, "Paid ahead, not used yet", null, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Kpi(
    label: String, value: String, color: Color, sub: String, bar: (@Composable () -> Unit)?, modifier: Modifier,
    subColor: Color? = null
) {
    RfCard(modifier.fillMaxHeight(), shape = RoundedCornerShape(16.dp), padding = PaddingValues(12.dp)) {
        Caps(label)
        Text(value, style = RfType.HeadlineSm.copy(fontWeight = FontWeight.ExtraBold), color = color, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
        bar?.invoke()
        // A coloured line under the figure is bold, the plain one muted
        Text(
            sub, style = RfType.LabelSm.copy(fontWeight = if (subColor != null) FontWeight.Bold else FontWeight.Medium),
            color = subColor ?: Rf.OnSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/* ------------------------------ Chart ------------------------------ */

/**
 * Collected (green, as on the web), expenses taken off (amber, as on the app's bills)
 * and still due (red, so it stays apart from the amber).
 */
private data class ChartColors(val collected: Color, val adjusted: Color, val due: Color) {
    /** Money and expenses as parts of a month's bar. */
    fun parts(m: MonthTotalsDto): List<Pair<Double, Color>> = moneyOf(m).let { listOf(it.collected to collected, it.adjusted to adjusted) }
}

@Composable
private fun chartColors(): ChartColors =
    ChartColors(if (Rf.Surface.luminance() < 0.5f) Color(0xFF18966D) else Color(0xFF0E8A5F), Rf.Amber, Rf.Error)

/** "₹1.5L", "₹68K", "₹500" for the axis. */
private fun compactRupees(value: Double): String = when {
    value >= 100000 -> "₹" + "%.1f".format(value / 100000).removeSuffix(".0") + "L"
    value >= 1000 -> "₹${(value / 1000).roundToInt()}K"
    else -> "₹${value.roundToInt()}"
}

/** A rounded "nice" step so the axis shows 3 to 5 lines. */
private fun niceStep(max: Double): Double {
    val rough = max / 4
    val power = 10.0.pow(floor(log10(if (rough > 0) rough else 1.0)))
    return listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * power }.firstOrNull { it >= rough } ?: (power * 10)
}

/**
 * One stacked bar per month: collected at the bottom, then expenses taken off, still due
 * on top, together the month's bill total. The first tap on a month shows its figures,
 * a second opens it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryChart(months: List<MonthTotalsDto>, onOpenMonth: (String) -> Unit) {
    val (collected, adjusted, due) = chartColors()
    val measurer = rememberTextMeasurer()
    val axisStyle = RfType.LabelSm.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Rf.OnSurfaceVariant)
    val nowStyle = axisStyle.copy(color = Rf.OnSurface, fontWeight = FontWeight.Bold)
    val grid = Rf.Low
    val emptyLine = Rf.OutlineVariant
    val highlight = Rf.OnSurface.copy(alpha = 0.06f)
    val now = YearMonth.now().apiMonth()
    var active by remember(months) { mutableStateOf<Int?>(null) }

    val max = months.maxOfOrNull { it.totalDue } ?: 0.0
    val step = niceStep(if (max > 0) max else 1000.0)
    val top = max(step, ceil(max / step) * step)

    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(14.dp), spacing = 10.dp) {
        Text("Billed vs collected, month by month", style = RfType.BodyMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOfNotNull(
                "Collected" to collected,
                ("Expenses taken off" to adjusted).takeIf { months.any { it.totalAdjusted > 0 } },
                "Still due" to due
            ).forEach { (label, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
                    Text(label, style = RfType.LabelSm.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurfaceVariant)
                }
            }
        }

        Canvas(
            Modifier.fillMaxWidth().height(200.dp).pointerInput(months) {
                detectTapGestures { position ->
                    val padLeft = 42.dp.toPx()
                    val slot = (size.width - padLeft - 6.dp.toPx()) / max(months.size, 1)
                    val index = ((position.x - padLeft) / slot).toInt()
                    if (position.x < padLeft || index !in months.indices) return@detectTapGestures
                    if (active != index) active = index else if (months[index].bills > 0) onOpenMonth(months[index].monthYear)
                }
            }
        ) {
            val padLeft = 42.dp.toPx()
            val padRight = 6.dp.toPx()
            val padTop = 12.dp.toPx()
            val padBottom = 26.dp.toPx()
            val plotHeight = size.height - padTop - padBottom
            fun y(v: Double) = padTop + plotHeight * (1 - (v / top).toFloat())
            val slot = (size.width - padLeft - padRight) / max(months.size, 1)
            val barWidth = min(18.dp.toPx(), slot * 0.58f)
            val radius = 4.dp.toPx()

            var v = 0.0
            while (v <= top + 0.5) {
                val yy = y(v)
                drawLine(grid, Offset(padLeft, yy), Offset(size.width - padRight, yy), strokeWidth = 1.dp.toPx())
                val text = measurer.measure(compactRupees(v), axisStyle)
                drawText(text, topLeft = Offset(padLeft - 8.dp.toPx() - text.size.width, yy - text.size.height / 2f))
                v += step
            }

            val everyOther = months.size > 7
            months.forEachIndexed { i, m ->
                val cx = padLeft + slot * i + slot / 2
                val x = cx - barWidth / 2
                val base = y(0.0)
                if (active == i) drawRect(highlight, Offset(padLeft + slot * i + 2.dp.toPx(), padTop), Size(slot - 4.dp.toPx(), plotHeight))
                if (m.totalDue > 0) {
                    val ySettled = y(m.totalSettled)
                    val yCollected = y(moneyOf(m).collected)
                    val hasExpenses = moneyOf(m).adjusted > 0
                    val gap = if (m.totalPending > 0 && m.totalSettled > 0) 2.dp.toPx() else 0f
                    segment(collected, x, barWidth, yCollected, base, roundTop = m.totalPending <= 0 && !hasExpenses, radius = radius)
                    if (hasExpenses) segment(adjusted, x, barWidth, ySettled, yCollected, roundTop = m.totalPending <= 0, radius = radius)
                    if (m.totalPending > 0) segment(due, x, barWidth, y(m.totalDue), ySettled - gap, roundTop = true, radius = radius)
                } else if (m.monthYear <= now) {
                    drawLine(emptyLine, Offset(x, base - 1.dp.toPx()), Offset(x + barWidth, base - 1.dp.toPx()), strokeWidth = 2.dp.toPx())
                }
                if (!everyOther || i % 2 == (months.size - 1) % 2) {
                    val label = measurer.measure(
                        parseApiMonth(m.monthYear)?.month?.name?.take(3)?.lowercase()?.replaceFirstChar { it.uppercase() } ?: m.monthYear,
                        if (m.monthYear == now) nowStyle else axisStyle
                    )
                    drawText(label, topLeft = Offset(cx - label.size.width / 2f, size.height - padBottom + 8.dp.toPx()))
                }
            }
        }

        // The tapped month's figures (the web's hover card)
        active?.let { months.getOrNull(it) }?.let { m ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Rf.InverseSurface).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    monthLabel(m.monthYear) + if (m.bills > 0) " • ${m.bills} ${if (m.bills == 1) "bill" else "bills"}" else "",
                    style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.InverseOnSurface
                )
                if (m.bills > 0) {
                    listOfNotNull(
                        Triple("Billed", m.totalDue, null),
                        Triple("Collected", moneyOf(m).collected, collected),
                        moneyOf(m).adjusted.takeIf { it > 0 }?.let { Triple("Expenses taken off", it, adjusted) },
                        Triple("Still due", m.totalPending, due),
                        if (m.totalAdvanceHeld > 0) Triple("Advance held", m.totalAdvanceHeld, null) else null
                    ).forEach { (label, value, color) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (color != null) Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
                            Text(label, style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.InverseOnSurface, modifier = Modifier.weight(1f))
                            Text(inr(value), style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.InverseOnSurface)
                        }
                    }
                    Text("Tap again to open", style = RfType.LabelSm, color = Rf.InverseOnSurface.copy(alpha = 0.7f), modifier = Modifier.padding(top = 2.dp))
                } else {
                    Text(if (m.monthYear > now) "Not started" else "No bills", style = RfType.LabelSm, color = Rf.InverseOnSurface)
                }
            }
        }
    }
}

/** A bar segment from [top] down to [bottom], its top corners rounded when [roundTop]. */
private fun DrawScope.segment(color: Color, x: Float, width: Float, top: Float, bottom: Float, roundTop: Boolean, radius: Float) {
    val height = bottom - top
    if (height <= 0) return
    if (!roundTop) {
        drawRect(color, Offset(x, top), Size(width, height))
        return
    }
    val r = min(radius, min(height, width / 2))
    drawRoundRect(color, Offset(x, top), Size(width, height), CornerRadius(r, r))
    // Square the bottom corners again
    if (height > r) drawRect(color, Offset(x, top + r), Size(width, height - r))
}

/* ------------------------------ Months ------------------------------ */

/** Newest month first; a month with bills opens in Rent Collection. */
@Composable
private fun MonthCards(months: List<MonthTotalsDto>, onOpenMonth: (String) -> Unit) {
    val now = YearMonth.now().apiMonth()
    val colors = chartColors()
    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(0.dp)) {
        months.reversed().forEachIndexed { index, m ->
            if (index > 0) HorizontalDivider(thickness = 1.dp, color = Rf.Low)
            val empty = m.bills == 0
            val percent = settledPercent(m)
            val money = moneyOf(m)
            Column(
                Modifier.fillMaxWidth().pressable(if (empty) null else ({ onOpenMonth(m.monthYear) }), pressScale = 0.99f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(monthLabel(m.monthYear), style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = if (empty) Rf.OnSurfaceVariant else Rf.OnSurface)
                    if (empty) {
                        Text(if (m.monthYear > now) "Not started" else "No bills", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
                    } else {
                        MonthTag(m, now)
                        Box(Modifier.weight(1f))
                        Text(inr(money.collected), style = RfType.LabelMd.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
                        Text("of ${inr(m.totalDue)}", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
                        LIcon(Lucide.ChevronRight, size = 16.dp, tint = Rf.Outline)
                    }
                }
                if (!empty) {
                    if (money.adjusted > 0) {
                        Text("+ ${inr(money.adjusted)} expenses taken off", style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = Rf.Amber)
                    }
                    PartsBar(colors.parts(m), m.totalDue, 5.dp)
                    Row {
                        Text(
                            "${m.bills} ${if (m.bills == 1) "bill" else "bills"} • $percent% ${if (money.adjusted > 0) "settled" else "collected"}",
                            style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, modifier = Modifier.weight(1f)
                        )
                        Text(
                            listOfNotNull(
                                m.totalPending.takeIf { it > 0 }?.let { "${inr(it)} due" },
                                m.totalAdvanceHeld.takeIf { it > 0 }?.let { "${inr(it)} advance" }
                            ).joinToString(" • "),
                            style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = if (m.totalPending > 0) Rf.Error else Rf.Primary
                        )
                    }
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = Rf.Low)
        Text(
            "Tap a month to open its bills in Rent Collection.",
            style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        )
    }
}

/** This month, Overdue (an earlier month with money still due) or Fully collected. */
@Composable
private fun MonthTag(m: MonthTotalsDto, now: String) {
    val (label, background, content) = when {
        m.monthYear == now -> Triple("This month", Rf.PrimaryFixed, Rf.Primary)
        m.monthYear < now && m.totalPending > 0 -> Triple("Overdue", Rf.ErrorContainer, Rf.Error)
        m.monthYear < now -> Triple("Fully collected", Rf.MintSoft, Rf.Secondary)
        else -> return
    }
    Text(
        label, style = RfType.LabelSm.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = content, maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(background).padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** One row per month, the way the list shows it, through Android's share menu. */
private fun shareHistory(context: android.content.Context, months: List<MonthTotalsDto>, label: String) {
    val header = listOf("Month", "Bills", "Billed", "Collected", "Expenses taken off", "Still due", "Settled %", "Advance held")
    val rows = months.map { m ->
        val money = moneyOf(m)
        listOf(
            monthLabel(m.monthYear), m.bills, m.totalDue, money.collected, money.adjusted, m.totalPending,
            if (m.bills > 0) settledPercent(m) else "", m.totalAdvanceHeld
        )
    }
    CsvShare.share(context, "rent-history-${label.trim().replace(Regex("\\s+"), "-").lowercase()}.csv", header, rows)
}
