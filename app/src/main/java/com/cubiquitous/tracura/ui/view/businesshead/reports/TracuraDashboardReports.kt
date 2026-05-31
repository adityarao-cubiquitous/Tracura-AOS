package com.cubiquitous.tracura.ui.view.businesshead.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.Phase
import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.ExpenseStatus
import com.cubiquitous.tracura.viewmodel.OverallReportsViewModel
import com.cubiquitous.tracura.viewmodel.ProjectViewModel
import com.cubiquitous.tracura.utils.FormatUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.ceil
import android.util.Log

/**
 * Formats numbers according to Indian numbering system:
 * 1-999: actual number
 * 1000-99999: X.XXk format (two decimals)
 * 100000-9999999: X.XX lakhs format (two decimals, lakhs shortform)
 * 10000000+: X.XX Cr format (two decimals)
 */
fun formatNumber(value: Double): String {
    return when {
        value >= 1 && value < 1000 -> {
            // Show actual numbers for 1 to 999
            value.toInt().toString()
        }
        value >= 1000 && value < 100000 -> {
            // Show as X.XXk for 1000 to 99999 (two decimals)
            String.format("%.2f", value / 1000) + "k"
        }
        value >= 100000 && value < 10000000 -> {
            // Show as X.XX lakhs for 100000 to 9999999 (two decimals, lakhs shortform)
            String.format("%.2f", value / 100000) + " lakhs"
        }
        value >= 10000000 -> {
            // Show as X.XX Cr for 10000000+ (two decimals)
            String.format("%.2f", value / 10000000) + " Cr"
        }
        else -> "0" // Handle values less than 1
    }
}

fun formatNumber(value: Int): String {
    return formatNumber(value.toDouble())
}

fun formatNumber(value: Float): String {
    return formatNumber(value.toDouble())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracuraDashboardReports(
    onNavigateBack: () -> Unit,
    onNavigateToProject: ((String) -> Unit)? = null,
    overallReportsViewModel: OverallReportsViewModel = hiltViewModel(),
    projectViewModel: ProjectViewModel = hiltViewModel()
) {
    val projects by projectViewModel.projects.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Business name state
    var businessName by remember { mutableStateOf("Tracura") } // Default fallback
    
    // Fetch business name from customer data
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    // For Production Head, their UID is the customerId
                    val customerId = firebaseUser.uid
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val customerDoc = firestore.collection("customers")
                        .document(customerId)
                        .get()
                        .await()
                    
                    if (customerDoc.exists()) {
                        val fetchedBusinessName = customerDoc.getString("businessName")
                        if (!fetchedBusinessName.isNullOrBlank()) {
                            businessName = fetchedBusinessName
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TracuraDashboard", "Error fetching business name: ${e.message}")
            }
        }
    }
    
    // Tab state
    var selectedTab by remember { mutableStateOf(0) } // 0 = Cost Insights, 1 = Project Insights
    
    // Filter states
    var selectedDateRange by remember { mutableStateOf("16 May - 16 Nov") }
    var selectedProjectStatus by remember { mutableStateOf(setOf("ALL Status", "ACTIVE", "COMPLETED", "MAINTENANCE", "ARCHIVE", "SUSPENDED")) }
    var selectedProject by remember { mutableStateOf("All Projects") }
    var selectedStage by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedDepartment by remember { mutableStateOf("all") }
    
    // Dynamic data - loaded from repositories
    var allPhases by remember { mutableStateOf<Map<String, List<Phase>>>(emptyMap()) } // projectId -> phases
    var allExpenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var availablePhases by remember { mutableStateOf<List<String>>(emptyList()) }
    var availableDepartments by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingData by remember { mutableStateOf(false) }
    
    var showDateRangePicker by remember { mutableStateOf(false) }
    var showProjectStatusDropdown by remember { mutableStateOf(false) }
    var showProjectDropdown by remember { mutableStateOf(false) }
    var showStageDropdown by remember { mutableStateOf(false) }
    var showDepartmentDropdown by remember { mutableStateOf(false) }
    
    // Date picker states
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    // Load projects
    LaunchedEffect(Unit) {
        projectViewModel.loadProjects()
        // Initialize dates
        val calendar = Calendar.getInstance()
        calendar.set(2025, Calendar.MAY, 16)
        startDate = calendar.time
        calendar.set(2025, Calendar.NOVEMBER, 16)
        endDate = calendar.time
        updateDateRangeString(startDate, endDate) { selectedDateRange = it }
    }
    
    // Load phases and expenses for all projects dynamically
    LaunchedEffect(projects) {
        if (projects.isNotEmpty()) {
            isLoadingData = true
            scope.launch {
                try {
                    val phasesMap = mutableMapOf<String, List<Phase>>()
                    val phaseNamesSet = mutableSetOf<String>()
                    val expensesList = mutableListOf<Expense>()
                    
                    // Load data for all projects to populate dropdowns
                    projects.forEach { project ->
                        project.id?.let { projectId ->
                            try {
                                // Load phases dynamically
                                val phases = overallReportsViewModel.getPhasesForProject(projectId)
                                phasesMap[projectId] = phases
                                phases.forEach { phase ->
                                    phaseNamesSet.add(phase.phaseName)
                                }
                                
                                // Load ALL expenses and filter for approved ones
                                // This uses the new Firestore structure: customers/{customerId}/Projects/{projectId}/expenses
                                android.util.Log.d("TracuraDashboard", "🔄 Loading expenses for project: ${project.name} ($projectId)")
                                val allProjectExpenses = overallReportsViewModel.getAllExpensesForProject(projectId)
                                android.util.Log.d("TracuraDashboard", "📊 Found ${allProjectExpenses.size} total expenses for ${project.name}")
                                
                                // Filter for approved expenses only
                                val approvedExpenses = allProjectExpenses.filter { it.status == ExpenseStatus.APPROVED }
                                android.util.Log.d("TracuraDashboard", "✅ Found ${approvedExpenses.size} approved expenses for ${project.name}")
                                
                                if (approvedExpenses.isNotEmpty()) {
                                    approvedExpenses.forEach { expense ->
                                        android.util.Log.d("TracuraDashboard", "   💰 Approved: ${expense.category} - ₹${expense.amount} - Dept: ${expense.department} - Date: ${expense.getDateAsTimestamp()?.toDate()}")
                                    }
                                }
                                
                                expensesList.addAll(approvedExpenses)
                            } catch (e: Exception) {
                                android.util.Log.e("TracuraDashboard", "❌ Error loading data for project $projectId: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    }
                    
                    android.util.Log.d("TracuraDashboard", "📊 Total approved expenses loaded: ${expensesList.size}")
                    
                    allPhases = phasesMap
                    allExpenses = expensesList
                    availablePhases = phaseNamesSet.sorted()
                } catch (e: Exception) {
                    android.util.Log.e("TracuraDashboard", "Error loading phases/expenses: ${e.message}")
                } finally {
                    isLoadingData = false
                }
            }
        }
    }
    
    // Update available departments - from selected phase or all phases
    LaunchedEffect(selectedStage, allPhases) {
        val departmentsSet = mutableSetOf<String>()
        
        if (selectedStage.size == 1) {
            // Get departments from selected phase
            val selectedPhaseName = selectedStage.first()
            allPhases.forEach { (_, phases) ->
                phases.firstOrNull { it.phaseName == selectedPhaseName }?.let { phase ->
                    departmentsSet.addAll(phase.departments.keys)
                }
            }
        } else {
            // Get all departments from all phases
            allPhases.values.flatten().forEach { phase ->
                departmentsSet.addAll(phase.departments.keys)
            }
        }
        
        availableDepartments = departmentsSet.sorted()
    }
    
    // Calculate filtered projects based on all filters
    val filteredProjects = remember(projects, selectedProjectStatus, selectedProject, selectedStage, selectedDepartment, startDate, endDate) {
        var filtered = projects
        
        // Filter by project status
        if (!selectedProjectStatus.contains("ALL Status")) {
            filtered = filtered.filter { project ->
                val status = project.status
                val isSuspended = project.isSuspended == true
                selectedProjectStatus.any { selectedStatus ->
                    when (selectedStatus) {
                        "ACTIVE" -> status == "ACTIVE" && !isSuspended
                        "COMPLETED" -> status == "COMPLETED"
                        "SUSPENDED" -> isSuspended
                        "MAINTENANCE" -> !project.maintenanceDate.isNullOrBlank()
                        "ARCHIVE" -> status == "ARCHIVE"
                        else -> false
                    }
                }
            }
        }
        
        // Filter by selected project
        if (selectedProject != "All Projects") {
            filtered = filtered.filter { it.name == selectedProject }
        }
        
        filtered
    }
    
    // Calculate Total Budget, Total Spent, and Remaining dynamically
    val financialSummary = remember(filteredProjects, allExpenses, allPhases, selectedStage, selectedDepartment, startDate, endDate) {
        var totalBudget = 0.0
        var totalSpent = 0.0
        
        filteredProjects.forEach { project ->
            project.id?.let { projectId ->
                // Calculate budget based on filters
                if (selectedStage.isEmpty() && selectedDepartment == "all") {
                    // No stage/department filter - use project budget
                    totalBudget += project.budget
                } else if (selectedStage.isNotEmpty()) {
                    // Filter by stage - sum budgets from selected stages
                    val phases = allPhases[projectId] ?: emptyList()
                    selectedStage.forEach { stageName ->
                        phases.firstOrNull { it.phaseName == stageName }?.let { phase ->
                            if (selectedDepartment == "all") {
                                // Sum all departments in this phase
                                totalBudget += phase.departments.values.sum()
                            } else {
                                // Use specific department budget
                                totalBudget += phase.departments[selectedDepartment] ?: 0.0
                            }
                        }
                    }
                } else if (selectedDepartment != "all") {
                    // Filter by department only (no stage filter)
                    // Sum department budgets across all phases
                    val phases = allPhases[projectId] ?: emptyList()
                    phases.forEach { phase ->
                        totalBudget += phase.departments[selectedDepartment] ?: 0.0
                    }
                    // If no phases, use project budget as fallback
                    if (phases.isEmpty()) {
                        totalBudget += project.budget
                    }
                } else {
                    // Use project budget
                    totalBudget += project.budget
                }
                
                // Calculate spent from expenses - filter by all criteria
                val projectExpenses = allExpenses.filter { expense ->
                    // Match project
                    val matchesProject = expense.projectId == projectId
                    if (!matchesProject) return@filter false
                    
                    // Match status - only count APPROVED expenses
                    val matchesStatus = expense.status == ExpenseStatus.APPROVED
                    if (!matchesStatus) return@filter false
                    
                    // Filter by date range - properly handle date comparison
                    val matchesDateRange = if (startDate != null && endDate != null) {
                        try {
                            val expenseDate = expense.getDateAsTimestamp()?.toDate() ?: expense.submittedAt?.toDate()
                            if (expenseDate != null) {
                                // Normalize dates to midnight for proper comparison
                                val startCal = Calendar.getInstance().apply {
                                    time = startDate
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val endCal = Calendar.getInstance().apply {
                                    time = endDate
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }
                                val expenseCal = Calendar.getInstance().apply { time = expenseDate }
                                
                                val inRange = !expenseCal.before(startCal) && !expenseCal.after(endCal)
                                if (!inRange) {
                                    android.util.Log.d("TracuraDashboard", "Expense ${expense.id} excluded: date ${expenseDate} not in range [$startDate - $endDate]")
                                }
                                inRange
                            } else {
                                // If expense has no date but date filter is set, include it to be safe
                                // (approved expenses should be counted even if date is missing)
                                android.util.Log.d("TracuraDashboard", "Expense ${expense.id} has no date, including it anyway")
                                true
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("TracuraDashboard", "Error filtering expense by date: ${e.message}")
                            // Include expense if date parsing fails to avoid losing data
                            true
                        }
                    } else {
                        // No date filter - include all expenses
                        true
                    }
                    if (!matchesDateRange) return@filter false
                    
                    // Filter by stage
                    val matchesStage = if (selectedStage.isEmpty()) {
                        true
                    } else {
                        val phases = allPhases[projectId] ?: emptyList()
                        if (phases.isEmpty()) {
                            // No phases for this project, but stage filter is set - include expense anyway
                            // (expense is still valid even if project has no phases defined)
                            android.util.Log.d("TracuraDashboard", "Expense ${expense.id}: no phases found for project, including anyway")
                            true
                        } else {
                            // Check if expense phaseId matches any selected stage
                            val expensePhaseId = expense.phaseId
                            if (expensePhaseId.isNullOrBlank()) {
                                // Expense has no phaseId - include it anyway if it matches other filters
                                // (approved expenses should be counted even if phaseId is missing)
                                android.util.Log.d("TracuraDashboard", "Expense ${expense.id}: no phaseId but matches project, including anyway")
                                true
                            } else {
                                val matches = selectedStage.any { stageName ->
                                    phases.any { it.phaseName == stageName && it.id == expensePhaseId }
                                }
                                if (!matches) {
                                    android.util.Log.d("TracuraDashboard", "Expense ${expense.id} excluded: phaseId $expensePhaseId doesn't match selected stages $selectedStage")
                                } else {
                                    android.util.Log.d("TracuraDashboard", "Expense ${expense.id}: phaseId $expensePhaseId matches selected stages")
                                }
                                matches
                            }
                        }
                    }
                    if (!matchesStage) return@filter false
                    
                    // Filter by department
                    val matchesDepartment = if (selectedDepartment == "all") {
                        true
                    } else {
                        val expenseDept = expense.department?.trim() ?: ""
                        val selectedDept = selectedDepartment.trim()
                        val matches = expenseDept.equals(selectedDept, ignoreCase = true)
                        if (!matches) {
                            android.util.Log.d("TracuraDashboard", "Expense ${expense.id} excluded: department '$expenseDept' doesn't match '$selectedDept'")
                        }
                        matches
                    }
                    if (!matchesDepartment) return@filter false
                    
                    // All filters passed
                    true
                }
                
                // Calculate total spent and log each expense
                var projectSpent = 0.0
                projectExpenses.forEach { expense ->
                    projectSpent += expense.amount
                    android.util.Log.d("TracuraDashboard", "   ✅ INCLUDED Expense: ${expense.id} - ₹${expense.amount} - ${expense.category} - Dept: ${expense.department} - PhaseId: ${expense.phaseId ?: "none"} - Date: ${expense.getDateAsTimestamp()?.toDate()}")
                }
                totalSpent += projectSpent
                
                android.util.Log.d("TracuraDashboard", "💰 Project: ${project.name} (${project.id})")
                android.util.Log.d("TracuraDashboard", "   Total approved expenses in project: ${allExpenses.count { it.projectId == projectId && it.status == ExpenseStatus.APPROVED }}")
                android.util.Log.d("TracuraDashboard", "   Filtered expenses (after all filters): ${projectExpenses.size}")
                android.util.Log.d("TracuraDashboard", "   Total Spent for this project: ₹$projectSpent")
                android.util.Log.d("TracuraDashboard", "   Filters applied: Stage=$selectedStage, Dept=$selectedDepartment, DateRange=[$startDate - $endDate]")
            }
        }
        
        val remaining = maxOf(0.0, totalBudget - totalSpent)
        
        // Summary logging
        val totalApprovedExpenses = allExpenses.count { it.status == ExpenseStatus.APPROVED }
        val filteredApprovedExpenses = allExpenses.count { expense ->
            expense.status == ExpenseStatus.APPROVED &&
            filteredProjects.any { it.id == expense.projectId }
        }
        
        android.util.Log.d("TracuraDashboard", "📊 FINANCIAL SUMMARY:")
        android.util.Log.d("TracuraDashboard", "   Total Budget: ₹$totalBudget")
        android.util.Log.d("TracuraDashboard", "   Total Spent: ₹$totalSpent")
        android.util.Log.d("TracuraDashboard", "   Remaining: ₹$remaining")
        android.util.Log.d("TracuraDashboard", "   Total approved expenses (all projects): $totalApprovedExpenses")
        android.util.Log.d("TracuraDashboard", "   Approved expenses in filtered projects: $filteredApprovedExpenses")
        android.util.Log.d("TracuraDashboard", "   Active Filters: Projects=${filteredProjects.size}, Stage=$selectedStage, Dept=$selectedDepartment, DateRange=[$startDate - $endDate]")
        
        Triple(totalBudget, totalSpent, remaining)
    }
    
    Scaffold(
        topBar = {
            // Light grey rounded card header matching reference
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF3F4F6), // Light grey background
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F4F6))
                ) {
                    // Header content with close button on left and centered title
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 16.dp) // Add top padding to move down from status bar
                    ) {
                        // Close button on left
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterStart)
                                .padding(start = 20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF60A5FA), // Light blue
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // Title and subtitle - centered
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = businessName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827), // Dark black
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Reports & Analytics",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280), // Light grey
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    // Tabs with pill-shaped buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TabButton(
                            text = "Cost Insights",
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            text = "Project Insights",
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F4F6))
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                // Filters Row
                FilterRow(
                    selectedDateRange = selectedDateRange,
                    selectedProjectStatus = selectedProjectStatus,
                    selectedProject = selectedProject,
                    selectedStage = selectedStage,
                    selectedDepartment = selectedDepartment,
                    projects = listOf("All Projects") + projects.map { it.name },
                    availablePhases = availablePhases,
                    availableDepartments = availableDepartments,
                    onDateRangeClick = { showDateRangePicker = true },
                    onProjectStatusClick = { showProjectStatusDropdown = !showProjectStatusDropdown },
                    onProjectClick = { showProjectDropdown = !showProjectDropdown },
                    onStageClick = { showStageDropdown = !showStageDropdown },
                    onDepartmentClick = { showDepartmentDropdown = !showDepartmentDropdown },
                    onProjectStatusToggle = { status ->
                        val newSet = selectedProjectStatus.toMutableSet()
                        if (status == "ALL Status") {
                            if (newSet.contains("ALL Status")) {
                                newSet.clear()
                            } else {
                                newSet.clear()
                                newSet.add("ALL Status")
                                newSet.addAll(listOf("ACTIVE", "COMPLETED", "MAINTENANCE", "ARCHIVE", "SUSPENDED"))
                            }
                        } else {
                            newSet.remove("ALL Status")
                            if (newSet.contains(status)) {
                                newSet.remove(status)
                            } else {
                                newSet.add(status)
                            }
                        }
                        selectedProjectStatus = newSet
                    },
                    onProjectSelected = { 
                        selectedProject = it
                        showProjectDropdown = false
                    },
                    onStageToggle = { stage ->
                        val newSet = selectedStage.toMutableSet()
                        if (newSet.contains(stage)) {
                            newSet.remove(stage)
                        } else {
                            newSet.add(stage)
                        }
                        selectedStage = newSet
                    },
                    onDepartmentSelected = { dept ->
                        selectedDepartment = dept
                        showDepartmentDropdown = false
                    },
                    showProjectStatusDropdown = showProjectStatusDropdown,
                    showProjectDropdown = showProjectDropdown,
                    showStageDropdown = showStageDropdown,
                    showDepartmentDropdown = showDepartmentDropdown,
                    onDismissProjectStatus = { showProjectStatusDropdown = false },
                    onDismissProject = { showProjectDropdown = false },
                    onDismissStage = { showStageDropdown = false },
                    onDismissDepartment = { showDepartmentDropdown = false }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // KPI Cards - Total Budget, Total Spent, Remaining
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KPICard(
                        label = "Total Budget",
                        value = "₹${formatNumber(financialSummary.first)}",
                        modifier = Modifier.weight(1f)
                    )
                    KPICard(
                        label = "Total Spent",
                        value = "₹${formatNumber(financialSummary.second)}",
                        modifier = Modifier.weight(1f)
                    )
                    KPICard(
                        label = "Remaining",
                        value = "₹${formatNumber(financialSummary.third)}",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Content based on selected tab
                if (selectedTab == 0) {
                    CostInsightsContent(
                        selectedProject = selectedProject,
                        selectedStage = selectedStage,
                        selectedDepartment = selectedDepartment,
                        selectedProjectStatus = selectedProjectStatus,
                        projects = filteredProjects,
                        allPhases = allPhases,
                        allExpenses = allExpenses,
                        startDate = startDate,
                        endDate = endDate
                    )
                } else {
                    ProjectInsightsContent(
                        selectedProject = selectedProject,
                        selectedStage = selectedStage,
                        selectedDepartment = selectedDepartment,
                        selectedProjectStatus = selectedProjectStatus,
                        projects = filteredProjects,
                        allPhases = allPhases,
                        allExpenses = allExpenses,
                        startDate = startDate,
                        endDate = endDate,
                        onNavigateToProject = onNavigateToProject
                    )
                }
            }
        }
    }
    
    // Date Range Picker Dialog
    if (showDateRangePicker) {
        DateRangePickerDialog(
            startDate = startDate,
            endDate = endDate,
            onStartDateSelected = { 
                startDate = it
                updateDateRangeString(startDate, endDate) { selectedDateRange = it }
            },
            onEndDateSelected = { 
                endDate = it
                updateDateRangeString(startDate, endDate) { selectedDateRange = it }
            },
            onDismiss = { showDateRangePicker = false }
        )
    }
    
    // Update date range string when dates change
    LaunchedEffect(startDate, endDate) {
        updateDateRangeString(startDate, endDate) { selectedDateRange = it }
    }
}

