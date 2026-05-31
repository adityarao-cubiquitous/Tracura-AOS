package com.cubiquitous.tracura.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.cubiquitous.tracura.R
import com.cubiquitous.tracura.MainActivity
import com.cubiquitous.tracura.model.Notification
import com.cubiquitous.tracura.model.NotificationType
import com.cubiquitous.tracura.utils.NotificationLocalStorageManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TracuraFirebaseMessagingService : FirebaseMessagingService() {
    
    // Track processed message IDs to prevent duplicate notifications
    private val processedMessageIds = mutableSetOf<String>()
    
    /**
     * Check if a message has already been processed
     * Uses both in-memory cache and SharedPreferences for persistence
     */
    private fun isMessageProcessed(messageId: String?): Boolean {
        if (messageId == null) return false
        
        // Check in-memory cache first (fast)
        if (processedMessageIds.contains(messageId)) {
            return true
        }
        
        // Check SharedPreferences (persistent across app restarts)
        val prefs = getSharedPreferences("fcm_processed_messages", Context.MODE_PRIVATE)
        val isProcessed = prefs.getBoolean("msg_$messageId", false)
        
        if (isProcessed) {
            // Add to in-memory cache
            processedMessageIds.add(messageId)
            return true
        }
        
        return false
    }
    
    /**
     * Mark a message as processed
     */
    private fun markMessageAsProcessed(messageId: String?) {
        if (messageId == null) return
        
        // Add to in-memory cache
        processedMessageIds.add(messageId)
        
        // Keep only last 100 message IDs in memory
        if (processedMessageIds.size > 100) {
            val oldestId = processedMessageIds.first()
            processedMessageIds.remove(oldestId)
        }
        
        // Save to SharedPreferences (persistent)
        val prefs = getSharedPreferences("fcm_processed_messages", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("msg_$messageId", true).apply()
        
        // Clean up old entries (keep only last 200)
        val allKeys = prefs.all.keys.filter { it.startsWith("msg_") }
        if (allKeys.size > 200) {
            val sortedKeys = allKeys.sorted()
            val keysToRemove = sortedKeys.take(allKeys.size - 200)
            val editor = prefs.edit()
            keysToRemove.forEach { editor.remove(it) }
            editor.apply()
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        android.util.Log.d("AVRFirebaseMessagingService", "📬 ========== FCM MESSAGE RECEIVED ==========")
        android.util.Log.d("AVRFirebaseMessagingService", "📬 From: ${remoteMessage.from}")
        android.util.Log.d("AVRFirebaseMessagingService", "📬 Message ID: ${remoteMessage.messageId}")
        android.util.Log.d("AVRFirebaseMessagingService", "📬 Data: ${remoteMessage.data}")
        android.util.Log.d("AVRFirebaseMessagingService", "📬 Notification: ${remoteMessage.notification}")
        android.util.Log.d("AVRFirebaseMessagingService", "📬 Message Type: ${remoteMessage.messageType}")
        android.util.Log.d("AVRFirebaseMessagingService", "📬 Collapse Key: ${remoteMessage.collapseKey}")
        
        // Check for duplicate messages using messageId
        val messageId = remoteMessage.messageId
        if (isMessageProcessed(messageId)) {
            android.util.Log.w("AVRFirebaseMessagingService", "⚠️ Duplicate message detected (ID: $messageId) - skipping")
            return
        }
        
        // Mark message as processed immediately to prevent race conditions
        markMessageAsProcessed(messageId)
        
        val data = remoteMessage.data
        val notificationPayload = remoteMessage.notification
        
        // IMPORTANT FCM BEHAVIOR:
        // 1. If server sends "notification" payload AND app is in BACKGROUND:
        //    → Android displays automatically, onMessageReceived is NOT called
        // 2. If server sends "notification" payload AND app is in FOREGROUND:
        //    → onMessageReceived IS called, remoteMessage.notification is NOT null
        //    → We must display manually
        // 3. If server sends "data-only" payload (no "notification" field):
        //    → onMessageReceived IS always called, remoteMessage.notification is null
        //    → We must display manually
        
        // Determine title and message from available sources
        // Priority: notification payload > data payload
        val title = notificationPayload?.title 
            ?: data["title"] 
            ?: data["notificationTitle"] 
            ?: "New Notification"
        
        val message = notificationPayload?.body 
            ?: data["message"] 
            ?: data["body"] 
            ?: data["notificationBody"] 
            ?: ""
        
        // Check if we have valid title and message to display
        if (title.isBlank() && message.isBlank()) {
            android.util.Log.w("AVRFirebaseMessagingService", "⚠️ Both title and message are empty - skipping notification")
            return
        }
        
        // Extract navigation data from data payload
        val projectId = data["projectId"] ?: ""
        val chatId = data["chatId"] ?: data["relatedId"] ?: ""
        val expenseId = data["expenseId"] ?: ""
        val navigationTarget = data["screen"] ?: data["navigationTarget"] ?: ""
        val customerId = data["customerId"] ?: ""
        
        if (notificationPayload != null) {
            // Server sent notification payload (Firebase Console style: {"notification": {"title": "...", "body": "..."}})
            // onMessageReceived is only called when app is in FOREGROUND for notification payloads
            // So we must display manually
            android.util.Log.d("AVRFirebaseMessagingService", "📬 Notification payload received (server sent notification field)")
            android.util.Log.d("AVRFirebaseMessagingService", "📱 App is in FOREGROUND - displaying notification manually")
            
            // Save to local storage for in-app notification list
            /*saveNotificationToLocalStorage(
                title, message, data, 
                projectId, chatId, expenseId, 
                navigationTarget, customerId
            )*/
            
            // Display notification manually (required when app is in foreground)
            showNotification(title, message, data, messageId)
            
        } else if (data.isNotEmpty()) {
            // Server sent data-only payload (custom server style: {"data": {"title": "...", "message": "..."}})
            // onMessageReceived is always called for data-only payloads
            // We must always display manually
            android.util.Log.d("AVRFirebaseMessagingService", "📬 Data-only payload received (custom server - no notification field)")
            android.util.Log.d("AVRFirebaseMessagingService", "📱 Displaying notification manually from data payload")
            
            // Display notification manually (required for data-only payloads)
            showNotification(title, message, data, messageId)
            
        } else {
            android.util.Log.w("AVRFirebaseMessagingService", "⚠️ No notification or data payload - nothing to display")
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("AVRFirebaseMessagingService", "🔄 ========== NEW FCM TOKEN RECEIVED ==========")
        android.util.Log.d("AVRFirebaseMessagingService", "🔄 New FCM token: ${token.take(20)}...")
        android.util.Log.d("AVRFirebaseMessagingService", "🔄 Token length: ${token.length}")
        
        // Automatically save the new token if user is logged in
        // This ensures push notifications continue to work after token refresh
        saveNewTokenToFirestore(token)
    }
    
    /**
     * Save new FCM token to Firestore when token is refreshed
     */
    private fun saveNewTokenToFirestore(token: String) {
        try {
            android.util.Log.d("AVRFirebaseMessagingService", "💾 Attempting to save new FCM token to Firestore...")
            
            // Get user info from SharedPreferences or Firebase Auth
            val prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            val userPhone = prefs.getString("current_user_phone", null)
            val userUid = prefs.getString("current_user_id", null)
            
            // Also try Firebase Auth
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val phoneFromAuth = firebaseUser?.phoneNumber?.replace("+91", "") ?: ""
            val uidFromAuth = firebaseUser?.uid ?: ""
            
            val phoneNumber = userPhone ?: phoneFromAuth
            val customerId = userUid ?: uidFromAuth
            
            if (phoneNumber.isNotBlank()) {
                android.util.Log.d("AVRFirebaseMessagingService", "💾 Saving FCM token for user phone: $phoneNumber")
                
                // Use coroutine to save token asynchronously
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val fcmTokenManager = FCMTokenManager()
                        fcmTokenManager.saveFCMTokenToUser(phoneNumber)
                        android.util.Log.d("AVRFirebaseMessagingService", "✅ New FCM token saved to users collection")
                    } catch (e: Exception) {
                        android.util.Log.e("AVRFirebaseMessagingService", "❌ Error saving new FCM token: ${e.message}", e)
                    }
                }
            } else if (customerId.isNotBlank()) {
                android.util.Log.d("AVRFirebaseMessagingService", "💾 Saving FCM token for customer: $customerId")
                
                // Use coroutine to save token asynchronously
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val fcmTokenManager = FCMTokenManager()
                        fcmTokenManager.saveFCMTokenToCustomer(customerId)
                        android.util.Log.d("AVRFirebaseMessagingService", "✅ New FCM token saved to customers collection")
                    } catch (e: Exception) {
                        android.util.Log.e("AVRFirebaseMessagingService", "❌ Error saving new FCM token: ${e.message}", e)
                    }
                }
            } else {
                android.util.Log.w("AVRFirebaseMessagingService", "⚠️ No user info found, token will be saved on next login")
            }
        } catch (e: Exception) {
            android.util.Log.e("AVRFirebaseMessagingService", "❌ Error in saveNewTokenToFirestore: ${e.message}", e)
        }
    }
    
    private fun showNotification(
        title: String,
        message: String,
        data: Map<String, String>,
        fcmMessageId: String?
    ) {
        try {
            android.util.Log.d("AVRFirebaseMessagingService", "📱 Creating notification: title='$title', message='$message'")
            
            // Determine notification type and channel
            val notificationType = data["type"] ?: data["notificationType"] ?: "CHAT_MESSAGE"
            val channelId = getChannelIdForType(notificationType)
            createNotificationChannel(this, channelId, getChannelNameForType(notificationType))
            
            // Extract navigation data
            val projectId = data["projectId"] ?: ""
            val chatId = data["chatId"] ?: data["relatedId"] ?: ""
            val expenseId = data["expenseId"] ?: ""
            val navigationTarget = data["screen"] ?: data["navigationTarget"] ?: ""
            val customerId = data["customerId"] ?: ""
            val messageIdForLocalStorage =
                fcmMessageId ?: data["google.message_id"] ?: data["messageId"] ?: data["notificationId"]
            
            // Save notification to local storage only (NOT to Firestore documents)
            // This allows offline access to notification history without storing in server documents
            saveNotificationToLocalStorage(
                title = title,
                message = message,
                data = data,
                projectId = projectId,
                chatId = chatId,
                expenseId = expenseId,
                navigationTarget = navigationTarget,
                customerId = customerId,
                fcmMessageId = messageIdForLocalStorage
            )
            
            // Create intent for notification tap
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (projectId.isNotEmpty()) putExtra("projectId", projectId)
                if (chatId.isNotEmpty()) putExtra("chatId", chatId)
                if (expenseId.isNotEmpty()) putExtra("expenseId", expenseId)
                if (customerId.isNotEmpty()) putExtra("customerId", customerId)
                if (navigationTarget.isNotEmpty()) putExtra("navigationTarget", navigationTarget)
                putExtra("notificationType", notificationType)
                putExtra("title", title)
                putExtra("message", message)
                putExtra("fromNotification", true)
                if (!messageIdForLocalStorage.isNullOrEmpty()) {
                    putExtra("google.message_id", messageIdForLocalStorage)
                }
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Build notification
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 1000, 500, 1000))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
            
            // Add action required indicator for important notifications
            if (data["actionRequired"] == "true" || 
                notificationType.contains("EXPENSE", ignoreCase = true) ||
                notificationType.contains("PENDING", ignoreCase = true)) {
                notificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
            }
            
            val notification = notificationBuilder.build()
            
            // Generate unique notification ID based on message content to prevent duplicates
            // Use a combination of projectId, expenseId, chatId, title, and message hash
            // Also include notificationId from data if available for better deduplication
            val notificationIdFromData = data["notificationId"] ?: ""
            val notificationIdSource = if (notificationIdFromData.isNotEmpty()) {
                notificationIdFromData
            } else {
                "${projectId}_${expenseId}_${chatId}_${title}_${message}"
            }
            val notificationId = notificationIdSource.hashCode()
            
            // Show notification
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Cancel any existing notification with the same ID to prevent duplicates
            notificationManager.cancel(notificationId)
            
            // Show the new notification
            notificationManager.notify(notificationId, notification)
            
            android.util.Log.d("AVRFirebaseMessagingService", "✅ Notification shown successfully (ID: $notificationId)")
            android.util.Log.d("AVRFirebaseMessagingService", "📬 ======================================")
            
        } catch (e: Exception) {
            android.util.Log.e("AVRFirebaseMessagingService", "❌ Error showing notification: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    /**
     * Save notification to local storage for offline access
     * NOTE: This does NOT save to Firestore documents - only local device storage
     * Push notifications from server are displayed immediately without creating Firestore documents
     */
    private fun saveNotificationToLocalStorage(
        title: String,
        message: String,
        data: Map<String, String>,
        projectId: String,
        chatId: String,
        expenseId: String,
        navigationTarget: String,
        customerId: String,
        fcmMessageId: String?
    ) {
        try {
            // Get recipient ID from data, Firebase Auth, or SharedPreferences
            // Since FCM sends notifications to specific user tokens, if we receive it,
            // it's likely for the current logged-in user
            val recipientId = data["recipientId"] ?: data["userId"] ?: 
                getCurrentUserIdFromPreferences() ?:
                FirebaseAuth.getInstance().currentUser?.phoneNumber?.replace("+91", "") ?: 
                FirebaseAuth.getInstance().currentUser?.uid ?: ""
            
            android.util.Log.d("AVRFirebaseMessagingService", "💾 Saving notification - recipientId: '$recipientId', title: '$title'")
            
            // Map notification type from server to app enum
            val typeString = data["type"] ?: "INFO"
            val notificationType = mapNotificationType(typeString)
            
            // Use FCM built-in messageId as the unique local ID when available
            // Fallback to payload IDs, then generated ID
            val messageIdFromPayload = data["google.message_id"] ?: data["messageId"]
            val notificationIdFromServer = data["notificationId"]
            val notificationId = when {
                !fcmMessageId.isNullOrEmpty() -> fcmMessageId
                !messageIdFromPayload.isNullOrEmpty() -> messageIdFromPayload
                !notificationIdFromServer.isNullOrEmpty() -> notificationIdFromServer
                else -> {
                // Generate unique ID using timestamp + message hash to prevent collisions
                val messageHash = "${title}_${message}_${projectId}_${chatId}".hashCode().toString().replace("-", "n")
                "fcm_${System.currentTimeMillis()}_${System.nanoTime() % 1000000}_$messageHash"
            }
            }
            
            // Get project name if available
            val projectName = data["projectName"] ?: ""
            
            // Determine related ID based on notification type
            val relatedId = when {
                expenseId.isNotEmpty() -> expenseId
                chatId.isNotEmpty() -> chatId
                else -> ""
            }
            
            // Check if action is required
            val actionRequired = data["actionRequired"] == "true" || 
                               typeString.contains("EXPENSE", ignoreCase = true) ||
                               typeString.contains("PENDING", ignoreCase = true) ||
                               typeString.contains("REVIEW", ignoreCase = true)
            
            // Get timestamp from server data if available, otherwise use current time
            // Server may send timestamp as "timestamp", "timestampSeconds", "createdAt", or "created_at"
            val createdAtTimestamp = try {
                when {
                    data.containsKey("timestampSeconds") -> {
                        val seconds = data["timestampSeconds"]?.toLongOrNull() ?: 0L
                        if (seconds > 0) Timestamp(seconds, 0) else Timestamp.now()
                    }
                    data.containsKey("timestamp") -> {
                        val timestampStr = data["timestamp"]
                        val timestampLong = timestampStr?.toLongOrNull() ?: 0L
                        if (timestampLong > 0) {
                            // If timestamp is in milliseconds, convert to seconds
                            if (timestampLong > 1_000_000_000_000) {
                                Timestamp(timestampLong / 1000, 0)
                            } else {
                                Timestamp(timestampLong, 0)
                            }
                        } else {
                            Timestamp.now()
                        }
                    }
                    data.containsKey("createdAt") || data.containsKey("created_at") -> {
                        val createdAtStr = data["createdAt"] ?: data["created_at"] ?: ""
                        val createdAtLong = createdAtStr.toLongOrNull() ?: 0L
                        if (createdAtLong > 0) {
                            if (createdAtLong > 1_000_000_000_000) {
                                Timestamp(createdAtLong / 1000, 0)
                            } else {
                                Timestamp(createdAtLong, 0)
                            }
                        } else {
                            Timestamp.now()
                        }
                    }
                    else -> {
                        // Use current time with microsecond precision to ensure uniqueness
                        // Add nanoseconds from System.nanoTime() to ensure each notification gets unique timestamp
                        val baseTime = System.currentTimeMillis() / 1000
                        val nanoOffset = (System.nanoTime() % 1_000_000) / 1_000_000 // Use last 6 digits of nanoseconds as nanoseconds
                        Timestamp(baseTime, nanoOffset.toInt())
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AVRFirebaseMessagingService", "❌ Error parsing timestamp: ${e.message}, using current time")
                Timestamp.now()
            }
            
            android.util.Log.d("AVRFirebaseMessagingService", "💾 Notification timestamp: ${createdAtTimestamp.seconds}.${createdAtTimestamp.nanoseconds} (from server: ${data.containsKey("timestamp") || data.containsKey("timestampSeconds") || data.containsKey("createdAt")})")
            
            // Create notification object
            val notification = Notification(
                id = notificationId,
                recipientId = recipientId,
                recipientRole = data["recipientRole"] ?: "",
                title = title,
                message = message,
                type = notificationType,
                projectId = projectId,
                projectName = projectName,
                relatedId = relatedId,
                isRead = false,
                createdAt = createdAtTimestamp,
                actionRequired = actionRequired,
                navigationTarget = navigationTarget
            )
            
            // Save to local storage only (NOT to Firestore documents)
            // This allows users to see notification history offline without creating server documents
            val storageManager = NotificationLocalStorageManager(this)
            storageManager.saveNotification(notification)
            
            android.util.Log.d("AVRFirebaseMessagingService", "💾 Saved notification to local storage (NOT Firestore): $title")
            android.util.Log.d("AVRFirebaseMessagingService", "💾 Notification details - ID: ${notification.id}, recipientId: '${notification.recipientId}', projectId: '${notification.projectId}', fcmMessageId: $fcmMessageId")
            
            // Broadcast notification saved event to refresh UI and badge
            // This ensures the notification appears in the app and badge counter updates
            val refreshIntent = Intent("com.cubiquitous.tracura.NOTIFICATION_SAVED").apply {
                putExtra("recipientId", recipientId)
                putExtra("notificationId", notification.id)
            }
            sendBroadcast(refreshIntent)
            android.util.Log.d("AVRFirebaseMessagingService", "📢 Broadcasted notification saved event for recipient: $recipientId")
            
        } catch (e: Exception) {
            android.util.Log.e("AVRFirebaseMessagingService", "❌ Error saving notification to local storage: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    /**
     * Get current user ID from SharedPreferences (stored during login)
     */
    private fun getCurrentUserIdFromPreferences(): String? {
        return try {
            val prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            val userId = prefs.getString("current_user_phone", null) ?: prefs.getString("current_user_id", null)
            android.util.Log.d("AVRFirebaseMessagingService", "💾 Retrieved user ID from preferences: $userId")
            userId
        } catch (e: Exception) {
            android.util.Log.e("AVRFirebaseMessagingService", "❌ Error getting user ID from preferences: ${e.message}", e)
            null
        }
    }
    
    /**
     * Map server notification type string to app NotificationType enum
     */
    private fun mapNotificationType(typeString: String): NotificationType {
        return when {
            typeString.contains("project_created", ignoreCase = true) -> NotificationType.PROJECT_CHANGED
            typeString.contains("project_assigned", ignoreCase = true) -> NotificationType.PROJECT_ASSIGNMENT
            typeString.contains("project_declined", ignoreCase = true) -> NotificationType.PROJECT_CHANGED
            typeString.contains("status_", ignoreCase = true) -> NotificationType.PROJECT_CHANGED
            typeString.contains("phase_created", ignoreCase = true) -> NotificationType.PROJECT_CHANGED
            typeString.contains("phase_schedule_update", ignoreCase = true) -> NotificationType.PROJECT_CHANGED
            typeString.contains("department_budget_update", ignoreCase = true) -> NotificationType.PROJECT_CHANGED
            typeString.contains("project_date_update", ignoreCase = true) -> NotificationType.PROJECT_CHANGED
            typeString.contains("user_added", ignoreCase = true) -> NotificationType.PROJECT_ASSIGNMENT
            typeString.contains("user_removed", ignoreCase = true) -> NotificationType.PROJECT_ASSIGNMENT
            typeString.contains("manager_added", ignoreCase = true) -> NotificationType.ROLE_ASSIGNMENT
            typeString.contains("manager_removed", ignoreCase = true) -> NotificationType.ROLE_ASSIGNMENT
            typeString.contains("temp_approver_added", ignoreCase = true) -> NotificationType.TEMPORARY_APPROVER_ASSIGNMENT
            typeString.contains("temp_approver_removed", ignoreCase = true) -> NotificationType.ROLE_ASSIGNMENT
            typeString.contains("expense_admin_review", ignoreCase = true) -> NotificationType.EXPENSE_SUBMITTED
            typeString.contains("expense_manager_review", ignoreCase = true) -> NotificationType.EXPENSE_SUBMITTED
            typeString.contains("expense_status_update", ignoreCase = true) -> NotificationType.EXPENSE_APPROVED
            typeString.contains("expense_admin_update", ignoreCase = true) -> NotificationType.EXPENSE_SUBMITTED
            typeString.contains("expense_manager_update", ignoreCase = true) -> NotificationType.EXPENSE_SUBMITTED
            typeString.contains("expense_chat_staff", ignoreCase = true) -> NotificationType.CHAT_MESSAGE
            typeString.contains("expense_chat_customer", ignoreCase = true) -> NotificationType.CHAT_MESSAGE
            typeString.contains("new_chat_message", ignoreCase = true) -> NotificationType.CHAT_MESSAGE
            typeString.contains("customer_chat_message", ignoreCase = true) -> NotificationType.CHAT_MESSAGE
            else -> NotificationType.INFO
        }
    }
    
    private fun getChannelIdForType(type: String): String {
        return when {
            type.contains("CHAT", ignoreCase = true) -> CHANNEL_ID_MESSAGES
            type.contains("EXPENSE", ignoreCase = true) -> CHANNEL_ID_EXPENSES
            type.contains("PROJECT", ignoreCase = true) -> CHANNEL_ID_PROJECTS
            else -> CHANNEL_ID_GENERAL
        }
    }
    
    private fun getChannelNameForType(type: String): String {
        return when {
            type.contains("CHAT", ignoreCase = true) -> "Chat Messages"
            type.contains("EXPENSE", ignoreCase = true) -> "Expense Notifications"
            type.contains("PROJECT", ignoreCase = true) -> "Project Updates"
            else -> "General Notifications"
        }
    }
    
    companion object {
        // Notification Channel IDs
        private const val CHANNEL_ID_MESSAGES = "message_notifications"
        private const val CHANNEL_ID_EXPENSES = "expense_notifications"
        private const val CHANNEL_ID_PROJECTS = "project_notifications"
        private const val CHANNEL_ID_GENERAL = "general_notifications"
        
        // Legacy channel ID for backward compatibility
        const val CHANNEL_ID = CHANNEL_ID_MESSAGES
        
        fun createNotificationChannel(context: Context, channelId: String = CHANNEL_ID, channelName: String = "Message Notifications") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = when (channelId) {
                        CHANNEL_ID_MESSAGES -> "Notifications for new chat messages"
                        CHANNEL_ID_EXPENSES -> "Notifications for expense approvals and updates"
                        CHANNEL_ID_PROJECTS -> "Notifications for project updates and changes"
                        else -> "General app notifications"
                    }
                    enableVibration(true)
                    enableLights(true)
                    vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                }
                
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                android.util.Log.d("AVRFirebaseMessagingService", "✅ Created notification channel: $channelId")
            }
        }
        
        fun createAllNotificationChannels(context: Context) {
            createNotificationChannel(context, CHANNEL_ID_MESSAGES, "Chat Messages")
            createNotificationChannel(context, CHANNEL_ID_EXPENSES, "Expense Notifications")
            createNotificationChannel(context, CHANNEL_ID_PROJECTS, "Project Updates")
            createNotificationChannel(context, CHANNEL_ID_GENERAL, "General Notifications")
        }
    }
}
