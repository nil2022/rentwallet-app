package com.thebackendguy.rentflow.ui.screens.landlord

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebackendguy.rentflow.data.Demo
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.PayStatus
import com.thebackendguy.rentflow.data.Photo
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.isValidMobile
import com.thebackendguy.rentflow.data.mobileDigits
import com.thebackendguy.rentflow.data.session.SessionUser
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.Caption
import com.thebackendguy.rentflow.ui.components.FormBar
import com.thebackendguy.rentflow.ui.components.FormSection
import com.thebackendguy.rentflow.ui.components.HealthTileSpec
import com.thebackendguy.rentflow.ui.components.HealthTiles
import com.thebackendguy.rentflow.ui.components.IconDetail
import com.thebackendguy.rentflow.ui.components.IconRows
import com.thebackendguy.rentflow.ui.components.IconTile
import com.thebackendguy.rentflow.ui.components.InfoCard
import com.thebackendguy.rentflow.ui.components.PersonAvatar
import com.thebackendguy.rentflow.ui.components.Pill
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.RfTextField
import com.thebackendguy.rentflow.ui.components.SectionHead
import com.thebackendguy.rentflow.ui.components.TitledCard
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.LogoutButton
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.screens.auth.PASSWORD_RULE
import com.thebackendguy.rentflow.ui.screens.auth.newPasswordError
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.launch

