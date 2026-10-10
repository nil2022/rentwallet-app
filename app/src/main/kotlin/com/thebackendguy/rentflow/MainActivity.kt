package com.thebackendguy.rentflow

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.thebackendguy.rentflow.data.AuthRepository
import com.thebackendguy.rentflow.data.apiMonth
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.TenantStore
import com.thebackendguy.rentflow.data.NotificationStore
import com.thebackendguy.rentflow.data.UserRole
import com.thebackendguy.rentflow.data.remote.Network
import com.thebackendguy.rentflow.data.session.SessionStore
import com.thebackendguy.rentflow.push.Notifications
import com.thebackendguy.rentflow.ui.components.AppDrawer
import com.thebackendguy.rentflow.ui.components.SystemBars
import com.thebackendguy.rentflow.ui.components.ToastHost
import com.thebackendguy.rentflow.ui.screens.LandlordMenu
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.screens.TenantTabs
import com.thebackendguy.rentflow.ui.screens.auth.ForgotPasswordScreen
import com.thebackendguy.rentflow.ui.screens.auth.LoginScreen
import com.thebackendguy.rentflow.ui.screens.auth.RegisterScreen
import com.thebackendguy.rentflow.ui.screens.landlord.AddTenantScreen
import com.thebackendguy.rentflow.ui.screens.landlord.AssignRoomScreen
import com.thebackendguy.rentflow.ui.screens.landlord.BillTenantPickerScreen
import com.thebackendguy.rentflow.ui.screens.landlord.RecordPaymentScreen
import com.thebackendguy.rentflow.ui.screens.landlord.ChangePasswordScreen
import com.thebackendguy.rentflow.ui.screens.landlord.LandlordOverviewScreen
import com.thebackendguy.rentflow.ui.screens.landlord.LandlordProfileScreen
import com.thebackendguy.rentflow.ui.screens.landlord.PropertiesScreen
import com.thebackendguy.rentflow.ui.screens.landlord.PropertyDetailsScreen
import com.thebackendguy.rentflow.ui.screens.landlord.PropertyFormScreen
import com.thebackendguy.rentflow.ui.screens.landlord.RoomDetailsScreen
import com.thebackendguy.rentflow.ui.screens.landlord.RentCollectionScreen
import com.thebackendguy.rentflow.ui.screens.landlord.RentHistoryScreen
import com.thebackendguy.rentflow.ui.screens.landlord.showCollectionMonth
import com.thebackendguy.rentflow.ui.screens.landlord.showOverdueBills
import com.thebackendguy.rentflow.ui.screens.landlord.RoomFormScreen
import com.thebackendguy.rentflow.ui.screens.landlord.RoomsScreen
import com.thebackendguy.rentflow.ui.screens.landlord.TenantDetailsScreen
import com.thebackendguy.rentflow.ui.screens.landlord.TenantFormScreen
import com.thebackendguy.rentflow.ui.screens.landlord.TenantsScreen
import com.thebackendguy.rentflow.ui.screens.tenant.TenantBillScreen
import com.thebackendguy.rentflow.ui.screens.tenant.TenantBillsScreen
import com.thebackendguy.rentflow.ui.screens.tenant.TenantHomeScreen
import com.thebackendguy.rentflow.ui.screens.tenant.TenantProfileScreen
import com.thebackendguy.rentflow.ui.theme.DarkPalette
import com.thebackendguy.rentflow.ui.theme.LightPalette
import com.thebackendguy.rentflow.ui.theme.RentFlowTheme
import com.thebackendguy.rentflow.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import java.time.YearMonth

private enum class Screen {
    Login, ForgotPassword, Register,
    TenantHome, PaymentHistory, TenantBill, TenantProfile,
    LandlordOverview, Properties, PropertyDetails, PropertyForm, Rooms, RoomDetails, RoomForm,
    Tenants, TenantDetails, TenantForm, AddTenant, AssignRoom, RentCollection, RentHistory, PickBillTenant, RecordPayment, LandlordProfile, ChangePassword
}

