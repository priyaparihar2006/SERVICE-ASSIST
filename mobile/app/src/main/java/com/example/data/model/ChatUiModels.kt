package com.example.data.model

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

data class ChatMessageUi(
    val id: String,
    val seq: Long?,
    val senderId: String,
    val senderRole: String,
    val text: String,
    val kind: String = "TEXT",
    val createdAtMillis: Long,
    val isMine: Boolean,
    val status: MessageStatus = MessageStatus.SENT
)

data class ConversationUi(
    val id: String,
    val bookingId: Long,
    val bookingCode: String,
    val serviceName: String,
    val counterpartName: String,
    val lastMessagePreview: String,
    val lastMessageAtMillis: Long?,
    val lastMessageFromMe: Boolean,
    val unreadCount: Int,
    val status: String,
    val peerReadSeq: Long = 0L,
    val peerDeliveredSeq: Long = 0L,
    val latestSeq: Long = 0L
)
