package com.thebackendguy.rentflow.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.thebackendguy.rentflow.data.AuthRepository
import com.thebackendguy.rentflow.data.UserRole
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.ui.components.AuthShell
import com.thebackendguy.rentflow.ui.components.Dot
import com.thebackendguy.rentflow.ui.components.InfoCard
import com.thebackendguy.rentflow.ui.components.OtpBoxes
import com.thebackendguy.rentflow.ui.components.PrimaryButton
import com.thebackendguy.rentflow.ui.components.RfTextField
import com.thebackendguy.rentflow.ui.components.Spinner
import com.thebackendguy.rentflow.ui.components.TextLink
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlinx.coroutines.launch

/**
 * Landlord sign-up: the web's phone registration form, then its email
 * verification step. Tenants are added by their landlord, so they don't register.
 *
 * Same order as the web: create the account, email a code, verify it, then
 * sign in with the password from the form.
 */
@Composable
fun RegisterScreen(onBack: () -> Unit, onLogin: () -> Unit, onRegistered: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val timer = rememberCountdown()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var resending by remember { mutableStateOf(false) }
    var serverError by rememberSaveable { mutableStateOf<String?>(null) }
    var resumeNote by rememberSaveable { mutableStateOf<String?>(null) }
    // The details the account was created with, so going back and on doesn't create it twice
    var createdWith by rememberSaveable { mutableStateOf<String?>(null) }

    val backToForm: () -> Unit = {
        step = 0
        serverError = null
    }
    BackHandler(enabled = step == 1) { backToForm() }

    fun codeSent(note: String?) {
        submitted = false
        code = ""
        timer.intValue = 60
        resumeNote = note
        serverError = null
        step = 1
    }

    AuthShell(onBack = { if (step == 1) backToForm() else onBack() }, error = serverError, onDismissError = { serverError = null }) {
        if (step == 0) {
            val nameErr = if (submitted && name.isBlank()) "Full name is required" else null
            val phoneValid = phone.length == 10 && phone.first() in '6'..'9'
            val phoneErr = if (submitted && !phoneValid) "Enter a valid 10-digit mobile number" else null
            val next: () -> Unit = {
                submitted = true
                if (name.isNotBlank() && emailError(email) == null && phoneValid && newPasswordError(password) == null && !busy) {
                    busy = true
                    serverError = null
                    val details = listOf(name.trim(), email.lowercase(), phone, password).joinToString("\n")
                    scope.launch {
                        if (createdWith != details) {
                            when (val created = AuthRepository.registerLandlord(name, email, phone, password)) {
                                is ApiResult.Ok -> createdWith = details
                                is ApiResult.Fail -> {
                                    if (ALREADY_TAKEN.containsMatchIn(created.message)) {
                                        // An unfinished sign-up may own this email: send it a new code
                                        when (val resumed = AuthRepository.sendSignupCode(email)) {
                                            is ApiResult.Ok -> {
                                                createdWith = details
                                                codeSent("This email already has an unverified account, so we sent it a new code.")
                                            }
                                            is ApiResult.Fail -> serverError =
                                                if (resumed.message.contains("not found", ignoreCase = true)) created.message else resumed.message
                                        }
                                    } else {
                                        serverError = created.message
                                    }
                                    busy = false
                                    return@launch
                                }
                            }
                        }
                        when (val sent = AuthRepository.sendSignupCode(email)) {
                            is ApiResult.Ok -> codeSent(null)
                            is ApiResult.Fail -> serverError = sent.message
                        }
                        busy = false
                    }
                }
            }

            Text("Landlord Registration", style = RfType.HeadlineLg, color = Rf.OnSurface)
            Text(
                "Create your landlord account to manage properties and tenants.",
                style = RfType.BodyMd, color = Rf.OnSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )
            InfoCard(
                Lucide.Info, "Only property owners can register", "You’ll be able to add your tenants after registration.",
                Modifier.padding(bottom = 24.dp), iconColor = Rf.Primary
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RfTextField(
                    label = "Full Name", value = name, onValueChange = { name = it }, icon = Lucide.User,
                    placeholder = "Enter your full name", capitalization = KeyboardCapitalization.Words, error = nameErr
                )
                RfTextField(
                    label = "Email Address", value = email, onValueChange = { email = it.trim() }, icon = Lucide.Mail,
                    placeholder = "name@company.com", keyboardType = KeyboardType.Email,
                    error = if (submitted) emailError(email) else null
                )
                // Only Indian mobiles are accepted, so +91 is fixed and the keypad is numeric
                RfTextField(
                    label = "Phone Number", value = phone, onValueChange = { phone = it.filter(Char::isDigit).take(10) },
                    icon = Lucide.Phone, placeholder = "98765 43210", prefix = "+91", keyboardType = KeyboardType.Number,
                    error = phoneErr
                )
                RfTextField(
                    label = "Password", value = password, onValueChange = { password = it }, icon = Lucide.Lock,
                    placeholder = "Create a password", isPassword = true, imeAction = ImeAction.Go, onImeAction = next,
                    hint = PASSWORD_RULE, error = if (submitted) newPasswordError(password) else null
                )
                PrimaryButton(
                    if (busy) "Creating account…" else "Continue to Verification",
                    onClick = next, loading = busy, modifier = Modifier.padding(top = 8.dp)
                )
            }
            Text(
                text = buildAnnotatedString {
                    append("By registering, you agree to our ")
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append("Terms of Service") }
                    append(" & ")
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append("Privacy Policy") }
                },
                style = RfType.BodySm, color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp)
            )
            InfoCard(Lucide.ShieldCheck, "Secured by SSL", "256-bit encryption on every sign-in", Modifier.padding(bottom = 24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account?", style = RfType.BodyMd, color = Rf.OnSurfaceVariant)
                TextLink("Login here", onClick = onLogin, bold = true, modifier = Modifier.padding(start = 4.dp))
            }
        } else {
            val complete = code.length == 6
            val verify: () -> Unit = {
                if (complete && !busy) {
                    busy = true
                    serverError = null
                    scope.launch {
                        when (val verified = AuthRepository.verifySignupCode(email, code)) {
                            is ApiResult.Fail -> serverError = verified.message
                            is ApiResult.Ok -> {
                                // Verified. Sign in with the form's password; if that fails, the login screen takes over
                                val signedIn = AuthRepository.login(UserRole.Landlord, email, password, remember = true)
                                if (signedIn is ApiResult.Ok) onRegistered() else onLogin()
                            }
                        }
                        busy = false
                    }
                }
            }
            val resend: () -> Unit = {
                if (!busy && !resending) {
                    resending = true
                    serverError = null
                    scope.launch {
                        when (val sent = AuthRepository.sendSignupCode(email)) {
                            is ApiResult.Ok -> {
                                code = ""
                                timer.intValue = 60
                            }
                            is ApiResult.Fail -> serverError = sent.message
                        }
                        resending = false
                    }
                }
            }
            HeadIcon(Lucide.MailCheck)
            Text("Verify Your Email", style = RfType.HeadlineLg, color = Rf.OnSurface)
            Text(
                "We have sent a 6-digit security verification code to:",
                style = RfType.BodyMd, color = Rf.OnSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Rf.Low).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LIcon(Lucide.AtSign, size = 15.dp, tint = Rf.Primary)
                Text(email, style = RfType.BodySm.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold), color = Rf.OnSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(
                    "Change Email",
                    style = RfType.LabelMd.copy(textDecoration = TextDecoration.Underline),
                    color = Rf.Primary,
                    modifier = Modifier.pressable(backToForm)
                )
            }
            resumeNote?.let {
                InfoCard(Lucide.Info, "Welcome back", it, Modifier.padding(top = 16.dp), iconColor = Rf.Primary)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ENTER 6-DIGIT SECURITY PIN", style = RfType.LabelSm.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em), color = Rf.OnSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Dot(Rf.PinReady)
                    Text(
                        if (complete) "Ready to verify" else "Awaiting PIN",
                        style = RfType.LabelSm.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                        color = Rf.PinReady
                    )
                }
            }
            OtpBoxes(code = code, onCodeChange = { code = it })
            Row(
                Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Didn’t receive the code?", style = RfType.BodySm.copy(fontSize = 12.sp), color = Rf.OnSurfaceVariant)
                val waiting = timer.intValue > 0
                Row(
                    Modifier
                        .pressable(if (waiting || busy || resending) null else resend)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Rf.Low)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tint = if (waiting) Rf.Outline else Rf.Primary
                    if (resending) Spinner(tint, size = 14.dp, strokeWidth = 1.5.dp) else LIcon(Lucide.Clock3, size = 14.dp, tint = tint)
                    Text(
                        when {
                            resending -> "Sending code…"
                            waiting -> "Resend Code in 00:%02ds".format(timer.intValue)
                            else -> "Resend Code"
                        },
                        style = RfType.LabelSm.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                        color = tint
                    )
                }
            }
            PrimaryButton(
                if (busy) "Verifying…" else "Verify Code & Go to Dashboard",
                onClick = verify,
                enabled = complete,
                loading = busy,
                modifier = Modifier.padding(top = 24.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LIcon(Lucide.Lock, size = 12.dp, tint = Rf.OnSurfaceVariant)
                Text("Secured by 256-bit SSL • Single-use OTP expires in 5 minutes", style = RfType.LabelSm.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.sp), color = Rf.OnSurfaceVariant)
            }
        }
    }
}

/** How the API words a sign-up for an email or mobile that already has an account. */
private val ALREADY_TAKEN = Regex("already|exists|registered", RegexOption.IGNORE_CASE)
