package com.thebackendguy.rentflow.data.remote

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** The landlord endpoints the web app uses: properties, rooms, tenants, leases and the profile. */
interface LandlordApi {
    @GET("landlord/property")
    suspend fun properties(@Query("page") page: Int, @Query("limit") limit: Int): ApiEnvelope<List<PropertyDto>>

    @POST("landlord/property")
    suspend fun createProperty(@Body body: PropertyBody): ApiEnvelope<PropertyDto>

    @PATCH("landlord/property/{id}")
    suspend fun updateProperty(@Path("id") id: String, @Body body: PropertyBody): ApiEnvelope<PropertyDto>

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

    @PATCH("landlord/room/{id}")
    suspend fun updateRoom(@Path("id") id: String, @Body body: RoomBody): ApiEnvelope<RoomDto>

    @DELETE("landlord/room/{id}")
    suspend fun deleteRoom(@Path("id") id: String): ApiEnvelope<JsonElement>

    /** Each tenant comes with its active lease (room and property included), or null. */
    @GET("landlord/tenant")
    suspend fun tenants(@Query("page") page: Int, @Query("limit") limit: Int): ApiEnvelope<List<TenantDto>>

    @POST("landlord/tenant")
    suspend fun createTenant(@Body body: NewTenantBody): ApiEnvelope<TenantDto>

    @PATCH("landlord/tenant/{id}")
    suspend fun updateTenant(@Path("id") id: String, @Body body: TenantUpdateBody): ApiEnvelope<TenantDto>

    @DELETE("landlord/tenant/{id}")
    suspend fun deleteTenant(@Path("id") id: String): ApiEnvelope<JsonElement>

    /** Assigning a room: the API marks the room taken once it's full. */
    @POST("landlord/lease")
    suspend fun createLease(@Body body: LeaseBody): ApiEnvelope<LeaseDto>

    /** Ending a lease (status "ended") frees the room. */
    @PATCH("landlord/lease/{id}")
    suspend fun updateLeaseStatus(@Path("id") id: String, @Body body: LeaseStatusBody): ApiEnvelope<LeaseDto>

    @PATCH("landlord/{id}")
    suspend fun updateProfile(@Path("id") id: String, @Body body: ProfileBody): ApiEnvelope<UserDto>

    @PATCH("landlord/auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordBody): ApiEnvelope<JsonElement>

    /** A one-time URL to put a photo straight into storage. */
    @POST("upload/presigned")
    suspend fun presign(@Body body: PresignBody): ApiEnvelope<PresignDto>
}
