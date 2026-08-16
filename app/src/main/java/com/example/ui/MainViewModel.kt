package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AiAgent
import com.example.data.api.AiModel
import com.example.data.api.ChatSummary
import com.example.data.api.UserProfile
import com.example.data.repository.InfinityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val repository = InfinityRepository()

    private val _models = MutableStateFlow<List<AiModel>>(emptyList())
    val models: StateFlow<List<AiModel>> = _models.asStateFlow()

    private val _recentChats = MutableStateFlow<List<ChatSummary>>(emptyList())
    val recentChats: StateFlow<List<ChatSummary>> = _recentChats.asStateFlow()
    
    private val _agents = MutableStateFlow<List<AiAgent>>(emptyList())
    val agents: StateFlow<List<AiAgent>> = _agents.asStateFlow()

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            launch {
                repository.getModels().collect { result ->
                    result.onSuccess { _models.value = it }
                }
            }
            launch {
                repository.getRecentChats().collect { result ->
                    result.onSuccess { _recentChats.value = it }
                }
            }
            launch {
                repository.getProfile().collect { result ->
                    result.onSuccess { _profile.value = it }
                }
            }
            launch {
                repository.getAgents().collect { result ->
                    result.onSuccess { _agents.value = it }
                }
            }
            _isLoading.value = false
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.login(email, password).onSuccess {
                _profile.value = it.user
                onSuccess()
            }.onFailure {
                onError(it.message ?: "Login failed")
            }
            _isLoading.value = false
        }
    }

    fun signup(name: String, email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.signup(name, email, password).onSuccess {
                _profile.value = it.user
                onSuccess()
            }.onFailure {
                onError(it.message ?: "Signup failed")
            }
            _isLoading.value = false
        }
    }

    suspend fun sendMessage(chatId: String, message: String, modelId: String) =
        repository.sendMessage(chatId, message, modelId)
        
    fun submitFeedback(messageId: String, rating: Int, comment: String?) {
        viewModelScope.launch {
            repository.submitFeedback(messageId, rating, comment)
        }
    }
    
    fun createAgent(name: String, persona: String, instructions: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.createAgent(name, persona, instructions)
            result.onSuccess { newAgent ->
                val updated = _agents.value.toMutableList()
                updated.add(newAgent)
                _agents.value = updated
                onSuccess()
            }.onFailure {
                _error.value = "Failed to create agent"
            }
        }
    }
}
