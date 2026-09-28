package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.ui.theme.ServoraTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Modern Brand Theme Palette (#009051)
private val brandEmeraldPrimary = Color(0xFF009051) // theme-invariant
private val brandEmeraldDark = Color(0xFF0B5433) // theme-invariant
private val brandEmeraldSoft = Color(0xFFE6F5EE) // theme-invariant
private val brandEmeraldTint = Color(0xFFCCEBDC) // theme-invariant

private val brandAmber = Color(0xFFF59E0B) // theme-invariant
private val brandAmberSoft = Color(0xFFFEF3C7) // theme-invariant
private val brandAmberDark = Color(0xFFB45309) // theme-invariant

private val brandBlue = Color(0xFF2563EB) // theme-invariant
private val brandBlueSoft = Color(0xFFEFF6FF) // theme-invariant

private val surfaceBg = Color(0xFFF8FAFC) // theme-invariant

fun getServiceImageDrawable(serviceId: String, serviceName: String): Int {
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

fun getBookingStatusLabel(status: BookingStatus): String = when (status) {
    BookingStatus.PENDING -> "Requested"
    BookingStatus.CONFIRMED -> "New request"
    BookingStatus.ASSIGNED -> "Job Assigned"
    BookingStatus.ON_THE_WAY -> "On the Way"
    BookingStatus.ARRIVED -> "Arrived at Doorstep"
    BookingStatus.STARTED -> "In Progress"
    BookingStatus.AWAITING_PAYMENT -> "Awaiting Payment"
    BookingStatus.COMPLETED -> "Completed & Settled"
    BookingStatus.CANCELLED -> "Cancelled"
}

/**
 * 1. Modern Hero Detail Card: Service Image, Title, Package, Scheduled Time, and Payout with Booking ID
 */
@Composable
fun JobHeroDetailCard(
    booking: Booking,
    isAccepted: Boolean,
    unreadCount: Int = 0,
    onOpenChat: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = ServoraTheme.colors
    val isDark = colors.isDark
    val isCompleted = booking.status == BookingStatus.COMPLETED
    val serviceImageRes = getServiceImageDrawable(booking.serviceId, booking.serviceName)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isCompleted) colors.divider else colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Service Image + Titles
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) colors.surfaceVariant else Color(0xFFF1F5F9)),
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
                    Text(
                        text = booking.serviceName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 21.sp
                        ),
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (booking.packageName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) colors.successContainer else brandEmeraldSoft
                        ) {
                            Text(
                                text = booking.packageName,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) colors.onSuccessContainer else brandEmeraldDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Scheduled Date & Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${booking.scheduledDate}  •  ${booking.scheduledTime}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = colors.divider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Row: Booking Code Chip (Clickable to copy) + Total Payout Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Booking Code Chip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) colors.surfaceVariant else Color(0xFFF1F5F9),
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Booking ID", booking.bookingCode)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied ID: ${booking.bookingCode}", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = booking.bookingCode.ifBlank { "SRV-${booking.id}" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = colors.textMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Total Price & Payment Status Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCompleted) {
                        if (isDark) colors.surfaceVariant else Color(0xFFF8FAFC)
                    } else {
                        if (isDark) colors.successContainer else brandEmeraldSoft
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isCompleted) colors.cardBorder else (if (isDark) colors.cardBorder else brandEmeraldTint)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "₹${booking.totalAmount}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 15.5.sp
                            ),
                            color = if (isCompleted) {
                                if (isDark) colors.onSuccessContainer else brandEmeraldDark
                            } else {
                                if (isDark) colors.success else brandEmeraldPrimary
                            },
                            maxLines = 1,
                            softWrap = false
                        )
                        if (booking.isPaid) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = if (isDark) colors.success else brandEmeraldPrimary
                            ) {
                                Text(
                                    text = "PAID",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White, /* theme-invariant */
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else if (booking.paymentMethod.isNotBlank()) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = if (isDark) colors.cardBackground else Color.White
                            ) {
                                Text(
                                    text = if (booking.paymentMethod.contains("Cash", ignoreCase = true)) "Cash" else "UPI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
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

/**
 * 2. Modern Customer & Location Card with Quick Actions and Dedicated Navigation Button
 */
@Composable
fun JobCustomerCard(
    booking: Booking,
    isAccepted: Boolean,
    onCallCustomer: (String) -> Unit = {},
    onOpenMap: (String) -> Unit = {},
    onOpenChat: () -> Unit = {},
    unreadCount: Int = 0
) {
    val colors = ServoraTheme.colors
    val isDark = colors.isDark
    val customerFirstName = booking.customerName.trim().split(" ").firstOrNull()?.ifBlank { "Customer" } ?: "Customer"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Customer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isDark) colors.successContainer else brandEmeraldSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isDark) colors.success else brandEmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isAccepted) booking.customerName.ifBlank { "Customer" } else customerFirstName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = if (isAccepted) booking.customerPhone.ifBlank { "+91 98765 43210" } else "Verified Client • ${booking.locality}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colors.textSecondary
                        )
                    }
                }

                // Call & Message Actions only when accepted
                if (isAccepted) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Call Action
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) colors.success else brandEmeraldPrimary,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable { onCallCustomer(booking.customerPhone) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call Customer",
                                    tint = Color.White, /* theme-invariant */
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Message Action
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) colors.surfaceVariant else Color.White,
                            border = BorderStroke(1.2.dp, if (isDark) colors.cardBorder else brandEmeraldTint),
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable { onOpenChat() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Message",
                                    tint = if (isDark) colors.success else brandEmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
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
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = colors.divider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Address & Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) colors.surfaceVariant else Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (isDark) colors.success else brandEmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAccepted) "Destination Address" else "Service Locality",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isAccepted) {
                            "${booking.addressText}, ${booking.locality} (${booking.city})"
                        } else {
                            "${booking.locality}, ${booking.city}"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.textPrimary
                    )
                }
            }

            // Dedicated Open Map Navigation Button
            if (isAccepted) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) colors.surfaceVariant else Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, if (isDark) colors.cardBorder else Color(0xFFDCFCE7)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenMap(booking.addressText) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Navigate",
                            tint = if (isDark) colors.success else brandEmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Navigation in Google Maps",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) colors.onSuccessContainer else brandEmeraldDark
                        )
                    }
                }
            }

            // Special Notes
            if (booking.specialNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) colors.surfaceVariant else Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, if (isDark) colors.cardBorder else Color(0xFFFEF3C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = if (isDark) colors.warning else Color(0xFFD97706),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Customer Instruction",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) colors.warning else Color(0xFFB45309)
                            )
                            Text(
                                text = booking.specialNotes,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                ),
                                color = colors.textPrimary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Modern Connected 5-Step Duty Tracker
 */
