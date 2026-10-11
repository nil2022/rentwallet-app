package com.thebackendguy.rentflow.data

import com.thebackendguy.rentflow.data.remote.ApiEnvelope
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.BillDto
import com.thebackendguy.rentflow.data.remote.BillSummaryDto
import com.thebackendguy.rentflow.data.remote.BillTotalsDto
import com.thebackendguy.rentflow.data.remote.Network
import com.thebackendguy.rentflow.data.remote.apiCall
import com.thebackendguy.rentflow.data.remote.discard
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/* ------------------------------ Months ------------------------------ */

private val ApiMonth = DateTimeFormatter.ofPattern("yyyy-MM", Locale.ENGLISH)
private val MonthYearLabel = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

/** "2026-10", the API's billing month. */
fun YearMonth.apiMonth(): String = format(ApiMonth)

/** "Oct 2026". */
fun YearMonth.label(): String = format(MonthYearLabel)

fun parseApiMonth(value: String): YearMonth? = runCatching { YearMonth.parse(value, ApiMonth) }.getOrNull()

/** "Oct 2026" for the API's "2026-10", or the text as it is. */
fun monthLabel(value: String): String = parseApiMonth(value)?.label() ?: value

/** The API has no due date: a bill from an earlier month that isn't fully paid is overdue. */
val BillDto.isOverdue: Boolean get() = paymentStatus != "paid" && monthYear < YearMonth.now().apiMonth()

/** What an expense the tenant paid for the landlord was, as the API names it, with the web's labels. */
val DeductionCategories = listOf(
    "maintenance" to "Maintenance", "repair" to "Repair", "plumbing" to "Plumbing", "electrical" to "Electrical", "other" to "Other"
)

fun categoryLabel(category: String?): String = DeductionCategories.find { it.first == category }?.second ?: "Other"

/** Cash, UPI or Cheque. */
fun methodLabel(method: String?): String = when (method) {
    "cash" -> "Cash"
    "upi" -> "UPI"
    "cheque" -> "Cheque"
    else -> "—"
}

/** A stored file link that points to a PDF (its key keeps the file's extension). */
fun isPdfLink(url: String?): Boolean = url != null && Regex("\\.pdf($|[?#])", RegexOption.IGNORE_CASE).containsMatchIn(url)

/* ------------------------------ Rent status ------------------------------ */

/** The web's rent status filter, most urgent first; [key] is the web's status value. */
enum class RentKind(val key: String, val label: String) {
    Overdue("overdue", "Overdue"),
    Due("pending", "Due"),
    Paid("paid", "Paid"),
    Unbilled("unbilled", "Not billed")
}

/**
 * Where a tenant stands on rent: [amount] is what is overdue or due (or what was paid),
 * [advance] the money they paid beyond their bills that no bill has used yet.
 */
data class RentStatus(val kind: RentKind, val amount: Double = 0.0, val advance: Double = 0.0)

/**
 * The web's paymentState: an unpaid bill from an earlier month makes the tenant
 * overdue, then this month's bill is due or paid; with no bill this month, not billed.
 */
fun rentStatus(current: BillDto?, overdue: List<BillDto>, advance: Double): RentStatus {
    val held = round2(advance)
    val overdueAmount = overdue.sumOf { it.pendingAmount }
    return when {
        overdueAmount > 0 -> RentStatus(RentKind.Overdue, overdueAmount, held)
        current != null && current.pendingAmount > 0 -> RentStatus(RentKind.Due, current.pendingAmount, held)
        current != null -> RentStatus(RentKind.Paid, current.paidAmount, held)
        else -> RentStatus(RentKind.Unbilled, advance = held)
    }
}

/* ------------------------------ Bills ------------------------------ */

/** The covered part of bills: [collected] money (received plus advance used) and [adjusted] expenses taken off. */
data class SettledSplit(val collected: Double, val adjusted: Double) {
    operator fun plus(other: SettledSplit) = SettledSplit(collected + other.collected, adjusted + other.adjusted)
}

/** The web's splitSettled: expenses never count for more than what is covered. */
fun splitSettled(settled: Double, adjusted: Double): SettledSplit {
    val covered = maxOf(settled, 0.0)
    val expenses = minOf(maxOf(adjusted, 0.0), covered)
    return SettledSplit(round2(covered - expenses), round2(expenses))
}

/** A bill's own split; its covered part is what isn't still due. */
val BillDto.settledSplit: SettledSplit get() = splitSettled(totalDue - pendingAmount, adjustedAmount)

