package com.cubiquitous.tracura.ui.view.approver

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.cubiquitous.tracura.model.ContractorMode
import com.cubiquitous.tracura.model.Department
import com.cubiquitous.tracura.model.DepartmentLineItemData
import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.ExpenseStatus
import com.cubiquitous.tracura.model.LineItem
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.DetailedExpense
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.viewmodel.ReportsViewModel
import com.cubiquitous.tracura.viewmodel.ProjectViewModel
import com.cubiquitous.tracura.viewmodel.ApproverProjectViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.model.UserRole
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.rememberDatePickerState
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.cubiquitous.tracura.ui.common.DepartmentLineItemsSection
import com.cubiquitous.tracura.ui.view.businesshead.TeamMembersDetailScreen

private data class IosDepartmentPalette(
    val tier1Background: Color,
    val tier2Surface: Color,
    val tier3Field: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val hairline: Color,
    val danger: Color,
    val info: Color,
    val warning: Color,
    val success: Color
)

@Composable
private fun rememberIosDepartmentPalette(accentColor: Color = MaterialTheme.colorScheme.primary): IosDepartmentPalette {
    val isDark = isSystemInDarkTheme()
    return remember(isDark, accentColor) {
        IosDepartmentPalette(
            tier1Background = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7),
            tier2Surface = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
            tier3Field = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF),
            textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
            textSecondary = if (isDark) Color(0x99EBEBF5) else Color(0x993C3C43),
            accent = accentColor,
            onAccent = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF),
            hairline = if (isDark) Color(0xFF38383A) else Color(0x1F000000),
            danger = Color(0xFFD32F2F),
            info = Color(0xFF0A84FF),
            warning = Color(0xFFFF9F0A),
            success = Color(0xFF30D158)
        )
    }
}

