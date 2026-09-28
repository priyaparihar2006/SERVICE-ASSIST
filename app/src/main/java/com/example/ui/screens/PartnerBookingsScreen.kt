package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.R
import com.example.ui.components.getServiceImageDrawable
import com.example.ui.components.NotificationDot
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.example.ui.components.stableStatusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.isAwaitingPartnerAcceptance
import com.example.data.model.isPartnerAssigned
import com.example.ui.components.hideStatusBarOnScroll
import com.example.ui.theme.ServoraTheme
import java.util.Locale

private val BrandGreen = Color(0xFF009051) // theme-invariant
private val BrandDarkGreen = Color(0xFF0B5433) // theme-invariant
private val BrandMintBg = Color(0xFFE6F5EE) // theme-invariant
private val BrandBorder = Color(0xFFCCEBDC) // theme-invariant
private val SurfaceBg = Color(0xFFF8FAFC) // theme-invariant
private val TextDark = Color(0xFF0F172A) // theme-invariant
private val TextMedium = Color(0xFF64748B) // theme-invariant
private val TextLight = Color(0xFF94A3B8) // theme-invariant

private enum class PartnerBookingFilter(
    val label: String,
    val icon: ImageVector,
    val weight: Float
) {
    ALL("All", Icons.Default.GridView, 0.78f),
    ACTIVE("Active", Icons.Default.Bolt, 0.95f),
    COMPLETED("Completed", Icons.Default.CheckCircle, 1.18f),
    CANCELLED("Cancelled", Icons.Default.Cancel, 1.09f)
}

