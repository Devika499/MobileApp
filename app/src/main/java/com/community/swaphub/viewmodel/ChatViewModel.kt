package com.community.swaphub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.community.swaphub.data.model.Chat
import com.community.swaphub.data.model.ChatConversation
import com.community.swaphub.data.model.User
import com.community.swaphub.data.repository.AuthRepository
import com.community.swaphub.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<Chat>>(emptyList())
    val messages: StateFlow<List<Chat>> = _messages.asStateFlow()

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // UI state for generic errors / loading states
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Expose a separate isLoading flag used by ConversationsListScreen
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // other user's name state (used by ChatScreen)
    private val _otherUserName = MutableStateFlow<String>("User")
    val otherUserName: StateFlow<String> = _otherUserName.asStateFlow()

    init {
        // Observe current user from AuthRepository
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                _currentUser.value = user
            }
        }
    }

    /**
     * Fetch the other user's profile (name) via repository helper.
     * This ensures the name is available even when backend conversation objects omit names.
     */
    fun loadOtherUser(otherUserId: String) {
        viewModelScope.launch {
            try {
                val user = chatRepository.fetchUser(otherUserId)
                _otherUserName.value = user?.name ?: "User"
            } catch (e: Exception) {
                _otherUserName.value = "User"
            }
        }
    }

    /**
     * Load conversations for the current user.
     * Sets isLoading while fetching.
     */
    fun loadConversations() {
        viewModelScope.launch {
            val userId = _currentUser.value?.id
            if (userId == null) {
                _uiState.value = ChatUiState.Error("User not logged in")
                return@launch
            }

            _isLoading.value = true
            _uiState.value = ChatUiState.Loading

            try {
                chatRepository.getConversations(userId).collect { result ->
                    result.onSuccess { list ->
                        _conversations.value = list
                        _uiState.value = ChatUiState.Success
                    }.onFailure { err ->
                        _uiState.value = ChatUiState.Error(err.message ?: "Failed to load conversations")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error(e.message ?: "Error loading conversations")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create or find conversation and load messages for it.
     * Sets uiState while loading messages.
     */
    fun loadMessages(otherUserId: String, itemId: String?) {
        viewModelScope.launch {
            val currentId = _currentUser.value?.id
            if (currentId == null) {
                _uiState.value = ChatUiState.Error("User not logged in")
                return@launch
            }

            _uiState.value = ChatUiState.Loading
            try {
                // Ensure we have conversations list updated (optional)
                // This helps the UI display names if they exist in conversations
                try {
                    chatRepository.getConversations(currentId).collect { result ->
                        result.onSuccess { list -> _conversations.value = list }
                    }
                } catch (_: Exception) { /* ignore */ }

                // Find or create conversation
                val conversationId = chatRepository.findOrCreateConversation(currentId, otherUserId, itemId)
                _currentConversationId.value = conversationId

                if (conversationId != null) {
                    chatRepository.getMessagesByConversation(conversationId).collect { result ->
                        result.onSuccess { msgs -> _messages.value = msgs }
                            .onFailure { err ->
                                _uiState.value = ChatUiState.Error(err.message ?: "Failed to load messages")
                            }
                    }
                    _uiState.value = ChatUiState.Success
                } else {
                    _uiState.value = ChatUiState.Error("Failed to create/find conversation")
                }
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error(e.message ?: "Error loading messages")
            }
        }
    }

    /**
     * Send a message and reload messages & conversations on success.
     */
    fun sendMessage(receiverId: String, message: String, itemId: String?) {
        viewModelScope.launch {
            val senderId = _currentUser.value?.id
            val convId = _currentConversationId.value

            if (senderId == null) {
                _uiState.value = ChatUiState.Error("User not logged in")
                return@launch
            }

            if (convId == null) {
                // create/find then retry
                val newConv = chatRepository.findOrCreateConversation(senderId, receiverId, itemId)
                _currentConversationId.value = newConv
            }

            val finalConvId = _currentConversationId.value
            if (finalConvId == null) {
                _uiState.value = ChatUiState.Error("Conversation not available")
                return@launch
            }

            val result = chatRepository.sendMessage(finalConvId, senderId, message)
            if (result.isSuccess) {
                // reload messages + conversations to update UI
                loadMessages(receiverId, itemId)
                loadConversations()
            } else {
                _uiState.value = ChatUiState.Error(result.exceptionOrNull()?.message ?: "Failed to send message")
            }
        }
    }

    fun clearError() {
        _uiState.value = ChatUiState.Idle
    }

    // Optionally expose a manual setter for debug/testing
    fun setCurrentUserForDebug(user: User) {
        _currentUser.value = user
    }
}

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    object Success : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}
