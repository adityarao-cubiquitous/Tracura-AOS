package com.cubiquitous.tracura.ui.view.common

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.R
import com.cubiquitous.tracura.model.Notification
import com.cubiquitous.tracura.model.NotificationType
import com.cubiquitous.tracura.navigation.buildExpenseListNotificationTag
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.viewmodel.NotificationViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProject: (String, String, String) -> Unit,
    onNavigateToExpense: (String, String, String) -> Unit,
    onNavigateToPendingApprovals: (String, String) -> Unit,
    onNavigateToChat: (String, String, String) -> Unit = { _, _, _ -> }, // projectId, chatId, otherUserName
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel
) {
    val notifications by notificationViewModel.notifications.collectAsState()
    val isLoading by notificationViewModel.isLoading.collectAsState()
    val error by notificationViewModel.error.collectAsState()
    val notificationBadge by notificationViewModel.notificationBadge.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    
    // Get user role
    val userRole = authState.user?.role?.name ?: ""
    
    // Load notifications when screen opens and refresh when screen becomes visible
    LaunchedEffect(Unit) {
        val currentUserId = authState.user?.phone ?: authState.user?.uid ?: ""
        if (currentUserId.isNotEmpty()) {
            // For Production Head, load all notifications (read and unread)
            // For others, load only unread notifications
            if (userRole == "BUSINESS_HEAD") {
                notificationViewModel.loadAllNotifications(currentUserId)
            } else {
                notificationViewModel.loadNotifications(currentUserId)
            }
        }
    }
    
    // Refresh notifications when screen becomes visible (for better UX)
    LaunchedEffect(Unit) {
        val currentUserId = authState.user?.phone ?: authState.user?.uid ?: ""
        if (currentUserId.isNotEmpty()) {
            // Small delay to ensure screen is fully loaded
            delay(500)
            notificationViewModel.onScreenVisible(currentUserId)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top Bar - matching third screenshot with "Done" button
        TopAppBar(
            title = {
                Text(
                    text = "Notifications",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black // Black text matching screenshot
                )
            },
            navigationIcon = {
                // No back button - matching screenshot
            },
            actions = {
                // Done button on right - matching third screenshot
                TextButton(
                    onClick = onNavigateBack
                ) {
                    Text(
                        text = "Done",
                        color = Color(0xFF4285F4), // Blue color matching screenshot
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFFF5F5F5) // Light gray background matching screenshot
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
                                val currentUserId = authState.user?.uid
                                if (currentUserId != null) {
                                    notificationViewModel.refreshNotifications(currentUserId)
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
                            text = "You'll see notifications here when there are updates about your expenses and projects",
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
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNotificationClick() }
                .background(if (notification.isRead) Color.White else Color(0xFFF0F8FF))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Dynamic icon based on notification type and content
            val (icon, iconColor) = getNotificationIconInfoLocal(notification)
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
            val isGreenPlayIcon = icon == Icons.Default.PlayArrow && iconColor == Color(0xFF06F610) && !isPhaseStarting && !isPhaseEnded && !isCompleted && !isStandby && !isSuspension && !isSuspensionEndingSoon && !isActive && !isMaintenance && !isMaintenanceStartingSoon && !isUpcomingProjectStart && !isHandoverTomorrow && !isHandoverToday
            val isActiveProjectStatus = isActive && icon == Icons.Default.PlayArrow
            val backgroundColor = if (isGreenPlayIcon) iconColor else iconColor.copy(alpha = 0.15f)
            val iconTint = if (isGreenPlayIcon) Color.White else iconColor

            when {
                isActiveProjectStatus -> {
                    Box(modifier = Modifier.size(32.dp).background(Color(0xFF06F610).copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(16.dp).background(Color(0xFF06F610), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                    }
                }
                isGreenPlayIcon -> Box(modifier = Modifier.size(32.dp).background(backgroundColor, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp)) }
                isCompleted -> Box(modifier = Modifier.size(32.dp).background(Color(0xFFE3F2FD), CircleShape), contentAlignment = Alignment.Center) { Icon(painterResource(id = R.drawable.check), contentDescription = null, tint = Color(0xFF03A9F4), modifier = Modifier.size(18.dp)) }
                isStandby -> Box(modifier = Modifier.size(32.dp).background(Color(0xFF9E9E9E).copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(18.dp)) }
                isSuspensionEndingSoon -> Box(modifier = Modifier.size(32.dp).background(Color(0xFFFF9800).copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp)) }
                isSuspension -> Box(modifier = Modifier.size(32.dp).background(Color(0xFFFF9800).copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp)) }
                isPhaseEnded -> Box(modifier = Modifier.size(32.dp).background(Color(0xFF06F610).copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) { Icon(painterResource(id = R.drawable.phasecompleted), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(18.dp)) }
                isPhaseStarting -> Box(modifier = Modifier.size(32.dp).background(Color(0xFF06F610).copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) { Icon(painterResource(id = R.drawable.flag1), contentDescription = null, tint = Color(0xFF06F610), modifier = Modifier.size(18.dp)) }
                isMaintenanceStartingSoon -> Box(modifier = Modifier.size(32.dp).background(Color(0xFF9C27B0).copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) { Icon(painterResource(id = R.drawable.houricon), contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(18.dp)) }
                isMaintenanceProjectStatusUpdate && !isMaintenanceStartingSoon -> Box(modifier = Modifier.size(32.dp).background(backgroundColor, CircleShape), contentAlignment = Alignment.Center) { Icon(painterResource(id = R.drawable.maintenance), contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp)) }
                isMaintenance && !isMaintenanceStartingSoon && !isMaintenanceProjectStatusUpdate -> Box(modifier = Modifier.size(32.dp).background(backgroundColor, CircleShape), contentAlignment = Alignment.Center) { Icon(painterResource(id = R.drawable.houricon), contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp)) }
                isUpcomingProjectStart -> Box(modifier = Modifier.size(32.dp).background(backgroundColor, CircleShape), contentAlignment = Alignment.Center) { Icon(painterResource(id = R.drawable.houricon), contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp)) }
                isHandoverTomorrow -> Box(
                    modifier = Modifier.size(32.dp).background(brush = Brush.linearGradient(colors = listOf(Color.Green.copy(alpha = 0.15f), Color.Blue.copy(alpha = 0.15f), Color(0xFF9C27B0).copy(alpha = 0.15f)), start = Offset(0f, 0f), end = Offset(32f, 32f)), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(painterResource(id = R.drawable.handover1), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp)) }
                isHandoverToday -> Box(
                    modifier = Modifier.size(32.dp).background(brush = Brush.linearGradient(colors = listOf(Color.Green.copy(alpha = 0.15f), Color.Blue.copy(alpha = 0.15f), Color(0xFF9C27B0).copy(alpha = 0.15f)), start = Offset(0f, 0f), end = Offset(32f, 32f)), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(painterResource(id = R.drawable.handover1), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(18.dp)) }
                else -> Box(modifier = Modifier.size(32.dp).background(backgroundColor, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp)) }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Notification content
            Column(modifier = Modifier.weight(1f)) {
                // Clean title (strip emoji) and add styled status keywords
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
                        withStyle(SpanStyle(color = Color(0xFFE3F2FD))) { append("COMPLETED") }
                        if (parts.size > 1) append(parts[1])
                    }
                    cleanTitle.contains("ACTIVE") -> buildAnnotatedString {
                        val parts = cleanTitle.split("ACTIVE")
                        append(parts[0])
                        withStyle(SpanStyle(color = Color(0xFF06F610))) { append("ACTIVE") }
                        if (parts.size > 1) append(parts[1])
                    }
                    cleanTitle.contains("MAINTENANCE") -> buildAnnotatedString {
                        val parts = cleanTitle.split("MAINTENANCE")
                        append(parts[0])
                        withStyle(SpanStyle(color = Color(0xFF9C27B0))) { append("MAINTENANCE") }
                        if (parts.size > 1) append(parts[1])
                    }
                    cleanTitle.contains("STANDBY") -> buildAnnotatedString {
                        val parts = cleanTitle.split("STANDBY")
                        append(parts[0])
                        withStyle(SpanStyle(color = Color(0xFFE91E63))) { append("STANDBY") }
                        if (parts.size > 1) append(parts[1])
                    }
                    else -> AnnotatedString(cleanTitle)
                }

                Text(
                    text = styledTitle,
                    fontSize = 14.sp,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

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
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Timestamp on right
            Log.d("NotificationTime2", "🔄 Loading notification for time: $notification.createdAt")
            Text(
                text = FormatUtils.formatTimeAgo(notification.createdAt),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        Divider(modifier = Modifier.padding(start = 60.dp), color = Color.LightGray.copy(alpha = 0.3f))
    }
}

@Composable
private fun getNotificationIconInfoLocal(
    notification: Notification
): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    val title = notification.title.lowercase()
    val message = notification.message.lowercase()
    return when {
        (title.contains("project status update") && message.contains("is now completed")) ||
        (title.contains("project status update") && message.contains("completed")) ->
            Pair(Icons.Default.CheckCircle, Color(0xFF4CAF50))
        (title.contains("project status update") && message.contains("is now standby")) ||
        (title.contains("project status update") && message.contains("standby")) ->
            Pair(Icons.Default.PauseCircle, Color(0xFF9E9E9E))
        title.contains("suspension ending soon", ignoreCase = true) ->
            Pair(Icons.Default.Warning, Color(0xFFFF9800))
        (title.contains("project status update") && message.contains("is now suspension")) ||
        (title.contains("project status update") && message.contains("suspension")) ->
            Pair(Icons.Default.Warning, Color(0xFFFF9800))
        (title.contains("project") && (title.contains("complete") || message.contains("complete") || message.contains("going to complete"))) ||
        (message.contains("is now completed") && !title.contains("project status update")) ->
            Pair(Icons.Default.Warning, Color(0xFFF44336))
        (title.contains("phase") && (title.contains("end") || message.contains("going to end"))) ||
        title.contains("phase ending") ->
            Pair(Icons.Default.Warning, Color(0xFFFFEB3B))
        (title.contains("project status update") && (message.contains("is now active") || message.contains("active"))) ||
        (title.contains("project status update") && (message.contains("going to active") || message.contains("going to start"))) ||
        (message.contains("is now active") && !title.contains("project status update")) ||
        (title.contains("upcoming project update") && message.contains("going to start")) ||
        (title.contains("project started") || message.contains("project started")) ->
            Pair(Icons.Default.PlayArrow, Color(0xFF06F610))
        (title.contains("phase") && (title.contains("start") || message.contains("going to start"))) ||
        title.contains("phase starting") ->
            Pair(Icons.Default.PlayArrow, Color(0xFF4CAF50))
        message.contains("maintenance") || message.contains("is now maintenance") ->
            Pair(Icons.Default.Build, Color(0xFF9C27B0))
        (title.contains("maintenance date changed") || message.contains("maintenance date changed")) ||
        (notification.type == NotificationType.PROJECT_CHANGED && !message.contains("start") && !message.contains("complete") && !message.contains("is now maintenance") && !message.contains("is now active")) ->
            Pair(Icons.Default.AccessTime, Color(0xFFFF9800))
        notification.type == NotificationType.DELEGATION_CHANGED ||
        notification.type == NotificationType.DELEGATION_EXPIRED ||
        notification.type == NotificationType.DELEGATION_REMOVED ||
        notification.type == NotificationType.DELEGATION_RESPONSE ||
        title.contains("delegation") ->
            Pair(Icons.Default.People, Color(0xFF90A4AE))
        notification.type == NotificationType.CHAT_MESSAGE && (message.contains("expense") || message.contains("logged") || title.contains("expense")) ->
            Pair(Icons.Default.Chat, Color(0xFF2196F3))
        notification.type == NotificationType.CHAT_MESSAGE ->
            Pair(Icons.Default.Chat, Color(0xFF9C27B0))
        else ->
            Pair(Icons.Default.AccessTime, Color(0xFFFF9800))
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
    onMarkAsRead()

    val navTarget = notification.navigationTarget
    val projectId = notification.projectId
    val relatedId = notification.relatedId   // chatId OR expenseId depending on type
    val messageLower = notification.message.lowercase()
    val notificationMessage = notification.message
    val expenseListNotificationTag = buildExpenseListNotificationTag(
        notificationType = notification.type.name,
        navigationTarget = navTarget,
        notifType = notification.type,
        title = notification.title,
        message = notification.message
    )

    when {
        // ── DELEGATION_REMOVED: mark read only, no nav ──────────────────
        notification.type == NotificationType.DELEGATION_REMOVED -> return

        // ── EXPENSE CHAT → goes to expense chat screen ──────────────────
        navTarget.contains("expense_chat", ignoreCase = true) ||
        (notification.type == NotificationType.CHAT_MESSAGE &&
         (messageLower.contains("expense") || messageLower.contains("logged"))) -> {
            if (relatedId.isNotEmpty()) {
                onNavigateToExpense(projectId, relatedId, expenseListNotificationTag)   // caller routes expenseId → expense chat
            } else if (projectId.isNotEmpty()) {
                onNavigateToProject(projectId, expenseListNotificationTag, notificationMessage)
            }
        }

        // ── CHAT_MESSAGE → project chat screen ──────────────────────────
        notification.type == NotificationType.CHAT_MESSAGE ||
        navTarget.startsWith("chat/", ignoreCase = true) ||
        navTarget.contains("chat_detail", ignoreCase = true) ||
        navTarget.contains("chat_screen", ignoreCase = true) -> {
            // Try to parse "chat/{projectId}/{chatId}/{otherUserName}" from navigationTarget
            if (navTarget.startsWith("chat/", ignoreCase = true)) {
                val parts = navTarget.split("/")
                val chatProjectId = parts.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: projectId
                val chatId        = parts.getOrNull(2)?.takeIf { it.isNotEmpty() } ?: relatedId
                val chatUserName  = parts.getOrNull(3) ?: notification.title
                if (chatProjectId.isNotEmpty() && chatId.isNotEmpty()) {
                    onNavigateToChat(chatProjectId, chatId, chatUserName)
                    return
                }
            }
            // Fallback: use projectId + relatedId (relatedId IS the chatId for chat notifications)
            if (projectId.isNotEmpty() && relatedId.isNotEmpty()) {
                onNavigateToChat(projectId, relatedId, notification.title)
            } else if (projectId.isNotEmpty()) {
                onNavigateToProject(projectId, expenseListNotificationTag, notificationMessage)
            }
        }

        // ── EXPENSE_SUBMITTED / PENDING_APPROVAL → pending approvals ────
        notification.type == NotificationType.EXPENSE_SUBMITTED ||
        notification.type == NotificationType.PENDING_APPROVAL ||
        navTarget.contains("pending_approval", ignoreCase = true) -> {
            if (projectId.isNotEmpty()) onNavigateToPendingApprovals(projectId, expenseListNotificationTag)
        }

        // ── EXPENSE_APPROVED / EXPENSE_REJECTED → expense detail ────────
        notification.type == NotificationType.EXPENSE_APPROVED ||
        notification.type == NotificationType.EXPENSE_REJECTED ||
        navTarget.contains("expense_detail", ignoreCase = true) ||
        navTarget.contains("expense_review", ignoreCase = true) -> {
            if (projectId.isNotEmpty() && relatedId.isNotEmpty()) {
                onNavigateToExpense(projectId, relatedId, expenseListNotificationTag)
            } else if (projectId.isNotEmpty()) {
                onNavigateToProject(projectId, expenseListNotificationTag, notificationMessage)
            }
        }

        // ── Everything else → project dashboard ─────────────────────────
        else -> {
            when {
                navTarget.contains("pending_approvals/", ignoreCase = true) -> {
                    onNavigateToPendingApprovals(navTarget.substringAfterLast("/"), expenseListNotificationTag)
                }
                projectId.isNotEmpty() -> onNavigateToProject(projectId, expenseListNotificationTag, notificationMessage)
            }
        }
    }
}