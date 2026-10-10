package com.thebackendguy.rentflow.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Storage folders the API accepts. */
enum class UploadFolder(val apiName: String) {
    Avatar("avatars"),
    Property("property-images"),
    Room("room-images"),
    MeterReading("meter-readings"),
    PaymentProof("payment-proofs")
}

/**
 * Photo uploads, the web's way: ask the API for a one-time URL, put the file
 * there, then send only the returned key with the record.
 *
 * Phone photos are often larger than the 5 MB the web allows, so each one is
 * resized to at most [MAX_SIDE] px and saved as a JPEG first. A payment proof may
 * also be a PDF, which goes up as it is (up to 5 MB, as on the web).
 */
object Uploads {
    private const val MAX_SIDE = 1600
    private const val MAX_PDF_BYTES = 5 * 1024 * 1024
    private const val JPEG = "image/jpeg"
    const val PDF = "application/pdf"

    /** Whether [uri] is a PDF, from what the phone says the file is (or its name). */
    fun isPdf(context: Context, uri: Uri): Boolean =
        context.contentResolver.getType(uri) == PDF || uri.toString().endsWith(".pdf", ignoreCase = true)

    suspend fun upload(context: Context, uri: Uri, folder: UploadFolder): ApiResult<String> {
        if (folder == UploadFolder.PaymentProof && isPdf(context, uri)) return uploadPdf(context, uri, folder)
        val bytes = withContext(Dispatchers.Default) { runCatching { compress(context, uri) }.getOrNull() }
            ?: return ApiResult.Fail("Couldn’t read that photo. Try a different one.")
        return put(bytes, "photo-${System.currentTimeMillis()}.jpg", JPEG, folder, what = "Photo")
    }

    private suspend fun uploadPdf(context: Context, uri: Uri, folder: UploadFolder): ApiResult<String> {
        val bytes = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        } ?: return ApiResult.Fail("Couldn’t read that PDF. Try a different one.")
        if (bytes.size > MAX_PDF_BYTES) return ApiResult.Fail("The PDF must be smaller than 5 MB.")
        return put(bytes, "proof-${System.currentTimeMillis()}.pdf", PDF, folder, what = "File")
    }

    private suspend fun put(bytes: ByteArray, fileName: String, type: String, folder: UploadFolder, what: String): ApiResult<String> {
        val presigned = apiCall { Network.landlord.presign(PresignBody(fileName, type, folder.apiName)) }
        val target = when (presigned) {
            is ApiResult.Fail -> return presigned
            is ApiResult.Ok -> presigned.value.data ?: return ApiResult.Fail("Couldn’t start the upload. Please try again.")
        }

        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(target.uploadUrl).put(bytes.toRequestBody(type.toMediaType())).build()
            try {
                Network.storage.newCall(request).execute().use { response ->
                    if (response.isSuccessful) ApiResult.Ok(target.key)
                    else ApiResult.Fail("$what upload failed. Please try again.")
                }
            } catch (e: IOException) {
                ApiResult.Fail("Couldn’t reach file storage. Check your connection and try again.")
            }
        }
    }

    private fun compress(context: Context, uri: Uri): ByteArray {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        // ImageDecoder also turns the photo upright from its camera orientation
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            val scale = min(1f, MAX_SIDE / max(width, height).toFloat())
            decoder.setTargetSize((width * scale).roundToInt().coerceAtLeast(1), (height * scale).roundToInt().coerceAtLeast(1))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }
}
