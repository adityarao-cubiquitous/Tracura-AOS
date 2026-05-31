package com.cubiquitous.tracura.ui.view.user

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.PaymentMode
import com.cubiquitous.tracura.model.LineItem
import com.cubiquitous.tracura.model.ContractorMode
import com.cubiquitous.tracura.viewmodel.ExpenseViewModel
import com.cubiquitous.tracura.viewmodel.NotificationViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.ui.components.BudgetWarningComponent
import com.cubiquitous.tracura.ui.components.BudgetExceededDialog
import com.cubiquitous.tracura.ui.common.AddLineItemModalSheet
import com.cubiquitous.tracura.utils.FormatUtils
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.model.ReceiptResult

private data class IosAddExpensePalette(
    val tier2Surface: Color,
    val tier3Field: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val hairline: Color,
)

@Composable
private fun rememberIosAddExpensePalette(): IosAddExpensePalette {
    val isDark = isSystemInDarkTheme()
    return remember(isDark) {
        IosAddExpensePalette(
            tier2Surface = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
            tier3Field = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF),
            textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
            textSecondary = if (isDark) Color(0x99EBEBF5) else Color(0x993C3C43),
            hairline = if (isDark) Color(0xFF38383A) else Color(0x1F000000),
        )
    }
}

private fun normalizePhoneForCompare(phone: String?): String {
    if (phone.isNullOrBlank()) return ""
    val digits = phone.filter { it.isDigit() }
    return if (digits.length > 10) digits.takeLast(10) else digits
}

private fun Date.atStartOfDay(): Date {
    val cal = Calendar.getInstance().apply {
        time = this@atStartOfDay
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.time
}

private fun isActiveTempApproverForDate(
    entry: com.cubiquitous.tracura.model.DepartmentTemporaryApproverEntry,
    normalizedPhone: String,
    today: Date
): Boolean {
    if (normalizePhoneForCompare(entry.phone) != normalizePhoneForCompare(normalizedPhone)) return false
    if (!entry.isAccepted || !entry.isActive) return false
    val todayDay = today.atStartOfDay()
    val startDate = entry.startDate?.atStartOfDay() ?: return false
    val hasStarted = !todayDay.before(startDate)
    val isBeforeOrOnEndDate = entry.endDate?.atStartOfDay()?.let { !todayDay.after(it) } ?: true
    return hasStarted && isBeforeOrOnEndDate
}

// Helper function for Material 3 OutlinedTextField colors
@Composable
private fun getTextFieldColors(
    isError: Boolean = false,
    isEnabled: Boolean = true,
    colorScheme: ColorScheme = MaterialTheme.colorScheme
) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = if (isError) colorScheme.error else colorScheme.primary,
    unfocusedBorderColor = if (isError) colorScheme.error else colorScheme.outlineVariant,
    disabledBorderColor = colorScheme.surfaceVariant,
    focusedTextColor = colorScheme.onSurface,
    unfocusedTextColor = colorScheme.onSurface,
    disabledTextColor = colorScheme.onSurface.copy(alpha = 0.38f),
    focusedLabelColor = if (isError) colorScheme.error else colorScheme.primary,
    unfocusedLabelColor = colorScheme.onSurfaceVariant,
    errorBorderColor = colorScheme.error,
    errorLabelColor = colorScheme.error,
    errorSupportingTextColor = colorScheme.error
)

