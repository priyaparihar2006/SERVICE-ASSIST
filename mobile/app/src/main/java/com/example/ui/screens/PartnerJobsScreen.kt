package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.UserProfile
import com.example.ui.components.hideStatusBarOnScroll

// Light Green & White Minimalistic Theme Palette
private val brandGreen = Color(0xFF009051)
private val brandDarkGreen = Color(0xFF0F5132)
private val brandMintBg = Color(0xFFEEF9F3)
private val brandMintPill = Color(0xFFE9F8F0)
private val brandBorder = Color(0xFFE5E7EB)
private val brandNoteBg = Color(0xFFF7FBF9)
private val textMain = Color(0xFF1E2022)
private val textSub = Color(0xFF6B7280)
private val textMuted = Color(0xFF9CA3AF)

@Composable
fun PartnerJobsScreen(
    partnerProfile: UserProfile,
    bookings: List<Booking>,
    onAdvanceStatus: (Long, BookingStatus) -> Unit,
    onSyncClick: () -> Unit,
    onOpenChat: (Booking) -> Unit = {},
    onCollectPayment: (Long) -> Unit = {},
    statusUpdateErrorFlow: kotlinx.coroutines.flow.SharedFlow<String>? = null,
    onAdvanceStatusWithCallback: ((Long, BookingStatus, (Boolean) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var isPartnerOnline by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Active Duties, 1: Completed

    var showOtpDialogForBooking by remember { mutableStateOf<Booking?>(null) }
    var enteredOtp by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(statusUpdateErrorFlow) {
        statusUpdateErrorFlow?.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val activeJobs = bookings.filter {
        it.status != BookingStatus.COMPLETED && it.status != BookingStatus.CANCELLED
    }
    val completedJobs = bookings.filter {
        it.status == BookingStatus.COMPLETED
    }

    val displayJobs = if (selectedTab == 0) activeJobs else completedJobs

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFCFA))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hideStatusBarOnScroll(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // ================= 1. TOP PARTNER AVAILABILITY & DUTY HEADER (MATCHES STATUS BAR) =================
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFF009051),
                                    Color(0xFF008249),
                                    Color(0xFF007542)
                                )
                            ),
                            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp, bottom = 20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top Row: Avatar + Name + PRO + Online Subtitle + Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar Circle
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = partnerProfile.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp
                                            ),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White.copy(alpha = 0.25f)
                                        ) {
                                            Text(
                                                text = "PRO",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isPartnerOnline) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.6f))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isPartnerOnline) "Online • Receiving Requests" else "Offline • Duty Paused",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            ),
                                            color = Color.White.copy(alpha = 0.95f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Switch(
                                checked = isPartnerOnline,
                                onCheckedChange = { isPartnerOnline = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = brandGreen,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.35f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Bottom Row: Location Zone info + Sync icon / Chevron
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSyncClick() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${partnerProfile.city} (${partnerProfile.locality})",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Refresh",
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ================= 2. TAB SELECTION (SEGMENTED PILL STYLE) =================
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF1F5F2),
                        border = BorderStroke(1.dp, brandBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Active Duties Tab
                            val isActiveSelected = selectedTab == 0
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedTab = 0 },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isActiveSelected) Color.White else Color.Transparent,
                                shadowElevation = if (isActiveSelected) 2.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Active Duties",
                                        fontWeight = if (isActiveSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.5.sp,
                                        color = if (isActiveSelected) brandDarkGreen else textSub
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isActiveSelected) brandMintBg else Color.White.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = "${activeJobs.size}",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActiveSelected) brandGreen else textSub
                                        )
                                    }
                                }
                            }

                            // Completed Tab
                            val isCompletedSelected = selectedTab == 1
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedTab = 1 },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCompletedSelected) Color.White else Color.Transparent,
                                shadowElevation = if (isCompletedSelected) 2.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Completed",
                                        fontWeight = if (isCompletedSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.5.sp,
                                        color = if (isCompletedSelected) brandDarkGreen else textSub
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCompletedSelected) brandMintBg else Color.White.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = "${completedJobs.size}",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCompletedSelected) brandGreen else textSub
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ================= 3. JOBS LIST / EMPTY STATE =================
            if (displayJobs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(brandMintBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = brandGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (selectedTab == 0) "No Active Duties" else "No Completed Jobs Yet",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = textMain
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (selectedTab == 0) "Stay online to receive requests in ${partnerProfile.city}" else "Your completed service history will appear here",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                color = textSub
                            )
                        }
                    }
                }
            } else {
                items(displayJobs, key = { it.id }) { booking ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        PartnerJobCard(
                            booking = booking,
                            onAdvanceStatus = { bId, currentSt ->
                                if (currentSt == BookingStatus.ARRIVED) {
                                    showOtpDialogForBooking = booking
                                    enteredOtp = ""
                                    otpError = null
                                } else {
                                    onAdvanceStatus(bId, currentSt)
                                }
                            },
                            onNavigate = { addr ->
                                val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(addr)}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                context.startActivity(mapIntent)
                            },
                            onOpenChat = onOpenChat,
                            onCollectPayment = onCollectPayment
                        )
                    }
                }
            }
        }

        // ================= 4. OTP VERIFICATION DIALOG =================
        showOtpDialogForBooking?.let { booking ->
            AlertDialog(
                onDismissRequest = { showOtpDialogForBooking = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = brandGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Verify Customer OTP",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Ask customer for the 4-digit start OTP to begin ${booking.serviceName}.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            color = textSub
                        )

                        OutlinedTextField(
                            value = enteredOtp,
                            onValueChange = { if (it.length <= 4) enteredOtp = it },
                            label = { Text("4-Digit OTP") },
                            isError = otpError != null,
                            supportingText = otpError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                },
                confirmButton = {
                    var isVerifying by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            if (enteredOtp == (booking.startOtp ?: "1234")) {
                                if (onAdvanceStatusWithCallback != null) {
                                    isVerifying = true
                                    onAdvanceStatusWithCallback(booking.id, BookingStatus.ARRIVED) { success ->
                                        isVerifying = false
                                        if (success) {
                                            showOtpDialogForBooking = null
                                        } else {
                                            otpError = "Couldn't update status on server — please check network & retry"
                                        }
                                    }
                                } else {
                                    onAdvanceStatus(booking.id, BookingStatus.ARRIVED)
                                    showOtpDialogForBooking = null
                                }
                            } else {
                                otpError = "Incorrect OTP! Please check with customer"
                            }
                        },
                        enabled = !isVerifying,
                        colors = ButtonDefaults.buttonColors(containerColor = brandGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Verify & Start Work", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showOtpDialogForBooking = null },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = textSub)
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PartnerJobCard(
    booking: Booking,
    onAdvanceStatus: (Long, BookingStatus) -> Unit,
    onNavigate: (String) -> Unit,
    onOpenChat: (Booking) -> Unit,
    onCollectPayment: (Long) -> Unit = {}
) {
    val isCompleted = booking.status == BookingStatus.COMPLETED

    val statusLabel = when (booking.status) {
        BookingStatus.ASSIGNED -> "Job Assigned"
        BookingStatus.ON_THE_WAY -> "On the Way"
        BookingStatus.ARRIVED -> "Arrived at Doorstep"
        BookingStatus.STARTED -> "In Progress"
        BookingStatus.COMPLETED -> "Completed"
        BookingStatus.CANCELLED -> "Cancelled"
        else -> "Job Assigned"
    }

    val serviceImageRes = getServiceImageDrawable(booking.serviceId, booking.serviceName)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, brandBorder),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Status Pill Badge on Left, Price on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when (booking.status) {
                        BookingStatus.COMPLETED -> Color(0xFFF1F5F9)
                        BookingStatus.CANCELLED -> Color(0xFFFEE2E2)
                        else -> brandMintPill
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when (booking.status) {
                                        BookingStatus.COMPLETED -> textSub
                                        BookingStatus.CANCELLED -> Color(0xFFDC2626)
                                        else -> brandGreen
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (booking.status) {
                                BookingStatus.COMPLETED -> textMain
                                BookingStatus.CANCELLED -> Color(0xFFDC2626)
                                else -> brandDarkGreen
                            }
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFDCFCE7))
                ) {
                    Text(
                        text = "₹${booking.totalAmount}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        ),
                        color = brandGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body: Service Image on Left + Service Name, Location & Time on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Service Product Image
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF2F9F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = serviceImageRes),
                        contentDescription = booking.serviceName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Service Title
                    Text(
                        text = booking.serviceName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            lineHeight = 19.sp
                        ),
                        color = textMain,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Address with Pin
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = brandGreen,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = booking.addressText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            color = textSub,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Schedule Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${booking.scheduledDate}  •  ${booking.scheduledTime}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = textSub
                        )
                    }
                }
            }

            // Client Note Box
            val clientNote = booking.specialNotes.ifBlank {
                "Ring bell twice, indoor split unit in master bedroom"
            }
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = brandNoteBg,
                border = BorderStroke(1.dp, Color(0xFFEDF4F0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = brandGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = clientNote,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = textSub,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Buttons for Active Jobs
            if (!isCompleted) {
                Spacer(modifier = Modifier.height(14.dp))

                // View Map & Message Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigate(booking.addressText) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, brandBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = brandGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "View Map",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = textMain
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenChat(booking) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, brandBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = brandGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Message",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = textMain
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Job Lifecycle Step CTA Button
                Button(
                    onClick = {
                        if (booking.status == BookingStatus.STARTED || booking.status == BookingStatus.AWAITING_PAYMENT) {
                            onCollectPayment(booking.id)
                        } else {
                            onAdvanceStatus(booking.id, booking.status)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (booking.status == BookingStatus.AWAITING_PAYMENT) Color(0xFFD97706) else brandGreen
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_partner_advance_job")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when (booking.status) {
                                BookingStatus.ASSIGNED -> Icons.Default.Navigation
                                BookingStatus.ON_THE_WAY -> Icons.Default.LocationOn
                                BookingStatus.ARRIVED -> Icons.Default.Key
                                BookingStatus.STARTED, BookingStatus.AWAITING_PAYMENT -> Icons.Default.CheckCircle
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (booking.status) {
                                BookingStatus.ASSIGNED -> "Start Travel to Customer"
                                BookingStatus.ON_THE_WAY -> "I Have Arrived at Doorstep"
                                BookingStatus.ARRIVED -> "Verify Customer OTP (${booking.startOtp})"
                                BookingStatus.STARTED -> "Complete Job & Collect ₹${booking.totalAmount}"
                                BookingStatus.AWAITING_PAYMENT -> "Awaiting Payment — Tap to Collect ₹${booking.totalAmount}"
                                else -> "Job Complete"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun getServiceImageDrawable(serviceId: String, serviceName: String): Int {
    val nameLower = serviceName.lowercase()
    val idLower = serviceId.lowercase()
    return when {
        idLower.contains("ac") || nameLower.contains("ac") || nameLower.contains("air condition") -> R.drawable.img_ac_repair
        idLower.contains("water") || nameLower.contains("water") || nameLower.contains("tank") || idLower.contains("ro") -> R.drawable.img_ro_water_work
        idLower.contains("electric") || nameLower.contains("electric") || nameLower.contains("wiring") -> R.drawable.img_electrician_work
        idLower.contains("plumb") || nameLower.contains("plumb") || nameLower.contains("leak") -> R.drawable.img_plumber_work
        idLower.contains("clean") || nameLower.contains("clean") -> R.drawable.img_cleaning_pro
        idLower.contains("bath") || nameLower.contains("bath") -> R.drawable.img_bathroom_cleaner
        idLower.contains("kitchen") || nameLower.contains("kitchen") -> R.drawable.img_kitchen_clean_work
        idLower.contains("sofa") || nameLower.contains("sofa") -> R.drawable.img_sofa_clean_work
        idLower.contains("paint") || nameLower.contains("paint") -> R.drawable.img_painter_work
        idLower.contains("carpent") || nameLower.contains("carpent") -> R.drawable.img_carpenter_work
        idLower.contains("pest") || nameLower.contains("pest") -> R.drawable.img_pest_control_work
        idLower.contains("fridge") || nameLower.contains("fridge") || nameLower.contains("refrigerat") -> R.drawable.img_fridge_repair
        idLower.contains("wash") || nameLower.contains("wash") -> R.drawable.img_washing_machine
        idLower.contains("salon") || nameLower.contains("salon") || nameLower.contains("facial") -> R.drawable.img_facial_cleanup
        idLower.contains("hair") || nameLower.contains("hair") -> R.drawable.img_haircut_styling
        idLower.contains("nail") || nameLower.contains("nail") -> R.drawable.img_nail_art
        else -> R.drawable.img_hero_service
    }
}
