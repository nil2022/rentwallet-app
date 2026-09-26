package com.thebackendguy.myandroidtestapp.ui.screens.landlord

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thebackendguy.myandroidtestapp.data.PayStatus
import com.thebackendguy.myandroidtestapp.data.Tenant
import com.thebackendguy.myandroidtestapp.data.inr
import com.thebackendguy.myandroidtestapp.ui.components.ActionButton
import com.thebackendguy.myandroidtestapp.ui.components.AlertCard
import com.thebackendguy.myandroidtestapp.ui.components.AppPage
import com.thebackendguy.myandroidtestapp.ui.components.Avatar
import com.thebackendguy.myandroidtestapp.ui.components.Caption
import com.thebackendguy.myandroidtestapp.ui.components.IconDetail
import com.thebackendguy.myandroidtestapp.ui.components.IconRows
import com.thebackendguy.myandroidtestapp.ui.components.InfoCard
import com.thebackendguy.myandroidtestapp.ui.components.Pill
import com.thebackendguy.myandroidtestapp.ui.components.RfCard
import com.thebackendguy.myandroidtestapp.ui.components.SectionHead
import com.thebackendguy.myandroidtestapp.ui.components.Segmented
import com.thebackendguy.myandroidtestapp.ui.components.TitledCard
import com.thebackendguy.myandroidtestapp.ui.components.softShadow
import com.thebackendguy.myandroidtestapp.ui.icons.LIcon
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.screens.Shell
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RfType

@Composable
fun TenantsScreen(tenants: List<Tenant>, shell: Shell, onOpenTenant: (Int) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val paidCount = tenants.count { it.status == PayStatus.Paid }

    AppPage(
        topBar = { shell.TopBar("Tenants", chip = "${tenants.size} properties") },
        bottomBar = { shell.BottomNav(1) }
    ) {
        SearchBox(query) { query = it }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHead("Tenant alerts") { Caption("2 open") }
            AlertCard(Lucide.TriangleAlert, "Follow up needed", "Two tenants have upcoming or overdue rent.", Rf.ErrorContainer, Rf.Error)
            AlertCard(Lucide.Banknote, "Withdrawal processing", "Bank transfer of ₹15,000 is on its way.", Rf.AmberSoft, Rf.Amber)
        }

        Column {
            SectionHead("All tenants") { Caption("May rent") }
            Segmented(
                options = listOf("All (${tenants.size})", "Paid ($paidCount)", "Pending (${tenants.size - paidCount})"),
                selected = filter,
                onSelect = { filter = it },
                modifier = Modifier.padding(top = 12.dp)
            )
            val shown = tenants.withIndex().filter { (_, t) ->
                (filter == 0 || (filter == 1) == (t.status == PayStatus.Paid)) &&
                    (query.isBlank() || t.name.contains(query, ignoreCase = true) || t.property.contains(query, ignoreCase = true))
            }
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (shown.isEmpty()) {
                    Text("No tenants match “$query”.", style = RfType.BodyMd, color = Rf.OnSurfaceVariant, modifier = Modifier.padding(4.dp))
                }
                shown.forEach { (index, tenant) -> TenantCard(tenant) { onOpenTenant(index) } }
            }
        }
    }
}

