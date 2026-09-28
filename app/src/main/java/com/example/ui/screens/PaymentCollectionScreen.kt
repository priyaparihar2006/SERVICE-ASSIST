package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.example.ui.components.stableStatusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.hideStatusBarOnScroll
import com.example.ui.theme.ServoraTheme
import com.example.ui.viewmodel.PaymentMethodTab
import com.example.ui.viewmodel.PaymentUiState
import com.example.ui.viewmodel.PaymentViewModel

private val BrandDarkGreen = Color(0xFF0F5132)
private val BrandMintBg = Color(0xFFEEF9F3)



private val BrandBorder = Color(0xFFE5E7EB)
private val TextMain = Color(0xFF1E2022)
private val TextSub = Color(0xFF6B7280)

@Composable
fun PaymentCollectionScreen(
    bookingId: Long,
    viewModel: PaymentViewModel,
    onBackClick: () -> Unit,
    onPaymentCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = ServoraTheme.colors.isDark
    val uiState by viewModel.uiState.collectAsState()
    var showCashConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(bookingId) {
        viewModel.loadBooking(bookingId)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is PaymentUiState.Loading, is PaymentUiState.Idle -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ServoraTheme.colors.primary)
                }
            }

            is PaymentUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .stableStatusBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isDark) ServoraTheme.colors.dangerContainer else Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = if (isDark) ServoraTheme.colors.danger else Color(0xFFDC2626),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Couldn't Prepare Payment",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        color = ServoraTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ServoraTheme.colors.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
                        ) {
                            Text("Go Back", color = ServoraTheme.colors.textSecondary, fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = { viewModel.loadBooking(state.bookingId) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ServoraTheme.colors.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White, /* theme-invariant */
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", fontWeight = FontWeight.Medium, color = Color.White /* theme-invariant */)
                        }
                    }
                }
            }

            is PaymentUiState.Success -> {
                LaunchedEffect(state) {
                    kotlinx.coroutines.delay(2800L)
                    onPaymentCompleted()
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .stableStatusBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (isDark) ServoraTheme.colors.successContainer else BrandMintBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = ServoraTheme.colors.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Payment Collected!",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                        color = ServoraTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val amountText = state.booking?.totalAmount?.let { "₹$it" } ?: ""
                    val methodLabel = if (state.method == "CASH") "Cash received at doorstep" else "UPI payment verified (${state.reference ?: "UTR"})"

                    Text(
                        text = "$amountText $methodLabel".trim(),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = ServoraTheme.colors.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Job has been successfully marked as completed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ServoraTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = onPaymentCompleted,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ServoraTheme.colors.primary)
                    ) {
                        Text(
                            text = "Done",
                            fontWeight = FontWeight.Medium,
                            color = Color.White /* theme-invariant */
                        )
                    }
                }
            }

            is PaymentUiState.Ready -> {
                val booking = state.booking

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .stableStatusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .hideStatusBarOnScroll()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = 90.dp)
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) ServoraTheme.colors.successContainer else BrandMintBg)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ServoraTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Collect Payment",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp
                                ),
                                color = ServoraTheme.colors.textPrimary
                            )
                            Text(
                                text = "Job ID: ${booking.bookingCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ServoraTheme.colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ================= 1. AMOUNT CARD =================
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) ServoraTheme.colors.successContainer else BrandMintBg,
                        border = BorderStroke(1.dp, if (isDark) ServoraTheme.colors.cardBorder else Color(0xFFC7EBD8))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "FINAL AMOUNT DUE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp
                                ),
                                color = if (isDark) ServoraTheme.colors.onSuccessContainer else BrandDarkGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "₹${booking.totalAmount}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 38.sp
                                ),
                                color = if (isDark) ServoraTheme.colors.onSuccessContainer else BrandDarkGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${booking.serviceName} • ${booking.packageName}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = ServoraTheme.colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Error Message Banner if any
                    if (state.errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) ServoraTheme.colors.dangerContainer else Color(0xFFFEF2F2)),
                            border = BorderStroke(1.dp, if (isDark) ServoraTheme.colors.danger else Color(0xFFFCA5A5))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = if (isDark) ServoraTheme.colors.danger else Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) ServoraTheme.colors.onDangerContainer else Color(0xFFB91C1C),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Retry",
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) ServoraTheme.colors.danger else BrandDarkGreen,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.clearError()
                                            viewModel.loadBooking(booking.id)
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ================= 2. PAYMENT METHOD SELECTOR TABS =================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) ServoraTheme.colors.cardBackgroundSubtle else Color(0xFFF1F5F2))
                            .padding(4.dp)
                    ) {
                        // Cash Tab
                        val isCash = state.selectedTab == PaymentMethodTab.CASH
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.selectTab(PaymentMethodTab.CASH) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCash) (if (isDark) ServoraTheme.colors.cardBackground else Color.White) else Color.Transparent,
                            shadowElevation = if (isCash) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = if (isCash) (ServoraTheme.colors.primary) else ServoraTheme.colors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Cash at Door",
                                    fontWeight = if (isCash) FontWeight.Medium else FontWeight.Medium,
                                    fontSize = 13.5.sp,
                                    color = if (isCash) ServoraTheme.colors.textPrimary else ServoraTheme.colors.textSecondary
                                )
                            }
                        }

                        // UPI QR Tab
                        val isUpi = state.selectedTab == PaymentMethodTab.UPI
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.selectTab(PaymentMethodTab.UPI) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isUpi) (if (isDark) ServoraTheme.colors.cardBackground else Color.White) else Color.Transparent,
                            shadowElevation = if (isUpi) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = if (isUpi) (ServoraTheme.colors.primary) else ServoraTheme.colors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "UPI QR Code",
                                    fontWeight = if (isUpi) FontWeight.Medium else FontWeight.Medium,
                                    fontSize = 13.5.sp,
                                    color = if (isUpi) ServoraTheme.colors.textPrimary else ServoraTheme.colors.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ================= 3. TAB CONTENT =================
                    when (state.selectedTab) {
                        PaymentMethodTab.CASH -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) ServoraTheme.colors.successContainer else BrandMintBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = null,
                                            tint = ServoraTheme.colors.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Collect Cash from Customer",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                        color = ServoraTheme.colors.textPrimary
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Please collect exact cash amount of ₹${booking.totalAmount} from the customer before marking job as complete.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ServoraTheme.colors.textSecondary,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Button(
                                        onClick = { showCashConfirmDialog = true },
                                        enabled = !state.isSubmitting,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ServoraTheme.colors.primary)
                                    ) {
                                        if (state.isSubmitting) {
                                            CircularProgressIndicator(
                                                color = Color.White, /* theme-invariant */
                                                modifier = Modifier.size(22.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White, /* theme-invariant */
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Cash Received — ₹${booking.totalAmount}",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 15.sp,
                                                color = Color.White /* theme-invariant */
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        PaymentMethodTab.UPI -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Scan to Pay ₹${booking.totalAmount}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                        color = ServoraTheme.colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Payee: ${state.upiPayeeName} (${state.upiPayeeVpa})",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = if (isDark) ServoraTheme.colors.success else BrandDarkGreen
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // QR Code Container (CRITICAL: Must remain pure White background with untinted QR bitmap for scanner readability)
                                    Box(
                                        modifier = Modifier
                                            .size(260.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White /* theme-invariant */)
                                            .border(1.5.dp, if (isDark) ServoraTheme.colors.cardBorder else BrandBorder, RoundedCornerShape(16.dp))
                                            .padding(14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (state.qrBitmap != null) {
                                            Image(
                                                bitmap = state.qrBitmap,
                                                contentDescription = "UPI Payment QR Code",
                                                modifier = Modifier.fillMaxSize() // theme-invariant: no tint applied to allow reliable camera scanning
                                            )
                                        } else {
                                            CircularProgressIndicator(color = ServoraTheme.colors.primary)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "Accepts Google Pay, PhonePe, Paytm, BHIM & all UPI apps",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = ServoraTheme.colors.textSecondary,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // UTR Input Field
                                    OutlinedTextField(
                                        value = state.utr,
                                        onValueChange = { viewModel.onUtrChanged(it) },
                                        label = { Text("12-Digit UPI Reference (UTR)") },
                                        placeholder = { Text("e.g. 423589102476") },
                                        singleLine = true,
                                        isError = state.utrError != null,
                                        supportingText = {
                                            if (state.utrError != null) {
                                                Text(state.utrError, color = MaterialTheme.colorScheme.error)
                                            } else {
                                                Text(
                                                    "Ask the customer for the 12-digit UTR on their payment receipt",
                                                    color = ServoraTheme.colors.textSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            Text(
                                                text = "${state.utr.length}/12",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                color = if (state.utr.length == 12) (ServoraTheme.colors.primary) else ServoraTheme.colors.textSecondary,
                                                modifier = Modifier.padding(end = 12.dp)
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Receipt,
                                                contentDescription = null,
                                                tint = ServoraTheme.colors.primary
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = ServoraTheme.colors.textPrimary,
                                            unfocusedTextColor = ServoraTheme.colors.textPrimary,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedBorderColor = ServoraTheme.colors.primary,
                                            unfocusedBorderColor = ServoraTheme.colors.cardBorder,
                                            focusedLabelColor = ServoraTheme.colors.primary,
                                            unfocusedLabelColor = ServoraTheme.colors.textSecondary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { viewModel.collectUpi() },
                                        enabled = state.utr.length == 12 && !state.isSubmitting,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ServoraTheme.colors.primary)
                                    ) {
                                        if (state.isSubmitting) {
                                            CircularProgressIndicator(
                                                color = Color.White, /* theme-invariant */
                                                modifier = Modifier.size(22.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White, /* theme-invariant */
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Confirm UPI Payment Received",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.5.sp,
                                                color = Color.White /* theme-invariant */
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Native Cash Confirmation Dialog
                if (showCashConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showCashConfirmDialog = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = { Text("Confirm Cash Collection", color = ServoraTheme.colors.textPrimary) },
                        text = {
                            Text("Confirm you have received ₹${booking.totalAmount} in cash from the customer?", color = ServoraTheme.colors.textSecondary)
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showCashConfirmDialog = false
                                    viewModel.collectCash()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ServoraTheme.colors.primary)
                            ) {
                                Text("Yes, Cash Received", color = Color.White /* theme-invariant */)
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { showCashConfirmDialog = false },
                                border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
                            ) {
                                Text("Cancel", color = ServoraTheme.colors.textSecondary)
                            }
                        }
                    )
                }
            }
        }
    }
}
