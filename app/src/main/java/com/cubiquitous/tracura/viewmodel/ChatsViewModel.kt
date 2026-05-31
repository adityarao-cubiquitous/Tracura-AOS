package com.cubiquitous.tracura.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.ChatParticipant
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.repository.AuthRepository
import com.cubiquitous.tracura.repository.ChatRepository
import com.cubiquitous.tracura.repository.ProjectRepository
import com.cubiquitous.tracura.service.PresenceManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for loading chat participants list
 * Based on Android Chat Implementation Guide
 */
@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore,
    private val presenceManager: PresenceManager
) : ViewModel() {
    
    private val _participants = MutableStateFlow<List<ChatParticipant>>(emptyList())
    val participants: StateFlow<List<ChatParticipant>> = _participants.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _presenceStatuses = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val presenceStatuses: StateFlow<Map<String, Boolean>> = _presenceStatuses.asStateFlow()

    private val presenceJobs = mutableMapOf<String, Job>()

    private var cachedParticipants: List<ChatParticipant> = emptyList()
    private var lastLoadTime: Date? = null
    private val cacheValidityDuration = 30_000L // 30 seconds

    fun startOwnPresenceTracking(userId: String) {
        presenceManager.startTracking(userId)
    }

    private fun observePresenceForParticipants(ids: List<String>) {
        val toCancel = presenceJobs.keys - ids.toSet()
        toCancel.forEach { presenceJobs.remove(it)?.cancel() }
        ids.forEach { id ->
            if (id !in presenceJobs) {
                presenceJobs[id] = viewModelScope.launch {
                    presenceManager.observeUserStatus(id).collect { isOnline ->
                        _presenceStatuses.update { it + (id to isOnline) }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        presenceJobs.values.forEach { it.cancel() }
    }
    
    /**
     * Load chat participants (excluding current user)
     * 
     * Process:
     * 1. Get participant IDs from project (teamMembers, managerId, tempApproverID)
     * 2. Remove current user from list
     * 3. Add BusinessHead if current user is not BusinessHead
     * 4. Fetch user details in parallel
     * 5. Fetch chat data (unread count, last message) for each participant
     * 6. Sort participants (BusinessHead first, then by role, then by name)
     */
    fun loadChatParticipants(project: Project, currentUserPhone: String?, currentUserRole: UserRole) {
        viewModelScope.launch {
            // Always fetch fresh data so unread counts are accurate
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                var currentUserDepartment: String? = ""
                // Step 1: Collect participant IDs
                val participantIds = mutableSetOf<String>()

                if (currentUserRole != UserRole.BUSINESS_HEAD){
                    if (currentUserRole == UserRole.USER){
                        currentUserDepartment = project.departmentUserAssignments.filter{it.value == currentUserPhone}.keys.firstOrNull()
                    }else{
                        currentUserDepartment = project.departmentApproverAssignments.filter{it.value == currentUserPhone}.keys.firstOrNull()

                        if (currentUserDepartment == null){
                            currentUserDepartment = project.departmentTemporaryApprover.filter{it.phone == currentUserPhone}.mapNotNull { it.departmentName }.firstOrNull()
                        }
                    }
                    currentUserDepartment?.let { department ->

                        participantIds.addAll(
                            project.departmentUserAssignments
                                .filter {
                                    it.key == department &&
                                            it.value != currentUserPhone
                                }
                                .mapNotNull { it.value }
                        )
                        participantIds.addAll(
                            project.departmentApproverAssignments
                                .filter {
                                    it.key == department &&
                                            it.value != currentUserPhone
                                }
                                .mapNotNull { it.value }
                        )
                        val currentDate = Date()
                        participantIds.addAll(
                            project.departmentTemporaryApprover
                                .filter {
                                    it.isActive &&
                                            it.isAccepted &&
                                            it.startDate != null &&
                                            it.endDate != null &&
                                            it.startDate <= currentDate &&
                                            it.endDate >= currentDate &&
                                            it.departmentName == currentUserDepartment &&
                                            it.phone != currentUserPhone
                                }
                                .mapNotNull { it.phone }
                        )
                    }
                }else{
                    // Add team members
                    participantIds.addAll(project.departmentUserAssignments
                        .filter {
                            it.value != currentUserPhone
                        }
                        .mapNotNull { it.value }
                    )

                    // Add manager
                    participantIds.addAll(project.departmentApproverAssignments
                        .filter {
                            it.value != currentUserPhone
                        }
                        .mapNotNull { it.value }
                    )

                    // Add temp approver if active
                    val currentDate = Date()

                    participantIds.addAll(
                        project.departmentTemporaryApprover
                            .filter {
                                it.isActive &&
                                        it.isAccepted &&
                                        it.startDate != null &&
                                        it.endDate != null &&
                                        it.startDate <= currentDate &&
                                        it.endDate >= currentDate &&
                                        it.phone != currentUserPhone
                            }
                            .mapNotNull { it.phone }
                    )
                }


//                // Step 2: Remove current user
//                currentUserPhone?.let { participantIds.remove(it) }
                
                // Step 3: Show basic UI first (BusinessHead if needed)
                val basicParticipants = mutableListOf<ChatParticipant>()
                if (currentUserRole != UserRole.BUSINESS_HEAD) {
                    basicParticipants.add(
                        ChatParticipant(
                            id = "BusinessHead",
                            name = "BusinessHead",
                            phoneNumber = "BusinessHead",
                            role = UserRole.BUSINESS_HEAD,
                            isOnline = true,
                            lastSeen = null,
                            unreadCount = 0,
                            lastMessage = null,
                            lastMessageTime = null
                        )
                    )
                }
                _participants.value = basicParticipants
                _isLoading.value = false
                
                // Step 4: Fetch detailed participants in parallel
                val detailedParticipants = participantIds.map { participantId ->
                    async {
                        fetchParticipantDetails(
                            participantId = participantId,
                            projectId = project.id ?: return@async null,
                            currentUserPhone = currentUserPhone ?: "BusinessHead"
                        )
                    }
                }.awaitAll().filterNotNull()
                
                // Step 5: Fetch chat data for BusinessHead if needed
                val finalParticipants = if (currentUserRole != UserRole.BUSINESS_HEAD) {
                    val businessHeadChatData = fetchChatData(
                        participantId = "BusinessHead",
                        projectId = project.id ?: return@launch,
                        currentUserPhone = currentUserPhone ?: "BusinessHead"
                    )
                    
                    val businessHeadParticipant = ChatParticipant(
                        id = "BusinessHead",
                        name = "BusinessHead",
                        phoneNumber = "BusinessHead",
                        role = UserRole.BUSINESS_HEAD,
                        isOnline = true,
                        lastSeen = null,
                        unreadCount = businessHeadChatData.unreadCount,
                        lastMessage = businessHeadChatData.lastMessage,
                        lastMessageTime = businessHeadChatData.lastMessageTime
                    )
                    
                    detailedParticipants + businessHeadParticipant
                } else {
                    detailedParticipants
                }
                
                // Step 6: Sort participants
                val sortedParticipants = finalParticipants.sortedWith(
                    compareBy<ChatParticipant> { participant ->
                        when (participant.role) {
                            UserRole.BUSINESS_HEAD -> 0
                            UserRole.APPROVER -> 1
                            UserRole.USER -> 2
                            else -> 3
                        }
                    }.thenBy { it.name }
                )
                
                // Update UI and cache
                _participants.value = sortedParticipants
                cachedParticipants = sortedParticipants
                lastLoadTime = Date()

                // Start real-time presence observation for all loaded participants
                observePresenceForParticipants(sortedParticipants.map { it.phoneNumber })
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load participants: ${e.message}"
                Log.e("ChatsViewModel", "Error loading participants: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Fetch participant details from users collection
     */
    private suspend fun fetchParticipantDetails(
        participantId: String,
        projectId: String,
        currentUserPhone: String
    ): ChatParticipant? {
        return try {
            // Fetch user from users collection
            val userSnapshot = firestore
                .collection("users")
                .whereEqualTo("phoneNumber", participantId)
                .limit(1)
                .get()
                .await()
            
            val userDoc = userSnapshot.documents.firstOrNull()
            if (userDoc == null) {
                Log.w("ChatsViewModel", "User not found: $participantId")
                return null
            }
            
            val userData = userDoc.data
            if (userData == null) {
                Log.w("ChatsViewModel", "User data is null for: $participantId")
                return null
            }
            
            val name = userData["name"] as? String ?: ""
            val phoneNumber = userData["phoneNumber"] as? String ?: participantId
            val roleString = userData["role"] as? String ?: "USER"
            val role = when (roleString.uppercase().replace(" ", "_")) {
                "BUSINESS_HEAD" -> UserRole.BUSINESS_HEAD
                "APPROVER" -> UserRole.APPROVER
                "ADMIN" -> UserRole.ADMIN
                else -> UserRole.USER
            }
            
            // Fetch chat data (unread count, last message)
            val chatData = fetchChatData(participantId, projectId, currentUserPhone)
            
            ChatParticipant(
                id = phoneNumber,
                name = name,
                phoneNumber = phoneNumber,
                role = role,
                isOnline = false, // TODO: Implement real online status
                lastSeen = null, // TODO: Implement real last seen
                unreadCount = chatData.unreadCount,
                lastMessage = chatData.lastMessage,
                lastMessageTime = chatData.lastMessageTime
            )
        } catch (e: Exception) {
            Log.e("ChatsViewModel", "Error fetching participant: ${e.message}", e)
            null
        }
    }
    
    /**
     * Fetch chat data (unread count, last message, last message time)
     * iOS approach: query messages where isRead==false, then filter senderId client-side.
     */
    private suspend fun fetchChatData(
        participantId: String,
        projectId: String,
        currentUserPhone: String
    ): ChatData {
        return try {
            val chatId = generateChatId(currentUserPhone, participantId)
            val customerId = projectRepository.findCustomerIdForProject(projectId)
                ?: return ChatData(0, null, null)

            val messagesRef = firestore
                .collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("chats")
                .document(chatId)
                .collection("messages")

            // iOS approach: query isRead==false (single field, no composite index),
            // then filter senderId != currentUserPhone client-side to count unread messages.
            Log.d("ChatsViewModel", "Querying unread messages for chatId: $chatId at path: customers/$customerId/projects/$projectId/chats/$chatId/messages")
            val unreadMessages = messagesRef
                .whereEqualTo("isRead", false)
                .get()
                .await()

            Log.d("ChatsViewModel", "Found ${unreadMessages.documents.size} messages with isRead=false in chatId: $chatId")

            // Normalize current user phone for comparison (handle +91 prefix)
            val normalizedCurrentPhone = currentUserPhone.removePrefix("+91").trim()
            var unreadCount = 0
            
            unreadMessages.documents.forEach { doc ->
                val senderId = doc.getString("senderId") ?: ""
                val normalizedSender = senderId.removePrefix("+91").trim()
                val isFromOther = normalizedSender != normalizedCurrentPhone && senderId != currentUserPhone
                
                Log.d("ChatsViewModel", "Message ${doc.id}: senderId=$senderId, normalizedSender=$normalizedSender, normalizedCurrentPhone=$normalizedCurrentPhone. isFromOther=$isFromOther")
                
                if (isFromOther) {
                    unreadCount++
                }
            }
            
            Log.d("ChatsViewModel", "Final unread count for $chatId: $unreadCount")

            // Fetch last message info from chat document
            val chatDoc = firestore
                .collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("chats")
                .document(chatId)
                .get()
                .await()

            val data = chatDoc.data
            val lastMessage = data?.get("lastMessage") as? String
            val lastTimestamp = (data?.get("lastMessageTime") as? Timestamp)
                ?: (data?.get("lastTimestamp") as? Timestamp)

            ChatData(
                unreadCount = unreadCount,
                lastMessage = lastMessage,
                lastMessageTime = lastTimestamp?.toDate()
            )
        } catch (e: Exception) {
            Log.e("ChatsViewModel", "Error fetching chat data: ${e.message}", e)
            ChatData(0, null, null)
        }
    }

    /**
     * iOS-matching unread count: query messages where isRead==false, filter senderId client-side.
     * Returns 0 on error.
     */
    private suspend fun countUnreadMessages(
        projectId: String,
        chatId: String,
        currentUserPhone: String,
        customerId: String
    ): Int {
        return try {
            val allUnread = firestore
                .collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val normalizedCurrentPhone = currentUserPhone.removePrefix("+91").trim()
            allUnread.documents.count { doc ->
                val senderId = doc.getString("senderId") ?: ""
                val normalizedSender = senderId.removePrefix("+91").trim()
                normalizedSender != normalizedCurrentPhone && senderId != currentUserPhone
            }
        } catch (e: Exception) {
            Log.e("ChatsViewModel", "Error counting unread messages: ${e.message}", e)
            0
        }
    }
    
    /**
     * Fetch valid temp approver
     */
    private suspend fun fetchValidTempApprover(
        projectId: String,
        approverId: String
    ): String? {
        return try {
            val customerId = projectRepository.findCustomerIdForProject(projectId)
                ?: return null
            val currentDate = Date()
            
            val snapshot = firestore
                .collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("tempApprover")
                .whereEqualTo("approverId", approverId)
                .whereEqualTo("status", "active")
                .whereGreaterThanOrEqualTo("endDate", Timestamp(currentDate))
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.let { doc ->
                val data = doc.data ?: return@let null
                val startTime = data["startDate"] as? Timestamp
                val endTime = data["endDate"] as? Timestamp
                
                if (startTime != null && endTime != null) {
                    val start = startTime.toDate()
                    val end = endTime.toDate()
                    if (start <= currentDate && end >= currentDate) {
                        approverId
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ChatsViewModel", "Error fetching temp approver: ${e.message}", e)
            null
        }
    }
    
    /**
     * Update unread count for a specific participant
     */
    fun updateUnreadCountForParticipant(participantId: String, projectId: String, currentUserPhone: String) {
        // Immediately zero out the badge in the UI (optimistic update)
        val immediateList = _participants.value.map { participant ->
            if (participant.phoneNumber == participantId) {
                participant.copy(unreadCount = 0)
            } else {
                participant
            }
        }
        _participants.value = immediateList
        cachedParticipants = immediateList

        // After chat is opened, re-confirm count from Firestore (should be 0 after marking as read)
        viewModelScope.launch {
            try {
                val chatId = generateChatId(currentUserPhone, participantId)
                val customerId = projectRepository.findCustomerIdForProject(projectId)
                    ?: return@launch

                val unreadCount = countUnreadMessages(projectId, chatId, currentUserPhone, customerId)

                val confirmedList = _participants.value.map { participant ->
                    if (participant.phoneNumber == participantId) {
                        participant.copy(unreadCount = unreadCount)
                    } else {
                        participant
                    }
                }

                _participants.value = confirmedList
                cachedParticipants = confirmedList

            } catch (e: Exception) {
                Log.e("ChatsViewModel", "Error updating unread count: ${e.message}", e)
            }
        }
    }
    
    /**
     * Mark messages as read for a participant
     */
    fun markMessagesAsRead(participantId: String, projectId: String, currentUserPhone: String) {
        viewModelScope.launch {
            try {
                val chatId = generateChatId(currentUserPhone, participantId)
                val customerId = projectRepository.findCustomerIdForProject(projectId)
                    ?: return@launch
                
                val snapshot = firestore
                    .collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(projectId)
                    .collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .whereNotEqualTo("senderId", currentUserPhone)
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()
                
                // Update all unread messages
                snapshot.documents.forEach { doc ->
                    doc.reference.update("isRead", true).await()
                }
                
                // Update unread count
                updateUnreadCountForParticipant(participantId, projectId, currentUserPhone)
                
            } catch (e: Exception) {
                Log.e("ChatsViewModel", "Error marking messages as read: ${e.message}", e)
            }
        }
    }
    
    /**
     * Refresh data (clear cache and reload)
     */
    fun refreshData(project: Project, currentUserPhone: String?, currentUserRole: UserRole) {
        viewModelScope.launch {
            _isRefreshing.value = true
            clearCache()
            loadChatParticipants(project, currentUserPhone, currentUserRole)
            _isRefreshing.value = false
        }
    }
    
    /**
     * Generate chat ID for individual chat
     */
    private fun generateChatId(participant1: String, participant2: String): String {
        val normalized1 = normalizeParticipantId(participant1)
        val normalized2 = normalizeParticipantId(participant2)
        return listOf(normalized1, normalized2).sorted().joinToString("_")
    }
    
    /**
     * Normalize participant ID (remove +91 prefix, handle BusinessHead)
     * Returns "BusinessHead" for document ID format: number_BusinessHead
     */
    private fun normalizeParticipantId(id: String): String {
        return if (id == "BusinessHead" || id == "BUSINESS_HEAD" || id == "123") {
            "BusinessHead"
        } else {
            id.removePrefix("+91").trim()
        }
    }
    
    /**
     * Check if cache is valid
     */
    private fun isCacheValid(): Boolean {
        val lastLoad = lastLoadTime ?: return false
        val now = Date()
        return (now.time - lastLoad.time) < cacheValidityDuration && cachedParticipants.isNotEmpty()
    }
    
    /**
     * Clear cache
     */
    private fun clearCache() {
        cachedParticipants = emptyList()
        lastLoadTime = null
    }
    
    /**
     * Chat data holder
     */
    private data class ChatData(
        val unreadCount: Int,
        val lastMessage: String?,
        val lastMessageTime: Date?
    )
}
