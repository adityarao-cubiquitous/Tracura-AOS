package com.cubiquitous.tracura.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Chat(
    @PropertyName("id") val id: String = "",
    @PropertyName("participants") val participants: List<String> = emptyList(), // List of user IDs in this chat
    @PropertyName("lastMessage") val lastMessage: String = "",
    @PropertyName("lastMessageTime") val lastMessageTime: Timestamp? = null,
    @PropertyName("lastMessageSenderId") val lastMessageSenderId: String = "",
    @PropertyName("unreadCount") val unreadCount: Map<String, Int> = emptyMap(), // userId -> unread count
    @PropertyName("createdAt") val createdAt: Timestamp? = null,
    @PropertyName("updatedAt") val updatedAt: Timestamp? = null,
    // Legacy field for backward compatibility
    @PropertyName("members") val members: List<String> = emptyList() // Legacy field - use participants instead
) {
    // Computed property to get participants (handles both new and old format)
    val participantsList: List<String>
        get() = if (participants.isNotEmpty()) participants else members
}

data class Message(
    @PropertyName("id") val id: String = "",
    @PropertyName("chatId") val chatId: String = "",
    @PropertyName("senderId") val senderId: String = "",
    @PropertyName("isGroupMessage") val isGroupMessage: Boolean = false,
    @PropertyName("isRead") val isRead: Boolean = false,
    @PropertyName("text") val text: String = "",
    @PropertyName("timeStamp") val timeStamp: String = "", // Formatted as "26 November 2025 at 16:19:02 UTC+5:30"
    @PropertyName("type") val type: String = "text", // "text" or "media"
    // Legacy fields for backward compatibility
    @PropertyName("messageType") val messageType: String = "Text", // Text or Media
    @PropertyName("message") val message: String = "",
    @PropertyName("mediaUrl") val mediaUrl: String? = null,
    @PropertyName("timestampTimestamp") val timestampTimestamp: Timestamp? = null, // Keep Timestamp for queries
    // Additional fields for UI
    @PropertyName("senderName") val senderName: String = "",
    @PropertyName("senderRole") val senderRole: String = "",
    @PropertyName("readBy") val readBy: List<String> = emptyList()
)

data class ChatMember(
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.USER,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
)

data class ExpenseChat(
    @PropertyName("id") val id: String = "",
    @PropertyName("mediaURL") val mediaURL: List<String> = emptyList(), // Array of Firebase Storage URLs
    @PropertyName("mention") val mention: List<String> = emptyList(), // List of mentioned user IDs/names
    @PropertyName("senderId") val senderId: String = "", // "BusinessHead" for BUSINESSHEAD role, phone number for USER/APPROVER
    @PropertyName("senderRole") val senderRole: String = "", // "BUSINESSHEAD", "APPROVER", or "USER"
    @PropertyName("textMessage") val textMessage: String = "",
    @PropertyName("timeStamp") val timeStamp: Timestamp? = null
)

/**
 * Chat Participant Model for displaying participants in chat list
 * Based on Android Chat Implementation Guide
 */
data class ChatParticipant(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val role: UserRole,
    val isOnline: Boolean = false,
    val lastSeen: java.util.Date? = null,
    val unreadCount: Int = 0,
    val lastMessage: String? = null,
    val lastMessageTime: java.util.Date? = null
) {
    val displayName: String
        get() = if (role == UserRole.BUSINESS_HEAD) {
            "$name (BusinessHead)"
        } else {
            name
        }
}

/**
 * Enhanced Chat Type enum
 */
enum class ChatType(val value: String) {
    INDIVIDUAL("individual"),
    GROUP("group");
    
    companion object {
        fun fromString(value: String): ChatType {
            return values().find { it.value == value } ?: INDIVIDUAL
        }
    }
}

/**
 * Enhanced Message Type enum
 */
enum class MessageType(val value: String) {
    TEXT("text"),
    IMAGE("image"),
    VIDEO("video"),
    FILE("file");
    
    companion object {
        fun fromString(value: String): MessageType {
            return values().find { it.value == value } ?: TEXT
        }
    }
}
