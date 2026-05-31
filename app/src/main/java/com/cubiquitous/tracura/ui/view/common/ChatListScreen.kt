package com.cubiquitous.tracura.ui.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.ChatMember
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    projectId: String,
    projectName: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String, String, String, String) -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val chatState by chatViewModel.chatState.collectAsState()
    val currentUser = authState.user

    LaunchedEffect(projectId, currentUser?.phone) {
        currentUser?.phone?.let { userPhone ->
            chatViewModel.loadTeamMembers(projectId, userPhone)
            chatViewModel.loadUserChats(userPhone, projectId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Messages",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = projectName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                chatState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                chatState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Something went wrong",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = chatState.error ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            FilledTonalButton(onClick = {
                                currentUser?.phone?.let { userPhone ->
                                    chatViewModel.loadTeamMembers(projectId, userPhone)
                                }
                            }) {
                                Text("Try again")
                            }
                        }
                    }
                }

                chatState.teamMembers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No teammates yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Team members will appear here once they join the project.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        // Group Chat
                        item {
                            val groupChatId = "group_chat_$projectId"
                            val groupChat = chatState.chats.find { it.id == groupChatId }
                            val groupUnreadCount = currentUser?.let { user ->
                                val phone = user.phone ?: ""
                                val uid = user.uid ?: ""
                                (groupChat?.unreadCount?.get(phone) as? Number)?.toInt()
                                    ?: (groupChat?.unreadCount?.get(uid) as? Number)?.toInt()
                                    ?: 0
                            } ?: 0

                            GroupChatItem(
                                projectName = projectName,
                                memberCount = chatState.teamMembers.size,
                                unreadCount = groupUnreadCount,
                                onClick = {
                                    currentUser?.let { user ->
                                        chatViewModel.startGroupChat(
                                            projectId = projectId,
                                            currentUserId = user.phone,
                                            onChatCreated = { chatId ->
                                                onNavigateToChat(projectId, chatId, "group", "Group Chat")
                                            }
                                        )
                                    }
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        // Individual chats
                        items(chatState.teamMembers) { member ->
                            val memberChat = chatState.chats.find { chat ->
                                val participants = chat.participantsList
                                val isGroupChat = chat.id == "group_chat_$projectId" || participants.size > 2
                                if (isGroupChat) return@find false
                                val isPrivateChat = participants.size == 2
                                if (!isPrivateChat) return@find false
                                val currentUserPhone = currentUser?.phone ?: ""
                                val containsCurrentUser = participants.contains(currentUserPhone) ||
                                        (currentUser?.uid != null && participants.contains(currentUser.uid))
                                val containsMember = participants.contains(member.userId) ||
                                        participants.contains(member.phone)
                                containsCurrentUser && containsMember
                            }

                            val unreadCount = currentUser?.let { user ->
                                val phone = user.phone ?: ""
                                val uid = user.uid ?: ""
                                (memberChat?.unreadCount?.get(phone) as? Number)?.toInt()
                                    ?: (memberChat?.unreadCount?.get(uid) as? Number)?.toInt()
                                    ?: 0
                            } ?: 0

                            TeamMemberChatItem(
                                member = member,
                                unreadCount = unreadCount,
                                onClick = {
                                    currentUser?.let { user ->
                                        chatViewModel.startChat(
                                            projectId = projectId,
                                            currentUserId = user.phone,
                                            otherUserId = member.userId,
                                            onChatCreated = { chatId ->
                                                onNavigateToChat(projectId, chatId, member.userId, member.name)
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupChatItem(
    projectName: String,
    memberCount: Int,
    unreadCount: Int = 0,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = "Team group",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        supportingContent = {
            Text(
                text = "$memberCount members",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (unreadCount > 0) {
                    Badge {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString()
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun TeamMemberChatItem(
    member: ChatMember,
    unreadCount: Int = 0,
    onClick: () -> Unit
) {
    val roleColor = getRoleColorM3(member.role)

    ListItem(
        headlineContent = {
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        supportingContent = {
            Text(
                text = getRoleDisplayName(member.role),
                style = MaterialTheme.typography.bodyMedium,
                color = roleColor
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(roleColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.first().uppercaseChar().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = roleColor
                )
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (member.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                } else if (member.lastSeen > 0L) {
                    Text(
                        text = getLastSeenText(member.lastSeen),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (unreadCount > 0) {
                    Badge {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString()
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
    HorizontalDivider(
        modifier = Modifier.padding(start = 80.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun getRoleColorM3(role: UserRole): Color {
    return when (role) {
        UserRole.APPROVER -> MaterialTheme.colorScheme.tertiary
        UserRole.BUSINESS_HEAD -> MaterialTheme.colorScheme.secondary
        UserRole.MANAGER -> MaterialTheme.colorScheme.secondary
        UserRole.ADMIN -> MaterialTheme.colorScheme.error
        UserRole.USER -> MaterialTheme.colorScheme.primary
    }
}

private fun getRoleDisplayName(role: UserRole): String {
    return when (role) {
        UserRole.APPROVER -> "Approver"
        UserRole.BUSINESS_HEAD -> "Production Head"
        UserRole.MANAGER -> "Manager"
        UserRole.ADMIN -> "Admin"
        UserRole.USER -> "Team Member"
    }
}

private fun getLastSeenText(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
