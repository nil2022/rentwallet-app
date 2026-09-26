package com.thebackendguy.myandroidtestapp.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.thebackendguy.myandroidtestapp.data.UserRole
import com.thebackendguy.myandroidtestapp.ui.components.AuthShell
import com.thebackendguy.myandroidtestapp.ui.components.PrimaryButton
import com.thebackendguy.myandroidtestapp.ui.components.RfTextField
import com.thebackendguy.myandroidtestapp.ui.icons.LIcon
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RfType

/**
 * Password reset in three steps, with the web's wording: email, then the code
 * and a new password, then a confirmation.
 */
@Composable
fun ForgotPasswordScreen(role: UserRole, onBack: () -> Unit, onBackToLogin: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val goBack: () -> Unit = {
        submitted = false
        if (step == 1) step = 0 else onBack()
    }
    BackHandler(enabled = step == 1) { goBack() }

    AuthShell(onBack = if (step == 2) onBackToLogin else goBack) {
        when (step) {
            0 -> {
                HeadIcon(Lucide.KeyRound)
                Text("Forgot Password?", style = RfType.HeadlineLg, color = Rf.OnSurface)
                Text(
                    "Enter your email address and we’ll send you an OTP to reset your password.",
                    style = RfType.BodyMd, color = Rf.OnSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                RoleChip(role)
                Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val sendOtp: () -> Unit = {
                        submitted = true
                        if (emailError(email) == null) {
                            submitted = false
                            step = 1
                        }
                    }
                    RfTextField(
                        label = "Email Address",
                        value = email,
                        onValueChange = { email = it.trim() },
                        icon = Lucide.Mail,
                        placeholder = "Enter your registered ${role.name.lowercase()} email",
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Send,
                        onImeAction = sendOtp,
                        error = if (submitted) emailError(email) else null
                    )
                    PrimaryButton("Send Reset OTP (${role.name})", onClick = sendOtp, modifier = Modifier.padding(top = 8.dp))
                }
                BackToLogin(onBackToLogin)
            }

            1 -> {
                HeadIcon(Lucide.LockKeyhole)
                Text("Reset Password", style = RfType.HeadlineLg, color = Rf.OnSurface)
                Text(
                    text = buildAnnotatedString {
                        append("Enter the OTP sent to ")
                        withStyle(SpanStyle(color = Rf.OnSurface, fontWeight = FontWeight.SemiBold)) { append(email) }
                        append(" along with your new password.")
                    },
                    style = RfType.BodyMd, color = Rf.OnSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )
                val confirmError = when {
                    !submitted -> null
                    confirm.isEmpty() -> "Confirm your new password"
                    confirm != newPassword -> "Passwords do not match"
                    else -> null
                }
                val reset: () -> Unit = {
                    submitted = true
                    if (codeError(code) == null && newPasswordError(newPassword) == null && confirm == newPassword) {
                        submitted = false
                        step = 2
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    RfTextField(
                        label = "OTP",
                        value = code,
                        onValueChange = { code = it.filter(Char::isDigit).take(6) },
                        icon = Lucide.KeyRound,
                        placeholder = "Enter 6-digit OTP",
                        keyboardType = KeyboardType.NumberPassword,
                        error = if (submitted) codeError(code) else null
                    )
                    RfTextField(
                        label = "New Password",
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        icon = Lucide.Lock,
                        placeholder = "Enter new password",
                        isPassword = true,
                        hint = PASSWORD_RULE,
                        error = if (submitted) newPasswordError(newPassword) else null
                    )
                    RfTextField(
                        label = "Confirm New Password",
                        value = confirm,
                        onValueChange = { confirm = it },
                        icon = Lucide.Lock,
                        placeholder = "Re-enter new password",
                        isPassword = true,
                        imeAction = ImeAction.Done,
                        onImeAction = reset,
                        error = confirmError
                    )
                    PrimaryButton("Reset Password", onClick = reset, modifier = Modifier.padding(top = 8.dp))
                }
                BackToLogin(onBackToLogin)
            }

            else -> {
                Column(
                    Modifier.fillMaxWidth().padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Rf.MintSoft.copy(alpha = 0.18f))
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(Rf.MintSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        LIcon(Lucide.CircleCheck, size = 36.dp, tint = Rf.Secondary)
                    }
                    Text("Password Reset Successful!", style = RfType.HeadlineLg, color = Rf.OnSurface, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp))
                    Text(
                        "Your password has been updated. You can now log in with your new password.",
                        style = RfType.BodyMd, color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center
                    )
                }
                PrimaryButton("Go to Login", onClick = onBackToLogin, modifier = Modifier.padding(top = 32.dp))
            }
        }
    }
}
