package com.thebackendguy.rentflow.data.remote

import com.thebackendguy.rentflow.data.session.SessionStore
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object Network {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val token = SessionStore.token
                val request = if (token == null) chain.request()
                else chain.request().newBuilder().header("Authorization", "Bearer $token").build()
                val response = chain.proceed(request)
                // The server rejected the saved token, so the session is over
                if (response.code == 401 && token != null && token == SessionStore.token) SessionStore.expire()
                response
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(API_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val auth: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val landlord: LandlordApi by lazy { retrofit.create(LandlordApi::class.java) }

    /** For file storage: it rejects requests that carry our Authorization header. */
    val storage: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Fail(val message: String) : ApiResult<Nothing>
}

fun <T> ApiResult<T>.discard(): ApiResult<Unit> = when (this) {
    is ApiResult.Ok -> ApiResult.Ok(Unit)
    is ApiResult.Fail -> this
}

/** Runs a request and turns every failure into a message a person can act on. */
suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Ok(block())
} catch (e: HttpException) {
    ApiResult.Fail(e.serverMessage() ?: "Something went wrong (error ${e.code()}). Please try again.")
} catch (e: IOException) {
    ApiResult.Fail("Can’t reach the server. Check your internet connection and try again.")
} catch (e: SerializationException) {
    ApiResult.Fail("The server sent a reply the app can’t read. Please try again later.")
}

/** The backend puts the reason in `message`, even for errors. */
private fun HttpException.serverMessage(): String? = runCatching {
    val body = response()?.errorBody()?.string() ?: return null
    Network.json.parseToJsonElement(body).jsonObject["message"]?.jsonPrimitive?.contentOrNull
}.getOrNull()?.takeIf { it.isNotBlank() }
