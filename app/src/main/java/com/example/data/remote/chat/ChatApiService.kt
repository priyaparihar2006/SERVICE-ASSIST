package com.example.data.remote.chat

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ChatApiService {

    @POST("functions/v1/chat-open")
    suspend fun openConversation(
        @Body request: ChatOpenRequest
    ): Response<ChatOpenResponse>

    @GET("functions/v1/chat-list")
    suspend fun listConversations(): Response<ChatListResponse>

    @GET("functions/v1/chat-read")
    suspend fun readMessages(
        @Query("conversation_id") conversationId: String,
        @Query("after_seq") afterSeq: Long = 0
    ): Response<ChatReadResponse>

    @POST("functions/v1/chat-send")
    suspend fun sendMessage(
        @Body request: ChatSendRequest
    ): Response<ChatSendResponse>

    @POST("functions/v1/chat-ack")
    suspend fun ackRead(
        @Body request: ChatAckRequest
    ): Response<ChatAckResponse>

    @GET("functions/v1/chat-sync")
    suspend fun syncChat(): Response<ChatSyncResponse>

    @GET("functions/v1/chat-admin-list")
    suspend fun listAdminConversations(
        @Query("user_id") userId: String? = null,
        @Query("booking_id") bookingId: Long? = null
    ): Response<ChatListResponse>

    @GET("functions/v1/chat-admin-read")
    suspend fun readAdminConversation(
        @Query("conversation_id") conversationId: String
    ): Response<ChatAdminReadResponse>
}
