package com.thebackendguy.rentflow

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.thebackendguy.rentflow.data.AuthRepository
import com.thebackendguy.rentflow.data.Demo
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.UserRole
import com.thebackendguy.rentflow.data.remote.Network
import com.thebackendguy.rentflow.data.session.SessionStore
import com.thebackendguy.rentflow.data.settled
import com.thebackendguy.rentflow.ui.components.BrandLockup
import com.thebackendguy.rentflow.ui.components.Notice
import com.thebackendguy.rentflow.ui.components.SystemBars
import com.thebackendguy.rentflow.ui.components.ToastHost
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.LandlordTabs
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.screens.TenantTabs
import com.thebackendguy.rentflow.ui.screens.auth.ForgotPasswordScreen
import com.thebackendguy.rentflow.ui.screens.auth.LoginScreen
import com.thebackendguy.rentflow.ui.screens.auth.RegisterScreen
import com.thebackendguy.rentflow.ui.screens.auth.WelcomeScreen
import com.thebackendguy.rentflow.ui.screens.landlord.AddTenantScreen
import com.thebackendguy.rentflow.ui.screens.landlord.AssignRoomScreen
import com.thebackendguy.rentflow.ui.screens.landlord.ChangePasswordScreen
import com.thebackendguy.rentflow.ui.screens.landlord.EditProfileScreen
import com.thebackendguy.rentflow.ui.screens.landlord.LandlordOverviewScreen
import com.thebackendguy.rentflow.ui.screens.landlord.LandlordProfileScreen
import com.thebackendguy.rentflow.ui.screens.landlord.PropertiesScreen
import com.thebackendguy.rentflow.ui.screens.landlord.PropertyDetailsScreen
import com.thebackendguy.rentflow.ui.screens.landlord.PropertyFormScreen
import com.thebackendguy.rentflow.ui.screens.landlord.RoomDetailsScreen
import com.thebackendguy.rentflow.ui.screens.landlord.RoomFormScreen
import com.thebackendguy.rentflow.ui.screens.landlord.TenantDetailsScreen
import com.thebackendguy.rentflow.ui.screens.landlord.TenantFormScreen
import com.thebackendguy.rentflow.ui.screens.landlord.TenantsScreen
import com.thebackendguy.rentflow.ui.screens.tenant.PayRentScreen
import com.thebackendguy.rentflow.ui.screens.tenant.PaymentHistoryScreen
import com.thebackendguy.rentflow.ui.screens.tenant.PaymentSuccessScreen
import com.thebackendguy.rentflow.ui.screens.tenant.ReceiptScreen
import com.thebackendguy.rentflow.ui.screens.tenant.TenantHomeScreen
import com.thebackendguy.rentflow.ui.screens.tenant.TenantProfileScreen
import com.thebackendguy.rentflow.ui.theme.DarkPalette
import com.thebackendguy.rentflow.ui.theme.LightPalette
import com.thebackendguy.rentflow.ui.theme.RentFlowTheme
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import com.thebackendguy.rentflow.ui.theme.ThemeMode
import kotlinx.coroutines.launch

private enum class Screen {
    Welcome, Login, ForgotPassword, Register,
    TenantHome, PayRent, PaymentSuccess, PaymentHistory, Receipt, TenantProfile,
    LandlordOverview, Properties, PropertyDetails, PropertyForm, RoomDetails, RoomForm,
    Tenants, TenantDetails, TenantForm, AddTenant, AssignRoom, LandlordProfile, EditProfile, ChangePassword
}

/** One entry of the back stack: a screen and the ids it shows (see each screen in [RentFlowApp]). */
private data class Route(val screen: Screen, val a: String? = null, val b: String? = null) {
    fun encode() = listOf(screen.name, a.orEmpty(), b.orEmpty()).joinToString("|")

    companion object {
        fun decode(value: String) = value.split("|").let { Route(Screen.valueOf(it[0]), it[1].ifEmpty { null }, it[2].ifEmpty { null }) }
    }
}

private val StackSaver = listSaver<List<Route>, String>(save = { stack -> stack.map { it.encode() } }, restore = { it.map(Route::decode) })