/**
 * What Rent Collection shows: the chosen month's bills with the API's totals, and
 * every bill still unpaid (pending or partial), from which the earlier months' are overdue.
 */
data class CollectionBills(val bills: List<BillDto>, val totals: BillTotalsDto?, val unpaid: List<BillDto>) {
    val overdue: List<BillDto> = unpaid.filter { it.isOverdue }
        .sortedWith(compareByDescending<BillDto> { it.monthYear }.thenBy { it.tenant?.fullName.orEmpty() })
}

/**
 * What Overview shows about rent this month (the web's useRentOverview): this month's
 * [bills], the [overdueBills] still unpaid from earlier months (oldest first), and the
 * money: [billed], [collected] (money received plus advance used), [adjusted] (expenses
 * the tenants paid and took off; with collected never more than billed) and the
 * [advance] held across all months.
 */
data class RentOverview(
    val month: YearMonth,
    val bills: List<BillDto>,
    val overdueBills: List<BillDto>,
    val billed: Double,
    val collected: Double,
    val adjusted: Double,
    val advance: Double
) {
    /** Settled: collected plus expenses taken off. */
    val percent: Int = if (billed > 0) Math.round((collected + adjusted) / billed * 100).toInt() else 0
    val overdueAmount: Double = overdueBills.sumOf { it.pendingAmount }
    val overdueTenants: Int = overdueBills.map { it.tenantId }.distinct().size
    val overdueSince: String? = overdueBills.firstOrNull()?.monthYear

    // Electricity in this month's bills
    private val metered = bills.filter { it.electricity != null }
    val units: Double = metered.sumOf { it.electricity?.unitsConsumed ?: 0.0 }
    val electricityAmount: Double = metered.sumOf { it.electricity?.amount ?: 0.0 }
    val readRooms: Int = metered.map { it.roomId }.distinct().size
    val billedRooms: Int = bills.map { it.roomId }.distinct().size
}

/**
 * The landlord's bills from the API (the calls the web's Rent Collection makes).
 * Screens load what they show; [version] goes up after any change to a bill, so they
 * load again.
 */
object PaymentStore {
    private const val PAGE_SIZE = 100 // the API's maximum

    private val api get() = Network.payments
    private val _version = MutableStateFlow(0)

    val version: StateFlow<Int> = _version.asStateFlow()

    /** Call after a bill is saved or deleted. */
    fun changed() {
        _version.value++
    }

    /** One month's bills (all properties, or [propertyId]) and the unpaid bills of any month. */
    suspend fun collection(month: YearMonth, propertyId: String?): ApiResult<CollectionBills> = coroutineScope {
        val monthCall = async { apiCall { api.bills(monthYear = month.apiMonth(), propertyId = propertyId, limit = PAGE_SIZE) } }
        val pendingCall = async { apiCall { api.bills(status = "pending", propertyId = propertyId, limit = PAGE_SIZE) } }
        val partialCall = async { apiCall { api.bills(status = "partial", propertyId = propertyId, limit = PAGE_SIZE) } }
        val monthBills = when (val r = monthCall.await()) {
            is ApiResult.Fail -> return@coroutineScope r
            is ApiResult.Ok -> r.value
        }
        val unpaid = mutableListOf<BillDto>()
        for (call in listOf(pendingCall, partialCall)) {
            when (val r = call.await()) {
                is ApiResult.Fail -> return@coroutineScope r
                is ApiResult.Ok -> unpaid += r.value.data.orEmpty()
            }
        }
        ApiResult.Ok(CollectionBills(monthBills.data.orEmpty(), monthBills.summary, unpaid))
    }

    /**
     * Every billed tenant's [RentStatus], by tenant id, from the web's four calls: this
     * month's bills, the pending and the partly paid ones (earlier months are overdue),
     * and the bills holding advance. A tenant in a room who isn't here is not billed.
     */
    suspend fun rentStatuses(): ApiResult<Map<String, RentStatus>> = coroutineScope {
        val month = YearMonth.now().apiMonth()
        val calls = listOf(
            async { apiCall { api.bills(monthYear = month, limit = PAGE_SIZE) } },
            async { apiCall { api.bills(status = "pending", limit = PAGE_SIZE) } },
            async { apiCall { api.bills(status = "partial", limit = PAGE_SIZE) } },
            async { apiCall { api.bills(hasAdvance = true, limit = PAGE_SIZE) } }
        )
        val lists = calls.mapIndexed { i, call ->
            when (val r = call.await()) {
                is ApiResult.Ok -> r.value.data.orEmpty()
                // The advance call is extra: without it the statuses still show
                is ApiResult.Fail -> if (i == 3) emptyList() else return@coroutineScope r
            }
        }
        val current = lists[0].associateBy { it.tenantId }
        val overdue = (lists[1] + lists[2]).filter { it.monthYear < month }.groupBy { it.tenantId }
        val held = lists[3].groupBy { it.tenantId }
        val ids = current.keys + overdue.keys + held.keys
        ApiResult.Ok(ids.associateWith { id ->
            rentStatus(current[id], overdue[id].orEmpty(), held[id].orEmpty().sumOf { it.advanceLeft })
        })
    }

