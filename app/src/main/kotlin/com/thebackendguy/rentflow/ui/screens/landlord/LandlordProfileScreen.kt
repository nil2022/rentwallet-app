package com.thebackendguy.rentflow.ui.screens.landlord

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thebackendguy.rentflow.data.AuthRepository
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.PaymentStore
import com.thebackendguy.rentflow.data.Photo
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.isValidMobile
import com.thebackendguy.rentflow.data.label
import com.thebackendguy.rentflow.data.mobileDigits
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.BillTotalsDto
import com.thebackendguy.rentflow.data.remote.UserDto
import com.thebackendguy.rentflow.data.roomFull
import com.thebackendguy.rentflow.data.session.SessionUser
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.FadeIn
import com.thebackendguy.rentflow.ui.components.FormBar
import com.thebackendguy.rentflow.ui.components.FormSection
import com.thebackendguy.rentflow.ui.components.PersonAvatar
import com.thebackendguy.rentflow.ui.components.PhotoViewer
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.RfTextField
import com.thebackendguy.rentflow.ui.components.Skeleton
import com.thebackendguy.rentflow.ui.components.Spinner
import com.thebackendguy.rentflow.ui.components.Toasts
import com.thebackendguy.rentflow.ui.components.photoChooser
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.screens.LogoutButton
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.screens.auth.emailError
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * The landlord's profile, the web's profile page on a phone (profile/index.jsx):
 * a header with the photo, verified pills and four portfolio tiles, the details
 * edited in place, Security & Login and the Account card.
 */

private val MemberSince = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
private val SignedIn = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)

/** The API's timestamp in the phone's time zone, or null. */
private fun timestamp(value: String?): java.time.ZonedDateTime? =
    value?.let { runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()) }.getOrNull() }

/** The web's short account id, e.g. RF-A1CA. */
private fun displayId(id: String): String = if (id.isBlank()) "—" else "RF-${id.takeLast(4).uppercase()}"

private fun signInMethod(method: String?): String? = when (method) {
    "password" -> "Password"
    "otp" -> "OTP"
    else -> null
}

@Composable
fun LandlordProfileScreen(
    user: SessionUser?,
    shell: Shell,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    onOpenProperties: () -> Unit,
    onOpenRooms: () -> Unit,
    onOpenTenants: () -> Unit,
    onOpenCollection: () -> Unit
) {
    val loaded = portfolioState()
    val portfolio = loaded.data
    LaunchedEffect(Unit) { LandlordStore.refresh() }

    // The account in full (verified flags, last sign-in), and this month's bill totals
    var profile by remember { mutableStateOf<UserDto?>(null) }
    var totals by remember { mutableStateOf<BillTotalsDto?>(null) }
    val billsVersion by PaymentStore.version.collectAsState()
    LaunchedEffect(Unit) { (AuthRepository.profile() as? ApiResult.Ok)?.let { profile = it.value } }
    LaunchedEffect(billsVersion) { (PaymentStore.monthTotals() as? ApiResult.Ok)?.let { totals = it.value } }

    AppPage(topBar = { shell.TopBar("Profile") }, bottomBar = { shell.BottomNav(-1) }) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProfileHeader(user, profile, onPhotoSaved = { profile = it })

            // Portfolio tiles; each opens the page its number comes from
            val rooms = portfolio?.allRooms.orEmpty()
            val full = portfolio?.let { p -> rooms.count { roomFull(it.occupancyType, p.occupants(it.id).size) } } ?: 0
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(
                        "Properties", portfolio?.properties?.size?.toString(),
                        "${rooms.size} ${if (rooms.size == 1) "room" else "rooms"}", Modifier.weight(1f), onOpenProperties
                    )
                    StatTile("Rooms full", portfolio?.let { "$full" }, "of ${rooms.size}", Modifier.weight(1f), onOpenRooms)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Tenants", portfolio?.tenants?.size?.toString(), "in total", Modifier.weight(1f), onOpenTenants)
                    val t = totals
                    StatTile(
                        "Collected • ${YearMonth.now().label()}", t?.let { inr(it.totalSettled) },
                        when {
                            t == null -> ""
                            t.totalDue > 0 -> "${Math.round(t.totalSettled / t.totalDue * 100)}% of ${inr(t.totalDue)}"
                            else -> "nothing billed yet"
                        },
                        Modifier.weight(1f), onOpenCollection
                    )
                }
            }
        }

        PersonalCard(user, profile, onSaved = { profile = it })
        SecurityCard(profile, onChangePassword)
        AccountCard(user, profile)
        LogoutButton(onLogout)
    }
}

