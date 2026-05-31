package com.cubiquitous.tracura.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.ProjectNotificationSummary
import com.cubiquitous.tracura.utils.FormatUtils

@Composable
fun ProjectNotificationCard(
    project: Project,
    notificationSummary: ProjectNotificationSummary? = null,
    onProjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasUnreadNotifications = notificationSummary?.unreadCount ?: 0 > 0
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProjectClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (hasUnreadNotifications) Color(0xFFF3E5F5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (hasUnreadNotifications) 4.dp else 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Project Initial Circle with notification indicator
            Box(
                modifier = Modifier.size(56.dp)
            ) {
                // Project circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = if (hasUnreadNotifications) {
                                Color(0xFFE8F5E8) // Light green for projects with notifications
                            } else {
                                Color(0xFFE3F2FD) // Default blue
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getProjectInitials(project.name),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasUnreadNotifications) {
                            Color(0xFF4CAF50) // Green for projects with notifications
                        } else {
                            Color(0xFF4285F4) // Default blue
                        }
                    )
                }
                
                // Notification badge overlay
                if (hasUnreadNotifications) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .background(
                                color = Color.Red,
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = Color.White,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if ((notificationSummary?.unreadCount ?: 0) > 99) {
                                "99+"
                            } else {
                                (notificationSummary?.unreadCount ?: 0).toString()
                            },
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Project Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasUnreadNotifications) Color(0xFF2E7D32) else Color.Black
                    )
                    
                    // Notification indicator icon
                    if (hasUnreadNotifications) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Unread notifications",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Budget: ${FormatUtils.formatCurrency(project.budget)}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                // Show project end date if available
                project.endDate?.let { endDate ->
                    Spacer(modifier = Modifier.height(2.dp))
                    val daysLeft = FormatUtils.calculateDaysLeft(endDate)
                    val formattedDate = FormatUtils.formatDate(endDate)
                    val daysText = when {
                        daysLeft > 0 -> "(${daysLeft} days left)"
                        daysLeft == 0L -> "(Today)"
                        else -> "(${kotlin.math.abs(daysLeft)} days overdue)"
                    }
                    Text(
                        text = "📅 Ends: $formattedDate $daysText",
                        fontSize = 12.sp,
                        color = if (daysLeft >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
                
                // Notification summary if available
                if (hasUnreadNotifications && notificationSummary != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (notificationSummary.pendingApprovals > 0) {
                            NotificationChip(
                                text = "${notificationSummary.pendingApprovals} Pending",
                                color = Color(0xFFFF9800)
                            )
                        }
                        
                        if (notificationSummary.expenseUpdates > 0) {
                            NotificationChip(
                                text = "${notificationSummary.expenseUpdates} Updates",
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                    
                    // Latest notification preview
                    notificationSummary.latestNotification?.let { notification ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notification.message,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
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

private fun getProjectInitials(projectName: String): String {
    return projectName
        .split(" ")
        .take(2)
        .map { it.firstOrNull()?.uppercaseChar() ?: "" }
        .joinToString("")
        .ifEmpty { projectName.take(2).uppercase() }
}

@Composable
fun ProjectCardWithNotificationBadge(
    project: Project,
    unreadCount: Int = 0,
    onProjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasUnreadNotifications = unreadCount > 0
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProjectClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (hasUnreadNotifications) Color(0xFFF3E5F5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (hasUnreadNotifications) 4.dp else 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Project Initial Circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (hasUnreadNotifications) {
                            Color(0xFFE8F5E8)
                        } else {
                            Color(0xFFE3F2FD)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getProjectInitials(project.name),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasUnreadNotifications) {
                        Color(0xFF4CAF50)
                    } else {
                        Color(0xFF4285F4)
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Project Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasUnreadNotifications) Color(0xFF2E7D32) else Color.Black
                    )
                    
                    // Simple notification badge
                    if (hasUnreadNotifications) {
                        NotificationBadgeComponent(
                            badge = com.cubiquitous.tracura.model.NotificationBadge(
                                count = unreadCount,
                                hasUnread = true
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Budget: ${FormatUtils.formatCurrency(project.budget)}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                // Show project end date if available
                project.endDate?.let { endDate ->
                    Spacer(modifier = Modifier.height(2.dp))
                    val daysLeft = FormatUtils.calculateDaysLeft(endDate)
                    val formattedDate = FormatUtils.formatDate(endDate)
                    val daysText = when {
                        daysLeft > 0 -> "(${daysLeft} days left)"
                        daysLeft == 0L -> "(Today)"
                        else -> "(${kotlin.math.abs(daysLeft)} days overdue)"
                    }
                    Text(
                        text = "📅 Ends: $formattedDate $daysText",
                        fontSize = 12.sp,
                        color = if (daysLeft >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }
        }
    }
} 