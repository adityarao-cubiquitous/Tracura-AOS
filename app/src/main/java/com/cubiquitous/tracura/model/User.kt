package com.cubiquitous.tracura.model

import com.google.firebase.firestore.PropertyName

data class User(
    @PropertyName("uid") val uid: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("email") val email: String = "",
    @PropertyName("phone") val phone: String = "",
    @PropertyName("role") val role: UserRole = UserRole.USER,
    @PropertyName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @PropertyName("isActive") val isActive: Boolean = true,
    @PropertyName("assignedProjects") val assignedProjects: List<String> = emptyList(),
    @PropertyName("deviceInfo") val deviceInfo: DeviceInfo = DeviceInfo(),
    @PropertyName("notificationPreferences") val notificationPreferences: NotificationPreferences = NotificationPreferences(),
    @PropertyName("customerId") val customerId: String? = null, // Customer ID (Business Head's UID) who created this user
    @PropertyName("department") val department: String = "" // Department name
)

enum class UserRole {
    USER,
    APPROVER,
    ADMIN,
    BUSINESS_HEAD,
    MANAGER;

    /** The string value stored in Firestore for this role. */
    val firestoreValue: String
        get() = when (this) {
            MANAGER -> "Manager"
            BUSINESS_HEAD -> "BUSINESS_HEAD"
            APPROVER -> "APPROVER"
            ADMIN -> "ADMIN"
            USER -> "USER"
        }

    companion object {
        fun fromFirestore(raw: String?): UserRole {
            val normalized = raw?.trim()?.replace("-", "_")?.replace(" ", "_")
            return when (normalized?.uppercase()) {
                "APPROVER" -> APPROVER
                "ADMIN" -> ADMIN
                "BUSINESS_HEAD", "BUSINESSHEAD" -> BUSINESS_HEAD
                "MANAGER" -> MANAGER
                "USER" -> USER
                else -> USER
            }
        }
    }
}