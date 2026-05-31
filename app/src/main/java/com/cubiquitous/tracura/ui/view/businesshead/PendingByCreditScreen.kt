package com.cubiquitous.tracura.ui.view.businesshead

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.ExpenseStatus
import com.cubiquitous.tracura.model.PaymentMode
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.viewmodel.ApprovalViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.*
import java.text.SimpleDateFormat
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

data class VendorExpenseGroup(
    val vendorName: String,
    val expenses: List<Expense>,
    val totalAmount: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingByCreditScreen(
    projectId: String? = null,
    onNavigateBack: () -> Unit,
    onReviewExpense: (String, String?) -> Unit = { _, _ -> },
    onPaymentSuccess: () -> Unit = {},
    approvalViewModel: ApprovalViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val pendingExpensesFlow by approvalViewModel.pendingExpenses.collectAsState()
    val isLoadingState by approvalViewModel.isLoading.collectAsState()
    val errorState by approvalViewModel.error.collectAsState()
    
    // Pull to refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Local mutable state to ensure UI updates
    var pendingExpenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var selectedVendor by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Search and filter state
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortOrder by rememberSaveable { mutableStateOf("Total (High to Low)") }
    
    // Refresh function
    val refreshExpenses = {
        if (projectId != null) {
            approvalViewModel.loadPendingApprovalsForProject(projectId, forceRefresh = true, userRole = UserRole.BUSINESS_HEAD)
        } else {
            approvalViewModel.loadPendingApprovals(forceRefresh = true, userRole = UserRole.BUSINESS_HEAD)
        }
    }
    
    // Update local state when flow changes
    LaunchedEffect(pendingExpensesFlow) {
        pendingExpenses = pendingExpensesFlow
        Log.d("checkingIssue", "count of pending expense : ${pendingExpenses.size}")
        pendingExpenses.forEach { expense ->
            Log.d("checkingIssue", "Expense: id=${expense.id}, vendor='${expense.vendor}', byCredit=${expense.byCredit}, status=${expense.status}, projectId=${expense.projectId}")
        }
        // Clear selected vendor if expenses are updated and vendor no longer exists
        selectedVendor?.let { vendor ->
            val vendorExists = pendingExpenses
                .filter { it.byCredit == true && it.status == ExpenseStatus.APPROVED }
                .groupBy { it.vendor }
                .containsKey(vendor)
            if (!vendorExists) {
                selectedVendor = null
            }
        }
    }
    
    // Load expenses when screen opens
    LaunchedEffect(projectId) {
        if (projectId != null) {
            approvalViewModel.loadPendingApprovalsForProject(projectId, forceRefresh = true, userRole = UserRole.BUSINESS_HEAD)
        } else {
            approvalViewModel.loadPendingApprovals(forceRefresh = true, userRole = UserRole.BUSINESS_HEAD)
        }
    }
    
    // Filter and group expenses by vendor
    val vendorGroups = remember(pendingExpenses, projectId, searchQuery, sortOrder) {
        val filteredExpenses = pendingExpenses
            .filter { expense ->
                val matches = expense.byCredit == true &&
                        expense.status == ExpenseStatus.APPROVED &&
                        (projectId == null || expense.projectId == projectId) &&
                        expense.vendor.isNotEmpty()
                
                if (!matches && expense.byCredit == true && expense.status == ExpenseStatus.APPROVED) {
                    Log.d("checkingIssue", "Expense filtered out: vendor='${expense.vendor}', projectId=${expense.projectId}, expectedProjectId=$projectId")
                }
                matches
            }
        
        Log.d("checkingIssue", "Filtered expenses count: ${filteredExpenses.size}, vendorGroups will be: ${filteredExpenses.groupBy { it.vendor }.size}")
        
        val grouped = filteredExpenses
            .groupBy { it.vendor }
            .map { (vendorName, expenses) ->
                VendorExpenseGroup(
                    vendorName = vendorName,
                    expenses = expenses.sortedByDescending { it.createdAt.toDate().time },
                    totalAmount = expenses.sumOf { expense ->
                        if (expense.remainingBalance != null && expense.remainingBalance!! >= 1)
                            expense.remainingBalance!!
                        else
                            expense.amount
                    }
                )
            }
        
        // Apply search filter
        val searchFiltered = if (searchQuery.isNotBlank()) {
            grouped.filter { it.vendorName.contains(searchQuery, ignoreCase = true) }
        } else {
            grouped
        }
        
        // Apply sort order
        when (sortOrder) {
            "Total (High to Low)" -> searchFiltered.sortedByDescending { it.totalAmount }
            "Total (Low to High)" -> searchFiltered.sortedBy { it.totalAmount }
            "Vendor Name (A-Z)" -> searchFiltered.sortedBy { it.vendorName }
            "Vendor Name (Z-A)" -> searchFiltered.sortedByDescending { it.vendorName }
            "Expense Count (High to Low)" -> searchFiltered.sortedByDescending { it.expenses.size }
            "Expense Count (Low to High)" -> searchFiltered.sortedBy { it.expenses.size }
            else -> searchFiltered.sortedByDescending { it.totalAmount }
        }
    }
    
    // Show vendor detail screen if vendor is selected
    selectedVendor?.let { vendorName ->
        val vendorExpenses = vendorGroups.find { it.vendorName == vendorName }?.expenses ?: emptyList()
        
        VendorExpenseDetailScreen(
            vendorName = vendorName,
            expenses = vendorExpenses,
            onNavigateBack = { selectedVendor = null },
            onReviewExpense = onReviewExpense,
            onRefreshExpenses = refreshExpenses,
            onPaymentSuccess = onPaymentSuccess,
            projectId = projectId
        )
        return
    }
    
    // Main vendor list screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Pending by Credit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = colorScheme.onPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colorScheme.primary,
                navigationIconContentColor = colorScheme.onPrimary,
                titleContentColor = colorScheme.onPrimary
            )
        )
        
        // Search Bar - Material Design 3
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sort Filter Chips - Material Design 3
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Sort:",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    val sortOptions = listOf(
                        "Total (High to Low)",
                        "Total (Low to High)",
                        "Vendor Name (A-Z)",
                        "Vendor Name (Z-A)",
                        "Expense Count (High to Low)",
                        "Expense Count (Low to High)"
                    )

                    items(sortOptions) { option ->
                        FilterChip(
                            selected = sortOrder == option,
                            onClick = { sortOrder = option },
                            label = {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = if (sortOrder == option) {
                                {
                                    Icon(
                                        imageVector = when {
                                            option.contains("High to Low") -> Icons.Default.ArrowDownward
                                            option.contains("Low to High") -> Icons.Default.ArrowUpward
                                            else -> Icons.Default.Sort
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.primaryContainer,
                                selectedLabelColor = colorScheme.onPrimaryContainer,
                                containerColor = colorScheme.surfaceContainerHighest,
                                labelColor = colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Search TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Search by vendor name...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outlineVariant,
                        focusedContainerColor = colorScheme.surface,
                        unfocusedContainerColor = colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

            }
        }
        
        // Content
        when {
            isLoadingState -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = colorScheme.primary)
                        Text(
                            text = "Loading vendors...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            errorState != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.error
                        )
                        Text(
                            text = errorState ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            vendorGroups.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No pending credit expenses",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                val scope = rememberCoroutineScope()


                SwipeRefresh(
                    state = rememberSwipeRefreshState(isRefreshing = isRefreshing || isLoadingState),
                    onRefresh = {
                        isRefreshing = true
                        refreshExpenses()

                        // 2. Now you can use it here
                        scope.launch {
                            delay(1500)
                            isRefreshing = false
                        }
                    }
                ){
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(vendorGroups, key = { it.vendorName }) { vendorGroup ->
                            VendorCard(
                                vendorGroup = vendorGroup,
                                onClick = { selectedVendor = vendorGroup.vendorName },
                                colorScheme = colorScheme
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VendorCard(
    vendorGroup: VendorExpenseGroup,
    onClick: () -> Unit,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vendor Icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vendorGroup.vendorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${vendorGroup.expenses.size} expense${if (vendorGroup.expenses.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = FormatUtils.formatCurrency(vendorGroup.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "View details",
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorExpenseDetailScreen(
    vendorName: String,
    expenses: List<Expense>,
    onNavigateBack: () -> Unit,
    onReviewExpense: (String, String?) -> Unit,
    onUpdatePaymentClick: (List<Expense>) -> Unit = {},
    onRefreshExpenses: () -> Unit = {},
    onPaymentSuccess: () -> Unit = {},
    projectId: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Filter out COMPLETE expenses - they should not be shown
    val activeExpenses = remember(expenses) {
        expenses.filter { it.status != ExpenseStatus.COMPLETE }
    }
    
    // Selection mode state
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    
    // Selection state for multiple expenses
    var selectedExpenseIds by remember { mutableStateOf(setOf<String>()) }
    
    // Search state
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    // Date filter state
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var dateFilterEnabled by rememberSaveable { mutableStateOf(false) }
    
    // Sort state
    var sortOrder by rememberSaveable { mutableStateOf("Date (Newest First)") }
    
    // Payment method selection state
    var showPaymentMethodDialog by remember { mutableStateOf(false) }
    var selectedPaymentMode by remember { mutableStateOf<PaymentMode?>(null) }
    var showPaymentProofDialog by remember { mutableStateOf(false) }
    var showCashConfirmationDialog by remember { mutableStateOf(false) }
    var showPaymentProofConfirmationDialog by remember { mutableStateOf(false) }
    var pendingCashPaymentMode by remember { mutableStateOf<PaymentMode?>(null) }
    var pendingPaymentProofMode by remember { mutableStateOf<PaymentMode?>(null) }
    var pendingPaymentProofUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPaymentProofName by remember { mutableStateOf<String?>(null) }
    var selectedPaymentProofUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPaymentProofName by remember { mutableStateOf<String?>(null) }
    var isUploadingPaymentProof by remember { mutableStateOf(false) }
    var isProcessingPaymentProof by remember { mutableStateOf(false) }
    var waitingForPaymentProof by remember { mutableStateOf(false) }
    
    // Partial payment state
    var showPartialPaymentDialog by remember { mutableStateOf(false) }
    var partialPaymentAmount by remember { mutableStateOf("") }
    var partialPaymentError by remember { mutableStateOf<String?>(null) }
    var showPartialPaymentSelectionDialog by remember { mutableStateOf(false) }
    var suggestedExpensesHigher by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var suggestedExpensesLower by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var suggestedAmountHigher by remember { mutableStateOf(0.0) }
    var suggestedAmountLower by remember { mutableStateOf(0.0) }
    
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val approvalViewModel: ApprovalViewModel = hiltViewModel()
    val isProcessing by approvalViewModel.isProcessing.collectAsState()
    val isLoading by approvalViewModel.isLoading.collectAsState()
    
    // Pull to refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Camera temp URI for payment proof
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }
    
    // Date picker states
    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDate?.time
    )
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = endDate?.time
    )
    
    // File picker launchers for payment proof
    val paymentProofCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraTempUri != null) {
            pendingPaymentProofUri = cameraTempUri
            pendingPaymentProofName = "payment_proof_camera_${System.currentTimeMillis()}.jpg"
            cameraTempUri = null
            // Show confirmation dialog
            if (selectedPaymentMode != null) {
                pendingPaymentProofMode = selectedPaymentMode
                showPaymentProofConfirmationDialog = true
            }
        }
    }
    
    val paymentProofPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingPaymentProofUri = it
            pendingPaymentProofName = it.toString().substringAfterLast("/").substringBefore("?")
                .takeIf { name -> name.isNotEmpty() && name.contains(".") }
                ?: "payment_proof_${System.currentTimeMillis()}.jpg"
            // Show confirmation dialog
            if (selectedPaymentMode != null) {
                pendingPaymentProofMode = selectedPaymentMode
                showPaymentProofConfirmationDialog = true
            }
        }
    }
    
    val paymentProofPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingPaymentProofUri = it
            pendingPaymentProofName = it.toString().substringAfterLast("/").substringBefore("?")
                .takeIf { name -> name.isNotEmpty() && name.contains(".") }
                ?: "payment_proof_${System.currentTimeMillis()}.pdf"
            // Show confirmation dialog
            if (selectedPaymentMode != null) {
                pendingPaymentProofMode = selectedPaymentMode
                showPaymentProofConfirmationDialog = true
            }
        }
    }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val tempUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                ContentValues()
            )
            cameraTempUri = tempUri
            tempUri?.let { paymentProofCameraLauncher.launch(it) }
        }
    }
    
    // Filter and sort expenses (use activeExpenses instead of expenses)
    val filteredAndSortedExpenses = remember(activeExpenses, searchQuery, startDate, endDate, dateFilterEnabled, sortOrder) {
        var filtered = activeExpenses
        
        // Apply search filter
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { expense ->
                val amountStr = FormatUtils.formatCurrency(expense.amount).lowercase()
                val departmentStr = FormatUtils.getDepartmentDisplayName(expense.department).lowercase()
                val descriptionStr = expense.description.lowercase()
                val reviewCommentsStr = expense.reviewComments.lowercase()
                val queryLower = searchQuery.lowercase()
                
                amountStr.contains(queryLower) ||
                departmentStr.contains(queryLower) ||
                descriptionStr.contains(queryLower) ||
                reviewCommentsStr.contains(queryLower)
            }
        }
        
        // Apply date filter
        if (dateFilterEnabled && (startDate != null || endDate != null)) {
            filtered = filtered.filter { expense ->
                val parsedDate: Date? = if (expense.date.isNotEmpty()) {
                    try {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(expense.date)
                    } catch (e: Exception) {
                        null
                    }
                } else null
                val expenseDate: Date = parsedDate ?: expense.createdAt.toDate()
                
                val afterStart = startDate == null || !expenseDate.before(startDate)
                val beforeEnd = endDate == null || !expenseDate.after(endDate)
                afterStart && beforeEnd
            }
        }
        
        // Apply sort
        filtered = when (sortOrder) {
            "Date (Newest First)" -> filtered.sortedByDescending { expense ->
                if (expense.date.isNotEmpty()) {
                    try {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(expense.date)?.time
                    } catch (e: Exception) {
                        null
                    }
                } else null
                    ?: expense.createdAt.toDate().time
            }
            "Date (Oldest First)" -> filtered.sortedBy { expense ->
                if (expense.date.isNotEmpty()) {
                    try {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(expense.date)?.time
                    } catch (e: Exception) {
                        null
                    }
                } else null
                    ?: expense.createdAt.toDate().time
            }
            "Amount (High to Low)" -> filtered.sortedByDescending { it.amount }
            "Amount (Low to High)" -> filtered.sortedBy { it.amount }
            else -> filtered
        }
        
        filtered
    }
    
    // Calculate total using remainingBalance if available and >= 1, otherwise use amount
    val totalAmount = filteredAndSortedExpenses.sumOf { expense ->
        if (expense.remainingBalance != null && expense.remainingBalance!! >= 1) {
            expense.remainingBalance!!
        } else {
            expense.amount
        }
    }
    
    val selectedExpenses = remember(selectedExpenseIds, filteredAndSortedExpenses) {
        filteredAndSortedExpenses.filter { it.id in selectedExpenseIds }
    }
    val selectedTotalAmount = selectedExpenses.sumOf { expense ->
        if (expense.remainingBalance != null && expense.remainingBalance!! >= 1) {
            expense.remainingBalance!!
        } else {
            expense.amount
        }
    }
    val hasSelection = selectedExpenseIds.isNotEmpty()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = if (isSelectionMode) {
                            "${selectedExpenseIds.size} selected"
                        } else {
                            vendorName
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onPrimary
                    )
                    Text(
                        text = if (isSelectionMode) {
                            FormatUtils.formatCurrency(selectedTotalAmount)
                        } else {
                            FormatUtils.formatCurrency(totalAmount)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            },
            navigationIcon = {
                if (isSelectionMode) {
                    IconButton(
                        onClick = {
                            isSelectionMode = false
                            selectedExpenseIds = emptySet()
                        }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel selection",
                            tint = colorScheme.onPrimary
                        )
                    }
                } else {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onPrimary
                        )
                    }
                }
            },
            actions = {
                if (isSelectionMode) {
                    // Select All button
                    IconButton(
                        onClick = {
                            selectedExpenseIds = filteredAndSortedExpenses.map { it.id }.toSet()
                        }
                    ) {
                        Icon(
                            Icons.Default.SelectAll,
                            contentDescription = "Select all",
                            tint = colorScheme.onPrimary
                        )
                    }
                    // Deselect All button
                    IconButton(
                        onClick = {
                            selectedExpenseIds = emptySet()
                        }
                    ) {
                        Icon(
                            Icons.Default.Deselect,
                            contentDescription = "Deselect all",
                            tint = colorScheme.onPrimary
                        )
                    }
                } else {
                    // Select menu - enter selection mode
                    TextButton(
                        onClick = { isSelectionMode = true }
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Select",
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colorScheme.primary,
                navigationIconContentColor = colorScheme.onPrimary,
                titleContentColor = colorScheme.onPrimary,
                actionIconContentColor = colorScheme.onPrimary
            )
        )
        
        // Search and Filter Bar - Material Design 3
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Search by amount, phase, description, review comments...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outlineVariant,
                        focusedContainerColor = colorScheme.surface,
                        unfocusedContainerColor = colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                
                // Filter and Sort Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date Filter Button
                    FilterChip(
                        selected = dateFilterEnabled,
                        onClick = {
                            if (!dateFilterEnabled) {
                                dateFilterEnabled = true
                            } else {
                                showStartDatePicker = true
                            }
                        },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = when {
                                        startDate != null && endDate != null -> 
                                            "${dateFormatter.format(startDate!!)} - ${dateFormatter.format(endDate!!)}"
                                        startDate != null -> 
                                            "From ${dateFormatter.format(startDate!!)}"
                                        endDate != null -> 
                                            "Until ${dateFormatter.format(endDate!!)}"
                                        else -> "Date Range"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        },
                        leadingIcon = if (dateFilterEnabled && (startDate != null || endDate != null)) {
                            {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colorScheme.primaryContainer,
                            selectedLabelColor = colorScheme.onPrimaryContainer,
                            containerColor = colorScheme.surfaceContainerHighest,
                            labelColor = colorScheme.onSurfaceVariant
                        )
                    )
                    
                    // Clear Date Filter Button
                    if (dateFilterEnabled && (startDate != null || endDate != null)) {
                        TextButton(
                            onClick = {
                                startDate = null
                                endDate = null
                                dateFilterEnabled = false
                            },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Sort Label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Sort:",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Sort Dropdown
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = false,
                            onClick = { showSortMenu = true },
                            label = {
                                Text(
                                    text = sortOrder,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = colorScheme.surfaceContainerHighest,
                                labelColor = colorScheme.onSurfaceVariant
                            )
                        )
                        
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            listOf(
                                "Date (Newest First)",
                                "Date (Oldest First)",
                                "Amount (High to Low)",
                                "Amount (Low to High)"
                            ).forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        sortOrder = option
                                        showSortMenu = false
                                    },
                                    leadingIcon = if (sortOrder == option) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Expenses List
        if (filteredAndSortedExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank() || dateFilterEnabled) 
                            "No expenses match your filters" 
                        else 
                            "No expenses found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                SwipeRefresh(
                    state = rememberSwipeRefreshState(isRefreshing = isRefreshing || isLoading),
                    onRefresh = {
                        isRefreshing = true
                        onRefreshExpenses()
                        scope.launch {
                            delay(1500) // Wait for refresh to complete
                            isRefreshing = false
                        }
                    }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = when {
                                isSelectionMode && hasSelection -> 88.dp
                                !isSelectionMode && filteredAndSortedExpenses.isNotEmpty() -> 88.dp
                                else -> 16.dp
                            }
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${filteredAndSortedExpenses.size} expense${if (filteredAndSortedExpenses.size != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                                if (isSelectionMode && hasSelection) {
                                    Text(
                                        text = "${selectedExpenseIds.size} selected",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        
                        items(filteredAndSortedExpenses, key = { it.id }) { expense ->
                            val isSelected = expense.id in selectedExpenseIds
                            ExpenseCard(
                                expense = expense,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onSelectionChange = if (isSelectionMode) {
                                    {
                                        selectedExpenseIds = if (isSelected) {
                                            selectedExpenseIds - expense.id
                                        } else {
                                            selectedExpenseIds + expense.id
                                        }
                                    }
                                } else null,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedExpenseIds = if (isSelected) {
                                            selectedExpenseIds - expense.id
                                        } else {
                                            selectedExpenseIds + expense.id
                                        }
                                    } else {
                                        onReviewExpense(expense.id, "BUSINESS_HEAD")
                                    }
                                },
                                colorScheme = colorScheme
                            )
                        }
                    }
                }
                
                // Fixed bottom bar - Pay amount / Pay partially (visible when NOT in selection mode and has expenses)
                if (!isSelectionMode && filteredAndSortedExpenses.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pay amount - green (full total)
                            Button(
                                onClick = {
                                    selectedPaymentMode = null
                                    selectedPaymentProofUri = null
                                    selectedPaymentProofName = null
                                    showPaymentMethodDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32),
                                    contentColor = colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 4.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Pay full amount",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = FormatUtils.formatCurrency(totalAmount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            // Pay partially - blue
                            Button(
                                onClick = {
                                    partialPaymentAmount = ""
                                    partialPaymentError = null
                                    showPartialPaymentDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1976D2),
                                    contentColor = colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 4.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Pay partially",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Select items",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Fixed bottom bar - Update payment (visible when in selection mode and at least one expense selected)
                if (isSelectionMode && hasSelection) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Update payment",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(selectedTotalAmount),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                )
                            }
                            Button(
                                onClick = {
                                    selectedPaymentMode = null
                                    selectedPaymentProofUri = null
                                    selectedPaymentProofName = null
                                    showPaymentMethodDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primary,
                                    contentColor = colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 4.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payment,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Update payment",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
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
                            startDate = Date(millis)
                            // If end date is before start date, clear it
                            if (endDate != null && startDate!!.after(endDate)) {
                                endDate = null
                            }
                            showStartDatePicker = false
                            // Show end date picker after start date is selected
                            showEndDatePicker = true
                        }
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
            DatePicker(
                state = startDatePickerState,
                modifier = Modifier.padding(16.dp),
                title = {
                    Text(
                        text = "Select Start Date",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = colorScheme.primary,
                    todayDateBorderColor = colorScheme.primary
                )
            )
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
                            val selectedDate = Date(millis)
                            // Validate that end date is not before start date
                            if (startDate == null || !selectedDate.before(startDate)) {
                                endDate = selectedDate
                                showEndDatePicker = false
                            }
                        }
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
            DatePicker(
                state = endDatePickerState,
                modifier = Modifier.padding(16.dp),
                title = {
                    Text(
                        text = "Select End Date",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = colorScheme.primary,
                    todayDateBorderColor = colorScheme.primary
                )
            )
        }
    }
    
    // Payment Method Selection Bottom Sheet
    if (showPaymentMethodDialog) {
        PaymentMethodSelectionBottomSheet(
            onDismiss = { showPaymentMethodDialog = false },
            onPaymentModeSelected = { mode ->
                selectedPaymentMode = mode
                showPaymentMethodDialog = false
                // If payment mode requires proof (UPI or Credit Card), show proof selection dialog
                if (mode == PaymentMode.UPI || mode == PaymentMode.CARD) {
                    waitingForPaymentProof = true
                    showPaymentProofDialog = true
                } else {
                    // For cash, show confirmation dialog
                    pendingCashPaymentMode = mode
                    showCashConfirmationDialog = true
                }
            }
        )
    }
    
    // Cash Confirmation Dialog
    if (showCashConfirmationDialog && pendingCashPaymentMode != null) {
        val expensesToUpdate = if (isSelectionMode && hasSelection) {
            filteredAndSortedExpenses.filter { it.id in selectedExpenseIds }
        } else {
            filteredAndSortedExpenses
        }
        val totalAmountToPay = expensesToUpdate.sumOf { expense ->
            if (expense.remainingBalance != null && expense.remainingBalance!! >= 1) {
                expense.remainingBalance!!
            } else {
                expense.amount
            }
        }

        AlertDialog(
            onDismissRequest = {
                showCashConfirmationDialog = false
                pendingCashPaymentMode = null
            },
            title = {
                Text(
                    text = "Confirm Payment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Payment Method: By Cash",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Total Amount: ${FormatUtils.formatCurrency(totalAmountToPay)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Number of Expenses: ${expensesToUpdate.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will mark ${expensesToUpdate.size} expense(s) as completed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cashMode = pendingCashPaymentMode ?: return@Button
                        val expensesSnapshot = expensesToUpdate.toList()
                        showCashConfirmationDialog = false
                        pendingCashPaymentMode = null
                        scope.launch {
                            approvalViewModel.bulkUpdateExpensesPayment(
                                expenses = expensesSnapshot,
                                paymentMode = cashMode,
                                paymentProofURL = null,
                                paymentProofName = null
                            )
                            // Clear selection mode after update
                            if (isSelectionMode) {
                                isSelectionMode = false
                                selectedExpenseIds = emptySet()
                            }
                            // Refresh expenses after payment
                            delay(500)
                            onRefreshExpenses()
                            onPaymentSuccess()
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary
                    )
                ) {
                    if (isProcessing) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Text("Processing...")
                        }
                    } else {
                        Text("Confirm & Submit")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCashConfirmationDialog = false
                        pendingCashPaymentMode = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    // Payment Proof Selection Dialog
    if (showPaymentProofDialog && selectedPaymentMode != null && waitingForPaymentProof) {
        PaymentProofSelectionDialog(
            onCameraSelected = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                
                if (hasPermission) {
                    val tempUri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        ContentValues()
                    )
                    cameraTempUri = tempUri
                    tempUri?.let { paymentProofCameraLauncher.launch(it) }
                    showPaymentProofDialog = false
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onPhotoSelected = {
                paymentProofPhotoPickerLauncher.launch("image/*")
                showPaymentProofDialog = false
            },
            onPdfSelected = {
                paymentProofPdfPickerLauncher.launch("application/pdf")
                showPaymentProofDialog = false
            },
            onDismiss = {
                showPaymentProofDialog = false
                waitingForPaymentProof = false
                selectedPaymentMode = null
            }
        )
    }
    
    // Partial Payment Amount Input Dialog
    if (showPartialPaymentDialog) {
        AlertDialog(
            onDismissRequest = {
                showPartialPaymentDialog = false
                partialPaymentAmount = ""
                partialPaymentError = null
            },
            title = {
                Text(
                    text = "Enter Partial Payment Amount",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Total Available: ${FormatUtils.formatCurrency(totalAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = partialPaymentAmount,
                        onValueChange = { newValue ->
                            partialPaymentAmount = newValue
                            partialPaymentError = null
                        },
                        label = { Text("Amount") },
                        placeholder = { Text("Enter amount") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CurrencyRupee,
                                contentDescription = "Amount"
                            )
                        },
                        isError = partialPaymentError != null,
                        supportingText = {
                            if (partialPaymentError != null) {
                                Text(
                                    text = partialPaymentError!!,
                                    color = colorScheme.error
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = partialPaymentAmount.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            partialPaymentError = "Please enter a valid amount"
                        } else if (amount > totalAmount) {
                            partialPaymentError = "Amount cannot be greater than total (${FormatUtils.formatCurrency(totalAmount)})"
                        } else if (isProcessing) {
                            // Don't process if already processing
                            return@Button
                        } else {
                            // Calculate suggested expenses
                            // Sort by: highest amount first, then oldest date first
                            val sortedExpenses = filteredAndSortedExpenses.sortedWith(
                                compareByDescending<Expense> { it.amount }
                                    .thenBy { expense ->
                                        val parsedDate: Date? = if (expense.date.isNotEmpty()) {
                                            try {
                                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(expense.date)
                                            } catch (e: Exception) {
                                                null
                                            }
                                        } else null
                                        parsedDate?.time ?: expense.createdAt.toDate().time
                                    }
                            )
                            
                            // Find expenses for higher amount (>= entered amount, closest to target)
                            // Use recursive approach to find best combination
                            var bestHigherSum = Double.MAX_VALUE
                            val bestHigherExpenses = mutableListOf<Expense>()
                            
                            fun findHigherCombination(
                                index: Int,
                                currentSum: Double,
                                currentExpenses: MutableList<Expense>
                            ) {
                                // If we've found a valid combination, check if it's better
                                if (currentSum >= amount) {
                                    if (currentSum < bestHigherSum) {
                                        bestHigherSum = currentSum
                                        bestHigherExpenses.clear()
                                        bestHigherExpenses.addAll(currentExpenses)
                                    }
                                    return
                                }
                                
                                // If we've processed all expenses or current sum is already too high, stop
                                if (index >= sortedExpenses.size) {
                                    return
                                }
                                
                                val expense = sortedExpenses[index]
                                
                                // Try without this expense
                                findHigherCombination(index + 1, currentSum, currentExpenses)
                                
                                // Try with this expense
                                val newExpenses = currentExpenses.toMutableList()
                                newExpenses.add(expense)
                                findHigherCombination(index + 1, currentSum + expense.amount, newExpenses)
                            }
                            
                            findHigherCombination(0, 0.0, mutableListOf())
                            
                            val higherExpenses = if (bestHigherExpenses.isNotEmpty() && bestHigherSum < Double.MAX_VALUE) {
                                bestHigherExpenses
                            } else if (sortedExpenses.isNotEmpty()) {
                                mutableListOf(sortedExpenses.first())
                            } else {
                                mutableListOf()
                            }
                            val higherSum = if (higherExpenses.isNotEmpty()) {
                                higherExpenses.sumOf { it.amount }
                            } else {
                                0.0
                            }
                            
                            // Find expenses for lower amount (<= entered amount, closest to target)
                            // Use recursive approach to find maximum sum <= amount
                            var bestLowerSum = 0.0
                            val bestLowerExpenses = mutableListOf<Expense>()
                            
                            fun findLowerCombination(
                                index: Int,
                                currentSum: Double,
                                currentExpenses: MutableList<Expense>
                            ) {
                                // Update best if current combination is better
                                if (currentSum <= amount && currentSum > bestLowerSum) {
                                    bestLowerSum = currentSum
                                    bestLowerExpenses.clear()
                                    bestLowerExpenses.addAll(currentExpenses)
                                }
                                
                                // If we've processed all expenses, stop
                                if (index >= sortedExpenses.size) {
                                    return
                                }
                                
                                val expense = sortedExpenses[index]
                                
                                // Try without this expense
                                findLowerCombination(index + 1, currentSum, currentExpenses)
                                
                                // Try with this expense (only if it doesn't exceed amount)
                                val newSum = currentSum + expense.amount
                                if (newSum <= amount) {
                                    val newExpenses = currentExpenses.toMutableList()
                                    newExpenses.add(expense)
                                    findLowerCombination(index + 1, newSum, newExpenses)
                                }
                            }
                            
                            findLowerCombination(0, 0.0, mutableListOf())
                            
                            val lowerExpenses = bestLowerExpenses.toMutableList()
                            val lowerSum = bestLowerSum
                            
                            // Validate amounts before proceeding
                            if (higherSum <= 0 && lowerSum <= 0) {
                                partialPaymentError = "No valid expense combination found for the entered amount"
                            } else if (higherSum <= 0) {
                                partialPaymentError = "No higher amount option available. Please try a different amount."
                            } else if (lowerSum <= 0) {
                                partialPaymentError = "No lower amount option available. Please try a different amount."
                            } else {
                                suggestedExpensesHigher = higherExpenses
                                suggestedExpensesLower = lowerExpenses
                                suggestedAmountHigher = higherSum
                                suggestedAmountLower = lowerSum
                                
                                showPartialPaymentDialog = false
                                showPartialPaymentSelectionDialog = true
                            }
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary
                    )
                ) {
                    if (isProcessing) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Text("Processing...")
                        }
                    } else {
                        Text("Continue")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPartialPaymentDialog = false
                        partialPaymentAmount = ""
                        partialPaymentError = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    // Partial Payment Selection Dialog (Higher/Lower options)
    if (showPartialPaymentSelectionDialog) {
        AlertDialog(
            onDismissRequest = {
                showPartialPaymentSelectionDialog = false
            },
            title = {
                Text(
                    text = "Select Payment Option",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Requested Amount: ${FormatUtils.formatCurrency(partialPaymentAmount.toDoubleOrNull() ?: 0.0)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    // Higher amount option
                    if (suggestedAmountHigher > 0 && suggestedExpensesHigher.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (suggestedAmountHigher > 0 && suggestedExpensesHigher.isNotEmpty()) {
                                        selectedExpenseIds = suggestedExpensesHigher.map { it.id }.toSet()
                                        isSelectionMode = true
                                        showPartialPaymentSelectionDialog = false
                                        // Trigger payment method selection
                                        selectedPaymentMode = null
                                        selectedPaymentProofUri = null
                                        selectedPaymentProofName = null
                                        showPaymentMethodDialog = true
                                    }
                                },
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.surfaceContainerHighest
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Higher Amount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(suggestedAmountHigher),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                )
                            }
                            Text(
                                text = "${suggestedExpensesHigher.size} expense(s) selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Amount: ${FormatUtils.formatCurrency(suggestedAmountHigher)} (≥ ${FormatUtils.formatCurrency(partialPaymentAmount.toDoubleOrNull() ?: 0.0)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    }
                    
                    // Lower amount option
                    if (suggestedAmountLower > 0 && suggestedExpensesLower.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (suggestedAmountLower > 0 && suggestedExpensesLower.isNotEmpty()) {
                                        selectedExpenseIds = suggestedExpensesLower.map { it.id }.toSet()
                                        isSelectionMode = true
                                        showPartialPaymentSelectionDialog = false
                                        // Trigger payment method selection
                                        selectedPaymentMode = null
                                        selectedPaymentProofUri = null
                                        selectedPaymentProofName = null
                                        showPaymentMethodDialog = true
                                    }
                                },
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.surfaceContainerHighest
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lower Amount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(suggestedAmountLower),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                )
                            }
                            Text(
                                text = "${suggestedExpensesLower.size} expense(s) selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Amount: ${FormatUtils.formatCurrency(suggestedAmountLower)} (≤ ${FormatUtils.formatCurrency(partialPaymentAmount.toDoubleOrNull() ?: 0.0)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    }
                    
                    // Show message if no valid options
                    if (suggestedAmountHigher <= 0 && suggestedAmountLower <= 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "No valid expense combination found for the entered amount. Please try a different amount.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showPartialPaymentSelectionDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    // Payment Proof Confirmation Dialog
    if (showPaymentProofConfirmationDialog && pendingPaymentProofMode != null && pendingPaymentProofUri != null) {
        val expensesToUpdate = if (isSelectionMode && hasSelection) {
            filteredAndSortedExpenses.filter { it.id in selectedExpenseIds }
        } else {
            filteredAndSortedExpenses
        }
        val totalAmountToPay = expensesToUpdate.sumOf { expense ->
            if (expense.remainingBalance != null && expense.remainingBalance!! >= 1) {
                expense.remainingBalance!!
            } else {
                expense.amount
            }
        }
        val paymentModeText = when (pendingPaymentProofMode) {
            PaymentMode.UPI -> "UPI"
            PaymentMode.CARD -> "Credit Card"
            else -> "Unknown"
        }
        
        AlertDialog(
            onDismissRequest = {
                showPaymentProofConfirmationDialog = false
                pendingPaymentProofMode = null
                pendingPaymentProofUri = null
                pendingPaymentProofName = null
                waitingForPaymentProof = false
                selectedPaymentMode = null
            },
            title = {
                Text(
                    text = "Confirm Payment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Payment Method: $paymentModeText",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Total Amount: ${FormatUtils.formatCurrency(totalAmountToPay)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Number of Expenses: ${expensesToUpdate.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Payment Proof: ${pendingPaymentProofName ?: "Selected"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will upload the payment proof and mark ${expensesToUpdate.size} expense(s) as completed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Set the selected proof URI and trigger upload
                        selectedPaymentProofUri = pendingPaymentProofUri
                        selectedPaymentProofName = pendingPaymentProofName
                        selectedPaymentMode = pendingPaymentProofMode
                        waitingForPaymentProof = true
                        showPaymentProofConfirmationDialog = false
                    },
                    enabled = !isUploadingPaymentProof && !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary
                    )
                ) {
                    if (isUploadingPaymentProof || isProcessing) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Text("Uploading...")
                        }
                    } else {
                        Text("Confirm & Submit")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPaymentProofConfirmationDialog = false
                        pendingPaymentProofMode = null
                        pendingPaymentProofUri = null
                        pendingPaymentProofName = null
                        waitingForPaymentProof = false
                        selectedPaymentMode = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    // Handle payment proof upload and expense update
    LaunchedEffect(selectedPaymentProofUri, selectedPaymentMode, waitingForPaymentProof) {
        if (selectedPaymentProofUri != null && selectedPaymentMode != null && waitingForPaymentProof && !isProcessingPaymentProof) {
            isProcessingPaymentProof = true
            
            // Upload payment proof and then update expenses
            val expensesToUpdate = if (isSelectionMode && selectedExpenseIds.isNotEmpty()) {
                filteredAndSortedExpenses.filter { it.id in selectedExpenseIds }
            } else {
                filteredAndSortedExpenses
            }
            
            if (expensesToUpdate.isNotEmpty()) {
                isUploadingPaymentProof = true
                try {
                    val fileName = selectedPaymentProofName ?: "payment_proof_${System.currentTimeMillis()}.jpg"
                    val firstExpense = expensesToUpdate.first()
                    
                    val uploadResult = approvalViewModel.uploadPaymentProofAndUpdate(
                        expense = firstExpense,
                        paymentProofUri = selectedPaymentProofUri!!,
                        paymentProofName = fileName,
                        paymentMode = selectedPaymentMode!!
                    )
                    
                    if (uploadResult.isSuccess) {
                        val (url, name) = uploadResult.getOrNull()!!
                        approvalViewModel.bulkUpdateExpensesPayment(
                            expenses = expensesToUpdate,
                            paymentMode = selectedPaymentMode!!,
                            paymentProofURL = url,
                            paymentProofName = name
                        )
                        // Clear selection mode after update
                        if (isSelectionMode) {
                            isSelectionMode = false
                            selectedExpenseIds = emptySet()
                        }
                        // Refresh expenses after payment
                        delay(1000)
                        onRefreshExpenses()
                        onPaymentSuccess()
                    } else {
                        // Handle error
                        Log.e("VendorExpenseDetailScreen", "Failed to upload payment proof")
                    }
                } catch (e: Exception) {
                    Log.e("VendorExpenseDetailScreen", "Error uploading payment proof", e)
                } finally {
                    isUploadingPaymentProof = false
                    selectedPaymentProofUri = null
                    selectedPaymentProofName = null
                    selectedPaymentMode = null
                    waitingForPaymentProof = false
                    isProcessingPaymentProof = false
                }
            } else {
                isProcessingPaymentProof = false
                waitingForPaymentProof = false
            }
        }
    }
    
    // Track previous processing state to detect completion
    var previousProcessingState by remember { mutableStateOf(false) }

    // Refresh expenses when processing completes (transitions from true to false)
    LaunchedEffect(isProcessing) {
        if (previousProcessingState && !isProcessing) {
            // Processing just completed, refresh expenses
            delay(1000) // Wait for Firebase to update
            onRefreshExpenses()
        }
        previousProcessingState = isProcessing
    }

    // Full-screen loading overlay while UPI/Card payment proof is uploading
    if (isUploadingPaymentProof || isProcessingPaymentProof) {
        AlertDialog(
            onDismissRequest = {},
            title = null,
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Processing payment...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Uploading proof and updating records",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {},
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun ExpenseCard(
    expense: Expense,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onSelectionChange: (() -> Unit)? = null,
    onClick: () -> Unit,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.4f)
            else colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectionChange?.invoke() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = colorScheme.primary,
                        uncheckedColor = colorScheme.outline,
                        checkmarkColor = colorScheme.onPrimary
                    )
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Display remaining balance if available, otherwise show original amount
                val displayAmount = if (expense.remainingBalance != null && expense.remainingBalance!! > 0) {
                    expense.remainingBalance!!
                } else {
                    expense.amount
                }
                val hasPartialPayment = expense.remainingBalance != null && expense.remainingBalance!! > 0
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = FormatUtils.formatCurrency(displayAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        // Show original amount if partial payment exists
                        if (hasPartialPayment) {
                            Text(
                                text = "Original: ${FormatUtils.formatCurrency(expense.amount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (expense.date.isNotEmpty()) FormatUtils.formatDateShort(expense.date) else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        // Show partial payment indicator
                        if (hasPartialPayment) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colorScheme.tertiaryContainer,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Partial",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Expense Code (highlighted)
                if (expense.expenseCode.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Code: ${expense.expenseCode}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Text(
                    text = if (expense.phaseName != null) expense.phaseName else FormatUtils.getDepartmentDisplayName(expense.department),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface
                )
            
            if (expense.category.isNotEmpty()) {
                Text(
                    text = expense.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            
            if (expense.description.isNotEmpty()) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            if (expense.reviewComments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Comment,
                            contentDescription = null,
                            tint = colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = expense.reviewComments,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSecondaryContainer,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
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
private fun PaymentMethodSelectionBottomSheet(
    onDismiss: () -> Unit,
    onPaymentModeSelected: (PaymentMode) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Select Payment Method",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Cash option
            PaymentMethodOption(
                icon = Icons.Default.AccountBalanceWallet,
                title = "By Cash",
                description = "No payment proof required",
                onClick = {
                    onPaymentModeSelected(PaymentMode.CASH)
                },
                colorScheme = colorScheme
            )
            
            // Credit Card option
            PaymentMethodOption(
                icon = Icons.Default.CreditCard,
                title = "Credit Card",
                description = "Payment proof required",
                onClick = {
                    onPaymentModeSelected(PaymentMode.CARD)
                },
                colorScheme = colorScheme
            )
            
            // UPI option
            PaymentMethodOption(
                icon = Icons.Default.Payment,
                title = "UPI",
                description = "Payment proof required",
                onClick = {
                    onPaymentModeSelected(PaymentMode.UPI)
                },
                colorScheme = colorScheme
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PaymentMethodOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerHighest
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Select",
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PaymentProofSelectionDialog(
    onCameraSelected: () -> Unit,
    onPhotoSelected: () -> Unit,
    onPdfSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Payment Proof",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PaymentProofOption(
                    icon = Icons.Default.PhotoCamera,
                    title = "Take Photo",
                    description = "Capture with camera",
                    onClick = onCameraSelected
                )
                
                PaymentProofOption(
                    icon = Icons.Default.PhotoLibrary,
                    title = "Choose Photo",
                    description = "Select from gallery",
                    onClick = onPhotoSelected
                )
                
                PaymentProofOption(
                    icon = Icons.Default.Description,
                    title = "Choose PDF",
                    description = "Select PDF document",
                    onClick = onPdfSelected
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun PaymentProofOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerHighest
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = title,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
