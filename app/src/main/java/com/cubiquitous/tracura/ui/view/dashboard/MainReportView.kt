package com.cubiquitous.tracura.ui.view.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cubiquitous.tracura.viewmodel.MainReportViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainReportView(
    onNavigateBack: () -> Unit = {},
    viewModel: MainReportViewModel = hiltViewModel()
) {
    // ViewModel state
    val reportData by viewModel.reportData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableStateOf("cost") } // "cost" or "project"
    var selectedProject by remember { mutableStateOf("all") }
    var selectedStage by remember { mutableStateOf("all") }
    var selectedProjectStatus by remember { mutableStateOf("all") }
    
    // Initialize default date range (last 6 months)
    val defaultStartDate = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -6)
        cal.time
    }
    val defaultEndDate = remember {
        Calendar.getInstance().time
    }
    
    var startDate by remember { mutableStateOf<Date?>(defaultStartDate) }
    var endDate by remember { mutableStateOf<Date?>(defaultEndDate) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDate?.time ?: defaultStartDate.time
    )
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = endDate?.time ?: defaultEndDate.time
    )
    
    val dateFormatter = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    // Get dynamic data from ViewModel
    val months = viewModel.getMonths()
    val projects = reportData.projects
    val allStages = remember(reportData.phases) {
        reportData.phases.values.flatten().map { it.phaseName }.distinct().sorted()
    }
    
    // Get stages for selected project
    val projectStages = remember(selectedProject, reportData.phases) {
        if (selectedProject == "all") {
            allStages
        } else {
            reportData.phases[selectedProject]?.map { it.phaseName }?.sorted() ?: emptyList()
        }
    }

    // Calculate KPIs based on filters
    val filteredExpenses = remember(selectedProject, reportData.expenses) {
        if (selectedProject == "all") {
            reportData.expenses
        } else {
            reportData.expenses.filter { it.projectId == selectedProject }
        }
    }
    
    val totalBudget = if (selectedProject == "all") {
        reportData.totalBudget / 10000000.0 // Convert to Cr
    } else {
        projects.find { it.id == selectedProject }?.budget?.div(10000000.0) ?: 0.0
    }
    
    val totalSpent = filteredExpenses.sumOf { it.amount } / 10000000.0 // Convert to Cr
    
    val remaining = maxOf(0.0, totalBudget - totalSpent)
    
    // Get cost trend data
    val costTrendData = reportData.monthlyCostTrend
    
    // Update filters when date range changes
    LaunchedEffect(startDate, endDate) {
        if (startDate != null && endDate != null) {
            viewModel.filterByDateRange(startDate, endDate)
        }
    }
    
    // Update filters when project status changes
    LaunchedEffect(selectedProjectStatus) {
        viewModel.filterByProjectStatus(selectedProjectStatus)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            HeaderSection(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onNavigateBack = onNavigateBack
            )

            // Main Content
            if (isLoading && reportData.projects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
            // Filters
            FilterRow(
                selectedProject = selectedProject,
                selectedStage = selectedStage,
                selectedProjectStatus = selectedProjectStatus,
                startDate = startDate,
                endDate = endDate,
                projects = projects,
                stages = projectStages,
                onProjectChanged = { selectedProject = it },
                onStageChanged = { selectedStage = it },
                onProjectStatusChanged = { selectedProjectStatus = it },
                onStartDateClick = { showStartDatePicker = true },
                onEndDateClick = { showEndDatePicker = true }
            )
            
            // Date Pickers - Show start date picker first, then end date picker
            if (showStartDatePicker) {
                DatePickerDialog(
                    title = "Select Start Date",
                    datePickerState = startDatePickerState,
                    onDismiss = { showStartDatePicker = false },
                    onDateSelected = { date ->
                        startDate = date
                        startDatePickerState.selectedDateMillis = date?.time
                        showStartDatePicker = false
                        // Automatically show end date picker after start date is selected
                        if (endDate == null || (date != null && endDate != null && date.after(endDate))) {
                            // If end date is before start date, clear it
                            endDate = null
                        }
                        // Show end date picker
                        showEndDatePicker = true
                    }
                )
            }
            
            if (showEndDatePicker && !showStartDatePicker) {
                DatePickerDialog(
                    title = "Select End Date",
                    datePickerState = endDatePickerState,
                    onDismiss = { showEndDatePicker = false },
                    onDateSelected = { date ->
                        if (date != null && startDate != null && date.before(startDate)) {
                            // End date cannot be before start date
                            return@DatePickerDialog
                        }
                        endDate = date
                        endDatePickerState.selectedDateMillis = date?.time
                        showEndDatePicker = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // KPI Cards
            KPIRow(
                totalBudget = totalBudget,
                totalSpent = totalSpent + remaining // Total = Spent + Remaining
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            when (selectedTab) {
                "cost" -> CostInsightsTab(
                    selectedProject = selectedProject,
                    selectedStage = selectedStage,
                    selectedProjectStatus = selectedProjectStatus,
                    months = months,
                    costTrendData = costTrendData,
                    totalSpent = totalSpent,
                    reportData = reportData
                )
                "project" -> ProjectInsightsTab(
                    selectedProject = selectedProject,
                    months = months,
                    projects = projects
                )
            }
            }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
    ) {
        // Top bar with close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close button
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF4285F4).copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // Title and subtitle
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Tracura",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Portfolio · Cost & Project Insights",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.75f)
            )
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(0.dp))
        ) {
            TabButton(
                text = "COST INSIGHTS",
                isSelected = selectedTab == "cost",
                onClick = { onTabSelected("cost") },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "PROJECT INSIGHTS",
                isSelected = selectedTab == "project",
                onClick = { onTabSelected("project") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight(600) else FontWeight.Normal,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.56f)
                        .height(3.dp)
                        .background(Color(0xFF4285F4), CircleShape)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    selectedProject: String,
    selectedStage: String,
    selectedProjectStatus: String,
    startDate: Date?,
    endDate: Date?,
    projects: List<com.cubiquitous.tracura.model.Project>,
    stages: List<String>,
    onProjectChanged: (String) -> Unit,
    onStageChanged: (String) -> Unit,
    onProjectStatusChanged: (String) -> Unit,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val dateRangeText = if (startDate != null && endDate != null) {
        "${dateFormatter.format(startDate)} - ${dateFormatter.format(endDate)}"
    } else {
        "Select Date Range"
    }
    
    // Build project options dynamically
    val projectOptions = remember(projects) {
        listOf("All Projects") + projects.map { it.name }
    }
    val projectValues = remember(projects) {
        listOf("all") + projects.mapNotNull { it.id }
    }
    
    // Build stage options dynamically
    val stageOptions = remember(stages) {
        listOf("All Stages") + stages
    }
    val stageValues = remember(stages) {
        listOf("all") + stages
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Date Range Filter
        Column {
            Text(
                text = "DATE RANGE",
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(bottom = 3.dp)
            )
            OutlinedTextField(
                value = dateRangeText,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = "Calendar",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        // Show start date picker first, then end date picker
                        onStartDateClick() 
                    }
                    .clip(CircleShape),
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE5E7EB),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedTextColor = Color(0xFF111827),
                    unfocusedTextColor = Color(0xFF111827)
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
            )
        }
        
        // Other Filters Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterDropdown(
                label = "PROJECT STATUS",
                options = listOf("ALL Status", "ACTIVE", "COMPLETED", "MAINTENANCE", "ARCHIVE", "SUSPENDED"),
                values = listOf("all", "ACTIVE", "COMPLETED", "MAINTENANCE", "ARCHIVE", "SUSPENDED"),
                selectedValue = selectedProjectStatus,
                onValueChanged = onProjectStatusChanged,
                modifier = Modifier.weight(1f)
            )
            FilterDropdown(
                label = "PROJECT",
                options = projectOptions,
                values = projectValues,
                selectedValue = selectedProject,
                onValueChanged = onProjectChanged,
                modifier = Modifier.weight(1f)
            )
            FilterDropdown(
                label = "STA",
                options = stageOptions,
                values = stageValues,
                selectedValue = selectedStage,
                onValueChanged = onStageChanged,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    options: List<String>,
    values: List<String>,
    selectedValue: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedIndex = values.indexOf(selectedValue)
    val selectedOption = if (selectedIndex >= 0) options[selectedIndex] else options[0]

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(bottom = 3.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .clip(CircleShape),
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE5E7EB),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedTextColor = Color(0xFF111827),
                    unfocusedTextColor = Color(0xFF111827)
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White, RoundedCornerShape(8.dp))
            ) {
                options.forEachIndexed { index, option ->
                    val isSelected = values[index] == selectedValue
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 14.sp,
                                    color = Color(0xFF111827)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF4285F4),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onValueChanged(values[index])
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KPIRow(
    totalBudget: Double,
    totalSpent: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KPICard(
            label = "Total Budget",
            value = formatCr(totalBudget),
            modifier = Modifier.weight(1f)
        )
        KPICard(
            label = "Total",
            value = formatCr(totalSpent),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KPICard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF94A3B8).copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF6B7280)
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight(600),
                color = Color(0xFF111827)
            )
        }
    }
}

