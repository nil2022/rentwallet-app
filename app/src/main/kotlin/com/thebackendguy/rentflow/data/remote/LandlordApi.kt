package com.thebackendguy.rentflow.data.remote

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** The landlord endpoints the web app uses: properties, rooms, tenants and their rooms, and the profile. */
interface LandlordApi {
    @GET("landlord/property")
    suspend fun properties(@Query("page") page: Int, @Query("limit") limit: Int): ApiEnvelope<List<PropertyDto>>

    @POST("landlord/property")
    suspend fun createProperty(@Body body: PropertyBody): ApiEnvelope<PropertyDto>

    /** The id travels in the body as `propertyId`. */
    @PATCH("landlord/property")
    suspend fun updateProperty(@Body body: PropertyBody): ApiEnvelope<PropertyDto>

    @DELETE("landlord/property/{id}")
    suspend fun deleteProperty(@Path("id") id: String): ApiEnvelope<JsonElement>

    @GET("landlord/room")
    suspend fun rooms(
        @Query("propertyId") propertyId: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): ApiEnvelope<List<RoomDto>>

    @POST("landlord/room")
    suspend fun createRoom(@Body body: RoomBody): ApiEnvelope<RoomDto>

    /** The id travels in the body as `roomId`. */
    @PATCH("landlord/room")
    suspend fun updateRoom(@Body body: RoomBody): ApiEnvelope<RoomDto>

    @DELETE("landlord/room/{id}")
    suspend fun deleteRoom(@Path("id") id: String): ApiEnvelope<JsonElement>

    /** Each tenant comes with its room and property, or null when they have none. */
    @GET("landlord/tenant")
    suspend fun tenants(@Query("page") page: Int, @Query("limit") limit: Int): ApiEnvelope<List<TenantDto>>

    @POST("landlord/tenant")
    suspend fun createTenant(@Body body: NewTenantBody): ApiEnvelope<TenantDto>

    /** The id travels in the body as `tenantId`. */
    @PATCH("landlord/tenant")
    suspend fun updateTenant(@Body body: TenantUpdateBody): ApiEnvelope<TenantDto>

    @DELETE("landlord/tenant/{id}")
    suspend fun deleteTenant(@Path("id") id: String): ApiEnvelope<JsonElement>

    /**
     * Puts a tenant in a room, moves them, or (roomId null) takes them out of it.
     * The body is `{ tenantId, roomId }`; roomId must be sent even when null, so it's
     * built as JSON rather than a class (the encoder leaves nulls out). A full room is a 409.
     */
    @POST("landlord/tenant/assign-room")
    suspend fun assignRoom(@Body body: JsonObject): ApiEnvelope<TenantDto>

    /** The account's id travels in the body as `userId`. */
    @PATCH("landlord")
    suspend fun updateProfile(@Body body: ProfileBody): ApiEnvelope<UserDto>

    @PATCH("landlord/auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordBody): ApiEnvelope<JsonElement>

    /** A one-time URL to put a photo straight into storage. */
    @POST("upload/presigned")
    suspend fun presign(@Body body: PresignBody): ApiEnvelope<PresignDto>
}
