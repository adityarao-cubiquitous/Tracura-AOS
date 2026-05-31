package com.cubiquitous.tracura.service

import android.util.Log
import com.cubiquitous.tracura.model.TemporaryApprover
import com.cubiquitous.tracura.model.isExpired
import com.cubiquitous.tracura.repository.ProjectRepository
import com.cubiquitous.tracura.repository.TemporaryApproverRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DelegationExpiryService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val projectRepository: ProjectRepository,
    private val temporaryApproverRepository: TemporaryApproverRepository,
    private val notificationService: NotificationService
) {
    
    companion object {
        private const val TAG = "DelegationExpiryService"
    }
    
    /**
     * Check and process all expired delegations across all projects
     */
    suspend fun processExpiredDelegations() {
        try {
            Log.d(TAG, "🔄 Starting delegation expiry check...")
            
            // Get all projects
            val allProjects = projectRepository.getAllProjects()
            Log.d(TAG, "📊 Found ${allProjects.size} projects to check")
            
            for (project in allProjects) {
                project.id?.let { processExpiredDelegationsForProject(it) }
            }
            
            Log.d(TAG, "✅ Completed delegation expiry check for all projects")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing expired delegations: ${e.message}", e)
        }
    }
    
    /**
     * Check and process expired delegations for a specific project
     */
    suspend fun processExpiredDelegationsForProject(projectId: String) {
        try {
            Log.d(TAG, "🔍 Checking expired delegations for project: $projectId")
            
            // Get all temporary approvers for this project
            val tempApprovers = temporaryApproverRepository.getTemporaryApproversForProject(projectId)
            
            if (tempApprovers.isEmpty()) {
                Log.d(TAG, "📭 No temporary approvers found for project: $projectId")
                return
            }
            
            val now = Date()
            val expiredApprovers = mutableListOf<TemporaryApprover>()
            
            // Find expired approverss
            for (approver in tempApprovers) {
                if (approver.isActive && approver.isExpired()) {
                    expiredApprovers.add(approver)
                    Log.d(TAG, "⏰ Found expired delegation: ${approver.approverName} (${approver.approverId}) for project: $projectId")
                }
            }
            
            if (expiredApprovers.isEmpty()) {
                Log.d(TAG, "✅ No expired delegations found for project: $projectId")
                return
            }
            
            // Process each expired delegation
            for (expiredApprover in expiredApprovers) {
                processExpiredDelegation(projectId, expiredApprover)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing expired delegations for project $projectId: ${e.message}", e)
        }
    }
    
    /**
     * Process a single expired delegation
     */
    private suspend fun processExpiredDelegation(projectId: String, expiredApprover: TemporaryApprover) {
        try {
            Log.d(TAG, "🔄 Processing expired delegation: ${expiredApprover.approverName} for project: $projectId")
            
            // 1. Remove the temporary approver from the project's teamMembers list
            removeTeamMemberFromProject(projectId, expiredApprover.approverId)
            
            // 2. Deactivate the temporary approver assignment
            deactivateTemporaryApprover(projectId, expiredApprover)
            
            // 3. Send notification to the user about delegation expiry
            sendDelegationExpiryNotification(projectId, expiredApprover)
            
            Log.d(TAG, "✅ Successfully processed expired delegation: ${expiredApprover.approverName}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing expired delegation: ${e.message}", e)
        }
    }
    
    /**
     * Remove team member from project's teamMembers list
     */
    private suspend fun removeTeamMemberFromProject(projectId: String, teamMemberId: String) {
        try {
            Log.d(TAG, "🔄 Removing team member $teamMemberId from project $projectId")
            
            // Get project from correct path: customers/{customerId}/Projects/{projectId}
            val customerId = projectRepository.findCustomerIdForProject(projectId)
            if (customerId == null) {
                Log.e(TAG, "❌ Could not find customer for project: $projectId")
                return
            }
            
            val projectRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
            val projectDoc = projectRef.get().await()
            
            if (!projectDoc.exists()) {
                Log.e(TAG, "❌ Project not found: $projectId under customer: $customerId")
                return
            }
            
            val projectData = projectDoc.data ?: return
            val currentTeamMembers = projectData["teamMembers"] as? List<String> ?: emptyList()
            
            // Remove the team member from the list
            val updatedTeamMembers = currentTeamMembers.filter { it != teamMemberId }
            
            if (updatedTeamMembers.size == currentTeamMembers.size) {
                Log.d(TAG, "ℹ️ Team member $teamMemberId was not in project $projectId team members list")
                return
            }
            
            // Update the project with the new team members list
            projectRef.update(
                mapOf(
                    "teamMembers" to updatedTeamMembers,
                    "updatedAt" to Timestamp.now()
                )
            ).await()
            
            Log.d(TAG, "✅ Successfully removed team member $teamMemberId from project $projectId")
            Log.d(TAG, "📊 Team members count: ${currentTeamMembers.size} -> ${updatedTeamMembers.size}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error removing team member from project: ${e.message}", e)
        }
    }
    
    /**
     * Deactivate the temporary approver assignment
     */
    private suspend fun deactivateTemporaryApprover(projectId: String, expiredApprover: TemporaryApprover) {
        try {
            Log.d(TAG, "🔄 Deactivating temporary approver assignment: ${expiredApprover.id}")
            
            // Update the temporary approver to mark as expired and inactive
            val updatedApprover = expiredApprover.copy(
                isActive = false,
                status = "EXPIRED",
                updatedAt = Timestamp.now()
            )
            
            temporaryApproverRepository.updateTemporaryApprover(
                projectId = projectId,
                updatedApprover = updatedApprover,
                originalApprover = expiredApprover,
                changedBy = "System"
            )
            
            // Also remove the temporary approver phone from the project document
            val customerId = projectRepository.findCustomerIdForProject(projectId)
            if (customerId != null) {
                firestore.collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(projectId)
                    .update("temporaryApproverPhone", com.google.firebase.firestore.FieldValue.delete())
                    .await()
            } else {
                Log.e(TAG, "❌ Could not find customer for project: $projectId")
            }
            
            Log.d(TAG, "✅ Successfully deactivated temporary approver assignment")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deactivating temporary approver: ${e.message}", e)
        }
    }
    
    /**
     * Send notification to user about delegation expiry
     */
    private suspend fun sendDelegationExpiryNotification(projectId: String, expiredApprover: TemporaryApprover) {
        try {
            Log.d(TAG, "📱 Sending delegation expiry notification to: ${expiredApprover.approverName}")
            
            // Get project details
            val project = projectRepository.getProjectById(projectId)
            if (project == null) {
                Log.e(TAG, "❌ Project not found for notification: $projectId")
                return
            }
            
            // Send notification
            notificationService.sendDelegationExpiryNotification(
                projectId = projectId,
                projectName = project.name,
                approverId = expiredApprover.approverId,
                approverName = expiredApprover.approverName,
                approverPhone = expiredApprover.approverPhone
            )
            
            Log.d(TAG, "✅ Delegation expiry notification sent successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending delegation expiry notification: ${e.message}", e)
        }
    }
    
    /**
     * Check if a specific delegation is expired
     */
    fun isDelegationExpired(approver: TemporaryApprover): Boolean {
        return approver.isActive && approver.isExpired()
    }
    
    /**
     * Get all expired delegations for a project
     */
    suspend fun getExpiredDelegationsForProject(projectId: String): List<TemporaryApprover> {
        return try {
            val tempApprovers = temporaryApproverRepository.getTemporaryApproversForProject(projectId)
            tempApprovers.filter { it.isActive && it.isExpired() }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting expired delegations: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Start background processing of expired delegations
     */
    fun startBackgroundProcessing() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🚀 Starting background delegation expiry processing")
                processExpiredDelegations()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in background processing: ${e.message}", e)
            }
        }
    }
}
