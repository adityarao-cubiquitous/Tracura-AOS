package com.cubiquitous.tracura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class ProjectViewModel @Inject constructor(
    val projectRepository: ProjectRepository
) : ViewModel() {
    
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()
    
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Business name state (for Business Head)
    private val _businessName = MutableStateFlow<String>("Business")
    val businessName: StateFlow<String> = _businessName.asStateFlow()
    
    private var currentJob: kotlinx.coroutines.Job? = null
    private var currentProjectJob: kotlinx.coroutines.Job? = null

    // Track if we're already collecting from a Flow to prevent duplicate collections
    private var isCollecting = false

    // Persistent job that holds the real-time Firestore snapshot listener
    private var realtimeListenerJob: kotlinx.coroutines.Job? = null
    
    /**
     * Load projects - only loads if projects list is empty (first load)
     * CRITICAL: This preserves cached data when navigating back from other screens
     * ViewModel lifecycle ensures StateFlow persists across navigation
     * 
     * Why this works:
     * - ViewModel survives configuration changes and navigation
     * - StateFlow retains its value even when Composable is recreated
     * - Only loads on first call, subsequent calls are ignored if data exists
     */
    fun loadProjects(userId: String? = null) {
        // CRITICAL: Only load if projects list is empty (first load)
        // This prevents clearing existing data when navigating back
        if (_projects.value.isNotEmpty()) {
            Log.d("ProjectViewModel", "✅ Projects already loaded (${_projects.value.size} projects), skipping reload")
            Log.d("ProjectViewModel", "✅ Using cached data - no Firebase call needed")
            return
        }
        
        // Prevent duplicate loads
        if (isCollecting) {
            Log.d("ProjectViewModel", "⚠️ Already loading projects, skipping duplicate call")
            return
        }
        
        // Cancel previous job if exists (shouldn't happen due to checks above, but safety)
        currentJob?.cancel()
        
        isCollecting = true
        currentJob = viewModelScope.launch {
            Log.d("ProjectViewModel", "🚀 Starting to load projects (first load)")
            Log.d("ProjectViewModel", "🚀 User ID: $userId")
            _isLoading.value = true
            _error.value = null
            
            try {
                if (userId != null && userId.isNotEmpty()) {
                    // Load projects for specific user using flow
                    Log.d("ProjectViewModel", "🔄 Loading projects for user: $userId")
                    
                    // CRITICAL: Flow operations run on IO dispatcher to prevent blocking Main thread
                    // Firebase Firestore operations are I/O bound and must not run on Main thread
                    projectRepository.getUserProjects(userId)
                        .flowOn(Dispatchers.IO) // Move Firebase operations to IO thread
                        .catch { e ->
                            Log.e("ProjectViewModel", "❌ Error in Flow: ${e.message}", e)
                            _error.value = "Failed to load projects: ${e.message}"
                            _isLoading.value = false
                            isCollecting = false
                        }
                        .collect { projectList ->
                            Log.d("ProjectViewModel", "📦 Received ${projectList.size} projects for user $userId")
                            // CRITICAL: Update StateFlow on Main thread (collect already runs on Main)
                            _projects.value = projectList
                            _isLoading.value = false
                            isCollecting = false
                        }
                } else {
                    // Load all projects for production heads using flow
                    Log.d("ProjectViewModel", "🔄 Loading all projects")
                    
                    // CRITICAL: Flow operations run on IO dispatcher to prevent blocking Main thread
                    // Firebase Firestore operations are I/O bound and must not run on Main thread
                    projectRepository.getAllProjectsFlow()
                        .flowOn(Dispatchers.IO) // Move Firebase operations to IO thread
                        .catch { e ->
                            Log.e("ProjectViewModel", "❌ Error in Flow: ${e.message}", e)
                            _error.value = "Failed to load projects: ${e.message}"
                            _isLoading.value = false
                            isCollecting = false
                        }
                        .collect { projectList ->
                            Log.d("ProjectViewModel", "📦 Received ${projectList.size} projects")
                            // CRITICAL: Update StateFlow on Main thread (collect already runs on Main)
                            _projects.value = projectList
                            _isLoading.value = false
                            isCollecting = false
                        }
                }
            } catch (e: Exception) {
                Log.e("ProjectViewModel", "❌ Error loading projects: ${e.message}", e)
                _error.value = e.message ?: "Failed to load projects"
                _isLoading.value = false
                isCollecting = false
            }
        }
    }
    
    /**
     * Load projects based on user role (following the guide's role-based fetching pattern)
     */
    fun loadProjectsByRole(userId: String, role: com.cubiquitous.tracura.model.UserRole, customerId: String? = null) {
        // CRITICAL: Only load if projects list is empty (first load)
        if (_projects.value.isNotEmpty()) {
            Log.d("ProjectViewModel", "✅ Projects already loaded (${_projects.value.size} projects), skipping reload")
            return
        }
        
        // Prevent duplicate loads
        if (isCollecting) {
            Log.d("ProjectViewModel", "⚠️ Already loading projects, skipping duplicate call")
            return
        }
        
        // Cancel previous job if exists
        currentJob?.cancel()
        
        isCollecting = true
        currentJob = viewModelScope.launch {
            Log.d("ProjectViewModel", "🚀 Starting role-based project load: userId=$userId, role=$role")
            _isLoading.value = true
            _error.value = null
            
            try {
                projectRepository.getProjectsByRole(userId, role, customerId)
                    .flowOn(Dispatchers.IO)
                    .catch { e ->
                        Log.e("ProjectViewModel", "❌ Error in role-based Flow: ${e.message}", e)
                        _error.value = "Failed to load projects: ${e.message}"
                        _isLoading.value = false
                        isCollecting = false
                    }
                    .collect { projectList ->
                        Log.d("ProjectViewModel", "📦 Received ${projectList.size} projects (role: $role)")
                        _projects.value = projectList
                        _isLoading.value = false
                        isCollecting = false
                    }
            } catch (e: Exception) {
                Log.e("ProjectViewModel", "❌ Error loading projects by role: ${e.message}", e)
                _error.value = e.message ?: "Failed to load projects"
                _isLoading.value = false
                isCollecting = false
            }
        }
    }
    
    /**
     * Start a persistent real-time listener for projects based on role.
     * The listener stays active until explicitly cancelled or restarted.
     * Firestore pushes changes immediately via addSnapshotListener — no polling needed.
     */
    fun startRealtimeListenerByRole(userId: String, role: com.cubiquitous.tracura.model.UserRole, customerId: String? = null) {
        if (realtimeListenerJob?.isActive == true) {
            Log.d("ProjectViewModel", "✅ Realtime listener already active, skipping")
            return
        }

        realtimeListenerJob?.cancel()
        var firstEmit = true

        realtimeListenerJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            projectRepository.getProjectsByRoleRealtime(userId, role, customerId)
                .flowOn(Dispatchers.IO)
                .catch { e ->
                    Log.e("ProjectViewModel", "❌ Realtime listener error: ${e.message}", e)
                    _error.value = "Failed to load projects: ${e.message}"
                    _isLoading.value = false
                }
                .collect { projectList ->
                    Log.d("ProjectViewModel", "📡 Realtime update: ${projectList.size} projects (role: $role)")
                    _projects.value = projectList
                    if (firstEmit) {
                        _isLoading.value = false
                        firstEmit = false
                    }
                }
        }
    }

    /**
     * Force refresh projects by role — cancels the current realtime listener and restarts it.
     * Used by pull-to-refresh and customer switching.
     */
    fun refreshProjectsByRole(userId: String, role: com.cubiquitous.tracura.model.UserRole, customerId: String? = null) {
        Log.d("ProjectViewModel", "🔄 Restarting realtime listener (refresh)")
        realtimeListenerJob?.cancel()
        realtimeListenerJob = null
        isCollecting = false
        startRealtimeListenerByRole(userId, role, customerId)
    }
    
    /**
     * Force refresh projects from Firebase (for pull-to-refresh)
     * This always reloads data, unlike loadProjects() which skips if data exists
     */
    fun refreshProjects(userId: String? = null) {
        Log.d("ProjectViewModel", "🔄 Force refreshing projects from Firebase")
        
        // Cancel previous job
        currentJob?.cancel()
        isCollecting = false
        
        isCollecting = true
        currentJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                if (userId != null && userId.isNotEmpty()) {
                    projectRepository.getUserProjects(userId)
                        .flowOn(Dispatchers.IO)
                        .catch { e ->
                            Log.e("ProjectViewModel", "❌ Error refreshing: ${e.message}", e)
                            _error.value = "Failed to refresh projects: ${e.message}"
                            _isLoading.value = false
                            isCollecting = false
                        }
                        .collect { projectList ->
                            Log.d("ProjectViewModel", "✅ Refreshed ${projectList.size} projects")
                            _projects.value = projectList
                            _isLoading.value = false
                            isCollecting = false
                        }
                } else {
                    projectRepository.getAllProjectsFlow()
                        .flowOn(Dispatchers.IO)
                        .catch { e ->
                            Log.e("ProjectViewModel", "❌ Error refreshing: ${e.message}", e)
                            _error.value = "Failed to refresh projects: ${e.message}"
                            _isLoading.value = false
                            isCollecting = false
                        }
                        .collect { projectList ->
                            Log.d("ProjectViewModel", "✅ Refreshed ${projectList.size} projects")
                            _projects.value = projectList
                            _isLoading.value = false
                            isCollecting = false
                        }
                }
            } catch (e: Exception) {
                Log.e("ProjectViewModel", "❌ Error refreshing projects: ${e.message}", e)
                _error.value = e.message ?: "Failed to refresh projects"
                _isLoading.value = false
                isCollecting = false
            }
        }
    }

    
    suspend fun createProject(project: Project, customerId: String): Result<String> {
        return projectRepository.createProject(project, customerId)
    }
    
    suspend fun updateProject(projectId: String, project: Project): Result<Unit> {
        return projectRepository.updateProject(projectId, project)
    }
    
    fun clearError() {
        _error.value = null
    }
    
    // State for phase requests across all projects
    private val _allPhaseRequests = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val allPhaseRequests: StateFlow<List<Map<String, Any>>> = _allPhaseRequests.asStateFlow()
    
    private val _isLoadingPhaseRequests = MutableStateFlow(false)
    val isLoadingPhaseRequests: StateFlow<Boolean> = _isLoadingPhaseRequests.asStateFlow()
    
    /**
     * Load all pending phase requests across all projects for production head
     */
    fun loadAllPhaseRequests() {
        viewModelScope.launch {
            _isLoadingPhaseRequests.value = true
            try {
                Log.d("ProjectViewModel", "🔄 Loading all phase requests across all projects")
                val requests = projectRepository.getAllPendingPhaseRequestsAcrossAllProjects()
                _allPhaseRequests.value = requests
                Log.d("ProjectViewModel", "✅ Loaded ${requests.size} phase requests")
            } catch (e: Exception) {
                Log.e("ProjectViewModel", "❌ Error loading all phase requests: ${e.message}")
                _allPhaseRequests.value = emptyList()
            } finally {
                _isLoadingPhaseRequests.value = false
            }
        }
    }
    
    /**
     * Approve a phase request
     */
    fun approvePhaseRequest(
        projectId: String,
        phaseId: String,
        requestId: String,
        extendedDate: String,
        reason: String = "Approved",
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isLoadingPhaseRequests.value = true
            try {
                Log.d("ProjectViewModel", "✅ Approving phase request: $requestId")
                Log.d("ProjectViewModel", "📅 Extended date: $extendedDate")
                Log.d("ProjectViewModel", "💬 Reason: $reason")
                
                val result = projectRepository.approvePhaseRequest(
                    projectId = projectId,
                    phaseId = phaseId,
                    requestId = requestId,
                    extendedDate = extendedDate,
                    reason = reason
                )
                if (result.isSuccess) {
                    Log.d("ProjectViewModel", "✅ Phase request approved - updating phase end date to $extendedDate")
                    Log.d("ProjectViewModel", "✅ Extended badge will now show for this phase")
                    
                    // Reload phase requests to remove approved request from list
                    loadAllPhaseRequests()
                    
                    // Reload projects to update phase end dates (so Extended badge shows)
                    loadProjects()
                    
                    onComplete(true)
                } else {
                    Log.e("ProjectViewModel", "❌ Failed to approve phase request")
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e("ProjectViewModel", "❌ Error approving phase request: ${e.message}")
                onComplete(false)
            } finally {
                _isLoadingPhaseRequests.value = false
            }
        }
    }
    
    /**
     * Reject a phase request
     */
    fun rejectPhaseRequest(
        projectId: String,
        phaseId: String,
        requestId: String,
        reason: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isLoadingPhaseRequests.value = true
            try {
                Log.d("ProjectViewModel", "❌ Rejecting phase request: $requestId")
                Log.d("ProjectViewModel", "💬 Rejection reason: $reason")
                
                val result = projectRepository.rejectPhaseRequest(
                    projectId = projectId,
                    phaseId = phaseId,
                    requestId = requestId,
                    reason = reason
                )
                if (result.isSuccess) {
                    Log.d("ProjectViewModel", "✅ Phase request rejected - user will see REJECTED status")
                    
                    // Reload phase requests to remove rejected request from list
                    loadAllPhaseRequests()
                    
                    onComplete(true)
                } else {
                    Log.e("ProjectViewModel", "❌ Failed to reject phase request")
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e("ProjectViewModel", "❌ Error rejecting phase request: ${e.message}")
                onComplete(false)
            } finally {
                _isLoadingPhaseRequests.value = false
            }
        }
    }
    
    // Load a single project by ID - faster than loading all projects
    fun loadProjectById(projectId: String) {
        // Cancel previous job if exists
        currentProjectJob?.cancel()
        
        currentProjectJob = viewModelScope.launch {
            Log.d("ProjectViewModel", "🚀 Loading project by ID: $projectId")
            _isLoading.value = true
            _error.value = null
            _currentProject.value = null
            
            try {
                val project = projectRepository.getProjectById(projectId)
                if (project != null) {
                    Log.d("ProjectViewModel", "✅ Found project: ${project.name}")
                    _currentProject.value = project
                    _isLoading.value = false
                } else {
                    Log.e("ProjectViewModel", "❌ Project not found: $projectId")
                    _error.value = "Project not found"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("ProjectViewModel", "❌ Error loading project: ${e.message}", e)
                _error.value = "Failed to load project: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Assign approvers and production heads to a project
    fun assignProjectMembers(
        projectId: String,
        approverIds: List<String>,
        BusinessHeadIds: List<String>
    ) {
        viewModelScope.launch {
            try {
                Log.d("ProjectViewModel", "🔄 Assigning project members to project: $projectId")
                
                val result = projectRepository.updateProjectAssignments(projectId, approverIds, BusinessHeadIds)
                
                if (result.isSuccess) {
                    Log.d("ProjectViewModel", "✅ Successfully assigned project members")
                    // Reload projects to get updated data
                    loadProjects()
                } else {
                    Log.e("ProjectViewModel", "❌ Failed to assign project members: ${result.exceptionOrNull()?.message}")
                    _error.value = "Failed to assign project members: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                Log.e("ProjectViewModel", "❌ Error assigning project members: ${e.message}")
                _error.value = "Error assigning project members: ${e.message}"
            }
        }
    }
    
    /**
     * Load business name from Firebase (for Business Head)
     * CRITICAL: This runs on IO dispatcher to prevent blocking Main thread
     * Only loads if business name is still default value (first load)
     */
    fun loadBusinessName(customerId: String) {
        // Only load if business name is still default (first load)
        if (_businessName.value != "Business") {
            Log.d("ProjectViewModel", "✅ Business name already loaded: ${_businessName.value}")
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("ProjectViewModel", "🔄 Loading business name for customer: $customerId")
                
                // CRITICAL: Firebase operations MUST run on IO dispatcher
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val customerDoc = firestore.collection("customers")
                    .document(customerId)
                    .get()
                    .await()
                
                if (customerDoc.exists()) {
                    val fetchedBusinessName = customerDoc.getString("businessName")
                    if (!fetchedBusinessName.isNullOrBlank()) {
                        // Update UI state on Main thread
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            _businessName.value = fetchedBusinessName
                            Log.d("ProjectViewModel", "✅ Business name loaded: $fetchedBusinessName")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ProjectViewModel", "❌ Error loading business name: ${e.message}", e)
            }
        }
    }
} 