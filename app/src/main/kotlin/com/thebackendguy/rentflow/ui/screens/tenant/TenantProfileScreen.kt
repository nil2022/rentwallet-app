package com.thebackendguy.rentflow.ui.screens.tenant

import androidx.compose.runtime.Composable
import com.thebackendguy.rentflow.data.Demo
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.data.session.SessionUser
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.IconDetail
import com.thebackendguy.rentflow.ui.components.IconRows
import com.thebackendguy.rentflow.ui.components.TitledCard
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.LogoutButton
import com.thebackendguy.rentflow.ui.screens.ProfileHeader
import com.thebackendguy.rentflow.ui.screens.Shell

@Composable
fun TenantProfileScreen(user: SessionUser?, shell: Shell, onLogout: () -> Unit) {
    AppPage(topBar = { shell.TopBar("Profile") }, bottomBar = { shell.BottomNav(2) }) {
        ProfileHeader(user?.name.orEmpty(), user?.photo, "Tenant • ${Demo.PROPERTY}")
        TitledCard("Personal details") {
            IconRows(
                listOf(
                    IconDetail(Lucide.Smartphone, "Mobile", user?.mobileLabel ?: "Not added"),
                    IconDetail(Lucide.Mail, "Email", user?.email.orEmpty()),
                    IconDetail(Lucide.Building2, "Current property", Demo.PROPERTY),
                    IconDetail(Lucide.User, "Landlord", Demo.LANDLORD_NAME)
                )
            )
        }
        TitledCard("Rent and wallet") {
            IconRows(
                listOf(
                    IconDetail(Lucide.Wallet, "Monthly rent", inr(Demo.MONTHLY_RENT)),
                    IconDetail(Lucide.Landmark, "Security deposit", inr(Demo.DEPOSIT)),
                    IconDetail(Lucide.CalendarClock, "Rent due day", "10th of every month"),
                    IconDetail(Lucide.CreditCard, "Payment method", "UPI / Bank")
                )
            )
        }
        LogoutButton(onLogout)
    }
}
