package com.cubiquitous.tracura.model

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val recipientId: String = "",
    val recipientRole: String = "", // USER, APPROVER, BUSINESS_HEAD
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.INFO,
    val projectId: String = "",
    val projectName: String = "",
    val relatedId: String = "", // Could be expenseId, userId, etc.
    val isRead: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val actionRequired: Boolean = false,
    val navigationTarget: String = "", // Where to navigate when clicked
    val expiresAt: Timestamp? = null // Expiration time for the notification (11:59 PM of relevant day)
) {
    /**
     * Check if notification is expired
     * Matches guide's isExpired computed property
     */
    val isExpired: Boolean
        get() = expiresAt?.let { 
            val now = Timestamp.now()
            now.seconds > it.seconds 
        } ?: false
}

enum class NotificationType {
    PROJECT_ASSIGNMENT,     // When assigned to a project
    PROJECT_CHANGED,        // When project details are modified
    EXPENSE_SUBMITTED,      // When expense is submitted (for approvers)
    EXPENSE_APPROVED,       // When expense is approved (for users)
    EXPENSE_REJECTED,       // When expense is rejected (for users)
    PENDING_APPROVAL,       // General pending approval notifications
    ROLE_ASSIGNMENT,        // When role is assigned/changed
    TEMPORARY_APPROVER_ASSIGNMENT, // When assigned as temporary approver
    DELEGATION_CHANGED,     // When delegation settings are modified
    DELEGATION_EXPIRED,     // When delegation assignment expires
    DELEGATION_REMOVED,     // When delegation assignment is removed
    DELEGATION_RESPONSE,    // When approver responds to delegation (accept/reject)
    CHAT_MESSAGE,           // New chat message notification
    INFO                    // General information
}

data class NotificationBadge(
    val count: Int = 0,
    val hasUnread: Boolean = false
)

data class ProjectNotificationSummary(
    val projectId: String,
    val projectName: String,
    val totalNotifications: Int = 0,
    val unreadCount: Int = 0,
    val latestNotification: Notification? = null,
    val pendingApprovals: Int = 0,
    val expenseUpdates: Int = 0
)

 