package com.thebackendguy.myandroidtestapp.data.remote

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

/** The room and property a lease points at, as the API embeds them. */
@Serializable
data class LeaseRoomDto(
    @SerialName("_id") val id: String = "",
    val roomNumber: String = "",
    @Serializable(with = LenientInt::class) val floor: Int = 0,
    val occupancyType: String? = null
)

@Serializable
data class LeasePropertyDto(
    @SerialName("_id") val id: String = "",
    val propertyName: String = "",
    val address: String? = null,
    val city: String? = null
)

@Serializable
data class LeaseDto(
    @SerialName("_id") val id: String = "",
    val propertyId: String = "",
    val roomId: String = "",
    val tenantId: String = "",
    @Serializable(with = LenientDouble::class) val rent: Double = 0.0,
    @Serializable(with = LenientDouble::class) val securityDeposit: Double = 0.0,
    @Serializable(with = LenientDouble::class) val maintenanceCharge: Double = 0.0,
    val startDate: String? = null,
    val endDate: String? = null,
    @Serializable(with = LenientInt::class) val tenureMonths: Int = 0,
    @Serializable(with = LenientInt::class) val rentDueDay: Int = 5,
    @Serializable(with = LenientDouble::class) val escalationPercent: Double = 0.0,
    @Serializable(with = LenientInt::class) val lockInMonths: Int = 0,
    @Serializable(with = LenientInt::class) val noticePeriodMonths: Int = 0,
    @Serializable(with = LenientDouble::class) val meterStartReading: Double? = null,
    @Serializable(with = LenientDouble::class) val meterRatePerUnit: Double = 0.0,
    val status: String = "active",
    val room: LeaseRoomDto? = null,
    val property: LeasePropertyDto? = null
)

/** A tenant account with its active lease, if any. */
@Serializable
data class TenantDto(
    @SerialName("_id") val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val mobile: String? = null,
    val profilePic: String? = null,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val lease: LeaseDto? = null
)

@Serializable
data class PresignDto(val key: String = "", val uploadUrl: String = "")

/* ------------------------------ Requests ------------------------------ */
// No default values here: the JSON encoder leaves defaults out, and an edit
// that sets a switch back to false must still send it.

@Serializable
data class PropertyBody(
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

@Serializable
data class NewTenantBody(
    val fullName: String,
    val email: String,
    val mobile: String,
    val password: String,
    val profilePic: String?
)

@Serializable
data class TenantUpdateBody(
    val fullName: String,
    val mobile: String,
    val isActive: Boolean,
    val profilePic: String?
)

@Serializable
data class LeaseBody(
    val roomId: String,
    val tenantId: String,
    val rent: Double,
    val securityDeposit: Double,
    val maintenanceCharge: Double,
    val startDate: String,
    val tenureMonths: Int,
    val rentDueDay: Int,
    val escalationPercent: Double,
    val lockInMonths: Int,
    val noticePeriodMonths: Int,
    val meterStartReading: Double?,
    val meterRatePerUnit: Double?
)

@Serializable
data class LeaseStatusBody(val status: String)

@Serializable
data class ProfileBody(val fullName: String, val mobile: String, val profilePic: String?)

@Serializable
data class ChangePasswordBody(val oldPassword: String, val newPassword: String)

@Serializable
data class PresignBody(val fileName: String, val contentType: String, val folderName: String)
