package com.thebackendguy.rentflow.ui.screens.landlord

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.thebackendguy.rentflow.R
import com.thebackendguy.rentflow.data.apiDate
import com.thebackendguy.rentflow.data.categoryLabel
import com.thebackendguy.rentflow.data.floorLabel
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.methodLabel
import com.thebackendguy.rentflow.data.mobileLabel
import com.thebackendguy.rentflow.data.monthLabel
import com.thebackendguy.rentflow.data.parseApiMonth
import com.thebackendguy.rentflow.data.remote.BillDto
import com.thebackendguy.rentflow.data.session.SessionUser
import com.thebackendguy.rentflow.ui.components.ToastAction
import com.thebackendguy.rentflow.ui.components.Toasts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

/*
 * A bill as a picture of one A4 page (approved design: bill-download mockup): who it
 * is for, the breakdown, every payment and expense taken off, and what is still due.
 * It is drawn on a Canvas so it looks the same on every phone, and saved as a PNG in
 * Downloads/RentFlow.
 */

/** Saves a bill as an image when [download] is called; [busy] while it is being made. */
internal class BillDownloader(private val context: Context, private val scope: CoroutineScope) {
    var busy by mutableStateOf(false)
        private set

    /** [issuedBy] is the landlord downloading their own bill; a tenant's copy has none. */
    fun download(bill: BillDto, issuedBy: SessionUser?) {
        if (busy) return
        busy = true
        scope.launch {
            val name = fileName(bill)
            val saved = withContext(Dispatchers.Default) {
                runCatching {
                    val image = BillPage(context, bill, issuedBy).render()
                    try {
                        withContext(Dispatchers.IO) { save(image, name) }
                    } finally {
                        image.recycle()
                    }
                }.getOrNull()
            }
            busy = false
            when {
                saved == null -> Toasts.error("The bill couldn’t be saved. Please try again.")
                // Android 9 can't save to Downloads without a storage permission: share it instead
                saved.authority == "${context.packageName}.fileprovider" -> share(saved)
                else -> Toasts.show(
                    "Bill saved as image", detail = "Downloads/RentFlow",
                    actions = listOf(ToastAction("Open") { open(saved) }, ToastAction("Share") { share(saved) })
                )
            }
        }
    }

    private fun save(image: Bitmap, name: String): Uri {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, name)
            file.outputStream().use { check(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, PNG)
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/RentFlow")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        try {
            resolver.openOutputStream(uri).use { out -> check(out != null && image.compress(Bitmap.CompressFormat.PNG, 100, out)) }
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        return uri
    }

    private fun open(uri: Uri) {
        val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, PNG).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(view)
        } catch (_: ActivityNotFoundException) {
            Toasts.error("No app on this phone can open the image.")
        }
    }

    private fun share(uri: Uri) {
        val send = Intent(Intent.ACTION_SEND).setType(PNG).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(Intent.createChooser(send, "Share the bill"))
        } catch (_: ActivityNotFoundException) {
            Toasts.error("No app on this phone can take the image.")
        }
    }

    private companion object {
        const val PNG = "image/png"

        /** "RentFlow-Bill-Oct-2026-Rahul-Das.png". */
        fun fileName(bill: BillDto): String {
            val parts = listOf("RentFlow", "Bill", monthLabel(bill.monthYear), bill.tenant?.fullName.orEmpty())
            return parts.joinToString("-") { it.trim().replace(Regex("[^A-Za-z0-9]+"), "-").trim('-') }
                .replace(Regex("-+"), "-").trim('-') + ".png"
        }
    }
}

@Composable
internal fun rememberBillDownloader(): BillDownloader {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) { BillDownloader(context, scope) }
}

/** "RF-2610-4F2A9C": the bill's year and month, then the end of its id. */
internal fun billNumber(bill: BillDto): String =
    "RF-" + bill.monthYear.filter(Char::isDigit).drop(2) + "-" + bill.id.takeLast(6).uppercase()

/* ------------------------------ The page ------------------------------ */