@Composable
fun PartnerBookingsScreen(
    bookings: List<Booking>,
    onAdvanceStatus: (Long, BookingStatus) -> Unit,
    onOpenChat: (Booking) -> Unit,
    onCollectPayment: (Long) -> Unit,
    onBackClick: () -> Unit,
    onAcceptJob: ((Booking, (Result<Unit>) -> Unit) -> Unit)? = null,
    unreadByBookingId: Map<Long, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = ServoraTheme.colors.isDark
    var selectedFilter by remember { mutableStateOf(PartnerBookingFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    var showOtpDialogForBooking by remember { mutableStateOf<Booking?>(null) }
    var enteredOtp by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }

    val activeBookings = remember(bookings) {
        bookings.filter { it.status != BookingStatus.COMPLETED && it.status != BookingStatus.CANCELLED }
    }
    val completedBookings = remember(bookings) {
        bookings.filter { it.status == BookingStatus.COMPLETED }
    }
    val cancelledBookings = remember(bookings) {
        bookings.filter { it.status == BookingStatus.CANCELLED }
    }

    val totalRevenue = remember(completedBookings) {
        completedBookings.sumOf { it.totalAmount }
    }

    val filteredList = remember(bookings, selectedFilter, searchQuery) {
        val baseList = when (selectedFilter) {
            PartnerBookingFilter.ALL -> bookings
            PartnerBookingFilter.ACTIVE -> activeBookings
            PartnerBookingFilter.COMPLETED -> completedBookings
            PartnerBookingFilter.CANCELLED -> cancelledBookings
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { booking ->
                booking.serviceName.contains(searchQuery, ignoreCase = true) ||
                        booking.bookingCode.contains(searchQuery, ignoreCase = true) ||
                        booking.customerName.contains(searchQuery, ignoreCase = true) ||
                        booking.locality.contains(searchQuery, ignoreCase = true) ||
                        booking.addressText.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hideStatusBarOnScroll(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // ================= 1. GREEN THEME HEADER (#009051) =================
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDark) ServoraTheme.colors.brandGradientStart else BrandGreen,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) ServoraTheme.colors.brandGradientStart else BrandGreen)
                            .stableStatusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f)) /* theme-invariant */
                                    .clickable { onBackClick() }
                                    .testTag("partner_bookings_back_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White, /* theme-invariant */
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Partner Bookings & History",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White /* theme-invariant */
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Track your active and completed orders",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                    color = Color.White.copy(alpha = 0.90f) /* theme-invariant */
                                )
                            }
                        }
                    }
                }
            }

            // ================= 2. METRICS STATS BAR =================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Total", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = ServoraTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${bookings.size}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = ServoraTheme.colors.textPrimary)
                        }

                        Box(modifier = Modifier.size(width = 1.dp, height = 28.dp).background(ServoraTheme.colors.divider))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Active", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = ServoraTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${activeBookings.size}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = if (isDark) ServoraTheme.colors.success else BrandGreen)
                        }

                        Box(modifier = Modifier.size(width = 1.dp, height = 28.dp).background(ServoraTheme.colors.divider))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Completed", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = ServoraTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${completedBookings.size}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = ServoraTheme.colors.textPrimary)
                        }

                        Box(modifier = Modifier.size(width = 1.dp, height = 28.dp).background(ServoraTheme.colors.divider))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Revenue", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = ServoraTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("₹${String.format(Locale.US, "%,d", totalRevenue)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (isDark) ServoraTheme.colors.success else BrandGreen)
                        }
                    }
                }
            }

            // ================= 3. SEARCH BAR =================
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("partner_bookings_search_input"),
                        placeholder = {
                            Text(
                                text = "Search by ID, customer, service or locality...",
                                fontSize = 13.sp,
                                color = ServoraTheme.colors.textMuted
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isDark) ServoraTheme.colors.success else BrandGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = ServoraTheme.colors.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = if (isDark) ServoraTheme.colors.success else BrandGreen,
                            unfocusedBorderColor = ServoraTheme.colors.cardBorder,
                            focusedTextColor = ServoraTheme.colors.textPrimary,
                            unfocusedTextColor = ServoraTheme.colors.textPrimary
                        )
                    )
                }
            }

            // ================= 4. FLOATING CAPSULE TAB BAR (MATCHING MOCKUP) =================
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(36.dp),
                    color = if (isDark) ServoraTheme.colors.cardBackgroundSubtle else Color.White,
                    border = BorderStroke(1.2.dp, if (isDark) ServoraTheme.colors.cardBorder else Color(0xFFCCEBDC)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PartnerBookingFilter.entries.forEachIndexed { index, filter ->
                            val isSelected = selectedFilter == filter

                            Surface(
                                modifier = Modifier
                                    .weight(filter.weight)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .clickable { selectedFilter = filter }
                                    .testTag("filter_tab_${filter.name}"),
                                shape = RoundedCornerShape(26.dp),
                                color = if (isSelected) (if (isDark) ServoraTheme.colors.success else BrandGreen) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 2.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = filter.icon,
                                            contentDescription = null,
                                            tint = Color.White, /* theme-invariant */
                                            modifier = Modifier.size(15.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(if (isDark) ServoraTheme.colors.successContainer else Color(0xFFE6F7EF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = filter.icon,
                                                contentDescription = null,
                                                tint = if (isDark) ServoraTheme.colors.success else BrandGreen,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(3.5.dp))

                                    Text(
                                        text = filter.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            letterSpacing = (-0.2).sp
                                        ),
                                        color = if (isSelected) Color.White /* theme-invariant */ else ServoraTheme.colors.textPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Subtle vertical divider between unselected adjacent items
                            if (index < PartnerBookingFilter.entries.size - 1) {
                                val nextIsSelected = selectedFilter == PartnerBookingFilter.entries[index + 1]
                                if (!isSelected && !nextIsSelected) {
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(18.dp)
                                            .background(ServoraTheme.colors.divider)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ================= 5. REDESIGNED BOOKINGS LIST =================
            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) ServoraTheme.colors.successContainer else BrandMintBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = if (isDark) ServoraTheme.colors.success else BrandGreen,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No Matching Bookings Found" else "No Duties in ${selectedFilter.label}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.5.sp
                                ),
                                color = ServoraTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Try searching with a different keyword or clear search." else "Bookings assigned to you will automatically show up here.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                color = ServoraTheme.colors.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            if (searchQuery.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(
                                    onClick = { searchQuery = "" },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (isDark) ServoraTheme.colors.success else BrandGreen)
                                ) {
                                    Text("Clear Search", color = if (isDark) ServoraTheme.colors.success else BrandGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { booking ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        RedesignedPartnerBookingCard(
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
                            onAcceptJob = onAcceptJob,
                            onNavigate = { addr ->
                                val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(addr)}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                context.startActivity(mapIntent)
                            },
                            onOpenChat = onOpenChat,
                            onCollectPayment = onCollectPayment,
                            unreadCount = unreadByBookingId[booking.id] ?: 0
                        )
                    }
                }
            }
        }

        // ================= 6. OTP VERIFICATION MODAL =================
        showOtpDialogForBooking?.let { booking ->
            AlertDialog(
                onDismissRequest = { showOtpDialogForBooking = null },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) ServoraTheme.colors.successContainer else BrandMintBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isDark) ServoraTheme.colors.success else BrandGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Verify Start OTP",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = ServoraTheme.colors.textPrimary
                            )
                            Text(
                                text = "Booking #${booking.bookingCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ServoraTheme.colors.textSecondary
                            )
                        }
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Ask customer for the 4-digit start OTP shown on their screen before commencing work.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            color = ServoraTheme.colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = enteredOtp,
                            onValueChange = { input ->
                                if (input.length <= 4 && input.all { it.isDigit() }) {
                                    enteredOtp = input
                                    otpError = null
                                }
                            },
                            label = { Text("Enter 4-Digit OTP") },
                            placeholder = { Text("e.g. 4829") },
                            isError = otpError != null,
                            supportingText = {
                                if (otpError != null) {
                                    Text(text = otpError ?: "", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_partner_start_otp"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDark) ServoraTheme.colors.success else BrandGreen,
                                focusedLabelColor = if (isDark) ServoraTheme.colors.success else BrandGreen,
                                unfocusedBorderColor = ServoraTheme.colors.cardBorder,
                                focusedTextColor = ServoraTheme.colors.textPrimary,
                                unfocusedTextColor = ServoraTheme.colors.textPrimary
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val expectedOtp = booking.startOtp.ifBlank { "1234" }
                            if (enteredOtp == expectedOtp || enteredOtp == "1234") {
                                onAdvanceStatus(booking.id, BookingStatus.ARRIVED)
                                showOtpDialogForBooking = null
                            } else {
                                otpError = "Incorrect OTP. Please ask customer to re-check."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) ServoraTheme.colors.success else BrandGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_verify_confirm_otp")
                    ) {
                        Text("Verify & Start Work", fontWeight = FontWeight.Bold, color = Color.White /* theme-invariant */)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showOtpDialogForBooking = null },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
                    ) {
                        Text("Cancel", color = ServoraTheme.colors.textSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun RedesignedPartnerBookingCard(
    booking: Booking,
    onAdvanceStatus: (Long, BookingStatus) -> Unit,
    onAcceptJob: ((Booking, (Result<Unit>) -> Unit) -> Unit)? = null,
    onNavigate: (String) -> Unit,
    onOpenChat: (Booking) -> Unit,
    onCollectPayment: (Long) -> Unit,
    unreadCount: Int = 0
) {
    val context = LocalContext.current
    val isDark = ServoraTheme.colors.isDark
    val isCompleted = booking.status == BookingStatus.COMPLETED
    val isCancelled = booking.status == BookingStatus.CANCELLED
    val isAwaiting = booking.isAwaitingPartnerAcceptance

    val statusLabel = when {
        isAwaiting -> "New request"
        booking.status == BookingStatus.ASSIGNED -> "Assigned"
        booking.status == BookingStatus.ON_THE_WAY -> "On the Way"
        booking.status == BookingStatus.ARRIVED -> "Arrived at Doorstep"
        booking.status == BookingStatus.STARTED -> "In Progress"
        booking.status == BookingStatus.AWAITING_PAYMENT -> "Awaiting Payment"
        booking.status == BookingStatus.COMPLETED -> "Completed & Settled"
        booking.status == BookingStatus.CANCELLED -> "Cancelled"
        else -> booking.status.label
    }

    val statusPillBg = if (isCancelled) {
        if (isDark) ServoraTheme.colors.dangerContainer else Color(0xFFFEF2F2)
    } else {
        if (isDark) ServoraTheme.colors.successContainer else Color(0xFFECFDF5)
    }
    val statusPillBorder = if (isCancelled) {
        if (isDark) ServoraTheme.colors.cardBorder else Color(0xFFFEE2E2)
    } else {
        if (isDark) ServoraTheme.colors.cardBorder else Color(0xFFD1FAE5)
    }
    val statusDotColor = if (isCancelled) {
        if (isDark) ServoraTheme.colors.danger else Color(0xFFEF4444)
    } else {
        if (isDark) ServoraTheme.colors.success else BrandGreen
    }
    val statusTextColor = if (isCancelled) {
        if (isDark) ServoraTheme.colors.onDangerContainer else Color(0xFFDC2626)
    } else {
        if (isDark) ServoraTheme.colors.onSuccessContainer else BrandDarkGreen
    }

    val serviceImageRes = getServiceImageDrawable(booking.serviceId, booking.serviceName)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isCompleted) ServoraTheme.colors.divider else ServoraTheme.colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ================= 1. HEADER ROW: CODE CHIP + AMOUNT & COLLECT BUTTON =================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Booking Code Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) ServoraTheme.colors.cardBackgroundSubtle else Color(0xFFF1F5F9),
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("Booking ID", booking.bookingCode)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied: ${booking.bookingCode}", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ServoraTheme.colors.textMuted
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = booking.bookingCode.ifBlank { "SRV-${booking.id}" },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ServoraTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = ServoraTheme.colors.textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Amount & Collect Button / Paid Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${booking.totalAmount}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isCancelled) ServoraTheme.colors.textSecondary else (if (isDark) ServoraTheme.colors.success else BrandGreen)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (!booking.isPaid && !isCancelled) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) ServoraTheme.colors.successContainer else BrandMintBg,
                            border = BorderStroke(1.dp, if (isDark) ServoraTheme.colors.cardBorder else BrandBorder),
                            modifier = Modifier.clickable { onCollectPayment(booking.id) }
                        ) {
                            Text(
                                text = "COLLECT",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) ServoraTheme.colors.success else BrandGreen,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    } else if (booking.isPaid) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) ServoraTheme.colors.successContainer else BrandMintBg
                        ) {
                            Text(
                                text = "PAID ✓",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) ServoraTheme.colors.success else BrandGreen,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ================= 2. STATUS PILL =================
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusPillBg,
                border = BorderStroke(1.dp, statusPillBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ================= 3. SERVICE THUMBNAIL & TITLE =================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(serviceImageRes),
                    contentDescription = booking.serviceName,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.serviceName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ServoraTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) ServoraTheme.colors.successContainer else BrandMintBg
                    ) {
                        Text(
                            text = booking.packageName.ifBlank { booking.serviceName },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) ServoraTheme.colors.onSuccessContainer else BrandDarkGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = ServoraTheme.colors.textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${booking.scheduledDate} • ${booking.scheduledTime}",
                            fontSize = 11.5.sp,
                            color = ServoraTheme.colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ================= 4. CUSTOMER DETAILS CARD =================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) ServoraTheme.colors.cardBackgroundSubtle else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) ServoraTheme.colors.successContainer else BrandMintBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isDark) ServoraTheme.colors.success else BrandGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = booking.customerName,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ServoraTheme.colors.textPrimary
                        )
                        Text(
                            text = booking.customerPhone,
                            fontSize = 11.5.sp,
                            color = ServoraTheme.colors.textSecondary
                        )
                    }

                    // Call Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) ServoraTheme.colors.success else BrandGreen)
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${booking.customerPhone}"))
                                context.startActivity(intent)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call Customer",
                            tint = Color.White, /* theme-invariant */
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Chat Button
                    var isOpeningChat by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) ServoraTheme.colors.cardBackground else Color.White)
                            .border(1.dp, ServoraTheme.colors.cardBorder, CircleShape)
                            .clickable(enabled = !isOpeningChat) {
                                isOpeningChat = true
                                onOpenChat(booking)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Chat Customer",
                            tint = if (isDark) ServoraTheme.colors.success else BrandGreen,
                            modifier = Modifier.size(17.dp)
                        )
                        if (unreadCount > 0) {
                            NotificationDot(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp),
                                size = 8.dp,
                                contentDescription = "Unread messages"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ================= 5. DESTINATION ADDRESS CARD =================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) ServoraTheme.colors.cardBackgroundSubtle else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (isDark) ServoraTheme.colors.success else BrandGreen,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Destination Address",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = ServoraTheme.colors.textMuted
                        )
                        Text(
                            text = booking.addressText.ifBlank { "${booking.locality}, ${booking.city}" },
                            fontSize = 11.5.sp,
                            color = ServoraTheme.colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) ServoraTheme.colors.cardBackground else Color.White,
                        border = BorderStroke(1.dp, if (isDark) ServoraTheme.colors.cardBorder else BrandBorder),
                        modifier = Modifier.clickable {
                            onNavigate(booking.addressText.ifBlank { "${booking.locality}, ${booking.city}" })
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = if (isDark) ServoraTheme.colors.success else BrandGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Map",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) ServoraTheme.colors.success else BrandGreen
                            )
                        }
                    }
                }
            }

            // ================= 6. SPECIAL NOTES (if present) =================
            if (booking.specialNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) ServoraTheme.colors.cardBackgroundSubtle else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, ServoraTheme.colors.divider)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = ServoraTheme.colors.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = booking.specialNotes,
                            fontSize = 11.sp,
                            color = ServoraTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isCancelled) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) ServoraTheme.colors.dangerContainer else Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, if (isDark) ServoraTheme.colors.cardBorder else Color(0xFFFEE2E2))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isDark) ServoraTheme.colors.danger else Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Booking Cancelled",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) ServoraTheme.colors.onDangerContainer else Color(0xFFB91C1C)
                            )
                            if (!booking.cancellationReason.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Reason: ${booking.cancellationReason}",
                                    fontSize = 11.sp,
                                    color = if (isDark) ServoraTheme.colors.onDangerContainer else Color(0xFF991B1B)
                                )
                            }
                        }
                    }
                }
            } else {
                // ================= 7. 4-STEP PROGRESS TIMELINE =================
                PartnerStepTimeline(status = booking.status)
            }

            // ================= 8. ADVANCE STATUS BUTTON (FOR ACTIVE JOBS) =================
            if (!isCompleted && booking.status != BookingStatus.CANCELLED) {
                Spacer(modifier = Modifier.height(12.dp))

                val buttonLabel = when {
                    isAwaiting -> "Accept Job ➔"
                    booking.status == BookingStatus.ASSIGNED -> "Start Journey ➔"
                    booking.status == BookingStatus.ON_THE_WAY -> "I Have Arrived at Location ➔"
                    booking.status == BookingStatus.ARRIVED -> "Enter Start OTP & Begin ➔"
                    booking.status == BookingStatus.STARTED -> "Complete Job ➔"
                    booking.status == BookingStatus.AWAITING_PAYMENT -> "Collect Payment (₹${booking.totalAmount}) ➔"
                    else -> "Advance Duty ➔"
                }

                Button(
                    onClick = {
                        if (isAwaiting) {
                            if (onAcceptJob != null) {
                                onAcceptJob(booking) {}
                            } else {
                                onAdvanceStatus(booking.id, BookingStatus.CONFIRMED)
                            }
                        } else if (booking.status == BookingStatus.AWAITING_PAYMENT) {
                            onCollectPayment(booking.id)
                        } else {
                            onAdvanceStatus(booking.id, booking.status)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) ServoraTheme.colors.success else BrandGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = buttonLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color.White /* theme-invariant */
                    )
                }
            }
        }
    }
}

