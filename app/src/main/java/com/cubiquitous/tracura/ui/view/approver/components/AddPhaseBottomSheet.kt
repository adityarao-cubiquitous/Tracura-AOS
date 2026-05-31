package com.cubiquitous.tracura.ui.view.approver.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubiquitous.tracura.model.ContractorMode
import com.cubiquitous.tracura.model.DepartmentDraft
import com.cubiquitous.tracura.model.LineItem
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.ui.common.AddDepartmentModalSheet
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.viewmodel.ApproverProjectViewModel
import com.cubiquitous.tracura.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPhaseBottomSheet(
    projectId: String,
    onDismiss: () -> Unit,
    onPhaseAdded: () -> Unit,
    approverProjectViewModel: ApproverProjectViewModel
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var phaseName by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()
    
    // Departments
    var departments by remember { mutableStateOf<List<DepartmentDraft>>(emptyList()) }
    
    // Department assignments: Map of department name to (approverPhone, userPhone)
    var departmentAssignments by remember { mutableStateOf<Map<String, Pair<String?, String?>>>(emptyMap()) }

    // Contractor amounts per department (Labour-Only departments only)
    var departmentContractorAmounts by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    
    // Department creation dialog state
    var showAddDeptDialog by remember { mutableStateOf(false) }
    var newDeptName by remember { mutableStateOf("") }
    var newDeptContractorMode by remember { mutableStateOf(ContractorMode.LABOUR_ONLY) }
    var newDeptLineItems by remember { mutableStateOf(mutableListOf<LineItem>()) }
    
    // Available users and approvers for department assignment
    var availableUsers by remember { mutableStateOf<List<com.cubiquitous.tracura.model.User>>(emptyList()) }
    var availableApprovers by remember { mutableStateOf<List<com.cubiquitous.tracura.model.User>>(emptyList()) }
    
    // Load users and approvers when dialog opens
    LaunchedEffect(showAddDeptDialog) {
        if (showAddDeptDialog) {
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                if (currentCustomerId != null) {
                    val authRepository = AuthRepository(FirebaseFirestore.getInstance())
                    
                    // Fetch CustomerUserRelations for this customer
                    val relationsResult = authRepository.fetchCustomerUserRelations(currentCustomerId)
                    
                    if (relationsResult.isSuccess) {
                        val relations = relationsResult.getOrNull() ?: emptyList()
                        
                        // Convert relations to User objects
                        val allUsers = relations.map { relation ->
                            authRepository.convertRelationToUser(relation)
                        }
                        
                        // Filter by role and active status
                        val regularUsers = allUsers.filter { 
                            it.role == UserRole.USER && it.isActive
                        }
                        val approvers = allUsers.filter { 
                            it.role == UserRole.APPROVER && it.isActive
                        }
                        
                        availableUsers = regularUsers
                        availableApprovers = approvers
                        
                        android.util.Log.d("PhaseAddingDebugging", "Loaded ${regularUsers.size} users and ${approvers.size} approvers from CustomerUserRelation")
                    } else {
                        android.util.Log.e("PhaseAddingDebugging", "Failed to fetch CustomerUserRelations: ${relationsResult.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PhaseAddingDebugging", "Error loading users: ${e.message}")
            }
        }
    }
    
    // Pass User lists directly - filtering by department will be done in AddDepartmentModalSheet
    
    val dateFormatter = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
    
    // Get existing phases to check for duplicates
    val existingPhases by approverProjectViewModel.allPhases.collectAsState()
    
    // Check for duplicate phase names (case-insensitive)
    val isDuplicatePhaseName = remember(phaseName, existingPhases) {
        val currentPhaseName = phaseName.trim()
        if (currentPhaseName.isEmpty()) {
            false
        } else {
            existingPhases.any { phase ->
                phase.phaseName.trim().lowercase() == currentPhaseName.lowercase()
            }
        }
    }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Header with Cancel, Title, and Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = colorScheme.primary)
                }
                Text(
                    text = "Add Phase",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                val isSaveEnabled = phaseName.isNotBlank() && 
                    !isDuplicatePhaseName &&
                    startDate != null && 
                    endDate != null && 
                    endDate!!.time >= startDate!!.time &&
                    departments.isNotEmpty()
                
                // Get current user UID as customerId
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val customerId = currentUser?.uid ?: ""
                
                TextButton(
                    onClick = {
                        if (isSaveEnabled && customerId.isNotBlank()) {
                            // Debug logging
                            android.util.Log.d("PhaseAddingDebugging", "=== SAVE BUTTON CLICKED ===")
                            android.util.Log.d("PhaseAddingDebugging", "Phase name: ${phaseName.trim()}")
                            android.util.Log.d("PhaseAddingDebugging", "Departments count: ${departments.size}")
                            android.util.Log.d("PhaseAddingDebugging", "Department assignments count: ${departmentAssignments.size}")
                            android.util.Log.d("PhaseAddingDebugging", "Department assignments: $departmentAssignments")
                            departmentAssignments.forEach { (dept, phones) ->
                                android.util.Log.d("PhaseAddingDebugging", "  - Dept: $dept, Approver: ${phones.first}, User: ${phones.second}")
                            }
                            
                            approverProjectViewModel.addPhase(
                                customerId = customerId,
                                projectId = projectId,
                                phaseName = phaseName.trim(),
                                startDate = dateFormatter.format(startDate!!),
                                endDate = dateFormatter.format(endDate!!),
                                departmentDrafts = departments,
                                departmentAssignments = departmentAssignments
                            ) { success, phaseId ->
                                if (success) {
                                    android.widget.Toast.makeText(context, "Phase added successfully", android.widget.Toast.LENGTH_SHORT).show()
                                    onPhaseAdded()
                                } else {
                                    android.widget.Toast.makeText(context, "Failed to add phase", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else if (customerId.isBlank()) {
                            android.widget.Toast.makeText(context, "User not authenticated", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = isSaveEnabled
                ) {
                    Text(
                        "Save",
                        color = if (isSaveEnabled) colorScheme.primary else colorScheme.outline
                    )
                }
            }

            Divider(color = colorScheme.outlineVariant, thickness = 1.dp)
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Phase Name
                OutlinedTextField(
                    value = phaseName,
                    onValueChange = { phaseName = it },
                    label = { Text("Phase Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    isError = isDuplicatePhaseName,
                    supportingText = if (isDuplicatePhaseName) {
                        { 
                            val duplicateName = phaseName.trim().ifEmpty { "This phase name" }
                            Text("$duplicateName already exists in this project. Enter a unique phase name.", color = colorScheme.error)
                        }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDuplicatePhaseName) colorScheme.error else colorScheme.primary,
                        unfocusedBorderColor = if (isDuplicatePhaseName) colorScheme.error else colorScheme.outline,
                        errorBorderColor = colorScheme.error,
                        focusedContainerColor = colorScheme.surface,
                        unfocusedContainerColor = colorScheme.surface
                    )
                )
                
                // Start Date
                OutlinedTextField(
                    value = startDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Start Date *") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            tint = colorScheme.primary,
                            modifier = Modifier.clickable { showStartDatePicker = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartDatePicker = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        focusedContainerColor = colorScheme.surface,
                        unfocusedContainerColor = colorScheme.surface
                    )
                )
                
                // End Date
                OutlinedTextField(
                    value = endDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("End Date *") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            tint = colorScheme.primary,
                            modifier = Modifier.clickable { showEndDatePicker = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEndDatePicker = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        focusedContainerColor = colorScheme.surface,
                        unfocusedContainerColor = colorScheme.surface
                    )
                )
                
                // Error if end date is before start date
                if (endDate != null && startDate != null && endDate!!.time < startDate!!.time) {
                    Text(
                        text = "End Date must be on or after Start Date",
                        color = colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Divider(color = colorScheme.outlineVariant, thickness = 1.dp)
                
                // Department Section
                Text(
                    text = "DEPARTMENTS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                
                Button(
                    onClick = { showAddDeptDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Department")
                }
                
                // Department Creation Modal Sheet
                val isDuplicateDepartmentName = remember(newDeptName, departments) {
                    val currentDeptName = newDeptName.trim()
                    if (currentDeptName.isEmpty()) {
                        false
                    } else {
                        departments.any { existingDept ->
                            existingDept.departmentName.trim().lowercase() == currentDeptName.lowercase()
                        }
                    }
                }
                
                if (showAddDeptDialog) {
                    AddDepartmentModalSheet(
                        phaseName = phaseName,
                        departmentName = newDeptName,
                        onDepartmentNameChange = { newDeptName = it },
                        contractorMode = newDeptContractorMode,
                        onContractorModeChange = { newDeptContractorMode = it },
                        lineItems = newDeptLineItems,
                        onLineItemsChange = { newDeptLineItems = it.toMutableList() },
                        afterProjectCreationNavigation = true,
                        approverOptions = availableApprovers,
                        userOptions = availableUsers,
                        isDuplicateDepartmentName = isDuplicateDepartmentName || newDeptName.trim().equals(phaseName.trim(), ignoreCase = true),
                        projectId = projectId,
                        onDismiss = {
                            showAddDeptDialog = false
                        },
                        onCreate = { approverPhone, userPhone, contractorAmount ->
                            val trimmedDeptName = newDeptName.trim()
                            
                            // Debug logging
                            android.util.Log.d("PhaseAddingDebugging", "=== onCreate CALLBACK ===")
                            android.util.Log.d("PhaseAddingDebugging", "Department name: $trimmedDeptName")
                            android.util.Log.d("PhaseAddingDebugging", "Approver phone: $approverPhone")
                            android.util.Log.d("PhaseAddingDebugging", "User phone: $userPhone")
                            android.util.Log.d("PhaseAddingDebugging", "Current departments in phase: ${departments.map { it.departmentName }}")
                            android.util.Log.d("PhaseAddingDebugging", "isDuplicateDepartmentName: $isDuplicateDepartmentName")
                            
                            // Prevent department name from being the same as phase name
                            if (trimmedDeptName.equals(phaseName.trim(), ignoreCase = true)) {
                                android.util.Log.d("PhaseAddingDebugging", "❌ REJECTED: Department name same as phase name")
                                android.widget.Toast.makeText(
                                    context,
                                    "Department name cannot be the same as phase name",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else if (departments.any { it.departmentName.equals(trimmedDeptName, ignoreCase = true) }) {
                                // Check if department already exists IN THIS PHASE
                                android.util.Log.d("PhaseAddingDebugging", "❌ REJECTED: Department already exists in this phase")
                                android.widget.Toast.makeText(
                                    context,
                                    "Department already exists",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else if (trimmedDeptName.isNotEmpty() && !isDuplicateDepartmentName) {
                                // Calculate budget based on contractor mode
                                val lineItemsTotal = newDeptLineItems.sumOf { it.total }
                                val amt = when (newDeptContractorMode) {
                                    ContractorMode.LABOUR_ONLY -> lineItemsTotal + contractorAmount
                                     ContractorMode.SELF_EXECUTION -> lineItemsTotal
                                     ContractorMode.TURNKEY_AMOUNT_ONLY -> contractorAmount
                                 }
                                val deptDraft = DepartmentDraft(
                                    departmentName = trimmedDeptName,
                                    contractorMode = newDeptContractorMode,
                                    lineItems = newDeptLineItems.toMutableList(),
                                    contractorAmount = contractorAmount
                                )
                                departments = departments + deptDraft
                                
                                // Store department assignments to be processed after phase creation
                                if (approverPhone != null || userPhone != null) {
                                    departmentAssignments = departmentAssignments + (trimmedDeptName to Pair(approverPhone, userPhone))
                                    android.util.Log.d("PhaseAddingDebugging", "✅ Stored assignment for $trimmedDeptName: approver=$approverPhone, user=$userPhone")
                                    android.util.Log.d("PhaseAddingDebugging", "Total assignments now: ${departmentAssignments.size}")
                                } else {
                                    android.util.Log.d("PhaseAddingDebugging", "⚠️ No assignment stored (both phones null)")
                                }
                                
                                newDeptName = ""
                                newDeptContractorMode = ContractorMode.LABOUR_ONLY
                                newDeptLineItems = mutableListOf()
                                showAddDeptDialog = false
                            }
                        }
                    )
                }
                
                // Department list
                if (departments.isNotEmpty()) {
                    departments.forEachIndexed { index, dept ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dept.departmentName,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = FormatUtils.formatCurrencyWithoutDecimals(dept.totalBudget),
                                        color = colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                                IconButton(onClick = {
                                    val deptName = dept.departmentName
                                    departments = departments.filterIndexed { idx, _ -> idx != index }
                                    // Also remove the assignment for this department
                                    departmentAssignments = departmentAssignments - deptName
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    
                    // Total Budget Display
                    val totalBudget = departments.sumOf { it.totalBudget }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Phase Budget",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = FormatUtils.formatCurrencyWithoutDecimals(totalBudget),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // Start Date Picker
    if (showStartDatePicker) {
        DatePickerDialog(
            onDateSelected = { selectedDate ->
                startDate = selectedDate
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false },
            datePickerState = startDatePickerState,
            title = "Select Start Date"
        )
    }
    
    // End Date Picker
    if (showEndDatePicker) {
        DatePickerDialog(
            onDateSelected = { selectedDate ->
                if (startDate == null || selectedDate.time >= startDate!!.time) {
                    endDate = selectedDate
                } else {
                    android.widget.Toast.makeText(context, "End Date must be on or after Start Date", android.widget.Toast.LENGTH_SHORT).show()
                }
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false },
            datePickerState = endDatePickerState,
            title = "Select End Date"
        )
    }
}

// Date Picker Dialog Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    datePickerState: DatePickerState,
    title: String
) {
    DatePickerDialog(
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
