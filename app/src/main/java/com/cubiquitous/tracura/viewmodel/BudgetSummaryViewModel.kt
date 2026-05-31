package com.cubiquitous.tracura.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.service.BudgetValidationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetSummaryViewModel @Inject constructor(
    private val budgetValidationService: BudgetValidationService
) : ViewModel() {
    
    private val _budgetSummary = MutableStateFlow<Map<String, BudgetValidationService.DepartmentBudgetSummary>>(emptyMap())
    val budgetSummary: StateFlow<Map<String, BudgetValidationService.DepartmentBudgetSummary>> = _budgetSummary.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun loadBudgetSummary(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("BudgetSummaryViewModel", "🔄 Loading budget summary for project: $projectId")
                
                val summary = budgetValidationService.getProjectBudgetSummary(projectId)
                _budgetSummary.value = summary
                
                Log.d("BudgetSummaryViewModel", "✅ Budget summary loaded: ${summary.keys}")
                
            } catch (e: Exception) {
                Log.e("BudgetSummaryViewModel", "❌ Error loading budget summary: ${e.message}")
                _error.value = "Failed to load budget summary: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshBudgetSummary(projectId: String) {
        loadBudgetSummary(projectId)
    }
    
    fun clearError() {
        _error.value = null
    }
    
    // Computed properties for easy access
    val totalAllocated: StateFlow<Double> = _budgetSummary.map { summary ->
        summary.values.sumOf { it.allocatedBudget }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )
    
    val totalSpent: StateFlow<Double> = _budgetSummary.map { summary ->
        summary.values.sumOf { it.spent }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )
    
    val totalRemaining: StateFlow<Double> = _budgetSummary.map { summary ->
        val allocated = summary.values.sumOf { it.allocatedBudget }
        val spent = summary.values.sumOf { it.spent }
        allocated - spent
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )
    
    val overallUsagePercentage: StateFlow<Double> = _budgetSummary.map { summary ->
        val allocated = summary.values.sumOf { it.allocatedBudget }
        val spent = summary.values.sumOf { it.spent }
        if (allocated > 0) (spent / allocated) * 100 else 0.0
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )
    
    val overBudgetDepartments: StateFlow<List<String>> = _budgetSummary.map { summary ->
        summary.entries
            .filter { it.value.remaining < 0 }
            .map { it.key }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val nearBudgetDepartments: StateFlow<List<String>> = _budgetSummary.map { summary ->
        summary.entries
            .filter { 
                it.value.remaining >= 0 && 
                it.value.percentage >= 80 && 
                it.value.percentage < 100 
            }
            .map { it.key }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val hasAlerts: StateFlow<Boolean> = _budgetSummary.map { summary ->
        summary.values.any { it.remaining < 0 || it.percentage >= 80 }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
}






