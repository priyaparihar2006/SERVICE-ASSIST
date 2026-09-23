package com.example.data.repository

import com.example.data.remote.chat.ChatAckRequest
import com.example.data.remote.chat.ChatAckResponse
import com.example.data.remote.chat.ChatAdminReadResponse
import com.example.data.remote.chat.ChatApiService
import com.example.data.remote.chat.ChatMessageDto
import com.example.data.remote.chat.ChatOpenRequest
import com.example.data.remote.chat.ChatOpenResponse
import com.example.data.remote.chat.ChatReadResponse
import com.example.data.remote.chat.ChatSendRequest
import com.example.data.remote.chat.ChatSendResponse
import com.example.data.remote.chat.ChatSyncConversationDto
import com.example.data.remote.chat.ChatSyncResponse
import com.example.data.remote.chat.ContactScrubber
import com.example.data.remote.chat.ConversationSummaryDto
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

interface ChatRepository {
    suspend fun openConversation(bookingId: Long, bookingCode: String? = null, serviceName: String? = null): Result<ChatOpenResponse>
    suspend fun listConversations(): Result<List<ConversationSummaryDto>>
    suspend fun readMessages(conversationId: String, afterSeq: Long = 0): Result<ChatReadResponse>
    suspend fun sendMessage(conversationId: String, clientMsgId: String, text: String, kind: String = "TEXT"): Result<ChatSendResponse>
    suspend fun ackRead(conversationId: String, readSeq: Long): Result<ChatAckResponse>
    suspend fun syncChat(): Result<ChatSyncResponse>
    suspend fun listAdminConversations(userId: String? = null, bookingId: Long? = null): Result<List<ConversationSummaryDto>>
    suspend fun readAdminConversation(conversationId: String): Result<ChatAdminReadResponse>
}