/* ------------------------------ Header ------------------------------ */

/** Photo (tap to view; the camera button changes or removes it), name, pills, member since and id. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileHeader(user: SessionUser?, profile: UserDto?, onPhotoSaved: (UserDto) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val name = user?.name.orEmpty()
    val photo = user?.photo

    fun savePhoto(uri: android.net.Uri?) {
        if (saving) return
        saving = true
        scope.launch {
            when (val r = LandlordStore.setProfilePhoto(context, uri)) {
                is ApiResult.Ok -> {
                    onPhotoSaved(r.value)
                    Toasts.show(if (uri != null) "Profile picture updated" else "Profile picture removed")
                }
                is ApiResult.Fail -> Toasts.error(r.message)
            }
            saving = false
        }
    }

    val choose = photoChooser(
        title = "Profile photo",
        onView = if (photo != null) ({ viewing = true }) else null,
        onRemove = if (photo != null) ({ savePhoto(null) }) else null,
        removeHint = "Your initials show instead"
    ) { uris -> savePhoto(uris.first()) }

    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(16.dp), spacing = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box {
                Box(Modifier.pressable(if (photo != null) ({ viewing = true }) else choose)) {
                    PersonAvatar(name.ifBlank { "?" }, photo, size = 76.dp, shape = RoundedCornerShape(18.dp))
                    if (saving) {
                        Box(Modifier.size(76.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                            Spinner(Color.White, size = 22.dp)
                        }
                    }
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 6.dp).size(30.dp).pressable(choose)
                        .clip(RoundedCornerShape(10.dp)).background(Rf.Lowest).padding(3.dp).clip(RoundedCornerShape(8.dp)).background(Rf.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    LIcon(Lucide.Camera, size = 13.dp, tint = Rf.OnPrimary, contentDescription = "Change profile picture")
                }
            }
            Column(Modifier.weight(1f)) {
                Text(name, style = RfType.HeadlineSm.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
                FlowRow(
                    Modifier.padding(top = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    HeaderPill("Landlord", null, Rf.PrimaryFixed, Rf.Primary)
                    if (profile == null) {
                        Skeleton(Modifier.size(width = 92.dp, height = 20.dp), radius = 10.dp)
                    } else {
                        VerifiedPill("Email", profile.isEmailVerified)
                        VerifiedPill("Mobile", profile.isMobileVerified)
                    }
                }
            }
        }
        // Member since and the account id
        FadeIn(profile != null, placeholder = { Skeleton(Modifier.fillMaxWidth(0.7f).height(10.dp), radius = 5.dp) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val since = timestamp(profile?.createdAt)
                if (since != null) {
                    LIcon(Lucide.CalendarDays, size = 13.dp, tint = Rf.OnSurfaceVariant)
                    Text("Member since ${since.format(MemberSince)}", style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
                    Text("•", style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
                }
                Text("Landlord ID: ${displayId(user?.id.orEmpty())}", style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
            }
        }
    }

    if (viewing && photo != null) {
        PhotoViewer(listOf(Photo.Stored("", photo)), 0, onClose = { viewing = false })
    }
}

@Composable
private fun HeaderPill(text: String, icon: LucideIcon?, background: Color, content: Color) {
    Row(
        Modifier.clip(CircleShape).background(background).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) LIcon(icon, size = 12.dp, tint = content)
        Text(text, style = RfType.LabelSm.copy(fontWeight = FontWeight.Bold), color = content, maxLines = 1)
    }
}

@Composable
private fun VerifiedPill(what: String, ok: Boolean) {
    if (ok) HeaderPill("$what verified", Lucide.CircleCheck, Rf.MintSoft, Rf.Secondary)
    else HeaderPill("$what not verified", Lucide.CircleAlert, Rf.AmberSoft, Rf.Amber)
}

/** A portfolio number with its unit; null [value] pulses while it loads. */
@Composable
private fun StatTile(label: String, value: String?, unit: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.pressable(onClick, pressScale = 0.97f).clip(RoundedCornerShape(12.dp)).background(Rf.Low)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Caps(label)
        FadeIn(value != null, placeholder = { Skeleton(Modifier.padding(top = 5.dp).width(64.dp).height(16.dp)) }) {
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(value.orEmpty(), style = RfType.BodyLg.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface, maxLines = 1)
                if (unit.isNotEmpty()) {
                    Text(unit, style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
                }
            }
        }
    }
}

