package com.cubiquitous.tracura.model

import java.util.Date

/**
 * Data class to hold draft project data for autosave functionality
 */
data class ProjectDraft(
    val id: String = java.util.UUID.randomUUID().toString(), // Unique ID for each draft
    val projectId: String? = null, // ID of the project if this draft is for an existing project
    val projectName: String = "",
    val description: String = "",
    val client: String = "",
    val clientPrimaryNumber: String = "",
    val clientSecondaryNumber: String = "",
    val location: String = "",
    val currency: String = "INR",
    val plannedStartDate: Long? = null, // Stored as timestamp
    val startDate: Long? = null, // Stored as timestamp
    val endDate: Long? = null, // Stored as timestamp
    val handoverDate: Long? = null, // Stored as timestamp
    val maintenanceDate: Long? = null, // Stored as timestamp
    val managerUid: String? = null, // Store Account Manager UID to reconstruct User object
    val approverUid: String? = null, // Store UID to reconstruct User object
    val teamMemberUids: List<String> = emptyList(), // Store UIDs to reconstruct User objects
    val phases: List<PhaseDraftSerializable> = emptyList(),
    val projectCategories: List<String> = emptyList(),
    val lastSaved: Long = System.currentTimeMillis() // Track when draft was last saved
)

/**
 * Serializable version of PhaseDraft for storage
 */
data class PhaseDraftSerializable(
    val phaseName: String = "",
    val startDate: Long? = null, // Stored as timestamp
    val endDate: Long? = null, // Stored as timestamp
    val departments: List<DepartmentBudget> = emptyList(),
    val categories: List<String> = emptyList()
)

