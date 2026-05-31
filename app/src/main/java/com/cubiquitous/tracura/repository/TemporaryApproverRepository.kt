package com.cubiquitous.tracura.repository

import android.util.Log
import com.cubiquitous.tracura.model.TemporaryApprover
import com.cubiquitous.tracura.model.isExpired
import com.cubiquitous.tracura.model.Notification
import com.cubiquitous.tracura.model.NotificationType
import com.cubiquitous.tracura.service.NotificationService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import dagger.Lazy
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemporaryApproverRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationService: Lazy<NotificationService>
) {
    
    companion object {
        private const val TAG = "TempApproverRepository"
        private const val COLLECTION_PROJECTS = "projects"
        private const val SUBCOLLECTION_TEMP_APPROVERS = "temporaryApprovers"
    }
    
    /**
     * Add a temporary approver to a project
     */
    suspend fun addTemporaryApprover(
        projectId: String,
        approverId: String,
        approverName: String,
        approverPhone: String,
        startdate: Timestamp,
        expiringDate: Timestamp,
        assignedBy: String,
        assignedByName: String
    ): Result<TemporaryApprover> {
        return try {
            Log.d(TAG, "🔄 Adding temporary approver $approverName to project $projectId")
            
            val tempApprover = TemporaryApprover(
                id = "", // Will be set by Firestore
                projectId = projectId,
                approverId = approverId,
                approverName = approverName,
                approverPhone = approverPhone,
                assignedDate = Timestamp.now(),
                startDate = startdate,
                expiringDate = expiringDate,
                isActive = true,
                assignedBy = assignedBy,
                assignedByName = assignedByName,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            
            val docRef = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .add(tempApprover)
                .await()
            
            val savedTempApprover = tempApprover.copy(id = docRef.id)
            
            // Also update the project document with the temporary approver phone number
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .update("temporaryApproverPhone", approverPhone)
                .await()
            
            Log.d(TAG, "✅ Temporary approver added successfully with ID: ${docRef.id}")
            Log.d(TAG, "✅ Project document updated with temporary approver phone: $approverPhone")
            
            // Send notification to the temporary approver
            notificationService.get().let { service ->
                sendTemporaryApproverNotification(projectId, approverId, approverName, approverPhone, expiringDate, service)
            }
            
            Result.success(savedTempApprover)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to add temporary approver", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all temporary approvers for a project
     */
    suspend fun getTemporaryApprovers(projectId: String): Result<List<TemporaryApprover>> {
        return try {
            Log.d(TAG, "🔍 Getting temporary approvers for project: $projectId")
            
            val snapshot = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .get()
                .await()
            
            val tempApprovers = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    TemporaryApprover(
                        id = doc.id,
                        projectId = data["projectId"] as? String ?: "",
                        approverId = data["approverId"] as? String ?: "",
                        approverName = data["approverName"] as? String ?: "",
                        approverPhone = data["approverPhone"] as? String ?: "",
                        assignedDate = data["assignedDate"] as? Timestamp ?: Timestamp.now(),
                        expiringDate = data["expiringDate"] as? Timestamp,
                        isActive = data["isActive"] as? Boolean ?: true,
                        assignedBy = data["assignedBy"] as? String ?: "",
                        assignedByName = data["assignedByName"] as? String ?: "",
                        createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                        updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing temporary approver document: ${doc.id}", e)
                    null
                }
            }
            
            Log.d(TAG, "✅ Found ${tempApprovers.size} temporary approvers")
            Result.success(tempApprovers)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get temporary approvers", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get active temporary approvers for a project (only accepted ones, not expired and isActive = true)
     */
    suspend fun getActiveTemporaryApprovers(projectId: String): Result<List<TemporaryApprover>> {
        return try {
            val result = getTemporaryApprovers(projectId)
            if (result.isSuccess) {
                val allTempApprovers = result.getOrNull() ?: emptyList()
                val activeTempApprovers = allTempApprovers.filter { tempApprover ->
                    tempApprover.isActive && 
                    !tempApprover.isExpired() && 
                    tempApprover.status == "ACCEPTED" // Only include accepted assignments
                }
                Log.d(TAG, "✅ Found ${activeTempApprovers.size} active accepted temporary approvers")
                Result.success(activeTempApprovers)
            } else {
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get active temporary approvers", e)
            Result.failure(e)
        }
    }
    
    /**
     * Deactivate a temporary approver
     */
    suspend fun deactivateTemporaryApprover(projectId: String, tempApproverId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Deactivating temporary approver: $tempApproverId")
            
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .document(tempApproverId)
                .update(
                    mapOf(
                        "isActive" to false,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            
            // Clear the temporary approver phone from project document when deactivated
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .update("temporaryApproverPhone", "")
                .await()
            
            Log.d(TAG, "✅ Temporary approver deactivated successfully")
            Log.d(TAG, "✅ Project document cleared of temporary approver phone")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to deactivate temporary approver", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update start date of a temporary approver
     */
    suspend fun updateStartDate(
        projectId: String, 
        tempApproverId: String, 
        newStartDate: Timestamp
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Updating start date for temporary approver: $tempApproverId")
            
            // Get the current approver data before updating
            val currentDoc = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .document(tempApproverId)
                .get()
                .await()
            
            if (!currentDoc.exists()) {
                Log.e(TAG, "❌ Temporary approver not found: $tempApproverId")
                return Result.failure(Exception("Temporary approver not found"))
            }
            
            val currentData = currentDoc.data ?: return Result.failure(Exception("No data found"))
            val originalApprover = TemporaryApprover(
                id = currentDoc.id,
                projectId = currentData["projectId"] as? String ?: "",
                approverId = currentData["approverId"] as? String ?: "",
                approverName = currentData["approverName"] as? String ?: "",
                approverPhone = currentData["approverPhone"] as? String ?: "",
                assignedDate = currentData["assignedDate"] as? Timestamp ?: Timestamp.now(),
                startDate = currentData["startdate"] as? Timestamp ?: Timestamp.now(),
                expiringDate = currentData["expiringDate"] as? Timestamp,
                isActive = currentData["isActive"] as? Boolean ?: true,
                assignedBy = currentData["assignedBy"] as? String ?: "",
                assignedByName = currentData["assignedByName"] as? String ?: "",
                createdAt = currentData["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = currentData["updatedAt"] as? Timestamp ?: Timestamp.now(),
                isAccepted = currentData["isAccepted"] as? Boolean,
                acceptedAt = currentData["acceptedAt"] as? Timestamp,
                rejectedAt = currentData["rejectedAt"] as? Timestamp,
                responseMessage = currentData["responseMessage"] as? String ?: "",
                status = currentData["status"] as? String ?: "PENDING"
            )
            
            // Update the start date
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .document(tempApproverId)
                .update(
                    mapOf(
                        "startdate" to newStartDate,  // Note: using lowercase "startdate" as per model
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            
            // Create updated approver for notification
            val updatedApprover = originalApprover.copy(
                startDate = newStartDate,
                updatedAt = Timestamp.now()
            )
            
            // Send notification about start date change
            sendDelegationChangeNotification(projectId, updatedApprover, originalApprover, "System")
            
            Log.d(TAG, "✅ Start date updated successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update start date", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update expiring date of a temporary approver
     */
    suspend fun updateExpiringDate(
        projectId: String, 
        tempApproverId: String, 
        newExpiringDate: Timestamp
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Updating expiring date for temporary approver: $tempApproverId")
            
            // Get the current approver data before updating
            val currentDoc = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .document(tempApproverId)
                .get()
                .await()
            
            if (!currentDoc.exists()) {
                Log.e(TAG, "❌ Temporary approver not found: $tempApproverId")
                return Result.failure(Exception("Temporary approver not found"))
            }
            
            val currentData = currentDoc.data ?: return Result.failure(Exception("No data found"))
            val originalApprover = TemporaryApprover(
                id = currentDoc.id,
                projectId = currentData["projectId"] as? String ?: "",
                approverId = currentData["approverId"] as? String ?: "",
                approverName = currentData["approverName"] as? String ?: "",
                approverPhone = currentData["approverPhone"] as? String ?: "",
                assignedDate = currentData["assignedDate"] as? Timestamp ?: Timestamp.now(),
                startDate = currentData["startdate"] as? Timestamp ?: Timestamp.now(),
                expiringDate = currentData["expiringDate"] as? Timestamp,
                isActive = currentData["isActive"] as? Boolean ?: true,
                assignedBy = currentData["assignedBy"] as? String ?: "",
                assignedByName = currentData["assignedByName"] as? String ?: "",
                createdAt = currentData["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = currentData["updatedAt"] as? Timestamp ?: Timestamp.now(),
                isAccepted = currentData["isAccepted"] as? Boolean,
                acceptedAt = currentData["acceptedAt"] as? Timestamp,
                rejectedAt = currentData["rejectedAt"] as? Timestamp,
                responseMessage = currentData["responseMessage"] as? String ?: "",
                status = currentData["status"] as? String ?: "PENDING"
            )
            
            // Update the expiring date
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .document(tempApproverId)
                .update(
                    mapOf(
                        "expiringDate" to newExpiringDate,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            
            // Create updated approver for notification
            val updatedApprover = originalApprover.copy(
                expiringDate = newExpiringDate,
                updatedAt = Timestamp.now()
            )
            
            // Send notification about end date change
            sendDelegationChangeNotification(projectId, updatedApprover, originalApprover, "System")
            
            Log.d(TAG, "✅ Expiring date updated successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update expiring date", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a temporary approver
     */
    suspend fun deleteTemporaryApprover(projectId: String, tempApproverId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Deleting temporary approver: $tempApproverId")
            
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .document(tempApproverId)
                .delete()
                .await()
            
            // Clear the temporary approver phone from project document when deleted
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .update("temporaryApproverPhone", "")
                .await()
            
            Log.d(TAG, "✅ Temporary approver deleted successfully")
            Log.d(TAG, "✅ Project document cleared of temporary approver phone")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete temporary approver", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check and deactivate expired temporary approvers for a project
     */
    suspend fun deactivateExpiredApprovers(projectId: String): Result<Int> {
        return try {
            Log.d(TAG, "🔄 Checking for expired temporary approvers in project: $projectId")
            
            val result = getTemporaryApprovers(projectId)
            if (result.isFailure) {
                return result.map { 0 }
            }
            
            val allTempApprovers = result.getOrNull() ?: emptyList()
            val expiredApprovers = allTempApprovers.filter { tempApprover ->
                tempApprover.isActive && tempApprover.isExpired()
            }
            
            var deactivatedCount = 0
            for (expiredApprover in expiredApprovers) {
                val deactivateResult = deactivateTemporaryApprover(projectId, expiredApprover.id)
                if (deactivateResult.isSuccess) {
                    deactivatedCount++
                    Log.d(TAG, "✅ Deactivated expired temporary approver: ${expiredApprover.approverName}")
                }
            }
            
            Log.d(TAG, "✅ Deactivated $deactivatedCount expired temporary approvers")
            Result.success(deactivatedCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to deactivate expired approvers", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if a user is a temporary approver for a project
     */
    suspend fun isTemporaryApprover(projectId: String, userId: String): Result<Boolean> {
        return try {
            val result = getActiveTemporaryApprovers(projectId)
            if (result.isSuccess) {
                val activeTempApprovers = result.getOrNull() ?: emptyList()
                val isTemporaryApprover = activeTempApprovers.any { it.approverId == userId }
                Result.success(isTemporaryApprover)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to check temporary approver status", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if a user is an accepted temporary approver for a project by phone number
     */
    suspend fun isAcceptedTemporaryApproverByPhone(projectId: String, userPhone: String): Result<Boolean> {
        return try {
            Log.d(TAG, "🔄 Checking if user with phone $userPhone is accepted temporary approver for project $projectId")
            
            val querySnapshot = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .whereEqualTo("approverPhone", userPhone)
                .whereEqualTo("isActive", true)
                .whereEqualTo("status", "ACCEPTED")
                .limit(1)
                .get()
                .await()
            
            val isAccepted = !querySnapshot.isEmpty
            Log.d(TAG, "✅ User $userPhone is accepted temporary approver: $isAccepted")
            Result.success(isAccepted)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to check accepted temporary approver status", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all accepted temporary approver project IDs for a user by phone number
     */
    suspend fun getAcceptedTemporaryApproverProjectIds(userPhone: String): Result<List<String>> {
        return try {
            Log.d(TAG, "🔄 Getting accepted temporary approver project IDs for user $userPhone")
            
            // Query all temporary approver assignments for this user
            val querySnapshot = firestore.collectionGroup(SUBCOLLECTION_TEMP_APPROVERS)
                .whereEqualTo("approverPhone", userPhone)
                .whereEqualTo("isActive", true)
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .await()
            
            val projectIds = querySnapshot.documents.mapNotNull { doc ->
                doc.getString("projectId")
            }.distinct()
            
            Log.d(TAG, "✅ Found ${projectIds.size} accepted temporary approver projects for user $userPhone")
            Result.success(projectIds)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get accepted temporary approver project IDs", e)
            Result.failure(e)
        }
    }
    
    /**
     * Accept a temporary approver assignment - only accepted assignments remain active
     */
    suspend fun acceptTemporaryApproverAssignment(
        projectId: String,
        approverId: String,
        responseMessage: String = ""
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Accepting temporary approver assignment for project $projectId by approver $approverId")
            Log.d(TAG, "🔍 Debug: approverId type: ${approverId::class.simpleName}, length: ${approverId.length}")
            
            // Find the temporary approver record by approverId
            var tempApproversQuery = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .whereEqualTo("approverId", approverId)
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()
            
            // If not found by approverId, try to find by phone number (in case approverId is actually a phone)
            if (tempApproversQuery.isEmpty) {
                Log.d(TAG, "🔄 Not found by approverId, trying to find by phone number: $approverId")
                tempApproversQuery = firestore.collection(COLLECTION_PROJECTS)
                    .document(projectId)
                    .collection(SUBCOLLECTION_TEMP_APPROVERS)
                    .whereEqualTo("approverPhone", approverId)
                    .whereEqualTo("isActive", true)
                    .limit(1)
                    .get()
                    .await()
            }
            
            if (tempApproversQuery.isEmpty) {
                Log.e(TAG, "❌ No active temporary approver assignment found for approver $approverId in project $projectId")
                return Result.failure(Exception("No active temporary approver assignment found"))
            }
            
            val tempApproverDoc = tempApproversQuery.documents.first()
            val tempApproverId = tempApproverDoc.id
            val docData = tempApproverDoc.data
            Log.d(TAG, "🔍 Found temporary approver record:")
            Log.d(TAG, "   - Document ID: $tempApproverId")
            Log.d(TAG, "   - approverId: ${docData?.get("approverId")}")
            Log.d(TAG, "   - approverPhone: ${docData?.get("approverPhone")}")
            Log.d(TAG, "   - status: ${docData?.get("status")}")
            Log.d(TAG, "   - isActive: ${docData?.get("isActive")}")
            
            // Update the temporary approver record to mark as accepted
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .document(tempApproverId)
                .update(
                    mapOf(
                        "isAccepted" to true,
                        "acceptedAt" to Timestamp.now(),
                        "responseMessage" to responseMessage,
                        "updatedAt" to Timestamp.now(),
                        "status" to "ACCEPTED"
                    )
                )
                .await()
            
            Log.d(TAG, "✅ Temporary approver assignment accepted successfully - status updated to ACCEPTED")
            
            // Clean up any other pending temporary approver assignments for this project
            cleanupPendingAssignments(projectId)
            
            // Send notification to production head about acceptance
            notificationService.get().let { service ->
                sendAssignmentResponseNotification(projectId, approverId, true, responseMessage, service)
            }
            
            // Send confirmation notification to the approver about their acceptance
            sendDelegationResponseConfirmation(projectId, approverId, true, responseMessage)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to accept temporary approver assignment", e)
            Result.failure(e)
        }
    }
    
    /**
     * Clean up pending temporary approver assignments that haven't been responded to
     * This ensures only accepted assignments remain active
     */
    private suspend fun cleanupPendingAssignments(projectId: String) {
        try {
            Log.d(TAG, "🔄 Cleaning up pending temporary approver assignments for project $projectId")
            
            // Find all temporary approvers for this project that are still pending (isAccepted is null)
            val pendingQuery = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            val batch = firestore.batch()
            var hasUpdates = false
            
            for (doc in pendingQuery.documents) {
                val status = doc.get("status") as? String
                if (status == "PENDING" || status == null) {
                    // This is a pending assignment, remove it
                    batch.delete(doc.reference)
                    hasUpdates = true
                    Log.d(TAG, "🗑️ Removing pending assignment: ${doc.id}")
                }
            }
            
            if (hasUpdates) {
                batch.commit().await()
                Log.d(TAG, "✅ Cleaned up pending temporary approver assignments")
            } else {
                Log.d(TAG, "ℹ️ No pending assignments to clean up")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to cleanup pending assignments", e)
            // Don't fail the main operation if cleanup fails
        }
    }
    
    /**
     * Clean up expired temporary approver assignments
     * This removes assignments that have passed their expiry date
     */
    suspend fun cleanupExpiredAssignments(projectId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Cleaning up expired temporary approver assignments for project $projectId")
            
            val querySnapshot = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            val batch = firestore.batch()
            var hasUpdates = false
            
            for (doc in querySnapshot.documents) {
                try {
                    val tempApprover = doc.toObject(TemporaryApprover::class.java)
                    if (tempApprover != null && tempApprover.isExpired()) {
                        // This assignment has expired, remove it
                        batch.delete(doc.reference)
                        hasUpdates = true
                        Log.d(TAG, "🗑️ Removing expired assignment: ${doc.id}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing document ${doc.id}", e)
                }
            }
            
            if (hasUpdates) {
                batch.commit().await()
                Log.d(TAG, "✅ Cleaned up expired temporary approver assignments")
            } else {
                Log.d(TAG, "ℹ️ No expired assignments to clean up")
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to cleanup expired assignments", e)
            Result.failure(e)
        }
    }
    
    /**
     * Reject a temporary approver assignment - completely removes them from the project
     */
    suspend fun rejectTemporaryApproverAssignment(
        projectId: String,
        approverId: String,
        responseMessage: String = ""
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Rejecting temporary approver assignment for project $projectId by approver $approverId")
            Log.d(TAG, "🔍 Debug: approverId type: ${approverId::class.simpleName}, length: ${approverId.length}")
            
            // Find the temporary approver record by approverId
            var tempApproversQuery = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .whereEqualTo("approverId", approverId)
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()
            
            // If not found by approverId, try to find by phone number (in case approverId is actually a phone)
            if (tempApproversQuery.isEmpty) {
                Log.d(TAG, "🔄 Not found by approverId, trying to find by phone number: $approverId")
                tempApproversQuery = firestore.collection(COLLECTION_PROJECTS)
                    .document(projectId)
                    .collection(SUBCOLLECTION_TEMP_APPROVERS)
                    .whereEqualTo("approverPhone", approverId)
                    .whereEqualTo("isActive", true)
                    .limit(1)
                    .get()
                    .await()
            }
            
            if (tempApproversQuery.isEmpty) {
                Log.e(TAG, "❌ No active temporary approver assignment found for approver $approverId in project $projectId")
                return Result.failure(Exception("No active temporary approver assignment found"))
            }
            
            val tempApproverDoc = tempApproversQuery.documents.first()
            val tempApproverId = tempApproverDoc.id
            val docData = tempApproverDoc.data
            Log.d(TAG, "🔍 Found temporary approver record:")
            Log.d(TAG, "   - Document ID: $tempApproverId")
            Log.d(TAG, "   - approverId: ${docData?.get("approverId")}")
            Log.d(TAG, "   - approverPhone: ${docData?.get("approverPhone")}")
            Log.d(TAG, "   - status: ${docData?.get("status")}")
            Log.d(TAG, "   - isActive: ${docData?.get("isActive")}")
            
            // Mark as rejected and deactivate instead of deleting (for audit trail)
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .document(tempApproverId)
                .update(
                    mapOf(
                        "isAccepted" to false,
                        "rejectedAt" to Timestamp.now(),
                        "responseMessage" to responseMessage,
                        "updatedAt" to Timestamp.now(),
                        "status" to "REJECTED",
                        "isActive" to false
                    )
                )
                .await()
            
            Log.d(TAG, "✅ Temporary approver assignment rejected successfully - status updated to REJECTED")
            
            // Also remove the temporary approver phone from the project document
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .update("temporaryApproverPhone", com.google.firebase.firestore.FieldValue.delete())
                .await()
            
            Log.d(TAG, "✅ Temporary approver assignment rejected and completely removed from project")
            
            // Send notification to production head about rejection
            notificationService.get().let { service ->
                sendAssignmentResponseNotification(projectId, approverId, false, responseMessage, service)
            }
            
            // Send confirmation notification to the approver about their rejection
            sendDelegationResponseConfirmation(projectId, approverId, false, responseMessage)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to reject temporary approver assignment", e)
            Result.failure(e)
        }
    }
    
    /**
     * Send notification to production head about assignment response
     */
    private suspend fun sendAssignmentResponseNotification(
        projectId: String,
        approverId: String,
        isAccepted: Boolean,
        responseMessage: String,
        service: NotificationService
    ) {
        try {
            Log.d(TAG, "🔄 Sending assignment response notification for project $projectId")
            
            // Get project details
            val projectDoc = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .get()
                .await()
            
            if (!projectDoc.exists()) {
                Log.e(TAG, "❌ Project not found for notification: $projectId")
                return
            }
            
            val projectData = projectDoc.data ?: return
            val projectName = projectData["name"] as? String ?: "Unknown Project"
            val BusinessHeadId = projectData["BusinessHeadId"] as? String ?: return
            val BusinessHeadPhone = projectData["BusinessHeadPhone"] as? String ?: return
            
            // Get approver details
            val approverDoc = firestore.collection("users")
                .whereEqualTo("uid", approverId)
                .limit(1)
                .get()
                .await()
            
            val approverName = if (!approverDoc.isEmpty) {
                approverDoc.documents.first().getString("name") ?: "Unknown Approver"
            } else {
                "Unknown Approver"
            }
            
            val action = if (isAccepted) "accepted" else "rejected"
            val title = "Temporary Approver Assignment $action"
            val message = "$approverName has $action the temporary approver assignment for project '$projectName'."
            
            val notification = Notification(
                recipientId = BusinessHeadPhone,
                recipientRole = "BUSINESS_HEAD",
                title = title,
                message = message,
                type = NotificationType.TEMPORARY_APPROVER_ASSIGNMENT,
                projectId = projectId,
                projectName = projectName,
                actionRequired = false,
                navigationTarget = "project_dashboard/$projectId"
            )
            
            // Send the notification
            val notificationResult = service.sendNotification(notification)
            
            if (notificationResult.isSuccess) {
                Log.d(TAG, "✅ Assignment response notification sent successfully")
            } else {
                Log.e(TAG, "❌ Failed to send assignment response notification: ${notificationResult.exceptionOrNull()?.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send assignment response notification", e)
        }
    }

    /**
     * Send notification to temporary approver about their assignment
     */
    private suspend fun sendTemporaryApproverNotification(
        projectId: String,
        approverId: String,
        approverName: String,
        approverPhone: String,
        expiringDate: Timestamp,
        service: NotificationService
    ) {
        try {
            Log.d(TAG, "🔄 Sending temporary approver notification to: $approverName ($approverPhone)")
            
            // Get project details for the notification
            val projectDoc = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .get()
                .await()
            
            if (!projectDoc.exists()) {
                Log.e(TAG, "❌ Project not found for notification: $projectId")
                return
            }
            
            val projectData = projectDoc.data ?: return
            val projectName = projectData["name"] as? String ?: "Unknown Project"
            
            // Format the expiry date
            val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy 'at' HH:mm", java.util.Locale.getDefault())
            val formattedExpiryDate = dateFormat.format(expiringDate.toDate())
            
            // Create notification - Use phone number as recipientId to match notification filtering
            val notification = Notification(
                recipientId = approverPhone, // Use phone number instead of UID
                recipientRole = "APPROVER",
                title = "Temporary Approver Assignment",
                message = "You have been assigned as a temporary approver to project '$projectName' until $formattedExpiryDate. You can now access and manage this project.",
                type = NotificationType.TEMPORARY_APPROVER_ASSIGNMENT,
                projectId = projectId,
                projectName = projectName,
                relatedId = approverId, // Store the actual approver ID for reference
                actionRequired = true,
                navigationTarget = "approver_project_dashboard/$projectId"
            )
            
            Log.d(TAG, "📋 Created notification:")
            Log.d(TAG, "   - recipientId: $approverPhone (phone number)")
            Log.d(TAG, "   - recipientRole: APPROVER")
            Log.d(TAG, "   - title: ${notification.title}")
            Log.d(TAG, "   - message: ${notification.message}")
            Log.d(TAG, "   - projectId: $projectId")
            Log.d(TAG, "   - projectName: $projectName")
            
            // Send the notification
            val notificationResult = service.sendNotification(notification)
            
            if (notificationResult.isSuccess) {
                Log.d(TAG, "✅ Temporary approver notification sent successfully to: $approverName")
            } else {
                Log.e(TAG, "❌ Failed to send notification: ${notificationResult.exceptionOrNull()?.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send temporary approver notification", e)
            // Don't fail the main operation if notification fails
        }
    }
    
    /**
     * Create a new temporary approver assignment
     */
    suspend fun createTemporaryApprover(
        projectId: String,
        approverId: String,
        approverName: String,
        approverPhone: String,
        startDate: Timestamp,
        expiringDate: Timestamp?
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Creating temporary approver assignment for project $projectId")
            
            // Get the production head details who is making this assignment
            val projectDoc = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .get()
                .await()
            
            val BusinessHeadPhone = projectDoc.getString("BusinessHeadPhone") ?: ""
            val BusinessHeadName = projectDoc.getString("BusinessHeadName") ?: "Production Head"
            
            val tempApprover = TemporaryApprover(
                id = "", // Will be auto-generated by Firestore
                projectId = projectId,
                approverId = approverId,
                approverName = approverName,
                approverPhone = approverPhone,
                startDate = startDate,
                expiringDate = expiringDate,
                isActive = true,
                assignedBy = BusinessHeadPhone,
                assignedByName = BusinessHeadName,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
                status = "ACCEPTED", // Changed from PENDING to ACCEPTED - no manual acceptance required
                isAccepted = true,
                acceptedAt = Timestamp.now(),
                responseMessage = "Auto-accepted"
            )
            
            // Add to Firestore
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .add(tempApprover)
                .await()
            
            // Update project document with temporary approver phone
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .update("temporaryApproverPhone", approverPhone)
                .await()
            
            Log.d(TAG, "✅ Temporary approver assignment created successfully (auto-accepted)")
            
            // Send informational notification to the temporary approver
            notificationService.get().sendTemporaryApproverAssignmentNotification(
                projectId = projectId,
                approverId = approverId,
                approverName = approverName,
                approverPhone = approverPhone,
                assignedByName = BusinessHeadName,
                startDate = startDate,
                expiringDate = expiringDate
            )?.also { result ->
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Notification sent to temporary approver")
                } else {
                    Log.e(TAG, "❌ Failed to send notification: ${result.exceptionOrNull()?.message}")
                }
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create temporary approver assignment", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing temporary approver assignment
     */
    suspend fun updateTemporaryApprover(
        projectId: String,
        updatedApprover: TemporaryApprover,
        originalApprover: TemporaryApprover? = null,
        changedBy: String = "System"
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Updating temporary approver assignment: ${updatedApprover.id}")
            
            // If we have the document ID, use it directly
            if (updatedApprover.id.isNotEmpty()) {
                val updatedData = updatedApprover.copy(updatedAt = Timestamp.now())
                
                firestore.collection(COLLECTION_PROJECTS)
                    .document(projectId)
                    .collection(SUBCOLLECTION_TEMP_APPROVERS)
                    .document(updatedApprover.id)
                    .set(updatedData)
                    .await()
                
                Log.d(TAG, "✅ Temporary approver assignment updated successfully using document ID")
                
                // Send notification about delegation changes
                sendDelegationChangeNotification(projectId, updatedApprover, originalApprover, changedBy)
                
                return Result.success(Unit)
            }
            
            // Fallback: Find the document by approverId
            val query = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .whereEqualTo("approverId", updatedApprover.approverId)
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()
            
            if (query.isEmpty) {
                Log.e(TAG, "❌ No active temporary approver found with ID: ${updatedApprover.approverId}")
                return Result.failure(Exception("Temporary approver not found"))
            }
            
            val doc = query.documents.first()
            val updatedData = updatedApprover.copy(updatedAt = Timestamp.now())
            
            // Update the document
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .document(doc.id)
                .set(updatedData)
                .await()
            
            Log.d(TAG, "✅ Temporary approver assignment updated successfully")
            
            // Send notification about delegation changes
            sendDelegationChangeNotification(projectId, updatedApprover, originalApprover, changedBy)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update temporary approver assignment", e)
            Result.failure(e)
        }
    }
    
    /**
     * Remove a temporary approver assignment by document ID
     */
    suspend fun removeTemporaryApproverById(
        projectId: String,
        documentId: String
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Removing temporary approver assignment by document ID: $documentId")
            
            // Get the document data first to get the approver phone
            val docRef = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .document(documentId)
            
            val docSnapshot = docRef.get().await()
            if (!docSnapshot.exists()) {
                Log.e(TAG, "❌ Temporary approver document not found: $documentId")
                return Result.failure(Exception("Temporary approver not found"))
            }
            
            val docData = docSnapshot.data
            val approverPhone = docData?.get("approverPhone") as? String ?: ""
            val approverId = docData?.get("approverId") as? String ?: ""
            val approverName = docData?.get("approverName") as? String ?: ""
            
            // Send notification to approver about delegation removal before deleting
            sendDelegationRemovalNotification(projectId, approverId, approverName, approverPhone)
            
            // Completely delete the temporary approver document
            docRef.delete().await()
            
            // Remove temporary approver phone from project document
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .update("temporaryApproverPhone", com.google.firebase.firestore.FieldValue.delete())
                .await()
            
            Log.d(TAG, "✅ Temporary approver assignment completely removed from project")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to remove temporary approver assignment", e)
            Result.failure(e)
        }
    }
    
    /**
     * Remove a temporary approver assignment by approver ID (fallback method)
     */
    suspend fun removeTemporaryApprover(
        projectId: String,
        approverId: String
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Removing temporary approver assignment: $approverId")
            
            // Find the document by approverId
            val query = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .whereEqualTo("approverId", approverId)
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()
            
            if (query.isEmpty) {
                Log.e(TAG, "❌ No active temporary approver found with ID: $approverId")
                return Result.failure(Exception("Temporary approver not found"))
            }
            
            val doc = query.documents.first()
            val docData = doc.data
            val approverPhone = docData?.get("approverPhone") as? String ?: ""
            
            // Completely delete the temporary approver document
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .document(doc.id)
                .delete()
                .await()
            
            // Remove temporary approver phone from project document
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .update("temporaryApproverPhone", com.google.firebase.firestore.FieldValue.delete())
                .await()
            
            Log.d(TAG, "✅ Temporary approver assignment completely removed from project")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to remove temporary approver assignment", e)
            Result.failure(e)
        }
    }
    
    /**
     * Send notification about delegation changes to the approver
     */
    private suspend fun sendDelegationChangeNotification(
        projectId: String,
        updatedApprover: TemporaryApprover,
        originalApprover: TemporaryApprover?,
        changedBy: String
    ) {
        try {
            // Get project details for the notification
            val projectDoc = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .get()
                .await()
            
            if (!projectDoc.exists()) {
                Log.e(TAG, "❌ Project not found for notification: $projectId")
                return
            }
            
            val projectData = projectDoc.data ?: return
            val projectName = projectData["name"] as? String ?: "Unknown Project"
            
            // Determine what changed
            val changeDescription = buildChangeDescription(updatedApprover, originalApprover)
            
            // Send notification using the notification service
            notificationService.get().sendDelegationChangeNotification(
                approverId = updatedApprover.approverId,
                approverPhone = updatedApprover.approverPhone,
                projectId = projectId,
                projectName = projectName,
                changeDescription = changeDescription,
                changedBy = changedBy
            )
            
            Log.d(TAG, "📧 Delegation change notification sent to: ${updatedApprover.approverName}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send delegation change notification", e)
        }
    }
    
    /**
     * Send notification about delegation removal to the approver
     */
    private suspend fun sendDelegationRemovalNotification(
        projectId: String,
        approverId: String,
        approverName: String,
        approverPhone: String
    ) {
        try {
            // Get project details for the notification
            val projectDoc = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .get()
                .await()
            
            if (!projectDoc.exists()) {
                Log.e(TAG, "❌ Project not found for removal notification: $projectId")
                return
            }
            
            val projectData = projectDoc.data ?: return
            val projectName = projectData["name"] as? String ?: "Unknown Project"
            
            // Send notification using the notification service
            notificationService.get().sendDelegationRemovalNotification(
                approverId = approverId,
                approverPhone = approverPhone,
                projectId = projectId,
                projectName = projectName
            )
            
            Log.d(TAG, "📧 Delegation removal notification sent to: $approverName")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send delegation removal notification", e)
        }
    }
    
    /**
     * Send confirmation notification to approver about their response (accept/reject)
     */
    private suspend fun sendDelegationResponseConfirmation(
        projectId: String,
        approverId: String,
        isAccepted: Boolean,
        responseMessage: String
    ) {
        try {
            // Get project details for the notification
            val projectDoc = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .get()
                .await()
            
            if (!projectDoc.exists()) {
                Log.e(TAG, "❌ Project not found for response confirmation: $projectId")
                return
            }
            
            val projectData = projectDoc.data ?: return
            val projectName = projectData["name"] as? String ?: "Unknown Project"
            
            // Get approver details
            val approverDoc = firestore.collection("users")
                .whereEqualTo("uid", approverId)
                .limit(1)
                .get()
                .await()
            
            if (approverDoc.isEmpty) {
                Log.e(TAG, "❌ Approver not found for response confirmation: $approverId")
                return
            }
            
            val approverData = approverDoc.documents.first().data
            val approverPhone = approverData?.get("phone") as? String ?: ""
            val approverName = approverData?.get("name") as? String ?: "Approver"
            
            // Send notification using the notification service
            notificationService.get().sendDelegationResponseConfirmation(
                approverId = approverId,
                approverPhone = approverPhone,
                projectId = projectId,
                projectName = projectName,
                isAccepted = isAccepted,
                responseMessage = responseMessage
            )
            
            Log.d(TAG, "📧 Delegation response confirmation sent to: $approverName")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send delegation response confirmation", e)
        }
    }
    
    /**
     * Build a description of what changed in the delegation
     */
    private fun buildChangeDescription(
        updatedApprover: TemporaryApprover,
        originalApprover: TemporaryApprover?
    ): String {
        if (originalApprover == null) {
            return "Delegation settings have been updated."
        }
        
        val changes = mutableListOf<String>()
        
        // Check if start date changed
        if (originalApprover.startDate != updatedApprover.startDate) {
            val originalDate = originalApprover.startDate.toDate()
            val updatedDate = updatedApprover.startDate.toDate()
            val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            changes.add("Start date changed from ${dateFormat.format(originalDate)} to ${dateFormat.format(updatedDate)}")
        }
        
        // Check if end date changed
        val originalEndDate = originalApprover.expiringDate?.toDate()
        val updatedEndDate = updatedApprover.expiringDate?.toDate()
        
        when {
            originalEndDate == null && updatedEndDate != null -> {
                val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                changes.add("End date set to ${dateFormat.format(updatedEndDate)}")
            }
            originalEndDate != null && updatedEndDate == null -> {
                changes.add("End date removed (delegation now has no expiry)")
            }
            originalEndDate != null && updatedEndDate != null && originalEndDate != updatedEndDate -> {
                val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                changes.add("End date changed from ${dateFormat.format(originalEndDate)} to ${dateFormat.format(updatedEndDate)}")
            }
        }
        
        // Check if approver changed
        if (originalApprover.approverId != updatedApprover.approverId) {
            changes.add("Approver changed from ${originalApprover.approverName} to ${updatedApprover.approverName}")
        }
        
        return if (changes.isEmpty()) {
            "Delegation settings have been updated."
        } else {
            changes.joinToString(". ")
        }
    }
    
    /**
     * Get all temporary approvers for a specific project
     */
    suspend fun getTemporaryApproversForProject(projectId: String): List<TemporaryApprover> {
        return try {
            Log.d(TAG, "🔄 Getting temporary approvers for project: $projectId")
            
            val query = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .get()
                .await()
            
            val approvers = query.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    
                    TemporaryApprover(
                        id = doc.id,
                        projectId = data["projectId"] as? String ?: "",
                        approverId = data["approverId"] as? String ?: "",
                        approverName = data["approverName"] as? String ?: "",
                        approverPhone = data["approverPhone"] as? String ?: "",
                        assignedDate = data["assignedDate"] as? Timestamp ?: Timestamp.now(),
                        startDate = data["startdate"] as? Timestamp ?: Timestamp.now(),
                        expiringDate = data["expiringDate"] as? Timestamp,
                        isActive = data["isActive"] as? Boolean ?: true,
                        assignedBy = data["assignedBy"] as? String ?: "",
                        assignedByName = data["assignedByName"] as? String ?: "",
                        createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                        updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now(),
                        isAccepted = data["isAccepted"] as? Boolean,
                        acceptedAt = data["acceptedAt"] as? Timestamp,
                        rejectedAt = data["rejectedAt"] as? Timestamp,
                        responseMessage = data["responseMessage"] as? String ?: "",
                        status = data["status"] as? String ?: "PENDING"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing temporary approver document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            Log.d(TAG, "✅ Found ${approvers.size} temporary approvers for project: $projectId")
            approvers
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting temporary approvers for project: ${e.message}", e)
            emptyList()
        }
    }
}


