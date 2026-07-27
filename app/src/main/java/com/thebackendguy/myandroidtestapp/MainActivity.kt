package com.thebackendguy.myandroidtestapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.VisualTransformation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebackendguy.myandroidtestapp.ui.landing.LandingRole
import com.thebackendguy.myandroidtestapp.ui.landing.LandingScreen
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.thebackendguy.myandroidtestapp.ui.theme.rentWalletColors
import com.thebackendguy.myandroidtestapp.ui.theme.MyAndroidTestAppTheme

private enum class AppScreen {
    Welcome,
    Login,
    TenantDashboard,
    PayRent,
    PaymentSuccess,
    PaymentHistory,
    ReceiptDetails,
    TenantProfile,
    LandlordDashboard,
    LandlordTenants,
    LandlordPropertyDetails,
    LandlordProfile,
    LandlordPlaceholder
}

private data class TenantProperty(
    val tenantName: String,
    val propertyName: String,
    val address: String,
    val rent: String,
    val deposit: String,
    val status: String,
    val dateLabel: String,
    val contact: String,
    val leaseStart: String,
    val leaseEnd: String,
    val dueDay: String,
    val walletStatus: String,
    val lastPayment: String
)

private val landlordTenants = listOf(
    TenantProperty(
        tenantName = "Rohan Mehta",
        propertyName = "Green View Residency",
        address = "Flat 4B, Salt Lake, Kolkata",
        rent = "Rs. 18,500",
        deposit = "Rs. 35,000",
        status = "Paid",
        dateLabel = "Paid 06 May",
        contact = "+91 98765 43210",
        leaseStart = "01 Apr 2025",
        leaseEnd = "31 Mar 2027",
        dueDay = "10th of every month",
        walletStatus = "Credited",
        lastPayment = "Rs. 18,500 on 06 May 2026"
    ),
    TenantProperty(
        tenantName = "Priya Sen",
        propertyName = "Lakefront Homes",
        address = "Tower 2, New Town, Kolkata",
        rent = "Rs. 20,000",
        deposit = "Rs. 40,000",
        status = "Paid",
        dateLabel = "Paid 05 May",
        contact = "+91 91234 56780",
        leaseStart = "01 Jan 2026",
        leaseEnd = "31 Dec 2026",
        dueDay = "7th of every month",
        walletStatus = "Credited",
        lastPayment = "Rs. 20,000 on 05 May 2026"
    ),
    TenantProperty(
        tenantName = "Arjun Das",
        propertyName = "Metro Heights",
        address = "Block C, Dum Dum, Kolkata",
        rent = "Rs. 18,500",
        deposit = "Rs. 36,000",
        status = "Pending",
        dateLabel = "Due 10 May",
        contact = "+91 90000 11122",
        leaseStart = "15 Feb 2026",
        leaseEnd = "14 Feb 2027",
        dueDay = "10th of every month",
        walletStatus = "Pending",
        lastPayment = "Rs. 18,500 on 09 Apr 2026"
    ),
    TenantProperty(
        tenantName = "Neha Roy",
        propertyName = "City Nest Apartment",
        address = "Flat 8A, Ballygunge, Kolkata",
        rent = "Rs. 18,500",
        deposit = "Rs. 35,000",
        status = "Pending",
        dateLabel = "Due 12 May",
        contact = "+91 98888 22233",
        leaseStart = "01 Mar 2026",
        leaseEnd = "28 Feb 2027",
        dueDay = "12th of every month",
        walletStatus = "Pending",
        lastPayment = "Rs. 18,500 on 12 Apr 2026"
    ),
    TenantProperty(
        tenantName = "Kabir Khan",
        propertyName = "Sunrise Enclave",
        address = "House 12, Behala, Kolkata",
        rent = "Rs. 17,000",
        deposit = "Rs. 34,000",
        status = "Paid",
        dateLabel = "Paid 04 May",
        contact = "+91 97777 33344",
        leaseStart = "01 Oct 2025",
        leaseEnd = "30 Sep 2026",
        dueDay = "5th of every month",
        walletStatus = "Credited",
        lastPayment = "Rs. 17,000 on 04 May 2026"
    )
)

private data class PaymentRecord(
    val month: String,
    val amount: String,
    val status: String,
    val dateLabel: String,
    val transactionId: String,
    val paymentMethod: String,
    val receiptStatus: String
)

private val tenantPaymentRecords = listOf(
    PaymentRecord(
        month = "May 2026",
        amount = "Rs. 18,500",
        status = "Pending",
        dateLabel = "Due 10 May 2026",
        transactionId = "Not generated",
        paymentMethod = "Not selected",
        receiptStatus = "Awaiting payment"
    ),
    PaymentRecord(
        month = "April 2026",
        amount = "Rs. 18,500",
        status = "Paid",
        dateLabel = "Paid 08 Apr 2026",
        transactionId = "RW-APR-2026-0914",
        paymentMethod = "UPI / Bank payment",
        receiptStatus = "Generated"
    ),
    PaymentRecord(
        month = "March 2026",
        amount = "Rs. 18,500",
        status = "Paid",
        dateLabel = "Paid 09 Mar 2026",
        transactionId = "RW-MAR-2026-0841",
        paymentMethod = "UPI / Bank payment",
        receiptStatus = "Generated"
    ),
    PaymentRecord(
        month = "February 2026",
        amount = "Rs. 18,500",
        status = "Failed",
        dateLabel = "Failed 10 Feb 2026",
        transactionId = "RW-FEB-2026-0773",
        paymentMethod = "UPI / Bank payment",
        receiptStatus = "Not generated"
    )
)

