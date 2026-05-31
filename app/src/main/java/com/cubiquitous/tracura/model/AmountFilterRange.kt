package com.cubiquitous.tracura.model

import java.text.NumberFormat
import java.util.*

/**
 * Sealed class representing different amount filter ranges
 */
sealed class AmountFilterRange {
    object Under1000 : AmountFilterRange()
    object Between1000And5000 : AmountFilterRange()
    object Between5000And10000 : AmountFilterRange()
    object Over10000 : AmountFilterRange()
    data class Custom(val min: Double, val max: Double) : AmountFilterRange()
    
    val displayName: String
        get() = when (this) {
            is Under1000 -> "Under ₹1,000"
            is Between1000And5000 -> "₹1,000 - ₹5,000"
            is Between5000And10000 -> "₹5,000 - ₹10,000"
            is Over10000 -> "Over ₹10,000"
            is Custom -> "${formatCurrency(min.toInt())} - ${formatCurrency(max.toInt())}"
        }
    
    val chipDisplayName: String
        get() = when (this) {
            is Under1000 -> "Under ₹1K"
            is Between1000And5000 -> "₹1K - ₹5K"
            is Between5000And10000 -> "₹5K - ₹10K"
            is Over10000 -> "Over ₹10K"
            is Custom -> "${formatCurrency(min.toInt())} - ${formatCurrency(max.toInt())}"
        }
}

fun formatCurrency(amount: Int): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}
