package com.thebackendguy.rentflow.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Every API reply: `{ status, message, data, count, token }`. */
@Serializable
data class ApiEnvelope<T>(
    val status: Boolean = false,
    val message: String? = null,
    val data: T? = null,
    val count: Int? = null,
    val token: String? = null
)

/** A landlord or tenant account, as login and profile return it. */
@Serializable
data class UserDto(
    @SerialName("_id") val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val mobile: String? = null,
    val role: String? = null,
    val profilePic: String? = null,
    val isActive: Boolean = true
)

@Serializable
data class LoginBody(val email: String, val password: String)

@Serializable
data class EmailBody(val email: String)

@Serializable
data class OtpBody(val email: String, val otp: String)

@Serializable
data class ResetPasswordBody(val email: String, val otp: String, val newPassword: String)

@Serializable
data class RegisterBody(val fullName: String, val email: String, val mobile: String, val password: String)