private data class AlertItem(
    val title: String,
    val message: String,
    val status: String
)

private val tenantDashboardAlerts = listOf(
    AlertItem(
        title = "Rent due reminder",
        message = "May rent is due on 10 May. Pay before the due date to keep your record clean.",
        status = "Due"
    ),
    AlertItem(
        title = "Receipt generated",
        message = "Your April rent receipt is available in Payment History.",
        status = "Receipt"
    )
)

private val tenantPaymentAlerts = listOf(
    AlertItem(
        title = "Payment failed",
        message = "February rent payment failed. Review the details before trying again.",
        status = "Failed"
    ),
    AlertItem(
        title = "Payment successful",
        message = "April rent was paid and credited to the landlord wallet.",
        status = "Paid"
    )
)

private val landlordDashboardAlerts = listOf(
    AlertItem(
        title = "Rent received",
        message = "Rohan's May rent has been credited to your wallet.",
        status = "Paid"
    ),
    AlertItem(
        title = "Pending rent",
        message = "Arjun and Neha still have pending May rent.",
        status = "Pending"
    )
)

private val landlordTenantAlerts = listOf(
    AlertItem(
        title = "Follow up needed",
        message = "Two tenants have upcoming or overdue rent actions.",
        status = "Action"
    ),
    AlertItem(
        title = "Withdrawal processing",
        message = "Bank transfer of Rs. 15,000 is currently processing.",
        status = "Processing"
    )
)

private enum class UserRole(
    val title: String,
    val description: String
) {
    Tenant(
        title = "Tenant",
        description = "View rent due, pay securely, and keep receipts in one place."
    ),
    Landlord(
        title = "Landlord",
        description = "Track rent collections, wallet balance, and tenant status."
    )
}

private val UserRole.accent: Color
    @Composable get() = when (this) {
        UserRole.Tenant -> MaterialTheme.rentWalletColors.tenantPrimary
        UserRole.Landlord -> MaterialTheme.rentWalletColors.landlordPrimary
    }

private val UserRole.onAccent: Color
    @Composable get() = when (this) {
        UserRole.Tenant -> MaterialTheme.rentWalletColors.tenantOnPrimary
        UserRole.Landlord -> MaterialTheme.rentWalletColors.landlordOnPrimary
    }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            MyAndroidTestAppTheme(darkTheme = false, dynamicColor = false) {
                RentWalletApp()
            }
        }
    }
}

