package com.thebackendguy.rentflow.data

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
}
