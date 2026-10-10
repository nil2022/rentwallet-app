package com.thebackendguy.rentflow.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * The web downloads a CSV; on a phone it goes to Android's share menu instead, so it
 * can be saved to Files or Drive, or sent by WhatsApp or email.
 */
object CsvShare {
    /** Whole numbers without ".0", as the web writes them. */
    fun number(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    private fun cell(value: Any?): String {
        val text = when (value) {
            null -> ""
            is Double -> number(value)
            else -> value.toString()
        }
        return "\"" + text.replace("\"", "\"\"") + "\""
    }

    fun share(context: Context, fileName: String, header: List<String>, rows: List<List<Any?>>) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        val result = runCatching {
            file.writeText((listOf(header) + rows).joinToString("\n") { row -> row.joinToString(",") { cell(it) } })
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
        val uri = result.getOrNull()
        if (uri == null) {
            Toasts.error("Couldn’t make the CSV file. Please try again.")
            return
        }
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, fileName)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(Intent.createChooser(send, "Share $fileName"))
        } catch (_: ActivityNotFoundException) {
            Toasts.error("No app on this phone can take the CSV file.")
        }
    }
}
