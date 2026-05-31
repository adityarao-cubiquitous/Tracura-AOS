package com.cubiquitous.tracura.ui.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.Notification
import com.cubiquitous.tracura.model.NotificationType
import com.cubiquitous.tracura.navigation.buildExpenseListNotificationTag
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.viewmodel.NotificationViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectNotificationScreen(
    projectId: String,
    projectName: String,
    onNavigateBack: () -> Unit,
    onNavigateToProject: (String, String, String) -> Unit,
    onNavigateToExpense: (String, String, String) -> Unit,
    onNavigateToPendingApprovals: (String, String) -> Unit,
    onNavigateToChat: (String, String, String) -> Unit = { _, _, _ -> }, // projectId, chatId, otherUserName
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel
) {
    val projectNotifications by notificationViewModel.projectNotifications.collectAsState()
    val isLoading by notificationViewModel.isLoading.collectAsState()
    val error by notificationViewModel.error.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    // Notification identity: phone-first (same as BusinessHeadProjectsTab), fallback to UID
    val notificationPrimaryUserId = remember(authState.user?.phone, authState.user?.uid) {
        when {
            !authState.user?.phone.isNullOrBlank() -> authState.user?.phone!!.trim()
            !authState.user?.uid.isNullOrBlank() -> authState.user?.uid!!.trim()
            else -> ""
        }
    }
    val notificationAlternateUserIds = remember(
        authState.user?.phone,
        authState.user?.uid,
        notificationPrimaryUserId
    ) {
        buildList {
            val phone = authState.user?.phone?.trim().orEmpty()
            val uid = authState.user?.uid?.trim().orEmpty()
            if (phone.isNotEmpty() && phone != notificationPrimaryUserId) add(phone)
            if (uid.isNotEmpty() && uid != notificationPrimaryUserId) add(uid)
        }.distinct()
    }
    
    // Keep notification filtering bound to this project
    LaunchedEffect(projectId) {
        notificationViewModel.setProjectId(projectId)
    }

    // Load notifications for this user and let ViewModel filter by projectId
    LaunchedEffect(projectId, notificationPrimaryUserId, notificationAlternateUserIds) {
        if (notificationPrimaryUserId.isNotEmpty()) {
            notificationViewModel.loadNotifications(notificationPrimaryUserId, notificationAlternateUserIds)
        }
    }

    // Keep notifications synced while this screen is visible
    LaunchedEffect(notificationPrimaryUserId, notificationAlternateUserIds, projectId) {
        if (notificationPrimaryUserId.isNotEmpty()) {
            while (true) {
                delay(10_000L)
                notificationViewModel.refreshFromLocalStorage(
                    notificationPrimaryUserId,
                    notificationAlternateUserIds
                )
            }
        }
    }

    val unreadCount = projectNotifications.count { !it.isRead }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top Bar
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = projectName,
                            fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                        )
                    if (unreadCount > 0) {
                            Text(
                            text = "$unreadCount unread notifications",
                                fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                        )
                    }
                },
                actions = {
                if (unreadCount > 0) {
                    TextButton(
                            onClick = { 
                            if (notificationPrimaryUserId.isNotEmpty()) {
                                notificationViewModel.markAllNotificationsAsRead(notificationPrimaryUserId)
                            }
                            }
                        ) {
                        Text(
                            text = "Mark All Read",
                            color = Color.White,
                            fontSize = 14.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF8B5FBF)
            )
        )
        
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                    CircularProgressIndicator(color = Color(0xFF8B5FBF))
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                            text = "Error Loading Notifications",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                            Text(
                            text = error ?: "Unknown error",
                                color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                            )
                            Button(
                                onClick = { 
                                notificationViewModel.clearError()
                                if (notificationPrimaryUserId.isNotEmpty()) {
                                    notificationViewModel.refreshNotifications(notificationPrimaryUserId)
                                }
                                },
                                colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B5FBF)
                            ),
                            modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                projectNotifications.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "No Project Notifications",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "You'll see notifications here when there are updates for this project",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                    )
                }
                }
            }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    items(projectNotifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onNotificationClick = {
                                handleNotificationClick(
                                    notification = notification,
                                    onNavigateToProject = onNavigateToProject,
                                    onNavigateToExpense = onNavigateToExpense,
                                    onNavigateToPendingApprovals = onNavigateToPendingApprovals,
                                    onNavigateToChat = onNavigateToChat,
                                    onMarkAsRead = {
                                        notificationViewModel.markNotificationAsRead(notification.id)
                                    }
                                )
                            }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
@Composable
private fun NotificationItem(
    notification: Notification,
    onNotificationClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNotificationClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else Color(0xFFF0F8FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Read/Unread indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        color = if (notification.isRead) Color.Gray else Color(0xFF8B5FBF),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .padding(top = 2.dp)
            ) {
                if (notification.isRead) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Read",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Notification content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = FormatUtils.formatTimeAgo(notification.createdAt),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    if (notification.actionRequired && !notification.isRead) {
                        Badge(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "Action Required",
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun handleNotificationClick(
    notification: Notification,
    onNavigateToProject: (String, String, String) -> Unit,
    onNavigateToExpense: (String, String, String) -> Unit,
    onNavigateToPendingApprovals: (String, String) -> Unit,
    onNavigateToChat: (String, String, String) -> Unit,
    onMarkAsRead: () -> Unit
) {
    // Mark notification as read
    onMarkAsRead()
    val notificationMessage = notification.message
    val expenseListNotificationTag = buildExpenseListNotificationTag(
        notificationType = notification.type.name,
        navigationTarget = notification.navigationTarget,
        notifType = notification.type,
        title = notification.title,
        message = notification.message
    )
    
    // Navigate based on notification type and navigation target
    when (notification.type) {
        NotificationType.DELEGATION_REMOVED -> {
            // For delegation removed notifications, just mark as read and don't navigate
            // The notification will disappear from the list
            return
        }
        NotificationType.EXPENSE_SUBMITTED -> {
            if (notification.projectId.isNotEmpty()) {
                onNavigateToPendingApprovals(notification.projectId, expenseListNotificationTag)
            }
        }
        NotificationType.EXPENSE_APPROVED, NotificationType.EXPENSE_REJECTED -> {
            if (notification.projectId.isNotEmpty() && notification.relatedId.isNotEmpty()) {
                onNavigateToExpense(notification.projectId, notification.relatedId, expenseListNotificationTag)
            } else if (notification.projectId.isNotEmpty()) {
                onNavigateToProject(notification.projectId, expenseListNotificationTag, notificationMessage)
            }
        }
        NotificationType.PROJECT_ASSIGNMENT -> {
            if (notification.projectId.isNotEmpty()) {
                onNavigateToProject(notification.projectId, expenseListNotificationTag, notificationMessage)
            }
        }
        NotificationType.PENDING_APPROVAL -> {
            if (notification.projectId.isNotEmpty()) {
                onNavigateToPendingApprovals(notification.projectId, expenseListNotificationTag)
            }
        }
        NotificationType.CHAT_MESSAGE -> {
            // Navigate to specific chat
            if (notification.navigationTarget.startsWith("chat/")) {
                val parts = notification.navigationTarget.split("/")
                if (parts.size >= 4) {
                    val projectId = parts[1]
                    val chatId = parts[2]
                    val otherUserName = parts[3]
                    // Navigate to chat screen
                    onNavigateToChat(projectId, chatId, otherUserName)
                }
            } else if (notification.projectId.isNotEmpty()) {
                onNavigateToProject(notification.projectId, expenseListNotificationTag, notificationMessage)
            }
        }
        else -> {
            // Default navigation based on navigation target
            when {
                notification.navigationTarget.startsWith("pending_approvals/") -> {
                    val projectId = notification.navigationTarget.substringAfter("pending_approvals/")
                    onNavigateToPendingApprovals(projectId, expenseListNotificationTag)
                }
                notification.navigationTarget.startsWith("expense_list/") -> {
                    val projectId = notification.navigationTarget.substringAfter("expense_list/")
                    onNavigateToProject(projectId, expenseListNotificationTag, notificationMessage)
                }
                notification.navigationTarget.startsWith("user_project_dashboard/") -> {
                    val projectId = notification.navigationTarget.substringAfter("user_project_dashboard/")
                    onNavigateToProject(projectId, expenseListNotificationTag, notificationMessage)
                }
                notification.navigationTarget.startsWith("approver_project_dashboard/") -> {
                    val projectId = notification.navigationTarget.substringAfter("approver_project_dashboard/")
                    onNavigateToProject(projectId, expenseListNotificationTag, notificationMessage)
                }
                notification.navigationTarget.startsWith("BUSINESS_HEAD_project_dashboard/") -> {
                    val projectId = notification.navigationTarget.substringAfter("BUSINESS_HEAD_project_dashboard/")
                    onNavigateToProject(projectId, expenseListNotificationTag, notificationMessage)
                }
                else -> {
                    // Default to project selection
                    if (notification.projectId.isNotEmpty()) {
                        onNavigateToProject(notification.projectId, expenseListNotificationTag, notificationMessage)
                    }
                }
            }
        }
    }
} 