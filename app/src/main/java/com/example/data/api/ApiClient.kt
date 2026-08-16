package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

import okhttp3.MediaType.Companion.toMediaType

object ApiClient {
    private const val BASE_URL = "https://api.infinitystudio.ai/v1/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor { chain ->
            // Mock interceptor to provide demo data since there is no real backend deployed yet
            // This prevents crashes and lets the user test the UI locally.
            // Normally we would just chain.proceed(chain.request())
            val request = chain.request()
            val path = request.url.encodedPath
            
            val responseString = when {
                path.contains("/models") -> """
                    [
                        {"id": "gpt-4", "provider": "OpenAI", "name": "GPT-4o", "capabilities": ["Vision", "Reasoning", "Coding"], "contextWindow": 128000, "speed": "Fast", "isFavorite": true},
                        {"id": "claude-3-opus", "provider": "Anthropic", "name": "Claude 3 Opus", "capabilities": ["Reasoning", "Coding", "Writing"], "contextWindow": 200000, "speed": "Medium", "isFavorite": false},
                        {"id": "gemini-1.5-pro", "provider": "Google", "name": "Gemini 1.5 Pro", "capabilities": ["Vision", "Reasoning", "Video"], "contextWindow": 2000000, "speed": "Fast", "isFavorite": true}
                    ]
                """.trimIndent()
                path.contains("/chats") -> """
                    [
                        {"id": "1", "title": "Android Architecture Discussion", "updatedAt": 1716382000000, "isPinned": true, "folder": "Work"},
                        {"id": "2", "title": "Explain Quantum Physics", "updatedAt": 1716380000000, "isPinned": false, "folder": null}
                    ]
                """.trimIndent()
                path.contains("/user/profile") -> """
                    {
                        "id": "u123", "name": "Alex Developer", "email": "alex@example.com", "plan": "Pro",
                        "creditsRemaining": 8500, "creditsUsed": 1500, "requests": 342, "favoriteModel": "GPT-4o"
                    }
                """.trimIndent()
                path.contains("/auth/login") || path.contains("/auth/signup") -> """
                    {"token": "mock_jwt_token", "user": {"id": "u123", "name": "Alex Developer", "email": "alex@example.com", "plan": "Pro", "creditsRemaining": 8500, "creditsUsed": 1500, "requests": 342, "favoriteModel": "GPT-4o"}}
                """.trimIndent()
                path.contains("/chat/message") && !path.contains("feedback") -> """
                    {"id": "m123", "text": "This is a simulated response from the backend. The integration is ready for the real Node.js Express server.", "timestamp": 1716382500000}
                """.trimIndent()
                path.contains("/feedback") -> "{}"
                path.contains("/agents") && request.method == "GET" -> """
                    [
                        {"id": "a1", "name": "Code Reviewer", "persona": "Strict senior developer focusing on security.", "instructions": "Review the provided code for security vulnerabilities and suggest robust fixes.", "hasKnowledgeFiles": true},
                        {"id": "a2", "name": "Creative Writer", "persona": "Imaginative and engaging copywriter.", "instructions": "Draft compelling narratives or marketing copy based on brief prompts.", "hasKnowledgeFiles": false}
                    ]
                """.trimIndent()
                path.contains("/agents") && request.method == "POST" -> """
                    {"id": "a3", "name": "Custom Agent", "persona": "Helpful AI", "instructions": "Follow instructions", "hasKnowledgeFiles": false}
                """.trimIndent()
                else -> "{}"
            }
            
            okhttp3.Response.Builder()
                .code(200)
                .message("OK")
                .request(request)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .body(okhttp3.ResponseBody.create("application/json".toMediaType(), responseString))
                .build()
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: InfinityApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(InfinityApiService::class.java)
    }
}