@Composable
private fun CostInsightsTab(
    selectedProject: String,
    selectedStage: String,
    selectedProjectStatus: String,
    months: List<String>,
    costTrendData: Map<String, List<Double>>,
    totalSpent: Double,
    reportData: com.cubiquitous.tracura.viewmodel.MainReportData
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cost Trend Chart
        ChartCard(
            title = "Cost Trend (MoM)",
            subtitle = "Monthly total cost · ₹",
            totalLabel = formatKValue(totalSpent * 10000000.0) // Convert Cr back to rupees for display
        ) {
            CostTrendChart(
                months = months,
                data = costTrendData[selectedProject] ?: costTrendData["all"] ?: emptyList(),
                modifier = Modifier.height(130.dp)
            )
        }

        // Stage Budget vs Actual
        ChartCard(
            title = "Stage Budget vs Actual",
            subtitle = "₹ Cr · Budget vs Actuals"
        ) {
            StageBudgetChart(
                selectedProject = selectedProject,
                selectedStage = selectedStage,
                stageBudgetData = reportData.stageBudgetData,
                modifier = Modifier.height(150.dp)
            )
        }

        // Project-wise Budget vs Actual - Only show when multiple projects available
        if (reportData.projects.size > 1) {
            ChartCard(
                title = "Project-wise Budget vs Actual",
                subtitle = "₹ · Budget vs Actuals"
            ) {
                ProjectWiseBudgetChart(
                    projects = reportData.projects,
                    projectBudgetData = reportData.projectBudgetData,
                    modifier = Modifier.height(150.dp)
                )
            }
        }

        // Projects at Selected Stage
        ChartCard(
            title = "Projects at Selected Stage",
            subtitle = "Budget vs Actual at this stage across projects"
        ) {
            if (selectedStage != "all") {
                StageAcrossProjectsChart(
                    selectedStage = selectedStage,
                    stageBudgetData = reportData.stageBudgetData,
                    projects = reportData.projects,
                    modifier = Modifier.height(150.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select a Stage above to compare projects.",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }

        // Two Column Layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ChartCard(
                title = "Sub-Category Spend",
                subtitle = "Filtered by Project · Department",
                modifier = Modifier.weight(1.1f)
            ) {
                SubCategorySpendChart(
                    selectedProject = selectedProject,
                    selectedProjectStatus = selectedProjectStatus,
                    modifier = Modifier.height(160.dp)
                )
            }
            ChartCard(
                title = "Cost by Project Status",
                subtitle = "₹ · Portfolio split",
                modifier = Modifier.weight(0.9f)
            ) {
                StatusCostChart(
                    projects = reportData.projects,
                    expenses = reportData.expenses,
                    modifier = Modifier.height(160.dp)
                )
            }
        }

        // Cost Overrun vs Stage Progress
        ChartCard(
            title = "Cost Overrun vs Stage Progress",
            subtitle = "Variance % vs Progress %"
        ) {
            OverrunScatterChart(
                selectedProject = selectedProject,
                modifier = Modifier.height(160.dp)
            )
        }

        // Burn Rate by Project
        ChartCard(
            title = "Burn Rate by Project",
            subtitle = "₹ Cr/day · last 30 days"
        ) {
            BurnRateChart(
                selectedProject = selectedProject,
                modifier = Modifier.height(160.dp)
            )
        }
    }
}

@Composable
private fun ProjectInsightsTab(
    selectedProject: String,
    months: List<String>,
    projects: List<com.cubiquitous.tracura.model.Project>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Active Projects Chart
        ChartCard(
            title = "Active Projects (MoM)",
            subtitle = "Count of active projects"
        ) {
            ActiveProjectsChart(
                months = months,
                selectedProject = selectedProject,
                modifier = Modifier.height(130.dp)
            )
        }

        // Stage Progress Status
        ChartCard(
            title = "Stage Progress Status",
            subtitle = "% share of stages"
        ) {
            StageProgressChart(
                modifier = Modifier.height(150.dp)
            )
        }

        // Sub-Category Activity
        ChartCard(
            title = "Sub-Category Activity",
            subtitle = "# of expenses · last 30 days"
        ) {
            SubCategoryActivityChart(
                modifier = Modifier.height(150.dp)
            )
        }

        // Delay Days vs Extra Cost
        ChartCard(
            title = "Delay Days vs Extra Cost",
            subtitle = "Project-level correlation"
        ) {
            DelayCorrelationChart(
                selectedProject = selectedProject,
                modifier = Modifier.height(160.dp)
            )
        }

        // Suspended Projects by Reason
        ChartCard(
            title = "Suspended Projects by Reason",
            subtitle = "Current FY"
        ) {
            SuspensionReasonChart(
                projects = projects,
                modifier = Modifier.height(160.dp)
            )
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    subtitle: String,
    totalLabel: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF94A3B8).copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp) // 8-16dp padding from edges
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight(600),
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                // Blue pill badge with value and fullscreen icon
                totalLabel?.let {
                    Row(
                        modifier = Modifier
                            .background(
                                Color(0xFF3B82F6),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = it,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // 16dp after subtitle
            content()
        }
    }
}

// Helper function to format values in "k" format (e.g., 23.10k)
private fun formatKValue(value: Double): String {
    return if (value >= 1000) {
        "₹${String.format("%.2f", value / 1000)}k"
    } else {
        "₹${String.format("%.2f", value)}"
    }
}

// Chart Components
@Composable
private fun CostTrendChart(
    months: List<String>,
    data: List<Double>, // Data is in Cr, need to convert to actual rupees for display
    modifier: Modifier = Modifier
) {
    // Convert Cr to actual rupees for calculation
    val dataInRupees = data.map { it * 10000000.0 }
    
    // Calculate max value - round up to next 5k increment (e.g., if max is 23.10k, show up to 25.00k)
    val maxValueInRupees = if (dataInRupees.isNotEmpty()) {
        val actualMax = dataInRupees.maxOrNull() ?: 0.0
        if (actualMax == 0.0) {
            25000.0 // Default to 25k
        } else {
            // Round up to next 5k increment
            val rounded = ((actualMax / 5000.0).toInt() + 1) * 5000.0
            // Ensure minimum of 25k for proper display
            maxOf(25000.0, rounded)
        }
    } else {
        25000.0
    }
    
    // Generate Y-axis labels dynamically (0, 5k, 10k, 15k, 20k, 25k or higher)
    val yAxisStep = maxValueInRupees / 5.0 // 5 intervals
    val yAxisLabels = remember(maxValueInRupees) {
        (0..5).map { i ->
            val value = i * yAxisStep
            if (value >= 1000) {
                "${String.format("%.2f", value / 1000)}k"
            } else {
                "0"
            }
        }
    }
    
    val minValue = 0.0
    val leftPadding = 45f // Space for Y-axis labels
    val rightPadding = 40f // Space for navigation button
    val topPadding = 10f
    val bottomPadding = 25f // Space for X-axis labels
    
    if (data.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data available",
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
        return
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val chartWidth = canvasWidth - leftPadding - rightPadding
            val chartHeight = canvasHeight - topPadding - bottomPadding

            // Draw grid lines (5 horizontal lines for 0, 5k, 10k, 15k, 20k, 25k)
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

            // Draw Y-axis labels dynamically
            val textPaint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
                color = android.graphics.Color.parseColor("#6B7280")
                textSize = 10.sp.toPx()
                isAntiAlias = true
            }
            
            for (i in 0..5) {
                val y = topPadding + (chartHeight / 5) * (5 - i)
                val label = yAxisLabels[i]
                val textBounds = android.graphics.Rect()
                textPaint.getTextBounds(label, 0, label.length, textBounds)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    leftPadding - textBounds.width() - 8f,
                    y + textBounds.height() / 2,
                    textPaint
                )
            }

            // Draw line and points
            val path = Path()
            val points = mutableListOf<Offset>()
            
            dataInRupees.forEachIndexed { index, value ->
                val divisor = if (data.size > 1) (data.size - 1).toFloat() else 1f
                val x = leftPadding + (chartWidth / divisor) * index
                val normalizedValue = (value / maxValueInRupees).toFloat().coerceIn(0f, 1f)
                val y = topPadding + chartHeight - (normalizedValue * chartHeight)
                val point = Offset(x, y)
                points.add(point)

                if (index == 0) {
                    path.moveTo(point.x, point.y)
                } else {
                    path.lineTo(point.x, point.y)
                }
            }

            // Fill area under line first
            val fillPath = Path()
            fillPath.moveTo(leftPadding, topPadding + chartHeight)
            points.forEach { point ->
                fillPath.lineTo(point.x, point.y)
            }
            fillPath.lineTo(leftPadding + chartWidth, topPadding + chartHeight)
            fillPath.close()
            drawPath(
                path = fillPath,
                color = Color(0xFF3B82F6).copy(alpha = 0.12f)
            )

            // Draw line on top
            drawPath(
                path = path,
                color = Color(0xFF3B82F6),
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw points - highlight last point with larger circle
            points.forEachIndexed { index, point ->
                val isLastPoint = index == points.size - 1
                val radius = if (isLastPoint) 5.dp.toPx() else 3.dp.toPx()
                
                // Draw shadow for last point
                if (isLastPoint) {
                    drawCircle(
                        color = Color(0xFF3B82F6).copy(alpha = 0.3f),
                        radius = radius + 2.dp.toPx(),
                        center = point
                    )
                }
                
                // Draw point
                drawCircle(
                    color = Color(0xFF3B82F6),
                    radius = radius,
                    center = point
                )
            }
        }

        // X-axis labels (months)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(start = leftPadding.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            months.forEach { month ->
                Text(
                    text = month,
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Gray circular navigation button on the right
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        ) {
            IconButton(
                onClick = { /* Handle navigation */ },
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFF3F4F6), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Navigate",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun StageBudgetChart(
    selectedProject: String,
    selectedStage: String,
    stageBudgetData: Map<String, Map<String, Pair<Double, Double>>>,
    modifier: Modifier = Modifier
) {
    val stageData = stageBudgetData[selectedProject] ?: emptyMap()

    val stages = if (selectedStage != "all") {
        listOf(selectedStage)
    } else {
        stageData.keys.toList().sorted()
    }

    val maxValue = if (stageData.values.isNotEmpty()) {
        maxOf(40.0, stageData.values.maxOfOrNull { maxOf(it.first, it.second) } ?: 40.0)
    } else {
        40.0
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 20f
            val chartWidth = canvasWidth - (padding * 2)
            val chartHeight = canvasHeight - (padding * 2)
            
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
            
            val barWidth = (chartWidth / stages.size) * 0.35f
            val spacing = (chartWidth / stages.size) * 0.15f

            stages.forEachIndexed { index, stage ->
                val data = stageData[stage] ?: Pair(0.0, 0.0)
                val x = padding + (chartWidth / stages.size) * index + spacing
                val budgetHeight = ((data.first / maxValue) * chartHeight).toFloat()
                val actualHeight = ((data.second / maxValue) * chartHeight).toFloat()

                // Budget bar (blue)
                drawRect(
                    color = Color(0xFF1D4ED8),
                    topLeft = Offset(x, padding + chartHeight - budgetHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, budgetHeight)
                )

                // Actual bar (green)
                drawRect(
                    color = Color(0xFF10B981),
                    topLeft = Offset(x + barWidth + 4.dp.toPx(), padding + chartHeight - actualHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, actualHeight)
                )
            }
        }
        
        // X-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            stages.forEach { stage ->
                Text(
                    text = stage.take(12),
                    fontSize = 9.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 1
                )
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(Color(0xFF1D4ED8), "Budget")
            LegendItem(Color(0xFF10B981), "Actual")
        }
    }
}

