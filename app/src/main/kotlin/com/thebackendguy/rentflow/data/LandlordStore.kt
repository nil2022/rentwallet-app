package com.thebackendguy.rentflow.data

import android.content.Context
import android.net.Uri
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.ChangePasswordBody
import com.thebackendguy.rentflow.data.remote.LeaseBody
import com.thebackendguy.rentflow.data.remote.LeaseStatusBody
import com.thebackendguy.rentflow.data.remote.Network
import com.thebackendguy.rentflow.data.remote.NewTenantBody
import com.thebackendguy.rentflow.data.remote.ProfileBody
import com.thebackendguy.rentflow.data.remote.PropertyBody
import com.thebackendguy.rentflow.data.remote.PropertyDto
import com.thebackendguy.rentflow.data.remote.RoomBody
import com.thebackendguy.rentflow.data.remote.RoomDto
import com.thebackendguy.rentflow.data.remote.TenantDto
import com.thebackendguy.rentflow.data.remote.TenantUpdateBody
import com.thebackendguy.rentflow.data.remote.UploadFolder
import com.thebackendguy.rentflow.data.remote.Uploads
import com.thebackendguy.rentflow.data.remote.apiCall
import com.thebackendguy.rentflow.data.remote.discard
import com.thebackendguy.rentflow.data.session.SessionStore
import com.thebackendguy.rentflow.data.session.toSessionUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Data a screen shows, with whether a reload is running and why the last one failed. */
data class Loadable<out T>(val data: T? = null, val loading: Boolean = false, val error: String? = null)

/** A photo in a form: already stored, or just picked from the phone. */
sealed interface Photo {
    data class Stored(val key: String, val url: String?) : Photo
    data class Picked(val uri: Uri) : Photo
}

/** Everything the landlord screens show, loaded together so they always agree. */
data class Portfolio(
    val properties: List<PropertyDto>,
    val rooms: Map<String, List<RoomDto>>,
    val tenants: List<TenantDto>
) {
    val allRooms: List<RoomDto> = rooms.values.flatten()
    val tenantsWithoutRoom: List<TenantDto> get() = tenants.filter { it.lease == null }

    fun property(id: String?) = properties.find { it.id == id }
    fun room(id: String?) = allRooms.find { it.id == id }
    fun tenant(id: String?) = tenants.find { it.id == id }
    fun roomsOf(propertyId: String) = rooms[propertyId].orEmpty().sortedWith(compareBy({ it.floor }, { it.roomNumber }))
    fun occupants(roomId: String) = tenants.filter { it.lease?.roomId == roomId }
}

/**
 * The landlord's properties, rooms and tenants from the API (the same calls the
 * web makes). Screens read [state]; every change reloads it so each screen
 * shows the result straight away.
 */
object LandlordStore {
    private const val PAGE_SIZE = 100 // the API's maximum

    private val api get() = Network.landlord
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(Loadable<Portfolio>())
    private var loadJob: Job? = null

    val state: StateFlow<Loadable<Portfolio>> = _state.asStateFlow()

    /** Reloads in the background; screens keep what they show meanwhile. */
    fun refresh() {
        if (loadJob?.isActive == true) return
        loadJob = scope.launch { load() }
    }

    /** Forgets everything, for sign-out. */
    fun clear() {
        loadJob?.cancel()
        _state.value = Loadable()
    }

    private suspend fun reload() {
        loadJob?.cancel()
        load()
    }

    private suspend fun load() {
        _state.update { it.copy(loading = true, error = null) }
        val result = fetch()
        _state.update {
            when (result) {
                is ApiResult.Ok -> Loadable(result.value)
                is ApiResult.Fail -> it.copy(loading = false, error = result.message)
            }
        }
    }

    private suspend fun fetch(): ApiResult<Portfolio> = coroutineScope {
        val propertiesCall = async { apiCall { api.properties(1, PAGE_SIZE) } }
        val tenantsCall = async { apiCall { api.tenants(1, PAGE_SIZE) } }
        val properties = when (val r = propertiesCall.await()) {
            is ApiResult.Fail -> return@coroutineScope r
            is ApiResult.Ok -> r.value.data.orEmpty()
        }
        val tenants = when (val r = tenantsCall.await()) {
            is ApiResult.Fail -> return@coroutineScope r
            is ApiResult.Ok -> r.value.data.orEmpty()
        }
        // The API lists rooms one property at a time
        val roomCalls = properties.map { p -> p.id to async { apiCall { api.rooms(p.id, 1, PAGE_SIZE) } } }
        val rooms = mutableMapOf<String, List<RoomDto>>()
        for ((id, call) in roomCalls) {
            when (val r = call.await()) {
                is ApiResult.Fail -> return@coroutineScope r
                is ApiResult.Ok -> rooms[id] = r.value.data.orEmpty()
            }
        }
        ApiResult.Ok(Portfolio(properties, rooms, tenants))
    }

    /** Runs a change, then reloads so every screen shows it. */
    private suspend fun <T> change(block: suspend () -> ApiResult<T>): ApiResult<T> {
        val result = block()
        if (result is ApiResult.Ok) reload()
        return result
    }

    /* ------------------------------ Properties ------------------------------ */

