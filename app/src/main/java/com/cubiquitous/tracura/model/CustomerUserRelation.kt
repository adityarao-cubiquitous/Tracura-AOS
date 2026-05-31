package com.cubiquitous.tracura.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Represents the relationship between a Customer and a User
 * Stored in CustomerUserRelation collection
 */
data class CustomerUserRelation(
    @DocumentId
    val id: String? = null,
    
    @PropertyName("userID")
    val userID: String = "", // Phone number of the user
    
    @PropertyName("CustomerId")
    val CustomerId: String = "", // Customer (Business Head) UID
    
    @PropertyName("userCode")
    val userCode: String = "", // Generated user code
    
    @PropertyName("departmentName")
    val departmentName: String = "",
    
    @PropertyName("UserName")
    val UserName: String = "",
    
    @PropertyName("role")
    val role: String = "", // USER or APPROVER as string
    
    @PropertyName("isActive")
    val isActive: Boolean = true,
    
    @PropertyName("createdAt")
    val createdAt: Timestamp = Timestamp.now(),
    
    @PropertyName("updatedAt")
    val updatedAt: Timestamp = Timestamp.now()
)
