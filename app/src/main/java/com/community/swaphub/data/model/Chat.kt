package com.community.swaphub.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val itemId: String? = null,
    val message: String,
    val timestamp: String? = null,
    val isRead: Boolean = false
)

data class ChatConversation(
    val id: String,
    val otherUserId: String,
    val otherUserName: String? = null,
    val otherUserImageUrl: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: String? = null,
    val unreadCount: Int = 0,
    val itemId: String? = null,
    val itemTitle: String? = null
)

// Backend response models
data class ChatBackendResponse(
    val id: String,
    val conversation: ConversationResponse?,
    val sender: UserResponse?,
    val message: String,
    val timestamp: String?
) {
    fun toChat(): Chat {
        // Extract receiverId from conversation using safe calls
        val receiverId = when {
            conversation?.user1?.id == sender?.id -> conversation?.user2?.id
            conversation?.user2?.id == sender?.id -> conversation?.user1?.id
            else -> null
        } ?: "" // Provide default value if null

        return Chat(
            id = id,
            conversationId = conversation?.id ?: "", // Safe null handling
            senderId = sender?.id ?: "", // Safe null handling
            receiverId = receiverId,
            itemId = conversation?.item?.id,
            message = message,
            timestamp = timestamp,
            isRead = false
        )
    }
}

data class ConversationResponse(
    val id: String,
    val user1: UserResponse?,
    val user2: UserResponse?,
    val item: ItemResponse?
)

data class UserResponse(
    val id: String,
    val name: String?,
    val email: String?
)

data class ItemResponse(
    val id: String,
    val title: String?
)

// DTOs for sending data to backend
data class ConversationRequestDTO(
    val senderId: String,
    val receiverId: String,
    val itemId: String
)

data class ChatMessageDTO(
    val conversationId: String?,
    val senderId: String,
    val message: String
)