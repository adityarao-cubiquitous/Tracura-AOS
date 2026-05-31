package com.cubiquitous.tracura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.ExpenseStatus
import com.cubiquitous.tracura.model.Phase
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.repository.ExpenseRepository
import com.cubiquitous.tracura.repository.ProjectRepository
import com.cubiquitous.tracura.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import com.cubiquitous.tracura.model.CategoryBudget
import com.cubiquitous.tracura.model.DepartmentBudgetBreakdown
import com.cubiquitous.tracura.model.ProjectBudgetSummary
import com.cubiquitous.tracura.model.PhaseBreakdown
import com.cubiquitous.tracura.model.Department
import com.cubiquitous.tracura.model.DepartmentDraft
import com.cubiquitous.tracura.model.PhaseWithDepartments
import com.cubiquitous.tracura.model.PhaseBudget
import com.cubiquitous.tracura.model.UserRole
import com.google.firebase.firestore.Source

@HiltViewModel
class ApproverProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val expenseRepository: ExpenseRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()
    
    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()
    
    private val _projectBudgetSummary = MutableStateFlow(ProjectBudgetSummary())
    val projectBudgetSummary: StateFlow<ProjectBudgetSummary> = _projectBudgetSummary.asStateFlow()
    private val _phaseBreakdowns = MutableStateFlow<List<PhaseBreakdown>>(emptyList())
    val phaseBreakdowns: StateFlow<List<PhaseBreakdown>> = _phaseBreakdowns.asStateFlow()
    
    // Pre-calculated ongoing phase breakdowns (filtered by date range)
    // This is computed off the UI thread to prevent lag during recomposition
    val ongoingPhaseBreakdowns: StateFlow<List<PhaseBreakdown>> = _phaseBreakdowns
        .map { breakdowns ->
            filterOngoingPhases(breakdowns)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Store all phases (unfiltered) for "View All Phases" functionality
    private val _allPhases = MutableStateFlow<List<Phase>>(emptyList())
    val allPhases: StateFlow<List<Phase>> = _allPhases.asStateFlow()
    
    // Store phases with departments from subcollection
    private val _phasesWithDepartments = MutableStateFlow<List<PhaseWithDepartments>>(emptyList())
    val phasesWithDepartments: StateFlow<List<PhaseWithDepartments>> = _phasesWithDepartments.asStateFlow()
    
    private val _projectExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val projectExpenses: StateFlow<List<Expense>> = _projectExpenses.asStateFlow()
    
    // Phase budget calculations (approved spent amounts)
    private val _phaseBudgetMap = MutableStateFlow<Map<String, PhaseBudget>>(emptyMap())
    val phaseBudgetMap: StateFlow<Map<String, PhaseBudget>> = _phaseBudgetMap.asStateFlow()
    
    private val _phaseDepartmentSpentMap = MutableStateFlow<Map<String, Map<String, Double>>>(emptyMap())
    val phaseDepartmentSpentMap: StateFlow<Map<String, Map<String, Double>>> = _phaseDepartmentSpentMap.asStateFlow()
    
    private val _phaseAnonymousExpensesMap = MutableStateFlow<Map<String, Double>>(emptyMap())
    val phaseAnonymousExpensesMap: StateFlow<Map<String, Double>> = _phaseAnonymousExpensesMap.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _userNameMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val userNameMap: StateFlow<Map<String, String>> = _userNameMap.asStateFlow()
    
    private val _businessHeadPhone = MutableStateFlow<String?>(null)
    val businessHeadPhone: StateFlow<String?> = _businessHeadPhone.asStateFlow()
    
    private val _phaseRequests = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val phaseRequests: StateFlow<List<Map<String, Any>>> = _phaseRequests.asStateFlow()
    
    private val _phaseRequestCount = MutableStateFlow(0)
    val phaseRequestCount: StateFlow<Int> = _phaseRequestCount.asStateFlow()
    
    // Maintenance status calculation (moved from UI to ViewModel for performance)
    val maintenanceStatus: StateFlow<String?> = _selectedProject
        .map { project ->
            calculateMaintenanceStatus(project)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    // Instant budget summary (merged from preloaded project and fetched data)
    val instantBudgetSummary: StateFlow<ProjectBudgetSummary> = combine(
        _selectedProject,
        _projectBudgetSummary
    ) { project, budgetSummary ->
        calculateInstantBudgetSummary(project, budgetSummary)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = ProjectBudgetSummary()
        )
    
    // Store preloaded project for instant budget summary calculation
    private var _preloadedProject: Project? = null
    
    fun loadProjects(userId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                if (userId != null && userId.isNotEmpty() && userId != "1234567891") {
                    // Load both regular and temporary projects using combine
                    combine(
                        projectRepository.getApproverProjects(userId),
                        projectRepository.getTemporaryApproverProjects(userId)
                    ) { regularList, temporaryList ->
                        // Combine and deduplicate projects
                        val allProjects = (regularList + temporaryList).distinctBy { it.id }
                        allProjects
                    }.collect { allProjects ->
                        _projects.value = allProjects
                        _isLoading.value = false
                    }
                } else {
                    // Load all active projects (fallback) - this is a one-time call
                    val projectList = projectRepository.getAllProjects()
                    _projects.value = projectList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Failed to load projects: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun resolveUserNames(expenses: List<Expense>) {
        val phonesToResolve = expenses.map { it.submittedBy }
            .filter { it.isNotBlank() && !_userNameMap.value.containsKey(it) }
            .distinct()

        if (phonesToResolve.isEmpty()) return

        viewModelScope.launch {
            val newNames = mutableMapOf<String, String>()
            phonesToResolve.forEach { phone ->
                try {
                    val user = authRepository.getUserByPhoneNumber(phone)
                    if (user != null && user.name.isNotBlank()) {
                        newNames[phone] = user.name
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ApproverProjectViewModel", "Error resolving name for $phone", e)
                }
            }
            if (newNames.isNotEmpty()) {
                _userNameMap.update { current -> current + newNames }
            }
        }
    }

    fun loadBusinessHeadPhone(userPhone: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ApproverProjectViewModel", "🔍 Loading business head phone for user: $userPhone")
                val bhPhone = authRepository.getBusinessHeadPhone(userPhone)
                _businessHeadPhone.value = bhPhone
                android.util.Log.d("ApproverProjectViewModel", "✅ Business head phone loaded: $bhPhone")
            } catch (e: Exception) {
                android.util.Log.e("ApproverProjectViewModel", "❌ Error loading business head phone", e)
            }
        }
    }

    private suspend fun getScopedPhases(projectId: String): List<Phase> {
        val currentRole = authRepository.getCurrentUserFromFirebase()?.role
        if (currentRole != UserRole.APPROVER && currentRole != UserRole.USER) {
            return projectRepository.getPhasesForProject(projectId)
        }

        val customerId = projectRepository.findCustomerIdForProject(projectId)
            ?: return emptyList()
        val allowedPhaseIds = projectRepository.getAllowedPhaseIdsForCurrentUserProject(projectId, customerId)
        if (allowedPhaseIds.isEmpty()) return emptyList()
        return projectRepository.getPhasesForProjectByIds(projectId, allowedPhaseIds, customerId)
    }

    private suspend fun getScopedPhasesWithDepartments(
        projectId: String,
        source: Source = Source.DEFAULT,
    ): List<PhaseWithDepartments> {
        val currentRole = authRepository.getCurrentUserFromFirebase()?.role
        if (currentRole != UserRole.APPROVER && currentRole != UserRole.USER) {
            return projectRepository.getPhasesWithDepartmentsForProject(projectId, source)
        }

        val customerId = projectRepository.findCustomerIdForProject(projectId)
            ?: return emptyList()
        val allowedPhaseIds = projectRepository.getAllowedPhaseIdsForCurrentUserProject(projectId, customerId)
        if (allowedPhaseIds.isEmpty()) return emptyList()
        return projectRepository.getPhasesWithDepartmentsForProjectByIds(projectId, allowedPhaseIds, source, customerId)
    }
    
    fun addDepartmentToPhase(
        projectId: String,
        phaseId: String,
        departmentName: String,
        budget: Double
    ) {
        viewModelScope.launch {
            try {
                // Get phase to check if department name matches phase name
                val phases = getScopedPhases(projectId)
                val phase = phases.firstOrNull { it.id == phaseId }
                
                // Prevent department name from being the same as phase name
                if (phase != null && departmentName.trim().equals(phase.phaseName.trim(), ignoreCase = true)) {
                    return@launch
                }
                
                val result = projectRepository.addDepartmentToPhase(
                    projectId = projectId,
                    phaseId = phaseId,
                    departmentName = departmentName,
                    budget = budget
                )
                if (result.isSuccess) {
                    loadProjectBudgetSummary(projectId, forceRefresh = true)
                }
            } catch (e: Exception) {
            }
        }
    }
    
    /**
     * Add department with full details (contractor mode, line items) as separate document
     */
    fun addDepartmentWithDetailsToPhase(
        projectId: String,
        phaseId: String,
        departmentDraft: DepartmentDraft
    ) {
        viewModelScope.launch {
            try {
                
                // Validate inputs
                if (projectId.isBlank() || phaseId.isBlank()) {
                    return@launch
                }
                
                if (departmentDraft.departmentName.isBlank()) {
                    return@launch
                }
                
                // Get customerId for this project
                val customerId = projectRepository.findCustomerIdForProject(projectId)
                if (customerId == null) {
                    return@launch
                }
                // Get phase to check if department name matches phase name
                val phases = try {
                    getScopedPhases(projectId)
                } catch (e: Exception) {
                    emptyList()
                }
                
                val phase = phases.firstOrNull { it.id == phaseId }
                
                // Prevent department name from being the same as phase name
                if (phase != null && departmentDraft.departmentName.trim().equals(phase.phaseName.trim(), ignoreCase = true)) {
                    return@launch
                }
                
                // Convert DepartmentDraft to Department with error handling
                val department = try {
                    val dept = Department.fromDepartmentDraft(departmentDraft, phaseId, projectId)
                    dept
                } catch (e: Exception) {
                    // Fallback to simple department creation
                    Department.fromNameAndBudget(
                        departmentDraft.departmentName,
                        if (departmentDraft.lineItems.isNotEmpty()) {
                            try {
                                // Explicitly specify type for sumOf lambda parameter
                                departmentDraft.lineItems.sumOf { item: com.cubiquitous.tracura.model.LineItem -> item.total }
                            } catch (e2: Exception) {
                                0.0
                            }
                        } else {
                            0.0
                        },
                        phaseId,
                        projectId
                    )
                }
                
                // Save department as separate document
                val result = try {
                    projectRepository.saveDepartmentToPhase(
                        customerId = customerId,
                        projectId = projectId,
                        phaseId = phaseId,
                        department = department
                    )
                } catch (e: Exception) {
                    Result.failure(e)
                }
                
                if (result.isSuccess) {
                    loadProjectBudgetSummary(projectId, forceRefresh = true)
                } else {
                    android.util.Log.e("ApproverProjectViewModel", "❌ Failed to save department: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun selectProject(project: Project) {
        _selectedProject.value = project
        project.id?.let { loadProjectBudgetSummary(it, forceRefresh = true) }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    // Preload project data instantly (for smooth navigation)
    fun preloadProjectData(project: Project) {
        _selectedProject.value = project
        _preloadedProject = project
        _isLoading.value = false
    }
    
    // Create instant budget summary from preloaded project for immediate UI display
    // This allows UI to show project details instantly while phases/expenses load in background
    fun createInstantBudgetSummary(project: Project) {
        viewModelScope.launch {
            try {
                // Create basic budget summary from preloaded project
                // Phases and expenses will be added when they load
                val instantSummary = ProjectBudgetSummary(
                    project = project,
                    totalBudget = project.budget,
                    totalSpent = 0.0, // Will be updated when expenses load
                    totalRemaining = project.budget, // Will be updated when expenses load
                    spentPercentage = 0.0, // Will be updated when expenses load
                    categoryBreakdown = emptyList(), // Will be updated when expenses load
                    departmentBreakdown = emptyList(), // Will be updated when phases/expenses load (fixed: was emptyMap(), should be emptyList())
                    recentExpenses = emptyList(), // Will be updated when expenses load
                    pendingApprovalsCount = 0, // Will be updated when expenses load
                    approvedExpensesCount = 0 // Will be updated when expenses load
                )
                _projectBudgetSummary.value = instantSummary
            } catch (e: Exception) {
                android.util.Log.e("ApproverProjectViewModel", "Error creating instant budget summary: ${e.message}", e)
            }
        }
    }
    
    fun loadProjectBudgetSummary(
        projectId: String,
        preloadedProject: Project? = null,
        forceRefresh: Boolean = false
    ) {
        // Validate projectId to prevent crashes
        if (projectId.isBlank()) {
            _error.value = "Invalid project ID"
            return
        }
        
        // If project is already loaded and matches, skip reloading
        if (!forceRefresh && _selectedProject.value?.id == projectId && _allPhases.value.isNotEmpty() && _projectExpenses.value.isNotEmpty()) {
            return
        }
        
        viewModelScope.launch {
            // Use preloaded project if provided and matches projectId
            val isPreloaded = preloadedProject?.id == projectId || _selectedProject.value?.id == projectId
            if (!isPreloaded) {
                _isLoading.value = true
            }
            _error.value = null
            
            try {
                // Load project, phases, and expenses in parallel for fastest loading
                // CRITICAL: Use preloaded project if available - skip Firebase fetch for basic project data
                var project: Project? = preloadedProject ?: _selectedProject.value?.takeIf { it.id == projectId }
                var phases: List<Phase>
                var initialExpenses: List<Expense>
                
                coroutineScope {
                    val projectDeferred = if (project == null) {
                        // Only fetch from Firebase if no preloaded project available
                        android.util.Log.d("ApproverProjectViewModel", "⚠️ No preloaded project, fetching from Firebase")
                        async { projectRepository.getProjectById(projectId) }
                    } else {
                        // Use preloaded project - no Firebase fetch needed
                        android.util.Log.d("ApproverProjectViewModel", "✅ Using preloaded project: ${project.name} - skipping Firebase fetch")
                        async { project }
                    }
                    
                    // Use getPhasesWithDepartmentsForProject to fetch departments from subcollection
                    val phasesWithDeptsDeferred = async { getScopedPhasesWithDepartments(projectId) }
                    val expensesDeferred = async { 
                        expenseRepository.getProjectExpenses(projectId).first() 
                    }
                    
                    project = projectDeferred.await()
                    val phasesWithDepts = phasesWithDeptsDeferred.await()
                    initialExpenses = expensesDeferred.await()
                    
                    // Extract phases from PhaseWithDepartments for backward compatibility
                    phases = phasesWithDepts.map { it.phase }
                    
                    // Store phases with departments
                    _phasesWithDepartments.value = phasesWithDepts
                }
                
                if (project == null) {
                    _error.value = "Project not found"
                    _isLoading.value = false
                    return@launch
                }
                
                // Update project immediately so UI can show it
                _selectedProject.value = project
                _allPhases.value = phases
                
                // Calculate summary with phases first for faster display
                if (initialExpenses.isEmpty()) {
                    updateBudgetSummary(project, phases, emptyList())
                }
                
                _projectExpenses.value = initialExpenses
                updateBudgetSummary(project, phases, initialExpenses)
                resolveUserNames(initialExpenses)
                
                // Load approved + complete spent calculations (remaining = budget - spent)
                // IMPORTANT: Use getAllExpensesForProject to get ALL expenses (not filtered by user)
                coroutineScope {
                    val customerId = projectRepository.findCustomerIdForProject(projectId)
                    if (customerId != null) {
                        val phasesWithDeptsForBudget = _phasesWithDepartments.value
                        val allExpenses = expenseRepository.getAllExpensesForProject(projectId)
                        android.util.Log.d("ApproverProjectViewModel", "📊 Got ${allExpenses.size} total expenses for spent/remaining calculation")

                        _phaseBudgetMap.value = loadPhaseBudgets(projectId, customerId, phasesWithDeptsForBudget, phases, allExpenses)
                        _phaseDepartmentSpentMap.value = loadPhaseDepartmentSpent(projectId, customerId, allExpenses)
                        _phaseAnonymousExpensesMap.value = loadPhaseAnonymousExpenses(projectId, customerId, allExpenses)
                        
                        android.util.Log.d("ApproverProjectViewModel", "✅ Approved spent calculations completed")
                        android.util.Log.d("ApproverProjectViewModel", "   - Phase budgets: ${_phaseBudgetMap.value.size} phases")
                        android.util.Log.d("ApproverProjectViewModel", "   - Department spent: ${_phaseDepartmentSpentMap.value.size} phases")
                        android.util.Log.d("ApproverProjectViewModel", "   - Anonymous expenses: ${_phaseAnonymousExpensesMap.value.size} phases")
                    } else {
                        android.util.Log.w("ApproverProjectViewModel", "⚠️ Could not find customerId for project $projectId")
                    }
                }
                
                // Set up real-time listener for ongoing updates (non-blocking)
                // Only update expenses and summary, don't reload project/phases unless needed
                launch {
                    val userPhone = projectRepository.getCurrentUserIdentifier()?.replace("+91", "")?.trim()
                    // Determine which expenses to fetch based on assignments
                    val expensesFlow = if (userPhone != null) {
                        val currentProject = _selectedProject.value ?: project
                        val assignedDepts = currentProject?.departmentApproverAssignments
                            ?.filter { (_, approverPhone) -> approverPhone.replace("+91", "").trim() == userPhone }
                            ?.keys?.toList() ?: emptyList()
                            
                        if (assignedDepts.isNotEmpty()) {
                            expenseRepository.getExpensesForApprover(projectId, assignedDepts)
                        } else {
                            android.util.Log.d("ApproverProjectViewModel123", "👤 No departments assigned, fetching own expenses")
                            expenseRepository.getProjectExpenses(projectId)
                        }
                    } else {
                        expenseRepository.getProjectExpenses(projectId)
                    }

                    expensesFlow
                        .collect { expenses ->
                            _projectExpenses.value = expenses
                            // Use cached project and phases for faster updates
                            val currentProject = _selectedProject.value ?: project
                            val currentPhases = _allPhases.value.ifEmpty { phases }
                            updateBudgetSummary(currentProject, currentPhases, expenses)
                            resolveUserNames(expenses)
                            
                            // Update approved spent calculations
                            // IMPORTANT: Use getAllExpensesForProject to get ALL expenses (not filtered by user)
                            val customerId = projectRepository.findCustomerIdForProject(projectId)
                            if (customerId != null) {
                                val phasesWithDepts = _phasesWithDepartments.value
                                
                                // Get ALL expenses for accurate calculations (not just user's expenses)
                                val allExpenses = expenseRepository.getAllExpensesForProject(projectId)
                                android.util.Log.d("ApproverProjectViewModel", "🔄 Updating approved spent calculations with ${allExpenses.size} total expenses")
                                
                                val currentPhases = _allPhases.value.ifEmpty { phases }
                                _phaseBudgetMap.value = loadPhaseBudgets(projectId, customerId, phasesWithDepts, currentPhases, allExpenses)
                                _phaseDepartmentSpentMap.value = loadPhaseDepartmentSpent(projectId, customerId, allExpenses)
                                _phaseAnonymousExpensesMap.value = loadPhaseAnonymousExpenses(projectId, customerId, allExpenses)
                                
                                android.util.Log.d("ApproverProjectViewModel", "✅ Approved spent calculations updated")
                            }
                        }
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load project data"
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun updateBudgetSummary(project: Project, phases: List<Phase>, expenses: List<Expense>) {
        try {
            // Use departments from subcollection instead of phase.departments dictionary
            val phasesWithDeptsList = _phasesWithDepartments.value
            
            // Calculate total budget from all enabled phases' departments from subcollection
            val totalBudget = if (phasesWithDeptsList.isNotEmpty()) {
                phasesWithDeptsList
                    .filter { it.phase.isEnabledValue } // Only count enabled phases
                    .sumOf { phaseWithDepts ->
                        phaseWithDepts.departments.sumOf { it.totalBudget }
                    }
            } else {
                // Fallback to project.budget if no phases
                project.budget
            }
            
            // Spent = APPROVED + COMPLETE (remaining = budget - spent)
            val spentExpenses = expenses.filter {
                it.status == ExpenseStatus.APPROVED || it.status == ExpenseStatus.COMPLETE
            }
            val totalSpent = spentExpenses.sumOf { it.amount }
            val totalRemaining = totalBudget - totalSpent
            val spentPercentage = if (totalBudget > 0) (totalSpent / totalBudget) * 100 else 0.0
            
            // Calculate category breakdown
            val categoryBreakdown = calculateCategoryBreakdown(expenses, totalBudget)
            
            // Build phase-wise breakdowns - FILTER: only show enabled phases in overview
            // Use departments from subcollection instead of phase.departments dictionary
            // phasesWithDeptsList is already declared above, reuse it
            val phaseBreakdowns = if (phasesWithDeptsList.isNotEmpty()) {
                phasesWithDeptsList
                    .filter { it.phase.isEnabledValue } // Only show enabled phases in overview
                    .map { phaseWithDepts ->
                        val phase = phaseWithDepts.phase
                        // Use departments from subcollection
                        val departmentsFromSubcollection = phaseWithDepts.departments
                        
                        // Build breakdown from actual department data
                        // Use approved spent calculation logic: only APPROVED expenses, exclude anonymous
                        val phaseDepartmentSpent = _phaseDepartmentSpentMap.value[phase.id] ?: emptyMap()
                        
                        val breakdown = departmentsFromSubcollection.map { dept ->
                            // Get spent amount using department key matching logic
                            val spent = getDepartmentSpentAmount(phase.id, dept.name)
                            
                            val remaining = dept.totalBudget - spent
                            val percentage = if (dept.totalBudget > 0) (spent / dept.totalBudget * 100) else 0.0
                            
                            DepartmentBudgetBreakdown(
                                department = dept.name,
                                budgetAllocated = dept.totalBudget,
                                spent = spent,
                                remaining = remaining,
                                percentage = percentage
                            )
                        }
                        
                        PhaseBreakdown(
                            phaseId = phase.id,
                            phaseTitle = if (phase.phaseName.isNotEmpty()) phase.phaseName else "Phase ${phase.phaseNumber}",
                            startDate = phase.startDate,
                            endDate = phase.endDate,
                            departments = breakdown,
                            isActive = phase.isEnabledValue // Keep isActive field name for PhaseBreakdown compatibility
                        )
                    }
            } else emptyList()

            // Collect all anonymous departments from all phases
            // Note: Anonymous departments are no longer tracked in phase.isanonymous
            // They are handled by checking if department exists in subcollection
            val allAnonymousDepartments = emptySet<String>()
            
            val departmentBreakdown = if (phaseBreakdowns.isNotEmpty()) {
                // aggregate across phases
                val merged: MutableMap<String, Double> = mutableMapOf()
                phaseBreakdowns.forEach { pb ->
                    pb.departments.forEach { d ->
                        merged[d.department] = (merged[d.department] ?: 0.0) + d.budgetAllocated
                    }
                }
                // Pass empty anonymous departments map since we're using subcollection
                calculateDepartmentBreakdownFromMap(expenses, merged, emptyMap(), emptyMap())
            } else {
                calculateDepartmentBreakdown(expenses, project)
            }
            // Get recent expenses (last 5)
            val recentExpenses = expenses.sortedByDescending { 
                it.submittedAt?.toDate()?.time ?: 0L 
            }.take(5)
            
            // Count pending and approved
            val pendingCount = expenses.count { it.status.name == "PENDING" }
            val approvedCount = expenses.count { it.status.name == "APPROVED" }
            
            val summary = ProjectBudgetSummary(
                project = project,
                totalBudget = totalBudget,
                totalSpent = totalSpent,
                totalRemaining = totalRemaining,
                spentPercentage = spentPercentage,
                categoryBreakdown = categoryBreakdown,
                departmentBreakdown = departmentBreakdown,
                recentExpenses = recentExpenses,
                pendingApprovalsCount = pendingCount,
                approvedExpensesCount = approvedCount
            )
            
            _projectBudgetSummary.value = summary
            _phaseBreakdowns.value = phaseBreakdowns
        } catch (e: Exception) {
            // Error handled silently
        }
    }
    
    /**
     * Filter phase breakdowns to only include ongoing phases (today within [start, end] date range)
     * This calculation is moved to ViewModel and runs off UI thread to prevent lag
     */
    private fun filterOngoingPhases(breakdowns: List<PhaseBreakdown>): List<PhaseBreakdown> {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val today = java.util.Calendar.getInstance().time
        
        return breakdowns.filter { pb ->
            try {
                val start = pb.startDate?.let { sdf.parse(it) }
                val end = pb.endDate?.let { sdf.parse(it) }
                val startsOk = start == null || !today.before(start)
                val endsOk = end == null || !today.after(end)
                startsOk && endsOk
            } catch (_: Exception) {
                true // If parsing fails, include the phase
            }
        }
    }
    
    private fun calculateCategoryBreakdown(expenses: List<Expense>, totalBudget: Double): List<CategoryBudget> {
        // Spent = APPROVED + COMPLETE (match remaining amount logic)
        val spentExpenses = expenses.filter {
            it.status == ExpenseStatus.APPROVED || it.status == ExpenseStatus.COMPLETE
        }
        val expensesByCategory = spentExpenses.groupBy { it.category.trim() }
            .filter { it.key.isNotEmpty() }

        if (expensesByCategory.isEmpty()) {
            return emptyList()
        }

        val totalSpent = spentExpenses.sumOf { it.amount }
        
        // Create dynamic category budget breakdown
        val result = expensesByCategory.map { (category, categoryExpenses) ->
            val spent = categoryExpenses.sumOf { it.amount }
            
            // Calculate allocated budget proportionally based on actual spending
            // If no expenses yet, use equal distribution; otherwise use proportional allocation
            val allocatedBudget = if (totalSpent > 0) {
                // Proportional allocation based on spending pattern
                val spendingRatio = spent / totalSpent
                totalBudget * spendingRatio
            } else {
                // Equal distribution if no spending yet
                totalBudget / expensesByCategory.size
            }
            
            val remaining = maxOf(0.0, allocatedBudget - spent) // Ensure non-negative
            val usedPercentage = if (allocatedBudget > 0) (spent / allocatedBudget) * 100 else 0.0
            
            CategoryBudget(
                category = category,
                budgetAllocated = allocatedBudget,
                spent = spent,
                remaining = remaining,
                percentage = usedPercentage
            )
        }.sortedByDescending { it.spent } // Sort by highest spending first
        
        return result
    }
    
    private fun calculateDepartmentBreakdown(expenses: List<Expense>, project: Project): List<DepartmentBudgetBreakdown> {
        // Spent = APPROVED + COMPLETE (match remaining amount logic)
        val spentExpenses = expenses.filter {
            it.status == ExpenseStatus.APPROVED || it.status == ExpenseStatus.COMPLETE
        }
        val spendByDepartment: Map<String, Double> = spentExpenses
            .groupBy { it.department.trim() }
            .filter { it.key.isNotEmpty() }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        // Build breakdown strictly from project's configured departmentBudgets
        val breakdownFromProjectBudgets = project.departmentBudgets?.entries?.map { (department, allocatedBudget) ->
            val spent = spendByDepartment[department] ?: 0.0
            val remaining = maxOf(0.0, allocatedBudget - spent)
            val usedPercentage = if (allocatedBudget > 0) (spent / allocatedBudget) * 100 else 0.0
            DepartmentBudgetBreakdown(
                department = department,
                budgetAllocated = allocatedBudget,
                spent = spent,
                remaining = remaining,
                percentage = usedPercentage
            )
        } ?: emptyList()

        // Optionally include departments that have spend but no configured budget (allocated = 0)
        val extras = spendByDepartment.keys
            .filter { it.isNotEmpty() && !(project.departmentBudgets?.containsKey(it) ?: false) }
            .map { department ->
                val spent = spendByDepartment[department] ?: 0.0
                DepartmentBudgetBreakdown(
                    department = department,
                    budgetAllocated = 0.0,
                    spent = spent,
                    remaining = 0.0,
                    percentage = 0.0
                )
            }

        val result = (breakdownFromProjectBudgets + extras)
            .sortedByDescending { it.spent }

        return result
    }

    private fun calculateDepartmentBreakdownFromMap(
        expenses: List<Expense>, 
        departmentBudgets: Map<String, Double>,
        anonymousDepartments: Map<String, Boolean> = emptyMap(),
        departmentNameMap: Map<String, String> = emptyMap()
    ): List<DepartmentBudgetBreakdown> {
        val spentExpenses = expenses.filter {
            it.status == ExpenseStatus.APPROVED || it.status == ExpenseStatus.COMPLETE
        }
        val spendByDepartment: Map<String, Double> = spentExpenses
            .groupBy { it.department.trim() }
            .filter { it.key.isNotEmpty() }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        // Normalize department names to avoid duplicates (case-insensitive, trim whitespace)
        val normalizedDepartmentBudgets = departmentBudgets.entries
            .groupBy { (dept, _) -> dept.trim().lowercase() }
            .map { (normalizedKey, entries) ->
                // Sum budgets if same department appears multiple times with different cases
                val totalBudget = entries.sumOf { it.value }
                // Use the first occurrence's original name (preserve case)
                val originalName = entries.first().key
                originalName to totalBudget
            }
            .toMap()

        val breakdown = normalizedDepartmentBudgets.entries.map { (departmentId, allocatedBudget) ->
            // Get department name from map, or use ID if name not found
            val departmentName = departmentNameMap[departmentId] ?: departmentId
            // Match spending by normalized name (case-insensitive) - try both ID and name
            val normalizedDept = departmentId.trim().lowercase()
            val normalizedName = departmentName.trim().lowercase()
            val spent = spendByDepartment.entries
                .firstOrNull { 
                    val key = it.key.trim().lowercase()
                    key == normalizedDept || key == normalizedName
                }
                ?.value ?: 0.0
            val remaining = maxOf(0.0, allocatedBudget - spent)
            val usedPercentage = if (allocatedBudget > 0) (spent / allocatedBudget) * 100 else 0.0
            DepartmentBudgetBreakdown(
                department = departmentName, // Use name instead of ID
                budgetAllocated = allocatedBudget,
                spent = spent,
                remaining = remaining,
                percentage = usedPercentage
            )
        }

        // Group expenses from anonymous (deleted) departments under "Others"
        val normalizedBudgetKeys = normalizedDepartmentBudgets.keys.map { it.trim().lowercase() }.toSet()
        val normalizedAnonymousKeys = anonymousDepartments.filter { it.value == true }
            .keys.map { it.trim().lowercase() }.toSet()
        
        // Calculate "Others" category: expenses from anonymous departments
        val othersExpenses = spendByDepartment.entries
            .filter { (dept, _) ->
                val normalizedDept = dept.trim().lowercase()
                normalizedAnonymousKeys.contains(normalizedDept)
            }
        val othersSpent = othersExpenses.sumOf { it.value }
        
        // Include departments with spend but no budget (normalized matching)
        // Exclude anonymous departments as they go to "Others"
        val extras = spendByDepartment.entries
            .filter { (dept, _) ->
                val normalizedDept = dept.trim().lowercase()
                dept.isNotEmpty() && 
                !normalizedBudgetKeys.contains(normalizedDept) &&
                !normalizedAnonymousKeys.contains(normalizedDept)
            }
            .map { (department, spent) ->
                DepartmentBudgetBreakdown(
                    department = department,
                    budgetAllocated = 0.0,
                    spent = spent,
                    remaining = 0.0,
                    percentage = 0.0
                )
            }

        // Add "Others" category if there are expenses from anonymous departments
        val othersCategory = if (othersSpent > 0) {
            listOf(
                DepartmentBudgetBreakdown(
                    department = "Others",
                    budgetAllocated = 0.0,
                    spent = othersSpent,
                    remaining = 0.0,
                    percentage = 0.0
                )
            )
        } else {
            emptyList()
        }

        // Deduplicate final result by department name (case-insensitive)
        val deduplicated = (breakdown + extras + othersCategory)
            .groupBy { it.department.trim().lowercase() }
            .map { (_, deptList) ->
                if (deptList.size > 1) {
                    // Merge duplicates
                    deptList.reduce { acc, dept ->
                        DepartmentBudgetBreakdown(
                            department = acc.department, // Use first occurrence's name
                            budgetAllocated = acc.budgetAllocated + dept.budgetAllocated,
                            spent = acc.spent + dept.spent,
                            remaining = (acc.budgetAllocated + dept.budgetAllocated) - (acc.spent + dept.spent),
                            percentage = if ((acc.budgetAllocated + dept.budgetAllocated) > 0) {
                                ((acc.spent + dept.spent) / (acc.budgetAllocated + dept.budgetAllocated)) * 100
                            } else 0.0
                        )
                    }
                } else {
                    deptList.first()
                }
            }

        return deduplicated.sortedByDescending { it.spent }
    }
    
    fun refreshProjectData() {
        _selectedProject.value?.let { project ->
            project.id?.let { loadProjectBudgetSummary(it, forceRefresh = true) }
        }
    }
    
    suspend fun canDeleteDepartment(projectId: String, departmentName: String): Boolean {
        return projectRepository.canDeleteDepartment(projectId, departmentName)
    }
    
    fun deleteDepartment(projectId: String, departmentName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = projectRepository.deleteDepartmentFromAllPhases(projectId, departmentName)
                if (result.isSuccess) {
                    // Refresh project data
                    loadProjectBudgetSummary(projectId, forceRefresh = true)
                    onSuccess()
                } else {
                    onError(result.exceptionOrNull()?.message ?: "Failed to delete department")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error deleting department")
            }
        }
    }
    
    suspend fun updateDepartmentBudget(projectId: String, phaseId: String, departmentName: String, newBudget: Double): Result<Unit> {
        return try {
            val result = projectRepository.updateDepartmentBudgetInPhase(projectId, phaseId, departmentName, newBudget)
            if (result.isSuccess) {
                loadProjectBudgetSummary(projectId, forceRefresh = true)
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update department with new line items. Recomputes department total from line items,
     * saves to Firebase, then recalculates project total budget (before/after is handled by repo).
     */
    suspend fun updateDepartmentWithLineItems(projectId: String, phaseId: String, department: Department): Result<Unit> {
        return try {
            val customerId = projectRepository.findCustomerIdForProject(projectId)
                ?: return Result.failure(Exception("Could not find customer for project"))
            
            val result = projectRepository.saveDepartmentToPhase(
                customerId = customerId,
                projectId = projectId,
                phaseId = phaseId,
                department = department
            )
            if (result.isSuccess) {
                // Refresh phases/departments from server so UI (e.g. DepartmentDetailScreen) sees updated data
                _phasesWithDepartments.value = getScopedPhasesWithDepartments(projectId, Source.SERVER)
                loadProjectBudgetSummary(projectId, forceRefresh = true)
            }
            if (result.isSuccess) Result.success(Unit) else Result.failure(result.exceptionOrNull()!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateDepartmentBudgetInAllPhases(projectId: String, departmentName: String, newBudget: Double): Result<Unit> {
        return try {
            // Use departments from subcollection instead of phase.departments dictionary
            val phasesWithDeptsList = _phasesWithDepartments.value.ifEmpty {
                getScopedPhasesWithDepartments(projectId)
            }
            
            // Find departments with matching name across all phases
            val results = phasesWithDeptsList.mapNotNull { phaseWithDepts ->
                val matchingDept = phaseWithDepts.departments.firstOrNull { 
                    it.name.trim().equals(departmentName.trim(), ignoreCase = true) 
                }
                
                if (matchingDept != null) {
                    projectRepository.updateDepartmentBudgetInPhase(projectId, phaseWithDepts.phase.id, matchingDept.name, newBudget)
                } else {
                    null
                }
            }
            
            if (results.isNotEmpty() && results.all { it.isSuccess }) {
                // Refresh project data to get updated budgets
                loadProjectBudgetSummary(projectId, forceRefresh = true)
                Result.success(Unit)
            } else {
                val firstFailure = results.firstOrNull { it.isFailure }
                val errorMsg = firstFailure?.exceptionOrNull()?.message ?: 
                    if (results.isEmpty()) "Department not found in any phase" else "Failed to update budget in some phases"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPhasesForProject(projectId: String) = getScopedPhases(projectId)
    
    suspend fun getDepartmentNamesByIds(departmentIds: List<String>) = projectRepository.getDepartmentNamesByIds(departmentIds)
    
    /**
     * Optimized refresh that updates UI immediately with new budget values
     * Uses cached data and optimistically updates the UI
     */
    private suspend fun refreshPhasesAndSummaryOptimized(
        projectId: String, 
        departmentName: String, 
        newBudget: Double,
        updatedPhases: List<Phase>
    ) {
        try {
            // Get current project and expenses from cache (no network calls)
            val project = _selectedProject.value
            if (project == null) {
                refreshPhasesAndSummary(projectId)
                return
            }
            
            // Update phases in state immediately
            _allPhases.value = updatedPhases
            
            // Use cached expenses (no network call)
            val expenses = _projectExpenses.value
            if (expenses.isEmpty()) {
                // Only fetch if cache is empty
                val fetchedExpenses = expenseRepository.getProjectExpenses(projectId).first()
                _projectExpenses.value = fetchedExpenses
                updateBudgetSummary(project, updatedPhases, fetchedExpenses)
            } else {
                // Update summary immediately with cached data
                updateBudgetSummary(project, updatedPhases, expenses)
            }
            
        } catch (e: Exception) {
            // Fallback to full refresh if fast refresh fails
            refreshPhasesAndSummary(projectId)
        }
    }
    
    /**
     * Full refresh that reloads everything from repository
     * Used as fallback when fast refresh fails
     */
    private suspend fun refreshPhasesAndSummary(projectId: String) {
        try {
            // Get current project and expenses
            val project = _selectedProject.value ?: projectRepository.getProjectById(projectId)
            if (project == null) {
                return
            }
            
            // Reload phases
            val updatedPhases = getScopedPhases(projectId)
            _allPhases.value = updatedPhases
            
            // Get current expenses
            val expenses = _projectExpenses.value.ifEmpty { 
                expenseRepository.getProjectExpenses(projectId).first() 
            }
            
            // Update summary
            updateBudgetSummary(project, updatedPhases, expenses)
            
        } catch (e: Exception) {
        }
    }

    fun updatePhaseEnabled(projectId: String, phaseId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val result = projectRepository.updatePhaseEnabled(projectId, phaseId, isEnabled)
            if (result.isSuccess) {
                loadProjectBudgetSummary(projectId, forceRefresh = true)
            } else {
            }
        }
    }
    
    /**
     * Complete a phase by setting end date to today and disabling it
     */
    fun completePhase(projectId: String, phaseId: String, endDate: String) {
        viewModelScope.launch {
            try {
                val result = projectRepository.completePhase(projectId, phaseId, endDate)
                if (result.isSuccess) {
                    // Force reload phases to get updated endDate and isEnabled
                    val phases = getScopedPhases(projectId)
                    _allPhases.value = phases
                    
                    // Also refresh project budget summary
                    loadProjectBudgetSummary(projectId, forceRefresh = true)
                } else {
                    val error = result.exceptionOrNull()
                }
            } catch (e: Exception) {
            }
        }
    }
    
    /**
     * Update department assignments in project document
     */
    fun updateDepartmentAssignments(
        projectId: String,
        phaseId: String,
        departmentName: String,
        approverPhone: String?,
        userPhone: String?
    ) {
        viewModelScope.launch {
            try {
                android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
                android.util.Log.d("DEPT_ADD_FLOW", "📋 Step 4: ViewModel.updateDepartmentAssignments() Called")
                android.util.Log.d("DEPT_ADD_FLOW", "   (Adding department to EXISTING phase)")
                android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
                android.util.Log.d("DEPT_ADD_FLOW", "   - Project ID: $projectId")
                android.util.Log.d("DEPT_ADD_FLOW", "   - Phase ID: $phaseId")
                android.util.Log.d("DEPT_ADD_FLOW", "   - Department name: '$departmentName'")
                android.util.Log.d("DEPT_ADD_FLOW", "   - Approver phone: $approverPhone")
                android.util.Log.d("DEPT_ADD_FLOW", "   - User phone: $userPhone")
                
                val result = projectRepository.updateProjectDepartmentAssignments(
                    projectId = projectId,
                    departmentName = departmentName,
                    approverPhone = approverPhone,
                    userPhone = userPhone
                )
                if (result.isSuccess) {
                    android.util.Log.d("DEPT_ADD_FLOW", "   ✅ Step 4A completed successfully")
                    
                    android.util.Log.d("DEPT_ADD_FLOW", "   ────────────────────────────────────────────────────────────")
                    android.util.Log.d("DEPT_ADD_FLOW", "   🔄 Step 4B: Updating User_Customer_Project_Phase_Relation...")
                    android.util.Log.d("DEPT_ADD_FLOW", "   ℹ️ Calling updateUserCustomerProjectPhaseRelationForDepartment")
                    
                    // Update User_Customer_Project_Phase_Relation collection
                    val relationResult = projectRepository.updateUserCustomerProjectPhaseRelationForDepartment(
                        projectId = projectId,
                        phaseId = phaseId,
                        departmentName = departmentName,
                        userPhone = userPhone,
                        approverPhone = approverPhone
                    )
                    if (relationResult.isSuccess) {
                        android.util.Log.d("DEPT_ADD_FLOW", "   ✅ Step 4B completed successfully")
                        android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
                        android.util.Log.d("DEPT_ADD_FLOW", "🎉 COMPLETE: Department '$departmentName' assignments saved")
                        android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
                    } else {
                        android.util.Log.e("DEPT_ADD_FLOW", "   ❌ Step 4B FAILED: ${relationResult.exceptionOrNull()?.message}")
                        android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
                    }
                } else {
                    android.util.Log.e("DEPT_ADD_FLOW", "   ❌ Step 4A FAILED: ${result.exceptionOrNull()?.message}")
                    android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
                }
            } catch (e: Exception) {
                android.util.Log.e("DEPT_ADD_FLOW", "❌ EXCEPTION in updateDepartmentAssignments: ${e.message}", e)
                android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
            }
        }
    }
    
    fun addPhase(
        customerId: String,
        projectId: String,
        phaseName: String,
        startDate: String,
        endDate: String,
        departmentDrafts: List<com.cubiquitous.tracura.model.DepartmentDraft>,
        departmentAssignments: Map<String, Pair<String?, String?>> = emptyMap(), // Map of deptName to (approverPhone, userPhone)
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                // Debug logging at function entry
                android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
                android.util.Log.d("DEPT_ADD_FLOW", "📋 Step 4: ViewModel.addPhase() Called")
                android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
                android.util.Log.d("DEPT_ADD_FLOW", "   - Phase name: '$phaseName'")
                android.util.Log.d("DEPT_ADD_FLOW", "   - Customer ID: $customerId")
                android.util.Log.d("DEPT_ADD_FLOW", "   - Project ID: $projectId")
                android.util.Log.d("DEPT_ADD_FLOW", "   - Start date: $startDate")
                android.util.Log.d("DEPT_ADD_FLOW", "   - End date: $endDate")
                android.util.Log.d("DEPT_ADD_FLOW", "   - Departments (${departmentDrafts.size}): ${departmentDrafts.map { it.departmentName }.joinToString()}")
                android.util.Log.d("DEPT_ADD_FLOW", "   - Department assignments (${departmentAssignments.size}):")
                departmentAssignments.forEach { (dept, phones) ->
                    android.util.Log.d("DEPT_ADD_FLOW", "      • Dept: '$dept', Approver: ${phones.first}, User: ${phones.second}")
                }
                
                // Get existing phases to determine next phase number
                android.util.Log.d("DEPT_ADD_FLOW", "   🔍 Determining next phase number...")
                val existingPhases = projectRepository.getPhasesForProject(projectId)
                val nextPhaseNumber = if (existingPhases.isEmpty()) {
                    1
                } else {
                    existingPhases.maxOfOrNull { it.phaseNumber }?.plus(1) ?: 1
                }
                android.util.Log.d("DEPT_ADD_FLOW", "   📊 Next phase number will be: $nextPhaseNumber (found ${existingPhases.size} existing phases)")
                
                val newPhase = com.cubiquitous.tracura.model.Phase(
                    phaseName = phaseName,
                    phaseNumber = nextPhaseNumber,
                    startDate = startDate,
                    endDate = endDate,
                    departments = departmentDrafts.associate { it.departmentName to it.totalBudget },
                    categories = emptyList(),
                    isEnabled = true // New phases are enabled by default
                )
                
                // Use customerId to add phase under customers/{customerId}/Projects/{projectId}/phases
                android.util.Log.d("DEPT_ADD_FLOW", "   💾 Creating phase in Firestore...")
                android.util.Log.d("DEPT_ADD_FLOW", "   📍 Path: customers/$customerId/projects/$projectId/phases")
                val result = projectRepository.addPhaseToProject(customerId, projectId, newPhase, departmentDrafts)
                if (result.isSuccess) {
                    val phaseId = result.getOrNull()
                    android.util.Log.d("DEPT_ADD_FLOW", "   ✅ Phase created successfully with ID: $phaseId")
                    
                    // If there are department assignments, save them first
                    if (phaseId != null && departmentAssignments.isNotEmpty()) {
                        android.util.Log.d("DEPT_ADD_FLOW", "   ────────────────────────────────────────────────────────────")
                        android.util.Log.d("DEPT_ADD_FLOW", "   🔄 Saving ${departmentAssignments.size} department assignments...")
                        
                        // Per AddDepartment.md: project-level assignments are stored WITHOUT phase prefix
                        // e.g., departmentUserAssignments["Plumbing"] = "phone", NOT "phaseId_Plumbing"
                        departmentAssignments.entries.forEachIndexed { index, entry ->
                            val deptName = entry.key
                            val (approverPhone, userPhone) = entry.value
                            
                            android.util.Log.d("DEPT_ADD_FLOW", "   [${index + 1}/${departmentAssignments.size}] Processing department: '$deptName'")
                            android.util.Log.d("DEPT_ADD_FLOW", "      - Approver phone: $approverPhone")
                            android.util.Log.d("DEPT_ADD_FLOW", "      - User phone: $userPhone")
                            
                            // Update project-level assignments (NO prefix)
                            android.util.Log.d("DEPT_ADD_FLOW", "      🔄 Calling updateProjectDepartmentAssignments...")
                            val assignmentResult = projectRepository.updateProjectDepartmentAssignments(
                                projectId = projectId,
                                departmentName = deptName, // Use unprefixed name
                                approverPhone = approverPhone,
                                userPhone = userPhone
                            )
                            
                            if (assignmentResult.isSuccess) {
                                android.util.Log.d("DEPT_ADD_FLOW", "      ✅ Saved project-level assignments for '$deptName'")
                            } else {
                                android.util.Log.e("DEPT_ADD_FLOW", "      ❌ Failed to save assignments for '$deptName': ${assignmentResult.exceptionOrNull()?.message}")
                            }
                        }
                        
                        android.util.Log.d("DEPT_ADD_FLOW", "   ────────────────────────────────────────────────────────────")
                        // Then update User_Customer_Project_Phase_Relation using iOS logic
                        // This collects phones from BOTH new assignments AND existing project-level assignments
                        android.util.Log.d("DEPT_ADD_FLOW", "   🔄 Updating User_Customer_Project_Phase_Relation...")
                        android.util.Log.d("DEPT_ADD_FLOW", "   ℹ️ This will collect phones from BOTH new AND existing project-level assignments")
                        val relationResult = projectRepository.updateUserCustomerProjectPhaseRelationForNewPhase(
                            customerId = customerId,
                            projectId = projectId,
                            phaseId = phaseId,
                            departments = departmentDrafts.associate { it.departmentName to it.totalBudget },
                            departmentAssignments = departmentAssignments
                        )
                        
                        if (relationResult.isSuccess) {
                            android.util.Log.d("DEPT_ADD_FLOW", "   ✅ Successfully updated User_Customer_Project_Phase_Relation")
                        } else {
                            android.util.Log.e("DEPT_ADD_FLOW", "   ❌ Failed to update User_Customer_Project_Phase_Relation: ${relationResult.exceptionOrNull()?.message}")
                        }
                    } else {
                        android.util.Log.d("DEPT_ADD_FLOW", "   ⚠️ No department assignments to save (phaseId=$phaseId, assignments count=${departmentAssignments.size})")
                    }
                    
                    // Refresh project data
                    android.util.Log.d("DEPT_ADD_FLOW", "   🔄 Refreshing project budget summary...")
                    loadProjectBudgetSummary(projectId, forceRefresh = true)
                    android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
                    android.util.Log.d("DEPT_ADD_FLOW", "🎉 COMPLETE: Phase '$phaseName' added successfully (ID: $phaseId)")
                    android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
                    onComplete(true, phaseId)
                } else {
                    android.util.Log.e("DEPT_ADD_FLOW", "❌ ERROR: Failed to create phase: ${result.exceptionOrNull()?.message}")
                    android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
                    onComplete(false, null)
                }
            } catch (e: Exception) {
                android.util.Log.e("DEPT_ADD_FLOW", "❌ EXCEPTION: Error creating phase: ${e.message}", e)
                android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
                onComplete(false, null)
            }
        }
    }
    
    fun updatePhaseDetails(
        projectId: String,
        phaseId: String,
        phaseName: String,
        startDate: String?,
        endDate: String?
    ) {
        viewModelScope.launch {
            try {
                val result = projectRepository.updatePhaseDetails(
                    projectId = projectId,
                    phaseId = phaseId,
                    phaseName = phaseName,
                    startDate = startDate,
                    endDate = endDate
                )
                if (result.isSuccess) {
                    // Force reload phases to get updated dates and isEnabled
                    val phases = getScopedPhases(projectId)
                    _allPhases.value = phases
                    
                    // Also refresh project budget summary
                    loadProjectBudgetSummary(projectId, forceRefresh = true)
                } else {
                }
            } catch (e: Exception) {
            }
        }
    }
    
    /**
     * Load all pending phase requests for a project
     */
    fun loadPhaseRequests(projectId: String) {
        viewModelScope.launch {
            try {
                val requests = projectRepository.getAllPendingPhaseRequests(projectId)
                _phaseRequests.value = requests
                _phaseRequestCount.value = requests.size
            } catch (e: Exception) {
                _phaseRequests.value = emptyList()
                _phaseRequestCount.value = 0
            }
        }
    }
    
    /**
     * Load phase request count only (faster, for badge display)
     */
    fun loadPhaseRequestCount(projectId: String) {
        // Validate projectId to prevent crashes
        if (projectId.isBlank()) {
            android.util.Log.e("ApproverProjectViewModel", "Cannot load phase request count: projectId is empty")
            _phaseRequestCount.value = 0
            return
        }
        
        viewModelScope.launch {
            try {
                val requests = projectRepository.getAllPendingPhaseRequests(projectId)
                _phaseRequestCount.value = requests.size
                // Also update the full list if not already loaded
                if (_phaseRequests.value.isEmpty()) {
                    _phaseRequests.value = requests
                }
            } catch (e: Exception) {
                _phaseRequestCount.value = 0
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
        reason: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = projectRepository.approvePhaseRequest(
                    projectId = projectId,
                    phaseId = phaseId,
                    requestId = requestId,
                    extendedDate = extendedDate,
                    reason = reason
                )
                if (result.isSuccess) {
                    // Refresh the count and list
                    loadPhaseRequests(projectId)
                    onComplete()
                } else {
                }
            } catch (e: Exception) {
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
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = projectRepository.rejectPhaseRequest(
                    projectId = projectId,
                    phaseId = phaseId,
                    requestId = requestId,
                    reason = reason
                )
                if (result.isSuccess) {
                    // Refresh the count and list
                    loadPhaseRequests(projectId)
                    onComplete()
                } else {
                }
            } catch (e: Exception) {
            }
        }
    }
    
    /**
     * Check if a phase has an approved override request
     */
    suspend fun hasApprovedPhaseRequest(projectId: String, phaseId: String): Boolean {
        return try {
            projectRepository.hasApprovedPhaseRequest(projectId, phaseId)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Update project status (e.g., for approval/rejection)
     */
    suspend fun updateProjectStatus(projectId: String, newStatus: String, rejectionReason: String? = null, rejectedBy: String? = null): Result<Unit> {
        return try {
            val result = projectRepository.updateProjectStatus(projectId, newStatus, rejectionReason, rejectedBy)
            if (result.isSuccess) {
                // Reload project data to reflect status change
                loadProjectBudgetSummary(projectId, forceRefresh = true)
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== Approved Spent Calculation Methods ==========
    
    /**
     * Load phase budgets - calculates total budget and spent amount per phase.
     * Spent = APPROVED + COMPLETE expenses (so remaining = budget - spent is correct).
     *
     * @param projectId The project ID
     * @param customerId The customer ID
     * @param phasesWithDepts List of phases with their departments
     * @param allPhases All phases for fallback
     * @param expenses All project expenses (spent = APPROVED + COMPLETE)
     * @return Map of phaseId to PhaseBudget
     */
    private suspend fun loadPhaseBudgets(
        projectId: String,
        customerId: String,
        phasesWithDepts: List<PhaseWithDepartments>,
        allPhases: List<Phase> = emptyList(),
        expenses: List<Expense> = emptyList()
    ): Map<String, com.cubiquitous.tracura.model.PhaseBudget> {
        return try {
            android.util.Log.d("ApproverProjectViewModel", "🔄 Loading phase budgets for project: $projectId")

            // Spent = APPROVED + COMPLETE (remaining = budget - spent)
            val spentExpenses = expenses.filter {
                it.status == ExpenseStatus.APPROVED || it.status == ExpenseStatus.COMPLETE
            }
            android.util.Log.d("ApproverProjectViewModel", "📊 Found ${spentExpenses.size} approved+complete expenses for spent")

            // Aggregate by phaseId (both department and anonymous)
            val phaseSpentMap = mutableMapOf<String, Double>()
            spentExpenses.forEach { expense ->
                val phaseId = expense.phaseId
                if (phaseId.isNullOrBlank()) {
                    android.util.Log.w("ApproverProjectViewModel", "⚠️ Expense ${expense.id} has no phaseId, skipping")
                    return@forEach
                }
                phaseSpentMap[phaseId] = (phaseSpentMap[phaseId] ?: 0.0) + expense.amount
                android.util.Log.d("ApproverProjectViewModel", "💰 Adding expense to phase $phaseId: ₹${expense.amount} (total now: ${phaseSpentMap[phaseId]})")
            }
            
            android.util.Log.d("ApproverProjectViewModel", "💰 Phase spent amounts: $phaseSpentMap")
            
            // Step 3: Use provided allPhases or fallback to _allPhases.value
            val phasesToProcess = if (allPhases.isNotEmpty()) allPhases else _allPhases.value
            android.util.Log.d("ApproverProjectViewModel", "📋 Found ${phasesToProcess.size} total phases, ${phasesWithDepts.size} phases with departments")
            
            // Step 4: Create a map of phaseId to PhaseWithDepartments for quick lookup
            val phasesWithDeptsMap = phasesWithDepts.associateBy { it.phase.id }
            
            // Step 5: Calculate total budget and create PhaseBudget for each phase
            // Include ALL phases (from allPhases) to ensure phases with expenses are included
            val budgetMap = mutableMapOf<String, com.cubiquitous.tracura.model.PhaseBudget>()
            
            // First, process phases with departments
            phasesWithDepts.forEach { phaseWithDepts ->
                val phase = phaseWithDepts.phase
                
                // Calculate total budget from departments
                // Priority: Use departmentList if available, otherwise fallback to departments dictionary
                val totalBudget = if (phaseWithDepts.departments.isNotEmpty()) {
                    // Sum budgets from departments subcollection
                    phaseWithDepts.departments.sumOf { it.totalBudget }
                } else {
                    // Fallback to phase.departments dictionary
                    phase.departments.values.sum()
                }
                
                // Get spent amount for this phase (default to 0 if no expenses)
                val spent = phaseSpentMap[phase.id] ?: 0.0
                
                // Create PhaseBudget object
                budgetMap[phase.id] = PhaseBudget(
                    id = phase.id,
                    totalBudget = totalBudget,
                    spent = spent
                )
                
                android.util.Log.d("ApproverProjectViewModel", "📊 Phase ${phase.id}: Budget=₹$totalBudget, Spent=₹$spent, Remaining=₹${totalBudget - spent}")
            }
            
            // Then, process phases that have expenses but might not be in phasesWithDepts
            // This ensures all phases with expenses are included in the budgetMap
            phasesToProcess.forEach { phase ->
                // Skip if already processed
                if (budgetMap.containsKey(phase.id)) {
                    return@forEach
                }
                
                // Check if this phase has expenses
                val spent = phaseSpentMap[phase.id] ?: 0.0
                if (spent > 0.0 || phaseSpentMap.containsKey(phase.id)) {
                    // Calculate total budget from phase.departments dictionary (fallback)
                    val totalBudget = phase.departments.values.sum()
                    
                    // Create PhaseBudget object even if no departments yet (for phases with expenses)
                    budgetMap[phase.id] = PhaseBudget(
                        id = phase.id,
                        totalBudget = totalBudget,
                        spent = spent
                    )
                    
                    android.util.Log.d("ApproverProjectViewModel", "📊 Phase ${phase.id} (no departments): Budget=₹$totalBudget, Spent=₹$spent, Remaining=₹${totalBudget - spent}")
                }
            }
            
            // Also include any phases that have expenses but aren't in allPhases yet (edge case)
            phaseSpentMap.keys.forEach { phaseId ->
                if (!budgetMap.containsKey(phaseId)) {
                    val spent = phaseSpentMap[phaseId] ?: 0.0
                    budgetMap[phaseId] = PhaseBudget(
                        id = phaseId,
                        totalBudget = 0.0, // Unknown budget if phase not found
                        spent = spent
                    )
                    android.util.Log.w("ApproverProjectViewModel", "⚠️ Phase $phaseId has expenses (₹$spent) but phase not found in allPhases")
                }
            }
            
            android.util.Log.d("ApproverProjectViewModel", "✅ Loaded ${budgetMap.size} phase budgets")
            budgetMap
        } catch (e: Exception) {
            android.util.Log.e("ApproverProjectViewModel", "❌ Error loading phase budgets: ${e.message}", e)
            emptyMap()
        }
    }
    
    /**
     * Load phase department spent - calculates spent amount per department per phase
     * Only APPROVED expenses are counted, excluding anonymous expenses
     * 
     * @param projectId The project ID
     * @param customerId The customer ID
     * @param expenses List of all expenses (will filter for approved)
     * @return Map structure: [phaseId: [departmentKey: spentAmount]]
     */
    private suspend fun loadPhaseDepartmentSpent(
        projectId: String,
        customerId: String,
        expenses: List<Expense>
    ): Map<String, Map<String, Double>> {
        return try {
            android.util.Log.d("ApproverProjectViewModel", "🔄 Loading phase department spent for project: $projectId")
            
            // Step 1: Filter APPROVED + COMPLETE expenses (excluding anonymous) for spent
            val spentExpenses = expenses.filter {
                (it.status == ExpenseStatus.APPROVED || it.status == ExpenseStatus.COMPLETE) &&
                it.isAnonymous != true
            }
            android.util.Log.d("ApproverProjectViewModel", "📊 Found ${spentExpenses.size} approved+complete non-anonymous expenses")

            // Step 2: Calculate spent amount per department per phase
            val departmentSpentMap = mutableMapOf<String, MutableMap<String, Double>>()

            spentExpenses.forEach { expense ->
                val phaseId = expense.phaseId ?: return@forEach
                
                // Initialize phase map if needed
                if (departmentSpentMap[phaseId] == null) {
                    departmentSpentMap[phaseId] = mutableMapOf()
                }
                
                // Step 3: Determine the correct department key
                // Expenses may be stored with format "phaseId_departmentName" or just "departmentName"
                val departmentKey = determineDepartmentKey(
                    expenseDepartment = expense.department,
                    phaseId = phaseId
                )
                
                // Step 4: Aggregate spent amount by department key
                val currentSpent = departmentSpentMap[phaseId]?.get(departmentKey) ?: 0.0
                departmentSpentMap[phaseId]?.put(departmentKey, currentSpent + expense.amount)
            }
            
            android.util.Log.d("ApproverProjectViewModel", "✅ Loaded department spent for ${departmentSpentMap.size} phases")
            departmentSpentMap
        } catch (e: Exception) {
            android.util.Log.e("ApproverProjectViewModel", "❌ Error loading phase department spent: ${e.message}", e)
            emptyMap()
        }
    }
    
    /**
     * Load anonymous expenses per phase
     * Only APPROVED anonymous expenses are counted
     * 
     * @param projectId The project ID
     * @param customerId The customer ID
     * @param expenses List of all expenses (will filter for approved anonymous)
     * @return Map of phaseId to total anonymous expense amount
     */
    private suspend fun loadPhaseAnonymousExpenses(
        projectId: String,
        customerId: String,
        expenses: List<Expense>
    ): Map<String, Double> {
        return try {
            android.util.Log.d("ApproverProjectViewModel", "🔄 Loading phase anonymous expenses for project: $projectId")
            
            // Filter APPROVED + COMPLETE anonymous expenses for spent
            val anonymousExpenses = expenses.filter {
                (it.status == ExpenseStatus.APPROVED || it.status == ExpenseStatus.COMPLETE) &&
                it.isAnonymous == true
            }
            android.util.Log.d("ApproverProjectViewModel", "📊 Found ${anonymousExpenses.size} approved+complete anonymous expenses")

            val anonymousMap = mutableMapOf<String, Double>()
            anonymousExpenses.forEach { expense ->
                val phaseId = expense.phaseId ?: return@forEach
                anonymousMap[phaseId] = (anonymousMap[phaseId] ?: 0.0) + expense.amount
            }
            
            android.util.Log.d("ApproverProjectViewModel", "✅ Loaded anonymous expenses for ${anonymousMap.size} phases")
            anonymousMap
        } catch (e: Exception) {
            android.util.Log.e("ApproverProjectViewModel", "❌ Error loading phase anonymous expenses: ${e.message}", e)
            emptyMap()
        }
    }
    
    /**
     * Determine the correct department key for an expense
     * 
     * Logic:
     * - If expense.department already has phaseId prefix (e.g., "phase123_Civil"), use it directly
     * - If expense.department is just the name (e.g., "Civil"), add phaseId prefix
     * 
     * This handles backward compatibility with expenses stored in different formats
     */
    private fun determineDepartmentKey(
        expenseDepartment: String,
        phaseId: String
    ): String {
        return if (expenseDepartment.startsWith("${phaseId}_")) {
            // Expense already has phaseId prefix - use it directly
            expenseDepartment
        } else {
            // Expense has just department name - add phaseId prefix to match phase format
            "${phaseId}_$expenseDepartment"
        }
    }
    
    /**
     * Get spent amount for a specific department in a phase
     * 
     * @param phaseId The phase ID
     * @param departmentName The department name (without phaseId prefix)
     * @return The spent amount for the department, or 0.0 if not found
     */
    fun getDepartmentSpentAmount(
        phaseId: String,
        departmentName: String
    ): Double {
        val phaseSpent = _phaseDepartmentSpentMap.value[phaseId] ?: return 0.0
        
        // Try multiple key formats for matching
        val possibleKeys = listOf(
            // Format 1: With phaseId prefix
            "${phaseId}_$departmentName",
            // Format 2: Just department name (backward compatibility)
            departmentName,
            // Format 3: Try matching by display name
            phaseSpent.keys.firstOrNull { key ->
                extractDepartmentName(key) == departmentName
            }
        )
        
        // Return first matching key's spent amount
        for (key in possibleKeys) {
            if (key != null && phaseSpent.containsKey(key)) {
                return phaseSpent[key] ?: 0.0
            }
        }
        
        return 0.0
    }
    
    /**
     * Extract department name from department key (removes phaseId prefix if present)
     */
    private fun extractDepartmentName(departmentKey: String): String {
        return if (departmentKey.contains("_")) {
            departmentKey.substringAfter("_")
        } else {
            departmentKey
        }
    }
    
    /**
     * Get total project spent (sum of all phase spent amounts)
     */
    fun getTotalProjectSpent(): Double {
        return _phaseBudgetMap.value.values.sumOf { it.spent }
    }
    
    /**
     * Get total project budget (sum of all phase budgets)
     */
    fun getTotalProjectBudget(): Double {
        return _phaseBudgetMap.value.values.sumOf { it.totalBudget }
    }
    
    /**
     * Calculate maintenance status based on project dates
     * Moved from UI to ViewModel for performance (runs on background thread)
     */
    private fun calculateMaintenanceStatus(project: Project?): String? {
        if (project == null) return null
        
        // If project is IN REVIEW, don't check maintenance dates
        if (project.status == "IN REVIEW") {
            return null
        }
        
        // Check if both handover and maintenance dates are set
        if (project.handoverDate.isNullOrBlank() || project.maintenanceDate.isNullOrBlank()) {
            return null // Missing dates, use project status
        }
        
        return try {
            val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            dateFormatter.isLenient = false
            val handoverDate = dateFormatter.parse(project.handoverDate)
            val maintenanceDate = dateFormatter.parse(project.maintenanceDate)
            
            if (handoverDate != null && maintenanceDate != null) {
                val today = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.time
                
                val handoverDateAtMidnight = java.util.Calendar.getInstance().apply {
                    time = handoverDate
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.time
                
                val maintenanceDateAtMidnight = java.util.Calendar.getInstance().apply {
                    time = maintenanceDate
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.time
                
                // Check if handover date has passed
                val isHandoverDatePassed = today.time >= handoverDateAtMidnight.time
                // Check if maintenance date has passed
                val isMaintenanceDatePassed = today.time > maintenanceDateAtMidnight.time
                
                // Calculate days since maintenance date passed
                val daysSinceMaintenance = if (isMaintenanceDatePassed) {
                    val diffInMillis = today.time - maintenanceDateAtMidnight.time
                    val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)
                    diffInDays
                } else {
                    0L
                }
                
                // Show MAINTENANCE only if handover date has passed AND maintenance date hasn't passed
                when {
                    isHandoverDatePassed && !isMaintenanceDatePassed -> "MAINTENANCE"
                    isMaintenanceDatePassed && daysSinceMaintenance >= 30 -> "ARCHIVED"
                    isMaintenanceDatePassed -> "COMPLETED"
                    else -> null // Use project status
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Calculate instant budget summary from preloaded project and fetched data
     * Moved from UI to ViewModel for performance (runs on background thread)
     */
    private fun calculateInstantBudgetSummary(
        project: Project?,
        budgetSummary: ProjectBudgetSummary
    ): ProjectBudgetSummary {
        val currentProject = project ?: _preloadedProject ?: return budgetSummary
        
        // Use preloaded project data immediately, merge with ViewModel data when available
        // Priority: ViewModel data (if loaded) > preloaded project data
        val totalBudget = if (budgetSummary.totalBudget > 0.0) {
            budgetSummary.totalBudget
        } else {
            currentProject.budget
        }
        
        val totalSpent = budgetSummary.totalSpent
        val totalRemaining = if (budgetSummary.totalRemaining != 0.0 && budgetSummary.totalSpent > 0.0) {
            budgetSummary.totalRemaining
        } else {
            totalBudget - totalSpent
        }
        
        return ProjectBudgetSummary(
            project = currentProject, // Always use current project for instant display
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            totalRemaining = totalRemaining,
            spentPercentage = if (totalBudget > 0) (totalSpent / totalBudget * 100) else 0.0,
            categoryBreakdown = budgetSummary.categoryBreakdown, // Updated when expenses load
            departmentBreakdown = budgetSummary.departmentBreakdown, // Updated when phases/expenses load
            recentExpenses = budgetSummary.recentExpenses, // Updated when expenses load
            pendingApprovalsCount = budgetSummary.pendingApprovalsCount, // Updated when expenses load
            approvedExpensesCount = budgetSummary.approvedExpensesCount // Updated when expenses load
        )
    }
} 
