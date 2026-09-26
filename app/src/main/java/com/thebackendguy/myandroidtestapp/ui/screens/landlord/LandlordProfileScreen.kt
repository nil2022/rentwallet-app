package com.thebackendguy.myandroidtestapp.ui.screens.landlord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebackendguy.myandroidtestapp.data.Demo
import com.thebackendguy.myandroidtestapp.data.PayStatus
import com.thebackendguy.myandroidtestapp.data.Tenant
import com.thebackendguy.myandroidtestapp.data.inr
import com.thebackendguy.myandroidtestapp.ui.components.AppPage
import com.thebackendguy.myandroidtestapp.ui.components.Caption
import com.thebackendguy.myandroidtestapp.ui.components.HealthTileSpec
import com.thebackendguy.myandroidtestapp.ui.components.HealthTiles
import com.thebackendguy.myandroidtestapp.ui.components.IconDetail
import com.thebackendguy.myandroidtestapp.ui.components.IconRows
import com.thebackendguy.myandroidtestapp.ui.components.IconTile
import com.thebackendguy.myandroidtestapp.ui.components.Pill
import com.thebackendguy.myandroidtestapp.ui.components.RfCard
import com.thebackendguy.myandroidtestapp.ui.components.SectionHead
import com.thebackendguy.myandroidtestapp.ui.components.TitledCard
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.screens.LogoutButton
import com.thebackendguy.myandroidtestapp.ui.screens.ProfileHeader
import com.thebackendguy.myandroidtestapp.ui.screens.Shell
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RfType

@Composable
fun LandlordProfileScreen(tenants: List<Tenant>, shell: Shell, onLogout: () -> Unit) {
    val paid = tenants.filter { it.status == PayStatus.Paid }
    val pending = tenants.filter { it.status != PayStatus.Paid }
    val percent = if (tenants.isEmpty()) 0 else paid.size * 100 / tenants.size

    AppPage(topBar = { shell.TopBar("Profile") }, bottomBar = { shell.BottomNav(2) }) {
        ProfileHeader("AS", Demo.LANDLORD_NAME, "Landlord • ${tenants.size} properties")

        TitledCard("Owner details") {
            IconRows(
                listOf(
                    IconDetail(Lucide.Smartphone, "Mobile", Demo.LANDLORD_PHONE),
                    IconDetail(Lucide.Mail, "Email", Demo.LANDLORD_EMAIL),
                    IconDetail(Lucide.Users, "Total tenants", tenants.size.toString()),
                    IconDetail(Lucide.Building2, "Active properties", tenants.size.toString())
                )
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHead("Portfolio health") { Caption("May cycle") }
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
