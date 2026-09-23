package com.example.data.remote.supabase

import com.example.data.remote.chat.ChatApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit? by lazy {
        if (!SupabaseConfig.isConfigured) {
            null
        } else {
            try {
                val baseUrl = if (SupabaseConfig.projectUrl.endsWith("/")) {
                    SupabaseConfig.projectUrl
                } else {
                    "${SupabaseConfig.projectUrl}/"
                }

                Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
            } catch (e: Exception) {
                null
            }
        }
    }

    val apiService: SupabaseApiService? by lazy {
        retrofit?.create(SupabaseApiService::class.java)
    }

    val chatApiService: ChatApiService? by lazy {
        retrofit?.create(ChatApiService::class.java)
    }

    val paymentApiService: com.example.data.remote.payment.PaymentApiService? by lazy {
        retrofit?.create(com.example.data.remote.payment.PaymentApiService::class.java)
    }

    val bookingApiService: com.example.data.remote.booking.BookingApiService? by lazy {
        retrofit?.create(com.example.data.remote.booking.BookingApiService::class.java)
    }
}
