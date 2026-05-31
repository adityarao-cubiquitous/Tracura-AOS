package com.cubiquitous.tracura.model

/**
 * Data class for Department Cell display
 * Used to transform Department data for UI display
 */
data class DepartmentCellData(
    val id: String,
    val name: String,
    val contractorMode: String,
    val budget: Double,
    val spent: Double,
    val materials: List<MaterialItem>,
    val labour: LabourInfo?,
    /** All line items from the department (for "View all" sheet) */
    val allLineItems: List<DepartmentLineItemData> = emptyList()
) {
    val remaining: Double
        get() = maxOf(budget - spent, 0.0)
}

/**
 * Material item for display in department cell
 */
data class MaterialItem(
    val name: String, // item field from lineItem
    val quantity: Double,
    val uom: String
)

/**
 * Labour information aggregated from labour line items
 */
data class LabourInfo(
    val totalAmount: Double,
    val totalQuantity: Double,
    val uom: String // Most common UOM from labour items
)
