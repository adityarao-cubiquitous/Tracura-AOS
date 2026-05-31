package com.cubiquitous.tracura.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp

object FormatUtils {
    
    // Currency formatting - replaces all formatCurrency duplicates
    fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
    }
    
    // Simple currency formatting for exports
    fun formatCurrencySimple(amount: Double): String {
        return "₹${String.format("%.2f", amount)}"
    }
    
    // Currency formatting without decimals
    fun formatCurrencyWithoutDecimals(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        formatter.maximumFractionDigits = 2
        return formatter.format(amount)
    }
    
    // Date formatting utilities - handles both Timestamp? and String? (dd/MM/yyyy format)
    fun formatDate(timestamp: Timestamp?, pattern: String = "dd/MM/yyyy"): String {
        return timestamp?.let {
            SimpleDateFormat(pattern, Locale.getDefault()).format(it.toDate())
        } ?: "N/A"
    }
    
    fun formatDate(dateString: String?, pattern: String = "dd/MM/yyyy"): String {
        if (dateString == null) return "N/A"
        return try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat(pattern, Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString // Return as-is if parsing fails
        }
    }
    
    fun formatDateShort(timestamp: Timestamp?): String {
        return formatDate(timestamp, "dd/MM")
    }
    
    fun formatDateShort(dateString: String?): String {
        return formatDate(dateString, "dd/MM")
    }
    
    fun formatDateLong(timestamp: Timestamp?): String {
        return formatDate(timestamp, "dd MMM yyyy")
    }
    
    fun formatDateLong(dateString: String?): String {
        return formatDate(dateString, "dd MMM yyyy")
    }
    
    fun formatDateTime(timestamp: Timestamp?): String {
        return formatDate(timestamp, "dd/MM/yyyy HH:mm")
    }
    
    // Helper to parse String date to Timestamp
    fun parseStringDateToTimestamp(dateString: String?): Timestamp? {
        if (dateString == null) return null
        return try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { Timestamp(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    // Helper to convert String date to milliseconds for calculateDaysLeft
    fun parseStringDateToMillis(dateString: String?): Long? {
        if (dateString == null) return null
        return try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.time
        } catch (e: Exception) {
            null
        }
    }
    
    // Current timestamp for file naming
    fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
    
    // Calculate days between dates
    fun calculateDaysLeft(endDate: Long): Long {
        val calToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            timeInMillis = endDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffInMillis = calEnd.timeInMillis - calToday.timeInMillis
        return diffInMillis / (1000 * 60 * 60 * 24)
    }
    
    // Calculate days left from String date (dd/MM/yyyy format)
    fun calculateDaysLeft(endDate: String?): Long {
        val endDateMillis = parseStringDateToMillis(endDate) ?: return 0L
        return calculateDaysLeft(endDateMillis)
    }
    
    // Project initials generator
    fun getProjectInitials(projectName: String): String {
        return projectName
            .split(" ")
            .take(2)
            .map { it.firstOrNull()?.uppercaseChar() ?: "" }
            .joinToString("")
            .ifEmpty { projectName.take(2).uppercase() }
    }
    
    // Format time ago for notifications
    /**
     * Creates department key in format: "{phaseId}_{departmentName}"
     * Ensures department uniqueness across phases.
     * 
     * @param phaseId The phase ID
     * @param departmentName The department name (may or may not have prefix)
     * @return Formatted department key: "{phaseId}_{departmentName}"
     */
    fun departmentKey(phaseId: String, departmentName: String): String {
        return if (phaseId.isNotBlank()) {
            // Check if departmentName already has phaseId prefix
            if (departmentName.startsWith("${phaseId}_")) {
                departmentName // Already has prefix
            } else {
                // Add phaseId prefix to ensure uniqueness
                "${phaseId}_$departmentName"
            }
        } else {
            // Fallback to departmentName if no phaseId (shouldn't happen)
            departmentName
        }
    }
    
    /**
     * Extract display name from department name.
     * If department name is in format "phaseDocumentId_enteredDepartmentName",
     * returns "enteredDepartmentName". Otherwise returns the name as-is.
     * This is used only for display purposes - backend operations should use the full format.
     * 
     * Handles both old format (just department name) and new format (phaseId_departmentName)
     */
    fun getDepartmentDisplayName(departmentName: String): String {
        return if (departmentName.contains("_")) {
            // Extract everything after the first underscore
            departmentName.substringAfter("_")
        } else {
            // Return as-is for backward compatibility (old format without prefix)
            departmentName
        }
    }
    
    /**
     * Extension function to get display department name
     * Handles both old format (just department name) and new format (phaseId_departmentName)
     */
    fun String.displayDepartmentName(): String {
        return getDepartmentDisplayName(this)
    }
    
    /**
     * Checks if a department key matches a given department name
     * Handles both old format (just department name) and new format (phaseId_departmentName)
     */
    fun String.matchesDepartmentName(departmentName: String): Boolean {
        val displayName = this.displayDepartmentName()
        return displayName == departmentName || this == departmentName
    }
    
    fun formatTimeAgo(timestamp: Timestamp?): String {
        if (timestamp == null) return "Unknown"
        
        val now = Calendar.getInstance()
        val notificationDate = timestamp.toDate()
        val notificationCalendar = Calendar.getInstance().apply { time = notificationDate }
        val nowMillis = now.timeInMillis
        val timestampMillis = notificationDate.time
        val diffInMillis = nowMillis - timestampMillis
        
        // Check if notification is from today
        val isToday = now.get(Calendar.YEAR) == notificationCalendar.get(Calendar.YEAR) &&
                      now.get(Calendar.DAY_OF_YEAR) == notificationCalendar.get(Calendar.DAY_OF_YEAR)
        
        return when {
            // For today's notifications, show actual time instead of "Just now"
            isToday && diffInMillis < 60_000 -> {
                // Show actual time for very recent notifications (less than 1 minute)
                //SimpleDateFormat("hh:mm a", Locale.getDefault()).format(notificationDate)
                "Just now"
            }
            isToday && diffInMillis < 3_600_000 -> {
                // Show minutes ago for today's notifications less than 1 hour old
                "${diffInMillis / 60_000}m ago"
            }
            isToday -> {
                // Show actual time for today's notifications older than 1 hour
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(notificationDate)
            }
            diffInMillis < 86_400_000 -> "${diffInMillis / 3_600_000}h ago"
            diffInMillis < 604_800_000 -> "${diffInMillis / 86_400_000}d ago"
            else -> formatDateShort(timestamp)
        }
    }
    
    // Format timestamp for message documents: "26 November 2025 at 16:19:02 UTC+5:30" (no quotes)
    fun formatMessageTimestamp(timestamp: Timestamp): String {
        val date = timestamp.toDate()
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val monthName = monthNames[calendar.get(Calendar.MONTH)]
        val year = calendar.get(Calendar.YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        
        // Get timezone offset
        val timeZone = calendar.timeZone
        val offsetInMillis = timeZone.getOffset(date.time)
        val offsetHours = offsetInMillis / (1000 * 60 * 60)
        val offsetMinutes = Math.abs(offsetInMillis / (1000 * 60)) % 60
        val offsetSign = if (offsetInMillis >= 0) "+" else "-"
        val offsetString = String.format("%s%02d:%02d", offsetSign, Math.abs(offsetHours), offsetMinutes)
        
        // Return timestamp without quotes, ensure single colons only
        return String.format(
            "%d %s %d at %02d:%02d:%02d UTC%s",
            dayOfMonth, monthName, year, hour, minute, second, offsetString
        )
    }
    
    // Parse timestamp string format "26 November 2025 at 16:19:02 UTC+5:30" back to Date
    // Handles timestamps with or without quotes, removes quotes if present
    fun parseMessageTimestamp(timestampString: String): Date? {
        if (timestampString.isBlank()) return null
        
        // Remove quotes if present (handles both single and double quotes)
        val cleanedTimestamp = timestampString.trim().removeSurrounding("\"").removeSurrounding("'").trim()
        
        return try {
            // Pattern: "26 November 2025 at 16:19:02 UTC+5:30" or "26 November 2025 at 16:19:02 UTC+05:30"
            val pattern = "d MMMM yyyy 'at' HH:mm:ss 'UTC'z"
            val formatter = SimpleDateFormat(pattern, Locale.ENGLISH)
            formatter.parse(cleanedTimestamp)
        } catch (e: Exception) {
            // Try alternative parsing if the first fails
            try {
                // Handle without UTC prefix: "26 November 2025 at 16:19:02 +5:30"
                val pattern2 = "d MMMM yyyy 'at' HH:mm:ss z"
                val formatter2 = SimpleDateFormat(pattern2, Locale.ENGLISH)
                formatter2.parse(cleanedTimestamp)
            } catch (e2: Exception) {
                // Try with different UTC offset format (handles +5:30 or +05:30)
                try {
                    val pattern3 = "d MMMM yyyy 'at' HH:mm:ss 'UTC'XXX"
                    val formatter3 = SimpleDateFormat(pattern3, Locale.ENGLISH)
                    formatter3.parse(cleanedTimestamp)
                } catch (e3: Exception) {
                    null
                }
            }
        }
    }
} 