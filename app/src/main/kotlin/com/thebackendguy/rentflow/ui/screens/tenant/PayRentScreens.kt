package com.thebackendguy.rentflow.ui.screens.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.thebackendguy.rentflow.data.Demo
import com.thebackendguy.rentflow.data.RentPayment
import com.thebackendguy.rentflow.data.inr
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.DarkCard
import com.thebackendguy.rentflow.ui.components.DarkCardHeader
import com.thebackendguy.rentflow.ui.components.DarkMetric
import com.thebackendguy.rentflow.ui.components.DashedDivider
import com.thebackendguy.rentflow.ui.components.Detail
import com.thebackendguy.rentflow.ui.components.DetailRows
import com.thebackendguy.rentflow.ui.components.DetailStyle
import com.thebackendguy.rentflow.ui.components.IconTile
import com.thebackendguy.rentflow.ui.components.InfoCard
import com.thebackendguy.rentflow.ui.components.Pill
import com.thebackendguy.rentflow.ui.components.PrimaryButton
import com.thebackendguy.rentflow.ui.components.RfCard
import com.thebackendguy.rentflow.ui.components.SectionHead
import com.thebackendguy.rentflow.ui.components.TitledCard
import com.thebackendguy.rentflow.ui.components.TonalButton
import com.thebackendguy.rentflow.ui.components.softShadow
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.Shell
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType

@Composable
fun PayRentScreen(payment: RentPayment, shell: Shell, onBack: () -> Unit, onConfirm: () -> Unit) {
    AppPage(
        topBar = { shell.TopBar("Pay Rent", onBack = onBack) },
        bottomBar = {
            Box(
                Modifier
                    .zIndex(1f)
                    .shadow(12.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), ambientColor = Rf.Shadow, spotColor = Rf.Shadow)
                    .background(Rf.Lowest, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                PrimaryButton("Confirm Payment • ${inr(payment.amount)}", onClick = onConfirm)
            }
        }
    ) {
        DarkCard {
            DarkCardHeader("${payment.month} rent") {
                Pill("Due ${payment.shortDate}", background = Rf.AmberText.copy(alpha = 0.18f), content = Rf.AmberText, dot = true, dotColor = Rf.AmberDot)
            }
            DarkMetric(label = "Rent amount", value = inr(payment.amount), note = "Review the details before you confirm.")
        }

        TitledCard("Rent details") {
            DetailRows(
                listOf(
                    Detail("Property", Demo.PROPERTY),
                    Detail("Address", Demo.ADDRESS),
                    Detail("Landlord", Demo.LANDLORD_NAME),
                    Detail("Due date", payment.dateLabel.removePrefix("Due ")),
                    Detail("Credited to", "Landlord wallet")
                )
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHead("Payment method")
            val shape = RoundedCornerShape(12.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .softShadow(shape)
                    .clip(shape)
                    .background(Rf.Lowest)
                    .border(2.dp, Rf.Primary, shape)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconTile(Lucide.Landmark, background = Rf.PrimaryFixed, tint = Rf.Primary, size = 40.dp, radius = 12.dp, iconSize = 20.dp)
                Column(Modifier.weight(1f)) {
                    Text("UPI / Bank payment", style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                    Text("Pay using your preferred bank or UPI app.", style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
                }
                LIcon(Lucide.CircleCheck, size = 22.dp, tint = Rf.Primary)
            }
        }

        TitledCard("Payment summary") {
            DetailRows(
                listOf(
                    Detail("Monthly rent", inr(payment.amount)),
                    Detail("Service fee", inr(0)),
                    Detail("Late fee", inr(0)),
                    Detail("Processing", "Instant credit")
                )
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(8.dp)).background(Rf.Low).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total payable", style = RfType.LabelMd, color = Rf.OnSurfaceVariant)
                Text(inr(payment.amount), style = RfType.HeadlineSm.copy(fontWeight = FontWeight.Bold), color = Rf.Primary)
            }
        }

        InfoCard(Lucide.Info, "Payment review", "Confirming marks the rent as paid and credits the landlord wallet.", iconColor = Rf.Primary)
    }
}

@Composable
fun PaymentSuccessScreen(payment: RentPayment, onBackToDashboard: () -> Unit, onViewReceipt: () -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Rf.Surface).statusBarsPadding().navigationBarsPadding()
    ) {
        val minHeight = maxHeight
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = minHeight).padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Rf.MintSoft.copy(alpha = 0.18f))
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(Rf.MintSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        LIcon(Lucide.Check, size = 36.dp, tint = Rf.Secondary)
                    }
                    Text("Payment successful", style = RfType.HeadlineLg, color = Rf.OnSurface, modifier = Modifier.padding(top = 8.dp))
                    Text(
                        "${Demo.LANDLORD_NAME}’s landlord wallet has been credited.",
                        style = RfType.BodyMd, color = Rf.OnSurfaceVariant, textAlign = TextAlign.Center
                    )
                }

                RfCard {
                    Column(
                        Modifier.fillMaxWidth().padding(bottom = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Amount paid", style = RfType.LabelSm, color = Rf.Outline)
                        Text(inr(payment.amount), style = RfType.Metric, color = Rf.OnSurface)
                    }
                    DashedDivider(Modifier.padding(bottom = 14.dp))
                    DetailRows(
                        listOf(
                            Detail("Transaction ID", payment.transactionId, DetailStyle.Mono),
                            Detail("Paid on", payment.dateLabel.removePrefix("Paid ")),
                            Detail("Payment method", payment.method),
                            Detail("Wallet credit", "Completed", DetailStyle.SuccessPill),
                            Detail("Receipt status", payment.receiptStatus, DetailStyle.SuccessPill)
                        )
                    )
                }

                InfoCard(Lucide.ReceiptText, "Receipt ready", "Receipt ${payment.transactionId} is saved in Payment History.", iconColor = Rf.Primary)

                Spacer(Modifier.weight(1f))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("Back to Dashboard", onClick = onBackToDashboard, showArrow = false)
                    TonalButton("View receipt", onClick = onViewReceipt)
                }
            }
        }
    }
}
