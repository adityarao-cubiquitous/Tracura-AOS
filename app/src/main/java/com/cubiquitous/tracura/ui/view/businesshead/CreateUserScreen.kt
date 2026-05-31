package com.cubiquitous.tracura.ui.view.businesshead

import android.Manifest
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.utils.ContactHelper
import com.cubiquitous.tracura.viewmodel.BusinessHeadViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CreateUserScreen(
    onNavigateBack: () -> Unit,
    onUserCreated: () -> Unit,
    viewModel: BusinessHeadViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val palette = businessHeadUiPalette()
    
    var phoneNumber by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.USER) }
    var showRoleDropdown by remember { mutableStateOf(false) }
    var phoneNumberError by remember { mutableStateOf<String?>(null) }
    var isCheckingContacts by remember { mutableStateOf(false) }
    var contactName by remember { mutableStateOf<String?>(null) }
    var contactInfoText by remember { mutableStateOf<String?>(null) }
    var selectedDepartment by remember { mutableStateOf("") }
    var showDepartmentDropdown by remember { mutableStateOf(false) }
    var departmentSearchQuery by remember { mutableStateOf("") }
    var showCreateDepartmentDialog by remember { mutableStateOf(false) }
    var showEditDepartmentDialog by remember { mutableStateOf(false) }
    var departmentToEdit by remember { mutableStateOf("") }
    var newDepartmentName by remember { mutableStateOf("") }
    
    // Focus requester for department search field
    val departmentSearchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val customerDepartments by viewModel.customerDepartments.collectAsState()
    val showDuplicateAlert by viewModel.showDuplicateAlert.collectAsState()
    val pendingUserData by viewModel.pendingUserData.collectAsState()
    
    // Permission state for contacts
    val contactsPermissionState = rememberPermissionState(Manifest.permission.READ_CONTACTS)
    
    // Track if we should open picker after permission is granted
    var shouldOpenPickerAfterPermission by remember { mutableStateOf(false) }
    
    // Contact picker launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let { contactUri ->
            val cursor = context.contentResolver.query(
                contactUri,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME
                ),
                null,
                null,
                null
            )
            
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
                    
                    if (nameIndex >= 0) {
                        val name = c.getString(nameIndex) ?: ""
                        fullName = name
                    }
                    
                    // Get phone number from the contact
                    if (idIndex >= 0) {
                        val contactId = c.getString(idIndex)
                        val phoneCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )
                        
                        phoneCursor?.use { pc ->
                            if (pc.moveToFirst()) {
                                val phoneIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (phoneIndex >= 0) {
                                    val phone = pc.getString(phoneIndex) ?: ""
                                    // Extract only digits from phone number
                                    val digitsOnly = phone.filter { it.isDigit() }
                                    // Take last 10 digits if longer
                                    phoneNumber = if (digitsOnly.length > 10) {
                                        digitsOnly.takeLast(10)
                                    } else {
                                        digitsOnly
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Function to open contact picker
    fun openContactPicker() {
        // Request permission first if not granted
        if (contactsPermissionState.status !is PermissionStatus.Granted) {
            shouldOpenPickerAfterPermission = true
            contactsPermissionState.launchPermissionRequest()
        } else {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            contactPickerLauncher.launch(intent)
        }
    }
    
    // Launch contact picker after permission is granted (if user clicked while permission was denied)
    LaunchedEffect(contactsPermissionState.status) {
        if (contactsPermissionState.status is PermissionStatus.Granted && shouldOpenPickerAfterPermission) {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            contactPickerLauncher.launch(intent)
            shouldOpenPickerAfterPermission = false
        }
    }
    
    // Handle success message
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            onUserCreated()
            viewModel.clearSuccessMessage()
        }
    }
    
    // Handle error
    LaunchedEffect(error) {
        if (error != null) {
            // You can show a snackbar here if needed
        }
    }

    // Show inline error for duplicate phone number
    LaunchedEffect(showDuplicateAlert, pendingUserData) {
        if (showDuplicateAlert && pendingUserData != null) {
            phoneNumberError = "Number already added"
            contactInfoText = null
        }
    }
    
    // Load customer departments on screen initialization
    LaunchedEffect(Unit) {
        viewModel.loadCustomerDepartments()
    }
    
    // Filter departments based on search query
    val filteredDepartments = remember(customerDepartments, departmentSearchQuery) {
        if (departmentSearchQuery.isBlank()) {
            customerDepartments
        } else {
            customerDepartments.filter { 
                it.contains(departmentSearchQuery, ignoreCase = true) 
            }
        }
    }
    
    // Validation functions
    fun validatePhoneNumber(phone: String): String? {
        // Remove non-digits for validation
        val digitsOnly = phone.filter { it.isDigit() }
        
        // Check if empty
        if (digitsOnly.isEmpty()) {
            return null
        }
        
        // Check if more than 10 digits
        if (digitsOnly.length > 10) {
            return "Phone number must be exactly 10 digits"
        }
        
        // Check if all digits are the same
        if (digitsOnly.length >= 2 && digitsOnly.all { it == digitsOnly[0] }) {
            return "Phone number cannot have all same digits"
        }
        
        // Check if exactly 10 digits
        if (digitsOnly.length < 10) {
            return "Phone number must be 10 digits"
        }
        
        return null
    }
    
    // Check contacts when phone number changes
    LaunchedEffect(phoneNumber, contactsPermissionState.status) {
        // Clear previous error and contact info
        phoneNumberError = null
        contactName = null
        contactInfoText = null
        
        // Validate phone number format
        val validationError = validatePhoneNumber(phoneNumber)
        if (validationError != null) {
            phoneNumberError = validationError
            return@LaunchedEffect
        }
        
        // Only check contacts if we have 10 digits
        val digitsOnly = phoneNumber.filter { it.isDigit() }
        if (digitsOnly.length == 10) {
            // Request permission if not granted
            if (contactsPermissionState.status !is PermissionStatus.Granted) {
                contactsPermissionState.launchPermissionRequest()
                delay(300) // Wait a bit for permission dialog
            }
            
            // Check contacts if permission is granted
            if (contactsPermissionState.status is PermissionStatus.Granted) {
                isCheckingContacts = true
                val exists = ContactHelper.isPhoneNumberInContacts(context, digitsOnly)
                if (exists) {
                    val name = ContactHelper.getContactName(context, digitsOnly)
                    contactName = name
                    // Show contact name as informational text (not error)
                    contactInfoText = if (name != null) "Found in contacts: $name" else "Found in contacts"
                } else {
                    contactName = null
                    contactInfoText = null
                }
                isCheckingContacts = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create User",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.accent
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val digitsOnly = phoneNumber.filter { it.isDigit() }
                            val validationError = validatePhoneNumber(phoneNumber)
                            if (validationError == null && digitsOnly.length == 10 && fullName.isNotEmpty() && phoneNumberError == null) {
                                viewModel.createUser(digitsOnly, fullName, selectedRole, selectedDepartment)
                            }
                        },
                        enabled = !isLoading && 
                                phoneNumber.filter { it.isDigit() }.length == 10 && 
                                fullName.isNotEmpty() && 
                                phoneNumberError == null
                    ) {
                        Text(
                            text = "Create",
                            color = if (phoneNumber.filter { it.isDigit() }.length == 10 && 
                                    fullName.isNotEmpty() && 
                                    phoneNumberError == null) {
                                palette.accent
                            } else {
                                palette.secondaryText
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.tier2Surface,
                    titleContentColor = palette.primaryText,
                    actionIconContentColor = palette.accent,
                    navigationIconContentColor = palette.accent
                )
            )
        },
        containerColor = palette.tier1Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Section Header
            Text(
                text = "USER INFORMATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = palette.secondaryText,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Phone Number Field
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { newValue ->
                    // Only allow digits and limit to 10 digits
                    val digitsOnly = newValue.filter { it.isDigit() }
                    if (digitsOnly.length <= 10) {
                        phoneNumber = digitsOnly
                    }
                },
                label = { Text("Phone Number") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Phone",
                        tint = palette.accent
                    )
                },
                trailingIcon = {
                    // Contact picker icon
                    IconButton(
                        onClick = { openContactPicker() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = "Pick from contacts",
                            tint = palette.accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (phoneNumberError != null || contactInfoText != null) 4.dp else 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.tier3Field,
                    unfocusedContainerColor = palette.tier3Field,
                    focusedBorderColor = if (phoneNumberError != null) palette.danger else palette.accent,
                    unfocusedBorderColor = if (phoneNumberError != null) palette.danger else palette.outline,
                    focusedTextColor = palette.primaryText,
                    unfocusedTextColor = palette.primaryText,
                    focusedLabelColor = palette.accent,
                    unfocusedLabelColor = palette.secondaryText,
                    focusedLeadingIconColor = palette.accent,
                    unfocusedLeadingIconColor = palette.accent,
                    errorBorderColor = palette.danger,
                    errorLabelColor = palette.danger,
                    errorSupportingTextColor = palette.danger
                ),
                singleLine = true,
                isError = phoneNumberError != null,
                supportingText = {
                    if (phoneNumberError != null) {
                        Text(
                            text = phoneNumberError!!,
                            color = palette.danger,
                            fontSize = 12.sp
                        )
                    } else if (contactInfoText != null) {
                        Text(
                            text = contactInfoText!!,
                            color = palette.accent,
                            fontSize = 12.sp
                        )
                    }
                }
            )
            
            // Full Name Field
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Person",
                        tint = palette.accent
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
                ),
                singleLine = true
            )
            
            // Role Selection
            Text(
                text = "Role",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = palette.secondaryText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            ExposedDropdownMenuBox(
                expanded = showRoleDropdown,
                onExpandedChange = { showRoleDropdown = !showRoleDropdown }
            ) {
                OutlinedTextField(
                    value = when (selectedRole) {
                        UserRole.USER -> "User"
                        UserRole.APPROVER -> "Approver"
                        UserRole.ADMIN -> "Admin"
                        UserRole.BUSINESS_HEAD -> "Production Head"
                        UserRole.MANAGER -> "Manager"
                    },
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Select Role") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Role",
                            tint = palette.accent
                        )
                    },
                    trailingIcon = {
                        Icon(
                            if (showRoleDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = palette.accent
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
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
                
                ExposedDropdownMenu(
                    expanded = showRoleDropdown,
                    onDismissRequest = { showRoleDropdown = false },
                    modifier = Modifier.background(palette.tier2Surface)
                ) {
                    RoleOption(
                        role = UserRole.USER,
                        title = "User",
                        description = "Can submit expenses and view project details",
                        isSelected = selectedRole == UserRole.USER,
                        onClick = {
                            selectedRole = UserRole.USER
                            showRoleDropdown = false
                        }
                    )
                    
                    RoleOption(
                        role = UserRole.APPROVER,
                        title = "Approver",
                        description = "Can approve/reject expenses and manage budgets",
                        isSelected = selectedRole == UserRole.APPROVER,
                        onClick = {
                            selectedRole = UserRole.APPROVER
                            showRoleDropdown = false
                        }
                    )

                    RoleOption(
                        role = UserRole.MANAGER,
                        title = "Account Manager",
                        description = "Can manage assigned projects, approve expenses, and has full project dashboard access. No department restriction.",
                        isSelected = selectedRole == UserRole.MANAGER,
                        onClick = {
                            selectedRole = UserRole.MANAGER
                            selectedDepartment = ""
                            showRoleDropdown = false
                        }
                    )
                }
            }
            
            if (selectedRole != UserRole.MANAGER) {
            Spacer(modifier = Modifier.height(16.dp))

            // Department Selection
            Text(
                text = "Department (Optional)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = palette.secondaryText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Request focus when dropdown opens
            LaunchedEffect(showDepartmentDropdown) {
                if (showDepartmentDropdown) {
                    delay(150) // Small delay to ensure dropdown is rendered
                    departmentSearchFocusRequester.requestFocus()
                    keyboardController?.show()
                } else {
                    keyboardController?.hide()
                }
            }
            
            ExposedDropdownMenuBox(
                expanded = showDepartmentDropdown,
                onExpandedChange = { 
                    showDepartmentDropdown = !showDepartmentDropdown
                    if (!showDepartmentDropdown) {
                        departmentSearchQuery = ""
                    }
                }
            ) {
                OutlinedTextField(
                    value = selectedDepartment,
                    onValueChange = { }, // No free text entry allowed
                    readOnly = true, // Make field read-only
                    label = { Text("Select Department") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Business,
                            contentDescription = "Department",
                            tint = palette.accent
                        )
                    },
                    trailingIcon = {
                        Row {
                            if (selectedDepartment.isNotEmpty() && customerDepartments.contains(selectedDepartment)) {
                                IconButton(
                                    onClick = {
                                        departmentToEdit = selectedDepartment
                                        newDepartmentName = selectedDepartment
                                        showEditDepartmentDialog = true
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Department",
                                        tint = palette.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    newDepartmentName = ""
                                    showCreateDepartmentDialog = true
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add Department",
                                    tint = palette.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Icon(
                                if (showDepartmentDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = palette.accent
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
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
                
                ExposedDropdownMenu(
                    expanded = showDepartmentDropdown,
                    onDismissRequest = { 
                        showDepartmentDropdown = false
                        departmentSearchQuery = ""
                    },
                    modifier = Modifier.background(palette.tier2Surface)
                ) {
                    // Search field inside dropdown
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = departmentSearchQuery,
                            onValueChange = { departmentSearchQuery = it },
                            label = { Text("Search Department") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = palette.accent
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(departmentSearchFocusRequester),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
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
                    
                    HorizontalDivider(color = palette.outline)
                    
                    // Show create option if search query doesn't match any department
                    if (filteredDepartments.isEmpty() && departmentSearchQuery.isNotBlank()) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Create \"${departmentSearchQuery}\"",
                                        fontSize = 14.sp,
                                        color = palette.accent
                                    )
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Create",
                                        tint = palette.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            onClick = {
                                viewModel.addDepartmentToCustomer(departmentSearchQuery)
                                selectedDepartment = departmentSearchQuery
                                showDepartmentDropdown = false
                                departmentSearchQuery = ""
                            }
                        )
                    } else {
                        // Show all departments if search is empty, otherwise show filtered
                        val departmentsToShow = if (departmentSearchQuery.isBlank()) {
                            customerDepartments
                        } else {
                            filteredDepartments
                        }
                        
                        if (departmentsToShow.isEmpty() && departmentSearchQuery.isBlank()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "No departments available. Click + to create one.",
                                        fontSize = 14.sp,
                                        color = palette.secondaryText
                                    )
                                },
                                onClick = { }
                            )
                        } else {
                            departmentsToShow.forEach { department ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = department,
                                                fontSize = 14.sp,
                                                color = palette.primaryText,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    departmentToEdit = department
                                                    newDepartmentName = department
                                                    showEditDepartmentDialog = true
                                                    showDepartmentDropdown = false
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit",
                                                    tint = palette.accent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedDepartment = department
                                        showDepartmentDropdown = false
                                        departmentSearchQuery = ""
                }
            )
        }
            } // end if (selectedRole != UserRole.MANAGER)

        // Duplicate User Alert Dialog
        if (showDuplicateAlert && pendingUserData != null) {
            AlertDialog(
                onDismissRequest = { 
                    viewModel.dismissDuplicateAlert()
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "User Already Exists",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "A user with phone number ${pendingUserData!!.phoneNumber} already exists for this customer.",
                            fontSize = 14.sp,
                            color = palette.primaryText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Do you want to update the existing user's information?",
                            fontSize = 14.sp,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This will update: name, role, department, and active status.",
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = palette.secondaryText
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.createUserWithOverwrite()
                        }
                    ) {
                        Text(
                            text = "Update",
                            color = palette.accent
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.dismissDuplicateAlert()
                        }
                    ) {
                        Text("Cancel", color = palette.secondaryText)
                    }
                },
                containerColor = palette.tier2Surface,
                titleContentColor = palette.primaryText,
                textContentColor = palette.primaryText
            )
        }
    }
}
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Role Permissions Section
            Text(
                text = "ROLE PERMISSIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = palette.secondaryText,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Role Permissions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = palette.tier2Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Blue circular icon with white person silhouette
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                palette.accent,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Role Icon",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Role information
                    Column {
                        Text(
                            text = when (selectedRole) {
                                UserRole.USER -> "User"
                                UserRole.APPROVER -> "Approver"
                                UserRole.ADMIN -> "Admin"
                                UserRole.BUSINESS_HEAD -> "Production Head"
                                UserRole.MANAGER -> "Manager"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Text(
                            text = when (selectedRole) {
                                UserRole.USER -> "Can submit expenses and view project details"
                                UserRole.APPROVER -> "Can approve/reject expenses and manage budgets"
                                UserRole.ADMIN -> "Full system access and user management"
                                UserRole.BUSINESS_HEAD -> "Can manage projects and oversee operations"
                                UserRole.MANAGER -> "Can manage assigned projects, approve expenses, and has full project dashboard access"
                            },
                            fontSize = 14.sp,
                            color = palette.secondaryText,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            // Error Message
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.adminContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = palette.danger,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            color = palette.danger,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // Loading Indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = palette.accent,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        // Create Department Dialog
        if (showCreateDepartmentDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showCreateDepartmentDialog = false
                    newDepartmentName = ""
                },
                title = {
                    Text(
                        text = "Create New Department",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    OutlinedTextField(
                        value = newDepartmentName,
                        onValueChange = { newDepartmentName = it },
                        label = { Text("Department Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.tier3Field,
                            unfocusedContainerColor = palette.tier3Field,
                            focusedBorderColor = palette.accent,
                            unfocusedBorderColor = palette.outline,
                            focusedTextColor = palette.primaryText,
                            unfocusedTextColor = palette.primaryText,
                            focusedLabelColor = palette.accent,
                            unfocusedLabelColor = palette.secondaryText
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newDepartmentName.isNotBlank()) {
                                viewModel.addDepartmentToCustomer(newDepartmentName.trim())
                                selectedDepartment = newDepartmentName.trim()
                                showCreateDepartmentDialog = false
                                newDepartmentName = ""
                            }
                        },
                        enabled = newDepartmentName.isNotBlank()
                    ) {
                        Text(
                            text = "Create",
                            color = if (newDepartmentName.isNotBlank()) palette.accent else palette.secondaryText
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showCreateDepartmentDialog = false
                            newDepartmentName = ""
                        }
                    ) {
                        Text("Cancel", color = palette.secondaryText)
                    }
                }
            )
        }
        
        // Edit Department Dialog
        if (showEditDepartmentDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showEditDepartmentDialog = false
                    newDepartmentName = ""
                    departmentToEdit = ""
                },
                title = {
                    Text(
                        text = "Edit Department",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    OutlinedTextField(
                        value = newDepartmentName,
                        onValueChange = { newDepartmentName = it },
                        label = { Text("Department Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.tier3Field,
                            unfocusedContainerColor = palette.tier3Field,
                            focusedBorderColor = palette.accent,
                            unfocusedBorderColor = palette.outline,
                            focusedTextColor = palette.primaryText,
                            unfocusedTextColor = palette.primaryText,
                            focusedLabelColor = palette.accent,
                            unfocusedLabelColor = palette.secondaryText
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newDepartmentName.isNotBlank() && newDepartmentName.trim() != departmentToEdit) {
                                viewModel.updateDepartmentInCustomer(departmentToEdit, newDepartmentName.trim())
                                if (selectedDepartment == departmentToEdit) {
                                    selectedDepartment = newDepartmentName.trim()
                                }
                                showEditDepartmentDialog = false
                                newDepartmentName = ""
                                departmentToEdit = ""
                            }
                        },
                        enabled = newDepartmentName.isNotBlank() && newDepartmentName.trim() != departmentToEdit
                    ) {
                        Text(
                            text = "Update",
                            color = if (newDepartmentName.isNotBlank() && newDepartmentName.trim() != departmentToEdit) palette.accent else palette.secondaryText
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showEditDepartmentDialog = false
                            newDepartmentName = ""
                            departmentToEdit = ""
                        }
                    ) {
                        Text("Cancel", color = palette.secondaryText)
                    }
                }
            )
        }
    }
}

@Composable
fun RoleOption(
    role: UserRole,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = businessHeadUiPalette()
    DropdownMenuItem(
        text = {
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.primaryText
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = palette.secondaryText,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        },
        onClick = onClick,
        leadingIcon = {
            Icon(
                Icons.Default.Person,
                contentDescription = title,
                tint = if (isSelected) palette.accent else palette.secondaryText
            )
        },
        modifier = Modifier.background(
            if (isSelected) palette.approverContainer.copy(alpha = 0.4f) else Color.Transparent
        )
    )
}
