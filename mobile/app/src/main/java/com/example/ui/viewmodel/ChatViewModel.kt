package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessageUi
import com.example.data.model.ConversationUi
import com.example.data.model.MessageStatus
import com.example.data.remote.chat.ChatMessageDto
import com.example.data.remote.chat.ContactScrubber
import com.example.data.remote.chat.ConversationSummaryDto
import com.example.data.repository.ChatRepository
import com.example.data.repository.FakeChatRepository
import com.example.util.ChatTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    private val repository: ChatRepository = FakeChatRepository(),
    private val enablePolling: Boolean = true
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<ConversationUi>>(emptyList())
    val conversations: StateFlow<List<ConversationUi>> = _conversations.asStateFlow()

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
        currentUserId = newProfileId
        currentUserRole = newRole
        com.example.data.remote.supabase.SupabaseClient.devProfileId = newProfileId
        com.example.data.remote.supabase.SupabaseClient.devUserRole = newRole

        globalSyncJob?.cancel()
        threadPollingJob?.cancel()
        _activeConversationId.value = null
        _activeThreadMessages.value = emptyList()
        _conversations.value = emptyList()
        _unreadTotal.value = 0
        _pendingMessages.value = emptyMap()
        _incomingBanner.value = null
        _errorMessage.value = null

        (repository as? com.example.data.repository.RemoteChatRepository)?.setIdentity(newProfileId, newRole)
        (repository as? FakeChatRepository)?.setIdentity(newProfileId, newRole)

        loadConversations()
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
            _isLoading.value = true
            repository.listConversations()
                .onSuccess { list ->
                    _errorMessage.value = null
                    updateConversationsFromDtos(list)
                }
                .onFailure { err ->
                    _errorMessage.value = err.message
                }
            _isLoading.value = false
        }
    }

    private fun updateConversationsFromDtos(dtos: List<ConversationSummaryDto>) {
        val mapped = dtos.map { dto ->
            val millis = ChatTime.parseIsoToEpochMillis(dto.lastMessageAt)
            ConversationUi(
                id = dto.conversationId,
                bookingId = dto.bookingId,
                bookingCode = dto.bookingCode,
                serviceName = dto.serviceName,
                counterpartName = dto.counterpartDisplayName,
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
        _unreadTotal.value = mapped.sumOf { it.unreadCount }
    }

    private fun startGlobalSyncLoop() {
        globalSyncJob?.cancel()
        globalSyncJob = viewModelScope.launch {
            var delayMs = 4000L
            while (isActive) {
                delay(delayMs)
                val previousConversations = _conversations.value
                val previousActiveId = _activeConversationId.value

                repository.syncChat()
                    .onSuccess { syncResp ->
                        delayMs = 4000L // Reset backoff

                        // Check for new incoming messages for background conversations to trigger banner
                        for (syncConv in syncResp.conversations) {
                            val prev = previousConversations.find { it.id == syncConv.conversationId }
                            val isDifferentThread = syncConv.conversationId != previousActiveId
                            val hasNewMessage = (prev != null && syncConv.latestSeq > prev.latestSeq) ||
                                                (prev == null && syncConv.latestSeq > 0)
                            val hasUnread = syncConv.unreadCount > (prev?.unreadCount ?: 0)

                            if (isDifferentThread && hasNewMessage && hasUnread) {
                                val convDetails = _conversations.value.find { it.id == syncConv.conversationId }
                                _incomingBanner.value = IncomingBannerData(
                                    conversationId = syncConv.conversationId,
                                    title = convDetails?.counterpartName ?: "New message",
                                    messageText = convDetails?.lastMessagePreview ?: "You received a new message"
                                )
                            }
                        }

                        // Re-fetch full conversation summaries if any sequence changed
                        val anySeqChanged = syncResp.conversations.any { sc ->
                            val prev = previousConversations.find { it.id == sc.conversationId }
                            prev == null || prev.latestSeq != sc.latestSeq || prev.unreadCount != sc.unreadCount ||
                            prev.peerReadSeq != sc.peerReadSeq || prev.peerDeliveredSeq != sc.peerDeliveredSeq
                        }

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
                        delayMs = (delayMs * 1.5).toLong().coerceAtMost(20000L)
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
                repository.readMessages(conversationId, afterSeq = 0)
                    .onSuccess { response ->
                        _errorMessage.value = null
                        activePeerReadSeq = response.peerReadSeq
                        activePeerDeliveredSeq = response.peerDeliveredSeq

                        val mapped = response.messages.map { msg ->
                            mapDtoToUi(msg, activePeerReadSeq, activePeerDeliveredSeq)
                        }
                        _activeThreadMessages.value = mapped

                        // Ack read up to highest seq
                        val maxSeq = response.lastSeq
                        if (maxSeq > 0) {
                            repository.ackRead(conversationId, maxSeq)
                        }

                        startThreadPolling(conversationId)
                        loadConversations() // refresh badges
                    }
                    .onFailure { err ->
                        _errorMessage.value = err.message
                    }
            }
            _isLoading.value = false
        }
    }

    private fun startThreadPolling(conversationId: String) {
        if (!enablePolling) return
        threadPollingJob?.cancel()
        threadPollingJob = viewModelScope.launch {
            var delayMs = 3000L
            while (isActive && _activeConversationId.value == conversationId) {
                delay(delayMs)
                val currentMaxSeq = _activeThreadMessages.value.mapNotNull { it.seq }.maxOrNull() ?: 0L
                repository.readMessages(conversationId, afterSeq = currentMaxSeq)
                    .onSuccess { newBatch ->
                        activePeerReadSeq = newBatch.peerReadSeq
                        activePeerDeliveredSeq = newBatch.peerDeliveredSeq

                        if (newBatch.messages.isNotEmpty()) {
                            val newUiMessages = newBatch.messages.map { msg ->
                                mapDtoToUi(msg, activePeerReadSeq, activePeerDeliveredSeq)
                            }

                            _activeThreadMessages.update { current ->
                                val existingMap = current.associateBy { it.id }.toMutableMap()
                                for (newMsg in newUiMessages) {
                                    existingMap[newMsg.id] = newMsg
                                }
                                existingMap.values.sortedWith(
                                    compareBy<ChatMessageUi> { it.seq ?: Long.MAX_VALUE }
                                        .thenBy { it.createdAtMillis }
                                )
                            }

                            // Ack read for newly arrived messages
                            val highestSeq = newBatch.lastSeq
                            if (highestSeq > 0) {
                                repository.ackRead(conversationId, highestSeq)
                            }
                        } else {
                            // Update status ticks of existing messages if peerReadSeq or peerDeliveredSeq changed
                            updateActiveThreadStatuses()
                        }
                        delayMs = 3000L
                    }
                    .onFailure {
                        delayMs = (delayMs * 1.5).toLong().coerceAtMost(10000L)
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
        val status = if (!dto.isMine) {
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
            isMine = dto.isMine,
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
            senderId = "me",
            senderRole = "CUSTOMER",
            text = text,
            kind = kind,
            createdAtMillis = nowMillis,
            isMine = true,
            status = MessageStatus.SENDING
        )

        _activeThreadMessages.update { it + optimisticUiMsg }

        viewModelScope.launch {
            repository.sendMessage(conversationId, clientMsgId, text, kind)
                .onSuccess { resp ->
                    _errorMessage.value = null
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

                    // Refresh conversation preview
                    loadConversations()
                }
                .onFailure { err ->
                    _pendingMessages.update { current ->
                        current + (clientMsgId to pending.copy(
                            status = MessageSendStatus.FAILED,
                            failureReason = err.message
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

                    _errorMessage.value = err.message ?: "Failed to send message"
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

        viewModelScope.launch {
            repository.sendMessage(conversationId, clientMsgId, text, kind)
                .onSuccess { resp ->
                    _errorMessage.value = null
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

                    loadConversations()
                }
                .onFailure { err ->
                    _pendingMessages.update { current ->
                        current + (clientMsgId to PendingMessage(clientMsgId, text, kind, MessageSendStatus.FAILED, err.message))
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

                    _errorMessage.value = err.message
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
    private val enablePolling: Boolean = true
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(repository, enablePolling) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
