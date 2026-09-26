package com.thebackendguy.myandroidtestapp.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thebackendguy.myandroidtestapp.data.UserRole
import com.thebackendguy.myandroidtestapp.ui.components.AuthShell
import com.thebackendguy.myandroidtestapp.ui.components.InfoCard
import com.thebackendguy.myandroidtestapp.ui.components.PrimaryButton
import com.thebackendguy.myandroidtestapp.ui.components.RfTextField
import com.thebackendguy.myandroidtestapp.ui.components.RoleSwitch
import com.thebackendguy.myandroidtestapp.ui.components.TextLink
import com.thebackendguy.myandroidtestapp.ui.components.pressable
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RfType

/**
 * One sign-in screen for both roles (the web's MobileLoginForm): email with a
 * password, or a one-time code. There is no API yet, so any valid email and
 * password, or any 6-digit code, signs in.
 */
@Composable
fun LoginScreen(
    initialRole: UserRole,
    onBack: () -> Unit,
    onSignedIn: (UserRole) -> Unit,
    onForgotPassword: (UserRole) -> Unit,
    onRegister: () -> Unit
) {
    var role by rememberSaveable { mutableStateOf(initialRole) }
    var usingOtp by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var otpSent by rememberSaveable { mutableStateOf(false) }
    var rememberMe by rememberSaveable { mutableStateOf(true) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val timer = rememberCountdown()

    val emailErr = if (submitted) emailError(email) else null
    val passwordErr = if (submitted && !usingOtp && password.isEmpty()) "Password is required" else null
    val codeErr = if (submitted && usingOtp) (if (!otpSent) "Tap Send code first" else codeError(code)) else null

    fun submit() {
        submitted = true
        val ok = emailError(email) == null && if (usingOtp) otpSent && codeError(code) == null else password.isNotEmpty()
        if (ok) onSignedIn(role)
    }

    fun sendCode() {
        if (emailError(email) != null) {
            submitted = true
            return
        }
        otpSent = true
        code = ""
        timer.intValue = 60
    }

    AuthShell(onBack = onBack) {
        AnimatedContent(targetState = role, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "loginTitle") { r ->
            Text(
                text = if (r == UserRole.Landlord) "Welcome back, Landlord" else "Welcome back, Resident",
                style = RfType.HeadlineLg,
                color = Rf.OnSurface
            )
        }
        Text(
            text = when {
                usingOtp -> "We’ll email you a one-time code to sign in."
                role == UserRole.Landlord -> "Manage your properties and track rent collections."
                else -> "Pay your rent, download receipts, & review your lease."
            },
            style = RfType.BodyMd,
            color = Rf.OnSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        RoleSwitch(selected = role, onSelect = { role = it })

        Column(Modifier.padding(top = 24.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RfTextField(
                label = "Email Address",
                value = email,
                onValueChange = { email = it.trim() },
                icon = Lucide.Mail,
                placeholder = "name@company.com",
                keyboardType = KeyboardType.Email,
                error = emailErr
            )
            if (usingOtp) {
                RfTextField(
                    label = "One-Time Code",
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(6) },
                    icon = Lucide.KeyRound,
                    placeholder = if (otpSent) "6-digit code" else "Tap Send code first",
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Go,
                    onImeAction = ::submit,
                    enabled = otpSent,
                    error = codeErr,
                    trailing = {
                        val waiting = timer.intValue > 0
                        Text(
                            text = when {
                                waiting -> "${timer.intValue}s"
                                otpSent -> "Resend"
                                else -> "Send code"
                            },
                            style = RfType.LabelSm,
                            color = if (waiting) Rf.Outline else Rf.Primary,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .pressable(if (waiting) null else ({ sendCode() }))
                                .clip(RoundedCornerShape(8.dp))
                                .background(Rf.Low)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                )
            } else {
                RfTextField(
                    label = "Password",
                    value = password,
                    onValueChange = { password = it },
                    icon = Lucide.Lock,
                    placeholder = "Enter your password",
                    isPassword = true,
                    imeAction = ImeAction.Go,
                    onImeAction = ::submit,
                    error = passwordErr
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RfCheckbox(checked = rememberMe, onCheckedChange = { rememberMe = it }, label = "Remember for 30 days")
                    TextLink("Forgot Password?", onClick = { onForgotPassword(role) })
                }
            }
            PrimaryButton(
                text = if (usingOtp) "Verify & Sign In" else "Sign In to Dashboard",
                onClick = ::submit,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Box(Modifier.fillMaxWidth().padding(bottom = 24.dp), contentAlignment = Alignment.Center) {
            TextLink(
                text = if (usingOtp) "Back to password login" else "Login with OTP instead",
                onClick = {
                    usingOtp = !usingOtp
                    submitted = false
                }
            )
        }

        InfoCard(Lucide.ShieldCheck, "Secured by SSL", "256-bit encryption on every sign-in", Modifier.padding(bottom = 24.dp))

        if (role == UserRole.Landlord) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Don’t have a landlord account?", style = RfType.BodyMd, color = Rf.OnSurfaceVariant)
                TextLink("Register here", onClick = onRegister, bold = true, modifier = Modifier.padding(start = 4.dp))
            }
        } else {
            Text(
                text = "Invited by your landlord? Sign in with the email they added you with.",
                style = RfType.BodyMd,
                color = Rf.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
