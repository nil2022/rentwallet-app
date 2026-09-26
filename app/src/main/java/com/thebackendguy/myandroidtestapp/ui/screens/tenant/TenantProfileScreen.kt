package com.thebackendguy.myandroidtestapp.ui.screens.tenant

import androidx.compose.runtime.Composable
import com.thebackendguy.myandroidtestapp.data.Demo
import com.thebackendguy.myandroidtestapp.data.inr
import com.thebackendguy.myandroidtestapp.ui.components.AppPage
import com.thebackendguy.myandroidtestapp.ui.components.IconDetail
import com.thebackendguy.myandroidtestapp.ui.components.IconRows
import com.thebackendguy.myandroidtestapp.ui.components.TitledCard
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.screens.LogoutButton
import com.thebackendguy.myandroidtestapp.ui.screens.ProfileHeader
import com.thebackendguy.myandroidtestapp.ui.screens.Shell

@Composable
fun TenantProfileScreen(shell: Shell, onLogout: () -> Unit) {
    AppPage(topBar = { shell.TopBar("Profile") }, bottomBar = { shell.BottomNav(2) }) {
        ProfileHeader("RM", Demo.TENANT_NAME, "Tenant • ${Demo.PROPERTY}")
        TitledCard("Personal details") {
            IconRows(
                listOf(
                    IconDetail(Lucide.Smartphone, "Mobile", Demo.TENANT_PHONE),
                    IconDetail(Lucide.Mail, "Email", Demo.TENANT_EMAIL),
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