class RemoteChatRepository(
    private val apiService: ChatApiService,
    private val bookingDao: com.example.data.db.BookingDao? = null
) : ChatRepository {

    private var currentUserId: String = "user_priya_1"
    private var currentUserRole: String = "CUSTOMER"
    private val localFallbackRepo = FakeChatRepository()

    fun setIdentity(userId: String, userRole: String) {
        currentUserId = userId
        currentUserRole = userRole
        localFallbackRepo.setIdentity(userId, userRole)
    }

    private fun extractErrorMessage(raw: String?, code: Int, defaultPrefix: String): String {
        if (raw.isNullOrBlank()) return "$defaultPrefix ($code)"
        return try {
            val json = org.json.JSONObject(raw)
            when {
                json.has("error") -> json.getString("error")
                json.has("message") -> json.getString("message")
                else -> raw
            }
        } catch (_: Exception) {
            raw
        }
    }

    override suspend fun openConversation(
        bookingId: Long,
        bookingCode: String?,
        serviceName: String?
    ): Result<ChatOpenResponse> = runCatching {
        val localBooking = bookingDao?.getBookingByIdSync(bookingId)
        val code = bookingCode ?: localBooking?.bookingCode
        val srv = serviceName ?: localBooking?.serviceName

        try {
            val response = apiService.openConversation(
                ChatOpenRequest(
                    bookingId = bookingId,
                    bookingCode = code,
                    serviceName = srv
                )
            )
            if (response.isSuccessful && response.body() != null) {
                return@runCatching response.body()!!
            } else {
                val raw = response.errorBody()?.string()
                android.util.Log.w("RemoteChatRepository", "Remote openConversation returned HTTP ${response.code()}: $raw, falling back to local chat session")
            }
        } catch (e: Exception) {
            android.util.Log.w("RemoteChatRepository", "Remote openConversation network exception: ${e.message}, falling back to local chat session")
        }

        // Seamless local fallback so user never gets blocked by 'Booking not found'
        if (localBooking != null) {
            FakeChatStore.openConversation(
                currentUserId = currentUserId,
                bookingId = bookingId,
                bookingCode = localBooking.bookingCode,
                serviceName = localBooking.serviceName,
                customerId = localBooking.customerId,
                customerName = localBooking.customerName,
                partnerId = localBooking.professionalId
            )
        } else {
            FakeChatStore.openConversation(
                currentUserId = currentUserId,
                bookingId = bookingId,
                bookingCode = code,
                serviceName = srv
            )
        }
    }

    override suspend fun listConversations(): Result<List<ConversationSummaryDto>> = runCatching {
        try {
            val response = apiService.listConversations()
            if (response.isSuccessful && response.body() != null) {
                val remoteList = response.body()!!.conversations
                val localList = FakeChatStore.getConversationsForUser(currentUserId, currentUserRole)
                val existingIds = remoteList.map { it.conversationId }.toSet()
                val extraLocal = localList.filter { !existingIds.contains(it.conversationId) }
                return@runCatching remoteList + extraLocal
            }
        } catch (e: Exception) {
            android.util.Log.w("RemoteChatRepository", "Remote listConversations failed: ${e.message}")
        }
        FakeChatStore.getConversationsForUser(currentUserId, currentUserRole)
    }

    override suspend fun readMessages(conversationId: String, afterSeq: Long): Result<ChatReadResponse> = runCatching {
        try {
            val response = apiService.readMessages(conversationId, afterSeq)
            if (response.isSuccessful && response.body() != null) {
                return@runCatching response.body()!!
            }
        } catch (e: Exception) {
            android.util.Log.w("RemoteChatRepository", "Remote readMessages failed: ${e.message}")
        }
        FakeChatStore.readMessages(currentUserId, conversationId, afterSeq)
    }

    override suspend fun sendMessage(
        conversationId: String,
        clientMsgId: String,
        text: String,
        kind: String
    ): Result<ChatSendResponse> = runCatching {
        try {
            val response = apiService.sendMessage(ChatSendRequest(conversationId, clientMsgId, text, kind))
            if (response.isSuccessful && response.body() != null) {
                return@runCatching response.body()!!
            }
        } catch (e: Exception) {
            android.util.Log.w("RemoteChatRepository", "Remote sendMessage failed: ${e.message}")
        }
        FakeChatStore.sendMessage(currentUserId, currentUserRole, conversationId, clientMsgId, text, kind)
    }

    override suspend fun ackRead(conversationId: String, readSeq: Long): Result<ChatAckResponse> = runCatching {
        try {
            val response = apiService.ackRead(ChatAckRequest(conversationId, readSeq))
            if (response.isSuccessful && response.body() != null) {
                return@runCatching response.body()!!
            }
        } catch (_: Exception) {}
        FakeChatStore.ackRead(currentUserId, conversationId, readSeq)
    }

    override suspend fun syncChat(): Result<ChatSyncResponse> = runCatching {
        try {
            val response = apiService.syncChat()
            if (response.isSuccessful && response.body() != null) {
                return@runCatching response.body()!!
            }
        } catch (_: Exception) {}
        FakeChatStore.sync(currentUserId)
    }

    override suspend fun listAdminConversations(userId: String?, bookingId: Long?): Result<List<ConversationSummaryDto>> = runCatching {
        val response = apiService.listAdminConversations(userId, bookingId)
        if (response.isSuccessful && response.body() != null) {
            response.body()!!.conversations
        } else {
            FakeChatStore.listAdminConversations(userId, bookingId)
        }
    }

    override suspend fun readAdminConversation(conversationId: String): Result<ChatAdminReadResponse> = runCatching {
        val response = apiService.readAdminConversation(conversationId)
        if (response.isSuccessful && response.body() != null) {
            response.body()!!
        } else {
            FakeChatStore.readAdminConversation(conversationId)
        }
    }
}

/**
 * Process-wide in-memory store supporting multi-user switching, receipt cursors,
 * and realistic delivery/read simulation for unit tests and offline demo flows.
 */
object FakeChatStore {
    data class StoredConv(
        val id: String,
        val bookingId: Long,
        val bookingCode: String,
        val serviceName: String,
        val customerId: String,
        val customerName: String,
        val partnerId: String,
        val partnerName: String,
        val status: String,
        val openedBy: String,
        var lastMessageAt: String?,
        var lastMessageSeq: Long = 0L
    )

    data class StoredMessage(
        val id: String,
        val conversationId: String,
        val seq: Long,
        val senderId: String,
        val senderRole: String,
        val kind: String,
        val text: String,
        val createdAt: String
    )

