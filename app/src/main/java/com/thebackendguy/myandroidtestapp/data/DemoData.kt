package com.thebackendguy.myandroidtestapp.data

enum class UserRole { Landlord, Tenant }

enum class PayStatus { Paid, Pending, Failed }

data class RentPayment(
    val month: String,
    val shortMonth: String,
    val amount: Int,
    val status: PayStatus,
    val dateLabel: String,
    val shortDate: String,
    val transactionId: String,
    val method: String,
    val receiptStatus: String
)

data class Tenant(
    val name: String,
    val initials: String,
    val property: String,
    val address: String,
    val area: String,
    val rent: Int,
    val deposit: Int,
    val status: PayStatus,
    val dateLabel: String,
    val shortDate: String,
    val phone: String,
    val leaseStart: String,
    val leaseEnd: String,
    val dueDay: String,
    val lastPayment: String
)

/** Sample data for the parts that don’t use the RentFlow API yet (rent, payments, wallet). */
object Demo {
    const val TENANT_NAME = "Rohan Mehta"
    const val LANDLORD_NAME = "Amit Sharma"
    const val PROPERTY = "Green View Residency"
    const val ADDRESS = "Flat 4B, Salt Lake, Kolkata"
    const val MONTHLY_RENT = 18_500
    const val DEPOSIT = 35_000
    const val WALLET_BALANCE = 74_000
    const val BANK_ACCOUNT = "HDFC Bank ••4821"

    val maySettled = RentPayment(
        month = "May 2026", shortMonth = "May", amount = MONTHLY_RENT, status = PayStatus.Paid,
        dateLabel = "Paid 06 May 2026", shortDate = "06 May", transactionId = "RW-MAY-2026-1042",
        method = "UPI / Bank payment", receiptStatus = "Generated"
    )

    val tenantPayments = listOf(
        RentPayment(
            month = "May 2026", shortMonth = "May", amount = MONTHLY_RENT, status = PayStatus.Pending,
            dateLabel = "Due 10 May 2026", shortDate = "10 May", transactionId = "Not generated",
            method = "Not selected", receiptStatus = "Awaiting payment"
        ),
        RentPayment(
            month = "April 2026", shortMonth = "Apr", amount = MONTHLY_RENT, status = PayStatus.Paid,
            dateLabel = "Paid 08 Apr 2026", shortDate = "08 Apr", transactionId = "RW-APR-2026-0914",
            method = "UPI / Bank payment", receiptStatus = "Generated"
        ),
        RentPayment(
            month = "March 2026", shortMonth = "Mar", amount = MONTHLY_RENT, status = PayStatus.Paid,
            dateLabel = "Paid 09 Mar 2026", shortDate = "09 Mar", transactionId = "RW-MAR-2026-0841",
            method = "UPI / Bank payment", receiptStatus = "Generated"
        ),
        RentPayment(
            month = "February 2026", shortMonth = "Feb", amount = MONTHLY_RENT, status = PayStatus.Failed,
            dateLabel = "Failed 10 Feb 2026", shortDate = "10 Feb", transactionId = "RW-FEB-2026-0773",
            method = "UPI / Bank payment", receiptStatus = "Not generated"
        )
    )

    val tenants = listOf(
        Tenant(
            name = "Rohan Mehta", initials = "RM", property = "Green View Residency",
            address = "Flat 4B, Salt Lake, Kolkata", area = "Flat 4B, Salt Lake", rent = 18_500, deposit = 35_000,
            status = PayStatus.Paid, dateLabel = "Paid 06 May", shortDate = "06 May", phone = "+91 98765 43210",
            leaseStart = "01 Apr 2025", leaseEnd = "31 Mar 2027", dueDay = "10th of every month",
            lastPayment = "₹18,500 on 06 May 2026"
        ),
        Tenant(
            name = "Priya Sen", initials = "PS", property = "Lakefront Homes",
            address = "Tower 2, New Town, Kolkata", area = "Tower 2, New Town", rent = 20_000, deposit = 40_000,
            status = PayStatus.Paid, dateLabel = "Paid 05 May", shortDate = "05 May", phone = "+91 91234 56780",
            leaseStart = "01 Jan 2026", leaseEnd = "31 Dec 2026", dueDay = "7th of every month",
            lastPayment = "₹20,000 on 05 May 2026"
        ),
        Tenant(
            name = "Arjun Das", initials = "AD", property = "Metro Heights",
            address = "Block C, Dum Dum, Kolkata", area = "Block C, Dum Dum", rent = 18_500, deposit = 36_000,
            status = PayStatus.Pending, dateLabel = "Due 10 May", shortDate = "10 May", phone = "+91 90000 11122",
            leaseStart = "15 Feb 2026", leaseEnd = "14 Feb 2027", dueDay = "10th of every month",
            lastPayment = "₹18,500 on 09 Apr 2026"
        ),
        Tenant(
            name = "Neha Roy", initials = "NR", property = "City Nest Apartment",
            address = "Flat 8A, Ballygunge, Kolkata", area = "Flat 8A, Ballygunge", rent = 18_500, deposit = 35_000,
            status = PayStatus.Pending, dateLabel = "Due 12 May", shortDate = "12 May", phone = "+91 98888 22233",
            leaseStart = "01 Mar 2026", leaseEnd = "28 Feb 2027", dueDay = "12th of every month",
            lastPayment = "₹18,500 on 12 Apr 2026"
        ),
        Tenant(
            name = "Kabir Khan", initials = "KK", property = "Sunrise Enclave",
            address = "House 12, Behala, Kolkata", area = "House 12, Behala", rent = 17_000, deposit = 34_000,
            status = PayStatus.Paid, dateLabel = "Paid 04 May", shortDate = "04 May", phone = "+91 97777 33344",
            leaseStart = "01 Oct 2025", leaseEnd = "30 Sep 2026", dueDay = "5th of every month",
            lastPayment = "₹17,000 on 04 May 2026"
        )
    )
}

/** The same payment after the tenant pays it in the app. */
fun RentPayment.settled(): RentPayment =
    if (this == Demo.tenantPayments.first()) Demo.maySettled
    else copy(
        status = PayStatus.Paid,
        dateLabel = "Paid 07 May 2026",
        shortDate = "07 May",
        method = "UPI / Bank payment",
        receiptStatus = "Generated"
    )

/** Rupees with Indian digit grouping, like the web's inr(): 145000 -> ₹1,45,000. */
fun inr(amount: Int): String {
    val digits = kotlin.math.abs(amount).toString()
    val grouped = if (digits.length <= 3) digits else {
        val head = digits.dropLast(3)
        head.reversed().chunked(2).joinToString(",").reversed() + "," + digits.takeLast(3)
    }
    return (if (amount < 0) "−₹" else "₹") + grouped
}
