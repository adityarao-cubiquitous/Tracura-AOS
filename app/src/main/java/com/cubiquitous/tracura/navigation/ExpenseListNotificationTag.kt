package com.cubiquitous.tracura.navigation

import com.cubiquitous.tracura.model.NotificationType
import android.util.Log

const val EXPENSE_LIST_NO_NOTIFICATION = "none"

fun normalizeExpenseListNotificationTag(fromNotification: String?): String {
    val value = fromNotification?.trim().orEmpty()
    return when {
        value.isBlank() -> ""
        value.equals(EXPENSE_LIST_NO_NOTIFICATION, ignoreCase = true) -> ""
        value.equals("EXPENSE_LIST_NO_NOTIFICATION", ignoreCase = true) -> ""
        value.equals("false", ignoreCase = true) -> ""
        else -> value
    }
}

fun expenseListRouteArgument(fromNotification: String?): String {
    return normalizeExpenseListNotificationTag(fromNotification).ifEmpty {
        EXPENSE_LIST_NO_NOTIFICATION
    }
}

fun buildExpenseListNotificationTag(
    notificationType: String,
    navigationTarget: String = "",
    notifType: NotificationType? = null,
    title: String = "",
    message: String = ""
): String {
    val typeValue = notificationType.trim()
    val typeLower = typeValue.lowercase()
    val targetValue = navigationTarget.trim()
    val targetLower = targetValue.lowercase()
    val titleLower = title.trim().lowercase()
    val messageValue = message.lowercase()
    val textValue = "$titleLower $messageValue"
    val isStructuredExpenseGeneralUpdate =
        typeLower.contains("expense_general_update") &&
            messageValue.contains("of expense in") &&
            messageValue.contains("has been updated")
    val keywordExclusions = setOf(
        "the",
        "of",
        "in",
        "has",
        "been",
        "please",
        "pls",
        "review",
        "changes",
        "change",
        "updated"
    )
    val expenseMessageRegex = Regex(
        """\bthe\s+(.+?)\s+of expense in\s+.+?\s+has been updated\b"""
    )
    val expenseChangedFieldsTag = if (isStructuredExpenseGeneralUpdate) {
        expenseMessageRegex
            .find(messageValue)
            ?.groupValues
            ?.getOrNull(1)
            ?.split(",")
            ?.mapNotNull { fieldChunk ->
                val token = fieldChunk
                    .trim()
                    .lowercase()
                    .split(Regex("[^a-z0-9]+"))
                    .filter { word -> word.isNotBlank() && word !in keywordExclusions }
                    .joinToString("") { word ->
                        word.replaceFirstChar { char -> char.uppercase() }
                    }
                token.ifBlank { null }
            }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString("")
            ?.let { changedFieldToken -> "Expense${changedFieldToken}Changed" }
    } else {
        null
    }
    val hasApproverUpdatedText =
        textValue.contains("approver was updated") ||
            textValue.contains("approver updated")
    val hasUserUpdatedText =
        textValue.contains("user was updated") ||
            textValue.contains("user updated")
    val hasTemporaryApproverUpdatedText =
        textValue.contains("temporary approver was updated") ||
            textValue.contains("temporary approver updated") ||
            textValue.contains("temporary approver")
    val isScheduleUpdateType =
        typeLower.contains("phase_schedule_update") ||
            typeLower.contains("project_schedule_update")
    val hasPhaseEndDateChangedText =
        textValue.contains("end date") &&
            textValue.contains("phase") &&
            (textValue.contains("has been changed") || textValue.contains("changed to"))
    val hasPhaseStartDateChangedText =
        textValue.contains("start date") &&
            textValue.contains("phase") &&
            (textValue.contains("has been changed") || textValue.contains("changed to"))
    val isUserExpenseStatusUpdate =
        textValue.contains("your expense") &&
            (textValue.contains("rejected") ||
                textValue.contains("approved") ||
                textValue.contains("complete"))

    Log.d("CheckingfromNotificationMesssage","Checking for phase highlighting keywords in text: '$textValue' and type: '$typeLower'")


    return when {
        // ===== Pending approval =====
        notifType == NotificationType.PENDING_APPROVAL ||
            typeLower.contains("pending_approval") ||
            targetLower.contains("pending_approval") -> "pending_approval"

        // ===== Expense chat =====
        typeLower.contains("expense_chat_staff") ||
            typeLower.contains("expense_chat_customer") ||
            targetLower.contains("expense_chat") ||
            (notifType == NotificationType.CHAT_MESSAGE &&
                (messageValue.contains("expense") || messageValue.contains("logged"))) -> "expense_chat"

        // ===== Expense submit/review/status =====
        notifType == NotificationType.EXPENSE_SUBMITTED -> "expense_submitted"
        isUserExpenseStatusUpdate ||
            notifType == NotificationType.EXPENSE_APPROVED ||
            notifType == NotificationType.EXPENSE_REJECTED -> "ExpenseStatusUpdate"

        typeLower.contains("expense_manager_review") -> "expense_manager_review"
        typeLower.contains("expense_businesshead_review") ||
            typeLower.contains("expense_business_head_review") -> "expense_business_head_review"
        typeLower.contains("expense_manager_update") -> "expense_manager_update"
        typeLower.contains("expense_businesshead_update") ||
            typeLower.contains("expense_business_head_update") -> "expense_business_head_update"
        typeLower.contains("expense_status_update") ||
            typeLower.contains("expense_approved_") ||
            typeLower.contains("expense_complete_") ||
            typeLower.contains("expense_rejected_") -> "ExpenseStatusUpdate"
        typeLower.contains("expense_general_update") ->
            expenseChangedFieldsTag ?: "ExpenseStatusUpdate"
        targetLower.contains("expense_detail") ||
            targetLower.contains("expense_review") -> "expense_review"

        // // ===== Project date change (MUST be before generic project_detail) =====
        // typeLower.contains("project_date_update") ||
        //     typeLower.contains("project_date_") ||
        //     (notifType == NotificationType.PROJECT_CHANGED &&
        //         (textValue.contains("planned date") ||
        //             textValue.contains("handover date") ||
        //             textValue.contains("maintenance date") ||
        //             textValue.contains("date has been changed") ||
        //             textValue.contains("date changed"))) -> "project_date_update"
        textValue.contains("handover date") || textValue.contains("handover")  -> "HandOverDateUpdate"
        textValue.contains("maintenance date") || textValue.contains("maintenance")  -> "MaintenanceDateUpdate"
        textValue.contains("planned date") || textValue.contains("is going to active")  -> "PlannedDateUpdate"

        // Phase End highlighting variable 
        textValue.contains("is going to end in") || textValue.contains("is ending tomorrow") || textValue.contains("ended today") -> "PhaseEndHighlighting"

         (isScheduleUpdateType && textValue.contains("end date")) || hasPhaseEndDateChangedText -> "PhaseEndDateChangeHighlighting"

        // Phase Start highlighting variable
        textValue.contains("is going to start") || textValue.contains("is starting today")  ||
            (isScheduleUpdateType && textValue.contains("start date")) || hasPhaseStartDateChangedText -> "PhaseStartHighlighting"

        // `The ${ChngedItem} of expense in ${projectName} has been updated. Please review the changes.`
        expenseChangedFieldsTag != null -> expenseChangedFieldsTag

        textValue.contains("status has changed to")  -> "StatusUpdateProject"

        textValue.contains("new phase")  -> "NewPhaseupdate"

        // ===== Phase updates =====
        typeLower.contains("phase_created") ||
            typeLower.contains("phase_deadline_reminder") -> "phase_update"

        // ===== Department updates =====
        // typeLower.contains("department_budget_update") ||
        //     typeLower.contains("department_change") ||
        //     typeLower.contains("department_") ||
        //     (notifType == NotificationType.PROJECT_CHANGED && textValue.contains("department")) -> "department_update"

        // ===== Team/assignment updates =====
        typeLower.contains("manager_added") -> "manager_added"
        typeLower.contains("manager_removed") -> "manager_removed"
        typeLower.contains("user_added") -> "user_added"
        typeLower.contains("user_removed") -> "user_removed"
        typeLower == "assigned" -> "user_added"
        typeLower == "removed" -> "user_removed"

        typeLower.contains("temp_approver_incoming") ||
            typeLower.contains("temp_approver_assigned") ||
            typeLower.contains("temp_approver_started") ||
            typeLower.contains("temp_approver_added") ||
            typeLower.contains("temp_approver_removed") ||
            typeLower.contains("temp_approver") ||
            (typeLower.contains("team_update") && hasTemporaryApproverUpdatedText) -> "temp_approver"

        hasApproverUpdatedText ||
            typeLower.contains("approver_updated") ||
            (typeLower.contains("team_update") && textValue.contains("approver")) ||
            (notifType == NotificationType.PROJECT_CHANGED && textValue.contains("approver")) -> "approver_updated"

        typeLower.contains("team_update") && hasUserUpdatedText -> "user_added"
        typeLower.contains("team_update") -> "approver_updated"

        // ===== Project status/review =====
        targetLower.contains("project_review") ||
            typeLower.contains("status_in_review") -> "project_review"
        typeLower.contains("status_general_update") ||
            typeLower.contains("project_suspension_reminder") ||
            typeLower.contains("status_") -> "status_update"

        // ===== Project assignment/create =====
        notifType == NotificationType.PROJECT_ASSIGNMENT ||
            typeLower.contains("project_assigned") -> "project_assigned"
        typeLower.contains("project_created") -> "project_created"
        targetLower.contains("project_creation") ||
            typeLower.contains("project_declined") -> "project_creation"


        // project remaining balance 
        textValue.contains("changed in") &&
            (textValue.contains("the total budget for") ||
                textValue.contains("the remaining amount for") ||
                textValue.contains("total budget and remaining amount for")) -> "RemainingBudgetUpdate"

        // // ===== Chat =====
        // notifType == NotificationType.CHAT_MESSAGE ||
        //     typeLower.contains("new_chat_message") ||
        //     typeLower.contains("customer_chat_message") ||
        //     typeLower.contains("chat_message") ||
        //     targetLower.contains("chat") -> "chat_message"

        // Generic project detail should NOT force project_assigned
        targetLower.contains("project_detail") -> "true"

        typeValue.isNotBlank() || targetValue.isNotBlank() || notifType != null -> "true"
        else -> EXPENSE_LIST_NO_NOTIFICATION
    }
}
