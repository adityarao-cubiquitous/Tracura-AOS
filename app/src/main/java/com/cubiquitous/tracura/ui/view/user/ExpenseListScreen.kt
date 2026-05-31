package com.cubiquitous.tracura.ui.view.user
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlusOne
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpOffset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.ExpenseStatus
import com.cubiquitous.tracura.model.PaymentMode
import com.cubiquitous.tracura.viewmodel.ExpenseViewModel
import com.cubiquitous.tracura.viewmodel.NotificationViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.model.AuthState
import com.cubiquitous.tracura.model.ExpenseNotificationSummary
import com.cubiquitous.tracura.navigation.Screen
import com.cubiquitous.tracura.navigation.normalizeExpenseListNotificationTag
import java.util.*
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.utils.PhaseStatusHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.ui.platform.LocalContext
import java.util.Date
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.model.DepartmentTemporaryApproverEntry
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.saveable.rememberSaveable
import com.cubiquitous.tracura.viewmodel.ApprovalViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

private data class IosExpensePalette(
    val tier1Background: Color,
    val tier2Surface: Color,
    val tier3Field: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val hairline: Color
)

@Composable
private fun rememberIosExpensePalette(accentColor: Color = MaterialTheme.colorScheme.primary): IosExpensePalette {
    val isDark = isSystemInDarkTheme()
    return remember(isDark, accentColor) {
        IosExpensePalette(
            tier1Background = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7),
            tier2Surface = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
            tier3Field = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF),
            textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
            textSecondary = if (isDark) Color(0x99EBEBF5) else Color(0x993C3C43),
            accent = accentColor,
            onAccent = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF),
            hairline = if (isDark) Color(0xFF38383A) else Color(0x1F000000)
        )
    }
}

// Data class for phase categories
private data class PhaseCategories(
    val completed: List<com.cubiquitous.tracura.model.Phase>,
    val current: List<com.cubiquitous.tracura.model.Phase>,
    val expired: List<com.cubiquitous.tracura.model.Phase>,
    val future: List<com.cubiquitous.tracura.model.Phase>
)

// Reusable composable for truncated  text with popup
@Composable
private fun TruncatedTextWithPopup(
    text: String,
    maxLength: Int = 10,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val iosPalette = rememberIosExpensePalette()
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExpenseListScreen(
    project: Project,
    fromNotification: String = "",
    notificationMessage: String = "",
    onNavigateBack: () -> Unit, 
    onAddExpense: () -> Unit, 
    onTrackSubmissions: () -> Unit = {},
    onNavigateToNotifications: (String) -> Unit = {},
    onShowAllExpenses: () -> Unit = {}, 
    expenseViewModel: ExpenseViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(), 
    authViewModel: AuthViewModel = hiltViewModel(),
    approvalViewModel: ApprovalViewModel = hiltViewModel(),
    navController: NavController
) {
    val iosPalette = rememberIosExpensePalette()
    val expenseSummary by expenseViewModel.expenseSummary.collectAsState()
    val expenses by expenseViewModel.expenses.collectAsState()
    val isLoading by expenseViewModel.isLoading.collectAsState()
    val error by expenseViewModel.error.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val phases by expenseViewModel.phases.collectAsState()
    val isProcessing by approvalViewModel.isProcessing.collectAsState()
    val context = LocalContext.current
    
    // Notification identity: phone-first (same as BusinessHeadProjectsTab), fallback to UID
    val notificationPrimaryUserId = remember(authState.user?.phone, authState.user?.uid) {
        when {
            !authState.user?.phone.isNullOrBlank() -> authState.user?.phone!!.trim()
            !authState.user?.uid.isNullOrBlank() -> authState.user?.uid!!.trim()
            else -> ""
        }
    }


    val notificationAlternateUserIds = remember(
        authState.user?.phone,
        authState.user?.uid,
        notificationPrimaryUserId
    ) {
        buildList {
            val phone = authState.user?.phone?.trim().orEmpty()
            val uid = authState.user?.uid?.trim().orEmpty()
            if (phone.isNotEmpty() && phone != notificationPrimaryUserId) add(phone)
            if (uid.isNotEmpty() && uid != notificationPrimaryUserId) add(uid)
        }.distinct()
    }
    
    // Project-specific notifications from ViewModel (normalized by projectId)
    val projectNotifications by notificationViewModel.projectNotifications.collectAsState()
    val projectNotificationBadge by notificationViewModel.projectNotificationBadge.collectAsState()

    // Keep notification filtering bound to the current project
    LaunchedEffect(project.id) {
        notificationViewModel.setProjectId(project.id)
    }
    
    val projectExpenseStatusSummary = remember(projectNotifications) {
        val approvedCount = projectNotifications.count { 
            it.type == com.cubiquitous.tracura.model.NotificationType.EXPENSE_APPROVED && !it.isRead 
        }
        val rejectedCount = projectNotifications.count { 
            it.type == com.cubiquitous.tracura.model.NotificationType.EXPENSE_REJECTED && !it.isRead 
        }
        val submittedCount = projectNotifications.count { 
            it.type == com.cubiquitous.tracura.model.NotificationType.EXPENSE_SUBMITTED && !it.isRead 
        }
        
        ExpenseNotificationSummary(
            approvedCount = approvedCount,
            rejectedCount = rejectedCount,
            submittedCount = submittedCount,
            totalUnread = projectNotifications.count { !it.isRead },
            hasUpdates = projectNotifications.any { !it.isRead }
        )
    }
    
    // Optimized loading: Screen displays immediately with project data
    // Only phases and expenses are fetched - project details (name, budget, dates, approver, team members) are already passed
    LaunchedEffect(project.id, authState.user?.role, notificationPrimaryUserId, notificationAlternateUserIds) {
        val projectId = project.id ?: return@LaunchedEffect
        
        // Use fast load method which sets project and loads phases/expenses in parallel
        // This method uses ExpenseRepository and ProjectRepository internally
        expenseViewModel.loadProjectExpensesFast(
            projectId = projectId,
            project = project, // Project details already passed - no fetch needed
            preloadedPhases = null, // Will be fetched
            preloadedExpenses = null, // Will be fetched
            userRole = authState.user?.role
        )
        
        // Initialize notifications for this user (non-blocking)
        if (notificationPrimaryUserId.isNotEmpty()) {
            notificationViewModel.loadNotifications(notificationPrimaryUserId, notificationAlternateUserIds)
        }
    }

    // Keep notifications in sync while this screen is visible (same behavior as BusinessHeadProjectsTab)
    LaunchedEffect(notificationPrimaryUserId, notificationAlternateUserIds, project.id) {
        if (notificationPrimaryUserId.isNotEmpty()) {
            while (true) {
                delay(10_000L)
                notificationViewModel.refreshFromLocalStorage(
                    notificationPrimaryUserId,
                    notificationAlternateUserIds
                )
            }
        }
    }
    
    // Update payment flow state (for approved, non-credit, amount < 100000)
    var expenseForPaymentUpdate by remember { mutableStateOf<Expense?>(null) }
    var showPaymentMethodSheet by remember { mutableStateOf(false) }
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
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    val delegationBringIntoViewRequester = remember { BringIntoViewRequester() }
    var delegationSectionCenterScrollPending by remember(fromNotification) {
        mutableStateOf(normalizeExpenseListNotificationTag(fromNotification) == "temp_approver")
    }

    val paymentProofCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraTempUri != null) {
            pendingPaymentProofUri = cameraTempUri
            pendingPaymentProofName = "payment_proof_camera_${System.currentTimeMillis()}.jpg"
            cameraTempUri = null
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

    // Check if project is suspended - disable Add Expense if project is suspended
    val isProjectSuspended = remember(project.isSuspended) {
        project.isSuspended == true
    }
    
    // Check if project is locked - disable Add Expense if project is locked
    val isProjectLocked = remember(project.status) {
        project.status.uppercase() == "LOCKED"
    }
    
    // Check if project is IN REVIEW - disable Add Expense for users (but not for approvers)
    val isProjectInReview = remember(project.status) {
        project.status.uppercase() == "IN REVIEW"
    }
    
    // Check if project is DECLINED (REVIEW REJECTED) - disable Add Expense for declined projects
    val isProjectDeclined = remember(project.status) {
        project.status.uppercase() == "REVIEW REJECTED" || project.status.uppercase() == "DECLINED"
    }
    
    // Combined check: disable Add Expense if project is suspended, locked, IN REVIEW, or DECLINED (for user flow only)
    val isAddExpenseDisabled = isProjectSuspended || isProjectLocked || isProjectInReview || isProjectDeclined

    val normalizedUserPhone = remember(authState.user?.phone) {
        authState.user?.phone?.replace("+91", "")?.trim()
    }
    val userDepartmentName = remember(project.departmentUserAssignments, normalizedUserPhone) {
        if (normalizedUserPhone.isNullOrBlank()) {
            null
        } else {
            project.departmentUserAssignments.entries.firstOrNull { (_, phone) ->
                phone.replace("+91", "").trim() == normalizedUserPhone
            }?.key
        }
    }
    val userDepartmentDelegations = remember(project.departmentTemporaryApprover, userDepartmentName) {
        val department = userDepartmentName
        if (department.isNullOrBlank()) {
            emptyList()
        } else {
            project.departmentTemporaryApprover.filter { delegation ->
                val delegationDepartment = delegation.departmentName
                delegationDepartment.equals(department, ignoreCase = true) ||
                    delegationDepartment.trim().endsWith("_$department", ignoreCase = true) ||
                    delegationDepartment.trim().substringAfterLast("_").equals(department, ignoreCase = true)
            }
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(iosPalette.tier1Background)
        ) {
            // Top Bar - iOS style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(iosPalette.tier2Surface)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = iosPalette.textPrimary
                        )
                    }
                    Text(
                        text = "Project Overview",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { 
                        project.id?.let { projectId ->
                            navController.navigate(Screen.ChatList.createRoute(projectId, project.name))
                        }
                    }) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "Chats",
                            tint = iosPalette.textPrimary
                        )
                    }
                    Box {
                        IconButton(onClick = {
                            project.id?.let { onNavigateToNotifications(it) }
                        }) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = iosPalette.textPrimary
                            )
                        }

                        if (projectNotificationBadge.hasUnread && projectNotificationBadge.count > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = 2.dp)
                                    .size(16.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (projectNotificationBadge.count > 99) "99+" else projectNotificationBadge.count.toString(),
                                    color = MaterialTheme.colorScheme.onError,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            
            // Project Suspended Banner (blue bar with pause icon)
            if (isProjectSuspended) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = iosPalette.accent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Pause,
                            contentDescription = null,
                            tint = iosPalette.onAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Project Suspended",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = iosPalette.onAccent
                        )
                    }
                }
            }
            
            // Recent Expenses - Show all expenses sorted by submittedAt (most recent first)
            val recentExpenses = remember(expenses) {
                expenses.sortedByDescending { 
                    it.submittedAt?.toDate()?.time ?: it.getDateAsTimestamp()?.toDate()?.time ?: 0L 
                }
            }
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
            // Project Header Card
            item {
                ProjectHeaderCard(project = project, expenses = expenses, fromNotification = fromNotification)
            }

            if (userDepartmentDelegations.isNotEmpty()) {
                item {
                    UserDepartmentDelegationSection(
                        departmentName = userDepartmentName.orEmpty(),
                        delegates = userDepartmentDelegations,
                        fromNotification = fromNotification,
                        bringIntoViewRequester = delegationBringIntoViewRequester
                    )
                }
            }
            
//            // Key Information Section
//            item {
//                KeyInformationCard(project = project, expenseSummary = expenseSummary)
//            }
            
            // Project Manager and Team Size Card
