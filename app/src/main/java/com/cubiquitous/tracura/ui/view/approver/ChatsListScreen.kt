package com.cubiquitous.tracura.ui.view.approver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.cubiquitous.tracura.model.ChatParticipant
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.viewmodel.ChatsViewModel
import com.cubiquitous.tracura.viewmodel.ApproverProjectViewModel
import com.cubiquitous.tracura.utils.FormatUtils
import java.util.Date
import com.google.firebase.Timestamp

private data class IosChatsPalette(
    val tier1Background: Color,
    val tier2Surface: Color,
    val tier3Field: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val hairline: Color
)

@Composable
private fun rememberIosChatsPalette(accentColor: Color = MaterialTheme.colorScheme.primary): IosChatsPalette {
    val isDark = isSystemInDarkTheme()
    return remember(isDark, accentColor) {
        IosChatsPalette(
            tier1Background = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7),
            tier2Surface = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
            tier3Field = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF),
            textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
            textSecondary = if (isDark) Color(0x99EBEBF5) else Color(0x993C3C43),
            accent = accentColor,
            onAccent = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF),
            hairline = if (isDark) Color(0xFF38383A) else Color(0x1F000000)
        )
    }
}

/**
 * Get chat identifier for user (phone number for regular users, "BusinessHead" for BusinessHead)
 */
private fun User.getChatIdentifier(): String {
    return if (this.role.name != "BUSINESS_HEAD") this.phone else "BusinessHead"
}

/**
 * Chats List Screen - Shows list of chat participants
 * Based on Android Chat Implementation Guide
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    projectId: String,
    projectName: String,
    onNavigateBack: () -> Unit,
    onNavigateToIndividualChat: (String, String, String) -> Unit, // projectId, chatId, participantName
    onNavigateToGroupChat: (String) -> Unit, // projectId
    chatsViewModel: ChatsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    approverProjectViewModel: ApproverProjectViewModel = hiltViewModel()
) {
    val iosPalette = rememberIosChatsPalette()
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.user
    val participants by chatsViewModel.participants.collectAsState()
    val isLoading by chatsViewModel.isLoading.collectAsState()
    val isRefreshing by chatsViewModel.isRefreshing.collectAsState()
    val errorMessage by chatsViewModel.errorMessage.collectAsState()
    val presenceStatuses by chatsViewModel.presenceStatuses.collectAsState()

    // Start tracking own online presence once the current user is known
    LaunchedEffect(currentUser) {
        currentUser?.let { chatsViewModel.startOwnPresenceTracking(it.getChatIdentifier()) }
    }
    
    val selectedProject by approverProjectViewModel.selectedProject.collectAsState()
    
    // Load project if not already loaded or if it's a different project
    LaunchedEffect(projectId) {
        val currentProject = approverProjectViewModel.selectedProject.value
        if (currentProject == null || currentProject.id != projectId) {
            // Load project data
            approverProjectViewModel.loadProjectBudgetSummary(projectId)
        }
    }
    
    // Wait for project to be loaded, then load participants
    LaunchedEffect(projectId, currentUser, selectedProject) {
        val project = selectedProject
        if (project != null && project.id == projectId && currentUser != null) {
            chatsViewModel.loadChatParticipants(
                project = project,
                currentUserPhone = currentUser.getChatIdentifier(),
                currentUserRole = currentUser.role
            )
        }
    }

    // Re-fetch unread counts every time user navigates back to this screen (like iOS onAppear)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Read fresh values directly from ViewModels to avoid stale closure capture
                val project = approverProjectViewModel.selectedProject.value
                val user = authViewModel.authState.value.user
                if (project != null && project.id == projectId && user != null) {
                    chatsViewModel.loadChatParticipants(
                        project = project,
                        currentUserPhone = user.getChatIdentifier(),
                        currentUserRole = user.role
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Chats",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosPalette.textPrimary
                        )
                        Text(
                            text = projectName,
                            fontSize = 14.sp,
                            color = iosPalette.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val project = selectedProject
                            if (project != null && currentUser != null) {
                                chatsViewModel.refreshData(
                                    project = project,
                                    currentUserPhone = currentUser.phone,
                                    currentUserRole = currentUser.role
                                )
                            }
                        }
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = iosPalette.accent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = iosPalette.accent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = iosPalette.tier2Surface,
                    titleContentColor = iosPalette.textPrimary,
                    navigationIconContentColor = iosPalette.accent,
                    actionIconContentColor = iosPalette.accent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(iosPalette.tier1Background)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Error loading participants",
                                color = Color.Red,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "Unknown error",
                                color = iosPalette.textSecondary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                if (selectedProject != null && currentUser != null) {
                                    chatsViewModel.refreshData(
                                        project = selectedProject!!,
                                        currentUserPhone = currentUser.phone,
                                        currentUserRole = currentUser.role
                                    )
                                }
                            }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                participants.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = iosPalette.textSecondary.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No participants",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = iosPalette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "There are no other participants in this project",
                                fontSize = 14.sp,
                                color = iosPalette.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        // // Group Chat option
                        // item {
                        //     GroupChatItem(
                        //         projectName = projectName,
                        //         onClick = {
                        //             onNavigateToGroupChat(projectId)
                        //         },
                        //         iosPalette = iosPalette
                        //     )
                        //     Divider(
                        //         modifier = Modifier.padding(horizontal = 16.dp),
                        //         color = iosPalette.hairline
                        //     )
                        // }
                        
                        // Individual participants
                        items(participants) { participant ->
                            ParticipantItem(
                                participant = participant,
                                isOnline = presenceStatuses[participant.phoneNumber] ?: false,
                                onClick = {
                                    val currentUserIdentifier = currentUser?.getChatIdentifier() ?: "BusinessHead"
                                    // Use "BusinessHead" for document ID if participant is BusinessHead, otherwise use phoneNumber
                                    val participantIdentifier = if (participant.role == UserRole.BUSINESS_HEAD) {
                                        "BusinessHead"
                                    } else {
                                        participant.phoneNumber
                                    }
                                    // Immediately zero badge (optimistic update)
                                    if (participant.unreadCount > 0) {
                                        chatsViewModel.updateUnreadCountForParticipant(
                                            participantId = participantIdentifier,
                                            projectId = projectId,
                                            currentUserPhone = currentUserIdentifier
                                        )
                                    }
                                    onNavigateToIndividualChat(
                                        projectId,
                                        generateChatId(
                                            currentUserIdentifier,
                                            participantIdentifier
                                        ),
                                        participant.displayName
                                    )
                                },
                                iosPalette = iosPalette
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Generate chat ID for individual chat (sorted participant IDs)
 */
