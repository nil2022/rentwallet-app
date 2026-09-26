package com.thebackendguy.myandroidtestapp.ui.screens.auth

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.thebackendguy.myandroidtestapp.data.UserRole
import com.thebackendguy.myandroidtestapp.ui.components.IconTile
import com.thebackendguy.myandroidtestapp.ui.components.pressable
import com.thebackendguy.myandroidtestapp.ui.icons.LIcon
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.icons.LucideIcon
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RfType
import kotlinx.coroutines.delay

/* ------------------------------ Validation ------------------------------ */

const val PASSWORD_RULE = "8+ characters with upper & lower case, a number and a symbol"

fun emailError(email: String): String? = when {
    email.isBlank() -> "Email is required"
    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address"
    else -> null
}

fun newPasswordError(password: String): String? = when {
    password.isEmpty() -> "Password is required"
    password.length < 8 || password.none(Char::isUpperCase) || password.none(Char::isLowerCase) ||
        password.none(Char::isDigit) || password.all(Char::isLetterOrDigit) -> "Use $PASSWORD_RULE"
    else -> null
}

fun codeError(code: String): String? = if (code.length == 6 && code.all(Char::isDigit)) null else "Enter the 6-digit code"

/* ------------------------------ Shared pieces ------------------------------ */

/** Seconds left before a code can be resent; counts down once per second. */
@Composable
fun rememberCountdown(): MutableIntState {
    val seconds = rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(seconds.intValue > 0) {
        while (seconds.intValue > 0) {
            delay(1000)
            seconds.intValue -= 1
        }
    }
    return seconds
}

/** Icon tile above a screen headline. */
@Composable
fun HeadIcon(icon: LucideIcon) {
    IconTile(icon, background = Rf.PrimaryFixed, tint = Rf.Primary, size = 44.dp, radius = 12.dp, iconSize = 22.dp,
        modifier = Modifier.padding(bottom = 16.dp))
}

@Composable
fun RfCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String) {
    Row(
        Modifier.pressable({ onCheckedChange(!checked) }, pressScale = 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val shape = RoundedCornerShape(4.dp)
        Box(
            Modifier
                .size(16.dp)
                .clip(shape)
                .background(if (checked) Rf.Primary else Rf.Lowest)
                .border(1.5.dp, if (checked) Rf.Primary else Rf.Outline, shape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) LIcon(Lucide.Check, size = 12.dp, tint = Color.White, strokeWidth = 3f)
        }
        Text(label, style = RfType.BodySm, color = Rf.OnSurfaceVariant)
    }
}

/** "Tenant account" / "Landlord account" chip on the reset screen. */
@Composable
fun RoleChip(role: UserRole) {
    Row(
        Modifier.clip(CircleShape).background(Rf.Low).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LIcon(if (role == UserRole.Tenant) Lucide.House else Lucide.Building2, size = 16.dp, tint = Rf.Primary)
        Text("${role.name} account", style = RfType.LabelMd, color = Rf.Primary)
    }
}

@Composable
fun BackToLogin(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            Modifier.pressable(onClick).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LIcon(Lucide.ArrowLeft, size = 16.dp, tint = Rf.Primary)
            Text("Back to Login", style = RfType.LabelMd, color = Rf.Primary)
        }
    }
}
