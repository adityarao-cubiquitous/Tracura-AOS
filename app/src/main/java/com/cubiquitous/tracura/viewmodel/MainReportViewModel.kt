package com.cubiquitous.tracura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.*
import com.cubiquitous.tracura.repository.ExpenseRepository
import com.cubiquitous.tracura.repository.ProjectRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class MainReportData(
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalRemaining: Double = 0.0,
    val projects: List<Project> = emptyList(),
    val phases: Map<String, List<Phase>> = emptyMap(), // projectId -> phases
    val departments: List<String> = emptyList(),
    val monthlyCostTrend: Map<String, List<Double>> = emptyMap(), // projectId -> monthly costs
    val stageBudgetData: Map<String, Map<String, Pair<Double, Double>>> = emptyMap(), // projectId -> stage -> (budget, actual)
    val projectBudgetData: Map<String, Pair<Double, Double>> = emptyMap(), // projectId -> (budget, actual)
    val expenses: List<Expense> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MainReportViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {
    
    private val _reportData = MutableStateFlow(MainReportData())
    val reportData: StateFlow<MainReportData> = _reportData.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var allProjects: List<Project> = emptyList()
    private var allExpenses: List<Expense> = emptyList()
    private var allPhases: Map<String, List<Phase>> = emptyMap()
    
    init {
        loadData()
    }
    
    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Load all projects
                allProjects = projectRepository.getAllProjects()
                android.util.Log.d("MainReportViewModel", "📊 Loaded ${allProjects.size} projects")
                
                // Load phases for each project
                allPhases = allProjects
                    .mapNotNull { project -> project.id?.let { it to project } }
                    .associate { (projectId, project) ->
                        val phases = projectRepository.getPhasesForProject(projectId)
                        projectId to phases
                    }
                android.util.Log.d("MainReportViewModel", "📋 Loaded phases for ${allPhases.size} projects")
                
                // Load all approved expenses
                allExpensesUnfiltered = loadAllApprovedExpenses()
                allExpenses = allExpensesUnfiltered
                android.util.Log.d("MainReportViewModel", "💰 Loaded ${allExpenses.size} approved expenses")
                
                // Calculate report data
                calculateReportData()
                
            } catch (e: Exception) {
                android.util.Log.e("MainReportViewModel", "❌ Error loading data: ${e.message}")
                _error.value = "Failed to load data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private var allExpensesUnfiltered: List<Expense> = emptyList()
    
    fun filterByDateRange(startDate: Date?, endDate: Date?) {
        viewModelScope.launch {
            try {
                // Reload all expenses if not already loaded
                if (allExpensesUnfiltered.isEmpty()) {
                    allExpensesUnfiltered = loadAllApprovedExpenses()
                }
                
                val filteredExpenses = if (startDate != null && endDate != null) {
                    // Include expenses on the boundary dates
                    val startCal = Calendar.getInstance().apply { time = startDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                    val endCal = Calendar.getInstance().apply { time = endDate; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }
                    
                    allExpensesUnfiltered.filter { expense ->
                        val expenseDate = expense.getDateAsTimestamp()?.toDate() ?: expense.submittedAt?.toDate()
                        if (expenseDate != null) {
                            val expenseCal = Calendar.getInstance().apply { time = expenseDate }
                            !expenseCal.before(startCal) && !expenseCal.after(endCal)
                        } else {
                            false
                        }
                    }
                } else {
                    allExpensesUnfiltered
                }
                
                allExpenses = filteredExpenses
                calculateReportData()
            } catch (e: Exception) {
                android.util.Log.e("MainReportViewModel", "❌ Error filtering by date range: ${e.message}")
                _error.value = "Failed to filter data: ${e.message}"
            }
        }
    }
    
    fun filterByProject(projectId: String) {
        // Project filtering is handled in calculateReportData based on selectedProject
        // This method is kept for API compatibility but actual filtering happens in UI
        calculateReportData()
    }
    
    fun filterByProjectStatus(status: String) {
        viewModelScope.launch {
            try {
                val filteredProjects = if (status == "all") {
                    projectRepository.getAllProjects()
                } else {
                    projectRepository.getAllProjects().filter { 
                        it.status.equals(status, ignoreCase = true) 
                    }
                }
                
                allProjects = filteredProjects
                calculateReportData()
            } catch (e: Exception) {
                android.util.Log.e("MainReportViewModel", "❌ Error filtering by status: ${e.message}")
            }
        }
    }
    
    private suspend fun loadAllApprovedExpenses(): List<Expense> {
        return try {
            val allExpenses = mutableListOf<Expense>()
            
            allProjects.forEach { project ->
                try {
                    val projectId = project.id ?: return@forEach
                    // Use getAllExpensesForProject and filter for approved
                    val allProjectExpenses = expenseRepository.getAllExpensesForProject(projectId)
                    val approvedExpenses = allProjectExpenses.filter { 
                        it.status == ExpenseStatus.APPROVED 
                    }
                    allExpenses.addAll(approvedExpenses)
                } catch (e: Exception) {
                    android.util.Log.e("MainReportViewModel", "❌ Error loading expenses for ${project.name}: ${e.message}")
                }
            }
            
            allExpenses.sortedByDescending { it.submittedAt?.toDate()?.time ?: 0L }
        } catch (e: Exception) {
            android.util.Log.e("MainReportViewModel", "❌ Error loading all expenses: ${e.message}")
            emptyList()
        }
    }
    
    private fun calculateReportData() {
        viewModelScope.launch {
            try {
                // Calculate total budget and spent
                val totalBudget = allProjects.sumOf { it.budget }
                val totalSpent = allExpenses.sumOf { it.amount }
                val totalRemaining = maxOf(0.0, totalBudget - totalSpent)
                
                // Get all unique departments from projects and phases
                val departments = mutableSetOf<String>()
                allProjects.forEach { project ->
                    project.departmentBudgets?.keys?.let { departments.addAll(it) }
                }
                allPhases.values.flatten().forEach { phase ->
                    departments.addAll(phase.departments.keys)
                }
                
                // Calculate monthly cost trend (last 6 months)
                val monthlyCostTrend = calculateMonthlyCostTrend()
                
                // Calculate stage budget vs actual
                val stageBudgetData = calculateStageBudgetData()
                
                // Calculate project-wise budget vs actual
                val projectBudgetData = calculateProjectBudgetData()
                
                _reportData.value = MainReportData(
                    totalBudget = totalBudget,
                    totalSpent = totalSpent,
                    totalRemaining = totalRemaining,
                    projects = allProjects,
                    phases = allPhases,
                    departments = departments.toList().sorted(),
                    monthlyCostTrend = monthlyCostTrend,
                    stageBudgetData = stageBudgetData,
                    projectBudgetData = projectBudgetData,
                    expenses = allExpenses,
                    isLoading = _isLoading.value,
                    error = _error.value
                )
                
            } catch (e: Exception) {
                android.util.Log.e("MainReportViewModel", "❌ Error calculating report data: ${e.message}")
                _error.value = "Failed to calculate report data: ${e.message}"
            }
        }
    }
    
    private fun calculateMonthlyCostTrend(): Map<String, List<Double>> {
        val calendar = Calendar.getInstance()
        val now = calendar.time
        
        // Get last 6 months
        val months = mutableListOf<String>()
        val monthData = mutableMapOf<String, MutableList<Double>>()
        
        for (i in 5 downTo 0) {
            calendar.time = now
            calendar.add(Calendar.MONTH, -i)
            val monthName = SimpleDateFormat("MMM", Locale.getDefault()).format(calendar.time)
            months.add(monthName)
        }
        
        // Initialize month data for all projects
        allProjects.forEach { project ->
            val projectId = project.id ?: return@forEach
            monthData[projectId] = MutableList(6) { 0.0 }
            monthData["all"] = MutableList(6) { 0.0 }
        }
        
        // Group expenses by month and project
        allExpenses.forEach { expense ->
            val expenseDate = expense.getDateAsTimestamp()?.toDate() ?: expense.submittedAt?.toDate() ?: return@forEach
            val expenseCalendar = Calendar.getInstance()
            expenseCalendar.time = expenseDate
            
            // Calculate which month index this expense belongs to
            var monthsAgo = -1
            for (i in 0 until 6) {
                val checkCalendar = Calendar.getInstance()
                checkCalendar.time = now
                checkCalendar.add(Calendar.MONTH, -i)
                if (expenseCalendar.get(Calendar.YEAR) == checkCalendar.get(Calendar.YEAR) &&
                    expenseCalendar.get(Calendar.MONTH) == checkCalendar.get(Calendar.MONTH)) {
                    monthsAgo = 5 - i // Reverse index (0 = 5 months ago, 5 = current month)
                    break
                }
            }
            
            if (monthsAgo in 0 until 6) {
                val projectId = expense.projectId
                monthData[projectId]?.let { 
                    it[monthsAgo] = it[monthsAgo] + expense.amount
                }
                monthData["all"]?.let {
                    it[monthsAgo] = it[monthsAgo] + expense.amount
                }
            }
        }
        
        // Convert to Cr (divide by 1 crore = 10,000,000)
        return monthData.mapValues { (_, values) ->
            values.map { it / 10000000.0 }
        }
    }
    
    private fun calculateStageBudgetData(): Map<String, Map<String, Pair<Double, Double>>> {
        val stageData = mutableMapOf<String, MutableMap<String, Pair<Double, Double>>>()
        
        allProjects.forEach { project ->
            val projectId = project.id ?: return@forEach
            val phases = allPhases[projectId] ?: emptyList()
            
            val projectStageData = mutableMapOf<String, Pair<Double, Double>>()
            
            phases.forEach { phase ->
                // Budget from phase departments
                val budget = phase.departments.values.sum() / 10000000.0 // Convert to Cr
                
                // Actual from expenses in this phase
                val actual = allExpenses
                    .filter { it.projectId == projectId && it.phaseId == phase.id }
                    .sumOf { it.amount } / 10000000.0 // Convert to Cr
                
                projectStageData[phase.phaseName] = Pair(budget, actual)
            }
            
            stageData[projectId] = projectStageData
        }
        
        // Add "all" projects combined
        val allStagesData = mutableMapOf<String, Pair<Double, Double>>()
        stageData.values.forEach { projectStages ->
            projectStages.forEach { (stage, data) ->
                val existing = allStagesData[stage] ?: Pair(0.0, 0.0)
                allStagesData[stage] = Pair(existing.first + data.first, existing.second + data.second)
            }
        }
        stageData["all"] = allStagesData
        
        return stageData
    }
    
    private fun calculateProjectBudgetData(): Map<String, Pair<Double, Double>> {
        val projectData = mutableMapOf<String, Pair<Double, Double>>()
        
        allProjects.forEach { project ->
            val projectId = project.id ?: return@forEach
            
            // Budget from project
            val budget = project.budget / 10000000.0 // Convert to Cr
            
            // Actual from expenses in this project
            val actual = allExpenses
                .filter { it.projectId == projectId }
                .sumOf { it.amount } / 10000000.0 // Convert to Cr
            
            projectData[projectId] = Pair(budget, actual)
        }
        
        return projectData
    }
    
    fun getMonths(): List<String> {
        val calendar = Calendar.getInstance()
        val months = mutableListOf<String>()
        for (i in 5 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.MONTH, -i)
            months.add(SimpleDateFormat("MMM", Locale.getDefault()).format(calendar.time))
        }
        return months
    }
    
    fun refresh() {
        loadData()
    }
}

