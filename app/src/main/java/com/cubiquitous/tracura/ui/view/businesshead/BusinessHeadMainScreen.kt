package com.cubiquitous.tracura.ui.view.businesshead

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.R
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.model.Project
import androidx.compose.foundation.isSystemInDarkTheme
import com.cubiquitous.tracura.viewmodel.ProjectViewModel

private data class IosMainPalette(
    val tier2Surface: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color
)

@Composable
private fun rememberIosMainPalette(accentColor: Color = MaterialTheme.colorScheme.primary): IosMainPalette {
    val isDark = isSystemInDarkTheme()
    return remember(isDark, accentColor) {
        IosMainPalette(
            tier2Surface = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
            textSecondary = if (isDark) Color(0x99EBEBF5) else Color(0x993C3C43),
            accent = accentColor,
            onAccent = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessHeadMainScreen(
    onNavigateToProject: (String, Project?, String?, String?) -> Unit,
    onNavigateToCreateUser: () -> Unit,
    onNavigateToViewAllUsers: () -> Unit,
    onNavigateToDepartmentUserManagement: () -> Unit = {},
    onNavigateToRoleManagement: () -> Unit,
    onNavigateToNewProject: () -> Unit,
    onNavigateToEditProject: (String) -> Unit,
    onNavigateToNewProjectForResubmission: (String) -> Unit,
    onNavigateToOverallReports: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToChat: (projectId: String, chatId: String, otherUserName: String) -> Unit = { _, _, _ -> },
    onNavigateToExpenseReview: (expenseId: String, notifTypeStr: String) -> Unit = { _, _ -> },
    onNavigateToExpenseChat: (expenseId: String) -> Unit = { _ -> },
    onNavigateToSelectTemplate: () -> Unit = {},
    onNavigateToProfile: (UserRole) -> Unit = {},
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    projectViewModel: ProjectViewModel = hiltViewModel()
) {
    val iosPalette = rememberIosMainPalette()
    val projects by projectViewModel.projects.collectAsState()
    // Use rememberSaveable to persist tab state across navigation
    var selectedTab by rememberSaveable { mutableStateOf(0) } // Default to Projects tab (0)
    
    Scaffold(
        floatingActionButton = {
            // Show FAB only on Projects tab when there are projects
            if (selectedTab == 0 && projects.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { onNavigateToSelectTemplate() },
                    containerColor = iosPalette.accent,
                    contentColor = iosPalette.onAccent,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Project",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.End,
        bottomBar = {
            NavigationBar(
                containerColor = iosPalette.tier2Surface
            ) {
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.projects),
                            contentDescription = "Projects",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Projects",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = iosPalette.accent,
                        selectedTextColor = iosPalette.accent,
                        unselectedIconColor = iosPalette.textSecondary,
                        unselectedTextColor = iosPalette.textSecondary
                    )
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.usermanagement),
                            contentDescription = "User Management",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "User Management",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = iosPalette.accent,
                        selectedTextColor = iosPalette.accent,
                        unselectedIconColor = iosPalette.textSecondary,
                        unselectedTextColor = iosPalette.textSecondary
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> BusinessHeadProjectsTab(
                    onNavigateToProject = onNavigateToProject,
                    onNavigateToNewProject = onNavigateToNewProject,
                    onNavigateToEditProject = onNavigateToEditProject,
                    onNavigateToNewProjectForResubmission = onNavigateToNewProjectForResubmission,
                    onNavigateToOverallReports = onNavigateToOverallReports,
                    onNavigateToNotifications = onNavigateToNotifications,
                    onNavigateToChat = onNavigateToChat,
                    onNavigateToExpenseReview = onNavigateToExpenseReview,
                    onNavigateToExpenseChat = onNavigateToExpenseChat,
                    onNavigateToProfile = onNavigateToProfile,
                    onLogout = onLogout,
                    onNavigateToUserManagement = { selectedTab = 1 }
                )
                1 -> BusinessHeadUserManagementTab(
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

