package com.example.data.remote.chat

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatOpenRequest(
    @Json(name = "booking_id") val bookingId: Long,
    @Json(name = "booking_code") val bookingCode: String? = null,
    @Json(name = "service_name") val serviceName: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatOpenResponse(
    @Json(name = "conversation_id") val conversationId: String,
    @Json(name = "booking_id") val bookingId: Long,
    @Json(name = "booking_code") val bookingCode: String?,
    @Json(name = "service_name") val serviceName: String?,
    @Json(name = "status") val status: String,
    @Json(name = "counterpart_display_name") val counterpartDisplayName: String
)

@JsonClass(generateAdapter = true)
data class ConversationSummaryDto(
    @Json(name = "conversation_id") val conversationId: String,
    @Json(name = "booking_id") val bookingId: Long,
    @Json(name = "booking_code") val bookingCode: String,
    @Json(name = "service_name") val serviceName: String,
    @Json(name = "counterpart_display_name") val counterpartDisplayName: String,
    @Json(name = "last_message_preview") val lastMessagePreview: String,
    @Json(name = "last_message_at") val lastMessageAt: String?,
    @Json(name = "last_message_from_me") val lastMessageFromMe: Boolean = false,
    @Json(name = "unread_count") val unreadCount: Int,
    @Json(name = "status") val status: String,
    @Json(name = "peer_read_seq") val peerReadSeq: Long = 0L,
    @Json(name = "peer_delivered_seq") val peerDeliveredSeq: Long = 0L,
    @Json(name = "latest_seq") val latestSeq: Long = 0L
)

@JsonClass(generateAdapter = true)
data class ChatListResponse(
    @Json(name = "conversations") val conversations: List<ConversationSummaryDto>
)

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    @Json(name = "id") val id: String,
    @Json(name = "seq") val seq: Long,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "sender_role") val senderRole: String,
    @Json(name = "kind") val kind: String, // TEXT, QUICK_REPLY, ETA, SYSTEM
    @Json(name = "text") val text: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "is_mine") val isMine: Boolean
)

@JsonClass(generateAdapter = true)
data class ChatReadResponse(
    @Json(name = "conversation_id") val conversationId: String,
    @Json(name = "status") val status: String,
    @Json(name = "messages") val messages: List<ChatMessageDto>,
    @Json(name = "last_seq") val lastSeq: Long,
    @Json(name = "peer_read_seq") val peerReadSeq: Long = 0L,
    @Json(name = "peer_delivered_seq") val peerDeliveredSeq: Long = 0L
)

@JsonClass(generateAdapter = true)
data class ChatSendRequest(
    @Json(name = "conversation_id") val conversationId: String,
    @Json(name = "client_message_id") val clientMessageId: String,
    @Json(name = "text") val text: String,
    @Json(name = "kind") val kind: String = "TEXT"
)

@JsonClass(generateAdapter = true)
data class ChatSendResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message_id") val messageId: String,
    @Json(name = "seq") val seq: Long,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class ChatAckRequest(
    @Json(name = "conversation_id") val conversationId: String,
    @Json(name = "read_seq") val readSeq: Long
)

@JsonClass(generateAdapter = true)
data class ChatAckResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "last_read_seq") val lastReadSeq: Long
)

@JsonClass(generateAdapter = true)
data class ChatSyncConversationDto(
    @Json(name = "conversation_id") val conversationId: String,
    @Json(name = "booking_id") val bookingId: Long,
    @Json(name = "status") val status: String,
    @Json(name = "last_message_at") val lastMessageAt: String?,
    @Json(name = "latest_seq") val latestSeq: Long,
    @Json(name = "unread_count") val unreadCount: Int,
    @Json(name = "peer_read_seq") val peerReadSeq: Long,
    @Json(name = "peer_delivered_seq") val peerDeliveredSeq: Long
)

@JsonClass(generateAdapter = true)
data class ChatSyncResponse(
    @Json(name = "conversations") val conversations: List<ChatSyncConversationDto>,
    @Json(name = "total_unread") val totalUnread: Int,
    @Json(name = "server_time") val serverTime: String
)

@JsonClass(generateAdapter = true)
data class ChatAdminReadResponse(
    @Json(name = "conversation_id") val conversationId: String,
    @Json(name = "booking_id") val bookingId: Long,
    @Json(name = "booking_code") val bookingCode: String?,
    @Json(name = "service_name") val serviceName: String?,
    @Json(name = "customer_name") val customerName: String?,
    @Json(name = "status") val status: String,
    @Json(name = "is_admin_view") val isAdminView: Boolean,
    @Json(name = "audit_notice") val auditNotice: String,
    @Json(name = "messages") val messages: List<ChatMessageDto>,
    @Json(name = "customer_read_seq") val customerReadSeq: Long = 0L,
    @Json(name = "customer_delivered_seq") val customerDeliveredSeq: Long = 0L,
    @Json(name = "partner_read_seq") val partnerReadSeq: Long = 0L,
    @Json(name = "partner_delivered_seq") val partnerDeliveredSeq: Long = 0L
)