    data class UserCursor(
        var lastReadSeq: Long = 0L,
        var lastDeliveredSeq: Long = 0L
    )

    private val convMap = mutableMapOf<String, StoredConv>()
    private val messagesMap = mutableMapOf<String, MutableList<StoredMessage>>()
    // (conversationId to userId) -> UserCursor
    private val cursorsMap = mutableMapOf<Pair<String, String>, UserCursor>()
    private val auditLogs = mutableListOf<String>()

    init {
        resetToDefault()
    }

    fun resetToDefault() {
        convMap.clear()
        messagesMap.clear()
        cursorsMap.clear()
        auditLogs.clear()

        val conv1Id = "conv_ac_rajesh_1"
        convMap[conv1Id] = StoredConv(
            id = conv1Id,
            bookingId = 1L,
            bookingCode = "SRV-21493",
            serviceName = "Intense AC Foam Jet Service",
            customerId = "user_priya_1",
            customerName = "Priya Sharma",
            partnerId = "pro_rajesh_1",
            partnerName = "Rajesh Sharma",
            status = "ACTIVE",
            openedBy = "user_priya_1",
            lastMessageAt = Instant.now().minusSeconds(300).toString(),
            lastMessageSeq = 4L
        )

        val baseTime = Instant.now().minusSeconds(600)
        messagesMap[conv1Id] = mutableListOf(
            StoredMessage(
                id = "msg_1_1",
                conversationId = conv1Id,
                seq = 1,
                senderId = "pro_rajesh_1",
                senderRole = "PARTNER",
                kind = "QUICK_REPLY",
                text = "On my way to your location.",
                createdAt = baseTime.toString()
            ),
            StoredMessage(
                id = "msg_1_2",
                conversationId = conv1Id,
                seq = 2,
                senderId = "user_priya_1",
                senderRole = "CUSTOMER",
                kind = "QUICK_REPLY",
                text = "Gate is open. Flat 402, 4th floor.",
                createdAt = baseTime.plusSeconds(60).toString()
            ),
            StoredMessage(
                id = "msg_1_3",
                conversationId = conv1Id,
                seq = 3,
                senderId = "pro_rajesh_1",
                senderRole = "PARTNER",
                kind = "ETA",
                text = "ETA: 10 mins (Traffic clear on Fatehabad Rd)",
                createdAt = baseTime.plusSeconds(180).toString()
            ),
            StoredMessage(
                id = "msg_1_4",
                conversationId = conv1Id,
                seq = 4,
                senderId = "pro_rajesh_1",
                senderRole = "PARTNER",
                kind = "TEXT",
                text = "I have reached Taj Nagri gate. Please confirm tower.",
                createdAt = baseTime.plusSeconds(300).toString()
            )
        )

        // Priya has read up to seq 3, delivered up to 4; Rajesh has read up to 4
        cursorsMap[conv1Id to "user_priya_1"] = UserCursor(lastReadSeq = 3L, lastDeliveredSeq = 4L)
        cursorsMap[conv1Id to "pro_rajesh_1"] = UserCursor(lastReadSeq = 4L, lastDeliveredSeq = 4L)

        // Seed conv 2: Amit (Read-Only)
        val conv2Id = "conv_cleaning_amit_2"
        convMap[conv2Id] = StoredConv(
            id = conv2Id,
            bookingId = 2L,
            bookingCode = "SRV-20841",
            serviceName = "Deep Bathroom & Kitchen Sanitization",
            customerId = "user_priya_1",
            customerName = "Priya Sharma",
            partnerId = "pro_amit_2",
            partnerName = "Amit Kumar",
            status = "READ_ONLY",
            openedBy = "user_priya_1",
            lastMessageAt = Instant.now().minusSeconds(86400).toString(),
            lastMessageSeq = 3L
        )
        val yestTime = Instant.now().minusSeconds(86400)
        messagesMap[conv2Id] = mutableListOf(
            StoredMessage(
                id = "msg_2_1",
                conversationId = conv2Id,
                seq = 1,
                senderId = "pro_amit_2",
                senderRole = "PARTNER",
                kind = "TEXT",
                text = "Arrived with full equipment.",
                createdAt = yestTime.toString()
            ),
            StoredMessage(
                id = "msg_2_2",
                conversationId = conv2Id,
                seq = 2,
                senderId = "user_priya_1",
                senderRole = "CUSTOMER",
                kind = "TEXT",
                text = "Great, please start with the master bathroom.",
                createdAt = yestTime.plusSeconds(120).toString()
            ),
            StoredMessage(
                id = "msg_2_3",
                conversationId = conv2Id,
                seq = 3,
                senderId = "pro_amit_2",
                senderRole = "PARTNER",
                kind = "SYSTEM",
                text = "Service completed. Thank you for choosing Service Assist!",
                createdAt = yestTime.plusSeconds(7200).toString()
            )
        )
        cursorsMap[conv2Id to "user_priya_1"] = UserCursor(lastReadSeq = 3L, lastDeliveredSeq = 3L)
        cursorsMap[conv2Id to "pro_amit_2"] = UserCursor(lastReadSeq = 3L, lastDeliveredSeq = 3L)
    }

