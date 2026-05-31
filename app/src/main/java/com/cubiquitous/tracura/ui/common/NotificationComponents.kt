package com.cubiquitous.tracura.ui.common

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.cubiquitous.tracura.model.Notification
import com.cubiquitous.tracura.model.NotificationBadge
import com.cubiquitous.tracura.model.NotificationType
import com.cubiquitous.tracura.model.ProjectNotificationSummary
import com.cubiquitous.tracura.utils.FormatUtils

@Composable
fun NotificationBadgeComponent(
    badge: NotificationBadge,
    modifier: Modifier = Modifier
) {
    if (badge.hasUnread && badge.count > 0) {
        Box(
            modifier = modifier
                .offset(x = 6.dp, y = (-2).dp)
                .size(18.dp)
                .background(
                    color = Color.Red,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (badge.count > 99) "99+" else badge.count.toString(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun NotificationPulseIndicator(
    hasUnread: Boolean,
    modifier: Modifier = Modifier
) {
    if (hasUnread) {
        var isAnimating by remember { mutableStateOf(true) }
        
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                isAnimating = !isAnimating
            }
        }
        
        Box(
            modifier = modifier
                .size(12.dp)
                .background(
                    color = if (isAnimating) Color.Red else Color.Red.copy(alpha = 0.6f),
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun NotificationCard(
    notification: Notification,
    onNotificationClick: (Notification) -> Unit,
    onMarkAsRead: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (notification.isRead) {
        Color.Transparent
    } else {
        Color(0xFFF3E5F5) // Light purple for unread
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNotificationClick(notification) }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 2.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Notification Icon
            NotificationIcon(
                type = notification.type,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Notification Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title,
                    fontSize = 16.sp,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    color = if (notification.isRead) Color.Gray else Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    color = if (notification.isRead) Color.Gray else Color.Black.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Project name
                    if (notification.projectName.isNotBlank()) {
                        Text(
                            text = notification.projectName,
                            fontSize = 12.sp,
                            color = Color(0xFF8B5FBF),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.background(
                                color = Color(0xFF8B5FBF).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ).padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    
                    // Timestamp
                    Text(
                        text = FormatUtils.formatTimeAgo(notification.createdAt),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // Mark as read button
            if (!notification.isRead) {
                IconButton(
                    onClick = { onMarkAsRead(notification.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Mark as Read",
                        tint = Color(0xFF8B5FBF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationIcon(
    type: NotificationType,
    modifier: Modifier = Modifier
) {
    val icon: androidx.compose.ui.graphics.vector.ImageVector
    val tint: androidx.compose.ui.graphics.Color
    
    when (type) {
        NotificationType.PROJECT_ASSIGNMENT -> {
            icon = Icons.Default.Add
            tint = Color(0xFF4CAF50)
        }
        NotificationType.PROJECT_CHANGED -> {
            icon = Icons.Default.Edit
            tint = Color(0xFF9C27B0)
        }
        NotificationType.EXPENSE_SUBMITTED -> {
            icon = Icons.Default.Create
            tint = Color(0xFFFF9800)
        }
        NotificationType.EXPENSE_APPROVED -> {
            icon = Icons.Default.CheckCircle
            tint = Color(0xFF4CAF50)
        }
        NotificationType.EXPENSE_REJECTED -> {
            icon = Icons.Default.Close
            tint = Color(0xFFF44336)
        }
        NotificationType.PENDING_APPROVAL -> {
            icon = Icons.Default.Notifications
            tint = Color(0xFFFF9800)
        }
        NotificationType.ROLE_ASSIGNMENT -> {
            icon = Icons.Default.Person
            tint = Color(0xFF2196F3)
        }
        NotificationType.TEMPORARY_APPROVER_ASSIGNMENT -> {
            icon = Icons.Default.Person
            tint = Color(0xFFFF9800)
        }
        NotificationType.DELEGATION_CHANGED -> {
            icon = Icons.Default.Edit
            tint = Color(0xFF9C27B0)
        }
        NotificationType.DELEGATION_EXPIRED -> {
            icon = Icons.Default.Schedule
            tint = Color(0xFFFF9800)
        }
        NotificationType.DELEGATION_REMOVED -> {
            icon = Icons.Default.Delete
            tint = Color(0xFFF44336)
        }
        NotificationType.DELEGATION_RESPONSE -> {
            icon = Icons.Default.CheckCircle
            tint = Color(0xFF4CAF50)
        }
        NotificationType.CHAT_MESSAGE -> {
            icon = Icons.Default.Chat
            tint = Color(0xFF2196F3)
        }
        NotificationType.INFO -> {
            icon = Icons.Default.Info
            tint = Color(0xFF607D8B)
        }
    }
    
    Icon(
        imageVector = icon,
        contentDescription = type.name,
        tint = tint,
        modifier = modifier
    )
}

@Composable
fun NotificationsList(
    notifications: List<Notification>,
    onNotificationClick: (Notification) -> Unit,
    onMarkAsRead: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notifications) { notification ->
            NotificationCard(
                notification = notification,
                onNotificationClick = onNotificationClick,
                onMarkAsRead = onMarkAsRead
            )
        }
    }
}

@Composable
fun ProjectNotificationSummaryCard(
    summary: ProjectNotificationSummary,
    onProjectClick: (String) -> Unit,
    onNotificationClick: (Notification) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProjectClick(summary.projectId) }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (summary.unreadCount > 0) Color(0xFFF3E5F5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Project header with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.projectName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                
                if (summary.unreadCount > 0) {
                    NotificationBadgeComponent(
                        badge = NotificationBadge(count = summary.unreadCount, hasUnread = true)
                    )
                }
            }
            
            if (summary.unreadCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Notification summary counts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (summary.pendingApprovals > 0) {
                        NotificationTypeChip(
                            text = "${summary.pendingApprovals} Pending",
                            color = Color(0xFFFF9800),
                            onClick = { onProjectClick(summary.projectId) }
                        )
                    }
                    
                    if (summary.expenseUpdates > 0) {
                        NotificationTypeChip(
                            text = "${summary.expenseUpdates} Updates",
                            color = Color(0xFF4CAF50),
                            onClick = { onProjectClick(summary.projectId) }
                        )
                    }
                }
                
                // Latest notification preview
                summary.latestNotification?.let { notification ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = notification.message,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onNotificationClick(notification) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationTypeChip(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmptyNotificationsState(
    title: String = "No Notifications",
    subtitle: String = "You're all caught up!",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = "No notifications",
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Notifications will appear here when:\n• Your expenses are approved or rejected\n• You're assigned to new projects\n• Important updates are available",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
} 