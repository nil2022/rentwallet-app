package com.thebackendguy.myandroidtestapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thebackendguy.myandroidtestapp.data.UserRole
import com.thebackendguy.myandroidtestapp.ui.components.Avatar
import com.thebackendguy.myandroidtestapp.ui.components.NavTab
import com.thebackendguy.myandroidtestapp.ui.components.Notice
import com.thebackendguy.myandroidtestapp.ui.components.RfBottomNav
import com.thebackendguy.myandroidtestapp.ui.components.RfCard
import com.thebackendguy.myandroidtestapp.ui.components.RfTopBar
import com.thebackendguy.myandroidtestapp.ui.components.TonalButton
import com.thebackendguy.myandroidtestapp.ui.icons.LIcon
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RfType

val TenantTabs = listOf(NavTab("Home", Lucide.House), NavTab("Payments", Lucide.History), NavTab("Profile", Lucide.User))
val LandlordTabs = listOf(
    NavTab("Overview", Lucide.LayoutDashboard),
    NavTab("Properties", Lucide.Building2),
    NavTab("Tenants", Lucide.Users),
    NavTab("Profile", Lucide.User)
)

/** Top bar and bottom tabs for one signed-in role, so every screen builds them the same way. */
class Shell(
    private val role: UserRole,
    private val initials: String,
    private val notices: List<Notice>,
    private val onMenu: () -> Unit,
    private val onProfile: () -> Unit,
    private val onTab: (Int) -> Unit
) {
    @Composable
    fun TopBar(title: String, onBack: (() -> Unit)? = null, chip: String? = null) {
        RfTopBar(
            title = title,
            initials = initials,
            onMenu = onMenu,
            onBack = onBack,
            chip = chip,
            notices = notices,
            onProfile = onProfile
        )
    }

    @Composable
    fun BottomNav(selected: Int) {
        RfBottomNav(tabs = if (role == UserRole.Tenant) TenantTabs else LandlordTabs, selected = selected, onSelect = onTab)
    }
}

@Composable
fun ProfileHeader(initials: String, name: String, sub: String) {
    RfCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Avatar(initials, background = Rf.PrimaryFixed, content = Rf.Primary, size = 64.dp, style = RfType.HeadlineMd.copy(fontSize = RfType.HeadlineMd.fontSize), online = true)
            Column(Modifier.weight(1f)) {
                Text(name, style = RfType.HeadlineSm, color = Rf.OnSurface)
                Text(sub, style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
            }
            LIcon(Lucide.ChevronRight, tint = Rf.Outline)
        }
    }
}

@Composable
fun LogoutButton(onClick: () -> Unit) {
    TonalButton("Log out", onClick = onClick, background = Rf.ErrorContainer, content = Rf.Error, icon = Lucide.LogOut)
}
