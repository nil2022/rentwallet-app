package com.thebackendguy.myandroidtestapp.ui.screens.auth

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
import com.thebackendguy.myandroidtestapp.ui.components.AuthShell
import com.thebackendguy.myandroidtestapp.ui.components.Dot
import com.thebackendguy.myandroidtestapp.ui.components.InfoCard
import com.thebackendguy.myandroidtestapp.ui.components.OtpBoxes
import com.thebackendguy.myandroidtestapp.ui.components.PrimaryButton
import com.thebackendguy.myandroidtestapp.ui.components.RfTextField
import com.thebackendguy.myandroidtestapp.ui.components.TextLink
import com.thebackendguy.myandroidtestapp.ui.components.pressable
import com.thebackendguy.myandroidtestapp.ui.icons.LIcon
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RfType

/**
 * Landlord sign-up: the web's phone registration form, then its email
 * verification step. Tenants are added by their landlord, so they don't register.
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

    BackHandler(enabled = step == 1) { step = 0 }

    AuthShell(onBack = { if (step == 1) step = 0 else onBack() }) {
        if (step == 0) {
            val nameErr = if (submitted && name.isBlank()) "Full name is required" else null
            val phoneErr = if (submitted && phone.length != 10) "Enter a 10-digit mobile number" else null
            val next: () -> Unit = {
                submitted = true
                if (name.isNotBlank() && emailError(email) == null && phone.length == 10 && newPasswordError(password) == null) {
                    submitted = false
                    code = ""
                    timer.intValue = 60
                    step = 1
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
                PrimaryButton("Continue to Verification", onClick = next, modifier = Modifier.padding(top = 8.dp))
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
                    modifier = Modifier.pressable({ step = 0 })
                )
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
                        .pressable(if (waiting) null else ({ code = ""; timer.intValue = 60 }))
                        .clip(RoundedCornerShape(10.dp))
                        .background(Rf.Low)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tint = if (waiting) Rf.Outline else Rf.Primary
                    LIcon(Lucide.Clock3, size = 14.dp, tint = tint)
                    Text(
                        if (waiting) "Resend Code in 00:%02ds".format(timer.intValue) else "Resend Code",
                        style = RfType.LabelSm.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                        color = tint
                    )
                }
            }
            PrimaryButton(
                "Verify Code & Go to Dashboard",
                onClick = onRegistered,
                enabled = complete,
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