/** One entry of the back stack: a screen and the ids it shows (see each screen in [RentFlowApp]). */
private data class Route(val screen: Screen, val a: String? = null, val b: String? = null) {
    fun encode() = listOf(screen.name, a.orEmpty(), b.orEmpty()).joinToString("|")

    companion object {
        fun decode(value: String) = value.split("|").let { Route(Screen.valueOf(it[0]), it[1].ifEmpty { null }, it[2].ifEmpty { null }) }
    }
}

private val StackSaver = listSaver<List<Route>, String>(save = { stack -> stack.map { it.encode() } }, restore = { it.map(Route::decode) })

/** Marks a Record Payment route that edits a saved bill: "edit:<bill id>". */
private const val EDIT_BILL = "edit:"

private val AuthScreens = setOf(Screen.Login, Screen.ForgotPassword, Screen.Register)
private val TenantTabScreens = listOf(Screen.TenantHome, Screen.PaymentHistory, Screen.TenantProfile)
private val LandlordTabScreens = listOf(Screen.LandlordOverview, Screen.Properties, Screen.Rooms, Screen.Tenants, Screen.RentCollection)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionStore.init(applicationContext)
        ThemeMode.init(applicationContext)
        Notifications.createChannel(applicationContext)
        Notifications.logToken()
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

    // Back in the app: pushes may have come while it was away
    override fun onResume() {
        super.onResume()
        NotificationStore.refreshCount()
    }
}

private fun homeFor(role: UserRole) = if (role == UserRole.Tenant) Screen.TenantHome else Screen.LandlordOverview