// Reusable composable for truncated text with popup - Material 3 styled
@Composable
fun TruncatedTextWithPopup(
    text: String,
    maxLength: Int = 10,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val iosPalette = rememberIosAddExpensePalette()
    var showPopup by remember { mutableStateOf(false) }
    val shouldTruncate = text.length > maxLength
    val displayText = if (shouldTruncate) text.take(maxLength) else text

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayText,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = colorScheme.onSurface
        )
        if (shouldTruncate) {
            Text(
                text = "...",
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .clickable { showPopup = true }
                    .padding(start = 2.dp)
            )
        }
    }

    // Popup dialog to show full text - Material 3 styled
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
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceContainerHighest
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showPopup = false },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Close",
                                style = MaterialTheme.typography.labelLarge
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
fun AddExpenseScreen(
    project: Project,
    userId: String,
    userName: String,
    expenseId: String? = null,
    onNavigateBack: () -> Unit,
    onExpenseAdded: () -> Unit,
    onNavigateToNotifications: (String) -> Unit = {},
    onNavigateToExpenseChat: (String) -> Unit = {},
    expenseViewModel: ExpenseViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    approvalViewModel: com.cubiquitous.tracura.viewmodel.ApprovalViewModel = hiltViewModel()
) {
    val formData by expenseViewModel.formData.collectAsState()
    val error by expenseViewModel.error.collectAsState()
    val fieldError by expenseViewModel.fieldError.collectAsState()
    val isSubmitting by expenseViewModel.isSubmitting.collectAsState()
    val successMessage by expenseViewModel.successMessage.collectAsState()
    val budgetValidationResult by expenseViewModel.budgetValidationResult.collectAsState()
    val budgetWarning by expenseViewModel.budgetWarning.collectAsState()

    // Notification states
    val authState by authViewModel.authState.collectAsState()
    val notificationBadge by notificationViewModel.notificationBadge.collectAsState()
    val notifications by notificationViewModel.notifications.collectAsState()
    val isNotificationsLoading by notificationViewModel.isLoading.collectAsState()

    // Filter notifications for this project
    val projectNotifications = notifications.filter { it.projectId == project.id }
    val projectNotificationBadge = remember(projectNotifications) {
        val unreadCount = projectNotifications.count { !it.isRead }
        com.cubiquitous.tracura.model.NotificationBadge(
            count = unreadCount,
            hasUnread = unreadCount > 0
        )
    }

    var showScannerSheet by remember { mutableStateOf(false) }
    var scannerResultMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var selectedAttachmentUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAttachmentName by remember { mutableStateOf<String?>(null) }
    var showPaymentProofDialog by remember { mutableStateOf(false) }
    var selectedPaymentProofUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPaymentProofName by remember { mutableStateOf<String?>(null) }
    var showPhaseDropdown by remember { mutableStateOf(false) }
    var showDepartmentDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var isOtherCategorySelected by remember { mutableStateOf(false) }
    var showBudgetExceededDialog by remember { mutableStateOf(false) }
    var showItemTypeDropdown by remember { mutableStateOf(false) }
    var showItemDropdown by remember { mutableStateOf(false) }
    var showSpecDropdown by remember { mutableStateOf(false) }
    var showUomDropdown by remember { mutableStateOf(false) }

    // Line items for Labour-Only and Self-Execution modes
    var expenseLineItems by remember { mutableStateOf<List<LineItem>>(emptyList()) }
    var showAddExpenseLineItemSheet by remember { mutableStateOf(false) }
    var editingExpenseLineItemIndex by remember { mutableStateOf<Int?>(null) }
    var labourContractorAmountText by remember { mutableStateOf("") }
    // Toggle for Labour-Only: true = contractor amount only, false = line items only
    var labourOnlyUseContractorAmount by remember { mutableStateOf(false) }

    // Initialize thickness with default value
    LaunchedEffect(Unit) {
        if (formData.thickness.isEmpty()) {
            expenseViewModel.updateFormField("thickness", "16 mm")
        }
    }

    // LazyListState for auto-scrolling to error fields
    val listState = rememberLazyListState()

    // Track positions of form fields for auto-scroll (Y position relative to parent)
    val fieldPositions = remember { mutableStateMapOf<String, Float>() }

    // Get phases from viewmodel
    val phases by expenseViewModel.phases.collectAsState()

    // Extract user role and assigned departments for filtering
    val userRole = authState.user?.role

    val userPhone = normalizePhoneForCompare(authState.user?.phone)
    
    // Get assigned departments based on user role
    val assignedDepartments = remember(userRole, userPhone, project) {
        when (userRole) {
            com.cubiquitous.tracura.model.UserRole.APPROVER -> {
                if (userPhone.isNotBlank()) {
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                    val permanentDepartments = project.departmentApproverAssignments
                        .filter { (_, approverPhone) ->
                            normalizePhoneForCompare(approverPhone) == userPhone
                        }
                        .keys.toSet()

                    val temporaryDepartments = project.departmentTemporaryApprover
                        .filter { entry ->
                            isActiveTempApproverForDate(entry, userPhone, today)
                        }
                        .map { it.departmentName }
                        .toSet()

                    permanentDepartments + temporaryDepartments
                } else {
                    emptySet()
                }
            }
            com.cubiquitous.tracura.model.UserRole.USER -> {
                if (userPhone.isNotBlank()) {
                    project.departmentUserAssignments
                        .filter { (_, assignedPhone) ->
                            normalizePhoneForCompare(assignedPhone) == userPhone
                        }
                        .keys.toSet()
                } else {
                    emptySet()
                }
            }
            else -> null // null means show all (BUSINESS_HEAD)
        }
    }

    val context = LocalContext.current

    // Helper function to format number with commas
    fun formatNumberWithCommas(value: String): String {
        if (value.isEmpty()) return ""
        // Remove all non-digit characters except decimal point
        val cleaned = value.replace(Regex("[^0-9.]"), "")
        if (cleaned.isEmpty()) return ""

        // Split by decimal point if exists
        val parts = cleaned.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) ".${parts[1]}" else ""

        // Format integer part with commas
        val formattedInteger = try {
            if (integerPart.isEmpty()) "" else {
                integerPart.reversed().chunked(3).joinToString(",").reversed()
            }
        } catch (e: Exception) {
            integerPart
        }

        return formattedInteger + decimalPart
    }

    // Helper function to remove commas from number string
    fun removeCommas(value: String): String {
        return value.replace(",", "")
    }

    // Helper function to check if phase is disabled
    fun isPhaseDisabled(phase: com.cubiquitous.tracura.model.Phase): Boolean {
        // Check if project is disabled (INACTIVE, COMPLETED, HANDOVER, LOCKED, IN REVIEW, REVIEW REJECTED/DECLINED)
        // For user flow: also disable IN REVIEW and DECLINED projects (approvers can still access them)
        val projectStatus = project.status.uppercase()
        val isProjectDisabled = projectStatus in listOf("INACTIVE", "COMPLETED", "HANDOVER", "LOCKED", "IN REVIEW", "REVIEW REJECTED", "DECLINED")

        // Check if phase is not active
        if (!phase.isEnabledValue) return true

        // Check phase dates
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val today = java.util.Calendar.getInstance()
        today.set(java.util.Calendar.HOUR_OF_DAY, 0)
        today.set(java.util.Calendar.MINUTE, 0)
        today.set(java.util.Calendar.SECOND, 0)
        today.set(java.util.Calendar.MILLISECOND, 0)

        // Check if end date has passed
        if (!phase.endDate.isNullOrEmpty()) {
            try {
                val endDate = dateFormat.parse(phase.endDate)
                endDate?.let {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = it
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    if (cal.before(today)) {
                        return true // End date has passed
                    }
                }
            } catch (e: Exception) {
                // Invalid date format, skip date check
            }
        }

        // Check if start date is in the future
        if (!phase.startDate.isNullOrEmpty()) {
            try {
                val startDate = dateFormat.parse(phase.startDate)
                startDate?.let {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = it
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    if (cal.after(today)) {
                        return true // Start date is in the future
                    }
                }
            } catch (e: Exception) {
                // Invalid date format, skip date check
            }
        }

        return isProjectDisabled
    }

    // Check if project is disabled (including LOCKED status - when planned date > current date)
    // For user flow: also disable IN REVIEW and DECLINED projects (approvers can still access them)
    val isProjectDisabled = project.status.uppercase() in listOf("INACTIVE", "COMPLETED", "HANDOVER", "LOCKED", "IN REVIEW", "REVIEW REJECTED", "DECLINED")

    // Show only selectable phases in dropdown
    val allProjectDepartments by expenseViewModel.allProjectDepartments.collectAsState()

    // Show only selectable phases in dropdown
    val selectablePhases = remember(phases, project.status, allProjectDepartments, userRole) {
        phases.filterNot { phase ->
            if (isPhaseDisabled(phase)) {
                true
            } else {
                val phaseDepartments = allProjectDepartments.filter { it.phaseId == phase.id }
                // Exclude phase if every department in that phase is finished
                phaseDepartments.isNotEmpty() && phaseDepartments.all { it.isFinished }
            }
        }
    }

    // Predefined category options for USER role with an 'Other' option
    val predefinedCategories = remember {
        listOf(
            "Labour",
            "Raw Materials (cement/steel/sand/bricks)",
            "Ready-Mix / Precast (RMC, precast items)",
            "Equipment/Machinery Hire",
            "Tools & Consumables (bits, blades, smalls)",
            "Subcontractor Services",
            "Transport & Logistics (freight, loading)",
            "Site Utilities (power, water, fuel, internet)",
            "Safety & Compliance (PPE, audits)",
            "Permits & Regulatory Fees",
            "Testing & Quality (soil/cube tests, inspections)",
            "Waste & Disposal (debris, haulage)",
            "Temporary Works (scaffolding, shuttering/formwork)",
            "Finishes & Fixtures (tiles, paint, sanitary, lights)",
            "Repairs & Rework / Snag-fix",
            "Maintenance (post-handover window)",
            "Misc / Other (notes required)",
            "Other"
        )
    }

    // Use project.id as key so phases/departments are only re-loaded when the actual
    // project changes, not on every Firestore snapshot update (updatedAt, etc.).
    LaunchedEffect(project.id ?: "") {
        expenseViewModel.setSelectedProject(project)
    }

    // Load departments from subcollection when phase is selected
    LaunchedEffect(formData.phaseId) {
        if (formData.phaseId.isNotEmpty() && project.id != null) {
            expenseViewModel.loadDepartmentsForPhase(project.id!!, formData.phaseId)
        } else {
            // Clear all downstream fields when phase is cleared
            // Note: ViewModel already clears department, itemType, item, spec when phaseId changes,
            // but we explicitly clear all fields here to ensure clean state when phase is cleared
            expenseViewModel.updateFormField("department", "")
            expenseViewModel.updateFormField("itemType", "")
            expenseViewModel.updateFormField("item", "")
            expenseViewModel.updateFormField("spec", "")
            expenseViewModel.updateFormField("uom", "")
        }
    }

    // Observe departments from subcollection and update itemTypes when department is selected
    val departmentsFromSubcollection by expenseViewModel.departmentsFromSubcollection.collectAsState()

    // Note: We don't filter phases because departments are only loaded after phase selection.
    // Instead, we only filter departments once a phase is selected and departments are loaded.
    
    // Filter departments to show only assigned ones (for APPROVER/USER roles)
    val filteredDepartments = remember(departmentsFromSubcollection, assignedDepartments, userRole) {
        if (assignedDepartments != null && assignedDepartments.isNotEmpty()) {
            departmentsFromSubcollection.filter { dept ->
                val deptName = if (dept.name.contains("_")) {
                    dept.name.substringAfter("_")
                } else {
                    dept.name
                }
                assignedDepartments.contains(deptName) && !dept.isFinished
            }
        } else if (assignedDepartments != null) {
            emptyList() // APPROVER/USER with no assignments sees nothing
        } else {
            // BUSINESS_HEAD: do not show finished departments in add-expense flow
            departmentsFromSubcollection.filter { !it.isFinished }
        }
    }

    val selectedDepartment = remember(formData.department, departmentsFromSubcollection) {
        departmentsFromSubcollection.firstOrNull { dept ->
            dept.name.equals(formData.department, ignoreCase = true)
        }
    }

    val isTurnkeyDepartment = selectedDepartment?.contractorModeEnumValue == com.cubiquitous.tracura.model.ContractorMode.TURNKEY_AMOUNT_ONLY
    val isTurnkeyAmountOnlyDepartment = selectedDepartment?.contractorModeEnumValue == com.cubiquitous.tracura.model.ContractorMode.TURNKEY_AMOUNT_ONLY
    val isLabourOnlyDepartment = selectedDepartment?.contractorModeEnumValue == com.cubiquitous.tracura.model.ContractorMode.LABOUR_ONLY
    val isSelfExecutionDepartment = selectedDepartment?.contractorModeEnumValue == com.cubiquitous.tracura.model.ContractorMode.SELF_EXECUTION
    val showLineItemsUi = isLabourOnlyDepartment || isSelfExecutionDepartment
    val isContractorAmountEntry = isLabourOnlyDepartment && formData.itemType == "Contractor Amount"
    val isSelectedDepartmentFinished = selectedDepartment?.isFinished == true
    val isBusinessHeadRole = userRole == UserRole.BUSINESS_HEAD
    val roleBasedDepartmentError = remember(isSelectedDepartmentFinished, userRole, formData.department) {
        if (!isSelectedDepartmentFinished || formData.department.isBlank()) {
            null
        } else {
            when (userRole) {
                UserRole.BUSINESS_HEAD -> "Business head cannot be selected for a completed department."
                UserRole.APPROVER, UserRole.USER -> "Department is completed and cannot be added."
                else -> null
            }
        }
    }
    val shouldBlockSubmitForCompletedDepartment =
        isSelectedDepartmentFinished && formData.department.isNotBlank() &&
            (userRole == UserRole.BUSINESS_HEAD || userRole == UserRole.APPROVER || userRole == UserRole.USER)

    val isRestrictedDepartmentRole = userRole == UserRole.APPROVER || userRole == UserRole.USER

    // Auto-select first available phase when phases load (new expense only).
    // Mirrors iOS loadPhases() → auto-select first phase where canAddExpense == true.
    LaunchedEffect(selectablePhases) {
        if (expenseId == null && formData.phaseId.isEmpty() && selectablePhases.isNotEmpty()) {
            val firstPhase = selectablePhases.first()
            val nameToDisplay = if (firstPhase.phaseName.isNotEmpty()) firstPhase.phaseName else "Phase ${firstPhase.phaseNumber}"
            expenseViewModel.updateFormField("phaseId", firstPhase.id)
            expenseViewModel.updateFormField("phaseName", nameToDisplay)
        }
    }

    // Auto-select department after phase selection.
    // USER/APPROVER: lock to their assigned department (hide manual chooser).
    // BUSINESS_HEAD: auto-select first dept when phase changes and current dept is not in the new phase.
    // Mirrors iOS applyRoleBasedDepartmentSelection() + phase-change department reset.
    LaunchedEffect(isRestrictedDepartmentRole, formData.phaseId, filteredDepartments) {
        if (formData.phaseId.isEmpty()) return@LaunchedEffect

        if (isRestrictedDepartmentRole) {
            val autoDepartment = filteredDepartments.firstOrNull()?.name.orEmpty()
            if (formData.department != autoDepartment) {
                expenseViewModel.updateFormField("department", autoDepartment)
            }
        } else if (expenseId == null && filteredDepartments.isNotEmpty()) {
            // BUSINESS_HEAD: keep current dept if it exists in this phase; otherwise pick first.
            val currentDeptInPhase = filteredDepartments.any { dept ->
                val deptName = if (dept.name.contains("_")) dept.name.substringAfter("_") else dept.name
                deptName.equals(formData.department, ignoreCase = true)
            }
            if (!currentDeptInPhase || formData.department.isEmpty()) {
                val firstDept = filteredDepartments.first()
                val deptName = if (firstDept.name.contains("_")) firstDept.name.substringAfter("_") else firstDept.name
                expenseViewModel.updateFormField("department", deptName)
            }
        }
    }
    
    // Watch formData.department only — the ViewModel's loadDepartmentsForPhase already calls
    // updateItemTypes() internally after departments finish loading, so watching .size here
    // caused a double-trigger on every department-list update.
    LaunchedEffect(formData.department) {
        if (formData.department.isNotEmpty() && departmentsFromSubcollection.isNotEmpty()) {
            expenseViewModel.updateItemTypesForSelectedDepartment()
        }
    }

    var prefilledExpenseId by remember(expenseId) { mutableStateOf<String?>(null) }

    // Load expense data if editing - use cached data first for instant loading
    LaunchedEffect(expenseId) {
        if (expenseId.isNullOrEmpty()) {
            prefilledExpenseId = null
            return@LaunchedEffect
        }

        // Check if we have cached expense data
        val cachedExpense = expenseViewModel.expenseForEditing.value
        if (cachedExpense != null && cachedExpense.id == expenseId) {
            Log.d("AddExpenseScreen", "✅ Using cached expense data for instant loading: $expenseId")
            expenseViewModel.setExpenseForEditing(cachedExpense)
            prefilledExpenseId = expenseId
        } else {
            // Fallback: fetch from Firestore
            Log.d("AddExpenseScreen", "⏳ Fetching expense from Firestore: $expenseId")
            approvalViewModel.fetchExpenseById(expenseId)
        }
    }

    // Populate form from fetched expense (fallback path only)
    val selectedExpense by approvalViewModel.selectedExpense.collectAsState()
    LaunchedEffect(selectedExpense?.id, expenseId, prefilledExpenseId) {
        val expense = selectedExpense ?: return@LaunchedEffect
        if (!expenseId.isNullOrEmpty() && expenseId == expense.id && prefilledExpenseId != expense.id) {
            Log.d("AddExpenseScreen", "📝 Populating form from Firestore fetch")
            expenseViewModel.setExpenseForEditing(expense)
            prefilledExpenseId = expense.id
        }
    }

    // Clear cached expense when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            expenseViewModel.clearExpenseForEditing()
        }
    }

    // Sync payment proof URI from form data to local state
    LaunchedEffect(formData.paymentProofUri) {
        if (formData.paymentProofUri.isNotEmpty() && selectedPaymentProofUri == null) {
            try {
                val uri = Uri.parse(formData.paymentProofUri)
                selectedPaymentProofUri = uri
                selectedPaymentProofName = formData.paymentProofUri.substringAfterLast("/").substringBefore("?")
                    .takeIf { it.isNotEmpty() } ?: "payment_proof.jpg"
            } catch (e: Exception) {
                // Invalid URI, ignore
            }
        } else if (formData.paymentProofUri.isEmpty() && selectedPaymentProofUri != null) {
            selectedPaymentProofUri = null
            selectedPaymentProofName = null
        }
    }

    // Reset line items, contractor amount, and toggle when department/mode changes
    LaunchedEffect(formData.department, showLineItemsUi) {
        if (showLineItemsUi) {
            expenseLineItems = emptyList()
            labourContractorAmountText = ""
            labourOnlyUseContractorAmount = false
            expenseViewModel.updateFormField("amount", "")
        }
    }

    // Sync amount from line items or contractor amount for Labour-Only/Self-Execution
    LaunchedEffect(expenseLineItems, labourContractorAmountText, showLineItemsUi, labourOnlyUseContractorAmount) {
        if (showLineItemsUi) {
            val totalAmt = when {
                isLabourOnlyDepartment && labourOnlyUseContractorAmount ->
                    labourContractorAmountText.toDoubleOrNull() ?: 0.0
                else ->
                    expenseLineItems.sumOf { it.total }
            }
            expenseViewModel.updateFormField("amount", if (totalAmt > 0) totalAmt.toString() else "")
        }
    }

    // Budget validation — debounced 400 ms so rapid keystrokes don't each hit Firestore.
    LaunchedEffect(formData.amount, formData.department, formData.phaseId) {
        val isDepartmentLoading = formData.department.contains("Checking", ignoreCase = true) ||
                                  formData.department.contains("Loading", ignoreCase = true) ||
                                  formData.department.contains("..", ignoreCase = false)

        if (formData.amount.isNotEmpty() && formData.department.isNotEmpty() && formData.phaseId.isNotEmpty() && !isDepartmentLoading) {
            val amount = formData.amount.toDoubleOrNull()
            if (amount != null && amount > 0) {
                delay(400)
                project.id?.let { expenseViewModel.validateBudget(it, formData.department, amount, formData.phaseId) }
            }
        }
    }

    // Auto-scroll to error field when field error occurs
    // Use both fieldError and error to ensure it triggers on each validation failure
    LaunchedEffect(fieldError, error) {
        fieldError?.let { fieldName ->
            // Small delay to ensure the error message and LazyColumn have rendered
            kotlinx.coroutines.delay(300)

            try {
                // Calculate the item index for EXPENSE DETAILS Card
                // Item structure: 0=Project Details, 1=Success (conditional), 2=Budget Warning, 3=EXPENSE DETAILS Card
                val expenseDetailsItemIndex = when {
                    successMessage != null -> 3 // Success message is shown
                    else -> 2 // No success message
                }

                // Scroll to the EXPENSE DETAILS section first
                listState.animateScrollToItem(
                    index = expenseDetailsItemIndex,
                    scrollOffset = 0
                )

                // Additional delay to ensure scroll completes, then scroll to specific field
                kotlinx.coroutines.delay(200)

                // Get the actual position of the field if available
                val fieldPosition = fieldPositions[fieldName]
                val datePosition = fieldPositions["date"] ?: 0f

                if (fieldPosition != null && fieldPosition > datePosition) {
                    // Calculate relative offset from the date field (first field)
                    // positionInParent() gives us pixels relative to the Column
                    val relativeOffset = fieldPosition - datePosition

                    // Scroll by the relative offset to bring the field into view
                    // Add some padding (70 pixels ≈ 20dp) to show the field label clearly
                    val scrollOffset = (relativeOffset - 70f).coerceAtLeast(0f)
                    if (scrollOffset > 0) {
                        listState.animateScrollBy(scrollOffset)
                    }
                } else {
                    // Fallback to approximate offsets if positions not yet tracked
                    // These are approximate pixel values (assuming ~3.5 pixels per dp)
                    val fieldOffsets = mapOf(
                        "date" to 0f,
                        "amount" to 280f, // ~80dp for Amount field
                        "phase" to 630f, // ~180dp for Phase field
                        "department" to 980f, // ~280dp for Department field
                        "category" to 1330f, // ~380dp for Category field
                        "description" to 1680f, // ~480dp for Description field
                        "paymentProof" to 2100f, // ~600dp for Payment Proof field
                        "attachment" to 2450f // ~700dp for Attachment field
                    )

                    val offset = fieldOffsets[fieldName] ?: 0f
                    if (offset > 0) {
                        listState.animateScrollBy(offset)
                    }
                }
            } catch (e: Exception) {
                // If scrolling fails, try to scroll to EXPENSE DETAILS section
                try {
                    val expenseDetailsItemIndex = when {
                        successMessage != null -> 3
                        else -> 2
                    }
                    listState.animateScrollToItem(expenseDetailsItemIndex)
                } catch (ex: Exception) {
                    // Ignore scroll errors
                }
            }
        }
    }

    // Load notifications when screen opens
    LaunchedEffect(Unit) {
        authState.user?.uid?.let { userId ->
            notificationViewModel.loadNotifications(userId)
        }
    }

    // Refresh notifications when expense is successfully submitted
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            // Refresh notifications after successful expense submission
            authState.user?.uid?.let { userId ->
                delay(1000) // Wait for notification to be created
                notificationViewModel.onScreenVisible(userId)
            }
        }
    }

    // Activity result launchers for attachment selection
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedAttachmentUri?.let { uri ->
                selectedAttachmentName = "camera_${System.currentTimeMillis()}.jpg"
                expenseViewModel.updateFormField("attachmentUri", uri.toString())
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedAttachmentUri = it
            selectedAttachmentName = "photo_${System.currentTimeMillis()}.jpg"
            expenseViewModel.updateFormField("attachmentUri", it.toString())
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedAttachmentUri = it
            selectedAttachmentName = "document_${System.currentTimeMillis()}.pdf"
            expenseViewModel.updateFormField("attachmentUri", it.toString())
        }
    }

    // Activity result launchers for payment proof selection
    val paymentProofCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedPaymentProofUri?.let { uri ->
                selectedPaymentProofName = "payment_proof_camera_${System.currentTimeMillis()}.jpg"
                expenseViewModel.updateFormField("paymentProofUri", uri.toString())
            }
        }
    }

    val paymentProofPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedPaymentProofUri = it
            selectedPaymentProofName = "payment_proof_photo_${System.currentTimeMillis()}.jpg"
            expenseViewModel.updateFormField("paymentProofUri", it.toString())
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Create a temporary URI for the camera to save the image
            val tempUri = context.contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                android.content.ContentValues()
            )
            selectedAttachmentUri = tempUri
            tempUri?.let { cameraLauncher.launch(it) }
        }
    }

    // Camera permission launcher for payment proof
    val paymentProofCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Create a temporary URI for the camera to save the image
            val tempUri = context.contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                android.content.ContentValues()
            )
            selectedPaymentProofUri = tempUri
            tempUri?.let { paymentProofCameraLauncher.launch(it) }
        }
    }

    LaunchedEffect(error) {
        if (error != null) {
            // Handle error display
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val iosPalette = rememberIosAddExpensePalette()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
    ) {
        // Modern Top Bar with Material 3 styling
        TopAppBar(
            title = {
                Text(
                    text = if (expenseId != null) "Edit Expense" else "New Expense",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colorScheme.onSurface
                    )
                }
            },
            actions = {
                IconButton(onClick = { showScannerSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = "Scan Receipt",
                        tint = colorScheme.primary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colorScheme.surface,
                titleContentColor = colorScheme.onSurface
            )
        )

        // Receipt scanner sheet
        if (showScannerSheet) {
            ReceiptScannerSheet(
                onDismiss = { showScannerSheet = false },
                candidateCategories = predefinedCategories,
                departmentLineItems = selectedDepartment?.lineItems ?: emptyList(),
                onLineItemsMatched = { matches, imageUri ->
                    showScannerSheet = false
                    // Attach scanned image
                    imageUri?.let {
                        selectedAttachmentUri = it
                        selectedAttachmentName = "receipt_scan_${System.currentTimeMillis()}.jpg"
                        expenseViewModel.updateFormField("attachmentUri", it.toString())
                    }
                    if (matches.isEmpty()) {
                        scannerResultMessage =
                            "Receipt uploaded. No matching line items found — please add them manually."
                    } else {
                        // Populate expenseLineItems from matched results
                        expenseLineItems = matches.map { match ->
                            com.cubiquitous.tracura.model.LineItem(
                                itemType  = match.matchedLineItem.itemType,
                                item      = match.matchedLineItem.item,
                                spec      = match.matchedLineItem.spec,
                                quantity  = match.extractedQuantity,
                                unitPrice = match.extractedUnitPrice,
                                uom       = match.matchedLineItem.uom
                            )
                        }
                        scannerResultMessage =
                            "Receipt scanned! ${matches.size} item(s) matched. Please verify quantities before submitting."
                    }
                },
                onResult = { result, imageUri ->
                    showScannerSheet = false

                    // Fill extracted OCR fields
                    result.date?.takeIf { it.isNotEmpty() }?.let {
                        expenseViewModel.updateFormField("date", it)
                    }
                    result.description?.takeIf { it.isNotEmpty() }?.let {
                        expenseViewModel.updateFormField("description", it)
                    }
                    result.modeOfPayment?.takeIf { it.isNotEmpty() }?.let {
                        expenseViewModel.updateFormField("modeOfPayment", it)
                    }
                    result.brand?.takeIf { it.isNotEmpty() }?.let {
                        expenseViewModel.updateFormField("brand", it)
                    }
                    result.spec?.takeIf { it.isNotEmpty() }?.let {
                        expenseViewModel.updateFormField("spec", it)
                    }
                    result.thickness?.takeIf { it.isNotEmpty() }?.let {
                        expenseViewModel.updateFormField("thickness", it)
                    }
                    result.quantity?.takeIf { it.isNotEmpty() }?.let {
                        expenseViewModel.updateFormField("quantity", it)
                    }
                    result.uom?.takeIf { it.isNotEmpty() }?.let {
                        expenseViewModel.updateFormField("uom", it)
                    }
                    // Prefer specific unitPrice; fall back to scanned total amount
                    val priceToFill = result.unitPrice?.takeIf { it.isNotEmpty() }
                        ?: result.amount?.takeIf { it.isNotEmpty() }
                    priceToFill?.let {
                        expenseViewModel.updateFormField("unitPrice", it)
                    }

                    // Set scanned receipt image as the expense attachment
                    imageUri?.let {
                        selectedAttachmentUri = it
                        selectedAttachmentName = "receipt_scan_${System.currentTimeMillis()}.jpg"
                        expenseViewModel.updateFormField("attachmentUri", it.toString())
                    }

                    // Inform user if no department was selected for line-item matching
                    if (showLineItemsUi && selectedDepartment?.lineItems.isNullOrEmpty()) {
                        scannerResultMessage =
                            "Receipt uploaded. Select a department to enable line item matching."
                    }
                }
            )
        }

        // Scanner result dialog (mirrors iOS showAlert)
        scannerResultMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { scannerResultMessage = null },
                title = {
                    Text(
                        text = "Receipt Scan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { scannerResultMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }

        // Helper function to get field-specific error - defined at composable level for access from all sections
        // Note: Date field errors are excluded as date is always set to current date by default
        fun getFieldError(fieldName: String): String? {
            return error?.let { err ->
                when {
                    // Date field errors are excluded - date is always set by default
                    fieldName == "amount" && (err.contains("amount", ignoreCase = true) || err.contains("Please enter a valid amount", ignoreCase = true)) -> err
                    fieldName == "phase" && (err.contains("phase", ignoreCase = true) || err.contains("Please select a phase", ignoreCase = true)) -> err
                    fieldName == "department" && (err.contains("department", ignoreCase = true) || err.contains("Please select a department", ignoreCase = true)) -> err
                    fieldName == "vendor" && (err.contains("vendor", ignoreCase = true) || err.contains("Please select a vendor", ignoreCase = true)) -> err
                    fieldName == "category" && (err.contains("category", ignoreCase = true) || err.contains("Please select a category", ignoreCase = true)) -> err
                    fieldName == "description" && (err.contains("description", ignoreCase = true) || err.contains("Please enter a description", ignoreCase = true)) -> err
                    fieldName == "sgst" && (err.contains("sgst", ignoreCase = true) || err.contains("Please enter SGST", ignoreCase = true)) -> err
                    fieldName == "cgst" && (err.contains("cgst", ignoreCase = true) || err.contains("Please enter CGST", ignoreCase = true)) -> err
                    fieldName == "attachment" && (err.contains("attachment", ignoreCase = true) || err.contains("Please add an attachment", ignoreCase = true)) -> err
                    fieldName == "paymentProof" && (err.contains("payment proof", ignoreCase = true) || err.contains("Please upload payment proof", ignoreCase = true)) -> err
                    else -> null
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Project Details Card
            item {
                ProjectDetailsCard(project = project)
            }

            // Success Message with Material 3 styling
            successMessage?.let {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Error Message - Hidden (errors now show inline with fields)

            // Budget Warning Component
            item {
                BudgetWarningComponent(
                    budgetValidationResult = budgetValidationResult,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // EXPENSE DETAILS Section with Material 3 styling
            item {
                Text(
                    text = "Expense Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                )
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = iosPalette.tier2Surface
                    ),
                    border = BorderStroke(1.dp, iosPalette.hairline)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Phase Field - Modern Material 3 styling
                        Column(
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                fieldPositions["phase"] = coordinates.positionInParent().y
                            }
                        ) {
                            Text(
                                text = "Phase",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            ExposedDropdownMenuBox(
                                expanded = showPhaseDropdown,
                                onExpandedChange = { showPhaseDropdown = !showPhaseDropdown }
                            ) {
                                val phaseError = getFieldError("phase")
                                var showPhasePopup by remember { mutableStateOf(false) }
                                val shouldTruncatePhase = formData.phaseName.length > 10
                                val displayPhase = if (shouldTruncatePhase) formData.phaseName.take(10) + "..." else formData.phaseName

                                OutlinedTextField(
                                    value = displayPhase,
                                    onValueChange = { },
                                    readOnly = true,
                                    placeholder = { 
                                        Text(
                                            "Select Phase", 
                                            color = colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyLarge
                                        ) 
                                    },
                                    trailingIcon = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (shouldTruncatePhase && formData.phaseName.isNotEmpty()) {
                                                Text(
                                                    text = "...",
                                                    fontSize = 14.sp,
                                                    color = colorScheme.primary,
                                                    modifier = Modifier
                                                        .clickable { showPhasePopup = true }
                                                        .padding(4.dp)
                                                )
                                            }
                                            Icon(
                                                Icons.Default.ArrowDropDown, 
                                                contentDescription = null, 
                                                tint = colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    isError = phaseError != null,
                                    supportingText = phaseError?.let {
                                        { Text(it, color = colorScheme.error) }
                                    },
                                    colors = getTextFieldColors(
                                        isError = phaseError != null,
                                        isEnabled = true,
                                        colorScheme = colorScheme
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Popup for full phase name
                                if (showPhasePopup && formData.phaseName.isNotEmpty()) {
                                    Dialog(
                                        onDismissRequest = { showPhasePopup = false },
                                        properties = DialogProperties(
                                            dismissOnBackPress = true,
                                            dismissOnClickOutside = true
                                        )
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHighest),
                                            shape = RoundedCornerShape(16.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp)
                                            ) {
                                                Text(
                                                    text = formData.phaseName,
                                                    fontSize = 16.sp,
                                                    color = colorScheme.onSurface,
                                                    modifier = Modifier.padding(bottom = 16.dp)
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    TextButton(onClick = { showPhasePopup = false }) {
                                                        Text(
                                                            text = "Close",
                                                            color = colorScheme.primary,
                                                            fontSize = 16.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                ExposedDropdownMenu(
                                    expanded = showPhaseDropdown,
                                    onDismissRequest = { showPhaseDropdown = false }
                                ) {
                                    if (selectablePhases.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No selectable phases available") },
                                            onClick = { }
                                        )
                                    } else {
                                        selectablePhases.forEach { phase ->
                                            val phaseDisplayName = if (phase.phaseName.isNotEmpty()) phase.phaseName else "Phase ${phase.phaseNumber}"

                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            TruncatedTextWithPopup(
                                                                text = phaseDisplayName,
                                                                maxLength = 10,
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Normal
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    val nameToDisplay = if (phase.phaseName.isNotEmpty()) phase.phaseName else "Phase ${phase.phaseNumber}"
                                                    expenseViewModel.updateFormField("phaseId", phase.id)
                                                    expenseViewModel.updateFormField("phaseName", nameToDisplay)
                                                    showPhaseDropdown = false
                                                },
                                                enabled = true
                                            )
                                        }
                                    }
                                }
                            }

                            // Show message if phase is disabled
                            if (formData.phaseId.isNotEmpty()) {
                                val selectedPhase = phases.find { it.id == formData.phaseId }
                                if (selectedPhase != null && isPhaseDisabled(selectedPhase)) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val reason = when {
                                        !selectedPhase.isEnabledValue -> "This phase is disabled. Please enable it to add expenses."
                                        selectedPhase.endDate?.let { endDate ->
                                            try {
                                                val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                                val parsedDate = dateFormat.parse(endDate)
                                                parsedDate?.let {
                                                    val cal = java.util.Calendar.getInstance()
                                                    cal.time = it
                                                    val today = java.util.Calendar.getInstance()
                                                    today.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                    today.set(java.util.Calendar.MINUTE, 0)
                                                    today.set(java.util.Calendar.SECOND, 0)
                                                    today.set(java.util.Calendar.MILLISECOND, 0)
                                                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                    cal.set(java.util.Calendar.MINUTE, 0)
                                                    cal.set(java.util.Calendar.SECOND, 0)
                                                    cal.set(java.util.Calendar.MILLISECOND, 0)
                                                    cal.before(today)
                                                }
                                            } catch (e: Exception) {
                                                false
                                            }
                                        } == true -> "This phase has ended. Cannot add expenses."
                                        selectedPhase.startDate?.let { startDate ->
                                            try {
                                                val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                                val parsedDate = dateFormat.parse(startDate)
                                                parsedDate?.let {
                                                    val cal = java.util.Calendar.getInstance()
                                                    cal.time = it
                                                    val today = java.util.Calendar.getInstance()
                                                    today.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                    today.set(java.util.Calendar.MINUTE, 0)
                                                    today.set(java.util.Calendar.SECOND, 0)
                                                    today.set(java.util.Calendar.MILLISECOND, 0)
                                                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                    cal.set(java.util.Calendar.MINUTE, 0)
                                                    cal.set(java.util.Calendar.SECOND, 0)
                                                    cal.set(java.util.Calendar.MILLISECOND, 0)
                                                    cal.after(today)
                                                }
                                            } catch (e: Exception) {
                                                false
                                            }
                                        } == true -> "This phase has not started yet. Cannot add expenses."
                                        isProjectDisabled -> {
                                            if (project.status.uppercase() == "LOCKED") {
                                                "Project is locked until planned start date. Cannot add expenses."
                                            } else {
                                                "Project is disabled. Cannot add expenses."
                                            }
                                        }
                                        else -> "This phase is disabled. Please enable it to add expenses."
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = colorScheme.tertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = reason,
                                            fontSize = 12.sp,
                                            color = colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }

                        // Department Field
                        Column(
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                fieldPositions["department"] = coordinates.positionInParent().y
                            }
                        ) {
                            Text(
                                text = "Department",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val isPhaseSelected = formData.phaseId.isNotEmpty()
                            val selectedPhase = phases.find { it.id == formData.phaseId }
                            val isPhaseEnabled = selectedPhase != null && !isPhaseDisabled(selectedPhase) && !isProjectDisabled

                            val departmentError = getFieldError("department")
                            val departmentPlaceholder = when {
                                !isPhaseSelected -> "Select Phase first"
                                isRestrictedDepartmentRole && filteredDepartments.isEmpty() -> "No assigned department"
                                isRestrictedDepartmentRole -> "Auto-selected"
                                else -> "Select Department"
                            }

                            if (isRestrictedDepartmentRole) {
                                OutlinedTextField(
                                    value = formData.department,
                                    onValueChange = { },
                                    readOnly = true,
                                    enabled = false,
                                    placeholder = {
                                        Text(
                                            departmentPlaceholder,
                                            color = iosPalette.textSecondary,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = departmentError != null,
                                    supportingText = departmentError?.let {
                                        { Text(it, color = colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                                    },
                                    colors = getTextFieldColors(
                                        isError = departmentError != null,
                                        isEnabled = false,
                                        colorScheme = colorScheme
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else {
                                ExposedDropdownMenuBox(
                                    expanded = showDepartmentDropdown,
                                    onExpandedChange = { showDepartmentDropdown = it }
                                ) {
                                    var showDepartmentPopup by remember { mutableStateOf(false) }
                                    val shouldTruncateDept = formData.department.length > 10
                                    val displayDepartment = if (shouldTruncateDept) formData.department.take(10) + "..." else formData.department

                                    OutlinedTextField(
                                        value = displayDepartment,
                                        onValueChange = { },
                                        readOnly = true,
                                        enabled = true,
                                        placeholder = {
                                            Text(
                                                if (!isPhaseSelected) "Select Phase first" else "Select Department",
                                                color = if (isPhaseSelected && isPhaseEnabled) iosPalette.textPrimary else iosPalette.textSecondary,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        },
                                        trailingIcon = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (shouldTruncateDept && formData.department.isNotEmpty()) {
                                                    Text(
                                                        text = "...",
                                                        fontSize = 14.sp,
                                                        color = if (isPhaseSelected && isPhaseEnabled) colorScheme.primary else iosPalette.textSecondary,
                                                        modifier = Modifier
                                                            .clickable(enabled = isPhaseSelected && isPhaseEnabled) {
                                                                showDepartmentPopup = true
                                                            }
                                                            .padding(4.dp)
                                                    )
                                                }
                                                Icon(
                                                    Icons.Default.ArrowDropDown,
                                                    contentDescription = null,
                                                    tint = if (isPhaseSelected && isPhaseEnabled) iosPalette.textSecondary else iosPalette.hairline
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        isError = departmentError != null,
                                        supportingText = departmentError?.let {
                                            { Text(it, color = colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                                        },
                                        colors = getTextFieldColors(
                                            isError = departmentError != null,
                                            isEnabled = isPhaseSelected && isPhaseEnabled,
                                            colorScheme = colorScheme
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    if (showDepartmentPopup && formData.department.isNotEmpty()) {
                                        Dialog(
                                            onDismissRequest = { showDepartmentPopup = false },
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
                                                        text = formData.department,
                                                        fontSize = 16.sp,
                                                        color = iosPalette.textPrimary,
                                                        modifier = Modifier.padding(bottom = 16.dp)
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End
                                                    ) {
                                                        TextButton(onClick = { showDepartmentPopup = false }) {
                                                            Text(
                                                                text = "Close",
                                                                color = colorScheme.primary,
                                                                fontSize = 16.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    ExposedDropdownMenu(
                                        expanded = showDepartmentDropdown && isPhaseSelected && isPhaseEnabled,
                                        onDismissRequest = { showDepartmentDropdown = false },
                                        containerColor = iosPalette.tier2Surface
                                    ) {
                                        if (!isPhaseSelected || !isPhaseEnabled) {
                                            DropdownMenuItem(
                                                text = { Text("Select Phase first", color = iosPalette.textSecondary) },
                                                onClick = { showDepartmentDropdown = false }
                                            )
                                        } else if (filteredDepartments.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("No departments available", color = iosPalette.textSecondary) },
                                                onClick = { }
                                            )
                                        } else {
                                            filteredDepartments.forEach { department ->
                                                DropdownMenuItem(
                                                    text = {
                                                        TruncatedTextWithPopup(
                                                            text = department.name,
                                                            maxLength = 10,
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Normal
                                                        )
                                                    },
                                                     onClick = {
                                                         val deptName = if (department.name.contains("_")) {
                                                             department.name.substringAfter("_")
                                                         } else {
                                                             department.name
                                                         }
                                                         expenseViewModel.updateFormField("department", deptName)
                                                         showDepartmentDropdown = false
                                                     }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (roleBasedDepartmentError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = roleBasedDepartmentError,
                                    color = colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        // Date Field - Third field as per screenshot
                        Column(
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                fieldPositions["date"] = coordinates.positionInParent().y
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Date",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface
                            )
                                // Format date display - date is always set to current date by default
                                val displayDate = try {
                                    val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                    val date = if (formData.date.isNotEmpty()) {
                                        inputFormat.parse(formData.date)
                                    } else {
                                        Date() // Fallback to current date if somehow empty
                                    }
                                    date?.let { outputFormat.format(it) } ?: SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
                                } catch (e: Exception) {
                                    // Fallback to current date if parsing fails
                                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
                                }

                                FilledTonalButton(
                                    onClick = { showDatePicker = true },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = colorScheme.surfaceContainerHighest,
                                        contentColor = colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = displayDate,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurface
                                    )
                                }
                            }
                            // Date field errors are excluded - date is always set to current date by default
                        }

                        if (isTurnkeyDepartment || isTurnkeyAmountOnlyDepartment) {
                            Column(
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    fieldPositions["amount"] = coordinates.positionInParent().y
                                }
                            ) {
                                Text(
                                    text = "Contractor Payment Amount",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                val amountError = getFieldError("amount")
                                OutlinedTextField(
                                    value = formatNumberWithCommas(formData.amount),
                                    onValueChange = { newValue ->
                                        val cleanedValue = removeCommas(newValue).filter { it.isDigit() || it == '.' }
                                        expenseViewModel.updateFormField("amount", cleanedValue)
                                    },
                                    placeholder = { Text("0", color = colorScheme.onSurfaceVariant) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = amountError != null,
                                    supportingText = amountError?.let {
                                        { Text(it, color = colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                                    },
                                    colors = getTextFieldColors(
                                        isError = amountError != null,
                                        isEnabled = true,
                                        colorScheme = colorScheme
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        } else if (isLabourOnlyDepartment) {
                            // Labour-Only: toggle between contractor amount entry and line items
                            Column(
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    fieldPositions["amount"] = coordinates.positionInParent().y
                                },
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Enter Contractor Amount",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = colorScheme.onSurface
                                    )
                                    Switch(
                                        checked = labourOnlyUseContractorAmount,
                                        onCheckedChange = { checked ->
                                            labourOnlyUseContractorAmount = checked
                                            labourContractorAmountText = ""
                                            expenseLineItems = emptyList()
                                            expenseViewModel.updateFormField("amount", "")
                                        }
                                    )
                                }
                                if (!labourOnlyUseContractorAmount) {
                                    Text(
                                        text = "Toggle on to enter a contractor amount instead of line items.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                if (labourOnlyUseContractorAmount) {
                                    OutlinedTextField(
                                        value = formatNumberWithCommas(labourContractorAmountText),
                                        onValueChange = { newValue ->
                                            labourContractorAmountText = removeCommas(newValue).filter { it.isDigit() || it == '.' }
                                        },
                                        label = { Text("Contractor Amount") },
                                        placeholder = { Text("0", color = colorScheme.onSurfaceVariant) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = getTextFieldColors(isEnabled = true, colorScheme = colorScheme),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                        // Self-Execution: no contractor amount — line items shown below

                        // Line Items section for Labour-Only (only when toggle is OFF) and Self-Execution
                        if (showLineItemsUi && !(isLabourOnlyDepartment && labourOnlyUseContractorAmount)) {
                            val lineItemContractorMode = if (isLabourOnlyDepartment) ContractorMode.LABOUR_ONLY else ContractorMode.SELF_EXECUTION
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = colorScheme.secondaryContainer.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Line Items",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSurface
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "sum equals budget",
                                                fontSize = 12.sp,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    expenseLineItems.forEachIndexed { index, lineItem ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        editingExpenseLineItemIndex = index
                                                        showAddExpenseLineItemSheet = true
                                                    }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = "${lineItem.itemType} — ${lineItem.item}",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = colorScheme.onSurface
                                                    )
                                                    if (lineItem.spec.isNotBlank()) {
                                                        Text(
                                                            text = "${lineItem.spec} | ${lineItem.quantity} ${lineItem.uom}",
                                                            fontSize = 12.sp,
                                                            color = colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        colorScheme.primary.copy(alpha = 0.12f),
                                                        RoundedCornerShape(16.dp)
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = FormatUtils.formatCurrencyWithoutDecimals(lineItem.total),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            editingExpenseLineItemIndex = null
                                            showAddExpenseLineItemSheet = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                                            contentColor = colorScheme.tertiary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Add Line Item",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    if (expenseLineItems.isNotEmpty()) {
                                        HorizontalDivider(color = colorScheme.outlineVariant)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Σ",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Line Items Total",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.onSurface
                                                )
                                            }
                                            Text(
                                                text = FormatUtils.formatCurrencyWithoutDecimals(expenseLineItems.sumOf { it.total }),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.primary
                                            )
                                        }

                                        if (isLabourOnlyDepartment && labourContractorAmountText.isNotBlank()) {
                                            val contractorAmt = labourContractorAmountText.toDoubleOrNull() ?: 0.0
                                            val grandTotal = contractorAmt + expenseLineItems.sumOf { it.total }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Grand Total",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.onSurface
                                                )
                                                Text(
                                                    text = FormatUtils.formatCurrencyWithoutDecimals(grandTotal),
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.tertiary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!isTurnkeyDepartment && !isContractorAmountEntry && !isTurnkeyAmountOnlyDepartment && !showLineItemsUi) {
                        // Material (Item) Field - Shows "Gender name" for Labour
                                                Column(
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                fieldPositions["item"] = coordinates.positionInParent().y
                            }
                                                ) {
                                                    Text(
                                                        text = if (formData.itemType == "Labour") "Gender" else "Material",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Medium,
                                                        color = colorScheme.onSurface,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                    )
                            val isItemTypeSelected = formData.itemType.isNotEmpty()
                            val items = if (isItemTypeSelected) {
                                expenseViewModel.getItemsForItemType(formData.itemType)
                            } else {
                                emptyList()
                            }

                            ExposedDropdownMenuBox(
                                expanded = showItemDropdown,
                                onExpandedChange = { showItemDropdown = it }
                            ) {
                                OutlinedTextField(
                                    value = formData.item,
                                    onValueChange = { },
                                    readOnly = true,
                                    enabled = true,
                                    placeholder = {
                                        Text(
                                            if (!isItemTypeSelected) "Select Sub-category first" else "Select Material",
                                            color = if (isItemTypeSelected) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = if (isItemTypeSelected) colorScheme.onSurfaceVariant else colorScheme.surfaceVariant
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = getTextFieldColors(
                                        isError = false,
                                        isEnabled = isItemTypeSelected,
                                        colorScheme = colorScheme
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = showItemDropdown && isItemTypeSelected,
                                    onDismissRequest = { showItemDropdown = false }
                                ) {
                                    if (!isItemTypeSelected) {
                                        DropdownMenuItem(
                                            text = { Text("Select Sub-category first") },
                                            onClick = { showItemDropdown = false }
                                        )
                                    } else if (items.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No materials available") },
                                            onClick = { }
                                        )
                                    } else {
                                        items.forEach { item ->
                                            DropdownMenuItem(
                                                text = { Text(item) },
                                                onClick = {
                                                    expenseViewModel.updateFormField("item", item)
                                                    showItemDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                        }
                                        }
                                    }

                        // Brand Field (Optional manual entry) - Hide for Labour
                        if (!isTurnkeyDepartment && !isContractorAmountEntry && !isTurnkeyAmountOnlyDepartment && formData.itemType != "Labour" && !showLineItemsUi) {
                            Column {
                                Text(
                                    text = "Brand",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                OutlinedTextField(
                                    value = formData.brand,
                                    onValueChange = { expenseViewModel.updateFormField("brand", it) },
                                    placeholder = { 
                                        Text(
                                            "Enter brand (optional)", 
                                            color = colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyLarge
                                        ) 
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = getTextFieldColors(
                                        isError = false,
                                        isEnabled = true,
                                        colorScheme = colorScheme
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Grade (Spec) Field - Hide for Labour
                        if (!isTurnkeyDepartment && !isContractorAmountEntry && !isTurnkeyAmountOnlyDepartment && formData.itemType != "Labour" && !showLineItemsUi) {
                            Column(
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    fieldPositions["spec"] = coordinates.positionInParent().y
                                }
                                        ) {
                                Text(
                                    text = "Grade",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                val isItemSelected = formData.item.isNotEmpty() && formData.itemType.isNotEmpty()
                                val specs = if (isItemSelected) {
                                    expenseViewModel.getSpecsForItemType(formData.itemType)
                                } else {
                                    emptyList()
                                }

                                ExposedDropdownMenuBox(
                                    expanded = showSpecDropdown,
                                    onExpandedChange = { showSpecDropdown = it }
                                ) {
                                    OutlinedTextField(
                                        value = formData.spec,
                                        onValueChange = { },
                                        readOnly = true,
                                        enabled = true,
                                        placeholder = {
                                            Text(
                                                if (!isItemSelected) "Select Material first" else "Select Grade",
                                                color = if (isItemSelected) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = if (isItemSelected) colorScheme.onSurfaceVariant else colorScheme.surfaceVariant
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        colors = getTextFieldColors(
                                            isError = false,
                                            isEnabled = isItemSelected,
                                            colorScheme = colorScheme
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = showSpecDropdown && isItemSelected,
                                        onDismissRequest = { showSpecDropdown = false }
                                    ) {
                                        if (!isItemSelected) {
                                            DropdownMenuItem(
                                                text = { Text("Select Material first") },
                                                onClick = { showSpecDropdown = false }
                                            )
                                        } else if (specs.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("No grades available") },
                                                onClick = { }
                                            )
                                        } else {
                                            specs.forEach { spec ->
                                                DropdownMenuItem(
                                                    text = { Text(spec) },
                                                    onClick = {
                                                        expenseViewModel.updateFormField("spec", spec)
                                                        showSpecDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Thickness Field (Default value) - Hide for Labour
                        if (!isTurnkeyDepartment && !isContractorAmountEntry && !isTurnkeyAmountOnlyDepartment && formData.itemType != "Labour" && !showLineItemsUi) {
                            Column {
                                Text(
                                    text = "Thickness",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                OutlinedTextField(
                                    value = formData.thickness,
                                    onValueChange = { expenseViewModel.updateFormField("thickness", it) },
                                    placeholder = { Text("16 mm", color = colorScheme.onSurfaceVariant) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = getTextFieldColors(
                                        isError = false,
                                        isEnabled = formData.thickness != "",
                                        colorScheme = colorScheme
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Quantity Field (Number only) - Shows "Members" for Labour
                        if (!isTurnkeyDepartment && !isContractorAmountEntry && !isTurnkeyAmountOnlyDepartment && !showLineItemsUi) Column {
                            Text(
                                text = if (formData.itemType == "Labour") "Members" else "Quantity",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            // Get remaining quantity for the selected line item
                            val remainingQty = remember(formData.department, formData.itemType, formData.item, formData.spec) {
                                expenseViewModel.getRemainingQuantityForLineItem(
                                    formData.itemType,
                                    formData.item,
                                    formData.spec
                                )
                            }
                            val enteredQty = formData.quantity.toDoubleOrNull() ?: 0.0
                            val isQuantityExceeded = remainingQty != null && enteredQty > remainingQty
                                OutlinedTextField(
                                value = formData.quantity,
                                onValueChange = { newValue ->
                                    // Only allow numbers and decimal point
                                    val filtered = newValue.filter { it.isDigit() || it == '.' }
                                    val enteredValue = filtered.toDoubleOrNull() ?: 0.0
                                    // Cap the value at remainingQuantity if available
                                    if (remainingQty != null && enteredValue > remainingQty) {
                                        // Don't allow input exceeding remaining quantity
                                        return@OutlinedTextField
                                    }
                                    expenseViewModel.updateFormField("quantity", filtered)
                                },
                                placeholder = { Text("0", color = colorScheme.onSurfaceVariant) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                colors = getTextFieldColors(
                                    isError = isQuantityExceeded,
                                    isEnabled = formData.quantity != "",
                                    colorScheme = colorScheme
                                ),
                                supportingText = if (remainingQty != null) {
                                    {
                                        Text(
                                            text = if (isQuantityExceeded) "Exceeds available: ${remainingQty}" else "Available: ${remainingQty}",
                                            color = if (isQuantityExceeded) colorScheme.error else colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                } else null,
                                isError = isQuantityExceeded,
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // UoM (Unit of Measure) Field - Dropdown for Labour, TextField for others
                        if (!isTurnkeyDepartment && !isContractorAmountEntry && !isTurnkeyAmountOnlyDepartment && !showLineItemsUi) Column {
                            Text(
                                text = "UoM",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Labour UoM options as per guide: ["nos", "sqft", "sft", "cft", "rmt", "kg", "ton", "ltr"]
                            val labourUomOptions = listOf("nos", "sqft", "sft", "cft", "rmt", "kg", "ton", "ltr")
                            val isLabour = formData.itemType == "Labour"

                            if (isLabour) {
                                // UoM Dropdown for Labour
                                ExposedDropdownMenuBox(
                                    expanded = showUomDropdown,
                                    onExpandedChange = { showUomDropdown = it }
                                ) {
                                    OutlinedTextField(
                                        value = formData.uom,
                                        onValueChange = { },
                                        readOnly = true,
                                        enabled = true,
                                        placeholder = {
                                            Text(
                                                if (labourUomOptions.isNotEmpty()) "Select UoM" else "Select UoM",
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = colorScheme.onSurfaceVariant
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        colors = getTextFieldColors(
                                            isError = false,
                                            isEnabled = formData.uom != "",
                                            colorScheme = colorScheme
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = showUomDropdown,
                                        onDismissRequest = { showUomDropdown = false }
                                    ) {
                                        labourUomOptions.forEach { uomOption ->
                                            DropdownMenuItem(
                                                text = { Text(uomOption) },
                                                onClick = {
                                                    expenseViewModel.updateFormField("uom", uomOption)
                                                    showUomDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                // UoM TextField for non-Labour (default: "ton")
                                OutlinedTextField(
                                    value = formData.uom,
                                    onValueChange = { expenseViewModel.updateFormField("uom", it) },
                                    placeholder = { Text("ton", color = colorScheme.onSurfaceVariant) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = true,
                                    colors = getTextFieldColors(
                                        isError = false,
                                        isEnabled = formData.uom != "",
                                        colorScheme = colorScheme
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Unit Price Field - Shows "{UoM} Price" for Labour, "Unit Price" for others
                        if (!isTurnkeyDepartment && !isContractorAmountEntry && !isTurnkeyAmountOnlyDepartment && !showLineItemsUi) Column {
                            Text(
                                text = if (formData.itemType == "Labour") {
                                    if (formData.uom.isNotEmpty()) {
                                        "${formData.uom} Price"
                                    } else {
                                        "UoM Price"
                                    }
                                } else {
                                    "Unit Price"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = formatNumberWithCommas(formData.unitPrice),
                                onValueChange = { newValue ->
                                    val cleanedValue = removeCommas(newValue)
                                    expenseViewModel.updateFormField("unitPrice", cleanedValue)
                                },
                                placeholder = { Text("0", color = colorScheme.onSurfaceVariant) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                colors = getTextFieldColors(
                                    isError = false,
                                    isEnabled = formData.unitPrice != "",
                                    colorScheme = colorScheme
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // Line Amount Field (Calculated, read-only)
                        if (!isTurnkeyDepartment && !isContractorAmountEntry && !isTurnkeyAmountOnlyDepartment && !showLineItemsUi) Column {
                            Text(
                                text = "Line Amount",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = formatNumberWithCommas(formData.amount),
                                onValueChange = { },
                                readOnly = true,
                                placeholder = { 
                                    Text(
                                        "0", 
                                        color = colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyLarge
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = getTextFieldColors(
                                    isError = false,
                                    isEnabled = true,
                                    colorScheme = colorScheme
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // Notes Field (Description) - hidden for Turnkey
                        if (!isTurnkeyDepartment && !isTurnkeyAmountOnlyDepartment) Column(
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                fieldPositions["description"] = coordinates.positionInParent().y
                            }
                        ) {
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val descriptionError = getFieldError("description")
                            OutlinedTextField(
                                value = formData.description,
                                onValueChange = { expenseViewModel.updateFormField("description", it) },
                                placeholder = { 
                                    Text(
                                        "Enter notes", 
                                        color = colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyLarge
                                    ) 
                                },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                                isError = descriptionError != null,
                                supportingText = descriptionError?.let {
                                    { Text(it, color = colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                                },
                                colors = getTextFieldColors(
                                    isError = descriptionError != null,
                                    isEnabled = true,
                                    colorScheme = colorScheme
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

                        // VENDOR Section (hidden for Turnkey)
                        if (!isTurnkeyDepartment && !isTurnkeyAmountOnlyDepartment) item {
                            Column(
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                        fieldPositions["vendor"] = coordinates.positionInParent().y
                    }
                ) {
                    Text(
                        text = "Vendor *",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    var showVendorDropdown by remember { mutableStateOf(false) }
                    var vendors by remember { mutableStateOf<Map<String, String>>(emptyMap()) } // Map<vendorName, departmentName>
                    var filteredVendors by remember { mutableStateOf<List<String>>(emptyList()) }
                    
                    // Fetch vendors from customers collection
                    LaunchedEffect(formData.department, departmentsFromSubcollection) {
                        try {
                            var phone = authState.user?.phone
                            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            var fetchedOwnerId: String? = null
                            if (userRole != UserRole.BUSINESS_HEAD && !phone.isNullOrEmpty()) { // Assuming 'phone' is your variable name
                                try {
                                    Log.d("OwnerFetch", "Fetching ownerId for phone document: $phone")

                                    val userDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(phone) // Using phone as the Document ID
                                        .get()
                                        .await() // Wait for the fetch to complete

                                    if (userDoc.exists()) {
                                        fetchedOwnerId = userDoc.getString("ownerID")
                                    }
                                } catch (e: Exception) {
                                }
                            }

                            val currentCustomerId = if (userRole == UserRole.BUSINESS_HEAD) firebaseUser?.uid else fetchedOwnerId

                            if (currentCustomerId != null && formData.department.isNotEmpty()) {
                                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val customerDoc = firestore.collection("customers")
                                    .document(currentCustomerId)
                                    .get()
                                    .await()
                                
                                if (customerDoc.exists()) {
                                    val vendorsAny = customerDoc.get("vendors")
                                    val allVendors: Map<String, String> = when (vendorsAny) {
                                        is Map<*, *> -> vendorsAny.mapNotNull { (k, v) ->
                                            val key = k as? String
                                            val value = v as? String
                                            if (key != null && value != null) key to value else null
                                        }.toMap()
                                        else -> emptyMap()
                                    }
                                    vendors = allVendors
                                    
                                    // Get actual department name from departmentsFromSubcollection
                                    val selectedDepartment = departmentsFromSubcollection.find { dept ->
                                        dept.name.equals(formData.department, ignoreCase = true)
                                    }
                                    val departmentNameForFiltering = selectedDepartment?.name ?: formData.department
                                    
                                    // Filter vendors by selected department (case-insensitive)
                                    filteredVendors = allVendors.filter { (_, dept) ->
                                        dept.equals(departmentNameForFiltering, ignoreCase = true)
                                    }.keys.toList().sorted() + listOf("Others")
                                } else {
                                    filteredVendors = emptyList()
                                }
                            } else {
                                filteredVendors = emptyList()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("AddExpenseScreen", "Error loading vendors: ${e.message}")
                            filteredVendors = emptyList()
                        }
                    }
                    
                    val vendorError = getFieldError("vendor")
                    
                    ExposedDropdownMenuBox(
                        expanded = showVendorDropdown,
                        onExpandedChange = { showVendorDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = formData.vendor,
                            onValueChange = { },
                            readOnly = true,
                            enabled = formData.department.isNotEmpty(),
                            placeholder = {
                                Text(
                                    if (formData.department.isEmpty()) "Select Department first" else "Select Vendor",
                                    color = if (formData.department.isNotEmpty()) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = if (formData.department.isNotEmpty()) colorScheme.onSurfaceVariant else colorScheme.surfaceVariant
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            isError = vendorError != null,
                            supportingText = vendorError?.let {
                                { Text(it, color = colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                            },
                            colors = getTextFieldColors(
                                isError = vendorError != null,
                                isEnabled = formData.department.isNotEmpty(),
                                colorScheme = colorScheme
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showVendorDropdown,
                            onDismissRequest = { showVendorDropdown = false }
                        ) {
                            if (filteredVendors.isEmpty() && formData.department.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            "No vendors found for this department", 
                                            color = colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        ) 
                                    },
                                    onClick = { },
                                    enabled = false
                                )
                            } else {
                                filteredVendors.forEach { vendorName ->
                                    DropdownMenuItem(
                                        text = { Text(vendorName) },
                                        onClick = {
                                            expenseViewModel.updateFormField("vendor", vendorName)
                                            showVendorDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // BY CREDIT Section (hidden for Turnkey)
            if (!isTurnkeyDepartment && !isTurnkeyAmountOnlyDepartment) item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "By Credit",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurface
                    )
                    Switch(
                        checked = formData.byCredit,
                        onCheckedChange = { expenseViewModel.updateByCredit(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.onPrimary,
                            checkedTrackColor = colorScheme.primary,
                            uncheckedThumbColor = colorScheme.outline,
                            uncheckedTrackColor = colorScheme.surfaceContainerHighest
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }


            // SGST & CGST Section (hidden for Turnkey)
            if (!isTurnkeyDepartment && !isTurnkeyAmountOnlyDepartment) item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SGST Field
                    Column(
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            fieldPositions["sgst"] = coordinates.positionInParent().y
                        }
                    ) {
                        val sgstError = getFieldError("sgst")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SGST",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = " *",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = formData.sgst,
                            onValueChange = { expenseViewModel.updateFormField("sgst", it) },
                            placeholder = {
                                Text(
                                    "Enter SGST",
                                    color = colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            isError = sgstError != null,
                            supportingText = sgstError?.let {
                                { Text(it, color = colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                            },
                            colors = getTextFieldColors(
                                isError = sgstError != null,
                                isEnabled = true,
                                colorScheme = colorScheme
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    // CGST Field
                    Column(
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            fieldPositions["cgst"] = coordinates.positionInParent().y
                        }
                    ) {
                        val cgstError = getFieldError("cgst")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CGST",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = " *",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = formData.cgst,
                            onValueChange = { expenseViewModel.updateFormField("cgst", it) },
                            placeholder = {
                                Text(
                                    "Enter CGST",
                                    color = colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            isError = cgstError != null,
                            supportingText = cgstError?.let {
                                { Text(it, color = colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                            },
                            colors = getTextFieldColors(
                                isError = cgstError != null,
                                isEnabled = true,
                                colorScheme = colorScheme
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ATTACHMENT Section (hidden for Turnkey)
            if (!isTurnkeyDepartment && !isTurnkeyAmountOnlyDepartment) item {
                Column(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        fieldPositions["attachment"] = coordinates.positionInParent().y
                    }
                ) {
                    Text(
                        text = "Attachment",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                    )
                    val attachmentError = getFieldError("attachment")
                    FilledTonalButton(
                        onClick = { showAttachmentDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (attachmentError != null) colorScheme.errorContainer else colorScheme.secondaryContainer,
                            contentColor = if (attachmentError != null) colorScheme.onErrorContainer else colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = "Add Attachment",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Add Attachment",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    // Show error message for attachment
                    attachmentError?.let { errorMsg ->
                        Text(
                            text = errorMsg,
                            color = colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                        )
                    }
                }

                // Show selected attachment info
                selectedAttachmentName?.let { name ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // View attachment when clicked
                                selectedAttachmentUri?.let { uri ->
                                    try {
                                        val mimeType = context.contentResolver.getType(uri) ?: "*/*"
                                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, mimeType)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(viewIntent)
                                    } catch (e: Exception) {
                                        android.util.Log.e(
                                            "AddExpenseScreen",
                                            "Error viewing attachment: ${e.message}",
                                            e
                                        )
                                    }
                                }
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
                                Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    selectedAttachmentUri = null
                                    selectedAttachmentName = null
                                    expenseViewModel.updateFormField("attachmentUri", "")
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = colorScheme.error
                                )
                            ) {
                                Text(
                                    "Remove", 
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Submit Button - Material 3 styled
            item {
                Button(
                    onClick = {
                        if (shouldBlockSubmitForCompletedDepartment) {
                            return@Button
                        }
                        // Always allow clicking - validation will handle missing fields
                        // Check if budget validation failed
                        if (budgetValidationResult?.isValid == false) {
                            showBudgetExceededDialog = true
                        } else {
                            // Submit expense - validation will scroll to missing fields if any
                            // The validation will set fieldError which triggers auto-scroll
                            expenseViewModel.submitExpense(
                                projectId = project.id ?: "",
                                userId = userId,
                                userName = userName,
                                onSuccess = onExpenseAdded,
                                expenseId = expenseId
                            )
                        }
                    },
                    enabled = !isSubmitting && !isProjectDisabled && !shouldBlockSubmitForCompletedDepartment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubmitting || isProjectDisabled) 
                            colorScheme.surfaceContainerHighest 
                        else 
                            colorScheme.primary,
                        disabledContainerColor = colorScheme.surfaceContainerHighest,
                        contentColor = if (isSubmitting || isProjectDisabled)
                            colorScheme.onSurfaceVariant
                        else
                            colorScheme.onPrimary,
                        disabledContentColor = colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        disabledElevation = 0.dp
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Submitting...",
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = if (expenseId != null) "Update Expense" else "Submit for Approval",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { dateString ->
                expenseViewModel.updateFormField("date", dateString)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Attachment Selection Dialog
    if (showAttachmentDialog) {
        AttachmentSelectionDialog(
            onCameraSelected = {
                showAttachmentDialog = false
                // Check camera permission
                when (PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                        // Create a temporary URI for the camera to save the image
                        val tempUri = context.contentResolver.insert(
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            android.content.ContentValues()
                        )
                        selectedAttachmentUri = tempUri
                        tempUri?.let { cameraLauncher.launch(it) }
                    }
                    else -> {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            },
            onPhotoSelected = {
                showAttachmentDialog = false
                photoPickerLauncher.launch("image/*")
            },
            onPdfSelected = {
                showAttachmentDialog = false
                documentPickerLauncher.launch("application/pdf")
            },
            onDismiss = { showAttachmentDialog = false }
        )
    }

    // Payment Proof Selection Dialog (only images)
    if (showPaymentProofDialog) {
        AttachmentSelectionDialog(
            onCameraSelected = {
                showPaymentProofDialog = false
                // Check camera permission
                when (PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                        // Create a temporary URI for the camera to save the image
                        val tempUri = context.contentResolver.insert(
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            android.content.ContentValues()
                        )
                        selectedPaymentProofUri = tempUri
                        tempUri?.let { paymentProofCameraLauncher.launch(it) }
                    }
                    else -> {
                        paymentProofCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            },
            onPhotoSelected = {
                showPaymentProofDialog = false
                paymentProofPhotoPickerLauncher.launch("image/*")
            },
            onPdfSelected = {
                // PDF not allowed for payment proof, only images
                showPaymentProofDialog = false
            },
            onDismiss = { showPaymentProofDialog = false }
        )
    }

    // Budget Exceeded Dialog
    if (showBudgetExceededDialog && budgetValidationResult != null) {
        val result = budgetValidationResult!!
        BudgetExceededDialog(
            budgetValidationResult = result,
            onDismiss = {
                showBudgetExceededDialog = false
                expenseViewModel.clearBudgetWarning()
            },
            onProceed = {
                showBudgetExceededDialog = false
                project.id?.let {
                    expenseViewModel.submitExpense(
                        projectId = it,
                        userId = userId,
                        userName = userName,
                        onSuccess = onExpenseAdded,
                        budgetExceeded = true, // Mark that budget is exceeded
                        expenseId = expenseId
                    )
                }
            }
        )
    }

    // Add Line Item Sheet for Labour-Only / Self-Execution
    if (showAddExpenseLineItemSheet) {
        val lineItemContractorMode = if (isLabourOnlyDepartment) ContractorMode.LABOUR_ONLY else ContractorMode.SELF_EXECUTION
        AddLineItemModalSheet(
            lineItem = editingExpenseLineItemIndex?.let { expenseLineItems.getOrNull(it) } ?: LineItem(),
            contractorMode = lineItemContractorMode,
            onDismiss = {
                showAddExpenseLineItemSheet = false
                editingExpenseLineItemIndex = null
            },
            onAdd = { newLineItem ->
                val updatedList = expenseLineItems.toMutableList()
                val idx = editingExpenseLineItemIndex
                if (idx != null && idx < updatedList.size) {
                    updatedList[idx] = newLineItem
                } else {
                    updatedList.add(newLineItem)
                }
                expenseLineItems = updatedList
                showAddExpenseLineItemSheet = false
                editingExpenseLineItemIndex = null
            },
            isEditMode = editingExpenseLineItemIndex != null
        )
    }
}

@Composable
fun ProjectDetailsCard(project: Project) {
    val colorScheme = MaterialTheme.colorScheme
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
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
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            TruncatedTextWithPopup(
                text = project.name,
                maxLength = 10,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Tracura",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.timeInMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.timeInMillis = millis
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    onDateSelected(dateFormat.format(selectedCalendar.time))
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}



@Composable
fun AttachmentSelectionDialog(
    onCameraSelected: () -> Unit,
    onPhotoSelected: () -> Unit,
    onPdfSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Attachment",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Choose how you want to add an attachment:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Camera Option
                AttachmentOptionItem(
                    icon = Icons.Default.Add,
                    title = "Take Photo",
                    description = "Use camera to capture receipt",
                    onClick = onCameraSelected
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Photo Gallery Option
                AttachmentOptionItem(
                    icon = Icons.Default.AccountCircle,
                    title = "Choose Photo",
                    description = "Select from photo gallery",
                    onClick = onPhotoSelected
                )

                Spacer(modifier = Modifier.height(8.dp))

                // PDF Option
                AttachmentOptionItem(
                    icon = Icons.Default.Email,
                    title = "Add PDF",
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
        }
    )
}

@Composable
private fun AttachmentOptionItem(
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
            containerColor = colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
