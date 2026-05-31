package com.cubiquitous.tracura.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.PaymentHistory
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.repository.ExpenseRepository
import com.cubiquitous.tracura.repository.ProjectRepository
import com.cubiquitous.tracura.repository.NotificationRepository
import com.cubiquitous.tracura.repository.ChatRepository
import com.cubiquitous.tracura.service.NotificationService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ApprovalSummary(
    val totalPendingCount: Int = 0,
    val totalPendingAmount: Double = 0.0,
    val recentSubmissions: List<Expense> = emptyList()
)

/** Holds live department budget data fetched from Firestore. */
data class DeptBudgetState(
    val totalBudget: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class ApprovalViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationService: NotificationService,
    private val chatRepository: ChatRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    
    private val _pendingExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val pendingExpenses: StateFlow<List<Expense>> = _pendingExpenses.asStateFlow()
    
    // Function to set selected expense from pending expenses by ID
    fun setSelectedExpenseById(expenseId: String) {
        val expense = _pendingExpenses.value.find { it.id == expenseId }
        _selectedExpense.value = expense
        if (expense == null) {
            _error.value = "Expense not found in pending approvals"
        }
    }
    
    private val _approvalSummary = MutableStateFlow(ApprovalSummary())
    val approvalSummary: StateFlow<ApprovalSummary> = _approvalSummary.asStateFlow()
    
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    // Add selectedExpense state for ReviewExpenseScreen
    private val _selectedExpense = MutableStateFlow<Expense?>(null)
    val selectedExpense: StateFlow<Expense?> = _selectedExpense.asStateFlow()

    private val _paymentHistory = MutableStateFlow<List<PaymentHistory>>(emptyList())
    val paymentHistory: StateFlow<List<PaymentHistory>> = _paymentHistory.asStateFlow()

    // Live department budget state (updated via Firestore snapshot listener)
    private val _deptBudgetState = MutableStateFlow(DeptBudgetState())
    val deptBudgetState: StateFlow<DeptBudgetState> = _deptBudgetState.asStateFlow()

    private var _deptBudgetListener: ListenerRegistration? = null

    /**
     * Starts a real-time Firestore listener on the department document matching
     * [departmentName] inside projects/{projectId}/phases/{phaseId}/departments.
     * Updates [deptBudgetState] whenever the document changes.
     * Call again with new params to reattach (old listener is removed first).
     */
    fun startDepartmentBudgetListener(projectId: String, phaseId: String, departmentName: String) {
        // Remove previous listener first
        _deptBudgetListener?.remove()
        _deptBudgetListener = null
        _deptBudgetState.value = DeptBudgetState(isLoading = true)

        if (projectId.isBlank() || phaseId.isBlank() || departmentName.isBlank()) {
            _deptBudgetState.value = DeptBudgetState(isLoading = false)
            return
        }

        viewModelScope.launch {
            try {
                val customerId = projectRepository.findCustomerIdForProject(projectId)
                if (customerId == null) {
                    Log.w("ApprovalViewModel", "⚠️ Could not find customerId for project $projectId")
                    _deptBudgetState.value = DeptBudgetState(isLoading = false)
                    return@launch
                }

                val query = firestore
                    .collection("customers").document(customerId)
                    .collection("projects").document(projectId)
                    .collection("phases").document(phaseId)
                    .collection("departments")
                    .whereEqualTo("name", departmentName)

                _deptBudgetListener = query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ApprovalViewModel", "❌ Department budget listener error: ${error.message}")
                        _deptBudgetState.value = DeptBudgetState(totalBudget = 0.0, remainingAmount = 0.0, isLoading = false)
                        return@addSnapshotListener
                    }
                    val doc = snapshot?.documents?.firstOrNull()
                    if (doc != null && doc.exists()) {
                        val total = (doc.get("totalBudget") as? Number)?.toDouble() ?: 0.0
                        val remaining = (doc.get("remainingAmount") as? Number)?.toDouble() ?: 0.0
                        Log.d("ApprovalViewModel", "✅ Live dept budget: total=$total, remaining=$remaining")
                        _deptBudgetState.value = DeptBudgetState(totalBudget = total, remainingAmount = remaining, isLoading = false)
                    } else {
                        Log.w("ApprovalViewModel", "⚠️ Department doc not found for name=$departmentName")
                        _deptBudgetState.value = DeptBudgetState(totalBudget = 0.0, remainingAmount = 0.0, isLoading = false)
                    }
                }
            } catch (e: Exception) {
                Log.e("ApprovalViewModel", "❌ Error setting up department budget listener: ${e.message}")
                _deptBudgetState.value = DeptBudgetState(totalBudget = 0.0, remainingAmount = 0.0, isLoading = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _deptBudgetListener?.remove()
    }

    fun fetchPaymentHistory(projectId: String, expenseId: String) {
        viewModelScope.launch {
            try {
                _paymentHistory.value = expenseRepository.getPaymentHistory(projectId, expenseId)
            } catch (e: Exception) {
                Log.e("ApprovalViewModel", "fetchPaymentHistory failed: ${e.message}")
            }
        }
    }
    
    // Track the current context - whether we're viewing project-specific or all pending approvals
    private val _currentProjectId = MutableStateFlow<String?>(null)
    val currentProjectId: StateFlow<String?> = _currentProjectId.asStateFlow()
    
    // Track the current user role for use in loadPendingApprovals
    private val _currentUserRole = MutableStateFlow<String?>(null)
    val currentUserRole: StateFlow<String?> = _currentUserRole.asStateFlow()


    
    fun loadPendingApprovals(forceRefresh: Boolean = false, userRole: UserRole) {
        viewModelScope.launch {
            // If data is already available and refresh is not forced, skip reloading
            if (!forceRefresh && _pendingExpenses.value.isNotEmpty()) {
                _currentProjectId.value = null
                return@launch
            }


            _isLoading.value = true
            _error.value = null
            _currentProjectId.value = null // Reset to indicate we're viewing all approvals
            
            try {
                val directExpenses = expenseRepository.getPendingExpensesDirectly(userRole = userRole)
                _pendingExpenses.value = directExpenses
                
                val totalAmount = directExpenses.sumOf { it.amount }
                val recentSubmissions = directExpenses.sortedByDescending { 
                    it.submittedAt?.toDate()?.time ?: 0L 
                }.take(5)
                
                _approvalSummary.value = ApprovalSummary(
                    totalPendingCount = directExpenses.size,
                    totalPendingAmount = totalAmount,
                    recentSubmissions = recentSubmissions
                )
                
            } catch (e: Exception) {
                _error.value = "Failed to load pending approvals: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadPendingApprovalsForProject(projectId: String, forceRefresh: Boolean = false, userRole: UserRole) {
        viewModelScope.launch {
            // If we already have data for this project and refresh is not forced, skip reloading
            if (!forceRefresh &&
                _currentProjectId.value == projectId &&
                _pendingExpenses.value.isNotEmpty()
            ) {
                return@launch
            }

            _isLoading.value = true
            _error.value = null
            _currentProjectId.value = projectId // Track that we're viewing project-specific approvals
            Log.d("CurentUserRole", "current user role in first loadinf approval is $userRole")
            try {
                val directExpenses = expenseRepository.getPendingExpensesDirectly(userRole = userRole)
                    .filter { it.projectId == projectId }
                
                _pendingExpenses.value = directExpenses
                
                val totalAmount = directExpenses.sumOf { it.amount }
                val recentSubmissions = directExpenses.sortedByDescending { 
                    it.submittedAt?.toDate()?.time ?: 0L 
                }.take(5)
                
                _approvalSummary.value = ApprovalSummary(
                    totalPendingCount = directExpenses.size,
                    totalPendingAmount = totalAmount,
                    recentSubmissions = recentSubmissions
                )
                
            } catch (e: Exception) {
                _error.value = "Failed to load project details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    
    // Function to fetch individual expense by ID for ReviewExpenseScreen
    fun fetchExpenseById(expenseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("FetchExpenseById", "🔍 Fetching expense from Firebase: $expenseId")
                val expense = expenseRepository.getExpenseById(expenseId)
                Log.d("FetchExpenseById", "✅ Fetched expense: id=${expense?.id}, status=${expense?.status}, amount=${expense?.amount}")
                _selectedExpense.value = expense
                
                if (expense == null) {
                    _error.value = "Expense not found"
                    Log.e("FetchExpenseById", "❌ Expense not found in Firebase: $expenseId")
                } else {
                    Log.d("FetchExpenseById", "📝 Full expense details: $expense")
                }
                
            } catch (e: Exception) {
                Log.e("FetchExpenseById", "❌ Error fetching expense: ${e.message}", e)
                _error.value = "Failed to load expense: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun approveExpense(expense: Expense, verifiedBy: String, comments: String, userRole: String? = null, byCredit: Boolean) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            if (userRole != null) {
                _currentUserRole.value = userRole
            }
            
            try {
                expenseRepository.updateExpenseStatus(
                    projectId = expense.projectId,
                    expenseId = expense.id,
                    status = com.cubiquitous.tracura.model.ExpenseStatus.APPROVED,
                    reviewComments = comments,
                    isVerified = true,
                    verifiedBy = verifiedBy,
                    byCredit = byCredit,
                    verifiedAt = com.google.firebase.Timestamp.now()
                )
                
                // Refresh the list after approval
                delay(500) // Give Firebase time to process update
                refreshPendingExpenses()
                
            } catch (e: Exception) {
                _error.value = "Failed to approve expense: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    fun rejectExpense(expense: Expense, reviewerName: String, comments: String, userRole: String? = null, byCredit: Boolean) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            
            // Store userRole for use in refreshPendingExpenses
            if (userRole != null) {
                _currentUserRole.value = userRole
            }
            
            try {
                expenseRepository.updateExpenseStatus(
                    projectId = expense.projectId,
                    expenseId = expense.id,
                    status = com.cubiquitous.tracura.model.ExpenseStatus.REJECTED,
                    reviewedBy = reviewerName,
                    reviewComments = comments,
                    isVerified = expense.isVerified,
                    verifiedBy = expense.verifiedBy,
                    byCredit = byCredit,
                    verifiedAt = expense.verifiedAt ?: com.google.firebase.Timestamp.now()
                )
                
                // Send notification to expense submitter
                sendExpenseStatusNotification(expense, false, reviewerName)
                
                // Refresh the list after rejection
                delay(500) // Give Firebase time to process update
                refreshPendingExpenses()
                
            } catch (e: Exception) {
                Log.e("ApprovalViewModel", "❌ Error rejecting expense: ${e.message}")
                _error.value = "Failed to reject expense: ${e.message}"
                e.printStackTrace()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun approveSelectedExpenses(expenseIds: List<String>, reviewerName: String, comments: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null

            try {
                val currentExpenses = _pendingExpenses.value
                var failCount = 0

                for (expenseId in expenseIds) {
                    try {
                        val expense = currentExpenses.find { it.id == expenseId }
                        if (expense != null) {
                            expenseRepository.updateExpenseStatus(
                                projectId = expense.projectId,
                                expenseId = expenseId,
                                status = com.cubiquitous.tracura.model.ExpenseStatus.APPROVED,
                                reviewComments = comments,
                                isVerified = true,
                                verifiedBy = reviewerName,
                                byCredit = expense.byCredit ?: false,
                                verifiedAt = com.google.firebase.Timestamp.now()
                            )
                            sendExpenseStatusNotification(expense, true, reviewerName)
                            delay(100)
                        } else {
                            failCount++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        failCount++
                    }
                }

                if (failCount > 0) {
                    _error.value = "Some expenses could not be approved ($failCount failed)"
                }

                delay(500)
                refreshPendingExpenses()

            } catch (e: Exception) {
                _error.value = "Failed to approve selected expenses: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    private fun refreshPendingExpenses() {
        viewModelScope.launch {
            try {
                val projectId = _currentProjectId.value
                val userRoleString = _currentUserRole.value
                
                // Convert String? to UserRole enum
                val userRole = when (userRoleString?.uppercase()) {
                    "BUSINESS_HEAD" -> UserRole.BUSINESS_HEAD
                    "APPROVER" -> UserRole.APPROVER
                    "ADMIN" -> UserRole.ADMIN
                    "USER" -> UserRole.USER
                    else -> UserRole.APPROVER // Default fallback
                }
                
                if (projectId != null) {
                    loadPendingApprovalsForProject(projectId, forceRefresh = true,userRole = userRole)
                } else {
                    loadPendingApprovals(forceRefresh = true, userRole = userRole)
                }
            } catch (_: Exception) {
                // Ignore refresh errors – UI already shows previous data and error state elsewhere
            }
        }
    }
    
    fun rejectSelectedExpenses(expenseIds: List<String>, reviewerName: String, comments: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null

            try {
                val currentExpenses = _pendingExpenses.value
                var failCount = 0

                for (expenseId in expenseIds) {
                    try {
                        val expense = currentExpenses.find { it.id == expenseId }
                        if (expense != null) {
                            expenseRepository.updateExpenseStatus(
                                projectId = expense.projectId,
                                expenseId = expenseId,
                                status = com.cubiquitous.tracura.model.ExpenseStatus.REJECTED,
                                reviewedBy = reviewerName,
                                reviewComments = comments,
                                isVerified = expense.isVerified,
                                verifiedBy = expense.verifiedBy,
                                byCredit = expense.byCredit ?: false,
                                verifiedAt = expense.verifiedAt ?: com.google.firebase.Timestamp.now()
                            )
                            sendExpenseStatusNotification(expense, false, reviewerName)
                            delay(100)
                        } else {
                            failCount++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        failCount++
                    }
                }

                if (failCount > 0) {
                    _error.value = "Some expenses could not be rejected ($failCount failed)"
                }

                delay(500)
                refreshPendingExpenses()

            } catch (e: Exception) {
                _error.value = "Failed to reject selected expenses: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun sendExpenseStatusNotification(expense: Expense, isApproved: Boolean, approverName: String) {
        viewModelScope.launch {
            try {
                notificationService.sendExpenseStatusNotification(
                    expenseId = expense.id,
                    projectId = expense.projectId,
                    submittedByUserId = expense.userId,
                    isApproved = isApproved,
                    amount = expense.amount,
                    reviewerName = approverName,
                    comments = expense.reviewComments ?: ""
                )
            } catch (_: Exception) {
                // Ignore notification failures – core approval flow should still succeed
            }
        }
    }
    
    /**
     * Upload payment proof file and update payment mode and payment proof for an expense
     */
    suspend fun uploadPaymentProofAndUpdate(
        expense: Expense,
        paymentProofUri: android.net.Uri,
        paymentProofName: String,
        paymentMode: com.cubiquitous.tracura.model.PaymentMode
    ): Result<Pair<String, String>> {
        return try {
            // Upload payment proof to Firebase Storage
            val uploadResult = expenseRepository.uploadAttachmentToStorage(
                attachmentUri = paymentProofUri.toString(),
                fileName = paymentProofName,
                projectId = expense.projectId,
                userId = expense.submittedBy
            )
            
            if (uploadResult.isSuccess) {
                val (url, name) = uploadResult.getOrNull()!!
                Result.success(Pair(url, name))
            } else {
                Result.failure(uploadResult.exceptionOrNull() ?: Exception("Upload failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update payment mode and payment proof for an expense
     */
    fun updateExpensePayment(
        expense: Expense,
        paymentMode: com.cubiquitous.tracura.model.PaymentMode,
        paymentProofURL: String? = null,
        paymentProofName: String? = null,
        paidBy: String = ""
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null

            try {
                Log.d("UpdatingPaymentModel", "going into Expesne Repository")
                expenseRepository.updateExpensePayment(
                    expense = expense,
                    paymentMode = paymentMode,
                    paymentProofURL = paymentProofURL,
                    paymentProofName = paymentProofName,
                    paidBy = paidBy
                )
                
                
                // Refresh the expense to get updated data
                delay(500)
                fetchExpenseById(expense.id)
                
                // Refresh pending approvals list to remove COMPLETE expenses
                delay(500)
                refreshPendingExpenses()
                
            } catch (e: Exception) {
                _error.value = "Failed to update payment: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Update expense with partial payment
     */
    fun updateExpensePartialPayment(
        expense: Expense,
        paymentAmount: Double,
        paymentMode: com.cubiquitous.tracura.model.PaymentMode,
        paymentProofURL: String? = null,
        paymentProofName: String? = null,
        paidBy: String
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            try {
                // Use NonCancellable to ensure this critical operation completes
                // even if the viewModelScope is cancelled
                withContext(kotlinx.coroutines.NonCancellable) {
                    expenseRepository.updateExpensePartialPayment(
                        projectId = expense.projectId,
                        expenseId = expense.id,
                        paymentAmount = paymentAmount,
                        paymentMode = paymentMode,
                        paymentProofURL = paymentProofURL,
                        paymentProofName = paymentProofName,
                        paidBy = paidBy
                    )
                }

                
                // Refresh the expense to get updated data
                delay(500)
                fetchExpenseById(expense.id)
                
                // Refresh pending approvals list
                delay(500)
                refreshPendingExpenses()
                
            } catch (e: Exception) {
                _error.value = "Failed to update partial payment: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    
    /**
     * Bulk update multiple expenses payment
     */
    fun bulkUpdateExpensesPayment(
        expenses: List<Expense>,
        paymentMode: com.cubiquitous.tracura.model.PaymentMode,
        paymentProofURL: String? = null,
        paymentProofName: String? = null
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null

            try {
                // Use updateExpensePayment per expense so department/phase/project budgets
                // are deducted the same way as single-expense payment in ReviewExpenseScreen.
                for (expense in expenses) {
                    expenseRepository.updateExpensePayment(
                        expense = expense,
                        paymentMode = paymentMode,
                        paymentProofURL = paymentProofURL,
                        paymentProofName = paymentProofName
                    )
                }

                // Refresh pending approvals list to remove COMPLETE expenses
                delay(500)
                refreshPendingExpenses()

            } catch (e: Exception) {
                _error.value = "Failed to update expenses payment: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Update credit payment date for an expense
     */
    fun updateCreditPaymentDate(
        expense: Expense,
        creditPaymentDate: com.google.firebase.Timestamp
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null

            try {
                expenseRepository.updateCreditPaymentDate(
                    projectId = expense.projectId,
                    expenseId = expense.id,
                    creditPaymentDate = creditPaymentDate
                )

                // Refresh the expense to get updated data
                delay(500)
                fetchExpenseById(expense.id)

            } catch (e: Exception) {
                _error.value = "Failed to update credit payment date: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun updateIsToManager(expense: Expense, isToManager: Boolean) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            try {
                expenseRepository.updateIsToManager(
                    projectId = expense.projectId,
                    expenseId = expense.id,
                    isToManager = isToManager
                )
                delay(500)
                fetchExpenseById(expense.id)
            } catch (e: Exception) {
                _error.value = "Failed to update manager request: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
} 
