package com.cubiquitous.tracura.ui.view.approver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.Notification
import com.cubiquitous.tracura.model.NotificationType
import com.cubiquitous.tracura.navigation.buildExpenseListNotificationTag
import com.cubiquitous.tracura.navigation.expenseListRouteArgument
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.viewmodel.NotificationViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.viewmodel.TemporaryApproverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproverNotificationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToApproverProjectDashboard: (String, String) -> Unit,
    onNavigateToApproverProjectReview: (String) -> Unit = { _ -> }, // Navigation to project review screen
    onNavigateToPendingApprovals: (String) -> Unit,
    onNavigateToChat: (String, String, String) -> Unit = { _, _, _ -> }, // projectId, chatId, otherUserName
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel,
    temporaryApproverViewModel: TemporaryApproverViewModel = hiltViewModel()
) {
    val notifications by notificationViewModel.notifications.collectAsState()
    val isLoading by notificationViewModel.isLoading.collectAsState()
    val error by notificationViewModel.error.collectAsState()
    val notificationBadge by notificationViewModel.notificationBadge.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    
    // Load notifications when screen opens
    LaunchedEffect(Unit) {
        val currentUserId = authState.user?.phone
        if (currentUserId != null) {
            notificationViewModel.loadAllNotifications(currentUserId)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Approver Notifications",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (notificationBadge.hasUnread) {
                        Badge(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = notificationBadge.count.toString(),
                                fontSize = 12.sp
                            )
                        }
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
                IconButton(
                    onClick = {
                        val currentUserId = authState.user?.phone
                        if (currentUserId != null) {
                            notificationViewModel.loadAllNotifications(currentUserId)
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh Notifications",
                        tint = Color.White
                    )
                }
                
                if (notificationBadge.hasUnread) {
                    TextButton(
                        onClick = {
                            val currentUserId = authState.user?.phone
                            if (currentUserId != null) {
                                notificationViewModel.markAllNotificationsAsRead(currentUserId)
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
                                val currentUserId = authState.user?.phone
                                if (currentUserId != null) {
                                    notificationViewModel.loadNotifications(currentUserId)
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
            notifications.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = "No Notifications",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Text(
                            text = "You'll see notifications here when there are updates about your projects",
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                    items(notifications) { notification ->
                        ApproverNotificationItem(
                            notification = notification,
                            onNotificationClick = {
                                handleApproverNotificationClick(
                                    notification = notification,
                                    onNavigateToApproverProjectDashboard = onNavigateToApproverProjectDashboard,
                                    onNavigateToApproverProjectReview = onNavigateToApproverProjectReview,
                                    onNavigateToPendingApprovals = onNavigateToPendingApprovals,
                                    onNavigateToChat = onNavigateToChat,
                                    onMarkAsRead = {
                                        notificationViewModel.markNotificationAsRead(notification.id)
                                    }
                                )
                            },
                            onAcceptAssignment = { projectId, approverId ->
                                temporaryApproverViewModel.acceptTemporaryApproverAssignment(
                                    projectId = projectId,
                                    approverId = approverId
                                )
                                notificationViewModel.markNotificationAsRead(notification.id)
                            },
                            onRejectAssignment = { projectId, approverId ->
                                temporaryApproverViewModel.rejectTemporaryApproverAssignment(
                                    projectId = projectId,
                                    approverId = approverId
                                )
                                notificationViewModel.markNotificationAsRead(notification.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApproverNotificationItem(
    notification: Notification,
    onNotificationClick: () -> Unit,
    onAcceptAssignment: (String, String) -> Unit = { _, _ -> },
    onRejectAssignment: (String, String) -> Unit = { _, _ -> }
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
                    maxLines = 3,
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
                    
                    // Show action required badge for notifications that need action (excluding temporary approver assignments)
                    if (notification.actionRequired && 
                        !notification.isRead && 
                        notification.type != NotificationType.TEMPORARY_APPROVER_ASSIGNMENT) {
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

private fun handleApproverNotificationClick(
    notification: Notification,
    onNavigateToApproverProjectDashboard: (String, String) -> Unit,
    onNavigateToApproverProjectReview: (String) -> Unit,
    onNavigateToPendingApprovals: (String) -> Unit,
    onNavigateToChat: (String, String, String) -> Unit,
    onMarkAsRead: () -> Unit
) {
    // Mark notification as read
    onMarkAsRead()

    val fromNotification = expenseListRouteArgument(
        buildExpenseListNotificationTag(
            notificationType = notification.type.name,
            navigationTarget = notification.navigationTarget,
            notifType = notification.type,
            title = notification.title,
            message = notification.message
        )
    )
    
    // For approvers, navigate based on notification type
    if (notification.projectId.isNotEmpty()) {
        when (notification.type) {
            NotificationType.DELEGATION_REMOVED -> {
                // For delegation removed notifications, just mark as read and don't navigate
                // The notification will disappear from the list
                return
            }
            NotificationType.PROJECT_ASSIGNMENT -> {
                // For new project assignments, navigate to project review screen
                onNavigateToApproverProjectReview(notification.projectId)
            }
            NotificationType.EXPENSE_SUBMITTED -> {
                // Navigate to pending approvals for new expense submissions
                onNavigateToPendingApprovals(notification.projectId)
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
                } else {
                    // Fallback to project dashboard if navigation target is invalid
                    onNavigateToApproverProjectDashboard(notification.projectId, fromNotification)
                }
            }
            else -> {
                // For other notification types, navigate to project dashboard
                onNavigateToApproverProjectDashboard(notification.projectId, fromNotification)
            }
        }
    }
}
