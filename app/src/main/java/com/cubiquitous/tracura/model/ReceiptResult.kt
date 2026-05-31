package com.cubiquitous.tracura.model

data class ReceiptResult(
    val date: String? = null,
    val amount: String? = null,
    val department: String? = null,
    val categories: String? = null,
    val modeOfPayment: String? = null,
    val description: String? = null,
    val itemType: String? = null,
    val item: String? = null,
    val brand: String? = null,
    val spec: String? = null,
    val thickness: String? = null,
    val quantity: String? = null,
    val uom: String? = null,
    val unitPrice: String? = null
)

/**
 * Represents one OCR-matched line item: the predefined [matchedLineItem] from the
 * department, plus the quantity and unit-price extracted directly from the receipt image.
 */
data class MatchedLineItemResult(
    val matchedLineItem: DepartmentLineItemData,
    val extractedQuantity: Double,
    val extractedUnitPrice: Double,
    val confidence: Float
)
