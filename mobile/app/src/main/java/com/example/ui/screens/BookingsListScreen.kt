package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PestControl
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.ui.components.hideStatusBarOnScroll

@Composable
fun BookingsListScreen(
    bookings: List<Booking>,
    onSelectBooking: (Booking) -> Unit,
    onBookAgain: (String) -> Unit,
    onExploreClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(1) } // Default to Past & Completed (1) or Active (0)

    val emeraldGreen = Color(0xFF009051)
    val darkEmerald = Color(0xFF0B5433)
    val textPrimary = Color(0xFF1E2022)
    val textSecondary = Color(0xFF6B7280)
    val cardBorder = Color(0xFFE5E7EB)

    val activeBookings = bookings.filter {
        it.status != BookingStatus.COMPLETED && it.status != BookingStatus.CANCELLED
    }
    val pastBookings = bookings.filter {
        it.status == BookingStatus.COMPLETED || it.status == BookingStatus.CANCELLED
    }

    val displayList = if (selectedTab == 0) activeBookings else pastBookings

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .hideStatusBarOnScroll(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ================= 1. HEADER & SEGMENTED TABS =================
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            ) {
                if (onBackClick != null) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F6EE))
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
                    text = "Manage your appointments & service history",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ================= SEGMENTED TABS =================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Active Tab
                        val isTab0 = selectedTab == 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isTab0) Color(0xFFE8F6EE) else Color.Transparent)
                                .border(
                                    width = if (isTab0) 1.dp else 0.dp,
                                    color = if (isTab0) emeraldGreen else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTab = 0 }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Active & Upcoming (${activeBookings.size})",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isTab0) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isTab0) darkEmerald else textSecondary
                            )
                        }

                        // Past Tab
                        val isTab1 = selectedTab == 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isTab1) Color(0xFFE8F6EE) else Color.Transparent)
                                .border(
                                    width = if (isTab1) 1.dp else 0.dp,
                                    color = if (isTab1) emeraldGreen else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTab = 1 }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Past & Completed (${pastBookings.size})",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isTab1) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isTab1) darkEmerald else textSecondary
                            )
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
                        text = if (selectedTab == 0) "No active bookings right now" else "No past bookings yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Book verified AC repair, home cleaning, salon or electrical services in Agra.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onExploreClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = emeraldGreen)
                    ) {
                        Text("Explore Services", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            items(displayList) { booking ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    BookingCardItem(
                        booking = booking,
                        onSelectBooking = { onSelectBooking(booking) },
                        onBookAgain = { onBookAgain(booking.serviceId) }
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
    onBookAgain: () -> Unit
) {
    val emeraldGreen = Color(0xFF009051)
    val darkEmerald = Color(0xFF0B5433)
    val textPrimary = Color(0xFF1E2022)
    val textSecondary = Color(0xFF6B7280)
    val cardBorder = Color(0xFFE5E7EB)

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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
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
                        fontSize = 11.sp
                    ),
                    color = textSecondary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (booking.status) {
                                BookingStatus.COMPLETED -> Color(0xFFE8F6EE)
                                BookingStatus.CANCELLED -> Color(0xFFFEF2F2)
                                else -> Color(0xFFE8F6EE)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (booking.status == BookingStatus.CANCELLED) Color(0xFFEF4444) else emeraldGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (booking.status == BookingStatus.COMPLETED) "Service Completed" else booking.status.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (booking.status == BookingStatus.CANCELLED) Color(0xFFEF4444) else emeraldGreen
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
                        .background(Color(0xFFE8F6EE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = serviceIcon,
                        contentDescription = null,
                        tint = emeraldGreen,
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
                        fontSize = 18.sp
                    ),
                    color = emeraldGreen
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onSelectBooking,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, emeraldGreen),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = emeraldGreen),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = "View Details",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = emeraldGreen,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    if (booking.status == BookingStatus.COMPLETED || booking.status == BookingStatus.CANCELLED) {
                        Button(
                            onClick = onBookAgain,
                            colors = ButtonDefaults.buttonColors(containerColor = darkEmerald),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Book Again",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onSelectBooking,
                            colors = ButtonDefaults.buttonColors(containerColor = emeraldGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text(
                                text = "Track Status",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = Color.White,
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
