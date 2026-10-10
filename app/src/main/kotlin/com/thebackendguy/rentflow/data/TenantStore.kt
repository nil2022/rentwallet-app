package com.thebackendguy.rentflow.data

import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.BillDto
import com.thebackendguy.rentflow.data.remote.BillTotalsDto
import com.thebackendguy.rentflow.data.remote.ChangePasswordBody
import com.thebackendguy.rentflow.data.remote.Network
import com.thebackendguy.rentflow.data.remote.apiCall
import com.thebackendguy.rentflow.data.remote.discard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

/**
 * The signed-in tenant's bills, newest month first, with the API's [totals] of all
 * of them: [current] is this month's, [overdue] the earlier ones not fully paid.
 */
data class TenantBills(val bills: List<BillDto>, val totals: BillTotalsDto?) {
    val current: BillDto? = bills.find { it.monthYear == YearMonth.now().apiMonth() }
    val overdue: List<BillDto> = bills.filter { it.isOverdue && it.pendingAmount > 0 }
    val advance: Double = round2(bills.sumOf { it.advanceLeft })

    /** The room and property of the newest bill: where the tenant lives as far as the bills say. */
    val latest: BillDto? = bills.firstOrNull()
}

/** The tenant's own bills from the API (`/tenant/payment`); Home, Payments and Profile read [state]. */
object TenantStore {
    private const val PAGE_SIZE = 100 // the API's maximum

    private val api get() = Network.tenant
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(Loadable<TenantBills>())
    private var loadJob: Job? = null

    val state: StateFlow<Loadable<TenantBills>> = _state.asStateFlow()

    /** Reloads in the background; screens keep what they show meanwhile. */
    fun refresh() {
        if (loadJob?.isActive == true) return
        loadJob = scope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = apiCall { api.bills(limit = PAGE_SIZE) }
            _state.update {
                when (result) {
                    is ApiResult.Ok -> Loadable(TenantBills(result.value.data.orEmpty().sortedByDescending { b -> b.monthYear }, result.value.summary))
                    is ApiResult.Fail -> it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    /** Forgets everything, for sign-out. */
    fun clear() {
        loadJob?.cancel()
        _state.value = Loadable()
    }

    /** One bill, fresh from the API. */
    suspend fun bill(id: String): ApiResult<BillDto> =
        when (val r = apiCall { api.bill(id) }) {
            is ApiResult.Fail -> r
            is ApiResult.Ok -> r.value.data?.let { ApiResult.Ok(it) } ?: ApiResult.Fail("The server sent an empty reply. Please try again.")
        }

    suspend fun changePassword(oldPassword: String, newPassword: String): ApiResult<Unit> =
        apiCall { api.changePassword(ChangePasswordBody(oldPassword, newPassword)) }.discard()
}