@Composable
private fun RentWalletApp() {
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.Welcome) }
    var selectedRole by rememberSaveable { mutableStateOf(UserRole.Tenant) }
    var selectedPaymentIndex by rememberSaveable { mutableStateOf(0) }
    var selectedTenantIndex by rememberSaveable { mutableStateOf(0) }

    BackHandler(enabled = currentScreen != AppScreen.Welcome) {
        currentScreen = when (currentScreen) {
            AppScreen.TenantDashboard -> AppScreen.Login
            AppScreen.Login -> AppScreen.Welcome
            AppScreen.PayRent -> AppScreen.TenantDashboard
            AppScreen.PaymentSuccess -> AppScreen.TenantDashboard
            AppScreen.PaymentHistory -> AppScreen.TenantDashboard
            AppScreen.ReceiptDetails -> AppScreen.PaymentHistory
            AppScreen.TenantProfile -> AppScreen.TenantDashboard
            AppScreen.LandlordDashboard -> AppScreen.Login
            AppScreen.LandlordTenants -> AppScreen.LandlordDashboard
            AppScreen.LandlordPropertyDetails -> AppScreen.LandlordTenants
            AppScreen.LandlordProfile -> AppScreen.LandlordDashboard
            else -> AppScreen.Welcome
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentScreen) {
            AppScreen.Welcome -> LandingScreen(
                onRoleSelected = { landingRole ->
                    selectedRole = when (landingRole) {
                        LandingRole.Tenant -> UserRole.Tenant
                        LandingRole.Landlord -> UserRole.Landlord
                    }
                    currentScreen = AppScreen.Login
                }
            )

            AppScreen.Login -> LoginScreen(
                role = selectedRole,
                onBack = { currentScreen = AppScreen.Welcome },
                onContinue = {
                    currentScreen = when (selectedRole) {
                        UserRole.Tenant -> AppScreen.TenantDashboard
                        UserRole.Landlord -> AppScreen.LandlordDashboard
            }
        }
            )

            AppScreen.TenantDashboard -> TenantDashboardScreen(
                onLogout = { currentScreen = AppScreen.Welcome },
                onPayRent = { currentScreen = AppScreen.PayRent },
                onOpenPayments = { currentScreen = AppScreen.PaymentHistory },
                onOpenProfile = { currentScreen = AppScreen.TenantProfile }
            )

            AppScreen.PayRent -> PayRentScreen(
                onBack = { currentScreen = AppScreen.TenantDashboard },
                onConfirmPayment = { currentScreen = AppScreen.PaymentSuccess }
            )

            AppScreen.PaymentSuccess -> PaymentSuccessScreen(
                onBackToDashboard = { currentScreen = AppScreen.TenantDashboard }
            )

            AppScreen.PaymentHistory -> PaymentHistoryScreen(
                onDashboard = { currentScreen = AppScreen.TenantDashboard },
                onProfile = { currentScreen = AppScreen.TenantProfile },
                onReceiptSelected = { index ->
                    selectedPaymentIndex = index
                    currentScreen = AppScreen.ReceiptDetails
                }
            )

            AppScreen.ReceiptDetails -> ReceiptDetailsScreen(
                payment = tenantPaymentRecords[selectedPaymentIndex.coerceIn(0, tenantPaymentRecords.lastIndex)],
                onBack = { currentScreen = AppScreen.PaymentHistory }
            )

            AppScreen.TenantProfile -> TenantProfileScreen(
                onDashboard = { currentScreen = AppScreen.TenantDashboard },
                onPayments = { currentScreen = AppScreen.PaymentHistory },
                onLogout = { currentScreen = AppScreen.Welcome }
            )

            AppScreen.LandlordDashboard -> LandlordDashboardScreen(
                onOpenTenants = { currentScreen = AppScreen.LandlordTenants },
                onOpenProfile = { currentScreen = AppScreen.LandlordProfile }
            )

            AppScreen.LandlordTenants -> LandlordTenantsScreen(
                onDashboard = { currentScreen = AppScreen.LandlordDashboard },
                onProfile = { currentScreen = AppScreen.LandlordProfile },
                onTenantSelected = { index ->
                    selectedTenantIndex = index
                    currentScreen = AppScreen.LandlordPropertyDetails
                }
            )

            AppScreen.LandlordPropertyDetails -> PropertyDetailsScreen(
                tenant = landlordTenants[selectedTenantIndex.coerceIn(0, landlordTenants.lastIndex)],
                onBack = { currentScreen = AppScreen.LandlordTenants }
            )

            AppScreen.LandlordProfile -> LandlordProfileScreen(
                onDashboard = { currentScreen = AppScreen.LandlordDashboard },
                onTenants = { currentScreen = AppScreen.LandlordTenants },
                onLogout = { currentScreen = AppScreen.Welcome }
            )

            AppScreen.LandlordPlaceholder -> LandlordPlaceholderScreen(
                onBack = { currentScreen = AppScreen.Login }
            )
        }
    }
}



@Composable
private fun LoginScreen(
    role: UserRole,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    when (role) {
        UserRole.Tenant -> TenantLoginScreen(onBack = onBack, onContinue = onContinue)
        UserRole.Landlord -> LandlordLoginScreen(onBack = onBack, onContinue = onContinue)
    }
}

