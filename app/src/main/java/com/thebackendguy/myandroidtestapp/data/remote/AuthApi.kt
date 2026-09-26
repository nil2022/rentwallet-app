package com.thebackendguy.myandroidtestapp.data.remote

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Sign-in endpoints. Landlords and tenants have the same routes under their
 * own prefix, so [role] is "landlord" or "tenant".
 */
interface AuthApi {
    @POST("{role}/auth/login")
    suspend fun login(@Path("role") role: String, @Body body: LoginBody): ApiEnvelope<UserDto>

    /** Emails a 6-digit code. `isSignup=true` is the landlord sign-up check. */
    @POST("{role}/auth/generate-otp")
    suspend fun sendOtp(
        @Path("role") role: String,
        @Body body: EmailBody,
        @Query("isSignup") isSignup: Boolean?
    ): ApiEnvelope<JsonElement>

    /** Signs in with the code, or with `isSignup=true` only marks the email verified (no token). */
    @POST("{role}/auth/verify-otp")
    suspend fun verifyOtp(
        @Path("role") role: String,
        @Body body: OtpBody,
        @Query("isSignup") isSignup: Boolean?
    ): ApiEnvelope<UserDto>

    @POST("{role}/auth/request-password-reset")
    suspend fun requestPasswordReset(@Path("role") role: String, @Body body: EmailBody): ApiEnvelope<JsonElement>

    @POST("{role}/auth/verify-reset-password")
    suspend fun resetPassword(@Path("role") role: String, @Body body: ResetPasswordBody): ApiEnvelope<JsonElement>

    /** Takes the token itself, because the session is cleared before this call goes out. */
    @POST("{role}/auth/logout")
    suspend fun logout(@Path("role") role: String, @Header("Authorization") authorization: String): ApiEnvelope<JsonElement>

    /** Creates an unverified landlord account; the email is verified with a code next. */
    @POST("landlord")
    suspend fun registerLandlord(@Body body: RegisterBody): ApiEnvelope<UserDto>

    @GET("{role}/profile")
    suspend fun profile(@Path("role") role: String): ApiEnvelope<UserDto>
}
