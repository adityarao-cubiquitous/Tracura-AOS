package com.cubiquitous.tracura.ui.common

import android.util.Log
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cubiquitous.tracura.R
import com.cubiquitous.tracura.model.Notification
import com.cubiquitous.tracura.model.NotificationType
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.utils.FormatUtils
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Comprehensive notification popup dialog for project selection view
 * Shows Phase Requests, Declined Projects, and Notifications sections
 * Matches the screenshot design with scrolling and proper layout
 */
@Composable
fun NotificationPopupDialog(
    notifications: List<Notification>,
    declinedProjects: List<Project> = emptyList(),
    inReviewProjects: List<Project> = emptyList(),
    phaseRequests: List<Map<String, Any>> = emptyList(),
    rejectedByNames: Map<String, String> = emptyMap(),
    userRole: UserRole,
    isLoadingPhaseRequests: Boolean = false,
    onDismiss: () -> Unit,
    onNotificationClick: (Notification) -> Unit = {},
    onDeclinedProjectClick: (String) -> Unit = {},
    onInReviewProjectClick: (String) -> Unit = {},
    onPhaseRequestClick: (String) -> Unit = {},
    onViewAllClick: () -> Unit = {},
    onMarkAsRead: (String) -> Unit = {},
) {
    val palette = notificationPopupPalette()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = palette.tier2Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header - "Notifications" title centered
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
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
                
                // Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Role-based sections
                    when (userRole) {
                        UserRole.BUSINESS_HEAD -> {
                            // Section 1: PHASE REQUESTS (for Business Head)
                            if (phaseRequests.isNotEmpty() || isLoadingPhaseRequests) {
                                item {
                                    SectionHeader(
                                        title = "PHASE REQUESTS",
                                        badgeCount = phaseRequests.size,
                                        badgeColor = palette.warning
                                    )
                                }
                                
                                if (isLoadingPhaseRequests) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = palette.warning
                                            )
                                        }
                                    }
                                } else {
                                    items(phaseRequests) { request ->
                                        PhaseRequestPopupItem(
                                            request = request,
                                            onClick = {
                                                val projectId = (request["projectId"] as? String) ?: ""
                                                if (projectId.isNotEmpty()) {
                                                    onPhaseRequestClick(projectId)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Section 2: DECLINED PROJECTS (for Business Head)
                            if (declinedProjects.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "DECLINED PROJECTS",
                                        badgeCount = declinedProjects.size,
                                        badgeColor = palette.danger
                                    )
                                }
                                
                                items(declinedProjects) { project ->
                                    DeclinedProjectPopupItem(
                                        project = project,
                                        rejectedByName = rejectedByNames[project.rejectedBy] ?: "Unknown",
                                        onClick = {
                                            project.id?.let { onDeclinedProjectClick(it) }
                                        }
                                    )
                                }
                            }
                            
                            // Section 3: NOTIFICATIONS (for Business Head)
                            val unreadNotifications = notifications.filter { !it.isRead }
                            if (unreadNotifications.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "NOTIFICATIONS",
                                        badgeCount = unreadNotifications.size,
                                        badgeColor = palette.accent
                                    )
                                }
                                
                                items(unreadNotifications.sortedByDescending { it.createdAt.seconds }) { notification ->
                                    NotificationPopupItem(
                                        notification = notification,
                                        onClick = {
                                            onNotificationClick(notification)
                                            if (!notification.isRead) {
                                                onMarkAsRead(notification.id)
                                            }
                                        }
                                    )
                                }
                            }
                            
                            // Empty state for Business Head
                            val hasNoBusinessHeadNotifications = phaseRequests.isEmpty() && 
                                declinedProjects.isEmpty() && 
                                unreadNotifications.isEmpty() && 
                                !isLoadingPhaseRequests
                            
                            if (hasNoBusinessHeadNotifications) {
                                item {
                                    EmptyStateView()
                                }
                            }
                        }
                        
                        UserRole.APPROVER -> {
                            // Section 1: IN_REVIEW PROJECTS (for Approver)
                            if (inReviewProjects.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "IN REVIEW",
                                        badgeCount = inReviewProjects.size,
                                        badgeColor = palette.warning
                                    )
                                }
                                
                                items(inReviewProjects) { project ->
                                    InReviewProjectPopupItem(
                                        project = project,
                                        onClick = {
                                            project.id?.let { onInReviewProjectClick(it) }
                                        }
                                    )
                                }
                            }
                            
                            // Section 2: NOTIFICATIONS (for Approver)
                            val unreadNotifications = notifications.filter { !it.isRead }
                            if (unreadNotifications.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "NOTIFICATIONS",
                                        badgeCount = unreadNotifications.size,
                                        badgeColor = palette.accent
                                    )
                                }
                                
                                items(unreadNotifications.sortedByDescending { it.createdAt.seconds }) { notification ->
                                    NotificationPopupItem(
                                        notification = notification,
                                        onClick = {
                                            onNotificationClick(notification)
                                            if (!notification.isRead) {
                                                onMarkAsRead(notification.id)
                                            }
                                        }
                                    )
                                }
                            }
                            
                            // Empty state for Approver
                            val hasNoApproverNotifications = inReviewProjects.isEmpty() && 
                                unreadNotifications.isEmpty()
                            
                            if (hasNoApproverNotifications) {
                                item {
                                    EmptyStateView()
                                }
                            }
                        }
                        
                        UserRole.USER -> {
                            // Section 1: NOTIFICATIONS ONLY (for User)
                            val unreadNotifications = notifications.filter { !it.isRead }
                            if (unreadNotifications.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "NOTIFICATIONS",
                                        badgeCount = unreadNotifications.size,
                                        badgeColor = palette.accent
                                    )
                                }
                                
                                items(unreadNotifications.sortedByDescending { it.createdAt.seconds }) { notification ->
                                    NotificationPopupItem(
                                        notification = notification,
                                        onClick = {
                                            onNotificationClick(notification)
                                            if (!notification.isRead) {
                                                onMarkAsRead(notification.id)
                                            }
                                        }
                                    )
                                }
                            } else {
                                // Empty state for User
                                item {
                                    EmptyStateView()
                                }
                            }
                        }
                        
                        else -> {
                            // Default: Show notifications only
                            val unreadNotifications = notifications.filter { !it.isRead }
                            if (unreadNotifications.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "NOTIFICATIONS",
                                        badgeCount = unreadNotifications.size,
                                        badgeColor = palette.accent
                                    )
                                }
                                
                                items(unreadNotifications.sortedByDescending { it.createdAt.seconds }) { notification ->
                                    NotificationPopupItem(
                                        notification = notification,
                                        onClick = {
                                            onNotificationClick(notification)
                                            if (!notification.isRead) {
                                                onMarkAsRead(notification.id)
                                            }
                                        }
                                    )
                                }
                            } else {
                                item {
                                    EmptyStateView()
                                }
                            }
                        }
                    }
                    
                    // Empty state (legacy - kept for backward compatibility, but should not be reached)
                    val unreadNotifications = notifications.filter { !it.isRead }
                    if (phaseRequests.isEmpty() && declinedProjects.isEmpty() && inReviewProjects.isEmpty() && unreadNotifications.isEmpty() && !isLoadingPhaseRequests) {
                        item {
                            Box(
                                modifier = Modifier
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
                                }
                            }
                        }
                    }
                    
                    // Spacer before buttons
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // Bottom buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // View All button (if there are notifications)
                    if (notifications.isNotEmpty()) {
                        TextButton(
                            onClick = onViewAllClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "View All (${notifications.size})",
                                color = palette.accent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Close button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent
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
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    badgeCount: Int,
    badgeColor: Color
) {
    val palette = notificationPopupPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
            if (badgeCount > 0) {
                Badge(
                    containerColor = badgeColor,
                    contentColor = Color.White
                ) {
                    Text(
                        text = badgeCount.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PhaseRequestPopupItem(
    request: Map<String, Any>,
    onClick: () -> Unit
) {
    val palette = notificationPopupPalette()
    val phaseName = (request["phaseName"] as? String) ?: "Phase"
    val userID = (request["userID"] as? String) ?: ""
    val extendedDate = (request["extendedDate"] as? String) ?: ""
    val createdAt = request["createdAt"] as? com.google.firebase.Timestamp
    
    val dateText = if (extendedDate.isNotEmpty()) {
        try {
            val dateParts = extendedDate.split("/")
            if (dateParts.size == 3) {
                val day = dateParts[0].toInt()
                val month = dateParts[1].toInt()
                val year = dateParts[2].toInt()
                val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = java.util.Calendar.getInstance().apply {
                    set(year, month - 1, day)
                }.time
                dateFormatter.format(date)
            } else {
                extendedDate
            }
        } catch (e: Exception) {
            extendedDate
        }
    } else {
        createdAt?.let { FormatUtils.formatTimeAgo(it) } ?: "Recently"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = palette.tier3Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Orange clock icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = palette.warning.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = palette.warning,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = phaseName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = userID,
                    fontSize = 14.sp,
                    color = palette.secondaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (extendedDate.isNotEmpty()) {
                    Text(
                        text = "Extend to: $extendedDate",
                        fontSize = 14.sp,
                        color = palette.accent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Date on right
            Text(
                text = dateText,
                fontSize = 12.sp,
                color = palette.secondaryText
            )
        }
    }
}

@Composable
private fun DeclinedProjectPopupItem(
    project: Project,
    rejectedByName: String,
    onClick: () -> Unit
) {
    val palette = notificationPopupPalette()
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val rejectedDate = dateFormatter.format(project.updatedAt.toDate())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = palette.tier3Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Project Name
            Text(
                text = project.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
            
            // Rejection Reason (in red)
            if (!project.rejectionReason.isNullOrBlank()) {
                Text(
                    text = project.rejectionReason ?: "",
                    fontSize = 14.sp,
                    color = palette.danger,
                    lineHeight = 20.sp
                )
            }
            
            // Date Rejected and Rejected By
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                        tint = palette.secondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = rejectedDate,
                        fontSize = 12.sp,
                        color = palette.secondaryText
                    )
                }
                
                // Rejected By
                if (!project.rejectedBy.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = palette.secondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Rejected by: $rejectedByName",
                            fontSize = 12.sp,
                            color = palette.secondaryText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationPopupItem(
    notification: Notification,
    onClick: () -> Unit
) {
    val palette = notificationPopupPalette()
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .background(if (notification.isRead) palette.tier3Surface else palette.unreadSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Dynamic icon based on notification type and content
            val (icon, iconColor) = getNotificationIconInfoPopup(notification)
            val title = notification.title.lowercase()
            val message = notification.message.lowercase()

            val isPhaseStarting = (title.contains("phase") && (title.contains("start") || message.contains("going to start"))) || title.contains("phase starting")
            val isPhaseEnded = (title.contains("phase") && (title.contains("ended") || title.contains("completed") || message.contains("ended today") || message.contains("ended"))) || (title.contains("phase ended") || title.contains("phase completed"))
            val isCompleted = title.contains("project status update") && (message.contains("is now completed") || message.contains("completed"))
            val isStandby = title.contains("project status update") && (message.contains("is now standby") || message.contains("standby"))
            val isSuspensionEndingSoon = notification.title.contains("Suspension Ending Soon", ignoreCase = true)
            val isSuspension = title.contains("project status update") && (message.contains("is now suspension") || message.contains("suspension"))
            val isActive = title.contains("project status update") && (message.contains("is now active") || message.contains("active") || message.contains("going to active"))
            val isMaintenance = (title.contains("project status update") && (message.contains("is now maintenance") || message.contains("maintenance"))) || (title.contains("upcoming project update") && message.contains("going to maintenance"))
            val isMaintenanceProjectStatusUpdate = title.contains("project status update") && (message.contains("is now maintenance") || message.contains("maintenance")) && !title.contains("upcoming project update")
            val isMaintenanceStartingSoon = notification.title.contains("Maintenance Starting Soon", ignoreCase = true)
            val isUpcomingProjectStart = title.contains("upcoming project update") && message.contains("going to start")
            val isHandoverTomorrow = notification.title.contains("Handover Tomorrow", ignoreCase = true) || (title.contains("upcoming project update") && message.contains("handover") && message.contains("scheduled for tomorrow"))
            val isHandoverToday = notification.title.contains("Ready for handover", ignoreCase = true) || (title.contains("upcoming project update") && message.contains("handover") && message.contains("scheduled for today"))
            val isGreenPlayIcon = icon == Icons.Default.PlayArrow && iconColor == palette.success && !isPhaseStarting && !isPhaseEnded && !isCompleted && !isStandby && !isSuspension && !isSuspensionEndingSoon && !isActive && !isMaintenance && !isMaintenanceStartingSoon && !isUpcomingProjectStart && !isHandoverTomorrow && !isHandoverToday
            val isActiveProjectStatus = isActive && icon == Icons.Default.PlayArrow
            val backgroundColor = if (isGreenPlayIcon) iconColor else iconColor.copy(alpha = 0.15f)
            val iconTint = if (isGreenPlayIcon) Color.White else iconColor

            when {
                isActiveProjectStatus -> {
                    Box(modifier = Modifier.size(32.dp).background(palette.success.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(16.dp).background(palette.success, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                    }
                }
                isGreenPlayIcon -> Box(modifier = Modifier.size(32.dp).background(backgroundColor, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp)) }
                isCompleted -> Box(modifier = Modifier.size(32.dp).background(palette.accent.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(painterResource(id = R.drawable.check), contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp)) }
                isStandby -> Box(modifier = Modifier.size(32.dp).background(palette.neutralIcon.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = palette.neutralIcon, modifier = Modifier.size(18.dp)) }
                isSuspensionEndingSoon -> Box(modifier = Modifier.size(32.dp).background(palette.warning.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = palette.warning, modifier = Modifier.size(18.dp)) }
                isSuspension -> Box(modifier = Modifier.size(32.dp).background(palette.warning.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = palette.warning, modifier = Modifier.size(18.dp)) }
                isPhaseEnded -> Box(modifier = Modifier.size(32.dp).background(palette.success.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) { Icon(painterResource(id = R.drawable.phasecompleted), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(18.dp)) }
                isPhaseStarting -> Box(modifier = Modifier.size(32.dp).background(palette.success.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) { Icon(painterResource(id = R.drawable.flag1), contentDescription = null, tint = palette.success, modifier = Modifier.size(18.dp)) }
                isMaintenanceStartingSoon -> Box(modifier = Modifier.size(32.dp).background(palette.project.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) { Icon(painterResource(id = R.drawable.houricon), contentDescription = null, tint = palette.project, modifier = Modifier.size(18.dp)) }
                isMaintenanceProjectStatusUpdate && !isMaintenanceStartingSoon -> Box(modifier = Modifier.size(32.dp).background(backgroundColor, CircleShape), contentAlignment = Alignment.Center) { Icon(painterResource(id = R.drawable.maintenance), contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp)) }
                isMaintenance && !isMaintenanceStartingSoon && !isMaintenanceProjectStatusUpdate -> Box(modifier = Modifier.size(32.dp).background(backgroundColor, CircleShape), contentAlignment = Alignment.Center) { Icon(painterResource(id = R.drawable.houricon), contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp)) }
                isUpcomingProjectStart -> Box(modifier = Modifier.size(32.dp).background(backgroundColor, CircleShape), contentAlignment = Alignment.Center) { Icon(painterResource(id = R.drawable.houricon), contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp)) }
                isHandoverTomorrow -> Box(
                    modifier = Modifier.size(32.dp).background(brush = Brush.linearGradient(colors = listOf(palette.success.copy(alpha = 0.15f), palette.accent.copy(alpha = 0.15f), palette.project.copy(alpha = 0.15f)), start = Offset(0f, 0f), end = Offset(32f, 32f)), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(painterResource(id = R.drawable.handover1), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp)) }
                isHandoverToday -> Box(
                    modifier = Modifier.size(32.dp).background(brush = Brush.linearGradient(colors = listOf(palette.success.copy(alpha = 0.15f), palette.accent.copy(alpha = 0.15f), palette.project.copy(alpha = 0.15f)), start = Offset(0f, 0f), end = Offset(32f, 32f)), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(painterResource(id = R.drawable.handover1), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(18.dp)) }
                else -> Box(modifier = Modifier.size(32.dp).background(backgroundColor, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp)) }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Notification content
            Column(modifier = Modifier.weight(1f)) {
                // Clean title and style status keywords
                var cleanTitle = notification.title
                    .replace("🔔", "").replace("⏳", "").replace("💬", "")
                    .replace("🔴", "").replace("🟡", "").replace("🟢", "").replace("🚀", "")
                    .replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), "")
                    .replace(Regex("[\\u2600-\\u27BF]"), "").trim()

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
                    if (status != null) cleanTitle = "Project Status Update : $status"
                }
                if (cleanTitle.contains("Phase Ended Today", ignoreCase = true)) {
                    cleanTitle = cleanTitle.replace(Regex("Phase Ended Today", RegexOption.IGNORE_CASE), "Phase Completed")
                }

                val styledTitle = when {
                    cleanTitle.contains("COMPLETED") -> buildAnnotatedString {
                        val parts = cleanTitle.split("COMPLETED")
                        append(parts[0])
                        withStyle(SpanStyle(color = palette.accent)) { append("COMPLETED") }
                        if (parts.size > 1) append(parts[1])
                    }
                    cleanTitle.contains("ACTIVE") -> buildAnnotatedString {
                        val parts = cleanTitle.split("ACTIVE")
                        append(parts[0])
                        withStyle(SpanStyle(color = palette.success)) { append("ACTIVE") }
                        if (parts.size > 1) append(parts[1])
                    }
                    cleanTitle.contains("MAINTENANCE") -> buildAnnotatedString {
                        val parts = cleanTitle.split("MAINTENANCE")
                        append(parts[0])
                        withStyle(SpanStyle(color = palette.project)) { append("MAINTENANCE") }
                        if (parts.size > 1) append(parts[1])
                    }
                    cleanTitle.contains("STANDBY") -> buildAnnotatedString {
                        val parts = cleanTitle.split("STANDBY")
                        append(parts[0])
                        withStyle(SpanStyle(color = palette.neutralIcon)) { append("STANDBY") }
                        if (parts.size > 1) append(parts[1])
                    }
                    else -> AnnotatedString(cleanTitle)
                }

                Text(
                    text = styledTitle,
                    fontSize = 14.sp,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    color = palette.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val displayMessage = notification.message
                    .replace(Regex("going to maintenance status", RegexOption.IGNORE_CASE), "going into maintenance phase")
                    .replace(Regex("is going to complete tomorrow", RegexOption.IGNORE_CASE), "is scheduled to complete tomorrow")
                    .replace(Regex("is going to complete in (\\d+) days?", RegexOption.IGNORE_CASE), "is coming to an end in $1 days")
                    .replace(Regex("Suspension status of the (.+?) ends tomorrow", RegexOption.IGNORE_CASE), "Suspension for $1 ends tomorrow")
                    .replace(Regex("(.+?) of (.+?) ended today", RegexOption.IGNORE_CASE), "$1 of $2 completed today")
                    .replace(Regex("(.+?) handover is scheduled for today", RegexOption.IGNORE_CASE), "$1 is ready for handover")

                Text(
                    text = displayMessage,
                    fontSize = 12.sp,
                    color = palette.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Time on right
            Log.d("NotificationTime1", "🔄 Loading notification for time: $notification.createdAt")
            Text(
                text = FormatUtils.formatTimeAgo(notification.createdAt),
                fontSize = 12.sp,
                color = palette.secondaryText
            )
        }
        HorizontalDivider(modifier = Modifier.padding(start = 60.dp), color = palette.divider)
    }
}

@Composable
private fun getNotificationIconInfoPopup(
    notification: Notification
): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    val palette = notificationPopupPalette()
    val title = notification.title.lowercase()
    val message = notification.message.lowercase()
    return when {
        (title.contains("project status update") && message.contains("is now completed")) ||
        (title.contains("project status update") && message.contains("completed")) ->
            Pair(Icons.Default.CheckCircle, palette.success)
        (title.contains("project status update") && message.contains("is now standby")) ||
        (title.contains("project status update") && message.contains("standby")) ->
            Pair(Icons.Default.PauseCircle, palette.neutralIcon)
        title.contains("suspension ending soon", ignoreCase = true) ->
            Pair(Icons.Default.Warning, palette.warning)
        (title.contains("project status update") && message.contains("is now suspension")) ||
        (title.contains("project status update") && message.contains("suspension")) ->
            Pair(Icons.Default.Warning, palette.warning)
        (title.contains("project") && (title.contains("complete") || message.contains("complete") || message.contains("going to complete"))) ||
        (message.contains("is now completed") && !title.contains("project status update")) ->
            Pair(Icons.Default.Warning, palette.danger)
        (title.contains("phase") && (title.contains("end") || message.contains("going to end"))) ||
        title.contains("phase ending") ->
            Pair(Icons.Default.Warning, palette.warning)
        (title.contains("project status update") && (message.contains("is now active") || message.contains("active"))) ||
        (title.contains("project status update") && (message.contains("going to active") || message.contains("going to start"))) ||
        (message.contains("is now active") && !title.contains("project status update")) ||
        (title.contains("upcoming project update") && message.contains("going to start")) ||
        (title.contains("project started") || message.contains("project started")) ->
            Pair(Icons.Default.PlayArrow, palette.success)
        (title.contains("phase") && (title.contains("start") || message.contains("going to start"))) ||
        title.contains("phase starting") ->
            Pair(Icons.Default.PlayArrow, palette.success)
        message.contains("maintenance") || message.contains("is now maintenance") ->
            Pair(Icons.Default.Build, palette.project)
        (title.contains("maintenance date changed") || message.contains("maintenance date changed")) ||
        (notification.type == NotificationType.PROJECT_CHANGED && !message.contains("start") && !message.contains("complete") && !message.contains("is now maintenance") && !message.contains("is now active")) ->
            Pair(Icons.Default.AccessTime, palette.warning)
        notification.type == NotificationType.DELEGATION_CHANGED ||
        notification.type == NotificationType.DELEGATION_EXPIRED ||
        notification.type == NotificationType.DELEGATION_REMOVED ||
        notification.type == NotificationType.DELEGATION_RESPONSE ||
        title.contains("delegation") ->
            Pair(Icons.Default.People, palette.neutralIcon)
        notification.type == NotificationType.CHAT_MESSAGE && (message.contains("expense") || message.contains("logged") || title.contains("expense")) ->
            Pair(Icons.Default.Chat, palette.accent)
        notification.type == NotificationType.CHAT_MESSAGE ->
            Pair(Icons.Default.Chat, palette.project)
        else ->
            Pair(Icons.Default.AccessTime, palette.warning)
    }
}

@Composable
private fun InReviewProjectPopupItem(
    project: Project,
    onClick: () -> Unit
) {
    val palette = notificationPopupPalette()
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val updatedDate = project.updatedAt?.toDate()?.let { dateFormatter.format(it) } ?: "N/A"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = palette.tier3Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Orange clock icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = palette.warning.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = palette.warning,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = project.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Status: IN REVIEW",
                    fontSize = 14.sp,
                    color = palette.warning,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Date on right
            Text(
                text = updatedDate,
                fontSize = 12.sp,
                color = palette.secondaryText
            )
        }
    }
}

@Composable
private fun EmptyStateView() {
    val palette = notificationPopupPalette()
    Box(
        modifier = Modifier
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
        }
    }
}
