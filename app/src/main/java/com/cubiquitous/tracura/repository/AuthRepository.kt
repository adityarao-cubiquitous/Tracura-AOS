package com.cubiquitous.tracura.repository

import android.util.Log
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.model.DeviceInfo
import com.cubiquitous.tracura.model.NotificationPreferences
import com.cubiquitous.tracura.model.Customer
import com.cubiquitous.tracura.model.CustomerUserRelation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<User> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                val phoneNumber = firebaseUser.phoneNumber?.replace("+91", "") ?: ""
                Log.d("AuthRepository", "🔍 Looking up user with phone: $phoneNumber")
                
                // First try to get user by phone number from Firebase
                val user = getUserByPhoneNumber(phoneNumber)
                if (user != null) {
                    Log.d("AuthRepository", "✅ Found existing user: ${user.name} with role: ${user.role}")
                    // Update the user with Firebase UID for future reference
                    val updatedUser = user.copy(uid = firebaseUser.uid)
                    updateUserUid(phoneNumber, firebaseUser.uid)
                    Result.success(updatedUser)
                } else {
                    Log.d("AuthRepository", "❌ User not found in database for phone: $phoneNumber")
                    Log.d("AuthRepository", "🚫 Access denied - user not authorized")
                    // Sign out the Firebase user since they're not authorized
                    auth.signOut()
                    Result.failure(Exception("User not authorized. Please contact administrator for access."))
                }
            } else {
                Result.failure(Exception("Authentication failed"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Sign in error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getUserByPhoneNumber(phoneNumber: String): User? {
        return try {
            Log.d("AuthRepository", "🔍 Querying Firestore for phone: '$phoneNumber'")
            Log.d("AuthRepository", "🔍 Phone number length: ${phoneNumber.length}")
            
            // Query users collection by phone number
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .await()
            
            Log.d("AuthRepository", "📊 Query completed. Found ${querySnapshot.documents.size} documents")
            
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                Log.d("AuthRepository", "📄 Document ID: ${document.id}")
                Log.d("AuthRepository", "📄 Raw document data: ${document.data}")
                
                val userData = document.data ?: return null
                
                // Log each field individually for debugging
                Log.d("AuthRepository", "🔍 Parsing user data:")
                Log.d("AuthRepository", "   - uid: ${userData["uid"]}")
                Log.d("AuthRepository", "   - name: ${userData["name"]}")
                Log.d("AuthRepository", "   - email: ${userData["email"]}")
                Log.d("AuthRepository", "   - phoneNumber: ${userData["phoneNumber"]}")
                Log.d("AuthRepository", "   - role: ${userData["role"]} (type: ${userData["role"]?.javaClass?.simpleName})")
                Log.d("AuthRepository", "   - isActive: ${userData["isActive"]}")
                Log.d("AuthRepository", "   - assignedProjects: ${userData["assignedProjects"]}")
                
                // Manually parse the user data to handle role conversion
                val roleString = userData["role"] as? String
                Log.d("AuthRepository", "🔍 Raw role from database: '$roleString'")
                val normalizedRole = roleString
                    ?.trim()
                    ?.replace("-", "_")
                    ?.replace(" ", "_")
                    ?.uppercase()
                Log.d("AuthRepository", "🔍 Processed role: '$normalizedRole'")
                val userRole = when (normalizedRole) {
                    "APPROVER" -> {
                        Log.d("AuthRepository", "✅ Detected APPROVER role")
                        UserRole.APPROVER
                    }
                    "ADMIN" -> {
                        Log.d("AuthRepository", "✅ Detected ADMIN role")
                        UserRole.ADMIN
                    }
                    "USER" -> {
                        Log.d("AuthRepository", "✅ Detected USER role")
                        UserRole.USER
                    }
                    "BUSINESS_HEAD", "BUSINESSHEAD" -> {
                        Log.d("AuthRepository", "✅ Detected BUSINESS_HEAD role")
                        UserRole.BUSINESS_HEAD
                    }
                    "MANAGER" -> {
                        Log.d("AuthRepository", "✅ Detected MANAGER role")
                        UserRole.MANAGER
                    }
                    else -> {
                        Log.w("AuthRepository", "⚠️ Unknown role: '$roleString', defaulting to USER")
                        UserRole.USER
                    }
                }

                // Parse device info
                val deviceInfoData = userData["deviceInfo"] as? Map<*, *>
                val deviceInfo = DeviceInfo(
                    fcmToken = deviceInfoData?.get("fcmToken") as? String ?: "",
                    deviceId = deviceInfoData?.get("deviceId") as? String ?: "",
                    deviceModel = deviceInfoData?.get("deviceModel") as? String ?: "",
                    osVersion = deviceInfoData?.get("osVersion") as? String ?: "",
                    appVersion = deviceInfoData?.get("appVersion") as? String ?: "",
                    lastLoginAt = deviceInfoData?.get("lastLoginAt") as? Long ?: System.currentTimeMillis(),
                    isOnline = deviceInfoData?.get("isOnline") as? Boolean ?: true
                )
                
                // Parse notification preferences
                val notificationPrefsData = userData["notificationPreferences"] as? Map<*, *>
                val notificationPreferences = NotificationPreferences(
                    pushNotifications = notificationPrefsData?.get("pushNotifications") as? Boolean ?: true,
                    expenseSubmitted = notificationPrefsData?.get("expenseSubmitted") as? Boolean ?: true,
                    expenseApproved = notificationPrefsData?.get("expenseApproved") as? Boolean ?: true,
                    expenseRejected = notificationPrefsData?.get("expenseRejected") as? Boolean ?: true,
                    projectAssignment = notificationPrefsData?.get("projectAssignment") as? Boolean ?: true,
                    pendingApprovals = notificationPrefsData?.get("pendingApprovals") as? Boolean ?: true
                )
                
                // Create User object from Customer data (BusinessHead users don't have users collection document)
                // For BusinessHead, create User object dynamically without deviceInfo/notificationPreferences
                val user = User(
                    uid = userData["uid"] as? String ?: "",
                    name = userData["name"] as? String ?: "",
                    email = userData["email"] as? String ?: "",
                    phone = userData["phoneNumber"] as? String ?: phoneNumber,
                    role = userRole,
                    createdAt = (userData["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                    isActive = userData["isActive"] as? Boolean ?: true,
                    assignedProjects = (userData["assignedProjects"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    deviceInfo = DeviceInfo(), // Empty defaults - not stored/verified for BusinessHead
                    notificationPreferences = NotificationPreferences(), // Empty defaults - not stored/verified for BusinessHead
                    customerId = userData["customerId"] as? String
                )
                
                Log.d("AuthRepository", "✅ Successfully parsed user:")
                Log.d("AuthRepository", "   - Final user: $user")
                return user
            } else {
                Log.d("AuthRepository", "❌ No documents found for phone: $phoneNumber")
                
                // Also try searching without country code for debugging
                val phoneWithoutPrefix = phoneNumber.replace("+91", "")
                if (phoneWithoutPrefix != phoneNumber) {
                    Log.d("AuthRepository", "🔄 Trying again without +91 prefix: $phoneWithoutPrefix")
                    return getUserByPhoneNumber(phoneWithoutPrefix)
                }
                
                // Also try searching with +91 prefix for debugging
                val phoneWithPrefix = "+91$phoneNumber"
                Log.d("AuthRepository", "🔄 Trying with +91 prefix: $phoneWithPrefix")
                val prefixQuerySnapshot = firestore.collection("users")
                    .whereEqualTo("phoneNumber", phoneWithPrefix)
                    .get()
                    .await()
                
                if (!prefixQuerySnapshot.isEmpty) {
                    Log.d("AuthRepository", "✅ Found user with +91 prefix!")
                    val document = prefixQuerySnapshot.documents.first()
                    Log.d("AuthRepository", "📄 Document data: ${document.data}")
                    
                    // Process this document the same way as above
                    val userData = document.data ?: return null
                    
                    // Log each field individually for debugging
                    Log.d("AuthRepository", "🔍 Parsing user data (fallback):")
                    Log.d("AuthRepository", "   - uid: ${userData["uid"]}")
                    Log.d("AuthRepository", "   - name: ${userData["name"]}")
                    Log.d("AuthRepository", "   - email: ${userData["email"]}")
                    Log.d("AuthRepository", "   - phoneNumber: ${userData["phoneNumber"]}")
                    Log.d("AuthRepository", "   - role: ${userData["role"]} (type: ${userData["role"]?.javaClass?.simpleName})")
                    Log.d("AuthRepository", "   - isActive: ${userData["isActive"]}")
                    Log.d("AuthRepository", "   - assignedProjects: ${userData["assignedProjects"]}")
                    
                    // Manually parse the user data to handle role conversion
                    val roleString = userData["role"] as? String
                    Log.d("AuthRepository", "🔍 Raw role from database (fallback): '$roleString'")
                    val normalizedRole = roleString
                        ?.trim()
                        ?.replace("-", "_")
                        ?.replace(" ", "_")
                        ?.uppercase()
                    Log.d("AuthRepository", "🔍 Processed role (fallback): '$normalizedRole'")
                    val userRole = when (normalizedRole) {
                        "APPROVER" -> {
                            Log.d("AuthRepository", "✅ Detected APPROVER role (fallback)")
                            UserRole.APPROVER
                        }
                        "ADMIN" -> {
                            Log.d("AuthRepository", "✅ Detected ADMIN role (fallback)")
                            UserRole.ADMIN
                        }
                        "USER" -> {
                            Log.d("AuthRepository", "✅ Detected USER role (fallback)")
                            UserRole.USER
                        }
                        "BUSINESS_HEAD", "BUSINESSHEAD" -> {
                            Log.d("AuthRepository", "✅ Detected BUSINESS_HEAD role (fallback)")
                            UserRole.BUSINESS_HEAD
                        }
                        "MANAGER" -> {
                            Log.d("AuthRepository", "✅ Detected MANAGER role (fallback)")
                            UserRole.MANAGER
                        }
                        else -> {
                            Log.w("AuthRepository", "⚠️ Unknown role (fallback): '$roleString', defaulting to USER")
                            UserRole.USER
                        }
                    }
                    
                    // Parse device info
                    val deviceInfoData = userData["deviceInfo"] as? Map<*, *>
                    val deviceInfo = DeviceInfo(
                        fcmToken = deviceInfoData?.get("fcmToken") as? String ?: "",
                        deviceId = deviceInfoData?.get("deviceId") as? String ?: "",
                        deviceModel = deviceInfoData?.get("deviceModel") as? String ?: "",
                        osVersion = deviceInfoData?.get("osVersion") as? String ?: "",
                        appVersion = deviceInfoData?.get("appVersion") as? String ?: "",
                        lastLoginAt = deviceInfoData?.get("lastLoginAt") as? Long ?: System.currentTimeMillis(),
                        isOnline = deviceInfoData?.get("isOnline") as? Boolean ?: true
                    )
                    
                    // Parse notification preferences
                    val notificationPrefsData = userData["notificationPreferences"] as? Map<*, *>
                    val notificationPreferences = NotificationPreferences(
                        pushNotifications = notificationPrefsData?.get("pushNotifications") as? Boolean ?: true,
                        expenseSubmitted = notificationPrefsData?.get("expenseSubmitted") as? Boolean ?: true,
                        expenseApproved = notificationPrefsData?.get("expenseApproved") as? Boolean ?: true,
                        expenseRejected = notificationPrefsData?.get("expenseRejected") as? Boolean ?: true,
                        projectAssignment = notificationPrefsData?.get("projectAssignment") as? Boolean ?: true,
                        pendingApprovals = notificationPrefsData?.get("pendingApprovals") as? Boolean ?: true
                    )
                    
                    val user = User(
                        uid = userData["uid"] as? String ?: "",
                        name = userData["name"] as? String ?: "",
                        email = userData["email"] as? String ?: "",
                        phone = userData["phoneNumber"] as? String ?: phoneWithPrefix,
                        role = userRole,
                        createdAt = (userData["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                        isActive = userData["isActive"] as? Boolean ?: true,
                        assignedProjects = (userData["assignedProjects"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        deviceInfo = deviceInfo,
                        notificationPreferences = notificationPreferences,
                        customerId = userData["customerId"] as? String
                    )
                    
                    Log.d("AuthRepository", "✅ Successfully parsed user (fallback):")
                    Log.d("AuthRepository", "   - Final user: $user")
                    return user
                }
                
                return null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error querying user by phone: $phoneNumber", e)
            Log.e("AuthRepository", "Exception details: ${e.message}")
            null
        }
    }
    
    /**
     * Get multiple users by phone numbers using batch query with whereIn
     * Handles Firestore's limit of 10 items per whereIn query by chunking
     */
    suspend fun getUsersByPhoneNumbers(phoneNumbers: List<String>): List<User> {
        if (phoneNumbers.isEmpty()) {
            return emptyList()
        }
        
        return try {
            Log.d("AuthRepository", "🔍 Batch querying ${phoneNumbers.size} phone numbers")
            
            // Firestore whereIn has a limit of 10, so we need to chunk
            val chunkSize = 10
            val chunks = phoneNumbers.chunked(chunkSize)
            
            // Execute all chunks in parallel
            val results = kotlinx.coroutines.coroutineScope {
                chunks.map { chunk ->
                    async {
                        try {
                            val querySnapshot = firestore.collection("users")
                                .whereIn("phoneNumber", chunk)
                                .get()
                                .await()
                            
                            querySnapshot.documents.mapNotNull { document ->
                                parseUserFromDocument(document, document.data?.get("phoneNumber") as? String)
                            }
                        } catch (e: Exception) {
                            Log.e("AuthRepository", "❌ Error in batch query chunk: ${e.message}", e)
                            emptyList<User>()
                        }
                    }
                }.map { it.await() }.flatten()
            }
            
            Log.d("AuthRepository", "✅ Batch query completed. Found ${results.size} users")
            results
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error in batch query: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Helper function to parse User from Firestore document
     */
    private fun parseUserFromDocument(document: com.google.firebase.firestore.DocumentSnapshot, phoneNumber: String?): User? {
        return try {
            val userData = document.data
            if (userData == null) return null
            
            // Parse role
            val roleString = userData["role"] as? String
            val userRole = when (roleString?.trim()?.uppercase()?.replace(" ", "_")) {
                "APPROVER" -> UserRole.APPROVER
                "ADMIN" -> UserRole.ADMIN
                "USER" -> UserRole.USER
                "BUSINESS_HEAD", "BUSINESSHEAD" -> UserRole.BUSINESS_HEAD
                "MANAGER" -> UserRole.MANAGER
                else -> UserRole.USER
            }
            
            // Parse device info
            val deviceInfoData = userData["deviceInfo"] as? Map<*, *>
            val deviceInfo = DeviceInfo(
                fcmToken = deviceInfoData?.get("fcmToken") as? String ?: "",
                deviceId = deviceInfoData?.get("deviceId") as? String ?: "",
                deviceModel = deviceInfoData?.get("deviceModel") as? String ?: "",
                osVersion = deviceInfoData?.get("osVersion") as? String ?: "",
                appVersion = deviceInfoData?.get("appVersion") as? String ?: "",
                lastLoginAt = deviceInfoData?.get("lastLoginAt") as? Long ?: System.currentTimeMillis(),
                isOnline = deviceInfoData?.get("isOnline") as? Boolean ?: true
            )
            
            // Parse notification preferences
            val notificationPrefsData = userData["notificationPreferences"] as? Map<*, *>
            val notificationPreferences = NotificationPreferences(
                pushNotifications = notificationPrefsData?.get("pushNotifications") as? Boolean ?: true,
                expenseSubmitted = notificationPrefsData?.get("expenseSubmitted") as? Boolean ?: true,
                expenseApproved = notificationPrefsData?.get("expenseApproved") as? Boolean ?: true,
                expenseRejected = notificationPrefsData?.get("expenseRejected") as? Boolean ?: true,
                projectAssignment = notificationPrefsData?.get("projectAssignment") as? Boolean ?: true,
                pendingApprovals = notificationPrefsData?.get("pendingApprovals") as? Boolean ?: true
            )
            
            User(
                uid = userData["uid"] as? String ?: "",
                name = userData["name"] as? String ?: "",
                email = userData["email"] as? String ?: "",
                phone = userData["phoneNumber"] as? String ?: phoneNumber ?: "",
                role = userRole,
                createdAt = (userData["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                isActive = userData["isActive"] as? Boolean ?: true,
                assignedProjects = (userData["assignedProjects"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                deviceInfo = deviceInfo,
                notificationPreferences = notificationPreferences,
                customerId = userData["customerId"] as? String
            )
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error parsing user from document: ${e.message}", e)
            null
        }
    }
    
    private suspend fun updateUserUid(phoneNumber: String, uid: String) {
        try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                firestore.collection("users")
                    .document(document.id)
                    .update("uid", uid)
                    .await()
                Log.d("AuthRepository", "✅ Updated user UID")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error updating user UID: ${e.message}")
        }
    }
    
    private suspend fun getUserFromFirestore(uid: String): User? {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            if (document.exists()) {
                document.toObject(User::class.java)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun createUserByPhone(phoneNumber: String, user: User): Result<Unit> {
        return try {
            firestore.collection("users").add(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun createUser(user: User): Result<Unit> {
        return try {
            firestore.collection("users").document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCurrentUserFromFirebase(): User? {
        return try {
            val firebaseUser = auth.currentUser
            Log.d("AuthRepository", "🔍 getCurrentUserFromFirebase called")
            Log.d("AuthRepository", "🔍 Firebase user: ${firebaseUser?.uid}")
            Log.d("AuthRepository", "🔍 Firebase user phone: ${firebaseUser?.phoneNumber}")
            Log.d("AuthRepository", "🔍 Firebase user email: ${firebaseUser?.email}")
            
            if (firebaseUser != null) {
                // First try to get user by phone number (for APPROVER/USER roles)
                val phoneNumber = firebaseUser.phoneNumber?.replace("+91", "") ?: ""
                if (phoneNumber.isNotBlank()) {
                    Log.d("AuthRepository", "🔍 Processing phone number: $phoneNumber")
                    Log.d("AuthRepository", "🔍 Original phone number: ${firebaseUser.phoneNumber}")
                    
                    val user = getUserByPhoneNumber(phoneNumber)
                    Log.d("AuthRepository", "🔍 getUserByPhoneNumber result: ${user?.name ?: "null"}")
                    
                    if (user != null) {
                        return user
                    }
                    
                    // Try with the original phone number as well
                    Log.d("AuthRepository", "⚠️ User not found with phone: $phoneNumber")
                    Log.d("AuthRepository", "⚠️ Trying with original phone number: ${firebaseUser.phoneNumber}")
                    val userWithOriginalPhone = getUserByPhoneNumber(firebaseUser.phoneNumber ?: "")
                    Log.d("AuthRepository", "🔍 getUserByPhoneNumber with original phone result: ${userWithOriginalPhone?.name ?: "null"}")
                    if (userWithOriginalPhone != null) {
                        return userWithOriginalPhone
                    }
                }
                
                // If phone lookup failed, try by email (for BUSINESS_HEAD role)
                val email = firebaseUser.email
                if (email != null && email.isNotBlank()) {
                    Log.d("AuthRepository", "🔍 Trying to get user by email: $email")
                    val userByEmail = getUserByEmail(email)
                    Log.d("AuthRepository", "🔍 getUserByEmail result: ${userByEmail?.name ?: "null"}")
                    if (userByEmail != null) {
                        return userByEmail
                    }
                }
                
                Log.d("AuthRepository", "❌ User not found by phone or email")
                return null
            } else {
                Log.d("AuthRepository", "❌ No Firebase user found")
                return null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error in getCurrentUserFromFirebase: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Get user by email (for Production Head users)
     * Validates only in customers collection (matching iOS behavior)
     */
    suspend fun getUserByEmail(email: String): User? {
        return try {
            Log.d("AuthRepository", "🔍 Querying customers collection for email: '$email'")
            
            val querySnapshot = firestore.collection("customers")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                val customerData = document.data ?: return null
                
                Log.d("AuthRepository", "✅ Found customer by email: ${customerData["name"]}")
                
                // Email-based login is always BUSINESS_HEAD (as per guide)
                val userRole = UserRole.BUSINESS_HEAD
                
                // Parse customer data from customers collection
                val customerName = customerData["name"] as? String ?: ""
                val customerPhone = customerData["phoneNumber"] as? String ?: ""
                val createdAt = (customerData["createdAt"] as? Timestamp)?.toDate()?.time 
                    ?: System.currentTimeMillis()
                
                // Use document ID as customerId
                val customerId = document.id
                
                // Get Firebase user UID if available
                val firebaseUser = auth.currentUser
                val uid = firebaseUser?.uid ?: customerId
                
                // Create User object dynamically from Customer data (as per guide)
                // Do NOT create/verify deviceInfo or notificationPreferences from users collection
                // These are not stored for BusinessHead users (as per guide)
                User(
                    uid = uid,
                    name = customerName,
                    email = email,
                    phone = customerPhone ?: "",
                    role = userRole,
                    createdAt = createdAt,
                    isActive = true,
                    assignedProjects = emptyList(),
                    deviceInfo = DeviceInfo(), // Empty defaults - not stored/verified for BusinessHead
                    notificationPreferences = NotificationPreferences(), // Empty defaults - not stored/verified for BusinessHead
                    customerId = customerId
                )
            } else {
                Log.d("AuthRepository", "❌ No customer found with email: $email")
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error getting customer by email: ${e.message}", e)
            null
        }
    }
    
    // Direct phone lookup for development authentication
    suspend fun getUserByPhoneNumberDirect(phoneNumber: String): User? {
        return getUserByPhoneNumber(phoneNumber)
    }
    

    
    fun getCurrentUser(): User? {
        return auth.currentUser?.let { firebaseUser ->
            User(
                uid = firebaseUser.uid,
                phone = firebaseUser.phoneNumber ?: ""
            )
        }
    }
    
    /**
     * Fetch userRole based on phone number and email logic:
     * 1. Get current user's email and phone number from Firebase Auth
     * 2. If email exists: Check customers collection -> BUSINESS_HEAD
     * 3. If phoneNumber exists: Remove +91, find document in users collection with ID = phoneNumber (without +91), get role field
     * 4. If phoneNumber == null and no email: Check if document exists in users collection without +91 prefix -> BUSINESS_HEAD
     */
    suspend fun getUserRoleByPhoneLogic(): String? {
        return try {
            val firebaseUser = auth.currentUser
            val phoneNumber = firebaseUser?.phoneNumber
            val email = firebaseUser?.email
            
            Log.d("AuthRepository", "🔍 getUserRoleByPhoneLogic - Phone: $phoneNumber, Email: $email")
            
            // First, check if user has email (BUSINESS_HEAD users use email-based auth)
            if (email != null && email.isNotBlank()) {
                Log.d("AuthRepository", "🔍 Email found, checking customers collection for BUSINESS_HEAD")
                val customerQuery = firestore.collection("customers")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .await()
                
                if (!customerQuery.isEmpty) {
                    Log.d("AuthRepository", "✅ Found in customers collection - Returning BUSINESS_HEAD")
                    return "BUSINESS_HEAD"
                } else {
                    Log.d("AuthRepository", "⚠️ Email exists but not found in customers collection")
                }
            }
            
            // If phone number exists, check users collection
            if (phoneNumber != null && phoneNumber.isNotBlank()) {
                // Remove +91 prefix from phone number
                val phoneWithoutPrefix = phoneNumber.removePrefix("+91").trim()
                Log.d("AuthRepository", "🔍 Phone without prefix: $phoneWithoutPrefix")
                
                // Look for document in users collection with ID = phoneNumber (without +91)
                val userDoc = firestore.collection("users")
                    .document(phoneWithoutPrefix)
                    .get()
                    .await()
                
                Log.d("AuthRepository", "🔍 Document exists: ${userDoc.exists()}, Document ID: ${userDoc.id}")
                
                if (userDoc.exists()) {
                    val userData = userDoc.data
                    val roleString = userData?.get("role") as? String
                    Log.d("AuthRepository", "🔍 Role from document: '$roleString'")
                    Log.d("AuthRepository", "🔍 Full user data: $userData")
                    
                    // Convert role string to enum name format
                    val processedRole = roleString?.uppercase()?.replace(" ", "_")
                    Log.d("AuthRepository", "🔍 Processed role: '$processedRole'")
                    
                    val finalRole = when (processedRole) {
                        "APPROVER" -> "APPROVER"
                        "ADMIN" -> "ADMIN"
                        "USER" -> "USER"
                        "BUSINESS_HEAD" -> "BUSINESS_HEAD"
                        "MANAGER" -> "MANAGER"
                        else -> {
                            Log.w("AuthRepository", "⚠️ Unknown role format: '$roleString', returning as-is")
                            roleString
                        }
                    }
                    Log.d("AuthRepository", "✅ Returning role: $finalRole")
                    return finalRole
                } else {
                    Log.d("AuthRepository", "❌ Document not found with ID: $phoneWithoutPrefix")
                    // Try alternative: check if document exists with phoneNumber field matching
                    val querySnapshot = firestore.collection("users")
                        .whereEqualTo("phoneNumber", phoneWithoutPrefix)
                        .limit(1)
                        .get()
                        .await()
                    
                    if (!querySnapshot.isEmpty) {
                        val doc = querySnapshot.documents.first()
                        val userData = doc.data
                        val roleString = userData?.get("role") as? String
                        Log.d("AuthRepository", "🔍 Found by phoneNumber field, role: '$roleString'")
                        
                        val processedRole = roleString?.uppercase()?.replace(" ", "_")
                        return when (processedRole) {
                            "APPROVER" -> "APPROVER"
                            "ADMIN" -> "ADMIN"
                            "USER" -> "USER"
                            "BUSINESS_HEAD" -> "BUSINESS_HEAD"
                            "MANAGER" -> "MANAGER"
                            else -> roleString
                        }
                    }
                }
                
                Log.d("AuthRepository", "❌ No user document found in users collection")
            } else {
                // Phone number is null - check if there's a document in users collection without +91 prefix
                Log.d("AuthRepository", "🔍 Phone number is null, checking for BUSINESS_HEAD in users collection")
                val usersSnapshot = firestore.collection("users")
                    .limit(100) // Check multiple documents
                    .get()
                    .await()
                
                Log.d("AuthRepository", "🔍 Found ${usersSnapshot.documents.size} documents in users collection")
                
                // Check if any document ID doesn't start with +91
                val hasDocumentWithoutPrefix = usersSnapshot.documents.any { doc ->
                    !doc.id.startsWith("+91")
                }
                
                Log.d("AuthRepository", "🔍 Has document without +91 prefix: $hasDocumentWithoutPrefix")
                
                if (hasDocumentWithoutPrefix) {
                    Log.d("AuthRepository", "✅ Returning BUSINESS_HEAD (no phone number, found document without +91)")
                    return "BUSINESS_HEAD"
                }
                Log.d("AuthRepository", "❌ No document without +91 prefix found")
            }
            
            return null
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error in getUserRoleByPhoneLogic: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
    
    suspend fun getBusinessHeadPhone(userPhone: String): String? {
        return try {
            val phoneWithoutPrefix = userPhone.replace("+91", "").trim()
            Log.d("AuthRepository", "🔍 Getting business head phone for user: $phoneWithoutPrefix")
            
            val userDoc = firestore.collection("users").document(phoneWithoutPrefix).get().await()
            if (userDoc.exists()) {
                val ownerID = userDoc.getString("ownerID")
                Log.d("AuthRepository", "🔍 Found ownerID: $ownerID")
                if (ownerID != null) {
                    val customerDoc = firestore.collection("customers").document(ownerID).get().await()
                    if (customerDoc.exists()) {
                        val bhPhone = customerDoc.getString("phoneNumber")
                        Log.d("AuthRepository", "🔍 Found business head phone: $bhPhone")
                        return bhPhone
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error getting business head phone: ${e.message}")
            null
        }
    }
    
    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    
    suspend fun signOut() {
        auth.signOut()
    }
    
    suspend fun getAllUsers(): List<User> {
        return try {
            Log.d("AuthRepository", "🔄 Fetching all users from Firestore...")
            val querySnapshot = firestore.collection("users")
                .get()
                .await()
            
            Log.d("AuthRepository", "📊 Retrieved ${querySnapshot.documents.size} user documents from Firestore")
            
            val users = querySnapshot.documents.mapNotNull { document ->
                try {
                    val userData = document.data ?: return@mapNotNull null
                    
                    val roleString = userData["role"] as? String
                    val userRole = when (roleString?.trim()?.uppercase()?.replace(" ", "_")) {
                        "APPROVER" -> UserRole.APPROVER
                        "ADMIN" -> UserRole.ADMIN
                        "USER" -> UserRole.USER
                        "BUSINESS_HEAD", "BUSINESSHEAD" -> UserRole.BUSINESS_HEAD
                        "MANAGER" -> UserRole.MANAGER
                        else -> UserRole.USER
                    }
                    
                    // Parse user data - only the fields we store: createdAt, fcmToken, isActive, name, ownerID, phoneNumber, role
                    val fcmToken = userData["fcmToken"] as? String ?: ""
                    val createdAt = (userData["createdAt"] as? Timestamp)?.toDate()?.time 
                        ?: (userData["timestamp"] as? Timestamp)?.toDate()?.time 
                        ?: System.currentTimeMillis()
                    
                    // Map ownerID to customerId (for backward compatibility with User model)
                    val ownerID = userData["ownerID"] as? String
                    val customerId = ownerID ?: (userData["customerId"] as? String)
                    
                    // Create default device info and notification preferences for User model compatibility
                    val deviceInfo = DeviceInfo(
                        fcmToken = fcmToken,
                        deviceId = "",
                        deviceModel = "",
                        osVersion = "",
                        appVersion = "",
                        lastLoginAt = System.currentTimeMillis(),
                        isOnline = true
                    )
                    
                    val notificationPreferences = NotificationPreferences(
                        pushNotifications = true,
                        expenseSubmitted = true,
                        expenseApproved = true,
                        expenseRejected = true,
                        projectAssignment = true,
                        pendingApprovals = true
                    )
                    
                    User(
                        uid = userData["uid"] as? String ?: document.id,
                        name = userData["name"] as? String ?: "",
                        email = userData["email"] as? String ?: "",
                        phone = userData["phoneNumber"] as? String ?: "",
                        role = userRole,
                        createdAt = createdAt,
                        isActive = userData["isActive"] as? Boolean ?: true,
                        assignedProjects = emptyList(), // Not stored in new structure
                        deviceInfo = deviceInfo,
                        notificationPreferences = notificationPreferences,
                        customerId = customerId, // Map from ownerID
                        department = (userData["department"] as? String ?: "")
                    )
                } catch (e: Exception) {
                    Log.e("AuthRepository", "❌ Error parsing user document ${document.id}: ${e.message}")
                    e.printStackTrace()
                    null
                }
            }
            
            Log.d("AuthRepository", "✅ Successfully parsed ${users.size} users from ${querySnapshot.documents.size} documents")
            Log.d("AuthRepository", "📋 User roles found: ${users.map { it.role.name }.distinct()}")
            users
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error fetching all users: ${e.message}")
            Log.e("AuthRepository", "❌ Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun createUserByAdmin(user: User, customerId: String? = null): Result<Unit> {
        return try {
            // Check if user already exists
            val existingUser = getUserByPhoneNumber(user.phone)
            if (existingUser != null) {
                return Result.failure(Exception("User with this phone number already exists"))
            }
            
            // Get customerId from current user if not provided
            val finalCustomerId = customerId ?: run {
                val firebaseUser = auth.currentUser
                firebaseUser?.uid
            }
            
            // Create user document with ONLY the fields shown in the image:
            // createdAt, fcmToken, isActive, name, ownerID, phoneNumber, role, department
            val userMap = mutableMapOf<String, Any>(
                "name" to user.name,
                "phoneNumber" to user.phone,
                "role" to user.role.firestoreValue,
                "createdAt" to com.google.firebase.Timestamp.now(),
                "isActive" to user.isActive,
                "fcmToken" to "", // Will be updated on login
                "department" to (user.department.ifBlank { "" })
            )
            
            // Add ownerID (mapped from customerId) if available
            if (finalCustomerId != null) {
                userMap["ownerID"] = finalCustomerId
                Log.d("AuthRepository", "📋 Storing ownerID: $finalCustomerId for user: ${user.name}")
            }

            firestore.collection("users")
                .document(user.phone) // Use phone number as document ID
                .set(userMap)         // Replaces `.add(...)`
                .await()
            Log.d("AuthRepository", "✅ Created user: ${user.name} with role: ${user.role}, customerId: $finalCustomerId")

            // Also update department count in the customer's document
            // For USER role   -> departmentUserCounts.<dept>
            // For APPROVER    -> departmentApproverCounts.<dept>
            if (user.department.isNotBlank() && finalCustomerId != null) {
                val (fieldPrefix, label) = when (user.role) {
                    UserRole.USER -> "departmentUserCounts" to "departmentUserCounts"
                    UserRole.APPROVER -> "departmentApproverCounts" to "departmentApproverCounts"
                    else -> null to null
                }

                if (fieldPrefix != null) {
                    try {
                        firestore.collection("customers")
                            .document(finalCustomerId)
                            .update(FieldPath.of(fieldPrefix, user.department), FieldValue.increment(1))
                            .await()
                        Log.d(
                            "AuthRepository",
                            "✅ Incremented $label for department='${user.department}' (customerId=$finalCustomerId)"
                        )
                    } catch (e: Exception) {
                        Log.e(
                            "AuthRepository",
                            "⚠️ Failed to update $label for department='${user.department}': ${e.message}"
                        )
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error creating user: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getUsersByRole(role: UserRole): List<User> {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("role", role.firestoreValue)
                .get()
                .await()
            
            querySnapshot.documents.mapNotNull { document ->
                try {
                    val userData = document.data ?: return@mapNotNull null
                    
                    val roleString = userData["role"] as? String
                    val userRole = when (roleString?.trim()?.uppercase()?.replace(" ", "_")) {
                        "APPROVER" -> UserRole.APPROVER
                        "ADMIN" -> UserRole.ADMIN
                        "USER" -> UserRole.USER
                        "BUSINESS_HEAD", "BUSINESSHEAD" -> UserRole.BUSINESS_HEAD
                        "MANAGER" -> UserRole.MANAGER
                        else -> UserRole.USER
                    }
                    
                    // Parse user data - only the fields we store: createdAt, fcmToken, isActive, name, ownerID, phoneNumber, role
                    val fcmToken = userData["fcmToken"] as? String ?: ""
                    val createdAt = (userData["createdAt"] as? Timestamp)?.toDate()?.time 
                        ?: (userData["timestamp"] as? Timestamp)?.toDate()?.time 
                        ?: System.currentTimeMillis()
                    
                    // Map ownerID to customerId (for backward compatibility with User model)
                    val ownerID = userData["ownerID"] as? String
                    val customerId = ownerID ?: (userData["customerId"] as? String)
                    
                    // Create default device info and notification preferences for User model compatibility
                    val deviceInfo = DeviceInfo(
                        fcmToken = fcmToken,
                        deviceId = "",
                        deviceModel = "",
                        osVersion = "",
                        appVersion = "",
                        lastLoginAt = System.currentTimeMillis(),
                        isOnline = true
                    )
                    
                    val notificationPreferences = NotificationPreferences(
                        pushNotifications = true,
                        expenseSubmitted = true,
                        expenseApproved = true,
                        expenseRejected = true,
                        projectAssignment = true,
                        pendingApprovals = true
                    )
                    
                    User(
                        uid = userData["uid"] as? String ?: document.id,
                        name = userData["name"] as? String ?: "",
                        email = userData["email"] as? String ?: "",
                        phone = userData["phoneNumber"] as? String ?: "",
                        role = userRole,
                        createdAt = createdAt,
                        isActive = userData["isActive"] as? Boolean ?: true,
                        assignedProjects = emptyList(), // Not stored in new structure
                        deviceInfo = deviceInfo,
                        notificationPreferences = notificationPreferences,
                        customerId = customerId, // Map from ownerID
                        department = (userData["department"] as? String ?: "")
                    )
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Error parsing user document: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching users by role: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun getUserById(userId: String): User? {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("uid", userId)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                val userData = document.data ?: return null
                
                val roleString = userData["role"] as? String
                val userRole = when (roleString?.uppercase()?.replace(" ", "_")) {
                    "APPROVER" -> UserRole.APPROVER
                    "ADMIN" -> UserRole.ADMIN
                    "USER" -> UserRole.USER
                    "BUSINESS_HEAD" -> UserRole.BUSINESS_HEAD
                    else -> UserRole.USER
                }
                
                // Parse device info
                val deviceInfoData = userData["deviceInfo"] as? Map<*, *>
                val deviceInfo = DeviceInfo(
                    fcmToken = deviceInfoData?.get("fcmToken") as? String ?: "",
                    deviceId = deviceInfoData?.get("deviceId") as? String ?: "",
                    deviceModel = deviceInfoData?.get("deviceModel") as? String ?: "",
                    osVersion = deviceInfoData?.get("osVersion") as? String ?: "",
                    appVersion = deviceInfoData?.get("appVersion") as? String ?: "",
                    lastLoginAt = deviceInfoData?.get("lastLoginAt") as? Long ?: System.currentTimeMillis(),
                    isOnline = deviceInfoData?.get("isOnline") as? Boolean ?: true
                )
                
                // Parse notification preferences
                val notificationPrefsData = userData["notificationPreferences"] as? Map<*, *>
                val notificationPreferences = NotificationPreferences(
                    pushNotifications = notificationPrefsData?.get("pushNotifications") as? Boolean ?: true,
                    expenseSubmitted = notificationPrefsData?.get("expenseSubmitted") as? Boolean ?: true,
                    expenseApproved = notificationPrefsData?.get("expenseApproved") as? Boolean ?: true,
                    expenseRejected = notificationPrefsData?.get("expenseRejected") as? Boolean ?: true,
                    projectAssignment = notificationPrefsData?.get("projectAssignment") as? Boolean ?: true,
                    pendingApprovals = notificationPrefsData?.get("pendingApprovals") as? Boolean ?: true
                )
                
                User(
                    uid = userData["uid"] as? String ?: "",
                    name = userData["name"] as? String ?: "",
                    email = userData["email"] as? String ?: "",
                    phone = userData["phoneNumber"] as? String ?: "",
                    role = userRole,
                    createdAt = (userData["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                    isActive = userData["isActive"] as? Boolean ?: true,
                    assignedProjects = (userData["assignedProjects"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    deviceInfo = deviceInfo,
                    notificationPreferences = notificationPreferences
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching user by ID: ${e.message}")
            null
        }
    }
    
    suspend fun updateUserFCMToken(userId: String, fcmToken: String): Result<Unit> {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("uid", userId)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                firestore.collection("users")
                    .document(document.id)
                    .update("deviceInfo.fcmToken", fcmToken)
                    .await()
                Log.d("AuthRepository", "✅ Updated FCM token for user: $userId")
                Result.success(Unit)
            } else {
                Log.e("AuthRepository", "❌ User not found for FCM token update: $userId")
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error updating FCM token: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateUserActiveStatus(phoneNumber: String, isActive: Boolean): Result<Unit> {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                firestore.collection("users")
                    .document(document.id)
                    .update("isActive", isActive)
                    .await()
                Log.d("AuthRepository", "✅ Updated isActive status for user: $phoneNumber to $isActive")
                Result.success(Unit)
            } else {
                Log.e("AuthRepository", "❌ User not found for isActive update: $phoneNumber")
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error updating isActive status: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Email/Password Authentication for Production Head
    suspend fun signUpWithEmail(
        name: String,
        email: String,
        password: String,
        phoneNumber: String?,
        businessName: String,
        businessType: String?,
        location: String?
    ): Result<User> {
        return try {
            val existingCustomer = firestore.collection("customers")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (!existingCustomer.isEmpty) {
                Log.e("AuthRepository", "❌ Email already exists in customers collection: $email")
                return Result.failure(Exception("Email already created. Please use another email or log in with existing credentials."))
            }

            // Create user in Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                // Create Customer document in Firestore with UID as document ID
                val customer = Customer(
                    name = name,
                    email = email,
                    phoneNumber = phoneNumber,
                    businessName = businessName,
                    businessType = businessType,
                    location = location,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
                
                // Store customer in Firestore "customers" collection with UID as document ID
                // This ensures projects are stored under customers/{uid}/Projects
                firestore.collection("customers")
                    .document(firebaseUser.uid)
                    .set(customer)
                    .await()
                
                Log.d("AuthRepository", "✅ Customer created in Firestore with UID: ${firebaseUser.uid} for email: $email")
                
                // For BusinessHead signup, ONLY create Customer document
                // Do NOT create User document in users collection (as per guide)
                // User object will be created dynamically from Customer data during login
                
                // Create User object for return value (in-memory only, not stored in Firestore)
                val user = User(
                    uid = firebaseUser.uid,
                    name = name,
                    email = email,
                    phone = phoneNumber ?: "",
                    role = UserRole.BUSINESS_HEAD,
                    createdAt = System.currentTimeMillis(),
                    isActive = true,
                    assignedProjects = emptyList(),
                    deviceInfo = DeviceInfo(), // Empty defaults
                    notificationPreferences = NotificationPreferences(), // Empty defaults
                    customerId = firebaseUser.uid
                )
                
                Log.d("AuthRepository", "✅ BusinessHead account created (Customer document only, no User document in users collection)")
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to create Firebase user"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Sign up error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            Log.d("AuthRepository", "🔐 Starting email sign in for: $email")
            
            // First, check if customer exists in customers collection (matching iOS behavior)
            Log.d("AuthRepository", "🔍 Checking customers collection for email: $email")
            val customerQuerySnapshot = firestore.collection("customers")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            if (customerQuerySnapshot.isEmpty) {
                Log.e("AuthRepository", "❌ Customer not found in customers collection for email: $email")
                return Result.failure(Exception("Customer not found. Please check your email or contact administrator."))
            }
            
            val customerDocument = customerQuerySnapshot.documents.first()
            val customerData = customerDocument.data ?: return Result.failure(Exception("Customer data not found"))
            Log.d("AuthRepository", "✅ Found customer in customers collection: ${customerData["name"]}")
            
            // Now authenticate with Firebase Auth
            Log.d("AuthRepository", "🔐 Authenticating with Firebase Auth...")
            val authResult = try {
                auth.signInWithEmailAndPassword(email, password).await()
            } catch (authError: Exception) {
                Log.e("AuthRepository", "❌ Firebase Auth error: ${authError.message}")
                Log.e("AuthRepository", "❌ Firebase Auth error type: ${authError.javaClass.simpleName}")
                
                // Check if it's a network error or user not found
                val errorMessage = authError.message ?: "Unknown error"
                when {
                    errorMessage.contains("network", ignoreCase = true) -> {
                        Log.e("AuthRepository", "⚠️ Network error detected. This might be a connectivity issue.")
                        return Result.failure(Exception("Network error. Please check your internet connection and try again."))
                    }
                    errorMessage.contains("user-not-found", ignoreCase = true) || 
                    errorMessage.contains("wrong-password", ignoreCase = true) || 
                    errorMessage.contains("invalid-credential", ignoreCase = true) -> {
                        Log.e("AuthRepository", "⚠️ Invalid credentials. User may not exist in Firebase Auth.")
                        return Result.failure(Exception("Invalid email or password. Please check your credentials."))
                    }
                    else -> {
                        return Result.failure(Exception("Authentication failed: $errorMessage"))
                    }
                }
            }
            
            val firebaseUser = authResult.user
            if (firebaseUser == null) {
                Log.e("AuthRepository", "❌ Firebase Auth returned null user")
                return Result.failure(Exception("Authentication failed: No user returned"))
            }
            
            Log.d("AuthRepository", "✅ Firebase Auth successful. UID: ${firebaseUser.uid}")
            
            // Email login = always BUSINESS_HEAD (as per guide requirement)
            val userRole = UserRole.BUSINESS_HEAD
            
            // Parse customer data from customers collection
            val customerName = customerData["name"] as? String ?: ""
            val customerPhone = customerData["phoneNumber"] as? String ?: ""
            val createdAt = (customerData["createdAt"] as? Timestamp)?.toDate()?.time 
                ?: System.currentTimeMillis()
            
            // Use document ID as customerId
            val customerId = customerDocument.id
            
            // Create User object dynamically from Customer data (as per guide)
            // Do NOT look for User document in users collection for BusinessHead
            // Do NOT verify deviceInfo or notificationPreferences from users collection
            val user = User(
                uid = firebaseUser.uid,
                name = customerName,
                email = email,
                phone = customerPhone ?: "",
                role = userRole,
                createdAt = createdAt,
                isActive = true,
                assignedProjects = emptyList(),
                deviceInfo = DeviceInfo(), // Empty defaults (not stored/verified from users collection)
                notificationPreferences = NotificationPreferences(), // Empty defaults (not stored/verified from users collection)
                customerId = customerId
            )
            
            Log.d("AuthRepository", "✅ Sign in successful for customer: ${user.name} with role: ${user.role}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Sign in error: ${e.message}", e)
            Log.e("AuthRepository", "❌ Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Update a user's department field in the users collection.
     * Uses phoneNumber as document ID (matching createUserByAdmin).
     */
    suspend fun updateUserDepartment(phoneNumber: String, department: String): Result<Unit> {
        return try {
            val cleanPhone = phoneNumber.replace("+91", "").trim()
            Log.d("AuthRepository", "🔄 Updating department for user: $cleanPhone to '$department'")
            
            firestore.collection("users")
                .document(cleanPhone)
                .update("department", department)
                .await()
            
            Log.d("AuthRepository", "✅ Updated department for user: $cleanPhone")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error updating user department: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Sets departmentName in CustomerUserRelation for a specific customer + user pair.
     * Used when assigning or adding a user/approver to a department.
     */
    suspend fun updateCustomerUserRelationDepartment(
        phoneNumber: String,
        customerId: String,
        departmentName: String
    ): Result<Unit> {
        return try {
            val cleanPhone = phoneNumber.replace("+91", "").trim()
            Log.d(
                "AuthRepository",
                "🔄 Setting CustomerUserRelation.departmentName='$departmentName' for user=$cleanPhone, customer=$customerId"
            )

            val snapshot = firestore.collection("CustomerUserRelation")
                .whereEqualTo("userID", cleanPhone)
                .whereEqualTo("CustomerId", customerId)
                .get()
                .await()

            if (snapshot.isEmpty) {
                Log.d("AuthRepository", "ℹ️ No CustomerUserRelation found for user=$cleanPhone, customer=$customerId")
                return Result.success(Unit)
            }

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.update(
                    doc.reference,
                    mapOf(
                        "departmentName" to departmentName,
                        "updatedAt" to Timestamp.now()
                    )
                )
            }
            batch.commit().await()

            Log.d("AuthRepository", "✅ Updated departmentName in ${snapshot.size()} CustomerUserRelation document(s)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error updating CustomerUserRelation departmentName: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Clears departmentName in CustomerUserRelation for a specific customer + user pair.
     */
    suspend fun clearCustomerUserRelationDepartment(phoneNumber: String, customerId: String): Result<Int> {
        return try {
            val cleanPhone = phoneNumber.replace("+91", "").trim()
            Log.d(
                "AuthRepository",
                "🔄 Clearing CustomerUserRelation.departmentName for user=$cleanPhone, customer=$customerId"
            )

            val snapshot = firestore.collection("CustomerUserRelation")
                .whereEqualTo("userID", cleanPhone)
                .whereEqualTo("CustomerId", customerId)
                .get()
                .await()

            if (snapshot.isEmpty) {
                Log.d("AuthRepository", "ℹ️ No CustomerUserRelation documents found for user=$cleanPhone, customer=$customerId")
                return Result.success(0)
            }

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.update(
                    doc.reference,
                    mapOf(
                        "departmentName" to "",
                        "updatedAt" to Timestamp.now()
                    )
                )
            }
            batch.commit().await()

            Log.d("AuthRepository", "✅ Cleared departmentName in ${snapshot.size()} CustomerUserRelation document(s)")
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error clearing CustomerUserRelation departmentName: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if a user exists for a specific customer
     * Matches iOS CheckUserExistsForCustomer logic
     */
    suspend fun checkUserExistsForCustomer(phoneNumber: String, customerId: String): Boolean {
        return try {
            val cleanPhone = phoneNumber.replace("+91", "").trim()
            Log.d("AuthRepository", "🔍 Checking if user exists: phone=$cleanPhone, customerId=$customerId")
            
            val querySnapshot = firestore.collection("CustomerUserRelation")
                .whereEqualTo("userID", cleanPhone)
                .whereEqualTo("CustomerId", customerId)
                .limit(1)
                .get()
                .await()
            
            val exists = !querySnapshot.isEmpty
            Log.d("AuthRepository", if (exists) "✅ User already exists for this customer" else "ℹ️ User does not exist for this customer")
            exists
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error checking user existence: ${e.message}")
            throw Exception("Failed to check for existing user: ${e.message}")
        }
    }
    
    /**
     * Fetch all CustomerUserRelation records for a specific customer
     * Matches iOS UserListViewModel.fetchUsers() logic
     */
    suspend fun fetchCustomerUserRelations(customerId: String): Result<List<CustomerUserRelation>> {
        return try {
            Log.d("AuthRepository", "🔍 Fetching CustomerUserRelations for customerId: $customerId")
            
            val querySnapshot = firestore.collection("CustomerUserRelation")
                .whereEqualTo("CustomerId", customerId)
                .get()
                .await()
            
            val relations = querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(CustomerUserRelation::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    Log.e("AuthRepository", "⚠️ Failed to parse relation document ${document.id}: ${e.message}")
                    null // Skip malformed documents
                }
            }.sortedBy { it.UserName } // Sort by Username as per iOS logic
            
            Log.d("AuthRepository", "✅ Fetched ${relations.size} CustomerUserRelations")
            Result.success(relations)
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error fetching CustomerUserRelations: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Toggle the isActive status of a CustomerUserRelation
     * Implements optimistic update pattern from iOS
     */
    suspend fun toggleUserRelationStatus(relationId: String, newStatus: Boolean): Result<Unit> {
        return try {
            if (relationId.isBlank()) {
                return Result.failure(Exception("Relation ID is missing"))
            }
            
            Log.d("AuthRepository", "🔄 Toggling user relation status: id=$relationId, newStatus=$newStatus")
            
            firestore.collection("CustomerUserRelation")
                .document(relationId)
                .update("isActive", newStatus)
                .await()
            
            Log.d("AuthRepository", "✅ Successfully toggled user relation status")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error toggling user relation status: ${e.message}")
            Result.failure(Exception("Failed to update user status: ${e.message}"))
        }
    }
    
    /**
     * Fetch CustomerUserRelations filtered by department and role
     * Used for fetching users/approvers for project department assignment
     */
    suspend fun fetchCustomerUserRelationsByDepartmentAndRole(
        customerId: String,
        departmentName: String,
        role: String
    ): Result<List<CustomerUserRelation>> {
        return try {
            Log.d("AuthRepository", "🔍 Fetching CustomerUserRelations: customerId=$customerId, dept=$departmentName, role=$role")
            
            val querySnapshot = firestore.collection("CustomerUserRelation")
                .whereEqualTo("CustomerId", customerId)
                .whereEqualTo("departmentName", departmentName)
                .whereEqualTo("role", role)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            val relations = querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(CustomerUserRelation::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    Log.e("AuthRepository", "⚠️ Failed to parse relation document ${document.id}: ${e.message}")
                    null
                }
            }.sortedBy { it.UserName }

            Log.d("AuthRepository", "✅ Fetched ${relations.size} CustomerUserRelations for dept=$departmentName, role=$role")
            Result.success(relations)
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error fetching CustomerUserRelations: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Convert CustomerUserRelation to User object for UI compatibility
     * Maps relation fields to User model fields
     */
    fun convertRelationToUser(relation: CustomerUserRelation): User {
        // Map role string to UserRole enum
        val userRole = when (relation.role.trim().replace("-", "_").replace(" ", "_").uppercase()) {
            "USER" -> UserRole.USER
            "APPROVER" -> UserRole.APPROVER
            "BUSINESS_HEAD", "BUSINESSHEAD" -> UserRole.BUSINESS_HEAD
            "ADMIN" -> UserRole.ADMIN
            "MANAGER" -> UserRole.MANAGER
            else -> UserRole.USER
        }
        
        return User(
            uid = "",
            name = relation.UserName,
            email = "",
            phone = relation.userID,
            role = userRole,
            department = relation.departmentName,
            isActive = relation.isActive,
            assignedProjects = emptyList(),
            deviceInfo = DeviceInfo(),
            notificationPreferences = NotificationPreferences(),
            createdAt = relation.createdAt?.seconds?.times(1000) ?: System.currentTimeMillis(),
            customerId = relation.CustomerId
        )
    }
    
    /**
     * Generate a unique user code for CustomerUserRelation
     */
    private fun generateUserCode(): String {
        return "U${System.currentTimeMillis().toString().takeLast(8)}"
    }

    private suspend fun upsertGlobalUserDocument(
        phoneNumber: String,
        name: String,
        role: UserRole,
        department: String,
        customerId: String
    ) {
        val userRef = firestore.collection("users").document(phoneNumber)
        val snapshot = userRef.get().await()

        val existingData = snapshot.data.orEmpty()
        val existingCustomerMap = mutableMapOf<String, String>()
        (existingData["CustomerID"] as? Map<*, *>)?.forEach { (key, value) ->
            val customerKey = key as? String
            val roleValue = value as? String
            if (customerKey != null && roleValue != null) {
                existingCustomerMap[customerKey] = roleValue
            }
        }

        existingCustomerMap[customerId] = role.firestoreValue

        val existingOwnerId = (existingData["ownerID"] as? String)
            ?.takeIf { it.isNotBlank() }
            ?: (existingData["customerId"] as? String)?.takeIf { it.isNotBlank() }
            ?: customerId

        val userData = mutableMapOf<String, Any>(
            "name" to ((existingData["name"] as? String)?.takeIf { it.isNotBlank() } ?: name),
            "phoneNumber" to phoneNumber,
            "role" to role.firestoreValue,
            "createdAt" to (existingData["createdAt"] ?: Timestamp.now()),
            "isActive" to (existingData["isActive"] as? Boolean ?: true),
            "ownerID" to existingOwnerId,
            "customerId" to existingOwnerId,
            "CustomerID" to existingCustomerMap,
            "department" to department
        )

        userData["fcmToken"] = existingData["fcmToken"] ?: ""

        userRef.set(userData, com.google.firebase.firestore.SetOptions.merge()).await()
    }
    
    /**
     * Create user with CustomerUserRelation support
     * Supports both new user creation and overwrite of existing relation
     */
    suspend fun createUserWithRelation(
        phoneNumber: String,
        name: String,
        role: UserRole,
        department: String,
        customerId: String,
        overwrite: Boolean = false
    ): Result<Unit> {
        return try {
            val cleanPhone = phoneNumber.replace("+91", "").trim()
            Log.d("AuthRepository", "🔄 Creating user: phone=$cleanPhone, name=$name, role=$role, department=$department, overwrite=$overwrite")

            upsertGlobalUserDocument(
                phoneNumber = cleanPhone,
                name = name,
                role = role,
                department = department,
                customerId = customerId
            )
            
            if (overwrite) {
                // Overwrite path: Update existing CustomerUserRelation
                Log.d("AuthRepository", "🔄 Overwrite mode: Updating existing relation")
                
                val querySnapshot = firestore.collection("CustomerUserRelation")
                    .whereEqualTo("userID", cleanPhone)
                    .whereEqualTo("CustomerId", customerId)
                    .limit(1)
                    .get()
                    .await()
                
                if (querySnapshot.isEmpty) {
                    return Result.failure(Exception("No existing relation found to overwrite"))
                }
                
                val doc = querySnapshot.documents.first()
                val existingUserCode = doc.getString("userCode") ?: generateUserCode()
                
                val updates = hashMapOf<String, Any>(
                    "userCode" to existingUserCode,
                    "departmentName" to department,
                    "UserName" to name,
                    "role" to role.firestoreValue,
                    "isActive" to true,
                    "updatedAt" to Timestamp.now()
                )
                
                firestore.collection("CustomerUserRelation")
                    .document(doc.id)
                    .update(updates)
                    .await()
                
                Log.d("AuthRepository", "✅ Updated existing CustomerUserRelation for user: $cleanPhone")
            } else {
                Log.d("AuthRepository", "🔄 Normal mode: Creating CustomerUserRelation")

                val userCode = generateUserCode()
                val relationMap = mapOf(
                    "userID" to cleanPhone,
                    "CustomerId" to customerId,
                    "userCode" to userCode,
                    "departmentName" to department,
                    "UserName" to name,
                    "role" to role.firestoreValue,
                    "isActive" to true,
                    "createdAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )

                firestore.collection("CustomerUserRelation")
                    .add(relationMap)
                    .await()

                Log.d("AuthRepository", "✅ Created CustomerUserRelation for user: $cleanPhone with code: $userCode")
            }
            
            // 3. Update department counts in customer document
            if (department.isNotBlank()) {
                val fieldPrefix = when (role) {
                    UserRole.USER -> "departmentUserCounts"
                    UserRole.APPROVER -> "departmentApproverCounts"
                    else -> null
                }
                
                if (fieldPrefix != null) {
                    try {
                        firestore.collection("customers")
                            .document(customerId)
                            .update(FieldPath.of(fieldPrefix, department), FieldValue.increment(1))
                            .await()
                        Log.d("AuthRepository", "✅ Incremented $fieldPrefix for department='$department'")
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "⚠️ Failed to update department count: ${e.message}")
                        // Don't fail the entire operation
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error creating user with relation: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Update User_Customer_Project_Phase_Relation for a single user/approver
     * Follows iOS logic from User_Customer_Project_Phase_Relation_Save_Logic.md
     * 
     * Document structure:
     * {
     *   "<customerId>": {
     *     "<projectId>": ["phaseId1", "phaseId2", ...]
     *   }
     * }
     * 
     * @param phone User phone number (document ID)
     * @param customerId Business head's customer ID
     * @param projectId Project ID
     * @param phaseId Phase ID to add
     */
    suspend fun updateUserCustomerProjectPhaseRelation(
        phone: String,
        customerId: String,
        projectId: String,
        phaseId: String
    ): Result<Unit> {
        return try {
            val cleanPhone = phone.replace("+91", "").trim()
            if (cleanPhone.isEmpty() || customerId.isEmpty() || projectId.isEmpty() || phaseId.isEmpty()) {
                return Result.failure(Exception("Invalid parameters: phone, customerId, projectId, or phaseId is empty"))
            }
            
            Log.d("AuthRepository", "🔄 Updating User_Customer_Project_Phase_Relation: phone=$cleanPhone, customerId=$customerId, projectId=$projectId, phaseId=$phaseId")
            
            val docRef = firestore.collection("User_Customer_Project_Phase_Relation").document(cleanPhone)
            
            // Read existing document
            val snapshot = docRef.get().await()
            
            // Start with a set containing the new phaseId
            val phaseIds = mutableSetOf(phaseId)
            
            // If document exists, check for existing phase IDs
            if (snapshot.exists()) {
                val data = snapshot.data
                val customerMap = data?.get(customerId) as? Map<*, *>
                val existingPhaseList = customerMap?.get(projectId) as? List<*>
                
                if (existingPhaseList != null) {
                    // Merge existing phase IDs
                    existingPhaseList.forEach { existingPhase ->
                        if (existingPhase is String) {
                            phaseIds.add(existingPhase)
                        }
                    }
                    Log.d("AuthRepository", "📋 Found ${existingPhaseList.size} existing phases, merging with new phase")
                }
            }
            
            // Prepare payload with sorted unique phase IDs
            val payload = mapOf(
                customerId to mapOf(
                    projectId to phaseIds.toList().sorted()
                )
            )
            
            // Save with merge to preserve other customers/projects
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            
            Log.d("AuthRepository", "✅ Successfully updated User_Customer_Project_Phase_Relation for phone=$cleanPhone (${phaseIds.size} total phases)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error updating User_Customer_Project_Phase_Relation: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Validate that a user has no active project/phase assignments for the customer,
     * then delete their CustomerUserRelation document.
     *
     * Validation: checks User_Customer_Project_Phase_Relation document (doc ID = cleanPhone).
     * If the customerId key exists and the inner map has any non-null value, deletion is blocked.
     */
    suspend fun validateAndDeleteUserFromCustomer(phoneNumber: String, customerId: String, role: String): Result<Unit> {
        return try {
            val cleanPhone = phoneNumber.replace("+91", "").trim()

            Log.d("AuthRepository", "🔍 Checking deletion eligibility: user=$cleanPhone, customer=$customerId, role=$role")

            // Manager check: if role is MANAGER, verify no projects are assigned to them
            if (role.trim().uppercase() == "MANAGER") {
                val projectsSnapshot = firestore
                    .collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .whereEqualTo("managerId", cleanPhone)
                    .get()
                    .await()

                if (!projectsSnapshot.isEmpty) {
                    val count = projectsSnapshot.size()
                    Log.w("AuthRepository", "⚠️ Cannot delete manager $cleanPhone - assigned to $count project(s)")
                    return Result.failure(
                        Exception("Can't delete the manager, this manager is assigned to $count project${if (count > 1) "s" else ""}, so can't delete")
                    )
                }
            }

            val phaseRelationDoc = firestore
                .collection("User_Customer_Project_Phase_Relation")
                .document(cleanPhone)
                .get()
                .await()

            if (phaseRelationDoc.exists()) {
                val customerEntry = phaseRelationDoc.data?.get(customerId)
                if (customerEntry != null) {
                    val innerMap = customerEntry as? Map<*, *>
                    if (innerMap != null) {
                        val hasAssignments = innerMap.values.any { it != null }
                        if (hasAssignments) {
                            Log.w("AuthRepository", "⚠️ Cannot delete: user $cleanPhone has active assignments")
                            return Result.failure(
                                Exception("Can't delete, please replace in department user management screen")
                            )
                        }
                    }
                }
            }

            val querySnapshot = firestore
                .collection("CustomerUserRelation")
                .whereEqualTo("userID", cleanPhone)
                .whereEqualTo("CustomerId", customerId)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("User relation not found"))
            }

            for (document in querySnapshot.documents) {
                document.reference.delete().await()
            }

            Log.d("AuthRepository", "✅ Deleted user $cleanPhone from customer $customerId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error deleting user: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update User_Customer_Project_Phase_Relation for multiple users/approvers
     * Follows iOS logic - processes all phone numbers and updates their phase access
     *
     * @param phones Set of phone numbers to update
     * @param customerId Business head's customer ID
     * @param projectId Project ID
     * @param phaseId Phase ID to add
     */
    suspend fun updateUserCustomerProjectPhaseRelationForMultiple(
        phones: Set<String>,
        customerId: String,
        projectId: String,
        phaseId: String
    ): Result<Unit> {
        return try {
            if (phones.isEmpty()) {
                Log.d("AuthRepository", "⚠️ No phones provided for User_Customer_Project_Phase_Relation update")
                return Result.success(Unit)
            }
            
            Log.d("AuthRepository", "🔄 Updating User_Customer_Project_Phase_Relation for ${phones.size} users")
            
            // Process each phone number
            val results = phones.map { phone ->
                updateUserCustomerProjectPhaseRelation(phone, customerId, projectId, phaseId)
            }
            
            // Check if any updates failed
            val failures = results.filter { it.isFailure }
            if (failures.isNotEmpty()) {
                val errorMessages = failures.mapNotNull { it.exceptionOrNull()?.message }
                Log.e("AuthRepository", "⚠️ ${failures.size} updates failed: ${errorMessages.joinToString(", ")}")
                return Result.failure(Exception("${failures.size} out of ${phones.size} updates failed"))
            }
            
            Log.d("AuthRepository", "✅ Successfully updated User_Customer_Project_Phase_Relation for all ${phones.size} users")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error updating User_Customer_Project_Phase_Relation for multiple users: ${e.message}")
            Result.failure(e)
        }
    }
} 
