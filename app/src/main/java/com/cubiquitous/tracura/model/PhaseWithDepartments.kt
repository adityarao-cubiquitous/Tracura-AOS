package com.cubiquitous.tracura.model

/**
 * Data class to hold Phase with its departments fetched from subcollection
 */
data class PhaseWithDepartments(
    val phase: Phase,
    val departments: List<Department> = emptyList()
)

