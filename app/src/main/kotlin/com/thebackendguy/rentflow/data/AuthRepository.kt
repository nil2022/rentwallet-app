package com.thebackendguy.rentflow.data

import com.thebackendguy.rentflow.data.remote.ApiEnvelope
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.EmailBody
import com.thebackendguy.rentflow.data.remote.LoginBody
import com.thebackendguy.rentflow.data.remote.Network
import com.thebackendguy.rentflow.data.remote.OtpBody
import com.thebackendguy.rentflow.data.remote.RegisterBody
import com.thebackendguy.rentflow.data.remote.ResetPasswordBody
import com.thebackendguy.rentflow.data.remote.UserDto
import com.thebackendguy.rentflow.data.remote.apiCall
import com.thebackendguy.rentflow.data.remote.discard
import com.thebackendguy.rentflow.data.session.SessionStore
import com.thebackendguy.rentflow.data.session.toSessionUser
import kotlinx.coroutines.withTimeoutOrNull

val UserRole.apiPath: String get() = name.lowercase()

/** Sign-in, sign-up and password reset against the RentFlow API (the same calls the web makes). */
object AuthRepository {
    private val api get() = Network.auth

    private fun String.normalized() = trim().lowercase()

    suspend fun login(role: UserRole, email: String, password: String, remember: Boolean): ApiResult<Unit> =
        signIn(role, remember) { api.login(role.apiPath, LoginBody(email.normalized(), password)) }

    suspend fun sendLoginCode(role: UserRole, email: String): ApiResult<Unit> =
        apiCall { api.sendOtp(role.apiPath, EmailBody(email.normalized()), null) }.discard()

    suspend fun verifyLoginCode(role: UserRole, email: String, code: String, remember: Boolean): ApiResult<Unit> =
        signIn(role, remember) { api.verifyOtp(role.apiPath, OtpBody(email.normalized(), code), null) }

    suspend fun requestPasswordReset(role: UserRole, email: String): ApiResult<Unit> =
        apiCall { api.requestPasswordReset(role.apiPath, EmailBody(email.normalized())) }.discard()

    suspend fun resetPassword(role: UserRole, email: String, code: String, newPassword: String): ApiResult<Unit> =
        apiCall { api.resetPassword(role.apiPath, ResetPasswordBody(email.normalized(), code, newPassword)) }.discard()

    suspend fun registerLandlord(name: String, email: String, mobile: String, password: String): ApiResult<Unit> =
        apiCall { api.registerLandlord(RegisterBody(name.trim(), email.normalized(), mobile, password)) }.discard()

    suspend fun sendSignupCode(email: String): ApiResult<Unit> =
        apiCall { api.sendOtp(UserRole.Landlord.apiPath, EmailBody(email.normalized()), true) }.discard()

    suspend fun verifySignupCode(email: String, code: String): ApiResult<Unit> =
        apiCall { api.verifyOtp(UserRole.Landlord.apiPath, OtpBody(email.normalized(), code), true) }.discard()

    /** Refreshes the saved name and contact details; a rejected token signs the user out. */
    suspend fun refreshProfile() {
        val user = SessionStore.session.value?.user ?: return
        val result = apiCall { api.profile(user.role.apiPath) }
        if (result is ApiResult.Ok) result.value.data?.let { SessionStore.updateUser(it.toSessionUser(user.role)) }
    }

    /** Forgets the session straight away, then tells the server. */
    suspend fun logout() {
        val session = SessionStore.session.value ?: return
        SessionStore.clear()
        withTimeoutOrNull(5_000) { apiCall { api.logout(session.user.role.apiPath, "Bearer ${session.token}") } }
    }

    private suspend fun signIn(role: UserRole, remember: Boolean, call: suspend () -> ApiEnvelope<UserDto>): ApiResult<Unit> =
        when (val result = apiCall(call)) {
            is ApiResult.Fail -> result
            is ApiResult.Ok -> {
                val token = result.value.token
                val user = result.value.data
                if (token == null || user == null) {
                    ApiResult.Fail("Sign-in didn’t complete. Please try again.")
                } else {
                    SessionStore.start(token, user.toSessionUser(role), remember)
                    ApiResult.Ok(Unit)
                }
            }
        }
}
