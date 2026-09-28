package com.example.data.remote.supabase

import com.example.data.remote.chat.ChatApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SupabaseClient {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Volatile
    var userAccessToken: String? = null

    @Volatile
    var devProfileId: String? = null

    @Volatile
    var devUserRole: String? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = userAccessToken?.takeIf { it.isNotBlank() } ?: SupabaseConfig.anonKey
        val requestBuilder = original.newBuilder()
            .header("apikey", SupabaseConfig.anonKey)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")

        if (com.example.BuildConfig.DEBUG && !devProfileId.isNullOrBlank()) {
            requestBuilder.header("X-Dev-Profile-Id", devProfileId!!)
            if (!devUserRole.isNullOrBlank()) {
                requestBuilder.header("X-Dev-User-Role", devUserRole!!)
            }
        }

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        // Ensure sensitive tokens, phone numbers, and chat message bodies are never printed to logcat
        if (!message.contains("authorization", ignoreCase = true) &&
            !message.contains("apikey", ignoreCase = true) &&
            !message.contains("ciphertext", ignoreCase = true) &&
            !message.contains("content", ignoreCase = true)
        ) {
            android.util.Log.d("SupabaseClient", message)
        }
    }.apply {
        level = if (com.example.BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.HEADERS // Never BODY for chat endpoints or release
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    // Shared connection pool across both write and poll clients
    private val sharedConnectionPool = ConnectionPool(10, 5, TimeUnit.MINUTES)

    // Dedicated client for user-initiated writes (chat-send, chat-open, booking create, status updates)
    val writeClient: OkHttpClient by lazy {
        val writeDispatcher = Dispatcher().apply {
            maxRequests = 30
            maxRequestsPerHost = 10
        }
        OkHttpClient.Builder()
            .dispatcher(writeDispatcher)
            .connectionPool(sharedConnectionPool)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // Dedicated client for background polling and reading (sync, list, read, polls)
    val pollClient: OkHttpClient by lazy {
        val pollDispatcher = Dispatcher().apply {
            maxRequests = 30
            maxRequestsPerHost = 10
        }
        OkHttpClient.Builder()
            .dispatcher(pollDispatcher)
            .connectionPool(sharedConnectionPool)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun createRetrofit(client: OkHttpClient): Retrofit? {
        if (!SupabaseConfig.isConfigured) return null
        return try {
            val baseUrl = if (SupabaseConfig.projectUrl.endsWith("/")) {
                SupabaseConfig.projectUrl
            } else {
                "${SupabaseConfig.projectUrl}/"
            }
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        } catch (e: Exception) {
            null
        }
    }

    private val writeRetrofit: Retrofit? by lazy { createRetrofit(writeClient) }
    private val pollRetrofit: Retrofit? by lazy { createRetrofit(pollClient) }

    val apiService: SupabaseApiService? by lazy {
        writeRetrofit?.create(SupabaseApiService::class.java)
    }

    val chatApiService: ChatApiService? by lazy {
        writeRetrofit?.create(ChatApiService::class.java)
    }

    val paymentApiService: com.example.data.remote.payment.PaymentApiService? by lazy {
        writeRetrofit?.create(com.example.data.remote.payment.PaymentApiService::class.java)
    }

    val bookingApiService: com.example.data.remote.booking.BookingApiService? by lazy {
        writeRetrofit?.create(com.example.data.remote.booking.BookingApiService::class.java)
    }
}
