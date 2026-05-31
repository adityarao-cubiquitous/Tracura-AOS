package com.cubiquitous.tracura.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.TemporaryApprover
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.repository.TemporaryApproverRepository
import com.cubiquitous.tracura.repository.AuthRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TemporaryApproverViewModel @Inject constructor(
    private val temporaryApproverRepository: TemporaryApproverRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "TempApproverViewModel"
    }
    
    // State for temporary approvers list
    private val _temporaryApprovers = MutableStateFlow<List<TemporaryApprover>>(emptyList())
    val temporaryApprovers: StateFlow<List<TemporaryApprover>> = _temporaryApprovers.asStateFlow()
    
    // State for available approvers (users who can be assigned as temporary approvers)
    private val _availableApprovers = MutableStateFlow<List<User>>(emptyList())
    val availableApprovers: StateFlow<List<User>> = _availableApprovers.asStateFlow()
    
    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isAddingApprover = MutableStateFlow(false)
    val isAddingApprover: StateFlow<Boolean> = _isAddingApprover.asStateFlow()
    
    // Error states
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Success message
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    /**
     * Load temporary approvers for a project
     */
    fun loadTemporaryApprovers(projectId: String) {
        // Validate projectId to prevent crashes
        if (projectId.isBlank()) {
            Log.e(TAG, "Cannot load temporary approvers: projectId is empty")
            _error.value = "Invalid project ID"
            return
        }
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d(TAG, "🔄 Loading temporary approvers for project: $projectId")
                
                // First, deactivate any expired approvers
                temporaryApproverRepository.deactivateExpiredApprovers(projectId)
                
                // Then load all temporary approvers
                val result = temporaryApproverRepository.getTemporaryApprovers(projectId)
                
                if (result.isSuccess) {
                    _temporaryApprovers.value = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "✅ Loaded ${_temporaryApprovers.value.size} temporary approvers")
                } else {
                    val exception = result.exceptionOrNull()
                    _error.value = "Failed to load temporary approvers: ${exception?.message}"
                    Log.e(TAG, "❌ Failed to load temporary approvers", exception)
                }
                
            } catch (e: Exception) {
                _error.value = "Error loading temporary approvers: ${e.message}"
                Log.e(TAG, "❌ Exception while loading temporary approvers", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load available approvers (users with APPROVER role who can be assigned)
     */
    fun loadAvailableApprovers() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Loading available approvers from database")
                
                // Clear any previous error
                _error.value = null
                
                // Get current user's customer ID (their UID)
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                if (currentCustomerId == null) {
                    Log.e(TAG, "❌ No current user found")
                    _error.value = "User not authenticated"
                    _availableApprovers.value = emptyList()
                    return@launch
                }
                
                // Get all users from the database (returns List<User> directly)
                val allUsers = authRepository.getAllUsers()
                
                // Filter by current customer ID - only show approvers belonging to this customer
                val customerUsers = allUsers.filter { user ->
                    user.customerId == currentCustomerId || (user.customerId == null && user.uid == currentCustomerId)
                }
                
                // Safely filter users who can be approvers (only APPROVER role) and are active
                val approvers = customerUsers.filter { user ->
                    try {
                        user.isActive && user.role.name == "APPROVER"
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Error checking user ${user.name}: ${e.message}")
                        false
                    }
                }
                
                _availableApprovers.value = approvers
                Log.d(TAG, "✅ Loaded ${approvers.size} available approvers for customer: $currentCustomerId")
                
                // Log each approver for debugging
                approvers.forEach { approver ->
                    Log.d(TAG, "📋 Available approver: ${approver.name} (${approver.phone}) - Role: ${approver.role.name}, customerId: ${approver.customerId}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception while loading available approvers", e)
                _error.value = "Failed to load available approvers: ${e.message}"
                // Set empty list on error to prevent crash
                _availableApprovers.value = emptyList()
            }
        }
    }
    
    /**
     * Add a temporary approver to a project
     */
    fun addTemporaryApprover(
        projectId: String,
        approverId: String,
        approverName: String,
        approverPhone: String,
        startDate: Date,
        expiringDate: Date,
        assignedBy: String,
        assignedByName: String
    ) {
        viewModelScope.launch {
            try {
                _isAddingApprover.value = true
                _error.value = null
                _successMessage.value = null
                
                Log.d(TAG, "🔄 Adding temporary approver: $approverName to project: $projectId")
                
                val expiringTimestamp = Timestamp(expiringDate)
                val startingTimestamp = Timestamp(startDate)
                
                val result = temporaryApproverRepository.addTemporaryApprover(
                    projectId = projectId,
                    approverId = approverId,
                    approverName = approverName,
                    approverPhone = approverPhone,
                    startdate = startingTimestamp,
                    expiringDate = expiringTimestamp,
                    assignedBy = assignedBy,
                    assignedByName = assignedByName
                )
                
                if (result.isSuccess) {
                    _successMessage.value = "Temporary approver added successfully"
                    Log.d(TAG, "✅ Temporary approver added successfully")
                    
                    // Reload the temporary approvers list
                    loadTemporaryApprovers(projectId)
                } else {
                    val exception = result.exceptionOrNull()
                    _error.value = "Failed to add temporary approver: ${exception?.message}"
                    Log.e(TAG, "❌ Failed to add temporary approver", exception)
                }
                
            } catch (e: Exception) {
                _error.value = "Error adding temporary approver: ${e.message}"
                Log.e(TAG, "❌ Exception while adding temporary approver", e)
            } finally {
                _isAddingApprover.value = false
            }
        }
    }
    
    /**
     * Deactivate a temporary approver
     */
    fun deactivateTemporaryApprover(projectId: String, tempApproverId: String) {
        viewModelScope.launch {
            try {
                _error.value = null
                _successMessage.value = null
                
                Log.d(TAG, "🔄 Deactivating temporary approver: $tempApproverId")
                
                val result = temporaryApproverRepository.deactivateTemporaryApprover(projectId, tempApproverId)
                
                if (result.isSuccess) {
                    _successMessage.value = "Temporary approver deactivated successfully"
                    Log.d(TAG, "✅ Temporary approver deactivated successfully")
                    
                    // Reload the temporary approvers list
                    loadTemporaryApprovers(projectId)
                } else {
                    val exception = result.exceptionOrNull()
                    _error.value = "Failed to deactivate temporary approver: ${exception?.message}"
                    Log.e(TAG, "❌ Failed to deactivate temporary approver", exception)
                }
                
            } catch (e: Exception) {
                _error.value = "Error deactivating temporary approver: ${e.message}"
                Log.e(TAG, "❌ Exception while deactivating temporary approver", e)
            }
        }
    }
    
    /**
     * Accept a temporary approver assignment
     */
    fun acceptTemporaryApproverAssignment(
        projectId: String,
        approverId: String,
        responseMessage: String = ""
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _successMessage.value = null
                
                Log.d(TAG, "🔄 Accepting temporary approver assignment for project $projectId by approver $approverId")
                
                val result = temporaryApproverRepository.acceptTemporaryApproverAssignment(
                    projectId = projectId,
                    approverId = approverId,
                    responseMessage = responseMessage
                )
                
                if (result.isSuccess) {
                    _successMessage.value = "Assignment accepted successfully"
                    Log.d(TAG, "✅ Temporary approver assignment accepted successfully")
                } else {
                    val exception = result.exceptionOrNull()
                    _error.value = "Failed to accept assignment: ${exception?.message}"
                    Log.e(TAG, "❌ Failed to accept temporary approver assignment", exception)
                }
                
            } catch (e: Exception) {
                _error.value = "Error accepting assignment: ${e.message}"
                Log.e(TAG, "❌ Exception while accepting temporary approver assignment", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Reject a temporary approver assignment
     */
    fun rejectTemporaryApproverAssignment(
        projectId: String,
        approverId: String,
        responseMessage: String = ""
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _successMessage.value = null
                
                Log.d(TAG, "🔄 Rejecting temporary approver assignment for project $projectId by approver $approverId")
                
                val result = temporaryApproverRepository.rejectTemporaryApproverAssignment(
                    projectId = projectId,
                    approverId = approverId,
                    responseMessage = responseMessage
                )
                
                if (result.isSuccess) {
                    _successMessage.value = "Assignment rejected successfully"
                    Log.d(TAG, "✅ Temporary approver assignment rejected successfully")
                } else {
                    val exception = result.exceptionOrNull()
                    _error.value = "Failed to reject assignment: ${exception?.message}"
                    Log.e(TAG, "❌ Failed to reject temporary approver assignment", exception)
                }
                
            } catch (e: Exception) {
                _error.value = "Error rejecting assignment: ${e.message}"
                Log.e(TAG, "❌ Exception while rejecting temporary approver assignment", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update start date of a temporary approver
     */
    fun updateStartDate(projectId: String, tempApproverId: String, newStartDate: Timestamp) {
        viewModelScope.launch {
            try {
                _error.value = null
                _successMessage.value = null
                
                Log.d(TAG, "🔄 Updating start date for temporary approver: $tempApproverId")
                
                val result = temporaryApproverRepository.updateStartDate(
                    projectId, 
                    tempApproverId, 
                    newStartDate
                )
                
                if (result.isSuccess) {
                    _successMessage.value = "Start date updated successfully"
                    Log.d(TAG, "✅ Start date updated successfully")
                    
                    // Reload the temporary approvers list
                    loadTemporaryApprovers(projectId)
                } else {
                    val exception = result.exceptionOrNull()
                    _error.value = "Failed to update start date: ${exception?.message}"
                    Log.e(TAG, "❌ Failed to update start date", exception)
                }
                
            } catch (e: Exception) {
                _error.value = "Error updating start date: ${e.message}"
                Log.e(TAG, "❌ Exception while updating start date", e)
            }
        }
    }
    
    /**
     * Update expiring date of a temporary approver
     */
    fun updateExpiringDate(projectId: String, tempApproverId: String, newExpiringDate: Date) {
        viewModelScope.launch {
            try {
                _error.value = null
                _successMessage.value = null
                
                Log.d(TAG, "🔄 Updating expiring date for temporary approver: $tempApproverId")
                
                val newExpiringTimestamp = Timestamp(newExpiringDate)
                
                val result = temporaryApproverRepository.updateExpiringDate(
                    projectId, 
                    tempApproverId, 
                    newExpiringTimestamp
                )
                
                if (result.isSuccess) {
                    _successMessage.value = "Expiring date updated successfully"
                    Log.d(TAG, "✅ Expiring date updated successfully")
                    
                    // Reload the temporary approvers list
                    loadTemporaryApprovers(projectId)
                } else {
                    val exception = result.exceptionOrNull()
                    _error.value = "Failed to update expiring date: ${exception?.message}"
                    Log.e(TAG, "❌ Failed to update expiring date", exception)
                }
                
            } catch (e: Exception) {
                _error.value = "Error updating expiring date: ${e.message}"
                Log.e(TAG, "❌ Exception while updating expiring date", e)
            }
        }
    }
    
    /**
     * Delete a temporary approver
     */
    fun deleteTemporaryApprover(projectId: String, tempApproverId: String) {
        viewModelScope.launch {
            try {
                _error.value = null
                _successMessage.value = null
                
                Log.d(TAG, "🔄 Deleting temporary approver: $tempApproverId")
                
                val result = temporaryApproverRepository.deleteTemporaryApprover(projectId, tempApproverId)
                
                if (result.isSuccess) {
                    _successMessage.value = "Temporary approver deleted successfully"
                    Log.d(TAG, "✅ Temporary approver deleted successfully")
                    
                    // Reload the temporary approvers list
                    loadTemporaryApprovers(projectId)
                } else {
                    val exception = result.exceptionOrNull()
                    _error.value = "Failed to delete temporary approver: ${exception?.message}"
                    Log.e(TAG, "❌ Failed to delete temporary approver", exception)
                }
                
            } catch (e: Exception) {
                _error.value = "Error deleting temporary approver: ${e.message}"
                Log.e(TAG, "❌ Exception while deleting temporary approver", e)
            }
        }
    }
    
    /**
     * Check and deactivate expired approvers
     */
    fun checkAndDeactivateExpiredApprovers(projectId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Checking for expired temporary approvers")
                
                val result = temporaryApproverRepository.deactivateExpiredApprovers(projectId)
                
                if (result.isSuccess) {
                    val deactivatedCount = result.getOrNull() ?: 0
                    if (deactivatedCount > 0) {
                        _successMessage.value = "$deactivatedCount expired temporary approver(s) deactivated"
                        Log.d(TAG, "✅ Deactivated $deactivatedCount expired temporary approvers")
                        
                        // Reload the temporary approvers list
                        loadTemporaryApprovers(projectId)
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "❌ Failed to check expired approvers", exception)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception while checking expired approvers", e)
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    /**
     * Get active temporary approvers only
     */
    fun getActiveTemporaryApprovers(): List<TemporaryApprover> {
        return _temporaryApprovers.value.filter { tempApprover ->
            tempApprover.isActive
        }
    }
    
    /**
     * Get expired temporary approvers
     */
    fun getExpiredTemporaryApprovers(): List<TemporaryApprover> {
        return _temporaryApprovers.value.filter { tempApprover ->
            !tempApprover.isActive
        }
    }
    
    /**
     * Get inactive temporary approvers
     */
    fun getInactiveTemporaryApprovers(): List<TemporaryApprover> {
        return _temporaryApprovers.value.filter { tempApprover ->
            !tempApprover.isActive
        }
    }
    
    /**
     * Create a new temporary approver assignment
     */
    fun createTemporaryApprover(
        projectId: String,
        approverId: String,
        approverName: String,
        approverPhone: String,
        startDate: Timestamp,
        expiringDate: Timestamp?
    ) {
        viewModelScope.launch {
            _isAddingApprover.value = true
            _error.value = null
            
            try {
                val result = temporaryApproverRepository.createTemporaryApprover(
                    projectId = projectId,
                    approverId = approverId,
                    approverName = approverName,
                    approverPhone = approverPhone,
                    startDate = startDate,
                    expiringDate = expiringDate
                )
                
                if (result.isSuccess) {
                    _successMessage.value = "Temporary approver assigned successfully"
                    // Reload the list
                    loadTemporaryApprovers(projectId)
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to assign temporary approver"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to assign temporary approver"
            } finally {
                _isAddingApprover.value = false
            }
        }
    }
    
    /**
     * Update an existing temporary approver assignment
     */
    fun updateTemporaryApprover(
        projectId: String,
        updatedApprover: TemporaryApprover,
        originalApprover: TemporaryApprover? = null,
        changedBy: String = "System"
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d(TAG, "🔄 Updating temporary approver: ${updatedApprover.id}")
                Log.d(TAG, "   - approverId: ${updatedApprover.approverId}")
                Log.d(TAG, "   - approverName: ${updatedApprover.approverName}")
                Log.d(TAG, "   - startDate: ${updatedApprover.startDate}")
                Log.d(TAG, "   - expiringDate: ${updatedApprover.expiringDate}")
                
                val result = temporaryApproverRepository.updateTemporaryApprover(
                    projectId = projectId,
                    updatedApprover = updatedApprover,
                    originalApprover = originalApprover,
                    changedBy = changedBy
                )
                
                if (result.isSuccess) {
                    _successMessage.value = "Temporary approver updated successfully"
                    Log.d(TAG, "✅ Temporary approver updated successfully")
                    // Reload the list
                    loadTemporaryApprovers(projectId)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to update temporary approver"
                    _error.value = errorMsg
                    Log.e(TAG, "❌ Failed to update temporary approver: $errorMsg")
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Failed to update temporary approver"
                _error.value = errorMsg
                Log.e(TAG, "❌ Exception while updating temporary approver: $errorMsg", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Remove a temporary approver assignment by document ID
     */
    fun removeTemporaryApproverById(
        projectId: String,
        documentId: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d(TAG, "🔄 Removing temporary approver by document ID: $documentId")
                
                val result = temporaryApproverRepository.removeTemporaryApproverById(
                    projectId = projectId,
                    documentId = documentId
                )
                
                if (result.isSuccess) {
                    _successMessage.value = "Temporary approver removed successfully"
                    Log.d(TAG, "✅ Temporary approver removed successfully")
                    // Reload the list
                    loadTemporaryApprovers(projectId)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to remove temporary approver"
                    _error.value = errorMsg
                    Log.e(TAG, "❌ Failed to remove temporary approver: $errorMsg")
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Failed to remove temporary approver"
                _error.value = errorMsg
                Log.e(TAG, "❌ Exception while removing temporary approver: $errorMsg", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Remove a temporary approver assignment by approver ID (fallback method)
     */
    fun removeTemporaryApprover(
        projectId: String,
        approverId: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d(TAG, "🔄 Removing temporary approver: $approverId")
                
                val result = temporaryApproverRepository.removeTemporaryApprover(
                    projectId = projectId,
                    approverId = approverId
                )
                
                if (result.isSuccess) {
                    _successMessage.value = "Temporary approver removed successfully"
                    Log.d(TAG, "✅ Temporary approver removed successfully")
                    // Reload the list
                    loadTemporaryApprovers(projectId)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to remove temporary approver"
                    _error.value = errorMsg
                    Log.e(TAG, "❌ Failed to remove temporary approver: $errorMsg")
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Failed to remove temporary approver"
                _error.value = errorMsg
                Log.e(TAG, "❌ Exception while removing temporary approver: $errorMsg", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