@Composable
private fun TenantLoginScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var mobileNumber by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { focusManager.clearFocus() }
            }
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(text = "Back")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.surfaceContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Tenant",
                            modifier = Modifier.size(38.dp),
                            tint = MaterialTheme.rentWalletColors.tenantPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Tenant Login",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sign in to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mobile Number") },
                    placeholder = {
                        Text(
                            text = "Enter mobile number",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = loginTextFieldColors(MaterialTheme.rentWalletColors.tenantPrimary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    placeholder = {
                        Text(
                            text = "Enter password",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Outlined.Visibility
                                } else {
                                    Icons.Outlined.VisibilityOff
                                },
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = loginTextFieldColors(MaterialTheme.rentWalletColors.tenantPrimary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.rentWalletColors.tenantPrimary,
                        contentColor = MaterialTheme.rentWalletColors.tenantOnPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = { /* Non-functional link */ }) {
                        Text(
                            text = "Forgot Password?",
                            color = MaterialTheme.rentWalletColors.tenantPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Secure Footer
        SecureLoginFooter()
    }
}

@Composable
private fun LandlordLoginScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var mobileNumber by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { focusManager.clearFocus() }
            }
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(text = "Back")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Landlord Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.surfaceContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Landlord",
                            modifier = Modifier.size(38.dp),
                            tint = MaterialTheme.rentWalletColors.landlordPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Landlord Login",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sign in to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mobile Number") },
                    placeholder = {
                        Text(
                            text = "Enter mobile number",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = loginTextFieldColors(MaterialTheme.rentWalletColors.landlordPrimary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    placeholder = {
                        Text(
                            text = "Enter password",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Outlined.Visibility
                                } else {
                                    Icons.Outlined.VisibilityOff
                                },
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = loginTextFieldColors(MaterialTheme.rentWalletColors.landlordPrimary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.rentWalletColors.landlordPrimary,
                        contentColor = MaterialTheme.rentWalletColors.landlordOnPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = { /* Non-functional link */ }) {
                        Text(
                            text = "Forgot Password?",
                            color = MaterialTheme.rentWalletColors.landlordPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Secure Footer
        SecureLoginFooter()
    }
}

@Composable
private fun SecureLoginFooter(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Secure Login  ·  Privacy Protected",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "© RentWallet",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun TenantDashboardScreen(
    onLogout: () -> Unit,
    onPayRent: () -> Unit,
    onOpenPayments: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            DashboardHeader(onLogout = onLogout)

            Spacer(modifier = Modifier.height(22.dp))

            CurrentRentCard(
                onPayRent = onPayRent
            )

            Spacer(modifier = Modifier.height(16.dp))

            TenantRentStatusCard()

            Spacer(modifier = Modifier.height(16.dp))

            AlertsSection(
                title = "Alerts",
                alerts = tenantDashboardAlerts,
                accent = MaterialTheme.rentWalletColors.tenantPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            RecentPaymentsCard()

            Spacer(modifier = Modifier.height(16.dp))

            PropertySummaryCard()

            Spacer(modifier = Modifier.height(16.dp))

            DashboardStatsRow()
        }

        TenantBottomBar(
            selectedTab = "Dashboard",
            onDashboard = {},
            onPayments = onOpenPayments,
            onProfile = onOpenProfile
        )
    }
}


@Composable
private fun PayRentScreen(
    onBack: () -> Unit,
    onConfirmPayment: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(text = "Back")
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Pay Rent",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Review the rent details before confirming payment.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(22.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Rent amount",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Rs. 18,500",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    StatusPill(
                        text = "Due",
                        background = MaterialTheme.rentWalletColors.warningContainer,
                        content = MaterialTheme.rentWalletColors.warning
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                PaymentInfoRow(label = "Property", value = "Green View Residency")
                PaymentInfoRow(label = "Address", value = "Flat 4B, Salt Lake, Kolkata")
                PaymentInfoRow(label = "Landlord", value = "Amit Sharma")
                PaymentInfoRow(label = "Due date", value = "10 May 2026")
                PaymentInfoRow(label = "Credited to", value = "Landlord wallet")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Payment method",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "UPI / Bank payment",
                            color = Color(0xFF0F766E),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pay using your preferred bank or UPI method.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        StatusPill(
                            text = "Selected",
                            background = Color.White,
                            content = Color(0xFF0F766E)
                        )
            }
        }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Payment summary",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                PaymentInfoRow(label = "Monthly rent", value = "Rs. 18,500")
                PaymentInfoRow(label = "Service fee", value = "Rs. 0")
                PaymentInfoRow(label = "Late fee", value = "Rs. 0")
                PaymentInfoRow(label = "Processing", value = "Instant credit")
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total payable",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Rs. 18,500",
                            color = Color(0xFF0F766E),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
            }
        }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PaymentSafetyNote()

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = onConfirmPayment,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.rentWalletColors.tenantPrimary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Confirm Payment",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.navigationBarsPadding().height(12.dp))
    }
}


@Composable
private fun PaymentSuccessScreen(
    onBackToDashboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .background(MaterialTheme.rentWalletColors.successContainer, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Paid",
                color = MaterialTheme.rentWalletColors.success,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Payment Successful",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Amit Sharma's landlord wallet has been credited.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                PaymentInfoRow(label = "Amount paid", value = "Rs. 18,500")
                PaymentInfoRow(label = "Transaction ID", value = "RW-MAY-2026-1042")
                PaymentInfoRow(label = "Paid on", value = "06 May 2026, 12:05 AM")
                PaymentInfoRow(label = "Payment method", value = "UPI / Bank payment")
                PaymentInfoRow(label = "Wallet credit", value = "Completed")
                PaymentInfoRow(label = "Receipt status", value = "Generated")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Receipt RW-MAY-2026-1042 is ready in Payment History.",
                color = Color(0xFF0F766E),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBackToDashboard,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.rentWalletColors.tenantPrimary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Back to Dashboard",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.navigationBarsPadding().height(12.dp))
    }
}


