package com.thebackendguy.rentflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thebackendguy.rentflow.data.UserRole
import com.thebackendguy.rentflow.ui.components.NavTab
import com.thebackendguy.rentflow.ui.components.PersonAvatar
import com.thebackendguy.rentflow.ui.components.RfBottomNav
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.RfTopBar
import com.thebackendguy.rentflow.ui.components.TonalButton
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType

val TenantTabs = listOf(NavTab("Home", Lucide.House), NavTab("Payments", Lucide.History), NavTab("Profile", Lucide.User))
/** The web's phone bottom bar; Profile opens from the photo and the menu. */
val LandlordTabs = listOf(
    NavTab("Overview", Lucide.LayoutDashboard),
    NavTab("Properties", Lucide.Building2),
    NavTab("Rooms", Lucide.DoorOpen),
    NavTab("Tenants", Lucide.Users),
    NavTab("Payments", Lucide.WalletCards)
)

/**
 * The web's sidebar, in its order and words. The first five open the tabs above
 * (Rent Collection is the Payments tab); the rest have no screen in the app yet,
 * so (like the web's placeholders) they do nothing.
 */
val LandlordMenu = listOf(
    NavTab("Dashboard", Lucide.LayoutDashboard),
    NavTab("Properties", Lucide.Building2),
    NavTab("Rooms", Lucide.DoorOpen),
    NavTab("Tenants", Lucide.Users),
    NavTab("Rent Collection", Lucide.WalletCards),
    NavTab("Rent History", Lucide.History, placeholder = true),
    NavTab("Rent Settings", Lucide.SlidersHorizontal, placeholder = true),
    NavTab("Expenses", Lucide.Receipt, placeholder = true),
    NavTab("Reports", Lucide.ChartColumn, placeholder = true),
    NavTab("Settings", Lucide.Settings, placeholder = true)
)

/** Top bar and bottom tabs for one signed-in role, so every screen builds them the same way. */
class Shell(
    private val role: UserRole,
    private val name: String,
    private val photo: String?,
    private val unread: Int,
    private val onMenu: () -> Unit,
    private val onProfile: () -> Unit,
    private val onTab: (Int) -> Unit
) {
    @Composable
    fun TopBar(title: String, onBack: (() -> Unit)? = null, chip: String? = null) {
        var showNotifications by remember { mutableStateOf(false) }
        RfTopBar(
            title = title,
            name = name,
            photo = photo,
            onMenu = onMenu,
            onBack = onBack,
            chip = chip,
            unread = unread,
            onNotifications = { showNotifications = true },
            onProfile = onProfile
        )
        if (showNotifications) NotificationsSheet(role, onDismiss = { showNotifications = false })
    }

    /** [selected] is the tab to highlight, or -1 for none (Profile is not a tab). */
    @Composable
    fun BottomNav(selected: Int) {
        RfBottomNav(tabs = if (role == UserRole.Tenant) TenantTabs else LandlordTabs, selected = selected, onSelect = onTab)
    }
}

@Composable
fun ProfileHeader(name: String, photo: String?, sub: String) {
    RfCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PersonAvatar(name, photo, size = 64.dp, online = true)
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
