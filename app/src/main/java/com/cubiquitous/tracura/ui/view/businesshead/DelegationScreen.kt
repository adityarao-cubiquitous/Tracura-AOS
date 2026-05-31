package com.cubiquitous.tracura.ui.view.businesshead

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.DepartmentTemporaryApproverEntry
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.viewmodel.BusinessHeadViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelegationScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    businessHeadViewModel: BusinessHeadViewModel = hiltViewModel()
) {
    val palette = businessHeadUiPalette()
    val editingProject by businessHeadViewModel.editingProject.collectAsState()
    val isLoading by businessHeadViewModel.isLoading.collectAsState()
    val successMessage by businessHeadViewModel.successMessage.collectAsState()
    val error by businessHeadViewModel.error.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var editingDelegation by remember { mutableStateOf<DepartmentTemporaryApproverEntry?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Load project on entry
    LaunchedEffect(projectId) {
        businessHeadViewModel.loadProjectForEdit(projectId)
    }

    // Show snackbar on success / error
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            businessHeadViewModel.clearSuccessMessage()
        }
    }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            businessHeadViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Delegation Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = palette.accent)
                    }
                },
                actions = {
                    FilledIconButton(
                        onClick = { showAddSheet = true },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = palette.accent
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Delegation",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.tier2Surface,
                    titleContentColor = palette.primaryText,
                    navigationIconContentColor = palette.accent,
                    actionIconContentColor = palette.accent
                )
            )
        },
        containerColor = palette.tier1Background
    ) { paddingValues ->

        if (isLoading && editingProject == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val delegations = editingProject?.departmentTemporaryApprover ?: emptyList()

            if (delegations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(palette.accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = palette.accent
                            )
                        }
                        Text(
                            text = "No Delegations Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.primaryText
                        )
                        Text(
                            text = "Tap + to add a temporary approver delegation",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryText
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "${delegations.size} delegation${if (delegations.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.secondaryText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(delegations) { approver ->
                        val todayAtMidnight = remember {
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.time
                        }
                        val canEditDates = approver.endDate?.let { !it.before(todayAtMidnight) } == true

                        DelegationCard(
                            palette = palette,
                            approver = approver,
                            onEditDates = if (canEditDates) {
                                { editingDelegation = approver }
                            } else {
                                null
                            },
                            onToggle = { newActive ->
                                businessHeadViewModel.toggleDepartmentTemporaryApproverActive(
                                    projectId = projectId,
                                    approverPhone = approver.phone,
                                    departmentName = approver.departmentName,
                                    isActive = newActive
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // ── Add Delegation Bottom Sheet ──────────────────────────────────────────
    if (showAddSheet) {
        val departments = remember(editingProject) {
            editingProject?.departmentApproverAssignments?.keys?.toList() ?: emptyList()
        }

        AddDelegationBottomSheet(
            projectId = projectId,
            departments = departments,
            existingDelegations = editingProject?.departmentTemporaryApprover ?: emptyList(),
            onDismiss = { showAddSheet = false },
            onSaved = {
                showAddSheet = false
                // _editingProject is refreshed by saveDepartmentTemporaryApprover
            },
            businessHeadViewModel = businessHeadViewModel
        )
    }

    editingDelegation?.let { delegation ->
        EditDelegationDatesBottomSheet(
            projectId = projectId,
            delegation = delegation,
            onDismiss = { editingDelegation = null },
            onSaved = { editingDelegation = null },
            businessHeadViewModel = businessHeadViewModel
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Delegation Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DelegationCard(
    palette: BusinessHeadUiPalette,
    approver: DepartmentTemporaryApproverEntry,
    onEditDates: (() -> Unit)? = null,
    onToggle: (Boolean) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val isActive = approver.isAccepted

    val containerColor by animateColorAsState(
        targetValue = if (isActive)
            palette.accent.copy(alpha = 0.16f)
        else
            palette.tier3Field,
        animationSpec = tween(300),
        label = "cardColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.tier2Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (androidx.compose.foundation.isSystemInDarkTheme()) 0.dp else 1.dp),
        border = androidx.compose.foundation.BorderStroke(0.75.dp, palette.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerColor)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Department badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.approverContainer.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = approver.departmentName.ifBlank { "—" },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.approverText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Active toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onEditDates != null) {
                        IconButton(
                            onClick = onEditDates,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit dates",
                                tint = palette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }

                    Text(
                        text = if (isActive) "Active" else "Inactive",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive)
                            palette.accent
                        else
                            palette.secondaryText
                    )
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = isActive,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = palette.success,
                            uncheckedTrackColor = palette.outline,
                            checkedThumbColor = Color.White,
                            uncheckedThumbColor = Color.White
                        ),
                        thumbContent = if (isActive) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        } else null
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Approver phone
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = palette.accent
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = approver.phone.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.primaryText
                )
            }

            Spacer(Modifier.height(8.dp))

            // Date range
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = palette.accent
                )
                Spacer(Modifier.width(6.dp))
                val startStr = approver.startDate?.let { dateFormat.format(it) } ?: "—"
                val endStr = approver.endDate?.let { dateFormat.format(it) } ?: "Ongoing"
                Text(
                    text = "$startStr → $endStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryText
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add Delegation Bottom Sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDelegationBottomSheet(
    projectId: String,
    departments: List<String>,
    existingDelegations: List<DepartmentTemporaryApproverEntry>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    businessHeadViewModel: BusinessHeadViewModel
) {
    val palette = businessHeadUiPalette()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var selectedDepartment by remember { mutableStateOf("") }
    var departmentExpanded by remember { mutableStateOf(false) }

    var selectedApprover by remember { mutableStateOf<User?>(null) }
    var approverExpanded by remember { mutableStateOf(false) }
    var availableApprovers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoadingApprovers by remember { mutableStateOf(false) }

    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var showDateConflictDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val successMessage by businessHeadViewModel.successMessage.collectAsState()
    val errorMessage by businessHeadViewModel.error.collectAsState()

    LaunchedEffect(errorMessage) {
        if (isSaving && !errorMessage.isNullOrBlank()) {
            isSaving = false
        }
    }

    // Load approvers when department changes
    LaunchedEffect(selectedDepartment) {
        if (selectedDepartment.isBlank()) {
            availableApprovers = emptyList()
            selectedApprover = null
            return@LaunchedEffect
        }
        isLoadingApprovers = true
        selectedApprover = null
        try {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val customerId = firebaseUser?.uid
            
            // Get the phone number of the current approver for this department from the project
            val currentProject = businessHeadViewModel.editingProject.value
            val currentApproverPhone = currentProject?.departmentApproverAssignments?.get(selectedDepartment)
            
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("role", "APPROVER")
                .get()
                .await()
            val all = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val dept = data["department"] as? String ?: ""
                val cid = (data["ownerID"] as? String) ?: (data["customerId"] as? String)
                val phone = data["phoneNumber"] as? String ?: ""
                
                // Filter logic:
                // 1. Must match selected department
                // 2. Must belong to same customer
                // 3. Must NOT be the current main approver for this department
                if (dept.equals(selectedDepartment, ignoreCase = true) &&
                    (customerId == null || cid == customerId) &&
                    (currentApproverPhone == null || phone != currentApproverPhone)
                ) {
                    User(
                        uid = data["uid"] as? String ?: doc.id,
                        name = data["name"] as? String ?: "",
                        email = data["email"] as? String ?: "",
                        phone = phone,
                        role = UserRole.APPROVER,
                        isActive = data["isActive"] as? Boolean ?: true,
                        customerId = cid,
                        department = dept
                    )
                } else null
            }
            availableApprovers = all
        } catch (e: Exception) {
            availableApprovers = emptyList()
        } finally {
            isLoadingApprovers = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = palette.tier2Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Delegation",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(palette.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = palette.accent
                    )
                }
            }

            HorizontalDivider(color = palette.outline)

            // ── Department Dropdown ──────────────────────────────────────────
            SectionLabel("Department")
            ExposedDropdownMenuBox(
                expanded = departmentExpanded,
                onExpandedChange = { departmentExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedDepartment.ifBlank { "Select department" },
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = departmentExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedContainerColor = palette.tier3Field,
                        unfocusedContainerColor = palette.tier3Field,
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.outline,
                        focusedTextColor = palette.primaryText,
                        unfocusedTextColor = palette.primaryText
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = departmentExpanded,
                    onDismissRequest = { departmentExpanded = false }
                ) {
                    if (departments.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No departments available") },
                            onClick = { departmentExpanded = false },
                            enabled = false
                        )
                    } else {
                        departments.forEach { dept ->
                            DropdownMenuItem(
                                text = { Text(dept) },
                                onClick = {
                                    selectedDepartment = dept
                                    departmentExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Approver Dropdown ────────────────────────────────────────────
            SectionLabel("Approver")
            ExposedDropdownMenuBox(
                expanded = approverExpanded && !isLoadingApprovers,
                onExpandedChange = {
                    if (selectedDepartment.isNotBlank() && !isLoadingApprovers)
                        approverExpanded = it
                }
            ) {
                OutlinedTextField(
                    value = when {
                        selectedDepartment.isBlank() -> "Select department first"
                        isLoadingApprovers -> "Loading approvers…"
                        selectedApprover != null -> "${selectedApprover!!.name} (${selectedApprover!!.phone})"
                        else -> "Select approver"
                    },
                    onValueChange = {},
                    readOnly = true,
                    enabled = selectedDepartment.isNotBlank() && !isLoadingApprovers,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        if (isLoadingApprovers) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = approverExpanded)
                        }
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedContainerColor = palette.tier3Field,
                        unfocusedContainerColor = palette.tier3Field,
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.outline,
                        focusedTextColor = palette.primaryText,
                        unfocusedTextColor = palette.primaryText
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = approverExpanded && !isLoadingApprovers,
                    onDismissRequest = { approverExpanded = false }
                ) {
                    if (availableApprovers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No approvers for this department") },
                            onClick = { approverExpanded = false },
                            enabled = false
                        )
                    } else {
                        availableApprovers.forEach { user ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = user.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = user.phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.secondaryText
                                        )
                                    }
                                },
                                onClick = {
                                    selectedApprover = user
                                    approverExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Start Date ───────────────────────────────────────────────────
            SectionLabel("Start Date")
            OutlinedTextField(
                value = startDate?.let { dateFormat.format(it) } ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Select start date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStartDatePicker = true },
                enabled = false,
                trailingIcon = {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = palette.accent
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = palette.primaryText,
                    disabledBorderColor = palette.outline,
                    disabledTrailingIconColor = palette.accent,
                    disabledPlaceholderColor = palette.secondaryText,
                    disabledContainerColor = palette.tier3Field
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // ── End Date ─────────────────────────────────────────────────────
            SectionLabel("End Date (Optional)")
            OutlinedTextField(
                value = endDate?.let { dateFormat.format(it) } ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Select end date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEndDatePicker = true },
                enabled = false,
                trailingIcon = {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = palette.accent
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = palette.primaryText,
                    disabledBorderColor = palette.outline,
                    disabledTrailingIconColor = palette.accent,
                    disabledPlaceholderColor = palette.secondaryText,
                    disabledContainerColor = palette.tier3Field
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(4.dp))

            // ── Save Button ──────────────────────────────────────────────────
            val canSave = selectedDepartment.isNotBlank() &&
                    selectedApprover != null &&
                    startDate != null &&
                    !isSaving

            Button(
                onClick = {
                    val approver = selectedApprover ?: return@Button
                    val sd = startDate ?: return@Button

                    val sameDepartmentDelegations = existingDelegations.filter {
                        it.departmentName.equals(selectedDepartment, ignoreCase = true)
                    }
                    val latestEndDate = sameDepartmentDelegations
                        .mapNotNull { it.endDate }
                        .maxByOrNull { it.time }

                    val hasOngoingWithoutEndDate = sameDepartmentDelegations.any { it.endDate == null }
                    val hasDateConflict = hasOngoingWithoutEndDate || (latestEndDate != null && !sd.after(latestEndDate))

                    if (hasDateConflict) {
                        showDateConflictDialog = true
                        return@Button
                    }

                    isSaving = true
                    val newEntry = DepartmentTemporaryApproverEntry(
                        phone = approver.phone,
                        startDate = sd,
                        endDate = endDate,
                        isAccepted = true,
                        departmentName = selectedDepartment
                    )
                    businessHeadViewModel.saveDepartmentTemporaryApprover(
                        projectId = projectId,
                        approver = newEntry
                    )
                    onSaved()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = canSave,
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Save Delegation",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // ── Date Pickers ─────────────────────────────────────────────────────────
    if (showStartDatePicker) {
        SimpleDatePickerDialog(
            title = "Select Start Date",
            initialDate = startDate ?: Date(),
            onDateSelected = {
                startDate = it
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        SimpleDatePickerDialog(
            title = "Select End Date",
            initialDate = endDate ?: (startDate ?: Date()),
            onDateSelected = {
                endDate = it
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }

    if (showDateConflictDialog) {
        AlertDialog(
            onDismissRequest = { showDateConflictDialog = false },
            title = { Text("Date Conflict") },
            text = {
                Text("For this department, the new temp approver start date must be after the existing end date. Please edit existing temp approver to extend to required date.")
            },
            confirmButton = {
                TextButton(onClick = { showDateConflictDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDelegationDatesBottomSheet(
    projectId: String,
    delegation: DepartmentTemporaryApproverEntry,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    businessHeadViewModel: BusinessHeadViewModel
) {
    val palette = businessHeadUiPalette()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val successMessage by businessHeadViewModel.successMessage.collectAsState()
    val errorMessage by businessHeadViewModel.error.collectAsState()

    var startDate by remember(delegation) { mutableStateOf(delegation.startDate) }
    var endDate by remember(delegation) { mutableStateOf(delegation.endDate) }
    var isUpdating by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val todayAtMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
    val canEditStartDate = delegation.startDate?.after(todayAtMidnight) == true

    LaunchedEffect(successMessage, isUpdating) {
        if (isUpdating && successMessage == "Delegation dates updated successfully") {
            isUpdating = false
            sheetState.hide()
            onSaved()
        }
    }

    LaunchedEffect(errorMessage, isUpdating) {
        if (isUpdating && !errorMessage.isNullOrBlank()) {
            isUpdating = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = palette.tier2Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Edit Delegation Dates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = palette.primaryText
            )

            SectionLabel("Start Date")
            OutlinedTextField(
                value = startDate?.let { dateFormat.format(it) } ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                placeholder = { Text("Select start date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = canEditStartDate) { showStartDatePicker = true },
                trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = palette.accent) },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = palette.primaryText,
                    disabledBorderColor = palette.outline,
                    disabledContainerColor = palette.tier3Field
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (!canEditStartDate) {
                Text(
                    text = "Start date can no longer be edited after it has started.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryText
                )
            }

            SectionLabel("End Date")
            OutlinedTextField(
                value = endDate?.let { dateFormat.format(it) } ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                placeholder = { Text("Select end date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEndDatePicker = true },
                trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = palette.accent) },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = palette.primaryText,
                    disabledBorderColor = palette.outline,
                    disabledContainerColor = palette.tier3Field
                ),
                shape = RoundedCornerShape(12.dp)
            )

            val canUpdate = startDate != null && !isUpdating

            Button(
                onClick = {
                    val newStart = startDate ?: return@Button
                    isUpdating = true
                    businessHeadViewModel.updateDepartmentTemporaryApproverDates(
                        projectId = projectId,
                        approverPhone = delegation.phone,
                        departmentName = delegation.departmentName,
                        originalStartDate = delegation.startDate,
                        originalEndDate = delegation.endDate,
                        newStartDate = newStart,
                        newEndDate = endDate
                    )
                },
                enabled = canUpdate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Update Dates", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showStartDatePicker) {
        SimpleDatePickerDialog(
            title = "Select Start Date",
            initialDate = startDate ?: Date(),
            onDateSelected = {
                startDate = it
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        SimpleDatePickerDialog(
            title = "Select End Date",
            initialDate = endDate ?: (startDate ?: Date()),
            onDateSelected = {
                endDate = it
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    val palette = businessHeadUiPalette()
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = palette.secondaryText
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDatePickerDialog(
    title: String,
    initialDate: Date,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = businessHeadUiPalette()
    val cal = Calendar.getInstance().apply { time = initialDate }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = cal.timeInMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    onDateSelected(Date(it))
                } ?: onDismiss()
            }) { Text("OK", color = palette.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryText) }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = { Text(title, modifier = Modifier.padding(start = 24.dp, top = 16.dp)) }
        )
    }
}
