package com.thebackendguy.rentflow.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * A landlord's bills: one per tenant per month (rent, electricity, other charges)
 * and the money received for it. Totals, status and advance are worked out by the
 * API. A filter left null is not sent.
 */
interface PaymentApi {
    /** Newest month first. The reply's `summary` totals every bill that matches, not just this page. */
    @GET("landlord/payment")
    suspend fun bills(
        @Query("monthYear") monthYear: String? = null,
        @Query("status") status: String? = null,
        @Query("propertyId") propertyId: String? = null,
        @Query("tenantId") tenantId: String? = null,
        @Query("roomId") roomId: String? = null,
        @Query("hasAdvance") hasAdvance: Boolean? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): ApiEnvelope<List<BillDto>>

    /**
     * Month-by-month totals from [from] to [to] ("YYYY-MM", both included, at most 36
     * months); months without bills come back with zeros, oldest first.
     */
    @GET("landlord/payment/summary")
    suspend fun summary(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("propertyId") propertyId: String? = null
    ): ApiEnvelope<BillSummaryDto>

    @GET("landlord/payment/{id}")
    suspend fun bill(@Path("id") id: String): ApiEnvelope<BillDto>

    /*
     * Saving takes JSON built by hand: an edit must be able to send null (no electricity,
     * remove a photo), which the encoder would otherwise leave out.
     */

    /** A new bill: tenantId, monthYear, rent, electricity, otherCharges, and any money received. */
    @POST("landlord/payment")
    suspend fun createBill(@Body body: JsonObject): ApiEnvelope<BillDto>

    /** Changes a bill; `paymentId` goes in the body. The tenant's advance is worked out again. */
    @PATCH("landlord/payment")
    suspend fun updateBill(@Body body: JsonObject): ApiEnvelope<BillDto>

    /** Adds, replaces or (null) removes the meter photo: `{ paymentId, meterPhoto }`. */
    @PUT("landlord/payment/meter-photo")
    suspend fun setMeterPhoto(@Body body: JsonObject): ApiEnvelope<BillDto>

    /** Deletes a bill; advance it gave to later bills is taken back from them. */
    @DELETE("landlord/payment/{id}")
    suspend fun deleteBill(@Path("id") id: String): ApiEnvelope<JsonElement>
}

/** The tenant a bill belongs to, as the API embeds it. */
@Serializable
data class BillTenantDto(
    @SerialName("_id") val id: String = "",
    val fullName: String = "",
    val email: String? = null,
    val mobile: String? = null
)

/** The meter part of a bill; [meterPhoto] is a link to the photo, or null. */
@Serializable
data class ElectricityDto(
    @Serializable(with = LenientDouble::class) val previousReading: Double = 0.0,
    @Serializable(with = LenientDouble::class) val currentReading: Double = 0.0,
    @Serializable(with = LenientDouble::class) val unitsConsumed: Double = 0.0,
    @Serializable(with = LenientDouble::class) val ratePerUnit: Double = 0.0,
    @Serializable(with = LenientDouble::class) val amount: Double = 0.0,
    val meterPhoto: String? = null
)

@Serializable
data class OtherChargeDto(
    val name: String = "",
    @Serializable(with = LenientDouble::class) val amount: Double = 0.0,
    val note: String? = null
)

/** Advance from an earlier, overpaid bill that this bill used. */
@Serializable
data class CreditSourceDto(
    val paymentId: String = "",
    val monthYear: String = "",
    @Serializable(with = LenientDouble::class) val amount: Double = 0.0
)

/**
 * One month's bill. [creditApplied] is advance from earlier bills used on it,
 * [advanceAmount] what was paid beyond it, [advanceLeft] the part of that no later
 * bill has used yet. [paymentStatus] is pending, partial or paid.
 */
@Serializable
data class BillDto(
    @SerialName("_id") val id: String = "",
    val tenantId: String = "",
    val roomId: String = "",
    val propertyId: String = "",
    val tenant: BillTenantDto? = null,
    val room: TenantRoomDto? = null,
    val property: TenantPropertyDto? = null,
    val monthYear: String = "",
    @Serializable(with = LenientDouble::class) val rent: Double = 0.0,
    val electricity: ElectricityDto? = null,
    val otherCharges: List<OtherChargeDto> = emptyList(),
    @Serializable(with = LenientDouble::class) val totalDue: Double = 0.0,
    @Serializable(with = LenientDouble::class) val paidAmount: Double = 0.0,
    @Serializable(with = LenientDouble::class) val creditApplied: Double = 0.0,
    val creditSources: List<CreditSourceDto> = emptyList(),
    @Serializable(with = LenientDouble::class) val pendingAmount: Double = 0.0,
    @Serializable(with = LenientDouble::class) val advanceAmount: Double = 0.0,
    @Serializable(with = LenientDouble::class) val advanceLeft: Double = 0.0,
    val paymentStatus: String = "pending",
    val paymentMethod: String? = null,
    val paymentDate: String? = null,
    val paymentProof: String? = null,
    val notes: String? = null
)

/** One month of Rent History (or the whole range, with an empty [monthYear]). */
@Serializable
data class MonthTotalsDto(
    val monthYear: String = "",
    @Serializable(with = LenientInt::class) val bills: Int = 0,
    @Serializable(with = LenientDouble::class) val totalDue: Double = 0.0,
    @Serializable(with = LenientDouble::class) val totalPaid: Double = 0.0,
    @Serializable(with = LenientDouble::class) val totalPending: Double = 0.0,
    @Serializable(with = LenientDouble::class) val totalSettled: Double = 0.0,
    @Serializable(with = LenientDouble::class) val totalCreditApplied: Double = 0.0,
    @Serializable(with = LenientDouble::class) val totalAdvanceHeld: Double = 0.0
)

@Serializable
data class BillSummaryDto(val months: List<MonthTotalsDto> = emptyList(), val total: MonthTotalsDto = MonthTotalsDto())

/**
 * Totals of every bill a list matched. [totalSettled] is the part of the bills
 * that is covered (money received plus advance used, never more than billed).
 */
@Serializable
data class BillTotalsDto(
    @Serializable(with = LenientDouble::class) val totalDue: Double = 0.0,
    @Serializable(with = LenientDouble::class) val totalPaid: Double = 0.0,
    @Serializable(with = LenientDouble::class) val totalPending: Double = 0.0,
    @Serializable(with = LenientDouble::class) val totalSettled: Double = 0.0,
    @Serializable(with = LenientDouble::class) val totalCreditApplied: Double = 0.0,
    @Serializable(with = LenientDouble::class) val totalAdvanceHeld: Double = 0.0
)
