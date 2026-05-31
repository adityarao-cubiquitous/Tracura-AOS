package com.cubiquitous.tracura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.DepartmentBudget
import com.cubiquitous.tracura.model.PhaseDraftSerializable
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.Phase
import com.cubiquitous.tracura.model.PhaseWithDepartments
import com.cubiquitous.tracura.model.DepartmentTemporaryApproverEntry
import com.cubiquitous.tracura.model.ProjectDraft
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.model.PhaseDraft
import com.cubiquitous.tracura.model.CustomerUserRelation
import com.cubiquitous.tracura.repository.AuthRepository
import com.cubiquitous.tracura.repository.ProjectRepository
import com.cubiquitous.tracura.repository.NotificationRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class BusinessHeadViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    
    // UI States
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    // User Creation States
    private val _availableUsers = MutableStateFlow<List<User>>(emptyList())
    val availableUsers: StateFlow<List<User>> = _availableUsers.asStateFlow()
    
    private val _availableApprovers = MutableStateFlow<List<User>>(emptyList())
    val availableApprovers: StateFlow<List<User>> = _availableApprovers.asStateFlow()

    private val _availableManagers = MutableStateFlow<List<User>>(emptyList())
    val availableManagers: StateFlow<List<User>> = _availableManagers.asStateFlow()

    private val _allUsersForManagement = MutableStateFlow<List<User>>(emptyList())
    val allUsersForManagement: StateFlow<List<User>> = _allUsersForManagement.asStateFlow()
    
    // Project Creation States
    private val _departmentBudgets = MutableStateFlow<List<DepartmentBudget>>(emptyList())
    val departmentBudgets: StateFlow<List<DepartmentBudget>> = _departmentBudgets.asStateFlow()
    
    private val _totalBudget = MutableStateFlow(0.0)
    val totalBudget: StateFlow<Double> = _totalBudget.asStateFlow()
    
    private val _totalAllocated = MutableStateFlow(0.0)
    val totalAllocated: StateFlow<Double> = _totalAllocated.asStateFlow()
    
    // Project Edit States
    private val _editingProject = MutableStateFlow<Project?>(null)
    val editingProject: StateFlow<Project?> = _editingProject.asStateFlow()
    
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()
    
    // Review Project Data
    var projectIdForResubmission: String? = null // For resubmitting REVIEW REJECTED projects
    
    data class ReviewProjectData(
        val projectName: String = "",
        val description: String = "",
        val client: String = "",
        val clientPrimaryNumber: String? = null,
        val clientSecondaryNumber: String? = null,
        val location: String = "",
        val currency: String = "",
        val plannedStartDate: java.util.Date? = null,
        val handoverDate: java.util.Date? = null,
        val maintenanceDate: java.util.Date? = null,
        val phases: List<PhaseDraft> = emptyList(),
        val approver: User? = null,
        val teamMembers: List<User> = emptyList(),
        val departmentUserAssignments: Map<String, String> = emptyMap(),
        val departmentApproverAssignments: Map<String, String> = emptyMap()
    )
    
    private val _reviewData = MutableStateFlow(ReviewProjectData())
    val reviewData: StateFlow<ReviewProjectData> = _reviewData.asStateFlow()
    
    // All existing phases from all projects (for duplicate checking)
    private val _allExistingPhases = MutableStateFlow<List<Phase>>(emptyList())
    val allExistingPhases: StateFlow<List<Phase>> = _allExistingPhases.asStateFlow()
    
    // Business type for filtering templates
    private val _businessType = MutableStateFlow<String?>(null)
    val businessType: StateFlow<String?> = _businessType.asStateFlow()
    
    // Customer departments (names only, for simple UIs)
    private val _customerDepartments = MutableStateFlow<List<String>>(emptyList())
    val customerDepartments: StateFlow<List<String>> = _customerDepartments.asStateFlow()

    // Customer department counts (users and approvers)
    private val _departmentUserCounts = MutableStateFlow<Map<String, Long>>(emptyMap())
    val departmentUserCounts: StateFlow<Map<String, Long>> = _departmentUserCounts.asStateFlow()

    private val _departmentApproverCounts = MutableStateFlow<Map<String, Long>>(emptyMap())
    val departmentApproverCounts: StateFlow<Map<String, Long>> = _departmentApproverCounts.asStateFlow()
    
    // Vendor management
    private val _vendors = MutableStateFlow<Map<String, String>>(emptyMap())
    val vendors: StateFlow<Map<String, String>> = _vendors.asStateFlow()
    
    // Department User Management
    private val _departmentUsersMap = MutableStateFlow<Map<String, List<User>>>(emptyMap())
    val departmentUsersMap: StateFlow<Map<String, List<User>>> = _departmentUsersMap.asStateFlow()
    
    private val _departmentApproversMap = MutableStateFlow<Map<String, List<User>>>(emptyMap())
    val departmentApproversMap: StateFlow<Map<String, List<User>>> = _departmentApproversMap.asStateFlow()
    
    // CustomerUserRelation management for View All Users screen
    private val _customerUserRelations = MutableStateFlow<List<CustomerUserRelation>>(emptyList())
    val customerUserRelations: StateFlow<List<CustomerUserRelation>> = _customerUserRelations.asStateFlow()

    private val _deleteUserError = MutableStateFlow<String?>(null)
    val deleteUserError: StateFlow<String?> = _deleteUserError.asStateFlow()

    private val _deleteUserSuccess = MutableStateFlow(false)
    val deleteUserSuccess: StateFlow<Boolean> = _deleteUserSuccess.asStateFlow()

    // ── Firestore Draft Projects ─────────────────────────────────────────────
    private val _firestoreDrafts = MutableStateFlow<List<ProjectDraft>>(emptyList())
    val firestoreDrafts: StateFlow<List<ProjectDraft>> = _firestoreDrafts.asStateFlow()

    private val _isSavingDraft = MutableStateFlow(false)
    val isSavingDraft: StateFlow<Boolean> = _isSavingDraft.asStateFlow()

    /** Tracks which Firestore draft doc is currently loaded so re-saves UPDATE it. */
    private var _currentDraftId: String? = null
    
    fun setReviewData(
        projectName: String,
        description: String,
        client: String,
        clientPrimaryNumber: String? = null,
        clientSecondaryNumber: String? = null,
        location: String,
        currency: String,
        plannedStartDate: java.util.Date?,
        handoverDate: java.util.Date?,
        maintenanceDate: java.util.Date?,
        phases: List<PhaseDraft>,
        approver: User?,
        teamMembers: List<User>,
        departmentUserAssignments: Map<String, String> = emptyMap(),
        departmentApproverAssignments: Map<String, String> = emptyMap()
    ) {
        // Deep copy phases to ensure departments are preserved
        val phasesCopy = phases.map { phase ->
            PhaseDraft(
                phaseName = phase.phaseName,
                start = phase.start,
                end = phase.end,
                departments = phase.departments.toMutableList(), // Ensure departments are copied
                categories = phase.categories.toMutableList()
            )
        }
        
        // Reduced excessive logging for better performance
        
        _reviewData.value = ReviewProjectData(
            projectName = projectName,
            description = description,
            client = client,
            clientPrimaryNumber = clientPrimaryNumber,
            clientSecondaryNumber = clientSecondaryNumber,
            location = location,
            currency = currency,
            plannedStartDate = plannedStartDate,
            handoverDate = handoverDate,
            maintenanceDate = maintenanceDate,
            phases = phasesCopy, // Use copied phases
            approver = approver,
            teamMembers = teamMembers,
            departmentUserAssignments = departmentUserAssignments,
            departmentApproverAssignments = departmentApproverAssignments
        )
        
        android.util.Log.d("BusinessHeadViewModel", "✅ Review data set successfully")
    }
    
    init {
        loadUsers(showLoading = false) // Load users in background without showing loading
        // Removed loadAllExistingPhases() from init - it will be loaded on-demand when needed
        // This significantly improves initial load time
    }
    
    // Load all existing phases from all projects for duplicate checking - optimized
    fun loadAllExistingPhases() {
        viewModelScope.launch {
            try {
                // Get current user's customer ID (their UID)
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                if (currentCustomerId == null) {
                    android.util.Log.e("BusinessHeadViewModel", "❌ No current user found for loading phases")
                    _allExistingPhases.value = emptyList()
                    return@launch
                }
                
                // Get all projects for this customer
                val allProjects = projectRepository.getAllProjects()
                
                // Load phases from all projects concurrently for better performance
                val allPhases = mutableListOf<Phase>()
                allProjects.forEach { project ->
                    try {
                        val phases = projectRepository.getPhasesForProject(project.id ?: "")
                        allPhases.addAll(phases)
                    } catch (e: Exception) {
                        android.util.Log.e("BusinessHeadViewModel", "Error loading phases for project ${project.id}")
                    }
                }
                
                _allExistingPhases.value = allPhases
                android.util.Log.d("BusinessHeadViewModel", "✅ Loaded ${allPhases.size} phases from ${allProjects.size} projects")
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error loading all existing phases: ${e.message}", e)
                _allExistingPhases.value = emptyList()
            }
        }
    }
    
    fun loadUsers(showLoading: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) {
                _isLoading.value = true
            }
            try {
                // Get current user's customer ID (their UID)
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                if (currentCustomerId == null) {
                    android.util.Log.e("BusinessHeadViewModel", "❌ No current user found")
                    _availableUsers.value = emptyList()
                    _availableApprovers.value = emptyList()
                    _availableManagers.value = emptyList()
                    _allUsersForManagement.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }
                
                android.util.Log.d("BusinessHeadViewModel", "🔄 Loading users from CustomerUserRelation for customer: $currentCustomerId")
                
                // Fetch CustomerUserRelations for this customer
                val relationsResult = authRepository.fetchCustomerUserRelations(currentCustomerId)
                
                if (relationsResult.isFailure) {
                    android.util.Log.e("BusinessHeadViewModel", "❌ Failed to fetch CustomerUserRelations: ${relationsResult.exceptionOrNull()?.message}")
                    _availableUsers.value = emptyList()
                    _availableApprovers.value = emptyList()
                    _availableManagers.value = emptyList()
                    _allUsersForManagement.value = emptyList()
                    _error.value = "Failed to load users: ${relationsResult.exceptionOrNull()?.message}"
                    return@launch
                }
                
                val relations = relationsResult.getOrNull() ?: emptyList()
                android.util.Log.d("BusinessHeadViewModel", "📊 Found ${relations.size} CustomerUserRelations")
                
                // Convert relations to User objects
                val allUsers = relations.map { relation ->
                    authRepository.convertRelationToUser(relation)
                }
                
                // Filter by role
                val regularUsers = allUsers.filter { it.role == UserRole.USER }
                val approvers = allUsers.filter { it.role == UserRole.APPROVER }
                val managers = allUsers.filter { it.role == UserRole.MANAGER }

                // Only expose ACTIVE users to the UI for project assignment
                val activeRegularUsers = regularUsers.filter { it.isActive }
                val activeApprovers = approvers.filter { it.isActive }
                val activeManagers = managers.filter { it.isActive }

                android.util.Log.d("BusinessHeadViewModel", "👥 Active regular users: ${activeRegularUsers.size}")
                android.util.Log.d("BusinessHeadViewModel", "✅ Active approvers: ${activeApprovers.size}")
                android.util.Log.d("BusinessHeadViewModel", "🧑‍💼 Active managers: ${activeManagers.size}")

                activeRegularUsers.forEach { user ->
                    android.util.Log.d("BusinessHeadViewModel", "  👤 User: ${user.name} (${user.phone}) - dept: ${user.department}")
                }

                activeApprovers.forEach { approver ->
                    android.util.Log.d("BusinessHeadViewModel", "  ✅ Approver: ${approver.name} (${approver.phone}) - dept: ${approver.department}")
                }

                _availableUsers.value = activeRegularUsers
                _availableApprovers.value = activeApprovers
                _availableManagers.value = activeManagers

                // Store ALL users (including inactive) for the management screen
                _allUsersForManagement.value = regularUsers + approvers + managers
                
                android.util.Log.d("BusinessHeadViewModel", "✅ Successfully loaded users from CustomerUserRelation")
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error loading users: ${e.message}", e)
                _error.value = "Failed to load users: ${e.message}"
            } finally {
                if (showLoading) {
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun updateUserActiveStatus(phoneNumber: String, isActive: Boolean) {
        viewModelScope.launch {
            // Update local state immediately without showing loading
            try {
                android.util.Log.d("BusinessHeadViewModel", "🔄 Updating user active status: $phoneNumber to $isActive")
                
                // Get user's UID before updating
                val userToUpdate = _allUsersForManagement.value.find { it.phone == phoneNumber }
                val userId = userToUpdate?.uid ?: ""
                
                // Update the user in ALL lists immediately
                val updatedUsers = _availableUsers.value.map { user ->
                    if (user.phone == phoneNumber) user.copy(isActive = isActive) else user
                }
                val updatedApprovers = _availableApprovers.value.map { user ->
                    if (user.phone == phoneNumber) user.copy(isActive = isActive) else user
                }
                val updatedAllUsers = _allUsersForManagement.value.map { user ->
                    if (user.phone == phoneNumber) user.copy(isActive = isActive) else user
                }
                
                _availableUsers.value = updatedUsers
                _availableApprovers.value = updatedApprovers
                _allUsersForManagement.value = updatedAllUsers
                
                // Update Firebase in the background
                val result = authRepository.updateUserActiveStatus(phoneNumber, isActive)
                if (result.isSuccess) {
                    android.util.Log.d("BusinessHeadViewModel", "✅ User active status updated successfully in Firebase")
                    
                    // If disabling the user, remove them from all projects
                    if (!isActive && userId.isNotEmpty()) {
                        android.util.Log.d("BusinessHeadViewModel", "🔄 Removing user from all projects")
                        val removeResult = projectRepository.removeUserFromAllProjects(userId)
                        if (removeResult.isSuccess) {
                            val removedCount = removeResult.getOrNull() ?: 0
                            android.util.Log.d("BusinessHeadViewModel", "✅ Successfully removed user from $removedCount project(s)")
                        } else {
                            android.util.Log.e("BusinessHeadViewModel", "❌ Failed to remove user from projects")
                        }
                    }
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to update user status"
                    android.util.Log.e("BusinessHeadViewModel", "❌ User active status update failed: $errorMsg")
                    
                    // Revert the local changes on error
                    loadUsers()
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error updating user active status: ${e.message}", e)
                // Revert the local changes on error
                loadUsers()
                _error.value = "Error updating user status: ${e.message}"
            }
        }
    }
    
    // State for duplicate user detection
    private val _showDuplicateAlert = MutableStateFlow(false)
    val showDuplicateAlert: StateFlow<Boolean> = _showDuplicateAlert.asStateFlow()
    
    private val _pendingUserData = MutableStateFlow<PendingUserData?>(null)
    val pendingUserData: StateFlow<PendingUserData?> = _pendingUserData.asStateFlow()
    
    data class PendingUserData(
        val phoneNumber: String,
        val fullName: String,
        val role: UserRole,
        val department: String
    )
    
    fun createUser(phoneNumber: String, fullName: String, role: UserRole, department: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Get current user's customer ID (their UID)
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                if (currentCustomerId == null) {
                    _error.value = "User not authenticated. Please sign in again."
                    _isLoading.value = false
                    return@launch
                }
                
                val cleanPhone = phoneNumber.replace("+91", "").trim()
                
                // Check if user already exists for this customer
                val exists = authRepository.checkUserExistsForCustomer(cleanPhone, currentCustomerId)
                
                if (exists) {
                    // Store pending data and show duplicate alert
                    _pendingUserData.value = PendingUserData(cleanPhone, fullName, role, department)
                    _showDuplicateAlert.value = true
                    _isLoading.value = false
                    android.util.Log.d("BusinessHeadViewModel", "⚠️ User already exists, showing duplicate alert")
                } else {
                    // No duplicate, proceed with creation
                    createUserInternal(cleanPhone, fullName, role, department, currentCustomerId, overwrite = false)
                }
            } catch (e: Exception) {
                _error.value = "Error checking user: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun createUserWithOverwrite() {
        viewModelScope.launch {
            val pending = _pendingUserData.value
            if (pending == null) {
                _error.value = "No pending user data"
                return@launch
            }
            
            _isLoading.value = true
            _error.value = null
            _showDuplicateAlert.value = false
            
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                if (currentCustomerId == null) {
                    _error.value = "User not authenticated. Please sign in again."
                    _isLoading.value = false
                    return@launch
                }
                
                createUserInternal(
                    pending.phoneNumber,
                    pending.fullName,
                    pending.role,
                    pending.department,
                    currentCustomerId,
                    overwrite = true
                )
            } catch (e: Exception) {
                _error.value = "Error creating user: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun dismissDuplicateAlert() {
        _showDuplicateAlert.value = false
        _pendingUserData.value = null
        _isLoading.value = false
    }
    
    private suspend fun createUserInternal(
        phoneNumber: String,
        fullName: String,
        role: UserRole,
        department: String,
        customerId: String,
        overwrite: Boolean
    ) {
        try {
            val result = authRepository.createUserWithRelation(
                phoneNumber = phoneNumber,
                name = fullName,
                role = role,
                department = department,
                customerId = customerId,
                overwrite = overwrite
            )
            
            if (result.isSuccess) {
                val action = if (overwrite) "updated" else "created"
                _successMessage.value = "User $action successfully!"
                _pendingUserData.value = null
                loadUsers() // Refresh user list
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to create user"
            }
        } catch (e: Exception) {
            _error.value = "Error creating user: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    fun loadCustomerDepartments() {
        viewModelScope.launch {
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                if (currentCustomerId != null) {
                    val customer = projectRepository.getCustomerById(currentCustomerId)
                    _customerDepartments.value = customer?.departments ?: emptyList()

                    // Also load department user / approver counts
                    val (userCounts, approverCounts) =
                        projectRepository.getCustomerDepartmentCounts(currentCustomerId)
                    _departmentUserCounts.value = userCounts
                    _departmentApproverCounts.value = approverCounts
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "Error loading departments: ${e.message}")
            }
        }
    }
    
    fun addDepartmentToCustomer(departmentName: String) {
        viewModelScope.launch {
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                if (currentCustomerId != null) {
                    val currentDepartments = _customerDepartments.value.toMutableList()
                    if (!currentDepartments.contains(departmentName)) {
                        currentDepartments.add(departmentName)
                        val result = projectRepository.updateCustomerDepartments(currentCustomerId, currentDepartments)
                        if (result.isSuccess) {
                            _customerDepartments.value = currentDepartments
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "Error adding department: ${e.message}")
            }
        }
    }
    
    fun updateDepartmentInCustomer(oldName: String, newName: String) {
        viewModelScope.launch {
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                if (currentCustomerId != null) {
                    val currentDepartments = _customerDepartments.value.toMutableList()
                    val index = currentDepartments.indexOf(oldName)
                    if (index != -1) {
                        currentDepartments[index] = newName
                        val result = projectRepository.updateCustomerDepartments(currentCustomerId, currentDepartments)
                        if (result.isSuccess) {
                            _customerDepartments.value = currentDepartments
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "Error updating department: ${e.message}")
            }
        }
    }
    
    fun loadVendors() {
        viewModelScope.launch {
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                if (currentCustomerId != null) {
                    val vendorsMap = projectRepository.getCustomerVendors(currentCustomerId)
                    _vendors.value = vendorsMap
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "Error loading vendors: ${e.message}")
            }
        }
    }
    
    fun addVendor(vendorName: String, departmentName: String) {
        viewModelScope.launch {
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                if (currentCustomerId != null) {
                    val result = projectRepository.addOrUpdateVendor(currentCustomerId, vendorName, departmentName)
                    if (result.isSuccess) {
                        // Reload vendors
                        loadVendors()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "Error adding vendor: ${e.message}")
            }
        }
    }
    
    fun deleteVendor(vendorName: String) {
        viewModelScope.launch {
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                if (currentCustomerId != null) {
                    val result = projectRepository.deleteVendor(currentCustomerId, vendorName)
                    if (result.isSuccess) {
                        // Reload vendors
                        loadVendors()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "Error deleting vendor: ${e.message}")
            }
        }
    }
    
    fun updateVendor(oldVendorName: String, newVendorName: String, newDepartmentName: String) {
        viewModelScope.launch {
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                if (currentCustomerId != null) {
                    if (oldVendorName == newVendorName) {
                        // Only department changed, just update
                        val result = projectRepository.addOrUpdateVendor(currentCustomerId, newVendorName, newDepartmentName)
                        if (result.isSuccess) {
                            loadVendors()
                        }
                    } else {
                        // Vendor name changed, delete old and add new
                        val deleteResult = projectRepository.deleteVendor(currentCustomerId, oldVendorName)
                        if (deleteResult.isSuccess) {
                            val addResult = projectRepository.addOrUpdateVendor(currentCustomerId, newVendorName, newDepartmentName)
                            if (addResult.isSuccess) {
                                loadVendors()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "Error updating vendor: ${e.message}")
            }
        }
    }
    
    fun addNewDepartmentToCustomer(departmentName: String) {
        viewModelScope.launch {
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                if (currentCustomerId != null) {
                    val result = projectRepository.addNewDepartmentToCustomer(currentCustomerId, departmentName)
                    if (result.isSuccess) {
                        // Reload departments
                        loadCustomerDepartments()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "Error adding new department: ${e.message}")
            }
        }
    }
    
    /**
     * Load all CustomerUserRelation records for the current customer
     * Matches iOS UserListViewModel.fetchUsers() logic
     */
    fun loadCustomerUserRelations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                if (currentCustomerId == null) {
                    android.util.Log.e("BusinessHeadViewModel", "❌ No current user found")
                    _error.value = "User not authenticated. Please sign in again."
                    _customerUserRelations.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }
                
                android.util.Log.d("BusinessHeadViewModel", "🔄 Loading CustomerUserRelations for customer: $currentCustomerId")
                
                val result = authRepository.fetchCustomerUserRelations(currentCustomerId)
                if (result.isSuccess) {
                    val relations = result.getOrNull() ?: emptyList()
                    _customerUserRelations.value = relations
                    android.util.Log.d("BusinessHeadViewModel", "✅ Loaded ${relations.size} CustomerUserRelations")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to load users"
                    _error.value = errorMsg
                    android.util.Log.e("BusinessHeadViewModel", "❌ Error loading CustomerUserRelations: $errorMsg")
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error loading CustomerUserRelations: ${e.message}", e)
                _error.value = "Error loading users: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Toggle the active status of a CustomerUserRelation
     * Implements optimistic update pattern from iOS
     */
    fun toggleUserRelationStatus(relationId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            // Optimistic update: Update UI immediately
            val newStatus = !currentStatus
            val currentRelations = _customerUserRelations.value
            val updatedRelations = currentRelations.map { relation ->
                if (relation.id == relationId) {
                    relation.copy(isActive = newStatus)
                } else {
                    relation
                }
            }
            _customerUserRelations.value = updatedRelations
            
            android.util.Log.d("BusinessHeadViewModel", "🔄 Toggling user relation status: id=$relationId, newStatus=$newStatus (optimistic)")
            
            // Send update to Firestore
            try {
                val result = authRepository.toggleUserRelationStatus(relationId, newStatus)
                if (result.isSuccess) {
                    android.util.Log.d("BusinessHeadViewModel", "✅ Successfully toggled user relation status in Firestore")
                } else {
                    // Revert optimistic update on failure
                    _customerUserRelations.value = currentRelations
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to update user status"
                    _error.value = errorMsg
                    android.util.Log.e("BusinessHeadViewModel", "❌ Failed to toggle status in Firestore, reverted UI: $errorMsg")
                }
            } catch (e: Exception) {
                // Revert optimistic update on exception
                _customerUserRelations.value = currentRelations
                _error.value = "Error updating user status: ${e.message}"
                android.util.Log.e("BusinessHeadViewModel", "❌ Exception toggling status, reverted UI: ${e.message}")
            }
        }
    }
    
    /**
     * Validate and delete a user's CustomerUserRelation.
     * Uses the current Firebase UID as the customerId.
     */
    fun deleteUserRelation(phoneNumber: String, role: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _deleteUserError.value = null
            try {
                val customerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    ?: run {
                        _deleteUserError.value = "User not authenticated"
                        _isLoading.value = false
                        return@launch
                    }
                val result = authRepository.validateAndDeleteUserFromCustomer(phoneNumber, customerId, role)
                if (result.isSuccess) {
                    _customerUserRelations.value = _customerUserRelations.value.filter { it.userID != phoneNumber }
                    _deleteUserSuccess.value = true
                    android.util.Log.d("BusinessHeadViewModel", "✅ Deleted user $phoneNumber")
                } else {
                    _deleteUserError.value = result.exceptionOrNull()?.message ?: "Failed to delete user"
                }
            } catch (e: Exception) {
                _deleteUserError.value = "Error deleting user: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearDeleteUserError() {
        _deleteUserError.value = null
    }

    fun clearDeleteUserSuccess() {
        _deleteUserSuccess.value = false
    }

    /**
     * Create project with PhaseDraft (includes full department details with line items)
     */
    fun createProjectWithPhaseDrafts(
        projectName: String,
        description: String,
        client: String,
        clientPrimaryNumber: String? = null,
        clientSecondaryNumber: String? = null,
        location: String,
        currency: String,
        startDate: Timestamp,
        endDate: Timestamp?,
        handoverDate: String? = null,
        maintenanceDate: String? = null,
        totalBudget: Double,
        managerId: String? = null, // Now nullable - using department-specific assignments
        teamMemberIds: List<String> = emptyList(), // Now optional - using department-specific assignments
        categories: List<String> = emptyList(),
        phaseDrafts: List<PhaseDraft> = emptyList(),
        departmentUserAssignments: Map<String, String> = emptyMap(),
        departmentApproverAssignments: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                android.util.Log.d("BusinessHeadViewModel", "🚀 Creating project with PhaseDrafts: $projectName")
                
                // Validate inputs
                if (projectName.isBlank()) {
                    throw Exception("Project name cannot be empty")
                }
                
                // Manager and team members are now optional - using department-specific assignments
                // Validation removed for managerId and teamMemberIds
                
                // Get current user to get UID for customer lookup
                val currentUser = authRepository.getCurrentUserFromFirebase()
                if (currentUser == null) {
                    throw Exception("User not authenticated. Please sign in again.")
                }
                
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (firebaseUser == null || firebaseUser.uid.isBlank()) {
                    throw Exception("Firebase user not found. Please sign in again.")
                }
                
                val customerId = firebaseUser.uid

                val isDuplicateName = projectRepository.isProjectNameAlreadyUsed(
                    customerId = customerId,
                    projectName = projectName
                )
                if (isDuplicateName) {
                    _error.value = "Project name already exists. Please select a different name."
                    _isLoading.value = false
                    return@launch
                }
                
                // Ensure customer document exists
                val userEmail = if (currentUser.email.isNotBlank()) {
                    currentUser.email
                } else if (currentUser.phone.isNotBlank()) {
                    "${currentUser.phone}@avr.local"
                } else {
                    "user@avr.local"
                }
                
                val customerResult = projectRepository.getOrCreateCustomerWithUid(
                    uid = customerId,
                    email = userEmail,
                    name = currentUser.name.ifBlank { "User ${currentUser.phone}" },
                    phoneNumber = currentUser.phone
                )
                
                if (customerResult.isFailure) {
                    android.util.Log.w("BusinessHeadViewModel", "⚠️ Customer creation failed: ${customerResult.exceptionOrNull()?.message}, but continuing with UID")
                } else {
                    android.util.Log.d("BusinessHeadViewModel", "✅ Customer document ensured with UID: $customerId")
                }
                
                // Format dates as "dd/MM/yyyy" strings
                val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val startDateString = startDate?.toDate()?.let { dateFormatter.format(it) }
                val endDateString = endDate?.toDate()?.let { dateFormatter.format(it) }
                
                // Create project
                val project = Project(
                    id = null,
                    name = projectName,
                    description = description,
                    client = client,
                    clientPrimaryNumber = clientPrimaryNumber,
                    clientSecondaryNumber = clientSecondaryNumber,
                    location = location,
                    currency = currency,
                    budget = totalBudget,
                    status = "IN REVIEW",
                    startDate = startDateString,
                    endDate = endDateString,
                    handoverDate = handoverDate,
                    maintenanceDate = maintenanceDate,
                    teamMembers = teamMemberIds,
                    departmentUserAssignments = departmentUserAssignments,
                    departmentApproverAssignments = departmentApproverAssignments,
                    tempApproverID = null,
                    Allow_Template_Overrides = null,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now(),
                    managerId = managerId, // Can be null - using department-specific assignments
                    remainingBalance = totalBudget // Initialize remaining balance with total budget

                )
                
                android.util.Log.d("BusinessHeadViewModel", "📦 Project object created, sending to repository")
                
                val result = projectRepository.createProject(project, customerId)
                if (result.isSuccess) {
                    val createdProjectId = result.getOrNull()
                    android.util.Log.d("BusinessHeadViewModel", "✅ Project created successfully with ID: $createdProjectId")
                    
                    val projectWithId = project.copy(id = createdProjectId)
                    
                    // Save phases with departments as separate documents
                    if (!phaseDrafts.isNullOrEmpty() && !createdProjectId.isNullOrEmpty()) {
                        android.util.Log.d("BusinessHeadViewModel", "📋 Saving ${phaseDrafts.size} phases with departments to project: $createdProjectId")
                        
                        val phaseResult = projectRepository.addPhasesWithDepartmentsToProject(
                            customerId, 
                            createdProjectId, 
                            phaseDrafts
                        )
                        if (phaseResult.isSuccess) {
                            android.util.Log.d("BusinessHeadViewModel", "✅ Successfully saved ${phaseDrafts.size} phases with departments")
                            
                            // Update User_Customer_Project_Phase_Relation collection
                            android.util.Log.d("BusinessHeadViewModel", "🔗 Updating User_Customer_Project_Phase_Relation for users and approvers")
                            val relationResult = projectRepository.updateUserCustomerProjectPhaseRelation(
                                customerId = customerId,
                                projectId = createdProjectId,
                                phaseDrafts = phaseDrafts,
                                departmentUserAssignments = departmentUserAssignments,
                                departmentApproverAssignments = departmentApproverAssignments
                            )
                            if (relationResult.isSuccess) {
                                android.util.Log.d("BusinessHeadViewModel", "✅ Successfully updated User_Customer_Project_Phase_Relation")
                            } else {
                                android.util.Log.e("BusinessHeadViewModel", "⚠️ Failed to update User_Customer_Project_Phase_Relation: ${relationResult.exceptionOrNull()?.message}")
                                // Don't fail the entire operation, just log the warning
                            }
                        } else {
                            android.util.Log.e("BusinessHeadViewModel", "❌ Failed to add phases: ${phaseResult.exceptionOrNull()?.message}")
                            _error.value = "Project created but failed to save phases: ${phaseResult.exceptionOrNull()?.message}"
                        }
                    } else {
                        android.util.Log.w("BusinessHeadViewModel", "⚠️ No phases to save or project ID is null")
                    }
                    
                    // Send notifications (only if managerId and teamMemberIds are provided)
                    if (createdProjectId != null && managerId != null && teamMemberIds.isNotEmpty()) {
                        sendNewProjectNotifications(projectWithId, managerId, teamMemberIds)
                    }
                    
                    loadAllExistingPhases()
                    _successMessage.value = "Project created successfully!"
                    clearProjectForm()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to create project"
                    android.util.Log.e("BusinessHeadViewModel", "❌ Project creation failed: $errorMsg")
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error creating project: ${e.message}", e)
                _error.value = "Error creating project: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createProject(
        projectName: String,
        description: String,
        client: String,
        location: String,
        currency: String,
        startDate: Timestamp,
        endDate: Timestamp?,
        handoverDate: String? = null,
        maintenanceDate: String? = null,
        totalBudget: Double,
        managerId: String,
        teamMemberIds: List<String>,
        departmentBudgets: List<DepartmentBudget>,
        categories: List<String> = emptyList(),
        phases: List<Phase> = emptyList()
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                android.util.Log.d("BusinessHeadViewModel", "🚀 Creating project: $projectName with ${teamMemberIds.size} members")
                
                // Validate inputs
                if (projectName.isBlank()) {
                    throw Exception("Project name cannot be empty")
                }
                
                if (managerId.isBlank()) {
                    throw Exception("Manager ID cannot be empty")
                }
                
                if (teamMemberIds.isEmpty()) {
                    throw Exception("At least one team member must be selected")
                }
                
                val budgetMap = departmentBudgets.associate { 
                    it.departmentName to it.allocatedBudget 
                }
                
                // Get current user to get UID for customer lookup
                val currentUser = authRepository.getCurrentUserFromFirebase()
                if (currentUser == null) {
                    throw Exception("User not authenticated. Please sign in again.")
                }
                
                // Get Firebase Auth current user to get UID
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (firebaseUser == null || firebaseUser.uid.isBlank()) {
                    throw Exception("Firebase user not authenticated")
                }
                
                // Use current user's UID as customerId (as per user requirement)
                val customerId = firebaseUser.uid
                android.util.Log.d("BusinessHeadViewModel", "✅ Using customer ID (current user UID): $customerId")

                val isDuplicateName = projectRepository.isProjectNameAlreadyUsed(
                    customerId = customerId,
                    projectName = projectName
                )
                if (isDuplicateName) {
                    _error.value = "Project name already exists. Please select a different name."
                    _isLoading.value = false
                    return@launch
                }
                
                // Ensure customer document exists with this UID
                val userEmail = if (currentUser.email.isNotBlank()) {
                    currentUser.email
                } else if (currentUser.phone.isNotBlank()) {
                    "${currentUser.phone}@avr.local"
                } else {
                    "user@avr.local"
                }
                
                // Create or update customer document with UID as document ID
                val customerResult = projectRepository.getOrCreateCustomerWithUid(
                    uid = customerId,
                    email = userEmail,
                    name = currentUser.name.ifBlank { "User ${currentUser.phone}" },
                    phoneNumber = currentUser.phone
                )
                
                if (customerResult.isFailure) {
                    android.util.Log.w("BusinessHeadViewModel", "⚠️ Customer creation failed: ${customerResult.exceptionOrNull()?.message}, but continuing with UID")
                } else {
                    android.util.Log.d("BusinessHeadViewModel", "✅ Customer document ensured with UID: $customerId")
                }
                
                // Format dates as "dd/MM/yyyy" strings to match iOS model
                val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val startDateString = startDate?.toDate()?.let { dateFormatter.format(it) }
                val endDateString = endDate?.toDate()?.let { dateFormatter.format(it) }
                
                // Create project matching iOS model structure
                val project = Project(
                    id = null, // Will be generated by Firestore
                    name = projectName,
                    description = description,
                    client = client,
                    location = location,
                    currency = currency,
                    budget = totalBudget,
                    status = "IN REVIEW", // Projects start as IN REVIEW when created, awaiting approver approval
                    startDate = startDateString,
                    endDate = endDateString,
                    handoverDate = handoverDate,
                    maintenanceDate = maintenanceDate,
                    teamMembers = teamMemberIds,
                    tempApproverID = null,
                    Allow_Template_Overrides = null,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now(),
                    remainingBalance = totalBudget // Initialize remaining balance with total budget
                )
                
                android.util.Log.d("BusinessHeadViewModel", "📦 Project object created, sending to repository")
                
                val result = projectRepository.createProject(project, customerId)
                if (result.isSuccess) {
                    val createdProjectId = result.getOrNull()
                    android.util.Log.d("BusinessHeadViewModel", "✅ Project created successfully with ID: $createdProjectId")
                    
                    // Create project object with the generated ID for notifications
                    val projectWithId = project.copy(id = createdProjectId)
                    
                    // Save phases if provided - Note: phases parameter is List<Phase> for backward compatibility
                    // But we'll need to handle PhaseDraft separately if needed
                    if (!phases.isNullOrEmpty() && !createdProjectId.isNullOrEmpty()) {
                        android.util.Log.d("BusinessHeadViewModel", "📋 Saving ${phases.size} phases to project: $createdProjectId")
                        phases.forEachIndexed { idx, phase ->
                            val phaseBudget = phase.departments.values.sum()
                            android.util.Log.d("BusinessHeadViewModel", "  Phase ${idx + 1}: ${phase.phaseName} (Budget: $phaseBudget)")
                            phase.departments.forEach { (dept, budget) ->
                                android.util.Log.d("BusinessHeadViewModel", "    - $dept: $budget")
                            }
                        }
                        
                        val phaseResult = projectRepository.addPhasesToProject(customerId, createdProjectId, phases)
                        if (phaseResult.isSuccess) {
                            android.util.Log.d("BusinessHeadViewModel", "✅ Successfully saved ${phases.size} phases")
                        } else {
                            android.util.Log.e("BusinessHeadViewModel", "❌ Failed to add phases: ${phaseResult.exceptionOrNull()?.message}")
                            _error.value = "Project created but failed to save phases: ${phaseResult.exceptionOrNull()?.message}"
                        }
                    } else {
                        android.util.Log.w("BusinessHeadViewModel", "⚠️ No phases to save or project ID is null")
                    }
                    
                    // Send notifications to approvers about the new project
                    if (createdProjectId != null) {
                        sendNewProjectNotifications(projectWithId, managerId, teamMemberIds)
                    }
                    
                    // Reload all existing phases to include the newly created project's phases
                    loadAllExistingPhases()
                    
                    _successMessage.value = "Project created successfully!"
                    clearProjectForm()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to create project"
                    android.util.Log.e("BusinessHeadViewModel", "❌ Project creation failed: $errorMsg")
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error creating project: ${e.message}", e)
                _error.value = "Error creating project: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun addDepartmentBudget(departmentName: String, budget: Double) {
        val currentList = _departmentBudgets.value.toMutableList()
        
        // Check if department already exists
        val existingIndex = currentList.indexOfFirst { it.departmentName == departmentName }
        if (existingIndex >= 0) {
            currentList[existingIndex] = DepartmentBudget(departmentName, budget)
        } else {
            currentList.add(DepartmentBudget(departmentName, budget))
        }
        
        _departmentBudgets.value = currentList
        calculateTotalAllocated()
    }
    
    fun removeDepartmentBudget(departmentName: String) {
        val currentList = _departmentBudgets.value.toMutableList()
        currentList.removeAll { it.departmentName == departmentName }
        _departmentBudgets.value = currentList
        calculateTotalAllocated()
    }
    
    fun updateTotalBudget(budget: Double) {
        _totalBudget.value = budget
    }
    
    private fun calculateTotalAllocated() {
        val total = _departmentBudgets.value.sumOf { it.allocatedBudget }
        _totalAllocated.value = total
    }
    
    private fun generateProjectCode(projectName: String): String {
        val words = projectName.split(" ")
        return if (words.size >= 2) {
            "${words[0].take(2).uppercase()}${words[1].take(1).uppercase()}"
        } else {
            projectName.take(3).uppercase()
        }
    }
    
    private fun clearProjectForm() {
        _departmentBudgets.value = emptyList()
        _totalBudget.value = 0.0
        _totalAllocated.value = 0.0
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun clearReviewData() {
        _reviewData.value = ReviewProjectData()
    }

    suspend fun isProjectNameAlreadyUsedForCurrentCustomer(
        projectName: String,
        excludeProjectId: String? = null
    ): Boolean {
        return try {
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val customerId = firebaseUser?.uid
            if (customerId.isNullOrBlank()) {
                false
            } else {
                projectRepository.isProjectNameAlreadyUsed(
                    customerId = customerId,
                    projectName = projectName,
                    excludeProjectId = excludeProjectId
                )
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun refreshUsers() {
        loadUsers()
    }
    
    // Preload project data instantly (for smooth navigation)
    fun preloadProjectForEdit(project: Project) {
        _editingProject.value = project
        _isEditMode.value = true
        
        // Load project data into form
        _totalBudget.value = project.budget
        
        // Convert department budgets map to list
        val deptBudgets = project.departmentBudgets?.map { (dept, budget) ->
            DepartmentBudget(dept, budget)
        } ?: emptyList()
        _departmentBudgets.value = deptBudgets
        calculateTotalAllocated()
        
        android.util.Log.d("BusinessHeadViewModel", "✅ Preloaded project for editing: ${project.name}")
    }
    
    // Project Edit Methods
    fun loadProjectForEdit(projectId: String) {
        // If project is already loaded and matches, don't reload
        if (_editingProject.value?.id == projectId) {
            android.util.Log.d("BusinessHeadViewModel", "✅ Project already loaded, skipping reload")
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val project = projectRepository.getProjectById(projectId)
                if (project != null) {
                    _editingProject.value = project
                    _isEditMode.value = true
                    
                    // Load project data into form
                    _totalBudget.value = project.budget
                    
                    // Convert department budgets map to list
                    val deptBudgets = project.departmentBudgets?.map { (dept, budget) ->
                        DepartmentBudget(dept, budget)
                    } ?: emptyList()
                    _departmentBudgets.value = deptBudgets
                    calculateTotalAllocated()
                    
                    android.util.Log.d("BusinessHeadViewModel", "✅ Loaded project for editing: ${project.name}")
                } else {
                    _error.value = "Project not found"
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error loading project for edit: ${e.message}")
                _error.value = "Error loading project: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Get phases for a project (for resubmission)
    suspend fun getPhasesForProject(projectId: String): List<Phase> {
        return try {
            projectRepository.getPhasesForProject(projectId)
        } catch (e: Exception) {
            android.util.Log.e("BusinessHeadViewModel", "❌ Error getting phases: ${e.message}")
            emptyList()
        }
    }
    
    // Get phases with departments from subcollection (preferred method)
    suspend fun getPhasesWithDepartmentsForProject(projectId: String): List<PhaseWithDepartments> {
        return try {
            projectRepository.getPhasesWithDepartmentsForProject(projectId)
        } catch (e: Exception) {
            android.util.Log.e("BusinessHeadViewModel", "❌ Error getting phases with departments: ${e.message}")
            emptyList()
        }
    }
    
    // Load customer business type
    fun loadBusinessType() {
        viewModelScope.launch {
            try {
                val businessType = projectRepository.getCustomerBusinessType()
                _businessType.value = businessType
                android.util.Log.d("BusinessHeadViewModel", "✅ Business type loaded: $businessType")
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error loading business type: ${e.message}")
                _businessType.value = null
            }
        }
    }
    
    fun updateProject(
        projectId: String,
        projectName: String,
        description: String,
        client: String,
        clientPrimaryNumber: String? = null,
        clientSecondaryNumber: String? = null,
        location: String,
        currency: String,
        startDate: Timestamp,
        endDate: Timestamp?,
        handoverDate: String? = null,
        maintenanceDate: String? = null,
        totalBudget: Double,
        managerId: String? = null, // Now nullable - using department-specific assignments
        teamMemberIds: List<String> = emptyList(), // Now optional - using department-specific assignments
        departmentBudgets: List<DepartmentBudget>,
        categories: List<String> = emptyList(),
        phases: List<Phase> = emptyList(),
        phaseDrafts: List<PhaseDraft> = emptyList(), // Full PhaseDraft list for resubmission (includes contractor mode, line items)
        status: String? = null,
        isSuspended: Boolean? = null,
        suspendedDate: String? = null,
        suspensionReason: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                android.util.Log.d("BusinessHeadViewModel", "🔄 Updating project: $projectName")
                
                // Store original project data for comparison
                val originalProject = _editingProject.value
                
                // Validate inputs
                if (projectName.isBlank()) {
                    throw Exception("Project name cannot be empty")
                }
                
                // Manager and team members are now optional - using department-specific assignments
                // Validation removed for managerId and teamMemberIds

                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val customerId = firebaseUser?.uid
                if (customerId.isNullOrBlank()) {
                    _error.value = "User not authenticated. Please sign in again."
                    _isLoading.value = false
                    return@launch
                }

                val isDuplicateName = projectRepository.isProjectNameAlreadyUsed(
                    customerId = customerId,
                    projectName = projectName,
                    excludeProjectId = projectId
                )
                if (isDuplicateName) {
                    _error.value = "Project name already exists. Please select a different name."
                    _isLoading.value = false
                    return@launch
                }
                
                val budgetMap = departmentBudgets.associate { 
                    it.departmentName to it.allocatedBudget 
                }
                
                // Format dates as "dd/MM/yyyy" strings to match iOS model
                val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                var startDateString = startDate?.toDate()?.let { dateFormatter.format(it) }
                val endDateString = endDate?.toDate()?.let { dateFormatter.format(it) }

                // Preserve existing start date for partial updates when incoming value is unavailable
                if (startDateString.isNullOrBlank()) {
                    startDateString = _editingProject.value?.startDate ?: _editingProject.value?.plannedDate
                }
                
                // Get today's date for comparison
                val today = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.time
                val todayString = dateFormatter.format(today)
                
                // Initialize date variables - will be updated based on finalStatus
                var finalHandoverDate = handoverDate ?: _editingProject.value?.handoverDate
                var finalMaintenanceDate = maintenanceDate ?: _editingProject.value?.maintenanceDate

                // Preserve manager/team values when UI sends empty values during partial edits
                val effectiveManagerId = managerId?.takeIf { it.isNotBlank() }
                    ?: _editingProject.value?.managerId?.takeIf { it.isNotBlank() }

                val effectiveTeamMembers = if (teamMemberIds.isNotEmpty()) {
                    teamMemberIds.filter { it.isNotBlank() }.distinct()
                } else {
                    _editingProject.value?.teamMembers?.filter { it.isNotBlank() }?.distinct() ?: emptyList()
                }

                val effectiveBudget = if (
                    totalBudget <= 0.0 && (_editingProject.value?.budget ?: 0.0) > 0.0
                ) {
                    _editingProject.value?.budget ?: totalBudget
                } else {
                    totalBudget
                }
                
                // Ensure createdAt is non-null Timestamp
                val createdAt = _editingProject.value?.createdAt ?: Timestamp.now()
                
                // Determine final status based on suspension logic
                val finalStatus = when {
                    status == "REVIEW REJECTED" -> "IN REVIEW"
                    isSuspended == true && suspendedDate != null && suspensionReason != null -> {
                        // Check if suspension date has passed
                        try {
                            val suspendedDateParsed = dateFormatter.parse(suspendedDate)
                            val today = java.util.Calendar.getInstance().apply {
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }.time
                            
                            val suspendedDateAtMidnight = java.util.Calendar.getInstance().apply {
                                time = suspendedDateParsed ?: java.util.Date()
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }.time

                            // If suspension date has passed, set to ACTIVE and clear suspension
                            if (today.time > suspendedDateAtMidnight.time) {
                                "ACTIVE"
                            } else {
                                "SUSPENDED"
                            }
                        } catch (e: Exception) {
                            "SUSPENDED" // Default to SUSPENDED if date parsing fails
                        }
                    }
                    isSuspended == false -> {
                        // If suspension is disabled, never persist SUSPENDED as final status.
                        // UI often sends back current status, which may still be "SUSPENDED".
                        val requestedStatus = status?.trim().orEmpty()
                        val previousStatus = _editingProject.value?.status?.trim().orEmpty()

                        when {
                            requestedStatus.isNotEmpty() && !requestedStatus.equals("SUSPENDED", ignoreCase = true) -> requestedStatus
                            previousStatus.isNotEmpty() && !previousStatus.equals("SUSPENDED", ignoreCase = true) -> previousStatus
                            else -> "ACTIVE"
                        }
                    }
                    else -> status ?: _editingProject.value?.status ?: "DRAFT"
                }
                
                // Clear suspension data if suspension is disabled or date has passed
                val finalIsSuspended = if (isSuspended == true && suspendedDate != null && suspensionReason != null) {
                    try {
                        val suspendedDateParsed = dateFormatter.parse(suspendedDate)
                        val today = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.time
                        
                        val suspendedDateAtMidnight = java.util.Calendar.getInstance().apply {
                            time = suspendedDateParsed ?: java.util.Date()
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.time

                        today.time <= suspendedDateAtMidnight.time
                    } catch (e: Exception) {
                        true
                    }
                } else {
                    false
                }
                
                val finalSuspensionDate = if (finalIsSuspended) suspendedDate else null
                val finalSuspensionReason = if (finalIsSuspended) suspensionReason else null
                
                // Handle date updates based on final status change (only if not SUSPENDED)
                // Note: Dates should already be updated in the UI, but this serves as a backup
                val originalProjectStatus = originalProject?.status
                val statusChanged = originalProjectStatus != null && originalProjectStatus != finalStatus
                
                if (finalStatus.uppercase() != "SUSPENDED" && statusChanged) {
                    when (finalStatus.uppercase()) {
                        "ACTIVE" -> {
                            // If plannedDate (startDate) > current date, set plannedDate = current date
                            if (startDateString != null) {
                                try {
                                    val startDateParsed = dateFormatter.parse(startDateString)
                                    val startDateAtMidnight = java.util.Calendar.getInstance().apply {
                                        time = startDateParsed ?: java.util.Date()
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                    }.time
                                    
                                    if (startDateParsed != null && startDateAtMidnight.after(today)) {
                                        startDateString = todayString
                                        android.util.Log.d("BusinessHeadViewModel", "📅 Updated plannedDate to current date for ACTIVE status: $todayString")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("BusinessHeadViewModel", "❌ Error parsing startDate: ${e.message}", e)
                                }
                            }
                        }
                        "MAINTENANCE" -> {
                            // Always set handoverDate to today's date when status is MAINTENANCE
                            finalHandoverDate = todayString
                            android.util.Log.d("BusinessHeadViewModel", "📅 Updated handoverDate to current date for MAINTENANCE status: $todayString")
                            
                            // If changing from COMPLETED to MAINTENANCE, set maintenance date to 1 month from now
                            if (originalProjectStatus == "COMPLETED") {
                                val calendar = java.util.Calendar.getInstance()
                                calendar.time = today
                                calendar.add(java.util.Calendar.MONTH, 1) // Add 1 month
                                val oneMonthFromNow = calendar.time
                                finalMaintenanceDate = dateFormatter.format(oneMonthFromNow)
                                android.util.Log.d("BusinessHeadViewModel", "📅 Updated maintenanceDate to 1 month from now (COMPLETED -> MAINTENANCE): ${finalMaintenanceDate}")
                            }
                        }
                        "COMPLETED" -> {
                            // Always set maintenanceDate to today's date when status is COMPLETED
                            // Handover Date should remain unchanged (fixed according to latest phase's end date)
                            finalMaintenanceDate = todayString
                            android.util.Log.d("BusinessHeadViewModel", "📅 Updated maintenanceDate to current date for COMPLETED status: $todayString")
                        }
                    }
                }
                
                android.util.Log.d("BusinessHeadViewModel", "📦 Suspension logic: isSuspended=$isSuspended, finalIsSuspended=$finalIsSuspended, finalStatus=$finalStatus")
                
                // Preserve existing phone numbers when the caller does not supply new values
                val effectiveClientPrimaryNumber = clientPrimaryNumber
                    ?: _editingProject.value?.clientPrimaryNumber
                val effectiveClientSecondaryNumber = clientSecondaryNumber
                    ?: _editingProject.value?.clientSecondaryNumber

                val updatedProject = Project(
                    id = projectId,
                    name = projectName,
                    description = description,
                    client = client,
                    clientPrimaryNumber = effectiveClientPrimaryNumber,
                    clientSecondaryNumber = effectiveClientSecondaryNumber,
                    location = location,
                    currency = currency,
                    budget = effectiveBudget,
                    status = finalStatus,
                    startDate = startDateString,
                    endDate = endDateString,
                    handoverDate = finalHandoverDate,
                    maintenanceDate = finalMaintenanceDate,
                    teamMembers = effectiveTeamMembers,
                    tempApproverID = _editingProject.value?.tempApproverID,
                    Allow_Template_Overrides = _editingProject.value?.Allow_Template_Overrides,
                    createdAt = createdAt,
                    updatedAt = Timestamp.now(),
                    // Legacy fields for backward compatibility
                    spent = _editingProject.value?.spent ?: 0.0,
                    managerId = effectiveManagerId, // Can be null - using department-specific assignments
                    approverIds = _editingProject.value?.approverIds,
                    BusinessHeadIds = _editingProject.value?.BusinessHeadIds,
                    code = _editingProject.value?.code ?: generateProjectCode(projectName),
                    departmentBudgets = budgetMap,
                    categories = categories,
                    rejectionReason = if (finalStatus == "IN REVIEW") null else _editingProject.value?.rejectionReason, // Clear rejection reason when resubmitting
                    // Suspension fields
                    isSuspended = if (finalIsSuspended) true else null,
                    suspendedDate = finalSuspensionDate,
                    suspensionReason = finalSuspensionReason
                )
                
                android.util.Log.d("BusinessHeadViewModel", "📦 Updated project object created, sending to repository")
                
                val result = projectRepository.updateProject(projectId, updatedProject)
                if (result.isSuccess) {
                    android.util.Log.d("BusinessHeadViewModel", "✅ Project updated successfully")

                    // Replace phases/departments subcollections if phaseDrafts are provided (resubmission flow)
                    if (phaseDrafts.isNotEmpty()) {
                        android.util.Log.d("BusinessHeadViewModel", "📋 Replacing ${phaseDrafts.size} phases with departments for resubmission")
                        val phaseResult = projectRepository.replacePhasesWithDepartmentsForProject(
                            customerId = customerId,
                            projectId = projectId,
                            phaseDrafts = phaseDrafts
                        )
                        if (phaseResult.isSuccess) {
                            android.util.Log.d("BusinessHeadViewModel", "✅ Phases and departments replaced successfully")
                        } else {
                            android.util.Log.e("BusinessHeadViewModel", "⚠️ Phase replacement failed: ${phaseResult.exceptionOrNull()?.message}")
                        }
                    }

                    // Send notifications for project changes
                    if (originalProject != null) {
                        sendProjectChangeNotifications(originalProject, updatedProject)
                    }
                    
                    // Reload the project to get updated data from backend
                    val reloadedProject = projectRepository.getProjectById(projectId)
                    if (reloadedProject != null) {
                        _editingProject.value = reloadedProject
                        android.util.Log.d("BusinessHeadViewModel", "🔄 Reloaded project after update - status: ${reloadedProject.status}, startDate: ${reloadedProject.startDate}, handoverDate: ${reloadedProject.handoverDate}, maintenanceDate: ${reloadedProject.maintenanceDate}, isSuspended: ${reloadedProject.isSuspended}, suspendedDate: ${reloadedProject.suspendedDate}, suspensionReason: ${reloadedProject.suspensionReason}")
                    }
                    
                    _successMessage.value = "Project updated successfully!"
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to update project"
                    android.util.Log.e("BusinessHeadViewModel", "❌ Project update failed: $errorMsg")
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error updating project: ${e.message}", e)
                _error.value = "Error updating project: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update all phase dates when project status changes to COMPLETED
     * Phase end dates in future → set to yesterday
     * Phase start dates in future → set to 2 days back
     * Matches guide's updatePhasesForCompletedStatus logic
     */
    fun updatePhasesForCompletedStatus(
        projectId: String,
        yesterdayDate: String,
        twoDaysBackDate: String
    ) {
        viewModelScope.launch {
            try {
                android.util.Log.d("BusinessHeadViewModel", "🔄 Updating phases for COMPLETED status: projectId=$projectId")
                android.util.Log.d("BusinessHeadViewModel", "📅 Yesterday date: $yesterdayDate, Two days back: $twoDaysBackDate")
                
                // Get all phases for this project
                val phases = projectRepository.getPhasesForProject(projectId)
                val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val today = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.time
                
                // Update each phase
                for (phase in phases) {
                    val phaseUpdateData = mutableMapOf<String, Any>()
                    var needsUpdate = false
                    
                    // Check and update end date if in future
                    phase.endDate?.let { endDateStr ->
                        try {
                            val endDate = dateFormatter.parse(endDateStr)
                            if (endDate != null) {
                                val phaseEnd = java.util.Calendar.getInstance().apply {
                                    time = endDate
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }.time
                                
                                if (phaseEnd.after(today)) {
                                    phaseUpdateData["endDate"] = yesterdayDate
                                    needsUpdate = true
                                    android.util.Log.d("BusinessHeadViewModel", "📅 Phase ${phase.id} endDate will be updated to $yesterdayDate")
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("BusinessHeadViewModel", "⚠️ Error parsing phase endDate: ${e.message}")
                        }
                    }
                    
                    // Check and update start date if in future
                    phase.startDate?.let { startDateStr ->
                        try {
                            val startDate = dateFormatter.parse(startDateStr)
                            if (startDate != null) {
                                val phaseStart = java.util.Calendar.getInstance().apply {
                                    time = startDate
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }.time
                                
                                if (phaseStart.after(today)) {
                                    phaseUpdateData["startDate"] = twoDaysBackDate
                                    needsUpdate = true
                                    android.util.Log.d("BusinessHeadViewModel", "📅 Phase ${phase.id} startDate will be updated to $twoDaysBackDate")
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("BusinessHeadViewModel", "⚠️ Error parsing phase startDate: ${e.message}")
                        }
                    }
                    
                    // Update phase if needed
                    if (needsUpdate) {
                        phaseUpdateData["updatedAt"] = Timestamp.now()
                        val result = projectRepository.updatePhaseDetails(
                            projectId = projectId,
                            phaseId = phase.id,
                            phaseName = phase.phaseName,
                            startDate = phaseUpdateData["startDate"] as? String ?: phase.startDate,
                            endDate = phaseUpdateData["endDate"] as? String ?: phase.endDate
                        )
                        
                        if (result.isSuccess) {
                            android.util.Log.d("BusinessHeadViewModel", "✅ Phase ${phase.id} updated successfully")
                        } else {
                            android.util.Log.e("BusinessHeadViewModel", "❌ Failed to update phase ${phase.id}: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }
                
                android.util.Log.d("BusinessHeadViewModel", "✅ Completed updating phases for COMPLETED status")
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error updating phases for completed status: ${e.message}", e)
            }
        }
    }
    
    fun updateProjectStatus(projectId: String, newStatus: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                android.util.Log.d("BusinessHeadViewModel", "🔄 Updating project status: $projectId to $newStatus")

                val result = projectRepository.updateProjectStatus(projectId, newStatus)
                if (result.isSuccess) {
                    android.util.Log.d("BusinessHeadViewModel", "✅ Project status updated successfully")
                    
                    // Send status change notifications
                    sendProjectStatusChangeNotifications(projectId, newStatus)
                    
                    _successMessage.value = "Project status updated to $newStatus!"
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to update project status"
                    android.util.Log.e("BusinessHeadViewModel", "❌ Project status update failed: $errorMsg")
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error updating project status: ${e.message}", e)
                _error.value = "Error updating project status: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun sendProjectStatusChangeNotifications(projectId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                // Get project details
                val project = projectRepository.getProjectById(projectId)
                if (project == null) {
                    android.util.Log.e("BusinessHeadViewModel", "❌ Project not found for status notifications: $projectId")
                    return@launch
                }
                
                // Get all users to determine their roles
                val allUsers = authRepository.getAllUsers()
                val currentUser = authRepository.getCurrentUser()
                val changedBy = currentUser?.name ?: "Production Head"
                
                val statusMessage = when (newStatus.uppercase()) {
                    "ACTIVE" -> "Project has been activated"
                    "PAUSED" -> "Project has been paused"
                    "COMPLETED" -> "Project has been marked as completed"
                    "CANCELLED" -> "Project has been cancelled"
                    else -> "Project status changed to $newStatus"
                }
                
                // Notification creation removed - notifications are now handled by FCM only
                android.util.Log.d("BusinessHeadViewModel", "📧 Status change notifications will be sent via FCM (not stored in Firestore)")
                
                android.util.Log.d("BusinessHeadViewModel", "✅ Sent status change notifications for project: ${project.name}")
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error sending status change notifications: ${e.message}")
            }
        }
    }



    private fun sendNewProjectNotifications(
        project: Project,
        managerId: String,
        teamMemberIds: List<String>
    ) {
        viewModelScope.launch {
            try {
                // Get all users to determine their roles
                val allUsers = authRepository.getAllUsers()
                
                // Notification creation removed - notifications are now handled by FCM only
                android.util.Log.d("BusinessHeadViewModel", "📧 New project notifications will be sent via FCM (not stored in Firestore)")
                
                android.util.Log.d("BusinessHeadViewModel", "✅ Sent new project notifications for project: ${project.name}")
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error sending new project notifications: ${e.message}")
            }
        }
    }

    private fun sendProjectChangeNotifications(originalProject: Project, updatedProject: Project) {
        viewModelScope.launch {
            try {
                // Get all users to determine their roles
                val allUsers = authRepository.getAllUsers()
                
                // Detect what changes were made
                val changes = mutableListOf<String>()
                
                if (originalProject.name != updatedProject.name) {
                    changes.add("Project name changed from '${originalProject.name}' to '${updatedProject.name}'")
                }
                
                if (originalProject.description != updatedProject.description) {
                    changes.add("Project description updated")
                }
                
                if (originalProject.budget != updatedProject.budget) {
                    changes.add("Budget changed from ₹${originalProject.budget} to ₹${updatedProject.budget}")
                }
                
                if (originalProject.startDate != updatedProject.startDate) {
                    changes.add("Start date updated")
                }
                
                if (originalProject.endDate != updatedProject.endDate) {
                    changes.add("End date updated")
                }
                
                if (originalProject.managerId != updatedProject.managerId) {
                    changes.add("Project manager changed")
                }
                
                if (originalProject.teamMembers != updatedProject.teamMembers) {
                    changes.add("Team members updated")
                }
                
                if (originalProject.departmentBudgets != updatedProject.departmentBudgets) {
                    changes.add("Department budgets updated")
                }
                
                if (originalProject.categories != updatedProject.categories) {
                    changes.add("Project categories updated")
                }
                
                // If no changes detected, don't send notifications
                if (changes.isEmpty()) {
                    android.util.Log.d("BusinessHeadViewModel", "ℹ️ No significant changes detected, skipping notifications")
                    return@launch
                }
                
                val changeDescription = changes.joinToString(". ")
                val currentUser = authRepository.getCurrentUser()
                val changedBy = currentUser?.name ?: "Production Head"
                
                // Notification creation removed - notifications are now handled by FCM only
                android.util.Log.d("BusinessHeadViewModel", "📧 Project change notifications will be sent via FCM (not stored in Firestore)")
                
                // Send special budget change notifications to approvers if budget was modified
                if (originalProject.budget != updatedProject.budget) {
                    sendBudgetChangeNotifications(updatedProject, originalProject.budget, updatedProject.budget, changedBy)
                }
                
                android.util.Log.d("BusinessHeadViewModel", "✅ Sent change notifications for project: ${updatedProject.name}")
                android.util.Log.d("BusinessHeadViewModel", "📝 Changes detected: $changeDescription")
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error sending change notifications: ${e.message}")
            }
        }
    }
    
    private fun sendBudgetChangeNotifications(
        project: Project,
        oldBudget: Double,
        newBudget: Double,
        changedBy: String
    ) {
        viewModelScope.launch {
            try {
                val allUsers = authRepository.getAllUsers()
                val budgetChangeMessage = "Budget changed from ₹$oldBudget to ₹$newBudget"
                
                // Notification creation removed - notifications are now handled by FCM only
                android.util.Log.d("BusinessHeadViewModel", "📧 Budget change notifications will be sent via FCM (not stored in Firestore)")
                
                android.util.Log.d("BusinessHeadViewModel", "💰 Sent budget change notifications for project: ${project.name}")
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error sending budget change notifications: ${e.message}")
            }
        }
    }

    fun clearEditState() {
        _editingProject.value = null
        _isEditMode.value = false
        clearProjectForm()
    }
    
    // Can delete project state
    private val _canDeleteProject = MutableStateFlow<Boolean?>(null)
    val canDeleteProject: StateFlow<Boolean?> = _canDeleteProject.asStateFlow()
    
    /**
     * Check if a project can be deleted (DRAFT status with no expenses)
     */
    fun checkCanDeleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                val canDelete = projectRepository.canDeleteProject(projectId)
                _canDeleteProject.value = canDelete
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "Error checking if project can be deleted: ${e.message}")
                _canDeleteProject.value = false
            }
        }
    }
    
    /**
     * Delete a project (only if in DRAFT status with no expenses)
     */
    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = projectRepository.deleteProject(projectId)
                result.fold(
                    onSuccess = {
                        _successMessage.value = "Project deleted successfully"
                        _editingProject.value = null
                        _canDeleteProject.value = false
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to delete project"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error deleting project: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Resubmit a declined project by deleting the old project and creating a new one with updated data
     * This is used when a project is DECLINED/REVIEW REJECTED and needs to be resubmitted
     */
    fun resubmitDeclinedProjectAsNew(
        oldProjectId: String,
        projectName: String,
        description: String,
        client: String,
        clientPrimaryNumber: String? = null,
        clientSecondaryNumber: String? = null,
        location: String,
        currency: String,
        startDate: Timestamp,
        endDate: Timestamp?,
        handoverDate: String? = null,
        maintenanceDate: String? = null,
        totalBudget: Double,
        managerId: String? = null,
        teamMemberIds: List<String> = emptyList(),
        categories: List<String> = emptyList(),
        phaseDrafts: List<PhaseDraft> = emptyList(),
        departmentUserAssignments: Map<String, String> = emptyMap(),
        departmentApproverAssignments: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                android.util.Log.d("BusinessHeadViewModel", "🔄 Resubmitting declined project as new: $projectName")
                
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val customerId = firebaseUser?.uid
                if (customerId.isNullOrBlank()) {
                    _error.value = "User not authenticated. Please sign in again."
                    _isLoading.value = false
                    return@launch
                }
                
                // Step 1: Get the old project data (for reference and to get any CustomTemporaryApproverRelation IDs)
                val oldProject = projectRepository.getProjectById(oldProjectId)
                android.util.Log.d("BusinessHeadViewModel", "✅ Retrieved old project: ${oldProject?.name}")
                
                // Step 2: Delete the old project (this will also delete all its phases, departments, expenses, etc.)
                android.util.Log.d("BusinessHeadViewModel", "🗑️ Deleting old declined project: $oldProjectId")
                val deleteResult = projectRepository.deleteProject(oldProjectId)
                if (deleteResult.isSuccess) {
                    android.util.Log.d("BusinessHeadViewModel", "✅ Old project deleted successfully")
                } else {
                    throw Exception("Failed to delete old project: ${deleteResult.exceptionOrNull()?.message}")
                }
                
                // Step 3: Delete CustomerTemporaryApproverRelation entries for the old project
                android.util.Log.d("BusinessHeadViewModel", "🔗 Cleaning up CustomTemporaryApproverRelation for old project")
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val relationSnapshot = firestore.collection("CustomerTemporaryApproverRelation")
                        .whereEqualTo("projectid", oldProjectId)
                        .get()
                        .await()
                    
                    val batch = firestore.batch()
                    relationSnapshot.documents.forEach { doc ->
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                    
                    android.util.Log.d("BusinessHeadViewModel", "✅ Deleted ${relationSnapshot.documents.size} CustomerTemporaryApproverRelation entries")
                } catch (e: Exception) {
                    android.util.Log.w("BusinessHeadViewModel", "⚠️ Failed to clean up CustomTemporaryApproverRelation: ${e.message}")
                    // Don't fail the entire operation
                }
                
                // Step 4: Create new project with the updated data
                android.util.Log.d("BusinessHeadViewModel", "✨ Creating new project from declined submission")
                
                // Format dates
                val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val startDateString = startDate?.toDate()?.let { dateFormatter.format(it) }
                val endDateString = endDate?.toDate()?.let { dateFormatter.format(it) }
                
                // Create new project object
                val newProject = Project(
                    id = null, // Will be generated by Firestore
                    name = projectName,
                    description = description,
                    client = client,
                    clientPrimaryNumber = clientPrimaryNumber,
                    clientSecondaryNumber = clientSecondaryNumber,
                    location = location,
                    currency = currency,
                    budget = totalBudget,
                    status = "IN REVIEW",
                    startDate = startDateString,
                    endDate = endDateString,
                    handoverDate = handoverDate,
                    maintenanceDate = maintenanceDate,
                    teamMembers = teamMemberIds,
                    departmentUserAssignments = departmentUserAssignments,
                    departmentApproverAssignments = departmentApproverAssignments,
                    tempApproverID = null,
                    Allow_Template_Overrides = null,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now(),
                    managerId = managerId,
                    remainingBalance = totalBudget
                )
                
                // Create the new project
                val createResult = projectRepository.createProject(newProject, customerId)
                if (createResult.isFailure) {
                    throw Exception("Failed to create new project: ${createResult.exceptionOrNull()?.message}")
                }
                
                val newProjectId = createResult.getOrNull()
                android.util.Log.d("BusinessHeadViewModel", "✅ New project created successfully with ID: $newProjectId")
                
                // Step 5: Save phases and departments for the new project
                if (phaseDrafts.isNotEmpty() && !newProjectId.isNullOrEmpty()) {
                    android.util.Log.d("BusinessHeadViewModel", "📋 Saving ${phaseDrafts.size} phases with departments to new project")
                    
                    val phaseResult = projectRepository.addPhasesWithDepartmentsToProject(
                        customerId, 
                        newProjectId, 
                        phaseDrafts
                    )
                    if (phaseResult.isSuccess) {
                        android.util.Log.d("BusinessHeadViewModel", "✅ Phases and departments saved successfully")
                    } else {
                        android.util.Log.e("BusinessHeadViewModel", "⚠️ Failed to save phases: ${phaseResult.exceptionOrNull()?.message}")
                        throw Exception("Failed to save phases: ${phaseResult.exceptionOrNull()?.message}")
                    }
                    
                    // Update User_Customer_Project_Phase_Relation
                    val relationResult = projectRepository.updateUserCustomerProjectPhaseRelation(
                        customerId = customerId,
                        projectId = newProjectId,
                        phaseDrafts = phaseDrafts,
                        departmentUserAssignments = departmentUserAssignments,
                        departmentApproverAssignments = departmentApproverAssignments
                    )
                    if (relationResult.isSuccess) {
                        android.util.Log.d("BusinessHeadViewModel", "✅ User_Customer_Project_Phase_Relation updated for new project")
                    } else {
                        android.util.Log.w("BusinessHeadViewModel", "⚠️ Failed to update User_Customer_Project_Phase_Relation: ${relationResult.exceptionOrNull()?.message}")
                    }
                }
                
                // Step 6: Load the new project into memory
                val projectWithId = newProject.copy(id = newProjectId)
                _editingProject.value = projectWithId
                
                loadAllExistingPhases()
                _successMessage.value = "Project resubmitted successfully! New project is now IN REVIEW."
                projectIdForResubmission = null // Clear the resubmission ID
                
                android.util.Log.d("BusinessHeadViewModel", "✅ Declined project resubmitted as new project with ID: $newProjectId")
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error resubmitting declined project: ${e.message}", e)
                _error.value = "Error resubmitting project: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Delegation Management
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Appends a new [DepartmentTemporaryApproverEntry] to the [DepartmentTemporaryApprover] list
     * in the project document at customers/{customerId}/projects/{projectId}.
     * 
     * As per TempAPproverFlow.md, this writes to 3 locations:
     * 1. Project document: DepartmentTemporaryApprover array + tempApproverID field
     * 2. tempApprover subcollection: customers/{customerId}/projects/{projectId}/tempApprover/{autoDocId}
     * 3. Root relation collection: CustomerTemporaryApproverRelation/{autoDocId}
     */
    fun saveDepartmentTemporaryApprover(projectId: String, approver: DepartmentTemporaryApproverEntry) {
        viewModelScope.launch {
            try {
                val customerId = projectRepository.getCurrentUserCustomerId()
                    ?: throw Exception("Could not determine customer ID")

                val firestore = FirebaseFirestore.getInstance()
                val projectDocRef = firestore
                    .collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(projectId)

                // Format dates as yyyy-MM-dd strings for consistency with iOS (TempAPproverFlow.md line 60)
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val startDateStr = approver.startDate?.let { dateFormat.format(it) } ?: ""
                val endDateStr = approver.endDate?.let { dateFormat.format(it) } ?: ""

                // 1) Update project document with DepartmentTemporaryApprover array entry + tempApproverID
                // (TempAPproverFlow.md lines 46-64)
                val approverMap = mapOf(
                    "phone" to approver.phone,
                    "departmentName" to approver.departmentName.trim(),
                    "startDate" to startDateStr,
                    "endDate" to endDateStr,
                    "isAccepted" to "true",  // Always "true" as per TempAPproverFlow.md line 62
                    "status" to "accepted"    // Always "accepted" as per line 63
                )
                
                projectDocRef.update(
                    mapOf(
                        "DepartmentTemporaryApprover" to FieldValue.arrayUnion(approverMap),
                        "tempApproverID" to approver.phone  // TempAPproverFlow.md line 53
                    )
                ).await()

                // 2) Add to tempApprover subcollection (compatibility path)
                // (TempAPproverFlow.md lines 66-83)
                projectDocRef.collection("tempApprover").add(
                    mapOf(
                        "approvedExpense" to emptyList<String>(),  // Line 73
                        "approverId" to approver.phone,             // Line 74
                        "departmentName" to approver.departmentName.trim(),  // Line 75
                        "startDate" to (approver.startDate ?: com.google.firebase.Timestamp.now().toDate()),  // Line 76 - Date object
                        "endDate" to approver.endDate,              // Line 77 - Date object or null
                        "status" to "accepted",                     // Line 78
                        "updatedAt" to com.google.firebase.Timestamp.now()  // Line 79
                    )
                ).await()

                // 3) Add to root CustomerTemporaryApproverRelation collection
                // (TempAPproverFlow.md lines 85-98)
                firestore.collection("CustomerTemporaryApproverRelation").add(
                    mapOf(
                        "userId" to approver.phone,      // Line 92
                        "customerId" to customerId,      // Line 93
                        "projectid" to projectId         // Line 94 (note lowercase 'id')
                    )
                ).await()

                _successMessage.value = "Delegation saved successfully"
                
                // Refresh the project in memory
                val updated = projectRepository.getProjectById(projectId)
                if (updated != null) _editingProject.value = updated
            } catch (e: Exception) {
                _error.value = "Failed to save delegation: ${e.message}"
            }
        }
    }

    /**
     * Toggles the [isAccepted] flag for the matching [DepartmentTemporaryApproverEntry]
     * (matched by phone + departmentName) in the project's [DepartmentTemporaryApprover] list
     * at customers/{customerId}/projects/{projectId}.
     */
    fun toggleDepartmentTemporaryApproverActive(
        projectId: String,
        approverPhone: String,
        departmentName: String,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            try {
                val customerId = projectRepository.getCurrentUserCustomerId()
                    ?: throw Exception("Could not determine customer ID")

                val project = projectRepository.getProjectById(projectId) ?: return@launch
                val updatedList = project.departmentTemporaryApprover.map { ta ->
                    if (ta.phone == approverPhone && ta.departmentName == departmentName) {
                        ta.copy(isAccepted = isActive)
                    } else ta
                }
                val listAsMap = updatedList.map { ta ->
                    mapOf(
                        "phone" to ta.phone,
                        "startDate" to ta.startDate,
                        "endDate" to ta.endDate,
                        "isAccepted" to ta.isAccepted,
                        "isActive" to ta.isActive,
                        "departmentName" to ta.departmentName
                    )
                }
                FirebaseFirestore.getInstance()
                    .collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(projectId)
                    .update("DepartmentTemporaryApprover", listAsMap)
                    .await()
                // Update local state immediately for responsive UI
                _editingProject.value = project.copy(departmentTemporaryApprover = updatedList)
            } catch (e: Exception) {
                _error.value = "Failed to update delegation: ${e.message}"
            }
        }
    }

    fun updateDepartmentTemporaryApproverDates(
        projectId: String,
        approverPhone: String,
        departmentName: String,
        originalStartDate: java.util.Date?,
        originalEndDate: java.util.Date?,
        newStartDate: java.util.Date,
        newEndDate: java.util.Date?
    ) {
        viewModelScope.launch {
            try {
                val customerId = projectRepository.getCurrentUserCustomerId()
                    ?: throw Exception("Could not determine customer ID")

                val project = projectRepository.getProjectById(projectId)
                    ?: throw Exception("Project not found")

                val updatedList = project.departmentTemporaryApprover.map { ta ->
                    val isTarget = ta.phone == approverPhone &&
                        ta.departmentName == departmentName &&
                        ta.startDate == originalStartDate &&
                        ta.endDate == originalEndDate

                    if (isTarget) {
                        ta.copy(startDate = newStartDate, endDate = newEndDate)
                    } else {
                        ta
                    }
                }

                val listAsMap = updatedList.map { ta ->
                    mapOf(
                        "phone" to ta.phone,
                        "startDate" to ta.startDate,
                        "endDate" to ta.endDate,
                        "isAccepted" to ta.isAccepted,
                        "isActive" to ta.isActive,
                        "departmentName" to ta.departmentName
                    )
                }

                FirebaseFirestore.getInstance()
                    .collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(projectId)
                    .update("DepartmentTemporaryApprover", listAsMap)
                    .await()

                _editingProject.value = project.copy(departmentTemporaryApprover = updatedList)
                _successMessage.value = "Delegation dates updated successfully"
            } catch (e: Exception) {
                _error.value = "Failed to update delegation dates: ${e.message}"
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Department User Management
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loads users grouped by department for the Department User Management screen.
     * Groups active USER-role users and APPROVER-role users into separate maps.
     */
    fun loadUsersGroupedByDepartment() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid ?: return@launch

                android.util.Log.d(
                    "BusinessHeadViewModel",
                    "🔄 Loading department users from CustomerUserRelation for customer: $currentCustomerId"
                )

                val relationsResult = authRepository.fetchCustomerUserRelations(currentCustomerId)
                if (relationsResult.isFailure) {
                    val errorMessage = relationsResult.exceptionOrNull()?.message ?: "Unknown error"
                    android.util.Log.e(
                        "BusinessHeadViewModel",
                        "❌ Failed to fetch CustomerUserRelations for department grouping: $errorMessage"
                    )
                    _departmentUsersMap.value = emptyMap()
                    _departmentApproversMap.value = emptyMap()
                    _availableUsers.value = emptyList()
                    _availableApprovers.value = emptyList()
                    _availableManagers.value = emptyList()
                    _error.value = "Failed to load department users: $errorMessage"
                    return@launch
                }

                val customerUsers = (relationsResult.getOrNull() ?: emptyList())
                    .map { relation -> authRepository.convertRelationToUser(relation) }
                
                val activeUsers = customerUsers.filter { it.role == UserRole.USER && it.isActive }
                val activeApprovers = customerUsers.filter { it.role == UserRole.APPROVER && it.isActive }
                val activeManagers = customerUsers.filter { it.role == UserRole.MANAGER && it.isActive }

                _departmentUsersMap.value = activeUsers
                    .filter { it.department.isNotBlank() }
                    .groupBy { it.department }

                _departmentApproversMap.value = activeApprovers
                    .filter { it.department.isNotBlank() }
                    .groupBy { it.department }

                // Also update available lists
                _availableUsers.value = activeUsers
                _availableApprovers.value = activeApprovers
                _availableManagers.value = activeManagers
                
                android.util.Log.d(
                    "BusinessHeadViewModel",
                    "✅ Loaded users grouped by department from CustomerUserRelation: ${_departmentUsersMap.value.size} dept-users, ${_departmentApproversMap.value.size} dept-approvers"
                )
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error loading users by department: ${e.message}", e)
                _error.value = "Failed to load department users: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Assign a user to a department (when no user is currently assigned).
     * Updates CustomerUserRelation.departmentName for the current customer and refreshes grouped data.
     */
    fun assignUserToDepartment(departmentName: String, userPhone: String, isApprover: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                    ?: throw IllegalStateException("No authenticated customer found")

                val result = authRepository.updateCustomerUserRelationDepartment(
                    phoneNumber = userPhone,
                    customerId = currentCustomerId,
                    departmentName = departmentName
                )
                if (result.isSuccess) {
                    // Keep relation collection in same nested format as new project flow.
                    projectRepository.syncUserCustomerProjectPhaseRelationForAssignment(
                        customerId = currentCustomerId,
                        phoneNumber = userPhone,
                        isApprover = isApprover
                    )

                    _successMessage.value = if (isApprover) "Approver assigned to $departmentName" else "User assigned to $departmentName"
                    loadUsersGroupedByDepartment()
                    loadUsers()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to assign user"
                }
            } catch (e: Exception) {
                _error.value = "Error assigning user: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Replace a user/approver in a department.
     * 1. Batch-update all projects that had the old user for this department
     * 2. Clear old user's departmentName in CustomerUserRelation for current customer
     * 3. Remove current customer key from old user's User_Customer_Project_Phase_Relation doc
     * 4. Set new user's departmentName in CustomerUserRelation for current customer
     * 5. Rebuild new user's User_Customer_Project_Phase_Relation in creation-flow format
     * 6. Refresh UI
     */
    fun replaceDepartmentAssignment(
        departmentName: String,
        oldUserPhone: String,
        newUserPhone: String,
        isApprover: Boolean
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                android.util.Log.d("BusinessHeadViewModel", "🔄 Replacing ${if (isApprover) "approver" else "user"} in $departmentName: $oldUserPhone → $newUserPhone")

                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                    ?: throw IllegalStateException("No authenticated customer found")

                // 1. Batch-update all projects (keep existing behavior)
                val projectResult = projectRepository.replaceDepartmentAssignmentInAllProjects(
                    departmentName = departmentName,
                    oldPhone = oldUserPhone,
                    newPhone = newUserPhone,
                    isApprover = isApprover
                )

                // 2. Do not clear old user in users collection.
                //    Instead clear departmentName in CustomerUserRelation for this customer.
                authRepository.clearCustomerUserRelationDepartment(
                    phoneNumber = oldUserPhone,
                    customerId = currentCustomerId
                )

                // 3. Remove {customerId: {...}} map entry from User_Customer_Project_Phase_Relation/oldPhone
                projectRepository.removeCustomerFromUserCustomerProjectPhaseRelation(
                    phoneNumber = oldUserPhone,
                    customerId = currentCustomerId
                )

                // 4. Set new user's departmentName in CustomerUserRelation for this customer
                authRepository.updateCustomerUserRelationDepartment(
                    phoneNumber = newUserPhone,
                    customerId = currentCustomerId,
                    departmentName = departmentName
                )

                // 5. Sync relation for new assignment using same format as project creation flow
                projectRepository.syncUserCustomerProjectPhaseRelationForAssignment(
                    customerId = currentCustomerId,
                    phoneNumber = newUserPhone,
                    isApprover = isApprover
                )
                
                val updatedCount = projectResult.getOrNull() ?: 0
                _successMessage.value = if (isApprover) {
                    "Approver replaced in $departmentName ($updatedCount project${if (updatedCount != 1) "s" else ""} updated)"
                } else {
                    "User replaced in $departmentName ($updatedCount project${if (updatedCount != 1) "s" else ""} updated)"
                }
                
                // 6. Refresh
                loadUsersGroupedByDepartment()
                loadUsers()
                
                android.util.Log.d("BusinessHeadViewModel", "✅ Replacement complete. $updatedCount projects updated.")
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error replacing department assignment: ${e.message}", e)
                _error.value = "Error replacing assignment: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Firestore Draft CRUD ──────────────────────────────────────────────────

    /**
     * Save current form state as a draft to Firestore.
     * If [currentDraftId] is non-null, the existing document is updated; otherwise a new one is created.
     * After saving, resets [_currentDraftId] to the saved document ID so subsequent saves update the same doc.
     */
    fun saveDraftToFirestore(
        projectName: String,
        description: String,
        client: String,
        clientPrimaryNumber: String? = null,
        clientSecondaryNumber: String? = null,
        location: String,
        currency: String,
        plannedStartDate: Long?,
        startDate: Long?,
        endDate: Long?,
        handoverDate: Long?,
        maintenanceDate: Long?,
        managerUid: String?,
        approverUid: String?,
        teamMemberUids: List<String>,
        phases: List<com.cubiquitous.tracura.model.PhaseDraft>,
        projectCategories: List<String>,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSavingDraft.value = true
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val customerId = firebaseUser?.uid
                if (customerId == null) {
                    onError("Not authenticated")
                    return@launch
                }

                val db = FirebaseFirestore.getInstance()
                val draftsCollection = db.collection("customers").document(customerId)
                    .collection("draft_projects")

                // Helper to convert nullable epoch millis to Firestore Timestamp
                val toTimestamp: (Long?) -> com.google.firebase.Timestamp? = { m -> m?.let { com.google.firebase.Timestamp(java.util.Date(it)) } }

                val formState: Map<String, Any?> = mapOf(
                    // Top-level formState keys must match draftsavingmodel.md
                    "projectName" to (projectName ?: ""),
                    "projectDescription" to (description ?: ""),
                    "client" to (client ?: ""),
                    "clientPrimaryNumber" to (clientPrimaryNumber ?: ""),
                    "clientSecondaryNumber" to (clientSecondaryNumber ?: ""),
                    "location" to (location ?: ""),
                    "plannedDate" to toTimestamp(plannedStartDate),
                    "currency" to (currency ?: "INR"),
                    "projectType" to null,
                    "allowTemplateOverrides" to false,
                    "phases" to phases.mapIndexed { idx, phase ->
                        val phaseId = java.util.UUID.randomUUID().toString()
                        mapOf(
                            "id" to phaseId,
                            "phaseNumber" to (idx + 1),
                            "phaseName" to (phase.phaseName ?: ""),
                            "startDate" to toTimestamp(phase.start?.time),
                            "endDate" to toTimestamp(phase.end?.time),
                            "hasStartDate" to (phase.start != null),
                            "hasEndDate" to (phase.end != null),
                            "managerSearchText" to "",
                            "teamMemberSearchText" to "",
                            "departments" to phase.departments.map { dept ->
                                val deptId = java.util.UUID.randomUUID().toString()
                                // Convert contractor mode enum to stored string values expected by iOS
                                 val contractorModeString = when (dept.contractorMode) {
                                     com.cubiquitous.tracura.model.ContractorMode.SELF_EXECUTION -> "Self-Execution"
                                     com.cubiquitous.tracura.model.ContractorMode.LABOUR_ONLY -> "Labour-Only"
                                     com.cubiquitous.tracura.model.ContractorMode.TURNKEY_AMOUNT_ONLY -> "Turnkey"
                                     else -> "Labour-Only"
                                 }
                                mapOf(
                                    "id" to deptId,
                                    "name" to (dept.departmentName ?: ""),
                                    // amount stored as String per contract
                                    "amount" to String.format("%s", if (dept.totalBudget == 0.0) "" else dept.totalBudget.toInt().toString()),
                                    "contractorMode" to contractorModeString,
                                    "contractorAmount" to if (dept.contractorAmount > 0.0) dept.contractorAmount else null,
                                    "lineItems" to dept.lineItems.map { li ->
                                        mapOf(
                                            "id" to java.util.UUID.randomUUID().toString(),
                                            "itemType" to (li.itemType ?: ""),
                                            "item" to (li.item ?: ""),
                                            "spec" to (li.spec ?: ""),
                                            // quantity and unitPrice stored as Strings per contract
                                            "quantity" to String.format("%s", if (li.quantity == 0.0) "" else li.quantity.toInt().toString()),
                                            "uom" to (li.uom ?: ""),
                                            "unitPrice" to String.format("%s", if (li.unitPrice == 0.0) "" else li.unitPrice.toInt().toString()),
                                            "numberOfDays" to 1.0
                                        )
                                    }
                                )
                            },
                            "categories" to phase.categories,
                            "selectedManagerId" to null,
                            "selectedTeamMemberIds" to emptyList<String>()
                        )
                    },
                    "selectedProjectManagerId" to approverUid,
                    "selectedProjectTeamMemberIds" to teamMemberUids,
                    "selectedManagerId" to null,
                    "accountManagerUid" to managerUid,
                    "attachmentURL" to null,
                    "attachmentName" to null,
                    "expandedPhaseIds" to emptyList<String>(),
                    "departmentUserAssignments" to null,
                    "departmentApproverAssignments" to null
                )

                val existingDraftId = _currentDraftId
                val now = Timestamp.now()

                if (existingDraftId != null) {
                    // Update existing doc — preserve createdAt
                    val draftRef = draftsCollection.document(existingDraftId)
                    val existing = draftRef.get().await()
                    val createdAt = if (existing.exists()) {
                        existing.getTimestamp("createdAt") ?: now
                    } else {
                        now
                    }
                    draftRef.set(
                        mapOf(
                            "formState" to formState,
                            "createdAt" to createdAt,
                            "updatedAt" to now
                        )
                    ).await()
                    android.util.Log.d("BusinessHeadViewModel", "✅ Firestore draft updated: $existingDraftId")
                } else {
                    // Create new doc
                    val draftRef = draftsCollection.document()
                    draftRef.set(
                        mapOf(
                            "formState" to formState,
                            "createdAt" to now,
                            "updatedAt" to now
                        )
                    ).await()
                    _currentDraftId = draftRef.id
                    android.util.Log.d("BusinessHeadViewModel", "✅ Firestore draft created: ${draftRef.id}")
                }

                // Update hasDrafts flag on customer doc
                db.collection("customers").document(customerId)
                    .update(
                        mapOf(
                            "hasDrafts" to true,
                            "lastDraftUpdatedAt" to now
                        )
                    ).await()

                // Refresh in-memory list
                loadFirestoreDrafts()
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error saving draft to Firestore: ${e.message}", e)
                onError(e.message ?: "Failed to save draft")
            } finally {
                _isSavingDraft.value = false
            }
        }
    }

    /**
     * Load all draft projects from Firestore for the current customer, ordered by updatedAt desc.
     */
    fun loadFirestoreDrafts() {
        viewModelScope.launch {
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val customerId = firebaseUser?.uid ?: return@launch

                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("customers").document(customerId)
                    .collection("draft_projects")
                    .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()

                val loadedDrafts = snapshot.documents.mapNotNull { doc ->
                    firestoreDocToProjectDraft(doc)
                }
                _firestoreDrafts.value = loadedDrafts
                android.util.Log.d("BusinessHeadViewModel", "✅ Loaded ${loadedDrafts.size} Firestore drafts")
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error loading Firestore drafts: ${e.message}", e)
            }
        }
    }

    /**
     * Load a specific Firestore draft by its document ID.
     * Returns a [ProjectDraft] compatible with the existing form-population code, or null if not found.
     */
    suspend fun loadFirestoreDraftById(draftId: String): ProjectDraft? {
        return try {
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val customerId = firebaseUser?.uid ?: return null

            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("customers").document(customerId)
                .collection("draft_projects")
                .document(draftId)
                .get()
                .await()

            if (!doc.exists()) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Firestore draft not found: $draftId")
                return null
            }
            _currentDraftId = draftId
            firestoreDocToProjectDraft(doc).also {
                if (it != null) android.util.Log.d("BusinessHeadViewModel", "✅ Loaded Firestore draft: $draftId")
            }
        } catch (e: Exception) {
            android.util.Log.e("BusinessHeadViewModel", "❌ Error loading Firestore draft by ID: ${e.message}", e)
            null
        }
    }

    /**
     * Delete a Firestore draft and update the customer's hasDrafts flag.
     */
    fun deleteFirestoreDraft(draftId: String) {
        viewModelScope.launch {
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val customerId = firebaseUser?.uid ?: return@launch

                val db = FirebaseFirestore.getInstance()
                val draftsCollection = db.collection("customers").document(customerId)
                    .collection("draft_projects")

                // If we're deleting the currently-loaded draft, clear the in-memory ID
                if (_currentDraftId == draftId) {
                    _currentDraftId = null
                }

                // Optimistic UI update
                _firestoreDrafts.value = _firestoreDrafts.value.filter { it.id != draftId }

                val draftRef = draftsCollection.document(draftId)
                val doc = draftRef.get().await()
                if (!doc.exists()) {
                    loadFirestoreDrafts()
                    return@launch
                }

                draftRef.delete().await()
                android.util.Log.d("BusinessHeadViewModel", "✅ Firestore draft deleted: $draftId")

                // Check if any drafts remain; if not, clear hasDrafts flag
                val remaining = draftsCollection.get().await()
                if (remaining.isEmpty) {
                    db.collection("customers").document(customerId)
                        .update("hasDrafts", false)
                        .await()
                }

                loadFirestoreDrafts()
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadViewModel", "❌ Error deleting Firestore draft: ${e.message}", e)
                // Restore optimistic removal
                loadFirestoreDrafts()
                _error.value = "Failed to delete draft: ${e.message}"
            }
        }
    }

    /**
     * Clear the currently-tracked draft ID so the next save creates a new doc.
     */
    fun clearCurrentDraftId() {
        _currentDraftId = null
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun firestoreDocToProjectDraft(doc: com.google.firebase.firestore.DocumentSnapshot): ProjectDraft? {
        return try {
            val formState = doc.get("formState") as? Map<String, Any?> ?: return null
            val updatedAtTs = doc.getTimestamp("updatedAt")
            val lastSaved = updatedAtTs?.toDate()?.time ?: System.currentTimeMillis()
            // Helper to convert Firestore Timestamp/Date/Number to epoch millis
            fun anyToMillis(v: Any?): Long? = when (v) {
                is com.google.firebase.Timestamp -> v.toDate().time
                is java.util.Date -> v.time
                is Number -> v.toLong()
                else -> null
            }

            // Parse phases list (handle new schema where phases have nested departments/lineItems)
            val rawPhases = formState["phases"] as? List<*> ?: emptyList<Any>()
            val phases = rawPhases.mapNotNull { rawPhase ->
                val phaseMap = rawPhase as? Map<String, Any?> ?: return@mapNotNull null
                val rawDepts = phaseMap["departments"] as? List<*> ?: emptyList<Any>()
                val departments = rawDepts.mapNotNull { rawDept ->
                    val deptMap = rawDept as? Map<String, Any?> ?: return@mapNotNull null
                    val name = (deptMap["name"] as? String) ?: (deptMap["departmentName"] as? String) ?: ""
                    val amountStr = deptMap["amount"] as? String
                    val allocatedBudget = amountStr?.toDoubleOrNull()
                        ?: (deptMap["contractorAmount"] as? Number)?.toDouble()
                        ?: (deptMap["allocatedBudget"] as? Number)?.toDouble()
                        ?: 0.0
                    DepartmentBudget(
                        departmentName = name,
                        allocatedBudget = allocatedBudget
                    )
                }
                val rawCategories = phaseMap["categories"] as? List<*> ?: emptyList<Any>()
                PhaseDraftSerializable(
                    phaseName = (phaseMap["phaseName"] as? String) ?: (phaseMap["phaseName"] as? String) ?: "",
                    startDate = anyToMillis(phaseMap["startDate"] ?: phaseMap["start"]),
                    endDate = anyToMillis(phaseMap["endDate"] ?: phaseMap["end"]),
                    departments = departments,
                    categories = rawCategories.filterIsInstance<String>()
                )
            }

            val rawTeamUids = (formState["selectedProjectTeamMemberIds"] as? List<*>)
                ?: (formState["teamMemberUids"] as? List<*>)
                ?: emptyList<Any>()
            val rawCategories = formState["projectCategories"] as? List<*> ?: emptyList<Any>()

            ProjectDraft(
                id = doc.id,
                projectId = null,
                projectName = formState["projectName"] as? String ?: "",
                description = (formState["projectDescription"] as? String)
                    ?: (formState["description"] as? String) ?: "",
                client = formState["client"] as? String ?: "",
                clientPrimaryNumber = formState["clientPrimaryNumber"] as? String ?: "",
                clientSecondaryNumber = formState["clientSecondaryNumber"] as? String ?: "",
                location = formState["location"] as? String ?: "",
                currency = formState["currency"] as? String ?: "INR",
                plannedStartDate = anyToMillis(formState["plannedDate"] ?: formState["plannedStartDate"]),
                startDate = anyToMillis(formState["startDate"]),
                endDate = anyToMillis(formState["endDate"]),
                handoverDate = anyToMillis(formState["handoverDate"]),
                maintenanceDate = anyToMillis(formState["maintenanceDate"]),
                managerUid = formState["accountManagerUid"] as? String,
                approverUid = formState["selectedProjectManagerId"] as? String ?: (formState["approverUid"] as? String),
                teamMemberUids = rawTeamUids.filterIsInstance<String>(),
                phases = phases,
                projectCategories = rawCategories.filterIsInstance<String>(),
                lastSaved = lastSaved
            )
        } catch (e: Exception) {
            android.util.Log.e("BusinessHeadViewModel", "❌ Error parsing Firestore draft doc ${doc.id}: ${e.message}", e)
            null
        }
    }

}
