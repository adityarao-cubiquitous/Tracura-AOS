package com.cubiquitous.tracura.service

import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.repository.ExpenseRepository
import com.cubiquitous.tracura.repository.ProjectRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetValidationService @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository
) {
    
    data class BudgetValidationResult(
        val isValid: Boolean,
        val departmentBudget: Double,
        val currentSpent: Double,
        val newExpenseAmount: Double,
        val remainingBudget: Double,
        val wouldExceedBudget: Boolean,
        val exceedsHardLimit: Boolean = false, // Hard block at 150%
        val warningMessage: String? = null
    )
    
    /**
     * Validates if a new expense would exceed the department's allocated budget
     * Now checks department budget in the selected phase instead of project level
     */
    suspend fun validateExpenseAgainstBudget(
        projectId: String,
        department: String,
        newExpenseAmount: Double,
        phaseId: String? = null
    ): BudgetValidationResult {
        return try {
            // Get project details
            val project = projectRepository.getProjectById(projectId)
            if (project == null) {
                return BudgetValidationResult(
                    isValid = false,
                    departmentBudget = 0.0,
                    currentSpent = 0.0,
                    newExpenseAmount = newExpenseAmount,
                    remainingBudget = 0.0,
                    wouldExceedBudget = true,
                    warningMessage = "Project not found"
                )
            }
            
            var departmentBudget = 0.0
            
            // If phaseId is provided, check budget in that specific phase from subcollection
            if (!phaseId.isNullOrEmpty()) {
                try {
                    // Find customerId for this project
                    val customerId = projectRepository.findCustomerIdForProject(projectId)
                    if (customerId != null) {
                        // Get departments from subcollection for this phase
                        val departments = projectRepository.getDepartmentsForPhase(customerId, projectId, phaseId)
                        
                        // Find department by name (case-insensitive matching)
                        val matchingDepartment = departments.find { 
                            it.name.trim().equals(department.trim(), ignoreCase = true) 
                        }
                        
                        if (matchingDepartment != null) {
                            // Get budget from department's totalBudget (stored field)
                            val totalBudget = matchingDepartment.totalBudget
                            val remainingAmount = matchingDepartment.remainingAmount
                            
                            android.util.Log.d("BudgetValidationService", "📊 Phase-based budget from subcollection: Phase=$phaseId, Department='${matchingDepartment.name}', Budget=₹$totalBudget, Remaining=₹$remainingAmount")
                            
                            // 150% Hard Limit Check: (Spent + New) > 1.5 * Total
                            // Spent = totalBudget - remainingAmount
                            val currentSpent = totalBudget - remainingAmount
                            val maxHardLimit = totalBudget * 1.5
                            val projectedTotal = currentSpent + newExpenseAmount
                            val exceedsHardLimit = projectedTotal > maxHardLimit
                            
                            // Check if new expense would exceed budget using remainingAmount directly
                            val wouldExceedBudget = newExpenseAmount > remainingAmount
                            
                            val warningMessage = when {
                                exceedsHardLimit -> "the amount crosses 150% of usesgae then allocted budget"
                                wouldExceedBudget -> "Expense amount (₹${String.format("%.2f", newExpenseAmount)}) would exceed remaining budget for $department department. " +
                                    "Remaining budget: ₹${String.format("%.2f", remainingAmount)}"
                                else -> null
                            }
                            
                            return BudgetValidationResult(
                                isValid = !exceedsHardLimit && !wouldExceedBudget,
                                departmentBudget = totalBudget,
                                currentSpent = currentSpent,
                                newExpenseAmount = newExpenseAmount,
                                remainingBudget = remainingAmount,
                                wouldExceedBudget = wouldExceedBudget,
                                exceedsHardLimit = exceedsHardLimit,
                                warningMessage = warningMessage
                            )
                        } else {
                            android.util.Log.w("BudgetValidationService", "⚠️ Department '$department' not found in phase $phaseId subcollection. Available departments: ${departments.map { it.name }}")
                            
                            // Try fallback to phase.departments map (for backward compatibility)
                            val phases = projectRepository.getPhasesForProject(projectId)
                            val selectedPhase = phases.find { it.id == phaseId }
                            if (selectedPhase != null) {
                                // Try exact match first
                                departmentBudget = selectedPhase.departments[department] ?: 0.0
                                
                                // If not found, try case-insensitive match
                                if (departmentBudget <= 0) {
                                    selectedPhase.departments.forEach { (deptName, budget) ->
                                        if (deptName.trim().equals(department.trim(), ignoreCase = true)) {
                                            departmentBudget = budget
                                            android.util.Log.d("BudgetValidationService", "📊 Found budget in phase.departments map (case-insensitive): $deptName = ₹$budget")
                                        }
                                    }
                                }
                                
                                if (departmentBudget > 0) {
                                    android.util.Log.d("BudgetValidationService", "📊 Phase-based budget from phase.departments map: Phase=${selectedPhase.phaseName}, Department=$department, Budget=₹$departmentBudget")
                                }
                            }
                        }
                    } else {
                        android.util.Log.w("BudgetValidationService", "⚠️ Could not find customerId for project: $projectId")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BudgetValidationService", "❌ Error getting departments from subcollection: ${e.message}", e)
                }
            }
            
            // Fallback to project-level department budgets if phase budget is 0 or phaseId not provided
            if (departmentBudget <= 0) {
                // Try exact match first
                departmentBudget = project.departmentBudgets?.get(department) ?: 0.0
                
                // If not found, try case-insensitive match
                if (departmentBudget <= 0 && project.departmentBudgets != null) {
                    project.departmentBudgets.forEach { (deptName, budget) ->
                        if (deptName.trim().equals(department.trim(), ignoreCase = true)) {
                            departmentBudget = budget
                            android.util.Log.d("BudgetValidationService", "📊 Found budget in project.departmentBudgets (case-insensitive): $deptName = ₹$budget")
                        }
                    }
                }
                
                if (departmentBudget > 0) {
                    android.util.Log.d("BudgetValidationService", "📊 Project-level budget check: Department=$department, Budget=₹$departmentBudget")
                }
            }
            
            if (departmentBudget <= 0) {
                return BudgetValidationResult(
                    isValid = false,
                    departmentBudget = departmentBudget,
                    currentSpent = 0.0,
                    newExpenseAmount = newExpenseAmount,
                    remainingBudget = 0.0,
                    wouldExceedBudget = true,
                    warningMessage = "No budget allocated for department: $department"
                )
            }
            
            // Get current spending for this department in the specific phase (if phaseId provided)
            // Optimization: Avoid fetching all expenses. If we reached here (fallback), assume 0 spent.
            // "if remainingAmount not there in firebase then take totalBudget only"
            val currentSpent = 0.0
            
            // Calculate remaining budget
            val remainingBudget = departmentBudget - currentSpent
            
            // 150% Hard Limit Check for fallback path
            val maxHardLimit = departmentBudget * 1.5
            val projectedTotal = currentSpent + newExpenseAmount
            val exceedsHardLimit = projectedTotal > maxHardLimit
            
            // Check if new expense would exceed budget
            val wouldExceedBudget = newExpenseAmount > remainingBudget
            
            val warningMessage = when {
                exceedsHardLimit -> "the amount crosses 150% of usesgae then allocted budget"
                wouldExceedBudget -> "Expense amount (₹${String.format("%.2f", newExpenseAmount)}) would exceed remaining budget for $department department. " +
                    "Remaining budget: ₹${String.format("%.2f", remainingBudget)}"
                else -> null
            }
            
            BudgetValidationResult(
                isValid = !exceedsHardLimit && !wouldExceedBudget,
                departmentBudget = departmentBudget,
                currentSpent = currentSpent,
                newExpenseAmount = newExpenseAmount,
                remainingBudget = remainingBudget,
                wouldExceedBudget = wouldExceedBudget,
                exceedsHardLimit = exceedsHardLimit,
                warningMessage = warningMessage
            )
            
        } catch (e: Exception) {
            android.util.Log.e("BudgetValidationService", "Error validating budget: ${e.message}", e)
            BudgetValidationResult(
                isValid = false,
                departmentBudget = 0.0,
                currentSpent = 0.0,
                newExpenseAmount = newExpenseAmount,
                remainingBudget = 0.0,
                wouldExceedBudget = true,
                warningMessage = "Error validating budget: ${e.message}"
            )
        }
    }
    
    /**
     * Gets the current total spending for a specific department in a project
     */
    private suspend fun getCurrentDepartmentSpending(projectId: String, department: String): Double {
        return try {
            val expenses = expenseRepository.getExpensesByProject(projectId).first()
            expenses
                .filter { it.department == department && it.status == com.cubiquitous.tracura.model.ExpenseStatus.APPROVED }
                .sumOf { it.amount }
        } catch (e: Exception) {
            android.util.Log.e("BudgetValidationService", "Error getting department spending: ${e.message}", e)
            0.0
        }
    }
    
    /**
     * Gets the current total spending for a specific department in a specific phase
     */
    private suspend fun getCurrentDepartmentSpendingForPhase(projectId: String, department: String, phaseId: String): Double {
        return try {
            val expenses = expenseRepository.getExpensesByProject(projectId).first()
            expenses
                .filter { 
                    it.department == department && 
                    it.phaseId == phaseId && 
                    it.status == com.cubiquitous.tracura.model.ExpenseStatus.APPROVED 
                }
                .sumOf { it.amount }
        } catch (e: Exception) {
            android.util.Log.e("BudgetValidationService", "Error getting department spending for phase: ${e.message}", e)
            0.0
        }
    }
    
    /**
     * Gets budget summary for all departments in a project
     */
    suspend fun getProjectBudgetSummary(projectId: String): Map<String, DepartmentBudgetSummary> {
        return try {
            val project = projectRepository.getProjectById(projectId)
            if (project == null) return emptyMap()
            
            val expenses = expenseRepository.getExpensesByProject(projectId).first()
            val approvedExpenses = expenses.filter { it.status == com.cubiquitous.tracura.model.ExpenseStatus.APPROVED }
            
            project.departmentBudgets?.mapValues { (department, allocatedBudget) ->
                val spent = approvedExpenses
                    .filter { it.department == department }
                    .sumOf { it.amount }
                
                DepartmentBudgetSummary(
                    department = department,
                    allocatedBudget = allocatedBudget,
                    spent = spent,
                    remaining = allocatedBudget - spent,
                    percentage = if (allocatedBudget > 0) (spent / allocatedBudget) * 100 else 0.0
                )
            } ?: emptyMap()
        } catch (e: Exception) {
            android.util.Log.e("BudgetValidationService", "Error getting budget summary: ${e.message}", e)
            emptyMap()
        }
    }
    
    /**
     * Checks if project has any budget allocated
     */
    suspend fun hasProjectBudget(projectId: String): Boolean {
        return try {
            val project = projectRepository.getProjectById(projectId)
            project?.departmentBudgets?.isNotEmpty() == true
        } catch (e: Exception) {
            android.util.Log.e("BudgetValidationService", "Error checking project budget: ${e.message}", e)
            false
        }
    }
    
    data class DepartmentBudgetSummary(
        val department: String,
        val allocatedBudget: Double,
        val spent: Double,
        val remaining: Double,
        val percentage: Double
    )
}