// Reusable composable for truncated text with popup
@Composable
private fun TruncatedTextWithPopup(
    text: String,
    maxLength: Int = 10,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val iosPalette = rememberIosDepartmentPalette()
    var showPopup by remember { mutableStateOf(false) }
    val shouldTruncate = text.length > maxLength
    val displayText = if (shouldTruncate) text.take(maxLength) else text
    
    val resolvedColor = if (color == Color.Unspecified) iosPalette.textPrimary else color
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayText,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = resolvedColor
        )
        if (shouldTruncate) {
            Text(
                text = "...",
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = resolvedColor,
                modifier = Modifier
                    .clickable { showPopup = true }
                    .padding(start = 2.dp)
            )
        }
    }
    
    // Popup dialog to show full text
    if (showPopup) {
        Dialog(
            onDismissRequest = { showPopup = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
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
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = text,
                        fontSize = 16.sp,
                        color = iosPalette.textPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showPopup = false }) {
                            Text(
                                text = "Close",
                                color = iosPalette.accent,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentDetailScreen(
    projectId: String,
    departmentName: String,
    onNavigateBack: () -> Unit,
    onNavigateToExpenseChat: (String) -> Unit = {},
    onNavigateToReview: (String) -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    // Optional budget values passed from parent screen (to avoid redundant fetching)
    initialBudgetAllocated: Double? = null,
    initialSpent: Double? = null,
    initialRemaining: Double? = null,
    reportsViewModel: ReportsViewModel = hiltViewModel(),
    projectViewModel: ProjectViewModel = hiltViewModel(),
    approverProjectViewModel: ApproverProjectViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    backStackEntry: NavBackStackEntry? = null
) {
    val iosPalette = rememberIosDepartmentPalette()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var showLineItemsSheet by remember { mutableStateOf(false) }
    var showEditDepartmentSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCannotDeleteDialog by remember { mutableStateOf(false) }
    var showFinishDepartmentDialog by remember { mutableStateOf(false) }
    var showRevertDepartmentDialog by remember { mutableStateOf(false) }
    var isLoadingDelete by remember { mutableStateOf(false) }
    var isFinishingDepartment by remember { mutableStateOf(false) }
    var isRevertingDepartment by remember { mutableStateOf(false) }
    var editDepartmentLineItems by remember { mutableStateOf<List<LineItem>>(emptyList()) }
    var editContractorMode by remember { mutableStateOf(ContractorMode.LABOUR_ONLY) }
    var editContractorAmountText by remember { mutableStateOf("") }
    var isSavingEditDepartment by remember { mutableStateOf(false) }
    val reportData by reportsViewModel.reportData.collectAsState()
    val isLoading by reportsViewModel.isLoading.collectAsState()
    val projects by projectViewModel.projects.collectAsState()
    val currentProject = projects.find { it.id == projectId }
    val authState by authViewModel.authState.collectAsState()
    val userRole = authViewModel.authState.value.user?.role
    val isBusinessHead = userRole == UserRole.BUSINESS_HEAD || userRole == UserRole.MANAGER
    val isApprover = userRole == UserRole.APPROVER
    
    // Budget values are passed via initialBudgetAllocated, initialSpent, initialRemaining parameters
    // These are set in AppNavHost from the previous backStackEntry's savedStateHandle
    // So we should use the initial parameters directly - they're already passed correctly
    val savedBudgetAllocated: Double? = null // Values come from initial parameters, not savedStateHandle
    val savedSpent: Double? = null
    val savedRemaining: Double? = null
    
    // Use initial parameters directly - they're passed from AppNavHost which reads from previous backStackEntry
    val providedBudgetAllocated = initialBudgetAllocated
    val providedSpent = initialSpent
    val providedRemaining = initialRemaining
    
    // Check if budget values are provided (don't need to fetch budget data)
    // If ANY budget value is provided, we should use them (even if some are null, use what we have)
    val hasBudgetValues = providedBudgetAllocated != null || providedSpent != null || providedRemaining != null
    
    // Log to verify values are received
    LaunchedEffect(providedBudgetAllocated, providedSpent, providedRemaining) {
        Log.d("DepartmentDetailScreen", "📊 Budget values received: allocated=$providedBudgetAllocated, spent=$providedSpent, remaining=$providedRemaining, hasValues=$hasBudgetValues")
    }
    
    // Load ONLY expenses when screen starts - budget values come from parent
    LaunchedEffect(projectId) {
        try {
            Log.d("DepartmentDetailScreen", "🔄 LaunchedEffect triggered for projectId: $projectId")
            
            // Get current state values for conditional loading
            val currentReportData = reportData.detailedExpenses
            val currentIsLoading = isLoading
            val currentProjects = projects
            
            Log.d("DepartmentDetailScreen", "📊 Current state: reportData.size=${currentReportData.size}, isLoading=$currentIsLoading, projects.size=${currentProjects.size}")
            
            // Parallel loading - ONLY expenses and projects (no budget fetching)
            coroutineScope {
                // 1. Load reports data (expenses) - ALWAYS fetch for this project (check if data is for different project)
                val reportsDeferred = if (!currentIsLoading) {
                    // Always reload to ensure we have fresh data for this project
                    async {
                        Log.d("DepartmentDetailScreen", "🚀 Loading reports for project: $projectId")
                        reportsViewModel.loadReportsForProject(projectId)
                        Log.d("DepartmentDetailScreen", "✅ Reports loading completed")
                    }
                } else {
                    Log.d("DepartmentDetailScreen", "⏸️ Skipping reports load - already loading")
                    null
                }
                
                // 2. Load projects list - only if empty (needed for project info)
                val projectsDeferred = if (currentProjects.isEmpty()) {
                    async {
                        projectViewModel.loadProjects()
                    }
                } else {
                    null
                }
                
                // 3. Load budget data ONLY if not provided from parent
                val budgetDataDeferred = if (!hasBudgetValues) {
                    // Need to fetch phases to calculate budget
                    val currentPhases = approverProjectViewModel.allPhases.value
                    val currentPhaseBreakdowns = approverProjectViewModel.phaseBreakdowns.value
                    if (currentPhases.isEmpty() || currentPhaseBreakdowns.isEmpty()) {
                        async {
                            approverProjectViewModel.loadProjectBudgetSummary(projectId)
                        }
                    } else {
                        null
                    }
                } else {
                    null // Budget values provided, skip fetching
                }
                
                // Wait for all parallel operations to complete
                reportsDeferred?.await()
                projectsDeferred?.await()
                budgetDataDeferred?.await()
                
                Log.d("DepartmentDetailScreen", "✅ All data loading completed")
            }
        } catch (e: Exception) {
            Log.e("DepartmentDetailScreen", "❌ Error loading data: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    // Debug: Log when reportData changes
    LaunchedEffect(reportData.detailedExpenses.size) {
        Log.d("DepartmentDetailScreen", "📊 reportData.detailedExpenses changed: ${reportData.detailedExpenses.size} expenses")
        if (reportData.detailedExpenses.isNotEmpty()) {
            Log.d("DepartmentDetailScreen", "🎯 Sample expenses:")
            reportData.detailedExpenses.take(3).forEach { expense ->
                Log.d("DepartmentDetailScreen", "  - ${expense.id}: ${expense.department} - ${expense.status} - ₹${expense.amount}")
            }
        }
    }
    
    // Get phase breakdowns and phases with departments (Firebase) from view model
    val phaseBreakdowns by approverProjectViewModel.phaseBreakdowns.collectAsState()
    val allPhases by approverProjectViewModel.allPhases.collectAsState()
    val phasesWithDepartments by approverProjectViewModel.phasesWithDepartments.collectAsState()

    // Get all anonymous (deleted) departments from all phases
    val anonymousDepartments = remember(allPhases) {
        allPhases.flatMap { phase ->
            phase.isanonymous.filter { it.value == true }.keys
        }.distinct()
    }
    
    // Check if this is phase-specific "Others" (format: "Others_phaseId")
    val isPhaseSpecificOthers = departmentName.startsWith("Others_")
    val specificPhaseId = if (isPhaseSpecificOthers) {
        departmentName.substringAfter("Others_")
    } else null
    
    // Extract phaseId from department name if it's in format "phaseId_departmentName"
    // This is used to filter budget and expenses to only show data from that specific phase
    val extractedPhaseId = remember(departmentName, allPhases) {
        if (departmentName.contains("_") && !isPhaseSpecificOthers) {
            // Extract phaseId from format "phaseId_departmentName"
            val parts = departmentName.split("_", limit = 2)
            if (parts.size == 2) {
                val possiblePhaseId = parts[0]
                // Verify this is actually a phaseId by checking if it exists in allPhases
                val phaseExists = allPhases.any { it.id == possiblePhaseId }
                if (phaseExists) {
                    possiblePhaseId
                } else {
                    null
                }
            } else {
                null
            }
        } else {
            null
        }
    }
    
    // Extract the display department name (without phaseId prefix)
    val displayDepartmentName = remember(departmentName, extractedPhaseId) {
        if (extractedPhaseId != null) {
            // Remove the phaseId prefix to get the actual department name
            departmentName.substringAfter("${extractedPhaseId}_")
        } else {
            // Use FormatUtils to extract display name (handles backward compatibility)
            FormatUtils.getDepartmentDisplayName(departmentName)
        }
    }

    // Resolve department line items from Firebase (phases/{phaseId}/departments subcollection)
    val departmentLineItems = remember(
        phasesWithDepartments,
        extractedPhaseId,
        displayDepartmentName,
        departmentName,
        isPhaseSpecificOthers
    ) {
        if (departmentName == "Others" || isPhaseSpecificOthers) return@remember emptyList<DepartmentLineItemData>()
        val phasesWithDepts = phasesWithDepartments
        if (phasesWithDepts.isEmpty()) return@remember emptyList()
        val phaseToUse = if (extractedPhaseId != null) {
            phasesWithDepts.firstOrNull { it.phase.id == extractedPhaseId }
        } else {
            phasesWithDepts.firstOrNull()
        } ?: return@remember emptyList()
        val dept = phaseToUse.departments.firstOrNull { dept ->
            dept.name.equals(displayDepartmentName, ignoreCase = true) ||
                dept.name.equals(departmentName, ignoreCase = true) ||
                (extractedPhaseId != null && dept.name.equals("${extractedPhaseId}_$displayDepartmentName", ignoreCase = true))
        }
        dept?.lineItems ?: emptyList()
    }

    // Full department for current phase (for Edit Department)
    val currentDepartment: Department? = remember(
        phasesWithDepartments,
        extractedPhaseId,
        displayDepartmentName,
        departmentName,
        isPhaseSpecificOthers
    ) {
        if (departmentName == "Others" || isPhaseSpecificOthers) return@remember null
        val phasesWithDepts = phasesWithDepartments
        if (phasesWithDepts.isEmpty()) return@remember null
        val phaseToUse = if (extractedPhaseId != null) {
            phasesWithDepts.firstOrNull { it.phase.id == extractedPhaseId }
        } else {
            phasesWithDepts.firstOrNull()
        } ?: return@remember null
        phaseToUse.departments.firstOrNull { dept ->
            dept.name.equals(displayDepartmentName, ignoreCase = true) ||
                dept.name.equals(departmentName, ignoreCase = true) ||
                (extractedPhaseId != null && dept.name.equals("${extractedPhaseId}_$displayDepartmentName", ignoreCase = true))
        }
    }

    val isDepartmentFinished = currentDepartment?.isFinished == true
    val shouldBlockAllInteractions = isDepartmentFinished && !isBusinessHead

    // Get the specific phase's anonymous departments if this is phase-specific
    val phaseSpecificAnonymousDepartments = remember(allPhases, specificPhaseId) {
        if (specificPhaseId != null) {
            val phase = allPhases.firstOrNull { it.id == specificPhaseId }
            phase?.isanonymous?.filter { it.value == true }?.keys?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    // Filter expenses for this specific department
    // If departmentName is "Others" or "Others_phaseId", show expenses from anonymous departments
    val departmentExpenses = remember(reportData.detailedExpenses, departmentName, anonymousDepartments, phaseSpecificAnonymousDepartments, specificPhaseId) {
        val filtered = when {
            isPhaseSpecificOthers && specificPhaseId != null -> {
                // Show expenses from anonymous departments of this specific phase only
                Log.d("DepartmentDetailScreen", "🔍 Filtering phase-specific Others: phaseId='$specificPhaseId'")
                Log.d("DepartmentDetailScreen", "📋 Anonymous departments for this phase: $phaseSpecificAnonymousDepartments")
                Log.d("DepartmentDetailScreen", "📊 Total expenses: ${reportData.detailedExpenses.size}")
                
                // If no anonymous departments, return empty list
                if (phaseSpecificAnonymousDepartments.isEmpty()) {
                    Log.d("DepartmentDetailScreen", "⚠️ No anonymous departments found for this phase")
                    return@remember emptyList()
                }
                
                val result = reportData.detailedExpenses.filter { expense ->
                    // Match by department name first (more reliable)
                    val deptMatch = phaseSpecificAnonymousDepartments.any { anonymousDept ->
                        val expenseDept = expense.department.trim()
                        val anonymousDeptTrimmed = anonymousDept.trim()
                        val matches = expenseDept.equals(anonymousDeptTrimmed, ignoreCase = true)
                        
                        if (matches) {
                            Log.d("DepartmentDetailScreen", "✅ Department match: '$expenseDept' == '$anonymousDeptTrimmed'")
                        }
                        
                        matches
                    }
                    
                    // If department matches, check phaseId (if phaseId is empty, still include it)
                    val phaseMatch = if (expense.phaseId.isBlank()) {
                        Log.d("DepartmentDetailScreen", "⚠️ Expense ${expense.id} has empty phaseId, including anyway")
                        true // Include expenses with empty phaseId if department matches
                    } else {
                        val matches = expense.phaseId.trim().equals(specificPhaseId.trim(), ignoreCase = true)
                        if (matches) {
                            Log.d("DepartmentDetailScreen", "✅ Phase match: '${expense.phaseId}' == '$specificPhaseId'")
                        } else {
                            Log.d("DepartmentDetailScreen", "❌ Phase mismatch: '${expense.phaseId}' != '$specificPhaseId'")
                        }
                        matches
                    }
                    
                    val shouldInclude = deptMatch && (phaseMatch || expense.phaseId.isBlank())
                    
                    if (shouldInclude) {
                        Log.d("DepartmentDetailScreen", "✅ Including expense: ${expense.id} - Phase: '${expense.phaseId}', Dept: '${expense.department}', Status: ${expense.status}, Amount: ₹${expense.amount}")
                    }
                    
                    shouldInclude
                }
                
                Log.d("DepartmentDetailScreen", "✅ Filtered ${result.size} expenses for phase-specific Others")
                result
            }
            departmentName == "Others" -> {
                // Show expenses from all anonymous (deleted) departments across all phases
                Log.d("DepartmentDetailScreen", "🔍 Filtering global Others")
                Log.d("DepartmentDetailScreen", "📋 Anonymous departments: $anonymousDepartments")
                Log.d("DepartmentDetailScreen", "📊 Total expenses: ${reportData.detailedExpenses.size}")
                
                // If no anonymous departments, return empty list
                if (anonymousDepartments.isEmpty()) {
                    Log.d("DepartmentDetailScreen", "⚠️ No anonymous departments found")
                    return@remember emptyList()
                }
                
                val result = reportData.detailedExpenses.filter { expense ->
                    val deptMatch = anonymousDepartments.any { anonymousDept ->
                        val expenseDept = expense.department.trim()
                        val anonymousDeptTrimmed = anonymousDept.trim()
                        val matches = expenseDept.equals(anonymousDeptTrimmed, ignoreCase = true)
                        
                        if (matches) {
                            Log.d("DepartmentDetailScreen", "✅ Department match: '$expenseDept' == '$anonymousDeptTrimmed'")
                        }
                        
                        matches
                    }
                    
                    if (deptMatch) {
                        Log.d("DepartmentDetailScreen", "✅ Including expense: ${expense.id} - Phase: '${expense.phaseId}', Dept: '${expense.department}', Status: ${expense.status}, Amount: ₹${expense.amount}")
                    }
                    
                    deptMatch
                }
                
                Log.d("DepartmentDetailScreen", "✅ Filtered ${result.size} expenses for global Others")
                result
            }
            else -> {
                // Match expense.department against displayDepartmentName (just the name, no phaseId prefix)
                // and match expense.phaseId against extractedPhaseId when available
                reportData.detailedExpenses.filter { expense ->
                    val departmentMatches = expense.department.equals(displayDepartmentName, ignoreCase = true)
                    if (extractedPhaseId != null) {
                        departmentMatches && expense.phaseId.equals(extractedPhaseId, ignoreCase = true)
                    } else {
                        departmentMatches
                    }
                }
            }
        }
        
        // Log all filtered expenses
        Log.d("DepartmentDetailScreen", "📋 Final filtered expenses (${filtered.size}):")
        filtered.forEach { expense ->
            Log.d("DepartmentDetailScreen", "  - ${expense.id}: ${expense.department} (${expense.status}) - ₹${expense.amount}")
        }
        
        filtered
    }
    
    // Use provided budget values if available, otherwise calculate from fetched data
    val departmentBudget = remember(
        providedBudgetAllocated, 
        hasBudgetValues,
        departmentName, 
        phaseBreakdowns, 
        currentProject, 
        isPhaseSpecificOthers, 
        extractedPhaseId,
        allPhases,
        currentDepartment
    ) {
        // If we have a full Department object from subcollection, use its totalBudget
        if (currentDepartment != null && !isPhaseSpecificOthers && departmentName != "Others") {
            return@remember currentDepartment.totalBudget
        }

        // If budget values are provided from parent, use them directly - NO CALCULATION
        if (hasBudgetValues && providedBudgetAllocated != null) {
            // Use provided value directly - don't calculate
            providedBudgetAllocated
        } else if (hasBudgetValues) {
            // If we have other budget values but not allocated, use 0 (for "Others" case)
            0.0
        } else {
            // Calculate budget (fallback when not provided from parent)
            if (departmentName == "Others" || isPhaseSpecificOthers) {
                0.0
            } else {
                var totalBudget = 0.0
                
                if (extractedPhaseId != null) {
                    val specificPhase = allPhases.find { it.id == extractedPhaseId }
                    if (specificPhase != null) {
                        specificPhase.departments.forEach { (deptKey, budget) ->
                            if (deptKey.trim().equals(departmentName.trim(), ignoreCase = true)) {
                                totalBudget = budget
                            }
                        }
                    }
                    
                    if (totalBudget == 0.0) {
                        val specificPhaseBreakdown = phaseBreakdowns.find { it.phaseId == extractedPhaseId }
                        specificPhaseBreakdown?.departments?.forEach { dept ->
                            val deptName = dept.department.trim()
                            val displayName = displayDepartmentName.trim()
                            if (deptName.equals(departmentName.trim(), ignoreCase = true) ||
                                deptName.equals(displayName, ignoreCase = true)) {
                                totalBudget = dept.budgetAllocated
                            }
                        }
                    }
                } else {
                    phaseBreakdowns.forEach { phaseBreakdown ->
                        phaseBreakdown.departments.forEach { dept ->
                            if (dept.department.trim().equals(departmentName.trim(), ignoreCase = true)) {
                                totalBudget += dept.budgetAllocated
                            }
                        }
                    }
                    
                    if (totalBudget == 0.0 && currentProject?.departmentBudgets != null) {
                        currentProject.departmentBudgets.forEach { (deptName, budget) ->
                            if (deptName.trim().equals(departmentName.trim(), ignoreCase = true)) {
                                totalBudget += budget
                            }
                        }
                    }
                }
                
                totalBudget
            }
        }
    }
    
    // Remaining is the source of truth: use Department subcollection value, then provided, then derived
    val remaining = remember(providedRemaining, departmentBudget, currentDepartment) {
        if (currentDepartment != null && !isPhaseSpecificOthers && departmentName != "Others") {
            return@remember currentDepartment.remainingAmount
        }
        providedRemaining ?: (departmentBudget - (providedSpent ?: 0.0))
    }

    // Spent is always derived from budget and remaining — single source of truth
    val totalSpent = departmentBudget - remaining
    
    // Calculate budget utilization with proper precision (use Double, not Int)
    // Show percentage even for very small amounts (e.g., ₹1)
    // If budget is 0, show 0% (can't calculate utilization without a budget)
    val budgetUtilization = if (departmentBudget > 0) {
        val utilization = (totalSpent / departmentBudget) * 100.0
        // Ensure we show at least 2 decimal places for small amounts
        utilization
    } else {
        0.0
    }
    val utilizationSeverity = budgetUtilizationSeverity(budgetUtilization)

    val isWithinCurrentPhaseDateRange = remember(allPhases, extractedPhaseId) {
        val phase = allPhases.firstOrNull { it.id == extractedPhaseId } ?: return@remember false
        if (phase.startDate.isNullOrBlank() || phase.endDate.isNullOrBlank()) return@remember false

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val start = try {
            dateFormat.parse(phase.startDate)
        } catch (_: Exception) {
            null
        } ?: return@remember false

        val end = try {
            dateFormat.parse(phase.endDate)
        } catch (_: Exception) {
            null
        } ?: return@remember false

        fun Date.startOfDay(): Date {
            val cal = Calendar.getInstance().apply {
                time = this@startOfDay
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.time
        }

        val today = Date().startOfDay()
        val startDay = start.startOfDay()
        val endDay = end.startOfDay()
        !today.before(startDay) && !today.after(endDay)
    }

    // Debug logging - removed to improve performance
    
    // Filter state
    var selectedStatus by remember { mutableStateOf<String?>("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    
    // Sort state
    var sortBy by remember { mutableStateOf<String?>(null) }
    var showSortDialog by remember { mutableStateOf(false) }
    
    // Date range state
    var dateRangeEnabled by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()
    
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    // Filtered and sorted expenses
    val filteredExpenses = remember(
        departmentExpenses, 
        selectedStatus, 
        searchQuery, 
        dateRangeEnabled, 
        startDate, 
        endDate,
        sortBy
    ) {
        var filtered = departmentExpenses.filter { expense ->
            val statusMatch = when (selectedStatus) {
                null, "All" -> true
                "Pending" -> expense.status == ExpenseStatus.PENDING
                "Approved" -> expense.status == ExpenseStatus.APPROVED
                "Rejected" -> expense.status == ExpenseStatus.REJECTED
                "Complete" -> expense.status == ExpenseStatus.COMPLETE
                else -> true
            }
            val searchMatch = searchQuery.isEmpty() || 
                expense.by.contains(searchQuery, ignoreCase = true) ||
                expense.invoice.contains(searchQuery, ignoreCase = true)
            
            // Date range filter
            val dateMatch = if (dateRangeEnabled && (startDate != null || endDate != null)) {
                val expenseDate = expense.date?.toDate()
                if (expenseDate != null) {
                    val afterStart = startDate == null || !expenseDate.before(startDate)
                    val beforeEnd = endDate == null || !expenseDate.after(endDate)
                    afterStart && beforeEnd
                } else {
                    false
                }
            } else {
                true
            }
            
            statusMatch && searchMatch && dateMatch
        }
        
        // Sort expenses
        filtered = when (sortBy) {
            "Amount (High to Low)" -> filtered.sortedByDescending { it.amount }
            "Amount (Low to High)" -> filtered.sortedBy { it.amount }
            "Date (Newest First)" -> filtered.sortedByDescending { it.date?.toDate() ?: Date(0) }
            "Date (Oldest First)" -> filtered.sortedBy { it.date?.toDate() ?: Date(0) }
            "Status" -> filtered.sortedBy { it.status.name }
            else -> filtered
        }
        
        // Debug logging
        Log.d("DepartmentDetailScreen", "🔍 Filtering expenses:")
        Log.d("DepartmentDetailScreen", "  Total department expenses: ${departmentExpenses.size}")
        Log.d("DepartmentDetailScreen", "  Selected status filter: $selectedStatus")
        Log.d("DepartmentDetailScreen", "  Search query: '$searchQuery'")
        Log.d("DepartmentDetailScreen", "  Date range enabled: $dateRangeEnabled")
        Log.d("DepartmentDetailScreen", "  Sort by: $sortBy")
        Log.d("DepartmentDetailScreen", "  Filtered result: ${filtered.size} expenses")
        
        filtered
    }

    val hasNoPendingInFilteredExpenses = remember(filteredExpenses) {
        filteredExpenses.none { it.status == ExpenseStatus.PENDING }
    }

    val shouldShowFinishDepartmentButton = remember(
        budgetUtilization,
        isWithinCurrentPhaseDateRange,
        hasNoPendingInFilteredExpenses,
    ) {
        budgetUtilization > 50.0 &&
            isWithinCurrentPhaseDateRange &&
            hasNoPendingInFilteredExpenses
    }
    
    // Data loading is now handled in the parallel LaunchedEffect above (lines 189-220)
    // This ensures all data loads simultaneously for faster screen rendering
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(iosPalette.tier1Background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar - Center aligned to match reference image
        CenterAlignedTopAppBar(
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TruncatedTextWithPopup(
                            text = if (isPhaseSpecificOthers) "Others" else displayDepartmentName,
                            maxLength = 20,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosPalette.textPrimary
                        )
                    }
                    Text(
                        text = "Department Expenses",
                        fontSize = 14.sp,
                        color = iosPalette.textSecondary
                    )
                }
            },
            navigationIcon = {
                TextButton(onClick = onNavigateBack) {
                    Text(
                        text = "Done",
                        color = iosPalette.accent,
                        fontSize = 16.sp
                    )
                }
            },
            actions = {
                // Info button: show all department line items from Firebase (Material 3)
                if (departmentName != "Others" && !isPhaseSpecificOthers) {
                    IconButton(
                        onClick = { showLineItemsSheet = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = iosPalette.tier3Field,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "View department line items",
                            tint = iosPalette.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                // Hide menu for "Others" since it's not a real department
                if (departmentName != "Others" && !isPhaseSpecificOthers && userRole == UserRole.BUSINESS_HEAD) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = iosPalette.tier3Field,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = iosPalette.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

//                        if (userRole == UserRole.BUSINESS_HEAD){
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                offset = androidx.compose.ui.unit.DpOffset(x = 0.dp, y = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.background(
                                    color = iosPalette.tier2Surface,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            ) {
                                // Edit Department (line items) - only when we have a phase and department from Firebase
                                if (extractedPhaseId != null && currentDepartment != null) {
                                    DropdownMenuItem(
                                        enabled = !isDepartmentFinished,
                                        text = {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Edit Department",
                                                    color = if (isDepartmentFinished) iosPalette.textSecondary else iosPalette.textPrimary,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Normal
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = null,
                                                    tint = if (isDepartmentFinished) iosPalette.textSecondary else iosPalette.textPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            showMenu = false
                                            editDepartmentLineItems = currentDepartment.lineItems.map { item ->
                                                LineItem(
                                                    itemType = item.itemType,
                                                    item = item.item,
                                                    spec = item.spec,
                                                    quantity = item.quantity,
                                                    unitPrice = item.unitPrice,
                                                    uom = item.uom
                                                )
                                            }
                                            editContractorMode = currentDepartment.contractorModeEnumValue
                                            editContractorAmountText = if (currentDepartment.contractorAmount > 0)
                                                currentDepartment.contractorAmount.toBigDecimal().stripTrailingZeros().toPlainString()
                                            else ""
                                            showEditDepartmentSheet = true
                                        }
                                    )
                                    HorizontalDivider(
                                        color = iosPalette.hairline,
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }

                                HorizontalDivider(
                                    color = iosPalette.hairline,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                // Delete Department Item
                                DropdownMenuItem(
                                    enabled = !isDepartmentFinished,
                                    text = {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Delete Department",
                                                color = if (isDepartmentFinished) iosPalette.textSecondary else iosPalette.danger,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = if (isDepartmentFinished) iosPalette.textSecondary else iosPalette.danger,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        coroutineScope.launch {
                                            val canDelete =
                                                approverProjectViewModel.canDeleteDepartment(
                                                    projectId,
                                                    departmentName
                                                )
                                            if (canDelete) {
                                                showDeleteDialog = true
                                            } else {
                                                showCannotDeleteDialog = true
                                            }
                                        }
                                    }
                                )
                            }
//                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = iosPalette.tier2Surface
            )
        )
        
        // Edit Department Bottom Sheet (line items; total drives department budget and project budget)
        if (showEditDepartmentSheet && extractedPhaseId != null && currentDepartment != null) {
            val deptToEdit = currentDepartment!!
            val phaseIdToEdit = extractedPhaseId!!
            ModalBottomSheet(
                onDismissRequest = { showEditDepartmentSheet = false },
                containerColor = iosPalette.tier2Surface,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEditDepartmentSheet = false }) {
                            Text("Cancel", color = iosPalette.accent, fontSize = 16.sp)
                        }
                        Text(
                            text = "Edit Department",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosPalette.textPrimary
                        )
                        TextButton(
                            onClick = {
                                if (isSavingEditDepartment) return@TextButton
                                val contractorAmt = editContractorAmountText.toDoubleOrNull() ?: 0.0

                                val validLineItems = editDepartmentLineItems.filter { item ->
                                    item.itemType.isNotBlank() &&
                                        item.item.isNotBlank() &&
                                        item.spec.isNotBlank() &&
                                        item.quantity > 0 &&
                                        item.unitPrice > 0
                                }

                                val lineItemsToSave = when (editContractorMode) {
                                    ContractorMode.TURNKEY_AMOUNT_ONLY -> emptyList()
                                    else -> if (validLineItems.isEmpty() && deptToEdit.lineItems.isNotEmpty()) {
                                        deptToEdit.lineItems
                                    } else {
                                        validLineItems.map { item ->
                                            DepartmentLineItemData(
                                                itemType = item.itemType,
                                                item = item.item,
                                                spec = item.spec,
                                                quantity = item.quantity,
                                                unitPrice = item.unitPrice,
                                                uom = item.uom,
                                                remainingQuantity = item.quantity
                                            )
                                        }
                                    }
                                }

                                val contractorModeStr = when (editContractorMode) {
                                    ContractorMode.LABOUR_ONLY -> "Labour-Only"
                                    ContractorMode.SELF_EXECUTION -> "Self Execution"
                                    ContractorMode.TURNKEY_AMOUNT_ONLY -> "Turnkey"
                                }

                                val lineItemsTotal = lineItemsToSave.sumOf { it.total }
                                val calculatedBudget = when (editContractorMode) {
                                    ContractorMode.LABOUR_ONLY -> lineItemsTotal + contractorAmt
                                     ContractorMode.SELF_EXECUTION -> lineItemsTotal
                                     ContractorMode.TURNKEY_AMOUNT_ONLY -> contractorAmt
                                 }

                                val existingRemainingBudget = deptToEdit.remainingAmount
                                val existingBudget = deptToEdit.totalBudget
                                val changedBudget = calculatedBudget - existingBudget
                                val presentRemainingBudget = existingRemainingBudget + changedBudget

                                val contractorAmountDelta = contractorAmt - deptToEdit.contractorAmount
                                val newContractorRemainingAmount = (deptToEdit.contractorRemainingAmount + contractorAmountDelta).coerceAtLeast(0.0)

                                val updatedDepartment = deptToEdit.copy(
                                    name = deptToEdit.name,
                                    contractorMode = contractorModeStr,
                                    lineItems = lineItemsToSave,
                                    phaseId = phaseIdToEdit,
                                    projectId = projectId,
                                    totalBudget = calculatedBudget,
                                    remainingAmount = presentRemainingBudget,
                                    contractorAmount = contractorAmt,
                                    contractorRemainingAmount = newContractorRemainingAmount
                                )
                                isSavingEditDepartment = true
                                coroutineScope.launch {
                                    try {
                                        val result = approverProjectViewModel.updateDepartmentWithLineItems(
                                            projectId = projectId,
                                            phaseId = phaseIdToEdit,
                                            department = updatedDepartment
                                        )
                                        if (result.isSuccess) {
                                            showEditDepartmentSheet = false
                                            android.widget.Toast.makeText(
                                                context,
                                                "Department updated. Budget recalculated from line items.",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Error: ${result.exceptionOrNull()?.message ?: "Failed to update"}",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Error: ${e.message}",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    } finally {
                                        isSavingEditDepartment = false
                                    }
                                }
                            },
                            enabled = !isSavingEditDepartment
                        ) {
                            Text(
                                if (isSavingEditDepartment) "Saving…" else "Save",
                                color = iosPalette.accent,
                                fontSize = 16.sp
                            )
                        }
                    }
                    HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)

                    // Contractor amount field for contractor-driven modes
                    if (
                        editContractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY ||
                        editContractorMode == ContractorMode.LABOUR_ONLY
                    ) {
                        val label = if (
                            editContractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY
                        ) "Turnkey" else "Contractor Amount"
                        OutlinedTextField(
                            value = editContractorAmountText,
                            onValueChange = { v -> editContractorAmountText = v.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text(label) },
                            placeholder = { Text("Enter amount") },
                            prefix = { Text("₹") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = iosPalette.accent,
                                unfocusedBorderColor = iosPalette.hairline,
                                focusedContainerColor = iosPalette.tier3Field,
                                unfocusedContainerColor = iosPalette.tier3Field,
                                focusedTextColor = iosPalette.textPrimary,
                                unfocusedTextColor = iosPalette.textPrimary
                            )
                        )
                    }

                    // Line items guidance text (hidden for pure Turnkey)
                     if (editContractorMode != ContractorMode.TURNKEY_AMOUNT_ONLY) {
                        Text(
                            text = "Edit line items for $displayDepartmentName. Total of line items becomes the department budget; project budget is updated automatically.",
                            fontSize = 14.sp,
                            color = iosPalette.textSecondary
                        )
                    }

                    // Always show contractor mode selector, even when currently Turnkey
                    DepartmentLineItemsSection(
                        contractorMode = editContractorMode,
                        onContractorModeChange = { editContractorMode = it },
                        lineItems = editDepartmentLineItems,
                        onLineItemsChange = { editDepartmentLineItems = it }
                    )
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Department") },
                text = { Text("Are you sure you want to delete this department? This will remove it from all phases.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isLoadingDelete = true
                            approverProjectViewModel.deleteDepartment(
                                projectId = projectId,
                                departmentName = departmentName,
                                onSuccess = {
                                    isLoadingDelete = false
                                    showDeleteDialog = false
                                    onNavigateBack()
                                },
                                onError = {
                                    isLoadingDelete = false
                                    showDeleteDialog = false
                                }
                            )
                        },
                        enabled = !isLoadingDelete
                    ) {
                        Text("Delete", color = iosPalette.danger)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = iosPalette.accent)
                    }
                },
                containerColor = iosPalette.tier2Surface
            )
        }

        if (showFinishDepartmentDialog && (isBusinessHead || isApprover) && currentDepartment != null && extractedPhaseId != null) {
            AlertDialog(
                onDismissRequest = {
                    if (!isFinishingDepartment) {
                        showFinishDepartmentDialog = false
                    }
                },
                title = { Text("Finish Department") },
                text = {
                    Text("Are you sure you want to finish this department? This step cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (isFinishingDepartment) return@TextButton
                            isFinishingDepartment = true
                            coroutineScope.launch {
                                try {
                                    val result = approverProjectViewModel.updateDepartmentWithLineItems(
                                        projectId = projectId,
                                        phaseId = extractedPhaseId,
                                        department = currentDepartment.copy(isFinished = true)
                                    )
                                    if (result.isSuccess) {
                                        showFinishDepartmentDialog = false
                                        android.widget.Toast.makeText(
                                            context,
                                            "Department marked as finished",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Error: ${result.exceptionOrNull()?.message ?: "Failed to finish department"}",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    isFinishingDepartment = false
                                }
                            }
                        },
                        enabled = !isFinishingDepartment
                    ) {
                        Text(
                            text = if (isFinishingDepartment) "Finishing..." else "Finish",
                            color = iosPalette.danger
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showFinishDepartmentDialog = false },
                        enabled = !isFinishingDepartment
                    ) {
                        Text("Cancel", color = iosPalette.accent)
                    }
                },
                containerColor = iosPalette.tier2Surface
            )
        }

        if (showRevertDepartmentDialog && isBusinessHead && currentDepartment != null && extractedPhaseId != null) {
            AlertDialog(
                onDismissRequest = {
                    if (!isRevertingDepartment) {
                        showRevertDepartmentDialog = false
                    }
                },
                title = { Text("Revert Finish") },
                text = {
                    Text("Are you sure you want to revert finish? This department will be active again.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (isRevertingDepartment) return@TextButton
                            isRevertingDepartment = true
                            coroutineScope.launch {
                                try {
                                    val result = approverProjectViewModel.updateDepartmentWithLineItems(
                                        projectId = projectId,
                                        phaseId = extractedPhaseId,
                                        department = currentDepartment.copy(isFinished = false)
                                    )
                                    if (result.isSuccess) {
                                        showRevertDepartmentDialog = false
                                        android.widget.Toast.makeText(
                                            context,
                                    "Department finish reverted",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Error: ${result.exceptionOrNull()?.message ?: "Failed to reopen department"}",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    isRevertingDepartment = false
                                }
                            }
                        },
                        enabled = !isRevertingDepartment
                    ) {
                        Text(
                            text = if (isRevertingDepartment) "Reverting..." else "Revert",
                            color = iosPalette.accent
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showRevertDepartmentDialog = false },
                        enabled = !isRevertingDepartment
                    ) {
                        Text("Cancel", color = iosPalette.accent)
                    }
                },
                containerColor = iosPalette.tier2Surface
            )
        }
        
        // Cannot Delete Dialog (from reference image)
        if (showCannotDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showCannotDeleteDialog = false },
                title = { 
                    Text(
                        "Cannot Delete Department",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = iosPalette.textPrimary
                    ) 
                },
                text = { 
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "There should be at least one department in each phase. Please add a new department before deleting this one.",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = iosPalette.textPrimary
                        )
                        
                        // Buttons - Only "Add Department" and "Cancel" as per reference
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    showCannotDeleteDialog = false
                                    // Navigate to add department - for now just close
                                    // You can add navigation here if needed
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = iosPalette.accent
                                )
                            ) {
                                Text(
                                    "Add Department", 
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            TextButton(
                                onClick = { showCannotDeleteDialog = false },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = iosPalette.accent
                                )
                            ) {
                                Text(
                                    "Cancel", 
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {},
                shape = RoundedCornerShape(16.dp),
                containerColor = iosPalette.tier2Surface
            )
        }

        // Department line items bottom sheet (Material 3) – all line items from Firebase
        if (showLineItemsSheet) {
            DepartmentLineItemsBottomSheet(
                departmentName = displayDepartmentName,
                lineItems = departmentLineItems,
                contractorMode = currentDepartment?.contractorModeEnumValue ?: ContractorMode.SELF_EXECUTION,
                contractorAmount = currentDepartment?.contractorAmount ?: 0.0,
                contractorRemainingAmount = currentDepartment?.contractorRemainingAmount ?: 0.0,
                onDismiss = { showLineItemsSheet = false }
            )
        }
        
        // Skip loading screen for Production Head users - show content directly
//        if (isLoading && authState.user?.role != UserRole.BUSINESS_HEAD) {
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                CircularProgressIndicator(color = Color(0xFF4285F4))
//            }
//        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isDepartmentFinished) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = iosPalette.warning.copy(alpha = 0.16f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = iosPalette.warning,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "This department is finished. Expense actions are disabled.",
                                color = iosPalette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (shouldBlockAllInteractions) 0.72f else 1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Budget Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Total Budget at top center
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Total Budget",
                                    fontSize = 13.sp,
                                    color = iosPalette.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = FormatUtils.formatCurrency(departmentBudget),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = iosPalette.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Spent and Remaining at bottom
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Spent Column - Left aligned
                                Column(
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = "Spent",
                                        fontSize = 13.sp,
                                        color = iosPalette.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = FormatUtils.formatCurrency(totalSpent),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = iosPalette.accent
                                    )
                                }
                                
                                // Remaining Column - Right aligned with triangle
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Remaining",
                                        fontSize = 13.sp,
                                        color = iosPalette.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = FormatUtils.formatCurrency(remaining),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (remaining < 0) iosPalette.danger else iosPalette.accent
                                        )
                                        // Triangle shape
                                        Canvas(
                                            modifier = Modifier.size(12.dp)
                                        ) {
                                            val trianglePath = Path().apply {
                                                moveTo(size.width / 2f, 0f) // Top point
                                                lineTo(0f, size.height) // Bottom left
                                                lineTo(size.width, size.height) // Bottom right
                                                close()
                                            }
                                            drawPath(
                                                path = trianglePath,
                                                color = if (remaining < 0) iosPalette.danger else iosPalette.accent
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Contractor amount row — shown for TURNKEY and LABOUR_ONLY
                            val deptContractorMode = currentDepartment?.contractorModeEnumValue
                             if (deptContractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY || deptContractorMode == ContractorMode.LABOUR_ONLY) {
                                 val contractorAmt = currentDepartment?.contractorAmount ?: 0.0
                                 val contractorRemaining = currentDepartment?.contractorRemainingAmount ?: 0.0
                                 val modeLabel = if (deptContractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY) "Turnkey" else "Contractor"
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = iosPalette.hairline, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            text = "$modeLabel Amount",
                                            fontSize = 12.sp,
                                            color = iosPalette.textSecondary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = FormatUtils.formatCurrency(contractorAmt),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = iosPalette.textPrimary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$modeLabel Remaining",
                                            fontSize = 12.sp,
                                            color = iosPalette.textSecondary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = FormatUtils.formatCurrency(contractorRemaining),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (contractorRemaining <= 0) iosPalette.danger else iosPalette.success
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Budget Utilization Section - Single continuous bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Budget Utilization",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = iosPalette.textPrimary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                // Single continuous progress bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(iosPalette.hairline)
                                ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                            .fillMaxWidth(budgetUtilizationProgress(budgetUtilization))
                                            .clip(RoundedCornerShape(4.dp))
                                            .then(
                                                when (utilizationSeverity) {
                                                    BudgetUtilizationSeverity.ERROR -> Modifier.background(iosPalette.danger)
                                                    BudgetUtilizationSeverity.WARNING -> Modifier.background(iosPalette.warning)
                                                    BudgetUtilizationSeverity.NORMAL -> Modifier.background(
                                                        brush = Brush.horizontalGradient(
                                                            colors = listOf(
                                                                Color(0xFF8E44AD),
                                                                Color(0xFF2ECC71),
                                                                Color(0xFF0A84FF)
                                                            )
                                                        )
                                                    )
                                                }
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (budgetUtilization == 0.0) {
                                        "0%"
                                    } else if (budgetUtilization < 0.01) {
                                        // For very small amounts (< 0.01%), show with 3 decimal places
                                        String.format("%.3f%%", budgetUtilization)
                                    } else if (budgetUtilization < 1.0) {
                                        // For small amounts (< 1%), show with 2 decimal places
                                        String.format("%.2f%%", budgetUtilization)
                                    } else {
                                        // For larger amounts (>= 1%), show with 1 decimal place
                                        String.format("%.1f%%", budgetUtilization)
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = when (utilizationSeverity) {
                                        BudgetUtilizationSeverity.ERROR -> iosPalette.danger
                                        BudgetUtilizationSeverity.WARNING -> iosPalette.warning
                                        BudgetUtilizationSeverity.NORMAL -> Color(0xFF0A84FF)
                                    }
                                )
                            }

                            val canManageFinishState = departmentName != "Others" &&
                                !isPhaseSpecificOthers &&
                                currentDepartment != null &&
                                extractedPhaseId != null
                            if (canManageFinishState) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (isDepartmentFinished) {
                                        if (isBusinessHead) {
                                            OutlinedButton(
                                                onClick = { showRevertDepartmentDialog = true },
                                                enabled = !isRevertingDepartment,
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = iosPalette.accent
                                                )
                                            ) {
                                                Text(
                                                    text = if (isRevertingDepartment) "Reverting..." else "Revert Finish",
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    } else {
                                        if ((isBusinessHead || isApprover) && shouldShowFinishDepartmentButton) {
                                            OutlinedButton(
                                                onClick = { showFinishDepartmentDialog = true },
                                                enabled = !isFinishingDepartment,
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = iosPalette.danger
                                                )
                                            ) {
                                                Text(
                                                    text = if (isFinishingDepartment) "Finishing..." else "Finish Department",
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Search Bar with Filter & Sort button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { if (!shouldBlockAllInteractions) searchQuery = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Search expenses...", color = iosPalette.textSecondary) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = iosPalette.textSecondary
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = iosPalette.accent,
                                unfocusedBorderColor = iosPalette.hairline,
                                unfocusedContainerColor = iosPalette.tier3Field,
                                focusedContainerColor = iosPalette.tier2Surface,
                                focusedTextColor = iosPalette.textPrimary,
                                unfocusedTextColor = iosPalette.textPrimary,
                                cursorColor = iosPalette.accent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !shouldBlockAllInteractions
                        )
                        Button(
                            onClick = { if (!shouldBlockAllInteractions) showFilterDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = iosPalette.tier3Field,
                                contentColor = iosPalette.accent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp),
                            enabled = !shouldBlockAllInteractions
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filter & Sort",
                                    tint = iosPalette.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Filter & Sort",
                                    color = iosPalette.accent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                @Composable
                fun StatusCard(
                    title: String,
                    amount: Double, // or whatever type 'amount' is (likely Double or Int)
                    backgroundColor: Color,
                    accentColor: Color,
                    modifier: Modifier = Modifier
                ) {
                    Card(
                        modifier = modifier,
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        shape = RoundedCornerShape(16.dp), // Slightly more rounded for modern look
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Flat is modern
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp), // Increased padding for breathability
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // The Dot
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                                // The Title
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = iosPalette.textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // The Amount
                            Text(
                                text = FormatUtils.formatCurrency(amount),
                                fontSize = 16.sp, // Slightly larger for emphasis
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                }
                
                item {
                    val approvedAmount = departmentExpenses
                        .filter { it.status == ExpenseStatus.APPROVED }
                        .sumOf { it.amount }
                    val pendingAmount = departmentExpenses
                        .filter { it.status == ExpenseStatus.PENDING }
                        .sumOf { it.amount }
                    val rejectedAmount = departmentExpenses
                        .filter { it.status == ExpenseStatus.REJECTED }
                        .sumOf { it.amount }
                    val completeAmount = departmentExpenses
                        .filter { it.status == ExpenseStatus.COMPLETE }
                        .sumOf { it.amount }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp) // Vertical gap between rows
                    ) {
                        // --- Top Row (Approved & Pending) ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp) // Horizontal gap
                        ) {
                            StatusCard(
                                title = "Approved",
                                amount = approvedAmount,
                                backgroundColor = iosPalette.tier3Field,
                                accentColor = iosPalette.info,
                                modifier = Modifier.weight(1f)
                            )
                            StatusCard(
                                title = "Pending",
                                amount = pendingAmount,
                                backgroundColor = iosPalette.tier3Field,
                                accentColor = iosPalette.warning,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // --- Bottom Row (Rejected & Complete) ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatusCard(
                                title = "Rejected",
                                amount = rejectedAmount,
                                backgroundColor = iosPalette.tier3Field,
                                accentColor = iosPalette.danger,
                                modifier = Modifier.weight(1f)
                            )
                            StatusCard(
                                title = "Complete",
                                amount = completeAmount,
                                backgroundColor = iosPalette.tier3Field,
                                accentColor = iosPalette.success,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                
                // Expenses List
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = iosPalette.accent
                            )
                        }
                    }
                } else if (filteredExpenses.isNotEmpty()) {
                    items(filteredExpenses) { expense ->
                        ModernExpenseCard(
                            expense = expense,
                            interactionsEnabled = !shouldBlockAllInteractions,
                            userRole = userRole?.name,
                            onNavigateToExpenseChat = { expenseId ->
                                onNavigateToExpenseChat(expenseId)
                            },
                            onNavigateToReview = { expenseId ->
                                onNavigateToReview(expenseId)
                            },
                            onNavigateToDetail = { expenseId ->
                                onNavigateToDetail(expenseId)
                            }
                        )
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🔍 No Expenses Found",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = iosPalette.textSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No expenses found for this department",
                                    fontSize = 14.sp,
                                    color = iosPalette.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            if (shouldBlockAllInteractions) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.08f))
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { })
                        }
                )
            }
            }
        
        // Filter & Sort Dialog - Overlay on top
        if (showFilterDialog) {
            FilterSortDialog(
                selectedStatus = selectedStatus ?: "All",
                onStatusSelected = { selectedStatus = it },
                sortBy = sortBy,
                onSortSelected = { sortBy = it },
                showSortDialog = showSortDialog,
                onShowSortDialog = { showSortDialog = it },
                dateRangeEnabled = dateRangeEnabled,
                onDateRangeEnabledChanged = { dateRangeEnabled = it },
                startDate = startDate,
                endDate = endDate,
                onStartDateSelected = { 
                    startDate = it
                    showStartDatePicker = false
                },
                onEndDateSelected = { 
                    endDate = it
                    showEndDatePicker = false
                },
                showStartDatePicker = showStartDatePicker,
                showEndDatePicker = showEndDatePicker,
                onShowStartDatePicker = { showStartDatePicker = it },
                onShowEndDatePicker = { showEndDatePicker = it },
                onDismiss = { showFilterDialog = false },
                dateFormatter = dateFormatter
            )
        }
        
        // Start Date Picker - Overlay on top
        if (showStartDatePicker) {
            DatePickerDialog(
                title = "Select Start Date",
                datePickerState = startDatePickerState,
                onDateSelected = { date ->
                    startDate = date
                    showStartDatePicker = false
                },
                onDismiss = { showStartDatePicker = false }
            )
        }
        
        // End Date Picker - Overlay on top
        if (showEndDatePicker) {
            DatePickerDialog(
                title = "Select End Date",
                datePickerState = endDatePickerState,
                onDateSelected = { date ->
                    endDate = date
                    showEndDatePicker = false
                },
                onDismiss = { showEndDatePicker = false }
            )
        }
    }
    }
}

@Composable
private fun FilterSortDialog(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    sortBy: String?,
    onSortSelected: (String?) -> Unit,
    showSortDialog: Boolean,
    onShowSortDialog: (Boolean) -> Unit,
    dateRangeEnabled: Boolean,
    onDateRangeEnabledChanged: (Boolean) -> Unit,
    startDate: Date?,
    endDate: Date?,
    onStartDateSelected: (Date) -> Unit,
    onEndDateSelected: (Date) -> Unit,
    showStartDatePicker: Boolean,
    showEndDatePicker: Boolean,
    onShowStartDatePicker: (Boolean) -> Unit,
    onShowEndDatePicker: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    dateFormatter: SimpleDateFormat
) {
    val iosPalette = rememberIosDepartmentPalette()
    // Overlay background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(iosPalette.tier1Background.copy(alpha = 0.7f))
            .clickable { onDismiss() }
    ) {
        // Dialog card positioned below the Filter & Sort button (right side, below search bar)
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(280.dp)
                .padding(top = 200.dp, end = 16.dp) // Position below search bar area, aligned to right
                .clickable(enabled = false) { }, // Prevent click propagation
            colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable(enabled = false) { } // Prevent click propagation
            ) {
                // Status Section - Blur when sort dialog is open
                Column(
                    modifier = Modifier
                        .alpha(if (showSortDialog) 0.3f else 1f)
                        .clickable(enabled = !showSortDialog) { }
                ) {
                    Text(
                        text = "Status",
                        fontSize = 14.sp,
                        color = iosPalette.textSecondary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Status Options - Close dialog when status is selected
                    StatusOption("All", selectedStatus == "All") { 
                        onStatusSelected("All")
                        onDismiss()
                    }
                    StatusOption("Pending", selectedStatus == "Pending") { 
                        onStatusSelected("Pending")
                        onDismiss()
                    }
                    StatusOption("Approved", selectedStatus == "Approved") { 
                        onStatusSelected("Approved")
                        onDismiss()
                    }
                    StatusOption("Complete", selectedStatus == "Complete") {
                        onStatusSelected("Complete")
                        onDismiss()
                    }
                    StatusOption("Rejected", selectedStatus == "Rejected") { 
                        onStatusSelected("Rejected")
                        onDismiss()
                    }
                }
                
                // Divider
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = iosPalette.hairline,
                    thickness = 1.dp
                )
                
                // Sort by Section
                if (!showSortDialog) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShowSortDialog(true) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (sortBy != null) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = iosPalette.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Sort by",
                                fontSize = 14.sp,
                                color = iosPalette.textPrimary
                            )
                        }
                    }
                } else {
                    // Sort Options - Inline when sort dialog is open
                    Column {
                        // Sort by Header with checkmark
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = iosPalette.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Sort by",
                                    fontSize = 14.sp,
                                    color = iosPalette.textPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        // Sort Options
                        val sortOptions = listOf(
                            "Date (Newest First)" to Icons.Default.DateRange,
                            "Date (Oldest First)" to Icons.Default.DateRange,
                            "Amount (High to Low)" to Icons.Default.ArrowDownward,
                            "Amount (Low to High)" to Icons.Default.ArrowUpward,
                            "Status" to Icons.Default.Label
                        )
                        
                        sortOptions.forEach { (option, icon) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSortSelected(option)
                                        onShowSortDialog(false)
                                        onDismiss() // Close the main dialog
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (sortBy == option) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = iosPalette.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = option,
                                        fontSize = 14.sp,
                                        color = iosPalette.textPrimary
                                    )
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iosPalette.textSecondary,
                                    modifier = Modifier.size(18.dp)
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
private fun StatusOption(
    status: String,
    isSelected: Boolean,
    onSelected: (String) -> Unit
) {
    val iosPalette = rememberIosDepartmentPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected(status) }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = status,
            fontSize = 14.sp,
            color = iosPalette.textPrimary
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = iosPalette.accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }

}

@Composable
private fun SortOptionsDialog(
    selectedSort: String?,
    onSortSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val iosPalette = rememberIosDepartmentPalette()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(iosPalette.tier1Background.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(240.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val sortOptions = listOf(
                    "Date (Newest First)" to Icons.Default.DateRange,
                    "Date (Oldest First)" to Icons.Default.DateRange,
                    "Amount (High to Low)" to Icons.Default.ArrowDownward,
                    "Amount (Low to High)" to Icons.Default.ArrowUpward,
                    "Status" to Icons.Default.Label
                )
                
                sortOptions.forEach { (option, icon) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortSelected(option) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedSort == option) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = iosPalette.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = option,
                                fontSize = 14.sp,
                                color = iosPalette.textPrimary
                            )
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iosPalette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    title: String,
    datePickerState: DatePickerState,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
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
        DatePicker(
            state = datePickerState,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernExpenseCard(
    expense: DetailedExpense,
    interactionsEnabled: Boolean = true,
    userRole: String? = null,
    onNavigateToExpenseChat: (String) -> Unit = {},
    onNavigateToReview: (String) -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {}
) {
    val iosPalette = rememberIosDepartmentPalette()
    var showReviewSheet by remember { mutableStateOf(false) }
    val reviewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = interactionsEnabled) {
                showReviewSheet = true
            },
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left side: Amount, Description, Sub Category, Submitted By
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Amount at top left (bold)
                Text(
                    text = FormatUtils.formatCurrency(expense.amount),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.textPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description below amount
                if (expense.description.isNotEmpty()) {
                    TruncatedTextWithPopup(
                        text = expense.description,
                        maxLength = 10,
                        fontSize = 14.sp,
                        color = iosPalette.textPrimary,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                
                // Sub Category below description
                if (expense.category.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Label,
                            contentDescription = "Sub Category",
                            tint = iosPalette.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        TruncatedTextWithPopup(
                            text = expense.category,
                            maxLength = 10,
                            fontSize = 12.sp,
                            color = iosPalette.textSecondary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                
                // Submitted By below sub category
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Submitted By",
                        tint = iosPalette.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TruncatedTextWithPopup(
                        text = expense.by.ifEmpty { "Unknown" },
                        maxLength = 10,
                        fontSize = 12.sp,
                        color = iosPalette.textSecondary,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            
            // Right side: Status chip, Chat icon (for pending), Mode of payment, Attachment/Proof icons
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            ) {
                // Status chip at top right
                val statusBg: Color
                val statusFg: Color
                val statusLabel: String
                when (expense.status) {
                    ExpenseStatus.APPROVED -> {
                        statusBg = iosPalette.tier3Field; statusFg = iosPalette.accent; statusLabel = "Approved"
                    }
                    ExpenseStatus.PENDING -> {
                        statusBg = iosPalette.tier3Field; statusFg = iosPalette.warning; statusLabel = "Pending"
                    }
                    ExpenseStatus.REJECTED -> {
                        statusBg = iosPalette.tier3Field; statusFg = iosPalette.danger; statusLabel = "Rejected"
                    }
                    ExpenseStatus.COMPLETE -> {
                        statusBg = iosPalette.tier3Field; statusFg = iosPalette.accent; statusLabel = "Completed"
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = statusBg),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusFg)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusLabel,
                            color = statusFg,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Chat icon for pending expenses
                if (expense.status == ExpenseStatus.PENDING) {
                    IconButton(
                        onClick = { onNavigateToExpenseChat(expense.id) },
                        enabled = interactionsEnabled,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Chat",
                            tint = iosPalette.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // Mode of payment with icons
                if (expense.modeOfPayment.isNotEmpty()) {
                    val paymentLabel = when (expense.modeOfPayment.lowercase()) {
                        "cash" -> "By cash"
                        "upi" -> "By UPI"
                        "check", "cheque" -> "By Cheque"
                        else -> "By ${expense.modeOfPayment}"
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$",
                            fontSize = 12.sp,
                            color = iosPalette.accent,
                            fontWeight = FontWeight.Bold
                        )
                        TruncatedTextWithPopup(
                            text = paymentLabel,
                            maxLength = 10,
                            fontSize = 12.sp,
                            color = iosPalette.accent,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Attachment and Payment Proof icons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Payment Proof Icon (if exists)
                        if (expense.paymentProofUrl.isNotEmpty()) {
                            val context = LocalContext.current
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Payment Proof",
                                tint = iosPalette.success,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(enabled = interactionsEnabled) {
                                        com.cubiquitous.tracura.utils.ImageUriHelper.openFirebaseStorageFile(
                                            context = context,
                                            storageUrl = expense.paymentProofUrl,
                                            fileName = expense.paymentProofFileName,
                                            onError = { error ->
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Error: $error",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }
                            )
                        }
                        
                        // Attachment Icon (if exists)
                        if (expense.attachmentUrl.isNotEmpty()) {
                            val context = LocalContext.current
                            val attachmentIcon = if (expense.attachmentFileName.contains(".jpg", ignoreCase = true) ||
                                expense.attachmentFileName.contains(".jpeg", ignoreCase = true) ||
                                expense.attachmentFileName.contains(".png", ignoreCase = true) ||
                                expense.attachmentFileName.contains("image", ignoreCase = true)) {
                                Icons.Default.Image
                            } else {
                                Icons.Default.Description
                            }
                            
                            val attachmentColor = if (attachmentIcon == Icons.Default.Image) {
                                iosPalette.accent
                            } else {
                                iosPalette.accent
                            }
                            
                            Icon(
                                attachmentIcon,
                                contentDescription = "Attachment",
                                tint = attachmentColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(enabled = interactionsEnabled) {
                                        com.cubiquitous.tracura.utils.ImageUriHelper.openFirebaseStorageFile(
                                            context = context,
                                            storageUrl = expense.attachmentUrl,
                                            fileName = expense.attachmentFileName,
                                            onError = { error ->
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Error: $error",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReviewSheet) {
        ModalBottomSheet(
            onDismissRequest = { showReviewSheet = false },
            sheetState = reviewSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = null
        ) {
            Box(modifier = Modifier.fillMaxHeight()) {
                ReviewExpenseScreen(
                    expenseId = expense.id,
                    onNavigateBack = { showReviewSheet = false },
                    onNavigateToExpenseChat = onNavigateToExpenseChat,
                    userRole = userRole
                )
            }
        }
    }
}

/**
 * Material 3 bottom sheet listing all department line items from Firebase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartmentLineItemsBottomSheet(
    departmentName: String,
    lineItems: List<DepartmentLineItemData>,
    contractorMode: ContractorMode = ContractorMode.SELF_EXECUTION,
    contractorAmount: Double = 0.0,
    contractorRemainingAmount: Double = 0.0,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iosPalette = rememberIosDepartmentPalette()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = iosPalette.tier2Surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            HorizontalDivider(
                modifier = Modifier
                    .width(32.dp)
                    .padding(vertical = 12.dp),
                color = iosPalette.hairline
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Department details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = iosPalette.textPrimary
            )
            Text(
                text = departmentName,
                style = MaterialTheme.typography.bodyMedium,
                color = iosPalette.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = iosPalette.hairline)
            Spacer(modifier = Modifier.height(8.dp))

            // Contractor amount summary for TURNKEY and LABOUR_ONLY
             if (contractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY || contractorMode == ContractorMode.LABOUR_ONLY) {
                 val modeLabel = if (contractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY) "Turnkey" else "Labour-Only"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = iosPalette.tier3Field),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$modeLabel Contractor",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = iosPalette.textPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Agreed amount", style = MaterialTheme.typography.bodyMedium, color = iosPalette.textSecondary)
                            Text(
                                FormatUtils.formatCurrency(contractorAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = iosPalette.textPrimary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Remaining", style = MaterialTheme.typography.bodyMedium, color = iosPalette.textSecondary)
                            Text(
                                FormatUtils.formatCurrency(contractorRemainingAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (contractorRemainingAmount <= 0) iosPalette.danger else iosPalette.success
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

             if (contractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY) {
                // Turnkey: no line items, nothing more to show
            } else if (lineItems.isEmpty()) {
                Text(
                    text = "No line items in this department.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = iosPalette.textSecondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Text(
                    text = "Line items",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = iosPalette.textPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    lineItems.forEachIndexed { index, lineItem ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = departmentLineItemDisplayTitle(lineItem),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = iosPalette.textPrimary
                                )
                            },
                            supportingContent = {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${lineItem.itemType.ifEmpty { "-" }} • ${departmentFormatQuantity(lineItem.quantity)} ${lineItem.uom.ifEmpty { "-" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = iosPalette.textSecondary
                                        )
                                        Text(
                                            text = "${FormatUtils.formatCurrency(lineItem.unitPrice)}/${lineItem.uom.ifEmpty { "unit" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = iosPalette.textSecondary
                                        )
                                    }
                                    // Remaining Quantity indicator
                                    val remainingRatio = if (lineItem.quantity > 0) lineItem.remainingQuantity / lineItem.quantity else 0.0
                                    val remainingColor = when {
                                        lineItem.remainingQuantity <= 0 -> iosPalette.danger
                                        remainingRatio <= 0.25 -> iosPalette.warning
                                        else -> iosPalette.success
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = remainingColor.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "Remaining: ${departmentFormatQuantity(lineItem.remainingQuantity)} ${lineItem.uom.ifEmpty { "" }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = remainingColor,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            trailingContent = {
                                Text(
                                    text = FormatUtils.formatCurrency(lineItem.total),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = iosPalette.accent
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                headlineColor = iosPalette.textPrimary,
                                supportingColor = iosPalette.textSecondary
                            )
                        )
                        if (index < lineItems.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = iosPalette.hairline
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun departmentLineItemDisplayTitle(lineItem: DepartmentLineItemData): String {
    val itemPart = lineItem.item.ifEmpty { lineItem.itemType.ifEmpty { "Item" } }
    return if (lineItem.spec.isNotBlank()) "$itemPart (${lineItem.spec})" else itemPart
}

private fun departmentFormatQuantity(quantity: Double): String {
    return if (quantity % 1.0 == 0.0) {
        String.format("%.0f", quantity)
    } else {
        String.format("%.1f", quantity)
    }
}