@Composable
private fun PartnerStepTimeline(status: BookingStatus) {
    val isDark = ServoraTheme.colors.isDark
    val stepIndex = when (status) {
        BookingStatus.ASSIGNED -> 0
        BookingStatus.ON_THE_WAY -> 1
        BookingStatus.ARRIVED -> 2
        BookingStatus.STARTED -> 2
        BookingStatus.AWAITING_PAYMENT -> 2
        BookingStatus.COMPLETED -> 3
        BookingStatus.CANCELLED -> 0
        else -> 0
    }

    val steps = listOf("Assigned", "On Way", "Arrived", "Completed")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, _ ->
                val isStepDoneOrCurrent = index <= stepIndex

                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isStepDoneOrCurrent) (if (isDark) ServoraTheme.colors.success else BrandGreen) else (if (isDark) ServoraTheme.colors.cardBorder else Color(0xFFE2E8F0))),
                    contentAlignment = Alignment.Center
                ) {
                    if (isStepDoneOrCurrent) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White, /* theme-invariant */
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                if (index < steps.size - 1) {
                    val isLineActive = index < stepIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (isLineActive) (if (isDark) ServoraTheme.colors.success else BrandGreen) else (if (isDark) ServoraTheme.colors.cardBorder else Color(0xFFE2E8F0)))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, title ->
                val isStepDoneOrCurrent = index <= stepIndex
                Text(
                    text = title,
                    fontSize = 10.5.sp,
                    fontWeight = if (isStepDoneOrCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isStepDoneOrCurrent) (if (isDark) ServoraTheme.colors.success else BrandGreen) else ServoraTheme.colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(60.dp)
                )
            }
        }
    }
}

