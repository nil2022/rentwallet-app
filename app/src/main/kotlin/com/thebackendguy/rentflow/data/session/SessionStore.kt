package com.thebackendguy.rentflow.data.session

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.edit
import com.thebackendguy.rentflow.data.UserRole
import com.thebackendguy.rentflow.data.remote.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SessionUser(
    val id: String,
    val role: UserRole,
    val name: String,
    val email: String,
    val mobile: String?,
    val photo: String? = null
) {
    val firstName: String get() = name.trim().substringBefore(' ').ifEmpty { "there" }

    val initials: String
        get() = name.trim().split(Regex("\\s+")).filter(String::isNotEmpty).take(2)
            .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

    /** "+91 98765 43210" for a 10-digit Indian mobile, otherwise as stored. */
    val mobileLabel: String?
        get() = mobile?.filter(Char::isDigit)?.takeLast(10)?.takeIf { it.length == 10 }
            ?.let { "+91 ${it.take(5)} ${it.drop(5)}" } ?: mobile
}

data class Session(val token: String, val user: SessionUser)

fun UserDto.toSessionUser(role: UserRole) =
    SessionUser(id = id, role = role, name = fullName, email = email, mobile = mobile, photo = profilePic?.takeIf(String::isNotBlank))

/**
 * The signed-in account and its token. With "Remember me" it's saved on the
 * phone so the app opens signed in; without it, it lasts until the user closes
 * the app (see [saveTo]).
 */
object SessionStore {
    private const val KEY_TOKEN = "token"
    private const val KEY_ROLE = "role"
    private const val KEY_ID = "id"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private const val KEY_MOBILE = "mobile"
    private const val KEY_PHOTO = "photo"

    private const val KEY_LAST_ROLE = "last_role"

    private lateinit var prefs: SharedPreferences
    // Kept apart from the session, so logging out doesn't forget it
    private lateinit var settings: SharedPreferences
    private val _session = MutableStateFlow<Session?>(null)
    private val _ended = MutableStateFlow(false)

    val session: StateFlow<Session?> = _session

    /** True when the server rejected the token, so login can say why the user is there. */
    val endedByServer: StateFlow<Boolean> = _ended

    val token: String? get() = _session.value?.token

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("rentflow_session", Context.MODE_PRIVATE)
        settings = context.applicationContext.getSharedPreferences("rentflow_settings", Context.MODE_PRIVATE)
        _session.value = sessionOf { prefs.getString(it, null) }
    }

    /**
     * Keeps a session without "Remember me" in the screen's saved state, so it
     * survives Android closing the app while it is away (the camera often needs
     * the memory). That state goes when the user closes the app, and so does the session.
     */
    fun saveTo(state: Bundle) {
        val current = _session.value ?: return
        if (prefs.contains(KEY_TOKEN)) return // saved on the phone already
        write(current) { key, value -> state.putString(key, value) }
    }

    /** The session [saveTo] kept, when Android reopens the app; a saved one comes first. */
    fun restoreFrom(state: Bundle?) {
        if (state == null || _session.value != null) return
        _session.value = sessionOf { state.getString(it) }
    }

    /** The role of the last sign-in, for Login to start on; Landlord before the first one. */
    val lastRole: UserRole
        get() = UserRole.entries.find { it.name == settings.getString(KEY_LAST_ROLE, null) } ?: UserRole.Landlord

    fun start(token: String, user: SessionUser, remember: Boolean) {
        _ended.value = false
        _session.value = Session(token, user)
        settings.edit { putString(KEY_LAST_ROLE, user.role.name) }
        if (remember) save(token, user) else prefs.edit { clear() }
    }

    fun updateUser(user: SessionUser) {
        val current = _session.value ?: return
        _session.value = current.copy(user = user)
        if (prefs.contains(KEY_TOKEN)) save(current.token, user)
    }

    fun clear() {
        _session.value = null
        prefs.edit { clear() }
    }

    fun expire() {
        if (_session.value == null) return
        clear()
        _ended.value = true
    }

    private fun save(token: String, user: SessionUser) = prefs.edit {
        write(Session(token, user)) { key, value -> putString(key, value) }
    }

    /** Each part of [session] under its key, for the prefs or the saved state. */
    private fun write(session: Session, put: (String, String?) -> Unit) {
        put(KEY_TOKEN, session.token)
        put(KEY_ROLE, session.user.role.name)
        put(KEY_ID, session.user.id)
        put(KEY_NAME, session.user.name)
        put(KEY_EMAIL, session.user.email)
        put(KEY_MOBILE, session.user.mobile)
        put(KEY_PHOTO, session.user.photo)
    }

    /** A session read back by key, or null without a token and role. */
    private fun sessionOf(read: (String) -> String?): Session? {
        val token = read(KEY_TOKEN) ?: return null
        val role = UserRole.entries.find { it.name == read(KEY_ROLE) } ?: return null
        return Session(
            token,
            SessionUser(
                id = read(KEY_ID).orEmpty(),
                role = role,
                name = read(KEY_NAME).orEmpty(),
                email = read(KEY_EMAIL).orEmpty(),
                mobile = read(KEY_MOBILE),
                photo = read(KEY_PHOTO)
            )
        )
    }
}
