package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.ChatConversationEntity
import com.example.data.db.ChatDao
import com.example.data.db.ChatMessageEntity
import com.example.data.model.ChatMessageUi
import com.example.data.model.ConversationUi
import com.example.data.model.MessageStatus
import com.example.data.remote.chat.ChatMessageDto
import com.example.data.remote.chat.ContactScrubber
import com.example.data.remote.chat.ConversationSummaryDto
import com.example.data.repository.ChatRepository
import com.example.data.repository.FakeChatRepository
import com.example.util.ChatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class MessageSendStatus {
    SENDING, SENT, DELIVERED, READ, FAILED
}

data class PendingMessage(
    val id: String,
    val text: String,
    val kind: String,
    val status: MessageSendStatus,
    val failureReason: String? = null
)

data class IncomingBannerData(
    val conversationId: String,
    val title: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel(
    private val repository: ChatRepository = com.example.data.repository.UnconfiguredChatRepository(),
    private val chatDao: ChatDao? = null,
    private val enablePolling: Boolean = true
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<ConversationUi>>(emptyList())
    val conversations: StateFlow<List<ConversationUi>> = _conversations.asStateFlow()

    private val _unreadByBookingId = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val unreadByBookingId: StateFlow<Map<Long, Int>> = _unreadByBookingId.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _activeThreadMessages = MutableStateFlow<List<ChatMessageUi>>(emptyList())
    val activeThreadMessages: StateFlow<List<ChatMessageUi>> = _activeThreadMessages.asStateFlow()

    private val _unreadTotal = MutableStateFlow(0)
    val unreadTotal: StateFlow<Int> = _unreadTotal.asStateFlow()

    private val _pendingMessages = MutableStateFlow<Map<String, PendingMessage>>(emptyMap())
    val pendingMessages: StateFlow<Map<String, PendingMessage>> = _pendingMessages.asStateFlow()

    private val _incomingBanner = MutableStateFlow<IncomingBannerData?>(null)
    val incomingBanner: StateFlow<IncomingBannerData?> = _incomingBanner.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentUserId: String = "user_priya_1"
    private var currentUserRole: String = "CUSTOMER"

    private fun isMine(senderId: String): Boolean = senderId == currentUserId

    private var activePeerReadSeq: Long = 0L
    private var activePeerDeliveredSeq: Long = 0L

    private var globalSyncJob: Job? = null
    private var threadPollingJob: Job? = null

    init {
        loadConversations()
        if (enablePolling) {
            startGlobalSyncLoop()
        }
    }

    fun onSessionChanged(newProfileId: String, newRole: String) {
        val isDifferentUser = newProfileId != currentUserId
        currentUserId = newProfileId
        currentUserRole = newRole
        com.example.data.remote.supabase.SupabaseClient.devProfileId = newProfileId
        com.example.data.remote.supabase.SupabaseClient.devUserRole = newRole

        globalSyncJob?.cancel()
        threadPollingJob?.cancel()
        _activeConversationId.value = null
        _activeThreadMessages.value = emptyList()
        _conversations.value = emptyList()
        _unreadByBookingId.value = emptyMap()
        _unreadTotal.value = 0
        _pendingMessages.value = emptyMap()
        _incomingBanner.value = null
        _errorMessage.value = null

        (repository as? com.example.data.repository.RemoteChatRepository)?.setIdentity(newProfileId, newRole)
        (repository as? FakeChatRepository)?.setIdentity(newProfileId, newRole)

        if (isDifferentUser && chatDao != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    chatDao.clearAllMessages()
                } catch (e: Exception) {
                    android.util.Log.w("ChatViewModel", "Error clearing messages on session switch: ${e.message}")
                }
                loadConversations()
            }
        } else {
            loadConversations()
        }
        if (enablePolling) {
            startGlobalSyncLoop()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun dismissBanner() {
        _incomingBanner.value = null
    }

    fun loadConversations() {
        viewModelScope.launch {
            // Load local Room cache first for instant display
            if (chatDao != null) {
                try {
                    val cached = withContext(Dispatchers.IO) { chatDao.getConversations(currentUserId) }
                    if (cached.isNotEmpty() && _conversations.value.isEmpty()) {
                        updateConversationsFromEntities(cached)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ChatViewModel", "Error loading cached conversations: ${e.message}")
                }
            }

            _isLoading.value = true
            repository.listConversations()
                .onSuccess { list ->
                    _errorMessage.value = null
                    updateConversationsFromDtos(list)

                    // Persist to Room
                    if (chatDao != null) {
                        withContext(Dispatchers.IO) {
                            try {
                                val entities = list.map { dto ->
                                    ChatConversationEntity(
                                        id = dto.conversationId,
                                        ownerProfileId = currentUserId,
                                        bookingId = dto.bookingId,
                                        bookingCode = dto.bookingCode ?: "",
                                        serviceName = dto.serviceName ?: "Service",
                                        counterpartDisplayName = dto.counterpartDisplayName ?: "Support",
                                        lastMessagePreview = dto.lastMessagePreview ?: "No messages yet",
                                        lastMessageAt = dto.lastMessageAt,
                                        lastMessageFromMe = dto.lastMessageFromMe,
                                        unreadCount = dto.unreadCount,
                                        status = dto.status,
                                        peerReadSeq = dto.peerReadSeq,
                                        peerDeliveredSeq = dto.peerDeliveredSeq,
                                        latestSeq = dto.latestSeq,
                                        localUpdatedAt = System.currentTimeMillis()
                                    )
                                }
                                chatDao.upsertConversations(entities)
                            } catch (e: Exception) {
                                android.util.Log.w("ChatViewModel", "Error saving conversations to Room: ${e.message}")
                            }
                        }
                    }
                }
                .onFailure { err ->
                    _errorMessage.value = err.message
                }
            _isLoading.value = false
        }
    }

    private fun updateConversationsFromEntities(entities: List<ChatConversationEntity>) {
        val mapped = entities.map { entity ->
            val millis = ChatTime.parseIsoToEpochMillis(entity.lastMessageAt)
            ConversationUi(
                id = entity.id,
                bookingId = entity.bookingId,
                bookingCode = entity.bookingCode,
                serviceName = entity.serviceName,
                counterpartName = entity.counterpartDisplayName,
                lastMessagePreview = entity.lastMessagePreview,
                lastMessageAtMillis = millis,
                lastMessageFromMe = entity.lastMessageFromMe,
                unreadCount = entity.unreadCount,
                status = entity.status,
                peerReadSeq = entity.peerReadSeq,
                peerDeliveredSeq = entity.peerDeliveredSeq,
                latestSeq = entity.latestSeq
            )
        }.sortedWith(
            compareByDescending<ConversationUi> { it.lastMessageAtMillis ?: 0L }
                .thenByDescending { it.id }
        )

        _conversations.value = mapped
        _unreadTotal.value = mapped.filter { it.status == "ACTIVE" }.sumOf { it.unreadCount }
        _unreadByBookingId.value = mapped.filter { it.status == "ACTIVE" && it.bookingId > 0 }.associate { it.bookingId to it.unreadCount }
    }

    private fun updateConversationsFromDtos(dtos: List<ConversationSummaryDto>) {
        val mapped = dtos.map { dto ->
            val millis = ChatTime.parseIsoToEpochMillis(dto.lastMessageAt)
            ConversationUi(
                id = dto.conversationId,
                bookingId = dto.bookingId,
                bookingCode = dto.bookingCode ?: "",
                serviceName = dto.serviceName ?: "Service",
                counterpartName = dto.counterpartDisplayName ?: "Support",
                lastMessagePreview = dto.lastMessagePreview ?: "No messages yet",
                lastMessageAtMillis = millis,
                lastMessageFromMe = dto.lastMessageFromMe,
                unreadCount = dto.unreadCount,
                status = dto.status,
                peerReadSeq = dto.peerReadSeq,
                peerDeliveredSeq = dto.peerDeliveredSeq,
                latestSeq = dto.latestSeq
            )
        }.sortedWith(
            compareByDescending<ConversationUi> { it.lastMessageAtMillis ?: 0L }
                .thenByDescending { it.id }
        )

        _conversations.value = mapped
        _unreadTotal.value = mapped.filter { it.status == "ACTIVE" }.sumOf { it.unreadCount }
        _unreadByBookingId.value = mapped.filter { it.status == "ACTIVE" && it.bookingId > 0 }.associate { it.bookingId to it.unreadCount }
    }

    private fun startGlobalSyncLoop() {
        globalSyncJob?.cancel()
        globalSyncJob = viewModelScope.launch {
            while (isActive) {
                val isListView = _activeConversationId.value == null
                val loopDelay = if (isListView) 4000L else 15000L
                delay(loopDelay)

                val previousConversations = _conversations.value
                val previousActiveId = _activeConversationId.value

                // Process pending outbox if any
                if (chatDao != null) {
                    try {
                        val outbox = withContext(Dispatchers.IO) { chatDao.getPendingOutboxMessages() }
                        for (pendingMsg in outbox) {
                            val clientMsgId = pendingMsg.clientMsgId ?: pendingMsg.id
                            repository.sendMessage(pendingMsg.conversationId, clientMsgId, pendingMsg.text, pendingMsg.kind)
                                .onSuccess { resp ->
                                    withContext(Dispatchers.IO) {
                                        chatDao.updateMessageSent(clientMsgId, resp.messageId, resp.seq, "SENT")
                                    }
                                }
                        }
                    } catch (_: Exception) {}
                }

                repository.syncChat()
                    .onSuccess { syncResp ->
                        // Check for new incoming messages for background conversations to trigger banner (ONLY FOR ACTIVE CHATS)
                        for (syncConv in syncResp.conversations) {
                            val prev = previousConversations.find { it.id == syncConv.conversationId }
                            val isDifferentThread = syncConv.conversationId != previousActiveId
                            val isActiveChat = syncConv.status == "ACTIVE"
                            val hasNewMessage = (prev != null && syncConv.latestSeq > prev.latestSeq) ||
                                                (prev == null && syncConv.latestSeq > 0)
                            val hasUnread = syncConv.unreadCount > (prev?.unreadCount ?: 0)

                            if (isActiveChat && isDifferentThread && hasNewMessage && hasUnread) {
                                val convDetails = _conversations.value.find { it.id == syncConv.conversationId }
                                _incomingBanner.value = IncomingBannerData(
                                    conversationId = syncConv.conversationId,
                                    title = convDetails?.counterpartName ?: "New message",
                                    messageText = convDetails?.lastMessagePreview ?: "You received a new message"
                                )
                            }
                        }

                        // Re-fetch full conversation summaries ONLY if sequence, unread count, or cursors changed
                        val prevMap = previousConversations.associateBy { it.id }
                        val anySeqChanged = syncResp.conversations.any { sc ->
                            val prev = prevMap[sc.conversationId]
                            prev == null || prev.latestSeq != sc.latestSeq || prev.unreadCount != sc.unreadCount ||
                            prev.peerReadSeq != sc.peerReadSeq || prev.peerDeliveredSeq != sc.peerDeliveredSeq
                        } || (syncResp.conversations.size != previousConversations.size)

                        if (anySeqChanged) {
                            repository.listConversations().onSuccess { dtos ->
                                updateConversationsFromDtos(dtos)
                            }
                        }

                        // If user is inside an active thread, refresh ticks or fetch new messages if seq advanced
                        if (previousActiveId != null) {
                            val activeSync = syncResp.conversations.find { it.conversationId == previousActiveId }
                            if (activeSync != null) {
                                activePeerReadSeq = activeSync.peerReadSeq
                                activePeerDeliveredSeq = activeSync.peerDeliveredSeq
                                updateActiveThreadStatuses()
                            }
                        }
                    }
                    .onFailure {
                        // Silent failure on background sync poll
                    }
            }
        }
    }

    fun openThreadForBooking(
        bookingId: Long,
        bookingCode: String? = null,
        serviceName: String? = null,
        onError: ((String) -> Unit)? = null,
        onReady: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.openConversation(bookingId, bookingCode, serviceName)
                .onSuccess { response ->
                    _errorMessage.value = null
                    loadConversations()
                    onReady(response.conversationId)
                }
                .onFailure { err ->
                    val msg = err.message ?: "Couldn't open chat — check your connection"
                    android.util.Log.w("ChatViewModel", "Remote openConversation failed for booking $bookingId ($bookingCode): $msg")
                    _errorMessage.value = msg
                    onError?.invoke(msg)
                }
            _isLoading.value = false
        }
    }

    fun openThread(conversationId: String, isAdminView: Boolean = false) {
        threadPollingJob?.cancel()
        _activeConversationId.value = conversationId

        // Dismiss banner if opening this conversation
        if (_incomingBanner.value?.conversationId == conversationId) {
            _incomingBanner.value = null
        }

        viewModelScope.launch {
            // Load messages from Room cache first
            if (chatDao != null && !isAdminView) {
                try {
                    val cachedMsgs = withContext(Dispatchers.IO) { chatDao.getMessages(conversationId) }
                    if (cachedMsgs.isNotEmpty()) {
                        _activeThreadMessages.value = cachedMsgs.map { entity ->
                            ChatMessageUi(
                                id = entity.id,
                                seq = if (entity.seq > 0) entity.seq else null,
                                senderId = entity.senderId,
                                senderRole = entity.senderRole,
                                text = entity.text,
                                kind = entity.kind,
                                createdAtMillis = entity.createdAt,
                                isMine = isMine(entity.senderId),
                                status = when (entity.status) {
                                    "SENDING" -> MessageStatus.SENDING
                                    "DELIVERED" -> MessageStatus.DELIVERED
                                    "READ" -> MessageStatus.READ
                                    "FAILED" -> MessageStatus.FAILED
                                    else -> MessageStatus.SENT
                                }
                            )
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ChatViewModel", "Error loading cached messages: ${e.message}")
                }
            }

            _isLoading.value = true
            if (isAdminView) {
                repository.readAdminConversation(conversationId)
                    .onSuccess { adminResp ->
                        _errorMessage.value = null
                        val mapped = adminResp.messages.map { msg ->
                            val millis = ChatTime.parseIsoToEpochMillis(msg.createdAt) ?: System.currentTimeMillis()
                            ChatMessageUi(
                                id = msg.id,
                                seq = msg.seq,
                                senderId = msg.senderId,
                                senderRole = msg.senderRole,
                                text = msg.text,
                                kind = msg.kind,
                                createdAtMillis = millis,
                                isMine = false,
                                status = MessageStatus.READ
                            )
                        }
                        _activeThreadMessages.value = mapped
                    }
                    .onFailure { err ->
                        _errorMessage.value = err.message
                    }
            } else {
                val cachedMaxSeq = if (chatDao != null) {
                    withContext(Dispatchers.IO) { chatDao.maxSeq(conversationId) }
                } else 0L

                // If cache exists, delta sync with afterSeq = cachedMaxSeq; otherwise full fetch afterSeq = 0
                val afterSeq = if (_activeThreadMessages.value.isNotEmpty()) cachedMaxSeq else 0L

                repository.readMessages(conversationId, afterSeq = afterSeq)
                    .onSuccess { response ->
                        _errorMessage.value = null
                        activePeerReadSeq = response.peerReadSeq
                        activePeerDeliveredSeq = response.peerDeliveredSeq

                        val incomingUiMsgs = response.messages.map { msg ->
                            mapDtoToUi(msg, activePeerReadSeq, activePeerDeliveredSeq)
                        }

                        if (afterSeq == 0L || _activeThreadMessages.value.isEmpty()) {
                            _activeThreadMessages.value = incomingUiMsgs
                        } else if (incomingUiMsgs.isNotEmpty()) {
                            _activeThreadMessages.update { current ->
                                val map = current.associateBy { it.id }.toMutableMap()
                                for (newMsg in incomingUiMsgs) {
                                    map[newMsg.id] = newMsg
                                }
                                map.values.sortedWith(
                                    compareBy<ChatMessageUi> { it.seq ?: Long.MAX_VALUE }
                                        .thenBy { it.createdAtMillis }
                                )
                            }
                        }

                        // Save to Room cache
                        if (chatDao != null && response.messages.isNotEmpty()) {
                            withContext(Dispatchers.IO) {
                                try {
                                    val entities = response.messages.map { msg ->
                                        ChatMessageEntity(
                                            id = msg.id,
                                            conversationId = conversationId,
                                            seq = msg.seq,
                                            senderId = msg.senderId,
                                            senderRole = msg.senderRole,
                                            isFromMe = isMine(msg.senderId),
                                            kind = msg.kind,
                                            text = msg.text,
                                            status = if (msg.seq <= activePeerReadSeq) "READ"
                                                     else if (msg.seq <= activePeerDeliveredSeq) "DELIVERED"
                                                     else "SENT",
                                            createdAt = ChatTime.parseIsoToEpochMillis(msg.createdAt) ?: System.currentTimeMillis()
                                        )
                                    }
                                    chatDao.upsertMessages(entities)
                                } catch (e: Exception) {
                                    android.util.Log.w("ChatViewModel", "Error saving messages to Room: ${e.message}")
                                }
                            }
                        }

                        // Ack read up to highest seq actually received in the batch
                        val maxSeq = response.messages.mapNotNull { it.seq }.maxOrNull() ?: 0L
                        if (maxSeq > 0) {
                            repository.ackRead(conversationId, maxSeq)
                        }

                        startThreadPolling(conversationId)
                        loadConversations() // refresh badges
                    }
                    .onFailure { err ->
                        _errorMessage.value = err.message
                        startThreadPolling(conversationId)
                    }
            }
            _isLoading.value = false
        }
    }

    private fun startThreadPolling(conversationId: String) {
        if (!enablePolling) return
        threadPollingJob?.cancel()
        threadPollingJob = viewModelScope.launch {
            var delayMs = 2500L
            var consecutiveFailures = 0
            while (isActive && _activeConversationId.value == conversationId) {
                delay(delayMs)
                val currentMaxSeq = _activeThreadMessages.value.mapNotNull { it.seq }.maxOrNull() ?: 0L
                repository.readMessages(conversationId, afterSeq = currentMaxSeq)
                    .onSuccess { newBatch ->
                        consecutiveFailures = 0
                        _errorMessage.value = null
                        activePeerReadSeq = newBatch.peerReadSeq
                        activePeerDeliveredSeq = newBatch.peerDeliveredSeq

                        if (newBatch.messages.isNotEmpty()) {
                            val newUiMessages = newBatch.messages.map { msg ->
                                mapDtoToUi(msg, activePeerReadSeq, activePeerDeliveredSeq)
                            }

                            _activeThreadMessages.update { current ->
                                if (current.isEmpty()) {
                                    newUiMessages
                                } else {
                                    val existingMap = current.associateBy { it.id }.toMutableMap()
                                    for (newMsg in newUiMessages) {
                                        existingMap[newMsg.id] = newMsg
                                    }
                                    existingMap.values.sortedWith(
                                        compareBy<ChatMessageUi> { it.seq ?: Long.MAX_VALUE }
                                            .thenBy { it.createdAtMillis }
                                    )
                                }
                            }

                            // Save to Room cache
                            if (chatDao != null) {
                                withContext(Dispatchers.IO) {
                                    try {
                                        val entities = newBatch.messages.map { msg ->
                                            ChatMessageEntity(
                                                id = msg.id,
                                                conversationId = conversationId,
                                                seq = msg.seq,
                                                senderId = msg.senderId,
                                                senderRole = msg.senderRole,
                                                isFromMe = isMine(msg.senderId),
                                                kind = msg.kind,
                                                text = msg.text,
                                                status = if (msg.seq <= activePeerReadSeq) "READ"
                                                         else if (msg.seq <= activePeerDeliveredSeq) "DELIVERED"
                                                         else "SENT",
                                                createdAt = ChatTime.parseIsoToEpochMillis(msg.createdAt) ?: System.currentTimeMillis()
                                            )
                                        }
                                        chatDao.upsertMessages(entities)
                                    } catch (_: Exception) {}
                                }
                            }

                            // Ack read for newly arrived messages up to highest batch seq
                            val highestSeq = newBatch.messages.mapNotNull { it.seq }.maxOrNull() ?: 0L
                            if (highestSeq > 0) {
                                repository.ackRead(conversationId, highestSeq)
                            }
                        } else {
                            // Update status ticks of existing messages if peerReadSeq or peerDeliveredSeq changed
                            updateActiveThreadStatuses()
                        }
                        delayMs = 3000L
                    }
                    .onFailure { err ->
                        consecutiveFailures++
                        if (consecutiveFailures >= 3) {
                            delayMs = 15000L
                            if (_errorMessage.value == null) {
                                _errorMessage.value = err.message ?: "Failed to read messages"
                            }
                        } else {
                            delayMs = (delayMs * 1.5).toLong().coerceAtMost(10000L)
                        }
                    }
            }
        }
    }

    private fun updateActiveThreadStatuses() {
        _activeThreadMessages.update { currentList ->
            currentList.map { msg ->
                if (!msg.isMine) {
                    msg
                } else {
                    val pending = _pendingMessages.value[msg.id]
                    val newStatus = when {
                        pending?.status == MessageSendStatus.SENDING -> MessageStatus.SENDING
                        pending?.status == MessageSendStatus.FAILED -> MessageStatus.FAILED
                        msg.seq != null && msg.seq <= activePeerReadSeq -> MessageStatus.READ
                        msg.seq != null && msg.seq <= activePeerDeliveredSeq -> MessageStatus.DELIVERED
                        else -> MessageStatus.SENT
                    }
                    msg.copy(status = newStatus)
                }
            }
        }
    }

    private fun mapDtoToUi(dto: ChatMessageDto, peerRead: Long, peerDelivered: Long): ChatMessageUi {
        val millis = ChatTime.parseIsoToEpochMillis(dto.createdAt) ?: System.currentTimeMillis()
        val pending = _pendingMessages.value[dto.id]
        val mine = isMine(dto.senderId)
        val status = if (!mine) {
            MessageStatus.SENT
        } else {
            when {
                pending?.status == MessageSendStatus.SENDING -> MessageStatus.SENDING
                pending?.status == MessageSendStatus.FAILED -> MessageStatus.FAILED
                dto.seq <= peerRead -> MessageStatus.READ
                dto.seq <= peerDelivered -> MessageStatus.DELIVERED
                else -> MessageStatus.SENT
            }
        }

        return ChatMessageUi(
            id = dto.id,
            seq = dto.seq,
            senderId = dto.senderId,
            senderRole = dto.senderRole,
            text = dto.text,
            kind = dto.kind,
            createdAtMillis = millis,
            isMine = mine,
            status = status
        )
    }

    fun leaveThread() {
        threadPollingJob?.cancel()
        threadPollingJob = null
        _activeConversationId.value = null
        _activeThreadMessages.value = emptyList()
        _errorMessage.value = null
        loadConversations()
    }

    fun sendMessage(conversationId: String, text: String, kind: String = "TEXT") {
        if (text.isBlank()) return

        // 1. Scrubber pre-validation
        if (kind == "TEXT") {
            val scrubResult = ContactScrubber.scrub(text)
            if (scrubResult.isBlocked) {
                _errorMessage.value = scrubResult.userMessage ?: "Contact sharing is restricted for your safety."
                return
            }
        }

        val clientMsgId = UUID.randomUUID().toString()
        val nowMillis = System.currentTimeMillis()

        val pending = PendingMessage(
            id = clientMsgId,
            text = text,
            kind = kind,
            status = MessageSendStatus.SENDING
        )

        _pendingMessages.update { it + (clientMsgId to pending) }

        // Optimistic UI insertion
        val optimisticUiMsg = ChatMessageUi(
            id = clientMsgId,
            seq = null, // unknown until server returns seq
            senderId = currentUserId,
            senderRole = currentUserRole,
            text = text,
            kind = kind,
            createdAtMillis = nowMillis,
            isMine = true,
            status = MessageStatus.SENDING
        )

        _activeThreadMessages.update { it + optimisticUiMsg }

        // Save to Room as SENDING outbox entity
        if (chatDao != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    chatDao.upsertMessage(
                        ChatMessageEntity(
                            id = clientMsgId,
                            conversationId = conversationId,
                            seq = 0L,
                            senderId = currentUserId,
                            senderRole = currentUserRole,
                            isFromMe = true,
                            kind = kind,
                            text = text,
                            status = "SENDING",
                            createdAt = nowMillis,
                            clientMsgId = clientMsgId
                        )
                    )
                } catch (_: Exception) {}
            }
        }

        viewModelScope.launch {
            repository.sendMessage(conversationId, clientMsgId, text, kind)
                .onSuccess { resp ->
                    _pendingMessages.update { current ->
                        current + (clientMsgId to pending.copy(status = MessageSendStatus.SENT))
                    }

                    // Update optimistic message with real seq and SENT status
                    _activeThreadMessages.update { list ->
                        list.map { msg ->
                            if (msg.id == clientMsgId) {
                                msg.copy(
                                    seq = resp.seq,
                                    status = if (resp.seq <= activePeerReadSeq) MessageStatus.READ
                                             else if (resp.seq <= activePeerDeliveredSeq) MessageStatus.DELIVERED
                                             else MessageStatus.SENT
                                )
                            } else {
                                msg
                            }
                        }
                    }

                    // Update Room database
                    if (chatDao != null) {
                        withContext(Dispatchers.IO) {
                            try {
                                chatDao.updateMessageSent(clientMsgId, resp.messageId, resp.seq, "SENT")
                            } catch (_: Exception) {}
                        }
                    }

                    // Refresh conversation preview
                    loadConversations()
                }
                .onFailure { err ->
                    val failureMsg = err.message ?: "Failed to send message"
                    _pendingMessages.update { current ->
                        current + (clientMsgId to pending.copy(
                            status = MessageSendStatus.FAILED,
                            failureReason = failureMsg
                        ))
                    }

                    _activeThreadMessages.update { list ->
                        list.map { msg ->
                            if (msg.id == clientMsgId) {
                                msg.copy(status = MessageStatus.FAILED)
                            } else {
                                msg
                            }
                        }
                    }

                    // Update Room database with FAILED
                    if (chatDao != null) {
                        withContext(Dispatchers.IO) {
                            try {
                                chatDao.updateMessageStatus(clientMsgId, "FAILED", failureMsg)
                            } catch (_: Exception) {}
                        }
                    }
                }
        }
    }

    fun retryMessage(conversationId: String, clientMsgId: String, text: String, kind: String = "TEXT") {
        _pendingMessages.update { current ->
            current + (clientMsgId to PendingMessage(clientMsgId, text, kind, MessageSendStatus.SENDING))
        }

        _activeThreadMessages.update { list ->
            list.map { msg ->
                if (msg.id == clientMsgId) {
                    msg.copy(status = MessageStatus.SENDING)
                } else {
                    msg
                }
            }
        }

        if (chatDao != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    chatDao.updateMessageStatus(clientMsgId, "SENDING", null)
                } catch (_: Exception) {}
            }
        }

        viewModelScope.launch {
            repository.sendMessage(conversationId, clientMsgId, text, kind)
                .onSuccess { resp ->
                    _pendingMessages.update { current ->
                        current + (clientMsgId to PendingMessage(clientMsgId, text, kind, MessageSendStatus.SENT))
                    }

                    _activeThreadMessages.update { list ->
                        list.map { msg ->
                            if (msg.id == clientMsgId) {
                                msg.copy(
                                    seq = resp.seq,
                                    status = if (resp.seq <= activePeerReadSeq) MessageStatus.READ
                                             else if (resp.seq <= activePeerDeliveredSeq) MessageStatus.DELIVERED
                                             else MessageStatus.SENT
                                )
                            } else {
                                msg
                            }
                        }
                    }

                    if (chatDao != null) {
                        withContext(Dispatchers.IO) {
                            try {
                                chatDao.updateMessageSent(clientMsgId, resp.messageId, resp.seq, "SENT")
                            } catch (_: Exception) {}
                        }
                    }

                    loadConversations()
                }
                .onFailure { err ->
                    val failureMsg = err.message ?: "Failed to send message"
                    _pendingMessages.update { current ->
                        current + (clientMsgId to PendingMessage(clientMsgId, text, kind, MessageSendStatus.FAILED, failureMsg))
                    }

                    _activeThreadMessages.update { list ->
                        list.map { msg ->
                            if (msg.id == clientMsgId) {
                                msg.copy(status = MessageStatus.FAILED)
                            } else {
                                msg
                            }
                        }
                    }

                    if (chatDao != null) {
                        withContext(Dispatchers.IO) {
                            try {
                                chatDao.updateMessageStatus(clientMsgId, "FAILED", failureMsg)
                            } catch (_: Exception) {}
                        }
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        globalSyncJob?.cancel()
        threadPollingJob?.cancel()
    }
}

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val chatDao: ChatDao? = null,
    private val enablePolling: Boolean = true
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(repository, chatDao, enablePolling) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