@Composable
private fun SearchBox(query: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().height(48.dp).softShadow(shape).clip(shape).background(Rf.Lowest).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LIcon(Lucide.Search, tint = Rf.Outline, strokeWidth = 1.75f)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text("Search tenant or property", style = RfType.BodyMd, color = Rf.Outline.copy(alpha = 0.8f))
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = RfType.BodyMd.copy(color = Rf.OnSurface, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(Rf.Primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TenantCard(tenant: Tenant, onClick: () -> Unit) {
    val paid = tenant.status == PayStatus.Paid
    val tint = if (paid) Rf.Secondary else Rf.Error
    val soft = if (paid) Rf.MintSoft else Rf.ErrorContainer
    RfCard(padding = PaddingValues(14.dp), spacing = 10.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(tenant.initials, background = soft, content = tint, size = 40.dp)
                Column {
                    Text(tenant.name, style = RfType.BodyLg.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${tenant.property} • ${tenant.area}", style = RfType.LabelSm, color = Rf.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Pill(if (paid) "Paid" else "Pending", background = soft, content = tint, dot = true)
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Rf.Low).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Monthly rent", style = RfType.LabelSm, color = Rf.Outline)
                Text(inr(tenant.rent), style = RfType.HeadlineSm.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface)
            }
            Text(tenant.dateLabel, style = RfType.LabelMd, color = tint)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                LIcon(Lucide.Phone, size = 14.dp, tint = Rf.Primary)
                Text(tenant.phone, style = RfType.LabelSm, color = Rf.Outline)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("View details", style = RfType.LabelSm, color = Rf.Primary)
                LIcon(Lucide.ChevronRight, size = 14.dp, tint = Rf.Primary)
            }
        }
    }
}

@Composable
fun PropertyDetailsScreen(tenant: Tenant, shell: Shell, onBack: () -> Unit) {
    val paid = tenant.status == PayStatus.Paid
    val context = LocalContext.current

    AppPage(topBar = { shell.TopBar(tenant.property, onBack = onBack) }, bottomBar = { shell.BottomNav(1) }) {
        RfCard(padding = PaddingValues(0.dp)) {
            Box(Modifier.fillMaxWidth().height(144.dp).background(Rf.Container)) {
                LIcon(Lucide.Building2, size = 36.dp, tint = Rf.Primary, modifier = Modifier.align(Alignment.Center))
                Pill(
                    "Occupied", background = Rf.InverseSurface.copy(alpha = 0.8f), content = Rf.InverseOnSurface, dot = true, dotColor = Rf.Mint,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Rf.InverseSurface.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Monthly rent", style = RfType.LabelSm, color = Rf.Highest.copy(alpha = 0.8f))
                    Text(inr(tenant.rent), style = RfType.HeadlineSm.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
            }
            Column(Modifier.padding(14.dp)) {
                Text(tenant.property, style = RfType.BodyLg.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                Text(tenant.address, style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
            }
        }

        if (paid) {
            InfoCard(
                Lucide.BadgeCheck, "Wallet credited", "${inr(tenant.rent)} from ${tenant.name} was credited on ${tenant.shortDate}.",
                iconColor = Rf.Secondary, background = Rf.MintWash, titleColor = Rf.OnSecondaryContainer
            )
        } else {
            InfoCard(
                Lucide.TriangleAlert, "Wallet credit pending", "${tenant.name}’s ${inr(tenant.rent)} rent is due on ${tenant.shortDate}.",
                iconColor = Rf.Amber, background = Rf.AmberSoft, titleColor = Rf.Amber
            )
        }

        TitledCard("Tenant details") {
            IconRows(
                listOf(
                    IconDetail(Lucide.User, "Tenant", tenant.name),
                    IconDetail(Lucide.Phone, "Contact", tenant.phone),
                    IconDetail(Lucide.Landmark, "Security deposit", inr(tenant.deposit)),
                    IconDetail(Lucide.CalendarClock, "Rent due day", tenant.dueDay),
                    IconDetail(Lucide.CircleCheck, "Payment status", if (paid) "Paid" else "Pending")
                )
            )
        }
        TitledCard("Lease and wallet") {
            IconRows(
                listOf(
                    IconDetail(Lucide.FileText, "Lease", "${tenant.leaseStart} – ${tenant.leaseEnd}"),
                    IconDetail(Lucide.Wallet, "Wallet status", if (paid) "Credited" else "Pending"),
                    IconDetail(Lucide.History, "Last payment", tenant.lastPayment),
                    IconDetail(Lucide.CheckCheck, "Next action", if (paid) "No action needed" else "Follow up with tenant")
                )
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Call tenant", Lucide.Phone, Rf.Container, Rf.Primary, Modifier.weight(1f)) {
                val number = tenant.phone.filter { it.isDigit() || it == '+' }
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
            }
            ActionButton("Send reminder", Lucide.Send, Rf.Secondary, Color.White, Modifier.weight(1f), doneLabel = "Reminder sent")
        }
    }
}
