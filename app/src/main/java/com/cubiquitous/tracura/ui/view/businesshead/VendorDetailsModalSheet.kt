package com.cubiquitous.tracura.ui.view.businesshead

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.viewmodel.BusinessHeadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorDetailsModalSheet(
    onDismiss: () -> Unit,
    viewModel: BusinessHeadViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val vendors by viewModel.vendors.collectAsState()
    val departmentUserCounts by viewModel.departmentUserCounts.collectAsState()
    val departmentApproverCounts by viewModel.departmentApproverCounts.collectAsState()
    
    var showCreateVendorDialog by remember { mutableStateOf(false) }
    var showEditVendorDialog by remember { mutableStateOf(false) }
    var showCreateDepartmentDialog by remember { mutableStateOf(false) }
    var editingVendor by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    // Material 3 color scheme
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    
    // Load vendors and departments on launch
    LaunchedEffect(Unit) {
        viewModel.loadVendors()
        viewModel.loadCustomerDepartments()
        sheetState.expand()
    }
    
    // Get all unique department names from both maps
    val allDepartments = remember(departmentUserCounts, departmentApproverCounts) {
        (departmentUserCounts.keys + departmentApproverCounts.keys).distinct().sorted()
    }
    
    // Group vendors by department
    val vendorsByDepartment = remember(vendors) {
        vendors.entries.groupBy { it.value }
            .mapValues { (_, entries) -> entries.map { it.key } }
            .toSortedMap()
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfaceColor,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
        ) {
            // Modern Header with gradient accent
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = surfaceColor,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Title with icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = primaryContainer,
                                tonalElevation = 0.dp
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = null,
                                        tint = onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Vendor Management",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = onSurfaceColor
                                )
                                Text(
                                    text = "${vendors.size} vendor${if (vendors.size != 1) "s" else ""}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                        // Add button with FAB style
                        FloatingActionButton(
                            onClick = { showCreateVendorDialog = true },
                            modifier = Modifier.size(56.dp),
                            containerColor = primaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 3.dp,
                                pressedElevation = 6.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Vendor",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            Divider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )
            
            // Vendor List with Material 3 design
            if (vendors.isEmpty()) {
                EmptyVendorsState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    onAddClick = { showCreateVendorDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    vendorsByDepartment.forEach { (department, vendorList) ->
                        item {
                            // Department Header with chip style
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = surfaceVariant.copy(alpha = 0.5f),
                                tonalElevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = department,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = onSurfaceColor
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Surface(
                                        shape = CircleShape,
                                        color = primaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = "${vendorList.size}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        items(vendorList) { vendorName ->
                            VendorItemCard(
                                vendorName = vendorName,
                                departmentName = department,
                                onEdit = {
                                    editingVendor = Pair(vendorName, department)
                                    showEditVendorDialog = true
                                },
                                onDelete = {
                                    viewModel.deleteVendor(vendorName)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Create Vendor Dialog
    if (showCreateVendorDialog) {
        CreateVendorDialog(
            departments = allDepartments,
            vendors = vendors,
            onDismiss = { showCreateVendorDialog = false },
            onCreateVendor = { vendorName, departmentName ->
                viewModel.addVendor(vendorName, departmentName)
                showCreateVendorDialog = false
            },
            onCreateDepartment = {
                showCreateVendorDialog = false
                showCreateDepartmentDialog = true
            }
        )
    }
    
    // Edit Vendor Dialog
    editingVendor?.let { (oldVendorName, currentDepartment) ->
        if (showEditVendorDialog) {
            EditVendorDialog(
                oldVendorName = oldVendorName,
                currentDepartment = currentDepartment,
                departments = allDepartments,
                vendors = vendors,
                onDismiss = { 
                    showEditVendorDialog = false
                    editingVendor = null
                },
                onUpdateVendor = { newVendorName, newDepartmentName ->
                    viewModel.updateVendor(oldVendorName, newVendorName, newDepartmentName)
                    showEditVendorDialog = false
                    editingVendor = null
                },
                onCreateDepartment = {
                    showEditVendorDialog = false
                    showCreateDepartmentDialog = true
                }
            )
        }
    }
    
    // Create Department Dialog
    if (showCreateDepartmentDialog) {
        val returnToEdit = editingVendor != null
        CreateDepartmentDialog(
            onDismiss = { 
                showCreateDepartmentDialog = false
                if (returnToEdit) {
                    showEditVendorDialog = true
                } else {
                    showCreateVendorDialog = true
                }
            },
            onCreateDepartment = { departmentName ->
                viewModel.addNewDepartmentToCustomer(departmentName)
                showCreateDepartmentDialog = false
                if (returnToEdit) {
                    showEditVendorDialog = true
                } else {
                    showCreateVendorDialog = true
                }
            }
        )
    }
}

@Composable
private fun EmptyVendorsState(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Large icon with gradient background
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                tonalElevation = 0.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "No vendors yet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Get started by adding your first vendor",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Filled button with Material 3 style
            Button(
                onClick = onAddClick,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Vendor",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun VendorItemCard(
    vendorName: String,
    departmentName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showActions = !showActions },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar-like icon
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 0.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = vendorName.take(1).uppercase(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = vendorName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = departmentName,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            // Animated action buttons
            AnimatedVisibility(
                visible = showActions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showActions = false
                            onEdit()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit", fontSize = 14.sp)
                    }
                    
                    OutlinedButton(
                        onClick = {
                            showActions = false
                            onDelete()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateVendorDialog(
    departments: List<String>,
    vendors: Map<String, String>,
    onDismiss: () -> Unit,
    onCreateVendor: (String, String) -> Unit,
    onCreateDepartment: () -> Unit
) {
    var selectedDepartment by remember { mutableStateOf("") }
    var vendorNameAndLocation by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var showConflictDialog by remember { mutableStateOf(false) }
    var conflictMessage by remember { mutableStateOf("") }
    
    // Compute suggestions based on selected department and text (as per vendormanagement.md)
    val suggestions = remember(vendors, selectedDepartment, vendorNameAndLocation) {
        if (selectedDepartment.isBlank() || vendorNameAndLocation.isBlank()) {
            emptyList()
        } else {
            vendors.entries
                .filter { it.value.equals(selectedDepartment, ignoreCase = true) }
                .filter { it.key.contains(vendorNameAndLocation, ignoreCase = true) }
                .map { it.key }
                .sortedBy { it.lowercase() }
                .take(5)
        }
    }
    
    // Detect duplicate in same department (case-insensitive as per vendormanagement.md)
    val duplicateInSameDepartment = remember(vendors, selectedDepartment, vendorNameAndLocation) {
        if (selectedDepartment.isBlank() || vendorNameAndLocation.isBlank()) {
            null
        } else {
            vendors.entries.firstOrNull {
                it.key.trim().equals(vendorNameAndLocation.trim(), ignoreCase = true) &&
                it.value.equals(selectedDepartment, ignoreCase = true)
            }?.key
        }
    }
    
    // Detect conflict in different department (case-insensitive)
    val conflictInDifferentDepartment = remember(vendors, selectedDepartment, vendorNameAndLocation) {
        if (selectedDepartment.isBlank() || vendorNameAndLocation.isBlank()) {
            null
        } else {
            vendors.entries.firstOrNull {
                it.key.trim().equals(vendorNameAndLocation.trim(), ignoreCase = true) &&
                !it.value.equals(selectedDepartment, ignoreCase = true)
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 0.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Add Vendor",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Department selection FIRST (as per vendormanagement.md)
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDepartment,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Department") },
                        placeholder = { Text("Select department first") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        departments.forEach { dept ->
                            DropdownMenuItem(
                                text = { Text(dept) },
                                onClick = {
                                    selectedDepartment = dept
                                    isDropdownExpanded = false
                                }
                            )
                        }
                        
                        HorizontalDivider()
                        
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Create New Department",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                isDropdownExpanded = false
                                onCreateDepartment()
                            }
                        )
                    }
                }
                
                // Vendor Name + Location (entered as single field, as per vendormanagement.md)
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = vendorNameAndLocation,
                        onValueChange = { vendorNameAndLocation = it },
                        label = { Text("Vendor Name + Location") },
                        placeholder = { Text("e.g., ABC Company - Mumbai") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedDepartment.isNotBlank(),
                        singleLine = true,
                        isError = duplicateInSameDepartment != null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            when {
                                // Show duplicate error for same department (as per vendormanagement.md)
                                duplicateInSameDepartment != null -> {
                                    Text(
                                        text = "$duplicateInSameDepartment already exists in $selectedDepartment.",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp
                                    )
                                }
                                selectedDepartment.isBlank() -> {
                                    Text(
                                        text = "Please select a department first",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    )
                    
                    // Vendor suggestions dropdown (as per vendormanagement.md section 6.1)
                    if (suggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column {
                                suggestions.forEach { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                vendorNameAndLocation = suggestion
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = suggestion,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (suggestion != suggestions.last()) {
                                        HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (vendorNameAndLocation.isNotBlank() && selectedDepartment.isNotBlank()) {
                        // Check for different department conflict (as per vendormanagement.md)
                        if (conflictInDifferentDepartment != null) {
                            conflictMessage = "${conflictInDifferentDepartment.key} already exists in ${conflictInDifferentDepartment.value}. Confirm override to move it to $selectedDepartment."
                            showConflictDialog = true
                        } else {
                            onCreateVendor(vendorNameAndLocation.trim(), selectedDepartment)
                        }
                    }
                },
                // Disable button if duplicate in same department (as per vendormanagement.md section 6.2)
                enabled = vendorNameAndLocation.isNotBlank() && 
                         selectedDepartment.isNotBlank() && 
                         duplicateInSameDepartment == null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Create", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
    
    // Conflict dialog for different department override (as per vendormanagement.md section 6.3)
    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Vendor Already Exists",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = conflictMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConflictDialog = false
                        onCreateVendor(vendorNameAndLocation.trim(), selectedDepartment)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Override Department", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConflictDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditVendorDialog(
    oldVendorName: String,
    currentDepartment: String,
    departments: List<String>,
    vendors: Map<String, String>,
    onDismiss: () -> Unit,
    onUpdateVendor: (String, String) -> Unit,
    onCreateDepartment: () -> Unit
) {
    var selectedDepartment by remember { mutableStateOf(currentDepartment) }
    var vendorNameAndLocation by remember { mutableStateOf(oldVendorName) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var showConflictDialog by remember { mutableStateOf(false) }
    var conflictMessage by remember { mutableStateOf("") }
    
    // Compute suggestions based on selected department and text, excluding the original vendor (as per vendormanagement.md section 6.1)
    val suggestions = remember(vendors, selectedDepartment, vendorNameAndLocation, oldVendorName) {
        if (selectedDepartment.isBlank() || vendorNameAndLocation.isBlank()) {
            emptyList()
        } else {
            vendors.entries
                .filter { it.value.equals(selectedDepartment, ignoreCase = true) }
                .filter { !it.key.equals(oldVendorName, ignoreCase = true) } // Exclude original
                .filter { it.key.contains(vendorNameAndLocation, ignoreCase = true) }
                .map { it.key }
                .sortedBy { it.lowercase() }
                .take(5)
        }
    }
    
    // Detect duplicate in same department, excluding original vendor (case-insensitive as per vendormanagement.md)
    val duplicateInSameDepartment = remember(vendors, selectedDepartment, vendorNameAndLocation, oldVendorName) {
        if (selectedDepartment.isBlank() || vendorNameAndLocation.isBlank()) {
            null
        } else {
            vendors.entries.firstOrNull {
                !it.key.equals(oldVendorName, ignoreCase = true) && // Exclude original
                it.key.trim().equals(vendorNameAndLocation.trim(), ignoreCase = true) &&
                it.value.equals(selectedDepartment, ignoreCase = true)
            }?.key
        }
    }
    
    // Detect conflict in different department, excluding original vendor (case-insensitive)
    val conflictInDifferentDepartment = remember(vendors, selectedDepartment, vendorNameAndLocation, oldVendorName) {
        if (selectedDepartment.isBlank() || vendorNameAndLocation.isBlank()) {
            null
        } else {
            vendors.entries.firstOrNull {
                !it.key.equals(oldVendorName, ignoreCase = true) && // Exclude original
                it.key.trim().equals(vendorNameAndLocation.trim(), ignoreCase = true) &&
                !it.value.equals(selectedDepartment, ignoreCase = true)
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 0.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Edit Vendor",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Department selection FIRST
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDepartment,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Department") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        departments.forEach { dept ->
                            DropdownMenuItem(
                                text = { Text(dept) },
                                onClick = {
                                    selectedDepartment = dept
                                    isDropdownExpanded = false
                                }
                            )
                        }
                        
                        HorizontalDivider()
                        
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Create New Department",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                isDropdownExpanded = false
                                onCreateDepartment()
                            }
                        )
                    }
                }
                
                // Vendor Name + Location
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = vendorNameAndLocation,
                        onValueChange = { vendorNameAndLocation = it },
                        label = { Text("Vendor Name + Location") },
                        placeholder = { Text("e.g., ABC Company - Mumbai") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = duplicateInSameDepartment != null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            if (duplicateInSameDepartment != null) {
                                Text(
                                    text = "$duplicateInSameDepartment already exists in $selectedDepartment.",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    )
                    
                    // Vendor suggestions dropdown (as per vendormanagement.md section 6.1)
                    if (suggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column {
                                suggestions.forEach { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                vendorNameAndLocation = suggestion
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = suggestion,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (suggestion != suggestions.last()) {
                                        HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (vendorNameAndLocation.isNotBlank() && selectedDepartment.isNotBlank()) {
                        // Check for different department conflict (as per vendormanagement.md)
                        if (conflictInDifferentDepartment != null) {
                            conflictMessage = "${conflictInDifferentDepartment.key} already exists in ${conflictInDifferentDepartment.value}. Confirm override to move it to $selectedDepartment."
                            showConflictDialog = true
                        } else {
                            onUpdateVendor(vendorNameAndLocation.trim(), selectedDepartment)
                        }
                    }
                },
                // Disable button if duplicate in same department (as per vendormanagement.md section 6.2)
                enabled = vendorNameAndLocation.isNotBlank() && 
                         selectedDepartment.isNotBlank() && 
                         duplicateInSameDepartment == null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
    
    // Conflict dialog for different department override (as per vendormanagement.md section 6.3)
    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Vendor Already Exists",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = conflictMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConflictDialog = false
                        onUpdateVendor(vendorNameAndLocation.trim(), selectedDepartment)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Override Department", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConflictDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun CreateDepartmentDialog(
    onDismiss: () -> Unit,
    onCreateDepartment: (String) -> Unit
) {
    var departmentName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 0.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Create New Department",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            OutlinedTextField(
                value = departmentName,
                onValueChange = { departmentName = it },
                label = { Text("Department Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (departmentName.isNotBlank()) {
                        onCreateDepartment(departmentName.trim())
                    }
                },
                enabled = departmentName.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Create", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}
