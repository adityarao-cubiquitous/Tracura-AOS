package com.cubiquitous.tracura.ui.view.businesshead

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.ExpenseStatus
import com.cubiquitous.tracura.model.AmountFilterRange
import com.cubiquitous.tracura.model.PaymentMode
import com.cubiquitous.tracura.ui.components.*
import com.cubiquitous.tracura.viewmodel.ApprovalViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.viewmodel.ChatViewModel
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.utils.ImageUriHelper
import androidx.compose.ui.platform.LocalContext
import com.cubiquitous.tracura.model.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BusinessHeadPendingApprovals(
    projectId: String? = null, // Optional project ID for project-specific approvals
    onNavigateBack: () -> Unit,
    onReviewExpense: (String, String?) -> Unit, // expenseId, role
    onNavigateToExpenseChat: (String) -> Unit = {}, // Add chat navigation parameter
    approvalViewModel: ApprovalViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    userRole: String? = null
) {
    val authState by authViewModel.authState.collectAsState()
    val pendingExpenses by approvalViewModel.pendingExpenses.collectAsState()
    val businessHeadPhone by authViewModel.businessHeadPhone.collectAsState()
    val isProcessing by approvalViewModel.isProcessing.collectAsState()
    val isLoading by approvalViewModel.isLoading.collectAsState()
    val error by approvalViewModel.error.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Pull to refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Refresh function
    val refreshExpenses = {
        if (userRole != null) {
            val userRoleEnum = when (userRole?.uppercase()) {
                "BUSINESS_HEAD" -> com.cubiquitous.tracura.model.UserRole.BUSINESS_HEAD
                "APPROVER" -> com.cubiquitous.tracura.model.UserRole.APPROVER
                "ADMIN" -> com.cubiquitous.tracura.model.UserRole.ADMIN
                "USER" -> com.cubiquitous.tracura.model.UserRole.USER
                else -> com.cubiquitous.tracura.model.UserRole.BUSINESS_HEAD
            }
            if (projectId != null) {
                approvalViewModel.loadPendingApprovalsForProject(projectId, forceRefresh = true, userRole = userRoleEnum)
            } else {
                approvalViewModel.loadPendingApprovals(forceRefresh = true, userRole = userRoleEnum)
            }
        }
    }
    
    // Fetch userRole if not provided
    var fetchedUserRole by remember { mutableStateOf<String?>(userRole) }
    
    LaunchedEffect(Unit) {
        if (fetchedUserRole.isNullOrEmpty()) {
            fetchedUserRole = authViewModel.getUserRoleByPhoneLogic()
        }
    }
    
    LaunchedEffect(authState.user?.phone) {
        authState.user?.phone?.let {
            authViewModel.loadBusinessHeadPhone(it)
        }
    }
    
    // Update when userRole parameter changes
    LaunchedEffect(userRole) {
        if (!userRole.isNullOrEmpty()) {
            fetchedUserRole = userRole
        }
    }
    
    var statusTypeFilter by remember { mutableStateOf<String?>(null) }
    var selectedDateFilter by remember { mutableStateOf<String?>(null) }
    var selectedAmountFilter by remember { mutableStateOf<AmountFilterRange?>(null) }
    var showStatusFilterSheet by remember { mutableStateOf(false) }
    var showDateFilterSheet by remember { mutableStateOf(false) }
    var showAmountFilterSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Multi-select state
    var selectedExpenseIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelectionMode = selectedExpenseIds.isNotEmpty()
    val reviewerName = authState.user?.name?.takeIf { it.isNotEmpty() }
        ?: authState.user?.phone ?: ""

    // Clear selection when bulk processing finishes
    var wasBulkProcessing by remember { mutableStateOf(false) }
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            wasBulkProcessing = true
        } else if (wasBulkProcessing) {
            selectedExpenseIds = emptySet()
            wasBulkProcessing = false
        }
    }
    
    // State for storing fetched user names (submittedBy -> name)
    var userNamesMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val firestore = FirebaseFirestore.getInstance()
    
    // Load data automatically when screen opens
    LaunchedEffect(projectId, fetchedUserRole) {
        if (fetchedUserRole.isNullOrEmpty()) {
            Log.d("CurentUserRole", "⚠️ userRole is null/empty, waiting...")
            return@LaunchedEffect
        }
        
        val userRoleEnum = when (fetchedUserRole?.uppercase()) {
            "BUSINESS_HEAD" -> com.cubiquitous.tracura.model.UserRole.BUSINESS_HEAD
            "APPROVER" -> com.cubiquitous.tracura.model.UserRole.APPROVER
            "ADMIN" -> com.cubiquitous.tracura.model.UserRole.ADMIN
            "USER" -> com.cubiquitous.tracura.model.UserRole.USER
            else -> {
                Log.w("CurentUserRole", "⚠️ Unknown role: '$fetchedUserRole', defaulting to BUSINESS_HEAD")
                com.cubiquitous.tracura.model.UserRole.BUSINESS_HEAD // Default to BUSINESS_HEAD for BusinessHeadPendingApprovals screen
            }
        }
        Log.d("CurentUserRole", "current user role in first in business approval is $userRoleEnum (from '$fetchedUserRole')")

        if (projectId != null) {
            // Load project-specific pending approvals
            approvalViewModel.loadPendingApprovalsForProject(projectId, userRole = userRoleEnum)
        } else {
            // Load all pending approvals
            approvalViewModel.loadPendingApprovals(userRole = userRoleEnum)
        }
    }
    
    
    // Fetch user names for submittedBy values that are not BusinessHead or Admin
    LaunchedEffect(pendingExpenses) {
        val namesToFetch = pendingExpenses
            .mapNotNull { expense ->
                val submittedBy = expense.submittedBy
                // Only fetch if not BusinessHead or Admin
                if (submittedBy.isNotEmpty() && 
                    submittedBy.uppercase() != "BUSINESSHEAD" && 
                    submittedBy.uppercase() != "ADMIN" &&
                    submittedBy != "BusinessHead" &&
                    submittedBy != "Admin" &&
                    !userNamesMap.containsKey(submittedBy)) {
                    submittedBy
                } else {
                    null
                }
            }
            .distinct()
        
        if (namesToFetch.isNotEmpty()) {
            try {
                val fetchedNames = mutableMapOf<String, String>()
                
                // Fetch each user document by document ID (submittedBy)
                namesToFetch.forEach { submittedBy ->
                    try {
                        val userDoc = firestore.collection("users")
                            .document(submittedBy)
                            .get()
                            .await()
                        
                        if (userDoc.exists()) {
                            val userData = userDoc.data
                            val userName = userData?.get("name") as? String
                            if (userName != null && userName.isNotEmpty()) {
                                fetchedNames[submittedBy] = userName
                                android.util.Log.d("BusinessHeadPendingApprovals", "✅ Fetched name for $submittedBy: $userName")
                            } else {
                                android.util.Log.d("BusinessHeadPendingApprovals", "⚠️ No name field found for $submittedBy")
                            }
                        } else {
                            android.util.Log.d("BusinessHeadPendingApprovals", "⚠️ User document not found: $submittedBy")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("BusinessHeadPendingApprovals", "❌ Error fetching user $submittedBy: ${e.message}")
                    }
                }
                
                // Update the map with fetched names
                if (fetchedNames.isNotEmpty()) {
                    userNamesMap = userNamesMap + fetchedNames
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessHeadPendingApprovals", "❌ Error fetching user names: ${e.message}")
            }
        }
    }
    
    val filteredExpenses = remember(
        pendingExpenses,
        statusTypeFilter,
        selectedDateFilter,
        selectedAmountFilter,
        searchQuery,
        userNamesMap
    ) {
        var filtered = pendingExpenses
        
        // Apply status/type filter first (default when view opens: (byCredit==false && approved) || pending)
        filtered = when (statusTypeFilter) {
            null -> {
                // Default: (byCredit == false && status == APPROVED) || (status == PENDING)
                filtered.filter { expense ->
                    (expense.byCredit == false && expense.status == ExpenseStatus.APPROVED) ||
                            (expense.status == ExpenseStatus.PENDING)
                }
            }
            "all" -> filtered
            "pending" -> filtered.filter { it.status == ExpenseStatus.PENDING }
            "approved_non_credit" -> filtered.filter { it.status == ExpenseStatus.APPROVED && it.byCredit == false }
            "approved_all" -> filtered.filter { it.status == ExpenseStatus.APPROVED }
            else -> filtered
        }
        
        // Apply date filter
        selectedDateFilter?.let { dateFilter ->
            val calendar = Calendar.getInstance()
            val now = Date()
            
            filtered = when (dateFilter) {
                "Today" -> {
                    filtered.filter { expense ->
                        val expenseDate = expense.createdAt.toDate()
                        calendar.time = expenseDate
                        val expenseYear = calendar.get(Calendar.YEAR)
                        val expenseDay = calendar.get(Calendar.DAY_OF_YEAR)
                        
                        calendar.time = now
                        val nowYear = calendar.get(Calendar.YEAR)
                        val nowDay = calendar.get(Calendar.DAY_OF_YEAR)
                        
                        expenseYear == nowYear && expenseDay == nowDay
                    }
                }
                "This Week" -> {
                    calendar.time = now
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val weekStart = calendar.time
                    
                    filtered.filter { expense ->
                        expense.createdAt.toDate() >= weekStart
                    }
                }
                "This Month" -> {
                    calendar.time = now
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val monthStart = calendar.time
                    
                    filtered.filter { expense ->
                        expense.createdAt.toDate() >= monthStart
                    }
                }
                else -> filtered
            }
        }
        
        // Apply text search across multiple fields
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.trim().lowercase()
            filtered = filtered.filter { expense ->
                // Amount match
                expense.amount.toString().contains(query) ||
                FormatUtils.formatCurrency(expense.amount).lowercase().contains(query) ||
                // Department
                expense.department.lowercase().contains(query) ||
                FormatUtils.getDepartmentDisplayName(expense.department).lowercase().contains(query) ||
                // Phase
                (expense.phaseName?.lowercase()?.contains(query) == true) ||
                // Vendor
                expense.vendor.lowercase().contains(query) ||
                // Category / Item type
                expense.category.lowercase().contains(query) ||
                (expense.itemType?.lowercase()?.contains(query) == true) ||
                (expense.item?.lowercase()?.contains(query) == true) ||
                // Description
                expense.description.lowercase().contains(query) ||
                // Submitter name (resolved from map)
                (userNamesMap[expense.submittedBy]?.lowercase()?.contains(query) == true) ||
                expense.submittedBy.lowercase().contains(query) ||
                // Brand / Spec
                (expense.brand?.lowercase()?.contains(query) == true) ||
                (expense.spec?.lowercase()?.contains(query) == true)
            }
        }
        
        // Sort by creation date (newest first)
        filtered.sortedByDescending { it.createdAt.toDate().time }
    }
    
    val colorScheme = MaterialTheme.colorScheme
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Material 3 Top App Bar
            TopAppBar(
                title = {
                    Text(
                        text = "Pending Approvals",
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
                    titleContentColor = colorScheme.onPrimary,
                    actionIconContentColor = colorScheme.onPrimary
                )
            )
            
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        text = "Search by name, amount, dept, vendor...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outlineVariant,
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surfaceContainerLow
                )
            )
            
            // Filter chips - Material 3 style with Status first (default filter)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                item {
                    FilterChip(
                        title = when (statusTypeFilter) {
                            null -> "Default"
                            "all" -> "All"
                            "pending" -> "Pending"
                            "approved_non_credit" -> "Approved (non-credit)"
                            "approved_all" -> "Approved (all)"
                            else -> "Status"
                        },
                        isSelected = true,
                        onClick = { showStatusFilterSheet = true }
                    )
                }
                item {
                    FilterChip(
                        title = selectedDateFilter ?: "All Dates",
                        isSelected = selectedDateFilter != null,
                        onClick = { showDateFilterSheet = true }
                    )
                }
                item {
                    FilterChip(
                        title = selectedAmountFilter?.chipDisplayName ?: "Amount",
                        isSelected = selectedAmountFilter != null,
                        onClick = { showAmountFilterSheet = true }
                    )
                }
            }
            
            // Expenses list
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing = isRefreshing || isLoading),
                onRefresh = {
                    isRefreshing = true
                    refreshExpenses()
                    scope.launch {
                        delay(1500)
                        isRefreshing = false
                    }
                }
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                items(filteredExpenses, key = { it.id }) { expense ->
                    ExpenseCard(
                        expense = expense,
                        businessHeadPhone = businessHeadPhone,
                        fetchedUserRole = fetchedUserRole,
                        isSelectionMode = isSelectionMode,
                        isSelected = expense.id in selectedExpenseIds,
                        onLongClick = {
                            if (expense.status == ExpenseStatus.PENDING) {
                                selectedExpenseIds = selectedExpenseIds + expense.id
                            }
                        },
                        onSelectionToggle = {
                            if (expense.status == ExpenseStatus.PENDING) {
                                selectedExpenseIds = if (expense.id in selectedExpenseIds) {
                                    selectedExpenseIds - expense.id
                                } else {
                                    selectedExpenseIds + expense.id
                                }
                            }
                        },
                        onExpenseClick = {
                            if (isSelectionMode) {
                                if (expense.status == ExpenseStatus.PENDING) {
                                    selectedExpenseIds = if (expense.id in selectedExpenseIds) {
                                        selectedExpenseIds - expense.id
                                    } else {
                                        selectedExpenseIds + expense.id
                                    }
                                }
                            } else {
                                onReviewExpense(expense.id, userRole)
                            }
                        },
                        userNamesMap = userNamesMap,
                        onChatClick = {
                            onNavigateToExpenseChat(expense.id)
                        },
                        onAttachmentClick = {
                            expense.attachmentUrl.takeIf { it.isNotEmpty() }?.let { url ->
                                ImageUriHelper.openFirebaseStorageFile(
                                    context = context,
                                    storageUrl = url,
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
                        }
                    )
                }
                
                // Show proper empty / loading states
                if (isLoading && pendingExpenses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Loading approvals...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (filteredExpenses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = if (pendingExpenses.isEmpty())
                                        "No pending approvals found"
                                    else
                                        "No expenses match the selected filters",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            }
        }
        
        // Date filter sheet
        if (showDateFilterSheet) {
            DateFilterSheet(
                selectedFilter = selectedDateFilter,
                onFilterSelected = { selectedDateFilter = it },
                onDismiss = { showDateFilterSheet = false }
            )
        }

        // Amount filter sheet
        if (showAmountFilterSheet) {
            AmountFilterSheet(
                selectedFilter = selectedAmountFilter,
                onFilterSelected = { selectedAmountFilter = it },
                onDismiss = { showAmountFilterSheet = false }
            )
        }

        // Status filter sheet
        if (showStatusFilterSheet) {
            StatusFilterSheet(
                selectedFilter = statusTypeFilter,
                onFilterSelected = { statusTypeFilter = it },
                onDismiss = { showStatusFilterSheet = false }
            )
        }

        // Bulk action bar — shown when items are selected
        if (isSelectionMode) {
            val pendingSelected = filteredExpenses.count {
                it.id in selectedExpenseIds && it.status == ExpenseStatus.PENDING
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = colorScheme.surface,
                shadowElevation = 12.dp,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedExpenseIds = emptySet() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel selection",
                                tint = colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "$pendingSelected selected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val ids = filteredExpenses
                                    .filter { it.id in selectedExpenseIds && it.status == ExpenseStatus.PENDING }
                                    .map { it.id }
                                if (ids.isNotEmpty()) {
                                    approvalViewModel.rejectSelectedExpenses(ids, reviewerName, "")
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = StatusRejected
                            ),
                            border = BorderStroke(1.dp, StatusRejected),
                            enabled = pendingSelected > 0
                        ) {
                            Text("Reject")
                        }
                        Button(
                            onClick = {
                                val ids = filteredExpenses
                                    .filter { it.id in selectedExpenseIds && it.status == ExpenseStatus.PENDING }
                                    .map { it.id }
                                if (ids.isNotEmpty()) {
                                    approvalViewModel.approveSelectedExpenses(ids, reviewerName, "")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary
                            ),
                            enabled = pendingSelected > 0
                        ) {
                            Text("Approve")
                        }
                    }
                }
            }
        }

        // Loading overlay during bulk processing
        if (isProcessing && wasBulkProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 40.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = colorScheme.primary,
                            modifier = Modifier.size(44.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Processing...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}


// ── Status Colors ──────────────────────────────────────────────────────────────
private val StatusApproved = Color(0xFF1565C0)
private val StatusComplete = Color(0xFF2E7D32)
private val StatusPending = Color(0xFFE65100)
private val StatusRejected = Color(0xFFC62828)
private val StatusDefault = Color(0xFF757575)
private val PrimaryBlueLight = Color(0xFFBBDEFB)
private val PrimaryBlueDark = Color(0xFF0D47A1)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseCard(
    expense: Expense,
    businessHeadPhone: String? = null,
    fetchedUserRole: String? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {},
    onSelectionToggle: () -> Unit = {},
    onExpenseClick: () -> Unit,
    onChatClick: () -> Unit = {},
    onAttachmentClick: () -> Unit = {},
    userNamesMap: Map<String, String> = emptyMap()
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    // Status color and label
    val statusColor = when (expense.status) {
        ExpenseStatus.APPROVED -> StatusApproved
        ExpenseStatus.COMPLETE -> StatusComplete
        ExpenseStatus.PENDING -> StatusPending
        ExpenseStatus.REJECTED -> StatusRejected
        else -> StatusDefault
    }
    val statusLabel = when (expense.status) {
        ExpenseStatus.APPROVED -> "Approved"
        ExpenseStatus.COMPLETE -> "Complete"
        ExpenseStatus.PENDING -> "Pending"
        ExpenseStatus.REJECTED -> "Rejected"
        else -> "Unknown"
    }

    // Date formatting
    val formattedDate = FormatUtils.formatDateLong(expense.date)

    // Payment method
    val paymentMethod = when (expense.modeOfPayment) {
        PaymentMode.CASH -> "Cash"
        PaymentMode.UPI -> "UPI"
        PaymentMode.CHEQUE -> "Cheque"
        else -> PaymentMode.toString(expense.modeOfPayment)
    }

    // Submitter name resolution
    val submitterName = remember(expense.submittedBy, userNamesMap, businessHeadPhone) {
        val normalizedSubmittedBy = expense.submittedBy.replace("+91", "").trim()
        val normalizedBHPhone = businessHeadPhone?.replace("+91", "")?.trim()
        
        if (normalizedBHPhone != null && normalizedSubmittedBy == normalizedBHPhone) {
            "Business Head"
        } else if (expense.submittedBy.isBlank()) {
            "Unknown"
        } else {
            userNamesMap[expense.submittedBy] ?: expense.submittedBy
        }
    }

    // Department name as phase tag fallback
    val phaseName = expense.phaseName?.takeIf { it.isNotEmpty() }
        ?: expense.description.take(15).ifEmpty { "Phase" }

    // Display amount: use remainingBalance if available
    val displayAmount = if (expense.remainingBalance != null && expense.remainingBalance!! >= 1) {
        expense.remainingBalance!!
    } else {
        expense.amount
    }

    val isPending = expense.status == ExpenseStatus.PENDING
    val selectionHighlight = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.3f)
    else colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onExpenseClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = selectionHighlight),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        border = if (isSelected) BorderStroke(1.5.dp, colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── Top Row: Checkbox (selection mode) + Phase tag + Status chip ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Checkbox — only visible in selection mode for pending expenses
                    if (isSelectionMode && isPending) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelectionToggle() },
                            modifier = Modifier.size(20.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = colorScheme.primary
                            )
                        )
                    }
                    // Phase tag
                    Surface(
                        color = PrimaryBlueLight.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = phaseName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryBlueDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
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

            // ── Expense Code (highlighted) ─────────────────────────────
            if (expense.expenseCode.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Code: ${expense.expenseCode}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Category / Item type + Department
                    Text(
                        text = buildString {
                            append(expense.itemType ?: expense.category.ifEmpty { "Uncategorized" })
                            if (expense.department.isNotEmpty()) {
                                append(" · ")
                                append(FormatUtils.getDepartmentDisplayName(expense.department))
                            }
                        },
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Amount
                Text(
                    text = FormatUtils.formatCurrency(displayAmount),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Divider ────────────────────────────────────────────────
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

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
                        color = colorScheme.onSurfaceVariant
                    )

                    if (paymentMethod.isNotEmpty()) {
                        Surface(
                            color = colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = paymentMethod,
                                fontSize = 11.sp,
                                color = colorScheme.onSurfaceVariant,
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
                    if (expense.attachmentUrl.isNotEmpty() || expense.attachmentFileName.isNotEmpty()) {
                        IconButton(
                            onClick = { onAttachmentClick() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = "Attachment",
                                modifier = Modifier.size(18.dp),
                                tint = colorScheme.primary
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
                                tint = StatusComplete
                            )
                        }
                    }

                    val normalizedSubmittedBy = expense.submittedBy.replace("+91", "").trim()

                    if (normalizedSubmittedBy != "BUSINESS_HEAD"){
                        // Chat icon
                        IconButton(
                            onClick = { onChatClick() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = "Chat",
                                modifier = Modifier.size(18.dp),
                                tint = colorScheme.primary
                            )
                        }
                    }

                    // Arrow to review
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "View",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
