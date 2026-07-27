package com.thebackendguy.myandroidtestapp.ui.landing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
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
        LandingHeader()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            HeroSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            val colors = MaterialTheme.rentWalletColors

            RoleSelectionCard(
                icon = Icons.Filled.Person,
                imageIcon = Icons.Filled.Home,
                title = "Tenant",
                description = "Pay your rent easily and track history.",
                accentColor = colors.tenantPrimary,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = { onRoleSelected(LandingRole.Tenant) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleSelectionCard(
                icon = Icons.Filled.Home,
                imageIcon = Icons.Filled.Business,
                title = "Landlord",
                description = "Collect payments and manage properties.",
                accentColor = colors.landlordPrimary,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                onClick = { onRoleSelected(LandingRole.Landlord) }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }

        LandingFooter()
    }
}
