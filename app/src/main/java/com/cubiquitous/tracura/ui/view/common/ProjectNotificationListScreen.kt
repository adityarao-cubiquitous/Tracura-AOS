package com.cubiquitous.tracura.ui.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.animation.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.Notification
import com.cubiquitous.tracura.model.NotificationType
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.viewmodel.NotificationViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.viewmodel.ProjectViewModel
import com.cubiquitous.tracura.ui.common.ProminentNotificationBadge
import com.cubiquitous.tracura.ui.view.businesshead.BusinessHeadProjectCardWithStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log
import androidx.compose.ui.res.painterResource
import com.cubiquitous.tracura.R
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight as TextFontWeight
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.navigation.buildExpenseListNotificationTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectNotificationListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProject: (String, String, String) -> Unit,
    onNavigateToPendingApprovals: (String) -> Unit,
    onNavigateToNewProjectForResubmission: (String) -> Unit, // New parameter for resubmission
    onViewAllNotifications: () -> Unit = {}, // New parameter for viewing all notifications
    onNavigateToChat: (String, String, String) -> Unit = { _, _, _ -> }, // projectId, chatId, otherUserName
    currentUserId: String,
    userRole: String,
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    projectViewModel: ProjectViewModel = hiltViewModel()
) {
    val notifications by notificationViewModel.notifications.collectAsState()
    val isLoading by notificationViewModel.isLoading.collectAsState()
    val error by notificationViewModel.error.collectAsState()
    val notificationBadge by notificationViewModel.notificationBadge.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val projects by projectViewModel.projects.collectAsState()
    val isLoadingProjects by projectViewModel.isLoading.collectAsState()
    
    // Get role from authState as fallback if userRole parameter is empty
    // Use both string and enum comparison for robustness
    val effectiveUserRole = remember(userRole, authState.user?.role) {
        if (userRole.isNotEmpty()) {
            userRole
        } else {
            authState.user?.role?.name ?: ""
        }
    }

    fun notificationTag(notification: Notification): String = buildExpenseListNotificationTag(
        notificationType = notification.type.name,
        navigationTarget = notification.navigationTarget,
        notifType = notification.type,
        title = notification.title,
        message = notification.message
    )
    
    // Determine if user is Production Head - use multiple checks for robustness
    val isBusinessHead = remember(effectiveUserRole, authState.user?.role) {
        effectiveUserRole == "BUSINESS_HEAD" || 
        authState.user?.role == com.cubiquitous.tracura.model.UserRole.BUSINESS_HEAD
    }
    
    // State for showing bottom sheet with all notifications
    var showAllNotificationsSheet by remember { mutableStateOf(false) }
    
    // State for showing bottom sheets with all phase requests and declined projects
    var showAllPhaseRequestsSheet by remember { mutableStateOf(false) }
    var showAllDeclinedProjectsSheet by remember { mutableStateOf(false) }
    
    // Business name state for Business Head background
    var businessName by remember { mutableStateOf("Business") }
    val scope = rememberCoroutineScope()
    
    // Fetch business name from customer data for Business Head
    LaunchedEffect(isBusinessHead) {
        if (isBusinessHead) {
            scope.launch {
                try {
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    if (firebaseUser != null) {
                        val customerId = firebaseUser.uid
                        val firestore = FirebaseFirestore.getInstance()
                        val customerDoc = firestore.collection("customers")
                            .document(customerId)
                            .get()
                            .await()
                        
                        if (customerDoc.exists()) {
                            val fetchedBusinessName = customerDoc.getString("businessName")
                            if (!fetchedBusinessName.isNullOrBlank()) {
                                businessName = fetchedBusinessName
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ProjectNotificationListScreen", "Error fetching business name: ${e.message}")
                }
            }
        }
    }
    
    // For Production Head: Show DECLINED projects (check both DECLINED and REVIEW REJECTED status)
    val declinedProjects = remember(projects, isBusinessHead) {
        if (isBusinessHead) {
            val filtered = projects.filter { 
                val status = it.status?.trim() ?: ""
                val isDeclined = status.equals("DECLINED", ignoreCase = true) || 
                                status.equals("REVIEW REJECTED", ignoreCase = true)
                isDeclined
            }.sortedByDescending { it.updatedAt?.seconds ?: 0L }
            filtered
        } else {
            emptyList()
        }
    }
    
    // Phase requests from ProjectViewModel
    val allPhaseRequests by projectViewModel.allPhaseRequests.collectAsState()
    val isLoadingPhaseRequests by projectViewModel.isLoadingPhaseRequests.collectAsState()
    
    // Map to store user names for rejectedBy
    val rejectedByNames = remember { mutableStateMapOf<String, String>() }
    
    // Fetch user names for rejectedBy
    LaunchedEffect(declinedProjects) {
        declinedProjects.forEach { project ->
            val rejectedBy = project.rejectedBy
            if (!rejectedBy.isNullOrBlank() && !rejectedByNames.containsKey(rejectedBy)) {
                scope.launch {
                    try {
                        val firestore = FirebaseFirestore.getInstance()
                        // Try to get user by document ID first
                        val userDoc = firestore.collection("users").document(rejectedBy).get().await()
                        if (userDoc.exists()) {
                            val userName = userDoc.getString("name") ?: "Unknown User"
                            rejectedByNames[rejectedBy] = userName
                        } else {
                            // Try by phone number
                            val phoneQuery = firestore.collection("users")
                                .whereEqualTo("phoneNumber", rejectedBy)
                                .limit(1)
                                .get()
                                .await()
                            if (!phoneQuery.isEmpty) {
                                val userName = phoneQuery.documents.first().getString("name") ?: "Unknown User"
                                rejectedByNames[rejectedBy] = userName
                            } else {
                                // Try by uid
                                val uidQuery = firestore.collection("users")
                                    .whereEqualTo("uid", rejectedBy)
                                    .limit(1)
                                    .get()
                                    .await()
                                if (!uidQuery.isEmpty) {
                                    val userName = uidQuery.documents.first().getString("name") ?: "Unknown User"
                                    rejectedByNames[rejectedBy] = userName
                                } else {
                                    rejectedByNames[rejectedBy] = "Unknown User"
                                }
                            }
                        }
                    } catch (e: Exception) {
                        rejectedByNames[rejectedBy] = "Unknown User"
                    }
                }
            }
        }
    }
    
    // For Production Head: Show ALL local saved notifications (no filtering)
    // The notifications section will show all notifications from local storage
    
    // Load projects, phase requests, and notifications for Production Head
    // Use both effectiveUserRole and authState.user?.role for robustness
    LaunchedEffect(isBusinessHead, currentUserId, authState.user) {
        if (isBusinessHead) {
            Log.d("ProjectNotificationListScreen", "🔄 Loading data for Production Head")
            // Force reload projects to ensure we have the latest data including declined projects
            // For Production Head, load all projects (pass null or empty to get all projects)
            projectViewModel.loadProjects(null) // Load all projects for Production Head
            projectViewModel.loadAllPhaseRequests() // Load all phase requests across all projects
            // Also load notifications for Production Head
            if (currentUserId.isNotEmpty()) {
                // For production heads, check both phone and UID to catch all notifications
                // Notifications might be saved with either phone number or UID as recipientId
                val alternateUserIds = mutableListOf<String>()
                val currentUser = authState.user
                if (currentUser != null) {
                    // If currentUserId is phone, also check UID
                    if (currentUserId == currentUser.phone && currentUser.uid.isNotEmpty()) {
                        alternateUserIds.add(currentUser.uid)
                    }
                    // If currentUserId is UID, also check phone
                    if (currentUserId == currentUser.uid && currentUser.phone.isNotEmpty()) {
                        alternateUserIds.add(currentUser.phone)
                    }
                }
                Log.d("ProjectNotificationListScreen", "🔄 Loading notifications for Production Head - userId: $currentUserId, alternateIds: $alternateUserIds")
                // Load all notifications (read and unread) for Production Head to show chat messages
                notificationViewModel.loadAllNotifications(currentUserId, alternateUserIds)
            }
        } else if (currentUserId.isNotEmpty()) {
            notificationViewModel.loadNotifications(currentUserId)
        }
    }
    
    // Refresh notifications when screen becomes visible (for better UX)
    LaunchedEffect(Unit) {
        if (currentUserId.isNotEmpty()) {
            delay(500)
            notificationViewModel.onScreenVisible(currentUserId)
        }
    }
    
    // Listen for push notification broadcasts to refresh notifications and badge
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.cubiquitous.tracura.NOTIFICATION_SAVED") {
                    val recipientId = intent.getStringExtra("recipientId") ?: ""
                    val notificationId = intent.getStringExtra("notificationId") ?: ""
                    
                    Log.d("ProjectNotificationListScreen", "📢 Received notification saved broadcast - recipientId: $recipientId, notificationId: $notificationId")
                    
                    // Refresh notifications if it's for the current user
                    if (currentUserId.isNotEmpty() && (recipientId == currentUserId || recipientId.isEmpty())) {
                        // For Business Head, check alternate IDs too
                        if (isBusinessHead) {
                            val alternateUserIds = mutableListOf<String>()
                            val currentUser = authState.user
                            if (currentUser != null) {
                                if (currentUserId == currentUser.phone && currentUser.uid.isNotEmpty()) {
                                    alternateUserIds.add(currentUser.uid)
                                }
                                if (currentUserId == currentUser.uid && currentUser.phone.isNotEmpty()) {
                                    alternateUserIds.add(currentUser.phone)
                                }
                            }
                            notificationViewModel.refreshFromLocalStorage(currentUserId, alternateUserIds)
                        } else {
                            notificationViewModel.refreshFromLocalStorage(currentUserId)
                        }
                        Log.d("ProjectNotificationListScreen", "✅ Refreshed notifications and badge after push notification")
                    }
                }
            }
        }
        
        val filter = IntentFilter("com.cubiquitous.tracura.NOTIFICATION_SAVED")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        
        Log.d("ProjectNotificationListScreen", "📢 Registered notification broadcast receiver")
        
        onDispose {
            try {
                context.unregisterReceiver(receiver)
                Log.d("ProjectNotificationListScreen", "📢 Unregistered notification broadcast receiver")
            } catch (e: Exception) {
                Log.e("ProjectNotificationListScreen", "❌ Error unregistering receiver: ${e.message}")
            }
        }
    }
    
    // Ensure role is refreshed from authState if not set initially (handles login timing issues)
    LaunchedEffect(authState.user?.role, authState.user) {
        if (authState.user?.role == com.cubiquitous.tracura.model.UserRole.BUSINESS_HEAD && isBusinessHead) {
            // Force reload data when role is confirmed
            projectViewModel.loadProjects(null)
            projectViewModel.loadAllPhaseRequests()
            if (currentUserId.isNotEmpty()) {
                // For production heads, check both phone and UID to catch all notifications
                val alternateUserIds = mutableListOf<String>()
                val currentUser = authState.user
                if (currentUser != null) {
                    // If currentUserId is phone, also check UID
                    if (currentUserId == currentUser.phone && currentUser.uid.isNotEmpty()) {
                        alternateUserIds.add(currentUser.uid)
                    }
                    // If currentUserId is UID, also check phone
                    if (currentUserId == currentUser.uid && currentUser.phone.isNotEmpty()) {
                        alternateUserIds.add(currentUser.phone)
                    }
                }
                notificationViewModel.loadAllNotifications(currentUserId, alternateUserIds)
            }
        }
    }
    
    // Convert to Dialog popup - Always show as popup, not full screen
    Dialog(
        onDismissRequest = onNavigateBack,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // Background content - show Business Head project selection screen behind dialog
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)) // Light gray background matching Business Head screen
        ) {
            // Show Business Head project list behind the dialog
            if (isBusinessHead) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Top Header Bar - matching BusinessHeadProjectsTab
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Business Name
                            val IosStyleFamily = FontFamily(
                                Font(R.font.inter_extralight, TextFontWeight.Light),
                                Font(R.font.inter_bold, TextFontWeight.Bold)
                            )
                            Text(
                                text = if (businessName.length > 21) businessName.take(21) + "..." else businessName,
                                fontSize = 24.sp,
                                fontFamily = IosStyleFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                softWrap = true,
                                modifier = Modifier.fillMaxWidth(0.5f)
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // Notification Icon (disabled - dialog is open)
                            Box {
                                IconButton(
                                    onClick = { /* Disabled - dialog is open */ },
                                    modifier = Modifier.size(40.dp),
                                    enabled = false
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.notification),
                                        contentDescription = "Notifications",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            // Filter Dropdown (disabled - dialog is open)
                            Surface(
                                modifier = Modifier.clickable(enabled = false) { },
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "All",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(1.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Project count and search bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${projects.size} projects",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Search bar
                        OutlinedTextField(
                            value = "",
                            onValueChange = { /* Disabled - dialog is open */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            placeholder = {
                                Text(
                                    text = "Search projects...",
                                    color = Color.Gray
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.Gray
                                )
                            },
                            enabled = false,
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF4285F4),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                disabledContainerColor = Color.White,
                                disabledBorderColor = Color(0xFFE0E0E0)
                            )
                        )
                    }
                    
                    // Project list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(projects.take(5)) { project -> // Show first 5 projects
                            BusinessHeadProjectCardWithStatus(
                                project = project,
                                onClick = { /* Disabled - dialog is open */ },
                                onEditClick = { /* Disabled - dialog is open */ },
                                userRole = UserRole.BUSINESS_HEAD
                            )
                        }
                    }
                }
            }
            
            // Semi-transparent overlay - lighter to show background content better
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                // Card with notification content - stop click propagation
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.8f)
                        .clickable(enabled = false) { },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header - "Notifications" title centered with close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Close button on the right
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .size(24.dp)
                    ) {
                       /* Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )*/
                    }
                    
                    // Centered title
                    Text(
                        text = "Notifications2",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                
                // Scrollable content
                // For Production Head, ALWAYS show the three sections (Phase Requests, Declined Projects, Notifications)
                // regardless of loading state, error, or empty data
                if (isBusinessHead) {
                        // Calculate latest notifications outside LazyColumn
                        val sortedNotifications = remember(notifications) {
                            notifications.sortedByDescending { it.createdAt?.seconds ?: 0L }
                        }
                        val latestNotifications = remember(sortedNotifications) {
                            sortedNotifications.take(3) // Only show latest 3
                        }
                        
                        // Calculate latest phase requests (show only 2)
                        val latestPhaseRequests = remember(allPhaseRequests) {
                            allPhaseRequests.take(2) // Only show latest 2
                        }
                        
                        // Calculate latest declined projects (show only 2)
                        val latestDeclinedProjects = remember(declinedProjects) {
                            declinedProjects.take(2) // Only show latest 2
                        }
                        
                        // Show three sections: Phase Requests, Declined Projects, and Notifications
                        // Always show all three sections for Production Head, even if empty
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Section 1: PHASE REQUESTS - Always show header
                            item {
                                SectionHeader(
                                    title = "PHASE REQUESTS",
                                    badgeCount = allPhaseRequests.size,
                                    badgeColor = Color(0xFFFF9800) // Orange
                                )
                            }
                            
                            if (isLoadingPhaseRequests) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Color(0xFFFF9800)
                                        )
                                    }
                                }
                            } else if (allPhaseRequests.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No phase requests",
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            } else {
                                items(latestPhaseRequests) { request ->
                                    var showAcceptRejectDialog by remember { mutableStateOf(false) }
                                    
                                    PhaseRequestCard(
                                        request = request,
                                        authViewModel = authViewModel,
                                        onProjectClick = { 
                                            // Open Accept/Reject dialog instead of navigating
                                            showAcceptRejectDialog = true
                                        }
                                    )
                                    
                                    // Accept/Reject Dialog
                                    if (showAcceptRejectDialog) {
                                        PhaseRequestAcceptRejectDialog(
                                            request = request,
                                            onDismiss = { showAcceptRejectDialog = false },
                                            onAccept = { extendedDate: String, reason: String ->
                                                showAcceptRejectDialog = false
                                                val projectId = (request["projectId"] as? String) ?: ""
                                                val phaseId = (request["phaseId"] as? String) ?: ""
                                                val requestId = (request["id"] as? String) ?: ""
                                                projectViewModel.approvePhaseRequest(
                                                    projectId = projectId,
                                                    phaseId = phaseId,
                                                    requestId = requestId,
                                                    extendedDate = extendedDate,
                                                    reason = reason,
                                                    onComplete = { success ->
                                                        if (success) {
                                                            // Request approved successfully
                                                        }
                                                    }
                                                )
                                            },
                                            onReject = { reason: String ->
                                                showAcceptRejectDialog = false
                                                val projectId = (request["projectId"] as? String) ?: ""
                                                val phaseId = (request["phaseId"] as? String) ?: ""
                                                val requestId = (request["id"] as? String) ?: ""
                                                projectViewModel.rejectPhaseRequest(
                                                    projectId = projectId,
                                                    phaseId = phaseId,
                                                    requestId = requestId,
                                                    reason = reason,
                                                    onComplete = { success ->
                                                        if (success) {
                                                            // Request rejected successfully
                                                        }
                                                    }
                                                )
                                            },
                                            projectViewModel = projectViewModel
                                        )
                                    }
                                }
                                
                                // View All button for Phase Requests (if there are more than 2)
                                if (allPhaseRequests.size > 2) {
                                    item {
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    showAllPhaseRequestsSheet = true
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "View All (${allPhaseRequests.size})",
                                                    color = Color(0xFF4285F4),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            
                                            // Divider after View All
                                            Divider(
                                                modifier = Modifier.padding(top = 4.dp, bottom = 1.dp),
                                                color = Color.LightGray.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                } else {
                                    // Divider after PHASE REQUESTS section (when no View All button)
                                    item {
                                        Divider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = Color.LightGray.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                            
                            // Section 2: DECLINED PROJECTS - Always show header
                            item {
                                SectionHeader(
                                    title = "DECLINED PROJECTS",
                                    badgeCount = declinedProjects.size,
                                    badgeColor = Color(0xFFF44336) // Red
                                )
                            }
                            
                            if (declinedProjects.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No declined projects",
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            } else {
                                items(latestDeclinedProjects) { project ->
                                    DeclinedProjectCard(
                                        project = project,
                                        rejectedByName = rejectedByNames[project.rejectedBy] ?: "Loading...",
                                        onProjectClick = {
                                            // Navigate to NewProjectScreen for resubmission
                                            onNavigateToNewProjectForResubmission(project.id ?: "")
                                        }
                                    )
                                }
                                
                                // View All button for Declined Projects (if there are more than 2)
                                if (declinedProjects.size > 2) {
                                    item {
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    showAllDeclinedProjectsSheet = true
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "View All (${declinedProjects.size})",
                                                    color = Color(0xFF4285F4),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            
                                            // Divider after View All
                                            Divider(
                                                modifier = Modifier.padding(top = 4.dp, bottom = 1.dp),
                                                color = Color.LightGray.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                } else {
                                    // Divider after DECLINED PROJECTS section (when no View All button)
                                    item {
                                        Divider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = Color.LightGray.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                            
                            // Section 3: NOTIFICATIONS - Always show header
                            item {
                                SectionHeader(
                                    title = "NOTIFICATIONS",
                                    badgeCount = notifications.count { !it.isRead },
                                    badgeColor = Color(0xFF4285F4) // Blue
                                )
                            }
                            
                            if (notifications.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No notifications",
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            } else {
                                items(latestNotifications) { notification ->
                                    NotificationCard(
                                        notification = notification,
                                        onNotificationClick = { clickedNotification: Notification ->
                                            // If project is DECLINED and user is Business Head, always go to resubmission
                                            val notifProject = projects.find { it.id == clickedNotification.projectId }
                                            val isDeclined = notifProject?.status?.trim()
                                                ?.let { it.equals("DECLINED", ignoreCase = true) || it.equals("REVIEW REJECTED", ignoreCase = true) } == true
                                            if (isDeclined && isBusinessHead && clickedNotification.projectId.isNotEmpty()) {
                                                onNavigateToNewProjectForResubmission(clickedNotification.projectId)
                                            } else {
                                            // Navigate based on notification type
                                            when (clickedNotification.type) {
                                                NotificationType.CHAT_MESSAGE -> {
                                                    // Navigate directly to chat
                                                    if (clickedNotification.navigationTarget.startsWith("chat/")) {
                                                        val parts = clickedNotification.navigationTarget.split("/")
                                                        if (parts.size >= 4) {
                                                            val projectId = parts[1]
                                                            val chatId = parts[2]
                                                            val otherUserName = parts[3]
                                                            onNavigateToChat(projectId, chatId, otherUserName)
                                                        } else if (clickedNotification.projectId.isNotEmpty() && clickedNotification.relatedId.isNotEmpty()) {
                                                            // Use relatedId as chatId if navigationTarget parsing fails
                                                            val otherUserName = extractUserNameFromMessage(clickedNotification.message) ?: "User"
                                                            onNavigateToChat(clickedNotification.projectId, clickedNotification.relatedId, otherUserName)
                                                        } else if (clickedNotification.projectId.isNotEmpty()) {
                                                            onNavigateToProject(
                                                                clickedNotification.projectId,
                                                                notificationTag(clickedNotification),
                                                                clickedNotification.message
                                                            )
                                                        }
                                                    } else if (clickedNotification.projectId.isNotEmpty() && clickedNotification.relatedId.isNotEmpty()) {
                                                        // relatedId contains chatId for chat notifications
                                                        val otherUserName = extractUserNameFromMessage(clickedNotification.message) ?: "User"
                                                        onNavigateToChat(clickedNotification.projectId, clickedNotification.relatedId, otherUserName)
                                                    } else if (clickedNotification.projectId.isNotEmpty()) {
                                                        onNavigateToProject(
                                                            clickedNotification.projectId,
                                                            notificationTag(clickedNotification),
                                                            clickedNotification.message
                                                        )
                                                    }
                                                }
                                                NotificationType.DELEGATION_REMOVED -> {
                                                    // Just mark as read, no navigation
                                                }
                                                NotificationType.EXPENSE_SUBMITTED,
                                                NotificationType.PENDING_APPROVAL -> {
                                                    onNavigateToPendingApprovals(clickedNotification.projectId)
                                                }
                                                NotificationType.EXPENSE_APPROVED,
                                                NotificationType.EXPENSE_REJECTED,
                                                NotificationType.PROJECT_ASSIGNMENT,
                                                NotificationType.PROJECT_CHANGED -> {
                                                    onNavigateToProject(
                                                        clickedNotification.projectId,
                                                        notificationTag(clickedNotification),
                                                        clickedNotification.message
                                                    )
                                                }
                                                else -> {
                                                    // Default navigation
                                                    if (clickedNotification.projectId.isNotEmpty()) {
                                                        onNavigateToProject(
                                                            clickedNotification.projectId,
                                                            notificationTag(clickedNotification),
                                                            clickedNotification.message
                                                        )
                                                    }
                                                }
                                            }
                                            }
                                            
                                            // Mark notification as read and delete from local storage
                                            notificationViewModel.markNotificationAsRead(clickedNotification.id)
                                            notificationViewModel.deleteNotification(clickedNotification.id)
                                        },
                                        onMarkAsRead = { notificationId ->
                                            notificationViewModel.markNotificationAsRead(notificationId)
                                        }
                                    )
                                }
                            }

                            
                            // Divider after NOTIFICATIONS section
                            /*item {
                                Divider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = Color.LightGray.copy(alpha = 0.3f)
                                )
                            }*/
                        }
                } else {
                    // For non-Production Head users, use when statement
                    when {
                        // Error state for non-Production Head users
                        error != null -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "❌ Error Loading Notifications",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = error ?: "Unknown error occurred",
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { 
                                            notificationViewModel.clearError()
                                            if (currentUserId.isNotEmpty()) {
                                                notificationViewModel.forceLoadNotifications(currentUserId)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4285F4)
                                        )
                                    ) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                        // Empty state for non-Production Head users
                        notifications.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Notifications,
                                        contentDescription = "No notifications",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text(
                                        text = "No Notifications",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                        else -> {
                            // Notifications list for other roles
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(notifications) { notification ->
                                    NotificationCard(
                                        notification = notification,
                                        onNotificationClick = { clickedNotification: Notification ->
                                            // Navigate based on notification type
                                            when (clickedNotification.type) {
                                                NotificationType.DELEGATION_REMOVED -> {
                                                    // For delegation removed notifications, just mark as read and don't navigate
                                                    // The notification will disappear from the list
                                                    // No navigation needed - just mark as read
                                                }
                                                NotificationType.EXPENSE_SUBMITTED,
                                                NotificationType.PENDING_APPROVAL -> {
                                                    onNavigateToPendingApprovals(clickedNotification.projectId)
                                                }
                                                NotificationType.EXPENSE_APPROVED,
                                                NotificationType.EXPENSE_REJECTED,
                                                NotificationType.PROJECT_ASSIGNMENT -> {
                                                    onNavigateToProject(
                                                        clickedNotification.projectId,
                                                        notificationTag(clickedNotification),
                                                        clickedNotification.message
                                                    )
                                                }
                                                else -> {
                                                    onNavigateToProject(
                                                        clickedNotification.projectId,
                                                        notificationTag(clickedNotification),
                                                        clickedNotification.message
                                                    )
                                                }
                                            }
                                            
                                            // Mark notification as read
                                            notificationViewModel.markNotificationAsRead(clickedNotification.id)
                                        },
                                        onMarkAsRead = { notificationId ->
                                            notificationViewModel.markNotificationAsRead(notificationId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Bottom buttons - View All and Close (only for Production Head)
                if (isBusinessHead) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // View All button (if there are more than 3 notifications)
                        if (notifications.size > 3) {
                            TextButton(
                                onClick = {
                                    showAllNotificationsSheet = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "View Alls (${notifications.size})",
                                    color = Color(0xFF4285F4),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            // Divider after View All
                            Divider(
                                modifier = Modifier.padding(top = 4.dp, bottom = 1.dp),
                                color = Color.LightGray.copy(alpha = 0.3f)
                            )
                        }
                        
                        // Close button
                        Button(
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Close",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color.LightGray.copy(alpha = 0.3f)
                    )
                }
            }

            }
        }
        
        // Bottom Sheet for All Notifications
        if (showAllNotificationsSheet) {
            AllNotificationsBottomSheet(
                notifications = notifications.sortedByDescending { it.createdAt?.seconds ?: 0L },
                onDismiss = { showAllNotificationsSheet = false },
                onNotificationClick = { clickedNotification: Notification ->
                    // Navigate based on notification type
                    when (clickedNotification.type) {
                        NotificationType.CHAT_MESSAGE -> {
                            showAllNotificationsSheet = false
                            // Navigate directly to chat
                            if (clickedNotification.navigationTarget.startsWith("chat/")) {
                                val parts = clickedNotification.navigationTarget.split("/")
                                if (parts.size >= 4) {
                                    val projectId = parts[1]
                                    val chatId = parts[2]
                                    val otherUserName = parts[3]
                                    onNavigateToChat(projectId, chatId, otherUserName)
                                } else if (clickedNotification.projectId.isNotEmpty() && clickedNotification.relatedId.isNotEmpty()) {
                                    // Use relatedId as chatId if navigationTarget parsing fails
                                    val otherUserName = extractUserNameFromMessage(clickedNotification.message) ?: "User"
                                    onNavigateToChat(clickedNotification.projectId, clickedNotification.relatedId, otherUserName)
                                } else if (clickedNotification.projectId.isNotEmpty()) {
                                    onNavigateToProject(
                                        clickedNotification.projectId,
                                        notificationTag(clickedNotification),
                                        clickedNotification.message
                                    )
                                }
                            } else if (clickedNotification.projectId.isNotEmpty() && clickedNotification.relatedId.isNotEmpty()) {
                                // relatedId contains chatId for chat notifications
                                val otherUserName = extractUserNameFromMessage(clickedNotification.message) ?: "User"
                                onNavigateToChat(clickedNotification.projectId, clickedNotification.relatedId, otherUserName)
                            } else if (clickedNotification.projectId.isNotEmpty()) {
                                onNavigateToProject(
                                    clickedNotification.projectId,
                                    notificationTag(clickedNotification),
                                    clickedNotification.message
                                )
                            }
                        }
                        NotificationType.DELEGATION_REMOVED -> {
                            // Just mark as read, no navigation
                        }
                        NotificationType.EXPENSE_SUBMITTED,
                        NotificationType.PENDING_APPROVAL -> {
                            showAllNotificationsSheet = false
                            onNavigateToPendingApprovals(clickedNotification.projectId)
                        }
                        NotificationType.EXPENSE_APPROVED,
                        NotificationType.EXPENSE_REJECTED,
                        NotificationType.PROJECT_ASSIGNMENT,
                        NotificationType.PROJECT_CHANGED -> {
                            showAllNotificationsSheet = false
                            onNavigateToProject(
                                clickedNotification.projectId,
                                notificationTag(clickedNotification),
                                clickedNotification.message
                            )
                        }
                        else -> {
                            // Default navigation
                            if (clickedNotification.projectId.isNotEmpty()) {
                                showAllNotificationsSheet = false
                                onNavigateToProject(
                                    clickedNotification.projectId,
                                    notificationTag(clickedNotification),
                                    clickedNotification.message
                                )
                            }
                        }
                    }
                    
                    // Mark notification as read
                    notificationViewModel.markNotificationAsRead(clickedNotification.id)
                }
            )
        }
        
        // Bottom Sheet for All Phase Requests
        if (showAllPhaseRequestsSheet) {
            AllPhaseRequestsBottomSheet(
                phaseRequests = allPhaseRequests,
                onDismiss = { showAllPhaseRequestsSheet = false },
                authViewModel = authViewModel,
                projectViewModel = projectViewModel
            )
        }
        
        // Bottom Sheet for All Declined Projects
        if (showAllDeclinedProjectsSheet) {
            AllDeclinedProjectsBottomSheet(
                declinedProjects = declinedProjects,
                rejectedByNames = rejectedByNames,
                onDismiss = { showAllDeclinedProjectsSheet = false },
                onNavigateToNewProjectForResubmission = onNavigateToNewProjectForResubmission
            )
        }
    }
}

/**
 * Bottom Sheet Modal for showing all notifications
 * Matches the second screenshot design exactly
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllNotificationsBottomSheet(
    notifications: List<Notification>,
    onDismiss: () -> Unit,
    onNotificationClick: (Notification) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    // LazyListState for tracking scroll position
    val listState = rememberLazyListState()
    
    // Track scroll position to show/hide title in header - matching New Project screen logic
    val showTitleInHeader by remember {
        derivedStateOf {
            // Show title in header when scrolled past the title item or when title starts scrolling out
            listState.firstVisibleItemIndex > 0 || (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset > 50)
        }
    }
    
    // Expand sheet to 97% height on launch
    LaunchedEffect(Unit) {
        try {
            sheetState.expand()
        } catch (e: Exception) {
            Log.e("AllNotificationsBottomSheet", "Error expanding sheet: ${e.message}")
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF5F5F5), // Light grey background matching screenshot
        dragHandle = null, // No drag handle for full-screen appearance
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f) // 95% of screen height
                .background(Color(0xFFF5F5F5))
        ) {
            // Top bar with Done button - matching New Project screen style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Empty space on left to balance Done button on right
                Spacer(modifier = Modifier.width(64.dp))
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Show title in header when scrolled - matching New Project screen
                AnimatedVisibility(
                    visible = showTitleInHeader,
                    enter = fadeIn() + slideInHorizontally(),
                    exit = fadeOut() + slideOutHorizontally()
                ) {
                    Text(
                        text = "Notifications1",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Done button - matching New Project screen style
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = "Done",
                        color = Color(0xFF4285F4), // Blue color matching screenshot
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Notifications List with scroll tracking
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Notifications Header - below top bar (hide when scrolled) - matching New Project screen
                item {
                    AnimatedVisibility(
                        visible = !showTitleInHeader,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = "Notifications",
                                fontSize = 48.sp, // Matching "New Project" font size
                                fontWeight = FontWeight.Medium,
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                // Notifications List - Empty state
                if (notifications.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "No notifications",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Notifications",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                } else {
                    // Notifications List - Items
                    items(notifications) { notification ->
                        AllNotificationItem(
                            notification = notification,
                            onClick = { onNotificationClick(notification) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom Sheet Modal for showing all phase requests
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllPhaseRequestsBottomSheet(
    phaseRequests: List<Map<String, Any>>,
    onDismiss: () -> Unit,
    authViewModel: AuthViewModel,
    projectViewModel: ProjectViewModel
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    val listState = rememberLazyListState()
    
    val showTitleInHeader by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset > 50)
        }
    }
    
    LaunchedEffect(Unit) {
        try {
            sheetState.expand()
        } catch (e: Exception) {
            Log.e("AllPhaseRequestsBottomSheet", "Error expanding sheet: ${e.message}")
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF5F5F5),
        dragHandle = null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .background(Color(0xFFF5F5F5))
        ) {
            // Top bar with Done button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(64.dp))
                
                Spacer(modifier = Modifier.weight(1f))
                
                AnimatedVisibility(
                    visible = showTitleInHeader,
                    enter = fadeIn() + slideInHorizontally(),
                    exit = fadeOut() + slideOutHorizontally()
                ) {
                    Text(
                        text = "Phase Requests",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = "Done",
                        color = Color(0xFF4285F4),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Phase Requests List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                item {
                    AnimatedVisibility(
                        visible = !showTitleInHeader,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = "Phase Requests",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                if (phaseRequests.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No Phase Requests",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                } else {
                    items(phaseRequests) { request ->
                        var showAcceptRejectDialog by remember { mutableStateOf(false) }
                        
                        PhaseRequestCard(
                            request = request,
                            authViewModel = authViewModel,
                            onProjectClick = {
                                showAcceptRejectDialog = true
                            }
                        )
                        
                        if (showAcceptRejectDialog) {
                            PhaseRequestAcceptRejectDialog(
                                request = request,
                                onDismiss = { showAcceptRejectDialog = false },
                                onAccept = { extendedDate: String, reason: String ->
                                    showAcceptRejectDialog = false
                                    val projectId = (request["projectId"] as? String) ?: ""
                                    val phaseId = (request["phaseId"] as? String) ?: ""
                                    val requestId = (request["id"] as? String) ?: ""
                                    projectViewModel.approvePhaseRequest(
                                        projectId = projectId,
                                        phaseId = phaseId,
                                        requestId = requestId,
                                        extendedDate = extendedDate,
                                        reason = reason,
                                        onComplete = { success ->
                                            if (success) {
                                                // Request approved successfully
                                            }
                                        }
                                    )
                                },
                                onReject = { reason: String ->
                                    showAcceptRejectDialog = false
                                    val projectId = (request["projectId"] as? String) ?: ""
                                    val phaseId = (request["phaseId"] as? String) ?: ""
                                    val requestId = (request["id"] as? String) ?: ""
                                    projectViewModel.rejectPhaseRequest(
                                        projectId = projectId,
                                        phaseId = phaseId,
                                        requestId = requestId,
                                        reason = reason,
                                        onComplete = { success ->
                                            if (success) {
                                                // Request rejected successfully
                                            }
                                        }
                                    )
                                },
                                projectViewModel = projectViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom Sheet Modal for showing all declined projects
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllDeclinedProjectsBottomSheet(
    declinedProjects: List<Project>,
    rejectedByNames: Map<String, String>,
    onDismiss: () -> Unit,
    onNavigateToNewProjectForResubmission: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    val listState = rememberLazyListState()
    
    val showTitleInHeader by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset > 50)
        }
    }
    
    LaunchedEffect(Unit) {
        try {
            sheetState.expand()
        } catch (e: Exception) {
            Log.e("AllDeclinedProjectsBottomSheet", "Error expanding sheet: ${e.message}")
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF5F5F5),
        dragHandle = null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .background(Color(0xFFF5F5F5))
        ) {
            // Top bar with Done button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(64.dp))
                
                Spacer(modifier = Modifier.weight(1f))
                
                AnimatedVisibility(
                    visible = showTitleInHeader,
                    enter = fadeIn() + slideInHorizontally(),
                    exit = fadeOut() + slideOutHorizontally()
                ) {
                    Text(
                        text = "Declined Projects",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = "Done",
                        color = Color(0xFF4285F4),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Declined Projects List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                item {
                    AnimatedVisibility(
                        visible = !showTitleInHeader,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = "Declined Projects",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                if (declinedProjects.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No Declined Projects",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                } else {
                    items(declinedProjects) { project ->
                        DeclinedProjectCard(
                            project = project,
                            rejectedByName = rejectedByNames[project.rejectedBy] ?: "Loading...",
                            onProjectClick = {
                                onNavigateToNewProjectForResubmission(project.id ?: "")
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Notification item for the bottom sheet - matching second screenshot exactly
 */
@Composable
private fun AllNotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 22.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White // White background matching screenshot
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp) // No rounded corners matching screenshot
    ) {
        Column {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 24.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Dynamic icon based on notification type and content - matching reference icons
                val (icon, iconColor) = getNotificationIconInfo(notification)
                val title = notification.title.lowercase()
                val message = notification.message.lowercase()
                // Check if this is a phase starting notification (use custom flag icon)
                val isPhaseStarting =
                    (title.contains("phase") && (title.contains("start") || message.contains("going to start"))) ||
                            (title.contains("phase starting"))
                // Check if this is a phase ended/completed notification (use custom phase completed icon)
                val isPhaseEnded =
                    (title.contains("phase") && (title.contains("ended") || title.contains("completed") || message.contains(
                        "ended today"
                    ) || message.contains("ended"))) ||
                            (title.contains("phase ended") || title.contains("phase completed"))
                // Check if this is a completed project status update (use custom check icon)
                val isCompleted =
                    (title.contains("project status update") && (message.contains("is now completed") || message.contains(
                        "completed"
                    )))
                // Check if this is a standby project status update (use pause circle icon)
                val isStandby =
                    (title.contains("project status update") && (message.contains("is now standby") || message.contains(
                        "standby"
                    )))
                // Check if this is a suspension ending soon notification
                val isSuspensionEndingSoon =
                    notification.title.contains("Suspension Ending Soon", ignoreCase = true)
                // Check if this is a suspension project status update (use warning icon)
                val isSuspension =
                    (title.contains("project status update") && (message.contains("is now suspension") || message.contains(
                        "suspension"
                    )))
                // Check if this is an active project status update (use special green play icon with halo)
                val isActive =
                    (title.contains("project status update") && (message.contains("is now active") || message.contains(
                        "active"
                    ) || message.contains("going to active")))
                // Check if this is a maintenance notification (use custom maintenance icon)
                val isMaintenance =
                    (title.contains("project status update") && (message.contains("is now maintenance") || message.contains(
                        "maintenance"
                    ))) ||
                            (title.contains("upcoming project update") && message.contains("going to maintenance"))
                // Check if this is specifically "Project Status Update : MAINTENANCE" (use maintenance.png icon)
                val isMaintenanceProjectStatusUpdate = title.contains("project status update") &&
                        (message.contains("is now maintenance") || message.contains("maintenance")) &&
                        !title.contains("upcoming project update")
                // Check if this is "Maintenance Starting Soon" (use houricon with purple tint)
                val isMaintenanceStartingSoon =
                    notification.title.contains("Maintenance Starting Soon", ignoreCase = true)
                // Check if this is an upcoming project update going to start tomorrow (use custom houricon)
                val isUpcomingProjectStart =
                    title.contains("upcoming project update") && message.contains("going to start")
                // Check if this is a handover tomorrow notification (use custom handover1 icon)
                val isHandoverTomorrow = (notification.title.contains("Handover Tomorrow", ignoreCase = true) ||
                                       (title.contains("upcoming project update") && message.contains("handover") && message.contains("scheduled for tomorrow")))
                // Check if this is a ready for handover notification (use custom handover2 icon)
                val isHandoverToday = notification.title.contains("Ready for handover", ignoreCase = true) ||
                                    (title.contains("upcoming project update") && message.contains("handover") && message.contains("scheduled for today"))
                // For green play icon, use solid green background with white icon and light green halo (only for non-active play icons)
                val isGreenPlayIcon =
                    icon == Icons.Default.PlayArrow && iconColor == Color(0xFF06F610) && !isPhaseStarting && !isPhaseEnded && !isCompleted && !isStandby && !isSuspension && !isSuspensionEndingSoon && !isActive && !isMaintenance && !isMaintenanceStartingSoon && !isUpcomingProjectStart && !isHandoverTomorrow && !isHandoverToday
                // Special handling for active project status updates - use solid green with light green halo
                val isActiveProjectStatus = isActive && icon == Icons.Default.PlayArrow
                val backgroundColor = if (isGreenPlayIcon) {
                    iconColor // Solid green background
                } else {
                    iconColor.copy(alpha = 0.15f) // Light background for other icons
                }
                val iconTint = if (isGreenPlayIcon) {
                    Color.White // White icon for green play button
                } else {
                    iconColor // Colored icon for others
                }

                // For active project status updates - use solid green circle with light green halo
                if (isActiveProjectStatus) {
                    Box(
                        modifier = Modifier
                            .size(30.dp) // Outer circle size (reduced by 2)
                            .background(
                                color = Color(0xFF06F610).copy(alpha = 0.15f), // Light green halo
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner solid green circle
                        Box(
                            modifier = Modifier
                                .size(14.dp) // Reduced by 2
                                .background(
                                    color = Color(0xFF06F610), // Solid green background
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = Color.White, // White play icon
                                modifier = Modifier.size(10.dp) // Reduced by 2
                            )
                        }
                    }
                } else if (isGreenPlayIcon) {
                    Box(
                        modifier = Modifier
                            .size(32.dp) // Same size as other icons
                            .background(
                                color = backgroundColor, // Solid green background
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (isCompleted) {
                    // Use custom check icon for completed project status updates with light blue background
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color(0xFFE3F2FD), // Light blue background
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.check),
                            contentDescription = null,
                            tint = Color(0xFF03A9F4), // Light blue color for check icon
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (isStandby) {
                    // Use pause circle icon for standby project status updates
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color(0xFFDA7B99).copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = Color(0xFFE91E63), // Grey - pause circle icon
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (isSuspensionEndingSoon) {
                    // Use warning icon for suspension ending soon with orange background
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color(0xFFFF9800).copy(alpha = 0.15f), // Light orange background
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = Color(0xFFFF9800), // Orange icon
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (isSuspension) {
                    // Use warning icon for suspension project status updates with orange background
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color(0xFFFF9800).copy(alpha = 0.15f), // Light orange background
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = Color(0xFFFF9800), // Orange icon
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (isPhaseEnded) {
                    // Use custom phase completed icon for phase ended/completed notifications with original colors
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color(0xFF06F610).copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.phasecompleted),
                            contentDescription = null,
                            tint = Color.Unspecified, // Use original colors from drawable
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (isPhaseStarting) {
                    // Use custom flag icon for phase starting notifications with green tint
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color(0xFF06F610).copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.flag1),
                            contentDescription = null,
                            tint = Color(0xFF06F610), // Green color for flag icon
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (isMaintenanceStartingSoon) {
                    // Use houricon with purple tint for Maintenance Starting Soon
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color(0xFF9C27B0).copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.houricon),
                            contentDescription = null,
                            tint = Color(0xFF9C27B0), // Purple tint
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (isMaintenanceProjectStatusUpdate && !isMaintenanceStartingSoon) {
                    // Use maintenance.png icon for "Project Status Update : MAINTENANCE"
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = backgroundColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.maintenance),
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (isMaintenance && !isMaintenanceStartingSoon && !isMaintenanceProjectStatusUpdate) {
                    // Use houricon for maintenance notifications that are not "Maintenance Starting Soon" and not "Project Status Update : MAINTENANCE"
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = backgroundColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.houricon),
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (isUpcomingProjectStart) {
                    // Use custom houricon for upcoming project update going to start
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = backgroundColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.houricon),
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (isHandoverTomorrow) {
                    // Use custom handover1 icon for handover tomorrow notifications with gradient background
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Green.copy(alpha = 0.15f),
                                        Color.Blue.copy(alpha = 0.15f),
                                        Color(0xFF9C27B0).copy(alpha = 0.15f) // Purple
                                    ),
                                    start = Offset(0f, 0f), // topLeading
                                    end = Offset(32f, 32f) // bottomTrailing
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.handover1),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (isHandoverToday) {
                    // Use custom handover2 icon for ready for handover notifications with gradient background
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Green.copy(alpha = 0.15f),
                                        Color.Blue.copy(alpha = 0.15f),
                                        Color(0xFF9C27B0).copy(alpha = 0.15f) // Purple
                                    ),
                                    start = Offset(0f, 0f), // topLeading
                                    end = Offset(32f, 32f) // bottomTrailing
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.handover1),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = backgroundColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Notification content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Remove emojis/icons from title (bell, hourglass, chat bubble, rocket, etc.)
                    var cleanTitle = notification.title
                        .replace("🔔", "")
                        .replace("⏳", "")
                        .replace("💬", "")
                        .replace("🔴", "")
                        .replace("🟡", "")
                        .replace("🟢", "")
                        .replace("🚀", "")
                        .replace(
                            Regex("[\uD83C-\uDBFF\uDC00-\uDFFF]+"),
                            ""
                        ) // Remove all emojis (Unicode ranges)
                        .replace(Regex("[\u2600-\u27BF]"), "") // Remove additional emoji ranges
                        .trim()

                    // Change title for Project Status Update notifications to include status
                    val titleLower = cleanTitle.lowercase()
                    val messageLower = notification.message.lowercase()
                    if (titleLower.contains("project status update")) {
                        val status = when {
                            messageLower.contains("is now active") || messageLower.contains("active") -> "ACTIVE"
                            messageLower.contains("is now completed") || messageLower.contains("completed") -> "COMPLETED"
                            messageLower.contains("is now maintenance") || messageLower.contains("maintenance") -> "MAINTENANCE"
                            messageLower.contains("is now standby") || messageLower.contains("standby") -> "STANDBY"
                            messageLower.contains("is now suspension") || messageLower.contains("suspension") -> "SUSPENSION"
                            else -> null
                        }
                        if (status != null) {
                            cleanTitle = "Project Status Update : $status"
                        }
                    }

                    // Change title for maintenance notifications
                    val isMaintenanceUpcoming =
                        cleanTitle.lowercase().contains("upcoming project update") &&
                                notification.message.lowercase().contains("going to maintenance")
                    if (isMaintenanceUpcoming) {
                        val messageLower = notification.message.lowercase()
                        if (messageLower.contains("tomorrow")) {
                            cleanTitle = "Maintenance Starts Tomorrow"
                        } else {
                            // Check if it's more than 2 days away by looking for day numbers or "in X days"
                            val daysPattern = Regex("in (\\d+) days?|(\\d+) days?")
                            val daysMatch = daysPattern.find(messageLower)
                            val daysAway = daysMatch?.groupValues?.get(1)?.toIntOrNull()
                                ?: daysMatch?.groupValues?.get(2)?.toIntOrNull()

                            if (daysAway != null && daysAway > 2) {
                                cleanTitle = "Maintenance Starting Soon"
                            } else if (daysAway == null || daysAway > 2) {
                                // Default to "Starting Soon" if we can't determine or it's more than 2 days
                                cleanTitle = "Maintenance Starting Soon"
                            } else {
                                // If 2 days or less but not tomorrow, keep original or use "Starting Soon"
                                cleanTitle = "Maintenance Starting Soon"
                            }
                        }
                    }

                    // Change title for completion notifications
                    val isCompletionUpcoming =
                        cleanTitle.lowercase().contains("upcoming project update") &&
                                notification.message.lowercase().contains("going to complete") &&
                                !notification.message.lowercase().contains("going to maintenance")
                    if (isCompletionUpcoming) {
                        val message = notification.message
                        val messageLower = message.lowercase()

                        // Extract project name from message (e.g., "Abcd is going to complete tomorrow" -> "Abcd")
                        // Try multiple patterns to extract project name
                        var projectName = "Project"
                        val patterns = listOf(
                            Regex(
                                "^([A-Za-z0-9\\s]+?)\\s+is\\s+going\\s+to\\s+complete",
                                RegexOption.IGNORE_CASE
                            ),
                            Regex(
                                "^([A-Za-z0-9\\s]+?)\\s+will\\s+complete",
                                RegexOption.IGNORE_CASE
                            ),
                            Regex(
                                "^([A-Za-z0-9\\s]+?)\\s+is\\s+completing",
                                RegexOption.IGNORE_CASE
                            )
                        )

                        for (pattern in patterns) {
                            val match = pattern.find(message)
                            if (match != null) {
                                projectName = match.groupValues[1].trim()
                                break
                            }
                        }

                        if (messageLower.contains("tomorrow")) {
                            cleanTitle = "$projectName Completes Tomorrow"
                        } else {
                            // For other cases, use "Project Completes Soon" or similar
                            cleanTitle = "$projectName Completes Soon"
                        }
                    }

                    // Change title for handover notifications
                    val isHandoverUpcoming =
                        cleanTitle.lowercase().contains("upcoming project update") &&
                                notification.message.lowercase().contains("handover") &&
                                notification.message.lowercase().contains("scheduled for tomorrow")
                    if (isHandoverUpcoming) {
                        cleanTitle = "Handover Tomorrow"
                    }

                    // Change title for handover notifications scheduled for today
                    val isHandoverToday =
                        cleanTitle.lowercase().contains("upcoming project update") &&
                                notification.message.lowercase().contains("handover") &&
                                notification.message.lowercase().contains("scheduled for today")
                    if (isHandoverToday) {
                        cleanTitle = "Ready for handover"
                    }

                    // Change title for "Upcoming Project Update" to "Upcoming Project" when message is "{Project name} is going to start tomorrow"
                    val isUpcomingProjectStart =
                        cleanTitle.lowercase().contains("upcoming project update") &&
                                notification.message.lowercase()
                                    .contains("is going to start tomorrow")
                    if (isUpcomingProjectStart) {
                        cleanTitle = cleanTitle.replace(
                            Regex(
                                "Upcoming Project Update",
                                RegexOption.IGNORE_CASE
                            ), "Upcoming Project"
                        )
                    }

                    // Change title "Phase Ended Today" to "Phase Completed"
                    if (cleanTitle.contains("Phase Ended Today", ignoreCase = true)) {
                        cleanTitle = cleanTitle.replace(
                            Regex("Phase Ended Today", RegexOption.IGNORE_CASE),
                            "Phase Completed"
                        )
                    }

                    // Style title with colored COMPLETED, ACTIVE, MAINTENANCE, or STANDBY text
                    val styledTitle = if (cleanTitle.contains("COMPLETED")) {
                        buildAnnotatedString {
                            val parts = cleanTitle.split("COMPLETED")
                            append(parts[0])
                            withStyle(style = SpanStyle(color = Color(0xFF03A9F4))) {
                                append("COMPLETED")
                            }
                            if (parts.size > 1) {
                                append(parts[1])
                            }
                        }
                    } else if (cleanTitle.contains("ACTIVE")) {
                        buildAnnotatedString {
                            val parts = cleanTitle.split("ACTIVE")
                            append(parts[0])
                            withStyle(style = SpanStyle(color = Color(0xFF06F610))) {
                                append("ACTIVE")
                            }
                            if (parts.size > 1) {
                                append(parts[1])
                            }
                        }
                    } else if (cleanTitle.contains("MAINTENANCE")) {
                        buildAnnotatedString {
                            val parts = cleanTitle.split("MAINTENANCE")
                            append(parts[0])
                            withStyle(style = SpanStyle(color = Color(0xFF9C27B0))) {
                                append("MAINTENANCE")
                            }
                            if (parts.size > 1) {
                                append(parts[1])
                            }
                        }
                    } else if (cleanTitle.contains("STANDBY")) {
                        buildAnnotatedString {
                            val parts = cleanTitle.split("STANDBY")
                            append(parts[0])
                            withStyle(style = SpanStyle(color = Color(0xFFE91E63))) {
                                append("STANDBY")
                            }
                            if (parts.size > 1) {
                                append(parts[1])
                            }
                        }
                    } else {
                        AnnotatedString(cleanTitle)
                    }

                    Text(
                        text = styledTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Transform message text: replace "going to maintenance status" with "going into maintenance phase"
                    // Also replace "is going to complete tomorrow" with "is completing tomorrow"
                    // Also replace "is going to complete in X days" with "is coming to an end in X days"
                    val displayMessage = notification.message
                        .replace(
                            Regex("going to maintenance status", RegexOption.IGNORE_CASE),
                            "going into maintenance phase"
                        )
                        .replace(
                            Regex("is going to complete tomorrow", RegexOption.IGNORE_CASE),
                            "is scheduled to complete tomorrow"
                        )
                        .replace(
                            Regex("is going to complete in (\\d+) days?", RegexOption.IGNORE_CASE),
                            "is coming to an end in $1 days"
                        )
                        .replace(
                            Regex(
                                "Suspension status of the (.+?) ends tomorrow",
                                RegexOption.IGNORE_CASE
                            ),
                            "Suspension for $1 ends tomorrow"
                        )
                        .replace(
                            Regex("(.+?) of (.+?) ended today", RegexOption.IGNORE_CASE),
                            "$1 of $2 completed today"
                        )
                        .replace(
                            Regex("(.+?) handover is scheduled for today", RegexOption.IGNORE_CASE),
                            "$1 is ready for handover"
                        )

                    Text(
                        text = displayMessage,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Timestamp on right - matching second screenshot
                /*Log.d("NotificationTime1", "🔄 Loading notification for time: $notification.createdAt")*/
                Text(
                    text = notification.createdAt?.let { FormatUtils.formatTimeAgo(it) } ?: "Just now",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Divider(
                modifier = Modifier.padding(start = 60.dp),
                color = Color.LightGray.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onNotificationClick: (Notification) -> Unit,
    onMarkAsRead: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // No Card wrapper - just content with clickable
    Column {


        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onNotificationClick(notification) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Dynamic icon based on notification type and content - matching reference icons
            val (icon, iconColor) = getNotificationIconInfo(notification)
            val title = notification.title.lowercase()
            val message = notification.message.lowercase()
            // Check if this is a phase starting notification (use custom flag icon)
            val isPhaseStarting =
                (title.contains("phase") && (title.contains("start") || message.contains("going to start"))) ||
                        (title.contains("phase starting"))
            // Check if this is a phase ended/completed notification (use custom phase completed icon)
            val isPhaseEnded =
                (title.contains("phase") && (title.contains("ended") || title.contains("completed") || message.contains(
                    "ended today"
                ) || message.contains("ended"))) ||
                        (title.contains("phase ended") || title.contains("phase completed"))
            // Check if this is a completed project status update (use custom check icon)
            val isCompleted =
                (title.contains("project status update") && (message.contains("is now completed") || message.contains(
                    "completed"
                )))
            // Check if this is a standby project status update (use pause circle icon)
            val isStandby =
                (title.contains("project status update") && (message.contains("is now standby") || message.contains(
                    "standby"
                )))
            // Check if this is a suspension ending soon notification
            val isSuspensionEndingSoon =
                notification.title.contains("Suspension Ending Soon", ignoreCase = true)
            // Check if this is a suspension project status update (use warning icon)
            val isSuspension =
                (title.contains("project status update") && (message.contains("is now suspension") || message.contains(
                    "suspension"
                )))
            // Check if this is an active project status update (use special green play icon with halo)
            val isActive =
                (title.contains("project status update") && (message.contains("is now active") || message.contains(
                    "active"
                ) || message.contains("going to active")))
            // Check if this is a maintenance notification (use custom maintenance icon)
            val isMaintenance =
                (title.contains("project status update") && (message.contains("is now maintenance") || message.contains(
                    "maintenance"
                ))) ||
                        (title.contains("upcoming project update") && message.contains("going to maintenance"))
            // Check if this is specifically "Project Status Update : MAINTENANCE" (use maintenance.png icon)
            val isMaintenanceProjectStatusUpdate = title.contains("project status update") &&
                    (message.contains("is now maintenance") || message.contains("maintenance")) &&
                    !title.contains("upcoming project update")
            // Check if this is "Maintenance Starting Soon" (use houricon with purple tint)
            val isMaintenanceStartingSoon =
                notification.title.contains("Maintenance Starting Soon", ignoreCase = true)
            // Check if this is an upcoming project update going to start tomorrow (use custom houricon)
            val isUpcomingProjectStart =
                title.contains("upcoming project update") && message.contains("going to start")
            // Check if this is a handover tomorrow notification (use custom handover1 icon)
            val isHandoverTomorrow =
                (notification.title.contains("Handover Tomorrow", ignoreCase = true) ||
                        (title.contains("upcoming project update") && message.contains("handover") && message.contains(
                            "scheduled for tomorrow"
                        )))
            // Check if this is a ready for handover notification (use custom handover2 icon)
            val isHandoverToday =
                notification.title.contains("Ready for handover", ignoreCase = true) ||
                        (title.contains("upcoming project update") && message.contains("handover") && message.contains(
                            "scheduled for today"
                        ))
            // For green play icon, use solid green background with white icon and light green halo (only for non-active play icons)
            val isGreenPlayIcon =
                icon == Icons.Default.PlayArrow && iconColor == Color(0xFF06F610) && !isPhaseStarting && !isPhaseEnded && !isCompleted && !isStandby && !isSuspension && !isSuspensionEndingSoon && !isActive && !isMaintenance && !isMaintenanceStartingSoon && !isUpcomingProjectStart && !isHandoverTomorrow && !isHandoverToday
            // Special handling for active project status updates - use solid green with light green halo
            val isActiveProjectStatus = isActive && icon == Icons.Default.PlayArrow
            val backgroundColor = if (isGreenPlayIcon) {
                iconColor // Solid green background
            } else {
                iconColor.copy(alpha = 0.15f) // Light background for other icons
            }
            val iconTint = if (isGreenPlayIcon) {
                Color.White // White icon for green play button
            } else {
                iconColor // Colored icon for others
            }

            // For active project status updates - use solid green circle with light green halo
            if (isActiveProjectStatus) {
                Box(
                    modifier = Modifier
                        .size(30.dp) // Outer circle size (reduced by 2)
                        .background(
                            color = Color(0xFF06F610).copy(alpha = 0.15f), // Light green halo
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner solid green circle
                    Box(
                        modifier = Modifier
                            .size(14.dp) // Reduced by 2
                            .background(
                                color = Color(0xFF06F610), // Solid green background
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = Color.White, // White play icon
                            modifier = Modifier.size(10.dp) // Reduced by 2
                        )
                    }
                }
            } else if (isGreenPlayIcon) {
                Box(
                    modifier = Modifier
                        .size(32.dp) // Same size as other icons
                        .background(
                            color = backgroundColor, // Solid green background
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isCompleted) {
                // Use custom check icon for completed project status updates with light blue background
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = Color(0xFFE3F2FD), // Light blue background
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.check),
                        contentDescription = null,
                        tint = Color(0xFF03A9F4), // Light blue color for check icon
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isStandby) {
                // Use pause circle icon for standby project status updates
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = Color(0xFF9E9E9E).copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E), // Grey - pause circle icon
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isSuspensionEndingSoon) {
                // Use warning icon for suspension ending soon with orange background
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = Color(0xFFFF9800).copy(alpha = 0.15f), // Light orange background
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color(0xFFFF9800), // Orange icon
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isSuspension) {
                // Use warning icon for suspension project status updates with orange background
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = Color(0xFFFF9800).copy(alpha = 0.15f), // Light orange background
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color(0xFFFF9800), // Orange icon
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isPhaseEnded) {
                // Use custom phase completed icon for phase ended/completed notifications with original colors
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = Color(0xFF06F610).copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.phasecompleted),
                        contentDescription = null,
                        tint = Color.Unspecified, // Use original colors from drawable
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isPhaseStarting) {
                // Use custom flag icon for phase starting notifications with green tint
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = Color(0xFF06F610).copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.flag1),
                        contentDescription = null,
                        tint = Color(0xFF06F610), // Green color for flag icon
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isMaintenanceStartingSoon) {
                // Use houricon with purple tint for Maintenance Starting Soon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = Color(0xFF9C27B0).copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.houricon),
                        contentDescription = null,
                        tint = Color(0xFF9C27B0), // Purple tint
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isMaintenanceProjectStatusUpdate && !isMaintenanceStartingSoon) {
                // Use maintenance.png icon for "Project Status Update : MAINTENANCE"
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = backgroundColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.maintenance),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isMaintenance && !isMaintenanceStartingSoon && !isMaintenanceProjectStatusUpdate) {
                // Use houricon for maintenance notifications that are not "Maintenance Starting Soon" and not "Project Status Update : MAINTENANCE"
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = backgroundColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.houricon),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isUpcomingProjectStart) {
                // Use custom houricon for upcoming project update going to start
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = backgroundColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.houricon),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isHandoverTomorrow) {
                // Use custom handover1 icon for handover tomorrow notifications with gradient background
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Green.copy(alpha = 0.15f),
                                    Color.Blue.copy(alpha = 0.15f),
                                    Color(0xFF9C27B0).copy(alpha = 0.15f) // Purple
                                ),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f), // topLeading
                                end = androidx.compose.ui.geometry.Offset(
                                    32f,
                                    32f
                                ) // bottomTrailing
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.handover1),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (isHandoverToday) {
                // Use custom handover2 icon for ready for handover notifications with gradient background
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Green.copy(alpha = 0.15f),
                                    Color.Blue.copy(alpha = 0.15f),
                                    Color(0xFF9C27B0).copy(alpha = 0.15f) // Purple
                                ),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f), // topLeading
                                end = androidx.compose.ui.geometry.Offset(
                                    32f,
                                    32f
                                ) // bottomTrailing
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.handover1),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = backgroundColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Notification content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title - matching screenshot style (remove emojis/icons from right side)
                var cleanTitle = notification.title
                    .replace("🔔", "")
                    .replace("⏳", "")
                    .replace("💬", "")
                    .replace("🔴", "")
                    .replace("🟡", "")
                    .replace("🟢", "")
                    .replace("🚀", "")
                    .replace(
                        Regex("[\uD83C-\uDBFF\uDC00-\uDFFF]+"),
                        ""
                    ) // Remove all emojis (Unicode ranges)
                    .replace(Regex("[\u2600-\u27BF]"), "") // Remove additional emoji ranges
                    .trim()

                // Change title for Project Status Update notifications to include status
                val titleLower = cleanTitle.lowercase()
                val messageLower = notification.message.lowercase()
                if (titleLower.contains("project status update")) {
                    val status = when {
                        messageLower.contains("is now active") || messageLower.contains("active") -> "ACTIVE"
                        messageLower.contains("is now completed") || messageLower.contains("completed") -> "COMPLETED"
                        messageLower.contains("is now maintenance") || messageLower.contains("maintenance") -> "MAINTENANCE"
                        messageLower.contains("is now standby") || messageLower.contains("standby") -> "STANDBY"
                        messageLower.contains("is now suspension") || messageLower.contains("suspension") -> "SUSPENSION"
                        else -> null
                    }
                    if (status != null) {
                        cleanTitle = "Project Status Update : $status"
                    }
                }

                // Change title for maintenance notifications
                val isMaintenanceUpcoming =
                    cleanTitle.lowercase().contains("upcoming project update") &&
                            notification.message.lowercase().contains("going to maintenance")
                if (isMaintenanceUpcoming) {
                    val messageLower = notification.message.lowercase()
                    if (messageLower.contains("tomorrow")) {
                        cleanTitle = "Maintenance Starts Tomorrow"
                    } else {
                        // Check if it's more than 2 days away by looking for day numbers or "in X days"
                        val daysPattern = Regex("in (\\d+) days?|(\\d+) days?")
                        val daysMatch = daysPattern.find(messageLower)
                        val daysAway = daysMatch?.groupValues?.get(1)?.toIntOrNull()
                            ?: daysMatch?.groupValues?.get(2)?.toIntOrNull()

                        if (daysAway != null && daysAway > 2) {
                            cleanTitle = "Maintenance Starting Soon"
                        } else if (daysAway == null || daysAway > 2) {
                            // Default to "Starting Soon" if we can't determine or it's more than 2 days
                            cleanTitle = "Maintenance Starting Soon"
                        } else {
                            // If 2 days or less but not tomorrow, keep original or use "Starting Soon"
                            cleanTitle = "Maintenance Starting Soon"
                        }
                    }
                }

                // Change title for completion notifications
                val isCompletionUpcoming =
                    cleanTitle.lowercase().contains("upcoming project update") &&
                            notification.message.lowercase().contains("going to complete") &&
                            !notification.message.lowercase().contains("going to maintenance")
                if (isCompletionUpcoming) {
                    val message = notification.message
                    val messageLower = message.lowercase()

                    // Extract project name from message (e.g., "Abcd is going to complete tomorrow" -> "Abcd")
                    // Try multiple patterns to extract project name
                    var projectName = "Project"
                    val patterns = listOf(
                        Regex(
                            "^([A-Za-z0-9\\s]+?)\\s+is\\s+going\\s+to\\s+complete",
                            RegexOption.IGNORE_CASE
                        ),
                        Regex("^([A-Za-z0-9\\s]+?)\\s+will\\s+complete", RegexOption.IGNORE_CASE),
                        Regex("^([A-Za-z0-9\\s]+?)\\s+is\\s+completing", RegexOption.IGNORE_CASE)
                    )

                    for (pattern in patterns) {
                        val match = pattern.find(message)
                        if (match != null) {
                            projectName = match.groupValues[1].trim()
                            break
                        }
                    }

                    if (messageLower.contains("tomorrow")) {
                        cleanTitle = "$projectName Completes Tomorrow"
                    } else {
                        // For other cases, use "Project Completes Soon" or similar
                        cleanTitle = "$projectName Completes Soon"
                    }
                }

                // Change title for handover notifications
                val isHandoverUpcoming =
                    cleanTitle.lowercase().contains("upcoming project update") &&
                            notification.message.lowercase().contains("handover") &&
                            notification.message.lowercase().contains("scheduled for tomorrow")
                if (isHandoverUpcoming) {
                    cleanTitle = "Handover Tomorrow"
                }

                // Change title for handover notifications scheduled for today
                val isHandoverToday = cleanTitle.lowercase().contains("upcoming project update") &&
                        notification.message.lowercase().contains("handover") &&
                        notification.message.lowercase().contains("scheduled for today")
                if (isHandoverToday) {
                    cleanTitle = "Ready for handover"
                }

                // Change title for "Upcoming Project Update" to "Upcoming Project" when message is "{Project name} is going to start tomorrow"
                val isUpcomingProjectStart =
                    cleanTitle.lowercase().contains("upcoming project update") &&
                            notification.message.lowercase().contains("is going to start tomorrow")
                if (isUpcomingProjectStart) {
                    cleanTitle = cleanTitle.replace(
                        Regex(
                            "Upcoming Project Update",
                            RegexOption.IGNORE_CASE
                        ), "Upcoming Project"
                    )
                }

                // Change title "Phase Ended Today" to "Phase Completed"
                if (cleanTitle.contains("Phase Ended Today", ignoreCase = true)) {
                    cleanTitle = cleanTitle.replace(
                        Regex("Phase Ended Today", RegexOption.IGNORE_CASE),
                        "Phase Completed"
                    )
                }

                // Style title with colored COMPLETED, ACTIVE, MAINTENANCE, or STANDBY text
                val styledTitle = if (cleanTitle.contains("COMPLETED")) {
                    buildAnnotatedString {
                        val parts = cleanTitle.split("COMPLETED")
                        append(parts[0])
                        withStyle(style = SpanStyle(color = Color(0xFFE3F2FD))) {
                            append("COMPLETED")
                        }
                        if (parts.size > 1) {
                            append(parts[1])
                        }
                    }
                } else if (cleanTitle.contains("ACTIVE")) {
                    buildAnnotatedString {
                        val parts = cleanTitle.split("ACTIVE")
                        append(parts[0])
                        withStyle(style = SpanStyle(color = Color(0xFF06F610))) {
                            append("ACTIVE")
                        }
                        if (parts.size > 1) {
                            append(parts[1])
                        }
                    }
                } else if (cleanTitle.contains("MAINTENANCE")) {
                    buildAnnotatedString {
                        val parts = cleanTitle.split("MAINTENANCE")
                        append(parts[0])
                        withStyle(style = SpanStyle(color = Color(0xFF9C27B0))) {
                            append("MAINTENANCE")
                        }
                        if (parts.size > 1) {
                            append(parts[1])
                        }
                    }
                } else if (cleanTitle.contains("STANDBY")) {
                    buildAnnotatedString {
                        val parts = cleanTitle.split("STANDBY")
                        append(parts[0])
                        withStyle(style = SpanStyle(color = Color(0xFFE91E63))) {
                            append("STANDBY")
                        }
                        if (parts.size > 1) {
                            append(parts[1])
                        }
                    }
                } else {
                    AnnotatedString(cleanTitle)
                }

                Text(
                    text = styledTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Transform message text: replace "going to maintenance status" with "going into maintenance phase"
                // Also replace "is going to complete tomorrow" with "is completing tomorrow"
                // Also replace "is going to complete in X days" with "is coming to an end in X days"
                val displayMessage = notification.message
                    .replace(
                        Regex("going to maintenance status", RegexOption.IGNORE_CASE),
                        "going into maintenance phase"
                    )
                    .replace(
                        Regex("is going to complete tomorrow", RegexOption.IGNORE_CASE),
                        "is scheduled to complete tomorrow"
                    )
                    .replace(
                        Regex("is going to complete in (\\d+) days?", RegexOption.IGNORE_CASE),
                        "is coming to an end in $1 days"
                    )
                    .replace(
                        Regex(
                            "Suspension status of the (.+?) ends tomorrow",
                            RegexOption.IGNORE_CASE
                        ),
                        "Suspension for $1 ends tomorrow"
                    )
                    .replace(
                        Regex("(.+?) of (.+?) ended today", RegexOption.IGNORE_CASE),
                        "$1 of $2 completed today"
                    )
                    .replace(
                        Regex("(.+?) handover is scheduled for today", RegexOption.IGNORE_CASE),
                        "$1 is ready for handover"
                    )

                Text(
                    text = displayMessage,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Time on right - removed badge, only show time
            Text(
                text = notification.createdAt?.let { FormatUtils.formatTimeAgo(it) } ?: "Just now",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        Divider(
            modifier = Modifier.padding(start = 60.dp),
            color = Color.LightGray.copy(alpha = 0.3f)
        )
    }
}

/**
 * Get notification icon based on type and content - matching reference icons
 */
@Composable
private fun getNotificationIconInfo(
    notification: Notification
): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    val title = notification.title.lowercase()
    val message = notification.message.lowercase()
    
    return when {
        // Green Check Icon - Project Status Update is Completed (use custom check.png)
        (title.contains("project status update") && message.contains("is now completed")) ||
        (title.contains("project status update") && message.contains("completed")) -> {
            // Return a special marker to use custom check icon
            Pair(Icons.Default.CheckCircle, Color(0xFF4CAF50)) // Green - will be replaced with check.png
        }
        // Pause Circle Icon - Project Status Update is Standby
        (title.contains("project status update") && message.contains("is now standby")) ||
        (title.contains("project status update") && message.contains("standby")) -> {
            // Return pause circle icon for standby
            Pair(Icons.Default.PauseCircle, Color(0xFF9E9E9E)) // Grey - pause circle icon
        }
        // Warning Icon - Suspension Ending Soon (orange warning icon)
        (title.contains("suspension ending soon", ignoreCase = true)) -> {
            // Return warning icon for suspension ending soon
            Pair(Icons.Default.Warning, Color(0xFFFF9800)) // Orange - warning icon
        }
        // Warning Icon - Project Status Update is Suspension
        (title.contains("project status update") && message.contains("is now suspension")) ||
        (title.contains("project status update") && message.contains("suspension")) -> {
            // Return warning icon for suspension
            Pair(Icons.Default.Warning, Color(0xFFFF9800)) // Orange - warning icon
        }
        // Red Warning Icon - Project Completion Reminder (3 days remaining)
        (title.contains("project") && (title.contains("complete") || message.contains("complete") || message.contains("going to complete"))) ||
        (message.contains("is now completed") && !title.contains("project status update")) -> {
            Pair(Icons.Default.Warning, Color(0xFFF44336)) // Red
        }
        // Yellow Warning Icon - Phase Completion Reminder (3 days remaining)
        (title.contains("phase") && (title.contains("end") || message.contains("going to end"))) ||
        (title.contains("phase ending")) -> {
            Pair(Icons.Default.Warning, Color(0xFFFFEB3B)) // Yellow
        }
        // Green Play Icon - Project Status Update is Active or going to Active (green circle with white play triangle)
        (title.contains("project status update") && (message.contains("is now active") || message.contains("active"))) ||
        (title.contains("project status update") && (message.contains("going to active") || message.contains("going to start"))) ||
        (message.contains("is now active") && !title.contains("project status update")) ||
        (title.contains("upcoming project update") && message.contains("going to start")) ||
        (title.contains("project started") || message.contains("project started")) -> {
            Pair(Icons.Default.PlayArrow, Color(0xFF06F610)) // Green background with new color, white icon
        }
        // Green Flag Icon - Phase Starting Tomorrow (using custom flag icon)
        (title.contains("phase") && (title.contains("start") || message.contains("going to start"))) ||
        (title.contains("phase starting")) -> {
            // Return a special marker to use custom flag icon
            Pair(Icons.Default.PlayArrow, Color(0xFF4CAF50)) // Green - will be replaced with flag icon
        }
        // Purple Tools Icon - Project has transitioned into Maintenance Phase today
        (message.contains("maintenance") || message.contains("is now maintenance")) -> {
            Pair(Icons.Default.Build, Color(0xFF9C27B0)) // Purple
        }
        // Orange Bell and Clock Icon - Project Update (budget/timeline update)
        // Includes: Maintenance Date Changed, budget updates, timeline updates
        (title.contains("maintenance date changed") || message.contains("maintenance date changed")) ||
        (notification.type == NotificationType.PROJECT_CHANGED && !message.contains("start") && !message.contains("complete") && !message.contains("is now maintenance") && !message.contains("is now active")) ||
        (title.contains("project status update") && !message.contains("start") && !message.contains("complete") && !message.contains("is now maintenance") && !message.contains("is now active")) ||
        (title.contains("project update") && !message.contains("start") && !message.contains("complete") && !message.contains("is now maintenance") && !message.contains("is now active")) -> {
            Pair(Icons.Default.AccessTime, Color(0xFFFF9800)) // Orange
        }
        // Light Blue/Grey People Icon - Project delegation
        (notification.type == NotificationType.DELEGATION_CHANGED ||
        notification.type == NotificationType.DELEGATION_EXPIRED ||
        notification.type == NotificationType.DELEGATION_REMOVED ||
        notification.type == NotificationType.DELEGATION_RESPONSE ||
        title.contains("delegation")) -> {
            Pair(Icons.Default.People, Color(0xFF90A4AE)) // Light Blue/Grey
        }
        // Blue Chat and Document Icon - A new chat by X for a particular logged expense
        (notification.type == NotificationType.CHAT_MESSAGE && (message.contains("expense") || message.contains("logged") || title.contains("expense"))) -> {
            Pair(Icons.Default.Chat, Color(0xFF2196F3)) // Blue chat bubble
        }
        // Purple Chat Icon - A new chat in a project by X
        (notification.type == NotificationType.CHAT_MESSAGE) -> {
            Pair(Icons.Default.Chat, Color(0xFF9C27B0)) // Purple chat bubble
        }
        // Default - Orange Bell and Clock for general updates
        else -> {
            Pair(Icons.Default.AccessTime, Color(0xFFFF9800)) // Orange
        }
    }
}

@Composable
private fun getNotificationIcon(notificationType: NotificationType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (notificationType) {
        NotificationType.EXPENSE_APPROVED -> Icons.Default.CheckCircle
        NotificationType.EXPENSE_REJECTED -> Icons.Default.CheckCircle
        NotificationType.EXPENSE_SUBMITTED -> Icons.Default.Notifications
        NotificationType.PENDING_APPROVAL -> Icons.Default.Notifications
        NotificationType.PROJECT_ASSIGNMENT -> Icons.Default.Notifications
        else -> Icons.Default.Notifications
    }
}

@Composable
private fun getNotificationIconColor(notificationType: NotificationType): Color {
    return when (notificationType) {
        NotificationType.EXPENSE_APPROVED -> Color(0xFF4CAF50) // Green
        NotificationType.EXPENSE_REJECTED -> Color(0xFFF44336) // Red
        NotificationType.EXPENSE_SUBMITTED -> Color(0xFF2196F3) // Blue
        NotificationType.PENDING_APPROVAL -> Color(0xFFFF9800) // Orange
        NotificationType.PROJECT_ASSIGNMENT -> Color(0xFF9C27B0) // Purple
        else -> Color(0xFF9E9E9E) // Gray
    }
}

@Composable
private fun DeclinedProjectCard(
    project: Project,
    rejectedByName: String,
    onProjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val rejectedDate = project.updatedAt?.toDate()?.let { dateFormatter.format(it) } ?: "Pending sync"
    
    // No Card wrapper - just content with clickable
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProjectClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
            // Project Name
            Text(
                text = project.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            // Rejection Reason (in red) - matching screenshot "Checking rejection reason"
            Text(
                text = if (!project.rejectionReason.isNullOrBlank()) {
                    project.rejectionReason ?: "Checking rejection reason"
                } else {
                    "Checking rejection reason"
                },
                fontSize = 14.sp,
                color = Color(0xFFF44336), // Red color matching screenshot
                lineHeight = 20.sp
            )
            
            // Date Rejected and Rejected By - side by side matching screenshot layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date Rejected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = rejectedDate,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                // Rejected By - matching screenshot with person icon (always show)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Rejected by: ${if (rejectedByName != "Loading..." && rejectedByName != "Unknown User") rejectedByName else "Unknown"}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
    }
}

/**
 * Section header with title and badge count
 */
@Composable
private fun SectionHeader(
    title: String,
    badgeCount: Int,
    badgeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title on the left
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray // Gray color matching reference image
        )
        
        // Badge on the extreme right
        if (badgeCount > 0) {
            Badge(
                containerColor = badgeColor,
                contentColor = Color.White
            ) {
                Text(
                    text = badgeCount.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Phase Request Card - matches screenshot design (no card, just content with divider)
 */
@Composable
private fun PhaseRequestCard(
    request: Map<String, Any>,
    authViewModel: AuthViewModel,
    onProjectClick: (String) -> Unit
) {
    val phaseName = (request["phaseName"] as? String) ?: "Phase"
    val projectName = (request["projectName"] as? String) ?: "Project"
    val projectId = (request["projectId"] as? String) ?: ""
    val userID = (request["userID"] as? String) ?: ""
    val extendedDate = (request["extendedDate"] as? String) ?: ""
    val createdAt = request["createdAt"] as? com.google.firebase.Timestamp
    
    // Fetch user name by phone number
    var userName by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(userID) {
        if (userID.isNotEmpty()) {
            try {
                val user = authViewModel.getUserByPhoneNumber(userID)
                userName = user?.name
            } catch (e: Exception) {
                Log.e("PhaseRequestCard", "Error fetching user name: ${e.message}")
            }
        }
    }
    
    // Format extended date - keep original format DD/MM/YYYY for display
    val displayExtendedDate = if (extendedDate.isNotEmpty()) {
        extendedDate
    } else {
        null
    }
    
    // Format date for right side display - show created date
    val dateText = createdAt?.let { 
        val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        dateFormatter.format(it.toDate())
    } ?: "Recently"
    
    // Display name: use fetched user name, fallback to phone number
    val displayName = userName ?: userID
    
    // No Card wrapper - just content with clickable
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (projectId.isNotEmpty()) onProjectClick(projectId) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Orange clock icon - circular shape matching screenshot
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = Color(0xFFFF9800).copy(alpha = 0.15f), // Slightly lighter orange background
                    shape = CircleShape // Circular shape matching screenshot
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.phaserequest),
                contentDescription = null,
                tint = Color(0xFFFF9800), // Darker orange icon
                modifier = Modifier.size(18.dp) // Slightly smaller icon
            )
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        // Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Phase name first (bold, black) - matching reference image
            Text(
                text = phaseName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Project name (blue) - matching reference image
            Text(
                text = projectName,
                fontSize = 14.sp,
                color = Color(0xFF4285F4), // Blue color matching reference
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            // User name (gray)
            Text(
                text = displayName,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Extend to date (blue) - matching reference image style
            if (displayExtendedDate != null) {
                Text(
                    text = "Extend to: $displayExtendedDate",
                    fontSize = 14.sp,
                    color = Color(0xFF4285F4), // Blue color matching reference
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Date on right (created date)
        Text(
            text = dateText,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

/**
 * Dialog for Accept/Reject Phase Request
 */
@Composable
private fun PhaseRequestAcceptRejectDialog(
    request: Map<String, Any>,
    onDismiss: () -> Unit,
    onAccept: (String, String) -> Unit,
    onReject: (String) -> Unit,
    projectViewModel: ProjectViewModel
) {
    val phaseName = (request["phaseName"] as? String) ?: "Phase"
    val projectName = (request["projectName"] as? String) ?: "Project"
    val userID = (request["userID"] as? String) ?: ""
    val extendedDate = (request["extendedDate"] as? String) ?: ""
    
    var showRejectReason by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var acceptReason by remember { mutableStateOf("") }
    val isLoading by projectViewModel.isLoadingPhaseRequests.collectAsState()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(if (showRejectReason) 0.7f else 0.5f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showRejectReason) "Reject Phase Request" else "Phase Request",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
                
                Divider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Color.LightGray.copy(alpha = 0.3f)
                )
                
                // Request Details
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Phase Name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Phase:",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.width(100.dp)
                        )
                        Text(
                            text = phaseName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    
                    // Project Name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Project:",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.width(100.dp)
                        )
                        Text(
                            text = projectName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                    }
                    
                    // User ID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Requested by:",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.width(100.dp)
                        )
                        Text(
                            text = userID,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                    
                    // Extended Date
                    if (extendedDate.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Extend to:",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.width(100.dp)
                            )
                            Text(
                                text = extendedDate,
                                fontSize = 14.sp,
                                color = Color(0xFF4285F4),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Reason Input
                    if (showRejectReason) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Reason for Rejection *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                            OutlinedTextField(
                                value = rejectReason,
                                onValueChange = { rejectReason = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        text = "Enter reason for rejection...",
                                        color = Color.Gray
                                    )
                                },
                                minLines = 3,
                                maxLines = 5,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF44336),
                                    unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    } else {
                        // Optional reason for acceptance
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Reason (Optional)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                            OutlinedTextField(
                                value = acceptReason,
                                onValueChange = { acceptReason = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        text = "Enter reason for acceptance...",
                                        color = Color.Gray
                                    )
                                },
                                minLines = 2,
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4CAF50),
                                    unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action Buttons
                if (showRejectReason) {
                    // Reject confirmation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel button
                        OutlinedButton(
                            onClick = { showRejectReason = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Confirm Reject button
                        Button(
                            onClick = {
                                if (rejectReason.isNotBlank()) {
                                    onReject(rejectReason)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = rejectReason.isNotBlank() && !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336),
                                disabledContainerColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reject",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    // Accept/Reject buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Reject button
                        OutlinedButton(
                            onClick = { showRejectReason = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF44336)
                            ),
                            border = BorderStroke(1.5.dp, Color(0xFFF44336)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reject",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Accept button
                        Button(
                            onClick = {
                                if (extendedDate.isNotEmpty()) {
                                    onAccept(extendedDate, acceptReason.ifBlank { "Approved" })
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading && extendedDate.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                disabledContainerColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Accept",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Extracts user name from notification message
 * Example: "6360090611 sent: hello" -> "6360090611"
 */
private fun extractUserNameFromMessage(message: String): String? {
    return try {
        val parts = message.split(" sent:")
        if (parts.isNotEmpty()) {
            parts[0].trim()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Simplified project card for background display behind notification dialog
 */
@Composable
private fun BusinessHeadProjectCardBackground(
    project: Project,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = false) { }, // Disabled - dialog is open
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Project icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4285F4).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = null,
                    tint = Color(0xFF4285F4),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Project details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = project.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (project.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = project.description,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹",
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = FormatUtils.formatCurrency(project.budget),
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
} 