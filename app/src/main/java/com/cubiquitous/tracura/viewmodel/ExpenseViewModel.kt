package com.cubiquitous.tracura.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.ExpenseStatus
import com.cubiquitous.tracura.model.PaymentMode
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.Department
import com.cubiquitous.tracura.repository.ExpenseRepository
import com.cubiquitous.tracura.repository.ProjectRepository
import com.cubiquitous.tracura.repository.NotificationRepository
import com.cubiquitous.tracura.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import com.cubiquitous.tracura.model.ExpenseFormData
import com.cubiquitous.tracura.model.StatusCounts
import com.cubiquitous.tracura.model.ExpenseSummary
import com.cubiquitous.tracura.service.NotificationService
import com.cubiquitous.tracura.service.BudgetValidationService
import com.cubiquitous.tracura.model.UserRole

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationService: NotificationService,
    private val budgetValidationService: BudgetValidationService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _userNameMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val userNameMap: StateFlow<Map<String, String>> = _userNameMap.asStateFlow()

    private val _businessHeadPhone = MutableStateFlow<String?>(null)
    val businessHeadPhone: StateFlow<String?> = _businessHeadPhone.asStateFlow()

    private val _expenseSummary = MutableStateFlow(ExpenseSummary())
    val expenseSummary: StateFlow<ExpenseSummary> = _expenseSummary.asStateFlow()

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()

    private val _phases = MutableStateFlow<List<com.cubiquitous.tracura.model.Phase>>(emptyList())
    val phases: StateFlow<List<com.cubiquitous.tracura.model.Phase>> = _phases.asStateFlow()
    
    // Store departments from subcollection for selected phase
    private val _departmentsFromSubcollection = MutableStateFlow<List<com.cubiquitous.tracura.model.Department>>(emptyList())
    val departmentsFromSubcollection: StateFlow<List<com.cubiquitous.tracura.model.Department>> = _departmentsFromSubcollection.asStateFlow()
    
    // Store ALL departments for the project (across all phases) - used for filtering phases by user assignment
    private val _allProjectDepartments = MutableStateFlow<List<com.cubiquitous.tracura.model.Department>>(emptyList())
    val allProjectDepartments: StateFlow<List<com.cubiquitous.tracura.model.Department>> = _allProjectDepartments.asStateFlow()
    
    // Store itemTypes as StateFlow for reactive updates
    private val _itemTypes = MutableStateFlow<List<String>>(emptyList())
    val itemTypes: StateFlow<List<String>> = _itemTypes.asStateFlow()

    private val _requestSubmissionSuccess = MutableStateFlow(false)
    val requestSubmissionSuccess: StateFlow<Boolean> = _requestSubmissionSuccess.asStateFlow()
    
    private val _phaseOverrideRequests = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val phaseOverrideRequests: StateFlow<List<Map<String, Any>>> = _phaseOverrideRequests.asStateFlow()

    // Additional properties for TrackSubmissionsScreen compatibility
    private val _statusCounts = MutableStateFlow(StatusCounts())
    val statusCounts: StateFlow<StatusCounts> = _statusCounts.asStateFlow()

    private val _filteredExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val filteredExpenses: StateFlow<List<Expense>> = _filteredExpenses.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<ExpenseStatus?>(null)
    val selectedStatusFilter: StateFlow<ExpenseStatus?> = _selectedStatusFilter.asStateFlow()

    // Form-related properties that UI screens expect
    // Initialize with current date as default
    private val _formData = MutableStateFlow(
        ExpenseFormData(
            date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
            amount = "0"
        )
    )
    val formData: StateFlow<ExpenseFormData> = _formData.asStateFlow()

    // Cache expense for editing to enable instant loading
    private val _expenseForEditing = MutableStateFlow<Expense?>(null)
    val expenseForEditing: StateFlow<Expense?> = _expenseForEditing.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Budget validation state
    private val _budgetValidationResult = MutableStateFlow<BudgetValidationService.BudgetValidationResult?>(null)
    val budgetValidationResult: StateFlow<BudgetValidationService.BudgetValidationResult?> = _budgetValidationResult.asStateFlow()

    private val _budgetWarning = MutableStateFlow<String?>(null)
    val budgetWarning: StateFlow<String?> = _budgetWarning.asStateFlow()

    // Field error tracking for auto-scroll
    private val _fieldError = MutableStateFlow<String?>(null)
    val fieldError: StateFlow<String?> = _fieldError.asStateFlow()

    // Get categories dynamically from selected project only
    val categories: List<String>
        get() {
            val projectCategories = _selectedProject.value?.categories ?: emptyList()
            Log.d("ExpenseViewModel", "🔍 Getting categories: $projectCategories from project: ${_selectedProject.value?.name}")
            return projectCategories
        }

    // Get departments dynamically from selected project and phase
    val departments: List<String>
        get() {
            // Use departments from subcollection if available
            val deptsFromSubcollection = _departmentsFromSubcollection.value.map { it.name }
            if (deptsFromSubcollection.isNotEmpty()) {
                Log.d("ExpenseViewModel", "🔍 Getting departments from subcollection: $deptsFromSubcollection")
                return deptsFromSubcollection
            }
            
            val formData = _formData.value
            val selectedPhaseId = formData.phaseId
            
            // If phase is selected, get departments from that phase (fallback)
            if (selectedPhaseId.isNotEmpty()) {
                val phase = _phases.value.find { it.id == selectedPhaseId }
                val depts = phase?.departments?.keys?.toList() ?: emptyList()
                Log.d("ExpenseViewModel", "🔍 Getting departments from phase ${phase?.phaseName}: $depts")
                return depts
            }
            
            // Otherwise, get from project (fallback for backward compatibility)
            val depts = _selectedProject.value?.departmentBudgets?.keys?.toList() ?: emptyList()
            Log.d("ExpenseViewModel", "🔍 Getting departments from project: ${_selectedProject.value?.name}: $depts")
            return depts
        }
    
    // Update itemTypes when department or departments change
    private fun updateItemTypes() {
        val formData = _formData.value
        val selectedDepartmentName = formData.department
        
        Log.d("ExpenseViewModel", "🔄 updateItemTypes called - department: '$selectedDepartmentName', total departments: ${_departmentsFromSubcollection.value.size}")
        
        if (selectedDepartmentName.isEmpty()) {
            _itemTypes.value = emptyList()
            Log.d("ExpenseViewModel", "  ⚠️ Department is empty, clearing itemTypes")
            return
        }
        
        // Try exact match first, then case-insensitive match with trimmed names
        val selectedDepartment = _departmentsFromSubcollection.value.find { dept ->
            dept.name == selectedDepartmentName || 
            dept.name.trim().equals(selectedDepartmentName.trim(), ignoreCase = true)
        }
        
        if (selectedDepartment == null) {
            Log.w("ExpenseViewModel", "  ⚠️ Department '$selectedDepartmentName' not found in ${_departmentsFromSubcollection.value.size} departments")
            Log.d("ExpenseViewModel", "  Available departments: ${_departmentsFromSubcollection.value.map { it.name }}")
            Log.d("ExpenseViewModel", "  Searching for: '${selectedDepartmentName.trim()}' (case-insensitive)")
            _itemTypes.value = emptyList()
            return
        }
        
        Log.d("ExpenseViewModel", "  ✅ Matched department: '${selectedDepartment.name}' (searched for: '$selectedDepartmentName')")
        
        Log.d("ExpenseViewModel", "  ✅ Found department: ${selectedDepartment.name}, lineItems count: ${selectedDepartment.lineItems.size}")
        
        // Log all lineItems details
        selectedDepartment.lineItems.forEachIndexed { index, lineItem ->
            Log.d("ExpenseViewModel", "    LineItem[$index]: itemType='${lineItem.itemType}', item='${lineItem.item}', spec='${lineItem.spec}'")
        }
        
        val itemTypesList = selectedDepartment.lineItems
            .mapNotNull { it.itemType }
            .distinct()
            .filter { it.isNotEmpty() }
            .sorted()

        val finalItemTypes = when (selectedDepartment.contractorModeEnumValue) {
            com.cubiquitous.tracura.model.ContractorMode.LABOUR_ONLY -> listOf("Contractor Amount") + itemTypesList
                                     com.cubiquitous.tracura.model.ContractorMode.SELF_EXECUTION -> itemTypesList
                                     com.cubiquitous.tracura.model.ContractorMode.TURNKEY_AMOUNT_ONLY -> emptyList()
                                 }

        _itemTypes.value = finalItemTypes
        Log.d(
            "ExpenseViewModel",
            "  ✅ Updated itemTypes: $finalItemTypes (from ${selectedDepartment.lineItems.size} lineItems, mode=${selectedDepartment.contractorMode})"
        )
    }
    
    // Public method to update itemTypes - can be called from UI when departments finish loading
    fun updateItemTypesForSelectedDepartment() {
        updateItemTypes()
    }
    
    // Get specs (grades) from selected department's lineItems based on itemType
    fun getSpecsForItemType(itemType: String): List<String> {
        val formData = _formData.value
        val selectedDepartmentName = formData.department
        
        if (selectedDepartmentName.isEmpty() || itemType.isEmpty()) return emptyList()
        
        val selectedDepartment = _departmentsFromSubcollection.value.find { it.name == selectedDepartmentName }
        val specs = selectedDepartment?.lineItems
            ?.filter { it.itemType == itemType }
            ?.mapNotNull { it.spec }
            ?.distinct() ?: emptyList()
        Log.d("ExpenseViewModel", "🔍 Getting specs for itemType $itemType in department $selectedDepartmentName: $specs")
        return specs
    }
    
    // Get items from selected department's lineItems based on itemType
    fun getItemsForItemType(itemType: String): List<String> {
        val formData = _formData.value
        val selectedDepartmentName = formData.department
        
        if (selectedDepartmentName.isEmpty() || itemType.isEmpty()) return emptyList()
        
        val selectedDepartment = _departmentsFromSubcollection.value.find { it.name == selectedDepartmentName }
        val items = selectedDepartment?.lineItems
            ?.filter { it.itemType == itemType }
            ?.mapNotNull { it.item }
            ?.distinct() ?: emptyList()
        Log.d("ExpenseViewModel", "🔍 Getting items for itemType $itemType in department $selectedDepartmentName: $items")
        return items
    }
    
    // Get UoM from selected department's lineItems (or default)
    fun getUomForItem(itemType: String, item: String): String {
        val formData = _formData.value
        val selectedDepartmentName = formData.department
        
        if (selectedDepartmentName.isEmpty() || itemType.isEmpty() || item.isEmpty()) return ""
        
        val selectedDepartment = _departmentsFromSubcollection.value.find { it.name == selectedDepartmentName }
        // Try to find UoM from lineItems
        val lineItem = selectedDepartment?.lineItems?.firstOrNull { 
            it.itemType == itemType && it.item == item 
        }
        
        // Return UOM from lineItem if found, otherwise use default based on itemType
        return lineItem?.uom?.takeIf { it.isNotEmpty() } ?: when (itemType) {
            "Raw material" -> "ton"
            "Labour" -> "day"
            "Machines & eq" -> "day"
            else -> ""
        }
    }
    
    // Get remainingQuantity for a specific line item in the selected department
    fun getRemainingQuantityForLineItem(itemType: String, item: String, spec: String): Double? {
        val formData = _formData.value
        val selectedDepartmentName = formData.department
        
        if (selectedDepartmentName.isEmpty() || itemType.isEmpty()) return null
        
        val selectedDepartment = _departmentsFromSubcollection.value.find { it.name == selectedDepartmentName }
            ?: return null
        
        // Find matching line item by itemType, item, and spec
        val lineItem = selectedDepartment.lineItems.firstOrNull {
            it.itemType == itemType &&
            (item.isEmpty() || it.item == item) &&
            (spec.isEmpty() || it.spec == spec)
        }
        
        return lineItem?.remainingQuantity
    }

    val paymentModes = listOf(
        "cash" to "By cash",
        "upi" to "By UPI", 
        "check" to "By cheque",
        "card" to "By Card"
    )

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
                    Log.e("ExpenseViewModel", "Error resolving name for $phone", e)
                }
            }
            if (newNames.isNotEmpty()) {
                _userNameMap.update { current -> current + newNames }
            }
        }
    }

    fun loadUserExpenses(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("ExpenseViewModel", "🔄 Loading expenses for user: $userId")
                
                // Load projects first  
                try {
                    val projects = projectRepository.getAllProjects()
                    _projects.value = projects
                } catch (exception: Exception) {
                    Log.e("ExpenseViewModel", "❌ Error loading projects: ${exception.message}")
                }
                
                // Load user expenses
                expenseRepository.getUserExpenses(userId)
                    .onEach { expenseList ->
                        _expenses.value = expenseList
                        calculateUserSummary(expenseList)
                        resolveUserNames(expenseList)
                        Log.d("ExpenseViewModel", "✅ Loaded ${expenseList.size} expenses for user")
                    }
                    .catch { exception ->
                        _error.value = "Failed to load expenses: ${exception.message}"
                        Log.e("ExpenseViewModel", "❌ Error loading user expenses: ${exception.message}")
                    }
                    .collect()
                
            } catch (e: Exception) {
                _error.value = "Failed to load user data: ${e.message}"
                Log.e("ExpenseViewModel", "❌ Error in loadUserExpenses: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadBusinessHeadPhone(userPhone: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ExpenseViewModel", "🔍 Loading business head phone for user: $userPhone")
                val bhPhone = authRepository.getBusinessHeadPhone(userPhone)
                _businessHeadPhone.value = bhPhone
                android.util.Log.d("ExpenseViewModel", "✅ Business head phone loaded: $bhPhone")
            } catch (e: Exception) {
                android.util.Log.e("ExpenseViewModel", "❌ Error loading business head phone", e)
            }
        }
    }

    private suspend fun getScopedPhasesForProject(
        projectId: String,
        roleHint: UserRole? = null,
    ): List<com.cubiquitous.tracura.model.Phase> {
        val allPhases = projectRepository.getPhasesForProject(projectId)
        val role = roleHint ?: authRepository.getCurrentUserFromFirebase()?.role
        if (role != UserRole.APPROVER && role != UserRole.USER) {
            return allPhases
        }

        val customerId = projectRepository.findCustomerIdForProject(projectId)
        val allowedPhaseIds = projectRepository.getAllowedPhaseIdsForCurrentUserProject(projectId, customerId)
        return allPhases.filter { it.id in allowedPhaseIds }
    }

    fun setSelectedProject(project: Project) {
        _selectedProject.value = project
        Log.d("ExpenseViewModel", "✅ Set selected project: ${project.name} with departments: ${project.departmentBudgets?.keys ?: emptySet()}")
        
        // Load phases for this project
        viewModelScope.launch {
            try {
                val projectId = project.id ?: return@launch
                val phases = getScopedPhasesForProject(projectId)
                _phases.value = phases
                Log.d("ExpenseViewModel", "✅ Loaded ${phases.size} phases for project: ${project.name}")
                
                // Load ALL departments for the project (for phase filtering)
                loadAllProjectDepartments(projectId)
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Error loading phases: ${e.message}")
                _phases.value = emptyList()
            }
        }
    }
    
    // Public method to load all departments for a project - can be called from UI
    fun loadAllProjectDepartments(projectId: String) {
        viewModelScope.launch {
            try {
                projectRepository.getAllDepartmentsForProject(projectId)
                    .collect { departments ->
                        _allProjectDepartments.value = departments
                        Log.d("ExpenseViewModel", "✅ Loaded ${departments.size} total departments for project: $projectId")
                    }
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Error loading all project departments: ${e.message}")
            }
        }
    }
    
    // Load departments from subcollection for selected phase
    fun loadDepartmentsForPhase(projectId: String, phaseId: String) {
        viewModelScope.launch {
            try {
                Log.d("ExpenseViewModel", "🔄 Loading departments for phase: $phaseId in project: $projectId")
                val customerId = projectRepository.findCustomerIdForProject(projectId)
                if (customerId != null && phaseId.isNotEmpty()) {
                    val departments = projectRepository.getDepartmentsForPhase(customerId, projectId, phaseId)
                    _departmentsFromSubcollection.value = departments
                    Log.d("ExpenseViewModel123", "✅ Loaded ${departments.size} departments from subcollection for phase: $phaseId")
                    
                    // Log department details for debugging
                    departments.forEach { dept ->
                        val itemTypesInDept = dept.lineItems.mapNotNull { it.itemType }.distinct().filter { it.isNotEmpty() }
                        Log.d("ExpenseViewModel", "  📋 Department: '${dept.name}', LineItems: ${dept.lineItems.size}, ItemTypes: $itemTypesInDept")
                        dept.lineItems.forEachIndexed { index, item ->
                            Log.d("ExpenseViewModel", "    [$index] itemType='${item.itemType}', item='${item.item}', spec='${item.spec}'")
                        }
                    }
                    
                    // Update itemTypes after loading departments (only if a department is already selected)
                    val currentDepartment = _formData.value.department
                    if (currentDepartment.isNotEmpty()) {
                        Log.d("ExpenseViewModel", "  🔄 Department '$currentDepartment' is already selected, updating itemTypes...")
                        updateItemTypes()
                    } else {
                        Log.d("ExpenseViewModel", "  ℹ️ No department selected yet, itemTypes will update when department is selected")
                    }
                } else {
                    Log.w("ExpenseViewModel", "  ⚠️ Cannot load departments: customerId=$customerId, phaseId=$phaseId")
                    _departmentsFromSubcollection.value = emptyList()
                    _itemTypes.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Error loading departments from subcollection: ${e.message}", e)
                _departmentsFromSubcollection.value = emptyList()
                _itemTypes.value = emptyList()
            }
        }
    }

    /**
     * Fast load with preloaded data - uses direct fetch and only fetches missing data
     * This method is optimized for instant display when data is already available
     */
    fun loadProjectExpensesFast(
        projectId: String,
        project: Project,
        preloadedPhases: List<com.cubiquitous.tracura.model.Phase>? = null,
        preloadedExpenses: List<Expense>? = null,
        userRole: UserRole? = null
    ) {
        viewModelScope.launch {
            _error.value = null
            
            try {
                // Set project immediately (no fetch needed)
                _selectedProject.value = project
                
                // Determine expenses flow based on role
                val expensesFlow = if (userRole == UserRole.APPROVER) {
                    val userPhone = projectRepository.getCurrentUserIdentifier()?.replace("+91", "")?.trim()
                    val assignedDepts = if (userPhone != null) {
                         project.departmentApproverAssignments
                            .filter { (_, approverPhone) -> approverPhone.replace("+91", "").trim() == userPhone }
                            .keys.toList()
                    } else emptyList()
                    
                    if (assignedDepts.isNotEmpty()) {
                        Log.d("ExpenseViewModel", "👨‍💼 Approver assigned to departments: $assignedDepts")
                        expenseRepository.getExpensesForApprover(projectId, assignedDepts)
                    } else {
                        Log.d("ExpenseViewModel", "👤 No departments assigned for approver, fetching own expenses")
                        expenseRepository.getProjectExpenses(projectId)
                    }
                } else {
                    expenseRepository.getProjectExpenses(projectId)
                }
                
                // Use preloaded phases if available, otherwise fetch quickly
                if (preloadedPhases != null && preloadedPhases.isNotEmpty()) {
                    _phases.value = preloadedPhases
                    Log.d("ExpenseViewModel", "✅ Using ${preloadedPhases.size} preloaded phases")
                } else {
                    // Fast fetch phases only if not preloaded
                    viewModelScope.launch {
                        try {
                            val phases = getScopedPhasesForProject(projectId, userRole)
                            _phases.value = phases
                            Log.d("ExpenseViewModel", "✅ Fast fetched ${phases.size} phases")
                        } catch (e: Exception) {
                            Log.e("ExpenseViewModel", "❌ Error loading phases: ${e.message}")
                        }
                    }
                }
                
                // Use preloaded expenses if available for instant display
                if (preloadedExpenses != null && preloadedExpenses.isNotEmpty()) {
                    _expenses.value = preloadedExpenses
                    _filteredExpenses.value = preloadedExpenses
                    calculateProjectSummary(preloadedExpenses)
                    resolveUserNames(preloadedExpenses)
                    Log.d("ExpenseViewModel", "✅ Using ${preloadedExpenses.size} preloaded expenses - instant display")
                } else {
                    // Fast direct fetch for immediate display (no loading state)
                    viewModelScope.launch {
                        try {
                            // Use direct fetch - get first value from Flow for speed
                            val directExpenses = expensesFlow.first()
                            
                            _expenses.value = directExpenses
                            _filteredExpenses.value = directExpenses
                            calculateProjectSummary(directExpenses)
                            resolveUserNames(directExpenses)
                            Log.d("ExpenseViewModel", "✅ Fast fetched ${directExpenses.size} expenses")
                        } catch (e: Exception) {
                            Log.e("ExpenseViewModel", "❌ Error fast fetching expenses: ${e.message}")
                        }
                    }
                }
                
                // Set up real-time listener in background (non-blocking)
                viewModelScope.launch {
                    try {
                        expensesFlow
                            .onEach { expenseList ->
                                // Only update if data changed (avoid unnecessary UI updates)
                                if (expenseList != _expenses.value) {
                                    _expenses.value = expenseList
                                    _filteredExpenses.value = expenseList
                                    calculateProjectSummary(expenseList)
                                    resolveUserNames(expenseList)
                                    Log.d("ExpenseViewModel", "✅ Real-time update: ${expenseList.size} expenses")
                                }
                            }
                            .catch { exception ->
                                Log.e("ExpenseViewModel", "❌ Error in real-time listener: ${exception.message}")
                            }
                            .collect()
                    } catch (e: Exception) {
                        Log.e("ExpenseViewModel", "❌ Error setting up real-time listener: ${e.message}")
                    }
                }
                
            } catch (e: Exception) {
                _error.value = "Failed to load: ${e.message}"
                Log.e("ExpenseViewModel", "❌ Error in loadProjectExpensesFast: ${e.message}")
            }
        }
    }

    fun loadProjectExpenses(projectId: String, userRole: UserRole? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Load project details
                val project = projectRepository.getProjectById(projectId)
                _selectedProject.value = project
                
                // Determine expenses flow based on role
                val expensesFlow = if (userRole == UserRole.APPROVER && project != null) {
                    val userPhone = projectRepository.getCurrentUserIdentifier()?.replace("+91", "")?.trim()
                    val assignedDepts = if (userPhone != null) {
                         project.departmentApproverAssignments
                            .filter { (_, approverPhone) -> approverPhone.replace("+91", "").trim() == userPhone }
                            .keys.toList()
                    } else emptyList()
                    
                    if (assignedDepts.isNotEmpty()) {
                        Log.d("ExpenseViewModel", "👨‍💼 Approver assigned to departments: $assignedDepts")
                        expenseRepository.getExpensesForApprover(projectId, assignedDepts)
                    } else {
                        Log.d("ExpenseViewModel", "👤 No departments assigned for approver, fetching own expenses")
                        expenseRepository.getProjectExpenses(projectId)
                    }
                } else {
                    expenseRepository.getProjectExpenses(projectId)
                }
                
                // Load initial expenses first for immediate display
                val initialExpenses = expensesFlow.first()
                _expenses.value = initialExpenses
                _filteredExpenses.value = initialExpenses
                calculateProjectSummary(initialExpenses)
                resolveUserNames(initialExpenses)
                _isLoading.value = false
                Log.d("ExpenseViewModel", "✅ Loaded ${initialExpenses.size} initial expenses for project")
                
                // Set up real-time listener for automatic updates in a separate coroutine
                viewModelScope.launch {
                expensesFlow
                    .onEach { expenseList ->
                        _expenses.value = expenseList
                        _filteredExpenses.value = expenseList
                        calculateProjectSummary(expenseList)
                        resolveUserNames(expenseList)
                            Log.d("ExpenseViewModel", "✅ Real-time update: ${expenseList.size} expenses for project")
                    }
                    .catch { exception ->
                            Log.e("ExpenseViewModel", "❌ Error in real-time listener: ${exception.message}")
                    }
                        .collect() // Keep collecting for real-time updates
                }
                
            } catch (e: Exception) {
                _error.value = "Failed to load project: ${e.message}"
                Log.e("ExpenseViewModel", "❌ Error in loadProjectExpenses: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    fun updateByCredit(value: Boolean) {
        val currentData = _formData.value
        _formData.value = currentData.copy(byCredit = value)
    }
    
    fun updateFormField(field: String, value: String) {
        val currentData = _formData.value
        _formData.value = when (field) {
            "date" -> currentData.copy(date = value)
            "amount" -> currentData.copy(amount = value)
            "phaseId" -> {
                // When phase changes, clear department selection and update phase name
                val phase = _phases.value.find { it.id == value }
                // Load departments from subcollection for this phase
                _selectedProject.value?.id?.let { projectId ->
                    if (value.isNotEmpty()) {
                        loadDepartmentsForPhase(projectId, value)
                    } else {
                        // Clear itemTypes when phase is cleared
                        _itemTypes.value = emptyList()
                    }
                }
                val newData = currentData.copy(
                    phaseId = value,
                    phaseName = phase?.phaseName ?: "",
                    department = "", // Clear department when phase changes
                    itemType = "", // Clear itemType when phase changes
                    item = "", // Clear item when phase changes
                    spec = "" // Clear spec when phase changes
                )
                // Clear itemTypes when phase changes (before departments are loaded)
                if (value.isEmpty()) {
                    _itemTypes.value = emptyList()
                }
                newData
            }
            "phaseName" -> currentData.copy(phaseName = value)
            "department" -> {
                // When department changes, clear itemType, related fields, and vendor
                val selectedDepartment = _departmentsFromSubcollection.value.find { dept ->
                    dept.name == value || dept.name.trim().equals(value.trim(), ignoreCase = true)
                }
                 val isTurnkey = selectedDepartment?.contractorModeEnumValue == com.cubiquitous.tracura.model.ContractorMode.TURNKEY_AMOUNT_ONLY

                val newData = currentData.copy(
                    department = value,
                    itemType = "",
                    item = "",
                    spec = "",
                    brand = "",
                    vendor = "", // Clear vendor when department changes
                    quantity = if (isTurnkey) "" else currentData.quantity,
                    unitPrice = if (isTurnkey) "" else currentData.unitPrice,
                    lineAmount = if (isTurnkey) "" else currentData.lineAmount,
                    amount = if (isTurnkey) "" else currentData.amount,
                    uom = if (isTurnkey) "" else currentData.uom,
                    thickness = if (isTurnkey) "" else currentData.thickness,
                )
                // Update itemTypes after department changes
                updateItemTypes()
                newData
            }
            "category" -> currentData.copy(category = value) // ExpenseFormData uses category (String), will convert to categories when creating Expense
            "description" -> currentData.copy(description = value)
            "modeOfPayment" -> currentData.copy(modeOfPayment = value) // ExpenseFormData uses modeOfPayment as String, will convert to enum when creating Expense
            "attachmentUri" -> currentData.copy(attachmentUri = value)
            "paymentProofUri" -> currentData.copy(paymentProofUri = value)
            "vendor" -> currentData.copy(vendor = value)
            "byCredit" -> currentData.copy(byCredit = value.toBooleanStrictOrNull() ?: false)
            "itemType" -> {
                // When itemType changes, clear item, spec, brand, and thickness (especially for Labour)
                // Also handle UoM: set to first Labour option if changing to Labour, or "ton" if changing from Labour
                val previousItemType = currentData.itemType

                if (value == "Contractor Amount") {
                    currentData.copy(
                        itemType = value,
                        item = "",
                        spec = "",
                        brand = "",
                        thickness = "",
                        quantity = "",
                        unitPrice = "",
                        lineAmount = "",
                        amount = "",
                        uom = "",
                    )
                } else {
                val newUom = when {
                    value == "Labour" -> {
                        // Set to first Labour UoM option if empty
                        if (currentData.uom.isEmpty()) "nos" else {
                            // Keep current UoM if it's a valid Labour option, otherwise set to first
                            val labourUomOptions = listOf("nos", "sqft", "sft", "cft", "rmt", "kg", "ton", "ltr")
                            if (labourUomOptions.contains(currentData.uom.lowercase())) currentData.uom else "nos"
                        }
                    }
                    previousItemType == "Labour" -> {
                        // When changing from Labour, set to "ton" (default for non-Labour)
                        "ton"
                    }
                    else -> currentData.uom // Keep current UoM for other changes
                }
                currentData.copy(
                    itemType = value,
                    item = "",
                    spec = "",
                    brand = if (value == "Labour") "" else currentData.brand,
                    thickness = if (value == "Labour") "" else currentData.thickness,
                    uom = newUom
                )
                }
            }
            "item" -> {
                // When item changes, update UoM and clear spec
                val uom = if (value.isNotEmpty() && currentData.itemType.isNotEmpty()) {
                    getUomForItem(currentData.itemType, value)
                } else {
                    currentData.uom
                }
                currentData.copy(
                    item = value,
                    uom = uom,
                    spec = ""
                )
            }
            "brand" -> currentData.copy(brand = value)
            "spec" -> currentData.copy(spec = value)
            "thickness" -> currentData.copy(thickness = value)
            "quantity" -> {
                // Calculate lineAmount when quantity changes
                val qty = value.toDoubleOrNull() ?: 0.0
                val unitPrice = currentData.unitPrice.toDoubleOrNull() ?: 0.0
                val lineAmount = (qty * unitPrice).toString()
                currentData.copy(
                    quantity = value,
                    lineAmount = lineAmount,
                    amount = lineAmount // Also update amount field
                )
            }
            "uom" -> currentData.copy(uom = value)
            "unitPrice" -> {
                // Calculate lineAmount when unitPrice changes
                val qty = currentData.quantity.toDoubleOrNull() ?: 0.0
                val price = value.toDoubleOrNull() ?: 0.0
                val lineAmount = (qty * price).toString()
                currentData.copy(
                    unitPrice = value,
                    lineAmount = lineAmount,
                    amount = lineAmount // Also update amount field
                )
            }
            "lineAmount" -> currentData.copy(lineAmount = value)
            "sgst" -> currentData.copy(sgst = value)
            "cgst" -> currentData.copy(cgst = value)
            else -> currentData
        }
        
        // Clear budget validation when form changes
        _budgetValidationResult.value = null
        _budgetWarning.value = null
    }

    fun submitExpense(
        projectId: String,
        userId: String,
        userName: String,
        onSuccess: () -> Unit,
        budgetExceeded: Boolean = false,
        expenseId: String? = null
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _error.value = null
            _fieldError.value = null // Clear previous field error to ensure fresh validation
            _budgetWarning.value = null
            
            try {
                val  formData =_formData.value
                
                // Date validation removed - date is always set to current date by default
                
                // Amount validation: either amount field OR (quantity × unitPrice) must be provided
                val hasAmount = formData.amount.isNotEmpty() && formData.amount.toDoubleOrNull() != null
                val hasQuantityAndPrice = formData.quantity.isNotEmpty() && formData.unitPrice.isNotEmpty() &&
                    formData.quantity.toDoubleOrNull() != null && formData.unitPrice.toDoubleOrNull() != null
                
                if (!hasAmount && !hasQuantityAndPrice) {
                    _error.value = "Please enter a valid amount or provide quantity and unit price"
                    _fieldError.value = "amount"
                    _isSubmitting.value = false
                    return@launch
                }
                
                // Validate phase selection
                if (formData.phaseId.isEmpty()) {
                    _error.value = "Please select a phase"
                    _fieldError.value = "phase"
                    _isSubmitting.value = false
                    return@launch
                }

                // Validate that selected phase is active (enabled)
                val selectedPhase = _phases.value.find { it.id == formData.phaseId }
                if (selectedPhase == null) {
                    _error.value = "Selected phase not found"
                    _isSubmitting.value = false
                    return@launch
                }
                
                // Get project to check status
                val project = _selectedProject.value
                if (project != null) {
                    val projectStatus = project.status.uppercase()
                    // For user flow: disable IN REVIEW and DECLINED projects (approvers can still access them via different flows)
                    val isProjectDisabled = projectStatus in listOf("INACTIVE", "COMPLETED", "HANDOVER", "LOCKED", "IN REVIEW", "REVIEW REJECTED", "DECLINED")
                    if (isProjectDisabled) {
                        val errorMessage = when (projectStatus) {
                            "LOCKED" -> "Cannot add expense: Project is locked until planned start date."
                            "IN REVIEW" -> "Cannot add expense: Project is under review. Please wait for approval."
                            "REVIEW REJECTED", "DECLINED" -> "Cannot add expense: Project has been declined. Expenses cannot be added to declined projects."
                            else -> "Cannot add expense: Project is disabled."
                        }
                        _error.value = errorMessage
                        _isSubmitting.value = false
                        return@launch
                    }
                }
                
                // Check if phase is not enabled
                if (!selectedPhase.isEnabledValue) {
                    _error.value = "Cannot add expense: Selected phase is disabled. Please enable the phase first."
                    _isSubmitting.value = false
                    return@launch
                }
                
                // Check phase dates
                val phaseDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val today = Calendar.getInstance()
                today.set(Calendar.HOUR_OF_DAY, 0)
                today.set(Calendar.MINUTE, 0)
                today.set(Calendar.SECOND, 0)
                today.set(Calendar.MILLISECOND, 0)
                
                // Check if end date has passed
                if (!selectedPhase.endDate.isNullOrEmpty()) {
                    try {
                        val endDate = phaseDateFormat.parse(selectedPhase.endDate)
                        endDate?.let {
                            val cal = Calendar.getInstance()
                            cal.time = it
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            if (cal.before(today)) {
                                _error.value = "Cannot add expense: Phase end date has passed."
                                _isSubmitting.value = false
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        // Invalid date format, skip date check
                    }
                }
                
                // Check if start date is in the future
                if (!selectedPhase.startDate.isNullOrEmpty()) {
                    try {
                        val startDate = phaseDateFormat.parse(selectedPhase.startDate)
                        startDate?.let {
                            val cal = Calendar.getInstance()
                            cal.time = it
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            if (cal.after(today)) {
                                _error.value = "Cannot add expense: Phase start date is in the future."
                                _isSubmitting.value = false
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        // Invalid date format, skip date check
                    }
                }
                
                if (formData.department.isEmpty()) {
                    _error.value = "Please select a department"
                    _fieldError.value = "department"
                    _isSubmitting.value = false
                    return@launch
                }

                val selectedDeptForMode = _departmentsFromSubcollection.value.find {
                    it.name.equals(formData.department, ignoreCase = true)
                }
                val mode = selectedDeptForMode?.contractorModeEnumValue
                 val isTurnkeyAmountOnly = mode == com.cubiquitous.tracura.model.ContractorMode.TURNKEY_AMOUNT_ONLY

                 // Turnkey: amount-only entry (no description, receipt, GST, vendor, etc.)
                 if (!isTurnkeyAmountOnly) {
                    // Validate vendor selection (compulsory)
                    if (formData.vendor.isEmpty()) {
                        _error.value = "Please select a vendor"
                        _fieldError.value = "vendor"
                        _isSubmitting.value = false
                        return@launch
                    }

                    // Category is optional if itemType is provided (material-based expense)
                    if (formData.category.isEmpty() && formData.itemType.isEmpty()) {
                        if (categories.isEmpty()) {
                            _error.value = "No categories configured for this project. Please contact the project manager to add categories."
                        } else {
                            _error.value = "Please select a category or sub-category"
                        }
                        _fieldError.value = "category"
                        _isSubmitting.value = false
                        return@launch
                    }

                    // Validate description field
                    if (formData.description.isEmpty() || formData.description.isBlank()) {
                        _error.value = "Please enter a description"
                        _fieldError.value = "description"
                        _isSubmitting.value = false
                        return@launch
                    }

                    // Validate SGST (compulsory)
                    if (formData.sgst.isBlank()) {
                        _error.value = "Please enter SGST"
                        _fieldError.value = "sgst"
                        _isSubmitting.value = false
                        return@launch
                    }

                    // Validate CGST (compulsory)
                    if (formData.cgst.isBlank()) {
                        _error.value = "Please enter CGST"
                        _fieldError.value = "cgst"
                        _isSubmitting.value = false
                        return@launch
                    }

                    // Validate attachment field
                    if (formData.attachmentUri.isEmpty()) {
                        _error.value = "Please add an attachment"
                        _fieldError.value = "attachment"
                        _isSubmitting.value = false
                        return@launch
                    }

                    // Validate payment proof for UPI and Cheque
                    val requiresPaymentProof = formData.modeOfPayment.lowercase() in listOf("upi", "check")
                    if (requiresPaymentProof && formData.paymentProofUri.isEmpty()) {
                        _error.value = "Please upload payment proof (screenshot/image) for ${if (formData.modeOfPayment.lowercase() == "upi") "UPI" else "Cheque"} payment"
                        _fieldError.value = "paymentProof"
                        _isSubmitting.value = false
                        return@launch
                    }
                }
                
                // Parse date
                val parsedDate = try {
                    phaseDateFormat.parse(formData.date)
                } catch (e: Exception) {
                    _error.value = "Invalid date format. Please use DD/MM/YYYY"
                    _isSubmitting.value = false
                    return@launch
                }
                
                // Calculate line amount (quantity × unitPrice) or use amount field
                // Per guide: Use lineAmount if quantity and unitPrice are provided, otherwise use amount
                val lineAmount = if (formData.quantity.isNotEmpty() && formData.unitPrice.isNotEmpty()) {
                    try {
                        val qty = formData.quantity.toDouble()
                        val price = formData.unitPrice.toDouble()
                        qty * price
                    } catch (e: NumberFormatException) {
                        // Fallback to amount field if calculation fails
                        try {
                            formData.amount.toDouble()
                        } catch (e2: NumberFormatException) {
                            _error.value = "Please enter a valid amount"
                            _fieldError.value = "amount"
                            _isSubmitting.value = false
                            return@launch
                        }
                    }
                } else {
                    // Use amount field
                    try {
                        formData.amount.toDouble()
                    } catch (e: NumberFormatException) {
                        _error.value = "Please enter a valid amount"
                        _fieldError.value = "amount"
                        _isSubmitting.value = false
                        return@launch
                    }
                }
                
                val amount = lineAmount
                
                // Format department key for budget validation (use display name for matching)
                // Budget validation expects the actual department name, not the formatted key
                val departmentNameForValidation = com.cubiquitous.tracura.utils.FormatUtils.getDepartmentDisplayName(formData.department)
                
                // Budget validation - now includes phaseId to check phase-specific budget
                Log.d("ExpenseViewModel", "🔍 Validating budget for department: $departmentNameForValidation, phase: ${formData.phaseId}, amount: $amount")
                val budgetValidation = budgetValidationService.validateExpenseAgainstBudget(
                    projectId = projectId,
                    department = departmentNameForValidation,
                    newExpenseAmount = amount,
                    phaseId = formData.phaseId
                )
                
                _budgetValidationResult.value = budgetValidation
                
                // 150% Hard Limit Check - ALWAYS block if exceeded, even if budgetExceeded flag is true
                if (budgetValidation.exceedsHardLimit) {
                    _budgetWarning.value = budgetValidation.warningMessage
                    _error.value = budgetValidation.warningMessage
                    _isSubmitting.value = false
                    Log.w("ExpenseViewModel", "❌ 150% Budget Hard Limit Exceeded: ${budgetValidation.warningMessage}")
                    return@launch
                }
                
                // Check if budget is exceeded (standard 100% check)
                val isBudgetExceeded = budgetExceeded || !budgetValidation.isValid
                
                if (!budgetValidation.isValid && !budgetExceeded) {
                    _budgetWarning.value = budgetValidation.warningMessage
                    _error.value = budgetValidation.warningMessage
                    _isSubmitting.value = false
                    Log.w("ExpenseViewModel", "⚠️ Budget validation failed (100%): ${budgetValidation.warningMessage}")
                    return@launch
                }

                // Upload attachment to Firebase Storage if it's a local URI
                var finalAttachmentUrl = formData.attachmentUri
                var finalAttachmentFileName = formData.attachmentUri.substringAfterLast("/").substringBefore("?")
                
                // Check if attachment is a local URI (content:// or file://)
                val isLocalUri = formData.attachmentUri.startsWith("content://") || formData.attachmentUri.startsWith("file://")
                
                if (isLocalUri) {
                    Log.d("ExpenseViewModel", "📤 Uploading attachment to Firebase Storage...")
                    try {
                        // Extract file name from URI or use default
                        val fileName = formData.attachmentUri.substringAfterLast("/").substringBefore("?")
                            .takeIf { it.isNotEmpty() && it.contains(".") } 
                            ?: "attachment_${System.currentTimeMillis()}.pdf"
                        
                        // Upload to Firebase Storage
                        val uploadResult = expenseRepository.uploadAttachmentToStorage(
                            attachmentUri = formData.attachmentUri,
                            fileName = fileName,
                            projectId = projectId,
                            userId = userId
                        )
                        
                        if (uploadResult.isSuccess) {
                            val (downloadUrl, sanitizedFileName) = uploadResult.getOrNull()!!
                            finalAttachmentUrl = downloadUrl
                            finalAttachmentFileName = sanitizedFileName
                        } else {
                            val error = uploadResult.exceptionOrNull()?.message ?: "Unknown error"
                            _error.value = "Failed to upload attachment: $error"
                            _isSubmitting.value = false
                            return@launch
                        }
                    } catch (e: Exception) {
                        _error.value = "Failed to upload attachment: ${e.message}"
                        _isSubmitting.value = false
                        return@launch
                    }
                } else {
                }
                
                // Upload payment proof to Firebase Storage if required and it's a local URI
                val requiresPaymentProof = formData.modeOfPayment.lowercase() in listOf("upi", "check")
                var finalPaymentProofUrl = formData.paymentProofUri
                var finalPaymentProofFileName = ""
                if (requiresPaymentProof && formData.paymentProofUri.isNotEmpty()) {
                    val isPaymentProofLocalUri = formData.paymentProofUri.startsWith("content://") || formData.paymentProofUri.startsWith("file://")
                    
                    if (isPaymentProofLocalUri) {
                        Log.d("ExpenseViewModel", "📤 Uploading payment proof to Firebase Storage...")
                        try {
                            // Extract file name from URI or use default
                            val fileName = formData.paymentProofUri.substringAfterLast("/").substringBefore("?")
                                .takeIf { it.isNotEmpty() && it.contains(".") } 
                                ?: "payment_proof_${System.currentTimeMillis()}.jpg"
                            
                            // Upload to Firebase Storage
                            val uploadResult = expenseRepository.uploadAttachmentToStorage(
                                attachmentUri = formData.paymentProofUri,
                                fileName = fileName,
                                projectId = projectId,
                                userId = userId
                            )
                            
                            if (uploadResult.isSuccess) {
                                val (downloadUrl, sanitizedFileName) = uploadResult.getOrNull()!!
                                finalPaymentProofUrl = downloadUrl
                                finalPaymentProofFileName = sanitizedFileName
                            } else {
                                val error = uploadResult.exceptionOrNull()?.message ?: "Unknown error"
                                _error.value = "Failed to upload payment proof: $error"
                                _isSubmitting.value = false
                                return@launch
                            }
                        } catch (e: Exception) {
                            _error.value = "Failed to upload payment proof: ${e.message}"
                            _isSubmitting.value = false
                            return@launch
                        }
                    } else {
                        finalPaymentProofFileName = formData.paymentProofUri.substringAfterLast("/").substringBefore("?")
                    }
                }
                
                // Format date as String (dd/MM/yyyy)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                val dateString = dateFormat.format(parsedDate!!)
                
                // Format department key with phaseId prefix (per guide: "{phaseId}_{departmentName}")
                val departmentKey = com.cubiquitous.tracura.utils.FormatUtils.departmentKey(
                    phaseId = formData.phaseId,
                    departmentName = formData.department
                )
                
                // Get phase name for expense
                val phaseName = selectedPhase.phaseName.ifEmpty { 
                    if (selectedPhase.phaseNumber > 0) "Phase ${selectedPhase.phaseNumber}" else ""
                }
                
                // Filter out empty categories
                val nonEmptyCategories = if (formData.category.isNotEmpty()) {
                    listOf(formData.category)
                } else {
                    emptyList()
                }
                
                // Create expense object
                val expense = Expense(
                    id = expenseId ?: "",
                    projectId = projectId,
                    date = dateString,
                    amount = amount,
                    department = formData.department, // Save only the department name (no phaseId prefix)
                    phaseName = phaseName.takeIf { it.isNotEmpty() },
                    categories = nonEmptyCategories, // Filtered non-empty categories
                    description = formData.description,
                    modeOfPayment = PaymentMode.fromString(formData.modeOfPayment),
                    status = ExpenseStatus.PENDING, // Reset to PENDING when editing
                    phaseId = formData.phaseId,
                    attachmentURL = finalAttachmentUrl.takeIf { it.isNotEmpty() },
                    attachmentName = finalAttachmentFileName.takeIf { finalAttachmentFileName.isNotEmpty() },
                    paymentProofURL = finalPaymentProofUrl.takeIf { it.isNotEmpty() },
                    paymentProofName = finalPaymentProofFileName.takeIf { finalPaymentProofFileName.isNotEmpty() },
                    submittedBy = userId,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now(),
                    // Material details (optional - for material-based expense entry)
                    itemType = formData.itemType.takeIf { it.isNotEmpty() },
                    item = formData.item.takeIf { it.isNotEmpty() },
                    brand = formData.brand.takeIf { it.isNotEmpty() },
                    spec = formData.spec.takeIf { it.isNotEmpty() },
                    thickness = formData.thickness.takeIf { it.isNotEmpty() },
                    quantity = formData.quantity.takeIf { it.isNotEmpty() },
                    uom = formData.uom.takeIf { it.isNotEmpty() },
                    unitPrice = formData.unitPrice.takeIf { it.isNotEmpty() },
                    // Tax fields
                    sgst = formData.sgst,
                    cgst = formData.cgst,
                    // Vendor and payment fields
                    vendor = formData.vendor,
                    byCredit = formData.byCredit,
                    // Legacy fields for backward compatibility (excluding attachmentUrl and attachmentFileName as per requirements)
                    userId = userId,
                    userName = userName,
                    paymentProofUrl = finalPaymentProofUrl,
                    paymentProofFileName = finalPaymentProofFileName,
                    submittedAt = Timestamp.now(),
                    budgetExceeded = isBudgetExceeded,
                )
                
                val result = if (expenseId != null && expenseId.isNotEmpty()) {
                    // Update existing expense
                    expenseRepository.updateExpense(expense)
                } else {
                    // Create new expense
                    expenseRepository.addExpense(expense)
                }
                
                if (result.isSuccess) {
                    val finalExpenseId: String = expenseId ?: (result.getOrNull() as? String) ?: ""
                    // Update expense with the actual ID from Firestore
                    val expenseWithId = expense.copy(id = finalExpenseId)
                    
                    // Send notifications using the expense with actual ID (only for new expenses)
                    // If budget exceeded, send only to Production Head
                    if (expenseId == null) {
                    sendExpenseSubmissionNotifications(expenseWithId, projectId, isBudgetExceeded)
                    }
                    
                    // Clear form and error (but keep current date as default)
                    _formData.value = ExpenseFormData(
                        date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                    )
                    _error.value = null
                    _fieldError.value = null
                    _budgetWarning.value = null
                    _budgetValidationResult.value = null
                    _successMessage.value = if (expenseId != null) "Expense updated successfully! ✅" else "Expense submitted successfully! ✅"
                    _isSubmitting.value = false
                    
                    // Minimal delay for faster navigation - notifications are sent asynchronously
                    delay(300) // Reduced to 300ms for faster submission
                    _successMessage.value = null
                    onSuccess()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to ${if (expenseId != null) "update" else "submit"} expense"
                    _isSubmitting.value = false
                }
                
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred while submitting expense"
                _isSubmitting.value = false
            }
        }
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    fun clearExpenseError() {
        _error.value = null
        _fieldError.value = null
    }

    fun resetForm() {
        // Reset form but keep current date as default
        _formData.value = ExpenseFormData(
            date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        )
        _error.value = null
        _fieldError.value = null
        _successMessage.value = null
        _budgetWarning.value = null
        _budgetValidationResult.value = null
    }
    
    // Expense editing cache methods for instant loading
    fun setExpenseForEditing(expense: Expense) {
        Log.d("ExpenseViewModel", "✅ Caching expense for instant editing: ${expense.id}")
        _expenseForEditing.value = expense
        // Immediately populate form data for instant display
        populateFormFromExpense(expense)
    }
    
    fun clearExpenseForEditing() {
        Log.d("ExpenseViewModel", "🧹 Clearing cached expense")
        _expenseForEditing.value = null
    }
    
    private fun populateFormFromExpense(expense: Expense) {
        Log.d("ExpenseViewModel", "📝 Populating form from expense: ${expense.id}")
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateString = expense.getDateAsTimestamp()?.toDate()?.let { dateFormat.format(it) }
            ?: expense.date
        
        // Clean department name - remove document ID prefix (everything before and including "_")
        val cleanDepartmentName = com.cubiquitous.tracura.utils.FormatUtils.getDepartmentDisplayName(expense.department)
        
        _formData.value = _formData.value.copy(
            date = dateString,
            amount = expense.amount.toString(),
            department = cleanDepartmentName, // Use cleaned department name for display
            phaseName = expense.phaseName ?: "", // Add phase name for display
            category = expense.categories.firstOrNull() ?: "",
            description = expense.description,
            modeOfPayment = PaymentMode.toString(expense.modeOfPayment),
            phaseId = expense.phaseId ?: "",
            vendor = expense.vendor,
            // Material fields
            itemType = expense.itemType ?: "",
            item = expense.item ?: "",
            brand = expense.brand ?: "",
            spec = expense.spec ?: "",
            thickness = expense.thickness ?: "",
            quantity = expense.quantity ?: "",
            uom = expense.uom ?: "",
            unitPrice = expense.unitPrice ?: "",
            // Tax fields
            sgst = expense.sgst,
            cgst = expense.cgst,
            // Attachment URIs (filenames are stored in Expense model, not FormData)
            attachmentUri = expense.attachmentURL ?: "",
            paymentProofUri = expense.paymentProofURL ?: "",
            byCredit = expense.byCredit
        )
        
        Log.d("ExpenseViewModel", "✅ Form populated - Department: '$cleanDepartmentName', Phase: '${expense.phaseName}'")
    }
    
    
    // Budget validation methods
    fun validateBudget(projectId: String, department: String, amount: Double, phaseId: String? = null) {
        viewModelScope.launch {
            try {
                Log.d("ExpenseViewModel", "🔍 Validating budget for department: $department, phase: $phaseId, amount: $amount")
                val validationResult = budgetValidationService.validateExpenseAgainstBudget(
                    projectId = projectId,
                    department = department,
                    newExpenseAmount = amount,
                    phaseId = phaseId
                )
                
                _budgetValidationResult.value = validationResult
                
                if (!validationResult.isValid) {
                    _budgetWarning.value = validationResult.warningMessage
                    Log.w("ExpenseViewModel", "⚠️ Budget validation failed: ${validationResult.warningMessage}")
                } else {
                    _budgetWarning.value = null
                    Log.d("ExpenseViewModel", "✅ Budget validation passed")
                }
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Error validating budget: ${e.message}")
                _budgetWarning.value = "Error validating budget: ${e.message}"
            }
        }
    }
    
    fun clearBudgetWarning() {
        _budgetWarning.value = null
        _budgetValidationResult.value = null
    }
    
    fun getBudgetSummary(projectId: String) {
        viewModelScope.launch {
            try {
                val budgetSummary = budgetValidationService.getProjectBudgetSummary(projectId)
                Log.d("ExpenseViewModel", "📊 Budget summary loaded: ${budgetSummary.keys}")
                // You can emit this to a StateFlow if needed for UI display
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Error getting budget summary: ${e.message}")
            }
        }
    }
    
    // Method to update user names for existing expenses
    fun updateExistingExpenseUserNames(projectId: String, userId: String, userName: String) {
        viewModelScope.launch {
            try {
                Log.d("ExpenseViewModel", "🔄 Updating existing expense user names...")
                val result = expenseRepository.updateExpenseUserNames(projectId, userId, userName)
                
                if (result.isSuccess) {
                    val updatedCount = result.getOrNull() ?: 0
                    Log.d("ExpenseViewModel", "✅ Updated $updatedCount expenses with userName: $userName")
                    _successMessage.value = "Updated $updatedCount expenses with user name: $userName"
                } else {
                    Log.e("ExpenseViewModel", "❌ Failed to update expense user names: ${result.exceptionOrNull()?.message}")
                    _error.value = "Failed to update expense user names: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Error updating expense user names: ${e.message}")
                _error.value = "Error updating expense user names: ${e.message}"
            }
        }
    }

    private fun calculateUserSummary(expenses: List<Expense>) {
        val approved = expenses.filter { it.status == ExpenseStatus.APPROVED }
        val pending = expenses.filter { it.status == ExpenseStatus.PENDING }
        val rejected = expenses.filter { it.status == ExpenseStatus.REJECTED }
        val complete = expenses.filter { it.status == ExpenseStatus.COMPLETE }
        
        val totalApproved = approved.sumOf { it.amount }
        val totalPending = pending.sumOf { it.amount }
        val totalRejected = rejected.sumOf { it.amount }
        
        // Group approved expenses by category
        val expensesByCategory = approved
            .filter { it.category.isNotEmpty() }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        
        // Group approved expenses by project
        val projectMap = _projects.value.associateBy { it.id }
        val expensesByProject = approved
            .filter { it.projectId.isNotEmpty() }
            .groupBy { it.projectId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .mapKeys { entry -> projectMap[entry.key]?.name ?: "Unknown Project" }
        
        // Get recent approved expenses
        val recentApproved = approved
            .sortedByDescending { it.reviewedAt?.toDate()?.time ?: 0L }
            .take(5)
        
        _expenseSummary.value = ExpenseSummary(
            totalApproved = totalApproved,
            totalPending = totalPending,
            totalRejected = totalRejected,
            approvedCount = approved.size,
            pendingCount = pending.size,
            rejectedCount = rejected.size,
            expensesByCategory = expensesByCategory,
            expensesByProject = expensesByProject,
            recentExpenses = recentApproved
        )
        
        Log.d("ExpenseViewModel", "📊 Summary - Approved: $totalApproved, Pending: $totalPending, Rejected: $totalRejected")
    }

    private fun calculateProjectSummary(expenses: List<Expense>) {
        val approved = expenses.filter { it.status == ExpenseStatus.APPROVED }
        val pending = expenses.filter { it.status == ExpenseStatus.PENDING }
        val rejected = expenses.filter { it.status == ExpenseStatus.REJECTED }
        val complete = expenses.filter { it.status == ExpenseStatus.COMPLETE }
        
        // Only count approved expenses in the total - as per user requirement
        val totalExpenses = approved.sumOf { it.amount }
        
        // Group only approved expenses by category - as per user requirement
        val expensesByCategory = approved
            .filter { it.category.isNotEmpty() }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        
        _expenseSummary.value = ExpenseSummary(
            totalExpenses = totalExpenses,
            approvedCount = approved.size,
            pendingCount = pending.size,
            rejectedCount = rejected.size,
            expensesByCategory = expensesByCategory
        )
        
        _statusCounts.value = StatusCounts(
            approved = approved.size,
            pending = pending.size,
            rejected = rejected.size,
            complete = complete.size,
            total = expenses.size
        )
        
        Log.d("ExpenseViewModel", "📊 Project Summary - Total Approved: $totalExpenses, Approved: ${approved.size}, Pending: ${pending.size}, Rejected: ${rejected.size}")
    }

    fun refreshData(userId: String) {
        loadUserExpenses(userId)
    }
    
    fun refreshProjectData(projectId: String) {
        Log.d("ExpenseViewModel", "🔄 Manually refreshing project expenses for: $projectId")
        loadProjectExpenses(projectId)
    }
    
    // Simple direct loading method for testing
    fun loadUserExpensesForProjectDirect(projectId: String, userId: String) {
        viewModelScope.launch {
            Log.d("ExpenseViewModel", "🔍 DIRECT LOADING: project=$projectId, user=$userId")
            _isLoading.value = true
            _error.value = null
            
            try {
                // Load project details
                val project = projectRepository.getProjectById(projectId)
                _selectedProject.value = project
                
                if (project == null) {
                    Log.e("ExpenseViewModel", "❌ Project not found: $projectId")
                    _error.value = "Project not found"
                    _isLoading.value = false
                    return@launch
                }
                
                // Use direct query
                val expenses = expenseRepository.getUserExpensesForProjectDirect(projectId, userId)
                Log.d("ExpenseViewModel", "📊 Direct loading received ${expenses.size} expenses")
                
                // Update all state
                _expenses.value = expenses
                _filteredExpenses.value = expenses
                calculateUserProjectSummary(expenses)
                
                Log.d("ExpenseViewModel", "✅ Direct loading completed successfully")
                _isLoading.value = false
                
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Direct loading failed: ${e.message}")
                e.printStackTrace()
                _error.value = "Failed to load expenses: ${e.message}"
                _isLoading.value = false
                
                // Reset state
                _expenses.value = emptyList()
                _filteredExpenses.value = emptyList()
                _statusCounts.value = StatusCounts()
            }
        }
    }
    
    fun loadUserExpensesForProject(projectId: String, userId: String) {
        viewModelScope.launch {
            Log.d("ExpenseViewModel", "🚀 STARTING loadUserExpensesForProject: project=$projectId, user=$userId")
            _isLoading.value = true
            _error.value = null
            
            try {
                // Load project details first
                Log.d("ExpenseViewModel", "🏗️ Loading project details for: $projectId")
                val project = projectRepository.getProjectById(projectId)
                _selectedProject.value = project
                Log.d("ExpenseViewModel", "🏗️ Project loaded: ${project?.name} (Budget: ₹${project?.budget})")
                
                if (project == null) {
                    Log.e("ExpenseViewModel", "❌ Project not found: $projectId")
                    _error.value = "Project not found"
                    _isLoading.value = false
                    return@launch
                }
                
                // Use direct loading for immediate results
                Log.d("ExpenseViewModel", "🔄 Loading expenses directly first...")
                val directExpenses = expenseRepository.getUserExpensesForProjectDirect(projectId, userId)
                Log.d("ExpenseViewModel", "📊 Direct loading received ${directExpenses.size} expenses")
                
                // Update state immediately with direct results
                _expenses.value = directExpenses
                _filteredExpenses.value = directExpenses
                calculateUserProjectSummary(directExpenses)
                
                // Set up real-time listener for updates in a separate coroutine
                Log.d("ExpenseViewModel", "🔄 Setting up real-time listener for ongoing updates...")
                viewModelScope.launch {
                    try {
                        expenseRepository.getUserExpensesForProject(projectId, userId)
                            .catch { exception ->
                                Log.e("ExpenseViewModel", "❌ Error in real-time listener: ${exception.message}")
                                // Don't clear data on listener error, keep showing what we have
                                Log.w("ExpenseViewModel", "⚠️ Keeping existing data due to real-time listener error")
                            }
                            .collect { expenses ->
                                Log.d("ExpenseViewModel", "📊 Real-time update: ${expenses.size} expenses")
                                
                                // Only update if we have new data and it's not empty (unless current is also empty)
                                if (expenses != _expenses.value && (expenses.isNotEmpty() || _expenses.value.isEmpty())) {
                                    _expenses.value = expenses
                                    
                                    // Update filtered expenses based on current filter
                                    if (_selectedStatusFilter.value == null) {
                                        _filteredExpenses.value = expenses
                                    } else {
                                        _filteredExpenses.value = expenses.filter { it.status == _selectedStatusFilter.value }
                                    }
                                    
                                    // Recalculate status counts
                                    calculateUserProjectSummary(expenses)
                                } else if (expenses.isEmpty() && _expenses.value.isNotEmpty()) {
                                    Log.w("ExpenseViewModel", "⚠️ Ignoring empty real-time update to preserve existing data")
                                }
                            }
                    } catch (e: Exception) {
                        Log.e("ExpenseViewModel", "❌ Error in real-time listener setup: ${e.message}")
                        // Don't clear data on error, keep showing what we have
                        Log.w("ExpenseViewModel", "⚠️ Keeping existing data due to real-time listener setup error")
                    }
                }
                
                // Only set loading to false after we have data and listener is set up
                _isLoading.value = false
                
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Error loading user expenses: ${e.message}")
                e.printStackTrace()
                _error.value = "Failed to load expenses: ${e.message}"
                _isLoading.value = false
                
                // Ensure we don't leave the UI in a hanging state
                _expenses.value = emptyList()
                _filteredExpenses.value = emptyList()
                _statusCounts.value = StatusCounts()
            }
        }
    }
    
    // Fallback method using direct query
    private fun loadUserExpensesForProjectFallback(projectId: String, userId: String) {
        viewModelScope.launch {
            Log.d("ExpenseViewModel", "🔄 Using fallback method for loading user expenses")
            _isLoading.value = true
            _error.value = null
            
            try {
                // Use direct query as fallback
                val expenses = expenseRepository.getUserExpensesForProjectDirect(projectId, userId)
                Log.d("ExpenseViewModel", "📊 Fallback method received ${expenses.size} expenses")
                
                // Update expenses state
                _expenses.value = expenses
                
                // Calculate status counts and update filtered expenses
                calculateUserProjectSummary(expenses)
                
                // If no filter is applied, show all expenses
                if (_selectedStatusFilter.value == null) {
                    _filteredExpenses.value = expenses
                    Log.d("ExpenseViewModel", "📋 Showing all ${expenses.size} expenses (no filter)")
                } else {
                    // Apply current filter
                    val filteredExpenses = expenses.filter { it.status == _selectedStatusFilter.value }
                    _filteredExpenses.value = filteredExpenses
                    Log.d("ExpenseViewModel", "📋 Showing ${filteredExpenses.size} expenses (filter: ${_selectedStatusFilter.value})")
                }
                
                // Log detailed status breakdown
                val statusBreakdown = expenses.groupBy { it.status }
                    .mapValues { it.value.size }
                Log.d("ExpenseViewModel", "📊 Fallback status breakdown: $statusBreakdown")
                
                // Log current status counts for debugging
                val currentCounts = _statusCounts.value
                Log.d("ExpenseViewModel", "📊 Fallback status counts: Approved=${currentCounts.approved}, Pending=${currentCounts.pending}, Rejected=${currentCounts.rejected}, Total=${currentCounts.total}")
                
                _isLoading.value = false
                
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Fallback method also failed: ${e.message}")
                e.printStackTrace()
                _error.value = "Failed to load expenses: ${e.message}"
                _isLoading.value = false
                
                // Ensure we don't leave the UI in a hanging state
                _expenses.value = emptyList()
                _filteredExpenses.value = emptyList()
                _statusCounts.value = StatusCounts()
            }
        }
    }

    private fun calculateUserProjectSummary(expenses: List<Expense>) {
        Log.d("ExpenseViewModel", "🧮 Calculating user project summary for ${expenses.size} expenses")
        
        // Filter expenses by status with detailed logging
        val approved = expenses.filter { it.status == ExpenseStatus.APPROVED }
        val pending = expenses.filter { it.status == ExpenseStatus.PENDING }
        val rejected = expenses.filter { it.status == ExpenseStatus.REJECTED }
        val complete = expenses.filter { it.status == ExpenseStatus.COMPLETE }

        
        // Log sample expenses for each status
        if (approved.isNotEmpty()) {
            approved.take(2).forEach { expense ->
                Log.d("ExpenseViewModel", "  ✅ ${expense.id}: ${expense.category} - ₹${expense.amount}")
            }
        }
        
        if (pending.isNotEmpty()) {
            Log.d("ExpenseViewModel", "📋 Sample pending expenses:")
            pending.take(2).forEach { expense ->
                Log.d("ExpenseViewModel", "  ⏳ ${expense.id}: ${expense.category} - ₹${expense.amount}")
            }
        }
        
        if (rejected.isNotEmpty()) {
            Log.d("ExpenseViewModel", "📋 Sample rejected expenses:")
            rejected.take(2).forEach { expense ->
                Log.d("ExpenseViewModel", "  ❌ ${expense.id}: ${expense.category} - ₹${expense.amount}")
            }
        }
        
        // Calculate totals
        val totalExpenses = expenses.sumOf { it.amount }
        val approvedAmount = approved.sumOf { it.amount }
        val pendingAmount = pending.sumOf { it.amount }
        val rejectedAmount = rejected.sumOf { it.amount }
        
        Log.d("ExpenseViewModel", "💰 Amount breakdown:")
        Log.d("ExpenseViewModel", "  💰 Total: ₹$totalExpenses")
        Log.d("ExpenseViewModel", "  ✅ Approved: ₹$approvedAmount")
        Log.d("ExpenseViewModel", "  ⏳ Pending: ₹$pendingAmount")
        Log.d("ExpenseViewModel", "  ❌ Rejected: ₹$rejectedAmount")
        
        // Group user's expenses by category
        val expensesByCategory = expenses
            .filter { it.category.isNotEmpty() }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        
        Log.d("ExpenseViewModel", "🏷️ Category breakdown: ${expensesByCategory.keys}")
        
        // Update expense summary
        _expenseSummary.value = ExpenseSummary(
            totalExpenses = totalExpenses,
            totalApproved = approvedAmount,
            totalPending = pendingAmount,
            totalRejected = rejectedAmount,
            approvedCount = approved.size,
            pendingCount = pending.size,
            rejectedCount = rejected.size,
            expensesByCategory = expensesByCategory
        )
        
        // Update status counts with detailed logging
        val newStatusCounts = StatusCounts(
            approved = approved.size,
            pending = pending.size,
            rejected = rejected.size,
            complete = complete.size,
            total = expenses.size
        )
        
        Log.d("ExpenseViewModel", "📊 UPDATING STATUS COUNTS:")
        Log.d("ExpenseViewModel", "  ✅ Approved: ${newStatusCounts.approved}")
        Log.d("ExpenseViewModel", "  ⏳ Pending: ${newStatusCounts.pending}")
        Log.d("ExpenseViewModel", "  ❌ Rejected: ${newStatusCounts.rejected}")
        Log.d("ExpenseViewModel", "  ✅ Complete: ${newStatusCounts.complete}")
        Log.d("ExpenseViewModel", "  📊 Total: ${newStatusCounts.total}")
        
        _statusCounts.value = newStatusCounts
        
        // Verify the update was successful
        val verifyUpdate = _statusCounts.value
        Log.d("ExpenseViewModel", "🔍 VERIFICATION - Status counts after update:")
        Log.d("ExpenseViewModel", "  ✅ Approved: ${verifyUpdate.approved}")
        Log.d("ExpenseViewModel", "  ⏳ Pending: ${verifyUpdate.pending}")
        Log.d("ExpenseViewModel", "  ❌ Rejected: ${verifyUpdate.rejected}")
        Log.d("ExpenseViewModel", "  📊 Total: ${verifyUpdate.total}")
        
        Log.d("ExpenseViewModel", "✅ User Project Summary calculation complete")
    }

    fun clearFilter() {
        _selectedStatusFilter.value = null
        _filteredExpenses.value = _expenses.value
    }

    fun filterByStatus(status: ExpenseStatus) {
        _selectedStatusFilter.value = status
        _filteredExpenses.value = _expenses.value.filter { it.status == status }
    }
    
    fun clearData() {
        _expenses.value = emptyList()
        _filteredExpenses.value = emptyList()
        _statusCounts.value = StatusCounts()
        _selectedStatusFilter.value = null
        _error.value = null
        Log.d("ExpenseViewModel", "🧹 Cleared all expense data")
    }
    
    // Method to manually recalculate status counts from current expenses
    fun recalculateStatusCounts() {
        val currentExpenses = _expenses.value
        Log.d("ExpenseViewModel", "🔄 Manually recalculating status counts from ${currentExpenses.size} expenses")
        calculateUserProjectSummary(currentExpenses)
    }
    
    // Force refresh all data for dynamic updates
    fun forceRefreshData(projectId: String, userId: String) {
        viewModelScope.launch {
            Log.d("ExpenseViewModel", "🔄 Force refreshing data for project: $projectId, user: $userId")
            _isLoading.value = true
            _error.value = null
            
            try {
                // Clear existing data
                _expenses.value = emptyList()
                _filteredExpenses.value = emptyList()
                _statusCounts.value = StatusCounts()
                _selectedStatusFilter.value = null
                
                // Reload project details
                val project = projectRepository.getProjectById(projectId)
                _selectedProject.value = project
                
                // Force reload expenses with fresh data
                val expenses = expenseRepository.getUserExpensesForProjectDirect(projectId, userId)
                Log.d("ExpenseViewModel", "📊 Force refresh received ${expenses.size} expenses")
                
                // Update all state
                _expenses.value = expenses
                _filteredExpenses.value = expenses
                calculateUserProjectSummary(expenses)
                
                Log.d("ExpenseViewModel", "✅ Force refresh completed successfully")
                
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Force refresh failed: ${e.message}")
                _error.value = "Failed to refresh data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Update status counts immediately when expenses change
    private fun updateStatusCountsImmediately() {
        val currentExpenses = _expenses.value
        Log.d("ExpenseViewModel", "⚡ Updating status counts immediately for ${currentExpenses.size} expenses")
        calculateUserProjectSummary(currentExpenses)
    }
    
    // Enhanced method to handle real-time status updates
    fun handleStatusUpdate(expenseId: String, newStatus: ExpenseStatus, reviewerName: String, comments: String) {
        viewModelScope.launch {
            try {
                Log.d("ExpenseViewModel", "🔄 Handling status update: $expenseId -> $newStatus")
                
                // Update the expense in the local list
                val updatedExpenses = _expenses.value.map { expense ->
                    if (expense.id == expenseId) {
                        expense.copy(
                            status = newStatus,
                            reviewedAt = com.google.firebase.Timestamp.now(),
                            reviewedBy = reviewerName,
                            reviewComments = comments
                        )
                    } else {
                        expense
                    }
                }
                
                // Update state immediately for instant UI feedback
                _expenses.value = updatedExpenses
                
                // Update filtered expenses if needed
                if (_selectedStatusFilter.value != null) {
                    _filteredExpenses.value = updatedExpenses.filter { it.status == _selectedStatusFilter.value }
                } else {
                    _filteredExpenses.value = updatedExpenses
                }
                
                // Recalculate status counts
                updateStatusCountsImmediately()
                
                Log.d("ExpenseViewModel", "✅ Status update handled successfully")
                
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Error handling status update: ${e.message}")
                _error.value = "Failed to update status: ${e.message}"
            }
        }
    }
    
    
    fun addDemoExpenses(projectId: String, userId: String, userName: String) {
        viewModelScope.launch {
            try {
                Log.d("ExpenseViewModel", "🎬 Adding demo expenses for testing...")
                
                // Get project departments and categories dynamically
                val project = projectRepository.getProjectById(projectId)
                val projectDepartments = project?.departmentBudgets?.keys?.toList() ?: emptyList()
                val projectCategories = project?.categories ?: emptyList()
                
                // Only create demo expenses if project has categories and departments
                if (projectCategories.isEmpty() || projectDepartments.isEmpty()) {
                    Log.w("ExpenseViewModel", "⚠️ Cannot add demo expenses: Project has no categories or departments")
                    _error.value = "Cannot add demo expenses: Project must have categories and departments configured"
                    return@launch
                }
                
                val demoExpenses = listOf(
                    Expense(
                        id = "demo_1_${System.currentTimeMillis()}",
                        projectId = projectId,
                        date = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()),
                        amount = 5000.0,
                        department = projectDepartments.first(),
                        categories = listOf(projectCategories.first()),
                        description = "Camera operator payment",
                        modeOfPayment = PaymentMode.CASH,
                        status = ExpenseStatus.APPROVED,
                        submittedBy = userId,
                        createdAt = Timestamp.now(),
                        updatedAt = Timestamp.now(),
                        userId = userId,
                        userName = userName,
                        submittedAt = Timestamp.now(),
                        receiptNumber = "RCP001"
                    ),
                    Expense(
                        id = "demo_2_${System.currentTimeMillis() + 1}",
                        projectId = projectId,
                        date = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()),
                        amount = 3000.0,
                        department = projectDepartments.getOrNull(1) ?: projectDepartments.first(),
                        categories = listOf(projectCategories.getOrNull(1) ?: projectCategories.first()),
                        description = "Lighting equipment rental",
                        modeOfPayment = PaymentMode.UPI,
                        status = ExpenseStatus.PENDING,
                        submittedBy = userId,
                        createdAt = Timestamp.now(),
                        updatedAt = Timestamp.now(),
                        userId = userId,
                        userName = userName,
                        submittedAt = Timestamp.now(),
                        receiptNumber = "RCP002"
                    ),
                    Expense(
                        id = "demo_3_${System.currentTimeMillis() + 2}",
                        projectId = projectId,
                        date = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()),
                        amount = 1500.0,
                        department = projectDepartments.getOrNull(2) ?: projectDepartments.first(),
                        categories = listOf(projectCategories.getOrNull(2) ?: projectCategories.first()),
                        description = "Lunch for crew",
                        modeOfPayment = PaymentMode.CASH,
                        status = ExpenseStatus.REJECTED,
                        submittedBy = userId,
                        createdAt = Timestamp.now(),
                        updatedAt = Timestamp.now(),
                        reviewComments = "Receipt not clear, please resubmit",
                        userId = userId,
                        userName = userName,
                        submittedAt = Timestamp.now(),
                        receiptNumber = "RCP003"
                    ),
                    Expense(
                        id = "demo_4_${System.currentTimeMillis() + 3}",
                        projectId = projectId,
                        date = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()),
                        amount = 2500.0,
                        department = projectDepartments.getOrNull(3) ?: projectDepartments.first(),
                        categories = listOf(projectCategories.getOrNull(3) ?: projectCategories.first()),
                        description = "Vehicle rental for location",
                        modeOfPayment = PaymentMode.CHEQUE,
                        status = ExpenseStatus.APPROVED,
                        submittedBy = userId,
                        createdAt = Timestamp.now(),
                        updatedAt = Timestamp.now(),
                        userId = userId,
                        userName = userName,
                        submittedAt = Timestamp.now(),
                        receiptNumber = "RCP004"
                    ),
                    Expense(
                        id = "demo_5_${System.currentTimeMillis() + 4}",
                        projectId = projectId,
                        date = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()),
                        amount = 800.0,
                        department = projectDepartments.getOrNull(4) ?: projectDepartments.first(),
                        categories = listOf(projectCategories.getOrNull(4) ?: projectCategories.first()),
                        description = "Costume materials",
                        modeOfPayment = PaymentMode.UPI,
                        status = ExpenseStatus.PENDING,
                        submittedBy = userId,
                        createdAt = Timestamp.now(),
                        updatedAt = Timestamp.now(),
                        userId = userId,
                        userName = userName,
                        submittedAt = Timestamp.now(),
                        receiptNumber = "RCP005"
                    )
                )
                
                // Update local state immediately for instant feedback
                _expenses.value = demoExpenses
                _filteredExpenses.value = demoExpenses
                calculateUserProjectSummary(demoExpenses)
                
                // Also save to Firebase in background
                var allSuccess = true
                var errorMessage: String? = null
                for (expense in demoExpenses) {
                    try {
                        val result = expenseRepository.addExpense(expense)
                        if (result.isFailure) {
                            allSuccess = false
                            errorMessage = result.exceptionOrNull()?.message
                            Log.e("ExpenseViewModel", "❌ Error saving to Firebase: $errorMessage")
                        }
                    } catch (e: Exception) {
                        allSuccess = false
                        errorMessage = e.message
                        Log.e("ExpenseViewModel", "❌ Exception saving to Firebase: $errorMessage")
                    }
                }
                if (allSuccess) {
                    _successMessage.value = "Demo expenses added successfully!"
                    Log.d("ExpenseViewModel", "✅ Added ${demoExpenses.size} demo expenses to Firestore")
                } else {
                    _error.value = "Failed to add some demo expenses: $errorMessage"
                }
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Error adding demo expenses: ${e.message}")
                _error.value = "Failed to add demo expenses: ${e.message}"
            }
        }
    }
    
    private fun sendExpenseSubmissionNotifications(expense: Expense, projectId: String, budgetExceeded: Boolean = false) {
        viewModelScope.launch {
            try {
                Log.d("ExpenseViewModel", "🔄 Sending expense submission notifications for expense: ${expense.id}, budgetExceeded: $budgetExceeded")
                
                // Use the new NotificationService to send project-specific notifications
                notificationService.sendExpenseSubmissionNotification(
                        projectId = projectId,
                        expenseId = expense.id,
                        submittedBy = expense.userName,
                        amount = expense.amount,
                        category = expense.category,
                        budgetExceeded = budgetExceeded
                ).onSuccess {
                    Log.d("ExpenseViewModel", "✅ Successfully sent expense submission notifications")
                }.onFailure { error ->
                    Log.e("ExpenseViewModel", "❌ Failed to send expense submission notifications: ${error.message}")
                }
                
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Error sending expense submission notifications: ${e.message}")
            }
        }
    }
    
    /**
     * Submit a phase override request
     */
    fun submitPhaseOverrideRequest(
        projectId: String,
        phaseId: String,
        reason: String,
        extendedDate: Date,
        userPhoneNumber: String,
        phase: com.cubiquitous.tracura.model.Phase
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _requestSubmissionSuccess.value = false
            
            try {
                // Format date as dd/MM/yyyy
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val extendedDateString = dateFormatter.format(extendedDate)
                
                Log.d("ExpenseViewModel", "📝 Submitting phase override request:")
                Log.d("ExpenseViewModel", "   Project ID: $projectId")
                Log.d("ExpenseViewModel", "   Phase ID: $phaseId")
                Log.d("ExpenseViewModel", "   Reason: $reason")
                Log.d("ExpenseViewModel", "   Extended Date: $extendedDateString")
                Log.d("ExpenseViewModel", "   User Phone: $userPhoneNumber")
                
                val result = projectRepository.addPhaseOverrideRequest(
                    projectId = projectId,
                    phaseId = phaseId,
                    reason = reason,
                    extendedDate = extendedDateString,
                    userPhoneNumber = userPhoneNumber,
                    phase = phase
                )
                
                result.onSuccess { requestId ->
                    Log.d("ExpenseViewModel", "✅ Phase override request submitted successfully with ID: $requestId")
                    _requestSubmissionSuccess.value = true
                    _successMessage.value = "Request submitted successfully"
                }.onFailure { error ->
                    Log.e("ExpenseViewModel", "❌ Failed to submit phase override request: ${error.message}")
                    _error.value = "Failed to submit request: ${error.message}"
                }
                
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Error submitting phase override request: ${e.message}", e)
                _error.value = "Error submitting request: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load phase override requests
     */
    fun loadPhaseOverrideRequests(projectId: String, phaseId: String, userPhoneNumber: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val requests = projectRepository.getPhaseOverrideRequests(
                    projectId = projectId,
                    phaseId = phaseId,
                    userPhoneNumber = userPhoneNumber
                )
                
                _phaseOverrideRequests.value = requests
                Log.d("ExpenseViewModel", "✅ Loaded ${requests.size} phase override requests")
                
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Error loading phase override requests: ${e.message}", e)
                _error.value = "Failed to load requests: ${e.message}"
            } finally {
                _isLoading.value = false
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
            Log.e("ExpenseViewModel", "Error checking approved phase request: ${e.message}", e)
            false
        }
    }
    
    /**
     * Start a phase by setting its start date to today
     * Per Dashboard Phase Logic Implementation Guide
     */
    fun startPhaseNow(projectId: String, phaseId: String, phaseName: String, currentEndDate: String?) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Format today's date as "dd/MM/yyyy"
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                val todayFormatted = dateFormatter.format(today)
                
                // Update phase start date to today
                val result = projectRepository.updatePhaseDetails(
                    projectId = projectId,
                    phaseId = phaseId,
                    phaseName = phaseName,
                    startDate = todayFormatted,
                    endDate = currentEndDate
                )
                
                if (result.isSuccess) {
                    // Reload phases to reflect the change
                    val updatedPhases = getScopedPhasesForProject(projectId)
                    _phases.value = updatedPhases
                    
                    Log.d("ExpenseViewModel", "✅ Phase $phaseId started successfully. Start date set to: $todayFormatted")
                } else {
                    _error.value = "Failed to start phase. Please try again."
                    Log.e("ExpenseViewModel", "❌ Error starting phase: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _error.value = "Error starting phase: ${e.message}"
                Log.e("ExpenseViewModel", "❌ Error starting phase: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
} 
