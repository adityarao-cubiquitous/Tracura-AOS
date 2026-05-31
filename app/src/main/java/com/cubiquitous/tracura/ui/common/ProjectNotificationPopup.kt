package com.cubiquitous.tracura.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cubiquitous.tracura.model.Notification
import com.cubiquitous.tracura.model.NotificationType
import com.cubiquitous.tracura.utils.FormatUtils

/**
 * Notification popup dialog that displays project-specific notifications
 * Shows recent 4 notifications with "View All" and "Close" buttons
 */
@Composable
fun ProjectNotificationPopup(
    notifications: List<Notification>,
    onDismiss: () -> Unit,
    onNotificationClick: (Notification) -> Unit = {},
    onMarkAsRead: (String) -> Unit = {}
) {
    var showAllNotificationsSheet by remember { mutableStateOf(false) }
    val palette = notificationPopupPalette()
    
    // Show only first 4 notifications
    val recentNotifications = notifications.take(4)
    
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
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = palette.tier2Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header - centered title only
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Notifications",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                }
                
                HorizontalDivider(color = palette.divider)
                
                // Notifications List (show only recent 4 notifications)
                if (recentNotifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = palette.secondaryText
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Notifications",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.secondaryText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You'll see notifications here when there are updates about this project",
                                fontSize = 12.sp,
                                color = palette.secondaryText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentNotifications) { notification ->
                            ProjectNotificationPopupItem(
                                notification = notification,
                                onClick = {
                                    try {
                                        onNotificationClick(notification)
                                        if (!notification.isRead && notification.id.isNotBlank()) {
                                            onMarkAsRead(notification.id)
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ProjectNotificationPopup", "Error handling notification click: ${e.message}", e)
                                    }
                                }
                            )
                        }
                    }
                }
                
                // View All - centered above the divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "View All (${notifications.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.accent,
                        modifier = Modifier
                            .clickable {
                                showAllNotificationsSheet = true
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
                
                // Divider
                HorizontalDivider(color = palette.divider)
                
                // Close button - full width below divider with proper padding
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Close",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
    
    // Bottom sheet for all notifications
    if (showAllNotificationsSheet) {
        ProjectAllNotificationsBottomSheet(
            notifications = notifications,
            onDismiss = { showAllNotificationsSheet = false },
            onNotificationClick = { notification ->
                try {
                    showAllNotificationsSheet = false
                    onNotificationClick(notification)
                    if (!notification.isRead && notification.id.isNotBlank()) {
                        onMarkAsRead(notification.id)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProjectNotificationPopup", "Error handling notification click in bottom sheet: ${e.message}", e)
                }
            }
        )
    }
}

@Composable
private fun ProjectNotificationPopupItem(
    notification: Notification,
    onClick: () -> Unit
) {
    val palette = notificationPopupPalette()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) palette.tier3Surface else palette.unreadSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Notification icon based on type - matching screenshot design (blue speech bubble)
            val icon = try {
                when {
                    notification.type == NotificationType.CHAT_MESSAGE -> Icons.Default.Chat
                    notification.type.name.contains("CHAT", ignoreCase = true) -> Icons.Default.Chat
                    notification.type.name.contains("EXPENSE", ignoreCase = true) -> Icons.Default.AttachMoney
                    notification.type.name.contains("PROJECT", ignoreCase = true) -> Icons.Default.Business
                    notification.type == NotificationType.PENDING_APPROVAL -> Icons.Default.Notifications
                    else -> Icons.Default.Notifications
                }
            } catch (e: Exception) {
                Icons.Default.Notifications
            }
            
            val iconColor = try {
                when {
                    notification.type == NotificationType.CHAT_MESSAGE -> palette.accent
                    notification.type.name.contains("CHAT", ignoreCase = true) -> palette.accent
                    notification.type.name.contains("EXPENSE", ignoreCase = true) -> palette.success
                    notification.type.name.contains("PROJECT", ignoreCase = true) -> palette.project
                    else -> palette.neutralIcon
                }
            } catch (e: Exception) {
                palette.neutralIcon
            }
            
            // Icon - matching screenshot (blue speech bubble for chat)
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Notification content - matching screenshot layout
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title ?: "Notification",
                    fontSize = 14.sp,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    color = palette.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.message ?: "",
                    fontSize = 12.sp,
                    color = palette.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Time and more options - matching screenshot layout (ellipsis and time on right)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // More options icon (ellipsis) - matching screenshot
                IconButton(
                    onClick = { onClick() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = palette.secondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Time - matching screenshot ("in 0 sec")
                Text(
                    text = try {
                        notification.createdAt?.let { FormatUtils.formatTimeAgo(it) } ?: "Just now"
                    } catch (e: Exception) {
                        "Just now"
                    },
                    fontSize = 10.sp,
                    color = palette.secondaryText
                )
            }
        }
    }
}

/**
 * Bottom Sheet Modal for showing all notifications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectAllNotificationsBottomSheet(
    notifications: List<Notification>,
    onDismiss: () -> Unit,
    onNotificationClick: (Notification) -> Unit
) {
    val palette = notificationPopupPalette()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    
    // Expand sheet to full height on launch
    LaunchedEffect(Unit) {
        sheetState.expand()
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.tier1Background,
        dragHandle = null, // No drag handle for full-screen appearance
        modifier = Modifier.fillMaxHeight() // Full height
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.tier1Background)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.tier1Background)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notifications",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
                
                // Done button
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = "Done",
                        color = palette.accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Notifications List
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "No notifications",
                            tint = palette.secondaryText,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Notifications",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.secondaryText
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(notifications) { notification ->
                        ProjectAllNotificationItem(
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
 * Notification item for the bottom sheet
 */
@Composable
private fun ProjectAllNotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    val palette = notificationPopupPalette()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = palette.tier2Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Notification icon based on type
            val icon = try {
                when {
                    notification.type == NotificationType.CHAT_MESSAGE -> Icons.Default.Chat
                    notification.type.name.contains("CHAT", ignoreCase = true) -> Icons.Default.Chat
                    notification.type.name.contains("EXPENSE", ignoreCase = true) -> Icons.Default.AttachMoney
                    notification.type.name.contains("PROJECT", ignoreCase = true) -> Icons.Default.Business
                    notification.type == NotificationType.PENDING_APPROVAL -> Icons.Default.Notifications
                    else -> Icons.Default.Notifications
                }
            } catch (e: Exception) {
                Icons.Default.Notifications
            }
            
            val iconColor = try {
                when {
                    notification.type == NotificationType.CHAT_MESSAGE -> palette.accent
                    notification.type.name.contains("CHAT", ignoreCase = true) -> palette.accent
                    notification.type.name.contains("EXPENSE", ignoreCase = true) -> palette.success
                    notification.type.name.contains("PROJECT", ignoreCase = true) -> palette.project
                    else -> palette.neutralIcon
                }
            } catch (e: Exception) {
                palette.neutralIcon
            }
            
            // Icon
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Notification content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title ?: "Notification",
                    fontSize = 14.sp,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    color = palette.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.message ?: "",
                    fontSize = 12.sp,
                    color = palette.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Time and more options
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // More options icon (ellipsis)
                IconButton(
                    onClick = { onClick() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = palette.secondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Time
                Text(
                    text = try {
                        notification.createdAt?.let { FormatUtils.formatTimeAgo(it) } ?: "Just now"
                    } catch (e: Exception) {
                        "Just now"
                    },
                    fontSize = 10.sp,
                    color = palette.secondaryText
                )
            }
        }
    }
}
