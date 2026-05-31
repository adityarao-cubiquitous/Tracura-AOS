package com.cubiquitous.tracura.model

import android.util.Log
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp

/**
 * Department Line Item Data model - for Firestore
 * Matches Swift DepartmentLineItemData structure
 */
data class DepartmentLineItemData(
    @PropertyName("itemType") val itemType: String = "",
    @PropertyName("item") val item: String = "",
    @PropertyName("spec") val spec: String = "",
    @PropertyName("quantity") val quantity: Double = 0.0,
    @PropertyName("uom") val uom: String = "", // Unit of Measurement
    @PropertyName("unitPrice") val unitPrice: Double = 0.0,
    @PropertyName("remainingQuantity") val remainingQuantity: Double = 0.0
) {
    val total: Double
        get() = quantity * unitPrice
}

/**
 * Department model - saved as separate documents in phases/{phaseId}/departments collection
 * Matches Swift Department structure
 */
data class Department(
    @DocumentId @PropertyName("id") val id: String? = null,
    @PropertyName("name") val name: String = "",
    @PropertyName("contractorMode") val contractorMode: String = "Labour-Only", // "Labour-Only", "Self Execution", or "Turnkey"
    @PropertyName("lineItems") val lineItems: List<DepartmentLineItemData> = emptyList(),
    @PropertyName("phaseId") val phaseId: String = "", // Reference to parent phase
    @PropertyName("projectId") val projectId: String = "", // Reference to parent project
    @PropertyName("remainingAmount") val remainingAmount: Double = 0.0, // Remaining budget balance
    @PropertyName("totalBudget") val totalBudget: Double = 0.0, // Total budget amount
    @PropertyName("contractorAmount") val contractorAmount: Double = 0.0, // Labour-Only: agreed contractor amount
    @PropertyName("contractorRemainingAmount") val contractorRemainingAmount: Double = 0.0, // Labour-Only: remaining contractor amount
    @PropertyName("isFinished") val isFinished: Boolean = false,
    @PropertyName("createdAt") val createdAt: Timestamp = Timestamp.now(),
    @PropertyName("updatedAt") val updatedAt: Timestamp = Timestamp.now()
) {
    val contractorModeEnumValue: ContractorMode
        get() = try {
            when (contractorMode) {
                "Labour-Only", "LABOUR_ONLY" -> ContractorMode.LABOUR_ONLY
                "Self Execution", "SELF_EXECUTION" -> ContractorMode.SELF_EXECUTION
                "Turnkey", "TURNKEY" -> ContractorMode.TURNKEY_AMOUNT_ONLY
                "Turnkey-Amount-Only", "TURNKEY_AMOUNT_ONLY" -> ContractorMode.TURNKEY_AMOUNT_ONLY
                else -> ContractorMode.LABOUR_ONLY
            }
        } catch (e: Exception) {
            ContractorMode.LABOUR_ONLY
        }
    
    companion object {
        /**
         * Convert ContractorMode enum to display format
         */
        private fun ContractorMode.toDisplayFormat(): String {
            return when (this) {
                ContractorMode.LABOUR_ONLY -> "Labour-Only"
                ContractorMode.SELF_EXECUTION -> "Self Execution"
                ContractorMode.TURNKEY_AMOUNT_ONLY -> "Turnkey"
            }
        }
        
        private fun ContractorMode.toFirestoreValue(): String {
            return when (this) {
                ContractorMode.LABOUR_ONLY -> "Labour-Only"
                ContractorMode.SELF_EXECUTION -> "Self Execution"
                ContractorMode.TURNKEY_AMOUNT_ONLY -> "Turnkey"
            }
        }
        
        /**
         * Parse display format string to ContractorMode enum
         */
        fun parseContractorMode(mode: String): ContractorMode {
            return when (mode) {
                "Labour-Only", "LABOUR_ONLY" -> ContractorMode.LABOUR_ONLY
                "Self Execution", "SELF_EXECUTION" -> ContractorMode.SELF_EXECUTION
                "Turnkey", "TURNKEY" -> ContractorMode.TURNKEY_AMOUNT_ONLY
                "Turnkey-Amount-Only", "TURNKEY_AMOUNT_ONLY" -> ContractorMode.TURNKEY_AMOUNT_ONLY
                else -> ContractorMode.LABOUR_ONLY
            }
        }
        
        fun fromDepartmentDraft(draft: DepartmentDraft, phaseId: String = "", projectId: String = ""): Department {
            return try {
                val mode = draft.contractorMode
        
                // Filter out null, empty, or invalid lineItems and convert LineItem to DepartmentLineItemData
                // Turnkey contractor-only and amount-only departments should not persist line items.
                val validLineItems = if (mode == ContractorMode.TURNKEY_AMOUNT_ONLY) {
                    emptyList()
                } else {
                    draft.lineItems.filterNotNull().filter { lineItem ->
                        lineItem.itemType.isNotBlank() &&
                            lineItem.item.isNotBlank() &&
                            lineItem.spec.isNotBlank() &&
                            lineItem.quantity > 0 &&
                            lineItem.unitPrice > 0
                    }.map { lineItem ->
                        DepartmentLineItemData(
                            itemType = lineItem.itemType,
                            item = lineItem.item,
                            spec = lineItem.spec,
                            quantity = lineItem.quantity,
                            uom = lineItem.uom,
                            unitPrice = lineItem.unitPrice,
                            remainingQuantity = lineItem.quantity
                        )
                    }
                }
        
                val contractorModeDisplay = mode.toDisplayFormat()
                val contractorModeFirestore = mode.toFirestoreValue()
        
                val lineItemsBudget = validLineItems.sumOf { it.total }
                val contractorAmt = when (mode) {
                    ContractorMode.LABOUR_ONLY -> draft.contractorAmount
                    ContractorMode.SELF_EXECUTION -> 0.0
                    ContractorMode.TURNKEY_AMOUNT_ONLY -> draft.contractorAmount
                }
                val calculatedBudget = when (mode) {
                    ContractorMode.LABOUR_ONLY -> lineItemsBudget + contractorAmt
                    ContractorMode.SELF_EXECUTION -> lineItemsBudget
                    ContractorMode.TURNKEY_AMOUNT_ONLY -> contractorAmt
                }
        
                Log.d("Department", "🔄 Converting DepartmentDraft to Department:")
                Log.d("Department", "  📝 Name: ${draft.departmentName}")
                Log.d("Department", "  🔧 ContractorMode enum: ${draft.contractorMode}, Display format: $contractorModeDisplay")
                Log.d("Department", "  📋 LineItems before filter: ${draft.lineItems.size}")
                Log.d("Department", "  ✅ Valid LineItems after filter: ${validLineItems.size}")
                Log.d("Department", "  💰 Line items budget: $lineItemsBudget, Contractor amount: $contractorAmt, Total Budget: $calculatedBudget")
        
                draft.lineItems.forEachIndexed { index, item ->
                    val isValid = item != null && item.itemType.isNotBlank() && item.item.isNotBlank() &&
                                  item.spec.isNotBlank() && item.quantity > 0 && item.unitPrice > 0
                    Log.d("Department", "    [$index] ${if (isValid) "✅" else "❌ EMPTY"} itemType='${item?.itemType ?: ""}', item='${item?.item ?: ""}', spec='${item?.spec ?: ""}', qty=${item?.quantity ?: 0.0}, price=${item?.unitPrice ?: 0.0}")
                }
        
                Department(
                    name = draft.departmentName.ifEmpty { "Unnamed Department" },
                    contractorMode = contractorModeFirestore,
                    lineItems = validLineItems,
                    phaseId = phaseId,
                    projectId = projectId,
                    remainingAmount = calculatedBudget,
                    totalBudget = calculatedBudget,
                    contractorAmount = contractorAmt,
                    contractorRemainingAmount = contractorAmt
                )
            } catch (e: Exception) {
                Log.e("Department", "❌ Error converting DepartmentDraft: ${e.message}", e)
                // Return a safe default department
                val contractorModeFirestore = draft.contractorMode.toFirestoreValue()
                val contractorModeDisplay = draft.contractorMode.toDisplayFormat()
                Log.d("Department", "  🔧 Fallback - ContractorMode enum: ${draft.contractorMode}, Display format: $contractorModeDisplay")
                Department(
                    name = draft.departmentName.ifEmpty { "Unnamed Department" },
                    contractorMode = contractorModeFirestore,
                    lineItems = emptyList(),
                    phaseId = phaseId,
                    projectId = projectId,
                    remainingAmount = 0.0,
                    totalBudget = 0.0
                )
            }
        }
        
        /**
         * Create a Department from just name and budget (for backward compatibility)
         * Used when departments are passed as Map<String, Double>
         */
        fun fromNameAndBudget(
            name: String,
            budget: Double,
            phaseId: String = "",
            projectId: String = "",
            contractorAmount: Double = 0.0
        ): Department {
            return Department(
                name = name,
                contractorMode = "Labour-Only",
                lineItems = emptyList(),
                phaseId = phaseId,
                projectId = projectId,
                remainingAmount = budget,
                totalBudget = budget,
                contractorAmount = contractorAmount,
                contractorRemainingAmount = contractorAmount
            )
        }
    }
}