    /** Creates the property when [id] is null. Returns its id. */
    suspend fun saveProperty(context: Context, id: String?, body: PropertyBody, photos: List<Photo>): ApiResult<String> = change {
        when (val keys = photoKeys(context, photos, UploadFolder.Property)) {
            is ApiResult.Fail -> keys
            is ApiResult.Ok -> {
                val full = body.copy(images = keys.value)
                when (val saved = apiCall { if (id == null) api.createProperty(full) else api.updateProperty(id, full) }) {
                    is ApiResult.Fail -> saved
                    is ApiResult.Ok -> ApiResult.Ok(saved.value.data?.id ?: id.orEmpty())
                }
            }
        }
    }

    suspend fun deleteProperty(id: String): ApiResult<Unit> = change { apiCall { api.deleteProperty(id) }.discard() }

    /* ------------------------------ Rooms ------------------------------ */

    /** Creates the room when [id] is null. Returns its id. */
    suspend fun saveRoom(context: Context, id: String?, body: RoomBody, photos: List<Photo>): ApiResult<String> = change {
        when (val keys = photoKeys(context, photos, UploadFolder.Room)) {
            is ApiResult.Fail -> keys
            is ApiResult.Ok -> {
                // The API ignores the property on an edit, so it's only sent for a new room
                val full = body.copy(images = keys.value, propertyId = if (id == null) body.propertyId else null)
                when (val saved = apiCall { if (id == null) api.createRoom(full) else api.updateRoom(id, full) }) {
                    is ApiResult.Fail -> saved
                    is ApiResult.Ok -> ApiResult.Ok(saved.value.data?.id ?: id.orEmpty())
                }
            }
        }
    }

    suspend fun deleteRoom(id: String): ApiResult<Unit> = change { apiCall { api.deleteRoom(id) }.discard() }

    /* ------------------------------ Tenants and leases ------------------------------ */

    /** Creates the tenant account and returns its id. The list reloads with [assignRoom] or [finish]. */
    suspend fun addTenant(context: Context, body: NewTenantBody, photo: Uri?): ApiResult<String> {
        val key = if (photo == null) null else when (val r = Uploads.upload(context, photo, UploadFolder.Avatar)) {
            is ApiResult.Fail -> return r
            is ApiResult.Ok -> r.value
        }
        return when (val created = apiCall { api.createTenant(body.copy(profilePic = key)) }) {
            is ApiResult.Fail -> created
            is ApiResult.Ok -> created.value.data?.id?.let { ApiResult.Ok(it) }
                ?: ApiResult.Fail("The tenant was saved, but the reply was incomplete. Open Tenants to see them.")
        }
    }

    /**
     * [photo] is null to keep the current photo, [Photo.Picked] to replace it,
     * and [removePhoto] clears it.
     */
    suspend fun updateTenant(
        context: Context,
        id: String,
        name: String,
        mobile: String,
        active: Boolean,
        photo: Photo.Picked?,
        removePhoto: Boolean
    ): ApiResult<Unit> = change {
        val key = when {
            photo != null -> when (val r = Uploads.upload(context, photo.uri, UploadFolder.Avatar)) {
                is ApiResult.Fail -> return@change r
                is ApiResult.Ok -> r.value
            }
            removePhoto -> ""
            else -> null
        }
        apiCall { api.updateTenant(id, TenantUpdateBody(name, mobile, active, key)) }.discard()
    }

    suspend fun deleteTenant(id: String): ApiResult<Unit> = change { apiCall { api.deleteTenant(id) }.discard() }

    suspend fun assignRoom(body: LeaseBody): ApiResult<Unit> = change { apiCall { api.createLease(body) }.discard() }

    suspend fun endLease(leaseId: String): ApiResult<Unit> =
        change { apiCall { api.updateLeaseStatus(leaseId, LeaseStatusBody("ended")) }.discard() }

    /** Reloads after a flow that saved something but stopped part-way. */
    suspend fun finish() = reload()

    /* ------------------------------ Profile ------------------------------ */

    suspend fun updateProfile(context: Context, name: String, mobile: String, photo: Photo.Picked?): ApiResult<Unit> {
        val user = SessionStore.session.value?.user ?: return ApiResult.Fail("Please sign in again.")
        val key = if (photo == null) null else when (val r = Uploads.upload(context, photo.uri, UploadFolder.Avatar)) {
            is ApiResult.Fail -> return r
            is ApiResult.Ok -> r.value
        }
        return when (val saved = apiCall { api.updateProfile(user.id, ProfileBody(name, mobile, key)) }) {
            is ApiResult.Fail -> saved
            is ApiResult.Ok -> {
                saved.value.data?.let { SessionStore.updateUser(it.toSessionUser(user.role)) }
                ApiResult.Ok(Unit)
            }
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): ApiResult<Unit> =
        apiCall { api.changePassword(ChangePasswordBody(oldPassword, newPassword)) }.discard()

    /** Uploads new photos in order and returns every key, old and new, in the order shown. */
    private suspend fun photoKeys(context: Context, photos: List<Photo>, folder: UploadFolder): ApiResult<List<String>> {
        val keys = mutableListOf<String>()
        for (photo in photos) {
            when (photo) {
                is Photo.Stored -> keys += photo.key
                is Photo.Picked -> when (val r = Uploads.upload(context, photo.uri, folder)) {
                    is ApiResult.Fail -> return r
                    is ApiResult.Ok -> keys += r.value
                }
            }
        }
        return ApiResult.Ok(keys)
    }
}