    /** This month's totals across all properties (the Profile's Collected tile); one bill is enough for them. */
    suspend fun monthTotals(): ApiResult<BillTotalsDto> =
        when (val r = apiCall { api.bills(monthYear = YearMonth.now().apiMonth(), limit = 1) }) {
            is ApiResult.Fail -> r
            is ApiResult.Ok -> ApiResult.Ok(r.value.summary ?: BillTotalsDto())
        }

    /** Overview's rent figures, from the web dashboard's four calls. */
    suspend fun overview(): ApiResult<RentOverview> = coroutineScope {
        val now = YearMonth.now()
        val month = now.apiMonth()
        val monthCall = async { apiCall { api.bills(monthYear = month, limit = PAGE_SIZE) } }
        val pendingCall = async { apiCall { api.bills(status = "pending", limit = PAGE_SIZE) } }
        val partialCall = async { apiCall { api.bills(status = "partial", limit = PAGE_SIZE) } }
        val advanceCall = async { apiCall { api.bills(hasAdvance = true, limit = PAGE_SIZE) } }
        val monthBills = when (val r = monthCall.await()) {
            is ApiResult.Fail -> return@coroutineScope r
            is ApiResult.Ok -> r.value
        }
        val unpaid = mutableListOf<BillDto>()
        for (call in listOf(pendingCall, partialCall)) {
            when (val r = call.await()) {
                is ApiResult.Fail -> return@coroutineScope r
                is ApiResult.Ok -> unpaid += r.value.data.orEmpty()
            }
        }
        // The advance call is extra: without it the advance shows as none
        val advance = (advanceCall.await() as? ApiResult.Ok)?.value
        val bills = monthBills.data.orEmpty()
        val summary = monthBills.summary
        val money = splitSettled(
            summary?.totalSettled ?: bills.sumOf { it.totalDue - it.pendingAmount },
            summary?.totalAdjusted ?: bills.sumOf { it.adjustedAmount }
        )
        ApiResult.Ok(
            RentOverview(
                month = now,
                bills = bills,
                overdueBills = unpaid.filter { it.monthYear < month }
                    .sortedWith(compareBy<BillDto> { it.monthYear }.thenByDescending { it.pendingAmount }),
                billed = summary?.totalDue ?: bills.sumOf { it.totalDue },
                collected = money.collected,
                adjusted = money.adjusted,
                advance = advance?.summary?.totalAdvanceHeld ?: advance?.data.orEmpty().sumOf { it.advanceLeft }
            )
        )
    }

    /** One tenant's bills, one room's bills, or one month's bills (newest month first). */
    suspend fun bills(tenantId: String? = null, roomId: String? = null, month: YearMonth? = null): ApiResult<List<BillDto>> =
        when (val r = apiCall { api.bills(monthYear = month?.apiMonth(), tenantId = tenantId, roomId = roomId, limit = PAGE_SIZE) }) {
            is ApiResult.Fail -> r
            is ApiResult.Ok -> ApiResult.Ok(r.value.data.orEmpty())
        }

    /** One bill, fresh from the API, with its payments and deductions. */
    suspend fun bill(id: String): ApiResult<BillDto> =
        when (val r = apiCall { api.bill(id) }) {
            is ApiResult.Fail -> r
            is ApiResult.Ok -> r.value.data?.let { ApiResult.Ok(it) } ?: ApiResult.Fail("The server sent an empty reply. Please try again.")
        }

    /** Rent History: totals for every month from [from] to [to], for all properties or [propertyId]. */
    suspend fun summary(from: YearMonth, to: YearMonth, propertyId: String?): ApiResult<BillSummaryDto> =
        when (val r = apiCall { api.summary(from.apiMonth(), to.apiMonth(), propertyId) }) {
            is ApiResult.Fail -> r
            is ApiResult.Ok -> r.value.data?.let { ApiResult.Ok(it) } ?: ApiResult.Fail("The server sent an empty reply. Please try again.")
        }

