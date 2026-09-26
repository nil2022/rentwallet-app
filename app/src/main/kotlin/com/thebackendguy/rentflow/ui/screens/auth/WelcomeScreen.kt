package com.thebackendguy.rentflow.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.thebackendguy.rentflow.data.UserRole
import com.thebackendguy.rentflow.ui.components.BRAND_NAME
import com.thebackendguy.rentflow.ui.components.BrandLockup
import com.thebackendguy.rentflow.ui.components.IconTile
import com.thebackendguy.rentflow.ui.components.pressable
import com.thebackendguy.rentflow.ui.components.softShadow
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.jakarta

/**
 * First screen, built from the web's phone landing page: navy header, gradient
 * hero and white role cards. Picking a role opens login with it selected.
 */
@Composable
fun WelcomeScreen(onChooseRole: (UserRole) -> Unit, onSignIn: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Rf.Slate50)) {
        // Header
        Column(Modifier.fillMaxWidth().background(Rf.Midnight).statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandLockup(color = Color.White)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Sign In",
                    style = jakarta(13.0, FontWeight.SemiBold),
                    color = Color.White,
                    modifier = Modifier
                        .pressable(onSignIn)
                        .border(1.dp, Rf.Slate700.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Rf.Slate800))
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // Hero
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Rf.Midnight, Rf.MidnightVia, Rf.Slate50)))
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
            ) {
                Text(
                    text = "Rent payments made clear",
                    style = jakarta(20.0, FontWeight.ExtraBold, lineHeight = 1.375, tracking = -0.025),
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "A simple rental wallet for tenants and landlords.",
                    style = jakarta(12.0, FontWeight.Normal, lineHeight = 1.625),
                    color = Rf.Slate300,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                HighlightChip()
            }

            // Role picker and trust card
            Column(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "CONTINUE AS", style = jakarta(10.0, FontWeight.Bold, tracking = 0.05), color = Rf.Primary)
                    RoleCard(Lucide.House, "Tenant", "Pay your rent easily and track history.") { onChooseRole(UserRole.Tenant) }
                }
                RoleCard(Lucide.Building2, "Landlord", "Collect payments and manage properties.") { onChooseRole(UserRole.Landlord) }
                SecurityCard()
                Column(
                    Modifier.fillMaxWidth().padding(top = 4.dp).navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("© 2026 $BRAND_NAME", style = jakarta(11.0, FontWeight.Medium), color = Rf.Slate600)
                    Text(
                        "Privacy Policy • Terms of Service",
                        style = jakarta(10.0, FontWeight.Normal),
                        color = Rf.Slate500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightChip() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Rf.Slate900.copy(alpha = 0.9f))
            .border(1.dp, Rf.Slate700.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Rf.Emerald500.copy(alpha = 0.15f))
                .border(1.dp, Rf.Emerald500.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            LIcon(Lucide.Wallet, size = 20.dp, tint = Rf.Emerald400)
        }
        Column {
            Text("RENT GOES STRAIGHT TO", style = jakarta(10.0, FontWeight.Bold, tracking = 0.05), color = Rf.Slate400)
            Text(
                text = buildAnnotatedString {
                    append("Landlord wallet ")
                    withStyle(jakarta(10.0, FontWeight.SemiBold).toSpanStyle().copy(color = Rf.Emerald400)) { append("(instant credit)") }
                },
                style = jakarta(16.0, FontWeight.Bold, tracking = -0.025),
                color = Color.White
            )
        }
    }
}

@Composable
private fun RoleCard(icon: LucideIcon, title: String, description: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(onClick, pressScale = 0.98f)
            .softShadow(shape, elevation = 3.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Rf.Slate200, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(icon, background = Rf.PrimaryFixed, tint = Rf.Primary, size = 40.dp, radius = 12.dp, iconSize = 20.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = jakarta(16.0, FontWeight.ExtraBold, tracking = -0.025), color = Rf.Slate900)
            Text(description, style = jakarta(11.0, FontWeight.Normal, lineHeight = 1.625), color = Rf.Slate600)
        }
        LIcon(Lucide.ChevronRight, size = 20.dp, tint = Rf.Slate400)
    }
}

@Composable
private fun SecurityCard() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Rf.Slate900).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LIcon(Lucide.Shield, size = 16.dp, tint = Rf.Emerald400)
            Text("SECURE BY DEFAULT", style = jakarta(11.0, FontWeight.Bold, tracking = 0.05), color = Rf.Emerald400)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrustTile(Lucide.Lock, Rf.Indigo400, "Secure payments", Modifier.weight(1f))
            TrustTile(Lucide.ShieldCheck, Rf.Emerald400, "Encrypted data", Modifier.weight(1f))
        }
    }
}

@Composable
private fun TrustTile(icon: LucideIcon, tint: Color, text: String, modifier: Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Rf.Slate800.copy(alpha = 0.8f))
            .border(1.dp, Rf.Slate700.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LIcon(icon, size = 14.dp, tint = tint)
        Text(text, style = jakarta(10.0, FontWeight.Normal), color = Rf.Slate300)
    }
}
