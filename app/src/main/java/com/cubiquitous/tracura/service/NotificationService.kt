package com.cubiquitous.tracura.service

import android.util.Log
import com.cubiquitous.tracura.model.Notification
import com.cubiquitous.tracura.model.NotificationType
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.repository.NotificationRepository
import com.cubiquitous.tracura.repository.ProjectRepository
import com.cubiquitous.tracura.repository.AuthRepository
import com.cubiquitous.tracura.repository.TemporaryApproverRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,
    private val firestore: FirebaseFirestore,
    private val fcmPushNotificationService: FCMPushNotificationService
) {
    
    /**
     * Send expense submission notification to project approvers and production heads
     */
    suspend fun sendExpenseSubmissionNotification(
        projectId: String,
        expenseId: String,
        submittedBy: String,
        amount: Double,
        category: String,
        budgetExceeded: Boolean = false
    ): Result<Unit> {
        return try {
            Log.d("NotificationService", "🔄 Sending expense submission notification for project: $projectId, expense: $expenseId")
            
            // Get project details from correct path: customers/{customerId}/Projects/{projectId}
            // Retry logic: Sometimes the project might not be immediately available after expense creation
            var projectDoc: com.google.firebase.firestore.DocumentSnapshot? = null
            var retryCount = 0
            val maxRetries = 3
            
            while (projectDoc == null && retryCount < maxRetries) {
                try {
                    projectDoc = projectRepository.getProjectDocument(projectId)
                    if (projectDoc == null || !projectDoc.exists()) {
                        retryCount++
                        if (retryCount < maxRetries) {
                            Log.w("NotificationService", "⚠️ Project not found, retrying... ($retryCount/$maxRetries)")
                            kotlinx.coroutines.delay(500L * retryCount) // Exponential backoff
                        } else {
                            Log.e("NotificationService", "❌ Project not found after $maxRetries retries: $projectId")
                            Log.e("NotificationService", "🔄 Attempting fallback: sending notifications to all approvers")
                            // Try to send notifications to all approvers as fallback (even without project details)
                            return sendNotificationsToAllApprovers(projectId, expenseId, submittedBy, amount, category, budgetExceeded)
                        }
                    } else {
                        Log.d("NotificationService", "✅ Found project document on attempt ${retryCount + 1}")
                        break // Exit the loop when project is found
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    Log.e("NotificationService", "❌ Job was cancelled while retrieving project document")
                    // Don't retry if job was cancelled, go to fallback
                    return sendNotificationsToAllApprovers(projectId, expenseId, submittedBy, amount, category, budgetExceeded)
                } catch (e: Exception) {
                    Log.e("NotificationService", "❌ Error retrieving project document: ${e.message}")
                    retryCount++
                    if (retryCount < maxRetries) {
                        Log.w("NotificationService", "⚠️ Retrying after error... ($retryCount/$maxRetries)")
                        kotlinx.coroutines.delay(500L * retryCount)
                    } else {
                        Log.e("NotificationService", "❌ Failed to retrieve project after $maxRetries retries")
                        return sendNotificationsToAllApprovers(projectId, expenseId, submittedBy, amount, category, budgetExceeded)
                    }
                }
            }
            
            if (projectDoc == null || !projectDoc.exists()) {
                Log.e("NotificationService", "❌ Project not found: $projectId")
                Log.e("NotificationService", "🔄 Attempting fallback: sending notifications to all approvers")
                return sendNotificationsToAllApprovers(projectId, expenseId, submittedBy, amount, category, budgetExceeded)
            }
            
            val projectData = projectDoc.data ?: return Result.failure(Exception("Project data is null"))
            val projectName = projectData["name"] as? String ?: "Unknown Project"
            @Suppress("UNCHECKED_CAST")
            val approverIds = (projectData["approverIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val BusinessHeadIds = (projectData["BusinessHeadIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val managerId = projectData["managerId"] as? String ?: ""

            Log.d("NotificationService", "📋 Project: $projectName")
            Log.d("NotificationService", "📋 Project approverIds: $approverIds")
            Log.d("NotificationService", "📋 Project BusinessHeadIds: $BusinessHeadIds")
            Log.d("NotificationService", "📋 Project managerId: $managerId")
            Log.d("NotificationService", "📋 Project teamMembers: ${projectData["teamMembers"]}")
            
            // Collect approver and production head IDs
            // Use the same approach as chat notifications: read directly from project document
            // No need to call getAllUsers() - we use the IDs from the project document directly
            val approverIdsList = mutableListOf<String>()
            val BusinessHeadIdsList = mutableListOf<String>()

            // Add project-specific approvers directly from project document (same as chat notifications)
            // approverIds are phone numbers stored in the project document
            approverIds.forEach { approverId ->
                if (approverId.isNotEmpty() && !approverIdsList.contains(approverId)) {
                    approverIdsList.add(approverId)
                    Log.d("NotificationService", "✅ Added approver from project approverIds: $approverId")
                }
            }
            
            // Add project-specific production heads directly from project document (same as chat notifications)
            // BusinessHeadIds are phone numbers stored in the project document
            BusinessHeadIds.forEach { BusinessHeadId ->
                if (BusinessHeadId.isNotEmpty() && !BusinessHeadIdsList.contains(BusinessHeadId)) {
                    BusinessHeadIdsList.add(BusinessHeadId)
                    Log.d("NotificationService", "✅ Added production head from project BusinessHeadIds: $BusinessHeadId")
                }
            }
            
            // Add project manager as approver directly from project document
            if (managerId.isNotEmpty() && !approverIdsList.contains(managerId)) {
                approverIdsList.add(managerId)
                Log.d("NotificationService", "✅ Added manager from project managerId: $managerId")
            }
            
            Log.d("NotificationService", "📋 Final: ${approverIdsList.size} approvers and ${BusinessHeadIdsList.size} production heads")
            
            // Check for temporary approver directly from project document (same as chat notifications)
            val temporaryApproverPhone = projectData["temporaryApproverPhone"] as? String
            if (!temporaryApproverPhone.isNullOrEmpty() && !approverIdsList.contains(temporaryApproverPhone)) {
                approverIdsList.add(temporaryApproverPhone)
                Log.d("NotificationService", "✅ Added temporary approver from project: $temporaryApproverPhone")
            }
            
            // Check tempApproverID (legacy field) - use directly as phone number
            val tempApproverID = projectData["tempApproverID"] as? String
            if (!tempApproverID.isNullOrEmpty() && !approverIdsList.contains(tempApproverID)) {
                approverIdsList.add(tempApproverID)
                Log.d("NotificationService", "✅ Added temporary approver (legacy) from project: $tempApproverID")
            }
            
            // Log final approver count
            Log.d("NotificationService", "📋 Found ${approverIdsList.size} approvers from project document")
            
            // If no approvers found, log warning (but don't use getAllUsers fallback - it's unreliable)
            if (approverIdsList.isEmpty()) {
                Log.w("NotificationService", "⚠️ No approvers found in project document for project: $projectName")
                Log.w("NotificationService", "⚠️ Project approverIds: $approverIds")
                Log.w("NotificationService", "⚠️ Project managerId: $managerId")
                Log.w("NotificationService", "⚠️ This project may not have approvers assigned")
            }
            
            // Log final production head count
            Log.d("NotificationService", "📋 Found ${BusinessHeadIdsList.size} production heads from project document")
            
            // If no production heads found, log warning
            if (BusinessHeadIdsList.isEmpty()) {
                Log.w("NotificationService", "⚠️ No production heads found in project document for project: $projectName")
                Log.w("NotificationService", "⚠️ Project BusinessHeadIds: $BusinessHeadIds")
            }
            
            Log.d("NotificationService", "📋 Final count: ${approverIdsList.size} approvers and ${BusinessHeadIdsList.size} production heads")
            
            // Send notifications to approvers ALWAYS (both when budget is exceeded and not exceeded)
            // Approvers need to know about all expense submissions for approval
            Log.d("NotificationService", "📤 Preparing to send notifications to ${approverIdsList.size} approver(s)")
            approverIdsList.forEachIndexed { index, approverId ->
                if (approverId.isNotEmpty()) {
                    val title = if (budgetExceeded) {
                        "Budget Exceeded - Expense Requires Approval"
                    } else {
                        "New Expense Submitted"
                    }
                    
                    val message = if (budgetExceeded) {
                        "Budget exceeded: New expense of ₹${String.format("%.2f", amount)} submitted by $submittedBy in ${projectName} (Category: $category) - Requires your approval"
                    } else {
                        "New expense of ₹${String.format("%.2f", amount)} submitted by $submittedBy in ${projectName} (Category: $category)"
                    }

                    Log.d("NotificationService", "📤 Sending notification #${index + 1} to approver:")
                    Log.d("NotificationService", "   - Recipient ID (phone): '$approverId'")
                    Log.d("NotificationService", "   - Title: '$title'")
                    Log.d("NotificationService", "   - Project: '$projectName' ($projectId)")
                    Log.d("NotificationService", "   - Expense ID: '$expenseId'")

                    val notification = Notification(
                        recipientId = approverId, // Using phone number as recipientId
                        recipientRole = "APPROVER",
                        title = title,
                        message = message,
                        type = NotificationType.EXPENSE_SUBMITTED,
                        projectId = projectId,
                        projectName = projectName,
                        relatedId = expenseId,
                        actionRequired = true,
                        navigationTarget = "pending_approvals/$projectId",
                        createdAt = Timestamp.now()
                    )

                    // Save notification to Firestore and send push notification
                    val createResult = notificationRepository.createNotification(notification)
                    if (createResult.isSuccess) {
                        val notificationId = createResult.getOrNull() ?: ""
                        val notificationWithId = notification.copy(id = notificationId)
                        
                        // Send push notification asynchronously
                        CoroutineScope(Dispatchers.IO).launch {
                            fcmPushNotificationService.sendPushNotificationFromModel(notificationWithId)
                        }
                        Log.d("NotificationService", "✅ Notification created and push notification queued for approver: $approverId")
                    } else {
                        Log.e("NotificationService", "❌ Failed to create notification for approver: $approverId")
                    }
                } else {
                    Log.w("NotificationService", "⚠️ Skipping notification #${index + 1} - empty approver ID")
                }
            }
            
            // Log if no approvers found
            if (approverIdsList.isEmpty()) {
                Log.w("NotificationService", "⚠️ No approvers found to notify for project: $projectName")
                Log.w("NotificationService", "⚠️ This means approvers won't receive notifications about this expense!")
                Log.w("NotificationService", "⚠️ Project approverIds: $approverIds")
                Log.w("NotificationService", "⚠️ Project managerId: $managerId")
            } else {
                Log.d("NotificationService", "✅ Successfully prepared notifications for ${approverIdsList.size} approver(s)")
            }

            // Send notifications to production heads (always, but with special message if budget exceeded)
            BusinessHeadIdsList.forEach { BusinessHeadId ->
                if (BusinessHeadId.isNotEmpty()) {
                    val message = if (budgetExceeded) {
                        "Budget exceeded: New expense of ₹${String.format("%.2f", amount)} submitted by $submittedBy in ${projectName} (Category: $category) - Requires your approval"
                    } else {
                        "New expense of ₹${String.format("%.2f", amount)} submitted by $submittedBy in ${projectName} (Category: $category)"
                    }
                    
                    val notification = Notification(
                        recipientId = BusinessHeadId,
                        recipientRole = "BUSINESS_HEAD",
                        title = if (budgetExceeded) "Budget Exceeded - Expense Requires Approval" else "New Expense Submitted",
                        message = message,
                        type = NotificationType.EXPENSE_SUBMITTED,
                        projectId = projectId,
                        projectName = projectName,
                        relatedId = expenseId,
                        actionRequired = true,
                        navigationTarget = "pending_approvals/$projectId",
                        createdAt = Timestamp.now()
                    )
                    
                    // Save notification to Firestore and send push notification
                    val createResult = notificationRepository.createNotification(notification)
                    if (createResult.isSuccess) {
                        val notificationId = createResult.getOrNull() ?: ""
                        val notificationWithId = notification.copy(id = notificationId)
                        
                        // Send push notification asynchronously
                        CoroutineScope(Dispatchers.IO).launch {
                            fcmPushNotificationService.sendPushNotificationFromModel(notificationWithId)
                        }
                        Log.d("NotificationService", "✅ Notification created and push notification queued for production head: $BusinessHeadId")
                    } else {
                        Log.e("NotificationService", "❌ Failed to create notification for production head: $BusinessHeadId")
                    }
                } else {
                    Log.w("NotificationService", "⚠️ Skipping notification for empty production head ID")
                }
            }
            
            // This service focuses on storing notifications in Firestore
            
            val totalNotificationsSent = approverIdsList.size + BusinessHeadIdsList.size
            Log.d("NotificationService", "✅ Successfully sent expense submission notifications")
            Log.d("NotificationService", "   - Total notifications sent: $totalNotificationsSent")
            Log.d("NotificationService", "   - Approvers notified: ${approverIdsList.size}")
            Log.d("NotificationService", "   - Production heads notified: ${BusinessHeadIdsList.size}")
            Log.d("NotificationService", "   - Project: $projectName ($projectId)")
            Log.d("NotificationService", "   - Expense: $expenseId")
            Log.d("NotificationService", "   - Submitted by: $submittedBy")
            Log.d("NotificationService", "   - Amount: ₹${String.format("%.2f", amount)}")
            Log.d("NotificationService", "   - Category: $category")
            Log.d("NotificationService", "   - Budget exceeded: $budgetExceeded")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("NotificationService", "❌ Error sending expense submission notification: ${e.message}")
            e.printStackTrace()
            // Even if there's an error, try to send notifications to all approvers as fallback
            Log.d("NotificationService", "🔄 Attempting fallback after error: sending notifications to all approvers")
            sendNotificationsToAllApprovers(projectId, expenseId, submittedBy, amount, category, budgetExceeded)
        }
    }
    
    /**
     * Fallback: Send notifications to all approvers when project document cannot be found
     */
    private suspend fun sendNotificationsToAllApprovers(
        projectId: String,
        expenseId: String,
        submittedBy: String,
        amount: Double,
        category: String,
        budgetExceeded: Boolean
    ): Result<Unit> {
        return try {
            Log.d("NotificationService", "🔄 Fallback: Sending notifications to all approvers and production heads")
            
            // Get all users to find approvers
            Log.d("NotificationService", "🔄 Fallback: Fetching all users from Firestore...")
            val allUsers = try {
                authRepository.getAllUsers()
            } catch (e: Exception) {
                Log.e("NotificationService", "❌ Error fetching users in fallback: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
            
            Log.d("NotificationService", "📋 Fallback: Retrieved ${allUsers.size} total users from Firestore")
            
            val approverIdsList = mutableListOf<String>()
            val BusinessHeadIdsList = mutableListOf<String>()
            
            // Find all approvers and production heads
            allUsers.forEach { user ->
                Log.d("NotificationService", "🔍 Fallback: Checking user: ${user.name}, role: ${user.role}, phone: ${user.phone}, isActive: ${user.isActive}")
                when (user.role) {
                    com.cubiquitous.tracura.model.UserRole.APPROVER -> {
                        val phoneNumber = user.phone
                        if (!phoneNumber.isNullOrEmpty() && !approverIdsList.contains(phoneNumber)) {
                            approverIdsList.add(phoneNumber)
                            Log.d("NotificationService", "✅ Added fallback approver: ${user.name} ($phoneNumber)")
                        } else {
                            Log.w("NotificationService", "⚠️ Skipping approver ${user.name}: phone empty or already in list")
                        }
                    }
                    com.cubiquitous.tracura.model.UserRole.BUSINESS_HEAD -> {
                        val phoneNumber = user.phone
                        if (!phoneNumber.isNullOrEmpty() && !BusinessHeadIdsList.contains(phoneNumber)) {
                            BusinessHeadIdsList.add(phoneNumber)
                            Log.d("NotificationService", "✅ Added fallback production head: ${user.name} ($phoneNumber)")
                        } else {
                            Log.w("NotificationService", "⚠️ Skipping production head ${user.name}: phone empty or already in list")
                        }
                    }
                    else -> {
                        Log.d("NotificationService", "⏭️ Skipping user ${user.name}: role is ${user.role}")
                    }
                }
            }
            
            Log.d("NotificationService", "📋 Fallback: Found ${approverIdsList.size} approvers and ${BusinessHeadIdsList.size} production heads")
            if (approverIdsList.isEmpty() && BusinessHeadIdsList.isEmpty()) {
                Log.e("NotificationService", "❌ CRITICAL: No approvers or production heads found in fallback!")
                Log.e("NotificationService", "❌ This means notifications cannot be sent to anyone!")
                Log.e("NotificationService", "❌ Total users retrieved: ${allUsers.size}")
                Log.e("NotificationService", "❌ User roles found: ${allUsers.map { it.role.name }.distinct()}")
            }
            
            val projectName = "Project $projectId" // Fallback project name
            
            // Send notifications to all approvers
            approverIdsList.forEach { approverId ->
                val notification = Notification(
                    recipientId = approverId,
                    recipientRole = "APPROVER",
                    title = if (budgetExceeded) "Budget Exceeded - Expense Requires Approval" else "New Expense Submitted",
                    message = if (budgetExceeded) {
                        "Budget exceeded: New expense of ₹${String.format("%.2f", amount)} submitted by $submittedBy (Category: $category) - Requires your approval"
                    } else {
                        "New expense of ₹${String.format("%.2f", amount)} submitted by $submittedBy (Category: $category)"
                    },
                    type = NotificationType.EXPENSE_SUBMITTED,
                    projectId = projectId,
                    projectName = projectName,
                    relatedId = expenseId,
                    actionRequired = true,
                    navigationTarget = "pending_approvals/$projectId",
                    createdAt = Timestamp.now()
                )
                
                // Save notification to Firestore and send push notification
                val createResult = notificationRepository.createNotification(notification)
                if (createResult.isSuccess) {
                    val notificationId = createResult.getOrNull() ?: ""
                    val notificationWithId = notification.copy(id = notificationId)
                    
                    // Send push notification asynchronously
                    CoroutineScope(Dispatchers.IO).launch {
                        fcmPushNotificationService.sendPushNotificationFromModel(notificationWithId)
                    }
                    Log.d("NotificationService", "✅ Fallback notification created and push notification queued for approver: $approverId")
                } else {
                    Log.e("NotificationService", "❌ Failed to create fallback notification for approver: $approverId")
                }
            }
            
            // Send to production heads
            BusinessHeadIdsList.forEach { BusinessHeadId ->
                val notification = Notification(
                    recipientId = BusinessHeadId,
                    recipientRole = "BUSINESS_HEAD",
                    title = if (budgetExceeded) "Budget Exceeded - Expense Requires Approval" else "New Expense Submitted",
                    message = if (budgetExceeded) {
                        "Budget exceeded: New expense of ₹${String.format("%.2f", amount)} submitted by $submittedBy (Category: $category) - Requires your approval"
                    } else {
                        "New expense of ₹${String.format("%.2f", amount)} submitted by $submittedBy (Category: $category)"
                    },
                    type = NotificationType.EXPENSE_SUBMITTED,
                    projectId = projectId,
                    projectName = projectName,
                    relatedId = expenseId,
                    actionRequired = true,
                    navigationTarget = "pending_approvals/$projectId",
                    createdAt = Timestamp.now()
                )
                
                // Save notification to Firestore and send push notification
                val createResult = notificationRepository.createNotification(notification)
                if (createResult.isSuccess) {
                    val notificationId = createResult.getOrNull() ?: ""
                    val notificationWithId = notification.copy(id = notificationId)
                    
                    // Send push notification asynchronously
                    CoroutineScope(Dispatchers.IO).launch {
                        fcmPushNotificationService.sendPushNotificationFromModel(notificationWithId)
                    }
                    Log.d("NotificationService", "✅ Fallback notification created and push notification queued for production head: $BusinessHeadId")
                } else {
                    Log.e("NotificationService", "❌ Failed to create fallback notification for production head: $BusinessHeadId")
                }
            }
            
            Log.d("NotificationService", "✅ Fallback notifications sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationService", "❌ Error in fallback notification sending: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Send expense approval/rejection notification to the user who submitted the expense
     */
    suspend fun sendExpenseStatusNotification(
        expenseId: String,
        projectId: String,
        submittedByUserId: String,
        isApproved: Boolean,
        amount: Double,
        reviewerName: String,
        comments: String = ""
    ): Result<Unit> {
        return try {
            Log.d("NotificationService", "🔄 Sending expense status notification for expense: $expenseId")
            Log.d("NotificationService", "📋 Submitted by user ID: $submittedByUserId")
            Log.d("NotificationService", "📋 Reviewer name: $reviewerName")
            Log.d("NotificationService", "📋 Amount: $amount")
            Log.d("NotificationService", "📋 Is approved: $isApproved")
            
            // Get project details from correct path: customers/{customerId}/Projects/{projectId}
            val projectDoc = projectRepository.getProjectDocument(projectId)
            if (projectDoc == null || !projectDoc.exists()) {
                Log.e("NotificationService", "❌ Project not found: $projectId")
                return Result.failure(Exception("Project not found"))
            }
            
            val projectData = projectDoc.data ?: return Result.failure(Exception("Project data is null"))
            val projectName = projectData["name"] as? String ?: "Unknown Project"
            
            // Get user details to include in notification
            val allUsers = authRepository.getAllUsers()
            val submittedByUser = allUsers.find { it.uid == submittedByUserId }
            val submittedByUserName = submittedByUser?.name ?: "User"
            
            // Get user's phoneNumber as recipientId (notifications use phoneNumber, not UID)
            val recipientId = submittedByUser?.phone ?: submittedByUserId
            Log.d("NotificationService", "📋 Found user: $submittedByUserName (UID: $submittedByUserId, Phone: $recipientId)")
            
            // Validate that recipient ID is not empty
            if (recipientId.isEmpty()) {
                Log.e("NotificationService", "❌ Cannot send notification: recipient phone number is empty")
                return Result.failure(Exception("Recipient phone number is empty"))
            }
            
            // Create more detailed notification message
            val notificationTitle = if (isApproved) "✅ Expense Approved" else "❌ Expense Rejected"
            val notificationMessage = if (isApproved) {
                "Your expense of ₹${String.format("%.2f", amount)} in ${projectName} has been approved by $reviewerName"
            } else {
                val baseMessage = "Your expense of ₹${String.format("%.2f", amount)} in ${projectName} has been rejected by $reviewerName"
                if (comments.isNotEmpty()) {
                    "$baseMessage - Reason: $comments"
                } else {
                    baseMessage
                }
            }
            
            Log.d("NotificationService", "📋 Notification title: $notificationTitle")
            Log.d("NotificationService", "📋 Notification message: $notificationMessage")
            
            val notification = Notification(
                recipientId = recipientId,
                recipientRole = "USER",
                title = notificationTitle,
                message = notificationMessage,
                type = if (isApproved) NotificationType.EXPENSE_APPROVED else NotificationType.EXPENSE_REJECTED,
                projectId = projectId,
                projectName = projectName,
                relatedId = expenseId,
                actionRequired = false, // User doesn't need to take action, just informational
                navigationTarget = "expense_list/$projectId", // Navigate to expense list for the specific project
                createdAt = Timestamp.now()
            )
            
            // Save notification to Firestore and send push notification
            val createResult = notificationRepository.createNotification(notification)
            if (createResult.isSuccess) {
                val notificationId = createResult.getOrNull() ?: ""
                val notificationWithId = notification.copy(id = notificationId)
                
                // Send push notification asynchronously
                CoroutineScope(Dispatchers.IO).launch {
                    fcmPushNotificationService.sendPushNotificationFromModel(notificationWithId)
                }
                Log.d("NotificationService", "✅ Expense status notification created and push notification queued")
            } else {
                Log.e("NotificationService", "❌ Failed to create expense status notification")
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("NotificationService", "❌ Error sending expense status notification: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Send project assignment notification
     */
    suspend fun sendProjectAssignmentNotification(
        projectId: String,
        userId: String,
        assignedRole: String
    ): Result<Unit> {
        return try {
            Log.d("NotificationService", "🔄 Sending project assignment notification for user: $userId")
            
            // Get project details from correct path: customers/{customerId}/Projects/{projectId}
            val projectDoc = projectRepository.getProjectDocument(projectId)
            if (projectDoc == null || !projectDoc.exists()) {
                Log.e("NotificationService", "❌ Project not found: $projectId")
                return Result.failure(Exception("Project not found"))
            }
            
            val projectData = projectDoc.data ?: return Result.failure(Exception("Project data is null"))
            val projectName = projectData["name"] as? String ?: "Unknown Project"
            
            // Get user details
            val user = authRepository.getAllUsers().find { it.uid == userId }
            if (user == null) {
                Log.e("NotificationService", "❌ User not found: $userId")
                return Result.failure(Exception("User not found"))
            }
            
            val notification = Notification(
                recipientId = userId,
                recipientRole = user.role.name,
                title = "New Project Assignment",
                message = "You have been assigned as $assignedRole to project: ${projectName}",
                type = NotificationType.PROJECT_ASSIGNMENT,
                projectId = projectId,
                projectName = projectName,
                actionRequired = true,
                navigationTarget = when (user.role) {
                    com.cubiquitous.tracura.model.UserRole.USER -> "user_project_dashboard/$projectId"
                    com.cubiquitous.tracura.model.UserRole.APPROVER -> "approver_project_dashboard/$projectId"
                    com.cubiquitous.tracura.model.UserRole.BUSINESS_HEAD -> "BUSINESS_HEAD_project_dashboard/$projectId"
                    else -> "project_selection"
                },
                createdAt = Timestamp.now()
            )
            
            // Save notification to Firestore and send push notification
            val createResult = notificationRepository.createNotification(notification)
            if (createResult.isSuccess) {
                val notificationId = createResult.getOrNull() ?: ""
                val notificationWithId = notification.copy(id = notificationId)
                
                // Send push notification asynchronously
                CoroutineScope(Dispatchers.IO).launch {
                    fcmPushNotificationService.sendPushNotificationFromModel(notificationWithId)
                }
                Log.d("NotificationService", "✅ Project assignment notification created and push notification queued")
            } else {
                Log.e("NotificationService", "❌ Failed to create project assignment notification")
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("NotificationService", "❌ Error sending project assignment notification: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Send pending approval notification to production heads
     */
    suspend fun sendPendingApprovalNotification(
        projectId: String,
        pendingCount: Int
    ): Result<Unit> {
        return try {
            Log.d("NotificationService", "🔄 Sending pending approval notification for project: $projectId")
            
            // Get project details from correct path: customers/{customerId}/Projects/{projectId}
            val projectDoc = projectRepository.getProjectDocument(projectId)
            if (projectDoc == null || !projectDoc.exists()) {
                Log.e("NotificationService", "❌ Project not found: $projectId")
                return Result.failure(Exception("Project not found"))
            }
            
            val projectData = projectDoc.data ?: return Result.failure(Exception("Project data is null"))
            val projectName = projectData["name"] as? String ?: "Unknown Project"
            @Suppress("UNCHECKED_CAST")
            val BusinessHeadIds = (projectData["BusinessHeadIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            
            // Get all users to find production heads
            val allUsers = authRepository.getAllUsers()
            
            // Send to project-specific production heads
            BusinessHeadIds.forEach { BusinessHeadId ->
                val user = allUsers.find { it.uid == BusinessHeadId }
                if (user?.role == com.cubiquitous.tracura.model.UserRole.BUSINESS_HEAD) {
                    val notification = Notification(
                        recipientId = BusinessHeadId,
                        recipientRole = "BUSINESS_HEAD",
                        title = "Pending Approvals",
                        message = "$pendingCount expenses awaiting approval in ${projectName}",
                        type = NotificationType.PENDING_APPROVAL,
                        projectId = projectId,
                        projectName = projectName,
                        actionRequired = true,
                        navigationTarget = "pending_approvals/$projectId",
                        createdAt = Timestamp.now()
                    )
                    
                    // Save notification to Firestore and send push notification
                    val createResult = notificationRepository.createNotification(notification)
                    if (createResult.isSuccess) {
                        val notificationId = createResult.getOrNull() ?: ""
                        val notificationWithId = notification.copy(id = notificationId)
                        
                        // Send push notification asynchronously
                        CoroutineScope(Dispatchers.IO).launch {
                            fcmPushNotificationService.sendPushNotificationFromModel(notificationWithId)
                        }
                        Log.d("NotificationService", "✅ Pending approval notification created and push notification queued")
                    } else {
                        Log.e("NotificationService", "❌ Failed to create pending approval notification")
                    }
                }
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("NotificationService", "❌ Error sending pending approval notification: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Send a generic notification
     */
    suspend fun sendNotification(notification: Notification): Result<Unit> {
        return try {
            Log.d("NotificationService", "🔄 Sending notification: ${notification.title}")
            Log.d("NotificationService", "📋 Notification details:")
            Log.d("NotificationService", "   - recipientId: ${notification.recipientId}")
            Log.d("NotificationService", "   - recipientRole: ${notification.recipientRole}")
            Log.d("NotificationService", "   - type: ${notification.type}")
            Log.d("NotificationService", "   - projectId: ${notification.projectId}")
            
            // Ensure createdAt is set
            val notificationWithTimestamp = if (notification.createdAt.seconds == 0L) {
                notification.copy(createdAt = Timestamp.now())
            } else {
                notification
            }
            
            // Save notification to Firestore and send push notification
            val createResult = notificationRepository.createNotification(notificationWithTimestamp)
            if (createResult.isSuccess) {
                val notificationId = createResult.getOrNull() ?: ""
                val notificationWithId = notificationWithTimestamp.copy(id = notificationId)
                
                // Send push notification asynchronously
                CoroutineScope(Dispatchers.IO).launch {
                    fcmPushNotificationService.sendPushNotificationFromModel(notificationWithId)
                }
                Log.d("NotificationService", "✅ Generic notification created and push notification queued")
                Result.success(Unit)
            } else {
                Log.e("NotificationService", "❌ Failed to create generic notification")
                Result.failure(createResult.exceptionOrNull() ?: Exception("Failed to create notification"))
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "❌ Error sending notification: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Send delegation change notification to approver
     */
    suspend fun sendDelegationChangeNotification(
        approverId: String,
        approverPhone: String,
        projectId: String,
        projectName: String,
        changeDescription: String,
        changedBy: String
    ): Result<Unit> {
        return try {
            Log.d("NotificationService", "🔄 Sending delegation change notification to: $approverPhone")
            
            val notification = Notification(
                recipientId = approverPhone,
                recipientRole = "APPROVER",
                title = "Delegation Change",
                message = changeDescription,
                type = NotificationType.DELEGATION_CHANGED,
                projectId = projectId,
                projectName = projectName,
                relatedId = approverId,
                actionRequired = false,
                navigationTarget = "approver_project_dashboard/$projectId",
                createdAt = Timestamp.now()
            )
            
            // Save notification to Firestore and send push notification
            val createResult = notificationRepository.createNotification(notification)
            if (createResult.isSuccess) {
                val notificationId = createResult.getOrNull() ?: ""
                val notificationWithId = notification.copy(id = notificationId)
                
                // Send push notification asynchronously
                CoroutineScope(Dispatchers.IO).launch {
                    fcmPushNotificationService.sendPushNotificationFromModel(notificationWithId)
                }
                Log.d("NotificationService", "✅ Delegation change notification created and push notification queued")
            } else {
                Log.e("NotificationService", "❌ Failed to create delegation change notification")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationService", "❌ Error sending delegation change notification: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Send temporary approver assignment notification with accept/reject actions
     */
    suspend fun sendTemporaryApproverAssignmentNotification(
        projectId: String,
        approverId: String,
        approverName: String,
        approverPhone: String,
        assignedByName: String,
        startDate: Timestamp,
        expiringDate: Timestamp?
    ): Result<Unit> {
        return try {
            Log.d("NotificationService", "🔄 Sending temporary approver assignment notification")
            Log.d("NotificationService", "📋 To: $approverName ($approverPhone)")
            Log.d("NotificationService", "📋 Project ID: $projectId")
            Log.d("NotificationService", "📋 Approver ID: $approverId")
            
            // Get project details
            val projectDoc = firestore.collection("projects").document(projectId).get().await()
            if (!projectDoc.exists()) {
                Log.e("NotificationService", "❌ Project not found: $projectId")
                return Result.failure(Exception("Project not found"))
            }
            
            val projectData = projectDoc.data ?: return Result.failure(Exception("Project data is null"))
            val projectName = projectData["name"] as? String ?: "Unknown Project"
            
            // Format dates
            val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            val startDateStr = dateFormat.format(startDate.toDate())
            val expiryDateStr = expiringDate?.let { dateFormat.format(it.toDate()) } ?: "Ongoing"
            
            // Create notification as informational only (no accept/reject actions)
            val notification = Notification(
                recipientId = approverPhone, // Use phone number to match user's phone
                recipientRole = "APPROVER",
                title = "Temporary Approver Assignment",
                message = "$assignedByName has assigned you as a temporary approver for '$projectName' from $startDateStr to $expiryDateStr.",
                type = NotificationType.TEMPORARY_APPROVER_ASSIGNMENT,
                projectId = projectId,
                projectName = projectName,
                relatedId = approverId, // Store approver ID for reference
                actionRequired = false, // Changed to false - no accept/reject buttons
                navigationTarget = "approver_project_dashboard/$projectId"
            )
            
            Log.d("NotificationService", "📋 Creating notification:")
            Log.d("NotificationService", "   - Title: ${notification.title}")
            Log.d("NotificationService", "   - Message: ${notification.message}")
            Log.d("NotificationService", "   - Action Required: ${notification.actionRequired}")
            
            // Save notification to Firestore and send push notification
            val notificationWithTimestamp = notification.copy(createdAt = Timestamp.now())
            val createResult = notificationRepository.createNotification(notificationWithTimestamp)
            if (createResult.isSuccess) {
                val notificationId = createResult.getOrNull() ?: ""
                val notificationWithId = notificationWithTimestamp.copy(id = notificationId)
                
                // Send push notification asynchronously
                CoroutineScope(Dispatchers.IO).launch {
                    fcmPushNotificationService.sendPushNotificationFromModel(notificationWithId)
                }
                Log.d("NotificationService", "✅ Temporary approver assignment notification created and push notification queued")
            } else {
                Log.e("NotificationService", "❌ Failed to create temporary approver assignment notification")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationService", "❌ Error sending temporary approver assignment notification: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Send notification about delegation expiry to the user
     */
    suspend fun sendDelegationExpiryNotification(
        projectId: String,
        projectName: String,
        approverId: String,
        approverName: String,
        approverPhone: String
    ): Result<Unit> {
        return try {
            Log.d("NotificationService", "🔄 Sending delegation expiry notification")
            Log.d("NotificationService", "📋 To: $approverName ($approverPhone)")
            Log.d("NotificationService", "📋 Project: $projectName")
            
            // Create notification about delegation expiry
            val notification = Notification(
                recipientId = approverPhone, // Use phone number to match user's phone
                recipientRole = "APPROVER",
                title = "Delegation Expired",
                message = "Your temporary approver assignment for project '$projectName' has expired. You have been automatically removed from the project team.",
                type = NotificationType.DELEGATION_EXPIRED,
                projectId = projectId,
                projectName = projectName,
                relatedId = approverId,
                actionRequired = false, // No action required, just informational
                navigationTarget = "user_dashboard"
            )
            
            Log.d("NotificationService", "📋 Creating delegation expiry notification:")
            Log.d("NotificationService", "   - Title: ${notification.title}")
            Log.d("NotificationService", "   - Message: ${notification.message}")
            
            // Save notification to Firestore and send push notification
            val notificationWithTimestamp = notification.copy(createdAt = Timestamp.now())
            val createResult = notificationRepository.createNotification(notificationWithTimestamp)
            if (createResult.isSuccess) {
                val notificationId = createResult.getOrNull() ?: ""
                val notificationWithId = notificationWithTimestamp.copy(id = notificationId)
                
                // Send push notification asynchronously
                CoroutineScope(Dispatchers.IO).launch {
                    fcmPushNotificationService.sendPushNotificationFromModel(notificationWithId)
                }
                Log.d("NotificationService", "✅ Delegation expiry notification created and push notification queued")
            } else {
                Log.e("NotificationService", "❌ Failed to create delegation expiry notification")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationService", "❌ Error sending delegation expiry notification: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Send notification about delegation removal to the approver
     */
    suspend fun sendDelegationRemovalNotification(
        approverId: String,
        approverPhone: String,
        projectId: String,
        projectName: String
    ): Result<Unit> {
        return try {
            Log.d("NotificationService", "🔄 Sending delegation removal notification")
            Log.d("NotificationService", "📋 To: $approverPhone")
            Log.d("NotificationService", "📋 Project: $projectName")
            
            // Create notification about delegation removal
            val notification = Notification(
                recipientId = approverPhone, // Use phone number to match user's phone
                recipientRole = "APPROVER",
                title = "Delegation Removed",
                message = "Your temporary approver assignment for project '$projectName' has been removed by the production head.",
                type = NotificationType.DELEGATION_REMOVED,
                projectId = projectId,
                projectName = projectName,
                relatedId = approverId,
                actionRequired = false, // No action required, just informational
                navigationTarget = "user_dashboard"
            )
            
            Log.d("NotificationService", "📋 Creating delegation removal notification:")
            Log.d("NotificationService", "   - Title: ${notification.title}")
            Log.d("NotificationService", "   - Message: ${notification.message}")
            
            // Save notification to Firestore and send push notification
            val notificationWithTimestamp = notification.copy(createdAt = Timestamp.now())
            val createResult = notificationRepository.createNotification(notificationWithTimestamp)
            if (createResult.isSuccess) {
                val notificationId = createResult.getOrNull() ?: ""
                val notificationWithId = notificationWithTimestamp.copy(id = notificationId)
                
                // Send push notification asynchronously
                CoroutineScope(Dispatchers.IO).launch {
                    fcmPushNotificationService.sendPushNotificationFromModel(notificationWithId)
                }
                Log.d("NotificationService", "✅ Delegation removal notification created and push notification queued")
            } else {
                Log.e("NotificationService", "❌ Failed to create delegation removal notification")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationService", "❌ Error sending delegation removal notification: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Send confirmation notification to approver about their response (accept/reject)
     */
    suspend fun sendDelegationResponseConfirmation(
        approverId: String,
        approverPhone: String,
        projectId: String,
        projectName: String,
        isAccepted: Boolean,
        responseMessage: String
    ): Result<Unit> {
        return try {
            Log.d("NotificationService", "🔄 Sending delegation response confirmation")
            Log.d("NotificationService", "📋 To: $approverPhone")
            Log.d("NotificationService", "📋 Project: $projectName")
            Log.d("NotificationService", "📋 Response: ${if (isAccepted) "Accepted" else "Rejected"}")
            
            val action = if (isAccepted) "accepted" else "rejected"
            val title = if (isAccepted) "Delegation Accepted" else "Delegation Rejected"
            val message = if (responseMessage.isNotEmpty()) {
                "You have $action the temporary approver assignment for project '$projectName'. Your response: \"$responseMessage\""
            } else {
                "You have $action the temporary approver assignment for project '$projectName'."
            }
            
            // Create notification about delegation response
            val notification = Notification(
                recipientId = approverPhone, // Use phone number to match user's phone
                recipientRole = "APPROVER",
                title = title,
                message = message,
                type = NotificationType.DELEGATION_RESPONSE,
                projectId = projectId,
                projectName = projectName,
                relatedId = approverId,
                actionRequired = false, // No action required, just confirmation
                navigationTarget = if (isAccepted) "approver_project_dashboard/$projectId" else "user_dashboard"
            )
            
            Log.d("NotificationService", "📋 Creating delegation response confirmation:")
            Log.d("NotificationService", "   - Title: ${notification.title}")
            Log.d("NotificationService", "   - Message: ${notification.message}")
            
            // Save notification to Firestore and send push notification
            val notificationWithTimestamp = notification.copy(createdAt = Timestamp.now())
            val createResult = notificationRepository.createNotification(notificationWithTimestamp)
            if (createResult.isSuccess) {
                val notificationId = createResult.getOrNull() ?: ""
                val notificationWithId = notificationWithTimestamp.copy(id = notificationId)
                
                // Send push notification asynchronously
                CoroutineScope(Dispatchers.IO).launch {
                    fcmPushNotificationService.sendPushNotificationFromModel(notificationWithId)
                }
                Log.d("NotificationService", "✅ Delegation response confirmation created and push notification queued")
            } else {
                Log.e("NotificationService", "❌ Failed to create delegation response confirmation")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationService", "❌ Error sending delegation response confirmation: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
} 