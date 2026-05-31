package com.cubiquitous.tracura.utils

import android.util.Log
import com.cubiquitous.tracura.model.Notification

/**
 * Handles navigation for notifications based on the screen type
 * Matches guide's NavigationManager.handleNavigation behavior
 * Maps screen types to appropriate navigation actions
 */
object NotificationNavigationHandler {
    
    /**
     * Handles navigation based on notification data with role-aware flow
     * Matches guide's handleNavigation method
     * 
     * @param notification The notification that was clicked
     * @param currentRole Current user role (BUSINESSHEAD, APPROVER, or USER)
     * @param currentProjectId Optional current project ID if already in a project view
     * @param onNavigateToChat Callback to navigate to chat screen (projectId, chatId, otherUserName)
     * @param onNavigateToExpense Callback to navigate to expense detail (expenseId)
     * @param onNavigateToPendingApprovals Callback to navigate to pending approvals (projectId)
     * @param onNavigateToProjectReview Callback to navigate to project review (projectId)
     * @param onNavigateToProjectDashboard Callback to navigate to project dashboard (projectId)
     * @param onDeleteNotification Callback to delete notification from local storage (notificationId)
     * @param onDismiss Callback to dismiss the notification popup
     */
    fun handleNotificationNavigation(
        notification: Notification,
        currentRole: String? = null,
        currentProjectId: String? = null,
        onNavigateToChat: (String, String, String) -> Unit = { _, _, _ -> },
        onNavigateToExpense: (String) -> Unit = { _ -> },
        onNavigateToPendingApprovals: (String) -> Unit = { _ -> },
        onNavigateToProjectReview: (String) -> Unit = { _ -> },
        onNavigateToProjectDashboard: (String) -> Unit = { _ -> },
        onDeleteNotification: (String) -> Unit = { _ -> },
        onDismiss: () -> Unit = {}
    ) {
        // Extract navigation parameters from notification
        val screen = notification.navigationTarget.trim()
        val notificationType = notification.type.name
        
        // Check type field to determine if expense notification should be chat
        val isExpenseChat = notificationType == "EXPENSE_CHAT_STAFF" || 
                           notificationType == "EXPENSE_CHAT_CUSTOMER" ||
                           notification.navigationTarget.contains("expense_chat", ignoreCase = true)
        
        // Determine actual screen - if type indicates expense_chat, override screen
        var actualScreen = screen
        if (isExpenseChat && (screen == "expense_detail" || screen == "expense_review")) {
            actualScreen = "expense_chat"
        }
        
        val projectId = notification.projectId.trim()
        val chatId = notification.relatedId.trim() // For chat notifications
        val expenseId = notification.relatedId.trim() // For expense notifications
        val phaseId = "" // Extract from data if needed
        val requestId = "" // Extract from data if needed
        val customerId = "" // Extract from data if needed
        
        Log.d("NotificationNavigation", "🔔 Handling navigation for notification: ${notification.title}")
        Log.d("NotificationNavigation", "   Screen: '$actualScreen', ProjectId: '$projectId', Type: '$notificationType'")
        
        // Determine if we need to navigate to project first
        val requiresProject = listOf(
            "project_detail", "chat_detail", "expense_detail", 
            "expense_chat", "phase_detail", "project_creation"
        ).contains(actualScreen)
        
        if (requiresProject && projectId.isEmpty()) {
            Log.w("NotificationNavigation", "   ⚠️ Screen $actualScreen requires projectId but none provided")
            return
        }
        
        // Remove notification from local storage when clicked (matches guide behavior)
        onDeleteNotification(notification.id)
        Log.d("NotificationNavigation", "   🗑️ Removed notification from local storage: ${notification.id}")
        
        // Map screen types from guide to navigation actions
        when {
            // Chat notifications (including expense chat)
            actualScreen == "chat_detail" || actualScreen == "chat_screen" || 
            actualScreen == "expense_chat" ||
            notificationType.contains("CHAT", ignoreCase = true) -> {
                if (projectId.isNotEmpty() && chatId.isNotEmpty()) {
                    // Extract otherUserName from notification message or use default
                    val otherUserName = extractUserNameFromMessage(notification.message) ?: "User"
                    Log.d("NotificationNavigation", "   → Navigating to Chat: projectId=$projectId, chatId=$chatId")
                    onNavigateToChat(projectId, chatId, otherUserName)
                    onDismiss()
                } else if (projectId.isNotEmpty()) {
                    // Navigate to chat list if chatId is not available
                    Log.d("NotificationNavigation", "   → Navigating to Project Dashboard (chat list): projectId=$projectId")
                    onNavigateToProjectDashboard(projectId)
                    onDismiss()
                }
            }
            
            // Expense notifications
            actualScreen == "expense_detail" || actualScreen == "expense_review" ||
            notificationType.contains("EXPENSE", ignoreCase = true) -> {
                if (expenseId.isNotEmpty()) {
                    Log.d("NotificationNavigation", "   → Navigating to Expense: expenseId=$expenseId")
                    onNavigateToExpense(expenseId)
                    onDismiss()
                } else if (projectId.isNotEmpty()) {
                    // Navigate to pending approvals if expenseId is not available
                    Log.d("NotificationNavigation", "   → Navigating to Pending Approvals: projectId=$projectId")
                    onNavigateToPendingApprovals(projectId)
                    onDismiss()
                }
            }
            
            // Project review notifications
            actualScreen == "project_review" -> {
                if (projectId.isNotEmpty()) {
                    Log.d("NotificationNavigation", "   → Navigating to Project Review: projectId=$projectId")
                    onNavigateToProjectReview(projectId)
                    onDismiss()
                }
            }
            
            // Project detail/dashboard notifications
            actualScreen == "project_detail" || actualScreen == "project_creation" ||
            actualScreen.isEmpty() -> {
                if (projectId.isNotEmpty()) {
                    Log.d("NotificationNavigation", "   → Navigating to Project Dashboard: projectId=$projectId")
                    onNavigateToProjectDashboard(projectId)
                    onDismiss()
                }
            }
            
            // Pending approvals
            actualScreen.contains("pending", ignoreCase = true) || 
            notificationType.contains("PENDING", ignoreCase = true) ||
            notificationType.contains("APPROVAL", ignoreCase = true) -> {
                if (projectId.isNotEmpty()) {
                    Log.d("NotificationNavigation", "   → Navigating to Pending Approvals: projectId=$projectId")
                    onNavigateToPendingApprovals(projectId)
                    onDismiss()
                }
            }
            
            // Default: navigate to project dashboard if projectId is available
            else -> {
                if (projectId.isNotEmpty()) {
                    Log.d("NotificationNavigation", "   → Default navigation to Project Dashboard: projectId=$projectId")
                    onNavigateToProjectDashboard(projectId)
                    onDismiss()
                } else {
                    Log.w("NotificationNavigation", "   ⚠️ No navigation available - missing projectId")
                }
            }
        }
    }
    
    /**
     * Extracts user name from notification message
     * Example: "Admin sent: Hi" -> "Admin"
     */
    private fun extractUserNameFromMessage(message: String): String? {
        return try {
            val parts = message.split(" sent:")
            if (parts.isNotEmpty()) {
                parts[0].trim()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
