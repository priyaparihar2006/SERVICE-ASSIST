package com.example.data.remote.supabase

import com.example.BuildConfig

object SupabaseConfig {
    /**
     * Resolves the Supabase URL from BuildConfig (injected via Secrets Gradle plugin from .env / .env.example)
     */
    val projectUrl: String
        get() = try {
            BuildConfig.SUPABASE_URL.trim().removeSuffix("/")
        } catch (e: Throwable) {
            "https://biaequumqtdtugjqqkqs.supabase.co"
        }

    /**
     * Resolves the Supabase Anon Key from BuildConfig
     */
    val anonKey: String
        get() = try {
            BuildConfig.SUPABASE_ANON_KEY.trim()
        } catch (e: Throwable) {
            ""
        }

    /**
     * Returns true if the user has supplied a valid custom Supabase URL and Anon key.
     */
    val isConfigured: Boolean
        get() {
            val url = projectUrl
            val key = anonKey
            return url.isNotBlank() &&
                    url.startsWith("http") &&
                    !url.contains("your-project") &&
                    key.isNotBlank() &&
                    !key.contains("your-supabase-anon-key")
        }

    /**
     * Masked display of the URL for safe UI presentation
     */
    val maskedUrl: String
        get() {
            val url = projectUrl
            return if (url.contains("://")) {
                val domain = url.substringAfter("://")
                if (domain.length > 20) domain.take(12) + "..." + domain.takeLast(10) else domain
            } else {
                url
            }
        }
}
