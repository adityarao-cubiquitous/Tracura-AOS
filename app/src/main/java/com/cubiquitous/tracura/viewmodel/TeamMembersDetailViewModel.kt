package com.cubiquitous.tracura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.DeviceInfo
import com.cubiquitous.tracura.model.NotificationPreferences
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.repository.ExpenseRepository
import com.cubiquitous.tracura.repository.ProjectRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class TeamMembersDetailViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {
    
    private val _teamMembers = MutableStateFlow<List<User>>(emptyList())
    val teamMembers: StateFlow<List<User>> = _teamMembers.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Map of user phone to department name
    private val _departmentAssignments = MutableStateFlow<Map<String, String>>(emptyMap())
    val departmentAssignments: StateFlow<Map<String, String>> = _departmentAssignments.asStateFlow()
    
    private var currentProject: Project? = null
    private var lastUserRole: UserRole? = null
    private var lastCurrentUserPhone: String? = null

    /**
     * Resolves team member phone numbers from [project] based on role.
     * - BUSINESS_HEAD or null: all unique user phones from departmentUserAssignments.
     * - APPROVER: only user phones from departments assigned to [currentUserPhone].
     * Falls back to project.teamMembers if departmentUserAssignments is empty.
     */
    private fun getMemberPhonesFromProject(
        project: Project,
        userRole: UserRole?,
        currentUserPhone: String?
    ): List<String> {
        val userAssignments = project.departmentUserAssignments   // deptName -> userPhone
        val approverAssignments = project.departmentApproverAssignments // deptName -> approverPhone
        val temporaryApproverAssignments = project.departmentTemporaryApprover

        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time

        fun isTempAssignmentActive(entry: com.cubiquitous.tracura.model.DepartmentTemporaryApproverEntry): Boolean {
            if (!entry.isAccepted || !entry.isActive) return false
            val start = entry.startDate
            val end = entry.endDate
            if (start == null || end == null) return false
            return !today.before(start) && !today.after(end)
        }

        // Collect all phones from both maps independently (same dept can have both a user AND an approver)
        val allPhones = (
            userAssignments.values +
                approverAssignments.values +
                temporaryApproverAssignments.filter { isTempAssignmentActive(it) }.map { it.phone }
            ).distinct()

        if (userAssignments.isEmpty() && approverAssignments.isEmpty()) {
            return project.teamMembers.distinct()
        }

        val normalizedCurrent = currentUserPhone?.replace("+91", "")?.trim() ?: ""
        if (userRole == UserRole.APPROVER && normalizedCurrent.isNotEmpty()) {
            // APPROVER: show members from departments where current approver is
            // either permanent approver OR active temporary approver.
            val assignedDeptNames = approverAssignments
                .filter { (_, phone) -> phone.replace("+91", "").trim() == normalizedCurrent }
                .keys
                .toMutableSet()

            temporaryApproverAssignments
                .filter { entry ->
                    entry.phone.replace("+91", "").trim() == normalizedCurrent &&
                        isTempAssignmentActive(entry) &&
                        entry.departmentName.isNotBlank()
                }
                .forEach { entry ->
                    assignedDeptNames.add(entry.departmentName)
                }

            if (assignedDeptNames.isEmpty()) return emptyList()

            fun departmentMatchesAssigned(deptName: String): Boolean =
                assignedDeptNames.any { assigned ->
                    deptName.equals(assigned, ignoreCase = true) ||
                        deptName.trim().endsWith("_$assigned", ignoreCase = true) ||
                        deptName.trim().substringAfterLast("_", deptName).equals(assigned, ignoreCase = true)
                }
            // Collect matching phones from both maps independently
            val matchingPhones = mutableListOf<String>()
            userAssignments.filter { (dept, _) -> departmentMatchesAssigned(dept) }.values.forEach { matchingPhones.add(it) }
            approverAssignments.filter { (dept, _) -> departmentMatchesAssigned(dept) }.values.forEach { matchingPhones.add(it) }
            temporaryApproverAssignments
                .filter { isTempAssignmentActive(it) && departmentMatchesAssigned(it.departmentName) }
                .map { it.phone }
                .forEach { matchingPhones.add(it) }

            return matchingPhones.distinct()
        }
        // BUSINESS_HEAD (or no role): all users + all approvers
        return allPhones
    }
    
    suspend fun loadTeamMembers(project: Project) {
        loadTeamMembers(project, userRole = null, currentUserPhone = null)
    }

    suspend fun loadTeamMembers(
        project: Project,
        userRole: UserRole?,
        currentUserPhone: String?
    ) {
        _isLoading.value = true
        _errorMessage.value = null
        currentProject = project
        lastUserRole = userRole
        lastCurrentUserPhone = currentUserPhone
        
        try {
            val memberIds = getMemberPhonesFromProject(project, userRole, currentUserPhone)
            val db = FirebaseFirestore.getInstance()
            
            // Build department assignments map (phone -> department name)
            // Include both user and approver assignments so all members show their department
            val deptAssignments = mutableMapOf<String, String>()
            project.departmentUserAssignments.forEach { (deptName, userPhone) ->
                val normalizedPhone = userPhone.replace("+91", "").trim()
                deptAssignments[normalizedPhone] = deptName
            }
            project.departmentApproverAssignments.forEach { (deptName, approverPhone) ->
                val normalizedPhone = approverPhone.replace("+91", "").trim()
                // Don't overwrite if already set by user assignment
                if (!deptAssignments.containsKey(normalizedPhone)) {
                    deptAssignments[normalizedPhone] = deptName
                }
            }
            project.departmentTemporaryApprover.forEach { entry ->
                val normalizedPhone = entry.phone.replace("+91", "").trim()
                if (entry.departmentName.isNotBlank() && !deptAssignments.containsKey(normalizedPhone)) {
                    deptAssignments[normalizedPhone] = entry.departmentName
                }
            }
            _departmentAssignments.value = deptAssignments
            
            val loadedMembers = coroutineScope {
                memberIds.map { memberId: String ->
                    async { fetchUserDetails(memberId, db) }
                }.awaitAll().filterNotNull()
            }
            
            val sortedMembers = loadedMembers.sortedWith(
                compareBy<User> { it.role != UserRole.BUSINESS_HEAD }
                    .thenBy { it.name }
            )
            
            _teamMembers.value = sortedMembers
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load team members: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    suspend fun loadTeamMembersByProjectId(projectId: String) {
        loadTeamMembersByProjectId(projectId, userRole = null, currentUserPhone = null)
    }
    
    suspend fun loadTeamMembersByProjectId(
        projectId: String,
        userRole: UserRole?,
        currentUserPhone: String?
    ) {
        _isLoading.value = true
        _errorMessage.value = null
        
        try {
            val project = projectRepository.getProjectById(projectId)
            if (project == null) {
                _errorMessage.value = "Project not found"
                _teamMembers.value = emptyList()
                return
            }
            
            currentProject = project
            loadTeamMembers(project, userRole, currentUserPhone)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load team members: ${e.message}"
            _teamMembers.value = emptyList()
        } finally {
            _isLoading.value = false
        }
    }
    
    private suspend fun fetchUserDetails(userId: String, db: FirebaseFirestore): User? {
        return try {
            val document = db.collection("users")
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                val userData = document.data ?: return null
                
                // Parse role
                val roleString = userData["role"] as? String
                val userRole = when (roleString?.uppercase()?.replace(" ", "_")) {
                    "APPROVER" -> com.cubiquitous.tracura.model.UserRole.APPROVER
                    "ADMIN" -> com.cubiquitous.tracura.model.UserRole.ADMIN
                    "USER" -> com.cubiquitous.tracura.model.UserRole.USER
                    "BUSINESS_HEAD" -> com.cubiquitous.tracura.model.UserRole.BUSINESS_HEAD
                    else -> com.cubiquitous.tracura.model.UserRole.USER
                }
                
                // Parse device info
                val deviceInfoData = userData["deviceInfo"] as? Map<*, *>
                val deviceInfo = DeviceInfo(
                    fcmToken = deviceInfoData?.get("fcmToken") as? String ?: "",
                    deviceId = deviceInfoData?.get("deviceId") as? String ?: "",
                    deviceModel = deviceInfoData?.get("deviceModel") as? String ?: "",
                    osVersion = deviceInfoData?.get("osVersion") as? String ?: "",
                    appVersion = deviceInfoData?.get("appVersion") as? String ?: "",
                    lastLoginAt = deviceInfoData?.get("lastLoginAt") as? Long ?: System.currentTimeMillis(),
                    isOnline = deviceInfoData?.get("isOnline") as? Boolean ?: true
                )
                
                // Parse notification preferences
                val notificationPrefsData = userData["notificationPreferences"] as? Map<*, *>
                val notificationPreferences = NotificationPreferences(
                    pushNotifications = notificationPrefsData?.get("pushNotifications") as? Boolean ?: true,
                    expenseSubmitted = notificationPrefsData?.get("expenseSubmitted") as? Boolean ?: true,
                    expenseApproved = notificationPrefsData?.get("expenseApproved") as? Boolean ?: true,
                    expenseRejected = notificationPrefsData?.get("expenseRejected") as? Boolean ?: true,
                    projectAssignment = notificationPrefsData?.get("projectAssignment") as? Boolean ?: true,
                    pendingApprovals = notificationPrefsData?.get("pendingApprovals") as? Boolean ?: true
                )
                
                // Parse user data
                User(
                    uid = document.id,
                    name = userData["name"] as? String ?: "",
                    email = userData["email"] as? String ?: "",
                    phone = userData["phoneNumber"] as? String ?: userData["phone"] as? String ?: userId,
                    role = userRole,
                    createdAt = (userData["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time 
                        ?: (userData["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: System.currentTimeMillis(),
                    isActive = userData["isActive"] as? Boolean ?: true,
                    assignedProjects = (userData["assignedProjects"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    deviceInfo = deviceInfo,
                    notificationPreferences = notificationPreferences,
                    customerId = userData["customerId"] as? String ?: userData["ownerID"] as? String
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun refreshData() {
        currentProject?.let { project ->
            viewModelScope.launch {
                loadTeamMembers(project, lastUserRole, lastCurrentUserPhone)
            }
        }
    }
    
    fun updateTeamMembers(members: List<User>) {
        _teamMembers.value = members
    }
}