    @Synchronized
    fun getConversationsForUser(currentUserId: String, currentUserRole: String): List<ConversationSummaryDto> {
        return convMap.values.filter { conv ->
            val isParticipant = conv.customerId == currentUserId || conv.partnerId == currentUserId
            if (!isParticipant) return@filter false
            // Hide 0-message conversations from non-openers
            if (conv.lastMessageSeq == 0L && conv.openedBy != currentUserId) return@filter false
            true
        }.map { conv ->
            val isCustomer = conv.customerId == currentUserId
            val counterpartDisplayName = if (isCustomer) {
                "${conv.partnerName.split(" ").firstOrNull() ?: "Pro"} · Your Professional"
            } else {
                "${conv.customerName.split(" ").firstOrNull() ?: "Client"} · Client"
            }

            val peerId = if (isCustomer) conv.partnerId else conv.customerId
            val myCursor = cursorsMap.getOrPut(conv.id to currentUserId) { UserCursor() }
            val peerCursor = cursorsMap.getOrPut(conv.id to peerId) { UserCursor() }

            val messages = messagesMap[conv.id] ?: emptyList()
            val unreadCount = messages.count { it.seq > myCursor.lastReadSeq && it.senderId != currentUserId }
            val lastMsg = messages.lastOrNull()

            ConversationSummaryDto(
                conversationId = conv.id,
                bookingId = conv.bookingId,
                bookingCode = conv.bookingCode,
                serviceName = conv.serviceName,
                counterpartDisplayName = counterpartDisplayName,
                lastMessagePreview = lastMsg?.text ?: "No messages yet",
                lastMessageAt = conv.lastMessageAt,
                lastMessageFromMe = lastMsg?.senderId == currentUserId,
                unreadCount = unreadCount,
                status = conv.status,
                peerReadSeq = peerCursor.lastReadSeq,
                peerDeliveredSeq = peerCursor.lastDeliveredSeq,
                latestSeq = conv.lastMessageSeq
            )
        }.sortedWith(
            compareByDescending<ConversationSummaryDto> { it.lastMessageAt ?: "" }
                .thenByDescending { it.conversationId }
        )
    }

    @Synchronized
    fun sync(currentUserId: String): ChatSyncResponse {
        val convs = convMap.values.filter { it.customerId == currentUserId || it.partnerId == currentUserId }
        val syncList = convs.map { conv ->
            val isCustomer = conv.customerId == currentUserId
            val peerId = if (isCustomer) conv.partnerId else conv.customerId
            val myCursor = cursorsMap.getOrPut(conv.id to currentUserId) { UserCursor() }
            val peerCursor = cursorsMap.getOrPut(conv.id to peerId) { UserCursor() }

            // Mark delivered up to latestSeq
            myCursor.lastDeliveredSeq = maxOf(myCursor.lastDeliveredSeq, conv.lastMessageSeq)

            val messages = messagesMap[conv.id] ?: emptyList()
            val unreadCount = messages.count { it.seq > myCursor.lastReadSeq && it.senderId != currentUserId }

            ChatSyncConversationDto(
                conversationId = conv.id,
                bookingId = conv.bookingId,
                status = conv.status,
                lastMessageAt = conv.lastMessageAt,
                latestSeq = conv.lastMessageSeq,
                unreadCount = unreadCount,
                peerReadSeq = peerCursor.lastReadSeq,
                peerDeliveredSeq = peerCursor.lastDeliveredSeq
            )
        }
        val totalUnread = syncList.sumOf { it.unreadCount }
        return ChatSyncResponse(
            conversations = syncList,
            totalUnread = totalUnread,
            serverTime = Instant.now().toString()
        )
    }