    /** Saves a new bill ([billId] null) or changes one; every screen showing bills loads again. */
    suspend fun save(billId: String?, body: JsonObject): ApiResult<ApiEnvelope<BillDto>> {
        val result = apiCall { if (billId == null) api.createBill(body) else api.updateBill(body) }
        if (result is ApiResult.Ok) changed()
        return result
    }

    /** Saves a bill's meter photo ([key] null removes it). */
    suspend fun setMeterPhoto(billId: String, key: String?): ApiResult<Unit> = changing {
        apiCall { api.setMeterPhoto(buildJsonObject { put("paymentId", billId); put("meterPhoto", key) }) }.discard()
    }

    /**
     * Adds a payment or an expense deduction to a bill. [fields] adds kind, amount,
     * date, method, proof, notes, title and category; the bill's id goes in by itself.
     */
    suspend fun addReceipt(billId: String, fields: JsonObjectBuilder.() -> Unit): ApiResult<ApiEnvelope<BillDto>> = changing {
        apiCall { api.addReceipt(buildJsonObject { put("paymentId", billId); fields() }) }
    }

    /** Changes one entry of a bill; [fields] holds only what changed (null proof removes the photo). */
    suspend fun updateReceipt(billId: String, receiptId: String, fields: JsonObjectBuilder.() -> Unit): ApiResult<ApiEnvelope<BillDto>> = changing {
        apiCall { api.updateReceipt(buildJsonObject { put("paymentId", billId); put("receiptId", receiptId); fields() }) }
    }

    /** Removes one entry of a bill; what is still due goes back up. */
    suspend fun deleteReceipt(billId: String, receiptId: String): ApiResult<ApiEnvelope<BillDto>> = changing {
        apiCall { api.deleteReceipt(billId, receiptId) }
    }

    suspend fun delete(billId: String): ApiResult<Unit> = changing { apiCall { api.deleteBill(billId) }.discard() }

    private suspend fun <T> changing(block: suspend () -> ApiResult<T>): ApiResult<T> {
        val result = block()
        if (result is ApiResult.Ok) changed()
        return result
    }
}

/* ------------------------------ Advance ------------------------------ */

fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0

/**
 * A bill as the advance rules see it: what it costs and what the tenant covered
 * themselves ([paid]: money received plus expenses taken off, as the backend counts it).
 */
data class AdvanceBill(val id: String, val monthYear: String, val total: Double, val paid: Double)

/**
 * The result for one bill: advance from earlier bills it uses ([credit], from the
 * months in [sources]), what is still due, what was paid beyond it ([advance]) and
 * how much of that no later bill used ([left]).
 */
data class AdvanceRow(
    val id: String,
    val monthYear: String,
    val credit: Double,
    val sources: List<String>,
    val pending: Double,
    val advance: Double,
    val left: Double
)

/**
 * The backend's advance rules (reconcileTenantCredit in the API), run here to
 * preview a payment before it is saved, as the web's carryAdvance does. Walking
 * from the oldest month, each bill's own money counts first, then advance left by
 * earlier bills (oldest first) covers what is still due; money beyond a bill's
 * total is advance for later bills only. Rows come oldest month first.
 */
fun carryAdvance(bills: List<AdvanceBill>): List<AdvanceRow> {
    class Pot(val index: Int, var left: Double)
    val pots = mutableListOf<Pot>()
    val rows = bills.sortedBy { it.monthYear }.mapIndexed { index, bill ->
        val total = round2(bill.total)
        val paid = round2(bill.paid)
        var need = round2(maxOf(total - paid, 0.0))
        val sources = mutableListOf<String>()
        var credit = 0.0
        pots.forEach { pot ->
            if (need <= 0 || pot.left <= 0) return@forEach
            val take = round2(minOf(pot.left, need))
            pot.left = round2(pot.left - take)
            need = round2(need - take)
            credit = round2(credit + take)
            sources += bills.sortedBy { it.monthYear }[pot.index].monthYear
        }
        val advance = round2(maxOf(paid - total, 0.0))
        if (advance > 0) pots += Pot(index, advance)
        AdvanceRow(bill.id, bill.monthYear, credit, sources, need, advance, 0.0)
    }
    return rows.mapIndexed { index, row -> row.copy(left = pots.find { it.index == index }?.left ?: 0.0) }
}
