package com.thebackendguy.myandroidtestapp.ui.landing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thebackendguy.myandroidtestapp.ui.theme.rentWalletColors

enum class LandingRole {
    Tenant,
    Landlord
}

@Composable
fun LandingScreen(
    onRoleSelected: (LandingRole) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            LandingHeader()
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            HeroSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            val colors = MaterialTheme.rentWalletColors

            RoleSelectionCard(
                icon = Icons.Filled.Person,
                title = "Tenant",
                description = "Pay your rent easily and track history.",
                buttonText = "Continue as Tenant",
                accentColor = colors.tenantPrimary,
                onButtonTextColor = colors.tenantOnPrimary,
                onClick = { onRoleSelected(LandingRole.Tenant) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleSelectionCard(
                icon = Icons.Filled.Home,
                title = "Landlord",
                description = "Collect payments and manage properties.",
                buttonText = "Continue as Landlord",
                accentColor = colors.landlordPrimary,
                onButtonTextColor = colors.landlordOnPrimary,
                onClick = { onRoleSelected(LandingRole.Landlord) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeatureHighlightItem(
                    icon = Icons.Filled.Shield,
                    label = "Secure Rent Payments",
                    modifier = Modifier.weight(1f)
                )
                FeatureHighlightItem(
                    icon = Icons.Filled.Receipt,
                    label = "Digital Receipts",
                    modifier = Modifier.weight(1f)
                )
                FeatureHighlightItem(
                    icon = Icons.Filled.History,
                    label = "Wallet & Payment History",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        TrustFooter(
            modifier = Modifier.navigationBarsPadding()
        )
    }
}
