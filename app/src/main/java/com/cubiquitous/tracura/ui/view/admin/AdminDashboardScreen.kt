package com.cubiquitous.tracura.ui.view.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.ui.view.businesshead.BusinessHeadProjectsTab
import com.cubiquitous.tracura.ui.view.businesshead.BusinessHeadUserManagementTab
import com.cubiquitous.tracura.viewmodel.AuthViewModel

private enum class AdminTab(val label: String) {
    Projects("Projects"),
    Users("User Management")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToManageUsers: () -> Unit,
    onNavigateToManageProjects: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToProject: (String, Project?, String?, String?) -> Unit = { _, _, _, _ -> },
    onNavigateToNewProject: () -> Unit = {},
    onNavigateToEditProject: (String) -> Unit = {},
    onNavigateToNewProjectForResubmission: (String) -> Unit = {},
    onNavigateToOverallReports: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToChat: (projectId: String, chatId: String, otherUserName: String) -> Unit = { _, _, _ -> },
    onNavigateToExpenseReview: (expenseId: String, notifTypeStr: String) -> Unit = { _, _ -> },
    onNavigateToExpenseChat: (expenseId: String) -> Unit = { _ -> },
    onNavigateToProfile: (UserRole) -> Unit = {},
    onNavigateToProjectDetail: ((String, Project?, String?, String?) -> Unit)? = null,
    onNavigateToCreateUser: () -> Unit = onNavigateToManageUsers,
    onNavigateToViewAllUsers: () -> Unit = onNavigateToManageUsers,
    onNavigateToDepartmentUserManagement: () -> Unit = {},
    onNavigateToRoleManagement: () -> Unit = {},
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(AdminTab.Projects) }
    
    // Refresh user data when screen opens to ensure current user is loaded
    LaunchedEffect(Unit) {
        authViewModel.refreshUserData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Admin",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            AdminBottomTabBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                AdminTab.Projects -> BusinessHeadProjectsTab(
                    onNavigateToProject = onNavigateToProject,
                    onNavigateToNewProject = onNavigateToNewProject,
                    onNavigateToEditProject = onNavigateToEditProject,
                    onNavigateToNewProjectForResubmission = onNavigateToNewProjectForResubmission,
                    onNavigateToOverallReports = onNavigateToOverallReports,
                    onNavigateToNotifications = onNavigateToNotifications,
                    onNavigateToChat = onNavigateToChat,
                    onNavigateToExpenseReview = onNavigateToExpenseReview,
                    onNavigateToExpenseChat = onNavigateToExpenseChat,
                    onLogout = onLogout,
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToProjectDetail = onNavigateToProjectDetail,
                    authViewModel = authViewModel
                )

                AdminTab.Users -> BusinessHeadUserManagementTab(
                    onNavigateToCreateUser = onNavigateToCreateUser,
                    onNavigateToViewAllUsers = onNavigateToViewAllUsers,
                    onNavigateToDepartmentUserManagement = onNavigateToDepartmentUserManagement,
                    onNavigateToRoleManagement = onNavigateToRoleManagement,
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
private fun AdminBottomTabBar(
    selectedTab: AdminTab,
    onTabSelected: (AdminTab) -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminTabButton(
                modifier = Modifier.weight(1f),
                label = AdminTab.Projects.label,
                icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                selected = selectedTab == AdminTab.Projects,
                onClick = { onTabSelected(AdminTab.Projects) }
            )
            AdminTabButton(
                modifier = Modifier.weight(1f),
                label = AdminTab.Users.label,
                icon = { Icon(Icons.Default.Group, contentDescription = null) },
                selected = selectedTab == AdminTab.Users,
                onClick = { onTabSelected(AdminTab.Users) }
            )
        }
    }
}

@Composable
private fun AdminTabButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    TextButton(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .background(
                color = if (selected) colors.primary.copy(alpha = 0.12f) else colors.surface,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (selected) colors.primary else colors.onSurfaceVariant
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}
