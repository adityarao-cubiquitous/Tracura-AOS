package com.cubiquitous.tracura.ui.view.approver

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import com.google.firebase.Timestamp
import com.cubiquitous.tracura.navigation.normalizeExpenseListNotificationTag
import com.cubiquitous.tracura.navigation.buildExpenseListNotificationTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import com.cubiquitous.tracura.model.DepartmentTemporaryApproverEntry
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.shape.CircleShape
import com.cubiquitous.tracura.ui.common.ProjectNotificationPopup
import com.cubiquitous.tracura.ui.common.AddDepartmentModalSheet
import com.cubiquitous.tracura.ui.common.DepartmentCell
import com.cubiquitous.tracura.model.DepartmentCellData
import com.cubiquitous.tracura.utils.transformDepartmentForCell
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowRight
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.viewmodel.ApproverProjectViewModel
import com.cubiquitous.tracura.viewmodel.NotificationViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.viewmodel.TemporaryApproverViewModel
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.model.DepartmentBudgetBreakdown
import com.cubiquitous.tracura.model.ProjectBudgetSummary
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.model.TemporaryApprover
import com.cubiquitous.tracura.model.ContractorMode
import com.cubiquitous.tracura.model.LineItem
import com.cubiquitous.tracura.model.DepartmentDraft
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.remember
import kotlin.math.max
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.cubiquitous.tracura.model.PhaseBreakdown
import com.cubiquitous.tracura.repository.ExpenseRepository
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.platform.LocalContext
import java.util.Date
import com.cubiquitous.tracura.model.ExpenseStatus
import com.cubiquitous.tracura.ui.view.businesshead.ViewAllTeamMembersSheet
import com.cubiquitous.tracura.ui.view.businesshead.AddTeamMemberSheet
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cubiquitous.tracura.ui.view.approver.components.UserExpensesBottomSheet
import com.cubiquitous.tracura.ui.view.approver.components.AddPhaseBottomSheet
import com.cubiquitous.tracura.ui.view.approver.components.PhaseProofUploadBottomSheet
import com.cubiquitous.tracura.ui.view.approver.components.PhaseProofViewerSheet
import com.cubiquitous.tracura.viewmodel.PhaseCompletionViewModel
import androidx.compose.material.icons.filled.AttachFile
import com.cubiquitous.tracura.model.PaymentMode
import androidx.compose.material.icons.filled.Chat
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.cubiquitous.tracura.model.ProjectStatus
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Tag


private inline fun debugLog(tag: String, message: () -> String) {
    if (Log.isLoggable(tag, Log.DEBUG)) {
        Log.d(tag, message())
    }
}

// Data class for Phase Budget Distribution
data class PhaseBudgetData(
    val phaseId: String,
    val phaseName: String,
    val departmentName: String,
    val budgetAllocated: Double,
    val spent: Double,
    val remaining: Double,
    val utilizationPercentage: Double
)

// Responsive sizing helpers for different screen sizes
@Composable

internal fun responsiveFontSize(
    small: androidx.compose.ui.unit.TextUnit = 12.sp,
    medium: androidx.compose.ui.unit.TextUnit = 14.sp,
    large: androidx.compose.ui.unit.TextUnit = 16.sp,
    xlarge: androidx.compose.ui.unit.TextUnit = 18.sp
): androidx.compose.ui.unit.TextUnit {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    return remember(screenWidthDp) {
        when {
            screenWidthDp < 360 -> small
            screenWidthDp < 480 -> medium
            screenWidthDp < 600 -> large
            else -> xlarge
        }
    }
}

@Composable
internal fun responsiveIconSize(
    small: androidx.compose.ui.unit.Dp = 16.dp,
    medium: androidx.compose.ui.unit.Dp = 20.dp,
    large: androidx.compose.ui.unit.Dp = 24.dp
): androidx.compose.ui.unit.Dp {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    return remember(screenWidthDp) {
        when {
            screenWidthDp < 360 -> small
            screenWidthDp < 480 -> medium
            else -> large
        }
    }
}

@Composable
internal fun responsivePadding(
    small: androidx.compose.ui.unit.Dp = 8.dp,
    medium: androidx.compose.ui.unit.Dp = 12.dp,
    large: androidx.compose.ui.unit.Dp = 16.dp
): androidx.compose.ui.unit.Dp {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    return remember(screenWidthDp) {
        when {
            screenWidthDp < 360 -> small
            screenWidthDp < 480 -> medium
            else -> large
        }
    }
}

@Composable
internal fun responsiveSpacing(
    small: androidx.compose.ui.unit.Dp = 4.dp,
    medium: androidx.compose.ui.unit.Dp = 8.dp,
    large: androidx.compose.ui.unit.Dp = 12.dp
): androidx.compose.ui.unit.Dp {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    return remember(screenWidthDp) {
        when {
            screenWidthDp < 360 -> small
            screenWidthDp < 480 -> medium
            else -> large
        }
    }
}

private data class IosDashboardPalette(
    val tier1Background: Color,
    val tier2Surface: Color,
    val tier3Field: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val hairline: Color
)

private val PositiveAmountColor = Color(0xFF2E7D32)
private val NegativeAmountColor = Color(0xFFD32F2F)
private val PhaseTotalTextColor = Color(0xFFE91E63)
private val PhaseTotalBackgroundColor = Color.Transparent
private val ApprovedAmountColor = Color(0xFF1E88E5)

private fun matchesAssignedDepartment(
    departmentName: String,
    assignedDepartments: Set<String>
): Boolean {
    if (assignedDepartments.isEmpty()) return false
    val normalized = departmentName.trim()
    return assignedDepartments.any { assigned ->
        val assignedNormalized = assigned.trim()
        normalized.equals(assignedNormalized, ignoreCase = true) ||
            normalized.endsWith("_$assignedNormalized", ignoreCase = true) ||
            normalized.substringAfterLast("_", normalized).equals(assignedNormalized, ignoreCase = true)
    }
}

private fun normalizePhoneForCompare(phone: String?): String {
    if (phone.isNullOrBlank()) return ""
    val digits = phone.filter { it.isDigit() }
    return if (digits.length > 10) digits.takeLast(10) else digits
}

private fun Date.atStartOfDay(): Date {
    val cal = java.util.Calendar.getInstance().apply {
        time = this@atStartOfDay
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    return cal.time
}

private fun isActiveTempApproverForDate(
    entry: DepartmentTemporaryApproverEntry,
    normalizedPhone: String,
    today: Date
): Boolean {
    if (normalizePhoneForCompare(entry.phone) != normalizePhoneForCompare(normalizedPhone)) return false
    if (!entry.isAccepted || !entry.isActive) return false
    val todayDay = today.atStartOfDay()
    val startDate = entry.startDate?.atStartOfDay() ?: return false
    val hasStarted = !todayDay.before(startDate)
    val isBeforeOrOnEndDate = entry.endDate?.atStartOfDay()?.let { !todayDay.after(it) } ?: true
    return hasStarted && isBeforeOrOnEndDate
}

@Composable
private fun rememberIosDashboardPalette(accentColor: Color = MaterialTheme.colorScheme.primary): IosDashboardPalette {
    val isDark = isSystemInDarkTheme()
    return remember(isDark, accentColor) {
        IosDashboardPalette(
            tier1Background = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7),
            tier2Surface = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
            tier3Field = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF),
            textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
            textSecondary = if (isDark) Color(0xB3EBEBF5) else Color(0x993C3C43),
            accent = accentColor,
            onAccent = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF),
            hairline = if (isDark) Color(0xFF38383A) else Color(0x1F000000)
        )
    }
}

// Reusable composable for truncated text with popup
@Composable
internal fun TruncatedTextWithPopup(
    text: String,
    maxLength: Int = 10,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    var showPopup by remember { mutableStateOf(false) }
    val shouldTruncate = text.length > maxLength
    val displayText = if (shouldTruncate) text.take(maxLength) else text
    val responsivePadding = responsivePadding()
    val iosPalette = rememberIosDashboardPalette()
    val resolvedTextColor = if (color == Color.Unspecified) iosPalette.textPrimary else color
    
    if (textAlign == TextAlign.Center) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = displayText,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = resolvedTextColor,
                    textAlign = textAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (shouldTruncate) {
                    Text(
                        text = "...",
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = resolvedTextColor,
                        modifier = Modifier
                            .clickable { showPopup = true }
                            .padding(start = responsivePadding / 4)
                    )
                }
            }
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = displayText,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = resolvedTextColor,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (shouldTruncate) {
                Text(
                    text = "...",
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = resolvedTextColor,
                    modifier = Modifier
                        .clickable { showPopup = true }
                        .padding(start = responsivePadding / 4)
                )
            }
        }
    }
    
    // Popup dialog to show full text
    if (showPopup) {
        Dialog(
            onDismissRequest = { showPopup = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(responsivePadding),
                colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(responsivePadding + 4.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = text,
                        fontSize = responsiveFontSize(14.sp, 15.sp, 16.sp, 17.sp),
                        color = iosPalette.textPrimary,
                        modifier = Modifier.padding(bottom = responsivePadding)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showPopup = false }) {
                            Text(
                                text = "Close",
                                color = iosPalette.accent,
                                fontSize = responsiveFontSize(14.sp, 15.sp, 16.sp, 17.sp)
                            )
                        }
                    }

                }
            }
        }
    }
}

