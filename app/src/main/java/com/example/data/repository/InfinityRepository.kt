package com.example.data.repository

import com.example.data.api.AiAgent
import com.example.data.api.AiModel
import com.example.data.api.ApiClient
import com.example.data.api.AuthResponse
import com.example.data.api.ChatMessageRequest
import com.example.data.api.ChatMessageResponse
import com.example.data.api.ChatSummary
import com.example.data.api.CreateAgentRequest
import com.example.data.api.FeedbackRequest
import com.example.data.api.LoginRequest
import com.example.data.api.SignupRequest
import com.example.data.api.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class InfinityRepository {
    private val api = ApiClient.service

    fun getModels(): Flow<Result<List<AiModel>>> = flow {
        try {
            emit(Result.success(api.getModels()))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getRecentChats(): Flow<Result<List<ChatSummary>>> = flow {
        try {
            emit(Result.success(api.getRecentChats()))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getProfile(): Flow<Result<UserProfile>> = flow {
        try {
            emit(Result.success(api.getProfile()))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            ApiClient.currentToken = response.token
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signup(name: String, email: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.signup(SignupRequest(name, email, password))
            ApiClient.currentToken = response.token
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAgents(): Flow<Result<List<AiAgent>>> = flow {
        try {
            emit(Result.success(api.getAgents()))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun createAgent(name: String, persona: String, instructions: String): Result<AiAgent> {
        return try {
            Result.success(api.createAgent(CreateAgentRequest(name, persona, instructions)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(chatId: String, message: String, modelId: String): Result<ChatMessageResponse> {
        return try {
            Result.success(api.sendMessage(ChatMessageRequest(chatId, message, modelId)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun submitFeedback(messageId: String, rating: Int, comment: String?): Result<Unit> {
        return try {
            api.submitFeedback(messageId, FeedbackRequest(rating, comment))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
