package com.cubiquitous.tracura.ui.view.user

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.runtime.*
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.ExpenseStatus
import com.cubiquitous.tracura.model.PaymentMode
import com.cubiquitous.tracura.model.Phase
import com.cubiquitous.tracura.viewmodel.ExpenseViewModel
import com.cubiquitous.tracura.viewmodel.ProjectViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.utils.ImageUriHelper
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.isSystemInDarkTheme

@Immutable
private data class AllExpensesPalette(
    val primaryBlue: Color,
    val primaryBlueDark: Color,
    val primaryBlueLight: Color,
    val surfaceColor: Color,
    val cardSurface: Color,
    val fieldSurface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val statusApproved: Color,
    val statusComplete: Color,
    val statusPending: Color,
    val statusRejected: Color,
    val statusDefault: Color,
    val badgeWarning: Color
)

@Composable
private fun allExpensesPalette(): AllExpensesPalette {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        AllExpensesPalette(
            primaryBlue = Color(0xFF4285F4),
            primaryBlueDark = Color(0xFF8AB4F8),
            primaryBlueLight = Color(0xFF1F3A5F),
            surfaceColor = Color(0xFF000000),
            cardSurface = Color(0xFF1C1C1E),
            fieldSurface = Color(0xFF2C2C2E),
            onSurface = Color(0xFFFFFFFF),
            onSurfaceVariant = Color(0x99EBEBF5),
            outline = Color(0xFF38383A),
            statusApproved = Color(0xFF4285F4),
            statusComplete = Color(0xFF34C759),
            statusPending = Color(0xFFFF9F0A),
            statusRejected = Color(0xFFFF453A),
            statusDefault = Color(0x99EBEBF5),
            badgeWarning = Color(0xFFFF9F0A)
        )
    } else {
        AllExpensesPalette(
            primaryBlue = Color(0xFF1565C0),
            primaryBlueDark = Color(0xFF0D47A1),
            primaryBlueLight = Color(0xFFBBDEFB),
            surfaceColor = Color(0xFFF2F2F7),
            cardSurface = Color(0xFFFFFFFF),
            fieldSurface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF000000),
            onSurfaceVariant = Color(0x993C3C43),
            outline = Color(0xFFD1D1D6),
            statusApproved = Color(0xFF1565C0),
            statusComplete = Color(0xFF2E7D32),
            statusPending = Color(0xFFE65100),
            statusRejected = Color(0xFFC62828),
            statusDefault = Color(0xFF757575),
            badgeWarning = Color(0xFFFF6F00)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllExpensesScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    onNavigateToExpenseChat: (String) -> Unit = {},
    onNavigateToExpenseReview: (String) -> Unit = {},
    onEditExpense: (String) -> Unit = {},
    expenseViewModel: ExpenseViewModel = hiltViewModel(),
    projectViewModel: ProjectViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val palette = allExpensesPalette()
    val expenses by expenseViewModel.expenses.collectAsState()
    val isLoading by expenseViewModel.isLoading.collectAsState()
    val error by expenseViewModel.error.collectAsState()
    val selectedProject by expenseViewModel.selectedProject.collectAsState()
    val userNameMap by expenseViewModel.userNameMap.collectAsState()
    val businessHeadPhone by expenseViewModel.businessHeadPhone.collectAsState()
    
    val authState by authViewModel.authState.collectAsState()
    val userRole = authState.user?.role
    val userPhone = authState.user?.phone?.replace("+91", "")?.trim()
    
    val allowedDepartments = remember(userRole, userPhone, selectedProject) {
        if (userRole == UserRole.APPROVER && userPhone != null && selectedProject != null) {
            selectedProject?.departmentApproverAssignments
                ?.filter { (_, approverPhone) ->
                    approverPhone.replace("+91", "").trim() == userPhone
                }
                ?.keys?.toSet()
        } else {
            null // Show all for other roles
        }
    }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(projectId, userRole, userPhone) {
        println("🔍 AllExpensesScreen: Loading expenses for project: $projectId")
        expenseViewModel.loadProjectExpenses(projectId, userRole)
        projectViewModel.loadProjects()
        
        if (userPhone != null) {
            expenseViewModel.loadBusinessHeadPhone(userPhone)
        }
        
        // Load phases using ProjectViewModel
        scope.launch {
            try {
                // Wait for projects to load
                kotlinx.coroutines.delay(300)
                val project = projectViewModel.projects.value.find { it.id == projectId }
                if (project != null) {
                    expenseViewModel.setSelectedProject(project)
                    kotlinx.coroutines.delay(1000)
                } else {
                    kotlinx.coroutines.delay(500)
                    val retryProject = projectViewModel.projects.value.find { it.id == projectId }
                    if (retryProject != null) {
                        expenseViewModel.setSelectedProject(retryProject)
                        kotlinx.coroutines.delay(1000)
                    }
                }
            } catch (e: Exception) {
                println("⚠️ AllExpensesScreen: Error loading phases: ${e.message}")
            }
        }
        
        // Add a timeout to prevent infinite loading
        kotlinx.coroutines.delay(10000)
        if (isLoading) {
            println("⚠️ AllExpensesScreen: Loading timeout reached")
        }
    }
    
    // Get phases from ExpenseViewModel
    val phases by expenseViewModel.phases.collectAsState()
    val allPhasesState = remember(phases) { phases }
    
    // Debug logging for phases
    LaunchedEffect(allPhasesState.size) {
        println("🔍 AllExpensesScreen: Loaded ${allPhasesState.size} phases")
        allPhasesState.forEach { phase ->
            println("  - Phase: ${phase.id} -> ${phase.phaseName}")
        }
    }
    
    // Debug logging
    LaunchedEffect(expenses.size, isLoading, error) {
        println("🔍 AllExpensesScreen: expenses.size=${expenses.size}, isLoading=$isLoading, error=$error")
    }
    
    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    
    // Filter & Sort modal state
    var showFilterModal by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedDepartment by remember { mutableStateOf("All") }
    var selectedPhase by remember { mutableStateOf("All") }
    var selectedStatus by remember { mutableStateOf("All") }
    var dateRangeEnabled by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    // Get unique categories and phases from expenses
    val uniqueCategories = remember(expenses) {
        expenses.map { it.category }.distinct().filter { it.isNotEmpty() }.sorted()
    }
    // Derive unique phases from expense phaseId, resolved via allPhasesState
    val uniquePhases = remember(expenses, allPhasesState) {
        expenses.mapNotNull { expense ->
            expense.phaseId?.let { pid ->
                allPhasesState.find { it.id == pid }?.phaseName
            } ?: expense.phaseName
        }.distinct().filter { it.isNotEmpty() }.sorted()
    }
    
    // Count active filters for badge
    val activeFilterCount = remember(selectedCategory, selectedPhase, selectedStatus, dateRangeEnabled) {
        var count = 0
        if (selectedCategory != "All") count++
        if (selectedPhase != "All") count++
        if (selectedStatus != "All") count++
        if (dateRangeEnabled) count++
        count
    }
    
    // Filter expenses — FIXED: swapped Approver/non-Approver + phase matching by phaseId
    val filteredExpenses = remember(expenses, searchQuery, selectedFilter, selectedCategory, selectedDepartment, selectedPhase, selectedStatus, startDate, endDate, dateRangeEnabled, allowedDepartments, userRole, allPhasesState) {
        expenses.filter { expense ->
            // Role-based department filter (for Approvers)
            val isDepartmentAllowed = if (allowedDepartments != null) {
                val deptName = try {
                    FormatUtils.getDepartmentDisplayName(expense.department)
                } catch (e: Exception) {
                    expense.department
                }
                allowedDepartments.contains(deptName)
            } else {
                true
            }
            
            if (!isDepartmentAllowed) return@filter false
            
            // Search filter
            val matchesSearch = when (selectedFilter) {
                "Amount" -> {
                    val amountStr = FormatUtils.formatCurrency(expense.amount)
                    amountStr.contains(searchQuery, ignoreCase = true) ||
                    expense.amount.toString().contains(searchQuery, ignoreCase = true)
                }
                "SubmittedBy" -> {
                    val submitterName = userNameMap[expense.submittedBy] ?: expense.submittedBy
                    submitterName.contains(searchQuery, ignoreCase = true)
                }
                "Category" -> {
                    expense.item?.contains(searchQuery, ignoreCase = true) == true ||
                    expense.itemType?.contains(searchQuery, ignoreCase = true) == true
                }
                else -> { // "All"
                    val submitterName = userNameMap[expense.submittedBy] ?: expense.submittedBy
                    expense.description.contains(searchQuery, ignoreCase = true) ||
                    expense.department.contains(searchQuery, ignoreCase = true) ||
                            expense.item?.contains(searchQuery, ignoreCase = true) == true ||
                            expense.itemType?.contains(searchQuery, ignoreCase = true) == true ||
                    expense.vendor.contains(searchQuery, ignoreCase = true) ||
                    submitterName.contains(searchQuery, ignoreCase = true) ||
                    FormatUtils.formatCurrency(expense.amount).contains(searchQuery, ignoreCase = true) ||
                            expense.phaseName?.contains(searchQuery, ignoreCase = true) == true ||
                    expense.amount.toString().contains(searchQuery, ignoreCase = true)
                }
            }
            
            // Advanced filters
            val matchesCategory = selectedCategory == "All" || expense.category == selectedCategory
            
            // Phase filter for ALL roles — match by phaseId via allPhasesState lookup
            val matchesPhase = if (selectedPhase == "All") {
                true
            } else {
                val selectedPhaseId = allPhasesState.find { it.phaseName == selectedPhase }?.id
                if (selectedPhaseId != null) {
                    expense.phaseId == selectedPhaseId
                } else {
                    // Fallback: match by phaseName if phase list not loaded
                    expense.phaseName == selectedPhase
                }
            }
            
            val matchesStatus = selectedStatus == "All" || when (selectedStatus) {
                "Approved" -> expense.status == ExpenseStatus.APPROVED
                "Pending" -> expense.status == ExpenseStatus.PENDING
                "Rejected" -> expense.status == ExpenseStatus.REJECTED
                "Complete" -> expense.status == ExpenseStatus.COMPLETE
                else -> true
            }
            
            // Date range filter
            val matchesDateRange = if (dateRangeEnabled && (startDate != null || endDate != null)) {
                expense.getDateAsTimestamp()?.toDate()?.let { expenseDate ->
                    val afterStart = startDate == null || expenseDate >= startDate
                    val beforeEnd = endDate == null || expenseDate <= endDate
                    afterStart && beforeEnd
                } ?: true
            } else {
                true
            }
            
            (matchesSearch || searchQuery.isEmpty()) && matchesCategory && matchesPhase && matchesStatus && matchesDateRange
        }
    }
    
    Scaffold(
        topBar = {
            // ── Material Top App Bar ───────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = palette.primaryBlue,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    
                    Text(
                        text = "All Expenses",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Filter icon with active count badge
                    BadgedBox(
                        badge = {
                            if (activeFilterCount > 0) {
                                Badge(
                                    containerColor = palette.badgeWarning,
                                    contentColor = Color.White
                                ) {
                                    Text("$activeFilterCount")
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = { showFilterModal = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        },
        containerColor = palette.surfaceColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Search Bar ─────────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                color = palette.fieldSurface,
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 2.dp,
                tonalElevation = 1.dp
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = when (selectedFilter) {
                                "Amount" -> "Search by amount..."
                                "SubmittedBy" -> "Search by sender..."
                                "Category" -> "Search by category..."
                                else -> "Search expenses..."
                            },
                            fontSize = 14.sp,
                            color = palette.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = palette.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = palette.fieldSurface,
                        unfocusedContainerColor = palette.fieldSurface,
                        focusedTextColor = palette.onSurface,
                        unfocusedTextColor = palette.onSurface
                    ),
                    shape = RoundedCornerShape(28.dp),
                    singleLine = true
                )
            }
            
            // ── Filter Chips Row ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem(palette, "All", selectedFilter == "All") { selectedFilter = "All" }
                FilterChipItem(palette, "Amount", selectedFilter == "Amount") { selectedFilter = "Amount" }
                FilterChipItem(palette, "Sender", selectedFilter == "SubmittedBy") { selectedFilter = "SubmittedBy" }
                FilterChipItem(palette, "Category", selectedFilter == "Category") { selectedFilter = "Category" }
            }
            
            // ── Results count ──────────────────────────────────────────────
            if (!isLoading && error == null && expenses.isNotEmpty()) {
                Text(
                    text = "${filteredExpenses.size} of ${expenses.size} expenses",
                    fontSize = 12.sp,
                    color = palette.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            
            // ── Content ────────────────────────────────────────────────────
            if (error != null) {
                // Error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            tint = palette.statusRejected,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Error loading expenses",
                            fontSize = 18.sp,
                            color = palette.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = error ?: "Unknown error",
                            fontSize = 14.sp,
                            color = palette.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { expenseViewModel.loadProjectExpenses(projectId) },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primaryBlue),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
            } else if (isLoading) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = palette.primaryBlue,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Loading expenses...",
                            fontSize = 14.sp,
                            color = palette.onSurfaceVariant
                        )
                    }
                }
            } else if (expenses.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            tint = palette.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No expenses found",
                            fontSize = 18.sp,
                            color = palette.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "This project doesn't have any expenses yet.",
                            fontSize = 14.sp,
                            color = palette.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredExpenses.isEmpty() && searchQuery.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = palette.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No matching expenses",
                                        fontSize = 16.sp,
                                        color = palette.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Try a different search term or filter",
                                        fontSize = 13.sp,
                                        color = palette.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredExpenses, key = { it.id }) { expense ->
                            ExpenseRowAllExpenses(
                                expense = expense,
                                palette = palette,
                                allPhases = allPhasesState,
                                currentUserPhone = userPhone,
                                userNameMap = userNameMap,
                                businessHeadPhone = businessHeadPhone,
                                onNavigateToChat = { onNavigateToExpenseChat(expense.id) },
                                onNavigateToReview = { onNavigateToExpenseReview(expense.id) },
                                onEditExpense = { expenseId ->
                                    onEditExpense(expenseId)
                                }
                            )
                        }
                    }
                    
                    // Bottom spacer
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
        
        // Filter & Sort Modal
        if (showFilterModal) {
            FilterSortModal(
                palette = palette,
                selectedCategory = selectedCategory,
                selectedPhase = selectedPhase,
                selectedStatus = selectedStatus,
                dateRangeEnabled = dateRangeEnabled,
                startDate = startDate,
                endDate = endDate,
                uniqueCategories = uniqueCategories,
                uniquePhases = uniquePhases,
                onCategorySelected = { selectedCategory = it },
                onPhaseSelected = { selectedPhase = it },
                onStatusSelected = { selectedStatus = it },
                onDateRangeToggled = { dateRangeEnabled = it },
                onStartDateSelected = { startDate = it },
                onEndDateSelected = { endDate = it },
                onClearAllFilters = {
                    selectedCategory = "All"
                    selectedPhase = "All"
                    selectedStatus = "All"
                    dateRangeEnabled = false
                    startDate = null
                    endDate = null
                },
                onDismiss = { showFilterModal = false }
            )
        }
    }
}