@Composable
private fun AutoSizeText(
    text: String,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
    stepGranularity: TextUnit = 1.sp
) {
    var textSize by remember(text, maxFontSize) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text, maxFontSize) { mutableStateOf(false) }

    Text(
        text = text,
        fontSize = textSize,
        fontWeight = fontWeight,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier.drawWithContent {
            if (readyToDraw) {
                drawContent()
            }
        },
        onTextLayout = { result ->
            if (result.hasVisualOverflow && textSize.value > minFontSize.value) {
                val nextSize = (textSize.value - stepGranularity.value).coerceAtLeast(minFontSize.value)
                if (nextSize != textSize.value) {
                    textSize = nextSize.sp
                } else {
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ApproverProjectDashboardScreen(
    projectId: String,
    NotificationMessage: String = "",
    fromNotification: String = "",
    preloadedProject: com.cubiquitous.tracura.model.Project? = null,
    onNavigateBack: () -> Unit,
    onNavigateToPendingApprovals: (String) -> Unit,
    onNavigateToAddExpense: () -> Unit = {},
    onNavigateToEditExpense: (String) -> Unit = {},
    onNavigateToAllExpenses: () -> Unit = {},
    onNavigateToReports: (String) -> Unit = {},
    onNavigateToAnalytics: (String) -> Unit = {},
    onNavigateToPendingByCredit: (String?) -> Unit = { _ -> },
    onNavigateToDepartmentDetail: (String, String, Double?, Double?, Double?) -> Unit = { _, _, _, _, _ -> },
    onNavigateToProjectNotifications: (String) -> Unit = {},
    onNavigateToDelegation: () -> Unit = {},
    onNavigateToChat: (String, String) -> Unit = { _, _ -> },
    onNavigateToChatDetail: (String, String, String) -> Unit = { _, _, _ -> }, // projectId, chatId, otherUserName
    onNavigateToExpenseReview: (String) -> Unit = { _ -> }, // expenseId
    onNavigateToExpenseDetail: (String) -> Unit = { _ -> }, // expenseId
    onNavigateToExpenseChat: (String) -> Unit = { _ -> }, // expenseId
    onNavigateToProjectReview: (String) -> Unit = { _ -> }, // projectId

    onNavigateToEditProject: (String) -> Unit = {},
    approverProjectViewModel: ApproverProjectViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    temporaryApproverViewModel: TemporaryApproverViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val projectBudgetSummary by approverProjectViewModel.projectBudgetSummary.collectAsState()
    val isLoading by approverProjectViewModel.isLoading.collectAsState()
    val error by approverProjectViewModel.error.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val selectedProject by approverProjectViewModel.selectedProject.collectAsState()
    val userRole = authState.user?.role

    debugLog("projectTempApprovercheck") { "userRole $userRole , " }
    // Notification identity: phone-first (same as BusinessHeadProjectsTab), fallback to UID
    val notificationPrimaryUserId = remember(authState.user?.phone, authState.user?.uid) {
        when {
            !authState.user?.phone.isNullOrBlank() -> authState.user?.phone!!.trim()
            !authState.user?.uid.isNullOrBlank() -> authState.user?.uid!!.trim()
            else -> ""
        }
    }
    val notificationAlternateUserIds = remember(
        authState.user?.phone,
        authState.user?.uid,
        notificationPrimaryUserId
    ) {
        buildList {
            val phone = authState.user?.phone?.trim().orEmpty()
            val uid = authState.user?.uid?.trim().orEmpty()
            if (phone.isNotEmpty() && phone != notificationPrimaryUserId) add(phone)
            if (uid.isNotEmpty() && uid != notificationPrimaryUserId) add(uid)
        }.distinct()
    }
    
    // State for DepartmentDetailScreen bottom sheet
    var showDepartmentDetailSheet by remember { mutableStateOf(false) }
    var departmentDetailProjectId by remember { mutableStateOf("") }
    var departmentDetailName by remember { mutableStateOf("") }
    var departmentDetailBudget by remember { mutableStateOf<Double?>(null) }
    var departmentDetailSpent by remember { mutableStateOf<Double?>(null) }
    var departmentDetailRemaining by remember { mutableStateOf<Double?>(null) }
    
    // Wrapper for onNavigateToDepartmentDetail that shows bottom sheet instead
    val onNavigateToDepartmentDetailWrapper: (String, String, Double?, Double?, Double?) -> Unit = { deptProjectId, deptName, budget, spent, remaining ->
        departmentDetailProjectId = deptProjectId
        departmentDetailName = deptName
        departmentDetailBudget = budget
        departmentDetailSpent = spent
        departmentDetailRemaining = remaining
        showDepartmentDetailSheet = true
    }
    
    // CRITICAL: Capture delegated property value into local variable for smart casting
    // This is required because Kotlin cannot smart cast delegated properties
    val selectedProjectValue = selectedProject
    
    // Use preloaded project for instant first paint, but prefer fresh ViewModel project once loaded.
    // Priority: selectedProject (from ViewModel) > preloadedProject > null
    val currentProject = remember(preloadedProject, selectedProjectValue, projectId) {
        when {
            selectedProjectValue?.id == projectId -> selectedProjectValue
            preloadedProject?.id == projectId -> preloadedProject
            else -> null
        }
    }
    debugLog("projectTempApprovercheck") {
        "projectIdArg=$projectId currentProjectId=${currentProject?.id} summaryProjectId=${projectBudgetSummary.project?.id}"
    }

    val normalizedApproverPhone = remember(authState.user?.phone) {
        normalizePhoneForCompare(authState.user?.phone)
    }

    val effectiveProject = remember(currentProject, projectBudgetSummary.project) {
        currentProject ?: projectBudgetSummary.project
    }

    val hasPermanentApproverAssignment = remember(effectiveProject, normalizedApproverPhone) {
        val project = effectiveProject
        if (project == null || normalizedApproverPhone.isNullOrBlank()) {
            false
        } else {
            project.departmentApproverAssignments.values.any { assignedPhone ->
                normalizePhoneForCompare(assignedPhone) == normalizedApproverPhone
            }
        }
    }

    val hasActiveTempApproverAssignment = remember(effectiveProject, normalizedApproverPhone) {
        val project = effectiveProject
        if (project == null || normalizedApproverPhone.isNullOrBlank()) {
            false
        } else {
            val today = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time

            project.departmentTemporaryApprover.any { entry ->
                isActiveTempApproverForDate(entry, normalizedApproverPhone, today)
            }
        }
    }

    val futureTempApproverWindow = remember(effectiveProject, normalizedApproverPhone) {
        val project = effectiveProject
        if (project == null || normalizedApproverPhone.isNullOrBlank()) {
            null
        } else {
            val today = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time

            project.departmentTemporaryApprover
                .asSequence()
                .filter { entry ->
                    entry.phone.replace("+91", "").trim() == normalizedApproverPhone &&
                        entry.isAccepted &&
                        entry.isActive &&
                        entry.startDate != null &&
                        entry.endDate != null
                }
                .mapNotNull { entry ->
                    val startDate = entry.startDate ?: return@mapNotNull null
                    val endDate = entry.endDate ?: return@mapNotNull null

                    val startAtMidnight = java.util.Calendar.getInstance().apply {
                        time = startDate
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.time
                    val endAtMidnight = java.util.Calendar.getInstance().apply {
                        time = endDate
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.time

                    val isFutureWindow = today.before(startAtMidnight)
                    if (isFutureWindow) startAtMidnight to endAtMidnight else null
                }
                .sortedBy { it.first.time }
                .firstOrNull()
        }
    }

    val hasAnyCurrentApproverAssignment = hasPermanentApproverAssignment || hasActiveTempApproverAssignment
    val canActAsApproverForProject =
        authState.user?.role == UserRole.APPROVER || hasAnyCurrentApproverAssignment

    val isTempApproverFutureRestricted =
        canActAsApproverForProject &&
            !hasAnyCurrentApproverAssignment &&
            futureTempApproverWindow != null

    val tempApproverFutureMessage = remember(futureTempApproverWindow) {
        val window = futureTempApproverWindow ?: return@remember ""
        val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        "This project starts for you from ${formatter.format(window.first)} to ${formatter.format(window.second)}"
    }
    
    // Get instant budget summary from ViewModel (calculated on background thread)
    val instantBudgetSummary by approverProjectViewModel.instantBudgetSummary.collectAsState()
    val maintenanceStatus by approverProjectViewModel.maintenanceStatus.collectAsState()
    val phaseBreakdowns by approverProjectViewModel.phaseBreakdowns.collectAsState()
    val ongoingPhaseBreakdowns by approverProjectViewModel.ongoingPhaseBreakdowns.collectAsState()
    val phaseBudgetMap by approverProjectViewModel.phaseBudgetMap.collectAsState()
    val phaseDepartmentSpentMap by approverProjectViewModel.phaseDepartmentSpentMap.collectAsState()
    val phaseAnonymousExpensesMap by approverProjectViewModel.phaseAnonymousExpensesMap.collectAsState()
    val phasesWithDepartments by approverProjectViewModel.phasesWithDepartments.collectAsState()
    val projectExpenses by approverProjectViewModel.projectExpenses.collectAsState()
    val allPhases by approverProjectViewModel.allPhases.collectAsState()
    val userNameMap by approverProjectViewModel.userNameMap.collectAsState()
    val businessHeadPhone by approverProjectViewModel.businessHeadPhone.collectAsState()

    //declaring the highlight color for mutbale string , like when ever change string will run this 

    var activeHighlightTag by remember { mutableStateOf("") }

    val rowHighlightColor by animateColorAsState(
        targetValue = if (activeHighlightTag.isNotEmpty())
            Color(0xFFAF52DE).copy(alpha = 0.2f)
        else
            Color.Transparent,
        animationSpec = tween(1000),
        label = "RowHighlightAnimation"
    )

    // For APPROVER: show only assigned departments' budget sum and remaining (budget − (APPROVED + COMPLETE) expenses)
    val approverDisplaySummary: ProjectBudgetSummary? = remember(
        canActAsApproverForProject,
        authState.user?.phone,
        effectiveProject,
        phasesWithDepartments
    ) {
        val userPhone = normalizePhoneForCompare(authState.user?.phone)
        val project = effectiveProject
        if (!canActAsApproverForProject || userPhone.isBlank() || project == null) return@remember null

        // Show 0 until Firebase has loaded the departments subcollection
        if (phasesWithDepartments.isEmpty()) {
            return@remember ProjectBudgetSummary(
                project = project,
                totalBudget = 0.0,
                totalSpent = 0.0,
                totalRemaining = 0.0,
                spentPercentage = 0.0,
                categoryBreakdown = emptyList(),
                departmentBreakdown = emptyList(),
                recentExpenses = emptyList(),
                pendingApprovalsCount = 0,
                approvedExpensesCount = 0
            )
        }

        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.time
        val permanentDeptNames = project.departmentApproverAssignments
            .filter { (_, phone) -> normalizePhoneForCompare(phone) == userPhone }
            .keys.toSet()
        val tempDeptNames = project.departmentTemporaryApprover
            .filter { entry ->
                isActiveTempApproverForDate(entry, userPhone, today)
            }
            .map { it.departmentName }.toSet()
        val assignedDeptNames = permanentDeptNames + tempDeptNames
        if (assignedDeptNames.isEmpty()) return@remember null

        fun departmentMatchesAssigned(deptName: String): Boolean =
            assignedDeptNames.any { assigned ->
                deptName.equals(assigned, ignoreCase = true) ||
                    deptName.trim().endsWith("_$assigned", ignoreCase = true) ||
                    deptName.trim().substringAfterLast("_").equals(assigned, ignoreCase = true)
            }

        val assignedPhaseDepartments = phasesWithDepartments.mapNotNull { phaseWithDepts ->
            val matching = phaseWithDepts.departments.filter { departmentMatchesAssigned(it.name) }
            matching.firstOrNull()
        }

        // Use totalBudget and remainingAmount directly from departments subcollection
        val assignedBudget = assignedPhaseDepartments.sumOf { it.totalBudget }
        val assignedRemaining = assignedPhaseDepartments.sumOf { it.remainingAmount }
        val totalSpentAmount = assignedBudget - assignedRemaining

        ProjectBudgetSummary(
            project = project,
            totalBudget = assignedBudget,
            totalSpent = totalSpentAmount,
            totalRemaining = assignedRemaining,
            spentPercentage = if (assignedBudget > 0) (totalSpentAmount / assignedBudget * 100) else 0.0,
            categoryBreakdown = emptyList(),
            departmentBreakdown = emptyList(),
            recentExpenses = emptyList(),
            pendingApprovalsCount = 0,
            approvedExpensesCount = 0
        )
    }

    val phaseBudgetData: List<PhaseBudgetData> = remember(
        canActAsApproverForProject,
        authState.user?.phone,
        effectiveProject,
        phasesWithDepartments,
        allPhases
    ) {
        val userPhone = normalizePhoneForCompare(authState.user?.phone)
        val project = effectiveProject
        
        if (!canActAsApproverForProject || userPhone.isBlank() || project == null) {
            debugLog("projectTempApprovercheck") {
                "phaseBudgetData skipped: canAct=$canActAsApproverForProject userPhone=$userPhone projectNull=${project == null}"
            }
            return@remember emptyList()
        }
        
        // Get assigned department names for this approver (permanent + valid temporary)
        val today2 = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.time
        val permanentDeptNames2 = project.departmentApproverAssignments
            .filter { (_, phone) -> normalizePhoneForCompare(phone) == userPhone }
            .keys.toSet()
        val tempDeptNames2 = project.departmentTemporaryApprover
            .filter { entry ->
                isActiveTempApproverForDate(entry, userPhone, today2)
            }
            .map { it.departmentName }.toSet()
        val assignedDeptNames = permanentDeptNames2 + tempDeptNames2
        
        if (assignedDeptNames.isEmpty()) {
            debugLog("projectTempApprovercheck") {
                "assigned departments empty: userPhone=$userPhone permanent=$permanentDeptNames2 temp=$tempDeptNames2 projectId=${project.id} tempRaw=${project.departmentTemporaryApprover.map { "${it.phone}|${it.departmentName}|${it.isAccepted}|${it.isActive}|${it.startDate}|${it.endDate}" }}"
            }
            return@remember emptyList()
        }

        debugLog("projectTempApprovercheck") { "permanent depts: $permanentDeptNames2, temp depts: $tempDeptNames2, all assigned: $assignedDeptNames" }
        
        // Helper function to check if a department matches assigned departments
        fun departmentMatchesAssigned(deptName: String): Boolean =
            assignedDeptNames.any { assigned ->
                deptName.equals(assigned, ignoreCase = true) ||
                    deptName.trim().endsWith("_$assigned", ignoreCase = true) ||
                    deptName.trim().substringAfterLast("_").equals(assigned, ignoreCase = true)
            }
        
        // Calculate budget data for each phase using values from departments subcollection
        allPhases.mapNotNull { phase ->
            // Get departments for this phase that match assigned departments
            val phaseDepartments = phasesWithDepartments
                .firstOrNull { it.phase.id == phase.id }
                ?.departments
                ?.filter { departmentMatchesAssigned(it.name) }
                ?: emptyList()

            if (phaseDepartments.isEmpty()) {
                return@mapNotNull null
            }

            // Use totalBudget and remainingAmount directly from the departments subcollection
            // Approver has one unique department per phase
            val phaseDepartment = phaseDepartments.first()
            val budgetAllocated = phaseDepartment.totalBudget
            val remaining = phaseDepartment.remainingAmount
            // spent is derived: totalBudget - remainingAmount (not from expenses)
            val spent = budgetAllocated - remaining
            val utilizationPercentage = if (budgetAllocated > 0) {
                (spent / budgetAllocated) * 100.0
            } else {
                0.0
            }

            PhaseBudgetData(
                phaseId = phase.id,
                phaseName = phase.phaseName,
                departmentName = phaseDepartment.name,
                budgetAllocated = budgetAllocated,
                spent = spent,
                remaining = remaining,
                utilizationPercentage = utilizationPercentage
            )
        }
    }


    // State for temporary approver user data
    var temporaryApproverUser by remember { mutableStateOf<User?>(null) }
    var isLoadingTemporaryApprover by remember { mutableStateOf(false) }
    
    // State for temporary approver data
    val temporaryApprovers by temporaryApproverViewModel.temporaryApprovers.collectAsState()
    val currentTemporaryApprover = temporaryApprovers.firstOrNull()
    
    // State for phase requests modal
    var showPhaseRequestsModal by remember { mutableStateOf(false) }
    
    // State for notification popup
    var showNotificationPopup by remember { mutableStateOf(false) }
    var notificationHighlightTag by remember { mutableStateOf("") }
    var notificationHighlightKey by remember { mutableStateOf(0) }
    
    // Project-specific notifications (filtered in ViewModel on background thread)
    val projectNotifications by notificationViewModel.projectNotifications.collectAsState()
    val projectNotificationBadge by notificationViewModel.projectNotificationBadge.collectAsState()

    val dashboardHighlightTag = remember(fromNotification, notificationHighlightTag) {
        if (notificationHighlightTag.isNotBlank()) {
            notificationHighlightTag
        } else {
            fromNotification
        }
    }
    
    // Set project ID for notification filtering
    LaunchedEffect(projectId) {
        notificationViewModel.setProjectId(projectId)
    }

    // Keep notifications synced while dashboard is visible (same behavior as BusinessHeadProjectsTab)
    LaunchedEffect(notificationPrimaryUserId, notificationAlternateUserIds, projectId) {
        if (notificationPrimaryUserId.isNotEmpty()) {
            while (true) {
                delay(10_000L)
                notificationViewModel.refreshFromLocalStorage(
                    notificationPrimaryUserId,
                    notificationAlternateUserIds
                )
            }
        }
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val phaseDistributionBringIntoViewRequester = remember { BringIntoViewRequester() }
    val departmentDistributionBringIntoViewRequester = remember { BringIntoViewRequester() }
    val specificPhaseHighlightBringIntoViewRequester = remember { BringIntoViewRequester() }
    // Hoisted scroll state for center-scroll support
    val mainScrollState = rememberScrollState()
    // Y-position of the highlighted phase item (set via onGloballyPositioned)
    var highlightedPhaseYOffset by remember { mutableStateOf(0) }
    var highlightedPhaseHeight by remember { mutableStateOf(0) }
    // Whether to fall back to the "All Phases" sheet (phase not found in ongoing list)
    var showAllPhasesFallback by remember { mutableStateOf(false) }
    var highlightFallbackPhaseId by remember { mutableStateOf("") }

    // Pull-to-refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshErrorMessage by remember { mutableStateOf<String?>(null) }
    
    // CRITICAL: Preload project data IMMEDIATELY if available (from BusinessHeadProjectsTab)
    // This sets the project in ViewModel instantly so UI can render immediately
    LaunchedEffect(preloadedProject?.id) {
        preloadedProject?.let { project ->
            if (project.id == projectId) {
                approverProjectViewModel.preloadProjectData(project)
                // Create instant budget summary from preloaded project
                approverProjectViewModel.createInstantBudgetSummary(project)
            }
        }
    }
    
    // Load ONLY missing data in parallel when screen starts - phases, expenses, etc.
    // Basic project data (budget, dates, team members) is already available from preloadedProject
    LaunchedEffect(projectId) {
        val userId = authState.user?.phone
        val userRole = authState.user?.role

        if (userId != null) {
            approverProjectViewModel.loadBusinessHeadPhone(userId)
        }
        
        // Early return: Skip if projectId is empty
        if (projectId.isBlank()) return@LaunchedEffect
        
        // Check if data is already loaded to avoid redundant fetches
        val currentProjectId = approverProjectViewModel.selectedProject.value?.id
        val hasPhases = approverProjectViewModel.allPhases.value.isNotEmpty()
        val hasExpenses = approverProjectViewModel.projectExpenses.value.isNotEmpty()
        
        // Only load if project changed or data is missing
        val needsLoading = currentProjectId != projectId || !hasPhases || !hasExpenses
        
        if (!needsLoading) return@LaunchedEffect
        
        // Load all independent data sources in parallel using async for maximum speed
        try {
            coroutineScope {
                // 1. Load ONLY missing data (phases, expenses) - skip basic project fetch if preloaded
                // If preloaded project exists, ViewModel will skip fetching basic project data
                val projectDeferred = async {
                    approverProjectViewModel.loadProjectBudgetSummary(projectId, preloadedProject)
                }
                
                // 2. Load temporary approvers (independent, can run in parallel)
                val tempApproverDeferred = async {
                    temporaryApproverViewModel.loadTemporaryApprovers(projectId)
                }
                
                // 3. Load phase request count (for Business Head and Manager, non-blocking)
                val phaseRequestDeferred = if (userRole == UserRole.BUSINESS_HEAD || userRole == UserRole.MANAGER) {
                    async {
                        approverProjectViewModel.loadPhaseRequestCount(projectId)
                    }
                } else {
                    null
                }
                
                // 4. Load notifications (from local storage, fast, non-blocking)
                val notificationsDeferred = if (notificationPrimaryUserId.isNotEmpty()) {
                    async {
                        notificationViewModel.loadNotifications(
                            notificationPrimaryUserId,
                            notificationAlternateUserIds
                        )
                    }
                } else {
                    null
                }
                
                // Fire all operations in parallel - don't await to start next operations faster
                // Critical data (project) is awaited first, others can complete in background
                projectDeferred.await()
                tempApproverDeferred.await()
                phaseRequestDeferred?.await()
                notificationsDeferred?.await()
            }
        } catch (e: Exception) {
            Log.e("ApproverProjectDashboard", "Error loading dashboard data: ${e.message}", e)
        }
    }
    
    // Load temporary approver user data when temporary approver data is loaded (non-blocking, background)
    // Optimized: Only load if user is not already loaded
    LaunchedEffect(currentTemporaryApprover?.approverPhone) {
        val approverPhone = currentTemporaryApprover?.approverPhone
        if (approverPhone != null && temporaryApproverUser?.phone != approverPhone) {
            isLoadingTemporaryApprover = true
            scope.launch {
                try {
                    val user = authViewModel.getUserByPhoneNumber(approverPhone)
                    temporaryApproverUser = user
                } catch (e: Exception) {
                    // Error loading temporary approver user
                } finally {
                    isLoadingTemporaryApprover = false
                }
            }
        } else if (approverPhone == null) {
            temporaryApproverUser = null
        }
    }
    
    // Refresh function for pull-to-refresh and notification-driven sync.
    // Forces a server-backed refresh for core dashboard data while keeping UI responsive.
    fun refreshData(trigger: String = "pull_to_refresh") {
        if (isRefreshing) return
        scope.launch {
            isRefreshing = true
            refreshErrorMessage = null
            try {
                val userId = authState.user?.phone
                val userRole = authState.user?.role
                
                // Refresh all data in parallel - critical data awaited, non-critical tasks can run concurrently.
                coroutineScope {
                    // Critical: Force latest project/phases/expenses from repository (Firestore-backed path).
                    val projectRefreshDeferred = async {
                        approverProjectViewModel.loadProjectBudgetSummary(
                            projectId = projectId,
                            preloadedProject = null,
                            forceRefresh = true
                        )
                    }
                    
                    // Critical: Temporary approvers needed for UI
                    val tempApproverRefreshDeferred = async {
                        temporaryApproverViewModel.loadTemporaryApprovers(projectId)
                    }
                    
                    // Notifications: refresh local notification state so popup/badge stay in sync.
                    if (notificationPrimaryUserId.isNotEmpty()) {
                        launch {
                            notificationViewModel.refreshFromLocalStorage(
                                notificationPrimaryUserId,
                                notificationAlternateUserIds
                            )
                        }
                    }
                    
                    // Non-critical: Phase request count can load in background (fire-and-forget)
                    if (userRole == UserRole.BUSINESS_HEAD || userRole == UserRole.MANAGER) {
                        launch {
                            approverProjectViewModel.loadPhaseRequestCount(projectId)
                        }
                    }
                    
                    // Wait only for critical operations to complete
                    projectRefreshDeferred.await()
                    tempApproverRefreshDeferred.await()
                }
                debugLog("ApproverProjectDashboard") { "Refresh completed successfully (trigger=$trigger)" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ApproverProjectDashboard", "Error refreshing data: ${e.message}", e)
                refreshErrorMessage = e.message ?: "Refresh failed. Please check your connection and retry."
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(refreshErrorMessage) {
        val message = refreshErrorMessage ?: return@LaunchedEffect
        android.widget.Toast.makeText(
            context,
            message,
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ApproverNavigationDrawer(
                projectId = projectId,
                project = selectedProject,
                isFutureDelegationRestricted = isTempApproverFutureRestricted,
                onNavigateToDashboard = {
                    scope.launch { drawerState.close() }
                    // Already on dashboard
                },
                onNavigateToPendingApprovals = {
                    scope.launch { drawerState.close() }
                    // Pass role along with projectId - need to update callback signature
                    onNavigateToPendingApprovals(projectId)
                },
                onNavigateToAddExpense = {
                    scope.launch { drawerState.close() }
                    onNavigateToAddExpense()
                },
                onNavigateToAnalytics = {
                    scope.launch { drawerState.close() }
                    onNavigateToAnalytics(projectId)
                },
                onNavigateToPendingByCredit = { projectIdParam ->
                    scope.launch { drawerState.close() }
                    onNavigateToPendingByCredit(projectIdParam)
                },
                onNavigateToDelegation = {
                    scope.launch { drawerState.close() }
                    onNavigateToDelegation()
                },
                onNavigateToChat = { projectId, projectName ->
                    scope.launch { drawerState.close() }
                    onNavigateToChat(projectId, projectName)
                },
                projectName = currentProject?.name ?: projectBudgetSummary.project?.name ?: "Project",
                userRole = authState.user?.role?.name ?: "APPROVER"
            )
        }
    ) {
        val iosPalette = rememberIosDashboardPalette()
        val dashboardBackgroundBrush = Brush.verticalGradient(
            colors = listOf(
                iosPalette.tier1Background,
                iosPalette.tier1Background,
                iosPalette.tier2Surface
            )
        )

        // cehcking for github actions 

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(dashboardBackgroundBrush)
        ) {
            // Top Bar - Center aligned
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (currentProject != null || projectBudgetSummary.project != null) {
                            Text(
                                text = currentProject?.name ?: projectBudgetSummary.project?.name ?: "",
                                fontSize = responsiveFontSize(16.sp, 17.sp, 18.sp, 20.sp),
                                fontWeight = FontWeight.Bold,
                                color = iosPalette.textPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            
                            val project = currentProject ?: projectBudgetSummary.project
                            val isInReview = project?.status == "IN REVIEW"
                            
                            val displayStatus = if (isInReview) {
                                "IN REVIEW"
                            } else {
                                maintenanceStatus ?: (project?.status ?: "ACTIVE")
                            }
                            // Transform display status text - show "DECLINED" instead of "REVIEW REJECTED"
                            val displayStatusText = if (displayStatus == "REVIEW REJECTED") "DECLINED" else displayStatus
                            Text(
                                text = displayStatusText,
                                fontSize = responsiveFontSize(10.sp, 11.sp, 12.sp, 13.sp),
                                color = iosPalette.textSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = iosPalette.accent,
                            modifier = Modifier.size(responsiveIconSize(20.dp, 22.dp, 24.dp))
                        )
                    }
                        Text(
                            text = "Back",
                            color = iosPalette.accent,
                            fontSize = responsiveFontSize(12.sp, 13.sp, 14.sp, 15.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Project notification bell icon (for all users)
                        Box {
                            IconButton(onClick = { showNotificationPopup = true }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Project Notifications",
                                    tint = iosPalette.accent,
                                    modifier = Modifier.size(responsiveIconSize(20.dp, 22.dp, 24.dp))
                                )
                            }
                            // Badge counter for project notifications - positioned exactly above the icon
                            if (projectNotificationBadge.hasUnread && projectNotificationBadge.count > 0) {
                                val badgeSize = responsiveIconSize(16.dp, 18.dp, 20.dp)
                                val badgeFontSize = responsiveFontSize(10.sp, 11.sp, 12.sp, 13.sp)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = responsiveSpacing() / 2, y = (-2).dp)
                                        .size(badgeSize)
                                        .background(
                                            MaterialTheme.colorScheme.error,
                                            androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (projectNotificationBadge.count > 99) "99+" else projectNotificationBadge.count.toString(),
                                        color = iosPalette.onAccent,
                                        fontSize = badgeFontSize,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = badgeFontSize * 1.2f,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        
                        // Edit project button for Business Head and Manager
                        if (authState.user?.role == UserRole.BUSINESS_HEAD || authState.user?.role == UserRole.MANAGER) {
                            IconButton(onClick = { onNavigateToEditProject(projectId) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Project",
                                    tint = iosPalette.accent,
                                    modifier = Modifier.size(responsiveIconSize(18.dp, 20.dp, 22.dp))
                                )
                            }
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
            
            when {
                (true) -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Notification Popup
                        if (showNotificationPopup) {
                            ProjectNotificationPopup(
                                notifications = projectNotifications,
                                onDismiss = { showNotificationPopup = false },
                                onNotificationClick = { notification ->
                                    try {
                                        showNotificationPopup = false
                                        refreshData(trigger = "notification_click")

                                        val navTarget = notification.navigationTarget
                                        val notificationType = notification.type.name
                                        val notifType = notification.type
                                        val notificationMessage = notification.message
                                        val projectIdFromNotification = notification.projectId
                                        val relatedId = notification.relatedId
                                        val notificationTitle = notification.title
                                        val notifTypeStr = buildExpenseListNotificationTag(
                                            notificationType = notificationType,
                                            navigationTarget = navTarget,
                                            notifType = notifType,
                                            title = notification.title,
                                            message = notificationMessage
                                        )

                                        fun highlightDashboard(tag: String) {
                                            val normalizedTag = normalizeExpenseListNotificationTag(tag)
                                            if (normalizedTag.isNotEmpty()) {
                                                notificationHighlightTag = normalizedTag
                                                notificationHighlightKey += 1
                                            }
                                        }
                                        debugLog("ChatnotificationfromDashboard") { "notifType $notifType, navTarget $navTarget, message $notificationMessage, title $notificationTitle" }


                                        when {
                                            // ── EXPENSE CHAT notifications → Expense Chat screen ──
                                            notificationTitle.contains("New expense chat msg", ignoreCase = true) ||
                                                (
                                                  (notificationMessage.lowercase().contains("expense chat") ||
                                                        notificationMessage.lowercase().contains("logged"))) -> {
                                                if (relatedId.isNotEmpty()) {
                                                    onNavigateToExpenseChat(relatedId)
                                                } else {
                                                    highlightDashboard(notifTypeStr)
                                                }
                                            }


                                            // ── CHAT notifications → Chat screen ──────────────────────────
                                            notifType == com.cubiquitous.tracura.model.NotificationType.CHAT_MESSAGE ||
                                                navTarget.startsWith("chat/", ignoreCase = true) ||
                                                navTarget.contains("chat_detail", ignoreCase = true) ||
                                                navTarget.contains("chat_screen", ignoreCase = true) ||
                                                navTarget.contains("customer_chat", ignoreCase = true) -> {
                                                val chatProjectId: String
                                                val chatId: String
                                                val chatUserName: String
                                                if (navTarget.startsWith("chat/", ignoreCase = true)) {
                                                    val parts = navTarget.split("/")
                                                    val sender = notificationMessage.split(" ")
                                                    chatProjectId = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
                                                        ?: projectIdFromNotification
                                                    chatId = parts.getOrNull(2)?.takeIf { it.isNotEmpty() } ?: relatedId
                                                    chatUserName = sender.getOrNull(0) ?: notification.title
                                                } else {
                                                    chatProjectId = projectIdFromNotification
                                                    chatId = relatedId
                                                    chatUserName = notification.title
                                                }
                                                if (chatProjectId.isNotEmpty() && chatId.isNotEmpty()) {
                                                                                                    debugLog("ChatnotificationfromDashboard") { "chatp projectid $chatProjectId, chatId $chatId, chatUserName $chatUserName" }
                                                    onNavigateToChatDetail(chatProjectId, chatId, chatUserName)
                                                } else {
                                                                                                    debugLog("ChatnotificationfromDashboard") { "Insufficient data to navigate to chat - projectId: $chatProjectId, chatId: $chatId" }
                                                    highlightDashboard(notifTypeStr)
                                                }
                                            }

                                            // ── EXPENSE REVIEW/STATUS notifications → Expense Review screen ─
                                            notifType == com.cubiquitous.tracura.model.NotificationType.EXPENSE_SUBMITTED ||
                                                notifType == com.cubiquitous.tracura.model.NotificationType.EXPENSE_APPROVED ||
                                                notifType == com.cubiquitous.tracura.model.NotificationType.EXPENSE_REJECTED ||
                                                navTarget.contains("expense_detail", ignoreCase = true) ||
                                                navTarget.contains("expense_review", ignoreCase = true) ||
                                                notificationMessage.contains("your expense", ignoreCase = true) ||
                                                (notificationMessage.contains("payment", ignoreCase = true) &&
                                                    (notificationMessage.contains("updated", ignoreCase = true) ||
                                                        notificationMessage.contains("completed", ignoreCase = true))) -> {
                                                if (relatedId.isNotEmpty()) {
                                                    onNavigateToExpenseReview(relatedId)
                                                } else {
                                                    highlightDashboard(notifTypeStr)
                                                }
                                            }

                                            // ── PENDING APPROVAL notifications → dashboard highlight ───────
                                            notifType == com.cubiquitous.tracura.model.NotificationType.PENDING_APPROVAL ||
                                                navTarget.contains("pending_approval", ignoreCase = true) -> {
                                                highlightDashboard(notifTypeStr)
                                            }

                                            // ── Everything else → dashboard highlight ─────────────────────
                                            else -> {
                                                highlightDashboard(notifTypeStr)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ApproverProjectDashboard", "Error handling notification click: ${e.message}", e)
                                    }
                                },
                                onMarkAsRead = { notificationId ->
                                    notificationViewModel.markNotificationAsRead(notificationId)
                                }
                            )
                        }
                        
                        SwipeRefresh(
                            state = rememberSwipeRefreshState(isRefreshing = isRefreshing || isLoading),
                            onRefresh = { refreshData(trigger = "pull_to_refresh") }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                            // Department Budgets Section
                            // Project Overview Section - show only enabled phases here (already filtered in ViewModel)
                            ProjectOverviewSection(
                                projectBudgetSummary = projectBudgetSummary,
                                fromNotification = dashboardHighlightTag,
                                notificationMessage = NotificationMessage,
                                currentProject = currentProject,
                                instantBudgetSummary = instantBudgetSummary,
                                approverDisplaySummary = approverDisplaySummary,
                                temporaryApproverUser = temporaryApproverUser,
                                isLoadingTemporaryApprover = isLoadingTemporaryApprover,
                                currentTemporaryApprover = currentTemporaryApprover,
                                phaseBreakdowns = phaseBreakdowns,
                                projectId = projectId,
                                approverProjectViewModel = approverProjectViewModel,
                                authState = authState,
                                selectedProject = selectedProject,
                                onNavigateToDepartmentDetailWrapper = onNavigateToDepartmentDetailWrapper,
                                onNavigateToDelegation = onNavigateToDelegation,
                                highlightKey = notificationHighlightKey,
                                onBudgetCardClick = {
                                    val requester = if (authState.user?.role == UserRole.APPROVER) {

                                        activeHighlightTag = NotificationMessage
                                        phaseDistributionBringIntoViewRequester

                                    } else {
                                        departmentDistributionBringIntoViewRequester
                                    }
                                    scope.launch {
                                        requester.bringIntoView()
                                    }
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))

                           // Log.d("CheckingfromNotificationMesssage", "from Notification ${fromNotification}")

                            LaunchedEffect(phaseBreakdowns) {
                                if ((fromNotification == "PhaseEndHighlighting" || fromNotification == "RemainingBudgetUpdate") && NotificationMessage.isNotBlank()) {
                                    // delay(300) // Small delay to ensure the layout is fully drawn first
                                    val centerOffset = screenHeightPx / 2f
                                    specificPhaseHighlightBringIntoViewRequester.bringIntoView(
                                        Rect(
                                            left = 0f,
                                            top = -centerOffset,   // Asks for space above
                                            right = 0f,
                                            bottom = centerOffset  // Asks for space below
                                        )
                                    )
                                }
                            }
                            
                            // Always show enabled phases in overview (if any)
                            if (phaseBreakdowns.isNotEmpty()) {
                                // Dashboard should show only active/ongoing phases.
                                val ongoing = ongoingPhaseBreakdowns

                                // For APPROVER role, filter departments to show only assigned ones
                                val userRole = authState.user?.role
                                val userPhone = normalizePhoneForCompare(authState.user?.phone)
                                val project = effectiveProject
                                val canActAsApprover = if (userPhone.isNotBlank() && project != null) {
                                    userRole == UserRole.APPROVER || project.departmentTemporaryApprover.any { entry ->
                                        val todayPhase = java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }.time
                                        isActiveTempApproverForDate(entry, userPhone, todayPhase)
                                    }
                                } else {
                                    false
                                }
                                
                                // Get assigned departments for current approver (permanent + valid temporary)
                                val assignedDepartments = if (canActAsApprover && project != null) {
                                    val todayPhase = java.util.Calendar.getInstance().apply {
                                        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
                                    }.time
                                    val permanentAssignments = project.departmentApproverAssignments
                                        .filter { (_, approverPhone) ->
                                            normalizePhoneForCompare(approverPhone) == userPhone
                                        }
                                        .keys.toSet()
                                    val tempAssignments = project.departmentTemporaryApprover
                                        .filter { entry ->
                                            isActiveTempApproverForDate(entry, userPhone, todayPhase)
                                        }
                                        .map { it.departmentName }.toSet()
                                    val assignments = permanentAssignments + tempAssignments
                                    
                                                                        debugLog("DeptFilter") { "User Role: $userRole" }
                                                                        debugLog("DeptFilter") { "User Phone: $userPhone" }
                                                                        debugLog("DeptFilter") { "Permanent assigned depts: $permanentAssignments, Temp depts: $tempAssignments" }
                                                                        debugLog("DeptFilter") { "All assigned departments for this approver: $assignments" }
                                    
                                    assignments
                                } else {
                                                                        debugLog("DeptFilter") { "User Role: $userRole - Showing all departments" }
                                    null // null means show all departments (for BUSINESS_HEAD)
                                }
                                
                                if (ongoing.isNotEmpty()) {

                                                                    debugLog("projectTempApprovercheck") { "assiend departments for approver: $assignedDepartments" }
                                    ongoing.forEachIndexed { idx, pb ->
                                        // Filter departments if user is APPROVER
                                        val filteredDepartments = if (assignedDepartments != null) {
                                            pb.departments.filter { dept ->
                                                // Extract department name from the breakdown
                                                // Department names in breakdown are in format: "phaseId_departmentName"
                                                val deptName = if (dept.department.contains("_")) {
                                                    dept.department.substringAfter("_")
                                                } else {
                                                    dept.department
                                                }
                                                
                                                val isAssigned = matchesAssignedDepartment(
                                                    deptName,
                                                    assignedDepartments
                                                )
                                                debugLog("projectTempApprovercheck") { "Department: ${dept.department}, Extracted: $deptName, IsAssigned: $isAssigned" }
                                                
                                                isAssigned
                                            }
                                        } else {
                                            pb.departments // Show all for BUSINESS_HEAD
                                        }

                                        debugLog("projectTempApprovercheck") { "checking the filtered department lenght : ${filteredDepartments.size} for phase ${pb.phaseTitle} with original dept size ${pb.departments.size}" }
                                        
                                        // Only show phase if it has departments assigned to this approver
                                        if (filteredDepartments.isNotEmpty()) {

                                            val isTarget = (fromNotification == "PhaseEndHighlighting" || fromNotification == "RemainingBudgetUpdate") && NotificationMessage.contains(pb.phaseTitle, ignoreCase = true)

                                            Box(
                                                modifier = if (isTarget) {
                                                    Modifier.bringIntoViewRequester(specificPhaseHighlightBringIntoViewRequester)
                                                } else {
                                                    Modifier
                                                }
                                            ) {
                                            PhaseBudgetsSection(
                                                title = pb.phaseTitle.ifEmpty { "Phase ${idx + 1}" },
                                                departments = filteredDepartments,
                                                fromNotification = if (isTarget) NotificationMessage else "",
                                                notificationMessage = NotificationMessage,
                                                onNavigateToDepartmentDetail = { deptProjectId: String, dept: String, budgetAllocated: Double?, spent: Double?, remaining: Double? -> 
                                                    // Check if dept is already in full format (phaseId_departmentName)
                                                    // The department name from breakdown might be:
                                                    // 1. Display name (if departmentNameMap had values) - e.g., "Stage 1"
                                                    // 2. Full format (if departmentNameMap was empty) - e.g., "phaseId_Stage 1"
                                                    val fullDeptName = if (dept.contains("_") && !dept.startsWith("Others_")) {
                                                        // Check if it starts with the current phaseId
                                                        if (dept.startsWith("${pb.phaseId}_")) {
                                                            dept // Already in correct full format
                                                        } else {
                                                            // It's in full format but for a different phase, reconstruct for current phase
                                                            // Extract the display name and reconstruct
                                                            val displayName = dept.substringAfter("_")
                                                            "${pb.phaseId}_$displayName"
                                                        }
                                                    } else {
                                                        // It's a display name, construct full format
                                                        "${pb.phaseId}_$dept"
                                                    }
                                                    // Pass budget values to avoid redundant fetching
                                                    onNavigateToDepartmentDetailWrapper(deptProjectId, fullDeptName, budgetAllocated, spent, remaining)
                                                },
                                                onAddDepartment = { departmentDraft ->
                                                    // Delegate to ViewModel to update Firestore with full department details
                                                    approverProjectViewModel.addDepartmentWithDetailsToPhase(
                                                        projectId = projectId,
                                                        phaseId = pb.phaseId,
                                                        departmentDraft = departmentDraft
                                                    )
                                                },
                                                startDate = pb.startDate,
                                                endDate = pb.endDate,
                                                isOngoing = true,
                                                phaseId = pb.phaseId,
                                                isActive = pb.isActive,
                                                onToggleActive = { active ->
                                                    approverProjectViewModel.updatePhaseEnabled(projectId, pb.phaseId, active)
                                                },
                                                userRole = authState.user?.role,
                                                projectId = projectId,
                                                expenseRepository = null, // Using ViewModel's expense data instead
                                                showEditButton = authState.user?.role == UserRole.BUSINESS_HEAD || authState.user?.role == UserRole.MANAGER,
                                                onEditClick = { /* Not used in Project Overview, but required parameter */ },
                                                approverProjectViewModel = approverProjectViewModel,
                                                phasesWithDepartmentsData = phasesWithDepartments,
                                                phaseDepartmentSpentMapData = phaseDepartmentSpentMap,
                                                phaseAnonymousExpensesMapData = phaseAnonymousExpensesMap,
                                                allPhasesData = allPhases,
                                                plannedStartDate = selectedProject?.startDate // Pass project's planned start date
                                            )
                                        }
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                } else if (allPhases.isNotEmpty()) {
                                    // Show message when all phases are disabled
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "NO ACTIVE PHASE TO SHOW",
                                                fontSize = responsiveFontSize(14.sp, 15.sp, 16.sp, 17.sp),
                                                fontWeight = FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            } else if (allPhases.isNotEmpty()) {
                                // Show message when all phases are disabled
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "NO ACTIVE PROJECTS TO SHOW",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            // View All Phases button - ALWAYS visible if there are any phases (enabled or disabled)
//                            var showAllPhases by remember { mutableStateOf(false) }
//                            if (allPhases.isNotEmpty()) {
//                                Button(
//                                    onClick = { showAllPhases = true },
//                                    modifier = Modifier.fillMaxWidth(),
//                                    shape = RoundedCornerShape(12.dp),
//                                    colors = ButtonDefaults.buttonColors(
//                                        containerColor = Color(0xFFE3F2FD),
//                                        contentColor = Color(0xFF1976D2)
//                                    ),
//                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
//                                ) {
//                                    Text(
//                                        "View All Phases",
//                                        fontSize = responsiveFontSize(14.sp, 15.sp, 16.sp, 17.sp),
//                                        fontWeight = FontWeight.Normal,
//                                        maxLines = 1,
//                                        overflow = TextOverflow.Ellipsis
//                                    )
//                                }
//                                Spacer(modifier = Modifier.height(16.dp))
//                            }


                            
                            // Department Budgets Section - REMOVED per user request
                            // DepartmentBudgetsSection(
                            //     departmentBreakdown = projectBudgetSummary.departmentBreakdown,
                            //     onNavigateToDepartmentDetail = onNavigateToDepartmentDetail,
                            //     projectId = projectId
                            // )
                            
                            // Spacer(modifier = Modifier.height(24.dp))
                            
                            // Department Distribution Section
                            val userPhone = normalizePhoneForCompare(authState.user?.phone)
                            val canActAsApprover = remember(userRole, userPhone, currentProject, selectedProject) {
                                val projectToCheck = currentProject ?: selectedProject
                                if (userPhone.isBlank() || projectToCheck == null) {
                                    false
                                } else {
                                    if (userRole == UserRole.APPROVER) {
                                        true
                                    } else {
                                        val todayExp = java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }.time
                                        projectToCheck.departmentTemporaryApprover.any { entry ->
                                            isActiveTempApproverForDate(entry, userPhone, todayExp)
                                        }
                                    }
                                }
                            }
                            
                            val assignedDepartments = remember(userRole, userPhone, currentProject, selectedProject) {
                                if (canActAsApprover && userPhone.isNotBlank()) {
                                    val projectToCheck = currentProject ?: selectedProject
                                    val todayExp = java.util.Calendar.getInstance().apply {
                                        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
                                    }.time
                                    val permanentExp = projectToCheck?.departmentApproverAssignments
                                        ?.filter { (_, approverPhone) ->
                                            normalizePhoneForCompare(approverPhone) == userPhone
                                        }
                                        ?.keys
                                        ?.map { it.trim() }
                                        ?.toSet() ?: emptySet()
                                    val tempExp = projectToCheck?.departmentTemporaryApprover
                                        ?.filter { entry ->
                                            isActiveTempApproverForDate(entry, userPhone, todayExp)
                                        }
                                        ?.map { it.departmentName.trim() }
                                        ?.toSet() ?: emptySet()
                                    (permanentExp + tempExp)
                                        .flatMap { dept ->
                                            listOf(
                                                dept,
                                                dept.substringAfterLast("_").trim()
                                            )
                                        }
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                        .toSet()
                                        .ifEmpty { null }
                                } else {
                                    null
                                }
                            }

                            if (canActAsApprover) {
                                val relevantExpenses = remember(projectExpenses, assignedDepartments) {
                                    if (assignedDepartments != null) {
                                        debugLog("projectExpenses") { "expenses List ${projectExpenses.size}" }
                                        projectExpenses.filter { expense ->
                                            val expenseDepartmentRaw = expense.department.trim()
                                            val expenseDepartmentDisplay = try {
                                                FormatUtils.getDepartmentDisplayName(expense.department)
                                            } catch (e: Exception) {
                                                expense.department
                                            }

                                            matchesAssignedDepartment(expenseDepartmentRaw, assignedDepartments) ||
                                                matchesAssignedDepartment(expenseDepartmentDisplay, assignedDepartments)
                                        }.sortedByDescending { 
                                            it.submittedAt?.toDate()?.time ?: it.getDateAsTimestamp()?.toDate()?.time ?: 0L
                                        }
                                    } else {
                                        emptyList()
                                    }
                                }
                                
                                val allPhasesList = remember(phasesWithDepartments) {
                                    phasesWithDepartments.map { it.phase }
                                }

                                // Phase Budget Distribution Section (only for APPROVER role)
                                if (canActAsApprover && phaseBudgetData.isNotEmpty()) {
                                    PhaseBudgetDistributionSection(
                                        notificationMessage = NotificationMessage,
                                        rowHighlightColor = rowHighlightColor,
                                        phaseBudgetData = phaseBudgetData,
                                        onPhaseClick = { phaseData ->
                                            val fullDepartmentName = if (
                                                phaseData.departmentName.startsWith("${phaseData.phaseId}_") ||
                                                phaseData.departmentName.startsWith("Others_")
                                            ) {
                                                phaseData.departmentName
                                            } else {
                                                "${phaseData.phaseId}_${phaseData.departmentName}"
                                            }
                                            onNavigateToDepartmentDetailWrapper(
                                                projectId,
                                                fullDepartmentName,
                                                phaseData.budgetAllocated,
                                                phaseData.spent,
                                                phaseData.remaining
                                            )
                                        },
                                        modifier = Modifier
                                            .bringIntoViewRequester(phaseDistributionBringIntoViewRequester)
                                            .padding(bottom = 16.dp)
                                    )
                                }

                                ApproverRecentExpensesCard(
                                    expenses = relevantExpenses.take(4),
                                    totalExpensesCount = relevantExpenses.size, // Total count of all relevant expenses
                                    phases = allPhases,
                                    userNameMap = userNameMap,
                                    onShowAll = { onNavigateToAllExpenses() },
                                    onNavigateToExpenseReview = onNavigateToExpenseReview,
                                    onEditExpense = onNavigateToEditExpense,
                                    currentUserPhone = authState.user?.phone?.replace("+91", "")?.trim(),
                                    businessHeadPhone = businessHeadPhone
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bringIntoViewRequester(departmentDistributionBringIntoViewRequester)
                                ) {
                                    DepartmentDistributionSection(
                                        phasesWithDepartments = phasesWithDepartments,
                                        projectExpenses = projectExpenses,
                                        onNavigateToReports = { onNavigateToReports(projectId) },
                                        scope = scope,
                                        drawerState = drawerState
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                        }

                        if (isTempApproverFutureRestricted) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { },
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = iosPalette.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = tempApproverFutureMessage,
                                            fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                                            color = iosPalette.textPrimary,
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Floating Action Buttons - Left and Right corners
                        // Left FAB - Open Navigation Drawer
                        FloatingActionButton(
                            onClick = { 
                                scope.launch { drawerState.open() }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = 16.dp)
                                .size(56.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = RoundedCornerShape(28.dp),
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 12.dp
                            )
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Open Menu",
                                modifier = Modifier.size(24.dp)
                            )
                        }

//                        if (userRole == UserRole.BUSINESS_HEAD){// Right FAB - Project Report
//                            FloatingActionButton(
//                                onClick = { onNavigateToReports(projectId) },
//                                modifier = Modifier
//                                    .align(Alignment.BottomEnd)
//                                    .padding(end = 16.dp, bottom = 16.dp)
//                                    .size(56.dp),
//                                containerColor = Color(0xFF4285F4),
//                                contentColor = Color.White,
//                                shape = RoundedCornerShape(28.dp),
//                                elevation = FloatingActionButtonDefaults.elevation(
//                                    defaultElevation = 8.dp,
//                                    pressedElevation = 12.dp
//                                )
//                            ) {
//                                Icon(
//                                    Icons.Default.Description,
//                                    contentDescription = "Project Report",
//                                    modifier = Modifier.size(24.dp)
//                                )
//                            }
//                        }
                    }
                }
            }
        }
    }
    
    // DepartmentDetailScreen Bottom Sheet
    if (showDepartmentDetailSheet) {
        val deptSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val windowHeight = LocalConfiguration.current.screenHeightDp.dp
        
        ModalBottomSheet(
            onDismissRequest = { showDepartmentDetailSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = deptSheetState,
            dragHandle = {
                Box(
                    modifier = Modifier
//                        .padding(top = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            color = Color(0xFFC6C6C8),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(windowHeight * 0.9f)
            ) {
                DepartmentDetailScreen(
                    projectId = departmentDetailProjectId,
                    departmentName = departmentDetailName,
                    onNavigateBack = { showDepartmentDetailSheet = false },
                    onNavigateToExpenseChat = { expenseId ->
                        showDepartmentDetailSheet = false
                        onNavigateToChatDetail(projectId, expenseId, "")
                    },
                    onNavigateToReview = { expenseId ->
                        showDepartmentDetailSheet = false
                        onNavigateToExpenseReview(expenseId)
                    },
                    onNavigateToDetail = { expenseId ->
                        showDepartmentDetailSheet = false
                        onNavigateToExpenseDetail(expenseId)
                    },
                    initialBudgetAllocated = departmentDetailBudget,
                    initialSpent = departmentDetailSpent,
                    initialRemaining = departmentDetailRemaining,
                    backStackEntry = null
                )
            }
        }
    }
    
    // Phase Requests Modal
    if (showPhaseRequestsModal) {
        PhaseRequestsModal(
            projectId = projectId,
            onDismiss = {
                showPhaseRequestsModal = false
                // Refresh count when modal closes
                if (authState.user?.role == UserRole.BUSINESS_HEAD || authState.user?.role == UserRole.MANAGER) {
                    approverProjectViewModel.loadPhaseRequestCount(projectId)
                }
            },
            onRequestClick = { request ->
                // Handle request click - could navigate to request details
                showPhaseRequestsModal = false
            },
            viewModel = approverProjectViewModel,
            authViewModel = authViewModel,
            onRequestProcessed = {
                // Refresh count when request is processed
                if (authState.user?.role == UserRole.BUSINESS_HEAD || authState.user?.role == UserRole.MANAGER) {
                    approverProjectViewModel.loadPhaseRequestCount(projectId)
                }
                approverProjectViewModel.loadProjectBudgetSummary(projectId, forceRefresh = true)
            }
        )
    }
}

@Composable
private fun ApproverNavigationDrawer(
    projectId: String,
    project: com.cubiquitous.tracura.model.Project?,
    isFutureDelegationRestricted: Boolean = false,
    onNavigateToDashboard: () -> Unit,
    onNavigateToPendingApprovals: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToPendingByCredit: (String?) -> Unit = { _ -> },
    onNavigateToDelegation: () -> Unit = {},
    onNavigateToChat: (String, String) -> Unit = { _, _ -> },
    projectName: String = "Project",
    userRole: String = "APPROVER"
) {
    val iosPalette = rememberIosDashboardPalette()

    // Check if project is suspended - disable Add Expense if project is suspended
    val isProjectSuspended = remember(project?.isSuspended) {
        project?.isSuspended == true
    }
    
    // Check if project is locked - disable Add Expense if project is locked
    val isProjectLocked = remember(project?.status) {
        project?.status?.uppercase() == "LOCKED"
    }
    
    // Check if project is IN REVIEW
    val isProjectInReview = remember(project?.status) {
        project?.status?.uppercase() == "IN REVIEW"
    }
    
    // Combined check: disable Add Expense if project is suspended, locked, or IN REVIEW
    val isAddExpenseDisabled = isProjectSuspended || isProjectLocked || isProjectInReview || isFutureDelegationRestricted
    // Disable Pending Approvals for suspended or IN REVIEW projects
    val isPendingApprovalsDisabled = isProjectSuspended || isProjectInReview || isFutureDelegationRestricted
    // Disable Dashboard and Chats for locked or IN REVIEW projects
    val isDashboardAndChatsDisabled = isProjectLocked || isProjectInReview || isFutureDelegationRestricted
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = iosPalette.tier2Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(iosPalette.tier2Surface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "TRACURA",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = iosPalette.accent,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Menu Items with colorful circular backgrounds
            if (userRole == "BUSINESS_HEAD") {
                ColorfulDrawerMenuItem(
                    icon = Icons.Default.Person,
                    iconColor = iosPalette.onAccent,
                    backgroundColor = iosPalette.accent,
                    title = "Delegate",
                    onClick = onNavigateToDelegation
                )
                
                ColorfulDrawerMenuItem(
                    icon = Icons.Default.CreditCard,
                    iconColor = iosPalette.onAccent,
                    backgroundColor = iosPalette.accent,
                    title = "Pending by Credit",
                    onClick = { onNavigateToPendingByCredit(projectId) }
                )
            }
            
            ColorfulDrawerMenuItem(
                icon = Icons.Default.Notifications,
                iconColor = if (isPendingApprovalsDisabled) {
                    iosPalette.textSecondary
                } else {
                    iosPalette.onAccent
                },
                backgroundColor = if (isPendingApprovalsDisabled) {
                    iosPalette.tier3Field
                } else {
                    iosPalette.accent
                },
                title = "Pending Approvals",
                onClick = { if (!isPendingApprovalsDisabled) onNavigateToPendingApprovals() },
                enabled = !isPendingApprovalsDisabled
            )
            
            ColorfulDrawerMenuItem(
                icon = Icons.Default.Add,
                iconColor = if (isAddExpenseDisabled) {
                    iosPalette.textSecondary
                } else {
                    iosPalette.onAccent
                },
                backgroundColor = if (isAddExpenseDisabled) {
                    iosPalette.tier3Field
                } else {
                    iosPalette.accent
                },
                title = "Add Expenses",
                onClick = { if (!isAddExpenseDisabled) onNavigateToAddExpense() },
                enabled = !isAddExpenseDisabled
            )
            
            // Analytics - Only show for Business Head users
            if (userRole == "BUSINESS_HEAD") {
                ColorfulDrawerMenuItem(
                    icon = Icons.Default.CheckCircle,
                    iconColor = iosPalette.onAccent,
                    backgroundColor = iosPalette.accent,
                    title = "Analytics",
                    onClick = onNavigateToAnalytics
                )
            }
            
            ColorfulDrawerMenuItem(
                icon = Icons.Default.Person,
                iconColor = if (isDashboardAndChatsDisabled) {
                    iosPalette.textSecondary
                } else {
                    iosPalette.onAccent
                },
                backgroundColor = if (isDashboardAndChatsDisabled) {
                    iosPalette.tier3Field
                } else {
                    iosPalette.accent
                },
                title = "Chats",
                onClick = { if (!isDashboardAndChatsDisabled) onNavigateToChat(projectId, projectName) },
                enabled = !isDashboardAndChatsDisabled
            )
        }
    }
}

@Composable
private fun ColorfulDrawerMenuItem(
    icon: ImageVector,
    iconColor: Color,
    backgroundColor: Color,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val iosPalette = rememberIosDashboardPalette()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { if (enabled) onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(backgroundColor, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) iosPalette.textPrimary else iosPalette.textSecondary
        )
    }
}


private fun DrawScope.drawPieChart(data: List<DepartmentBudgetBreakdown>, total: Double) {
    var startAngle = -90f
    val radius = size.minDimension / 2

    data.forEachIndexed { index, department ->
        val sweepAngle = ((department.spent / total) * 360).toFloat()
        if (sweepAngle > 0) {
            drawArc(
                color = getDepartmentColor(department.department),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true
            )
            startAngle += sweepAngle
        }
    }

    // Draw inner circle to make it a donut chart
    drawCircle(
        color = Color.White,
        radius = radius * 0.6f
    )
}

// Dynamic color assignment for departments with distinct colors
// DepartmentBudget data class for Department Distribution
data class DepartmentBudget(
    val department: String,
    val totalBudget: Double,
    val approvedBudget: Double,
    val color: Color
)

private fun getDepartmentColor(departmentName: String): Color {
    val normalizedName = departmentName.lowercase().trim()
    return when {
        normalizedName.contains("set design") || normalizedName.contains("set design & construction") || normalizedName.contains("production design") -> 
            Color(0xFF3399FF) // Apple Blue (0.2, 0.6, 1.0)
        normalizedName.contains("costumes") || normalizedName.contains("costume design") || normalizedName.contains("wardrobe") -> 
            Color(0xFF4DCC66) // Apple Green (0.3, 0.8, 0.4)
        normalizedName.contains("miscellaneous") || normalizedName.contains("misc") || normalizedName.contains("general") -> 
            Color(0xFFCC66E6) // Apple Purple (0.8, 0.4, 0.9)
        normalizedName.contains("equipment") || normalizedName.contains("equipment rental") || normalizedName.contains("technical") -> 
            Color(0xFFFF9933) // Apple Orange (1.0, 0.6, 0.2)
        normalizedName.contains("travel") || normalizedName.contains("transportation") || normalizedName.contains("logistics") -> 
            Color(0xFFFF4D4D) // Apple Red (1.0, 0.3, 0.3)
        normalizedName.contains("wages") || normalizedName.contains("crew wages") || normalizedName.contains("personnel") -> 
            Color(0xFF33CCCC) // Apple Teal (0.2, 0.8, 0.8)
        normalizedName.contains("marketing") || normalizedName.contains("promotion") || normalizedName.contains("advertising") -> 
            Color(0xFFFF6699) // Apple Pink (1.0, 0.4, 0.6)
        normalizedName.contains("location") || normalizedName.contains("venue") || normalizedName.contains("site") -> 
            Color(0xFF9966CC) // Apple Indigo (0.6, 0.4, 0.8)
        normalizedName.contains("post production") || normalizedName.contains("editing") || normalizedName.contains("post") -> 
            Color(0xFFCCCC33) // Apple Yellow (0.8, 0.8, 0.2)
        normalizedName.contains("sound") || normalizedName.contains("audio") || normalizedName.contains("music") -> 
            Color(0xFF66CC99) // Apple Mint (0.4, 0.8, 0.6)
        normalizedName.contains("lighting") || normalizedName.contains("grip") || normalizedName.contains("electrical") -> 
            Color(0xFFFFB34D) // Apple Amber (1.0, 0.7, 0.3)
        normalizedName.contains("catering") || normalizedName.contains("food") || normalizedName.contains("refreshments") -> 
            Color(0xFFE680B3) // Apple Rose (0.9, 0.5, 0.7)
        normalizedName.contains("insurance") || normalizedName.contains("legal") || normalizedName.contains("compliance") -> 
            Color(0xFF80B3E6) // Apple Sky Blue (0.5, 0.7, 0.9)
        normalizedName.contains("permits") || normalizedName.contains("licenses") || normalizedName.contains("authorization") -> 
            Color(0xFFB399E6) // Apple Lavender (0.7, 0.6, 0.9)
        normalizedName.contains("props") || normalizedName.contains("properties") || normalizedName.contains("accessories") -> 
            Color(0xFFCCE666) // Apple Lime (0.8, 0.9, 0.4)
        normalizedName.contains("makeup") || normalizedName.contains("hair") || normalizedName.contains("beauty") -> 
            Color(0xFFFF80CC) // Apple Magenta (1.0, 0.5, 0.8)
        normalizedName.contains("stunts") || normalizedName.contains("action") || normalizedName.contains("special effects") -> 
            Color(0xFFE64D80) // Apple Crimson (0.9, 0.3, 0.5)
        normalizedName.contains("research") || normalizedName.contains("development") || normalizedName.contains("pre-production") -> 
            Color(0xFF6699CC) // Apple Steel Blue (0.4, 0.6, 0.8)
        normalizedName.contains("distribution") || normalizedName.contains("delivery") || normalizedName.contains("shipping") -> 
            Color(0xFF99CC66) // Apple Chartreuse (0.6, 0.8, 0.4)
        normalizedName.contains("publicity") || normalizedName.contains("media") || normalizedName.contains("communications") -> 
            Color(0xFFCC6699) // Apple Orchid (0.8, 0.4, 0.6)
        normalizedName.contains("security") || normalizedName.contains("safety") || normalizedName.contains("protection") -> 
            Color(0xFFB3804D) // Apple Brown (0.7, 0.5, 0.3)
        normalizedName == "other expenses" -> 
            Color.Gray
        else -> {
            // Hash-based color generation for unknown departments
            // Use a predefined palette of vibrant colors
            val vibrantColors = listOf(
                Color(0xFF3399FF), // Blue
                Color(0xFF4DCC66), // Green
                Color(0xFFCC66E6), // Purple
                Color(0xFFFF9933), // Orange
                Color(0xFFFF4D4D), // Red
                Color(0xFF33CCCC), // Teal
                Color(0xFFFF6699), // Pink
                Color(0xFF9966CC), // Indigo
                Color(0xFFCCCC33), // Yellow
                Color(0xFF66CC99), // Mint
                Color(0xFFFFB34D), // Amber
                Color(0xFFE680B3), // Rose
                Color(0xFF80B3E6), // Sky Blue
                Color(0xFFB399E6), // Lavender
                Color(0xFFCCE666), // Lime
                Color(0xFFFF80CC), // Magenta
                Color(0xFFE64D80), // Crimson
                Color(0xFF6699CC), // Steel Blue
                Color(0xFF99CC66), // Chartreuse
                Color(0xFFCC6699)  // Orchid
            )
            val hash = kotlin.math.abs(departmentName.hashCode())
            val colorIndex = hash % vibrantColors.size
            vibrantColors[colorIndex]
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ProjectOverviewSection(
    projectBudgetSummary: ProjectBudgetSummary,
    fromNotification: String = "",
    notificationMessage: String = "",
    currentProject: com.cubiquitous.tracura.model.Project?,
    instantBudgetSummary: ProjectBudgetSummary,
    approverDisplaySummary: ProjectBudgetSummary? = null,
    temporaryApproverUser: User?,
    isLoadingTemporaryApprover: Boolean,
    currentTemporaryApprover: TemporaryApprover?,
    phaseBreakdowns: List<PhaseBreakdown> = emptyList(),
    projectId: String,
    approverProjectViewModel: ApproverProjectViewModel,
    authState: com.cubiquitous.tracura.model.AuthState,
    selectedProject: com.cubiquitous.tracura.model.Project?,
    onNavigateToDepartmentDetailWrapper: (String, String, Double?, Double?, Double?) -> Unit,
    onNavigateToDelegation: () -> Unit = {},
    highlightKey: Int = 0,
    onBudgetCardClick: () -> Unit = {}
) {

    val context = LocalContext.current
    val userRole = authState.user?.role
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val phasesWithDepartments by approverProjectViewModel.phasesWithDepartments.collectAsState()
    val phaseBudgetMap by approverProjectViewModel.phaseBudgetMap.collectAsState()
    val phaseDepartmentSpentMap by approverProjectViewModel.phaseDepartmentSpentMap.collectAsState()
    val phaseAnonymousExpensesMap by approverProjectViewModel.phaseAnonymousExpensesMap.collectAsState()
    val density = LocalDensity.current
    val iosPalette = rememberIosDashboardPalette()
    val delegationBringIntoViewRequester = remember { BringIntoViewRequester() }
    var delegationCenterScrollPending by remember(fromNotification, highlightKey) {
        mutableStateOf(normalizeExpenseListNotificationTag(fromNotification) == "temp_approver")
    }

    val headerOffsetPx = with(density) { 80.dp.toPx().toInt() }

    // Project code section at the very top of the overview
    val project = currentProject ?: projectBudgetSummary.project
    // if (project?.projectCode?.isNotBlank() == true) {
    //     Text(
    //         text = "#${project.projectCode}",
    //         fontSize = responsiveFontSize(14.sp, 15.sp, 16.sp, 17.sp),
    //         fontWeight = FontWeight.Bold,
    //         color = Color(0xFF1976D2), // Sky blue color for project code
    //         modifier = Modifier.padding(bottom = 8.dp)
    //     )
    // }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Project Overview",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = iosPalette.tier3Field),
            shape = RoundedCornerShape(16.dp)
        ) {
            val buttonPadding = responsivePadding()
            Text(
                text = "Project Details",
                fontSize = responsiveFontSize(12.sp, 13.sp, 14.sp, 15.sp),
                color = iosPalette.accent,
                modifier = Modifier.padding(
                    horizontal = buttonPadding,
                    vertical = buttonPadding / 2
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    // Project Dates Card - Planned, Handover, Maintenance
    val allPhases = approverProjectViewModel.allPhases.collectAsState().value
    
    // Load approver user details
    val authViewModel: AuthViewModel = hiltViewModel()
    var approverUser by remember { mutableStateOf<com.cubiquitous.tracura.model.User?>(null) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(project?.managerId) {
        val managerId = project?.managerId
        if (managerId != null && managerId.isNotEmpty()) {
            scope.launch {
                try {
                    approverUser = authViewModel.getUserByPhoneNumber(managerId)
                } catch (e: Exception) {
                    Log.e("ProjectOverviewSection", "Error loading approver: ${e.message}")
                }
            }
        } else {
            approverUser = null
        }
    }
    
    // Calculate dates
    val plannedDate = project?.startDate
    val isMaintenanceProject =
        project?.statusType == ProjectStatus.MAINTENANCE ||
            project?.status.equals("MAINTENANCE", ignoreCase = true)
    val isSuspendedProject =
        project?.statusType == ProjectStatus.SUSPENDED ||
            project?.status.equals("SUSPENDED", ignoreCase = true)
    // Prioritize project's handoverDate (manually set in backend), fall back to last phase's end date
    val handoverDate = remember(project?.id, project?.handoverDate, allPhases) {
        // First check if project has a manually set handoverDate
        if (!project?.handoverDate.isNullOrBlank()) {
            project?.handoverDate
        } else {
            // Fall back to last created phase's end date
            allPhases
                .sortedByDescending { it.createdAt } // Last created phase
                .firstOrNull()
                ?.endDate
        }
    }
    // Prioritize project's maintenanceDate (manually set in backend), otherwise calculate from handoverDate
    val maintenanceDate = remember(project?.id, project?.maintenanceDate, handoverDate) {
        // First check if project has a manually set maintenanceDate
        if (!project?.maintenanceDate.isNullOrBlank()) {
            project?.maintenanceDate
        } else if (handoverDate != null) {
            // Fall back to calculating from handoverDate (add 30 days)
            try {
                val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val handoverDateObj = dateFormatter.parse(handoverDate)
                if (handoverDateObj != null) {
                    val calendar = java.util.Calendar.getInstance()
                    calendar.time = handoverDateObj
                    calendar.add(java.util.Calendar.DAY_OF_MONTH, 30) // Add 30 days
                    dateFormatter.format(calendar.time)
                } else null
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    
    if (plannedDate != null || handoverDate != null || maintenanceDate != null) {
        val cardPadding = responsivePadding() + 4.dp
        val cardSpacing = responsiveSpacing()
        val iconSize = responsiveIconSize(18.dp, 19.dp, 20.dp)

        val initialHighlightTag = remember(fromNotification, highlightKey) {
        normalizeExpenseListNotificationTag(fromNotification)
    }
    var activeHighlightTag by remember(initialHighlightTag) {
        mutableStateOf(initialHighlightTag)
    }



    LaunchedEffect(initialHighlightTag) {
        activeHighlightTag = initialHighlightTag
        if (activeHighlightTag.isNotEmpty()) {
            delay(2000L)
            activeHighlightTag = ""
        }
    }

    val rowHighlightColor by animateColorAsState(
        targetValue = if (activeHighlightTag.isNotEmpty()) iosPalette.accent.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = tween(durationMillis = 1000),
        label = "RowHighlightAnimation"
    )

    debugLog("CheckingActiveTag") { "activeHighlightTag: $activeHighlightTag initialHighlightTag: $initialHighlightTag fromNotification: $fromNotification" }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                verticalArrangement = Arrangement.spacedBy(cardSpacing)
            ) {

                //Project Code

                //   if (project?.projectCode?.isNotBlank() == true) {
                //         Text(
                //             text = "#${project.projectCode}",
                //             fontSize = responsiveFontSize(14.sp, 15.sp, 16.sp, 17.sp),
                //             fontWeight = FontWeight.Bold,
                //             color = Color(0xFF1976D2), // Sky blue color for project code
                //             modifier = Modifier.padding(bottom = 8.dp)
                //         )
                //     }

                    if (project?.projectCode?.isNotBlank() == true){
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(responsiveSpacing()),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tag, // This gives you a standard # icon
                                    contentDescription = "Handover",
                                    tint = iosPalette.accent,
                                    modifier = Modifier.size(iconSize)
                                )
                                Text(
                                    text = "Project Code: ",
                                    fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                                    color = iosPalette.textSecondary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "${project.projectCode}",
                                fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }


                // Planned Date
                if (plannedDate != null && (project?.status == ProjectStatus.ACTIVE.toString() || project?.status == ProjectStatus.IN_REVIEW.toString() || project?.status == ProjectStatus.LOCKED.toString())) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                                            .background(color = if (activeHighlightTag == "PlannedDateUpdate") rowHighlightColor else Color.Transparent , shape = RoundedCornerShape(8.dp)) // activeHighlightTag   rowHighlightColor
                                        .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(responsiveSpacing()),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Planned",
                                tint = iosPalette.accent,
                                modifier = Modifier.size(iconSize)
                            )
                            Text(
                                text = "Planned:",
                                fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                                color = iosPalette.textSecondary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = plannedDate,
                            fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                debugLog("handoverDateDebug") { "Calculated handoverDate: $handoverDate for project ${project?.id} with status ${project?.status} and statusType ${project?.statusType}" }
                
                // Handover Date
                if (
                    handoverDate != null &&
                    project?.statusType == ProjectStatus.ACTIVE &&
                    !isMaintenanceProject
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                        .background(color = if (activeHighlightTag == "HandOverDateUpdate") rowHighlightColor else Color.Transparent , shape = RoundedCornerShape(8.dp)) // activeHighlightTag   rowHighlightColor
                                        .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(responsiveSpacing()),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Handover",
                                tint = iosPalette.accent,
                                modifier = Modifier.size(iconSize)
                            )
                            Text(
                                text = "Handover:",
                                fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                                color = iosPalette.textSecondary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = handoverDate,
                            fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Maintenance Date
                if (isMaintenanceProject) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                                        .background(color = if (activeHighlightTag == "MaintenanceDateUpdate") rowHighlightColor else Color.Transparent , shape = RoundedCornerShape(8.dp)) // activeHighlightTag   rowHighlightColor
                                        .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(responsiveSpacing()),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Maintenance",
                                tint = iosPalette.accent,
                                modifier = Modifier.size(iconSize)
                            )
                            Text(
                                text = "Maintenance:",
                                fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                                color = iosPalette.textSecondary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = maintenanceDate ?: "null",
                            fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Suspended Date
                if (isSuspendedProject) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (activeHighlightTag == "SuspendedDateUpdate") rowHighlightColor else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(responsiveSpacing()),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Suspended",
                                tint = iosPalette.accent,
                                modifier = Modifier.size(iconSize)
                            )
                            Text(
                                text = "Suspended:",
                                fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                                color = iosPalette.textSecondary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = project?.suspendedDate ?: "null",
                            fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Client Field
                if (!project?.client.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                                        .background(color = if (activeHighlightTag == "ClientUpdate") rowHighlightColor else Color.Transparent , shape = RoundedCornerShape(8.dp)) // activeHighlightTag   rowHighlightColor
                                        .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(responsiveSpacing()),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "Client",
                                tint = iosPalette.accent,
                                modifier = Modifier.size(iconSize)
                            )
                            Text(
                                text = "Client:",
                                fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                                color = iosPalette.textSecondary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = project?.client ?: "",
                            fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Client Primary Number Field (visible to BUSINESS_HEAD and MANAGER only)
                if (!project?.clientPrimaryNumber.isNullOrBlank() &&
                    (userRole == UserRole.BUSINESS_HEAD || userRole == UserRole.MANAGER)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(responsiveSpacing()),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Client Phone",
                                tint = iosPalette.accent,
                                modifier = Modifier.size(iconSize)
                            )
                            Text(
                                text = "Mobile:",
                                fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                                color = iosPalette.textSecondary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = project?.clientPrimaryNumber ?: "",
                                fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${project?.clientPrimaryNumber}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call Client",
                                    tint = iosPalette.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Location Field
                if (!project?.location.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(responsiveSpacing()),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = iosPalette.accent,
                                modifier = Modifier.size(iconSize)
                            )
                            Text(
                                text = "Location:",
                                fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                                color = iosPalette.textSecondary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = project?.location ?: "",
                            fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Approver name and phone number
                if (approverUser != null) {
                    // Check if current user is the approver
                    // authState is passed as a parameter
                    val currentUserPhone = authState.user?.phone?.replace("+91", "")?.replace(" ", "")?.trim() ?: ""
                    val approverPhone = approverUser?.phone?.replace("+91", "")?.replace(" ", "")?.trim() ?: ""
                    val isCurrentUser = currentUserPhone.isNotEmpty() && approverPhone.isNotEmpty() && currentUserPhone == approverPhone
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Approver",
                            tint = iosPalette.textSecondary,
                            modifier = Modifier.size(iconSize)
                        )
                        Spacer(modifier = Modifier.width(responsiveSpacing()))
                        Text(
                            text = if (isCurrentUser) "You" else "${approverUser?.name ?: ""} • ${approverUser?.phone ?: ""}",
                            fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                            color = iosPalette.textSecondary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Dynamic Overview Cards in 2x2 Grid
    val cardSpacing = responsiveSpacing()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(cardSpacing)
    ) {
        val initialHighlightTag = remember(fromNotification, highlightKey) {
            normalizeExpenseListNotificationTag(fromNotification)
        }
        var activeHighlightTag by remember(initialHighlightTag) {
            mutableStateOf(initialHighlightTag)
        }

        val currentTotalBudget = (approverDisplaySummary ?: instantBudgetSummary).totalBudget

        LaunchedEffect(initialHighlightTag, currentTotalBudget) {
            if (activeHighlightTag.isNotEmpty()) {
                if (activeHighlightTag == "RemainingBudgetUpdate" && currentTotalBudget <= 0.0) {
                    // Firebase budget hasn't loaded yet. Keep tag active so it highlights once loaded.
                } else {
                    // Either it's a different tag or the budget has finally loaded.
                    delay(2000L)
                    activeHighlightTag = ""
                }
            }
        }

        val rowHighlightColor by animateColorAsState(
            targetValue = if (activeHighlightTag.isNotEmpty()) iosPalette.accent.copy(alpha = 0.2f) else Color.Transparent,
            animationSpec = tween(durationMillis = 1000),
            label = "RowHighlightAnimation"
        )

        if (true) {
            // Show ACTIVE status card when no temp approver assigned
            val configuration = LocalConfiguration.current
            val statusCardHeight = remember(configuration.screenWidthDp) {
                when {
                    configuration.screenWidthDp < 360 -> 100.dp
                    configuration.screenWidthDp < 480 -> 105.dp
                    else -> 110.dp
                }
            }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(statusCardHeight)
                    .widthIn(min = 140.dp),
                    
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                val statusCardPadding = responsivePadding()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // .padding(statusCardPadding)
                        .background(color = if (activeHighlightTag == "StatusUpdateProject")  rowHighlightColor else Color.Transparent , shape = RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                ) {
                    // Check if planned start date has passed
                    val project = currentProject ?: projectBudgetSummary.project
                    val hasPlannedStartDatePassed = remember(project?.startDate) {
                        if (project?.startDate.isNullOrBlank()) {
                            false
                        } else {
                            try {
                                val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                val plannedStartDate = dateFormatter.parse(project?.startDate)
                                if (plannedStartDate != null) {
                                    val today = java.util.Calendar.getInstance().apply {
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                    }.time
                                    
                                    val plannedStartDateAtMidnight = java.util.Calendar.getInstance().apply {
                                        time = plannedStartDate
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                    }.time
                                    
                                    today.time >= plannedStartDateAtMidnight.time
                                } else {
                                    false
                                }
                            } catch (e: Exception) {
                                false
                            }
                        }
                    }
                    
                    // Get maintenance status from ViewModel (calculated on background thread)
                    val maintenanceStatus by approverProjectViewModel.maintenanceStatus.collectAsState()
                    
                    // Check suspension status from backend
                    val suspensionStatus = remember(project?.isSuspended, project?.suspendedDate) {
                        if (project?.isSuspended == true && !project?.suspendedDate.isNullOrBlank()) {
                            try {
                                val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                dateFormatter.isLenient = false
                                val suspendedDate = dateFormatter.parse(project?.suspendedDate)
                                
                                if (suspendedDate != null) {
                                    val today = java.util.Calendar.getInstance().apply {
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                    }.time
                                    
                                    val suspendedDateAtMidnight = java.util.Calendar.getInstance().apply {
                                        time = suspendedDate
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                    }.time
                                    
                                    // If suspension date hasn't passed, show SUSPENDED
                                    if (today.time <= suspendedDateAtMidnight.time) {
                                        "SUSPENDED"
                                    } else {
                                        null // Suspension date has passed, use project status
                                    }
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }
                    
                    // Status circle indicator (green for ACTIVE, orange for DRAFT, red-orange for SUSPENDED, purple for LOCKED/IN REVIEW, red for REVIEW REJECTED, amber for MAINTENANCE, gray for COMPLETED)
                    val status = project?.status ?: "ACTIVE"
                    // Priority: IN REVIEW (always show) > Suspension > Maintenance > Project Status
                    val displayStatus = if (status == "IN REVIEW") {
                        "IN REVIEW"
                    } else {
                        suspensionStatus ?: maintenanceStatus ?: status
                    }
                    val isDraft = displayStatus == "DRAFT"
                    val isLocked = displayStatus == "LOCKED"
                    val isInReview = displayStatus == "IN REVIEW"
                    val isReviewRejected = displayStatus == "REVIEW REJECTED"
                    val isCompleted = displayStatus == "COMPLETED"
                    val isArchived = displayStatus == "ARCHIVED"
                    // Check if project is suspended (check both displayStatus and project.status)
                    val isSuspended = displayStatus == "SUSPENDED" || project?.status == "SUSPENDED"
                    // Maintenance: maintenance date is set and hasn't passed yet
                    val isMaintenance = displayStatus == "MAINTENANCE"
                    
                    // Check if it's standby (suspended without reason and without suspension date - no active phases)
                    // Standby = project is SUSPENDED but has no suspendedDate and no suspensionReason (auto-suspended due to no active phases)
                    val isStandby = isSuspended && 
                                   (project?.suspendedDate.isNullOrBlank()) && 
                                   (project?.suspensionReason.isNullOrBlank())
                    
                    // Transform display status text - show "DECLINED" instead of "REVIEW REJECTED", "STANDBY" instead of "SUSPENDED" when no reason
                    val displayStatusText = when {
                        displayStatus == "REVIEW REJECTED" -> "DECLINED"
                        isStandby -> "STANDBY"
                        else -> displayStatus
                    }
                    
                    val statusColor = when {
                        isDraft -> Color(0xFFFF9800) // Orange for Draft (keeping existing)
                        isLocked -> Color(0xFF3F51B5) // Indigo for LOCKED
                        isInReview -> Color(0xFF00BCD4) // Cyan for IN_REVIEW
                        isReviewRejected -> Color(0xFFF44336) // Red for REVIEW_REJECTED
                        isStandby -> Color(0xFFE91E63) // Pink for STANDBY
                        isSuspended -> Color(0xFFF44336) // Red for SUSPENDED (with reason)
                        isCompleted -> Color(0xFF2196F3) // Blue for COMPLETED
                        isArchived -> Color(0xFF9E9E9E) // Gray for ARCHIVE
                        isMaintenance -> Color(0xFF9C27B0) // Purple for MAINTENANCE
                        else -> Color(0xFF4CAF50) // Green for ACTIVE
                    }
                    val statusIndicatorSize = responsiveIconSize(12.dp, 13.dp, 14.dp)
                    Box(
                        modifier = Modifier
                            .size(statusIndicatorSize)
                            .background(
                                statusColor,
                                CircleShape
                            )
                    )
                    
                    Spacer(modifier = Modifier.height(responsiveSpacing()))
                    
                    // Status text - matching other cards' main value size
                    // Show Eye icon beside IN REVIEW status
                    // Use smaller font for IN REVIEW and MAINTENANCE (longer text)
                    val statusFontSize = responsiveFontSize(18.sp, 19.sp, 20.sp, 22.sp)
                    val statusIconSize = responsiveIconSize(18.dp, 20.dp, 22.dp)
                    if (isInReview) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(responsiveSpacing() / 2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "In Review",
                                tint = Color(0xFF9C27B0), // Purple color
                                modifier = Modifier.size(statusIconSize)
                            )
                            Text(
                                text = displayStatusText,
                                fontSize = statusFontSize,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Visible,
                                lineHeight = statusFontSize * 1.1f,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    } else if (isMaintenance) {
                        Text(
                            text = displayStatusText,
                            fontSize = statusFontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Visible,
                            lineHeight = statusFontSize * 1.1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = displayStatusText,
                            fontSize = responsiveFontSize(24.sp, 26.sp, 28.sp, 30.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Visible,
                            lineHeight = responsiveFontSize(24.sp, 26.sp, 28.sp, 30.sp) * 1.1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(responsiveSpacing() / 2))
                    
                    // Project Status text - matching other cards' label size
                    Text(
                        text = "Project Status",
                        fontSize = responsiveFontSize(12.sp, 13.sp, 14.sp, 15.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Visible,
                        lineHeight = responsiveFontSize(12.sp, 13.sp, 14.sp, 15.sp) * 1.2f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Total Budget Card —  : entire project budget; APPROVER: assigned departments sum, remaining = budget − COMPLETE expenses
        val budgetSummaryForCard = approverDisplaySummary ?: instantBudgetSummary
        val budgetLabel = if (approverDisplaySummary != null) "Assigned Budget" else "Total Budget"
        // Use the same reactive summary source as total budget so remaining updates immediately.
        val remainingValue = budgetSummaryForCard.totalRemaining
         debugLog("chekcinghighlight") { "activeHighlight $activeHighlightTag , from Notiification $fromNotification " }


        DynamicOverviewCard(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 140.dp),
            icon = Icons.Default.Home,
            iconColor = iosPalette.accent,
            value = FormatUtils.formatCurrencyWithoutDecimals(budgetSummaryForCard.totalBudget),
            label = budgetLabel,
            subtitle = "Remaining: ${FormatUtils.formatCurrencyWithoutDecimals(remainingValue)}",
            subtitleColor = if (remainingValue > 0) PositiveAmountColor else NegativeAmountColor,
            containerColor = iosPalette.tier2Surface,
            onClick = onBudgetCardClick
        )
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        
        // Team Members Card - Dynamic count
        var showTeamMembersDialog by remember { mutableStateOf(false) }
        var showAddTeamMemberSheet by remember { mutableStateOf(false) }
        var selectedUser by remember { mutableStateOf<User?>(null) }
        var showUserExpensesSheet by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        var activeTeamMembersCount by remember { mutableStateOf(0) }
        var teamMembersList by remember { mutableStateOf<List<User>>(emptyList()) }
        var isLoadingTeamMembers by remember { mutableStateOf(false) }
        
        val configuration = LocalConfiguration.current
        val teamMembersCardHeight = remember(configuration.screenWidthDp) {
            when {
                configuration.screenWidthDp < 360 -> 100.dp
                configuration.screenWidthDp < 480 -> 105.dp
                else -> 110.dp
            }
        }
        val teamMembersCardPadding = responsivePadding()
        val teamMembersIconSize = responsiveIconSize(24.dp, 26.dp, 28.dp)
        val teamMembersNumberSize = responsiveFontSize(24.sp, 26.sp, 28.sp, 30.sp)
        val teamMembersLabelSize = responsiveFontSize(12.sp, 13.sp, 14.sp, 15.sp)
        
        // Load available users and approvers for adding team members
        var availableUsers by remember { mutableStateOf<List<User>>(emptyList()) }
        var availableApprovers by remember { mutableStateOf<List<User>>(emptyList()) }
        
        LaunchedEffect(showAddTeamMemberSheet) {
            if (showAddTeamMemberSheet) {
                try {
                    // Get current user's customer ID
                    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    val currentCustomerId = firebaseUser?.uid
                    
                    if (currentCustomerId != null) {
                        val authRepository = com.cubiquitous.tracura.repository.AuthRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance())
                        val allUsers = authRepository.getAllUsers()
                        
                        // Filter users by current customer ID (check both customerId and ownerID mapping)
                        val customerUsers = allUsers.filter { user ->
                            user.customerId == currentCustomerId || (user.customerId == null && user.uid == currentCustomerId)
                        }
                        
                        // Filter active users ONLY (no approvers should be shown)
                        val regularUsers = customerUsers.filter { 
                            it.role == com.cubiquitous.tracura.model.UserRole.USER && (it.isActive == true || it.isActive) 
                        }
                        
                        availableUsers = regularUsers
                        availableApprovers = emptyList() // No approvers should be shown
                    }
                } catch (e: Exception) {
                    Log.e("ApproverProjectDashboard", "Error loading users: ${e.message}")
                }
            }
        }
        
        // Team member phones: BUSINESS_HEAD = users + approvers; APPROVER = assigned departments only
        // Keys: only project id + assignment maps + user identity — NOT projectBudgetSummary (separate
        // StateFlow that changes on every expense update and would cause constant re-fetches).
        val teamMemberPhoneNumbers = remember(
            currentProject?.id,
            currentProject?.departmentUserAssignments,
            currentProject?.departmentApproverAssignments,
            currentProject?.departmentTemporaryApprover,
            authState.user?.role,
            authState.user?.phone
        ) {
            val proj = currentProject ?: return@remember emptyList<String>()
            val userAssignments = proj.departmentUserAssignments   // deptName -> userPhone
            val approverAssignments = proj.departmentApproverAssignments // deptName -> approverPhone
            val userPhone = authState.user?.phone?.replace("+91", "")?.trim()
            val today = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time
            val hasActiveTempAssignment = if (!userPhone.isNullOrBlank()) {
                proj.departmentTemporaryApprover.any { entry ->
                    isActiveTempApproverForDate(entry, userPhone, today)
                }
            } else {
                false
            }

            if (userAssignments.isEmpty() && approverAssignments.isEmpty()) {
                proj.teamMembers.distinct()
            } else {
                val userRole = authState.user?.role
                if ((userRole == UserRole.APPROVER || hasActiveTempAssignment) && !userPhone.isNullOrBlank()) {
                    // APPROVER: only members from departments assigned to this approver
                    val permanentAssignedDeptNames = approverAssignments
                        .filter { (_, phone) -> phone.replace("+91", "").trim() == userPhone }
                        .keys
                        .toSet()

                    val tempAssignedDeptNames = proj.departmentTemporaryApprover
                        .asSequence()
                        .filter { entry ->
                            entry.phone.replace("+91", "").trim() == userPhone &&
                                entry.isAccepted &&
                                entry.isActive &&
                                entry.startDate != null &&
                                entry.endDate != null &&
                                !today.before(entry.startDate) &&
                                !today.after(entry.endDate)
                        }
                        .map { it.departmentName }
                        .toSet()

                    val assignedDeptNames = permanentAssignedDeptNames + tempAssignedDeptNames
                    if (assignedDeptNames.isEmpty()) emptyList()
                    else {
                        fun departmentMatchesAssigned(deptName: String): Boolean =
                            assignedDeptNames.any { assigned ->
                                deptName.equals(assigned, ignoreCase = true) ||
                                    deptName.trim().endsWith("_$assigned", ignoreCase = true) ||
                                    deptName.trim().substringAfterLast("_", deptName).equals(assigned, ignoreCase = true)
                            }
                        // Collect from each map independently to avoid losing entries for shared dept keys
                        val matching = mutableListOf<String>()
                        userAssignments.filter { (dept, _) -> departmentMatchesAssigned(dept) }.values.forEach { matching.add(it) }
                        approverAssignments.filter { (dept, _) -> departmentMatchesAssigned(dept) }.values.forEach { matching.add(it) }
                        matching.distinct()
                    }
                } else {
                    // BUSINESS_HEAD: collect from each map independently — same dept can have both user + approver
                    (userAssignments.values + approverAssignments.values).distinct()
                }
            }
        }

        // Fetch count whenever the actual phone-number list content changes.
        // Re-throws CancellationException so a restart never wipes the displayed count.
        LaunchedEffect(teamMemberPhoneNumbers) {
            if (teamMemberPhoneNumbers.isEmpty()) {
                activeTeamMembersCount = 0
                return@LaunchedEffect
            }
            try {
                val allUsers = authViewModel.getUsersByPhoneNumbers(teamMemberPhoneNumbers)
                activeTeamMembersCount = allUsers.count { it.isActive }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e  // Never swallow cancellation — keeps stale count visible
            } catch (e: Exception) {
                Log.e("ApproverProjectDashboard", "Error counting team members: ${e.message}")
                // Keep whatever count is already displayed rather than flashing 0
            }
        }

        // Load full member list only when the dialog is opened.
        // Key is showTeamMembersDialog only — teamMemberPhoneNumbers is read at call time,
        // so we avoid a duplicate fetch every time assignments change in the background.
        LaunchedEffect(showTeamMembersDialog) {
            if (!showTeamMembersDialog) return@LaunchedEffect
            isLoadingTeamMembers = true
            try {
                val allUsers = authViewModel.getUsersByPhoneNumbers(teamMemberPhoneNumbers)
                val members = allUsers.filter { it.isActive }
                teamMembersList = members
                activeTeamMembersCount = members.size
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ApproverProjectDashboard", "Error loading team members: ${e.message}")
                teamMembersList = emptyList()
            } finally {
                isLoadingTeamMembers = false
            }
        }
        
        Card(
            modifier = Modifier
                .weight(1f)
                .height(teamMembersCardHeight)
                .widthIn(min = 140.dp)
                .clickable { showTeamMembersDialog = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Navigation arrow in top-right - improved styling
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "View Team Members",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(responsiveIconSize(18.dp, 19.dp, 20.dp))
                        .padding(responsiveSpacing() / 2)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(teamMembersCardPadding),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Icon at the top - improved styling
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Team Members",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(teamMembersIconSize)
                    )
                    
                    // Bottom section - Number and Label
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Number
                        Text(
                            text = activeTeamMembersCount.toString(),
                            fontSize = teamMembersNumberSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Visible
                        )
                        
                        Spacer(modifier = Modifier.height(responsiveSpacing() / 2))
                        
                        // Label - allow wrapping on small screens
                        Text(
                            text = "Team Members",
                            fontSize = teamMembersLabelSize,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Visible,
                            lineHeight = teamMembersLabelSize * 1.2f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        val userRole = authState.user?.role

        // Team Members Dialog - Using TeamMembersDetailScreen
        if (showTeamMembersDialog) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            
            ModalBottomSheet(
                onDismissRequest = { showTeamMembersDialog = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = null
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.95f)
                ) {
                    com.cubiquitous.tracura.ui.view.businesshead.TeamMembersDetailScreen(
                        projectID = projectId,
                        customerID = null,
                        userRole = userRole,
                        currentUserPhone = authState.user?.phone,
                        onBackClick = { showTeamMembersDialog = false },
                        onAddMemberClick = {
                            showTeamMembersDialog = false
                            showAddTeamMemberSheet = true
                        },
                        onMemberClick = {},
                        showExpensesInternally = true,
                        initialSelectedMember = null
                    )
                }
            }
        }
        
        // Add Team Member Sheet
        if (showAddTeamMemberSheet && projectBudgetSummary.project != null) {
            val currentTeamMemberPhones = teamMemberPhoneNumbers.toSet()
            val managerPhone = projectBudgetSummary.project?.managerId ?: ""
            
            // Filter out already selected team members and current project manager
            val unselectedUsers = availableUsers.filter { 
                it.phone !in currentTeamMemberPhones && it.phone != managerPhone 
            }
            val unselectedApprovers = availableApprovers.filter { 
                it.phone !in currentTeamMemberPhones && it.phone != managerPhone 
            }
            
            AddTeamMemberSheet(
                availableUsers = unselectedUsers,
                availableApprovers = unselectedApprovers,
                onDismiss = { showAddTeamMemberSheet = false },
                onAddMember = { phone ->
                    coroutineScope.launch {
                        try {
                            // Get current project
                            val project = currentProject ?: projectBudgetSummary.project ?: return@launch
                            
                            // Get current team members
                            val currentTeamMembers = project.teamMembers.toMutableList()
                            
                            // Add new member if not already present
                            if (!currentTeamMembers.contains(phone)) {
                                currentTeamMembers.add(phone)
                                
                                // Update project using ProjectRepository
                                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val tempApproverRepo = com.cubiquitous.tracura.repository.TemporaryApproverRepository(firestore, dagger.Lazy { throw IllegalStateException("Not needed") })
                                val authRepo = com.cubiquitous.tracura.repository.AuthRepository(firestore)
                                val projectRepo = com.cubiquitous.tracura.repository.ProjectRepository(
                                    firestore,
                                    tempApproverRepo,
                                    authRepo,
                                    context.applicationContext,
                                )
                                
                                // Find customer ID for project
                                val customerId = projectRepo.findCustomerIdForProject(projectId)
                                if (customerId != null) {
                                    val projectRef = firestore.collection("customers")
                                        .document(customerId)
                                        .collection("projects")
                                        .document(projectId)
                                    
                                    projectRef.update("teamMembers", currentTeamMembers).await()
                                    
                                    // Reload project data
                                    approverProjectViewModel.loadProjectBudgetSummary(projectId, forceRefresh = true)
                                    
                                    // Close the add member sheet
                                    showAddTeamMemberSheet = false
                                    
                                    // Show success message
                                    android.widget.Toast.makeText(
                                        context,
                                        "Team member added successfully",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ApproverProjectDashboard", "Error adding team member: ${e.message}")
                            android.widget.Toast.makeText(
                                context,
                                "Error adding team member: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }
        
        // User Expenses Bottom Sheet
        selectedUser?.let { user ->
            if (showUserExpensesSheet) {
                UserExpensesBottomSheet(
                    user = user,
                    projectId = projectBudgetSummary.project?.id ?: "",
                    onDismiss = {
                        showUserExpensesSheet = false
                        selectedUser = null
                    }
                )
            }
        }
        
        // Phases Card - Dynamic count (showing number of phases)
        val phasesCardHeight = remember(configuration.screenWidthDp) {
            when {
                configuration.screenWidthDp < 360 -> 100.dp
                configuration.screenWidthDp < 480 -> 105.dp
                else -> 110.dp
            }
        }
        val phasesCardPadding = responsivePadding()
        val phasesIconSize = responsiveIconSize(24.dp, 26.dp, 28.dp)
        val phasesNumberSize = responsiveFontSize(24.sp, 26.sp, 28.sp, 30.sp)
        val phasesLabelSize = responsiveFontSize(12.sp, 13.sp, 14.sp, 15.sp)
        var showAllPhases by remember { mutableStateOf(false) }

        val initialHighlightTag = remember(fromNotification, highlightKey) {
            normalizeExpenseListNotificationTag(fromNotification)
        }
        var activeHighlightTag by remember(initialHighlightTag) {
            mutableStateOf(initialHighlightTag)
        }



        LaunchedEffect(initialHighlightTag, allPhases) {
            activeHighlightTag = initialHighlightTag
            if ((activeHighlightTag == "PhaseStartHighlighting" || activeHighlightTag == "PhaseEndDateChangeHighlighting" )  && allPhases.isNotEmpty()) {
                //delay(2000L)
                showAllPhases = true // Automatically open phases sheet if notification is about phases update
                activeHighlightTag = ""
            }
        }

        val rowHighlightColor by animateColorAsState(
            targetValue = if (activeHighlightTag.isNotEmpty()) Color(0xFFAF52DE).copy(alpha = 0.2f) else Color.Transparent,
            animationSpec = tween(durationMillis = 1000),
            label = "RowHighlightAnimation"
        )
        
        val activePhasesCount = remember(
            phaseBreakdowns,
            authState.user?.role,
            authState.user?.phone,
            currentProject,
            selectedProject
        ) {
            val role = authState.user?.role
            val userPhone = normalizePhoneForCompare(authState.user?.phone)
            val project = currentProject ?: selectedProject

            if (role == UserRole.APPROVER && userPhone.isNotBlank() && project != null) {
                val today = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.time

                val assignedDepartments = (
                    project.departmentApproverAssignments
                        .filter { (_, phone) -> normalizePhoneForCompare(phone) == userPhone }
                        .keys +
                        project.departmentTemporaryApprover
                            .filter { entry -> isActiveTempApproverForDate(entry, userPhone, today) }
                            .map { it.departmentName }
                    ).toSet()

                if (assignedDepartments.isEmpty()) {
                    0
                } else {
                    phaseBreakdowns.count { phase ->
                        phase.departments.any { dept ->
                            val deptName = if (dept.department.contains("_")) {
                                dept.department.substringAfter("_")
                            } else {
                                dept.department
                            }
                            matchesAssignedDepartment(deptName, assignedDepartments)
                        }
                    }
                }
            } else {
                phaseBreakdowns.size
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .height(phasesCardHeight)
                .widthIn(min = 140.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp),
            onClick = {
                // View All Phases button - ALWAYS visible if there are any phases (enabled or disabled)
                if (allPhases.isNotEmpty()) {
                    showAllPhases = true
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = if (activeHighlightTag == "NewPhaseupdate")  rowHighlightColor else Color.Transparent , shape = RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon at the top - improved styling
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Phases",
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(phasesIconSize)
                )
                
                // Bottom section - Number and Label
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Number - showing phases count
                    Text(
                        text = activePhasesCount.toString(),
                        fontSize = phasesNumberSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Visible
                    )
                    
                    Spacer(modifier = Modifier.height(responsiveSpacing() / 2))
                    
                    // Label - allow wrapping on small screens
                    Text(
                        text = "Active Phases",
                        fontSize = phasesLabelSize,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Visible,
                        lineHeight = phasesLabelSize * 1.2f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }



        if (showAllPhases) {
            var showAddPhaseSheet by remember { mutableStateOf(false) }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val windowHeight = LocalConfiguration.current.screenHeightDp.dp
            val iosPalette = rememberIosDashboardPalette()

            ModalBottomSheet(
                onDismissRequest = { showAllPhases = false },
                containerColor = iosPalette.tier2Surface,
                sheetState = sheetState,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                color = iosPalette.hairline,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                },
                modifier = Modifier.fillMaxHeight()
            ) {
                val navigateToDeptDetail: (String, String, Double?, Double?, Double?) -> Unit = onNavigateToDepartmentDetailWrapper

                // Use phasesWithDepartments for "View All Phases" - show ALL phases regardless of isEnabled
                // This ensures we get departments from subcollection with proper budgets
                // Use phaseDepartmentSpentMap for accurate department spent amounts (proper department key matching)
                val allPhaseBreakdowns: List<PhaseBreakdown> = remember(phasesWithDepartments, phaseBudgetMap, phaseDepartmentSpentMap, allPhases) {
                    phasesWithDepartments.map { phaseWithDepts ->
                        val phase = phaseWithDepts.phase

                        // Get department spent map for this phase from ViewModel (uses proper department key matching)
                        val phaseDeptSpentMap = phaseDepartmentSpentMap[phase.id] ?: emptyMap()

                        // Build breakdown from departments in subcollection
                        // Use getDepartmentSpentAmount for proper department key matching
                        val breakdown = phaseWithDepts.departments.map { dept ->
                            // Use ViewModel's getDepartmentSpentAmount for accurate spent calculation
                            // This handles department key matching correctly (phaseId_departmentName vs departmentName)
                            val spent = approverProjectViewModel.getDepartmentSpentAmount(phase.id, dept.name)

                            val remaining = dept.totalBudget - spent
                            val usedPercentage = if (dept.totalBudget > 0) (spent / dept.totalBudget) * 100 else 0.0

                            com.cubiquitous.tracura.model.DepartmentBudgetBreakdown(
                                department = dept.name,
                                budgetAllocated = dept.totalBudget,
                                spent = spent,
                                remaining = remaining,
                                percentage = usedPercentage
                            )
                        }.sortedByDescending { it.spent }

                        com.cubiquitous.tracura.model.PhaseBreakdown(
                            phaseId = phase.id,
                            phaseTitle = if (phase.phaseName.isNotEmpty()) phase.phaseName else "Phase ${phase.phaseNumber}",
                            startDate = phase.startDate,
                            endDate = phase.endDate,
                            departments = breakdown,
                            isActive = phase.isEnabledValue
                        )
                    }
                }

                // Filter departments for APPROVER role (same logic as main dashboard)
                val userRole = authState.user?.role
                val userPhone = authState.user?.phone?.replace("+91", "")?.trim()
                val project = currentProject ?: selectedProject
                val canActAsApproverInAllPhases = if (userPhone != null && project != null) {
                    userRole == UserRole.APPROVER || project.departmentTemporaryApprover.any { entry ->
                        val todayAll = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.time
                        isActiveTempApproverForDate(entry, userPhone, todayAll)
                    }
                } else {
                    false
                }

                val assignedDepartmentsForAllPhases = if (canActAsApproverInAllPhases && userPhone != null && project != null) {
                    val todayAll = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
                    }.time
                    val permanentAll = project.departmentApproverAssignments
                        .filter { (_, approverPhone) ->
                            approverPhone.replace("+91", "").trim() == userPhone
                        }
                        .keys.toSet()
                    val tempAll = project.departmentTemporaryApprover
                        .filter { entry ->
                            isActiveTempApproverForDate(entry, userPhone, todayAll)
                        }
                        .map { it.departmentName }.toSet()
                    permanentAll + tempAll
                } else {
                    null // null means show all departments (for BUSINESS_HEAD)
                }

                // Filter each phase's departments if user is APPROVER
                val filteredAllPhaseBreakdowns: List<PhaseBreakdown> = remember(allPhaseBreakdowns, assignedDepartmentsForAllPhases) {
                    if (assignedDepartmentsForAllPhases != null) {
                        allPhaseBreakdowns.mapNotNull { pb ->
                            val filteredDepts = pb.departments.filter { dept ->
                                val deptName = if (dept.department.contains("_")) {
                                    dept.department.substringAfter("_")
                                } else {
                                    dept.department
                                }
                                matchesAssignedDepartment(
                                    deptName,
                                    assignedDepartmentsForAllPhases
                                )
                            }
                            // Only include phase if it has at least one assigned department
                            if (filteredDepts.isNotEmpty()) {
                                pb.copy(departments = filteredDepts)
                            } else {
                                null
                            }
                        }
                    } else {
                        allPhaseBreakdowns // Show all for BUSINESS_HEAD
                    }
                }

                // Store edit phase sheet states - use Set for better recomposition
                val editPhaseSheetIndices = remember { mutableStateOf(setOf<Int>()) }

                LaunchedEffect(fromNotification, showAllPhases) { // Or whatever variable triggers this logic
                    // 1. Find the index of "Phase 9" in your data list
                    // (Using equals(..., ignoreCase = true) is safer just in case it comes as "phase 9")
                    val dataIndex = filteredAllPhaseBreakdowns.indexOfFirst { 
                        notificationMessage.contains(it.phaseTitle)
                        //Log.d("chekcingValuedataIndex","checking ${it.phaseTitle} contains: ${notificationMessage.contains(it.phaseTitle)}")
                    }

                    debugLog("chekcingValuedataIndex") { "dataIndex $dataIndex , notificationMessage $notificationMessage , fromNotification $fromNotification" }

                    
                    // 2. If it exists in the list, scroll to it!
                    if (dataIndex != -1) {
                        // Add + 1 to account for the stickyHeader at index 0
                        val scrollTargetIndex = dataIndex + 1 
                        
                        // Optional delay to ensure the list is drawn before scrolling
                        kotlinx.coroutines.delay(300) 
                        
            
                               
                        // 2. Pass the negative offset to push the item down from the top edge
                        listState.animateScrollToItem(
                            index = scrollTargetIndex,
                            scrollOffset = -headerOffsetPx // <-- THIS is the magic fix
                        )
    
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(windowHeight * 0.9f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sticky Header with Back button (left) and Add Phase button (right)
                    stickyHeader {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(iosPalette.tier2Surface)
                                .border(1.dp, iosPalette.hairline)
                                .padding(horizontal = 0.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back button - improved styling
                            IconButton(
                                onClick = { showAllPhases = false },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = iosPalette.textPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Title
                            Text(
                                text = "All Phases",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosPalette.textPrimary,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )

                            // Add Phase button (for Business Head and Manager)
                            if (authState.user?.role == UserRole.BUSINESS_HEAD || authState.user?.role == UserRole.MANAGER) {
                                IconButton(
                                    onClick = { showAddPhaseSheet = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Phase",
                                        tint = iosPalette.accent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            } else {
                                // Spacer to maintain layout when button is not shown
                                Spacer(modifier = Modifier.size(48.dp))
                            }
                        }
                    }

                    items(
                        items = filteredAllPhaseBreakdowns,
                        key = { it.phaseId }
                    ) { pbAll ->
                        val idx2 = filteredAllPhaseBreakdowns.indexOf(pbAll)
                        // Check if this phase is ongoing
                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        val today = java.util.Calendar.getInstance().time
                        val isPhaseOngoing = try {
                            val start = pbAll.startDate?.let { sdf.parse(it) }
                            val end = pbAll.endDate?.let { sdf.parse(it) }
                            val startsOk = start == null || !today.before(start)
                            val endsOk = end == null || !today.after(end)
                            startsOk && endsOk
                        } catch (_: Exception) { false }

                        PhaseBudgetsSection(
                            title = pbAll.phaseTitle.ifEmpty { "Phase ${idx2 + 1}" },
                            fromNotification = if (notificationMessage.contains(pbAll.phaseTitle)) fromNotification else "",
                            notificationMessage = notificationMessage,
                            departments = pbAll.departments,
                            onNavigateToDepartmentDetail = { deptProjectId: String, dept: String, budgetAllocated: Double?, spent: Double?, remaining: Double? ->
                                // Department name is now directly from Department.name (no need for ID mapping)
                                // Construct full format: phaseId_departmentName for navigation
                                val fullDeptName = if (dept.contains("_") && !dept.startsWith("Others_")) {
                                    // Already in full format, verify it matches current phase
                                    if (dept.startsWith("${pbAll.phaseId}_")) {
                                        dept // Already in correct full format
                                    } else {
                                        // Extract the display name and reconstruct for current phase
                                        val displayName = dept.substringAfter("_")
                                        "${pbAll.phaseId}_$displayName"
                                    }
                                } else {
                                    // It's a display name, construct full format
                                    "${pbAll.phaseId}_$dept"
                                }
                                // Pass budget values to avoid redundant fetching
                                navigateToDeptDetail(deptProjectId, fullDeptName, budgetAllocated, spent, remaining)
                            },
                            onAddDepartment = { departmentDraft ->
                                // Delegate to ViewModel to update Firestore with full department details
                                approverProjectViewModel.addDepartmentWithDetailsToPhase(
                                    projectId = projectId,
                                    phaseId = pbAll.phaseId,
                                    departmentDraft = departmentDraft
                                )
                            },
                            showAddButton = false,
                            startDate = pbAll.startDate,
                            endDate = pbAll.endDate,
                            isOngoing = isPhaseOngoing,
                            phaseId = pbAll.phaseId,
                            isActive = pbAll.isActive,
                            onToggleActive = { active ->
                                approverProjectViewModel.updatePhaseEnabled(projectId, pbAll.phaseId, active)
                            },
                            userRole = authState.user?.role,
                            projectId = projectId,
                            expenseRepository = null, // Using ViewModel's expense data instead
                            showEditButton = authState.user?.role == UserRole.BUSINESS_HEAD || authState.user?.role == UserRole.MANAGER,
                            onEditClick = {
                                editPhaseSheetIndices.value = editPhaseSheetIndices.value + idx2
                            },
                            showDaysRemaining = false,
                            approverProjectViewModel = approverProjectViewModel,
                            phasesWithDepartmentsData = phasesWithDepartments,
                            phaseDepartmentSpentMapData = phaseDepartmentSpentMap,
                            phaseAnonymousExpensesMapData = phaseAnonymousExpensesMap,
                            allPhasesData = allPhases,
                            plannedStartDate = selectedProject?.startDate // Pass project's planned start date
                        )
                    }

                    // Bottom spacing
                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Edit Phase Bottom Sheets (outside LazyColumn)
                filteredAllPhaseBreakdowns.forEachIndexed { idx2, pbAll ->
                    if (editPhaseSheetIndices.value.contains(idx2)) {
                        EditPhaseBottomSheet(
                            projectId = projectId,
                            phaseId = pbAll.phaseId,
                            currentPhaseName = pbAll.phaseTitle.ifEmpty { "Phase ${idx2 + 1}" },
                            currentStartDate = pbAll.startDate,
                            currentEndDate = pbAll.endDate,
                            onDismiss = {
                                editPhaseSheetIndices.value = editPhaseSheetIndices.value - idx2
                            },
                            onPhaseUpdated = {
                                editPhaseSheetIndices.value = editPhaseSheetIndices.value - idx2
                                approverProjectViewModel.loadProjectBudgetSummary(projectId, forceRefresh = true)
                            },
                            approverProjectViewModel = approverProjectViewModel
                        )
                    }
                }
            }

            // Add Phase Bottom Sheet
            if (showAddPhaseSheet) {
                AddPhaseBottomSheet(
                    projectId = projectId,
                    onDismiss = { showAddPhaseSheet = false },
                    onPhaseAdded = {
                        showAddPhaseSheet = false
                        approverProjectViewModel.loadProjectBudgetSummary(projectId, forceRefresh = true)
                    },
                    approverProjectViewModel = approverProjectViewModel
                )
            }
        }
    }

    // ── Delegate Preview Section (outside the cards Row) ─────────────────────
    val delegationPreviewItems = remember(
        authState.user?.role,
        authState.user?.phone,
        currentProject,
        selectedProject
    ) {
        val project = currentProject ?: selectedProject
        val allDelegations = project?.departmentTemporaryApprover ?: emptyList()
        val role = authState.user?.role
        val userPhone = normalizePhoneForCompare(authState.user?.phone)

        if (role != UserRole.APPROVER || userPhone.isBlank() || project == null) {
            allDelegations
        } else {
            val today = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time

            val permanentDeptNames = project.departmentApproverAssignments
                .filter { (_, phone) ->
                    normalizePhoneForCompare(phone) == userPhone
                }
                .keys
                .toSet()

            val temporaryDeptNames = project.departmentTemporaryApprover
                .filter { entry ->
                    isActiveTempApproverForDate(entry, userPhone, today)
                }
                .map { it.departmentName }
                .toSet()

            debugLog("projectTempApprovercheck") { "temp approver department name : $temporaryDeptNames and approver department name : $permanentDeptNames" }

            val approverRelevantDepartments = permanentDeptNames + temporaryDeptNames
            if (approverRelevantDepartments.isEmpty()) {
                emptyList()
            } else {
                fun isRelevantDepartment(departmentName: String): Boolean =
                    approverRelevantDepartments.any { assigned ->
                        departmentName.equals(assigned, ignoreCase = true) ||
                            departmentName.trim().endsWith("_$assigned", ignoreCase = true) ||
                            departmentName.trim().substringAfterLast("_").equals(assigned, ignoreCase = true)
                    }

                allDelegations.filter { delegation ->
                    isRelevantDepartment(delegation.departmentName)
                }
            }
        }
    }

    if (delegationPreviewItems.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        DelegatePreviewSection(
            delegates = delegationPreviewItems,
            isInteractive = authState.user?.role != UserRole.APPROVER,
            fromNotification = fromNotification,
            highlightKey = highlightKey,
            bringIntoViewRequester = delegationBringIntoViewRequester,
            onClick = onNavigateToDelegation
        )
    }

    LaunchedEffect(delegationCenterScrollPending, delegationPreviewItems) {
        if (delegationCenterScrollPending && delegationPreviewItems.isNotEmpty()) {
            delay(300L)
            delegationBringIntoViewRequester.bringIntoView()
            delegationCenterScrollPending = false
        }
    }
}

@Composable
private fun DynamicOverviewCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String,
    subtitle: String?,
    onClick: (() -> Unit)? = null,
    subtitleColor: Color = Color.Gray,
    containerColor: Color = Color.White
) {
    val iosPalette = rememberIosDashboardPalette()
    val cardPadding = responsivePadding()
    val configuration = LocalConfiguration.current
    val cardHeight = remember(configuration.screenWidthDp) {
        when {
            configuration.screenWidthDp < 360 -> 100.dp
            configuration.screenWidthDp < 480 -> 105.dp
            else -> 110.dp
        }
    }
    
    Card(
        modifier = modifier
            .height(cardHeight)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(cardPadding),
            verticalArrangement = Arrangement.Center
        ) {
            // Main amount at the top - responsive font size based on screen size
            val screenWidthDp = configuration.screenWidthDp
            
            // Calculate responsive font size: base size scales with screen width
            // For small screens (< 360dp): use smaller font, for larger screens: scale up
            val valueFontSize = remember(screenWidthDp) {
                when {
                    screenWidthDp < 360 -> 20.sp
                    screenWidthDp < 480 -> 24.sp
                    screenWidthDp < 600 -> 28.sp
                    screenWidthDp < 840 -> 32.sp
                    else -> 36.sp
                }
            }
            val minValueFontSize = remember(valueFontSize) {
                max(12f, valueFontSize.value * 0.6f).sp
            }
            val labelFontSize = responsiveFontSize(12.sp, 13.sp, 14.sp, 15.sp)
            val subtitleFontSize = responsiveFontSize(11.sp, 12.sp, 13.sp, 14.sp)
            val minSubtitleFontSize = remember(subtitleFontSize) {
                max(10f, subtitleFontSize.value * 0.7f).sp
            }
            
            AutoSizeText(
                text = value,
                maxFontSize = valueFontSize,
                minFontSize = minValueFontSize,
                fontWeight = FontWeight.Bold,
                color = iosPalette.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(responsiveSpacing() / 2))
            
            // Label in the middle
            Text(
                text = label,
                fontSize = labelFontSize,
                fontWeight = FontWeight.Normal,
                color = iosPalette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Visible,
                lineHeight = labelFontSize * 1.2f,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            // Remaining amount at the bottom
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(responsiveSpacing() / 2))
                AutoSizeText(
                    text = subtitle,
                    maxFontSize = subtitleFontSize,
                    minFontSize = minSubtitleFontSize,
                    fontWeight = FontWeight.Normal,
                    color = subtitleColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhaseBudgetsSection(
    title: String,
    fromNotification: String = "",
    notificationMessage: String = "",
    highlightKey: Int = 0,
    departments: List<DepartmentBudgetBreakdown>,
    onNavigateToDepartmentDetail: (String, String, Double?, Double?, Double?) -> Unit,
    onAddDepartment: (DepartmentDraft) -> Unit,
    showAddButton: Boolean = true,
    verticalList: Boolean = false,
    startDate: String? = null,
    endDate: String? = null,
    isOngoing: Boolean = false,
    phaseId: String,
    isActive: Boolean = true,
    onToggleActive: (Boolean) -> Unit = {},
    userRole: UserRole? = null,
    projectId: String = "",
    expenseRepository: ExpenseRepository? = null,
    showEditButton: Boolean = false,
    onEditClick: () -> Unit = {},
    showDaysRemaining: Boolean = true,
    approverProjectViewModel: ApproverProjectViewModel? = null,
    phasesWithDepartmentsData: List<com.cubiquitous.tracura.model.PhaseWithDepartments> = emptyList(),
    phaseDepartmentSpentMapData: Map<String, Map<String, Double>> = emptyMap(),
    phaseAnonymousExpensesMapData: Map<String, Double> = emptyMap(),
    allPhasesData: List<com.cubiquitous.tracura.model.Phase> = emptyList(),
    plannedStartDate: String? = null // Project's planned start date
) {
    val context = LocalContext.current
    val iosPalette = rememberIosDashboardPalette()
    val configuration = LocalConfiguration.current
    // Cell width = screen width minus horizontal padding (Material 16.dp each side) so content doesn't clip
    val horizontalPaddingDp = 32
    val departmentCellWidth = (configuration.screenWidthDp - horizontalPaddingDp).coerceAtLeast(280).dp
    var showAddDeptSheet by remember { mutableStateOf(false) }
    var newDeptName by remember { mutableStateOf("") }
    var newDeptContractorMode by remember { mutableStateOf(ContractorMode.LABOUR_ONLY) }
    var newDeptLineItems by remember { mutableStateOf(mutableListOf<LineItem>()) }
    
    // Available users and approvers for department assignment
    var availableUsers by remember { mutableStateOf<List<com.cubiquitous.tracura.model.User>>(emptyList()) }
    var availableApprovers by remember { mutableStateOf<List<com.cubiquitous.tracura.model.User>>(emptyList()) }
    
    // Load users and approvers when dialog opens
    LaunchedEffect(showAddDeptSheet) {
        if (showAddDeptSheet) {
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentCustomerId = firebaseUser?.uid
                
                if (currentCustomerId != null) {
                    val authRepository = com.cubiquitous.tracura.repository.AuthRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance())
                    val allUsers = authRepository.getAllUsers()
                    
                    // Filter users by current customer ID
                    val customerUsers = allUsers.filter { user ->
                        user.customerId == currentCustomerId || (user.customerId == null && user.uid == currentCustomerId)
                    }
                    
                    // Filter active users and approvers
                    val regularUsers = customerUsers.filter { 
                        it.role == com.cubiquitous.tracura.model.UserRole.USER && (it.isActive == true || it.isActive) 
                    }
                    val approvers = customerUsers.filter { 
                        it.role == com.cubiquitous.tracura.model.UserRole.APPROVER && (it.isActive == true || it.isActive) 
                    }
                    
                    availableUsers = regularUsers
                    availableApprovers = approvers
                }
            } catch (e: Exception) {
                android.util.Log.e("PhaseBudgetsSection", "Error loading users: ${e.message}")
            }
        }
    }
    
    // Dialog states for Start Phase and proof-upload / viewer sheet state
    var showStartPhaseDialog by remember { mutableStateOf(false) }
    var phaseIdForDialog by remember { mutableStateOf("") }
    // Proof upload bottom sheet
    var showProofUploadSheet by remember { mutableStateOf(false) }
    var phaseForProofUpload by remember { mutableStateOf<com.cubiquitous.tracura.model.Phase?>(null) }
    // Proof viewer bottom sheet
    var showProofViewerSheet by remember { mutableStateOf(false) }
    var proofUrlToView by remember { mutableStateOf<String?>(null) }
    // ViewModel for proof upload
    val phaseCompletionViewModel: PhaseCompletionViewModel = hiltViewModel()
    
    // Get today's date formatted
    val dateFormatter = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
    val todayFormatted = remember {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        dateFormatter.format(calendar.time)
    }
    
    // Check if phase has an approved override request
    var hasApprovedRequest by remember { mutableStateOf(false) }
    
    LaunchedEffect(phaseId, projectId) {
        if (projectId.isNotEmpty() && phaseId.isNotEmpty() && approverProjectViewModel != null) {
            try {
                hasApprovedRequest = approverProjectViewModel.hasApprovedPhaseRequest(projectId, phaseId)
            } catch (e: Exception) {
                hasApprovedRequest = false
            }
        }
    }
    
    // Use passed data from parent to avoid repeated Flow collectors per phase card
    val phasesWithDepartments = phasesWithDepartmentsData
    val phaseWithDepts = phasesWithDepartments.firstOrNull { it.phase.id == phaseId }
    val departmentsFromFirebase = phaseWithDepts?.departments ?: emptyList()
    
    // Get spent calculations from ViewModel (uses proper department key matching)
    val phaseDepartmentSpentMap = phaseDepartmentSpentMapData
    val phaseAnonymousExpensesMap = phaseAnonymousExpensesMapData
    
    // Transform departments from Firebase to DepartmentCellData
    // Use the new approved spent calculation logic with proper department key matching
    // IMPORTANT: Filter departmentsFromFirebase to match the filtered departments passed in (for APPROVER role filtering)
    val filteredDepartmentNames = remember(departments) {
        departments.map { it.department }.toSet()
    }
    
    debugLog("DeptFilter") { "PhaseBudgetsSection - Phase: $phaseId" }
    debugLog("DeptFilter") { "PhaseBudgetsSection - Filtered department names: $filteredDepartmentNames" }
    debugLog("DeptFilter") { "PhaseBudgetsSection - All departments from Firebase: ${departmentsFromFirebase.map { it.name }}" }
    
    val filteredDepartmentsFromFirebase = remember(departmentsFromFirebase, filteredDepartmentNames) {
        if (filteredDepartmentNames.isNotEmpty()) {
            departmentsFromFirebase.filter { dept ->
                // Match department names - handle both "phaseId_deptName" and "deptName" formats
                val deptName = if (dept.name.contains("_")) {
                    dept.name.substringAfter("_")
                } else {
                    dept.name
                }
                val isIncluded = filteredDepartmentNames.contains(dept.name) || filteredDepartmentNames.contains(deptName)
                debugLog("DeptFilter") { "PhaseBudgetsSection - Dept: ${dept.name}, Extracted: $deptName, Included: $isIncluded" }
                isIncluded
            }
        } else {
            departmentsFromFirebase
        }
    }
    
    debugLog("DeptFilter") { "PhaseBudgetsSection - Filtered departments result: ${filteredDepartmentsFromFirebase.map { it.name }}" }
    
    val departmentCellDataList = remember(filteredDepartmentsFromFirebase, phaseDepartmentSpentMap, phaseId) {
        filteredDepartmentsFromFirebase.map { dept ->
            // Use getDepartmentSpentAmount for proper department key matching
            val spentAmount = if (approverProjectViewModel != null) {
                approverProjectViewModel.getDepartmentSpentAmount(phaseId, dept.name)
            } else {
                0.0
            }
            
            transformDepartmentForCell(dept, spentAmount)
        }
    }
    
    // Get phase data to find anonymous departments for this specific phase
    val allPhases = allPhasesData
    
    // Get the current phase to find its anonymous departments
    val currentPhase = allPhases.firstOrNull { it.id == phaseId }
    val phaseAnonymousDepartments = remember(currentPhase) {
        currentPhase?.isanonymous?.filter { it.value == true }?.keys?.toList() ?: emptyList()
    }
    
    val approverDepartment = remember(userRole, filteredDepartmentsFromFirebase) {
        if (userRole == UserRole.APPROVER) {
            filteredDepartmentsFromFirebase.firstOrNull()
        } else {
            null
        }
    }

    // Use phase totals from the phase document to avoid scanning expenses
    val storedTotalBudget = currentPhase?.totalBudget ?: 0.0
    val totalPhaseBudget = if (approverDepartment != null) {
        approverDepartment.totalBudget
    } else if (storedTotalBudget > 0.0) {
        storedTotalBudget
    } else if (filteredDepartmentsFromFirebase.isNotEmpty()) {
        filteredDepartmentsFromFirebase.sumOf { it.totalBudget }
    } else {
        departments.sumOf { it.budgetAllocated }
    }
    
    // Calculate total spent from only APPROVED expenses (for "Others" spent)
    // Use phaseAnonymousExpensesMap for accurate anonymous expenses calculation
    val othersSpent = remember(phaseAnonymousExpensesMap, phaseId) {
        phaseAnonymousExpensesMap[phaseId] ?: 0.0
    }
    
    // Spent = totalBudget - remainingBudget (default remaining to 0 if missing)
    val remainingAmount = if (approverDepartment != null) {
        approverDepartment.remainingAmount
    } else {
        (currentPhase?.remainingBudget ?: 0.0).coerceAtLeast(0.0)
    }
    val phaseExpensesTotal = if (approverDepartment != null) {
        totalPhaseBudget - remainingAmount
    } else {
        (totalPhaseBudget - remainingAmount).coerceAtLeast(0.0)
    }

    debugLog("PhaseBudgetforRemaining") { "PhaseBudgetsSection - Phase ID: $phaseId, Total Budget: $totalPhaseBudget, Remaining: $remainingAmount, Calculated Spent: $phaseExpensesTotal, Others Spent: $othersSpent" }
    
    // Check if phase should be in completed state
    // Phase is completed if:
    // 1. isEnabled is false (manually completed via "Complete Phase" action), OR
    // 2. today >= end date (end date is today or has passed)
    // Include allPhases in remember key to ensure recalculation when phases are updated
    // Use currentPhase?.endDate instead of endDate prop to get the latest value from allPhases
    val isPhaseCompleted = remember(allPhases, currentPhase?.isEnabled, currentPhase?.endDate) {
        // First check if phase is disabled (manually completed)
        if (currentPhase?.isEnabled == false) {
            return@remember true
        }
        
        // Then check if end date has passed - use currentPhase?.endDate to get latest value
        val phaseEndDate = currentPhase?.endDate ?: endDate
        if (phaseEndDate.isNullOrBlank()) {
            false // No end date means not completed
        } else {
            try {
                val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val endDateObj = dateFormatter.parse(phaseEndDate)
                val today = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.time
                
                // Phase is completed if today >= end date (today equals or after end date)
                // This handles the case where "Complete Phase" sets end date to today
                // Compare using timeInMillis since both dates are normalized to midnight
                endDateObj != null && today.time >= endDateObj.time
            } catch (e: Exception) {
                false
            }
        }
    }
    
    // Toggle should be disabled if phase is completed (today > end date)
    val isToggleEnabled = !isPhaseCompleted
    
    // Check if phase start date is in the future (greater than current date)
    // Show three dots menu with "Start Now" option if phase start date is in the future
    val showStartNowOption = remember(startDate) {
        if (startDate == null) {
            false
        } else {
            try {
                val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val phaseStartDateObj = dateFormatter.parse(startDate)
                val today = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.time
                
                // Phase start date must be in the future (after today)
                phaseStartDateObj != null && today.before(phaseStartDateObj)
            } catch (e: Exception) {
                false
            }
        }
    }
    val shouldShowPlannedBadge = !isPhaseCompleted && showStartNowOption
    val canManagePhaseProgress = userRole == UserRole.BUSINESS_HEAD || userRole == UserRole.MANAGER

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Start Phase Now Dialog
        if (showStartPhaseDialog) {
            AlertDialog(
                onDismissRequest = { showStartPhaseDialog = false },
                title = {
                    Text(
                        text = "Start Phase Now",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "The phase start date will be set to today ($todayFormatted).",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showStartPhaseDialog = false
                            // Execute start phase action
                            if (approverProjectViewModel != null && projectId.isNotEmpty() && phaseIdForDialog.isNotEmpty()) {
                                // Get phase from ViewModel
                                val allPhasesList = approverProjectViewModel.allPhases.value
                                val currentPhase = allPhasesList.firstOrNull { it.id == phaseIdForDialog }
                                if (currentPhase != null) {
                                    approverProjectViewModel.updatePhaseDetails(
                                        projectId = projectId,
                                        phaseId = phaseIdForDialog,
                                        phaseName = currentPhase.phaseName,
                                        startDate = todayFormatted,
                                        endDate = currentPhase.endDate
                                    )
                                } else {
                                    // If phase not found in allPhases, just update the start date
                                    approverProjectViewModel.updatePhaseDetails(
                                        projectId = projectId,
                                        phaseId = phaseIdForDialog,
                                        phaseName = title,
                                        startDate = todayFormatted,
                                        endDate = endDate
                                    )
                                }
                                
                                android.widget.Toast.makeText(
                                    context,
                                    "Phase started. Start date set to today.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Text(
                            text = "Confirm",
                            color = Color(0xFF007AFF),
                            fontSize = 16.sp
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showStartPhaseDialog = false }
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF007AFF),
                            fontSize = 16.sp
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }
        
        // Proof upload bottom sheet
        if (showProofUploadSheet && phaseForProofUpload != null) {
            PhaseProofUploadBottomSheet(
                phase = phaseForProofUpload!!,
                viewModel = phaseCompletionViewModel,
                projectId = projectId,
                onDismiss = {
                    showProofUploadSheet = false
                    phaseForProofUpload = null
                },
                onSuccess = {
                    showProofUploadSheet = false
                    phaseForProofUpload = null
                    android.widget.Toast.makeText(
                        context,
                        "Phase completed successfully.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    approverProjectViewModel?.loadProjectBudgetSummary(projectId, forceRefresh = true)
                }
            )
        }

        // Proof viewer bottom sheet
        if (showProofViewerSheet && proofUrlToView != null) {
            PhaseProofViewerSheet(
                url = proofUrlToView!!,
                onDismiss = {
                    showProofViewerSheet = false
                    proofUrlToView = null
                }
            )
        }

             val initialHighlightTag = remember(fromNotification, highlightKey) {
                            normalizeExpenseListNotificationTag(fromNotification)
                }
            var activeHighlightTag by remember(initialHighlightTag) {
                mutableStateOf(initialHighlightTag)
            }



            LaunchedEffect(initialHighlightTag) {
                activeHighlightTag = initialHighlightTag
                if (activeHighlightTag.isNotEmpty()) {
                    delay(2000L)
                    activeHighlightTag = ""
                }
            }

            val rowHighlightColor by animateColorAsState(
                targetValue = if (activeHighlightTag.isNotEmpty()) iosPalette.accent.copy(alpha = 0.2f) else Color.Transparent,
                animationSpec = tween(durationMillis = 1000),
                label = "RowHighlightAnimation"
            )
            val approvedRemainingCardColor by animateColorAsState(
                targetValue = if (activeHighlightTag == "RemainingBudgetUpdate") iosPalette.accent.copy(alpha = 0.2f) else iosPalette.tier3Field,
                animationSpec = tween(durationMillis = 1000),
                label = "ApprovedRemainingCardHighlight"
            )

        // Single horizontal row: Phase name, days remaining chip, Active/Extended chips, toggle, plus button
        if (showDaysRemaining && endDate != null) {

            
            // Show days remaining in a chip
            val daysRemaining = FormatUtils.calculateDaysLeft(endDate)
            val daysText = when {
                daysRemaining < 0 -> "Expired"
                daysRemaining == 0L -> "Ends today"
                daysRemaining == 1L -> "1 day remaining"
                else -> "$daysRemaining days remaining"
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp),
                   
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Phase name and days remaining stacked vertically
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Phase name - improved styling
                    TruncatedTextWithPopup(
                        text = title,
                        maxLength = 25,
                        fontSize = responsiveFontSize(18.sp, 19.sp, 20.sp, 22.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Days remaining with clock icon - color coded by urgency
                    val daysRemainingColor = when {
                        daysRemaining <= 10 -> MaterialTheme.colorScheme.error
                        daysRemaining > 10 && daysRemaining <= 15 -> MaterialTheme.colorScheme.tertiary
                        else -> iosPalette.accent
                    }
                    
                    // State to store the width of days remaining row
                    var daysRemainingWidth by remember { mutableStateOf(0.dp) }
                    val density = LocalDensity.current
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            daysRemainingWidth = with(density) { coordinates.size.width.toDp() }
                        }
                        .background(
                        color = if (activeHighlightTag.isNotEmpty() && activeHighlightTag != "RemainingBudgetUpdate") rowHighlightColor else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = daysRemainingColor,
                            modifier = Modifier.size(responsiveIconSize(14.dp, 15.dp, 16.dp))
                        )
                        Text(
                            text = daysText,
                            fontSize = responsiveFontSize(12.sp, 13.sp, 14.sp, 15.sp),
                            color = daysRemainingColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Total Budget below days remaining with purple chip label
                    if (totalPhaseBudget > 0 || phaseExpensesTotal > 0) {
                        // Purple background container matching iOS design - width at least matches days remaining, can expand
                        if (daysRemainingWidth > 0.dp) {
                            Box(
                                modifier = Modifier
                                    .widthIn(min = daysRemainingWidth)
                                    .background(
                                        color = if (activeHighlightTag == "RemainingBudgetUpdate") rowHighlightColor else PhaseTotalBackgroundColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                            Row(
                                modifier = Modifier.wrapContentWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // "Total" text with its own background
                                Text(
                                    text = "Total",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PhaseTotalTextColor,
                                    modifier = Modifier
                                        .background(
                                            color = PhaseTotalTextColor.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )

                                // Budget amount - no truncation, full amount displayed
                                AutoSizeText(
                                    text = FormatUtils.formatCurrencyWithoutDecimals(totalPhaseBudget),
                                    maxFontSize = 14.sp,
                                    minFontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PhaseTotalTextColor
                                )
                            }
                            }
                        }
                    }
                    
                }
                
                // Right side: Active chip, then vertically aligned three dots, plus icon, and toggle
                if (isOngoing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Active, Extended, and Completed chips stacked vertically
                        Column(
                            modifier = Modifier.widthIn(min = 92.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            if (shouldShowPlannedBadge) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = iosPalette.tier3Field),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Text(
                                        text = "Planned",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = iosPalette.accent,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Completed chip - show if phase is completed (end date is today or passed)
                            if (isPhaseCompleted) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = iosPalette.tier3Field),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Text(
                                        text = "Completed",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            
                            // Active chip - show only if phase is not completed and not planned
                            if (!isPhaseCompleted && !shouldShowPlannedBadge) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = iosPalette.tier3Field),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Text(
                                        text = "Active",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = iosPalette.accent,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            
                            // Extended chip - show if phase has approved request and is not completed
                            if (hasApprovedRequest && !isPhaseCompleted) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = iosPalette.tier3Field),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Text(
                                        text = "Extended",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = iosPalette.accent,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        
                        // Row: Toggle on left, Column (Three dots above Plus icon) on right
                        if (( userRole == UserRole.BUSINESS_HEAD || userRole == UserRole.MANAGER) && !isPhaseCompleted) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Toggle switch
                                var activeState by remember(phaseId) { mutableStateOf(isActive) }
                                Switch(
                                    checked = activeState,
                                    onCheckedChange = { newVal ->
                                        if (isToggleEnabled) {
                                            activeState = newVal
                                            onToggleActive(newVal)
                                        }
                                    },
                                    enabled = isToggleEnabled,
                                    modifier = Modifier.scale(0.82f),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = iosPalette.onAccent,
                                        checkedTrackColor = iosPalette.accent,
                                        uncheckedThumbColor = iosPalette.textSecondary,
                                        uncheckedTrackColor = iosPalette.tier3Field,
                                        disabledCheckedThumbColor = iosPalette.textSecondary,
                                        disabledCheckedTrackColor = iosPalette.tier3Field,
                                        disabledUncheckedThumbColor = iosPalette.textSecondary,
                                        disabledUncheckedTrackColor = iosPalette.tier3Field
                                    )
                                )
                                
                                // Column: Three dots above Plus icon (vertically aligned)
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Three dots menu: show for Business Head when active, OR when completed with a proof URL
                                    if (canManagePhaseProgress && (!isPhaseCompleted || !currentPhase?.phaseOverProofUrl.isNullOrBlank())) {
                                        var showMenu by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(
                                                onClick = { showMenu = true },
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .padding(8.dp)
                                                    // .background(iosPalette.tier3Field)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreHoriz,
                                                    contentDescription = "More options",
                                                    tint = iosPalette.textSecondary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            
                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false },
                                                containerColor = iosPalette.tier2Surface,
                                                shape = RoundedCornerShape(14.dp),
                                                shadowElevation = 0.dp,
                                                modifier = Modifier.border(1.dp, iosPalette.hairline, RoundedCornerShape(14.dp))
                                            ) {
                                                // See Proof - only when completed and proof URL exists
                                                if (isPhaseCompleted && !currentPhase?.phaseOverProofUrl.isNullOrBlank()) {
                                                    DropdownMenuItem(
                                                        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                                                        text = { Text("View Proof") },
                                                        leadingIcon = {
                                                            Icon(
                                                                Icons.Default.AttachFile,
                                                                contentDescription = null,
                                                                tint = Color(0xFF027A48)
                                                            )
                                                        },
                                                        onClick = {
                                                            showMenu = false
                                                            proofUrlToView = currentPhase?.phaseOverProofUrl
                                                            showProofViewerSheet = true
                                                        }
                                                    )
                                                }

                                                // Start Now option - show if phase start date is in the future
                                                if (!isPhaseCompleted && showStartNowOption) {
                                                    DropdownMenuItem(
                                                        modifier = Modifier
                                                            .height(38.dp)
                                                            .fillMaxWidth(),
                                                        text = {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.PlayArrow,
                                                                    contentDescription = null,
                                                                    tint = iosPalette.accent,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Text(
                                                                    text = "Start Now",
                                                                    fontSize = 16.sp,
                                                                    color = iosPalette.accent,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            }
                                                        },
                                                        onClick = {
                                                            showMenu = false
                                                            phaseIdForDialog = phaseId
                                                            showStartPhaseDialog = true
                                                        }
                                                    )
                                                }
                                                
                                                // Complete Phase - show only when not completed and not a future phase
                                                if (canManagePhaseProgress && !isPhaseCompleted && !showStartNowOption) {
                                                    DropdownMenuItem(
                                                        modifier = Modifier
                                                            .height(38.dp)
                                                            .fillMaxWidth(),
                                                        text = {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.error,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Text("Complete Now", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                                                            }
                                                        },
                                                        onClick = {
                                                            showMenu = false
                                                            phaseForProofUpload = currentPhase
                                                            showProofUploadSheet = true
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Plus icon
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0A84FF))
                                            .clickable { showAddDeptSheet = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Department",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                    }
                }
            }
            
            // Edit icon row below (only for Business Head in All Phases view - not in main Project Overview)
            // Only show edit icon in "View All Phases" (when showDaysRemaining = false), not in main Project Overview
            if (showEditButton && !showDaysRemaining) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Phase",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else if (startDate != null || endDate != null) {
            // Show original start/end dates when showDaysRemaining is false - same layout as days remaining
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Phase name and dates stacked vertically
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Phase name with edit icon beside it
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (showEditButton) {
                            IconButton(
                                onClick = onEditClick,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Phase",
                                    tint = iosPalette.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    
                    // Start Date and End Date - stacked vertically
                    Column(
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Start Date
                        if (startDate != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(
                                            color = if (fromNotification == "PhaseStartHighlighting") rowHighlightColor else Color.Transparent, 
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        // Add padding AFTER the background so the highlight expands around the text nicely
                                        .padding(horizontal = 8.dp, vertical = 4.dp), 
                                    verticalAlignment = Alignment.CenterVertically,
                                    // Adds a tiny gap between "Start:" and the date
                                    horizontalArrangement = Arrangement.spacedBy(4.dp) 
                                ) {
                                    Text(
                                        text = "Start:",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Normal
                                    )
                                    
                                    Text(
                                        text = startDate,
                                        fontSize = 14.sp,
                                        // If your rowHighlightColor is dark, you might want to conditionally change 
                                        // this text color to White so it's readable, otherwise 8E8E93 is fine!
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        
                        // End Date
                        if (endDate != null) {
                            Row(
                                    modifier = Modifier
                                        .background(
                                                color = if (fromNotification == "PhaseEndDateChangeHighlighting") rowHighlightColor else Color.Transparent, 
                                                shape = RoundedCornerShape(12.dp)
                                        )
                                        // Add padding AFTER the background so the highlight expands around the text nicely
                                        .padding(horizontal = 8.dp, vertical = 4.dp), 
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "End:",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Normal
                                )
                                Text(
                                    text = endDate,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                    
                    // Total Budget below dates with purple chip label (same as when showDaysRemaining is true)
                    if (totalPhaseBudget > 0 || phaseExpensesTotal > 0) {
                        // Purple background container matching iOS design
                        Box(
                            modifier = Modifier
                                .wrapContentWidth()
                                .background(
                                    color = if (activeHighlightTag == "RemainingBudgetUpdate") rowHighlightColor else PhaseTotalBackgroundColor,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.wrapContentWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // "Total" text with its own background
                                Text(
                                    text = "Total",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PhaseTotalTextColor,
                                    modifier = Modifier
                                        .background(
                                            color = PhaseTotalTextColor.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )

                                // Budget amount - no truncation, full amount displayed
                                AutoSizeText(
                                    text = FormatUtils.formatCurrencyWithoutDecimals(totalPhaseBudget),
                                    maxFontSize = 14.sp,
                                    minFontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PhaseTotalTextColor
                                )
                            }
                        }
                    }

                }

                // Right side: Active and Extended chips (vertical), toggle, and plus button
                // Match the exact structure from main view (showDaysRemaining = true)
                if (isOngoing || isPhaseCompleted || shouldShowPlannedBadge) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Active, Extended, and Completed chips stacked vertically
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            if (shouldShowPlannedBadge) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Text(
                                        text = "Planned",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1976D2),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Completed chip - show if phase is completed (end date is today or passed)
                            if (isPhaseCompleted) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEEF0)),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Text(
                                        text = "Completed",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFD32F2F), // Soft red text
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            
                            // Active chip - show only if phase is not completed and not planned
                            if (!isPhaseCompleted && !shouldShowPlannedBadge) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Text(
                                        text = "Active",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF4CAF50),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            
                            // Extended chip - show if phase has approved request and is not completed
                            if (hasApprovedRequest && !isPhaseCompleted) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Text(
                                        text = "Extended",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFF9800),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        
                        // Row: Toggle on left, Column (Three dots above Plus icon) on right
                        if (( userRole == UserRole.BUSINESS_HEAD || userRole == UserRole.MANAGER) && !isPhaseCompleted) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Toggle switch
                                var activeState by remember(phaseId) { mutableStateOf(isActive) }
                                Switch(
                                    checked = activeState,
                                    onCheckedChange = { newVal ->
                                        if (isToggleEnabled) {
                                            activeState = newVal
                                            onToggleActive(newVal)
                                        }
                                    },
                                    enabled = isToggleEnabled,
                                    modifier = Modifier.scale(0.82f),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = iosPalette.onAccent,
                                        checkedTrackColor = iosPalette.accent,
                                        uncheckedThumbColor = iosPalette.textSecondary,
                                        uncheckedTrackColor = iosPalette.tier3Field,
                                        disabledCheckedThumbColor = iosPalette.textSecondary,
                                        disabledCheckedTrackColor = iosPalette.tier3Field,
                                        disabledUncheckedThumbColor = iosPalette.textSecondary,
                                        disabledUncheckedTrackColor = iosPalette.tier3Field
                                    )
                                )
                                
                                // Column: Three dots above Plus icon (vertically aligned)
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Three dots menu - show if phase start date is in the future OR for Business Head
                                    // Hide three dots menu if phase is completed
                                    if (canManagePhaseProgress && !isPhaseCompleted) {
                                        var showMenu by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(
                                                onClick = { showMenu = true },
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .padding(8.dp)
                                                    // .background(iosPalette.tier3Field)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreHoriz,
                                                    contentDescription = "More options",
                                                    tint = iosPalette.textSecondary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            
                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false },
                                                containerColor = iosPalette.tier2Surface,
                                                shape = RoundedCornerShape(14.dp),
                                                shadowElevation = 0.dp,
                                                modifier = Modifier.border(1.dp, iosPalette.hairline, RoundedCornerShape(14.dp))
                                            ) {
                                                // Start Now option - show if phase start date is in the future
                                                if (showStartNowOption) {
                                                    DropdownMenuItem(
                                                        modifier = Modifier
                                                            .height(38.dp)
                                                            .fillMaxWidth(),
                                                        text = {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.PlayArrow,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Text(
                                                                    text = "Start Now",
                                                                    fontSize = 16.sp,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            }
                                                        },
                                                        onClick = {
                                                            showMenu = false
                                                            // Update phase start date to today
                                                            if (approverProjectViewModel != null && projectId.isNotEmpty() && phaseId.isNotEmpty()) {
                                                                val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                                                val today = java.util.Calendar.getInstance().apply {
                                                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                                    set(java.util.Calendar.MINUTE, 0)
                                                                    set(java.util.Calendar.SECOND, 0)
                                                                    set(java.util.Calendar.MILLISECOND, 0)
                                                                }.time
                                                                val todayFormatted = dateFormatter.format(today)
                                                                
                                                                // Get phase from ViewModel
                                                                val allPhasesList = approverProjectViewModel.allPhases.value
                                                                val currentPhase = allPhasesList.firstOrNull { it.id == phaseId }
                                                                if (currentPhase != null) {
                                                                    approverProjectViewModel.updatePhaseDetails(
                                                                        projectId = projectId,
                                                                        phaseId = phaseId,
                                                                        phaseName = currentPhase.phaseName,
                                                                        startDate = todayFormatted,
                                                                        endDate = currentPhase.endDate
                                                                    )
                                                                } else {
                                                                    // If phase not found in allPhases, just update the start date
                                                                    approverProjectViewModel.updatePhaseDetails(
                                                                        projectId = projectId,
                                                                        phaseId = phaseId,
                                                                        phaseName = title, // Use the title parameter
                                                                        startDate = todayFormatted,
                                                                        endDate = endDate
                                                                    )
                                                                }
                                                                
                                                                android.widget.Toast.makeText(
                                                                    context,
                                                                    "Phase started. Start date set to today.",
                                                                    android.widget.Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                    )
                                                }
                                                
                                                // Complete Phase option - only for Business Head, only when not yet completed
                                                if (canManagePhaseProgress && !showStartNowOption && !isPhaseCompleted) {
                                                    DropdownMenuItem(
                                                        modifier = Modifier
                                                            .height(38.dp)
                                                            .fillMaxWidth(),
                                                        text = {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.error,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Text("Complete Now", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                                                            }
                                                        },
                                                        onClick = {
                                                            showMenu = false
                                                            phaseForProofUpload = currentPhase
                                                            showProofUploadSheet = true
                                                        }
                                                    )
                                                }

                                                // See Proof option for completed phases with proof URL
                                                if (isPhaseCompleted && !currentPhase?.phaseOverProofUrl.isNullOrBlank()) {
                                                    DropdownMenuItem(
                                                        text = { Text("View Proof") },
                                                        leadingIcon = {
                                                            Icon(
                                                                Icons.Default.AttachFile,
                                                                contentDescription = null,
                                                                tint = Color(0xFF027A48)
                                                            )
                                                        },
                                                        onClick = {
                                                            showMenu = false
                                                            proofUrlToView = currentPhase?.phaseOverProofUrl
                                                            showProofViewerSheet = true
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Plus icon
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(iosPalette.accent)
                                            .clickable { showAddDeptSheet = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Department",
                                            tint = iosPalette.onAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (canManagePhaseProgress && (!isPhaseCompleted || !currentPhase?.phaseOverProofUrl.isNullOrBlank())) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(iosPalette.tier3Field)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "More options",
                                    tint = iosPalette.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                containerColor = iosPalette.tier2Surface,
                                shape = RoundedCornerShape(14.dp),
                                shadowElevation = 0.dp,
                                modifier = Modifier.border(1.dp, iosPalette.hairline, RoundedCornerShape(14.dp))
                            ) {
                                // See Proof - only when completed and proof URL exists (shown first)
                                if (isPhaseCompleted && !currentPhase?.phaseOverProofUrl.isNullOrBlank()) {
                                    DropdownMenuItem(
                                        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                                        text = { Text("View Proof") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.AttachFile,
                                                contentDescription = null,
                                                tint = Color(0xFF027A48)
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            proofUrlToView = currentPhase?.phaseOverProofUrl
                                            showProofViewerSheet = true
                                        }
                                    )
                                }

                                if (!isPhaseCompleted && showStartNowOption) {
                                    DropdownMenuItem(
                                        modifier = Modifier
                                            .height(38.dp)
                                            .fillMaxWidth(),
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = iosPalette.accent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Start Now",
                                                    fontSize = 16.sp,
                                                    color = iosPalette.accent,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        },

                                        onClick = {
                                            showMenu = false
                                            phaseIdForDialog = phaseId
                                            showStartPhaseDialog = true
                                        }
                                    )
                                }

                                if (!showStartNowOption && !isPhaseCompleted) {
                                    DropdownMenuItem(
                                        modifier = Modifier
                                            .height(38.dp)
                                            .fillMaxWidth(),
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text("Complete Now", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                                            }
                                        },
                                        onClick = {
                                            showMenu = false
                                            phaseForProofUpload = currentPhase
                                            showProofUploadSheet = true
                                        }
                                    )
                                }
                            }
                        }

                    }
                }
            }
        } else if (isOngoing) {
            // If no date but is ongoing, show Active badge, toggle and plus icon on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Chips column (Active, Extended, and Completed)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        if (shouldShowPlannedBadge) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = "Planned",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF1976D2),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Completed chip - show if phase is completed (end date is today or passed)
                        if (isPhaseCompleted) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), // Soft red background
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = "Completed",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFFD32F2F), // Soft red text
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        // Active chip - show only if phase is not completed and not planned
                        if (!isPhaseCompleted && !shouldShowPlannedBadge) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9)),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = "Active",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        // Extended chip - show if phase has approved request and is not completed
                        if (hasApprovedRequest && !isPhaseCompleted) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0B2)),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = "Extended",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    // Toggle and Plus icon row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Toggle switch - for Business Head and Manager
                        if (userRole == UserRole.BUSINESS_HEAD || userRole == UserRole.MANAGER) {
                            var activeState by remember(phaseId) { mutableStateOf(isActive) }
                            Switch(
                                checked = activeState,
                                onCheckedChange = { newVal ->
                                    if (isToggleEnabled) {
                                        activeState = newVal
                                        onToggleActive(newVal)
                                    }
                                },
                                enabled = isToggleEnabled,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = iosPalette.onAccent,
                                    checkedTrackColor = iosPalette.accent,
                                    uncheckedThumbColor = iosPalette.textSecondary,
                                    uncheckedTrackColor = iosPalette.tier3Field,
                                    disabledCheckedThumbColor = iosPalette.textSecondary,
                                    disabledCheckedTrackColor = iosPalette.tier3Field,
                                    disabledUncheckedThumbColor = iosPalette.textSecondary,
                                    disabledUncheckedTrackColor = iosPalette.tier3Field
                                )
                            )
                        }
                        
                        // Three dots menu - show if phase start date is in the future OR for Business Head in All Phases view
                        // Hide three dots menu if phase is completed
                        if (canManagePhaseProgress && !isPhaseCompleted) {
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(iosPalette.tier3Field)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreHoriz,
                                        contentDescription = "More options",
                                        tint = iosPalette.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    containerColor = iosPalette.tier2Surface,
                                    shape = RoundedCornerShape(14.dp),
                                    shadowElevation = 0.dp,
                                    modifier = Modifier.border(1.dp, iosPalette.hairline, RoundedCornerShape(14.dp))
                                ) {
                                    // Start Now option - show if phase start date is in the future
                                    if (showStartNowOption) {
                                        DropdownMenuItem(
                                            modifier = Modifier
                                                .height(38.dp)
                                                .fillMaxWidth(),
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = iosPalette.accent,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "Start Now",
                                                        fontSize = 16.sp,
                                                        color = iosPalette.accent,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            },
                                            onClick = {
                                                showMenu = false
                                                // Update phase start date to today
                                                if (approverProjectViewModel != null && projectId.isNotEmpty() && phaseId.isNotEmpty()) {
                                                    val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                                    val today = java.util.Calendar.getInstance().apply {
                                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                        set(java.util.Calendar.MINUTE, 0)
                                                        set(java.util.Calendar.SECOND, 0)
                                                        set(java.util.Calendar.MILLISECOND, 0)
                                                    }.time
                                                    val todayFormatted = dateFormatter.format(today)
                                                    
                                                    // Get phase from ViewModel
                                                    val allPhasesList = approverProjectViewModel.allPhases.value
                                                    val currentPhase = allPhasesList.firstOrNull { it.id == phaseId }
                                                    if (currentPhase != null) {
                                                        approverProjectViewModel.updatePhaseDetails(
                                                            projectId = projectId,
                                                            phaseId = phaseId,
                                                            phaseName = currentPhase.phaseName,
                                                            startDate = todayFormatted,
                                                            endDate = currentPhase.endDate
                                                        )
                                                    } else {
                                                        // If phase not found in allPhases, just update the start date
                                                        approverProjectViewModel.updatePhaseDetails(
                                                            projectId = projectId,
                                                            phaseId = phaseId,
                                                            phaseName = title, // Use the title parameter
                                                            startDate = todayFormatted,
                                                            endDate = endDate
                                                        )
                                                    }
                                                    
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Phase started. Start date set to today.",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        )
                                    }
                                    
                                    // Complete Phase option - only for Business Head in All Phases view, only when not yet completed
                                    if (canManagePhaseProgress && !showStartNowOption && !isPhaseCompleted) {
                                        DropdownMenuItem(
                                            modifier = Modifier
                                                .height(38.dp)
                                                .fillMaxWidth(),
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text("Complete Now", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                                                }
                                            },
                                            onClick = {
                                                showMenu = false
                                                phaseForProofUpload = currentPhase
                                                showProofUploadSheet = true
                                            }

                                        )
                                    }

                                    // See Proof option for completed phases with proof URL
                                    if (isPhaseCompleted && !currentPhase?.phaseOverProofUrl.isNullOrBlank()) {
                                        DropdownMenuItem(
                                            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                                            text = { Text("View Proof") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.AttachFile,
                                                    contentDescription = null,
                                                    tint = Color(0xFF027A48)
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                proofUrlToView = currentPhase?.phaseOverProofUrl
                                                showProofViewerSheet = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Plus icon - only for Business Head - improved styling
                        if (( userRole == UserRole.BUSINESS_HEAD || userRole == UserRole.MANAGER) && !isPhaseCompleted) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0A84FF))
                                    .clickable { showAddDeptSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Department",
                                    tint = iosPalette.onAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        if (!isOngoing && showAddButton && ( userRole == UserRole.BUSINESS_HEAD || userRole == UserRole.MANAGER) && !isPhaseCompleted) {
            IconButton(onClick = { showAddDeptSheet = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Department",
                    tint = iosPalette.accent
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Approved and Remaining Amount Card - Below Total Budget
        if (totalPhaseBudget > 0 || phaseExpensesTotal > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = approvedRemainingCardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Approved - Left, Blue
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Approved",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = iosPalette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AutoSizeText(
                            text = FormatUtils.formatCurrencyWithoutDecimals(phaseExpensesTotal),
                            maxFontSize = 20.sp,
                            minFontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApprovedAmountColor
                        )
                    }
                    
                    // Remaining - Right, Green or Red
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Remaining",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = iosPalette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AutoSizeText(
                            text = FormatUtils.formatCurrencyWithoutDecimals(remainingAmount),
                            maxFontSize = 20.sp,
                            minFontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingAmount > 0) PositiveAmountColor else NegativeAmountColor
                        )
                    }
                }
            }
        }
        
        // Create "Others" department breakdown for this phase
        // Show "Others" card only if there are deleted departments (regardless of expenses)
        val othersDepartment = remember(othersSpent, phaseAnonymousDepartments) {
            if (phaseAnonymousDepartments.isNotEmpty()) {
                DepartmentBudgetBreakdown(
                    department = "Others",
                    budgetAllocated = 0.0,
                    spent = othersSpent,
                    remaining = 0.0,
                    percentage = 0.0
                )
            } else null
        }
        
        // Filter out departments with same name as phase
        val regularDepartmentCells = departmentCellDataList.filter { dept ->
            !dept.name.trim().equals(title.trim(), ignoreCase = true)
        }
        
        if (regularDepartmentCells.isNotEmpty() || othersDepartment != null) {
            // Always show departments horizontally (side-by-side) using DepartmentCell
            val lazyListState = rememberLazyListState()
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyRow(
                    state = lazyListState,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Show regular departments first using DepartmentCell (full screen width per Material layout)
                    items(regularDepartmentCells) { departmentCell ->
                        DepartmentCell(
                            data = departmentCell,
                            cellWidth = departmentCellWidth,
                            onClick = {
                                val fullDepartmentName = "${phaseId}_${departmentCell.name}"
                                onNavigateToDepartmentDetail(
                                    projectId,
                                    fullDepartmentName,
                                    departmentCell.budget,
                                    departmentCell.spent,
                                    departmentCell.remaining
                                )
                            }
                        )
                    }
                    
                    // Show "Others" card at the end if there are deleted departments
                    // Note: "Others" doesn't have lineItems, so we'll use a simple card for it
                    if (othersDepartment != null) {
                        item {
                            // Create a simple DepartmentCellData for "Others" (no materials/labour)
                            val othersCellData = DepartmentCellData(
                                id = "Others",
                                name = "Others",
                                contractorMode = "",
                                budget = othersDepartment.budgetAllocated,
                                spent = othersDepartment.spent,
                                materials = emptyList(),
                                labour = null
                            )
                            
                            DepartmentCell(
                                data = othersCellData,
                                cellWidth = departmentCellWidth,
                                onClick = {
                                    onNavigateToDepartmentDetail(
                                        projectId,
                                        "Others_${phaseId}",
                                        othersDepartment.budgetAllocated,
                                        othersDepartment.spent,
                                        othersDepartment.remaining
                                    )
                                }
                            )
                        }
                    }
                }
                
                // Horizontal scrollbar indicator
                if (lazyListState.layoutInfo.totalItemsCount > 0) {
                    val viewportSize = lazyListState.layoutInfo.viewportSize.width
                    val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo
                    
                    // Calculate total content size from visible items
                    val totalContentSize = if (visibleItemsInfo.isNotEmpty()) {
                        val lastItem = visibleItemsInfo.last()
                        lastItem.offset + lastItem.size
                    } else {
                        viewportSize
                    }
                    
                    if (totalContentSize > viewportSize && totalContentSize > 0) {
                        val firstVisibleIndex = lazyListState.firstVisibleItemIndex
                        val scrollOffset = lazyListState.firstVisibleItemScrollOffset
                        
                        // Calculate scroll position based on visible items
                        val startOffset = if (visibleItemsInfo.isNotEmpty()) {
                            visibleItemsInfo.first().offset
                        } else 0
                        
                        val scrollableRange = (totalContentSize - viewportSize).toFloat()
                        val currentScroll = (-startOffset.toFloat()).coerceIn(0f, scrollableRange)
                        val scrollRatio = if (scrollableRange > 0f) (currentScroll / scrollableRange).coerceIn(0f, 1f) else 0f
                        
                        val scrollbarWidth = (viewportSize.toFloat() / totalContentSize.toFloat()).coerceIn(0.1f, 1f)
                        val maxScrollbarPosition = 1f - scrollbarWidth
                        val scrollbarPosition = scrollRatio * maxScrollbarPosition
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        val density = LocalDensity.current
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(iosPalette.hairline, RoundedCornerShape(2.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(scrollbarWidth)
                                    .fillMaxHeight()
                                    .align(Alignment.CenterStart)
                                    .offset(x = with(density) { (scrollbarPosition * viewportSize).toDp() })
                                    .background(iosPalette.textSecondary, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No department budgets allocated",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Check for duplicate department names within the same phase (case-insensitive)
    val isDuplicateDepartmentName = remember(newDeptName, departments) {
        val currentDeptName = newDeptName.trim()
        if (currentDeptName.isEmpty()) {
            false
        } else {
            departments.any { existingDept ->
                existingDept.department.trim().lowercase() == currentDeptName.lowercase()
            }
        }
    }
    
    if (showAddDeptSheet) {
        AddDepartmentModalSheet(
            phaseName = title,
            departmentName = newDeptName,
            onDepartmentNameChange = { newDeptName = it },
            contractorMode = newDeptContractorMode,
            onContractorModeChange = { newDeptContractorMode = it },
            lineItems = newDeptLineItems,
            onLineItemsChange = { newDeptLineItems = it.toMutableList() },
            isDuplicateDepartmentName = isDuplicateDepartmentName,
            afterProjectCreationNavigation = true,
            approverOptions = availableApprovers,
            userOptions = availableUsers,
            onAssignmentChange = { _, _ -> }, // Assignment changes handled in onCreate
            projectId = projectId,
            onDismiss = {
                showAddDeptSheet = false
                newDeptName = ""
                newDeptContractorMode = ContractorMode.LABOUR_ONLY
                newDeptLineItems = mutableListOf()
            },
            onCreate = { approverPhone, userPhone, contractorAmount ->
                val trimmedDeptName = newDeptName.trim()
                
                if (trimmedDeptName.isNotEmpty() && !isDuplicateDepartmentName) {
                    // Filter out empty/invalid lineItems before saving
                    val validLineItems = newDeptLineItems.filter { lineItem ->
                        lineItem.itemType.isNotBlank() &&
                        lineItem.item.isNotBlank() &&
                        lineItem.spec.isNotBlank() &&
                        lineItem.quantity > 0 &&
                        lineItem.unitPrice > 0
                    }
                    
                    // Create DepartmentDraft with filtered lineItems
                    val departmentDraft = DepartmentDraft(
                        departmentName = trimmedDeptName,
                        contractorMode = newDeptContractorMode,
                        lineItems = validLineItems.toMutableList(),
                        contractorAmount = contractorAmount
                    )
                    
                    // Save department assignments to project if provided
                    if ((approverPhone != null && approverPhone.isNotBlank()) || 
                        (userPhone != null && userPhone.isNotBlank())) {
                        approverProjectViewModel?.updateDepartmentAssignments(
                            projectId = projectId,
                            phaseId = phaseId,
                            departmentName = trimmedDeptName,
                            approverPhone = approverPhone,
                            userPhone = userPhone
                        )
                    }
                    
                    // Add department to phase
                    onAddDepartment(departmentDraft)
                    
                    // Reset form
                    newDeptName = ""
                    newDeptContractorMode = ContractorMode.LABOUR_ONLY
                    newDeptLineItems = mutableListOf()
                    showAddDeptSheet = false
                }
            }
        )
    }
}

@Composable
private fun DepartmentBudgetCard(
    department: DepartmentBudgetBreakdown,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val cardWidth = remember(configuration.screenWidthDp) {
        when {
            configuration.screenWidthDp < 360 -> 200.dp
            configuration.screenWidthDp < 480 -> 215.dp
            else -> 230.dp
        }
    }
    val cardPadding = responsivePadding()
    val cardSpacing = responsiveSpacing()
    val valueFontSize = responsiveFontSize(14.sp, 15.sp, 16.sp, 17.sp)
    val minValueFontSize = remember(valueFontSize) {
        max(10f, valueFontSize.value * 0.7f).sp
    }
    
    Card(
        modifier = modifier
            .width(cardWidth)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding)
        ) {
            // Header with title and info icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TruncatedTextWithPopup(
                    text = FormatUtils.getDepartmentDisplayName(department.department),
                    maxLength = 15,
                    fontSize = responsiveFontSize(16.sp, 17.sp, 18.sp, 20.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Info",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(responsiveIconSize(16.dp, 17.dp, 18.dp))
                )
            }
            
            Spacer(modifier = Modifier.height(cardSpacing))
            
            // Budget row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget:",
                    fontSize = responsiveFontSize(12.sp, 13.sp, 14.sp, 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                AutoSizeText(
                    text = FormatUtils.formatCurrencyWithoutDecimals(department.budgetAllocated),
                    maxFontSize = valueFontSize,
                    minFontSize = minValueFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(cardSpacing))
            
            // Approved row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Approved:",
                    fontSize = responsiveFontSize(12.sp, 13.sp, 14.sp, 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                AutoSizeText(
                    text = FormatUtils.formatCurrencyWithoutDecimals(department.spent),
                    maxFontSize = valueFontSize,
                    minFontSize = minValueFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4)
                )
            }
            
            Spacer(modifier = Modifier.height(cardSpacing))
            
            // Remaining row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Remaining:",
                    fontSize = responsiveFontSize(12.sp, 13.sp, 14.sp, 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                AutoSizeText(
                    text = FormatUtils.formatCurrencyWithoutDecimals(department.remaining),
                    maxFontSize = valueFontSize,
                    minFontSize = minValueFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
            
            Spacer(modifier = Modifier.height(cardSpacing))
            
            // Progress bar showing approved vs budget
            val progress = if (department.budgetAllocated > 0) {
                (department.spent / department.budgetAllocated).coerceIn(0.0, 1.0)
            } else {
                0.0
            }
            LinearProgressIndicator(
                progress = { progress.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color(0xFF4285F4),
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

/**
 * Aggregate department budgets from all phases
 * Sums budgets across all phases for each department
 * Returns Pair of (budgets map, departmentKeyMap)
 */
private fun aggregateDepartmentBudgets(phasesWithDepartments: List<com.cubiquitous.tracura.model.PhaseWithDepartments>): Pair<Map<String, Double>, Map<String, String>> {
    val budgets = mutableMapOf<String, Double>()
    val departmentKeyMap = mutableMapOf<String, String>() // Maps phaseId_departmentName -> department name
    
    phasesWithDepartments.forEach { phaseWithDepts ->
        phaseWithDepts.departments.forEach { department ->
            val departmentName = department.name
            val current = budgets[departmentName] ?: 0.0
            budgets[departmentName] = current + department.totalBudget
            
            // Map department key for expense matching
            val deptKey = "${phaseWithDepts.phase.id}_$departmentName"
            departmentKeyMap[deptKey] = departmentName
        }
    }
    
    return Pair(budgets, departmentKeyMap)
}

/**
 * Aggregate approved expenses by department
 * Matches expenses to departments using departmentKeyMap
 * Handles anonymous expenses separately
 */
private fun aggregateApprovedExpenses(
    expenses: List<com.cubiquitous.tracura.model.Expense>,
    departmentKeyMap: Map<String, String>
): Map<String, Double> {
    val expensesByDepartment = mutableMapOf<String, Double>()
    var anonymousExpenses = 0.0
    val validDepartmentKeys = departmentKeyMap.keys.toSet()
    
    expenses.forEach { expense ->
        if (expense.status == com.cubiquitous.tracura.model.ExpenseStatus.APPROVED) {
            val expenseDepartmentKey = expense.department
            
            if (validDepartmentKeys.contains(expenseDepartmentKey)) {
                val departmentName = departmentKeyMap[expenseDepartmentKey] ?: return@forEach
                expensesByDepartment[departmentName] = (expensesByDepartment[departmentName] ?: 0.0) + expense.amount
            } else if (expense.isAnonymous == true) {
                // Anonymous expense
                anonymousExpenses += expense.amount
            } else {
                // Department doesn't exist, treat as anonymous
                anonymousExpenses += expense.amount
            }
        }
    }
    
    if (anonymousExpenses > 0) {
        expensesByDepartment["Other Expenses"] = anonymousExpenses
    }
    
    return expensesByDepartment
}

@Composable
private fun DepartmentDistributionSection(
    phasesWithDepartments: List<com.cubiquitous.tracura.model.PhaseWithDepartments>,
    projectExpenses: List<com.cubiquitous.tracura.model.Expense>,
    onNavigateToReports: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    drawerState: DrawerState
) {
    val iosPalette = rememberIosDashboardPalette()
    val neutralTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Aggregate department budgets from all phases
    val departmentBudgets = remember(phasesWithDepartments, projectExpenses) {
        val (aggregatedBudgets, departmentKeyMap) = aggregateDepartmentBudgets(phasesWithDepartments)
        val aggregatedExpenses = aggregateApprovedExpenses(projectExpenses, departmentKeyMap)
        
        val list = aggregatedBudgets.map { (department, budget) ->
            DepartmentBudget(
                department = department,
                totalBudget = budget,
                approvedBudget = aggregatedExpenses[department] ?: 0.0,
                color = getDepartmentColor(department)
            )
        }.sortedBy { it.department }
        
        // Add "Other Expenses" if there are anonymous expenses
        val otherExpensesAmount = aggregatedExpenses["Other Expenses"] ?: 0.0
        if (otherExpensesAmount > 0) {
            list + DepartmentBudget(
                department = "Other Expenses",
                totalBudget = 0.0,
                approvedBudget = otherExpensesAmount,
                color = neutralTextColor
            )
        } else {
            list
        }
    }
    if (departmentBudgets.isEmpty()) {
        return
    }
    
    // Calculate total budget (using max of totalBudget and approvedBudget to include "Other Expenses")
    val totalBudget = departmentBudgets.sumOf { max(it.totalBudget, it.approvedBudget) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Department Distribution",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = iosPalette.tier3Field),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Budget Allocation",
                fontSize = 14.sp,
                color = iosPalette.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Donut Chart - Responsive size based on screen width
            val configuration = LocalConfiguration.current
            val screenWidthDp = configuration.screenWidthDp
            val donutChartSize = remember(screenWidthDp) {
                when {
                    screenWidthDp < 360 -> 240.dp
                    screenWidthDp < 480 -> 280.dp
                    screenWidthDp < 600 -> 320.dp
                    screenWidthDp < 840 -> 360.dp
                    else -> 400.dp
                }
            }
            val strokeWidthDp = remember(screenWidthDp) {
                when {
                    screenWidthDp < 360 -> 28.dp
                    screenWidthDp < 480 -> 32.dp
                    screenWidthDp < 600 -> 36.dp
                    screenWidthDp < 840 -> 40.dp
                    else -> 44.dp
                }
            }
            val centerCardSize = remember(screenWidthDp) {
                when {
                    screenWidthDp < 360 -> 120.dp
                    screenWidthDp < 480 -> 140.dp
                    screenWidthDp < 600 -> 160.dp
                    screenWidthDp < 840 -> 180.dp
                    else -> 200.dp
                }
            }
            val centerCardPadding = remember(screenWidthDp) {
                when {
                    screenWidthDp < 360 -> 12.dp
                    screenWidthDp < 480 -> 14.dp
                    screenWidthDp < 600 -> 16.dp
                    screenWidthDp < 840 -> 18.dp
                    else -> 20.dp
                }
            }
            val budgetLabelFontSize = remember(screenWidthDp) {
                when {
                    screenWidthDp < 360 -> 10.sp
                    screenWidthDp < 480 -> 11.sp
                    screenWidthDp < 600 -> 12.sp
                    screenWidthDp < 840 -> 13.sp
                    else -> 14.sp
                }
            }
            val budgetAmountFontSize = remember(screenWidthDp) {
                when {
                    screenWidthDp < 360 -> 16.sp
                    screenWidthDp < 480 -> 18.sp
                    screenWidthDp < 600 -> 20.sp
                    screenWidthDp < 840 -> 22.sp
                    else -> 24.sp
                }
            }
            val minBudgetAmountFontSize = remember(budgetAmountFontSize) {
                max(10f, budgetAmountFontSize.value * 0.6f).sp
            }
            
            Box(
                modifier = Modifier
                    .size(donutChartSize)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                // Background circle and data segments in single Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = strokeWidthDp.toPx()
                    val radius = size.minDimension / 2 - strokeWidth / 2
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    
                    // Draw background circle
                    drawCircle(
                        color = iosPalette.hairline,
                        radius = radius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = strokeWidth)
                    )
                    
                    // Draw data segments
                    departmentBudgets.forEachIndexed { index, budget ->
                        val startAngle = startAngleForIndex(departmentBudgets, index, totalBudget)
                        val endAngle = endAngleForIndex(departmentBudgets, index, totalBudget)
                        val sweepAngle = (endAngle - startAngle) * 360f
                        
                        if (sweepAngle > 0) {
                            drawArc(
                                color = budget.color,
                                startAngle = startAngle * 360f - 90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round
                                ),
                                topLeft = Offset(
                                    centerX - radius,
                                    centerY - radius
                                ),
                                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                            )
                        }
                    }
                }
                
                // Center content - Single line budget display
                Card(
                    modifier = Modifier.size(centerCardSize),
                    colors = CardDefaults.cardColors(containerColor = PhaseTotalBackgroundColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(centerCardPadding),
                        verticalArrangement = Arrangement.Center, // Centers content vertically
                        horizontalAlignment = Alignment.CenterHorizontally // Centers content horizontally
                    ) {
                        Text(
                            text = "Total Budget", // Removed the colon ":" since it's now a header
                            fontSize = budgetLabelFontSize,
                            color = PhaseTotalTextColor,
                            fontWeight = FontWeight.Medium
                        )

                        // Optional: Add a small space between the label and the amount
                        Spacer(modifier = Modifier.height(4.dp))

                        AutoSizeText(
                            text = FormatUtils.formatCurrency(departmentBudgets.sumOf { it.totalBudget }),
                            maxFontSize = budgetAmountFontSize,
                            minFontSize = minBudgetAmountFontSize,
                            fontWeight = FontWeight.Bold,
                            color = PhaseTotalTextColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // Legend
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                departmentBudgets.forEachIndexed { index, budget ->
                    val budgetValue = max(budget.totalBudget, budget.approvedBudget)
                    val percentage = if (totalBudget > 0) {
                        ((budgetValue / totalBudget) * 100).toInt()
                    } else 0
                    
                    DepartmentLegendRow(
                        budget = budget,
                        budgetValue = budgetValue,
                        percentage = percentage,
                        index = index
                    )
                }
            }
        }
    }
}

/**
 * Calculate start angle for a department segment
 */
private fun startAngleForIndex(departmentBudgets: List<DepartmentBudget>, index: Int, totalBudget: Double): Float {
    if (departmentBudgets.isEmpty() || totalBudget == 0.0) return 0f
    
    val previousBudgets = departmentBudgets
        .take(index)
        .sumOf { max(it.totalBudget, it.approvedBudget) }
    
    return (previousBudgets / totalBudget).toFloat()
}

/**
 * Calculate end angle for a department segment
 */
private fun endAngleForIndex(departmentBudgets: List<DepartmentBudget>, index: Int, totalBudget: Double): Float {
    if (departmentBudgets.isEmpty() || totalBudget == 0.0) return 0f
    
    val currentAndPreviousBudgets = departmentBudgets
        .take(index + 1)
        .sumOf { max(it.totalBudget, it.approvedBudget) }
    
    return (currentAndPreviousBudgets / totalBudget).toFloat()
}

@Composable
private fun DepartmentLegendRow(
    budget: DepartmentBudget,
    budgetValue: Double,
    percentage: Int,
    index: Int
) {
    val iosPalette = rememberIosDashboardPalette()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier3Field),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = budget.color.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(budget.color, budget.color.copy(alpha = 0.8f))
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = iosPalette.tier2Surface,
                        shape = RoundedCornerShape(6.dp)
                    )
            )
            
            // Department info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = budget.department,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = FormatUtils.formatCurrency(budgetValue),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$percentage%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Percentage badge
            Text(
                text = "$percentage%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = budget.color,
                modifier = Modifier
                    .background(
                        color = budget.color.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// Team Member Item Component
@Composable
private fun TeamMemberItem(
    user: User,
    onClick: () -> Unit = {}
) {
    val iosPalette = rememberIosDashboardPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iosPalette.accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = iosPalette.onAccent,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Member Info
        Column(modifier = Modifier.weight(1f)) {
            TruncatedTextWithPopup(
                text = user.name.ifEmpty { "Unknown User" },
                maxLength = 10,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = user.role.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 14.sp,
                color = iosPalette.accent
            )
        }
        
        // Status indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (user.isActive) iosPalette.accent else iosPalette.textSecondary)
        )
    }
}

// Date Picker Dialog Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    datePickerState: DatePickerState,
    title: String
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Date(millis))
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            modifier = Modifier.padding(16.dp),
            title = {
                Text(
                    text = title,
                    modifier = Modifier.padding(16.dp)
                )
            },
            showModeToggle = false
        )
    }
}


// Edit Phase Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPhaseBottomSheet(
    projectId: String,
    phaseId: String,
    currentPhaseName: String,
    currentStartDate: String?,
    currentEndDate: String?,
    onDismiss: () -> Unit,
    onPhaseUpdated: () -> Unit,
    approverProjectViewModel: ApproverProjectViewModel
) {
    val context = LocalContext.current
    var phaseName by remember { mutableStateOf(currentPhaseName) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()
    
    val dateFormatter = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
    
    // Initialize dates from current values
    LaunchedEffect(currentStartDate, currentEndDate) {
        if (currentStartDate != null) {
            try {
                startDate = dateFormatter.parse(currentStartDate)
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
        if (currentEndDate != null) {
            try {
                endDate = dateFormatter.parse(currentEndDate)
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
    }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Header with Cancel, Title, and Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFF007AFF))
                }
                Text(
                    text = "Edit Phase",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val isSaveEnabled = phaseName.isNotBlank() && 
                    startDate != null && 
                    endDate != null && 
                    endDate!!.time >= startDate!!.time
                TextButton(
                    onClick = {
                        if (isSaveEnabled) {
                            approverProjectViewModel.updatePhaseDetails(
                                projectId = projectId,
                                phaseId = phaseId,
                                phaseName = phaseName.trim(),
                                startDate = dateFormatter.format(startDate!!),
                                endDate = dateFormatter.format(endDate!!)
                            )
                            android.widget.Toast.makeText(context, "Phase updated successfully", android.widget.Toast.LENGTH_SHORT).show()
                            onPhaseUpdated()
                        }
                    },
                    enabled = isSaveEnabled
                ) {
                    Text(
                        "Save",
                        color = if (isSaveEnabled) Color(0xFF007AFF) else Color(0xFFC7C7CC)
                    )
                }
            }
            
            Divider(color = Color(0xFFE5E5EA), thickness = 1.dp)
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Phase Name
                OutlinedTextField(
                    value = phaseName,
                    onValueChange = { phaseName = it },
                    label = { Text("Phase Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color(0xFFC7C7CC)
                    )
                )
                
                // Start Date
                OutlinedTextField(
                    value = startDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Start Date *") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.clickable { showStartDatePicker = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartDatePicker = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color(0xFFC7C7CC)
                    )
                )
                
                // End Date
                OutlinedTextField(
                    value = endDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("End Date *") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.clickable { showEndDatePicker = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEndDatePicker = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color(0xFFC7C7CC)
                    )
                )
                
                // Error if end date is before start date
                if (endDate != null && startDate != null && endDate!!.time < startDate!!.time) {
                    Text(
                        text = "End Date must be on or after Start Date",
                        color = Color(0xFFD32F2F),
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // Start Date Picker
    if (showStartDatePicker) {
        DatePickerDialog(
            onDateSelected = { selectedDate ->
                startDate = selectedDate
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false },
            datePickerState = startDatePickerState,
            title = "Select Start Date"
        )
    }
    
    // End Date Picker
    if (showEndDatePicker) {
        DatePickerDialog(
            onDateSelected = { selectedDate ->
                if (startDate == null || selectedDate.time >= startDate!!.time) {
                    endDate = selectedDate
                } else {
                    android.widget.Toast.makeText(context, "End Date must be on or after Start Date", android.widget.Toast.LENGTH_SHORT).show()
                }
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false },
            datePickerState = endDatePickerState,
            title = "Select End Date"
        )
    }
}

// UserExpensesBottomSheet has been extracted to components/UserExpensesBottomSheet.kt

// Phase Budget Distribution Section for Approvers
@Composable
fun PhaseBudgetDistributionSection(
    notificationMessage: String = "",
    rowHighlightColor: Color = Color.Transparent,
    phaseBudgetData: List<PhaseBudgetData>,
    onPhaseClick: (PhaseBudgetData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val iosPalette = rememberIosDashboardPalette()
    if (phaseBudgetData.isEmpty()) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Phase Budget Distribution",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = iosPalette.textPrimary
            )
            
            // Display each phase budget card
            phaseBudgetData.forEach { phaseData ->
                PhaseBudgetCard(
                    phaseData = phaseData,
                    notificationMessage = notificationMessage,
                    rowHighlightColor = rowHighlightColor,
                    onClick = { onPhaseClick(phaseData) }
                )
            }
        }
    }
}


@Composable
fun PhaseBudgetCard(
    phaseData: PhaseBudgetData,
    notificationMessage: String = "",
    rowHighlightColor: Color = Color.Transparent,
    onClick: () -> Unit = {}
) {
   val iosPalette = rememberIosDashboardPalette()
   val isHighlighted =
        notificationMessage.isNotEmpty() &&
        notificationMessage.contains(phaseData.phaseName, ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted)
                rowHighlightColor
            else
                iosPalette.tier3Field
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) 
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Phase Name (30% width)
            Text(
                text = phaseData.phaseName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosPalette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.3f)
            )

            // Progress Bar Section (50% width)
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Mini stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "₹${formatCompact(phaseData.spent)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF34C759)
                    )
                    Text(
                        text = "₹${formatCompact(phaseData.remaining)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (phaseData.remaining < 0) Color(0xFFFF3B30) else Color(0xFF007AFF)
                    )
                }

                // Compact progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(iosPalette.hairline)
                ) {
                    val utilizationSeverity = budgetUtilizationSeverity(phaseData.utilizationPercentage)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(budgetUtilizationProgress(phaseData.utilizationPercentage))
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when (utilizationSeverity) {
                                    BudgetUtilizationSeverity.ERROR -> Color(0xFFFF3B30)
                                    BudgetUtilizationSeverity.WARNING -> Color(0xFFFF9500)
                                    BudgetUtilizationSeverity.NORMAL -> Color(0xFF34C759)
                                }
                            )
                    )
                }
            }

            // Percentage Badge (20% width)
            Box(
                modifier = Modifier
                    .weight(0.2f)
                    .wrapContentWidth(Alignment.End)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (val utilizationSeverity = budgetUtilizationSeverity(phaseData.utilizationPercentage)) {
                        BudgetUtilizationSeverity.ERROR -> Color(0xFFFF3B30).copy(alpha = 0.1f)
                        BudgetUtilizationSeverity.WARNING -> Color(0xFFFF9500).copy(alpha = 0.1f)
                        BudgetUtilizationSeverity.NORMAL -> Color(0xFF34C759).copy(alpha = 0.1f)
                    }
                ) {
                    Text(
                        text = "${phaseData.utilizationPercentage.toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (budgetUtilizationSeverity(phaseData.utilizationPercentage)) {
                            BudgetUtilizationSeverity.ERROR -> Color(0xFFFF3B30)
                            BudgetUtilizationSeverity.WARNING -> Color(0xFFFF9500)
                            BudgetUtilizationSeverity.NORMAL -> Color(0xFF34C759)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// Helper function to format currency compactly
fun formatCompact(value: Double): String {
    return when {
        value >= 10_000_000 -> String.format("%.1fCr", value / 10_000_000)
        value >= 100_000 -> String.format("%.1fL", value / 100_000)
        value >= 1_000 -> String.format("%.1fK", value / 1_000)
        else -> String.format("%.0f", value)
    }
}

@Composable

fun ApproverRecentExpensesCard(
    expenses: List<com.cubiquitous.tracura.model.Expense>,
    totalExpensesCount: Int,
    phases: List<com.cubiquitous.tracura.model.Phase> = emptyList(),
    onShowAll: () -> Unit,
    onNavigateToExpenseReview: (String) -> Unit = {},
    onEditExpense: (String) -> Unit = {},
    currentUserPhone: String?,
    userNameMap: Map<String, String> = emptyMap(),
    businessHeadPhone: String? = null
) {
    val iosPalette = rememberIosDashboardPalette()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
            ) {
                Text(
                    text = "Recent Expenses",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.textPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            
            if (expenses.isEmpty()) {
                Text(
                    text = "No recent expenses for your assigned departments",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                expenses.forEach { expense ->
                    ApproverExpenseRow(
                        expense = expense,
                        phases = phases,
                        onNavigateToReview = { onNavigateToExpenseReview(expense.id) },
                        onEditExpense = { expenseId ->
                            onEditExpense(expenseId)
                        },
                        currentUserPhone = currentUserPhone,
                        userNameMap = userNameMap,
                        businessHeadPhone = businessHeadPhone
                    )
                    if (expense != expenses.last()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                // View All Expenses link
                if (totalExpensesCount > expenses.size) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "View All Expenses",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = iosPalette.accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShowAll() },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ApproverExpenseRow(
    expense: com.cubiquitous.tracura.model.Expense,
    phases: List<com.cubiquitous.tracura.model.Phase> = emptyList(),
    onNavigateToReview: () -> Unit = {},
    onEditExpense: (String) -> Unit = {},
    currentUserPhone: String?,
    userNameMap: Map<String, String> = emptyMap(),
    businessHeadPhone: String? = null
) {
    val iosPalette = rememberIosDashboardPalette()
    val statusColor = when (expense.status) {
        ExpenseStatus.APPROVED -> Color(0xFF4CAF50)
        ExpenseStatus.COMPLETE -> Color(0xFF2196F3)
        ExpenseStatus.PENDING  -> Color(0xFFFF9800)
        ExpenseStatus.REJECTED -> Color(0xFFF44336)
        else                   -> Color(0xFF9E9E9E)
    }

    val statusLabel = when (expense.status) {
        ExpenseStatus.APPROVED -> "Approved"
        ExpenseStatus.COMPLETE -> "Complete"
        ExpenseStatus.PENDING  -> "Pending"
        ExpenseStatus.REJECTED -> "Rejected"
        else                   -> "Unknown"
    }

    val formattedDate = FormatUtils.formatDateLong(expense.date)
    val paymentMethod = when (expense.modeOfPayment) {
        PaymentMode.CASH   -> "Cash"
        PaymentMode.UPI    -> "UPI"
        PaymentMode.CHEQUE -> "Cheque"
        else               -> if (expense.modeOfPayment != null) expense.modeOfPayment.toString() else ""
    }

    val submitterName = remember(expense.submittedBy, userNameMap, businessHeadPhone) {
        val normalizedSubmittedBy = expense.submittedBy.replace("+91", "").trim()
        val normalizedBHPhone = businessHeadPhone?.replace("+91", "")?.trim()
        when {
            normalizedBHPhone != null && normalizedSubmittedBy == normalizedBHPhone -> "Business Head"
            expense.submittedBy.isBlank() -> "Unknown"
            else -> userNameMap[expense.submittedBy] ?: expense.submittedBy
        }
    }

    val phaseName = remember(expense.phaseId, phases) {
        if (expense.phaseId?.isNotEmpty() == true && phases.isNotEmpty()) {
            val found = phases.find { it.id.trim() == expense.phaseId?.trim() }
                ?: phases.find { it.id.trim().equals(expense.phaseId?.trim() ?: "", ignoreCase = true) }
            found?.phaseName?.takeIf { it.isNotEmpty() } ?: expense.description.take(15).ifEmpty { "Phase" }
        } else {
            expense.description.take(15).ifEmpty { "Phase" }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToReview() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── Top Row: Phase tag + Status chip ───────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phase tag
                Surface(
                    color = Color(0xFF1565C0).copy(alpha = 0.10f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = phaseName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1565C0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Status chip
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // ── Expense Code (highlighted) ─────────────────────────────────
            if (expense.expenseCode.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Code: ${expense.expenseCode}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Middle Row: Submitter + Amount ─────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = submitterName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosPalette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = expense.itemType ?: expense.category.ifEmpty { "Uncategorized" },
                        fontSize = 13.sp,
                        color = iosPalette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = FormatUtils.formatCurrency(expense.amount),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Divider ────────────────────────────────────────────────────
            HorizontalDivider(color = iosPalette.hairline, thickness = 0.5.dp)

            Spacer(modifier = Modifier.height(10.dp))

            // ── Bottom Row: Date + Payment method + Action icons ───────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: date + payment pill
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        fontSize = 12.sp,
                        color = iosPalette.textSecondary
                    )
                    if (paymentMethod.isNotEmpty()) {
                        Surface(
                            color = iosPalette.tier3Field,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = paymentMethod,
                                fontSize = 11.sp,
                                color = iosPalette.textSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Right: action icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (expense.attachmentUrl.isNotEmpty()) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Attachment",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF1565C0)
                        )
                    }
                    if (expense.paymentProofUrl.isNotEmpty()) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Payment Proof",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF2196F3)
                        )
                    }
                    val isSubmittedByCurrentUser = currentUserPhone != null &&
                        expense.submittedBy.isNotEmpty() &&
                        expense.submittedBy.replace("+91", "").trim() == currentUserPhone
                    if (expense.status == ExpenseStatus.PENDING && isSubmittedByCurrentUser) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onEditExpense(expense.id) },
                            tint = Color(0xFFFF9800)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Delegate Preview Section
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DelegatePreviewSection(
    delegates: List<DepartmentTemporaryApproverEntry>,
    isInteractive: Boolean = true,
    fromNotification: String = "",
    highlightKey: Int = 0,
    bringIntoViewRequester: BringIntoViewRequester? = null,
    onClick: () -> Unit
) {
    val iosPalette = rememberIosDashboardPalette()
    val isDark = isSystemInDarkTheme()
    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()) }
    val preview = delegates.take(2)
    val initialHighlightTag = remember(fromNotification, highlightKey) {
        normalizeExpenseListNotificationTag(fromNotification)
    }
    var activeHighlightTag by remember(initialHighlightTag) {
        mutableStateOf(initialHighlightTag)
    }
    LaunchedEffect(initialHighlightTag) {
        activeHighlightTag = initialHighlightTag
        if (activeHighlightTag == "temp_approver") {
            delay(2500L)
            activeHighlightTag = ""
        }
    }
    val highlightColor by animateColorAsState(
        targetValue = if (activeHighlightTag == "temp_approver") {
            iosPalette.accent.copy(alpha = if (isDark) 0.26f else 0.14f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 900),
        label = "TempApproverHighlight"
    )
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    var sectionHeightPx by remember { mutableStateOf(0f) }

    LaunchedEffect(initialHighlightTag, sectionHeightPx, bringIntoViewRequester) {
        if (initialHighlightTag == "temp_approver" && sectionHeightPx > 0f) {
            val centeredTop = ((screenHeightPx - sectionHeightPx) / 2f).coerceAtLeast(0f)
            bringIntoViewRequester?.bringIntoView(
                Rect(
                    0f,
                    -centeredTop,
                    1f,
                    sectionHeightPx - centeredTop
                )
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (bringIntoViewRequester != null) {
                    Modifier.bringIntoViewRequester(bringIntoViewRequester)
                } else {
                    Modifier
                }
            )
            .onGloballyPositioned { coordinates ->
                sectionHeightPx = coordinates.size.height.toFloat()
            }
            .clickable(enabled = isInteractive) {
                if (isInteractive) onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = iosPalette.tier2Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp),
        border = BorderStroke(0.75.dp, iosPalette.hairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(highlightColor, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = iosPalette.accent.copy(alpha = if (isDark) 0.28f else 0.14f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = iosPalette.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Delegations",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iosPalette.textPrimary
                        )
                        Text(
                            text = "${delegates.size} delegate${if (delegates.size != 1) "s" else ""}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (isInteractive) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "View all delegations",
                        tint = iosPalette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (preview.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                preview.forEachIndexed { index, entry ->
                    DelegatePreviewRow(entry = entry, dateFormat = dateFormat)
                    if (index < preview.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                if (delegates.size > 2) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = iosPalette.hairline, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "+${delegates.size - 2} more",
                        fontSize = 12.sp,
                        color = iosPalette.accent,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun DelegatePreviewRow(
    entry: DepartmentTemporaryApproverEntry,
    dateFormat: java.text.SimpleDateFormat
) {
    val iosPalette = rememberIosDashboardPalette()
    val isDark = isSystemInDarkTheme()
    val today = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.time
    }
    val isCurrentlyActive = entry.isAccepted && entry.isActive &&
        entry.startDate != null && entry.endDate != null &&
        !today.before(entry.startDate) && !today.after(entry.endDate)

    val statusColor = if (isCurrentlyActive) PositiveAmountColor else iosPalette.textSecondary
    val statusBg = if (isCurrentlyActive) {
        PositiveAmountColor.copy(alpha = if (isDark) 0.24f else 0.14f)
    } else {
        iosPalette.textSecondary.copy(alpha = if (isDark) 0.20f else 0.10f)
    }
    val statusText = if (isCurrentlyActive) "Active" else "Inactive"

    // Fetch approver name from users collection (document ID = phone number)
    var approverName by remember(entry.phone) { mutableStateOf<String?>(null) }
    LaunchedEffect(entry.phone) {
        if (entry.phone.isNotBlank()) {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(entry.phone)
                    .get()
                    .await()
                approverName = doc.getString("name")?.takeIf { it.isNotBlank() }
            } catch (_: Exception) { }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Avatar circle with initials
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iosPalette.accent.copy(alpha = if (isDark) 0.24f else 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = iosPalette.accent,
                modifier = Modifier.size(20.dp)
            )
        }

        // Info column
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Department badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PhaseTotalTextColor.copy(alpha = if (isDark) 0.24f else 0.12f)
                ) {
                    Text(
                        text = entry.departmentName.ifBlank { "—" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PhaseTotalTextColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                // Status chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            // Name + Phone
            if (approverName != null) {
                Text(
                    text = approverName!!,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iosPalette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = entry.phone.ifBlank { "—" },
                fontSize = 12.sp,
                color = if (approverName != null) iosPalette.textSecondary else iosPalette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Date range
            val startStr = entry.startDate?.let { dateFormat.format(it) } ?: "—"
            val endStr = entry.endDate?.let { dateFormat.format(it) } ?: "Ongoing"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "$startStr → $endStr",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
