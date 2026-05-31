package com.cubiquitous.tracura.ui.view.approver

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.viewmodel.ApproverProjectViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import android.widget.Toast
import androidx.compose.material.icons.filled.Description
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.text.SimpleDateFormat
import java.util.Locale
import com.cubiquitous.tracura.model.ProjectStatus

/**
 * Determines the project status when approved by approver based on planned start date
 * This is called AFTER approval - projects should start as "IN REVIEW" before approval
 * - If planned start date <= current date (today or in the past): ACTIVE
 * - If planned start date > current date (future): LOCKED
 */
private fun determineProjectStatusOnApproval(plannedStartDate: String?): String {
    if (plannedStartDate.isNullOrBlank()) {
        // No start date, default to ACTIVE
        return "ACTIVE"
    }
    
    return try {
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val plannedDate = dateFormatter.parse(plannedStartDate)
        
        if (plannedDate == null) {
            // Could not parse date, default to ACTIVE
            return "ACTIVE"
        }
        
        // Get current date at 12:00 AM (start of day)
        val currentDate = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time
        
        // Get planned date at 12:00 AM (start of day)
        val plannedDateAtMidnight = java.util.Calendar.getInstance().apply {
            time = plannedDate
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time
        
        // If planned start date <= current date (today or past): ACTIVE
        // If planned start date > current date (future): LOCKED
        if (currentDate >= plannedDateAtMidnight) {
            "ACTIVE" // Planned date has arrived or passed
        } else {
            "LOCKED" // Planned date is in the future
        }
    } catch (e: Exception) {
        android.util.Log.e("ApproverProjectReviewScreen", "Error determining project status: ${e.message}", e)
        "ACTIVE" // Default to ACTIVE on error
    }
}

/**
 * iOS-style color palette for Approver Project Review Screen
 */
private data class ApproverReviewPalette(
    val tier1Background: Color,      // Screen background
    val tier2Surface: Color,          // Card background
    val tier3Field: Color,            // Inner elements (dividers, fields)
    val textPrimary: Color,           // Primary text color
    val textSecondary: Color,         // Secondary text color
    val accent: Color,                // Accent color (iOS blue)
    val onAccent: Color,              // Text on accent background
    val hairline: Color,              // Divider lines
    val success: Color,               // Success/approve color
    val destructive: Color            // Reject/error color
)

@Composable
private fun rememberApproverReviewPalette(): ApproverReviewPalette {
    val isDark = isSystemInDarkTheme()
    return remember(isDark) {
        ApproverReviewPalette(
            tier1Background = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7),
            tier2Surface = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
            tier3Field = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF),
            textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
            textSecondary = if (isDark) Color(0xFFEBEBF5).copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f),
            accent = Color(0xFF007AFF),
            onAccent = Color(0xFFFFFFFF),
            hairline = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5E5),
            success = Color(0xFF34C759),
            destructive = Color(0xFFFF3B30)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproverProjectReviewScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    onProjectApproved: () -> Unit,
    viewModel: ApproverProjectViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val iosPalette = rememberApproverReviewPalette()
    
    val projectBudgetSummary by viewModel.projectBudgetSummary.collectAsState()
    val allPhases by viewModel.allPhases.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.user
    
    var isApproving by remember { mutableStateOf(false) }
    var isRejecting by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    
    // Load project data
    LaunchedEffect(projectId) {
        viewModel.loadProjectBudgetSummary(projectId)
    }
    
    val project = projectBudgetSummary.project
    val phases = allPhases.filter { it.isEnabledValue } // Only show enabled phases
    
    // Format currency symbol
    val currencySymbol = when (project?.currency) {
        "INR" -> "₹"
        "USD" -> "$"
        else -> "₹"
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iosPalette.tier1Background)
    ) {
        // Top Bar
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Review Project",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.textPrimary
                )
            },
            navigationIcon = {
                TextButton(onClick = onNavigateBack) {
                    Text(
                        text = "Cancel",
                        color = iosPalette.accent,
                        fontSize = 16.sp
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = iosPalette.tier2Surface
            )
        )
        
        if (isLoading || project == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = iosPalette.accent)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Project Review Header with Icon
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Purple review icon
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Project Review",
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.size(64.dp)
                        )
                        
                        Text(
                            text = "Project Review",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosPalette.textPrimary
                        )
                        
                        Text(
                            text = "Please review all project details before approving or rejecting this project.",
                            fontSize = 14.sp,
                            color = iosPalette.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                
                // Project Basics
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
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
                                    tint = iosPalette.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Project Basics",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = iosPalette.textPrimary
                                )
                            }
                            
                            // Project Code Badge
                            if (project.projectCode.isNotBlank()) {
                                Surface(
                                    color = Color(0xFFE3F2FD), // Sky blue background
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFF2196F3)) // Sky blue border
                                ) {
                                    Text(
                                        text = project.projectCode,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1976D2), // Darker sky blue text
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            
                            // Key-Value pairs
                            KeyValueRow("Project Name", project.name, iosPalette)
                            HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                            // Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Status",
                                    fontSize = 14.sp,
                                    color = iosPalette.textSecondary
                                )
                                val statusColor = ProjectStatus.fromString(project.status).getColor()
                                Surface(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = ProjectStatus.fromString(project.status).getDisplayText(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                            KeyValueRow("Client", project.client.ifEmpty { "Not specified" }, iosPalette)
                            if (!project.clientPrimaryNumber.isNullOrBlank()) {
                                HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                                KeyValueRow("Client Primary No.", project.clientPrimaryNumber!!, iosPalette)
                            }
                            if (!project.clientSecondaryNumber.isNullOrBlank()) {
                                HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                                KeyValueRow("Client Secondary No.", project.clientSecondaryNumber!!, iosPalette)
                            }
                            HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                            KeyValueRow("Location", project.location.ifEmpty { "Not specified" }, iosPalette)
                            HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                            KeyValueRow("Currency", "$currencySymbol ${project.currency}", iosPalette)
                            HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                            KeyValueRow("Total Budget", "$currencySymbol ${String.format("%.2f", project.budget)}", iosPalette)
                            if (project.remainingBalance != null) {
                                HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                                KeyValueRow("Remaining Balance", "$currencySymbol ${String.format("%.2f", project.remainingBalance!!)}", iosPalette)
                            }
                            if (project.spent != null) {
                                HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                                KeyValueRow("Amount Spent", "$currencySymbol ${String.format("%.2f", project.spent!!)}", iosPalette)
                            }
                            val plannedStartDate = project.plannedDate ?: project.startDate
                            if (!plannedStartDate.isNullOrBlank()) {
                                HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                                KeyValueRow("Planned Start Date", plannedStartDate, iosPalette)
                            }
                            if (!project.endDate.isNullOrBlank()) {
                                HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                                KeyValueRow("End Date", project.endDate!!, iosPalette)
                            }
                            if (!project.handoverDate.isNullOrBlank()) {
                                HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                                KeyValueRow("Handover Date", project.handoverDate!!, iosPalette)
                            }
                            if (!project.maintenanceDate.isNullOrBlank()) {
                                HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                                KeyValueRow("Maintenance Date", project.maintenanceDate!!, iosPalette)
                            }
                            if (project.Allow_Template_Overrides != null) {
                                HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                                KeyValueRow("Template Overrides", if (project.Allow_Template_Overrides == true) "Allowed" else "Not Allowed", iosPalette)
                            }
                            if (!project.code.isNullOrBlank()) {
                                HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                                KeyValueRow("Legacy Code", project.code!!, iosPalette)
                            }
                            HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                            KeyValueRow("Created At", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(project.createdAt.toDate()), iosPalette)
                            HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                            KeyValueRow("Updated At", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(project.updatedAt.toDate()), iosPalette)
                        }
                    }
                }
                
                // Description
                if (!project.description.isNullOrEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = iosPalette.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Description",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = iosPalette.textPrimary
                                    )
                                }
                                Text(
                                    text = project.description,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = iosPalette.textPrimary
                                )
                            }
                        }
                    }
                }
                
                // Project Team
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "PROJECT TEAM",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = iosPalette.textSecondary,
                                letterSpacing = 0.5.sp
                            )
                            
                            // Manager
                            if (!project.managerId.isNullOrEmpty()) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "MANAGER",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = iosPalette.textSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(iosPalette.accent, shape = RoundedCornerShape(4.dp))
                                        )
                                        Text(
                                            text = project.managerId!!,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = iosPalette.textPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            
                            // Team Members
                            if (project.teamMembers.isNotEmpty()) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "TEAM MEMBERS (${project.teamMembers.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = iosPalette.textSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    project.teamMembers.forEach { memberId ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(iosPalette.accent, shape = RoundedCornerShape(4.dp))
                                            )
                                            Text(
                                                text = memberId,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = iosPalette.textPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // Approvers
                            if (!project.approverIds.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "APPROVERS (${project.approverIds!!.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = iosPalette.textSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    project.approverIds!!.forEach { approverId ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(Color(0xFF34C759), shape = RoundedCornerShape(4.dp))
                                            )
                                            Text(
                                                text = approverId,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = iosPalette.textPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // Business Heads
                            if (!project.BusinessHeadIds.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "BUSINESS HEADS (${project.BusinessHeadIds!!.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = iosPalette.textSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    project.BusinessHeadIds!!.forEach { bhId ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(Color(0xFF9C27B0), shape = RoundedCornerShape(4.dp))
                                            )
                                            Text(
                                                text = bhId,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = iosPalette.textPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // Temp Approver ID
                            if (!project.tempApproverID.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "TEMP APPROVER ID",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = iosPalette.textSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFFFF9800), shape = RoundedCornerShape(4.dp))
                                        )
                                        Text(
                                            text = project.tempApproverID!!,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = iosPalette.textPrimary
                                        )
                                    }
                                }
                            }

                            // Temporary Approver Phone
                            if (!project.temporaryApproverPhone.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "TEMPORARY APPROVER PHONE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = iosPalette.textSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFFFF9800), shape = RoundedCornerShape(4.dp))
                                        )
                                        Text(
                                            text = project.temporaryApproverPhone!!,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = iosPalette.textPrimary
                                        )
                                    }
                                }
                            }

                            // Department User Assignments
                            if (project.departmentUserAssignments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "DEPARTMENT USER ASSIGNMENTS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = iosPalette.textSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    project.departmentUserAssignments.forEach { (dept, userPhone) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = dept, fontSize = 14.sp, color = iosPalette.textSecondary)
                                            Text(text = userPhone, fontSize = 14.sp, color = iosPalette.textPrimary)
                                        }
                                    }
                                }
                            }

                            // Department Approver Assignments
                            if (project.departmentApproverAssignments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "DEPARTMENT APPROVER ASSIGNMENTS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = iosPalette.textSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    project.departmentApproverAssignments.forEach { (dept, approverPhone) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = dept, fontSize = 14.sp, color = iosPalette.textSecondary)
                                            Text(text = approverPhone, fontSize = 14.sp, color = iosPalette.textPrimary)
                                        }
                                    }
                                }
                            }

                            // Department Temporary Approvers
                            if (project.departmentTemporaryApprover.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "DEPT. TEMPORARY APPROVERS (${project.departmentTemporaryApprover.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = iosPalette.textSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    project.departmentTemporaryApprover.forEach { entry ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            KeyValueRow("Dept", entry.departmentName.ifEmpty { "—" }, iosPalette)
                                            KeyValueRow("Phone", entry.phone.ifEmpty { "—" }, iosPalette)
                                            KeyValueRow("Active", if (entry.isActive) "Yes" else "No", iosPalette)
                                            KeyValueRow("Accepted", if (entry.isAccepted) "Yes" else "No", iosPalette)
                                            if (entry.startDate != null) {
                                                KeyValueRow("Start", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(entry.startDate), iosPalette)
                                            }
                                            if (entry.endDate != null) {
                                                KeyValueRow("End", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(entry.endDate), iosPalette)
                                            }
                                            HorizontalDivider(color = iosPalette.hairline, thickness = 0.5.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Categories (Legacy)
                if (!project.categories.isNullOrEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
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
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = iosPalette.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Categories",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = iosPalette.textPrimary
                                    )
                                }
                                project.categories!!.forEachIndexed { idx, cat ->
                                    if (idx > 0) HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                                    Text(text = cat, fontSize = 14.sp, color = iosPalette.textPrimary)
                                }
                            }
                        }
                    }
                }

                // Department Budgets (Legacy)
                if (!project.departmentBudgets.isNullOrEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
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
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = iosPalette.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Department Budgets",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = iosPalette.textPrimary
                                    )
                                }
                                project.departmentBudgets!!.entries.toList().forEachIndexed { idx, entry ->
                                    if (idx > 0) HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = entry.key, fontSize = 14.sp, color = iosPalette.textSecondary)
                                        Text(
                                            text = "$currencySymbol ${String.format("%.2f", entry.value)}",
                                            fontSize = 14.sp,
                                            color = iosPalette.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Rejection Info
                if (!project.rejectionReason.isNullOrBlank() || !project.rejectedBy.isNullOrBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
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
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color(0xFFFF3B30),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Rejection Info",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF3B30)
                                    )
                                }
                                if (!project.rejectedBy.isNullOrBlank()) {
                                    KeyValueRow("Rejected By", project.rejectedBy!!, iosPalette)
                                }
                                if (!project.rejectionReason.isNullOrBlank()) {
                                    if (!project.rejectedBy.isNullOrBlank()) {
                                        HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                                    }
                                    Text(text = "Reason", fontSize = 12.sp, color = iosPalette.textSecondary)
                                    Text(text = project.rejectionReason!!, fontSize = 14.sp, color = iosPalette.textPrimary)
                                }
                            }
                        }
                    }
                }

                // Suspension Info
                if (project.isSuspended == true || !project.suspensionReason.isNullOrBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
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
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Suspension Info",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                                KeyValueRow("Suspended", if (project.isSuspended == true) "Yes" else "No", iosPalette)
                                if (!project.suspendedDate.isNullOrBlank()) {
                                    HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                                    KeyValueRow("Suspended Date", project.suspendedDate!!, iosPalette)
                                }
                                if (!project.suspensionReason.isNullOrBlank()) {
                                    HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                                    Text(text = "Reason", fontSize = 12.sp, color = iosPalette.textSecondary)
                                    Text(text = project.suspensionReason!!, fontSize = 14.sp, color = iosPalette.textPrimary)
                                }
                            }
                        }
                    }
                }

                // Project Phases
                if (phases.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
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
                                        tint = iosPalette.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Project Phases",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = iosPalette.textPrimary
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
                                                    text = "Phase ${index + 1}",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = iosPalette.textPrimary
                                                )
                                                if (phase.phaseName.isNotEmpty()) {
                                                    Text(
                                                        text = phase.phaseName,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        color = iosPalette.textPrimary
                                                    )
                                                }
                                                if (phase.startDate != null) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DateRange,
                                                            contentDescription = null,
                                                            tint = iosPalette.textSecondary,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = "Start: ${phase.startDate} • End: ${phase.endDate ?: "Not set"}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Normal,
                                                            color = iosPalette.textSecondary
                                                        )
                                                    }
                                                }
                                            }
                                            val phaseBudget = phase.departments.values.sum()
                                            Text(
                                                text = "$currencySymbol ${String.format("%.0f", phaseBudget)}",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = iosPalette.accent
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
                                                    color = iosPalette.textSecondary,
                                                    letterSpacing = 0.5.sp
                                                )
                                                phase.departments.forEach { entry ->
                                                    val deptName = entry.key
                                                    val budget = entry.value
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = deptName,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Normal,
                                                            color = iosPalette.textPrimary
                                                        )
                                                        Text(
                                                            text = "$currencySymbol ${String.format("%.0f", budget)}",
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Normal,
                                                            color = iosPalette.textPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        if (index < phases.size - 1) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Approve Button
                item {
                    Button(
                        onClick = {
                            isApproving = true
                            scope.launch {
                                try {
                                    // Determine status based on planned start date
                                    val newStatus = determineProjectStatusOnApproval(project?.startDate)
                                    
                                    val result = viewModel.updateProjectStatus(projectId, newStatus)
                                    if (result.isSuccess) {
                                        val statusMessage = when (newStatus) {
                                            "ACTIVE" -> "Project approved and activated successfully"
                                            "LOCKED" -> "Project approved and locked until start date"
                                            else -> "Project approved successfully"
                                        }
                                        Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
                                        onProjectApproved()
                                    } else {
                                        Toast.makeText(context, "Error approving project: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        isApproving = false
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error approving project: ${e.message}", Toast.LENGTH_LONG).show()
                                    isApproving = false
                                }
                            }
                        },
                        enabled = !isApproving && !isRejecting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = iosPalette.success,
                            disabledContainerColor = Color(0xFF8E8E93)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isApproving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = iosPalette.onAccent
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = iosPalette.onAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Approve Project",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = iosPalette.onAccent
                                )
                            }
                        }
                    }
                }
                
                // Reject Button
                item {
                    Button(
                        onClick = {
                            showRejectDialog = true
                        },
                        enabled = !isApproving && !isRejecting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEBEE),
                            disabledContainerColor = Color(0xFF8E8E93)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = iosPalette.destructive,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reject Project",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosPalette.destructive
                            )
                        }
                    }
                }
                
                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
    
    // Reject Project Dialog
    if (showRejectDialog) {
        Dialog(
            onDismissRequest = {
                if (!isRejecting) {
                    showRejectDialog = false
                    rejectionReason = ""
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = !isRejecting,
                dismissOnClickOutside = !isRejecting
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header with Cancel button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reject Project",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosPalette.textPrimary
                        )
                        TextButton(onClick = {
                            if (!isRejecting) {
                                showRejectDialog = false
                                rejectionReason = ""
                            }
                        }) {
                            Text(
                                text = "Cancel",
                                color = iosPalette.accent,
                                fontSize = 16.sp
                            )
                        }
                    }
                    
                    // Red X Icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.CenterHorizontally)
                            .background(
                                Color(0xFFFFEBEE),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Reject",
                            tint = iosPalette.destructive,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    // Title
                    Text(
                        text = "Reject Project",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    // Instructional text
                    Text(
                        text = "Please provide a reason for rejecting this project. The admin will be able to edit and resubmit the project for review.",
                        fontSize = 14.sp,
                        color = iosPalette.textSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 20.sp
                    )
                    
                    // Reason label
                    Text(
                        text = "Reason for Rejection",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary
                    )
                    
                    // Text input field
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        placeholder = {
                            Text(
                                text = "Enter your reason...",
                                color = iosPalette.textSecondary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = iosPalette.accent,
                            unfocusedBorderColor = iosPalette.hairline
                        ),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 5,
                        minLines = 3,
                        enabled = !isRejecting,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel button
                        Button(
                            onClick = {
                                if (!isRejecting) {
                                    showRejectDialog = false
                                    rejectionReason = ""
                                }
                            },
                            enabled = !isRejecting,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE3F2FD),
                                disabledContainerColor = Color(0xFF8E8E93)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosPalette.accent
                            )
                        }
                        
                        // Confirm Rejection button
                        Button(
                            onClick = {
                                if (rejectionReason.isNotBlank()) {
                                    isRejecting = true
                                    scope.launch {
                                        try {
                                            // Get current user's document ID (phone or uid) for rejectedBy
                                            val rejectedBy = currentUser?.phone ?: currentUser?.uid ?: ""
                                            val result = viewModel.updateProjectStatus(projectId, "DECLINED", rejectionReason, rejectedBy)
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "Project rejected", Toast.LENGTH_SHORT).show()
                                                showRejectDialog = false
                                                rejectionReason = ""
                                                onProjectApproved() // Navigate back
                                            } else {
                                                Toast.makeText(context, "Error rejecting project: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                isRejecting = false
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error rejecting project: ${e.message}", Toast.LENGTH_LONG).show()
                                            isRejecting = false
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Please provide a reason for rejection", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isRejecting && rejectionReason.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = iosPalette.destructive,
                                disabledContainerColor = Color(0xFF8E8E93)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isRejecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = iosPalette.onAccent
                                )
                            } else {
                                Text(
                                    text = "Confirm Rejection",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = iosPalette.onAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyValueRow(key: String, value: String, palette: ApproverReviewPalette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = palette.textSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = palette.textPrimary
        )
    }
}
