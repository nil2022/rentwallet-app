package com.thebackendguy.rentflow.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* ------------------------------ Replies ------------------------------ */

/** A stored photo: its storage key (sent back on save) and a temporary URL to show it. */
@Serializable
data class ImageDto(val key: String = "", val url: String? = null)

@Serializable
data class PropertyDto(
    @SerialName("_id") val id: String = "",
    val propertyName: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "",
    @Serializable(with = LenientText::class) val pincode: String = "",
    val propertyType: String = "apartment",
    @Serializable(with = LenientInt::class) val floorCount: Int = 1,
    @Serializable(with = LenientInt::class) val roomCount: Int = 1,
    val hasParking: Boolean = false,
    val hasLift: Boolean = false,
    val description: String? = null,
    val images: List<ImageDto> = emptyList()
)

@Serializable
data class RoomDto(
    @SerialName("_id") val id: String = "",
    val propertyId: String = "",
    val roomNumber: String = "",
    @Serializable(with = LenientInt::class) val floor: Int = 0,
    val occupancyType: String = "single",
    @Serializable(with = LenientDouble::class) val rent: Double = 0.0,
    @Serializable(with = LenientDouble::class) val securityDeposit: Double = 0.0,
    val isFurnished: Boolean = false,
    val furnitureDetails: String? = null,
    val hasAttachedWashroom: Boolean = false,
    val hasBalcony: Boolean = false,
    val roomSize: String? = null,
    val isAvailable: Boolean = true,
    val amenities: String? = null,
    val images: List<ImageDto> = emptyList()
)

/** The room a tenant lives in, as the API embeds it. */
@Serializable
data class TenantRoomDto(
    @SerialName("_id") val id: String = "",
    val roomNumber: String = "",
    @Serializable(with = LenientInt::class) val floor: Int = 0,
    val occupancyType: String? = null,
    val roomSize: String? = null
)

/** The property of that room. */
@Serializable
data class TenantPropertyDto(
    @SerialName("_id") val id: String = "",
    val propertyName: String = "",
    val address: String? = null,
    val city: String? = null
)

/** A tenant account with the room they live in, or null ids when they have none. */
@Serializable
data class TenantDto(
    @SerialName("_id") val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val mobile: String? = null,
    val profilePic: String? = null,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val roomId: String? = null,
    val propertyId: String? = null,
    val room: TenantRoomDto? = null,
    val property: TenantPropertyDto? = null
)

@Serializable
data class PresignDto(val key: String = "", val uploadUrl: String = "")

/* ------------------------------ Requests ------------------------------ */
// No default values here: the JSON encoder leaves defaults out, and an edit
// that sets a switch back to false must still send it. The one exception is the
// record's id: an edit sends it in the body (never in the URL), a create leaves it null.

@Serializable
data class PropertyBody(
    val propertyId: String? = null,
    val propertyName: String,
    val address: String,
    val city: String,
    val state: String,
    val country: String,
    val pincode: String,
    val propertyType: String,
    val floorCount: Int,
    val roomCount: Int,
    val hasParking: Boolean,
    val hasLift: Boolean,
    val description: String,
    val images: List<String>
)

@Serializable
data class RoomBody(
    val roomId: String? = null,
    val propertyId: String?,
    val roomNumber: String,
    val floor: Int,
    val occupancyType: String,
    val rent: Double,
    val securityDeposit: Double,
    val isFurnished: Boolean,
    val furnitureDetails: String,
    val hasAttachedWashroom: Boolean,
    val hasBalcony: Boolean,
    val roomSize: String,
    val amenities: String,
    val images: List<String>
)

/** A tenant's email is optional: null leaves it out (the tenant signs in with their mobile). */
@Serializable
data class NewTenantBody(
    val fullName: String,
    val email: String?,
    val mobile: String,
    val password: String,
    val profilePic: String?
)

/** [email] null means no change: the API can change a saved email but not remove it. */
@Serializable
data class TenantUpdateBody(
    val tenantId: String,
    val fullName: String,
    val email: String?,
    val mobile: String,
    val isActive: Boolean,
    val profilePic: String?
)

@Serializable
data class ProfileBody(val userId: String, val fullName: String, val mobile: String, val profilePic: String?)

@Serializable
data class ChangePasswordBody(val oldPassword: String, val newPassword: String)

@Serializable
data class PresignBody(val fileName: String, val contentType: String, val folderName: String)
