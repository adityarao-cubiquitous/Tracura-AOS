package com.cubiquitous.tracura.repository

import com.cubiquitous.tracura.model.Chat
import com.cubiquitous.tracura.model.ChatMember
import com.cubiquitous.tracura.model.Message
import com.cubiquitous.tracura.model.Notification
import com.cubiquitous.tracura.model.NotificationType
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import com.cubiquitous.tracura.utils.NotificationLocalStorageManager

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
//    private val notificationRepository: NotificationRepository,
    private val projectRepository: ProjectRepository,
    @ApplicationContext private val context: Context
) {
    private val usersCollection = firestore.collection("users")
    // Note: projectsCollection is deprecated - use projectRepository.getProjectDocument() instead
    private val projectsCollection = firestore.collection("projects")
    
    // Helper functions to get subcollections
    // These now work with the new structure: customers/{customerId}/Projects/{projectId}/chats
    private suspend fun getChatsCollection(projectId: String): com.google.firebase.firestore.CollectionReference {
        // Find customer ID for this project
        val customerId = projectRepository.findCustomerIdForProject(projectId)
        if (customerId != null) {
            return firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("chats")
        } else {
            // Fallback to old structure for backward compatibility
            return projectsCollection.document(projectId).collection("chats")
        }
    }
    
    private suspend fun getMessagesCollection(projectId: String, chatId: String): com.google.firebase.firestore.CollectionReference {
        // Find customer ID for this project
        val customerId = projectRepository.findCustomerIdForProject(projectId)
        if (customerId != null) {
            return firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("chats")
                .document(chatId)
                .collection("messages")
        } else {
            // Fallback to old structure for backward compatibility
            return projectsCollection.document(projectId).collection("chats").document(chatId).collection("messages")
        }
    }
    
    // Get expenseChats collection reference for expenses/{expenseId}/expenseChats
    private suspend fun getExpenseChatsCollection(projectId: String, expenseId: String): com.google.firebase.firestore.CollectionReference {
        // Find customer ID for this project
        val customerId = projectRepository.findCustomerIdForProject(projectId)
        if (customerId != null) {
            return firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("expenses")
                .document(expenseId)
                .collection("expenseChats")
        } else {
            // Fallback to old structure for backward compatibility
            return projectsCollection.document(projectId).collection("expenses").document(expenseId).collection("expenseChats")
        }
    }

    // Get all team members for a specific project
    suspend fun getProjectTeamMembers(projectId: String, currentUserId: String): List<ChatMember> {
        return try {
            
            // Get project from correct path: customers/{customerId}/Projects/{projectId}
            val projectDoc = projectRepository.getProjectDocument(projectId)
            if (projectDoc == null || !projectDoc.exists()) {
                return emptyList()
            }
            
            val projectData = projectDoc.toObject(Project::class.java)
            if (projectData == null) {
                return emptyList()
            }
            
            // Extract participants directly from departmentUserAssignments and departmentApproverAssignments
            val usersFromDepartments = projectData.departmentUserAssignments.values.distinct()
            val approversFromDepartments = projectData.departmentApproverAssignments.values.distinct()

            // Get current user's role and phone number to determine what to exclude
            var currentUserRole: UserRole? = null
            var currentUserPhone: String = currentUserId // Default to currentUserId (might be phone)
            var currentUserUid: String = currentUserId // Default to currentUserId (might be UID)

            try {
                // Try to get user by document ID first (UID)
                var currentUserDoc = usersCollection.document(currentUserId).get().await()
                var currentUserData = currentUserDoc.data

                // If not found by UID, try by phone number
                if (currentUserData == null || !currentUserDoc.exists()) {
                    val phoneQuery = usersCollection
                        .whereEqualTo("phoneNumber", currentUserId)
                        .limit(1)
                        .get()
                        .await()

                    if (!phoneQuery.isEmpty) {
                        currentUserDoc = phoneQuery.documents.first()
                        currentUserData = currentUserDoc.data
                        currentUserUid = currentUserDoc.id
                    } else {
                        // Try phone field
                        val phoneFieldQuery = usersCollection
                            .whereEqualTo("phone", currentUserId)
                            .limit(1)
                            .get()
                            .await()

                        if (!phoneFieldQuery.isEmpty) {
                            currentUserDoc = phoneFieldQuery.documents.first()
                            currentUserData = currentUserDoc.data
                            currentUserUid = currentUserDoc.id
                        }
                    }
                } else {
                    currentUserUid = currentUserDoc.id
                }

                if (currentUserData != null) {
                    val roleString = currentUserData["role"] as? String
                    currentUserRole = when (roleString?.uppercase()?.replace(" ", "_")) {
                        "APPROVER" -> UserRole.APPROVER
                        "BUSINESSHEAD" -> UserRole.BUSINESS_HEAD
                        "ADMIN" -> UserRole.ADMIN
                        else -> UserRole.USER
                    }
                    currentUserPhone = currentUserData["phoneNumber"] as? String ?: currentUserData["phone"] as? String ?: currentUserId
                }
            } catch (e: Exception) {
            }

            // Build participant set from assignment maps + businessHead (fetched by role below)
            val allMemberIds = mutableSetOf<String>()
            allMemberIds.addAll(usersFromDepartments)
            allMemberIds.addAll(approversFromDepartments)

            // Remove current user from the list
            allMemberIds.remove(currentUserId)
            // Also remove by phone in case currentUserId is phone
            allMemberIds.removeAll { it.replace("+91", "").trim() == currentUserPhone.replace("+91", "").trim() }
            
            
            // Fetch user details and filter by role
            val members = mutableListOf<ChatMember>()
            for (userId in allMemberIds) {
                try {
                    
                    // Try to get user by document ID first (UID)
                    var userDoc = usersCollection.document(userId).get().await()
                    var userData = userDoc.data
                    var actualUserId = userId
                    
                    // If not found by UID, try to find by phone number
                    if (userData == null || !userDoc.exists()) {
                        val phoneQuery = usersCollection
                            .whereEqualTo("phoneNumber", userId)
                            .limit(1)
                            .get()
                            .await()
                        
                        if (!phoneQuery.isEmpty) {
                            userDoc = phoneQuery.documents.first()
                            userData = userDoc.data
                            actualUserId = userDoc.id
                        } else {
                            // Try phone field as well
                            val phoneFieldQuery = usersCollection
                                .whereEqualTo("phone", userId)
                                .limit(1)
                                .get()
                                .await()
                            
                            if (!phoneFieldQuery.isEmpty) {
                                userDoc = phoneFieldQuery.documents.first()
                                userData = userDoc.data
                                actualUserId = userDoc.id
                            }
                        }
                    }
                    
                    if (userData != null && userDoc.exists()) {
                        val deviceInfoData = userData["deviceInfo"] as? Map<*, *>
                        val isOnline = deviceInfoData?.get("isOnline") as? Boolean ?: false
                        val lastLoginAt = deviceInfoData?.get("lastLoginAt") as? Long ?: 0L
                        
                        val roleString = userData["role"] as? String
                        val userRole = when (roleString?.uppercase()?.replace(" ", "_")) {
                            "APPROVER" -> UserRole.APPROVER
                            "BUSINESS_HEAD" -> UserRole.BUSINESS_HEAD
                            "ADMIN" -> UserRole.ADMIN
                            else -> UserRole.USER
                        }
                        
                        // Get user's phone number for proper ID matching
                        val userPhone = userData["phoneNumber"] as? String ?: userData["phone"] as? String ?: ""

                        // Check if this is the current user by comparing both UID and phone
                        val normalizedUserPhone = userPhone.replace("+91", "").trim()
                        val normalizedCurrentPhone = currentUserPhone.replace("+91", "").trim()
                        val isCurrentUser = (actualUserId == currentUserId) ||
                                          (userPhone == currentUserId) ||
                                          (userId == currentUserId) ||
                                          (normalizedUserPhone.isNotEmpty() && normalizedUserPhone == normalizedCurrentPhone)

                        if (isCurrentUser) {
                            continue
                        }
                        
                        // Use phone number as userId for chat operations (consistent with how chats are created)
                        val member = ChatMember(
                            userId = userPhone.ifEmpty { actualUserId }, // Prefer phone number for chat ID
                            name = userData["name"] as? String ?: "Unknown",
                            phone = userPhone,
                            role = userRole,
                            isOnline = isOnline,
                            lastSeen = lastLoginAt
                        )
                        
                        members.add(member)
                    } else {
                    }
                } catch (e: Exception) {
                }
            }
            
            // Add Production Head by role if not already in the list AND current user is NOT a Production Head
            if (currentUserRole != UserRole.BUSINESS_HEAD) {
                val BusinessHead = getBusinessHeadByRole()
                if (BusinessHead != null) {
                    val BusinessHeadPhone = BusinessHead.phone
                    val BusinessHeadUserId = BusinessHead.userId
                    
                    // Check if this Production Head is the current user by comparing both phone and UID
                    val isCurrentUser = (BusinessHeadPhone == currentUserPhone) || 
                                      (BusinessHeadPhone == currentUserId) ||
                                      (BusinessHeadUserId == currentUserUid) ||
                                      (BusinessHeadUserId == currentUserId)
                    
                    // Also check if already in the member list
                    val alreadyInList = allMemberIds.contains(BusinessHeadPhone) || 
                                      allMemberIds.contains(BusinessHeadUserId) ||
                                      members.any { it.phone == BusinessHeadPhone || it.userId == BusinessHeadUserId }
                    
                    if (!isCurrentUser && !alreadyInList) {
                        // Use phone number as userId for chat operations (consistent with how chats are created)
                        val BusinessHeadMember = BusinessHead.copy(
                            userId = BusinessHeadPhone.ifEmpty { BusinessHeadUserId }
                        )
                        members.add(BusinessHeadMember)
                    } else {
                        if (isCurrentUser) {
                        } else {
                        }
                    }
                }
            } else {
            }
            
            members.sortedBy { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Helper function to get the correct user ID for chat operations
    private suspend fun getChatUserId(userId: String): String {
        return try {
            // First try to get user by phone number
            val userByPhone = usersCollection.document(userId).get().await()
            if (userByPhone.exists()) {
                return userByPhone.id
            }
            
            // If not found, try to find by phone number in the document
            val usersByPhone = usersCollection
                .whereEqualTo("phoneNumber", userId)
                .get()
                .await()
            
            if (!usersByPhone.isEmpty) {
                val docId = usersByPhone.documents.first().id
                return docId
            }
            
            // If still not found, try the phone field
            val usersByPhoneField = usersCollection
                .whereEqualTo("phone", userId)
                .get()
                .await()
            
            if (!usersByPhoneField.isEmpty) {
                val docId = usersByPhoneField.documents.first().id
                return docId
            }
            
            userId // Return original if not found
        } catch (e: Exception) {
            userId
        }
    }
    
    /**
     * Get user phone number and role for chat document ID generation
     */
    private suspend fun getUserPhoneAndRole(userId: String): Pair<String, String> {
        return try {
            // Handle BusinessHead identifier directly - use "BusinessHead" for document ID
            if (userId == "BUSINESS_HEAD" || userId == "BusinessHead" || userId == "123") {
                return Pair("BusinessHead", "BUSINESS_HEAD")
            }
            
            // Try to get user by document ID first
            var userDoc = usersCollection.document(userId).get().await()
            var userData = userDoc.data
            
            // If not found, try by phone number
            if (userData == null || !userDoc.exists()) {
                val phoneQuery = usersCollection
                    .whereEqualTo("phoneNumber", userId)
                    .limit(1)
                    .get()
                    .await()
                
                if (!phoneQuery.isEmpty) {
                    userDoc = phoneQuery.documents.first()
                    userData = userDoc.data
                } else {
                    // Try phone field
                    val phoneFieldQuery = usersCollection
                        .whereEqualTo("phone", userId)
                        .limit(1)
                        .get()
                        .await()
                    
                    if (!phoneFieldQuery.isEmpty) {
                        userDoc = phoneFieldQuery.documents.first()
                        userData = userDoc.data
                    }
                }
            }
            
            if (userData != null && userDoc.exists()) {
                val phoneNumber = (userData["phoneNumber"] as? String ?: userData["phone"] as? String ?: userId).replace("+91", "").trim()
                val role = (userData["role"] as? String ?: "USER").uppercase()
                return Pair(phoneNumber, role)
            }
            
            // Fallback: assume userId is phone number
            Pair(userId.replace("+91", "").trim(), "USER")
        } catch (e: Exception) {
            // Fallback: assume userId is phone number
            Pair(userId.replace("+91", "").trim(), "USER")
        }
    }
    
    /**
     * Generate chat document ID based on user roles and phone numbers
     * Format (as per screenshot):
     * - phoneNumber_BusinessHead - for chats between user and production head
     * - phoneNumber_Admin - for chats between user and admin
     * - phoneNumber_phoneNumber - for chats between two users
     * - group_<uniqueId> - for group chats
     */
    private suspend fun generateChatDocumentId(
        currentUserId: String,
        otherUserId: String,
        isGroupChat: Boolean = false
    ): String {
        return try {
            if (isGroupChat) {
                // For group chats, use format: group_<uniqueId>
                // Generate unique alphanumeric ID (similar to screenshot: group_dc95v6I4SIDiuGTU2veh)
                val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                val uniqueId = (1..20).map { chars.random() }.joinToString("")
                return "group_$uniqueId"
            }
            
            // Get phone numbers and roles for both users
            val (currentPhone, currentRole) = getUserPhoneAndRole(currentUserId)
            val (otherPhone, otherRole) = getUserPhoneAndRole(otherUserId)
            
            
            // Determine which user is the regular user and which is production head/admin
            val isCurrentUserPH = currentRole == "BUSINESS_HEAD"
            val isCurrentUserAdmin = currentRole == "ADMIN"
            val isOtherUserPH = otherRole == "BUSINESS_HEAD"
            val isOtherUserAdmin = otherRole == "ADMIN"
            
            // Use pattern: if role is BUSINESS_HEAD, use "BusinessHead" for document ID, otherwise use phone number
            val currentIdentifier = if (currentRole == "BUSINESS_HEAD") "BusinessHead" else currentPhone
            val otherIdentifier = if (otherRole == "BUSINESS_HEAD") "BusinessHead" else otherPhone
            
            // Sort identifiers to ensure consistent chat ID regardless of who initiates
            val sortedIdentifiers = listOf(currentIdentifier, otherIdentifier).sorted()
            val chatId = "${sortedIdentifiers[0]}_${sortedIdentifiers[1]}"
            return chatId
        } catch (e: Exception) {
            // Fallback: use sorted phone numbers
            val sortedPhones = listOf(currentUserId, otherUserId).sorted()
            "${sortedPhones[0]}_${sortedPhones[1]}"
        }
    }

    // Get expense details from chat ID (for expense approval chats)
    private suspend fun getExpenseFromChatId(projectId: String, chatId: String): com.cubiquitous.tracura.model.Expense? {
        return try {
            // Extract expense ID from chat ID (format: "expense_approval_{expenseId}")
            if (!chatId.startsWith("expense_approval_")) {
                return null
            }
            
            val expenseId = chatId.removePrefix("expense_approval_")
            
            // Get expense from Firestore - try new structure first
            // Find customer ID for this project
            val customerId = projectRepository.findCustomerIdForProject(projectId)
            val expenseDoc = if (customerId != null) {
                // Try new structure: customers/{customerId}/Projects/{projectId}/expenses
                firestore.collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(projectId)
                    .collection("expenses")
                    .document(expenseId)
                    .get()
                    .await()
            } else {
                // Fallback to old structure
                firestore.collection("projects")
                    .document(projectId)
                    .collection("expenses")
                    .document(expenseId)
                    .get()
                    .await()
            }
                
            if (!expenseDoc.exists()) {
                return null
            }
            
            // Map document to Expense object
            val expenseData = expenseDoc.data
            if (expenseData == null) {
                return null
            }
            
            // Handle date: can be String (new format) or Timestamp (old format)
            val dateString = when {
                expenseData["date"] is String -> expenseData["date"] as String
                expenseData["date"] is com.google.firebase.Timestamp -> {
                    val timestamp = expenseData["date"] as com.google.firebase.Timestamp
                    val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
                    dateFormat.format(timestamp.toDate())
                }
                else -> ""
            }
            
            // Handle categories: can be List<String> (new format) or String (old format)
            val categoriesList = when {
                expenseData["categories"] is List<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    (expenseData["categories"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                }
                expenseData["category"] is String -> {
                    val categoryStr = expenseData["category"] as? String ?: ""
                    if (categoryStr.isNotEmpty()) listOf(categoryStr) else emptyList()
                }
                else -> emptyList()
            }
            
            // Handle modeOfPayment: convert String to PaymentMode enum
            val modeOfPaymentStr = expenseData["modeOfPayment"] as? String ?: "cash"
            val modeOfPayment = com.cubiquitous.tracura.model.PaymentMode.fromString(modeOfPaymentStr)
            
            // Handle attachment fields: prefer new format, fallback to old
            val attachmentURL = expenseData["attachmentURL"] as? String 
                ?: expenseData["attachmentUrl"] as? String
            val attachmentName = expenseData["attachmentName"] as? String 
                ?: expenseData["attachmentFileName"] as? String
            
            // Handle payment proof fields: prefer new format, fallback to old
            val paymentProofURL = expenseData["paymentProofURL"] as? String 
                ?: expenseData["paymentProofUrl"] as? String
            val paymentProofName = expenseData["paymentProofName"] as? String 
                ?: expenseData["paymentProofFileName"] as? String
            
            // Handle submittedBy: prefer new format, fallback to userId
            val submittedBy = expenseData["submittedBy"] as? String 
                ?: expenseData["userId"] as? String ?: ""
            
            // Handle status
            val status = when (expenseData["status"] as? String) {
                "APPROVED" -> com.cubiquitous.tracura.model.ExpenseStatus.APPROVED
                "REJECTED" -> com.cubiquitous.tracura.model.ExpenseStatus.REJECTED
                else -> com.cubiquitous.tracura.model.ExpenseStatus.PENDING
            }
            
            // Handle timestamps: prefer createdAt/updatedAt, fallback to submittedAt
            val createdAt = expenseData["createdAt"] as? com.google.firebase.Timestamp 
                ?: expenseData["submittedAt"] as? com.google.firebase.Timestamp 
                ?: com.google.firebase.Timestamp.now()
            val updatedAt = expenseData["updatedAt"] as? com.google.firebase.Timestamp 
                ?: expenseData["reviewedAt"] as? com.google.firebase.Timestamp 
                ?: createdAt
            
            val expense = com.cubiquitous.tracura.model.Expense(
                id = expenseDoc.id,
                projectId = projectId,
                date = dateString,
                amount = (expenseData["amount"] as? Number)?.toDouble() ?: 0.0,
                department = expenseData["department"] as? String ?: "",
                phaseId = expenseData["phaseId"] as? String,
                phaseName = expenseData["phaseName"] as? String,
                categories = categoriesList,
                modeOfPayment = modeOfPayment,
                description = expenseData["description"] as? String ?: "",
                attachmentURL = attachmentURL,
                attachmentName = attachmentName,
                paymentProofURL = paymentProofURL,
                paymentProofName = paymentProofName,
                submittedBy = submittedBy,
                status = status,
                remark = expenseData["remark"] as? String,
                isAdmin = expenseData["isAdmin"] as? Boolean ?: false,
                approvedBy = expenseData["approvedBy"] as? String,
                rejectedBy = expenseData["rejectedBy"] as? String ?: "",
                
                // Material Details
                itemType = expenseData["itemType"] as? String,
                item = expenseData["item"] as? String,
                brand = expenseData["brand"] as? String,
                spec = expenseData["spec"] as? String,
                thickness = expenseData["thickness"] as? String,
                quantity = expenseData["quantity"] as? String,
                uom = expenseData["uom"] as? String,
                unitPrice = expenseData["unitPrice"] as? String,
                
                // Anonymous Department Tracking
                isAnonymous = expenseData["isAnonymous"] as? Boolean,
                originalDepartment = expenseData["originalDepartment"] as? String,
                departmentDeletedAt = expenseData["departmentDeletedAt"] as? com.google.firebase.Timestamp,
                
                // Firestore Timestamps
                createdAt = createdAt,
                updatedAt = updatedAt,
                
                // Legacy fields for backward compatibility
                userId = expenseData["userId"] as? String ?: "",
                userName = expenseData["userName"] as? String ?: "",
                attachmentUrl = attachmentURL ?: "",
                attachmentFileName = attachmentName ?: "",
                paymentProofUrl = paymentProofURL ?: "",
                paymentProofFileName = paymentProofName ?: "",
                submittedAt = createdAt,
                reviewedAt = expenseData["reviewedAt"] as? com.google.firebase.Timestamp,
                reviewedBy = expenseData["reviewedBy"] as? String ?: "",
                reviewComments = expenseData["reviewComments"] as? String ?: "",
                receiptNumber = expenseData["receiptNumber"] as? String ?: "",
                budgetExceeded = expenseData["budgetExceeded"] as? Boolean ?: false
            )
            
            expense
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Get all approvers for a project (for expense approval chats)
    private suspend fun getApproversForExpenseChat(projectId: String, chatId: String): List<String> {
        return try {
            
            val chatsCollection = getChatsCollection(projectId)
            val messagesCollection = getMessagesCollection(projectId, chatId)
            
            // First check if chat already exists and has messages from approvers
            val existingChatDoc = chatsCollection.document(chatId).get().await()
            if (existingChatDoc.exists()) {
                
                // Get all messages in this chat
                val messages = messagesCollection.get().await()
                
                // Find unique sender IDs who are approvers (have sent messages in this chat)
                val approverSenders = mutableSetOf<String>()
                for (messageDoc in messages.documents) {
                    val senderId = messageDoc.get("senderId") as? String
                    if (senderId != null) {
                        try {
                            // Check if this sender is an approver
                            val userDoc = usersCollection.document(senderId).get().await()
                            val role = userDoc.get("role") as? String
                            if (role != null && (role.uppercase().contains("APPROVER") || role.uppercase().contains("PRODUCTION"))) {
                                approverSenders.add(senderId)
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
                
                // If there are approvers who have actively participated, only notify them
                if (approverSenders.isNotEmpty()) {
                    return approverSenders.toList()
                }
                
            }
            
            // If chat doesn't exist or no approvers have participated yet, get all project participants
            
            // Get project details from correct path: customers/{customerId}/Projects/{projectId}
            val projectDoc = projectRepository.getProjectDocument(projectId)
            if (projectDoc == null || !projectDoc.exists()) {
                return emptyList()
            }
            
            val approvers = mutableSetOf<String>()
            
            // Get participants from departmentApproverAssignments (Map<DepartmentName, ApproverPhone>)
            val departmentApproverAssignments = projectDoc.get("departmentApproverAssignments") as? Map<*, *>
            departmentApproverAssignments?.values?.filterIsInstance<String>()?.filter { it.isNotEmpty() }?.forEach { approverPhone ->
                approvers.add(approverPhone)
            }
            
            // Get participants from departmentUserAssignments (Map<DepartmentName, UserPhone>)
            val departmentUserAssignments = projectDoc.get("departmentUserAssignments") as? Map<*, *>
            departmentUserAssignments?.values?.filterIsInstance<String>()?.filter { it.isNotEmpty() }?.forEach { userPhone ->
                approvers.add(userPhone)
            }
            
            // Add BusinessHead by role
            val businessHead = getBusinessHeadByRole()
            if (businessHead != null && businessHead.phone.isNotEmpty()) {
                approvers.add(businessHead.phone)
            }
            
            approvers.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Get all project members for group chat (includes ALL members regardless of current user role)
    private suspend fun getAllProjectMembersForGroupChat(projectId: String, currentUserId: String): List<String> {
        return try {
            
            // Get project from correct path: customers/{customerId}/projects/{projectId}
            val projectDoc = projectRepository.getProjectDocument(projectId)
            if (projectDoc == null || !projectDoc.exists()) {
                return emptyList()
            }
            
            val projectData = projectDoc.toObject(Project::class.java)
            if (projectData == null) {
                return emptyList()
            }
            
            // Get raw document data
            val rawProjectData = projectDoc.data
            
            // NEW: Extract approvers from departmentApproverAssignments (Map<DepartmentName, ApproverPhone>)
            val departmentApproverAssignments = projectData.departmentApproverAssignments
            val approversFromDepartments = departmentApproverAssignments.values.distinct()
            
            // NEW: Extract users from departmentUserAssignments (Map<DepartmentName, UserPhone>)
            val departmentUserAssignments = projectData.departmentUserAssignments
            val usersFromDepartments = departmentUserAssignments.values.distinct()
            
            // LEGACY: Get all member IDs (don't filter by current user role for group chat)
            val approverIds = projectData.approverIds ?: emptyList()
            val BusinessHeadIds = projectData.BusinessHeadIds ?: emptyList()
            val teamMembers = projectData.teamMembers
            val managerId = projectData.managerId ?: ""

            // Combine all approvers/managers (prefer new departmentApproverAssignments, fallback to legacy)
            val allApproverIds = if (approversFromDepartments.isNotEmpty()) {
                approversFromDepartments
            } else {
                (listOfNotNull(managerId.takeIf { it.isNotEmpty() }) + approverIds).distinct()
            }
            
            val allMemberIds = mutableSetOf<String>()
            
            // Add ALL approvers/managers (for group chat, include everyone)
            allMemberIds.addAll(allApproverIds)
            
            // Add ALL production heads (for group chat, include everyone)
            allMemberIds.addAll(BusinessHeadIds)
            
            // Add ALL team members/users (for group chat, include everyone)
            // Prefer new departmentUserAssignments, fallback to legacy teamMembers
            val teamMembersToAdd = if (usersFromDepartments.isNotEmpty()) {
                usersFromDepartments
            } else {
                teamMembers
            }
            allMemberIds.addAll(teamMembersToAdd)
            
            // Add manager if exists
            if (managerId.isNotEmpty()) {
                allMemberIds.add(managerId)
            }
            
            // Remove current user from the list (they shouldn't see themselves in group chat members)
            allMemberIds.remove(currentUserId)
            
            
            // Convert to phone numbers for consistency with chat operations
            val memberPhones = mutableListOf<String>()
            for (memberId in allMemberIds) {
                try {
                    // Try to get user by document ID first (UID)
                    var userDoc = usersCollection.document(memberId).get().await()
                    var userData = userDoc.data
                    
                    // If not found by UID, try by phone number
                    if (userData == null || !userDoc.exists()) {
                        val phoneQuery = usersCollection
                            .whereEqualTo("phoneNumber", memberId)
                            .limit(1)
                            .get()
                            .await()
                        
                        if (!phoneQuery.isEmpty) {
                            userDoc = phoneQuery.documents.first()
                            userData = userDoc.data
                        } else {
                            val phoneFieldQuery = usersCollection
                                .whereEqualTo("phone", memberId)
                                .limit(1)
                                .get()
                                .await()
                            
                            if (!phoneFieldQuery.isEmpty) {
                                userDoc = phoneFieldQuery.documents.first()
                                userData = userDoc.data
                            }
                        }
                    }
                    
                    if (userData != null) {
                        val phone = userData["phoneNumber"] as? String ?: userData["phone"] as? String ?: memberId
                        if (phone.isNotEmpty()) {
                            memberPhones.add(phone)
                        } else {
                            // If no phone, use the memberId as is
                            memberPhones.add(memberId)
                        }
                    } else {
                        // If user not found, use memberId as is
                        memberPhones.add(memberId)
                    }
                } catch (e: Exception) {
                    // Use memberId as fallback
                    memberPhones.add(memberId)
                }
            }
            
            // Add current user's phone number
            try {
                val currentUserDoc = usersCollection.document(currentUserId).get().await()
                val currentUserData = currentUserDoc.data
                if (currentUserData == null || !currentUserDoc.exists()) {
                    // Try by phone
                    val phoneQuery = usersCollection
                        .whereEqualTo("phoneNumber", currentUserId)
                        .limit(1)
                        .get()
                        .await()
                    
                    if (!phoneQuery.isEmpty) {
                        val phone = phoneQuery.documents.first().data?.get("phoneNumber") as? String ?: 
                                   phoneQuery.documents.first().data?.get("phone") as? String ?: currentUserId
                        memberPhones.add(phone)
                    } else {
                        memberPhones.add(currentUserId)
                    }
                } else {
                    val currentUserPhone = currentUserData["phoneNumber"] as? String ?: 
                                         currentUserData["phone"] as? String ?: currentUserId
                    memberPhones.add(currentUserPhone)
                }
            } catch (e: Exception) {
                memberPhones.add(currentUserId)
            }
            
            memberPhones.distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Get or create a group chat for a project (includes all users and approvers)
    suspend fun getOrCreateGroupChat(projectId: String, currentUserId: String): String {
        return try {
            val chatsCollection = getChatsCollection(projectId)
            
            // Generate group chat document ID in format: group_<uniqueId>
            // Generate unique alphanumeric ID (similar to screenshot: group_dc95v6I4SIDiuGTU2veh)
            // Use lowercase letters and numbers for Firebase-like appearance
            val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
            val uniqueId = (1..20).map { chars.random() }.joinToString("")
            val groupChatId = "group_$uniqueId"
            
            
            // Check if group chat already exists for this project
            // Look for existing group chats for this project
            val existingGroupChats = chatsCollection
                .whereEqualTo("isGroupChat", true)
                .get()
                .await()
            
            // Find group chat that contains current user and is for this project
            val existingGroupChat = existingGroupChats.documents.firstOrNull { doc ->
                val participants = (doc.get("participants") as? List<*>)?.filterIsInstance<String>() 
                    ?: (doc.get("members") as? List<*>)?.filterIsInstance<String>() // Backward compatibility
                    ?: emptyList()
                val currentChatUserId = getChatUserId(currentUserId)
                participants.contains(currentChatUserId)
            }
            
            if (existingGroupChat != null) {
                val existingChatId = existingGroupChat.id
                
                // Update participants list to ensure all current project members are included
                val currentParticipants = (existingGroupChat.get("participants") as? List<*>)?.filterIsInstance<String>()?.toMutableSet() 
                    ?: (existingGroupChat.get("members") as? List<*>)?.filterIsInstance<String>()?.toMutableSet() // Backward compatibility
                    ?: mutableSetOf()
                val allMemberIds = getAllProjectMembersForGroupChat(projectId, currentUserId).toSet()
                
                // Add any missing participants
                val participantsUpdated = allMemberIds.any { !currentParticipants.contains(it) }
                if (participantsUpdated) {
                    val updatedParticipants = (currentParticipants + allMemberIds).toList()
                    chatsCollection.document(existingChatId).update(
                        mapOf(
                            "participants" to updatedParticipants,
                            "updatedAt" to Timestamp.now()
                        )
                    ).await()
                }
                
                // If existing chat doesn't follow new format, migrate it
                if (!existingChatId.startsWith("group_")) {
                    val chatData = existingGroupChat.data?.toMutableMap() ?: mutableMapOf()
                    chatsCollection.document(groupChatId).set(chatData).await()
                    // Optionally delete old document (commented out for safety)
                    // existingGroupChat.reference.delete().await()
                    return groupChatId
                }
                
                return existingChatId
            }
            
            // Create new group chat
            
            // Get all project members (users and approvers) - includes everyone for group chat
            val allMemberIds = getAllProjectMembersForGroupChat(projectId, currentUserId)
            
            
            if (allMemberIds.isEmpty()) {
                return ""
            }
            
            // Create unread count map for all participants
            val initialUnreadCount = allMemberIds.associateWith { 0 }
            
            val groupChatData = hashMapOf(
                "participants" to allMemberIds,
                "lastMessage" to "",
                "lastMessageTime" to Timestamp.now(),
                "lastMessageSenderId" to "",
                "unreadCount" to initialUnreadCount,
                "isGroupChat" to true, // Flag to identify group chats
                "projectId" to projectId, // Store projectId for reference
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
            
            chatsCollection.document(groupChatId).set(groupChatData).await()
            
            groupChatId
        } catch (e: Exception) {
            ""
        }
    }
    
    // Get or create a chat between two users for a specific project
    suspend fun getOrCreateChat(projectId: String, currentUserId: String, otherUserId: String): String {
        return try {
            val chatsCollection = getChatsCollection(projectId)
            
            // Get the correct user IDs for chat operations
            val currentChatUserId = getChatUserId(currentUserId)
            val otherChatUserId = getChatUserId(otherUserId)
            
            // Generate chat document ID based on format: phoneNumber_BusinessHead, phoneNumber_phoneNumber, etc.
            val chatDocumentId = generateChatDocumentId(currentUserId, otherUserId, isGroupChat = false)
            
            
            // Check if chat already exists with the generated document ID
            val existingChatDoc = chatsCollection.document(chatDocumentId).get().await()
            
            if (existingChatDoc.exists()) {
                
                // Verify it contains both users
                val participants = (existingChatDoc.get("participants") as? List<*>)?.filterIsInstance<String>() 
                    ?: (existingChatDoc.get("members") as? List<*>)?.filterIsInstance<String>() // Backward compatibility
                    ?: emptyList()
                val containsBothUsers = participants.contains(currentChatUserId) && participants.contains(otherChatUserId)
                
                if (containsBothUsers) {
                    return chatDocumentId
                } else {
                }
            }
            
            // Also check for existing chats using old format (backward compatibility)
            val existingChats = chatsCollection
                .whereArrayContains("participants", currentChatUserId)
                .get()
                .await()
            
            // If no results, try with old "members" field for backward compatibility
            val existingChatsOld = if (existingChats.isEmpty) {
                chatsCollection
                    .whereArrayContains("members", currentChatUserId)
                    .get()
                    .await()
            } else {
                existingChats
            }
            
            
            val existingChat = existingChatsOld.documents.firstOrNull { doc ->
                val chatId = doc.id
                val isGroupChat = chatId.startsWith("group_") || 
                                 chatId == "group_chat_$projectId" ||
                                 (doc.get("isGroupChat") as? Boolean) == true
                
                // Skip group chats
                if (isGroupChat) {
                    return@firstOrNull false
                }
                
                val participants = (doc.get("participants") as? List<*>)?.filterIsInstance<String>() 
                    ?: (doc.get("members") as? List<*>)?.filterIsInstance<String>() // Backward compatibility
                    ?: emptyList()
                val containsOtherUser = participants.contains(otherChatUserId)
                val isPrivateChat = participants.size == 2 // Private chats should have exactly 2 participants
                
                
                // Only match if it's a private chat (2 participants) AND contains both users
                containsOtherUser && isPrivateChat
            }
            
            if (existingChat != null) {
                // Migrate to new format if needed
                if (existingChat.id != chatDocumentId) {
                    // Copy data to new document ID
                    val chatData = existingChat.data?.toMutableMap() ?: mutableMapOf()
                    chatsCollection.document(chatDocumentId).set(chatData).await()
                    // Optionally delete old document (commented out for safety)
                    // existingChat.reference.delete().await()
                }
                return chatDocumentId
            }
            
            // Create new PRIVATE chat with exactly 2 participants using the generated document ID
            val chatData = hashMapOf(
                "participants" to listOf(currentChatUserId, otherChatUserId),
                "lastMessage" to "",
                "lastMessageTime" to Timestamp.now(),
                "lastMessageSenderId" to "",
                "unreadCount" to mapOf(currentChatUserId to 0, otherChatUserId to 0),
                "isGroupChat" to false, // Explicitly mark as NOT a group chat
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
            
            chatsCollection.document(chatDocumentId).set(chatData).await()
            chatDocumentId
        } catch (e: Exception) {
            ""
        }
    }

    // Send a message
    suspend fun sendMessage(
        projectId: String,
        chatId: String, 
        senderId: String, 
        senderName: String, 
        senderRole: String, 
        message: String,
        messageType: String = "Text",
        mediaUrl: String? = null,
        context: android.content.Context? = null
    ): Boolean {
        // Wrap entire function in NonCancellable to ensure it completes,m
        return withContext(NonCancellable) {
            try {
            val isExpenseChat = chatId.startsWith("expense_approval_")
            val chatsCollection = getChatsCollection(projectId)
            
            // For expense chats, get expenseChats collection; for regular chats, get messages collection
            val expenseChatsCollection = if (isExpenseChat) {
                val expenseId = chatId.removePrefix("expense_approval_")
                if (expenseId.isNotEmpty()) {
                    getExpenseChatsCollection(projectId, expenseId)
                } else {
                    return@withContext false
                }
            } else {
                null
            }

            val messagesCollection = if (!isExpenseChat) {
                getMessagesCollection(projectId, chatId)
            } else {
                null
            }
            
            // OPTIMIZATION 1: Parallel data fetching - fetch chatDoc and projectDoc simultaneously
            val (chatDoc, projectDoc) = coroutineScope {
                val chatDocDeferred = async { chatsCollection.document(chatId).get().await() }
                val projectDocDeferred = async { projectRepository.getProjectDocument(projectId) }
                
                // Await both in parallel
                Pair(chatDocDeferred.await(), projectDocDeferred.await())
            }
            
            // Check if chat exists, if not create it (only for chat metadata, not for expenseChats)
            // OPTIMIZATION 3: Use local logic for unread counts - get participants and unreadCount from initial fetch
            val chatParticipants = if (chatDoc.exists()) {
                (chatDoc.get("participants") as? List<*>)?.filterIsInstance<String>() 
                    ?: (chatDoc.get("members") as? List<*>)?.filterIsInstance<String>() // Backward compatibility
                    ?: emptyList()
            } else {
                emptyList()
            }
            val isGroupMessage = chatParticipants.size > 2
            
            // Get unread count from initial fetch (don't re-fetch)
            val unreadCount = if (chatDoc.exists()) {
                (chatDoc.get("unreadCount") as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value }?.toMutableMap() ?: mutableMapOf<String, Any?>()
            } else {
                mutableMapOf<String, Any?>()
            }
            
            // Handle chat creation/update logic
            var participantsToUse = chatParticipants.toMutableSet()
            var needsParticipantUpdate = false
            
            if (!chatDoc.exists()) {
                // FOR EXPENSE APPROVAL CHATS: Include both user and approver in initial chat creation
                val initialMembers = if (isExpenseChat) {
                    try {
                        val participants = mutableSetOf<String>()
                        participants.add(senderId)

                        // Get expense details to find the user who submitted it
                        val expense = getExpenseFromChatId(projectId, chatId)
                        if (expense != null) {
                            if (expense.userId != senderId) {
                                participants.add(expense.userId)
                            }
                            
                            // Use already fetched projectDoc — department-based participants
                            if (projectDoc != null) {
                                val deptApproverAssignments = projectDoc.get("departmentApproverAssignments") as? Map<*, *>
                                deptApproverAssignments?.values?.filterIsInstance<String>()?.filter { it.isNotEmpty() }?.forEach { approverPhone ->
                                    participants.add(approverPhone)
                                }
                                
                                val deptUserAssignments = projectDoc.get("departmentUserAssignments") as? Map<*, *>
                                deptUserAssignments?.values?.filterIsInstance<String>()?.filter { it.isNotEmpty() }?.forEach { userPhone ->
                                    participants.add(userPhone)
                                }
                                
                                // Add BusinessHead
                                val businessHead = getBusinessHeadByRole()
                                if (businessHead != null && businessHead.phone.isNotEmpty()) {
                                    participants.add(businessHead.phone)
                                }
                            }
                        } else {
                            val allApprovers = getApproversForExpenseChat(projectId, chatId)
                            participants.addAll(allApprovers)
                        }
                        participants.toList()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        listOf(senderId)
                    }
                } else {
                    listOf(senderId)
                }
                
                participantsToUse = initialMembers.toMutableSet()
                // Initialize unread count for all participants
                initialMembers.forEach { participantId ->
                    if (!unreadCount.containsKey(participantId)) {
                        unreadCount[participantId] = 0 as Any?
                    }
                }
            } else if (isExpenseChat) {
                // Chat exists - FOR EXPENSE APPROVAL CHATS: Ensure both user and approver are ALWAYS participants
                try {


                    val currentParticipants = chatParticipants.toMutableSet()
                    
                    if (!currentParticipants.contains(senderId)) {
                        currentParticipants.add(senderId)
                        needsParticipantUpdate = true
                    }
                    
                    val expense = getExpenseFromChatId(projectId, chatId)
                    if (expense != null) {
                        if (!currentParticipants.contains(expense.userId)) {
                            currentParticipants.add(expense.userId)
                            needsParticipantUpdate = true
                        }
                        
                        // Use already fetched projectDoc — department-based participants
                        if (projectDoc != null) {
                            val deptApproverAssignments = projectDoc.get("departmentApproverAssignments") as? Map<*, *>
                            deptApproverAssignments?.values?.filterIsInstance<String>()?.filter { it.isNotEmpty() }?.forEach { approverPhone ->
                                if (!currentParticipants.contains(approverPhone)) {
                                    currentParticipants.add(approverPhone)
                                    needsParticipantUpdate = true
                                }
                            }
                            
                            val deptUserAssignments = projectDoc.get("departmentUserAssignments") as? Map<*, *>
                            deptUserAssignments?.values?.filterIsInstance<String>()?.filter { it.isNotEmpty() }?.forEach { userPhone ->
                                if (!currentParticipants.contains(userPhone)) {
                                    currentParticipants.add(userPhone)
                                    needsParticipantUpdate = true
                                }
                            }
                            
                            // Add BusinessHead
                            val businessHead = getBusinessHeadByRole()
                            if (businessHead != null && businessHead.phone.isNotEmpty() && !currentParticipants.contains(businessHead.phone)) {
                                currentParticipants.add(businessHead.phone)
                                needsParticipantUpdate = true
                            }
                        }
                    }
                    
                    participantsToUse = currentParticipants
                    
                    // Initialize unread count for new participants
                    if (needsParticipantUpdate) {
                        currentParticipants.forEach { participantId ->
                            if (!unreadCount.containsKey(participantId)) {
                                unreadCount[participantId] = 0 as Any?
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Use Firebase Timestamp object directly (not string)
            val timestampNow = Timestamp.now()
            
            // Determine message type (text or media)
            val messageTypeString = if (messageType == "Media" || mediaUrl != null) "media" else "text"
            
            // Use message text or media placeholder
            val messageText = if (messageType == "Media" && mediaUrl != null) {
                "📷 Image"
            } else {
                message
            }
            
            // OPTIMIZATION 2: Use WriteBatch to combine all writes
            val batch = firestore.batch()
            val chatDocRef = chatsCollection.document(chatId)
            
            // Prepare message data and add to batch
            if (isExpenseChat && expenseChatsCollection != null) {
                Log.d("ExpenseChatDebug", "Writing to expenseChatsCollection path: ${expenseChatsCollection.path}")
                val formattedSenderRole = when (senderRole.uppercase().replace("_", "")) {
                    "BUSINESSHEAD", "BUSINESSHEAD" -> "BUSINESSHEAD"
                    "APPROVER" -> "APPROVER"
                    "USER" -> "USER"
                    else -> senderRole.uppercase().replace("_", "")
                }
                
                val mediaURLList = if (!mediaUrl.isNullOrEmpty()) {
                    listOf(mediaUrl)
                } else {
                    emptyList<String>()
                }
                
                val expenseChatData = hashMapOf<String, Any>(
                    "senderId" to senderId,
                    "senderRole" to formattedSenderRole,
                    "textMessage" to message,
                    "timeStamp" to timestampNow,
                    "mediaURL" to mediaURLList,
                    "mention" to emptyList<String>()
                )
                
                // Add message document to batch
                val expenseChatDocRef = expenseChatsCollection.document()
                batch.set(expenseChatDocRef, expenseChatData)
                
                // Update the document with id field in the same batch
                batch.update(expenseChatDocRef, "id", expenseChatDocRef.id)
            } else if (!isExpenseChat && messagesCollection != null) {
                val messageData = hashMapOf<String, Any>(
                    "isGroupMessage" to isGroupMessage,
                    "isRead" to false,
                    "senderId" to senderId,
                    "text" to messageText,
                    "timestamp" to timestampNow,
                    "type" to messageTypeString,
                    "messageType" to messageType,
                    "message" to message,
                    "senderName" to senderName,
                    "senderRole" to senderRole
                )
                
                // Add mediaUrl if present (for image/file messages)
                if (!mediaUrl.isNullOrEmpty()) {
                    messageData["mediaUrl"] = mediaUrl
                }
                
                // Create a new document reference and add to batch
                val messageDocRef = messagesCollection.document()
                batch.set(messageDocRef, messageData)
            } else {
                return@withContext false
            }
            
            // OPTIMIZATION 3: Calculate unread counts locally (don't re-fetch)
            // Filter out any empty string keys from existing unreadCount and participants
            val updatedUnreadCount = mutableMapOf<String, Any?>()
            unreadCount.forEach { (k, v) -> if (k.isNotEmpty()) updatedUnreadCount[k] = v ?: 0 }
            
            // Remove any empty strings from participants (safety net)
            participantsToUse.remove("")
            
            participantsToUse.forEach { participantId ->
                if (participantId != senderId) {
                    val currentCount = (updatedUnreadCount[participantId] as? Number)?.toInt() ?: 0
                    updatedUnreadCount[participantId] = (currentCount + 1) as Any?
                }
            }
            
            // Prepare chat metadata update
            val chatUpdateData = mutableMapOf<String, Any>(
                "lastMessage" to message,
                "lastMessageTime" to timestampNow,
                "lastMessageSenderId" to senderId,
                "updatedAt" to timestampNow,
                "unreadCount" to updatedUnreadCount
            )
            
            // Add participant update if needed
            if (needsParticipantUpdate && isExpenseChat) {
                chatUpdateData["participants"] = participantsToUse.toList()
            }
            
            // Create chat if it doesn't exist, otherwise update
            if (!chatDoc.exists()) {
                chatUpdateData["createdAt"] = timestampNow
                batch.set(chatDocRef, chatUpdateData)
            } else {
                batch.update(chatDocRef, chatUpdateData)
            }
            
            // Commit all writes in a single batch operation
            Log.d("ExpenseChatDebug", "Committing batch... participants=${participantsToUse.toList()}")
            batch.commit().await()
            Log.d("ExpenseChatDebug", "Batch committed successfully!")
            
            // OPTIMIZATION 4: Return immediately after batch commit, move all notification logic to background
            val otherParticipants = participantsToUse.filter { it != senderId }
            
            // Launch notification logic in background (non-blocking)
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (otherParticipants.isNotEmpty()) {
                        // Get project name for notification (use already fetched projectDoc if available)
                        val projectName = if (projectDoc != null) {
                            projectDoc.get("name") as? String ?: "Unknown Project"
                        } else {
                            // Fallback: fetch if not already available
                            val fallbackProjectDoc = projectRepository.getProjectDocument(projectId)
                            if (fallbackProjectDoc != null) {
                                fallbackProjectDoc.get("name") as? String ?: "Unknown Project"
                            } else {
                                "Unknown Project"
                            }
                        }
                        
                        // Create notifications for each receiver
                        for (receiverId in otherParticipants) {
                            try {
                                val normalizedReceiverId = getChatUserId(receiverId)
                                val receiverDoc = usersCollection.document(normalizedReceiverId).get().await()
                                val receiverRole = receiverDoc.get("role") as? String ?: "USER"
                                
                                val notificationTitle = if (isExpenseChat) {
                                    "New message about pending expense from $senderName"
                                } else {
                                    "New message from $senderName"
                                }
                                
                                val notification = Notification(
                                    recipientId = normalizedReceiverId,
                                    recipientRole = receiverRole,
                                    title = notificationTitle,
                                    message = message,
                                    type = NotificationType.CHAT_MESSAGE,
                                    projectId = projectId,
                                    projectName = projectName,
                                    relatedId = chatId,
                                    isRead = false,
                                    actionRequired = if (isExpenseChat) true else false,
                                    navigationTarget = "chat/$projectId/$chatId/$senderName",
                                    createdAt = Timestamp.now()
                                )
                                
                                // Notification creation removed - notifications are now handled by FCM only
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        // Notify Production Head for all chat messages
                        try {
                            val projectDocForPH = projectDoc ?: projectRepository.getProjectDocument(projectId)
                            if (projectDocForPH != null) {
                                val projectNameForPH = projectDocForPH.get("name") as? String ?: "Unknown Project"
                                val BusinessHeadIds = projectDocForPH.get("BusinessHeadIds") as? List<*>
                                val ownerID = projectDocForPH.get("ownerID") as? String
                                
                                val allBusinessHeadIds = mutableListOf<String>()
                                
                                if (BusinessHeadIds != null && BusinessHeadIds.isNotEmpty()) {
                                    BusinessHeadIds.filterIsInstance<String>().forEach { phId ->
                                        if (phId.isNotEmpty() && !allBusinessHeadIds.contains(phId)) {
                                            allBusinessHeadIds.add(phId)
                                        }
                                    }
                                }
                                
                                if (!ownerID.isNullOrEmpty() && !allBusinessHeadIds.contains(ownerID)) {
                                    allBusinessHeadIds.add(ownerID)
                                }
                                
                                for (BusinessHeadId in allBusinessHeadIds) {
                                    try {
                                        if (BusinessHeadId == senderId || getChatUserId(BusinessHeadId) == getChatUserId(senderId)) {
                                            continue
                                        }
                                        
                                        var BusinessHeadRole = "BUSINESS_HEAD"
                                        try {
                                            val normalizedPHId = getChatUserId(BusinessHeadId)
                                            val phDoc = usersCollection.document(normalizedPHId).get().await()
                                            if (phDoc.exists()) {
                                                BusinessHeadRole = phDoc.get("role") as? String ?: "BUSINESS_HEAD"
                                            } else {
                                                val phoneQuery = usersCollection.whereEqualTo("phoneNumber", BusinessHeadId).limit(1).get().await()
                                                if (!phoneQuery.isEmpty) {
                                                    val userDoc = phoneQuery.documents.first()
                                                    BusinessHeadRole = userDoc.get("role") as? String ?: "BUSINESS_HEAD"
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        
                                        val phNotificationTitle = if (isExpenseChat) {
                                            "New message about pending expense from $senderName"
                                        } else {
                                            "New message from $senderName in $projectNameForPH"
                                        }
                                        
                                        val phNotification = Notification(
                                            recipientId = BusinessHeadId,
                                            recipientRole = BusinessHeadRole,
                                            title = phNotificationTitle,
                                            message = message,
                                            type = NotificationType.CHAT_MESSAGE,
                                            projectId = projectId,
                                            projectName = projectNameForPH,
                                            relatedId = chatId,
                                            isRead = false,
                                            actionRequired = if (isExpenseChat) true else false,
                                            navigationTarget = "chat/$projectId/$chatId/$senderName",
                                            createdAt = Timestamp.now()
                                        )
                                        
                                        // Notification creation removed - notifications are now handled by FCM only
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        
                        // Special handling: Notify approvers when user sends message in pending expense chat
                        if (isExpenseChat) {
                            try {
                                val expense = getExpenseFromChatId(projectId, chatId)
                                if (expense != null) {
                                    val isPending = expense.status == com.cubiquitous.tracura.model.ExpenseStatus.PENDING
                                    val isUserSender = senderRole.uppercase() != "APPROVER" && 
                                                      senderRole.uppercase() != "BUSINESS_HEAD" &&
                                                      senderRole.uppercase() != "PRODUCTION HEAD"
                                    
                                    if (isPending && isUserSender) {
                                        val approvers = getApproversForExpenseChat(projectId, chatId)
                                        val projectNameForApprovers = if (projectDoc != null) {
                                            projectDoc.get("name") as? String ?: "Unknown Project"
                                        } else {
                                            val fallbackProjectDoc = projectRepository.getProjectDocument(projectId)
                                            if (fallbackProjectDoc != null) {
                                                fallbackProjectDoc.get("name") as? String ?: "Unknown Project"
                                            } else {
                                                "Unknown Project"
                                            }
                                        }
                                        
                                        for (approverId in approvers) {
                                            try {
                                                if (approverId == senderId) {
                                                    continue
                                                }
                                                
                                                val normalizedApproverId = getChatUserId(approverId)
                                                
                                                var approverRole = "APPROVER"
                                                try {
                                                    val approverDoc = usersCollection.document(normalizedApproverId).get().await()
                                                    if (approverDoc.exists()) {
                                                        approverRole = approverDoc.get("role") as? String ?: "APPROVER"
                                                    } else {
                                                        val phoneQuery = usersCollection.whereEqualTo("phoneNumber", approverId).limit(1).get().await()
                                                        if (!phoneQuery.isEmpty) {
                                                            val userDoc = phoneQuery.documents.first()
                                                            approverRole = userDoc.get("role") as? String ?: "APPROVER"
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                                
                                                val approverNotification = Notification(
                                                    recipientId = approverId,
                                                    recipientRole = approverRole,
                                                    title = "New message about pending expense from $senderName",
                                                    message = "Message: $message\n\nExpense: ${expense.category} - ₹${String.format("%.2f", expense.amount)}",
                                                    type = NotificationType.CHAT_MESSAGE,
                                                    projectId = projectId,
                                                    projectName = projectNameForApprovers,
                                                    relatedId = expense.id,
                                                    isRead = false,
                                                    actionRequired = true,
                                                    navigationTarget = "expense_chat/${projectId}/${expense.id}",
                                                    createdAt = Timestamp.now()
                                                )
                                                
                                                val notificationId = "expense_chat_${projectId}_${expense.id}_${System.currentTimeMillis()}"
                                                val notificationWithId = approverNotification.copy(id = notificationId)
                                                
                                                // Save notification to local storage
                                                try {
                                                    if (context != null) {
                                                        val storageManager = NotificationLocalStorageManager(context)
                                                        storageManager.saveNotification(notificationWithId)
                                                    }
                                                } catch (e: Exception) {
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Return true immediately after batch commit (non-blocking)
            true
            } catch (e: Exception) {
                Log.e("ExpenseChatDebug", "sendMessage FAILED with exception", e)
                e.printStackTrace()
                false
            }
        }
    }

    // Send an image or file message
    suspend fun sendImageMessage(
        projectId: String,
        chatId: String, 
        senderId: String, 
        senderName: String, 
        senderRole: String, 
        imageUri: android.net.Uri
    ): Boolean {
        return try {
            
            // Check if URI is valid
            if (imageUri.toString().isEmpty()) {
                Log.e("ChatRepository", "sendImageMessage: URI is empty")
                return false
            }
            
            // Get application context
            val context = FirebaseAuth.getInstance().app?.applicationContext
            if (context == null) {
                Log.e("ChatRepository", "sendImageMessage: Cannot get application context")
                return false
            }
            
            // Detect MIME type from URI
            val mimeType = context.contentResolver.getType(imageUri) ?: "application/octet-stream"
            Log.d("ChatRepository", "sendImageMessage: MIME type = $mimeType")
            
            // Map MIME type to file extension
            val extension = when {
                mimeType.startsWith("image/jpeg") || mimeType.startsWith("image/jpg") -> "jpg"
                mimeType.startsWith("image/png") -> "png"
                mimeType.startsWith("image/gif") -> "gif"
                mimeType.startsWith("image/webp") -> "webp"
                mimeType.startsWith("application/pdf") -> "pdf"
                mimeType.startsWith("application/msword") -> "doc"
                mimeType.contains("wordprocessingml") -> "docx"
                mimeType.contains("spreadsheetml") || mimeType.startsWith("application/vnd.ms-excel") -> "xlsx"
                mimeType.contains("presentationml") || mimeType.startsWith("application/vnd.ms-powerpoint") -> "pptx"
                mimeType.startsWith("text/plain") -> "txt"
                mimeType.startsWith("application/zip") -> "zip"
                mimeType.startsWith("image/") -> "jpg"
                else -> {
                    val uriPath = imageUri.lastPathSegment ?: ""
                    val dotIndex = uriPath.lastIndexOf('.')
                    if (dotIndex >= 0) uriPath.substring(dotIndex + 1) else "file"
                }
            }
            
            // STEP 1: Copy the content URI to a stable cache file
            // This is critical because content URIs from file pickers can expire
            val timestamp = System.currentTimeMillis()
            val cacheFile = java.io.File(context.cacheDir, "upload_${timestamp}.${extension}")
            
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                if (inputStream == null) {
                    Log.e("ChatRepository", "sendImageMessage: Cannot open input stream for URI: $imageUri")
                    return false
                }
                inputStream.use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d("ChatRepository", "sendImageMessage: Copied to cache: ${cacheFile.absolutePath}, size: ${cacheFile.length()}")
            } catch (e: Exception) {
                Log.e("ChatRepository", "sendImageMessage: Failed to copy file to cache: ${e.message}", e)
                return false
            }
            
            // Verify the cache file is valid
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                Log.e("ChatRepository", "sendImageMessage: Cache file is empty or doesn't exist")
                return false
            }
            
            // Determine if this is an image or a file
            val isImage = mimeType.startsWith("image/")
            
            // STEP 2: Upload the stable cache file to Firebase Storage
            val storagePath = if (isImage) {
                "chat_images/${projectId}/${chatId}/${timestamp}.${extension}"
            } else {
                "chat_files/${projectId}/${chatId}/${timestamp}.${extension}"
            }
            
            val messageText = when {
                isImage -> "📷 Image"
                mimeType.startsWith("application/pdf") -> "📄 PDF Document"
                mimeType.contains("word") || mimeType.contains("document") -> "📄 Word Document"
                mimeType.contains("spreadsheet") || mimeType.contains("excel") -> "📄 Spreadsheet"
                mimeType.contains("presentation") || mimeType.contains("powerpoint") -> "📄 Presentation"
                mimeType.startsWith("text/") -> "📄 Text File"
                else -> "📎 File"
            }
            
            val storageRef = FirebaseStorage.getInstance().reference
            val fileRef = storageRef.child(storagePath)
            
            // Upload from cache file URI (not the original content URI)
            val cacheUri = android.net.Uri.fromFile(cacheFile)
            val uploadTask = fileRef.putFile(cacheUri)
            
            // Wait for upload to complete
            uploadTask.await()
            
            // Get download URL
            val downloadUrl = fileRef.downloadUrl.await()
            Log.d("ChatRepository", "sendImageMessage: Upload complete, URL: $downloadUrl")
            
            // Clean up cache file
            try { cacheFile.delete() } catch (e: Exception) { /* ignore */ }
            
            if (downloadUrl.toString().isEmpty()) {
                Log.e("ChatRepository", "sendImageMessage: Download URL is empty")
                return false
            }
            
            // STEP 3: Send message with file URL
            val success = sendMessage(
                projectId = projectId,
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                senderRole = senderRole,
                message = messageText,
                messageType = "Media",
                mediaUrl = downloadUrl.toString()
            )
            
            success
        } catch (e: Exception) {
            Log.e("ChatRepository", "sendImageMessage: Failed: ${e.message}", e)
            false
        }
    }

    // Get messages for a chat
    fun getChatMessages(projectId: String, chatId: String): Flow<List<Message>> = callbackFlow {
        // Check if this is an expense approval chat - if so, use expenseChats collection
        val isExpenseChat = chatId.startsWith("expense_approval_")
        
        // Get the collection reference (suspend function - callbackFlow supports suspend)
        val messagesCollection = try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                if (isExpenseChat) {
                    // For expense approval chats, use expenseChats collection
                    val expenseId = chatId.removePrefix("expense_approval_")
                    getExpenseChatsCollection(projectId, expenseId)
                } else {
                    // For regular chats, use messages collection
                    getMessagesCollection(projectId, chatId)
                }
            }
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }
        
        val collectionPath = messagesCollection.path
        
        // Get all messages and sort in memory (handles both old and new formats)
        // Use MetadataChanges.INCLUDE to ensure we catch ALL changes, including manual edits in Firestore console
        // This makes the listener REAL-TIME and will update immediately when you change messages in Firestore
        
        // First, verify the collection exists by doing an initial fetch
        try {
            val initialSnapshot = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                messagesCollection.get().await()
            }
        } catch (e: Exception) {
            // Don't close - continue to set up listener anyway
        }
        
        val listener = messagesCollection
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                // CRITICAL: This log confirms the listener callback is being triggered
                
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    return@addSnapshotListener
                }
                
                // Log metadata info to track changes
                val metadata = snapshot.metadata
                val hasPendingWrites = metadata.hasPendingWrites()
                val isFromCache = metadata.isFromCache()
                
                
                // Log document changes to track what changed
                if (snapshot.documentChanges.isNotEmpty()) {
                    snapshot.documentChanges.forEach { change ->
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                            val text = change.document.data["text"] as? String ?: change.document.data["message"] as? String
                        }
                    }
                } else {
                }
                
                val messages = snapshot.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        
                        if (isExpenseChat) {
                            // Parse expenseChats format: senderId, senderRole, textMessage, timeStamp, mediaURL (List), mention (List)
                            val timestampTimestamp = when {
                                data["timeStamp"] is Timestamp -> data["timeStamp"] as Timestamp
                                data["timeStamp"] is String -> {
                                    val timestampString = data["timeStamp"] as String
                                    if (timestampString.isNotBlank()) {
                                        com.cubiquitous.tracura.utils.FormatUtils.parseMessageTimestamp(timestampString)?.let { Timestamp(it) }
                                    } else {
                                        null
                                    }
                                }
                                data["timestamp"] is Timestamp -> data["timestamp"] as Timestamp // Fallback
                                data["timestamp"] is String -> {
                                    val timestampString = data["timestamp"] as String
                                    if (timestampString.isNotBlank()) {
                                        com.cubiquitous.tracura.utils.FormatUtils.parseMessageTimestamp(timestampString)?.let { Timestamp(it) }
                                    } else {
                                        null
                                    }
                                }
                                else -> null
                            }
                            
                            val timestampString = timestampTimestamp?.let {
                                com.cubiquitous.tracura.utils.FormatUtils.formatMessageTimestamp(it)
                            } ?: (data["timeStamp"] as? String ?: data["timestamp"] as? String ?: "")
                            
                            // Handle mediaURL: can be List<String> (new format) or String (old format for backward compatibility)
                            val mediaURLList = when (val mediaURLData = data["mediaURL"]) {
                                is List<*> -> mediaURLData.filterIsInstance<String>()
                                is String -> if (mediaURLData.isNotEmpty()) listOf(mediaURLData) else emptyList()
                                else -> emptyList<String>()
                            }
                            
                            // Determine message type based on mediaURL
                            val messageType = if (mediaURLList.isNotEmpty()) "Media" else "Text"
                            val mediaUrlString = mediaURLList.firstOrNull()
                            
                            // Get text message
                            val textMessage = (data["textMessage"] as? String) ?: ""
                            
                            // Get id from document data, fallback to doc.id for backward compatibility
                            val messageId = (data["id"] as? String) ?: doc.id
                            
                            Message(
                                id = messageId,
                                chatId = "",
                                senderId = (data["senderId"] as? String) ?: "",
                                isGroupMessage = false, // Expense chats are always group chats
                                isRead = false, // ExpenseChats format doesn't have isRead field
                                text = textMessage,
                                timeStamp = timestampString,
                                type = if (mediaURLList.isNotEmpty()) "media" else "text",
                                messageType = messageType,
                                message = textMessage,
                                mediaUrl = mediaUrlString,
                                timestampTimestamp = timestampTimestamp,
                                senderName = "", // Not stored in expenseChats format
                                senderRole = (data["senderRole"] as? String) ?: "",
                                readBy = emptyList()
                            )
                        } else {
                            // Parse regular chat format: isGroupMessage, isRead, senderId, text, timestamp, type
                            val timestampTimestamp = when {
                                data["timestamp"] is Timestamp -> data["timestamp"] as Timestamp
                                data["timestamp"] is String -> {
                                    val timestampString = data["timestamp"] as String
                                    if (timestampString.isNotBlank()) {
                                        com.cubiquitous.tracura.utils.FormatUtils.parseMessageTimestamp(timestampString)?.let { Timestamp(it) }
                                    } else {
                                        null
                                    }
                                }
                                data["timeStamp"] is Timestamp -> data["timeStamp"] as Timestamp // Fallback
                                data["timeStamp"] is String -> {
                                    val timestampString = data["timeStamp"] as String
                                    if (timestampString.isNotBlank()) {
                                        com.cubiquitous.tracura.utils.FormatUtils.parseMessageTimestamp(timestampString)?.let { Timestamp(it) }
                                    } else {
                                        null
                                    }
                                }
                                else -> null
                            }
                            
                            val timestampString = timestampTimestamp?.let {
                                com.cubiquitous.tracura.utils.FormatUtils.formatMessageTimestamp(it)
                            } ?: (data["timestamp"] as? String ?: data["timeStamp"] as? String ?: "")
                            
                            Message(
                                id = doc.id,
                                chatId = "",
                                senderId = (data["senderId"] as? String) ?: "",
                                isGroupMessage = (data["isGroupMessage"] as? Boolean) ?: false,
                                isRead = (data["isRead"] as? Boolean) ?: false,
                                text = (data["text"] as? String) ?: "",
                                timeStamp = timestampString,
                                type = (data["type"] as? String) ?: "text",
                                messageType = (data["messageType"] as? String) ?: "Text",
                                message = (data["message"] as? String) ?: (data["text"] as? String) ?: "",
                                mediaUrl = (data["mediaUrl"] as? String),
                                timestampTimestamp = timestampTimestamp ?: (data["timestampTimestamp"] as? Timestamp),
                                senderName = (data["senderName"] as? String) ?: "",
                                senderRole = (data["senderRole"] as? String) ?: "",
                                readBy = (data["readBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                            )
                        }
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                // Sort messages by timeStamp (parse string timeStamp if timestampTimestamp is null)
                val sortedMessages = messages.sortedBy { msg ->
                    msg.timestampTimestamp?.toDate()?.time 
                        ?: (msg.timeStamp.takeIf { it.isNotBlank() }?.let { 
                            com.cubiquitous.tracura.utils.FormatUtils.parseMessageTimestamp(it)?.time 
                        } ?: 0L)
                }
                
                // Log each message text to verify updates
                sortedMessages.forEach { msg ->
                }
                trySend(sortedMessages)
            }
        
        awaitClose { 
            listener.remove()
        }
    }

    // Mark messages as read
    suspend fun markMessagesAsRead(projectId: String, chatId: String, userId: String) {
        // Use NonCancellable to ensure this operation completes even if the coroutine is cancelled
        withContext(NonCancellable) {
            try {
                val isExpenseChat = chatId.startsWith("expense_approval_")
                val chatsCollection = getChatsCollection(projectId)
                
                // For expense chats, expenseChats format doesn't have isRead field, so skip marking individual messages
                // For regular chats, mark messages as read
                if (!isExpenseChat) {
                    val messagesCollection = getMessagesCollection(projectId, chatId)
                    
                    val messages = messagesCollection
                        .whereNotEqualTo("senderId", userId)
                        .get()
                        .await()
                    
                    messages.documents.forEach { doc ->
                        // Update isRead field (readBy field is not stored in new format)
                        val currentIsRead = doc.get("isRead") as? Boolean ?: false
                        if (!currentIsRead) {
                            messagesCollection.document(doc.id).update(
                                mapOf(
                                    "isRead" to true
                                )
                            ).await()
                        }
                    }
                }
                
                // Reset unread count - need to handle both phone and UID formats
                val chat = chatsCollection.document(chatId).get().await()
                val unreadCount = (chat.get("unreadCount") as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value } ?: emptyMap()
                val updatedUnreadCount = mutableMapOf<String, Any>()
                unreadCount.forEach { (k, v) -> updatedUnreadCount[k] = v ?: 0 }
                
                // Try to find user by phone or UID to get both identifiers
                var userPhone = userId
                var userUid = userId
                
                try {
                    // Try to get user document to find both phone and UID
                    val userDoc = usersCollection.document(userId).get().await()
                    if (userDoc.exists()) {
                        val userData = userDoc.data
                        userPhone = userData?.get("phoneNumber") as? String ?: userData?.get("phone") as? String ?: userId
                        userUid = userDoc.id
                    } else {
                        // Try to find by phone number
                        val phoneQuery = usersCollection
                            .whereEqualTo("phoneNumber", userId)
                            .limit(1)
                            .get()
                            .await()
                        
                        if (!phoneQuery.isEmpty) {
                            val doc = phoneQuery.documents.first()
                            userUid = doc.id
                            val userData = doc.data
                            userPhone = userData?.get("phoneNumber") as? String ?: userData?.get("phone") as? String ?: userId
                        }
                    }
                } catch (e: Exception) {
                }
                
                // Reset unread count for both phone and UID (in case either is used as key)
                // Also check all existing keys in unreadCount to find any that match the user
                val keysToReset = mutableSetOf<String>()
                keysToReset.add(userPhone)
                keysToReset.add(userUid)
                keysToReset.add(userId)
                
                // Check if any existing keys in unreadCount match the user (by phone or UID)
                unreadCount.keys.forEach { key ->
                    val keyStr = key.toString()
                    // If the key matches userPhone, userUid, or userId, reset it
                    if (keyStr == userPhone || keyStr == userUid || keyStr == userId) {
                        keysToReset.add(keyStr)
                    }
                }
                
                // Reset unread count for all matching keys
                keysToReset.forEach { key ->
                    updatedUnreadCount[key] = 0
                }
                
                
                // Update unread count - use update() with the map directly
                chatsCollection.document(chatId).update("unreadCount", updatedUnreadCount).await()
                
                // Verify the update was successful
                val verifyChat = chatsCollection.document(chatId).get().await()
                val verifyUnreadCount = (verifyChat.get("unreadCount") as? Map<*, *>)?.mapKeys { it.key.toString() } ?: emptyMap()
                
            } catch (e: Exception) {
                e.printStackTrace()
                // Don't re-throw - we want to complete even if there's an error
            }
        }
    }

    // Get user chats
    fun getUserChats(userId: String, projectId: String): Flow<List<Chat>> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        
        try {
            // Get chats collection - this is a suspend function, so we need to await it
            val chatsCollection = withContext(kotlinx.coroutines.Dispatchers.IO) {
                getChatsCollection(projectId)
            }
            
            
            // Helper function to handle chat snapshot (defined before use)
            fun handleChatSnapshot(snapshot: com.google.firebase.firestore.QuerySnapshot?, error: Exception?, flow: kotlinx.coroutines.channels.ProducerScope<List<Chat>>, userId: String) {
                if (error != null) {
                    // Send empty list instead of closing with error to prevent crash
                    flow.trySend(emptyList())
                    return
                }
                
                
                val chats = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val chat = doc.toObject(Chat::class.java)?.copy(id = doc.id)
                        // Log unread counts for debugging
                        chat?.let {
                            val unreadCount = it.unreadCount
                            val rawUnreadCount = doc.get("unreadCount") as? Map<*, *>
                            // Check specific user's unread count
                            val userUnreadCount = (unreadCount[userId] as? Number)?.toInt() ?: 0
                        }
                        chat
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                // Sort by lastMessageTime in memory (descending - most recent first)
                val sortedChats = chats.sortedByDescending { chat ->
                    chat.lastMessageTime?.seconds ?: 0L
                }
                
                flow.trySend(sortedChats)
            }
            
            // Remove orderBy to avoid requiring a composite index
            // We'll sort in memory instead
            // Try participants first, fallback to members for backward compatibility
            listener = try {
                chatsCollection
                    .whereArrayContains("participants", userId)
                    .addSnapshotListener { snapshot, error ->
                        handleChatSnapshot(snapshot, error, this@callbackFlow, userId)
                    }
            } catch (e: Exception) {
                // Fallback to old "members" field for backward compatibility
                chatsCollection
                    .whereArrayContains("members", userId)
                    .addSnapshotListener { snapshot, error ->
                        handleChatSnapshot(snapshot, error, this@callbackFlow, userId)
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Send empty list on error to prevent crash
            trySend(emptyList())
        }
        
        awaitClose { 
            listener?.remove()
        }
    }

    // Get Production Head by role
    private suspend fun getBusinessHeadByRole(): ChatMember? {
        return try {
            // Try different role formats
            val roleVariations = listOf("BUSINESS_HEAD", "Production Head", "BUSINESS_HEAD", "PRODUCTION HEAD")
            
            for (roleFormat in roleVariations) {
                val querySnapshot = usersCollection
                    .whereEqualTo("role", roleFormat)
                    .get()
                    .await()
            
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents.first()
                    val userData = document.data
                    
                    if (userData != null) {
                        val deviceInfoData = userData["deviceInfo"] as? Map<*, *>
                        val isOnline = deviceInfoData?.get("isOnline") as? Boolean ?: false
                        val lastLoginAt = deviceInfoData?.get("lastLoginAt") as? Long ?: 0L
                        
                        val member = ChatMember(
                            userId = document.id,
                            name = userData["name"] as? String ?: "Unknown",
                            phone = userData["phoneNumber"] as? String ?: userData["phone"] as? String ?: "",
                            role = UserRole.BUSINESS_HEAD,
                            isOnline = isOnline,
                            lastSeen = lastLoginAt
                        )
                        
                        return member
                    } else {
                    }
                } else {
                }
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }

    // Get user details by ID
    suspend fun getUserById(userId: String): ChatMember? {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val userData = userDoc.data
            
            if (userData != null) {
                val deviceInfoData = userData["deviceInfo"] as? Map<*, *>
                val isOnline = deviceInfoData?.get("isOnline") as? Boolean ?: false
                val lastLoginAt = deviceInfoData?.get("lastLoginAt") as? Long ?: 0L
                
                val roleString = userData["role"] as? String
                val userRole = when (roleString?.uppercase()?.replace(" ", "_")) {
                    "APPROVER" -> UserRole.APPROVER
                    "BUSINESS_HEAD" -> UserRole.BUSINESS_HEAD
                    "ADMIN" -> UserRole.ADMIN
                    else -> UserRole.USER
                }
                
                ChatMember(
                    userId = userId,
                    name = userData["name"] as? String ?: "Unknown",
                    phone = userData["phoneNumber"] as? String ?: userData["phone"] as? String ?: "",
                    role = userRole,
                    isOnline = isOnline,
                    lastSeen = lastLoginAt
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    // Get expenseChats for a specific expense
    fun getExpenseChats(projectId: String, expenseId: String): Flow<List<com.cubiquitous.tracura.model.ExpenseChat>> = callbackFlow {
        
        try {
            val expenseChatsCollection = getExpenseChatsCollection(projectId, expenseId)
            
            val listener = expenseChatsCollection
                .orderBy("timeStamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val expenseChats = snapshot.documents.mapNotNull { doc ->
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
                                
                                // Get id from document data, fallback to doc.id for backward compatibility
                                val expenseChatId = (data["id"] as? String) ?: doc.id
                                
                                val expenseChat = com.cubiquitous.tracura.model.ExpenseChat(
                                    id = expenseChatId,
                                    mediaURL = mediaURLList,
                                    mention = mentionList,
                                    senderId = data["senderId"] as? String ?: "",
                                    senderRole = data["senderRole"] as? String ?: "",
                                    textMessage = data["textMessage"] as? String ?: "",
                                    timeStamp = (data["timeStamp"] as? Timestamp) ?: (data["timestamp"] as? Timestamp) // Backward compatibility
                                )
                                
                                expenseChat
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        trySend(expenseChats)
                    } else {
                        trySend(emptyList())
                    }
                }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            close(e)
        }
    }
}
