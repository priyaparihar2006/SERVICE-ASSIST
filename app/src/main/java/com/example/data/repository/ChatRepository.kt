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

class ChatApiException(val statusCode: Int, message: String) : Exception(message)

class UnconfiguredChatRepository : ChatRepository {
    private val unconfiguredError = ChatApiException(503, "Chat unavailable — server not configured")
    override suspend fun openConversation(bookingId: Long, bookingCode: String?, serviceName: String?): Result<ChatOpenResponse> = Result.failure(unconfiguredError)
    override suspend fun listConversations(): Result<List<ConversationSummaryDto>> = Result.failure(unconfiguredError)
    override suspend fun readMessages(conversationId: String, afterSeq: Long): Result<ChatReadResponse> = Result.failure(unconfiguredError)
    override suspend fun sendMessage(conversationId: String, clientMsgId: String, text: String, kind: String): Result<ChatSendResponse> = Result.failure(unconfiguredError)
    override suspend fun ackRead(conversationId: String, readSeq: Long): Result<ChatAckResponse> = Result.failure(unconfiguredError)
    override suspend fun syncChat(): Result<ChatSyncResponse> = Result.failure(unconfiguredError)
    override suspend fun listAdminConversations(userId: String?, bookingId: Long?): Result<List<ConversationSummaryDto>> = Result.failure(unconfiguredError)
    override suspend fun readAdminConversation(conversationId: String): Result<ChatAdminReadResponse> = Result.failure(unconfiguredError)
}

