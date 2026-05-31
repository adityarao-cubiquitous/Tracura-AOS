package com.cubiquitous.tracura.model

import com.google.firebase.Timestamp

data class ReportData(
    val totalSpent: Double = 0.0,
    val totalBudget: Double = 0.0,
    val budgetUsagePercentage: Double = 0.0,
    val expensesByCategory: Map<String, Double> = emptyMap(),
    val expensesByDepartment: Map<String, Double> = emptyMap(),
    val detailedExpenses: List<DetailedExpense> = emptyList(),
    val timeRange: String = "This Year",
    val selectedDepartment: String = "All Departments"
)

data class DetailedExpense(
    val id: String = "",
    val date: Timestamp? = null,
    val invoice: String = "",
    val by: String = "",
    val amount: Double = 0.0,
    val remainingBalance: Double? = null,
    val department: String = "",
    val category: String = "",
    val description: String = "",
    val modeOfPayment: String = "",
    val status: ExpenseStatus = ExpenseStatus.PENDING,
    val phaseId: String = "",
    val attachmentUrl: String = "",
    val attachmentFileName: String = "",
    val paymentProofUrl: String = "",
    val paymentProofFileName: String = ""
)

data class CategoryData(
    val category: String,
    val amount: Double,
    val color: Long
)

data class CategoryBudgetData(
    val category: String,
    val spent: Double,
    val budgetAllocated: Double,
    val color: Long
)

data class DepartmentFilter(
    val name: String,
    val value: String
)

data class TimeRangeFilter(
    val displayName: String,
    val value: String
)

data class ExportData(
    val totalSpent: Double,
    val timeRange: String,
    val department: String,
    val categoryBreakdown: Map<String, Double>,
    val detailedExpenses: List<DetailedExpense>,
    val generatedAt: Timestamp
)

// Filter options
object FilterOptions {
    val timeRanges = listOf(
        TimeRangeFilter("This Month", "this_month"),
        TimeRangeFilter("This Year", "this_year"),
        TimeRangeFilter("Last 6 Months", "last_6_months"),
        TimeRangeFilter("Last 12 Months", "last_12_months"),
        TimeRangeFilter("All Time", "all_time")
    )
    
    // This will be populated dynamically based on project departments
    val departments = mutableListOf<DepartmentFilter>()
    
    fun updateDepartments(projectDepartments: List<String>) {
        departments.clear()
        departments.add(DepartmentFilter("All Departments", "all"))
        departments.addAll(projectDepartments.map { DepartmentFilter(it, it) })
    }
    
    // Dynamic category colors - will be generated as needed
    private val categoryColors = mutableMapOf<String, Long>()
    private val categoryColorPalette = listOf(
        0xFF4285F4, 0xFF34A853, 0xFFFBBC05, 0xFFEA4335, 0xFF9C27B0,
        0xFF607D8B, 0xFF3F51B5, 0xFFFF9800, 0xFF4CAF50, 0xFF2196F3,
        0xFFFF5722, 0xFF795548, 0xFF009688, 0xFFE91E63, 0xFF673AB7
    )
    
    fun getCategoryColor(category: String): Long {
        return categoryColors.getOrPut(category) {
            val index = categoryColors.size % categoryColorPalette.size
            categoryColorPalette[index]
        }
    }
    
    // Dynamic department colors - will be generated as needed
    private val departmentColors = mutableMapOf<String, Long>()
    private val colorPalette = listOf(
        0xFF4CAF50, 0xFF2196F3, 0xFFFF5722, 0xFF795548, 0xFF009688, 
        0xFFE91E63, 0xFF673AB7, 0xFF795548, 0xFF3F51B5, 0xFF607D8B,
        0xFF9C27B0, 0xFFFF9800, 0xFF4285F4, 0xFF34A853, 0xFFFBBC05
    )
    
    fun getDepartmentColor(department: String): Long {
        return departmentColors.getOrPut(department) {
            val index = departmentColors.size % colorPalette.size
            colorPalette[index]
        }
    }
} 