@Composable
private fun PaymentHistoryScreen(
    onDashboard: () -> Unit,
    onProfile: () -> Unit,
    onReceiptSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = "Payment History",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Track paid, pending, and failed rent payments.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.rentWalletColors.tenantPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "2026 summary",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rs. 37,000 paid",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1 rent due and 1 failed payment need attention.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusSummaryChip(
                    label = "Paid",
                    count = "2",
                    color = MaterialTheme.rentWalletColors.success,
                    modifier = Modifier.weight(1f)
                )
                StatusSummaryChip(
                    label = "Pending",
                    count = "1",
                    color = MaterialTheme.rentWalletColors.warning,
                    modifier = Modifier.weight(1f)
                )
                StatusSummaryChip(
                    label = "Failed",
                    count = "1",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AlertsSection(
                title = "Payment alerts",
                alerts = tenantPaymentAlerts,
                accent = MaterialTheme.rentWalletColors.tenantPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            tenantPaymentRecords.forEachIndexed { index, payment ->
                PaymentHistoryRow(
                    payment = payment,
                    onClick = { onReceiptSelected(index) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        TenantBottomBar(
            selectedTab = "Payments",
            onDashboard = onDashboard,
            onPayments = {},
            onProfile = onProfile
        )
    }
}


@Composable
private fun PaymentHistoryRow(
    payment: PaymentRecord,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = payment.month,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = payment.dateLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
                StatusPill(
                    text = payment.status,
                    background = paymentStatusBackground(payment.status),
                    content = paymentStatusContent(payment.status)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = receiptActionText(payment.status),
                color = paymentStatusContent(payment.status),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = payment.amount,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (payment.status == "Paid") "View receipt" else "View details",
                    color = Color(0xFF0F766E),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
private fun ReceiptDetailsScreen(
    payment: PaymentRecord,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(text = "Back")
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Receipt Details",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = payment.month,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(22.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatusPill(
                    text = payment.status,
                    background = paymentStatusBackground(payment.status),
                    content = paymentStatusContent(payment.status)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = payment.amount,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (payment.status == "Paid") {
                        "Landlord wallet credited"
                    } else {
                        "Wallet credit pending"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                PaymentInfoRow(label = "Tenant", value = "Rohan Mehta")
                PaymentInfoRow(label = "Landlord", value = "Amit Sharma")
                PaymentInfoRow(label = "Property", value = "Green View Residency")
                PaymentInfoRow(label = "Address", value = "Flat 4B, Salt Lake, Kolkata")
                PaymentInfoRow(label = "Transaction ID", value = payment.transactionId)
                PaymentInfoRow(label = "Payment date", value = payment.dateLabel)
                PaymentInfoRow(label = "Payment method", value = payment.paymentMethod)
                PaymentInfoRow(label = "Wallet status", value = if (payment.status == "Paid") "Credited" else "Not credited")
                PaymentInfoRow(label = "Receipt status", value = payment.receiptStatus)
            }
        }

        Spacer(modifier = Modifier.navigationBarsPadding().height(12.dp))
    }
}


@Composable
private fun TenantProfileScreen(
    onDashboard: () -> Unit,
    onPayments: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            ProfileTitle(title = "Tenant Profile", subtitle = "Manage your rental identity and payment details.")

            Spacer(modifier = Modifier.height(20.dp))

            ProfileHeroCard(
                initials = "RM",
                name = "Rohan Mehta",
                role = "Tenant",
                accent = MaterialTheme.rentWalletColors.tenantPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileInfoCard(
                title = "Personal details",
                rows = listOf(
                    "Mobile" to "+91 98765 43210",
                    "Email" to "rohan.mehta@example.com",
                    "Current property" to "Green View Residency",
                    "Landlord" to "Amit Sharma"
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileInfoCard(
                title = "Rent and wallet",
                rows = listOf(
                    "Monthly rent" to "Rs. 18,500",
                    "Security deposit" to "Rs. 35,000",
                    "Rent due day" to "10th of every month",
                    "Payment method" to "UPI / Bank"
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF344054),
                    contentColor = Color.White
                )
            ) {
                Text(text = "Logout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        TenantBottomBar(
            selectedTab = "Profile",
            onDashboard = onDashboard,
            onPayments = onPayments,
            onProfile = {}
        )
    }
}


@Composable
private fun LandlordProfileScreen(
    onDashboard: () -> Unit,
    onTenants: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            ProfileTitle(title = "Landlord Profile", subtitle = "Manage owner, wallet, and property account details.")

            Spacer(modifier = Modifier.height(20.dp))

            ProfileHeroCard(
                initials = "AS",
                name = "Amit Sharma",
                role = "Landlord",
                accent = MaterialTheme.rentWalletColors.landlordPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileInfoCard(
                title = "Owner details",
                rows = listOf(
                    "Mobile" to "+91 90123 45678",
                    "Email" to "amit.sharma@example.com",
                    "Total tenants" to "5",
                    "Active properties" to "5"
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileInfoCard(
                title = "Portfolio health",
                rows = listOf(
                    "Collection progress" to "60%",
                    "Paid tenants" to "3",
                    "Pending tenants" to "2",
                    "Attention needed" to "Rs. 37,000 pending"
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileInfoCard(
                title = "Wallet and bank",
                rows = listOf(
                    "Wallet balance" to "Rs. 74,000",
                    "May rent received" to "Rs. 55,500",
                    "Pending rent" to "Rs. 37,000",
                    "Bank account" to "HDFC **** 4821"
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF344054),
                    contentColor = Color.White
                )
            ) {
                Text(text = "Logout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        LandlordBottomBar(
            selectedTab = "Profile",
            onDashboard = onDashboard,
            onTenants = onTenants,
            onProfile = {}
        )
    }
}


@Composable
private fun ProfileTitle(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
    }
}


@Composable
private fun ProfileHeroCard(
    initials: String,
    name: String,
    role: String,
    accent: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = accent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = role,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}


@Composable
private fun ProfileInfoCard(
    title: String,
    rows: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            rows.forEach { row ->
                PaymentInfoRow(label = row.first, value = row.second)
            }
        }
    }
}


@Composable
private fun LandlordDashboardScreen(
    onOpenTenants: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            LandlordHeader()

            Spacer(modifier = Modifier.height(22.dp))

            LandlordWalletCard()

            Spacer(modifier = Modifier.height(16.dp))

            LandlordCollectionSummary()

            Spacer(modifier = Modifier.height(16.dp))

            LandlordAttentionCard(onOpenTenants = onOpenTenants)

            Spacer(modifier = Modifier.height(16.dp))

            AlertsSection(
                title = "Important alerts",
                alerts = landlordDashboardAlerts,
                accent = MaterialTheme.rentWalletColors.landlordPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            LandlordRecentTransactions()

            Spacer(modifier = Modifier.height(16.dp))

            LandlordTenantPreview(onOpenTenants = onOpenTenants)

            Spacer(modifier = Modifier.navigationBarsPadding().height(12.dp))
        }

        LandlordBottomBar(
            selectedTab = "Dashboard",
            onDashboard = {},
            onTenants = onOpenTenants,
            onProfile = onOpenProfile
        )
    }
}


@Composable
private fun LandlordHeader() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Hello, Amit",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your landlord wallet overview",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}


@Composable
private fun LandlordWalletCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.rentWalletColors.landlordPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Wallet balance",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Rs. 74,000",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Rent collections are credited here after successful tenant payments.",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusPill(
                    text = "Available",
                    background = Color.White,
                    content = Color(0xFFB7791F)
                )
                StatusPill(
                    text = "5 properties",
                    background = Color.White.copy(alpha = 0.16f),
                    content = Color.White
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.rentWalletColors.landlordOnPrimary
                )
            ) {
                Text(
                    text = "Withdraw",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
private fun LandlordCollectionSummary() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
                Text(
                    text = "May collection summary",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "3 of 5 tenants have paid this month.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            CollectionProgressBar(progress = 0.6f)
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallMetricCard(
                    label = "Received",
                    value = "Rs. 55.5k",
                    modifier = Modifier.weight(1f)
                )
                SmallMetricCard(
                    label = "Pending",
                    value = "Rs. 37k",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallMetricCard(
                    label = "Total tenants",
                    value = "5",
                    modifier = Modifier.weight(1f)
                )
                SmallMetricCard(
                    label = "Paid",
                    value = "3 of 5",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}


@Composable
private fun LandlordAttentionCard(
    onOpenTenants: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "2 rents pending",
                        color = MaterialTheme.rentWalletColors.warning,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Arjun and Neha still need to complete May rent.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
                OutlinedButton(
                    onClick = onOpenTenants,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.rentWalletColors.warning
                    )
                ) {
                    Text(text = "Review")
                }
            }
        }
    }
}


@Composable
private fun CollectionProgressBar(
    progress: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(10.dp)
                .background(MaterialTheme.rentWalletColors.landlordPrimary, RoundedCornerShape(8.dp))
        )
    }
}


