package com.cubiquitous.tracura.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp

// ProjectStatus enum matching iOS and guide
enum class ProjectStatus(val value: String) {
    DRAFT("DRAFT"),
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    COMPLETED("COMPLETED"),
    HANDOVER("HANDOVER"),
    SUSPENDED("SUSPENDED"),
    LOCKED("LOCKED"),
    IN_REVIEW("IN_REVIEW"),
    DECLINED("DECLINED"),
    STANDBY("STANDBY"),
    MAINTENANCE("MAINTENANCE"),
    ARCHIVE("ARCHIVE");
    
    companion object {
        fun fromString(value: String): ProjectStatus {
            // Handle both "IN REVIEW" and "IN_REVIEW" formats
            val normalizedValue = value.uppercase().replace(" ", "_")
            return values().find { it.value == normalizedValue || it.value == value } ?: INACTIVE
        }
    }
    
    /**
     * Get color for status badge (as per guide)
     */
    fun getColor(): androidx.compose.ui.graphics.Color {
        return when (this) {
            ProjectStatus.ACTIVE -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
            ProjectStatus.COMPLETED -> androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
            ProjectStatus.IN_REVIEW -> androidx.compose.ui.graphics.Color(0xFF00BCD4) // Cyan
            ProjectStatus.LOCKED -> androidx.compose.ui.graphics.Color(0xFF3F51B5) // Indigo
            ProjectStatus.SUSPENDED -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
            ProjectStatus.STANDBY -> androidx.compose.ui.graphics.Color(0xFFE91E63) // Pink
            ProjectStatus.DECLINED -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
            ProjectStatus.MAINTENANCE -> androidx.compose.ui.graphics.Color(0xFF9C27B0) // Purple
            ProjectStatus.ARCHIVE -> androidx.compose.ui.graphics.Color(0xFF9E9E9E) // Gray
            ProjectStatus.DRAFT -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
            else -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Default Green
        }
    }
    
    /**
     * Get display text for status (as per guide)
     */
    fun getDisplayText(): String {
        return when (this) {
            ProjectStatus.ACTIVE -> "ACTIVE"
            ProjectStatus.COMPLETED -> "COMPLETED"
            ProjectStatus.IN_REVIEW -> "IN REVIEW"
            ProjectStatus.LOCKED -> "LOCKED"
            ProjectStatus.SUSPENDED -> "SUSPENDED"
            ProjectStatus.STANDBY -> "STANDBY"
            ProjectStatus.DECLINED -> "DECLINED"
            ProjectStatus.MAINTENANCE -> "MAINTENANCE"
            ProjectStatus.ARCHIVE -> "ARCHIVE"
            ProjectStatus.DRAFT -> "DRAFT"
            else -> value
        }
    }
}

