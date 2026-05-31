package com.cubiquitous.tracura.ui.view.businesshead

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.rememberModalBottomSheetState
import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.ExpenseStatus
import com.cubiquitous.tracura.model.PaymentMode
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.viewmodel.MemberExpensesViewModel
import com.cubiquitous.tracura.viewmodel.TeamMembersDetailViewModel
import com.cubiquitous.tracura.utils.FormatUtils
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private data class IosTeamPalette(
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
private fun rememberIosTeamPalette(accentColor: Color = MaterialTheme.colorScheme.primary): IosTeamPalette {
    val isDark = isSystemInDarkTheme()
    return remember(isDark, accentColor) {
        IosTeamPalette(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamMembersDetailScreen(
    projectID: String,
    customerID: String? = null,
    userRole: UserRole?,
    currentUserPhone: String? = null,
    onBackClick: () -> Unit,
    onAddMemberClick: () -> Unit = {},
    onMemberClick: (User) -> Unit = {},
    showExpensesInternally: Boolean = true,
    initialSelectedMember: User? = null
) {
    val iosPalette = rememberIosTeamPalette()
    val viewModel: TeamMembersDetailViewModel = hiltViewModel()
    val teamMembers by viewModel.teamMembers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var selectedMember by remember { mutableStateOf<User?>(initialSelectedMember) }

    // Load team members from departmentUserAssignments (role-based: all for BUSINESS_HEAD, assigned depts for APPROVER)
    LaunchedEffect(projectID, userRole, currentUserPhone) {
        viewModel.loadTeamMembersByProjectId(
            projectID,
            userRole = userRole,
            currentUserPhone = currentUserPhone
        )
    }

    // Auto-select initial member if provided
    LaunchedEffect(initialSelectedMember) {
        if (initialSelectedMember != null) {
            selectedMember = initialSelectedMember
        }
    }

    val filteredMembers = teamMembers.filter { member ->
        searchText.isEmpty() ||
        member.name.contains(searchText, ignoreCase = true) ||
        member.phone.contains(searchText, ignoreCase = true) ||
        member.email.contains(searchText, ignoreCase = true)
    }

    // Show MemberExpensesView if a member is selected (only if showExpensesInternally is true)
    // If showExpensesInternally is false, let the parent handle navigation via onMemberClick
    if (showExpensesInternally) {
        selectedMember?.let { member ->
            MemberExpensesView(
                member = member,
                projectID = projectID,
                onBackClick = {
                    selectedMember = null
                }
            )
            return
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient( //0xFF4285F4
                    colors = listOf(
                        iosPalette.accent,
                        iosPalette.accent,
                        iosPalette.tier1Background
                    ),
                    startY = 0f,
                    endY = 800f
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            HeaderView(
                memberCount = filteredMembers.size,
                isAdmin = userRole == UserRole.BUSINESS_HEAD,
                userRole = userRole,
                onBackClick = onBackClick,
                onAddClick = onAddMemberClick,
                onRefreshClick = { viewModel.refreshData() },
                iosPalette = iosPalette
            )

            // Search
            SearchView(
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                iosPalette = iosPalette
            )

            // Content
            when {
                isLoading -> LoadingView()
                filteredMembers.isEmpty() -> EmptyView()
                else -> MembersListView(
                    members = filteredMembers,
                    projectID = projectID,
                    isAdmin = userRole == UserRole.BUSINESS_HEAD,
                    iosPalette = iosPalette,
                    onMemberClick = { member ->
                        if (showExpensesInternally) {
                            // Use internal state to show expenses view
                            selectedMember = member
                        }
                        // Always call the callback (parent can handle navigation if needed)
                        onMemberClick(member)
                    }
                )
            }

            errorMessage?.let { error ->
                LaunchedEffect(error) {
                    // Handle error display
                }
            }
        }
    }
}

@Composable
private fun HeaderView(
    memberCount: Int,
    isAdmin: Boolean,
    userRole: UserRole?,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onRefreshClick: () -> Unit,
    iosPalette: IosTeamPalette
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button with circular background
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.25f),
                onClick = onBackClick
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Team Members",
                    color = iosPalette.onAccent,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "$memberCount ${if (memberCount == 1) "member" else "members"}",
                        color = iosPalette.onAccent,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isAdmin || userRole == UserRole.APPROVER) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.25f),
                        onClick = onAddClick
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = "Add member",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.25f),
                    onClick = onRefreshClick
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchView(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    iosPalette: IosTeamPalette
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = iosPalette.tier2Surface.copy(alpha = 0.95f),
        shadowElevation = 4.dp
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            placeholder = { 
                Text(
                    "Search by name, phone, or email...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = iosPalette.textSecondary.copy(alpha = 0.6f)
                )
            },
            leadingIcon = { 
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = iosPalette.accent,
                    modifier = Modifier.size(22.dp)
                )
            },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = iosPalette.accent.copy(alpha = 0.1f),
                        onClick = { onSearchTextChange("") }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = iosPalette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = iosPalette.accent
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = iosPalette.textPrimary,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Text(
                text = "Loading team members...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PersonOff,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "No Team Members",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Team members will appear here once they are added to the project.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun MembersListView(
    members: List<User>,
    projectID: String,
    isAdmin: Boolean,
    iosPalette: IosTeamPalette,
    onMemberClick: (User) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(members) { member ->
            TeamMemberRow(
                member = member,
                projectID = projectID,
                isAdmin = isAdmin,
                iosPalette = iosPalette,
                onTap = { onMemberClick(member) }
            )
        }
    }
}

@Composable
private fun TeamMemberRow(
    member: User,
    projectID: String,
    isAdmin: Boolean,
    iosPalette: IosTeamPalette,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: TeamMembersDetailViewModel = hiltViewModel()
    val departmentAssignments by viewModel.departmentAssignments.collectAsState()
    
    // Get department name from departmentUserAssignments
    val departmentName = remember(member.phone, departmentAssignments) {
        val normalizedPhone = member.phone.replace("+91", "").trim()
        departmentAssignments[normalizedPhone] ?: member.department.ifEmpty { "" }
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = iosPalette.tier2Surface,
        shadowElevation = 3.dp,
        tonalElevation = 1.dp,
        onClick = onTap
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            member.role.color.copy(alpha = 0.05f),
                            iosPalette.tier2Surface
                        ),
                        startX = 0f,
                        endX = 600f
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with gradient background
                Box(
                    modifier = Modifier.size(64.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            member.role.color.copy(alpha = 0.8f),
                                            member.role.color.copy(alpha = 0.5f)
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Member Info with improved hierarchy
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Name
                    Text(
                        text = member.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        style = MaterialTheme.typography.titleMedium,
                        color = iosPalette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Department Name (from departmentUserAssignments)
                    if (departmentName.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1E88E5).copy(alpha = 0.15f),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Business,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color(0xFF1E88E5)
                                    )
                                }
                            }
                            Text(
                                text = departmentName,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1E88E5),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // Phone/Email
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (member.phone.isNotEmpty()) Icons.Default.Phone else Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = iosPalette.textSecondary.copy(alpha = 0.7f)
                        )
                        Text(
                            text = member.phone.ifEmpty { member.email.ifEmpty { "No contact" } },
                            color = iosPalette.textSecondary,
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Role Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = member.role.color.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(member.role.color, CircleShape)
                            )
                            Text(
                                text = member.role.displayName,
                                fontSize = 11.sp,
                                style = MaterialTheme.typography.labelSmall,
                                color = member.role.color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Chevron icon for navigation
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = Color(0xFF1E88E5).copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "View details",
                            tint = Color(0xFF1E88E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// Extension for UserRole color
private val UserRole.color: Color
    get() = when (this) {
        UserRole.BUSINESS_HEAD -> Color(0xFFFF0000) // Red
        UserRole.MANAGER -> Color(0xFFFF0000)        // Red
        UserRole.APPROVER -> Color(0xFFFF9500)      // Orange
        UserRole.USER -> Color(0xFF007AFF)           // Blue
        UserRole.ADMIN -> Color(0xFF34C759)          // Green
    }

// Extension for UserRole displayName
private val UserRole.displayName: String
    get() = when (this) {
        UserRole.BUSINESS_HEAD -> "BusinessHead"
        UserRole.MANAGER -> "Manager"
        UserRole.APPROVER -> "Approver"
        UserRole.USER -> "User"
        UserRole.ADMIN -> "Admin"
    }

// Member Expenses View
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberExpensesView(
    member: User,
    projectID: String,
    onBackClick: () -> Unit
) {
    val iosPalette = rememberIosTeamPalette()
    val viewModel: MemberExpensesViewModel = hiltViewModel()
    val expenses by viewModel.expenses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Filter and Sort State
    var searchText by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<ExpenseStatus?>(null) }
    var selectedSortOption by remember { mutableStateOf("Date (Newest First)") }
    var showFilterSheet by remember { mutableStateOf(false) }

    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(member, projectID) {
        viewModel.loadExpenses(projectID, member)
    }

    // Filter and sort expenses
    val filteredAndSortedExpenses = remember(expenses, searchText, selectedStatusFilter, selectedSortOption) {
        var filtered = expenses

        if (selectedStatusFilter != null) {
            filtered = filtered.filter { it.status == selectedStatusFilter }
        }

        if (searchText.isNotBlank()) {
            filtered = filtered.filter { expense ->
                expense.description.contains(searchText, ignoreCase = true) ||
                expense.amount.toString().contains(searchText, ignoreCase = true) ||
                expense.category.contains(searchText, ignoreCase = true) ||
                expense.id.contains(searchText, ignoreCase = true)
            }
        }

        filtered = when (selectedSortOption) {
            "Date (Newest First)" -> filtered.sortedByDescending { it.createdAt.toDate().time }
            "Date (Oldest First)" -> filtered.sortedBy { it.createdAt.toDate().time }
            "Amount (High to Low)" -> filtered.sortedByDescending { it.amount }
            "Amount (Low to High)" -> filtered.sortedBy { it.amount }
            "Status" -> filtered.sortedBy { it.status.name }
            else -> filtered
        }

        filtered
    }

    val totalExpenses = filteredAndSortedExpenses.size
    val totalAmount = filteredAndSortedExpenses.sumOf { it.amount }
    val activeFilterCount = if (selectedStatusFilter != null) 1 else 0

    // Totals from full (unfiltered) list for the stat squares
    val approvedTotal = expenses.filter { it.status == ExpenseStatus.APPROVED }.sumOf { it.amount }
    val pendingTotal  = expenses.filter { it.status == ExpenseStatus.PENDING  }.sumOf { it.amount }
    val rejectedTotal = expenses.filter { it.status == ExpenseStatus.REJECTED }.sumOf { it.amount }
    val completeTotal = expenses.filter { it.status == ExpenseStatus.COMPLETE }.sumOf { it.amount }

    Scaffold(
        topBar = {
            MemberExpensesTopBar(
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                onFilterClick = { showFilterSheet = true },
                onBackClick = onBackClick,
                activeFilterCount = activeFilterCount,
                iosPalette = iosPalette
            )
        },
        containerColor = iosPalette.tier1Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Compact profile strip ─────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = iosPalette.tier2Surface,
                    border = BorderStroke(0.75.dp, iosPalette.hairline),
                    shadowElevation = if (isSystemInDarkTheme()) 0.dp else 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Mini avatar
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = iosPalette.accent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = member.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = iosPalette.onAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = iosPalette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = member.role.name.replace("_", " "),
                                style = MaterialTheme.typography.bodySmall,
                                color = iosPalette.textSecondary
                            )
                        }

                        // Stats pill
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$totalExpenses expense${if (totalExpenses != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = iosPalette.textSecondary
                            )
                            Text(
                                text = FormatUtils.formatCurrency(totalAmount),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = iosPalette.accent
                            )
                        }
                    }
                }

                // ── 4 status stat squares ─────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("Approved", approvedTotal, Color(0xFF4CAF50)),
                        Triple("Pending",  pendingTotal,  Color(0xFFFF9800)),
                        Triple("Rejected", rejectedTotal, Color(0xFFF44336)),
                        Triple("Complete", completeTotal, Color(0xFF2196F3))
                    ).forEach { (label, amount, color) ->
                        val status = when (label) {
                            "Approved" -> ExpenseStatus.APPROVED
                            "Pending" -> ExpenseStatus.PENDING
                            "Rejected" -> ExpenseStatus.REJECTED
                            else -> ExpenseStatus.COMPLETE
                        }
                        val isSelected = selectedStatusFilter == status
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable {
                                    selectedStatusFilter = if (selectedStatusFilter == status) null else status
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) color.copy(alpha = 0.18f) else iosPalette.tier2Surface,
                            shadowElevation = if (isSelected || isSystemInDarkTheme()) 0.dp else 1.dp,
                            border = if (isSelected) {
                                BorderStroke(1.5.dp, color.copy(alpha = 0.5f))
                            } else {
                                BorderStroke(0.75.dp, iosPalette.hairline)
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(color, CircleShape)
                                )
                                Column {
                                    Text(
                                        text = FormatUtils.formatCurrency(amount),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = iosPalette.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Active filter chip (only when filter is on) ───────────────
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedStatusFilter != null,
                    enter = androidx.compose.animation.expandVertically() +
                            androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() +
                           androidx.compose.animation.fadeOut()
                ) {
                    val statusColor = when (selectedStatusFilter) {
                        ExpenseStatus.APPROVED -> Color(0xFF4CAF50)
                        ExpenseStatus.PENDING  -> Color(0xFFFF9800)
                        ExpenseStatus.REJECTED -> Color(0xFFF44336)
                        ExpenseStatus.COMPLETE -> Color(0xFF2196F3)
                        null                   -> MaterialTheme.colorScheme.primary
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(iosPalette.tier1Background)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Filtered:",
                            style = MaterialTheme.typography.labelSmall,
                            color = iosPalette.textSecondary
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = statusColor.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(statusColor, CircleShape)
                                )
                                Text(
                                    text = selectedStatusFilter?.name?.lowercase()
                                        ?.replaceFirstChar { it.uppercase() } ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear filter",
                                    tint = statusColor,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { selectedStatusFilter = null }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "${filteredAndSortedExpenses.size} result${if (filteredAndSortedExpenses.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = iosPalette.textSecondary
                        )
                    }
                }

                // ── Expense List ──────────────────────────────────────────────
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = iosPalette.accent)
                        }
                    }
                    filteredAndSortedExpenses.isEmpty() -> {
                        EmptyExpensesView(memberName = member.name, iosPalette = iosPalette)
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredAndSortedExpenses) { expense ->
                                MaterialExpenseCard(expense = expense, iosPalette = iosPalette)
                            }
                        }
                    }
                }
            }

            // ── Floating Filter Bottom Sheet ──────────────────────────────────
            if (showFilterSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showFilterSheet = false },
                    sheetState = filterSheetState,
                    containerColor = iosPalette.tier2Surface,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(top = 12.dp, bottom = 4.dp)
                                .width(40.dp)
                                .height(4.dp)
                                .background(iosPalette.hairline, RoundedCornerShape(2.dp))
                        )
                    }
                ) {
                    FilterSortBottomSheetContent(
                        selectedStatusFilter = selectedStatusFilter,
                        onStatusFilterSelected = {
                            selectedStatusFilter = it
                            showFilterSheet = false
                        },
                        selectedSortOption = selectedSortOption,
                        onSortOptionSelected = { selectedSortOption = it },
                        onClearAll = {
                            selectedStatusFilter = null
                            selectedSortOption = "Date (Newest First)"
                            showFilterSheet = false
                        },
                        onDone = { showFilterSheet = false },
                        iosPalette = iosPalette
                    )
                }
            }
        }
    }
}

