package com.example.ui.screens

import android.widget.Toast
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
import com.example.ui.components.NotificationDot
import com.example.ui.components.stableStatusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.UserProfile
import com.example.data.model.isAwaitingPartnerAcceptance
import com.example.data.model.isPartnerAssigned
import com.example.ui.components.PartnerJobSummaryCard
import com.example.ui.components.hideStatusBarOnScroll
import com.example.ui.theme.ServoraTheme
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch


@Composable
fun PartnerJobsScreen(
    partnerProfile: UserProfile,
    bookings: List<Booking>,
    onAcceptJob: (Booking, (Result<Unit>) -> Unit) -> Unit,
    onOpenJob: (Long) -> Unit,
    onSyncClick: () -> Unit,
    onViewAllBookings: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onOpenChat: (Booking) -> Unit = {},
    unreadNotificationsCount: Int = 0,
    unreadByBookingId: Map<Long, Int> = emptyMap(),
    statusUpdateErrorFlow: SharedFlow<String>? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isPartnerOnline by remember { mutableStateOf(true) }
    var acceptingBookingId by remember { mutableStateOf<Long?>(null) }

    val isDark = ServoraTheme.colors.isDark

    LaunchedEffect(statusUpdateErrorFlow) {
        statusUpdateErrorFlow?.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Split bookings into New (needs acceptance) and Ongoing (accepted in-flight)
    val newJobs = remember(bookings) {
        bookings.filter { it.isAwaitingPartnerAcceptance }
            .sortedByDescending { it.createdAt }
    }

    val ongoingJobs = remember(bookings) {
        bookings.filter {
            it.status != BookingStatus.COMPLETED &&
                    it.status != BookingStatus.CANCELLED &&
                    !it.isAwaitingPartnerAcceptance
        }.sortedByDescending { it.localUpdatedAt }
    }

    val hasAnyActiveJobs = newJobs.isNotEmpty() || ongoingJobs.isNotEmpty()

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
            // ================= 1. PREMIUM BRAND HEADER WITH STATUS & AVAILABILITY =================
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFF074828),
                                    ServoraTheme.colors.onPrimaryContainer,
                                    ServoraTheme.colors.primary
                                ) /* theme-invariant */
                            ),
                            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                        )
                        .stableStatusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top Row: Avatar + Name + PRO Badge + Notifications Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = partnerProfile.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifBlank { "PR" },
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 17.sp,
                                        color = Color.White /* theme-invariant */
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = partnerProfile.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 17.sp
                                            ),
                                            color = Color.White /* theme-invariant */
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF59E0B)
                                        ) {
                                            Text(
                                                text = "PRO",
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF451A03) /* theme-invariant */
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${partnerProfile.locality}, ${partnerProfile.city}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.85f) /* theme-invariant */
                                        )
                                    )
                                }
                            }

                            // Notification Bell Icon with Badge
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { onNotificationsClick() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = Color.White, /* theme-invariant */
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (unreadNotificationsCount > 0) {
                                        NotificationDot(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(top = 7.dp, end = 7.dp),
                                            ringColor = ServoraTheme.colors.headerBackgroundStart,
                                            contentDescription = "Unread notifications"
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Duty Status Switch Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isPartnerOnline) Color(0xFF22C55E).copy(alpha = 0.25f)
                                                else Color.White.copy(alpha = 0.2f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FlashOn,
                                            contentDescription = null,
                                            tint = if (isPartnerOnline) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (isPartnerOnline) "On-Duty & Ready" else "Off-Duty (Standby)",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.5.sp,
                                            color = Color.White /* theme-invariant */
                                        )
                                        Text(
                                            text = if (isPartnerOnline) "Receiving express jobs in your zone" else "You will not receive new order requests",
                                            fontSize = 11.5.sp,
                                            color = Color.White.copy(alpha = 0.8f), /* theme-invariant */
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Switch(
                                    checked = isPartnerOnline,
                                    onCheckedChange = { isPartnerOnline = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ServoraTheme.colors.primary,
                                        checkedTrackColor = Color.White,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Stats & Live Sync Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = "${newJobs.size} New • ${ongoingJobs.size} In-Flight",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White /* theme-invariant */
                                    )
                                }
                            }

                            // Cloud Sync Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onSyncClick()
                                        Toast.makeText(context, "Syncing with Servora Dispatch...", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync",
                                        tint = Color.White, /* theme-invariant */
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sync",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White /* theme-invariant */
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ================= 2. EMPTY STATE WHEN NO JOBS =================
            if (!hasAnyActiveJobs) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(ServoraTheme.colors.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ServoraTheme.colors.primary,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Active Duties Right Now",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 17.sp
                                ),
                                color = ServoraTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Keep your status online to receive incoming service orders in ${partnerProfile.city}.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                ),
                                color = ServoraTheme.colors.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }
            }

            // ================= 3. NEW JOBS SECTION (Needs Acceptance) =================
            if (newJobs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .padding(top = 18.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ServoraTheme.colors.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Jobs",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            ),
                            color = ServoraTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ServoraTheme.colors.primaryContainer
                        ) {
                            Text(
                                text = "${newJobs.size} New",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ServoraTheme.colors.primary
                            )
                        }
                    }
                }

                items(newJobs, key = { "new_${it.id}" }) { booking ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        PartnerJobSummaryCard(
                            booking = booking,
                            isNew = true,
                            isAccepting = (acceptingBookingId == booking.id),
                            onAcceptClick = {
                                acceptingBookingId = booking.id
                                onAcceptJob(booking) { result ->
                                    acceptingBookingId = null
                                    result.fold(
                                        onSuccess = {
                                            onOpenJob(booking.id)
                                        },
                                        onFailure = { err ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    err.message ?: "Failed to accept job"
                                                )
                                            }
                                        }
                                    )
                                }
                            },
                            onOpenJobClick = { onOpenJob(booking.id) },
                            unreadCount = unreadByBookingId[booking.id] ?: 0,
                            onOpenChat = { onOpenChat(booking) }
                        )
                    }
                }
            }

            // ================= 4. ONGOING JOBS SECTION (Accepted in-flight) =================
            if (ongoingJobs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .padding(top = 18.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isDark) ServoraTheme.colors.info else Color(0xFF2563EB))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ongoing Jobs",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            ),
                            color = ServoraTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) ServoraTheme.colors.infoContainer else Color(0xFFEFF6FF)
                        ) {
                            Text(
                                text = "${ongoingJobs.size} In-Flight",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) ServoraTheme.colors.info else Color(0xFF2563EB)
                            )
                        }
                    }
                }

                items(ongoingJobs, key = { "ongoing_${it.id}" }) { booking ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        PartnerJobSummaryCard(
                            booking = booking,
                            isNew = false,
                            onOpenJobClick = { onOpenJob(booking.id) },
                            unreadCount = unreadByBookingId[booking.id] ?: 0,
                            onOpenChat = { onOpenChat(booking) }
                        )
                    }
                }
            }

            // ================= 5. VIEW ALL BOOKINGS LINK =================
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onViewAllBookings() },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View All Bookings & History",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            ),
                            color = ServoraTheme.colors.textPrimary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = ServoraTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
