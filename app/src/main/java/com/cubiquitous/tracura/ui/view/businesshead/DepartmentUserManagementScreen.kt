package com.cubiquitous.tracura.ui.view.businesshead

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.viewmodel.BusinessHeadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentUserManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: BusinessHeadViewModel = hiltViewModel()
) {
    val palette = businessHeadUiPalette()
    val customerDepartments by viewModel.customerDepartments.collectAsState()
    val departmentUsersMap by viewModel.departmentUsersMap.collectAsState()
    val departmentApproversMap by viewModel.departmentApproversMap.collectAsState()
    val availableUsers by viewModel.availableUsers.collectAsState()
    val availableApprovers by viewModel.availableApprovers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    // Bottom sheet state for user/approver picker
    var showPickerSheet by remember { mutableStateOf(false) }
    var pickerDepartment by remember { mutableStateOf("") }
    var pickerIsApprover by remember { mutableStateOf(false) }
    var pickerOldUserPhone by remember { mutableStateOf<String?>(null) } // null = assign, non-null = replace
    var pickerOldUserName by remember { mutableStateOf("") }

    // Confirmation dialog
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmNewUser by remember { mutableStateOf<User?>(null) }

    // Search query for the picker
    var searchQuery by remember { mutableStateOf("") }

    // Create new user dialog
    var showCreateUserDialog by remember { mutableStateOf(false) }
    var newUserPhone by remember { mutableStateOf("") }
    var newUserName by remember { mutableStateOf("") }
    var newUserPhoneError by remember { mutableStateOf<String?>(null) }
    val showDuplicateAlert by viewModel.showDuplicateAlert.collectAsState()
    val pendingUserData by viewModel.pendingUserData.collectAsState()

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Load data
    LaunchedEffect(Unit) {
        viewModel.loadCustomerDepartments()
        viewModel.loadUsersGroupedByDepartment()
    }

    // Handle success messages
    LaunchedEffect(successMessage) {
        successMessage?.let {
            if (showCreateUserDialog) {
                showCreateUserDialog = false
                showPickerSheet = false
                newUserPhone = ""
                newUserName = ""
                newUserPhoneError = null
            }
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccessMessage()
        }
    }

    // Handle errors
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Department Users",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = palette.tier1Background
    ) { paddingValues ->

        when {
            isLoading && customerDepartments.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = palette.accent)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading departments…", color = palette.secondaryText, fontSize = 14.sp)
                    }
                }
            }

            customerDepartments.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Business,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = palette.secondaryText
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No Departments",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Create departments first from the project creation flow.",
                            fontSize = 14.sp,
                            color = palette.secondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                // Loading overlay
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header summary
                        item {
                            Text(
                                text = "${customerDepartments.size} department${if (customerDepartments.size != 1) "s" else ""}",
                                fontSize = 13.sp,
                                color = palette.secondaryText,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(customerDepartments) { department ->
                            DepartmentCard(
                                palette = palette,
                                departmentName = department,
                                users = departmentUsersMap[department] ?: emptyList(),
                                approvers = departmentApproversMap[department] ?: emptyList(),
                                onAssignUser = {
                                    pickerDepartment = department
                                    pickerIsApprover = false
                                    pickerOldUserPhone = null
                                    pickerOldUserName = ""
                                    searchQuery = ""
                                    showPickerSheet = true
                                },
                                onReplaceUser = { oldUser ->
                                    pickerDepartment = department
                                    pickerIsApprover = false
                                    pickerOldUserPhone = oldUser.phone
                                    pickerOldUserName = oldUser.name
                                    searchQuery = ""
                                    showPickerSheet = true
                                },
                                onAssignApprover = {
                                    pickerDepartment = department
                                    pickerIsApprover = true
                                    pickerOldUserPhone = null
                                    pickerOldUserName = ""
                                    searchQuery = ""
                                    showPickerSheet = true
                                },
                                onReplaceApprover = { oldApprover ->
                                    pickerDepartment = department
                                    pickerIsApprover = true
                                    pickerOldUserPhone = oldApprover.phone
                                    pickerOldUserName = oldApprover.name
                                    searchQuery = ""
                                    showPickerSheet = true
                                }
                            )
                        }

                        // Bottom spacer
                        item { Spacer(Modifier.height(24.dp)) }
                    }

                    // Loading overlay
                    AnimatedVisibility(
                        visible = isLoading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(palette.scrim),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = palette.tier2Surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = palette.accent)
                                    Spacer(Modifier.height(16.dp))
                                    Text("Updating…", fontSize = 14.sp, color = palette.secondaryText)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── User / Approver Picker Bottom Sheet ──────────────────────────────────
    if (showPickerSheet) {
        val candidateList = if (pickerIsApprover) availableApprovers else availableUsers
        val filteredCandidates = candidateList.filter { user ->
            // Only show users with no department assigned (unassigned users)
            user.department.isBlank() &&
            // Exclude the old user (they're being replaced)
            user.phone != pickerOldUserPhone &&
            (searchQuery.isBlank() ||
                user.name.contains(searchQuery, ignoreCase = true) ||
                user.phone.contains(searchQuery))
        }

        ModalBottomSheet(
            onDismissRequest = { showPickerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = palette.tier2Surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Header
                Text(
                    text = if (pickerOldUserPhone != null) {
                        "Replace ${if (pickerIsApprover) "Approver" else "User"} in $pickerDepartment"
                    } else {
                        "Assign ${if (pickerIsApprover) "Approver" else "User"} to $pickerDepartment"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (pickerOldUserPhone != null) {
                    Text(
                        text = "Replacing: $pickerOldUserName",
                        fontSize = 13.sp,
                        color = palette.secondaryText,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                }

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or phone…", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = palette.secondaryText) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = palette.secondaryText)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.tier3Field,
                        unfocusedContainerColor = palette.tier3Field,
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.outline,
                        focusedTextColor = palette.primaryText,
                        unfocusedTextColor = palette.primaryText,
                        focusedPlaceholderColor = palette.secondaryText,
                        unfocusedPlaceholderColor = palette.secondaryText,
                        focusedLeadingIconColor = palette.secondaryText,
                        unfocusedLeadingIconColor = palette.secondaryText
                    )
                )

                // Create new user option
                OutlinedButton(
                    onClick = {
                        showPickerSheet = false
                        showCreateUserDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(palette.accent)
                    )
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = palette.accent
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Create New ${if (pickerIsApprover) "Approver" else "User"}",
                        color = palette.accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                if (filteredCandidates.isNotEmpty()) {
                    Text(
                        text = "Or pick from existing",
                        fontSize = 12.sp,
                        color = palette.secondaryText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Candidates list
                if (filteredCandidates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.PersonOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = palette.secondaryText
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No ${if (pickerIsApprover) "approvers" else "users"} available",
                                fontSize = 14.sp,
                                color = palette.secondaryText
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filteredCandidates) { user ->
                            CandidateUserRow(
                                palette = palette,
                                user = user,
                                isApprover = pickerIsApprover,
                                onClick = {
                                    if (pickerOldUserPhone != null) {
                                        // Replace flow → show confirmation
                                        confirmNewUser = user
                                        showConfirmDialog = true
                                    } else {
                                        // Assign flow → assign directly
                                        viewModel.assignUserToDepartment(
                                            departmentName = pickerDepartment,
                                            userPhone = user.phone,
                                            isApprover = pickerIsApprover
                                        )
                                        showPickerSheet = false
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // ── Confirmation Dialog ──────────────────────────────────────────────────
    if (showConfirmDialog && confirmNewUser != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                confirmNewUser = null
            },
            icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = palette.accent) },
            title = {
                Text(
                    "Replace ${if (pickerIsApprover) "Approver" else "User"}?",
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
            },
            text = {
                Column {
                    Text(
                        "Department: $pickerDepartment",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = palette.primaryText
                    )
                    Spacer(Modifier.height(12.dp))

                    // Old user
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PersonRemove,
                            contentDescription = null,
                            tint = palette.danger,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = pickerOldUserName,
                            fontSize = 14.sp,
                            color = palette.danger
                        )
                    }

                    // Arrow
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = palette.secondaryText,
                        modifier = Modifier
                            .padding(vertical = 4.dp, horizontal = 10.dp)
                            .size(18.dp)
                    )

                    // New user
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = palette.userText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = confirmNewUser!!.name,
                            fontSize = 14.sp,
                            color = palette.userText,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "All projects with this department will be updated automatically.",
                        fontSize = 12.sp,
                        color = palette.secondaryText
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        confirmNewUser?.let { newUser ->
                            viewModel.replaceDepartmentAssignment(
                                departmentName = pickerDepartment,
                                oldUserPhone = pickerOldUserPhone!!,
                                newUserPhone = newUser.phone,
                                isApprover = pickerIsApprover
                            )
                        }
                        showConfirmDialog = false
                        confirmNewUser = null
                        showPickerSheet = false
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = palette.approverContainer,
                        contentColor = palette.accent
                    )
                ) {
                    Text("Replace", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    confirmNewUser = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Create New User Dialog ────────────────────────────────────────────────
    if (showCreateUserDialog) {
        val roleLabel = if (pickerIsApprover) "Approver" else "User"
        AlertDialog(
            onDismissRequest = {
                showCreateUserDialog = false
                viewModel.clearError()
                newUserPhone = ""
                newUserName = ""
                newUserPhoneError = null
            },
            icon = {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = palette.accent)
            },
            title = {
                Text(
                    "Create New $roleLabel",
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
            },
            text = {
                Column {
                    Text(
                        text = "Department: $pickerDepartment",
                        fontSize = 13.sp,
                        color = palette.secondaryText,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = newUserPhone,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 10) {
                                newUserPhone = digits
                                newUserPhoneError = when {
                                    digits.length == 10 && digits.all { it == digits[0] } -> "Invalid phone number"
                                    digits.length in 1..9 -> "Must be 10 digits"
                                    else -> null
                                }
                            }
                        },
                        label = { Text("Phone Number") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = palette.accent)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        isError = newUserPhoneError != null,
                        supportingText = {
                            newUserPhoneError?.let {
                                Text(it, color = palette.danger, fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.tier3Field,
                            unfocusedContainerColor = palette.tier3Field,
                            focusedBorderColor = if (newUserPhoneError != null) palette.danger else palette.accent,
                            unfocusedBorderColor = if (newUserPhoneError != null) palette.danger else palette.outline,
                            focusedTextColor = palette.primaryText,
                            unfocusedTextColor = palette.primaryText,
                            focusedLabelColor = palette.accent,
                            unfocusedLabelColor = palette.secondaryText,
                            focusedLeadingIconColor = palette.accent,
                            unfocusedLeadingIconColor = palette.accent
                        )
                    )

                    OutlinedTextField(
                        value = newUserName,
                        onValueChange = { newUserName = it },
                        label = { Text("Full Name") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = palette.accent)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.tier3Field,
                            unfocusedContainerColor = palette.tier3Field,
                            focusedBorderColor = palette.accent,
                            unfocusedBorderColor = palette.outline,
                            focusedTextColor = palette.primaryText,
                            unfocusedTextColor = palette.primaryText,
                            focusedLabelColor = palette.accent,
                            unfocusedLabelColor = palette.secondaryText,
                            focusedLeadingIconColor = palette.accent,
                            unfocusedLeadingIconColor = palette.accent
                        )
                    )
                }
            },
            confirmButton = {
                val canCreate = newUserPhone.length == 10 && newUserName.isNotBlank() && newUserPhoneError == null
                FilledTonalButton(
                    onClick = {
                        val role = if (pickerIsApprover) UserRole.APPROVER else UserRole.USER
                        viewModel.createUser(newUserPhone, newUserName.trim(), role, pickerDepartment)
                    },
                    enabled = canCreate,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = palette.approverContainer,
                        contentColor = palette.accent
                    )
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateUserDialog = false
                    newUserPhone = ""
                    newUserName = ""
                    newUserPhoneError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Duplicate User Alert ──────────────────────────────────────────────────
    if (showDuplicateAlert && pendingUserData != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateAlert() },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = palette.danger)
            },
            title = {
                Text("User Already Exists", fontWeight = FontWeight.Bold, color = palette.primaryText)
            },
            text = {
                Column {
                    Text(
                        "A user with phone ${pendingUserData!!.phoneNumber} already exists in your organization.",
                        color = palette.primaryText,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Do you want to update their name, role, and department?",
                        color = palette.secondaryText,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = { viewModel.createUserWithOverwrite() },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = palette.approverContainer,
                        contentColor = palette.accent
                    )
                ) {
                    Text("Update", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDuplicateAlert() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── Department Card ─────────────────────────────────────────────────────────

@Composable
private fun DepartmentCard(
    palette: BusinessHeadUiPalette,
    departmentName: String,
    users: List<User>,
    approvers: List<User>,
    onAssignUser: () -> Unit,
    onReplaceUser: (User) -> Unit,
    onAssignApprover: () -> Unit,
    onReplaceApprover: (User) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = palette.tier2Surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Department header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.approverContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Apartment,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = departmentName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = palette.outline)
            Spacer(Modifier.height(12.dp))

            // ── User Section ─────────────────────────────────────────────
            AssignmentSection(
                label = "User",
                chipColor = palette.userContainer,
                chipTextColor = palette.userText,
                assignedUsers = users,
                onAssign = onAssignUser,
                onReplace = onReplaceUser,
                palette = palette
            )

            Spacer(Modifier.height(14.dp))

            // ── Approver Section ─────────────────────────────────────────
            AssignmentSection(
                label = "Approver",
                chipColor = palette.approverContainer,
                chipTextColor = palette.approverText,
                assignedUsers = approvers,
                onAssign = onAssignApprover,
                onReplace = onReplaceApprover,
                palette = palette
            )
        }
    }
}

@Composable
private fun AssignmentSection(
    label: String,
    chipColor: Color,
    chipTextColor: Color,
    assignedUsers: List<User>,
    onAssign: () -> Unit,
    onReplace: (User) -> Unit,
    palette: BusinessHeadUiPalette
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header row: Role chip + Assign button (when empty)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Role chip
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = chipTextColor
                    )
                },
                colors = AssistChipDefaults.assistChipColors(containerColor = chipColor),
                border = null,
                modifier = Modifier.height(28.dp)
            )

            Spacer(Modifier.width(12.dp))

            if (assignedUsers.isEmpty()) {
                Text(
                    text = "Not assigned",
                    fontSize = 13.sp,
                    color = palette.secondaryText,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onAssign,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(palette.accent)
                    )
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = palette.accent
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Assign", fontSize = 12.sp, color = palette.accent, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text(
                    text = "${assignedUsers.size} assigned",
                    fontSize = 13.sp,
                    color = palette.secondaryText,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onAssign,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(palette.accent)
                    )
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = palette.accent
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Add", fontSize = 12.sp, color = palette.accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Per-user rows with individual Replace buttons
        if (assignedUsers.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            assignedUsers.forEach { user ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, bottom = 6.dp)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(chipColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(1).uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = chipTextColor
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = user.phone,
                            fontSize = 11.sp,
                            color = palette.secondaryText
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // Per-user Replace button
                    FilledTonalButton(
                        onClick = { onReplace(user) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = palette.approverContainer,
                            contentColor = palette.accent
                        )
                    ) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Replace", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Candidate User Row in Picker ────────────────────────────────────────────

@Composable
private fun CandidateUserRow(
    palette: BusinessHeadUiPalette,
    user: User,
    isApprover: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isApprover) palette.approverContainer else palette.userContainer
    val textColor = if (isApprover) palette.approverText else palette.userText

    ListItem(
        headlineContent = {
            Text(
                text = user.name,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.phone,
                    fontSize = 13.sp,
                    color = palette.secondaryText
                )
                if (user.department.isNotBlank()) {
                    Text(
                        text = " · ${user.department}",
                        fontSize = 12.sp,
                        color = palette.secondaryText
                    )
                }
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(1).uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        },
        trailingContent = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = palette.secondaryText,
                modifier = Modifier.size(20.dp)
            )
        },
        modifier = Modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(12.dp)),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
