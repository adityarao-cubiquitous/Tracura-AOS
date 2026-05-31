package com.cubiquitous.tracura.model

data class AuthState(
    val isAuthenticated: Boolean = false,
    val user: User? = null,
    val selectedProjectId: String? = null,
    val selectedProject: Project? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) 