@Composable
private fun ProjectWiseBudgetChart(
    projects: List<com.cubiquitous.tracura.model.Project>,
    projectBudgetData: Map<String, Pair<Double, Double>>,
    modifier: Modifier = Modifier
) {
    // Prepare project data - use actual values (second value in pair)
    // projectBudgetData values are in Cr, convert to lakhs for display
    val projectData = remember(projects, projectBudgetData) {
        projects.mapNotNull { project ->
            val projectId = project.id ?: return@mapNotNull null
            val data = projectBudgetData[projectId] ?: Pair(0.0, 0.0)
            // Convert from Cr to lakhs: 1 Cr = 100 lakhs
            val valueInLakhs = data.second * 100.0
            project.name to valueInLakhs
        }
    }

    // Fixed Y-axis labels: 0, 1.00L, 2.00L, 2.82L (do not auto-scale)
    val yAxisLabels = listOf("0", "1.00L", "2.00L", "2.82L")
    val maxValueInLakhs = 2.82
    val displayMax = maxValueInLakhs

    if (projectData.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No project data available",
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
        return
    }

    val scrollState = rememberScrollState()
    val minBarWidth = 60.dp // Minimum width per bar
    val barSpacing = 16.dp // Spacing between bars
    val chartContentWidth = (minBarWidth + barSpacing) * projectData.size

    Box(modifier = modifier) {
        // Y-axis labels (left-aligned, fixed)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .padding(start = 12.dp, top = 20.dp, bottom = 30.dp),
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

        // Scrollable chart area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 50.dp, end = 12.dp, top = 12.dp, bottom = 30.dp)
        ) {
            Box(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .width(chartContentWidth)
                    .fillMaxHeight()
            ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val leftPadding = 0f
                        val rightPadding = 0f
                        val topPadding = 0f
                        val bottomPadding = 0f
                        val chartWidth = canvasWidth - leftPadding - rightPadding
                        val chartHeight = canvasHeight - topPadding - bottomPadding

                        // Draw Y-axis tick marks only (no grid lines) - 4 tick marks matching labels: 0, 1.00L, 2.00L, 2.82L
                        val tickColor = Color(0xFF94A3B8).copy(alpha = 0.5f)
                        val tickValues = listOf(0.0, 1.0, 2.0, 2.82) // Values in lakhs
                        tickValues.forEach { value ->
                            val normalizedY = (value / maxValueInLakhs).toFloat().coerceIn(0f, 1f)
                            val y = topPadding + chartHeight - (normalizedY * chartHeight)
                            // Small tick mark on the left
                            drawLine(
                                color = tickColor,
                                start = Offset(leftPadding, y),
                                end = Offset(leftPadding + 4f, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw bars - single bright blue color
                        val barWidth = minBarWidth.toPx()
                        val spacing = barSpacing.toPx()

                        projectData.forEachIndexed { index, (_, value) ->
                            val x = leftPadding + (barWidth + spacing) * index
                            val barHeight = ((value / displayMax).toFloat().coerceIn(0f, 1f) * chartHeight).coerceAtLeast(0f)

                            // Single blue bar
                            drawRect(
                                color = Color(0xFF167DFF), // Bright blue
                                topLeft = Offset(x, topPadding + chartHeight - barHeight),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                            )
                        }
                    }

                    // X-axis labels (slanted/angled if needed)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(barSpacing)
                    ) {
                        projectData.forEach { (projectName, _) ->
                            Box(
                                modifier = Modifier.width(minBarWidth),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Text(
                                    text = projectName,
                                    fontSize = 9.sp,
                                    color = Color(0xFF6B7280),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .rotate(-45f)
                                        .padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
private fun StageAcrossProjectsChart(
    selectedStage: String,
    stageBudgetData: Map<String, Map<String, Pair<Double, Double>>>,
    projects: List<com.cubiquitous.tracura.model.Project>,
    modifier: Modifier = Modifier
) {
    // Extract project data for the selected stage
    val projectData = remember(selectedStage, stageBudgetData, projects) {
        projects.mapNotNull { project ->
            val projectId = project.id ?: return@mapNotNull null
            val projectStages = stageBudgetData[projectId] ?: emptyMap()
            val stageData = projectStages[selectedStage] ?: return@mapNotNull null
            
            // Only include projects that have data for this stage
            if (stageData.first > 0 || stageData.second > 0) {
                Triple(project.name, stageData.first, stageData.second) // name, budget, actual
            } else {
                null
            }
        }
    }
    
    if (projectData.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No projects found with the selected stage.",
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
        return
    }
    
    val labels = projectData.map { it.first }
    val budgetData = projectData.map { it.second }
    val actualData = projectData.map { it.third }
    
    val maxValue = maxOf(
        40.0,
        max(budgetData.maxOrNull() ?: 0.0, actualData.maxOrNull() ?: 0.0)
    )
    
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val leftPadding = 50.dp.toPx() // Space for Y-axis labels
            val rightPadding = 20.dp.toPx()
            val topPadding = 20.dp.toPx()
            val bottomPadding = 60.dp.toPx() // Space for X-axis labels and legend
            val chartWidth = canvasWidth - leftPadding - rightPadding
            val chartHeight = canvasHeight - topPadding - bottomPadding
            
            // Draw grid lines
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
            
            val groupWidth = chartWidth / labels.size
            val barWidth = groupWidth * 0.35f
            val barGap = 4.dp.toPx() // Small gap between Budget and Actual bars
            
            labels.forEachIndexed { index, _ ->
                val groupStartX = leftPadding + groupWidth * index
                val groupCenterX = groupStartX + groupWidth / 2
                val totalBarsWidth = (barWidth * 2) + barGap
                val barsStartX = groupCenterX - totalBarsWidth / 2
                
                // Budget bar (blue)
                val budgetHeight = ((budgetData[index] / maxValue) * chartHeight).toFloat()
                drawRect(
                    color = Color(0xFF1D4ED8),
                    topLeft = Offset(barsStartX, topPadding + chartHeight - budgetHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, budgetHeight)
                )
                
                // Actual bar (green)
                val actualHeight = ((actualData[index] / maxValue) * chartHeight).toFloat()
                drawRect(
                    color = Color(0xFF10B981),
                    topLeft = Offset(barsStartX + barWidth + barGap, topPadding + chartHeight - actualHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, actualHeight)
                )
            }
        }
        
        // Y-axis labels (Budget values)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .padding(start = 4.dp, top = 20.dp, bottom = 60.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 4 downTo 0) {
                val value = (maxValue / 4) * i
                Text(
                    text = String.format("%.2f", value) + " Cr",
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
        
        // X-axis labels (Project names) and Legend below
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 50.dp, vertical = 4.dp)
        ) {
            // Project names
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = Color(0xFF6B7280),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Legend (Budget and Actual) - centered below project names
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                LegendItem(Color(0xFF1D4ED8), "Budget")
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(Color(0xFF10B981), "Actual")
            }
        }
    }
}

@Composable
private fun SubCategorySpendChart(
    selectedProject: String,
    selectedProjectStatus: String,
    modifier: Modifier = Modifier
) {
    val data = when (selectedProjectStatus) {
        "active" -> listOf("Civil Works" to 45.0, "Labour" to 28.0, "Steel" to 22.0)
        "completed" -> listOf("Finishing" to 15.0, "MEP" to 12.0, "Services" to 8.0)
        "maintenance" -> listOf("Maintenance" to 8.0, "Services" to 5.0)
        else -> listOf("Civil Works" to 58.0, "Labour" to 34.0, "Steel" to 28.0, "Cement" to 24.0)
    }

    val maxValue = data.maxOfOrNull { it.second } ?: 1.0

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 20f
            val chartWidth = canvasWidth - (padding * 2)
            val chartHeight = canvasHeight - (padding * 2)
            val barHeight = (chartHeight / data.size) * 0.7f
            val spacing = (chartHeight / data.size) * 0.3f

            data.forEachIndexed { index, (_, value) ->
                val y = padding + (chartHeight / data.size) * index + spacing
                val barWidth = ((value / maxValue) * chartWidth).toFloat()

                drawRect(
                    color = Color(0xFF0EA5E9),
                    topLeft = Offset(padding, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                )
            }
        }
    }
}

@Composable
private fun StatusCostChart(
    projects: List<com.cubiquitous.tracura.model.Project>,
    expenses: List<com.cubiquitous.tracura.model.Expense>,
    modifier: Modifier = Modifier
) {
    // Calculate cost by status dynamically
    val statusCostData = remember(projects, expenses) {
        val statusMap = mutableMapOf<String, Double>()
        
        // Helper function to determine the actual display status of a project
        fun getProjectDisplayStatus(project: com.cubiquitous.tracura.model.Project): String {
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
                else -> project.status.replaceFirstChar { it.uppercaseChar() }
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
                    expenses
                        .filter { expense ->
                            expense.projectId == projectId && 
                            expense.status == com.cubiquitous.tracura.model.ExpenseStatus.APPROVED
                        }
                        .sumOf { it.amount }
                } ?: 0.0
            }
            
            // Always add status, even if totalSpent is 0
            statusMap[displayStatus] = totalSpent
        }
        
        // Sort by status name for consistent display
        statusMap.toList().sortedBy { it.first }
    }
    
    // Convert data values to lakhs for display (1 lakh = 100,000 rupees)
    val dataInLakhs = remember(statusCostData) {
        statusCostData.map { (status, value) ->
            status to (value / 100000.0) // Convert rupees to lakhs
        }
    }
    
    val actualMax = dataInLakhs.maxOfOrNull { it.second } ?: 0.0
    // Round up to next 10L increment, minimum 30L, maximum 80L for display
    val maxValueInLakhs = when {
        actualMax <= 0 -> 30.0
        actualMax <= 30 -> 30.0
        actualMax <= 40 -> 40.0
        actualMax <= 50 -> 50.0
        actualMax <= 60 -> 60.0
        actualMax <= 70 -> 70.0
        actualMax <= 80 -> 80.0
        else -> ((actualMax / 10).toInt() + 1) * 10.0 // Round up to next 10L
    }
    
    // Generate Y-axis labels dynamically based on max value (in 10L increments)
    val yAxisLabels = remember(maxValueInLakhs) {
        val step = 10.0 // 10L intervals
        val labels = mutableListOf<String>()
        labels.add("0")
        var current = step
        while (current <= maxValueInLakhs) {
            labels.add(String.format("%.2fL", current))
            current += step
        }
        labels
    }
    
    val displayMax = maxValueInLakhs
    
    // Calculate tick values for grid lines (in 10L increments)
    val tickValues = remember(maxValueInLakhs) {
        val step = 10.0 // 10L intervals
        val values = mutableListOf(0.0)
        var current = step
        while (current <= maxValueInLakhs) {
            values.add(current)
            current += step
        }
        values
    }
    
    if (statusCostData.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No project data available",
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
        return
    }
    
    Box(modifier = modifier) {
        // Y-axis labels (left-aligned, moved further left)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .padding(start = 4.dp, top = 20.dp, bottom = 30.dp),
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
        
        // Chart area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 45.dp, end = 12.dp, top = 12.dp, bottom = 30.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val leftPadding = 0f
                val rightPadding = 0f
                val topPadding = 0f
                val bottomPadding = 0f
                val chartWidth = canvasWidth - leftPadding - rightPadding
                val chartHeight = canvasHeight - topPadding - bottomPadding
                
                // Draw Y-axis grid lines (in 10L increments)
                val gridColor = Color(0xFF94A3B8).copy(alpha = 0.25f)
                tickValues.forEach { value ->
                    val normalizedY = (value / maxValueInLakhs).toFloat().coerceIn(0f, 1f)
                    val y = topPadding + chartHeight - (normalizedY * chartHeight)
                    // Draw horizontal grid line
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPadding, y),
                        end = Offset(leftPadding + chartWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                // Draw bars - orange color
                val barWidth = (chartWidth / dataInLakhs.size) * 0.6f
                val spacing = (chartWidth / dataInLakhs.size) * 0.4f
                
                dataInLakhs.forEachIndexed { index, (_, value) ->
                    val x = leftPadding + (chartWidth / dataInLakhs.size) * index + spacing
                    val barHeight = ((value / displayMax).toFloat().coerceIn(0f, 1f) * chartHeight).coerceAtLeast(0f)
                    
                    // Orange bar
                    drawRect(
                        color = Color(0xFFF97316), // Orange
                        topLeft = Offset(x, topPadding + chartHeight - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                    )
                }
            }
            
            // X-axis labels (horizontal, not slanted)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dataInLakhs.forEach { (status, _) ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = status,
                            fontSize = 9.sp,
                            color = Color(0xFF6B7280),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverrunScatterChart(
    selectedProject: String,
    modifier: Modifier = Modifier
) {
    val data = when (selectedProject) {
        "aurum" -> listOf(
            Triple("Excavation", 30f, -3f),
            Triple("Sub-structure", 60f, 5f),
            Triple("Super-structure", 65f, 10f),
            Triple("Finishing", 35f, 4f)
        )
        "tracura-res" -> listOf(
            Triple("Excavation", 20f, 2f),
            Triple("Sub-structure", 45f, 6f),
            Triple("Super-structure", 55f, 9f)
        )
        "lotus" -> listOf(
            Triple("Excavation", 100f, 0f),
            Triple("Super-structure", 100f, -2f),
            Triple("Finishing", 100f, 1f)
        )
        else -> listOf(
            Triple("Excavation", 25f, -5f),
            Triple("Sub-structure", 55f, 8f),
            Triple("Super-structure", 70f, 12f),
            Triple("Finishing", 40f, 3f),
            Triple("External Works", 15f, 15f)
        )
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 30f
            val chartWidth = canvasWidth - (padding * 2)
            val chartHeight = canvasHeight - (padding * 2)

            // Draw grid
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

            // Draw scatter points
            data.forEach { (_, progress, overrun) ->
                val x = padding + (progress / 100f) * chartWidth
                val y = padding + chartHeight / 2 - (overrun / 20f) * (chartHeight / 2)

                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
private fun BurnRateChart(
    selectedProject: String,
    modifier: Modifier = Modifier
) {
    val data = when (selectedProject) {
        "aurum" -> listOf("Aurum Heights" to 0.58)
        "tracura-res" -> listOf("Tracura Residency" to 0.42)
        "lotus" -> listOf("Lotus Enclave" to 0.37)
        else -> listOf(
            "Aurum Heights" to 0.58,
            "Tracura Residency" to 0.42,
            "Lotus Enclave" to 0.37
        )
    }

    val maxValue = data.maxOfOrNull { it.second } ?: 1.0

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 20f
            val chartWidth = canvasWidth - (padding * 2)
            val chartHeight = canvasHeight - (padding * 2)
            val barHeight = (chartHeight / data.size) * 0.7f
            val spacing = (chartHeight / data.size) * 0.3f

            data.forEachIndexed { index, (_, value) ->
                val y = padding + (chartHeight / data.size) * index + spacing
                val barWidth = ((value / maxValue) * chartWidth).toFloat()

                drawRect(
                    color = Color(0xFF22C55E),
                    topLeft = Offset(padding, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                )
            }
        }
    }
}

@Composable
private fun ActiveProjectsChart(
    months: List<String>,
    selectedProject: String,
    modifier: Modifier = Modifier
) {
    val data = when (selectedProject) {
        "aurum", "tracura-res", "lotus" -> listOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        else -> listOf(4.0, 6.0, 7.0, 9.0, 10.0, 12.0)
    }

    CostTrendChart(months, data, modifier)
}

@Composable
private fun StageProgressChart(
    modifier: Modifier = Modifier
) {
    // Stacked bar chart implementation
    Box(modifier = modifier) {
        Text(
            text = "Stage Progress Chart",
            modifier = Modifier.align(Alignment.Center),
            fontSize = 12.sp,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
private fun SubCategoryActivityChart(
    modifier: Modifier = Modifier
) {
    val data = listOf("Labour" to 84.0, "MEP Works" to 52.0, "Equipment" to 31.0, "Finishes" to 40.0)
    SubCategorySpendChart("all", "all", modifier)
}

@Composable
private fun DelayCorrelationChart(
    selectedProject: String,
    modifier: Modifier = Modifier
) {
    val data = when (selectedProject) {
        "aurum" -> listOf(Triple("Aurum Heights", 18f, 1.8f))
        "tracura-res" -> listOf(Triple("Tracura Residency", 24f, 2.2f))
        "lotus" -> listOf(Triple("Lotus Enclave", 10f, 0.7f))
        else -> listOf(
            Triple("Aurum Heights", 18f, 1.8f),
            Triple("Tracura Residency", 24f, 2.2f),
            Triple("Lotus Enclave", 10f, 0.7f)
        )
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 30f
            val chartWidth = canvasWidth - (padding * 2)
            val chartHeight = canvasHeight - (padding * 2)

            val maxDelay = data.maxOfOrNull { it.second } ?: 30f
            val maxCost = data.maxOfOrNull { it.third } ?: 3f

            data.forEach { (_, delay, cost) ->
                val x = padding + (delay / maxDelay) * chartWidth
                val y = padding + chartHeight - ((cost / maxCost) * chartHeight)

                drawCircle(
                    color = Color(0xFF0EA5E9),
                    radius = 8.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
private fun SuspensionReasonChart(
    projects: List<com.cubiquitous.tracura.model.Project>,
    modifier: Modifier = Modifier
) {
    // Calculate suspension reasons from all projects
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
        reasonCountMap.map { (reason, count) -> reason to count.toDouble() }
            .sortedByDescending { it.second }
    }

    val maxValue = suspensionReasonData.maxOfOrNull { it.second } ?: 1.0

    Box(modifier = modifier) {
        if (suspensionReasonData.isEmpty()) {
            // Show empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No suspended projects",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        } else {
            // Labels on the left side
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(110.dp)
                    .padding(start = 8.dp, top = 10.dp)
            ) {
                suspensionReasonData.forEachIndexed { index, (reason, _) ->
                    val maxLines = 2
                    val truncatedReason = if (reason.length > 25) {
                        reason.take(22) + "..."
                    } else {
                        reason
                    }
                    
                    Text(
                        text = truncatedReason,
                        fontSize = 10.sp,
                        color = Color(0xFF111827),
                        maxLines = maxLines,
                        modifier = Modifier
                            .height((160.dp / suspensionReasonData.size) * 0.6f)
                            .padding(vertical = 2.dp)
                    )
                }
            }
            
            // Chart area with bars and count labels
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val leftPadding = 120f // Space for labels
                    val rightPadding = 50f // Space for count labels
                    val topPadding = 10f
                    val bottomPadding = 25f // Extra space for X-axis label
                    val chartWidth = canvasWidth - leftPadding - rightPadding
                    val chartHeight = canvasHeight - topPadding - bottomPadding
                    val barHeight = (chartHeight / suspensionReasonData.size) * 0.6f
                    val spacing = (chartHeight / suspensionReasonData.size) * 0.4f

                    suspensionReasonData.forEachIndexed { index, (reason, count) ->
                        val y = topPadding + (chartHeight / suspensionReasonData.size) * index + spacing
                        val barWidth = ((count / maxValue) * chartWidth).toFloat()

                        // Draw bar
                        drawRect(
                            color = Color(0xFFF97316), // Orange color
                            topLeft = Offset(leftPadding, y),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                        )

                        // Draw count label at the end of the bar
                        val countText = count.toInt().toString()
                        val textPaint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
                            color = android.graphics.Color.parseColor("#111827")
                            textSize = 10.sp.toPx()
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        val textBounds = android.graphics.Rect()
                        textPaint.getTextBounds(countText, 0, countText.length, textBounds)
                        val textX = leftPadding + barWidth + 5f
                        val textY = y + barHeight / 2 + textBounds.height() / 2
                        drawContext.canvas.nativeCanvas.drawText(
                            countText,
                            textX,
                            textY,
                            textPaint
                        )
                    }
                }
                
                // X-axis label "Count" at the bottom
                Text(
                    text = "Count",
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF6B7280)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    title: String,
    datePickerState: DatePickerState,
    onDismiss: () -> Unit,
    onDateSelected: (Date) -> Unit
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

private fun formatCr(value: Double): String {
    return "₹${String.format("%.1f", value)} Cr"
}

