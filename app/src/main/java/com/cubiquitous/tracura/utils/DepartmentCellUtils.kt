package com.cubiquitous.tracura.utils

import com.cubiquitous.tracura.model.Department
import com.cubiquitous.tracura.model.DepartmentCellData
import com.cubiquitous.tracura.model.LabourInfo
import com.cubiquitous.tracura.model.MaterialItem

/**
 * Transform Department data for cell display
 */
fun transformDepartmentForCell(
    department: Department,
    spentAmount: Double
): DepartmentCellData {
    // Filter materials (itemType == "Raw material")
    val materials = department.lineItems
        .filter { it.itemType.equals("Raw material", ignoreCase = true) }
        .map { lineItem ->
            MaterialItem(
                name = lineItem.item,
                quantity = lineItem.quantity,
                uom = lineItem.uom
            )
        }
    
    // Filter and aggregate labour (itemType == "Labour")
    val labourItems = department.lineItems.filter { 
        it.itemType.equals("Labour", ignoreCase = true) 
    }
    val labour = if (labourItems.isNotEmpty()) {
        val totalAmount = labourItems.sumOf { it.total }
        val totalQuantity = labourItems.sumOf { it.quantity }
        val mostCommonUom = labourItems.groupBy { it.uom }
            .maxByOrNull { it.value.size }?.key ?: labourItems.first().uom
        
        LabourInfo(
            totalAmount = totalAmount,
            totalQuantity = totalQuantity,
            uom = mostCommonUom
        )
    } else {
        null
    }
    
    return DepartmentCellData(
        id = department.id ?: "",
        name = department.name,
        contractorMode = department.contractorMode,
        budget = department.totalBudget,
        spent = spentAmount,
        materials = materials,
        labour = labour,
        allLineItems = department.lineItems
    )
}
