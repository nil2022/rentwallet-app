package com.thebackendguy.rentflow.data.remote

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The signed-in tenant's own bills (recorded by their landlord) and password. The
 * bills have the landlord's shape; the reply's `summary` totals all of them.
 */
interface TenantApi {
    /** Newest month first. */
    @GET("tenant/payment")
    suspend fun bills(
        @Query("monthYear") monthYear: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): ApiEnvelope<List<BillDto>>

    @GET("tenant/payment/{id}")
    suspend fun bill(@Path("id") id: String): ApiEnvelope<BillDto>

    @PATCH("tenant/auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordBody): ApiEnvelope<JsonElement>
}
