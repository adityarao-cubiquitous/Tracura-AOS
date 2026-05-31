package com.cubiquitous.tracura.ui.view.businesshead

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.model.PhaseDraft
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.viewmodel.BusinessHeadViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewProjectScreen(
    projectName: String,
    description: String,
    client: String,
    clientPrimaryNumber: String? = null,
    clientSecondaryNumber: String? = null,
    location: String,
    currency: String,
    plannedStartDate: Date?,
    handoverDate: Date?,
    maintenanceDate: Date?,
    phases: List<PhaseDraft>,
    selectedApprover: User?,
    selectedTeamMembers: List<User>,
    departmentUserAssignments: Map<String, String> = emptyMap(),
    departmentApproverAssignments: Map<String, String> = emptyMap(),
    onNavigateBack: () -> Unit,
    onProjectCreated: () -> Unit,
    projectIdForResubmission: String? = null, // For resubmitting REVIEW REJECTED projects
    viewModel: BusinessHeadViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val availableUsers by viewModel.availableUsers.collectAsState()
    val availableApprovers by viewModel.availableApprovers.collectAsState()
    
    // Load users on screen initialization
    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }
    
    // State for showing success dialog
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // Helper function to get user name from phone number
    fun getUserNameFromPhone(phone: String): String {
        val user = availableUsers.find { it.phone == phone } 
            ?: availableApprovers.find { it.phone == phone }
        return user?.name ?: phone
    }
    
    // Collect all unique departments from phases
    val allDepartments = remember(phases, departmentUserAssignments, departmentApproverAssignments) {
        phases.flatMap { it.departments }
            .mapNotNull { it.departmentName }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    
    // Debug logging for phases
    LaunchedEffect(phases) {
        Log.d("ReviewProjectScreen", "📊 Phases received: ${phases.size}")
        phases.forEachIndexed { idx, phase ->
            val phaseBudget = phase.departments.sumOf { it.totalBudget }
            Log.d("ReviewProjectScreen", "  Phase ${idx + 1}: ${phase.phaseName}, Departments: ${phase.departments.size}, Budget: $phaseBudget")
            phase.departments.forEach { dept ->
                Log.d("ReviewProjectScreen", "    - ${dept.departmentName}: ${dept.totalBudget}")
            }
        }
    }
    
    // Calculate total budget - ensure we're using the actual phases passed
    val totalBudget = remember(phases) {
        val calculated = phases.sumOf { draft -> 
            val phaseBudget = draft.departments.sumOf { it.totalBudget }
            Log.d("ReviewProjectScreen", "Phase '${draft.phaseName}' budget: $phaseBudget (${draft.departments.size} departments)")
            phaseBudget
        }
        Log.d("ReviewProjectScreen", "💰 Total budget calculated: $calculated from ${phases.size} phases")
        calculated
    }
    
    // Format currency symbol
    val currencySymbol = when (currency) {
        "INR" -> "₹"
        "USD" -> "$"
        else -> "₹"
    }
    
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Top Bar
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Review Project",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            navigationIcon = {
                TextButton(onClick = onNavigateBack, enabled = !isLoading) {
                    Text(
                        text = "Edit",
                        color = if (isLoading) colorScheme.onSurface.copy(alpha = 0.38f) else colorScheme.primary,
                        fontSize = 16.sp
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        try {
                            // Build phases list and total budget
                            val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            
                            // Recalculate total budget from department drafts to ensure accuracy
                            val calculatedBudget = phases.sumOf { phase ->
                                phase.departments.sumOf { it.totalBudget }
                            }
                            
                            Log.d("ReviewProjectScreen", "💰 Total budget calculated: $calculatedBudget from ${phases.size} phases")
                            phases.forEachIndexed { idx, phase ->
                                val phaseBudget = phase.departments.sumOf { it.totalBudget }
                                Log.d("ReviewProjectScreen", "  Phase ${idx + 1}: ${phase.phaseName} = $phaseBudget")
                                phase.departments.forEach { dept ->
                                    Log.d("ReviewProjectScreen", "    - ${dept.departmentName}: ${dept.totalBudget}")
                                }
                            }
                            
                            // Format dates for storage
                            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val handoverDateString = handoverDate?.let { dateFormatter.format(it) }
                            val maintenanceDateString = maintenanceDate?.let { dateFormatter.format(it) }
                            
                            // Calculate project endDate from the last phase's end date
                            val projectEndDate = phases.lastOrNull()?.end?.let { endDate ->
                                com.google.firebase.Timestamp(endDate)
                            }
                            
                            if (projectIdForResubmission != null) {
                                // For declined projects (REVIEW REJECTED or DECLINED status), use delete-and-recreate approach
                                // This ensures a fresh project ID and clean slate for resubmission
                                viewModel.resubmitDeclinedProjectAsNew(
                                    oldProjectId = projectIdForResubmission,
                                    projectName = projectName,
                                    description = description,
                                    client = client,
                                    clientPrimaryNumber = clientPrimaryNumber,
                                    clientSecondaryNumber = clientSecondaryNumber,
                                    location = location,
                                    currency = currency,
                                    startDate = plannedStartDate?.let { com.google.firebase.Timestamp(it) } ?: com.google.firebase.Timestamp.now(),
                                    endDate = projectEndDate,
                                    handoverDate = handoverDateString,
                                    maintenanceDate = maintenanceDateString,
                                    totalBudget = calculatedBudget,
                                    managerId = selectedApprover?.phone?.takeIf { it.isNotBlank() },
                                    teamMemberIds = emptyList(),
                                    categories = emptyList(),
                                    phaseDrafts = phases, // Full PhaseDraft data with contractor mode + line items
                                    departmentUserAssignments = departmentUserAssignments,
                                    departmentApproverAssignments = departmentApproverAssignments
                                )
                            } else {
                                // Create new project with PhaseDrafts (includes full department details)
                                viewModel.createProjectWithPhaseDrafts(
                                    projectName = projectName,
                                    description = description,
                                    client = client,
                                    clientPrimaryNumber = clientPrimaryNumber,
                                    clientSecondaryNumber = clientSecondaryNumber,
                                    location = location,
                                    currency = currency,
                                    startDate = plannedStartDate?.let { com.google.firebase.Timestamp(it) } ?: com.google.firebase.Timestamp.now(),
                                    endDate = projectEndDate,
                                    handoverDate = handoverDateString,
                                    maintenanceDate = maintenanceDateString,
                                    totalBudget = calculatedBudget,
                                    managerId = selectedApprover?.phone?.takeIf { it.isNotBlank() },
                                    teamMemberIds = emptyList(),
                                    categories = emptyList(),
                                    phaseDrafts = phases, // Pass PhaseDraft directly
                                    departmentUserAssignments = departmentUserAssignments,
                                    departmentApproverAssignments = departmentApproverAssignments
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("ReviewProjectScreen", "❌ Error creating project: ${e.message}", e)
                            Toast.makeText(context, "Error creating project: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isLoading // Department assignments are validated in NewProjectScreen before navigation
                ) {
                    Text(
                        text = "Confirm",
                        color = colorScheme.primary,
                        fontSize = 16.sp
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colorScheme.surface
            )
        )
        
        // Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project Basics
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Project Basics",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }
                        
                        // Key-Value pairs
                        KeyValueRow("Project Name", projectName)
                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 1.dp)
                        KeyValueRow("Client", client.ifEmpty { "Not specified" })
                        if (!clientPrimaryNumber.isNullOrBlank()) {
                            HorizontalDivider(color = colorScheme.outlineVariant, thickness = 1.dp)
                            KeyValueRow("Client Primary No.", clientPrimaryNumber)
                        }
                        if (!clientSecondaryNumber.isNullOrBlank()) {
                            HorizontalDivider(color = colorScheme.outlineVariant, thickness = 1.dp)
                            KeyValueRow("Client Secondary No.", clientSecondaryNumber)
                        }
                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 1.dp)
                        KeyValueRow("Location", location.ifEmpty { "Not specified" })
                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 1.dp)
                        KeyValueRow("Currency", "$currencySymbol $currency")
                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 1.dp)
                        KeyValueRow("Planned Start Date", plannedStartDate?.let { 
                            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            dateFormatter.format(it)
                        } ?: "Not specified")
                        if (handoverDate != null) {
                            HorizontalDivider(color = colorScheme.outlineVariant, thickness = 1.dp)
                            KeyValueRow("Handover Date", {
                                val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                dateFormatter.format(handoverDate)
                            }())
                        }
                        if (maintenanceDate != null) {
                            HorizontalDivider(color = colorScheme.outlineVariant, thickness = 1.dp)
                            KeyValueRow("Maintenance Date", {
                                val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                dateFormatter.format(maintenanceDate)
                            }())
                        }
                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 1.dp)
                        KeyValueRow("Total Budget", "$currencySymbol ${String.format("%.0f", totalBudget)}")
                    }
                }
            }
            
            // Description
            if (description.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Description",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = description,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            
            // Project Team
            if (selectedApprover != null || selectedTeamMembers.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Project Team",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                            }

                            // Account Manager
                            if (selectedApprover != null) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = colorScheme.primaryContainer.copy(alpha = 0.4f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Account Manager",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = selectedApprover.name.ifBlank { selectedApprover.phone.ifBlank { "—" } },
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colorScheme.onSurface
                                            )
                                            if (selectedApprover.phone.isNotBlank()) {
                                                Text(
                                                    text = selectedApprover.phone,
                                                    fontSize = 12.sp,
                                                    color = colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Team Members
                            if (selectedTeamMembers.isNotEmpty()) {
                                Text(
                                    text = "TEAM MEMBERS (${selectedTeamMembers.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.5.sp
                                )
                                selectedTeamMembers.forEach { member ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = colorScheme.secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = member.name.ifBlank { member.phone.ifBlank { "—" } },
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = colorScheme.onSurface
                                                )
                                                if (member.phone.isNotBlank()) {
                                                    Text(
                                                        text = member.phone,
                                                        fontSize = 12.sp,
                                                        color = colorScheme.onSurfaceVariant
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

            // Department Assignments
            if (allDepartments.isNotEmpty() && (departmentUserAssignments.isNotEmpty() || departmentApproverAssignments.isNotEmpty())) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Department Assignments",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            // Department-wise assignments
                            allDepartments.forEachIndexed { index, department ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = colorScheme.outlineVariant
                                    )
                                }
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Department name
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Business,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = department,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    
                                    // User Assignment
                                    if (departmentUserAssignments.containsKey(department)) {
                                        val userPhone = departmentUserAssignments[department]
                                        if (!userPhone.isNullOrBlank()) {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            text = "User",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = getUserNameFromPhone(userPhone),
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Normal,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Assigned",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Approver Assignment
                                    if (departmentApproverAssignments.containsKey(department)) {
                                        val approverPhone = departmentApproverAssignments[department]
                                        if (!approverPhone.isNullOrBlank()) {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            text = "Approver",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = getUserNameFromPhone(approverPhone),
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Normal,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Assigned",
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(20.dp)
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
            
            // Project Phases
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Project Phases",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Phases
                        phases.forEachIndexed { index, phase ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Phase header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Phase ${index + 1}${if (phase.phaseName.isNotEmpty()) ": ${phase.phaseName}" else ""}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSurface
                                        )
                                        if (phase.phaseName.isNotEmpty()) {
                                            Text(
                                                text = phase.phaseName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = colorScheme.onSurface
                                            )
                                        }
                                        if (phase.start != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DateRange,
                                                    contentDescription = null,
                                                    tint = colorScheme.outlineVariant,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Start: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(phase.start)}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "$currencySymbol ${String.format("%.0f", phase.departments.sumOf { it.totalBudget })}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.primary
                                    )
                                }
                                
                                // Departments
                                if (phase.departments.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "DEPARTMENTS",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = colorScheme.onSurfaceVariant,
                                            letterSpacing = 0.5.sp
                                        )
                                        phase.departments.forEach { dept ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = dept.departmentName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "$currencySymbol ${String.format("%.0f", dept.totalBudget)}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                if (index < phases.size - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = colorScheme.outlineVariant, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    } // end Column

    // Full-screen loading overlay while project is being created/updated
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = colorScheme.primary)
                Text(
                    text = "Creating project...",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    } // end Box

    // Show success dialog when project is created
    LaunchedEffect(successMessage) {
        if (successMessage?.isNotEmpty() == true) {
            showSuccessDialog = true
        }
    }
    
    // Handle error messages
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }
    
    // Success Dialog - Project Status Pop-up
    if (showSuccessDialog) {
        Dialog(
            onDismissRequest = {
                // Don't allow dismissing by clicking outside
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Title
                    Text(
                        text = "Project Status",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    
                    // Message
                    Text(
                        text = "Project created successfully! The project is now IN REVIEW and will be sent to the approver for approval.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                    
                    // OK Button
                    Button(
                        onClick = {
                            showSuccessDialog = false
                            viewModel.clearSuccessMessage()
                            viewModel.clearReviewData()
                            onProjectCreated()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "OK",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyValueRow(key: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = colorScheme.onSurface
        )
    }
}