@Composable
fun JobTrackerSection(currentStatus: BookingStatus) {
    val colors = ServoraTheme.colors
    val isDark = colors.isDark

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Duty Progress",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = colors.textPrimary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (currentStatus) {
                        BookingStatus.COMPLETED -> if (isDark) colors.successContainer else brandEmeraldSoft
                        BookingStatus.CANCELLED -> if (isDark) colors.dangerContainer else Color(0xFFFEE2E2)
                        BookingStatus.AWAITING_PAYMENT -> if (isDark) colors.warningContainer else brandAmberSoft
                        BookingStatus.ARRIVED -> if (isDark) colors.infoContainer else brandBlueSoft
                        else -> if (isDark) colors.successContainer else brandEmeraldSoft
                    }
                ) {
                    Text(
                        text = getBookingStatusLabel(currentStatus),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (currentStatus) {
                            BookingStatus.COMPLETED -> if (isDark) colors.onSuccessContainer else brandEmeraldDark
                            BookingStatus.CANCELLED -> if (isDark) colors.danger else Color(0xFFDC2626)
                            BookingStatus.AWAITING_PAYMENT -> if (isDark) colors.onWarningContainer else brandAmberDark
                            BookingStatus.ARRIVED -> if (isDark) colors.onInfoContainer else brandBlue
                            else -> if (isDark) colors.onSuccessContainer else brandEmeraldDark
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val steps = listOf(
                "Assigned" to (currentStatus.stepIndex >= BookingStatus.ASSIGNED.stepIndex),
                "On Way" to (currentStatus.stepIndex >= BookingStatus.ON_THE_WAY.stepIndex),
                "Arrived" to (currentStatus.stepIndex >= BookingStatus.ARRIVED.stepIndex),
                "Started" to (currentStatus.stepIndex >= BookingStatus.STARTED.stepIndex),
                "Payment" to (currentStatus == BookingStatus.AWAITING_PAYMENT || currentStatus == BookingStatus.COMPLETED)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                steps.forEachIndexed { index, step ->
                    val isCurrent = when (index) {
                        0 -> currentStatus == BookingStatus.ASSIGNED
                        1 -> currentStatus == BookingStatus.ON_THE_WAY
                        2 -> currentStatus == BookingStatus.ARRIVED
                        3 -> currentStatus == BookingStatus.STARTED
                        4 -> currentStatus == BookingStatus.AWAITING_PAYMENT || currentStatus == BookingStatus.COMPLETED
                        else -> false
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        step.second -> if (isDark) colors.success else brandEmeraldPrimary
                                        else -> if (isDark) colors.surfaceVariant else Color(0xFFE2E8F0)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (step.second && !isCurrent) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White, /* theme-invariant */
                                    modifier = Modifier.size(13.dp)
                                )
                            } else if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White) /* theme-invariant */
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = step.first,
                            fontSize = 10.sp,
                            fontWeight = if (isCurrent || step.second) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent || step.second) {
                                if (isDark) colors.success else brandEmeraldDark
                            } else {
                                colors.textMuted
                            },
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    if (index < steps.size - 1) {
                        val isLineActive = steps[index + 1].second
                        Box(
                            modifier = Modifier
                                .weight(0.6f)
                                .height(2.5.dp)
                                .padding(bottom = 14.dp)
                                .background(
                                    if (isLineActive) {
                                        if (isDark) colors.success else brandEmeraldPrimary
                                    } else {
                                        if (isDark) colors.surfaceVariant else Color(0xFFE2E8F0)
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Legacy Helper Components kept for backwards-compatibility with existing screens
 */
@Composable
fun JobHeaderSection(
    booking: Booking,
    isAccepted: Boolean,
    unreadCount: Int = 0,
    onOpenChat: () -> Unit = {}
) {
    JobHeroDetailCard(booking, isAccepted, unreadCount, onOpenChat)
}

@Composable
fun JobServiceSection(booking: Booking) {
    // Merged into JobHeroDetailCard
}

@Composable
fun JobCustomerSection(
    booking: Booking,
    isAccepted: Boolean,
    onCallCustomer: (String) -> Unit = {},
    onOpenMap: (String) -> Unit = {},
    onOpenChat: () -> Unit = {},
    unreadCount: Int = 0
) {
    JobCustomerCard(booking, isAccepted, onCallCustomer, onOpenMap, onOpenChat, unreadCount)
}

/**
 * 5. OTP Banner for ARRIVED status
 */
@Composable
fun JobOtpBanner(startOtp: String = "") {
    val isDark = ServoraTheme.colors.isDark

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) ServoraTheme.colors.warningContainer else Color(0xFFFEF3C7),
        border = BorderStroke(1.dp, if (isDark) ServoraTheme.colors.cardBorder else Color(0xFFFDE68A))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = if (isDark) ServoraTheme.colors.warning else Color(0xFFD97706),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Ask customer for the 4-digit start OTP to begin work.",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) ServoraTheme.colors.onWarningContainer else Color(0xFF92400E)
            )
        }
    }
}

/**
 * 6. Primary Action Button for advancing booking status
 */
@Composable
fun JobPrimaryActionButton(
    booking: Booking,
    isUpdating: Boolean = false,
    onAdvanceClick: () -> Unit
) {
    val isDark = ServoraTheme.colors.isDark

    Button(
        onClick = onAdvanceClick,
        enabled = !isUpdating,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when (booking.status) {
                BookingStatus.AWAITING_PAYMENT -> if (isDark) ServoraTheme.colors.warning else Color(0xFFD97706)
                BookingStatus.ARRIVED -> if (isDark) ServoraTheme.colors.info else Color(0xFF0284C7)
                else -> if (isDark) ServoraTheme.colors.success else brandEmeraldPrimary
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("btn_partner_advance_job"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (isUpdating) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White, /* theme-invariant */
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when (booking.status) {
                        BookingStatus.ASSIGNED -> Icons.Default.Navigation
                        BookingStatus.ON_THE_WAY -> Icons.Default.LocationOn
                        BookingStatus.ARRIVED -> Icons.Default.Key
                        BookingStatus.STARTED, BookingStatus.AWAITING_PAYMENT -> Icons.Default.Payments
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = Color.White, /* theme-invariant */
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (booking.status) {
                        BookingStatus.ASSIGNED -> "Start Travel to Customer"
                        BookingStatus.ON_THE_WAY -> "I Have Arrived at Doorstep"
                        BookingStatus.ARRIVED -> "Verify Customer OTP & Start"
                        BookingStatus.STARTED -> "Complete Duty • ₹${booking.totalAmount}"
                        BookingStatus.AWAITING_PAYMENT -> "Proceed to Payment • ₹${booking.totalAmount}"
                        else -> "Job Complete"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White /* theme-invariant */
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 7. Completed Job Payout Breakdown
 */
@Composable
fun JobPayoutBreakdown(booking: Booking) {
    val isDark = ServoraTheme.colors.isDark
    var isBreakdownExpanded by remember { mutableStateOf(false) }

    Column {
        // Settlement & Earnings Confirmation Box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) ServoraTheme.colors.successContainer else brandEmeraldSoft,
            border = BorderStroke(1.dp, if (isDark) ServoraTheme.colors.cardBorder else brandEmeraldTint)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isDark) ServoraTheme.colors.success else brandEmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Earnings Settled: ₹${booking.totalAmount}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) ServoraTheme.colors.onSuccessContainer else brandEmeraldDark
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) ServoraTheme.colors.cardBackground else Color.White
                    ) {
                        Text(
                            text = booking.paymentMethod.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) ServoraTheme.colors.success else brandEmeraldPrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                if (!booking.paymentReference.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Transaction Ref (UTR): ${booking.paymentReference}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = ServoraTheme.colors.textSecondary
                    )
                }

                val dateFormatted = try {
                    val timeMs = booking.paidAt ?: booking.createdAt
                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timeMs))
                } catch (_: Exception) {
                    "${booking.scheduledDate} • Completed"
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Completed on $dateFormatted",
                    fontSize = 11.sp,
                    color = ServoraTheme.colors.textMuted
                )
            }
        }

        // Verified Review & Rating Card
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) ServoraTheme.colors.warningContainer else Color(0xFFFFFBEB),
            border = BorderStroke(1.dp, if (isDark) ServoraTheme.colors.cardBorder else Color(0xFFFEF3C7))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B), /* theme-invariant */
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "5.0",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) ServoraTheme.colors.onWarningContainer else Color(0xFF92400E)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• Verified Service Feedback",
                        fontSize = 11.5.sp,
                        color = if (isDark) ServoraTheme.colors.onWarningContainer else Color(0xFFB45309)
                    )
                }

                Text(
                    text = "100% On-Time",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) ServoraTheme.colors.onWarningContainer else Color(0xFF92400E)
                )
            }
        }

        // Collapsible Invoice Breakdown
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isBreakdownExpanded = !isBreakdownExpanded },
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) ServoraTheme.colors.cardBackgroundSubtle else Color.White,
            border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = ServoraTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "View Cost & Payout Breakdown",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ServoraTheme.colors.textPrimary
                        )
                    }
                    Icon(
                        imageVector = if (isBreakdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = ServoraTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isBreakdownExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = ServoraTheme.colors.divider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Service Base Fare", fontSize = 12.sp, color = ServoraTheme.colors.textSecondary)
                        Text("₹${booking.totalAmount + booking.discountAmount}", fontSize = 12.sp, color = ServoraTheme.colors.textPrimary)
                    }
                    if (booking.discountAmount > 0) {
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Discount Offered", fontSize = 12.sp, color = if (isDark) ServoraTheme.colors.success else brandEmeraldPrimary)
                            Text("-₹${booking.discountAmount}", fontSize = 12.sp, color = if (isDark) ServoraTheme.colors.success else brandEmeraldPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Taxes & Convenience", fontSize = 12.sp, color = ServoraTheme.colors.textSecondary)
                        Text("Included", fontSize = 12.sp, color = ServoraTheme.colors.textSecondary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = ServoraTheme.colors.cardBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Settled", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) ServoraTheme.colors.success else brandEmeraldDark)
                        Text("₹${booking.totalAmount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) ServoraTheme.colors.success else brandEmeraldDark)
                    }
                }
            }
        }
    }
}

