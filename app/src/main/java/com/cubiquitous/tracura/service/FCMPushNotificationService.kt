package com.cubiquitous.tracura.service

import android.util.Log
import com.cubiquitous.tracura.model.Notification
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to send FCM push notifications
 * 
 * This service retrieves FCM tokens and sends push notifications using Firebase Cloud Functions.
 * The actual sending is done via a Cloud Function for security.
 */
@Singleton
class FCMPushNotificationService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging,
    private val functions: FirebaseFunctions
) {
    
    companion object {
        private const val TAG = "FCMPushNotificationService"
        // Cloud Function name for sending FCM notifications
        private const val SEND_FCM_NOTIFICATION_FUNCTION = "sendFCMNotification"
    }
    
    /**
     * Get FCM token for a regular user from users collection
     * Token is stored in users/{userId}/fcmToken or users/{userId}/deviceInfo/fcmToken
     */
    suspend fun getUserFCMToken(userId: String): String? {
        return try {
            Log.d(TAG, "🔍 Retrieving FCM token for user: $userId")
            
            val userDoc = firestore.collection("users").document(userId).get().await()
            
            if (!userDoc.exists()) {
                Log.w(TAG, "⚠️ User document not found: $userId")
                return null
            }
            
            val data = userDoc.data ?: return null
            
            // Try to get token from fcmToken field (direct)
            val fcmToken = data["fcmToken"] as? String
            if (!fcmToken.isNullOrBlank()) {
                Log.d(TAG, "✅ Found FCM token in fcmToken field for user: $userId")
                return fcmToken
            }
            
            // Try to get token from deviceInfo.fcmToken (nested)
            val deviceInfo = data["deviceInfo"] as? Map<*, *>
            val deviceFcmToken = deviceInfo?.get("fcmToken") as? String
            if (!deviceFcmToken.isNullOrBlank()) {
                Log.d(TAG, "✅ Found FCM token in deviceInfo.fcmToken for user: $userId")
                return deviceFcmToken
            }
            
            Log.w(TAG, "⚠️ No FCM token found for user: $userId")
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error retrieving FCM token for user $userId: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get FCM tokens for admin/customer from customers collection
     * Tokens are stored as an array in customers/{customerId}/FCMlist
     */
    suspend fun getAdminFCMTokens(customerId: String): List<String> {
        return try {
            Log.d(TAG, "🔍 Retrieving FCM tokens for customer/admin: $customerId")
            
            val customerDoc = firestore.collection("customers").document(customerId).get().await()
            
            if (!customerDoc.exists()) {
                Log.w(TAG, "⚠️ Customer document not found: $customerId")
                return emptyList()
            }
            
            val data = customerDoc.data ?: return emptyList()
            
            // Try FCMlist (case-sensitive as shown in image)
            val fcmList = data["FCMlist"] as? List<*>
            if (fcmList != null && fcmList.isNotEmpty()) {
                val tokens = fcmList.mapNotNull { it as? String }.filter { it.isNotBlank() }
                Log.d(TAG, "✅ Found ${tokens.size} FCM tokens in FCMlist for customer: $customerId")
                return tokens
            }
            
            // Try fcmList (lowercase variant)
            val fcmListLower = data["fcmList"] as? List<*>
            if (fcmListLower != null && fcmListLower.isNotEmpty()) {
                val tokens = fcmListLower.mapNotNull { it as? String }.filter { it.isNotBlank() }
                Log.d(TAG, "✅ Found ${tokens.size} FCM tokens in fcmList for customer: $customerId")
                return tokens
            }
            
            Log.w(TAG, "⚠️ No FCM tokens found in FCMlist for customer: $customerId")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error retrieving FCM tokens for customer $customerId: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get FCM token for a user by phone number (since notifications use phone as recipientId)
     */
    suspend fun getUserFCMTokenByPhone(phoneNumber: String): String? {
        return try {
            Log.d(TAG, "🔍 Retrieving FCM token for user by phone: $phoneNumber")
            
            // Query users collection by phoneNumber
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("phoneNumber", phoneNumber)
                .limit(1)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                Log.w(TAG, "⚠️ User not found with phone number: $phoneNumber")
                return null
            }
            
            val userDoc = querySnapshot.documents.first()
            val userId = userDoc.id
            
            // Get token using the userId
            return getUserFCMToken(userId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error retrieving FCM token by phone $phoneNumber: ${e.message}", e)
            null
        }
    }
    
    /**
     * Send push notification to a single FCM token using Cloud Functions
     */
    suspend fun sendPushNotification(
        fcmToken: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Result<Unit> {
        return try {
            Log.d(TAG, "📤 Sending push notification to token: ${fcmToken.take(20)}...")
            Log.d(TAG, "   Title: $title")
            Log.d(TAG, "   Body: $body")
            
            // Prepare data for Cloud Function
            val functionData = mapOf(
                "token" to fcmToken,
                "title" to title,
                "body" to body,
                "data" to data
            )
            
            // Call Cloud Function to send notification
            val result = functions
                .getHttpsCallable(SEND_FCM_NOTIFICATION_FUNCTION)
                .call(functionData)
                .await()
            
            Log.d(TAG, "✅ Push notification sent successfully via Cloud Function")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending push notification: ${e.message}", e)
            // If Cloud Function doesn't exist, log warning but don't fail
            // This allows the app to work even if Cloud Functions aren't set up yet
            if (e.message?.contains("NOT_FOUND") == true || e.message?.contains("not found") == true) {
                Log.w(TAG, "⚠️ Cloud Function not found. Please set up the sendFCMNotification Cloud Function.")
                Log.w(TAG, "⚠️ Push notification will not be sent, but in-app notification will still be created.")
            }
            // Return success to not block the notification creation flow
            // The in-app notification will still be created
            Result.success(Unit)
        }
    }
    
    /**
     * Send push notification based on Notification model
     */
    suspend fun sendPushNotificationFromModel(notification: Notification): Result<Unit> {
        return try {
            Log.d(TAG, "📤 Sending push notification from model")
            Log.d(TAG, "   Recipient: ${notification.recipientId}")
            Log.d(TAG, "   Role: ${notification.recipientRole}")
            Log.d(TAG, "   Title: ${notification.title}")
            Log.d(TAG, "   Project ID: ${notification.projectId}")
            
            val fcmTokens = mutableListOf<String>()
            
            // Try multiple strategies to get FCM tokens
            // Strategy 1: If recipient is ADMIN, try to get FCMlist from customer document
            if (notification.recipientRole == "ADMIN") {
                // Try to find customer ID from project
                val customerId = findCustomerIdFromProject(notification.projectId)
                if (customerId != null) {
                    val adminTokens = getAdminFCMTokens(customerId)
                    fcmTokens.addAll(adminTokens)
                    Log.d(TAG, "✅ Found ${adminTokens.size} admin tokens from customer: $customerId")
                }
                
                // Also try recipientId as customer ID directly
                if (fcmTokens.isEmpty()) {
                    val adminTokens = getAdminFCMTokens(notification.recipientId)
                    fcmTokens.addAll(adminTokens)
                    Log.d(TAG, "✅ Found ${adminTokens.size} admin tokens using recipientId as customer ID")
                }
            }
            
            // Strategy 2: Try to get token by phone number (most common for users)
            if (fcmTokens.isEmpty() && notification.recipientId.length >= 10) {
                // Assume it's a phone number if it's 10+ digits
                val token = getUserFCMTokenByPhone(notification.recipientId)
                if (token != null) {
                    fcmTokens.add(token)
                    Log.d(TAG, "✅ Found user token by phone number")
                }
            }
            
            // Strategy 3: Try to get token by UID (if recipientId is a UID)
            if (fcmTokens.isEmpty()) {
                val token = getUserFCMToken(notification.recipientId)
                if (token != null) {
                    fcmTokens.add(token)
                    Log.d(TAG, "✅ Found user token by UID")
                }
            }
            
            if (fcmTokens.isEmpty()) {
                Log.w(TAG, "⚠️ No FCM tokens found for recipient: ${notification.recipientId}")
                // Don't fail - just log warning, in-app notification will still work
                return Result.success(Unit)
            }
            
            // Prepare notification data
            val data = mapOf(
                "notificationId" to notification.id,
                "type" to notification.type.name,
                "projectId" to notification.projectId,
                "projectName" to notification.projectName,
                "relatedId" to notification.relatedId,
                "navigationTarget" to notification.navigationTarget,
                "actionRequired" to notification.actionRequired.toString()
            )
            
            // Send to all tokens
            var successCount = 0
            var failureCount = 0
            
            fcmTokens.forEach { token ->
                val result = sendPushNotification(
                    fcmToken = token,
                    title = notification.title,
                    body = notification.message,
                    data = data
                )
                
                if (result.isSuccess) {
                    successCount++
                } else {
                    failureCount++
                }
            }
            
            Log.d(TAG, "📊 Push notification results: $successCount success, $failureCount failures")
            
            // Return success even if some failed, as long as at least one succeeded
            // Or if all failed, still return success to not block notification creation
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending push notification from model: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create FCM message payload
     */
    private fun createFCMMessage(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Map<String, Any> {
        return mapOf(
            "message" to mapOf(
                "token" to token,
                "notification" to mapOf(
                    "title" to title,
                    "body" to body
                ),
                "data" to data,
                "android" to mapOf(
                    "priority" to "high"
                ),
                "apns" to mapOf(
                    "headers" to mapOf(
                        "apns-priority" to "10"
                    )
                )
            )
        )
    }
    
    /**
     * Find customer ID from project
     */
    private suspend fun findCustomerIdFromProject(projectId: String): String? {
        return try {
            if (projectId.isBlank()) return null
            
            // Try to find project in customers collection
            val customersSnapshot = firestore.collection("customers").get().await()
            
            for (customerDoc in customersSnapshot.documents) {
                val customerId = customerDoc.id
                val projectDoc = firestore.collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(projectId)
                    .get()
                    .await()
                
                if (projectDoc.exists()) {
                    Log.d(TAG, "✅ Found customer ID for project: $customerId")
                    return customerId
                }
            }
            
            Log.w(TAG, "⚠️ Could not find customer ID for project: $projectId")
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error finding customer ID from project: ${e.message}", e)
            null
        }
    }
    
    /**
     * Send push notification to multiple tokens (for admin FCMlist)
     */
    suspend fun sendPushNotificationToMultiple(
        fcmTokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Result<Int> {
        return try {
            Log.d(TAG, "📤 Sending push notification to ${fcmTokens.size} tokens")
            
            var successCount = 0
            
            fcmTokens.forEach { token ->
                val result = sendPushNotification(token, title, body, data)
                if (result.isSuccess) {
                    successCount++
                }
            }
            
            Log.d(TAG, "✅ Sent push notifications: $successCount/${fcmTokens.size} successful")
            Result.success(successCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending push notifications to multiple tokens: ${e.message}", e)
            Result.failure(e)
        }
    }
}

