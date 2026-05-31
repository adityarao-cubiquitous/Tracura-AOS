package com.cubiquitous.tracura.utils

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.cubiquitous.tracura.model.Phase
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper utility for determining phase status based on dates
 * Follows the Android Phase Status Implementation Guide
 * 
 * Key Principles:
 * - Dates are normalized to start of day (00:00:00) for comparison
 * - End date is inclusive (phase is still active on end date)
 * - Phases without dates default to "always visible" (in progress)
 */
object PhaseStatusHelper {
    
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    /**
     * Normalize date to start of day (00:00:00)
     */
    private fun normalizeToStartOfDay(date: Date): Date {
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    /**
     * Parse date string (dd/MM/yyyy) to Date object
     */
    private fun parseDate(dateString: String?): Date? {
        if (dateString.isNullOrBlank()) return null
        return try {
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            Log.e("PhaseStatusHelper", "Error parsing date: $dateString", e)
            null
        }
    }
    
    /**
     * Check if phase is currently in progress
     * Rules:
     * - No dates: Always in progress
     * - Only start date: In progress if start date <= today
     * - Only end date: In progress if today <= end date (inclusive)
     * - Both dates: In progress if start <= today <= end (end inclusive)
     */
    fun isPhaseInProgress(phase: Phase): Boolean {
        val current = normalizeToStartOfDay(Date())
        
        val startDate = parseDate(phase.startDate)
        val endDate = parseDate(phase.endDate)
        
        return when {
            startDate == null && endDate == null -> {
                // No dates: always visible/in progress
                true
            }
            startDate != null && endDate == null -> {
                // Only start date: visible if start date passed or is today
                val startOfDay = normalizeToStartOfDay(startDate)
                startOfDay <= current
            }
            startDate == null && endDate != null -> {
                // Only end date: visible if before or on end date (inclusive)
                val endOfDay = normalizeToStartOfDay(endDate)
                current <= endOfDay
            }
            else -> {
                // Both dates: visible if in range (end date inclusive)
                val startOfDay = normalizeToStartOfDay(startDate!!)
                val endOfDay = normalizeToStartOfDay(endDate!!)
                startOfDay <= current && current <= endOfDay
            }
        }
    }
    
    /**
     * Check if phase has expired
     * Rules:
     * - No dates: Not expired
     * - Only start date: Not expired
     * - Only end date: Expired if today > end date
     * - Both dates: Expired if today > end date
     */
    fun isPhaseExpired(phase: Phase): Boolean {
        val current = normalizeToStartOfDay(Date())
        
        val startDate = parseDate(phase.startDate)
        val endDate = parseDate(phase.endDate)
        
        return when {
            startDate == null && endDate == null -> {
                // No dates: not expired
                false
            }
            startDate != null && endDate == null -> {
                // Only start date: not expired
                false
            }
            startDate == null && endDate != null -> {
                // Only end date: expired if past end date
                val endOfDay = normalizeToStartOfDay(endDate)
                current > endOfDay
            }
            else -> {
                // Both dates: expired if past end date
                val endOfDay = normalizeToStartOfDay(endDate!!)
                current > endOfDay
            }
        }
    }
    
    /**
     * Check if phase is upcoming (hasn't started yet)
     */
    fun isPhaseUpcoming(phase: Phase): Boolean {
        val startDate = parseDate(phase.startDate) ?: return false
        
        val current = normalizeToStartOfDay(Date())
        val startOfDay = normalizeToStartOfDay(startDate)
        
        return startOfDay > current
    }
    
    /**
     * Calculate days remaining until phase end date
     * Returns null if no end date or phase has expired
     */
    fun getDaysRemaining(phase: Phase): Int? {
        val endDate = parseDate(phase.endDate) ?: return null
        
        val current = normalizeToStartOfDay(Date())
        val endOfDay = normalizeToStartOfDay(endDate)
        
        // Calculate difference in days
        val diffInMillis = endOfDay.time - current.time
        val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        
        return if (diffInDays >= 0) diffInDays else null
    }
    
    /**
     * Get color for days remaining indicator
     */
    fun getDaysRemainingColor(days: Int?): Color {
        return when {
            days == null -> Color(0xFF8E8E93) // Gray
            days < 0 -> Color(0xFFFF3B30) // Red - Overdue
            days <= 7 -> Color(0xFFFF3B30) // Red - Critical (< 7 days)
            days <= 14 -> Color(0xFFFF9800) // Orange - Warning (7-14 days)
            else -> Color(0xFF34C759) // Green - Good (> 14 days)
        }
    }
    
    /**
     * Get progress color based on spent percentage
     */
    fun getProgressColor(spentPercentage: Double): Color {
        return when {
            spentPercentage > 1.0 -> Color(0xFFFF3B30) // Red - Over budget
            spentPercentage > 0.8 -> Color(0xFFFF9800) // Orange - Warning (> 80%)
            else -> Color(0xFF007AFF) // Blue - Good
        }
    }
    
    /**
     * Get formatted days remaining text
     */
    fun getDaysRemainingText(phase: Phase): String? {
        val days = getDaysRemaining(phase) ?: return null
        
        return when {
            days < 0 -> "Phase ends today. Please submit all expenses."
            days == 0 -> "Phase ends today. Please submit all expenses."
            else -> "$days days remaining"
        }
    }
    
    /**
     * Check if phase is currently active (strict definition for filtering)
     * Current phases: start date <= today AND end date >= today
     * This is more strict than isPhaseInProgress - requires both dates to be present
     * for a phase to be considered "current" in the list view
     */
    fun isPhaseCurrent(phase: Phase): Boolean {
        val current = normalizeToStartOfDay(Date())
        
        val startDate = parseDate(phase.startDate)
        val endDate = parseDate(phase.endDate)
        
        // For a phase to be "current", we need both start and end dates
        // and start date <= today AND end date >= today
        return when {
            startDate == null || endDate == null -> {
                // Missing dates: not considered "current" for filtering purposes
                false
            }
            else -> {
                // Both dates: current if start <= today <= end (end inclusive)
                val startOfDay = normalizeToStartOfDay(startDate)
                val endOfDay = normalizeToStartOfDay(endDate)
                startOfDay <= current && current <= endOfDay
            }
        }
    }
    
    /**
     * Check if phase is completed (end date has passed)
     * Per Dashboard Phase Logic Implementation Guide
     * Returns true if today > endDate
     */
    fun isPhaseCompleted(phase: Phase): Boolean {
        val endDate = parseDate(phase.endDate) ?: return false
        
        val current = normalizeToStartOfDay(Date())
        val endOfDay = normalizeToStartOfDay(endDate)
        
        return current.after(endOfDay)
    }
    
    /**
     * Check if phase is in future (hasn't started yet)
     * Per Dashboard Phase Logic Implementation Guide
     * Returns true if startDate > today
     * Note: This is the same as isPhaseUpcoming, but kept for consistency with guide
     */
    fun isPhaseInFuture(phase: Phase): Boolean {
        return isPhaseUpcoming(phase)
    }
    
    /**
     * Get phase badge type based on status
     * Priority: Completed > Active > Planned
     * Per Dashboard Phase Logic Implementation Guide
     */
    enum class PhaseBadgeType {
        COMPLETED,  // Blue
        ACTIVE,     // Green
        PLANNED,    // Purple
        EXTENDED    // Orange (shown alongside others)
    }
    
    /**
     * Get phase badge type
     * Returns null if no badge should be shown
     */
    fun getPhaseBadgeType(phase: Phase, isEnabled: Boolean): PhaseBadgeType? {
        return when {
            isPhaseCompleted(phase) -> PhaseBadgeType.COMPLETED
            isPhaseInProgress(phase) && isEnabled -> PhaseBadgeType.ACTIVE
            isPhaseInFuture(phase) -> PhaseBadgeType.PLANNED
            else -> null
        }
    }
    
    /**
     * Get badge text for phase badge type
     */
    fun getBadgeText(badgeType: PhaseBadgeType): String {
        return when (badgeType) {
            PhaseBadgeType.COMPLETED -> "Completed"
            PhaseBadgeType.ACTIVE -> "Active"
            PhaseBadgeType.PLANNED -> "Planned"
            PhaseBadgeType.EXTENDED -> "Extended"
        }
    }
    
    /**
     * Get badge text color for phase badge type
     */
    fun getBadgeTextColor(badgeType: PhaseBadgeType): Color {
        return when (badgeType) {
            PhaseBadgeType.COMPLETED -> Color(0xFF007AFF) // Blue
            PhaseBadgeType.ACTIVE -> Color(0xFF34C759) // Green
            PhaseBadgeType.PLANNED -> Color(0xFFAF52DE) // Purple
            PhaseBadgeType.EXTENDED -> Color(0xFFFF9500) // Orange
        }
    }
    
    /**
     * Get badge background color for phase badge type (12% opacity)
     */
    fun getBadgeBackgroundColor(badgeType: PhaseBadgeType): Color {
        return when (badgeType) {
            PhaseBadgeType.COMPLETED -> Color(0xFF007AFF).copy(alpha = 0.12f) // Blue 12%
            PhaseBadgeType.ACTIVE -> Color(0xFF34C759).copy(alpha = 0.12f) // Green 12%
            PhaseBadgeType.PLANNED -> Color(0xFFAF52DE).copy(alpha = 0.12f) // Purple 12%
            PhaseBadgeType.EXTENDED -> Color(0xFFFF9500).copy(alpha = 0.12f) // Orange 12%
        }
    }
}