@Composable
private fun LandlordRecentTransactions() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Recent wallet transactions",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            WalletTransactionRow(title = "Rent received from Rohan", detail = "Green View Residency", amount = "+ Rs. 18,500", status = "Credited")
            Spacer(modifier = Modifier.height(12.dp))
            WalletTransactionRow(title = "Rent received from Priya", detail = "Lakefront Homes", amount = "+ Rs. 20,000", status = "Credited")
            Spacer(modifier = Modifier.height(12.dp))
            WalletTransactionRow(title = "Wallet withdrawal", detail = "Bank transfer", amount = "- Rs. 15,000", status = "Processing")
        }
    }
}


@Composable
private fun WalletTransactionRow(
    title: String,
    detail: String,
    amount: String,
    status: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = amount,
                color = if (amount.startsWith("+")) MaterialTheme.rentWalletColors.success else MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            )
            Spacer(modifier = Modifier.height(4.dp))
            StatusPill(
                text = status,
                background = if (status == "Credited") MaterialTheme.rentWalletColors.successContainer else MaterialTheme.rentWalletColors.warningContainer,
                content = if (status == "Credited") MaterialTheme.rentWalletColors.success else MaterialTheme.rentWalletColors.warning
            )
        }
    }
}


@Composable
private fun LandlordTenantPreview(
    onOpenTenants: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tenant rent status",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onOpenTenants,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.rentWalletColors.landlordOnPrimary
                    )
                ) {
                    Text(text = "View all")
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            TenantStatusRow(name = "Rohan Mehta", property = "Green View Residency", amount = "Rs. 18,500", status = "Paid", date = "06 May")
            Spacer(modifier = Modifier.height(12.dp))
            TenantStatusRow(name = "Priya Sen", property = "Lakefront Homes", amount = "Rs. 20,000", status = "Paid", date = "05 May")
            Spacer(modifier = Modifier.height(12.dp))
            TenantStatusRow(name = "Arjun Das", property = "Metro Heights", amount = "Rs. 18,500", status = "Pending", date = "Due 10 May")
        }
    }
}


@Composable
private fun TenantStatusRow(
    name: String,
    property: String,
    amount: String,
    status: String,
    date: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$property • $amount • $date",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        StatusPill(
            text = status,
            background = paymentStatusBackground(status),
            content = paymentStatusContent(status)
        )
    }
}


