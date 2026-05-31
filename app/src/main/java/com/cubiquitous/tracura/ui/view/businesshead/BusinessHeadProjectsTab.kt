package com.cubiquitous.tracura.ui.view.businesshead

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.viewmodel.ProjectViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.viewmodel.BusinessHeadViewModel
import com.cubiquitous.tracura.viewmodel.NotificationViewModel
import com.cubiquitous.tracura.ui.common.NotificationBadgeComponent
import com.cubiquitous.tracura.ui.common.NotificationPopupDialog
import com.cubiquitous.tracura.model.NotificationBadge
import com.cubiquitous.tracura.utils.FormatUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.navigation.buildExpenseListNotificationTag
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.cubiquitous.tracura.R

private data class IosTabPalette(
    val tier1Background: Color,
    val tier2Surface: Color,
    val tier3Field: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val hairline: Color,
    val warning: Color,
    val danger: Color,
    val dangerContainer: Color
)

@Composable
private fun rememberIosTabPalette(accentColor: Color = MaterialTheme.colorScheme.primary): IosTabPalette {
    val isDark = isSystemInDarkTheme()
    return remember(isDark, accentColor) {
        IosTabPalette(
            tier1Background = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7),
            tier2Surface = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
            tier3Field = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF),
            textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
            textSecondary = if (isDark) Color(0x99EBEBF5) else Color(0x993C3C43),
            accent = accentColor,
            onAccent = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF),
            hairline = if (isDark) Color(0xFF38383A) else Color(0x1F000000),
            warning = Color(0xFFFFC107),
            danger = Color(0xFFEC5350),
            dangerContainer = if (isDark) Color(0xFF3A1E1E) else Color(0xFFFFEFEF)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessHeadProjectsTab(
    onNavigateToProject: (String, Project?, String?, String?) -> Unit,
    onNavigateToNewProject: () -> Unit,
    onNavigateToEditProject: (String) -> Unit,
    onNavigateToNewProjectForResubmission: (String) -> Unit,
    onNavigateToOverallReports: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToChat: (projectId: String, chatId: String, otherUserName: String) -> Unit = { _, _, _ -> },
    onNavigateToExpenseReview: (expenseId: String, notifTypeStr: String) -> Unit = { _, _ -> },
    onNavigateToExpenseChat: (expenseId: String) -> Unit = { _ -> },
    onLogout: () -> Unit,
    onNavigateToProfile: (UserRole) -> Unit = {},
    onNavigateToProjectReview: ((String) -> Unit)? = null, // For APPROVER role to review IN_REVIEW projects
    onNavigateToProjectDetail: ((String, Project?, String?, String?) -> Unit)? = null, // For USER role to navigate to project detail
    onNavigateToUserManagement: () -> Unit = {},
    projectViewModel: ProjectViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    BusinessHeadViewModel: BusinessHeadViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val iosPalette = rememberIosTabPalette()
    val projects by projectViewModel.projects.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.user
    val customerSelectionItems by authViewModel.customerSelectionItems.collectAsState()
    val currentCustomerId by authViewModel.currentCustomerId.collectAsState()
    
    // Get userRole directly from authState.user - this ensures we always have the correct role
    val userRole = currentUser?.role ?: UserRole.APPROVER // Fallback to APPROVER if user is null (shouldn't happen)
    val isManagerOrBH = userRole == UserRole.BUSINESS_HEAD || userRole == UserRole.MANAGER

    // Determine if user is ready for project loading
    val isUserReady = remember(authState.user, authState.user?.phone, userRole) {
        val user = authState.user
        when {
            user == null -> false
            userRole != UserRole.BUSINESS_HEAD && userRole != UserRole.MANAGER && user.phone.isNullOrEmpty() -> false
            else -> true
        }
    }
    val IosStyleFamily = FontFamily(
        Font(R.font.inter_extralight, FontWeight.Light),
        Font(R.font.inter_bold, FontWeight.Bold) // Use Bold file for iOS SemiBold/Bold look
    )
    
    // State for sign out confirmation dialog
    var showSignOutDialog by remember { mutableStateOf(false) }
    
    // State for notification popup dialog
    var showNotificationPopup by remember { mutableStateOf(false) }
    
    // Collect real unread notification count from NotificationViewModel
    val notifications by notificationViewModel.notifications.collectAsState()
    val unreadNotificationBadge by notificationViewModel.notificationBadge.collectAsState()
    
    // CRITICAL: Business name is now loaded in ViewModel (runs on IO dispatcher)
    // This prevents blocking Main thread and removes Firebase operations from Composable
    val businessName by projectViewModel.businessName.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated) {
            authViewModel.loadCustomerSelections()
        }
    }

    // Keep business name in sync with selected customer
    LaunchedEffect(currentCustomerId, userRole, currentUser?.uid) {
        val targetCustomerId = when {
            !currentCustomerId.isNullOrBlank() -> currentCustomerId
            userRole == UserRole.BUSINESS_HEAD -> currentUser?.uid
            else -> null
        }
        targetCustomerId?.let { projectViewModel.loadBusinessName(it) }
    }
    
    // Filter state
    var selectedFilter by remember { mutableStateOf("All PROJECTS") }
    var showFilterDropdown by remember { mutableStateOf(false) }
    var showCustomerDropdown by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showBusinessNameDialog by remember { mutableStateOf(false) }
    
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    
    val filterOptions = listOf("All PROJECTS", "IN REVIEW", "STANDBY", "LOCKED", "ACTIVE", "MAINTENANCE", "COMPLETED", "DECLINED", "ARCHIVE")

    // Pull-to-refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Load projects on initial screen load only (not when coming back)
    // CRITICAL: Wait for user data to be available (especially after OTP login)
    // This ensures phone number is loaded before attempting to fetch projects
    // Uses role-based fetching as per the guide
    LaunchedEffect(authState, userRole) {
        // Check if user is ready for project loading
        if (!isUserReady) {
            Log.d("BusinessHeadProjectsTab", "⏳ Waiting for user data to be ready (user: ${authState.user?.name}, phone: ${authState.user?.phone}, role: $userRole)...")
            authViewModel.refreshUserData()
            return@LaunchedEffect
        }
        
        // User is ready, proceed with loading projects
        val user = authState.user
        if (user == null) {
            Log.w("BusinessHeadProjectsTab", "⚠️ User is null despite isUserReady being true")
            return@LaunchedEffect
        }
        
        // Determine user ID based on role
        val userId = when (userRole) {
            UserRole.BUSINESS_HEAD -> user.uid
            else -> user.phone // MANAGER, APPROVER, USER all use phone
        }

        // Get customer ID
        val customerId = when (userRole) {
            UserRole.BUSINESS_HEAD -> currentCustomerId ?: user.uid
            UserRole.MANAGER -> currentCustomerId
            else -> currentCustomerId
        }

        Log.d("BusinessHeadProjectsTab", "🚀 Starting realtime listener: userId=$userId, role=$userRole, customerId=$customerId")
        projectViewModel.startRealtimeListenerByRole(userId, userRole, customerId)
        projectViewModel.loadAllPhaseRequests() // Load phase requests for notification badge
    }
    
    // Refresh function for pull-to-refresh
    // CRITICAL: Uses refreshProjectsByRole() which forces reload from Firebase
    // This is the ONLY place we force reload - user explicitly requested refresh
    // Uses role-based fetching as per the guide
    fun refreshData() {
        scope.launch {
            isRefreshing = true
            try {
                if (currentUser == null) {
                    Log.w("BusinessHeadProjectsTab", "⚠️ User not available, refreshing user data first")
                    authViewModel.refreshUserData()
                    kotlinx.coroutines.delay(300)
                    // Retry after user data is refreshed
                    val updatedUser = authState.user
                    if (updatedUser == null) {
                        Log.e("BusinessHeadProjectsTab", "❌ User still not available after refresh")
                        isRefreshing = false
                        return@launch
                    }
                }
                
                val user = currentUser ?: authState.user
                if (user == null) {
                    Log.e("BusinessHeadProjectsTab", "❌ User not available for refresh")
                    isRefreshing = false
                    return@launch
                }
                
                // Determine user ID based on role
                val userId = when (userRole) {
                    UserRole.BUSINESS_HEAD -> {
                        // For BUSINESS_HEAD, use email if available, otherwise use UID
                        user.email.ifEmpty { user.uid }
                    }
                    else -> {
                        // For other roles, use phone number
                        if (user.phone.isNullOrEmpty()) {
                            Log.w("BusinessHeadProjectsTab", "⚠️ User phone number is null or empty")
                            isRefreshing = false
                            return@launch
                        }
                        user.phone
                    }
                }
                
                // Get customer ID - for USER/APPROVER, we need to fetch ownerID from users collection
                val customerId = when (userRole) {
                    UserRole.BUSINESS_HEAD -> {
                        // For BUSINESS_HEAD, customer ID is their UID
                        currentCustomerId ?: user.uid
                    }
                    else -> {
                        currentCustomerId
                    }
                }
                
                Log.d("BusinessHeadProjectsTab", "🔄 Refreshing projects using role-based fetch: userId=$userId, role=$userRole, customerId=$customerId (will fetch from users collection if null)")
                projectViewModel.refreshProjectsByRole(userId, userRole, customerId)
                projectViewModel.loadAllPhaseRequests()
                
                val targetCustomerId = currentCustomerId ?: if (userRole == UserRole.BUSINESS_HEAD) user.uid else null
                targetCustomerId?.let { projectViewModel.loadBusinessName(it) }
                
                // Wait a bit for refresh to complete
                kotlinx.coroutines.delay(500)
                isRefreshing = false
            } catch (e: Exception) {
                Log.e("BusinessHeadProjectsTab", "❌ Error refreshing data: ${e.message}", e)
                isRefreshing = false
            }
        }
    }
    
    // Load notifications when screen opens
    LaunchedEffect(currentUser?.phone) {
        currentUser?.phone?.let { userId ->
            notificationViewModel.loadNotifications(userId)
        }
    }

    // Periodically refresh notifications so the badge updates in real-time
    // (local storage doesn't push changes, so we poll every 10 seconds while screen is active)
    LaunchedEffect(currentUser?.phone) {
        currentUser?.phone?.let { userId ->
            while (true) {
                kotlinx.coroutines.delay(10_000L) // wait 10 seconds between refreshes
                notificationViewModel.refreshFromLocalStorage(userId)
            }
        }
    }
    
    // Get phase requests for BUSINESS_HEAD
    val allPhaseRequests by projectViewModel.allPhaseRequests.collectAsState()
    val isLoadingPhaseRequests by projectViewModel.isLoadingPhaseRequests.collectAsState()
    
    // Filter declined projects for BUSINESS_HEAD
    val declinedProjects = remember(projects, userRole) {
        if (isManagerOrBH) {
            projects.filter {
                it.statusType == com.cubiquitous.tracura.model.ProjectStatus.DECLINED ||
                it.status == "REVIEW REJECTED" ||
                it.status == "DECLINED"
            }.sortedByDescending { it.updatedAt.seconds }
        } else {
            emptyList()
        }
    }

    // Filter IN_REVIEW projects for APPROVER and MANAGER
    val inReviewProjects = remember(projects, userRole) {
        if (userRole == UserRole.MANAGER) {
            projects.filter { 
                it.statusType == com.cubiquitous.tracura.model.ProjectStatus.IN_REVIEW ||
                it.status == "IN REVIEW"
            }.sortedByDescending { it.updatedAt.seconds }
        } else {
            emptyList()
        }
    }
    
    // Map to store user names for rejectedBy
    var rejectedByNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    
    // Fetch user names for rejectedBy fields
    LaunchedEffect(declinedProjects) {
        if (isManagerOrBH && declinedProjects.isNotEmpty()) {
            scope.launch {
                val namesMap = mutableMapOf<String, String>()
                val uniqueRejectedBy = declinedProjects.mapNotNull { it.rejectedBy }.distinct()
                
                uniqueRejectedBy.forEach { rejectedBy ->
                    if (!namesMap.containsKey(rejectedBy)) {
                        try {
                            val firestore = FirebaseFirestore.getInstance()
                            // Try to get user by document ID first
                            val userDoc = firestore.collection("users").document(rejectedBy).get().await()
                            if (userDoc.exists()) {
                                val userName = userDoc.getString("name") ?: "Unknown User"
                                namesMap[rejectedBy] = userName
                            } else {
                                // Try querying by phoneNumber field
                                val phoneQuery = firestore.collection("users")
                                    .whereEqualTo("phoneNumber", rejectedBy)
                                    .limit(1)
                                    .get()
                                    .await()
                                if (!phoneQuery.isEmpty) {
                                    val userName = phoneQuery.documents.first().getString("name") ?: "Unknown User"
                                    namesMap[rejectedBy] = userName
                                } else {
                                    namesMap[rejectedBy] = "Unknown User"
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("BusinessHeadProjectsTab", "Error fetching user name for $rejectedBy: ${e.message}")
                            namesMap[rejectedBy] = "Unknown User"
                        }
                    }
                }
                rejectedByNames = namesMap
            }
        }
    }
    
    // Sort projects by status priority: IN_REVIEW, Active, Locked, Maintenance, Completed, Suspended, Declined
    val statusPriority = mapOf(
        "IN REVIEW" to 0,
        "ACTIVE" to 1,
        "LOCKED" to 2,
        "MAINTENANCE" to 3,
        "COMPLETED" to 4,
        "SUSPENDED" to 5,
        "REVIEW REJECTED" to 6,
        "DRAFT" to 7
    )
    
    fun getProjectStatusPriority(project: Project): Int {
        return statusPriority[project.status] ?: 99
    }

    fun isFutureTempDelegationProject(project: Project): Boolean {
        if (userRole != UserRole.APPROVER) return false

        val normalizedPhone = currentUser?.phone?.replace("+91", "")?.trim()
        if (normalizedPhone.isNullOrBlank()) return false

        val hasPermanentAssignment = project.departmentApproverAssignments.values.any { assignedPhone ->
            assignedPhone.replace("+91", "").trim() == normalizedPhone
        }
        if (hasPermanentAssignment) return false

        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time

        return project.departmentTemporaryApprover.any { entry ->
            entry.phone.replace("+91", "").trim() == normalizedPhone &&
                entry.isAccepted &&
                entry.isActive &&
                entry.startDate != null &&
                entry.endDate != null &&
                today.before(entry.startDate)
        }
    }
    
    // Filter projects based on selected filter and search query
    val filteredProjects = remember(projects, selectedFilter, searchQuery, userRole, currentUser?.phone) {
        // First apply status filter
        val statusFiltered = when (selectedFilter) {
            "All PROJECTS" -> projects
            "IN REVIEW" -> projects.filter { it.status == "IN REVIEW" }
            "STANDBY" -> projects.filter { 
                it.isSuspended == true && 
                it.suspendedDate.isNullOrBlank() && 
                it.suspensionReason.isNullOrBlank()
            }
            "ACTIVE" -> projects.filter { it.status == "ACTIVE" || it.status == "DRAFT" }
            "COMPLETED" -> projects.filter { it.status == "COMPLETED" }
            "SUSPENDED" -> projects.filter { 
                it.isSuspended == true && !it.suspendedDate.isNullOrBlank() && try {
                    val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    dateFormatter.isLenient = false
                    val suspendedDate = dateFormatter.parse(it.suspendedDate)
                    if (suspendedDate != null) {
                        val today = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.time
                        val suspendedDateAtMidnight = java.util.Calendar.getInstance().apply {
                            time = suspendedDate
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.time
                        today.time <= suspendedDateAtMidnight.time
                    } else false
                } catch (e: Exception) { false }
            }
            "MAINTENANCE" -> projects.filter { 
                !it.maintenanceDate.isNullOrBlank() && try {
                    val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    dateFormatter.isLenient = false
                    val maintenanceDate = dateFormatter.parse(it.maintenanceDate)
                    if (maintenanceDate != null) {
                        val today = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.time
                        val maintenanceDateAtMidnight = java.util.Calendar.getInstance().apply {
                            time = maintenanceDate
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.time
                        today.time <= maintenanceDateAtMidnight.time
                    } else false
                } catch (e: Exception) { false }
            }
            "LOCKED" -> projects.filter { it.status == "LOCKED" }
            "DECLINED" -> projects.filter { it.status == "REVIEW REJECTED" }
            "ARCHIVE" -> projects.filter {
                if (!it.maintenanceDate.isNullOrBlank() && !it.handoverDate.isNullOrBlank()) {
                    try {
                        val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        dateFormatter.isLenient = false
                        val maintenanceDate = dateFormatter.parse(it.maintenanceDate)
                        val handoverDate = dateFormatter.parse(it.handoverDate)
                        
                        if (maintenanceDate != null && handoverDate != null) {
                            val today = java.util.Calendar.getInstance().apply {
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }.time
                            
                            val handoverDateAtMidnight = java.util.Calendar.getInstance().apply {
                                time = handoverDate
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }.time
                            
                            val maintenanceDateAtMidnight = java.util.Calendar.getInstance().apply {
                                time = maintenanceDate
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }.time
                            
                            val isHandoverDatePassed = today.time >= handoverDateAtMidnight.time
                            val isMaintenanceDatePassed = today.time > maintenanceDateAtMidnight.time
                            
                            if (isHandoverDatePassed && isMaintenanceDatePassed) {
                                val diffInMillis = today.time - maintenanceDateAtMidnight.time
                                val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)
                                diffInDays >= 30
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false
                }
            }
            else -> projects
        }
        
        // Then apply search filter if query is not empty
        val searchFiltered = if (searchQuery.isNotBlank()) {
            statusFiltered.filter { project ->
                project.name.contains(searchQuery, ignoreCase = true) ||
                project.description.contains(searchQuery, ignoreCase = true) ||
                project.client.contains(searchQuery, ignoreCase = true) ||
                project.location.contains(searchQuery, ignoreCase = true) ||
                (project.categories?.any { it.contains(searchQuery, ignoreCase = true) } == true)
            }
        } else {
            statusFiltered
        }
        
        // Sort by updatedAt descending (most recently updated first), but move future temp delegation projects to bottom
        searchFiltered.sortedWith(
            compareBy<Project> { if (isFutureTempDelegationProject(it)) 1 else 0 }
                .thenByDescending { it.updatedAt.seconds }
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(iosPalette.tier1Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
    ) {
        // Top Header BarfUSER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(iosPalette.tier1Background)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Left: Business Names
            /*Text(
                text = businessName,
                fontSize = 24.sp,
                fontFamily=IosStyleFamily,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            */

            /*Text(
                text = businessName,
                fontSize = 24.sp,
                fontFamily = IosStyleFamily,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 2,                 // 👈 break into 2 lines if needed
                softWrap = true,              // 👈 allows word wrapping
                overflow = TextOverflow.Ellipsis, // optional (remove if you want full text)
                modifier = Modifier
                    //.fillMaxWidth()           // 👈 important for proper wrapping
                    .align(Alignment.CenterStart)
            )*/

            // Right: Filter dropdown and Menu
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val selectedCustomerName = customerSelectionItems
                    .firstOrNull { it.customerId == currentCustomerId }
                    ?.businessName
                    ?: businessName
                val canSwitchCustomer = customerSelectionItems.size > 1

                Text(
                    if (selectedCustomerName.length > 21) selectedCustomerName.take(21) + "..." else selectedCustomerName,
                    fontSize = 22.sp,
                    fontFamily = IosStyleFamily,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.textPrimary,
                   // maxLines = 2,                 // 👈 break into 2 lines if needed
                    softWrap = true,              // 👈 allows word wrapping
                    //overflow = TextOverflow.Ellipsis, // optional (remove if you want full text)
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (selectedCustomerName.length > 21) {
                                Modifier.clickable { showBusinessNameDialog = true }
                            } else {
                                Modifier
                            }
                        )
                       // .align(Alignment.CenterStart)
                )
                if (canSwitchCustomer) {
                    IconButton(
                        onClick = { showCustomerDropdown = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Customer",
                            tint = iosPalette.accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (canSwitchCustomer) {
                    DropdownMenu(
                        expanded = showCustomerDropdown,
                        onDismissRequest = { showCustomerDropdown = false },
                        containerColor = iosPalette.tier2Surface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        customerSelectionItems.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = item.businessName,
                                            fontSize = 14.sp,
                                            color = iosPalette.textPrimary,
                                            fontWeight = if (item.customerId == currentCustomerId) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (item.businessType.isNotBlank()) {
                                            Text(
                                                text = item.businessType,
                                                fontSize = 12.sp,
                                                color = iosPalette.textSecondary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    showCustomerDropdown = false
                                    if (item.customerId == currentCustomerId) {
                                        return@DropdownMenuItem
                                    }

                                    val changed = authViewModel.selectCustomer(item.customerId)
                                    if (!changed) {
                                        return@DropdownMenuItem
                                    }

                                    val updatedUser = authViewModel.authState.value.user ?: return@DropdownMenuItem
                                    val updatedRole = updatedUser.role
                                    val userId = when (updatedRole) {
                                        UserRole.BUSINESS_HEAD -> updatedUser.uid
                                        else -> updatedUser.phone
                                    }

                                    if (userId.isNotBlank()) {
                                        projectViewModel.refreshProjectsByRole(userId, updatedRole, item.customerId)
                                        projectViewModel.loadAllPhaseRequests()
                                        projectViewModel.loadBusinessName(item.customerId)
                                    }
                                }
                            )
                        }
                    }
                }

                // Notification Icon with Badge
                Box {
                    IconButton(
                        onClick = {
                            try {
                                showNotificationPopup = true
                            } catch (e: Exception) {
                                android.util.Log.e("BusinessHeadProjectsTab", "Error showing notification popup: ${e.message}", e)
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.notification),
                            contentDescription = "Notifications",
                            tint = iosPalette.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Notification badge - only show if there are unread notifications
                    if (unreadNotificationBadge.count > 0 && unreadNotificationBadge.hasUnread) {
                        NotificationBadgeComponent(
                            badge = unreadNotificationBadge,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                }
                // Filter Dropdown (for BUSINESS_HEAD and MANAGER)
                if (isManagerOrBH) {
                    Box {
                        Surface(
                            modifier = Modifier
                                .clickable { showFilterDropdown = true },
                            color = iosPalette.tier2Surface,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (selectedFilter == "All PROJECTS") "All" else selectedFilter,
                                fontSize = 14.sp,
                                color = iosPalette.accent,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(1.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = iosPalette.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    
                    DropdownMenu(
                        expanded = showFilterDropdown,
                        onDismissRequest = { showFilterDropdown = false },
                        // Use containerColor instead of Modifier.background
                        containerColor = iosPalette.tier2Surface,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 0.dp, // This removes the "extra border" shadow
                        modifier = Modifier.border(1.dp, iosPalette.hairline, RoundedCornerShape(14.dp)) // Optional subtle border
                    //) {
                    ) {
                        filterOptions.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                modifier = Modifier
                                    .height(38.dp) // Force it much smaller than the default 48dp
                                    .fillMaxWidth(),
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                            //.offset(y=(-20).dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = option,
                                            modifier = Modifier.padding(end=85.dp),
                                            fontSize = 18.sp,
                                            color = iosPalette.textPrimary
                                        )
                                       // Spacer(modifier = Modifier.height(40.dp))
                                        /*if (option == selectedFilter) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF4285F4),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }*/
                                    }
                                },
                                onClick = {
                                    selectedFilter = option
                                    showFilterDropdown = false
                                },
                                contentPadding=PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            )
                            // Add divider after each item except the last one
                            if (index < filterOptions.size - 1) {
                               if (index == 0){
                                   HorizontalDivider(
                                       modifier = Modifier.fillMaxWidth(),
                                        color = iosPalette.hairline,
                                       thickness = 8.dp
                                    )
                                }else{
                                    HorizontalDivider(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = iosPalette.hairline,
                                        thickness = 2.dp
                                    )
                                }
                            }
                        }
                    }
                    }
                }
                
                /*// Notification Icon with Badge
                Box {
                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.notification),
                            contentDescription = "Notifications",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Notification badge
                    NotificationBadgeComponent(
                        badge = notificationBadge,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }*/
                
                // Hamburger Menu (Three lines) with unread notification badge
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = iosPalette.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

            // Project Count
            Text(
                text = "${filteredProjects.size} project${if (filteredProjects.size != 1) "s" else ""}",
                fontSize = 15.sp,
                fontWeight= FontWeight.Light,
                color = iosPalette.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        
        /*// Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            placeholder = {
                Text(
                    text = "Search projects...",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1976D2),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
        */
            // Define your reduced padding here
            val reducedPaddingValues = PaddingValues(horizontal = 1.dp, vertical = 1.dp)

            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(40.dp),
                singleLine = true,
                decorationBox = { innerTextField ->
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = searchQuery,
                        innerTextField = innerTextField,
                        enabled = true,
                        singleLine = true,
                        visualTransformation = VisualTransformation.None,
                        interactionSource = remember { MutableInteractionSource() },
                        placeholder = {
                            Text(
                                text = "Search projects...",
                                color = iosPalette.textSecondary.copy(alpha=0.7f),
                                fontSize = 16.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = iosPalette.textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = iosPalette.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        // REDUCE PADDING HERE
                        contentPadding = reducedPaddingValues,
                        container = {
                            OutlinedTextFieldDefaults.ContainerBox(
                                enabled = true,
                                isError = false,
                                interactionSource = remember { MutableInteractionSource() },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    disabledBorderColor = Color.Transparent,
                                    focusedContainerColor = iosPalette.tier2Surface,
                                    unfocusedContainerColor = iosPalette.tier2Surface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                focusedBorderThickness = 1.dp,
                                unfocusedBorderThickness = 1.dp
                            )
                        }
                    )
                }
            )

        
    // Projects List
    if (filteredProjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No projects found",
                        fontSize = 16.sp,
                        color = iosPalette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            // Retry using role-based fetching
                            val user = currentUser ?: authState.user
                            if (user != null) {
                                val userId = when (userRole) {
                                    UserRole.BUSINESS_HEAD -> user.email.ifEmpty { user.uid }
                                    else -> {
                                        if (user.phone.isNotEmpty()) {
                                            user.phone
                                        } else {
                                            Log.w("BusinessHeadProjectsTab", "⚠️ Retry: User phone number is null")
                                            authViewModel.refreshUserData()
                                            return@Button
                                        }
                                    }
                                }
                                val customerId = if (userRole == UserRole.BUSINESS_HEAD) (currentCustomerId ?: user.uid) else currentCustomerId
                                Log.d("BusinessHeadProjectsTab", "🔄 Retry: Starting realtime listener: userId=$userId, role=$userRole, customerId=$customerId")
                                projectViewModel.startRealtimeListenerByRole(userId, userRole, customerId)
                                projectViewModel.loadAllPhaseRequests()
                            } else {
                                Log.w("BusinessHeadProjectsTab", "⚠️ Retry: User not available, refreshing user data first")
                                authViewModel.refreshUserData()
                            }
                        }
                    ) {
                        Text("Retry")
                    }

                    if (projects.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val user = currentUser ?: authState.user
                                        val targetCustomerId = when {
                                            !currentCustomerId.isNullOrBlank() -> currentCustomerId
                                            userRole == UserRole.BUSINESS_HEAD -> user?.uid
                                            else -> null
                                        }

                                        if (targetCustomerId.isNullOrBlank()) {
                                            Toast.makeText(
                                                context,
                                                "Customer not selected. Please select a customer first.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@launch
                                        }

                                        val relationDocs = FirebaseFirestore.getInstance()
                                            .collection("CustomerUserRelation")
                                            .whereEqualTo("CustomerId", targetCustomerId)
                                            .whereEqualTo("isActive", true)
                                            .get()
                                            .await()

                                        val roles = relationDocs.documents.mapNotNull { it.getString("role") }
                                            .map { it.trim().uppercase() }
                                            .toSet()

                                        val hasUser = roles.contains("USER")
                                        val hasApprover = roles.contains("APPROVER")
                                        val hasManager = roles.contains("MANAGER")

                                        if (hasUser && hasApprover && hasManager) {
                                            onNavigateToNewProject()
                                        } else {
                                            val message = when {
                                                !hasUser && !hasApprover && !hasManager -> "Please create users, approvers, and account managers to create a project."
                                                !hasUser && !hasApprover -> "Please create users and approvers to create a project."
                                                !hasUser && !hasManager -> "Please create users and account managers to create a project."
                                                !hasApprover && !hasManager -> "Please create approvers and account managers to create a project."
                                                !hasUser -> "Please create at least one user to create a project."
                                                !hasApprover -> "Please create at least one approver to create a project."
                                                else -> "Please create at least one account manager to create a project."
                                            }
                                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("BusinessHeadProjectsTab", "Error validating CustomerUserRelation: ${e.message}", e)
                                        Toast.makeText(
                                            context,
                                            "Unable to validate users/approvers right now. Please try again.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        ) {
                            Text("Create New Project")
                        }
                    }
                }
            }
        } else {
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing),
                onRefresh = { refreshData() }
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProjects) { project ->
                        BusinessHeadProjectCardWithStatus(
                            project = project,
                            onClick = {
                                project.id?.let { projectId ->
                                    // Role-based navigation as per guide
                                    when (userRole) {
                                        UserRole.BUSINESS_HEAD -> {
                                            // BUSINESSHEAD: DECLINED -> Edit, Others -> Dashboard
                                            if (project.statusType == com.cubiquitous.tracura.model.ProjectStatus.DECLINED ||
                                                project.status == "REVIEW REJECTED" ||
                                                project.status == "DECLINED") {
                                                onNavigateToNewProjectForResubmission(projectId)
                                            } else {
                                                BusinessHeadViewModel.preloadProjectForEdit(project)
                                                onNavigateToProject(projectId, project, null, "")
                                            }
                                        }
                                        UserRole.MANAGER -> {
                                            // MANAGER: IN_REVIEW -> Review Sheet, Others -> Dashboard
                                            if (project.statusType == com.cubiquitous.tracura.model.ProjectStatus.IN_REVIEW ||
                                                project.status == "IN REVIEW") {
                                                onNavigateToProjectReview?.invoke(projectId) ?: run {
                                                    BusinessHeadViewModel.preloadProjectForEdit(project)
                                                    onNavigateToProject(projectId, project, null, "")
                                                }
                                            } else {
                                                BusinessHeadViewModel.preloadProjectForEdit(project)
                                                onNavigateToProject(projectId, project, null, "")
                                            }
                                        }
                                        UserRole.APPROVER -> {
                                            // APPROVER: navigate to dashboard for all statuses
                                            BusinessHeadViewModel.preloadProjectForEdit(project)
                                            onNavigateToProject(projectId, project, null, "")
                                        }
                                        UserRole.USER -> {
                                            // USER: IN_REVIEW/DECLINED -> Disabled (handled in card), Others -> Project Detail
                                            val isRestricted = project.statusType == com.cubiquitous.tracura.model.ProjectStatus.IN_REVIEW || 
                                                              project.statusType == com.cubiquitous.tracura.model.ProjectStatus.DECLINED ||
                                                              project.status == "IN REVIEW" ||
                                                              project.status == "REVIEW REJECTED" ||
                                                              project.status == "DECLINED"
                                            if (!isRestricted) {
                                                onNavigateToProjectDetail?.invoke(projectId, project, null, "") ?: run {
                                                    // Fallback to dashboard if detail callback not provided
                                                    BusinessHeadViewModel.preloadProjectForEdit(project)
                                                    onNavigateToProject(projectId, project, null, "")
                                                }
                                            }
                                        }
                                        UserRole.ADMIN -> {
                                            // ADMIN: Navigate to dashboard
                                            BusinessHeadViewModel.preloadProjectForEdit(project)
                                            onNavigateToProject(projectId, project, null, "")
                                        }
                                    }
                                }
                            },
                            onReviewClick = {
                                // Review button click for MANAGER + IN_REVIEW
                                project.id?.let { projectId ->
                                    onNavigateToProjectReview?.invoke(projectId)
                                }
                            },
                            onEditClick = { 
                                project.id?.let { projectId ->
                                    // Preload project data instantly for smooth navigation
                                    BusinessHeadViewModel.preloadProjectForEdit(project)
                                    // Navigate immediately - state will propagate during navigation
                                    onNavigateToEditProject(projectId)
                                }
                            },
                            userRole = userRole,
                            currentUserPhone = currentUser?.phone
                        )
                    }
                }
            }
        }
        }

        if (isManagerOrBH) { // Report FAB - Left aligned
            FloatingActionButton(
                onClick = onNavigateToOverallReports,
                containerColor = iosPalette.accent,
                contentColor = iosPalette.onAccent,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
                    .size(56.dp),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = "Reports",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
    
    // Menu Bottom Sheet
    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            containerColor = iosPalette.tier2Surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .fillMaxWidth()
                        .offset(y = (-30).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Menu",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosPalette.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Settings & Account",
                            fontSize = 18.sp,
                            fontWeight=FontWeight.Medium,
                            color = iosPalette.textSecondary
                        )
                    }
                    IconButton(onClick = { showMenu = false }) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(iosPalette.tier3Field),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = iosPalette.textSecondary
                            )
                            /*Text(

                                text = "✕",

                                style = TextStyle(

                                    fontSize = 22.sp,

                                    fontWeight = FontWeight.ExtraBold,

                                    color = Color(0xFF6C6969)

                                ),

                                )*/
                        }

                        /*Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Black
                        )*/
                    }
                }
               // Spacer(modifier = Modifier.height(16.dp))

                // Profile card
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = iosPalette.tier3Field,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMenu = false
                                onNavigateToProfile(userRole)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(iosPalette.tier2Surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = iosPalette.accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Profile",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosPalette.textPrimary
                            )
                            Text(
                                text = "View your account details",
                                fontSize = 15.sp,
                                color = iosPalette.textSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = iosPalette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

               // About card
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = iosPalette.tier3Field,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* About action can be wired later */ }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(iosPalette.tier2Surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = iosPalette.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "About",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosPalette.textPrimary
                            )
                            Text(
                                text = "App information & version",
                                fontSize = 15.sp,
                                color = iosPalette.textSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = iosPalette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Settings card
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = iosPalette.tier3Field,
                    shadowElevation = 2.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* Settings action can be wired later */ }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(iosPalette.tier2Surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = iosPalette.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Settings",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosPalette.textPrimary
                            )
                            Text(
                                text = "Preferences & configuration",
                                fontSize = 15.sp,
                                color = iosPalette.textSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = iosPalette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

//                // Notifications card with unread count
//                Surface(
//                    modifier = Modifier
//                        .padding(horizontal = 14.dp)
//                        .fillMaxWidth()
//                        .clip(RoundedCornerShape(16.dp)),
//                    color = Color.White,
//                    shadowElevation = 2.dp
//                ) {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .clickable {
//                                showMenu = false
//                                onNavigateToNotifications()
//                            }
//                            .padding(horizontal = 16.dp, vertical = 14.dp),
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(16.dp)
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .size(24.dp)
//                                .clip(CircleShape)
//                                .background(Color(0xFFE3F2FD)),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Icon(
//                                painter = painterResource(id = R.drawable.notification),
//                                contentDescription = null,
//                                tint = Color(0xFF1E88E5),
//                                modifier = Modifier.size(18.dp)
//                            )
//                        }
//                        Column(modifier = Modifier.weight(1f)) {
//                            Text(
//                                text = "Notifications",
//                                fontSize = 18.sp,
//                                fontWeight = FontWeight.SemiBold,
//                                color = Color.Black
//                            )
//                            Text(
//                                text = if (unreadNotificationBadge.count > 0)
//                                    "${unreadNotificationBadge.count} unread notification${if (unreadNotificationBadge.count != 1) "s" else ""}"
//                                else "No new notifications",
//                                fontSize = 15.sp,
//                                color = Color(0xFF6B6B6B)
//                            )
//                        }
//                        // Unread count badge pill
//                        if (unreadNotificationBadge.count > 0) {
//                            Box(
//                                modifier = Modifier
//                                    .background(
//                                        color = Color(0xFFE53935),
//                                        shape = RoundedCornerShape(12.dp)
//                                    )
//                                    .padding(horizontal = 8.dp, vertical = 2.dp),
//                                contentAlignment = Alignment.Center
//                            ) {
//                                Text(
//                                    text = if (unreadNotificationBadge.count > 99) "99+" else unreadNotificationBadge.count.toString(),
//                                    color = Color.White,
//                                    fontSize = 13.sp,
//                                    fontWeight = FontWeight.Bold
//                                )
//                            }
//                        }
//                        Icon(
//                            imageVector = Icons.Default.KeyboardArrowRight,
//                            contentDescription = null,
//                            tint = Color(0xFFBDBDBD)
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(20.dp))

                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    color = iosPalette.hairline,
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Sign Out
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .fillMaxWidth()
                        .clickable {
                            showSignOutDialog = true
                            showMenu = false
                        },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(width = 0.5.dp, color = iosPalette.danger.copy(alpha = 0.5f)),
                    color = iosPalette.dangerContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        /*Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935)),
                            contentAlignment = Alignment.Center
                        ){*/
                        Icon(
                            painter = painterResource(id = R.drawable.signout),
                            contentDescription = "Logout",
                            tint = iosPalette.danger,
                            modifier = Modifier.size(32.dp)
                        )

                    //}
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sign Out",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                //color = Color.Red,
                                color = iosPalette.danger
                            )
                            Text(
                                text = "Logout from your account",
                                fontSize = 15.sp,
                                color = iosPalette.textSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = iosPalette.danger.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
    
    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        val iosPalette = rememberIosTabPalette()
        Dialog(onDismissRequest = { showSignOutDialog = false }) {
            // Use a Surface to define the background, shape, and "look" of the dialog
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iosPalette.tier2Surface,
                modifier = Modifier
                    .fillMaxWidth(0.85f) // Adjust width to 85% of screen
                    .wrapContentHeight() // This is key: it makes the height shrink to fit
            ) {
                Column(
                    modifier = Modifier.padding(0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign Out",
                        modifier = Modifier.padding(top=14.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Are you sure you want to sign out of your account?",
                        modifier = Modifier.padding(horizontal=14.dp),
                        fontSize = 18.sp,
                        color = iosPalette.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = iosPalette.hairline,
                        thickness = 1.dp
                    )

                    // Buttons row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp), // Fixed height for button row
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Cancel button
                        TextButton(
                            onClick = { showSignOutDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Text(
                                text = "Cancel",
                                color = iosPalette.accent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Vertical divider between buttons - extends full height
                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp),
                            color = iosPalette.hairline,
                            thickness = 1.dp
                        )
                        
                        // Sign Out button
                        TextButton(
                            onClick = {
                                showSignOutDialog = false
                                scope.launch {
                                    authViewModel.performLogout()
                                    onLogout()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Text(
                                text = "Sign Out",
                                color = iosPalette.danger,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Business Name Dialog
    if (showBusinessNameDialog) {
        val iosPalette = rememberIosTabPalette()
        Dialog(onDismissRequest = { showBusinessNameDialog = false }) {
            // Use a Surface to define the background, shape, and "look" of the dialog
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iosPalette.tier2Surface,
                modifier = Modifier
                    .fillMaxWidth(0.85f) // Adjust width to 85% of screen
                    .wrapContentHeight() // This is key: it makes the height shrink to fit
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Business Name",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = businessName,
                        fontSize = 18.sp,
                        color = iosPalette.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = iosPalette.hairline,
                        thickness = 1.dp
                    )

                    TextButton(
                        onClick = { showBusinessNameDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "OK",
                            color = iosPalette.accent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
    
    // Notification Popup Dialog
    if (showNotificationPopup) {
        NotificationPopupDialog(
            notifications = notifications,
            declinedProjects = declinedProjects,
            inReviewProjects = inReviewProjects,
            phaseRequests = allPhaseRequests,
            rejectedByNames = rejectedByNames,
            userRole = userRole,
            isLoadingPhaseRequests = isLoadingPhaseRequests,
            onDismiss = { showNotificationPopup = false },
            onNotificationClick = { notification ->
                // Handle notification click — mirrors AppNavHost notification deep-link logic
                try {
                    notificationViewModel.markNotificationAsRead(notification.id)
                    showNotificationPopup = false

                    val navTarget = notification.navigationTarget
                    val notificationType = notification.type.name
                    val notifType = notification.type
                    val notificationMessage = notification.message
                    val projectId = notification.projectId  // non-nullable String, may be ""
                    val relatedId = notification.relatedId  // chatId or expenseId
                    val notificationTitle = notification.title
                    val notifTypeStr = buildExpenseListNotificationTag(
                        notificationType = notificationType,
                        navigationTarget = navTarget,
                        notifType = notifType,
                        title = notification.title,
                        message = notificationMessage
                    )
                    android.util.Log.d("BHProjectsTab", "notifyTypeStr is '$notifTypeStr' for message '${notification.message}' type '${notificationType}' and navTarget '$navTarget' and title '${notification.title}'")

                    when {
                        // ── EXPENSE CHAT notifications → Expense Chat screen (check FIRST) ──
                        notificationTitle.contains("New expense chat msg", ignoreCase = true) ||
                        (notifType == com.cubiquitous.tracura.model.NotificationType.CHAT_MESSAGE &&
                         (notificationMessage.lowercase().contains("expense chat") ||
                          notification.message.lowercase().contains("logged"))) -> {
                            if (relatedId.isNotEmpty()) {
                                android.util.Log.d("BHProjectsTab", "🚀 Navigating to Expense Chat → expenseId='$relatedId'")
                                onNavigateToExpenseChat(relatedId)
                            } else if (projectId.isNotEmpty()) {
                                android.util.Log.d("BHProjectsTab", "⚠️ No expenseId for expense chat, falling back to project dashboard")
                                onNavigateToProject(projectId, null, notifTypeStr, notificationMessage) // fallback to project dashboard with notification context
                            }
                        }

                        // ── CHAT notifications → Chat screen ──────────────────────────
                        notifType == com.cubiquitous.tracura.model.NotificationType.CHAT_MESSAGE ||
                        navTarget.startsWith("chat/", ignoreCase = true) ||
                        navTarget.contains("chat_detail", ignoreCase = true) ||
                        navTarget.contains("chat_screen", ignoreCase = true) ||
                        navTarget.contains("customer_chat", ignoreCase = true) -> {
                            // Parse chatId from navigationTarget "chat/{projectId}/{chatId}/{name}"
                            val chatProjectId: String
                            val chatId: String
                            val chatUserName: String
                            if (navTarget.startsWith("chat/", ignoreCase = true)) {
                                val parts = navTarget.split("/")
                                var sender = notificationMessage.split(" ")
                                chatProjectId = parts.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: projectId
                                chatId = parts.getOrNull(2)?.takeIf { it.isNotEmpty() } ?: relatedId
                                chatUserName = sender.getOrNull(0) ?: notification.title
                            } else {
                                chatProjectId = projectId
                                chatId = relatedId
                                chatUserName = notification.title
                            }
                            if (chatProjectId.isNotEmpty() && chatId.isNotEmpty()) {
                                android.util.Log.w("BHProjectsTab", "🚀 CALLING onNavigateToChat → projectId='$chatProjectId' chatId='$chatId' name='$chatUserName'")
                                onNavigateToChat(chatProjectId, chatId, chatUserName)
                            } else if (projectId.isNotEmpty()) {
                                onNavigateToProject(projectId, null, notifTypeStr, notificationMessage) // fallback to project dashboard with notification context
                            }
                        }

                        // ── EXPENSE REVIEW/STATUS notifications → Expense Review screen ─
                        notifType == com.cubiquitous.tracura.model.NotificationType.EXPENSE_SUBMITTED ||
                        notifType == com.cubiquitous.tracura.model.NotificationType.EXPENSE_APPROVED ||
                        notifType == com.cubiquitous.tracura.model.NotificationType.EXPENSE_REJECTED ||
                        navTarget.contains("expense_detail", ignoreCase = true) ||
                        navTarget.contains("expense_review", ignoreCase = true) ||
                        notificationMessage.contains("your expense", ignoreCase = true) || 
                        (notificationMessage.contains("payment", ignoreCase = true) && 
                        notificationMessage.contains("updated", ignoreCase = true) || 
                        notificationMessage.contains("completed", ignoreCase = true))-> {
                            if (relatedId.isNotEmpty()) {
                                android.util.Log.d("BHProjectsTab", "🚀 Navigating to Expense Review → expenseId='$relatedId'")
                                onNavigateToExpenseReview(relatedId, notifTypeStr)
                            } else if (projectId.isNotEmpty()) {
                                android.util.Log.d("BHProjectsTab", "⚠️ No expenseId for expense review, falling back to project dashboard")
                                onNavigateToProject(projectId, null, notifTypeStr, notificationMessage) // pass notification message for contextual banner in project dashboard
                            }
                        }

                        // ── PENDING APPROVAL notifications → project dashboard ─────────
                        notifType == com.cubiquitous.tracura.model.NotificationType.PENDING_APPROVAL ||
                        navTarget.contains("pending_approval", ignoreCase = true) -> {
                            onNavigateToProject(projectId, null, notifTypeStr, notificationMessage) // pass notification message for contextual banner in project dashboard
                        }

                        // ── Everything else → project dashboard (original behaviour) ──
                        else -> {
                            onNavigateToProject(projectId, null, notifTypeStr, notificationMessage) // pass notification message for contextual banner in project dashboard
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BusinessHeadProjectsTab", "Error handling notification click: ${e.message}", e)
                }
            },
            onDeclinedProjectClick = { projectId ->
                // Navigate to edit project for resubmission
                showNotificationPopup = false
                onNavigateToNewProjectForResubmission(projectId)
            },
            onInReviewProjectClick = { projectId ->
                // Navigate to project review for APPROVER
                showNotificationPopup = false
                onNavigateToProjectReview?.invoke(projectId) ?: run {
                    // Fallback to project dashboard
                    onNavigateToProject(projectId, null, "project_review", "")
                }
            },
            onPhaseRequestClick = { projectId ->
                // Navigate to project dashboard
                showNotificationPopup = false
                onNavigateToProject(projectId, null, "phase_update", "")
            },
            onViewAllClick = {
                showNotificationPopup = false
                onNavigateToNotifications()
            },
            onMarkAsRead = { notificationId ->
                notificationViewModel.markNotificationAsRead(notificationId)
            }
        )
    }
}

@Composable
fun BusinessHeadProjectCardWithStatus(
    project: Project,
    onClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onReviewClick: (() -> Unit)? = null, // For APPROVER + IN_REVIEW review button
    userRole: UserRole,
    currentUserPhone: String? = null
) {
    val iosPalette = rememberIosTabPalette()
    // State for project name dialog
    var showProjectNameDialog by remember { mutableStateOf(false) }

    val displayStatus = project.status

    // Determine actual ProjectStatus from displayStatus (use statusType helper)
    val actualStatus = when {
        displayStatus == "IN REVIEW" -> com.cubiquitous.tracura.model.ProjectStatus.IN_REVIEW
        displayStatus == "REVIEW REJECTED" || displayStatus == "DECLINED" -> com.cubiquitous.tracura.model.ProjectStatus.DECLINED
        displayStatus == "STANDBY" -> com.cubiquitous.tracura.model.ProjectStatus.STANDBY
        displayStatus == "MAINTENANCE" -> com.cubiquitous.tracura.model.ProjectStatus.MAINTENANCE
        displayStatus == "ARCHIVED" -> com.cubiquitous.tracura.model.ProjectStatus.ARCHIVE
        else -> project.statusType // Use computed statusType from Project model
    }

    val isDraft = actualStatus == com.cubiquitous.tracura.model.ProjectStatus.DRAFT
    val isLocked = actualStatus == com.cubiquitous.tracura.model.ProjectStatus.LOCKED
    val isInReview = actualStatus == com.cubiquitous.tracura.model.ProjectStatus.IN_REVIEW
    val isDeclined = actualStatus == com.cubiquitous.tracura.model.ProjectStatus.DECLINED || 
                     displayStatus == "REVIEW REJECTED" || 
                     displayStatus == "DECLINED"
    val isCompleted = actualStatus == com.cubiquitous.tracura.model.ProjectStatus.COMPLETED
    val isArchived = actualStatus == com.cubiquitous.tracura.model.ProjectStatus.ARCHIVE
    val isSuspended = actualStatus == com.cubiquitous.tracura.model.ProjectStatus.SUSPENDED || 
                      displayStatus == "SUSPENDED" || 
                      project.status == "SUSPENDED"
    val isMaintenance = actualStatus == com.cubiquitous.tracura.model.ProjectStatus.MAINTENANCE
    val isStandby = actualStatus == com.cubiquitous.tracura.model.ProjectStatus.STANDBY ||
                    (isSuspended && project.suspendedDate.isNullOrBlank() && project.suspensionReason.isNullOrBlank())

    // Use ProjectStatus helper functions for colors (as per guide)
    val statusColor = actualStatus.getColor()
    val statusBgColor = statusColor.copy(alpha = 0.12f)
    
    val normalizedCurrentUserPhone = remember(currentUserPhone) {
        currentUserPhone?.replace("+91", "")?.trim()
    }

    val hasPermanentApproverAssignment = remember(project.departmentApproverAssignments, normalizedCurrentUserPhone) {
        if (normalizedCurrentUserPhone.isNullOrBlank()) {
            false
        } else {
            project.departmentApproverAssignments.values.any { assignedPhone ->
                assignedPhone.replace("+91", "").trim() == normalizedCurrentUserPhone
            }
        }
    }

    val futureTempApproverWindow = remember(project.departmentTemporaryApprover, normalizedCurrentUserPhone) {
        if (normalizedCurrentUserPhone.isNullOrBlank()) {
            null
        } else {
            val today = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time

            project.departmentTemporaryApprover
                .asSequence()
                .filter { entry ->
                    entry.phone.replace("+91", "").trim() == normalizedCurrentUserPhone &&
                        entry.isAccepted &&
                        entry.isActive &&
                        entry.startDate != null &&
                        entry.endDate != null
                }
                .mapNotNull { entry ->
                    val startDate = entry.startDate ?: return@mapNotNull null
                    val endDate = entry.endDate ?: return@mapNotNull null

                    val startAtMidnight = java.util.Calendar.getInstance().apply {
                        time = startDate
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.time
                    val endAtMidnight = java.util.Calendar.getInstance().apply {
                        time = endDate
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.time

                    val isWithinWindow = !today.before(startAtMidnight) && !today.after(endAtMidnight)
                    val isFutureWindow = today.before(startAtMidnight)

                    Triple(startAtMidnight, endAtMidnight, isWithinWindow to isFutureWindow)
                }
                .sortedBy { it.first.time }
                .firstOrNull()
        }
    }

    val isTempApproverFutureRestricted =
        userRole == UserRole.APPROVER &&
            !hasPermanentApproverAssignment &&
            (futureTempApproverWindow?.third?.second == true)

    val tempApproverFutureMessage = remember(futureTempApproverWindow) {
        val window = futureTempApproverWindow ?: return@remember null
        val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        "This project starts for you from ${formatter.format(window.first)} to ${formatter.format(window.second)}"
    }

    // Role-based interaction rules (as per guide)
    val isRestrictedForUser = (userRole == UserRole.USER) && 
                              (isInReview || isDeclined)
    val showReviewButton = (userRole == UserRole.MANAGER) && isInReview
    val isClickable = !isRestrictedForUser && !isTempApproverFutureRestricted

    // Format suspension date for display (only for actual suspensions with reason, not standby)
    val suspendedDateDisplay =
        if (isSuspended && !isStandby && !project.suspendedDate.isNullOrBlank()) {
            try {
                val inputFormatter =
                    java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val outputFormatter =
                    java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                val date = inputFormatter.parse(project.suspendedDate)
                date?.let { outputFormatter.format(it) }
            } catch (e: Exception) {
                project.suspendedDate
            }
        } else {
            null
        }

    // Use ProjectStatus helper function for display text (as per guide)
    val statusText = actualStatus.getDisplayText()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isRestrictedForUser || isTempApproverFutureRestricted) 0.6f else 1.0f)
            .clickable(enabled = isClickable) {
                if (isClickable) onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        border = BorderStroke(1.dp, iosPalette.hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Project code, Project name and Status badge with Edit icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                //Column for Project Code and Project Name
                Row(
                    modifier = Modifier.weight(1f),
                    // horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Project Code with sky blue highlight and border
                    if (project.projectCode.isNotBlank()) {
                        Surface(
                            // color = Color(0xFFE3F2FD), // Sky blue background
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF2196F3)) // Sky blue border
                        ) {
                            Text(
                                text = project.projectCode,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2), // Darker blue for text
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    
                    val truncatedName = if (project.name.length > 24) {
                        project.name.take(24) + "..."
                    } else {
                        project.name
                    }
                    val isTruncated = project.name.length > 24

                    Text(
                        text = truncatedName,
                        maxLines = 1,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosPalette.textPrimary,
                        modifier = Modifier
                            .then(
                                if (isTruncated) {
                                    Modifier.clickable { showProjectNameDialog = true }
                                } else {
                                    Modifier
                                }
                            )
                    )
                }

                // Status badge and edit icon
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    //verticalAlignment = Alignment.Top
                ) {
                    // Status badge
                    Surface(
                        color = statusBgColor,
                        modifier = Modifier.heightIn(min = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.1f))
                    ) {
                        if (isSuspended && suspendedDateDisplay != null) {
                            // For suspended projects, show status and date vertically inside badge
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = statusText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = statusColor,
                                        modifier = Modifier.offset(y = (-1).dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = suspendedDateDisplay,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = iosPalette.warning,
                                    modifier = Modifier.padding(start = 14.dp)
                                )
                            }
                        } else {
                            // For non-suspended projects, show status horizontally
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = statusText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = statusColor,
                                    modifier = Modifier.offset(y = (-1).dp)
                                )
                            }
                        }
                    }

                    // Review button for MANAGER + IN_REVIEW
                    if (showReviewButton && onReviewClick != null) {
                        IconButton(
                            onClick = { onReviewClick() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Review Project",
                                tint = iosPalette.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // Edit button for BUSINESSHEAD and MANAGER (hide for DECLINED - DECLINED navigates to edit on click)
                    if ((userRole == UserRole.BUSINESS_HEAD || userRole == UserRole.MANAGER) && !isDeclined) {
                        IconButton(
                            onClick = { onEditClick() },
                            modifier = Modifier
                                .background(
                                    color = iosPalette.tier3Field,
                                    shape = CircleShape
                                )
                                .size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Project",
                                tint = iosPalette.accent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Row 2: Description (if exists)
            if (project.description.isNotEmpty()) {
                Text(
                    text = project.description,
                    fontSize = 13.sp,
                    color = iosPalette.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Row 3: Budget and Team members
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Budget
                if (userRole != UserRole.USER){
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp) // Total size of the circular background
                                .background(
                                    color = iosPalette.tier3Field,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center // Centers the icon inside the circle
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyRupee,
                                contentDescription = null,
                                tint = iosPalette.accent,
                                modifier = Modifier.size(10.dp) // Icon size inside the circle
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = FormatUtils.formatCurrency(project.budget),
                            fontSize = 13.sp,
                            color = iosPalette.textSecondary
                        )
                    }

                }
                Spacer(modifier = Modifier.weight(1f))

                // Location
                if (!project.location.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.client),
                            contentDescription = null,
                            tint = iosPalette.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = project.location,
                            fontSize = 12.sp,
                            color = iosPalette.textSecondary
                        )
                    }
                }
            }

            // Row 4: Suspension reason (if exists)
            if (isSuspended && !project.suspensionReason.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = iosPalette.warning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = project.suspensionReason,
                        fontSize = 12.sp,
                        color = iosPalette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Rejection reason for DECLINED status (as per guide)
            if (isDeclined && !project.rejectionReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        tint = iosPalette.danger,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = project.rejectionReason,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = iosPalette.danger,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (isTempApproverFutureRestricted && !tempApproverFutureMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = iosPalette.warning,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tempApproverFutureMessage,
                        fontSize = 12.sp,
                        color = iosPalette.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 5: Location and Client (if exists)
            if (!project.location.isNullOrBlank() || !project.client.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Team members
                    if (project.teamMembers.size > 0){
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp) // background size
                                    .background(
                                        color = iosPalette.tier3Field,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = iosPalette.accent,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${project.teamMembers.size} member${if (project.teamMembers.size != 1) "s" else ""}",
                                fontSize = 13.sp,
                                color = iosPalette.textSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    // Client name
                    if (!project.client.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.client1),
                                contentDescription = null,
                                tint = iosPalette.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = project.client,
                                fontSize = 12.sp,
                                color = iosPalette.textSecondary
                            )
                        }
                    }
                }
            }


            // Row 6: Date range
            // Use plannedDate from Firebase (mapped from "plannedDate" field) instead of startDate
            // Use handoverDate from Firebase instead of endDate
            val formattedDateRange = remember(project.plannedDate, project.handoverDate) {
                // Use plannedDate if available, otherwise fallback to startDate for backward compatibility
                val startDateValue = project.plannedDate ?: project.startDate
                // Use handoverDate instead of endDate
                val endDateValue = project.handoverDate ?: project.endDate
                
                if (!startDateValue.isNullOrBlank()) {

                    val formatter = java.text.SimpleDateFormat(
                        "dd/MM/yyyy",
                        java.util.Locale.getDefault()
                    )

                    try {
                        val startDate = formatter.parse(startDateValue)

                        if (!endDateValue.isNullOrBlank()) {
                            val endDate = formatter.parse(endDateValue)

                            if (startDate != null && endDate != null) {
                                "${formatter.format(startDate)} - ${formatter.format(endDate)}"
                            } else {
                                "$startDateValue - $endDateValue"
                            }
                        } else {
                            startDate?.let { formatter.format(it) } ?: startDateValue
                        }

                    } catch (e: Exception) {
                        if (!endDateValue.isNullOrBlank()) {
                            "$startDateValue - $endDateValue"
                        } else {
                            startDateValue
                        }
                    }
                } else {
                    null
                }
            }

            if (formattedDateRange != null) {
                // Days remaining based on status-specific deadline:
                // ACTIVE -> handoverDate, MAINTENANCE -> maintenanceDate, PLANNED -> plannedDate
                // COMPLETED and SUSPENDED do not show days remaining.
                val daysRemainingInfo = remember(
                    project.handoverDate,
                    project.maintenanceDate,
                    project.plannedDate,
                    project.startDate,
                    project.status,
                    project.statusType,
                    isMaintenance,
                    isCompleted,
                    isSuspended,
                ) {
                    if (isCompleted || isSuspended) {
                        null
                    } else {
                        val isPlanned = project.status.equals("PLANNED", ignoreCase = true)
                        val isActive = project.status.equals("ACTIVE", ignoreCase = true) ||
                            project.statusType == com.cubiquitous.tracura.model.ProjectStatus.ACTIVE

                        val deadlineType = when {
                            isMaintenance -> "MAINTENANCE"
                            isPlanned -> "PLANNED"
                            isActive -> "HANDOVER"
                            else -> null
                        }

                        val deadlineDateValue = when (deadlineType) {
                            "MAINTENANCE" -> project.maintenanceDate
                            "PLANNED" -> project.plannedDate ?: project.startDate
                            "HANDOVER" -> project.handoverDate
                            else -> null
                        }

                        if (deadlineType == null || deadlineDateValue.isNullOrBlank()) {
                            null
                        } else {
                            try {
                                val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                val deadlineDate = formatter.parse(deadlineDateValue)

                                if (deadlineDate != null) {
                                    val today = java.util.Calendar.getInstance().apply {
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                    }.time

                                    val deadlineAtMidnight = java.util.Calendar.getInstance().apply {
                                        time = deadlineDate
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                    }.time

                                    val diffInDays = ((deadlineAtMidnight.time - today.time) / (1000 * 60 * 60 * 24)).toInt()
                                    val daysColor = when {
                                        diffInDays == 3 -> iosPalette.warning
                                        diffInDays == 0 && deadlineType == "PLANNED" -> Color(0xFF2E7D32)
                                        diffInDays == 0 -> iosPalette.danger
                                        else -> iosPalette.textSecondary
                                    }
                                    diffInDays to daysColor
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (daysRemainingInfo != null) {
                        Text(
                            text = "${daysRemainingInfo.first} days left",
                            fontSize = 12.sp,
                            color = daysRemainingInfo.second
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = iosPalette.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedDateRange,
                        fontSize = 12.sp,
                        color = iosPalette.textSecondary
                    )
                }
            }
        }

        // Project Name Dialog
        /*if (showProjectNameDialog) {
            Dialog(onDismissRequest = { showProjectNameDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Status with dot (e.g., MAINTENANCE with purple dot)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = statusText.uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFE5E5EA),
                            thickness = 1.dp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Project Name label
                        Text(
                            text = "Project Name",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Full project name
                        Text(
                            text = project.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }*/
        if (showProjectNameDialog) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, 120), // Adjust Y to move it above or below
                onDismissRequest = { showProjectNameDialog = false },
                properties = PopupProperties(focusable = true)
            ) {
                // The actual Popover UI (ProjectNamePopoverView equivalent)
                Box(
                    modifier = Modifier
                        .width(IntrinsicSize.Max) // Ensures children define the width
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .background(iosPalette.tier2Surface, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.width(IntrinsicSize.Max)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                                    //.padding(bottom=8.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText.uppercase(),
                                color = iosPalette.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                /*modifier = Modifier.padding(bottom = 4.dp)*/
                            )
                        }

                        // REMOVED .fillMaxWidth() here
                        HorizontalDivider(
                            color = iosPalette.hairline,
                            thickness = 1.dp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Project Name",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = iosPalette.textSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = project.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
