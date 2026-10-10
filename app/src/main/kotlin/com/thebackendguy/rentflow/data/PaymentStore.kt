package com.thebackendguy.rentflow.data

import com.thebackendguy.rentflow.data.remote.ApiEnvelope
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.BillDto
import com.thebackendguy.rentflow.data.remote.BillTotalsDto
import com.thebackendguy.rentflow.data.remote.Network
import com.thebackendguy.rentflow.data.remote.apiCall
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
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

/** Cash, UPI or Cheque. */
fun methodLabel(method: String?): String = when (method) {
    "cash" -> "Cash"
    "upi" -> "UPI"
    "cheque" -> "Cheque"
    else -> "—"
}

/** A stored file link that points to a PDF (its key keeps the file's extension). */
fun isPdfLink(url: String?): Boolean = url != null && Regex("\\.pdf($|[?#])", RegexOption.IGNORE_CASE).containsMatchIn(url)

/* ------------------------------ Bills ------------------------------ */

/**
 * What Rent Collection shows: the chosen month's bills with the API's totals, and
 * every bill still unpaid (pending or partial), from which the earlier months' are overdue.
 */
data class CollectionBills(val bills: List<BillDto>, val totals: BillTotalsDto?, val unpaid: List<BillDto>) {
    val overdue: List<BillDto> = unpaid.filter { it.isOverdue }
        .sortedWith(compareByDescending<BillDto> { it.monthYear }.thenBy { it.tenant?.fullName.orEmpty() })
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

    /** One tenant's bills, one room's bills, or one month's bills (newest month first). */
    suspend fun bills(tenantId: String? = null, roomId: String? = null, month: YearMonth? = null): ApiResult<List<BillDto>> =
        when (val r = apiCall { api.bills(monthYear = month?.apiMonth(), tenantId = tenantId, roomId = roomId, limit = PAGE_SIZE) }) {
            is ApiResult.Fail -> r
            is ApiResult.Ok -> ApiResult.Ok(r.value.data.orEmpty())
        }

    /** Saves a new bill ([billId] null) or changes one; every screen showing bills loads again. */
    suspend fun save(billId: String?, body: JsonObject): ApiResult<ApiEnvelope<BillDto>> {
        val result = apiCall { if (billId == null) api.createBill(body) else api.updateBill(body) }
        if (result is ApiResult.Ok) changed()
        return result
    }
}

/* ------------------------------ Advance ------------------------------ */

fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0

/** A bill as the advance rules see it: what it costs and the money received for it. */
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
