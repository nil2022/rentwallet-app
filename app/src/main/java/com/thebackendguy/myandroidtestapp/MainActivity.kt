package com.thebackendguy.myandroidtestapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thebackendguy.myandroidtestapp.data.AuthRepository
import com.thebackendguy.myandroidtestapp.data.Demo
import com.thebackendguy.myandroidtestapp.data.UserRole
import com.thebackendguy.myandroidtestapp.data.session.SessionStore
import com.thebackendguy.myandroidtestapp.data.settled
import com.thebackendguy.myandroidtestapp.ui.components.BrandLockup
import com.thebackendguy.myandroidtestapp.ui.components.Notice
import com.thebackendguy.myandroidtestapp.ui.components.StatusBarIcons
import com.thebackendguy.myandroidtestapp.ui.icons.LIcon
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.screens.LandlordTabs
import com.thebackendguy.myandroidtestapp.ui.screens.Shell
import com.thebackendguy.myandroidtestapp.ui.screens.TenantTabs
import com.thebackendguy.myandroidtestapp.ui.screens.auth.ForgotPasswordScreen
import com.thebackendguy.myandroidtestapp.ui.screens.auth.LoginScreen
import com.thebackendguy.myandroidtestapp.ui.screens.auth.RegisterScreen
import com.thebackendguy.myandroidtestapp.ui.screens.auth.WelcomeScreen
import com.thebackendguy.myandroidtestapp.ui.screens.landlord.LandlordOverviewScreen
import com.thebackendguy.myandroidtestapp.ui.screens.landlord.LandlordProfileScreen
import com.thebackendguy.myandroidtestapp.ui.screens.landlord.PropertyDetailsScreen
import com.thebackendguy.myandroidtestapp.ui.screens.landlord.TenantsScreen
import com.thebackendguy.myandroidtestapp.ui.screens.tenant.PayRentScreen
import com.thebackendguy.myandroidtestapp.ui.screens.tenant.PaymentHistoryScreen
import com.thebackendguy.myandroidtestapp.ui.screens.tenant.PaymentSuccessScreen
import com.thebackendguy.myandroidtestapp.ui.screens.tenant.ReceiptScreen
import com.thebackendguy.myandroidtestapp.ui.screens.tenant.TenantHomeScreen
import com.thebackendguy.myandroidtestapp.ui.screens.tenant.TenantProfileScreen
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RentFlowTheme
import com.thebackendguy.myandroidtestapp.ui.theme.RfType
import kotlinx.coroutines.launch

private enum class Screen {
    Welcome, Login, ForgotPassword, Register,
    TenantHome, PayRent, PaymentSuccess, PaymentHistory, Receipt, TenantProfile,
    LandlordOverview, Tenants, PropertyDetails, LandlordProfile
}

private val TenantTabScreens = listOf(Screen.TenantHome, Screen.PaymentHistory, Screen.TenantProfile)
private val LandlordTabScreens = listOf(Screen.LandlordOverview, Screen.Tenants, Screen.LandlordProfile)

private val TenantNotices = listOf(
    Notice("Rent due reminder", "May rent is due on 10 May."),
    Notice("Receipt generated", "Your April rent receipt is ready.")
)
private val LandlordNotices = listOf(
    Notice("Rent received", "Rohan’s May rent was credited to your wallet."),
    Notice("Pending rent", "Arjun and Neha still have May rent pending.")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionStore.init(applicationContext)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            RentFlowTheme {
                RentFlowApp()
            }
        }
    }
}

private fun homeFor(role: UserRole) = if (role == UserRole.Tenant) Screen.TenantHome else Screen.LandlordOverview

