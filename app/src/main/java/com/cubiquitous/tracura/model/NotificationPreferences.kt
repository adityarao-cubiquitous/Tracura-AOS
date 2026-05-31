package com.cubiquitous.tracura.model

data class NotificationPreferences(
    val pushNotifications: Boolean = true,
    val expenseSubmitted: Boolean = true,
    val expenseApproved: Boolean = true,
    val expenseRejected: Boolean = true,
    val projectAssignment: Boolean = true,
    val pendingApprovals: Boolean = true
) 