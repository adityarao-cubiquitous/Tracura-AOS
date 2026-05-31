package com.cubiquitous.tracura.model

data class CustomerSelectionItem(
    val customerId: String,
    val businessName: String,
    val businessType: String,
    val role: UserRole,
)
