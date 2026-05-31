package com.cubiquitous.tracura.model

/**
 * Phase budget information with total budget and spent amount
 * Used for calculating approved/spent amounts per phase
 */
data class PhaseBudget(
    val id: String,
    val totalBudget: Double,
    val spent: Double
) {
    val remaining: Double
        get() = totalBudget - spent
    
    val spentPercentage: Double
        get() = if (totalBudget > 0) (spent / totalBudget) * 100 else 0.0
}
