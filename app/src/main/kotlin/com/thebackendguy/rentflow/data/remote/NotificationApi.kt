package com.thebackendguy.rentflow.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** The signed-in user's notifications. The same routes serve landlords and tenants. */
interface NotificationApi {
    /** Newest first; `count` is the total. The server marks every notification read when this is called. */
    @GET("notification")
    suspend fun list(@Query("page") page: Int, @Query("limit") limit: Int): ApiEnvelope<List<NotificationDto>>

    @GET("notification/count")
    suspend fun unreadCount(): ApiEnvelope<CountDto>
}

/** [type] is payment_reminder, payment_confirmation, property_update, booking, message or system. */
@Serializable
data class NotificationDto(
    @SerialName("_id") val id: String = "",
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val isRead: Boolean = true,
    val createdAt: String? = null
)

@Serializable
data class CountDto(val count: Int = 0)
