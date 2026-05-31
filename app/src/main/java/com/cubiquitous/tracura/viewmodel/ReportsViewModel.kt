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
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository,
    private val exportRepository: ExportRepository,
    private val professionalExportRepository: ProfessionalExportRepository
) : ViewModel() {
    
    private val _reportData = MutableStateFlow(ReportData())
    val reportData: StateFlow<ReportData> = _reportData.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _selectedTimeRange = MutableStateFlow("this_year")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()
    
    private val _selectedDepartment = MutableStateFlow("all")
    val selectedDepartment: StateFlow<String> = _selectedDepartment.asStateFlow()
    
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()
    
    private var currentProjectId: String = ""
    
    fun loadReportsForProject(projectId: String) {
        currentProjectId = projectId
        android.util.Log.d("ReportsViewModel", "🎯 loadReportsForProject called with projectId: $projectId")
        loadReports()
    }
    
    fun updateTimeRange(timeRange: String) {
        _selectedTimeRange.value = timeRange
        loadReports()
    }
    
    fun updateDepartment(department: String) {
        _selectedDepartment.value = department
        loadReports()
    }
    
    private fun loadReports() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                android.util.Log.d("ReportsViewModel", "🔄 Loading reports for project: $currentProjectId")
                
                // Get project data for budget information
                val project = projectRepository.getProjectById(currentProjectId)
                val totalBudget = project?.budget ?: 0.0
                android.util.Log.d("ReportsViewModel", "📊 Project: ${project?.name}, Budget: $totalBudget")
                
                // Get expenses based on filters (all statuses: approved, pending, rejected)
                val expenses = getFilteredExpenses()
                android.util.Log.d("ReportsViewModel", "💰 Found ${expenses.size} total expenses (all statuses)")
                
                // Calculate total spent
                val totalSpent = expenses.sumOf { it.amount }
                android.util.Log.d("ReportsViewModel", "💵 Total spent: $totalSpent")
                
                // Calculate budget usage percentage
                val budgetUsagePercentage = if (totalBudget > 0) {
                    (totalSpent / totalBudget) * 100
                } else {
                    0.0
                }
                android.util.Log.d("ReportsViewModel", "📈 Budget usage: $budgetUsagePercentage%")
                
                // Group by category - use categories list, fallback to single category
                val expensesByCategory = expenses.flatMap { expense ->
                    val categories = if (expense.categories.isNotEmpty()) {
                        expense.categories
                    } else {
                        listOf(expense.category).filter { it.isNotEmpty() }
                    }
                    categories.map { category -> category to expense }
                }.groupBy({ it.first }, { it.second })
                    .mapValues { it.value.sumOf { expense -> expense.amount } }
                android.util.Log.d("ReportsViewModel", "🏷️ Categories: ${expensesByCategory.keys}")
                
                // Group by department
                val expensesByDepartment = expenses.groupBy { it.department }
                    .mapValues { it.value.sumOf { expense -> expense.amount } }
                
                android.util.Log.d("ReportsViewModel", "🏢 Department breakdown:")
                expensesByDepartment.forEach { (dept, amount) ->
                    android.util.Log.d("ReportsViewModel", "   - $dept: ₹$amount")
                }
                
                // Convert to detailed expenses
                val detailedExpenses = expenses.map { expense ->
                    DetailedExpense(
                        id = expense.id,
                        date = expense.getDateAsTimestamp(), // Convert String date to Timestamp for backward compatibility
                        invoice = expense.receiptNumber.ifEmpty { "N/A" },
                        by = expense.userName.ifEmpty { expense.submittedBy }, // Use submittedBy if userName is empty
                        amount = expense.amount,
                        department = expense.department,
                        category = expense.category, // Uses computed property that returns first category
                        description = expense.description,
                        modeOfPayment = PaymentMode.toString(expense.modeOfPayment), // Convert enum to string
                        status = expense.status,
                        phaseId = expense.phaseId ?: "", // Handle nullable phaseId
                        attachmentUrl = expense.attachmentURL ?: expense.attachmentUrl, // Prefer new field, fallback to old
                        attachmentFileName = expense.attachmentName ?: expense.attachmentFileName, // Prefer new field, fallback to old
                        paymentProofUrl = expense.paymentProofURL ?: expense.paymentProofUrl, // Prefer new field, fallback to old
                        paymentProofFileName = expense.paymentProofName ?: expense.paymentProofFileName // Prefer new field, fallback to old
                    )
                }.sortedByDescending { it.date?.toDate()?.time ?: 0L }
                
                android.util.Log.d("ReportsViewModel", "📋 Detailed expenses: ${detailedExpenses.size}")
                
                // Update FilterOptions with project departments
                val projectDepartments = project?.departmentBudgets?.keys?.toList() ?: emptyList()
                FilterOptions.updateDepartments(projectDepartments)
                
                // Update state
                _reportData.value = ReportData(
                    totalSpent = totalSpent,
                    totalBudget = totalBudget,
                    budgetUsagePercentage = budgetUsagePercentage,
                    expensesByCategory = expensesByCategory,
                    expensesByDepartment = expensesByDepartment,
                    detailedExpenses = detailedExpenses,
                    timeRange = FilterOptions.timeRanges.find { it.value == _selectedTimeRange.value }?.displayName ?: "This Year",
                    selectedDepartment = FilterOptions.departments.find { it.value == _selectedDepartment.value }?.name ?: "All Departments"
                )
                
                android.util.Log.d("ReportsViewModel", "✅ Reports loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("ReportsViewModel", "❌ Error loading reports: ${e.message}", e)
                _error.value = "Failed to load reports: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun getFilteredExpenses(): List<Expense> {
        return try {
            android.util.Log.d("ReportsViewModel", "🔍 Getting filtered expenses for project: $currentProjectId")
            
            // Get ALL expenses (not just approved) to support filtering by status
            android.util.Log.d("ReportsViewModel", "🔄 Using getAllExpensesForProject to get all statuses...")
            
            // Get all expenses regardless of status
            val allProjectExpenses = expenseRepository.getAllExpensesForProject(currentProjectId)
            android.util.Log.d("ReportsViewModel", "📦 getAllExpensesForProject returned: ${allProjectExpenses.size} total expenses")
            
            // Log expense details by status for debugging
            val statusCounts = allProjectExpenses.groupBy { it.status }
            statusCounts.forEach { (status, expenses) ->
                android.util.Log.d("ReportsViewModel", "📊 ${status.name} expenses: ${expenses.size}")
                expenses.take(3).forEachIndexed { index, expense ->
                    val categoryDisplay = expense.categories.joinToString(", ").ifEmpty { expense.category }
                    val userNameDisplay = expense.userName.ifEmpty { expense.submittedBy }
                    android.util.Log.d("ReportsViewModel", "💰 ${status.name} Expense $index: $categoryDisplay - ${expense.department} - ₹${expense.amount} by $userNameDisplay")
                }
            }
            
            // Filter by time range
            val timeFilteredExpenses = filterByTimeRange(allProjectExpenses, _selectedTimeRange.value)
            android.util.Log.d("ReportsViewModel", "⏰ After time filter (${_selectedTimeRange.value}): ${timeFilteredExpenses.size}")
            
            // Filter by department
            val departmentFilteredExpenses = if (_selectedDepartment.value == "all") {
                timeFilteredExpenses
            } else {
                timeFilteredExpenses.filter { 
                    it.department.equals(_selectedDepartment.value, ignoreCase = true) 
                }
            }
            android.util.Log.d("ReportsViewModel", "🏢 After department filter (${_selectedDepartment.value}): ${departmentFilteredExpenses.size}")
            
            departmentFilteredExpenses
        } catch (e: Exception) {
            android.util.Log.e("ReportsViewModel", "❌ Error in getFilteredExpenses: ${e.message}", e)
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
            // Handle date as String (new format) or Timestamp (old format)
            val expenseDate = expense.getDateAsTimestamp() ?: expense.submittedAt
            expenseDate?.toDate()?.after(startDate.time) == true
        }
    }
    
    fun exportToPDF(onSuccess: (android.content.Intent?) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val exportData = ExportData(
                    totalSpent = _reportData.value.totalSpent,
                    timeRange = _reportData.value.timeRange,
                    department = _reportData.value.selectedDepartment,
                    categoryBreakdown = _reportData.value.expensesByCategory,
                    detailedExpenses = _reportData.value.detailedExpenses,
                    generatedAt = Timestamp.now()
                )
                
                val result = professionalExportRepository.exportToPDF(exportData)
                result.fold(
                    onSuccess = { file ->
                        val shareIntent = professionalExportRepository.shareFile(file, "application/pdf")
                        onSuccess(shareIntent)
                    },
                    onFailure = { exception ->
                        onError("Failed to export PDF: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
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
                val exportData = ExportData(
                    totalSpent = _reportData.value.totalSpent,
                    timeRange = _reportData.value.timeRange,
                    department = _reportData.value.selectedDepartment,
                    categoryBreakdown = _reportData.value.expensesByCategory,
                    detailedExpenses = _reportData.value.detailedExpenses,
                    generatedAt = Timestamp.now()
                )
                
                val result = professionalExportRepository.exportToCSV(exportData)
                result.fold(
                    onSuccess = { file ->
                        val shareIntent = professionalExportRepository.shareFile(file, "text/csv")
                        onSuccess(shareIntent)
                    },
                    onFailure = { exception ->
                        onError("Failed to export CSV: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                onError("Failed to export CSV: ${e.message}")
            } finally {
                _isExporting.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun getCategoryData(): List<CategoryData> {
        val categoryMap = _reportData.value.expensesByCategory
        return categoryMap.map { (category, amount) ->
            CategoryData(
                category = category,
                amount = amount,
                color = FilterOptions.getCategoryColor(category)
            )
        }.sortedByDescending { it.amount }
    }
} 