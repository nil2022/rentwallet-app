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
val OccupancyTypes = listOf("single" to "Single", "double" to "Double", "triple" to "Triple", "other" to "Other")

fun propertyTypeLabel(type: String) = PropertyTypes.find { it.first == type }?.second ?: type.replaceFirstChar { it.uppercase() }
fun occupancyLabel(type: String?) = OccupancyTypes.find { it.first == type }?.second
    ?: type?.replaceFirstChar { it.uppercase() }.orEmpty()

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

fun dueDayLabel(day: Int) = "${ordinal(day)} of every month"

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

/** The date the API expects for a lease start. */
fun LocalDate.apiDay(): String = toString()

/** Last day of a lease that starts on [start] and runs [months] months, as the API works it out. */
fun leaseEnd(start: LocalDate, months: Int): LocalDate = start.plusMonths(months.toLong()).minusDays(1)

/** "+91 98765 43210" for a 10-digit Indian mobile, otherwise as stored. */
fun mobileLabel(mobile: String?): String? =
    mobile?.filter(Char::isDigit)?.takeLast(10)?.takeIf { it.length == 10 }?.let { "+91 ${it.take(5)} ${it.drop(5)}" } ?: mobile

/** The last 10 digits, as the API stores a mobile. */
fun mobileDigits(mobile: String?): String = mobile?.filter(Char::isDigit)?.takeLast(10).orEmpty()

fun isValidMobile(digits: String) = digits.length == 10 && digits.first() in '6'..'9'
