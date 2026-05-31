package com.cubiquitous.tracura.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.ExpenseChat
import com.cubiquitous.tracura.repository.ProjectRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * ViewModel for Expense Chat feature
 * Provides real-time message fetching and sending functionality
 * 
 * Path: customers/{customerId}/projects/{projectId}/expenses/{expenseId}/expenseChats
 */
@HiltViewModel
class ExpenseChatViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ExpenseChat>>(emptyList())
    val messages: StateFlow<List<ExpenseChat>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // Cache for user names to avoid repeated fetches
    private val userNameCache = mutableMapOf<String, String>()

    private var listenerRegistration: ListenerRegistration? = null

    /**
     * Load expense chat messages in real-time
     * Uses addSnapshotListener for instant updates without delays
     * 
     * @param expenseId The expense document ID
     * @param projectId The project document ID
     * @param customerId Optional customer ID. If null, will be fetched automatically
     */
    fun loadMessages(expenseId: String, projectId: String, customerId: String? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Get customer ID if not provided
                val finalCustomerId = customerId ?: projectRepository.findCustomerIdForProject(projectId)
                
                if (finalCustomerId == null) {
                    _error.value = "Could not find customer ID for project"
                    _isLoading.value = false
                    Log.e("ExpenseChatViewModel", "❌ Could not find customer ID for project: $projectId")
                    return@launch
                }

                Log.d("ExpenseChatViewModel", "📋 Loading messages for expense: $expenseId, project: $projectId, customer: $finalCustomerId")

                // Remove previous listener if exists
                listenerRegistration?.remove()

                // Get expenseChats collection reference
                val expenseChatsCollection = firestore
                    .collection("customers")
                    .document(finalCustomerId)
                    .collection("projects")
                    .document(projectId)
                    .collection("expenses")
                    .document(expenseId)
                    .collection("expenseChats")

                // Set up real-time listener (fast fetch method - no delays)
                listenerRegistration = expenseChatsCollection
                    .orderBy("timeStamp", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        _isLoading.value = false

                        if (error != null) {
                            _error.value = "Error loading messages: ${error.message}"
                            Log.e("ExpenseChatViewModel", "❌ Error loading messages: ${error.message}")
                            return@addSnapshotListener
                        }

                        if (snapshot == null) {
                            Log.w("ExpenseChatViewModel", "⚠️ Snapshot is null")
                            _messages.value = emptyList()
                            return@addSnapshotListener
                        }

                        Log.d("ExpenseChatViewModel", "📥 Received ${snapshot.documents.size} messages from Firestore")

                        // Parse documents to ExpenseChat objects
                        val parsedMessages = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data ?: return@mapNotNull null

                                // Handle mediaURL: can be List<String> (new format) or String (old format for backward compatibility)
                                val mediaURLList = when (val mediaURLData = data["mediaURL"]) {
                                    is List<*> -> mediaURLData.filterIsInstance<String>()
                                    is String -> if (mediaURLData.isNotEmpty()) listOf(mediaURLData) else emptyList()
                                    else -> emptyList<String>()
                                }

                                // Handle mention: can be List<String> (new format) or String (old format for backward compatibility)
                                val mentionList = when (val mentionData = data["mention"]) {
                                    is List<*> -> mentionData.filterIsInstance<String>()
                                    is String -> if (mentionData.isNotEmpty()) listOf(mentionData) else emptyList()
                                    else -> emptyList<String>()
                                }

                                ExpenseChat(
                                    id = doc.id,
                                    mediaURL = mediaURLList,
                                    mention = mentionList,
                                    senderId = data["senderId"] as? String ?: "",
                                    senderRole = data["senderRole"] as? String ?: "",
                                    textMessage = data["textMessage"] as? String ?: "",
                                    timeStamp = (data["timeStamp"] as? Timestamp) ?: (data["timestamp"] as? Timestamp) // Backward compatibility
                                )
                            } catch (e: Exception) {
                                Log.e("ExpenseChatViewModel", "❌ Error parsing message ${doc.id}: ${e.message}", e)
                                null
                            }
                        }

                        Log.d("ExpenseChatViewModel", "✅ Parsed ${parsedMessages.size} messages")
                        _messages.value = parsedMessages
                    }

            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Error loading messages: ${e.message}"
                Log.e("ExpenseChatViewModel", "❌ Error in loadMessages: ${e.message}", e)
            }
        }
    }

    /**
     * Send a text message to expense chat
     * 
     * @param message The message text
     * @param senderId The sender identifier ("BusinessHead" for BUSINESSHEAD role, phone number for USER/APPROVER)
     * @param senderRole The sender role ("BUSINESSHEAD", "APPROVER", or "USER")
     * @param expenseId The expense document ID
     * @param projectId The project document ID
     * @param customerId Optional customer ID. If null, will be fetched automatically
     */
    suspend fun sendMessage(
        message: String,
        senderId: String,
        senderRole: String,
        expenseId: String,
        projectId: String,
        customerId: String? = null
    ): Result<Unit> {
        return try {
            _isSending.value = true
            _error.value = null

            // Get customer ID if not provided
            val finalCustomerId = customerId ?: projectRepository.findCustomerIdForProject(projectId)
            
            if (finalCustomerId == null) {
                _error.value = "Could not find customer ID for project"
                _isSending.value = false
                return Result.failure(Exception("Could not find customer ID for project"))
            }

            // Format senderRole according to guide: "BUSINESSHEAD", "APPROVER", or "USER"
            val formattedSenderRole = when (senderRole.uppercase().replace("_", "")) {
                "BUSINESSHEAD", "BUSINESSHEAD" -> "BUSINESSHEAD"
                "APPROVER" -> "APPROVER"
                "USER" -> "USER"
                else -> senderRole.uppercase().replace("_", "")
            }

            Log.d("ExpenseChatViewModel", "🚀 Sending message:")
            Log.d("ExpenseChatViewModel", "   ExpenseId: $expenseId")
            Log.d("ExpenseChatViewModel", "   ProjectId: $projectId")
            Log.d("ExpenseChatViewModel", "   CustomerId: $finalCustomerId")
            Log.d("ExpenseChatViewModel", "   SenderId: $senderId")
            Log.d("ExpenseChatViewModel", "   SenderRole: $formattedSenderRole")
            Log.d("ExpenseChatViewModel", "   Message: $message")

            // Get expenseChats collection reference
            val expenseChatsCollection = firestore
                .collection("customers")
                .document(finalCustomerId)
                .collection("projects")
                .document(projectId)
                .collection("expenses")
                .document(expenseId)
                .collection("expenseChats")

            // Create message data according to guide format
            val messageData = hashMapOf<String, Any>(
                "senderId" to senderId,
                "senderRole" to formattedSenderRole,
                "textMessage" to message,
                "timeStamp" to FieldValue.serverTimestamp(),
                "mediaURL" to emptyList<String>(), // Always save as empty list if no media (per guide)
                "mention" to emptyList<String>() // Always save as empty list (per guide)
            )

            // Add message to Firestore and get document reference
            val expenseChatDocRef = expenseChatsCollection.add(messageData).await()
            
            // Update the document with the id field set to the document ID
            expenseChatDocRef.update("id", expenseChatDocRef.id).await()

            Log.d("ExpenseChatViewModel", "✅ Message sent successfully")
            _isSending.value = false
            Result.success(Unit)

        } catch (e: Exception) {
            _isSending.value = false
            _error.value = "Error sending message: ${e.message}"
            Log.e("ExpenseChatViewModel", "❌ Error sending message: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send a message with media (images)
     * 
     * @param message The message text (can be empty, will default to "📷 Image")
     * @param mediaURLs List of Firebase Storage download URLs
     * @param senderId The sender identifier
     * @param senderRole The sender role
     * @param expenseId The expense document ID
     * @param projectId The project document ID
     * @param customerId Optional customer ID. If null, will be fetched automatically
     */
    suspend fun sendMessageWithMedia(
        message: String,
        mediaURLs: List<String>,
        senderId: String,
        senderRole: String,
        expenseId: String,
        projectId: String,
        customerId: String? = null
    ): Result<Unit> {
        return try {
            _isSending.value = true
            _error.value = null

            // Get customer ID if not provided
            val finalCustomerId = customerId ?: projectRepository.findCustomerIdForProject(projectId)
            
            if (finalCustomerId == null) {
                _error.value = "Could not find customer ID for project"
                _isSending.value = false
                return Result.failure(Exception("Could not find customer ID for project"))
            }

            // Format senderRole according to guide
            val formattedSenderRole = when (senderRole.uppercase().replace("_", "")) {
                "BUSINESSHEAD", "BUSINESSHEAD" -> "BUSINESSHEAD"
                "APPROVER" -> "APPROVER"
                "USER" -> "USER"
                else -> senderRole.uppercase().replace("_", "")
            }

            // Use default message if empty
            val finalMessage = if (message.isBlank()) "📷 Image" else message

            Log.d("ExpenseChatViewModel", "🚀 Sending message with media:")
            Log.d("ExpenseChatViewModel", "   Media URLs: ${mediaURLs.size}")
            Log.d("ExpenseChatViewModel", "   Message: $finalMessage")

            // Get expenseChats collection reference
            val expenseChatsCollection = firestore
                .collection("customers")
                .document(finalCustomerId)
                .collection("projects")
                .document(projectId)
                .collection("expenses")
                .document(expenseId)
                .collection("expenseChats")

            // Create message data with media URLs
            val messageData = hashMapOf<String, Any>(
                "senderId" to senderId,
                "senderRole" to formattedSenderRole,
                "textMessage" to finalMessage,
                "timeStamp" to FieldValue.serverTimestamp(),
                "mediaURL" to mediaURLs, // List of Firebase Storage URLs
                "mention" to emptyList<String>() // Always save as empty list (per guide)
            )

            // Add message to Firestore and get document reference
            val expenseChatDocRef = expenseChatsCollection.add(messageData).await()
            
            // Update the document with the id field set to the document ID
            expenseChatDocRef.update("id", expenseChatDocRef.id).await()

            Log.d("ExpenseChatViewModel", "✅ Message with media sent successfully")
            _isSending.value = false
            Result.success(Unit)

        } catch (e: Exception) {
            _isSending.value = false
            _error.value = "Error sending message with media: ${e.message}"
            Log.e("ExpenseChatViewModel", "❌ Error sending message with media: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send a message with mentions
     * 
     * @param message The message text
     * @param mentions List of mentioned user identifiers (phone numbers or names)
     * @param senderId The sender identifier
     * @param senderRole The sender role
     * @param expenseId The expense document ID
     * @param projectId The project document ID
     * @param customerId Optional customer ID. If null, will be fetched automatically
     */
    suspend fun sendMessageWithMentions(
        message: String,
        mentions: List<String>,
        senderId: String,
        senderRole: String,
        expenseId: String,
        projectId: String,
        customerId: String? = null
    ): Result<Unit> {
        return try {
            _isSending.value = true
            _error.value = null

            // Get customer ID if not provided
            val finalCustomerId = customerId ?: projectRepository.findCustomerIdForProject(projectId)
            
            if (finalCustomerId == null) {
                _error.value = "Could not find customer ID for project"
                _isSending.value = false
                return Result.failure(Exception("Could not find customer ID for project"))
            }

            // Format senderRole according to guide
            val formattedSenderRole = when (senderRole.uppercase().replace("_", "")) {
                "BUSINESSHEAD", "BUSINESSHEAD" -> "BUSINESSHEAD"
                "APPROVER" -> "APPROVER"
                "USER" -> "USER"
                else -> senderRole.uppercase().replace("_", "")
            }

            Log.d("ExpenseChatViewModel", "🚀 Sending message with mentions:")
            Log.d("ExpenseChatViewModel", "   Mentions: $mentions")

            // Get expenseChats collection reference
            val expenseChatsCollection = firestore
                .collection("customers")
                .document(finalCustomerId)
                .collection("projects")
                .document(projectId)
                .collection("expenses")
                .document(expenseId)
                .collection("expenseChats")

            // Create message data with mentions
            val messageData = hashMapOf<String, Any>(
                "senderId" to senderId,
                "senderRole" to formattedSenderRole,
                "textMessage" to message,
                "timeStamp" to FieldValue.serverTimestamp(),
                "mediaURL" to emptyList<String>(), // Always save as empty list if no media (per guide)
                "mention" to mentions // List of mentioned user IDs/names
            )

            // Add message to Firestore and get document reference
            val expenseChatDocRef = expenseChatsCollection.add(messageData).await()
            
            // Update the document with the id field set to the document ID
            expenseChatDocRef.update("id", expenseChatDocRef.id).await()

            Log.d("ExpenseChatViewModel", "✅ Message with mentions sent successfully")
            _isSending.value = false
            Result.success(Unit)

        } catch (e: Exception) {
            _isSending.value = false
            _error.value = "Error sending message with mentions: ${e.message}"
            Log.e("ExpenseChatViewModel", "❌ Error sending message with mentions: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch user name from users collection based on senderId (phone number)
     * Similar to iOS loadUserData method
     * 
     * @param senderId The sender identifier (phone number for USER/APPROVER, "BusinessHead" for BUSINESSHEAD)
     * @return User name if found, null otherwise
     */
    suspend fun fetchUserName(senderId: String): String? {
        // Check cache first
        userNameCache[senderId]?.let { return it }

        // Handle BusinessHead case
        if (senderId == "BusinessHead") {
            return "BusinessHead"
        }

        return try {
            Log.d("ExpenseChatViewModel", "🔍 Fetching user name for senderId: $senderId")

            // Try to get user by document ID (phone number)
            val userDoc = firestore
                .collection("users")
                .document(senderId)
                .get()
                .await()

            if (userDoc.exists()) {
                val name = userDoc.getString("name")
                if (name != null && name.isNotBlank()) {
                    // Cache the name
                    userNameCache[senderId] = name
                    Log.d("ExpenseChatViewModel", "✅ Found user name: $name for senderId: $senderId")
                    return name
                }
            }

            // Try querying by phoneNumber field if document ID didn't work
            val phoneQuery = firestore
                .collection("users")
                .whereEqualTo("phoneNumber", senderId)
                .limit(1)
                .get()
                .await()

            if (!phoneQuery.isEmpty) {
                val name = phoneQuery.documents.first().getString("name")
                if (name != null && name.isNotBlank()) {
                    userNameCache[senderId] = name
                    Log.d("ExpenseChatViewModel", "✅ Found user name by phoneNumber: $name for senderId: $senderId")
                    return name
                }
            }

            // Try phone field (alternative field name)
            val phoneQuery2 = firestore
                .collection("users")
                .whereEqualTo("phone", senderId)
                .limit(1)
                .get()
                .await()

            if (!phoneQuery2.isEmpty) {
                val name = phoneQuery2.documents.first().getString("name")
                if (name != null && name.isNotBlank()) {
                    userNameCache[senderId] = name
                    Log.d("ExpenseChatViewModel", "✅ Found user name by phone: $name for senderId: $senderId")
                    return name
                }
            }

            Log.w("ExpenseChatViewModel", "⚠️ User not found for senderId: $senderId")
            null

        } catch (e: Exception) {
            Log.e("ExpenseChatViewModel", "❌ Error fetching user name: ${e.message}", e)
            null
        }
    }

    /**
     * Get cached or fetch user name
     * Returns cached name if available, otherwise fetches and caches it
     * 
     * @param senderId The sender identifier
     * @return User name or senderId as fallback
     */
    suspend fun getCachedOrFetchUserName(senderId: String): String {
        return userNameCache[senderId] ?: run {
            val name = fetchUserName(senderId) ?: senderId
            if (name != senderId) {
                userNameCache[senderId] = name
            }
            name
        }
    }

    /**
     * Clear user name cache
     */
    fun clearUserNameCache() {
        userNameCache.clear()
    }

    /**
     * Clean up listener when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
        listenerRegistration = null
        Log.d("ExpenseChatViewModel", "🧹 ViewModel cleared, listener removed")
    }
}
