package com.community.swaphub.data.repository

import com.community.swaphub.data.api.ApiService
import com.community.swaphub.data.local.dao.ChatDao
import com.community.swaphub.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val apiService: ApiService,
    private val chatDao: ChatDao
) {

    suspend fun sendMessage(conversationId: String, senderId: String, message: String): Result<Chat> {
        return try {
            val chatMessageDTO = ChatMessageDTO(
                conversationId = conversationId,
                senderId = senderId,
                message = message
            )

            val response = apiService.sendMessage(chatMessageDTO)
            if (response.isSuccessful && response.body() != null) {
                val backendResponse = response.body()!!
                // Convert backend response to our Chat model
                val chat = backendResponse.toChat()
                chatDao.insertMessage(chat)
                Result.success(chat)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to send message"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createConversation(senderId: String, receiverId: String, itemId: String?): Result<ConversationResponse> {
        return try {
            // Only send itemId if it's not null and not empty and is a valid UUID
            val validItemId = if (itemId.isNullOrEmpty() || !isValidUUID(itemId)) {
                null
            } else {
                itemId
            }

            val request = ConversationRequestDTO(
                senderId = senderId,
                receiverId = receiverId,
                itemId = validItemId ?: "" // Send empty string instead of null
            )

            val response = apiService.createConversation(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to create conversation"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getConversations(userId: String): Flow<Result<List<ChatConversation>>> = flow {
        try {
            println("DEBUG: Fetching conversations for user: $userId")
            val response = apiService.getConversations(userId)
            println("DEBUG: API Response code: ${response.code()}")
            println("DEBUG: API Response message: ${response.message()}")

            if (response.isSuccessful) {
                val backendConversations = response.body() ?: emptyList()
                println("DEBUG: Received ${backendConversations.size} conversations from backend")

                // Convert each backend Conversation to ChatConversation
                val conversations = mutableListOf<ChatConversation>()
                for (backendConv in backendConversations) {
                    println("DEBUG: Processing conversation: ${backendConv.id}")
                    val chatConversation = convertToChatConversation(backendConv, userId)
                    conversations.add(chatConversation)
                    println("DEBUG: Converted to ChatConversation: ${chatConversation.otherUserName}")
                }

                println("DEBUG: Successfully converted ${conversations.size} conversations")
                emit(Result.success(conversations))
            } else {
                val errorMsg = "Failed to fetch conversations: ${response.code()} - ${response.message()}"
                println("DEBUG: $errorMsg")
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            val errorMsg = "Exception fetching conversations: ${e.message}"
            println("DEBUG: $errorMsg")
            e.printStackTrace()
            emit(Result.failure(e))
        }
    }

    private suspend fun convertToChatConversation(
        conversation: ConversationResponse,
        currentUserId: String
    ): ChatConversation {

        // Determine other user ID
        val otherUserId = when {
            conversation.user1?.id == currentUserId -> conversation.user2?.id
            conversation.user2?.id == currentUserId -> conversation.user1?.id
            else -> null
        } ?: ""

        // Try to get from backend conversation
        var otherUserName = when {
            conversation.user1?.id == otherUserId -> conversation.user1?.name
            conversation.user2?.id == otherUserId -> conversation.user2?.name
            else -> null
        }

        // 🔥 FIX: Backend often returns name = null → fetch from /api/users/{id}
        if (otherUserName.isNullOrBlank() && otherUserId.isNotEmpty()) {
            try {
                val response = apiService.getUser(otherUserId)
                if (response.isSuccessful && response.body() != null) {
                    otherUserName = response.body()!!.name
                }
            } catch (_: Exception) { }
        }

        // Load last message
        val lastMessageInfo = getLastMessageForConversation(conversation.id)

        return ChatConversation(
            id = conversation.id,
            otherUserId = otherUserId,
            otherUserName = otherUserName ?: "Unknown User",
            otherUserImageUrl = null,
            lastMessage = lastMessageInfo?.message ?: "Start a conversation...",
            lastMessageTime = lastMessageInfo?.timestamp,
            unreadCount = 0,
            itemId = conversation.item?.id,
            itemTitle = conversation.item?.title
        )
    }



    private suspend fun getLastMessageForConversation(conversationId: String): Chat? {
        return try {
            val response = apiService.getMessagesByConversation(conversationId)
            if (response.isSuccessful && response.body() != null) {
                val messages = response.body()!!
                // Get the last message (assuming messages are ordered by timestamp)
                messages.lastOrNull()?.toChat()
            } else {
                null
            }
        } catch (e: Exception) {
            println("DEBUG: Error getting last message for conversation $conversationId: ${e.message}")
            null
        }
    }

    suspend fun fetchUser(userId: String): User? {
        return try {
            val response = apiService.getUser(userId)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }
    fun getMessagesByConversation(conversationId: String): Flow<Result<List<Chat>>> = flow {
        try {
            val response = apiService.getMessagesByConversation(conversationId)
            if (response.isSuccessful && response.body() != null) {
                val backendMessages = response.body()!!
                // Convert all backend responses to Chat models
                val messages = backendMessages.map { it.toChat() }
                chatDao.insertMessages(messages)
                emit(Result.success(messages))
            } else {
                // Return empty list instead of error if no messages found
                emit(Result.success(emptyList()))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // Helper method to find or create conversation between two users
    suspend fun findOrCreateConversation(userId1: String, userId2: String, itemId: String?): String? {
        return try {
            // First, get all conversations for user1
            val conversationsResponse = apiService.getConversations(userId1)
            if (conversationsResponse.isSuccessful && conversationsResponse.body() != null) {
                val backendConversations = conversationsResponse.body()!!

                // Look for existing conversation with userId2 (ignore itemId to find any conversation)
                val existingConversation = backendConversations.find { backendConv ->
                    val otherUser = if (backendConv.user1?.id == userId1) backendConv.user2 else backendConv.user1
                    otherUser?.id == userId2
                }

                if (existingConversation != null) {
                    return existingConversation.id
                }
            }

            // Create new conversation if none exists - use null for itemId to avoid backend errors
            val result = createConversation(userId1, userId2, null)
            result.getOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }

    fun getLocalMessages(userId1: String, userId2: String): Flow<List<Chat>> {
        return chatDao.getMessages(userId1, userId2)
    }

    suspend fun markAsRead(userId: String, otherUserId: String) {
        chatDao.markAsRead(userId, otherUserId)
    }

    // Helper function to check if a string is a valid UUID
    private fun isValidUUID(string: String): Boolean {
        return try {
            java.util.UUID.fromString(string)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}