private val AuthScreens = setOf(Screen.Welcome, Screen.Login, Screen.ForgotPassword, Screen.Register)
private val TenantTabScreens = listOf(Screen.TenantHome, Screen.PaymentHistory, Screen.TenantProfile)
private val LandlordTabScreens = listOf(Screen.LandlordOverview, Screen.Properties, Screen.Tenants, Screen.LandlordProfile)

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
        ThemeMode.init(applicationContext)
        // Photos download with the same timeouts as the API
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components { add(OkHttpNetworkFetcherFactory(callFactory = { Network.images })) }
                .build()
        }
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
    var stack by rememberSaveable(stateSaver = StackSaver) {
        mutableStateOf(listOf(Route(saved?.let { homeFor(it.user.role) } ?: Screen.Welcome)))
    }
    var role by rememberSaveable { mutableStateOf(saved?.user?.role ?: UserRole.Tenant) }
    // Payments the tenant has made in this session, by index into the demo list
    var paidIndexes by rememberSaveable { mutableStateOf(listOf<Int>()) }

    val route = stack.last()
    val payments = Demo.tenantPayments.mapIndexed { i, p -> if (i in paidIndexes) p.settled() else p }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun go(screen: Screen, a: String? = null, b: String? = null) {
        stack = stack + Route(screen, a, b)
    }

    fun replace(screen: Screen, a: String? = null, b: String? = null) {
        stack = stack.dropLast(1) + Route(screen, a, b)
    }

    fun back() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    /** A tab sits on top of the role's home, so back from any tab goes home. */
    fun openTab(screen: Screen) {
        val home = homeFor(role)
        stack = if (screen == home) listOf(Route(home)) else listOf(Route(home), Route(screen))
    }

    fun enterApp(signedInAs: UserRole) {
        role = signedInAs
        LandlordStore.clear()
        stack = listOf(Route(homeFor(signedInAs)))
    }

    fun signOut() {
        paidIndexes = emptyList()
        LandlordStore.clear()
        stack = listOf(Route(Screen.Welcome))
        scope.launch { AuthRepository.logout() }
    }

    BackHandler(enabled = drawerState.isOpen || stack.size > 1) {
        if (drawerState.isOpen) scope.launch { drawerState.close() } else back()
    }

    val isSignedInScreen = route.screen !in AuthScreens

    // Signed-in screens follow the theme button; the sign-in screens stay light, as on the web
    val appDark = ThemeMode.isDark()
    val dark = appDark && isSignedInScreen
    SystemBars(
        darkIcons = route.screen != Screen.Welcome && !dark,
        background = if (dark) DarkPalette.surface else LightPalette.surface
    )

    // Check the saved token and refresh the name and contact details
    LaunchedEffect(Unit) { AuthRepository.refreshProfile() }

    // The session ended (token rejected, or the app restarted without "Remember me")
    LaunchedEffect(session == null, isSignedInScreen) {
        if (session == null && isSignedInScreen) {
            drawerState.close()
            paidIndexes = emptyList()
            LandlordStore.clear()
            stack = listOf(Route(Screen.Welcome), Route(Screen.Login))
        }
    }

    val user = session?.user
    val tabScreens = if (role == UserRole.Tenant) TenantTabScreens else LandlordTabScreens
    val shell = Shell(
        role = role,
        name = user?.name.orEmpty(),
        photo = user?.photo,
        notices = if (role == UserRole.Tenant) TenantNotices else LandlordNotices,
        onMenu = { scope.launch { drawerState.open() } },
        onProfile = { openTab(tabScreens.last()) },
        onTab = { openTab(tabScreens[it]) }
    )

    RentFlowTheme(dark = dark) {
        Box(Modifier.fillMaxSize()) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = isSignedInScreen && drawerState.isOpen,
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
                                    selected = route.screen == tabScreens[index],
                                    onClick = {
                                        openTab(tabScreens[index])
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
                    targetState = route,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "screen"
                ) { current ->
                    val a = current.a
                    val b = current.b
                    RentFlowTheme(dark = appDark && current.screen !in AuthScreens) {
                        when (current.screen) {
                            /* ---------- Sign in ---------- */
                            Screen.Welcome -> WelcomeScreen(
                                onChooseRole = {
                                    role = it
                                    go(Screen.Login)
                                },
                                onSignIn = { go(Screen.Login) }
                            )

                            Screen.Login -> LoginScreen(
                                initialRole = role,
                                notice = if (endedByServer) "Your session has ended. Please sign in again." else null,
                                onBack = ::back,
                                onSignedIn = ::enterApp,
                                onForgotPassword = {
                                    role = it
                                    go(Screen.ForgotPassword)
                                },
                                onRegister = {
                                    role = UserRole.Landlord
                                    go(Screen.Register)
                                }
                            )

                            Screen.ForgotPassword -> ForgotPasswordScreen(role = role, onBack = ::back, onBackToLogin = ::back)

                            Screen.Register -> RegisterScreen(
                                onBack = ::back,
                                onLogin = ::back,
                                onRegistered = { enterApp(UserRole.Landlord) }
                            )

                            /* ---------- Tenant (rent and payments are sample data) ---------- */
                            Screen.TenantHome -> TenantHomeScreen(
                                firstName = user?.firstName.orEmpty(),
                                payments = payments,
                                shell = shell,
                                onPayRent = { go(Screen.PayRent, "0") },
                                onOpenReceipt = { go(Screen.Receipt, "$it") },
                                onOpenHistory = { openTab(Screen.PaymentHistory) },
                                onOpenProfile = { openTab(Screen.TenantProfile) }
                            )

                            Screen.PayRent -> {
                                val index = a?.toIntOrNull() ?: 0
                                PayRentScreen(
                                    payment = payments[index],
                                    shell = shell,
                                    onBack = ::back,
                                    onConfirm = {
                                        paidIndexes = (paidIndexes + index).distinct()
                                        replace(Screen.PaymentSuccess, "$index")
                                    }
                                )
                            }

                            Screen.PaymentSuccess -> {
                                val index = a?.toIntOrNull() ?: 0
                                PaymentSuccessScreen(
                                    payment = payments[index],
                                    onBackToDashboard = { openTab(Screen.TenantHome) },
                                    onViewReceipt = { replace(Screen.Receipt, "$index") }
                                )
                            }

                            Screen.PaymentHistory -> PaymentHistoryScreen(
                                payments = payments,
                                shell = shell,
                                onOpenReceipt = { go(Screen.Receipt, "$it") },
                                onPay = { go(Screen.PayRent, "$it") }
                            )

                            Screen.Receipt -> ReceiptScreen(payment = payments[a?.toIntOrNull() ?: 0], shell = shell, onBack = ::back)

                            Screen.TenantProfile -> TenantProfileScreen(user = user, shell = shell, onLogout = ::signOut)

                            /* ---------- Landlord ---------- */
                            Screen.LandlordOverview -> LandlordOverviewScreen(
                                firstName = user?.firstName.orEmpty(),
                                tenants = Demo.tenants,
                                shell = shell,
                                onOpenTenants = { openTab(Screen.Tenants) },
                                onAddProperty = { go(Screen.PropertyForm) },
                                onAddTenant = { go(Screen.AddTenant) }
                            )

                            Screen.Properties -> PropertiesScreen(
                                shell = shell,
                                onOpenProperty = { go(Screen.PropertyDetails, it) },
                                onAddProperty = { go(Screen.PropertyForm) }
                            )

                            // a = property id
                            Screen.PropertyDetails -> PropertyDetailsScreen(
                                propertyId = a.orEmpty(),
                                shell = shell,
                                onBack = ::back,
                                onEdit = { go(Screen.PropertyForm, a) },
                                onAddRoom = { go(Screen.RoomForm, a) },
                                onOpenRoom = { go(Screen.RoomDetails, it) },
                                onDeleted = ::back
                            )

                            // a = property id when editing
                            Screen.PropertyForm -> PropertyFormScreen(
                                propertyId = a,
                                shell = shell,
                                onBack = ::back,
                                onSaved = { id -> if (a == null) replace(Screen.PropertyDetails, id) else back() }
                            )

                            // a = room id
                            Screen.RoomDetails -> RoomDetailsScreen(
                                roomId = a.orEmpty(),
                                shell = shell,
                                onBack = ::back,
                                onEdit = { go(Screen.RoomForm, null, a) },
                                onOpenTenant = { go(Screen.TenantDetails, it) },
                                onAssign = { go(Screen.AssignRoom, a) },
                                onDeleted = ::back
                            )

                            // a = property id for a new room, b = room id when editing
                            Screen.RoomForm -> RoomFormScreen(
                                propertyId = a,
                                roomId = b,
                                shell = shell,
                                onBack = ::back,
                                onSaved = { id -> if (b == null) replace(Screen.RoomDetails, id) else back() }
                            )

                            Screen.Tenants -> TenantsScreen(
                                shell = shell,
                                onOpenTenant = { go(Screen.TenantDetails, it) },
                                onAddTenant = { go(Screen.AddTenant) },
                                onAssignRoom = { go(Screen.AddTenant, it) }
                            )

                            // a = tenant id
                            Screen.TenantDetails -> TenantDetailsScreen(
                                tenantId = a.orEmpty(),
                                shell = shell,
                                onBack = ::back,
                                onEdit = { go(Screen.TenantForm, a) },
                                onAssignRoom = { go(Screen.AddTenant, a) },
                                onOpenRoom = { go(Screen.RoomDetails, it) },
                                onDeleted = ::back
                            )

                            // a = tenant id
                            Screen.TenantForm -> TenantFormScreen(tenantId = a.orEmpty(), shell = shell, onBack = ::back, onSaved = ::back)

                            // a = an existing tenant to give a room to, b = a room already chosen
                            Screen.AddTenant -> AddTenantScreen(
                                existingTenantId = a,
                                presetRoomId = b,
                                shell = shell,
                                onBack = ::back,
                                onDone = { id ->
                                    if (a != null) {
                                        back()
                                    } else {
                                        // Coming from "Assign room" on a room, that screen is done too
                                        stack = stack.dropLast(1).dropLastWhile { it.screen == Screen.AssignRoom } + Route(Screen.TenantDetails, id)
                                    }
                                }
                            )

                            // a = room id
                            Screen.AssignRoom -> AssignRoomScreen(
                                roomId = a.orEmpty(),
                                shell = shell,
                                onBack = ::back,
                                onNewTenant = { go(Screen.AddTenant, null, a) },
                                onDone = ::back
                            )

                            Screen.LandlordProfile -> LandlordProfileScreen(
                                user = user,
                                shell = shell,
                                onEditProfile = { go(Screen.EditProfile) },
                                onChangePassword = { go(Screen.ChangePassword) },
                                onLogout = ::signOut
                            )

                            Screen.EditProfile -> EditProfileScreen(user = user, shell = shell, onBack = ::back)

                            Screen.ChangePassword -> ChangePasswordScreen(shell = shell, onBack = ::back)
                        }
                    }
                }
            }
            // Save and delete messages float above the bottom tabs and form buttons
            ToastHost(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 72.dp))
        }
    }
}
