package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConversationUi
import com.example.data.model.MessageStatus
import com.example.ui.components.MessageStatusTicks
import com.example.ui.components.hideStatusBarOnScroll
import com.example.ui.components.stableStatusBars
import com.example.ui.theme.ServoraTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.util.ChatTime

enum class ChatFilterTab(
    val label: String,
    val icon: ImageVector,
    val weight: Float
) {
    ACTIVE("Active Chats", Icons.Default.Bolt, 1.0f),
    RECENT("Recent Chats", Icons.Default.History, 1.0f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    chatViewModel: ChatViewModel,
    onOpenThread: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = ServoraTheme.colors
    val conversations by chatViewModel.conversations.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val unreadTotal by chatViewModel.unreadTotal.collectAsState()

    var selectedTab by remember { mutableStateOf(ChatFilterTab.ACTIVE) }

    // Segregate conversations into Active vs Recent (Completed/Closed/Read-only)
    val activeConversations = remember(conversations) {
        conversations.filter { it.status == "ACTIVE" }
    }
    val recentConversations = remember(conversations) {
        conversations.filter { it.status != "ACTIVE" }
    }

    val activeUnreadCount = remember(activeConversations) {
        activeConversations.sumOf { it.unreadCount }
    }
    val recentUnreadCount = remember(recentConversations) {
        recentConversations.sumOf { it.unreadCount }
    }

    val displayConversations = remember(selectedTab, activeConversations, recentConversations) {
        when (selectedTab) {
            ChatFilterTab.ACTIVE -> activeConversations
            ChatFilterTab.RECENT -> recentConversations
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.stableStatusBars,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.stableStatusBars,
                title = {
                    Column {
                        Text(
                            text = "Service Messages",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp
                            ),
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (unreadTotal > 0) "$unreadTotal unread in active chats" else "Direct & masked in-app support",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = if (unreadTotal > 0) colors.success else colors.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.textPrimary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { chatViewModel.loadConversations() },
                        modifier = Modifier.testTag("btn_refresh_chats")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = colors.success
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ================= REDESIGNED PREMIUM CAPSULE TAB BAR =================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, colors.cardBorder),
                shadowElevation = if (colors.isDark) 0.dp else 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChatFilterTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val count = if (tab == ChatFilterTab.ACTIVE) activeConversations.size else recentConversations.size
                        val tabUnread = if (tab == ChatFilterTab.ACTIVE) activeUnreadCount else recentUnreadCount

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { selectedTab = tab }
                                .testTag("chat_tab_${tab.name}"),
                            shape = RoundedCornerShape(22.dp),
                            color = if (isSelected) colors.success else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Icon with styled backdrop
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color.White.copy(alpha = 0.22f)
                                            else colors.chipBackground
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White /* theme-invariant */ else colors.success,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Medium,
                                        fontSize = 13.5.sp,
                                        letterSpacing = 0.sp
                                    ),
                                    color = if (isSelected) Color.White /* theme-invariant */ else colors.textPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )

                                if (tab == ChatFilterTab.ACTIVE && activeUnreadCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White /* theme-invariant */ else colors.success),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = activeUnreadCount.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = if (isSelected) colors.success else Color.White /* theme-invariant */
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading && conversations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.success)
                }
            } else if (displayConversations.isEmpty()) {
                EmptyChatsView(selectedTab = selectedTab)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .hideStatusBarOnScroll()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayConversations, key = { it.id }) { conv ->
                        ConversationItem(
                            conversation = conv,
                            onClick = { onOpenThread(conv.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(84.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: ConversationUi,
    onClick: () -> Unit
) {
    val colors = ServoraTheme.colors
    val isUnread = conversation.unreadCount > 0
    val timeFormatted = ChatTime.formatConversationListTime(conversation.lastMessageAtMillis)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_conversation_${conversation.id}")
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) {
                if (colors.isDark) colors.cardBackgroundSubtle else Color(0xFFFCFEFD)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isUnread) {
                if (colors.isDark) colors.success.copy(alpha = 0.5f) else Color(0xFFB7E4C7)
            } else {
                colors.cardBorder
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with initials
            val initials = conversation.counterpartName
                .split(" ", "·")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first() }
                .joinToString("")
                .ifBlank { "SA" }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isUnread) colors.chipBackground else colors.successContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = colors.success
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.counterpartName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Medium,
                            fontSize = 15.sp
                        ),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (timeFormatted.isNotBlank()) {
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal
                            ),
                            color = if (isUnread) colors.success else colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${conversation.bookingCode} · ${conversation.serviceName}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.success,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Status chip
                    val isClosed = conversation.status == "CLOSED" || conversation.status == "READ_ONLY"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isClosed) MaterialTheme.colorScheme.surfaceVariant
                                else colors.successContainer
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isClosed) "Read-only" else "Active",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (isClosed) colors.textSecondary else colors.onSuccessContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (conversation.lastMessageFromMe) {
                            val lastStatus = when {
                                conversation.latestSeq <= conversation.peerReadSeq -> MessageStatus.READ
                                conversation.latestSeq <= conversation.peerDeliveredSeq -> MessageStatus.DELIVERED
                                else -> MessageStatus.SENT
                            }
                            MessageStatusTicks(status = lastStatus)
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Text(
                            text = conversation.lastMessagePreview,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal
                            ),
                            color = if (isUnread) colors.textPrimary else colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF009051) /* theme-invariant */),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conversation.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White /* theme-invariant */
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatsView(
    selectedTab: ChatFilterTab = ChatFilterTab.ACTIVE
) {
    val colors = ServoraTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colors.chipBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selectedTab == ChatFilterTab.ACTIVE) Icons.AutoMirrored.Filled.Chat else Icons.Default.History,
                    contentDescription = null,
                    tint = colors.success,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (selectedTab == ChatFilterTab.ACTIVE) "No Active Chats" else "No Recent Chats",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                ),
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (selectedTab == ChatFilterTab.ACTIVE) {
                    "Only active bookings and ongoing jobs appear in Active Chats. Once a job is completed or cancelled, its chat will move to Recent Chats."
                } else {
                    "Your past booking conversations and completed job discussions will be archived here for your reference."
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
