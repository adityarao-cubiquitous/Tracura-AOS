package com.cubiquitous.tracura.ui.view.businesshead

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.DepartmentBudget
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.model.TemporaryApprover
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.viewmodel.BusinessHeadViewModel
import com.cubiquitous.tracura.viewmodel.TemporaryApproverViewModel
import com.cubiquitous.tracura.ui.view.businesshead.MemberExpensesView
import com.cubiquitous.tracura.ui.view.businesshead.TeamMembersDetailScreen
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePickerDefaults

data class IosEditPalette(
    val tier1Background: Color,
    val tier2Surface: Color,
    val tier3Field: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val hairline: Color,
    val success: Color,
    val warning: Color,
    val danger: Color
)

@Composable
private fun rememberIosEditPalette(accentColor: Color = MaterialTheme.colorScheme.primary): IosEditPalette {
    val isDark = isSystemInDarkTheme()
    return remember(isDark, accentColor) {
        IosEditPalette(
            tier1Background = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7),
            tier2Surface = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
            tier3Field = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF),
            textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
            textSecondary = if (isDark) Color(0x99EBEBF5) else Color(0x993C3C43),
            accent = accentColor,
            onAccent = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF),
            hairline = if (isDark) Color(0xFF38383A) else Color(0x1F000000),
            success = Color(0xFF34C759),
            warning = Color(0xFFFF9F0A),
            danger = Color(0xFFFF453A)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProjectScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    onProjectDeleted: () -> Unit = onNavigateBack,
    viewModel: BusinessHeadViewModel = hiltViewModel(),
    tempApproverViewModel: TemporaryApproverViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val iosPalette = rememberIosEditPalette()
    val authState by authViewModel.authState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val editingProject by viewModel.editingProject.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    
    // Track if we've attempted to load the project
    var hasAttemptedLoad by remember { mutableStateOf(false) }
    
    val availableUsers by viewModel.availableUsers.collectAsState()
    val availableApprovers by viewModel.availableApprovers.collectAsState()
    val departmentBudgets by viewModel.departmentBudgets.collectAsState()
    val totalBudget by viewModel.totalBudget.collectAsState()
    val totalAllocated by viewModel.totalAllocated.collectAsState()
    
    // Temporary approvers
    val temporaryApprovers by tempApproverViewModel.temporaryApprovers.collectAsState()
    
    
    // Form state
    var projectName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var client by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }
    var selectedManagerId by remember { mutableStateOf("") }
    var selectedTeamMembers by remember { mutableStateOf(setOf<String>()) }
    var startDate by remember { mutableStateOf<Date?>(null) } // Planned Start Date
    var handoverDate by remember { mutableStateOf<Date?>(null) }
    var maintenanceDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var newDepartmentName by remember { mutableStateOf("") }
    var newDepartmentBudget by remember { mutableStateOf("") }
    
    // Date picker states
    var showPlannedStartDatePicker by remember { mutableStateOf(false) }
    var showHandoverDatePicker by remember { mutableStateOf(false) }
    var showMaintenanceDatePicker by remember { mutableStateOf(false) }
    val plannedStartDatePickerState = rememberDatePickerState()
    val handoverDatePickerState = rememberDatePickerState()
    val maintenanceDatePickerState = rememberDatePickerState()
    
    // Date validation errors
    var handoverDateError by remember { mutableStateOf<String?>(null) }
    var maintenanceDateError by remember { mutableStateOf<String?>(null) }
    
    // Edit states for date fields
    var isEditingPlannedStartDate by remember { mutableStateOf(false) }
    var isEditingHandoverDate by remember { mutableStateOf(false) }
    var isEditingMaintenanceDate by remember { mutableStateOf(false) }
    
    // Delete dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteMenu by remember { mutableStateOf(false) }
    var isDeletingProject by remember { mutableStateOf(false) }
    val canDeleteProject by viewModel.canDeleteProject.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Status change confirmation dialog state
    var showStatusChangeConfirmation by remember { mutableStateOf(false) }
    var pendingStatusChange by remember { mutableStateOf<String?>(null) }
    var originalStatus by remember { mutableStateOf<String?>(null) }
    var statusChangeMessage by remember { mutableStateOf("") }
    
    // Project status
    var projectStatus by remember { mutableStateOf("DRAFT") }
    var showStatusDropdown by remember { mutableStateOf(false) }
    val allStatusOptions = listOf("ACTIVE", "COMPLETED", "MAINTENANCE")
    // Filter status options based on current status:
    // - COMPLETED/MAINTENANCE: cannot change to ACTIVE
    // - LOCKED: can only change to ACTIVE (not MAINTENANCE or COMPLETED)
    val statusOptions = remember(projectStatus) {
        when (projectStatus.uppercase()) {
            "COMPLETED", "MAINTENANCE" -> allStatusOptions.filter { it != "ACTIVE" }
            "LOCKED" -> allStatusOptions.filter { it == "ACTIVE" }
            else -> allStatusOptions
        }
    }
    
    // Project Suspension states
    var enableSuspension by remember { mutableStateOf(false) }
    var suspendedUntil by remember { mutableStateOf<Date?>(null) }
    var selectedSuspensionReason by remember { mutableStateOf<String?>(null) }
    var showSuspensionReasonDropdown by remember { mutableStateOf(false) }
    var suspensionNotes by remember { mutableStateOf("") }
    var showSuspendedUntilDatePicker by remember { mutableStateOf(false) }
    var suspensionReasonError by remember { mutableStateOf<String?>(null) }
    var suspensionNotesError by remember { mutableStateOf<String?>(null) }
    val suspendedUntilDatePickerState = rememberDatePickerState()
    val suspensionReasons = listOf(
        "Payment Milestone Delay",
        "Site Access/Permit Hold",
        "Design Approval Pending",
        "Vendor/Material Shortage",
        "Safety Non-Compliance",
        "Regulatory Pending",
        "Resource Reallocation",
        "Weather/Force Majeure",
        "Other"
    )
    
    // Edit states for each field
    var isEditingProjectName by remember { mutableStateOf(false) }
    var isEditingDescription by remember { mutableStateOf(false) }
    var isEditingClient by remember { mutableStateOf(false) }
    var isEditingLocation by remember { mutableStateOf(false) }
    var isEditingTeam by remember { mutableStateOf(false) }
    
    // Search states for Team Management
    var projectManagerSearch by remember { mutableStateOf("") }
    var teamMembersSearch by remember { mutableStateOf("") }
    var showProjectManagerDropdown by remember { mutableStateOf(false) }
    var showTeamMembersDropdown by remember { mutableStateOf(false) }
    
    // View All Team Members sheet state
    var showViewAllTeamMembersSheet by remember { mutableStateOf(false) }
    var selectedMemberForDetail by remember { mutableStateOf<User?>(null) }
    
    // Add Team Member sheet state
    var showAddTeamMemberSheet by remember { mutableStateOf(false) }

    // Helpers for resilient single-field saves
    val dateFormatterForStorage = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    fun parseStoredDate(dateValue: String?): Date? {
        if (dateValue.isNullOrBlank()) return null
        return try {
            dateFormatterForStorage.parse(dateValue)
        } catch (e: Exception) {
            null
        }
    }

    fun resolveStartTimestamp(): Timestamp {
        val currentStart = startDate
        if (currentStart != null) return Timestamp(currentStart)
        val storedStart = editingProject?.startDate ?: editingProject?.plannedDate
        return parseStoredDate(storedStart)?.let { Timestamp(it) } ?: Timestamp.now()
    }

    fun resolveManagerId(): String? {
        val selected = selectedManagerId.trim()
        if (selected.isNotEmpty()) return selected
        return editingProject?.managerId?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun resolveTeamMemberIds(): List<String> {
        val selected = selectedTeamMembers.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (selected.isNotEmpty()) return selected
        return editingProject?.teamMembers
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?: emptyList()
    }
    
    // Check immediately if project is preloaded and handle loading
    LaunchedEffect(projectId, editingProject?.id) {
        // If project is already preloaded, mark as attempted immediately
        if (editingProject?.id == projectId) {
            hasAttemptedLoad = true
            android.util.Log.d("EditProjectScreen", "✅ Project already preloaded")
            tempApproverViewModel.loadTemporaryApprovers(projectId)
        } else if (!hasAttemptedLoad) {
            // Only load if we haven't attempted yet
            viewModel.loadProjectForEdit(projectId)
            tempApproverViewModel.loadTemporaryApprovers(projectId)
        }
    }
    
    // Mark as attempted when project loads or error occurs
    LaunchedEffect(editingProject?.id, error) {
        if (editingProject?.id == projectId) {
            hasAttemptedLoad = true
        } else if (error != null && editingProject == null && !hasAttemptedLoad) {
            hasAttemptedLoad = true
        }
    }
    
    // Update dates when project status changes (UI preview only - actual updates happen on save)
    // This LaunchedEffect is kept for UI preview but actual date updates follow guide logic (yesterday, etc.)
    LaunchedEffect(projectStatus) {
        // Note: Actual date updates with confirmation dialogs happen in the save button handler
        // This effect is kept minimal to avoid conflicts with the confirmation flow
    }
    
    // Update form when project is loaded - use project ID, suspension state, dates, and status as key to ensure updates
    // This runs immediately when project is preloaded
    LaunchedEffect(editingProject?.id, editingProject?.isSuspended, editingProject?.suspendedDate, editingProject?.suspensionReason, editingProject?.status, editingProject?.startDate, editingProject?.handoverDate, editingProject?.maintenanceDate, editingProject?.departmentUserAssignments, editingProject?.departmentApproverAssignments, authState.user?.role, authState.user?.phone) {
        editingProject?.let { project ->
            // Only populate if this is the project we're editing
            if (project.id == projectId) {
                try {
                    android.util.Log.d("EditProjectScreen", "Loading project for edit: ${project.name}, status: ${project.status}, maintenanceDate: ${project.maintenanceDate}")
            projectName = project.name
            description = project.description
            client = project.client
            location = project.location
            currency = project.currency.ifEmpty { "INR" }
            selectedManagerId = project.managerId ?: ""
            // Team members from departmentUserAssignments: BUSINESS_HEAD sees all; APPROVER sees only assigned departments
            selectedTeamMembers = if (project.departmentUserAssignments.isEmpty()) {
                project.teamMembers.toSet()
            } else {
                val userRole = authState.user?.role
                val userPhone = authState.user?.phone?.replace("+91", "")?.trim()
                val assignmentMembers = if (userRole == UserRole.APPROVER && !userPhone.isNullOrBlank()) {
                    val assignedDeptNames = project.departmentApproverAssignments
                        .filter { (_, phone) -> phone.replace("+91", "").trim() == userPhone }
                        .keys.toSet()
                    fun departmentMatchesAssigned(deptName: String): Boolean =
                        assignedDeptNames.any { assigned ->
                            deptName.equals(assigned, ignoreCase = true) ||
                                deptName.trim().endsWith("_$assigned", ignoreCase = true) ||
                                deptName.trim().substringAfterLast("_", deptName).equals(assigned, ignoreCase = true)
                        }
                    project.departmentUserAssignments.filter { (dept, _) -> departmentMatchesAssigned(dept) }.values.toSet()
                } else {
                    project.departmentUserAssignments.values.toSet()
                }
                (assignmentMembers + project.teamMembers).toSet()
            }
            projectStatus = project.status
            originalStatus = project.status
            
            project.startDate?.let { start ->
                try {
                val inputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    inputFormat.isLenient = false
                startDate = inputFormat.parse(start)
                startDate?.let {
                    plannedStartDatePickerState.selectedDateMillis = it.time
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EditProjectScreen", "Error parsing startDate: $start - ${e.message}", e)
                    startDate = null
                }
            }
            
            project.handoverDate?.let { handover ->
                try {
                val inputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    inputFormat.isLenient = false
                handoverDate = inputFormat.parse(handover)
                handoverDate?.let {
                    handoverDatePickerState.selectedDateMillis = it.time
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EditProjectScreen", "Error parsing handoverDate: $handover - ${e.message}", e)
                    handoverDate = null
                }
            }
            
            project.maintenanceDate?.let { maintenance ->
                try {
                val inputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    inputFormat.isLenient = false
                maintenanceDate = inputFormat.parse(maintenance)
                maintenanceDate?.let {
                    maintenanceDatePickerState.selectedDateMillis = it.time
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EditProjectScreen", "Error parsing maintenanceDate: $maintenance - ${e.message}", e)
                    maintenanceDate = null
                }
            }
            
            project.endDate?.let { end ->
                try {
                val inputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    inputFormat.isLenient = false
                endDate = inputFormat.parse(end)
                } catch (e: Exception) {
                    android.util.Log.e("EditProjectScreen", "Error parsing endDate: $end - ${e.message}", e)
                    endDate = null
                }
            }
            
            // Load suspension data - toggle should be enabled if isSuspended is true
            val wasSuspensionEnabled = enableSuspension
            enableSuspension = project.isSuspended == true
            android.util.Log.d("EditProjectScreen", "🔄 Loading suspension state - isSuspended: ${project.isSuspended}, enableSuspension: $enableSuspension, wasEnabled: $wasSuspensionEnabled")
            
            if (enableSuspension) {
                // Load suspension date if present
                if (!project.suspendedDate.isNullOrBlank()) {
                try {
                    val inputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    inputFormat.isLenient = false
                    suspendedUntil = inputFormat.parse(project.suspendedDate)
                    suspendedUntil?.let {
                        suspendedUntilDatePickerState.selectedDateMillis = it.time
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EditProjectScreen", "Error parsing suspendedDate: ${project.suspendedDate} - ${e.message}", e)
                        suspendedUntil = null
                    }
                } else {
                    suspendedUntil = null
                }
                
                // Load suspension reason if present
                if (!project.suspensionReason.isNullOrBlank()) {
                    val reason = project.suspensionReason
                    // Check if reason matches any of the predefined reasons
                    if (reason in suspensionReasons) {
                        selectedSuspensionReason = reason
                        suspensionNotes = ""
                    } else {
                        // It's a custom note (from "Other")
                        selectedSuspensionReason = "Other"
                        suspensionNotes = reason
                    }
                } else {
                    selectedSuspensionReason = null
                    suspensionNotes = ""
                }
            } else {
                suspendedUntil = null
                selectedSuspensionReason = null
                suspensionNotes = ""
            }
                } catch (e: Exception) {
                    android.util.Log.e("EditProjectScreen", "❌ Error loading project data: ${e.message}", e)
                    // Don't crash - just log the error
                }
            }
        }
    }
    
    // Handle success message
    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            if (message.contains("deleted", ignoreCase = true)) {
                isDeletingProject = false
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                onProjectDeleted()
            } else if (message.contains("updated", ignoreCase = true)) {
                // Show toast but don't navigate back - let user see the updated project
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                // Project will be reloaded by ViewModel, and LaunchedEffect will update the UI
            } else {
                onNavigateBack()
            }
        }
    }
    
    // Reset deletion flag on any error so the overlay clears
    LaunchedEffect(error) {
        if (error != null) isDeletingProject = false
    }

    // Show Member Expenses View if a member is selected (full screen)
//    selectedMemberForDetail?.let { member ->
//        val currentProject = editingProject
//        if (currentProject != null) {
//            MemberExpensesView(
//                member = member,
//                project = currentProject,
//                onBackClick = {
//                    selectedMemberForDetail = null
//                    showViewAllTeamMembersSheet = true
//                }
//            )
//            return
//        }
//    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Project Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary
                    )
                },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = iosPalette.accent
                            )
                        }
                        Text(
                            text = "Back",
                            color = iosPalette.accent,
                            fontSize = 14.sp
                        )
                    }
                },
                actions = {
                    // Show delete menu only for Draft status projects
                    if (true) {
                        Box {
                            IconButton(onClick = { showDeleteMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = iosPalette.accent
                                )
                            }
                            DropdownMenu(
                                expanded = showDeleteMenu,
                                onDismissRequest = { showDeleteMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = iosPalette.danger,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                "Delete Project",
                                                color = iosPalette.danger
                                            )
                                        }
                                    },
                                    onClick = {
                                        showDeleteMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = iosPalette.tier2Surface
                )
            )
        },
        containerColor = iosPalette.tier1Background
    ) { paddingValues ->
        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text(
                        text = "Delete Project",
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete this project? This action cannot be undone.",
                        color = iosPalette.textPrimary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            isDeletingProject = true
                            viewModel.deleteProject(projectId)
                        }
                    ) {
                        Text(
                            text = "Delete",
                            color = iosPalette.danger,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text(
                            text = "Cancel",
                            color = iosPalette.textSecondary
                        )
                    }
                },
                containerColor = iosPalette.tier2Surface,
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        // Show loading only if we're actively loading project AND it's not already preloaded
        // Don't show loading if project is already available, even if users are still loading
        if (isLoading && editingProject?.id != projectId && !hasAttemptedLoad) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = iosPalette.accent)
            }
        } else if (editingProject?.id == projectId) {
            val selectedManager = availableApprovers.find { it.phone == selectedManagerId }
            val selectedTeamMembersList = availableUsers.filter { it.phone in selectedTeamMembers }
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(iosPalette.tier1Background),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error message
                error?.let { errorMsg ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = iosPalette.danger.copy(alpha = 0.14f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = errorMsg,
                                color = iosPalette.danger,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                
                // Rejection Reason Display (if project is REVIEW REJECTED)
                editingProject?.let { project ->
                    if (project.status == "REVIEW REJECTED" && !project.rejectionReason.isNullOrBlank()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = iosPalette.danger.copy(alpha = 0.14f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 2.dp),
                                border = BorderStroke(0.75.dp, iosPalette.hairline)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = iosPalette.danger,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Rejection Reason",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = iosPalette.danger
                                        )
                                    }
                                    Text(
                                        text = project.rejectionReason ?: "",
                                        fontSize = 14.sp,
                                        color = iosPalette.textPrimary,
                                        lineHeight = 20.sp
                                    )
                                    Text(
                                        text = "Please review and update the project details, then resubmit for approval.",
                                        fontSize = 12.sp,
                                        color = iosPalette.textSecondary,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Header Section with Project Administration
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Large blue folder icon
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = iosPalette.accent,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Project Administration",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosPalette.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Manage project settings, team members, and budget allocations.",
                            fontSize = 14.sp,
                            color = iosPalette.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                // Project Name Card
                item {
                    ProjectFieldCard(
                        icon = Icons.Default.Title,
                        iconColor = iosPalette.accent,
                        iosPalette = iosPalette,
                        title = "Project Name",
                        content = projectName,
                        isEditing = isEditingProjectName,
                        onEditClick = { isEditingProjectName = !isEditingProjectName },
                        onSave = {
                            isEditingProjectName = false
                            // Save logic will be handled elsewhere
                        },
                        editContent = {
                            OutlinedTextField(
                                value = projectName,
                                onValueChange = { projectName = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = iosPalette.accent,
                                    unfocusedBorderColor = iosPalette.hairline,
                                    focusedContainerColor = iosPalette.tier3Field,
                                    unfocusedContainerColor = iosPalette.tier3Field,
                                    focusedTextColor = iosPalette.textPrimary,
                                    unfocusedTextColor = iosPalette.textPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        },
                        displayContent = {
                            Text(
                                text = projectName.ifEmpty { "No project name" },
                                fontSize = 16.sp,
                                color = iosPalette.textPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                }
                
                // Description Card
                item {
                    ProjectFieldCard(
                        icon = Icons.Default.Description,
                        iconColor = iosPalette.accent,
                        iosPalette = iosPalette,
                        title = "Description",
                        content = description,
                        isEditing = isEditingDescription,
                        onEditClick = { isEditingDescription = !isEditingDescription },
                        onSave = {
                            isEditingDescription = false
                        },
                        editContent = {
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = iosPalette.accent,
                                    unfocusedBorderColor = iosPalette.hairline,
                                    focusedContainerColor = iosPalette.tier3Field,
                                    unfocusedContainerColor = iosPalette.tier3Field,
                                    focusedTextColor = iosPalette.textPrimary,
                                    unfocusedTextColor = iosPalette.textPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        },
                        displayContent = {
                            Text(
                                text = description.ifEmpty { "No description" },
                                fontSize = 16.sp,
                                color = iosPalette.textPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                }
                
                // Client Name Card
                item {
                    ProjectFieldCard(
                        icon = Icons.Default.Business,
                        iconColor = iosPalette.success,
                        iosPalette = iosPalette,
                        title = "Client Name",
                        content = client,
                        isEditing = isEditingClient,
                        onEditClick = { isEditingClient = !isEditingClient },
                        onSave = {
                            isEditingClient = false
                        },
                        editContent = {
                            OutlinedTextField(
                                value = client,
                                onValueChange = { client = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = iosPalette.accent,
                                    unfocusedBorderColor = iosPalette.hairline,
                                    focusedContainerColor = iosPalette.tier3Field,
                                    unfocusedContainerColor = iosPalette.tier3Field,
                                    focusedTextColor = iosPalette.textPrimary,
                                    unfocusedTextColor = iosPalette.textPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        },
                        displayContent = {
                            Text(
                                text = client.ifEmpty { "No client name" },
                                fontSize = 16.sp,
                                color = iosPalette.textPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                }
                
                // Location Card
                item {
                    ProjectFieldCard(
                        icon = Icons.Default.LocationOn,
                        iconColor = iosPalette.accent,
                        iosPalette = iosPalette,
                        title = "Location",
                        content = location,
                        isEditing = isEditingLocation,
                        onEditClick = { isEditingLocation = !isEditingLocation },
                        onSave = {
                            isEditingLocation = false
                        },
                        editContent = {
                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = iosPalette.accent,
                                    unfocusedBorderColor = iosPalette.hairline,
                                    focusedContainerColor = iosPalette.tier3Field,
                                    unfocusedContainerColor = iosPalette.tier3Field,
                                    focusedTextColor = iosPalette.textPrimary,
                                    unfocusedTextColor = iosPalette.textPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        },
                        displayContent = {
                            Text(
                                text = location.ifEmpty { "No location" },
                                fontSize = 16.sp,
                                color = iosPalette.textPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                }
                
                // Planned Start Date Card
                item {
                    DateFieldCard(
                        icon = Icons.Default.DateRange,
                        iconColor = iosPalette.accent,
                        iosPalette = iosPalette,
                        title = "Planned Start Date",
                        date = startDate,
                        isEditing = isEditingPlannedStartDate,
                        onEditClick = { isEditingPlannedStartDate = !isEditingPlannedStartDate },
                        onDateClick = { showPlannedStartDatePicker = true },
                        onSave = {
                            isEditingPlannedStartDate = false
                        },
                        minDate = null, // No minimum for planned start date
                        errorMessage = null
                    )
                }
                
                // Handover Date Card
                item {
                    DateFieldCard(
                        icon = Icons.Default.CheckCircle,
                        iconColor = iosPalette.accent,
                        iosPalette = iosPalette,
                        title = "Handover Date",
                        date = handoverDate,
                        isEditing = isEditingHandoverDate,
                        onEditClick = { isEditingHandoverDate = !isEditingHandoverDate },
                        onDateClick = { showHandoverDatePicker = true },
                        onSave = {
                            // Validate handover date >= planned start date
                            val currentStartDate = startDate
                            if (currentStartDate != null && handoverDate != null) {
                                val calendarHandover = java.util.Calendar.getInstance().apply {
                                    time = handoverDate!!
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }.time
                                
                                val calendarPlanned = java.util.Calendar.getInstance().apply {
                                    time = currentStartDate
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }.time
                                
                                if (calendarHandover.time < calendarPlanned.time) {
                                    handoverDateError = "Handover date must be on or after Planned Start Date"
                                    return@DateFieldCard
                                }
                            }
                            handoverDateError = null
                            isEditingHandoverDate = false
                        },
                        minDate = startDate,
                        errorMessage = handoverDateError
                    )
                }
                
                // Maintenance Date Card
                item {
                    val maintenanceInfoText = remember(handoverDate, maintenanceDate) {
                        if (handoverDate != null && maintenanceDate != null) {
                            val calendar = java.util.Calendar.getInstance()
                            calendar.time = handoverDate!!
                            val handoverYear = calendar.get(java.util.Calendar.YEAR)
                            val handoverMonth = calendar.get(java.util.Calendar.MONTH)
                            
                            calendar.time = maintenanceDate!!
                            val maintenanceYear = calendar.get(java.util.Calendar.YEAR)
                            val maintenanceMonth = calendar.get(java.util.Calendar.MONTH)
                            
                            val monthsDiff = (maintenanceYear - handoverYear) * 12 + (maintenanceMonth - handoverMonth)
                            if (monthsDiff > 0) {
                                "$monthsDiff ${if (monthsDiff == 1) "month" else "months"} from handover"
                            } else null
                        } else null
                    }
                    
                    DateFieldCard(
                        icon = Icons.Default.Build,
                        iconColor = iosPalette.warning,
                        iosPalette = iosPalette,
                        title = "Maintenance Date",
                        date = maintenanceDate,
                        isEditing = isEditingMaintenanceDate,
                        onEditClick = { isEditingMaintenanceDate = !isEditingMaintenanceDate },
                        onDateClick = { showMaintenanceDatePicker = true },
                        onSave = {
                            // Validate maintenance date >= handover date (or planned start date if no handover)
                            val minDateForMaintenance = handoverDate ?: startDate
                            if (minDateForMaintenance != null && maintenanceDate != null) {
                                val calendarMaintenance = java.util.Calendar.getInstance().apply {
                                    time = maintenanceDate!!
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }.time
                                
                                val calendarMin = java.util.Calendar.getInstance().apply {
                                    time = minDateForMaintenance
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }.time
                                
                                if (calendarMaintenance.time < calendarMin.time) {
                                    val minDateName = if (handoverDate != null) "Handover Date" else "Planned Start Date"
                                    maintenanceDateError = "Maintenance date must be on or after $minDateName"
                                    return@DateFieldCard
                                }
                            }
                            maintenanceDateError = null
                            isEditingMaintenanceDate = false
                        },
                        minDate = handoverDate ?: startDate,
                        errorMessage = maintenanceDateError,
                        infoText = maintenanceInfoText
                    )
                }
                
                // Project Status Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = iosPalette.warning,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Project Status",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = iosPalette.textPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Box {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (enableSuspension) {
                                                Modifier
                                            } else {
                                                Modifier.clickable { showStatusDropdown = true }
                                            }
                                        ),
                                    color = if (enableSuspension) iosPalette.hairline.copy(alpha = 0.25f) else iosPalette.tier3Field,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = projectStatus,
                                            fontSize = 16.sp,
                                            color = if (enableSuspension) iosPalette.textSecondary else iosPalette.accent,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = iosPalette.textSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = showStatusDropdown && !enableSuspension,
                                    onDismissRequest = { showStatusDropdown = false },
                                    modifier = Modifier
                                        .background(iosPalette.tier2Surface, RoundedCornerShape(8.dp))
                                        .heightIn(max = 300.dp)
                                ) {
                                    statusOptions.forEach { status ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = status,
                                                        fontSize = 16.sp,
                                                        color = iosPalette.textPrimary,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    if (status == projectStatus) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = iosPalette.accent,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                projectStatus = status
                                                showStatusDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Project Suspension Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = null,
                                        tint = iosPalette.danger,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Project Suspension",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = iosPalette.textPrimary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = iosPalette.warning,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Enable Suspension toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Enable Suspension",
                                    fontSize = 16.sp,
                                    color = iosPalette.textPrimary
                                )
                                Switch(
                                    checked = enableSuspension,
                                    onCheckedChange = { enableSuspension = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = iosPalette.onAccent,
                                        checkedTrackColor = iosPalette.success,
                                        uncheckedThumbColor = iosPalette.onAccent,
                                        uncheckedTrackColor = iosPalette.hairline
                                    )
                                )
                            }
                            
                            if (enableSuspension) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Suspended Until date field
                                Column {
                                    Text(
                                        text = "Suspended Until",
                                        fontSize = 14.sp,
                                        color = iosPalette.textSecondary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showSuspendedUntilDatePicker = true },
                                        color = iosPalette.tier3Field,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = suspendedUntil?.let {
                                                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
                                                } ?: "Select date",
                                                fontSize = 16.sp,
                                                color = if (suspendedUntil != null) iosPalette.textPrimary else iosPalette.textSecondary
                                            )
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = iosPalette.textSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Reason for Suspension dropdown
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Reason for Suspension",
                                            fontSize = 14.sp,
                                            color = iosPalette.textSecondary,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        Text(
                                            text = " *",
                                            fontSize = 14.sp,
                                            color = iosPalette.danger,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }
                                    Box {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showSuspensionReasonDropdown = true },
                                            color = iosPalette.tier3Field,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = selectedSuspensionReason ?: "Select reason",
                                                    fontSize = 16.sp,
                                                    color = if (selectedSuspensionReason != null) iosPalette.textPrimary else iosPalette.textSecondary
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = iosPalette.textSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = showSuspensionReasonDropdown,
                                            onDismissRequest = { showSuspensionReasonDropdown = false },
                                            modifier = Modifier
                                                .background(iosPalette.tier2Surface, RoundedCornerShape(8.dp))
                                                .fillMaxWidth()
                                                .heightIn(max = 300.dp)
                                        ) {
                                            suspensionReasons.forEach { reason ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = reason,
                                                                fontSize = 16.sp,
                                                                color = iosPalette.textPrimary
                                                            )
                                                            if (reason == selectedSuspensionReason) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = iosPalette.accent,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedSuspensionReason = reason
                                                        showSuspensionReasonDropdown = false
                                                        suspensionReasonError = null
                                                        // Clear suspension notes if not "Other"
                                                        if (reason != "Other") {
                                                            suspensionNotes = ""
                                                            suspensionNotesError = null
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    if (suspensionReasonError != null) {
                                        Text(
                                            text = suspensionReasonError!!,
                                            fontSize = 12.sp,
                                            color = iosPalette.danger,
                                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                        )
                                    }
                                }
                                
                                // Suspension Notes field (only shown when "Other" is selected)
                                if (selectedSuspensionReason == "Other") {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Suspension Notes",
                                                fontSize = 14.sp,
                                                color = iosPalette.textSecondary,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                            Text(
                                                text = " *",
                                                fontSize = 14.sp,
                                                color = iosPalette.danger,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                        }
                                        OutlinedTextField(
                                            value = suspensionNotes,
                                            onValueChange = { 
                                                suspensionNotes = it
                                                if (it.isNotBlank()) {
                                                    suspensionNotesError = null
                                                } else if (enableSuspension && selectedSuspensionReason == "Other") {
                                                    suspensionNotesError = "Suspension notes are required"
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp),
                                            placeholder = {
                                                Text(
                                                    text = "Enter suspension notes (required)",
                                                    color = iosPalette.textSecondary
                                                )
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = if (suspensionNotesError != null) iosPalette.danger else iosPalette.accent,
                                                unfocusedBorderColor = if (suspensionNotesError != null) iosPalette.danger else iosPalette.hairline,
                                                errorBorderColor = iosPalette.danger,
                                                focusedContainerColor = iosPalette.tier3Field,
                                                unfocusedContainerColor = iosPalette.tier3Field,
                                                focusedTextColor = iosPalette.textPrimary,
                                                unfocusedTextColor = iosPalette.textPrimary,
                                                focusedPlaceholderColor = iosPalette.textSecondary,
                                                unfocusedPlaceholderColor = iosPalette.textSecondary
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            maxLines = 4,
                                            isError = suspensionNotesError != null
                                        )
                                        if (suspensionNotesError != null) {
                                            Text(
                                                text = suspensionNotesError!!,
                                                fontSize = 12.sp,
                                                color = iosPalette.danger,
                                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = "${suspensionNotes.length} characters",
                                            fontSize = 12.sp,
                                            color = iosPalette.textSecondary,
                                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Team Management Card
                item {
                    TeamManagementCard(
                        iosPalette = iosPalette,
                        isEditing = isEditingTeam,
                        onEditClick = { isEditingTeam = !isEditingTeam },
                        onClose = { isEditingTeam = false },
                        onSave = {
                            isEditingTeam = false
                        },
                        availableApprovers = availableApprovers,
                        availableUsers = availableUsers,
                        temporaryApprovers = temporaryApprovers,
                        selectedManagerId = selectedManagerId,
                        onManagerSelected = { selectedManagerId = it },
                        selectedTeamMembers = selectedTeamMembers,
                        onTeamMemberToggle = { phone ->
                            selectedTeamMembers = if (selectedTeamMembers.contains(phone)) {
                                selectedTeamMembers - phone
                            } else {
                                selectedTeamMembers + phone
                            }
                        },
                        onRemoveTeamMember = { phone ->
                            selectedTeamMembers = selectedTeamMembers - phone
                        },
                        onAddTempApprover = { approver ->
                            // Assign temp approver immediately with start now and no expiry
                            tempApproverViewModel.createTemporaryApprover(
                                projectId = projectId,
                                approverId = approver.phone,
                                approverName = approver.name,
                                approverPhone = approver.phone,
                                startDate = Timestamp.now(),
                                expiringDate = null
                            )
                        },
                        onViewAllTeamMembers = {
                            showViewAllTeamMembersSheet = true
                        }
                    )
                }

                // Save button at bottom
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Validate suspension reason if suspension is enabled
                            if (enableSuspension) {
                                if (suspendedUntil == null) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Please select a suspension date",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                
                                if (selectedSuspensionReason == null) {
                                    suspensionReasonError = "Reason is required"
                                    return@Button
                                } else {
                                    suspensionReasonError = null
                                }
                                
                                // Validate suspension notes if "Other" is selected
                                if (selectedSuspensionReason == "Other" && suspensionNotes.isBlank()) {
                                    suspensionNotesError = "Suspension notes are required"
                                    return@Button
                                } else {
                                    suspensionNotesError = null
                                }
                            }
                            
                            if (projectName.isNotBlank()) {
                                val startTimestamp = resolveStartTimestamp()
                                val endTimestamp = endDate?.let { Timestamp(it) }
                                
                                // Format dates for storage (dd/MM/yyyy format)
                                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val handoverDateString = handoverDate?.let { dateFormat.format(it) }
                                val maintenanceDateString = maintenanceDate?.let { dateFormat.format(it) }
                                
                                // Format suspension date if suspension is enabled
                                val suspendedDateString = if (enableSuspension && suspendedUntil != null) {
                                    dateFormat.format(suspendedUntil)
                                } else {
                                    null
                                }
                                
                                // Get suspension reason (use notes if "Other" is selected)
                                val finalSuspensionReason = if (enableSuspension && selectedSuspensionReason != null) {
                                    if (selectedSuspensionReason == "Other") {
                                        suspensionNotes
                                    } else {
                                        selectedSuspensionReason
                                    }
                                } else {
                                    null
                                }
                                
                                // If project is REVIEW REJECTED, set status to IN REVIEW when resubmitting
                                // Otherwise, let ViewModel handle status based on suspension logic
                                val newStatus = if (editingProject?.status == "REVIEW REJECTED") {
                                    "IN REVIEW"
                                } else {
                                    projectStatus
                                }
                                
                                // Check if status has changed and determine which dates will change
                                // Show confirmation dialog only if dates will actually be updated
                                val statusChanged = originalStatus != null && originalStatus != newStatus
                                
                                if (statusChanged) {
                                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    val calendar = java.util.Calendar.getInstance()
                                    val today = calendar.apply {
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                    }.time
                                    
                                    val dateChanges = mutableListOf<String>()
                                    var message = ""
                                    
                                    when (newStatus.uppercase()) {
                                        "ACTIVE" -> {
                                            // Only check for planned date change if changing from LOCKED to ACTIVE
                                            if (originalStatus?.uppercase() == "LOCKED" && startDate != null) {
                                                val planned = calendar.apply {
                                                    time = startDate!!
                                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                    set(java.util.Calendar.MINUTE, 0)
                                                    set(java.util.Calendar.SECOND, 0)
                                                    set(java.util.Calendar.MILLISECOND, 0)
                                                }.time
                                                
                                                if (planned.after(today)) {
                                                    dateChanges.add("Planned Start Date")
                                                }
                                            }
                                            
                                            if (dateChanges.isNotEmpty()) {
                                                val dateList = dateChanges.joinToString(" and ")
                                                message = "Changing status to ACTIVE will automatically update the $dateList to yesterday's date (to reflect immediately in UI). Do you want to continue?"
                                            }
                                        }
                                        "MAINTENANCE" -> {
                                            // If changing from COMPLETED to MAINTENANCE, maintenance date will be set to 1 month from now
                                            if (originalStatus?.uppercase() == "COMPLETED") {
                                                dateChanges.add("Maintenance Date")
                                                message = "Changing status to MAINTENANCE will automatically update the Maintenance Date to 1 month from now. Do you want to continue?"
                                            } else {
                                                // If handoverDate > today, set handoverDate = yesterday
                                                if (handoverDate != null) {
                                                    val handover = calendar.apply {
                                                        time = handoverDate!!
                                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                        set(java.util.Calendar.MINUTE, 0)
                                                        set(java.util.Calendar.SECOND, 0)
                                                        set(java.util.Calendar.MILLISECOND, 0)
                                                    }.time
                                                    
                                                    if (handover.after(today)) {
                                                        dateChanges.add("Handover Date")
                                                    }
                                                }
                                                
                                                if (dateChanges.isNotEmpty()) {
                                                    val dateList = dateChanges.joinToString(" and ")
                                                    message = "Changing status to MAINTENANCE will automatically update the $dateList to yesterday's date (to reflect immediately in UI). Do you want to continue?"
                                                }
                                            }
                                        }
                                        "COMPLETED" -> {
                                            // Check handoverDate
                                            if (handoverDate != null) {
                                                val handover = calendar.apply {
                                                    time = handoverDate!!
                                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                    set(java.util.Calendar.MINUTE, 0)
                                                    set(java.util.Calendar.SECOND, 0)
                                                    set(java.util.Calendar.MILLISECOND, 0)
                                                }.time
                                                
                                                if (handover.after(today)) {
                                                    dateChanges.add("Handover Date")
                                                }
                                            }
                                            
                                            // Check maintenanceDate
                                            if (maintenanceDate != null) {
                                                val maintenance = calendar.apply {
                                                    time = maintenanceDate!!
                                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                    set(java.util.Calendar.MINUTE, 0)
                                                    set(java.util.Calendar.SECOND, 0)
                                                    set(java.util.Calendar.MILLISECOND, 0)
                                                }.time
                                                
                                                if (maintenance.after(today)) {
                                                    dateChanges.add("Maintenance Date")
                                                }
                                            }
                                            
                                            if (dateChanges.isNotEmpty()) {
                                                val dateList = dateChanges.joinToString(" and ")
                                                message = "Changing status to COMPLETED will automatically update the $dateList to yesterday's date (to reflect immediately in UI). All phase dates will also be updated. Do you want to continue?"
                                            }
                                        }
                                    }
                                    
                                    // Show confirmation dialog only if dates will change
                                    if (dateChanges.isNotEmpty() && message.isNotEmpty()) {
                                        pendingStatusChange = newStatus
                                        statusChangeMessage = message
                                        showStatusChangeConfirmation = true
                                        return@Button
                                    }
                                }
                                
                                // Proceed with save if no date updates needed
                                viewModel.updateProject(
                                    projectId = projectId,
                                    projectName = projectName,
                                    description = description,
                                    client = client,
                                    location = location,
                                    currency = currency,
                                    startDate = startTimestamp,
                                    endDate = endTimestamp,
                                    handoverDate = handoverDateString,
                                    maintenanceDate = maintenanceDateString,
                                    totalBudget = totalBudget,
                                    managerId = resolveManagerId(),
                                    teamMemberIds = resolveTeamMemberIds(),
                                    departmentBudgets = departmentBudgets,
                                    status = newStatus,
                                    isSuspended = enableSuspension,
                                    suspendedDate = suspendedDateString,
                                    suspensionReason = finalSuspensionReason
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = iosPalette.accent,
                            contentColor = iosPalette.onAccent
                        ),
                        enabled = projectName.isNotBlank() && !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = iosPalette.onAccent
                            )
                        } else {
                            Text(
                                text = if (editingProject?.status == "REVIEW REJECTED") "Resubmit for Review" else "Save Changes",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosPalette.onAccent,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    Text(
                        text = "You can save even if only one field changed.",
                        fontSize = 12.sp,
                        color = iosPalette.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        } else if (hasAttemptedLoad && !isLoading && editingProject == null) {
            // Only show "Project not found" if we've attempted to load and it's not loading
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Project not found", color = iosPalette.textPrimary)
            }
        } else {
            // Still waiting for preloaded data or initial load
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = iosPalette.accent)
            }
        }
        
        // Status Change Confirmation Dialog
        if (showStatusChangeConfirmation && pendingStatusChange != null && statusChangeMessage.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = {
                    showStatusChangeConfirmation = false
                    pendingStatusChange = null
                    statusChangeMessage = ""
                },
                title = {
                    Text(
                        text = "Confirm Status Change",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = statusChangeMessage,
                        fontSize = 16.sp,
                        color = iosPalette.textPrimary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val startTimestamp = resolveStartTimestamp()
                            val endTimestamp = endDate?.let { Timestamp(it) }
                            
                            // Format dates for storage (dd/MM/yyyy format)
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val calendar = java.util.Calendar.getInstance()
                            val today = calendar.apply {
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }.time
                            
                            // Calculate yesterday
                            val yesterday = calendar.apply {
                                time = today
                                add(java.util.Calendar.DAY_OF_MONTH, -1)
                            }.time
                            
                            // Calculate 2 days back (for phase start dates)
                            val twoDaysBack = calendar.apply {
                                time = today
                                add(java.util.Calendar.DAY_OF_MONTH, -2)
                            }.time
                            
                            // Update dates based on status change (matching guide logic)
                            var updatedStartDate = startDate
                            var updatedHandoverDate = handoverDate
                            var updatedMaintenanceDate = maintenanceDate
                            
                            when (pendingStatusChange?.uppercase()) {
                                "ACTIVE" -> {
                                    // If changing from LOCKED to ACTIVE and plannedDate > today, set plannedDate = yesterday
                                    if (originalStatus?.uppercase() == "LOCKED" && startDate != null) {
                                        val planned = calendar.apply {
                                            time = startDate!!
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }.time
                                        
                                        if (planned.after(today)) {
                                            updatedStartDate = yesterday
                                            startDate = yesterday
                                            plannedStartDatePickerState.selectedDateMillis = yesterday.time
                                        }
                                    }
                                }
                                "MAINTENANCE" -> {
                                    // If changing from COMPLETED to MAINTENANCE, set maintenance date to 1 month from now
                                    if (originalStatus?.uppercase() == "COMPLETED") {
                                        val oneMonthFromNow = calendar.apply {
                                            time = today
                                            add(java.util.Calendar.MONTH, 1)
                                        }.time
                                        updatedMaintenanceDate = oneMonthFromNow
                                        maintenanceDate = oneMonthFromNow
                                        maintenanceDatePickerState.selectedDateMillis = oneMonthFromNow.time
                                    } else {
                                        // If handoverDate > today, set handoverDate = yesterday
                                        if (handoverDate != null) {
                                            val handover = calendar.apply {
                                                time = handoverDate!!
                                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                set(java.util.Calendar.MINUTE, 0)
                                                set(java.util.Calendar.SECOND, 0)
                                                set(java.util.Calendar.MILLISECOND, 0)
                                            }.time
                                            
                                            if (handover.after(today)) {
                                                updatedHandoverDate = yesterday
                                                handoverDate = yesterday
                                                handoverDatePickerState.selectedDateMillis = yesterday.time
                                            }
                                        }
                                    }
                                }
                                "COMPLETED" -> {
                                    // If handoverDate > today, set handoverDate = yesterday
                                    if (handoverDate != null) {
                                        val handover = calendar.apply {
                                            time = handoverDate!!
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }.time
                                        
                                        if (handover.after(today)) {
                                            updatedHandoverDate = yesterday
                                            handoverDate = yesterday
                                            handoverDatePickerState.selectedDateMillis = yesterday.time
                                        }
                                    }
                                    
                                    // If maintenanceDate > today, set maintenanceDate = yesterday
                                    if (maintenanceDate != null) {
                                        val maintenance = calendar.apply {
                                            time = maintenanceDate!!
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }.time
                                        
                                        if (maintenance.after(today)) {
                                            updatedMaintenanceDate = yesterday
                                            maintenanceDate = yesterday
                                            maintenanceDatePickerState.selectedDateMillis = yesterday.time
                                        }
                                    }
                                    
                                    // Update all phase dates (will be handled in ViewModel/Repository)
                                    // Phase end dates in future → set to yesterday
                                    // Phase start dates in future → set to 2 days back
                                }
                            }
                            
                            val handoverDateString = updatedHandoverDate?.let { dateFormat.format(it) }
                            val maintenanceDateString = updatedMaintenanceDate?.let { dateFormat.format(it) }
                            
                            // Format suspension date if suspension is enabled
                            val suspendedDateString = if (enableSuspension && suspendedUntil != null) {
                                dateFormat.format(suspendedUntil)
                            } else {
                                null
                            }
                            
                            // Get suspension reason (use notes if "Other" is selected)
                            val finalSuspensionReason = if (enableSuspension && selectedSuspensionReason != null) {
                                if (selectedSuspensionReason == "Other") {
                                    suspensionNotes
                                } else {
                                    selectedSuspensionReason
                                }
                            } else {
                                null
                            }
                            
                            viewModel.updateProject(
                                projectId = projectId,
                                projectName = projectName,
                                description = description,
                                client = client,
                                location = location,
                                currency = currency,
                                startDate = updatedStartDate?.let { Timestamp(it) } ?: startTimestamp,
                                endDate = endTimestamp,
                                handoverDate = handoverDateString,
                                maintenanceDate = maintenanceDateString,
                                totalBudget = totalBudget,
                                managerId = resolveManagerId(),
                                teamMemberIds = resolveTeamMemberIds(),
                                departmentBudgets = departmentBudgets,
                                status = pendingStatusChange!!,
                                isSuspended = enableSuspension,
                                suspendedDate = suspendedDateString,
                                suspensionReason = finalSuspensionReason
                            )
                            
                            // If status changed to COMPLETED, update phase dates
                            if (pendingStatusChange?.uppercase() == "COMPLETED") {
                                viewModel.updatePhasesForCompletedStatus(
                                    projectId = projectId,
                                    yesterdayDate = dateFormat.format(yesterday),
                                    twoDaysBackDate = dateFormat.format(twoDaysBack)
                                )
                            }
                            
                            // Update originalStatus to the new status so LaunchedEffect doesn't trigger again
                            originalStatus = pendingStatusChange
                            
                            showStatusChangeConfirmation = false
                            pendingStatusChange = null
                            statusChangeMessage = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = iosPalette.accent,
                            contentColor = iosPalette.onAccent
                        )
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showStatusChangeConfirmation = false
                            pendingStatusChange = null
                            statusChangeMessage = ""
                        }
                    ) {
                        Text("Cancel", color = iosPalette.accent)
                    }
                }
            )
        }
        
        // View All Team Members Sheet
        if (showViewAllTeamMembersSheet && editingProject != null && selectedMemberForDetail == null) {
            val currentProject = editingProject!!
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
            
            ModalBottomSheet(
                onDismissRequest = { showViewAllTeamMembersSheet = false },
                sheetState = sheetState,
                containerColor = iosPalette.tier2Surface,
                dragHandle = null
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.95f)
                ) {
                    TeamMembersDetailScreen(
                        projectID = currentProject.id ?: "",
                        customerID = null,
                        userRole = authState.user?.role ?: UserRole.BUSINESS_HEAD,
                        currentUserPhone = authState.user?.phone,
                        onBackClick = { showViewAllTeamMembersSheet = false },
                        onAddMemberClick = {
                            showViewAllTeamMembersSheet = false
                            showAddTeamMemberSheet = true
                        },
                        onMemberClick = { member ->
                            selectedMemberForDetail = member
                            showViewAllTeamMembersSheet = false
                        },
                        showExpensesInternally = false
                    )
                }
            }
        }
        
        // Add Team Member Sheet
        if (showAddTeamMemberSheet && editingProject != null && selectedMemberForDetail == null) {
            // Filter out already selected team members and current project manager
            val unselectedUsers = availableUsers.filter { 
                it.phone !in selectedTeamMembers && it.phone != selectedManagerId 
            }
            val unselectedApprovers = availableApprovers.filter { 
                it.phone !in selectedTeamMembers && it.phone != selectedManagerId 
            }
            
            AddTeamMemberSheet(
                availableUsers = unselectedUsers,
                availableApprovers = unselectedApprovers,
                onDismiss = { showAddTeamMemberSheet = false },
                onAddMember = { phone ->
                    // Add to local state
                    val updatedTeamMembers = selectedTeamMembers + phone
                    selectedTeamMembers = updatedTeamMembers

                    // Update backend
                    val startTimestamp = resolveStartTimestamp()
                    val endTimestamp = endDate?.let { Timestamp(it) }

                    // Preserve existing project date fields
                    val handoverDateString = handoverDate?.let { dateFormatterForStorage.format(it) }
                        ?: editingProject?.handoverDate
                    val maintenanceDateString = maintenanceDate?.let { dateFormatterForStorage.format(it) }
                        ?: editingProject?.maintenanceDate
                    
                    // Format suspension data
                    val suspendedDateString = if (enableSuspension && suspendedUntil != null) {
                        dateFormatterForStorage.format(suspendedUntil)
                    } else {
                        null
                    }
                    val finalSuspensionReason = if (enableSuspension && selectedSuspensionReason != null) {
                        if (selectedSuspensionReason == "Other") {
                            suspensionNotes
                        } else {
                            selectedSuspensionReason
                        }
                    } else {
                        null
                    }
                    
                    viewModel.updateProject(
                        projectId = projectId,
                        projectName = projectName,
                        description = description,
                        client = client,
                        location = location,
                        currency = currency,
                        startDate = startTimestamp,
                        endDate = endTimestamp,
                        handoverDate = handoverDateString,
                        maintenanceDate = maintenanceDateString,
                        totalBudget = totalBudget,
                        managerId = resolveManagerId(),
                        teamMemberIds = updatedTeamMembers.toList(),
                        departmentBudgets = departmentBudgets,
                        status = projectStatus,
                        isSuspended = enableSuspension,
                        suspendedDate = suspendedDateString,
                        suspensionReason = finalSuspensionReason
                    )
                    
                    // Reload project to get updated data
                    viewModel.loadProjectForEdit(projectId)
                }
            )
        }
        
        // Suspended Until Date Picker
        if (showSuspendedUntilDatePicker) {
            EditProjectDatePickerDialog(
                onDateSelected = { date ->
                    suspendedUntil = date
                    suspendedUntilDatePickerState.selectedDateMillis = date.time
                    showSuspendedUntilDatePicker = false
                },
                onDismiss = { showSuspendedUntilDatePicker = false },
                datePickerState = suspendedUntilDatePickerState,
                title = "Select Suspended Until Date"
            )
        }
        
        // Planned Start Date Picker
        if (showPlannedStartDatePicker) {
            EditProjectDatePickerDialog(
                onDateSelected = { selectedDate: Date ->
                    startDate = selectedDate
                    plannedStartDatePickerState.selectedDateMillis = selectedDate.time
                    showPlannedStartDatePicker = false
                },
                onDismiss = { showPlannedStartDatePicker = false },
                datePickerState = plannedStartDatePickerState,
                title = "Select Planned Start Date"
            )
        }
        
        // Handover Date Picker
        if (showHandoverDatePicker) {
            val minDateMillis = startDate?.time
            if (minDateMillis != null) {
                LaunchedEffect(showHandoverDatePicker) {
                    if (handoverDate != null) {
                        handoverDatePickerState.selectedDateMillis = maxOf(handoverDate!!.time, minDateMillis)
                    } else {
                        handoverDatePickerState.selectedDateMillis = minDateMillis
                    }
                }
            }
            
            EditProjectDatePickerDialog(
                onDateSelected = { selectedDate: Date ->
                    // Validate: Handover date must be >= Planned Start Date
                    val currentStartDate = startDate
                    if (currentStartDate != null) {
                        val calendarSelected = java.util.Calendar.getInstance().apply {
                            time = selectedDate
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.time
                        
                        val calendarPlanned = java.util.Calendar.getInstance().apply {
                            time = currentStartDate
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.time
                        
                        if (calendarSelected.time < calendarPlanned.time) {
                            android.widget.Toast.makeText(
                                context,
                                "Handover date must be on or after Planned Start Date",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            return@EditProjectDatePickerDialog
                        }
                    }
                    handoverDate = selectedDate
                    handoverDatePickerState.selectedDateMillis = selectedDate.time
                    handoverDateError = null
                    showHandoverDatePicker = false
                },
                onDismiss = { showHandoverDatePicker = false },
                datePickerState = handoverDatePickerState,
                title = "Select Handover Date",
                minimumDateMillis = minDateMillis
            )
        }
        
        // Maintenance Date Picker
        if (showMaintenanceDatePicker) {
            val minDateForMaintenance = handoverDate ?: startDate
            val minDateMillis = minDateForMaintenance?.time
            if (minDateMillis != null) {
                LaunchedEffect(showMaintenanceDatePicker) {
                    if (maintenanceDate != null) {
                        maintenanceDatePickerState.selectedDateMillis = maxOf(maintenanceDate!!.time, minDateMillis)
                    } else {
                        maintenanceDatePickerState.selectedDateMillis = minDateMillis
                    }
                }
            }
            
            EditProjectDatePickerDialog(
                onDateSelected = { selectedDate: Date ->
                    // Validate: Maintenance date must be >= Handover Date (or Planned Start Date)
                    if (minDateForMaintenance != null) {
                        val calendarSelected = java.util.Calendar.getInstance().apply {
                            time = selectedDate
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.time
                        
                        val calendarMin = java.util.Calendar.getInstance().apply {
                            time = minDateForMaintenance
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.time
                        
                        if (calendarSelected.time < calendarMin.time) {
                            val minDateName = if (handoverDate != null) "Handover Date" else "Planned Start Date"
                            android.widget.Toast.makeText(
                                context,
                                "Maintenance date must be on or after $minDateName",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            return@EditProjectDatePickerDialog
                        }
                    }
                    maintenanceDate = selectedDate
                    maintenanceDatePickerState.selectedDateMillis = selectedDate.time
                    maintenanceDateError = null
                    showMaintenanceDatePicker = false
                },
                onDismiss = { showMaintenanceDatePicker = false },
                datePickerState = maintenanceDatePickerState,
                title = "Select Maintenance Date",
                minimumDateMillis = minDateMillis
            )
        }

        // Full-screen loading overlay shown while deletion is in progress
        if (isDeletingProject) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = iosPalette.accent)
            }
        }
    }

}

/**
 * Date Picker Dialog Component for Edit Project Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProjectDatePickerDialog(
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    datePickerState: DatePickerState,
    title: String,
    minimumDateMillis: Long? = null
) {
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Validate minimum date if set
                        if (minimumDateMillis != null && millis < minimumDateMillis) {
                            return@TextButton
                        }
                        onDateSelected(Date(millis))
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        Column {
            // Show warning if minimum date is set
            if (minimumDateMillis != null) {
                val minimumDate = Date(minimumDateMillis)
                val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                Text(
                    text = "Please select a date on or after ${dateFormatter.format(minimumDate)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            DatePicker(
                state = datePickerState,
                modifier = Modifier.padding(16.dp),
                title = {
                    Text(
                        text = title,
                        modifier = Modifier.padding(16.dp)
                    )
                },
                showModeToggle = false
            )
        }
    }
}

/**
 * Reusable card component for project fields with edit functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFieldCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iosPalette: IosEditPalette,
    title: String,
    content: String,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onSave: () -> Unit,
    editContent: @Composable () -> Unit,
    displayContent: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 2.dp),
        border = BorderStroke(0.75.dp, iosPalette.hairline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary
                    )
                }
                // Edit button - pencil icon
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = iosPalette.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isEditing) {
                editContent()
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onEditClick() }) {
                        Text("Cancel", color = iosPalette.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = iosPalette.accent,
                            contentColor = iosPalette.onAccent
                        )
                    ) {
                        Text("Save")
                    }
                }
            } else {
                Surface(
                    color = iosPalette.tier3Field,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(0.75.dp, iosPalette.hairline)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        displayContent()
                    }
                }
            }
        }
    }
}

/**
 * Date Field Card with date picker and "Past Date" badge
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFieldCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iosPalette: IosEditPalette,
    title: String,
    date: Date?,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onDateClick: () -> Unit,
    onSave: () -> Unit,
    minDate: Date? = null,
    errorMessage: String? = null,
    infoText: String? = null
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val currentDate = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time
    }
    
    val isPastDate = remember(date) {
        date?.let {
            val calendarDate = java.util.Calendar.getInstance().apply {
                time = it
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time
            calendarDate.time < currentDate.time
        } ?: false
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 2.dp),
        border = BorderStroke(0.75.dp, iosPalette.hairline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary
                    )
                }
                // Edit button - pencil icon
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = iosPalette.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isEditing) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDateClick() },
                    color = iosPalette.tier3Field,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.75.dp, iosPalette.hairline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = date?.let { dateFormatter.format(it) } ?: "Select date",
                            fontSize = 16.sp,
                            color = if (date == null) iosPalette.textSecondary else iosPalette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            tint = iosPalette.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = iosPalette.danger,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onEditClick() }) {
                        Text("Cancel", color = iosPalette.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = iosPalette.accent,
                            contentColor = iosPalette.onAccent
                        )
                    ) {
                        Text("Save")
                    }
                }
            } else {
                Surface(
                    color = iosPalette.tier3Field,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(0.75.dp, iosPalette.hairline)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = date?.let { dateFormatter.format(it) } ?: "No date set",
                                    fontSize = 16.sp,
                                    color = iosPalette.textPrimary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (infoText != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = infoText,
                                        fontSize = 12.sp,
                                        color = iosPalette.textSecondary
                                    )
                                }
                            }
                            if (isPastDate) {
                                Surface(
                                    color = iosPalette.warning,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = iosPalette.onAccent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Past Date",
                                            fontSize = 10.sp,
                                            color = iosPalette.onAccent,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Team Management Card with search functionality and chip-based selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamManagementCard(
    iosPalette: IosEditPalette,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    availableApprovers: List<User>,
    availableUsers: List<User>,
    temporaryApprovers: List<TemporaryApprover>,
    selectedManagerId: String,
    onManagerSelected: (String) -> Unit,
    selectedTeamMembers: Set<String>,
    onTeamMemberToggle: (String) -> Unit,
    onRemoveTeamMember: (String) -> Unit,
    onAddTempApprover: (User) -> Unit,
    onViewAllTeamMembers: () -> Unit = {}
) {
    var projectManagerSearch by remember { mutableStateOf("") }
    var teamMembersSearch by remember { mutableStateOf("") }
    var showProjectManagerDropdown by remember { mutableStateOf(false) }
    var showTeamMembersDropdown by remember { mutableStateOf(false) }
    var showTempApproverModal by remember { mutableStateOf(false) }
    
    val selectedManager = availableApprovers.find { it.phone == selectedManagerId }
    val selectedTeamMembersList = availableUsers.filter { it.phone in selectedTeamMembers }
    
    // Filter approvers based on search
    val filteredApprovers = if (projectManagerSearch.isBlank()) {
        availableApprovers
    } else {
        availableApprovers.filter {
            it.name.contains(projectManagerSearch, ignoreCase = true) ||
            it.phone.contains(projectManagerSearch, ignoreCase = true)
        }
    }
    
    // Filter users based on search
    val filteredUsers = if (teamMembersSearch.isBlank()) {
        availableUsers
    } else {
        availableUsers.filter {
            it.name.contains(teamMembersSearch, ignoreCase = true) ||
            it.phone.contains(teamMembersSearch, ignoreCase = true)
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 2.dp),
        border = BorderStroke(0.75.dp, iosPalette.hairline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = iosPalette.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Team Management",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary
                    )
                }
                // Close button - only show when editing
                if (isEditing) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = iosPalette.danger,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            if (isEditing) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Project Manager Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = iosPalette.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Project Manager",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = iosPalette.textPrimary
                    )
                }
                Text(
                    text = "Select the project approver",
                    fontSize = 12.sp,
                    color = iosPalette.textSecondary,
                    modifier = Modifier.padding(start = 26.dp, bottom = 8.dp)
                )
                Box {
                    OutlinedTextField(
                        value = projectManagerSearch,
                        onValueChange = {
                            projectManagerSearch = it
                            showProjectManagerDropdown = it.isNotBlank()
                        },
                        placeholder = {
                            Text(
                                text = "Search project manager...",
                                fontSize = 14.sp,
                                color = iosPalette.textSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = iosPalette.accent,
                            unfocusedBorderColor = iosPalette.hairline,
                            focusedContainerColor = iosPalette.tier3Field,
                            unfocusedContainerColor = iosPalette.tier3Field,
                            focusedTextColor = iosPalette.textPrimary,
                            unfocusedTextColor = iosPalette.textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = iosPalette.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                    // Dropdown for Project Manager
                    if (showProjectManagerDropdown && filteredApprovers.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = 56.dp),
                            colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 4.dp),
                            border = BorderStroke(0.75.dp, iosPalette.hairline)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                            ) {
                                items(filteredApprovers.size) { index ->
                                    val approver = filteredApprovers[index]
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onManagerSelected(approver.phone)
                                                projectManagerSearch = approver.name
                                                showProjectManagerDropdown = false
                                            },
                                        color = if (selectedManagerId == approver.phone) iosPalette.accent.copy(alpha = 0.14f) else iosPalette.tier2Surface
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = approver.name,
                                                fontSize = 14.sp,
                                                color = iosPalette.textPrimary,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(end = 8.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = approver.phone,
                                                fontSize = 12.sp,
                                                color = iosPalette.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Team Members Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = iosPalette.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Team Members",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = iosPalette.textPrimary
                    )
                }
                Text(
                    text = "Add team members to the project",
                    fontSize = 12.sp,
                    color = iosPalette.textSecondary,
                    modifier = Modifier.padding(start = 26.dp, bottom = 8.dp)
                )
                Box {
                    OutlinedTextField(
                        value = teamMembersSearch,
                        onValueChange = {
                            teamMembersSearch = it
                            showTeamMembersDropdown = it.isNotBlank()
                        },
                        placeholder = {
                            Text(
                                text = "Search team members...",
                                fontSize = 14.sp,
                                color = iosPalette.textSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = iosPalette.accent,
                            unfocusedBorderColor = iosPalette.hairline,
                            focusedContainerColor = iosPalette.tier3Field,
                            unfocusedContainerColor = iosPalette.tier3Field,
                            focusedTextColor = iosPalette.textPrimary,
                            unfocusedTextColor = iosPalette.textPrimary,
                            focusedPlaceholderColor = iosPalette.textSecondary,
                            unfocusedPlaceholderColor = iosPalette.textSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = iosPalette.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                    // Dropdown for Team Members
                    if (showTeamMembersDropdown && filteredUsers.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = 56.dp),
                            colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 4.dp),
                            border = BorderStroke(0.75.dp, iosPalette.hairline)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                            ) {
                                items(filteredUsers.size) { index ->
                                    val user = filteredUsers[index]
                                    val isSelected = selectedTeamMembers.contains(user.phone)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onTeamMemberToggle(user.phone)
                                                teamMembersSearch = ""
                                                showTeamMembersDropdown = false
                                            },
                                        color = if (isSelected) iosPalette.success.copy(alpha = 0.14f) else iosPalette.tier2Surface
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = user.name,
                                                fontSize = 14.sp,
                                                color = iosPalette.textPrimary,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(end = 8.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = user.phone,
                                                fontSize = 12.sp,
                                                color = iosPalette.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Selected Team Members Section
                if (selectedTeamMembersList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Selected Team Members",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    // Use wrapContentHeight and wrapContentWidth for chips to wrap
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        selectedTeamMembersList.chunked(3).forEach { chunk ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chunk.forEach { member ->
                                    Chip(
                                        iosPalette = iosPalette,
                                        text = member.name,
                                        initial = member.name.firstOrNull()?.uppercaseChar() ?: '?',
                                        onRemove = { onRemoveTeamMember(member.phone) }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Temporary Approvers Section
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = iosPalette.warning,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Temporary Approvers",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = iosPalette.textPrimary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (temporaryApprovers.isEmpty()) "No temporary approver assigned" else "${temporaryApprovers.size} temporary approver(s)",
                        fontSize = 12.sp,
                        color = iosPalette.textSecondary
                    )
                    TextButton(
                        onClick = { showTempApproverModal = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = iosPalette.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            fontSize = 14.sp,
                            color = iosPalette.accent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Show Temporary Approvers List if any exist
                if (temporaryApprovers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    temporaryApprovers.forEach { tempApprover ->
                        Surface(
                            color = iosPalette.warning.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = iosPalette.warning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = tempApprover.approverName,
                                        fontSize = 14.sp,
                                        color = iosPalette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    text = tempApprover.approverPhone,
                                    fontSize = 12.sp,
                                    color = iosPalette.textSecondary,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Save Team Button
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = iosPalette.success),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = iosPalette.onAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Team",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosPalette.onAccent
                    )
                }
            } else {
                // Display Mode
                Spacer(modifier = Modifier.height(12.dp))
                
                // Project Manager Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = iosPalette.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = iosPalette.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Project Manager",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = iosPalette.textPrimary
                    )
                }
                Text(
                    text = "Select project approver (single selection)",
                    fontSize = 12.sp,
                    color = iosPalette.textSecondary,
                    modifier = Modifier.padding(start = 26.dp, bottom = 8.dp)
                )
                Box {
                    Surface(
                        color = iosPalette.tier3Field,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProjectManagerDropdown = !showProjectManagerDropdown }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedManager?.name ?: "No manager selected",
                                fontSize = 16.sp,
                                color = iosPalette.textPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = iosPalette.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // Dropdown for Project Manager - Using Material3 DropdownMenu for proper positioning
                    DropdownMenu(
                        expanded = showProjectManagerDropdown && availableApprovers.isNotEmpty(),
                        onDismissRequest = { showProjectManagerDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .background(iosPalette.tier2Surface, RoundedCornerShape(8.dp))
                    ) {
                        availableApprovers.forEach { approver ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = approver.name,
                                            fontSize = 14.sp,
                                            color = iosPalette.textPrimary,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = approver.phone,
                                            fontSize = 12.sp,
                                            color = iosPalette.textSecondary
                                        )
                                        if (selectedManagerId == approver.phone) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = iosPalette.accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onManagerSelected(approver.phone)
                                    showProjectManagerDropdown = false
                                },
                                modifier = Modifier.background(
                                    if (selectedManagerId == approver.phone) iosPalette.accent.copy(alpha = 0.14f) else Color.Transparent
                                )
                            )
                        }
                    }
                }
                
                // Team Members Section
                if (selectedTeamMembersList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = iosPalette.success,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Team Members",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = iosPalette.textPrimary
                        )
                    }
                    
                    // Show only the first team member
                    val firstMember = selectedTeamMembersList.first()
                    val memberInitial = firstMember.name.firstOrNull()?.uppercaseChar() ?: '?'
                    
                    Surface(
                        color = iosPalette.tier3Field,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Avatar circle with initial
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(iosPalette.accent.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = memberInitial.toString(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = iosPalette.onAccent
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = firstMember.name,
                                    fontSize = 16.sp,
                                    color = iosPalette.textPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = iosPalette.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = firstMember.phone,
                                        fontSize = 14.sp,
                                        color = iosPalette.textSecondary
                                    )
                                }
                            }
                        }
                    }
                    
                    // View All Team Members Button
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewAllTeamMembers() },
                        color = iosPalette.accent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = iosPalette.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "View All Team Members",
                                    fontSize = 16.sp,
                                    color = iosPalette.accent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = iosPalette.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // Temporary Approver Selection Modal
            if (showTempApproverModal) {
                AssignTempApproverModal(
                    iosPalette = iosPalette,
                    availableApprovers = availableApprovers,
                    onDismiss = { showTempApproverModal = false },
                    onSelectApprover = { approver ->
                        onAddTempApprover(approver)
                        showTempApproverModal = false
                    }
                )
            }
        }
    }
}

/**
 * Modal Bottom Sheet for Assigning Temporary Approver
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignTempApproverModal(
    iosPalette: IosEditPalette,
    availableApprovers: List<User>,
    onDismiss: () -> Unit,
    onSelectApprover: (User) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedApproverPhone by remember { mutableStateOf<String?>(null) }
    
    val filteredApprovers = if (searchQuery.isBlank()) {
        availableApprovers
    } else {
        availableApprovers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true)
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = iosPalette.tier2Surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with Cancel button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        fontSize = 16.sp,
                        color = iosPalette.accent
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Title
            Text(
                text = "Assign Temp Approver",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = iosPalette.textPrimary,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search by name or phone number",
                        fontSize = 14.sp,
                        color = iosPalette.textSecondary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = iosPalette.accent,
                    unfocusedBorderColor = iosPalette.hairline,
                    focusedContainerColor = iosPalette.tier3Field,
                    unfocusedContainerColor = iosPalette.tier3Field,
                    focusedTextColor = iosPalette.textPrimary,
                    unfocusedTextColor = iosPalette.textPrimary,
                    focusedPlaceholderColor = iosPalette.textSecondary,
                    unfocusedPlaceholderColor = iosPalette.textSecondary
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = iosPalette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Approvers List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApprovers.size) { index ->
                    val approver = filteredApprovers[index]
                    val isSelected = selectedApproverPhone == approver.phone
                    val initial = approver.name.firstOrNull()?.uppercaseChar() ?: '?'
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedApproverPhone = approver.phone
                                onSelectApprover(approver)
                            },
                        color = if (isSelected) iosPalette.accent.copy(alpha = 0.14f) else iosPalette.tier2Surface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Avatar with initial
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(iosPalette.accent.copy(alpha = 0.14f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initial.toString(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = iosPalette.accent
                                    )
                                }
                                
                                // Name and Phone
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = approver.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = iosPalette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = approver.phone,
                                        fontSize = 14.sp,
                                        color = iosPalette.textSecondary
                                    )
                                }
                            }
                            
                            // Radio Button
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedApproverPhone = approver.phone
                                    onSelectApprover(approver)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chip component for selected team members
 */
@Composable
fun Chip(
    iosPalette: IosEditPalette,
    text: String,
    initial: Char,
    onRemove: () -> Unit
) {
    Surface(
        color = iosPalette.success.copy(alpha = 0.16f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(iosPalette.success, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.onAccent
                )
            }
            Text(
                text = text,
                fontSize = 14.sp,
                color = iosPalette.textPrimary,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = iosPalette.danger,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * View All Team Members Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewAllTeamMembersSheet(
    teamMembers: List<User>,
    project: Project? = null,
    projectID: String,
    onDismiss: () -> Unit,
    userRole : UserRole? = UserRole.APPROVER,
    currentUserPhone: String? = null,
    onMemberClick: (User) -> Unit = {},
    onDeleteMember: (String) -> Unit,
    onAddMember: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val iosPalette = rememberIosEditPalette()
    var searchQuery by remember { mutableStateOf("") }
    val filteredMembers = if (searchQuery.isBlank()) {
        teamMembers
    } else {
        teamMembers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true)
        }
    }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = iosPalette.tier2Surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .background(iosPalette.tier2Surface)
                .padding(top = 24.dp)
        ) {
            // Blue Header
            Surface(
                color = iosPalette.accent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Back icon in top-left
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = iosPalette.onAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Centered title and member count
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TEAM MEMBERS",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosPalette.onAccent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${filteredMembers.size} members",
                            fontSize = 14.sp,
                            color = iosPalette.onAccent
                        )
                    }
                    
                    // Action icons in top-right
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (userRole == UserRole.BUSINESS_HEAD || userRole == UserRole.APPROVER){
                            IconButton(
                                onClick = onAddMember,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Add Member",
                                    tint = iosPalette.onAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = iosPalette.onAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            // Search Bar
            Surface(
                color = iosPalette.tier3Field,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search members...",
                            fontSize = 14.sp,
                            color = iosPalette.textSecondary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = iosPalette.tier3Field,
                        unfocusedContainerColor = iosPalette.tier3Field,
                        focusedTextColor = iosPalette.textPrimary,
                        unfocusedTextColor = iosPalette.textPrimary,
                        focusedPlaceholderColor = iosPalette.textSecondary,
                        unfocusedPlaceholderColor = iosPalette.textSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = iosPalette.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
            
            // Spacing between search bar and team member list
            Spacer(modifier = Modifier.height(16.dp))
            
            // Team Members List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 8.dp)
            ) {
                items(filteredMembers) { member ->
                    TeamMemberListItem(
                        iosPalette = iosPalette,
                        projectID = projectID,
                        member = member,
                        userRole = userRole,
                        currentUserPhone = currentUserPhone,
                        onTap = { onMemberClick(member) },
                        onDelete = { onDeleteMember(member.phone) }
                    )
                }
            }
        }
    }
}

/**
 * Team Member List Item
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamMemberListItem(
    iosPalette: IosEditPalette,
    projectID: String,
    member: User,
    onTap: () -> Unit = {},
    onDelete: () -> Unit,
    userRole: UserRole? = UserRole.APPROVER,
    currentUserPhone: String? = null
) {
    val memberInitial = member.name.firstOrNull()?.uppercaseChar() ?: '?'
    var showUserDetailView by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                showUserDetailView = true
            },
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 2.dp),
        border = BorderStroke(0.75.dp, iosPalette.hairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iosPalette.accent.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = memberInitial.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.onAccent
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Name and Contact Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = member.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = iosPalette.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = member.phone,
                        fontSize = 14.sp,
                        color = iosPalette.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(iosPalette.accent, CircleShape)
                    )
                    Text(
                        text = "User",
                        fontSize = 12.sp,
                        color = iosPalette.textSecondary
                    )
                }
            }
            
            // Delete Button
            if (userRole == UserRole.BUSINESS_HEAD || userRole == UserRole.APPROVER){
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = iosPalette.danger,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
    // iOS-style Bottom Sheet for TeamMembersDetailScreen
    if (showUserDetailView) {
        ModalBottomSheet(
            onDismissRequest = { showUserDetailView = false },
            sheetState = sheetState,
            containerColor = iosPalette.tier2Surface,
            dragHandle = null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
            ) {
                TeamMembersDetailScreen(
                    projectID = projectID,
                    customerID = null,
                    userRole = userRole,
                    currentUserPhone = currentUserPhone,
                    onBackClick = { showUserDetailView = false },
                    onAddMemberClick = {},
                    onMemberClick = {},
                    showExpensesInternally = true,
                    initialSelectedMember = member
                )
            }
        }
    }
}

/**
 * Add Team Member Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTeamMemberSheet(
    availableUsers: List<User>,
    availableApprovers: List<User>,
    onDismiss: () -> Unit,
    onAddMember: (String) -> Unit
) {
    val iosPalette = rememberIosEditPalette()
    var searchQuery by remember { mutableStateOf("") }
    
    // Only show users (no approvers) - filter out approvers
    val allUsers = remember(availableUsers, availableApprovers) {
        // Only include users, exclude approvers
        availableUsers
            .distinctBy { it.phone }
            .map { user ->
                user to false // All shown users are regular users, not approvers
            }
    }
    
    val filteredUsers = if (searchQuery.isBlank()) {
        allUsers
    } else {
        allUsers.filter { (user, _) ->
            user.name.contains(searchQuery, ignoreCase = true) ||
            user.phone.contains(searchQuery, ignoreCase = true) ||
            (user.email?.contains(searchQuery, ignoreCase = true) == true)
        }
    }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = iosPalette.tier2Surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(iosPalette.tier2Surface)
        ) {
            // Header
            Surface(
                color = iosPalette.tier2Surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            fontSize = 16.sp,
                            color = iosPalette.accent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Add Team Member",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary
                    )
                    Spacer(modifier = Modifier.width(60.dp)) // Balance the layout
                }
            }
            
            // Search Bar
            Surface(
                color = iosPalette.tier3Field,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search by name, phone, or email...",
                            fontSize = 14.sp,
                            color = iosPalette.textSecondary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = iosPalette.tier3Field,
                        unfocusedContainerColor = iosPalette.tier3Field,
                        focusedTextColor = iosPalette.textPrimary,
                        unfocusedTextColor = iosPalette.textPrimary,
                        focusedPlaceholderColor = iosPalette.textSecondary,
                        unfocusedPlaceholderColor = iosPalette.textSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = iosPalette.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
            
            // Users List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredUsers.size) { index ->
                    val (user, isApprover) = filteredUsers[index]
                    AddTeamMemberListItem(
                        iosPalette = iosPalette,
                        user = user,
                        isApprover = isApprover,
                        onAdd = { onAddMember(user.phone) }
                    )
                }
            }
        }
    }
}

/**
 * Add Team Member List Item
 */
@Composable
fun AddTeamMemberListItem(
    iosPalette: IosEditPalette,
    user: User,
    isApprover: Boolean,
    onAdd: () -> Unit
) {
    val userInitial = user.name.firstOrNull()?.uppercaseChar() ?: '?'
    val avatarColor = if (isApprover) iosPalette.warning.copy(alpha = 0.35f) else iosPalette.accent.copy(alpha = 0.35f)
    val roleColor = if (isApprover) iosPalette.warning else iosPalette.accent
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 2.dp),
        border = BorderStroke(0.75.dp, iosPalette.hairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(avatarColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userInitial.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.onAccent
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Name and Contact Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = iosPalette.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = user.phone,
                        fontSize = 14.sp,
                        color = iosPalette.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(roleColor, CircleShape)
                    )
                    Text(
                        text = if (isApprover) "Approver" else "User",
                        fontSize = 12.sp,
                        color = iosPalette.textSecondary
                    )
                }
            }
            
            // Add Button
            IconButton(
                onClick = onAdd,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iosPalette.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = iosPalette.onAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
} 
