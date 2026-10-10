package com.thebackendguy.rentflow.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/* Words the landlord screens show for API values, matching the web. */

/** API value and label, in the order the web lists them. */
val PropertyTypes = listOf("apartment" to "Apartment", "house" to "House", "villa" to "Villa", "hostel" to "Hostel", "pg" to "PG")
val OccupancyTypes = listOf(
    "single" to "Single", "double" to "Double", "triple" to "Triple", "shared" to "Shared", "other" to "Other"
)

fun propertyTypeLabel(type: String) = PropertyTypes.find { it.first == type }?.second ?: type.replaceFirstChar { it.uppercase() }
fun occupancyLabel(type: String?) = OccupancyTypes.find { it.first == type }?.second
    ?: type?.replaceFirstChar { it.uppercase() }.orEmpty()

/** How many tenants a room takes, as the API counts it; null for a shared room (no fixed limit). */
fun roomCapacity(type: String?): Int? = when (type) {
    "double" -> 2
    "triple" -> 3
    "shared" -> null
    else -> 1
}

/** Whether [tenants] fill a room of this type; a shared room never fills up. */
fun roomFull(type: String?, tenants: Int): Boolean = roomCapacity(type)?.let { tenants >= it } ?: false

/** "1 of 2 tenants", or "3 tenants" in a shared room. */
fun occupancyCount(type: String?, tenants: Int): String {
    val capacity = roomCapacity(type)
    val noun = if ((capacity ?: tenants) == 1) "tenant" else "tenants"
    return if (capacity == null) "$tenants $noun" else "$tenants of $capacity $noun"
}

/** "Ground Floor", "1st Floor", "12th Floor". */
fun floorLabel(floor: Int): String = if (floor == 0) "Ground Floor" else "${ordinal(floor)} Floor"

/** Ground floor to 10th, as on the web. */
val FloorOptions = (0..10).toList()

fun ordinal(n: Int): String {
    val suffix = if (n % 100 in 11..13) "th" else when (n % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    return "$n$suffix"
}

fun inr(amount: Double): String = inr(amount.roundToInt())

/** Whole rupees without the symbol, for editing ("18500"). */
fun plainAmount(amount: Double): String = if (amount == 0.0) "" else amount.roundToInt().toString()

private val ShortDate = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
private val MonthYear = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

/** The API's ISO timestamps as a local date. */
fun apiDate(value: String?): LocalDate? = value?.let {
    runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
        ?: runCatching { LocalDate.parse(it.take(10)) }.getOrNull()
}

fun LocalDate.shortLabel(): String = format(ShortDate)
fun LocalDate.monthLabel(): String = format(MonthYear)

/** "+91 98765 43210" for a 10-digit Indian mobile, otherwise as stored. */
fun mobileLabel(mobile: String?): String? =
    mobile?.filter(Char::isDigit)?.takeLast(10)?.takeIf { it.length == 10 }?.let { "+91 ${it.take(5)} ${it.drop(5)}" } ?: mobile

/** The last 10 digits, as the API stores a mobile. */
fun mobileDigits(mobile: String?): String = mobile?.filter(Char::isDigit)?.takeLast(10).orEmpty()

fun isValidMobile(digits: String) = digits.length == 10 && digits.first() in '6'..'9'
