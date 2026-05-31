package com.cubiquitous.tracura.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.Timestamp

enum class PaymentMode {
    CASH,
    UPI,
    CHEQUE,
    CARD;
    
    companion object {
        fun fromString(value: String?): PaymentMode {
            return when (value?.lowercase()) {
                "cash", "by cash" -> CASH
                "upi", "by upi" -> UPI
                "cheque", "check", "by cheque", "by check" -> CHEQUE
                "card", "by card" -> CARD
                else -> CASH // Default fallback
            }
        }
        
        fun toString(mode: PaymentMode): String {
            return when (mode) {
                CASH -> "By cash"
                UPI -> "By UPI"
                CHEQUE -> "By cheque"
                CARD -> "By Card"
            }
        }
    }
}

enum class ExpenseStatus {
    PENDING,
    APPROVED,
    REJECTED,
    COMPLETE
}

data class Expense(
    @PropertyName("id") val id: String = "",
    @PropertyName("expenseCode") val expenseCode: String = "",
    @PropertyName("projectId") val projectId: String = "",
    @PropertyName("date") val date: String = "", // Format: "dd/MM/yyyy"
    @PropertyName("amount") val amount: Double = 0.0,
    @PropertyName("department") val department: String = "",
    @PropertyName("phaseId") val phaseId: String? = null,
    @PropertyName("phaseName") val phaseName: String? = null,
    @PropertyName("categories") val categories: List<String> = emptyList(),
    @PropertyName("modeOfPayment") val modeOfPayment: PaymentMode = PaymentMode.CASH,
    @PropertyName("description") val description: String = "",
    @PropertyName("attachmentURL") val attachmentURL: String? = null,
    @PropertyName("attachmentName") val attachmentName: String? = null,
    @PropertyName("paymentProofURL") val paymentProofURL: String? = null,
    @PropertyName("paymentProofName") val paymentProofName: String? = null,
    @PropertyName("submittedBy") val submittedBy: String = "", // User phone number
    @PropertyName("status") val status: ExpenseStatus,
    @PropertyName("remark") val remark: String? = null,
    @PropertyName("isAdmin") val isAdmin: Boolean = false,
    @PropertyName("approvedBy") val approvedBy: String? = null,
    @PropertyName("rejectedBy") val rejectedBy: String = "",
    
    // Verification and Payment fields
    @PropertyName("isVerified") val isVerified: Boolean = false,
    @PropertyName("verifiedBy") val verifiedBy: String = "",
    @PropertyName("verifiedAt") val verifiedAt: Timestamp? = null,
    @PropertyName("isPaymentCompleted") val isPaymentCompleted: Boolean = false,
    @PropertyName("By-Credict") val byCredit: Boolean = false,
    @PropertyName("vendour") val vendor: String = "",
    @PropertyName("CreditPaymentDate") val creditPaymentDate: Timestamp? = null,
    @PropertyName("remainingBalance") val remainingBalance: Double? = null,
    
    // Material Details (optional - for backward compatibility)
    @PropertyName("lineItems") val lineItems: List<DepartmentLineItemData> = emptyList(),
    @PropertyName("itemType") val itemType: String? = null, // Sub-category (Global)
    @PropertyName("item") val item: String? = null, // Material
    @PropertyName("brand") val brand: String? = null, // Brand (optional)
    @PropertyName("spec") val spec: String? = null, // Grade/Spec
    @PropertyName("thickness") val thickness: String? = null, // Thickness
    @PropertyName("quantity") val quantity: String? = null, // Quantity
    @PropertyName("uom") val uom: String? = null, // Unit of Measure
    @PropertyName("unitPrice") val unitPrice: String? = null, // Unit Price
    @PropertyName("sgst") val sgst: String = "",
    @PropertyName("cgst") val cgst: String = "",
    @PropertyName("contractAmount") val contractAmount: Double? = null,

    // Anonymous Department Tracking
    @PropertyName("isAnonymous") val isAnonymous: Boolean? = null,
    @PropertyName("originalDepartment") val originalDepartment: String? = null,
    @PropertyName("departmentDeletedAt") val departmentDeletedAt: Timestamp? = null,
    
    // Firestore Timestamps
    @PropertyName("createdAt") val createdAt: Timestamp = Timestamp.now(),
    @PropertyName("updatedAt") val updatedAt: Timestamp = Timestamp.now(),
    
    // Legacy fields for backward compatibility (deprecated)
    @PropertyName("userId") val userId: String = "",
    @PropertyName("userName") val userName: String = "",
    // Note: category is now a computed property (see below) - removed from constructor to avoid conflict
    @PropertyName("attachmentUrl") val attachmentUrl: String = "", // Use attachmentURL instead
    @PropertyName("attachmentFileName") val attachmentFileName: String = "", // Use attachmentName instead
    @PropertyName("paymentProofUrl") val paymentProofUrl: String = "", // Use paymentProofURL instead
    @PropertyName("paymentProofFileName") val paymentProofFileName: String = "", // Use paymentProofName instead
    @PropertyName("submittedAt") val submittedAt: Timestamp? = null, // Use createdAt instead
    @PropertyName("reviewedAt") val reviewedAt: Timestamp? = null,
    @PropertyName("reviewedBy") val reviewedBy: String = "",
    @PropertyName("reviewComments") val reviewComments: String = "",
    @PropertyName("receiptNumber") val receiptNumber: String = "",
    @PropertyName("budgetExceeded") val budgetExceeded: Boolean = false,
    @PropertyName("isToManager") val isToManager: Boolean = false
) {
    // Computed properties for backward compatibility
    val category: String get() = categories.firstOrNull() ?: ""
    
    // Helper to get date as Timestamp for backward compatibility
    fun getDateAsTimestamp(): Timestamp? {
        return try {
            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
            val parsedDate = dateFormat.parse(date)
            parsedDate?.let { Timestamp(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    // Helper to format amount
    fun getAmountFormatted(): String {
        val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "IN"))
        return formatter.format(amount)
    }
    
    // Helper to format date
    fun getDateFormatted(): String {
        return try {
            val inputFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
            val outputFormatter = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
            val dateObj = inputFormatter.parse(date)
            dateObj?.let { outputFormatter.format(it) } ?: "Invalid Date"
        } catch (e: Exception) {
            "Invalid Date"
        }
    }
    
    // Helper to get categories as string
    fun getCategoriesString(): String {
        return categories.joinToString(", ")
    }
}

data class PaymentHistory(
    @PropertyName("id") val id: String = "",
    @PropertyName("amount") val amount: Double = 0.0,
    @PropertyName("paymentMode") val paymentMode: PaymentMode = PaymentMode.CASH,
    @PropertyName("paymentProofURL") val paymentProofURL: String? = null,
    @PropertyName("paymentProofName") val paymentProofName: String? = null,
    @PropertyName("paidAt") val paidAt: Timestamp = Timestamp.now(),
    @PropertyName("paidBy") val paidBy: String = ""
) {
    // Helper to format amount
    fun getAmountFormatted(): String {
        val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "IN"))
        return formatter.format(amount)
    }
    
    // Helper to format date
    fun getDateFormatted(): String {
        return try {
            val formatter = java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a", java.util.Locale.US)
            formatter.format(paidAt.toDate())
        } catch (e: Exception) {
            "Invalid Date"
        }
    }
}

// Note: Summary data classes moved to com.cubiquitous.tracura.model.Summary
// Import from Summary.kt: ExpenseSummary, ExpenseFormData, StatusCounts 
