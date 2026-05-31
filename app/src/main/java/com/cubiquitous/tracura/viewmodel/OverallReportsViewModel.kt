package com.cubiquitous.tracura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.*
import com.cubiquitous.tracura.repository.ExpenseRepository
import com.cubiquitous.tracura.repository.ProjectRepository
import com.cubiquitous.tracura.repository.ExportRepository
import com.cubiquitous.tracura.repository.ProfessionalExportRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.*
import javax.inject.Inject

data class OverallReportData(
    val totalSpent: Double = 0.0,
    val totalBudget: Double = 0.0,
    val budgetUsagePercentage: Double = 0.0,
    val expensesByCategory: Map<String, Double> = emptyMap(),
    val expensesByDepartment: Map<String, Double> = emptyMap(),
    val expensesByProject: Map<String, Double> = emptyMap(),
    val detailedExpenses: List<DetailedExpenseWithProject> = emptyList(),
    val timeRange: String = "This Year",
    val selectedProject: String = "All Projects",
    val availableProjects: List<Project> = emptyList()
)

data class DetailedExpenseWithProject(
    val id: String = "",
    val date: Timestamp? = null,
    val invoice: String = "",
    val by: String = "",
    val amount: Double = 0.0,
    val department: String = "",
    val category: String = "",
    val description: String = "",
    val modeOfPayment: String = "",
    val projectName: String = "",
    val phaseId: String = "",
    val attachmentUrl: String = "",
    val attachmentFileName: String = "",
    val paymentProofUrl: String = "",
    val paymentProofFileName: String = ""
)

data class ProjectFilter(
    val name: String,
    val value: String
)

