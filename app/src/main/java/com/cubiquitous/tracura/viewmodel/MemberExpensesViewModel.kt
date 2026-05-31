package com.cubiquitous.tracura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.repository.ExpenseRepository
import com.cubiquitous.tracura.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemberExpensesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val totalExpenses: Int
        get() = _expenses.value.size

    val totalAmount: String
        get() = _expenses.value.sumOf { it.amount }.toInt().formatCurrency()

    /**
     * Load all expenses for a member in a project.
     *
     * Expenses can be stored with submittedBy as any of:
     *   - phone without +91  (e.g. "9876543210")
     *   - phone with +91     (e.g. "+919876543210")
     *   - uid                (Firebase Auth UID)
     *   - email
     *
     * We build a candidate list of all known identifiers and use whereIn so we
     * catch expenses no matter which value was stored.
     */
    suspend fun loadExpenses(projectId: String, member: User) {
        _isLoading.value = true
        _errorMessage.value = null

        try {
            // Build all candidate identifiers for this member
            val candidates = buildSet<String> {
                // Phone variants
                val rawPhone = member.phone.trim()
                if (rawPhone.isNotEmpty()) {
                    add(rawPhone)
                    // Strip +91 prefix
                    val stripped = rawPhone.removePrefix("+91").trim()
                    add(stripped)
                    // Add +91 prefix if not already present
                    if (!rawPhone.startsWith("+")) add("+91$rawPhone")
                }
                // UID
                if (member.uid.isNotEmpty()) add(member.uid)
                // Email
                if (member.email.isNotEmpty()) add(member.email.trim())
            }.filter { it.isNotEmpty() }.distinct()

            val expenses = expenseRepository.getMemberExpensesForProject(projectId, candidates)

            _expenses.value = expenses.sortedByDescending { it.createdAt.toDate().time }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load expenses: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    private fun Int.formatCurrency(): String {
        val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "IN"))
        formatter.maximumFractionDigits = 0
        return formatter.format(this)
    }
}
