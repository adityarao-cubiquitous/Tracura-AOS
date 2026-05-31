package com.cubiquitous.tracura.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.Timestamp
import java.util.Date

@IgnoreExtraProperties
data class Customer(
	@DocumentId var id: String? = null,
	var name: String = "",
	var email: String = "",
	var phoneNumber: String? = null,
	var businessName: String = "",
	var businessType: String? = null,
	var location: String? = null,
	var departments: List<String> = emptyList(),
	var createdAt: Timestamp = Timestamp.now(),
	var updatedAt: Timestamp = Timestamp.now()
)