fun updateDateRangeString(startDate: Date?, endDate: Date?, onUpdate: (String) -> Unit) {
    if (startDate != null && endDate != null) {
        // Use compact format: "dd MMM - dd MMM" or "dd - dd MMM" if same month
        val dayFormatter = SimpleDateFormat("dd", Locale.getDefault())
        val monthFormatter = SimpleDateFormat("MMM", Locale.getDefault())
        val startDay = dayFormatter.format(startDate)
        val endDay = dayFormatter.format(endDate)
        val startMonth = monthFormatter.format(startDate)
        val endMonth = monthFormatter.format(endDate)
        
        val formattedRange = if (startMonth == endMonth) {
            // Same month: "16 - 17 May"
            "$startDay - $endDay $startMonth"
        } else {
            // Different months: "16 May - 16 Nov"
            "$startDay $startMonth - $endDay $endMonth"
        }
        onUpdate(formattedRange)
    } else if (startDate != null) {
        val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
        onUpdate(formatter.format(startDate))
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(999.dp), // Pill shape
        color = if (isSelected) Color(0xFF2563EB) else Color.White, // Blue when selected, white when not
        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE5E7EB)) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                color = if (isSelected) Color.White else Color(0xFF111827), // White when selected, black when not
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun FilterRow(
    selectedDateRange: String,
    selectedProjectStatus: Set<String>,
    selectedProject: String,
    selectedStage: Set<String>,
    selectedDepartment: String,
    projects: List<String>,
    availablePhases: List<String>,
    availableDepartments: List<String>,
    onDateRangeClick: () -> Unit,
    onProjectStatusClick: () -> Unit,
    onProjectClick: () -> Unit,
    onStageClick: () -> Unit,
    onDepartmentClick: () -> Unit,
    onProjectStatusToggle: (String) -> Unit,
    onProjectSelected: (String) -> Unit,
    onStageToggle: (String) -> Unit,
    onDepartmentSelected: (String) -> Unit,
    showProjectStatusDropdown: Boolean,
    showProjectDropdown: Boolean,
    showStageDropdown: Boolean,
    showDepartmentDropdown: Boolean,
    onDismissProjectStatus: () -> Unit,
    onDismissProject: () -> Unit,
    onDismissStage: () -> Unit,
    onDismissDepartment: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // First Row: Date Range, Project Status, Project
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date Range Filter - Give it more space (1.2x weight)
            FilterButton(
                label = "DATE RANGE",
                value = selectedDateRange,
                icon = Icons.Default.DateRange,
                onClick = onDateRangeClick,
                modifier = Modifier.weight(1.2f)
            )
            
            // Project Status Filter
            Box(modifier = Modifier.weight(1f)) {
                FilterButton(
                    label = "PROJECT STATUS",
                    value = if (selectedProjectStatus.contains("ALL Status")) "ALL Status" else "${selectedProjectStatus.size} selected",
                    icon = Icons.Default.KeyboardArrowDown,
                    onClick = onProjectStatusClick,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (showProjectStatusDropdown) {
                    ProjectStatusDropdown(
                        selectedStatuses = selectedProjectStatus,
                        onStatusToggle = onProjectStatusToggle,
                        onDismiss = onDismissProjectStatus,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp)
                    )
                }
            }
            
            // Project Filter
            Box(modifier = Modifier.weight(1f)) {
                FilterButton(
                    label = "PROJECT",
                    value = selectedProject,
                    icon = Icons.Default.KeyboardArrowDown,
                    onClick = onProjectClick,
                    modifier = Modifier.fillMaxWidth()
                )
                
                DropdownMenu(
                    expanded = showProjectDropdown,
                    onDismissRequest = onDismissProject,
                    modifier = Modifier.background(Color.White, RoundedCornerShape(8.dp))
                ) {
                    projects.forEach { project ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = project,
                                    fontSize = 13.sp,
                                    color = Color(0xFF111827)
                                )
                            },
                            onClick = {
                                onProjectSelected(project)
                            }
                        )
                    }
                }
            }
        }
        
        // Second Row: Stage/Phase Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Stage/Phase Filter
            Box(modifier = Modifier.weight(1f)) {
                FilterButton(
                    label = "STAGE",
                    value = if (selectedStage.isEmpty()) "All Stages" else "${selectedStage.size} selected",
                    icon = Icons.Default.KeyboardArrowDown,
                    onClick = onStageClick,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (showStageDropdown) {
                    StageDropdown(
                        selectedStages = selectedStage,
                        availablePhases = availablePhases,
                        onStageToggle = onStageToggle,
                        onDismiss = onDismissStage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp)
                    )
                }
            }
            
            // Department Filter - always show, but populate based on phase selection
            Box(modifier = Modifier.weight(1f)) {
                FilterButton(
                    label = "DEPARTMENT",
                    value = if (selectedDepartment == "all") {
                        "All Depart..."
                    } else {
                        selectedDepartment
                    },
                    icon = Icons.Default.KeyboardArrowDown,
                    onClick = onDepartmentClick,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (showDepartmentDropdown) {
                    DropdownMenu(
                        expanded = showDepartmentDropdown,
                        onDismissRequest = onDismissDepartment,
                        modifier = Modifier.background(Color.White, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Departments", fontSize = 13.sp, color = Color(0xFF111827)) },
                            onClick = {
                                onDepartmentSelected("all")
                            }
                        )
                        availableDepartments.forEach { dept ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = dept,
                                        fontSize = 13.sp,
                                        color = Color(0xFF111827)
                                    )
                                },
                                onClick = {
                                    onDepartmentSelected(dept)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterButton(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon == Icons.Default.DateRange) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp), // Smaller icon for date range
                            tint = Color(0xFF6B7280)
                        )
                    }
                    Text(
                        text = value,
                        fontSize = if (icon == Icons.Default.DateRange) 11.sp else 12.sp, // Smaller font for date range
                        color = Color(0xFF111827),
                        maxLines = if (icon == Icons.Default.DateRange) 1 else 2, // Single line for date range
                        overflow = if (icon == Icons.Default.DateRange) TextOverflow.Ellipsis else TextOverflow.Visible, // Ellipsis for date range if too long
                        lineHeight = if (icon == Icons.Default.DateRange) 14.sp else 16.sp, // Compact line height for date range
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF6B7280)
                )
            }
        }
    }
}

@Composable
fun ProjectStatusDropdown(
    selectedStatuses: Set<String>,
    onStatusToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            val statuses = listOf("ALL Status", "ACTIVE", "COMPLETED", "MAINTENANCE", "ARCHIVE", "SUSPENDED")
            statuses.forEach { status ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStatusToggle(status) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = status,
                        fontSize = 14.sp,
                        color = Color(0xFF111827)
                    )
                    if (selectedStatuses.contains(status)) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF2563EB)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StageDropdown(
    selectedStages: Set<String>,
    availablePhases: List<String>,
    onStageToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            availablePhases.forEach { phase ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStageToggle(phase) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = phase,
                        fontSize = 14.sp,
                        color = Color(0xFF111827)
                    )
                    if (selectedStages.contains(phase)) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF2563EB)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KPICard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color(0xFF94A3B8).copy(alpha = 0.25f)),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF6B7280)
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111827)
            )
        }
    }
}