//            item {
//                ManagerTeamCard(project = project)
//            }
            
            // Phase Budget Breakdown
            item {
                PhaseBudgetBreakdownCard(
                    project = project,
                    fromNotification = fromNotification,
                    notificationMessage = notificationMessage,
                    phases = phases,
                    expenses = expenses,
                    authState = authState,
                    expenseViewModel = expenseViewModel,
                    navController = navController
                )
            }
            
            // Recent Expenses
            item {
                RecentExpensesCard(
                    expenses = recentExpenses.take(4),
                    totalExpensesCount = recentExpenses.size,
                    phases = phases,
                    onShowAll = onShowAllExpenses,
                    onNavigateToExpenseChat = { expenseId ->
                        navController.navigate(Screen.UserExpenseChat.createRoute(expenseId))
                    },
                    onEditExpense = { expenseId ->
                        // Cache expense for instant loading
                        recentExpenses.find { it.id == expenseId }?.let { expense ->
                            expenseViewModel.setExpenseForEditing(expense)
                        }
                        navController.navigate(Screen.AddExpense.createRoute(project.id ?: "", expenseId))
                    },
                    onUpdatePaymentClick = { expense ->
                        expenseForPaymentUpdate = expense
                        selectedPaymentMode = null
                        selectedPaymentProofUri = null
                        selectedPaymentProofName = null
                        pendingPaymentProofMode = null
                        pendingPaymentProofUri = null
                        pendingPaymentProofName = null
                        showPaymentMethodSheet = true
                    },
                    onNavigateToExpenseDetail = { expenseId ->
                            navController.currentBackStackEntry?.savedStateHandle?.set("readOnly", true)
                            navController.navigate(Screen.ReviewExpense.createRoute(expenseId))
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        }
        
        // Floating Action Button - Full width (disabled if project is suspended or locked)
        if (isAddExpenseDisabled) {
            // Disabled state - non-clickable Surface
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                color = iosPalette.tier3Field,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = iosPalette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add New Expense",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = iosPalette.textSecondary
                    )
                }
            }
        } else {
            // Enabled state - clickable Surface
            Surface(
                onClick = onAddExpense,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                color = iosPalette.accent,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = iosPalette.onAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add New Expense",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = iosPalette.onAccent
                    )
                }
            }
        }

        // Update payment flow: Payment Method Selection Bottom Sheet
        if (showPaymentMethodSheet && expenseForPaymentUpdate != null) {
            val expense = expenseForPaymentUpdate!!
            UpdatePaymentMethodBottomSheet(
                onDismiss = {
                    showPaymentMethodSheet = false
                    expenseForPaymentUpdate = null
                },
                onPaymentModeSelected = { mode ->
                    selectedPaymentMode = mode
                    showPaymentMethodSheet = false
                    if (mode == PaymentMode.UPI || mode == PaymentMode.CARD) {
                        waitingForPaymentProof = true
                        showPaymentProofDialog = true
                    } else {
                        pendingCashPaymentMode = mode
                        showCashConfirmationDialog = true
                    }
                }
            )
        }

        // Cash Confirmation Dialog
        if (showCashConfirmationDialog && pendingCashPaymentMode != null && expenseForPaymentUpdate != null) {
            val expense = expenseForPaymentUpdate!!
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Payment Method: By Cash",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Amount: ${FormatUtils.formatCurrency(expense.amount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "This will mark the expense as completed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            approvalViewModel.updateExpensePayment(
                                expense = expense,
                                paymentMode = pendingCashPaymentMode!!,
                                paymentProofURL = null,
                                paymentProofName = null
                            )
                            showCashConfirmationDialog = false
                            pendingCashPaymentMode = null
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isProcessing) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Text("Processing...")
                            }
                        } else {
                            Text("Submit")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCashConfirmationDialog = false
                        pendingCashPaymentMode = null
                    }) { Text("Cancel") }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Refresh expenses when approval VM processing completes (e.g. after cash payment)
        var previousProcessing by remember { mutableStateOf(false) }
        LaunchedEffect(isProcessing) {
            if (previousProcessing && !isProcessing && expenseForPaymentUpdate != null) {
                delay(500)
                expenseViewModel.loadProjectExpensesFast(
                    projectId = project.id ?: "",
                    project = project,
                    preloadedPhases = null,
                    preloadedExpenses = null
                )
                expenseForPaymentUpdate = null
            }
            previousProcessing = isProcessing
        }

        // Payment Proof Selection Dialog (UPI / Card)
        if (showPaymentProofDialog && selectedPaymentMode != null && waitingForPaymentProof) {
            UpdatePaymentProofSelectionDialog(
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

        // Payment Proof Confirmation Dialog
        if (showPaymentProofConfirmationDialog && pendingPaymentProofMode != null && pendingPaymentProofUri != null && expenseForPaymentUpdate != null) {
            val expense = expenseForPaymentUpdate!!
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Payment Method: ${if (pendingPaymentProofMode == PaymentMode.UPI) "UPI" else "Card"}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Amount: ${FormatUtils.formatCurrency(expense.amount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Payment Proof: ${pendingPaymentProofName ?: "Selected"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "This will upload the payment proof and mark the expense as completed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedPaymentProofUri = pendingPaymentProofUri
                            selectedPaymentProofName = pendingPaymentProofName
                            selectedPaymentMode = pendingPaymentProofMode
                            waitingForPaymentProof = true
                            showPaymentProofConfirmationDialog = false
                        },
                        enabled = !isUploadingPaymentProof && !isProcessingPaymentProof,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isUploadingPaymentProof || isProcessingPaymentProof) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Text("Uploading...")
                            }
                        } else {
                            Text("Submit")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPaymentProofConfirmationDialog = false
                        pendingPaymentProofMode = null
                        pendingPaymentProofUri = null
                        pendingPaymentProofName = null
                        waitingForPaymentProof = false
                        selectedPaymentMode = null
                    }) { Text("Cancel") }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Handle payment proof upload and expense update
        LaunchedEffect(selectedPaymentProofUri, selectedPaymentMode, waitingForPaymentProof, expenseForPaymentUpdate) {
            if (selectedPaymentProofUri != null && selectedPaymentMode != null && waitingForPaymentProof &&
                expenseForPaymentUpdate != null && !isProcessingPaymentProof
            ) {
                val expense = expenseForPaymentUpdate!!
                isProcessingPaymentProof = true
                isUploadingPaymentProof = true
                try {
                    val fileName = selectedPaymentProofName ?: "payment_proof_${System.currentTimeMillis()}.jpg"
                    val uploadResult = approvalViewModel.uploadPaymentProofAndUpdate(
                        expense = expense,
                        paymentProofUri = selectedPaymentProofUri!!,
                        paymentProofName = fileName,
                        paymentMode = selectedPaymentMode!!
                    )
                    if (uploadResult.isSuccess) {
                        val (url, name) = uploadResult.getOrNull()!!
                        approvalViewModel.updateExpensePayment(
                            expense = expense,
                            paymentMode = selectedPaymentMode!!,
                            paymentProofURL = url,
                            paymentProofName = name
                        )
                        // Refresh and clear handled by LaunchedEffect(isProcessing) when processing completes
                    } else {
                        Log.e("ExpenseListScreen", "Failed to upload payment proof")
                        expenseForPaymentUpdate = null
                    }
                } catch (e: Exception) {
                    Log.e("ExpenseListScreen", "Error uploading payment proof", e)
                    expenseForPaymentUpdate = null
                } finally {
                    isUploadingPaymentProof = false
                    selectedPaymentProofUri = null
                    selectedPaymentProofName = null
                    selectedPaymentMode = null
                    waitingForPaymentProof = false
                    isProcessingPaymentProof = false
                }
            }
        }

        LaunchedEffect(delegationSectionCenterScrollPending, userDepartmentDelegations) {
            if (delegationSectionCenterScrollPending && userDepartmentDelegations.isNotEmpty()) {
                delay(300L)
                delegationBringIntoViewRequester.bringIntoView()
                delegationSectionCenterScrollPending = false
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserDepartmentDelegationSection(
    departmentName: String,
    delegates: List<DepartmentTemporaryApproverEntry>,
    fromNotification: String = "",
    bringIntoViewRequester: BringIntoViewRequester? = null
) {
    val iosPalette = rememberIosExpensePalette()
    val isDark = isSystemInDarkTheme()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val preview = delegates.take(2)
    val initialHighlightTag = remember(fromNotification) {
        normalizeExpenseListNotificationTag(fromNotification)
    }
    var activeHighlightTag by remember(initialHighlightTag) {
        mutableStateOf(initialHighlightTag)
    }
    LaunchedEffect(initialHighlightTag) {
        activeHighlightTag = initialHighlightTag
        if (activeHighlightTag == "temp_approver") {
            delay(2500L)
            activeHighlightTag = ""
        }
    }
    val highlightColor by animateColorAsState(
        targetValue = if (activeHighlightTag == "temp_approver") {
            iosPalette.accent.copy(alpha = if (isDark) 0.26f else 0.14f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 900),
        label = "UserTempApproverHighlight"
    )
    val density = androidx.compose.ui.platform.LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    var sectionHeightPx by remember { mutableStateOf(0f) }

    LaunchedEffect(initialHighlightTag, sectionHeightPx, bringIntoViewRequester) {
        if (initialHighlightTag == "temp_approver" && sectionHeightPx > 0f) {
            val centeredTop = ((screenHeightPx - sectionHeightPx) / 2f).coerceAtLeast(0f)
            bringIntoViewRequester?.bringIntoView(
                Rect(
                    0f,
                    -centeredTop,
                    1f,
                    sectionHeightPx - centeredTop
                )
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (bringIntoViewRequester != null) {
                    Modifier.bringIntoViewRequester(bringIntoViewRequester)
                } else {
                    Modifier
                }
            )
            .onGloballyPositioned { coordinates ->
                sectionHeightPx = coordinates.size.height.toFloat()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp),
        border = BorderStroke(0.75.dp, iosPalette.hairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(highlightColor, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Delegation (${departmentName.substringAfterLast("_")})",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosPalette.textPrimary
            )
            Text(
                text = "${delegates.size} assignment${if (delegates.size != 1) "s" else ""}",
                fontSize = 11.sp,
                color = iosPalette.textSecondary
            )

            if (preview.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                preview.forEachIndexed { index, entry ->
                    UserDepartmentDelegationRow(entry = entry, dateFormat = dateFormat)
                    if (index < preview.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                if (delegates.size > 2) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "+${delegates.size - 2} more",
                        fontSize = 12.sp,
                        color = iosPalette.accent,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun UserDepartmentDelegationRow(
    entry: DepartmentTemporaryApproverEntry,
    dateFormat: SimpleDateFormat
) {
    val iosPalette = rememberIosExpensePalette()
    val isDark = isSystemInDarkTheme()
    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    val isCurrentlyActive = entry.isAccepted && entry.isActive &&
        entry.startDate != null && entry.endDate != null &&
        !today.before(entry.startDate) && !today.after(entry.endDate)

    val statusColor = if (isCurrentlyActive) Color(0xFF2E7D32) else iosPalette.textSecondary
    val statusBg = if (isCurrentlyActive) {
        Color(0xFF2E7D32).copy(alpha = if (isDark) 0.24f else 0.14f)
    } else {
        iosPalette.textSecondary.copy(alpha = if (isDark) 0.20f else 0.10f)
    }

    // Fetch approver name from users collection (document ID = phone number)
    var approverName by remember(entry.phone) { mutableStateOf<String?>(null) }
    LaunchedEffect(entry.phone) {
        if (entry.phone.isNotBlank()) {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(entry.phone)
                    .get()
                    .await()
                approverName = doc.getString("name")?.takeIf { it.isNotBlank() }
            } catch (_: Exception) { }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (approverName != null) {
                Text(
                    text = approverName!!,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iosPalette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = entry.phone,
                fontSize = if (approverName != null) 11.sp else 13.sp,
                fontWeight = if (approverName != null) FontWeight.Normal else FontWeight.Medium,
                color = if (approverName != null) iosPalette.textSecondary else iosPalette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val from = entry.startDate?.let { dateFormat.format(it) } ?: "-"
            val to = entry.endDate?.let { dateFormat.format(it) } ?: "-"
            Text(
                text = "$from - $to",
                fontSize = 11.sp,
                color = iosPalette.textSecondary
            )
        }

        Surface(
            color = statusBg,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isCurrentlyActive) "Active" else "Inactive",
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ProjectHeaderCard(project: Project, expenses: List<Expense> = emptyList(), fromNotification: String = "") {
    val iosPalette = rememberIosExpensePalette()
    val scope = rememberCoroutineScope()
    var approverUser by remember { mutableStateOf<User?>(null) }
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    
    // Fetch department manager (approver) for the user's department
    LaunchedEffect(project.id, authState.user?.phone) {
        val currentUserPhone = authState.user?.phone
        if (currentUserPhone != null && currentUserPhone.isNotEmpty()) {
            scope.launch {
                try {
                    // Normalize phone number (remove +91 prefix)
                    val normalizedPhone = currentUserPhone.replace("+91", "").trim()
                    
                    // Find user's department from departmentUserAssignments
                    var userDepartment: String? = null
                    project.departmentUserAssignments.forEach { (deptName, userPhone) ->
                        val normalizedUserPhone = userPhone.replace("+91", "").trim()
                        if (normalizedUserPhone == normalizedPhone) {
                            userDepartment = deptName
                            return@forEach
                        }
                    }
                    
                    // Get department approver from departmentApproverAssignments
                    if (userDepartment != null) {
                        val approverPhone = project.departmentApproverAssignments[userDepartment]
                        if (approverPhone != null && approverPhone.isNotEmpty()) {
                            approverUser = authViewModel.getUserByPhoneNumber(approverPhone)
                        }
                    }
                    
                    // Fallback to project manager if no department manager found
                    if (approverUser == null) {
                        val managerId = project.managerId
                        if (managerId != null && managerId.isNotEmpty()) {
                            approverUser = authViewModel.getUserByPhoneNumber(managerId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ProjectHeaderCard", "Error loading department manager: ${e.message}")
                }
            }
        }
    }

    val initialHighlightTag = remember(fromNotification) {
        normalizeExpenseListNotificationTag(fromNotification)
    }
    var activeHighlightTag by remember(initialHighlightTag) {
        mutableStateOf(initialHighlightTag)
    }

    Log.d(
        "HighlightColorChekcing",
        "fromNotification value: $fromNotification, normalizedTag: $initialHighlightTag"
    )

    LaunchedEffect(initialHighlightTag) {
        activeHighlightTag = initialHighlightTag
        if (activeHighlightTag.isNotEmpty()) {
            delay(2000L)
            activeHighlightTag = ""
        }
    }

    Log.d(
        "HighlightColorChekcing",
        "Does highlitre color or not ${activeHighlightTag.isNotEmpty()} ($activeHighlightTag)"
    )

    val rowHighlightColor by animateColorAsState(
        targetValue = if (activeHighlightTag.isNotEmpty()) iosPalette.accent.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = tween(durationMillis = 1000),
        label = "RowHighlightAnimation"
    )


    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Check if project is suspended - disable Add Expense if project is suspended
            val isProjectSuspended = remember(project.isSuspended) {
                project.isSuspended == true
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                    
                    // Project Name
                    Text(
                        text = project.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary
                    )
                }
            }
            
            // Show suspension reason with yellow warning icon if suspended (right aligned)
            if (isProjectSuspended && !project.suspensionReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFC107), // Yellow color
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = project.suspensionReason,
                        fontSize = 12.sp,
                        color = iosPalette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // First row: DEPARTMENT MANAGER label on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KEY INFORMATION",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.textSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(0.dp))
                
                // DEPARTMENT MANAGER label on the right
                Text(
                    text = "DEPARTMENT MANAGER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = iosPalette.textSecondary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Second row: Location icon + name on left, Department Manager name on right (aligned horizontally)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Location icon + name on the left
                if (project.location.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = iosPalette.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    Text(
                        text = project.location,
                        fontSize = 15.sp,
                            color = iosPalette.textSecondary
                    )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // Department Manager name on the right
                if (approverUser != null) {
                    Text(
                        text = approverUser!!.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                } else {
                    val managerId = project.managerId
                    if (managerId != null && managerId.isNotEmpty()) {
                        Text(
                            text = managerId,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosPalette.textPrimary,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    } else {
                        Text(
                            text = "Not assigned",
                            fontSize = 15.sp,
                            color = iosPalette.textSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Third row: Client icon + name on left, Department Manager phone on right (aligned horizontally)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Client icon + name on the left
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = iosPalette.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                Text(
                    text = project.client.ifEmpty { "Not specified" },
                    fontSize = 15.sp,
                        color = iosPalette.textSecondary
                )
                }
                
                // Department Manager phone on the right
                if (approverUser != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                        .background(color = if (activeHighlightTag == "approver_updated" || activeHighlightTag == "temp_approver" || activeHighlightTag == "user_removed") rowHighlightColor else Color.Transparent , shape = RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = iosPalette.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = approverUser!!.phone,
                            fontSize = 14.sp,
                            color = iosPalette.textSecondary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(20.dp)
//            ) {


                // Project Timeline
                if (project.startDate != null && project.handoverDate != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                                        .background(color = if (activeHighlightTag in listOf("HandOverDateUpdate", "MaintenanceDateUpdate", "PlannedDateUpdate")) rowHighlightColor else Color.Transparent , shape = RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(iosPalette.accent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = iosPalette.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "PROJECT TIMELINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = iosPalette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${project.startDate ?: "N/A"} - ${project.handoverDate ?: "N/A"}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosPalette.textPrimary
                            )
                        }
                    }
                }

                if (project.teamMembers.size > 0){
                    var showTeamMembersSheet by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = if (activeHighlightTag == "user_added" || activeHighlightTag == "user_removed") rowHighlightColor else Color.Transparent , shape = RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .clickable { showTeamMembersSheet = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(iosPalette.accent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = iosPalette.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "TEAM SIZE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = iosPalette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${project.teamMembers.size} members",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosPalette.textPrimary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "View team members",
                            tint = iosPalette.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Team Members Bottom Sheet
                    if (showTeamMembersSheet) {
                        TeamMembersBottomSheet(
                            teamMemberPhones = project.teamMembers,
                            authViewModel = authViewModel,
                            onDismiss = { showTeamMembersSheet = false }
                        )
                    }
                }
                

//            }
        }
    }
}

//@Composable
//fun KeyInformationCard(
//    project: Project,
//    expenseSummary: com.cubiquitous.tracura.model.ExpenseSummary
//) {
//    val approvedExpenses = expenseSummary.totalExpenses
//    val remainingBudget = project.budget - approvedExpenses
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(containerColor = Color.White),
//        shape = RoundedCornerShape(12.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(20.dp)
//        ) {
//            Text(
//                text = "KEY INFORMATION",
//                fontSize = 13.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color.Gray,
//                letterSpacing = 0.5.sp
//            )
//
//            // Project Timeline
//            if (project.startDate != null && project.endDate != null) {
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .size(40.dp)
//                            .background(Color(0xFFAF52DE).copy(alpha = 0.15f), CircleShape),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Icon(
//                            Icons.Default.DateRange,
//                            contentDescription = null,
//                            tint = Color(0xFFAF52DE),
//                            modifier = Modifier.size(20.dp)
//                        )
//                    }
//                    Spacer(modifier = Modifier.width(16.dp))
//                    Column {
//                        Text(
//                            text = "PROJECT TIMELINE",
//                            fontSize = 11.sp,
//                            fontWeight = FontWeight.Medium,
//                            color = Color.Gray
//                        )
//                        Spacer(modifier = Modifier.height(4.dp))
//                        Text(
//                            text = "${project.startDate ?: "N/A"} - ${project.endDate ?: "N/A"}",
//                            fontSize = 15.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = Color.Black
//                        )
//                    }
//                }
//            }
//
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Box(
//                    modifier = Modifier
//                        .size(40.dp)
//                        .background(Color(0xFF007AFF).copy(alpha = 0.15f), CircleShape),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        Icons.Default.Person,
//                        contentDescription = null,
//                        tint = Color(0xFF007AFF),
//                        modifier = Modifier.size(20.dp)
//                    )
//                }
//                Spacer(modifier = Modifier.width(16.dp))
//                Column {
//                    Text(
//                        text = "TEAM SIZE",
//                        fontSize = 11.sp,
//                        fontWeight = FontWeight.Medium,
//                        color = Color.Gray
//                    )
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Text(
//                        text = "${project.teamMembers.size} members",
//                        fontSize = 15.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.Black
//                    )
//                }
//            }
//        }
//    }
//}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    label: String,
    value: String
) {
    val iosPalette = rememberIosExpensePalette()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = iosPalette.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = iosPalette.textPrimary
            )
        }
    }
}

//@Composable
//fun ManagerTeamCard(project: Project) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(containerColor = Color.White),
//        shape = RoundedCornerShape(12.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(20.dp)
//        ) {
//            // Team Size
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Box(
//                    modifier = Modifier
//                        .size(40.dp)
//                        .background(Color(0xFF007AFF).copy(alpha = 0.15f), CircleShape),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        Icons.Default.Person,
//                        contentDescription = null,
//                        tint = Color(0xFF007AFF),
//                        modifier = Modifier.size(20.dp)
//                    )
//                }
//                Spacer(modifier = Modifier.width(16.dp))
//                Column {
//                    Text(
//                        text = "TEAM SIZE",
//                        fontSize = 11.sp,
//                        fontWeight = FontWeight.Medium,
//                        color = Color.Gray
//                    )
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Text(
//                        text = "${project.teamMembers.size} members",
//                        fontSize = 15.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.Black
//                    )
//                }
//            }
//        }
//    }
//}

@Composable
fun DepartmentBudgetCard(
    project: Project,
    expenseSummary: com.cubiquitous.tracura.model.ExpenseSummary,
    expenses: List<Expense>
) {
    val iosPalette = rememberIosExpensePalette()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "DEPARTMENT BUDGET BREAKDOWN",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = iosPalette.textSecondary,
                letterSpacing = 0.5.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Show department budgets
            project.departmentBudgets?.forEach { (deptName, budget) ->
                // Calculate total approved expenses for this department
                val spent = expenses
                    .filter { it.department == deptName && it.status == ExpenseStatus.APPROVED }
                    .sumOf { it.amount }
                
                val remaining = budget - spent
                val utilization = if (budget > 0) (spent / budget * 100) else 0.0
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = deptName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "ALLOCATED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = iosPalette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FormatUtils.formatCurrency(budget),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosPalette.textPrimary
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "APPROVED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = iosPalette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FormatUtils.formatCurrency(spent),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosPalette.accent
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "REMAINING",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = iosPalette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FormatUtils.formatCurrency(remaining),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34C759)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "${String.format("%.2f", utilization)}% utilized",
                        fontSize = 13.sp,
                        color = iosPalette.textSecondary
                    )
                    // Utilization bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(iosPalette.tier3Field)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(
                                    ((utilization.coerceIn(
                                        0.0,
                                        100.0
                                    )) / 100.0).toFloat()
                                )
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(iosPalette.accent)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
            
            // If no department budgets, show general budget
            if (project.departmentBudgets?.isEmpty() != false) {
                val budget = project.budget
                // Calculate total approved expenses
                val spent = expenses
                    .filter { it.status == ExpenseStatus.APPROVED }
                    .sumOf { it.amount }
                val remaining = budget - spent
                val utilization = if (budget > 0) (spent / budget * 100) else 0.0
                
                Text(
                    text = "General",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                color = iosPalette.textPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ALLOCATED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = iosPalette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FormatUtils.formatCurrency(budget),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosPalette.textPrimary
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "APPROVED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = iosPalette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FormatUtils.formatCurrency(spent),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosPalette.accent
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "REMAINING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = iosPalette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FormatUtils.formatCurrency(remaining),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34C759)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${String.format("%.2f", utilization)}% utilized",
                    fontSize = 13.sp,
                    color = iosPalette.textSecondary
                )
                // Utilization bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(iosPalette.tier3Field)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(((utilization.coerceIn(0.0, 100.0)) / 100.0).toFloat())
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(iosPalette.accent)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhaseBudgetBreakdownCard(
    project: Project,
    fromNotification: String = "",
    notificationMessage: String = "",
    phases: List<com.cubiquitous.tracura.model.Phase>,
    expenses: List<Expense>,
    authState: AuthState,
    expenseViewModel: ExpenseViewModel,
    navController: NavController
) {
    val iosPalette = rememberIosExpensePalette()
    var showAllPhases by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val coroutineScope = rememberCoroutineScope()
    val targetPhaseRequester = remember { BringIntoViewRequester() }
    var targetPhaseSize by remember { mutableStateOf(IntSize.Zero) }
    val configuration = LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val viewportHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
            
    // Explicitly trigger loading of all departments for filtering
    val projectId = project.id
    LaunchedEffect(projectId) {
        if (!projectId.isNullOrEmpty()) {
            expenseViewModel.loadAllProjectDepartments(projectId)
        }
    }
    
    // Filter phases for overview: only show enabled phases that are active (current, not expired, not upcoming)
    // Using PhaseStatusHelper for consistent date-based logic per implementation guide
    // Current phases: start date <= today AND end date >= today (both dates required)
    val filteredPhases = remember(phases) {
        phases.filter { phase ->
            // Only show enabled phases in overview
            if (!phase.isEnabledValue) return@filter false
            
            // Exclude upcoming phases (start date in future)
            if (PhaseStatusHelper.isPhaseUpcoming(phase)) return@filter false
            
            // Current phases: start date <= today AND end date >= today (both dates required)
            PhaseStatusHelper.isPhaseCurrent(phase)
        }
    }
    
    val allProjectDepartments by expenseViewModel.allProjectDepartments.collectAsState()
    
    // Filter by User Assignments (Departments)
    // If project has department assignments, only show phases that contain at least one department assigned to the user
    val userVisiblePhases = remember(filteredPhases, project.departmentUserAssignments, authState.user?.phone, allProjectDepartments) {
        if (project.departmentUserAssignments.isNotEmpty()) {
            val currentUserPhone = authState.user?.phone ?: ""
            val userDepartments = project.departmentUserAssignments
                .filterValues { it == currentUserPhone }
                .keys
            
            filteredPhases.filter { phase ->
                // Check if any department in the subcollection for this phase matches user's assigned departments
                // logic: find departments belonging to this phase, check if any name is in userDepartments

                val phaseDepts = allProjectDepartments.filter { it.phaseId == phase.id }
                if (phaseDepts.isNotEmpty()) {
                    // Use subcollection data if available
                    phaseDepts.any { dept ->
                        dept.name in userDepartments && !dept.isFinished
                    }
                } else {
                    // Fallback to map if subcollection data not yet loaded or empty
                    phase.departments.keys.any { it in userDepartments }
                }
            }
        } else {
            filteredPhases
        }
    }
    val notificationAutoOpenKey = remember(fromNotification, notificationMessage) {
        "${fromNotification.trim()}|${notificationMessage.trim()}"
    }


    // Show only first 3 enabled phases in the card
    val phasesToShow = userVisiblePhases.take(3)
    val normalizedNotificationMessage = remember(notificationMessage) { notificationMessage.lowercase() }
    val shouldScrollToTargetPhase = remember(fromNotification, normalizedNotificationMessage, phasesToShow) {
        fromNotification.equals("PhaseEndHighlighting", ignoreCase = true) &&
            phasesToShow.any { phase ->
                normalizedNotificationMessage.contains(phase.phaseName.lowercase())
            }
    }

    val showAllPhasesByNotification = remember(fromNotification, normalizedNotificationMessage, phasesToShow){
        (fromNotification.equals("PhaseEndDateChangeHighlighting", ignoreCase =  true) || fromNotification.equals("PhaseStartHighlighting", ignoreCase = true)) && 
        phases.any { phase ->
            normalizedNotificationMessage.contains(phase.phaseName.lowercase())
        }
    }

    LaunchedEffect(shouldScrollToTargetPhase, phasesToShow.size, targetPhaseSize, viewportHeightPx) {
        if (shouldScrollToTargetPhase && phasesToShow.isNotEmpty() && targetPhaseSize.height > 0) {
            delay(250)
            val itemHeightPx = targetPhaseSize.height.toFloat()
            val itemWidthPx = targetPhaseSize.width.toFloat().coerceAtLeast(1f)
            val rectTop = (itemHeightPx / 2f) - (viewportHeightPx / 2f)
            val rectBottom = rectTop + viewportHeightPx
            targetPhaseRequester.bringIntoView(
                Rect(
                    left = 0f,
                    top = rectTop,
                    right = itemWidthPx,
                    bottom = rectBottom
                )
            )
            Log.d("ExpenseListScreen123", "Requested bringIntoView for highlighted phase from notification '$fromNotification'")
        }
    }
    Log.d("ExpenseListScreen123", "Evaluating shouldScrollToTargetPhase: $shouldScrollToTargetPhase for notification '$fromNotification' with message '$notificationMessage' against phases: ${phasesToShow.map { it.phaseName }}")

    LaunchedEffect(showAllPhasesByNotification, phases.size ) {
        Log.d("ExpenseListScreen123", "Evaluating auto-open for 'View All Phases' modal: showAllPhasesByNotification=$showAllPhasesByNotification, phases.size=${phases.size}, notification='$fromNotification', message='$notificationMessage'")
        if (
            showAllPhasesByNotification &&
            phases.size > 0 
        ) {
            showAllPhases = true
            Log.d("ExpenseListScreen123", "Auto-opening 'View All Phases' modal due to notification '$fromNotification' with message '$notificationMessage'")
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE PHASES",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.textSecondary,
                    letterSpacing = 0.5.sp
                )
                
                // Always show "View All Phases" button if there are any phases (enabled or disabled)
                if (phases.isNotEmpty()) {
                    TextButton(onClick = { showAllPhases = true }) {
                        Text(
                            text = "View All Phases",
                            fontSize = 13.sp,
                            color = iosPalette.accent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (phasesToShow.isEmpty()) {
                Text(
                    text = "No phases available",
                    fontSize = 14.sp,
                    color = iosPalette.textSecondary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                phasesToShow.forEachIndexed { index, phase ->
                val isTarget = normalizedNotificationMessage.contains(phase.phaseName.lowercase()) &&
                    fromNotification.equals("PhaseEndHighlighting", ignoreCase = true)
           // Log.d("ExpenseListScreen123", "Checking phase '${phase.phaseName}' for highlight: isTarget=$isTarget, notificationMessage='$notificationMessage' fromNotification='$fromNotification'") 
                // 2. Wrap the section in a Box to apply the modifier
                Box(
                    modifier = if (isTarget) {
                        Modifier
                            .onSizeChanged { targetPhaseSize = it }
                            .bringIntoViewRequester(targetPhaseRequester)
                    } else {
                        Modifier
                    }
                ){
                    PhaseBudgetItem(
                        phase = phase,
                        fromNotification = if (isTarget) fromNotification else "",
                        notificationMessage =notificationMessage,
                        expenses = expenses,
                        project = project,
                        authState = authState,
                        expenseViewModel = expenseViewModel,
                        navController = navController
                    )
                }
                    if (index < phasesToShow.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
    
    // View All Phases Modal
    if (showAllPhases) {
        // Separate phases into categories: Completed, Current, Expired, Future
        // Per Dashboard Phase Logic Implementation Guide
        val phaseCategories = remember(phases) {
            // Filter out disabled phases for categorization
            var enabledPhases = phases.filter { phase -> phase.isEnabledValue }
            
            // Filter by User Assignments (Departments)
            // If project has department assignments, only show phases that contain at least one department assigned to the user
            if (project.departmentUserAssignments.isNotEmpty()) {
                val currentUserPhone = authState.user?.phone ?: ""
                val userDepartments = project.departmentUserAssignments
                    .filterValues { it == currentUserPhone }
                    .keys
                
                enabledPhases = enabledPhases.filter { phase ->
                    // Check if any department in the subcollection for this phase matches user's assigned departments
                    val phaseDepts = allProjectDepartments.filter { it.phaseId == phase.id }
                    
                    if (phaseDepts.isNotEmpty()) {
                        // Use subcollection data if available
                        phaseDepts.any { dept ->
                            dept.name in userDepartments && !dept.isFinished
                        }
                    } else {
                        // Fallback to map if subcollection data not yet loaded or empty
                        phase.departments.keys.any { it in userDepartments }
                    }
                }
            }
            
            // Separate into categories based on status
            val completed = mutableListOf<com.cubiquitous.tracura.model.Phase>()
            val current = mutableListOf<com.cubiquitous.tracura.model.Phase>()
            val expired = mutableListOf<com.cubiquitous.tracura.model.Phase>()
            val future = mutableListOf<com.cubiquitous.tracura.model.Phase>()
            
            enabledPhases.forEach { phase ->
                // Check status in priority order (per Dashboard Phase Logic Guide)
                // IMPORTANT: Check Future FIRST because a phase with start date in future
                // cannot be completed/current/expired at the same time
                val isFuture = PhaseStatusHelper.isPhaseInFuture(phase)
                val isCompleted = PhaseStatusHelper.isPhaseCompleted(phase)
                val isCurrent = PhaseStatusHelper.isPhaseCurrent(phase)
                val isExpired = PhaseStatusHelper.isPhaseExpired(phase)
                
                when {
                    // Priority 1: Future (start date > today) - MUST check first
                    isFuture -> {
                        future.add(phase)
                        android.util.Log.d("ExpenseListScreen", "✅ Categorized as FUTURE: ${phase.phaseName}, startDate=${phase.startDate}")
                    }
                    // Priority 2: Completed (end date has passed)
                    isCompleted -> {
                        completed.add(phase)
                    }
                    // Priority 3: Current (in progress and not expired)
                    isCurrent -> {
                        current.add(phase)
                    }
                    // Priority 4: Expired (end date < today, but not completed status)
                    isExpired -> {
                        expired.add(phase)
                    }
                    // Fallback: If phase doesn't fit any category, log it
                    else -> {
                        android.util.Log.w("ExpenseListScreen", "⚠️ Phase doesn't fit any category: ${phase.phaseName}, startDate=${phase.startDate}, endDate=${phase.endDate}")
                        // Add to current as fallback
                        current.add(phase)
                    }
                }
            }
            
            // Debug logging
            android.util.Log.d("ExpenseListScreen", "Phase Categories - Completed: ${completed.size}, Current: ${current.size}, Expired: ${expired.size}, Future: ${future.size}")
            future.forEach { phase ->
                android.util.Log.d("ExpenseListScreen", "Future Phase: ${phase.phaseName}, StartDate: ${phase.startDate}, EndDate: ${phase.endDate}")
            }
            
            // Sort each category by phaseNumber
            PhaseCategories(
                completed = completed.sortedBy { it.phaseNumber },
                current = current.sortedBy { it.phaseNumber },
                expired = expired.sortedBy { it.phaseNumber },
                future = future.sortedBy { it.phaseNumber }
            )
        }
        
        val completedPhases = phaseCategories.completed
        val currentPhases = phaseCategories.current
        val expiredPhases = phaseCategories.expired
        val futurePhases = phaseCategories.future
        
        // Debug: Log phase counts
        LaunchedEffect(futurePhases.size) {
            android.util.Log.d("ExpenseListScreen", "🔍 Future phases count: ${futurePhases.size}")
            if (futurePhases.isNotEmpty()) {
                futurePhases.forEach { phase ->
                    android.util.Log.d("ExpenseListScreen", "  - ${phase.phaseName}: startDate=${phase.startDate}, endDate=${phase.endDate}, isFuture=${PhaseStatusHelper.isPhaseInFuture(phase)}")
                }
            }
        }

        LaunchedEffect(showAllPhases, fromNotification, normalizedNotificationMessage) {
            if (fromNotification.equals("PhaseEndDateChangeHighlighting", ignoreCase = true) || fromNotification.equals("PhaseStartHighlighting", ignoreCase = true)) {
                // delay(250)
                val itemHeightPx = targetPhaseSize.height.toFloat()
                val itemWidthPx = targetPhaseSize.width.toFloat().coerceAtLeast(1f)
                val rectTop = (itemHeightPx / 2f) - (viewportHeightPx / 2f)
                val rectBottom = rectTop + viewportHeightPx
                targetPhaseRequester.bringIntoView(
                    Rect(
                        left = 0f,
                        top = rectTop,
                        right = itemWidthPx,
                        bottom = rectBottom
                    )
                )
                Log.d("ExpenseListScreen123", "Requested bringIntoView for highlighted phase from notification '$fromNotification'")
            }
        }

        
        
        ModalBottomSheet(
            onDismissRequest = { showAllPhases = false },
            containerColor = iosPalette.tier1Background,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with Done button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(48.dp))
                    Text(
                        text = "All Phases",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = { showAllPhases = false }) {
                        Text(
                            text = "Done",
                            fontSize = 17.sp,
                            color = iosPalette.accent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Completed Phases Section (per Dashboard Phase Logic Guide)
                if (completedPhases.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = iosPalette.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "COMPLETED PHASES",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosPalette.textPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    completedPhases.forEachIndexed { idx, phase ->
                        val isTarget = normalizedNotificationMessage.contains(phase.phaseName.lowercase()) &&
                            (fromNotification.equals("PhaseEndDateChangeHighlighting", ignoreCase = true) || fromNotification.equals("PhaseStartHighlighting", ignoreCase = true))

                        Box(
                            modifier = if (isTarget) {
                                Modifier
                                    .onSizeChanged { targetPhaseSize = it }
                                    .bringIntoViewRequester(targetPhaseRequester)
                            } else {
                                Modifier
                            }
                        ){    
                            PhaseBudgetItemForModal(
                                phase = phase,
                                fromNotification = if (isTarget) fromNotification else "",
                                notificationMessage = notificationMessage,
                                expenses = expenses,
                                project = project,
                                expenseViewModel = expenseViewModel,
                                badgeType = PhaseStatusHelper.PhaseBadgeType.COMPLETED
                            )
                        }
                        if (idx < completedPhases.size - 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    
                    if (currentPhases.isNotEmpty() || expiredPhases.isNotEmpty() || futurePhases.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                
                // Current Phases Section
                if (currentPhases.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color(0xFF34C759), // Green
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "CURRENT PHASES",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosPalette.textPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    currentPhases.forEachIndexed { idx, phase ->
                            val isTarget = normalizedNotificationMessage.contains(phase.phaseName.lowercase()) &&
                                (fromNotification.equals("PhaseEndDateChangeHighlighting", ignoreCase = true) || fromNotification.equals("PhaseStartHighlighting", ignoreCase = true))
                        Box(
                            modifier = if (isTarget) {
                                Modifier
                                    .onSizeChanged { targetPhaseSize = it }
                                    .bringIntoViewRequester(targetPhaseRequester)
                            } else {
                                Modifier
                            }
                        ){    PhaseBudgetItemForModal(
                            phase = phase,
                            fromNotification = if (isTarget) fromNotification else "",
                            notificationMessage = notificationMessage,
                            expenses = expenses,
                            project = project,
                            expenseViewModel = expenseViewModel,
                            badgeType = PhaseStatusHelper.PhaseBadgeType.ACTIVE
                        )
                    }
                        if (idx < currentPhases.size - 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    
                    if (expiredPhases.isNotEmpty() || futurePhases.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                
                // Expired Phases Section
                if (expiredPhases.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = iosPalette.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "EXPIRED PHASES",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosPalette.textPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    expiredPhases.forEachIndexed { idx, phase ->
                     val isTarget = normalizedNotificationMessage.contains(phase.phaseName.lowercase()) &&
                            (fromNotification.equals("PhaseEndDateChangeHighlighting", ignoreCase = true) || fromNotification.equals("PhaseStartHighlighting", ignoreCase = true))

                        Box(
                            modifier = if (isTarget) {
                                Modifier
                                    .onSizeChanged { targetPhaseSize = it }
                                    .bringIntoViewRequester(targetPhaseRequester)
                            } else {
                                Modifier
                            }
                        ){    PhaseBudgetItemForModal(
                            phase = phase,
                            fromNotification = if (isTarget) fromNotification else "",
                            notificationMessage = notificationMessage,
                            expenses = expenses,
                            project = project,
                            expenseViewModel = expenseViewModel,
                            badgeType = null // Expired phases don't show badge
                        )
                    }
                        if (idx < expiredPhases.size - 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    
                    if (futurePhases.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                
                // Future Phases Section (per Dashboard Phase Logic Guide)
                // Always show section if there are future phases
                if (futurePhases.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = iosPalette.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "FUTURE PHASES (${futurePhases.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosPalette.textPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Display all future phases with PLANNED badge
                    futurePhases.forEachIndexed { idx, phase ->
                        // Verify this is actually a future phase
                        val isActuallyFuture = PhaseStatusHelper.isPhaseInFuture(phase)
                        android.util.Log.d("ExpenseListScreen", "Displaying future phase: ${phase.phaseName}, isFuture=$isActuallyFuture, startDate=${phase.startDate}")
                        
                        // Always pass PLANNED badge type for future phases
                     val isTarget = normalizedNotificationMessage.contains(phase.phaseName.lowercase()) &&
                            (fromNotification.equals("PhaseEndDateChangeHighlighting", ignoreCase = true) || fromNotification.equals("PhaseStartHighlighting", ignoreCase = true))

                        Box(
                            modifier = if (isTarget) {
                                Modifier
                                    .onSizeChanged { targetPhaseSize = it }
                                    .bringIntoViewRequester(targetPhaseRequester)
                            } else {
                                Modifier
                            }
                        ){    PhaseBudgetItemForModal(
                            phase = phase,
                            fromNotification = if (isTarget) fromNotification else "",
                            notificationMessage = notificationMessage,
                            expenses = expenses,
                            project = project,
                            expenseViewModel = expenseViewModel,
                            badgeType = PhaseStatusHelper.PhaseBadgeType.PLANNED // Explicitly set PLANNED badge
                        )
                    }
                        if (idx < futurePhases.size - 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                } else {
                    // Debug: Show message if no future phases found
                    LaunchedEffect(Unit) {
                        android.util.Log.d("ExpenseListScreen", "⚠️ No future phases found. Total phases: ${phases.size}, Enabled: ${phases.count { it.isEnabledValue }}")
                        phases.forEach { phase ->
                            val isFuture = PhaseStatusHelper.isPhaseInFuture(phase)
                            val isCompleted = PhaseStatusHelper.isPhaseCompleted(phase)
                            val isCurrent = PhaseStatusHelper.isPhaseCurrent(phase)
                            val isExpired = PhaseStatusHelper.isPhaseExpired(phase)
                            android.util.Log.d("ExpenseListScreen", "  Phase: ${phase.phaseName}, startDate=${phase.startDate}, endDate=${phase.endDate}, enabled=${phase.isEnabledValue}, isFuture=$isFuture, isCompleted=$isCompleted, isCurrent=$isCurrent, isExpired=$isExpired")
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PhaseBudgetItem(
    phase: com.cubiquitous.tracura.model.Phase,
    fromNotification: String = "",
    notificationMessage: String = "",
    expenses: List<Expense>,
    project: Project,
    authState: AuthState,
    expenseViewModel: ExpenseViewModel,
    navController: NavController
) { 
    val iosPalette = rememberIosExpensePalette()

      val initialHighlightTag = remember(fromNotification) {
        normalizeExpenseListNotificationTag(fromNotification)
    }
    var activeHighlightTag by remember(initialHighlightTag) {
        mutableStateOf(initialHighlightTag)
    }

    LaunchedEffect(initialHighlightTag) {
        activeHighlightTag = initialHighlightTag
        if (activeHighlightTag.isNotEmpty()) {
            delay(2000L)
            activeHighlightTag = ""
        }
    }

    val rowHighlightColor by animateColorAsState(
        targetValue = if (activeHighlightTag.isNotEmpty()) iosPalette.accent.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = tween(durationMillis = 1000),
        label = "RowHighlightAnimation"
    )

    var isExpanded by remember { mutableStateOf(false) }
    var showDepartments by remember { mutableStateOf(false) } // Departments closed by default
    var showMenu by remember { mutableStateOf(false) }
    var showRequestOverrideSheet by remember { mutableStateOf(false) }
    var showRequestStatusSheet by remember { mutableStateOf(false) }
    
    // State for Department Line Items Bottom Sheet
    var showLineItemsSheet by remember { mutableStateOf(false) }
    var selectedDepartmentName by remember { mutableStateOf("") }
    var selectedLineItems by remember { mutableStateOf<List<com.cubiquitous.tracura.model.DepartmentLineItemData>>(emptyList()) }
    
    // Load departments from subcollection immediately when phase is displayed (not just when expanded)
    // Use local state per phase to avoid overwriting when multiple phases load simultaneously
    var departmentsFromSubcollection by remember(phase.id) { mutableStateOf<List<com.cubiquitous.tracura.model.Department>>(emptyList()) }
    var isLoadingDepartments by remember(phase.id) { mutableStateOf(false) }
    
    // Observe the shared StateFlow and capture departments for this specific phase
    val sharedDepartments by expenseViewModel.departmentsFromSubcollection.collectAsState()
    
    LaunchedEffect(phase.id, project.id, sharedDepartments) {
        // Load departments immediately when phase is displayed
        if (phase.id.isNotEmpty() && project.id != null && !isLoadingDepartments && departmentsFromSubcollection.isEmpty()) {
            isLoadingDepartments = true
            // Trigger load - this will update the shared StateFlow
            expenseViewModel.loadDepartmentsForPhase(project.id!!, phase.id)
        }
        
        // Capture departments when they're loaded for this phase
        // Check if the loaded departments belong to this phase by checking if any department's phaseId matches
        if (sharedDepartments.isNotEmpty() && departmentsFromSubcollection.isEmpty()) {
            // Verify these departments belong to this phase
            val phaseDepartments = sharedDepartments.filter { it.phaseId == phase.id }
            
            if (phaseDepartments.isNotEmpty()) {
                // Store them in local state to prevent overwriting by other phases
                departmentsFromSubcollection = phaseDepartments
                isLoadingDepartments = false
                android.util.Log.d("ExpenseListScreen", "✅ Captured ${phaseDepartments.size} departments for phase: ${phase.phaseName}")
            }
        }
    }
    
    // Use departments from subcollection if available, otherwise fallback to phase.departments
    val validDepartments = remember(
        departmentsFromSubcollection,
    ) {
        if (departmentsFromSubcollection.isNotEmpty()) {
            android.util.Log.d("ExpenseListScreen", "Using Subcollection for ${phase.phaseName}: ${departmentsFromSubcollection.map { it.name }}")
            // Use departments from subcollection
            departmentsFromSubcollection
                .filter { dept ->
                    // Only include departments that are NOT marked as anonymous (deleted)
                    phase.isanonymous[dept.name] != true
                }
        } else {
            android.util.Log.d("ExpenseListScreen", "Using Fallback for ${phase.phaseName}: ${phase.departments.keys}")
            // Fallback to phase.departments
            phase.departments.filter { (deptName, _) ->
                // Only include departments that are NOT marked as anonymous (deleted)
                phase.isanonymous[deptName] != true
            }.map { (name, budget) ->
                com.cubiquitous.tracura.model.Department(
                    name = name,
                    totalBudget = budget,
                    remainingAmount = budget // Default for fallback
                )
            }
        }
    }
    
    // Calculate phase budget and spending (only from valid departments)
    val phaseBudget = validDepartments.sumOf { it.totalBudget }
    val phaseExpenses = expenses.filter { 
        it.phaseId == phase.id && it.status == ExpenseStatus.APPROVED 
    }
    val phaseSpent = phaseExpenses.sumOf { it.amount }
    val phaseRemaining = phaseBudget - phaseSpent
    val phaseUtilization = if (phaseBudget > 0) (phaseSpent / phaseBudget * 100) else 0.0
    
    // Check if phase is current using PhaseStatusHelper (per implementation guide)
    // Current phases: start date <= today AND end date >= today (both dates required)
    val isCurrent = PhaseStatusHelper.isPhaseCurrent(phase)
    
    // Format dates
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val formattedStartDate = phase.startDate?.let {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(it)
            date?.let { dateFormatter.format(it) } ?: it
        } catch (e: Exception) { it }
    } ?: "N/A"
    
    val formattedEndDate = phase.endDate?.let {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(it)
            date?.let { dateFormatter.format(it) } ?: it
        } catch (e: Exception) { it }
    } ?: "N/A"
    
    // Check if over budget
    val isOverBudget = phaseRemaining < 0
    val displayUtilization = if (phaseBudget > 0) {
        if (isOverBudget) {
            (phaseSpent / phaseBudget) * 100
        } else {
            phaseUtilization
        }
    } else 0.0
    
    // Check for Extended status
    var hasApprovedRequest by remember { mutableStateOf(false) }
    LaunchedEffect(phase.id, project.id) {
        if (project.id != null && phase.id.isNotEmpty()) {
            try {
                hasApprovedRequest = expenseViewModel.hasApprovedPhaseRequest(project.id!!, phase.id)
            } catch (e: Exception) {
                hasApprovedRequest = false
            }
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Phase Name and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TruncatedTextWithPopup(
                        text = phase.phaseName.ifEmpty { "Phase ${phase.phaseNumber}" },
                        maxLength = 25,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Active chip if phase is enabled and current
                    if (phase.isEnabledValue && isCurrent) {
                        val activeStatusColor = Color(0xFF34C759) // Green color for active status
                        val activeStatusBgColor = activeStatusColor.copy(alpha = 0.12f)
                        
                        Surface(
                            color = activeStatusBgColor,
                            modifier = Modifier.height(25.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, activeStatusColor.copy(alpha = 0.1f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
//                                Box(
//                                    modifier = Modifier
//                                        .size(10.dp)
//                                        .clip(CircleShape)
//                                        .background(activeStatusColor)
//                                )
//                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Active",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = activeStatusColor,
                                    modifier = Modifier.offset(y = (-1).dp)
                                )
                            }
                        }
                    }
                    
                    // Extended chip if phase has approved request
                    if (hasApprovedRequest) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0B2)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Extended",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                    }
                }
                
                // Three dots menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = iosPalette.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        offset = DpOffset(x = (-120).dp, y = 8.dp),
                        modifier = Modifier.background(iosPalette.tier2Surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = iosPalette.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "See Request Status",
                                        fontSize = 16.sp,
                                        color = iosPalette.textPrimary
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                showRequestStatusSheet = true
                            }
                        )
                        
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Request Override",
                                        fontSize = 16.sp,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                showRequestOverrideSheet = true
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Dates
        Row(
            modifier = Modifier
                .background(
                    color = if (fromNotification == "PhaseEndHighlighting") rowHighlightColor else Color.Transparent, 
                    shape = RoundedCornerShape(12.dp)
                )
                // Add padding AFTER the background so the highlight expands around the text nicely
                .padding(horizontal = 8.dp, vertical = 4.dp), 
            verticalAlignment = Alignment.CenterVertically,
            // Adds a tiny gap between "Start:" and the date
            horizontalArrangement = Arrangement.spacedBy(4.dp) 
        ){ 
            Text(
                text = "Start: $formattedStartDate • End: $formattedEndDate",
                fontSize = 13.sp,
                color = iosPalette.textSecondary
            )
        }
            
            // Days remaining indicator (per implementation guide)
            val daysRemaining = PhaseStatusHelper.getDaysRemaining(phase)
            val daysRemainingText = PhaseStatusHelper.getDaysRemainingText(phase)
            if (daysRemainingText != null && isCurrent) {
                Spacer(modifier = Modifier.height(4.dp))
                   Row(
                        modifier = Modifier
                            .background(
                                color = if (fromNotification == "PhaseEndHighlighting") rowHighlightColor else Color.Transparent, 
                                shape = RoundedCornerShape(12.dp)
                            )
                            // Add padding AFTER the background so the highlight expands around the text nicely
                            .padding(horizontal = 8.dp, vertical = 4.dp), 
                        verticalAlignment = Alignment.CenterVertically,
                        // Adds a tiny gap between "Start:" and the date
                        horizontalArrangement = Arrangement.spacedBy(4.dp) 
                    ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime, // The standard clock icon
                        contentDescription = "Days remaining",
                        // Optional: Match the icon color to your dynamic text color
                        tint = PhaseStatusHelper.getDaysRemainingColor(daysRemaining),
                        modifier = Modifier.size(14.dp) // Scaled to match your 12.sp text
                    )

                    Spacer(modifier = Modifier.width(4.dp)) // Small gap between Icon and Text

                    Text(
                        text = daysRemainingText,
                        fontSize = 12.sp,
                        color = PhaseStatusHelper.getDaysRemainingColor(daysRemaining),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // TOTAL BUDGET (Centered)
//            Column(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Text(
//                    text = "TOTAL BUDGET",
//                    fontSize = 11.sp,
//                    fontWeight = FontWeight.Medium,
//                    color = Color.Gray
//                )
//                Spacer(modifier = Modifier.height(4.dp))
//                Text(
//                    text = FormatUtils.formatCurrency(phaseBudget),
//                    fontSize = 20.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = Color.Black
//                )
//            }

//            Spacer(modifier = Modifier.height(16.dp))
//
//            // APPROVED and REMAINING (Side by side)
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                // APPROVED
//                Column {
//                    Text(
//                        text = "APPROVED",
//                        fontSize = 11.sp,
//                        fontWeight = FontWeight.Medium,
//                        color = Color.Gray
//                    )
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Text(
//                        text = FormatUtils.formatCurrency(phaseSpent),
//                        fontSize = 16.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color(0xFF007AFF)
//                    )
//                }
//
//                // REMAINING
//                Column(horizontalAlignment = Alignment.End) {
//                    Text(
//                        text = "REMAINING",
//                        fontSize = 11.sp,
//                        fontWeight = FontWeight.Medium,
//                        color = Color.Gray
//                    )
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Text(
//                        text = FormatUtils.formatCurrency(phaseRemaining),
//                        fontSize = 16.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = if (isOverBudget) Color(0xFFFF3B30) else Color(0xFF34C759)
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))

            // Budget Utilization Bar
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(4.dp)
//                    .clip(RoundedCornerShape(2.dp))
//                    .background(if (isOverBudget) Color(0xFFFF3B30) else Color(0xFFE0E0E0))
//            ) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth(((displayUtilization.coerceIn(0.0, 200.0) / 200.0).coerceAtMost(1.0)).toFloat())
//                        .height(4.dp)
//                        .clip(RoundedCornerShape(2.dp))
//                        .background(if (isOverBudget) Color(0xFFFF3B30) else Color(0xFF2196F3))
//                )
//            }

//            Spacer(modifier = Modifier.height(8.dp))

            // Utilization text and Over budget warning
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "${String.format("%.2f", displayUtilization)}% utilized",
//                    fontSize = 13.sp,
//                    color = Color.Gray
//                )
//
//                if (isOverBudget) {
//                    Text(
//                        text = "Over budget!",
//                        fontSize = 13.sp,
//                        fontWeight = FontWeight.Medium,
//                        color = Color(0xFFFF3B30)
//                    )
//                }
//            }

//            Spacer(modifier = Modifier.height(20.dp))

            // Departments Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Departments (${validDepartments.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.textPrimary
                )

                IconButton(
                    onClick = { showDepartments = !showDepartments },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (showDepartments) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showDepartments) "Collapse departments" else "Expand departments",
                        tint = iosPalette.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Departments List
            if (showDepartments && validDepartments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    validDepartments.forEach { dept ->
                        val currentUserPhone = authState.user?.phone ?: ""
                        val assignedPhone = project.departmentUserAssignments[dept.name]
                        
                        // Show department if:
                        // 1. No assignments exist for the project (show all)
                        // 2. OR The department is explicitly assigned to the current user
                        val shouldShow = if (project.departmentUserAssignments.isNotEmpty()) {
                            assignedPhone == currentUserPhone
                        } else {
                            true
                        }

                        if (shouldShow) {
                            val remainingAmount = dept.remainingAmount

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dept.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = iosPalette.textPrimary
                                )
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = FormatUtils.formatCurrency(remainingAmount),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (remainingAmount < 0) Color(0xFFFF3B30) else Color(0xFF34C759)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            selectedDepartmentName = dept.name
                                            selectedLineItems = dept.lineItems
                                            showLineItemsSheet = true
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "View line items",
                                            tint = iosPalette.accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                             
                            Divider(color = iosPalette.hairline, thickness = 0.5.dp)
                        }
                    }
                }
            }

        }
    }
    
    // Request Override Bottom Sheet
    if (showRequestOverrideSheet) {
        RequestOverrideBottomSheet(
            phase = phase,
            projectId = project.id ?: "",
            userPhoneNumber = authState.user?.phone ?: "",
            expenseViewModel = expenseViewModel,
            onDismiss = { showRequestOverrideSheet = false },
            onSendRequest = { description, extensionDate ->
                if (extensionDate != null && description.isNotBlank()) {
                    expenseViewModel.submitPhaseOverrideRequest(
                        projectId = project.id ?: "",
                        phaseId = phase.id,
                        reason = description,
                        extendedDate = extensionDate,
                        userPhoneNumber = authState.user?.phone ?: "",
                        phase = phase
                    )
                    // Don't close immediately - wait for success/error
                } else {
                    showRequestOverrideSheet = false
                }
            }
        )
    }
    
    // Request Status Bottom Sheet
    if (showRequestStatusSheet) {
        RequestStatusBottomSheet(
            phase = phase,
            projectId = project.id ?: "",
            userPhoneNumber = authState.user?.phone ?: "",
            expenseViewModel = expenseViewModel,
            onDismiss = { showRequestStatusSheet = false }
        )
    }

    // Department Line Items Bottom Sheet
    if (showLineItemsSheet) {
        DepartmentLineItemsBottomSheet(
            departmentName = selectedDepartmentName,
            lineItems = selectedLineItems,
            onDismiss = { showLineItemsSheet = false }
        )
    }
}

@Composable
private fun PhaseBudgetItemForModal(
    phase: com.cubiquitous.tracura.model.Phase,
    fromNotification: String = "",
    notificationMessage: String = "",
    expenses: List<Expense>,
    project: Project,
    expenseViewModel: ExpenseViewModel,
    badgeType: PhaseStatusHelper.PhaseBadgeType? = null
) {
    val iosPalette = rememberIosExpensePalette()
   
    //Highlighiting the dates when changed 
    
    val initialHighlightTag = remember(fromNotification) {
        normalizeExpenseListNotificationTag(fromNotification)
    }
    var activeHighlightTag by remember(initialHighlightTag) {
        mutableStateOf(initialHighlightTag)
    }

    LaunchedEffect(initialHighlightTag) {
        activeHighlightTag = initialHighlightTag
        if (activeHighlightTag.isNotEmpty()) {
            delay(2000L)
            activeHighlightTag = ""
        }
    }

    val rowHighlightColor by animateColorAsState(
        targetValue = if (activeHighlightTag.isNotEmpty()) iosPalette.accent.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = tween(durationMillis = 1000),
        label = "RowHighlightAnimation"
    )

    var isExpanded by remember { mutableStateOf(false) }
    
    // Load departments from subcollection immediately when phase is displayed
    // Use local state per phase to avoid overwriting when multiple phases load simultaneously
    var departmentsFromSubcollection by remember(phase.id) { mutableStateOf<List<com.cubiquitous.tracura.model.Department>>(emptyList()) }
    var isLoadingDepartments by remember(phase.id) { mutableStateOf(false) }
    var lastLoadedPhaseId by remember(phase.id) { mutableStateOf<String?>(null) }
    
    // Observe the shared StateFlow and capture departments for this specific phase
    val sharedDepartments by expenseViewModel.departmentsFromSubcollection.collectAsState()
    
    LaunchedEffect(phase.id, project.id) {
        // Load departments immediately when phase is displayed
        if (phase.id.isNotEmpty() && project.id != null && !isLoadingDepartments && departmentsFromSubcollection.isEmpty()) {
            isLoadingDepartments = true
            lastLoadedPhaseId = phase.id
            // Trigger load - this will update the shared StateFlow
            expenseViewModel.loadDepartmentsForPhase(project.id!!, phase.id)
        }
    }
    
    // Capture departments when they're loaded for this phase
    LaunchedEffect(sharedDepartments, phase.id, lastLoadedPhaseId) {
        // Only capture if these departments were loaded for this specific phase
        if (sharedDepartments.isNotEmpty() && 
            lastLoadedPhaseId == phase.id && 
            departmentsFromSubcollection.isEmpty() &&
            sharedDepartments.any { it.phaseId == phase.id }) {
            // Verify these departments belong to this phase
            val phaseDepartments = sharedDepartments.filter { it.phaseId == phase.id }
            if (phaseDepartments.isNotEmpty()) {
                departmentsFromSubcollection = phaseDepartments
                isLoadingDepartments = false
                android.util.Log.d("ExpenseListScreen", "✅ Captured ${phaseDepartments.size} departments for phase: ${phase.phaseName}")
            }
        }
    }
    
    // Use departments from subcollection if available, otherwise fallback to phase.departments
    val validDepartments = remember(
        departmentsFromSubcollection, 
        phase.departments, 
        phase.isanonymous
    ) {
        if (departmentsFromSubcollection.isNotEmpty()) {
            // Use departments from subcollection with calculated budgets from lineItems
            departmentsFromSubcollection
                .filter { dept ->
                    // Only include departments that are NOT marked as anonymous (deleted)
                    phase.isanonymous[dept.name] != true
                }
                .map { dept ->
                    dept.name to dept.totalBudget
                }
                .toMap()
        } else {
            // Fallback to phase.departments
            phase.departments.filter { (deptName, _) ->
                // Only include departments that are NOT marked as anonymous (deleted)
                phase.isanonymous[deptName] != true
            }
        }
    }
    
    // Calculate phase budget and spending (only from valid departments)
    val phaseBudget = validDepartments.values.sum()
    val phaseExpenses = expenses.filter { 
        it.phaseId == phase.id && it.status == ExpenseStatus.APPROVED 
    }
    val phaseSpent = phaseExpenses.sumOf { it.amount }
    val phaseRemaining = phaseBudget - phaseSpent
    val phaseUtilization = if (phaseBudget > 0) (phaseSpent / phaseBudget * 100) else 0.0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Phase Header with Badge (per Dashboard Phase Logic Guide)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TruncatedTextWithPopup(
                        text = phase.phaseName.ifEmpty { "Phase ${phase.phaseNumber}" },
                        maxLength = 10,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary
                    )
                    
                    // Show badge based on badgeType (per Dashboard Phase Logic Guide)
                    // Priority: Completed > Active > Planned
                    // Always show badge if badgeType is provided
                    // For PLANNED badge, make it more prominent with purple color
                    when (badgeType) {
                        PhaseStatusHelper.PhaseBadgeType.PLANNED -> {
                            // PLANNED badge - Purple color, always visible
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = iosPalette.accent.copy(alpha = 0.12f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = "Planned",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = iosPalette.accent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        PhaseStatusHelper.PhaseBadgeType.COMPLETED -> {
                            // COMPLETED badge - Blue color
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = iosPalette.accent.copy(alpha = 0.12f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = "Completed",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = iosPalette.accent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        PhaseStatusHelper.PhaseBadgeType.ACTIVE -> {
                            // ACTIVE badge - Green color
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF34C759).copy(alpha = 0.12f) // Green background 12% opacity
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = "Active",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF34C759), // Green text
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        null -> {
                            // No badge
                        }
                        else -> {
                            // Other badge types
                            val badgeText = PhaseStatusHelper.getBadgeText(badgeType)
                            val badgeTextColor = PhaseStatusHelper.getBadgeTextColor(badgeType)
                            val badgeBgColor = PhaseStatusHelper.getBadgeBackgroundColor(badgeType)
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = badgeBgColor
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = badgeTextColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    
                    // Extended badge (shown alongside other badges if phase has approved request)
                    var hasApprovedRequest by remember { mutableStateOf(false) }
                    LaunchedEffect(phase.id, project.id) {
                        if (project.id != null && phase.id.isNotEmpty()) {
                            try {
                                hasApprovedRequest = expenseViewModel.hasApprovedPhaseRequest(project.id!!, phase.id)
                            } catch (e: Exception) {
                                hasApprovedRequest = false
                            }
                        }
                    }
                    
                    if (hasApprovedRequest && badgeType != PhaseStatusHelper.PhaseBadgeType.COMPLETED) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = PhaseStatusHelper.getBadgeBackgroundColor(PhaseStatusHelper.PhaseBadgeType.EXTENDED)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PhaseStatusHelper.getBadgeTextColor(PhaseStatusHelper.PhaseBadgeType.EXTENDED),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = PhaseStatusHelper.getBadgeText(PhaseStatusHelper.PhaseBadgeType.EXTENDED),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = PhaseStatusHelper.getBadgeTextColor(PhaseStatusHelper.PhaseBadgeType.EXTENDED)
                                )
                            }
                        }
                    }
                }
                
                // Right side: Three dots menu (for future phases - Start Phase option) or indicator
                // Always show 3-dots menu for PLANNED phases
                when (badgeType) {
                    PhaseStatusHelper.PhaseBadgeType.PLANNED -> {
                        // Three dots menu (for future phases - Start Phase option)
                        var showMenu by remember { mutableStateOf(false) }
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val coroutineScope = rememberCoroutineScope()
                        
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options - Start Phase",
                                    tint = iosPalette.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                offset = DpOffset(x = (-120).dp, y = 8.dp),
                                modifier = Modifier.background(iosPalette.tier2Surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = iosPalette.accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Start Phase",
                                                fontSize = 16.sp,
                                                color = iosPalette.textPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        // Start phase by setting start date to today
                                        expenseViewModel.startPhaseNow(
                                            projectId = project.id ?: "",
                                            phaseId = phase.id,
                                            phaseName = phase.phaseName.ifEmpty { "Phase ${phase.phaseNumber}" },
                                            currentEndDate = phase.endDate
                                        )
                                        // Show toast message
                                        coroutineScope.launch {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Phase started. Start date set to today.",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                    PhaseStatusHelper.PhaseBadgeType.ACTIVE -> {
                        // Small blue dot indicator (only for current phases)
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(iosPalette.accent)
                        )
                    }
                    else -> {
                        // Empty space for alignment
                        Spacer(modifier = Modifier.width(40.dp))
                    }
                }
            }
            
            // Phase Dates - Start Date and End Date
            if (phase.startDate != null || phase.endDate != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Format dates
                val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                val formattedStartDate = phase.startDate?.let {
                    try {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val date = sdf.parse(it)
                        date?.let { dateFormatter.format(it) } ?: it
                    } catch (e: Exception) { it }
                } ?: "N/A"
                
                val formattedEndDate = phase.endDate?.let {
                    try {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val date = sdf.parse(it)
                        date?.let { dateFormatter.format(it) } ?: it
                    } catch (e: Exception) { it }
                } ?: "N/A"
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start Date
                    Row(modifier = Modifier.background(
                        color = if (activeHighlightTag == "PhaseStartHighlighting") rowHighlightColor else Color.Transparent
                        , shape = RoundedCornerShape(8.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = iosPalette.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Start: $formattedStartDate",
                            fontSize = 12.sp,
                            color = iosPalette.textSecondary
                        )
                    }
                    
                    // End Date
                    Row(modifier = Modifier.background(
                        color = if (activeHighlightTag == "PhaseEndDateChangeHighlighting") rowHighlightColor else Color.Transparent
                        , shape = RoundedCornerShape(8.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = iosPalette.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "End: $formattedEndDate",
                            fontSize = 12.sp,
                            color = iosPalette.textSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Budget Information - TOTAL BUDGET, APPROVED, REMAINING
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Column {
//                    Text(
//                        text = "TOTAL BUDGET",
//                        fontSize = 11.sp,
//                        fontWeight = FontWeight.Medium,
//                        color = Color.Gray
//                    )
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Text(
//                        text = FormatUtils.formatCurrency(phaseBudget),
//                        fontSize = 15.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.Black
//                    )
//                }
//
//                Column(horizontalAlignment = Alignment.End) {
//                    Text(
//                        text = "APPROVED",
//                        fontSize = 11.sp,
//                        fontWeight = FontWeight.Medium,
//                        color = Color.Gray
//                    )
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Text(
//                        text = FormatUtils.formatCurrency(phaseSpent),
//                        fontSize = 15.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color(0xFF007AFF)
//                    )
//                }
//
//                Column(horizontalAlignment = Alignment.End) {
//                    Text(
//                        text = "REMAINING",
//                        fontSize = 11.sp,
//                        fontWeight = FontWeight.Medium,
//                        color = Color.Gray
//                    )
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Text(
//                        text = FormatUtils.formatCurrency(phaseRemaining),
//                        fontSize = 15.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color(0xFF34C759)
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            Divider(color = Color(0xFFE5E5EA), thickness = 1.dp)
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            Text(
//                text = "${String.format("%.2f", phaseUtilization)}% utilized",
//                fontSize = 13.sp,
//                color = Color.Gray
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // Utilization bar
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(6.dp)
//                    .clip(RoundedCornerShape(3.dp))
//                    .background(Color(0xFFE0E0E0))
//            ) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth((phaseUtilization.coerceIn(0.0, 100.0) / 100.0).toFloat())
//                        .height(6.dp)
//                        .clip(RoundedCornerShape(3.dp))
//                        .background(Color(0xFF2196F3))
//                )
//            }
            
            // Departments Section
//            if (validDepartments.isNotEmpty()) {
//                Spacer(modifier = Modifier.height(16.dp))
//
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .clickable { isExpanded = !isExpanded },
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = "Departments (${validDepartments.size})",
//                        fontSize = 14.sp,
//                        fontWeight = FontWeight.Medium,
//                        color = Color.Black
//                    )
//
//                    Icon(
//                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
//                        contentDescription = if (isExpanded) "Collapse" else "Expand",
//                        tint = Color(0xFF007AFF),
//                        modifier = Modifier.size(20.dp)
//                    )
//                }
//
//                // Expandable Department Details
//                if (isExpanded) {
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    LazyRow(
//                        horizontalArrangement = Arrangement.spacedBy(8.dp),
//                        contentPadding = PaddingValues(horizontal = 4.dp)
//                    ) {
//                        items(validDepartments.toList()) { (deptName, budget) ->
//                            val deptExpenses = expenses.filter {
//                                it.department == deptName &&
//                                it.phaseId == phase.id &&
//                                it.status == ExpenseStatus.APPROVED
//                            }
//                            val deptSpent = deptExpenses.sumOf { it.amount }
//                            val deptRemaining = budget - deptSpent
//                            val deptUtilization = if (budget > 0) (deptSpent / budget * 100) else 0.0
//
//                            Card(
//                                modifier = Modifier
//                                    .width(230.dp),
//                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
//                                shape = RoundedCornerShape(8.dp),
//                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//                            ) {
//                                Column(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .padding(8.dp)
//                                ) {
//                                    // Department name with blue dot
//                                    Row(
//                                        modifier = Modifier.fillMaxWidth(),
//                                        verticalAlignment = Alignment.CenterVertically,
//                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
//                                    ) {
//                                        Text(
//                                            text = deptName,
//                                            fontSize = 12.sp,
//                                            fontWeight = FontWeight.Bold,
//                                            color = Color.Black
//                                        )
//                                        Box(
//                                            modifier = Modifier
//                                                .size(6.dp)
//                                                .clip(CircleShape)
//                                                .background(Color(0xFF007AFF))
//                                        )
//                                    }
//
//                                    Spacer(modifier = Modifier.height(8.dp))
//
//                                    // Total Budget row
//                                    Row(
//                                        modifier = Modifier.fillMaxWidth(),
//                                        horizontalArrangement = Arrangement.SpaceBetween,
//                                        verticalAlignment = Alignment.CenterVertically
//                                    ) {
//                                        Text(
//                                            text = "Total Budget",
//                                            fontSize = 11.sp,
//                                            color = Color.Gray
//                                        )
//                                        Text(
//                                            text = FormatUtils.formatCurrency(budget),
//                                            fontSize = 11.sp,
//                                            fontWeight = FontWeight.Bold,
//                                            color = Color.Black
//                                        )
//                                    }
//
//                                        Spacer(modifier = Modifier.height(6.dp))
//
//                                    // Approved row
//                                    Row(
//                                        modifier = Modifier.fillMaxWidth(),
//                                        horizontalArrangement = Arrangement.SpaceBetween,
//                                        verticalAlignment = Alignment.CenterVertically
//                                    ) {
//                                        Text(
//                                            text = "Approved",
//                                            fontSize = 11.sp,
//                                            color = Color.Gray
//                                        )
//                                        Text(
//                                            text = FormatUtils.formatCurrency(deptSpent),
//                                            fontSize = 11.sp,
//                                            fontWeight = FontWeight.Bold,
//                                            color = Color(0xFF007AFF)
//                                        )
//                                    }
//
//                                        Spacer(modifier = Modifier.height(6.dp))
//
//                                    // Remaining row
//                                    Row(
//                                        modifier = Modifier.fillMaxWidth(),
//                                        horizontalArrangement = Arrangement.SpaceBetween,
//                                        verticalAlignment = Alignment.CenterVertically
//                                    ) {
//                                        Text(
//                                            text = "Remaining",
//                                            fontSize = 11.sp,
//                                            color = Color.Gray
//                                        )
//                                        Text(
//                                            text = FormatUtils.formatCurrency(deptRemaining),
//                                            fontSize = 11.sp,
//                                            fontWeight = FontWeight.Bold,
//                                            color = Color(0xFF34C759)
//                                        )
//                                    }
//
//                                    Spacer(modifier = Modifier.height(8.dp))
//
//                                    Text(
//                                        text = "${String.format("%.2f", deptUtilization)}% utilized",
//                                        fontSize = 10.sp,
//                                        color = Color.Gray
//                                    )
//
//                                    Spacer(modifier = Modifier.height(3.dp))
//
//                                    Box(
//                                        modifier = Modifier
//                                            .fillMaxWidth()
//                                            .height(3.dp)
//                                            .clip(RoundedCornerShape(2.dp))
//                                            .background(Color(0xFFE0E0E0))
//                                    ) {
//                                        Box(
//                                            modifier = Modifier
//                                                .fillMaxWidth(((deptUtilization.coerceIn(0.0, 100.0)) / 100.0).toFloat())
//                                                .height(3.dp)
//                                                .clip(RoundedCornerShape(2.dp))
//                                                .background(Color(0xFF34C759))
//                                        )
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
        }
    }
}

@Composable
fun RecentExpensesCard(
    expenses: List<Expense>,
    totalExpensesCount: Int,
    phases: List<com.cubiquitous.tracura.model.Phase> = emptyList(),
    onShowAll: () -> Unit,
    onNavigateToExpenseChat: (String) -> Unit = {},
    onEditExpense: (String) -> Unit = {},
    onUpdatePaymentClick: (Expense) -> Unit = {},
    onNavigateToExpenseDetail: (String) -> Unit = {}
) {
    val iosPalette = rememberIosExpensePalette()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
            ) {
                Text(
                    text = "Recent Expenses",
                fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                color = iosPalette.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (expenses.isEmpty()) {
                Text(
                    text = "No expenses yet",
                    fontSize = 15.sp,
                    color = iosPalette.textSecondary,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                expenses.forEach { expense ->
                    ExpenseRow(
                        expense = expense,
                        phases = phases,
                        onNavigateToChat = { onNavigateToExpenseChat(expense.id) },
                        onEditExpense = { expenseId ->
                            onEditExpense(expenseId)
                        },
                        onUpdatePaymentClick = onUpdatePaymentClick,
                        onNavigateToExpenseDetail = onNavigateToExpenseDetail
                    )
                    if (expense != expenses.last()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                // View All Expenses link
                if (totalExpensesCount > expenses.size) {
                        Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "View All Expenses ($totalExpensesCount)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = iosPalette.accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShowAll() },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseRow(
    expense: Expense,
    phases: List<com.cubiquitous.tracura.model.Phase> = emptyList(),
    onNavigateToChat: () -> Unit = {},
    onEditExpense: (String) -> Unit = {},
    onUpdatePaymentClick: (Expense) -> Unit = {},
    onNavigateToExpenseDetail: (String) -> Unit = {}
) {
    val iosPalette = rememberIosExpensePalette()
    val canUpdatePayment = expense.status == ExpenseStatus.APPROVED &&
        !expense.byCredit &&
        expense.amount < 100_000.0
    val statusColor = when (expense.status) {
        ExpenseStatus.APPROVED -> Color(0xFF1976D2) // Blue (Material primary blue)
        ExpenseStatus.COMPLETE -> Color(0xFF4CAF50) // Green (Material success green)
        ExpenseStatus.PENDING -> Color(0xFFFF9800) // Orange
        ExpenseStatus.REJECTED -> Color(0xFFFF3B30) // Red
        else -> Color(0xFF999999) // Gray (Draft, etc.)
    }
    
    val formattedDate = FormatUtils.formatDateLong(expense.date)
    val paymentMethod = when (expense.modeOfPayment) {
        PaymentMode.CASH -> "By cash"
        PaymentMode.UPI -> "By UPI"
        PaymentMode.CHEQUE -> "By Cheque"
        else -> "By ${PaymentMode.toString(expense.modeOfPayment)}"
    }
    
    // Get phase name from phases list using phaseId
    val phaseName = remember(expense.phaseId, phases) {
        if (expense.phaseId?.isNotEmpty() == true && phases.isNotEmpty()) {
            val foundPhase = phases.find { 
                it.id.trim() == expense.phaseId?.trim() 
            } ?: phases.find { 
                it.id.trim().equals(expense.phaseId?.trim() ?: "", ignoreCase = true)
            }
            foundPhase?.phaseName?.takeIf { it.isNotEmpty() } ?: "Phase"
        } else {
            "Phase"
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (canUpdatePayment) {
                    onUpdatePaymentClick(expense)
                } else {
                    onNavigateToExpenseDetail(expense.id)
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier3Field),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
    ) {
            // Left side: Status dot + Phase (blue) + Department + Payment + Category (all left-aligned)
        Row(
            modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(statusColor, CircleShape)
                    .padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Phase name in blue color
                Text(
                        text = phaseName,
                        fontSize = 13.sp,
                        color = iosPalette.accent,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    
                    // Expense Code (highlighted)
                    if (expense.expenseCode.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = expense.expenseCode,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    
                    // Department name
                Text(
                        text = if (expense.department.isNotEmpty()) FormatUtils.getDepartmentDisplayName(expense.department) else "Department...",
                        fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                )
                    
                    // Payment method
                    if (paymentMethod.isNotEmpty()) {
                    Text(
                            text = paymentMethod,
                            fontSize = 12.sp,
                            color = iosPalette.textSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    // Category
                    Text(
                        text = expense.category.ifEmpty { "Category..." },
                        fontSize = 12.sp,
                        color = iosPalette.textSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
            }
        }
        
            Spacer(modifier = Modifier.width(16.dp))
            
            // Right side: Amount + Attachment/Proof icons + Edit/Chat icons + Date (all right-aligned)
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(100.dp)
            ) {
                // Amount at top
            Text(
                text = FormatUtils.formatCurrency(expense.amount),
                    fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                    color = iosPalette.textPrimary,
                    textAlign = TextAlign.End
            )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Attachment and proof icons (below amount)
            Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                    // Attachment icon (blue if exists)
                    if (expense.attachmentUrl.isNotEmpty()) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Attachment",
                            modifier = Modifier.size(18.dp),
                            tint = iosPalette.accent
                        )
                    }
                    
                    // Payment proof icon (green if exists)
                    if (expense.paymentProofUrl.isNotEmpty()) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Payment Proof",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF34C759)
                        )
                    }
                }
                
                // Edit and Chat icons (below attachments, for pending and rejected expenses)
                // Edit button shows for both PENDING and REJECTED
                // Chat button only shows for PENDING (not for rejected)
                if (expense.status == ExpenseStatus.PENDING || expense.status == ExpenseStatus.REJECTED) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Edit icon (for both pending and rejected)
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onEditExpense(expense.id) },
                            tint = Color(0xFFFF9800)
                        )
                        
                        // Chat icon (only for pending expenses)
                        if (expense.status == ExpenseStatus.PENDING) {
                            Icon(
                            Icons.Default.Chat,
                            contentDescription = "Chat",
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onNavigateToChat() },
                                tint = iosPalette.accent
                            )
                        } else {
                            // Placeholder for alignment when rejected (no chat button)
                            Spacer(modifier = Modifier.size(18.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Date at bottom right
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = iosPalette.textSecondary,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestOverrideBottomSheet(
    phase: com.cubiquitous.tracura.model.Phase,
    projectId: String,
    userPhoneNumber: String,
    expenseViewModel: ExpenseViewModel,
    onDismiss: () -> Unit,
    onSendRequest: (String, Date?) -> Unit
) {
    val iosPalette = rememberIosExpensePalette()
    val requestSubmissionSuccess by expenseViewModel.requestSubmissionSuccess.collectAsState()
    val isLoading by expenseViewModel.isLoading.collectAsState()
    val error by expenseViewModel.error.collectAsState()
    
    LaunchedEffect(requestSubmissionSuccess) {
        if (requestSubmissionSuccess) {
            // Show success message and close after a delay
            kotlinx.coroutines.delay(1500)
            onDismiss()
        }
    }
    var description by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val phaseDateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    
    // Calculate minimum date (day after phase end date)
    val minimumDateMillis = remember(phase.endDate) {
        if (!phase.endDate.isNullOrEmpty()) {
            try {
                val endDate = phaseDateFormatter.parse(phase.endDate)
                if (endDate != null) {
                    val calendar = Calendar.getInstance()
                    calendar.time = endDate
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    // Add one day to get the day after end date
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    calendar.timeInMillis
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    // Error state for invalid date selection
    var dateError by remember { mutableStateOf<String?>(null) }
    
    // Initialize date picker state with minimum date if available
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = minimumDateMillis
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = iosPalette.tier2Surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header with Cancel and Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        color = iosPalette.accent,
                        fontSize = 16.sp
                    )
                }
                
                Text(
                    text = "Request Override",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.width(80.dp)) // Balance the Cancel button
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Phase Information Section
            Text(
                text = "Phase Information",
                fontSize = 13.sp,
                color = iosPalette.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Phase field (read-only)
            OutlinedTextField(
                value = phase.phaseName.ifEmpty { "Phase ${phase.phaseNumber}" },
                onValueChange = { },
                readOnly = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Phase", color = iosPalette.textPrimary) },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = iosPalette.textPrimary,
                    disabledLabelColor = iosPalette.textPrimary,
                    disabledBorderColor = iosPalette.hairline,
                    disabledContainerColor = iosPalette.tier3Field
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Request Details Section
            Text(
                text = "Request Details",
                fontSize = 13.sp,
                color = iosPalette.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Description text area
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("Description", color = iosPalette.textPrimary) },
                placeholder = { Text("", color = iosPalette.textSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = iosPalette.textPrimary,
                    unfocusedTextColor = iosPalette.textPrimary,
                    focusedLabelColor = iosPalette.textPrimary,
                    unfocusedLabelColor = iosPalette.textPrimary,
                    focusedBorderColor = iosPalette.hairline,
                    unfocusedBorderColor = iosPalette.hairline,
                    focusedContainerColor = iosPalette.tier3Field,
                    unfocusedContainerColor = iosPalette.tier3Field
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Helper text
            Text(
                text = "Please provide a detailed reason for requesting a phase extension.",
                fontSize = 12.sp,
                color = iosPalette.textSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Extension Date Section
            Text(
                text = "Extension Date",
                fontSize = 13.sp,
                color = iosPalette.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Date picker field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                OutlinedTextField(
                    value = selectedDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = { },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Extend Phase To", color = iosPalette.textPrimary) },
                    placeholder = { Text("Select extension date", color = iosPalette.textSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = iosPalette.textPrimary,
                        disabledLabelColor = iosPalette.textPrimary,
                        disabledBorderColor = iosPalette.hairline,
                        disabledContainerColor = iosPalette.tier3Field
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Date error message
            if (dateError != null) {
                Text(
                    text = dateError ?: "",
                    fontSize = 12.sp,
                    color = Color(0xFFFF3B30), // Red color
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
            
            // Info message
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = iosPalette.accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "The phase will be extended to this date if approved. Please select a date after ${phase.endDate ?: "the phase end date"}.",
                    fontSize = 12.sp,
                    color = iosPalette.textSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Error message
            if (error != null) {
                Text(
                    text = error ?: "",
                    fontSize = 14.sp,
                    color = Color(0xFFFF3B30), // Red color
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            
            // Success message
            if (requestSubmissionSuccess) {
                Text(
                    text = "Request submitted successfully!",
                    fontSize = 14.sp,
                    color = Color(0xFF34C759), // Green color
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            
            // Send Request Button
            Button(
                onClick = {
                    onSendRequest(description, selectedDate)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = iosPalette.accent,
                    contentColor = iosPalette.onAccent,
                    disabledContainerColor = iosPalette.tier3Field,
                    disabledContentColor = iosPalette.textSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading && description.isNotBlank() && selectedDate != null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = iosPalette.onAccent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Send Request",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = iosPalette.onAccent
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        RequestOverrideDatePickerDialog(
            onDateSelected = { date ->
                // Validate that the selected date is after the end date
                if (minimumDateMillis != null && date.time < minimumDateMillis) {
                    dateError = "Please select a date after the phase end date"
                    showDatePicker = false
                } else {
                    selectedDate = date
                    dateError = null
                    showDatePicker = false
                }
            },
            onDismiss = { 
                showDatePicker = false
                dateError = null
            },
            datePickerState = datePickerState,
            title = "Select Extension Date",
            minimumDateMillis = minimumDateMillis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestOverrideDatePickerDialog(
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    datePickerState: DatePickerState,
    title: String,
    minimumDateMillis: Long?
) {
    val iosPalette = rememberIosExpensePalette()
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
        Column {
            // Show warning if minimum date is set
            if (minimumDateMillis != null) {
                val minimumDate = Date(minimumDateMillis)
                val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                Text(
                    text = "Please select a date after ${dateFormatter.format(minimumDate)}",
                    fontSize = 12.sp,
                    color = iosPalette.accent,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamMembersBottomSheet(
    teamMemberPhones: List<String>,
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val iosPalette = rememberIosExpensePalette()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    // State to store team member details
    var teamMembers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Load team member details
    LaunchedEffect(teamMemberPhones) {
        if (teamMemberPhones.isNotEmpty()) {
            isLoading = true
            val members = mutableListOf<User>()
            
            teamMemberPhones.forEach { phone ->
                try {
                    val user = authViewModel.getUserByPhoneNumber(phone)
                    if (user != null) {
                        members.add(user)
                    }
                } catch (e: Exception) {
                    Log.e("TeamMembersBottomSheet", "Error loading user for phone $phone: ${e.message}")
                }
            }
            
            teamMembers = members
            isLoading = false
        } else {
            teamMembers = emptyList()
            isLoading = false
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = iosPalette.tier1Background,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Done button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(48.dp))
                Text(
                    text = "Team Members",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.textPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Done",
                        fontSize = 17.sp,
                        color = iosPalette.accent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (teamMembers.isEmpty()) {
                Text(
                    text = "No team members found",
                    fontSize = 16.sp,
                    color = iosPalette.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                teamMembers.forEachIndexed { index, member ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Avatar/Initial circle
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(iosPalette.accent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.name.take(1).uppercase(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = iosPalette.accent
                                )
                            }
                            
                            // Name and phone
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
                            }
                        }
                    }
                    
                    if (index < teamMembers.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdatePaymentMethodBottomSheet(
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
            UpdatePaymentMethodOption(
                icon = Icons.Default.AccountBalanceWallet,
                title = "By Cash",
                description = "No payment proof required",
                onClick = { onPaymentModeSelected(PaymentMode.CASH) },
                colorScheme = colorScheme
            )
            UpdatePaymentMethodOption(
                icon = Icons.Default.CreditCard,
                title = "By Card",
                description = "Payment proof required",
                onClick = { onPaymentModeSelected(PaymentMode.CARD) },
                colorScheme = colorScheme
            )
            UpdatePaymentMethodOption(
                icon = Icons.Default.Payment,
                title = "By UPI",
                description = "Payment proof required",
                onClick = { onPaymentModeSelected(PaymentMode.UPI) },
                colorScheme = colorScheme
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun UpdatePaymentMethodOption(
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
private fun UpdatePaymentProofSelectionDialog(
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                UpdatePaymentProofOption(
                    icon = Icons.Default.PhotoCamera,
                    title = "Take Photo",
                    description = "Capture with camera",
                    onClick = onCameraSelected
                )
                UpdatePaymentProofOption(
                    icon = Icons.Default.PhotoLibrary,
                    title = "Choose Photo",
                    description = "Select from gallery",
                    onClick = onPhotoSelected
                )
                UpdatePaymentProofOption(
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
private fun UpdatePaymentProofOption(
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

/**
 * Material 3 bottom sheet listing all department line items from Firebase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartmentLineItemsBottomSheet(
    departmentName: String,
    lineItems: List<com.cubiquitous.tracura.model.DepartmentLineItemData>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            HorizontalDivider(
                modifier = Modifier
                    .width(32.dp)
                    .padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
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
                text = "Department line items",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = departmentName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            if (lineItems.isEmpty()) {
                Text(
                    text = "No line items in this department.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    lineItems.forEachIndexed { index, lineItem ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = departmentLineItemDisplayTitle(lineItem),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            supportingContent = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${lineItem.itemType.ifEmpty { "-" }} • ${departmentFormatQuantity(lineItem.quantity)} ${lineItem.uom.ifEmpty { "-" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${FormatUtils.formatCurrency(lineItem.unitPrice)}/${lineItem.uom.ifEmpty { "unit" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            trailingContent = {
                                Text(
                                    text = FormatUtils.formatCurrency(lineItem.total),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                headlineColor = MaterialTheme.colorScheme.onSurface,
                                supportingColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        if (index < lineItems.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun departmentLineItemDisplayTitle(lineItem: com.cubiquitous.tracura.model.DepartmentLineItemData): String {
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