@Composable
private fun LandlordTenantsScreen(
    onDashboard: () -> Unit,
    onProfile: () -> Unit,
    onTenantSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = "Tenants",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Review each tenant, property, and rent status.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusSummaryChip(
                    label = "Paid",
                    count = "3",
                    color = MaterialTheme.rentWalletColors.success,
                    modifier = Modifier.weight(1f)
                )
                StatusSummaryChip(
                    label = "Pending",
                    count = "2",
                    color = MaterialTheme.rentWalletColors.warning,
                    modifier = Modifier.weight(1f)
                )
                StatusSummaryChip(
                    label = "Total",
                    count = "5",
                    color = Color(0xFFB7791F),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AlertsSection(
                title = "Tenant alerts",
                alerts = landlordTenantAlerts,
                accent = MaterialTheme.rentWalletColors.landlordPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            landlordTenants.forEachIndexed { index, tenant ->
                TenantListCard(
                    tenant = tenant,
                    onClick = { onTenantSelected(index) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.navigationBarsPadding().height(12.dp))
        }

        LandlordBottomBar(
            selectedTab = "Tenants",
            onDashboard = onDashboard,
            onTenants = {},
            onProfile = onProfile
        )
    }
}


@Composable
private fun TenantListCard(
    tenant: TenantProperty,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = tenant.tenantName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tenant.propertyName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                StatusPill(
                    text = tenant.status,
                    background = paymentStatusBackground(tenant.status),
                    content = paymentStatusContent(tenant.status)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = tenant.address,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Contact ${tenant.contact} • Wallet ${tenant.walletStatus}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = tenant.rent,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tenant.dateLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "View Details",
                    color = Color(0xFFB7791F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
private fun PropertyDetailsScreen(
    tenant: TenantProperty,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(text = "Back")
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = tenant.propertyName,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = tenant.address,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.rentWalletColors.landlordPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Monthly rent",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tenant.rent,
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tenant.dateLabel,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    StatusPill(
                        text = tenant.status,
                        background = paymentStatusBackground(tenant.status),
                        content = paymentStatusContent(tenant.status)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PropertyWalletStatusCard(tenant = tenant)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Tenant details",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                PaymentInfoRow(label = "Tenant", value = tenant.tenantName)
                PaymentInfoRow(label = "Contact", value = tenant.contact)
                PaymentInfoRow(label = "Security deposit", value = tenant.deposit)
                PaymentInfoRow(label = "Rent due day", value = tenant.dueDay)
                PaymentInfoRow(label = "Payment status", value = tenant.status)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Lease and wallet",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                PaymentInfoRow(label = "Lease start", value = tenant.leaseStart)
                PaymentInfoRow(label = "Lease end", value = tenant.leaseEnd)
                PaymentInfoRow(label = "Wallet status", value = tenant.walletStatus)
                PaymentInfoRow(label = "Last payment", value = tenant.lastPayment)
                PaymentInfoRow(label = "Next action", value = if (tenant.status == "Paid") "No action needed" else "Follow up with tenant")
            }
        }

        Spacer(modifier = Modifier.navigationBarsPadding().height(12.dp))
    }
}


@Composable
private fun PropertyWalletStatusCard(
    tenant: TenantProperty
) {
    val isPaid = tenant.status == "Paid"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) MaterialTheme.rentWalletColors.successContainer else Color(0xFFFFF8E6)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (isPaid) "Wallet credited" else "Wallet credit pending",
                color = if (isPaid) MaterialTheme.rentWalletColors.success else MaterialTheme.rentWalletColors.warning,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isPaid) {
                    "${tenant.rent} from ${tenant.tenantName} has been credited."
                } else {
                    "${tenant.tenantName}'s ${tenant.rent} rent is still pending."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}


@Composable
private fun DashboardHeader(
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Hello, Rohan",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your May rent overview",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }

        OutlinedButton(
            onClick = onLogout,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(text = "Logout")
        }
    }
}


@Composable
private fun CurrentRentCard(
    onPayRent: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.rentWalletColors.tenantPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Current rent due",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Rs. 18,500",
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                StatusPill(
                    text = "Pending",
                    background = MaterialTheme.rentWalletColors.warningContainer,
                    content = MaterialTheme.rentWalletColors.warning
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            DashboardDetailRow(label = "Due date", value = "10 May 2026")
            Spacer(modifier = Modifier.height(8.dp))
            DashboardDetailRow(label = "Landlord", value = "Amit Sharma")

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onPayRent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.rentWalletColors.tenantOnPrimary
                )
            ) {
                Text(
                    text = "Pay Rent",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
private fun TenantRentStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Due in 3 days",
                        color = MaterialTheme.rentWalletColors.warning,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pay before 10 May to keep this month marked on time.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
                StatusPill(
                    text = "Action",
                    background = Color.White,
                    content = MaterialTheme.rentWalletColors.warning
                )
            }
        }
    }
}


@Composable
private fun PropertySummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Property",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Green View Residency",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Flat 4B, Salt Lake, Kolkata",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}


@Composable
private fun DashboardStatsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmallMetricCard(
            label = "Lease ends",
            value = "31 Mar",
            modifier = Modifier.weight(1f)
        )
        SmallMetricCard(
            label = "Deposit",
            value = "Rs. 35k",
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
private fun SmallMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.heightIn(min = 94.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun RecentPaymentsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Recent payments",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            PaymentPreviewRow(month = "April 2026", amount = "Rs. 18,500", status = "Paid")
            Spacer(modifier = Modifier.height(12.dp))
            PaymentPreviewRow(month = "March 2026", amount = "Rs. 18,500", status = "Paid")
        }
    }
}


@Composable
private fun PaymentSafetyNote() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.rentWalletColors.infoContainer, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Payment review",
                color = MaterialTheme.rentWalletColors.info,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Confirming marks the rent as paid and credits the landlord wallet in this app flow.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}