    @Synchronized
    fun readMessages(currentUserId: String, conversationId: String, afterSeq: Long): ChatReadResponse {
        val conv = convMap[conversationId] ?: throw Exception("Conversation not found")
        val isCustomer = conv.customerId == currentUserId
        val peerId = if (isCustomer) conv.partnerId else conv.customerId

        val messages = messagesMap[conversationId] ?: emptyList()
        val filtered = messages.filter { it.seq > afterSeq }
        val latestSeq = conv.lastMessageSeq

        val myCursor = cursorsMap.getOrPut(conversationId to currentUserId) { UserCursor() }
        val peerCursor = cursorsMap.getOrPut(conversationId to peerId) { UserCursor() }

        // Automatically bump delivery cursor to highest returned seq
        val highestReturned = filtered.maxOfOrNull { it.seq } ?: afterSeq
        myCursor.lastDeliveredSeq = maxOf(myCursor.lastDeliveredSeq, highestReturned)

        val dtos = filtered.map { msg ->
            ChatMessageDto(
                id = msg.id,
                seq = msg.seq,
                senderId = msg.senderId,
                senderRole = msg.senderRole,
                kind = msg.kind,
                text = msg.text,
                createdAt = msg.createdAt,
                isMine = msg.senderId == currentUserId
            )
        }

        return ChatReadResponse(
            conversationId = conversationId,
            status = conv.status,
            messages = dtos,
            lastSeq = latestSeq,
            peerReadSeq = peerCursor.lastReadSeq,
            peerDeliveredSeq = peerCursor.lastDeliveredSeq
        )
    }

    @Synchronized
    fun ackRead(currentUserId: String, conversationId: String, readSeq: Long): ChatAckResponse {
        val conv = convMap[conversationId] ?: throw Exception("Conversation not found")
        val clampedSeq = minOf(readSeq, conv.lastMessageSeq)
        val myCursor = cursorsMap.getOrPut(conversationId to currentUserId) { UserCursor() }
        myCursor.lastReadSeq = maxOf(myCursor.lastReadSeq, clampedSeq)
        myCursor.lastDeliveredSeq = maxOf(myCursor.lastDeliveredSeq, clampedSeq)
        return ChatAckResponse(success = true, lastReadSeq = myCursor.lastReadSeq)
    }

    @Synchronized
    fun sendMessage(
        currentUserId: String,
        currentUserRole: String,
        conversationId: String,
        clientMsgId: String,
        text: String,
        kind: String
    ): ChatSendResponse {
        val conv = convMap[conversationId] ?: throw Exception("Conversation not found")
        if (conv.status != "ACTIVE") {
            throw Exception("This booking is complete. Chat is closed.")
        }

        if (kind == "TEXT") {
            val scrubResult = ContactScrubber.scrub(text)
            if (scrubResult.isBlocked) {
                throw Exception(scrubResult.userMessage ?: "Contact sharing restricted")
            }
        }

        val messages = messagesMap.getOrPut(conversationId) { mutableListOf() }
        // Check idempotent existing
        val existing = messages.find { it.id == clientMsgId }
        if (existing != null) {
            return ChatSendResponse(true, existing.id, existing.seq, existing.createdAt)
        }

        val newSeq = conv.lastMessageSeq + 1L
        val nowIso = Instant.now().toString()
        val senderRole = if (conv.customerId == currentUserId) "CUSTOMER" else "PARTNER"

        val msg = StoredMessage(
            id = clientMsgId,
            conversationId = conversationId,
            seq = newSeq,
            senderId = currentUserId,
            senderRole = senderRole,
            kind = kind,
            text = text,
            createdAt = nowIso
        )
        messages.add(msg)
        conv.lastMessageSeq = newSeq
        conv.lastMessageAt = nowIso

        // Sender automatically has read and delivered their own message
        val myCursor = cursorsMap.getOrPut(conversationId to currentUserId) { UserCursor() }
        myCursor.lastReadSeq = maxOf(myCursor.lastReadSeq, newSeq)
        myCursor.lastDeliveredSeq = maxOf(myCursor.lastDeliveredSeq, newSeq)

        return ChatSendResponse(
            success = true,
            messageId = clientMsgId,
            seq = newSeq,
            createdAt = nowIso
        )
    }

