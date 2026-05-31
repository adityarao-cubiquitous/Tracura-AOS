package com.cubiquitous.tracura.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.ui.view.admin.AdminDashboardScreen
import com.cubiquitous.tracura.ui.view.admin.ManageProjectsScreen
import com.cubiquitous.tracura.ui.view.admin.ManageUsersScreen
import com.cubiquitous.tracura.ui.view.admin.ReportsScreen
import com.cubiquitous.tracura.ui.view.approver.ApproverProjectDashboardScreen
import com.cubiquitous.tracura.ui.view.approver.ApproverProjectReviewScreen
import com.cubiquitous.tracura.ui.view.approver.ApproverNotificationScreen
import com.cubiquitous.tracura.ui.view.approver.CategoryDetailScreen
import com.cubiquitous.tracura.ui.view.approver.ReviewExpenseScreen
import com.cubiquitous.tracura.ui.view.approver.ApproverExpenseChatScreen
import com.cubiquitous.tracura.ui.view.approver.ReportsScreen as ApproverReportsScreen
import com.cubiquitous.tracura.ui.view.approver.AnalyticsScreen
import com.cubiquitous.tracura.ui.view.approver.OverallReportsScreen
import com.cubiquitous.tracura.ui.view.auth.LoginScreen
import com.cubiquitous.tracura.ui.view.auth.AdminSignUpScreen
import com.cubiquitous.tracura.ui.view.auth.OtpVerificationScreen
import com.cubiquitous.tracura.ui.view.auth.AccessRestrictedScreen
import com.cubiquitous.tracura.ui.view.auth.CustomerSelectionScreen
import com.cubiquitous.tracura.ui.view.businesshead.BusinessHeadProjectsTab
import com.cubiquitous.tracura.ui.view.businesshead.UserProfileScreen
import com.cubiquitous.tracura.ui.view.businesshead.PendingByCreditScreen
import com.cubiquitous.tracura.ui.view.common.NotificationListScreen
import com.cubiquitous.tracura.ui.view.common.ProjectNotificationScreen
import com.cubiquitous.tracura.ui.view.common.ProjectNotificationListScreen
import com.cubiquitous.tracura.ui.view.common.ChatScreen
import com.cubiquitous.tracura.ui.view.user.AddExpenseScreen
import com.cubiquitous.tracura.ui.view.user.ExpenseChatScreen
import com.cubiquitous.tracura.ui.view.user.ExpenseListScreen
import com.cubiquitous.tracura.ui.view.user.AllExpensesScreen
import com.cubiquitous.tracura.ui.view.user.TrackSubmissionsScreen
import com.cubiquitous.tracura.ui.view.user.UserDashboardScreen
import com.cubiquitous.tracura.ui.view.user.UserExpenseChatScreen
import com.cubiquitous.tracura.ui.view.businesshead.CreateUserScreen
import com.cubiquitous.tracura.ui.view.businesshead.NewProjectScreen
import com.cubiquitous.tracura.ui.view.businesshead.ReviewProjectScreen
import com.cubiquitous.tracura.ui.view.businesshead.DraftProjectsScreen
import com.cubiquitous.tracura.ui.view.businesshead.EditProjectScreen
import com.cubiquitous.tracura.ui.view.businesshead.BusinessHeadMainScreen
import com.cubiquitous.tracura.ui.view.businesshead.ViewAllUsersScreen
import com.cubiquitous.tracura.ui.view.businesshead.DepartmentUserManagementScreen
import com.cubiquitous.tracura.ui.view.businesshead.RoleManagementScreen
import com.cubiquitous.tracura.ui.view.businesshead.BusinessHeadPendingApprovals
import com.cubiquitous.tracura.ui.view.businesshead.BusinessHeadReports
import com.cubiquitous.tracura.ui.view.businesshead.BusinessHeadAnalytics
import com.cubiquitous.tracura.ui.view.businesshead.BusinessHeadOverallReports
import com.cubiquitous.tracura.ui.view.businesshead.BusinessHeadCategoryDetail
import com.cubiquitous.tracura.viewmodel.BusinessHeadViewModel
import com.cubiquitous.tracura.ui.view.businesshead.DelegationScreen
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.viewmodel.ProjectViewModel
import com.cubiquitous.tracura.viewmodel.ApproverProjectViewModel
import com.cubiquitous.tracura.ui.view.approver.DepartmentDetailScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.cubiquitous.tracura.model.Customer

/**
 * iOS-style horizontal slide navigation transitions
 * 
 * Forward navigation (push):
 * - New screen slides in from right → left (enterTransition: slideInHorizontally { it })
 * - Old screen slides out to left (exitTransition: slideOutHorizontally { -it })
 * 
 * Back navigation (pop):
 * - Previous screen slides in from left → right (popEnterTransition: slideInHorizontally { -it })
 * - Current screen slides out to right (popExitTransition: slideOutHorizontally { it })
 * 
 * Animation duration: 300ms (standard iOS timing)
 */
private fun defaultSlideTransitions(): androidx.compose.animation.EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { it }, // Slide in from right (positive offset)
        animationSpec = tween(300)
    )
}

private fun defaultSlideExitTransitions(): androidx.compose.animation.ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { -it }, // Slide out to left (negative offset)
        animationSpec = tween(300)
    )
}

private fun defaultPopEnterTransitions(): androidx.compose.animation.EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { -it }, // Slide in from left (negative offset)
        animationSpec = tween(300)
    )
}

private fun defaultPopExitTransitions(): androidx.compose.animation.ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { it }, // Slide out to right (positive offset)
        animationSpec = tween(300)
    )
}

