package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.chat.ContactScrubber
import com.example.ui.components.ChatBubble
import com.example.ui.components.DayDivider
import com.example.ui.components.QuickReplyRow
import com.example.ui.components.hideStatusBarOnScroll
import com.example.ui.components.stableStatusBars
import com.example.ui.theme.ServoraTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.util.ChatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    conversationId: String,
    chatViewModel: ChatViewModel,
    isAdminView: Boolean = false,
    isPartner: Boolean = false,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ServoraTheme.colors
    val conversations by chatViewModel.conversations.collectAsState()
    val activeThreadMessages by chatViewModel.activeThreadMessages.collectAsState()
    val pendingMessages by chatViewModel.pendingMessages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val errorMessage by chatViewModel.errorMessage.collectAsState()

    val currentConv = conversations.find { it.id == conversationId }

    var inputText by remember { mutableStateOf("") }
    var scrubberWarning by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    BackHandler {
        onBack()
    }

    LaunchedEffect(conversationId, isAdminView) {
        chatViewModel.openThread(conversationId, isAdminView)
    }

    DisposableEffect(conversationId) {
        onDispose {
            chatViewModel.leaveThread()
        }
    }

    // Auto-scroll to bottom on new messages
    val messageCount = activeThreadMessages.size
    LaunchedEffect(messageCount) {
        if (messageCount > 0) {
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    // Dynamic quick reply choices
    val quickReplies = if (isPartner) {
        listOf("On my way", "Arrived outside", "Stuck in traffic (5 mins)", "Service in progress", "Job completed, thank you!")
    } else {
        listOf("I'm home", "Please ring bell", "Running 5 mins late", "Please leave at door", "Thank you!")
    }

    val isClosed = currentConv?.status == "CLOSED" || currentConv?.status == "READ_ONLY" || isAdminView

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding(),
        contentWindowInsets = WindowInsets.stableStatusBars,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.stableStatusBars,
                title = {
                    Column {
                        Text(
                            text = if (isAdminView) "Audit: ${currentConv?.bookingCode ?: conversationId}"
                                   else currentConv?.counterpartName ?: "Support & Pro Chat",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val threadClosed = currentConv?.status == "CLOSED" || currentConv?.status == "READ_ONLY"
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (threadClosed) colors.textMuted else colors.success)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAdminView) "Admin View (Read Only)"
                                       else if (threadClosed) "Closed • Read-Only"
                                       else currentConv?.serviceName ?: "Active Service",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (threadClosed) colors.textSecondary else colors.success
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_chat_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // Contextual Top Banner showing Active Booking details
            currentConv?.let { conv ->
                Surface(
                    color = colors.successContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_active_booking_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (conv.status == "ACTIVE") colors.success else colors.textMuted)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Active Booking: ${conv.bookingCode}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = colors.onSuccessContainer
                                )
                                Text(
                                    text = conv.serviceName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp
                                    ),
                                    color = colors.success,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (colors.isDark) colors.cardBackgroundSubtle else Color(0xFFDCFCE7))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (conv.status == "ACTIVE") "ACTIVE" else conv.status,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.onSuccessContainer
                            )
                        }
                    }
                }
                HorizontalDivider(color = colors.cardBorder, thickness = 1.dp)
            }

            // Error banner if any
            errorMessage?.let { err ->
                Surface(
                    color = colors.dangerContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onDangerContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                chatViewModel.clearError()
                                chatViewModel.openThread(conversationId, isAdminView)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Retry",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.onDangerContainer
                            )
                        }
                        IconButton(
                            onClick = { chatViewModel.clearError() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = colors.onDangerContainer,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Messages area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(colors.chatWallpaper)
            ) {
                if (isLoading && activeThreadMessages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.success)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .hideStatusBarOnScroll()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(activeThreadMessages, key = { _, msg -> msg.id }) { index, msg ->
                            // Check if DayDivider is needed
                            val showDateDivider = if (index == 0) {
                                true
                            } else {
                                val prevMsg = activeThreadMessages[index - 1]
                                val prevDay = ChatTime.formatDayDivider(prevMsg.createdAtMillis)
                                val currentDay = ChatTime.formatDayDivider(msg.createdAtMillis)
                                prevDay != currentDay
                            }

                            if (showDateDivider) {
                                DayDivider(dateText = ChatTime.formatDayDivider(msg.createdAtMillis))
                            }

                            val pending = pendingMessages[msg.id]
                            val isOutgoing = if (isAdminView) msg.senderRole == "PARTNER" else msg.isMine
                            val showTicks = isOutgoing && !isAdminView

                            val prev = activeThreadMessages.getOrNull(index - 1)
                            val isFirstInGroup = showDateDivider || prev == null || prev.isMine != msg.isMine || prev.kind == "BOOKING_UPDATE" || prev.kind == "SYSTEM" || prev.senderRole == "SYSTEM"

                            ChatBubble(
                                message = msg,
                                isOutgoing = isOutgoing,
                                isFirstInGroup = isFirstInGroup,
                                showTicks = showTicks,
                                failureReason = pending?.failureReason,
                                onRetry = {
                                    chatViewModel.retryMessage(conversationId, msg.id, msg.text, msg.kind)
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Bottom Area (Composer / Read-Only Box)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                HorizontalDivider(color = colors.divider)

                if (isClosed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAdminView) "Admin view is read-only. Responses are disabled."
                                       else "This booking is completed. Chat is read-only.",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = colors.textSecondary
                            )
                        }
                    }
                } else {
                    // Quick reply row
                    QuickReplyRow(
                        replies = quickReplies,
                        onSelect = { reply ->
                            chatViewModel.sendMessage(conversationId, reply)
                        }
                    )

                    // Scrubber warning banner if active
                    AnimatedVisibility(visible = scrubberWarning != null) {
                        Surface(
                            color = colors.dangerContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = colors.danger,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = scrubberWarning ?: "",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = colors.onDangerContainer
                                )
                            }
                        }
                    }

                    // Input row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { newText ->
                                if (newText.length <= 1000) {
                                    inputText = newText
                                    val scrubbed = ContactScrubber.scrub(newText)
                                    scrubberWarning = if (scrubbed.isBlocked) {
                                        scrubbed.userMessage ?: "For your safety, sharing phone numbers or external links is not allowed."
                                    } else {
                                        null
                                    }
                                }
                            },
                            placeholder = {
                                Text("Type a message...", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field"),
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                focusedBorderColor = colors.success,
                                unfocusedBorderColor = colors.cardBorder,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank() && scrubberWarning == null) {
                                        chatViewModel.sendMessage(conversationId, inputText.trim())
                                        inputText = ""
                                    }
                                }
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        val canSend = inputText.isNotBlank() && scrubberWarning == null
                        IconButton(
                            onClick = {
                                if (canSend) {
                                    chatViewModel.sendMessage(conversationId, inputText.trim())
                                    inputText = ""
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (canSend) colors.success else colors.cardBorder)
                                .testTag("btn_chat_send")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (canSend) Color.White /* theme-invariant */ else colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
