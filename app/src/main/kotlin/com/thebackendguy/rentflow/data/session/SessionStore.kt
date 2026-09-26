package com.thebackendguy.rentflow.data.session

import android.content.Context
import android.content.SharedPreferences
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
 * phone so the app opens signed in; without it, it lasts until the app closes.
 */
object SessionStore {
    private const val KEY_TOKEN = "token"
    private const val KEY_ROLE = "role"
    private const val KEY_ID = "id"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private const val KEY_MOBILE = "mobile"
    private const val KEY_PHOTO = "photo"

    private lateinit var prefs: SharedPreferences
    private val _session = MutableStateFlow<Session?>(null)
    private val _ended = MutableStateFlow(false)

    val session: StateFlow<Session?> = _session

    /** True when the server rejected the token, so login can say why the user is there. */
    val endedByServer: StateFlow<Boolean> = _ended

    val token: String? get() = _session.value?.token

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("rentflow_session", Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)
        val role = UserRole.entries.find { it.name == prefs.getString(KEY_ROLE, null) }
        if (token != null && role != null) {
            _session.value = Session(
                token,
                SessionUser(
                    id = prefs.getString(KEY_ID, "").orEmpty(),
                    role = role,
                    name = prefs.getString(KEY_NAME, "").orEmpty(),
                    email = prefs.getString(KEY_EMAIL, "").orEmpty(),
                    mobile = prefs.getString(KEY_MOBILE, null),
                    photo = prefs.getString(KEY_PHOTO, null)
                )
            )
        }
    }

    fun start(token: String, user: SessionUser, remember: Boolean) {
        _ended.value = false
        _session.value = Session(token, user)
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
        putString(KEY_TOKEN, token)
        putString(KEY_ROLE, user.role.name)
        putString(KEY_ID, user.id)
        putString(KEY_NAME, user.name)
        putString(KEY_EMAIL, user.email)
        putString(KEY_MOBILE, user.mobile)
        putString(KEY_PHOTO, user.photo)
    }
}