// ── Filter Chip ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipItem(
    palette: AllExpensesPalette,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = palette.primaryBlue,
            selectedLabelColor = Color.White,
            containerColor = palette.fieldSurface,
            labelColor = palette.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = palette.outline,
            selectedBorderColor = palette.primaryBlue,
            enabled = true,
            selected = isSelected
        ),
        shape = RoundedCornerShape(20.dp)
    )
}

// ── Expense Row Card ───────────────────────────────────────────────────────────
@Composable
private fun ExpenseRowAllExpenses(
    expense: Expense,
    palette: AllExpensesPalette,
    allPhases: List<Phase> = emptyList(),
    currentUserPhone: String? = null,
    userNameMap: Map<String, String> = emptyMap(),
    businessHeadPhone: String? = null,
    onNavigateToChat: () -> Unit = {},
    onNavigateToReview: () -> Unit = {},
    onEditExpense: (String) -> Unit = {}
) {
    val statusColor = when (expense.status) {
        ExpenseStatus.APPROVED -> palette.statusApproved
        ExpenseStatus.COMPLETE -> palette.statusComplete
        ExpenseStatus.PENDING -> palette.statusPending
        ExpenseStatus.REJECTED -> palette.statusRejected
        else -> palette.statusDefault
    }
    
    val statusLabel = when (expense.status) {
        ExpenseStatus.APPROVED -> "Approved"
        ExpenseStatus.COMPLETE -> "Complete"
        ExpenseStatus.PENDING -> "Pending"
        ExpenseStatus.REJECTED -> "Rejected"
        else -> "Unknown"
    }
    
    val formattedDate = FormatUtils.formatDateLong(expense.date)
    val paymentMethod = when (expense.modeOfPayment) {
        PaymentMode.CASH -> "Cash"
        PaymentMode.UPI -> "UPI"
        PaymentMode.CHEQUE -> "Cheque"
        else -> PaymentMode.toString(expense.modeOfPayment)
    }

    val submitterName = remember(expense.submittedBy, userNameMap, businessHeadPhone) {
        val normalizedSubmittedBy = expense.submittedBy.replace("+91", "").trim()
        val normalizedBHPhone = businessHeadPhone?.replace("+91", "")?.trim()
        
        if (normalizedBHPhone != null && normalizedSubmittedBy == normalizedBHPhone) {
            "Business Head"
        } else if (expense.submittedBy.isBlank()) {
            "Unknown"
        } else {
            userNameMap[expense.submittedBy] ?: expense.submittedBy
        }
    }
    
    // Get phase name if available
    val phaseName = remember(expense.phaseId, allPhases) {
        if (expense.phaseId?.isNotEmpty() == true && allPhases.isNotEmpty()) {
            val foundPhase = allPhases.find { 
                it.id.trim() == expense.phaseId?.trim() 
            } ?: allPhases.find { 
                it.id.trim().equals(expense.phaseId?.trim() ?: "", ignoreCase = true)
            }
            foundPhase?.phaseName?.takeIf { it.isNotEmpty() } ?: expense.description.take(15).ifEmpty { "Phase" }
        } else {
            expense.description.take(15).ifEmpty { "Phase" }
        }
    }
    
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToReview() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── Top Row: Phase tag + Status chip ───────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phase tag
                Surface(
                    color = palette.primaryBlueLight.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = phaseName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.primaryBlueDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                
                // Status chip
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // ── Middle Row: Submitter + Amount ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = submitterName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Category / Item type
                    Text(
                        text = expense.itemType ?: expense.category.ifEmpty { "Uncategorized" },
                        fontSize = 13.sp,
                        color = palette.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Amount
                Text(
                    text = FormatUtils.formatCurrency(expense.amount),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // ── Divider ────────────────────────────────────────────────
            HorizontalDivider(color = palette.outline.copy(alpha = 0.45f), thickness = 0.5.dp)
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // ── Bottom Row: Date + Payment + Action icons ──────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Date + Payment method
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        fontSize = 12.sp,
                        color = palette.onSurfaceVariant
                    )
                    
                    if (paymentMethod.isNotEmpty()) {
                        Surface(
                            color = palette.fieldSurface,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = paymentMethod,
                                fontSize = 11.sp,
                                color = palette.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                // Right: Action icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment icon
                    if (expense.attachmentUrl.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                ImageUriHelper.openFirebaseStorageFile(
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
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = "Attachment",
                                modifier = Modifier.size(18.dp),
                                tint = palette.primaryBlue
                            )
                        }
                    }
                    
                    // Payment proof icon
                    if (expense.paymentProofUrl.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                ImageUriHelper.openFirebaseStorageFile(
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
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Payment Proof",
                                modifier = Modifier.size(18.dp),
                                tint = palette.statusComplete
                            )
                        }
                    }
                    
                    // Edit icon - only for pending + own expenses
                    if (expense.status == ExpenseStatus.PENDING || expense.status == ExpenseStatus.REJECTED) {
                        val isSubmittedByCurrentUser = if (currentUserPhone != null && expense.submittedBy.isNotEmpty()) {
                            expense.submittedBy.replace("+91", "").trim() == currentUserPhone
                        } else {
                            false
                        }
                        
                        if (isSubmittedByCurrentUser) {
                            IconButton(
                                onClick = { onEditExpense(expense.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    modifier = Modifier.size(18.dp),
                                    tint = palette.statusPending
                                )
                            }
                        }
                        
                        // Chat icon
                        IconButton(
                            onClick = { onNavigateToChat() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = "Chat",
                                modifier = Modifier.size(18.dp),
                                tint = palette.primaryBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Filter & Sort Modal (Material Bottom Sheet) ────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSortModal(
    palette: AllExpensesPalette,
    selectedCategory: String,
    selectedPhase: String,
    selectedStatus: String,
    dateRangeEnabled: Boolean,
    startDate: Date?,
    endDate: Date?,
    uniqueCategories: List<String>,
    uniquePhases: List<String>,
    onCategorySelected: (String) -> Unit,
    onPhaseSelected: (String) -> Unit,
    onStatusSelected: (String) -> Unit,
    onDateRangeToggled: (Boolean) -> Unit,
    onStartDateSelected: (Date) -> Unit,
    onEndDateSelected: (Date) -> Unit,
    onClearAllFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showPhaseDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDate?.time ?: System.currentTimeMillis()
    )
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = endDate?.time ?: System.currentTimeMillis()
    )
    
    val dateFormatter = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.cardSurface,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(palette.outline)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Title
            Text(
                text = "Filters",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = palette.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = TextAlign.Center
            )
            
            // ── Category Filter ────────────────────────────────────────
            FilterDropdownRow(
                palette = palette,
                label = "CATEGORY",
                selectedValue = selectedCategory,
                options = listOf("All") + uniqueCategories,
                showDropdown = showCategoryDropdown,
                onToggleDropdown = { showCategoryDropdown = !showCategoryDropdown },
                onOptionSelected = {
                    onCategorySelected(it)
                    showCategoryDropdown = false
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            // ── Phase filter for ALL roles ─────────────────────────
            FilterDropdownRow(
                palette = palette,
                label = "PHASE",
                selectedValue = selectedPhase,
                options = listOf("All") + uniquePhases,
                showDropdown = showPhaseDropdown,
                onToggleDropdown = { showPhaseDropdown = !showPhaseDropdown },
                onOptionSelected = {
                    onPhaseSelected(it)
                    showPhaseDropdown = false
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // ── Status Filter (with Complete option) ───────────────────
            FilterDropdownRow(
                palette = palette,
                label = "STATUS",
                selectedValue = selectedStatus,
                options = listOf("All", "Approved", "Pending", "Rejected", "Complete"),
                showDropdown = showStatusDropdown,
                onToggleDropdown = { showStatusDropdown = !showStatusDropdown },
                onOptionSelected = {
                    onStatusSelected(it)
                    showStatusDropdown = false
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // ── Date Range ─────────────────────────────────────────────
            Surface(
                color = palette.fieldSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date Range",
                            fontSize = 14.sp,
                            color = palette.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = dateRangeEnabled,
                            onCheckedChange = onDateRangeToggled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = palette.primaryBlue,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = palette.outline
                            )
                        )
                    }
                    
                    if (dateRangeEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Start Date
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showStartDatePicker = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "From",
                                fontSize = 14.sp,
                                color = palette.onSurfaceVariant
                            )
                            Surface(
                                color = palette.fieldSurface,
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 1.dp
                            ) {
                                Text(
                                    text = startDate?.let { dateFormatter.format(it) } ?: "Select date",
                                    fontSize = 13.sp,
                                    color = if (startDate != null) palette.onSurface else palette.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                        
                        // End Date
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEndDatePicker = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "To",
                                fontSize = 14.sp,
                                color = palette.onSurfaceVariant
                            )
                            Surface(
                                color = palette.fieldSurface,
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 1.dp
                            ) {
                                Text(
                                    text = endDate?.let { dateFormatter.format(it) } ?: "Select date",
                                    fontSize = 13.sp,
                                    color = if (endDate != null) palette.onSurface else palette.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // ── Apply Filters button ───────────────────────────────────
            Button(
                onClick = { onDismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryBlue
                ),
                shape = RoundedCornerShape(26.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "Apply Filters",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // ── Clear All Filters button ───────────────────────────────
            TextButton(
                onClick = {
                    onClearAllFilters()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                            text = "Clear All Filters",
                            fontSize = 15.sp,
                            color = palette.statusRejected,
                            fontWeight = FontWeight.Medium
                        )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // Date Pickers
    if (showStartDatePicker) {
        DatePickerDialog(
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
                    Text("OK", color = palette.primaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel", color = palette.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(
                state = startDatePickerState,
                showModeToggle = false
            )
        }
    }
    
    if (showEndDatePicker) {
        DatePickerDialog(
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
                    Text("OK", color = palette.primaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel", color = palette.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(
                state = endDatePickerState,
                showModeToggle = false
            )
        }
    }
}

// ── Filter Dropdown Row ────────────────────────────────────────────────────────
@Composable
private fun FilterDropdownRow(
    palette: AllExpensesPalette,
    label: String,
    selectedValue: String,
    options: List<String>,
    showDropdown: Boolean,
    onToggleDropdown: () -> Unit,
    onOptionSelected: (String) -> Unit
) {
    Column {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = palette.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleDropdown() },
            color = palette.fieldSurface,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedValue,
                    fontSize = 15.sp,
                    color = if (selectedValue == "All") palette.onSurfaceVariant else palette.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                Icon(
                    imageVector = if (showDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = palette.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Dropdown menu
        AnimatedVisibility(
            visible = showDropdown,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    options.forEach { option ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOptionSelected(option) },
                            color = if (option == selectedValue) palette.primaryBlueLight.copy(alpha = 0.28f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 14.sp,
                                    color = if (option == selectedValue) palette.primaryBlue else palette.onSurface,
                                    fontWeight = if (option == selectedValue) FontWeight.SemiBold else FontWeight.Normal
                                )
                                if (option == selectedValue) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = palette.primaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (option != options.last()) {
                            HorizontalDivider(
                                color = palette.outline.copy(alpha = 0.45f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