@Composable
private fun RentFlowApp() {
    val session by SessionStore.session.collectAsState()
    val endedByServer by SessionStore.endedByServer.collectAsState()
    val unread by NotificationStore.unread.collectAsState()
    // A saved session opens straight on its home screen; otherwise the app opens on Login
    val saved = remember { SessionStore.session.value }
    var stack by rememberSaveable(stateSaver = StackSaver) {
        mutableStateOf(listOf(Route(saved?.let { homeFor(it.user.role) } ?: Screen.Login)))
    }
    // Login starts on the role used last time (Landlord the first time)
    var role by rememberSaveable { mutableStateOf(saved?.user?.role ?: SessionStore.lastRole) }

    val route = stack.last()
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
        TenantStore.clear()
        NotificationStore.clear()
        stack = listOf(Route(homeFor(signedInAs)))
    }

    fun signOut() {
        LandlordStore.clear()
        TenantStore.clear()
        NotificationStore.clear()
        stack = listOf(Route(Screen.Login))
        scope.launch { AuthRepository.logout() }
    }

    BackHandler(enabled = drawerState.isOpen || stack.size > 1) {
        if (drawerState.isOpen) scope.launch { drawerState.close() } else back()
    }

    val isSignedInScreen = route.screen !in AuthScreens

    // Every screen follows the theme button, or the phone until one is picked
    val dark = ThemeMode.isDark()
    SystemBars(
        darkIcons = !dark && drawerState.targetValue == DrawerValue.Closed,
        background = if (dark) DarkPalette.surface else LightPalette.surface
    )

    // Check the saved token and refresh the name and contact details
    LaunchedEffect(Unit) { AuthRepository.refreshProfile() }

    // Ask for notification permission once the user is inside the app (Android 13+)
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(isSignedInScreen) {
        if (isSignedInScreen && Notifications.needsPermission(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Every time a home screen (Overview or tenant Home) opens: keep this phone's push token
    // on the server and refresh the bell's count
    LaunchedEffect(route) {
        if (route.screen == Screen.LandlordOverview || route.screen == Screen.TenantHome) {
            NotificationStore.refreshCount()
            AuthRepository.savePushToken()
        }
    }

    // The session ended (token rejected, or the app restarted without "Remember me")
    LaunchedEffect(session == null, isSignedInScreen) {
        if (session == null && isSignedInScreen) {
            drawerState.close()
            LandlordStore.clear()
            TenantStore.clear()
            NotificationStore.clear()
            stack = listOf(Route(Screen.Login))
        }
    }

    val user = session?.user
    val tabScreens = if (role == UserRole.Tenant) TenantTabScreens else LandlordTabScreens
    // A tenant's Profile is their last tab; a landlord's opens from the photo and the menu, as on the web
    val profileScreen = if (role == UserRole.Tenant) Screen.TenantProfile else Screen.LandlordProfile
    // The side menu: a landlord's tabs, then Rent History (its other entries do nothing yet)
    val menuScreens = if (role == UserRole.Tenant) TenantTabScreens else LandlordTabScreens + Screen.RentHistory
    val shell = Shell(
        role = role,
        name = user?.name.orEmpty(),
        photo = user?.photo,
        unread = unread,
        onMenu = { scope.launch { drawerState.open() } },
        onProfile = { openTab(profileScreen) },
        onTab = { openTab(tabScreens[it]) }
    )

    RentFlowTheme(dark = dark) {
        Box(Modifier.fillMaxSize()) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = isSignedInScreen && drawerState.isOpen,
                scrimColor = Color.Black.copy(alpha = 0.5f),
                drawerContent = {
                    val tenant = role == UserRole.Tenant
                    AppDrawer(
                        tabs = if (tenant) TenantTabs else LandlordMenu,
                        selected = menuScreens.indexOf(route.screen).takeIf { it >= 0 },
                        tagline = if (tenant) "Tenant" else "Property Manager",
                        name = user?.name.orEmpty(),
                        roleLabel = if (tenant) "Tenant" else "Owner",
                        photo = user?.photo,
                        onSelect = { index ->
                            // The menu's placeholder entries can't be tapped, so every index here has a screen
                            menuScreens.getOrNull(index)?.let(::openTab)
                            scope.launch { drawerState.close() }
                        },
                        onProfile = {
                            openTab(profileScreen)
                            scope.launch { drawerState.close() }
                        },
                        onLogout = {
                            scope.launch { drawerState.close() }
                            signOut()
                        }
                    )
                }
            ) {
                AnimatedContent(
                    targetState = route,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "screen"
                ) { current ->
                    val a = current.a
                    val b = current.b
                    when (current.screen) {
                        /* ---------- Sign in ---------- */
                        Screen.Login -> LoginScreen(
                            initialRole = role,
                            notice = if (endedByServer) "Your session has ended. Please sign in again." else null,
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

                        /* ---------- Tenant (their own bills) ---------- */
                        Screen.TenantHome -> TenantHomeScreen(
                            firstName = user?.firstName.orEmpty(),
                            shell = shell,
                            onOpenBill = { go(Screen.TenantBill, it) },
                            onOpenBills = { openTab(Screen.PaymentHistory) }
                        )

                        Screen.PaymentHistory -> TenantBillsScreen(shell = shell, onOpenBill = { go(Screen.TenantBill, it) })

                        // a = bill id
                        Screen.TenantBill -> TenantBillScreen(billId = a.orEmpty(), shell = shell, onBack = ::back)

                        Screen.TenantProfile -> TenantProfileScreen(
                            user = user,
                            shell = shell,
                            onChangePassword = { go(Screen.ChangePassword) },
                            onLogout = ::signOut
                        )

                        /* ---------- Landlord ---------- */
                        Screen.LandlordOverview -> LandlordOverviewScreen(
                            firstName = user?.firstName.orEmpty(),
                            shell = shell,
                            onRecordPayment = { go(Screen.PickBillTenant) },
                            onOpenCollection = {
                                showCollectionMonth(YearMonth.now().apiMonth(), null)
                                openTab(Screen.RentCollection)
                            },
                            onOpenOverdue = {
                                showOverdueBills()
                                openTab(Screen.RentCollection)
                            },
                            onCollect = { tenantId, month -> go(Screen.RecordPayment, tenantId, month) },
                            onOpenProperties = { openTab(Screen.Properties) },
                            onOpenProperty = { go(Screen.PropertyDetails, it) },
                            onAddProperty = { go(Screen.PropertyForm) }
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

                        Screen.Rooms -> RoomsScreen(
                            shell = shell,
                            onOpenRoom = { go(Screen.RoomDetails, it) },
                            onAssign = { go(Screen.AssignRoom, it) },
                            onAddRoom = { go(Screen.RoomForm, it) }
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
                            // Opened from the Rooms tab it stays lit; from a property, Properties does
                            tab = if (stack.any { it.screen == Screen.Rooms }) 2 else 1,
                            shell = shell,
                            onBack = ::back,
                            onEdit = { go(Screen.RoomForm, null, a) },
                            onOpenTenant = { go(Screen.TenantDetails, it) },
                            onOpenProperty = { go(Screen.PropertyDetails, it) },
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
                            onEditTenant = { go(Screen.TenantForm, it) },
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
                            onOpenProperty = { go(Screen.PropertyDetails, it) },
                            onOpenTenant = { go(Screen.TenantDetails, it) },
                            onRecordPayment = { month -> go(Screen.RecordPayment, a, month) },
                            onEditBill = { billId -> go(Screen.RecordPayment, a, "$EDIT_BILL$billId") },
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
                            onDone = ::back
                        )

                        Screen.RentCollection -> RentCollectionScreen(
                            shell = shell,
                            onRecord = { go(Screen.PickBillTenant) },
                            onRecordFor = { tenantId, month -> go(Screen.RecordPayment, tenantId, month) },
                            onEditBill = { tenantId, billId -> go(Screen.RecordPayment, tenantId, "$EDIT_BILL$billId") },
                            onHistory = { go(Screen.RentHistory) }
                        )

                        Screen.RentHistory -> RentHistoryScreen(
                            shell = shell,
                            onBack = ::back,
                            onOpenMonth = { month, propertyId ->
                                showCollectionMonth(month, propertyId)
                                openTab(Screen.RentCollection)
                            }
                        )

                        Screen.PickBillTenant -> BillTenantPickerScreen(
                            shell = shell,
                            onBack = ::back,
                            onPick = { replace(Screen.RecordPayment, it) }
                        )

                        // a = tenant id, b = billing month ("YYYY-MM", this month when null) or "edit:<bill id>"
                        Screen.RecordPayment -> RecordPaymentScreen(
                            tenantId = a.orEmpty(),
                            initialMonth = b?.takeUnless { it.startsWith(EDIT_BILL) },
                            editBillId = b?.takeIf { it.startsWith(EDIT_BILL) }?.removePrefix(EDIT_BILL),
                            shell = shell,
                            onBack = ::back,
                            onDone = ::back
                        )

                        Screen.LandlordProfile -> LandlordProfileScreen(
                            user = user,
                            shell = shell,
                            onChangePassword = { go(Screen.ChangePassword) },
                            onLogout = ::signOut,
                            onOpenProperties = { openTab(Screen.Properties) },
                            onOpenRooms = { openTab(Screen.Rooms) },
                            onOpenTenants = { openTab(Screen.Tenants) },
                            onOpenCollection = {
                                showCollectionMonth(YearMonth.now().apiMonth(), null)
                                openTab(Screen.RentCollection)
                            }
                        )

                        Screen.ChangePassword -> ChangePasswordScreen(
                            shell = shell,
                            onBack = ::back,
                            change = if (role == UserRole.Tenant) TenantStore::changePassword else LandlordStore::changePassword
                        )
                    }
                }
            }
            // Save and delete messages float above the bottom tabs and form buttons
            ToastHost(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 72.dp))
        }
    }
}
