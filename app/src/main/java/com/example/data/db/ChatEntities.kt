package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_conversations",
    indices = [
        Index("ownerProfileId"),
        Index("bookingId")
    ]
)
data class ChatConversationEntity(
    @PrimaryKey
    val id: String,
    val ownerProfileId: String,
    val bookingId: Long,
    val bookingCode: String,
    val serviceName: String,
    val counterpartDisplayName: String,
    val lastMessagePreview: String,
    val lastMessageAt: String?,
    val lastMessageFromMe: Boolean,
    val unreadCount: Int,
    val status: String,
    val peerReadSeq: Long,
    val peerDeliveredSeq: Long,
    val latestSeq: Long,
    val localUpdatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    indices = [
        Index("conversationId"),
        Index("seq"),
        Index("status")
    ]
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val seq: Long,
    val senderId: String,
    val senderRole: String,
    val isFromMe: Boolean,
    val kind: String,
    val text: String,
    val status: String, // SENDING, SENT, DELIVERED, READ, FAILED
    val failureReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val clientMsgId: String? = null
)
