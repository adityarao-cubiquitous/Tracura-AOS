package com.cubiquitous.tracura.ui.view.businesshead

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.CustomerUserRelation
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.viewmodel.BusinessHeadViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

private enum class UserRoleFilter(val label: String) {
    All("All"),
    User("User"),
    Approver("Approver"),
    Manager("Manager")
}

private fun roleMatchesFilter(role: String, filter: UserRoleFilter): Boolean {
    val normalizedRole = role.trim().uppercase()
    return when (filter) {
        UserRoleFilter.All -> true
        UserRoleFilter.User -> normalizedRole == UserRole.USER.name
        UserRoleFilter.Approver -> normalizedRole == UserRole.APPROVER.name
        UserRoleFilter.Manager -> normalizedRole == "MANAGER" ||
            normalizedRole == "BUSINESSHEAD" ||
            normalizedRole == "BUSINESS_HEAD" ||
            normalizedRole == "PRODUCTION_HEAD"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewAllUsersScreen(
    onNavigateBack: () -> Unit,
    viewModel: BusinessHeadViewModel = hiltViewModel()
) {
    val palette = businessHeadUiPalette()
    val customerUserRelations by viewModel.customerUserRelations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val deleteUserError by viewModel.deleteUserError.collectAsState()
    val deleteUserSuccess by viewModel.deleteUserSuccess.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf(UserRoleFilter.All) }
    var userToDelete by remember { mutableStateOf<CustomerUserRelation?>(null) }

    val filteredUserRelations = remember(customerUserRelations, searchQuery, selectedRoleFilter) {
        val normalizedQuery = searchQuery.trim()
        customerUserRelations.filter { relation ->
            val matchesSearch = normalizedQuery.isBlank() ||
                relation.UserName.contains(normalizedQuery, ignoreCase = true) ||
                relation.userID.contains(normalizedQuery, ignoreCase = true)

            val matchesRole = roleMatchesFilter(relation.role, selectedRoleFilter)
            matchesSearch && matchesRole
        }
    }

    // Load users when screen appears
    LaunchedEffect(Unit) {
        viewModel.loadCustomerUserRelations()
    }

    LaunchedEffect(deleteUserSuccess) {
        if (deleteUserSuccess) {
            viewModel.clearDeleteUserSuccess()
        }
    }

    // Confirmation dialog
    userToDelete?.let { relation ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete User") },
            text = { Text("Are you sure you want to remove ${relation.UserName} from this customer?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteUserRelation(relation.userID, relation.role)
                        userToDelete = null
                    }
                ) {
                    Text("Delete", color = palette.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error dialog for blocked deletion
    deleteUserError?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearDeleteUserError() },
            title = { Text("Cannot Delete User") },
            text = { Text(errorMsg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearDeleteUserError() }) {
                    Text("OK")
                }
            }
        )
    }

    val userCount = filteredUserRelations.size
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All Users",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onNavigateBack) {
                            Text(
                                text = "Done",
                                fontSize = 17.sp,
                                color = palette.accent
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.tier2Surface,
                    titleContentColor = palette.primaryText,
                    navigationIconContentColor = palette.accent
                )
            )
        },
        containerColor = palette.tier1Background
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = palette.accent)
                        Text(
                            text = "Loading users...",
                            fontSize = 16.sp,
                            color = palette.secondaryText
                        )
                    }
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = palette.danger
                        )
                        Text(
                            text = "Error Loading Users",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Text(
                            text = error ?: "Unknown error",
                            fontSize = 14.sp,
                            color = palette.secondaryText,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.loadCustomerUserRelations() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.accent
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            customerUserRelations.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = palette.secondaryText
                        )
                        Text(
                            text = "No Users",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Text(
                            text = "No users have been added yet",
                            fontSize = 14.sp,
                            color = palette.secondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                SwipeRefresh(
                    state = rememberSwipeRefreshState(isLoading),
                    onRefresh = { viewModel.loadCustomerUserRelations() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            singleLine = true,
                            placeholder = { Text("Search by name or mobile number") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search users"
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UserRoleFilter.entries.forEach { filter ->
                                FilterChip(
                                    selected = selectedRoleFilter == filter,
                                    onClick = { selectedRoleFilter = filter },
                                    label = { Text(filter.label) }
                                )
                            }
                        }

                        // User count
                        Text(
                            text = if (searchQuery.isBlank() && selectedRoleFilter == UserRoleFilter.All) {
                                "$userCount user${if (userCount != 1) "s" else ""}"
                            } else {
                                "$userCount user${if (userCount != 1) "s" else ""} found"
                            },
                            fontSize = 14.sp,
                            color = palette.secondaryText,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        // Users list
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredUserRelations) { relation ->
                                UserRelationCard(
                                    relation = relation,
                                    palette = palette,
                                    onToggleActive = { isActive ->
                                        relation.id?.let { id ->
                                            viewModel.toggleUserRelationStatus(id, isActive)
                                        }
                                    },
                                    onDeleteClick = { userToDelete = relation }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRelationCard(
    relation: CustomerUserRelation,
    palette: BusinessHeadUiPalette,
    onToggleActive: (Boolean) -> Unit,
    onDeleteClick: () -> Unit
) {
    // Apply reduced opacity for inactive users
    val cardAlpha = if (relation.isActive) 1f else 0.6f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = palette.tier2Surface.copy(alpha = cardAlpha)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // User name
                Text(
                    text = relation.UserName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText.copy(alpha = cardAlpha)
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Phone number and department
                Text(
                    text = "${relation.userID} • ${relation.departmentName}",
                    fontSize = 14.sp,
                    color = palette.secondaryText.copy(alpha = cardAlpha)
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                // Role badge
                Surface(
                    color = when (relation.role.uppercase()) {
                        "USER" -> Color(0xFF4CAF50)
                        "APPROVER" -> Color(0xFF2196F3)
                        "BUSINESSHEAD", "BUSINESS_HEAD" -> Color(0xFF9C27B0)
                        "ADMIN" -> Color(0xFFFFC107)
                        else -> Color(0xFF9E9E9E)
                    }.copy(alpha = if (relation.isActive) 0.2f else 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when (relation.role.uppercase()) {
                            "USER" -> "User"
                            "APPROVER" -> "Approver"
                            "BUSINESSHEAD", "BUSINESS_HEAD" -> "Production Head"
                            "ADMIN" -> "Admin"
                            else -> relation.role
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when (relation.role.uppercase()) {
                            "USER" -> Color(0xFF4CAF50)
                            "APPROVER" -> Color(0xFF2196F3)
                            "BUSINESSHEAD", "BUSINESS_HEAD" -> Color(0xFF9C27B0)
                            "ADMIN" -> Color(0xFFFFC107)
                            else -> Color(0xFF9E9E9E)
                        }.copy(alpha = cardAlpha),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Toggle switch
            Switch(
                checked = relation.isActive,
                onCheckedChange = { checked ->
                    onToggleActive(checked)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = palette.success,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = palette.outline
                )
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Delete icon
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete user",
                    tint = palette.danger
                )
            }
        }
    }
}
