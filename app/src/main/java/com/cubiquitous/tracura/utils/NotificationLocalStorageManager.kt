package com.cubiquitous.tracura.utils

import android.content.Context
import android.content.SharedPreferences
import com.cubiquitous.tracura.model.Notification
import com.cubiquitous.tracura.model.NotificationType
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date
import java.util.Calendar
import java.util.Locale

/**
 * Utility class to manage local storage of notifications using SharedPreferences
 * Stores notifications received from FCM for offline access and display
 */
class NotificationLocalStorageManager(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "notification_local_storage"
        private const val KEY_NOTIFICATIONS = "notifications_list"
        private const val MAX_NOTIFICATIONS_PER_PROJECT = 100 // Limit per project
        private const val MAX_TOTAL_NOTIFICATIONS = 1000 // Limit total (if no project filter)
    }
    
    /**
     * Save a notification to local storage
     * If notification with same ID exists, it will be updated
     */
    fun saveNotification(notification: Notification) {
        try {
            val allNotifications = getAllNotifications().toMutableList()
            
            // Check if notification with same ID already exists
            val existingNotification = allNotifications.find { it.id == notification.id }
            
            // Calculate expiration time based on notification content
            val expiresAt = calculateExpirationTime(notification)
            
            if (existingNotification != null) {
                // If notification exists, preserve the original timestamp to prevent overwriting
                // This ensures that when notifications are saved multiple times (e.g., duplicate saves),
                // the original timestamp is maintained
                val preservedTimestamp = existingNotification.createdAt
                val preservedExpiresAt = existingNotification.expiresAt ?: expiresAt
                
                // Update existing notification but preserve original timestamp and expiration
                val updatedNotification = notification.copy(
                    createdAt = preservedTimestamp,
                    expiresAt = preservedExpiresAt
                )
                allNotifications.removeAll { it.id == notification.id }
                allNotifications.add(0, updatedNotification)
                
                android.util.Log.d("NotificationLocalStorage", "🔄 Updated existing notification: ${notification.title} (ID: ${notification.id}, preserved timestamp: ${preservedTimestamp.seconds}, expiresAt: ${preservedExpiresAt?.seconds})")
            } else {
                // Add new notification with calculated expiration time
                val notificationWithExpiration = notification.copy(expiresAt = expiresAt)
                allNotifications.add(0, notificationWithExpiration)
                android.util.Log.d("NotificationLocalStorage", "➕ Added new notification: ${notification.title} (ID: ${notification.id}, timestamp: ${notification.createdAt.seconds}, expiresAt: ${expiresAt?.seconds})")
            }
            
            // Sort by date (newest first) before limiting
            allNotifications.sortByDescending { it.createdAt.seconds }
            
            // Limit notifications per project if projectId is specified
            if (notification.projectId.isNotBlank()) {
                val projectNotifications = allNotifications.filter { it.projectId == notification.projectId }
                if (projectNotifications.size > MAX_NOTIFICATIONS_PER_PROJECT) {
                    // Get IDs of oldest notifications to remove
                    val toRemoveIds = projectNotifications
                        .sortedBy { it.createdAt.seconds }
                        .take(projectNotifications.size - MAX_NOTIFICATIONS_PER_PROJECT)
                        .map { it.id }
                    // Remove oldest notifications for this project
                    allNotifications.removeAll { notif ->
                        notif.projectId == notification.projectId && toRemoveIds.contains(notif.id)
                    }
                }
            }
            
            // Also limit total notifications (regardless of project filter)
            if (allNotifications.size > MAX_TOTAL_NOTIFICATIONS) {
                // Keep only the newest MAX_TOTAL_NOTIFICATIONS
                val toKeep = allNotifications.take(MAX_TOTAL_NOTIFICATIONS)
                allNotifications.clear()
                allNotifications.addAll(toKeep)
            }
            
            // Save to SharedPreferences - convert to NotificationData to preserve timestamp
            val json = gson.toJson(allNotifications.map { it.toNotificationData() })
            sharedPreferences.edit()
                .putString(KEY_NOTIFICATIONS, json)
                .apply()
            
            android.util.Log.d("NotificationLocalStorage", "✅ Saved notification: ${notification.title} (ID: ${notification.id}, timestamp: ${allNotifications.first().createdAt.seconds})")
        } catch (e: Exception) {
            android.util.Log.e("NotificationLocalStorage", "❌ Error saving notification: ${e.message}", e)
        }
    }
    
    /**
     * Save multiple notifications to local storage
     */
    fun saveNotifications(notifications: List<Notification>) {
        try {
            val allNotifications = getAllNotifications().toMutableList()
            
            notifications.forEach { notification ->
                // Remove existing notification with same ID if present
                allNotifications.removeAll { it.id == notification.id }
                
                // Calculate expiration time based on notification content
                val expiresAt = calculateExpirationTime(notification)
                val notificationWithExpiration = notification.copy(expiresAt = expiresAt)
                
                // Add new notification at the beginning
                allNotifications.add(0, notificationWithExpiration)
            }
            
            // Sort by timestamp (most recent first)
            allNotifications.sortByDescending { it.createdAt.seconds }
            
            // Group by project and limit per project
            val projectGroups = allNotifications.groupBy { it.projectId }
            val limited = mutableListOf<Notification>()
            
            projectGroups.forEach { (projectId, projectNotifs) ->
                if (projectId.isNotBlank()) {
                    // Limit per project (keep newest MAX_NOTIFICATIONS_PER_PROJECT)
                    val sorted = projectNotifs.sortedByDescending { it.createdAt.seconds }
                    limited.addAll(sorted.take(MAX_NOTIFICATIONS_PER_PROJECT))
                } else {
                    // No project ID - add all (will be limited by total later)
                    limited.addAll(projectNotifs)
                }
            }
            
            // Sort again and limit total (keep newest MAX_TOTAL_NOTIFICATIONS)
            limited.sortByDescending { it.createdAt.seconds }
            val limitedNotifications = if (limited.size > MAX_TOTAL_NOTIFICATIONS) {
                limited.take(MAX_TOTAL_NOTIFICATIONS)
            } else {
                limited
            }
            
            // Save to SharedPreferences - convert to NotificationData to preserve timestamp
            val json = gson.toJson(limitedNotifications.map { it.toNotificationData() })
            sharedPreferences.edit()
                .putString(KEY_NOTIFICATIONS, json)
                .apply()
            
            android.util.Log.d("NotificationLocalStorage", "✅ Saved ${notifications.size} notifications")
        } catch (e: Exception) {
            android.util.Log.e("NotificationLocalStorage", "❌ Error saving notifications: ${e.message}", e)
        }
    }
    
    /**
     * Get all notifications from local storage
     * Filters out expired notifications automatically
     */
    fun getAllNotifications(): List<Notification> {
        return try {
            val json = sharedPreferences.getString(KEY_NOTIFICATIONS, null)
            if (json != null) {
                val type = object : TypeToken<List<NotificationData>>() {}.type
                val notificationDataList = gson.fromJson<List<NotificationData>>(json, type)
                
                val now = Timestamp.now()
                
                // Convert NotificationData to Notification and filter out expired ones
                notificationDataList.mapNotNull { data ->
                    val expiresAt = if (data.expiresAtSeconds > 0) {
                        Timestamp(data.expiresAtSeconds, 0)
                    } else {
                        null
                    }
                    
                    // Skip expired notifications
                    if (expiresAt != null && expiresAt.seconds < now.seconds) {
                        android.util.Log.d("NotificationLocalStorage", "⏰ Filtered out expired notification: ${data.title} (expired at: ${expiresAt.seconds})")
                        null
                    } else {
                        Notification(
                            id = data.id,
                            recipientId = data.recipientId,
                            recipientRole = data.recipientRole,
                            title = data.title,
                            message = data.message,
                            type = try {
                                NotificationType.valueOf(data.type)
                            } catch (e: Exception) {
                                NotificationType.INFO
                            },
                            projectId = data.projectId,
                            projectName = data.projectName,
                            relatedId = data.relatedId,
                            isRead = data.isRead,
                            createdAt = if (data.createdAtSeconds > 0) {
                                Timestamp(data.createdAtSeconds, 0)
                            } else {
                                Timestamp.now()
                            },
                            actionRequired = data.actionRequired,
                            navigationTarget = data.navigationTarget,
                            expiresAt = expiresAt
                        )
                    }
                }.sortedByDescending { it.createdAt.seconds }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationLocalStorage", "❌ Error loading notifications: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get notifications for a specific user
     * Includes notifications with empty recipientId (FCM notifications received when user wasn't logged in)
     * For production heads, can check multiple ID formats (phone, UID, etc.)
     */
    fun getNotificationsForUser(userId: String, alternateUserIds: List<String> = emptyList()): List<Notification> {
        val allUserIds = listOf(userId) + alternateUserIds
        return getAllNotifications().filter { notification ->
            // Match any of the provided user IDs
            allUserIds.contains(notification.recipientId) ||
            // Include notifications with empty/blank recipientId (FCM notifications received when user wasn't logged in)
            notification.recipientId.isEmpty() ||
            notification.recipientId.isBlank()
        }
    }
    
    /**
     * Get notifications for a specific project
     * Returns notifications filtered by project ID (excluding expired)
     * Matches guide's getNotificationsForProject method
     * 
     * @param projectId Project ID to filter by
     * @return List of notifications for the project (excluding expired)
     */
    fun getNotificationsForProject(projectId: String): List<Notification> {
        val trimmedProjectId = projectId.trim()
        if (trimmedProjectId.isEmpty()) {
            return emptyList()
        }
        // getAllNotifications() already filters expired notifications
        return getAllNotifications().filter { 
            val notifProjectId = it.projectId.trim()
            notifProjectId.equals(trimmedProjectId, ignoreCase = true) || 
            notifProjectId == trimmedProjectId
        }
    }
    
    /**
     * Get unread notifications for a user
     * Includes notifications with empty recipientId (FCM notifications received when user wasn't logged in)
     */
    fun getUnreadNotificationsForUser(userId: String): List<Notification> {
        return getAllNotifications().filter { 
            !it.isRead && (
                it.recipientId == userId || 
                it.recipientId.isEmpty() || 
                it.recipientId.isBlank()
            )
        }
    }
    
    /**
     * Mark a notification as read
     */
    fun markNotificationAsRead(notificationId: String) {
        try {
            val allNotifications = getAllNotifications().toMutableList()
            val notification = allNotifications.find { it.id == notificationId }
            
            if (notification != null) {
                val updatedNotification = notification.copy(isRead = true)
                allNotifications.removeAll { it.id == notificationId }
                allNotifications.add(0, updatedNotification)
                
                val json = gson.toJson(allNotifications.map { it.toNotificationData() })
                sharedPreferences.edit()
                    .putString(KEY_NOTIFICATIONS, json)
                    .apply()
                
                android.util.Log.d("NotificationLocalStorage", "✅ Marked notification as read: $notificationId")
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationLocalStorage", "❌ Error marking notification as read: ${e.message}", e)
        }
    }
    
    /**
     * Mark all notifications as read for a user
     */
    fun markAllNotificationsAsRead(userId: String) {
        try {
            val allNotifications = getAllNotifications().toMutableList()
            val updatedNotifications = allNotifications.map { notification ->
                if (notification.recipientId == userId && !notification.isRead) {
                    notification.copy(isRead = true)
                } else {
                    notification
                }
            }
            
            val json = gson.toJson(updatedNotifications.map { it.toNotificationData() })
            sharedPreferences.edit()
                .putString(KEY_NOTIFICATIONS, json)
                .apply()
            
            android.util.Log.d("NotificationLocalStorage", "✅ Marked all notifications as read for user: $userId")
        } catch (e: Exception) {
            android.util.Log.e("NotificationLocalStorage", "❌ Error marking all notifications as read: ${e.message}", e)
        }
    }
    
    /**
     * Delete a notification
     */
    fun deleteNotification(notificationId: String) {
        deleteNotificationByMessageId(notificationId)
    }

    /**
     * Delete notification by FCM message ID (stored as notification.id)
     */
    fun deleteNotificationByMessageId(messageId: String): Boolean {
        try {
            if (messageId.isBlank()) {
                return false
            }

            val allNotifications = getAllNotifications().toMutableList()
            val removed = allNotifications.removeAll { it.id == messageId }

            if (!removed) {
                android.util.Log.d("NotificationLocalStorage", "ℹ️ No notification found for messageId: $messageId")
                return false
            }
            
            val json = gson.toJson(allNotifications.map { it.toNotificationData() })
            sharedPreferences.edit()
                .putString(KEY_NOTIFICATIONS, json)
                .apply()
            
            android.util.Log.d("NotificationLocalStorage", "✅ Deleted notification by messageId: $messageId")
            return true
        } catch (e: Exception) {
            android.util.Log.e("NotificationLocalStorage", "❌ Error deleting notification by messageId: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Clear all notifications
     */
    fun clearAllNotifications() {
        try {
            sharedPreferences.edit()
                .remove(KEY_NOTIFICATIONS)
                .apply()
            
            android.util.Log.d("NotificationLocalStorage", "✅ Cleared all notifications")
        } catch (e: Exception) {
            android.util.Log.e("NotificationLocalStorage", "❌ Error clearing notifications: ${e.message}", e)
        }
    }
    
    /**
     * Get notification count for a user
     */
    fun getUnreadCountForUser(userId: String): Int {
        return getUnreadNotificationsForUser(userId).size
    }
    
    /**
     * Clean up expired notifications from storage
     * This should be called periodically to remove expired notifications
     */
    fun cleanupExpiredNotifications() {
        try {
            val json = sharedPreferences.getString(KEY_NOTIFICATIONS, null)
            if (json != null) {
                val type = object : TypeToken<List<NotificationData>>() {}.type
                val notificationDataList = gson.fromJson<List<NotificationData>>(json, type)
                
                val now = Timestamp.now()
                
                // Filter out expired notifications
                val activeNotifications = notificationDataList.filter { data ->
                    if (data.expiresAtSeconds > 0) {
                        val expiresAt = Timestamp(data.expiresAtSeconds, 0)
                        val isExpired = expiresAt.seconds < now.seconds
                        if (isExpired) {
                            android.util.Log.d("NotificationLocalStorage", "🗑️ Removing expired notification: ${data.title}")
                        }
                        !isExpired
                    } else {
                        true // Keep notifications without expiration
                    }
                }
                
                // Save back only active notifications
                val updatedJson = gson.toJson(activeNotifications)
                sharedPreferences.edit()
                    .putString(KEY_NOTIFICATIONS, updatedJson)
                    .apply()
                
                val removedCount = notificationDataList.size - activeNotifications.size
                if (removedCount > 0) {
                    android.util.Log.d("NotificationLocalStorage", "✅ Cleaned up $removedCount expired notifications")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationLocalStorage", "❌ Error cleaning up expired notifications: ${e.message}", e)
        }
    }
    
    /**
     * Data class for JSON serialization (since Timestamp is not directly serializable)
     */
    private data class NotificationData(
        val id: String = "",
        val recipientId: String = "",
        val recipientRole: String = "",
        val title: String = "",
        val message: String = "",
        val type: String = "INFO",
        val projectId: String = "",
        val projectName: String = "",
        val relatedId: String = "",
        val isRead: Boolean = false,
        val createdAtSeconds: Long = 0,
        val actionRequired: Boolean = false,
        val navigationTarget: String = "",
        val expiresAtSeconds: Long = 0 // Expiration timestamp in seconds
    )
    
    /**
     * Extension function to convert Notification to NotificationData
     */
    private fun Notification.toNotificationData(): NotificationData {
        return NotificationData(
            id = this.id,
            recipientId = this.recipientId,
            recipientRole = this.recipientRole,
            title = this.title,
            message = this.message,
            type = this.type.name,
            projectId = this.projectId,
            projectName = this.projectName,
            relatedId = this.relatedId,
            isRead = this.isRead,
            createdAtSeconds = this.createdAt.seconds,
            actionRequired = this.actionRequired,
            navigationTarget = this.navigationTarget,
            expiresAtSeconds = this.expiresAt?.seconds ?: 0
        )
    }
    
    /**
     * Calculate expiration time based on notification content
     * Matches guide's expiration logic exactly:
     * - Status changes: expire today at 11:59 PM
     * - "Today" references: expire today at 11:59 PM
     * - "Tomorrow" references: expire TODAY at 11:59 PM (not tomorrow)
     * - "In X days" pattern: expire (X-1) days from now at 11:59 PM
     * - Default: expire in 3 days at 11:59 PM
     */
    private fun calculateExpirationTime(notification: Notification): Timestamp? {
        val title = notification.title.lowercase()
        val message = notification.message.lowercase()
        val combinedText = "$title $message"
        
        val calendar = Calendar.getInstance()
        val now = Date()
        
        // Get today at 11:59 PM
        calendar.time = now
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayEnd = calendar.time
        
        // Check for status change notifications - expire today at 11:59 PM
        if (combinedText.contains("status has changed", ignoreCase = true) || 
            combinedText.contains("is now", ignoreCase = true) || 
            combinedText.contains("status changed", ignoreCase = true)) {
            val expiresAt = Timestamp(todayEnd.time / 1000, 0)
            android.util.Log.d("NotificationExpiration", "📅 Notification '${notification.title}' expires today at 11:59 PM (status change)")
            return expiresAt
        }
        
        // Check for "today" references - expire today at 11:59 PM
        if (combinedText.contains("today", ignoreCase = true)) {
            val expiresAt = Timestamp(todayEnd.time / 1000, 0)
            android.util.Log.d("NotificationExpiration", "📅 Notification '${notification.title}' expires today at 11:59 PM (mentions today)")
            return expiresAt
        }
        
        // Check for "tomorrow" references - expire TODAY at 11:59 PM (not tomorrow)
        if (combinedText.contains("tomorrow", ignoreCase = true)) {
            val expiresAt = Timestamp(todayEnd.time / 1000, 0)
            android.util.Log.d("NotificationExpiration", "📅 Notification '${notification.title}' expires today at 11:59 PM (mentions tomorrow)")
            return expiresAt
        }
        
        // Check for "in X days" pattern (e.g., "in 3 days", "in 2 days")
        val daysPattern = Regex("in (\\d+) days?")
        val match = daysPattern.find(combinedText)
        if (match != null) {
            val days = match.groupValues[1].toIntOrNull() ?: return null
            // Expire (days - 1) days from now at 11:59 PM
            val daysToExpire = maxOf(0, days - 1)
            calendar.time = todayEnd
            calendar.add(Calendar.DAY_OF_MONTH, daysToExpire)
            val expiresAt = Timestamp(calendar.timeInMillis / 1000, 0)
            android.util.Log.d("NotificationExpiration", "📅 Notification '${notification.title}' expires in $daysToExpire days at 11:59 PM (mentions 'in $days days')")
            return expiresAt
        }
        
        // Default: No time reference found - expire in 3 days at 11:59 PM
        calendar.time = todayEnd
        calendar.add(Calendar.DAY_OF_MONTH, 3)
        val expiresAt = Timestamp(calendar.timeInMillis / 1000, 0)
        android.util.Log.d("NotificationExpiration", "📅 Notification '${notification.title}' expires in 3 days at 11:59 PM (default)")
        return expiresAt
    }
}