private fun navigateAfterLogin(
    navController: NavHostController,
    role: UserRole,
) {
    when (role) {
        UserRole.USER -> navController.navigate(Screen.ProjectSelection.route) {
            popUpTo(navController.graph.startDestinationId) { inclusive = false }
            launchSingleTop = true
            restoreState = true
        }

        UserRole.APPROVER -> navController.navigate(Screen.BusinessHeadProjectsTab.route) {
            popUpTo(navController.graph.startDestinationId) { inclusive = false }
            launchSingleTop = true
            restoreState = true
        }

        UserRole.ADMIN -> navController.navigate(Screen.AdminDashboard.route) {
            popUpTo(navController.graph.startDestinationId) { inclusive = false }
            launchSingleTop = true
            restoreState = true
        }

        UserRole.BUSINESS_HEAD -> navController.navigate(Screen.BusinessHeadDashboard.route) {
            popUpTo(navController.graph.startDestinationId) { inclusive = false }
            launchSingleTop = true
            restoreState = true
        }

        UserRole.MANAGER -> navController.navigate(Screen.BusinessHeadProjectsTab.route) {
            popUpTo(navController.graph.startDestinationId) { inclusive = false }
            launchSingleTop = true
            restoreState = true
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Login.route,
    notificationIntent: android.content.Intent? = null,
    onNotificationConsumed: () -> Unit = {}
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    
    // Handle logout: Redirect unauthenticated users to login
    LaunchedEffect(authState.isAuthenticated) {
        val currentRoute = navController.currentDestination?.route
        
        // If user is not authenticated and we're not on login screen, navigate to login
        if (!authState.isAuthenticated && currentRoute != Screen.Login.route && currentRoute != null) {
            android.util.Log.d("AppNavHost", "🚪 User logged out - navigating to login")
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    // ======= NOTIFICATION DEEP-LINK NAVIGATION =======
    // Fires when a notification intent is present AND user is authenticated
    LaunchedEffect(notificationIntent, authState.isAuthenticated) {
        if (notificationIntent == null || !authState.isAuthenticated) return@LaunchedEffect
        
        val projectId = notificationIntent.getStringExtra("projectId") ?: ""
        val chatId = notificationIntent.getStringExtra("chatId") ?: ""
        val expenseId = notificationIntent.getStringExtra("expenseId") ?: ""
        val navigationTarget = notificationIntent.getStringExtra("navigationTarget") ?: ""
        val notificationType = notificationIntent.getStringExtra("notificationType") ?: ""
        val notificationTitle = notificationIntent.getStringExtra("title")
            ?: notificationIntent.getStringExtra("notificationTitle")
            ?: ""
        val notificationMessage = notificationIntent.getStringExtra("message")
            ?: notificationIntent.getStringExtra("body")
            ?: notificationIntent.getStringExtra("notificationBody")
            ?: notificationIntent.getStringExtra("notification_body")
            ?: notificationIntent.getStringExtra("text")
            ?: notificationIntent.getStringExtra("description")
            ?: notificationIntent.getStringExtra("content")
            ?: ""
        val customerId = notificationIntent.getStringExtra("customerId") ?: ""
        val fromNotificationExtra = notificationIntent.getStringExtra("fromNotification") ?: "false"
        val expenseListNotificationTag = expenseListRouteArgument(
            buildExpenseListNotificationTag(
                notificationType = notificationType,
                navigationTarget = navigationTarget,
                title = notificationTitle,
                message = notificationMessage
            ).takeUnless { it == EXPENSE_LIST_NO_NOTIFICATION } ?: fromNotificationExtra
        )
        val ChatUserName = notificationMessage.split(" ").getOrNull(0) ?: "Chat"
        
        
        // Skip if no useful navigation data
        if (projectId.isEmpty() && expenseId.isEmpty() && chatId.isEmpty()) {
            onNotificationConsumed()
            return@LaunchedEffect
        }
        
        
        // Determine user role
        val userRole = authViewModel.getUserRoleByPhoneLogic() ?: ""

        // For MANAGER: if the notified project is IN_REVIEW, open the review screen directly
        if (userRole == "MANAGER" && projectId.isNotEmpty() && customerId.isNotEmpty()) {
            try {
                val projectDoc = FirebaseFirestore.getInstance()
                    .collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(projectId)
                    .get()
                    .await()
                val projectStatus = projectDoc.getString("status") ?: ""
                if (projectStatus == "IN_REVIEW") {
                    delay(300)
                    navController.navigate(Screen.ApproverProjectReview.createRoute(projectId)) {
                        launchSingleTop = true
                    }
                    onNotificationConsumed()
                    return@LaunchedEffect
                }
            } catch (e: Exception) {
                android.util.Log.e("AppNavHost", "Error checking project status for MANAGER notification: ${e.message}")
            }
        }

        // For BUSINESS_HEAD: if the notified project is DECLINED, open NewProjectScreen for resubmission
        if (userRole == "BUSINESS_HEAD" && projectId.isNotEmpty() && customerId.isNotEmpty()) {
            try {
                val projectDoc = FirebaseFirestore.getInstance()
                    .collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(projectId)
                    .get()
                    .await()
                val projectStatus = projectDoc.getString("status") ?: ""
                val isDeclined = projectStatus.equals("DECLINED", ignoreCase = true) ||
                    projectStatus.equals("REVIEW REJECTED", ignoreCase = true)
                if (isDeclined) {
                    delay(300)
                    navController.navigate(Screen.NewProjectWithId.createRoute(projectId)) {
                        launchSingleTop = true
                    }
                    onNotificationConsumed()
                    return@LaunchedEffect
                }
            } catch (e: Exception) {
                android.util.Log.e("AppNavHost", "Error checking project status for BUSINESS_HEAD notification: ${e.message}")
            }
        }

        // Small delay to ensure navigation graph is ready
        delay(300)
        
        // Determine the target route based on screen type, notification type, and user role
        val targetRoute = when {
                // --- CHAT NOTIFICATIONS ---
                navigationTarget == "chat_detail" || navigationTarget == "chat_screen" ||
                notificationType.contains("chat_message", ignoreCase = true) ||
                notificationType.contains("customer_chat", ignoreCase = true) -> {
                    if (projectId.isNotEmpty() && chatId.isNotEmpty()) {
                        Screen.Chat.createRoute(projectId, chatId, ChatUserName)
                    } else null
                }
                
                // --- EXPENSE CHAT NOTIFICATIONS ---
                navigationTarget == "expense_chat" ||
                notificationType.contains("expense_chat", ignoreCase = true) -> {
                    if (expenseId.isNotEmpty()) {
                        when (userRole) {
                            "USER" -> Screen.UserExpenseChat.createRoute(expenseId)
                            "APPROVER" -> Screen.ApproverExpenseChat.createRoute(expenseId)
                            "BUSINESS_HEAD", "MANAGER" -> Screen.BusinessHeadReviewExpense.createRoute(expenseId)
                            else -> Screen.UserExpenseChat.createRoute(expenseId)
                        }
                    } else null
                }
                
                // --- EXPENSE REVIEW / STATUS NOTIFICATIONS ---
                navigationTarget == "expense_detail" || navigationTarget == "expense_review" ||
                notificationType.contains("expense_manager_review", ignoreCase = true) ||
                notificationType.contains("expense_BusinessHead_review", ignoreCase = true) ||
                notificationType.contains("expense_status_update", ignoreCase = true) ||
                notificationType.contains("expense_manager_update", ignoreCase = true) ||
                notificationType.contains("expense_BusinessHead_update", ignoreCase = true) || 
                notificationMessage.contains("your expense", ignoreCase = true) || 
                (notificationMessage.contains("payment", ignoreCase = true) && 
                notificationMessage.contains("updated", ignoreCase = true) || 
                notificationMessage.contains("completed", ignoreCase = true)) || (
                    notificationMessage.contains("of expense in", ignoreCase = true) && 
                    notificationMessage.contains("has been updated", ignoreCase = true)
                )
                -> {
                    if (expenseId.isNotEmpty()) {
                        when (userRole) {
                            "APPROVER" -> Screen.ReviewExpense.createRoute(expenseId, expenseListNotificationTag)
                            "BUSINESS_HEAD", "MANAGER" -> Screen.BusinessHeadReviewExpense.createRoute(expenseId, expenseListNotificationTag)
                            else -> Screen.ReviewExpense.createRoute(expenseId, expenseListNotificationTag)
                        }
                    } else if (projectId.isNotEmpty()) {
                        when (userRole) {
                            "APPROVER" -> Screen.ProjectPendingApprovals.createRoute(projectId)
                            "BUSINESS_HEAD", "MANAGER" -> Screen.BusinessHeadProjectPendingApprovals.createRoute(projectId)
                            else -> Screen.ExpenseList.createRoute(projectId, expenseListNotificationTag, notificationMessage)
                        }
                    } else null
                }
                
                // --- PROJECT REVIEW NOTIFICATIONS ---
                navigationTarget == "project_review" ||
                notificationType.contains("project_created", ignoreCase = true) ||
                notificationType.contains("status_in_review", ignoreCase = true) -> {
                    if (projectId.isNotEmpty()) {
                        when (userRole) {
                            "APPROVER" -> Screen.ApproverProjectReview.createRoute(projectId)
                            "BUSINESS_HEAD", "MANAGER" -> Screen.BusinessHeadProjectDashboard.createRoute(projectId)
                            else -> Screen.ExpenseList.createRoute(projectId, expenseListNotificationTag, notificationMessage)
                        }
                    } else null
                }
                
                // --- PROJECT CREATION / DECLINED (BusinessHead resubmission) ---
                navigationTarget == "project_creation" ||
                notificationType.contains("project_declined", ignoreCase = true) -> {
                    if (projectId.isNotEmpty()) {
                        when (userRole) {
                            "BUSINESS_HEAD" -> Screen.NewProjectWithId.createRoute(projectId)
                            else -> Screen.ExpenseList.createRoute(projectId, expenseListNotificationTag, notificationMessage)
                        }
                    } else null
                }
                
                // --- PROJECT DETAIL / GENERAL STATUS NOTIFICATIONS ---
                navigationTarget == "project_detail" ||
                notificationType.contains("project_assigned", ignoreCase = true) ||
                notificationType.contains("status_", ignoreCase = true) ||
                notificationType.contains("phase_", ignoreCase = true) ||
                notificationType.contains("department_", ignoreCase = true) ||
                notificationType.contains("project_date_", ignoreCase = true) ||
                notificationType.contains("user_added", ignoreCase = true) ||
                notificationType.contains("user_removed", ignoreCase = true) ||
                notificationType.contains("manager_added", ignoreCase = true) ||
                notificationType.contains("manager_removed", ignoreCase = true) ||
                notificationType.contains("temp_approver", ignoreCase = true) ||
                notificationType.contains("approver was updated", ignoreCase = true) ||
                notificationType.contains("approver_updated", ignoreCase = true) -> {
                    
                    if (projectId.isNotEmpty()) {
                        when (userRole) {
                            "APPROVER" -> Screen.ApproverProjectDashboard.createRoute(projectId, expenseListNotificationTag, notificationMessage)
                            "BUSINESS_HEAD", "MANAGER" -> Screen.BusinessHeadProjectDashboard.createRoute(projectId, expenseListNotificationTag, notificationMessage)
                            else -> Screen.ExpenseList.createRoute(projectId, expenseListNotificationTag, notificationMessage)
                        }
                    } else null
                }

                // --- FALLBACK: navigate to project dashboard if projectId exists ---
                else -> {
                    if (projectId.isNotEmpty()) {
                        when (userRole) {
                            "APPROVER" -> Screen.ApproverProjectDashboard.createRoute(projectId, expenseListNotificationTag, notificationMessage)
                            "BUSINESS_HEAD", "MANAGER" -> Screen.BusinessHeadProjectDashboard.createRoute(projectId, expenseListNotificationTag, notificationMessage)
                            "USER" -> Screen.ExpenseList.createRoute(projectId, expenseListNotificationTag, notificationMessage)
                            else -> null
                        }
                    } else null
                }
            }
        
        if (targetRoute != null) {
            
            navController.navigate(targetRoute) {
                // Don't pop the entire stack — just ensure single top
                launchSingleTop = true
            }
        } else {
            android.util.Log.w("AppNavHost", "🔔 No target route determined for notification")
        }
        
        // Mark as consumed so it doesn't re-fire
        onNotificationConsumed()
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth Flow
        composable(Screen.Login.route) {
            val isAccessRestricted by authViewModel.isAccessRestricted.collectAsState()
            val restrictedPhoneNumber by authViewModel.restrictedPhoneNumber.collectAsState()
            
            // Navigate to access restricted screen if access is denied
            LaunchedEffect(isAccessRestricted) {
                if (isAccessRestricted && restrictedPhoneNumber != null) {
                    android.util.Log.d("Navigation", "🚫 Access restricted, navigating to restriction screen")
                    navController.navigate("${Screen.AccessRestricted.route}/$restrictedPhoneNumber")
                }
            }
            
            // Navigation will be handled by the global LaunchedEffect above
            // No need for duplicate navigation logic here
            
            LoginScreen(
                onNavigateToOtp = { phoneNumber ->
                    navController.navigate("otp_verification/${phoneNumber}")
                },
                onSkipForDevelopment = {
                    // Development skip - navigation handled by direct callback
                    android.util.Log.d("Navigation", "🔍 Development skip navigation handled by direct callback")
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.AdminSignUp.route)
                },
                onNavigateToRole = { role ->
                    android.util.Log.d("Navigation", "🎯 Direct navigation callback received")
                    android.util.Log.d("Navigation", "🎯 Received role: $role")
                    android.util.Log.d("Navigation", "🎯 Role type: ${role.javaClass.simpleName}")
                    val requiresCustomerSelection = authViewModel.requiresCustomerSelection.value
                    if ((role == UserRole.USER || role == UserRole.APPROVER) && requiresCustomerSelection) {
                        navController.navigate(Screen.CustomerSelection.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        navigateAfterLogin(navController, role)
                    }
                }
            )
        }
        
        // CRITICAL: Login screen (start destination) has NO animations - it's the entry point
        // All other screens use iOS-style horizontal slide animations
        
        composable(
            route = Screen.AdminSignUp.route,
            // iOS-style slide animations: forward navigation slides in from right
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            // Back navigation: previous screen slides in from left
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            val authViewModel: AuthViewModel = hiltViewModel()
            val authState by authViewModel.authState.collectAsState()
            
            AdminSignUpScreen(
                onNavigateBack = {
                    // Navigate back to login screen after account creation
                    navController.popBackStack()
                },
                onSignUpSuccess = {
                    navController.navigate(Screen.BusinessHeadDashboard.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        
        composable(
            route = "${Screen.AccessRestricted.route}/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            val authViewModel: AuthViewModel = hiltViewModel()
            
            AccessRestrictedScreen(
                phoneNumber = phoneNumber,
                onNavigateBack = {
                    authViewModel.clearAccessRestriction()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Splash screen removed – direct navigation is handled above
        
        composable(
            route = "otp_verification/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            val authViewModel: AuthViewModel = hiltViewModel(
                viewModelStoreOwner = remember(backStackEntry) { navController.getBackStackEntry(Screen.Login.route) }
            )
            
            OtpVerificationScreen(
                phoneNumber = phoneNumber,
                authViewModel = authViewModel,
                onVerificationSuccess = {
                    val currentUser = authViewModel.authState.value.user
                    if (currentUser == null) {
                        return@OtpVerificationScreen
                    }

                    if (authViewModel.requiresCustomerSelection.value) {
                        navController.navigate(Screen.CustomerSelection.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        navigateAfterLogin(navController, currentUser.role)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CustomerSelection.route,
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            CustomerSelectionScreen(
                authViewModel = authViewModel,
                onCustomerSelected = {
                    val role = authViewModel.authState.value.user?.role ?: UserRole.USER
                    navigateAfterLogin(navController, role)
                }
            )
        }
        
        // Project Selection
        composable(
            route = Screen.ProjectSelection.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            // BusinessHeadProjectsTab will fetch userRole directly from authState.user
            val businessHeadViewModel: BusinessHeadViewModel = hiltViewModel()
            BusinessHeadProjectsTab(
                    onNavigateToProject = { projectId, project, fromNotification, NotificationMessage ->
                        // Preload project in ViewModel before navigation (Project contains Timestamp which is not Serializable)
                        // This allows screen to open immediately without loading
                        project?.let {
                            businessHeadViewModel.preloadProjectForEdit(it)
                            android.util.Log.d("AppNavHost", "📦 Preloading project in ViewModel: ${it.name} (${it.id})")
                        }
                        navController.navigate(
                            Screen.ExpenseList.createRoute(
                                projectId,
                                fromNotification = expenseListRouteArgument(fromNotification),
                                notificationMessage = NotificationMessage ?: ""
                            )
                        )
                    },
                    onNavigateToNewProject = {
                        // Not used for this navigation context
                    },
                    onNavigateToEditProject = { projectId ->
                        // Not used for this navigation context
                    },
                    onNavigateToNewProjectForResubmission = { projectId ->
                        // Not used for this navigation context
                    },
                    onNavigateToOverallReports = {
                        // Not used for this navigation context
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Screen.NotificationList.route)
                    },
                    onNavigateToChat = { projectId, chatId, otherUserName ->
                        navController.navigate(Screen.Chat.createRoute(projectId, chatId, otherUserName))
                    },
                    onNavigateToExpenseReview = { expenseId, notifTypeStr ->
                        android.util.Log.d("AppNavHost", "🔔 ProjectSelection → Expense Review: expenseId=$expenseId, notifTypeStr=$notifTypeStr")
                        navController.navigate(Screen.ReviewExpense.createRoute(expenseId, notifTypeStr))
                    },
                    onNavigateToExpenseChat = { expenseId ->
                        android.util.Log.d("AppNavHost", "🔔 ProjectSelection → Expense Chat: expenseId=$expenseId")
                        navController.navigate(Screen.UserExpenseChat.createRoute(expenseId))
                    },
                    onNavigateToProfile = { userRole ->
                        navController.currentBackStackEntry?.savedStateHandle?.set("profileUserRole", userRole.name)
                        navController.navigate(Screen.UserProfile.route)
                    },
                    onNavigateToProjectDetail = { projectId, project, fromNotification, NotificationMessage ->
                        project?.let {
                            businessHeadViewModel.preloadProjectForEdit(it)
                            android.util.Log.d("AppNavHost", "📦 Preloading user project in ViewModel: ${it.name} (${it.id})")
                        }
                        navController.navigate(
                            Screen.ExpenseList.createRoute(
                                projectId,
                                fromNotification = expenseListRouteArgument(fromNotification),
                                notificationMessage = NotificationMessage ?: ""
                            )
                        )
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
        }
        
        // Notification Screen
        composable(
            route = Screen.NotificationList.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            val authState = authViewModel.authState.collectAsState().value
            val currentUser = authState.user
            // Fetch userRole using phone number logic
            var userRole by remember { mutableStateOf<String?>(null) }
            
            LaunchedEffect(Unit) {
                userRole = authViewModel.getUserRoleByPhoneLogic()
            }
            
            val userRoleString = userRole ?: ""
            val currentUserId = currentUser?.phone ?: currentUser?.uid ?: ""
            
            // Check if user is Business Head or Manager using fetched role
            val isBusinessHead = userRoleString == "BUSINESS_HEAD" || userRoleString == "MANAGER"
            
            // For Business Head, ALWAYS show ProjectNotificationListScreen with three sections
            if (isBusinessHead) {
                ProjectNotificationListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProject = { projectId, fromNotification, NotificationMessage ->
                        // No project data available here, pass null - will fetch from Firebase
                        val currentEntry = navController.currentBackStackEntry
                        currentEntry?.savedStateHandle?.set("preloadedProject", null)
                        navController.navigate(Screen.BusinessHeadProjectDashboard.createRoute(projectId, fromNotification, NotificationMessage))
                    },
                    onNavigateToPendingApprovals = { projectId ->
                        navController.navigate(Screen.BusinessHeadProjectPendingApprovals.createRoute(projectId))
                    },
                    onNavigateToNewProjectForResubmission = { projectId ->
                        navController.navigate(Screen.NewProjectWithId.createRoute(projectId))
                    },
                    onViewAllNotifications = {
                        // Not used - bottom sheet is shown instead
                    },
                    onNavigateToChat = { projectId, chatId, otherUserName ->
                        navController.navigate(Screen.Chat.createRoute(projectId, chatId, otherUserName))
                    },
                    currentUserId = currentUserId,
                    userRole = userRoleString.ifEmpty { "BUSINESS_HEAD" }, // Fallback to ensure role is set
                    authViewModel = authViewModel
                )
            } else {
                val notifUserRole = userRole ?: ""
                NotificationListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProject = { projectId, fromNotification, NotificationMessage ->
                        when (notifUserRole) {
                            "APPROVER" -> navController.navigate(
                                Screen.ApproverProjectDashboard.createRoute(
                                    projectId,
                                    fromNotification = expenseListRouteArgument(fromNotification), NotificationMessage = NotificationMessage
                                )
                            )
                            else -> navController.navigate(
                                Screen.ExpenseList.createRoute(
                                    projectId,
                                    expenseListRouteArgument(fromNotification), notificationMessage = NotificationMessage
                                )
                            )
                        }
                    },
                    onNavigateToExpense = { projectId, expenseId, fromNotification ->
                        // expenseId present → go to expense chat; otherwise fall back to project
                        when {
                            expenseId.isNotEmpty() -> when (notifUserRole) {
                                "APPROVER" -> navController.navigate(Screen.ApproverExpenseChat.createRoute(expenseId))
                                else -> navController.navigate(Screen.UserExpenseChat.createRoute(expenseId))
                            }
                            projectId.isNotEmpty() -> when (notifUserRole) {
                                "APPROVER" -> navController.navigate(
                                    Screen.ApproverProjectDashboard.createRoute(
                                        projectId,
                                        fromNotification = expenseListRouteArgument(fromNotification)
                                    )
                                )
                                else -> navController.navigate(
                                    Screen.ExpenseList.createRoute(
                                        projectId,
                                        expenseListRouteArgument(fromNotification)
                                    )
                                )
                            }
                        }
                    },
                    onNavigateToPendingApprovals = { projectId, fromNotification ->
                        when (notifUserRole) {
                            "APPROVER" -> navController.navigate(Screen.ProjectPendingApprovals.createRoute(projectId))
                            else -> navController.navigate(
                                Screen.ExpenseList.createRoute(
                                    projectId,
                                    expenseListRouteArgument(fromNotification)
                                )
                            )
                        }
                    },
                    onNavigateToChat = { projectId, chatId, otherUserName ->
                        navController.navigate(Screen.Chat.createRoute(projectId, chatId, otherUserName))
                    },
                    authViewModel = authViewModel
                )
            }
        }
        
        // Project-specific Notification Screen
        composable(
            route = Screen.ProjectNotifications.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val projectViewModel: ProjectViewModel = hiltViewModel()
            
            // Ensure projects are loaded
            LaunchedEffect(Unit) {
                projectViewModel.loadProjects()
            }
            
            // Get the project from the viewmodel
            val projects by projectViewModel.projects.collectAsState()
            val isLoading by projectViewModel.isLoading.collectAsState()
            val selectedProject = projects.find { it.id == projectId }
            
            when {
                // Still loading — show a spinner instead of "not found" immediately
                isLoading && selectedProject == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF4285F4))
                    }
                }
                selectedProject != null -> {
                    ProjectNotificationScreen(
                        projectId = projectId,
                        projectName = selectedProject.name,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToProject = { projectId, fromNotification, NotificationMessage ->
                            navController.navigate(
                                Screen.ExpenseList.createRoute(
                                    projectId,
                                    expenseListRouteArgument(fromNotification), notificationMessage = NotificationMessage
                                )
                            )
                        },
                        onNavigateToExpense = { projectId, expenseId, fromNotification ->
                            if (expenseId.isNotEmpty()) {
                                navController.navigate(Screen.UserExpenseChat.createRoute(expenseId))
                            } else {
                                navController.navigate(
                                    Screen.ExpenseList.createRoute(
                                        projectId,
                                        expenseListRouteArgument(fromNotification)
                                    )
                                )
                            }
                        },
                        onNavigateToPendingApprovals = { projectId, fromNotification ->
                            navController.navigate(
                                Screen.ExpenseList.createRoute(
                                    projectId,
                                    expenseListRouteArgument(fromNotification)
                                )
                            )
                        },
                        onNavigateToChat = { projectId, chatId, otherUserName ->
                            navController.navigate(Screen.Chat.createRoute(projectId, chatId, otherUserName))
                        },
                        authViewModel = authViewModel
                    )
                }
                else -> {
                    // Project not found after loading finished
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "❌ Project Not Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The selected project could not be loaded.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4)
                            )
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
        
        // Expense Flow
        composable(
            route = Screen.ExpenseList.route,
            arguments = listOf(
                        navArgument("projectId") { type = NavType.StringType },
                                navArgument("fromNotification") { type = NavType.StringType },
                                navArgument("notificationMessage") { type = NavType.StringType; defaultValue = "" }
                                    ),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            Log.d("AppNavHostNotification", "🚀 Navigated to ExpenseList with projectId: $projectId")
            val projectViewModel: ProjectViewModel = hiltViewModel()
            val fromNotification = Uri.decode(backStackEntry.arguments?.getString("fromNotification") ?: "")
            val notificationMessage = Uri.decode(backStackEntry.arguments?.getString("notificationMessage") ?: "")
            // Always load project freshly from Firebase by projectId
            LaunchedEffect(projectId) {
                if (projectId.isNotEmpty()) {
                    projectViewModel.loadProjectById(projectId)
                }
            }

            val currentProject by projectViewModel.currentProject.collectAsState()
            val isLoading by projectViewModel.isLoading.collectAsState()
            val error by projectViewModel.error.collectAsState()

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF007AFF))
                    }
                }
                currentProject != null -> {
                    ExpenseListScreen(
                        project = currentProject!!,
                        fromNotification = fromNotification,
                        notificationMessage = notificationMessage,
                        onNavigateBack = { navController.popBackStack() },
                        onAddExpense = {
                            navController.navigate(Screen.AddExpense.createRoute(projectId))
                        },
                        onTrackSubmissions = {
                            navController.navigate(Screen.TrackSubmissions.createRoute(projectId))
                        },
                        onNavigateToNotifications = { id ->
                            navController.navigate(Screen.ProjectNotifications.createRoute(id))
                        },
                        onShowAllExpenses = {
                            navController.navigate(Screen.AllExpenses.createRoute(projectId))
                        },
                        navController = navController
                    )
                } else -> {
                    // Error state after loading completed
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "❌ Project Not Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error ?: "The selected project could not be loaded.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
        
        composable(
            route = Screen.AddExpense.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("expenseId") { 
                    type = NavType.StringType
                    defaultValue = "0" // Use "0" as placeholder when adding new expense
                }
            ),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            // Extract expenseId from route arguments (set when navigating from edit)
            val expenseIdArg = backStackEntry.arguments?.getString("expenseId") ?: "0"
            val expenseId = expenseIdArg.takeIf { it != "0" && it.isNotEmpty() }
            
            val authViewModel: AuthViewModel = hiltViewModel()
            val authState by authViewModel.authState.collectAsState()
            
            // Load project directly using ProjectViewModel (consistent with other routes)
            val projectViewModel: ProjectViewModel = hiltViewModel()
            
            // Load the specific project directly by ID (faster than loading all projects)
            LaunchedEffect(projectId) {
                if (projectId.isNotEmpty()) {
                    projectViewModel.loadProjectById(projectId)
                }
            }
            
            // Get the project and loading state from the viewmodel
            val currentProject by projectViewModel.currentProject.collectAsState()
            val isLoading by projectViewModel.isLoading.collectAsState()
            val error by projectViewModel.error.collectAsState()
            
            when {
                isLoading || authState.isLoading -> {
                    // Show loading while project OR auth is still loading
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF007AFF)
                        )
                    }
                }
                currentProject != null && authState.user != null -> {
                    // Use user data from AuthViewModel (works for all user roles)
                    val currentUser = authState.user
                    val userName = currentUser?.name ?: ""
                    val userPhone = currentUser?.phone ?: ""

                    // Load role asynchronously; show spinner until resolved
                    var userRole by remember { mutableStateOf<String?>(null) }
                    var isRoleLoading by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        userRole = authViewModel.getUserRoleByPhoneLogic()
                        isRoleLoading = false
                    }

                    when {
                        isRoleLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF007AFF))
                            }
                        }
                        else -> {
                            val isBusinessHead = userRole == "BUSINESS_HEAD"
                            // BusinessHead may have no phone in customers collection — that's fine
                            // because their userId is hardcoded to "BUSINESS_HEAD" anyway.
                            val canShowScreen = userName.isNotEmpty() &&
                                (isBusinessHead || userPhone.isNotEmpty())

                            if (canShowScreen) {
                                AddExpenseScreen(
                                    project = currentProject!!,
                                    userId = if (!isBusinessHead) userPhone else "BUSINESS_HEAD",
                                    userName = userName,
                                    expenseId = expenseId?.takeIf { it.isNotEmpty() },
                                    onNavigateBack = { navController.popBackStack() },
                                    onExpenseAdded = { navController.popBackStack() },
                                    onNavigateToExpenseChat = { projectId ->
                                        navController.navigate(Screen.ExpenseChat.createRoute(projectId))
                                    }
                                )
                            } else {
                                // User data is genuinely missing after role is confirmed
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "❌ User Data Not Available",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Unable to load user information. Please try again.",
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { navController.popBackStack() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4285F4)
                                        )
                                    ) {
                                        Text("Go Back")
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Project not found or error - show error and back button (only after loading completes)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (error != null) "❌ Error Loading Project" else "❌ Project Not Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error ?: "The selected project could not be loaded.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4)
                            )
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
        
        // Expense Chat Screen
        composable(
            route = Screen.ExpenseChat.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val projectViewModel: ProjectViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()
            val authState by authViewModel.authState.collectAsState()
            
            // Load the specific project directly by ID (faster than loading all projects)
            LaunchedEffect(projectId) {
                if (projectId.isNotEmpty()) {
                    projectViewModel.loadProjectById(projectId)
                }
            }
            
            // Get the project and loading state from the viewmodel
            val currentProject by projectViewModel.currentProject.collectAsState()
            val isLoading by projectViewModel.isLoading.collectAsState()
            val error by projectViewModel.error.collectAsState()
            
            when {
                isLoading -> {
                    // Show loading state instead of error
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF007AFF)
                        )
                    }
                }
                currentProject != null -> {
                    ExpenseChatScreen(
                        project = currentProject!!,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                else -> {
                    // Project not found - show error and back button (only after loading completes)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "❌ Project Not Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error ?: "The requested project could not be found.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4)
                            )
                        ) {
                            Text("Go Back", color = Color.White)
                        }
                    }
                }
            }
        }
        
        composable(
            route = Screen.TrackSubmissions.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val projectViewModel: ProjectViewModel = hiltViewModel()
            
            // Load the specific project directly by ID (faster than loading all projects)
            LaunchedEffect(projectId) {
                if (projectId.isNotEmpty()) {
                    projectViewModel.loadProjectById(projectId)
                }
            }
            
            // Get the project and loading state from the viewmodel
            val currentProject by projectViewModel.currentProject.collectAsState()
            val isLoading by projectViewModel.isLoading.collectAsState()
            val error by projectViewModel.error.collectAsState()
            
            when {
                isLoading -> {
                    // Show loading state instead of error
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF007AFF)
                        )
                    }
                }
                currentProject != null -> {
                    TrackSubmissionsScreen(
                        project = currentProject!!,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToExpenseChat = { expenseId ->
                            navController.navigate(Screen.UserExpenseChat.createRoute(expenseId))
                        }
                    )
                }
                else -> {
                    // Project not found - show error and back button
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "❌ Project Not Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The selected project could not be loaded.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4)
                            )
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }

        composable(
            route = Screen.UserExpenseChat.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
            UserExpenseChatScreen(
                expenseId = expenseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.AllExpenses.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            AllExpensesScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExpenseChat = { expenseId ->
                    navController.navigate(Screen.UserExpenseChat.createRoute(expenseId))
                },
                onNavigateToExpenseReview = { expenseId ->
                    navController.navigate(Screen.ReviewExpense.createRoute(expenseId))
                },
                onEditExpense = { expenseId ->
                    // Pass expenseId directly in the route for editing
                    navController.navigate(Screen.AddExpense.createRoute(projectId, expenseId))
                }
            )
        }
        
        // Request Status Screen
        composable(
            route = Screen.RequestStatus.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("phaseId") { type = NavType.StringType }
            ),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val phaseId = backStackEntry.arguments?.getString("phaseId") ?: ""
            com.cubiquitous.tracura.ui.view.user.RequestStatusScreen(
                projectId = projectId,
                phaseId = phaseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // User Dashboard (Alternative entry point)
        composable(
            route = Screen.UserDashboard.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            val authViewModel: AuthViewModel = hiltViewModel()
            UserDashboardScreen(
                onNavigateToProjectSelection = {
                    navController.navigate(Screen.ProjectSelection.route)
                },
                onNavigateToAddExpense = {
                    navController.navigate(Screen.ProjectSelection.route)
                },
                onNavigateToExpenseList = {
                    navController.navigate(Screen.ProjectSelection.route)
                },
                onNavigateToTrackSubmissions = {
                    navController.navigate(Screen.ProjectSelection.route)
                },
                onNavigateToChat = { projectId, projectName ->
                    navController.navigate(Screen.ChatList.createRoute(projectId, projectName))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Approver Flow
        composable(
            route = Screen.ApproverNotificationScreen.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            ApproverNotificationScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToApproverProjectDashboard = { projectId, fromNotification ->
                    navController.navigate(
                        Screen.ApproverProjectDashboard.createRoute(
                            projectId,
                            fromNotification = fromNotification
                        )
                    )
                },
                onNavigateToApproverProjectReview = { projectId ->
                    navController.navigate(Screen.ApproverProjectReview.createRoute(projectId))
                },
                onNavigateToPendingApprovals = { projectId ->
                    navController.navigate(Screen.ProjectPendingApprovals.createRoute(projectId))
                },
                onNavigateToChat = { projectId, chatId, otherUserName ->
                    navController.navigate(Screen.Chat.createRoute(projectId, chatId, otherUserName))
                },
                authViewModel = authViewModel
            )
        }
        
        composable(
            route = Screen.ApproverProjectDashboard.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("fromNotification") { type = NavType.StringType },
                navArgument("NotificationMessage") { type = NavType.StringType; defaultValue = "" }
            ),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")?.takeIf { it.isNotBlank() } ?: ""
            val fromNotification = backStackEntry.arguments?.getString("fromNotification") ?: ""
            val notificationMessage = backStackEntry.arguments?.getString("NotificationMessage") ?: ""
            
            // Validate projectId - if empty, navigate back to prevent crashes
            if (projectId.isEmpty()) {
                LaunchedEffect(Unit) {
                    android.util.Log.e("AppNavHost", "❌ ApproverProjectDashboard: Empty projectId, navigating back")
                    navController.popBackStack()
                }
                return@composable
            }
            
            // CRITICAL: Retrieve preloaded project directly from savedStateHandle (passed via navigation parameter)
            // Priority: savedStateHandle > ViewModel (for backward compatibility)
            // This contains all basic project data (budget, dates, team members, etc.) already fetched
            // Try to get from current entry first, then from previous entry (where it was set before navigation)
            val previousEntry = navController.previousBackStackEntry
            val preloadedProjectFromState = remember(backStackEntry, projectId, previousEntry) {
                try {
                    // Try current entry first
                    val projectFromCurrent = backStackEntry.savedStateHandle.get<Project>("preloadedProject")?.takeIf { it.id == projectId }
                    if (projectFromCurrent != null) {
                        android.util.Log.d("AppNavHost", "✅ Found project in current entry's savedStateHandle")
                        projectFromCurrent
                    } else {
                        // Try previous entry (where it was set before navigation)
                        val projectFromPrevious = previousEntry?.savedStateHandle?.get<Project>("preloadedProject")?.takeIf { it.id == projectId }
                        if (projectFromPrevious != null) {
                            android.util.Log.d("AppNavHost", "✅ Found project in previous entry's savedStateHandle")
                        }
                        projectFromPrevious
                    }
                } catch (e: Exception) {
                    android.util.Log.d("AppNavHost", "⚠️ Could not retrieve project from savedStateHandle for APPROVER: ${e.message}, falling back to ViewModel")
                    null
                }
            }
            
            // Fallback to ViewModel for backward compatibility (if not in savedStateHandle)
            // Try to get ViewModel from parent entry (BusinessHeadProjectsTab) where project was preloaded
            val parentEntry = remember(backStackEntry) {
                try {
                    navController.getBackStackEntry(Screen.BusinessHeadProjectsTab.route)
                } catch (e: Exception) {
                    null
                }
            }
            
            val approverProjectViewModel: ApproverProjectViewModel = if (parentEntry != null) {
                hiltViewModel(parentEntry)
            } else {
                hiltViewModel()
            }
            val selectedProject by approverProjectViewModel.selectedProject.collectAsState()
            val preloadedProjectFromViewModel = selectedProject?.takeIf { it.id == projectId }
            
            // Use project from savedStateHandle first, fallback to ViewModel
            val preloadedProject = preloadedProjectFromState ?: preloadedProjectFromViewModel

            // Log which source was used
            if (preloadedProject != null) {
                android.util.Log.d("AppNavHost", "✅ ApproverProjectDashboard: Using preloaded project: ${preloadedProject.name} - budget: ${preloadedProject.budget}, teamMembers: ${preloadedProject.teamMembers.size} (source: ${if (preloadedProjectFromState != null) "savedStateHandle" else "ViewModel"})")
            } else {
                android.util.Log.d("AppNavHost", "⚠️ ApproverProjectDashboard: No preloaded project found, ApproverProjectDashboardScreen will fetch from Firebase")
            }

            // Refresh budget summary when returning from PendingByCredit after a payment
            val paymentUpdated by backStackEntry.savedStateHandle.getStateFlow("paymentUpdated", false).collectAsState()
            LaunchedEffect(paymentUpdated) {
                if (paymentUpdated) {
                    backStackEntry.savedStateHandle["paymentUpdated"] = false
                    approverProjectViewModel.loadProjectBudgetSummary(projectId, forceRefresh = true)
                }
            }

            ApproverProjectDashboardScreen(
                projectId = projectId,
                NotificationMessage = notificationMessage,
                fromNotification = fromNotification,
                preloadedProject = preloadedProject,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPendingApprovals = { projectId ->
                    navController.navigate(Screen.ProjectPendingApprovals.createRoute(projectId))
                },
                onNavigateToAddExpense = {
                    navController.navigate(Screen.AddExpense.createRoute(projectId))
                },
                onNavigateToEditExpense = { expenseId ->
                    navController.navigate(Screen.AddExpense.createRoute(projectId, expenseId))
                },
                onNavigateToAllExpenses = {
                    navController.navigate(Screen.AllExpenses.createRoute(projectId))
                },
                onNavigateToReports = { projectId ->
                    navController.navigate(Screen.ApproverReports.createRoute(projectId))
                },
                onNavigateToAnalytics = { projectId ->
                    navController.navigate(Screen.ApproverAnalytics.createRoute(projectId))
                },
                onNavigateToPendingByCredit = { projectIdParam ->
                    val route = if (projectIdParam != null) {
                        Screen.PendingByCredit.createRoute(projectIdParam)
                    } else {
                        Screen.PendingByCredit.createRoute(null)
                    }
                    navController.navigate(route)
                },
                onNavigateToDepartmentDetail = { projectId: String, departmentName: String, budgetAllocated: Double?, spent: Double?, remaining: Double? ->
                    // Store budget values in current backStackEntry's savedStateHandle BEFORE navigation
                    // After navigation, this entry becomes the previous entry, and we can read from it
                    val currentEntry = navController.currentBackStackEntry
                    currentEntry?.savedStateHandle?.set("budgetAllocated", budgetAllocated)
                    currentEntry?.savedStateHandle?.set("spent", spent)
                    currentEntry?.savedStateHandle?.set("remaining", remaining)
                    
                    // Log to verify values are being set
                    android.util.Log.d("AppNavHost", "Setting budget values before navigation: allocated=$budgetAllocated, spent=$spent, remaining=$remaining")
                    
                    val route = Screen.DepartmentDetail.createRoute(projectId, departmentName)
                    navController.navigate(route)
                },
                onNavigateToProjectNotifications = { projectId ->
                    navController.navigate(Screen.ProjectNotifications.createRoute(projectId))
                },
                onNavigateToEditProject = { projectId ->
                    navController.navigate(Screen.EditProject.createRoute(projectId))
                },
                onNavigateToChat = { projectId, projectName ->
                    navController.navigate(Screen.ChatList.createRoute(projectId, projectName))
                },
                onNavigateToChatDetail = { projectId, chatId, otherUserName ->
                    navController.navigate(Screen.Chat.createRoute(projectId, chatId, otherUserName))
                },
                onNavigateToExpenseReview = { expenseId ->
                    navController.navigate(Screen.ReviewExpense.createRoute(expenseId))
                },
                onNavigateToExpenseDetail = { expenseId ->
                    // ApprovedExpenseDetailScreen navigation removed
                    // navController.navigate(Screen.ExpenseDetail.createRoute(expenseId))
                },
                onNavigateToExpenseChat = { expenseId ->
                    navController.navigate(Screen.ApproverExpenseChat.createRoute(expenseId))
                },
                onNavigateToProjectReview = { projectId ->
                    navController.navigate(Screen.ApproverProjectReview.createRoute(projectId))
                }
            )
        }
        
        composable(
            route = Screen.ApproverProjectReview.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ApproverProjectReviewScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() },
                onProjectApproved = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.CategoryDetail.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType }
            ),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            CategoryDetailScreen(
                projectId = projectId,
                categoryName = categoryName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.DepartmentDetail.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("departmentName") { type = NavType.StringType }
            ),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val departmentName = backStackEntry.arguments?.getString("departmentName") ?: ""
            // Get budget values from savedStateHandle if passed from parent screen
            // Values are set in the previous backStackEntry before navigation
            // Try to get from current entry first, then from previous entry
            val previousEntry = navController.previousBackStackEntry
            val budgetAllocated = backStackEntry.savedStateHandle.get<Double>("budgetAllocated")
                ?: previousEntry?.savedStateHandle?.get<Double>("budgetAllocated")
            val spent = backStackEntry.savedStateHandle.get<Double>("spent")
                ?: previousEntry?.savedStateHandle?.get<Double>("spent")
            val remaining = backStackEntry.savedStateHandle.get<Double>("remaining")
                ?: previousEntry?.savedStateHandle?.get<Double>("remaining")
            
            // Log to verify values are being passed
            android.util.Log.d("AppNavHost", "DepartmentDetail - Budget values: allocated=$budgetAllocated, spent=$spent, remaining=$remaining")
            
            DepartmentDetailScreen(
                projectId = projectId,
                departmentName = departmentName,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExpenseChat = { expenseId ->
                    navController.navigate(Screen.ApproverExpenseChat.createRoute(expenseId))
                },
                onNavigateToReview = { expenseId ->
                    navController.navigate(Screen.ReviewExpense.createRoute(expenseId))
                },
                onNavigateToDetail = { expenseId ->
                    // ApprovedExpenseDetailScreen navigation removed
                    // navController.navigate(Screen.ExpenseDetail.createRoute(expenseId))
                },
                initialBudgetAllocated = budgetAllocated,
                initialSpent = spent,
                initialRemaining = remaining,
                backStackEntry = backStackEntry
            )
        }
        
        composable(
            route = Screen.ApproverReports.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ApproverReportsScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.ApproverAnalytics.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            AnalyticsScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.OverallReports.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            val authViewModel: AuthViewModel = hiltViewModel()
            OverallReportsScreen(
                onNavigateBack = { navController.popBackStack() },
                authViewModel = authViewModel
            )
        }
        
        composable(
            route = Screen.PendingApprovals.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            val authViewModel: AuthViewModel = hiltViewModel()
            // Fetch userRole using phone number logic
            var userRole by remember { mutableStateOf<String?>(null) }
            
            LaunchedEffect(Unit) {
                userRole = authViewModel.getUserRoleByPhoneLogic()
            }
            
            val userRoleString = userRole ?: ""
            
            BusinessHeadPendingApprovals(
                projectId = null, // General pending approvals
                onNavigateBack = { navController.popBackStack() },
                onReviewExpense = { expenseId, role ->
                    // Store role in savedStateHandle before navigation
                    navController.currentBackStackEntry?.savedStateHandle?.set("userRole", role)
                    navController.navigate(Screen.ReviewExpense.createRoute(expenseId))
                },
                onNavigateToExpenseChat = { expenseId ->
                    navController.navigate(Screen.ApproverExpenseChat.createRoute(expenseId))
                },
                userRole = userRoleString
            )
        }
        
        composable(
            route = Screen.ProjectPendingApprovals.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val authViewModel: AuthViewModel = hiltViewModel()
            // Fetch userRole using phone number logic
            var userRole by remember { mutableStateOf<String?>(null) }
            
            LaunchedEffect(Unit) {
                userRole = authViewModel.getUserRoleByPhoneLogic()
            }
            
            val userRoleString = userRole ?: ""
            
            BusinessHeadPendingApprovals(
                projectId = projectId, // Pass the projectId for project-specific approvals
                onNavigateBack = { navController.popBackStack() },
                onReviewExpense = { expenseId, role ->
                    // Store role in savedStateHandle before navigation
                    backStackEntry.savedStateHandle["userRole"] = role
                    navController.navigate(Screen.ReviewExpense.createRoute(expenseId))
                },
                onNavigateToExpenseChat = { expenseId ->
                    navController.navigate(Screen.ApproverExpenseChat.createRoute(expenseId))
                }
            )
        }
        
        composable(
            route = Screen.ReviewExpense.route, // "review_expense/{expenseId}/{fromNotification}"
            arguments = listOf(
                navArgument("expenseId") { type = NavType.StringType },
                navArgument("fromNotification") {
                    type = NavType.StringType
                    defaultValue = "false"
                }
            ),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
            // readOnly is passed via savedStateHandle to avoid mixing query params with path params
            val isReadOnly = backStackEntry.savedStateHandle.get<Boolean>("readOnly") ?: false
            val fromNotification = backStackEntry.arguments?.getString("fromNotification") ?: "false"
            // Get role from savedStateHandle (passed from previous screen)
            val userRoleFromHandle = backStackEntry.savedStateHandle.get<String>("userRole")
            
            // If role not in savedStateHandle, fetch using phone number logic
            val authViewModel: AuthViewModel = hiltViewModel()
            var userRole by remember { mutableStateOf<String?>(userRoleFromHandle) }
            
            LaunchedEffect(Unit) {
                if (userRole == null) {
                    userRole = authViewModel.getUserRoleByPhoneLogic()
                }
            }
            
            val userRoleString = userRole ?: ""

            ReviewExpenseScreen(
                expenseId = expenseId,
                fromNotification = fromNotification,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExpenseChat = { expenseId ->
                    navController.navigate(Screen.ApproverExpenseChat.createRoute(expenseId))
                },
                userRole = userRoleString,
                // isReadOnly = isReadOnly
            )
        }
        
        composable(
            route = Screen.ApproverExpenseChat.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
            ApproverExpenseChatScreen(
                expenseId = expenseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Admin Flow
        composable(
            route = Screen.AdminDashboard.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            val authViewModel: AuthViewModel = hiltViewModel()
            AdminDashboardScreen(
                onNavigateToManageUsers = {
                    navController.navigate(Screen.ManageUsers.route)
                },
                onNavigateToManageProjects = {
                    navController.navigate(Screen.ManageProjects.route)
                },
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = Screen.ManageUsers.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            ManageUsersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.ManageProjects.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            ManageProjectsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.Reports.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            ReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.EditProject.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            EditProjectScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() },
                onProjectDeleted = {
                    // Try APPROVER/MANAGER home first, then BUSINESS_HEAD home
                    var popped = navController.popBackStack(
                        Screen.BusinessHeadProjectsTab.route,
                        inclusive = false
                    )
                    if (!popped) {
                        popped = navController.popBackStack(
                            Screen.BusinessHeadDashboard.route,
                            inclusive = false
                        )
                    }
                    if (!popped) navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.BusinessHeadProjectsTab.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            BusinessHeadProjectsTab(
                onNavigateToProject = { projectId, project, fromNotification, NotificationMessage ->
                    // Store project in savedStateHandle before navigation for direct parameter passing
                    // This avoids re-fetching project data in the destination screen
                    // Note: If Project is not Parcelable/Serializable, this will fail gracefully and fall back to ViewModel
                    try {
                        val currentEntry = navController.currentBackStackEntry
                        currentEntry?.savedStateHandle?.set("preloadedProject", project)
                        android.util.Log.d("AppNavHost", "📦 Storing project in savedStateHandle: ${project?.name} (${project?.id})")
                    } catch (e: Exception) {
                        android.util.Log.w("AppNavHost", "⚠️ Could not store project in savedStateHandle (Project may not be Parcelable): ${e.message}, will use ViewModel fallback")
                    }
                    navController.navigate(
                        Screen.BusinessHeadProjectDashboard.createRoute(
                            projectId,
                            fromNotification = expenseListRouteArgument(fromNotification),
                            NotificationMessage = NotificationMessage ?: ""
                        )
                    )
                },
                onNavigateToNewProject = {
                    navController.navigate(Screen.NewProject.route)
                },
                onNavigateToEditProject = { projectId ->
                    navController.navigate(Screen.EditProject.createRoute(projectId))
                },
                onNavigateToNewProjectForResubmission = { projectId ->
                    navController.navigate(Screen.NewProjectWithId.createRoute(projectId))
                },
                onNavigateToOverallReports = {
                    navController.navigate(Screen.BusinessHeadOverallReports.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.NotificationList.route)
                },
                onNavigateToChat = { projectId, chatId, otherUserName ->
                    navController.navigate(Screen.Chat.createRoute(projectId, chatId, otherUserName))
                },
                onNavigateToExpenseReview = { expenseId, notifTypeStr ->
                    android.util.Log.d("AppNavHost", "🔔 Approver → Expense Review: expenseId=$expenseId, notifTypeStr=$notifTypeStr")
                    navController.navigate(Screen.ReviewExpense.createRoute(expenseId, notifTypeStr))
                },
                onNavigateToExpenseChat = { expenseId ->
                    android.util.Log.d("AppNavHost", "🔔 Approver → Expense Chat: expenseId=$expenseId")
                    navController.navigate(Screen.ApproverExpenseChat.createRoute(expenseId))
                },
                onNavigateToProfile = { userRole ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("profileUserRole", userRole.name)
                    navController.navigate(Screen.UserProfile.route)
                },
                onNavigateToProjectReview = { projectId ->
                    // For APPROVER role to review IN_REVIEW projects
                    navController.navigate(Screen.ApproverProjectReview.createRoute(projectId))
                },
                onNavigateToProjectDetail = { projectId, project, fromNotification, NotificationMessage ->
                    // For USER role to navigate to project detail
                    try {
                        val currentEntry = navController.currentBackStackEntry
                        currentEntry?.savedStateHandle?.set("preloadedProject", project)
                    } catch (e: Exception) {
                        android.util.Log.w("AppNavHost", "⚠️ Could not store project in savedStateHandle: ${e.message}")
                    }
                    navController.navigate(
                        Screen.ExpenseList.createRoute(
                            projectId,
                            expenseListRouteArgument(fromNotification),
                            notificationMessage = NotificationMessage ?: ""
                        )
                    )
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = Screen.BusinessHeadDashboard.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            BusinessHeadMainScreen(
                onNavigateToProject = { projectId, project, fromNotification, NotificationMessage ->
                    // Store project in savedStateHandle before navigation for direct parameter passing
                    // If project is null, will fetch from Firebase
                    try {
                        val currentEntry = navController.currentBackStackEntry
                        currentEntry?.savedStateHandle?.set("preloadedProject", project)
                        android.util.Log.d("AppNavHost", "📦 Storing project in savedStateHandle: ${project?.name} (${project?.id})")
                    } catch (e: Exception) {
                        android.util.Log.w("AppNavHost", "⚠️ Could not store project in savedStateHandle: ${e.message}, will use ViewModel fallback")
                    }
                    navController.navigate(
                        Screen.BusinessHeadProjectDashboard.createRoute(
                            projectId,
                            fromNotification = expenseListRouteArgument(fromNotification),
                            NotificationMessage = NotificationMessage ?: ""
                        )
                    )
                },
                onNavigateToCreateUser = {
                    navController.navigate(Screen.CreateUser.route)
                },
                onNavigateToViewAllUsers = {
                    navController.navigate(Screen.ViewAllUsers.route)
                },
                onNavigateToDepartmentUserManagement = {
                    navController.navigate(Screen.DepartmentUserManagement.route)
                },
                onNavigateToRoleManagement = {
                    navController.navigate(Screen.RoleManagement.route)
                },
                onNavigateToNewProject = {
                    navController.navigate(Screen.NewProject.route)
                },
                onNavigateToEditProject = { projectId ->
                    navController.navigate(Screen.EditProject.createRoute(projectId))
                },
                onNavigateToNewProjectForResubmission = { projectId ->
                    navController.navigate(Screen.NewProjectWithId.createRoute(projectId))
                },
                onNavigateToOverallReports = {
                    navController.navigate(Screen.BusinessHeadOverallReports.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.NotificationList.route)
                },
                onNavigateToChat = { projectId, chatId, otherUserName ->
                    android.util.Log.w("NotificationNaviationChekcing", "BusinessHeadMainScreen → Chat: project=$projectId, chatId=$chatId, otherUserName=$otherUserName")
                    navController.navigate(Screen.Chat.createRoute(projectId, chatId, otherUserName))
                },
                onNavigateToExpenseReview = { expenseId, notifTypeStr ->
                    android.util.Log.d("AppNavHost", "🔔 BusinessHead → Expense Review: expenseId=$expenseId, notifTypeStr=$notifTypeStr")
                    navController.navigate(Screen.BusinessHeadReviewExpense.createRoute(expenseId, notifTypeStr))
                },
                onNavigateToExpenseChat = { expenseId ->
                    android.util.Log.d("AppNavHost", "🔔 BusinessHead → Expense Chat: expenseId=$expenseId")
                    navController.navigate(Screen.BusinessHeadReviewExpense.createRoute(expenseId))
                },
                onNavigateToSelectTemplate = {
                    navController.navigate(Screen.SelectTemplate.route)
                },
                onNavigateToProfile = { userRole ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("profileUserRole", userRole.name)
                    navController.navigate(Screen.UserProfile.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // User Profile Screen — data fetched here, passed as plain params to UserProfileScreen
        composable(
            route = Screen.UserProfile.route,
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            // Use FirebaseAuth UID directly — always authoritative, updates immediately on login/logout.
            // DO NOT use authState.user.phone as a key — the ViewModel can hold stale phone data
            // between user switches (uid updates correctly, phone may lag behind).
            val firebaseUid: String? = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

            Log.d("CurrentuserRole", "firebaseUid key = $firebaseUid")

            // All mutable state keyed only on uid — resets automatically on every login/logout
            var profileIsLoading    by remember(firebaseUid) { mutableStateOf(true) }
            var profileName         by remember(firebaseUid) { mutableStateOf("") }
            var profilePhone        by remember(firebaseUid) { mutableStateOf("") }
            var profileEmail        by remember(firebaseUid) { mutableStateOf<String?>(null) }
            var profileRole         by remember(firebaseUid) { mutableStateOf(UserRole.USER) }
            var profileDepartment   by remember(firebaseUid) { mutableStateOf<String?>(null) }
            var profileBusinessName by remember(firebaseUid) { mutableStateOf<String?>(null) }
            var profileBusinessType by remember(firebaseUid) { mutableStateOf<String?>(null) }
            var profileLocation     by remember(firebaseUid) { mutableStateOf<String?>(null) }
            var profileIsActive     by remember(firebaseUid) { mutableStateOf<Boolean?>(null) }
            var profileJoinDate     by remember(firebaseUid) { mutableStateOf<String?>(null) }

            LaunchedEffect(firebaseUid) {
                profileIsLoading = true
                profileName = ""; profilePhone = ""; profileEmail = null
                profileRole = UserRole.USER; profileDepartment = null
                profileBusinessName = null; profileBusinessType = null; profileLocation = null
                profileIsActive = null; profileJoinDate = null

                if (firebaseUid == null) { profileIsLoading = false; return@LaunchedEffect }

                try {
                    val roleString = authViewModel.getUserRoleByPhoneLogic() ?: "USER"
                    val db = FirebaseFirestore.getInstance()
                    
                    if (roleString == "USER" || roleString == "APPROVER") {
                        // Read phone directly from Firebase Auth — never stale
                        val rawPhone: String? = com.google.firebase.auth.FirebaseAuth
                            .getInstance().currentUser?.phoneNumber

                        val normalizedPhone: String? = rawPhone
                            ?.trim()?.removePrefix("+91")?.replace(" ", "")
                        // Try normalized phone first, then raw phone as fallback
                        val doc = if (normalizedPhone != null) {
                            val d = db.collection("users").document(normalizedPhone).get().await()
                            if (d.exists()) d
                            else {
                                // Fallback: try the raw phone in case it's stored differently
                                val rawPhoneTrimmed = rawPhone?.trim() ?: ""
                                if (rawPhoneTrimmed.isNotEmpty() && rawPhoneTrimmed != normalizedPhone) {
                                    db.collection("users").document(rawPhoneTrimmed).get().await()
                                } else d
                            }
                        } else null
                        if (doc != null && doc.exists()) {
                            profileName = doc.getString("name") ?: ""
                            profilePhone = doc.getString("phoneNumber") ?: rawPhone ?: ""
                            profileDepartment = doc.getString("department")
                            profileIsActive = doc.getBoolean("isActive")
                            // createdAt is a Firestore Timestamp in users collection, NOT a Long
                            profileJoinDate = runCatching {
                                doc.getTimestamp("createdAt")?.toDate()?.let { date ->
                                    java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                                        .format(date)
                                }
                            }.getOrNull()
                            profileRole = when (roleString) {
                                "APPROVER" -> UserRole.APPROVER
                                else -> UserRole.USER
                            }
                            android.util.Log.d("CurrentuserRole", "✅ Profile loaded from users collection for $profilePhone")
                        } else {
                            android.util.Log.w("CurrentuserRole", "⚠️ users doc not found for $normalizedPhone")
                        }
                    } else {
                        // BUSINESS_HEAD / ADMIN — customers collection is queried by EMAIL
                        // (document ID is auto-generated Firestore ID, NOT the Firebase Auth UID)
                        val firebaseEmail: String? = com.google.firebase.auth.FirebaseAuth
                            .getInstance().currentUser?.email
                        android.util.Log.d("CurrentuserRole", "🔍 Looking up customer by email: $firebaseEmail")

                        val querySnapshot = if (!firebaseEmail.isNullOrBlank()) {
                            db.collection("customers")
                                .whereEqualTo("email", firebaseEmail)
                                .limit(1)
                                .get()
                                .await()
                        } else null

                        val doc = querySnapshot?.documents?.firstOrNull()
                        if (doc != null && doc.exists()) {
                            val customer: Customer? = doc.toObject(Customer::class.java)
                            profileName = customer?.name ?: ""
                            profilePhone = customer?.phoneNumber ?: ""
                            val rawEmail: String? = customer?.email
                            profileEmail = if (!rawEmail.isNullOrBlank()) rawEmail else null
                            val rawBizName: String? = customer?.businessName
                            profileBusinessName = if (!rawBizName.isNullOrBlank()) rawBizName else null
                            val rawBizType: String? = customer?.businessType
                            profileBusinessType = if (!rawBizType.isNullOrBlank()) rawBizType else null
                            val rawLocation: String? = customer?.location
                            profileLocation = if (!rawLocation.isNullOrBlank()) rawLocation else null
                            profileJoinDate = runCatching {
                                java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                                    .format(customer?.createdAt?.toDate() ?: java.util.Date())
                            }.getOrNull()
                            profileRole = when (roleString) {
                                "ADMIN" -> UserRole.ADMIN
                                else -> UserRole.BUSINESS_HEAD
                            }
                            android.util.Log.d("CurrentuserRole", "✅ Profile loaded from customers for email=$firebaseEmail name=${profileName}")
                        } else {
                            android.util.Log.w("CurrentuserRole", "⚠️ customers doc not found for email=$firebaseEmail")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CurrentuserRole", "❌ Failed to load profile", e)
                } finally {
                    profileIsLoading = false
                }
            }

            UserProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                name = profileName,
                phone = profilePhone,
                email = profileEmail,
                role = profileRole,
                department = profileDepartment,
                businessName = profileBusinessName,
                businessType = profileBusinessType,
                location = profileLocation,
                isActive = profileIsActive,
                joinDate = profileJoinDate,
                isLoading = profileIsLoading
            )
        }
        
        composable(
            route = Screen.BusinessHeadProjectDashboard.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("fromNotification") { type = NavType.StringType },
                navArgument("NotificationMessage") { type = NavType.StringType; defaultValue = "" }
            ),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val fromNotification = backStackEntry.arguments?.getString("fromNotification") ?: ""
            val notificationMessage = backStackEntry.arguments?.getString("NotificationMessage") ?: ""

            // Log.d("ExpenseListNotification", "fromNotification $fromNotification , notificatjion messgage $notificationMessage")
            
            // CRITICAL: Retrieve preloaded project directly from savedStateHandle (passed via navigation parameter)
            // Priority: savedStateHandle > ViewModel (for backward compatibility)
            // This contains all basic project data (budget, dates, team members, etc.) already fetched
            // Note: Project contains Timestamp which is not Serializable, so we use ViewModel as fallback
            val preloadedProjectFromState = remember(backStackEntry, projectId) {
                try {
                    // Try to get from savedStateHandle - may fail if Project is not Parcelable/Serializable
                    // In that case, we'll fall back to ViewModel
                    backStackEntry.savedStateHandle.get<Project>("preloadedProject")?.takeIf { it.id == projectId }
                } catch (e: Exception) {
                    android.util.Log.d("AppNavHost", "⚠️ Could not retrieve project from savedStateHandle: ${e.message}, falling back to ViewModel")
                    null
                }
            }
            
            // Fallback to ViewModel for backward compatibility (if not in savedStateHandle)
            // CRITICAL: Always try ViewModel approach since savedStateHandle may fail due to Timestamp serialization
            val parentEntry = remember(backStackEntry) {
                try {
                    // Try to get parent entry - could be BusinessHeadProjectsTab or BusinessHeadDashboard
                    try {
                        navController.getBackStackEntry(Screen.BusinessHeadProjectsTab.route)
                    } catch (e: Exception) {
                        try {
                            navController.getBackStackEntry(Screen.BusinessHeadDashboard.route)
                        } catch (e2: Exception) {
                            null
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }
            
            val businessHeadViewModel: BusinessHeadViewModel = if (parentEntry != null) {
                hiltViewModel(parentEntry)
            } else {
                android.util.Log.d("AppNavHost", "⚠️ Could not access parent ViewModel, using current scope")
                hiltViewModel()
            }
            
            val preloadedProjectState by businessHeadViewModel.editingProject.collectAsState()
            val preloadedProjectFromViewModel = preloadedProjectState?.takeIf { it.id == projectId }
            
            // Use project from savedStateHandle first, fallback to ViewModel
            val preloadedProject = preloadedProjectFromState ?: preloadedProjectFromViewModel
            
            // Log which source was used
            if (preloadedProject != null) {
                android.util.Log.d("AppNavHost", "✅ Retrieved preloaded project: ${preloadedProject.name} (source: ${if (preloadedProjectFromState != null) "savedStateHandle" else "ViewModel"})")
            } else {
                android.util.Log.d("AppNavHost", "⚠️ No preloaded project found - will fetch from Firebase")
            }
            
            // If project is preloaded and matches, use it - otherwise will fetch from Firebase
            if (preloadedProject != null) {
                android.util.Log.d("AppNavHost", "✅ BusinessHeadProjectDashboard: Using preloaded project ${preloadedProject.name} - budget: ${preloadedProject.budget}, teamMembers: ${preloadedProject.teamMembers.size} (source: ${if (preloadedProjectFromState != null) "savedStateHandle" else "ViewModel"})")
            } else {
                android.util.Log.d("AppNavHost", "⚠️ BusinessHeadProjectDashboard: No preloaded project found, will fetch from Firebase")
            }

            // Refresh budget summary when returning from PendingByCredit after a payment
            val bhApproverProjectViewModel: ApproverProjectViewModel = hiltViewModel()
            val bhPaymentUpdated by backStackEntry.savedStateHandle.getStateFlow("paymentUpdated", false).collectAsState()
            LaunchedEffect(bhPaymentUpdated) {
                if (bhPaymentUpdated) {
                    backStackEntry.savedStateHandle["paymentUpdated"] = false
                    bhApproverProjectViewModel.loadProjectBudgetSummary(projectId, forceRefresh = true)
                }
            }

            ApproverProjectDashboardScreen(
                projectId = projectId,
                NotificationMessage = notificationMessage,
                fromNotification = fromNotification,
                preloadedProject = preloadedProject,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPendingApprovals = { projectIdParam ->
                    navController.navigate(Screen.BusinessHeadProjectPendingApprovals.createRoute(projectIdParam))
                },
                onNavigateToAddExpense = {
                    navController.navigate(Screen.AddExpense.createRoute(projectId))
                },
                onNavigateToEditExpense = { expenseId ->
                    navController.navigate(Screen.AddExpense.createRoute(projectId, expenseId))
                },
                onNavigateToAllExpenses = {
                    navController.navigate(Screen.AllExpenses.createRoute(projectId))
                },
                onNavigateToReports = { projectIdParam ->
                    navController.navigate(Screen.BusinessHeadReports.createRoute(projectIdParam))
                },
                onNavigateToAnalytics = { projectIdParam ->
                    navController.navigate(Screen.BusinessHeadAnalytics.createRoute(projectIdParam))
                },
                onNavigateToPendingByCredit = { projectIdParam ->
                    val route = if (projectIdParam != null) {
                        Screen.PendingByCredit.createRoute(projectIdParam)
                    } else {
                        Screen.PendingByCredit.createRoute(null)
                    }
                    navController.navigate(route)
                },
                onNavigateToProjectNotifications = { projectId ->
                    navController.navigate(Screen.ProjectNotifications.createRoute(projectId))
                },
                onNavigateToDepartmentDetail = { projectIdParam: String, departmentName: String, budgetAllocated: Double?, spent: Double?, remaining: Double? ->
                    // Store budget values in current backStackEntry's savedStateHandle BEFORE navigation
                    // After navigation, this entry becomes the previous entry, and we can read from it
                    val currentEntry = navController.currentBackStackEntry
                    currentEntry?.savedStateHandle?.set("budgetAllocated", budgetAllocated)
                    currentEntry?.savedStateHandle?.set("spent", spent)
                    currentEntry?.savedStateHandle?.set("remaining", remaining)
                    
                    val route = Screen.DepartmentDetail.createRoute(projectIdParam, departmentName)
                    navController.navigate(route)
                },
                onNavigateToDelegation = {
                    navController.navigate(Screen.Delegation.createRoute(projectId))
                },
                onNavigateToEditProject = { projectIdParam ->
                    navController.navigate(Screen.EditProject.createRoute(projectIdParam))
                },
                onNavigateToExpenseReview = { expenseId->
                    navController.navigate(Screen.ReviewExpense.createRoute(expenseId))
                },
                onNavigateToExpenseChat = { expenseId ->
                    navController.navigate(Screen.ApproverExpenseChat.createRoute(expenseId))
                },
                onNavigateToChat = { projectIdParam, projectName ->
                    navController.navigate(Screen.ChatList.createRoute(projectIdParam, projectName))
                },
                onNavigateToChatDetail = { projectIdParam, chatId, otherUserName ->
                    navController.navigate(Screen.Chat.createRoute(projectIdParam, chatId, otherUserName))
                }
            )
        }
        
        composable(
            route = Screen.Delegation.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            DelegationScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.BusinessHeadPendingApprovals.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            val authViewModel: AuthViewModel = hiltViewModel()
            // Fetch userRole using phone number logic
            var userRole by remember { mutableStateOf<String?>(null) }
            
            LaunchedEffect(Unit) {
                userRole = authViewModel.getUserRoleByPhoneLogic()
            }
            
            val userRoleString = userRole ?: ""
            
            BusinessHeadPendingApprovals(
                projectId = null, // Overall pending approvals
                onNavigateBack = { navController.popBackStack() },
                onReviewExpense = { expenseId, role ->
                    // Store role in savedStateHandle before navigation
                    navController.currentBackStackEntry?.savedStateHandle?.set("userRole", role)
                    navController.navigate(Screen.BusinessHeadReviewExpense.createRoute(expenseId))
                },
                onNavigateToExpenseChat = { expenseId ->
                    navController.navigate(Screen.ApproverExpenseChat.createRoute(expenseId))
                }
            )
        }
        
        composable(
            route = Screen.BusinessHeadProjectPendingApprovals.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val authViewModel: AuthViewModel = hiltViewModel()
            // Fetch userRole using phone number logic
            var userRole by remember { mutableStateOf<String?>(null) }
            
            LaunchedEffect(Unit) {
                userRole = authViewModel.getUserRoleByPhoneLogic()
            }
            
            val userRoleString = userRole ?: ""
            
            BusinessHeadPendingApprovals(
                projectId = projectId, // Project-specific pending approvals
                onNavigateBack = { navController.popBackStack() },
                onReviewExpense = { expenseId, role ->
                    // Store role in savedStateHandle before navigation
                    backStackEntry.savedStateHandle["userRole"] = role
                    navController.navigate(Screen.BusinessHeadReviewExpense.createRoute(expenseId))
                },
                onNavigateToExpenseChat = { expenseId ->
                    navController.navigate(Screen.ApproverExpenseChat.createRoute(expenseId))
                }
            )
        }
        
        composable(
            route = Screen.BusinessHeadReviewExpense.route,
            arguments = listOf(
                navArgument("expenseId") { type = NavType.StringType },
                navArgument("fromNotification") {
                    type = NavType.StringType
                    defaultValue = EXPENSE_LIST_NO_NOTIFICATION
                }
            ),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
            val fromNotification = backStackEntry.arguments?.getString("fromNotification") ?: EXPENSE_LIST_NO_NOTIFICATION
            // Get role from savedStateHandle (passed from previous screen)
            val userRoleFromHandle = backStackEntry.savedStateHandle.get<String>("userRole")
            
            // If role not in savedStateHandle, fetch using phone number logic
            val authViewModel: AuthViewModel = hiltViewModel()
            var userRole by remember { mutableStateOf<String?>(userRoleFromHandle) }
            
            LaunchedEffect(Unit) {
                if (userRole == null) {
                    userRole = authViewModel.getUserRoleByPhoneLogic()
                }
            }
            
            val userRoleString = userRole ?: ""
            
            ReviewExpenseScreen(
                expenseId = expenseId,
                fromNotification = fromNotification,
                onNavigateBack = { navController.popBackStack() },
                userRole = userRoleString
            )
        }
        
        composable(
            route = Screen.BusinessHeadReports.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            BusinessHeadReports(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.BusinessHeadAnalytics.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            BusinessHeadAnalytics(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "pending_by_credit/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectIdArg = backStackEntry.arguments?.getString("projectId") ?: "all"
            val projectId = if (projectIdArg == "all") null else projectIdArg
            val authViewModel: AuthViewModel = hiltViewModel()
            var userRole by remember { mutableStateOf<String?>(null) }
            
            LaunchedEffect(Unit) {
                userRole = authViewModel.getUserRoleByPhoneLogic()
            }
            
            val userRoleString = userRole ?: ""
            
            PendingByCreditScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() },
                onReviewExpense = { expenseId, role ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("userRole", role ?: userRoleString)
                    navController.navigate(Screen.BusinessHeadReviewExpense.createRoute(expenseId))
                },
                onPaymentSuccess = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("paymentUpdated", true)
                }
            )
        }
        
        composable(
            route = Screen.BusinessHeadCategoryDetail.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType }
            ),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            BusinessHeadCategoryDetail(
                projectId = projectId,
                categoryName = categoryName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.BusinessHeadOverallReports.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            BusinessHeadOverallReports(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProject = { projectId ->
                    // No project data available here, pass null - will fetch from Firebase
                    val currentEntry = navController.currentBackStackEntry
                    currentEntry?.savedStateHandle?.set("preloadedProject", null)
                    navController.navigate(Screen.BusinessHeadProjectDashboard.createRoute(projectId))
                }
            )
        }
        
        composable(
            route = Screen.CreateUser.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            CreateUserScreen(
                onNavigateBack = { 
                    // Simply pop back to previous screen
                    navController.popBackStack()
                },
                onUserCreated = { 
                    // Navigate back to Business Head Dashboard after creating user
                    navController.navigate(Screen.BusinessHeadDashboard.route) {
                        popUpTo(Screen.BusinessHeadDashboard.route) { inclusive = false }
                    }
                }
            )
        }
        
        composable(
            route = Screen.ViewAllUsers.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            ViewAllUsersScreen(
                onNavigateBack = { 
                    // Simply pop back to previous screen
                    navController.popBackStack()
                }
            )
        }
        
        // Department User Management Screen
        composable(
            route = Screen.DepartmentUserManagement.route,
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            DepartmentUserManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.RoleManagement.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            RoleManagementScreen(
                onNavigateBack = { 
                    // Simply pop back to previous screen
                    navController.popBackStack()
                }
            )
        }
        
        // New Project with draft ID (for loading draft projects)
        // IMPORTANT: This must come BEFORE NewProjectWithId to avoid route conflicts
        composable(
            route = Screen.NewProjectWithDraft.route,
            arguments = listOf(navArgument("draftId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            android.util.Log.d("AppNavHost", "🎯 NewProjectWithDraft composable called!")
            val draftId = backStackEntry.arguments?.getString("draftId")
            val viewModel: BusinessHeadViewModel = hiltViewModel()
            
            android.util.Log.d("AppNavHost", "📥 Loading NewProject with draft ID: $draftId")
            android.util.Log.d("AppNavHost", "📥 Route: ${backStackEntry.destination.route}")
            android.util.Log.d("AppNavHost", "📥 Arguments: ${backStackEntry.arguments}")
            android.util.Log.d("AppNavHost", "📥 Draft ID extracted: $draftId")
            
            NewProjectScreen(
                onNavigateBack = { navController.popBackStack() },
                onProjectCreated = { navController.popBackStack() },
                onNavigateToDraftProjects = {
                    navController.navigate(Screen.DraftProjects.route)
                },
                selectedDraftId = draftId,
                onNavigateToReview = { projectName, description, client, clientPrimaryNumber, clientSecondaryNumber, location, currency, plannedStartDate, handoverDate, maintenanceDate, phases, approver, teamMembers, userAssignments, approverAssignments ->
                    // Debug logging before storing
                    android.util.Log.d("AppNavHost", "🚀 Navigating to Review with ${phases.size} phases (Draft)")
                    phases.forEachIndexed { idx, phase ->
                        val phaseBudget = phase.departments.sumOf { it.totalBudget }
                        android.util.Log.d("AppNavHost", "  Phase ${idx + 1}: ${phase.phaseName}, Budget: $phaseBudget, Departments: ${phase.departments.size}")
                    }

                    // Store data temporarily in ViewModel and navigate
                    viewModel.setReviewData(projectName, description, client, clientPrimaryNumber, clientSecondaryNumber, location, currency, plannedStartDate, handoverDate, maintenanceDate, phases, approver, teamMembers, userAssignments, approverAssignments)
                    navController.navigate(Screen.ReviewProject.route)
                },
                viewModel = viewModel
            )
        }

        // New Project with projectId (for resubmitting REVIEW REJECTED projects)
        composable(
            route = Screen.NewProjectWithId.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val viewModel: BusinessHeadViewModel = hiltViewModel()
            
            // Set projectIdForResubmission in ViewModel
            LaunchedEffect(projectId) {
                viewModel.projectIdForResubmission = projectId
            }
            
            NewProjectScreen(
                onNavigateBack = { navController.popBackStack() },
                onProjectCreated = {
                    navController.popBackStack()
                },
                onNavigateToDraftProjects = {
                    navController.navigate(Screen.DraftProjects.route)
                },
                projectIdForResubmission = projectId,
                onNavigateToReview = { projectName, description, client, clientPrimaryNumber, clientSecondaryNumber, location, currency, plannedStartDate, handoverDate, maintenanceDate, phases, approver, teamMembers, userAssignments, approverAssignments ->
                    // Debug logging before storing
                    android.util.Log.d("AppNavHost", "🚀 Navigating to Review with ${phases.size} phases (Resubmission)")
                    phases.forEachIndexed { idx, phase ->
                        val phaseBudget = phase.departments.sumOf { it.totalBudget }
                        android.util.Log.d("AppNavHost", "  Phase ${idx + 1}: ${phase.phaseName}, Budget: $phaseBudget, Departments: ${phase.departments.size}")
                    }

                    // Store data temporarily in ViewModel and navigate
                    // Keep projectIdForResubmission in ViewModel so ReviewProjectScreen can use it
                    viewModel.setReviewData(projectName, description, client, clientPrimaryNumber, clientSecondaryNumber, location, currency, plannedStartDate, handoverDate, maintenanceDate, phases, approver, teamMembers, userAssignments, approverAssignments)
                    navController.navigate(Screen.ReviewProject.route)
                },
                viewModel = viewModel
            )
        }
        
        composable(
            route = Screen.NewProject.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            android.util.Log.d("AppNavHost", "⚠️ Regular NewProject route called (not draft route)")
            // This ViewModel instance will be the source of truth for review data.
            val viewModel: BusinessHeadViewModel = hiltViewModel()
            val savedStateHandle = backStackEntry.savedStateHandle
            
            // Observe loadDraftId from savedStateHandle - use a key to force recomposition
            var loadDraftId by remember { mutableStateOf<String?>(null) }
            
            // Check for draft ID when screen becomes visible or recomposes
            LaunchedEffect(backStackEntry) {
                // Check immediately when composable is created
                val draftId = savedStateHandle.get<String>("loadDraftId")
                if (draftId != null) {
                    loadDraftId = draftId
                    android.util.Log.d("AppNavHost", "📥 Found draft ID in savedStateHandle: $draftId")
                    // Don't remove immediately - let NewProjectScreen handle it after loading
                }
            }
            
            // Also check when savedStateHandle might have changed (e.g., when returning from DraftProjects)
            LaunchedEffect(Unit) {
                // Re-check savedStateHandle periodically to catch updates
                kotlinx.coroutines.delay(100) // Small delay to ensure savedStateHandle is updated
                val draftId = savedStateHandle.get<String>("loadDraftId")
                if (draftId != null && draftId != loadDraftId) {
                    loadDraftId = draftId
                    android.util.Log.d("AppNavHost", "📥 Draft ID updated after delay: $draftId")
                }
            }
            
            NewProjectScreen(
                onNavigateBack = { navController.popBackStack() },
                onProjectCreated = { navController.popBackStack() },
                onNavigateToDraftProjects = {
                    navController.navigate(Screen.DraftProjects.route)
                },
                selectedDraftId = loadDraftId,
                onNavigateToReview = { projectName, description, client, clientPrimaryNumber, clientSecondaryNumber, location, currency, plannedStartDate, handoverDate, maintenanceDate, phases, approver, teamMembers, userAssignments, approverAssignments ->
                    // Debug logging before storing
                    android.util.Log.d("AppNavHost", "🚀 Navigating to Review with ${phases.size} phases")
                    phases.forEachIndexed { idx, phase ->
                        val phaseBudget = phase.departments.sumOf { it.totalBudget }
                        android.util.Log.d("AppNavHost", "  Phase ${idx + 1}: ${phase.phaseName}, Budget: $phaseBudget, Departments: ${phase.departments.size}")
                    }

                    // Store data temporarily in ViewModel and navigate
                    viewModel.setReviewData(projectName, description, client, clientPrimaryNumber, clientSecondaryNumber, location, currency, plannedStartDate, handoverDate, maintenanceDate, phases, approver, teamMembers, userAssignments, approverAssignments)
                    navController.navigate(Screen.ReviewProject.route)
                },
                viewModel = viewModel
            )
        }
        
        composable(
            route = Screen.DraftProjects.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            DraftProjectsScreen(
                onNavigateBack = { navController.popBackStack() },
                onDraftSelected = { draft ->
                    // Navigate directly to NewProject screen with draft ID in route
                    val route = Screen.NewProjectWithDraft.createRoute(draft.id)
                    android.util.Log.d("AppNavHost", "🚀 Navigating to NewProject with draft ID: ${draft.id}")
                    android.util.Log.d("AppNavHost", "🚀 Route: $route")
                    
                    // Navigate to NewProject screen with draft ID as route parameter
                    navController.navigate(route) {
                        // Pop back to remove DraftProjects screen from back stack
                        popUpTo(Screen.DraftProjects.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        
        // Select Template Screen
        composable(
            route = Screen.SelectTemplate.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            val projectViewModel: ProjectViewModel = hiltViewModel()
            com.cubiquitous.tracura.ui.view.businesshead.SelectTemplateScreen(
                onDismiss = { navController.popBackStack() },
                onTemplateSelected = { template ->
                    // Navigate to NewProject with template ID
                    navController.navigate(Screen.NewProjectWithTemplate.createRoute(template.id)) {
                        popUpTo(Screen.SelectTemplate.route) { inclusive = true }
                    }
                },
                onCreateNew = {
                    navController.navigate(Screen.NewProject.route) {
                        popUpTo(Screen.SelectTemplate.route) { inclusive = true }
                    }
                },
                projectRepository = projectViewModel.projectRepository
            )
        }
        
        // New Project with Template
        composable(
            route = Screen.NewProjectWithTemplate.route,
            arguments = listOf(navArgument("templateId") { type = NavType.StringType }),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId") ?: ""
            val viewModel: BusinessHeadViewModel = hiltViewModel()
            
            // Find template and pre-fill NewProjectScreen
            val template = com.cubiquitous.tracura.model.ProjectTemplates.templates.find { it.id == templateId }
            
            android.util.Log.d("AppNavHost", "📥 Loading template with ID: $templateId")
            android.util.Log.d("AppNavHost", "📥 Template found: ${template != null}")
            if (template != null) {
                android.util.Log.d("AppNavHost", "📥 Template name: ${template.name}, Phases: ${template.phases.size}")
            }
            
            NewProjectScreen(
                onNavigateBack = { navController.popBackStack() },
                onProjectCreated = { navController.popBackStack() },
                onNavigateToDraftProjects = {
                    navController.navigate(Screen.DraftProjects.route)
                },
                selectedTemplate = template,
                onNavigateToReview = { projectName, description, client, clientPrimaryNumber, clientSecondaryNumber, location, currency, plannedStartDate, handoverDate, maintenanceDate, phases, approver, teamMembers, userAssignments, approverAssignments ->
                    android.util.Log.d("AppNavHost", "🚀 Navigating to Review with ${phases.size} phases (Template)")
                    phases.forEachIndexed { idx, phase ->
                        val phaseBudget = phase.departments.sumOf { it.totalBudget }
                        android.util.Log.d("AppNavHost", "  Phase ${idx + 1}: ${phase.phaseName}, Budget: $phaseBudget, Departments: ${phase.departments.size}")
                    }

                    viewModel.setReviewData(projectName, description, client, clientPrimaryNumber, clientSecondaryNumber, location, currency, plannedStartDate, handoverDate, maintenanceDate, phases, approver, teamMembers, userAssignments, approverAssignments)
                    navController.navigate(Screen.ReviewProject.route)
                },
                viewModel = viewModel
            )
        }
        
        composable(
            route = Screen.ReviewProject.route,
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) {
            // Scope to the NewProject destination so we share the SAME ViewModel instance
            // Always prefer previousBackStackEntry if it is one of the known new-project routes,
            // because getBackStackEntry(NewProject.route) can match a stale entry further back
            // in the stack (e.g. NewProject → DraftProjects → NewProjectWithDraft → ReviewProject).
            val parentEntry = remember(navController) {
                val validParentRoutes = listOf(
                    Screen.NewProject.route,
                    Screen.NewProjectWithId.route,
                    Screen.NewProjectWithDraft.route,
                    Screen.NewProjectWithTemplate.route
                )
                val prevEntry = navController.previousBackStackEntry
                if (prevEntry != null && validParentRoutes.contains(prevEntry.destination.route)) {
                    prevEntry
                } else {
                    try {
                        navController.getBackStackEntry(Screen.NewProject.route)
                    } catch (e: Exception) {
                        try {
                            navController.getBackStackEntry(Screen.NewProjectWithId.route)
                        } catch (e2: Exception) {
                            prevEntry ?: navController.currentBackStackEntry!!
                        }
                    }
                }
            }
            val viewModel: BusinessHeadViewModel = hiltViewModel(parentEntry)
            val reviewData by viewModel.reviewData.collectAsState()
            val projectIdForResubmission = viewModel.projectIdForResubmission
            
            // Ensure we have valid review data before rendering
            LaunchedEffect(Unit) {
                android.util.Log.d("AppNavHost", "📋 ReviewProject composable - Phases: ${reviewData.phases.size}")
                reviewData.phases.forEachIndexed { idx, phase ->
                    val phaseBudget = phase.departments.sumOf { it.totalBudget }
                    android.util.Log.d("AppNavHost", "  Phase ${idx + 1}: ${phase.phaseName}, Budget: $phaseBudget")
                }
            }
            
            ReviewProjectScreen(
                projectName = reviewData.projectName,
                description = reviewData.description,
                client = reviewData.client,
                clientPrimaryNumber = reviewData.clientPrimaryNumber,
                clientSecondaryNumber = reviewData.clientSecondaryNumber,
                location = reviewData.location,
                currency = reviewData.currency,
                plannedStartDate = reviewData.plannedStartDate,
                handoverDate = reviewData.handoverDate,
                maintenanceDate = reviewData.maintenanceDate,
                phases = reviewData.phases, // This will trigger recomposition when reviewData changes
                selectedApprover = reviewData.approver,
                selectedTeamMembers = reviewData.teamMembers,
                departmentUserAssignments = reviewData.departmentUserAssignments,
                departmentApproverAssignments = reviewData.departmentApproverAssignments,
                onNavigateBack = { navController.popBackStack() },
                onProjectCreated = {
                    // Clear projectIdForResubmission after successful resubmission
                    viewModel.projectIdForResubmission = null
                    navController.popBackStack()
                },
                projectIdForResubmission = projectIdForResubmission,
                viewModel = viewModel
            )
        }
        
        // Chat Screens
        composable(
            route = Screen.ChatList.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("projectName") { type = NavType.StringType }
            ),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val projectName = backStackEntry.arguments?.getString("projectName") ?: ""
            com.cubiquitous.tracura.ui.view.approver.ChatsListScreen(
                projectId = projectId,
                projectName = projectName,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToIndividualChat = { pId, chatId, participantName ->
                    navController.navigate(Screen.Chat.createRoute(pId, chatId, participantName))
                },
                onNavigateToGroupChat = { pId ->
                    // Navigate to group chat - using group_{projectId} as chatId
                    navController.navigate(Screen.Chat.createRoute(pId, "group_$pId", "Group Chat"))
                }
            )
        }
        
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("chatId") { type = NavType.StringType },
                navArgument("otherUserName") { type = NavType.StringType }
            ),
            // iOS-style slide animations
            enterTransition = { defaultSlideTransitions() },
            exitTransition = { defaultSlideExitTransitions() },
            popEnterTransition = { defaultPopEnterTransitions() },
            popExitTransition = { defaultPopExitTransitions() }
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: ""
            ChatScreen(
                projectId = projectId,
                chatId = chatId,
                otherUserName = otherUserName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
