package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.example.ui.theme.ServoraCharcoal
import com.example.ui.theme.ServoraGreen
import com.example.ui.theme.ServoraSubtext
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
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isAdminView) "Audit: ${currentConv?.bookingCode ?: conversationId}"
                                   else currentConv?.counterpartName ?: "Support & Pro Chat",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = ServoraCharcoal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val threadClosed = currentConv?.status == "CLOSED" || currentConv?.status == "READ_ONLY"
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (threadClosed) Color(0xFF9CA3AF) else ServoraGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAdminView) "Admin View (Read Only)"
                                       else if (threadClosed) "Closed • Read-Only"
                                       else currentConv?.serviceName ?: "Active Service",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (threadClosed) Color(0xFF6B7280) else ServoraGreen
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
                            tint = ServoraCharcoal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF7F8FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // Error banner if any
            errorMessage?.let { err ->
                Surface(
                    color = Color(0xFFFEF2F2),
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
                            color = Color(0xFFDC2626),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { chatViewModel.clearError() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFFDC2626),
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
            ) {
                if (isLoading && activeThreadMessages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ServoraGreen)
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
                            val isOutgoing = if (isAdminView) false else msg.isMine

                            ChatBubble(
                                message = msg,
                                isOutgoing = isOutgoing,
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
                    .background(Color.White)
            ) {
                HorizontalDivider(color = Color(0xFFE5E7EB))

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
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAdminView) "Admin view is read-only. Responses are disabled."
                                       else "This booking is completed. Chat is read-only.",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFF6B7280)
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
                            color = Color(0xFFFEF2F2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = scrubberWarning ?: "",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = Color(0xFFDC2626)
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
                                Text("Type a message...", style = MaterialTheme.typography.bodyMedium, color = ServoraSubtext)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field"),
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ServoraGreen,
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedContainerColor = Color(0xFFF9FAFB),
                                unfocusedContainerColor = Color(0xFFF9FAFB)
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
                                .background(if (canSend) ServoraGreen else Color(0xFFE5E7EB))
                                .testTag("btn_chat_send")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (canSend) Color.White else Color(0xFF9CA3AF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