/* ------------------------------ Cards ------------------------------ */

@Composable
private fun PanelHead(icon: LucideIcon, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Rf.PrimaryFixed), contentAlignment = Alignment.Center) {
            LIcon(icon, size = 17.dp, tint = Rf.Primary)
        }
        Column {
            Text(title, style = RfType.BodyLg.copy(fontWeight = FontWeight.ExtraBold), color = Rf.OnSurface)
            Text(subtitle, style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
        }
    }
}

/** A field's label with a verified pill on the right. */
@Composable
private fun FieldLabel(text: String, verified: Boolean?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface, modifier = Modifier.weight(1f))
        when (verified) {
            true -> HeaderPill("Verified", Lucide.CircleCheck, Rf.MintSoft, Rf.Secondary)
            false -> HeaderPill("Not verified", null, Rf.Low, Rf.OnSurfaceVariant)
            null -> Unit
        }
    }
}

/**
 * Name, email and mobile, edited in place as on the web. Changing one shows the
 * save bar; only the fields that changed are sent.
 */
@Composable
private fun PersonalCard(user: SessionUser?, profile: UserDto?, onSaved: (UserDto) -> Unit) {
    val scope = rememberCoroutineScope()
    val savedName = user?.name.orEmpty()
    val savedEmail = user?.email.orEmpty()
    val savedMobile = mobileDigits(user?.mobile)
    var name by rememberSaveable(savedName) { mutableStateOf(savedName) }
    var email by rememberSaveable(savedEmail) { mutableStateOf(savedEmail) }
    var mobile by rememberSaveable(savedMobile) { mutableStateOf(savedMobile) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val dirty = name.trim() != savedName || email.trim().lowercase() != savedEmail.lowercase() || mobile != savedMobile
    val emailChanged = email.trim().lowercase() != savedEmail.lowercase()
    val nameErr = if (submitted && name.trim().length < 2) "Enter your full name" else null
    val emailErr = if (submitted) emailError(email) else null
    val mobileErr = if (submitted && !isValidMobile(mobile)) "Enter a valid 10-digit mobile number" else null

    fun discard() {
        name = savedName
        email = savedEmail
        mobile = savedMobile
        submitted = false
    }

    fun save() {
        submitted = true
        if (name.trim().length < 2 || emailError(email) != null || !isValidMobile(mobile) || saving) return
        saving = true
        scope.launch {
            val result = LandlordStore.updateProfile(
                name = name.trim().takeIf { it != savedName },
                email = email.takeIf { emailChanged },
                mobile = mobile.takeIf { it != savedMobile }
            )
            saving = false
            when (result) {
                is ApiResult.Ok -> {
                    submitted = false
                    onSaved(result.value)
                    Toasts.show("Profile updated")
                }
                is ApiResult.Fail -> Toasts.error(result.message)
            }
        }
    }

    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(0.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PanelHead(Lucide.UserRound, "Personal & Contact Details", "Name, email and phone on your account")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Full name", null)
                RfTextField(
                    label = "", value = name, onValueChange = { name = it }, icon = Lucide.User, placeholder = "Your full name",
                    capitalization = KeyboardCapitalization.Words, error = nameErr
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Email address", profile?.isEmailVerified)
                RfTextField(
                    label = "", value = email, onValueChange = { email = it.trim() }, icon = Lucide.Mail, placeholder = "you@example.com",
                    keyboardType = KeyboardType.Email, error = emailErr,
                    hint = if (emailChanged) "You’ll sign in with this email after you save." else null
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Mobile number", profile?.isMobileVerified)
                RfTextField(
                    label = "", value = mobile, onValueChange = { mobile = it.filter(Char::isDigit).take(10) }, icon = Lucide.Phone,
                    placeholder = "98765 43210", prefix = "+91", keyboardType = KeyboardType.Number, imeAction = ImeAction.Done,
                    onImeAction = ::save, error = mobileErr
                )
            }
            Text("Tenants see this name and phone on their bills.", style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
        }
        if (dirty) {
            HorizontalDivider(thickness = 1.dp, color = Rf.Low)
            Row(
                Modifier.fillMaxWidth().background(Rf.Low).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("You have unsaved changes", style = RfType.LabelSm.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface, modifier = Modifier.weight(1f))
                Text(
                    "Discard", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface,
                    modifier = Modifier.pressable(::discard).clip(RoundedCornerShape(10.dp)).background(Rf.Lowest)
                        .border(1.dp, Rf.OutlineVariant, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Row(
                    Modifier.pressable(::save, enabled = !saving).clip(RoundedCornerShape(10.dp)).background(Rf.Primary)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (saving) Spinner(Rf.OnPrimary, size = 14.dp) else LIcon(Lucide.Save, size = 14.dp, tint = Rf.OnPrimary)
                    Text(if (saving) "Saving…" else "Save changes", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.OnPrimary)
                }
            }
        }
    }
}

/** Password with Change Password, and the last sign-in. */
@Composable
private fun SecurityCard(profile: UserDto?, onChangePassword: () -> Unit) {
    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(16.dp), spacing = 12.dp) {
        PanelHead(Lucide.ShieldCheck, "Security & Login", "Your password and how you sign in")
        SecurityRow(Lucide.KeyRound, "Password", "Use at least 6 characters. Change it if you think someone else knows it.") {
            Text(
                "Change Password", style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.Primary,
                modifier = Modifier.pressable(onChangePassword).clip(RoundedCornerShape(10.dp)).background(Rf.PrimaryFixed)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        timestamp(profile?.lastLoginAt)?.let { at ->
            val method = signInMethod(profile?.loginMethod)
            SecurityRow(Lucide.Clock3, "Last sign-in", at.format(SignedIn) + (method?.let { " • with ${if (it == "OTP") "OTP" else "password"}" } ?: ""))
        }
    }
}

@Composable
private fun SecurityRow(icon: LucideIcon, title: String, caption: String, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Rf.Low).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LIcon(icon, size = 18.dp, tint = Rf.OnSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(title, style = RfType.LabelMd.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface)
            Text(caption, style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
        }
        action?.invoke()
    }
}

/** Facts about the landlord account, straight from the profile API. */
@Composable
private fun AccountCard(user: SessionUser?, profile: UserDto?) {
    RfCard(shape = RoundedCornerShape(16.dp), padding = PaddingValues(16.dp), spacing = 8.dp) {
        PanelHead(Lucide.Info, "Account", "Details of this landlord account")
        Column {
            AccountRow("Landlord ID", displayId(user?.id.orEmpty()), first = true)
            timestamp(profile?.createdAt)?.let { AccountRow("Member since", it.format(MemberSince)) }
            signInMethod(profile?.loginMethod)?.let { AccountRow("Sign-in method", it) }
            AccountRow("Email", profile?.let { if (it.isEmailVerified) "Verified" else "Not verified" }, ok = profile?.isEmailVerified == true)
            AccountRow("Mobile", profile?.let { if (it.isMobileVerified) "Verified" else "Not verified" }, ok = profile?.isMobileVerified == true)
        }
    }
}

/** A null [value] pulses while the profile loads. */
@Composable
private fun AccountRow(label: String, value: String?, first: Boolean = false, ok: Boolean? = null) {
    if (!first) HorizontalDivider(thickness = 1.dp, color = Rf.Low)
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = RfType.BodySm, color = Rf.OnSurfaceVariant, modifier = Modifier.weight(1f))
        FadeIn(value != null, placeholder = { Skeleton(Modifier.width(70.dp).height(13.dp)) }) {
            Text(
                value.orEmpty(), style = RfType.BodySm.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.End,
                color = when (ok) {
                    true -> Rf.Secondary
                    false -> Rf.OnSurfaceVariant
                    null -> Rf.OnSurface
                }
            )
        }
    }
}

/* ------------------------------ Change password ------------------------------ */

/** The web's rule for a new password (the API's changePasswordSchema). */
private const val MIN_PASSWORD = 6

/** [change] saves the new password: the landlord's endpoint by default, the tenant's for a tenant. */
@Composable
fun ChangePasswordScreen(
    shell: Shell,
    onBack: () -> Unit,
    change: suspend (String, String) -> ApiResult<Unit> = LandlordStore::changePassword
) {
    val scope = rememberCoroutineScope()
    var current by rememberSaveable { mutableStateOf("") }
    var next by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    // The web's three checks; the button saves once all are met
    val rules = listOf(
        "At least $MIN_PASSWORD characters" to (next.length >= MIN_PASSWORD),
        "Different from your current password" to (next.isNotEmpty() && next != current),
        "Both new passwords match" to (confirm.isNotEmpty() && next == confirm)
    )
    val ready = current.isNotEmpty() && rules.all { it.second }

    fun save() {
        if (!ready || saving) return
        saving = true
        scope.launch {
            val result = change(current, next)
            saving = false
            if (result.announce("Password changed")) onBack()
        }
    }

    AppPage(
        topBar = { shell.TopBar("Change Password", onBack = onBack) },
        bottomBar = { FormBar("Change password", ::save, loading = saving, secondary = "Cancel", onSecondary = onBack, enabled = ready) }
    ) {
        FormSection(Lucide.KeyRound, "Password") {
            RfTextField(
                label = "Current password", value = current, onValueChange = { current = it }, icon = Lucide.Lock,
                placeholder = "Your current password", isPassword = true
            )
            RfTextField(
                label = "New password", value = next, onValueChange = { next = it }, icon = Lucide.Lock,
                placeholder = "At least $MIN_PASSWORD characters", isPassword = true
            )
            RfTextField(
                label = "Confirm new password", value = confirm, onValueChange = { confirm = it }, icon = Lucide.Lock,
                placeholder = "Type the new password again", isPassword = true, imeAction = ImeAction.Done, onImeAction = ::save
            )
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Rf.Low).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                rules.forEach { (text, ok) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        LIcon(Lucide.CircleCheck, size = 15.dp, tint = if (ok) Rf.Secondary else Rf.Outline)
                        Text(
                            text, style = RfType.LabelMd.copy(fontWeight = if (ok) FontWeight.SemiBold else FontWeight.Medium),
                            color = if (ok) Rf.Secondary else Rf.OnSurfaceVariant
                        )
                    }
                }
            }
        }
        Text(
            "Forgot your current password? Sign out and use “Forgot Password?” on the sign-in page.",
            style = RfType.BodySm, color = Rf.OnSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
