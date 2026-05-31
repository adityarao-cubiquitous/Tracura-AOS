package com.cubiquitous.tracura.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.DocumentId

/**
 * Phase model - matches Swift Phase structure
 * Departments are stored in subcollection: phases/{phaseId}/departments
 * The departments Map is kept for backward compatibility
 */
data class Phase(
	@DocumentId @PropertyName("id") val id: String = "",
	@PropertyName("phaseName") val phaseName: String = "",
	@PropertyName("phaseNumber") val phaseNumber: Int = 0, // Order of the phase (1, 2, 3, etc.)
	@PropertyName("startDate") val startDate: String? = null, // Format: "dd/MM/yyyy"
	@PropertyName("endDate") val endDate: String? = null, // Format: "dd/MM/yyyy"
	@PropertyName("departments") val departments: Map<String, Double> = emptyMap(), // Departments with their budgets (backward compatibility)
	@PropertyName("categories") val categories: List<String> = emptyList(), // Categories for this phase
	@PropertyName("isEnabled") val isEnabled: Boolean? = null, // Whether this phase is enabled (default true)
	@PropertyName("phaseOverProofUrl") val phaseOverProofUrl: String? = null, // URL for phase completion proof document
	@PropertyName("isanonymous") val isanonymous: Map<String, Boolean> = emptyMap(), // Track deleted departments: departmentName -> true
	@PropertyName("totalBudget") val totalBudgetValue: Double? = null, // Stored phase total budget (from subcollection sum)
	@PropertyName("remainingBudget") val remainingBudget: Double? = 0.0, // Tracks remaining phase budget after payments; null = not yet set (use totalBudget)
	@PropertyName("createdAt") val createdAt: Timestamp = Timestamp.now(),
	@PropertyName("updatedAt") val updatedAt: Timestamp = Timestamp.now()
) {
	// Total budget uses stored field when available; falls back to departments map for older data
	val totalBudget: Double
		get() = totalBudgetValue ?: departments.values.sum()
	
	// Default to true when the field is absent in Firestore
	val isEnabledValue: Boolean
		get() = isEnabled ?: true
	
	// Format date range for display
	fun dateRangeFormatted(): String {
		return when {
			startDate != null && endDate != null -> "$startDate - $endDate"
			startDate != null -> "Since: $startDate"
			endDate != null -> "Until: $endDate"
			else -> "Dates not set"
		}
	}
}