@Composable
private fun StatusSummaryChip(
    label: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.heightIn(min = 72.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun AlertsSection(
    title: String,
    alerts: List<AlertItem>,
    accent: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            alerts.forEachIndexed { index, alert ->
                AlertRow(alert = alert, accent = accent)
                if (index != alerts.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}


@Composable
private fun AlertRow(
    alert: AlertItem,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(alertBackground(alert.status, accent), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = alert.status.take(1),
                color = alertContent(alert.status, accent),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alert.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusPill(
                    text = alert.status,
                    background = alertBackground(alert.status, accent),
                    content = alertContent(alert.status, accent)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = alert.message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}


@Composable
private fun PaymentPreviewRow(
    month: String,
    amount: String,
    status: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = month,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = amount,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        StatusPill(
            text = status,
            background = MaterialTheme.rentWalletColors.successContainer,
            content = MaterialTheme.rentWalletColors.success
        )
    }
}


@Composable
private fun PaymentInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.2f)
        )
    }
}


@Composable
private fun DashboardDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


@Composable
private fun StatusPill(
    text: String,
    background: Color,
    content: Color
) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = content,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
private fun paymentStatusBackground(status: String): Color = when (status) {
    "Paid" -> MaterialTheme.rentWalletColors.successContainer
    "Pending" -> MaterialTheme.rentWalletColors.warningContainer
    "Failed" -> MaterialTheme.colorScheme.errorContainer
    else -> Color(0xFFF2F4F7)
}

@Composable
private fun paymentStatusContent(status: String): Color = when (status) {
    "Paid" -> MaterialTheme.rentWalletColors.success
    "Pending" -> MaterialTheme.rentWalletColors.warning
    "Failed" -> MaterialTheme.colorScheme.error
    else -> Color(0xFF475467)
}

private fun receiptActionText(status: String): String = when (status) {
    "Paid" -> "Receipt generated and landlord wallet credited."
    "Pending" -> "Payment is due. Receipt will generate after payment."
    "Failed" -> "Payment failed. No wallet credit was completed."
    else -> "Payment details available."
}

@Composable
private fun alertBackground(status: String, accent: Color): Color = when (status) {
    "Paid", "Receipt" -> MaterialTheme.rentWalletColors.successContainer
    "Due", "Pending", "Action", "Processing" -> MaterialTheme.rentWalletColors.warningContainer
    "Failed" -> MaterialTheme.colorScheme.errorContainer
    else -> accent.copy(alpha = 0.12f)
}

@Composable
private fun alertContent(status: String, accent: Color): Color = when (status) {
    "Paid", "Receipt" -> MaterialTheme.rentWalletColors.success
    "Due", "Pending", "Action", "Processing" -> MaterialTheme.rentWalletColors.warning
    "Failed" -> MaterialTheme.colorScheme.error
    else -> accent
}

@Composable
private fun TenantBottomBar(
    selectedTab: String,
    onDashboard: () -> Unit,
    onPayments: () -> Unit,
    onProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BottomMenuItem(
            title = "Dashboard",
            selected = selectedTab == "Dashboard",
            onClick = onDashboard,
            modifier = Modifier.weight(1f)
        )
        BottomMenuItem(
            title = "Payments",
            selected = selectedTab == "Payments",
            onClick = onPayments,
            modifier = Modifier.weight(1f)
        )
        BottomMenuItem(
            title = "Profile",
            selected = selectedTab == "Profile",
            onClick = onProfile,
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
private fun LandlordBottomBar(
    selectedTab: String,
    onDashboard: () -> Unit,
    onTenants: () -> Unit,
    onProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BottomMenuItem(
            title = "Dashboard",
            selected = selectedTab == "Dashboard",
            selectedColor = MaterialTheme.rentWalletColors.landlordPrimary,
            onClick = onDashboard,
            modifier = Modifier.weight(1f)
        )
        BottomMenuItem(
            title = "Tenants",
            selected = selectedTab == "Tenants",
            selectedColor = MaterialTheme.rentWalletColors.landlordPrimary,
            onClick = onTenants,
            modifier = Modifier.weight(1f)
        )
        BottomMenuItem(
            title = "Profile",
            selected = selectedTab == "Profile",
            selectedColor = MaterialTheme.rentWalletColors.landlordPrimary,
            onClick = onProfile,
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
private fun BottomMenuItem(
    title: String,
    selected: Boolean,
    selectedColor: Color = MaterialTheme.rentWalletColors.tenantPrimary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) selectedColor.copy(alpha = 0.12f) else Color(0xFFF2F4F7)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = if (selected) selectedColor else Color(0xFF475467),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun LandlordPlaceholderScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Landlord Dashboard",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "This side will be added after the tenant rent flow is ready.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.rentWalletColors.landlordPrimary,
                contentColor = Color.White
            )
        ) {
            Text(text = "Back to Login")
        }
    }
}




@Composable
private fun LoginHero(role: UserRole) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = role.accent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Text(
                text = role.title,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = role.description,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                lineHeight = 23.sp
            )
        }
    }
}


@Composable
private fun loginTextFieldColors(accent: Color) = TextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    cursorColor = accent,
    focusedIndicatorColor = accent,
    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = accent,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
)
