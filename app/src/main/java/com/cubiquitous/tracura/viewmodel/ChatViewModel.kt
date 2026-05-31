package com.cubiquitous.tracura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.Chat
import com.cubiquitous.tracura.model.ChatMember
import com.cubiquitous.tracura.model.Message
import com.cubiquitous.tracura.model.ExpenseChat
import com.cubiquitous.tracura.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import com.google.firebase.Timestamp
import com.cubiquitous.tracura.utils.FormatUtils
import javax.inject.Inject

data class ChatState(
    val teamMembers: List<ChatMember> = emptyList(),
    val chats: List<Chat> = emptyList(),
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentChatId: String = "",
    val currentChatUser: ChatMember? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()
    
    private val _expenseChats = MutableStateFlow<List<ExpenseChat>>(emptyList())
    val expenseChats: StateFlow<List<ExpenseChat>> = _expenseChats.asStateFlow()

    // Track the current message collection job to prevent multiple listeners
    private var messagesCollectionJob: Job? = null

    // Load team members for a project
    fun loadTeamMembers(projectId: String, currentUserId: String) {
        viewModelScope.launch {
            _chatState.value = _chatState.value.copy(isLoading = true, error = null)
            try {
                val members = chatRepository.getProjectTeamMembers(projectId, currentUserId)
                _chatState.value = _chatState.value.copy(
                    teamMembers = members,
                    isLoading = false
                )
            } catch (e: Exception) {
                _chatState.value = _chatState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    // Start a group chat for a project
    fun startGroupChat(projectId: String, currentUserId: String, onChatCreated: (String) -> Unit) {
        viewModelScope.launch {
            _chatState.value = _chatState.value.copy(isLoading = true)
            try {
                val chatId = chatRepository.getOrCreateGroupChat(projectId, currentUserId)
                _chatState.value = _chatState.value.copy(
                    currentChatId = chatId,
                    isLoading = false
                )
                onChatCreated(chatId)
            } catch (e: Exception) {
                _chatState.value = _chatState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    // Start a chat with a user
    fun startChat(projectId: String, currentUserId: String, otherUserId: String, onChatCreated: (String) -> Unit) {
        viewModelScope.launch {
            _chatState.value = _chatState.value.copy(isLoading = true)
            try {
                val chatId = chatRepository.getOrCreateChat(projectId, currentUserId, otherUserId)
                val user = chatRepository.getUserById(otherUserId)
                _chatState.value = _chatState.value.copy(
                    currentChatId = chatId,
                    currentChatUser = user,
                    isLoading = false
                )
                onChatCreated(chatId)
            } catch (e: Exception) {
                _chatState.value = _chatState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    // Load messages for a chat
    // Uses Firestore real-time listener — messages appear instantly without optimistic updates
    fun loadMessages(projectId: String, chatId: String) {
        // Cancel previous collection job to prevent multiple listeners
        messagesCollectionJob?.cancel()
        
        messagesCollectionJob = viewModelScope.launch {
            chatRepository.getChatMessages(projectId, chatId)
                .catch { e ->
                    _chatState.value = _chatState.value.copy(error = "Error loading messages: ${e.message}")
                }
                .collect { messages ->
                    _chatState.value = _chatState.value.copy(messages = messages)
                }
        }
    }

    // Send a text message
    fun sendMessage(projectId: String, chatId: String, senderId: String, senderName: String, senderRole: String, message: String, context: android.content.Context? = null) {
        viewModelScope.launch {
            try {
                chatRepository.sendMessage(projectId, chatId, senderId, senderName, senderRole, message, context = context)
            } catch (e: Exception) {
                _chatState.value = _chatState.value.copy(
                    error = "Failed to send message: ${e.message}"
                )
            }
        }
    }

    // Send a media message (image or file)
    suspend fun sendImageMessage(projectId: String, chatId: String, senderId: String, senderName: String, senderRole: String, imageUri: android.net.Uri): Boolean {
        return try {
            val success = chatRepository.sendImageMessage(projectId, chatId, senderId, senderName, senderRole, imageUri)
            if (!success) {
                _chatState.value = _chatState.value.copy(
                    error = "Failed to send attachment"
                )
            }
            success
        } catch (e: Exception) {
            _chatState.value = _chatState.value.copy(
                error = "Error sending attachment: ${e.message}"
            )
            false
        }
    }

    // Non-suspend wrapper for sending image/file message
    fun sendImageMessageAsync(projectId: String, chatId: String, senderId: String, senderName: String, senderRole: String, imageUri: android.net.Uri) {
        viewModelScope.launch {
            sendImageMessage(projectId, chatId, senderId, senderName, senderRole, imageUri)
        }
    }

    // Mark messages as read
    fun markMessagesAsRead(projectId: String, chatId: String, userId: String) {
        viewModelScope.launch(kotlinx.coroutines.NonCancellable) {
            try {
                chatRepository.markMessagesAsRead(projectId, chatId, userId)
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Retry with NonCancellable to ensure it completes
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
            chatRepository.markMessagesAsRead(projectId, chatId, userId)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "❌ Error in markMessagesAsRead: ${e.message}", e)
            }
        }
    }

    // Load user chats
    fun loadUserChats(userId: String, projectId: String) {
        viewModelScope.launch {
            chatRepository.getUserChats(userId, projectId).collect { chats ->
                chats.forEach { chat ->
                    val unreadCount = chat.unreadCount
                }
                _chatState.value = _chatState.value.copy(chats = chats)
            }
        }
    }

    // Set current chat user
    fun setCurrentChatUser(user: ChatMember?) {
        _chatState.value = _chatState.value.copy(currentChatUser = user)
    }

    // Clear error
    fun clearError() {
        _chatState.value = _chatState.value.copy(error = null)
    }
    
    // Load expenseChats for a specific expense
    fun loadExpenseChats(projectId: String, expenseId: String) {
        viewModelScope.launch {
            chatRepository.getExpenseChats(projectId, expenseId).collect { chats ->
                _expenseChats.value = chats
            }
        }
    }
}