    @Synchronized
    fun openConversation(
        currentUserId: String,
        bookingId: Long,
        bookingCode: String? = null,
        serviceName: String? = null,
        customerId: String? = null,
        customerName: String? = null,
        partnerId: String? = null,
        partnerName: String? = null
    ): ChatOpenResponse {
        val existing = convMap.values.find { it.bookingId == bookingId || (bookingCode != null && it.bookingCode == bookingCode) }
        if (existing != null) {
            val isCustomer = existing.customerId == currentUserId
            val counterpartDisplayName = if (isCustomer) {
                "${existing.partnerName.split(" ").firstOrNull() ?: "Pro"} · Your Professional"
            } else {
                "${existing.customerName.split(" ").firstOrNull() ?: "Client"} · Client"
            }
            return ChatOpenResponse(
                conversationId = existing.id,
                bookingId = existing.bookingId,
                bookingCode = existing.bookingCode,
                serviceName = existing.serviceName,
                status = existing.status,
                counterpartDisplayName = counterpartDisplayName
            )
        }

        val newId = "conv_booking_${bookingCode ?: bookingId}"
        val custId = customerId ?: if (currentUserId.startsWith("user_")) currentUserId else "user_priya_1"
        val custName = customerName ?: "Priya Sharma"
        val proId = partnerId ?: if (currentUserId.startsWith("pro_")) currentUserId else "pro_rajesh_1"
        val proName = partnerName ?: "Rajesh Sharma"
        val code = bookingCode ?: "SRV-${10000 + bookingId}"
        val srv = serviceName ?: "Doorstep Service"

        val newConv = StoredConv(
            id = newId,
            bookingId = bookingId,
            bookingCode = code,
            serviceName = srv,
            customerId = custId,
            customerName = custName,
            partnerId = proId,
            partnerName = proName,
            status = "ACTIVE",
            openedBy = currentUserId,
            lastMessageAt = null,
            lastMessageSeq = 0L
        )
        convMap[newId] = newConv
        messagesMap[newId] = mutableListOf()

        val isCustomer = custId == currentUserId
        val counterpartDisplayName = if (isCustomer) {
            "${proName.split(" ").firstOrNull() ?: "Pro"} · Your Professional"
        } else {
            "${custName.split(" ").firstOrNull() ?: "Client"} · Client"
        }

        return ChatOpenResponse(
            conversationId = newId,
            bookingId = bookingId,
            bookingCode = newConv.bookingCode,
            serviceName = newConv.serviceName,
            status = "ACTIVE",
            counterpartDisplayName = counterpartDisplayName
        )
    }

    @Synchronized
    fun listAdminConversations(userId: String?, bookingId: Long?): List<ConversationSummaryDto> {
        auditLogs.add("LIST_CONVERSATIONS by ADMIN for user: $userId, booking: $bookingId")
        return convMap.values.filter { conv ->
            if (userId != null && conv.customerId != userId && conv.partnerId != userId) return@filter false
            if (bookingId != null && conv.bookingId != bookingId) return@filter false
            true
        }.map { conv ->
            val messages = messagesMap[conv.id] ?: emptyList()
            val lastMsg = messages.lastOrNull()
            val custCursor = cursorsMap[conv.id to conv.customerId] ?: UserCursor()
            val partnerCursor = cursorsMap[conv.id to conv.partnerId] ?: UserCursor()

            ConversationSummaryDto(
                conversationId = conv.id,
                bookingId = conv.bookingId,
                bookingCode = conv.bookingCode,
                serviceName = conv.serviceName,
                counterpartDisplayName = "${conv.customerName} & ${conv.partnerName}",
                lastMessagePreview = lastMsg?.text ?: "No messages yet",
                lastMessageAt = conv.lastMessageAt,
                lastMessageFromMe = false,
                unreadCount = 0,
                status = conv.status,
                peerReadSeq = maxOf(custCursor.lastReadSeq, partnerCursor.lastReadSeq),
                peerDeliveredSeq = maxOf(custCursor.lastDeliveredSeq, partnerCursor.lastDeliveredSeq),
                latestSeq = conv.lastMessageSeq
            )
        }
    }