/**
 * 8. Partner Job Summary Card for Partner Home Screen (Duty & Jobs tab)
 * Used for both "New Jobs" and "Ongoing Jobs"
 */
@Composable
fun PartnerJobSummaryCard(
    booking: Booking,
    isNew: Boolean,
    isAccepting: Boolean = false,
    onAcceptClick: () -> Unit = {},
    onOpenJobClick: () -> Unit = {},
    unreadCount: Int = 0,
    onOpenChat: () -> Unit = {}
) {
    val isDark = ServoraTheme.colors.isDark

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenJobClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            JobHeaderSection(
                booking = booking,
                isAccepted = !isNew,
                unreadCount = unreadCount,
                onOpenChat = onOpenChat
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = ServoraTheme.colors.divider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Service info
            JobServiceSection(booking = booking)

            Spacer(modifier = Modifier.height(14.dp))

            // Customer info (privacy-preserved if isNew)
            JobCustomerSection(
                booking = booking,
                isAccepted = !isNew,
                onCallCustomer = {},
                onOpenMap = {},
                onOpenChat = onOpenChat,
                unreadCount = unreadCount
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            if (isNew) {
                // Accept Job full-width brand green button
                Button(
                    onClick = onAcceptClick,
                    enabled = !isAccepting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) ServoraTheme.colors.success else brandEmeraldPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_partner_accept_job_${booking.id}"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (isAccepting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White, /* theme-invariant */
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White, /* theme-invariant */
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Accept Job",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White /* theme-invariant */
                                )
                            )
                        }
                    }
                }
            } else {
                // Continue Job outlined green button
                OutlinedButton(
                    onClick = onOpenJobClick,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, if (isDark) ServoraTheme.colors.success else brandEmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_partner_continue_job_${booking.id}"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = if (isDark) ServoraTheme.colors.success else brandEmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Continue Job",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = if (isDark) ServoraTheme.colors.success else brandEmeraldPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}
