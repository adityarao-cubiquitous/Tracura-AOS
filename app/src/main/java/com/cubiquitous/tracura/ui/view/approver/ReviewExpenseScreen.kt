package com.cubiquitous.tracura.ui.view.approver

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.ExpenseStatus
import com.cubiquitous.tracura.model.PaymentMode
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.model.ProjectStatus
import com.cubiquitous.tracura.viewmodel.ApprovalViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.viewmodel.ApproverProjectViewModel
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.utils.ImageUriHelper
import kotlinx.coroutines.launch
import java.util.*
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import androidx.compose.ui.res.painterResource
import com.cubiquitous.tracura.navigation.normalizeExpenseListNotificationTag
import kotlinx.coroutines.delay
import com.cubiquitous.tracura.R


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ReviewExpenseScreen(
    expenseId: String,
    fromNotification: String = "false", // New parameter to identify if opened from notification
    onNavigateBack: () -> Unit,
    onNavigateToExpenseChat: (String) -> Unit = {},
    userRole: String? = null, // User role passed from navigation
    isReadOnly: Boolean = false, // Read-only mode flag
    approvalViewModel: ApprovalViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    projectViewModel: ApproverProjectViewModel = hiltViewModel()
) {
    val selectedExpense by approvalViewModel.selectedExpense.collectAsState()
    val pendingExpenses by approvalViewModel.pendingExpenses.collectAsState()
    val isLoading by approvalViewModel.isLoading.collectAsState()
    val error by approvalViewModel.error.collectAsState()
    val isProcessing by approvalViewModel.isProcessing.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val selectedExpenseForScreen = selectedExpense?.takeIf { it.id == expenseId }
    
    val projectBudgetSummary by projectViewModel.projectBudgetSummary.collectAsState()
    val selectedProject by projectViewModel.selectedProject.collectAsState()
    val phasesWithDepartments by projectViewModel.phasesWithDepartments.collectAsState()
    val projectExpenses by projectViewModel.projectExpenses.collectAsState()
    val phaseDepartmentSpentMap by projectViewModel.phaseDepartmentSpentMap.collectAsState()

    val currentViewerRole = remember(userRole, authState.user?.role) {
        when (userRole?.uppercase()) {
            "BUSINESS_HEAD" -> UserRole.BUSINESS_HEAD
            "APPROVER" -> UserRole.APPROVER
            "ADMIN" -> UserRole.ADMIN
            "USER" -> UserRole.USER
            "MANAGER" -> UserRole.MANAGER
            else -> authState.user?.role ?: UserRole.APPROVER
        }
    }
    
    var reviewerNote by remember { mutableStateOf("") }
    var wasProcessing by remember { mutableStateOf(false) }
    var activeReviewAction by remember { mutableStateOf<String?>(null) }
    var updateByCredict by remember { mutableStateOf(false) }
    var requestManagerToggle by remember { mutableStateOf(false) }
    var showRequestManagerConfirmDialog by remember { mutableStateOf(false) }

    // Payment update modal state
    var showPaymentUpdateModal by remember { mutableStateOf(false) }
    var selectedPaymentMode by remember { mutableStateOf<PaymentMode?>(null) }
    var selectedPaymentProofUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPaymentProofName by remember { mutableStateOf<String?>(null) }
    var showPaymentProofDialog by remember { mutableStateOf(false) }
    var isUploadingPaymentProof by remember { mutableStateOf(false) }
    
    // Payment type selection state (Full/Partial)
    var showPaymentTypeDialog by remember { mutableStateOf(false) }
    var showPartialPaymentAmountDialog by remember { mutableStateOf(false) }
    var partialPaymentAmount by remember { mutableStateOf("") }
    var partialPaymentError by remember { mutableStateOf<String?>(null) }
    val paymentHistory by approvalViewModel.paymentHistory.collectAsState()
    var showPaymentHistorySheet by remember { mutableStateOf(false) }

    // Credit Payment Date state
    var showDatePicker by remember { mutableStateOf(false) }
    var showDaysSelector by remember { mutableStateOf(false) }
    var selectedPaymentDate by remember { mutableStateOf<Date?>(null) }
    var selectedDays by remember { mutableStateOf(30) }
    var isSavingCreditDate by remember { mutableStateOf(false) }


    //paymentNavigation

    var howPaymentOpened by remember { mutableStateOf<String?>("") }
    
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val decodedFromNotification = remember(fromNotification) {
        Uri.decode(fromNotification)
    }
    val initialHighlightTag = remember(decodedFromNotification) {
        normalizeExpenseListNotificationTag(decodedFromNotification)
    }
    var activeHighlightTag by remember(initialHighlightTag) {
        mutableStateOf("")
    }
    var hasTriggeredNotificationHighlight by remember(initialHighlightTag, expenseId) {
        mutableStateOf(false)
    }

    LaunchedEffect(initialHighlightTag, selectedExpenseForScreen?.id, expenseId) {
        if (selectedExpenseForScreen == null) return@LaunchedEffect
        if (initialHighlightTag.isBlank()) return@LaunchedEffect
        if (hasTriggeredNotificationHighlight) return@LaunchedEffect

        hasTriggeredNotificationHighlight = true
        activeHighlightTag = initialHighlightTag
    }


    val rowHighlightColor by animateColorAsState(
        targetValue = if (activeHighlightTag.isNotEmpty()) Color(0xFFAF52DE).copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = tween(durationMillis = 1000),
        label = "RowHighlightAnimation"
    )

    val normalizedHighlightTag = remember(activeHighlightTag) {
        activeHighlightTag
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }
    val hasField = remember(normalizedHighlightTag) {
        { token: String -> normalizedHighlightTag.contains(token.lowercase()) }
    }
    Log.d("isNavigatingtoRevirewPage", "fromNotification: $fromNotification")

    val isAmountFieldHighlighted = hasField("amount")
    val isDepartmentFieldHighlighted = hasField("department")
    val isPhaseFieldHighlighted = hasField("phase")
    val isDateFieldHighlighted = hasField("date")
    val isSubCategoryFieldHighlighted = hasField("subcategory")
    val isCategoryFieldHighlighted = hasField("category") && !isSubCategoryFieldHighlighted
    val isItemFieldHighlighted = hasField("item")
    val isBrandFieldHighlighted = hasField("brand")
    val isSpecificationFieldHighlighted = hasField("specification")
    val isThicknessFieldHighlighted = hasField("thickness")
    val isQuantityFieldHighlighted = hasField("quantity") || hasField("uom")
    val isUnitPriceFieldHighlighted = hasField("unitprice")
    val isDescriptionFieldHighlighted = hasField("description")
    val isVendorFieldHighlighted = hasField("vendor")
    val isPaymentModeFieldHighlighted = hasField("paymentmode")
    val isAttachmentFieldHighlighted = hasField("attachment") || hasField("paymentproof")
    val isByCreditFieldHighlighted = hasField("bycreditstatus") || hasField("bycredit")
    val isStatusFieldHighlighted =
        hasField("status")

    val amountBringIntoViewRequester = remember { BringIntoViewRequester() }
    val departmentBringIntoViewRequester = remember { BringIntoViewRequester() }
    val phaseBringIntoViewRequester = remember { BringIntoViewRequester() }
    val dateBringIntoViewRequester = remember { BringIntoViewRequester() }
    val categoryBringIntoViewRequester = remember { BringIntoViewRequester() }
    val subCategoryBringIntoViewRequester = remember { BringIntoViewRequester() }
    val itemBringIntoViewRequester = remember { BringIntoViewRequester() }
    val brandBringIntoViewRequester = remember { BringIntoViewRequester() }
    val specificationBringIntoViewRequester = remember { BringIntoViewRequester() }
    val thicknessBringIntoViewRequester = remember { BringIntoViewRequester() }
    val quantityBringIntoViewRequester = remember { BringIntoViewRequester() }
    val unitPriceBringIntoViewRequester = remember { BringIntoViewRequester() }
    val descriptionBringIntoViewRequester = remember { BringIntoViewRequester() }
    val vendorBringIntoViewRequester = remember { BringIntoViewRequester() }
    val byCreditBringIntoViewRequester = remember { BringIntoViewRequester() }
    val attachmentBringIntoViewRequester = remember { BringIntoViewRequester() }
    val statusBringIntoViewRequester = remember { BringIntoViewRequester() }
    val reviewerNoteBringIntoViewRequester = remember { BringIntoViewRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(normalizedHighlightTag, activeHighlightTag, selectedExpenseForScreen?.id, initialHighlightTag, expenseId) {
        if (selectedExpenseForScreen == null) return@LaunchedEffect
        if (initialHighlightTag.isBlank() || initialHighlightTag == "false") return@LaunchedEffect
        if (activeHighlightTag.isBlank()) return@LaunchedEffect

        val targetRequester = when {
            hasField("status") -> statusBringIntoViewRequester
            hasField("amount") -> amountBringIntoViewRequester
            hasField("department") -> departmentBringIntoViewRequester
            hasField("phase") -> phaseBringIntoViewRequester
            hasField("date") -> dateBringIntoViewRequester
            hasField("subcategory") -> subCategoryBringIntoViewRequester
            hasField("category") -> categoryBringIntoViewRequester
            hasField("item") -> itemBringIntoViewRequester
            hasField("brand") -> brandBringIntoViewRequester
            hasField("specification") -> specificationBringIntoViewRequester
            hasField("thickness") -> thicknessBringIntoViewRequester
            hasField("quantity") || hasField("uom") -> quantityBringIntoViewRequester
            hasField("unitprice") -> unitPriceBringIntoViewRequester
            hasField("description") -> descriptionBringIntoViewRequester
            hasField("vendor") -> vendorBringIntoViewRequester
            hasField("paymentmode") || hasField("bycreditstatus") || hasField("bycredit") -> byCreditBringIntoViewRequester
            hasField("attachment") || hasField("paymentproof") -> attachmentBringIntoViewRequester
            activeHighlightTag.equals("ExpenseStatusUpdate", ignoreCase = true) ||
            activeHighlightTag.equals("expense_status_update", ignoreCase = true) ||
            activeHighlightTag.equals("expense_approved", ignoreCase = true) ||
            activeHighlightTag.equals("expense_rejected", ignoreCase = true) -> reviewerNoteBringIntoViewRequester
            else -> reviewerNoteBringIntoViewRequester
        }

        android.util.Log.d("ReviewExpenseScreen", "Scrolling for tag: $activeHighlightTag")

        targetRequester?.let { requester ->
            for (i in 1..10) {
                kotlinx.coroutines.delay(200)
                try {
                    requester.bringIntoView()
                    android.util.Log.d("ReviewExpenseScreen", "Scrolled successfully on attempt $i")
                    // Nudge scroll down by 1/3 viewport height to center the view approximately
                    kotlinx.coroutines.delay(100)
                    scrollState.animateScrollTo(
                         if (targetRequester != amountBringIntoViewRequester){(scrollState.value + 400).coerceAtMost(scrollState.maxValue)}
                         else {
                                // For amount field, scroll slightly less to avoid overshooting due to its larger size
                                (scrollState.value).coerceAtMost(scrollState.maxValue)
                         }
                    )
                    break
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.d("ReviewExpenseScreen", "Layout not ready for scroll, retrying... ($i/10)")
                }
            }
        }

        kotlinx.coroutines.delay(2000L)
        activeHighlightTag = ""
    }
    
    // Initialize selected payment mode from expense
    LaunchedEffect(selectedExpense) {
        selectedExpense?.let {
            selectedPaymentMode = it.modeOfPayment
            updateByCredict = it.byCredit

            // Initialize credit payment date
            selectedPaymentDate = if (it.creditPaymentDate != null) {
                val creditDate = it.creditPaymentDate.toDate()
                // Calculate days from createdAt
                val createdAt = it.createdAt.toDate()
                val daysDiff = ((creditDate.time - createdAt.time) / (1000 * 60 * 60 * 24)).toInt()
                if (daysDiff > 0) {
                    selectedDays = daysDiff
                }
                creditDate
            } else {
                // Default to 30 days from createdAt
                selectedDays = 30
                val calendar = Calendar.getInstance()
                calendar.time = it.createdAt.toDate()
                calendar.add(Calendar.DAY_OF_YEAR, 30)
                calendar.time
            }
        }
    }
    
    // File picker launchers for payment proof
    val paymentProofCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedPaymentProofUri?.let { uri ->
                selectedPaymentProofName = "payment_proof_camera_${System.currentTimeMillis()}.jpg"
            }
        }
    }
    
    val paymentProofPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedPaymentProofUri = it
            selectedPaymentProofName = it.toString().substringAfterLast("/").substringBefore("?")
                .takeIf { name -> name.isNotEmpty() && name.contains(".") }
                ?: "payment_proof_${System.currentTimeMillis()}.jpg"
        }
    }
    
    val paymentProofPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedPaymentProofUri = it
            selectedPaymentProofName = it.toString().substringAfterLast("/").substringBefore("?")
                .takeIf { name -> name.isNotEmpty() && name.contains(".") }
                ?: "payment_proof_${System.currentTimeMillis()}.pdf"
        }
    }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val tempUri = context.contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                android.content.ContentValues()
            )
            selectedPaymentProofUri = tempUri
            tempUri?.let { paymentProofCameraLauncher.launch(it) }
        }
    }

    // Load pending approvals using loadPendingApprovals function with userRole
    LaunchedEffect(expenseId, userRole) {
        // Convert String? to UserRole enum
        val userRoleEnum = when (userRole?.uppercase()) {
            "BUSINESS_HEAD" -> com.cubiquitous.tracura.model.UserRole.BUSINESS_HEAD
            "APPROVER" -> com.cubiquitous.tracura.model.UserRole.APPROVER
            "ADMIN" -> com.cubiquitous.tracura.model.UserRole.ADMIN
            "USER" -> com.cubiquitous.tracura.model.UserRole.USER
            else -> com.cubiquitous.tracura.model.UserRole.APPROVER // Default fallback
        }
        // Load pending approvals (for PENDING expenses)
        approvalViewModel.loadPendingApprovals(forceRefresh = true, userRole = userRoleEnum)
    }
    
    // Always fetch fresh expense data directly from Firebase to avoid stale cached data
    LaunchedEffect(expenseId) {
        // Fetch the expense directly from Firebase to get the latest status
        approvalViewModel.fetchExpenseById(expenseId)
    }

    // Fetch payment history subcollection whenever the expense doc is loaded
    LaunchedEffect(selectedExpenseForScreen?.id) {
        selectedExpenseForScreen?.let { expense ->
            if (expense.projectId.isNotEmpty()) {
                approvalViewModel.fetchPaymentHistory(expense.projectId, expense.id)
            }
        }
    }

    // Load fresh project budget summary when review page expense is available
    // forceRefresh=true ensures we don't keep stale cached values.
    LaunchedEffect(selectedExpenseForScreen?.id, selectedExpenseForScreen?.projectId) {
        selectedExpenseForScreen?.projectId?.let { projectId ->
            projectViewModel.loadProjectBudgetSummary(projectId, forceRefresh = true)
        }
    }
    
    // Navigate back automatically after successful approval/rejection
    LaunchedEffect(isProcessing) {
        if (wasProcessing && !isProcessing && error == null) {
            kotlinx.coroutines.delay(250)
            onNavigateBack()
        }
        if (!isProcessing) {
            activeReviewAction = null
        }
        wasProcessing = isProcessing
    }
    
    // Track initial status to only auto-navigate if status changes from PENDING
    var initialStatus by remember { mutableStateOf<ExpenseStatus?>(null) }
    
    LaunchedEffect(selectedExpense) {
        val expense = selectedExpense
        if (expense != null && initialStatus == null) {
            initialStatus = expense.status
        }
    }
    
    // Alternative navigation trigger based on status change (only if it was PENDING and changed)
    LaunchedEffect(selectedExpense?.status) {
        val expense = selectedExpense
        if (expense != null) {
            // Only auto-navigate if:
            // 1. Initial status was PENDING
            // 2. Current status is no longer PENDING (was approved/rejected)
            // 3. Don't navigate if user opened an already APPROVED/REJECTED expense
            if (initialStatus == ExpenseStatus.PENDING && expense.status != ExpenseStatus.PENDING) {
                kotlinx.coroutines.delay(200)
                onNavigateBack()
            }
        }
    }
    
    // Calculate department-level budget summary dynamically (live from phases/departments + expense stream)
    val deptBudgetState by approvalViewModel.deptBudgetState.collectAsState()

    // Setup live listener when expense changes
    LaunchedEffect(selectedExpenseForScreen) {
        selectedExpenseForScreen?.let { expense ->
            val projId = expense.projectId
            val phaId = expense.phaseId
            val dept = expense.department
            if (!projId.isNullOrEmpty() && !phaId.isNullOrEmpty() && !dept.isNullOrEmpty()) {
                approvalViewModel.startDepartmentBudgetListener(
                    projectId = projId,
                    phaseId = phaId,
                    departmentName = dept
                )
            }
        }
    }

    // Derived UI state for Budget Summary
    val budgetData = remember(deptBudgetState, selectedExpenseForScreen) {
        val totalBudget = deptBudgetState.totalBudget
        val remainingAmount = deptBudgetState.remainingAmount
        val spent = totalBudget - remainingAmount
        
        // Use expense.remainingBalance to calculate afterApproval
        val expenseBalance = selectedExpenseForScreen?.remainingBalance ?: selectedExpenseForScreen?.amount ?: 0.0
        val afterApproval = remainingAmount - expenseBalance

        object {
            val departmentBudget = totalBudget
            val departmentSpent = spent
            val remaining = remainingAmount
            val afterApproval = afterApproval
            val isLoading = deptBudgetState.isLoading
        }
    }

    val departmentBudget = budgetData.departmentBudget
    val departmentSpent = budgetData.departmentSpent
    val remaining = budgetData.remaining
    val afterApproval = budgetData.afterApproval

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
    ) {
        // Modern Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Review Expense",
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
            actions = {
                // Payment history icon moved to budget card
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colorScheme.primary,
                navigationIconContentColor = colorScheme.onPrimary,
                titleContentColor = colorScheme.onPrimary
            )
        )

        if (selectedExpenseForScreen == null) {
            // Loading or Error State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = colorScheme.primary
                        )
                        Text(
                            text = "Loading expense details...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Expense not found",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.error
                        )
                        Text(
                            text = "This expense may have been already processed or removed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary
                            )
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        } else {
            val context = LocalContext.current
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .background(
                        color = if (activeHighlightTag.isNotEmpty()) rowHighlightColor else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Amount Highlight Card - Most Important Information
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(amountBringIntoViewRequester),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAmountFieldHighlighted) rowHighlightColor else colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Expense Amount",
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = FormatUtils.formatCurrency(selectedExpense?.amount ?: 0.0),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onPrimaryContainer
                        )
                        
                        val remainingBalance = selectedExpense?.remainingBalance
                        if (remainingBalance != null && remainingBalance > 0 && remainingBalance < (selectedExpense?.amount ?: 0.0)) {
                            Surface(
                                color = colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    if (paymentHistory.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = colorScheme.error,
                                                    contentColor = colorScheme.onError
                                                ) {
                                                    Text(
                                                        text = paymentHistory.size.toString(),
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                        ) {
                                            IconButton(onClick = { showPaymentHistorySheet = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.Payments,
                                                    contentDescription = "Payment History",
                                                    tint = colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = "Remaining Unpaid Amount: ${FormatUtils.formatCurrency(remainingBalance)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
                Log.d("ReviewExpenseScreen1", "activeHighlightTag = '$activeHighlightTag', rowHighlightColor = $rowHighlightColor fromNotification = $fromNotification")

                // Expense Code Display (highlighted)
                if (selectedExpense?.expenseCode?.isNotEmpty() == true) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.secondaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = "Expense Code",
                                tint = colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Code: ${selectedExpense?.expenseCode}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Reviewer Note Section
                if (!isReadOnly){
                    Card(
                        modifier = Modifier.fillMaxWidth()
                        .bringIntoViewRequester(reviewerNoteBringIntoViewRequester)
                        .background(
                        color = colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp),
                        // shape = RoundedCornerShape(16.dp),
                        // colors = CardDefaults.cardColors(
                        //     containerColor = colorScheme.surfaceContainerHighest
                        // ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                    .background(
                                        color = if (isStatusFieldHighlighted) rowHighlightColor else colorScheme.surfaceContainerHighest)
                                        .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Reviewer Note",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = { onNavigateToExpenseChat(expenseId) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = "Chat with Submitter",
                                        tint = colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            if (selectedExpense?.isVerified != true && selectedExpense?.status == ExpenseStatus.PENDING) {
                                OutlinedTextField(
                                    value = reviewerNote,
                                    onValueChange = { reviewerNote = it },
                                    label = { Text("Add your note (optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colorScheme.primary,
                                        unfocusedBorderColor = colorScheme.outlineVariant,
                                        focusedLabelColor = colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    maxLines = 4,
                                    minLines = 2
                                )
                            } else {
                                // Display Review Comments with Material Design 3
                                selectedExpense?.let { expense ->
                                    val hasReviewComments = expense.reviewComments.isNotEmpty()
                                    val isApproved = expense.status == ExpenseStatus.APPROVED
                                    val isRejected = expense.status == ExpenseStatus.REJECTED

                                    if (hasReviewComments || expense.reviewedBy.isNotEmpty()) {
                                        // Review Status Card
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = when {
                                                isApproved -> colorScheme.primaryContainer
                                                isRejected -> colorScheme.errorContainer
                                                else -> colorScheme.surfaceContainerHighest
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // Status Header
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            8.dp
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isApproved)
                                                                Icons.Default.CheckCircle
                                                            else
                                                                Icons.Default.Info,
                                                            contentDescription = null,
                                                            tint = when {
                                                                isApproved -> colorScheme.onPrimaryContainer
                                                                isRejected -> colorScheme.onErrorContainer
                                                                else -> colorScheme.onSurfaceVariant
                                                            },
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Text(
                                                            text = when {
                                                                isApproved -> "Approved"
                                                                isRejected -> "Rejected"
                                                                else -> "Reviewed"
                                                            },
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = when {
                                                                isApproved -> colorScheme.onPrimaryContainer
                                                                isRejected -> colorScheme.onErrorContainer
                                                                else -> colorScheme.onSurfaceVariant
                                                            }
                                                        )
                                                    }

                                                    // Reviewed By and Date
                                                    if (expense.reviewedBy.isNotEmpty() || expense.reviewedAt != null) {
                                                        Column(
                                                            horizontalAlignment = Alignment.End
                                                        ) {
                                                            if (expense.reviewedBy.isNotEmpty()) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(
                                                                        4.dp
                                                                    )
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Person,
                                                                        contentDescription = null,
                                                                        tint = when {
                                                                            isApproved -> colorScheme.onPrimaryContainer.copy(
                                                                                alpha = 0.7f
                                                                            )

                                                                            isRejected -> colorScheme.onErrorContainer.copy(
                                                                                alpha = 0.7f
                                                                            )

                                                                            else -> colorScheme.onSurfaceVariant.copy(
                                                                                alpha = 0.7f
                                                                            )
                                                                        },
                                                                        modifier = Modifier.size(14.dp)
                                                                    )
                                                                    Text(
                                                                        text = expense.reviewedBy,
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = when {
                                                                            isApproved -> colorScheme.onPrimaryContainer.copy(
                                                                                alpha = 0.7f
                                                                            )

                                                                            isRejected -> colorScheme.onErrorContainer.copy(
                                                                                alpha = 0.7f
                                                                            )

                                                                            else -> colorScheme.onSurfaceVariant.copy(
                                                                                alpha = 0.7f
                                                                            )
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                            expense.reviewedAt?.let { reviewedAt ->
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(
                                                                        4.dp
                                                                    ),
                                                                    modifier = Modifier.padding(top = 4.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.AccessTime,
                                                                        contentDescription = null,
                                                                        tint = when {
                                                                            isApproved -> colorScheme.onPrimaryContainer.copy(
                                                                                alpha = 0.6f
                                                                            )

                                                                            isRejected -> colorScheme.onErrorContainer.copy(
                                                                                alpha = 0.6f
                                                                            )

                                                                            else -> colorScheme.onSurfaceVariant.copy(
                                                                                alpha = 0.6f
                                                                            )
                                                                        },
                                                                        modifier = Modifier.size(12.dp)
                                                                    )
                                                                    Text(
                                                                        text = FormatUtils.formatDate(
                                                                            reviewedAt,
                                                                            "MMM d, yyyy 'at' h:mm a"
                                                                        ),
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = when {
                                                                            isApproved -> colorScheme.onPrimaryContainer.copy(
                                                                                alpha = 0.6f
                                                                            )

                                                                            isRejected -> colorScheme.onErrorContainer.copy(
                                                                                alpha = 0.6f
                                                                            )

                                                                            else -> colorScheme.onSurfaceVariant.copy(
                                                                                alpha = 0.6f
                                                                            )
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                // Review Comments
                                                if (hasReviewComments) {
                                                    Divider(
                                                        color = when {
                                                            isApproved -> colorScheme.onPrimaryContainer.copy(
                                                                alpha = 0.2f
                                                            )

                                                            isRejected -> colorScheme.onErrorContainer.copy(
                                                                alpha = 0.2f
                                                            )

                                                            else -> colorScheme.onSurfaceVariant.copy(
                                                                alpha = 0.2f
                                                            )
                                                        },
                                                        modifier = Modifier.padding(vertical = 4.dp)
                                                    )

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.Top,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            8.dp
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Comment,
                                                            contentDescription = null,
                                                            tint = when {
                                                                isApproved -> colorScheme.onPrimaryContainer
                                                                isRejected -> colorScheme.onErrorContainer
                                                                else -> colorScheme.onSurfaceVariant
                                                            },
                                                            modifier = Modifier
                                                                .size(18.dp)
                                                                .padding(top = 2.dp)
                                                        )
                                                        Text(
                                                            text = expense.reviewComments,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = when {
                                                                isApproved -> colorScheme.onPrimaryContainer
                                                                isRejected -> colorScheme.onErrorContainer
                                                                else -> colorScheme.onSurface
                                                            },
                                                            lineHeight = 20.sp,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                } else {
                                                    // No comments provided
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            8.dp
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Comment,
                                                            contentDescription = null,
                                                            tint = when {
                                                                isApproved -> colorScheme.onPrimaryContainer.copy(
                                                                    alpha = 0.6f
                                                                )

                                                                isRejected -> colorScheme.onErrorContainer.copy(
                                                                    alpha = 0.6f
                                                                )

                                                                else -> colorScheme.onSurfaceVariant.copy(
                                                                    alpha = 0.6f
                                                                )
                                                            },
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Text(
                                                            text = "No review comments provided",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = when {
                                                                isApproved -> colorScheme.onPrimaryContainer.copy(
                                                                    alpha = 0.7f
                                                                )

                                                                isRejected -> colorScheme.onErrorContainer.copy(
                                                                    alpha = 0.7f
                                                                )

                                                                else -> colorScheme.onSurfaceVariant.copy(
                                                                    alpha = 0.7f
                                                                )
                                                            },
                                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // No review information available
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = colorScheme.surfaceContainerHighest,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "No review information available",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Expense Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surfaceContainerHighest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Expense Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        ModernDetailRow(
                            icon = Icons.Default.Business,
                            iconColor = colorScheme.primary,
                            label = "Department",
                            value = selectedExpense?.department?.let { FormatUtils.getDepartmentDisplayName(it) } ?: "N/A",
                            modifier = Modifier
                                .bringIntoViewRequester(departmentBringIntoViewRequester)
                                .background(
                                    color = if (isDepartmentFieldHighlighted) rowHighlightColor else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        )

                        selectedExpense?.phaseName?.let { phase ->
                            ModernDetailRow(
                                icon = Icons.Default.Label,
                                iconColor = colorScheme.secondary,
                                label = "Phase",
                                value = phase,
                                modifier = Modifier
                                    .bringIntoViewRequester(phaseBringIntoViewRequester)
                                    .background(
                                        color = if (isPhaseFieldHighlighted) rowHighlightColor else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            )
                        }

                        selectedExpense?.category?.let { category ->
                            if (category.isNotEmpty()) {
                                ModernDetailRow(
                                    icon = Icons.Default.Category,
                                    iconColor = colorScheme.tertiary,
                                    label = "Category",
                                    value = category,
                                    modifier = Modifier
                                        .bringIntoViewRequester(categoryBringIntoViewRequester)
                                        .background(
                                            color = if (isCategoryFieldHighlighted) rowHighlightColor else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                )
                            }
                        }

                        selectedExpense?.vendor?.let { vendor ->
                            if (vendor.isNotEmpty()) {
                                ModernDetailRow(
                                    icon = Icons.Default.ShoppingCart,
                                    iconColor = colorScheme.primary,
                                    label = "Vendor",
                                    value = vendor,
                                    modifier = Modifier
                                        .bringIntoViewRequester(vendorBringIntoViewRequester)
                                        .background(
                                            color = if (isVendorFieldHighlighted) rowHighlightColor else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                )
                            }
                        }

                        // By Credit Status - Material Design 3 Chip Style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(byCreditBringIntoViewRequester)
                                .background(
                                    color = if (isByCreditFieldHighlighted || isPaymentModeFieldHighlighted) rowHighlightColor else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "By Credit",
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "By Credit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            // Material Design 3 Chip for Credit Status
                            if (!isReadOnly && selectedExpense?.isVerified != true && selectedExpense?.status == ExpenseStatus.PENDING){
                                //Drop Down with Yes or no , approver/ verifier can cahnge the payment type by credit or not from this screen
                                var showByCreditDropdown by remember { mutableStateOf(false) }
                                val byCreditOptions = listOf("Yes", "No")
                                val selectedByCreditText = if (updateByCredict) "Yes" else "No"

                                ExposedDropdownMenuBox(
                                    expanded = showByCreditDropdown,
                                    onExpandedChange = { showByCreditDropdown = it },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = selectedByCreditText,
                                        onValueChange = { },
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showByCreditDropdown)
                                        },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .width(120.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colorScheme.primary,
                                            unfocusedBorderColor = colorScheme.outlineVariant,
                                            focusedLabelColor = colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        textStyle = MaterialTheme.typography.bodyMedium
                                    )

                                    ExposedDropdownMenu(
                                        expanded = showByCreditDropdown,
                                        onDismissRequest = { showByCreditDropdown = false }
                                    ) {
                                        byCreditOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    updateByCredict = option == "Yes"
                                                    showByCreditDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }else{
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (selectedExpense?.byCredit == true)
                                        colorScheme.primaryContainer
                                    else
                                        colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        )
                                    ) {
                                        if (selectedExpense?.byCredit == true) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = if (selectedExpense?.byCredit == true) "Yes" else "No",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selectedExpense?.byCredit == true)
                                                colorScheme.onPrimaryContainer
                                            else
                                                colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Date of Payment - Only show for BUSINESS_HEAD when status is APPROVED, isVerified is true, and byCredit is true
                        selectedExpense?.let { expense ->
                            val showDateOfPayment = userRole == UserRole.BUSINESS_HEAD.name &&
                                    expense.status == ExpenseStatus.APPROVED &&
                                    expense.isVerified == true &&
                                    expense.byCredit == true

                            if (showDateOfPayment) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(), // Needed for SpaceBetween to work!
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ){

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ){
                                            Icon(
                                                painter = painterResource(id = R.drawable.moneycalendar),
                                                contentDescription = "Date of Payment",
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Date of Payment",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Spacer(modifier = Modifier.weight(1f))
                                            // Display selected date or default
                                            val displayDate = selectedPaymentDate?.let {
                                                SimpleDateFormat(
                                                    "MMM d, yyyy",
                                                    Locale.getDefault()
                                                ).format(it)
                                            } ?: run {
                                                val defaultDate =
                                                    expense.creditPaymentDate?.toDate() ?: run {
                                                        val cal = Calendar.getInstance()
                                                        cal.time = expense.createdAt.toDate()
                                                        cal.add(Calendar.DAY_OF_YEAR, 30)
                                                        cal.time
                                                    }
                                                SimpleDateFormat(
                                                    "MMM d, yyyy",
                                                    Locale.getDefault()
                                                ).format(defaultDate)
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = colorScheme.surfaceContainerHighest,
                                                modifier = Modifier
                                                    .clickable { showDatePicker = true }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit date",
                                                        tint = colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = displayDate,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = colorScheme.onSurface
                                                    )
                                                }
                                            }

                                            // Days selector button
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = colorScheme.surfaceContainerHighest,
                                                modifier = Modifier
                                                    .clickable { showDaysSelector = true }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit date",
                                                        tint = colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "${selectedDays} days",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = colorScheme.onSurface
                                                    )
                                                }
                                            }

                                            // Save button
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        isSavingCreditDate = true
                                                        try {
                                                            val finalDate =
                                                                selectedPaymentDate ?: run {
                                                                    val cal = Calendar.getInstance()
                                                                    cal.time =
                                                                        expense.createdAt.toDate()
                                                                    cal.add(
                                                                        Calendar.DAY_OF_YEAR,
                                                                        selectedDays
                                                                    )
                                                                    cal.time
                                                                }
                                                            val timestamp = Timestamp(finalDate)
                                                            approvalViewModel.updateCreditPaymentDate(
                                                                expense,
                                                                timestamp
                                                            )
                                                        } catch (e: Exception) {

                                                        } finally {
                                                            isSavingCreditDate = false
                                                        }
                                                    }
                                                },
                                                enabled = !isSavingCreditDate && !isProcessing,
                                                modifier = Modifier.height(36.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = colorScheme.primary,
                                                    contentColor = colorScheme.onPrimary
                                                )
                                            ) {
                                                if (isSavingCreditDate) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        color = colorScheme.onPrimary,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Save,
                                                        contentDescription = "Save",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        ModernDetailRow(
                            icon = Icons.Default.CalendarToday,
                            iconColor = colorScheme.secondary,
                            label = "Date of Submission",
                            value = selectedExpense?.date?.let { FormatUtils.formatDate(it, "MMM d, yyyy") } ?: "N/A",
                            modifier = Modifier
                                .bringIntoViewRequester(dateBringIntoViewRequester)
                                .background(
                                    color = if (isDateFieldHighlighted) rowHighlightColor else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        )

                        ModernDetailRow(
                            icon = Icons.Default.Person,
                            iconColor = colorScheme.tertiary,
                            label = "Submitted By",
                            value = selectedExpense?.submittedBy?.ifBlank {
                                selectedExpense?.userName?.ifBlank { selectedExpense?.userId ?: "" } ?: ""
                            } ?: ""
                        )

                        Log.d("checkingSelcetedExpense","expense status is ${selectedExpense?.status} and in string is ${selectedExpense?.status.toString()}")

                        ModernStatusRow(
                            icon = Icons.Default.Person,
                            iconColor = colorScheme.tertiary,
                            label = "Status",
                            status = selectedExpense?.status,
                            modifier = Modifier
                                .bringIntoViewRequester(statusBringIntoViewRequester)
                                .background(
                                    color = if (isStatusFieldHighlighted) rowHighlightColor else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        )

                        selectedExpense?.remark?.let { remark ->
                            if (remark.isNotEmpty()) {
                                ModernDetailRow(
                                    icon = Icons.Default.Create,
                                    iconColor = colorScheme.onSurfaceVariant,
                                    label = "Remark",
                                    value = remark
                                )
                            }
                        }

                        // Payment Mode
                        selectedExpense?.let { expense ->
                            ModernDetailRow(
                                icon = Icons.Default.Payments,
                                iconColor = colorScheme.primary,
                                label = "Payment Mode",
                                value = PaymentMode.toString(expense.modeOfPayment),
                                modifier = Modifier
                                    .background(
                                        color = if (isPaymentModeFieldHighlighted) rowHighlightColor else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            )
                        }

                        // Contract Amount
                        selectedExpense?.contractAmount?.let { contractAmt ->
                            if (contractAmt > 0) {
                                ModernDetailRow(
                                    icon = Icons.Default.AccountBalanceWallet,
                                    iconColor = colorScheme.secondary,
                                    label = "Contract Amount",
                                    value = FormatUtils.formatCurrency(contractAmt)
                                )
                            }
                        }

                        // SGST
                        selectedExpense?.let { expense ->
                            if (expense.sgst.isNotEmpty()) {
                                ModernDetailRow(
                                    icon = Icons.Default.CurrencyRupee,
                                    iconColor = colorScheme.tertiary,
                                    label = "SGST",
                                    value = expense.sgst
                                )
                            }
                        }

                        // CGST
                        selectedExpense?.let { expense ->
                            if (expense.cgst.isNotEmpty()) {
                                ModernDetailRow(
                                    icon = Icons.Default.CurrencyRupee,
                                    iconColor = colorScheme.tertiary,
                                    label = "CGST",
                                    value = expense.cgst
                                )
                            }
                        }

                        // Receipt Number
                        selectedExpense?.let { expense ->
                            if (expense.receiptNumber.isNotEmpty()) {
                                ModernDetailRow(
                                    icon = Icons.Default.Description,
                                    iconColor = colorScheme.onSurfaceVariant,
                                    label = "Receipt No.",
                                    value = expense.receiptNumber
                                )
                            }
                        }
                    }
                }

                // Notes Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(descriptionBringIntoViewRequester),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDescriptionFieldHighlighted) rowHighlightColor else colorScheme.surfaceContainerHighest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Create,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Notes from Submitter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )
                        }
                        Text(
                            text = selectedExpense?.description?.ifEmpty { "No notes provided" } ?: "No notes provided",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Material Details Card
                selectedExpense?.let { expense ->
                    if (hasMaterialDetails(expense)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = colorScheme.surfaceContainerHighest
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Material Details",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSurface
                                    )
                                }

                                expense.itemType?.let { itemType ->
                                    ModernMaterialRow(
                                        icon = Icons.Default.Label,
                                        iconColor = colorScheme.primary,
                                        label = "Sub-category",
                                        value = itemType,
                                        modifier = Modifier
                                            .bringIntoViewRequester(subCategoryBringIntoViewRequester)
                                            .background(
                                                color = if (isSubCategoryFieldHighlighted) rowHighlightColor else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                    )
                                }

                                expense.item?.let { item ->
                                    ModernMaterialRow(
                                        icon = Icons.Default.ShoppingCart,
                                        iconColor = colorScheme.secondary,
                                        label = "Material",
                                        value = item,
                                        modifier = Modifier
                                            .bringIntoViewRequester(itemBringIntoViewRequester)
                                            .background(
                                                color = if (isItemFieldHighlighted) rowHighlightColor else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                    )
                                }

                                expense.brand?.let { brand ->
                                    ModernMaterialRow(
                                        icon = Icons.Default.Category,
                                        iconColor = colorScheme.tertiary,
                                        label = "Brand",
                                        value = brand,
                                        modifier = Modifier
                                            .bringIntoViewRequester(brandBringIntoViewRequester)
                                            .background(
                                                color = if (isBrandFieldHighlighted) rowHighlightColor else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                    )
                                }

                                expense.spec?.let { spec ->
                                    ModernMaterialRow(
                                        icon = Icons.Default.Star,
                                        iconColor = colorScheme.primary,
                                        label = "Grade",
                                        value = spec,
                                        modifier = Modifier
                                            .bringIntoViewRequester(specificationBringIntoViewRequester)
                                            .background(
                                                color = if (isSpecificationFieldHighlighted) rowHighlightColor else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                    )
                                }

                                expense.thickness?.let { thickness ->
                                    ModernMaterialRow(
                                        icon = Icons.Default.Height,
                                        iconColor = colorScheme.secondary,
                                        label = "Thickness",
                                        value = thickness,
                                        modifier = Modifier
                                            .bringIntoViewRequester(thicknessBringIntoViewRequester)
                                            .background(
                                                color = if (isThicknessFieldHighlighted) rowHighlightColor else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                    )
                                }

                                expense.quantity?.let { quantity ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bringIntoViewRequester(quantityBringIntoViewRequester)
                                            .background(
                                                color = if (isQuantityFieldHighlighted) rowHighlightColor else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Pin,
                                                contentDescription = null,
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Quantity:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = quantity,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = colorScheme.onSurface
                                            )
                                            expense.uom?.let { uom ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = colorScheme.secondaryContainer
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Balance,
                                                            contentDescription = null,
                                                            tint = colorScheme.onSecondaryContainer,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = uom,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = colorScheme.onSecondaryContainer
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                expense.unitPrice?.let { unitPrice ->
                                    val unitPriceDouble = parseNumber(unitPrice)
                                    val quantityDouble = expense.quantity?.let { parseNumber(it) } ?: 0.0
                                    val lineAmount = unitPriceDouble * quantityDouble

                                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bringIntoViewRequester(unitPriceBringIntoViewRequester)
                                            .background(
                                                color = if (isUnitPriceFieldHighlighted) rowHighlightColor else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CurrencyRupee,
                                                contentDescription = null,
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Unit Price:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = FormatUtils.formatCurrency(unitPriceDouble),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = colorScheme.primaryContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AttachMoney,
                                                    contentDescription = null,
                                                    tint = colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "Line Amount",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colorScheme.onPrimaryContainer
                                                )
                                            }
                                            Text(
                                                text = FormatUtils.formatCurrency(lineAmount),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Line Items Card (from expense.lineItems)
                selectedExpense?.let { expense ->
                    if (expense.lineItems.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = colorScheme.surfaceContainerHighest
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingCart,
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Line Items",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorScheme.onSurface
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = if (expense.lineItems.size == 1) "1 item" else "${expense.lineItems.size} items",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Divider()

                                expense.lineItems.forEachIndexed { index, lineItem ->
                                    ExpenseLineItemRow(
                                        index = index + 1,
                                        lineItem = lineItem,
                                        colorScheme = colorScheme
                                    )
                                    if (index < expense.lineItems.size - 1) {
                                        Divider(
                                            color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }

                                // Grand total row
                                val lineItemsTotal = expense.lineItems.sumOf { it.total }
                                if (lineItemsTotal > 0) {
                                    Divider(modifier = Modifier.padding(top = 4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = colorScheme.primaryContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AttachMoney,
                                                    contentDescription = null,
                                                    tint = colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "Line Items Total",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colorScheme.onPrimaryContainer
                                                )
                                            }
                                            Text(
                                                text = FormatUtils.formatCurrency(lineItemsTotal),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Attachment Card
                val hasAttachment = selectedExpense?.attachmentUrl?.isNotEmpty() == true ||
                                   selectedExpense?.attachmentFileName?.isNotEmpty() == true
                var isLoadingAttachment by remember { mutableStateOf(false) }
                var attachmentError by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(attachmentError) {
                    attachmentError?.let { error ->
                        android.widget.Toast.makeText(
                            context,
                            "Error: $error",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        attachmentError = null
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(attachmentBringIntoViewRequester)
                        .then(
                            if (hasAttachment && !isLoadingAttachment) {
                                Modifier.clickable {
                                    selectedExpense?.attachmentUrl?.let { url ->
                                        isLoadingAttachment = true

                                        selectedExpense?.attachmentFileName?.let { fileName ->
                                            ImageUriHelper.openFirebaseStorageFile(
                                                context = context,
                                                storageUrl = url,
                                                fileName = fileName,
                                                onError = { error ->
                                                    isLoadingAttachment = false
                                                    attachmentError = error
                                                },
                                                onSuccess = {
                                                    isLoadingAttachment = false
                                                }
                                            )
                                        } ?: run {
                                            ImageUriHelper.openFirebaseStorageFile(
                                                context = context,
                                                storageUrl = url,
                                                onError = { error ->
                                                    isLoadingAttachment = false
                                                    attachmentError = error
                                                },
                                                onSuccess = {
                                                    isLoadingAttachment = false
                                                }
                                            )
                                        }

                                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                                            .launch {
                                                kotlinx.coroutines.delay(5000)
                                                if (isLoadingAttachment) {
                                                    isLoadingAttachment = false
                                                }
                                            }
                                    } ?: run {
                                        attachmentError = "No attachment URL found"
                                    }
                                }
                            } else {
                                Modifier
                            }
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAttachmentFieldHighlighted) {
                            rowHighlightColor
                        } else if (hasAttachment) {
                            colorScheme.secondaryContainer
                        } else {
                            colorScheme.surfaceContainerHighest
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = if (hasAttachment) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Attachment",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (hasAttachment) {
                                        selectedExpense?.attachmentFileName?.ifEmpty { "File attached" } ?: "File attached"
                                    } else {
                                        "No attachment"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasAttachment) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (hasAttachment) {
                            if (isLoadingAttachment) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = colorScheme.secondary
                                )
                            } else {
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Tap to view",
                                    tint = colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // ─── Project Details Section ───────────────────────────
                projectBudgetSummary.project?.let { proj ->
                    val projCurrencySymbol = when (proj.currency) { "USD" -> "$" else -> "₹" }

                    // Project Overview Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHighest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Business, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text("Project Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                            }
                            Divider()
                            // Project code badge
                            if (proj.projectCode.isNotBlank()) {
                                Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(6.dp)) {
                                    Text(proj.projectCode, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                            ModernDetailRow(icon = Icons.Default.Description, iconColor = colorScheme.primary, label = "Project Name", value = proj.name.ifEmpty { "—" })
                            // Status chip
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text("Project Status", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
                                }
                                val statusColor = ProjectStatus.fromString(proj.status).getColor()
                                Surface(shape = RoundedCornerShape(12.dp), color = statusColor.copy(alpha = 0.15f)) {
                                    Text(ProjectStatus.fromString(proj.status).getDisplayText(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                }
                            }
                            ModernDetailRow(icon = Icons.Default.Person, iconColor = colorScheme.secondary, label = "Client", value = proj.client.ifEmpty { "Not specified" })
                            if (!proj.clientPrimaryNumber.isNullOrBlank()) {
                                ModernDetailRow(icon = Icons.Default.Person, iconColor = colorScheme.secondary, label = "Client Primary No.", value = proj.clientPrimaryNumber!!)
                            }
                            if (!proj.clientSecondaryNumber.isNullOrBlank()) {
                                ModernDetailRow(icon = Icons.Default.Person, iconColor = colorScheme.secondary, label = "Client Secondary No.", value = proj.clientSecondaryNumber!!)
                            }
                            ModernDetailRow(icon = Icons.Default.Business, iconColor = colorScheme.tertiary, label = "Location", value = proj.location.ifEmpty { "Not specified" })
                            ModernDetailRow(icon = Icons.Default.AttachMoney, iconColor = colorScheme.primary, label = "Currency", value = "$projCurrencySymbol ${proj.currency}")
                        }
                    }

                    // Project Budget Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHighest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text("Project Budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                            }
                            Divider()
                            ModernBudgetRow("Total Budget", proj.budget, colorScheme.onSurface)
                            ModernBudgetRow("Total Spent", projectBudgetSummary.totalSpent, colorScheme.onSurfaceVariant)
                            ModernBudgetRow("Total Remaining", projectBudgetSummary.totalRemaining, colorScheme.primary)
                            if (proj.remainingBalance != null) {
                                ModernBudgetRow("Balance on Record", proj.remainingBalance!!, colorScheme.tertiary)
                            }
                            // Spent progress bar
                            val spentPct = (projectBudgetSummary.spentPercentage / 100.0).toFloat().coerceIn(0f, 1f)
                            if (spentPct > 0f) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Budget Used", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                        Text("${String.format("%.1f", projectBudgetSummary.spentPercentage)}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = if (spentPct > 0.9f) colorScheme.error else colorScheme.primary)
                                    }
                                    LinearProgressIndicator(
                                        progress = { spentPct },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = if (spentPct > 0.9f) colorScheme.error else colorScheme.primary,
                                        trackColor = colorScheme.surfaceVariant
                                    )
                                }
                            }
                            // Department breakdown
                            if (projectBudgetSummary.departmentBreakdown.isNotEmpty()) {
                                Divider()
                                Text("BY DEPARTMENT", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
                                projectBudgetSummary.departmentBreakdown.forEach { dept ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(dept.department, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurface, modifier = Modifier.weight(1f))
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(FormatUtils.formatCurrency(dept.budgetAllocated), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = colorScheme.onSurface)
                                            Text("Spent: ${FormatUtils.formatCurrency(dept.spent)}", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Project Dates Card
                    val projPlannedStart = proj.plannedDate ?: proj.startDate
                    if (!projPlannedStart.isNullOrBlank() || !proj.endDate.isNullOrBlank() || !proj.handoverDate.isNullOrBlank() || !proj.maintenanceDate.isNullOrBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHighest),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Text("Project Dates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                                }
                                Divider()
                                if (!projPlannedStart.isNullOrBlank()) ModernDetailRow(Icons.Default.CalendarToday, colorScheme.primary, "Planned Start", projPlannedStart)
                                if (!proj.endDate.isNullOrBlank()) ModernDetailRow(Icons.Default.CalendarToday, colorScheme.secondary, "End Date", proj.endDate!!)
                                if (!proj.handoverDate.isNullOrBlank()) ModernDetailRow(Icons.Default.CalendarToday, colorScheme.tertiary, "Handover Date", proj.handoverDate!!)
                                if (!proj.maintenanceDate.isNullOrBlank()) ModernDetailRow(Icons.Default.CalendarToday, colorScheme.onSurfaceVariant, "Maintenance Date", proj.maintenanceDate!!)
                            }
                        }
                    }

                    // Project Team Card
                    val hasTeamInfo = !proj.managerId.isNullOrEmpty() || proj.teamMembers.isNotEmpty() ||
                        !proj.approverIds.isNullOrEmpty() || !proj.BusinessHeadIds.isNullOrEmpty() ||
                        proj.departmentUserAssignments.isNotEmpty() || proj.departmentApproverAssignments.isNotEmpty()
                    if (hasTeamInfo) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHighest),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Text("Project Team", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                                }
                                Divider()
                                if (!proj.managerId.isNullOrEmpty()) {
                                    ProjectMemberRow("Manager", proj.managerId!!, colorScheme.primary, colorScheme)
                                }
                                proj.teamMembers.forEach { member ->
                                    ProjectMemberRow("Team Member", member, colorScheme.secondary, colorScheme)
                                }
                                proj.approverIds?.forEach { approver ->
                                    ProjectMemberRow("Approver", approver, Color(0xFF34C759), colorScheme)
                                }
                                proj.BusinessHeadIds?.forEach { bh ->
                                    ProjectMemberRow("Business Head", bh, Color(0xFF9C27B0), colorScheme)
                                }
                                if (proj.departmentUserAssignments.isNotEmpty()) {
                                    Divider()
                                    Text("DEPT. USER ASSIGNMENTS", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
                                    proj.departmentUserAssignments.forEach { (dept, phone) ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(dept, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                            Text(phone, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurface)
                                        }
                                    }
                                }
                                if (proj.departmentApproverAssignments.isNotEmpty()) {
                                    Divider()
                                    Text("DEPT. APPROVER ASSIGNMENTS", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
                                    proj.departmentApproverAssignments.forEach { (dept, phone) ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(dept, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                            Text(phone, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Project Phases Card
                    if (phasesWithDepartments.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHighest),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Category, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Text("Project Phases (${phasesWithDepartments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                                }
                                Divider()
                                phasesWithDepartments.forEachIndexed { idx, phaseData ->
                                    val phase = phaseData.phase
                                    val phaseDepts = phaseData.departments
                                    val phaseTotalBudget = phase.totalBudgetValue ?: phaseDepts.sumOf { it.totalBudget }
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = "Phase ${idx + 1}${if (phase.phaseName.isNotBlank()) ": ${phase.phaseName}" else ""}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.onSurface
                                                )
                                                if (phase.startDate != null || phase.endDate != null) {
                                                    Text("${phase.startDate ?: "?"} → ${phase.endDate ?: "?"}", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Text("$projCurrencySymbol ${String.format("%.0f", phaseTotalBudget)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                                        }
                                        // Departments
                                        if (phaseDepts.isNotEmpty()) {
                                            phaseDepts.forEach { dept ->
                                                Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                                        Box(modifier = Modifier.size(6.dp).background(colorScheme.secondary, shape = RoundedCornerShape(3.dp)))
                                                        Text(dept.name, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text("$projCurrencySymbol ${String.format("%.0f", dept.totalBudget)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = colorScheme.onSurface)
                                                        if (dept.remainingAmount > 0) {
                                                            Text("Rem: $projCurrencySymbol ${String.format("%.0f", dept.remainingAmount)}", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                }
                                            }
                                        } else if (phase.departments.isNotEmpty()) {
                                            phase.departments.forEach { (deptName, deptBudget) ->
                                                Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(deptName, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                                    Text("$projCurrencySymbol ${String.format("%.0f", deptBudget)}", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface)
                                                }
                                            }
                                        }
                                    }
                                    if (idx < phasesWithDepartments.size - 1) Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }

                    // Project Description Card
                    if (proj.description.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHighest),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Text("Project Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                                }
                                Text(proj.description, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                            }
                        }
                    }
                }
                // ──────────────────────────────────────────────────────────

                // Budget Summary Card
                val expenseStatus = selectedExpense?.status
                val shouldShowBudget = (expenseStatus == ExpenseStatus.PENDING || expenseStatus == ExpenseStatus.APPROVED)
                        && currentViewerRole != UserRole.USER
                        
                if (shouldShowBudget) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.surfaceContainerHighest
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Budget Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface
                                )
                                if (budgetData.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = colorScheme.primary
                                    )
                                }
                            }

                            Divider()

                            val displayBudget = if (budgetData.isLoading) 0.0 else budgetData.departmentBudget
                            val displaySpent = if (budgetData.isLoading) 0.0 else budgetData.departmentSpent
                            val displayRemaining = if (budgetData.isLoading) 0.0 else budgetData.remaining
                            val displayAfterApproval = if (budgetData.isLoading) 0.0 else budgetData.afterApproval

                            ModernBudgetRow("Budget", displayBudget, colorScheme.onSurface)
                            ModernBudgetRow("Spent", displaySpent, colorScheme.onSurfaceVariant)
                            ModernBudgetRow("Remaining", displayRemaining, colorScheme.primary)

                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (displayAfterApproval >= 0)
                                    colorScheme.tertiaryContainer
                                else
                                    colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "After Payment:",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (displayAfterApproval >= 0)
                                            colorScheme.onTertiaryContainer
                                        else
                                            colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = FormatUtils.formatCurrency(displayAfterApproval),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (displayAfterApproval >= 0)
                                            colorScheme.onTertiaryContainer
                                        else
                                            colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
                
                

                // Action Buttons
                if (!isReadOnly) {
                    // Request Manager Toggle - shown to BusinessHead for APPROVED + byCredit + isToManager == false
                    val showRequestManagerToggle = userRole == UserRole.BUSINESS_HEAD.name &&
                        selectedExpense?.status == ExpenseStatus.APPROVED &&
                        selectedExpense?.byCredit == true &&
                        selectedExpense?.isToManager == false

                    if (showRequestManagerToggle) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHighest),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Request Manager to Complete Payment",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = requestManagerToggle,
                                    onCheckedChange = { requestManagerToggle = it }
                                )
                            }
                        }
                    }

                    val expenseAmount = selectedExpense?.amount ?: 0.0
                    val currentUserPhone = authState.user?.phone?.trim()?.removePrefix("+91") ?: ""

                    // BH sees Update Payment only when: verified + APPROVED + (byCredit OR large amount) + isToManager is NOT true
                    val shouldShowUpdatePaymentForBusinessHead = selectedExpense?.isVerified == true &&
                        selectedExpense?.status == ExpenseStatus.APPROVED &&
                        userRole == UserRole.BUSINESS_HEAD.name &&
                        (selectedExpense?.byCredit == true || expenseAmount > 99999) &&
                        selectedExpense?.isToManager != true

                    val shouldShowUpdatePaymentForApprover = selectedExpense?.isVerified == true &&
                        selectedExpense?.status == ExpenseStatus.APPROVED &&
                        expenseAmount < 100000 &&
                        userRole == UserRole.APPROVER.name &&
                        selectedExpense?.submittedBy == currentUserPhone

                    val shouldShowUpdatePaymentForUser = selectedExpense?.isVerified == true &&
                        selectedExpense?.status == ExpenseStatus.APPROVED &&
                        expenseAmount < 100000 &&
                        userRole == UserRole.USER.name &&
                        selectedExpense?.submittedBy == currentUserPhone

                    // Manager sees Update Payment only when: APPROVED + byCredit == true + isToManager == true
                    val shouldShowUpdatePaymentForManager = selectedExpense?.isVerified == true &&
                        selectedExpense?.status == ExpenseStatus.APPROVED &&
                        userRole?.uppercase() == "MANAGER" &&
                        selectedExpense?.byCredit == true &&
                        selectedExpense?.isToManager == true

                    val isPending = selectedExpense?.isVerified != true && selectedExpense?.status == ExpenseStatus.PENDING
                    val showMainActionButton = isPending ||
                        shouldShowUpdatePaymentForBusinessHead ||
                        shouldShowUpdatePaymentForApprover ||
                        shouldShowUpdatePaymentForUser ||
                        shouldShowUpdatePaymentForManager

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (showMainActionButton) {
                            Button(
                                onClick = {
                                    activeReviewAction = "approve"
                                    if (isPending) {
                                        selectedExpense?.let { expense ->
                                            approvalViewModel.approveExpense(
                                                expense = expense,
                                                verifiedBy = if (userRole == UserRole.BUSINESS_HEAD.name) "BusinessHead" else currentUserPhone,
                                                comments = reviewerNote.ifEmpty { "" },
                                                userRole = userRole,
                                                byCredit = updateByCredict
                                            )
                                        }
                                    } else {
                                        if (requestManagerToggle && userRole?.uppercase() == "BUSINESS_HEAD") {
                                            showRequestManagerConfirmDialog = true
                                        } else {
                                            showPaymentTypeDialog = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPending) Color(0xFF2196F3) else Color(0xFF4CAF50),
                                    contentColor = Color.White
                                ),
                                enabled = !isProcessing,
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 4.dp
                                )
                            ) {
                                if (isProcessing && activeReviewAction == "approve") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isPending) "Approving..." else "Updating...",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    val buttonLabel = when {
                                        isPending -> "Approve"
                                        requestManagerToggle && userRole?.uppercase() == "BUSINESS_HEAD" -> "Request Manager"
                                        else -> "Update Payment"
                                    }
                                    Text(
                                        text = buttonLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (selectedExpense?.isVerified != true && selectedExpense?.status == ExpenseStatus.PENDING){
                            Button(
                                onClick = {
                                    activeReviewAction = "reject"
                                    val currentUser = authState.user
                                    val reviewerName = currentUser?.name?.takeIf { it.isNotEmpty() }
                                        ?: "System Reviewer"

                                    selectedExpense?.let { expense ->
                                        approvalViewModel.rejectExpense(
                                            expense = expense,
                                            reviewerName = reviewerName,
                                            comments = reviewerNote.ifEmpty { "Rejected by $reviewerName" },
                                            userRole = userRole, // Pass userRole to ViewModel
                                            byCredit = updateByCredict
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.error,
                                    contentColor = colorScheme.onError,
                                    disabledContainerColor = colorScheme.surfaceVariant
                                ),
                                enabled = !isProcessing,
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 4.dp
                                )
                                ) {
                                if (isProcessing && activeReviewAction == "reject") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = colorScheme.onError,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Rejecting...",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = colorScheme.onError
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Reject",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                
                
                // Error Message
                error?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
    
    // Payment History Bottom Sheet
    if (showPaymentHistorySheet) {
        PaymentHistoryBottomSheet(
            paymentHistory = paymentHistory,
            context = context,
            onDismiss = { showPaymentHistorySheet = false }
        )
    }

    // Payment Update Modal Bottom Sheet
    if (showPaymentUpdateModal) {
        PaymentUpdateBottomSheet(
            selectedPaymentMode = selectedPaymentMode ?: PaymentMode.CASH,
            onPaymentModeSelected = { selectedPaymentMode = it },
            selectedPaymentProofUri = selectedPaymentProofUri,
            selectedPaymentProofName = selectedPaymentProofName,
            onPaymentProofSelected = { showPaymentProofDialog = true },
            onRemovePaymentProof = {
                selectedPaymentProofUri = null
                selectedPaymentProofName = null
            },
            isUploading = isUploadingPaymentProof || isProcessing,
            onSave = {
                selectedExpense?.let { expense ->
                    scope.launch {
                        try {
                            // Upload payment proof if selected
                            var finalPaymentProofURL: String? = null
                            var finalPaymentProofName: String? = null
                            
                            if (selectedPaymentProofUri != null) {
                                isUploadingPaymentProof = true
                                try {
                                    // Use ViewModel method to upload payment proof
                                    val uploadResult = approvalViewModel.uploadPaymentProofAndUpdate(
                                        expense = expense,
                                        paymentProofUri = selectedPaymentProofUri!!,
                                        paymentProofName = selectedPaymentProofName ?: "payment_proof_${System.currentTimeMillis()}.jpg",
                                        paymentMode = selectedPaymentMode ?: PaymentMode.CASH
                                    )
                                    
                                    if (uploadResult.isSuccess) {
                                        val (url, name) = uploadResult.getOrNull()!!
                                        finalPaymentProofURL = url
                                        finalPaymentProofName = name
                                    } else {
                                        isUploadingPaymentProof = false
                                        return@launch
                                    }
                                } catch (e: Exception) {
                                    isUploadingPaymentProof = false
                                    return@launch
                                }
                                isUploadingPaymentProof = false
                            }
                            
                            // Determine paidBy: named roles use display name, others use phone
                            val currentUser = authState.user
                            val paidBy = when (userRole?.uppercase()) {
                                "BUSINESS_HEAD" -> "BusinessHead"
                                "MANAGER" -> "Manager"
                                else -> currentUser?.phone?.trim()?.removePrefix("+91") ?: ""
                            }

                            // Treat as full payment if partial amount equals the remaining balance
                            val paymentAmount = partialPaymentAmount.toDoubleOrNull() ?: 0.0
                            val expenseRemainingBalance = expense.remainingBalance ?: expense.amount
                            val isPartialPayment = partialPaymentAmount.isNotBlank() &&
                                    paymentAmount < expenseRemainingBalance

                            // Call the appropriate payment method
                            if (isPartialPayment) {
                                approvalViewModel.updateExpensePartialPayment(
                                    expense = expense,
                                    paymentAmount = paymentAmount,
                                    paymentMode = selectedPaymentMode ?: PaymentMode.CASH,
                                    paymentProofURL = finalPaymentProofURL,
                                    paymentProofName = finalPaymentProofName,
                                    paidBy = paidBy
                                )
                            } else {
                                approvalViewModel.updateExpensePayment(
                                    expense = expense,
                                    paymentMode = selectedPaymentMode ?: PaymentMode.CASH,
                                    paymentProofURL = finalPaymentProofURL,
                                    paymentProofName = finalPaymentProofName,
                                    paidBy = paidBy
                                )
                            }
                            
                            // Wait for processing to complete by observing isProcessing state
                            // Give it a maximum of 10 seconds
                            var waitTime = 0
                            while (isProcessing && waitTime < 10000) {
                                kotlinx.coroutines.delay(100)
                                waitTime += 100
                            }
                            
                            // Additional small delay to ensure Firestore operations complete
                            kotlinx.coroutines.delay(500)
                            
                            // Only dismiss and navigate after successful completion
                            showPaymentUpdateModal = false
                            partialPaymentAmount = ""
                            
                            // Navigate back
                            kotlinx.coroutines.delay(300)
                            onNavigateBack()
                            
                        } catch (e: Exception) {
                            // Keep modal open on error so user can retry
                        }
                    }
                }
            },
            onDismiss = { showPaymentUpdateModal = false }
        )
    }
    
    // Payment Proof Selection Dialog
    if (showPaymentProofDialog) {
        PaymentProofSelectionDialog(
            onCameraSelected = {
                showPaymentProofDialog = false
                when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                    PackageManager.PERMISSION_GRANTED -> {
                        val tempUri = context.contentResolver.insert(
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            android.content.ContentValues()
                        )
                        selectedPaymentProofUri = tempUri
                        tempUri?.let { paymentProofCameraLauncher.launch(it) }
                    }
                    else -> {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            },
            onPhotoSelected = {
                showPaymentProofDialog = false
                paymentProofPhotoPickerLauncher.launch("image/*")
            },
            onPdfSelected = {
                showPaymentProofDialog = false
                paymentProofPdfPickerLauncher.launch("application/pdf")
            },
            onDismiss = { showPaymentProofDialog = false }
        )
    }
    
    // Credit Payment Date Picker Dialog
    selectedExpense?.let { expense ->
        if (showDatePicker) {
            val initialDateMillis = selectedPaymentDate?.time ?: run {
                val cal = Calendar.getInstance()
                cal.time = expense.createdAt.toDate()
                cal.add(Calendar.DAY_OF_YEAR, 30)
                cal.timeInMillis
            }
            
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = initialDateMillis
            )
            
            androidx.compose.material3.DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                            selectedPaymentDate = Date(millis)
                            // Calculate days from createdAt
                            val createdAt = expense.createdAt.toDate()
                            val daysDiff = ((millis - createdAt.time) / (1000 * 60 * 60 * 24)).toInt()
                            if (daysDiff > 0) {
                                selectedDays = daysDiff
                            }
                            showDatePicker = false
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                modifier = Modifier.padding(16.dp),
                title = {
                    Text(
                        text = "Select Payment Date",
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
    }
    
    // Payment Type Selection Dialog (Full/Partial)
    if (showPaymentTypeDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentTypeDialog = false },
            title = {
                Text(
                    text = "Select Payment Type",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "How would you like to process this payment?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    
                    // Full Payment Option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPaymentTypeDialog = false
                                showPaymentUpdateModal = true
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Full Payment",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onPrimaryContainer
                                )
                                selectedExpense?.let { expense ->
                                    val remainingAmount = expense.remainingBalance ?: expense.amount
                                    Text(
                                        text = FormatUtils.formatCurrency(remainingAmount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Partial Payment Option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPaymentTypeDialog = false
                                partialPaymentAmount = ""
                                partialPaymentError = null
                                showPartialPaymentAmountDialog = true
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Partial Payment",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Pay a portion of the amount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPaymentTypeDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    // Partial Payment Amount Dialog
    if (showPartialPaymentAmountDialog) {
        val remainingAmount = selectedExpense?.remainingBalance ?: selectedExpense?.amount ?: 0.0
        
        AlertDialog(
            onDismissRequest = { 
                showPartialPaymentAmountDialog = false
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
                        text = "Remaining Balance: ${FormatUtils.formatCurrency(remainingAmount)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.primary
                    )
                    
                    OutlinedTextField(
                        value = partialPaymentAmount,
                        onValueChange = { 
                            partialPaymentAmount = it
                            partialPaymentError = null
                        },
                        label = { Text("Payment Amount") },
                        placeholder = { Text("Enter amount") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = partialPaymentError != null,
                        supportingText = {
                            partialPaymentError?.let {
                                Text(
                                    text = it,
                                    color = colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CurrencyRupee,
                                contentDescription = null
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant,
                            focusedLabelColor = colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = partialPaymentAmount.toDoubleOrNull()
                        when {
                            amount == null || amount <= 0 -> {
                                partialPaymentError = "Please enter a valid amount"
                            }
                            amount > remainingAmount -> {
                                partialPaymentError = "Amount cannot exceed remaining balance"
                            }
                            else -> {
                                // Valid amount, proceed to payment method selection
                                showPartialPaymentAmountDialog = false
                                howPaymentOpened = "PartialPayment"
                                showPaymentUpdateModal = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary
                    )
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showPartialPaymentAmountDialog = false
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

    
    // Days Selector Dialog
    if (showDaysSelector) {
        var tempDays by remember { mutableStateOf(selectedDays.toString()) }
        
        AlertDialog(
            onDismissRequest = { showDaysSelector = false },
            title = {
                Text(
                    text = "Select Days to Repay",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempDays,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                tempDays = newValue
                            }
                        },
                        label = { Text("Number of days") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        )
                    )
                    
                    // Quick select buttons
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 45, 60, 90).forEach { days ->
                            FilterChip(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                selected = tempDays == days.toString(),
                                onClick = { tempDays = days.toString() },
                                label = { Text("$days days") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.primaryContainer,
                                    selectedLabelColor = colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = tempDays.toIntOrNull() ?: 30
                        selectedDays = days.coerceIn(1, 365)
                        
                        // Calculate date from days
                        selectedExpense?.let { expense ->
                            val cal = Calendar.getInstance()
                            cal.time = expense.createdAt.toDate()
                            cal.add(Calendar.DAY_OF_YEAR, selectedDays)
                            selectedPaymentDate = cal.time
                        }
                        
                        showDaysSelector = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDaysSelector = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = colorScheme.surface
        )
    }

    // Request Manager Confirmation Dialog
    if (showRequestManagerConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRequestManagerConfirmDialog = false },
            title = {
                Text(
                    text = "Request Manager to Complete Payment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to request the manager to complete this payment? You will no longer be able to complete the payment yourself.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRequestManagerConfirmDialog = false
                        selectedExpense?.let { expense ->
                            approvalViewModel.updateIsToManager(expense, true)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRequestManagerConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun ProjectMemberRow(
    label: String,
    value: String,
    dotColor: Color,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(0.4f)
        ) {
            Box(modifier = Modifier.size(8.dp).background(dotColor, shape = RoundedCornerShape(4.dp)))
            Text(label, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurface, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
    }
}

@Composable
private fun ModernDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier

) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ModernStatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    status: ExpenseStatus?,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor) = when (status) {
        ExpenseStatus.REJECTED -> "Rejected" to Color(0xFFC62828)
        ExpenseStatus.PENDING -> "Pending" to Color(0xFFF9A825)
        ExpenseStatus.APPROVED -> "Accepted" to Color(0xFF1565C0)
        ExpenseStatus.COMPLETE -> "Complete" to Color(0xFF2E7D32)
        null -> "Unknown" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = statusColor.copy(alpha = 0.14f)
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun ModernMaterialRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun ModernBudgetRow(
    label: String,
    amount: Double,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = FormatUtils.formatCurrency(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentHistoryBottomSheet(
    paymentHistory: List<com.cubiquitous.tracura.model.PaymentHistory>,
    context: android.content.Context,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Surface(
                    shape = CircleShape,
                    color = colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${paymentHistory.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            Divider()
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(paymentHistory) { record ->
                    PaymentHistoryCard(
                        record = record,
                        context = context,
                        colorScheme = colorScheme
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun PaymentHistoryCard(
    record: com.cubiquitous.tracura.model.PaymentHistory,
    context: android.content.Context,
    colorScheme: androidx.compose.material3.ColorScheme
) {
    var isLoadingProof by remember { mutableStateOf(false) }
    var proofError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(proofError) {
        proofError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            proofError = null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Amount + Payment Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = FormatUtils.formatCurrency(record.amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.secondaryContainer
                ) {
                    Text(
                        text = PaymentMode.toString(record.paymentMode),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Divider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Paid By
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Paid by:",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = record.paidBy.ifEmpty { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
            }

            // Paid At
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = record.getDateFormatted(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            // Payment Proof row (only when URL is present)
            if (!record.paymentProofURL.isNullOrEmpty()) {
                Divider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = record.paymentProofName?.ifEmpty { "Payment Proof" } ?: "Payment Proof",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    if (isLoadingProof) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colorScheme.secondary
                        )
                    } else {
                        IconButton(
                            onClick = {
                                isLoadingProof = true
                                ImageUriHelper.openFirebaseStorageFile(
                                    context = context,
                                    storageUrl = record.paymentProofURL,
                                    fileName = record.paymentProofName ?: "payment_proof",
                                    onError = { err ->
                                        isLoadingProof = false
                                        proofError = err
                                    },
                                    onSuccess = { isLoadingProof = false }
                                )
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open proof",
                                tint = colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseLineItemRow(
    index: Int,
    lineItem: com.cubiquitous.tracura.model.DepartmentLineItemData,
    colorScheme: androidx.compose.material3.ColorScheme
) {
    val itemColor = lineItemColor(lineItem.itemType)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sub-row 1: colored index badge + item type tag / name / spec + trailing total
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Colored circle badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .background(itemColor.copy(alpha = 0.15f), CircleShape)
            ) {
                Text(
                    text = "$index",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = itemColor
                )
            }

            // Item type tag + item name + spec
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (lineItem.itemType.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = itemColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = lineItem.itemType,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = itemColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                if (lineItem.item.isNotEmpty()) {
                    Text(
                        text = lineItem.item,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                }
                if (lineItem.spec.isNotEmpty()) {
                    Text(
                        text = lineItem.spec,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            // Trailing total
            if (lineItem.total > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatUtils.formatCurrency(lineItem.total),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Sub-row 2: chips row (quantity, unit price, remaining)
        val showQtyChip = lineItem.quantity > 0
        val showPriceChip = lineItem.unitPrice > 0
        val showRemChip = lineItem.remainingQuantity > 0 && lineItem.remainingQuantity < lineItem.quantity
        if (showQtyChip || showPriceChip || showRemChip) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showQtyChip) {
                    val qtyLabel = if (lineItem.uom.isNotEmpty())
                        "${lineItem.quantity.cleanQuantity()} ${lineItem.uom}"
                    else
                        lineItem.quantity.cleanQuantity()
                    LineItemChip(
                        icon = Icons.Default.Pin,
                        label = qtyLabel,
                        color = Color(0xFF2196F3)
                    )
                }
                if (showPriceChip) {
                    LineItemChip(
                        icon = Icons.Default.CurrencyRupee,
                        label = "${FormatUtils.formatCurrency(lineItem.unitPrice)}/unit",
                        color = Color(0xFFFF9800)
                    )
                }
                if (showRemChip) {
                    val remLabel = if (lineItem.uom.isNotEmpty())
                        "Rem: ${lineItem.remainingQuantity.cleanQuantity()} ${lineItem.uom}"
                    else
                        "Rem: ${lineItem.remainingQuantity.cleanQuantity()}"
                    LineItemChip(
                        icon = Icons.Default.ArrowDropDown,
                        label = remLabel,
                        color = Color(0xFF9C27B0)
                    )
                }
            }
        }
    }
}

@Composable
private fun LineItemChip(
    icon: ImageVector,
    label: String,
    color: Color
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

private fun lineItemColor(itemType: String): Color {
    val lower = itemType.lowercase()
    return when {
        lower.contains("raw") -> Color(0xFF795548)
        lower.contains("labour") -> Color(0xFF3F51B5)
        lower.contains("machine") -> Color(0xFF607D8B)
        lower.contains("electrical") -> Color(0xFFFFC107)
        lower.contains("plumbing") -> Color(0xFF2196F3)
        lower.contains("flooring") -> Color(0xFFFF9800)
        lower.contains("tile") || lower.contains("granite") -> Color(0xFF009688)
        lower.contains("sanitary") -> Color(0xFF00BCD4)
        lower.contains("paint") -> Color(0xFFE91E63)
        lower.contains("carpentry") -> Color(0xFF795548)
        lower.contains("glass") -> Color(0xFF26A69A)
        lower.contains("ceiling") -> Color(0xFF9C27B0)
        lower.contains("hardware") -> Color(0xFF607D8B)
        lower.contains("water") -> Color(0xFF009688)
        lower.contains("tool") -> Color(0xFFF44336)
        else -> Color(0xFF6650A4)
    }
}

private fun Double.cleanQuantity(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()

private fun hasMaterialDetails(expense: Expense): Boolean {
    return expense.itemType != null || expense.item != null || expense.brand != null ||
            expense.spec != null || expense.thickness != null || expense.quantity != null ||
            expense.uom != null || expense.unitPrice != null
}

private fun parseNumber(value: String): Double {
    return try {
        value.replace(",", "").replace(" ", "").toDouble()
    } catch (e: Exception) {
        0.0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentUpdateBottomSheet(
    selectedPaymentMode: PaymentMode,
    onPaymentModeSelected: (PaymentMode) -> Unit,
    selectedPaymentProofUri: Uri?,
    selectedPaymentProofName: String?,
    onPaymentProofSelected: () -> Unit,
    onRemovePaymentProof: () -> Unit,
    isUploading: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Title
            Text(
                text = "Update Payment",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            
            // Payment Mode Selection
            Text(
                text = "Payment Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PaymentMode.values().forEach { mode ->
                    PaymentModeOption(
                        mode = mode,
                        isSelected = selectedPaymentMode == mode,
                        onClick = { onPaymentModeSelected(mode) }
                    )
                }
            }
            
            // Payment Proof Section (only for UPI, CHEQUE, CARD)
            if (selectedPaymentMode != PaymentMode.CASH) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Payment Proof",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                
                if (selectedPaymentProofUri != null) {
                    // Show selected file
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = colorScheme.onSecondaryContainer
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedPaymentProofName ?: "Payment Proof",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = colorScheme.onSecondaryContainer,
                                        maxLines = 1
                                    )
                                }
                            }
                            IconButton(onClick = onRemovePaymentProof) {
                                Icon(
                                    imageVector = Icons.Default.Create,
                                    contentDescription = "Remove",
                                    tint = colorScheme.error
                                )
                            }
                        }
                    }
                } else {
                    // Upload button
                    OutlinedButton(
                        onClick = onPaymentProofSelected,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Payment Proof")
                    }
                }
            }
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = !isUploading && selectedPaymentMode != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PaymentModeOption(
    mode: PaymentMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, colorScheme.primary)
        } else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = PaymentMode.toString(mode),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
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
    icon: ImageVector,
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
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant
            )
        }
    }
}
