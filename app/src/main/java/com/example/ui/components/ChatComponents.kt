package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageUi
import com.example.data.model.MessageStatus
import com.example.ui.theme.ServoraTheme
import com.example.ui.viewmodel.IncomingBannerData
import com.example.util.ChatTime

@Composable
fun MessageStatusTicks(
    status: MessageStatus,
    modifier: Modifier = Modifier
) {
    val colors = ServoraTheme.colors
    when (status) {
        MessageStatus.SENDING -> {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Sending",
                tint = colors.textMuted,
                modifier = modifier.size(13.dp)
            )
        }
        MessageStatus.SENT -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = colors.textMuted,
                modifier = modifier.size(13.dp)
            )
        }
        MessageStatus.DELIVERED -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                tint = colors.textMuted,
                modifier = modifier.size(14.dp)
            )
        }
        MessageStatus.READ -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read",
                tint = colors.chatReadTick,
                modifier = modifier.size(14.dp)
            )
        }
        MessageStatus.FAILED -> {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Failed",
                tint = colors.danger,
                modifier = modifier.size(13.dp)
            )
        }
    }
}

@Composable
fun ChatSafetyBanner(modifier: Modifier = Modifier) {
    val colors = ServoraTheme.colors
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chat_safety_banner"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.successContainer),
        border = BorderStroke(1.dp, colors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Security",
                tint = colors.success,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Chats are private between you and your professional. Servora may review chats for safety and support.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                ),
                color = colors.onSuccessContainer
            )
        }
    }
}

@Composable
fun AdminAuditBanner(modifier: Modifier = Modifier) {
    val colors = ServoraTheme.colors
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chat_admin_banner"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.warningContainer),
        border = BorderStroke(1.dp, colors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Audit Notice",
                tint = colors.warning,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Read-only · Admin audit view · This access is logged for safety compliance.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 15.sp
                ),
                color = colors.onWarningContainer
            )
        }
    }
}

@Composable
fun DayDivider(dateText: String, modifier: Modifier = Modifier) {
    val colors = ServoraTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = colors.textSecondary
            )
        }
    }
}

@Composable
fun BookingUpdateCard(
    message: ChatMessageUi,
    modifier: Modifier = Modifier
) {
    val text = message.text
    val colors = ServoraTheme.colors

    val serviceTitle = remember(text) {
        val colonIdx = text.indexOf(":")
        val parenIdx = text.indexOf("(ID:")
        if (colonIdx != -1 && parenIdx != -1 && parenIdx > colonIdx) {
            text.substring(colonIdx + 1, parenIdx).trim()
        } else if (colonIdx != -1) {
            val forIdx = text.indexOf(" for ")
            if (forIdx != -1 && forIdx > colonIdx) {
                text.substring(colonIdx + 1, forIdx).trim()
            } else {
                text.substring(colonIdx + 1).trim()
            }
        } else {
            "Service Booking"
        }
    }

    val bookingCode = remember(text) {
        val idRegex = Regex("""(?:ID:\s*#?|#)([A-Za-z0-9\-]+)""")
        idRegex.find(text)?.groupValues?.get(1)?.let { "#$it" } ?: ""
    }

    val scheduleText = remember(text) {
        val forIdx = text.indexOf(" for ")
        if (forIdx != -1) {
            text.substring(forIdx + 5).trim()
        } else {
            ""
        }
    }

    val formattedTime = remember(message.createdAtMillis) {
        ChatTime.formatMessageTime(message.createdAtMillis)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("booking_update_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            border = BorderStroke(1.dp, colors.cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.successContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.success)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "SERVICE BOOKING ACCEPTED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp
                                ),
                                color = colors.onSuccessContainer
                            )
                        }
                    }

                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = colors.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Service Name Title
                Text(
                    text = serviceTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Details Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    if (bookingCode.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Booking ID:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = bookingCode,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.textPrimary
                            )
                        }
                    }

                    if (scheduleText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Scheduled:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = scheduleText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = colors.success
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessageUi,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier,
    isFirstInGroup: Boolean = true,
    showTicks: Boolean = isOutgoing,
    failureReason: String? = null,
    onRetry: (() -> Unit)? = null
) {
    if (message.kind == "BOOKING_UPDATE") {
        BookingUpdateCard(message = message, modifier = modifier)
        return
    }

    val colors = ServoraTheme.colors

    if (message.kind == "SYSTEM" || message.senderRole == "SYSTEM") {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.chatSystemBubble,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = colors.onChatSystemBubble,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val bubbleColor = if (isOutgoing) colors.chatBubbleMine else colors.chatBubbleTheirs
    val textColor = if (isOutgoing) colors.onChatBubbleMine else colors.onChatBubbleTheirs
    val subTextColor = if (isOutgoing) colors.onChatBubbleMine.copy(alpha = 0.75f) else colors.chatBubbleTheirsMeta

    val shape = if (isOutgoing) {
        if (isFirstInGroup) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        } else {
            RoundedCornerShape(16.dp)
        }
    } else {
        if (isFirstInGroup) {
            RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        } else {
            RoundedCornerShape(16.dp)
        }
    }

    val formattedTime = remember(message.createdAtMillis) {
        ChatTime.formatMessageTime(message.createdAtMillis)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (isOutgoing) 48.dp else 0.dp,
                end = if (isOutgoing) 0.dp else 48.dp,
                top = if (isFirstInGroup) 4.dp else 1.dp,
                bottom = 1.dp
            ),
        contentAlignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        val maxBubbleWidth = maxWidth * 0.78f

        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
        ) {
            // Bubble
            Surface(
                shape = shape,
                color = bubbleColor,
                border = if (!isOutgoing) BorderStroke(1.dp, colors.chatBubbleTheirsBorder) else null,
                shadowElevation = if (!isOutgoing && !colors.isDark) 1.dp else 0.dp,
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .semantics {
                        contentDescription = if (isOutgoing) "Sent message: ${message.text}" else "Received message: ${message.text}"
                    }
                    .testTag(if (isOutgoing) "chat_bubble_outgoing" else "chat_bubble_incoming")
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Kind badge if special (ETA, etc.)
                    if (message.kind == "ETA") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.successContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "⏱ ETA UPDATE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.onSuccessContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Message text
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 19.sp
                        ),
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Timestamp and delivery status
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = subTextColor
                        )

                        if (showTicks) {
                            Spacer(modifier = Modifier.width(4.dp))
                            MessageStatusTicks(status = message.status)
                        }
                    }
                }
            }

            // Retry button if failed
            if (message.status == MessageStatus.FAILED && onRetry != null) {
                Row(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clickable { onRetry() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = colors.danger,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = failureReason ?: "Failed to send. Tap to retry",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = colors.danger
                    )
                }
            }
        }
    }
}

@Composable
fun IncomingMessageBanner(
    bannerData: IncomingBannerData,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ServoraTheme.colors
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("incoming_message_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.success),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = Color.White, // theme-invariant
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bannerData.title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = bannerData.messageText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun QuickReplyRow(
    replies: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ServoraTheme.colors
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_reply_row")
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(replies) { reply ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                    .background(colors.chipBackground)
                    .clickable { onSelect(reply) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = reply,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = colors.chipText
                )
            }
        }
    }
}
