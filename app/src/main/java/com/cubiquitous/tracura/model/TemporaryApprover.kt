package com.cubiquitous.tracura.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.Timestamp
import java.util.Date

data class TemporaryApprover(
    @PropertyName("id") val id: String = "",
    @PropertyName("projectId") val projectId: String = "",
    @PropertyName("approverId") val approverId: String = "",
    @PropertyName("approverName") val approverName: String = "",
    @PropertyName("approverPhone") val approverPhone: String = "",
    @PropertyName("assignedDate") val assignedDate: Timestamp = Timestamp.now(),
    @PropertyName("startdate") val startDate: Timestamp = Timestamp.now(),
    @PropertyName("expiringDate") val expiringDate: Timestamp? = null,
    @PropertyName("isActive") val isActive: Boolean = true,
    @PropertyName("assignedBy") val assignedBy: String = "", // Production head who assigned
    @PropertyName("assignedByName") val assignedByName: String = "",
    @PropertyName("createdAt") val createdAt: Timestamp = Timestamp.now(),
    @PropertyName("updatedAt") val updatedAt: Timestamp = Timestamp.now(),
    @PropertyName("isAccepted") val isAccepted: Boolean? = null, // null = pending, true = accepted, false = rejected
    @PropertyName("acceptedAt") val acceptedAt: Timestamp? = null,
    @PropertyName("rejectedAt") val rejectedAt: Timestamp? = null,
    @PropertyName("responseMessage") val responseMessage: String = "", // Optional message from approver
    @PropertyName("status") val status: String = "PENDING" // PENDING, ACCEPTED, REJECTED
)

// Helper function to check if temporary approver is expired
fun TemporaryApprover.isExpired(): Boolean {
    val now = Date()
    return expiringDate?.toDate()?.before(now) == true
}

// Helper function to get remaining days
fun TemporaryApprover.getRemainingDays(): Long {
    val now = Date()
    val expiryDate = expiringDate?.toDate() ?: return 0L
    val diffInMillis = expiryDate.time - now.time
    return diffInMillis / (1000 * 60 * 60 * 24) // Convert to days
}


