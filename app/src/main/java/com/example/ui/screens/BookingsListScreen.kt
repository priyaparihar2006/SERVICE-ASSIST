package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.example.ui.components.stableStatusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PestControl
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.isAwaitingPartnerAcceptance
import com.example.data.model.isPartnerAssigned
import com.example.ui.components.hideStatusBarOnScroll
import com.example.ui.theme.ServoraTheme

enum class CustomerBookingTab(
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
fun BookingsListScreen(
    bookings: List<Booking>,
    onSelectBooking: (Booking) -> Unit,
    onBookAgain: (String) -> Unit,
    onExploreClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    onOpenChat: ((Booking) -> Unit)? = null,
    unreadByBookingId: Map<Long, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val isDark = ServoraTheme.colors.isDark
    val emeraldGreen = if (isDark) ServoraTheme.colors.primary else Color(0xFF009051)
    val darkEmerald = if (isDark) ServoraTheme.colors.primaryContainer else Color(0xFF0B5433)
    val textPrimary = ServoraTheme.colors.textPrimary
    val textSecondary = ServoraTheme.colors.textSecondary

    // Segregate Bookings
    val activeBookings = remember(bookings) {
        bookings.filter { it.status != BookingStatus.COMPLETED && it.status != BookingStatus.CANCELLED }
    }
    val completedBookings = remember(bookings) {
        bookings.filter { it.status == BookingStatus.COMPLETED }
    }
    val cancelledBookings = remember(bookings) {
        bookings.filter { it.status == BookingStatus.CANCELLED }
    }

    var selectedTab by remember {
        mutableStateOf(
            when {
                activeBookings.isNotEmpty() -> CustomerBookingTab.ACTIVE
                completedBookings.isNotEmpty() -> CustomerBookingTab.COMPLETED
                cancelledBookings.isNotEmpty() -> CustomerBookingTab.CANCELLED
                else -> CustomerBookingTab.ALL
            }
        )
    }

    val displayList = remember(bookings, selectedTab) {
        when (selectedTab) {
            CustomerBookingTab.ALL -> bookings
            CustomerBookingTab.ACTIVE -> activeBookings
            CustomerBookingTab.COMPLETED -> completedBookings
            CustomerBookingTab.CANCELLED -> cancelledBookings
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .hideStatusBarOnScroll(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ================= 1. HEADER & REDESIGNED 4-SEGMENT CAPSULE TAB BAR =================
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .stableStatusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            ) {
                if (onBackClick != null) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) ServoraTheme.colors.cardBackgroundSubtle else Color(0xFFE8F6EE))
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = emeraldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text(
                    text = "My Bookings",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Live updates and real-time tracking",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ================= 4-SEGMENT FLOATING CAPSULE BAR (MATCHING MOCKUP) =================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(36.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.2.dp, if (isDark) ServoraTheme.colors.cardBorder else Color(0xFFCCEBDC)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomerBookingTab.entries.forEachIndexed { index, tab ->
                            val isSelected = selectedTab == tab

                            Surface(
                                modifier = Modifier
                                    .weight(tab.weight)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .clickable { selectedTab = tab }
                                    .testTag("customer_tab_${tab.name}"),
                                shape = RoundedCornerShape(26.dp),
                                color = if (isSelected) emeraldGreen else Color.Transparent /* theme-invariant */
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
                                            imageVector = tab.icon,
                                            contentDescription = null,
                                            tint = Color.White, /* theme-invariant */
                                            modifier = Modifier.size(15.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(if (isDark) ServoraTheme.colors.cardBackgroundSubtle else Color(0xFFE6F7EF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = null,
                                                tint = emeraldGreen,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(3.5.dp))

                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            letterSpacing = (-0.2).sp
                                        ),
                                        color = if (isSelected) Color.White /* theme-invariant */ else textPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Subtle vertical divider between unselected adjacent items
                            if (index < CustomerBookingTab.entries.size - 1) {
                                val nextIsSelected = selectedTab == CustomerBookingTab.entries[index + 1]
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
        }

        // ================= 2. BOOKINGS LIST OR EMPTY STATE =================
        if (displayList.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = emeraldGreen,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (selectedTab) {
                            CustomerBookingTab.ALL -> "No bookings found"
                            CustomerBookingTab.ACTIVE -> "No active bookings right now"
                            CustomerBookingTab.COMPLETED -> "No completed bookings yet"
                            CustomerBookingTab.CANCELLED -> "No cancelled bookings"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Book verified AC repair, home cleaning, salon or electrical services in Agra.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onExploreClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = emeraldGreen)
                    ) {
                        Text("Explore Services", color = Color.White /* theme-invariant */, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            items(displayList, key = { it.id }) { booking ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    BookingCardItem(
                        booking = booking,
                        onSelectBooking = { onSelectBooking(booking) },
                        onBookAgain = { onBookAgain(booking.serviceId) },
                        onOpenChat = onOpenChat?.let { { it(booking) } },
                        unreadCount = unreadByBookingId[booking.id] ?: 0
                    )
                }
            }
        }
    }
}

@Composable
fun BookingCardItem(
    booking: Booking,
    onSelectBooking: () -> Unit,
    onBookAgain: () -> Unit,
    onOpenChat: (() -> Unit)? = null,
    unreadCount: Int = 0
) {
    val isDark = ServoraTheme.colors.isDark
    val emeraldGreen = if (isDark) ServoraTheme.colors.primary else Color(0xFF009051)
    val darkEmerald = if (isDark) ServoraTheme.colors.primaryContainer else Color(0xFF0B5433)
    val textPrimary = ServoraTheme.colors.textPrimary
    val textSecondary = ServoraTheme.colors.textSecondary
    val cardBorder = ServoraTheme.colors.cardBorder
    val isCancelled = booking.status == BookingStatus.CANCELLED
    val isCompleted = booking.status == BookingStatus.COMPLETED

    val serviceIcon: ImageVector = when {
        booking.serviceName.contains("Electric", ignoreCase = true) || booking.serviceName.contains("Plumb", ignoreCase = true) -> Icons.Default.Bolt
        booking.serviceName.contains("Clean", ignoreCase = true) -> Icons.Default.CleaningServices
        booking.serviceName.contains("Facial", ignoreCase = true) || booking.serviceName.contains("Salon", ignoreCase = true) || booking.serviceName.contains("Spa", ignoreCase = true) -> Icons.Default.Spa
        booking.serviceName.contains("AC", ignoreCase = true) || booking.serviceName.contains("Appliance", ignoreCase = true) -> Icons.Default.AcUnit
        booking.serviceName.contains("Paint", ignoreCase = true) -> Icons.Default.FormatPaint
        booking.serviceName.contains("Pest", ignoreCase = true) -> Icons.Default.PestControl
        else -> Icons.Default.Home
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelectBooking() }
            .testTag("booking_card_${booking.bookingCode}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ================= 1. CARD TOP: Code + Status Badge =================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.bookingCode,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    ),
                    color = textSecondary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (booking.status) {
                                BookingStatus.COMPLETED -> if (isDark) ServoraTheme.colors.successContainer else Color(0xFFE8F6EE)
                                BookingStatus.CANCELLED -> if (isDark) ServoraTheme.colors.dangerContainer else Color(0xFFFEF2F2)
                                else -> if (isDark) ServoraTheme.colors.successContainer else Color(0xFFE8F6EE)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isCancelled) Icons.Default.Cancel else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isCancelled) (if (isDark) ServoraTheme.colors.danger else Color(0xFFEF4444)) else emeraldGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCompleted) "Service Completed" else booking.status.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (isCancelled) (if (isDark) ServoraTheme.colors.onDangerContainer else Color(0xFFEF4444)) else (if (isDark) ServoraTheme.colors.onSuccessContainer else emeraldGreen)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ================= 2. CARD MIDDLE: Service Icon + Title + Subtitle =================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Service Icon Circle
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isDark) ServoraTheme.colors.cardBackgroundSubtle else if (isCancelled) Color(0xFFF1F5F9) else Color(0xFFE8F6EE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = serviceIcon,
                        contentDescription = null,
                        tint = if (isCancelled) textSecondary else emeraldGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.serviceName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = booking.packageName,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = emeraldGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${booking.scheduledDate} • ${booking.scheduledTime}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = textSecondary
                        )
                    }
                }
            }

            // Customer Start OTP Section (Visible ONLY to customer for active bookings)
            if (!isCompleted && !isCancelled) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) ServoraTheme.colors.cardBackgroundSubtle else Color(0xFFEEF9F3),
                    border = BorderStroke(1.dp, if (isDark) ServoraTheme.colors.cardBorder else Color(0xFFC6F0D8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = emeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Start OTP",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) ServoraTheme.colors.textPrimary else darkEmerald
                                )
                                Text(
                                    text = "Tell this OTP to partner upon arrival",
                                    fontSize = 10.5.sp,
                                    color = textSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, emeraldGreen)
                        ) {
                            val otpDigits = booking.startOtp.ifBlank { "4829" }
                            Text(
                                text = otpDigits,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                color = if (isDark) emeraldGreen else darkEmerald,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Cancellation Reason Banner
            if (isCancelled && !booking.cancellationReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) ServoraTheme.colors.dangerContainer else Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, if (isDark) ServoraTheme.colors.dangerContainer else Color(0xFFFEE2E2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isDark) ServoraTheme.colors.danger else Color(0xFFEF4444))
                        )
                        val prefix = if (booking.cancelledBy == "PARTNER") "Cancelled by professional: " else "Reason: "
                        Text(
                            text = "$prefix${booking.cancellationReason}",
                            fontSize = 11.5.sp,
                            color = if (isDark) ServoraTheme.colors.onDangerContainer else Color(0xFFB91C1C),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= 3. CARD BOTTOM: Price + Action Buttons =================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${booking.totalAmount}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = if (isCancelled) textSecondary else emeraldGreen,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onSelectBooking,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.2.dp, emeraldGreen),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = emeraldGreen),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "View Details",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = emeraldGreen,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    if (isCompleted || isCancelled) {
                        Button(
                            onClick = onBookAgain,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) emeraldGreen else darkEmerald),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF00210F) else Color.White, /* theme-invariant */
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Book Again",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (isDark) Color(0xFF00210F) else Color.White, /* theme-invariant */
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    } else if (!booking.isAwaitingPartnerAcceptance) {
                        Button(
                            onClick = {
                                if (onOpenChat != null) {
                                    onOpenChat()
                                } else {
                                    onSelectBooking()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = emeraldGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "Message",
                                    tint = Color.White, /* theme-invariant */
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (unreadCount > 0) "Message ($unreadCount)" else "Message",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color.White, /* theme-invariant */
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