@Composable
private fun RentFlowApp() {
    val session by SessionStore.session.collectAsState()
    val endedByServer by SessionStore.endedByServer.collectAsState()
    // A saved session opens straight on its home screen
    val saved = remember { SessionStore.session.value }
    var screen by rememberSaveable { mutableStateOf(saved?.let { homeFor(it.user.role) } ?: Screen.Welcome) }
    var role by rememberSaveable { mutableStateOf(saved?.user?.role ?: UserRole.Tenant) }
    var payIndex by rememberSaveable { mutableIntStateOf(0) }
    var receiptIndex by rememberSaveable { mutableIntStateOf(0) }
    var receiptBack by rememberSaveable { mutableStateOf(Screen.PaymentHistory) }
    var tenantIndex by rememberSaveable { mutableIntStateOf(0) }
    var propertyBack by rememberSaveable { mutableStateOf(Screen.Tenants) }
    // Payments the tenant has made in this session, by index into the demo list
    var paidIndexes by rememberSaveable { mutableStateOf(listOf<Int>()) }

    val payments = Demo.tenantPayments.mapIndexed { i, p -> if (i in paidIndexes) p.settled() else p }
    val tenants = Demo.tenants
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun signOut() {
        paidIndexes = emptyList()
        screen = Screen.Welcome
        scope.launch { AuthRepository.logout() }
    }

    fun openReceipt(index: Int, from: Screen) {
        receiptIndex = index
        receiptBack = from
        screen = Screen.Receipt
    }

    fun openTenant(index: Int, from: Screen) {
        tenantIndex = index
        propertyBack = from
        screen = Screen.PropertyDetails
    }

    val parent: Screen? = when (screen) {
        Screen.Login -> Screen.Welcome
        Screen.ForgotPassword, Screen.Register -> Screen.Login
        Screen.PayRent, Screen.PaymentSuccess, Screen.PaymentHistory, Screen.TenantProfile -> Screen.TenantHome
        Screen.Receipt -> receiptBack
        Screen.Tenants, Screen.LandlordProfile -> Screen.LandlordOverview
        Screen.PropertyDetails -> propertyBack
        // Home screens and Welcome leave the app
        else -> null
    }
    BackHandler(enabled = drawerState.isOpen || parent != null) {
        if (drawerState.isOpen) scope.launch { drawerState.close() } else parent?.let { screen = it }
    }

    StatusBarIcons(darkIcons = screen != Screen.Welcome)

    val signedIn = screen in TenantTabScreens || screen in LandlordTabScreens ||
        screen in listOf(Screen.PayRent, Screen.PaymentSuccess, Screen.Receipt, Screen.PropertyDetails)

    // Check the saved token and refresh the name and contact details
    LaunchedEffect(Unit) { AuthRepository.refreshProfile() }

    // The session ended (token rejected, or the app restarted without "Remember me")
    LaunchedEffect(session == null, signedIn) {
        if (session == null && signedIn) {
            drawerState.close()
            paidIndexes = emptyList()
            screen = Screen.Login
        }
    }

    val user = session?.user
    val tabScreens = if (role == UserRole.Tenant) TenantTabScreens else LandlordTabScreens
    val shell = Shell(
        role = role,
        initials = user?.initials ?: "",
        notices = if (role == UserRole.Tenant) TenantNotices else LandlordNotices,
        onMenu = { scope.launch { drawerState.open() } },
        onProfile = { screen = tabScreens.last() },
        onTab = { screen = tabScreens[it] }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = signedIn && drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Rf.Lowest) {
                Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 16.dp)) {
                    BrandLockup(color = Rf.OnSurface, modifier = Modifier.padding(start = 12.dp, bottom = 4.dp))
                    Text(
                        text = "${user?.name.orEmpty()} • ${role.name}",
                        style = RfType.LabelSm,
                        color = Rf.OnSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, bottom = 16.dp)
                    )
                    val tabs = if (role == UserRole.Tenant) TenantTabs else LandlordTabs
                    tabs.forEachIndexed { index, tab ->
                        NavigationDrawerItem(
                            label = { Text(tab.label, style = RfType.LabelMd) },
                            icon = { LIcon(tab.icon) },
                            selected = screen == tabScreens[index],
                            onClick = {
                                screen = tabScreens[index]
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Rf.PrimaryFixed,
                                selectedTextColor = Rf.Primary,
                                selectedIconColor = Rf.Primary,
                                unselectedTextColor = Rf.OnSurface,
                                unselectedIconColor = Rf.OnSurfaceVariant
                            )
                        )
                    }
                    HorizontalDivider(color = Rf.Low, modifier = Modifier.padding(vertical = 12.dp))
                    NavigationDrawerItem(
                        label = { Text("Log out", style = RfType.LabelMd) },
                        icon = { LIcon(Lucide.LogOut) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            signOut()
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedTextColor = Rf.Error, unselectedIconColor = Rf.Error)
                    )
                }
            }
        }
    ) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "screen"
        ) { current ->
            when (current) {
                Screen.Welcome -> WelcomeScreen(
                    onChooseRole = {
                        role = it
                        screen = Screen.Login
                    },
                    onSignIn = { screen = Screen.Login }
                )

                Screen.Login -> LoginScreen(
                    initialRole = role,
                    notice = if (endedByServer) "Your session has ended. Please sign in again." else null,
                    onBack = { screen = Screen.Welcome },
                    onSignedIn = {
                        role = it
                        screen = if (it == UserRole.Tenant) Screen.TenantHome else Screen.LandlordOverview
                    },
                    onForgotPassword = {
                        role = it
                        screen = Screen.ForgotPassword
                    },
                    onRegister = {
                        role = UserRole.Landlord
                        screen = Screen.Register
                    }
                )

                Screen.ForgotPassword -> ForgotPasswordScreen(
                    role = role,
                    onBack = { screen = Screen.Login },
                    onBackToLogin = { screen = Screen.Login }
                )

                Screen.Register -> RegisterScreen(
                    onBack = { screen = Screen.Login },
                    onLogin = { screen = Screen.Login },
                    onRegistered = {
                        role = UserRole.Landlord
                        screen = Screen.LandlordOverview
                    }
                )

                Screen.TenantHome -> TenantHomeScreen(
                    firstName = user?.firstName.orEmpty(),
                    payments = payments,
                    shell = shell,
                    onPayRent = {
                        payIndex = 0
                        screen = Screen.PayRent
                    },
                    onOpenReceipt = { openReceipt(it, Screen.TenantHome) },
                    onOpenHistory = { screen = Screen.PaymentHistory },
                    onOpenProfile = { screen = Screen.TenantProfile }
                )

                Screen.PayRent -> PayRentScreen(
                    payment = payments[payIndex],
                    shell = shell,
                    onBack = { screen = Screen.TenantHome },
                    onConfirm = {
                        paidIndexes = (paidIndexes + payIndex).distinct()
                        screen = Screen.PaymentSuccess
                    }
                )

                Screen.PaymentSuccess -> PaymentSuccessScreen(
                    payment = payments[payIndex],
                    onBackToDashboard = { screen = Screen.TenantHome },
                    onViewReceipt = { openReceipt(payIndex, Screen.TenantHome) }
                )

                Screen.PaymentHistory -> PaymentHistoryScreen(
                    payments = payments,
                    shell = shell,
                    onOpenReceipt = { openReceipt(it, Screen.PaymentHistory) },
                    onPay = {
                        payIndex = it
                        screen = Screen.PayRent
                    }
                )

                Screen.Receipt -> ReceiptScreen(
                    payment = payments[receiptIndex],
                    shell = shell,
                    onBack = { screen = receiptBack }
                )

                Screen.TenantProfile -> TenantProfileScreen(user = user, shell = shell, onLogout = ::signOut)

                Screen.LandlordOverview -> LandlordOverviewScreen(
                    firstName = user?.firstName.orEmpty(),
                    tenants = tenants,
                    shell = shell,
                    onOpenTenants = { screen = Screen.Tenants },
                    onOpenTenant = { openTenant(it, Screen.LandlordOverview) }
                )

                Screen.Tenants -> TenantsScreen(
                    tenants = tenants,
                    shell = shell,
                    onOpenTenant = { openTenant(it, Screen.Tenants) }
                )

                Screen.PropertyDetails -> PropertyDetailsScreen(
                    tenant = tenants[tenantIndex],
                    shell = shell,
                    onBack = { screen = propertyBack }
                )

                Screen.LandlordProfile -> LandlordProfileScreen(user = user, tenants = tenants, shell = shell, onLogout = ::signOut)
            }
        }
    }
}