// ── Floating Filter & Sort Bottom Sheet Content ───────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSortBottomSheetContent(
    selectedStatusFilter: ExpenseStatus?,
    onStatusFilterSelected: (ExpenseStatus?) -> Unit,
    selectedSortOption: String,
    onSortOptionSelected: (String) -> Unit,
    onClearAll: () -> Unit,
    onDone: () -> Unit,
    iosPalette: IosTeamPalette
) {
    val primaryColor = iosPalette.accent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        // Sheet header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter & Sort",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = iosPalette.textPrimary
            )
            if (selectedStatusFilter != null) {
                TextButton(onClick = onClearAll) {
                    Text(
                        "Clear all",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFF44336)
                    )
                }
            }
        }

        HorizontalDivider(color = iosPalette.hairline)
        Spacer(modifier = Modifier.height(16.dp))

        // ── Status Filter ─────────────────────────────────────────────────────
        Text(
            text = "STATUS",
            style = MaterialTheme.typography.labelSmall,
            color = iosPalette.textSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        val statusConfigs = listOf(
            Triple(null as ExpenseStatus?, Color(0xFF607D8B), "All"),
            Triple(ExpenseStatus.APPROVED, Color(0xFF4CAF50), "Approved"),
            Triple(ExpenseStatus.PENDING, Color(0xFFFF9800), "Pending"),
            Triple(ExpenseStatus.REJECTED, Color(0xFFF44336), "Rejected"),
            Triple(ExpenseStatus.COMPLETE, Color(0xFF2196F3), "Complete"),
        )

        // 3-column grid of status chips
        val rows = statusConfigs.chunked(3)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (status, color, label) ->
                    val isSelected = selectedStatusFilter == status
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onStatusFilterSelected(status) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) color.copy(alpha = 0.12f) else iosPalette.tier3Field,
                        border = if (isSelected)
                            BorderStroke(1.5.dp, color.copy(alpha = 0.6f))
                        else BorderStroke(0.75.dp, iosPalette.hairline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color, CircleShape)
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) color else iosPalette.textPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = color,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                // Fill empty slots in last row
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = iosPalette.hairline)
        Spacer(modifier = Modifier.height(16.dp))

        // ── Sort Options ──────────────────────────────────────────────────────
        Text(
            text = "SORT BY",
            style = MaterialTheme.typography.labelSmall,
            color = iosPalette.textSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        val sortOptions = listOf(
            "Date (Newest First)" to Icons.Default.DateRange,
            "Date (Oldest First)" to Icons.Default.DateRange,
            "Amount (High to Low)" to Icons.Default.ArrowDownward,
            "Amount (Low to High)" to Icons.Default.ArrowUpward,
            "Status" to Icons.Default.Label
        )

        sortOptions.forEach { (option, icon) ->
            val isSelected = selectedSortOption == option
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clickable { onSortOptionSelected(option) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) primaryColor.copy(alpha = 0.08f) else Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (isSelected) primaryColor else iosPalette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) primaryColor else iosPalette.textPrimary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Done button
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                contentColor = iosPalette.onAccent
            )
        ) {
            Text(
                "Done",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}



// Material Design 2 Top Bar — redesigned with MD2 guidelines
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberExpensesTopBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onBackClick: () -> Unit,
    activeFilterCount: Int = 0,
    iosPalette: IosTeamPalette
) {
    val primaryColor = iosPalette.accent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = if (isSystemInDarkTheme()) 0.dp else 4.dp)
            .background(iosPalette.tier2Surface)
            .border(BorderStroke(0.75.dp, iosPalette.hairline))
    ) {
        // ── Top App Bar ──────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Member Expenses",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = iosPalette.textPrimary
                    )
                    if (activeFilterCount > 0) {
                        Text(
                            text = "$activeFilterCount filter${if (activeFilterCount > 1) "s" else ""} active",
                            style = MaterialTheme.typography.labelSmall,
                            color = iosPalette.textSecondary
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = iosPalette.textPrimary
                    )
                }
            },
            actions = {
                // Filter button with badge
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(onClick = onFilterClick) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = iosPalette.textPrimary
                        )
                    }
                    if (activeFilterCount > 0) {
                        Surface(
                            modifier = Modifier
                                .padding(top = 6.dp, end = 6.dp)
                                .size(16.dp),
                            shape = CircleShape,
                            color = Color(0xFFFFD600) // MD2 amber accent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = activeFilterCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF212121)
                                )
                            }
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // ── Elevated Search Bar (MD2 style) ──────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 2.dp),
            colors = CardDefaults.cardColors(containerColor = iosPalette.tier3Field),
            border = BorderStroke(0.75.dp, iosPalette.hairline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = primaryColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )

                BasicTextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = iosPalette.textPrimary
                    ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (searchText.isEmpty()) {
                            Text(
                                text = "Search by description, category, amount...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = iosPalette.textSecondary
                            )
                        }
                        innerTextField()
                    }
                )

                // Animated clear button
                androidx.compose.animation.AnimatedVisibility(
                    visible = searchText.isNotEmpty(),
                    enter = androidx.compose.animation.fadeIn() +
                            androidx.compose.animation.scaleIn(),
                    exit = androidx.compose.animation.fadeOut() +
                           androidx.compose.animation.scaleOut()
                ) {
                    IconButton(
                        onClick = { onSearchTextChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = iosPalette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialExpenseCard(
    expense: Expense,
    iosPalette: IosTeamPalette
) {
    val statusColor = when (expense.status) {
        ExpenseStatus.APPROVED -> Color(0xFF4CAF50)
        ExpenseStatus.PENDING  -> Color(0xFFFF9800)
        ExpenseStatus.REJECTED -> Color(0xFFF44336)
        ExpenseStatus.COMPLETE -> Color(0xFF2196F3)
        else                   -> Color(0xFF9E9E9E)
    }
    
    // Format date and time
    val formattedDateTime = remember(expense.createdAt) {
        try {
            val date = expense.createdAt.toDate()
            val dateFormat = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
            dateFormat.format(date)
        } catch (e: Exception) {
            expense.date
        }
    }
    
    // Payment method text
    val paymentMethod = when (expense.modeOfPayment) {
        PaymentMode.CASH -> "Cash"
        PaymentMode.UPI -> "UPI"
        PaymentMode.CHEQUE -> "Cheque"
        PaymentMode.CARD -> "Card"
        else -> PaymentMode.toString(expense.modeOfPayment)
    }
    
    // Check if expense has images
    val imageCount = remember(expense) {
        var count = 0
        if (expense.attachmentURL?.isNotEmpty() == true) count++
        if (expense.paymentProofURL?.isNotEmpty() == true) count++
        if (expense.attachmentUrl.isNotEmpty()) count++
        if (expense.paymentProofUrl.isNotEmpty()) count++
        count
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.75.dp, iosPalette.hairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row - Status and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(statusColor, CircleShape)
                        )
                        Text(
                            text = expense.status.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                    }
                }
                
                // Amount
                Text(
                    text = FormatUtils.formatCurrency(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.textPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            Text(
                text = expense.description.ifEmpty { expense.category },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = iosPalette.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Metadata Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Expense ID
                MetadataRow(
                    icon = Icons.Default.Tag,
                    text = expense.id.take(24) + if (expense.id.length > 24) "..." else "",
                    iosPalette = iosPalette
                )
                
                // Date and Time
                MetadataRow(
                    icon = Icons.Default.DateRange,
                    text = formattedDateTime,
                    iosPalette = iosPalette
                )
                
                // Payment Method and Attachments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetadataRow(
                        icon = when (expense.modeOfPayment) {
                            PaymentMode.CARD -> Icons.Default.CreditCard
                            PaymentMode.CHEQUE -> Icons.Default.Description
                            else -> Icons.Default.Payment
                        },
                        text = paymentMethod,
                        iosPalette = iosPalette
                    )
                    
                    if (imageCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = iosPalette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$imageCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = iosPalette.accent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    iosPalette: IosTeamPalette
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iosPalette.textSecondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = iosPalette.textSecondary
        )
    }
}

// Empty Expenses View
@Composable
private fun EmptyExpensesView(memberName: String, iosPalette: IosTeamPalette) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Search,
                null,
                modifier = Modifier.size(80.dp),
                tint = iosPalette.textSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Expenses",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = iosPalette.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$memberName hasn't submitted any expenses yet.",
                fontSize = 14.sp,
                color = iosPalette.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}


private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}

private fun Int.formatCurrency(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(this)
}

private fun Double.formatCurrency(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 2
    return formatter.format(this)
}
