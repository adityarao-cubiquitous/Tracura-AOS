package com.cubiquitous.tracura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.Notification
import com.cubiquitous.tracura.model.NotificationBadge
import com.cubiquitous.tracura.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class ProjectNotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    
    private val _projectNotifications = MutableStateFlow<List<Notification>>(emptyList())
    val projectNotifications: StateFlow<List<Notification>> = _projectNotifications.asStateFlow()
    
    private val _projectNotificationBadge = MutableStateFlow(NotificationBadge())
    val projectNotificationBadge: StateFlow<NotificationBadge> = _projectNotificationBadge.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var currentProjectId: String = ""
    private var currentUserId: String = ""
    private var currentUserRole: String = "USER"
    
    // Load project-specific notifications
    fun loadProjectNotifications(userId: String, projectId: String, userRole: String = "USER") {
        viewModelScope.launch {
            try {
                Log.d("ProjectNotificationViewModel", "🔄 Loading project notifications for user: $userId, project: $projectId, role: $userRole")
                _isLoading.value = true
                _error.value = null
                
                currentUserId = userId
                currentProjectId = projectId
                currentUserRole = userRole
                
                val notifications = notificationRepository.getProjectNotificationsForUser(userId, projectId, userRole)
                _projectNotifications.value = notifications
                
                // Update badge count
                updateProjectNotificationBadge(notifications)
                
                Log.d("ProjectNotificationViewModel", "✅ Loaded ${notifications.size} project notifications")
            } catch (e: Exception) {
                Log.e("ProjectNotificationViewModel", "❌ Error loading project notifications: ${e.message}")
                _error.value = "Failed to load project notifications: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Load project-specific notifications with real-time updates
    fun loadProjectNotificationsRealtime(userId: String, projectId: String, userRole: String = "USER") {
        viewModelScope.launch {
            try {
                Log.d("ProjectNotificationViewModel", "🔄 Setting up real-time project notifications for user: $userId, project: $projectId, role: $userRole")
                _isLoading.value = true
                _error.value = null
                
                currentUserId = userId
                currentProjectId = projectId
                currentUserRole = userRole
                
                notificationRepository.getProjectNotificationsForUserRealtime(userId, projectId, userRole).collect { notifications ->
                    _projectNotifications.value = notifications
                    updateProjectNotificationBadge(notifications)
                    Log.d("ProjectNotificationViewModel", "📡 Real-time update: ${notifications.size} project notifications")
                }
            } catch (e: Exception) {
                Log.e("ProjectNotificationViewModel", "❌ Error loading real-time project notifications: ${e.message}")
                _error.value = "Failed to load project notifications: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Mark project notification as read
    fun markProjectNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                Log.d("ProjectNotificationViewModel", "🔄 Marking project notification as read: $notificationId")
                
                notificationRepository.markNotificationAsRead(notificationId).onSuccess {
                    // Remove the read notification from the local list since it should disappear
                    val updatedNotifications = _projectNotifications.value.filter { notification ->
                        notification.id != notificationId
                    }
                    _projectNotifications.value = updatedNotifications
                    updateProjectNotificationBadge(updatedNotifications)
                    
                    Log.d("ProjectNotificationViewModel", "✅ Marked project notification as read and removed from list")
                }.onFailure { error ->
                    Log.e("ProjectNotificationViewModel", "❌ Failed to mark project notification as read: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e("ProjectNotificationViewModel", "❌ Error marking project notification as read: ${e.message}")
            }
        }
    }
    
    // Mark project notification as read and immediately remove from list (for USER role)
    fun markProjectNotificationAsReadAndRemove(notificationId: String) {
        viewModelScope.launch {
            try {
                Log.d("ProjectNotificationViewModel", "🔄 Marking project notification as read and removing for USER role: $notificationId")
                
                notificationRepository.markNotificationAsRead(notificationId).onSuccess {
                    // Immediately remove the notification from the local list for USER role
                    val updatedNotifications = _projectNotifications.value.filter { notification ->
                        notification.id != notificationId
                    }
                    _projectNotifications.value = updatedNotifications
                    updateProjectNotificationBadge(updatedNotifications)
                    
                    Log.d("ProjectNotificationViewModel", "✅ Marked project notification as read and immediately removed from list for USER role")
                }.onFailure { error ->
                    Log.e("ProjectNotificationViewModel", "❌ Failed to mark project notification as read and remove: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e("ProjectNotificationViewModel", "❌ Error marking project notification as read and remove: ${e.message}")
            }
        }
    }
    
    // Mark all project notifications as read
    fun markAllProjectNotificationsAsRead() {
        viewModelScope.launch {
            try {
                Log.d("ProjectNotificationViewModel", "🔄 Marking all project notifications as read")
                
                notificationRepository.markAllProjectNotificationsAsRead(currentUserId, currentProjectId).onSuccess {
                    // Clear the entire list since all notifications are now read and should disappear
                    _projectNotifications.value = emptyList()
                    updateProjectNotificationBadge(emptyList())
                    
                    Log.d("ProjectNotificationViewModel", "✅ Marked all project notifications as read and cleared list")
                }.onFailure { error ->
                    Log.e("ProjectNotificationViewModel", "❌ Failed to mark all project notifications as read: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e("ProjectNotificationViewModel", "❌ Error marking all project notifications as read: ${e.message}")
            }
        }
    }
    
    // Mark all project notifications as read and immediately remove from list (for USER role)
    fun markAllProjectNotificationsAsReadAndRemove() {
        viewModelScope.launch {
            try {
                Log.d("ProjectNotificationViewModel", "🔄 Marking all project notifications as read and removing for USER role")
                
                notificationRepository.markAllProjectNotificationsAsRead(currentUserId, currentProjectId).onSuccess {
                    // Immediately clear the entire list for USER role
                    _projectNotifications.value = emptyList()
                    updateProjectNotificationBadge(emptyList())
                    
                    Log.d("ProjectNotificationViewModel", "✅ Marked all project notifications as read and immediately removed from list for USER role")
                }.onFailure { error ->
                    Log.e("ProjectNotificationViewModel", "❌ Failed to mark all project notifications as read and remove: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e("ProjectNotificationViewModel", "❌ Error marking all project notifications as read and remove: ${e.message}")
            }
        }
    }
    
    // Refresh project notifications
    fun refreshProjectNotifications() {
        if (currentUserId.isNotEmpty() && currentProjectId.isNotEmpty()) {
            loadProjectNotifications(currentUserId, currentProjectId, currentUserRole)
        }
    }
    
    // Update project notification badge
    private fun updateProjectNotificationBadge(notifications: List<Notification>) {
        val unreadCount = notifications.count { !it.isRead }
        _projectNotificationBadge.value = NotificationBadge(
            count = unreadCount,
            hasUnread = unreadCount > 0
        )
        Log.d("ProjectNotificationViewModel", "📊 Project notification badge updated: $unreadCount unread")
    }
    
    // Clear error
    fun clearError() {
        _error.value = null
    }
    
    // Get current project ID
    fun getCurrentProjectId(): String = currentProjectId
    
    // Get current user ID
    fun getCurrentUserId(): String = currentUserId
} 