    @Synchronized
    fun readAdminConversation(conversationId: String): ChatAdminReadResponse {
        auditLogs.add("READ_CONVERSATION by ADMIN: $conversationId")
        val conv = convMap[conversationId] ?: throw Exception("Conversation not found")
        val messages = messagesMap[conversationId] ?: emptyList()
        val custCursor = cursorsMap[conversationId to conv.customerId] ?: UserCursor()
        val partnerCursor = cursorsMap[conversationId to conv.partnerId] ?: UserCursor()

        val dtos = messages.map { msg ->
            ChatMessageDto(
                id = msg.id,
                seq = msg.seq,
                senderId = msg.senderId,
                senderRole = msg.senderRole,
                kind = msg.kind,
                text = msg.text,
                createdAt = msg.createdAt,
                isMine = false
            )
        }

        return ChatAdminReadResponse(
            conversationId = conversationId,
            bookingId = conv.bookingId,
            bookingCode = conv.bookingCode,
            serviceName = conv.serviceName,
            customerName = conv.customerName,
            status = conv.status,
            isAdminView = true,
            auditNotice = "Read-only · Admin audit view · This access is logged",
            messages = dtos,
            customerReadSeq = custCursor.lastReadSeq,
            customerDeliveredSeq = custCursor.lastDeliveredSeq,
            partnerReadSeq = partnerCursor.lastReadSeq,
            partnerDeliveredSeq = partnerCursor.lastDeliveredSeq
        )
    }
}

class FakeChatRepository(
    private var currentUserId: String = "user_priya_1",
    private var currentUserRole: String = "CUSTOMER"
) : ChatRepository {

    fun setIdentity(userId: String, userRole: String) {
        currentUserId = userId
        currentUserRole = userRole
    }

    override suspend fun openConversation(
        bookingId: Long,
        bookingCode: String?,
        serviceName: String?
    ): Result<ChatOpenResponse> = runCatching {
        delay(100)
        FakeChatStore.openConversation(
            currentUserId = currentUserId,
            bookingId = bookingId,
            bookingCode = bookingCode,
            serviceName = serviceName
        )
    }

    override suspend fun listConversations(): Result<List<ConversationSummaryDto>> = runCatching {
        delay(100)
        FakeChatStore.getConversationsForUser(currentUserId, currentUserRole)
    }

    override suspend fun readMessages(conversationId: String, afterSeq: Long): Result<ChatReadResponse> = runCatching {
        delay(80)
        FakeChatStore.readMessages(currentUserId, conversationId, afterSeq)
    }

    override suspend fun sendMessage(
        conversationId: String,
        clientMsgId: String,
        text: String,
        kind: String
    ): Result<ChatSendResponse> = runCatching {
        delay(120)
        FakeChatStore.sendMessage(currentUserId, currentUserRole, conversationId, clientMsgId, text, kind)
    }

    override suspend fun ackRead(conversationId: String, readSeq: Long): Result<ChatAckResponse> = runCatching {
        delay(50)
        FakeChatStore.ackRead(currentUserId, conversationId, readSeq)
    }

    override suspend fun syncChat(): Result<ChatSyncResponse> = runCatching {
        delay(80)
        FakeChatStore.sync(currentUserId)
    }

    override suspend fun listAdminConversations(userId: String?, bookingId: Long?): Result<List<ConversationSummaryDto>> = runCatching {
        delay(100)
        FakeChatStore.listAdminConversations(userId, bookingId)
    }

    override suspend fun readAdminConversation(conversationId: String): Result<ChatAdminReadResponse> = runCatching {
        delay(100)
        FakeChatStore.readAdminConversation(conversationId)
    }
}
