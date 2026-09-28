package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.example.ui.components.stableStatusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotification
import com.example.data.model.NotificationType
import com.example.data.model.UserRole
import com.example.ui.theme.ServoraTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Theme Colors
private val BrandGreen = Color(0xFF009051)
private val BrandGreenDark = Color(0xFF007340)
private val BrandGreenLight = Color(0xFFE6F5EF)
private val BrandGreenTint = Color(0xFFCCEBDC)

private enum class NotificationFilter(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Default.Notifications),
    BOOKINGS("Bookings", Icons.Default.CalendarMonth),
    MESSAGES("Messages", Icons.AutoMirrored.Filled.Chat),
    OFFERS("Offers & Updates", Icons.Default.LocalOffer)
}

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    userRole: UserRole,
    onNotificationClick: (AppNotification) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onClearAll: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(NotificationFilter.ALL) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Role-filtered notifications
    val roleFilteredNotifications = remember(notifications, userRole) {
        notifications.filter { it.targetRole == userRole }
    }

    // Category-filtered notifications
    val displayedNotifications = remember(roleFilteredNotifications, selectedFilter) {
        when (selectedFilter) {
            NotificationFilter.ALL -> roleFilteredNotifications
            NotificationFilter.BOOKINGS -> roleFilteredNotifications.filter {
                it.type in listOf(
                    NotificationType.NEW_BOOKING,
                    NotificationType.BOOKING_ASSIGNED,
                    NotificationType.BOOKING_CANCELLED,
                    NotificationType.STATUS_UPDATE,
                    NotificationType.JOB_STARTED,
                    NotificationType.PAYMENT_COLLECTED
                )
            }
            NotificationFilter.MESSAGES -> roleFilteredNotifications.filter {
                it.type == NotificationType.NEW_MESSAGE
            }
            NotificationFilter.OFFERS -> roleFilteredNotifications.filter {
                it.type in listOf(NotificationType.PROMO_OFFER, NotificationType.SYSTEM_UPDATE)
            }
        }
    }

    val unreadCount = remember(roleFilteredNotifications) {
        roleFilteredNotifications.count { !it.isRead }
    }

    // Grouping by time: Today, Yesterday, Earlier
    val groupedNotifications = remember(displayedNotifications) {
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val todayStart = now - (now % oneDayMillis)

        val todayList = mutableListOf<AppNotification>()
        val yesterdayList = mutableListOf<AppNotification>()
        val earlierList = mutableListOf<AppNotification>()

        displayedNotifications.forEach { notif ->
            val diff = now - notif.timestamp
            when {
                diff < oneDayMillis -> todayList.add(notif)
                diff < 2 * oneDayMillis -> yesterdayList.add(notif)
                else -> earlierList.add(notif)
            }
        }

        buildList {
            if (todayList.isNotEmpty()) add("Today" to todayList)
            if (yesterdayList.isNotEmpty()) add("Yesterday" to yesterdayList)
            if (earlierList.isNotEmpty()) add("Earlier" to earlierList)
        }
    }

    val isDark = ServoraTheme.colors.isDark
    val emeraldGreen = if (isDark) ServoraTheme.colors.primary else BrandGreen
    val headerBg = if (isDark) ServoraTheme.colors.headerBackgroundStart else BrandGreen

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ================= 1. MODERN BRAND GREEN HEADER (#009051) =================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = headerBg,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .stableStatusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button + Header Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)) /* theme-invariant */
                                    .clickable { onBackClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White, /* theme-invariant */
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notifications",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 20.sp,
                                        letterSpacing = 0.sp
                                    ),
                                    color = Color.White, /* theme-invariant */
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (userRole == UserRole.PROFESSIONAL) "Partner alerts & job updates" else "Booking alerts & messages",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = Color.White.copy(alpha = 0.85f), /* theme-invariant */
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Header Actions Menu (Clean circular buttons)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)) /* theme-invariant */
                                        .clickable { onMarkAllAsRead() }
                                        .testTag("mark_all_read_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Mark all read",
                                        tint = Color.White, /* theme-invariant */
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            // 3-dot overflow options menu
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)) /* theme-invariant */
                                        .clickable { showOptionsMenu = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = Color.White, /* theme-invariant */
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showOptionsMenu,
                                    onDismissRequest = { showOptionsMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Mark all as read", color = ServoraTheme.colors.textPrimary) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.DoneAll,
                                                contentDescription = null,
                                                tint = emeraldGreen
                                            )
                                        },
                                        onClick = {
                                            showOptionsMenu = false
                                            onMarkAllAsRead()
                                        }
                                    )
                                    if (roleFilteredNotifications.isNotEmpty()) {
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "Clear all notifications",
                                                    color = ServoraTheme.colors.danger
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.DeleteOutline,
                                                    contentDescription = null,
                                                    tint = ServoraTheme.colors.danger
                                                )
                                            },
                                            onClick = {
                                                showOptionsMenu = false
                                                showClearConfirmDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= 2. MODERN CAPSULE FILTER TABS =================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(NotificationFilter.entries) { filter ->
                        val isSelected = selectedFilter == filter
                        val filterCount = when (filter) {
                            NotificationFilter.ALL -> roleFilteredNotifications.size
                            NotificationFilter.BOOKINGS -> roleFilteredNotifications.count {
                                it.type in listOf(
                                    NotificationType.NEW_BOOKING,
                                    NotificationType.BOOKING_ASSIGNED,
                                    NotificationType.BOOKING_CANCELLED,
                                    NotificationType.STATUS_UPDATE,
                                    NotificationType.JOB_STARTED,
                                    NotificationType.PAYMENT_COLLECTED
                                )
                            }
                            NotificationFilter.MESSAGES -> roleFilteredNotifications.count {
                                it.type == NotificationType.NEW_MESSAGE
                            }
                            NotificationFilter.OFFERS -> roleFilteredNotifications.count {
                                it.type in listOf(NotificationType.PROMO_OFFER, NotificationType.SYSTEM_UPDATE)
                            }
                        }

                        val filterLabel = if (userRole == UserRole.PROFESSIONAL && filter == NotificationFilter.BOOKINGS) "Duties" else filter.label

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) emeraldGreen else (if (isDark) ServoraTheme.colors.surfaceVariant else Color(0xFFF1F5F9)))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) emeraldGreen else ServoraTheme.colors.cardBorder,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = filter.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) (if (isDark) ServoraTheme.colors.onPrimary else Color.White /* theme-invariant */) else ServoraTheme.colors.subtext,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = filterLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Medium,
                                        fontSize = 13.sp
                                    ),
                                    color = if (isSelected) (if (isDark) ServoraTheme.colors.onPrimary else Color.White /* theme-invariant */) else ServoraTheme.colors.textPrimary
                                )
                                if (filterCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) (if (isDark) ServoraTheme.colors.onPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.25f)) else (if (isDark) ServoraTheme.colors.surfaceVariant else Color(0xFFE2E8F0))
                                    ) {
                                        Text(
                                            text = "$filterCount",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp
                                            ),
                                            color = if (isSelected) (if (isDark) ServoraTheme.colors.onPrimary else Color.White /* theme-invariant */) else ServoraTheme.colors.subtext
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= 3. NOTIFICATION LIST / GROUPED SECTIONS =================
            if (displayedNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(if (isDark) ServoraTheme.colors.surfaceVariant else BrandGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsNone,
                                contentDescription = null,
                                tint = emeraldGreen,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "No Notifications",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp
                            ),
                            color = ServoraTheme.colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = when (selectedFilter) {
                                NotificationFilter.ALL -> "You're completely caught up! New alerts about bookings, assignments, and messages will show up here."
                                NotificationFilter.BOOKINGS -> "No booking alerts in this section."
                                NotificationFilter.MESSAGES -> "No chat message notifications."
                                NotificationFilter.OFFERS -> "No offers or updates right now."
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.5.sp,
                                lineHeight = 19.sp
                            ),
                            color = ServoraTheme.colors.subtext,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedNotifications.forEach { (sectionHeader, sectionItems) ->
                        item(key = "header_$sectionHeader") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sectionHeader.uppercase(Locale.getDefault()),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = ServoraTheme.colors.subtext
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(ServoraTheme.colors.divider)
                                )
                            }
                        }

                        items(
                            items = sectionItems,
                            key = { it.id }
                        ) { notif ->
                            NotificationCard(
                                notification = notif,
                                onClick = { onNotificationClick(notif) },
                                onDelete = { onDeleteNotification(notif.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Clearing All Notifications
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear all notifications?",
                    fontWeight = FontWeight.Medium,
                    color = ServoraTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This will remove all notifications for your current role. You cannot undo this action.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ServoraTheme.colors.subtext
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearAll()
                    }
                ) {
                    Text(
                        text = "Clear All",
                        fontWeight = FontWeight.Medium,
                        color = ServoraTheme.colors.danger
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(text = "Cancel", color = ServoraTheme.colors.subtext)
                }
            }
        )
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = ServoraTheme.colors.isDark
    val emeraldGreen = if (isDark) ServoraTheme.colors.primary else BrandGreen
    val typeConfig = getNotificationTypeConfig(notification.type, isDark)
    val timeAgo = formatTimeAgo(notification.timestamp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("notif_card_${notification.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead) (if (isDark) ServoraTheme.colors.cardBackgroundSubtle else Color(0xFFF9FDFB)) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (!notification.isRead) 1.2.dp else 1.dp,
            color = if (!notification.isRead) (if (isDark) ServoraTheme.colors.primary.copy(alpha = 0.5f) else BrandGreenTint) else ServoraTheme.colors.cardBorder
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (!notification.isRead) 2.dp else 0.5.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 1. Left Gradient Squircle Icon Badge (46.dp)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                typeConfig.backgroundColor,
                                typeConfig.backgroundColor.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .border(1.dp, typeConfig.borderColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeConfig.icon,
                    contentDescription = null,
                    tint = typeConfig.iconColor,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(modifier = Modifier.width(13.dp))

            // 2. Main Content Column
            Column(modifier = Modifier.weight(1f)) {
                // Top Title & Timestamp Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (!notification.isRead) FontWeight.Medium else FontWeight.Medium,
                                fontSize = 14.5.sp,
                                letterSpacing = 0.sp
                            ),
                            color = ServoraTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!notification.isRead) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(emeraldGreen)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = timeAgo,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = ServoraTheme.colors.subtext
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Message Text
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.5.sp
                    ),
                    color = ServoraTheme.colors.subtext
                )

                // 3. Bottom Interactive Action Button Strip (If booking reference or action present)
                if (!notification.bookingCode.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) ServoraTheme.colors.primaryContainer else BrandGreenLight)
                            .border(0.6.dp, if (isDark) ServoraTheme.colors.cardBorder else BrandGreenTint, RoundedCornerShape(8.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = if (isDark) ServoraTheme.colors.primary else BrandGreenDark,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Ref: ${notification.bookingCode}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp
                            ),
                            color = if (isDark) ServoraTheme.colors.primary else BrandGreenDark
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (isDark) ServoraTheme.colors.primary else BrandGreenDark,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else if (notification.type == NotificationType.PROMO_OFFER) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF3B0764) else Color(0xFFF3E8FF))
                            .border(0.6.dp, if (isDark) Color(0xFF581C87) else Color(0xFFE9D5FF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFC084FC) else Color(0xFF7E22CE),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "View Offers ➔",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp
                            ),
                            color = if (isDark) Color(0xFFC084FC) else Color(0xFF7E22CE)
                        )
                    }
                }
            }
        }
    }
}

