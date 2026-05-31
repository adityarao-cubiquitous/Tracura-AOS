package com.cubiquitous.tracura.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    // Auth Screens
    object Login : Screen("login")
    object AdminSignUp : Screen("admin_signup")
    object OtpVerification : Screen("otp_verification")
    object CustomerSelection : Screen("customer_selection")
    object AccessRestricted : Screen("access_restricted")
    object Splash : Screen("splash")
    
    // Common Screens
    object ProjectSelection : Screen("project_selection")
    object NotificationList : Screen("notification_list")
    object ProjectNotifications : Screen("project_notifications/{projectId}") {
        fun createRoute(projectId: String) = "project_notifications/$projectId"
    }
    
    // User Flow Screens
    object UserDashboard : Screen("user_dashboard")
    object ExpenseList : Screen("expense_list/{projectId}/{fromNotification}/{notificationMessage}") {
        fun createRoute(
            projectId: String,
            fromNotification: String = EXPENSE_LIST_NO_NOTIFICATION,
            notificationMessage: String = ""
        ) = "expense_list/$projectId/${Uri.encode(fromNotification)}/${Uri.encode(notificationMessage)}"
    }
    object AddExpense : Screen("add_expense/{projectId}/{expenseId}") {
        fun createRoute(projectId: String, expenseId: String? = null) = 
            if (expenseId != null && expenseId.isNotEmpty()) {
                "add_expense/$projectId/$expenseId"
            } else {
                "add_expense/$projectId/0" // Use "0" as placeholder when no expenseId
            }
    }
    object ExpenseChat : Screen("expense_chat/{projectId}") {
        fun createRoute(projectId: String) = "expense_chat/$projectId"
    }
    object TrackSubmissions : Screen("track_submissions/{projectId}") {
        fun createRoute(projectId: String) = "track_submissions/$projectId"
    }
    object ExpenseDetail : Screen("expense_detail/{expenseId}") {
        fun createRoute(expenseId: String) = "expense_detail/$expenseId"
    }
    object UserExpenseChat : Screen("user_expense_chat/{expenseId}") {
        fun createRoute(expenseId: String) = "user_expense_chat/$expenseId"
    }
    object AllExpenses : Screen("all_expenses/{projectId}") {
        fun createRoute(projectId: String) = "all_expenses/$projectId"
    }
    object RequestStatus : Screen("request_status/{projectId}/{phaseId}") {
        fun createRoute(projectId: String, phaseId: String) = "request_status/$projectId/$phaseId"
    }
    
    // Approver Flow
    object PendingApprovals : Screen("pending_approvals")
    object ProjectPendingApprovals : Screen("project_pending_approvals/{projectId}") {
        fun createRoute(projectId: String) = "project_pending_approvals/$projectId"
    }
     //object ExpenseList : Screen("expense_list/{projectId}/{fromNotification}") {
       // fun createRoute(projectId: String, fromNotification: String = EXPENSE_LIST_NO_NOTIFICATION) = "expense_list/$projectId/$fromNotification"
    //

    object ReviewExpense : Screen("review_expense/{expenseId}/{fromNotification}") {
        fun createRoute(expenseId: String, fromNotification: String = EXPENSE_LIST_NO_NOTIFICATION) =
            "review_expense/$expenseId/${Uri.encode(fromNotification)}"
    }
    object ApproverExpenseChat : Screen("approver_expense_chat/{expenseId}") {
        fun createRoute(expenseId: String) = "approver_expense_chat/$expenseId"
    }
    
    // New Approver Project Flow
    object ApproverProjectSelection : Screen("approver_project_selection")
    object ApproverNotificationScreen : Screen("approver_notification_screen")
    object ApproverProjectDashboard : Screen("approver_project_dashboard/{projectId}/{fromNotification}/{NotificationMessage}") {
        fun createRoute(
            projectId: String,
            fromNotification: String = EXPENSE_LIST_NO_NOTIFICATION,
            NotificationMessage: String = ""
        ) = "approver_project_dashboard/$projectId/${Uri.encode(fromNotification)}/${Uri.encode(NotificationMessage)}"
    }
    object ApproverProjectReview : Screen("approver_project_review/{projectId}") {
        fun createRoute(projectId: String) = "approver_project_review/$projectId"
    }
    object ApproverReports : Screen("approver_reports/{projectId}") {
        fun createRoute(projectId: String) = "approver_reports/$projectId"
    }
    object ApproverAnalytics : Screen("approver_analytics/{projectId}") {
        fun createRoute(projectId: String) = "approver_analytics/$projectId"
    }
    object CategoryDetail : Screen("category_detail/{projectId}/{categoryName}") {
        fun createRoute(projectId: String, categoryName: String) = "category_detail/$projectId/$categoryName"
    }
    object DepartmentDetail : Screen("department_detail/{projectId}/{departmentName}") {
        fun createRoute(projectId: String, departmentName: String) = "department_detail/$projectId/$departmentName"
    }
    object OverallReports : Screen("overall_reports")
    
    // Admin Flow Screens
    object AdminDashboard : Screen("admin_dashboard")
    object ManageUsers : Screen("manage_users")
    object ManageProjects : Screen("manage_projects")
    object Reports : Screen("reports")
    
    // Business Head Flow Screens
    object BusinessHeadProjectSelection : Screen("business_head_project_selection")
    object CreateUser : Screen("create_user")
    object ViewAllUsers : Screen("view_all_users")
    object DepartmentUserManagement : Screen("department_user_management")
    object RoleManagement : Screen("role_management")
    object NewProject : Screen("new_project")
    object NewProjectWithDraft : Screen("new_project/draft/{draftId}") {
        fun createRoute(draftId: String) = "new_project/draft/$draftId"
    }
    object NewProjectWithId : Screen("new_project/{projectId}") {
        fun createRoute(projectId: String) = "new_project/$projectId"
    }
    object NewProjectWithTemplate : Screen("new_project/template/{templateId}") {
        fun createRoute(templateId: String) = "new_project/template/$templateId"
    }
    object SelectTemplate : Screen("select_template")
    object DraftProjects : Screen("draft_projects")
    object ReviewProject : Screen("review_project")
    object EditProject : Screen("edit_project/{projectId}") {
        fun createRoute(projectId: String) = "edit_project/$projectId"
    }
    
    // Business Head Approval Flow (same functionality as Approver)
    object BusinessHeadProjectsTab : Screen("Navigate to the project list view")
    object BusinessHeadDashboard : Screen("business_head_dashboard")
    object BusinessHeadPendingApprovals : Screen("business_head_pending_approvals")
    object BusinessHeadProjectPendingApprovals : Screen("business_head_project_pending_approvals/{projectId}") {
        fun createRoute(projectId: String) = "business_head_project_pending_approvals/$projectId"
    }
    object BusinessHeadReviewExpense : Screen("business_head_review_expense/{expenseId}/{fromNotification}") {
        fun createRoute(expenseId: String, fromNotification: String = EXPENSE_LIST_NO_NOTIFICATION) =
            "business_head_review_expense/$expenseId/${Uri.encode(fromNotification)}"
    }

    //object ExpenseList : Screen("expense_list/{projectId}/{fromNotification}") {
       // fun createRoute(projectId: String, fromNotification: String = EXPENSE_LIST_NO_NOTIFICATION) = "expense_list/$projectId/$fromNotification"
    //
    object BusinessHeadProjectDashboard : Screen("business_head_project_dashboard/{projectId}/{fromNotification}/{NotificationMessage}") {
        fun createRoute(
            projectId: String,
            fromNotification: String = EXPENSE_LIST_NO_NOTIFICATION,
            NotificationMessage: String = ""
        ) = "business_head_project_dashboard/$projectId/${Uri.encode(fromNotification)}/${Uri.encode(NotificationMessage)}"
    }
    object BusinessHeadReports : Screen("business_head_reports/{projectId}") {
        fun createRoute(projectId: String) = "business_head_reports/$projectId"
    }
    object BusinessHeadAnalytics : Screen("business_head_analytics/{projectId}") {
        fun createRoute(projectId: String) = "business_head_analytics/$projectId"
    }
    object BusinessHeadCategoryDetail : Screen("business_head_category_detail/{projectId}/{categoryName}") {
        fun createRoute(projectId: String, categoryName: String) = "business_head_category_detail/$projectId/$categoryName"
    }
    object BusinessHeadOverallReports : Screen("business_head_overall_reports")
    object UserProfile : Screen("user_profile")
    object Delegation : Screen("delegation/{projectId}") {
        fun createRoute(projectId: String) = "delegation/$projectId"
    }
    object PendingByCredit : Screen("pending_by_credit") {
        fun createRoute(projectId: String? = null) = if (projectId != null) "pending_by_credit/$projectId" else "pending_by_credit/all"
    }
    
    // Chat Screens
    object ChatList : Screen("chat_list/{projectId}/{projectName}") {
        fun createRoute(projectId: String, projectName: String) =
            "chat_list/${Uri.encode(projectId)}/${Uri.encode(projectName)}"
    }
    object Chat : Screen("chat/{projectId}/{chatId}/{otherUserName}") {
        fun createRoute(projectId: String, chatId: String, otherUserName: String) =
            "chat/${Uri.encode(projectId)}/${Uri.encode(chatId)}/${Uri.encode(otherUserName)}"
    }
}