/** Rupees with paise and Indian grouping, as on a printed bill: ₹1,45,000.00. */
private fun rupees(amount: Double): String {
    val paise = abs(amount * 100).roundToLong()
    return (if (amount < 0) "− " else "") + inr((paise / 100).toInt()) + "." + (paise % 100).toString().padStart(2, '0')
}

private val DayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val MonthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val StampFormat = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)

/**
 * One A4 page at 150 dpi (1240 × 1754 px); a bill with many entries grows taller.
 * Sizes follow the mockup's 595 px page, about twice as large.
 */
private class BillPage(context: Context, private val bill: BillDto, private val issuedBy: SessionUser?) {
    private val font: Typeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans) ?: Typeface.DEFAULT

    private fun paint(size: Float, weight: Int, color: Int, spacing: Float = 0f) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = font
        // The font is variable: its weight is an axis
        setFontVariationSettings("'wght' $weight")
        textSize = size
        this.color = color
        letterSpacing = spacing
    }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun layout(text: CharSequence, paint: TextPaint, width: Float, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, max(width.toInt(), 1))
            .setAlignment(align).setIncludePad(false).setLineSpacing(0f, 1.2f).build()

    private fun Canvas.draw(layout: StaticLayout, x: Float, y: Float) {
        save()
        translate(x, y)
        layout.draw(this)
        restore()
    }

    private fun Canvas.rect(left: Float, top: Float, right: Float, bottom: Float, color: Int, radius: Float = 0f) {
        fill.color = color
        fill.style = Paint.Style.FILL
        drawRoundRect(RectF(left, top, right, bottom), radius, radius, fill)
    }

    /** The finished page. */
    fun render(): Bitmap {
        val picture = Picture()
        val bottom = content(picture.beginRecording(WIDTH, MAX_HEIGHT))
        picture.endRecording()
        val footer = layout(
            "Made with RentFlow on ${LocalDateTime.now().format(StampFormat).replace("AM", "am").replace("PM", "pm")}. Payments are recorded by the landlord.",
            paint(20f, 400, MUTED), CONTENT
        )
        val height = max(A4_HEIGHT, (bottom + 60 + 26 + footer.height + MARGIN_BOTTOM).toInt())
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(PAPER)
            drawPicture(picture)
            // The footer sits at the foot of the page
            val top = height - MARGIN_BOTTOM - footer.height - 26f
            rect(MARGIN, top, WIDTH - MARGIN, top + 2, LINE)
            draw(footer, MARGIN, top + 26)
        }
        return bitmap
    }

    /** Everything above the footer; returns where it ends. */
    private fun content(canvas: Canvas): Float {
        var y = MARGIN_TOP
        y = header(canvas, y) + 34
        y = parties(canvas, y) + 40
        y = section(canvas, "Breakdown", y)
        y = table(canvas, y, listOf("Item", "Details", "Amount"), 0.30f, breakdownRows()) + 40
        val entries = entryRows()
        if (entries.isNotEmpty()) {
            y = section(canvas, "Payments & expenses taken off", y)
            y = table(canvas, y, listOf("Date", "Entry", "Amount"), 0.22f, entries) + 40
        }
        y = summary(canvas, y) + 36
        bill.notes?.trim()?.takeIf(String::isNotEmpty)?.let { y = note(canvas, it, y) }
        return y
    }

    private fun header(canvas: Canvas, top: Float): Float {
        // The logo: an indigo tile with R, then the name
        canvas.rect(MARGIN, top, MARGIN + 50, top + 50, LOGO, 14f)
        val r = layout("R", paint(26f, 800, PAPER), 50f, Layout.Alignment.ALIGN_CENTER)
        canvas.draw(r, MARGIN, top + (50 - r.height) / 2f)
        val brand = layout("RentFlow", paint(27f, 800, INDIGO), 300f)
        canvas.draw(brand, MARGIN + 66, top + (50 - brand.height) / 2f)
        val title = layout("Rent bill", paint(50f, 800, INK, -0.02f), 500f)
        canvas.draw(title, MARGIN, top + 72)
        val month = layout(parseApiMonth(bill.monthYear)?.format(MonthFormat) ?: bill.monthYear, paint(27f, 400, BODY), 500f)
        canvas.draw(month, MARGIN, top + 72 + title.height + 4)
        val left = top + 72 + title.height + 4 + month.height

        // Bill number, the day it was made and the status stamp, on the right
        var y = top
        for ((label, value) in listOf("Bill no. " to billNumber(bill), "Issued " to LocalDateTime.now().format(DayFormat))) {
            val valuePaint = paint(22f, 700, INK)
            val labelPaint = paint(22f, 400, BODY)
            val right = WIDTH - MARGIN
            val valueWidth = valuePaint.measureText(value)
            canvas.drawText(value, right - valueWidth, y + 22f, valuePaint)
            canvas.drawText(label, right - valueWidth - labelPaint.measureText(label), y + 22f, labelPaint)
            y += 38
        }
        val (stamp, ink, wash) = when (bill.paymentStatus) {
            "paid" -> Triple("PAID", GREEN, GREEN_WASH)
            "partial" -> Triple("PARTLY PAID", AMBER, AMBER_WASH)
            else -> Triple("DUE", AMBER, AMBER_WASH)
        }
        val stampPaint = paint(22f, 800, ink, 0.12f)
        val w = stampPaint.measureText(stamp) + 40
        val box = RectF(WIDTH - MARGIN - w, y + 8, WIDTH - MARGIN, y + 56)
        canvas.rect(box.left, box.top, box.right, box.bottom, wash, 12f)
        fill.color = ink
        fill.style = Paint.Style.STROKE
        fill.strokeWidth = 3f
        canvas.drawRoundRect(box, 12f, 12f, fill)
        canvas.drawText(stamp, box.left + 20, box.centerY() - (stampPaint.descent() + stampPaint.ascent()) / 2, stampPaint)

        val bottom = max(left, box.bottom) + 30
        canvas.rect(MARGIN, bottom, WIDTH - MARGIN, bottom + 4, INDIGO)
        return bottom + 4
    }

    private fun parties(canvas: Canvas, top: Float): Float {
        val column = (CONTENT - 34) / 2
        val tenant = bill.tenant
        val room = bill.room
        val property = bill.property
        val leftLines = listOfNotNull(mobileLabel(tenant?.mobile)?.takeIf(String::isNotBlank), tenant?.email?.takeIf(String::isNotBlank))
        val rightLines = listOfNotNull(
            listOfNotNull(property?.address, property?.city).filter(String::isNotBlank).joinToString(", ").takeIf(String::isNotEmpty),
            listOfNotNull(room?.roomNumber?.let { "Room $it" }, room?.let { floorLabel(it.floor) }).joinToString(" • ").takeIf(String::isNotEmpty),
            issuedBy?.let { listOfNotNull("Issued by ${it.name}", it.mobileLabel).joinToString(" • ") }
        )
        val leftEnd = party(canvas, "Billed to", tenant?.fullName ?: "Tenant", leftLines, MARGIN, top, column)
        val rightEnd = party(canvas, "Property", property?.propertyName ?: "Property", rightLines, MARGIN + column + 34, top, column)
        return max(leftEnd, rightEnd)
    }

    private fun party(canvas: Canvas, label: String, name: String, lines: List<String>, x: Float, top: Float, width: Float): Float {
        var y = top
        val caps = layout(label.uppercase(), paint(19f, 800, MUTED, 0.12f), width)
        canvas.draw(caps, x, y)
        y += caps.height + 8
        val title = layout(name, paint(28f, 700, INK), width)
        canvas.draw(title, x, y)
        y += title.height + 6
        lines.forEach { line ->
            val text = layout(line, paint(23f, 400, BODY), width)
            canvas.draw(text, x, y)
            y += text.height + 4
        }
        return y
    }

    private fun section(canvas: Canvas, title: String, top: Float): Float {
        val caps = layout(title.uppercase(), paint(19f, 800, MUTED, 0.12f), CONTENT)
        canvas.draw(caps, MARGIN, top)
        return top + caps.height + 12
    }

    /** One row of a table: the first two columns (the second with a smaller line), the amount, and their colours. */
    private class TableRow(
        val first: String,
        val second: String,
        val amount: String,
        val small: String? = null,
        val color: Int = INK,
        val firstColor: Int = color,
        val amountColor: Int = color,
        val total: Boolean = false
    )

    private fun table(canvas: Canvas, top: Float, heads: List<String>, split: Float, rows: List<TableRow>): Float {
        val left = MARGIN
        val right = WIDTH - MARGIN
        val secondX = left + CONTENT * split
        val amountWidth = 230f
        // The header band
        val headPaint = paint(19f, 800, BODY, 0.08f)
        canvas.rect(left, top, right, top + 42, HEAD)
        val baseline = top + 21 - (headPaint.descent() + headPaint.ascent()) / 2
        canvas.drawText(heads[0].uppercase(), left + 20, baseline, headPaint)
        canvas.drawText(heads[1].uppercase(), secondX, baseline, headPaint)
        val amountHead = heads[2].uppercase()
        canvas.drawText(amountHead, right - 20 - headPaint.measureText(amountHead), baseline, headPaint)

        var y = top + 42
        rows.forEach { row ->
            if (row.total) canvas.rect(left, y, right, y + 3, INK)
            val weight = if (row.total) 800 else 400
            val first = layout(row.first, paint(24f, weight, row.firstColor), secondX - left - 40)
            val second = layout(row.second, paint(24f, 400, row.color), right - 20 - amountWidth - secondX)
            val small = row.small?.let { layout(it, paint(21f, 400, MUTED), right - 20 - amountWidth - secondX) }
            val amount = layout(row.amount, paint(24f, weight, row.amountColor), amountWidth, Layout.Alignment.ALIGN_OPPOSITE)
            val pad = 16f
            canvas.draw(first, left + 20, y + pad)
            canvas.draw(second, secondX, y + pad)
            small?.let { canvas.draw(it, secondX, y + pad + second.height + 2) }
            canvas.draw(amount, right - 20 - amountWidth, y + pad)
            val height = max(max(first.height.toFloat(), second.height + (small?.let { it.height + 2f } ?: 0f)), amount.height.toFloat()) + pad * 2
            y += height
            if (!row.total) canvas.rect(left, y - 2, right, y, LINE)
        }
        return y
    }

    private fun breakdownRows(): List<TableRow> = buildList {
        add(TableRow("Rent", parseApiMonth(bill.monthYear)?.format(MonthFormat).orEmpty(), rupees(bill.rent)))
        bill.electricity?.let { meter ->
            add(
                TableRow(
                    "Electricity", "${plainNumber(meter.previousReading)} → ${plainNumber(meter.currentReading)} kWh", rupees(meter.amount),
                    small = "${plainNumber(meter.unitsConsumed)} units × ${rupees(meter.ratePerUnit)}"
                )
            )
        }
        bill.otherCharges.forEach { add(TableRow(it.name, "", rupees(it.amount))) }
        add(TableRow("Total due", "", rupees(bill.totalDue), total = true))
        if (bill.creditApplied > 0) {
            val from = bill.creditSources.map { monthLabel(it.monthYear) }.distinct()
            val details = if (from.isEmpty()) "From earlier bills" else "From the ${from.joinToString(", ")} bill${if (from.size > 1) "s" else ""}"
            add(TableRow("Advance used", details, rupees(-bill.creditApplied), color = INDIGO))
            add(TableRow("To pay", "", rupees(max(bill.totalDue - bill.creditApplied, 0.0)), total = true))
        }
    }

    /** Payments and expenses, oldest first, as they happened. */
    private fun entryRows(): List<TableRow> =
        bill.receipts.sortedWith(compareBy({ apiDate(it.date) }, { it.createdAt.orEmpty() })).map { receipt ->
            val day = apiDate(receipt.date)?.format(DayFormat) ?: "—"
            val note = receipt.notes?.trim()?.takeIf(String::isNotEmpty)
            if (receipt.isDeduction) {
                // The date stays in ink; the expense's name and amount are amber
                TableRow(
                    day, entryTitle(bill, receipt), rupees(-receipt.amount),
                    small = listOfNotNull("${categoryLabel(receipt.category)} • paid by the tenant, taken off the rent", note).joinToString("\n"),
                    color = AMBER, firstColor = INK
                )
            } else {
                TableRow(day, "Payment ${entryNumber(bill, receipt)} • ${methodLabel(receipt.method)}", rupees(receipt.amount), small = note, amountColor = GREEN)
            }
        }

    private fun summary(canvas: Canvas, top: Float): Float {
        val width = 520f
        val left = WIDTH - MARGIN - width
        val toPay = max(bill.totalDue - bill.creditApplied, 0.0)
        val lines = buildList {
            add((if (bill.creditApplied > 0) "To pay" else "Total due") to rupees(toPay))
            add("Received" to rupees(bill.paidAmount))
            if (bill.adjustedAmount > 0) add("Expenses taken off" to rupees(bill.adjustedAmount))
        }
        val (lastLabel, lastValue, lastColor) = when {
            bill.pendingAmount > 0 -> Triple("Still due", rupees(bill.pendingAmount), RED)
            bill.advanceAmount > 0 -> Triple("Advance carried forward", rupees(bill.advanceAmount), INDIGO)
            else -> Triple("Fully paid", rupees(0.0), GREEN)
        }
        val rowPaint = paint(24f, 400, INK)
        val lastPaint = paint(29f, 800, lastColor)
        val height = 24 + lines.size * 40f + 22 + 50 + 20
        canvas.rect(left, top, left + width, top + height, BOX, 20f)
        fill.color = LINE
        fill.style = Paint.Style.STROKE
        fill.strokeWidth = 2f
        canvas.drawRoundRect(RectF(left, top, left + width, top + height), 20f, 20f, fill)
        var y = top + 24
        lines.forEach { (label, value) ->
            canvas.drawText(label, left + 24, y + 26, rowPaint)
            canvas.drawText(value, left + width - 24 - rowPaint.measureText(value), y + 26, rowPaint)
            y += 40
        }
        y += 10
        canvas.rect(left + 24, y, left + width - 24, y + 3, INK)
        y += 16
        canvas.drawText(lastLabel, left + 24, y + 32, lastPaint)
        canvas.drawText(lastValue, left + width - 24 - lastPaint.measureText(lastValue), y + 32, lastPaint)
        return top + height
    }

    private fun note(canvas: Canvas, text: String, top: Float): Float {
        val body = layout(
            SpannableStringBuilder().apply {
                append("Note: ", StyleSpan(Typeface.BOLD), 0)
                append(text)
            },
            paint(23f, 400, BODY), CONTENT - 24
        )
        canvas.rect(MARGIN, top, MARGIN + 6, top + body.height + 6, NOTE_BAR)
        canvas.draw(body, MARGIN + 24, top + 3)
        return top + body.height + 6
    }

    companion object {
        const val WIDTH = 1240
        const val A4_HEIGHT = 1754
        const val MAX_HEIGHT = 12000
        const val MARGIN = 84f
        const val MARGIN_TOP = 76f
        const val MARGIN_BOTTOM = 58f
        const val CONTENT = WIDTH - 2 * MARGIN

        // The page is paper in every theme, so its colours are fixed
        const val PAPER = 0xFFFFFFFF.toInt()
        const val INK = 0xFF0B1C30.toInt()
        const val BODY = 0xFF464555.toInt()
        const val MUTED = 0xFF777587.toInt()
        const val INDIGO = 0xFF3525CD.toInt()
        const val LOGO = 0xFF4F46E5.toInt()
        const val GREEN = 0xFF047857.toInt()
        const val GREEN_WASH = 0xFFD1FAE5.toInt()
        const val AMBER = 0xFFB45309.toInt()
        const val AMBER_WASH = 0xFFFEF3C7.toInt()
        const val RED = 0xFFBA1A1A.toInt()
        const val HEAD = 0xFFEFF4FF.toInt()
        const val LINE = 0xFFE5EEFF.toInt()
        const val BOX = 0xFFF8F9FF.toInt()
        const val NOTE_BAR = 0xFFC7C4D8.toInt()
    }
}