@HiltViewModel
class OverallReportsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository,
    private val exportRepository: ExportRepository,
    private val professionalExportRepository: ProfessionalExportRepository
) : ViewModel() {
    
    private val _reportData = MutableStateFlow(OverallReportData())
    val reportData: StateFlow<OverallReportData> = _reportData.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _selectedTimeRange = MutableStateFlow("this_year")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()
    
    private val _selectedProject = MutableStateFlow("all")
    val selectedProject: StateFlow<String> = _selectedProject.asStateFlow()
    
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()
    
    // Pagination for detailed expenses
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    
    private val _hasMoreExpenses = MutableStateFlow(false)
    val hasMoreExpenses: StateFlow<Boolean> = _hasMoreExpenses.asStateFlow()
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
    
    private val pageSize = 15 // Show 15 expenses per page
    
    private var allProjects: List<Project> = emptyList()
    private var allExpenses: List<Expense> = emptyList()
    private var filteredExpenses: List<DetailedExpenseWithProject> = emptyList()
    
    // New fields for user-specific filtering
    private var currentUserId: String? = null
    private var currentUserRole: String? = null
    
    init {
        // Don't auto-load data until user context is set
    }
    
    // New method to set user context and load data
    fun setUserContextAndLoadData(userId: String, userRole: String) {
        currentUserId = userId
        currentUserRole = userRole
        loadOverallReports()
    }
    
    // Helper method to load approver projects
    private suspend fun loadApproverProjects(userId: String): List<Project> {
        return try {
            android.util.Log.d("OverallReportsViewModel", "🔄 Starting to load approver projects for user: $userId")
            
            // Get the first emission from the Flow without timeout
            val projects = projectRepository.getApproverProjects(userId).first()
            
            android.util.Log.d("OverallReportsViewModel", "✅ Successfully loaded ${projects.size} approver projects for user: $userId")
            projects.forEach { project ->
                android.util.Log.d("OverallReportsViewModel", "📊 Project: ${project.name} (${project.id})")
            }
            
            projects
        } catch (e: Exception) {
            android.util.Log.e("OverallReportsViewModel", "❌ Error loading approver projects: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun loadOverallReports() {
        // Don't load if user context is not set
        if (currentUserId == null || currentUserRole == null) {
            android.util.Log.w("OverallReportsViewModel", "⚠️ User context not set, cannot load reports")
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                android.util.Log.d("OverallReportsViewModel", "🔄 Loading overall reports for user: $currentUserId (role: $currentUserRole)")
                
                // Load projects based on user role
                allProjects = when (currentUserRole) {
                    "APPROVER" -> {
                        android.util.Log.d("OverallReportsViewModel", "🎯 Loading approver-specific projects...")
                        try {
                            val approverProjects = loadApproverProjects(currentUserId!!)
                            android.util.Log.d("OverallReportsViewModel", "✅ Loaded ${approverProjects.size} approver projects")
                            approverProjects
                        } catch (e: Exception) {
                            android.util.Log.e("OverallReportsViewModel", "❌ Failed to load approver projects: ${e.message}")
                            e.printStackTrace()
                            emptyList()
                        }
                    }
                    "BUSINESS_HEAD" -> {
                        android.util.Log.d("OverallReportsViewModel", "🏭 Loading all projects for production head...")
                        projectRepository.getAllProjects()
                    }
                    else -> {
                        android.util.Log.d("OverallReportsViewModel", "👤 Loading all projects for other roles...")
                        projectRepository.getAllProjects()
                    }
                }
                
                android.util.Log.d("OverallReportsViewModel", "📊 Loaded ${allProjects.size} projects for user: $currentUserId")
                
                if (allProjects.isEmpty()) {
                    android.util.Log.w("OverallReportsViewModel", "⚠️ No projects found for user: $currentUserId")
                    
                    // For approvers, show a more specific message
                    val errorMessage = if (currentUserRole == "APPROVER") {
                        "No projects are currently assigned to you. Please contact your production head."
                    } else {
                        "No projects found for your account"
                    }
                    
                    _error.value = errorMessage
                    _isLoading.value = false
                    return@launch
                }
                
                // Load all approved expenses across user's projects
                allExpenses = loadAllApprovedExpenses()
                android.util.Log.d("OverallReportsViewModel", "💰 Loaded ${allExpenses.size} approved expenses for user: $currentUserId")
                
                // Calculate total budget based on user's projects
                val totalBudget = allProjects.sumOf { it.budget }
                
                // Even if no expenses, show the report with budget info
                if (allExpenses.isEmpty()) {
                    android.util.Log.w("OverallReportsViewModel", "⚠️ No approved expenses found for user: $currentUserId")
                    _reportData.value = OverallReportData(
                        totalSpent = 0.0,
                        totalBudget = totalBudget,
                        budgetUsagePercentage = 0.0,
                        timeRange = "This Year",
                        selectedProject = "All Projects",
                        availableProjects = allProjects,
                        expensesByProject = allProjects.associate { it.name to 0.0 }
                    )
                } else {
                    // Apply filters and update report with expense data
                    applyFiltersAndUpdateReport()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("OverallReportsViewModel", "❌ Failed to load overall reports for user $currentUserId: ${e.message}")
                e.printStackTrace()
                _error.value = "Failed to load overall reports: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateTimeRange(timeRange: String) {
        android.util.Log.d("OverallReportsViewModel", "📅 Updating time range to: $timeRange")
        _selectedTimeRange.value = timeRange
        
        // Reset pagination when time range changes
        viewModelScope.launch {
            _isLoading.value = true
            _currentPage.value = 0
            try {
                applyFiltersAndUpdateReport()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateProject(projectId: String) {
        android.util.Log.d("OverallReportsViewModel", "🎯 Updating project filter to: $projectId")
        _selectedProject.value = projectId
        
        // Force reload data when project changes
        viewModelScope.launch {
            _isLoading.value = true
            _currentPage.value = 0
            try {
                applyFiltersAndUpdateReport()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun applyFiltersAndUpdateReport() {
        viewModelScope.launch {
            try {
                android.util.Log.d("OverallReportsViewModel", "🔄 Applying filters...")
                android.util.Log.d("OverallReportsViewModel", "📅 Time range: ${_selectedTimeRange.value}")
                android.util.Log.d("OverallReportsViewModel", "🎯 Selected project: ${_selectedProject.value}")
                
                // Filter expenses by time range
                val timeFilteredExpenses = filterByTimeRange(allExpenses, _selectedTimeRange.value)
                android.util.Log.d("OverallReportsViewModel", "📊 After time filter: ${timeFilteredExpenses.size} expenses")
                
                // Filter by project
                val projectFilteredExpenses = if (_selectedProject.value == "all") {
                    timeFilteredExpenses
                } else {
                    timeFilteredExpenses.filter { it.projectId == _selectedProject.value }
                }
                android.util.Log.d("OverallReportsViewModel", "🎯 After project filter: ${projectFilteredExpenses.size} expenses")
                
                // Calculate total spent
                val totalSpent = projectFilteredExpenses.sumOf { it.amount }
                android.util.Log.d("OverallReportsViewModel", "💰 Total spent: ₹$totalSpent")
                
                // Calculate total budget based on selected projects
                val totalBudget = if (_selectedProject.value == "all") {
                    allProjects.sumOf { it.budget }
                } else {
                    allProjects.find { it.id == _selectedProject.value }?.budget ?: 0.0
                }
                android.util.Log.d("OverallReportsViewModel", "💼 Total budget: ₹$totalBudget")
                
                // Calculate budget usage percentage
                val budgetUsagePercentage = if (totalBudget > 0) {
                    (totalSpent / totalBudget) * 100
                } else {
                    0.0
                }
                android.util.Log.d("OverallReportsViewModel", "📈 Budget usage: $budgetUsagePercentage%")
                
                // Group by category
                val expensesByCategory = projectFilteredExpenses
                    .filter { it.category.isNotBlank() }
                    .groupBy { it.category }
                    .mapValues { it.value.sumOf { expense -> expense.amount } }
                android.util.Log.d("OverallReportsViewModel", "🏷️ Categories: ${expensesByCategory.keys}")
                android.util.Log.d("OverallReportsViewModel", "📊 Category totals: $expensesByCategory")
                
                // Group by department
                val expensesByDepartment = projectFilteredExpenses
                    .filter { it.department.isNotBlank() }
                    .groupBy { it.department }
                    .mapValues { it.value.sumOf { expense -> expense.amount } }
                android.util.Log.d("OverallReportsViewModel", "🏢 Departments: ${expensesByDepartment.keys}")
                
                // Group by project (only for All Projects view)
                val expensesByProject = if (_selectedProject.value == "all") {
                    val projectExpenses = projectFilteredExpenses.groupBy { it.projectId }
                        .mapValues { it.value.sumOf { expense -> expense.amount } }
                        .mapKeys { entry ->
                            allProjects.find { it.id == entry.key }?.name ?: "Unknown Project"
                        }
                    
                    // Ensure we show all projects even if no expenses
                    val allProjectsWithExpenses = allProjects.associate { project ->
                        project.name to (projectExpenses[project.name] ?: 0.0)
                    }
                    
                    allProjectsWithExpenses
                } else {
                    emptyMap()
                }
                android.util.Log.d("OverallReportsViewModel", "🎯 Projects: ${expensesByProject.keys}")
                
                // Convert to detailed expenses with project names
                filteredExpenses = projectFilteredExpenses.map { expense ->
                    val projectName = allProjects.find { it.id == expense.projectId }?.name ?: "Unknown Project"
                    DetailedExpenseWithProject(
                        id = expense.id,
                        date = expense.getDateAsTimestamp(), // Convert String date to Timestamp
                        invoice = expense.receiptNumber.ifEmpty { "N/A" },
                        by = expense.userName.ifEmpty { expense.submittedBy }, // Use submittedBy if userName is empty
                        amount = expense.amount,
                        department = expense.department,
                        category = expense.category,
                        description = expense.description,
                        modeOfPayment = PaymentMode.toString(expense.modeOfPayment),
                        projectName = projectName,
                        phaseId = expense.phaseId ?: "", // Handle nullable phaseId
                        attachmentUrl = expense.attachmentURL ?: expense.attachmentUrl, // Prefer new field, fallback to old
                        attachmentFileName = expense.attachmentName ?: expense.attachmentFileName, // Prefer new field, fallback to old
                        paymentProofUrl = expense.paymentProofURL ?: expense.paymentProofUrl, // Prefer new field, fallback to old
                        paymentProofFileName = expense.paymentProofName ?: expense.paymentProofFileName // Prefer new field, fallback to old
                    )
                }.sortedByDescending { it.date?.toDate()?.time ?: 0L }
                
                // Reset pagination when data changes
                _currentPage.value = 0
                updatePaginatedExpenses()
                
                android.util.Log.d("OverallReportsViewModel", "📋 Total filtered expenses: ${filteredExpenses.size}")
                
                // Create project filters
                val projectFilters = mutableListOf(
                    ProjectFilter("All Projects", "all")
                ).apply {
                    addAll(allProjects.mapNotNull { project -> 
                        project.id?.let { ProjectFilter(project.name, it) }
                    })
                }
                
                val timeRangeDisplay = FilterOptions.timeRanges.find { it.value == _selectedTimeRange.value }?.displayName ?: "This Year"
                val selectedProjectDisplay = projectFilters.find { it.value == _selectedProject.value }?.name ?: "All Projects"
                
                // Get current page of expenses
                val currentPageExpenses = getCurrentPageExpenses()
                
                // Update state
                _reportData.value = OverallReportData(
                    totalSpent = totalSpent,
                    totalBudget = totalBudget,
                    budgetUsagePercentage = budgetUsagePercentage,
                    expensesByCategory = expensesByCategory,
                    expensesByDepartment = expensesByDepartment,
                    expensesByProject = expensesByProject,
                    detailedExpenses = currentPageExpenses,
                    timeRange = timeRangeDisplay,
                    selectedProject = selectedProjectDisplay,
                    availableProjects = allProjects
                )
                
                android.util.Log.d("OverallReportsViewModel", "✅ Report data updated successfully")
                android.util.Log.d("OverallReportsViewModel", "📊 Final summary: ₹$totalSpent / ₹$totalBudget ($budgetUsagePercentage%)")
                android.util.Log.d("OverallReportsViewModel", "🏷️ Categories found: ${expensesByCategory.size}")
                android.util.Log.d("OverallReportsViewModel", "📋 Expenses in overview: ${filteredExpenses.size}")
                
            } catch (e: Exception) {
                android.util.Log.e("OverallReportsViewModel", "❌ Error applying filters: ${e.message}")
                _error.value = "Failed to process report data: ${e.message}"
            }
        }
    }
    
    private suspend fun loadAllApprovedExpenses(): List<Expense> {
        return try {
            val allExpenses = mutableListOf<Expense>()
            var errorCount = 0
            
            android.util.Log.d("OverallReportsViewModel", "🔄 Loading expenses from ${allProjects.size} projects...")
            
            allProjects.forEach { project ->
                try {
                    val projectId = project.id ?: return@forEach
                    android.util.Log.d("OverallReportsViewModel", "📊 Loading expenses for project: ${project.name} ($projectId)")
                    
                    // First try to get approved expenses
                    val approvedExpenses = expenseRepository.getApprovedExpensesForProject(projectId)
                    android.util.Log.d("OverallReportsViewModel", "💰 Found ${approvedExpenses.size} approved expenses for ${project.name}")
                    
                    if (approvedExpenses.isNotEmpty()) {
                        allExpenses.addAll(approvedExpenses)
                        // Log sample expense for verification
                        approvedExpenses.firstOrNull()?.let { expense ->
                            android.util.Log.d("OverallReportsViewModel", "📝 Sample approved expense from ${project.name}: ${expense.category} - ₹${expense.amount}")
                        }
                    } else {
                        // If no approved expenses, try to get all expenses and filter for reportable ones
                        android.util.Log.d("OverallReportsViewModel", "🔍 No approved expenses found for ${project.name}, checking all expenses...")
                        try {
                            val allProjectExpenses = expenseRepository.getAllExpensesForProject(projectId)
                            android.util.Log.d("OverallReportsViewModel", "📊 Found ${allProjectExpenses.size} total expenses for ${project.name}")
                            
                            // Filter for expenses that should be shown in reports (APPROVED, PENDING, or any with amount > 0)
                            val reportableExpenses = allProjectExpenses.filter { expense ->
                                expense.status == ExpenseStatus.APPROVED || 
                                expense.status == ExpenseStatus.PENDING ||
                                expense.amount > 0
                            }
                            
                            android.util.Log.d("OverallReportsViewModel", "📋 Found ${reportableExpenses.size} reportable expenses for ${project.name}")
                            
                            if (reportableExpenses.isNotEmpty()) {
                                allExpenses.addAll(reportableExpenses)
                                reportableExpenses.forEach { expense ->
                                    android.util.Log.d("OverallReportsViewModel", "📝 Reportable expense: ${expense.id} - Status: ${expense.status} - Category: ${expense.category} - Amount: ₹${expense.amount}")
                                }
                            } else {
                                allProjectExpenses.forEach { expense ->
                                    android.util.Log.d("OverallReportsViewModel", "📝 All expense: ${expense.id} - Status: ${expense.status} - Category: ${expense.category} - Amount: ₹${expense.amount}")
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("OverallReportsViewModel", "❌ Error loading all expenses for ${project.name}: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    errorCount++
                    android.util.Log.e("OverallReportsViewModel", "❌ Error loading expenses for project ${project.name}: ${e.message}")
                    e.printStackTrace()
                    // Continue with other projects
                }
            }
            
            if (errorCount > 0) {
                android.util.Log.w("OverallReportsViewModel", "⚠️ Failed to load expenses for $errorCount projects")
            }
            
            android.util.Log.d("OverallReportsViewModel", "✅ Successfully loaded ${allExpenses.size} total reportable expenses")
            
            // Sort by date descending and return the sorted list
            allExpenses.sortedByDescending { it.submittedAt?.toDate()?.time ?: 0L }
        } catch (e: Exception) {
            android.util.Log.e("OverallReportsViewModel", "❌ Critical error loading expenses: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun filterByTimeRange(expenses: List<Expense>, timeRange: String): List<Expense> {
        val now = Calendar.getInstance()
        val startDate = Calendar.getInstance()
        
        when (timeRange) {
            "this_month" -> {
                startDate.set(Calendar.DAY_OF_MONTH, 1)
                startDate.set(Calendar.HOUR_OF_DAY, 0)
                startDate.set(Calendar.MINUTE, 0)
                startDate.set(Calendar.SECOND, 0)
                startDate.set(Calendar.MILLISECOND, 0)
            }
            "this_year" -> {
                startDate.set(Calendar.DAY_OF_YEAR, 1)
                startDate.set(Calendar.HOUR_OF_DAY, 0)
                startDate.set(Calendar.MINUTE, 0)
                startDate.set(Calendar.SECOND, 0)
                startDate.set(Calendar.MILLISECOND, 0)
            }
            "last_6_months" -> {
                startDate.add(Calendar.MONTH, -6)
            }
            "last_12_months" -> {
                startDate.add(Calendar.MONTH, -12)
            }
            "all_time" -> {
                return expenses
            }
        }
        
        return expenses.filter { expense ->
            // Handle date as String (new format) - convert to Timestamp for comparison
            val expenseDate = expense.getDateAsTimestamp() ?: expense.submittedAt
            expenseDate?.toDate()?.after(startDate.time) == true
        }
    }
    
    fun exportToPDF(onSuccess: (android.content.Intent?) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                android.util.Log.d("OverallReportsViewModel", "🔄 Starting PDF export...")
                android.util.Log.d("OverallReportsViewModel", "📊 Current report data: ${_reportData.value}")
                
                val exportData = ExportData(
                    totalSpent = _reportData.value.totalSpent,
                    timeRange = _reportData.value.timeRange,
                    department = _reportData.value.selectedProject,
                    categoryBreakdown = _reportData.value.expensesByCategory,
                    detailedExpenses = _reportData.value.detailedExpenses.map { detailedExpense ->
                        DetailedExpense(
                            id = detailedExpense.id,
                            date = detailedExpense.date,
                            invoice = detailedExpense.invoice,
                            by = detailedExpense.by,
                            amount = detailedExpense.amount,
                            department = detailedExpense.department,
                            category = detailedExpense.category,
                            description = detailedExpense.description,
                            modeOfPayment = detailedExpense.modeOfPayment,
                            status = ExpenseStatus.APPROVED,
                            phaseId = detailedExpense.phaseId,
                            attachmentUrl = detailedExpense.attachmentUrl,
                            attachmentFileName = detailedExpense.attachmentFileName,
                            paymentProofUrl = detailedExpense.paymentProofUrl,
                            paymentProofFileName = detailedExpense.paymentProofFileName
                        )
                    },
                    generatedAt = Timestamp.now()
                )
                
                android.util.Log.d("OverallReportsViewModel", "📋 Export data prepared: ${exportData.detailedExpenses.size} expenses, ₹${exportData.totalSpent}")
                android.util.Log.d("OverallReportsViewModel", "🏷️ Categories: ${exportData.categoryBreakdown.size}")
                
                val result = professionalExportRepository.exportToPDF(exportData)
                result.fold(
                    onSuccess = { file ->
                        android.util.Log.d("OverallReportsViewModel", "✅ PDF export successful: ${file.absolutePath}")
                        val shareIntent = professionalExportRepository.shareFile(file, "application/pdf")
                        android.util.Log.d("OverallReportsViewModel", "📤 Share intent created: ${shareIntent != null}")
                        onSuccess(shareIntent)
                    },
                    onFailure = { exception ->
                        android.util.Log.e("OverallReportsViewModel", "❌ PDF export failed: ${exception.message}")
                        exception.printStackTrace()
                        onError("Failed to export PDF: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("OverallReportsViewModel", "❌ PDF export exception: ${e.message}")
                e.printStackTrace()
                onError("Failed to export PDF: ${e.message}")
            } finally {
                _isExporting.value = false
            }
        }
    }
    
    fun exportToCSV(onSuccess: (android.content.Intent?) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                android.util.Log.d("OverallReportsViewModel", "🔄 Starting CSV export...")
                android.util.Log.d("OverallReportsViewModel", "📊 Current report data: ${_reportData.value}")
                
                val exportData = ExportData(
                    totalSpent = _reportData.value.totalSpent,
                    timeRange = _reportData.value.timeRange,
                    department = _reportData.value.selectedProject,
                    categoryBreakdown = _reportData.value.expensesByCategory,
                    detailedExpenses = _reportData.value.detailedExpenses.map { detailedExpense ->
                        DetailedExpense(
                            id = detailedExpense.id,
                            date = detailedExpense.date,
                            invoice = detailedExpense.invoice,
                            by = detailedExpense.by,
                            amount = detailedExpense.amount,
                            department = detailedExpense.department,
                            category = detailedExpense.category,
                            description = detailedExpense.description,
                            modeOfPayment = detailedExpense.modeOfPayment,
                            status = ExpenseStatus.APPROVED,
                            phaseId = detailedExpense.phaseId,
                            attachmentUrl = detailedExpense.attachmentUrl,
                            attachmentFileName = detailedExpense.attachmentFileName,
                            paymentProofUrl = detailedExpense.paymentProofUrl,
                            paymentProofFileName = detailedExpense.paymentProofFileName
                        )
                    },
                    generatedAt = Timestamp.now()
                )
                
                android.util.Log.d("OverallReportsViewModel", "📋 Export data prepared: ${exportData.detailedExpenses.size} expenses, ₹${exportData.totalSpent}")
                android.util.Log.d("OverallReportsViewModel", "🏷️ Categories: ${exportData.categoryBreakdown.size}")
                
                val result = professionalExportRepository.exportToCSV(exportData)
                result.fold(
                    onSuccess = { file ->
                        android.util.Log.d("OverallReportsViewModel", "✅ CSV export successful: ${file.absolutePath}")
                        val shareIntent = professionalExportRepository.shareFile(file, "text/csv")
                        android.util.Log.d("OverallReportsViewModel", "📤 Share intent created: ${shareIntent != null}")
                        onSuccess(shareIntent)
                    },
                    onFailure = { exception ->
                        android.util.Log.e("OverallReportsViewModel", "❌ CSV export failed: ${exception.message}")
                        exception.printStackTrace()
                        onError("Failed to export CSV: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("OverallReportsViewModel", "❌ CSV export exception: ${e.message}")
                e.printStackTrace()
                onError("Failed to export CSV: ${e.message}")
            } finally {
                _isExporting.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    // Test export function to verify functionality
    fun testExport(onSuccess: (android.content.Intent?) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                android.util.Log.d("OverallReportsViewModel", "🧪 Testing export functionality...")
                
                val result = exportRepository.testExport()
                result.fold(
                    onSuccess = { file ->
                        android.util.Log.d("OverallReportsViewModel", "✅ Test export successful: ${file.absolutePath}")
                        val shareIntent = professionalExportRepository.shareFile(file, "application/pdf")
                        android.util.Log.d("OverallReportsViewModel", "📤 Test share intent created: ${shareIntent != null}")
                        onSuccess(shareIntent)
                    },
                    onFailure = { exception ->
                        android.util.Log.e("OverallReportsViewModel", "❌ Test export failed: ${exception.message}")
                        exception.printStackTrace()
                        onError("Test export failed: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("OverallReportsViewModel", "❌ Test export exception: ${e.message}")
                e.printStackTrace()
                onError("Test export exception: ${e.message}")
            } finally {
                _isExporting.value = false
            }
        }
    }
    
    fun getCategoryData(): List<CategoryData> {
        val categoryMap = _reportData.value.expensesByCategory
        android.util.Log.d("OverallReportsViewModel", "📊 Getting category data for project: ${_selectedProject.value}")
        android.util.Log.d("OverallReportsViewModel", "📊 Category map: $categoryMap")
        
        return categoryMap.map { (category, amount) ->
            CategoryData(
                category = category,
                amount = amount,
                color = FilterOptions.getCategoryColor(category)
            )
        }.sortedByDescending { it.amount }
    }
    
    fun getCategoryDataWithBudget(): List<CategoryBudgetData> {
        val categoryMap = _reportData.value.expensesByCategory
        val totalBudget = _reportData.value.totalBudget
        val totalSpent = _reportData.value.totalSpent
        
        return categoryMap.map { (category, amount) ->
            // Calculate proportional budget allocation for this category
            val budgetAllocation = if (totalSpent > 0 && totalBudget > 0) {
                (amount / totalSpent) * totalBudget
            } else {
                totalBudget / maxOf(1, categoryMap.size) // Equal distribution if no spending
            }
            
            CategoryBudgetData(
                category = category,
                spent = amount,
                budgetAllocated = budgetAllocation,
                color = FilterOptions.getCategoryColor(category)
            )
        }.sortedByDescending { it.spent }
    }
    
    fun getProjectFilters(): List<ProjectFilter> {
        return listOf(ProjectFilter("All Projects", "all")) + 
               allProjects.mapNotNull { project -> 
                   project.id?.let { ProjectFilter(project.name, it) }
               }
    }
    
    // Method to generate sample data for testing/demonstration
    fun generateSampleData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("OverallReportsViewModel", "🧪 Generating sample data for demonstration...")
                createSampleReportData()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun createSampleReportData() {
        android.util.Log.d("OverallReportsViewModel", "📝 Creating sample report data for demonstration")
        
        val totalBudget = if (allProjects.isNotEmpty()) {
            allProjects.sumOf { it.budget }
        } else {
            1000000.0 // Sample budget of 10 lakhs
        }
        
        // Filter sample data based on selected project
        val isAllProjects = _selectedProject.value == "all"
        val selectedProjectName = if (isAllProjects) "All Projects" else {
            allProjects.find { it.id == _selectedProject.value }?.name ?: "Unknown Project"
        }
        
        // Create sample expense data
        val sampleTotalSpent = if (isAllProjects) 250000.0 else 80000.0 // Adjust for specific project
        val budgetUsagePercentage = (sampleTotalSpent / totalBudget) * 100
        
        // Sample category breakdown (only for specific projects)
        val sampleCategoryExpenses = if (!isAllProjects) {
            val projectCategories = allProjects.find { it.id == _selectedProject.value }?.categories ?: emptyList()
            if (projectCategories.isNotEmpty()) {
                // Use actual data instead of random values
                projectCategories.associateWith { 0.0 } // Initialize with 0, will be populated with real data
            } else {
                emptyMap() // No hardcoded fallback - use empty map if no categories
            }
        } else {
            emptyMap()
        }
        
        // Sample department breakdown - use actual project departments
        val projectDepartments = if (isAllProjects) {
            allProjects.flatMap { it.departmentBudgets?.keys ?: emptySet() }.distinct()
        } else {
            allProjects.find { it.id == _selectedProject.value }?.departmentBudgets?.keys?.toList() ?: emptyList()
        }
        
        val sampleDepartmentExpenses = if (projectDepartments.isNotEmpty()) {
            projectDepartments.associateWith { dept ->
                // Use actual department budget instead of random values
                allProjects.find { it.departmentBudgets?.containsKey(dept) ?: false }?.departmentBudgets?.get(dept) ?: 0.0
            }
        } else {
            emptyMap()
        }
        
        // Sample project breakdown (only for All Projects)
        val sampleProjectExpenses = if (isAllProjects) {
            if (allProjects.isNotEmpty()) {
                allProjects.associate { project ->
                    project.name to (project.spent ?: 0.0)
                }
            } else {
                emptyMap()
            }
        } else {
            emptyMap()
        }
        
        // Generate more sample detailed expenses
        val sampleDetailedExpenses = generateSampleExpenses(selectedProjectName, isAllProjects)
        
        // Store as filtered expenses for pagination
        filteredExpenses = sampleDetailedExpenses
        _currentPage.value = 0
        updatePaginatedExpenses()
        
        _reportData.value = OverallReportData(
            totalSpent = sampleTotalSpent,
            totalBudget = totalBudget,
            budgetUsagePercentage = budgetUsagePercentage,
            expensesByCategory = sampleCategoryExpenses,
            expensesByDepartment = sampleDepartmentExpenses,
            expensesByProject = sampleProjectExpenses,
            detailedExpenses = getCurrentPageExpenses(),
            timeRange = "This Year",
            selectedProject = selectedProjectName,
            availableProjects = allProjects
        )
        
        android.util.Log.d("OverallReportsViewModel", "✅ Sample data created: ₹$sampleTotalSpent / ₹$totalBudget ($budgetUsagePercentage%)")
        android.util.Log.d("OverallReportsViewModel", "📊 Sample categories: ${sampleCategoryExpenses.size}")
        android.util.Log.d("OverallReportsViewModel", "📋 Sample expenses: ${sampleDetailedExpenses.size}")
    }
    
    private fun generateSampleExpenses(projectName: String, isAllProjects: Boolean): List<DetailedExpenseWithProject> {
        val expenses = mutableListOf<DetailedExpenseWithProject>()
        val sampleProjects = if (isAllProjects) {
            allProjects.map { it.name }
        } else {
            listOf(projectName)
        }
        
        // Get departments dynamically from projects
        val departments = allProjects.flatMap { it.departmentBudgets?.keys ?: emptySet() }.distinct()
        val users = listOf("John Doe", "Jane Smith", "Mike Johnson", "Sarah Wilson", "Tom Brown", "Lisa Davis")
        // Get categories dynamically from projects
        val categories = allProjects.flatMap { it.categories ?: emptyList() }.distinct()
        
        // Use actual expense data instead of generating random samples
        // This will be populated by real data from the repository
        return emptyList() // Return empty list - real data will be loaded separately
    }
    
    // Method to force reload without sample data (for testing with real data)
    fun loadRealDataOnly() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                android.util.Log.d("OverallReportsViewModel", "🔄 Loading REAL data only (no samples)...")
                
                // Load all projects first
                allProjects = projectRepository.getAllProjects()
                android.util.Log.d("OverallReportsViewModel", "📊 Loaded ${allProjects.size} projects")
                
                // Load all approved expenses across all projects
                allExpenses = loadAllApprovedExpenses()
                android.util.Log.d("OverallReportsViewModel", "💰 Loaded ${allExpenses.size} approved expenses")
                
                if (allExpenses.isEmpty()) {
                    // Show empty state with real budget info
                    val totalBudget = allProjects.sumOf { it.budget }
                    _reportData.value = OverallReportData(
                        totalSpent = 0.0,
                        totalBudget = totalBudget,
                        budgetUsagePercentage = 0.0,
                        timeRange = "This Year",
                        selectedProject = "All Projects",
                        availableProjects = allProjects
                    )
                } else {
                    // Apply current filters and update report data
                    applyFiltersAndUpdateReport()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("OverallReportsViewModel", "❌ Failed to load real data: ${e.message}")
                _error.value = "Failed to load real data: ${e.message}"
            } finally {
                _isLoading.value = false
                         }
         }
     }
     
     // NEW: Real-time observation method for overall reports
     fun observeAllApprovedExpensesRealtime() {
         viewModelScope.launch {
             _isLoading.value = true
             _error.value = null
             
             try {
                 android.util.Log.d("OverallReportsViewModel", "🔄 Setting up real-time observation for all approved expenses...")
                 
                 // Load all projects first
                 allProjects = projectRepository.getAllProjects()
                 android.util.Log.d("OverallReportsViewModel", "📊 Loaded ${allProjects.size} projects")
                 
                 if (allProjects.isEmpty()) {
                     android.util.Log.w("OverallReportsViewModel", "⚠️ No projects found")
                     _error.value = "No projects found in the system"
                     return@launch
                 }
                 
                 // Set up real-time listeners for all projects
                 setupRealTimeListeners()
                 
             } catch (e: Exception) {
                 android.util.Log.e("OverallReportsViewModel", "❌ Failed to setup real-time observation: ${e.message}")
                 _error.value = "Failed to setup real-time observation: ${e.message}"
             } finally {
                 _isLoading.value = false
             }
         }
     }
     
     private fun setupRealTimeListeners() {
         viewModelScope.launch {
             try {
                 android.util.Log.d("OverallReportsViewModel", "🔥 Setting up real-time listeners for ${allProjects.size} projects...")
                 
                 // Create a map to store all expenses by project
                 val allExpensesMap = mutableMapOf<String, List<Expense>>()
                 
                 // Set up listeners for each project using launch for concurrent collection
                 allProjects.forEach { project ->
                     val projectId = project.id ?: return@forEach
                     launch {
                         expenseRepository.getProjectExpenses(projectId)
                             .collect { projectExpenses ->
                                 // Filter for approved expenses only
                                 val approvedExpenses = projectExpenses.filter { it.status.name == "APPROVED" }
                                 
                                 android.util.Log.d("OverallReportsViewModel", "📊 Project ${project.name}: ${approvedExpenses.size} approved expenses")
                                 
                                 // Update the map
                                 allExpensesMap[projectId] = approvedExpenses
                                 
                                 // Combine all approved expenses
                                 allExpenses = allExpensesMap.values.flatten()
                                     .sortedByDescending { it.submittedAt?.toDate()?.time ?: 0L }
                                 
                                 android.util.Log.d("OverallReportsViewModel", "💰 Total approved expenses across all projects: ${allExpenses.size}")
                                 
                                 // Apply current filters and update report
                                 applyFiltersAndUpdateReport()
                             }
                     }
                 }
                 
             } catch (e: Exception) {
                 android.util.Log.e("OverallReportsViewModel", "❌ Error in real-time listeners: ${e.message}")
                 _error.value = "Failed to setup real-time listeners: ${e.message}"
             }
         }
     }
     
         // Pagination methods
    private fun updatePaginatedExpenses() {
        val totalExpenses = filteredExpenses.size
        val totalPages = (totalExpenses + pageSize - 1) / pageSize // Ceiling division
        _hasMoreExpenses.value = _currentPage.value < totalPages - 1
        
        android.util.Log.d("OverallReportsViewModel", "📄 Pagination update: page ${_currentPage.value + 1}/$totalPages, hasMore: ${_hasMoreExpenses.value}")
    }
    
    private fun getCurrentPageExpenses(): List<DetailedExpenseWithProject> {
        val startIndex = _currentPage.value * pageSize
        val endIndex = kotlin.math.min(startIndex + pageSize, filteredExpenses.size)
        return if (startIndex < filteredExpenses.size) {
            filteredExpenses.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }
    
    fun loadNextPage() {
        if (_hasMoreExpenses.value && !_isLoadingMore.value) {
            viewModelScope.launch {
                _isLoadingMore.value = true
                
                try {
                    _currentPage.value += 1
                    updatePaginatedExpenses()
                    
                    // Update report data with new page
                    val currentPageExpenses = getCurrentPageExpenses()
                    val currentData = _reportData.value
                    _reportData.value = currentData.copy(
                        detailedExpenses = currentData.detailedExpenses + currentPageExpenses
                    )
                    
                    android.util.Log.d("OverallReportsViewModel", "📄 Loaded page ${_currentPage.value + 1}, showing ${currentData.detailedExpenses.size + currentPageExpenses.size} total expenses")
                    
                    // Small delay to show loading state
                    kotlinx.coroutines.delay(300)
                    
                } catch (e: Exception) {
                    android.util.Log.e("OverallReportsViewModel", "❌ Error loading next page: ${e.message}")
                    // Revert page increment on error
                    _currentPage.value -= 1
                    updatePaginatedExpenses()
                } finally {
                    _isLoadingMore.value = false
                }
            }
        }
    }
    
    fun resetPagination() {
        _currentPage.value = 0
        updatePaginatedExpenses()
        
        // Update report data with first page only
        val currentPageExpenses = getCurrentPageExpenses()
        val currentData = _reportData.value
        _reportData.value = currentData.copy(detailedExpenses = currentPageExpenses)
        
        android.util.Log.d("OverallReportsViewModel", "🔄 Pagination reset, showing ${currentPageExpenses.size} expenses")
    }
     
         fun getTotalExpenseCount(): Int = filteredExpenses.size
    fun getCurrentPageNumber(): Int = _currentPage.value + 1
    fun getTotalPages(): Int = (filteredExpenses.size + pageSize - 1) / pageSize
     
     // Method to refresh data manually
     fun refreshData() {
         // Use the main load method which now handles user context
         loadOverallReports()
     }
     
         // Method to load data without real-time observation (for better performance)
    fun loadDataOnce() {
        // Use the main load method which now handles user context
        loadOverallReports()
    }
    
    // Debug method to check current state
    fun getDebugInfo(): String {
        return """
            OverallReportsViewModel Debug Info:
            - Current User ID: $currentUserId
            - Current User Role: $currentUserRole
            - Is Loading: ${_isLoading.value}
            - Error: ${_error.value}
            - Projects Count: ${allProjects.size}
            - Expenses Count: ${allExpenses.size}
            - Report Data Available: ${_reportData.value.availableProjects.isNotEmpty()}
        """.trimIndent()
    }
    
    // Public methods to access repositories for TracuraDashboardReports
    suspend fun getPhasesForProject(projectId: String): List<Phase> {
        return projectRepository.getPhasesForProject(projectId)
    }
    
    suspend fun getApprovedExpensesForProject(projectId: String): List<Expense> {
        return expenseRepository.getApprovedExpensesForProject(projectId)
    }
    
    suspend fun getAllExpensesForProject(projectId: String): List<Expense> {
        return expenseRepository.getAllExpensesForProject(projectId)
    }
}  