data class Project(
    @DocumentId @PropertyName("id") val id: String? = null,
    @PropertyName("name") val name: String = "",
    @PropertyName("description") val description: String = "",
    @PropertyName("client") val client: String = "",
    @PropertyName("clientPrimaryNumber") val clientPrimaryNumber: String? = null,
    @PropertyName("clientSecondaryNumber") val clientSecondaryNumber: String? = null,
    @PropertyName("location") val location: String = "",
    @PropertyName("currency") val currency: String = "",
    @PropertyName("budget") val budget: Double = 0.0,
    @PropertyName("status") val status: String = "INACTIVE",
    @PropertyName("plannedDate") val plannedDate: String? = null, // Format: "dd/MM/yyyy" - Planned Start Date (Firebase field name)
    @PropertyName("startDate") val startDate: String? = null, // Format: "dd/MM/yyyy" - Planned Start Date (legacy/fallback)
    @PropertyName("endDate") val endDate: String? = null, // Format: "dd/MM/yyyy"
    @PropertyName("handoverDate") val handoverDate: String? = null, // Format: "dd/MM/yyyy"
    @PropertyName("maintenanceDate") val maintenanceDate: String? = null, // Format: "dd/MM/yyyy"
    @PropertyName("teamMembers") val teamMembers: List<String> = emptyList(),
    @PropertyName("departmentUserAssignments") val departmentUserAssignments: Map<String, String> = emptyMap(), // Map<DepartmentName, UserPhone>
    @PropertyName("departmentApproverAssignments") val departmentApproverAssignments: Map<String, String> = emptyMap(), // Map<DepartmentName, ApproverPhone>
    @PropertyName("tempApproverID") val tempApproverID: String? = null,
    @PropertyName("Allow_Template_Overrides") val Allow_Template_Overrides: Boolean? = null,
    @PropertyName("projectCode") val projectCode: String = "", // Auto-generated project code (e.g., "RGV1", "STO2")
    @PropertyName("createdAt") val createdAt: Timestamp = Timestamp.now(),
    @PropertyName("updatedAt") val updatedAt: Timestamp = Timestamp.now(),
    
    // Legacy fields for backward compatibility (will be removed in future)
    @PropertyName("spent") val spent: Double? = null,
    @PropertyName("managerId") val managerId: String? = null,
    @PropertyName("approverIds") val approverIds: List<String>? = null,
    @PropertyName("BusinessHeadIds") val BusinessHeadIds: List<String>? = null,
    @PropertyName("code") val code: String? = null,
    @PropertyName("departmentBudgets") val departmentBudgets: Map<String, Double>? = null,
    @PropertyName("categories") val categories: List<String>? = null,
    @PropertyName("temporaryApproverPhone") val temporaryApproverPhone: String? = null,
    @PropertyName("rejectionReason") val rejectionReason: String? = null,
    @PropertyName("rejectedBy") val rejectedBy: String? = null, // Document ID of user who rejected the project
    
    // Project Suspension fields
    @PropertyName("isSuspended") val isSuspended: Boolean? = null,
    @PropertyName("suspendedDate") val suspendedDate: String? = null, // Format: "dd/MM/yyyy"
    @PropertyName("suspensionReason") val suspensionReason: String? = null,
    
    // Budget Tracking
    @PropertyName("remainingBalance") val remainingBalance: Double? = null,
    
    // Temporary Approver Management
    @PropertyName("DepartmentTemporaryApprover") val departmentTemporaryApprover: List<DepartmentTemporaryApproverEntry> = emptyList()
) {
    // Computed properties matching iOS
    val statusType: ProjectStatus
        get() = ProjectStatus.fromString(status)
    
    val budgetFormatted: String
        get() {
            val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "IN"))
            return formatter.format(budget) ?: "₹0.00"
        }
    
    val dateRangeFormatted: String
        get() {
            val inputFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val outputFormatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            
            // Use plannedDate if available, otherwise fallback to startDate for backward compatibility
            val startDateValue = plannedDate ?: startDate
            
            return when {
                startDateValue != null && endDate != null -> {
                    val start = inputFormatter.parse(startDateValue)
                    val end = inputFormatter.parse(endDate)
                    if (start != null && end != null) {
                        "${outputFormatter.format(start)} - ${outputFormatter.format(end)}"
                    } else {
                        "Invalid Dates"
                    }
                }
                startDateValue != null -> {
                    val start = inputFormatter.parse(startDateValue)
                    if (start != null) {
                        "From ${outputFormatter.format(start)}"
                    } else {
                        "Invalid Date"
                    }
                }
                endDate != null -> {
                    val end = inputFormatter.parse(endDate)
                    if (end != null) {
                        "Until ${outputFormatter.format(end)}"
                    } else {
                        "Invalid Date"
                    }
                }
                else -> "No timeline set"
            }
        }
    
    val lastUpdatedDate: java.util.Date
        get() = updatedAt.toDate() // updatedAt is non-null Timestamp
}

data class DepartmentBudget(
    val departmentName: String = "",
    val allocatedBudget: Double = 0.0
) 


data class DepartmentTemporaryApproverEntry(
    @PropertyName("phone") val phone: String = "",
    @PropertyName("startDate") val startDate: java.util.Date? = null,
    @PropertyName("endDate") val endDate: java.util.Date? = null,
    @PropertyName("isAccepted") val isAccepted: Boolean = true,
    @PropertyName("isActive") val isActive: Boolean = true,
    @PropertyName("departmentName") val departmentName: String = ""
)

// Contractor Mode enum
enum class ContractorMode {
    LABOUR_ONLY,
    SELF_EXECUTION,
    TURNKEY_AMOUNT_ONLY
}

// Line Item data model
data class LineItem(
    @PropertyName("itemType") val itemType: String = "",
    @PropertyName("item") val item: String = "",
    @PropertyName("spec") val spec: String = "",
    @PropertyName("quantity") val quantity: Double = 0.0,
    @PropertyName("unitPrice") val unitPrice: Double = 0.0,
    @PropertyName("uom") val uom: String = ""
) {
    val total: Double
        get() = quantity * unitPrice
}

// Department with contractor mode and line items (UI only, not saved to backend)
data class DepartmentDraft(
    val departmentName: String = "",
    val contractorMode: ContractorMode = ContractorMode.LABOUR_ONLY,
    val lineItems: MutableList<LineItem> = mutableListOf(),
    val contractorAmount: Double = 0.0
) {
    val totalBudget: Double
        get() = when (contractorMode) {
            ContractorMode.LABOUR_ONLY -> lineItems.sumOf { it.total } + contractorAmount
            ContractorMode.SELF_EXECUTION -> lineItems.sumOf { it.total }
            ContractorMode.TURNKEY_AMOUNT_ONLY -> contractorAmount
        }
}

// Draft model for building phases in the UI before saving
data class PhaseDraft(
    var phaseName: String = "",
    var start: java.util.Date? = null,
    var end: java.util.Date? = null,
    var departments: MutableList<DepartmentDraft> = mutableListOf(), // Changed from DepartmentBudget to DepartmentDraft
    var categories: MutableList<String> = mutableListOf()
) 