private data class NotificationTypeConfig(
    val icon: ImageVector,
    val iconColor: Color,
    val backgroundColor: Color,
    val borderColor: Color
)

private fun getNotificationTypeConfig(type: NotificationType, isDark: Boolean = false): NotificationTypeConfig {
    return when (type) {
        NotificationType.NEW_BOOKING -> NotificationTypeConfig(
            icon = Icons.Default.CalendarMonth,
            iconColor = if (isDark) Color(0xFF34C27F) else Color(0xFF009051),
            backgroundColor = if (isDark) Color(0xFF0B3D26) else Color(0xFFE6F5EF),
            borderColor = if (isDark) Color(0xFF165337) else Color(0xFFCCEBDC)
        )
        NotificationType.BOOKING_ASSIGNED -> NotificationTypeConfig(
            icon = Icons.Default.Handyman,
            iconColor = if (isDark) Color(0xFF7AB8FF) else Color(0xFF0284C7),
            backgroundColor = if (isDark) Color(0xFF0E2742) else Color(0xFFE0F2FE),
            borderColor = if (isDark) Color(0xFF1E3A5F) else Color(0xFFBAE6FD)
        )
        NotificationType.BOOKING_CANCELLED -> NotificationTypeConfig(
            icon = Icons.Default.Cancel,
            iconColor = if (isDark) Color(0xFFFF6B6B) else Color(0xFFEF4444),
            backgroundColor = if (isDark) Color(0xFF3B1414) else Color(0xFFFEE2E2),
            borderColor = if (isDark) Color(0xFF5A1E1E) else Color(0xFFFECACA)
        )
        NotificationType.STATUS_UPDATE -> NotificationTypeConfig(
            icon = Icons.Default.CheckCircle,
            iconColor = if (isDark) Color(0xFF34C27F) else Color(0xFF059669),
            backgroundColor = if (isDark) Color(0xFF0B3D26) else Color(0xFFD1FAE5),
            borderColor = if (isDark) Color(0xFF165337) else Color(0xFFA7F3D0)
        )
        NotificationType.JOB_STARTED -> NotificationTypeConfig(
            icon = Icons.Default.ElectricBolt,
            iconColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
            backgroundColor = if (isDark) Color(0xFF3A2E0B) else Color(0xFFFEF3C7),
            borderColor = if (isDark) Color(0xFF594511) else Color(0xFFFDE68A)
        )
        NotificationType.PAYMENT_COLLECTED -> NotificationTypeConfig(
            icon = Icons.Default.CurrencyRupee,
            iconColor = if (isDark) Color(0xFF34C27F) else Color(0xFF16A34A),
            backgroundColor = if (isDark) Color(0xFF0B3D26) else Color(0xFFDCFCE7),
            borderColor = if (isDark) Color(0xFF165337) else Color(0xFFBBF7D0)
        )
        NotificationType.NEW_MESSAGE -> NotificationTypeConfig(
            icon = Icons.AutoMirrored.Filled.Chat,
            iconColor = if (isDark) Color(0xFFA5B4FC) else Color(0xFF6366F1),
            backgroundColor = if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF),
            borderColor = if (isDark) Color(0xFF312E81) else Color(0xFFE0E7FF)
        )
        NotificationType.PROMO_OFFER -> NotificationTypeConfig(
            icon = Icons.Default.LocalOffer,
            iconColor = if (isDark) Color(0xFFC084FC) else Color(0xFF9333EA),
            backgroundColor = if (isDark) Color(0xFF3B0764) else Color(0xFFF3E8FF),
            borderColor = if (isDark) Color(0xFF581C87) else Color(0xFFE9D5FF)
        )
        NotificationType.SYSTEM_UPDATE -> NotificationTypeConfig(
            icon = Icons.Default.Security,
            iconColor = if (isDark) Color(0xFFA9B7B0) else Color(0xFF475569),
            backgroundColor = if (isDark) Color(0xFF1F2723) else Color(0xFFF1F5F9),
            borderColor = if (isDark) Color(0xFF33403A) else Color(0xFFE2E8F0)
        )
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}