@Composable
fun CostInsightsContent(
    selectedProject: String = "all",
    selectedStage: Set<String> = emptySet(),
    selectedDepartment: String = "all",
    selectedProjectStatus: Set<String> = emptySet(),
    projects: List<Project> = emptyList(),
    allPhases: Map<String, List<Phase>> = emptyMap(),
    allExpenses: List<Expense> = emptyList(),
    startDate: Date? = null,
    endDate: Date? = null
) {
    // Calculate real monthly cost data from approved expenses
    // Use submittedAt field for grouping by month (as per Firebase schema)
    // Filter by: status=APPROVED, date range, selected projects, selected stages, selected departments
    val costTrendData = remember(projects, allExpenses, selectedStage, selectedDepartment, startDate, endDate, allPhases) {
        // Get date range for calculation - use provided dates or default to last 6 months
        val calcStartDate = startDate ?: Calendar.getInstance().apply {
            add(Calendar.MONTH, -5) // Default to last 6 months
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val calcEndDate = endDate ?: Date()
        
        // Generate list of months in range
        val calendar = Calendar.getInstance()
        calendar.time = calcStartDate
        val monthsList = mutableListOf<Pair<String, Calendar>>()
        val monthFormatter = SimpleDateFormat("MMM", Locale.getDefault())
        
        while (calendar.time <= calcEndDate) {
            monthsList.add(Pair(monthFormatter.format(calendar.time), calendar.clone() as Calendar))
            calendar.add(Calendar.MONTH, 1)
        }
        
        // Filter expenses based on all criteria
        val allFilteredExpenses = mutableListOf<Expense>()
        
        projects.forEach { project ->
            project.id?.let { projectId ->
                val projectExpenses = allExpenses.filter { expense ->
                    // 1. Match project
                    val matchesProject = expense.projectId == projectId
                if (!matchesProject) return@filter false
                
                    // 2. Match status - ONLY APPROVED expenses
                val matchesStatus = expense.status == ExpenseStatus.APPROVED
                if (!matchesStatus) return@filter false
                
                    // 3. Filter by date range using submittedAt (primary) or date (fallback)
                    val matchesDateRange = if (startDate != null && endDate != null) {
                        // Use submittedAt as primary field (as per requirements)
                        val expenseDate = expense.submittedAt?.toDate() ?: expense.getDateAsTimestamp()?.toDate()
                        if (expenseDate != null) {
                            val expenseCal = Calendar.getInstance().apply { time = expenseDate }
                            val startCal = Calendar.getInstance().apply {
                                time = startDate
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val endCal = Calendar.getInstance().apply {
                                time = endDate
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }
                            !expenseCal.before(startCal) && !expenseCal.after(endCal)
                        } else {
                            false // Exclude expenses without submittedAt/date for monthly breakdown
                        }
                    } else {
                        true // No date filter
                    }
                    if (!matchesDateRange) return@filter false
                    
                    // 4. Filter by stage (phaseName) - match expense.phaseId with selected stage phaseName
                val matchesStage = if (selectedStage.isEmpty()) {
                    true
                } else {
                    val phases = allPhases[projectId] ?: emptyList()
                    if (phases.isEmpty()) {
                            true // No phases for this project, include expense anyway
                    } else {
                        val expensePhaseId = expense.phaseId
                        if (expensePhaseId.isNullOrBlank()) {
                                true // Expense has no phaseId, include it anyway
                        } else {
                                // Check if expense's phaseId matches any selected stage's phaseName
                            selectedStage.any { stageName ->
                                phases.any { it.phaseName == stageName && it.id == expensePhaseId }
                            }
                        }
                    }
                }
                if (!matchesStage) return@filter false
                
                    // 5. Filter by department - match expense.department with selected department
                val matchesDepartment = if (selectedDepartment == "all") {
                    true
                } else {
                    val expenseDept = expense.department?.trim() ?: ""
                    val selectedDept = selectedDepartment.trim()
                    expenseDept.equals(selectedDept, ignoreCase = true)
                }
                if (!matchesDepartment) return@filter false
                
                    // All filters passed
                    true
                }
                
                allFilteredExpenses.addAll(projectExpenses)
            }
        }
        
        // Debug: Log total filtered expenses
        android.util.Log.d("CostTrend", "🔍 Total filtered expenses (status=APPROVED, after all filters): ${allFilteredExpenses.size}")
        android.util.Log.d("CostTrend", "   Total amount: ₹${allFilteredExpenses.sumOf { it.amount }}")
        android.util.Log.d("CostTrend", "   Filters: Projects=${projects.size}, Stages=$selectedStage, Dept=$selectedDepartment, DateRange=[$startDate - $endDate]")
        
        // Calculate monthly costs by grouping filtered expenses by month using submittedAt
        val monthlyCosts = monthsList.map { (monthName, monthStart) ->
            val monthEnd = (monthStart.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            
            // Filter expenses for this specific month using submittedAt (primary) or date (fallback)
            val monthExpenses = allFilteredExpenses.filter { expense ->
                // Use submittedAt as primary field for monthly grouping
                val expenseDate = expense.submittedAt?.toDate() ?: expense.getDateAsTimestamp()?.toDate()
                if (expenseDate != null) {
                    val expenseCal = Calendar.getInstance().apply { time = expenseDate }
                    !expenseCal.before(monthStart) && !expenseCal.after(monthEnd)
                } else {
                    false // Exclude expenses without submittedAt/date
                }
            }
            
            val monthTotal = monthExpenses.sumOf { it.amount }
            
            // Debug logging for each month
            if (monthExpenses.isNotEmpty()) {
                android.util.Log.d("CostTrend", "   📅 $monthName: ${monthExpenses.size} expenses, Total: ₹$monthTotal")
                monthExpenses.take(5).forEach { expense -> // Log first 5 to avoid spam
                    val expDate = expense.submittedAt?.toDate() ?: expense.getDateAsTimestamp()?.toDate()
                    android.util.Log.d("CostTrend", "      - Expense ${expense.id}: ₹${expense.amount} (submittedAt: $expDate, Dept: ${expense.department}, PhaseId: ${expense.phaseId})")
                }
            }
            
            Pair(monthName, monthTotal) // Keep in actual amount (not Cr)
        }
        
        monthlyCosts
    }
    
    val months = costTrendData.map { it.first }
    val costValues = costTrendData.map { it.second }
    
    // Debug logging for monthly costs
    android.util.Log.d("CostTrend", "📊 Monthly Cost Data:")
    costTrendData.forEach { (month, cost) ->
        val formatted = formatNumber(cost)
        android.util.Log.d("CostTrend", "   $month: ₹$cost ($formatted)")
    }
    val maxMonthly = costValues.maxOrNull() ?: 0.0
    val sumMonthly = costValues.sum()
    android.util.Log.d("CostTrend", "   Max monthly cost: ₹$maxMonthly (${formatNumber(maxMonthly)})")
    android.util.Log.d("CostTrend", "   Sum of monthly costs: ₹$sumMonthly (${formatNumber(sumMonthly)})")
    
    // Calculate total spent - use the same filtered expenses as monthly calculation for consistency
    // This ensures the total matches the sum of monthly costs
    val totalSpent = remember(costTrendData) {
        // Sum of all monthly costs should equal total spent
        costValues.sum()
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cost Trend Chart
        CostTrendChartCard(
            title = "Cost Trend (MoM)",
            subtitle = "Monthly total cost · ₹",
            totalValue = if (totalSpent > 0) formatNumber(totalSpent) else "0",
            months = months,
            costValues = costValues
        )
        
        // Stage Budget vs Actual - Show only when selectedStage.size > 1
        if (selectedStage.size > 1) {
            val stageBudgetVsActualData = remember(selectedStage, projects, allPhases, allExpenses, selectedDepartment, startDate, endDate) {
                val data = mutableListOf<Triple<String, Double, Double>>() // phaseName, budget, actual
                
                // For each selected stage/phase, calculate total budget and actual across all filtered projects
                selectedStage.forEach { phaseName ->
                    var totalPhaseBudget = 0.0
                    var totalPhaseSpent = 0.0
                    
                    projects.forEach { project ->
                        project.id?.let { projectId ->
                            val phases = allPhases[projectId] ?: emptyList()
                            val phase = phases.firstOrNull { it.phaseName == phaseName }
                            
                            if (phase != null) {
                                // Calculate phase budget (sum of all departments or specific department)
                                val phaseBudget = if (selectedDepartment == "all") {
                                    phase.departments.values.sum()
                                } else {
                                    phase.departments[selectedDepartment] ?: 0.0
                                }
                                totalPhaseBudget += phaseBudget
                                
                                // Calculate actual spent from approved expenses for this project and phase
                                val phaseExpenses = allExpenses.filter { expense ->
                                    val matchesProject = expense.projectId == projectId
                                    val matchesPhase = expense.phaseId == phase.id
                                    val matchesStatus = expense.status == ExpenseStatus.APPROVED
                                    val matchesDepartment = selectedDepartment == "all" || 
                                        expense.department.equals(selectedDepartment, ignoreCase = true)
                                    
                                    // Filter by date range
                                    val matchesDateRange = if (startDate != null && endDate != null) {
                                        try {
                                            val expenseDate = expense.getDateAsTimestamp()?.toDate() ?: expense.submittedAt?.toDate()
                                            if (expenseDate != null) {
                                                val startCal = Calendar.getInstance().apply {
                                                    time = startDate
                                                    set(Calendar.HOUR_OF_DAY, 0)
                                                    set(Calendar.MINUTE, 0)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                val endCal = Calendar.getInstance().apply {
                                                    time = endDate
                                                    set(Calendar.HOUR_OF_DAY, 23)
                                                    set(Calendar.MINUTE, 59)
                                                    set(Calendar.SECOND, 59)
                                                    set(Calendar.MILLISECOND, 999)
                                                }
                                                val expenseCal = Calendar.getInstance().apply { time = expenseDate }
                                                !expenseCal.before(startCal) && !expenseCal.after(endCal)
                                            } else {
                                                true // Include expenses without dates
                                            }
                                        } catch (e: Exception) {
                                            true
                                        }
                                    } else {
                                        true
                                    }
                                    
                                    matchesProject && matchesPhase && matchesStatus && matchesDepartment && matchesDateRange
                                }
                                
                                totalPhaseSpent += phaseExpenses.sumOf { it.amount }
                            }
                        }
                    }
                    
                    // Keep values in raw rupees (not converted to Cr) - formatNumber will handle formatting
                    // Only add if there's budget or actual spending
                    if (totalPhaseBudget > 0 || totalPhaseSpent > 0) {
                        data.add(Triple(phaseName, totalPhaseBudget, totalPhaseSpent))
                    }
                }
                
                data
            }
            
            if (stageBudgetVsActualData.isNotEmpty()) {
                ChartCard(
                    title = "Stage Budget vs Actual",
                    subtitle = "₹ · Budget vs Actuals"
                ) {
                    BarChartWithTruncatedLabels(
                        labels = stageBudgetVsActualData.map { it.first },
                        budgetData = stageBudgetVsActualData.map { it.second },
                        actualData = stageBudgetVsActualData.map { it.third },
                        modifier = Modifier.height(150.dp)
                    )
                }
            }
        }
        
        // Project-wise Budget vs Actual - Show only when filteredProjects count > 1
        if (projects.size > 1) {
            val projectWiseData = remember(projects, allPhases, allExpenses, selectedStage, selectedDepartment, startDate, endDate) {
                val data = mutableListOf<Triple<String, Double, Double>>() // projectName, budget, actual
                
                projects.forEach { project ->
                    project.id?.let { projectId ->
                        var projectBudget = 0.0
                        var projectSpent = 0.0
                        
                        // Calculate budget based on filters (same logic as financialSummary)
                        if (selectedStage.isEmpty() && selectedDepartment == "all") {
                            // No stage/department filter - use project budget
                            projectBudget = project.budget
                        } else if (selectedStage.isNotEmpty()) {
                            // Filter by stage - sum budgets from selected stages
                            val phases = allPhases[projectId] ?: emptyList()
                            selectedStage.forEach { stageName ->
                                phases.firstOrNull { it.phaseName == stageName }?.let { phase ->
                                    if (selectedDepartment == "all") {
                                        // Sum all departments in this phase
                                        projectBudget += phase.departments.values.sum()
                                    } else {
                                        // Use specific department budget
                                        projectBudget += phase.departments[selectedDepartment] ?: 0.0
                                    }
                                }
                            }
                        } else if (selectedDepartment != "all") {
                            // Filter by department only (no stage filter)
                            // Sum department budgets across all phases
                            val phases = allPhases[projectId] ?: emptyList()
                            phases.forEach { phase ->
                                projectBudget += phase.departments[selectedDepartment] ?: 0.0
                            }
                            // If no phases, use project budget as fallback
                            if (phases.isEmpty()) {
                                projectBudget = project.budget
                            }
                        } else {
                            // Use project budget
                            projectBudget = project.budget
                        }
                        
                        // Calculate spent from expenses (same filtering logic as financialSummary)
                        val projectExpenses = allExpenses.filter { expense ->
                            // Match project
                            val matchesProject = expense.projectId == projectId
                            if (!matchesProject) return@filter false
                            
                            // Match status - only count APPROVED expenses
                            val matchesStatus = expense.status == ExpenseStatus.APPROVED
                            if (!matchesStatus) return@filter false
                            
                            // Filter by date range
                            val matchesDateRange = if (startDate != null && endDate != null) {
                                try {
                                    val expenseDate = expense.getDateAsTimestamp()?.toDate() ?: expense.submittedAt?.toDate()
                                    if (expenseDate != null) {
                                        val startCal = Calendar.getInstance().apply {
                                            time = startDate
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        val endCal = Calendar.getInstance().apply {
                                            time = endDate
                                            set(Calendar.HOUR_OF_DAY, 23)
                                            set(Calendar.MINUTE, 59)
                                            set(Calendar.SECOND, 59)
                                            set(Calendar.MILLISECOND, 999)
                                        }
                                        val expenseCal = Calendar.getInstance().apply { time = expenseDate }
                                        !expenseCal.before(startCal) && !expenseCal.after(endCal)
                                    } else {
                                        true // Include expenses without dates
                                    }
                                } catch (e: Exception) {
                                    true
                                }
                            } else {
                                true
                            }
                            if (!matchesDateRange) return@filter false
                            
                            // Filter by stage
                            val matchesStage = if (selectedStage.isEmpty()) {
                                true
                            } else {
                                val phases = allPhases[projectId] ?: emptyList()
                                if (phases.isEmpty()) {
                                    true
                                } else {
                                    val expensePhaseId = expense.phaseId
                                    if (expensePhaseId.isNullOrBlank()) {
                                        true // Include expenses without phaseId
                                    } else {
                                        selectedStage.any { stageName ->
                                            phases.any { it.phaseName == stageName && it.id == expensePhaseId }
                                        }
                                    }
                                }
                            }
                            if (!matchesStage) return@filter false
                            
                            // Filter by department
                            val matchesDepartment = if (selectedDepartment == "all") {
                                true
                            } else {
                                val expenseDept = expense.department?.trim() ?: ""
                                val selectedDept = selectedDepartment.trim()
                                expenseDept.equals(selectedDept, ignoreCase = true)
                            }
                            if (!matchesDepartment) return@filter false
                            
                            true
                        }
                        
                        projectSpent = projectExpenses.sumOf { it.amount }
                        
                        // Keep values in raw rupees (not converted to Cr) - formatNumber will handle formatting
                        // Only add if there's budget or actual spending
                        if (projectBudget > 0 || projectSpent > 0) {
                            data.add(Triple(project.name, projectBudget, projectSpent))
                        }
                    }
                }
                
                data
            }
            
            if (projectWiseData.isNotEmpty()) {
                ChartCard(
                    title = "Project-wise Budget vs Actual",
                    subtitle = "₹ · Budget vs Actuals"
                ) {
                    BarChartWithTruncatedLabels(
                        labels = projectWiseData.map { it.first },
                        budgetData = projectWiseData.map { it.second },
                        actualData = projectWiseData.map { it.third },
                        modifier = Modifier.height(150.dp)
                    )
                }
            }
        }
        
        // Projects at Selected Stage - Show when single phase is selected
        if (selectedStage.size == 1) {
            val selectedPhaseName = selectedStage.first()
            
            // Find projects that have this phase and calculate budget vs actual
            val projectData = remember(selectedPhaseName, projects, allPhases, allExpenses, selectedDepartment, startDate, endDate) {
                val data = mutableListOf<Triple<String, Double, Double>>() // projectName, budget, actual
                
                projects.forEach { project ->
                    project.id?.let { projectId ->
                        val phases = allPhases[projectId] ?: emptyList()
                        val phase = phases.firstOrNull { it.phaseName == selectedPhaseName }
                        
                        if (phase != null) {
                            // Calculate phase budget (sum of all departments or specific department)
                            val phaseBudget = if (selectedDepartment == "all") {
                                phase.departments.values.sum()
                            } else {
                                phase.departments[selectedDepartment] ?: 0.0
                            }
                            
                            // Calculate actual spent from approved expenses for this project and phase
                            val phaseExpenses = allExpenses.filter { expense ->
                                val matchesProject = expense.projectId == projectId
                                if (!matchesProject) return@filter false
                                
                                val matchesPhase = expense.phaseId == phase.id
                                if (!matchesPhase) return@filter false
                                
                                val matchesStatus = expense.status == ExpenseStatus.APPROVED
                                if (!matchesStatus) return@filter false
                                
                                val matchesDepartment = selectedDepartment == "all" || 
                                    expense.department.equals(selectedDepartment, ignoreCase = true)
                                if (!matchesDepartment) return@filter false
                                
                                // Filter by date range
                                val matchesDateRange = if (startDate != null && endDate != null) {
                                    try {
                                        val expenseDate = expense.getDateAsTimestamp()?.toDate() ?: expense.submittedAt?.toDate()
                                        if (expenseDate != null) {
                                            val startCal = Calendar.getInstance().apply {
                                                time = startDate
                                                set(Calendar.HOUR_OF_DAY, 0)
                                                set(Calendar.MINUTE, 0)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            val endCal = Calendar.getInstance().apply {
                                                time = endDate
                                                set(Calendar.HOUR_OF_DAY, 23)
                                                set(Calendar.MINUTE, 59)
                                                set(Calendar.SECOND, 59)
                                                set(Calendar.MILLISECOND, 999)
                                            }
                                            val expenseCal = Calendar.getInstance().apply { time = expenseDate }
                                            !expenseCal.before(startCal) && !expenseCal.after(endCal)
                                        } else {
                                            true // Include expenses without dates
                                        }
                                    } catch (e: Exception) {
                                        true
                                    }
                                } else {
                                    true
                                }
                                
                                matchesDateRange
                            }
                            
                            val actualSpent = phaseExpenses.sumOf { it.amount } / 10000000.0 // Convert to Cr
                            val budgetInCr = phaseBudget / 10000000.0 // Convert to Cr
                            
                            if (budgetInCr > 0 || actualSpent > 0) {
                                data.add(Triple(project.name, budgetInCr, actualSpent))
                            }
                        }
                    }
                }
                
                data
            }
            
            if (projectData.isNotEmpty()) {
                ChartCard(
                    title = "Projects at Selected Stage",
                    subtitle = "Budget vs Actual at this stage across projects"
                ) {
                    BarChartWithTooltips(
                        labels = projectData.map { it.first },
                        budgetData = projectData.map { it.second },
                        actualData = projectData.map { it.third },
                        modifier = Modifier.height(150.dp)
                    )
                }
            } else {
                ChartCard(
                    title = "Projects at Selected Stage",
                    subtitle = "Budget vs Actual at this stage across projects"
                ) {
                    Text(
                        text = "No projects found with the selected phase.",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Sub-Category Spend - Full width, calculate from actual expense data
        val categorySpendData = remember(projects, allExpenses, selectedStage, selectedDepartment, startDate, endDate, allPhases) {
            // Filter expenses based on all criteria
            val filteredExpenses = allExpenses.filter { expense ->
                // Match project
                val matchesProject = projects.any { it.id == expense.projectId }
                if (!matchesProject) return@filter false
                
                // Match status - only count APPROVED expenses
                val matchesStatus = expense.status == ExpenseStatus.APPROVED
                if (!matchesStatus) return@filter false
                
                // Filter by date range using submittedAt (primary) or date (fallback)
                val matchesDateRange = if (startDate != null && endDate != null) {
                    val expenseDate = expense.submittedAt?.toDate() ?: expense.getDateAsTimestamp()?.toDate()
                    if (expenseDate != null) {
                        val expenseCal = Calendar.getInstance().apply { time = expenseDate }
                        val startCal = Calendar.getInstance().apply {
                            time = startDate
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val endCal = Calendar.getInstance().apply {
                            time = endDate
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        !expenseCal.before(startCal) && !expenseCal.after(endCal)
                    } else {
                        false
                    }
                } else {
                    true
                }
                if (!matchesDateRange) return@filter false
                
                // Filter by stage
                val matchesStage = if (selectedStage.isEmpty()) {
                    true
                } else {
                    val projectId = expense.projectId
                    val phases = allPhases[projectId] ?: emptyList()
                    if (phases.isEmpty()) {
                        true
                    } else {
                        val expensePhaseId = expense.phaseId
                        if (expensePhaseId.isNullOrBlank()) {
                            true
                        } else {
                            selectedStage.any { stageName ->
                                phases.any { it.phaseName == stageName && it.id == expensePhaseId }
                            }
                        }
                    }
                }
                if (!matchesStage) return@filter false
                
                // Filter by department
                val matchesDepartment = if (selectedDepartment == "all") {
                    true
                } else {
                    val expenseDept = expense.department?.trim() ?: ""
                    val selectedDept = selectedDepartment.trim()
                    expenseDept.equals(selectedDept, ignoreCase = true)
                }
                if (!matchesDepartment) return@filter false
                
                // Must have a category
                val hasCategory = expense.category.isNotEmpty()
                if (!hasCategory) return@filter false
                
                true
            }
            
            // Group by category and sum amounts
            val categoryMap = mutableMapOf<String, Double>()
            filteredExpenses.forEach { expense ->
                val category = expense.category.trim().takeIf { it.isNotEmpty() } ?: "Uncategorized"
                categoryMap[category] = (categoryMap[category] ?: 0.0) + expense.amount
            }
            
            // Sort by amount in descending order (maximum first)
            categoryMap.toList()
                .sortedByDescending { it.second }
                .map { Pair(it.first, it.second) }
        }
        
        if (categorySpendData.isNotEmpty()) {
            ChartCard(
                title = "Sub-Category Spend",
                subtitle = "Filtered by Project · Department"
            ) {
                // Calculate height based on number of categories (minimum 200dp, add 40dp per category)
                val chartHeight = maxOf(200.dp, (categorySpendData.size * 40).dp)
                HorizontalBarChart(
                    labels = categorySpendData.map { it.first },
                    values = categorySpendData.map { it.second },
                    modifier = Modifier.height(chartHeight)
                )
            }
        }
        
        // Two Column Layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cost by Project Status - Based on selected statuses
            ChartCard(
                title = "Cost by Project Status",
                subtitle = "₹ · Portfolio split",
                modifier = Modifier.weight(0.9f)
            ) {
                // Calculate cost by status - fetch ALL statuses dynamically from projects
                val statusCostData = remember(projects, allExpenses, startDate, endDate) {
                    val statusMap = mutableMapOf<String, Double>()
                    
                    // Helper function to determine the actual display status of a project
                    fun getProjectDisplayStatus(project: Project): String {
                        // Priority: Suspended > Maintenance > Project Status
                        return when {
                            // Check if project is suspended (has isSuspended flag or suspendedDate)
                            project.isSuspended == true || !project.suspendedDate.isNullOrBlank() -> "Suspended"
                            // Check if project is in maintenance (has maintenanceDate)
                            !project.maintenanceDate.isNullOrBlank() -> "Maintenance"
                            // Otherwise use the project status field
                            project.status.equals("ACTIVE", ignoreCase = true) -> "Active"
                            project.status.equals("COMPLETED", ignoreCase = true) -> "Completed"
                            project.status.equals("ARCHIVE", ignoreCase = true) -> "Archive"
                            project.status.equals("SUSPENDED", ignoreCase = true) -> "Suspended"
                            else -> project.status.replaceFirstChar { it.uppercaseChar() } // Capitalize other statuses
                        }
                    }
                    
                    // Get all unique statuses from projects dynamically
                    val allStatuses = projects.map { getProjectDisplayStatus(it) }.distinct().sorted()
                    
                    // Calculate spent for each status (including 0 if no expenses)
                    allStatuses.forEach { displayStatus ->
                        // Get projects with this status
                        val projectsWithStatus = projects.filter { project ->
                            getProjectDisplayStatus(project) == displayStatus
                        }
                        
                        // Calculate total spent from approved expenses for these projects
                        val totalSpent = projectsWithStatus.sumOf { project ->
                            project.id?.let { projectId ->
                                allExpenses
                                    .filter { expense ->
                                        val matchesProject = expense.projectId == projectId
                                        if (!matchesProject) return@filter false
                                        
                                        val matchesStatus = expense.status == ExpenseStatus.APPROVED
                                        if (!matchesStatus) return@filter false
                                        
                                        // Filter by date range
                                        val matchesDateRange = if (startDate != null && endDate != null) {
                                            try {
                                                val expenseDate = expense.getDateAsTimestamp()?.toDate() ?: expense.submittedAt?.toDate()
                                                if (expenseDate != null) {
                                                    val startCal = Calendar.getInstance().apply {
                                                        time = startDate
                                                        set(Calendar.HOUR_OF_DAY, 0)
                                                        set(Calendar.MINUTE, 0)
                                                        set(Calendar.SECOND, 0)
                                                        set(Calendar.MILLISECOND, 0)
                                                    }
                                                    val endCal = Calendar.getInstance().apply {
                                                        time = endDate
                                                        set(Calendar.HOUR_OF_DAY, 23)
                                                        set(Calendar.MINUTE, 59)
                                                        set(Calendar.SECOND, 59)
                                                        set(Calendar.MILLISECOND, 999)
                                                    }
                                                    val expenseCal = Calendar.getInstance().apply { time = expenseDate }
                                                    !expenseCal.before(startCal) && !expenseCal.after(endCal)
                                                } else {
                                                    false // Exclude expenses without dates for date-filtered queries
                                                }
                                            } catch (e: Exception) {
                                                false
                                            }
                                        } else {
                                            true
                                        }
                                        
                                        matchesDateRange
                                    }
                                    .sumOf { it.amount }
                            } ?: 0.0
                        } // Keep in raw rupees - formatNumber will handle formatting
                        
                        // Always add status, even if totalSpent is 0
                        statusMap[displayStatus] = totalSpent
                    }
                    
                    // Sort by status name for consistent display
                    statusMap.toList().sortedBy { it.first }
                }
                
                if (statusCostData.isNotEmpty()) {
                    VerticalBarChartWithTooltips(
                        labels = statusCostData.map { it.first },
                        values = statusCostData.map { it.second },
                        modifier = Modifier.height(160.dp)
                    )
                } else {
                    Text(
                        text = "Select project statuses to view cost breakdown.",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Cost Overrun vs Stage Progress - Calculate dynamically from phases and expenses
        val scatterData = remember(projects, allPhases, allExpenses, selectedStage, selectedDepartment, startDate, endDate) {
            val points = mutableListOf<ScatterPoint>()
            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val currentDate = Calendar.getInstance()
            
            projects.forEach { project ->
                project.id?.let { projectId ->
                    val phases = allPhases[projectId] ?: emptyList()
                    
                    phases.forEach { phase ->
                        // Skip if stage filter is applied and this phase doesn't match
                        if (selectedStage.isNotEmpty()) {
                            val matchesStage = selectedStage.any { stageName ->
                                phase.phaseName == stageName
                            }
                            if (!matchesStage) return@forEach
                        }
                        
                        // Calculate progress % based on phase startDate and endDate
                        var progressPercent = 0f
                        if (!phase.startDate.isNullOrBlank() && !phase.endDate.isNullOrBlank()) {
                            try {
                                val startDate = dateFormatter.parse(phase.startDate)
                                val endDate = dateFormatter.parse(phase.endDate)
                                
                                if (startDate != null && endDate != null) {
                                    val startCal = Calendar.getInstance().apply { time = startDate }
                                    val endCal = Calendar.getInstance().apply { time = endDate }
                                    val currentCal = Calendar.getInstance()
                                    
                                    val totalDuration = endCal.timeInMillis - startCal.timeInMillis
                                    if (totalDuration > 0) {
                                        val elapsed = currentCal.timeInMillis - startCal.timeInMillis
                                        progressPercent = ((elapsed.toFloat() / totalDuration.toFloat()) * 100f).coerceIn(0f, 100f)
                                    }
                                }
                            } catch (e: Exception) {
                                // If date parsing fails, skip this phase
                                return@forEach
                            }
                        } else {
                            // No dates, skip this phase
                            return@forEach
                        }
                        
                        // Calculate phase budget (sum of all departments or specific department)
                        val phaseBudget = if (selectedDepartment == "all") {
                            phase.departments.values.sum()
                        } else {
                            phase.departments[selectedDepartment] ?: 0.0
                        }
                        
                        if (phaseBudget <= 0) {
                            // No budget, skip this phase
                            return@forEach
                        }
                        
                        // Get approved expenses for this phase
                        val phaseExpenses = allExpenses.filter { expense ->
                            val matchesProject = expense.projectId == projectId
                            if (!matchesProject) return@filter false
                            
                            val matchesPhase = expense.phaseId == phase.id
                            if (!matchesPhase) return@filter false
                            
                            val matchesStatus = expense.status == ExpenseStatus.APPROVED
                            if (!matchesStatus) return@filter false
                            
                            // Filter by department if selected
                            val matchesDepartment = if (selectedDepartment == "all") {
                                true
                            } else {
                                val expenseDept = expense.department?.trim() ?: ""
                                val selectedDept = selectedDepartment.trim()
                                expenseDept.equals(selectedDept, ignoreCase = true)
                            }
                            if (!matchesDepartment) return@filter false
                            
                            // Filter by date range if provided
                            val matchesDateRange = if (startDate != null && endDate != null) {
                                val expenseDate = expense.submittedAt?.toDate() ?: expense.getDateAsTimestamp()?.toDate()
                                if (expenseDate != null) {
                                    val expenseCal = Calendar.getInstance().apply { time = expenseDate }
                                    val startCal = Calendar.getInstance().apply {
                                        time = startDate
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    val endCal = Calendar.getInstance().apply {
                                        time = endDate
                                        set(Calendar.HOUR_OF_DAY, 23)
                                        set(Calendar.MINUTE, 59)
                                        set(Calendar.SECOND, 59)
                                        set(Calendar.MILLISECOND, 999)
                                    }
                                    !expenseCal.before(startCal) && !expenseCal.after(endCal)
                                } else {
                                    false
                                }
                            } else {
                                true
                            }
                            
                            matchesDateRange
                        }
                        
                        val totalSpent = phaseExpenses.sumOf { it.amount }
                        
                        // Calculate cost overrun %
                        var overrunPercent = 0f
                        var shouldShow = false
                        
                        // Condition 1: Spent exceeds budget (positive overrun)
                        if (totalSpent > phaseBudget) {
                            overrunPercent = ((totalSpent - phaseBudget) / phaseBudget * 100f).toFloat()
                            shouldShow = true
                        }
                        // Condition 2: Phase has progressed > 25% and no approved expenses (negative overrun)
                        else if (phaseExpenses.isEmpty() && progressPercent > 25f) {
                            // Show negative overrun based on how much time has passed beyond 25%
                            // Negative overrun = -((progress - 25%) / 25% * 100)
                            // This represents the percentage of the "expected expense period" that has passed without expenses
                            val progressBeyondQuarter = progressPercent - 25f
                            // Calculate negative overrun as percentage of the remaining 75% of timeline
                            // If 50% progress with no expenses, that's 25% beyond quarter = -33% overrun (25/75 * 100)
                            overrunPercent = -(progressBeyondQuarter / 75f * 100f)
                            shouldShow = true
                        }
                        
                        // Only add point if condition is met
                        if (shouldShow) {
                            points.add(ScatterPoint(progressPercent, overrunPercent, phase.phaseName))
                        }
                    }
                }
            }
            
            points
        }
        
        if (scatterData.isNotEmpty()) {
        ChartCard(
            title = "Cost Overrun vs Stage Progress",
            subtitle = "Variance % vs Progress %"
        ) {
            ScatterChart(
                    data = scatterData,
                modifier = Modifier.height(160.dp)
            )
            }
        }
        
        // Burn Rate by Project - Calculate from last 30 days of approved expenses
        val burnRateData = remember(projects, allExpenses, selectedStage, selectedDepartment, allPhases) {
            val thirtyDaysAgo = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -30)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time
            
            val projectBurnRates = mutableListOf<Pair<String, Double>>()
            
            projects.forEach { project ->
                project.id?.let { projectId ->
                    // Get approved expenses for this project in last 30 days
                    val projectExpenses = allExpenses.filter { expense ->
                        val matchesProject = expense.projectId == projectId
                        if (!matchesProject) return@filter false
                        
                        val matchesStatus = expense.status == ExpenseStatus.APPROVED
                        if (!matchesStatus) return@filter false
                        
                        // Filter by date range (last 30 days)
                        val expenseDate = expense.submittedAt?.toDate() ?: expense.getDateAsTimestamp()?.toDate()
                        if (expenseDate != null) {
                            val expenseCal = Calendar.getInstance().apply { time = expenseDate }
                            !expenseCal.before(thirtyDaysAgo) && !expenseCal.after(today)
                        } else {
                            false
                        }
                    }
                    
                    // Filter by stage if selected
                    val filteredExpenses = if (selectedStage.isNotEmpty()) {
                        val phases = allPhases[projectId] ?: emptyList()
                        projectExpenses.filter { expense ->
                            val expensePhaseId = expense.phaseId
                            if (expensePhaseId.isNullOrBlank()) {
                                true
                            } else {
                                selectedStage.any { stageName ->
                                    phases.any { it.phaseName == stageName && it.id == expensePhaseId }
                                }
                            }
                        }
                    } else {
                        projectExpenses
                    }
                    
                    // Filter by department if selected
                    val finalExpenses = if (selectedDepartment != "all") {
                        filteredExpenses.filter { expense ->
                            val expenseDept = expense.department?.trim() ?: ""
                            val selectedDept = selectedDepartment.trim()
                            expenseDept.equals(selectedDept, ignoreCase = true)
                        }
                    } else {
                        filteredExpenses
                    }
                    
                    // Calculate total spent in last 30 days
                    val totalSpent = finalExpenses.sumOf { it.amount }
                    
                    // Calculate burn rate (Cr/day) = total spent / 30 days / 10000000
                    if (totalSpent > 0) {
                        val burnRateCrPerDay = totalSpent / 30.0 / 10000000.0
                        projectBurnRates.add(Pair(project.name, burnRateCrPerDay))
                    }
                }
            }
            
            // Sort by burn rate (descending - highest first)
            projectBurnRates.sortedByDescending { it.second }
        }
        
        if (burnRateData.isNotEmpty()) {
        ChartCard(
            title = "Burn Rate by Project",
            subtitle = "₹ Cr/day · last 30 days"
        ) {
                // Calculate height based on number of projects
                val chartHeight = maxOf(160.dp, (burnRateData.size * 50).dp)
                HorizontalBarChartWithScroll(
                    labels = burnRateData.map { it.first },
                    values = burnRateData.map { it.second },
                    modifier = Modifier.height(chartHeight)
                )
            }
        }
    }
}

@Composable
fun ProjectInsightsContent(
    selectedProject: String = "all",
    selectedStage: Set<String> = emptySet(),
    selectedDepartment: String = "all",
    selectedProjectStatus: Set<String> = emptySet(),
    projects: List<Project> = emptyList(),
    allPhases: Map<String, List<Phase>> = emptyMap(),
    allExpenses: List<Expense> = emptyList(),
    startDate: Date? = null,
    endDate: Date? = null,
    onNavigateToProject: ((String) -> Unit)? = null
) {
    // State for category detail bottom sheet
    var showCategoryDetailSheet by remember { mutableStateOf(false) }
    var selectedCategoryName by remember { mutableStateOf<String?>(null) }
    
    // State for suspension reason detail bottom sheet
    var showSuspensionReasonDetailSheet by remember { mutableStateOf(false) }
    var selectedSuspensionReason by remember { mutableStateOf<String?>(null) }
    // Calculate active projects month-over-month based on selected dates and filters
    val activeProjectsData = remember(projects, selectedProjectStatus, selectedStage, selectedDepartment, startDate, endDate, allPhases, allExpenses) {
        // Get date range for calculation - use provided dates or default to last 6 months
        val calcStartDate = startDate ?: Calendar.getInstance().apply {
            add(Calendar.MONTH, -5) // Default to last 6 months
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val calcEndDate = endDate ?: Date()
        
        // Generate list of months in range
        val calendar = Calendar.getInstance()
        calendar.time = calcStartDate
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val monthsList = mutableListOf<Pair<String, Calendar>>()
        val monthFormatter = SimpleDateFormat("MMM", Locale.getDefault())
        val endCalendar = Calendar.getInstance().apply { time = calcEndDate }
        
        while (!calendar.after(endCalendar)) {
            val monthName = monthFormatter.format(calendar.time)
            monthsList.add(Pair(monthName, calendar.clone() as Calendar))
            calendar.add(Calendar.MONTH, 1)
        }
        
        // Calculate active projects count for each month
        val activeCounts = monthsList.map { (monthName, monthStart) ->
            val monthEnd = (monthStart.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
                add(Calendar.DAY_OF_MONTH, -1)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            
            // Count active projects for this month
            // A project is active if:
            // 1. status == "ACTIVE" (not suspended, not in maintenance)
            // 2. Current month is between startDate (plannedDate) and handoverDate
            val activeCount = projects.count { project ->
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                
                // Check 1: status must be "ACTIVE"
                // Project is ACTIVE if:
                // - status field equals "ACTIVE" (case insensitive)
                // - NOT suspended (isSuspended != true and no suspendedDate before/during this month)
                // - NOT in maintenance (no maintenanceDate before/during this month)
                val isStatusActive = project.status.equals("ACTIVE", ignoreCase = true)
                if (!isStatusActive) return@count false
                
                // Check if suspended during this month
                val isSuspended = project.isSuspended == true || (!project.suspendedDate.isNullOrBlank() && try {
                    val suspendedDate = dateFormatter.parse(project.suspendedDate)
                    if (suspendedDate != null) {
                        val suspensionCal = Calendar.getInstance().apply { time = suspendedDate }
                        !suspensionCal.after(monthEnd) // Suspended before or during this month
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                })
                if (isSuspended) return@count false
                
                // Check if in maintenance during this month
                val isInMaintenance = !project.maintenanceDate.isNullOrBlank() && try {
                    val maintenanceDate = dateFormatter.parse(project.maintenanceDate)
                    if (maintenanceDate != null) {
                        val maintenanceCal = Calendar.getInstance().apply { time = maintenanceDate }
                        !maintenanceCal.after(monthEnd) // Maintenance started before or during this month
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
                if (isInMaintenance) return@count false
                
                // Check 2: Current month must be between startDate (plannedDate) and handoverDate
                // Project must have started (startDate is before or during this month)
                val hasStarted = if (!project.startDate.isNullOrBlank()) {
                    try {
                        val startDate = dateFormatter.parse(project.startDate)
                        if (startDate != null) {
                            val startCal = Calendar.getInstance().apply { time = startDate }
                            !startCal.after(monthEnd) // Started before or during this month
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false // No start date, consider inactive
                }
                if (!hasStarted) return@count false
                
                // Project must not have been handed over yet (handoverDate is after this month or not set)
                // Project is active if handoverDate is not set OR handoverDate is after the end of this month
                val isHandedOver = if (!project.handoverDate.isNullOrBlank()) {
                    try {
                        val handoverDate = dateFormatter.parse(project.handoverDate)
                        if (handoverDate != null) {
                            val handoverCal = Calendar.getInstance().apply { time = handoverDate }
                            !handoverCal.after(monthEnd) // Handed over before or during this month
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false // No handover date, project is still active
                }
                if (isHandedOver) return@count false
                
                // Filter by selected project status if not "ALL Status"
                val matchesStatus = if (!selectedProjectStatus.contains("ALL Status") && selectedProjectStatus.isNotEmpty()) {
                    selectedProjectStatus.contains("ACTIVE")
                } else {
                    true
                }
                if (!matchesStatus) return@count false
                
                // Filter by stage if selected
                val matchesStage = if (selectedStage.isNotEmpty()) {
                    project.id?.let { projectId ->
                        val phases = allPhases[projectId] ?: emptyList()
                        selectedStage.any { stageName ->
                            phases.any { it.phaseName == stageName }
                        }
                    } ?: false
                } else {
                    true
                }
                if (!matchesStage) return@count false
                
                // Filter by department if selected (check if project has expenses in this department)
                val matchesDepartment = if (selectedDepartment != "all") {
                    project.id?.let { projectId ->
                        allExpenses.any { expense ->
                            expense.projectId == projectId &&
                            expense.department?.trim()?.equals(selectedDepartment.trim(), ignoreCase = true) == true
                        }
                    } ?: false
                } else {
                    true
                }
                if (!matchesDepartment) return@count false
                
                true
            }
            
            Pair(monthName, activeCount)
        }
        
        activeCounts
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Active Projects (MoM)
        if (activeProjectsData.isNotEmpty()) {
            ChartCard(
                title = "Active Projects (MoM)",
                subtitle = "Count of active projects"
            ) {
                LineChartWithGradient(
                    data = activeProjectsData.map { it.second.toDouble() },
                    labels = activeProjectsData.map { it.first },
                    modifier = Modifier.height(130.dp)
                )
            }
        }
        
        // Projects Percentage by Status - Calculate from all projects
        // Only show: ACTIVE, COMPLETED, MAINTENANCE, ARCHIVE, SUSPENDED
        val projectsPercentageByStatus = remember(projects) {
            // Helper function to determine the actual display status of a project
            fun getProjectDisplayStatus(project: Project): String? {
                // Priority: Suspended > Maintenance > Project Status
                // Only return valid statuses: Active, Completed, Maintenance, Archive, Suspended
                return when {
                    project.isSuspended == true || !project.suspendedDate.isNullOrBlank() -> "Suspended"
                    !project.maintenanceDate.isNullOrBlank() -> "Maintenance"
                    project.status.equals("ACTIVE", ignoreCase = true) -> "Active"
                    project.status.equals("COMPLETED", ignoreCase = true) -> "Completed"
                    project.status.equals("ARCHIVE", ignoreCase = true) -> "Archive"
                    project.status.equals("SUSPENDED", ignoreCase = true) -> "Suspended"
                    else -> null // Filter out other statuses
                }
            }
            
            // Use all projects to calculate percentages (across all projects)
            if (projects.isEmpty()) {
                emptyList()
            } else {
                // Count projects by status (only valid statuses)
                val statusCounts = mutableMapOf<String, Int>()
                projects.forEach { project ->
                    val displayStatus = getProjectDisplayStatus(project)
                    if (displayStatus != null) {
                        statusCounts[displayStatus] = (statusCounts[displayStatus] ?: 0) + 1
                    }
                }
                
                // Calculate percentages and keep counts, then sort by percentage descending
                val totalProjects = projects.size
                statusCounts.map { (status, count) ->
                    val percentage = (count.toDouble() / totalProjects.toDouble()) * 100.0
                    Triple(status, percentage, count) // Include count for tooltip
                }.sortedByDescending { it.second } // Sort by percentage descending (highest first)
            }
        }
        
        if (projectsPercentageByStatus.isNotEmpty()) {
            ChartCard(
                title = "Projects Percentage by Status",
                subtitle = "% of projects by status"
            ) {
                // Calculate height based on number of statuses
                val chartHeight = maxOf(150.dp, (projectsPercentageByStatus.size * 40).dp)
                HorizontalBarChartForPercentage(
                    labels = projectsPercentageByStatus.map { it.first },
                    values = projectsPercentageByStatus.map { it.second },
                    projectCounts = projectsPercentageByStatus.map { it.third }, // Pass counts
                    modifier = Modifier.height(chartHeight)
                    // No onStatusClick - tooltip only, no bottom sheet
                )
            }
        }
        
        // Sub-Category Activity - Count expenses by category (last 30 days)
        val subCategoryActivityData = remember(projects, allExpenses, selectedStage, selectedDepartment, selectedProjectStatus, startDate, endDate, allPhases) {
            // Calculate date range for last 30 days
            val calendar = Calendar.getInstance()
            val today = calendar.time
            calendar.add(Calendar.DAY_OF_YEAR, -30)
            val thirtyDaysAgo = calendar.time
            
            // Use provided date range if available, otherwise use last 30 days
            val filterStartDate = startDate ?: thirtyDaysAgo
            val filterEndDate = endDate ?: today
            
            // Filter expenses based on all criteria
            val filteredExpenses = allExpenses.filter { expense ->
                // Match project
                val matchesProject = projects.any { it.id == expense.projectId }
                if (!matchesProject) return@filter false
                
                // Match status - only count APPROVED expenses
                val matchesStatus = expense.status == ExpenseStatus.APPROVED
                if (!matchesStatus) return@filter false
                
                // Filter by date range (last 30 days or selected date range)
                val matchesDateRange = try {
                    val expenseDate = expense.submittedAt?.toDate() ?: expense.getDateAsTimestamp()?.toDate()
                    if (expenseDate != null) {
                        val expenseCal = Calendar.getInstance().apply { time = expenseDate }
                        val startCal = Calendar.getInstance().apply {
                            time = filterStartDate
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val endCal = Calendar.getInstance().apply {
                            time = filterEndDate
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        !expenseCal.before(startCal) && !expenseCal.after(endCal)
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
                if (!matchesDateRange) return@filter false
                
                // Filter by stage
                val matchesStage = if (selectedStage.isEmpty()) {
                    true
                } else {
                    val projectId = expense.projectId
                    val phases = allPhases[projectId] ?: emptyList()
                    if (phases.isEmpty()) {
                        true
                    } else {
                        val expensePhaseId = expense.phaseId
                        if (expensePhaseId.isNullOrBlank()) {
                            true
                        } else {
                            selectedStage.any { stageName ->
                                phases.any { it.phaseName == stageName && it.id == expensePhaseId }
                            }
                        }
                    }
                }
                if (!matchesStage) return@filter false
                
                // Filter by department
                val matchesDepartment = if (selectedDepartment == "all") {
                    true
                } else {
                    val expenseDept = expense.department?.trim() ?: ""
                    val selectedDept = selectedDepartment.trim()
                    expenseDept.equals(selectedDept, ignoreCase = true)
                }
                if (!matchesDepartment) return@filter false
                
                // Must have a category
                val hasCategory = expense.category.isNotEmpty()
                if (!hasCategory) return@filter false
                
                true
            }
            
            // Group by category and count expenses (not sum amounts)
            val categoryCounts = mutableMapOf<String, Int>()
            filteredExpenses.forEach { expense ->
                val category = expense.category.trim().takeIf { it.isNotEmpty() } ?: "Uncategorized"
                categoryCounts[category] = (categoryCounts[category] ?: 0) + 1
            }
            
            // Sort by count in descending order (maximum first)
            categoryCounts.toList()
                .sortedByDescending { it.second }
                .map { Pair(it.first, it.second.toDouble()) } // Convert to Double for chart
        }
        
        if (subCategoryActivityData.isNotEmpty()) {
            ChartCard(
                title = "Sub-Category Activity",
                subtitle = "# of expenses · last 30 days"
            ) {
                // Calculate height based on number of categories
                val chartHeight = maxOf(150.dp, (subCategoryActivityData.size * 40).dp)
                HorizontalBarChartForCount(
                    labels = subCategoryActivityData.map { it.first },
                    values = subCategoryActivityData.map { it.second },
                    modifier = Modifier.height(chartHeight),
                    onCategoryClick = { categoryName ->
                        selectedCategoryName = categoryName
                        showCategoryDetailSheet = true
                    }
                )
            }
        }
        
        // Category Detail Bottom Sheet
        if (showCategoryDetailSheet && selectedCategoryName != null) {
            CategoryDetailBottomSheet(
                categoryName = selectedCategoryName!!,
                projects = projects,
                allExpenses = allExpenses,
                selectedStage = selectedStage,
                selectedDepartment = selectedDepartment,
                selectedProjectStatus = selectedProjectStatus,
                startDate = startDate,
                endDate = endDate,
                allPhases = allPhases,
                onDismiss = {
                    showCategoryDetailSheet = false
                    selectedCategoryName = null
                },
                onProjectClick = { projectId ->
                    // Handle project click if needed
                    // You can navigate to project details here
                }
            )
        }
        
        // Delay Days vs Extra Cost
        ChartCard(
            title = "Delay Days vs Extra Cost",
            subtitle = "Project-level correlation"
        ) {
            ScatterChart(
                data = listOf(
                    ScatterPoint(18f, 1.8f, "Aurum Heights"),
                    ScatterPoint(24f, 2.2f, "Tracura Residency"),
                    ScatterPoint(10f, 0.7f, "Lotus Enclave")
                ),
                modifier = Modifier.height(160.dp)
            )
        }
        
        // Suspended Projects by Reason - Calculate from projects
        val suspensionReasonData = remember(projects) {
            val reasonCountMap = mutableMapOf<String, Int>()
            
            // Count suspension reasons from all suspended projects
            projects.forEach { project ->
                if (project.isSuspended == true && !project.suspensionReason.isNullOrBlank()) {
                    val reason = project.suspensionReason.trim()
                    reasonCountMap[reason] = reasonCountMap.getOrDefault(reason, 0) + 1
                }
            }
            
            // Convert to list of pairs and sort by count in descending order
            reasonCountMap.map { (reason, count) -> Pair(reason, count.toDouble()) }
                .sortedByDescending { it.second }
        }
        
        if (suspensionReasonData.isNotEmpty()) {
            ChartCard(
                title = "Suspended Projects by Reason",
                subtitle = "Current FY"
            ) {
                // Calculate height based on number of reasons
                val chartHeight = maxOf(160.dp, (suspensionReasonData.size * 40).dp)
                HorizontalBarChartForSuspensionReasons(
                    labels = suspensionReasonData.map { it.first },
                    values = suspensionReasonData.map { it.second },
                    modifier = Modifier.height(chartHeight),
                    onReasonClick = { reasonName ->
                        selectedSuspensionReason = reasonName
                        showSuspensionReasonDetailSheet = true
                    }
                )
            }
        }
        
        // Suspension Reason Detail Bottom Sheet
        if (showSuspensionReasonDetailSheet && selectedSuspensionReason != null) {
            SuspensionReasonDetailBottomSheet(
                reasonName = selectedSuspensionReason!!,
                projects = projects,
                onDismiss = {
                    showSuspensionReasonDetailSheet = false
                    selectedSuspensionReason = null
                },
                onProjectClick = { projectId ->
                    // Close bottom sheet and navigate to project
                    showSuspensionReasonDetailSheet = false
                    selectedSuspensionReason = null
                    onNavigateToProject?.invoke(projectId)
                }
            )
        }
    }
}

@Composable
fun CostTrendChartCard(
    title: String,
    subtitle: String,
    totalValue: String,
    months: List<String>,
    costValues: List<Double>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, Color(0xFF94A3B8).copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                // Blue pill-shaped total value with expand icon
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .background(Color(0xFF2563EB), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "₹$totalValue",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Expand",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF6B7280)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (months.isEmpty() || costValues.isEmpty()) {
                // Show empty state with a flat line at 0
                CostTrendLineChart(
                    data = listOf(0.0),
                    labels = listOf("No Data"),
                    modifier = Modifier.height(160.dp)
                )
            } else {
                CostTrendLineChart(
                    data = costValues,
                    labels = months,
                    modifier = Modifier.height(160.dp)
                )
            }
        }
    }
}

@Composable
fun ChartCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    totalValue: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), // Match shadow from reference
        border = BorderStroke(1.dp, Color(0xFF94A3B8).copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp) // Match reference: 12px 14px
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (totalValue != null) {
                    Text(
                        text = totalValue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
fun CostTrendLineChart(
    data: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    // Calculate minimum width based on data points (responsive)
    val minWidthPerPoint = 80.dp
    val calculatedMinWidth = (data.size.toFloat() * minWidthPerPoint.value).dp.coerceAtLeast(300.dp)
    val scrollState = rememberScrollState()
    
    // State for selected point and tooltip
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipPosition by remember { mutableStateOf<Offset?>(null) }
    
    // Determine the format unit based on max value
    val maxValue = data.maxOrNull() ?: 0.0
    
    // Debug logging
    android.util.Log.d("CostTrendChart", "📈 Chart Data:")
    android.util.Log.d("CostTrendChart", "   Data values: $data")
    android.util.Log.d("CostTrendChart", "   Labels: $labels")
    val formattedMax = formatNumber(maxValue)
    android.util.Log.d("CostTrendChart", "   Max value: ₹$maxValue ($formattedMax)")
    
    val formatUnit = when {
        maxValue >= 10000000 -> "Cr" // Crores
        maxValue >= 100000 -> "lakhs" // Lakhs
        maxValue >= 1000 -> "k" // Thousands
        else -> "" // No unit for values < 1000
    }
    
    // Calculate divisor for formatting
    val divisor = when {
        maxValue >= 10000000 -> 10000000.0
        maxValue >= 100000 -> 100000.0
        maxValue >= 1000 -> 1000.0
        else -> 1.0
    }
    
    // Convert data to display units
    val displayData = data.map { it / divisor }
    val rawDisplayMax = if (maxValue == 0.0) 1.0 else (maxValue / divisor)
    
    // Calculate Y-axis max based on actual data - round up conservatively to show accurate representation
    // For example: if max is 2.01 lakhs, show up to 2.5 or 3 lakhs (not 10 or 20)
    val displayMax = if (rawDisplayMax > 0) {
        // Add small padding (10-15%) to ensure data is visible, but keep it minimal for accuracy
        val paddedMax = rawDisplayMax * 1.15
        
        // Determine appropriate step size based on the actual max value
        // Use smaller steps for smaller values to show accurate representation
        val stepSize = when {
            rawDisplayMax < 0.01 -> 0.002  // For very small values (e.g., 0.005 lakhs = 500 rupees)
            rawDisplayMax < 0.05 -> 0.01   // For small values (e.g., 0.03 lakhs = 3k)
            rawDisplayMax < 0.1 -> 0.02    // For values like 0.05 lakhs = 5k
            rawDisplayMax < 0.5 -> 0.1     // For small values (e.g., 0.3 lakhs = 30k)
            rawDisplayMax < 1 -> 0.2       // For values less than 1 lakh
            rawDisplayMax < 2 -> 0.4       // For values 1-2 lakhs (e.g., 1.5 lakhs)
            rawDisplayMax < 3 -> 0.5       // For values 2-3 lakhs (e.g., 2.01 lakhs -> show up to 2.5 or 3)
            rawDisplayMax < 5 -> 1.0       // For values 3-5 lakhs
            rawDisplayMax < 10 -> 2.0      // For values 5-10 lakhs
            rawDisplayMax < 20 -> 4.0      // For values 10-20 lakhs
            rawDisplayMax < 50 -> 10.0     // For values 20-50 lakhs
            rawDisplayMax < 100 -> 20.0    // For values 50-100 lakhs
            else -> {
                // For larger values, calculate step as 1/5 of rounded magnitude
                val logValue = floor(log10(rawDisplayMax)).toInt()
                var magnitude = 1.0
                repeat(logValue) {
                    magnitude *= 10.0
                }
                magnitude * 2.0
            }
        }
        
        // Calculate max that gives us 5 intervals, but don't round up too much
        val numStepsNeeded = ceil(paddedMax / stepSize)
        val intervalsNeeded = ceil(numStepsNeeded / 5.0).toInt()
        val calculatedMax = intervalsNeeded * 5 * stepSize
        
        // Ensure we don't round up more than 30% of the original value for accuracy
        if (calculatedMax > rawDisplayMax * 1.3) {
            // Use a more conservative rounding - just round up to next step that gives us at least 3 intervals
            val conservativeSteps = ceil(paddedMax / stepSize).toInt()
            val conservativeIntervals = maxOf(3, ((conservativeSteps + 2) / 3) * 3) // At least 3 intervals
            conservativeIntervals * stepSize
        } else {
            calculatedMax
        }
    } else {
        // Default for zero/empty data
        when (formatUnit) {
            "k" -> 5.0
            "lakhs" -> 1.0
            "Cr" -> 0.5
            else -> 5.0
        }
    }
    
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Box(modifier = Modifier.widthIn(min = calculatedMinWidth)) {
                // Store points and chart dimensions for click detection
                var chartSize by remember { mutableStateOf(Size.Zero) }
                var storedPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
                val density = LocalDensity.current
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                // Find the closest point to the tap
                                val touchRadius = 30.dp.toPx() // Radius for touch detection
                                var closestIndex: Int? = null
                                var minDistance = Float.MAX_VALUE
                                
                                storedPoints.forEachIndexed { index, point ->
                                    val distance = kotlin.math.sqrt(
                                        (tapOffset.x - point.x) * (tapOffset.x - point.x) +
                                        (tapOffset.y - point.y) * (tapOffset.y - point.y)
                                    )
                                    if (distance < touchRadius && distance < minDistance) {
                                        minDistance = distance
                                        closestIndex = index
                                    }
                                }
                                
                                if (closestIndex != null) {
                                    selectedPointIndex = closestIndex
                                    // Position tooltip above the point
                                    val point = storedPoints[closestIndex]
                                    tooltipPosition = Offset(point.x, point.y - 40.dp.toPx())
                                } else {
                                    // Click outside any point - dismiss tooltip
                                    selectedPointIndex = null
                                    tooltipPosition = null
                                }
                            }
                        }
                ) {
                    if (displayData.isEmpty()) return@Canvas
                    
                    // Padding: left for Y-axis labels, right/top/bottom for chart edges
                    val leftPadding = 40.dp.toPx()
                    val rightPadding = 20.dp.toPx()
                    val topPadding = 20.dp.toPx()
                    val bottomPadding = 30.dp.toPx()
                    
                    val calculatedMinWidthPx = calculatedMinWidth.toPx()
                    val chartWidth = maxOf(size.width - leftPadding - rightPadding, calculatedMinWidthPx - leftPadding - rightPadding)
                    val chartHeight = size.height - topPadding - bottomPadding
                    val minValue = 0.0
                    val valueRange = if (displayMax > minValue) displayMax - minValue else 1.0
                    
                    // Store size for click detection
                    chartSize = size
                    
                    // Draw grid lines - 6 lines for 5 intervals (0, 1, 2, 3, 4, 5)
                    val gridColor = Color(0xFF94A3B8).copy(alpha = 0.25f)
                    for (i in 0..5) {
                        val y = topPadding + (chartHeight / 5) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(leftPadding, y),
                            end = Offset(leftPadding + chartWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    
                    // Calculate all data points
                    val points = mutableListOf<Offset>()
                    displayData.forEachIndexed { index, value ->
                        val x = if (displayData.size == 1) {
                            leftPadding + chartWidth / 2
                        } else {
                            leftPadding + (chartWidth / (displayData.size - 1).coerceAtLeast(1)) * index
                        }
                            val y = if (value == 0.0 && displayMax == 0.0) {
                            topPadding + chartHeight
                            } else {
                            topPadding + chartHeight - ((value - minValue) / valueRange * chartHeight)
                            }
                        points.add(Offset(x, y.toFloat()))
                    }
                    
                    // Store points for click detection
                    storedPoints = points
                    
                    // Create smooth curve using cubic bezier
                    val smoothPath = Path()
                    if (points.isNotEmpty()) {
                        if (points.size == 1) {
                            smoothPath.moveTo(points[0].x, points[0].y)
                            } else {
                            smoothPath.moveTo(points[0].x, points[0].y)
                            
                            // Use cubic bezier curves for smooth interpolation
                            for (i in 0 until points.size - 1) {
                                val currentPoint = points[i]
                                val nextPoint = points[i + 1]
                                
                                // Calculate control points for smooth curve
                                val dx = nextPoint.x - currentPoint.x
                                val dy = nextPoint.y - currentPoint.y
                                
                                // Control point 1: extends from current point
                                val cp1x = currentPoint.x + dx * 0.5f
                                val cp1y = currentPoint.y
                                
                                // Control point 2: extends from next point
                                val cp2x = nextPoint.x - dx * 0.5f
                                val cp2y = nextPoint.y
                                
                                smoothPath.cubicTo(
                                    cp1x, cp1y,
                                    cp2x, cp2y,
                                    nextPoint.x, nextPoint.y
                                )
                            }
                        }
                    }
                    
                    // Draw gradient fill with smooth curve
                    val fillPath = Path().apply {
                        if (points.isNotEmpty()) {
                            // Start from first point
                            moveTo(points.first().x, points.first().y)
                            
                            // Add smooth curve
                            if (points.size == 1) {
                                // Single point - just a line
                            } else {
                                // Use the same smooth curve logic
                                for (i in 0 until points.size - 1) {
                                    val currentPoint = points[i]
                                    val nextPoint = points[i + 1]
                                    
                                    val dx = nextPoint.x - currentPoint.x
                                    
                                    val cp1x = currentPoint.x + dx * 0.5f
                                    val cp1y = currentPoint.y
                                    
                                    val cp2x = nextPoint.x - dx * 0.5f
                                    val cp2y = nextPoint.y
                                    
                                    cubicTo(
                                        cp1x, cp1y,
                                        cp2x, cp2y,
                                        nextPoint.x, nextPoint.y
                                    )
                            }
                        }
                            
                            // Close the path to bottom
                            lineTo(leftPadding + chartWidth, topPadding + chartHeight)
                            lineTo(leftPadding, topPadding + chartHeight)
                        close()
                        }
                    }
                    
                    // Draw gradient fill first (behind the line)
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2563EB).copy(alpha = 0.12f),
                                Color(0xFF2563EB).copy(alpha = 0.0f)
                            ),
                            startY = topPadding,
                            endY = topPadding + chartHeight
                        )
                    )
                    
                    // Draw smooth curve line
                    drawPath(
                        path = smoothPath,
                        color = Color(0xFF2563EB),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    
                    // Draw dot points on top of the line
                    points.forEachIndexed { index, point ->
                        val isSelected = selectedPointIndex == index
                        
                        if (isSelected) {
                            // Draw larger highlight circle for selected point
                            drawCircle(
                                color = Color(0xFF2563EB).copy(alpha = 0.2f),
                                radius = 12.dp.toPx(),
                                center = point
                            )
                        }
                        
                        // Draw white background circle for better visibility
                        drawCircle(
                            color = Color.White,
                            radius = if (isSelected) 5.dp.toPx() else 4.dp.toPx(),
                            center = point
                        )
                        // Draw blue circle
                        drawCircle(
                            color = Color(0xFF2563EB),
                            radius = if (isSelected) 4.dp.toPx() else 3.dp.toPx(),
                            center = point
                        )
                    }
                }
                
                // Display tooltip when a point is selected - overlay on top of chart
                selectedPointIndex?.let { index ->
                    tooltipPosition?.let { position ->
                        if (index < data.size && index < labels.size) {
                            val actualValue = data[index]
                            val monthLabel = labels[index]
                            val formattedValue = formatNumber(actualValue)
                            
                            // Determine unit label for tooltip
                            val unitLabel = when {
                                actualValue >= 10000000 -> "Cr"
                                actualValue >= 100000 -> "lakhs"
                                actualValue >= 1000 -> "k"
                                else -> ""
                            }
                            
                            // Overlay tooltip on top of chart
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.TopStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = with(density) { position.x.toDp() - 40.dp },
                                            y = with(density) { position.y.toDp() - 50.dp }
                                        )
                                        .background(
                                            color = Color(0xFF1F2937),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Cost ($unitLabel)",
                                            fontSize = 10.sp,
                                            color = Color(0xFF9CA3AF),
                                            fontWeight = FontWeight.Normal
                                        )
                                        Text(
                                            text = formattedValue,
                                            fontSize = 14.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Y-axis labels - 6 labels for 5 intervals, aligned with grid lines
                // Convert display values back to actual values and format using formatNumber function
                val uniqueValues = mutableSetOf<Double>()
                val labelPositions = mutableListOf<Pair<Int, Double>>()
                
                for (i in 5 downTo 0) {
                    val displayValue = (displayMax / 5) * i
                    // Convert back to actual value by multiplying by divisor
                    val actualValue = displayValue * divisor
                    // Round to avoid floating point precision issues
                    val roundedValue = (actualValue * 100).toInt() / 100.0
                    if (!uniqueValues.contains(roundedValue)) {
                        uniqueValues.add(roundedValue)
                        labelPositions.add(Pair(i, roundedValue))
                    }
                }
                
                // Display labels using Column with SpaceBetween to match grid line positions
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .padding(start = 0.dp, top = 20.dp, bottom = 30.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Create a map for quick lookup
                    val labelMap = labelPositions.associate { it.first to it.second }
                    
                    // Display labels at positions 5, 4, 3, 2, 1, 0
                    for (i in 5 downTo 0) {
                        val actualValue = labelMap[i]
                        if (actualValue != null) {
                            // Use formatNumber function to format according to rules:
                            // 1-999: actual numbers
                            // 1000-99999: X.XXk
                            // 100000-9999999: X.XX lakhs
                            // Above: Cr
                            val labelText = if (actualValue == 0.0) {
                                "0"
                            } else {
                                formatNumber(actualValue)
                        }
                        Text(
                            text = labelText,
                            fontSize = 10.sp,
                                color = Color(0xFF6B7280),
                                modifier = Modifier.padding(start = 0.dp)
                        )
                        } else {
                            // Empty space for duplicate positions to maintain alignment
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
                
                // X-axis labels - properly aligned with data points
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 40.dp, end = 20.dp, bottom = 4.dp),
                    horizontalArrangement = if (labels.size == 1) {
                        Arrangement.Center
                    } else {
                        Arrangement.SpaceBetween
                    }
                ) {
                    labels.forEachIndexed { index, label ->
                        val labelWidthValue = if (labels.size == 1) {
                            calculatedMinWidth.value - 60.dp.value // Account for padding
                        } else {
                            ((calculatedMinWidth.value - 60.dp.value) / labels.size.coerceAtLeast(1)).coerceAtLeast(40f)
                        }
                        val labelWidth = labelWidthValue.dp
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.width(labelWidth),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // Scrolling indicator arrow (right edge) - show when scrollable
                LaunchedEffect(scrollState.maxValue, scrollState.value) {
                    // This will recompose when scroll state changes
                }
                if (scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Scroll",
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .size(16.dp),
                        tint = Color(0xFF6B7280).copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun LineChartWithGradient(
    data: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    // Calculate minimum width based on data points (responsive)
    // Minimum 80dp per data point for better readability
    val minWidthPerPoint = 80.dp
    val calculatedMinWidth = (data.size.toFloat() * minWidthPerPoint.value).dp.coerceAtLeast(300.dp)
    val scrollState = rememberScrollState()
    
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Box(modifier = Modifier.widthIn(min = calculatedMinWidth)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (data.isEmpty()) return@Canvas
                    
                    val padding = 20.dp.toPx()
                    // Use calculated minimum width for chart width when scrolling
                    val calculatedMinWidthPx = calculatedMinWidth.toPx()
                    val chartWidth = maxOf(size.width - (padding * 2f), calculatedMinWidthPx - (padding * 2f))
                    val chartHeight = size.height - (padding * 2)
                    val maxValue = data.maxOrNull() ?: 0.0
                    val minValue = data.minOrNull() ?: 0.0
                    
                    // Calculate proper Y-axis max with clean intervals
                    // Round up to next "nice" number for better visualization
                    val displayMax = if (maxValue == 0.0) {
                        1.0 // Show 0-1 range when all values are 0
                    } else {
                        val roundedMax = ceil(maxValue).toInt()
                        // Round up to next nice number (1, 2, 5, 10, 20, 50, etc.)
                        when {
                            roundedMax <= 1 -> 1.0
                            roundedMax <= 2 -> 2.0
                            roundedMax <= 5 -> 5.0
                            roundedMax <= 10 -> 10.0
                            roundedMax <= 20 -> 20.0
                            roundedMax <= 50 -> 50.0
                            else -> {
                                // For larger values, round up to next multiple of 10
                                ((roundedMax + 9) / 10) * 10.0
                            }
                        }
                    }
                    val valueRange = if (displayMax > minValue) displayMax - minValue else 1.0 // Avoid division by zero
                    
                    // Draw grid lines
                    val gridColor = Color(0xFF94A3B8).copy(alpha = 0.25f)
                    for (i in 0..4) {
                        val y = padding + (chartHeight / 4) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(padding, y),
                            end = Offset(padding + chartWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    
                    // Draw line and points
                    val path = Path()
                    val points = mutableListOf<Offset>()
                    
                    if (data.size == 1) {
                        // Single data point - draw a point at the bottom if value is 0, or at calculated position
                        val x = padding + chartWidth / 2
                        val y = if (data[0] == 0.0) {
                            padding + chartHeight // Flat line at bottom
                        } else {
                            padding + chartHeight - ((data[0] - minValue) / valueRange * chartHeight)
                        }
                        val point = Offset(x, y.toFloat())
                        points.add(point)
                        path.moveTo(point.x, point.y)
                    } else {
                        // Multiple data points
                        data.forEachIndexed { index, value ->
                            val x = padding + (chartWidth / (data.size - 1).coerceAtLeast(1)) * index
                            val y = if (value == 0.0 && maxValue == 0.0) {
                                padding + chartHeight // Flat line at bottom when all values are 0
                            } else {
                                padding + chartHeight - ((value - minValue) / valueRange * chartHeight)
                            }
                            val point = Offset(x, y.toFloat())
                            points.add(point)
                            
                            if (index == 0) {
                                path.moveTo(point.x, point.y)
                            } else {
                                path.lineTo(point.x, point.y)
                            }
                        }
                    }
            
                    // Draw gradient fill
                    val fillPath = Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points.first().x, points.first().y)
                            points.drop(1).forEach { point ->
                                lineTo(point.x, point.y)
                            }
                        }
                        lineTo(padding + chartWidth, padding + chartHeight)
                        lineTo(padding, padding + chartHeight)
                        close()
                    }
                    
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2563EB).copy(alpha = 0.12f),
                                Color(0xFF2563EB).copy(alpha = 0.0f)
                            ),
                            startY = padding,
                            endY = padding + chartHeight
                        )
                    )
                    
                    // Draw line
                    drawPath(
                        path = path,
                        color = Color(0xFF2563EB),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    
                    // Draw points
                    points.forEach { point ->
                        drawCircle(
                            color = Color(0xFF2563EB),
                            radius = 3.dp.toPx(),
                            center = point
                        )
                    }
                }
                
                // Y-axis labels - show simple integer counts (0, 1, 2, 3, 4, 5, etc.) without duplicates
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .padding(start = 4.dp, top = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    val maxValue = data.maxOrNull() ?: 0.0
                    // Calculate displayMax (same as Canvas calculation)
                    val displayMax = if (maxValue == 0.0) {
                        1.0
                    } else {
                        val roundedMax = ceil(maxValue).toInt()
                        when {
                            roundedMax <= 1 -> 1.0
                            roundedMax <= 2 -> 2.0
                            roundedMax <= 5 -> 5.0
                            roundedMax <= 10 -> 10.0
                            roundedMax <= 20 -> 20.0
                            roundedMax <= 50 -> 50.0
                            else -> {
                                ((roundedMax + 9) / 10) * 10.0
                            }
                        }
                    }
                    
                    // Generate Y-axis labels with unique integer values
                    // Always show 0 at bottom and displayMax at top, with unique values in between
                    val maxInt = displayMax.toInt()
                    val uniqueValues = mutableSetOf<Int>()
                    val labelPositions = mutableListOf<Pair<Int, Int>>()
                    
                    // Always include 0 and max value
                    uniqueValues.add(0)
                    uniqueValues.add(maxInt)
                    labelPositions.add(Pair(0, 0))
                    labelPositions.add(Pair(4, maxInt))
                    
                    // Calculate step size for intermediate values
                    val stepSize = when {
                        maxInt <= 1 -> 1  // For max 1: just show 0, 1
                        maxInt <= 5 -> 1  // For max 5: show 0, 1, 2, 3, 4, 5
                        maxInt <= 10 -> 2  // For max 10: show 0, 2, 4, 6, 8, 10
                        maxInt <= 20 -> 4  // For max 20: show 0, 4, 8, 12, 16, 20
                        maxInt <= 50 -> 10  // For max 50: show 0, 10, 20, 30, 40, 50
                        else -> (maxInt / 5).coerceAtLeast(1)  // For larger values: divide by 5
                    }
                    
                    // Add intermediate values at positions 1, 2, 3
                    for (i in 1..3) {
                        val value = (stepSize * i).coerceAtMost(maxInt)
                        if (!uniqueValues.contains(value) && value <= maxInt) {
                            uniqueValues.add(value)
                            labelPositions.add(Pair(i, value))
                        }
                    }
                    
                    // Sort by position and create map
                    labelPositions.sortBy { it.first }
                    val labelMap = labelPositions.associate { it.first to it.second }
                    
                    // Display labels at positions 4, 3, 2, 1, 0 (top to bottom)
                    for (i in 4 downTo 0) {
                        val value = labelMap[i]
                        if (value != null) {
                            Text(
                                text = value.toString(), // Show as simple integer (0, 1, 2, 3, 4, 5, etc.)
                                fontSize = 10.sp,
                                color = Color(0xFF6B7280)
                            )
                        } else {
                            // Empty space for positions without labels to maintain alignment
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
                
                // X-axis labels (scrollable with proper spacing)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labels.forEach { label ->
                        val labelWidthValue = (calculatedMinWidth.value / labels.size.coerceAtLeast(1)).coerceAtLeast(40f)
                        val labelWidth = labelWidthValue.dp
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.width(labelWidth)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarChartWithTruncatedLabels(
    labels: List<String>,
    budgetData: List<Double>,
    actualData: List<Double>,
    modifier: Modifier = Modifier
) {
    var expandedLabelIndex by remember { mutableStateOf<Int?>(null) }
    val maxLabelLength = 8 // Maximum characters before truncation
    
    // Convert values to lakhs for display (1 lakh = 100,000 rupees)
    val budgetDataInLakhs = remember(budgetData) {
        budgetData.map { it / 100000.0 }
    }
    val actualDataInLakhs = remember(actualData) {
        actualData.map { it / 100000.0 }
    }
    
    // Calculate displayMax in lakhs - round up to next 10L increment, minimum 10L
    val rawMaxValueInLakhs = max(budgetDataInLakhs.maxOrNull() ?: 0.0, actualDataInLakhs.maxOrNull() ?: 0.0)
    val displayMaxInLakhs = remember(rawMaxValueInLakhs) {
        when {
            rawMaxValueInLakhs <= 0 -> 10.0
            rawMaxValueInLakhs <= 10 -> 10.0
            rawMaxValueInLakhs <= 20 -> 20.0
            rawMaxValueInLakhs <= 30 -> 30.0
            rawMaxValueInLakhs <= 40 -> 40.0
            rawMaxValueInLakhs <= 50 -> 50.0
            else -> ((rawMaxValueInLakhs / 10).toInt() + 1) * 10.0 // Round up to next 10L
        }
    }
    
    // Generate Y-axis labels dynamically based on max value (in 10L increments)
    val yAxisLabels = remember(displayMaxInLakhs) {
        val step = 10.0 // 10L intervals
        val labels = mutableListOf<String>()
        labels.add("0")
        var current = step
        while (current <= displayMaxInLakhs) {
            labels.add(String.format("%.2fL", current))
            current += step
        }
        labels
    }
    
    // Calculate tick values for grid lines (in 10L increments)
    val tickValues = remember(displayMaxInLakhs) {
        val step = 10.0
        val values = mutableListOf(0.0)
        var current = step
        while (current <= displayMaxInLakhs) {
            values.add(current)
            current += step
        }
        values
    }
    
    val displayMax = displayMaxInLakhs
    
    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (labels.isEmpty()) return@Canvas
            
            val padding = 20.dp.toPx()
            val bottomPaddingForLegend = 50.dp.toPx() // Extra space for legend below project names
            val chartWidth = size.width - (padding * 2)
            val chartHeight = size.height - padding - bottomPaddingForLegend
            
            val barWidth = (chartWidth / labels.size) * 0.35f
            val spacing = (chartWidth / labels.size) * 0.15f
            
            // Draw grid lines matching tick values
            val gridColor = Color(0xFF94A3B8).copy(alpha = 0.25f)
            tickValues.forEach { value ->
                val normalizedY = (value / displayMaxInLakhs).toFloat().coerceIn(0f, 1f)
                val y = padding + chartHeight - (normalizedY * chartHeight)
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(padding + chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            
            labels.forEachIndexed { index, _ ->
                val x = padding + (chartWidth / labels.size) * index + spacing
                
                // Budget bar (blue) - use displayMaxInLakhs for consistent scaling
                val budgetValueInLakhs = budgetDataInLakhs[index]
                val budgetHeight = if (displayMaxInLakhs > 0) {
                    ((budgetValueInLakhs / displayMaxInLakhs).toFloat().coerceIn(0f, 1f) * chartHeight).coerceAtLeast(0f)
                } else {
                    0f
                }
                drawRect(
                    color = Color(0xFF1D4ED8),
                    topLeft = Offset(x, padding + chartHeight - budgetHeight),
                    size = Size(barWidth, budgetHeight)
                )
                
                // Actual bar (green) - use displayMaxInLakhs for consistent scaling
                val actualValueInLakhs = actualDataInLakhs[index]
                val actualHeight = if (displayMaxInLakhs > 0) {
                    ((actualValueInLakhs / displayMaxInLakhs).toFloat().coerceIn(0f, 1f) * chartHeight).coerceAtLeast(0f)
                } else {
                    0f
                }
                drawRect(
                    color = Color(0xFF10B981),
                    topLeft = Offset(x + barWidth + spacing, padding + chartHeight - actualHeight),
                    size = Size(barWidth, actualHeight)
                )
            }
        }
        
        // Y-axis labels - using "L" format, moved further left
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .padding(start = 0.dp, top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            yAxisLabels.reversed().forEach { label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // X-axis labels with truncation and tap-to-expand, with legend below
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 4.dp)
        ) {
            // Project names
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                labels.forEachIndexed { index, label ->
                    val displayText = if (label.length <= maxLabelLength) {
                        label
                    } else {
                        label.take(maxLabelLength) + ".."
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { 
                                expandedLabelIndex = index
                            }
                    ) {
                        Text(
                            text = displayText,
                            fontSize = 9.sp,
                            color = Color(0xFF6B7280),
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // Legend below project names - centered
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                LegendItem(Color(0xFF1D4ED8), "Budget")
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(Color(0xFF10B981), "Actual")
            }
        }
        
        // Show full name in a tooltip/dialog when expanded
        expandedLabelIndex?.let { index ->
            if (index < labels.size) {
                AlertDialog(
                    onDismissRequest = { expandedLabelIndex = null },
                    title = { Text("Phase Name") },
                    text = { Text(labels[index]) },
                    confirmButton = {
                        TextButton(onClick = { expandedLabelIndex = null }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BarChartWithTooltips(
    labels: List<String>,
    budgetData: List<Double>,
    actualData: List<Double>,
    modifier: Modifier = Modifier
) {
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipOffset by remember { mutableStateOf<Offset?>(null) }
    
    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Dismiss tooltip when tapping outside chart area
                    detectTapGestures { tapOffset ->
                        val padding = 20.dp.toPx()
                        val chartWidth = size.width - (padding * 2)
                        
                        val x = tapOffset.x
                        // Calculate which project group was tapped (each group has budget + actual bars)
                        val groupWidth = chartWidth / labels.size
                        val index = ((x - padding) / groupWidth).toInt()
                        
                        if (index >= 0 && index < labels.size) {
                            // Toggle tooltip - if same index, dismiss; otherwise show new one
                            if (hoveredIndex == index) {
                                hoveredIndex = null
                                tooltipOffset = null
                            } else {
                                hoveredIndex = index
                                tooltipOffset = tapOffset
                            }
                        } else {
                            hoveredIndex = null
                            tooltipOffset = null
                        }
                    }
                }
        ) {
            if (labels.isEmpty()) return@Canvas
            
            val padding = 20.dp.toPx()
            val chartWidth = size.width - (padding * 2)
            val chartHeight = size.height - (padding * 2)
            val maxValue = max(budgetData.maxOrNull() ?: 0.0, actualData.maxOrNull() ?: 0.0)
            
            val barWidth = (chartWidth / labels.size) * 0.35f
            val spacing = (chartWidth / labels.size) * 0.15f
            
            // Draw grid lines
            val gridColor = Color(0xFF94A3B8).copy(alpha = 0.25f)
            for (i in 0..4) {
                val y = padding + (chartHeight / 4) * i
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(padding + chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            
            labels.forEachIndexed { index, _ ->
                val x = padding + (chartWidth / labels.size) * index + spacing
                val isHovered = hoveredIndex == index
                
                // Budget bar (dark grey/blue)
                val budgetHeight = (budgetData[index] / maxValue * chartHeight).toFloat()
                drawRect(
                    color = if (isHovered) Color(0xFF1D4ED8).copy(alpha = 0.8f) else Color(0xFF4B5563),
                    topLeft = Offset(x, padding + chartHeight - budgetHeight),
                    size = Size(barWidth, budgetHeight)
                )
                
                // Actual bar (green)
                val actualHeight = (actualData[index] / maxValue * chartHeight).toFloat()
                drawRect(
                    color = if (isHovered) Color(0xFF10B981).copy(alpha = 0.8f) else Color(0xFF10B981),
                    topLeft = Offset(x + barWidth + spacing, padding + chartHeight - actualHeight),
                    size = Size(barWidth, actualHeight)
                )
            }
        }
        
        // Y-axis labels
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .padding(start = 4.dp, top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val maxValue = max(budgetData.maxOrNull() ?: 0.0, actualData.maxOrNull() ?: 0.0)
            for (i in 4 downTo 0) {
                val value = (maxValue / 4) * i
                Text(
                    text = formatNumber(value),
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
        
        // X-axis labels
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 9.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 1
                )
            }
        }
        
        // Legend
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 30.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(Color(0xFF4B5563), "Budget (₹ Cr)")
            LegendItem(Color(0xFF10B981), "Actual (₹ Cr)")
        }
        
        // Tooltip
        hoveredIndex?.let { index ->
            tooltipOffset?.let { offset ->
                if (index < labels.size) {
                    Card(
                        modifier = Modifier
                            .offset(x = (offset.x - 80).dp, y = (offset.y - 60).dp)
                            .width(160.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = labels[index],
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Budget (₹ Cr): ${String.format("%.2f", budgetData[index])}",
                                fontSize = 11.sp,
                                color = Color(0xFFD1D5DB)
                            )
                            Text(
                                text = "Actual (₹ Cr): ${String.format("%.2f", actualData[index])}",
                                fontSize = 11.sp,
                                color = Color(0xFFD1D5DB)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalBarChartWithTooltips(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier
) {
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipOffset by remember { mutableStateOf<Offset?>(null) }
    
    // Convert values to lakhs for display (1 lakh = 100,000 rupees)
    val valuesInLakhs = remember(values) {
        values.map { it / 100000.0 }
    }
    
    // Calculate displayMax in lakhs - round up to next 10L increment, minimum 30L
    val rawMaxValueInLakhs = valuesInLakhs.maxOrNull() ?: 0.0
    val displayMaxInLakhs = remember(rawMaxValueInLakhs) {
        when {
            rawMaxValueInLakhs <= 0 -> 30.0
            rawMaxValueInLakhs <= 30 -> 30.0
            rawMaxValueInLakhs <= 40 -> 40.0
            rawMaxValueInLakhs <= 50 -> 50.0
            rawMaxValueInLakhs <= 60 -> 60.0
            rawMaxValueInLakhs <= 70 -> 70.0
            rawMaxValueInLakhs <= 80 -> 80.0
            else -> ((rawMaxValueInLakhs / 10).toInt() + 1) * 10.0 // Round up to next 10L
        }
    }
    
    // Generate Y-axis labels dynamically based on max value (in 10L increments)
    val yAxisLabels = remember(displayMaxInLakhs) {
        val step = 10.0 // 10L intervals
        val labels = mutableListOf<String>()
        labels.add("0")
        var current = step
        while (current <= displayMaxInLakhs) {
            labels.add(String.format("%.2fL", current))
            current += step
        }
        labels
    }
    
    // Calculate tick values for grid lines (in 10L increments)
    val tickValues = remember(displayMaxInLakhs) {
        val step = 10.0
        val values = mutableListOf(0.0)
        var current = step
        while (current <= displayMaxInLakhs) {
            values.add(current)
            current += step
        }
        values
    }
    
    val displayMax = displayMaxInLakhs
    
    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val padding = 20.dp.toPx()
                        val chartWidth = size.width - (padding * 2)
                        val x = tapOffset.x
                        
                        // Calculate which bar was tapped
                        val barWidth = (chartWidth / labels.size) * 0.6f
                        val spacing = (chartWidth / labels.size) * 0.2f
                        val groupWidth = chartWidth / labels.size
                        val index = ((x - padding) / groupWidth).toInt()
                        
                        if (index >= 0 && index < labels.size) {
                            // Toggle tooltip - if same index, dismiss; otherwise show new one
                            if (hoveredIndex == index) {
                                hoveredIndex = null
                                tooltipOffset = null
                            } else {
                                hoveredIndex = index
                                tooltipOffset = tapOffset
                            }
                        } else {
                            hoveredIndex = null
                            tooltipOffset = null
                        }
                    }
                }
        ) {
            if (labels.isEmpty()) return@Canvas
            
            val padding = 20.dp.toPx()
            val chartWidth = size.width - (padding * 2)
            val chartHeight = size.height - (padding * 2)
            
            val barWidth = (chartWidth / labels.size) * 0.6f
            val spacing = (chartWidth / labels.size) * 0.2f
            
            // Draw grid lines matching tick values
            val gridColor = Color(0xFF94A3B8).copy(alpha = 0.25f)
            tickValues.forEach { value ->
                val normalizedY = (value / displayMaxInLakhs).toFloat().coerceIn(0f, 1f)
                val y = padding + chartHeight - (normalizedY * chartHeight)
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(padding + chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            
            labels.forEachIndexed { index, _ ->
                val x = padding + (chartWidth / labels.size) * index + spacing
                val isHovered = hoveredIndex == index
                
                // Bar (orange color) - use displayMaxInLakhs for consistent scaling
                val valueInLakhs = valuesInLakhs[index]
                val barHeight = if (displayMaxInLakhs > 0) {
                    ((valueInLakhs / displayMaxInLakhs).toFloat().coerceIn(0f, 1f) * chartHeight).coerceAtLeast(0f)
                } else {
                    0f
                }
                drawRoundRect(
                    color = if (isHovered) Color(0xFFF97316).copy(alpha = 0.9f) else Color(0xFFF97316),
                    topLeft = Offset(x, padding + chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
        
        // Y-axis labels - moved further left, using "L" format
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .padding(start = 0.dp, top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            yAxisLabels.reversed().forEach { label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // X-axis labels
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 9.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 1
                )
            }
        }
        
        // Tooltip with orange icon
        hoveredIndex?.let { index ->
            tooltipOffset?.let { offset ->
                if (index < labels.size) {
                    Card(
                        modifier = Modifier
                            .offset(x = (offset.x - 80).dp, y = (offset.y - 70).dp)
                            .width(160.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = labels[index],
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Orange square icon
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color(0xFFF97316), RoundedCornerShape(2.dp))
                                )
                                // Format tooltip - show in lakhs with "L"
                                val actualValue = values[index]
                                val valueInLakhs = actualValue / 100000.0
                                val formattedValue = String.format("%.2fL", valueInLakhs)
                                Text(
                                    text = "Cost (₹ L): $formattedValue",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD1D5DB)
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
fun BarChart(
    labels: List<String>,
    budgetData: List<Double>,
    actualData: List<Double>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (labels.isEmpty()) return@Canvas
            
            val padding = 20.dp.toPx()
            val chartWidth = size.width - (padding * 2)
            val chartHeight = size.height - (padding * 2)
            val maxValue = max(budgetData.maxOrNull() ?: 0.0, actualData.maxOrNull() ?: 0.0)
            
            val barWidth = (chartWidth / labels.size) * 0.35f
            val spacing = (chartWidth / labels.size) * 0.15f
            
            // Draw grid lines
            val gridColor = Color(0xFF94A3B8).copy(alpha = 0.25f)
            for (i in 0..4) {
                val y = padding + (chartHeight / 4) * i
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(padding + chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            
            labels.forEachIndexed { index, _ ->
                val x = padding + (chartWidth / labels.size) * index + spacing
                
                // Budget bar (blue)
                val budgetHeight = (budgetData[index] / maxValue * chartHeight).toFloat()
                drawRect(
                    color = Color(0xFF1D4ED8),
                    topLeft = Offset(x, padding + chartHeight - budgetHeight),
                    size = Size(barWidth, budgetHeight)
                )
                
                // Actual bar (green)
                val actualHeight = (actualData[index] / maxValue * chartHeight).toFloat()
                drawRect(
                    color = Color(0xFF10B981),
                    topLeft = Offset(x + barWidth + spacing, padding + chartHeight - actualHeight),
                    size = Size(barWidth, actualHeight)
                )
            }
        }
        
        // Y-axis labels
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .padding(start = 4.dp, top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val maxValue = max(budgetData.maxOrNull() ?: 0.0, actualData.maxOrNull() ?: 0.0)
            for (i in 4 downTo 0) {
                val value = (maxValue / 4) * i
                Text(
                    text = formatNumber(value),
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
        
        // X-axis labels
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 9.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 1
                )
            }
        }
        
        // Legend
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 30.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(Color(0xFF1D4ED8), "Budget")
            LegendItem(Color(0xFF10B981), "Actual")
        }
    }
}

@Composable
fun HorizontalBarChartForCount(
    labels: List<String>,
    values: List<Double>, // Values are counts (number of expenses)
    modifier: Modifier = Modifier,
    onCategoryClick: ((String) -> Unit)? = null // Callback when tooltip is clicked
) {
    // State for tracking selected bar and tooltip position
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipPosition by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    
    // Calculate max value for X-axis - round up to nice number (0, 200, 400, 600, 800, etc.)
    // This needs to be outside Canvas so it can be used in both Canvas and X-axis labels
    val displayMax = remember(values) {
        val maxCount = values.maxOrNull() ?: 0.0
        when {
            maxCount == 0.0 -> 200.0 // Default to 200 for empty data
            maxCount <= 200 -> 200.0 // Round up to 200
            maxCount <= 400 -> 400.0 // Round up to 400
            maxCount <= 600 -> 600.0 // Round up to 600
            maxCount <= 800 -> 800.0 // Round up to 800
            else -> {
                // For larger values, round up to next multiple of 200
                ((ceil(maxCount / 200.0).toInt()) * 200).toDouble()
            }
        }
    }
    
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        if (labels.isEmpty()) return@detectTapGestures
                        
                        val leftPadding = 120.dp.toPx()
                        val rightPadding = 20.dp.toPx()
                        val topPadding = 20.dp.toPx()
                        val bottomPadding = 30.dp.toPx()
                        val chartWidth = size.width - leftPadding - rightPadding
                        val chartHeight = size.height - topPadding - bottomPadding
                        val barHeight = (chartHeight / labels.size) * 0.6f
                        val spacing = (chartHeight / labels.size) * 0.4f
                        
                        // Check if tap is within chart area
                        if (tapOffset.x >= leftPadding && tapOffset.x <= size.width - rightPadding &&
                            tapOffset.y >= topPadding && tapOffset.y <= size.height - bottomPadding) {
                            
                            // Calculate which bar was clicked
                            val clickedIndex = labels.indices.firstOrNull { index ->
                                val y = topPadding + (chartHeight / labels.size) * index + spacing / 2
                                val barWidth = (values[index] / displayMax * chartWidth).toFloat()
                                
                                // Check if tap is within the bar bounds
                                tapOffset.y >= y && tapOffset.y <= y + barHeight &&
                                tapOffset.x >= leftPadding && tapOffset.x <= leftPadding + barWidth
                            }
                            
                            if (clickedIndex != null) {
                                selectedBarIndex = clickedIndex
                                // Position tooltip above the bar, centered horizontally
                                val barY = topPadding + (chartHeight / labels.size) * clickedIndex + spacing / 2
                                val barWidth = (values[clickedIndex] / displayMax * chartWidth).toFloat()
                                val tooltipX = leftPadding + (barWidth / 2)
                                tooltipPosition = Offset(tooltipX, barY - 10.dp.toPx())
                            } else {
                                // Click outside bars - hide tooltip
                                selectedBarIndex = null
                                tooltipPosition = null
                            }
                        } else {
                            // Click outside chart area - hide tooltip
                            selectedBarIndex = null
                            tooltipPosition = null
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (labels.isEmpty()) return@Canvas
                
                val leftPadding = 120.dp.toPx() // Space for Y-axis category labels
                val rightPadding = 20.dp.toPx()
                val topPadding = 20.dp.toPx()
                val bottomPadding = 30.dp.toPx() // Space for X-axis labels
                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding
                
                val barHeight = (chartHeight / labels.size) * 0.6f
                val spacing = (chartHeight / labels.size) * 0.4f
                
                // Draw grid lines (vertical lines for X-axis counts)
                val gridColor = Color(0xFF94A3B8).copy(alpha = 0.25f)
                for (i in 0..4) {
                    val x = leftPadding + (chartWidth / 4) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(x, topPadding),
                        end = Offset(x, topPadding + chartHeight),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                // Draw horizontal bars with blue color
                labels.forEachIndexed { index, _ ->
                    val y = topPadding + (chartHeight / labels.size) * index + spacing / 2
                    val barWidth = (values[index] / displayMax * chartWidth).toFloat()
                    
                    // Highlight selected bar with slightly different color
                    val barColor = if (selectedBarIndex == index) {
                        Color(0xFF2563EB) // Darker blue for selected
                    } else {
                        Color(0xFF3B82F6) // Normal blue color
                    }
                    
                    drawRect(
                        color = barColor,
                        topLeft = Offset(leftPadding, y),
                        size = Size(barWidth, barHeight)
                    )
                }
            }
            
            // Y-axis labels (category names on left) - aligned with bars
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .padding(start = 8.dp, top = 20.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(110.dp)
                    )
                }
            }
            
            // X-axis labels (counts at bottom) - show as integers (0, 200, 400, 600, 800)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 120.dp, end = 20.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0..4) {
                    val value = ((displayMax / 4) * i).toInt()
                    Text(
                        text = value.toString(),
                        fontSize = 10.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
            
            // Tooltip overlay - show when a bar is selected
            selectedBarIndex?.let { index ->
                tooltipPosition?.let { position ->
                    if (index < labels.size && index < values.size) {
                        val categoryName = labels[index]
                        val expenseCount = values[index].toInt()
                        
                        // Convert position from pixels to dp for layout and center tooltip
                        val tooltipX = with(density) { position.x.toDp() }
                        val tooltipY = with(density) { position.y.toDp() }
                        
                        // Tooltip bubble - positioned above the bar, centered horizontally
                        Box(
                            modifier = Modifier
                                .offset(x = tooltipX - 90.dp, y = tooltipY - 75.dp) // Center tooltip above bar
                                .background(
                                    color = Color(0xFFE0F2FE), // Light blue background
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = onCategoryClick != null) {
                                    onCategoryClick?.invoke(categoryName)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Category name in bold
                                Text(
                                    text = categoryName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827) // Dark black
                                )
                                
                                // Icon and expense count
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = "Expenses",
                                        tint = Color(0xFF3B82F6), // Blue icon
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Expenses",
                                        fontSize = 11.sp,
                                        color = Color(0xFF6B7280) // Grey text
                                    )
                                    Text(
                                        text = expenseCount.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF111827) // Dark black
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailBottomSheet(
    categoryName: String,
    projects: List<Project>,
    allExpenses: List<Expense>,
    selectedStage: Set<String>,
    selectedDepartment: String,
    selectedProjectStatus: Set<String>,
    startDate: Date?,
    endDate: Date?,
    allPhases: Map<String, List<Phase>>,
    onDismiss: () -> Unit,
    onProjectClick: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Calculate date range for last 30 days
    val calendar = Calendar.getInstance()
    val today = calendar.time
    calendar.add(Calendar.DAY_OF_YEAR, -30)
    val thirtyDaysAgo = calendar.time
    
    // Use provided date range if available, otherwise use last 30 days
    val filterStartDate = startDate ?: thirtyDaysAgo
    val filterEndDate = endDate ?: today
    
    // Filter expenses for this category
    val categoryExpenses = remember(categoryName, projects, allExpenses, selectedStage, selectedDepartment, selectedProjectStatus, startDate, endDate, allPhases) {
        allExpenses.filter { expense ->
            // Match category
            val matchesCategory = expense.category.trim().equals(categoryName.trim(), ignoreCase = true)
            if (!matchesCategory) return@filter false
            
            // Match project
            val matchesProject = projects.any { it.id == expense.projectId }
            if (!matchesProject) return@filter false
            
            // Match status - only count APPROVED expenses
            val matchesStatus = expense.status == ExpenseStatus.APPROVED
            if (!matchesStatus) return@filter false
            
            // Filter by date range
            val matchesDateRange = try {
                val expenseDate = expense.submittedAt?.toDate() ?: expense.getDateAsTimestamp()?.toDate()
                if (expenseDate != null) {
                    val expenseCal = Calendar.getInstance().apply { time = expenseDate }
                    val startCal = Calendar.getInstance().apply {
                        time = filterStartDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val endCal = Calendar.getInstance().apply {
                        time = filterEndDate
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    !expenseCal.before(startCal) && !expenseCal.after(endCal)
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
            if (!matchesDateRange) return@filter false
            
            // Filter by stage
            val matchesStage = if (selectedStage.isEmpty()) {
                true
            } else {
                val projectId = expense.projectId
                val phases = allPhases[projectId] ?: emptyList()
                if (phases.isEmpty()) {
                    true
                } else {
                    val expensePhaseId = expense.phaseId
                    if (expensePhaseId.isNullOrBlank()) {
                        true
                    } else {
                        selectedStage.any { stageName ->
                            phases.any { it.phaseName == stageName && it.id == expensePhaseId }
                        }
                    }
                }
            }
            if (!matchesStage) return@filter false
            
            // Filter by department
            val matchesDepartment = if (selectedDepartment == "all") {
                true
            } else {
                val expenseDept = expense.department?.trim() ?: ""
                val selectedDept = selectedDepartment.trim()
                expenseDept.equals(selectedDept, ignoreCase = true)
            }
            if (!matchesDepartment) return@filter false
            
            true
        }
    }
    
    // Get unique project IDs from expenses
    val projectIds = categoryExpenses.mapNotNull { it.projectId }.distinct()
    
    // Get projects that have expenses in this category
    val categoryProjects = remember(projectIds, projects) {
        projects.filter { it.id in projectIds }
    }
    
    // Count expenses and projects
    val expenseCount = categoryExpenses.size
    val projectCount = categoryProjects.size
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF5F5F7), // Light gray background
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        color = Color(0xFFC6C6C8),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with Done button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title
                Text(
                    text = categoryName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111),
                    modifier = Modifier.weight(1f)
                )
                
                // Done button
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Done",
                        fontSize = 17.sp,
                        color = Color(0xFF3B82F6), // Blue color
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Summary stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expenses count
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = expenseCount.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6) // Blue
                    )
                    Text(
                        text = "Expenses",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280) // Gray
                    )
                }
                
                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color(0xFFE5E7EB))
                )
                
                // Projects count
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = projectCount.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981) // Green
                    )
                    Text(
                        text = "Projects",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280) // Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Projects list
            if (categoryProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No projects found",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            } else {
                categoryProjects.forEach { project ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onProjectClick != null) {
                                onProjectClick?.invoke(project.id ?: "")
                            },
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Project icon
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "Project",
                                tint = Color(0xFF3B82F6), // Blue icon
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Project name
                            Text(
                                text = project.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF111827),
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Chevron
                            if (onProjectClick != null) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Navigate",
                                    tint = Color(0xFF9CA3AF),
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

@Composable
fun HorizontalBarChartForSuspensionReasons(
    labels: List<String>,
    values: List<Double>, // Values are counts (number of projects)
    modifier: Modifier = Modifier,
    onReasonClick: ((String) -> Unit)? = null // Callback when tooltip is clicked
) {
    // State for tracking selected bar and tooltip position
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipPosition by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    
    // Calculate max value for X-axis - round up to nice number
    val displayMax = remember(values) {
        val maxCount = values.maxOrNull() ?: 0.0
        when {
            maxCount == 0.0 -> 1.0
            maxCount <= 1.0 -> 1.0
            maxCount <= 2.0 -> 2.0
            maxCount <= 5.0 -> 5.0
            maxCount <= 10.0 -> 10.0
            else -> {
                ((ceil(maxCount / 5.0).toInt()) * 5).toDouble()
            }
        }
    }
    
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        if (labels.isEmpty()) return@detectTapGestures
                        
                        val leftPadding = 120.dp.toPx()
                        val rightPadding = 20.dp.toPx()
                        val topPadding = 20.dp.toPx()
                        val bottomPadding = 30.dp.toPx()
                        val chartWidth = size.width - leftPadding - rightPadding
                        val chartHeight = size.height - topPadding - bottomPadding
                        val barHeight = (chartHeight / labels.size) * 0.6f
                        val spacing = (chartHeight / labels.size) * 0.4f
                        
                        // Check if tap is within chart area
                        if (tapOffset.x >= leftPadding && tapOffset.x <= size.width - rightPadding &&
                            tapOffset.y >= topPadding && tapOffset.y <= size.height - bottomPadding) {
                            
                            // Calculate which bar was clicked
                            val clickedIndex = labels.indices.firstOrNull { index ->
                                val y = topPadding + (chartHeight / labels.size) * index + spacing / 2
                                val barWidth = (values[index] / displayMax * chartWidth).toFloat()
                                
                                // Check if tap is within the bar bounds
                                tapOffset.y >= y && tapOffset.y <= y + barHeight &&
                                tapOffset.x >= leftPadding && tapOffset.x <= leftPadding + barWidth
                            }
                            
                            if (clickedIndex != null) {
                                selectedBarIndex = clickedIndex
                                // Position tooltip above the bar, centered horizontally
                                val barY = topPadding + (chartHeight / labels.size) * clickedIndex + spacing / 2
                                val barWidth = (values[clickedIndex] / displayMax * chartWidth).toFloat()
                                val tooltipX = leftPadding + (barWidth / 2)
                                tooltipPosition = Offset(tooltipX, barY - 10.dp.toPx())
                            } else {
                                // Click outside bars - hide tooltip
                                selectedBarIndex = null
                                tooltipPosition = null
                            }
                        } else {
                            // Click outside chart area - hide tooltip
                            selectedBarIndex = null
                            tooltipPosition = null
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (labels.isEmpty()) return@Canvas
                
                val leftPadding = 120.dp.toPx() // Space for Y-axis category labels
                val rightPadding = 20.dp.toPx()
                val topPadding = 20.dp.toPx()
                val bottomPadding = 30.dp.toPx() // Space for X-axis labels
                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding
                
                val barHeight = (chartHeight / labels.size) * 0.6f
                val spacing = (chartHeight / labels.size) * 0.4f
                
                // Draw grid lines (vertical lines for X-axis counts)
                val gridColor = Color(0xFF94A3B8).copy(alpha = 0.25f)
                val numGridLines = if (displayMax <= 5) displayMax.toInt() else 5
                for (i in 0..numGridLines) {
                    val x = leftPadding + (chartWidth / numGridLines) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(x, topPadding),
                        end = Offset(x, topPadding + chartHeight),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                // Draw horizontal bars with orange color
                labels.forEachIndexed { index, _ ->
                    val y = topPadding + (chartHeight / labels.size) * index + spacing / 2
                    val barWidth = (values[index] / displayMax * chartWidth).toFloat()
                    
                    // Highlight selected bar with slightly different color
                    val barColor = if (selectedBarIndex == index) {
                        Color(0xFFEA580C) // Darker orange for selected
                    } else {
                        Color(0xFFF97316) // Normal orange color
                    }
                    
                    drawRect(
                        color = barColor,
                        topLeft = Offset(leftPadding, y),
                        size = Size(barWidth, barHeight)
                    )
                }
            }
            
            // Y-axis labels (reason names on left) - aligned with bars
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .padding(start = 8.dp, top = 20.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = Color(0xFF6B7280),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(110.dp)
                    )
                }
            }
            
            // X-axis labels (counts at bottom)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 120.dp, end = 20.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val numLabels = if (displayMax <= 5) displayMax.toInt() else 5
                for (i in 0..numLabels) {
                    val value = ((displayMax / numLabels) * i)
                    Text(
                        text = if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value),
                        fontSize = 10.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
            
            // Tooltip overlay - show when a bar is selected
            selectedBarIndex?.let { index ->
                tooltipPosition?.let { position ->
                    if (index < labels.size && index < values.size) {
                        val reasonName = labels[index]
                        val projectCount = values[index].toInt()
                        
                        // Convert position from pixels to dp for layout and center tooltip
                        val tooltipX = with(density) { position.x.toDp() }
                        val tooltipY = with(density) { position.y.toDp() }
                        
                        // Tooltip bubble - positioned above the bar, centered horizontally
                        Box(
                            modifier = Modifier
                                .offset(x = tooltipX - 90.dp, y = tooltipY - 75.dp) // Center tooltip above bar
                                .background(
                                    color = Color(0xFFFEF3C7), // Light beige/yellow background (matching reference)
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = onReasonClick != null) {
                                    onReasonClick?.invoke(reasonName)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Reason name in bold
                                Text(
                                    text = reasonName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827) // Dark black
                                )
                                
                                // Icon and project count
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Orange circular icon
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(
                                                color = Color(0xFFF97316), // Orange
                                                shape = androidx.compose.foundation.shape.CircleShape
                                            )
                                    )
                                    Text(
                                        text = "Projects",
                                        fontSize = 11.sp,
                                        color = Color(0xFF6B7280) // Grey text
                                    )
                                    Text(
                                        text = projectCount.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF111827) // Dark black
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuspensionReasonDetailBottomSheet(
    reasonName: String,
    projects: List<Project>,
    onDismiss: () -> Unit,
    onProjectClick: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Filter projects by suspension reason
    val suspendedProjects = remember(reasonName, projects) {
        projects.filter { project ->
            project.isSuspended == true &&
            project.suspensionReason?.trim()?.equals(reasonName.trim(), ignoreCase = true) == true
        }
    }
    
    // Count projects
    val projectCount = suspendedProjects.size
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF5F5F7), // Light gray background
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        color = Color(0xFFC6C6C8),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with Done button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title
                Text(
                    text = reasonName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111),
                    modifier = Modifier.weight(1f)
                )
                
                // Done button
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Done",
                        fontSize = 17.sp,
                        color = Color(0xFF3B82F6), // Blue color
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Summary stats row - Only show Projects count (centered)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = projectCount.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF97316) // Orange (matching reference)
                    )
                    Text(
                        text = "Projects",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280) // Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Projects list
            if (suspendedProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No projects found",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            } else {
                suspendedProjects.forEach { project ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onProjectClick != null) {
                                onProjectClick?.invoke(project.id ?: "")
                            },
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Project icon - Orange (matching reference)
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "Project",
                                tint = Color(0xFFF97316), // Orange icon (matching reference)
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Project name
                            Text(
                                text = project.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF111827),
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Chevron
                            if (onProjectClick != null) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Navigate",
                                    tint = Color(0xFF9CA3AF),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectStatusDetailBottomSheet(
    statusName: String,
    projects: List<Project>,
    onDismiss: () -> Unit,
    onProjectClick: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Helper function to determine the actual display status of a project
    fun getProjectDisplayStatus(project: Project): String {
        // Priority: Suspended > Maintenance > Project Status
        return when {
            project.isSuspended == true || !project.suspendedDate.isNullOrBlank() -> "Suspended"
            !project.maintenanceDate.isNullOrBlank() -> "Maintenance"
            project.status.equals("ACTIVE", ignoreCase = true) -> "Active"
            project.status.equals("COMPLETED", ignoreCase = true) -> "Completed"
            project.status.equals("ARCHIVE", ignoreCase = true) -> "Archive"
            project.status.equals("SUSPENDED", ignoreCase = true) -> "Suspended"
            project.status.equals("LOCKED", ignoreCase = true) -> "LOCKED"
            project.status.equals("IN REVIEW", ignoreCase = true) -> "IN REVIEW"
            else -> project.status.replaceFirstChar { it.uppercaseChar() }
        }
    }
    
    // Filter projects by status
    val statusProjects = remember(statusName, projects) {
        projects.filter { project ->
            val displayStatus = getProjectDisplayStatus(project)
            displayStatus.equals(statusName, ignoreCase = true)
        }
    }
    
    // Count projects
    val projectCount = statusProjects.size
    
    // Calculate percentage
    val percentage = if (projects.isNotEmpty()) {
        (projectCount.toDouble() / projects.size.toDouble()) * 100.0
    } else {
        0.0
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF5F5F7), // Light gray background
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        color = Color(0xFFC6C6C8),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with Done button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title
                Text(
                    text = statusName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111),
                    modifier = Modifier.weight(1f)
                )
                
                // Done button
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Done",
                        fontSize = 17.sp,
                        color = Color(0xFF3B82F6), // Blue color
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Summary stats row - Percentage and Projects count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Percentage
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${String.format("%.1f", percentage)}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6) // Blue
                    )
                    Text(
                        text = "Percentage",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280) // Gray
                    )
                }
                
                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color(0xFFE5E7EB))
                )
                
                // Projects count
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = projectCount.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981) // Green
                    )
                    Text(
                        text = "Projects",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280) // Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Projects list
            if (statusProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No projects found",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            } else {
                statusProjects.forEach { project ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onProjectClick != null) {
                                onProjectClick?.invoke(project.id ?: "")
                            },
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Project icon - Use status color
                            val statusColor = when {
                                statusName.equals("Active", ignoreCase = true) -> Color(0xFF10B981) // Green
                                statusName.equals("Suspended", ignoreCase = true) -> Color(0xFFF97316) // Orange
                                statusName.equals("Completed", ignoreCase = true) -> Color(0xFF3B82F6) // Blue
                                statusName.equals("Maintenance", ignoreCase = true) -> Color(0xFFA855F7) // Purple
                                statusName.equals("Archive", ignoreCase = true) -> Color(0xFF6B7280) // Gray
                                statusName.equals("LOCKED", ignoreCase = true) -> Color(0xFF9C27B0) // Purple
                                statusName.equals("IN REVIEW", ignoreCase = true) -> Color(0xFF6366F1) // Indigo
                                else -> Color(0xFF6B7280) // Default gray
                            }
                            
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "Project",
                                tint = statusColor, // Use status color for icon
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Project name
                            Text(
                                text = project.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF111827),
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Chevron
                            if (onProjectClick != null) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Navigate",
                                    tint = Color(0xFF9CA3AF),
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

@Composable
fun HorizontalBarChartForPercentage(
    labels: List<String>,
    values: List<Double>, // Values are already in percentages (0-100)
    projectCounts: List<Int> = emptyList(), // Project counts for each status
    modifier: Modifier = Modifier,
    onStatusClick: ((String) -> Unit)? = null // Callback when tooltip is clicked
) {
    // State for tracking selected bar and tooltip position
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipPosition by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        if (labels.isEmpty()) return@detectTapGestures
                        
                        val leftPadding = 120.dp.toPx()
                        val rightPadding = 20.dp.toPx()
                        val topPadding = 20.dp.toPx()
                        val bottomPadding = 30.dp.toPx()
                        val chartWidth = size.width - leftPadding - rightPadding
                        val chartHeight = size.height - topPadding - bottomPadding
                        val barHeight = (chartHeight / labels.size) * 0.6f
                        val spacing = (chartHeight / labels.size) * 0.4f
                        val maxValue = 100.0
                        
                        // Check if tap is within chart area
                        if (tapOffset.x >= leftPadding && tapOffset.x <= size.width - rightPadding &&
                            tapOffset.y >= topPadding && tapOffset.y <= size.height - bottomPadding) {
                            
                            // Calculate which bar was clicked
                            val clickedIndex = labels.indices.firstOrNull { index ->
                                val y = topPadding + (chartHeight / labels.size) * index + spacing / 2
                                val barWidth = (values[index] / maxValue * chartWidth).toFloat()
                                
                                // Check if tap is within the bar bounds
                                tapOffset.y >= y && tapOffset.y <= y + barHeight &&
                                tapOffset.x >= leftPadding && tapOffset.x <= leftPadding + barWidth
                            }
                            
                            if (clickedIndex != null) {
                                selectedBarIndex = clickedIndex
                                // Position tooltip above the bar, centered horizontally
                                val barY = topPadding + (chartHeight / labels.size) * clickedIndex + spacing / 2
                                val barWidth = (values[clickedIndex] / maxValue * chartWidth).toFloat()
                                val tooltipX = leftPadding + (barWidth / 2)
                                tooltipPosition = Offset(tooltipX, barY - 10.dp.toPx())
                            } else {
                                // Click outside bars - hide tooltip
                                selectedBarIndex = null
                                tooltipPosition = null
                            }
                        } else {
                            // Click outside chart area - hide tooltip
                            selectedBarIndex = null
                            tooltipPosition = null
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
            if (labels.isEmpty()) return@Canvas
            
            val leftPadding = 120.dp.toPx() // Space for Y-axis status labels
            val rightPadding = 20.dp.toPx()
            val topPadding = 20.dp.toPx()
            val bottomPadding = 30.dp.toPx() // Space for X-axis labels
            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding
            
            val barHeight = (chartHeight / labels.size) * 0.6f
            val spacing = (chartHeight / labels.size) * 0.4f
            
            // X-axis max is always 100% for percentage chart
            val maxValue = 100.0
            
            // Draw grid lines (vertical lines for X-axis percentages)
            val gridColor = Color(0xFF94A3B8).copy(alpha = 0.25f)
            for (i in 0..4) {
                val x = leftPadding + (chartWidth / 4) * i
                drawLine(
                    color = gridColor,
                    start = Offset(x, topPadding),
                    end = Offset(x, topPadding + chartHeight),
                    strokeWidth = 1.dp.toPx()
                )
            }
            
            // Draw horizontal bars with different colors for each status
            labels.forEachIndexed { index, status ->
                val y = topPadding + (chartHeight / labels.size) * index + spacing / 2
                val barWidth = (values[index] / maxValue * chartWidth).toFloat()
                
                // Assign colors based on status (matching reference image)
                val baseColor = when {
                    status.equals("Active", ignoreCase = true) -> Color(0xFF10B981) // Green
                    status.equals("Suspended", ignoreCase = true) -> Color(0xFFF97316) // Orange
                    status.equals("Completed", ignoreCase = true) -> Color(0xFF3B82F6) // Blue
                    status.equals("Maintenance", ignoreCase = true) -> Color(0xFFA855F7) // Purple
                    status.equals("Archive", ignoreCase = true) -> Color(0xFF6B7280) // Gray
                    else -> Color(0xFF6B7280) // Default gray
                }
                
                // Highlight selected bar with darker color
                val barColor = if (selectedBarIndex == index) {
                    baseColor.copy(alpha = 0.8f) // Slightly darker for selected
                } else {
                    baseColor
                }
                
                drawRect(
                    color = barColor,
                    topLeft = Offset(leftPadding, y),
                    size = Size(barWidth, barHeight)
                )
            }
            }
            
            // Y-axis labels (status names on left) - aligned with bars
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .padding(start = 8.dp, top = 20.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(110.dp)
                )
            }
        }
        
            // X-axis labels (percentages at bottom)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 120.dp, end = 20.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0..4) {
                    val value = (100 / 4) * i
                    Text(
                        text = "${value}%",
                        fontSize = 10.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
            
            // Tooltip overlay - show when a bar is selected
            selectedBarIndex?.let { index ->
                tooltipPosition?.let { position ->
                    if (index < labels.size && index < values.size) {
                        val statusName = labels[index]
                        val percentage = values[index]
                        val projectCount = if (index < projectCounts.size) projectCounts[index] else 0
                        
                        // Convert position from pixels to dp for layout and center tooltip
                        val tooltipX = with(density) { position.x.toDp() }
                        val tooltipY = with(density) { position.y.toDp() }
                        
                        // Get status color for the dot (matching reference)
                        val statusColor = when {
                            statusName.equals("Active", ignoreCase = true) -> Color(0xFF10B981) // Green
                            statusName.equals("Suspended", ignoreCase = true) -> Color(0xFFF97316) // Orange
                            statusName.equals("Completed", ignoreCase = true) -> Color(0xFF3B82F6) // Blue
                            statusName.equals("Maintenance", ignoreCase = true) -> Color(0xFFA855F7) // Purple
                            statusName.equals("Archive", ignoreCase = true) -> Color(0xFF6B7280) // Gray
                            else -> Color(0xFF6B7280) // Default gray
                        }
                        
                        // Tooltip bubble - positioned above the bar, centered horizontally (static, not clickable)
                        Box(
                            modifier = Modifier
                                .offset(x = tooltipX - 100.dp, y = tooltipY - 75.dp) // Center tooltip above bar
                                .background(
                                    color = Color(0xFFFEF3C7), // Light beige/yellow background (matching reference)
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Status name in bold
                                Text(
                                    text = statusName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827) // Dark black
                                )
                                
                                // Percentage and project count row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Colored circular dot matching the bar color
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                color = statusColor,
                                                shape = androidx.compose.foundation.shape.CircleShape
                                            )
                                    )
                                    Text(
                                        text = "${String.format("%.1f", percentage)}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF111827) // Dark black
                                    )
                                    Text(
                                        text = "$projectCount projects",
                                        fontSize = 11.sp,
                                        color = Color(0xFF6B7280) // Grey text
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

@Composable
fun HorizontalBarChartWithScroll(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier
) {
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipOffset by remember { mutableStateOf<Offset?>(null) }
    var storedBarPositions by remember { mutableStateOf<List<Pair<Offset, Size>>>(emptyList()) }
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    
    // Calculate displayMax for consistent scaling (values are in Cr/day)
    val rawMaxValue = values.maxOrNull() ?: 0.0
    val displayMax = remember(rawMaxValue) {
        if (rawMaxValue > 0) {
            val paddedMax = rawMaxValue * 1.15
            // For Cr/day, use smaller steps
            val stepSize = when {
                paddedMax < 0.1 -> 0.01
                paddedMax < 0.5 -> 0.05
                paddedMax < 1.0 -> 0.1
                paddedMax < 2.0 -> 0.2
                paddedMax < 5.0 -> 0.5
                else -> 1.0
            }
            val numStepsNeeded = ceil(paddedMax / stepSize)
            val intervalsNeeded = ceil(numStepsNeeded / 6.0).toInt()
            val calculatedMax = intervalsNeeded * 6 * stepSize
            if (calculatedMax > rawMaxValue * 1.3) {
                val conservativeSteps = ceil(paddedMax / stepSize).toInt()
                val conservativeIntervals = maxOf(6, ((conservativeSteps + 5) / 6) * 6)
                conservativeIntervals * stepSize
            } else {
                calculatedMax
            }
        } else {
            0.1
        }
    }
    
    Box(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Scrollable chart area (Y-axis labels + chart)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Y-axis labels (project names) on left - scrollable vertically
                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight()
                        .verticalScroll(verticalScrollState, enabled = true)
                        .padding(start = 8.dp, top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(50.dp)
                ) {
                    labels.forEach { label ->
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = Color(0xFF6B7280),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Scrollable chart canvas (horizontal scroll, vertical scroll synced with Y-axis)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(horizontalScrollState)
                        .verticalScroll(verticalScrollState, enabled = true)
                ) {
                    Box(modifier = Modifier.widthIn(min = 400.dp)) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((labels.size * 50).dp)
                                .pointerInput(Unit) {
                                    detectTapGestures { tapOffset ->
                                        // Find the closest bar to the tap
                                        val touchRadius = 30.dp.toPx()
                                        var closestIndex: Int? = null
                                        var minDistance = Float.MAX_VALUE
                                        
                                        storedBarPositions.forEachIndexed { index, (offset, size) ->
                                            val barCenter = Offset(offset.x + size.width / 2, offset.y + size.height / 2)
                                            val distance = kotlin.math.sqrt(
                                                (tapOffset.x - barCenter.x) * (tapOffset.x - barCenter.x) +
                                                (tapOffset.y - barCenter.y) * (tapOffset.y - barCenter.y)
                                            )
                                            if (distance < touchRadius && distance < minDistance) {
                                                minDistance = distance
                                                closestIndex = index
                                            }
                                        }
                                        
                                        if (closestIndex != null) {
                                            selectedBarIndex = closestIndex
                                            val (offset, size) = storedBarPositions[closestIndex]
                                            tooltipOffset = Offset(offset.x + size.width, offset.y + size.height / 2)
                                        } else {
                                            selectedBarIndex = null
                                            tooltipOffset = null
                                        }
                                    }
                                }
                        ) {
                            if (labels.isEmpty()) return@Canvas
                            
                            val leftPadding = 0.dp.toPx()
                            val rightPadding = 20.dp.toPx()
                            val topPadding = 20.dp.toPx()
                            val bottomPadding = 0.dp.toPx()
                            val chartWidth = maxOf(size.width - leftPadding - rightPadding, 400.dp.toPx() - leftPadding - rightPadding)
                            val chartHeight = size.height - topPadding - bottomPadding
                            
                            val barHeight = (chartHeight / labels.size) * 0.6f
                            val spacing = (chartHeight / labels.size) * 0.4f
                            
                            // Draw grid lines (vertical lines for X-axis)
                            val gridColor = Color(0xFF94A3B8).copy(alpha = 0.25f)
                            for (i in 0..6) {
                                val x = leftPadding + (chartWidth / 6) * i
                                drawLine(
                                    color = gridColor,
                                    start = Offset(x, topPadding),
                                    end = Offset(x, topPadding + chartHeight),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            
                            // Draw horizontal bars
                            val barPositions = mutableListOf<Pair<Offset, Size>>()
                            labels.forEachIndexed { index, _ ->
                                val y = topPadding + (chartHeight / labels.size) * index + spacing / 2
                                val barWidth = if (displayMax > 0) {
                                    (values[index] / displayMax * chartWidth).toFloat()
                                } else {
                                    0f
                                }
                                
                                val isSelected = selectedBarIndex == index
                                
                                // Draw bar (green color)
                                val barOffset = Offset(leftPadding, y)
                                val barSize = Size(barWidth, barHeight)
                                barPositions.add(Pair(barOffset, barSize))
                                
                                drawRect(
                                    color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.8f) else Color(0xFF10B981),
                                    topLeft = barOffset,
                                    size = barSize
                                )
                            }
                            storedBarPositions = barPositions
                        }
                    }
                }
            }
            
            // Fixed X-axis at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 120.dp, end = 20.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0..6) {
                    val value = (displayMax / 6) * i
                    Text(
                        text = String.format("%.2f", value),
                        fontSize = 10.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
        
        // Interactive tooltip
        selectedBarIndex?.let { index ->
            tooltipOffset?.let { offset ->
                if (index < labels.size && index < values.size) {
                    val burnRate = values[index]
                    val density = LocalDensity.current
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Card(
                            modifier = Modifier
                                .offset(
                                    x = with(density) { offset.x.toDp() + 10.dp },
                                    y = with(density) { offset.y.toDp() - 20.dp }
                                )
                                .width(150.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = labels[index],
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${String.format("%.2f", burnRate)} Cr/day",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD1D5DB)
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
fun HorizontalBarChart(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier
) {
    // Calculate displayMax for consistent scaling
    val rawMaxValue = values.maxOrNull() ?: 0.0
    val displayMax = remember(rawMaxValue) {
        if (rawMaxValue > 0) {
            val paddedMax = rawMaxValue * 1.15
            val stepSize = when {
                paddedMax < 1000 -> 200.0
                paddedMax < 10000 -> 1000.0
                paddedMax < 100000 -> 10000.0
                paddedMax < 1000000 -> 100000.0
                paddedMax < 10000000 -> 1000000.0
                paddedMax < 100000000 -> 10000000.0
                else -> {
                    val logValue = floor(log10(paddedMax)).toInt()
                    var magnitude = 1.0
                    repeat(logValue) {
                        magnitude *= 10.0
                    }
                    magnitude * 2.0
                }
            }
            val numStepsNeeded = ceil(paddedMax / stepSize)
            val intervalsNeeded = ceil(numStepsNeeded / 4.0).toInt()
            val calculatedMax = intervalsNeeded * 4 * stepSize
            if (calculatedMax > rawMaxValue * 1.3) {
                val conservativeSteps = ceil(paddedMax / stepSize).toInt()
                val conservativeIntervals = maxOf(4, ((conservativeSteps + 3) / 4) * 4)
                conservativeIntervals * stepSize
            } else {
                calculatedMax
            }
        } else {
            1000.0
        }
    }
    
    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (labels.isEmpty()) return@Canvas
            
            val leftPadding = 120.dp.toPx() // Space for Y-axis category labels
            val rightPadding = 20.dp.toPx()
            val topPadding = 20.dp.toPx()
            val bottomPadding = 30.dp.toPx() // Space for X-axis labels
            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding
            
            val barHeight = (chartHeight / labels.size) * 0.6f
            val spacing = (chartHeight / labels.size) * 0.4f
            
            // Draw grid lines (vertical lines for X-axis)
            val gridColor = Color(0xFF94A3B8).copy(alpha = 0.25f)
            for (i in 0..4) {
                val x = leftPadding + (chartWidth / 4) * i
                drawLine(
                    color = gridColor,
                    start = Offset(x, topPadding),
                    end = Offset(x, topPadding + chartHeight),
                    strokeWidth = 1.dp.toPx()
                )
            }
            
            // Draw horizontal bars
            labels.forEachIndexed { index, _ ->
                val y = topPadding + (chartHeight / labels.size) * index + spacing / 2
                val barWidth = if (displayMax > 0) {
                    (values[index] / displayMax * chartWidth).toFloat()
                } else {
                    0f
                }
                
                drawRect(
                    color = Color(0xFF0EA5E9),
                    topLeft = Offset(leftPadding, y),
                    size = Size(barWidth, barHeight)
                )
            }
        }
        
        // Y-axis labels (category names on left) - aligned with bars
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .padding(start = 8.dp, top = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(110.dp)
                )
            }
        }
        
        // X-axis labels (amount spent at bottom)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 120.dp, end = 20.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0..4) {
                val value = (displayMax / 4) * i
                Text(
                    text = formatNumber(value),
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

data class ScatterPoint(
    val x: Float, // Progress %
    val y: Float, // Overrun %
    val label: String // Phase/Stage name
)

@Composable
fun ScatterChart(
    data: List<ScatterPoint>,
    modifier: Modifier = Modifier
) {
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipOffset by remember { mutableStateOf<Offset?>(null) }
    var storedPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    
    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        // Find the closest point to the tap
                        val touchRadius = 20.dp.toPx()
                        var closestIndex: Int? = null
                        var minDistance = Float.MAX_VALUE
                        
                        storedPoints.forEachIndexed { index, point ->
                            val distance = kotlin.math.sqrt(
                                (tapOffset.x - point.x) * (tapOffset.x - point.x) +
                                (tapOffset.y - point.y) * (tapOffset.y - point.y)
                            )
                            if (distance < touchRadius && distance < minDistance) {
                                minDistance = distance
                                closestIndex = index
                            }
                        }
                        
                        if (closestIndex != null) {
                            selectedPointIndex = closestIndex
                            val point = storedPoints[closestIndex]
                            tooltipOffset = Offset(point.x, point.y - 30.dp.toPx())
                        } else {
                            selectedPointIndex = null
                            tooltipOffset = null
                        }
                    }
                }
        ) {
            if (data.isEmpty()) return@Canvas
            
            val leftPadding = 40.dp.toPx() // Space for Y-axis labels
            val rightPadding = 20.dp.toPx()
            val topPadding = 20.dp.toPx()
            val bottomPadding = 30.dp.toPx() // Space for X-axis labels
            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding
            
            // X-axis: 0 to 100 (Progress %)
            val maxX = 100f
            // Y-axis: Calculate range from data (Overrun %)
            val maxY = data.maxOfOrNull { it.y }?.coerceAtLeast(15f) ?: 15f
            val minY = data.minOfOrNull { it.y }?.coerceAtMost(-5f) ?: -5f
            val yRange = maxY - minY
            
            // Draw grid lines (horizontal for Y-axis)
            val gridColor = Color(0xFF94A3B8).copy(alpha = 0.25f)
            for (i in 0..4) {
                val y = topPadding + (chartHeight / 4) * i
                drawLine(
                    color = gridColor,
                    start = Offset(leftPadding, y),
                    end = Offset(leftPadding + chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            
            // Draw vertical grid lines for X-axis (every 10%)
            for (i in 0..10) {
                val x = leftPadding + (chartWidth / 10) * i
                drawLine(
                    color = gridColor,
                    start = Offset(x, topPadding),
                    end = Offset(x, topPadding + chartHeight),
                    strokeWidth = 1.dp.toPx()
                )
            }
            
            // Calculate and store points for click detection
            val points = mutableListOf<Offset>()
            data.forEach { point ->
                val x = leftPadding + (point.x / maxX * chartWidth)
                val y = topPadding + chartHeight - ((point.y - minY) / yRange * chartHeight)
                points.add(Offset(x, y))
                
                // Draw point
                val isSelected = selectedPointIndex == points.size - 1
                if (isSelected) {
                    // Draw larger highlight circle for selected point
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                        radius = 12.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
                
                // Draw white background circle
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 6.dp.toPx() else 5.dp.toPx(),
                    center = Offset(x, y)
                )
                // Draw red circle
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = if (isSelected) 5.dp.toPx() else 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            storedPoints = points
        }
        
        // Y-axis labels (Cost Overrun %)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .padding(start = 4.dp, top = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val maxY = data.maxOfOrNull { it.y }?.coerceAtLeast(15f) ?: 15f
            val minY = data.minOfOrNull { it.y }?.coerceAtMost(-5f) ?: -5f
            val yRange = maxY - minY
            for (i in 4 downTo 0) {
                val value = minY + (yRange / 4) * i
                Text(
                    text = "${String.format("%.0f", value)}%",
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
        
        // X-axis labels (Stage Progress %)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 40.dp, end = 20.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0..10) {
                val value = (100 / 10) * i
                Text(
                    text = "${value}%",
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
        
        // Interactive tooltip
        selectedPointIndex?.let { index ->
            tooltipOffset?.let { offset ->
                if (index < data.size) {
                    val point = data[index]
                    val density = LocalDensity.current
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Card(
                            modifier = Modifier
                                .offset(
                                    x = with(density) { offset.x.toDp() - 100.dp },
                                    y = with(density) { offset.y.toDp() - 50.dp }
                                )
                                .width(200.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Stage: ${point.label} · Progress: ${String.format("%.0f", point.x)}% · Overrun: ${String.format("%.1f", point.y)}%",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    lineHeight = 16.sp
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
fun StackedBarChart(
    labels: List<String>,
    inProgress: List<Int>,
    handover: List<Int>,
    delayed: List<Int>,
    complete: List<Int>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (labels.isEmpty()) return@Canvas
            
            val padding = 20.dp.toPx()
            val chartWidth = size.width - (padding * 2)
            val chartHeight = size.height - (padding * 2)
            val maxValue = 100f
            
            val barWidth = (chartWidth / labels.size) * 0.6f
            val spacing = (chartWidth / labels.size) * 0.4f
            
            // Draw grid lines
            val gridColor = Color(0xFF94A3B8).copy(alpha = 0.25f)
            for (i in 0..4) {
                val y = padding + (chartHeight / 4) * i
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(padding + chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            
            labels.forEachIndexed { index, _ ->
                val x = padding + (chartWidth / labels.size) * index + spacing / 2
                var currentY = padding + chartHeight
                
                // Draw stacked segments
                val segments = listOf(
                    inProgress[index] to Color(0xFF3B82F6),
                    handover[index] to Color(0xFFFACC15),
                    delayed[index] to Color(0xFFF97316),
                    complete[index] to Color(0xFF9CA3AF)
                )
                
                segments.forEach { (value, color) ->
                    val height = (value / maxValue * chartHeight).toFloat()
                    currentY -= height
                    drawRect(
                        color = color,
                        topLeft = Offset(x, currentY),
                        size = Size(barWidth, height)
                    )
                }
            }
        }
        
        // Y-axis labels
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .padding(start = 4.dp, top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 4 downTo 0) {
                val value = (100 / 4) * i
                Text(
                    text = value.toString(),
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
        
        // X-axis labels
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 9.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 1
                )
            }
        }
        
        // Legend
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 30.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendItem(Color(0xFF3B82F6), "In Progress")
            LegendItem(Color(0xFFFACC15), "Handover")
            LegendItem(Color(0xFFF97316), "Delayed")
            LegendItem(Color(0xFF9CA3AF), "Complete")
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF6B7280)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    startDate: Date?,
    endDate: Date?,
    onStartDateSelected: (Date) -> Unit,
    onEndDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDate?.time
    )
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = endDate?.time
    )
    
    // Date formatter for display (e.g., "Jun 30, 2024")
    val dateFormatter = remember {
        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        color = Color(0xFFC6C6C8),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with Cancel, Title, and Done
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel button
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        fontSize = 17.sp,
                        color = Color(0xFF3B82F6), // Blue color
                        fontWeight = FontWeight.Normal
                    )
                }
                
                // Title
                Text(
                    text = "Date Range",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                
                // Done button
                TextButton(
                    onClick = {
                        // Update dates if pickers were used
                        startDatePickerState.selectedDateMillis?.let { millis ->
                            onStartDateSelected(Date(millis))
                        }
                        endDatePickerState.selectedDateMillis?.let { millis ->
                            onEndDateSelected(Date(millis))
                        }
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "Done",
                        fontSize = 17.sp,
                        color = Color(0xFF3B82F6), // Blue color
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // SELECT DATE RANGE section header
            Text(
                text = "SELECT DATE RANGE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280), // Gray
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Date fields container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Start Date field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartDatePicker = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Start Date",
                            fontSize = 16.sp,
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.Normal
                        )
                        
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF3F4F6) // Light gray pill background
                        ) {
                            Text(
                                text = if (startDate != null) {
                                    dateFormatter.format(startDate)
                                } else {
                                    "Select date"
                                },
                                fontSize = 14.sp,
                                color = Color(0xFF111827),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    
                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE5E7EB))
                    )
                    
                    // End Date field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEndDatePicker = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "End Date",
                            fontSize = 16.sp,
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.Normal
                        )
                        
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF3F4F6) // Light gray pill background
                        ) {
                            Text(
                                text = if (endDate != null) {
                                    dateFormatter.format(endDate)
                                } else {
                                    "Select date"
                                },
                                fontSize = 14.sp,
                                color = Color(0xFF111827),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Descriptive text
            Text(
                text = "Choose a date range to filter the reports data",
                fontSize = 14.sp,
                color = Color(0xFF6B7280), // Gray
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
    
    // Start Date Picker Dialog
    if (showStartDatePicker) {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDatePickerState.selectedDateMillis?.let { millis ->
                            onStartDateSelected(Date(millis))
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }
    
    // End Date Picker Dialog
    if (showEndDatePicker) {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        endDatePickerState.selectedDateMillis?.let { millis ->
                            onEndDateSelected(Date(millis))
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
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
            }
        )
    }
}
