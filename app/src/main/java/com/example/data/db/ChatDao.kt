package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_conversations WHERE ownerProfileId = :ownerProfileId ORDER BY localUpdatedAt DESC")
    fun observeConversations(ownerProfileId: String): Flow<List<ChatConversationEntity>>

    @Query("SELECT * FROM chat_conversations WHERE ownerProfileId = :ownerProfileId ORDER BY localUpdatedAt DESC")
    suspend fun getConversations(ownerProfileId: String): List<ChatConversationEntity>

    @Query("SELECT * FROM chat_conversations WHERE bookingId = :bookingId LIMIT 1")
    fun observeConversationByBooking(bookingId: Long): Flow<ChatConversationEntity?>

    @Query("SELECT * FROM chat_conversations WHERE bookingId = :bookingId LIMIT 1")
    suspend fun getConversationByBooking(bookingId: Long): ChatConversationEntity?

    @Query("SELECT * FROM chat_conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversation(conversationId: String): ChatConversationEntity?

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY seq ASC, createdAt ASC")
    fun observeMessages(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY seq ASC, createdAt ASC")
    suspend fun getMessages(conversationId: String): List<ChatMessageEntity>

    @Query("SELECT COALESCE(MAX(seq), 0) FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun maxSeq(conversationId: String): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversations(convs: List<ChatConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conv: ChatConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<ChatMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET status = :status, failureReason = :failureReason WHERE id = :id OR clientMsgId = :id")
    suspend fun updateMessageStatus(id: String, status: String, failureReason: String? = null)

    @Query("UPDATE chat_messages SET id = :serverMsgId, seq = :seq, status = :status, failureReason = NULL WHERE id = :clientMsgId OR clientMsgId = :clientMsgId")
    suspend fun updateMessageSent(clientMsgId: String, serverMsgId: String, seq: Long, status: String)

    @Query("SELECT * FROM chat_messages WHERE status = 'SENDING' ORDER BY createdAt ASC")
    suspend fun getPendingOutboxMessages(): List<ChatMessageEntity>

    @Query("DELETE FROM chat_conversations WHERE ownerProfileId = :ownerProfileId")
    suspend fun clearConversationsForOwner(ownerProfileId: String)

    @Query("DELETE FROM chat_messages WHERE conversationId IN (SELECT id FROM chat_conversations WHERE ownerProfileId = :ownerProfileId)")
    suspend fun clearMessagesForOwner(ownerProfileId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Transaction
    suspend fun clearForOwner(ownerProfileId: String) {
        clearMessagesForOwner(ownerProfileId)
        clearConversationsForOwner(ownerProfileId)
    }
}