class RemoteChatRepository(
    private val apiService: ChatApiService,
    private val bookingDao: com.example.data.db.BookingDao? = null
) : ChatRepository {

    private var currentUserId: String = "user_priya_1"
    private var currentUserRole: String = "CUSTOMER"

    fun setIdentity(userId: String, userRole: String) {
        currentUserId = userId
        currentUserRole = userRole
    }

    private fun extractErrorMessage(raw: String?, code: Int, defaultPrefix: String, endpoint: String = ""): String {
        if (raw.isNullOrBlank()) {
            if (com.example.BuildConfig.DEBUG && endpoint.isNotBlank()) {
                android.util.Log.w("ChatRepo", "$endpoint $code")
            }
            return when (code) {
                403 -> "This chat is closed."
                404 -> "This chat no longer exists."
                409 -> "This conversation can't be decrypted. Please start a new chat."
                else -> "$defaultPrefix ($code)"
            }
        }
        return try {
            val json = org.json.JSONObject(raw)
            val errCode = json.optString("code", "")
            val stage = json.optString("stage", "")
            if (com.example.BuildConfig.DEBUG && endpoint.isNotBlank()) {
                android.util.Log.w("ChatRepo", "$endpoint $code code=$errCode stage=$stage")
            }
            when {
                errCode == "CONTACT_SHARING_RESTRICTED" -> {
                    json.optString("error", "Contact sharing is restricted. Keep chatting in the app.")
                }
                errCode == "CONVERSATION_KEY" -> {
                    json.optString("error", "This conversation can't be decrypted. Please start a new chat.")
                }
                errCode == "MIGRATION_MISSING" -> {
                    json.optString("error", "Chat server is being updated. Please try again shortly.")
                }
                json.has("error") -> json.getString("error")
                json.has("message") -> json.getString("message")
                else -> when (code) {
                    403 -> "This chat is closed."
                    404 -> "This chat no longer exists."
                    409 -> "This conversation can't be decrypted. Please start a new chat."
                    else -> raw
                }
            }
        } catch (_: Exception) {
            when (code) {
                403 -> "This chat is closed."
                404 -> "This chat no longer exists."
                409 -> "This conversation can't be decrypted. Please start a new chat."
                else -> raw
            }
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
                val err = extractErrorMessage(raw, response.code(), "Failed to open conversation", "chat-open")
                throw ChatApiException(response.code(), err)
            }
        } catch (e: java.io.IOException) {
            throw java.io.IOException("No connection — could not open chat", e)
        } catch (e: com.squareup.moshi.JsonDataException) {
            throw ChatApiException(200, "Unexpected server response")
        }
    }

    override suspend fun listConversations(): Result<List<ConversationSummaryDto>> = runCatching {
        try {
            val response = apiService.listConversations()
            if (response.isSuccessful && response.body() != null) {
                return@runCatching response.body()!!.conversations
            } else {
                val raw = response.errorBody()?.string()
                val err = extractErrorMessage(raw, response.code(), "Failed to load chats", "chat-list")
                throw ChatApiException(response.code(), err)
            }
        } catch (e: java.io.IOException) {
            throw java.io.IOException("No connection — could not load chats", e)
        } catch (e: com.squareup.moshi.JsonDataException) {
            throw ChatApiException(200, "Unexpected server response")
        }
    }

    override suspend fun readMessages(conversationId: String, afterSeq: Long): Result<ChatReadResponse> = runCatching {
        try {
            val response = apiService.readMessages(conversationId, afterSeq)
            if (response.isSuccessful && response.body() != null) {
                return@runCatching response.body()!!
            } else {
                val raw = response.errorBody()?.string()
                val err = extractErrorMessage(raw, response.code(), "Failed to load messages", "chat-read")
                throw ChatApiException(response.code(), err)
            }
        } catch (e: java.io.IOException) {
            throw java.io.IOException("No connection — could not load messages", e)
        } catch (e: com.squareup.moshi.JsonDataException) {
            throw ChatApiException(200, "Unexpected server response")
        }
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
            } else {
                val raw = response.errorBody()?.string()
                val err = extractErrorMessage(raw, response.code(), "Failed to send message", "chat-send")
                throw ChatApiException(response.code(), err)
            }
        } catch (e: java.io.IOException) {
            throw java.io.IOException("No connection — message not sent", e)
        } catch (e: com.squareup.moshi.JsonDataException) {
            throw ChatApiException(200, "Unexpected server response")
        }
    }

    override suspend fun ackRead(conversationId: String, readSeq: Long): Result<ChatAckResponse> = runCatching {
        try {
            val response = apiService.ackRead(ChatAckRequest(conversationId, readSeq))
            if (response.isSuccessful && response.body() != null) {
                return@runCatching response.body()!!
            } else {
                val raw = response.errorBody()?.string()
                val err = extractErrorMessage(raw, response.code(), "Failed to ack receipt", "chat-ack")
                throw ChatApiException(response.code(), err)
            }
        } catch (e: java.io.IOException) {
            throw java.io.IOException("No connection — could not ack receipt", e)
        } catch (e: com.squareup.moshi.JsonDataException) {
            throw ChatApiException(200, "Unexpected server response")
        }
    }

    override suspend fun syncChat(): Result<ChatSyncResponse> = runCatching {
        try {
            val response = apiService.syncChat()
            if (response.isSuccessful && response.body() != null) {
                return@runCatching response.body()!!
            } else {
                val raw = response.errorBody()?.string()
                val err = extractErrorMessage(raw, response.code(), "Failed to sync chat", "chat-sync")
                throw ChatApiException(response.code(), err)
            }
        } catch (e: java.io.IOException) {
            throw java.io.IOException("No connection — could not sync chat", e)
        } catch (e: com.squareup.moshi.JsonDataException) {
            throw ChatApiException(200, "Unexpected server response")
        }
    }

    override suspend fun listAdminConversations(userId: String?, bookingId: Long?): Result<List<ConversationSummaryDto>> = runCatching {
        try {
            val response = apiService.listAdminConversations(userId, bookingId)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.conversations
            } else {
                val raw = response.errorBody()?.string()
                val err = extractErrorMessage(raw, response.code(), "Failed to list admin conversations", "chat-admin-list")
                throw ChatApiException(response.code(), err)
            }
        } catch (e: java.io.IOException) {
            throw java.io.IOException("No connection — could not list admin conversations", e)
        }
    }

    override suspend fun readAdminConversation(conversationId: String): Result<ChatAdminReadResponse> = runCatching {
        try {
            val response = apiService.readAdminConversation(conversationId)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                val raw = response.errorBody()?.string()
                val err = extractErrorMessage(raw, response.code(), "Failed to read admin conversation", "chat-admin-read")
                throw ChatApiException(response.code(), err)
            }
        } catch (e: java.io.IOException) {
            throw java.io.IOException("No connection — could not read admin conversation", e)
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
    fun syncFromBookings(bookings: List<com.example.data.model.Booking>, currentUserId: String, currentUserRole: String) {
        if (convMap.isEmpty()) {
            resetToDefault()
        }
        bookings.forEach { b ->
            val custId = if (b.customerId.isNotBlank()) b.customerId else "user_priya_1"
            val custName = if (b.customerName.isNotBlank()) b.customerName else "Priya Sharma"
            val proId = if (b.professionalId.isNotBlank()) b.professionalId else "pro_rajesh_1"
            val proName = when (proId) {
                "pro_amit_2" -> "Amit Kumar"
                "pro_meera_3" -> "Meera Saxena"
                "pro_dinesh_4" -> "Dinesh Verma"
                "pro_rajesh_1", "user_rajesh_pro" -> "Rajesh Sharma"
                else -> if (proId.startsWith("pro_")) {
                    proId.removePrefix("pro_").replace("_", " ").split(" ")
                        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
                } else "Rajesh Sharma"
            }

            val isClosedBooking = b.status == com.example.data.model.BookingStatus.COMPLETED || b.status == com.example.data.model.BookingStatus.CANCELLED
            val targetStatus = if (isClosedBooking) "READ_ONLY" else "ACTIVE"

            val msgTimestamp = if (b.createdAt > 0L) {
                Instant.ofEpochMilli(b.createdAt).toString()
            } else {
                Instant.now().toString()
            }

            // Look up existing conversation for this (customer, partner) pair
            val existing = convMap.values.find {
                (it.customerId == custId && it.partnerId == proId) ||
                (it.id == "conv_ac_rajesh_1" && custId == "user_priya_1" && (proId == "pro_rajesh_1" || proId == "user_rajesh_pro")) ||
                (it.id == "conv_cleaning_amit_2" && custId == "user_priya_1" && proId == "pro_amit_2")
            }

            if (existing != null) {
                // If this is a new or different booking with the same partner
                if (existing.bookingId != b.id) {
                    val scheduleInfo = if (b.scheduledDate.isNotBlank()) {
                        " for ${b.scheduledDate}${if (b.scheduledTime.isNotBlank()) " at ${b.scheduledTime}" else ""}"
                    } else ""
                    val bookingUpdateText = "New booking accepted: ${b.serviceName} (ID: #${b.bookingCode})$scheduleInfo"

                    val custFirst = custName.split(" ").firstOrNull() ?: "there"
                    val pkgText = if (b.packageName.isNotBlank()) " (${b.packageName})" else ""
                    val timeText = if (b.scheduledTime.isNotBlank()) " at ${b.scheduledTime}" else ""
                    val locText = if (b.locality.isNotBlank()) " in ${b.locality}" else ""
                    val introMessageText = "Hello $custFirst! I'm $proName, your assigned professional for ${b.serviceName}$pkgText. Your booking #${b.bookingCode} is confirmed for ${b.scheduledDate}$timeText$locText. I'll arrive on time with all necessary equipment. Feel free to message me here if you have any questions or instructions!"

                    val newSeq1 = existing.lastMessageSeq + 1L
                    val newSeq2 = existing.lastMessageSeq + 2L
                    val messages = messagesMap.getOrPut(existing.id) { mutableListOf() }
                    messages.add(
                        StoredMessage(
                            id = "msg_update_${b.id}_${newSeq1}",
                            conversationId = existing.id,
                            seq = newSeq1,
                            senderId = "SYSTEM",
                            senderRole = "SYSTEM",
                            kind = "BOOKING_UPDATE",
                            text = bookingUpdateText,
                            createdAt = msgTimestamp
                        )
                    )
                    messages.add(
                        StoredMessage(
                            id = "msg_intro_${b.id}_${newSeq2}",
                            conversationId = existing.id,
                            seq = newSeq2,
                            senderId = proId,
                            senderRole = "PARTNER",
                            kind = "TEXT",
                            text = introMessageText,
                            createdAt = msgTimestamp
                        )
                    )

                    convMap[existing.id] = existing.copy(
                        bookingId = b.id,
                        bookingCode = b.bookingCode,
                        serviceName = b.serviceName,
                        status = targetStatus,
                        lastMessageAt = msgTimestamp,
                        lastMessageSeq = newSeq2
                    )

                    val myCursor = cursorsMap.getOrPut(existing.id to currentUserId) { UserCursor() }
                    myCursor.lastDeliveredSeq = maxOf(myCursor.lastDeliveredSeq, newSeq2)
                } else if (existing.status != targetStatus) {
                    convMap[existing.id] = existing.copy(status = targetStatus)
                }
            } else {
                val convId = "conv_${custId}_${proId}"
                val custFirst = custName.split(" ").firstOrNull() ?: "there"
                val pkgText = if (b.packageName.isNotBlank()) " (${b.packageName})" else ""
                val timeText = if (b.scheduledTime.isNotBlank()) " at ${b.scheduledTime}" else ""
                val locText = if (b.locality.isNotBlank()) " in ${b.locality}" else ""
                val scheduleInfo = if (b.scheduledDate.isNotBlank()) " for ${b.scheduledDate}$timeText$locText" else ""

                val bookingNoticeText = "Service booking accepted: ${b.serviceName}$pkgText (ID: #${b.bookingCode})$scheduleInfo"
                val introMessageText = "Hello $custFirst! I'm $proName, your assigned professional for ${b.serviceName}$pkgText. Your booking #${b.bookingCode} is confirmed for ${b.scheduledDate}$timeText$locText. I'll arrive on time with all necessary equipment. Feel free to message me here if you have any questions or instructions!"

                convMap[convId] = StoredConv(
                    id = convId,
                    bookingId = b.id,
                    bookingCode = b.bookingCode,
                    serviceName = b.serviceName,
                    customerId = custId,
                    customerName = custName,
                    partnerId = proId,
                    partnerName = proName,
                    status = targetStatus,
                    openedBy = custId,
                    lastMessageAt = msgTimestamp,
                    lastMessageSeq = 2L
                )
                messagesMap[convId] = mutableListOf(
                    StoredMessage(
                        id = "msg_${b.id}_sys",
                        conversationId = convId,
                        seq = 1L,
                        senderId = "SYSTEM",
                        senderRole = "SYSTEM",
                        kind = "BOOKING_UPDATE",
                        text = bookingNoticeText,
                        createdAt = msgTimestamp
                    ),
                    StoredMessage(
                        id = "msg_${b.id}_intro",
                        conversationId = convId,
                        seq = 2L,
                        senderId = proId,
                        senderRole = "PARTNER",
                        kind = "TEXT",
                        text = introMessageText,
                        createdAt = msgTimestamp
                    )
                )
                cursorsMap[convId to proId] = UserCursor(lastReadSeq = 2L, lastDeliveredSeq = 2L)
                cursorsMap[convId to custId] = UserCursor(lastReadSeq = 0L, lastDeliveredSeq = 2L)
            }
        }
    }

    @Synchronized
    fun getConversationsForUser(currentUserId: String, currentUserRole: String): List<ConversationSummaryDto> {
        if (convMap.isEmpty()) {
            resetToDefault()
        }

        val isCustomerRole = currentUserRole.equals("CUSTOMER", ignoreCase = true) || (!currentUserRole.equals("PROFESSIONAL", ignoreCase = true) && !currentUserRole.equals("PARTNER", ignoreCase = true) && !currentUserRole.equals("ADMIN", ignoreCase = true))

        return convMap.values.filter { conv ->
            val isParticipant = conv.customerId == currentUserId ||
                conv.partnerId == currentUserId ||
                currentUserRole.equals("ADMIN", ignoreCase = true)
            if (!isParticipant) return@filter false
            true
        }.map { conv ->
            val isCustomer = if (conv.customerId == currentUserId) true else if (conv.partnerId == currentUserId) false else isCustomerRole
            val counterpartDisplayName = if (isCustomer) {
                "${conv.partnerName.split(" ").firstOrNull() ?: "Pro"} · Your Professional"
            } else {
                "${conv.customerName.split(" ").firstOrNull() ?: "Client"} · Client"
            }

            val peerId = if (isCustomer) conv.partnerId else conv.customerId
            val myCursor = cursorsMap.getOrPut(conv.id to currentUserId) {
                if (conv.id == "conv_ac_rajesh_1" && isCustomer) {
                    UserCursor(lastReadSeq = 3L, lastDeliveredSeq = 4L)
                } else {
                    UserCursor(lastReadSeq = conv.lastMessageSeq, lastDeliveredSeq = conv.lastMessageSeq)
                }
            }
            val peerCursor = cursorsMap.getOrPut(conv.id to peerId) { UserCursor(lastReadSeq = conv.lastMessageSeq, lastDeliveredSeq = conv.lastMessageSeq) }

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
        if (convMap.isEmpty()) {
            resetToDefault()
        }

        val isCustomer = !currentUserId.startsWith("pro_")
        val convs = convMap.values.filter { conv ->
            conv.customerId == currentUserId || conv.partnerId == currentUserId ||
            (isCustomer && (conv.customerId == "user_priya_1" || conv.customerId.startsWith("user_") || !conv.customerId.startsWith("pro_"))) ||
            (!isCustomer && (conv.partnerId == "pro_rajesh_1" || conv.partnerId.startsWith("pro_")))
        }

        val syncList = convs.map { conv ->
            val isCust = conv.customerId == currentUserId || isCustomer
            val peerId = if (isCust) conv.partnerId else conv.customerId
            val myCursor = cursorsMap.getOrPut(conv.id to currentUserId) {
                if (conv.id == "conv_ac_rajesh_1" && isCust) {
                    UserCursor(lastReadSeq = 3L, lastDeliveredSeq = 4L)
                } else {
                    UserCursor(lastReadSeq = conv.lastMessageSeq, lastDeliveredSeq = conv.lastMessageSeq)
                }
            }
            val peerCursor = cursorsMap.getOrPut(conv.id to peerId) { UserCursor(lastReadSeq = conv.lastMessageSeq, lastDeliveredSeq = conv.lastMessageSeq) }

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
        val isCustomer = conv.customerId == currentUserId || !currentUserId.startsWith("pro_")
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
                isMine = msg.senderId == currentUserId || (isCustomer && msg.senderRole == "CUSTOMER") || (!isCustomer && msg.senderRole == "PARTNER")
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
        val custId = customerId ?: if (currentUserId.startsWith("user_")) currentUserId else "user_priya_1"
        val custName = customerName ?: "Priya Sharma"
        val proId = partnerId ?: if (currentUserId.startsWith("pro_") || currentUserId == "user_rajesh_pro") currentUserId else "pro_rajesh_1"
        val proName = partnerName ?: when (proId) {
            "pro_amit_2" -> "Amit Kumar"
            "pro_meera_3" -> "Meera Saxena"
            "pro_dinesh_4" -> "Dinesh Verma"
            "pro_rajesh_1", "user_rajesh_pro" -> "Rajesh Sharma"
            else -> if (proId.startsWith("pro_")) {
                proId.removePrefix("pro_").replace("_", " ").split(" ")
                    .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
            } else "Rajesh Sharma"
        }
        val code = bookingCode ?: "SRV-${10000 + bookingId}"
        val srv = serviceName ?: "Doorstep Service"
        val nowIso = Instant.now().toString()

        val existing = convMap.values.find {
            (it.customerId == custId && it.partnerId == proId) ||
            (it.id == "conv_ac_rajesh_1" && custId == "user_priya_1" && (proId == "pro_rajesh_1" || proId == "user_rajesh_pro")) ||
            (it.id == "conv_cleaning_amit_2" && custId == "user_priya_1" && proId == "pro_amit_2") ||
            it.bookingId == bookingId ||
            (bookingCode != null && it.bookingCode == bookingCode)
        }

        if (existing != null) {
            val isCustomer = existing.customerId == currentUserId || currentUserId.startsWith("user_")
            val counterpartDisplayName = if (isCustomer) {
                "${existing.partnerName.split(" ").firstOrNull() ?: "Pro"} · Your Professional"
            } else {
                "${existing.customerName.split(" ").firstOrNull() ?: "Client"} · Client"
            }

            if (existing.bookingId != bookingId) {
                val newSeq1 = existing.lastMessageSeq + 1L
                val newSeq2 = existing.lastMessageSeq + 2L
                val bookingUpdateText = "New booking accepted: $srv (ID: #$code)"
                val custFirst = custName.split(" ").firstOrNull() ?: "there"
                val introMessageText = "Hello $custFirst! I'm $proName, your assigned professional for $srv. Your booking #$code is confirmed. I will arrive on time with all necessary equipment."

                val messages = messagesMap.getOrPut(existing.id) { mutableListOf() }
                messages.add(
                    StoredMessage(
                        id = "msg_update_${bookingId}_${newSeq1}",
                        conversationId = existing.id,
                        seq = newSeq1,
                        senderId = "SYSTEM",
                        senderRole = "SYSTEM",
                        kind = "BOOKING_UPDATE",
                        text = bookingUpdateText,
                        createdAt = nowIso
                    )
                )
                messages.add(
                    StoredMessage(
                        id = "msg_intro_${bookingId}_${newSeq2}",
                        conversationId = existing.id,
                        seq = newSeq2,
                        senderId = proId,
                        senderRole = "PARTNER",
                        kind = "TEXT",
                        text = introMessageText,
                        createdAt = nowIso
                    )
                )

                convMap[existing.id] = existing.copy(
                    bookingId = bookingId,
                    bookingCode = code,
                    serviceName = srv,
                    status = "ACTIVE",
                    lastMessageAt = nowIso,
                    lastMessageSeq = newSeq2
                )

                val myCursor = cursorsMap.getOrPut(existing.id to currentUserId) { UserCursor() }
                myCursor.lastDeliveredSeq = maxOf(myCursor.lastDeliveredSeq, newSeq2)
            } else if (existing.status != "ACTIVE") {
                convMap[existing.id] = existing.copy(status = "ACTIVE")
            }

            val updated = convMap[existing.id] ?: existing
            return ChatOpenResponse(
                conversationId = updated.id,
                bookingId = updated.bookingId,
                bookingCode = updated.bookingCode,
                serviceName = updated.serviceName,
                status = updated.status,
                counterpartDisplayName = counterpartDisplayName
            )
        }

        val newId = "conv_${custId}_${proId}"
        val custFirst = custName.split(" ").firstOrNull() ?: "there"
        val bookingNoticeText = "Service booking accepted: $srv (ID: #$code)"
        val introMessageText = "Hello $custFirst! I'm $proName, your assigned professional for $srv. Your booking #$code is confirmed. I will arrive on time with all necessary equipment."

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
            lastMessageAt = nowIso,
            lastMessageSeq = 2L
        )
        convMap[newId] = newConv
        messagesMap[newId] = mutableListOf(
            StoredMessage(
                id = "msg_${bookingId}_sys",
                conversationId = newId,
                seq = 1L,
                senderId = "SYSTEM",
                senderRole = "SYSTEM",
                kind = "BOOKING_UPDATE",
                text = bookingNoticeText,
                createdAt = nowIso
            ),
            StoredMessage(
                id = "msg_${bookingId}_intro",
                conversationId = newId,
                seq = 2L,
                senderId = proId,
                senderRole = "PARTNER",
                kind = "TEXT",
                text = introMessageText,
                createdAt = nowIso
            )
        )
        cursorsMap[newId to proId] = UserCursor(lastReadSeq = 2L, lastDeliveredSeq = 2L)
        cursorsMap[newId to custId] = UserCursor(lastReadSeq = 0L, lastDeliveredSeq = 2L)

        val isCustomer = custId == currentUserId || currentUserId.startsWith("user_")
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
