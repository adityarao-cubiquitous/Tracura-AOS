package com.cubiquitous.tracura.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubiquitous.tracura.model.ContractorMode
import com.cubiquitous.tracura.model.LineItem
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.utils.FormatUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

private data class AddDepartmentDraftCache(
    val departmentName: String,
    val contractorMode: ContractorMode,
    val lineItems: List<LineItem>,
    val cachedLineItems: List<LineItem>,
    val contractorAmountText: String,
    val selectedApproverId: String,
    val selectedUserId: String,
    val approverDisplayName: String,
    val userDisplayName: String
)

private object AddDepartmentDraftCacheStore {
    private val cache = mutableMapOf<String, AddDepartmentDraftCache>()

    fun get(key: String): AddDepartmentDraftCache? = cache[key]

    fun set(key: String, value: AddDepartmentDraftCache) {
        cache[key] = value
    }

    fun clear(key: String) {
        cache.remove(key)
    }

    fun clearAll() {
        cache.clear()
    }
}

private fun addDepartmentCacheKey(
    phaseName: String,
    projectId: String,
    afterProjectCreationNavigation: Boolean,
    isEditMode: Boolean
): String {
    val safeProjectId = projectId.ifBlank { "no_project" }
    val safePhaseName = phaseName.ifBlank { "no_phase" }
    return listOf(
        safeProjectId,
        safePhaseName,
        afterProjectCreationNavigation.toString(),
        isEditMode.toString()
    ).joinToString("|")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddDepartmentModalSheet(
    phaseName: String,
    departmentName: String,
    onDepartmentNameChange: (String) -> Unit,
    contractorMode: ContractorMode,
    onContractorModeChange: (ContractorMode) -> Unit,
    lineItems: List<LineItem>,
    onLineItemsChange: (List<LineItem>) -> Unit,
    isDuplicateDepartmentName: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String?, String?, Double) -> Unit = { _, _, _ -> }, // approverPhone, userPhone, contractorAmount
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier,
    afterProjectCreationNavigation: Boolean = false,
    approverOptions: List<User> = emptyList(),
    userOptions: List<User> = emptyList(),
    onAssignmentChange: (String, String) -> Unit = { _, _ -> },
    projectId: String = "", // Project ID to fetch department assignments
) {
    val colorScheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cacheKey = remember(phaseName, projectId, afterProjectCreationNavigation, isEditMode) {
        addDepartmentCacheKey(phaseName, projectId, afterProjectCreationNavigation, isEditMode)
    }
    val cachedDraft = remember(cacheKey) {
        AddDepartmentDraftCacheStore.get(cacheKey)
    }
    var hasRestoredDraft by remember(cacheKey) { mutableStateOf(false) }
    var isClosing by remember(cacheKey) { mutableStateOf(false) }
    
    var showAddLineItemSheet by remember { mutableStateOf(false) }
    var editingLineItemIndex by remember { mutableStateOf<Int?>(null) }

    // Contractor amount (Labour-Only and Turnkey contractor-only)
    var contractorAmountText by remember {
        mutableStateOf(cachedDraft?.contractorAmountText ?: "")
    }

    var cachedLineItems by remember {
        mutableStateOf(cachedDraft?.cachedLineItems ?: lineItems)
    }

    // State variables for department assignments (used when afterProjectCreationNavigation is true)
    var selectedApproverId by remember {
        mutableStateOf(cachedDraft?.selectedApproverId ?: "")
    }
    var selectedUserId by remember {
        mutableStateOf(cachedDraft?.selectedUserId ?: "")
    }
    var approverDisplayName by remember {
        mutableStateOf(cachedDraft?.approverDisplayName ?: "")
    }
    var userDisplayName by remember {
        mutableStateOf(cachedDraft?.userDisplayName ?: "")
    }
    
    // Fetch department counts directly from Firebase
    var departmentUserCounts by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var departmentApproverCounts by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    
    // Fetch department assignments from project
    var departmentApproverAssignments by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var departmentUserAssignments by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    
    // Check if department already has assignments (case-insensitive)
    // Per AddDepartment.md: project-level assignments are stored WITHOUT phase prefix
    // e.g., departmentUserAssignments["Plumbing"] = "phone", NOT "phaseId_Plumbing"
    val ifDepartmentAlreadyContains = remember(departmentName, departmentApproverAssignments, departmentUserAssignments) {
        val trimmedDeptName = departmentName.trim()
        if (trimmedDeptName.isBlank()) {
            false
        } else {
            // Check if department name exists as exact key match (case-insensitive)
            val deptNameLower = trimmedDeptName.lowercase()
            
            Log.d("departmentAddingDebug", "🔍 Checking if department '$trimmedDeptName' already exists")
            Log.d("departmentAddingDebug", "   Looking for exact key: '$trimmedDeptName' (case-insensitive)")
            Log.d("departmentAddingDebug", "   Approver assignment keys: ${departmentApproverAssignments.keys}")
            Log.d("departmentAddingDebug", "   User assignment keys: ${departmentUserAssignments.keys}")
            
            val hasApprover = departmentApproverAssignments.keys.any { key ->
                // Exact match (case-insensitive)
                val matches = key.lowercase() == deptNameLower
                if (matches) {
                    Log.d("departmentAddingDebug", "   ✅ Found approver key: $key")
                }
                matches
            }
            val hasUser = departmentUserAssignments.keys.any { key ->
                // Exact match (case-insensitive)
                val matches = key.lowercase() == deptNameLower
                if (matches) {
                    Log.d("departmentAddingDebug", "   ✅ Found user key: $key")
                }
                matches
            }
            
            val result = hasApprover && hasUser
            Log.d("departmentAddingDebug", "   Result: hasApprover=$hasApprover, hasUser=$hasUser, ifDepartmentAlreadyContains=$result")
            result
        }
    }
    
    // Fetch project assignments when projectId is available
    LaunchedEffect(projectId) {
        if (projectId.isNotBlank() && afterProjectCreationNavigation) {
            try {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                Log.d("departmentAddingDebug", "🔍 Fetching assignments for projectId: $projectId")
                
                if (currentCustomerId != null) {
                    val firestore = FirebaseFirestore.getInstance()
                    val projectDoc = firestore.collection("customers")
                        .document(currentCustomerId)
                        .collection("projects")
                        .document(projectId)
                        .get()
                        .await()
                    
                    if (projectDoc.exists()) {
                        val approverAssignmentsAny = projectDoc.get("departmentApproverAssignments")
                        val userAssignmentsAny = projectDoc.get("departmentUserAssignments")
                        
                        departmentApproverAssignments = (approverAssignmentsAny as? Map<*, *>)?.mapNotNull { (k, v) ->
                            (k as? String)?.let { key -> key to v.toString() }
                        }?.toMap() ?: emptyMap()
                        departmentUserAssignments = (userAssignmentsAny as? Map<*, *>)?.mapNotNull { (k, v) ->
                            (k as? String)?.let { key -> key to v.toString() }
                        }?.toMap() ?: emptyMap()
                        
                        Log.d("departmentAddingDebug", "📋 Fetched approver assignments: $departmentApproverAssignments")
                        Log.d("departmentAddingDebug", "📋 Fetched user assignments: $departmentUserAssignments")
                    } else {
                        Log.w("departmentAddingDebug", "⚠️ Project document does not exist")
                    }
                } else {
                    Log.w("departmentAddingDebug", "⚠️ Current customer ID is null")
                }
            } catch (e: Exception) {
                android.util.Log.e("departmentAddingDebug", "Error fetching project assignments: ${e.message}")
            }
        } else {
            Log.d("departmentAddingDebug", "⏭️ Skipping assignment fetch: projectId='$projectId', afterProjectCreationNavigation=$afterProjectCreationNavigation")
        }
    }
    
    LaunchedEffect(Unit) {
        try {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val currentCustomerId = firebaseUser?.uid
            
            if (currentCustomerId != null) {
                val firestore = FirebaseFirestore.getInstance()
                val snapshot = firestore.collection("customers")
                    .document(currentCustomerId)
                    .get()
                    .await()
                
                if (snapshot.exists()) {
                    val userCountsAny = snapshot.get("departmentUserCounts")
                    val approverCountsAny = snapshot.get("departmentApproverCounts")
                    
                    fun parseCounts(any: Any?): Map<String, Long> {
                        return when (any) {
                            is Map<*, *> -> any.mapNotNull { (k, v) ->
                                val key = k as? String
                                val value = when (v) {
                                    is Long -> v
                                    is Int -> v.toLong()
                                    is Double -> v.toLong()
                                    else -> null
                                }
                                if (key != null && value != null) key to value else null
                            }.toMap()
                            else -> emptyMap()
                        }
                    }
                    
                    departmentUserCounts = parseCounts(userCountsAny)
                    departmentApproverCounts = parseCounts(approverCountsAny)
                }
            }
            } catch (e: Exception) {
            android.util.Log.e("departmentAddingDebug", "Error loading department counts: ${e.message}")
        }
    }
    
    // Get all unique department names from both maps
    val allDepartments = remember(departmentUserCounts, departmentApproverCounts) {
        (departmentUserCounts.keys + departmentApproverCounts.keys).distinct().sorted()
    }
    
    // Categorize departments: those with both users and approvers (enabled) vs only one (disabled)
    val enabledDepartments = remember(departmentUserCounts, departmentApproverCounts) {
        allDepartments.filter { dept ->
            val userCount = departmentUserCounts[dept] ?: 0L
            val approverCount = departmentApproverCounts[dept] ?: 0L
            // A department is enabled if it has both users and approvers (both counts > 0)
            userCount > 0 && approverCount > 0
        }
    }
    
    val disabledDepartments = remember(departmentUserCounts, departmentApproverCounts) {
        allDepartments.filter { dept ->
            val userCount = departmentUserCounts[dept] ?: 0L
            val approverCount = departmentApproverCounts[dept] ?: 0L
            // A department is disabled if it has users but no approvers, or has approvers but no users
            (userCount > 0 && approverCount == 0L) || (userCount == 0L && approverCount > 0)
        }
    }
    
    // Search state
    var searchQuery by remember { mutableStateOf(cachedDraft?.departmentName ?: "") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    
    // Filter departments based on search query
    val filteredEnabledDepartments = remember(enabledDepartments, searchQuery) {
        if (searchQuery.isBlank()) {
            enabledDepartments
        } else {
            enabledDepartments.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    val filteredDisabledDepartments = remember(disabledDepartments, searchQuery) {
        if (searchQuery.isBlank()) {
            disabledDepartments
        } else {
            disabledDepartments.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    // Check if department name exists in the available departments list
    val isCustomDepartmentName = remember(departmentName, allDepartments) {
        departmentName.isNotBlank() && !allDepartments.contains(departmentName)
    }

    LaunchedEffect(cachedDraft) {
        if (cachedDraft != null && !hasRestoredDraft) {
            if (departmentName != cachedDraft.departmentName) {
                onDepartmentNameChange(cachedDraft.departmentName)
            }
            if (contractorMode != cachedDraft.contractorMode) {
                onContractorModeChange(cachedDraft.contractorMode)
            }
            if (lineItems != cachedDraft.lineItems) {
                onLineItemsChange(cachedDraft.lineItems)
            }
            if (searchQuery != cachedDraft.departmentName) {
                searchQuery = cachedDraft.departmentName
            }
            hasRestoredDraft = true
        }
    }

    LaunchedEffect(
        departmentName,
        contractorMode,
        lineItems,
        cachedLineItems,
        contractorAmountText,
        selectedApproverId,
        selectedUserId,
        approverDisplayName,
        userDisplayName
    ) {
        if (isClosing) {
            return@LaunchedEffect
        }
        AddDepartmentDraftCacheStore.set(
            cacheKey,
            AddDepartmentDraftCache(
                departmentName = departmentName,
                contractorMode = contractorMode,
                lineItems = lineItems,
                cachedLineItems = cachedLineItems,
                contractorAmountText = contractorAmountText,
                selectedApproverId = selectedApproverId,
                selectedUserId = selectedUserId,
                approverDisplayName = approverDisplayName,
                userDisplayName = userDisplayName
            )
        )
    }

    val handleDismiss = {
        isClosing = true
        onDismiss()
    }

    val resetDraft = {
        AddDepartmentDraftCacheStore.clearAll()
        hasRestoredDraft = true
        onContractorModeChange(ContractorMode.LABOUR_ONLY)
        onDepartmentNameChange("")
        searchQuery = ""
        contractorAmountText = ""
        cachedLineItems = emptyList()
        onLineItemsChange(emptyList())
        selectedApproverId = ""
        selectedUserId = ""
        approverDisplayName = ""
        userDisplayName = ""
    }
    
    // Check if selected department is disabled
    // A department is disabled if it has users but no approvers, or has approvers but no users
    val isSelectedDepartmentDisabled = remember(departmentName, departmentUserCounts, departmentApproverCounts) {
        if (departmentName.isBlank()) false
        else {
            val userCount = departmentUserCounts[departmentName] ?: 0L
            val approverCount = departmentApproverCounts[departmentName] ?: 0L
            (userCount > 0 && approverCount == 0L) || (userCount == 0L && approverCount > 0)
        }
    }
    
    // Determine what message to show for disabled selected department
    val disabledMessage = remember(departmentName, departmentUserCounts, departmentApproverCounts) {
        if (!isSelectedDepartmentDisabled) null
        else {
            val userCount = departmentUserCounts[departmentName] ?: 0L
            val approverCount = departmentApproverCounts[departmentName] ?: 0L
            when {
                userCount > 0 && approverCount == 0L -> "Add approver for $departmentName department to activate"
                userCount == 0L && approverCount > 0 -> "Add user for $departmentName department to activate"
                else -> null
            }
        }
    }
    
    LaunchedEffect(lineItems, contractorMode) {
        if (contractorMode != ContractorMode.TURNKEY_AMOUNT_ONLY && lineItems != cachedLineItems) {
            cachedLineItems = lineItems
        }
    }

    val turnkeyAmountOk = remember(contractorMode, contractorAmountText) {
        when (contractorMode) {
            ContractorMode.TURNKEY_AMOUNT_ONLY ->
                (contractorAmountText.toDoubleOrNull() ?: 0.0) > 0.0
            else -> true
        }
    }

    val isCreateEnabled = departmentName.isNotBlank() &&
        !isDuplicateDepartmentName &&
        !isSelectedDepartmentDisabled &&
        !isCustomDepartmentName &&
        turnkeyAmountOk
    
    ModalBottomSheet(
        onDismissRequest = handleDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        color = colorScheme.outline,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Header with Cancel, Title, and Create
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = handleDismiss) {
                    Text("Cancel", color = colorScheme.primary)
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    // Vertical blue line on the left
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(24.dp)
                                .background(colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = if (isEditMode) "Edit Department" else "Add Department1",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }
                    
                    // Subtitle with calendar icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "to $phaseName",
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = resetDraft,
                        enabled = !isClosing
                    ) {
                        Text(
                            "Reset",
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = {
                            val trimmedDeptName = departmentName.trim()

                            Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
                            Log.d("DEPT_ADD_FLOW", "🏁 START: Create button clicked in AddDepartmentModalSheet")
                            Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
                            Log.d("DEPT_ADD_FLOW", "📋 Step 1: Initial State Check")
                            Log.d("DEPT_ADD_FLOW", "   - Department name: '$trimmedDeptName'")
                            Log.d("DEPT_ADD_FLOW", "   - Phase name: '$phaseName'")
                            Log.d("DEPT_ADD_FLOW", "   - Project ID: '$projectId'")
                            Log.d("DEPT_ADD_FLOW", "   - afterProjectCreationNavigation: $afterProjectCreationNavigation")
                            Log.d("DEPT_ADD_FLOW", "   - isEditMode: $isEditMode")
                            Log.d("DEPT_ADD_FLOW", "   - Line items count: ${lineItems.size}")
                            Log.d("DEPT_ADD_FLOW", "   - Contractor mode: $contractorMode")

                            // Determine which phone numbers to use
                            val approverPhone: String?
                            val userPhone: String?

                            if (afterProjectCreationNavigation) {
                                Log.d("DEPT_ADD_FLOW", "────────────────────────────────────────────────────────────")
                                Log.d("DEPT_ADD_FLOW", "📋 Step 2: Assignment Resolution (afterProjectCreationNavigation = true)")
                                Log.d("DEPT_ADD_FLOW", "   - ifDepartmentAlreadyContains: $ifDepartmentAlreadyContains")
                                Log.d("DEPT_ADD_FLOW", "   - Existing project approver assignments: ${departmentApproverAssignments.keys.joinToString()}")
                                Log.d("DEPT_ADD_FLOW", "   - Existing project user assignments: ${departmentUserAssignments.keys.joinToString()}")

                                if (ifDepartmentAlreadyContains) {
                                    Log.d("DEPT_ADD_FLOW", "   ℹ️ Department exists - reusing assignments from project")
                                    // Department already exists in project - find the exact key match
                                    // Per AddDepartment.md: assignments stored without phase prefix
                                    val deptNameLower = trimmedDeptName.lowercase()

                                    // Find exact key match (case-insensitive)
                                    val approverKey = departmentApproverAssignments.keys.find { key ->
                                        key.lowercase() == deptNameLower
                                    }
                                    val userKey = departmentUserAssignments.keys.find { key ->
                                        key.lowercase() == deptNameLower
                                    }

                                    approverPhone = approverKey?.let { departmentApproverAssignments[it] }
                                    userPhone = userKey?.let { departmentUserAssignments[it] }
                                    Log.d("DEPT_ADD_FLOW", "   ✅ Resolved approver key: '$approverKey' → phone: $approverPhone")
                                    Log.d("DEPT_ADD_FLOW", "   ✅ Resolved user key: '$userKey' → phone: $userPhone")
                                } else {
                                    Log.d("DEPT_ADD_FLOW", "   ℹ️ New department - using dropdown selections")
                                    Log.d("DEPT_ADD_FLOW", "   - Selected approver ID: '$selectedApproverId'")
                                    Log.d("DEPT_ADD_FLOW", "   - Selected user ID: '$selectedUserId'")
                                    // New department - use selected values from dropdowns
                                    approverPhone = if (selectedApproverId.isNotBlank()) selectedApproverId else null
                                    userPhone = if (selectedUserId.isNotBlank()) selectedUserId else null
                                    Log.d("DEPT_ADD_FLOW", "   ✅ Resolved approver phone: $approverPhone")
                                    Log.d("DEPT_ADD_FLOW", "   ✅ Resolved user phone: $userPhone")
                                }
                            } else {
                                Log.d("DEPT_ADD_FLOW", "────────────────────────────────────────────────────────────")
                                Log.d("DEPT_ADD_FLOW", "📋 Step 2: Assignment Resolution (afterProjectCreationNavigation = false)")
                                Log.d("DEPT_ADD_FLOW", "   ⚠️ No assignments set (not in post-project-creation flow)")
                                approverPhone = null
                                userPhone = null
                            }

                            Log.d("DEPT_ADD_FLOW", "────────────────────────────────────────────────────────────")
                            Log.d("DEPT_ADD_FLOW", "📋 Step 3: Invoking onCreate Callback")
                            Log.d("DEPT_ADD_FLOW", "   - Approver phone to pass: $approverPhone")
                            Log.d("DEPT_ADD_FLOW", "   - User phone to pass: $userPhone")
                            Log.d("DEPT_ADD_FLOW", "   ⏩ Calling onCreate(approverPhone='$approverPhone', userPhone='$userPhone')")
                            Log.d("DEPT_ADD_FLOW", "   ⏩ This will trigger:")
                            Log.d("DEPT_ADD_FLOW", "      1. Department added to 'departments' collection")
                            Log.d("DEPT_ADD_FLOW", "      2. Department details saved to 'projects' collection")
                            Log.d("DEPT_ADD_FLOW", "      3. PhaseId added to User_Customer_Project_Phase_Relation")
                            Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")

                            val contractorAmt = when (contractorMode) {
                                ContractorMode.LABOUR_ONLY, ContractorMode.TURNKEY_AMOUNT_ONLY ->
                                    contractorAmountText.toDoubleOrNull() ?: 0.0
                                ContractorMode.SELF_EXECUTION -> 0.0
                            }
                            isClosing = true
                            AddDepartmentDraftCacheStore.clear(cacheKey)
                            onCreate(approverPhone, userPhone, contractorAmt)
                        },
                        enabled = isCreateEnabled
                    ) {
                        Text(
                            if (isEditMode) "Save" else "Create",
                            color = if (isCreateEnabled) colorScheme.primary else colorScheme.outline
                        )
                    }
                }
            }

            Divider(color = colorScheme.outlineVariant, thickness = 1.dp)
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Department Name Section (Light Blue Card)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.primaryContainer.copy(alpha = 0.35f)
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Building/Grid icon
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Department Name",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }
                        
                        // Searchable Dropdown
                        ExposedDropdownMenuBox(
                            expanded = isDropdownExpanded,
                            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = departmentName,
                                onValueChange = { newValue ->
                                    // Update search query for filtering dropdown
                                    searchQuery = newValue
                                    // Allow typing for search, but only save if it's a valid selection
                                    // This allows users to type to search, but they must select from dropdown
                                    onDepartmentNameChange(newValue)
                                    isDropdownExpanded = true
                                },
                                readOnly = false, // Allow typing for search
                                placeholder = { Text("Search and select department") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = if (isDropdownExpanded) "Collapse" else "Expand",
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                },
                                isError = isDuplicateDepartmentName || isSelectedDepartmentDisabled || isCustomDepartmentName,
                                supportingText = when {
                                    isCustomDepartmentName -> {
                                        { Text("Please add at least one user and approver for department", color = colorScheme.error, fontSize = 11.sp) }
                                    }
                                    isDuplicateDepartmentName -> {
                                        { Text("Department name must be unique", color = colorScheme.error, fontSize = 11.sp) }
                                    }
                                    isSelectedDepartmentDisabled -> {
                                        { Text(disabledMessage ?: "", color = colorScheme.error, fontSize = 11.sp) }
                                    }
                                    else -> null
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colorScheme.primary,
                                    unfocusedBorderColor = colorScheme.outline,
                                    errorBorderColor = colorScheme.error,
                                    focusedContainerColor = colorScheme.surface,
                                    unfocusedContainerColor = colorScheme.surface,
                                    disabledContainerColor = if (isSelectedDepartmentDisabled) colorScheme.errorContainer else colorScheme.surface
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            
                            ExposedDropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false },
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                // Show enabled departments first
                                if (filteredEnabledDepartments.isNotEmpty()) {
                                    filteredEnabledDepartments.forEach { dept ->
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    text = dept,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            },
                                            onClick = {
                                                onDepartmentNameChange(dept)
                                                searchQuery = dept // Set search query to selected value
                                                isDropdownExpanded = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                                
                                // Show disabled departments with visual indication
                                // Debug: Log all departments and their counts
                                Log.d("departmentUsersTracking", "Total departments: ${allDepartments.size}, Enabled: ${enabledDepartments.size}, Disabled: ${disabledDepartments.size}")
                                allDepartments.forEach { dept ->
                                    val userCount = departmentUserCounts[dept] ?: 0L
                                    val approverCount = departmentApproverCounts[dept] ?: 0L
                                    Log.d("departmentUsersTracking", "Department: $dept, userCount: $userCount, approverCount: $approverCount")
                                }
                                
                                if (filteredDisabledDepartments.isNotEmpty()) {
                                    if (filteredEnabledDepartments.isNotEmpty()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = colorScheme.outlineVariant
                                        )
                                    }
                                    filteredDisabledDepartments.forEach { dept ->
                                        val userCount = departmentUserCounts[dept] ?: 0L
                                        val approverCount = departmentApproverCounts[dept] ?: 0L
                                        val needsApprover = approverCount == 0L
                                        val needsUser = userCount == 0L

                                        Log.d("departmentUsersTracking","Disabled department: $dept, userCount: $userCount, approverCount: $approverCount")
                                        
                                        DropdownMenuItem(
                                            text = { 
                                                Column {
                                                    Text(
                                                        text = dept,
                                                        fontWeight = FontWeight.Medium,
                                                        color = colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = if (needsUser && needsApprover) "Needs approver and User"
                                                        else if (needsApprover) "Needs approver"
                                                        else "Needs user",
                                                        fontSize = 11.sp,
                                                        color = colorScheme.error
                                                    )
                                                }
                                            },
                                            onClick = {
                                                // Allow selection but show warning
                                                onDepartmentNameChange(dept)
                                                searchQuery = dept // Set search query to selected value
                                                isDropdownExpanded = false
                                            },
                                            enabled = true, // Allow selection to show warning
                                            colors = MenuDefaults.itemColors(
                                                textColor = colorScheme.onSurfaceVariant,
                                                disabledTextColor = colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                                
                                // Show message if no departments found
                                if (filteredEnabledDepartments.isEmpty() && filteredDisabledDepartments.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = "No departments found",
                                                color = colorScheme.onSurfaceVariant,
                                                fontSize = 14.sp
                                            )
                                        },
                                        onClick = { },
                                        enabled = false
                                    )
                                }
                            }
                        }
                        
                        // Red warning message at bottom for disabled selected department or custom name
                        if ((isSelectedDepartmentDisabled && disabledMessage != null) || isCustomDepartmentName) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        colorScheme.errorContainer.copy(alpha = 0.65f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isCustomDepartmentName) {
                                        "Please add at least one user and approver for department"
                                    } else {
                                        disabledMessage ?: ""
                                    },
                                    fontSize = 12.sp,
                                    color = colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                // Contractor Mode Section (Light Purple Card)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.tertiaryContainer.copy(alpha = 0.35f)
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Two people icon (using Person icon)
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Contractor Mode",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }
                        
                         FlowRow(
                             modifier = Modifier.fillMaxWidth(),
                             horizontalArrangement = Arrangement.spacedBy(12.dp),
                             verticalArrangement = Arrangement.spacedBy(12.dp),
                             maxItemsInEachRow = 2
                         ) {
                              // Labour-Only
                              Button(
                                  onClick = {
                                      // When switching to LABOUR_ONLY, filter out Labour items
                                      if (contractorMode != ContractorMode.LABOUR_ONLY) {
                                          val baseItems = if (contractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY) {
                                              cachedLineItems
                                          } else {
                                              lineItems
                                          }
                                          val filteredItems = baseItems.filter { it.itemType != "Labour" }
                                          onLineItemsChange(filteredItems)
                                      }
                                      onContractorModeChange(ContractorMode.LABOUR_ONLY)
                                  },
                                  modifier = Modifier.weight(1f),
                                  colors = ButtonDefaults.buttonColors(
                                     containerColor = if (contractorMode == ContractorMode.LABOUR_ONLY) colorScheme.primary else colorScheme.surfaceVariant,
                                     contentColor = if (contractorMode == ContractorMode.LABOUR_ONLY) colorScheme.onPrimary else colorScheme.onSurface
                                 ),
                                 shape = RoundedCornerShape(12.dp)
                             ) {
                                 Text(
                                     "Labour-Only",
                                     fontSize = 15.sp,
                                     fontWeight = FontWeight.Medium
                                 )
                             }
 
                              // Self Execution (line items only)
                              Button(
                                  onClick = {
                                      contractorAmountText = ""
                                      if (contractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY) {
                                          onLineItemsChange(cachedLineItems)
                                      }
                                      onContractorModeChange(ContractorMode.SELF_EXECUTION)
                                  },
                                  modifier = Modifier.weight(1f),
                                  colors = ButtonDefaults.buttonColors(
                                     containerColor = if (contractorMode == ContractorMode.SELF_EXECUTION) colorScheme.primary else colorScheme.surfaceVariant,
                                     contentColor = if (contractorMode == ContractorMode.SELF_EXECUTION) colorScheme.onPrimary else colorScheme.onSurface
                                 ),
                                 shape = RoundedCornerShape(12.dp)
                             ) {
                                 Text(
                                     "Self Execution",
                                     fontSize = 15.sp,
                                     fontWeight = FontWeight.Medium
                                 )
                             }
 
                              // Turnkey (contractor amount only; no line items)
                              Button(
                                  onClick = {
                                      if (contractorMode != ContractorMode.TURNKEY_AMOUNT_ONLY) {
                                          cachedLineItems = lineItems
                                          onLineItemsChange(emptyList())
                                      }
                                      onContractorModeChange(ContractorMode.TURNKEY_AMOUNT_ONLY)
                                  },
                                  modifier = Modifier.weight(1f),
                                  colors = ButtonDefaults.buttonColors(
                                     containerColor = if (contractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY) colorScheme.primary else colorScheme.surfaceVariant,
                                     contentColor = if (contractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY) colorScheme.onPrimary else colorScheme.onSurface
                                 ),
                                 shape = RoundedCornerShape(12.dp)
                              ) {
                                  Text(
                                      "Turnkey",
                                      fontSize = 15.sp,
                                      fontWeight = FontWeight.Medium
                                  )
                              }
                         }
 
                         // Contractor amount input — Labour-Only + Turnkey modes (contractor-only)
                         if (
                             contractorMode == ContractorMode.LABOUR_ONLY ||
                             contractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY
                         ) {
                             OutlinedTextField(
                                 value = contractorAmountText,
                                 onValueChange = { contractorAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                                  label = {
                                      Text(
                                          if (contractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY) "Turnkey" else "Contractor Amount"
                                      )
                                  },
                                 placeholder = { Text("Enter amount") },
                                 modifier = Modifier.fillMaxWidth(),
                                 singleLine = true,
                                 keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                     keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                 ),
                                 shape = RoundedCornerShape(12.dp),
                                 colors = OutlinedTextFieldDefaults.colors(
                                     focusedBorderColor = colorScheme.primary,
                                     unfocusedBorderColor = colorScheme.outline,
                                     focusedContainerColor = colorScheme.surface,
                                      unfocusedContainerColor = colorScheme.surface
                                  )
                             )
                         }
                     }
                 }

                // Line Items Section (applicable for Labour-Only and Self Execution, not for Turnkey)
                if (contractorMode == ContractorMode.LABOUR_ONLY || contractorMode == ContractorMode.SELF_EXECUTION) Card(
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // List icon
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = null,
                                    tint = colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Line Items",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                            }
                            
                            // Info icon with "sum equals budget" text
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
                        
                        // Existing Line Items
                        lineItems.forEachIndexed { index, lineItem ->
                            val titleParts = listOf(lineItem.itemType, lineItem.item)
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            val itemTitle = if (titleParts.isNotEmpty()) {
                                titleParts.joinToString(" • ")
                            } else {
                                "Line Item"
                            }

                            val itemDetailParts = listOf(
                                lineItem.spec.trim().takeIf { it.isNotBlank() },
                                if (lineItem.quantity > 0) lineItem.quantity.toString() else null,
                                lineItem.uom.trim().takeIf { it.isNotBlank() }
                            ).filterNotNull()
                            val itemDetails = itemDetailParts.joinToString(" | ")

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            editingLineItemIndex = index
                                            showAddLineItemSheet = true
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = itemTitle,
                                            fontSize = 14.sp,
                                            color = colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (itemDetails.isNotBlank()) {
                                            Text(
                                                text = itemDetails,
                                                fontSize = 12.sp,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                
                                // Green pill with amount
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
                        
                        // Add Line Item Button (Orange)
                        Button(
                            onClick = {
                                editingLineItemIndex = null
                                showAddLineItemSheet = true
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
                        
                        // Total Section
                        if (lineItems.isNotEmpty()) {
                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = colorScheme.outlineVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Sigma symbol (Σ)
                                    Text(
                                        text = "Σ",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Total",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = FormatUtils.formatCurrencyWithoutDecimals(lineItems.sumOf { it.total }),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                )
                            }
                        }
                    }
                    if (afterProjectCreationNavigation && !ifDepartmentAlreadyContains && departmentName.isNotBlank()){

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(20.dp) // Gap between elements
                            ) {
                                // Header
                                Text(
                                    text = "Assign Roles",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )

                                // Filter approvers by department
                                val approverOptionsForDepartment = remember(approverOptions, departmentName) {
                                    approverOptions.filter { user ->
                                        user.department.equals(departmentName, ignoreCase = true)
                                    }
                                }
                                
                                // Create map from filtered approvers (name -> phone)
                                val approverOptionsMap = remember(approverOptionsForDepartment) {
                                    approverOptionsForDepartment.associate { it.name to it.phone }
                                }

                                // 3. Approver Dropdown
                                ModernDropdown(
                                    label = "Select Approver",
                                    icon = Icons.Default.SupervisorAccount,
                                    options = approverOptionsMap.keys.toList(),
                                    selectedText = approverDisplayName,
                                    onOptionSelected = { name ->
                                        approverDisplayName = name
                                        selectedApproverId = approverOptionsMap[name] ?: "" // Update the ID variable
                                        onAssignmentChange(selectedApproverId, selectedUserId)
                                    }
                                )

                                // Filter users by department
                                val userOptionsForDepartment = remember(userOptions, departmentName) {
                                    userOptions.filter { user ->
                                        user.department.equals(departmentName, ignoreCase = true)
                                    }
                                }
                                
                                // Create map from filtered users (name -> phone)
                                val userOptionsMap = remember(userOptionsForDepartment) {
                                    userOptionsForDepartment.associate { it.name to it.phone }
                                }

                                // 4. User Dropdown
                                ModernDropdown(
                                    label = "Select User",
                                    icon = Icons.Default.Person,
                                    options = userOptionsMap.keys.toList(),
                                    selectedText = userDisplayName,
                                    onOptionSelected = { name ->
                                        userDisplayName = name
                                        selectedUserId = userOptionsMap[name] ?: "" // Update the ID variable
                                        onAssignmentChange(selectedApproverId, selectedUserId)
                                    }
                                )

                                // Debug Text (Optional: to show you the vars are updating)
                                /*
                                Text(
                                    text = "Selected IDs: Approver=$approverId, User=$userId",
                                    fontSize = 10.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                                */
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Add Line Item Sheet
    if (showAddLineItemSheet) {
        AddLineItemModalSheet(
            lineItem = editingLineItemIndex?.let { lineItems.getOrNull(it) } ?: LineItem(),
            contractorMode = contractorMode,
            onDismiss = {
                showAddLineItemSheet = false
                editingLineItemIndex = null
            },
            onAdd = { newLineItem ->
                val updatedList = lineItems.toMutableList()
                if (editingLineItemIndex != null && editingLineItemIndex!! < updatedList.size) {
                    updatedList[editingLineItemIndex!!] = newLineItem
                } else {
                    updatedList.add(newLineItem)
                }
                onLineItemsChange(updatedList)
                showAddLineItemSheet = false
                editingLineItemIndex = null
            },
            isEditMode = editingLineItemIndex != null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernDropdown(
    label: String,
    icon: ImageVector,
    options: List<String>,
    selectedText: String,
    onOptionSelected: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {}, // Read-only
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Choose...") },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorScheme.primary
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = RoundedCornerShape(12.dp), // Modern Rounded Corners
                                  colors = OutlinedTextFieldDefaults.colors(
                                      focusedBorderColor = colorScheme.primary,
                                      unfocusedBorderColor = colorScheme.outline,
                                      focusedContainerColor = colorScheme.surface,
                                      unfocusedContainerColor = colorScheme.surface
                                  ),
                                  modifier = Modifier
                                      .fillMaxWidth()
                                      .menuAnchor()
                                )
                                
                                ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(colorScheme.surface)
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No options available", color = colorScheme.onSurfaceVariant) },
                    onClick = {}
                )
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