private fun generateChatId(participant1: String, participant2: String): String {
    val normalized1 = normalizeParticipantId(participant1)
    val normalized2 = normalizeParticipantId(participant2)
    return listOf(normalized1, normalized2).sorted().joinToString("_")
}

/**
 * Normalize participant ID (remove +91 prefix, handle BusinessHead)
 * Returns "BusinessHead" for document ID format: number_BusinessHead
 */
private fun normalizeParticipantId(id: String): String {
    return if (id == "BusinessHead" || id == "BUSINESS_HEAD" || id == "123") {
        "BusinessHead"
    } else {
        id.removePrefix("+91").trim()
    }
}

/**
 * Group Chat Item
 */
@Composable
private fun GroupChatItem(
    projectName: String,
    onClick: () -> Unit,
    iosPalette: IosChatsPalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Group icon
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF4285F4)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = "Group Chat",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Group chat info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Group Chat",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = iosPalette.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Chat with all project members",
                fontSize = 14.sp,
                color = iosPalette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = 180f },
            tint = iosPalette.textSecondary
        )
    }
}

/**
 * Participant Item
 */
@Composable
private fun ParticipantItem(
    participant: ChatParticipant,
    isOnline: Boolean,
    onClick: () -> Unit,
    iosPalette: IosChatsPalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with online indicator overlay
        Box(modifier = Modifier.size(56.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(getRoleColor(participant.role)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = participant.name.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF34C759))
                        .border(2.dp, iosPalette.tier2Surface, CircleShape)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Participant info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = participant.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = iosPalette.textPrimary
                )
                if (participant.role == UserRole.BUSINESS_HEAD) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "👑",
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (participant.lastMessage != null) {
                    Text(
                        text = participant.lastMessage,
                        fontSize = 14.sp,
                        color = iosPalette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (participant.lastMessageTime != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = FormatUtils.formatTimeAgo(
                                com.google.firebase.Timestamp(participant.lastMessageTime)
                            ),
                            fontSize = 12.sp,
                            color = iosPalette.textSecondary
                        )
                    }
                } else {
                    Text(
                        text = getRoleDisplayName(participant.role),
                        fontSize = 14.sp,
                        color = iosPalette.textSecondary
                    )
                }
            }
        }
        
        // Unread badge
        if (participant.unreadCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Red),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (participant.unreadCount > 99) "99+" else participant.unreadCount.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = 180f },
            tint = iosPalette.textSecondary
        )
    }
}

/**
 * Get role color
 */
private fun getRoleColor(role: UserRole): Color {
    return when (role) {
        UserRole.BUSINESS_HEAD -> Color(0xFFFF3B30) // Red
        UserRole.MANAGER -> Color(0xFFFF3B30) // Red
        UserRole.APPROVER -> Color(0xFFFF9500) // Orange
        UserRole.USER -> Color(0xFF007AFF) // Blue
        UserRole.ADMIN -> Color(0xFF007AFF) // Blue
    }
}

/**
 * Get role display name
 */
private fun getRoleDisplayName(role: UserRole): String {
    return when (role) {
        UserRole.BUSINESS_HEAD -> "Business Head"
        UserRole.MANAGER -> "Manager"
        UserRole.APPROVER -> "Approver"
        UserRole.USER -> "User"
        UserRole.ADMIN -> "Admin"
    }
}
