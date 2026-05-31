package com.cubiquitous.tracura.ui.view.approver

internal enum class BudgetUtilizationSeverity {
    NORMAL,
    WARNING,
    ERROR,
}

internal fun budgetUtilizationSeverity(utilizationPercent: Double): BudgetUtilizationSeverity {
    val safePercent = if (utilizationPercent.isFinite()) utilizationPercent else 0.0
    return when {
        safePercent >= 100.0 -> BudgetUtilizationSeverity.ERROR
        safePercent > 95.0 -> BudgetUtilizationSeverity.WARNING
        else -> BudgetUtilizationSeverity.NORMAL
    }
}

internal fun budgetUtilizationProgress(utilizationPercent: Double): Float {
    val safePercent = if (utilizationPercent.isFinite()) utilizationPercent else 0.0
    return (safePercent / 100.0).coerceIn(0.0, 1.0).toFloat()
}
