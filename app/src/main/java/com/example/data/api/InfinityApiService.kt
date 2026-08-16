package com.example.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

import com.squareup.moshi.JsonClass

interface InfinityApiService {
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("/api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): AuthResponse

    @GET("/api/models")
    suspend fun getModels(): List<AiModel>

    @GET("/api/chats")
    suspend fun getRecentChats(): List<ChatSummary>

    @POST("/api/chat/message")
    suspend fun sendMessage(@Body request: ChatMessageRequest): ChatMessageResponse
    
    @POST("/api/chat/message/{messageId}/feedback")
    suspend fun submitFeedback(@Path("messageId") messageId: String, @Body request: FeedbackRequest)
    
    @GET("/api/user/profile")
    suspend fun getProfile(): UserProfile

    @GET("/api/agents")
    suspend fun getAgents(): List<AiAgent>

    @POST("/api/agents")
    suspend fun createAgent(@Body request: CreateAgentRequest): AiAgent
}

@JsonClass(generateAdapter = true)
data class FeedbackRequest(val rating: Int, val comment: String?)

@JsonClass(generateAdapter = true)
data class AiAgent(
    val id: String,
    val name: String,
    val persona: String,
    val instructions: String,
    val hasKnowledgeFiles: Boolean
)

@JsonClass(generateAdapter = true)
data class CreateAgentRequest(
    val name: String,
    val persona: String,
    val instructions: String
)

@JsonClass(generateAdapter = true)
data class LoginRequest(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class SignupRequest(val name: String, val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class AuthResponse(val token: String, val user: UserProfile)

@JsonClass(generateAdapter = true)
data class AiModel(
    val id: String,
    val provider: String,
    val name: String,
    val capabilities: List<String>,
    val contextWindow: Int,
    val speed: String,
    val isFavorite: Boolean
)

@JsonClass(generateAdapter = true)
data class ChatSummary(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val isPinned: Boolean,
    val folder: String?
)

@JsonClass(generateAdapter = true)
data class ChatMessageRequest(
    val chatId: String,
    val message: String,
    val modelId: String
)

@JsonClass(generateAdapter = true)
data class ChatMessageResponse(
    val id: String,
    val text: String,
    val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val plan: String,
    val creditsRemaining: Int,
    val creditsUsed: Int,
    val requests: Int,
    val favoriteModel: String
)
