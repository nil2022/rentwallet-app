package com.thebackendguy.rentflow.ui.screens.tenant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thebackendguy.rentflow.data.AuthRepository
import com.thebackendguy.rentflow.data.TenantStore
import com.thebackendguy.rentflow.data.session.SessionUser
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.IconDetail
import com.thebackendguy.rentflow.ui.components.IconRows
import com.thebackendguy.rentflow.ui.components.IconTile
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.TitledCard
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.LogoutButton
import com.thebackendguy.rentflow.ui.screens.ProfileHeader
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType

/**
 * The tenant's profile, read-only as on the web (their landlord manages it). The
 * profile API returns only ids for the room, so room and property come from the
 * newest bill; before the first bill they show a dash.
 */
@Composable
fun TenantProfileScreen(user: SessionUser?, shell: Shell, onChangePassword: () -> Unit, onLogout: () -> Unit) {
    val state = tenantBillsState()
    LaunchedEffect(Unit) {
        TenantStore.refresh()
        AuthRepository.refreshProfile()
    }
    val latest = state.data?.latest
    // Room and property pulse while the bills load; a dash before the first bill or if they can't load
    val loading = state.data == null && state.error == null
    val room = latest?.room?.roomNumber?.let { "Room $it" } ?: if (loading) null else "—"
    val property = latest?.property?.let { p -> listOf(p.propertyName, p.city.orEmpty()).filter(String::isNotBlank).joinToString(", ") }
        ?: if (loading) null else "—"

    AppPage(topBar = { shell.TopBar("Profile") }, bottomBar = { shell.BottomNav(2) }) {
        ProfileHeader(user?.name.orEmpty(), user?.photo, latest?.room?.roomNumber?.let { "Tenant • Room $it" } ?: "Tenant")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TitledCard("Your details") {
                IconRows(
                    listOf(
                        IconDetail(Lucide.Smartphone, "Mobile", user?.mobileLabel ?: "Not added"),
                        IconDetail(Lucide.Mail, "Email", user?.email?.takeIf(String::isNotBlank) ?: "Not added"),
                        IconDetail(Lucide.DoorOpen, "Room", room),
                        IconDetail(Lucide.Building2, "Property", property)
                    )
                )
            }
            Text(
                "Your name, email and phone are managed by your landlord." +
                    if (state.data != null && latest == null) " Your room shows here after your first bill." else "",
                style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        RfCard(onClick = onChangePassword) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(Lucide.KeyRound, background = Rf.Low, tint = Rf.Primary)
                Column(Modifier.weight(1f)) {
                    Text("Change password", style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                    Text("At least 6 characters", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
                }
                LIcon(Lucide.ChevronRight, tint = Rf.Outline)
            }
        }
        LogoutButton(onLogout)
    }
}
