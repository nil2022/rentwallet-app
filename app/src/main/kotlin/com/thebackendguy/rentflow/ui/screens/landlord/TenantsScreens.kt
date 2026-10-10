package com.thebackendguy.rentflow.ui.screens.landlord

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.Photo
import com.thebackendguy.rentflow.data.isValidMobile
import com.thebackendguy.rentflow.data.mobileDigits
import com.thebackendguy.rentflow.data.remote.TenantDto
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.FormBar
import com.thebackendguy.rentflow.ui.components.FormSection
import com.thebackendguy.rentflow.ui.components.PersonAvatar
import com.thebackendguy.rentflow.ui.components.PhotoViewer
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.RfTextField
import com.thebackendguy.rentflow.ui.components.SwitchRow
import com.thebackendguy.rentflow.ui.components.photoChooser
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.components.rememberPhotoLoad
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.screens.auth.optionalEmailError
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.launch

/* ------------------------------ Edit tenant ------------------------------ */

@Composable
fun TenantFormScreen(tenantId: String, shell: Shell, onBack: () -> Unit, onSaved: () -> Unit) {
    val state = portfolioState()
    val tenant = state.data?.tenant(tenantId)
    if (tenant == null) {
        AppPage(topBar = { shell.TopBar("Edit Tenant", onBack = onBack) }) {
            PortfolioContent(state) { NotFound("Tenant", onBack) }
        }
        return
    }
    EditTenantForm(tenant, shell, onBack, onSaved)
}

@Composable
private fun EditTenantForm(tenant: TenantDto, shell: Shell, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(tenant.fullName) }
    var mobile by rememberSaveable { mutableStateOf(mobileDigits(tenant.mobile)) }
    var email by rememberSaveable { mutableStateOf(tenant.email) }
    var active by rememberSaveable { mutableStateOf(tenant.isActive) }
    var picked by rememberSaveable { mutableStateOf<Uri?>(null) }
    var removed by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val nameErr = if (submitted && name.trim().length < 2) "Enter the tenant’s full name" else null
    val mobileErr = if (submitted && !isValidMobile(mobile)) "Enter a valid 10-digit mobile number" else null
    val emailErr = if (submitted) optionalEmailError(email) else null

    fun save() {
        submitted = true
        if (name.trim().length < 2 || !isValidMobile(mobile) || optionalEmailError(email) != null) return
        if (saving) return
        saving = true
        scope.launch {
            val result = LandlordStore.updateTenant(context, tenant.id, name.trim(), email, mobile, active, picked?.let { Photo.Picked(it) }, removed)
            saving = false
            if (result.announce("Tenant saved")) onSaved()
        }
    }

    AppPage(
        topBar = { shell.TopBar("Edit Tenant", onBack = onBack) },
        bottomBar = { FormBar("Save changes", ::save, loading = saving, secondary = "Cancel", onSecondary = onBack) }
    ) {
        AvatarPicker(
            name = name,
            photo = picked ?: if (removed) null else tenant.profilePic,
            onPick = {
                picked = it
                removed = false
            },
            onRemove = {
                picked = null
                removed = true
            }
        )
        FormSection(Lucide.UserRound, "Details") {
            RfTextField(
                label = "Full name", value = name, onValueChange = { name = it }, icon = Lucide.User,
                placeholder = "e.g. Priya Mehta", capitalization = KeyboardCapitalization.Words, error = nameErr
            )
            RfTextField(
                label = "Mobile number", value = mobile, onValueChange = { mobile = it.filter(Char::isDigit).take(10) },
                icon = Lucide.Phone, placeholder = "98765 43210", prefix = "+91", keyboardType = KeyboardType.Number, error = mobileErr
            )
            RfTextField(
                label = "Email address (optional)", value = email, onValueChange = { email = it.trim() }, icon = Lucide.Mail,
                placeholder = "priya.mehta@example.com", keyboardType = KeyboardType.Email, error = emailErr,
                // As on the web: the API can change a saved email but not remove it
                hint = if (tenant.email.isNotBlank()) "A saved email can be changed but not removed."
                else "Without an email the tenant signs in with their mobile number."
            )
        }
        FormSection(Lucide.ShieldCheck, "Account") {
            SwitchRow(Lucide.UserCheck, "Account active", active, { active = it }, sub = "Inactive tenants can’t sign in")
        }
    }
}

/**
 * Round photo with a camera button; shows [name]'s initials when there's no
 * photo. Tapping it offers to view, take, choose (then crop round) or remove.
 */
@Composable
internal fun AvatarPicker(name: String, photo: Any?, onPick: (Uri) -> Unit, onRemove: (() -> Unit)?) {
    var viewing by remember { mutableStateOf(false) }
    val open = photoChooser(
        round = true,
        title = "Profile photo",
        onView = if (photo != null) ({ viewing = true }) else null,
        onRemove = if (photo != null) onRemove else null
    ) { uris -> onPick(uris.first()) }
    // The saved photo's download, so a failed one can say so next to the text
    val load = rememberPhotoLoad(photo as? String)
    RfCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(68.dp).pressable(open)) {
                when (photo) {
                    null -> PersonAvatar(name.ifBlank { "?" }, null, size = 64.dp)
                    is String -> PersonAvatar(name, photo, size = 64.dp, retryBadge = false, load = load)
                    else -> AsyncImage(
                        model = photo, contentDescription = name, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(CircleShape)
                    )
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).size(26.dp).clip(CircleShape).background(Rf.Lowest).padding(2.dp)
                        .clip(CircleShape).background(Rf.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    LIcon(Lucide.Camera, size = 14.dp, tint = Rf.OnPrimary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Profile photo", style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                Text("Optional. Tap the photo to take or choose one.", style = RfType.BodySm, color = Rf.OnSurfaceVariant)
                if (photo is String && load.failed) {
                    Row(Modifier.pressable(load::retry), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LIcon(Lucide.RefreshCw, size = 14.dp, tint = Rf.Error)
                        Text("Photo didn’t load · Retry", style = RfType.LabelMd, color = Rf.Error)
                    }
                }
            }
        }
    }
    if (viewing && photo != null) {
        val shown = if (photo is Uri) Photo.Picked(photo) else Photo.Stored("", photo.toString())
        PhotoViewer(listOf(shown), 0, onClose = { viewing = false })
    }
}
