package com.cubiquitous.tracura.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.Notification
import com.cubiquitous.tracura.model.NotificationBadge
import com.cubiquitous.tracura.model.NotificationType
import com.cubiquitous.tracura.utils.NotificationLocalStorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val localStorageManager = NotificationLocalStorageManager(context)

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _notificationBadge = MutableStateFlow(NotificationBadge())
    val notificationBadge: StateFlow<NotificationBadge> = _notificationBadge.asStateFlow()
    
    // Project-specific filtered notifications (moved from UI to ViewModel for performance)
    private val _currentProjectId = MutableStateFlow<String?>(null)
    
    val projectNotifications: StateFlow<List<Notification>> = combine(
        _notifications,
        _currentProjectId
    ) { notifications, projectId ->
        if (projectId.isNullOrBlank()) {
            emptyList()
        } else {
            filterProjectNotifications(notifications, projectId)
        }
    }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val projectNotificationBadge: StateFlow<com.cubiquitous.tracura.model.NotificationBadge> = projectNotifications
        .map { notifications ->
            val unreadCount = notifications.count { !it.isRead }
            com.cubiquitous.tracura.model.NotificationBadge(
                count = unreadCount,
                hasUnread = unreadCount > 0
            )
        }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = com.cubiquitous.tracura.model.NotificationBadge()
        )
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var currentUserId: String? = null
    private var notificationListener: kotlinx.coroutines.Job? = null
    
    /**
     * Set the current project ID for filtering notifications
     */
    fun setProjectId(projectId: String?) {
        _currentProjectId.value = projectId
    }
    
    /**
     * Filter notifications for a specific project (moved from UI to ViewModel)
     */
    private fun filterProjectNotifications(notifications: List<Notification>, projectId: String): List<Notification> {
        val trimmedProjectId = projectId.trim()
        return notifications
            .filterNotNull()
            .filter { notif ->
                !notif.isRead &&
                notif.projectId?.trim()?.let { notifProjectId ->
                    notifProjectId.equals(trimmedProjectId, ignoreCase = true) || 
                    notifProjectId == trimmedProjectId
                } ?: false
            }
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    fun loadNotifications(userId: String, alternateUserIds: List<String> = emptyList()) {
        // Cancel any existing listener
        notificationListener?.cancel()
        
        currentUserId = userId
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Clean up expired notifications from storage
                localStorageManager.cleanupExpiredNotifications()
                
                Log.d("NotificationViewModel", "🔄 Loading notifications for user: $userId (LOCAL STORAGE ONLY)")
                if (alternateUserIds.isNotEmpty()) {
                    Log.d("NotificationViewModel", "🔄 Also checking alternate user IDs: $alternateUserIds")
                }
                
                // Load notifications from local storage only (no Firestore fetching)
                // For production heads, check both phone and UID to catch all notifications
                val localNotifications = if (alternateUserIds.isNotEmpty()) {
                    localStorageManager.getNotificationsForUser(userId, alternateUserIds)
                } else {
                    localStorageManager.getNotificationsForUser(userId)
                }
                Log.d("NotificationViewModel", "💾 Loaded ${localNotifications.size} total notifications from local storage")
                
                // Filter out read notifications - only show unread ones
                val unreadNotifications = localNotifications.filter { !it.isRead }
                Log.d("NotificationViewModel", "📋 Filtered to ${unreadNotifications.size} unread notifications (removed ${localNotifications.size - unreadNotifications.size} read notifications)")
                
                // Show only unread notifications, sorted by date
                val sortedNotifications = unreadNotifications.sortedByDescending { it.createdAt.seconds }
                _notifications.value = sortedNotifications
                
                // Log each notification for debugging
                sortedNotifications.forEach { notification ->
                    Log.d("NotificationViewModel", "📋 Notification: ${notification.title} - Recipient: ${notification.recipientId} - ProjectId: '${notification.projectId}' - ProjectName: ${notification.projectName} - Read: ${notification.isRead}")
                }
                
                // Update badge immediately
                updateNotificationBadge(userId)
                
                Log.d("NotificationViewModel", "✅ Loaded ${sortedNotifications.size} unread notifications from local storage only")
                
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error loading notifications: ${e.message}")
                _error.value = "Failed to load notifications: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Load all notifications (only unread) - for approver notification screen
    // Read notifications are filtered out so they don't display
    fun loadAllNotifications(userId: String, alternateUserIds: List<String> = emptyList()) {
        // Cancel any existing listener
        notificationListener?.cancel()
        
        currentUserId = userId
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Clean up expired notifications from storage
                localStorageManager.cleanupExpiredNotifications()
                
                Log.d("NotificationViewModel", "🔄 Loading unread notifications for user: $userId (LOCAL STORAGE ONLY)")
                if (alternateUserIds.isNotEmpty()) {
                    Log.d("NotificationViewModel", "🔄 Also checking alternate user IDs: $alternateUserIds")
                }
                
                // Load notifications from local storage only (no Firestore fetching)
                // For production heads, check both phone and UID to catch all notifications
                val localNotifications = if (alternateUserIds.isNotEmpty()) {
                    localStorageManager.getNotificationsForUser(userId, alternateUserIds)
                } else {
                    localStorageManager.getNotificationsForUser(userId)
                }
                Log.d("NotificationViewModel", "💾 Loaded ${localNotifications.size} total notifications from local storage")
                
                // Filter out read notifications - only show unread ones
                val unreadNotifications = localNotifications.filter { !it.isRead }
                Log.d("NotificationViewModel", "📋 Filtered to ${unreadNotifications.size} unread notifications (removed ${localNotifications.size - unreadNotifications.size} read notifications)")
                
                // Show only unread notifications, sorted by date
                val sortedNotifications = unreadNotifications.sortedByDescending { it.createdAt.seconds }
                _notifications.value = sortedNotifications
                
                // Update badge immediately
                updateNotificationBadge(userId)
                
                Log.d("NotificationViewModel", "✅ Loaded ${sortedNotifications.size} unread notifications from local storage only")
                
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error loading notifications: ${e.message}")
                _error.value = "Failed to load notifications: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads saved notifications from NotificationLocalStorageManager for a specific project
     * This is synchronous and instant since it reads from SharedPreferences
     * Matches guide's loadSavedNotifications(projectId) method
     * 
     * @param projectId Optional project ID to filter notifications. 
     *                  If provided, stores it as currentProjectId for future updates.
     *                  If null, loads all notifications (for APPROVER and BUSINESSHEAD roles)
     */
    fun loadSavedNotifications(projectId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Clean up expired notifications from storage
                localStorageManager.cleanupExpiredNotifications()
                
                // Load directly from NotificationLocalStorageManager (already sorted and cached)
                val notifications = if (projectId != null) {
                    // Project-specific notifications (excluding expired)
                    localStorageManager.getNotificationsForProject(projectId)
                        .filter { !it.isExpired }
                } else {
                    // All notifications (excluding expired) - for APPROVER and BUSINESSHEAD roles
                    localStorageManager.getAllNotifications()
                        .filter { !it.isExpired }
                }
                
                // Ensure sorted by date (newest first) - though NotificationLocalStorageManager already does this
                val sortedNotifications = notifications.sortedByDescending { it.createdAt.seconds }
                _notifications.value = sortedNotifications
                
                // Update badge
                currentUserId?.let { updateNotificationBadge(it) }
                
                Log.d("NotificationViewModel", "✅ Loaded ${sortedNotifications.size} notifications (projectId: ${projectId ?: "all"})")
                
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error loading saved notifications: ${e.message}")
                _error.value = "Failed to load notifications: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Returns notifications filtered by project ID (excluding expired)
     * Matches guide's getNotificationsForProject method
     * 
     * @param projectId Project ID to filter by
     * @return List of notifications for the project
     */
    fun getNotificationsForProject(projectId: String): List<Notification> {
        return localStorageManager.getNotificationsForProject(projectId)
            .filter { !it.isExpired }
    }
    
    /**
     * Returns all notifications (for APPROVER and BUSINESSHEAD roles) excluding expired
     * Matches guide's getAllNotifications method
     * 
     * @return List of all non-expired notifications
     */
    fun getAllNotifications(): List<Notification> {
        return localStorageManager.getAllNotifications()
            .filter { !it.isExpired }
    }
    
    /**
     * Returns unread notification count for a project (excluding expired)
     * Matches guide's getUnreadCountForProject method
     */
    fun getUnreadCountForProject(projectId: String): Int {
        return getNotificationsForProject(projectId).size
    }
    
    /**
     * Returns total unread notification count (excluding expired)
     * Matches guide's getTotalUnreadCount method
     */
    fun getTotalUnreadCount(): Int {
        return getAllNotifications().size
    }
    
    /**
     * Load project-specific notifications (for DashboardView)
     * Matches guide's behavior for project-specific filtering
     */
    fun loadProjectNotifications(userId: String, projectId: String) {
        loadSavedNotifications(projectId)
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                Log.d("NotificationViewModel", "🗑️ Deleting notification: $notificationId")
                localStorageManager.deleteNotification(notificationId)
                
                // Remove from current notifications list
                val updatedNotifications = _notifications.value.filter { it.id != notificationId }
                _notifications.value = updatedNotifications
                
                // Update badge
                currentUserId?.let { userId ->
                    updateNotificationBadge(userId)
                }
                
                Log.d("NotificationViewModel", "✅ Deleted notification: $notificationId")
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error deleting notification: ${e.message}")
            }
        }
    }
    
    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                Log.d("NotificationViewModel", "🔄 Marking notification as read: $notificationId (LOCAL STORAGE ONLY)")
                
                // Mark as read in local storage only (no Firestore)
                localStorageManager.markNotificationAsRead(notificationId)
                
                // Remove the read notification from the local list since it should disappear
                val updatedNotifications = _notifications.value.filter { notification ->
                    notification.id != notificationId
                }
                _notifications.value = updatedNotifications
                
                // Update badge
                currentUserId?.let { updateNotificationBadge(it) }
                
                Log.d("NotificationViewModel", "✅ Marked notification as read and removed from list")
                
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error marking notification as read: ${e.message}")
                _error.value = "Failed to mark notification as read: ${e.message}"
            }
        }
    }

    fun markAllNotificationsAsRead(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("NotificationViewModel", "🔄 Marking all notifications as read for user: $userId (LOCAL STORAGE ONLY)")
                
                // Mark all as read in local storage only (no Firestore)
                localStorageManager.markAllNotificationsAsRead(userId)
                
                // Clear the entire list since all notifications are now read and should disappear
                _notifications.value = emptyList()
                
                // Update badge
                updateNotificationBadge(userId)
                
                Log.d("NotificationViewModel", "✅ Marked all notifications as read and cleared list")
                
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error marking all notifications as read: ${e.message}")
                _error.value = "Failed to mark all notifications as read: ${e.message}"
            }
        }
    }
    
    private suspend fun updateNotificationBadge(userId: String) {
        try {
            Log.d("NotificationViewModel", "🔄 Updating notification badge for user: $userId (LOCAL STORAGE ONLY)")
            
            // Calculate badge from ALL unread notifications in local storage (not just current state)
            // This ensures badge reflects all notifications including newly received push notifications
            val allLocalNotifications = if (currentUserId != null) {
                localStorageManager.getNotificationsForUser(userId)
            } else {
                localStorageManager.getNotificationsForUser(userId)
            }
            val unreadCount = allLocalNotifications.count { !it.isRead }
            val hasUnread = unreadCount > 0
            
            Log.d("NotificationViewModel", "📊 Calculated from ALL local notifications: unread=$unreadCount, hasUnread=$hasUnread")
            Log.d("NotificationViewModel", "📊 Total notifications in local storage: ${allLocalNotifications.size}")
            Log.d("NotificationViewModel", "📊 Total notifications in state: ${_notifications.value.size}")
            
            val finalBadge = NotificationBadge(
                count = unreadCount,
                hasUnread = hasUnread
            )
            
            _notificationBadge.value = finalBadge
            
            Log.d("NotificationViewModel", "📊 Final notification badge: count=$unreadCount, hasUnread=$hasUnread")
            
        } catch (e: Exception) {
            Log.e("NotificationViewModel", "❌ Error updating notification badge: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Refresh notifications and badge from local storage
     * Call this when a push notification is received to update the UI
     */
    fun refreshFromLocalStorage(userId: String, alternateUserIds: List<String> = emptyList()) {
        viewModelScope.launch {
            try {
                Log.d("NotificationViewModel", "🔄 Refreshing from local storage for user: $userId")
                
                // Reload notifications from local storage
                val localNotifications = if (alternateUserIds.isNotEmpty()) {
                    localStorageManager.getNotificationsForUser(userId, alternateUserIds)
                } else {
                    localStorageManager.getNotificationsForUser(userId)
                }
                
                // Filter out read notifications - only show unread ones
                val unreadNotifications = localNotifications.filter { !it.isRead }
                val sortedNotifications = unreadNotifications.sortedByDescending { it.createdAt.seconds }
                
                // Update state
                _notifications.value = sortedNotifications
                
                // Update badge
                updateNotificationBadge(userId)
                
                Log.d("NotificationViewModel", "✅ Refreshed: ${sortedNotifications.size} unread notifications")
                
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error refreshing from local storage: ${e.message}")
            }
        }
    }
    
    fun debugNotificationState(userId: String) {
        Log.d("NotificationViewModel", "🔍 === DEBUG NOTIFICATION STATE (LOCAL STORAGE ONLY) ===")
        Log.d("NotificationViewModel", "🔍 Current user ID: $userId")
        Log.d("NotificationViewModel", "🔍 Current notifications count: ${_notifications.value.size}")
        Log.d("NotificationViewModel", "🔍 Current badge: count=${_notificationBadge.value.count}, hasUnread=${_notificationBadge.value.hasUnread}")
        
        _notifications.value.forEach { notification ->
            Log.d("NotificationViewModel", "🔍 Notification: ${notification.title} - Read: ${notification.isRead} - Recipient: ${notification.recipientId}")
        }
        
        // Check local storage state
        viewModelScope.launch {
            try {
                val localNotifications = localStorageManager.getNotificationsForUser(userId)
                
                Log.d("NotificationViewModel", "🔍 Local storage notifications count: ${localNotifications.size}")
                
                localNotifications.forEach { notification ->
                    Log.d("NotificationViewModel", "🔍 Local storage notification: ${notification.title} - Read: ${notification.isRead} - Recipient: ${notification.recipientId}")
                }
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "🔍 Error in debug: ${e.message}")
            }
        }
    }

    fun forceLoadNotifications(userId: String) {
        Log.d("NotificationViewModel", "🔄 Force loading notifications for user: $userId")
        
        // Clear everything and reload
        _notifications.value = emptyList()
        _notificationBadge.value = NotificationBadge()
        currentUserId = null
        
        // Load notifications
        loadNotifications(userId)
    }

    fun refreshNotifications(userId: String) {
        Log.d("NotificationViewModel", "🔄 Force refreshing notifications for user: $userId")
        
        // Clear current notifications to force reload
        _notifications.value = emptyList()
        currentUserId = null
        
        // Force reload notifications
        loadNotifications(userId)
    }
    
    fun onScreenVisible(userId: String) {
        Log.d("NotificationViewModel", "👁️ Screen became visible, refreshing notifications for user: $userId")
        
        // Refresh notifications when screen becomes visible
        viewModelScope.launch {
            try {
                // Small delay to ensure smooth transition
                delay(100)
                refreshNotifications(userId)
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error refreshing on screen visible: ${e.message}")
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up the notification listener when ViewModel is cleared
        notificationListener?.cancel()
        Log.d("NotificationViewModel", "🧹 Cleaned up notification listener")
    }
    
    fun getUnreadCount(): Int = _notificationBadge.value.count
    
    fun hasUnreadNotifications(): Boolean = _notificationBadge.value.hasUnread
    
    // Update badge for Production Head based on declined projects count
    fun updateBadgeForBusinessHead(declinedProjectsCount: Int) {
        Log.d("NotificationViewModel", "🔄 Updating badge for Production Head: declinedProjectsCount=$declinedProjectsCount")
        val badge = NotificationBadge(
            count = declinedProjectsCount,
            hasUnread = declinedProjectsCount > 0
        )
        _notificationBadge.value = badge
        Log.d("NotificationViewModel", "📊 Production Head badge updated: count=$declinedProjectsCount, hasUnread=${declinedProjectsCount > 0}")
    }
    
    fun getNotificationsByType(type: NotificationType): List<Notification> {
        return _notifications.value.filter { it.type == type }
    }
    
    fun getProjectNotifications(projectId: String): List<Notification> {
        return _notifications.value.filter { it.projectId == projectId }
    }
    
    fun getUnreadNotifications(): List<Notification> {
        return _notifications.value.filter { !it.isRead }
    }
    
    fun getActionRequiredNotifications(): List<Notification> {
        return _notifications.value.filter { it.actionRequired && !it.isRead }
    }

    fun loadAllNotificationsForDebug() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("NotificationViewModel", "🔄 Loading ALL notifications for debugging (LOCAL STORAGE ONLY)")
                
                val allNotifications = localStorageManager.getAllNotifications()
                Log.d("NotificationViewModel", "📋 Found ${allNotifications.size} total notifications in local storage")
                
                // Show notifications with empty recipientId
                val emptyRecipientNotifications = allNotifications.filter { it.recipientId.isEmpty() }
                if (emptyRecipientNotifications.isNotEmpty()) {
                    Log.w("NotificationViewModel", "⚠️ Found ${emptyRecipientNotifications.size} notifications with empty recipientId:")
                    emptyRecipientNotifications.forEach { notification ->
                        Log.w("NotificationViewModel", "⚠️ Empty recipient notification: ${notification.title} - Project: ${notification.projectName}")
                    }
                }
                
                _notifications.value = allNotifications
                
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error loading all notifications: ${e.message}")
                _error.value = "Failed to load all notifications: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fixEmptyRecipientNotifications(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("NotificationViewModel", "🔄 Fixing notifications with empty recipientId for user: $userId (LOCAL STORAGE ONLY)")
                
                val allNotifications = localStorageManager.getAllNotifications()
                val emptyRecipientNotifications = allNotifications.filter { it.recipientId.isEmpty() }
                
                if (emptyRecipientNotifications.isNotEmpty()) {
                    Log.d("NotificationViewModel", "📋 Found ${emptyRecipientNotifications.size} notifications with empty recipientId, fixing...")
                    
                    // Update each notification to assign to current user in local storage
                    emptyRecipientNotifications.forEach { notification ->
                        try {
                            // Create updated notification with recipientId
                            val updatedNotification = notification.copy(recipientId = userId)
                            localStorageManager.saveNotification(updatedNotification)
                            // Delete the old one
                            localStorageManager.deleteNotification(notification.id)
                            Log.d("NotificationViewModel", "✅ Fixed notification: ${notification.title}")
                        } catch (e: Exception) {
                            Log.e("NotificationViewModel", "❌ Failed to fix notification ${notification.id}: ${e.message}")
                        }
                    }
                    
                    // Reload notifications
                    loadNotifications(userId)
                } else {
                    Log.d("NotificationViewModel", "📋 No notifications with empty recipientId found")
                }
                
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error fixing empty recipient notifications: ${e.message}")
                _error.value = "Failed to fix notifications: ${e.message}"
            }
        }
    }
} 