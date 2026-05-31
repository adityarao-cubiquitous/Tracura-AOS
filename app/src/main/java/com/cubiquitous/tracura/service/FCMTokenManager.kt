package com.cubiquitous.tracura.service

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FCMTokenManager @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val messaging = FirebaseMessaging.getInstance()
    
    suspend fun getFCMToken(): String? {
        return try {
            Log.d("FCMTokenManager", "🔍 Attempting to get FCM token...")
            val token = messaging.token.await()
            if (token.isNotBlank()) {
                Log.d("FCMTokenManager", "✅ FCM Token retrieved successfully: ${token.take(20)}...")
                token
            } else {
                Log.w("FCMTokenManager", "⚠️ FCM Token is blank")
                null
            }
        } catch (e: Exception) {
            Log.e("FCMTokenManager", "❌ Error getting FCM token: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get FCM token with retry logic
     * Retries up to 3 times with delays if token is not available immediately
     */
    suspend fun getFCMTokenWithRetry(maxRetries: Int = 3, delayMs: Long = 1000): String? {
        var retryCount = 0
        while (retryCount < maxRetries) {
            val token = getFCMToken()
            if (token != null && token.isNotBlank()) {
                return token
            }
            
            retryCount++
            if (retryCount < maxRetries) {
                Log.d("FCMTokenManager", "🔄 Retrying FCM token retrieval (attempt $retryCount/$maxRetries)...")
                kotlinx.coroutines.delay(delayMs)
            }
        }
        
        Log.e("FCMTokenManager", "❌ Failed to get FCM token after $maxRetries attempts")
        return null
    }
    
    /**
     * Save FCM token to users collection with proper structure as shown in image
     * Document structure: fcmToken, createdAt, isActive, name, ownerID, phoneNumber, role
     */
    suspend fun saveFCMTokenToUser(phoneNumber: String) {
        try {
            Log.d("FCMTokenManager", "💾 Saving FCM token for phone: $phoneNumber")
            
            if (phoneNumber.isBlank()) {
                Log.w("FCMTokenManager", "⚠️ Phone number is blank, cannot save FCM token")
                return
            }
            
            // Normalize phone number (remove +91 prefix if present, remove spaces)
            val normalizedPhone = phoneNumber.replace("+91", "").replace(" ", "").trim()
            Log.d("FCMTokenManager", "🔍 Normalized phone number: $normalizedPhone")
            
            // Get FCM token with retry logic (token might not be ready immediately)
            val token = getFCMTokenWithRetry(maxRetries = 3, delayMs = 1000)
            
            if (token == null || token.isBlank()) {
                Log.e("FCMTokenManager", "❌ FCM token is null or blank, cannot save for phone: $normalizedPhone")
                return
            }
            
            Log.d("FCMTokenManager", "✅ FCM token retrieved: ${token.take(20)}...")
            
            // Try to find user document by phone number (try both formats)
            var querySnapshot = firestore.collection("users")
                .whereEqualTo("phoneNumber", normalizedPhone)
                .get()
                .await()
            
            // If not found, try with original phone number
            if (querySnapshot.isEmpty && normalizedPhone != phoneNumber) {
                Log.d("FCMTokenManager", "🔍 Trying with original phone number format: $phoneNumber")
                querySnapshot = firestore.collection("users")
                    .whereEqualTo("phoneNumber", phoneNumber)
                    .get()
                    .await()
            }
            
            // Also try with document ID if phone number is used as document ID
            if (querySnapshot.isEmpty) {
                Log.d("FCMTokenManager", "🔍 Trying to find by document ID: $normalizedPhone")
                try {
                    val docSnapshot = firestore.collection("users").document(normalizedPhone).get().await()
                    if (docSnapshot.exists()) {
                        val document = docSnapshot
                        val userDoc = firestore.collection("users").document(document.id)
                        val userData = document.data
                        
                        Log.d("FCMTokenManager", "✅ Found user by document ID: ${document.id}")
                        
                        // Update with FCM token only (don't modify isActive)
                        val updates = mutableMapOf<String, Any>(
                            "fcmToken" to token
                        )
                        
                        // Update createdAt if not exists
                        if (userData != null && !userData.containsKey("createdAt")) {
                            if (userData.containsKey("timestamp")) {
                                updates["createdAt"] = userData["timestamp"] as? Timestamp ?: Timestamp.now()
                            } else {
                                updates["createdAt"] = Timestamp.now()
                            }
                        }
                        
                        // Ensure ownerID field is set
                        val customerId = userData?.get("customerId") as? String
                        if (customerId != null && userData?.containsKey("ownerID") != true) {
                            updates["ownerID"] = customerId
                        }
                        
                        userDoc.update(updates).await()
                        
                        Log.d("FCMTokenManager", "✅ FCM token saved to users collection for phone: $normalizedPhone (document ID)")
                        return
                    }
                } catch (e: Exception) {
                    Log.w("FCMTokenManager", "⚠️ Error checking document ID: ${e.message}")
                }
            }
            
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                val userDoc = firestore.collection("users").document(document.id)
                val userData = document.data
                
                Log.d("FCMTokenManager", "✅ Found user document: ${document.id}")
                
                // Update with FCM token only (don't modify isActive)
                val updates = mutableMapOf<String, Any>(
                    "fcmToken" to token
                )
                
                // Update createdAt if not exists
                if (userData != null && !userData.containsKey("createdAt")) {
                    if (userData.containsKey("timestamp")) {
                        updates["createdAt"] = userData["timestamp"] as? Timestamp ?: Timestamp.now()
                    } else {
                        updates["createdAt"] = Timestamp.now()
                    }
                }
                
                // Ensure ownerID field is set
                val customerId = userData?.get("customerId") as? String
                if (customerId != null && userData?.containsKey("ownerID") != true) {
                    updates["ownerID"] = customerId
                }
                
                userDoc.update(updates).await()
                
                // Verify the update
                val verifyDoc = userDoc.get().await()
                val verifyData = verifyDoc.data
                val savedToken = verifyData?.get("fcmToken") as? String
                Log.d("FCMTokenManager", "✅ Verification - FCM token saved: ${savedToken != null && savedToken.isNotBlank()}")
                
                Log.d("FCMTokenManager", "✅ FCM token saved to users collection for phone: $normalizedPhone")
            } else {
                Log.w("FCMTokenManager", "⚠️ User not found with phone number: $normalizedPhone (tried: $phoneNumber)")
            }
        } catch (e: Exception) {
            Log.e("FCMTokenManager", "❌ Error saving FCM token to user: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    /**
     * Save FCM token to customers collection FCMlist array for production head
     * Document structure: FCMlist (array), businessName, createdAt, email
     */
    suspend fun saveFCMTokenToCustomer(customerId: String) {
        try {
            Log.d("FCMTokenManager", "💾 Saving FCM token to customer: $customerId")
            
            if (customerId.isBlank()) {
                Log.w("FCMTokenManager", "⚠️ Customer ID is blank, cannot save FCM token")
                return
            }
            
            // Get FCM token with retry logic (token might not be ready immediately)
            val token = getFCMTokenWithRetry(maxRetries = 3, delayMs = 1000)
            
            if (token == null || token.isBlank()) {
                Log.e("FCMTokenManager", "❌ FCM token is null or blank, cannot save for customer: $customerId")
                return
            }
            
            Log.d("FCMTokenManager", "✅ FCM token retrieved: ${token.take(20)}...")
            
            val customerDoc = firestore.collection("customers").document(customerId)
            
            // Get current document to check if FCMlist exists
            val docSnapshot = customerDoc.get().await()
            
            if (docSnapshot.exists()) {
                val data = docSnapshot.data ?: return
                val currentFCMList = (data["FCMlist"] as? List<*>)?.mapNotNull { it as? String }?.toMutableList() 
                    ?: mutableListOf()
                
                // Add token if not already in list
                if (!currentFCMList.contains(token)) {
                    currentFCMList.add(token)
                    customerDoc.update("FCMlist", currentFCMList).await()
                    
                    // Verify the update
                    val verifyDoc = customerDoc.get().await()
                    val verifyData = verifyDoc.data
                    val verifyList = (verifyData?.get("FCMlist") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    Log.d("FCMTokenManager", "✅ Verification - FCM token in list: ${verifyList.contains(token)}, total tokens: ${verifyList.size}")
                    
                    Log.d("FCMTokenManager", "✅ FCM token added to customers FCMlist for customer: $customerId")
                } else {
                    Log.d("FCMTokenManager", "ℹ️ FCM token already exists in FCMlist for customer: $customerId")
                }
            } else {
                // Create new document with FCMlist
                customerDoc.set(mapOf(
                    "FCMlist" to listOf(token),
                    "createdAt" to Timestamp.now()
                )).await()
                Log.d("FCMTokenManager", "✅ Created customer document with FCMlist for customer: $customerId")
            }
        } catch (e: Exception) {
            Log.e("FCMTokenManager", "❌ Error saving FCM token to customer: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    suspend fun removeFCMTokenFromUser(userId: String) {
        try {
            val userDoc = firestore.collection("users").document(userId)
            val deviceInfo = mapOf(
                "fcmToken" to null,
                "isOnline" to false,
                "lastLoginAt" to System.currentTimeMillis()
            )
            
            userDoc.update("deviceInfo", deviceInfo).await()
            userDoc.update("fcmToken", null).await()
            // Note: Not modifying isActive - FCM token removal should not affect user active status
            Log.d("FCMTokenManager", "FCM token removed for user: $userId")
        } catch (e: Exception) {
            Log.e("FCMTokenManager", "Error removing FCM token: ${e.message}")
        }
    }
    
    /**
     * Delete FCM token from users collection by phone number
     * Used when user logs out
     */
    suspend fun deleteFCMTokenFromUser(phoneNumber: String) {
        try {
            Log.d("FCMTokenManager", "🗑️ Deleting FCM token for phone: $phoneNumber")
            
            if (phoneNumber.isBlank()) {
                Log.w("FCMTokenManager", "⚠️ Phone number is blank, cannot delete FCM token")
                return
            }
            
            // Normalize phone number (remove +91 prefix if present, remove spaces)
            val normalizedPhone = phoneNumber.replace("+91", "").replace(" ", "").trim()
            Log.d("FCMTokenManager", "🔍 Normalized phone number: $normalizedPhone")
            
            // Try to find user document by phone number (try both formats)
            var querySnapshot = firestore.collection("users")
                .whereEqualTo("phoneNumber", normalizedPhone)
                .get()
                .await()
            
            // If not found, try with original phone number
            if (querySnapshot.isEmpty && normalizedPhone != phoneNumber) {
                Log.d("FCMTokenManager", "🔍 Trying with original phone number format: $phoneNumber")
                querySnapshot = firestore.collection("users")
                    .whereEqualTo("phoneNumber", phoneNumber)
                    .get()
                    .await()
            }
            
            // Also try with document ID if phone number is used as document ID
            if (querySnapshot.isEmpty) {
                Log.d("FCMTokenManager", "🔍 Trying to find by document ID: $normalizedPhone")
                try {
                    val docSnapshot = firestore.collection("users").document(normalizedPhone).get().await()
                    if (docSnapshot.exists()) {
                        val document = docSnapshot
                        val userDoc = firestore.collection("users").document(document.id)
                        val userData = document.data
                        
                        Log.d("FCMTokenManager", "✅ Found user by document ID: ${document.id}")
                        
                        // Delete fcmToken only (don't modify isActive)
                        val updates = mutableMapOf<String, Any>(
                            "fcmToken" to FieldValue.delete()
                        )
                        
                        // Also update deviceInfo if it exists
                        if (userData != null && userData.containsKey("deviceInfo")) {
                            val existingDeviceInfo = userData["deviceInfo"] as? Map<*, *>
                            val deviceInfo = mutableMapOf<String, Any>()
                            
                            // Copy existing deviceInfo values (excluding fcmToken which we're deleting)
                            existingDeviceInfo?.forEach { (key, value) ->
                                if (key is String && key != "fcmToken" && value != null) {
                                    deviceInfo[key] = value as Any
                                }
                            }
                            
                            // Set isOnline to false
                            deviceInfo["isOnline"] = false
                            updates["deviceInfo"] = deviceInfo
                        }
                        
                        // Perform the update
                        userDoc.update(updates).await()
                        
                        Log.d("FCMTokenManager", "✅ FCM token deleted from users collection for phone: $normalizedPhone")
                        return
                    }
                } catch (e: Exception) {
                    Log.w("FCMTokenManager", "⚠️ Error checking document ID: ${e.message}")
                }
            }
            
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                val userDoc = firestore.collection("users").document(document.id)
                val userData = document.data
                
                Log.d("FCMTokenManager", "✅ Found user document: ${document.id}")
                Log.d("FCMTokenManager", "📋 Current user data - fcmToken: ${userData?.get("fcmToken")}, isActive: ${userData?.get("isActive")}")
                
                // Delete fcmToken only (don't modify isActive)
                val updates = mutableMapOf<String, Any>(
                    "fcmToken" to FieldValue.delete()
                )
                
                // Also update deviceInfo if it exists
                if (userData != null && userData.containsKey("deviceInfo")) {
                    val existingDeviceInfo = userData["deviceInfo"] as? Map<*, *>
                    val deviceInfo = mutableMapOf<String, Any>()
                    
                    // Copy existing deviceInfo values (excluding fcmToken which we're deleting)
                    existingDeviceInfo?.forEach { (key, value) ->
                        if (key is String && key != "fcmToken" && value != null) {
                            deviceInfo[key] = value as Any
                        }
                    }
                    
                    // Set isOnline to false
                    deviceInfo["isOnline"] = false
                    updates["deviceInfo"] = deviceInfo
                }
                
                // Perform the update and wait for completion
                val updateResult = userDoc.update(updates).await()
                
                // Verify the update by reading the document back
                val verifyDoc = userDoc.get().await()
                val verifyData = verifyDoc.data
                Log.d("FCMTokenManager", "✅ Verification - fcmToken exists: ${verifyData?.containsKey("fcmToken")}")
                
                Log.d("FCMTokenManager", "✅ FCM token deleted from users collection for phone: $normalizedPhone")
            } else {
                Log.w("FCMTokenManager", "⚠️ User not found with phone number: $normalizedPhone (tried: $phoneNumber)")
            }
        } catch (e: Exception) {
            Log.e("FCMTokenManager", "❌ Error deleting FCM token from user: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    /**
     * Remove FCM token from customers collection FCMlist array
     * Used when production head logs out
     */
    suspend fun removeFCMTokenFromCustomer(customerId: String) {
        try {
            Log.d("FCMTokenManager", "🗑️ Removing FCM token from customer FCMlist: $customerId")
            
            // Get current FCM token to remove it
            val currentToken = getFCMToken()
            
            if (currentToken != null) {
                val customerDoc = firestore.collection("customers").document(customerId)
                val docSnapshot = customerDoc.get().await()
                
                if (docSnapshot.exists()) {
                    val data = docSnapshot.data ?: return
                    val currentFCMList = (data["FCMlist"] as? List<*>)?.mapNotNull { it as? String }?.toMutableList() 
                        ?: mutableListOf()
                    
                    // Remove token from list if it exists
                    if (currentFCMList.contains(currentToken)) {
                        currentFCMList.remove(currentToken)
                        customerDoc.update("FCMlist", currentFCMList).await()
                        Log.d("FCMTokenManager", "✅ FCM token removed from customers FCMlist for customer: $customerId")
                    } else {
                        Log.d("FCMTokenManager", "ℹ️ FCM token not found in FCMlist for customer: $customerId")
                    }
                } else {
                    Log.w("FCMTokenManager", "⚠️ Customer document not found: $customerId")
                }
            }
        } catch (e: Exception) {
            Log.e("FCMTokenManager", "❌ Error removing FCM token from customer: ${e.message}", e)
        }
    }
}



























