@Composable
fun LandlordProfileScreen(
    user: SessionUser?,
    shell: Shell,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit
) {
    val portfolio = portfolioState().data
    LaunchedEffect(Unit) { LandlordStore.refresh() }
    // Collection numbers stay sample data until the payments API exists
    val demo = Demo.tenants
    val paid = demo.filter { it.status == PayStatus.Paid }
    val pending = demo.filter { it.status != PayStatus.Paid }
    val percent = if (demo.isEmpty()) 0 else paid.size * 100 / demo.size
    val propertyCount = portfolio?.properties?.size

    AppPage(topBar = { shell.TopBar("Profile") }, bottomBar = { shell.BottomNav(3) }) {
        RfCard(onClick = onEditProfile) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PersonAvatar(user?.name.orEmpty(), user?.photo, size = 64.dp)
                Column(Modifier.weight(1f)) {
                    Text(user?.name.orEmpty(), style = RfType.HeadlineSm, color = Rf.OnSurface)
                    Text(
                        if (propertyCount != null) "Landlord • $propertyCount ${if (propertyCount == 1) "property" else "properties"}" else "Landlord",
                        style = RfType.LabelSm, color = Rf.OnSurfaceVariant
                    )
                }
                LIcon(Lucide.ChevronRight, tint = Rf.Outline)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHead("Owner details") {
                Row(Modifier.pressable(onEditProfile), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LIcon(Lucide.Pencil, size = 14.dp, tint = Rf.Primary)
                    Text("Edit", style = RfType.LabelMd, color = Rf.Primary)
                }
            }
            RfCard {
                IconRows(
                    listOf(
                        IconDetail(Lucide.Smartphone, "Mobile", user?.mobileLabel ?: "Not added"),
                        IconDetail(Lucide.Mail, "Email", user?.email.orEmpty()),
                        IconDetail(Lucide.Users, "Total tenants", portfolio?.tenants?.size?.toString() ?: "…"),
                        IconDetail(Lucide.Building2, "Properties", propertyCount?.toString() ?: "…")
                    )
                )
            }
        }

        TitledCard("Security") {
            Row(
                Modifier.fillMaxWidth().pressable(onChangePassword, pressScale = 0.98f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconTile(Lucide.KeyRound, background = Rf.Low, tint = Rf.Primary)
                Column(Modifier.weight(1f)) {
                    Text("Change password", style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                    Text("Use at least 8 characters", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
                }
                LIcon(Lucide.ChevronRight, tint = Rf.Outline)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHead("Collections") { Caption("May cycle") }
            HealthTiles(
                listOf(
                    HealthTileSpec(Lucide.TrendingUp, "May", "$percent%", "Collection progress", Rf.High, Rf.Primary, Rf.OnSurfaceVariant),
                    HealthTileSpec(Lucide.CircleCheck, "Paid", "${paid.size} tenants", "Rent received", Rf.MintSoft, Rf.Secondary, Rf.Secondary),
                    HealthTileSpec(
                        Lucide.TriangleAlert, "Attention", "${pending.size} tenants", "${inr(pending.sumOf { it.rent })} pending",
                        Rf.ErrorContainer, Rf.Error, Rf.Error,
                        background = Rf.ErrorContainer.copy(alpha = 0.4f), titleColor = Rf.OnErrorContainer, subColor = Rf.OnErrorContainer
                    ),
                    HealthTileSpec(Lucide.Wallet, "Wallet", inr(Demo.WALLET_BALANCE), "Available balance", Rf.Container, Rf.Primary, Rf.OnSurfaceVariant, background = Rf.High)
                )
            )
        }

        RfCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconTile(Lucide.Landmark, background = Rf.High, tint = Rf.Primary)
                Column(Modifier.weight(1f)) {
                    Text("Bank account", style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                    Text(Demo.BANK_ACCOUNT, style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
                }
                Pill("Verified", background = Rf.MintSoft, content = Rf.Secondary)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(8.dp)).background(Rf.Low).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniStat("May rent received", inr(paid.sumOf { it.rent }), Modifier.weight(1f))
                MiniStat("Pending rent", inr(pending.sumOf { it.rent }), Modifier.weight(1f))
            }
        }

        LogoutButton(onLogout)
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = RfType.LabelSm, color = Rf.Outline)
        Text(value, style = RfType.LabelMd.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = Rf.OnSurface)
    }
}

/* ------------------------------ Edit profile ------------------------------ */

@Composable
fun EditProfileScreen(user: SessionUser?, shell: Shell, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(user?.name.orEmpty()) }
    var mobile by rememberSaveable { mutableStateOf(mobileDigits(user?.mobile)) }
    var picked by rememberSaveable { mutableStateOf<Uri?>(null) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val nameErr = if (submitted && name.trim().length < 2) "Enter your full name" else null
    val mobileErr = if (submitted && !isValidMobile(mobile)) "Enter a valid 10-digit mobile number" else null

    fun save() {
        submitted = true
        if (name.trim().length < 2 || !isValidMobile(mobile) || saving) return
        saving = true
        scope.launch {
            val result = LandlordStore.updateProfile(context, name.trim(), mobile, picked?.let { Photo.Picked(it) })
            saving = false
            if (result.announce("Profile saved")) onBack()
        }
    }

    AppPage(
        topBar = { shell.TopBar("Edit Profile", onBack = onBack) },
        bottomBar = { FormBar("Save changes", ::save, loading = saving, secondary = "Cancel", onSecondary = onBack) }
    ) {
        AvatarPicker(name, picked ?: user?.photo, onPick = { picked = it }, onRemove = null)
        FormSection(Lucide.UserRound, "Your details") {
            RfTextField(
                label = "Full name", value = name, onValueChange = { name = it }, icon = Lucide.User,
                placeholder = "Enter your full name", capitalization = KeyboardCapitalization.Words, error = nameErr
            )
            RfTextField(
                label = "Mobile number", value = mobile, onValueChange = { mobile = it.filter(Char::isDigit).take(10) },
                icon = Lucide.Phone, placeholder = "98765 43210", prefix = "+91", keyboardType = KeyboardType.Number, error = mobileErr
            )
            RfTextField(
                label = "Email address", value = user?.email.orEmpty(), onValueChange = {}, icon = Lucide.Mail, placeholder = "",
                enabled = false, hint = "You sign in with this email, so it can’t be changed here."
            )
        }
    }
}

/* ------------------------------ Change password ------------------------------ */

@Composable
fun ChangePasswordScreen(shell: Shell, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var current by rememberSaveable { mutableStateOf("") }
    var next by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val currentErr = if (submitted && current.isEmpty()) "Enter your current password" else null
    val nextErr = if (submitted) newPasswordError(next) else null
    val confirmErr = when {
        !submitted -> null
        confirm.isEmpty() -> "Confirm your new password"
        confirm != next -> "Passwords do not match"
        else -> null
    }

    fun save() {
        submitted = true
        if (current.isEmpty() || newPasswordError(next) != null || confirm != next || saving) return
        saving = true
        scope.launch {
            val result = LandlordStore.changePassword(current, next)
            saving = false
            if (result.announce("Password changed")) onBack()
        }
    }

    AppPage(
        topBar = { shell.TopBar("Change Password", onBack = onBack) },
        bottomBar = { FormBar("Update password", ::save, loading = saving) }
    ) {
        FormSection(Lucide.KeyRound, "Password") {
            RfTextField(
                label = "Current password", value = current, onValueChange = { current = it }, icon = Lucide.Lock,
                placeholder = "Enter your current password", isPassword = true, error = currentErr
            )
            RfTextField(
                label = "New password", value = next, onValueChange = { next = it }, icon = Lucide.Lock,
                placeholder = "Create a new password", isPassword = true, hint = PASSWORD_RULE, error = nextErr
            )
            RfTextField(
                label = "Confirm new password", value = confirm, onValueChange = { confirm = it }, icon = Lucide.Lock,
                placeholder = "Re-enter new password", isPassword = true, imeAction = ImeAction.Done, onImeAction = ::save,
                error = confirmErr
            )
        }
        InfoCard(Lucide.ShieldCheck, "You stay signed in here", "Use the new password the next time you sign in.", iconColor = Rf.Primary)
    }
}
